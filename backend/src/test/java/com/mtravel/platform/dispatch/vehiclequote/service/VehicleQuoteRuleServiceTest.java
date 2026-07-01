package com.mtravel.platform.dispatch.vehiclequote.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteCalculateRequest;
import com.mtravel.platform.dispatch.vehiclequote.dto.VehicleQuoteRuleSaveRequest;
import com.mtravel.platform.dispatch.vehiclequote.entity.VehicleQuoteRuleEntity;
import com.mtravel.platform.dispatch.vehiclequote.mapper.VehicleQuoteRuleMapper;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 座位数报价规则服务测试。
 *
 * <p>用车报价只给产品团队安排和计调询价提供参考价，不代表正式派车成本。测试重点固定：
 * 基础公里、超公里单价、浮动比例、最低价和规则快照。</p>
 */
class VehicleQuoteRuleServiceTest {

    @Test
    void calculateShouldUseBasePriceExtraKilometersAndFloatRate() {
        VehicleQuoteRuleMapper mapper = mock(VehicleQuoteRuleMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        VehicleQuoteRuleService service = new VehicleQuoteRuleService(mapper, expenseItemMapper);
        VehicleQuoteRuleEntity rule = rule(
                "39座大巴",
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("5.00"),
                new BigDecimal("900.00"),
                new BigDecimal("1.10")
        );
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(rule);

        var result = service.calculate(
                new VehicleQuoteCalculateRequest("39座大巴", "浙江省", "杭州市", null, 150_000),
                1L
        );

        assertThat(result.vehicleType()).isEqualTo("39座大巴");
        assertThat(result.distanceKilometers()).isEqualByComparingTo("150.00");
        assertThat(result.calculatedAmount()).isEqualByComparingTo("1375.00");
        assertThat(result.ruleSnapshot().basePrice()).isEqualByComparingTo("1000.00");
        assertThat(result.ruleSnapshot().extraKilometerPrice()).isEqualByComparingTo("5.00");
        assertThat(result.ruleSnapshot().floatRate()).isEqualByComparingTo("1.10");
    }

    @Test
    void calculateShouldRespectMinimumPriceWhenDistanceIsShort() {
        VehicleQuoteRuleMapper mapper = mock(VehicleQuoteRuleMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        VehicleQuoteRuleService service = new VehicleQuoteRuleService(mapper, expenseItemMapper);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(rule(
                "7座商务",
                new BigDecimal("300.00"),
                new BigDecimal("80.00"),
                new BigDecimal("4.00"),
                new BigDecimal("500.00"),
                BigDecimal.ONE
        ));

        var result = service.calculate(
                new VehicleQuoteCalculateRequest("7座商务", "浙江省", "杭州市", null, 20_000),
                1L
        );

        assertThat(result.distanceKilometers()).isEqualByComparingTo("20.00");
        assertThat(result.calculatedAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void createShouldPersistActiveRuleWithTenantAndOperator() {
        VehicleQuoteRuleMapper mapper = mock(VehicleQuoteRuleMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        VehicleQuoteRuleService service = new VehicleQuoteRuleService(mapper, expenseItemMapper);
        ArgumentCaptor<VehicleQuoteRuleEntity> captor = ArgumentCaptor.forClass(VehicleQuoteRuleEntity.class);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(VehicleQuoteRuleEntity.class))).thenAnswer(invocation -> {
            VehicleQuoteRuleEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        });
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            VehicleQuoteRuleEntity entity = rule("39座大巴", new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("5"), new BigDecimal("900"), BigDecimal.ONE);
            entity.setId(99L);
            entity.setTenantId(1L);
            return entity;
        });

        service.create(new VehicleQuoteRuleSaveRequest(
                "39座大巴",
                "浙江省",
                "杭州市",
                null,
                new BigDecimal("1000"),
                new BigDecimal("100"),
                new BigDecimal("5"),
                new BigDecimal("900"),
                BigDecimal.ONE,
                "active",
                "杭州周边参考价"
        ), 1L, "admin");

        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getVehicleType()).isEqualTo("39座大巴");
        assertThat(captor.getValue().getProvince()).isNull();
        assertThat(captor.getValue().getCity()).isNull();
        assertThat(captor.getValue().getDistrict()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getIsDeleted()).isFalse();
    }

