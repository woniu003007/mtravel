package com.mtravel.platform.agent.product.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerCapability;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.customer.productauth.entity.CustomerProductAuthorizationEntity;
import com.mtravel.platform.customer.productauth.mapper.CustomerProductAuthorizationMapper;
import com.mtravel.platform.sales.product.dto.SalesProductItineraryDayResponse;
import com.mtravel.platform.sales.product.dto.SalesProductResponse;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.product.service.SalesProductService;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** Agent 产品搜索、授权校验和公开详情服务。 */
@Service
public class AgentProductService {

    private final SalesProductMapper productMapper;
    private final CustomerProductAuthorizationMapper authorizationMapper;
    private final SalesTeamMapper teamMapper;
    private final AgentCustomerService customerService;
    private final SalesProductService productService;

    public AgentProductService(
            SalesProductMapper productMapper,
            CustomerProductAuthorizationMapper authorizationMapper,
            SalesTeamMapper teamMapper,
            AgentCustomerService customerService,
            SalesProductService productService
    ) {
        this.productMapper = productMapper;
        this.authorizationMapper = authorizationMapper;
        this.teamMapper = teamMapper;
        this.customerService = customerService;
        this.productService = productService;
    }

    /** 按客户授权和搜索条件返回可销售产品摘要。 */
    public AgentProductApi.SearchResult search(Long tenantId, AgentProductApi.SearchRequest request) {
        validateSearchRequest(request);
        AgentCustomerAccess access = customerService.requireCapability(
                tenantId,
                request.customerId(),
                AgentCustomerCapability.QUERY_PRODUCTS
        );
        int pageNo = request.page() == null ? 1 : request.page();
        int pageSize = request.pageSize() == null ? 20 : Math.min(request.pageSize(), 50);
        Set<Long> authorizedProductIds = authorizedProductIds(
                tenantId,
                request.customerId(),
                access.publicContext().productAccessMode(),
                LocalDate.now()
        );
        if ("authorized_only".equals(access.publicContext().productAccessMode()) && authorizedProductIds.isEmpty()) {
            return new AgentProductApi.SearchResult(List.of(), pageNo, pageSize, 0, OffsetDateTime.now());
        }

        QueryWrapper<SalesProductEntity> wrapper = new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_scope", "template")
                .eq("status", "active")
                .in("authorized_only".equals(access.publicContext().productAccessMode()), "id", authorizedProductIds)
                .in(!CollectionUtils.isEmpty(request.businessTypes()), "business_type", clean(request.businessTypes()))
                .in(!CollectionUtils.isEmpty(request.productThemes()), "product_theme", clean(request.productThemes()))
                .in(!CollectionUtils.isEmpty(request.receptionStandards()), "reception_standard", clean(request.receptionStandards()))
                .ge(request.travelDays() != null && request.travelDays().min() != null, "travel_days",
                        request.travelDays() == null ? null : request.travelDays().min())
                .le(request.travelDays() != null && request.travelDays().max() != null, "travel_days",
                        request.travelDays() == null ? null : request.travelDays().max());
        applyKeywordFilter(wrapper, request.keyword());
        applyDestinationFilter(wrapper, request.destinations());
        applyScheduleFilter(wrapper, tenantId, request);
        wrapper.orderByDesc("updated_at").orderByDesc("id");

        Page<SalesProductEntity> page = productMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, SalesTeamEntity> nearestTeams = nearestTeams(tenantId, page.getRecords(), request);
        int requestedSeats = request.party() == null ? 0 : request.party().totalSeats();
        List<AgentProductApi.SearchItem> items = page.getRecords().stream()
                .map(product -> toSearchItem(product, nearestTeams.get(product.getId()), requestedSeats))
                .toList();
        return new AgentProductApi.SearchResult(items, pageNo, pageSize, page.getTotal(), OffsetDateTime.now());
    }

    /** 查询严格白名单化的产品详情。 */
    public AgentProductApi.ProductDetail detail(Long tenantId, Long customerId, Long productId) {
        SalesProductEntity entity = requireProductEntity(tenantId, customerId, productId);
        SalesProductResponse detail = productService.detail(productId, tenantId);
        List<AgentProductApi.ItineraryDay> itinerary = detail.itineraryDays() == null ? List.of()
                : detail.itineraryDays().stream().map(this::toItineraryDay).toList();
        BigDecimal roomDifference = money(detail.singleRoomDifference());
        AgentProductApi.DefaultSingleRoomSupplement supplement = roomDifference.signum() > 0
                ? new AgentProductApi.DefaultSingleRoomSupplement(amount(roomDifference), "CNY", "confirmed")
                : new AgentProductApi.DefaultSingleRoomSupplement(null, "CNY", "manual_quote");
        return new AgentProductApi.ProductDetail(
                entity.getId(), productCode(entity.getId()), detail.productName(), detail.businessType(),
                detail.domesticInternational(),
                new AgentProductApi.ReceivingLocation(detail.province(), detail.city(), detail.district()),
                detail.tripType(), detail.receptionStandard(), detail.productTheme(), detail.travelDays(),
                detail.closeDaysBefore(), supplement, detail.bookingNotice(), detail.productDescription(),
                detail.feeIncluded(), detail.feeExcluded(), detail.childPolicy(), detail.shoppingArrangement(),
                detail.optionalItems(), detail.giftItems(), detail.attentionItems(), detail.warmReminder(),
                itinerary, "product-%d-%d".formatted(entity.getId(), version(entity.getUpdatedAt())),
                entity.getUpdatedAt()
        );
    }

