package com.mtravel.platform.purchase.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.purchase.supplier.dto.SupplierResponse;
import com.mtravel.platform.purchase.supplier.dto.SupplierSaveRequest;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 供应商管理服务。
 *
 * <p>供应商是采购侧共用基础档案。酒店、景区、采购关系和采购合同都会引用供应商，
 * 因此查询、更新、删除都必须保留租户边界和软删除条件。</p>
 */
@Service
public class SupplierService extends BusinessCrudService<SupplierEntity, SupplierResponse> {

    private final SupplierMapper mapper;
    private final CustomerUnitMapper customerMapper;
    private final PurchaseRelationMapper relationMapper;
    private final SupplierResourcePriceMapper priceMapper;

    public SupplierService(
            SupplierMapper mapper,
            CustomerUnitMapper customerMapper,
            PurchaseRelationMapper relationMapper,
            SupplierResourcePriceMapper priceMapper
    ) {
        super(mapper);
        this.mapper = mapper;
        this.customerMapper = customerMapper;
        this.relationMapper = relationMapper;
        this.priceMapper = priceMapper;
    }

    /** 分页查询供应商。 */
    public PageResult<SupplierResponse> page(
            Long tenantId,
            String keyword,
            String category,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<SupplierEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(category), "supplier_category", category)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("supplier_name", keyword)
                        .or()
                        .like("contact_name", keyword)
                        .or()
                        .like("contact_phone", keyword))
                .orderByDesc("id");
        Page<SupplierEntity> result = mapper.selectPage(Page.of(page, pageSize), wrapper);
        List<SupplierEntity> entities = result.getRecords();
        Map<Long, String> buyerNames = loadBuyerNames(tenantId, entities);
        Map<Long, SupplierRelationSummary> relationSummaries = loadRelationSummaries(tenantId, entities);
        List<SupplierResponse> items = entities.stream()
                .map(entity -> {
                    SupplierRelationSummary summary = relationSummaries.get(entity.getId());
                    return SupplierResponse.fromEntity(
                            entity,
                            resolveBuyerName(buyerNames, entity.getBuyerId()),
                            summary == null ? 0 : summary.boundResourceCount(),
                            summary == null ? null : summary.defaultResourceNames(),
                            summary == null ? null : summary.priceSummary()
                    );
                })
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /** 查询启用供应商列表，用于资源和合同下拉框。 */
    public List<SupplierResponse> listActive(Long tenantId, String category) {
        return mapper.selectList(baseQuery(tenantId)
                        .eq("status", "active")
                        .eq(StringUtils.hasText(category), "supplier_category", category)
                        .orderByAsc("supplier_name"))
                .stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }

    /** 新增供应商。 */
    public SupplierResponse create(SupplierSaveRequest request, Long tenantId, String operator) {
        assertUnique(request, tenantId, null);
        assertBuyerExists(tenantId, request.buyerId());

        SupplierEntity entity = new SupplierEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /** 修改供应商。 */
    public SupplierResponse update(Long id, SupplierSaveRequest request, Long tenantId) {
        assertUnique(request, tenantId, id);
        assertBuyerExists(tenantId, request.buyerId());

        SupplierEntity entity = new SupplierEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    @Override
    public SupplierResponse detail(Long id, Long tenantId) {
        SupplierEntity entity = mapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException(notFoundMessage());
        }
        Map<Long, String> buyerNames = loadBuyerNames(tenantId, List.of(entity));
        SupplierRelationSummary summary = loadRelationSummaries(tenantId, List.of(entity)).get(entity.getId());
        return SupplierResponse.fromEntity(
                entity,
                resolveBuyerName(buyerNames, entity.getBuyerId()),
                summary == null ? 0 : summary.boundResourceCount(),
                summary == null ? null : summary.defaultResourceNames(),
                summary == null ? null : summary.priceSummary()
        );
    }

    /** 同一租户下未删除供应商名称和编码必须唯一。 */
    private void assertUnique(SupplierSaveRequest request, Long tenantId, Long excludeId) {
        assertValueNotExists(tenantId, "supplier_name", request.supplierName(), excludeId, "供应商名称已存在");
        assertValueNotExists(tenantId, "supplier_code", request.supplierCode(), excludeId, "供应商编码已存在");
    }

    private void applyFields(SupplierEntity entity, SupplierSaveRequest request) {
        entity.setSupplierCode(clean(request.supplierCode()));
        entity.setSupplierName(cleanRequired(request.supplierName()));
        entity.setSupplierCategory(StringUtils.hasText(request.supplierCategory()) ? request.supplierCategory() : "common");
        entity.setBuyerId(normalizeBuyerId(request.buyerId()));
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setSettlementMethod(clean(request.settlementMethod()));
        entity.setBasicInfo(clean(request.basicInfo()));
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setFaxNumber(clean(request.faxNumber()));
        entity.setOfficeAddress(clean(request.officeAddress()));
        entity.setAgreementName(clean(request.agreementName()));
        entity.setRating(number(request.rating()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(request.remark());
    }

    /** 关联采购商是老系统 BuyerId 关系，用于财务应收应付冲抵；0 或空值表示不关联。 */
    private Long normalizeBuyerId(Long buyerId) {
        return buyerId == null || buyerId <= 0 ? null : buyerId;
    }

    /** 校验关联采购商必须是当前租户下未删除的客户单位，避免跨租户或脏 ID 写入供应商档案。 */
    private void assertBuyerExists(Long tenantId, Long buyerId) {
        Long normalizedBuyerId = normalizeBuyerId(buyerId);
        if (normalizedBuyerId == null) {
            return;
        }
        CustomerUnitEntity buyer = customerMapper.selectOne(new QueryWrapper<CustomerUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", normalizedBuyerId));
        if (buyer == null) {
            throw new BizException("关联采购商不存在或已删除");
        }
    }

    /** 根据供应商记录中的 BuyerId 查询客户单位名称，用于列表和编辑回显。 */
    private String buyerName(Long tenantId, Long buyerId) {
        Long normalizedBuyerId = normalizeBuyerId(buyerId);
        if (normalizedBuyerId == null) {
            return null;
        }
        CustomerUnitEntity buyer = customerMapper.selectOne(new QueryWrapper<CustomerUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", normalizedBuyerId));
        return buyer == null ? null : buyer.getCustomerName();
    }

    private Map<Long, String> loadBuyerNames(Long tenantId, List<SupplierEntity> entities) {
        List<Long> buyerIds = entities.stream()
                .map(SupplierEntity::getBuyerId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (buyerIds.isEmpty()) {
            return Map.of();
        }
        return customerMapper.selectList(new QueryWrapper<CustomerUnitEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("id", buyerIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(CustomerUnitEntity::getId, CustomerUnitEntity::getCustomerName));
    }

    private String resolveBuyerName(Map<Long, String> buyerNames, Long buyerId) {
        return buyerId == null ? null : buyerNames.get(buyerId);
    }

    private Map<Long, SupplierRelationSummary> loadRelationSummaries(Long tenantId, List<SupplierEntity> entities) {
        List<Long> supplierIds = entities.stream().map(SupplierEntity::getId).distinct().toList();
        if (supplierIds.isEmpty()) {
            return Map.of();
        }
        List<PurchaseRelationEntity> relations = relationMapper.selectList(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("supplier_id", supplierIds)
                .orderByDesc("is_default")
                .orderByDesc("id"));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PurchaseRelationEntity>> relationGroups = relations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PurchaseRelationEntity::getSupplierId,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        List<Long> relationIds = relations.stream().map(PurchaseRelationEntity::getId).toList();
        Map<Long, List<SupplierResourcePriceEntity>> priceGroups = loadPriceGroups(tenantId, relationIds);
        Map<Long, SupplierRelationSummary> summaryMap = new LinkedHashMap<>();
        for (Map.Entry<Long, List<PurchaseRelationEntity>> entry : relationGroups.entrySet()) {
            List<PurchaseRelationEntity> supplierRelations = entry.getValue();
            int boundCount = supplierRelations.size();
            String defaultResourceNames = supplierRelations.stream()
                    .filter(relation -> Boolean.TRUE.equals(relation.getIsDefault()))
                    .map(PurchaseRelationEntity::getResourceName)
                    .distinct()
                    .toList()
                    .stream()
                    .reduce((a, b) -> a + "、" + b)
                    .orElse(null);
            List<String> priceTexts = new ArrayList<>();
            for (PurchaseRelationEntity relation : supplierRelations) {
                String resourceName = StringUtils.hasText(relation.getResourceName())
                        ? relation.getResourceName()
                        : "未命名资源";
                if ("unified".equals(relation.getPriceMode()) && relation.getUnifiedPrice() != null) {
                    priceTexts.add(resourceName + "：统一报价¥" + formatMoney(relation.getUnifiedPrice()));
                    continue;
                }
                List<SupplierResourcePriceEntity> prices = priceGroups.getOrDefault(relation.getId(), List.of());
                if (prices.isEmpty()) {
                    continue;
                }
                String relationText = prices.stream()
                        .sorted(Comparator.comparing(SupplierResourcePriceEntity::getProjectName))
                        .map(price -> price.getProjectName() + "¥" + formatMoney(price.getTeamPrice()))
                        .reduce((a, b) -> a + "，" + b)
                        .orElse(null);
                if (relationText != null) {
                    priceTexts.add(resourceName + "：" + relationText);
                }
            }
            summaryMap.put(entry.getKey(), new SupplierRelationSummary(boundCount, defaultResourceNames,
                    priceTexts.isEmpty() ? null : String.join("；", priceTexts)));
        }
        return summaryMap;
    }

    private Map<Long, List<SupplierResourcePriceEntity>> loadPriceGroups(Long tenantId, List<Long> relationIds) {
        if (relationIds.isEmpty()) {
            return Map.of();
        }
        List<SupplierResourcePriceEntity> prices = priceMapper.selectList(new QueryWrapper<SupplierResourcePriceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("relation_id", relationIds)
                .orderByAsc("id"));
        return prices.stream().collect(java.util.stream.Collectors.groupingBy(
                SupplierResourcePriceEntity::getRelationId,
                LinkedHashMap::new,
                java.util.stream.Collectors.toList()));
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "0.00" : value.stripTrailingZeros().toPlainString();
    }

    private record SupplierRelationSummary(Integer boundResourceCount, String defaultResourceNames, String priceSummary) {}

    @Override
    protected SupplierEntity newEntity() {
        return new SupplierEntity();
    }

    @Override
    protected SupplierResponse toResponse(SupplierEntity entity) {
        return SupplierResponse.fromEntity(entity, buyerName(entity.getTenantId(), entity.getBuyerId()));
    }

    @Override
    protected String notFoundMessage() {
        return "供应商不存在或已删除";
    }
}
