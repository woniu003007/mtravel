package com.mtravel.platform.agent.quote;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.agent.quote.dto.AgentQuoteApi;
import com.mtravel.platform.agent.quote.entity.AgentQuoteRequestEntity;
import com.mtravel.platform.agent.quote.mapper.AgentQuoteRequestMapper;
import com.mtravel.platform.agent.quote.service.AgentQuoteRequestService;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Agent 询价任务严格校验、幂等和客户可见结果测试。 */
class AgentQuoteRequestServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void repeatedSameRequestShouldReturnOriginalTaskWithoutSecondInsert() throws Exception {
        AgentQuoteRequestMapper mapper = mock(AgentQuoteRequestMapper.class);
        AtomicReference<AgentQuoteRequestEntity> stored = new AtomicReference<>();
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(ignored -> stored.get());
        when(mapper.insertIdempotent(any(AgentQuoteRequestEntity.class))).thenAnswer(invocation -> {
            AgentQuoteRequestEntity entity = invocation.getArgument(0);
            entity.setId(88L);
            stored.set(entity);
            return 1;
        });
        AgentQuoteRequestService service = service(mapper);
        AgentQuoteApi.CreateRequest request = hotelRequest(requirements(""));

        var first = service.create(caller(), "quote-key-00000001", request);
        stored.get().setStatus("processing");
        var replay = service.create(caller(), "quote-key-00000001", request);

        assertThat(replay.quoteRequestId()).isEqualTo(first.quoteRequestId());
        assertThat(replay.status()).isEqualTo("pending");
        verify(mapper, times(1)).insertIdempotent(any(AgentQuoteRequestEntity.class));
    }

    @Test
    void repeatedKeyWithDifferentBodyShouldConflict() throws Exception {
        AgentQuoteRequestMapper mapper = mock(AgentQuoteRequestMapper.class);
        AtomicReference<AgentQuoteRequestEntity> stored = new AtomicReference<>();
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(ignored -> stored.get());
        when(mapper.insertIdempotent(any(AgentQuoteRequestEntity.class))).thenAnswer(invocation -> {
            AgentQuoteRequestEntity entity = invocation.getArgument(0);
            entity.setId(89L);
            stored.set(entity);
            return 1;
        });
        AgentQuoteRequestService service = service(mapper);
        service.create(caller(), "quote-key-00000002", hotelRequest(requirements("")));

        assertThatThrownBy(() -> service.create(
                caller(), "quote-key-00000002", hotelRequest(requirements(",\"notes\":\"另一个需求\""))
        )).isInstanceOfSatisfying(AgentException.class, error ->
                assertThat(error.errorType()).isEqualTo("IDEMPOTENCY_CONFLICT")
        );
    }

    @Test
    void requirementsShouldRejectUnknownFields() throws Exception {
        AgentQuoteRequestService service = service(mock(AgentQuoteRequestMapper.class));

        assertThatThrownBy(() -> service.create(
                caller(), "quote-key-00000003", hotelRequest(requirements(",\"supplierPrice\":100"))
        )).isInstanceOfSatisfying(AgentException.class, error ->
                assertThat(error.errorType()).isEqualTo("VALIDATION_FAILED")
        );
    }

    @Test
    void detailShouldExposeOnlyApprovedVisibleAndUnexpiredQuote() {
        AgentQuoteRequestMapper mapper = mock(AgentQuoteRequestMapper.class);
        AgentQuoteRequestEntity entity = new AgentQuoteRequestEntity();
        entity.setTenantId(1L);
        entity.setCustomerId(13L);
        entity.setRequestNo("QR-20260711-0001");
        entity.setStatus("quoted");
        entity.setCustomerVisible(true);
        entity.setReplyText("双标间含早，每间480元。");
        entity.setTotalAmount(new BigDecimal("4800.00"));
        entity.setCurrency("CNY");
        entity.setValidUntil(OffsetDateTime.now().plusHours(3));
        entity.setApprovedAt(OffsetDateTime.now());
        entity.setAssignedEmployeeName("计调A");
        entity.setAssignedDepartmentName("华东销售部");
        entity.setUpdatedAt(OffsetDateTime.now());
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(entity);

        var result = service(mapper).detail(1L, 13L, "QR-20260711-0001");

        assertThat(result.quote()).isNotNull();
        assertThat(result.quote().totalAmount()).isEqualTo("4800.00");
        assertThat(result.quote().replyText()).contains("每间480元");
    }

    @Test
    void detailShouldHideInternalOrExpiredQuoteValues() {
        AgentQuoteRequestMapper mapper = mock(AgentQuoteRequestMapper.class);
        AgentQuoteRequestEntity entity = new AgentQuoteRequestEntity();
        entity.setTenantId(1L);
        entity.setCustomerId(13L);
        entity.setRequestNo("QR-20260711-0002");
        entity.setStatus("quoted");
        entity.setCustomerVisible(false);
        entity.setReplyText("不允许对客显示");
        entity.setTotalAmount(new BigDecimal("1.00"));
        entity.setValidUntil(OffsetDateTime.now().minusMinutes(1));
        entity.setUpdatedAt(OffsetDateTime.now());
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(entity);

        var result = service(mapper).detail(1L, 13L, "QR-20260711-0002");

        assertThat(result.quote()).isNull();
    }

    private AgentQuoteRequestService service(AgentQuoteRequestMapper mapper) {
        AgentCustomerService customerService = mock(AgentCustomerService.class);
        AgentProductService productService = mock(AgentProductService.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        when(customerService.requireCapability(any(), any(), any())).thenReturn(customerAccess());
        when(customerService.accessContext(1L, 13L)).thenReturn(customerAccess());
        return new AgentQuoteRequestService(mapper, customerService, productService, teamMapper, objectMapper);
    }

    private AgentQuoteApi.CreateRequest hotelRequest(JsonNode requirements) {
        return new AgentQuoteApi.CreateRequest(
                "web-group-0001", 13L, "hotel_extra_stay", null, null,
                "我们想提前一天到南京，夫子庙附近双标间需要10间。", requirements
        );
    }

    private JsonNode requirements(String extraField) throws Exception {
        return objectMapper.readTree("""
                {
                  "city": "南京市",
                  "area": "夫子庙附近",
                  "checkIn": "2099-07-14",
                  "checkOut": "2099-07-15",
                  "roomType": "双标间",
                  "roomCount": 10,
                  "guestCount": 20,
                  "breakfastRequired": true
                  %s
                }
                """.formatted(extraField));
    }

    private AgentServicePrincipal caller() {
        return new AgentServicePrincipal(5L, 1L, "agent-test", Set.of("agent:write:quote-request"));
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
}
