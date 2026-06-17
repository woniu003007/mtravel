package com.mtravel.platform.customer.productauth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.productauth.dto.CustomerProductAuthorizationResponse;
import com.mtravel.platform.customer.productauth.dto.CustomerProductAuthorizationSaveRequest;
import com.mtravel.platform.customer.productauth.entity.CustomerProductAuthorizationEntity;
import com.mtravel.platform.customer.productauth.mapper.CustomerProductAuthorizationMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 客户产品授权服务。
 *
 * <p>授权记录用于控制客户能销售或下单哪些产品。首版先提供授权台账维护；
 * 订单下单时的产品授权校验后续由销售订单模块接入。</p>
 */
@Service
public class CustomerProductAuthorizationService
        extends BusinessCrudService<CustomerProductAuthorizationEntity, CustomerProductAuthorizationResponse> {

    private final CustomerProductAuthorizationMapper mapper;
    private final CustomerUnitMapper customerMapper;

    public CustomerProductAuthorizationService(
            CustomerProductAuthorizationMapper mapper,
            CustomerUnitMapper customerMapper
    ) {
        super(mapper);
        this.mapper = mapper;
        this.customerMapper = customerMapper;
    }

    /** 分页查询客户产品授权。 */
    public PageResult<CustomerProductAuthorizationResponse> page(
            Long tenantId,
            String keyword,
            String status,
            Long customerId,
            long page,
            long pageSize
    ) {
        QueryWrapper<CustomerProductAuthorizationEntity> wrapper = baseQuery(tenantId)
                .eq(customerId != null, "customer_id", customerId)
                .eq(StringUtils.hasText(status), "authorization_status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("product_name", keyword)
                        .or()
                        .like("product_code", keyword))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /** 新增客户产品授权，同一客户同一产品不允许重复授权。 */
    public CustomerProductAuthorizationResponse create(
            CustomerProductAuthorizationSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertCustomerExists(tenantId, request.customerId());
        assertDuplicate(tenantId, request.customerId(), request.productName(), null);

        CustomerProductAuthorizationEntity entity = new CustomerProductAuthorizationEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /** 修改客户产品授权。 */
    public CustomerProductAuthorizationResponse update(
            Long id,
            CustomerProductAuthorizationSaveRequest request,
            Long tenantId
    ) {
        assertCustomerExists(tenantId, request.customerId());
        assertDuplicate(tenantId, request.customerId(), request.productName(), id);

        CustomerProductAuthorizationEntity entity = new CustomerProductAuthorizationEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    private void applyFields(
            CustomerProductAuthorizationEntity entity,
            CustomerProductAuthorizationSaveRequest request
    ) {
        entity.setCustomerId(request.customerId());
        entity.setProductCode(clean(request.productCode()));
        entity.setProductName(cleanRequired(request.productName()));
        entity.setAuthorizedStartDate(request.authorizedStartDate());
        entity.setAuthorizedEndDate(request.authorizedEndDate());
        entity.setAuthorizationStatus(
                StringUtils.hasText(request.authorizationStatus()) ? request.authorizationStatus() : "active"
        );
        entity.setSaleScope(request.saleScope());
        entity.setRemark(request.remark());
    }

    private void assertDuplicate(Long tenantId, Long customerId, String productName, Long excludeId) {
        Long count = mapper.selectCount(baseQuery(tenantId)
                .eq("customer_id", customerId)
                .eq("product_name", productName)
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("客户产品授权已存在");
        }
    }

    private void assertCustomerExists(Long tenantId, Long customerId) {
        CustomerUnitEntity customer = customerMapper.selectOne(new QueryWrapper<CustomerUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", customerId));
        if (customer == null) {
            throw new BizException("客户单位不存在或已删除");
        }
    }

    private String customerName(Long tenantId, Long customerId) {
        CustomerUnitEntity customer = customerMapper.selectOne(new QueryWrapper<CustomerUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", customerId));
        return customer == null ? null : customer.getCustomerName();
    }

    @Override
    protected CustomerProductAuthorizationEntity newEntity() {
        return new CustomerProductAuthorizationEntity();
    }

    @Override
    protected CustomerProductAuthorizationResponse toResponse(CustomerProductAuthorizationEntity entity) {
        return CustomerProductAuthorizationResponse.fromEntity(
                entity,
                customerName(entity.getTenantId(), entity.getCustomerId())
        );
    }

    @Override
    protected String notFoundMessage() {
        return "客户产品授权不存在或已删除";
    }
}
