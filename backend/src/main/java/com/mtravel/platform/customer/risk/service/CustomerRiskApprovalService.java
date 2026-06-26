package com.mtravel.platform.customer.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.contract.entity.ContractEntity;
import com.mtravel.platform.contract.mapper.ContractMapper;
import com.mtravel.platform.customer.credit.entity.CustomerCreditAccountEntity;
import com.mtravel.platform.customer.credit.mapper.CustomerCreditAccountMapper;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalApplyRequest;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalResponse;
import com.mtravel.platform.customer.risk.dto.CustomerRiskCheckResponse;
import com.mtravel.platform.customer.risk.entity.CustomerRiskApprovalRequestEntity;
import com.mtravel.platform.customer.risk.enums.CustomerRiskApprovalStatus;
import com.mtravel.platform.customer.risk.mapper.CustomerRiskApprovalRequestMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.system.config.service.BusinessRiskConfigService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 客户风控审批业务服务。
 *
 * <p>服务层统一处理客户合同到期、授信超限的风险计算、审批申请、审批状态流转和订单保存校验。
 * Controller 只传入当前租户、操作人和角色，不直接判断审批规则。</p>
 */
@Service
public class CustomerRiskApprovalService {

    private static final String RISK_CONTRACT_EXPIRED = "contract_expired";
    private static final String RISK_CREDIT_OVER_LIMIT = "credit_over_limit";
    private static final DateTimeFormatter REQUEST_NO_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final CustomerRiskApprovalRequestMapper approvalMapper;
    private final CustomerUnitMapper customerMapper;
    private final CustomerCreditAccountMapper creditMapper;
    private final ContractMapper contractMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final BusinessRiskConfigService configService;
    private final Clock clock;

    @Autowired
    public CustomerRiskApprovalService(
            CustomerRiskApprovalRequestMapper approvalMapper,
            CustomerUnitMapper customerMapper,
            CustomerCreditAccountMapper creditMapper,
            ContractMapper contractMapper,
            SalesBookingOrderMapper orderMapper,
            BusinessRiskConfigService configService
    ) {
        this(approvalMapper, customerMapper, creditMapper, contractMapper, orderMapper, configService, Clock.systemDefaultZone());
    }

    CustomerRiskApprovalService(
            CustomerRiskApprovalRequestMapper approvalMapper,
            CustomerUnitMapper customerMapper,
            CustomerCreditAccountMapper creditMapper,
            ContractMapper contractMapper,
            SalesBookingOrderMapper orderMapper,
            BusinessRiskConfigService configService,
            Clock clock
    ) {
        this.approvalMapper = approvalMapper;
        this.customerMapper = customerMapper;
        this.creditMapper = creditMapper;
        this.contractMapper = contractMapper;
        this.orderMapper = orderMapper;
        this.configService = configService;
        this.clock = clock;
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
            String operator
    ) {
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
        CustomerRiskApprovalRequestEntity entity = new CustomerRiskApprovalRequestEntity();
        entity.setTenantId(tenantId);
        entity.setCustomerId(customer.getId());
        entity.setCustomerName(customer.getCustomerName());
        entity.setTeamId(request.teamId());
        entity.setOrderId(bindableOrderId);
        entity.setRequestNo(nextRequestNo());
        entity.setRequestedAmount(snapshot.requestedAmount());
        entity.setRiskTypes(String.join(",", snapshot.riskTypes()));
        entity.setRiskSummary(snapshot.riskSummary());
        entity.setContractExpireDate(snapshot.contractExpireDate());
        entity.setCreditLimit(snapshot.creditLimit());
        entity.setOccupiedAmount(snapshot.occupiedAmount());
        entity.setPendingApprovalAmount(snapshot.pendingApprovalAmount());
        entity.setAvailableAmount(snapshot.availableAmount());
        entity.setOverLimitAmount(snapshot.overLimitAmount());
        entity.setStatus(CustomerRiskApprovalStatus.PENDING.value());
        entity.setApplicant(operator);
        entity.setCreatedBy(operator);
        entity.setRemark(clean(request.remark()));
        entity.setIsDeleted(false);
        approvalMapper.insert(entity);
        return CustomerRiskApprovalResponse.fromEntity(entity);
    }

