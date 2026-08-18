package com.mtravel.platform.sales.team.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.enums.DispatchTeamGuideStatus;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementSectionStatusEntity;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementSectionStatusMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.team.dto.SalesTeamDisplayNameFormatter;
import com.mtravel.platform.sales.team.dto.SalesTeamListResponse;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamListSummaryEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamListSummaryMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 团队列表汇总表刷新服务。
 *
 * <p>{@code sales_team_list_summaries} 是团队管理列表的查询模型，不是业务事实表。订单、导游、
 * 团队安排或团队主信息变化后，统一通过本服务从事实表重算并覆盖汇总行，避免列表缓存变成旧数据。</p>
 */
@Service
public class SalesTeamListSummaryRefreshService {

    private static final String ARRANGE_STATUS_NONE = "none";
    private static final String ARRANGE_STATUS_PENDING = "pending";
    private static final String ARRANGE_STATUS_CONFIRMED = "confirmed";
    private static final String ARRANGEMENT_STATUS_ACTIVE = "active";

    private final SalesTeamMapper teamMapper;
    private final SalesTeamListSummaryMapper summaryMapper;
    private final SalesProductMapper productMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final DispatchTeamGuideMapper guideMapper;
    private final DispatchTeamArrangementMapper arrangementMapper;
    private final DispatchTeamArrangementSectionStatusMapper sectionStatusMapper;

    /**
     * 构造团队列表汇总刷新服务。
     *
     * @param teamMapper 团队主表访问对象
     * @param summaryMapper 团队列表汇总表访问对象
     * @param productMapper 产品快照访问对象
     * @param orderMapper 收客订单访问对象
     * @param guideMapper 团队导游安排访问对象
     * @param arrangementMapper 正式团队安排访问对象
     * @param sectionStatusMapper 团队安排分类状态访问对象
     */
    public SalesTeamListSummaryRefreshService(
            SalesTeamMapper teamMapper,
            SalesTeamListSummaryMapper summaryMapper,
            SalesProductMapper productMapper,
            SalesBookingOrderMapper orderMapper,
            DispatchTeamGuideMapper guideMapper,
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementSectionStatusMapper sectionStatusMapper
    ) {
        this.teamMapper = teamMapper;
        this.summaryMapper = summaryMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.guideMapper = guideMapper;
        this.arrangementMapper = arrangementMapper;
        this.sectionStatusMapper = sectionStatusMapper;
    }

    /**
     * 刷新单个团队列表汇总行。
     *
     * <p>刷新时不基于本次变更做字符串增量拼接，而是重新读取该团队所有有效订单、导游和安排记录后覆盖整行。
     * 这样导游删除、订单取消、客户清空等场景也能把旧摘要清掉。</p>
     *
     * @param teamId 团队 ID
     * @param tenantId 当前租户 ID
     */
    public void refresh(Long teamId, Long tenantId) {
        if (teamId == null || tenantId == null) {
            return;
        }
        SalesTeamEntity team = teamMapper.selectOne(baseTeamQuery(tenantId).eq("id", teamId));
        if (team == null) {
            markSummaryDeleted(teamId, tenantId);
            return;
        }

        SalesProductEntity product = team.getProductId() == null
                ? null
                : productMapper.selectOne(baseProductQuery(tenantId).eq("id", team.getProductId()));
        List<SalesBookingOrderEntity> visibleOrders = loadVisibleOrders(teamId, tenantId);
        List<DispatchTeamGuideEntity> activeGuides = loadActiveGuides(teamId, tenantId);
        SalesTeamListResponse.ArrangePlans plans = loadArrangePlans(teamId, tenantId, activeGuides);

        SalesTeamListSummaryEntity summary = buildSummary(team, product, visibleOrders, activeGuides, plans, tenantId);
        upsertSummary(summary);
    }

    /**
     * 刷新租户下所有有效团队汇总行。
     *
     * <p>用于修复历史缓存脏数据。该方法只重算汇总表，不改变订单、游客、导游或团队安排事实表。</p>
     *
     * @param tenantId 当前租户 ID
     * @return 刷新的团队数量
     */
    public int refreshAll(Long tenantId) {
        if (tenantId == null) {
            return 0;
        }
        List<SalesTeamEntity> teams = teamMapper.selectList(baseTeamQuery(tenantId).orderByAsc("id"));
        if (CollectionUtils.isEmpty(teams)) {
            return 0;
        }
        for (SalesTeamEntity team : teams) {
            refresh(team.getId(), tenantId);
        }
        return teams.size();
    }

