package com.mtravel.platform.sales.team.dto;

import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.enums.SalesTeamStatus;
import com.mtravel.platform.sales.team.enums.SalesTeamType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 团队操作页只读详情返回对象。
 *
 * <p>该对象对应老系统“销售管理 / 团队操作”页面。第一版只聚合团队、产品说明、客户类型价格和
 * 操作按钮状态，订单列表等下游链路未完成前保持空数组，避免为了展示填充假数据。</p>
 */
public record SalesTeamOperationResponse(
        TeamInfo team,
        ProductInfo product,
        ContentInfo content,
        RouteSummary routeSummary,
        List<ItineraryDayInfo> itineraryDays,
        List<SalesTeamPriceResponse> prices,
        List<OrderRow> orders,
        List<ActionInfo> actions
) {
    /**
     * 团队主信息，前端顶部信息卡和指标区直接使用。
     */
    public record TeamInfo(
            Long id,
            Long productId,
            String teamNo,
            String teamType,
            String teamTypeLabel,
            String businessType,
            String status,
            String statusLabel,
            LocalDate departureDate,
            LocalDate endDate,
            Long departmentId,
            String departmentName,
            Long operatorEmployeeId,
            String operatorEmployeeName,
            Long escortEmployeeId,
            String escortEmployeeName,
            Integer totalSeats,
            Integer usedSeats,
            Integer remainingSeats,
            Integer travelDays,
            Integer closeDaysBefore,
            String guideSummary,
            String leaderSummary,
            String escortSummary
    ) {
    }

    /**
     * 产品模板信息，来源于团队关联的销售产品。
     */
    public record ProductInfo(
            Long id,
            String productName,
            String businessType,
            String domesticInternational,
            String receptionStandard,
            String productTheme,
            String departurePlace
    ) {
    }

    /**
     * 页面说明内容。内部备注取团队备注，产品说明和收客须知取产品说明表。
     */
    public record ContentInfo(
            String productDescription,
            String bookingNotice,
            String internalRemark,
            BigDecimal perCapitaPitAmount,
            BigDecimal optionalMarkupRate,
            BigDecimal perCapitaShoppingAmount
    ) {
    }

    /**
     * 团队行程路书汇总，来自产品每日行程维护的路书距离和车程。
     */
    public record RouteSummary(
            Integer totalDistanceMeters,
            Integer totalDurationSeconds
    ) {
    }

    /**
     * 团队操作页查看行程弹窗的每日行程信息。
     *
     * <p>当前取产品模板行程。后续如果团队单独调整行程，可扩展为团队行程快照，避免影响已成团数据。</p>
     */
    public record ItineraryDayInfo(
            Long id,
            Integer dayNo,
            String dayTitle,
            String itineraryContent,
            String accommodationNote,
            String relatedHotel,
            Boolean breakfastIncluded,
            Boolean lunchIncluded,
            Boolean dinnerIncluded,
            String roadbookSummary,
            Integer roadbookTotalDistanceMeters,
            Integer roadbookTotalDurationSeconds,
            String remark
    ) {
    }

    /**
     * 订单列表行占位结构。订单模块接入后按老系统订单表字段回填。
     */
    public record OrderRow(
            Long id,
            String orderNo,
            String orderRole,
            String orderRoleLabel,
            String orderInfo,
            String pickupInfo,
            String dropoffInfo,
            String originalOrderInfo,
            List<OrderRelationInfo> mergeOrderInfos,
            List<OrderRelationInfo> sourceOrderInfos,
            String pickupRemark,
            String sourcePlace,
            String guestName,
            Integer guestCount,
            String guestCountText,
            String priceDetail,
            String receivableAmount,
            String receivedAmount,
            String balanceAmount,
            String feeRemark,
            String orderRemark,
            String bookingInfo,
            String status
    ) {
    }

    /**
     * 订单拼团关系展示信息。
     *
     * <p>目标团拼入订单使用 originalOrderInfo 回链来源订单；来源订单使用本结构展示已拼到哪些目标团。</p>
     */
    public record OrderRelationInfo(
            Long orderId,
            Long teamId,
            String summary
    ) {
    }

    /**
     * 团队操作按钮状态。高风险动作第一版仅提示待接入或需要选择订单。
     */
    public record ActionInfo(
            String code,
            String label,
            String group,
            boolean enabled,
            String target,
            String note
    ) {
    }

    /** 将团队、产品、说明和价格行组装为团队操作页详情。 */
    public static SalesTeamOperationResponse from(
            SalesTeamEntity team,
            SalesProductEntity product,
            SalesProductDescriptionEntity description,
            List<SalesTeamPriceResponse> prices
    ) {
        return from(team, product, description, prices, List.of());
    }

    /** 将团队、产品、说明、价格行和每日行程组装为团队操作页详情。 */
    public static SalesTeamOperationResponse from(
            SalesTeamEntity team,
            SalesProductEntity product,
            SalesProductDescriptionEntity description,
            List<SalesTeamPriceResponse> prices,
            List<SalesProductItineraryDayEntity> itineraryDays
    ) {
        return from(team, product, description, prices, itineraryDays, List.of());
    }

    /** 将团队、产品、说明、价格行、每日行程和订单行组装为团队操作页详情。 */
    public static SalesTeamOperationResponse from(
            SalesTeamEntity team,
            SalesProductEntity product,
            SalesProductDescriptionEntity description,
            List<SalesTeamPriceResponse> prices,
            List<SalesProductItineraryDayEntity> itineraryDays,
            List<OrderRow> orders
    ) {
        return from(team, product, description, prices, itineraryDays, orders, null, null);
    }

    /** 将团队操作页详情组装为返回对象，并带上执行页顶部的导游和领队摘要。 */
    public static SalesTeamOperationResponse from(
            SalesTeamEntity team,
            SalesProductEntity product,
            SalesProductDescriptionEntity description,
            List<SalesTeamPriceResponse> prices,
            List<SalesProductItineraryDayEntity> itineraryDays,
            List<OrderRow> orders,
            String guideSummary,
            String leaderSummary
    ) {
        Integer travelDays = resolveTravelDays(product);
        LocalDate endDate = team.getDepartureDate() == null ? null : team.getDepartureDate().plusDays(travelDays - 1L);
        List<ItineraryDayInfo> dayInfos = toItineraryDayInfos(itineraryDays);
        return new SalesTeamOperationResponse(
                new TeamInfo(
                        team.getId(),
                        team.getProductId(),
                        team.getTeamNo(),
                        team.getTeamType(),
                        teamTypeLabel(team.getTeamType()),
                        team.getBusinessType(),
                        team.getStatus(),
                        statusLabel(team.getStatus()),
                        team.getDepartureDate(),
                        endDate,
                        team.getDepartmentId(),
                        team.getDepartmentName(),
                        team.getOperatorEmployeeId(),
                        team.getOperatorEmployeeName(),
                        team.getEscortEmployeeId(),
                        team.getEscortEmployeeName(),
                        team.getTotalSeats(),
                        team.getUsedSeats(),
                        team.getRemainingSeats(),
                        travelDays,
                        team.getCloseDaysBefore(),
                        guideSummary,
                        leaderSummary,
                        team.getEscortEmployeeName()
                ),
                product == null ? null : new ProductInfo(
                        product.getId(),
                        team.getTeamName() == null || team.getTeamName().isBlank()
                                ? SalesTeamDisplayNameFormatter.productDisplayName(product.getProductName(), team.getTeamNo())
                                : team.getTeamName().trim(),
                        product.getBusinessType(),
                        product.getDomesticInternational(),
                        product.getReceptionStandard(),
                        product.getProductTheme(),
                        joinPlace(product.getProvince(), product.getCity(), product.getDistrict())
                ),
                new ContentInfo(
                        description == null ? null : description.getProductDescription(),
                        description == null ? null : description.getBookingNotice(),
                        team.getRemark(),
                        team.getPerCapitaPitAmount(),
                        team.getOptionalMarkupRate(),
                        team.getPerCapitaShoppingAmount()
                ),
                routeSummary(dayInfos),
                dayInfos,
                prices == null ? List.of() : prices,
                orders == null ? List.of() : orders,
                defaultActions()
        );
    }

    private static List<ItineraryDayInfo> toItineraryDayInfos(List<SalesProductItineraryDayEntity> itineraryDays) {
        return Objects.requireNonNullElse(itineraryDays, List.<SalesProductItineraryDayEntity>of()).stream()
                .map(day -> new ItineraryDayInfo(
                        day.getId(),
                        day.getDayNo(),
                        day.getDayTitle(),
                        day.getItineraryContent(),
                        day.getAccommodationNote(),
                        day.getRelatedHotel(),
                        day.getBreakfastIncluded(),
                        day.getLunchIncluded(),
                        day.getDinnerIncluded(),
                        day.getRoadbookSummary(),
                        day.getRoadbookTotalDistanceMeters(),
                        day.getRoadbookTotalDurationSeconds(),
                        day.getRemark()
                ))
                .toList();
    }

    private static RouteSummary routeSummary(List<ItineraryDayInfo> itineraryDays) {
        int totalDistanceMeters = itineraryDays.stream()
                .map(ItineraryDayInfo::roadbookTotalDistanceMeters)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int totalDurationSeconds = itineraryDays.stream()
                .map(ItineraryDayInfo::roadbookTotalDurationSeconds)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        return new RouteSummary(totalDistanceMeters, totalDurationSeconds);
    }

    private static Integer resolveTravelDays(SalesProductEntity product) {
        return product != null && product.getTravelDays() != null && product.getTravelDays() > 0
                ? product.getTravelDays()
                : 1;
    }

    private static String teamTypeLabel(String value) {
        return Stream.of(SalesTeamType.values())
                .filter(item -> item.getValue().equals(value))
                .map(SalesTeamType::getLabel)
                .findFirst()
                .orElse("--");
    }

    private static String statusLabel(String value) {
        return Stream.of(SalesTeamStatus.values())
                .filter(item -> item.getValue().equals(value))
                .map(SalesTeamStatus::getLabel)
                .findFirst()
                .orElse("--");
    }

    private static String joinPlace(String province, String city, String district) {
        List<String> parts = Stream.of(province, city, district)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
        return parts.isEmpty() ? null : parts.stream().collect(Collectors.joining(""));
    }

    private static List<ActionInfo> defaultActions() {
        return List.of(
                new ActionInfo("orderFile", "订单文件", "tool", true, null, "订单文件页待接入"),
                new ActionInfo("printItinerary", "打印行程单", "tool", true, null, "打印功能待接入"),
                new ActionInfo("printGuestList", "打印团队名单", "tool", true, null, "打印功能待接入"),
                new ActionInfo("printSettlement", "打印团队结算单", "tool", true, null, "打印功能待接入"),
                new ActionInfo("exportPickup", "导出接送机名单", "tool", true, null, "导出功能待接入"),
                new ActionInfo("eContract", "电子合同", "business", true, null, "电子合同页待接入"),
                new ActionInfo("insideMemo", "内部备注", "business", true, null, "内部备注编辑待接入"),
                new ActionInfo("copyTeam", "复制团队", "business", true, null, "复制团队待接入"),
                new ActionInfo("mergeOrder", "拼团操作", "business", true, null, "请先选择订单"),
                new ActionInfo("moveOrder", "转团操作", "business", true, null, "请先选择订单"),
                new ActionInfo("editTeam", "修改团队", "business", true, null, "修改团队页待接入"),
                new ActionInfo("stopBooking", "暂停收客", "danger", true, null, "状态动作待接入"),
                new ActionInfo("bookingOrder", "收客订单", "business", true, null, "收客订单页待接入"),
                new ActionInfo("cancelTeam", "取消团队", "danger", true, null, "状态动作待接入"),
                new ActionInfo("teamArrangement", "团队安排", "business", true, null, "跳转团队安排"),
                new ActionInfo("shoppingReconciliation", "购物核对/补佣", "business", true, null, "录入购物店实际反馈并计算公司补佣"),
                new ActionInfo("guideBill", "导游报账", "business", true, null, "导游报账页待接入")
        );
    }
}
