package com.mtravel.platform.agent.handoff;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.handoff.dto.AgentHandoffApi;
import com.mtravel.platform.agent.handoff.entity.AgentHandoffEntity;
import com.mtravel.platform.agent.handoff.mapper.AgentHandoffMapper;
import com.mtravel.platform.agent.handoff.mapper.AgentHandoffMessageMapper;
import com.mtravel.platform.agent.handoff.service.AgentHandoffService;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.agent.quote.mapper.AgentQuoteRequestMapper;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Agent 转人工待办幂等、消息保存和路由测试。 */
class AgentHandoffServiceTest {

    @Test
    void repeatedSameRequestShouldCreateOneHandoffAndOneMessageBatch() {
        Fixture fixture = fixture();
        AtomicReference<AgentHandoffEntity> stored = new AtomicReference<>();
        when(fixture.handoffMapper().selectOne(any(Wrapper.class))).thenAnswer(ignored -> stored.get());
        when(fixture.handoffMapper().insertIdempotentReturningId(any(AgentHandoffEntity.class)))
                .thenAnswer(invocation -> {
                    AgentHandoffEntity entity = invocation.getArgument(0);
                    entity.setId(88L);
                    stored.set(entity);
                    return 88L;
                });
        when(fixture.messageMapper().insertBatch(any())).thenReturn(1);
        AgentHandoffApi.CreateRequest request = request("客户询问未成年人接待规则。", List.of(message("msg-101")));

        var first = fixture.service().create(caller(), "handoff-key-00001", request);
        stored.get().setStatus("processing");
        var replay = fixture.service().create(caller(), "handoff-key-00001", request);

        assertThat(replay.handoffId()).isEqualTo(first.handoffId());
        assertThat(replay.status()).isEqualTo("open");
        verify(fixture.handoffMapper(), times(1)).insertIdempotentReturningId(any(AgentHandoffEntity.class));
        verify(fixture.messageMapper(), times(1)).insertBatch(any());
    }

    @Test
    void repeatedKeyWithDifferentSummaryShouldConflict() {
        Fixture fixture = fixture();
        AtomicReference<AgentHandoffEntity> stored = new AtomicReference<>();
        when(fixture.handoffMapper().selectOne(any(Wrapper.class))).thenAnswer(ignored -> stored.get());
        when(fixture.handoffMapper().insertIdempotentReturningId(any(AgentHandoffEntity.class)))
                .thenAnswer(invocation -> {
                    AgentHandoffEntity entity = invocation.getArgument(0);
                    entity.setId(89L);
                    stored.set(entity);
                    return 89L;
                });
        when(fixture.messageMapper().insertBatch(any())).thenReturn(1);
        fixture.service().create(
                caller(), "handoff-key-00002", request("第一个摘要。", List.of(message("msg-102")))
        );

        assertThatThrownBy(() -> fixture.service().create(
                caller(), "handoff-key-00002", request("已经改过的摘要。", List.of(message("msg-102")))
        )).isInstanceOfSatisfying(AgentException.class, error ->
                assertThat(error.errorType()).isEqualTo("IDEMPOTENCY_CONFLICT")
        );
    }

    @Test
    void duplicateSourceMessageIdsShouldBeRejectedBeforeInsert() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service().create(
                caller(), "handoff-key-00003",
                request("需要人工处理。", List.of(message("msg-dup"), message("msg-dup")))
        )).isInstanceOfSatisfying(AgentException.class, error ->
                assertThat(error.errorType()).isEqualTo("VALIDATION_FAILED")
        );
        verify(fixture.handoffMapper(), never()).insertIdempotentReturningId(any(AgentHandoffEntity.class));
    }

    @Test
    void scheduleOperatorShouldTakePriorityOverCustomerDispatcher() {
        Fixture fixture = fixture();
        SalesTeamEntity schedule = new SalesTeamEntity();
        schedule.setId(47L);
        schedule.setProductId(32L);
        schedule.setOperatorEmployeeId(9L);
        schedule.setOperatorEmployeeName("团期计调B");
        schedule.setDepartmentName("计调二部");
        when(fixture.teamMapper().selectOne(any(Wrapper.class))).thenReturn(schedule);
        when(fixture.handoffMapper().selectOne(any(Wrapper.class))).thenReturn(null);
        when(fixture.handoffMapper().insertIdempotentReturningId(any(AgentHandoffEntity.class))).thenReturn(90L);
        when(fixture.messageMapper().insertBatch(any())).thenReturn(1);
        AgentHandoffApi.CreateRequest request = new AgentHandoffApi.CreateRequest(
                "web-group-0001", 13L, "policy_review", "normal", "团期问题需人工确认。",
                List.of(message("msg-103")), new AgentHandoffApi.Related(32L, 47L, null, null)
        );

        var result = fixture.service().create(caller(), "handoff-key-00004", request);

        assertThat(result.assignee().employeeId()).isEqualTo(9L);
        assertThat(result.assignee().employeeName()).isEqualTo("团期计调B");
    }

    private Fixture fixture() {
        AgentHandoffMapper handoffMapper = mock(AgentHandoffMapper.class);
        AgentHandoffMessageMapper messageMapper = mock(AgentHandoffMessageMapper.class);
        AgentCustomerService customerService = mock(AgentCustomerService.class);
        AgentProductService productService = mock(AgentProductService.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        AgentQuoteRequestMapper quoteMapper = mock(AgentQuoteRequestMapper.class);
        when(customerService.requireCapability(any(), any(), any())).thenReturn(customerAccess());
        return new Fixture(
                new AgentHandoffService(
                        handoffMapper, messageMapper, customerService, productService, teamMapper,
                        orderMapper, quoteMapper, new ObjectMapper().findAndRegisterModules()
                ),
                handoffMapper, messageMapper, teamMapper
        );
    }

    private AgentHandoffApi.CreateRequest request(String summary, List<AgentHandoffApi.SourceMessage> messages) {
        return new AgentHandoffApi.CreateRequest(
                "web-group-0001", 13L, "policy_review", "normal", summary, messages, null
        );
    }

    private AgentHandoffApi.SourceMessage message(String messageId) {
        return new AgentHandoffApi.SourceMessage(
                messageId, "孙经理", OffsetDateTime.parse("2026-07-10T16:40:00+08:00"),
                "两个16岁学生没有家长同行，需要人工确认。"
        );
    }

    private AgentServicePrincipal caller() {
        return new AgentServicePrincipal(5L, 1L, "agent-test", Set.of("agent:write:handoff"));
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

    private record Fixture(
            AgentHandoffService service,
            AgentHandoffMapper handoffMapper,
            AgentHandoffMessageMapper messageMapper,
            SalesTeamMapper teamMapper
    ) { }
}
