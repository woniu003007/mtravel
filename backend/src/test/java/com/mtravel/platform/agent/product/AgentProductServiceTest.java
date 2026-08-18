package com.mtravel.platform.agent.product;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.customer.productauth.mapper.CustomerProductAuthorizationMapper;
import com.mtravel.platform.sales.product.dto.SalesProductResponse;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.product.service.SalesProductService;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Agent 产品授权边界和详情白名单测试。 */
class AgentProductServiceTest {

    @Test
    void authorizedOnlyCustomerWithEmptyWhitelistShouldReceiveEmptySearchResult() {
        Fixture fixture = fixture("authorized_only");
        when(fixture.authorizationMapper().selectList(any(Wrapper.class))).thenReturn(List.of());
        AgentProductApi.SearchRequest request = new AgentProductApi.SearchRequest(
                13L, null, List.of(), List.of(), List.of(), List.of(),
                null, null, new AgentProductApi.Party(2, 0, 0, 0), false, 1, 20
        );

        var result = fixture.service().search(1L, request);

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
        verify(fixture.productMapper(), never()).selectPage(any(), any());
    }

    @Test
    void directUnauthorizedProductAndMissingProductShouldUseSameProtectedError() {
        Fixture fixture = fixture("authorized_only");
        when(fixture.productMapper().selectOne(any(Wrapper.class))).thenReturn(product());
        when(fixture.authorizationMapper().selectCount(any(Wrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> fixture.service().requireProductEntity(1L, 13L, 32L))
                .isInstanceOfSatisfying(AgentException.class, error -> {
                    assertThat(error.errorType()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(error.code()).isEqualTo(40402);
                });
    }

    @Test
    void publicDetailShouldNotSerializeInternalProductFields() throws Exception {
        Fixture fixture = fixture("all_active");
        SalesProductEntity product = product();
        when(fixture.productMapper().selectOne(any(Wrapper.class))).thenReturn(product);
        when(fixture.productService().detail(32L, 1L)).thenReturn(SalesProductResponse.fromEntity(product));

        var detail = fixture.service().detail(1L, 13L, 32L);
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(detail);

        assertThat(json).doesNotContain(
                "arrangementItems", "remark", "createdBy", "purchasePrice", "costAmount", "supplier"
        );
        assertThat(detail.productName()).isEqualTo("南京苏州研学3日营");
    }

    @Test
    void searchShouldTokenizeKeywordAndFilterSpecifiedScheduleRangeInDatabase() {
        Fixture fixture = fixture("all_active");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SalesProductEntity> emptyPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20, 0);
        emptyPage.setRecords(List.of());
        when(fixture.productMapper().selectPage(any(), any(Wrapper.class))).thenReturn(emptyPage);
        AgentProductApi.SearchRequest request = new AgentProductApi.SearchRequest(
                13L, "南京 苏州 研学", List.of(), List.of(), List.of(), List.of(), null,
                new AgentProductApi.DateRange(LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 31)),
                new AgentProductApi.Party(20, 0, 0, 0), false, 1, 20
        );

        fixture.service().search(1L, request);

        ArgumentCaptor<Wrapper<SalesProductEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(fixture.productMapper()).selectPage(any(), captor.capture());
        Wrapper<SalesProductEntity> wrapper = captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("EXISTS", "sales_teams");
        assertThat(((com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) wrapper)
                .getParamNameValuePairs().values().stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> value.contains("南京") || value.contains("苏州") || value.contains("研学")))
                .contains("%南京%", "%苏州%", "%研学%");
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture(String accessMode) {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        CustomerProductAuthorizationMapper authorizationMapper = mock(CustomerProductAuthorizationMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        AgentCustomerService customerService = mock(AgentCustomerService.class);
        SalesProductService productService = mock(SalesProductService.class);
        when(customerService.requireCapability(any(), any(), any())).thenReturn(access(accessMode));
        return new Fixture(
                new AgentProductService(
                        productMapper, authorizationMapper, teamMapper, customerService, productService
                ),
                productMapper, authorizationMapper, productService
        );
    }

    private AgentCustomerAccess access(String accessMode) {
        var context = new AgentCustomerApi.ServiceContext(
                13L, "CU-013", "南京金陵假日旅行社",
                new AgentCustomerApi.CustomerCategory(6L, "组团旅行社"),
                "normal", "正常服务", true, true, true, true, true,
                accessMode,
                new AgentCustomerApi.ServiceDepartment(2L, "华东销售部"),
                new AgentCustomerApi.Dispatcher(7L, "计调A"),
                List.of(), OffsetDateTime.now()
        );
        return new AgentCustomerAccess(context, 6L, true);
    }

    private SalesProductEntity product() {
        SalesProductEntity product = new SalesProductEntity();
        product.setId(32L);
        product.setTenantId(1L);
        product.setProductName("南京苏州研学3日营");
        product.setProductScope("template");
        product.setTravelDays(3);
        product.setStatus("active");
        product.setUpdatedAt(OffsetDateTime.now());
        product.setIsDeleted(false);
        return product;
    }

    private record Fixture(
            AgentProductService service,
            SalesProductMapper productMapper,
            CustomerProductAuthorizationMapper authorizationMapper,
            SalesProductService productService
    ) { }
}
