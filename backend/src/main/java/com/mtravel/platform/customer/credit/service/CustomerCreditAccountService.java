package com.mtravel.platform.customer.credit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.credit.dto.CustomerCreditAccountResponse;
import com.mtravel.platform.customer.credit.dto.CustomerCreditAccountSaveRequest;
import com.mtravel.platform.customer.credit.entity.CustomerCreditAccountEntity;
import com.mtravel.platform.customer.credit.mapper.CustomerCreditAccountMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 客户授信账户服务。
 *
 * <p>本服务只做额度台账维护。订单确认和费用变更自动占用额度需要等销售订单模块完成后再接入，
 * 因此当前 occupiedAmount 和 pendingApprovalAmount 都允许财务或管理员手工维护。</p>
 */
@Service
public class CustomerCreditAccountService extends BusinessCrudService<CustomerCreditAccountEntity, CustomerCreditAccountResponse> {

    private final CustomerCreditAccountMapper mapper;
    private final CustomerUnitMapper customerMapper;

    public CustomerCreditAccountService(CustomerCreditAccountMapper mapper, CustomerUnitMapper customerMapper) {
        super(mapper);
        this.mapper = mapper;
        this.customerMapper = customerMapper;
    }

    /** 分页查询客户授信账户。 */
    public PageResult<CustomerCreditAccountResponse> page(Long tenantId, String keyword, String status, long page, long pageSize) {
        QueryWrapper<CustomerCreditAccountEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(status), "status", status)
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /** 新增客户授信账户，同一客户只能保留一条未删除账户。 */
    public CustomerCreditAccountResponse create(CustomerCreditAccountSaveRequest request, Long tenantId, String operator) {
        assertCustomerExists(tenantId, request.customerId());
        assertValueNotExists(tenantId, "customer_id", request.customerId(), null, "客户授信账户已存在");
        CustomerCreditAccountEntity entity = new CustomerCreditAccountEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /** 修改客户授信账户。 */
    public CustomerCreditAccountResponse update(Long id, CustomerCreditAccountSaveRequest request, Long tenantId) {
        assertCustomerExists(tenantId, request.customerId());
        assertValueNotExists(tenantId, "customer_id", request.customerId(), id, "客户授信账户已存在");
        CustomerCreditAccountEntity entity = new CustomerCreditAccountEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    private void applyFields(CustomerCreditAccountEntity entity, CustomerCreditAccountSaveRequest request) {
        entity.setCustomerId(request.customerId());
        entity.setCreditLimit(money(request.creditLimit()));
        entity.setOccupiedAmount(money(request.occupiedAmount()));
        entity.setPendingApprovalAmount(money(request.pendingApprovalAmount()));
        entity.setWarningThreshold(money(request.warningThreshold()));
        entity.setOverLimitAction(StringUtils.hasText(request.overLimitAction()) ? request.overLimitAction() : "remind");
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(request.remark());
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

    @Override protected CustomerCreditAccountEntity newEntity() { return new CustomerCreditAccountEntity(); }
    @Override protected CustomerCreditAccountResponse toResponse(CustomerCreditAccountEntity entity) { return CustomerCreditAccountResponse.fromEntity(entity, customerName(entity.getTenantId(), entity.getCustomerId())); }
    @Override protected String notFoundMessage() { return "客户授信账户不存在或已删除"; }

    private String customerName(Long tenantId, Long customerId) {
        CustomerUnitEntity customer = customerMapper.selectOne(new QueryWrapper<CustomerUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", customerId));
        return customer == null ? null : customer.getCustomerName();
    }
}
