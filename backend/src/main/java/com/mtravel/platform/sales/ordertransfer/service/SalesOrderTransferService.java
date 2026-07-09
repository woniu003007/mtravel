package com.mtravel.platform.sales.ordertransfer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderChargeLineEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingGuestType;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderRole;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderChargeLineMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderGuestMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.ordertransfer.dto.SalesOrderTransferMergeItemRequest;
import com.mtravel.platform.sales.ordertransfer.dto.SalesOrderTransferMergeRequest;
import com.mtravel.platform.sales.ordertransfer.dto.SalesOrderTransferMergeResult;
import com.mtravel.platform.sales.ordertransfer.dto.SalesOrderTransferMoveRequest;
import com.mtravel.platform.sales.ordertransfer.dto.SalesOrderTransferRemarkRequest;
import com.mtravel.platform.sales.ordertransfer.entity.SalesOrderTransferLogEntity;
import com.mtravel.platform.sales.ordertransfer.mapper.SalesOrderTransferLogMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamPriceEntity;
import com.mtravel.platform.sales.team.enums.SalesTeamStatus;
import com.mtravel.platform.sales.team.enums.SalesTeamType;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamPriceMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 销售订单拼团和转团业务服务。
 *
 * <p>服务层负责团队操作页高风险动作的租户校验、团队状态校验、订单归属变更、子订单生成、
 * 团队人数刷新和流转留痕。Controller 不直接修改订单或团队。</p>
 */
@Service
public class SalesOrderTransferService {

    private final SalesBookingOrderMapper orderMapper;
    private final SalesBookingOrderChargeLineMapper chargeLineMapper;
    private final SalesBookingOrderGuestMapper guestMapper;
    private final SalesTeamMapper teamMapper;
    private final SalesTeamPriceMapper priceMapper;
    private final SalesOrderTransferLogMapper logMapper;

    /**
     * 构造订单团队流转服务。
     *
     * @param orderMapper 收客订单 Mapper
     * @param chargeLineMapper 订单收入明细 Mapper
     * @param guestMapper 游客名单 Mapper
     * @param teamMapper 团队 Mapper
     * @param priceMapper 团队价格 Mapper
     * @param logMapper 流转日志 Mapper
     */
    public SalesOrderTransferService(
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderChargeLineMapper chargeLineMapper,
            SalesBookingOrderGuestMapper guestMapper,
            SalesTeamMapper teamMapper,
            SalesTeamPriceMapper priceMapper,
            SalesOrderTransferLogMapper logMapper
    ) {
        this.orderMapper = orderMapper;
        this.chargeLineMapper = chargeLineMapper;
        this.guestMapper = guestMapper;
        this.teamMapper = teamMapper;
        this.priceMapper = priceMapper;
        this.logMapper = logMapper;
    }

