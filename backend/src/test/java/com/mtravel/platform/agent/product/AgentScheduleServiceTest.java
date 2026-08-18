package com.mtravel.platform.agent.product;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.customer.service.AgentCustomerAccess;
import com.mtravel.platform.agent.customer.service.AgentCustomerCapability;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.agent.product.service.AgentScheduleService;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamPriceEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamPriceMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Agent 团期余位和客户适用价测试。 */
class AgentScheduleServiceTest {

    @Test
    void zeroRequiredPriceShouldReturnManualQuoteInsteadOfFree() {
        AgentScheduleService service = service(price("0.00"), true);

        var result = service.schedules(
                1L, 13L, 32L,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(20),
                new AgentProductApi.Party(2, 0, 0, 0), 0, 1, 20
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().price().status()).isEqualTo("manual_quote");
        assertThat(result.items().getFirst().price().calculatedTotal()).isNull();
        assertThat(result.items().getFirst().price().calculationComplete()).isFalse();
    }

    @Test
    void confirmedPriceShouldBeCalculatedByBackendWithDecimalAmount() {
        AgentScheduleService service = service(price("1000.00"), true);

        var result = service.schedules(
                1L, 13L, 32L,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(20),
                new AgentProductApi.Party(2, 0, 0, 0), 0, 1, 20
        );

        var price = result.items().getFirst().price();
        assertThat(price.status()).isEqualTo("confirmed");
        assertThat(price.calculatedTotal()).isEqualTo("2000.00");
        assertThat(price.calculationComplete()).isTrue();
        assertThat(price.taxIncluded()).isTrue();
    }

    @Test
    void missingDateShouldReturnValidationErrorInsteadOfNullPointerFailure() {
        AgentScheduleService service = service(price("1000.00"), true);

        assertThatThrownBy(() -> service.schedules(
                1L, 13L, 32L, null, LocalDate.now().plusDays(20),
                new AgentProductApi.Party(2, 0, 0, 0), 0, 1, 20
        )).isInstanceOfSatisfying(AgentException.class, error ->
                assertThat(error.errorType()).isEqualTo("VALIDATION_FAILED")
        );
    }

    @SuppressWarnings("unchecked")
    private AgentScheduleService service(SalesTeamPriceEntity price, Boolean taxIncluded) {
        AgentCustomerService customerService = mock(AgentCustomerService.class);
        AgentProductService productService = mock(AgentProductService.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        when(customerService.requireCapability(eq(1L), eq(13L), eq(AgentCustomerCapability.QUERY_PRODUCTS)))
                .thenReturn(access(taxIncluded));
        when(productService.requireProductEntity(1L, 13L, 32L)).thenReturn(product());
        Page<SalesTeamEntity> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(team()));
        when(teamMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);
        when(priceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(price));
        return new AgentScheduleService(customerService, productService, teamMapper, priceMapper);
    }

    private AgentCustomerAccess access(Boolean taxIncluded) {
        var context = new AgentCustomerApi.ServiceContext(
                13L, "CU-013", "南京金陵假日旅行社",
                new AgentCustomerApi.CustomerCategory(6L, "组团旅行社"),
                "normal", "正常服务", true, true, true, true, true,
                "authorized_only",
                new AgentCustomerApi.ServiceDepartment(2L, "华东销售部"),
                new AgentCustomerApi.Dispatcher(7L, "计调A"),
                List.of(), OffsetDateTime.now()
        );
        return new AgentCustomerAccess(context, 6L, taxIncluded);
    }

    private SalesProductEntity product() {
        SalesProductEntity product = new SalesProductEntity();
        product.setId(32L);
        product.setTenantId(1L);
        product.setProductName("南京苏州研学3日营");
        product.setProductScope("template");
        product.setTravelDays(3);
        product.setStatus("active");
        product.setIsDeleted(false);
        return product;
    }

    private SalesTeamEntity team() {
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(47L);
        team.setTenantId(1L);
        team.setProductId(32L);
        team.setTeamNo("CS-SP-BK-260715A");
        team.setStatus("normal");
        team.setDepartureDate(LocalDate.now().plusDays(12));
        team.setCloseDaysBefore(3);
        team.setTotalSeats(40);
        team.setRemainingSeats(28);
        team.setSingleRoomDifference(new BigDecimal("180.00"));
        team.setUpdatedAt(OffsetDateTime.now());
        team.setIsDeleted(false);
        return team;
    }

    private SalesTeamPriceEntity price(String adultPrice) {
        SalesTeamPriceEntity price = new SalesTeamPriceEntity();
        price.setId(70L);
        price.setTenantId(1L);
        price.setTeamId(47L);
        price.setProductId(32L);
        price.setCustomerCategoryId(6L);
        price.setCustomerCategoryName("组团旅行社");
        price.setAdultPrice(new BigDecimal(adultPrice));
        price.setChildPrice(BigDecimal.ZERO);
        price.setChildNoBedPrice(BigDecimal.ZERO);
        price.setSeniorPrice(BigDecimal.ZERO);
        price.setExtraFee(BigDecimal.ZERO);
        price.setStatus("active");
        price.setIsDeleted(false);
        return price;
    }
}
