package com.mtravel.platform.finance.guideimprest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementPriceLineEntity;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementPriceLineMapper;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestApplyRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestCalcLineResponse;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestDecisionRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestPaymentRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestPreviewResponse;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestResponse;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestResponse.CurrentCalculation;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestCalcLineEntity;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestEntity;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestPaymentEntity;
import com.mtravel.platform.finance.guideimprest.enums.GuideImprestStatus;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestCalcLineMapper;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestMapper;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestPaymentMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 导游备用金业务服务。
 *
 * <p>服务层负责备用金计算、申请快照、总经理审批和财务付款登记。所有金额都在后端计算或校验，
 * 前端只展示计算结果和提交业务动作。</p>
 */
@Service
public class FinanceGuideImprestService {

    private static final DateTimeFormatter REQUEST_NO_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final FinanceGuideImprestMapper imprestMapper;
    private final FinanceGuideImprestCalcLineMapper calcLineMapper;
    private final FinanceGuideImprestPaymentMapper paymentMapper;
    private final SalesTeamMapper teamMapper;
    private final DispatchTeamGuideMapper teamGuideMapper;
    private final DispatchTeamArrangementMapper arrangementMapper;
    private final DispatchTeamArrangementPriceLineMapper priceLineMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final GuideImprestConfigService configService;
    private final Clock clock;

    @Autowired
    public FinanceGuideImprestService(
            FinanceGuideImprestMapper imprestMapper,
            FinanceGuideImprestCalcLineMapper calcLineMapper,
            FinanceGuideImprestPaymentMapper paymentMapper,
            SalesTeamMapper teamMapper,
            DispatchTeamGuideMapper teamGuideMapper,
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementPriceLineMapper priceLineMapper,
            SalesBookingOrderMapper orderMapper,
            GuideImprestConfigService configService
    ) {
        this(
                imprestMapper,
                calcLineMapper,
                paymentMapper,
                teamMapper,
                teamGuideMapper,
                arrangementMapper,
                priceLineMapper,
                orderMapper,
                configService,
                Clock.systemDefaultZone()
        );
    }

    FinanceGuideImprestService(
            FinanceGuideImprestMapper imprestMapper,
            FinanceGuideImprestCalcLineMapper calcLineMapper,
            FinanceGuideImprestPaymentMapper paymentMapper,
            SalesTeamMapper teamMapper,
            DispatchTeamGuideMapper teamGuideMapper,
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementPriceLineMapper priceLineMapper,
            SalesBookingOrderMapper orderMapper,
            GuideImprestConfigService configService,
            Clock clock
    ) {
        this.imprestMapper = imprestMapper;
        this.calcLineMapper = calcLineMapper;
        this.paymentMapper = paymentMapper;
        this.teamMapper = teamMapper;
        this.teamGuideMapper = teamGuideMapper;
        this.arrangementMapper = arrangementMapper;
        this.priceLineMapper = priceLineMapper;
        this.orderMapper = orderMapper;
        this.configService = configService;
        this.clock = clock;
    }

    /**
     * 预览导游备用金计算结果。
     *
     * <p>公式：建议备用金 = 现付总成本 - 自费加点抵扣金额。结果为负数时，应发备用金为 0，
     * 负数绝对值记为导游应上交金额。</p>
     */
    public GuideImprestPreviewResponse preview(Long tenantId, Long teamId, Long guideId) {
        return preview(tenantId, teamId, guideId, null);
    }

    /** 按指定公司加点率预览导游备用金计算结果。 */
    public GuideImprestPreviewResponse preview(Long tenantId, Long teamId, Long guideId, BigDecimal companyMarkupRateOverride) {
        SalesTeamEntity team = requireTeam(tenantId, teamId);
        DispatchTeamGuideEntity guide = requireTeamGuide(tenantId, teamId, guideId);
        Integer guestCount = Math.max(0, Objects.requireNonNullElse(orderMapper.sumGuestCountByTeam(tenantId, teamId), 0));
        BigDecimal companyMarkupRate = resolveCompanyMarkupRate(tenantId, companyMarkupRateOverride, team);
        List<DispatchTeamArrangementEntity> arrangements = arrangementMapper.selectList(baseArrangementQuery(tenantId, teamId));

        Calculation calculation = calculate(teamId, guestCount, companyMarkupRate, arrangements);
        BigDecimal occupiedAuthorizationAmount = occupiedAuthorizationAmount(tenantId, teamId, guideId, null);
        BigDecimal availableAuthorizationAmount = availableAuthorizationAmount(
                calculation.suggestedImprestAmount(),
                occupiedAuthorizationAmount
        );
        return new GuideImprestPreviewResponse(
                team.getId(),
                team.getTeamNo(),
                guide.getGuideId(),
                guide.getGuideName(),
                guestCount,
                companyMarkupRate,
                calculation.cashCostAmount(),
                calculation.optionalDeductionAmount(),
                calculation.calculatedAmount(),
                calculation.suggestedImprestAmount(),
                calculation.guideTurnInAmount(),
                occupiedAuthorizationAmount,
                availableAuthorizationAmount,
                calculation.responseLines()
        );
    }