    /** 分页查询客户风控审批申请。 */
    public PageResult<CustomerRiskApprovalResponse> page(
            Long tenantId,
            String keyword,
            String status,
            Long customerId,
            Long teamId,
            Long orderId,
            long page,
            long pageSize
    ) {
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
        Page<CustomerRiskApprovalRequestEntity> result = approvalMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(CustomerRiskApprovalResponse::fromEntity).toList(),
                result.getTotal()
        );
    }

    /** 查询审批申请详情。 */
    public CustomerRiskApprovalResponse detail(Long tenantId, Long id) {
        return CustomerRiskApprovalResponse.fromEntity(requireApproval(tenantId, id));
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

    /** 总经理或管理员同意审批申请。 */
    @Transactional
    public CustomerRiskApprovalResponse approve(Long tenantId, Long id, String approvalRemark, String operator, List<String> roles) {
        assertManagerRole(roles);
        CustomerRiskApprovalRequestEntity current = requireApproval(tenantId, id);
        assertPending(current);
        assertApprovalMatchesBoundOrder(tenantId, current, "审批单与订单客户不一致，不能同意");
        assertNoOtherApprovedApprovalForOrder(
                tenantId,
                current.getOrderId(),
                current.getId(),
                "当前订单已有已通过的风控审批单，不能重复同意"
        );
        CustomerRiskApprovalRequestEntity update = new CustomerRiskApprovalRequestEntity();
        update.setStatus(CustomerRiskApprovalStatus.APPROVED.value());
        update.setApprovedBy(operator);
        update.setApprovedAt(OffsetDateTime.now(clock));
        update.setApprovalRemark(clean(approvalRemark));
        approvalMapper.update(update, baseUpdate(tenantId).eq("id", id));
        current.setStatus(update.getStatus());
        current.setApprovedBy(update.getApprovedBy());
        current.setApprovedAt(update.getApprovedAt());
        current.setApprovalRemark(update.getApprovalRemark());
        return CustomerRiskApprovalResponse.fromEntity(current);
    }

    /** 总经理或管理员拒绝审批申请。 */
    @Transactional
    public CustomerRiskApprovalResponse reject(Long tenantId, Long id, String approvalRemark, String operator, List<String> roles) {
        assertManagerRole(roles);
        CustomerRiskApprovalRequestEntity current = requireApproval(tenantId, id);
        assertPending(current);
        CustomerRiskApprovalRequestEntity update = new CustomerRiskApprovalRequestEntity();
        update.setStatus(CustomerRiskApprovalStatus.REJECTED.value());
        update.setRejectedBy(operator);
        update.setRejectedAt(OffsetDateTime.now(clock));
        update.setApprovalRemark(clean(approvalRemark));
        approvalMapper.update(update, baseUpdate(tenantId).eq("id", id));
        current.setStatus(update.getStatus());
        current.setRejectedBy(update.getRejectedBy());
        current.setRejectedAt(update.getRejectedAt());
        current.setApprovalRemark(update.getApprovalRemark());
        return CustomerRiskApprovalResponse.fromEntity(current);
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
            throw new BizException("客户合同或授信风险需要总经理审批后才能提交订单");
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
        if (approval.getOrderId() != null && orderId != null && !Objects.equals(approval.getOrderId(), orderId)) {
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
        approvalMapper.update(update, baseUpdate(tenantId).eq("id", riskApprovalRequestId));
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

    private CustomerRiskApprovalRequestEntity requireApproval(Long tenantId, Long id) {
        CustomerRiskApprovalRequestEntity approval = approvalMapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (approval == null) {
            throw new BizException("客户风控审批申请不存在或已删除");
        }
        return approval;
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

    private void assertManagerRole(List<String> roles) {
        boolean allowed = Objects.requireNonNullElse(roles, List.<String>of())
                .stream()
                .map(role -> role == null ? "" : role.toLowerCase(Locale.ROOT).replace("role_", ""))
                .anyMatch(role -> "boss".equals(role) || "admin".equals(role));
        if (!allowed) {
            throw new BizException("只有总经理或管理员可以处理风控审批");
        }
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

    private String nextRequestNo() {
        String datePart = LocalDate.now(clock).format(REQUEST_NO_DATE);
        long suffix = Math.abs(System.nanoTime() % 100_000);
        return "RA-%s-%05d".formatted(datePart, suffix);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
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
