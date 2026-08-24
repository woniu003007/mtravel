package com.mtravel.platform.purchase.resource.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.optional.entity.PurchaseRelationOptionalItemEntity;
import com.mtravel.platform.purchase.relation.optional.mapper.PurchaseRelationOptionalItemMapper;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
import com.mtravel.platform.purchase.resource.dto.ResourceSupplierCreateRequest;
import com.mtravel.platform.purchase.resource.dto.ResourceSupplierOptionalItemRequest;
import com.mtravel.platform.purchase.resource.dto.ResourceSupplierPriceLineRequest;
import com.mtravel.platform.purchase.resource.dto.ResourceSupplierUpdateRequest;
import com.mtravel.platform.purchase.resource.dto.ScenicSupplierCreateRequest;
import com.mtravel.platform.purchase.resource.dto.ScenicSupplierCreateResponse;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;
import com.mtravel.platform.purchase.resource.optional.mapper.PurchaseResourceOptionalItemMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 景区资源页快捷新增供应商服务。
 *
 * <p>该服务只负责景区页内“新增供应商并绑定当前景区”场景：创建供应商档案、创建采购关系、
 * 设置默认供应商、写入价格明细。景区资料与知识库链路不在这里处理。</p>
 */
@Service
public class ScenicSupplierCreateService {

    private static final String RESOURCE_TYPE = "scenic";
    private static final String VEHICLE_RESOURCE_TYPE = "vehicle";
    private static final String SUPPLIER_CATEGORY = "scenic";

    private final PurchaseResourceMapper resourceMapper;
    private final SupplierMapper supplierMapper;
    private final PurchaseRelationMapper relationMapper;
    private final SupplierResourcePriceMapper priceMapper;
    private final PurchaseRelationOptionalItemMapper optionalItemMapper;
    private final PurchaseResourceOptionalItemMapper resourceOptionalItemMapper;
    private final EnterpriseExpenseItemMapper expenseItemMapper;

    @Autowired
    public ScenicSupplierCreateService(
            PurchaseResourceMapper resourceMapper,
            SupplierMapper supplierMapper,
            PurchaseRelationMapper relationMapper,
            SupplierResourcePriceMapper priceMapper,
            EnterpriseExpenseItemMapper expenseItemMapper,
            PurchaseRelationOptionalItemMapper optionalItemMapper,
            PurchaseResourceOptionalItemMapper resourceOptionalItemMapper
    ) {
        this.resourceMapper = resourceMapper;
        this.supplierMapper = supplierMapper;
        this.relationMapper = relationMapper;
        this.priceMapper = priceMapper;
        this.expenseItemMapper = expenseItemMapper;
        this.optionalItemMapper = optionalItemMapper;
        this.resourceOptionalItemMapper = resourceOptionalItemMapper;
    }

    /** 保留旧单元测试和历史构造入口，生产环境由 Spring 注入自费项目 Mapper。 */
    public ScenicSupplierCreateService(
            PurchaseResourceMapper resourceMapper,
            SupplierMapper supplierMapper,
            PurchaseRelationMapper relationMapper,
            SupplierResourcePriceMapper priceMapper,
            EnterpriseExpenseItemMapper expenseItemMapper
    ) {
        this(resourceMapper, supplierMapper, relationMapper, priceMapper, expenseItemMapper, null, null);
    }

    /** 兼容已有单测和历史构造入口。 */
    public ScenicSupplierCreateService(
            PurchaseResourceMapper resourceMapper, SupplierMapper supplierMapper,
            PurchaseRelationMapper relationMapper, SupplierResourcePriceMapper priceMapper,
            EnterpriseExpenseItemMapper expenseItemMapper, PurchaseRelationOptionalItemMapper optionalItemMapper
    ) { this(resourceMapper, supplierMapper, relationMapper, priceMapper, expenseItemMapper, optionalItemMapper, null); }

