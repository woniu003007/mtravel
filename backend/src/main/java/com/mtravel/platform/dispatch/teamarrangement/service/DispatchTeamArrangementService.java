package com.mtravel.platform.dispatch.teamarrangement.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementOrderAllocationResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementPriceLineRequest;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementPriceLineResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSectionStatusResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSummaryResponse;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementFlowRecordEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementOrderAllocationEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementPriceLineEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementSectionStatusEntity;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementAllocationMode;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementFlowType;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementSettlementType;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementSplitMode;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementType;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementFlowRecordMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementOrderAllocationMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementPriceLineMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementSectionStatusMapper;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingSettlementEntity;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingSettlementMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderRole;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderStatus;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.ordertransfer.entity.SalesOrderTransferLogEntity;
import com.mtravel.platform.sales.ordertransfer.mapper.SalesOrderTransferLogMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.service.SalesTeamListSummaryRefreshService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 正式团队安排成本服务。
 *
 * <p>本服务集中处理团队安排实际成本的保存、订单归属拆分、导游报账同步和删除锁定。
 * 这些规则会影响应付、导游报账、计调审核、财务审核、团队毛利和资源采购统计。</p>
 */
@Service
public class DispatchTeamArrangementService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String STATUS_ACTIVE = "active";
    private static final List<String> SECTION_STATUSES = List.of("pending", "none", "done");
    private static final String STAGE_ARRANGEMENT = "arrangement";
    private static final String FLOW_STATUS_SYNCED = "synced";
    private static final String FLOW_SYNC_NO_GUIDE_REPORT = "no_guide_report";
    private static final List<String> SUMMARY_TYPES = List.of(
            "traffic", "hotel", "vehicle", "scenic", "meal", "other",
            "ground_agent", "extra_fee", "optional", "shopping"
    );
    private static final List<String> REGULAR_COST_TYPES = List.of(
            "traffic", "hotel", "vehicle", "scenic", "meal", "other", "ground_agent", "extra_fee"
    );
    private static final Map<String, String> SUMMARY_TYPE_LABELS = Map.ofEntries(
            Map.entry("traffic", "大交通"),
            Map.entry("hotel", "住宿"),
            Map.entry("vehicle", "用车"),
            Map.entry("scenic", "景区"),
            Map.entry("meal", "用餐"),
            Map.entry("other", "其它"),
            Map.entry("ground_agent", "地接"),
            Map.entry("extra_fee", "附加"),
            Map.entry("optional", "自费"),
            Map.entry("shopping", "购物")
    );

    private final DispatchTeamArrangementMapper arrangementMapper;
    private final DispatchTeamArrangementPriceLineMapper priceLineMapper;
    private final DispatchTeamArrangementOrderAllocationMapper allocationMapper;
    private final DispatchTeamArrangementFlowRecordMapper flowRecordMapper;
    private final DispatchTeamArrangementSectionStatusMapper sectionStatusMapper;
    private final SalesTeamMapper teamMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final SalesOrderTransferLogMapper transferLogMapper;
    private final DispatchTeamGuideMapper guideMapper;
    private final FinanceShoppingSettlementMapper shoppingSettlementMapper;
    private SalesTeamListSummaryRefreshService teamListSummaryRefreshService;

    /**
     * 构造正式团队安排成本服务。
     */
    public DispatchTeamArrangementService(
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementPriceLineMapper priceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper allocationMapper,
            DispatchTeamArrangementFlowRecordMapper flowRecordMapper,
            SalesTeamMapper teamMapper,
            SalesBookingOrderMapper orderMapper
    ) {
        this(
                arrangementMapper,
                priceLineMapper,
                allocationMapper,
                flowRecordMapper,
                null,
                teamMapper,
                orderMapper,
                null,
                null,
                null,
                null
        );
    }

    /**
     * 构造正式团队安排成本服务。
     */
    public DispatchTeamArrangementService(
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementPriceLineMapper priceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper allocationMapper,
            DispatchTeamArrangementFlowRecordMapper flowRecordMapper,
            DispatchTeamArrangementSectionStatusMapper sectionStatusMapper,
            SalesTeamMapper teamMapper,
            SalesBookingOrderMapper orderMapper,
            DispatchTeamGuideMapper guideMapper
    ) {
        this(
                arrangementMapper,
                priceLineMapper,
                allocationMapper,
                flowRecordMapper,
                sectionStatusMapper,
                teamMapper,
                orderMapper,
                null,
                guideMapper,
                null,
                null
        );
    }

    /**
     * 构造正式团队安排成本服务。
     */
    @Autowired
    public DispatchTeamArrangementService(
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementPriceLineMapper priceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper allocationMapper,
            DispatchTeamArrangementFlowRecordMapper flowRecordMapper,
            DispatchTeamArrangementSectionStatusMapper sectionStatusMapper,
            SalesTeamMapper teamMapper,
            SalesBookingOrderMapper orderMapper,
            SalesOrderTransferLogMapper transferLogMapper,
            DispatchTeamGuideMapper guideMapper,
            FinanceShoppingSettlementMapper shoppingSettlementMapper
    ) {
        this(
                arrangementMapper,
                priceLineMapper,
                allocationMapper,
                flowRecordMapper,
                sectionStatusMapper,
                teamMapper,
                orderMapper,
                transferLogMapper,
                guideMapper,
                shoppingSettlementMapper,
                null
        );
    }

    /**
     * 构造正式团队安排成本服务。
     */
    public DispatchTeamArrangementService(
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementPriceLineMapper priceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper allocationMapper,
            DispatchTeamArrangementFlowRecordMapper flowRecordMapper,
            DispatchTeamArrangementSectionStatusMapper sectionStatusMapper,
            SalesTeamMapper teamMapper,
            SalesBookingOrderMapper orderMapper,
            SalesOrderTransferLogMapper transferLogMapper,
            DispatchTeamGuideMapper guideMapper,
            FinanceShoppingSettlementMapper shoppingSettlementMapper,
            SalesTeamListSummaryRefreshService teamListSummaryRefreshService
    ) {
        this.arrangementMapper = arrangementMapper;
        this.priceLineMapper = priceLineMapper;
        this.allocationMapper = allocationMapper;
        this.flowRecordMapper = flowRecordMapper;
        this.sectionStatusMapper = sectionStatusMapper;
        this.teamMapper = teamMapper;
        this.orderMapper = orderMapper;
        this.transferLogMapper = transferLogMapper;
        this.guideMapper = guideMapper;
        this.shoppingSettlementMapper = shoppingSettlementMapper;
        this.teamListSummaryRefreshService = teamListSummaryRefreshService;
    }

    /**
     * 注入团队列表汇总刷新服务。
     *
     * <p>保留原有运行时构造器签名，避免破坏既有测试和手工构造；Spring 启动时通过该 setter 补充缓存刷新能力。</p>
     */
    @Autowired
    public void setTeamListSummaryRefreshService(SalesTeamListSummaryRefreshService teamListSummaryRefreshService) {
        this.teamListSummaryRefreshService = teamListSummaryRefreshService;
    }

    /**
     * 查询团队正式安排列表。
     *
     * @param teamId 团队 ID
     * @param arrangementType 资源类型，可为空
     * @param tenantId 当前租户 ID
     * @return 安排列表
     */
    public List<TeamArrangementResponse> list(Long teamId, String arrangementType, Long tenantId) {
        QueryWrapper<DispatchTeamArrangementEntity> wrapper = baseArrangementQuery(tenantId)
                .eq("team_id", teamId)
                .eq(StringUtils.hasText(arrangementType), "arrangement_type", arrangementType)
                .orderByAsc("arrangement_type")
                .orderByAsc("business_date")
                .orderByAsc("id");
        List<DispatchTeamArrangementEntity> arrangements = arrangementMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(arrangements)) {
            return List.of();
        }
        List<Long> ids = arrangements.stream().map(DispatchTeamArrangementEntity::getId).toList();
        Map<Long, List<TeamArrangementPriceLineResponse>> priceLinesByArrangement = priceLineMapper.selectList(
                        new QueryWrapper<DispatchTeamArrangementPriceLineEntity>()
                                .eq("tenant_id", tenantId)
                                .eq("is_deleted", false)
                                .in("arrangement_id", ids)
                                .orderByAsc("arrangement_id")
                                .orderByAsc("sort_order")
                                .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        DispatchTeamArrangementPriceLineEntity::getArrangementId,
                        LinkedHashMap::new,
                        Collectors.mapping(TeamArrangementPriceLineResponse::fromEntity, Collectors.toList())
                ));
        Map<Long, List<TeamArrangementOrderAllocationResponse>> allocationsByArrangement = allocationMapper.selectList(
                        new QueryWrapper<DispatchTeamArrangementOrderAllocationEntity>()
                                .eq("tenant_id", tenantId)
                                .eq("is_deleted", false)
                                .in("arrangement_id", ids)
                                .orderByAsc("arrangement_id")
                                .orderByAsc("sort_order")
                                .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        DispatchTeamArrangementOrderAllocationEntity::getArrangementId,
                        LinkedHashMap::new,
                        Collectors.mapping(TeamArrangementOrderAllocationResponse::fromEntity, Collectors.toList())
                ));
        Map<Long, Long> flowCounts = new LinkedHashMap<>();
        flowRecordMapper.selectList(new QueryWrapper<DispatchTeamArrangementFlowRecordEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("arrangement_id", ids)
                        .and(flowWrapper -> flowWrapper
                                .ne("sync_source", FLOW_SYNC_NO_GUIDE_REPORT)
                                .or()
                                .isNull("sync_source")))
                .forEach(item -> flowCounts.merge(item.getArrangementId(), 1L, Long::sum));

        return arrangements.stream()
                .map(item -> TeamArrangementResponse.fromEntity(
                        item,
                        priceLinesByArrangement.getOrDefault(item.getId(), List.of()),
                        allocationsByArrangement.getOrDefault(item.getId(), List.of()),
                        flowCounts.getOrDefault(item.getId(), 0L) == 0
                ))
                .toList();
    }

    /**
     * 查询团队安排页后端权威金额汇总。
     *
     * <p>团队安排页的应收、成本总览和预算利润属于敏感经营金额，不能由前端按页面数组自行聚合。
     * 这里统一套用老系统预算利润口径，仅过滤取消订单；历史拼出来源订单继续按来源团订单参与统计。</p>
     *
     * @param teamId 团队 ID
     * @param tenantId 当前租户 ID
     * @return 团队安排页金额汇总
     */
    public TeamArrangementSummaryResponse summary(Long teamId, Long tenantId) {
        requireTeam(teamId, tenantId);
        List<SalesBookingOrderEntity> orders = effectiveOrders(teamId, tenantId);
        List<DispatchTeamArrangementEntity> arrangements = summaryArrangements(teamId, tenantId);
        List<DispatchTeamGuideEntity> guides = summaryGuides(teamId, tenantId);

        BigDecimal orderReceivable = sumOrders(orders, SalesBookingOrderEntity::getReceivableAmount);
        BigDecimal orderReceived = sumOrders(orders, SalesBookingOrderEntity::getReceivedAmount);
        BigDecimal orderBalance = sumOrders(orders, SalesBookingOrderEntity::getBalanceAmount);
        BigDecimal regularCost = arrangements.stream()
                .filter(item -> REGULAR_COST_TYPES.contains(item.getArrangementType()))
                .map(this::arrangementTotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal optionalProfit = arrangements.stream()
                .filter(item -> "optional".equals(item.getArrangementType()))
                .map(item -> money(item.getSaleAmount())
                        .subtract(money(item.getCostAmount()))
                        .subtract(money(item.getGuideCommissionAmount())))
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal shoppingProfit = shoppingProfitAmount(teamId, tenantId, arrangements);
        BigDecimal guideFee = sumGuides(guides, DispatchTeamGuideEntity::getGuideFee);
        BigDecimal guideOperationFee = sumGuides(guides, DispatchTeamGuideEntity::getOperationFee);
        BigDecimal guideImprest = sumGuides(guides, DispatchTeamGuideEntity::getImprestAmount);
        BigDecimal budgetProfit = orderReceivable
                .add(optionalProfit)
                .add(shoppingProfit)
                .subtract(regularCost)
                .subtract(guideFee)
                .setScale(2, RoundingMode.HALF_UP);

        return new TeamArrangementSummaryResponse(
                orderReceivable,
                orderReceived,
                orderBalance,
                regularCost,
                optionalProfit,
                shoppingProfit,
                guideFee,
                guideOperationFee,
                guideImprest,
                budgetProfit,
                costColumns(arrangements, guideFee, guideOperationFee, guideImprest),
                sectionSummaries(arrangements)
        );
    }

    /**
     * 计算购物公司利润。
     *
     * <p>购物店反馈完成并生成结算快照后，优先使用结算快照中的内账利润。还没有结算快照时，
     * 回退到团队安排中的购物预估利润，保证排团阶段预算利润仍可展示。</p>
     */
    private BigDecimal shoppingProfitAmount(
            Long teamId,
            Long tenantId,
            List<DispatchTeamArrangementEntity> arrangements
    ) {
        if (shoppingSettlementMapper != null) {
            FinanceShoppingSettlementEntity settlement = shoppingSettlementMapper.selectOne(
                    new QueryWrapper<FinanceShoppingSettlementEntity>()
                            .eq("tenant_id", tenantId)
                            .eq("team_id", teamId)
                            .eq("is_deleted", false)
                            .eq("status", STATUS_ACTIVE)
                            .orderByDesc("calculated_at")
                            .orderByDesc("id")
                            .last("limit 1")
            );
            if (settlement != null) {
                return money(settlement.getInternalCompanyProfitAmount());
            }
        }
        return arrangements.stream()
                .filter(item -> "shopping".equals(item.getArrangementType()))
                .map(item -> money(item.getHeadFeeAmount())
                        .add(money(item.getCompanyRebateAmount()))
                        .subtract(money(item.getGuideCommissionAmount())))
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 保存正式团队安排。
     *
     * @param teamId 团队 ID
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 保存结果；多订单均摊时包含多条安排 ID
     */
    @Transactional
    public TeamArrangementSaveResponse save(Long teamId, TeamArrangementSaveRequest request, Long tenantId, String operator) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        DispatchArrangementType type = DispatchArrangementType.fromValue(request.arrangementType());
        DispatchArrangementAllocationMode allocationMode =
                DispatchArrangementAllocationMode.fromValueOrDefault(request.allocationMode());
        validateNoGuideReport(request);
        if (allocationMode == DispatchArrangementAllocationMode.MULTI_ORDER_AVERAGE) {
            if (request.arrangementId() != null) {
                throw new BizException("多订单均摊成本只能新增，不能直接编辑已拆分记录");
            }
            TeamArrangementSaveResponse response = saveMultiOrderAverage(team, type, request, tenantId, operator);
            refreshTeamListSummary(teamId, tenantId);
            return response;
        }
        if (request.arrangementId() != null) {
            softDeleteArrangementTree(teamId, request.arrangementId(), tenantId, operator);
        }
        DispatchTeamArrangementEntity entity = buildArrangement(
                team,
                type.getValue(),
                allocationMode.getValue(),
                null,
                null,
                request,
                totalAmount(request),
                money(request.cashAmount()),
                creditAmount(request, totalAmount(request), money(request.cashAmount()), money(request.prepaidAmount())),
                tenantId,
                operator
        );
        arrangementMapper.insert(entity);
        savePriceLines(entity, request.priceLines(), tenantId, operator, null);
        saveSingleAllocation(entity, request, totalAmount(request), tenantId, operator);
        syncNoGuideReportFlows(entity, tenantId, operator);
        refreshTeamListSummary(teamId, tenantId);
        return new TeamArrangementSaveResponse(entity.getId(), List.of(entity.getId()));
    }

    /**
     * 删除正式团队安排。
     *
     * <p>只有“无需导游报账”自动同步流水时允许随录错安排一起作废；
     * 已进入人工导游报账、计调审核或财务审核的安排不允许直接删除，避免财务查账断链。</p>
     */
    @Transactional
    public void delete(Long teamId, Long arrangementId, Long tenantId, String operator) {
        DispatchTeamArrangementEntity current = arrangementMapper.selectOne(baseArrangementQuery(tenantId)
                .eq("team_id", teamId)
                .eq("id", arrangementId));
        if (current == null) {
            throw new BizException("团队安排不存在或已删除");
        }
        if (activeFlowCount(arrangementId, tenantId) > 0) {
            throw new BizException("该安排已进入人工导游报账或审核流程，不能直接删除");
        }
        softDeleteArrangementTree(teamId, arrangementId, tenantId, operator);
        refreshTeamListSummary(teamId, tenantId);
    }

    /**
     * 查询团队安排分类流程状态。
     *
     * @param teamId 团队 ID
     * @param tenantId 当前租户 ID
     * @return 已保存的分类流程状态
     */
    public List<TeamArrangementSectionStatusResponse> listSectionStatuses(Long teamId, Long tenantId) {
        requireTeam(teamId, tenantId);
        if (sectionStatusMapper == null) {
            return List.of();
        }
        return sectionStatusMapper.selectList(baseSectionStatusQuery(tenantId)
                        .eq("team_id", teamId)
                        .orderByAsc("arrangement_type")
                        .orderByAsc("id"))
                .stream()
                .map(TeamArrangementSectionStatusResponse::fromEntity)
                .toList();
    }

    /**
     * 保存团队安排分类流程状态。
     *
     * <p>该状态驱动团队管理列表的绿色/黄色标记；资源确认字段 confirmed 只表示供应商确认。</p>
     */
    @Transactional
    public TeamArrangementSectionStatusResponse saveSectionStatus(
            Long teamId,
            String arrangementType,
            String status,
            Long tenantId,
            String operator
    ) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        DispatchArrangementType type = DispatchArrangementType.fromValue(arrangementType);
        String cleanStatus = cleanRequired(status);
        if (!SECTION_STATUSES.contains(cleanStatus)) {
            throw new BizException("团队安排分类状态不合法");
        }
        if (sectionStatusMapper == null) {
            throw new BizException("团队安排分类状态未接入");
        }
        DispatchTeamArrangementSectionStatusEntity current = sectionStatusMapper.selectOne(baseSectionStatusQuery(tenantId)
                .eq("team_id", teamId)
                .eq("arrangement_type", type.getValue()));
        if (current == null) {
            DispatchTeamArrangementSectionStatusEntity entity = new DispatchTeamArrangementSectionStatusEntity();
            entity.setTenantId(tenantId);
            entity.setTeamId(team.getId());
            entity.setTeamNo(team.getTeamNo());
            entity.setTeamType(team.getTeamType());
            entity.setArrangementType(type.getValue());
            entity.setStatus(cleanStatus);
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            sectionStatusMapper.insert(entity);
            refreshTeamListSummary(teamId, tenantId);
            return TeamArrangementSectionStatusResponse.fromEntity(entity);
        }
        DispatchTeamArrangementSectionStatusEntity update = new DispatchTeamArrangementSectionStatusEntity();
        update.setStatus(cleanStatus);
        sectionStatusMapper.update(update, new UpdateWrapper<DispatchTeamArrangementSectionStatusEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", current.getId())
                .eq("is_deleted", false));
        current.setStatus(cleanStatus);
        refreshTeamListSummary(teamId, tenantId);
        return TeamArrangementSectionStatusResponse.fromEntity(current);
    }

    /**
     * 刷新团队列表缓存中的资源安排图标状态。
     *
     * <p>团队安排和分类状态改变后，列表页的酒店、用车、景区等计划状态必须从事实表重算，避免缓存继续显示旧图标。</p>
     */
    private void refreshTeamListSummary(Long teamId, Long tenantId) {
        if (teamListSummaryRefreshService != null) {
            teamListSummaryRefreshService.refresh(teamId, tenantId);
        }
    }

    /** 保存多订单均摊成本，按旧系统口径拆成多条单订单成本记录。 */
    private TeamArrangementSaveResponse saveMultiOrderAverage(
            SalesTeamEntity team,
            DispatchArrangementType type,
            TeamArrangementSaveRequest request,
            Long tenantId,
            String operator
    ) {
        if (CollectionUtils.isEmpty(request.selectedOrderIds()) || request.selectedOrderIds().size() < 2) {
            throw new BizException("多订单均摊成本至少选择两个订单");
        }
        if (request.priceLines() == null || request.priceLines().size() != 1) {
            throw new BizException("多订单均摊成本时，价格信息只能保留一条记录");
        }
        DispatchArrangementSplitMode splitMode = DispatchArrangementSplitMode.fromValue(request.multiOrderSplitMode());
        List<SalesBookingOrderEntity> orders = loadOrders(team.getId(), request.selectedOrderIds(), tenantId);
        if (orders.size() != request.selectedOrderIds().size()) {
            throw new BizException("选择的订单不存在或不属于当前团队");
        }
        BigDecimal total = totalAmount(request);
        List<BigDecimal> splitAmounts = splitAmounts(total, orders, splitMode);
        String batchNo = "TA" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        List<Long> savedIds = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            BigDecimal itemTotal = splitAmounts.get(i);
            BigDecimal itemCash = proportionalAmount(money(request.cashAmount()), total, itemTotal, i, orders.size(), splitAmounts);
            BigDecimal itemPrepaid = proportionalAmount(money(request.prepaidAmount()), total, itemTotal, i, orders.size(), splitAmounts);
            BigDecimal itemCredit = itemTotal.subtract(itemCash).subtract(itemPrepaid).max(ZERO).setScale(2, RoundingMode.HALF_UP);
            DispatchTeamArrangementEntity entity = buildArrangement(
                    team,
                    type.getValue(),
                    DispatchArrangementAllocationMode.GROUP_ORDER_AVERAGE.getValue(),
                    splitMode.getValue(),
                    batchNo,
                    request,
                    itemTotal,
                    itemCash,
                    itemCredit,
                    tenantId,
                    operator
            );
            arrangementMapper.insert(entity);
            savePriceLines(entity, request.priceLines(), tenantId, operator, itemTotal);
            saveOrderAllocation(entity, orders.get(i), total, itemTotal, splitMode.getValue(), batchNo, i + 1, tenantId, operator);
            syncNoGuideReportFlows(entity, tenantId, operator);
            savedIds.add(entity.getId());
        }
        return new TeamArrangementSaveResponse(savedIds.get(0), savedIds);
    }

    /** 保存单条安排的订单归属。 */
    private void saveSingleAllocation(
            DispatchTeamArrangementEntity entity,
            TeamArrangementSaveRequest request,
            BigDecimal originalAmount,
            Long tenantId,
            String operator
    ) {
        List<Long> orderIds = request.selectedOrderIds() == null
                ? List.of()
                : request.selectedOrderIds().stream().filter(id -> id != null && id > 0).distinct().toList();
        if (orderIds.isEmpty()) {
            DispatchTeamArrangementOrderAllocationEntity allocation = newAllocationBase(entity, originalAmount, entity.getTotalAmount(), tenantId, operator);
            allocation.setAllocationScope("team");
            allocation.setSortOrder(1);
            allocationMapper.insert(allocation);
            return;
        }
        if (orderIds.size() > 1) {
            throw new BizException("全团/订单均摊只能选择一个订单；多个订单请使用多订单均摊成本");
        }
        SalesBookingOrderEntity order = loadOrders(entity.getTeamId(), orderIds, tenantId).stream().findFirst()
                .orElseThrow(() -> new BizException("选择的订单不存在或不属于当前团队"));
        saveOrderAllocation(entity, order, originalAmount, entity.getTotalAmount(), null, null, 1, tenantId, operator);
    }

    /** 保存订单归属明细。 */
    private void saveOrderAllocation(
            DispatchTeamArrangementEntity entity,
            SalesBookingOrderEntity order,
            BigDecimal originalAmount,
            BigDecimal allocationAmount,
            String splitMode,
            String splitBatchNo,
            int sortOrder,
            Long tenantId,
            String operator
    ) {
        DispatchTeamArrangementOrderAllocationEntity allocation =
                newAllocationBase(entity, originalAmount, allocationAmount, tenantId, operator);
        allocation.setAllocationScope("order");
        allocation.setOrderId(order.getId());
        allocation.setOrderNo(order.getOrderNo());
        allocation.setCustomerId(order.getCustomerId());
        allocation.setCustomerName(order.getCustomerName());
        allocation.setGuestCount(number(order.getGuestCount()));
        allocation.setSplitMode(splitMode);
        allocation.setSplitBatchNo(splitBatchNo);
        allocation.setSortOrder(sortOrder);
        allocationMapper.insert(allocation);
    }

    /** 构建订单归属明细公共字段。 */
    private DispatchTeamArrangementOrderAllocationEntity newAllocationBase(
            DispatchTeamArrangementEntity entity,
            BigDecimal originalAmount,
            BigDecimal allocationAmount,
            Long tenantId,
            String operator
    ) {
        DispatchTeamArrangementOrderAllocationEntity allocation = new DispatchTeamArrangementOrderAllocationEntity();
        allocation.setTenantId(tenantId);
        allocation.setArrangementId(entity.getId());
        allocation.setTeamId(entity.getTeamId());
        allocation.setTeamNo(entity.getTeamNo());
        allocation.setAllocationMode(entity.getAllocationMode());
        allocation.setOriginalAmount(originalAmount);
        allocation.setAllocationAmount(allocationAmount);
        allocation.setCreatedBy(operator);
        allocation.setIsDeleted(false);
        return allocation;
    }

    /** 构建安排主记录。 */
    private DispatchTeamArrangementEntity buildArrangement(
            SalesTeamEntity team,
            String arrangementType,
            String allocationMode,
            String splitMode,
            String splitBatchNo,
            TeamArrangementSaveRequest request,
            BigDecimal total,
            BigDecimal cash,
            BigDecimal credit,
            Long tenantId,
            String operator
    ) {
        DispatchTeamArrangementEntity entity = new DispatchTeamArrangementEntity();
        entity.setTenantId(tenantId);
        entity.setTeamId(team.getId());
        entity.setTeamNo(team.getTeamNo());
        entity.setTeamType(team.getTeamType());
        entity.setBusinessType(team.getBusinessType());
        entity.setDepartmentId(team.getDepartmentId());
        entity.setDepartmentName(team.getDepartmentName());
        entity.setOperatorEmployeeId(team.getOperatorEmployeeId());
        entity.setOperatorEmployeeName(team.getOperatorEmployeeName());
        entity.setArrangementType(arrangementType);
        entity.setItemName(cleanRequired(request.itemName()));
        entity.setArrangementContent(clean(request.arrangementContent()));
        entity.setAllocationMode(allocationMode);
        entity.setSplitMode(splitMode);
        entity.setSplitBatchNo(splitBatchNo);
        entity.setScheduleStartDay(clean(request.scheduleStartDay()));
        entity.setScheduleEndDay(clean(request.scheduleEndDay()));
        entity.setBusinessDate(team.getDepartureDate());
        entity.setDeparturePlace(clean(request.departurePlace()));
        entity.setArrivalPlace(clean(request.arrivalPlace()));
        entity.setDaysCount(number(request.daysCount()));
        entity.setResourceName(clean(request.resourceName()));
        entity.setSupplierId(request.supplierId());
        entity.setSupplierName(clean(request.supplierName()));
        entity.setTrafficType(clean(request.trafficType()));
        entity.setVehicleType(clean(request.vehicleType()));
        entity.setDriverName(clean(request.driverName()));
        entity.setVehiclePlate(clean(request.vehiclePlate()));
        entity.setResponsibleEmployeeId(request.responsibleEmployeeId());
        entity.setResponsibleEmployeeName(clean(request.responsibleEmployeeName()));
        entity.setSettlementType(DispatchArrangementSettlementType.fromValueOrDefault(request.settlementType()).getValue());
        entity.setMealType(clean(request.mealType()));
        entity.setFundIncluded(clean(request.fundIncluded()));
        entity.setConfirmed(Boolean.TRUE.equals(request.confirmed()));
        entity.setConfirmationNo(clean(request.confirmationNo()));
        entity.setGuideId(request.guideId());
        entity.setGuideName(clean(request.guideName()));
        entity.setTotalAmount(total);
        entity.setCashAmount(cash);
        entity.setCreditAmount(credit);
        entity.setPrepaidAmount(money(request.prepaidAmount()));
        entity.setSaleAmount(money(request.saleAmount()));
        entity.setCostAmount(money(request.costAmount()).compareTo(ZERO) > 0 ? money(request.costAmount()) : total);
        entity.setGuideCommissionAmount(money(request.guideCommissionAmount()));
        entity.setCompanyRebateAmount(money(request.companyRebateAmount()));
        entity.setHeadFeeAmount(money(request.headFeeAmount()));
        entity.setConsumptionAmount(money(request.consumptionAmount()));
        entity.setPeopleCount(money(request.peopleCount()));
        entity.setNoGuideReport(Boolean.TRUE.equals(request.noGuideReport()));
        entity.setGuideInvolved(!Boolean.TRUE.equals(request.noGuideReport()));
        entity.setCostStage(STAGE_ARRANGEMENT);
        entity.setGuideReportStatus(Boolean.TRUE.equals(request.noGuideReport()) ? FLOW_STATUS_SYNCED : "pending");
        entity.setOperatorAuditStatus(Boolean.TRUE.equals(request.noGuideReport()) ? FLOW_STATUS_SYNCED : "pending");
        entity.setFinanceAuditStatus("pending");
        entity.setStatus(STATUS_ACTIVE);
        entity.setRemark(clean(request.remark()));
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        return entity;
    }

    /** 保存价格明细；多订单拆分后每条记录保留一条拆分金额明细。 */
    private void savePriceLines(
            DispatchTeamArrangementEntity arrangement,
            List<TeamArrangementPriceLineRequest> priceLines,
            Long tenantId,
            String operator,
            BigDecimal overrideAmount
    ) {
        List<TeamArrangementPriceLineRequest> lines = CollectionUtils.isEmpty(priceLines)
                ? List.of(defaultPriceLine(arrangement))
                : priceLines;
        int index = 1;
        for (TeamArrangementPriceLineRequest item : lines) {
            BigDecimal amount = overrideAmount == null ? priceLineAmount(item) : overrideAmount;
            DispatchTeamArrangementPriceLineEntity entity = new DispatchTeamArrangementPriceLineEntity();
            entity.setTenantId(tenantId);
            entity.setArrangementId(arrangement.getId());
            entity.setTeamId(arrangement.getTeamId());
            entity.setProjectId(item.projectId());
            entity.setProjectName(StringUtils.hasText(item.projectName()) ? clean(item.projectName()) : arrangement.getItemName());
            entity.setUnitPrice(overrideAmount == null ? money(item.unitPrice()) : amount);
            entity.setQuantity(overrideAmount == null ? money(item.quantity()) : BigDecimal.ONE.setScale(2));
            entity.setAmount(amount);
            entity.setSalePrice(money(item.salePrice()));
            entity.setCostPrice(money(item.costPrice()));
            entity.setCashAmount(money(item.cashAmount()));
            entity.setCreditAmount(money(item.creditAmount()));
            entity.setGuideCommissionAmount(money(item.guideCommissionAmount()));
            entity.setGuideCommissionRate(money(item.guideCommissionRate()));
            entity.setCompanyRebateAmount(money(item.companyRebateAmount()));
            entity.setCompanyRebateRate(money(item.companyRebateRate()));
            entity.setHeadFeeAmount(money(item.headFeeAmount()));
            entity.setConsumptionAmount(money(item.consumptionAmount()));
            entity.setSortOrder(item.sortOrder() == null ? index : item.sortOrder());
            entity.setRemark(clean(item.remark()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            priceLineMapper.insert(entity);
            index++;
        }
    }

    /** 无需导游报账时自动写入导游报账和计调审核同步流水。 */
    private void syncNoGuideReportFlows(DispatchTeamArrangementEntity entity, Long tenantId, String operator) {
        if (!Boolean.TRUE.equals(entity.getNoGuideReport())) {
            return;
        }
        insertFlow(entity, DispatchArrangementFlowType.GUIDE_REPORT.getValue(), tenantId, operator);
        insertFlow(entity, DispatchArrangementFlowType.OPERATOR_AUDIT.getValue(), tenantId, operator);
    }

    /** 新增一条下游同步流水。 */
    private void insertFlow(DispatchTeamArrangementEntity entity, String flowType, Long tenantId, String operator) {
        DispatchTeamArrangementFlowRecordEntity flow = new DispatchTeamArrangementFlowRecordEntity();
        flow.setTenantId(tenantId);
        flow.setArrangementId(entity.getId());
        flow.setTeamId(entity.getTeamId());
        flow.setTeamNo(entity.getTeamNo());
        flow.setFlowType(flowType);
        flow.setSyncSource(FLOW_SYNC_NO_GUIDE_REPORT);
        flow.setFlowStatus(FLOW_STATUS_SYNCED);
        flow.setFlowAmount(entity.getTotalAmount());
        flow.setRegisteredBy(operator);
        flow.setRegisteredAt(OffsetDateTime.now());
        flow.setCreatedBy(operator);
        flow.setRemark("无需导游报账自动同步");
        flow.setIsDeleted(false);
        flowRecordMapper.insert(flow);
    }

    /** 软删除安排及其子表。 */
    private void softDeleteArrangementTree(Long teamId, Long arrangementId, Long tenantId, String operator) {
        OffsetDateTime now = OffsetDateTime.now();
        DispatchTeamArrangementEntity arrangement = new DispatchTeamArrangementEntity();
        arrangement.setIsDeleted(true);
        arrangement.setDeletedAt(now);
        arrangement.setDeletedBy(operator);
        arrangementMapper.update(arrangement, new UpdateWrapper<DispatchTeamArrangementEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("id", arrangementId)
                .eq("is_deleted", false));

        DispatchTeamArrangementPriceLineEntity priceLine = new DispatchTeamArrangementPriceLineEntity();
        priceLine.setIsDeleted(true);
        priceLine.setDeletedAt(now);
        priceLine.setDeletedBy(operator);
        priceLineMapper.update(priceLine, new UpdateWrapper<DispatchTeamArrangementPriceLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("arrangement_id", arrangementId)
                .eq("is_deleted", false));

        DispatchTeamArrangementOrderAllocationEntity allocation = new DispatchTeamArrangementOrderAllocationEntity();
        allocation.setIsDeleted(true);
        allocation.setDeletedAt(now);
        allocation.setDeletedBy(operator);
        allocationMapper.update(allocation, new UpdateWrapper<DispatchTeamArrangementOrderAllocationEntity>()
                .eq("tenant_id", tenantId)
                .eq("arrangement_id", arrangementId)
                .eq("is_deleted", false));

        DispatchTeamArrangementFlowRecordEntity flow = new DispatchTeamArrangementFlowRecordEntity();
        flow.setIsDeleted(true);
        flow.setDeletedAt(now);
        flow.setDeletedBy(operator);
        flowRecordMapper.update(flow, new UpdateWrapper<DispatchTeamArrangementFlowRecordEntity>()
                .eq("tenant_id", tenantId)
                .eq("arrangement_id", arrangementId)
                .eq("is_deleted", false));
    }

    /** 按团队和订单 ID 加载有效订单，并按用户选择顺序返回。 */
    private List<SalesBookingOrderEntity> loadOrders(Long teamId, List<Long> selectedOrderIds, Long tenantId) {
        List<Long> ids = selectedOrderIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<SalesBookingOrderEntity> orders = orderMapper.selectList(new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .in("id", ids)
                .in("status", List.of("pending", "confirmed")));
        if (orders == null) {
            orders = List.of();
        }
        Map<Long, SalesBookingOrderEntity> orderById = orders.stream()
                .collect(Collectors.toMap(SalesBookingOrderEntity::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<Long> missingIds = ids.stream()
                .filter(id -> !orderById.containsKey(id))
                .toList();
        if (!missingIds.isEmpty()) {
            loadMergedSourceOrders(teamId, missingIds, tenantId).forEach(order -> orderById.put(order.getId(), order));
        }
        return ids.stream().map(orderById::get).filter(item -> item != null).toList();
    }

    /**
     * 加载已拼入当前目标团的来源订单。
     *
     * <p>老系统团队安排成本的订单下拉显示来源订单，而不只是目标团子订单。这里只根据已完成拼团日志放行
     * 来源订单，避免把没有拼团关系的外团订单错误挂到当前执行团成本上。</p>
     */
    private List<SalesBookingOrderEntity> loadMergedSourceOrders(Long targetTeamId, List<Long> sourceOrderIds, Long tenantId) {
        if (transferLogMapper == null || sourceOrderIds.isEmpty()) {
            return List.of();
        }
        List<SalesOrderTransferLogEntity> logs = transferLogMapper.selectList(new QueryWrapper<SalesOrderTransferLogEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("transfer_type", "merge")
                .eq("transfer_status", "completed")
                .eq("target_team_id", targetTeamId)
                .in("source_order_id", sourceOrderIds));
        List<Long> allowedSourceOrderIds = Objects.requireNonNullElse(logs, List.<SalesOrderTransferLogEntity>of())
                .stream()
                .map(SalesOrderTransferLogEntity::getSourceOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (allowedSourceOrderIds.isEmpty()) {
            return List.of();
        }
        return Objects.requireNonNullElse(orderMapper.selectList(new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("id", allowedSourceOrderIds)
                .in("status", List.of("pending", "confirmed"))), List.<SalesBookingOrderEntity>of());
    }

    /** 查询团队安排金额汇总使用的有效订单。 */
    private List<SalesBookingOrderEntity> effectiveOrders(Long teamId, Long tenantId) {
        List<SalesBookingOrderEntity> orders = orderMapper.selectList(new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .orderByAsc("id"));
        return Objects.requireNonNullElse(orders, List.<SalesBookingOrderEntity>of())
                .stream()
                .filter(this::isEffectiveOrder)
                .toList();
    }

    /** 订单收入统计包含未取消的普通订单、拼入订单和历史拼出来源订单。 */
    private boolean isEffectiveOrder(SalesBookingOrderEntity order) {
        String status = order.getStatus();
        boolean activeStatus = Objects.equals(status, SalesBookingOrderStatus.PENDING.value())
                || Objects.equals(status, SalesBookingOrderStatus.CONFIRMED.value());
        String role = StringUtils.hasText(order.getOrderRole())
                ? order.getOrderRole()
                : SalesBookingOrderRole.NORMAL.value();
        boolean activeRole = Objects.equals(role, SalesBookingOrderRole.NORMAL.value())
                || Objects.equals(role, SalesBookingOrderRole.MERGE_CHILD.value())
                || Objects.equals(role, SalesBookingOrderRole.MERGE_SOURCE.value());
        return activeStatus && activeRole;
    }

    /** 查询团队安排金额汇总使用的安排明细。 */
    private List<DispatchTeamArrangementEntity> summaryArrangements(Long teamId, Long tenantId) {
        List<DispatchTeamArrangementEntity> arrangements = arrangementMapper.selectList(baseArrangementQuery(tenantId)
                .eq("team_id", teamId)
                .orderByAsc("arrangement_type")
                .orderByAsc("business_date")
                .orderByAsc("id"));
        return Objects.requireNonNullElse(arrangements, List.of());
    }

    /** 查询团队安排金额汇总使用的导游费用。 */
    private List<DispatchTeamGuideEntity> summaryGuides(Long teamId, Long tenantId) {
        if (guideMapper == null) {
            return List.of();
        }
        List<DispatchTeamGuideEntity> guides = guideMapper.selectList(new QueryWrapper<DispatchTeamGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .eq("status", STATUS_ACTIVE)
                .orderByAsc("id"));
        return Objects.requireNonNullElse(guides, List.of());
    }

    /** 生成成本总览列，现结/挂账由后端统一汇总。 */
    private List<TeamArrangementSummaryResponse.CostColumn> costColumns(
            List<DispatchTeamArrangementEntity> arrangements,
            BigDecimal guideFee,
            BigDecimal guideOperationFee,
            BigDecimal guideImprest
    ) {
        List<TeamArrangementSummaryResponse.CostColumn> base = SUMMARY_TYPES.stream()
                .map(type -> new TeamArrangementSummaryResponse.CostColumn(
                        type,
                        SUMMARY_TYPE_LABELS.getOrDefault(type, type),
                        sumArrangements(arrangements, type, DispatchTeamArrangementEntity::getCashAmount),
                        sumArrangements(arrangements, type, DispatchTeamArrangementEntity::getCreditAmount)
                ))
                .toList();
        BigDecimal cashTotal = base.stream()
                .map(TeamArrangementSummaryResponse.CostColumn::cashAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal creditTotal = base.stream()
                .map(TeamArrangementSummaryResponse.CostColumn::creditAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        List<TeamArrangementSummaryResponse.CostColumn> result = new ArrayList<>(base);
        result.add(new TeamArrangementSummaryResponse.CostColumn("total", "合计", cashTotal, creditTotal));
        result.add(new TeamArrangementSummaryResponse.CostColumn("guide_service", "导服", guideFee, ZERO));
        result.add(new TeamArrangementSummaryResponse.CostColumn("operation_fee", "操作费", guideOperationFee, ZERO));
        result.add(new TeamArrangementSummaryResponse.CostColumn("reserve_fund", "备用金", guideImprest, ZERO));
        return result;
    }

    /** 生成每个安排分类的小计。 */
    private List<TeamArrangementSummaryResponse.SectionSummary> sectionSummaries(
            List<DispatchTeamArrangementEntity> arrangements
    ) {
        return SUMMARY_TYPES.stream()
                .map(type -> {
                    List<DispatchTeamArrangementEntity> records = arrangements.stream()
                            .filter(item -> type.equals(item.getArrangementType()))
                            .toList();
                    BigDecimal costAmount = records.stream()
                            .map(this::arrangementTotal)
                            .reduce(ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new TeamArrangementSummaryResponse.SectionSummary(
                            type,
                            records.size(),
                            costAmount,
                            sumMoney(records, DispatchTeamArrangementEntity::getCashAmount),
                            sumMoney(records, DispatchTeamArrangementEntity::getCreditAmount)
                    );
                })
                .toList();
    }

    /** 读取安排合计金额，优先 total_amount，兼容旧数据 cost_amount。 */
    private BigDecimal arrangementTotal(DispatchTeamArrangementEntity item) {
        BigDecimal total = money(item.getTotalAmount());
        return total.compareTo(ZERO) != 0 ? total : money(item.getCostAmount());
    }

    private BigDecimal sumOrders(
            List<SalesBookingOrderEntity> orders,
            Function<SalesBookingOrderEntity, BigDecimal> mapper
    ) {
        return orders.stream()
                .map(mapper)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumGuides(
            List<DispatchTeamGuideEntity> guides,
            Function<DispatchTeamGuideEntity, BigDecimal> mapper
    ) {
        return guides.stream()
                .map(mapper)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumArrangements(
            List<DispatchTeamArrangementEntity> arrangements,
            String type,
            Function<DispatchTeamArrangementEntity, BigDecimal> mapper
    ) {
        return arrangements.stream()
                .filter(item -> type.equals(item.getArrangementType()))
                .map(mapper)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumMoney(
            List<DispatchTeamArrangementEntity> arrangements,
            Function<DispatchTeamArrangementEntity, BigDecimal> mapper
    ) {
        return arrangements.stream()
                .map(mapper)
                .map(this::money)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** 按规则拆分多订单均摊金额。 */
    private List<BigDecimal> splitAmounts(BigDecimal total, List<SalesBookingOrderEntity> orders, DispatchArrangementSplitMode splitMode) {
        List<BigDecimal> result = new ArrayList<>();
        BigDecimal allocated = ZERO;
        int orderCount = orders.size();
        int peopleTotal = orders.stream().mapToInt(item -> Math.max(number(item.getGuestCount()), 0)).sum();
        if (splitMode == DispatchArrangementSplitMode.BY_PEOPLE && peopleTotal <= 0) {
            throw new BizException("按人数均摊时，选中订单人数必须大于0");
        }
        for (int i = 0; i < orderCount; i++) {
            BigDecimal amount;
            if (i == orderCount - 1) {
                amount = total.subtract(allocated).setScale(2, RoundingMode.HALF_UP);
            } else if (splitMode == DispatchArrangementSplitMode.BY_ORDER) {
                amount = total.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
            } else {
                amount = total.multiply(BigDecimal.valueOf(number(orders.get(i).getGuestCount())))
                        .divide(BigDecimal.valueOf(peopleTotal), 2, RoundingMode.HALF_UP);
            }
            result.add(amount);
            allocated = allocated.add(amount);
        }
        return result;
    }

    /** 按拆分金额比例拆分现结或预付款。 */
    private BigDecimal proportionalAmount(
            BigDecimal source,
            BigDecimal total,
            BigDecimal itemTotal,
            int index,
            int count,
            List<BigDecimal> splitAmounts
    ) {
        if (source.compareTo(ZERO) <= 0 || total.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        if (index == count - 1) {
            BigDecimal allocated = ZERO;
            for (int i = 0; i < index; i++) {
                allocated = allocated.add(source.multiply(splitAmounts.get(i)).divide(total, 2, RoundingMode.HALF_UP));
            }
            return source.subtract(allocated).max(ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        return source.multiply(itemTotal).divide(total, 2, RoundingMode.HALF_UP);
    }

    /** 校验无需导游报账和现结金额不能同时存在。 */
    private void validateNoGuideReport(TeamArrangementSaveRequest request) {
        if (Boolean.TRUE.equals(request.noGuideReport()) && money(request.cashAmount()).compareTo(ZERO) > 0) {
            throw new BizException("已选择“无需导游报账”，现结金额须为0！");
        }
    }

    /** 读取会锁定删除的人工或真实下游流程数量；无需导游报账自动同步流水允许随录错安排一起作废。 */
    private long activeFlowCount(Long arrangementId, Long tenantId) {
        Long count = flowRecordMapper.selectCount(new QueryWrapper<DispatchTeamArrangementFlowRecordEntity>()
                .eq("tenant_id", tenantId)
                .eq("arrangement_id", arrangementId)
                .and(flowWrapper -> flowWrapper
                        .ne("sync_source", FLOW_SYNC_NO_GUIDE_REPORT)
                        .or()
                        .isNull("sync_source"))
                .eq("is_deleted", false));
        return count == null ? 0 : count;
    }

    /** 查询并校验团队。 */
    private SalesTeamEntity requireTeam(Long teamId, Long tenantId) {
        SalesTeamEntity team = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", teamId)
                .eq("is_deleted", false));
        if (team == null) {
            throw new BizException("团队不存在或已删除");
        }
        return team;
    }

    private QueryWrapper<DispatchTeamArrangementEntity> baseArrangementQuery(Long tenantId) {
        return new QueryWrapper<DispatchTeamArrangementEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<DispatchTeamArrangementSectionStatusEntity> baseSectionStatusQuery(Long tenantId) {
        return new QueryWrapper<DispatchTeamArrangementSectionStatusEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private TeamArrangementPriceLineRequest defaultPriceLine(DispatchTeamArrangementEntity arrangement) {
        return new TeamArrangementPriceLineRequest(
                null,
                arrangement.getItemName(),
                arrangement.getTotalAmount(),
                BigDecimal.ONE,
                arrangement.getTotalAmount(),
                ZERO,
                ZERO,
                arrangement.getCashAmount(),
                arrangement.getCreditAmount(),
                ZERO,
                ZERO,
                ZERO,
                ZERO,
                ZERO,
                ZERO,
                1,
                null
        );
    }

    private BigDecimal totalAmount(TeamArrangementSaveRequest request) {
        BigDecimal explicitTotal = money(request.totalAmount());
        if (explicitTotal.compareTo(ZERO) > 0) {
            return explicitTotal;
        }
        if (CollectionUtils.isEmpty(request.priceLines())) {
            return ZERO;
        }
        return request.priceLines().stream()
                .map(this::priceLineAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal priceLineAmount(TeamArrangementPriceLineRequest item) {
        BigDecimal amount = money(item.amount());
        if (amount.compareTo(ZERO) > 0) {
            return amount;
        }
        return money(item.unitPrice()).multiply(money(item.quantity())).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal creditAmount(TeamArrangementSaveRequest request, BigDecimal total, BigDecimal cash, BigDecimal prepaid) {
        BigDecimal explicit = money(request.creditAmount());
        if (explicit.compareTo(ZERO) > 0) {
            return explicit;
        }
        return total.subtract(cash).subtract(prepaid).max(ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String cleanRequired(String value) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            throw new BizException("安排名称不能为空");
        }
        return cleaned;
    }
}