    /**
     * 执行团队操作页拼团。
     *
     * <p>按老系统实测口径，多个来源订单可以一次拼到多个目标团队，实际生成
     * “来源订单 x 目标团队”的拼团子订单矩阵。来源订单保留在来源团，不再改成统计过滤角色；
     * 目标团子订单金额只取确认页填写的拼团单价和价格类型，不复制来源订单原价。</p>
     *
     * @param currentTeamId 当前团队 ID；订单管理全局入口传 null
     * @param request 拼团请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 拼团执行结果
     */
    @Transactional
    public SalesOrderTransferMergeResult mergeOrders(
            Long currentTeamId,
            SalesOrderTransferMergeRequest request,
            Long tenantId,
            String operator
    ) {
        List<Long> orderIds = distinctIds(request.orderIds());
        if (CollectionUtils.isEmpty(orderIds)) {
            throw new BizException("请选择订单");
        }
        List<Long> targetTeamIds = distinctIds(request.targetTeamIds());
        if (CollectionUtils.isEmpty(targetTeamIds)) {
            throw new BizException("请选择目标团队");
        }
        if (currentTeamId != null && targetTeamIds.contains(currentTeamId)) {
            throw new BizException("不能拼到当前团队");
        }
        SalesTeamEntity currentTeam = currentTeamId == null ? null : requireTeam(currentTeamId, tenantId);
        Map<Long, SalesTeamEntity> targetTeams = new LinkedHashMap<>();
        for (Long targetTeamId : targetTeamIds) {
            SalesTeamEntity targetTeam = requireTargetTeam(targetTeamId, tenantId);
            assertTeamCanReceive(targetTeam);
            assertMergeTargetTeamType(targetTeam);
            targetTeams.put(targetTeamId, targetTeam);
        }
        Map<Long, SalesBookingOrderEntity> sourceOrders = new LinkedHashMap<>();
        Map<Long, SalesTeamEntity> sourceTeams = new LinkedHashMap<>();
        for (Long orderId : orderIds) {
            SalesBookingOrderEntity sourceOrder = requireOrder(orderId, tenantId);
            assertOrderCanMerge(sourceOrder, currentTeamId);
            SalesTeamEntity sourceTeam = currentTeam == null ? requireTeam(sourceOrder.getTeamId(), tenantId) : currentTeam;
            sourceOrders.put(orderId, sourceOrder);
            sourceTeams.put(orderId, sourceTeam);
        }
        Set<String> existingPairs = existingMergePairs(tenantId, orderIds);
        Map<Long, List<SalesBookingOrderGuestEntity>> guestsByOrder = new LinkedHashMap<>();
        List<SalesOrderTransferMergeResult.SkippedItem> skippedItems = new ArrayList<>();
        Set<Long> changedTeamIds = new LinkedHashSet<>();
        int createdCount = 0;
        for (Long targetTeamId : targetTeamIds) {
            SalesTeamEntity targetTeam = targetTeams.get(targetTeamId);
            for (Long orderId : orderIds) {
                SalesBookingOrderEntity sourceOrder = sourceOrders.get(orderId);
                SalesTeamEntity sourceTeam = sourceTeams.get(orderId);
                if (sourceOrder == null || sourceTeam == null || targetTeam == null) {
                    continue;
                }
                if (Objects.equals(sourceOrder.getTeamId(), targetTeamId)) {
                    skippedItems.add(new SalesOrderTransferMergeResult.SkippedItem(orderId, targetTeamId, "不能拼到来源团队"));
                    continue;
                }
                String pairKey = mergePairKey(orderId, targetTeamId);
                if (existingPairs.contains(pairKey)) {
                    skippedItems.add(new SalesOrderTransferMergeResult.SkippedItem(orderId, targetTeamId, "已存在拼团关系"));
                    continue;
                }
                List<SalesBookingOrderGuestEntity> sourceGuests = guestsByOrder.computeIfAbsent(
                        orderId,
                        key -> loadSourceGuests(sourceOrder.getId(), tenantId)
                );
                if (sourceGuests.isEmpty()) {
                    throw new BizException("拼团订单必须先维护游客名单");
                }
                SalesOrderTransferMergeItemRequest item = itemFor(request, orderId, targetTeamId);
                int guestCount = mergeGuestCount(sourceOrder, sourceGuests);
                BigDecimal unitPrice = money(item == null ? null : item.unitPrice());
                BigDecimal quantity = new BigDecimal(guestCount).setScale(2, RoundingMode.HALF_UP);
                BigDecimal receivableAmount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
                SalesBookingOrderEntity childOrder = copyOrderForTarget(
                        sourceOrder,
                        sourceTeam,
                        targetTeam,
                        sourceGuests,
                        receivableAmount,
                        tenantId,
                        operator
                );
                orderMapper.insert(childOrder);
                insertMergePriceLine(
                        childOrder.getId(),
                        targetTeam.getId(),
                        item == null ? null : item.priceType(),
                        unitPrice,
                        quantity,
                        receivableAmount,
                        tenantId,
                        operator
                );
                copyGuests(sourceGuests, childOrder.getId(), targetTeam.getId(), tenantId, operator);
                insertLog(
                        tenantId,
                        "merge",
                        sourceOrder.getId(),
                        sourceTeam.getId(),
                        targetTeam.getId(),
                        childOrder.getId(),
                        request.tagFlag(),
                        remarkFor(request, sourceOrder.getId(), targetTeam.getId()),
                        operator
                );
                existingPairs.add(pairKey);
                changedTeamIds.add(sourceTeam.getId());
                changedTeamIds.add(targetTeam.getId());
                createdCount++;
            }
        }
        for (Long changedTeamId : changedTeamIds) {
            refreshTeamSeats(changedTeamId, tenantId);
        }
        return new SalesOrderTransferMergeResult(createdCount, skippedItems.size(), skippedItems);
    }

