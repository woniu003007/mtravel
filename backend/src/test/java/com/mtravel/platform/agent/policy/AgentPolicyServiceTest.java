package com.mtravel.platform.agent.policy;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.policy.entity.AgentBusinessPolicyEntity;
import com.mtravel.platform.agent.policy.mapper.AgentBusinessPolicyMapper;
import com.mtravel.platform.agent.policy.service.AgentPolicyService;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Agent 结构化政策解析测试。 */
class AgentPolicyServiceTest {

    @Test
    void missingPolicyShouldRequireHandoff() {
        Fixture fixture = fixture(List.of());

        var result = fixture.service().search(
                1L, 13L, null, null, "minor_without_guardian", LocalDate.of(2026, 7, 15)
        );

        assertThat(result.answerable()).isFalse();
        assertThat(result.mustHandoff()).isTrue();
        assertThat(result.effectiveReviewLevel()).isEqualTo("human_review");
        assertThat(result.items()).isEmpty();
    }

    @Test
    void schedulePolicyShouldOverrideLowerPriorityPolicies() {
        Fixture fixture = fixture(List.of(
                policy(1L, "enterprise", null, "auto_answer", "企业规则"),
                policy(2L, "customer_category", 6L, "human_review", "客户类别规则"),
                policy(3L, "product", 32L, "human_review", "产品规则"),
                policy(4L, "schedule", 47L, "auto_answer", "团期规则")
        ));

        var result = fixture.service().search(
                1L, 13L, 32L, 47L, "minor_without_guardian", LocalDate.of(2026, 7, 15)
        );

        assertThat(result.items()).extracting(item -> item.policyId()).containsExactly(4L);
        assertThat(result.effectiveReviewLevel()).isEqualTo("auto_answer");
        assertThat(result.answerable()).isTrue();
        assertThat(result.mustHandoff()).isFalse();
    }

    @Test
    void conflictingPoliciesAtSameScopeShouldEscalateToHumanReview() {
        Fixture fixture = fixture(List.of(
                policy(11L, "product", 32L, "auto_answer", "允许接待"),
                policy(12L, "product", 32L, "auto_answer", "不允许接待")
        ));

        var result = fixture.service().search(
                1L, 13L, 32L, null, "minor_without_guardian", LocalDate.of(2026, 7, 15)
        );

        assertThat(result.conflicts()).hasSize(1);
        assertThat(result.conflicts().getFirst().policyIds()).containsExactlyInAnyOrder(11L, 12L);
        assertThat(result.effectiveReviewLevel()).isEqualTo("human_review");
        assertThat(result.mustHandoff()).isTrue();
    }

    @Test
    void prohibitedPolicyShouldNeverBeAnswerable() {
        Fixture fixture = fixture(List.of(
                policy(21L, "enterprise", null, "prohibited", "禁止自动回答")
        ));

        var result = fixture.service().search(
                1L, 13L, null, null, "refund_change", LocalDate.of(2026, 7, 15)
        );

        assertThat(result.effectiveReviewLevel()).isEqualTo("prohibited");
        assertThat(result.answerable()).isFalse();
        assertThat(result.mustHandoff()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture(List<AgentBusinessPolicyEntity> policies) {
        AgentBusinessPolicyMapper policyMapper = mock(AgentBusinessPolicyMapper.class);
        AgentCustomerService customerService = mock(AgentCustomerService.class);
        AgentProductService productService = mock(AgentProductService.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        when(customerService.requireCapability(any(), any(), any())).thenReturn(customerAccess());
        when(policyMapper.selectList(any(Wrapper.class))).thenReturn(policies);
        SalesProductEntity product = new SalesProductEntity();
        product.setId(32L);
        product.setTenantId(1L);
        when(productService.requireProductEntity(1L, 13L, 32L)).thenReturn(product);
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(47L);
        team.setTenantId(1L);
        team.setProductId(32L);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        return new Fixture(new AgentPolicyService(policyMapper, customerService, productService, teamMapper));
    }

    private AgentCustomerAccess customerAccess() {
        AgentCustomerApi.ServiceContext context = new AgentCustomerApi.ServiceContext(
                13L, "CU-013", "南京金陵假日旅行社",
                new AgentCustomerApi.CustomerCategory(6L, "组团旅行社"),
                "normal", "正常服务", true, true, true, true, true,
                "authorized_only",
                new AgentCustomerApi.ServiceDepartment(2L, "华东销售部"),
                new AgentCustomerApi.Dispatcher(7L, "计调A"),
                List.of(), OffsetDateTime.now()
        );
        return new AgentCustomerAccess(context, 6L, true);
    }

    private AgentBusinessPolicyEntity policy(
            Long id,
            String scopeType,
            Long scopeId,
            String reviewLevel,
            String content
    ) {
        AgentBusinessPolicyEntity entity = new AgentBusinessPolicyEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setScopeType(scopeType);
        entity.setScopeId(scopeId);
        entity.setTopic("minor_without_guardian");
        entity.setTitle("未成年人接待规则");
        entity.setContent(content);
        entity.setReviewLevel(reviewLevel);
        entity.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        entity.setVersion("2026.1");
        entity.setStatus("active");
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setIsDeleted(false);
        return entity;
    }

    private record Fixture(AgentPolicyService service) { }
}
