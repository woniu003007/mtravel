package com.mtravel.platform.agent.handoff.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.common.AgentIdempotencySupport;
import com.mtravel.platform.agent.common.AgentInputSanitizer;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerCapability;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.handoff.dto.AgentHandoffApi;
import com.mtravel.platform.agent.handoff.entity.AgentHandoffEntity;
import com.mtravel.platform.agent.handoff.entity.AgentHandoffMessageEntity;
import com.mtravel.platform.agent.handoff.enums.AgentHandoffPriority;
import com.mtravel.platform.agent.handoff.enums.AgentHandoffReason;
import com.mtravel.platform.agent.handoff.enums.AgentHandoffStatus;
import com.mtravel.platform.agent.handoff.mapper.AgentHandoffMapper;
import com.mtravel.platform.agent.handoff.mapper.AgentHandoffMessageMapper;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.agent.quote.entity.AgentQuoteRequestEntity;
import com.mtravel.platform.agent.quote.mapper.AgentQuoteRequestMapper;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Agent 转人工待办的幂等创建、关联校验、消息保存和负责人路由服务。 */
@Service
public class AgentHandoffService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final AgentHandoffMapper handoffMapper;
    private final AgentHandoffMessageMapper messageMapper;
    private final AgentCustomerService customerService;
    private final AgentProductService productService;
    private final SalesTeamMapper teamMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final AgentQuoteRequestMapper quoteMapper;
    private final ObjectMapper objectMapper;

    public AgentHandoffService(
            AgentHandoffMapper handoffMapper,
            AgentHandoffMessageMapper messageMapper,
            AgentCustomerService customerService,
            AgentProductService productService,
            SalesTeamMapper teamMapper,
            SalesBookingOrderMapper orderMapper,
            AgentQuoteRequestMapper quoteMapper,
            ObjectMapper objectMapper
    ) {
        this.handoffMapper = handoffMapper;
        this.messageMapper = messageMapper;
        this.customerService = customerService;
        this.productService = productService;
        this.teamMapper = teamMapper;
        this.orderMapper = orderMapper;
        this.quoteMapper = quoteMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 原子创建转人工待办并批量保存聊天上下文。
     * 待办不代表消息已向客户发送，也不会修改关联业务对象。
     */
    @Transactional
    public AgentHandoffApi.CreateResult create(
            AgentServicePrincipal caller,
            String idempotencyKey,
            AgentHandoffApi.CreateRequest request
    ) {
        String key = AgentIdempotencySupport.requireKey(idempotencyKey);
        NormalizedRequest normalized = normalizeAndValidate(request);
        String requestHash = requestHash(normalized);
        AgentHandoffEntity existing = findByIdempotency(caller.tenantId(), caller.tokenId(), key);
        if (existing != null) return replay(existing, requestHash);

        AgentCustomerAccess customer = customerService.requireCapability(
                caller.tenantId(), normalized.customerId(), AgentCustomerCapability.CREATE_HANDOFF
        );
        RelatedContext related = validateRelated(caller.tenantId(), normalized);
        validateRaisedPriority(normalized, related);

        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        Assignee assignee = route(customer, related.routingTeam());
        AgentHandoffEntity entity = new AgentHandoffEntity();
        entity.setTenantId(caller.tenantId());
        entity.setHandoffNo(newHandoffNo(now));
        entity.setServiceTokenId(caller.tokenId());
        entity.setIdempotencyKey(key);
        entity.setRequestHash(requestHash);
        entity.setConversationId(normalized.conversationId());
        entity.setCustomerId(normalized.customerId());
        entity.setReasonCode(normalized.reason().value());
        entity.setPriority(normalized.priority().value());
        entity.setSummary(normalized.summary());
        entity.setRelatedProductId(normalized.related().productId());
        entity.setRelatedScheduleId(normalized.related().scheduleId());
        entity.setRelatedTeamNo(normalized.related().teamNo());
        entity.setRelatedQuoteRequestNo(normalized.related().quoteRequestId());
        entity.setAssignedEmployeeId(assignee.employeeId());
        entity.setAssignedEmployeeName(assignee.employeeName());
        entity.setAssignedDepartmentName(assignee.departmentName());
        entity.setStatus(AgentHandoffStatus.OPEN.value());
        entity.setCreatedBy("customer_service_agent");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setIsDeleted(false);

        Long insertedId = handoffMapper.insertIdempotentReturningId(entity);
        if (insertedId == null) {
            AgentHandoffEntity concurrent = findByIdempotency(caller.tenantId(), caller.tokenId(), key);
            if (concurrent == null) {
                throw AgentException.serviceUnavailable("转人工待办幂等写入结果暂时不可用");
            }
            return replay(concurrent, requestHash);
        }
        entity.setId(insertedId);
        List<AgentHandoffMessageEntity> messages = toMessageEntities(
                caller.tenantId(), insertedId, normalized.messages(), now
        );
        int insertedMessages = messageMapper.insertBatch(messages);
        if (insertedMessages != messages.size()) {
            throw AgentException.serviceUnavailable("转人工来源消息未完整保存");
        }
        return toCreateResult(entity);
    }

    private NormalizedRequest normalizeAndValidate(AgentHandoffApi.CreateRequest request) {
        if (request == null || request.customerId() == null || request.customerId() <= 0) {
            throw validation("customerId", "must be a positive integer");
        }
        String conversationId = AgentInputSanitizer.requiredText(
                "conversationId", request.conversationId(), 1, 100
        );
        AgentHandoffReason reason = AgentHandoffReason.fromValue(request.reasonCode());
        if (reason == null) throw validation("reasonCode", "unsupported");
        AgentHandoffPriority priority = AgentHandoffPriority.fromValue(request.priority());
        if (priority == null) throw validation("priority", "unsupported");
        String summary = AgentInputSanitizer.requiredText("summary", request.summary(), 1, 1000);
        List<NormalizedMessage> messages = normalizeMessages(request.sourceMessages());
        AgentHandoffApi.Related sourceRelated = request.related() == null
                ? new AgentHandoffApi.Related(null, null, null, null)
                : request.related();
        if (sourceRelated.productId() != null && sourceRelated.productId() <= 0) {
            throw validation("related.productId", "must be positive");
        }
        if (sourceRelated.scheduleId() != null && sourceRelated.scheduleId() <= 0) {
            throw validation("related.scheduleId", "must be positive");
        }
        if (sourceRelated.scheduleId() != null && sourceRelated.productId() == null) {
            throw validation("related.productId", "required when scheduleId is present");
        }
        NormalizedRelated related = new NormalizedRelated(
                sourceRelated.productId(),
                sourceRelated.scheduleId(),
                AgentInputSanitizer.optionalText("related.teamNo", sourceRelated.teamNo(), 80),
                AgentInputSanitizer.optionalText("related.quoteRequestId", sourceRelated.quoteRequestId(), 40)
        );
        return new NormalizedRequest(conversationId, request.customerId(), reason, priority, summary, messages, related);
    }

    private List<NormalizedMessage> normalizeMessages(List<AgentHandoffApi.SourceMessage> sourceMessages) {
        if (sourceMessages == null || sourceMessages.isEmpty() || sourceMessages.size() > 20) {
            throw validation("sourceMessages", "must contain 1 to 20 messages");
        }
        List<NormalizedMessage> result = new ArrayList<>(sourceMessages.size());
        Set<String> messageIds = new HashSet<>();
        int totalCharacters = 0;
        for (int index = 0; index < sourceMessages.size(); index++) {
            AgentHandoffApi.SourceMessage source = sourceMessages.get(index);
            if (source == null || source.sentAt() == null) {
                throw validation("sourceMessages[" + index + "].sentAt", "required");
            }
            String prefix = "sourceMessages[" + index + "]";
            String messageId = AgentInputSanitizer.requiredText(prefix + ".messageId", source.messageId(), 1, 100);
            if (!messageIds.add(messageId)) throw validation("sourceMessages.messageId", "must be unique");
            String senderName = AgentInputSanitizer.requiredText(prefix + ".senderName", source.senderName(), 1, 100);
            String content = AgentInputSanitizer.requiredText(prefix + ".content", source.content(), 1, 2000);
            totalCharacters += content.length();
            result.add(new NormalizedMessage(messageId, senderName, source.sentAt(), content));
        }
        if (totalCharacters > 20_000) {
            throw validation("sourceMessages", "total content length must not exceed 20000");
        }
        return List.copyOf(result);
    }

    private RelatedContext validateRelated(Long tenantId, NormalizedRequest request) {
        NormalizedRelated related = request.related();
        if (related.productId() != null) {
            productService.requireProductEntity(tenantId, request.customerId(), related.productId());
        }

        SalesTeamEntity schedule = null;
        if (related.scheduleId() != null) {
            schedule = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("id", related.scheduleId())
                    .eq("product_id", related.productId())
                    .eq("is_deleted", false)
                    .last("LIMIT 1"));
            if (schedule == null) throw AgentException.resourceNotFound();
        }

        SalesTeamEntity team = null;
        if (related.teamNo() != null) {
            team = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("team_no", related.teamNo())
                    .eq("is_deleted", false)
                    .last("LIMIT 1"));
            if (team == null) throw AgentException.resourceNotFound();
            if (schedule != null && !Objects.equals(schedule.getId(), team.getId())) {
                throw AgentException.resourceNotFound();
            }
            if (related.productId() != null && !Objects.equals(team.getProductId(), related.productId())) {
                throw AgentException.resourceNotFound();
            }
            requireTeamCustomerAccess(tenantId, request.customerId(), team);
        }

        AgentQuoteRequestEntity quote = null;
        if (related.quoteRequestId() != null) {
            quote = quoteMapper.selectOne(new QueryWrapper<AgentQuoteRequestEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("customer_id", request.customerId())
                    .eq("conversation_id", request.conversationId())
                    .eq("request_no", related.quoteRequestId())
                    .eq("is_deleted", false)
                    .last("LIMIT 1"));
            if (quote == null) throw AgentException.resourceNotFound();
            if (related.productId() != null
                    && quote.getRelatedProductId() != null
                    && !Objects.equals(related.productId(), quote.getRelatedProductId())) {
                throw AgentException.resourceNotFound();
            }
            if (related.scheduleId() != null
                    && quote.getRelatedScheduleId() != null
                    && !Objects.equals(related.scheduleId(), quote.getRelatedScheduleId())) {
                throw AgentException.resourceNotFound();
            }
        }
        return new RelatedContext(schedule, team, quote);
    }

    private void requireTeamCustomerAccess(Long tenantId, Long customerId, SalesTeamEntity team) {
        Long orderCount = orderMapper.selectCount(new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", team.getId())
                .eq("customer_id", customerId)
                .ne("status", "cancelled")
                .eq("is_deleted", false));
        if (orderCount != null && orderCount > 0) return;
        if (team.getProductId() == null) throw AgentException.resourceNotFound();
        try {
            productService.requireProductEntity(tenantId, customerId, team.getProductId());
        } catch (AgentException exception) {
            throw AgentException.resourceNotFound();
        }
    }

    private void validateRaisedPriority(NormalizedRequest request, RelatedContext related) {
        if (request.priority() == AgentHandoffPriority.LOW || request.priority() == AgentHandoffPriority.NORMAL) return;
        SalesTeamEntity routingTeam = related.routingTeam();
        boolean sameDayDeparture = routingTeam != null
                && LocalDate.now(BUSINESS_ZONE).equals(routingTeam.getDepartureDate());
        if (request.reason() != AgentHandoffReason.COMPLAINT && !sameDayDeparture) {
            throw validation("priority", "high or urgent requires complaint or same-day departure");
        }
    }

    private Assignee route(AgentCustomerAccess customer, SalesTeamEntity team) {
        if (team != null && (team.getOperatorEmployeeId() != null
                || StringUtils.hasText(team.getOperatorEmployeeName()))) {
            return new Assignee(team.getOperatorEmployeeId(), team.getOperatorEmployeeName(), team.getDepartmentName());
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

    private List<AgentHandoffMessageEntity> toMessageEntities(
            Long tenantId,
            Long handoffId,
            List<NormalizedMessage> sourceMessages,
            OffsetDateTime now
    ) {
        return sourceMessages.stream().map(source -> {
            AgentHandoffMessageEntity entity = new AgentHandoffMessageEntity();
            entity.setTenantId(tenantId);
            entity.setHandoffId(handoffId);
            entity.setMessageId(source.messageId());
            entity.setSenderName(source.senderName());
            entity.setSentAt(source.sentAt());
            entity.setContent(source.content());
            entity.setCreatedBy("customer_service_agent");
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setIsDeleted(false);
            return entity;
        }).toList();
    }

    private AgentHandoffEntity findByIdempotency(Long tenantId, Long tokenId, String key) {
        return handoffMapper.selectOne(new QueryWrapper<AgentHandoffEntity>()
                .eq("tenant_id", tenantId)
                .eq("service_token_id", tokenId)
                .eq("idempotency_key", key)
                .eq("is_deleted", false)
                .last("LIMIT 1"));
    }

    private AgentHandoffApi.CreateResult replay(AgentHandoffEntity entity, String requestHash) {
        if (!requestHash.equals(entity.getRequestHash())) throw AgentException.idempotencyConflict();
        return toCreateResult(entity);
    }

    private AgentHandoffApi.CreateResult toCreateResult(AgentHandoffEntity entity) {
        AgentHandoffStatus status = AgentHandoffStatus.OPEN;
        return new AgentHandoffApi.CreateResult(
                entity.getHandoffNo(), status.value(), status.label(),
                new AgentHandoffApi.Assignee(
                        entity.getAssignedEmployeeId(), entity.getAssignedEmployeeName(),
                        entity.getAssignedDepartmentName()
                ),
                entity.getCreatedAt()
        );
    }

    private String requestHash(NormalizedRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("method", "POST");
        payload.put("path", "/agent/v1/handoffs");
        payload.set("body", objectMapper.valueToTree(request));
        return AgentIdempotencySupport.hash(objectMapper, payload);
    }

    private String newHandoffNo(OffsetDateTime now) {
        return "HO-%s-%s".formatted(
                now.toLocalDate().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase()
        );
    }

    private AgentException validation(String field, String reason) {
        return AgentException.validation("转人工待办请求不符合业务 Schema", Map.of(field, reason));
    }

    private record NormalizedRequest(
            String conversationId,
            Long customerId,
            AgentHandoffReason reason,
            AgentHandoffPriority priority,
            String summary,
            List<NormalizedMessage> messages,
            NormalizedRelated related
    ) { }

    private record NormalizedMessage(
            String messageId,
            String senderName,
            OffsetDateTime sentAt,
            String content
    ) { }

    private record NormalizedRelated(
            Long productId,
            Long scheduleId,
            String teamNo,
            String quoteRequestId
    ) { }

    private record RelatedContext(
            SalesTeamEntity schedule,
            SalesTeamEntity team,
            AgentQuoteRequestEntity quote
    ) {
        private SalesTeamEntity routingTeam() {
            return schedule != null ? schedule : team;
        }
    }

    private record Assignee(Long employeeId, String employeeName, String departmentName) { }
}