    /**
     * 执行团队操作页转团。
     *
     * <p>转团会迁移订单当前归属，并同步更新订单收入明细和游客名单的团队 ID。若选择新团队模式，
     * 先复制当前团队基础资料生成新团队，再把订单迁入。</p>
     *
     * @param currentTeamId 当前团队 ID
     * @param request 转团请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     */
    @Transactional
    public void moveOrders(Long currentTeamId, SalesOrderTransferMoveRequest request, Long tenantId, String operator) {
        List<Long> orderIds = distinctIds(request.orderIds());
        if (CollectionUtils.isEmpty(orderIds)) {
            throw new BizException("请选择订单");
        }
        if (!request.createNewTeam() && Objects.equals(currentTeamId, request.targetTeamId())) {
            throw new BizException("不能转到当前团队");
        }
        SalesTeamEntity sourceTeam = requireTeam(currentTeamId, tenantId);
        SalesTeamEntity targetTeam = request.createNewTeam()
                ? createTeamByCopy(sourceTeam, request, tenantId, operator)
                : requireTargetTeam(request.targetTeamId(), tenantId);
        if (Objects.equals(currentTeamId, targetTeam.getId())) {
            throw new BizException("不能转到当前团队");
        }
        assertTeamCanReceive(targetTeam);
        for (Long orderId : orderIds) {
            SalesBookingOrderEntity order = requireOrder(orderId, tenantId);
            assertOrderBelongsToTeam(order, currentTeamId);
            moveOrderToTeam(order, sourceTeam, targetTeam, tenantId, operator);
            insertLog(
                    tenantId,
                    "move",
                    order.getId(),
                    sourceTeam.getId(),
                    targetTeam.getId(),
                    order.getId(),
                    false,
                    clean(request.remark()),
                    operator
            );
        }
        refreshTeamSeats(sourceTeam.getId(), tenantId);
        refreshTeamSeats(targetTeam.getId(), tenantId);
    }

