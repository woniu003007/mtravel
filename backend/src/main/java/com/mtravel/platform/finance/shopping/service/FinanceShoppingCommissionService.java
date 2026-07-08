package com.mtravel.platform.finance.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.finance.shopping.dto.ShoppingCommissionOverviewResponse;
import com.mtravel.platform.finance.shopping.dto.ShoppingCommissionRuleResponse;
import com.mtravel.platform.finance.shopping.dto.ShoppingCommissionRuleSaveRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingFeedbackDetailLineSaveRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingFeedbackDetailLineResponse;
import com.mtravel.platform.finance.shopping.dto.ShoppingFeedbackLineResponse;
import com.mtravel.platform.finance.shopping.dto.ShoppingFeedbackLineSaveRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingSettlementCalculateRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingSettlementLineResponse;
import com.mtravel.platform.finance.shopping.dto.ShoppingSettlementResponse;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingCommissionRuleEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingFeedbackDetailLineEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingFeedbackLineEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingSettlementEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingSettlementLineEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingTeamRuleOverrideEntity;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingCommissionRuleMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingFeedbackDetailLineMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingFeedbackLineMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingSettlementLineMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingSettlementMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingTeamRuleOverrideMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 购物业绩和公司补佣业务服务。
 *
 * <p>服务层负责购物店反馈、团队参考规则覆盖、公司补佣和结算快照生成。所有购物利润均由
 * 后端计算，不能由前端页面本地公式作为正式结算结果。</p>
 */
@Service
public class FinanceShoppingCommissionService {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("5000.00");
    private static final BigDecimal DEFAULT_BASE_RATE = new BigDecimal("8.00");
    private static final BigDecimal DEFAULT_TARGET_RATE = new BigDecimal("10.00");
    private static final String FULL_AMOUNT_DIFF = "full_amount_diff";
    private static final String MODE_TOTAL = "total";
    private static final String MODE_CATEGORY = "category";
    private static final String CATEGORY_COMPREHENSIVE = "综合";

    private final SalesTeamMapper teamMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final FinanceShoppingCommissionRuleMapper ruleMapper;
    private final FinanceShoppingTeamRuleOverrideMapper overrideMapper;
    private final FinanceShoppingFeedbackLineMapper feedbackLineMapper;
    private final FinanceShoppingFeedbackDetailLineMapper feedbackDetailLineMapper;
    private final FinanceShoppingSettlementMapper settlementMapper;
    private final FinanceShoppingSettlementLineMapper settlementLineMapper;
    private final FinanceShoppingCommissionCalculator calculator;
    private final Clock clock;

    @Autowired
    public FinanceShoppingCommissionService(
            SalesTeamMapper teamMapper,
            SalesBookingOrderMapper orderMapper,
            FinanceShoppingCommissionRuleMapper ruleMapper,
            FinanceShoppingTeamRuleOverrideMapper overrideMapper,
            FinanceShoppingFeedbackLineMapper feedbackLineMapper,
            FinanceShoppingFeedbackDetailLineMapper feedbackDetailLineMapper,
            FinanceShoppingSettlementMapper settlementMapper,
            FinanceShoppingSettlementLineMapper settlementLineMapper,
            FinanceShoppingCommissionCalculator calculator
    ) {
        this(
                teamMapper,
                orderMapper,
                ruleMapper,
                overrideMapper,
                feedbackLineMapper,
                feedbackDetailLineMapper,
                settlementMapper,
                settlementLineMapper,
                calculator,
                Clock.systemDefaultZone()
        );
    }

    FinanceShoppingCommissionService(
            SalesTeamMapper teamMapper,
            SalesBookingOrderMapper orderMapper,
            FinanceShoppingCommissionRuleMapper ruleMapper,
            FinanceShoppingTeamRuleOverrideMapper overrideMapper,
            FinanceShoppingFeedbackLineMapper feedbackLineMapper,
            FinanceShoppingFeedbackDetailLineMapper feedbackDetailLineMapper,
            FinanceShoppingSettlementMapper settlementMapper,
            FinanceShoppingSettlementLineMapper settlementLineMapper,
            FinanceShoppingCommissionCalculator calculator,
            Clock clock
    ) {
        this.teamMapper = teamMapper;
        this.orderMapper = orderMapper;
        this.ruleMapper = ruleMapper;
        this.overrideMapper = overrideMapper;
        this.feedbackLineMapper = feedbackLineMapper;
        this.feedbackDetailLineMapper = feedbackDetailLineMapper;
        this.settlementMapper = settlementMapper;
        this.settlementLineMapper = settlementLineMapper;
        this.calculator = calculator;
        this.clock = clock;
    }