    @Test
    void createActiveRuleShouldCreateMissingVehicleExpenseItem() {
        VehicleQuoteRuleMapper mapper = mock(VehicleQuoteRuleMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        VehicleQuoteRuleService service = new VehicleQuoteRuleService(mapper, expenseItemMapper);
        ArgumentCaptor<EnterpriseExpenseItemEntity> captor = ArgumentCaptor.forClass(EnterpriseExpenseItemEntity.class);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(VehicleQuoteRuleEntity.class))).thenAnswer(invocation -> {
            VehicleQuoteRuleEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        });
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            VehicleQuoteRuleEntity entity = rule("60座", new BigDecimal("1800"), new BigDecimal("100"), new BigDecimal("8"), new BigDecimal("1200"), BigDecimal.ONE);
            entity.setId(99L);
            entity.setTenantId(1L);
            return entity;
        });
        when(expenseItemMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        service.create(new VehicleQuoteRuleSaveRequest(
                "60座",
                null,
                null,
                null,
                new BigDecimal("1800"),
                new BigDecimal("100"),
                new BigDecimal("8"),
                new BigDecimal("1200"),
                BigDecimal.ONE,
                "active",
                "六十座参考价"
        ), 1L, "admin");

        verify(expenseItemMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getResourceType()).isEqualTo("vehicle");
        assertThat(captor.getValue().getProjectName()).isEqualTo("60座");
        assertThat(captor.getValue().getStatisticsEnabled()).isTrue();
        assertThat(captor.getValue().getSortOrder()).isEqualTo(60);
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getIsDeleted()).isFalse();
    }

    @Test
    void updateActiveRuleShouldEnableExistingVehicleExpenseItem() {
        VehicleQuoteRuleMapper mapper = mock(VehicleQuoteRuleMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        VehicleQuoteRuleService service = new VehicleQuoteRuleService(mapper, expenseItemMapper);
        EnterpriseExpenseItemEntity existing = new EnterpriseExpenseItemEntity();
        existing.setId(7L);
        existing.setTenantId(1L);
        existing.setResourceType("vehicle");
        existing.setProjectName("60座");
        existing.setStatus("disabled");
        ArgumentCaptor<EnterpriseExpenseItemEntity> captor = ArgumentCaptor.forClass(EnterpriseExpenseItemEntity.class);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.update(any(VehicleQuoteRuleEntity.class), any(Wrapper.class))).thenReturn(1);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(rule("60座", new BigDecimal("1800"), new BigDecimal("100"), new BigDecimal("8"), new BigDecimal("1200"), BigDecimal.ONE));
        when(expenseItemMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(expenseItemMapper.update(any(EnterpriseExpenseItemEntity.class), any(Wrapper.class))).thenReturn(1);

        service.update(99L, new VehicleQuoteRuleSaveRequest(
                "60座",
                null,
                null,
                null,
                new BigDecimal("1800"),
                new BigDecimal("100"),
                new BigDecimal("8"),
                new BigDecimal("1200"),
                BigDecimal.ONE,
                "active",
                "六十座参考价"
        ), 1L, "admin");

        verify(expenseItemMapper, never()).insert(any(EnterpriseExpenseItemEntity.class));
        verify(expenseItemMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
    }

    @Test
    void createDisabledRuleShouldNotCreateVehicleExpenseItem() {
        VehicleQuoteRuleMapper mapper = mock(VehicleQuoteRuleMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        VehicleQuoteRuleService service = new VehicleQuoteRuleService(mapper, expenseItemMapper);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(VehicleQuoteRuleEntity.class))).thenAnswer(invocation -> {
            VehicleQuoteRuleEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        });
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(rule("60座", new BigDecimal("1800"), new BigDecimal("100"), new BigDecimal("8"), new BigDecimal("1200"), BigDecimal.ONE));

        service.create(new VehicleQuoteRuleSaveRequest(
                "60座",
                null,
                null,
                null,
                new BigDecimal("1800"),
                new BigDecimal("100"),
                new BigDecimal("8"),
                new BigDecimal("1200"),
                BigDecimal.ONE,
                "disabled",
                "六十座参考价"
        ), 1L, "admin");

        verify(expenseItemMapper, never()).insert(any(EnterpriseExpenseItemEntity.class));
        verify(expenseItemMapper, never()).update(any(EnterpriseExpenseItemEntity.class), any(Wrapper.class));
    }

    private VehicleQuoteRuleEntity rule(
            String vehicleType,
            BigDecimal basePrice,
            BigDecimal baseKilometers,
            BigDecimal extraKilometerPrice,
            BigDecimal minimumPrice,
            BigDecimal floatRate
    ) {
        VehicleQuoteRuleEntity entity = new VehicleQuoteRuleEntity();
        entity.setId(10L);
        entity.setTenantId(1L);
        entity.setVehicleType(vehicleType);
        entity.setProvince("浙江省");
        entity.setCity("杭州市");
        entity.setBasePrice(basePrice);
        entity.setBaseKilometers(baseKilometers);
        entity.setExtraKilometerPrice(extraKilometerPrice);
        entity.setMinimumPrice(minimumPrice);
        entity.setFloatRate(floatRate);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }
}
