package com.mtravel.platform.sales.team.dto;

import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 销售团队管理列表返回对象。
 *
 * <p>用于复刻老系统团队管理全局列表。团队主信息来自 sales_teams，产品名称、业务类型、天数和出发地
 * 由 sales_products 批量补充；订单、客户、导游和资源安排状态后续接正式业务表后再回填。</p>
 */
public record SalesTeamListResponse(
        Long id,
        Long productId,
        String teamNo,
        String teamType,
        String status,
        String productName,
        String businessType,
        Integer travelDays,
        LocalDate departureDate,
        LocalDate endDate,
        String departurePlace,
        Integer totalSeats,
        Integer usedSeats,
        Integer remainingSeats,
        String customerSummary,
        String guideSummary,
        String guidePlan,
        String trafficPlan,
        String hotelPlan,
        String vehiclePlan,
        String scenicPlan,
        String mealPlan,
        String otherPlan,
        String optionalPlan,
        String shoppingPlan,
        String groundAgentPlan,
        String operatorEmployeeName,
        String remark,
        OffsetDateTime createdAt
) {
    /** 将团队实体和产品实体合并为团队管理列表行。 */
    public static SalesTeamListResponse fromEntity(SalesTeamEntity team, SalesProductEntity product) {
        return fromEntity(team, product, ArrangePlans.empty(), null);
    }

    /** 将团队实体、产品实体和资源安排状态合并为团队管理列表行。 */
    public static SalesTeamListResponse fromEntity(
            SalesTeamEntity team,
            SalesProductEntity product,
            ArrangePlans plans
    ) {
        return fromEntity(team, product, plans, null);
    }

    /** 将团队实体、产品实体、资源安排状态和客户摘要合并为团队管理列表行。 */
    public static SalesTeamListResponse fromEntity(
            SalesTeamEntity team,
            SalesProductEntity product,
            ArrangePlans plans,
            String customerSummary
    ) {
        return fromEntity(team, product, plans, customerSummary, null);
    }

    /** 将团队实体、产品实体、资源安排状态、客户摘要和导游摘要合并为团队管理列表行。 */
    public static SalesTeamListResponse fromEntity(
            SalesTeamEntity team,
            SalesProductEntity product,
            ArrangePlans plans,
            String customerSummary,
            String guideSummary
    ) {
        ArrangePlans safePlans = plans == null ? ArrangePlans.empty() : plans;
        int days = product != null && product.getTravelDays() != null && product.getTravelDays() > 0
                ? product.getTravelDays()
                : 1;
        LocalDate endDate = team.getDepartureDate() == null ? null : team.getDepartureDate().plusDays(days - 1L);
        return new SalesTeamListResponse(
                team.getId(),
                team.getProductId(),
                team.getTeamNo(),
                team.getTeamType(),
                team.getStatus(),
                product == null ? null : product.getProductName(),
                team.getBusinessType() == null ? product == null ? null : product.getBusinessType() : team.getBusinessType(),
                days,
                team.getDepartureDate(),
                endDate,
                product == null ? null : joinPlace(product.getProvince(), product.getCity(), product.getDistrict()),
                team.getTotalSeats(),
                team.getUsedSeats(),
                team.getRemainingSeats(),
                customerSummary,
                guideSummary,
                safePlans.guidePlan(),
                safePlans.trafficPlan(),
                safePlans.hotelPlan(),
                safePlans.vehiclePlan(),
                safePlans.scenicPlan(),
                safePlans.mealPlan(),
                safePlans.otherPlan(),
                safePlans.optionalPlan(),
                safePlans.shoppingPlan(),
                safePlans.groundAgentPlan(),
                team.getOperatorEmployeeName(),
                team.getRemark(),
                team.getCreatedAt()
        );
    }

    /**
     * 团队列表资源安排状态。
     *
     * <p>字段值供前端统一映射图标：none 未安排，pending 已录入待确认，confirmed 已确认。</p>
     */
    public record ArrangePlans(
            String guidePlan,
            String trafficPlan,
            String hotelPlan,
            String vehiclePlan,
            String scenicPlan,
            String mealPlan,
            String otherPlan,
            String optionalPlan,
            String shoppingPlan,
            String groundAgentPlan
    ) {
        /** 创建默认空安排状态。 */
        public static ArrangePlans empty() {
            return new ArrangePlans("none", "none", "none", "none", "none", "none", "none", "none", "none", "none");
        }
    }

    private static String joinPlace(String province, String city, String district) {
        List<String> parts = Stream.of(province, city, district)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
        return parts.isEmpty() ? null : parts.stream().collect(Collectors.joining(""));
    }
}