    /** 查询团队购物佣金总览。 */
    public ShoppingCommissionOverviewResponse overview(Long tenantId, Long teamId) {
        requireTeam(tenantId, teamId);
        EffectiveRule effectiveRule = effectiveRule(tenantId, teamId);
        List<FinanceShoppingFeedbackLineEntity> feedbackEntities = loadFeedbackLines(tenantId, teamId);
        Map<Long, List<ShoppingFeedbackDetailLineResponse>> detailLinesByFeedbackId =
                loadFeedbackDetailLines(tenantId, feedbackEntities)
                        .stream()
                        .collect(Collectors.groupingBy(
                                FinanceShoppingFeedbackDetailLineEntity::getFeedbackLineId,
                                Collectors.mapping(ShoppingFeedbackDetailLineResponse::fromEntity, Collectors.toList())
                        ));
        List<ShoppingFeedbackLineResponse> feedbackLines = feedbackEntities
                .stream()
                .map(item -> ShoppingFeedbackLineResponse.fromEntity(
                        item,
                        detailLinesByFeedbackId.getOrDefault(item.getId(), List.of())
                ))
                .toList();
        FinanceShoppingSettlementEntity settlement = latestSettlement(tenantId, teamId);
        ShoppingSettlementResponse latestSettlement = settlement == null
                ? null
                : ShoppingSettlementResponse.fromEntity(
                        settlement,
                        loadSettlementLineResponses(tenantId, settlement.getId())
                );
        return new ShoppingCommissionOverviewResponse(effectiveRule.response(), feedbackLines, latestSettlement);
    }

    /** 保存单条购物店反馈。 */
    @Transactional
    public ShoppingFeedbackLineResponse saveFeedbackLine(
            Long tenantId,
            Long teamId,
            ShoppingFeedbackLineSaveRequest request,
            String operator
    ) {
        SalesTeamEntity team = requireTeam(tenantId, teamId);
        FinanceShoppingFeedbackLineEntity entity = request.id() == null
                ? new FinanceShoppingFeedbackLineEntity()
                : requireFeedbackLine(tenantId, teamId, request.id());
        String rebateCalcMode = normalizeRebateCalcMode(request.rebateCalcMode());
        List<NormalizedFeedbackDetail> detailLines = normalizeFeedbackDetails(request, rebateCalcMode);
        FeedbackSummary summary = summarizeFeedbackDetails(request.peopleCount(), detailLines);
        entity.setTenantId(tenantId);
        entity.setTeamId(teamId);
        entity.setTeamNo(team.getTeamNo());
        entity.setSupplierId(request.supplierId());
        entity.setShopName(clean(request.shopName()));
        entity.setGuideId(request.guideId());
        entity.setGuideName(clean(request.guideName()));
        entity.setBusinessDate(request.businessDate());
        entity.setPeopleCount(summary.peopleCount());
        entity.setConsumptionAmount(summary.consumptionAmount());
        entity.setCompanyRebateAmount(summary.companyRebateAmount());
        entity.setGuideCommissionAmount(summary.guideCommissionAmount());
        entity.setHeadFeeAmount(summary.headFeeAmount());
        entity.setRebateCalcMode(rebateCalcMode);
        entity.setFeedbackSource("manual");
        entity.setStatus("active");
        entity.setCreatedBy(operator);
        entity.setRemark(clean(request.remark()));
        entity.setIsDeleted(false);
        if (request.id() == null) {
            feedbackLineMapper.insert(entity);
        } else {
            feedbackLineMapper.update(entity, baseFeedbackUpdate(tenantId).eq("id", request.id()));
        }
        List<FinanceShoppingFeedbackDetailLineEntity> savedDetailLines =
                replaceFeedbackDetailLines(tenantId, teamId, entity.getId(), detailLines, operator);
        return ShoppingFeedbackLineResponse.fromEntity(
                entity,
                savedDetailLines.stream().map(ShoppingFeedbackDetailLineResponse::fromEntity).toList()
        );
    }

