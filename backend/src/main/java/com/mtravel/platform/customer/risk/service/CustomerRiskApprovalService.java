package com.mtravel.platform.customer.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.contract.entity.ContractEntity;
import com.mtravel.platform.contract.mapper.ContractMapper;
import com.mtravel.platform.customer.category.entity.CustomerCategoryApprovalMemberEntity;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryApprovalMemberMapper;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.customer.credit.entity.CustomerCreditAccountEntity;
import com.mtravel.platform.customer.credit.mapper.CustomerCreditAccountMapper;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalApplyRequest;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalCcResponse;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalResponse;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalStepResponse;
import com.mtravel.platform.customer.risk.dto.CustomerRiskCheckResponse;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalCcEntity;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalRequestEntity;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalStepEntity;
import com.mtravel.platform.customer.risk.enums.CustomerRiskApprovalStatus;
import com.mtravel.platform.customer.risk.enums.CustomerRiskApprovalView;
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
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 客户风控审批业务服务。
 *
 * <p>服务层统一处理客户合同到期、授信超限的风险计算、审批申请、审批状态流转和订单保存校验。
 * Controller 只传入当前租户和登录用户，不直接判断审批规则。</p>
 */
@Service
public class CustomerRiskApprovalService {

    private static final String RISK_CONTRACT_EXPIRED = "contract_expired";
    private static final String RISK_CREDIT_OVER_LIMIT = "credit_over_limit";
    private static final DateTimeFormatter REQUEST_NO_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final CustomerRiskApprovalRequestMapper approvalMapper;
    private final CustomerRiskApprovalStepMapper stepMapper;
    private final CustomerRiskApprovalCcMapper ccMapper;
    private final CustomerUnitMapper customerMapper;
    private final CustomerCategoryMapper categoryMapper;
    private final CustomerCategoryApprovalMemberMapper categoryMemberMapper;
    private final SystemUserMapper userMapper;
    private final CustomerCreditAccountMapper creditMapper;
    private final ContractMapper contractMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final BusinessRiskConfigService configService;
    private final Clock clock;

    @Autowired
    public CustomerRiskApprovalService(
            CustomerRiskApprovalRequestMapper approvalMapper,
            CustomerRiskApprovalStepMapper stepMapper,
            CustomerRiskApprovalCcMapper ccMapper,
            CustomerUnitMapper customerMapper,
            CustomerCategoryMapper categoryMapper,
            CustomerCategoryApprovalMemberMapper categoryMemberMapper,
            SystemUserMapper userMapper,
            CustomerCreditAccountMapper creditMapper,
            ContractMapper contractMapper,
            SalesBookingOrderMapper orderMapper,
            BusinessRiskConfigService configService
    ) {
        this(
                approvalMapper,
                stepMapper,
                ccMapper,
                customerMapper,
                categoryMapper,
                categoryMemberMapper,
                userMapper,
                creditMapper,
                contractMapper,
                orderMapper,
                configService,
                Clock.systemDefaultZone()
        );
    }

    CustomerRiskApprovalService(
            CustomerRiskApprovalRequestMapper approvalMapper,
            CustomerRiskApprovalStepMapper stepMapper,
            CustomerRiskApprovalCcMapper ccMapper,
            CustomerUnitMapper customerMapper,
            CustomerCategoryMapper categoryMapper,
            CustomerCategoryApprovalMemberMapper categoryMemberMapper,
            SystemUserMapper userMapper,
            CustomerCreditAccountMapper creditMapper,
            ContractMapper contractMapper,
            SalesBookingOrderMapper orderMapper,
            BusinessRiskConfigService configService,
            Clock clock
    ) {
        this.approvalMapper = approvalMapper;
        this.stepMapper = stepMapper;
        this.ccMapper = ccMapper;
        this.customerMapper = customerMapper;
        this.categoryMapper = categoryMapper;
        this.categoryMemberMapper = categoryMemberMapper;
        this.userMapper = userMapper;
        this.creditMapper = creditMapper;
        this.contractMapper = contractMapper;
        this.orderMapper = orderMapper;
        this.configService = configService;
        this.clock = clock;
    }

    /**
     * 兼容旧版单级风控服务测试的构造方式。
     *
     * <p>新的申请和审批测试必须使用完整依赖，该构造方式只支撑不触发新流程的风险校验和订单关联边界测试。</p>
     */
    CustomerRiskApprovalService(
            CustomerRiskApprovalRequestMapper approvalMapper,
            CustomerUnitMapper customerMapper,
            CustomerCreditAccountMapper creditMapper,
            ContractMapper contractMapper,
            SalesBookingOrderMapper orderMapper,
            BusinessRiskConfigService configService,
            Clock clock
    ) {
        this(
                approvalMapper,
                null,
                null,
                customerMapper,
                null,
                null,
                null,
                creditMapper,
                contractMapper,
                orderMapper,
                configService,
                clock
        );
    }

    /**
     * 校验客户合同和授信风险。
     *
     * @param tenantId 当前租户 ID
     * @param customerId 客户单位 ID
     * @param teamId 团队 ID，可为空
     * @param orderId 订单 ID，可为空
     * @param requestedAmount 本次订单预计应收金额
     * @return 风险判断和金额快照
     */
    public CustomerRiskCheckResponse check(
            Long tenantId,
            Long customerId,
            Long teamId,
            Long orderId,
            BigDecimal requestedAmount
    ) {
        CustomerUnitEntity customer = requireCustomer(tenantId, customerId);
        RiskSnapshot snapshot = buildRiskSnapshot(tenantId, customer, teamId, orderId, requestedAmount);
        CustomerRiskApprovalRequestEntity approval = findReusableApprovedApproval(
                tenantId,
                customerId,
                teamId,
                orderId,
                snapshot.requestedAmount()
        );
        return snapshot.toCheckResponse(configService.isCustomerRiskApprovalEnabled(tenantId), approval);
    }