    private SalesBookingOrderEntity copyOrderForTarget(
            SalesBookingOrderEntity source,
            SalesTeamEntity sourceTeam,
            SalesTeamEntity targetTeam,
            List<SalesBookingOrderGuestEntity> sourceGuests,
            BigDecimal receivableAmount,
            Long tenantId,
            String operator
    ) {
        SalesBookingOrderEntity child = new SalesBookingOrderEntity();
        child.setTenantId(tenantId);
        child.setTeamId(targetTeam.getId());
        child.setOrderNo(source.getOrderNo() + "-PT-" + targetTeam.getTeamNo());
        child.setCustomerId(source.getCustomerId());
        child.setCustomerName(source.getCustomerName());
        child.setContactName(source.getContactName());
        child.setContactPhone(source.getContactPhone());
        child.setCustomerTeamNo(source.getCustomerTeamNo());
        child.setOriginalOrderInfo(sourceSummary(source, sourceTeam));
        child.setOrderRole(SalesBookingOrderRole.MERGE_CHILD.value());
        child.setSalespersonEmployeeId(source.getSalespersonEmployeeId());
        child.setSalespersonEmployeeName(source.getSalespersonEmployeeName());
        child.setBookingOperatorEmployeeId(source.getBookingOperatorEmployeeId());
        child.setBookingOperatorEmployeeName(source.getBookingOperatorEmployeeName());
        child.setSourceProvince(source.getSourceProvince());
        child.setSourceCity(source.getSourceCity());
        child.setSourceDistrict(source.getSourceDistrict());
        child.setTravelDescription(source.getTravelDescription());
        child.setPickupInfo(source.getPickupInfo());
        child.setDropoffInfo(source.getDropoffInfo());
        child.setPickupRemark(source.getPickupRemark());
        child.setGuideName(source.getGuideName());
        child.setGuidePhone(source.getGuidePhone());
        child.setGuideRemark(source.getGuideRemark());
        child.setHotelInfo(source.getHotelInfo());
        int guestCount = mergeGuestCount(source, sourceGuests);
        // 拼团计价、目标团实收和列表人数按来源订单占位人数走；游客名单只作为明细追溯复制。
        child.setGuestCount(guestCount);
        child.setAdultCount(Math.min(guestCount, number(source.getAdultCount())));
        child.setChildCount(numberWithinTotal(source.getChildCount(), guestCount, child.getAdultCount()));
        child.setChildNoBedCount(numberWithinTotal(source.getChildNoBedCount(), guestCount, child.getAdultCount() + child.getChildCount()));
        child.setSeniorCount(numberWithinTotal(source.getSeniorCount(), guestCount, child.getAdultCount() + child.getChildCount() + child.getChildNoBedCount()));
        child.setEscortCount(numberWithinTotal(source.getEscortCount(), guestCount, child.getAdultCount() + child.getChildCount() + child.getChildNoBedCount() + child.getSeniorCount()));
        child.setReceivableAmount(money(receivableAmount));
        // 拼团子订单承接目标团收入，但不能复制来源订单已收金额；收款后续由收款核销链路回写。
        child.setReceivedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        child.setBalanceAmount(money(receivableAmount));
        child.setFeeRemark(source.getFeeRemark());
        child.setConfirmRemark(source.getConfirmRemark());
        child.setOrderRemark(source.getOrderRemark());
        child.setStatus(source.getStatus());
        child.setBookedBy(source.getBookedBy());
        child.setBookedAt(source.getBookedAt());
        child.setRemark(source.getRemark());
        child.setCreatedBy(operator);
        child.setIsDeleted(false);
        return child;
    }

    private void moveOrderToTeam(
            SalesBookingOrderEntity order,
            SalesTeamEntity sourceTeam,
            SalesTeamEntity targetTeam,
            Long tenantId,
            String operator
    ) {
        SalesBookingOrderEntity update = new SalesBookingOrderEntity();
        update.setTeamId(targetTeam.getId());
        update.setOriginalOrderInfo(moveSummary(order, sourceTeam, targetTeam));
        update.setCreatedBy(operator);
        int updated = orderMapper.update(update, baseOrderUpdate(tenantId).eq("id", order.getId()));
        if (updated == 0) {
            throw new BizException("订单状态已变化，请刷新后重试");
        }
        SalesBookingOrderChargeLineEntity chargeUpdate = new SalesBookingOrderChargeLineEntity();
        chargeUpdate.setTeamId(targetTeam.getId());
        chargeLineMapper.update(chargeUpdate, baseChargeUpdate(tenantId).eq("order_id", order.getId()));

        SalesBookingOrderGuestEntity guestUpdate = new SalesBookingOrderGuestEntity();
        guestUpdate.setTeamId(targetTeam.getId());
        guestMapper.update(guestUpdate, baseGuestUpdate(tenantId).eq("order_id", order.getId()));
    }

    private void copyGuests(
            List<SalesBookingOrderGuestEntity> guests,
            Long childOrderId,
            Long targetTeamId,
            Long tenantId,
            String operator
    ) {
        for (SalesBookingOrderGuestEntity source : guests) {
            guestMapper.insert(copyGuest(source, childOrderId, targetTeamId, tenantId, operator));
        }
    }