    /** 校验客户产品访问权限并返回产品实体。 */
    public SalesProductEntity requireProductEntity(Long tenantId, Long customerId, Long productId) {
        AgentCustomerAccess access = customerService.requireCapability(
                tenantId,
                customerId,
                AgentCustomerCapability.QUERY_PRODUCTS
        );
        SalesProductEntity product = productMapper.selectOne(new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", productId)
                .eq("product_scope", "template")
                .eq("status", "active")
                .eq("is_deleted", false)
                .last("LIMIT 1"));
        if (product == null || !isAuthorized(
                tenantId,
                customerId,
                productId,
                access.publicContext().productAccessMode(),
                LocalDate.now()
        )) {
            throw AgentException.resourceNotFound();
        }
        return product;
    }

    private boolean isAuthorized(Long tenantId, Long customerId, Long productId, String accessMode, LocalDate onDate) {
        if ("all_active".equals(accessMode)) return true;
        if (!"authorized_only".equals(accessMode)) return false;
        return authorizationMapper.selectCount(new QueryWrapper<CustomerProductAuthorizationEntity>()
                .eq("tenant_id", tenantId)
                .eq("customer_id", customerId)
                .eq("product_id", productId)
                .eq("authorization_status", "active")
                .eq("is_deleted", false)
                .and(nested -> nested.isNull("authorized_start_date").or().le("authorized_start_date", onDate))
                .and(nested -> nested.isNull("authorized_end_date").or().ge("authorized_end_date", onDate))) > 0;
    }