    private SalesTeamListSummaryEntity buildSummary(
            SalesTeamEntity team,
            SalesProductEntity product,
            List<SalesBookingOrderEntity> visibleOrders,
            List<DispatchTeamGuideEntity> activeGuides,
            SalesTeamListResponse.ArrangePlans plans,
            Long tenantId
    ) {
        SalesTeamListSummaryEntity summary = new SalesTeamListSummaryEntity();
        summary.setTenantId(tenantId);
        summary.setTeamId(team.getId());
        summary.setTeamNo(team.getTeamNo());
        summary.setTeamName(displayTeamName(team, product));
        summary.setTeamType(team.getTeamType());
        summary.setStatus(team.getStatus());
        summary.setDepartureDate(team.getDepartureDate());
        int days = product != null && product.getTravelDays() != null && product.getTravelDays() > 0
                ? product.getTravelDays()
                : 1;
        summary.setTravelDays(days);
        summary.setEndDate(team.getDepartureDate() == null ? null : team.getDepartureDate().plusDays(days - 1L));
        summary.setDeparturePlace(product == null ? null : joinPlaceText(product.getProvince(), product.getCity(), product.getDistrict()));
        summary.setBusinessType(firstText(team.getBusinessType(), product == null ? null : product.getBusinessType()));
        summary.setDepartmentName(clean(team.getDepartmentName()));
        summary.setOperatorEmployeeName(clean(team.getOperatorEmployeeName()));
        summary.setCustomerSummary(joinDistinct(visibleOrders.stream().map(SalesBookingOrderEntity::getCustomerName).toList()));
        summary.setSalespersonSummary(joinDistinct(visibleOrders.stream().map(SalesBookingOrderEntity::getSalespersonEmployeeName).toList()));
        summary.setOrderStatusSummary(joinDistinct(visibleOrders.stream().map(SalesBookingOrderEntity::getStatus).toList()));
        summary.setGuideSummary(buildGuideSummary(activeGuides));
        summary.setTotalSeats(number(team.getTotalSeats()));
        summary.setUsedSeats(number(team.getUsedSeats()));
        summary.setRemainingSeats(number(team.getRemainingSeats()));
        summary.setGuidePlan(plans.guidePlan());
        summary.setTrafficPlan(plans.trafficPlan());
        summary.setHotelPlan(plans.hotelPlan());
        summary.setVehiclePlan(plans.vehiclePlan());
        summary.setScenicPlan(plans.scenicPlan());
        summary.setMealPlan(plans.mealPlan());
        summary.setOtherPlan(plans.otherPlan());
        summary.setOptionalPlan(plans.optionalPlan());
        summary.setShoppingPlan(plans.shoppingPlan());
        summary.setGroundAgentPlan(plans.groundAgentPlan());
        summary.setCreatedBy(team.getCreatedBy());
        summary.setCreatedAt(team.getCreatedAt());
        summary.setRemark(team.getRemark());
        summary.setIsDeleted(false);
        summary.setDeletedAt(null);
        summary.setDeletedBy(null);
        return summary;
    }

    private void upsertSummary(SalesTeamListSummaryEntity summary) {
        UpdateWrapper<SalesTeamListSummaryEntity> wrapper = new UpdateWrapper<SalesTeamListSummaryEntity>()
                .eq("tenant_id", summary.getTenantId())
                .eq("team_id", summary.getTeamId())
                .set("team_no", summary.getTeamNo())
                .set("team_name", summary.getTeamName())
                .set("team_type", summary.getTeamType())
                .set("status", summary.getStatus())
                .set("departure_date", summary.getDepartureDate())
                .set("end_date", summary.getEndDate())
                .set("departure_place", summary.getDeparturePlace())
                .set("travel_days", summary.getTravelDays())
                .set("business_type", summary.getBusinessType())
                .set("department_name", summary.getDepartmentName())
                .set("operator_employee_name", summary.getOperatorEmployeeName())
                .set("customer_summary", summary.getCustomerSummary())
                .set("salesperson_summary", summary.getSalespersonSummary())
                .set("guide_summary", summary.getGuideSummary())
                .set("order_status_summary", summary.getOrderStatusSummary())
                .set("total_seats", summary.getTotalSeats())
                .set("used_seats", summary.getUsedSeats())
                .set("remaining_seats", summary.getRemainingSeats())
                .set("guide_plan", summary.getGuidePlan())
                .set("traffic_plan", summary.getTrafficPlan())
                .set("hotel_plan", summary.getHotelPlan())
                .set("vehicle_plan", summary.getVehiclePlan())
                .set("scenic_plan", summary.getScenicPlan())
                .set("meal_plan", summary.getMealPlan())
                .set("other_plan", summary.getOtherPlan())
                .set("optional_plan", summary.getOptionalPlan())
                .set("shopping_plan", summary.getShoppingPlan())
                .set("ground_agent_plan", summary.getGroundAgentPlan())
                .set("created_by", summary.getCreatedBy())
                .set("created_at", summary.getCreatedAt())
                .set("remark", summary.getRemark())
                .set("is_deleted", false)
                .set("deleted_at", null)
                .set("deleted_by", null);
        int updated = summaryMapper.update(new SalesTeamListSummaryEntity(), wrapper);
        if (updated == 0) {
            summaryMapper.insert(summary);
        }
    }