    private SalesBookingOrderGuestEntity copyGuest(
            SalesBookingOrderGuestEntity source,
            Long childOrderId,
            Long targetTeamId,
            Long tenantId,
            String operator
    ) {
        SalesBookingOrderGuestEntity copy = new SalesBookingOrderGuestEntity();
        copy.setTenantId(tenantId);
        copy.setOrderId(childOrderId);
        copy.setTeamId(targetTeamId);
        copy.setIndexNo(source.getIndexNo());
        copy.setGuestName(source.getGuestName());
        copy.setEnglishName(source.getEnglishName());
        copy.setCertificateNo(source.getCertificateNo());
        copy.setPassportNo(source.getPassportNo());
        copy.setGender(source.getGender());
        copy.setBirthDate(source.getBirthDate());
        copy.setAge(source.getAge());
        copy.setPhone(source.getPhone());
        copy.setGuestType(source.getGuestType());
        copy.setRoomGroup(source.getRoomGroup());
        copy.setLeaderFlag(source.getLeaderFlag());
        copy.setIdCardValid(source.getIdCardValid());
        copy.setIdCardWarning(source.getIdCardWarning());
        copy.setRoomRemark(source.getRoomRemark());
        copy.setCreatedBy(operator);
        copy.setRemark(source.getRemark());
        copy.setIsDeleted(false);
        return copy;
    }

    private void insertMergePriceLine(
            Long childOrderId,
            Long targetTeamId,
            String priceType,
            BigDecimal unitPrice,
            BigDecimal quantity,
            BigDecimal amount,
            Long tenantId,
            String operator
    ) {
        SalesBookingOrderChargeLineEntity line = new SalesBookingOrderChargeLineEntity();
        line.setTenantId(tenantId);
        line.setOrderId(childOrderId);
        line.setTeamId(targetTeamId);
        line.setLineKind("base_price");
        line.setLineType(priceLineType(priceType));
        line.setItemName(priceLineName(priceType));
        line.setUnitPrice(money(unitPrice));
        line.setQuantity(money(quantity));
        line.setAmount(money(amount));
        line.setStatus("effective");
        line.setRegisteredBy(operator);
        line.setRegisteredAt(OffsetDateTime.now());
        line.setSortOrder(1);
        line.setCreatedBy(operator);
        line.setIsDeleted(false);
        chargeLineMapper.insert(line);
    }

    private List<SalesBookingOrderGuestEntity> loadSourceGuests(Long sourceOrderId, Long tenantId) {
        return Objects.requireNonNullElse(guestMapper.selectList(baseGuestQuery(tenantId)
                .eq("order_id", sourceOrderId)
                .orderByAsc("index_no")
                .orderByAsc("id")), List.of());
    }

    private SalesTeamEntity createTeamByCopy(
            SalesTeamEntity sourceTeam,
            SalesOrderTransferMoveRequest request,
            Long tenantId,
            String operator
    ) {
        if (request.tourDate() == null || request.allNum() == null || !StringUtils.hasText(request.lineName())) {
            throw new BizException("新团队发团日期、总位数和团队名称不能为空");
        }
        SalesTeamEntity target = new SalesTeamEntity();
        target.setTenantId(tenantId);
        target.setProductId(sourceTeam.getProductId());
        target.setTeamNo(clean(request.lineName()));
        target.setTeamType(SalesTeamType.fromValueOrDefault(request.lineType()).getValue());
        target.setBusinessType(sourceTeam.getBusinessType());
        target.setDepartureDate(request.tourDate());
        target.setDepartmentId(sourceTeam.getDepartmentId());
        target.setDepartmentName(sourceTeam.getDepartmentName());
        target.setOperatorEmployeeId(sourceTeam.getOperatorEmployeeId());
        target.setOperatorEmployeeName(sourceTeam.getOperatorEmployeeName());
        target.setEscortEmployeeId(sourceTeam.getEscortEmployeeId());
        target.setEscortEmployeeName(sourceTeam.getEscortEmployeeName());
        target.setStatus(SalesTeamStatus.NORMAL.getValue());
        target.setTotalSeats(Math.max(0, request.allNum()));
        target.setUsedSeats(0);
        target.setRemainingSeats(Math.max(0, request.allNum()));
        target.setSingleRoomDifference(sourceTeam.getSingleRoomDifference());
        target.setCloseDaysBefore(sourceTeam.getCloseDaysBefore());
        target.setCreatedBy(operator);
        target.setRemark(clean(request.memo()));
        target.setIsDeleted(false);
        teamMapper.insert(target);
        copyTeamPrices(sourceTeam.getId(), target.getId(), target.getProductId(), tenantId, operator);
        return target;
    }