    /** 分页查询导游备用金申请。 */
    public PageResult<GuideImprestResponse> page(
            Long tenantId,
            String keyword,
            String status,
            Long teamId,
            Long guideId,
            long page,
            long pageSize
    ) {
        QueryWrapper<FinanceGuideImprestEntity> wrapper = baseImprestQuery(tenantId)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(teamId != null, "team_id", teamId)
                .eq(guideId != null, "guide_id", guideId)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("request_no", keyword)
                        .or()
                        .like("team_no", keyword)
                        .or()
                        .like("guide_name", keyword))
                .orderByDesc("created_at")
                .orderByDesc("id");
        Page<FinanceGuideImprestEntity> result = imprestMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<GuideImprestResponse> items = result.getRecords()
                .stream()
                .map(entity -> GuideImprestResponse.fromEntity(entity, List.of()))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /** 查询导游备用金申请详情。 */
    public GuideImprestResponse detail(Long tenantId, Long id) {
        FinanceGuideImprestEntity entity = requireImprest(tenantId, id);
        return GuideImprestResponse.fromEntity(entity, loadCalcLineResponses(tenantId, id), currentCalculation(tenantId, entity));
    }

    /**
     * 提交导游备用金申请。
     *
     * <p>允许同一团队同一导游多次申请。每次申请都保存当时计算快照，后续团队成本变化不会覆盖历史审批依据。</p>
     */
    @Transactional
    public GuideImprestResponse submit(Long tenantId, GuideImprestApplyRequest request, String operator) {
        SalesTeamEntity team = requireTeam(tenantId, request.teamId());
        DispatchTeamGuideEntity guide = requireTeamGuide(tenantId, request.teamId(), request.guideId());
        Integer guestCount = Math.max(0, Objects.requireNonNullElse(orderMapper.sumGuestCountByTeam(tenantId, request.teamId()), 0));
        BigDecimal companyMarkupRate = resolveCompanyMarkupRate(tenantId, request.companyMarkupRate(), team);
        List<DispatchTeamArrangementEntity> arrangements = arrangementMapper.selectList(baseArrangementQuery(tenantId, request.teamId()));
        Calculation calculation = calculate(request.teamId(), guestCount, companyMarkupRate, arrangements);
        BigDecimal occupiedAuthorizationAmount = occupiedAuthorizationAmount(tenantId, request.teamId(), request.guideId(), null);
        BigDecimal availableAuthorizationAmount = availableAuthorizationAmount(
                calculation.suggestedImprestAmount(),
                occupiedAuthorizationAmount
        );
        BigDecimal requestedAmount = request.requestedAmount() == null
                ? calculation.suggestedImprestAmount()
                : money(request.requestedAmount());
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("申请备用金必须大于0");
        }
        BigDecimal totalAfterApply = occupiedAuthorizationAmount.add(requestedAmount);
        if (totalAfterApply.compareTo(calculation.suggestedImprestAmount()) > 0
                && !StringUtils.hasText(request.remark())) {
            throw new BizException("申请后累计备用金超过系统建议金额，请填写应急或特殊项目说明");
        }

        FinanceGuideImprestEntity entity = new FinanceGuideImprestEntity();
        entity.setTenantId(tenantId);
        entity.setRequestNo(nextRequestNo());
        fillTeamSnapshot(entity, team);
        fillGuideSnapshot(entity, guide);
        entity.setGuestCount(guestCount);
        entity.setCompanyMarkupRate(companyMarkupRate);
        entity.setCashCostAmount(calculation.cashCostAmount());
        entity.setOptionalDeductionAmount(calculation.optionalDeductionAmount());
        entity.setCalculatedAmount(calculation.calculatedAmount());
        entity.setSuggestedImprestAmount(calculation.suggestedImprestAmount());
        entity.setGuideTurnInAmount(calculation.guideTurnInAmount());
        entity.setRequestedAmount(requestedAmount);
        entity.setApprovedAmount(BigDecimal.ZERO.setScale(2));
        entity.setPaidAmount(BigDecimal.ZERO.setScale(2));
        entity.setBalanceAmount(requestedAmount);
        entity.setStatus(GuideImprestStatus.PENDING_MANAGER.value());
        entity.setApplicant(operator);
        entity.setAppliedAt(OffsetDateTime.now(clock));
        entity.setCreatedBy(operator);
        entity.setRemark(clean(request.remark()));
        entity.setIsDeleted(false);
        imprestMapper.insert(entity);