    private void markSummaryDeleted(Long teamId, Long tenantId) {
        summaryMapper.update(new SalesTeamListSummaryEntity(), new UpdateWrapper<SalesTeamListSummaryEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .set("is_deleted", true)
                .set("deleted_at", OffsetDateTime.now()));
    }

    private List<SalesBookingOrderEntity> loadVisibleOrders(Long teamId, Long tenantId) {
        List<SalesBookingOrderEntity> orders = orderMapper.selectList(new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("team_id", teamId)
                .orderByAsc("id"));
        if (CollectionUtils.isEmpty(orders)) {
            return List.of();
        }
        return orders.stream().filter(this::visibleCustomerOrder).toList();
    }

    private boolean visibleCustomerOrder(SalesBookingOrderEntity order) {
        if (order == null || "cancelled".equals(order.getStatus())) {
            return false;
        }
        String role = StringUtils.hasText(order.getOrderRole()) ? order.getOrderRole() : "normal";
        return "normal".equals(role) || "merge_child".equals(role) || "merge_source".equals(role);
    }

    private List<DispatchTeamGuideEntity> loadActiveGuides(Long teamId, Long tenantId) {
        List<DispatchTeamGuideEntity> guides = guideMapper.selectList(new QueryWrapper<DispatchTeamGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("team_id", teamId)
                .eq("status", DispatchTeamGuideStatus.ACTIVE.getValue())
                .orderByAsc("id"));
        return Objects.requireNonNullElse(guides, List.of());
    }

