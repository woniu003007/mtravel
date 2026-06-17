package com.mtravel.platform.purchase.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.purchase.common.SupplierLookupService;
import com.mtravel.platform.purchase.hotel.dto.HotelResourceResponse;
import com.mtravel.platform.purchase.hotel.dto.HotelResourceSaveRequest;
import com.mtravel.platform.purchase.hotel.entity.HotelResourceEntity;
import com.mtravel.platform.purchase.hotel.mapper.HotelResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 酒店资源服务。
 *
 * <p>酒店资源按“酒店 + 房型”维护，首版聚焦资源基础库和采购价格台账。
 * 房态扣减、占房和调房属于计调房态模块，不在本服务里提前联动。</p>
 */
@Service
public class HotelResourceService extends BusinessCrudService<HotelResourceEntity, HotelResourceResponse> {

    private final HotelResourceMapper mapper;
    private final SupplierLookupService supplierLookup;

    public HotelResourceService(HotelResourceMapper mapper, SupplierLookupService supplierLookup) {
        super(mapper);
        this.mapper = mapper;
        this.supplierLookup = supplierLookup;
    }

    /** 分页查询酒店资源。 */
    public PageResult<HotelResourceResponse> page(
            Long tenantId,
            String keyword,
            String city,
            String status,
            Long supplierId,
            long page,
            long pageSize
    ) {
        QueryWrapper<HotelResourceEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(city), "city", city)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(supplierId != null, "supplier_id", supplierId)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("hotel_name", keyword)
                        .or()
                        .like("room_type", keyword))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /** 新增酒店资源。 */
    public HotelResourceResponse create(HotelResourceSaveRequest request, Long tenantId, String operator) {
        supplierLookup.assertSupplierIfPresent(tenantId, request.supplierId());
        assertDuplicate(tenantId, request.hotelName(), request.roomType(), null);

        HotelResourceEntity entity = new HotelResourceEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /** 修改酒店资源。 */
    public HotelResourceResponse update(Long id, HotelResourceSaveRequest request, Long tenantId) {
        supplierLookup.assertSupplierIfPresent(tenantId, request.supplierId());
        assertDuplicate(tenantId, request.hotelName(), request.roomType(), id);

        HotelResourceEntity entity = new HotelResourceEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    private void assertDuplicate(Long tenantId, String hotelName, String roomType, Long excludeId) {
        Long count = mapper.selectCount(baseQuery(tenantId)
                .eq("hotel_name", hotelName)
                .eq("room_type", roomType)
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("酒店资源已存在");
        }
    }

    private void applyFields(HotelResourceEntity entity, HotelResourceSaveRequest request) {
        entity.setHotelName(cleanRequired(request.hotelName()));
        entity.setRoomType(cleanRequired(request.roomType()));
        entity.setCity(clean(request.city()));
        entity.setArea(clean(request.area()));
        entity.setAddress(clean(request.address()));
        entity.setStarStandard(clean(request.starStandard()));
        entity.setSupplierId(request.supplierId());
        entity.setPurchasePrice(money(request.purchasePrice()));
        entity.setAgreementPrice(money(request.agreementPrice()));
        entity.setPriceUnit(StringUtils.hasText(request.priceUnit()) ? request.priceUnit() : "间夜");
        entity.setValidFrom(request.validFrom());
        entity.setValidTo(request.validTo());
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(request.remark());
    }

    @Override
    protected HotelResourceEntity newEntity() {
        return new HotelResourceEntity();
    }

    @Override
    protected HotelResourceResponse toResponse(HotelResourceEntity entity) {
        return HotelResourceResponse.fromEntity(
                entity,
                supplierLookup.supplierName(entity.getTenantId(), entity.getSupplierId())
        );
    }

    @Override
    protected String notFoundMessage() {
        return "酒店资源不存在或已删除";
    }
}
