package com.mtravel.platform.agent.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Agent 产品、团期、余位和客户适用价格协议对象。 */
public final class AgentProductApi {

    private AgentProductApi() {
    }

    /** 人数结构，所有人数均按非负整数处理。 */
    @Schema(name = "AgentProductParty")
    public record Party(
            @Min(0) Integer adults,
            @Min(0) Integer children,
            @Min(0) Integer childrenNoBed,
            @Min(0) Integer seniors
    ) {
        /** 返回所有需要占用团期座位的人数。 */
        public int totalSeats() {
            return value(adults) + value(children) + value(childrenNoBed) + value(seniors);
        }

        private int value(Integer number) {
            return number == null ? 0 : number;
        }
    }

    /** 旅游天数范围。 */
    @Schema(name = "AgentProductTravelDaysRange")
    public record TravelDaysRange(@Min(1) Integer min, @Min(1) Integer max) { }

    /** 发团日期范围。 */
    @Schema(name = "AgentProductDateRange")
    public record DateRange(LocalDate from, LocalDate to) { }

    /** 产品聚合搜索请求。 */
    @Schema(name = "AgentProductSearchRequest")
    public record SearchRequest(
            @NotNull @Positive Long customerId,
            @Size(max = 200) String keyword,
            @Size(max = 20) List<@Size(max = 80) String> destinations,
            @Size(max = 20) List<@Size(max = 120) String> businessTypes,
            @Size(max = 20) List<@Size(max = 120) String> productThemes,
            @Size(max = 20) List<@Size(max = 120) String> receptionStandards,
            @Valid TravelDaysRange travelDays,
            @Valid DateRange departureDate,
            @Valid Party party,
            Boolean onlyAvailable,
            @Min(1) Integer page,
            @Min(1) @Max(50) Integer pageSize
    ) { }

    /** 搜索结果中的最近团期摘要。 */
    @Schema(name = "AgentProductNearestSchedule")
    public record NearestSchedule(
            Long scheduleId,
            String teamNo,
            LocalDate departureDate,
            Integer remainingSeats,
            String availabilityStatus,
            String priceStatus
    ) { }

    /** 产品搜索结果行。 */
    @Schema(name = "AgentProductSearchItem")
    public record SearchItem(
            Long productId,
            String productCode,
            String productName,
            String businessType,
            String productTheme,
            String receptionStandard,
            Integer travelDays,
            String tripType,
            List<String> destinations,
            String receivingCity,
            Integer closeDaysBefore,
            String authorizationState,
            NearestSchedule nearestSchedule,
            OffsetDateTime updatedAt
    ) { }

    /** 产品搜索分页响应。 */
    @Schema(name = "AgentProductSearchResult")
    public record SearchResult(
            List<SearchItem> items,
            long page,
            long pageSize,
            long total,
            OffsetDateTime asOf
    ) { }

    /** 产品接团地点。 */
    @Schema(name = "AgentProductReceivingLocation")
    public record ReceivingLocation(String province, String city, String district) { }

    /** 对外产品默认房差。 */
    @Schema(name = "AgentProductDefaultSingleRoomSupplement")
    public record DefaultSingleRoomSupplement(String amount, String currency, String status) { }

    /** 行程住宿公开描述。 */
    @Schema(name = "AgentProductAccommodation")
    public record Accommodation(String description, String hotelDisplayName) { }

    /** 行程餐食标识。 */
    @Schema(name = "AgentProductMeals")
    public record Meals(boolean breakfast, boolean lunch, boolean dinner) { }

    /** 行程路书摘要。 */
    @Schema(name = "AgentProductRoute")
    public record Route(String summary, Integer distanceMeters, Integer durationSeconds) { }

    /** 对外每日行程。 */
    @Schema(name = "AgentProductItineraryDay")
    public record ItineraryDay(
            Integer dayNo,
            String title,
            String content,
            Accommodation accommodation,
            Meals meals,
            Route route
    ) { }

    /** 对外产品详情白名单。 */
    @Schema(name = "AgentProductDetail")
    public record ProductDetail(
            Long productId,
            String productCode,
            String productName,
            String businessType,
            String domesticInternational,
            ReceivingLocation receivingLocation,
            String tripType,
            String receptionStandard,
            String productTheme,
            Integer travelDays,
            Integer closeDaysBefore,
            DefaultSingleRoomSupplement defaultSingleRoomSupplement,
            String bookingNotice,
            String productDescription,
            String feeIncluded,
            String feeExcluded,
            String childPolicy,
            String shoppingArrangement,
            String optionalItems,
            String giftItems,
            String attentionItems,
            String warmReminder,
            List<ItineraryDay> itineraryDays,
            String dataVersion,
            OffsetDateTime updatedAt
    ) { }

    /** 团期余位判断。 */
    @Schema(name = "AgentScheduleAvailability")
    public record Availability(
            String status,
            String statusLabel,
            Integer totalSeats,
            Integer remainingSeats,
            Integer requestedSeats,
            boolean canReceive,
            String reason
    ) { }

    /** 单个公开价格项目。 */
    @Schema(name = "AgentSchedulePriceItem")
    public record PriceItem(String amount, String unit, String status) { }

    /** 客户适用价格项目。 */
    @Schema(name = "AgentSchedulePriceItems")
    public record PriceItems(
            PriceItem adult,
            PriceItem child,
            PriceItem childNoBed,
            PriceItem senior,
            PriceItem singleRoomSupplement
    ) { }

    /** 必收附加费用。 */
    @Schema(name = "AgentScheduleExtraFee")
    public record ExtraFee(
            String name,
            String amount,
            String unit,
            boolean required,
            String description
    ) { }

    /** 本次价格计算对应人数。 */
    @Schema(name = "AgentScheduleCalculatedFor")
    public record CalculatedFor(
            int adults,
            int children,
            int childrenNoBed,
            int seniors,
            int singleRooms
    ) { }

    /** 团期客户适用价。 */
    @Schema(name = "AgentSchedulePrice")
    public record SchedulePrice(
            String status,
            String statusLabel,
            String currency,
            Boolean taxIncluded,
            Long customerCategoryId,
            String customerCategoryName,
            PriceItems items,
            List<ExtraFee> extraFees,
            CalculatedFor calculatedFor,
            String calculatedTotal,
            boolean calculationComplete,
            OffsetDateTime validAt
    ) { }

    /** 团期响应行。 */
    @Schema(name = "AgentScheduleItem")
    public record ScheduleItem(
            Long scheduleId,
            String teamNo,
            LocalDate departureDate,
            LocalDate endDate,
            String salesStatus,
            String salesStatusLabel,
            OffsetDateTime bookingDeadline,
            Availability availability,
            SchedulePrice price,
            OffsetDateTime updatedAt
    ) { }

    /** 产品团期分页响应。 */
    @Schema(name = "AgentScheduleResult")
    public record ScheduleResult(
            Long productId,
            String productName,
            LocalDate earliestAvailableDate,
            List<ScheduleItem> items,
            long page,
            long pageSize,
            long total,
            OffsetDateTime asOf
    ) { }
}
