package com.mtravel.platform.finance.shopping.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.finance.shopping.dto.ShoppingCommissionRuleSaveRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingFeedbackDetailLineSaveRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingFeedbackLineSaveRequest;
import com.mtravel.platform.finance.shopping.dto.ShoppingSettlementCalculateRequest;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingFeedbackDetailLineEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingFeedbackLineEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingSettlementEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingSettlementLineEntity;
import com.mtravel.platform.finance.shopping.entity.FinanceShoppingTeamRuleOverrideEntity;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingCommissionRuleMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingFeedbackDetailLineMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingFeedbackLineMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingSettlementLineMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingSettlementMapper;
import com.mtravel.platform.finance.shopping.mapper.FinanceShoppingTeamRuleOverrideMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 购物佣金结算服务测试。
 *
 * <p>服务层负责读取团队人数、团队规则覆盖和购物店反馈，并把计算结果保存为结算快照。</p>
 */
class FinanceShoppingCommissionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-07T00:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void calculateShouldUseTeamRuleOverrideAndPersistSettlementSnapshot() {
        TestFixture fixture = fixture();
        FinanceShoppingTeamRuleOverrideEntity override = new FinanceShoppingTeamRuleOverrideEntity();
        override.setThresholdPerCapitaAmount(new BigDecimal("5000.00"));
        override.setBaseCommissionRate(new BigDecimal("8.00"));
        override.setTargetCommissionRate(new BigDecimal("11.00"));
        override.setLadderCalcMode("full_amount_diff");
        when(fixture.overrideMapper.selectOne(any(Wrapper.class))).thenReturn(override);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(50);
        when(fixture.feedbackLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                feedbackLine(11L, "丝绸购物店", "100000.00", "10000.00", "8000.00", "2000.00"),
                feedbackLine(12L, "珠宝购物店", "200000.00", "20000.00", "16000.00", "3000.00")
        ));
        doAnswer(invocation -> {
            FinanceShoppingSettlementEntity entity = invocation.getArgument(0);
            entity.setId(9001L);
            return 1;
        }).when(fixture.settlementMapper).insert(any(FinanceShoppingSettlementEntity.class));

        var response = fixture.service.calculateSettlement(1L, 1001L, "op01");

        assertThat(response.totalConsumptionAmount()).isEqualByComparingTo("300000.00");
        assertThat(response.perCapitaConsumptionAmount()).isEqualByComparingTo("6000.00");
        assertThat(response.ladderExtraCommissionAmount()).isEqualByComparingTo("9000.00");
        assertThat(response.companyProfitAmount()).isEqualByComparingTo("35000.00");
        ArgumentCaptor<FinanceShoppingSettlementEntity> captor =
                ArgumentCaptor.forClass(FinanceShoppingSettlementEntity.class);
        verify(fixture.settlementMapper).insert(captor.capture());
        assertThat(captor.getValue().getGuestCount()).isEqualTo(50);
        assertThat(captor.getValue().getTargetCommissionRate()).isEqualByComparingTo("11.00");
        assertThat(captor.getValue().getLadderExtraCommissionAmount()).isEqualByComparingTo("9000.00");
        assertThat(captor.getValue().getManualGuideBonusAmount()).isEqualByComparingTo("0.00");
        assertThat(captor.getValue().getInternalCompanyProfitAmount()).isEqualByComparingTo("35000.00");
        verify(fixture.settlementLineMapper, org.mockito.Mockito.times(2))
                .insert(any(FinanceShoppingSettlementLineEntity.class));
    }

    @Test
    void calculateShouldPersistManualCompanyBonusAndSubtractItFromInternalProfit() {
        TestFixture fixture = fixture();
        FinanceShoppingTeamRuleOverrideEntity override = new FinanceShoppingTeamRuleOverrideEntity();
        override.setThresholdPerCapitaAmount(new BigDecimal("5000.00"));
        override.setBaseCommissionRate(new BigDecimal("8.00"));
        override.setTargetCommissionRate(new BigDecimal("11.00"));
        override.setLadderCalcMode("full_amount_diff");
        when(fixture.overrideMapper.selectOne(any(Wrapper.class))).thenReturn(override);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(50);
        when(fixture.feedbackLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                feedbackLine(11L, "丝绸购物店", "100000.00", "10000.00", "8000.00", "2000.00"),
                feedbackLine(12L, "珠宝购物店", "200000.00", "20000.00", "16000.00", "3000.00")
        ));
        doAnswer(invocation -> {
            FinanceShoppingSettlementEntity entity = invocation.getArgument(0);
            entity.setId(9001L);
            return 1;
        }).when(fixture.settlementMapper).insert(any(FinanceShoppingSettlementEntity.class));

        var response = fixture.service.calculateSettlement(
                1L,
                1001L,
                new ShoppingSettlementCalculateRequest(new BigDecimal("9000.00"), "超额达标，公司补给导游"),
                "op01"
        );

        assertThat(response.manualGuideBonusAmount()).isEqualByComparingTo("9000.00");
        assertThat(response.manualGuideBonusRemark()).isEqualTo("超额达标，公司补给导游");
        assertThat(response.companyProfitAmount()).isEqualByComparingTo("26000.00");
        ArgumentCaptor<FinanceShoppingSettlementEntity> settlementCaptor =
                ArgumentCaptor.forClass(FinanceShoppingSettlementEntity.class);
        verify(fixture.settlementMapper).insert(settlementCaptor.capture());
        assertThat(settlementCaptor.getValue().getManualGuideBonusAmount()).isEqualByComparingTo("9000.00");
        assertThat(settlementCaptor.getValue().getManualGuideBonusRemark()).isEqualTo("超额达标，公司补给导游");
        assertThat(settlementCaptor.getValue().getInternalCompanyProfitAmount()).isEqualByComparingTo("26000.00");
    }

    @Test
    void saveFeedbackLineShouldInsertManualFeedbackWithoutRebuildingExistingLines() {
        TestFixture fixture = fixture();
        doAnswer(invocation -> {
            FinanceShoppingFeedbackLineEntity entity = invocation.getArgument(0);
            entity.setId(3001L);
            return 1;
        }).when(fixture.feedbackLineMapper).insert(any(FinanceShoppingFeedbackLineEntity.class));

        var response = fixture.service.saveFeedbackLine(
                1L,
                1001L,
                new ShoppingFeedbackLineSaveRequest(
                        null,
                        88L,
                        "丝绸购物店",
                        501L,
                        "姚导",
                        LocalDate.of(2026, 7, 7),
                        50,
                        new BigDecimal("100000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("8000.00"),
                        new BigDecimal("2000.00"),
                        "群反馈",
                        "total",
                        null
                ),
                "op01"
        );

        assertThat(response.id()).isEqualTo(3001L);
        assertThat(response.shopName()).isEqualTo("丝绸购物店");
        ArgumentCaptor<FinanceShoppingFeedbackLineEntity> captor =
                ArgumentCaptor.forClass(FinanceShoppingFeedbackLineEntity.class);
        verify(fixture.feedbackLineMapper).insert(captor.capture());
        assertThat(captor.getValue().getFeedbackSource()).isEqualTo("manual");
        assertThat(captor.getValue().getConsumptionAmount()).isEqualByComparingTo("100000.00");
        assertThat(captor.getValue().getRebateCalcMode()).isEqualTo("total");
        verify(fixture.feedbackDetailLineMapper)
                .insert(any(FinanceShoppingFeedbackDetailLineEntity.class));
    }

    @Test
    void saveFeedbackLineShouldAggregateCategoryDetailLinesIntoParentSummary() {
        TestFixture fixture = fixture();
        doAnswer(invocation -> {
            FinanceShoppingFeedbackLineEntity entity = invocation.getArgument(0);
            entity.setId(3002L);
            return 1;
        }).when(fixture.feedbackLineMapper).insert(any(FinanceShoppingFeedbackLineEntity.class));
        doAnswer(invocation -> {
            FinanceShoppingFeedbackDetailLineEntity entity = invocation.getArgument(0);
            entity.setId(entity.getSortOrder().longValue());
            return 1;
        }).when(fixture.feedbackDetailLineMapper).insert(any(FinanceShoppingFeedbackDetailLineEntity.class));

        var response = fixture.service.saveFeedbackLine(
                1L,
                1001L,
                new ShoppingFeedbackLineSaveRequest(
                        null,
                        88L,
                        "综合购物店",
                        501L,
                        "姚导",
                        LocalDate.of(2026, 7, 7),
                        55,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "按品类核对",
                        "category",
                        List.of(
                                new ShoppingFeedbackDetailLineSaveRequest(
                                        null,
                                        "翡翠",
                                        55,
                                        new BigDecimal("1.00"),
                                        new BigDecimal("5315.00"),
                                        new BigDecimal("50.00"),
                                        null,
                                        new BigDecimal("10.00"),
                                        null,
                                        BigDecimal.ZERO,
                                        "7573",
                                        1
                                ),
                                new ShoppingFeedbackDetailLineSaveRequest(
                                        null,
                                        "丝绸",
                                        55,
                                        new BigDecimal("2.00"),
                                        new BigDecimal("2000.00"),
                                        BigDecimal.ZERO,
                                        new BigDecimal("300.00"),
                                        BigDecimal.ZERO,
                                        new BigDecimal("120.00"),
                                        BigDecimal.ZERO,
                                        "手工确认金额",
                                        2
                                )
                        )
                ),
                "op01"
        );

        assertThat(response.rebateCalcMode()).isEqualTo("category");
        assertThat(response.consumptionAmount()).isEqualByComparingTo("7315.00");
        assertThat(response.companyRebateAmount()).isEqualByComparingTo("2957.50");
        assertThat(response.guideCommissionAmount()).isEqualByComparingTo("651.50");
        assertThat(response.headFeeAmount()).isEqualByComparingTo("3.00");
        assertThat(response.detailLines()).hasSize(2);
        assertThat(response.detailLines().get(0).companyRebateAmount()).isEqualByComparingTo("2657.50");
        assertThat(response.detailLines().get(0).guideCommissionAmount()).isEqualByComparingTo("531.50");
        ArgumentCaptor<FinanceShoppingFeedbackLineEntity> parentCaptor =
                ArgumentCaptor.forClass(FinanceShoppingFeedbackLineEntity.class);
        verify(fixture.feedbackLineMapper).insert(parentCaptor.capture());
        assertThat(parentCaptor.getValue().getRebateCalcMode()).isEqualTo("category");
        assertThat(parentCaptor.getValue().getConsumptionAmount()).isEqualByComparingTo("7315.00");
        verify(fixture.feedbackDetailLineMapper, org.mockito.Mockito.times(2))
                .insert(any(FinanceShoppingFeedbackDetailLineEntity.class));
    }

    @Test
    void saveFeedbackLineShouldCreateSyntheticComprehensiveDetailForTotalMode() {
        TestFixture fixture = fixture();
        doAnswer(invocation -> {
            FinanceShoppingFeedbackLineEntity entity = invocation.getArgument(0);
            entity.setId(3003L);
            return 1;
        }).when(fixture.feedbackLineMapper).insert(any(FinanceShoppingFeedbackLineEntity.class));

        var response = fixture.service.saveFeedbackLine(
                1L,
                1001L,
                new ShoppingFeedbackLineSaveRequest(
                        null,
                        88L,
                        "综合购物店",
                        501L,
                        "姚导",
                        LocalDate.of(2026, 7, 7),
                        55,
                        new BigDecimal("5315.00"),
                        new BigDecimal("2657.50"),
                        new BigDecimal("531.50"),
                        new BigDecimal("55.00"),
                        "总额反馈",
                        "total",
                        List.of()
                ),
                "op01"
        );

        assertThat(response.rebateCalcMode()).isEqualTo("total");
        assertThat(response.detailLines()).hasSize(1);
        assertThat(response.detailLines().get(0).categoryName()).isEqualTo("综合");
        assertThat(response.detailLines().get(0).consumptionAmount()).isEqualByComparingTo("5315.00");
        ArgumentCaptor<FinanceShoppingFeedbackDetailLineEntity> detailCaptor =
                ArgumentCaptor.forClass(FinanceShoppingFeedbackDetailLineEntity.class);
        verify(fixture.feedbackDetailLineMapper).insert(detailCaptor.capture());
        assertThat(detailCaptor.getValue().getCategoryName()).isEqualTo("综合");
        assertThat(detailCaptor.getValue().getCompanyRebateAmount()).isEqualByComparingTo("2657.50");
    }

    @Test
    void saveTeamRuleOverrideShouldPersistDispatcherAdjustableRates() {
        TestFixture fixture = fixture();
        doAnswer(invocation -> {
            FinanceShoppingTeamRuleOverrideEntity entity = invocation.getArgument(0);
            entity.setId(8001L);
            return 1;
        }).when(fixture.overrideMapper).insert(any(FinanceShoppingTeamRuleOverrideEntity.class));

        var response = fixture.service.saveTeamRuleOverride(
                1L,
                1001L,
                new ShoppingCommissionRuleSaveRequest(
                        new BigDecimal("4800.00"),
                        new BigDecimal("8.00"),
                        new BigDecimal("10.50"),
                        "团质较好，按本团协议比例"
                ),
                "dispatcher01"
        );

        assertThat(response.thresholdPerCapitaAmount()).isEqualByComparingTo("4800.00");
        assertThat(response.targetCommissionRate()).isEqualByComparingTo("10.50");
        ArgumentCaptor<FinanceShoppingTeamRuleOverrideEntity> captor =
                ArgumentCaptor.forClass(FinanceShoppingTeamRuleOverrideEntity.class);
        verify(fixture.overrideMapper).insert(captor.capture());
        assertThat(captor.getValue().getOverrideReason()).isEqualTo("团质较好，按本团协议比例");
    }

    private static TestFixture fixture() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        FinanceShoppingCommissionRuleMapper ruleMapper = mock(FinanceShoppingCommissionRuleMapper.class);
        FinanceShoppingTeamRuleOverrideMapper overrideMapper = mock(FinanceShoppingTeamRuleOverrideMapper.class);
        FinanceShoppingFeedbackLineMapper feedbackLineMapper = mock(FinanceShoppingFeedbackLineMapper.class);
        FinanceShoppingFeedbackDetailLineMapper feedbackDetailLineMapper = mock(FinanceShoppingFeedbackDetailLineMapper.class);
        FinanceShoppingSettlementMapper settlementMapper = mock(FinanceShoppingSettlementMapper.class);
        FinanceShoppingSettlementLineMapper settlementLineMapper = mock(FinanceShoppingSettlementLineMapper.class);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team());
        FinanceShoppingCommissionService service = new FinanceShoppingCommissionService(
                teamMapper,
                orderMapper,
                ruleMapper,
                overrideMapper,
                feedbackLineMapper,
                feedbackDetailLineMapper,
                settlementMapper,
                settlementLineMapper,
                new FinanceShoppingCommissionCalculator(),
                FIXED_CLOCK
        );
        return new TestFixture(
                service,
                teamMapper,
                orderMapper,
                ruleMapper,
                overrideMapper,
                feedbackLineMapper,
                feedbackDetailLineMapper,
                settlementMapper,
                settlementLineMapper
        );
    }

    private static SalesTeamEntity team() {
        SalesTeamEntity entity = new SalesTeamEntity();
        entity.setId(1001L);
        entity.setTenantId(1L);
        entity.setTeamNo("CS-DJ-260707A");
        entity.setTeamType("sanpin");
        entity.setBusinessType("迪士尼");
        entity.setDepartureDate(LocalDate.of(2026, 7, 10));
        return entity;
    }

    private static FinanceShoppingFeedbackLineEntity feedbackLine(
            Long id,
            String shopName,
            String consumptionAmount,
            String companyRebateAmount,
            String guideCommissionAmount,
            String headFeeAmount
    ) {
        FinanceShoppingFeedbackLineEntity entity = new FinanceShoppingFeedbackLineEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setTeamId(1001L);
        entity.setShopName(shopName);
        entity.setBusinessDate(LocalDate.of(2026, 7, 7));
        entity.setPeopleCount(50);
        entity.setConsumptionAmount(new BigDecimal(consumptionAmount));
        entity.setCompanyRebateAmount(new BigDecimal(companyRebateAmount));
        entity.setGuideCommissionAmount(new BigDecimal(guideCommissionAmount));
        entity.setHeadFeeAmount(new BigDecimal(headFeeAmount));
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private record TestFixture(
            FinanceShoppingCommissionService service,
            SalesTeamMapper teamMapper,
            SalesBookingOrderMapper orderMapper,
            FinanceShoppingCommissionRuleMapper ruleMapper,
            FinanceShoppingTeamRuleOverrideMapper overrideMapper,
            FinanceShoppingFeedbackLineMapper feedbackLineMapper,
            FinanceShoppingFeedbackDetailLineMapper feedbackDetailLineMapper,
            FinanceShoppingSettlementMapper settlementMapper,
            FinanceShoppingSettlementLineMapper settlementLineMapper
    ) {
    }
}