    private void copyTeamPrices(Long sourceTeamId, Long targetTeamId, Long productId, Long tenantId, String operator) {
        List<SalesTeamPriceEntity> prices = priceMapper.selectList(basePriceQuery(tenantId)
                .eq("team_id", sourceTeamId)
                .orderByAsc("id"));
        for (SalesTeamPriceEntity source : prices) {
            SalesTeamPriceEntity copy = new SalesTeamPriceEntity();
            copy.setTenantId(tenantId);
            copy.setTeamId(targetTeamId);
            copy.setProductId(productId);
            copy.setCustomerCategoryId(source.getCustomerCategoryId());
            copy.setCustomerCategoryName(source.getCustomerCategoryName());
            copy.setAdultPrice(source.getAdultPrice());
            copy.setChildPrice(source.getChildPrice());
            copy.setChildNoBedPrice(source.getChildNoBedPrice());
            copy.setSeniorPrice(source.getSeniorPrice());
            copy.setExtraFee(source.getExtraFee());
            copy.setStatus(source.getStatus());
            copy.setCreatedBy(operator);
            copy.setRemark(source.getRemark());
            copy.setIsDeleted(false);
            priceMapper.insert(copy);
        }
    }

    private void insertLog(
            Long tenantId,
            String transferType,
            Long sourceOrderId,
            Long sourceTeamId,
            Long targetTeamId,
            Long childOrderId,
            boolean tagFlag,
            String remark,
            String operator
    ) {
        SalesOrderTransferLogEntity log = new SalesOrderTransferLogEntity();
        log.setTenantId(tenantId);
        log.setSourceOrderId(sourceOrderId);
        log.setSourceTeamId(sourceTeamId);
        log.setTargetTeamId(targetTeamId);
        log.setChildOrderId(childOrderId);
        log.setTransferType(transferType);
        log.setTransferStatus("completed");
        log.setTagFlag(tagFlag);
        log.setOperator(operator);
        log.setOperatedAt(OffsetDateTime.now());
        log.setCreatedBy(operator);
        log.setRemark(remark);
        log.setIsDeleted(false);
        logMapper.insert(log);
    }