        for (FinanceGuideImprestCalcLineEntity line : calculation.entityLines()) {
            line.setTenantId(tenantId);
            line.setImprestId(entity.getId());
            line.setCreatedBy(operator);
            line.setIsDeleted(false);
            calcLineMapper.insert(line);
        }
        return GuideImprestResponse.fromEntity(
                entity,
                calculation.responseLines(),
                currentCalculationFrom(
                        entity,
                        calculation,
                        occupiedAuthorizationAmount.add(requestedAmount),
                        null
                )
        );
    }

    /** 总经理同意导游备用金申请。 */
    @Transactional
    public GuideImprestResponse approve(
            Long tenantId,
            Long id,
            GuideImprestDecisionRequest request,
            String operator,
            List<String> roles
    ) {
        assertManagerRole(roles);
        FinanceGuideImprestEntity current = requireImprest(tenantId, id);
        assertPendingManager(current);
        CurrentCalculation currentCalculation = currentCalculation(tenantId, current);
        if (Boolean.TRUE.equals(currentCalculation.calculationChanged())) {
            throw new BizException(currentCalculation.calculationChangeMessage() + "请计调重新计算备用金后再审批");
        }
        FinanceGuideImprestEntity update = new FinanceGuideImprestEntity();
        update.setStatus(GuideImprestStatus.MANAGER_APPROVED.value());
        update.setApprovedAmount(money(current.getRequestedAmount()));
        update.setBalanceAmount(money(current.getRequestedAmount()).subtract(money(current.getPaidAmount())));
        update.setApprovedBy(operator);
        update.setApprovedAt(OffsetDateTime.now(clock));
        update.setApprovalRemark(clean(request == null ? null : request.approvalRemark()));
        imprestMapper.update(update, baseImprestUpdate(tenantId).eq("id", id));
        current.setStatus(update.getStatus());
        current.setApprovedAmount(update.getApprovedAmount());
        current.setBalanceAmount(update.getBalanceAmount());
        current.setApprovedBy(update.getApprovedBy());
        current.setApprovedAt(update.getApprovedAt());
        current.setApprovalRemark(update.getApprovalRemark());
        return GuideImprestResponse.fromEntity(current, loadCalcLineResponses(tenantId, id), currentCalculation);
    }

    /** 总经理拒绝导游备用金申请。 */
    @Transactional
    public GuideImprestResponse reject(
            Long tenantId,
            Long id,
            GuideImprestDecisionRequest request,
            String operator,
            List<String> roles
    ) {
        assertManagerRole(roles);
        FinanceGuideImprestEntity current = requireImprest(tenantId, id);
        assertPendingManager(current);
        FinanceGuideImprestEntity update = new FinanceGuideImprestEntity();
        update.setStatus(GuideImprestStatus.MANAGER_REJECTED.value());
        update.setRejectedBy(operator);
        update.setRejectedAt(OffsetDateTime.now(clock));
        update.setApprovalRemark(clean(request == null ? null : request.approvalRemark()));
        imprestMapper.update(update, baseImprestUpdate(tenantId).eq("id", id));
        current.setStatus(update.getStatus());
        current.setRejectedBy(update.getRejectedBy());
        current.setRejectedAt(update.getRejectedAt());
        current.setApprovalRemark(update.getApprovalRemark());
        return GuideImprestResponse.fromEntity(current, loadCalcLineResponses(tenantId, id), currentCalculation(tenantId, current));
    }

    /**
     * 财务登记导游备用金付款。
     *
     * <p>可以分多次付款；累计付款达到审批金额时，申请状态变为 paid。</p>
     */
    @Transactional
    public GuideImprestResponse registerPayment(
            Long tenantId,
            Long id,
            GuideImprestPaymentRequest request,
            String operator
    ) {
        FinanceGuideImprestEntity current = requireImprest(tenantId, id);
        if (!GuideImprestStatus.MANAGER_APPROVED.value().equals(current.getStatus())
                && !GuideImprestStatus.PAID.value().equals(current.getStatus())) {
            throw new BizException("导游备用金需总经理审批通过后才能付款");
        }
        CurrentCalculation currentCalculation = currentCalculation(tenantId, current);
        if (Boolean.TRUE.equals(currentCalculation.calculationChanged())) {
            throw new BizException(currentCalculation.calculationChangeMessage() + "请作废旧备用金申请并重新提交后再付款");
        }
        BigDecimal amount = money(request.amount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("付款金额必须大于0");
        }
        BigDecimal approvedAmount = money(current.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0
                ? current.getApprovedAmount()
                : current.getRequestedAmount());
        BigDecimal paidAmount = money(current.getPaidAmount()).add(amount).setScale(2, RoundingMode.HALF_UP);
        if (paidAmount.compareTo(approvedAmount) > 0) {
            throw new BizException("付款金额不能超过审批金额余额");
        }
        BigDecimal balanceAmount = approvedAmount.subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);

        FinanceGuideImprestPaymentEntity payment = new FinanceGuideImprestPaymentEntity();
        payment.setTenantId(tenantId);
        payment.setImprestId(id);
        payment.setTeamId(current.getTeamId());
        payment.setPaymentNo(nextPaymentNo());
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentMethod(clean(request.paymentMethod()));
        payment.setPaymentAccountName(clean(request.paymentAccountName()));
        payment.setAmount(amount);
        payment.setPayer(operator);
        payment.setStatus("active");
        payment.setCreatedBy(operator);
        payment.setRemark(clean(request.remark()));
        payment.setIsDeleted(false);
        paymentMapper.insert(payment);

        FinanceGuideImprestEntity update = new FinanceGuideImprestEntity();
        update.setPaidAmount(paidAmount);
        update.setBalanceAmount(balanceAmount);
        update.setStatus(balanceAmount.compareTo(BigDecimal.ZERO) == 0
                ? GuideImprestStatus.PAID.value()
                : GuideImprestStatus.MANAGER_APPROVED.value());
        imprestMapper.update(update, baseImprestUpdate(tenantId).eq("id", id));
        current.setPaidAmount(paidAmount);
        current.setBalanceAmount(balanceAmount);
        current.setStatus(update.getStatus());
        return GuideImprestResponse.fromEntity(current, loadCalcLineResponses(tenantId, id), currentCalculation);
    }

    /**
     * 作废未付款导游备用金申请。
     *
     * <p>作废只影响后续建议余额和风险提示汇总，不删除原始申请和计算快照，便于审计追溯。</p>
     */
    @Transactional
    public GuideImprestResponse cancel(Long tenantId, Long id, String cancelReason, String operator) {
        FinanceGuideImprestEntity current = requireImprest(tenantId, id);
        if (GuideImprestStatus.CANCELLED.value().equals(current.getStatus())) {
            throw new BizException("导游备用金申请已作废");
        }
        if (money(current.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("已付款备用金申请不能作废");
        }
        FinanceGuideImprestEntity update = new FinanceGuideImprestEntity();
        update.setStatus(GuideImprestStatus.CANCELLED.value());
        update.setBalanceAmount(BigDecimal.ZERO.setScale(2));
        update.setCancelledBy(operator);
        update.setCancelledAt(OffsetDateTime.now(clock));
        update.setCancelReason(clean(cancelReason));
        imprestMapper.update(update, baseImprestUpdate(tenantId).eq("id", id));
        current.setStatus(update.getStatus());
        current.setBalanceAmount(update.getBalanceAmount());
        current.setCancelledBy(update.getCancelledBy());
        current.setCancelledAt(update.getCancelledAt());
        current.setCancelReason(update.getCancelReason());
        return GuideImprestResponse.fromEntity(current, loadCalcLineResponses(tenantId, id), currentCalculation(tenantId, current));
    }

    /** 计算当前申请对应团队和导游的最新备用金状态。 */
    private CurrentCalculation currentCalculation(Long tenantId, FinanceGuideImprestEntity entity) {
        Integer guestCount = Math.max(0, Objects.requireNonNullElse(orderMapper.sumGuestCountByTeam(tenantId, entity.getTeamId()), 0));
        BigDecimal companyMarkupRate = resolveCompanyMarkupRate(tenantId, entity.getCompanyMarkupRate());
        List<DispatchTeamArrangementEntity> arrangements = Objects.requireNonNullElse(
                arrangementMapper.selectList(baseArrangementQuery(tenantId, entity.getTeamId())),
                List.of()
        );
        Calculation calculation = calculate(
                entity.getTeamId(),
                guestCount,
                companyMarkupRate,
                arrangements
        );
        BigDecimal occupiedAuthorizationAmount = occupiedAuthorizationAmount(
                tenantId,
                entity.getTeamId(),
                entity.getGuideId(),
                null
        );
        String calculationChangeMessage = calculationChangeMessage(
                tenantId,
                entity,
                calculation,
                guestCount,
                arrangements
        );
        return currentCalculationFrom(
                entity,
                calculation,
                occupiedAuthorizationAmount,
                calculationChangeMessage,
                guestCount
        );
    }

    /** 构造当前团队安排和申请快照的金额差异说明，便于审批和付款环节直接说明异常来源。 */
    private String calculationChangeMessage(
            Long tenantId,
            FinanceGuideImprestEntity entity,
            Calculation calculation,
            Integer currentGuestCount,
            List<DispatchTeamArrangementEntity> currentArrangements
    ) {
        List<String> changes = new ArrayList<>();
        appendLineLevelChanges(changes, tenantId, entity, calculation, currentArrangements);
        Integer snapshotGuestCount = Objects.requireNonNullElse(entity.getGuestCount(), 0);
        if (!Objects.equals(currentGuestCount, snapshotGuestCount)) {
            changes.add("实收人数从 " + snapshotGuestCount + " 人变为 " + currentGuestCount + " 人");
        }
        appendMoneyChange(changes, "现付总成本", entity.getCashCostAmount(), calculation.cashCostAmount());
        appendMoneyChange(changes, "自费抵扣", entity.getOptionalDeductionAmount(), calculation.optionalDeductionAmount());
        appendMoneyChange(changes, "建议备用金", entity.getSuggestedImprestAmount(), calculation.suggestedImprestAmount());
        appendMoneyChange(changes, "导游应上交", entity.getGuideTurnInAmount(), calculation.guideTurnInAmount());
        if (changes.isEmpty()) {
            return null;
        }
        return "团队安排已变化：" + String.join("；", changes) + "。";
    }

    /** 追加申请快照明细与当前团队安排之间的项目级差异，优先定位到具体费用项。 */
    private void appendLineLevelChanges(
            List<String> changes,
            Long tenantId,
            FinanceGuideImprestEntity entity,
            Calculation calculation,
            List<DispatchTeamArrangementEntity> currentArrangements
    ) {
        List<FinanceGuideImprestCalcLineEntity> snapshotLines = loadCalcLines(tenantId, entity.getId());
        if (snapshotLines.isEmpty()) {
            return;
        }
        Map<Long, DispatchTeamArrangementEntity> currentArrangementById = currentArrangements.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(DispatchTeamArrangementEntity::getId, Function.identity(), (left, right) -> left));
        Map<CalcLineKey, FinanceGuideImprestCalcLineEntity> currentLineByKey = calculation.entityLines()
                .stream()
                .filter(item -> item.getSourceArrangementId() != null)
                .collect(Collectors.toMap(this::calcLineKey, Function.identity(), (left, right) -> left));

        for (FinanceGuideImprestCalcLineEntity snapshotLine : snapshotLines) {
            if ("cash_cost".equals(snapshotLine.getLineType())) {
                appendCashCostLineChange(changes, snapshotLine, currentArrangementById);
                continue;
            }
            if ("optional_deduction".equals(snapshotLine.getLineType())) {
                appendOptionalDeductionLineChange(changes, snapshotLine, currentLineByKey.get(calcLineKey(snapshotLine)));
            }
        }
    }

    /** 追加现付成本项目级变化说明。 */
    private void appendCashCostLineChange(
            List<String> changes,
            FinanceGuideImprestCalcLineEntity snapshotLine,
            Map<Long, DispatchTeamArrangementEntity> currentArrangementById
    ) {
        Long sourceArrangementId = snapshotLine.getSourceArrangementId();
        DispatchTeamArrangementEntity currentArrangement = sourceArrangementId == null
                ? null
                : currentArrangementById.get(sourceArrangementId);
        if (currentArrangement == null) {
            changes.add(itemName(snapshotLine) + "：申请时现付 "
                    + formatMoney(snapshotLine.getAmount()) + "，当前安排已删除或不再生效");
            return;
        }
        BigDecimal currentCashAmount = money(currentArrangement.getCashAmount());
        if (!amountEquals(snapshotLine.getAmount(), currentCashAmount)) {
            changes.add(itemName(snapshotLine) + "：现付从 "
                    + formatMoney(snapshotLine.getAmount()) + " 变为 " + formatMoney(currentCashAmount));
        }
    }

    /** 追加自费抵扣项目级变化说明。 */
    private void appendOptionalDeductionLineChange(
            List<String> changes,
            FinanceGuideImprestCalcLineEntity snapshotLine,
            FinanceGuideImprestCalcLineEntity currentLine
    ) {
        if (currentLine == null) {
            changes.add(itemName(snapshotLine) + "：申请时自费抵扣 "
                    + formatMoney(snapshotLine.getAmount()) + "，当前已删除或不再参与自费抵扣");
            return;
        }
        if (!amountEquals(snapshotLine.getAmount(), currentLine.getAmount())) {
            changes.add(itemName(snapshotLine) + "：自费抵扣从 "
                    + formatMoney(snapshotLine.getAmount()) + " 变为 " + formatMoney(currentLine.getAmount()));
        }
    }

    private CalcLineKey calcLineKey(FinanceGuideImprestCalcLineEntity line) {
        return new CalcLineKey(line.getLineType(), line.getSourceArrangementId(), line.getSourcePriceLineId());
    }

    private String itemName(FinanceGuideImprestCalcLineEntity line) {
        return StringUtils.hasText(line.getItemName()) ? line.getItemName() : "未命名项目";
    }

    /** 金额发生变化时追加一段“从 A 变为 B”的中文说明。 */
    private void appendMoneyChange(List<String> changes, String label, BigDecimal snapshotAmount, BigDecimal currentAmount) {
        if (!amountEquals(currentAmount, snapshotAmount)) {
            changes.add(label + "从 " + formatMoney(snapshotAmount) + " 变为 " + formatMoney(currentAmount));
        }
    }

    /** 将金额格式化为审批提示中使用的人民币金额。 */
    private String formatMoney(BigDecimal amount) {
        return "¥" + money(amount).toPlainString();
    }

    private CurrentCalculation currentCalculationFrom(
            FinanceGuideImprestEntity entity,
            Calculation calculation,
            BigDecimal occupiedAuthorizationAmount,
            String calculationChangeMessage
    ) {
        return currentCalculationFrom(
                entity,
                calculation,
                occupiedAuthorizationAmount,
                calculationChangeMessage,
                Objects.requireNonNullElse(entity.getGuestCount(), 0)
        );
    }

    private CurrentCalculation currentCalculationFrom(
            FinanceGuideImprestEntity entity,
            Calculation calculation,
            BigDecimal occupiedAuthorizationAmount,
            String calculationChangeMessage,
            Integer currentGuestCount
    ) {
        BigDecimal availableAuthorizationAmount = availableAuthorizationAmount(
                calculation.suggestedImprestAmount(),
                occupiedAuthorizationAmount
        );
        boolean changed = StringUtils.hasText(calculationChangeMessage);
        return new CurrentCalculation(
                money(occupiedAuthorizationAmount),
                availableAuthorizationAmount,
                currentGuestCount,
                calculation.cashCostAmount(),
                calculation.optionalDeductionAmount(),
                calculation.calculatedAmount(),
                calculation.suggestedImprestAmount(),
                calculation.guideTurnInAmount(),
                changed,
                calculationChangeMessage
        );
    }

    /** 汇总同一团队同一导游已占用的备用金授权金额。 */
    private BigDecimal occupiedAuthorizationAmount(Long tenantId, Long teamId, Long guideId, Long excludeId) {
        List<FinanceGuideImprestEntity> records = imprestMapper.selectList(baseImprestQuery(tenantId)
                .eq("team_id", teamId)
                .eq("guide_id", guideId));
        if (records == null || records.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal result = BigDecimal.ZERO.setScale(2);
        for (FinanceGuideImprestEntity item : records) {
            if (excludeId != null && Objects.equals(excludeId, item.getId())) {
                continue;
            }
            if (!occupiesAuthorization(item.getStatus())) {
                continue;
            }
            result = result.add(authorizationAmount(item));
        }
        return money(result);
    }

    private boolean occupiesAuthorization(String status) {
        return GuideImprestStatus.PENDING_MANAGER.value().equals(status)
                || GuideImprestStatus.MANAGER_APPROVED.value().equals(status)
                || GuideImprestStatus.PAID.value().equals(status)
                || GuideImprestStatus.SETTLED.value().equals(status);
    }

    private BigDecimal authorizationAmount(FinanceGuideImprestEntity item) {
        if ((GuideImprestStatus.MANAGER_APPROVED.value().equals(item.getStatus())
                || GuideImprestStatus.PAID.value().equals(item.getStatus())
                || GuideImprestStatus.SETTLED.value().equals(item.getStatus()))
                && money(item.getApprovedAmount()).compareTo(BigDecimal.ZERO) > 0) {
            return money(item.getApprovedAmount());
        }
        return money(item.getRequestedAmount());
    }

    private BigDecimal availableAuthorizationAmount(BigDecimal suggestedAmount, BigDecimal occupiedAmount) {
        return money(suggestedAmount).subtract(money(occupiedAmount)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean amountEquals(BigDecimal left, BigDecimal right) {
        return money(left).compareTo(money(right)) == 0;
    }

    private Calculation calculate(
            Long teamId,
            Integer guestCount,
            BigDecimal companyMarkupRate,
            List<DispatchTeamArrangementEntity> arrangements
    ) {
        List<FinanceGuideImprestCalcLineEntity> entityLines = new ArrayList<>();
        List<GuideImprestCalcLineResponse> responseLines = new ArrayList<>();
        BigDecimal cashCostAmount = BigDecimal.ZERO.setScale(2);
        int sortOrder = 1;

        for (DispatchTeamArrangementEntity arrangement : arrangements) {
            BigDecimal cashAmount = money(arrangement.getCashAmount());
            if (cashAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            cashCostAmount = cashCostAmount.add(cashAmount);
            FinanceGuideImprestCalcLineEntity line = baseCalcLine(
                    teamId,
                    "cash_cost",
                    arrangement,
                    null,
                    arrangement.getItemName(),
                    companyMarkupRate,
                    guestCount,
                    cashAmount,
                    sortOrder++
            );
            entityLines.add(line);
            responseLines.add(toResponseLine(line));
        }

        List<DispatchTeamArrangementEntity> optionalArrangements = arrangements.stream()
                .filter(item -> "optional".equals(item.getArrangementType()))
                .toList();
        Map<Long, DispatchTeamArrangementEntity> optionalById = optionalArrangements.stream()
                .collect(Collectors.toMap(DispatchTeamArrangementEntity::getId, Function.identity()));
        List<DispatchTeamArrangementPriceLineEntity> priceLines = loadOptionalPriceLines(teamId, optionalById.keySet().stream().toList());
        BigDecimal optionalDeductionAmount = BigDecimal.ZERO.setScale(2);
        for (DispatchTeamArrangementPriceLineEntity priceLine : priceLines) {
            DispatchTeamArrangementEntity arrangement = optionalById.get(priceLine.getArrangementId());
            if (arrangement == null) {
                continue;
            }
            OptionalDeduction deduction = calculateOptionalDeduction(priceLine, companyMarkupRate, guestCount);
            if (deduction.amount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            optionalDeductionAmount = optionalDeductionAmount.add(deduction.amount());
            FinanceGuideImprestCalcLineEntity line = baseCalcLine(
                    teamId,
                    "optional_deduction",
                    arrangement,
                    priceLine,
                    StringUtils.hasText(priceLine.getProjectName()) ? priceLine.getProjectName() : arrangement.getItemName(),
                    companyMarkupRate,
                    guestCount,
                    deduction.amount(),
                    sortOrder++
            );
            line.setSalePrice(money(priceLine.getSalePrice()));
            line.setCostPrice(money(priceLine.getCostPrice()));
            line.setGuideCommissionAmount(deduction.guideCommissionAmount());
            line.setGuideCommissionRate(money(priceLine.getGuideCommissionRate()));
            line.setGuideCommissionCalcType(deduction.guideCommissionCalcType());
            entityLines.add(line);
            responseLines.add(toResponseLine(line));
        }

        cashCostAmount = money(cashCostAmount);
        optionalDeductionAmount = money(optionalDeductionAmount);
        BigDecimal calculatedAmount = cashCostAmount.subtract(optionalDeductionAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal suggestedAmount = calculatedAmount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal guideTurnInAmount = calculatedAmount.compareTo(BigDecimal.ZERO) < 0
                ? calculatedAmount.abs().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);
        return new Calculation(
                cashCostAmount,
                optionalDeductionAmount,
                calculatedAmount,
                suggestedAmount,
                guideTurnInAmount,
                entityLines,
                responseLines
        );
    }

    private OptionalDeduction calculateOptionalDeduction(
            DispatchTeamArrangementPriceLineEntity priceLine,
            BigDecimal companyMarkupRate,
            Integer guestCount
    ) {
        BigDecimal salePrice = money(priceLine.getSalePrice());
        BigDecimal costPrice = money(priceLine.getCostPrice());
        BigDecimal grossProfit = salePrice.subtract(costPrice);
        if (grossProfit.compareTo(BigDecimal.ZERO) <= 0 || guestCount == null || guestCount <= 0) {
            return new OptionalDeduction(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), "fixed");
        }
        BigDecimal fixedCommission = money(priceLine.getGuideCommissionAmount());
        BigDecimal rate = money(priceLine.getGuideCommissionRate());
        String calcType = "fixed";
        BigDecimal guideCost = fixedCommission;
        if (guideCost.compareTo(BigDecimal.ZERO) <= 0 && rate.compareTo(BigDecimal.ZERO) > 0) {
            calcType = "percent";
            guideCost = grossProfit.multiply(rate)
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        BigDecimal companyMarkup = grossProfit.subtract(guideCost);
        if (companyMarkup.compareTo(BigDecimal.ZERO) <= 0) {
            return new OptionalDeduction(BigDecimal.ZERO.setScale(2), money(guideCost), calcType);
        }
        BigDecimal amount = companyMarkup
                .multiply(companyMarkupRate)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(guestCount))
                .setScale(2, RoundingMode.HALF_UP);
        return new OptionalDeduction(amount, money(guideCost), calcType);
    }

    private List<DispatchTeamArrangementPriceLineEntity> loadOptionalPriceLines(Long teamId, List<Long> arrangementIds) {
        if (arrangementIds == null || arrangementIds.isEmpty()) {
            return List.of();
        }
        return priceLineMapper.selectList(new QueryWrapper<DispatchTeamArrangementPriceLineEntity>()
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .in("arrangement_id", arrangementIds));
    }

    private FinanceGuideImprestCalcLineEntity baseCalcLine(
            Long teamId,
            String lineType,
            DispatchTeamArrangementEntity arrangement,
            DispatchTeamArrangementPriceLineEntity priceLine,
            String itemName,
            BigDecimal companyMarkupRate,
            Integer guestCount,
            BigDecimal amount,
            Integer sortOrder
    ) {
        FinanceGuideImprestCalcLineEntity line = new FinanceGuideImprestCalcLineEntity();
        line.setTeamId(teamId);
        line.setLineType(lineType);
        line.setSourceArrangementId(arrangement.getId());
        line.setSourcePriceLineId(priceLine == null ? null : priceLine.getId());
        line.setArrangementType(arrangement.getArrangementType());
        line.setItemName(itemName);
        line.setSalePrice(BigDecimal.ZERO.setScale(2));
        line.setCostPrice(BigDecimal.ZERO.setScale(2));
        line.setGuideCommissionAmount(BigDecimal.ZERO.setScale(2));
        line.setGuideCommissionRate(BigDecimal.ZERO.setScale(2));
        line.setGuideCommissionCalcType(null);
        line.setCompanyMarkupRate(companyMarkupRate);
        line.setGuestCount(guestCount);
        line.setAmount(money(amount));
        line.setSortOrder(sortOrder);
        return line;
    }

    private GuideImprestCalcLineResponse toResponseLine(FinanceGuideImprestCalcLineEntity line) {
        return new GuideImprestCalcLineResponse(
                line.getLineType(),
                line.getArrangementType(),
                line.getItemName(),
                line.getSalePrice(),
                line.getCostPrice(),
                line.getGuideCommissionAmount(),
                line.getGuideCommissionRate(),
                line.getGuideCommissionCalcType(),
                line.getCompanyMarkupRate(),
                line.getGuestCount(),
                line.getAmount(),
                line.getSortOrder()
        );
    }

    private List<GuideImprestCalcLineResponse> loadCalcLineResponses(Long tenantId, Long imprestId) {
        return loadCalcLines(tenantId, imprestId)
                .stream()
                .map(this::toResponseLine)
                .toList();
    }

    private List<FinanceGuideImprestCalcLineEntity> loadCalcLines(Long tenantId, Long imprestId) {
        return Objects.requireNonNullElse(calcLineMapper.selectList(new QueryWrapper<FinanceGuideImprestCalcLineEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("imprest_id", imprestId)
                        .eq("is_deleted", false)
                        .orderByAsc("sort_order")
                        .orderByAsc("id")), List.<FinanceGuideImprestCalcLineEntity>of());
    }

    private SalesTeamEntity requireTeam(Long tenantId, Long teamId) {
        SalesTeamEntity team = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", teamId)
                .eq("is_deleted", false)
                .last("limit 1"));
        if (team == null) {
            throw new BizException("团队不存在或已删除");
        }
        return team;
    }

    private DispatchTeamGuideEntity requireTeamGuide(Long tenantId, Long teamId, Long guideId) {
        if (guideId == null) {
            throw new BizException("请选择导游");
        }
        DispatchTeamGuideEntity guide = teamGuideMapper.selectOne(new QueryWrapper<DispatchTeamGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("guide_id", guideId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .last("limit 1"));
        if (guide == null) {
            throw new BizException("该团队未安排此导游");
        }
        return guide;
    }

    private FinanceGuideImprestEntity requireImprest(Long tenantId, Long id) {
        FinanceGuideImprestEntity entity = imprestMapper.selectOne(baseImprestQuery(tenantId)
                .eq("id", id)
                .last("limit 1"));
        if (entity == null) {
            throw new BizException("导游备用金申请不存在或已删除");
        }
        return entity;
    }

    private QueryWrapper<DispatchTeamArrangementEntity> baseArrangementQuery(Long tenantId, Long teamId) {
        return new QueryWrapper<DispatchTeamArrangementEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .eq("status", "active");
    }

    private QueryWrapper<FinanceGuideImprestEntity> baseImprestQuery(Long tenantId) {
        return new QueryWrapper<FinanceGuideImprestEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<FinanceGuideImprestEntity> baseImprestUpdate(Long tenantId) {
        return new UpdateWrapper<FinanceGuideImprestEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private void fillTeamSnapshot(FinanceGuideImprestEntity entity, SalesTeamEntity team) {
        entity.setTeamId(team.getId());
        entity.setTeamNo(team.getTeamNo());
        entity.setTeamType(team.getTeamType());
        entity.setBusinessType(team.getBusinessType());
        entity.setDepartureDate(team.getDepartureDate());
        entity.setDepartmentId(team.getDepartmentId());
        entity.setDepartmentName(team.getDepartmentName());
        entity.setOperatorEmployeeId(team.getOperatorEmployeeId());
        entity.setOperatorEmployeeName(team.getOperatorEmployeeName());
    }

    private void fillGuideSnapshot(FinanceGuideImprestEntity entity, DispatchTeamGuideEntity guide) {
        entity.setGuideId(guide.getGuideId());
        entity.setGuideName(guide.getGuideName());
        entity.setGuideMobile(guide.getGuideMobile());
    }

    private void assertPendingManager(FinanceGuideImprestEntity entity) {
        if (!GuideImprestStatus.PENDING_MANAGER.value().equals(entity.getStatus())) {
            throw new BizException("只有待总经理审批的备用金申请可以处理");
        }
    }

    private void assertManagerRole(List<String> roles) {
        boolean allowed = Objects.requireNonNullElse(roles, List.<String>of())
                .stream()
                .map(role -> role == null ? "" : role.toLowerCase(Locale.ROOT).replace("role_", ""))
                .anyMatch(role -> "boss".equals(role) || "admin".equals(role));
        if (!allowed) {
            throw new BizException("只有总经理或管理员可以处理导游备用金审批");
        }
    }

    private String nextRequestNo() {
        String datePart = LocalDate.now(clock).format(REQUEST_NO_DATE);
        long suffix = Math.abs(System.nanoTime() % 100_000);
        return "GI-%s-%05d".formatted(datePart, suffix);
    }

    private String nextPaymentNo() {
        String datePart = LocalDate.now(clock).format(REQUEST_NO_DATE);
        long suffix = Math.abs(System.nanoTime() % 100_000);
        return "GIP-%s-%05d".formatted(datePart, suffix);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveCompanyMarkupRate(Long tenantId, BigDecimal override) {
        BigDecimal rate = override == null ? configService.getCompanyMarkupRatePercent(tenantId) : override;
        BigDecimal result = money(rate);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("公司加点率不能小于0");
        }
        return result;
    }

    /**
     * 解析备用金计算使用的公司加点率。
     *
     * <p>计调手工传入的本次加点率优先；未传时，团队内部备注里的自费加点率作为本团默认锚点；
     * 团队未设置或为 0 时再回退系统配置，避免历史团队默认 0 误把自费抵扣清零。</p>
     */
    private BigDecimal resolveCompanyMarkupRate(Long tenantId, BigDecimal override, SalesTeamEntity team) {
        if (override != null) {
            return resolveCompanyMarkupRate(tenantId, override);
        }
        BigDecimal teamRate = money(team == null ? null : team.getOptionalMarkupRate());
        if (teamRate.compareTo(BigDecimal.ZERO) > 0) {
            return teamRate;
        }
        return resolveCompanyMarkupRate(tenantId, null);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record OptionalDeduction(
            BigDecimal amount,
            BigDecimal guideCommissionAmount,
            String guideCommissionCalcType
    ) {
    }

    private record CalcLineKey(
            String lineType,
            Long sourceArrangementId,
            Long sourcePriceLineId
    ) {
    }

    private record Calculation(
            BigDecimal cashCostAmount,
            BigDecimal optionalDeductionAmount,
            BigDecimal calculatedAmount,
            BigDecimal suggestedImprestAmount,
            BigDecimal guideTurnInAmount,
            List<FinanceGuideImprestCalcLineEntity> entityLines,
            List<GuideImprestCalcLineResponse> responseLines
    ) {
    }
}