    private Set<Long> authorizedProductIds(Long tenantId, Long customerId, String mode, LocalDate onDate) {
        if ("all_active".equals(mode)) return Set.of();
        if (!"authorized_only".equals(mode)) return Set.of();
        return authorizationMapper.selectList(new QueryWrapper<CustomerProductAuthorizationEntity>()
                        .select("product_id")
                        .eq("tenant_id", tenantId)
                        .eq("customer_id", customerId)
                        .eq("authorization_status", "active")
                        .eq("is_deleted", false)
                        .isNotNull("product_id")
                        .and(nested -> nested.isNull("authorized_start_date").or().le("authorized_start_date", onDate))
                        .and(nested -> nested.isNull("authorized_end_date").or().ge("authorized_end_date", onDate)))
                .stream()
                .map(CustomerProductAuthorizationEntity::getProductId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Map<Long, SalesTeamEntity> nearestTeams(
            Long tenantId,
            List<SalesProductEntity> products,
            AgentProductApi.SearchRequest request
    ) {
        if (products.isEmpty()) return Map.of();
        List<Long> ids = products.stream().map(SalesProductEntity::getId).toList();
        LocalDate from = request.departureDate() == null || request.departureDate().from() == null
                ? LocalDate.now() : request.departureDate().from();
        LocalDate to = request.departureDate() == null || request.departureDate().to() == null
                ? from.plusYears(1) : request.departureDate().to();
        return teamMapper.selectList(new QueryWrapper<SalesTeamEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("status", "normal")
                        .in("product_id", ids)
                        .ge("departure_date", from)
                        .le("departure_date", to)
                        .orderByAsc("departure_date")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.toMap(
                        SalesTeamEntity::getProductId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    private AgentProductApi.SearchItem toSearchItem(
            SalesProductEntity product,
            SalesTeamEntity team,
            int requestedSeats
    ) {
        AgentProductApi.NearestSchedule nearest = team == null ? null : new AgentProductApi.NearestSchedule(
                team.getId(), team.getTeamNo(), team.getDepartureDate(), team.getRemainingSeats(),
                availability(team.getRemainingSeats(), requestedSeats), "manual_quote"
        );
        LinkedHashSet<String> destinations = new LinkedHashSet<>();
        if (StringUtils.hasText(product.getCity())) destinations.add(product.getCity());
        if (StringUtils.hasText(product.getDistrict())) destinations.add(product.getDistrict());
        return new AgentProductApi.SearchItem(
                product.getId(), productCode(product.getId()), product.getProductName(), product.getBusinessType(),
                product.getProductTheme(), product.getReceptionStandard(), product.getTravelDays(),
                product.getTripType(), List.copyOf(destinations), product.getCity(), product.getCloseDaysBefore(),
                "authorized", nearest, product.getUpdatedAt()
        );
    }

    private AgentProductApi.ItineraryDay toItineraryDay(SalesProductItineraryDayResponse day) {
        return new AgentProductApi.ItineraryDay(
                day.dayNo(), day.dayTitle(), day.itineraryContent(),
                new AgentProductApi.Accommodation(day.accommodationNote(), null),
                new AgentProductApi.Meals(
                        Boolean.TRUE.equals(day.breakfastIncluded()),
                        Boolean.TRUE.equals(day.lunchIncluded()),
                        Boolean.TRUE.equals(day.dinnerIncluded())
                ),
                new AgentProductApi.Route(
                        day.roadbookSummary(),
                        day.roadbookTotalDistanceMeters(),
                        day.roadbookTotalDurationSeconds()
                )
        );
    }

    private void applyDestinationFilter(QueryWrapper<SalesProductEntity> wrapper, List<String> destinations) {
        List<String> values = clean(destinations);
        if (values.isEmpty()) return;
        wrapper.and(nested -> {
            for (int index = 0; index < values.size(); index++) {
                String value = values.get(index);
                if (index > 0) nested.or();
                nested.like("city", value).or().like("district", value);
            }
        });
    }

    private void applyKeywordFilter(QueryWrapper<SalesProductEntity> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) return;
        List<String> tokens = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
        if (tokens.isEmpty()) return;
        wrapper.and(group -> {
            for (int index = 0; index < tokens.size(); index++) {
                String token = tokens.get(index);
                if (index > 0) group.or();
                group.nested(match -> match
                        .like("product_name", token)
                        .or().like("city", token)
                        .or().like("district", token)
                        .or().like("business_type", token)
                        .or().like("product_theme", token)
                        .or().like("reception_standard", token));
            }
        });
    }

    private void applyScheduleFilter(
            QueryWrapper<SalesProductEntity> wrapper,
            Long tenantId,
            AgentProductApi.SearchRequest request
    ) {
        boolean dateSpecified = request.departureDate() != null
                && (request.departureDate().from() != null || request.departureDate().to() != null);
        boolean onlyAvailable = Boolean.TRUE.equals(request.onlyAvailable());
        if (!dateSpecified && !onlyAvailable) return;
        LocalDate today = LocalDate.now();
        LocalDate from = request.departureDate() == null || request.departureDate().from() == null
                ? today : request.departureDate().from();
        LocalDate to = request.departureDate() == null || request.departureDate().to() == null
                ? from.plusYears(1) : request.departureDate().to();
        int seats = request.party() == null ? 0 : request.party().totalSeats();
        String baseSql = """
                EXISTS (
                  SELECT 1
                  FROM sales_teams agent_team
                  WHERE agent_team.tenant_id = {0}
                    AND agent_team.product_id = sales_products.id
                    AND agent_team.is_deleted = false
                    AND agent_team.departure_date >= {1}
                    AND agent_team.departure_date <= {2}
                """;
        if (onlyAvailable) {
            wrapper.apply(baseSql + """
                    AND agent_team.status = 'normal'
                    AND COALESCE(agent_team.remaining_seats, 0) > 0
                    AND ({3} = 0 OR COALESCE(agent_team.remaining_seats, 0) >= {3})
                    AND agent_team.departure_date
                        - (COALESCE(agent_team.close_days_before, 0) * INTERVAL '1 day') >= CURRENT_DATE
                  )
                    """, tenantId, from, to, seats);
        } else {
            wrapper.apply(baseSql + ")", tenantId, from, to);
        }
    }

    private void validateSearchRequest(AgentProductApi.SearchRequest request) {
        if (request == null || request.customerId() == null || request.customerId() <= 0) {
            throw AgentException.validation("产品搜索参数不合法", Map.of("customerId", "required"));
        }
        if (request.page() != null && request.page() < 1
                || request.pageSize() != null && (request.pageSize() < 1 || request.pageSize() > 50)) {
            throw AgentException.validation("产品搜索分页参数不合法", Map.of("pagination", "invalid"));
        }
        if (request.travelDays() != null
                && request.travelDays().min() != null
                && request.travelDays().max() != null
                && request.travelDays().max() < request.travelDays().min()) {
            throw AgentException.validation("行程天数范围不合法", Map.of("travelDays", "min must not exceed max"));
        }
        if (request.departureDate() != null
                && request.departureDate().from() != null
                && request.departureDate().to() != null
                && request.departureDate().to().isBefore(request.departureDate().from())) {
            throw AgentException.validation("发团日期范围不合法", Map.of("departureDate", "from must not exceed to"));
        }
    }

    private List<String> clean(Collection<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private String availability(Integer remainingSeats, int requestedSeats) {
        int remaining = remainingSeats == null ? 0 : remainingSeats;
        if (remaining <= 0) return "sold_out";
        if (requestedSeats > 0 && remaining < requestedSeats) return "insufficient";
        return "available";
    }

    private String productCode(Long productId) {
        return "PRODUCT-%06d".formatted(productId);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String amount(BigDecimal value) {
        return money(value).toPlainString();
    }

    private long version(OffsetDateTime updatedAt) {
        return updatedAt == null ? 0 : updatedAt.toInstant().toEpochMilli();
    }
}
