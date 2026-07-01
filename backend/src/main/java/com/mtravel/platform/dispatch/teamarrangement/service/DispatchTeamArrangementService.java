package com.mtravel.platform.dispatch.teamarrangement.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementOrderAllocationResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementPriceLineRequest;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementPriceLineResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementResponse;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveResponse;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementFlowRecordEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementOrderAllocationEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementPriceLineEntity;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementAllocationMode;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementFlowType;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementSettlementType;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementSplitMode;
import com.mtravel.platform.dispatch.teamarrangement.enums.DispatchArrangementType;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementFlowRecordMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementOrderAllocationMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementPriceLineMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private static final String STAGE_ARRANGEMENT = "arrangement";
    private static final String FLOW_STATUS_SYNCED = "synced";
    private static final String FLOW_SYNC_NO_GUIDE_REPORT = "no_guide_report";

    private final DispatchTeamArrangementMapper arrangementMapper;
    private final DispatchTeamArrangementPriceLineMapper priceLineMapper;
    private final DispatchTeamArrangementOrderAllocationMapper allocationMapper;
    private final DispatchTeamArrangementFlowRecordMapper flowRecordMapper;
    private final SalesTeamMapper teamMapper;
    private final SalesBookingOrderMapper orderMapper;

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
        this.arrangementMapper = arrangementMapper;
        this.priceLineMapper = priceLineMapper;
        this.allocationMapper = allocationMapper;
        this.flowRecordMapper = flowRecordMapper;
        this.teamMapper = teamMapper;
        this.orderMapper = orderMapper;
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
            return saveMultiOrderAverage(team, type, request, tenantId, operator);
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
        return ids.stream().map(orderById::get).filter(item -> item != null).toList();
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
