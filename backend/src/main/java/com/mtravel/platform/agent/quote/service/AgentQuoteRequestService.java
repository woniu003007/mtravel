package com.mtravel.platform.agent.quote.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.common.AgentIdempotencySupport;
import com.mtravel.platform.agent.common.AgentInputSanitizer;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerCapability;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.agent.quote.dto.AgentQuoteApi;
import com.mtravel.platform.agent.quote.entity.AgentQuoteRequestEntity;
import com.mtravel.platform.agent.quote.enums.AgentQuoteStatus;
import com.mtravel.platform.agent.quote.enums.AgentQuoteType;
import com.mtravel.platform.agent.quote.mapper.AgentQuoteRequestMapper;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Agent 非标准需求询价任务创建、幂等和客户可见结果服务。 */
@Service
public class AgentQuoteRequestService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> HOTEL_FIELDS = Set.of(
            "city", "area", "checkIn", "checkOut", "roomType", "roomCount", "guestCount",
            "starStandard", "breakfastRequired", "budgetPerRoom", "notes"
    );
    private static final Set<String> VEHICLE_FIELDS = Set.of(
            "serviceStartDate", "serviceEndDate", "pickupPlace", "dropoffPlace", "passengerCount",
            "vehicleType", "vehicleCount", "budgetTotal", "notes"
    );
    private static final Set<String> CUSTOM_ROUTE_FIELDS = Set.of(
            "startDate", "travelDays", "cities", "party", "notes", "budgetTotal"
    );
    private static final Set<String> EXTRA_ATTRACTION_FIELDS = Set.of(
            "visitDate", "attractionName", "participantCount", "notes"
    );
    private static final Set<String> SPECIAL_MEAL_FIELDS = Set.of(
            "mealDate", "city", "mealType", "participantCount", "dietaryRequirements", "notes"
    );

    private final AgentQuoteRequestMapper quoteMapper;
    private final AgentCustomerService customerService;
    private final AgentProductService productService;
    private final SalesTeamMapper teamMapper;
    private final ObjectMapper objectMapper;

    public AgentQuoteRequestService(
            AgentQuoteRequestMapper quoteMapper,
            AgentCustomerService customerService,
            AgentProductService productService,
            SalesTeamMapper teamMapper,
            ObjectMapper objectMapper
    ) {
        this.quoteMapper = quoteMapper;
        this.customerService = customerService;
        this.productService = productService;
        this.teamMapper = teamMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 原子创建询价任务。相同幂等键和请求返回原任务，不同请求返回冲突。
     * 本方法只写 Agent 任务表，不会占位、下单或修改价格。
     */
    @Transactional
    public AgentQuoteApi.CreateResult create(
            AgentServicePrincipal caller,
            String idempotencyKey,
            AgentQuoteApi.CreateRequest request
    ) {
        String key = AgentIdempotencySupport.requireKey(idempotencyKey);
        NormalizedRequest normalized = normalizeAndValidate(request);
        String requestHash = requestHash(normalized);
        AgentQuoteRequestEntity existing = findByIdempotency(caller.tenantId(), caller.tokenId(), key);
        if (existing != null) return replay(existing, requestHash);

        AgentCustomerAccess customer = customerService.requireCapability(
                caller.tenantId(), normalized.customerId(), AgentCustomerCapability.CREATE_QUOTE_REQUEST
        );
        RelatedContext related = validateRelated(caller.tenantId(), normalized);

        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        Assignee assignee = route(customer, related.schedule());
        AgentQuoteRequestEntity entity = new AgentQuoteRequestEntity();
        entity.setTenantId(caller.tenantId());
        entity.setRequestNo(newRequestNo(now));
        entity.setServiceTokenId(caller.tokenId());
        entity.setIdempotencyKey(key);
        entity.setRequestHash(requestHash);
        entity.setConversationId(normalized.conversationId());
        entity.setCustomerId(normalized.customerId());
        entity.setQuoteType(normalized.quoteType().value());
        entity.setSourceMessage(normalized.sourceMessage());
        entity.setRequirementsJson(AgentIdempotencySupport.canonicalJson(objectMapper, normalized.requirements()));
        entity.setRelatedProductId(normalized.relatedProductId());
        entity.setRelatedScheduleId(normalized.relatedScheduleId());
        entity.setAssignedEmployeeId(assignee.employeeId());
        entity.setAssignedEmployeeName(assignee.employeeName());
        entity.setAssignedDepartmentName(assignee.departmentName());
        entity.setStatus(AgentQuoteStatus.PENDING.value());
        entity.setCustomerVisible(false);
        entity.setCurrency("CNY");
        entity.setCreatedBy("customer_service_agent");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setIsDeleted(false);

        int inserted = quoteMapper.insertIdempotent(entity);
        if (inserted == 0) {
            AgentQuoteRequestEntity concurrent = findByIdempotency(caller.tenantId(), caller.tokenId(), key);
            if (concurrent == null) {
                throw AgentException.serviceUnavailable("询价任务幂等写入结果暂时不可用");
            }
            return replay(concurrent, requestHash);
        }
        return toCreateResult(entity);
    }

    /** 按租户和客户边界查询询价结果，仅返回已审核的客户可见报价。 */
    public AgentQuoteApi.DetailResult detail(Long tenantId, Long customerId, String quoteRequestId) {
        customerService.accessContext(tenantId, customerId);
        AgentQuoteRequestEntity entity = quoteMapper.selectOne(new QueryWrapper<AgentQuoteRequestEntity>()
                .eq("tenant_id", tenantId)
                .eq("customer_id", customerId)
                .eq("request_no", quoteRequestId)
                .eq("is_deleted", false)
                .last("LIMIT 1"));
        if (entity == null) throw AgentException.resourceNotFound();
        AgentQuoteStatus status = AgentQuoteStatus.fromStoredValue(entity.getStatus());
        boolean visible = Boolean.TRUE.equals(entity.getCustomerVisible());
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        boolean unexpired = entity.getValidUntil() == null || entity.getValidUntil().isAfter(now);
        AgentQuoteApi.PublicQuote quote = status == AgentQuoteStatus.QUOTED
                && visible
                && unexpired
                && StringUtils.hasText(entity.getReplyText())
                && entity.getTotalAmount() != null
                ? new AgentQuoteApi.PublicQuote(
                        entity.getReplyText(), money(entity.getTotalAmount()), value(entity.getCurrency(), "CNY"),
                        entity.getValidUntil(), entity.getApprovedAt()
                )
                : null;
        return new AgentQuoteApi.DetailResult(
                entity.getRequestNo(), status.value(), status.label(), visible && quote != null, quote,
                new AgentQuoteApi.AssignedTo(
                        null, entity.getAssignedEmployeeName(), entity.getAssignedDepartmentName()
                ),
                entity.getUpdatedAt()
        );
    }

    private NormalizedRequest normalizeAndValidate(AgentQuoteApi.CreateRequest request) {
        if (request == null || request.customerId() == null || request.customerId() <= 0) {
            throw validation("customerId", "must be a positive integer");
        }
        String conversationId = AgentInputSanitizer.requiredText(
                "conversationId", request.conversationId(), 1, 100
        );
        String sourceMessage = AgentInputSanitizer.requiredText(
                "sourceMessage", request.sourceMessage(), 1, 2000
        );
        AgentQuoteType type = AgentQuoteType.fromValue(request.quoteType());
        if (type == null) throw validation("quoteType", "unsupported");
        if (request.relatedProductId() != null && request.relatedProductId() <= 0) {
            throw validation("relatedProductId", "must be positive");
        }
        if (request.relatedScheduleId() != null && request.relatedScheduleId() <= 0) {
            throw validation("relatedScheduleId", "must be positive");
        }
        if (request.relatedScheduleId() != null && request.relatedProductId() == null) {
            throw validation("relatedProductId", "required when relatedScheduleId is present");
        }
        JsonNode requirements = AgentInputSanitizer.normalizeJsonStrings(request.requirements(), "requirements");
        validateRequirements(type, requirements);
        return new NormalizedRequest(
                conversationId, request.customerId(), type, request.relatedProductId(), request.relatedScheduleId(),
                sourceMessage, requirements
        );
    }

    private void validateRequirements(AgentQuoteType type, JsonNode requirements) {
        requireObject(requirements, "requirements");
        switch (type) {
            case HOTEL_EXTRA_STAY, HOTEL_CHANGE -> validateHotel(requirements);
            case VEHICLE -> validateVehicle(requirements);
            case CUSTOM_ROUTE -> validateCustomRoute(requirements);
            case EXTRA_ATTRACTION -> validateExtraAttraction(requirements);
            case SPECIAL_MEAL -> validateSpecialMeal(requirements);
            case OTHER -> {
                assertAllowed(requirements, Set.of("notes"));
                requireText(requirements, "notes", 1, 2000);
            }
        }
    }

    private void validateHotel(JsonNode node) {
        assertAllowed(node, HOTEL_FIELDS);
        requireText(node, "city", 1, 100);
        requireText(node, "roomType", 1, 100);
        LocalDate checkIn = requireDate(node, "checkIn");
        LocalDate checkOut = requireDate(node, "checkOut");
        if (!checkOut.isAfter(checkIn) || ChronoUnit.DAYS.between(checkIn, checkOut) > 30) {
            throw validation("requirements.checkOut", "must be after checkIn and within 30 nights");
        }
        requireInteger(node, "roomCount", 1, 200);
        requireInteger(node, "guestCount", 1, 1000);
        optionalText(node, "area", 200);
        optionalText(node, "starStandard", 100);
        optionalText(node, "notes", 2000);
        optionalBoolean(node, "breakfastRequired");
        optionalMoney(node, "budgetPerRoom");
    }

    private void validateVehicle(JsonNode node) {
        assertAllowed(node, VEHICLE_FIELDS);
        LocalDate start = requireDate(node, "serviceStartDate");
        LocalDate end = requireDate(node, "serviceEndDate");
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 30) {
            throw validation("requirements.serviceEndDate", "must not precede start and must be within 30 days");
        }
        requireText(node, "pickupPlace", 1, 200);
        requireText(node, "dropoffPlace", 1, 200);
        requireText(node, "vehicleType", 1, 100);
        requireInteger(node, "passengerCount", 1, 1000);
        requireInteger(node, "vehicleCount", 1, 50);
        optionalText(node, "notes", 2000);
        optionalMoney(node, "budgetTotal");
    }

    private void validateCustomRoute(JsonNode node) {
        assertAllowed(node, CUSTOM_ROUTE_FIELDS);
        requireDate(node, "startDate");
        requireInteger(node, "travelDays", 1, 30);
        JsonNode cities = node.get("cities");
        if (cities == null || !cities.isArray() || cities.isEmpty() || cities.size() > 20) {
            throw validation("requirements.cities", "must contain 1 to 20 cities");
        }
        for (int index = 0; index < cities.size(); index++) {
            textValue(cities.get(index), "requirements.cities[" + index + "]", 1, 100);
        }
        JsonNode party = node.get("party");
        requireObject(party, "requirements.party");
        assertAllowed(party, Set.of("adults", "children", "childrenNoBed", "seniors"));
        int total = optionalInteger(party, "adults", 0, 1000)
                + optionalInteger(party, "children", 0, 1000)
                + optionalInteger(party, "childrenNoBed", 0, 1000)
                + optionalInteger(party, "seniors", 0, 1000);
        if (total < 1 || total > 1000) throw validation("requirements.party", "total must be between 1 and 1000");
        requireText(node, "notes", 1, 2000);
        optionalMoney(node, "budgetTotal");
    }

    private void validateExtraAttraction(JsonNode node) {
        assertAllowed(node, EXTRA_ATTRACTION_FIELDS);
        requireDate(node, "visitDate");
        requireText(node, "attractionName", 1, 200);
        requireInteger(node, "participantCount", 1, 1000);
        optionalText(node, "notes", 2000);
    }

    private void validateSpecialMeal(JsonNode node) {
        assertAllowed(node, SPECIAL_MEAL_FIELDS);
        requireDate(node, "mealDate");
        requireText(node, "city", 1, 100);
        requireText(node, "mealType", 1, 100);
        requireInteger(node, "participantCount", 1, 1000);
        optionalText(node, "dietaryRequirements", 1000);
        optionalText(node, "notes", 2000);
    }

    private RelatedContext validateRelated(Long tenantId, NormalizedRequest request) {
        if (request.relatedProductId() != null) {
            productService.requireProductEntity(tenantId, request.customerId(), request.relatedProductId());
        }
        if (request.relatedScheduleId() == null) return new RelatedContext(null);
        SalesTeamEntity schedule = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", request.relatedScheduleId())
                .eq("product_id", request.relatedProductId())
                .eq("is_deleted", false)
                .last("LIMIT 1"));
        if (schedule == null) throw AgentException.resourceNotFound();
        return new RelatedContext(schedule);
    }

    private Assignee route(AgentCustomerAccess customer, SalesTeamEntity schedule) {
        if (schedule != null && (schedule.getOperatorEmployeeId() != null
                || StringUtils.hasText(schedule.getOperatorEmployeeName()))) {
            return new Assignee(
                    schedule.getOperatorEmployeeId(), schedule.getOperatorEmployeeName(), schedule.getDepartmentName()
            );
        }
        var context = customer.publicContext();
        if (context.dispatcher() != null && (context.dispatcher().id() != null
                || StringUtils.hasText(context.dispatcher().name()))) {
            return new Assignee(
                    context.dispatcher().id(), context.dispatcher().name(),
                    context.serviceDepartment() == null ? null : context.serviceDepartment().name()
            );
        }
        if (context.serviceDepartment() != null && StringUtils.hasText(context.serviceDepartment().name())) {
            return new Assignee(null, "部门公共队列", context.serviceDepartment().name());
        }
        return new Assignee(null, "客服公共待处理队列", "客服中心");
    }

    private AgentQuoteRequestEntity findByIdempotency(Long tenantId, Long tokenId, String key) {
        return quoteMapper.selectOne(new QueryWrapper<AgentQuoteRequestEntity>()
                .eq("tenant_id", tenantId)
                .eq("service_token_id", tokenId)
                .eq("idempotency_key", key)
                .eq("is_deleted", false)
                .last("LIMIT 1"));
    }

    private AgentQuoteApi.CreateResult replay(AgentQuoteRequestEntity entity, String requestHash) {
        if (!requestHash.equals(entity.getRequestHash())) throw AgentException.idempotencyConflict();
        return toCreateResult(entity);
    }

    private AgentQuoteApi.CreateResult toCreateResult(AgentQuoteRequestEntity entity) {
        AgentQuoteStatus status = AgentQuoteStatus.PENDING;
        return new AgentQuoteApi.CreateResult(
                entity.getRequestNo(), status.value(), status.label(),
                new AgentQuoteApi.AssignedTo(
                        entity.getAssignedEmployeeId(), entity.getAssignedEmployeeName(),
                        entity.getAssignedDepartmentName()
                ),
                entity.getCreatedAt()
        );
    }

    private String requestHash(NormalizedRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("method", "POST");
        payload.put("path", "/agent/v1/quote-requests");
        payload.set("body", objectMapper.valueToTree(request));
        return AgentIdempotencySupport.hash(objectMapper, payload);
    }

    private String newRequestNo(OffsetDateTime now) {
        return "QR-%s-%s".formatted(
                now.toLocalDate().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase()
        );
    }

    private void assertAllowed(JsonNode object, Set<String> allowed) {
        Set<String> unknown = new HashSet<>();
        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) unknown.add(field);
        }
        if (!unknown.isEmpty()) throw validation("requirements", "unknown fields: " + unknown);
    }

    private void requireObject(JsonNode node, String field) {
        if (node == null || !node.isObject()) throw validation(field, "must be an object");
    }

    private String requireText(JsonNode object, String field, int min, int max) {
        return textValue(object.get(field), "requirements." + field, min, max);
    }

    private String optionalText(JsonNode object, String field, int max) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) return null;
        return textValue(value, "requirements." + field, 1, max);
    }

    private String textValue(JsonNode value, String field, int min, int max) {
        if (value == null || !value.isTextual()) throw validation(field, "must be text");
        return AgentInputSanitizer.requiredText(field, value.textValue(), min, max);
    }

    private LocalDate requireDate(JsonNode object, String field) {
        String value = requireText(object, field, 10, 10);
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.isBefore(LocalDate.now(BUSINESS_ZONE))) {
                throw validation("requirements." + field, "must not be in the past");
            }
            return date;
        } catch (DateTimeException exception) {
            throw validation("requirements." + field, "must use YYYY-MM-DD");
        }
    }

    private int requireInteger(JsonNode object, String field, int min, int max) {
        JsonNode value = object.get(field);
        if (value == null || !value.canConvertToInt() || !value.isIntegralNumber()) {
            throw validation("requirements." + field, "must be an integer");
        }
        int number = value.intValue();
        if (number < min || number > max) {
            throw validation("requirements." + field, "must be between " + min + " and " + max);
        }
        return number;
    }

    private int optionalInteger(JsonNode object, String field, int min, int max) {
        return object.has(field) ? requireInteger(object, field, min, max) : 0;
    }

    private void optionalBoolean(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value != null && !value.isNull() && !value.isBoolean()) {
            throw validation("requirements." + field, "must be boolean");
        }
    }

    private void optionalMoney(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) return;
        requireObject(value, "requirements." + field);
        assertAllowed(value, Set.of("amount", "currency"));
        String amount = textValue(value.get("amount"), "requirements." + field + ".amount", 1, 20);
        String currency = textValue(value.get("currency"), "requirements." + field + ".currency", 3, 3);
        try {
            BigDecimal decimal = new BigDecimal(amount);
            if (decimal.signum() < 0 || decimal.scale() > 2) throw new NumberFormatException();
        } catch (RuntimeException exception) {
            throw validation("requirements." + field + ".amount", "must be a non-negative decimal string");
        }
        if (!"CNY".equals(currency)) throw validation("requirements." + field + ".currency", "must be CNY");
    }

    private AgentException validation(String field, String reason) {
        return AgentException.validation("询价需求不符合业务 Schema", Map.of(field, reason));
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String value(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record NormalizedRequest(
            String conversationId,
            Long customerId,
            AgentQuoteType quoteType,
            Long relatedProductId,
            Long relatedScheduleId,
            String sourceMessage,
            JsonNode requirements
    ) { }

    private record RelatedContext(SalesTeamEntity schedule) { }

    private record Assignee(Long employeeId, String employeeName, String departmentName) { }
}