    private void refreshTeamSeats(Long teamId, Long tenantId) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        int totalSeats = number(team.getTotalSeats());
        int usedSeats = number(orderMapper.sumGuestCountByTeam(tenantId, teamId));
        SalesTeamEntity update = new SalesTeamEntity();
        update.setUsedSeats(usedSeats);
        update.setRemainingSeats(Math.max(0, totalSeats - usedSeats));
        teamMapper.update(update, baseTeamUpdate(tenantId).eq("id", teamId));
    }

    private SalesTeamEntity requireTargetTeam(Long targetTeamId, Long tenantId) {
        if (targetTeamId == null) {
            throw new BizException("请选择目标团队");
        }
        return requireTeam(targetTeamId, tenantId);
    }

    private SalesTeamEntity requireTeam(Long teamId, Long tenantId) {
        SalesTeamEntity team = teamMapper.selectOne(baseTeamQuery(tenantId).eq("id", teamId));
        if (team == null) {
            throw new BizException("团队不存在或已删除");
        }
        return team;
    }

    private SalesBookingOrderEntity requireOrder(Long orderId, Long tenantId) {
        SalesBookingOrderEntity order = orderMapper.selectOne(baseOrderQuery(tenantId).eq("id", orderId));
        if (order == null) {
            throw new BizException("订单不存在或已删除");
        }
        return order;
    }

    private void assertOrderBelongsToTeam(SalesBookingOrderEntity order, Long currentTeamId) {
        if (!Objects.equals(order.getTeamId(), currentTeamId)) {
            throw new BizException("订单不属于当前团队");
        }
    }

    private void assertOrderCanMerge(SalesBookingOrderEntity order, Long currentTeamId) {
        if (currentTeamId != null) {
            assertOrderBelongsToTeam(order, currentTeamId);
        }
        if ("cancelled".equals(order.getStatus())) {
            throw new BizException("已取消订单不能拼团");
        }
        String role = StringUtils.hasText(order.getOrderRole())
                ? order.getOrderRole()
                : SalesBookingOrderRole.NORMAL.value();
        if (SalesBookingOrderRole.MERGE_CHILD.value().equals(role)) {
            throw new BizException("拼团子订单不能再次拼团");
        }
    }

    private void assertTeamCanReceive(SalesTeamEntity team) {
        if (SalesTeamStatus.CANCELLED.getValue().equals(team.getStatus())) {
            throw new BizException("取消团队不能接收订单");
        }
        if (SalesTeamStatus.STOPPED.getValue().equals(team.getStatus())) {
            throw new BizException("停售团队不能接收订单");
        }
    }

    /**
     * 校验拼团目标团队类型。
     *
     * <p>老系统拼团目标列表只提供散拼团队。这里在服务层同步兜底，避免绕过前端接口把订单拼到整团、
     * 散团或单项团队，导致后续团队收入、成本和毛利归属混乱。</p>
     *
     * @param team 待接收拼团订单的目标团队
     */
    private void assertMergeTargetTeamType(SalesTeamEntity team) {
        if (!SalesTeamType.SANPIN.getValue().equals(team.getTeamType())) {
            throw new BizException("拼团目标团队必须是散拼团队");
        }
    }

    private String remarkFor(SalesOrderTransferMergeRequest request, Long orderId, Long targetTeamId) {
        SalesOrderTransferMergeItemRequest mergeItem = itemFor(request, orderId, targetTeamId);
        if (mergeItem != null && StringUtils.hasText(mergeItem.remark())) {
            return clean(mergeItem.remark());
        }
        List<SalesOrderTransferRemarkRequest> remarks = Objects.requireNonNullElse(request.remarks(), List.of());
        return remarks.stream()
                .filter(item -> Objects.equals(item.orderId(), orderId) && Objects.equals(item.targetTeamId(), targetTeamId))
                .map(SalesOrderTransferRemarkRequest::remark)
                .filter(StringUtils::hasText)
                .findFirst()
                .map(this::clean)
                .orElse(clean(request.remark()));
    }

    private SalesOrderTransferMergeItemRequest itemFor(
            SalesOrderTransferMergeRequest request,
            Long orderId,
            Long targetTeamId
    ) {
        return Objects.requireNonNullElse(request.items(), List.<SalesOrderTransferMergeItemRequest>of()).stream()
                .filter(item -> Objects.equals(item.orderId(), orderId) && Objects.equals(item.targetTeamId(), targetTeamId))
                .findFirst()
                .orElse(null);
    }

    private Set<String> existingMergePairs(Long tenantId, List<Long> orderIds) {
        List<SalesOrderTransferLogEntity> logs = Objects.requireNonNullElse(
                logMapper.selectCompletedMergeBySourceOrders(tenantId, orderIds),
                List.of()
        );
        Set<String> keys = new HashSet<>();
        for (SalesOrderTransferLogEntity log : logs) {
            if (log != null && log.getSourceOrderId() != null && log.getTargetTeamId() != null) {
                keys.add(mergePairKey(log.getSourceOrderId(), log.getTargetTeamId()));
            }
        }
        return keys;
    }

    private String mergePairKey(Long orderId, Long targetTeamId) {
        return orderId + ":" + targetTeamId;
    }

    private int mergeGuestCount(SalesBookingOrderEntity sourceOrder, List<SalesBookingOrderGuestEntity> guests) {
        int occupiedSeats = number(sourceOrder.getGuestCount());
        if (occupiedSeats > 0) {
            return occupiedSeats;
        }
        return CollectionUtils.isEmpty(guests) ? 0 : guests.size();
    }

    private int numberWithinTotal(Integer value, int total, int used) {
        return Math.min(Math.max(0, total - used), number(value));
    }

    private int countGuests(List<SalesBookingOrderGuestEntity> guests, SalesBookingGuestType type) {
        if (CollectionUtils.isEmpty(guests)) {
            return 0;
        }
        return (int) guests.stream()
                .filter(guest -> type.value().equals(guest.getGuestType()))
                .count();
    }

    private String priceLineType(String priceType) {
        String text = StringUtils.hasText(priceType) ? priceType.trim() : "成人";
        return switch (text) {
            case "adult", "成人" -> "adult";
            case "child", "儿童", "儿童占床" -> "child";
            case "child_no_bed", "儿童不占床" -> "child_no_bed";
            case "senior", "老人" -> "senior";
            case "escort", "全陪" -> "escort";
            default -> text;
        };
    }

    private String priceLineName(String priceType) {
        String text = StringUtils.hasText(priceType) ? priceType.trim() : "成人";
        return switch (text) {
            case "adult" -> "成人";
            case "child" -> "儿童";
            case "child_no_bed" -> "儿童不占床";
            case "senior" -> "老人";
            case "escort" -> "全陪";
            default -> text;
        };
    }

    private String sourceSummary(SalesBookingOrderEntity order, SalesTeamEntity sourceTeam) {
        String dateText = sourceTeam.getDepartureDate() == null
                ? clean(sourceTeam.getTeamNo())
                : "[" + chineseWeekday(sourceTeam.getDepartureDate().getDayOfWeek()) + "]" + sourceTeam.getDepartureDate();
        String orderText = StringUtils.hasText(order.getTravelDescription())
                ? clean(order.getTravelDescription())
                : clean(order.getOrderNo());
        return joinNonBlank(dateText, orderText, order.getCustomerName());
    }

    private String chineseWeekday(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "一";
            case TUESDAY -> "二";
            case WEDNESDAY -> "三";
            case THURSDAY -> "四";
            case FRIDAY -> "五";
            case SATURDAY -> "六";
            case SUNDAY -> "日";
        };
    }

    private String moveSummary(SalesBookingOrderEntity order, SalesTeamEntity sourceTeam, SalesTeamEntity targetTeam) {
        String existing = clean(order.getOriginalOrderInfo());
        String moveText = "转团：" + sourceTeam.getTeamNo() + " -> " + targetTeam.getTeamNo();
        return StringUtils.hasText(existing) ? existing + "\n" + moveText : moveText;
    }

    private QueryWrapper<SalesTeamEntity> baseTeamQuery(Long tenantId) {
        return new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesTeamEntity> baseTeamUpdate(Long tenantId) {
        return new UpdateWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesBookingOrderEntity> baseOrderQuery(Long tenantId) {
        return new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesBookingOrderEntity> baseOrderUpdate(Long tenantId) {
        return new UpdateWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesBookingOrderChargeLineEntity> baseChargeQuery(Long tenantId) {
        return new QueryWrapper<SalesBookingOrderChargeLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesBookingOrderChargeLineEntity> baseChargeUpdate(Long tenantId) {
        return new UpdateWrapper<SalesBookingOrderChargeLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesBookingOrderGuestEntity> baseGuestQuery(Long tenantId) {
        return new QueryWrapper<SalesBookingOrderGuestEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesBookingOrderGuestEntity> baseGuestUpdate(Long tenantId) {
        return new UpdateWrapper<SalesBookingOrderGuestEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesTeamPriceEntity> basePriceQuery(Long tenantId) {
        return new QueryWrapper<SalesTeamPriceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        Set<Long> seen = new HashSet<>();
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(seen::add)
                .toList();
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String joinNonBlank(String... values) {
        return java.util.Arrays.stream(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
