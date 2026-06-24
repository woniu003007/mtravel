package com.mtravel.platform.sales.booking.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.sales.booking.aiimport.service.IdCardValidationResult;
import com.mtravel.platform.sales.booking.aiimport.service.IdCardValidator;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingFeeChangeResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderGuestRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderGuestResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderPriceLineRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderPriceLineResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderFeeChangeEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderPriceLineEntity;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingGuestType;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderStatus;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderFeeChangeMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderGuestMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderPriceLineMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.enums.SalesTeamStatus;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 销售收客订单业务服务。
 *
 * <p>服务层负责旧系统收客页主链路：保存订单主信息、价格明细、游客名单，并根据已确认订单
 * 聚合刷新团队实收和余位。Controller 不处理这些业务规则。</p>
 */
@Service
public class SalesBookingOrderService {

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private final SalesBookingOrderMapper orderMapper;
    private final SalesBookingOrderPriceLineMapper priceLineMapper;
    private final SalesBookingOrderGuestMapper guestMapper;
    private final SalesBookingOrderFeeChangeMapper feeChangeMapper;
    private final SalesTeamMapper teamMapper;
    private final IdCardValidator idCardValidator;

    /**
     * 单元测试兼容构造器。测试只验证主链路，可不显式传身份证校验器。
     */
    @Autowired
    public SalesBookingOrderService(
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderPriceLineMapper priceLineMapper,
            SalesBookingOrderGuestMapper guestMapper,
            SalesBookingOrderFeeChangeMapper feeChangeMapper,
            SalesTeamMapper teamMapper
    ) {
        this(orderMapper, priceLineMapper, guestMapper, feeChangeMapper, teamMapper, new IdCardValidator());
    }

    /**
     * 运行时构造器，注入订单、价格、游客、费用变更和团队 Mapper。
     */
    public SalesBookingOrderService(
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderPriceLineMapper priceLineMapper,
            SalesBookingOrderGuestMapper guestMapper,
            SalesBookingOrderFeeChangeMapper feeChangeMapper,
            SalesTeamMapper teamMapper,
            IdCardValidator idCardValidator
    ) {
        this.orderMapper = orderMapper;
        this.priceLineMapper = priceLineMapper;
        this.guestMapper = guestMapper;
        this.feeChangeMapper = feeChangeMapper;
        this.teamMapper = teamMapper;
        this.idCardValidator = idCardValidator;
    }

    /**
     * 保存收客订单。
     *
     * <p>新增订单时自动生成订单编号；修改订单时校验订单属于当前租户和团队。价格明细与游客名单
     * 按当前订单重建为未删除数据，避免逐行匹配导致历史脏数据残留。保存完成后按团队聚合刷新实收人数。</p>
     *
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 保存后的订单详情
     */
    @Transactional
    public SalesBookingOrderResponse save(SalesBookingOrderSaveRequest request, Long tenantId, String operator) {
        SalesTeamEntity team = requireTeam(request.teamId(), tenantId);
        SalesBookingOrderStatus status = parseStatus(request.status());
        SalesBookingOrderEntity current = request.id() == null ? null : requireOrder(request.id(), tenantId);
        assertTeamCanReceive(team, status, current == null);

        SalesBookingOrderEntity entity = current == null ? new SalesBookingOrderEntity() : new SalesBookingOrderEntity();
        applyOrderFields(entity, request, team, status, tenantId, operator, current);
        if (current == null) {
            orderMapper.insert(entity);
        } else {
            orderMapper.update(entity, baseOrderUpdate(tenantId).eq("id", current.getId()));
            entity.setId(current.getId());
        }

        rebuildPriceLines(entity, request.priceLines(), tenantId, operator);
        rebuildGuests(entity, request.guests(), tenantId, operator);
        refreshTeamSeats(team, tenantId);

        return toResponse(entity);
    }

    /**
     * 查询收客订单详情。
     *
     * @param orderId 订单 ID
     * @param tenantId 当前租户 ID
     * @return 订单详情
     */
    public SalesBookingOrderResponse detail(Long orderId, Long tenantId) {
        SalesBookingOrderEntity order = requireOrder(orderId, tenantId);
        return toResponse(order);
    }