    private String buildGuideSummary(List<DispatchTeamGuideEntity> guides) {
        if (CollectionUtils.isEmpty(guides)) {
            return null;
        }
        LinkedHashSet<String> names = guides.stream()
                .filter(guide -> StringUtils.hasText(guide.getGuideName()))
                .map(this::guideSummaryText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return names.isEmpty() ? null : String.join("、", names);
    }

    private String guideSummaryText(DispatchTeamGuideEntity guide) {
        String name = clean(guide.getGuideName());
        if (!StringUtils.hasText(guide.getGuideMobile())) {
            return name;
        }
        return name + "[Tel:" + clean(guide.getGuideMobile()) + "]";
    }

    private SalesTeamListResponse.ArrangePlans loadArrangePlans(
            Long teamId,
            Long tenantId,
            List<DispatchTeamGuideEntity> activeGuides
    ) {
        MutableArrangePlans plans = new MutableArrangePlans();
        applyTeamArrangementPlans(teamId, tenantId, plans);
        applyTeamArrangementSectionStatusPlans(teamId, tenantId, plans);
        applyGuidePlans(activeGuides, plans);
        return plans.toResponse();
    }

    private void applyTeamArrangementPlans(Long teamId, Long tenantId, MutableArrangePlans plans) {
        List<DispatchTeamArrangementEntity> arrangements = arrangementMapper.selectList(
                new QueryWrapper<DispatchTeamArrangementEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("team_id", teamId)
                        .eq("status", ARRANGEMENT_STATUS_ACTIVE)
        );
        if (CollectionUtils.isEmpty(arrangements)) {
            return;
        }
        for (DispatchTeamArrangementEntity arrangement : arrangements) {
            plans.merge(arrangement.getArrangementType(), ARRANGE_STATUS_PENDING);
        }
    }

    private void applyTeamArrangementSectionStatusPlans(Long teamId, Long tenantId, MutableArrangePlans plans) {
        List<DispatchTeamArrangementSectionStatusEntity> sectionStatuses = sectionStatusMapper.selectList(
                new QueryWrapper<DispatchTeamArrangementSectionStatusEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("team_id", teamId)
        );
        if (CollectionUtils.isEmpty(sectionStatuses)) {
            return;
        }
        for (DispatchTeamArrangementSectionStatusEntity sectionStatus : sectionStatuses) {
            String listStatus = switch (String.valueOf(sectionStatus.getStatus())) {
                case "done" -> ARRANGE_STATUS_CONFIRMED;
                case "none" -> ARRANGE_STATUS_NONE;
                default -> ARRANGE_STATUS_PENDING;
            };
            plans.set(sectionStatus.getArrangementType(), listStatus);
        }
    }

    private void applyGuidePlans(List<DispatchTeamGuideEntity> guides, MutableArrangePlans plans) {
        if (CollectionUtils.isEmpty(guides)) {
            return;
        }
        for (DispatchTeamGuideEntity guide : guides) {
            String status = Boolean.TRUE.equals(guide.getIsTentative())
                    ? ARRANGE_STATUS_PENDING
                    : ARRANGE_STATUS_CONFIRMED;
            plans.mergeGuide(status);
        }
    }

    private QueryWrapper<SalesTeamEntity> baseTeamQuery(Long tenantId) {
        return new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesProductEntity> baseProductQuery(Long tenantId) {
        return new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private String displayTeamName(SalesTeamEntity team, SalesProductEntity product) {
        if (StringUtils.hasText(team.getTeamName())) {
            return clean(team.getTeamName());
        }
        return product == null ? null : SalesTeamDisplayNameFormatter.productDisplayName(product.getProductName(), team.getTeamNo());
    }

    private String joinDistinct(List<String> values) {
        LinkedHashSet<String> cleaned = values.stream()
                .map(this::clean)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return cleaned.isEmpty() ? null : String.join("、", cleaned);
    }

    private String joinPlaceText(String province, String city, String district) {
        return Stream.of(province, city, district)
                .map(this::clean)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining());
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String cleaned = clean(value);
            if (StringUtils.hasText(cleaned)) {
                return cleaned;
            }
        }
        return null;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Integer number(Integer value) {
        return value == null ? 0 : value;
    }

    /** 团队列表安排状态的可变聚合对象，用于处理同一团队同一模块多条安排记录的优先级。 */
    private static class MutableArrangePlans {
        private String guidePlan = ARRANGE_STATUS_NONE;
        private String trafficPlan = ARRANGE_STATUS_NONE;
        private String hotelPlan = ARRANGE_STATUS_NONE;
        private String vehiclePlan = ARRANGE_STATUS_NONE;
        private String scenicPlan = ARRANGE_STATUS_NONE;
        private String mealPlan = ARRANGE_STATUS_NONE;
        private String otherPlan = ARRANGE_STATUS_NONE;
        private String optionalPlan = ARRANGE_STATUS_NONE;
        private String shoppingPlan = ARRANGE_STATUS_NONE;
        private String groundAgentPlan = ARRANGE_STATUS_NONE;

        void mergeGuide(String status) {
            guidePlan = higherStatus(guidePlan, status);
        }

        void merge(String arrangementType, String status) {
            switch (arrangementType) {
                case "traffic" -> trafficPlan = higherStatus(trafficPlan, status);
                case "hotel" -> hotelPlan = higherStatus(hotelPlan, status);
                case "vehicle" -> vehiclePlan = higherStatus(vehiclePlan, status);
                case "scenic" -> scenicPlan = higherStatus(scenicPlan, status);
                case "meal" -> mealPlan = higherStatus(mealPlan, status);
                case "other" -> otherPlan = higherStatus(otherPlan, status);
                case "optional" -> optionalPlan = higherStatus(optionalPlan, status);
                case "shopping" -> shoppingPlan = higherStatus(shoppingPlan, status);
                case "ground_agent" -> groundAgentPlan = higherStatus(groundAgentPlan, status);
                default -> {
                    // 列表只展示固定资源安排列，其它类型不参与图标聚合。
                }
            }
        }

        void set(String arrangementType, String status) {
            switch (arrangementType) {
                case "traffic" -> trafficPlan = status;
                case "hotel" -> hotelPlan = status;
                case "vehicle" -> vehiclePlan = status;
                case "scenic" -> scenicPlan = status;
                case "meal" -> mealPlan = status;
                case "other" -> otherPlan = status;
                case "optional" -> optionalPlan = status;
                case "shopping" -> shoppingPlan = status;
                case "ground_agent" -> groundAgentPlan = status;
                default -> {
                    // 列表只展示固定资源安排列，其它类型不参与图标聚合。
                }
            }
        }

        SalesTeamListResponse.ArrangePlans toResponse() {
            return new SalesTeamListResponse.ArrangePlans(
                    guidePlan,
                    trafficPlan,
                    hotelPlan,
                    vehiclePlan,
                    scenicPlan,
                    mealPlan,
                    otherPlan,
                    optionalPlan,
                    shoppingPlan,
                    groundAgentPlan
            );
        }

        private static String higherStatus(String current, String next) {
            return statusRank(next) > statusRank(current) ? next : current;
        }

        private static int statusRank(String status) {
            if (ARRANGE_STATUS_CONFIRMED.equals(status)) {
                return 2;
            }
            if (ARRANGE_STATUS_PENDING.equals(status)) {
                return 1;
            }
            return 0;
        }
    }
}