    /** 作废单条购物反馈。 */
    @Transactional
    public void cancelFeedbackLine(Long tenantId, Long teamId, Long id, String operator) {
        requireFeedbackLine(tenantId, teamId, id);
        FinanceShoppingFeedbackLineEntity update = new FinanceShoppingFeedbackLineEntity();
        update.setStatus("cancelled");
        update.setDeletedBy(operator);
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now(clock));
        feedbackLineMapper.update(update, baseFeedbackUpdate(tenantId).eq("id", id));
        FinanceShoppingFeedbackDetailLineEntity detailUpdate = new FinanceShoppingFeedbackDetailLineEntity();
        detailUpdate.setDeletedBy(operator);
        detailUpdate.setIsDeleted(true);
        detailUpdate.setDeletedAt(OffsetDateTime.now(clock));
        feedbackDetailLineMapper.update(
                detailUpdate,
                new UpdateWrapper<FinanceShoppingFeedbackDetailLineEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("team_id", teamId)
                        .eq("feedback_line_id", id)
                        .eq("is_deleted", false)
        );
    }

    /** 保存团队级购物参考阶梯规则覆盖。 */
    @Transactional
    public ShoppingCommissionRuleResponse saveTeamRuleOverride(
            Long tenantId,
            Long teamId,
            ShoppingCommissionRuleSaveRequest request,
            String operator
    ) {
        SalesTeamEntity team = requireTeam(tenantId, teamId);
        overrideMapper.update(
                supersededOverride(),
                new UpdateWrapper<FinanceShoppingTeamRuleOverrideEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("team_id", teamId)
                        .eq("is_deleted", false)
                        .eq("status", "active")
        );
        FinanceShoppingTeamRuleOverrideEntity entity = new FinanceShoppingTeamRuleOverrideEntity();
        entity.setTenantId(tenantId);
        entity.setTeamId(teamId);
        entity.setTeamNo(team.getTeamNo());
        entity.setThresholdPerCapitaAmount(money(request.thresholdPerCapitaAmount()));
        entity.setBaseCommissionRate(money(request.baseCommissionRate()));
        entity.setTargetCommissionRate(money(request.targetCommissionRate()));
        entity.setLadderCalcMode(FULL_AMOUNT_DIFF);
        entity.setOverrideReason(clean(request.overrideReason()));
        entity.setOverriddenBy(operator);
        entity.setOverriddenAt(OffsetDateTime.now(clock));
        entity.setStatus("active");
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        overrideMapper.insert(entity);
        return ShoppingCommissionRuleResponse.fromOverride(entity);
    }

    /** 重新计算并保存团队购物佣金结算快照。 */
    @Transactional
    public ShoppingSettlementResponse calculateSettlement(Long tenantId, Long teamId, String operator) {
        return calculateSettlement(tenantId, teamId, null, operator);
    }

    /** 重新计算并保存团队购物业绩和公司补佣结算快照。 */
    @Transactional
    public ShoppingSettlementResponse calculateSettlement(
            Long tenantId,
            Long teamId,
            ShoppingSettlementCalculateRequest request,
            String operator
    ) {
        SalesTeamEntity team = requireTeam(tenantId, teamId);
        EffectiveRule effectiveRule = effectiveRule(tenantId, teamId);
        Integer guestCount = Math.max(0, Objects.requireNonNullElse(orderMapper.sumGuestCountByTeam(tenantId, teamId), 0));
        List<FinanceShoppingFeedbackLineEntity> feedbackLines = loadFeedbackLines(tenantId, teamId);
        FinanceShoppingSettlementEntity previousSettlement = latestSettlement(tenantId, teamId);
        ManualGuideBonus manualGuideBonus = resolveManualGuideBonus(request, previousSettlement);
        FinanceShoppingCommissionCalculator.Result result = calculator.calculate(
                guestCount,
                effectiveRule.snapshot(),
                feedbackLines.stream().map(this::toFeedbackAmount).toList(),
                manualGuideBonus.amount()
        );
        supersedeActiveSettlements(tenantId, teamId);
        FinanceShoppingSettlementEntity settlement = createSettlement(
                team,
                tenantId,
                guestCount,
                effectiveRule,
                result,
                manualGuideBonus,
                operator
        );
        settlementMapper.insert(settlement);

        List<ShoppingSettlementLineResponse> responseLines = new ArrayList<>();
        int sortOrder = 1;
        for (FinanceShoppingFeedbackLineEntity feedback : feedbackLines) {
            FinanceShoppingSettlementLineEntity line = createSettlementLine(tenantId, settlement.getId(), feedback, sortOrder++);
            settlementLineMapper.insert(line);
            responseLines.add(ShoppingSettlementLineResponse.fromEntity(line));
        }
        return ShoppingSettlementResponse.fromEntity(settlement, responseLines);
    }

    private FinanceShoppingSettlementEntity createSettlement(
            SalesTeamEntity team,
            Long tenantId,
            Integer guestCount,
            EffectiveRule effectiveRule,
            FinanceShoppingCommissionCalculator.Result result,
            ManualGuideBonus manualGuideBonus,
            String operator
    ) {
        FinanceShoppingSettlementEntity entity = new FinanceShoppingSettlementEntity();
        entity.setTenantId(tenantId);
        entity.setTeamId(team.getId());
        entity.setTeamNo(team.getTeamNo());
        entity.setTeamType(team.getTeamType());
        entity.setBusinessType(team.getBusinessType());
        entity.setDepartureDate(team.getDepartureDate());
        entity.setRuleSource(effectiveRule.ruleSource());
        entity.setGuestCount(guestCount);
        entity.setThresholdPerCapitaAmount(effectiveRule.snapshot().thresholdPerCapitaAmount());
        entity.setBaseCommissionRate(effectiveRule.snapshot().baseCommissionRate());
        entity.setTargetCommissionRate(effectiveRule.snapshot().targetCommissionRate());
        entity.setLadderCalcMode(effectiveRule.snapshot().ladderCalcMode());
        entity.setTotalConsumptionAmount(result.totalConsumptionAmount());
        entity.setPerCapitaConsumptionAmount(result.perCapitaConsumptionAmount());
        entity.setThresholdReached(result.thresholdReached());
        entity.setBaseGuideCommissionAmount(result.baseGuideCommissionAmount());
        entity.setLadderExtraCommissionAmount(result.ladderExtraCommissionAmount());
        entity.setGuideCommissionTotalAmount(result.guideCommissionTotalAmount());
        entity.setManualGuideBonusAmount(manualGuideBonus.amount());
        entity.setManualGuideBonusRemark(manualGuideBonus.remark());
        entity.setCompanyRebateAmount(result.companyRebateAmount());
        entity.setHeadFeeAmount(result.headFeeAmount());
        entity.setInternalCompanyProfitAmount(result.companyProfitAmount());
        entity.setExternalCompanyProfitAmount(BigDecimal.ZERO.setScale(2));
        entity.setCalculatedBy(operator);
        entity.setCalculatedAt(OffsetDateTime.now(clock));
        entity.setStatus("active");
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        return entity;
    }

    private FinanceShoppingSettlementLineEntity createSettlementLine(
            Long tenantId,
            Long settlementId,
            FinanceShoppingFeedbackLineEntity feedback,
            int sortOrder
    ) {
        FinanceShoppingSettlementLineEntity line = new FinanceShoppingSettlementLineEntity();
        line.setTenantId(tenantId);
        line.setSettlementId(settlementId);
        line.setTeamId(feedback.getTeamId());
        line.setFeedbackLineId(feedback.getId());
        line.setSupplierId(feedback.getSupplierId());
        line.setShopName(feedback.getShopName());
        line.setBusinessDate(feedback.getBusinessDate());
        line.setPeopleCount(feedback.getPeopleCount());
        line.setConsumptionAmount(money(feedback.getConsumptionAmount()));
        line.setCompanyRebateAmount(money(feedback.getCompanyRebateAmount()));
        line.setGuideCommissionAmount(money(feedback.getGuideCommissionAmount()));
        line.setHeadFeeAmount(money(feedback.getHeadFeeAmount()));
        line.setLineCompanyProfitAmount(money(feedback.getHeadFeeAmount())
                .add(money(feedback.getCompanyRebateAmount()))
                .setScale(2, RoundingMode.HALF_UP));
        line.setSortOrder(sortOrder);
        line.setIsDeleted(false);
        return line;
    }

    private ManualGuideBonus resolveManualGuideBonus(
            ShoppingSettlementCalculateRequest request,
            FinanceShoppingSettlementEntity previousSettlement
    ) {
        if (request != null) {
            return new ManualGuideBonus(
                    money(request.manualGuideBonusAmount()),
                    clean(request.manualGuideBonusRemark())
            );
        }
        if (previousSettlement == null) {
            return new ManualGuideBonus(BigDecimal.ZERO.setScale(2), null);
        }
        return new ManualGuideBonus(
                money(previousSettlement.getManualGuideBonusAmount()),
                clean(previousSettlement.getManualGuideBonusRemark())
        );
    }

    private FinanceShoppingCommissionCalculator.FeedbackAmount toFeedbackAmount(FinanceShoppingFeedbackLineEntity entity) {
        return new FinanceShoppingCommissionCalculator.FeedbackAmount(
                entity.getId(),
                entity.getShopName(),
                entity.getConsumptionAmount(),
                entity.getCompanyRebateAmount(),
                entity.getGuideCommissionAmount(),
                entity.getHeadFeeAmount(),
                entity.getPeopleCount()
        );
    }

    private EffectiveRule effectiveRule(Long tenantId, Long teamId) {
        FinanceShoppingTeamRuleOverrideEntity override = overrideMapper.selectOne(
                new QueryWrapper<FinanceShoppingTeamRuleOverrideEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("team_id", teamId)
                        .eq("is_deleted", false)
                        .eq("status", "active")
                        .orderByDesc("id")
                        .last("limit 1")
        );
        if (override != null) {
            return new EffectiveRule(
                    "team_override",
                    new FinanceShoppingCommissionCalculator.RuleSnapshot(
                            money(override.getThresholdPerCapitaAmount()),
                            money(override.getBaseCommissionRate()),
                            money(override.getTargetCommissionRate()),
                            override.getLadderCalcMode()
                    ),
                    ShoppingCommissionRuleResponse.fromOverride(override)
            );
        }
        FinanceShoppingCommissionRuleEntity rule = ruleMapper.selectOne(
                new QueryWrapper<FinanceShoppingCommissionRuleEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("status", "active")
                        .orderByDesc("id")
                        .last("limit 1")
        );
        if (rule != null) {
            return new EffectiveRule(
                    "default_rule",
                    new FinanceShoppingCommissionCalculator.RuleSnapshot(
                            money(rule.getThresholdPerCapitaAmount()),
                            money(rule.getBaseCommissionRate()),
                            money(rule.getTargetCommissionRate()),
                            rule.getLadderCalcMode()
                    ),
                    new ShoppingCommissionRuleResponse(
                            "default_rule",
                            money(rule.getThresholdPerCapitaAmount()),
                            money(rule.getBaseCommissionRate()),
                            money(rule.getTargetCommissionRate()),
                            rule.getLadderCalcMode(),
                            null
                    )
            );
        }
        return new EffectiveRule(
                "system_default",
                new FinanceShoppingCommissionCalculator.RuleSnapshot(
                        DEFAULT_THRESHOLD,
                        DEFAULT_BASE_RATE,
                        DEFAULT_TARGET_RATE,
                        FULL_AMOUNT_DIFF
                ),
                new ShoppingCommissionRuleResponse(
                        "system_default",
                        DEFAULT_THRESHOLD,
                        DEFAULT_BASE_RATE,
                        DEFAULT_TARGET_RATE,
                        FULL_AMOUNT_DIFF,
                        null
                )
        );
    }

    private List<FinanceShoppingFeedbackLineEntity> loadFeedbackLines(Long tenantId, Long teamId) {
        return feedbackLineMapper.selectList(new QueryWrapper<FinanceShoppingFeedbackLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .orderByAsc("business_date")
                .orderByAsc("id"));
    }

    private List<FinanceShoppingFeedbackDetailLineEntity> loadFeedbackDetailLines(
            Long tenantId,
            List<FinanceShoppingFeedbackLineEntity> feedbackLines
    ) {
        List<Long> feedbackIds = feedbackLines.stream()
                .map(FinanceShoppingFeedbackLineEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (feedbackIds.isEmpty()) {
            return List.of();
        }
        return feedbackDetailLineMapper.selectList(new QueryWrapper<FinanceShoppingFeedbackDetailLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("feedback_line_id", feedbackIds)
                .orderByAsc("feedback_line_id")
                .orderByAsc("sort_order")
                .orderByAsc("id"));
    }

    private List<FinanceShoppingFeedbackDetailLineEntity> replaceFeedbackDetailLines(
            Long tenantId,
            Long teamId,
            Long feedbackLineId,
            List<NormalizedFeedbackDetail> detailLines,
            String operator
    ) {
        FinanceShoppingFeedbackDetailLineEntity update = new FinanceShoppingFeedbackDetailLineEntity();
        update.setDeletedBy(operator);
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now(clock));
        feedbackDetailLineMapper.update(
                update,
                new UpdateWrapper<FinanceShoppingFeedbackDetailLineEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("team_id", teamId)
                        .eq("feedback_line_id", feedbackLineId)
                        .eq("is_deleted", false)
        );
        List<FinanceShoppingFeedbackDetailLineEntity> saved = new ArrayList<>();
        for (NormalizedFeedbackDetail detail : detailLines) {
            FinanceShoppingFeedbackDetailLineEntity entity = new FinanceShoppingFeedbackDetailLineEntity();
            entity.setTenantId(tenantId);
            entity.setTeamId(teamId);
            entity.setFeedbackLineId(feedbackLineId);
            entity.setCategoryName(detail.categoryName());
            entity.setPeopleCount(detail.peopleCount());
            entity.setHeadFeeAmount(detail.headFeeAmount());
            entity.setConsumptionAmount(detail.consumptionAmount());
            entity.setCompanyRebateRate(detail.companyRebateRate());
            entity.setCompanyRebateAmount(detail.companyRebateAmount());
            entity.setGuideCommissionRate(detail.guideCommissionRate());
            entity.setGuideCommissionAmount(detail.guideCommissionAmount());
            entity.setCashAmount(detail.cashAmount());
            entity.setSortOrder(detail.sortOrder());
            entity.setCreatedBy(operator);
            entity.setRemark(detail.remark());
            entity.setIsDeleted(false);
            feedbackDetailLineMapper.insert(entity);
            saved.add(entity);
        }
        return saved;
    }

    private String normalizeRebateCalcMode(String value) {
        if (MODE_CATEGORY.equals(value)) {
            return MODE_CATEGORY;
        }
        return MODE_TOTAL;
    }

    private List<NormalizedFeedbackDetail> normalizeFeedbackDetails(
            ShoppingFeedbackLineSaveRequest request,
            String rebateCalcMode
    ) {
        if (MODE_TOTAL.equals(rebateCalcMode)) {
            return List.of(new NormalizedFeedbackDetail(
                    CATEGORY_COMPREHENSIVE,
                    safeCount(request.peopleCount()),
                    money(request.headFeeAmount()),
                    money(request.consumptionAmount()),
                    BigDecimal.ZERO.setScale(2),
                    money(request.companyRebateAmount()),
                    BigDecimal.ZERO.setScale(2),
                    money(request.guideCommissionAmount()),
                    BigDecimal.ZERO.setScale(2),
                    clean(request.remark()),
                    1
            ));
        }
        List<ShoppingFeedbackDetailLineSaveRequest> sourceLines = request.detailLines() == null
                ? List.of()
                : request.detailLines();
        if (sourceLines.isEmpty()) {
            throw new BizException("按品类返佣至少需要一条消费详情");
        }
        List<NormalizedFeedbackDetail> result = new ArrayList<>();
        int sortOrder = 1;
        for (ShoppingFeedbackDetailLineSaveRequest item : sourceLines) {
            String categoryName = clean(item.categoryName());
            if (!StringUtils.hasText(categoryName)) {
                throw new BizException("请填写购物品类");
            }
            BigDecimal consumption = money(item.consumptionAmount());
            BigDecimal companyRate = percent(item.companyRebateRate());
            BigDecimal guideRate = percent(item.guideCommissionRate());
            result.add(new NormalizedFeedbackDetail(
                    categoryName,
                    safeCount(item.peopleCount()),
                    money(item.headFeeAmount()),
                    consumption,
                    companyRate,
                    rateAmountOrManual(consumption, companyRate, item.companyRebateAmount()),
                    guideRate,
                    rateAmountOrManual(consumption, guideRate, item.guideCommissionAmount()),
                    money(item.cashAmount()),
                    clean(item.remark()),
                    item.sortOrder() == null || item.sortOrder() < 1 ? sortOrder : item.sortOrder()
            ));
            sortOrder++;
        }
        return result;
    }

    private FeedbackSummary summarizeFeedbackDetails(
            Integer requestPeopleCount,
            List<NormalizedFeedbackDetail> detailLines
    ) {
        int peopleCount = safeCount(requestPeopleCount);
        if (peopleCount == 0) {
            peopleCount = detailLines.stream()
                    .map(NormalizedFeedbackDetail::peopleCount)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
        }
        return new FeedbackSummary(
                peopleCount,
                sumDetails(detailLines, NormalizedFeedbackDetail::consumptionAmount),
                sumDetails(detailLines, NormalizedFeedbackDetail::companyRebateAmount),
                sumDetails(detailLines, NormalizedFeedbackDetail::guideCommissionAmount),
                sumDetails(detailLines, NormalizedFeedbackDetail::headFeeAmount)
        );
    }

    private BigDecimal sumDetails(List<NormalizedFeedbackDetail> items, DetailAmountExtractor extractor) {
        return items.stream()
                .map(extractor::amount)
                .map(this::money)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private FinanceShoppingSettlementEntity latestSettlement(Long tenantId, Long teamId) {
        return settlementMapper.selectOne(new QueryWrapper<FinanceShoppingSettlementEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .orderByDesc("calculated_at")
                .orderByDesc("id")
                .last("limit 1"));
    }

    private List<ShoppingSettlementLineResponse> loadSettlementLineResponses(Long tenantId, Long settlementId) {
        return settlementLineMapper.selectList(new QueryWrapper<FinanceShoppingSettlementLineEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("settlement_id", settlementId)
                        .eq("is_deleted", false)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .map(ShoppingSettlementLineResponse::fromEntity)
                .toList();
    }

    private void supersedeActiveSettlements(Long tenantId, Long teamId) {
        FinanceShoppingSettlementEntity update = new FinanceShoppingSettlementEntity();
        update.setStatus("superseded");
        settlementMapper.update(update, new UpdateWrapper<FinanceShoppingSettlementEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .eq("status", "active"));
    }

    private FinanceShoppingTeamRuleOverrideEntity supersededOverride() {
        FinanceShoppingTeamRuleOverrideEntity update = new FinanceShoppingTeamRuleOverrideEntity();
        update.setStatus("superseded");
        return update;
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

    private FinanceShoppingFeedbackLineEntity requireFeedbackLine(Long tenantId, Long teamId, Long id) {
        FinanceShoppingFeedbackLineEntity entity = feedbackLineMapper.selectOne(new QueryWrapper<FinanceShoppingFeedbackLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("id", id)
                .eq("is_deleted", false)
                .last("limit 1"));
        if (entity == null) {
            throw new BizException("购物反馈明细不存在或已删除");
        }
        return entity;
    }

    private UpdateWrapper<FinanceShoppingFeedbackLineEntity> baseFeedbackUpdate(Long tenantId) {
        return new UpdateWrapper<FinanceShoppingFeedbackLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal rateAmountOrManual(BigDecimal consumptionAmount, BigDecimal ratePercent, BigDecimal manualAmount) {
        if (money(consumptionAmount).compareTo(BigDecimal.ZERO) > 0
                && percent(ratePercent).compareTo(BigDecimal.ZERO) > 0) {
            return money(consumptionAmount)
                    .multiply(percent(ratePercent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return money(manualAmount);
    }

    private int safeCount(Integer value) {
        return Math.max(Objects.requireNonNullElse(value, 0), 0);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private interface DetailAmountExtractor {
        BigDecimal amount(NormalizedFeedbackDetail item);
    }

    private record EffectiveRule(
            String ruleSource,
            FinanceShoppingCommissionCalculator.RuleSnapshot snapshot,
            ShoppingCommissionRuleResponse response
    ) {
    }

    private record ManualGuideBonus(BigDecimal amount, String remark) {
    }

    private record NormalizedFeedbackDetail(
            String categoryName,
            Integer peopleCount,
            BigDecimal headFeeAmount,
            BigDecimal consumptionAmount,
            BigDecimal companyRebateRate,
            BigDecimal companyRebateAmount,
            BigDecimal guideCommissionRate,
            BigDecimal guideCommissionAmount,
            BigDecimal cashAmount,
            String remark,
            Integer sortOrder
    ) {
    }

    private record FeedbackSummary(
            Integer peopleCount,
            BigDecimal consumptionAmount,
            BigDecimal companyRebateAmount,
            BigDecimal guideCommissionAmount,
            BigDecimal headFeeAmount
    ) {
    }
}
