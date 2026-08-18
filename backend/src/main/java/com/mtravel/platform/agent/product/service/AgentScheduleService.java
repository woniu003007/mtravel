package com.mtravel.platform.agent.product.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerCapability;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamPriceEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamPriceMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Agent 实时团期、余位和客户适用价格服务。 */
@Service
public class AgentScheduleService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final AgentCustomerService customerService;
    private final AgentProductService productService;
    private final SalesTeamMapper teamMapper;
    private final SalesTeamPriceMapper priceMapper;

    public AgentScheduleService(
            AgentCustomerService customerService,
            AgentProductService productService,
            SalesTeamMapper teamMapper,
            SalesTeamPriceMapper priceMapper
    ) {
        this.customerService = customerService;
        this.productService = productService;
        this.teamMapper = teamMapper;
        this.priceMapper = priceMapper;
    }

    /** 查询指定产品日期范围内的实时团期和后端权威价格状态。 */
    public AgentProductApi.ScheduleResult schedules(
            Long tenantId,
            Long customerId,
            Long productId,
            LocalDate from,
            LocalDate to,
            AgentProductApi.Party party,
            Integer singleRooms,
            int pageNo,
            int pageSize
    ) {
        if (from == null || to == null || to.isBefore(from)) {
            throw AgentException.validation("团期日期范围不合法", Map.of(
                    "from", String.valueOf(from),
                    "to", String.valueOf(to)
            ));
        }
        if (pageNo < 1 || pageSize < 1 || pageSize > 50) {
            throw AgentException.validation("团期分页参数不合法", Map.of(
                    "page", String.valueOf(pageNo),
                    "pageSize", String.valueOf(pageSize)
            ));
        }
        AgentCustomerAccess access = customerService.requireCapability(
                tenantId, customerId, AgentCustomerCapability.QUERY_PRODUCTS
        );
        SalesProductEntity product = productService.requireProductEntity(tenantId, customerId, productId);
        Page<SalesTeamEntity> page = teamMapper.selectPage(
                new Page<>(pageNo, Math.min(pageSize, 50)),
                new QueryWrapper<SalesTeamEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("product_id", productId)
                        .eq("is_deleted", false)
                        .ge("departure_date", from)
                        .le("departure_date", to)
                        .orderByAsc("departure_date")
                        .orderByAsc("id")
        );
        List<Long> teamIds = page.getRecords().stream().map(SalesTeamEntity::getId).toList();
        List<SalesTeamPriceEntity> prices = teamIds.isEmpty() ? List.of()
                : priceMapper.selectList(new QueryWrapper<SalesTeamPriceEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("status", "active")
                        .in("team_id", teamIds));
        Map<Long, List<SalesTeamPriceEntity>> pricesByTeam = prices.stream()
                .collect(Collectors.groupingBy(SalesTeamPriceEntity::getTeamId));
        AgentProductApi.Party normalizedParty = party == null ? new AgentProductApi.Party(0, 0, 0, 0) : party;
        int normalizedSingleRooms = singleRooms == null ? 0 : Math.max(singleRooms, 0);
        OffsetDateTime asOf = OffsetDateTime.now(BUSINESS_ZONE);
        List<AgentProductApi.ScheduleItem> items = page.getRecords().stream()
                .map(team -> toScheduleItem(
                        product,
                        team,
                        pricesByTeam.getOrDefault(team.getId(), List.of()),
                        access,
                        normalizedParty,
                        normalizedSingleRooms,
                        asOf
                ))
                .toList();
        LocalDate earliest = items.stream()
                .filter(item -> "available".equals(item.availability().status()))
                .map(AgentProductApi.ScheduleItem::departureDate)
                .findFirst().orElse(null);
        return new AgentProductApi.ScheduleResult(
                productId, product.getProductName(), earliest, items,
                pageNo, Math.min(pageSize, 50), page.getTotal(), asOf
        );
    }

    private AgentProductApi.ScheduleItem toScheduleItem(
            SalesProductEntity product,
            SalesTeamEntity team,
            List<SalesTeamPriceEntity> priceRows,
            AgentCustomerAccess access,
            AgentProductApi.Party party,
            int singleRooms,
            OffsetDateTime asOf
    ) {
        int travelDays = product.getTravelDays() == null ? 1 : product.getTravelDays();
        LocalDate endDate = team.getDepartureDate().plusDays(Math.max(travelDays - 1L, 0));
        LocalDate cutoffDate = team.getDepartureDate().minusDays(value(team.getCloseDaysBefore()));
        OffsetDateTime deadline = OffsetDateTime.of(cutoffDate, LocalTime.MAX, ZoneOffset.ofHours(8));
        AgentProductApi.Availability availability = availability(team, party.totalSeats(), cutoffDate);
        SalesTeamPriceEntity selectedPrice = selectPrice(priceRows, access.categoryId());
        AgentProductApi.SchedulePrice price = buildPrice(
                selectedPrice, team, access, party, singleRooms, asOf
        );
        return new AgentProductApi.ScheduleItem(
                team.getId(), team.getTeamNo(), team.getDepartureDate(), endDate,
                salesStatus(team), salesStatusLabel(team), deadline, availability, price,
                team.getUpdatedAt()
        );
    }

    private AgentProductApi.Availability availability(SalesTeamEntity team, int requestedSeats, LocalDate cutoffDate) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        int remaining = value(team.getRemainingSeats());
        String status;
        String label;
        String reason = null;
        if ("cancelled".equals(team.getStatus())) {
            status = "cancelled"; label = "团期已取消"; reason = "团队已取消";
        } else if ("stopped".equals(team.getStatus())) {
            status = "sales_stopped"; label = "已停售"; reason = "团队已停止收客";
        } else if (team.getDepartureDate().isBefore(today)) {
            status = "departed"; label = "已发团"; reason = "发团日期已过";
        } else if (today.isAfter(cutoffDate)) {
            status = "cutoff_passed"; label = "已过截止收客时间"; reason = "已超过截止收客日期";
        } else if (remaining <= 0) {
            status = "sold_out"; label = "已售罄"; reason = "当前没有余位";
        } else if (requestedSeats > 0 && remaining < requestedSeats) {
            status = "insufficient"; label = "余位不足"; reason = "余位少于询问人数";
        } else {
            status = "available"; label = "余位充足";
        }
        return new AgentProductApi.Availability(
                status, label, value(team.getTotalSeats()), remaining, requestedSeats,
                "available".equals(status), reason
        );
    }

    private AgentProductApi.SchedulePrice buildPrice(
            SalesTeamPriceEntity row,
            SalesTeamEntity team,
            AgentCustomerAccess access,
            AgentProductApi.Party party,
            int singleRooms,
            OffsetDateTime asOf
    ) {
        AgentProductApi.CalculatedFor calculatedFor = new AgentProductApi.CalculatedFor(
                value(party.adults()), value(party.children()), value(party.childrenNoBed()),
                value(party.seniors()), singleRooms
        );
        AgentProductApi.PriceItems items = new AgentProductApi.PriceItems(
                priceItem(row == null ? null : row.getAdultPrice(), "person"),
                priceItem(row == null ? null : row.getChildPrice(), "person"),
                priceItem(row == null ? null : row.getChildNoBedPrice(), "person"),
                priceItem(row == null ? null : row.getSeniorPrice(), "person"),
                priceItem(team.getSingleRoomDifference(), "person_per_trip")
        );
        List<AgentProductApi.ExtraFee> extraFees = row != null && positive(row.getExtraFee())
                ? List.of(new AgentProductApi.ExtraFee(
                        "附加费用", amount(row.getExtraFee()), "person", true, "该团期客户类型必收附加费用"
                )) : List.of();
        boolean complete = access.publicContext().canQueryPrices()
                && row != null
                && party.totalSeats() > 0
                && access.defaultTaxIncluded() != null
                && requiredPriceAvailable(party.adults(), row.getAdultPrice())
                && requiredPriceAvailable(party.children(), row.getChildPrice())
                && requiredPriceAvailable(party.childrenNoBed(), row.getChildNoBedPrice())
                && requiredPriceAvailable(party.seniors(), row.getSeniorPrice())
                && (singleRooms == 0 || positive(team.getSingleRoomDifference()));
        BigDecimal total = complete ? calculateTotal(row, team, party, singleRooms) : null;
        String status = complete ? "confirmed" : "manual_quote";
        return new AgentProductApi.SchedulePrice(
                status,
                complete ? "客户适用价已确认" : "价格待人工确认",
                "CNY",
                complete ? access.defaultTaxIncluded() : null,
                row == null ? access.categoryId() : row.getCustomerCategoryId(),
                row == null ? access.publicContext().customerCategory().name() : row.getCustomerCategoryName(),
                items,
                extraFees,
                calculatedFor,
                total == null ? null : amount(total),
                complete,
                asOf
        );
    }

    private BigDecimal calculateTotal(
            SalesTeamPriceEntity row,
            SalesTeamEntity team,
            AgentProductApi.Party party,
            int singleRooms
    ) {
        BigDecimal total = multiply(row.getAdultPrice(), party.adults())
                .add(multiply(row.getChildPrice(), party.children()))
                .add(multiply(row.getChildNoBedPrice(), party.childrenNoBed()))
                .add(multiply(row.getSeniorPrice(), party.seniors()))
                .add(multiply(team.getSingleRoomDifference(), singleRooms));
        if (positive(row.getExtraFee())) {
            total = total.add(multiply(row.getExtraFee(), party.totalSeats()));
        }
        return money(total);
    }

    private SalesTeamPriceEntity selectPrice(List<SalesTeamPriceEntity> rows, Long categoryId) {
        return rows.stream().filter(row -> categoryId != null && categoryId.equals(row.getCustomerCategoryId()))
                .findFirst()
                .orElseGet(() -> rows.stream().filter(row -> row.getCustomerCategoryId() == null).findFirst().orElse(null));
    }

    private AgentProductApi.PriceItem priceItem(BigDecimal value, String unit) {
        return positive(value)
                ? new AgentProductApi.PriceItem(amount(value), unit, "confirmed")
                : null;
    }

    private boolean requiredPriceAvailable(Integer count, BigDecimal price) {
        return value(count) == 0 || positive(price);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private BigDecimal multiply(BigDecimal amount, Integer quantity) {
        return money(amount).multiply(BigDecimal.valueOf(value(quantity)));
    }

    private int value(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String amount(BigDecimal value) {
        return money(value).toPlainString();
    }

    private String salesStatus(SalesTeamEntity team) {
        return switch (team.getStatus()) {
            case "cancelled" -> "cancelled";
            case "stopped" -> "sales_stopped";
            default -> "selling";
        };
    }

    private String salesStatusLabel(SalesTeamEntity team) {
        return switch (team.getStatus()) {
            case "cancelled" -> "已取消";
            case "stopped" -> "已停售";
            default -> "正常收客";
        };
    }
}
