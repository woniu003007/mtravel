package com.mtravel.platform.customer.risk.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.contract.mapper.ContractMapper;
import com.mtravel.platform.customer.credit.entity.CustomerCreditAccountEntity;
import com.mtravel.platform.customer.credit.mapper.CustomerCreditAccountMapper;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalApplyRequest;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalRequestEntity;
import com.mtravel.platform.customer.risk.mapper.CustomerRiskApprovalRequestMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.system.config.service.BusinessRiskConfigService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户风控审批服务测试。
 *
 * <p>收客页选择客户和提交订单都依赖这里判断合同到期、授信超限以及总经理审批状态。
 * 测试固定这些业务边界，避免前端只提示但后端保存仍可绕过。</p>
 */
class CustomerRiskApprovalServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-26T00:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void checkShouldBlockWhenContractExpiredAndApprovalEnabled() {
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2026, 1, 1), "50000"),
                null,
                true
        );

        var response = service.check(1L, 3001L, 1001L, null, new BigDecimal("9000.00"));

        assertThat(response.blocked()).isTrue();
        assertThat(response.approvalEnabled()).isTrue();
        assertThat(response.contractExpired()).isTrue();
        assertThat(response.riskTypes()).contains("contract_expired");
        assertThat(response.riskSummary()).contains("合同已于 2026-01-01 到期");
    }

    @Test
    void checkShouldWarnOnlyWhenApprovalDisabled() {
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2026, 1, 1), "50000"),
                null,
                false
        );

        var response = service.check(1L, 3001L, 1001L, null, new BigDecimal("9000.00"));

        assertThat(response.blocked()).isFalse();
        assertThat(response.approvalEnabled()).isFalse();
        assertThat(response.contractExpired()).isTrue();
        assertThat(response.riskTypes()).contains("contract_expired");
    }

    @Test
    void checkShouldDetectCreditOverLimit() {
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                credit("1000", "800", "100"),
                true
        );

        var response = service.check(1L, 3001L, 1001L, null, new BigDecimal("200.00"));

        assertThat(response.blocked()).isTrue();
        assertThat(response.creditOverLimit()).isTrue();
        assertThat(response.availableAmount()).isEqualByComparingTo("100.00");
        assertThat(response.overLimitAmount()).isEqualByComparingTo("100.00");
        assertThat(response.riskTypes()).contains("credit_over_limit");
    }

    @Test
    void applyShouldCreatePendingApprovalSnapshot() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                credit("1000", "800", "100"),
                true,
                approvalMapper
        );
        doAnswer(invocation -> {
            CustomerRiskApprovalRequestEntity entity = invocation.getArgument(0);
            entity.setId(77L);
            return 1;
        }).when(approvalMapper).insert(any(CustomerRiskApprovalRequestEntity.class));

        var response = service.apply(
                1L,
                new CustomerRiskApprovalApplyRequest(3001L, 1001L, null, new BigDecimal("200.00"), "客户要求先确认"),
                "sales01"
        );

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.requestedAmount()).isEqualByComparingTo("200.00");
        ArgumentCaptor<CustomerRiskApprovalRequestEntity> captor = ArgumentCaptor.forClass(CustomerRiskApprovalRequestEntity.class);
        verify(approvalMapper).insert(captor.capture());
        assertThat(captor.getValue().getRiskTypes()).contains("credit_over_limit");
        assertThat(captor.getValue().getApplicant()).isEqualTo("sales01");
        assertThat(captor.getValue().getAvailableAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void checkShouldReturnReusableApprovedApprovalForCurrentRiskAmount() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalRequestEntity approved = approval(
                77L,
                3001L,
                1001L,
                2001L,
                "approved",
                "200.00"
        );
        when(approvalMapper.selectOne(any(Wrapper.class))).thenReturn(approved);
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                credit("1000", "800", "100"),
                true,
                approvalMapper
        );

        var response = service.check(1L, 3001L, 1001L, 2001L, new BigDecimal("200.00"));

        assertThat(response.blocked()).isTrue();
        assertThat(response.riskApprovalRequestId()).isEqualTo(77L);
        assertThat(response.riskApprovalStatus()).isEqualTo("approved");
        assertThat(response.riskApprovalRequestedAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void approveShouldOnlyAllowBossOrAdmin() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalRequestEntity pending = new CustomerRiskApprovalRequestEntity();
        pending.setId(77L);
        pending.setTenantId(1L);
        pending.setCustomerId(3001L);
        pending.setStatus("pending");
        when(approvalMapper.selectOne(any(Wrapper.class))).thenReturn(pending);
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                null,
                true,
                approvalMapper
        );

        assertThatThrownBy(() -> service.approve(1L, 77L, "同意", "sales01", List.of("sales")))
                .isInstanceOf(BizException.class)
                .hasMessage("只有总经理或管理员可以处理风控审批");

        service.approve(1L, 77L, "同意", "boss01", List.of("boss"));

        ArgumentCaptor<CustomerRiskApprovalRequestEntity> captor = ArgumentCaptor.forClass(CustomerRiskApprovalRequestEntity.class);
        verify(approvalMapper).update(captor.capture(), any(UpdateWrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo("approved");
        assertThat(captor.getValue().getApprovedBy()).isEqualTo("boss01");
    }

    @Test
    void approveShouldRejectWhenOrderAlreadyHasApprovedApproval() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalRequestEntity pending = approval(
                78L,
                3001L,
                1001L,
                2001L,
                "pending",
                "200.00"
        );
        CustomerRiskApprovalRequestEntity approved = approval(
                77L,
                3001L,
                1001L,
                2001L,
                "approved",
                "200.00"
        );
        when(approvalMapper.selectOne(any(Wrapper.class))).thenReturn(pending, approved);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order(2001L, 1001L, 3001L));
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                null,
                true,
                approvalMapper,
                orderMapper
        );

        assertThatThrownBy(() -> service.approve(1L, 78L, "同意", "boss01", List.of("boss")))
                .isInstanceOf(BizException.class)
                .hasMessage("当前订单已有已通过的风控审批单，不能重复同意");
    }

    @Test
    void bindOrderShouldRejectWhenOrderAlreadyHasApprovedApproval() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalRequestEntity approved = approval(
                77L,
                3001L,
                1001L,
                2001L,
                "approved",
                "200.00"
        );
        when(approvalMapper.selectOne(any(Wrapper.class))).thenReturn(approved);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order(2001L, 1001L, 3001L));
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                null,
                true,
                approvalMapper,
                orderMapper
        );

        assertThatThrownBy(() -> service.bindOrder(1L, 78L, 2001L, 1001L))
                .isInstanceOf(BizException.class)
                .hasMessage("当前订单已有已通过的风控审批单，不能重复绑定");
    }

    @Test
    void applyShouldNotBindOrderWhenOrderCustomerDoesNotMatchApprovalCustomer() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order(2001L, 1001L, null));
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                credit("1000", "800", "100"),
                true,
                approvalMapper,
                orderMapper
        );
        doAnswer(invocation -> {
            CustomerRiskApprovalRequestEntity entity = invocation.getArgument(0);
            entity.setId(79L);
            return 1;
        }).when(approvalMapper).insert(any(CustomerRiskApprovalRequestEntity.class));

        service.apply(
                1L,
                new CustomerRiskApprovalApplyRequest(3001L, 1001L, 2001L, new BigDecimal("200.00"), "客户要求先确认"),
                "sales01"
        );

        ArgumentCaptor<CustomerRiskApprovalRequestEntity> captor = ArgumentCaptor.forClass(CustomerRiskApprovalRequestEntity.class);
        verify(approvalMapper).insert(captor.capture());
        assertThat(captor.getValue().getOrderId()).isNull();
    }

    @Test
    void bindOrderShouldRejectWhenOrderCustomerDoesNotMatchApprovalCustomer() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalRequestEntity approved = approval(
                77L,
                3001L,
                1001L,
                null,
                "approved",
                "200.00"
        );
        when(approvalMapper.selectOne(any(Wrapper.class))).thenReturn(approved);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order(2001L, 1001L, 4001L));
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                null,
                true,
                approvalMapper,
                orderMapper
        );

        assertThatThrownBy(() -> service.bindOrder(1L, 77L, 2001L, 1001L))
                .isInstanceOf(BizException.class)
                .hasMessage("审批单与订单客户不一致，不能绑定");
    }

    private CustomerRiskApprovalService service(
            CustomerUnitEntity customer,
            CustomerCreditAccountEntity credit,
            boolean approvalEnabled
    ) {
        return service(customer, credit, approvalEnabled, mock(CustomerRiskApprovalRequestMapper.class));
    }

    private CustomerRiskApprovalService service(
            CustomerUnitEntity customer,
            CustomerCreditAccountEntity credit,
            boolean approvalEnabled,
            CustomerRiskApprovalRequestMapper approvalMapper
    ) {
        return service(customer, credit, approvalEnabled, approvalMapper, mock(SalesBookingOrderMapper.class));
    }

    private CustomerRiskApprovalService service(
            CustomerUnitEntity customer,
            CustomerCreditAccountEntity credit,
            boolean approvalEnabled,
            CustomerRiskApprovalRequestMapper approvalMapper,
            SalesBookingOrderMapper orderMapper
    ) {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCreditAccountMapper creditMapper = mock(CustomerCreditAccountMapper.class);
        ContractMapper contractMapper = mock(ContractMapper.class);
        BusinessRiskConfigService configService = mock(BusinessRiskConfigService.class);
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(customer);
        when(creditMapper.selectOne(any(Wrapper.class))).thenReturn(credit);
        when(contractMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(configService.isCustomerRiskApprovalEnabled(1L)).thenReturn(approvalEnabled);
        return new CustomerRiskApprovalService(
                approvalMapper,
                customerMapper,
                creditMapper,
                contractMapper,
                orderMapper,
                configService,
                FIXED_CLOCK
        );
    }

    private CustomerUnitEntity customer(LocalDate contractExpireDate, String creditLimit) {
        CustomerUnitEntity customer = new CustomerUnitEntity();
        customer.setId(3001L);
        customer.setTenantId(1L);
        customer.setCustomerName("杭州百缘旅行社");
        customer.setContractExpireDate(contractExpireDate);
        customer.setCreditLimit(new BigDecimal(creditLimit));
        customer.setIsDeleted(false);
        customer.setStatus("active");
        return customer;
    }

    private CustomerCreditAccountEntity credit(String limit, String occupied, String pending) {
        CustomerCreditAccountEntity credit = new CustomerCreditAccountEntity();
        credit.setId(9001L);
        credit.setTenantId(1L);
        credit.setCustomerId(3001L);
        credit.setCreditLimit(new BigDecimal(limit));
        credit.setOccupiedAmount(new BigDecimal(occupied));
        credit.setPendingApprovalAmount(new BigDecimal(pending));
        credit.setStatus("active");
        credit.setIsDeleted(false);
        return credit;
    }

    private CustomerRiskApprovalRequestEntity approval(
            Long id,
            Long customerId,
            Long teamId,
            Long orderId,
            String status,
            String requestedAmount
    ) {
        CustomerRiskApprovalRequestEntity approval = new CustomerRiskApprovalRequestEntity();
        approval.setId(id);
        approval.setTenantId(1L);
        approval.setCustomerId(customerId);
        approval.setCustomerName("杭州百缘旅行社");
        approval.setTeamId(teamId);
        approval.setOrderId(orderId);
        approval.setRequestNo("RA-260626-00077");
        approval.setRequestedAmount(new BigDecimal(requestedAmount));
        approval.setRiskTypes("credit_over_limit");
        approval.setRiskSummary("授信超限");
        approval.setStatus(status);
        approval.setIsDeleted(false);
        return approval;
    }

    private SalesBookingOrderEntity order(Long id, Long teamId, Long customerId) {
        SalesBookingOrderEntity order = new SalesBookingOrderEntity();
        order.setId(id);
        order.setTenantId(1L);
        order.setTeamId(teamId);
        order.setCustomerId(customerId);
        order.setIsDeleted(false);
        return order;
    }
}