    /**
     * 创建客户风控审批申请。
     *
     * <p>申请时重新计算风险并固化快照，避免审批页面显示的数据被后续客户资料修改覆盖。</p>
     */
    @Transactional
    public CustomerRiskApprovalResponse apply(
            Long tenantId,
            CustomerRiskApprovalApplyRequest request,
            Long applicantUserId,
            String operator
    ) {
        if (applicantUserId == null) {
            throw new BizException("未获取到发起人用户 ID，无法发起授信审批");
        }
        CustomerUnitEntity customer = requireCustomer(tenantId, request.customerId());
        Long bindableOrderId = resolveBindableOrderId(tenantId, request.orderId(), customer.getId(), request.teamId());
        RiskSnapshot snapshot = buildRiskSnapshot(
                tenantId,
                customer,
                request.teamId(),
                bindableOrderId,
                request.requestedAmount()
        );
        if (snapshot.riskTypes().isEmpty()) {
            throw new BizException("当前客户没有需要审批的合同或授信风险");
        }
        CustomerRiskApprovalRequestEntity approved = findReusableApprovedApproval(
                tenantId,
                customer.getId(),
                request.teamId(),
                bindableOrderId,
                snapshot.requestedAmount()
        );
        if (approved != null) {
            return CustomerRiskApprovalResponse.fromEntity(approved);
        }
        ApprovalConfiguration approvalConfiguration = requireApprovalConfiguration(
                tenantId,
                customer,
                snapshot.creditOverLimit(),
                applicantUserId
        );
        CustomerRiskApprovalRequestEntity entity = new CustomerRiskApprovalRequestEntity();
        entity.setTenantId(tenantId);
        entity.setCustomerId(customer.getId());
        entity.setCustomerName(customer.getCustomerName());
        entity.setTeamId(request.teamId());
        entity.setOrderId(bindableOrderId);
        entity.setRequestNo(nextRequestNo(tenantId));
        entity.setRequestedAmount(snapshot.requestedAmount());
        entity.setRiskTypes(String.join(",", snapshot.riskTypes()));
        entity.setRiskSummary(snapshot.riskSummary());
        entity.setContractExpireDate(snapshot.contractExpireDate());
        entity.setCreditLimit(snapshot.creditLimit());
        entity.setOccupiedAmount(snapshot.occupiedAmount());
        entity.setPendingApprovalAmount(snapshot.pendingApprovalAmount());
        entity.setAvailableAmount(snapshot.availableAmount());
        entity.setOverLimitAmount(snapshot.overLimitAmount());
        entity.setCategoryId(approvalConfiguration.category().getId());
        entity.setCategoryName(approvalConfiguration.category().getCategoryName());
        entity.setCreditTermDays(number(approvalConfiguration.category().getCreditTermDays()));
        // 审批配置的步骤序号由客户等级维护，首步不一定恰好为 1。
        entity.setCurrentApprovalStep(approvalConfiguration.approvers().getFirst().getStepOrder());
        entity.setStatus(CustomerRiskApprovalStatus.PENDING.value());
        entity.setApplicantUserId(applicantUserId);
        entity.setApplicant(operator);
        entity.setCreatedBy(operator);
        entity.setRemark(clean(request.remark()));
        entity.setIsDeleted(false);
        approvalMapper.insert(entity);
        createWorkflowSnapshots(tenantId, entity.getId(), approvalConfiguration, operator);
        return response(entity, applicantUserId);
    }

    /**
     * 兼容旧内部调用。旧调用没有用户 ID，无法安全发起指定人员审批。
     */
    @Deprecated
    public CustomerRiskApprovalResponse apply(
            Long tenantId,
            CustomerRiskApprovalApplyRequest request,
            String operator
    ) {
        throw new BizException("未获取到发起人用户 ID，无法发起授信审批");
    }

    /** 兼容旧内部调用，新审批必须按指定用户 ID 校验。 */
    @Deprecated
    public CustomerRiskApprovalResponse approve(
            Long tenantId,
            Long id,
            String approvalRemark,
            String operator,
            List<String> roles
    ) {
        throw new BizException("未获取到审批人用户 ID，无法处理授信审批");
    }