    /**
     * 为当前资源新建供应商并自动绑定。
     *
     * <p>该入口适用于资源总览的八类资源。统一报价保存在采购关系上，不再伪造为每个费用项目的分类价；
     * 分类报价才按前端传入的项目明细保存。</p>
     */
    @Transactional
    public ScenicSupplierCreateResponse createResourceSupplier(
            Long tenantId,
            Long resourceId,
            ResourceSupplierCreateRequest request,
            String operator
    ) {
        PurchaseResourceEntity resource = requireResource(tenantId, resourceId);
        validateResourceSupplierRequest(resource, request);
        assertSupplierNameAvailable(tenantId, request.supplierName());
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearOtherDefaultRelations(tenantId, resource.getId(), null);
        }

        SupplierEntity supplier = new SupplierEntity();
        supplier.setTenantId(tenantId);
        supplier.setSupplierCategory(resource.getResourceType());
        supplier.setSupplierName(cleanRequired(request.supplierName()));
        supplier.setProvince(clean(request.province()));
        supplier.setCity(clean(request.city()));
        supplier.setDistrict(clean(request.district()));
        supplier.setBasicInfo(clean(request.basicInfo()));
        supplier.setContactName(clean(request.contactName()));
        supplier.setContactPhone(clean(request.contactPhone()));
        supplier.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        supplier.setCreatedBy(operator);
        supplier.setRemark(clean(request.remark()));
        supplier.setIsDeleted(false);
        supplierMapper.insert(supplier);

        PurchaseRelationEntity relation = new PurchaseRelationEntity();
        relation.setTenantId(tenantId);
        relation.setResourceType(resource.getResourceType());
        relation.setResourceId(resource.getId());
        relation.setResourceName(resource.getResourceName());
        relation.setSupplierId(supplier.getId());
        relation.setGroupQuantity(0);
        relation.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        relation.setPriceMode(request.priceMode());
        applyRelationPrice(
                relation,
                request.priceMode(),
                resolveRelationUnifiedPrice(resource, request.priceMode(), request.unifiedPrice()),
                request.priceRemark()
        );
        relation.setStatus("active");
        relation.setCreatedBy(operator);
        relation.setRemark(clean(request.remark()));
        relation.setIsDeleted(false);
        relationMapper.insert(relation);

