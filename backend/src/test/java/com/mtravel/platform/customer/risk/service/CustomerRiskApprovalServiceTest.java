package com.mtravel.platform.customer.risk.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.contract.mapper.ContractMapper;
import com.mtravel.platform.customer.category.entity.CustomerCategoryApprovalMemberEntity;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryApprovalMemberMapper;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.customer.credit.entity.CustomerCreditAccountEntity;
import com.mtravel.platform.customer.credit.mapper.CustomerCreditAccountMapper;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalApplyRequest;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalCcEntity;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalRequestEntity;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalStepEntity;
import com.mtravel.platform.customer.risk.mapper.CustomerRiskApprovalCcMapper;
import com.mtravel.platform.customer.risk.mapper.CustomerRiskApprovalRequestMapper;
import com.mtravel.platform.customer.risk.mapper.CustomerRiskApprovalStepMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.system.config.service.BusinessRiskConfigService;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户风控审批服务测试。
 *
 * <p>收客页选择客户和提交订单都依赖这里判断合同到期、授信超限以及客户等级审批状态。
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
        WorkflowFixture workflow = workflowService(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                credit("1000", "800", "100"),
                true,
                approvalMapper,
                mock(SalesBookingOrderMapper.class),
                List.of(member("approver", 21L, 1), member("approver", 22L, 2), member("cc", 31L, 0)),
                List.of(user(21L, "boss01", "一级审批人", "boss"),
                        user(22L, "finance01", "二级审批人", "finance"),
                        user(31L, "cc01", "抄送人", "sales"))
        );
        doAnswer(invocation -> {
            CustomerRiskApprovalRequestEntity entity = invocation.getArgument(0);
            entity.setId(77L);
            return 1;
        }).when(approvalMapper).insert(any(CustomerRiskApprovalRequestEntity.class));
        when(approvalMapper.maxRequestNoSuffix(any(), any())).thenReturn(7);

        var response = workflow.service().apply(
                1L,
                new CustomerRiskApprovalApplyRequest(3001L, 1001L, null, new BigDecimal("200.00"), "客户要求先确认"),
                11L,
                "sales01"
        );

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.requestedAmount()).isEqualByComparingTo("200.00");
        ArgumentCaptor<CustomerRiskApprovalRequestEntity> captor = ArgumentCaptor.forClass(CustomerRiskApprovalRequestEntity.class);
        verify(approvalMapper).insert(captor.capture());
        assertThat(captor.getValue().getRiskTypes()).contains("credit_over_limit");
        assertThat(captor.getValue().getRequestNo()).isEqualTo("RA-260626-00008");
        assertThat(captor.getValue().getApplicantUserId()).isEqualTo(11L);
        assertThat(captor.getValue().getApplicant()).isEqualTo("sales01");
        assertThat(captor.getValue().getCategoryId()).isEqualTo(9L);
        assertThat(captor.getValue().getCategoryName()).isEqualTo("A类客户");
        assertThat(captor.getValue().getCreditTermDays()).isEqualTo(30);
        assertThat(captor.getValue().getCurrentApprovalStep()).isEqualTo(1);
        assertThat(captor.getValue().getAvailableAmount()).isEqualByComparingTo("100.00");
        ArgumentCaptor<CustomerRiskApprovalStepEntity> stepCaptor = ArgumentCaptor.forClass(CustomerRiskApprovalStepEntity.class);
        verify(workflow.stepMapper(), org.mockito.Mockito.times(2)).insert(stepCaptor.capture());
        assertThat(stepCaptor.getAllValues()).extracting(CustomerRiskApprovalStepEntity::getStepOrder)
                .containsExactly(1, 2);
        assertThat(stepCaptor.getAllValues()).extracting(CustomerRiskApprovalStepEntity::getApproverUserId)
                .containsExactly(21L, 22L);
        ArgumentCaptor<CustomerRiskApprovalCcEntity> ccCaptor = ArgumentCaptor.forClass(CustomerRiskApprovalCcEntity.class);
        verify(workflow.ccMapper()).insert(ccCaptor.capture());
        assertThat(ccCaptor.getValue().getCcUserId()).isEqualTo(31L);
        assertThat(ccCaptor.getValue().getVisibleAt()).isNull();
        verify(approvalMapper).lockRequestNoGeneration(1L, "RA-260626-");
        verify(approvalMapper).maxRequestNoSuffix(1L, "RA-260626-");
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
    void legacyApprovalShouldOnlyAllowBossOrAdmin() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalRequestEntity pending = approval(77L, 3001L, null, null, "pending", "200.00");
        pending.setApplicantUserId(11L);
        when(approvalMapper.selectForUpdate(1L, 77L)).thenReturn(pending);
        when(approvalMapper.update(any(CustomerRiskApprovalRequestEntity.class), any(UpdateWrapper.class))).thenReturn(1);
        WorkflowFixture workflow = workflowService(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                null,
                true,
                approvalMapper,
                mock(SalesBookingOrderMapper.class),
                List.of(),
                List.of()
        );
        when(workflow.stepMapper().selectCount(any(Wrapper.class))).thenReturn(0L);
        when(workflow.userMapper().selectOne(any(Wrapper.class)))
                .thenReturn(user(12L, "sales02", "普通员工", "sales"), user(21L, "boss01", "总经理", "boss"));

        assertThatThrownBy(() -> workflow.service().approve(1L, 77L, "同意", 12L, "sales02"))
                .isInstanceOf(BizException.class)
                .hasMessage("只有总经理或管理员可以处理风控审批");

        workflow.service().approve(1L, 77L, "同意", 21L, "boss01");

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
        pending.setApplicantUserId(11L);
        when(approvalMapper.selectForUpdate(1L, 78L)).thenReturn(pending);
        when(approvalMapper.selectOne(any(Wrapper.class))).thenReturn(approved);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order(2001L, 1001L, 3001L));
        WorkflowFixture workflow = workflowService(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                null,
                true,
                approvalMapper,
                orderMapper,
                List.of(),
                List.of(user(21L, "boss01", "总经理", "boss"))
        );
        when(workflow.stepMapper().selectCount(any(Wrapper.class))).thenReturn(0L);
        when(workflow.userMapper().selectOne(any(Wrapper.class))).thenReturn(user(21L, "boss01", "总经理", "boss"));

        assertThatThrownBy(() -> workflow.service().approve(1L, 78L, "同意", 21L, "boss01"))
                .isInstanceOf(BizException.class)
                .hasMessage("当前订单已有已通过的风控审批单，不能重复同意");
    }

    @Test
    void approveShouldAdvanceToNextSnapshotStepBeforeFinalApproval() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalStepMapper stepMapper = mock(CustomerRiskApprovalStepMapper.class);
        CustomerRiskApprovalCcMapper ccMapper = mock(CustomerRiskApprovalCcMapper.class);
        CustomerRiskApprovalRequestEntity pending = approval(80L, 3001L, null, null, "pending", "200.00");
        pending.setApplicantUserId(11L);
        pending.setCurrentApprovalStep(1);
        CustomerRiskApprovalStepEntity first = step(801L, 80L, 1, 21L, "pending");
        CustomerRiskApprovalStepEntity second = step(802L, 80L, 2, 22L, "pending");
        when(approvalMapper.selectForUpdate(1L, 80L)).thenReturn(pending);
        when(stepMapper.selectForUpdate(1L, 80L, 1)).thenReturn(first);
        when(stepMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
        when(stepMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(stepMapper.selectOne(any(Wrapper.class))).thenReturn(second);
        when(approvalMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        CustomerRiskApprovalService service = fullService(
                approvalMapper, stepMapper, ccMapper,
                user(21L, "boss01", "一级审批人", "boss"),
                mock(SalesBookingOrderMapper.class)
        );

        var response = service.approve(1L, 80L, "一级同意", 21L, "boss01");

        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.currentApprovalStep()).isEqualTo(2);
        verify(approvalMapper).update(any(CustomerRiskApprovalRequestEntity.class), any(UpdateWrapper.class));
        verify(ccMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void finalSnapshotApprovalShouldApproveRequestAndExposeCc() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalStepMapper stepMapper = mock(CustomerRiskApprovalStepMapper.class);
        CustomerRiskApprovalCcMapper ccMapper = mock(CustomerRiskApprovalCcMapper.class);
        CustomerRiskApprovalRequestEntity pending = approval(83L, 3001L, null, null, "pending", "200.00");
        pending.setApplicantUserId(11L);
        pending.setCurrentApprovalStep(2);
        CustomerRiskApprovalStepEntity second = step(832L, 83L, 2, 22L, "pending");
        when(approvalMapper.selectForUpdate(1L, 83L)).thenReturn(pending);
        when(stepMapper.selectForUpdate(1L, 83L, 2)).thenReturn(second);
        when(stepMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
        when(stepMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(stepMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(approvalMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        CustomerRiskApprovalService service = fullService(
                approvalMapper, stepMapper, ccMapper,
                user(22L, "finance01", "二级审批人", "finance"),
                mock(SalesBookingOrderMapper.class)
        );

        var response = service.approve(1L, 83L, "最终同意", 22L, "finance01");

        assertThat(response.status()).isEqualTo("approved");
        assertThat(response.approvedBy()).isEqualTo("finance01");
        verify(ccMapper).update(any(CustomerRiskApprovalCcEntity.class), any(UpdateWrapper.class));
    }

    @Test
    void rejectShouldCancelFollowingSnapshotSteps() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalStepMapper stepMapper = mock(CustomerRiskApprovalStepMapper.class);
        CustomerRiskApprovalCcMapper ccMapper = mock(CustomerRiskApprovalCcMapper.class);
        CustomerRiskApprovalRequestEntity pending = approval(81L, 3001L, null, null, "pending", "200.00");
        pending.setApplicantUserId(11L);
        pending.setCurrentApprovalStep(1);
        CustomerRiskApprovalStepEntity first = step(811L, 81L, 1, 21L, "pending");
        when(approvalMapper.selectForUpdate(1L, 81L)).thenReturn(pending);
        when(stepMapper.selectForUpdate(1L, 81L, 1)).thenReturn(first);
        when(stepMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
        when(stepMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(approvalMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        CustomerRiskApprovalService service = fullService(
                approvalMapper, stepMapper, ccMapper,
                user(21L, "boss01", "一级审批人", "boss"),
                mock(SalesBookingOrderMapper.class)
        );

        var response = service.reject(1L, 81L, "风险较高", 21L, "boss01");

        assertThat(response.status()).isEqualTo("rejected");
        verify(stepMapper, org.mockito.Mockito.times(2)).update(any(), any(UpdateWrapper.class));
        verify(approvalMapper).update(any(CustomerRiskApprovalRequestEntity.class), any(UpdateWrapper.class));
    }

    @Test
    void applicantCannotApproveOwnSnapshotStep() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalStepMapper stepMapper = mock(CustomerRiskApprovalStepMapper.class);
        CustomerRiskApprovalRequestEntity pending = approval(82L, 3001L, null, null, "pending", "200.00");
        pending.setApplicantUserId(21L);
        pending.setCurrentApprovalStep(1);
        when(approvalMapper.selectForUpdate(1L, 82L)).thenReturn(pending);
        CustomerRiskApprovalService service = fullService(
                approvalMapper, stepMapper, mock(CustomerRiskApprovalCcMapper.class),
                user(21L, "boss01", "总经理", "boss"), mock(SalesBookingOrderMapper.class)
        );

        assertThatThrownBy(() -> service.approve(1L, 82L, "自审", 21L, "boss01"))
                .isInstanceOf(BizException.class)
                .hasMessage("发起人不能审批自己的申请");
        verify(stepMapper, never()).selectForUpdate(any(), any(), any());
    }

    @Test
    void onlyConfiguredCurrentStepApproverCanApproveRegardlessOfRole() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalStepMapper stepMapper = mock(CustomerRiskApprovalStepMapper.class);
        CustomerRiskApprovalRequestEntity pending = approval(84L, 3001L, null, null, "pending", "200.00");
        pending.setApplicantUserId(11L);
        pending.setCurrentApprovalStep(1);
        CustomerRiskApprovalStepEntity configuredStep = step(841L, 84L, 1, 21L, "pending");
        when(approvalMapper.selectForUpdate(1L, 84L)).thenReturn(pending);
        when(stepMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(stepMapper.selectForUpdate(1L, 84L, 1)).thenReturn(configuredStep);
        CustomerRiskApprovalService service = fullService(
                approvalMapper,
                stepMapper,
                mock(CustomerRiskApprovalCcMapper.class),
                user(99L, "boss99", "未配置审批人", "boss"),
                mock(SalesBookingOrderMapper.class)
        );

        assertThatThrownBy(() -> service.approve(1L, 84L, "越权同意", 99L, "boss99"))
                .isInstanceOf(BizException.class)
                .hasMessage("只有当前步骤指定审批人可以处理");
        verify(stepMapper, never()).update(any(), any(UpdateWrapper.class));
        verify(approvalMapper, never()).update(any(CustomerRiskApprovalRequestEntity.class), any(UpdateWrapper.class));
    }

    @Test
    void pageViewsShouldUseAssignedStepApplicantAndVisibleCcFilters() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalStepMapper stepMapper = mock(CustomerRiskApprovalStepMapper.class);
        CustomerRiskApprovalCcMapper ccMapper = mock(CustomerRiskApprovalCcMapper.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCreditAccountMapper creditMapper = mock(CustomerCreditAccountMapper.class);
        ContractMapper contractMapper = mock(ContractMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerCategoryApprovalMemberMapper memberMapper = mock(CustomerCategoryApprovalMemberMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        BusinessRiskConfigService configService = mock(BusinessRiskConfigService.class);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user(21L, "boss01", "总经理", "boss"));
        when(approvalMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenReturn(new Page<CustomerRiskApprovalRequestEntity>(1, 20));
        CustomerRiskApprovalService service = new CustomerRiskApprovalService(
                approvalMapper, stepMapper, ccMapper, customerMapper, categoryMapper, memberMapper,
                userMapper, creditMapper, contractMapper, orderMapper, configService, FIXED_CLOCK
        );
        ArgumentCaptor<Wrapper<CustomerRiskApprovalRequestEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);

        service.page(1L, 21L, "to_approve", null, null, null, null, null, 1, 20);
        service.page(1L, 21L, "initiated", null, null, null, null, null, 1, 20);
        service.page(1L, 21L, "cc", null, null, null, null, null, 1, 20);

        verify(approvalMapper, org.mockito.Mockito.times(3)).selectPage(any(Page.class), captor.capture());
        List<String> sql = captor.getAllValues().stream().map(Wrapper::getCustomSqlSegment).toList();
        assertThat(sql.get(0)).contains("customer_risk_approval_steps", "approver_user_id", "current_approval_step", "status");
        assertThat(sql.get(1)).contains("applicant_user_id");
        assertThat(sql.get(2)).contains("customer_risk_approval_ccs", "visible_at");
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
    void assertOrderCanSaveShouldRejectBoundApprovalForNewOrder() {
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

        assertThatThrownBy(() -> service.assertOrderCanSave(
                1L, 3001L, 1001L, null, new BigDecimal("200.00"), 77L))
                .isInstanceOf(BizException.class)
                .hasMessage("客户风控审批单与当前订单不一致");
    }

    @Test
    void bindOrderShouldRejectRebindingApprovalToDifferentOrder() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        when(approvalMapper.selectOne(any(Wrapper.class))).thenReturn(approval(
                77L, 3001L, 1001L, 2001L, "approved", "200.00"));
        CustomerRiskApprovalService service = service(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                null,
                true,
                approvalMapper,
                mock(SalesBookingOrderMapper.class)
        );

        assertThatThrownBy(() -> service.bindOrder(1L, 77L, 2002L, 1001L))
                .isInstanceOf(BizException.class)
                .hasMessage("审批单已绑定其他订单，不能重复使用");
        verify(approvalMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void finalApprovalShouldTranslateConcurrentUniqueConflict() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        CustomerRiskApprovalStepMapper stepMapper = mock(CustomerRiskApprovalStepMapper.class);
        CustomerRiskApprovalCcMapper ccMapper = mock(CustomerRiskApprovalCcMapper.class);
        CustomerRiskApprovalRequestEntity pending = approval(85L, 3001L, null, 2001L, "pending", "200.00");
        pending.setApplicantUserId(11L);
        pending.setCurrentApprovalStep(1);
        CustomerRiskApprovalStepEntity currentStep = step(851L, 85L, 1, 21L, "pending");
        when(approvalMapper.selectForUpdate(1L, 85L)).thenReturn(pending);
        when(stepMapper.selectForUpdate(1L, 85L, 1)).thenReturn(currentStep);
        when(stepMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(stepMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(stepMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(approvalMapper.update(any(), any(UpdateWrapper.class)))
                .thenThrow(new DataIntegrityViolationException("unique index"));
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order(2001L, null, 3001L));
        CustomerRiskApprovalService service = fullService(
                approvalMapper, stepMapper, ccMapper,
                user(21L, "manager01", "审批人", "manager"), orderMapper
        );

        assertThatThrownBy(() -> service.approve(1L, 85L, "同意", 21L, "manager01"))
                .isInstanceOf(BizException.class)
                .hasMessage("当前订单已有已通过的风控审批单，不能重复同意");
        verify(ccMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void applyShouldNotBindOrderWhenOrderCustomerDoesNotMatchApprovalCustomer() {
        CustomerRiskApprovalRequestMapper approvalMapper = mock(CustomerRiskApprovalRequestMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order(2001L, 1001L, null));
        WorkflowFixture workflow = workflowService(
                customer(LocalDate.of(2027, 1, 1), "50000"),
                credit("1000", "800", "100"),
                true,
                approvalMapper,
                orderMapper,
                List.of(member("approver", 21L, 1)),
                List.of(user(21L, "boss01", "总经理", "boss"))
        );
        doAnswer(invocation -> {
            CustomerRiskApprovalRequestEntity entity = invocation.getArgument(0);
            entity.setId(79L);
            return 1;
        }).when(approvalMapper).insert(any(CustomerRiskApprovalRequestEntity.class));

        workflow.service().apply(
                1L,
                new CustomerRiskApprovalApplyRequest(3001L, 1001L, 2001L, new BigDecimal("200.00"), "客户要求先确认"),
                11L,
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

    private WorkflowFixture workflowService(
            CustomerUnitEntity customer,
            CustomerCreditAccountEntity credit,
            boolean approvalEnabled,
            CustomerRiskApprovalRequestMapper approvalMapper,
            SalesBookingOrderMapper orderMapper,
            List<CustomerCategoryApprovalMemberEntity> members,
            List<SystemUserEntity> users
    ) {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCreditAccountMapper creditMapper = mock(CustomerCreditAccountMapper.class);
        ContractMapper contractMapper = mock(ContractMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerCategoryApprovalMemberMapper memberMapper = mock(CustomerCategoryApprovalMemberMapper.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        CustomerRiskApprovalStepMapper stepMapper = mock(CustomerRiskApprovalStepMapper.class);
        CustomerRiskApprovalCcMapper ccMapper = mock(CustomerRiskApprovalCcMapper.class);
        BusinessRiskConfigService configService = mock(BusinessRiskConfigService.class);
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(customer);
        when(creditMapper.selectOne(any(Wrapper.class))).thenReturn(credit);
        when(contractMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        CustomerCategoryEntity category = new CustomerCategoryEntity();
        category.setId(9L);
        category.setTenantId(1L);
        category.setCategoryName("A类客户");
        category.setCreditTermDays(30);
        category.setAllowOverLimit(true);
        category.setIsDeleted(false);
        customer.setCategoryId(9L);
        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(category);
        when(memberMapper.selectList(any(Wrapper.class))).thenReturn(members);
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(users);
        when(configService.isCustomerRiskApprovalEnabled(1L)).thenReturn(approvalEnabled);
        return new WorkflowFixture(
                new CustomerRiskApprovalService(
                        approvalMapper,
                        stepMapper,
                        ccMapper,
                        customerMapper,
                        categoryMapper,
                        memberMapper,
                        userMapper,
                        creditMapper,
                        contractMapper,
                        orderMapper,
                        configService,
                        FIXED_CLOCK
                ),
                stepMapper,
                ccMapper,
                userMapper
        );
    }

    private CustomerRiskApprovalService fullService(
            CustomerRiskApprovalRequestMapper approvalMapper,
            CustomerRiskApprovalStepMapper stepMapper,
            CustomerRiskApprovalCcMapper ccMapper,
            SystemUserEntity currentUser,
            SalesBookingOrderMapper orderMapper
    ) {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCreditAccountMapper creditMapper = mock(CustomerCreditAccountMapper.class);
        ContractMapper contractMapper = mock(ContractMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerCategoryApprovalMemberMapper memberMapper = mock(CustomerCategoryApprovalMemberMapper.class);
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        BusinessRiskConfigService configService = mock(BusinessRiskConfigService.class);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(currentUser);
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(customer(LocalDate.of(2027, 1, 1), "50000"));
        when(contractMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(configService.isCustomerRiskApprovalEnabled(1L)).thenReturn(true);
        return new CustomerRiskApprovalService(
                approvalMapper, stepMapper, ccMapper, customerMapper, categoryMapper, memberMapper,
                userMapper, creditMapper, contractMapper, orderMapper, configService, FIXED_CLOCK
        );
    }

    private CustomerRiskApprovalStepEntity step(Long id, Long requestId, int order, Long approverId, String status) {
        CustomerRiskApprovalStepEntity step = new CustomerRiskApprovalStepEntity();
        step.setId(id);
        step.setTenantId(1L);
        step.setRequestId(requestId);
        step.setStepOrder(order);
        step.setApproverUserId(approverId);
        step.setApproverName("审批人" + order);
        step.setStatus(status);
        return step;
    }

    private CustomerCategoryApprovalMemberEntity member(String type, Long userId, int stepOrder) {
        CustomerCategoryApprovalMemberEntity member = new CustomerCategoryApprovalMemberEntity();
        member.setId((long) stepOrder + 100);
        member.setTenantId(1L);
        member.setCategoryId(9L);
        member.setMemberType(type);
        member.setSystemUserId(userId);
        member.setStepOrder(stepOrder);
        member.setIsDeleted(false);
        return member;
    }

    private SystemUserEntity user(Long id, String username, String realName, String roleCode) {
        SystemUserEntity user = new SystemUserEntity();
        user.setId(id);
        user.setTenantId(1L);
        user.setUsername(username);
        user.setRealName(realName);
        user.setRoleCode(roleCode);
        user.setStatus("active");
        user.setIsDeleted(false);
        return user;
    }

    private record WorkflowFixture(
            CustomerRiskApprovalService service,
            CustomerRiskApprovalStepMapper stepMapper,
            CustomerRiskApprovalCcMapper ccMapper,
            SystemUserMapper userMapper
    ) {
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