    /** 分页查询客户风控审批申请。 */
    public PageResult<CustomerRiskApprovalResponse> page(
            Long tenantId,
            Long userId,
            String view,
            String keyword,
            String status,
            Long customerId,
            Long teamId,
            Long orderId,
            long page,
            long pageSize
    ) {
        CustomerRiskApprovalView approvalView = CustomerRiskApprovalView.fromValue(view);
        boolean legacyApprovalOperator = approvalView == CustomerRiskApprovalView.TO_APPROVE
                && isLegacyApprovalOperator(tenantId, userId);
        QueryWrapper<CustomerRiskApprovalRequestEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(customerId != null, "customer_id", customerId)
                .eq(teamId != null, "team_id", teamId)
                .eq(orderId != null, "order_id", orderId)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("customer_name", keyword)
                        .or()
                        .like("request_no", keyword))
                .orderByDesc("created_at")
                .orderByDesc("id");
        applyViewFilter(wrapper, approvalView, tenantId, userId, legacyApprovalOperator);
        Page<CustomerRiskApprovalRequestEntity> result = approvalMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResult<>(
                responses(result.getRecords(), userId, legacyApprovalOperator),
                result.getTotal()
        );
    }

    /** 查询审批申请详情。 */
    public CustomerRiskApprovalResponse detail(Long tenantId, Long id, Long userId) {
        CustomerRiskApprovalRequestEntity approval = requireApproval(tenantId, id);
        boolean legacyApprovalOperator = !hasWorkflowSteps(tenantId, approval.getId())
                && isLegacyApprovalOperator(tenantId, userId);
        if (!isVisibleToUser(tenantId, approval, userId, legacyApprovalOperator)) {
            throw new BizException("无权查看该客户授信超额审批");
        }
        return response(approval, userId, legacyApprovalOperator);
    }

    /**
     * 查询订单已关联的最新通过审批单。
     *
     * <p>用于修改订单页面回填审批单 ID，避免已有风险订单在再次保存时因为前端缺少审批单 ID 被误拦截。</p>
     */
    public CustomerRiskApprovalResponse latestApprovedForOrder(Long tenantId, Long customerId, Long teamId, Long orderId) {
        if (orderId == null) {
            return null;
        }
        CustomerRiskApprovalRequestEntity approval = findReusableApprovedApproval(
                tenantId,
                customerId,
                teamId,
                orderId,
                null
        );
        return approval == null ? null : CustomerRiskApprovalResponse.fromEntity(approval);
    }

    /** 当前指定审批人同意申请，最后一级通过后主单才生效。 */
    @Transactional
    public CustomerRiskApprovalResponse approve(
            Long tenantId,
            Long id,
            String approvalRemark,
            Long userId,
            String operator
    ) {
        if (userId == null) {
            throw new BizException("未获取到审批人用户 ID，无法处理授信审批");
        }
        CustomerRiskApprovalRequestEntity current = requireApprovalForUpdate(tenantId, id);
        assertPending(current);
        CustomerRiskApprovalStepEntity currentStep = requireCurrentStepForUpdate(tenantId, current, userId);
        assertApprovalMatchesBoundOrder(tenantId, current, "审批单与订单客户不一致，不能同意");
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (currentStep != null) {
            updateStepDecision(currentStep, CustomerRiskApprovalStatus.APPROVED.value(), approvalRemark, now);
        }

        CustomerRiskApprovalStepEntity nextStep = currentStep == null
                ? null
                : findNextStep(tenantId, id, currentStep.getStepOrder());
        CustomerRiskApprovalRequestEntity update = new CustomerRiskApprovalRequestEntity();
        if (nextStep != null) {
            update.setCurrentApprovalStep(nextStep.getStepOrder());
            assertRequestUpdated(approvalMapper.update(update, baseUpdate(tenantId)
                    .eq("id", id)
                    .eq("status", CustomerRiskApprovalStatus.PENDING.value())
                    .eq("current_approval_step", currentStep.getStepOrder())), "审批单已被处理，请刷新后重试");
            current.setCurrentApprovalStep(nextStep.getStepOrder());
        } else {
            assertNoOtherApprovedApprovalForOrder(
                    tenantId,
                    current.getOrderId(),
                    current.getId(),
                    "当前订单已有已通过的风控审批单，不能重复同意"
            );
            update.setStatus(CustomerRiskApprovalStatus.APPROVED.value());
            update.setApprovedBy(operator);
            update.setApprovedAt(now);
            update.setApprovalRemark(clean(approvalRemark));
            UpdateWrapper<CustomerRiskApprovalRequestEntity> wrapper = baseUpdate(tenantId)
                    .eq("id", id)
                    .eq("status", CustomerRiskApprovalStatus.PENDING.value());
            if (currentStep != null) {
                wrapper.eq("current_approval_step", currentStep.getStepOrder());
            }
            try {
                assertRequestUpdated(approvalMapper.update(update, wrapper), "审批单已被处理，请刷新后重试");
            } catch (DataIntegrityViolationException exception) {
                // 数据库部分唯一索引是并发终态的最终防线，这里转成可读的业务提示。
                throw new BizException("当前订单已有已通过的风控审批单，不能重复同意");
            }
            markCcVisible(tenantId, id, now);
            current.setStatus(update.getStatus());
            current.setApprovedBy(update.getApprovedBy());
            current.setApprovedAt(update.getApprovedAt());
            current.setApprovalRemark(update.getApprovalRemark());
        }
        return response(current, userId, currentStep == null);
    }

    /** 当前指定审批人拒绝申请，后续未处理步骤同时取消。 */
    @Transactional
    public CustomerRiskApprovalResponse reject(
            Long tenantId,
            Long id,
            String approvalRemark,
            Long userId,
            String operator
    ) {
        if (userId == null) {
            throw new BizException("未获取到审批人用户 ID，无法处理授信审批");
        }
        CustomerRiskApprovalRequestEntity current = requireApprovalForUpdate(tenantId, id);
        assertPending(current);
        CustomerRiskApprovalStepEntity currentStep = requireCurrentStepForUpdate(tenantId, current, userId);
        assertApprovalMatchesBoundOrder(tenantId, current, "审批单与订单客户不一致，不能拒绝");
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (currentStep != null) {
            updateStepDecision(currentStep, CustomerRiskApprovalStatus.REJECTED.value(), approvalRemark, now);
            cancelFollowingSteps(tenantId, id, currentStep.getStepOrder());
        }
        CustomerRiskApprovalRequestEntity update = new CustomerRiskApprovalRequestEntity();
        update.setStatus(CustomerRiskApprovalStatus.REJECTED.value());
        update.setRejectedBy(operator);
        update.setRejectedAt(now);
        update.setApprovalRemark(clean(approvalRemark));
        UpdateWrapper<CustomerRiskApprovalRequestEntity> wrapper = baseUpdate(tenantId)
                .eq("id", id)
                .eq("status", CustomerRiskApprovalStatus.PENDING.value());
        if (currentStep != null) {
            wrapper.eq("current_approval_step", currentStep.getStepOrder());
        }
        assertRequestUpdated(approvalMapper.update(update, wrapper), "审批单已被处理，请刷新后重试");
        current.setStatus(update.getStatus());
        current.setRejectedBy(update.getRejectedBy());
        current.setRejectedAt(update.getRejectedAt());
        current.setApprovalRemark(update.getApprovalRemark());
        return response(current, userId, currentStep == null);
    }

    /**
     * 校验订单保存是否具备风控审批授权。
     *
     * <p>配置关闭时仅提醒不阻断；配置开启且存在风险时，必须传入已通过、客户/团队/金额匹配的审批单。</p>
     */
    public void assertOrderCanSave(
            Long tenantId,
            Long customerId,
            Long teamId,
            Long orderId,
            BigDecimal requestedAmount,
            Long riskApprovalRequestId
    ) {
        if (customerId == null || !configService.isCustomerRiskApprovalEnabled(tenantId)) {
            return;
        }
        CustomerRiskCheckResponse risk = check(tenantId, customerId, teamId, orderId, requestedAmount);
        if (!risk.blocked()) {
            return;
        }
        if (riskApprovalRequestId == null) {
            throw new BizException("客户合同或授信风险需要客户等级指定审批人审批通过后才能提交订单");
        }
        CustomerRiskApprovalRequestEntity approval = requireApproval(tenantId, riskApprovalRequestId);
        if (!CustomerRiskApprovalStatus.APPROVED.value().equals(approval.getStatus())) {
            throw new BizException("客户风控审批尚未通过，不能提交订单");
        }
        if (!Objects.equals(approval.getCustomerId(), customerId)) {
            throw new BizException("客户风控审批单与当前客户不一致");
        }
        if (approval.getTeamId() != null && teamId != null && !Objects.equals(approval.getTeamId(), teamId)) {
            throw new BizException("客户风控审批单与当前团队不一致");
        }
        // 已绑定的授信审批只对原订单有效；新订单 orderId 为空时也不能复用。
        if (approval.getOrderId() != null && !Objects.equals(approval.getOrderId(), orderId)) {
            throw new BizException("客户风控审批单与当前订单不一致");
        }
        if (money(approval.getRequestedAmount()).compareTo(money(requestedAmount)) < 0) {
            throw new BizException("客户风控审批金额小于当前订单金额，请重新申请审批");
        }
    }

    /** 新订单保存成功后回填审批单订单 ID，便于后续审批页和订单页互相追溯。 */
    public void bindOrder(Long tenantId, Long riskApprovalRequestId, Long orderId, Long teamId) {
        if (riskApprovalRequestId == null || orderId == null) {
            return;
        }
        CustomerRiskApprovalRequestEntity current = requireApproval(tenantId, riskApprovalRequestId);
        if (current.getOrderId() != null && !Objects.equals(current.getOrderId(), orderId)) {
            throw new BizException("审批单已绑定其他订单，不能重复使用");
        }
        assertApprovalMatchesOrder(tenantId, current, orderId, teamId, "审批单与订单客户不一致，不能绑定");
        assertNoOtherApprovedApprovalForOrder(
                tenantId,
                orderId,
                riskApprovalRequestId,
                "当前订单已有已通过的风控审批单，不能重复绑定"
        );
        CustomerRiskApprovalRequestEntity update = new CustomerRiskApprovalRequestEntity();
        update.setOrderId(orderId);
        update.setTeamId(teamId);
        try {
            assertRequestUpdated(approvalMapper.update(update, baseUpdate(tenantId)
                    .eq("id", riskApprovalRequestId)
                    .and(boundary -> boundary.isNull("order_id").or().eq("order_id", orderId))),
                    "审批单已绑定其他订单，不能重复使用");
        } catch (DataIntegrityViolationException exception) {
            throw new BizException("当前订单已有已通过的风控审批单，不能重复绑定");
        }
    }

    private RiskSnapshot buildRiskSnapshot(
            Long tenantId,
            CustomerUnitEntity customer,
            Long teamId,
            Long orderId,
            BigDecimal requestedAmount
    ) {
        BigDecimal amount = money(requestedAmount);
        LocalDate today = LocalDate.now(clock);
        LocalDate contractExpireDate = resolveContractExpireDate(tenantId, customer);
        boolean contractExpired = contractExpireDate != null && contractExpireDate.isBefore(today);

        CreditSnapshot credit = resolveCreditSnapshot(tenantId, customer);
        boolean creditOverLimit = amount.compareTo(credit.availableAmount()) > 0;
        BigDecimal overLimitAmount = creditOverLimit
                ? amount.subtract(credit.availableAmount()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<String> riskTypes = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        if (contractExpired) {
            riskTypes.add(RISK_CONTRACT_EXPIRED);
            summaries.add("合同已于 %s 到期".formatted(contractExpireDate));
        }
        if (creditOverLimit) {
            riskTypes.add(RISK_CREDIT_OVER_LIMIT);
            summaries.add("授信可用额度 %s 元，本次订单 %s 元，超限 %s 元".formatted(
                    moneyText(credit.availableAmount()),
                    moneyText(amount),
                    moneyText(overLimitAmount)
            ));
        }

        return new RiskSnapshot(
                customer.getId(),
                customer.getCustomerName(),
                teamId,
                orderId,
                amount,
                riskTypes,
                summaries.isEmpty() ? "当前客户合同和授信额度正常" : String.join("；", summaries),
                contractExpired,
                contractExpireDate,
                creditOverLimit,
                credit.creditLimit(),
                credit.occupiedAmount(),
                credit.pendingApprovalAmount(),
                credit.availableAmount(),
                overLimitAmount
        );
    }

    private LocalDate resolveContractExpireDate(Long tenantId, CustomerUnitEntity customer) {
        List<ContractEntity> contracts = contractMapper.selectList(new QueryWrapper<ContractEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("contract_type", "customer")
                .eq("customer_id", customer.getId())
                .eq("status", "active")
                .orderByDesc("end_date")
                .orderByDesc("id"));
        if (contracts == null || contracts.isEmpty()) {
            return customer.getContractExpireDate();
        }
        boolean hasOpenEndedContract = contracts.stream().anyMatch(contract -> contract.getEndDate() == null);
        if (hasOpenEndedContract) {
            return null;
        }
        return contracts.stream()
                .map(ContractEntity::getEndDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(customer.getContractExpireDate());
    }

    private CreditSnapshot resolveCreditSnapshot(Long tenantId, CustomerUnitEntity customer) {
        CustomerCreditAccountEntity account = creditMapper.selectOne(new QueryWrapper<CustomerCreditAccountEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("customer_id", customer.getId())
                .eq("status", "active")
                .last("limit 1"));
        if (account != null) {
            BigDecimal creditLimit = money(account.getCreditLimit());
            BigDecimal occupied = money(account.getOccupiedAmount());
            BigDecimal pending = money(account.getPendingApprovalAmount());
            BigDecimal available = account.getAvailableAmount() == null
                    ? creditLimit.subtract(occupied).subtract(pending)
                    : account.getAvailableAmount();
            return new CreditSnapshot(
                    creditLimit,
                    occupied,
                    pending,
                    available.setScale(2, RoundingMode.HALF_UP)
            );
        }
        BigDecimal creditLimit = money(customer.getCreditLimit());
        return new CreditSnapshot(
                creditLimit,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                creditLimit.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private CustomerUnitEntity requireCustomer(Long tenantId, Long customerId) {
        CustomerUnitEntity customer = customerMapper.selectOne(new QueryWrapper<CustomerUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", customerId));
        if (customer == null) {
            throw new BizException("客户单位不存在或已删除");
        }
        return customer;
    }

    /**
     * 读取客户当前等级的授信超额审批配置。
     *
     * <p>当前流程用于授信超额。如果只有合同到期风险，也复用该等级的指定审批链，
     * 避免在新旧审批模型之间留下无人可审的申请。</p>
     */
    private ApprovalConfiguration requireApprovalConfiguration(
            Long tenantId,
            CustomerUnitEntity customer,
            boolean creditOverLimit,
            Long applicantUserId
    ) {
        if (customer.getCategoryId() == null) {
            throw new BizException("客户未配置客户等级，无法发起授信审批");
        }
        CustomerCategoryEntity category = categoryMapper.selectOne(
                new QueryWrapper<CustomerCategoryEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("id", customer.getCategoryId())
                        .eq("is_deleted", false)
        );
        if (category == null) {
            throw new BizException("客户等级不存在或已删除，无法发起授信审批");
        }
        if (creditOverLimit && !Boolean.TRUE.equals(category.getAllowOverLimit())) {
            throw new BizException("该客户等级不允许授信超额，不能发起审批");
        }
        List<CustomerCategoryApprovalMemberEntity> members = Objects.requireNonNullElse(
                categoryMemberMapper.selectList(new QueryWrapper<CustomerCategoryApprovalMemberEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("category_id", category.getId())
                        .eq("is_deleted", false)
                        .orderByAsc("member_type")
                        .orderByAsc("step_order")
                        .orderByAsc("id")),
                List.of()
        );
        List<CustomerCategoryApprovalMemberEntity> approvers = members.stream()
                .filter(member -> "approver".equals(member.getMemberType()))
                .sorted(java.util.Comparator.comparing(CustomerCategoryApprovalMemberEntity::getStepOrder))
                .toList();
        if (approvers.isEmpty()) {
            throw new BizException("该客户等级未配置授信超额审批人");
        }
        if (approvers.stream().anyMatch(member -> Objects.equals(member.getSystemUserId(), applicantUserId))) {
            throw new BizException("发起人不能同时作为本流程审批人");
        }
        Set<Long> userIds = members.stream()
                .map(CustomerCategoryApprovalMemberEntity::getSystemUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<SystemUserEntity> activeUsers = Objects.requireNonNullElse(userMapper.selectList(new QueryWrapper<SystemUserEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("status", "active")
                        .in("id", userIds)), List.of());
        Map<Long, SystemUserEntity> users = activeUsers.stream()
                .collect(Collectors.toMap(SystemUserEntity::getId, Function.identity()));
        if (users.size() != userIds.size()) {
            throw new BizException("审批人或抄送人已停用，请先更新客户等级授信配置");
        }
        List<CustomerCategoryApprovalMemberEntity> ccUsers = members.stream()
                .filter(member -> "cc".equals(member.getMemberType()))
                .toList();
        return new ApprovalConfiguration(category, approvers, ccUsers, users);
    }

    /** 保存审批人和抄送人快照，使历史流程不受等级配置变更影响。 */
    private void createWorkflowSnapshots(
            Long tenantId,
            Long requestId,
            ApprovalConfiguration configuration,
            String operator
    ) {
        for (CustomerCategoryApprovalMemberEntity member : configuration.approvers()) {
            SystemUserEntity user = configuration.users().get(member.getSystemUserId());
            CustomerRiskApprovalStepEntity step = new CustomerRiskApprovalStepEntity();
            step.setTenantId(tenantId);
            step.setRequestId(requestId);
            step.setStepOrder(member.getStepOrder());
            step.setApproverUserId(member.getSystemUserId());
            step.setApproverName(displayName(user));
            step.setStatus(CustomerRiskApprovalStatus.PENDING.value());
            stepMapper.insert(step);
        }
        for (CustomerCategoryApprovalMemberEntity member : configuration.ccUsers()) {
            SystemUserEntity user = configuration.users().get(member.getSystemUserId());
            CustomerRiskApprovalCcEntity cc = new CustomerRiskApprovalCcEntity();
            cc.setTenantId(tenantId);
            cc.setRequestId(requestId);
            cc.setCcUserId(member.getSystemUserId());
            cc.setCcName(displayName(user));
            ccMapper.insert(cc);
        }
    }

    private String displayName(SystemUserEntity user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private CustomerRiskApprovalRequestEntity requireApproval(Long tenantId, Long id) {
        CustomerRiskApprovalRequestEntity approval = approvalMapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (approval == null) {
            throw new BizException("客户风控审批申请不存在或已删除");
        }
        return approval;
    }

    private CustomerRiskApprovalRequestEntity requireApprovalForUpdate(Long tenantId, Long id) {
        CustomerRiskApprovalRequestEntity approval = approvalMapper.selectForUpdate(tenantId, id);
        if (approval == null) {
            throw new BizException("客户风控审批申请不存在或已删除");
        }
        return approval;
    }

    private CustomerRiskApprovalStepEntity requireCurrentStepForUpdate(
            Long tenantId,
            CustomerRiskApprovalRequestEntity approval,
            Long userId
    ) {
        if (Objects.equals(approval.getApplicantUserId(), userId)) {
            throw new BizException("发起人不能审批自己的申请");
        }
        // 旧审批单没有步骤快照，只允许仍在职的总经理、管理员或租户管理员兜底处理。
        if (!hasWorkflowSteps(tenantId, approval.getId())) {
            assertLegacyApprovalOperator(tenantId, userId);
            return null;
        }
        CustomerRiskApprovalStepEntity step = stepMapper.selectForUpdate(
                tenantId,
                approval.getId(),
                number(approval.getCurrentApprovalStep())
        );
        if (step == null || !CustomerRiskApprovalStatus.PENDING.value().equals(step.getStatus())) {
            throw new BizException("当前审批步骤不存在或已处理");
        }
        if (!Objects.equals(step.getApproverUserId(), userId)) {
            throw new BizException("只有当前步骤指定审批人可以处理");
        }
        return step;
    }

    private boolean hasWorkflowSteps(Long tenantId, Long requestId) {
        if (stepMapper == null) {
            return false;
        }
        Long count = stepMapper.selectCount(new QueryWrapper<CustomerRiskApprovalStepEntity>()
                .eq("tenant_id", tenantId)
                .eq("request_id", requestId));
        return count != null && count > 0;
    }

    private void assertLegacyApprovalOperator(Long tenantId, Long userId) {
        if (!isLegacyApprovalOperator(tenantId, userId)) {
            throw new BizException("只有总经理或管理员可以处理风控审批");
        }
    }

    private boolean isLegacyApprovalOperator(Long tenantId, Long userId) {
        if (userId == null || userMapper == null) {
            return false;
        }
        SystemUserEntity user = userMapper.selectOne(new QueryWrapper<SystemUserEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", userId)
                .eq("is_deleted", false)
                .eq("status", "active"));
        return user != null && (Boolean.TRUE.equals(user.getIsTenantAdmin())
                || "admin".equals(user.getRoleCode())
                || "boss".equals(user.getRoleCode()));
    }

    private void updateStepDecision(
            CustomerRiskApprovalStepEntity step,
            String status,
            String decisionRemark,
            OffsetDateTime decidedAt
    ) {
        CustomerRiskApprovalStepEntity update = new CustomerRiskApprovalStepEntity();
        update.setStatus(status);
        update.setDecidedAt(decidedAt);
        update.setDecisionRemark(clean(decisionRemark));
        int updated = stepMapper.update(update, new UpdateWrapper<CustomerRiskApprovalStepEntity>()
                .eq("tenant_id", step.getTenantId())
                .eq("id", step.getId())
                .eq("status", CustomerRiskApprovalStatus.PENDING.value()));
        if (updated != 1) {
            throw new BizException("当前审批步骤已被处理，请刷新后重试");
        }
    }

    private void assertRequestUpdated(int updated, String message) {
        if (updated != 1) {
            throw new BizException(message);
        }
    }

    private CustomerRiskApprovalStepEntity findNextStep(Long tenantId, Long requestId, Integer currentStep) {
        return stepMapper.selectOne(new QueryWrapper<CustomerRiskApprovalStepEntity>()
                .eq("tenant_id", tenantId)
                .eq("request_id", requestId)
                .gt("step_order", currentStep)
                .eq("status", CustomerRiskApprovalStatus.PENDING.value())
                .orderByAsc("step_order")
                .last("limit 1"));
    }

    private void cancelFollowingSteps(Long tenantId, Long requestId, Integer currentStep) {
        if (stepMapper == null) {
            return;
        }
        CustomerRiskApprovalStepEntity update = new CustomerRiskApprovalStepEntity();
        update.setStatus(CustomerRiskApprovalStatus.CANCELLED.value());
        stepMapper.update(update, new UpdateWrapper<CustomerRiskApprovalStepEntity>()
                .eq("tenant_id", tenantId)
                .eq("request_id", requestId)
                .gt("step_order", currentStep)
                .eq("status", CustomerRiskApprovalStatus.PENDING.value()));
    }

    private void markCcVisible(Long tenantId, Long requestId, OffsetDateTime visibleAt) {
        if (ccMapper == null) {
            return;
        }
        CustomerRiskApprovalCcEntity update = new CustomerRiskApprovalCcEntity();
        update.setVisibleAt(visibleAt);
        ccMapper.update(update, new UpdateWrapper<CustomerRiskApprovalCcEntity>()
                .eq("tenant_id", tenantId)
                .eq("request_id", requestId)
                .isNull("visible_at"));
    }

    private Long resolveBindableOrderId(Long tenantId, Long orderId, Long customerId, Long teamId) {
        if (orderId == null) {
            return null;
        }
        SalesBookingOrderEntity order = requireOrder(tenantId, orderId);
        if (teamId != null && order.getTeamId() != null && !Objects.equals(order.getTeamId(), teamId)) {
            throw new BizException("审批申请与当前订单团队不一致");
        }
        return Objects.equals(order.getCustomerId(), customerId) ? orderId : null;
    }

    private void assertApprovalMatchesBoundOrder(
            Long tenantId,
            CustomerRiskApprovalRequestEntity approval,
            String message
    ) {
        if (approval.getOrderId() == null) {
            return;
        }
        assertApprovalMatchesOrder(tenantId, approval, approval.getOrderId(), approval.getTeamId(), message);
    }

    private void assertApprovalMatchesOrder(
            Long tenantId,
            CustomerRiskApprovalRequestEntity approval,
            Long orderId,
            Long teamId,
            String message
    ) {
        SalesBookingOrderEntity order = requireOrder(tenantId, orderId);
        if (teamId != null && order.getTeamId() != null && !Objects.equals(order.getTeamId(), teamId)) {
            throw new BizException("审批单与订单团队不一致，不能绑定");
        }
        if (!Objects.equals(order.getCustomerId(), approval.getCustomerId())) {
            throw new BizException(message);
        }
    }

    private SalesBookingOrderEntity requireOrder(Long tenantId, Long orderId) {
        SalesBookingOrderEntity order = orderMapper.selectOne(new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", orderId));
        if (order == null) {
            throw new BizException("订单不存在或已删除");
        }
        return order;
    }

    private void assertNoOtherApprovedApprovalForOrder(
            Long tenantId,
            Long orderId,
            Long currentApprovalId,
            String message
    ) {
        if (orderId == null) {
            return;
        }
        CustomerRiskApprovalRequestEntity existing = approvalMapper.selectOne(baseQuery(tenantId)
                .eq("order_id", orderId)
                .eq("status", CustomerRiskApprovalStatus.APPROVED.value())
                .ne(currentApprovalId != null, "id", currentApprovalId)
                .last("limit 1"));
        if (existing != null) {
            throw new BizException(message);
        }
    }

    private CustomerRiskApprovalRequestEntity findReusableApprovedApproval(
            Long tenantId,
            Long customerId,
            Long teamId,
            Long orderId,
            BigDecimal requestedAmount
    ) {
        if (customerId == null) {
            return null;
        }
        if (orderId != null) {
            CustomerRiskApprovalRequestEntity boundApproval = approvalMapper.selectOne(reusableApprovalQuery(
                    tenantId,
                    customerId,
                    teamId,
                    orderId,
                    requestedAmount
            ));
            if (boundApproval != null) {
                return boundApproval;
            }
        }
        return approvalMapper.selectOne(reusableApprovalQuery(
                tenantId,
                customerId,
                teamId,
                null,
                requestedAmount
        ));
    }

    private QueryWrapper<CustomerRiskApprovalRequestEntity> reusableApprovalQuery(
            Long tenantId,
            Long customerId,
            Long teamId,
            Long orderId,
            BigDecimal requestedAmount
    ) {
        return baseQuery(tenantId)
                .eq("customer_id", customerId)
                .eq(teamId != null, "team_id", teamId)
                .eq(orderId != null, "order_id", orderId)
                .isNull(orderId == null, "order_id")
                .eq("status", CustomerRiskApprovalStatus.APPROVED.value())
                .ge(requestedAmount != null, "requested_amount", money(requestedAmount))
                .orderByDesc("approved_at")
                .orderByDesc("id")
                .last("limit 1");
    }

    private void assertPending(CustomerRiskApprovalRequestEntity approval) {
        if (!CustomerRiskApprovalStatus.PENDING.value().equals(approval.getStatus())) {
            throw new BizException("只有待审批申请可以处理");
        }
    }

    private void applyViewFilter(
            QueryWrapper<CustomerRiskApprovalRequestEntity> wrapper,
            CustomerRiskApprovalView view,
            Long tenantId,
            Long userId,
            boolean legacyApprovalOperator
    ) {
        switch (view) {
            case TO_APPROVE -> {
                // 待办只看当前步骤，历史无步骤单仅对总经理/管理员开放兜底处理。
                wrapper.eq("status", CustomerRiskApprovalStatus.PENDING.value())
                        .and(nested -> nested.isNull("applicant_user_id").or().ne("applicant_user_id", userId));
                String assignedCurrentStep = "EXISTS (SELECT 1 FROM customer_risk_approval_steps s "
                        + "WHERE s.tenant_id = {0} "
                        + "AND s.request_id = customer_risk_approval_requests.id "
                        + "AND s.step_order = customer_risk_approval_requests.current_approval_step "
                        + "AND s.approver_user_id = {1} "
                        + "AND s.status = 'pending')";
                if (legacyApprovalOperator) {
                    wrapper.and(nested -> nested.apply(assignedCurrentStep, tenantId, userId)
                            .or()
                            .apply("NOT EXISTS (SELECT 1 FROM customer_risk_approval_steps legacy "
                                            + "WHERE legacy.tenant_id = {0} "
                                            + "AND legacy.request_id = customer_risk_approval_requests.id)",
                                    tenantId));
                } else {
                    wrapper.apply(assignedCurrentStep, tenantId, userId);
                }
            }
            case INITIATED -> wrapper.eq("applicant_user_id", userId);
            // 抄送在整张审批单最终通过、visible_at 写入后才对抄送人可见。
            case CC -> wrapper.apply(
                    "EXISTS (SELECT 1 FROM customer_risk_approval_ccs c "
                            + "WHERE c.tenant_id = {0} "
                            + "AND c.request_id = customer_risk_approval_requests.id "
                            + "AND c.cc_user_id = {1} "
                            + "AND c.visible_at IS NOT NULL)",
                    tenantId,
                    userId
            );
        }
    }

    private boolean isVisibleToUser(
            Long tenantId,
            CustomerRiskApprovalRequestEntity approval,
            Long userId,
            boolean legacyApprovalOperator
    ) {
        if (Objects.equals(approval.getApplicantUserId(), userId)) {
            return true;
        }
        if (legacyApprovalOperator) {
            return true;
        }
        if (stepMapper == null || ccMapper == null) {
            return false;
        }
        Long stepCount = stepMapper.selectCount(new QueryWrapper<CustomerRiskApprovalStepEntity>()
                .eq("tenant_id", tenantId)
                .eq("request_id", approval.getId())
                .eq("approver_user_id", userId));
        if (stepCount != null && stepCount > 0) {
            return true;
        }
        Long ccCount = ccMapper.selectCount(new QueryWrapper<CustomerRiskApprovalCcEntity>()
                .eq("tenant_id", tenantId)
                .eq("request_id", approval.getId())
                .eq("cc_user_id", userId)
                .isNotNull("visible_at"));
        return ccCount != null && ccCount > 0;
    }

    private CustomerRiskApprovalResponse response(CustomerRiskApprovalRequestEntity approval, Long userId) {
        return response(approval, userId, false);
    }

    private CustomerRiskApprovalResponse response(
            CustomerRiskApprovalRequestEntity approval,
            Long userId,
            boolean legacyApprovalOperator
    ) {
        List<CustomerRiskApprovalStepEntity> steps = loadSteps(approval.getTenantId(), List.of(approval.getId()));
        List<CustomerRiskApprovalCcEntity> ccUsers = loadCcUsers(approval.getTenantId(), List.of(approval.getId()));
        return toResponse(approval, steps, ccUsers, userId, legacyApprovalOperator);
    }

    private List<CustomerRiskApprovalResponse> responses(
            List<CustomerRiskApprovalRequestEntity> approvals,
            Long userId,
            boolean legacyApprovalOperator
    ) {
        if (approvals.isEmpty()) {
            return List.of();
        }
        Long tenantId = approvals.getFirst().getTenantId();
        List<Long> requestIds = approvals.stream().map(CustomerRiskApprovalRequestEntity::getId).toList();
        Map<Long, List<CustomerRiskApprovalStepEntity>> stepsByRequest = loadSteps(tenantId, requestIds).stream()
                .collect(Collectors.groupingBy(CustomerRiskApprovalStepEntity::getRequestId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<CustomerRiskApprovalCcEntity>> ccByRequest = loadCcUsers(tenantId, requestIds).stream()
                .collect(Collectors.groupingBy(CustomerRiskApprovalCcEntity::getRequestId, LinkedHashMap::new, Collectors.toList()));
        return approvals.stream()
                .map(approval -> toResponse(
                        approval,
                        stepsByRequest.getOrDefault(approval.getId(), List.of()),
                        ccByRequest.getOrDefault(approval.getId(), List.of()),
                        userId,
                        legacyApprovalOperator
                ))
                .toList();
    }

    private List<CustomerRiskApprovalStepEntity> loadSteps(Long tenantId, Collection<Long> requestIds) {
        if (stepMapper == null || requestIds.isEmpty()) {
            return List.of();
        }
        return Objects.requireNonNullElse(stepMapper.selectList(new QueryWrapper<CustomerRiskApprovalStepEntity>()
                .eq("tenant_id", tenantId)
                .in("request_id", requestIds)
                .orderByAsc("request_id")
                .orderByAsc("step_order")), List.of());
    }

    private List<CustomerRiskApprovalCcEntity> loadCcUsers(Long tenantId, Collection<Long> requestIds) {
        if (ccMapper == null || requestIds.isEmpty()) {
            return List.of();
        }
        return Objects.requireNonNullElse(ccMapper.selectList(new QueryWrapper<CustomerRiskApprovalCcEntity>()
                .eq("tenant_id", tenantId)
                .in("request_id", requestIds)
                .orderByAsc("request_id")
                .orderByAsc("id")), List.of());
    }

    private CustomerRiskApprovalResponse toResponse(
            CustomerRiskApprovalRequestEntity approval,
            List<CustomerRiskApprovalStepEntity> steps,
            List<CustomerRiskApprovalCcEntity> ccUsers,
            Long userId,
            boolean legacyApprovalOperator
    ) {
        boolean canApprove = CustomerRiskApprovalStatus.PENDING.value().equals(approval.getStatus())
                && !Objects.equals(approval.getApplicantUserId(), userId)
                && ((legacyApprovalOperator && steps.isEmpty())
                        || steps.stream().anyMatch(step -> Objects.equals(step.getStepOrder(), approval.getCurrentApprovalStep())
                                && Objects.equals(step.getApproverUserId(), userId)
                                && CustomerRiskApprovalStatus.PENDING.value().equals(step.getStatus())));
        return CustomerRiskApprovalResponse.fromEntity(
                approval,
                steps.stream().map(CustomerRiskApprovalStepResponse::fromEntity).toList(),
                ccUsers.stream().map(CustomerRiskApprovalCcResponse::fromEntity).toList(),
                canApprove
        );
    }

    private QueryWrapper<CustomerRiskApprovalRequestEntity> baseQuery(Long tenantId) {
        return new QueryWrapper<CustomerRiskApprovalRequestEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<CustomerRiskApprovalRequestEntity> baseUpdate(Long tenantId) {
        return new UpdateWrapper<CustomerRiskApprovalRequestEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private String nextRequestNo(Long tenantId) {
        String datePart = LocalDate.now(clock).format(REQUEST_NO_DATE);
        String prefix = "RA-%s-".formatted(datePart);
        approvalMapper.lockRequestNoGeneration(tenantId, prefix);
        int nextSuffix = number(approvalMapper.maxRequestNoSuffix(tenantId, prefix)) + 1;
        return prefix + "%05d".formatted(nextSuffix);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String moneyText(BigDecimal value) {
        return money(value).stripTrailingZeros().toPlainString();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record CreditSnapshot(
            BigDecimal creditLimit,
            BigDecimal occupiedAmount,
            BigDecimal pendingApprovalAmount,
            BigDecimal availableAmount
    ) {
    }

    private record ApprovalConfiguration(
            CustomerCategoryEntity category,
            List<CustomerCategoryApprovalMemberEntity> approvers,
            List<CustomerCategoryApprovalMemberEntity> ccUsers,
            Map<Long, SystemUserEntity> users
    ) {
    }

    private record RiskSnapshot(
            Long customerId,
            String customerName,
            Long teamId,
            Long orderId,
            BigDecimal requestedAmount,
            List<String> riskTypes,
            String riskSummary,
            boolean contractExpired,
            LocalDate contractExpireDate,
            boolean creditOverLimit,
            BigDecimal creditLimit,
            BigDecimal occupiedAmount,
            BigDecimal pendingApprovalAmount,
            BigDecimal availableAmount,
            BigDecimal overLimitAmount
    ) {
        CustomerRiskCheckResponse toCheckResponse(
                boolean approvalEnabled,
                CustomerRiskApprovalRequestEntity reusableApproval
        ) {
            return new CustomerRiskCheckResponse(
                    customerId,
                    customerName,
                    teamId,
                    orderId,
                    requestedAmount,
                    riskTypes,
                    riskSummary,
                    contractExpired,
                    contractExpireDate,
                    creditOverLimit,
                    creditLimit,
                    occupiedAmount,
                    pendingApprovalAmount,
                    availableAmount,
                    overLimitAmount,
                    approvalEnabled,
                    approvalEnabled && !riskTypes.isEmpty(),
                    reusableApproval == null ? null : reusableApproval.getId(),
                    reusableApproval == null ? null : reusableApproval.getRequestNo(),
                    reusableApproval == null ? null : reusableApproval.getStatus(),
                    reusableApproval == null ? null : reusableApproval.getRequestedAmount()
            );
        }
    }
}