        saveClassifiedPrices(tenantId, relation.getId(), resource.getResourceType(), request.priceMode(),
                request.priceLines(), operator);
        saveOptionalItems(tenantId, relation.getId(), resource.getId(), resource.getResourceType(), request.optionalItems(), operator);
        return new ScenicSupplierCreateResponse(supplier.getId(), relation.getId());
    }

    /**
     * 为当前景区新建供应商并自动绑定。
     *
     * <p>同租户同名未删除供应商不重复创建；默认供应商切换在同一事务内完成。统一报价保存在采购关系上，
     * 分类报价才写入具体团队价明细。</p>
     */
    @Transactional
    public ScenicSupplierCreateResponse create(Long tenantId, Long resourceId, ScenicSupplierCreateRequest request, String operator) {
        PurchaseResourceEntity resource = requireScenicResource(tenantId, resourceId);
        validateRequest(request);
        assertSupplierNameAvailable(tenantId, request.supplierName());
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearOtherDefaultRelations(tenantId, resource.getId(), null);
        }

        SupplierEntity supplier = new SupplierEntity();
        supplier.setTenantId(tenantId);
        supplier.setSupplierCategory(SUPPLIER_CATEGORY);
        supplier.setSupplierName(cleanRequired(request.supplierName()));
        supplier.setProvince(clean(request.province()));
        supplier.setCity(clean(request.city()));
        supplier.setDistrict(clean(request.district()));
        supplier.setBasicInfo(clean(request.basicInfo()));
        supplier.setContactName(clean(request.contactName()));
        supplier.setContactPhone(clean(request.contactPhone()));
        supplier.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        supplier.setCreatedBy(operator);
        supplier.setRemark(clean(request.remark()));
        supplier.setIsDeleted(false);
        supplierMapper.insert(supplier);

        PurchaseRelationEntity relation = new PurchaseRelationEntity();
        relation.setTenantId(tenantId);
        relation.setResourceType(resource.getResourceType());
        relation.setResourceId(resource.getId());
        relation.setResourceName(resource.getResourceName());
        relation.setSupplierId(supplier.getId());
        relation.setGroupQuantity(0);
        relation.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        relation.setPriceMode(request.priceMode());
        applyRelationPrice(relation, request.priceMode(), request.unifiedPrice(), request.priceRemark());
        relation.setStatus("active");
        relation.setCreatedBy(operator);
        relation.setRemark(clean(request.remark()));
        relation.setIsDeleted(false);
        relationMapper.insert(relation);

        saveScenicClassifiedPrices(tenantId, relation.getId(), request, operator);
        saveOptionalItems(tenantId, relation.getId(), resource.getId(), resource.getResourceType(), request.optionalItems(), operator);
        return new ScenicSupplierCreateResponse(supplier.getId(), relation.getId());
    }

    /**
     * 在资源页事务内更新供应商档案、当前资源绑定关系和报价明细。
     *
     * <p>报价明细采用软删除后批量重建，保证统一报价切换为分类报价时不会残留旧项目。</p>
     */
    @Transactional
    public ScenicSupplierCreateResponse updateResourceSupplier(
            Long tenantId,
            Long resourceId,
            Long relationId,
            ResourceSupplierUpdateRequest request,
            String operator
    ) {
        PurchaseResourceEntity resource = requireResource(tenantId, resourceId);
        PurchaseRelationEntity relation = relationMapper.selectOne(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", relationId)
                .eq("resource_id", resourceId)
                .eq("resource_type", resource.getResourceType()));
        if (relation == null) {
            throw new BizException("资源供应商绑定关系不存在或已删除");
        }
        validateResourceSupplierRequest(resource, new ResourceSupplierCreateRequest(
                request.supplierName(), request.province(), request.city(), request.district(),
                request.basicInfo(), request.contactName(), request.contactPhone(), request.status(),
                request.isDefault(), request.priceMode(), request.unifiedPrice(), request.priceLines(),
                request.optionalItems(), request.priceRemark(), request.remark()
        ));
        assertSupplierNameAvailable(tenantId, request.supplierName(), relation.getSupplierId());
        String supplierStatus = StringUtils.hasText(request.status()) ? request.status() : "active";
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearOtherDefaultRelations(tenantId, resourceId, relationId);
        }

        SupplierEntity supplier = new SupplierEntity();
        supplier.setSupplierName(cleanRequired(request.supplierName()));
        supplier.setSupplierCategory(resource.getResourceType());
        supplier.setProvince(clean(request.province()));
        supplier.setCity(clean(request.city()));
        supplier.setDistrict(clean(request.district()));
        supplier.setBasicInfo(clean(request.basicInfo()));
        supplier.setContactName(clean(request.contactName()));
        supplier.setContactPhone(clean(request.contactPhone()));
        supplier.setStatus(supplierStatus);
        supplier.setRemark(clean(request.remark()));
        int supplierUpdated = supplierMapper.update(supplier, new UpdateWrapper<SupplierEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", relation.getSupplierId()));
        if (supplierUpdated == 0) {
            throw new BizException("供应商不存在或已删除");
        }

        PurchaseRelationEntity relationUpdate = new PurchaseRelationEntity();
        relationUpdate.setPriceMode(request.priceMode());
        relationUpdate.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        relationUpdate.setStatus("active".equals(supplierStatus) ? relation.getStatus() : "disabled");
        relationUpdate.setRemark(clean(request.remark()));
        BigDecimal relationUnifiedPrice = resolveRelationUnifiedPrice(
                resource,
                request.priceMode(),
                request.unifiedPrice()
        );
        UpdateWrapper<PurchaseRelationEntity> relationWrapper = new UpdateWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", relationId)
                .set(VEHICLE_RESOURCE_TYPE.equals(resource.getResourceType()), "purchase_price", BigDecimal.ZERO)
                .set("unified_price", "unified".equals(request.priceMode()) ? relationUnifiedPrice : null)
                .set("price_remark", "unified".equals(request.priceMode()) ? clean(request.priceRemark()) : null);
        int relationUpdated = relationMapper.update(relationUpdate, relationWrapper);
        if (relationUpdated == 0) {
            throw new BizException("资源供应商绑定关系不存在或已删除");
        }

        priceMapper.update(null, new UpdateWrapper<SupplierResourcePriceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("relation_id", relationId)
                .set("is_deleted", true)
                .set("deleted_at", java.time.OffsetDateTime.now())
                .set("deleted_by", operator));
        saveClassifiedPrices(tenantId, relationId, resource.getResourceType(), request.priceMode(),
                request.priceLines(), operator);
        if (optionalItemMapper != null) {
            optionalItemMapper.softDeleteByRelation(tenantId, relationId, operator);
        }
        saveOptionalItems(tenantId, relationId, resource.getId(), resource.getResourceType(), request.optionalItems(), operator);
        return new ScenicSupplierCreateResponse(relation.getSupplierId(), relationId);
    }

    private void validateRequest(ScenicSupplierCreateRequest request) {
        if (!StringUtils.hasText(request.priceMode())) {
            throw new BizException("报价模式不能为空");
        }
        if ("unified".equals(request.priceMode())) {
            if (request.unifiedPrice() == null) {
                throw new BizException("统一报价不能为空");
            }
            validateMoneyScale(request.unifiedPrice());
            return;
        }
        validateMoneyScale(request.adultPrice());
        validateMoneyScale(request.childPrice());
        validateMoneyScale(request.studentPrice());
        validateMoneyScale(request.seniorPrice());
        validateMoneyScale(request.preferentialPrice());
        boolean hasAny = request.adultPrice() != null
                || request.childPrice() != null
                || request.studentPrice() != null
                || request.seniorPrice() != null
                || request.preferentialPrice() != null;
        if (!hasAny) {
            throw new BizException("分类报价至少填写一项");
        }
        String supplierStatus = StringUtils.hasText(request.status()) ? request.status() : "active";
        if (Boolean.TRUE.equals(request.isDefault()) && !"active".equals(supplierStatus)) {
            throw new BizException("默认供应商必须是合作中状态");
        }
    }

    /**
     * 景区允许仅维护自费项目报价；用车只绑定车队、真实排团时再询价；其它资源仍需维护报价。
     */
    private void validateResourceSupplierRequest(
            PurchaseResourceEntity resource,
            ResourceSupplierCreateRequest request
    ) {
        if (!StringUtils.hasText(request.priceMode())) {
            throw new BizException("报价模式不能为空");
        }
        String supplierStatus = StringUtils.hasText(request.status()) ? request.status() : "active";
        if (Boolean.TRUE.equals(request.isDefault()) && !"active".equals(supplierStatus)) {
            throw new BizException("默认供应商必须是合作中状态");
        }
        if (VEHICLE_RESOURCE_TYPE.equals(resource.getResourceType())) {
            return;
        }
        boolean hasOptionalItemQuote = RESOURCE_TYPE.equals(resource.getResourceType())
                && request.optionalItems() != null
                && request.optionalItems().stream().anyMatch(item ->
                        StringUtils.hasText(item.projectName()) && item.costPrice() != null);
        if ("unified".equals(request.priceMode())) {
            if (request.unifiedPrice() != null) {
                validateMoneyScale(request.unifiedPrice());
            } else if (!hasOptionalItemQuote) {
                throw new BizException("请填写门票统一报价，或至少维护一条自费项目报价");
            }
            return;
        }
        boolean hasAny = false;
        for (ResourceSupplierPriceLineRequest line : Objects.requireNonNullElse(
                request.priceLines(), List.<ResourceSupplierPriceLineRequest>of())) {
            if (line.teamPrice() != null) {
                validateMoneyScale(line.teamPrice());
                hasAny = true;
            }
        }
        if (!hasAny && !hasOptionalItemQuote) {
            throw new BizException("分类门票报价至少填写一项，或至少维护一条自费项目报价");
        }
    }

    /** 统一报价属于整条采购关系；分类模式必须清空关系级报价，防止两套金额并存。 */
    private void applyRelationPrice(
            PurchaseRelationEntity relation,
            String priceMode,
            BigDecimal unifiedPrice,
            String priceRemark
    ) {
        if ("unified".equals(priceMode)) {
            relation.setUnifiedPrice(unifiedPrice);
            relation.setPriceRemark(clean(priceRemark));
            return;
        }
        relation.setUnifiedPrice(null);
        relation.setPriceRemark(null);
    }

    /**
     * 景区只有自费项目而没有门票时，门票统一价按零元保存。
     *
     * <p>数据库要求统一报价必须有非负金额；零元明确表达“无门票成本”，自费项目仍在独立报价表保存，
     * 避免把该供应商关系误判为待询价。</p>
     */
    private BigDecimal resolveRelationUnifiedPrice(
            PurchaseResourceEntity resource,
            String priceMode,
            BigDecimal unifiedPrice
    ) {
        if (RESOURCE_TYPE.equals(resource.getResourceType())
                && "unified".equals(priceMode)
                && unifiedPrice == null) {
            return BigDecimal.ZERO;
        }
        return unifiedPrice;
    }

    /** 仅分类报价写入费用项目明细。 */
    private void saveClassifiedPrices(
            Long tenantId,
            Long relationId,
            String resourceType,
            String priceMode,
            List<ResourceSupplierPriceLineRequest> priceLines,
            String operator
    ) {
        if ("unified".equals(priceMode)) {
            return;
        }
        List<ResourceSupplierPriceLineRequest> lines = Objects.requireNonNullElse(priceLines, List.of());
        List<SupplierResourcePriceEntity> prices = new java.util.ArrayList<>(lines.size());
        for (ResourceSupplierPriceLineRequest line : lines) {
            if (line.teamPrice() == null) {
                continue;
            }
            EnterpriseExpenseItemEntity project = requireProject(tenantId, resourceType, line.resourceProjectId());
            prices.add(buildPrice(tenantId, relationId, project, line.teamPrice(), line.priceDescription(), operator));
        }
        if (!prices.isEmpty()) {
            priceMapper.insertBatch(prices);
        }
    }

    private void saveScenicClassifiedPrices(Long tenantId, Long relationId, ScenicSupplierCreateRequest request, String operator) {
        List<SupplierResourcePriceEntity> prices = new java.util.ArrayList<>(5);
        if ("unified".equals(request.priceMode())) {
            return;
        }
        addPriceIfPresent(prices, tenantId, relationId, "成人", request.adultPrice(), request.priceRemark(), operator);
        addPriceIfPresent(prices, tenantId, relationId, "儿童", request.childPrice(), request.priceRemark(), operator);
        addPriceIfPresent(prices, tenantId, relationId, "学生", request.studentPrice(), request.priceRemark(), operator);
        addPriceIfPresent(prices, tenantId, relationId, "老人", request.seniorPrice(), request.priceRemark(), operator);
        addPriceIfPresent(prices, tenantId, relationId, "优待", request.preferentialPrice(), request.priceRemark(), operator);
        if (!prices.isEmpty()) {
            priceMapper.insertBatch(prices);
        }
    }

    /**
     * 保存供应商关系下的自费项目报价。
     *
     * <p>自费项目只属于景区资源，价格是供应商成本价且固定按元/人。项目名称在同一绑定关系内
     * 不能重复；更新时由调用方先软删除旧快照，再插入当前表单中的启用/停用项目。</p>
     */
    private void saveOptionalItems(
            Long tenantId,
            Long relationId,
            Long resourceId,
            String resourceType,
            List<ResourceSupplierOptionalItemRequest> optionalItems,
            String operator
    ) {
        List<ResourceSupplierOptionalItemRequest> items = Objects.requireNonNullElse(optionalItems, List.of());
        if (items.isEmpty()) {
            return;
        }
        if (!RESOURCE_TYPE.equals(resourceType)) {
            throw new BizException("自费项目报价仅适用于景区资源");
        }
        if (optionalItemMapper == null) {
            throw new BizException("自费项目报价服务未配置");
        }
        if (resourceOptionalItemMapper == null) {
            throw new BizException("自费项目主档服务未配置");
        }

        Set<String> names = new HashSet<>();
        List<PurchaseRelationOptionalItemEntity> entities = new java.util.ArrayList<>(items.size());
        for (ResourceSupplierOptionalItemRequest item : items) {
            String projectName = clean(item.projectName());
            if (!StringUtils.hasText(projectName)) {
                throw new BizException("自费项目名称不能为空");
            }
            if (!names.add(projectName)) {
                throw new BizException("同一供应商下自费项目名称不能重复");
            }
            if (item.costPrice() == null) {
                throw new BizException("自费项目供应商成本价不能为空");
            }
            validateMoneyScale(item.costPrice());
            if (item.suggestedSalePrice() != null) {
                validateMoneyScale(item.suggestedSalePrice());
            }
            Long optionalItemId = resolveOptionalItemMaster(
                    tenantId, resourceId, item.resourceOptionalItemId(), projectName, operator);

            PurchaseRelationOptionalItemEntity entity = new PurchaseRelationOptionalItemEntity();
            entity.setTenantId(tenantId);
            entity.setRelationId(relationId);
            entity.setResourceOptionalItemId(optionalItemId);
            entity.setProjectName(projectName);
            entity.setCostPrice(item.costPrice());
            entity.setSuggestedSalePrice(item.suggestedSalePrice());
            entity.setPriceUnit("yuan_per_person");
            entity.setPriceDescription(clean(item.priceDescription()));
            entity.setStatus(StringUtils.hasText(item.status()) ? item.status() : "active");
            if (!"active".equals(entity.getStatus()) && !"disabled".equals(entity.getStatus())) {
                throw new BizException("自费项目状态不合法");
            }
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            entities.add(entity);
        }
        optionalItemMapper.insertBatch(entities);
    }

    /**
     * 将手工录入的报价项目同步到当前景区的项目主档。
     *
     * <p>报价表单只要求业务人员填写名称；同名主档已存在时直接复用，不存在时自动补建，
     * 使介绍素材和产品设计仍能稳定地按主档 ID 关联。</p>
     */
    private Long resolveOptionalItemMaster(
            Long tenantId,
            Long resourceId,
            Long requestedOptionalItemId,
            String projectName,
            String operator
    ) {
        if (requestedOptionalItemId != null) {
            PurchaseResourceOptionalItemEntity master = resourceOptionalItemMapper.selectOne(
                    new QueryWrapper<PurchaseResourceOptionalItemEntity>()
                            .eq("tenant_id", tenantId).eq("id", requestedOptionalItemId)
                            .eq("resource_id", resourceId).eq("is_deleted", false).last("limit 1"));
            if (master == null) {
                throw new BizException("关联自费项目不属于当前景区或已删除");
            }
            return master.getId();
        }

        PurchaseResourceOptionalItemEntity master = resourceOptionalItemMapper.selectOne(
                new QueryWrapper<PurchaseResourceOptionalItemEntity>()
                        .eq("tenant_id", tenantId).eq("resource_id", resourceId)
                        .eq("project_name", projectName).eq("is_deleted", false).last("limit 1"));
        if (master != null) {
            return master.getId();
        }

        master = new PurchaseResourceOptionalItemEntity();
        master.setTenantId(tenantId);
        master.setResourceId(resourceId);
        master.setProjectName(projectName);
        master.setItemType("recommended_self_pay");
        master.setPriceUnit("yuan_per_person");
        master.setStatus("active");
        master.setCreatedBy(operator);
        master.setIsDeleted(false);
        resourceOptionalItemMapper.insert(master);
        return master.getId();
    }

    private void addPriceIfPresent(
            List<SupplierResourcePriceEntity> prices,
            Long tenantId,
            Long relationId,
            String projectName,
            BigDecimal teamPrice,
            String priceRemark,
            String operator
    ) {
        if (teamPrice == null) {
            return;
        }
        EnterpriseExpenseItemEntity project = requireProject(tenantId, projectName);
        prices.add(buildPrice(tenantId, relationId, project, teamPrice, priceRemark, operator));
    }

    private SupplierResourcePriceEntity buildPrice(
            Long tenantId,
            Long relationId,
            String projectName,
            BigDecimal teamPrice,
            String priceRemark,
            String operator
    ) {
        EnterpriseExpenseItemEntity project = requireProject(tenantId, projectName);
        return buildPrice(tenantId, relationId, project, teamPrice, priceRemark, operator);
    }

    private SupplierResourcePriceEntity buildPrice(
            Long tenantId,
            Long relationId,
            EnterpriseExpenseItemEntity project,
            BigDecimal teamPrice,
            String priceRemark,
            String operator
    ) {
        SupplierResourcePriceEntity price = new SupplierResourcePriceEntity();
        price.setTenantId(tenantId);
        price.setRelationId(relationId);
        price.setResourceProjectId(project.getId());
        price.setProjectName(project.getProjectName());
        price.setMarketPrice(BigDecimal.ZERO);
        price.setPeerPrice(BigDecimal.ZERO);
        price.setTeamPrice(teamPrice);
        price.setPriceDescription(clean(priceRemark));
        price.setStatus("active");
        price.setCreatedBy(operator);
        price.setRemark(null);
        price.setIsDeleted(false);
        return price;
    }

    private PurchaseResourceEntity requireResource(Long tenantId, Long resourceId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", resourceId));
        if (resource == null) {
            throw new BizException("采购资源不存在或已删除");
        }
        return resource;
    }

    private PurchaseResourceEntity requireScenicResource(Long tenantId, Long resourceId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", resourceId));
        if (resource == null || !RESOURCE_TYPE.equals(resource.getResourceType())) {
            throw new BizException("景区资源不存在或已删除");
        }
        return resource;
    }

    private PurchaseRelationEntity requireRelation(
            Long tenantId,
            PurchaseResourceEntity resource,
            Long relationId
    ) {
        PurchaseRelationEntity relation = relationMapper.selectOne(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", relationId)
                .eq("resource_id", resource.getId())
                .eq("resource_type", resource.getResourceType()));
        if (relation == null) {
            throw new BizException("资源供应商绑定关系不存在或已删除");
        }
        return relation;
    }

    private EnterpriseExpenseItemEntity requireProject(Long tenantId, String projectName) {
        EnterpriseExpenseItemEntity project = expenseItemMapper.selectOne(new QueryWrapper<EnterpriseExpenseItemEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_type", RESOURCE_TYPE)
                .eq("project_name", projectName));
        if (project == null) {
            throw new BizException("景区费用项目不存在，请先维护资源项目");
        }
        return project;
    }

    private EnterpriseExpenseItemEntity requireProject(Long tenantId, String resourceType, Long projectId) {
        EnterpriseExpenseItemEntity project = expenseItemMapper.selectOne(new QueryWrapper<EnterpriseExpenseItemEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", projectId));
        if (project == null || !resourceType.equals(project.getResourceType())) {
            throw new BizException("资源费用项目不存在或和资源类型不匹配");
        }
        return project;
    }

    /** 同名供应商必须去供应商管理页绑定已有档案，避免资源页快捷新增覆盖已有供应商信息。 */
    private void assertSupplierNameAvailable(Long tenantId, String supplierName) {
        assertSupplierNameAvailable(tenantId, supplierName, null);
    }

    private void assertSupplierNameAvailable(Long tenantId, String supplierName, Long excludeSupplierId) {
        Long count = supplierMapper.selectCount(new QueryWrapper<SupplierEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("supplier_name", cleanRequired(supplierName))
                .ne(excludeSupplierId != null, "id", excludeSupplierId));
        if (count != null && count > 0) {
            throw new BizException("供应商名称已存在，请选择绑定已有供应商");
        }
    }

    private void clearOtherDefaultRelations(Long tenantId, Long resourceId, Long excludeRelationId) {
        PurchaseRelationEntity entity = new PurchaseRelationEntity();
        entity.setIsDefault(Boolean.FALSE);
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PurchaseRelationEntity> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("is_default", true);
        if (excludeRelationId != null) {
            wrapper.ne("id", excludeRelationId);
        }
        relationMapper.update(entity, wrapper);
    }

    private void validateMoneyScale(BigDecimal value) {
        if (value != null && value.scale() > 2) {
            throw new BizException("报价最多保留两位小数");
        }
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }
}
