package com.mtravel.platform.agent.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerCapability;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.policy.dto.AgentPolicyApi;
import com.mtravel.platform.agent.policy.entity.AgentBusinessPolicyEntity;
import com.mtravel.platform.agent.policy.enums.AgentPolicyReviewLevel;
import com.mtravel.platform.agent.policy.enums.AgentPolicyTopic;
import com.mtravel.platform.agent.policy.mapper.AgentBusinessPolicyMapper;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Agent 政策有效性、优先级、冲突和复核级别解析服务。 */
@Service
public class AgentPolicyService {

    private final AgentBusinessPolicyMapper policyMapper;
    private final AgentCustomerService customerService;
    private final AgentProductService productService;
    private final SalesTeamMapper teamMapper;

    public AgentPolicyService(
            AgentBusinessPolicyMapper policyMapper,
            AgentCustomerService customerService,
            AgentProductService productService,
            SalesTeamMapper teamMapper
    ) {
        this.policyMapper = policyMapper;
        this.customerService = customerService;
        this.productService = productService;
        this.teamMapper = teamMapper;
    }

    /** 按团期、产品、客户类别、企业的顺序解析查询日有效政策。 */
    public AgentPolicyApi.SearchResult search(
            Long tenantId,
            Long customerId,
            Long productId,
            Long scheduleId,
            String topic,
            LocalDate onDate
    ) {
        if (!AgentPolicyTopic.supports(topic) || onDate == null) {
            throw AgentException.validation("政策查询参数不合法", Map.of("topic", String.valueOf(topic)));
        }
        if (scheduleId != null && productId == null) {
            throw AgentException.validation("查询团期政策时必须同时传入 productId", Map.of("productId", "required"));
        }

        AgentCustomerAccess customer = customerService.requireCapability(
                tenantId, customerId, AgentCustomerCapability.QUERY_POLICIES
        );
        if (productId != null) {
            productService.requireProductEntity(tenantId, customerId, productId);
        }
        if (scheduleId != null) {
            SalesTeamEntity schedule = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("id", scheduleId)
                    .eq("product_id", productId)
                    .eq("is_deleted", false)
                    .last("LIMIT 1"));
            if (schedule == null) throw AgentException.resourceNotFound();
        }

        Long categoryId = customer.categoryId();
        List<AgentBusinessPolicyEntity> applicable = policyMapper.selectList(
                        policyQuery(tenantId, topic, onDate, categoryId, productId, scheduleId)
                ).stream()
                .filter(policy -> applies(policy, categoryId, productId, scheduleId))
                .toList();
        if (applicable.isEmpty()) return missingPolicy();

        int highestPriority = applicable.stream().mapToInt(policy -> priority(policy.getScopeType())).max().orElse(0);
        List<AgentBusinessPolicyEntity> effective = applicable.stream()
                .filter(policy -> priority(policy.getScopeType()) == highestPriority)
                .sorted(Comparator.comparing(AgentBusinessPolicyEntity::getId))
                .toList();
        List<AgentPolicyApi.PolicyConflict> conflicts = conflicts(effective);
        AgentPolicyReviewLevel level = effective.stream()
                .map(policy -> AgentPolicyReviewLevel.fromStoredValue(policy.getReviewLevel()))
                .max(Comparator.comparingInt(AgentPolicyReviewLevel::severity))
                .orElse(AgentPolicyReviewLevel.HUMAN_REVIEW);
        if (!conflicts.isEmpty() && level == AgentPolicyReviewLevel.AUTO_ANSWER) {
            level = AgentPolicyReviewLevel.HUMAN_REVIEW;
        }
        boolean prohibited = level == AgentPolicyReviewLevel.PROHIBITED;
        return new AgentPolicyApi.SearchResult(
                !prohibited,
                level != AgentPolicyReviewLevel.AUTO_ANSWER,
                level.value(),
                conflicts,
                effective.stream().map(this::toItem).toList(),
                OffsetDateTime.now()
        );
    }

    private QueryWrapper<AgentBusinessPolicyEntity> policyQuery(
            Long tenantId,
            String topic,
            LocalDate onDate,
            Long categoryId,
            Long productId,
            Long scheduleId
    ) {
        QueryWrapper<AgentBusinessPolicyEntity> wrapper = new QueryWrapper<AgentBusinessPolicyEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("topic", topic)
                .le("effective_from", onDate)
                .and(date -> date.isNull("effective_to").or().ge("effective_to", onDate));
        wrapper.and(scope -> {
            scope.nested(item -> item.eq("scope_type", "enterprise").isNull("scope_id"));
            if (categoryId != null) {
                scope.or(item -> item.eq("scope_type", "customer_category").eq("scope_id", categoryId));
            }
            if (productId != null) {
                scope.or(item -> item.eq("scope_type", "product").eq("scope_id", productId));
            }
            if (scheduleId != null) {
                scope.or(item -> item.eq("scope_type", "schedule").eq("scope_id", scheduleId));
            }
        });
        return wrapper.orderByDesc("updated_at").orderByDesc("id");
    }

    private boolean applies(
            AgentBusinessPolicyEntity policy,
            Long categoryId,
            Long productId,
            Long scheduleId
    ) {
        return switch (policy.getScopeType()) {
            case "enterprise" -> policy.getScopeId() == null;
            case "customer_category" -> Objects.equals(policy.getScopeId(), categoryId);
            case "product" -> Objects.equals(policy.getScopeId(), productId);
            case "schedule" -> Objects.equals(policy.getScopeId(), scheduleId);
            default -> false;
        };
    }

    private List<AgentPolicyApi.PolicyConflict> conflicts(List<AgentBusinessPolicyEntity> policies) {
        long distinctRules = policies.stream()
                .map(policy -> policy.getReviewLevel() + "\u0000" + policy.getContent())
                .distinct()
                .count();
        if (distinctRules <= 1) return List.of();
        return List.of(new AgentPolicyApi.PolicyConflict(
                policies.stream().map(AgentBusinessPolicyEntity::getId).toList(),
                "同一政策范围存在多条不一致的有效规则，需人工确认"
        ));
    }

    private AgentPolicyApi.PolicyItem toItem(AgentBusinessPolicyEntity entity) {
        return new AgentPolicyApi.PolicyItem(
                entity.getId(), entity.getScopeType(), entity.getTopic(), entity.getTitle(), entity.getContent(),
                AgentPolicyReviewLevel.fromStoredValue(entity.getReviewLevel()).value(), entity.getEffectiveFrom(),
                entity.getEffectiveTo(), entity.getVersion(), entity.getUpdatedAt()
        );
    }

    private AgentPolicyApi.SearchResult missingPolicy() {
        return new AgentPolicyApi.SearchResult(
                false, true, AgentPolicyReviewLevel.HUMAN_REVIEW.value(), List.of(), List.of(), OffsetDateTime.now()
        );
    }

    private int priority(String scopeType) {
        return switch (scopeType) {
            case "schedule" -> 4;
            case "product" -> 3;
            case "customer_category" -> 2;
            case "enterprise" -> 1;
            default -> 0;
        };
    }
}
