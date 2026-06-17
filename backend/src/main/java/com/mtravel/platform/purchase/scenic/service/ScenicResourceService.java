package com.mtravel.platform.purchase.scenic.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.common.SupplierLookupService;
import com.mtravel.platform.purchase.scenic.dto.ScenicResourceResponse;
import com.mtravel.platform.purchase.scenic.dto.ScenicResourceSaveRequest;
import com.mtravel.platform.purchase.scenic.entity.ScenicResourceEntity;
import com.mtravel.platform.purchase.scenic.mapper.ScenicResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 景区资源服务。
 *
 * <p>景区资源按“景区 + 票种”维护，重点保存供应商、采购价、协议价和免票半票规则。
 * 票务系统自动下单和 Excel 模板生成属于销售票务模块，后续再通过景区资源数据联动。</p>
 */
@Service
public class ScenicResourceService extends BusinessCrudService<ScenicResourceEntity, ScenicResourceResponse> {

    private final ScenicResourceMapper mapper;
    private final SupplierLookupService supplierLookup;

    public ScenicResourceService(ScenicResourceMapper mapper, SupplierLookupService supplierLookup) {
        super(mapper);
        this.mapper = mapper;
        this.supplierLookup = supplierLookup;
    }

    /** 分页查询景区资源。 */
    public PageResult<ScenicResourceResponse> page(
            Long tenantId,
            String keyword,
            String city,
            String status,
            Long supplierId,
            long page,
            long pageSize
    ) {
        QueryWrapper<ScenicResourceEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(city), "city", city)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(supplierId != null, "supplier_id", supplierId)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("scenic_name", keyword)
                        .or()
                        .like("ticket_type", keyword))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /** 新增景区资源。 */
    public ScenicResourceResponse create(ScenicResourceSaveRequest request, Long tenantId, String operator) {
        supplierLookup.assertSupplierIfPresent(tenantId, request.supplierId());
        assertDuplicate(tenantId, request.scenicName(), request.ticketType(), null);

        ScenicResourceEntity entity = new ScenicResourceEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /** 修改景区资源。 */
    public ScenicResourceResponse update(Long id, ScenicResourceSaveRequest request, Long tenantId) {
        supplierLookup.assertSupplierIfPresent(tenantId, request.supplierId());
        assertDuplicate(tenantId, request.scenicName(), request.ticketType(), id);

        ScenicResourceEntity entity = new ScenicResourceEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    private void assertDuplicate(Long tenantId, String scenicName, String ticketType, Long excludeId) {
        Long count = mapper.selectCount(baseQuery(tenantId)
                .eq("scenic_name", scenicName)
                .eq("ticket_type", ticketType)
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("景区资源已存在");
        }
    }

    private void applyFields(ScenicResourceEntity entity, ScenicResourceSaveRequest request) {
        entity.setScenicName(cleanRequired(request.scenicName()));
        entity.setTicketType(cleanRequired(request.ticketType()));
        entity.setCity(clean(request.city()));
        entity.setArea(clean(request.area()));
        entity.setAddress(clean(request.address()));
        entity.setSupplierId(request.supplierId());
        entity.setPurchasePrice(money(request.purchasePrice()));
        entity.setAgreementPrice(money(request.agreementPrice()));
        entity.setPriceUnit(StringUtils.hasText(request.priceUnit()) ? request.priceUnit() : "人");
        entity.setValidFrom(request.validFrom());
        entity.setValidTo(request.validTo());
        entity.setFreeTicketRule(request.freeTicketRule());
        entity.setHalfTicketRule(request.halfTicketRule());
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(request.remark());
    }

    @Override
    protected ScenicResourceEntity newEntity() {
        return new ScenicResourceEntity();
    }

    @Override
    protected ScenicResourceResponse toResponse(ScenicResourceEntity entity) {
        return ScenicResourceResponse.fromEntity(
                entity,
                supplierLookup.supplierName(entity.getTenantId(), entity.getSupplierId())
        );
    }

    @Override
    protected String notFoundMessage() {
        return "景区资源不存在或已删除";
    }
}