    /**
     * 查询团队下订单列表。
     *
     * <p>供团队操作页展示订单行使用，只加载当前团队订单，不附带游客名单和价格明细大对象。</p>
     */
    public List<SalesBookingOrderEntity> listOrdersByTeam(Long teamId, Long tenantId) {
        return orderMapper.selectActiveByTeam(tenantId, teamId);
    }

    private SalesBookingOrderResponse toResponse(SalesBookingOrderEntity order) {
        Long tenantId = order.getTenantId();
        Long orderId = order.getId();
        List<SalesBookingOrderPriceLineEntity> priceLineEntities = Objects.requireNonNullElse(
                priceLineMapper.selectList(basePriceLineQuery(tenantId)
                        .eq("order_id", orderId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id")),
                List.of()
        );
        List<SalesBookingOrderPriceLineResponse> priceLines = priceLineEntities
                .stream()
                .map(SalesBookingOrderPriceLineResponse::fromEntity)
                .toList();
        List<SalesBookingOrderGuestEntity> guestEntities = Objects.requireNonNullElse(
                guestMapper.selectList(baseGuestQuery(tenantId)
                        .eq("order_id", orderId)
                        .orderByAsc("index_no")
                        .orderByAsc("id")),
                List.of()
        );
        List<SalesBookingOrderGuestResponse> guests = guestEntities
                .stream()
                .map(SalesBookingOrderGuestResponse::fromEntity)
                .toList();
        List<SalesBookingOrderFeeChangeEntity> feeChangeEntities = Objects.requireNonNullElse(
                feeChangeMapper.selectList(baseFeeChangeQuery(tenantId)
                        .eq("order_id", orderId)
                        .orderByDesc("registered_at")
                        .orderByDesc("id")),
                List.of()
        );
        List<SalesBookingFeeChangeResponse> feeChanges = feeChangeEntities
                .stream()
                .map(SalesBookingFeeChangeResponse::fromEntity)
                .toList();
        return SalesBookingOrderResponse.fromEntity(order, priceLines, guests, feeChanges);
    }

    private void applyOrderFields(
            SalesBookingOrderEntity entity,
            SalesBookingOrderSaveRequest request,
            SalesTeamEntity team,
            SalesBookingOrderStatus status,
            Long tenantId,
            String operator,
            SalesBookingOrderEntity current
    ) {
        List<SalesBookingOrderGuestRequest> guests = Objects.requireNonNullElse(request.guests(), List.of());
        List<SalesBookingOrderPriceLineRequest> priceLines = Objects.requireNonNullElse(request.priceLines(), List.of());
        entity.setTenantId(tenantId);
        entity.setTeamId(team.getId());
        entity.setOrderNo(resolveOrderNo(current));
        entity.setCustomerId(request.customerId());
        entity.setCustomerName(clean(request.customerName()));
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setCustomerTeamNo(clean(request.customerTeamNo()));
        entity.setSourceProvince(clean(request.sourceProvince()));
        entity.setSourceCity(clean(request.sourceCity()));
        entity.setSourceDistrict(clean(request.sourceDistrict()));
        entity.setTravelDescription(clean(request.travelDescription()));
        entity.setPickupInfo(clean(request.pickupInfo()));
        entity.setDropoffInfo(clean(request.dropoffInfo()));
        entity.setPickupRemark(clean(request.pickupRemark()));
        entity.setGuideName(clean(request.guideName()));
        entity.setGuidePhone(clean(request.guidePhone()));
        entity.setGuideRemark(clean(request.guideRemark()));
        entity.setHotelInfo(clean(request.hotelInfo()));
        entity.setAdultCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.ADULT));
        entity.setChildCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.CHILD));
        entity.setChildNoBedCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.CHILD_NO_BED));
        entity.setSeniorCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.SENIOR));
        entity.setEscortCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.ESCORT));
        entity.setGuestCount(resolveOrderGuestCount(priceLines, guests));
        BigDecimal receivable = sumReceivable(priceLines);
        BigDecimal received = money(request.receivedAmount());
        entity.setReceivableAmount(receivable);
        entity.setReceivedAmount(received);
        entity.setBalanceAmount(receivable.subtract(received).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        entity.setFeeRemark(clean(request.feeRemark()));
        entity.setConfirmRemark(clean(request.confirmRemark()));
        entity.setOrderRemark(clean(request.orderRemark()));
        entity.setStatus(status.value());
        entity.setBookedBy(current == null || !StringUtils.hasText(current.getBookedBy()) ? operator : current.getBookedBy());
        entity.setBookedAt(current == null || current.getBookedAt() == null ? OffsetDateTime.now() : current.getBookedAt());
        entity.setCreatedBy(current == null ? operator : current.getCreatedBy());
        entity.setIsDeleted(false);
    }

    private void rebuildPriceLines(
            SalesBookingOrderEntity order,
            List<SalesBookingOrderPriceLineRequest> requests,
            Long tenantId,
            String operator
    ) {
        softDeletePriceLines(order.getId(), tenantId, operator);
        List<SalesBookingOrderPriceLineRequest> lines = Objects.requireNonNullElse(requests, List.of());
        for (int index = 0; index < lines.size(); index++) {
            SalesBookingOrderPriceLineRequest request = lines.get(index);
            if (isBlankPriceLine(request)) {
                continue;
            }
            SalesBookingOrderPriceLineEntity entity = new SalesBookingOrderPriceLineEntity();
            entity.setTenantId(tenantId);
            entity.setOrderId(order.getId());
            entity.setTeamId(order.getTeamId());
            entity.setLineType(resolveLineType(request.lineType()));
            entity.setItemName(resolveItemName(request.itemName(), entity.getLineType()));
            entity.setUnitPrice(money(request.unitPrice()));
            entity.setQuantity(money(request.quantity()));
            entity.setSubtotalAmount(entity.getUnitPrice().multiply(entity.getQuantity()).setScale(2, RoundingMode.HALF_UP));
            entity.setSortOrder(index + 1);
            entity.setRemark(clean(request.remark()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            priceLineMapper.insert(entity);
        }
    }

    private void rebuildGuests(
            SalesBookingOrderEntity order,
            List<SalesBookingOrderGuestRequest> requests,
            Long tenantId,
            String operator
    ) {
        softDeleteGuests(order.getId(), tenantId, operator);
        List<SalesBookingOrderGuestRequest> guests = Objects.requireNonNullElse(requests, List.of());
        for (int index = 0; index < guests.size(); index++) {
            SalesBookingOrderGuestRequest request = guests.get(index);
            if (!StringUtils.hasText(request.guestName())) {
                continue;
            }
            SalesBookingOrderGuestEntity entity = new SalesBookingOrderGuestEntity();
            entity.setTenantId(tenantId);
            entity.setOrderId(order.getId());
            entity.setTeamId(order.getTeamId());
            entity.setIndexNo(request.indexNo() == null ? index + 1 : request.indexNo());
            entity.setGuestName(clean(request.guestName()));
            entity.setEnglishName(clean(request.englishName()));
            entity.setCertificateNo(clean(request.certificateNo()));
            entity.setPassportNo(clean(request.passportNo()));
            entity.setPhone(clean(request.phone()));
            entity.setGuestType(resolveGuestType(request.guestType()));
            entity.setRoomGroup(clean(request.roomGroup()));
            entity.setRoomRemark(clean(request.roomRemark()));
            entity.setLeaderFlag(Boolean.TRUE.equals(request.leaderFlag()));
            entity.setRemark(clean(request.remark()));
            applyIdentityFields(entity, request);
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            guestMapper.insert(entity);
        }
    }

    private void applyIdentityFields(SalesBookingOrderGuestEntity entity, SalesBookingOrderGuestRequest request) {
        if (StringUtils.hasText(request.certificateNo())) {
            IdCardValidationResult result = idCardValidator.validate(request.certificateNo());
            entity.setIdCardValid(result.valid());
            entity.setIdCardWarning(String.join("；", result.warnings()));
            entity.setBirthDate(request.birthDate() == null && result.birthDate() != null
                    ? LocalDate.parse(result.birthDate())
                    : request.birthDate());
            entity.setGender(StringUtils.hasText(request.gender()) ? clean(request.gender()) : result.gender());
            entity.setAge(request.age() == null ? result.age() : request.age());
            return;
        }
        entity.setIdCardValid(null);
        entity.setIdCardWarning(null);
        entity.setBirthDate(request.birthDate());
        entity.setGender(clean(request.gender()));
        entity.setAge(request.age());
    }

    private void refreshTeamSeats(SalesTeamEntity team, Long tenantId) {
        int totalSeats = number(team.getTotalSeats());
        int usedSeats = number(orderMapper.sumGuestCountByTeam(tenantId, team.getId()));
        SalesTeamEntity update = new SalesTeamEntity();
        update.setUsedSeats(usedSeats);
        update.setRemainingSeats(Math.max(totalSeats - usedSeats, 0));
        teamMapper.update(update, baseTeamUpdate(tenantId).eq("id", team.getId()));
    }

    private void softDeletePriceLines(Long orderId, Long tenantId, String operator) {
        SalesBookingOrderPriceLineEntity update = new SalesBookingOrderPriceLineEntity();
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(operator);
        priceLineMapper.update(update, basePriceLineUpdate(tenantId).eq("order_id", orderId));
    }

    private void softDeleteGuests(Long orderId, Long tenantId, String operator) {
        SalesBookingOrderGuestEntity update = new SalesBookingOrderGuestEntity();
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(operator);
        guestMapper.update(update, baseGuestUpdate(tenantId).eq("order_id", orderId));
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
            throw new BizException("收客订单不存在或已删除");
        }
        return order;
    }

    private void assertTeamCanReceive(SalesTeamEntity team, SalesBookingOrderStatus status, boolean creating) {
        if (SalesTeamStatus.CANCELLED.getValue().equals(team.getStatus())) {
            throw new BizException("团队已取消，不能新增或确认订单");
        }
        if (SalesTeamStatus.STOPPED.getValue().equals(team.getStatus())
                && (creating || status == SalesBookingOrderStatus.CONFIRMED)) {
            throw new BizException("团队已暂停收客，不能新增或确认订单");
        }
    }

    private SalesBookingOrderStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return SalesBookingOrderStatus.PENDING;
        }
        for (SalesBookingOrderStatus status : SalesBookingOrderStatus.values()) {
            if (status.value().equals(value)) {
                return status;
            }
        }
        throw new BizException("订单状态不合法");
    }

    private BigDecimal sumReceivable(List<SalesBookingOrderPriceLineRequest> priceLines) {
        return Objects.requireNonNullElse(priceLines, List.<SalesBookingOrderPriceLineRequest>of()).stream()
                .filter(request -> !isBlankPriceLine(request))
                .map(request -> money(request.unitPrice()).multiply(money(request.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int countByPriceOrGuests(
            List<SalesBookingOrderPriceLineRequest> priceLines,
            List<SalesBookingOrderGuestRequest> guests,
            SalesBookingGuestType type
    ) {
        int priceCount = priceLineQuantity(priceLines, type);
        if (priceCount > 0) {
            return priceCount;
        }
        return countGuests(guests, type);
    }

    private int countGuests(List<SalesBookingOrderGuestRequest> guests, SalesBookingGuestType type) {
        return (int) guests.stream()
                .filter(request -> StringUtils.hasText(request.guestName()))
                .filter(request -> type.value().equals(resolveGuestType(request.guestType())))
                .count();
    }

    private int resolveOrderGuestCount(
            List<SalesBookingOrderPriceLineRequest> priceLines,
            List<SalesBookingOrderGuestRequest> guests
    ) {
        int pricePassengerCount = passengerPriceQuantity(priceLines);
        if (pricePassengerCount > 0) {
            return pricePassengerCount;
        }
        return (int) guests.stream()
                .filter(request -> StringUtils.hasText(request.guestName()))
                .count();
    }

    private int passengerPriceQuantity(List<SalesBookingOrderPriceLineRequest> priceLines) {
        return priceLineQuantity(priceLines, SalesBookingGuestType.ADULT)
                + priceLineQuantity(priceLines, SalesBookingGuestType.CHILD)
                + priceLineQuantity(priceLines, SalesBookingGuestType.CHILD_NO_BED)
                + priceLineQuantity(priceLines, SalesBookingGuestType.SENIOR)
                + priceLineQuantity(priceLines, SalesBookingGuestType.ESCORT);
    }

    private int priceLineQuantity(List<SalesBookingOrderPriceLineRequest> priceLines, SalesBookingGuestType type) {
        return Objects.requireNonNullElse(priceLines, List.<SalesBookingOrderPriceLineRequest>of()).stream()
                .filter(request -> request != null && type.value().equals(request.lineType()))
                .map(SalesBookingOrderPriceLineRequest::quantity)
                .filter(Objects::nonNull)
                .mapToInt(BigDecimal::intValue)
                .sum();
    }

    private boolean isBlankPriceLine(SalesBookingOrderPriceLineRequest request) {
        return request == null
                || (!StringUtils.hasText(request.itemName())
                && request.unitPrice() == null
                && request.quantity() == null
                && !StringUtils.hasText(request.lineType()));
    }

    private String resolveOrderNo(SalesBookingOrderEntity current) {
        if (current != null && StringUtils.hasText(current.getOrderNo())) {
            return current.getOrderNo();
        }
        return "SO-" + LocalDate.now().format(ORDER_DATE_FORMATTER) + "-" + System.currentTimeMillis() % 100_000;
    }

    private String resolveGuestType(String value) {
        if (!StringUtils.hasText(value)) {
            return SalesBookingGuestType.ADULT.value();
        }
        return SalesBookingGuestType.valid(value) ? value : SalesBookingGuestType.ADULT.value();
    }

    private String resolveLineType(String value) {
        if (!StringUtils.hasText(value)) {
            return "misc";
        }
        return clean(value);
    }

    private String resolveItemName(String itemName, String lineType) {
        if (StringUtils.hasText(itemName)) {
            return clean(itemName);
        }
        return switch (lineType) {
            case "adult" -> "成人";
            case "child" -> "儿童";
            case "child_no_bed" -> "儿童不占床";
            case "senior" -> "老人";
            case "escort" -> "全陪";
            default -> "其他费用";
        };
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

    private QueryWrapper<SalesBookingOrderPriceLineEntity> basePriceLineQuery(Long tenantId) {
        return new QueryWrapper<SalesBookingOrderPriceLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesBookingOrderPriceLineEntity> basePriceLineUpdate(Long tenantId) {
        return new UpdateWrapper<SalesBookingOrderPriceLineEntity>()
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

    private QueryWrapper<SalesBookingOrderFeeChangeEntity> baseFeeChangeQuery(Long tenantId) {
        return new QueryWrapper<SalesBookingOrderFeeChangeEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
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

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 将订单列表转换为团队操作页订单行。
     *
     * <p>该方法集中处理金额和状态展示，避免团队服务重复了解订单状态细节。</p>
     */
    public List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRow> toOperationRows(
            List<SalesBookingOrderEntity> orders
    ) {
        if (CollectionUtils.isEmpty(orders)) {
            return List.of();
        }
        List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRow> rows = new ArrayList<>();
        for (SalesBookingOrderEntity order : orders) {
            rows.add(new com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRow(
                    order.getId(),
                    order.getOrderNo(),
                    joinNonBlank(order.getCustomerName(), order.getCustomerTeamNo()),
                    order.getPickupRemark(),
                    joinNonBlank(order.getSourceProvince(), order.getSourceCity(), order.getSourceDistrict()),
                    firstLeaderOrCustomer(order),
                    order.getGuestCount(),
                    order.getFeeRemark(),
                    moneyText(order.getReceivableAmount()),
                    moneyText(order.getReceivedAmount()),
                    moneyText(order.getBalanceAmount()),
                    order.getFeeRemark(),
                    order.getOrderRemark(),
                    joinNonBlank(order.getBookedAt() == null ? null : order.getBookedAt().toLocalDate().toString(), order.getBookedBy()),
                    SalesBookingOrderStatus.labelOf(order.getStatus())
            ));
        }
        return rows;
    }

    private String firstLeaderOrCustomer(SalesBookingOrderEntity order) {
        if (StringUtils.hasText(order.getContactName())) {
            return order.getContactName();
        }
        return order.getCustomerName();
    }

    private String moneyText(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String joinNonBlank(String... values) {
        return java.util.Arrays.stream(values)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" / "));
    }
}
