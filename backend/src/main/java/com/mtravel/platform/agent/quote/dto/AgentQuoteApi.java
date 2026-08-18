package com.mtravel.platform.agent.quote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Agent 询价任务创建和客户可见结果协议对象。 */
public final class AgentQuoteApi {

    private AgentQuoteApi() {
    }

    /** 创建单一类型询价任务的请求。 */
    @JsonIgnoreProperties(ignoreUnknown = false)
    @Schema(name = "AgentQuoteCreateRequest", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record CreateRequest(
            @NotBlank @Size(max = 100) String conversationId,
            @NotNull @Positive Long customerId,
            @NotBlank @Schema(allowableValues = {
                    "hotel_extra_stay", "hotel_change", "vehicle", "custom_route",
                    "extra_attraction", "special_meal", "other"
            }) String quoteType,
            @Positive Long relatedProductId,
            @Positive Long relatedScheduleId,
            @NotBlank @Size(max = 2000) String sourceMessage,
            @NotNull @Schema(
                    description = "按 quoteType 使用对应严格 Schema，禁止额外字段",
                    discriminatorProperty = "quoteType",
                    oneOf = {
                            HotelRequirements.class,
                            VehicleRequirements.class,
                            CustomRouteRequirements.class,
                            ExtraAttractionRequirements.class,
                            SpecialMealRequirements.class,
                            OtherRequirements.class
                    }
            ) JsonNode requirements
    ) { }

    /** 酒店提前入住或变更询价需求。 */
    @Schema(name = "AgentQuoteHotelRequirements", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record HotelRequirements(
            String city,
            String area,
            LocalDate checkIn,
            LocalDate checkOut,
            String roomType,
            Integer roomCount,
            Integer guestCount,
            String starStandard,
            Boolean breakfastRequired,
            Money budgetPerRoom,
            String notes
    ) { }

    /** 车辆询价需求。 */
    @Schema(name = "AgentQuoteVehicleRequirements", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record VehicleRequirements(
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            String pickupPlace,
            String dropoffPlace,
            Integer passengerCount,
            String vehicleType,
            Integer vehicleCount,
            Money budgetTotal,
            String notes
    ) { }

    /** 定制线路询价需求。 */
    @Schema(name = "AgentQuoteCustomRouteRequirements", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record CustomRouteRequirements(
            LocalDate startDate,
            Integer travelDays,
            List<String> cities,
            Party party,
            String notes,
            Money budgetTotal
    ) { }

    /** 加景点询价需求。 */
    @Schema(name = "AgentQuoteExtraAttractionRequirements", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record ExtraAttractionRequirements(
            LocalDate visitDate,
            String attractionName,
            Integer participantCount,
            String notes
    ) { }

    /** 特殊用餐询价需求。 */
    @Schema(name = "AgentQuoteSpecialMealRequirements", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record SpecialMealRequirements(
            LocalDate mealDate,
            String city,
            String mealType,
            Integer participantCount,
            String dietaryRequirements,
            String notes
    ) { }

    /** 其它非标需求。 */
    @Schema(name = "AgentQuoteOtherRequirements", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record OtherRequirements(String notes) { }

    /** 定制团人数结构。 */
    @Schema(name = "AgentQuoteParty", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record Party(Integer adults, Integer children, Integer childrenNoBed, Integer seniors) { }

    /** 严格十进制预算结构。 */
    @Schema(name = "AgentQuoteMoney", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record Money(String amount, String currency) { }

    /** 任务分配结果，不包含员工联系方式或权限。 */
    @Schema(name = "AgentQuoteAssignedTo")
    public record AssignedTo(Long employeeId, String employeeName, String departmentName) { }

    /** 询价创建成功结果。 */
    @Schema(name = "AgentQuoteCreateResult")
    public record CreateResult(
            String quoteRequestId,
            String status,
            String statusLabel,
            AssignedTo assignedTo,
            OffsetDateTime createdAt
    ) { }

    /** 经人工审核且允许对客显示的报价内容。 */
    @Schema(name = "AgentQuotePublicResult")
    public record PublicQuote(
            String replyText,
            String totalAmount,
            String currency,
            OffsetDateTime validUntil,
            OffsetDateTime approvedAt
    ) { }

    /** 询价任务的客户安全查询结果。 */
    @Schema(name = "AgentQuoteDetailResult")
    public record DetailResult(
            String quoteRequestId,
            String status,
            String statusLabel,
            boolean customerVisible,
            PublicQuote quote,
            AssignedTo assignedTo,
            OffsetDateTime updatedAt
    ) { }
}
