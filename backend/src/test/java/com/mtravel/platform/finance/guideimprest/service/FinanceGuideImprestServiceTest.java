package com.mtravel.platform.finance.guideimprest.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementPriceLineEntity;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementPriceLineMapper;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestApplyRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestDecisionRequest;
import com.mtravel.platform.finance.guideimprest.dto.GuideImprestPaymentRequest;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestCalcLineEntity;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestEntity;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestPaymentEntity;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestCalcLineMapper;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestMapper;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestPaymentMapper;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 导游备用金服务测试。
 *
 * <p>备用金属于独立资金链路，金额必须由后端按照团队现付成本、自费加点抵扣和团队实收人数计算，
 * 不能让前端自己拼公式。</p>
 */
class FinanceGuideImprestServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-06T00:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void previewShouldDeductOptionalMarkupWithFixedGuideCommission() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(50);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "3000.00"),
                arrangement(12L, "optional", "迪士尼尊享自费", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                optionalLine(12L, "尊享导览", "100.00", "60.00", "5.00", "0")
        ));

        var response = fixture.service.preview(1L, 1001L, 501L);

        assertThat(response.guestCount()).isEqualTo(50);
        assertThat(response.cashCostAmount()).isEqualByComparingTo("3000.00");
        assertThat(response.optionalDeductionAmount()).isEqualByComparingTo("1225.00");
        assertThat(response.calculatedAmount()).isEqualByComparingTo("1775.00");
        assertThat(response.suggestedImprestAmount()).isEqualByComparingTo("1775.00");
        assertThat(response.guideTurnInAmount()).isEqualByComparingTo("0.00");
        assertThat(response.calcLines()).hasSize(2);
    }

    @Test
    void previewShouldUseTeamOptionalMarkupRateAsDefaultAnchor() {
        TestFixture fixture = fixture();
        SalesTeamEntity team = team();
        team.setOptionalMarkupRate(new BigDecimal("40"));
        when(fixture.teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(50);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "3000.00"),
                arrangement(12L, "optional", "迪士尼尊享自费", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                optionalLine(12L, "尊享导览", "100.00", "60.00", "5.00", "0")
        ));

        var response = fixture.service.preview(1L, 1001L, 501L);

        assertThat(response.companyMarkupRate()).isEqualByComparingTo("40.00");
        assertThat(response.optionalDeductionAmount()).isEqualByComparingTo("700.00");
        assertThat(response.calculatedAmount()).isEqualByComparingTo("2300.00");
        assertThat(response.calcLines().get(1).companyMarkupRate()).isEqualByComparingTo("40.00");
    }

    @Test
    void previewShouldFallbackToSystemConfigWhenTeamOptionalMarkupRateIsZero() {
        TestFixture fixture = fixture();
        SalesTeamEntity team = team();
        team.setOptionalMarkupRate(BigDecimal.ZERO);
        when(fixture.teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(50);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "3000.00"),
                arrangement(12L, "optional", "迪士尼尊享自费", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                optionalLine(12L, "尊享导览", "100.00", "60.00", "5.00", "0")
        ));

        var response = fixture.service.preview(1L, 1001L, 501L);

        assertThat(response.companyMarkupRate()).isEqualByComparingTo("70.00");
        assertThat(response.optionalDeductionAmount()).isEqualByComparingTo("1225.00");
    }

    @Test
    void previewShouldUseDispatcherOverrideCompanyMarkupRate() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(50);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "3000.00"),
                arrangement(12L, "optional", "迪士尼尊享自费", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                optionalLine(12L, "尊享导览", "100.00", "60.00", "5.00", "0")
        ));

        var response = fixture.service.preview(1L, 1001L, 501L, new BigDecimal("60"));

        assertThat(response.companyMarkupRate()).isEqualByComparingTo("60.00");
        assertThat(response.optionalDeductionAmount()).isEqualByComparingTo("1050.00");
        assertThat(response.calculatedAmount()).isEqualByComparingTo("1950.00");
        assertThat(response.calcLines().get(1).companyMarkupRate()).isEqualByComparingTo("60.00");
    }

    @Test
    void previewShouldCalculatePercentageGuideCommissionFromOptionalGrossProfit() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(50);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "3000.00"),
                arrangement(12L, "optional", "迪士尼尊享自费", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                optionalLine(12L, "尊享导览", "100.00", "60.00", "0.00", "10")
        ));

        var response = fixture.service.preview(1L, 1001L, 501L);

        assertThat(response.optionalDeductionAmount()).isEqualByComparingTo("1260.00");
        assertThat(response.calculatedAmount()).isEqualByComparingTo("1740.00");
    }

    @Test
    void previewShouldTurnNegativeResultIntoGuideTurnInAmount() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(50);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "1000.00"),
                arrangement(12L, "optional", "迪士尼尊享自费", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                optionalLine(12L, "尊享导览", "100.00", "60.00", "0.00", "10")
        ));

        var response = fixture.service.preview(1L, 1001L, 501L);

        assertThat(response.calculatedAmount()).isEqualByComparingTo("-260.00");
        assertThat(response.suggestedImprestAmount()).isEqualByComparingTo("0.00");
        assertThat(response.guideTurnInAmount()).isEqualByComparingTo("260.00");
    }

    @Test
    void submitShouldAllowMultipleApplicationsForSameTeamAndGuide() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(10);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "800.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(fixture.imprestMapper.maxRequestNoSuffix(any(), any())).thenReturn(0, 1);
        doAnswer(invocation -> {
            FinanceGuideImprestEntity entity = invocation.getArgument(0);
            entity.setId(entity.getId() == null ? 7001L : entity.getId());
            return 1;
        }).when(fixture.imprestMapper).insert(any(FinanceGuideImprestEntity.class));

        fixture.service.submit(
                1L,
                new GuideImprestApplyRequest(1001L, 501L, new BigDecimal("800.00"), "首笔备用金"),
                "op01"
        );
        fixture.service.submit(
                1L,
                new GuideImprestApplyRequest(1001L, 501L, new BigDecimal("300.00"), "追加备用金"),
                "op01"
        );

        ArgumentCaptor<FinanceGuideImprestEntity> captor = ArgumentCaptor.forClass(FinanceGuideImprestEntity.class);
        verify(fixture.imprestMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(FinanceGuideImprestEntity::getStatus)
                .containsExactly("pending_manager", "pending_manager");
        assertThat(captor.getAllValues()).extracting(FinanceGuideImprestEntity::getRequestNo)
                .containsExactly("GI-260706-00001", "GI-260706-00002");
        assertThat(captor.getAllValues()).extracting(FinanceGuideImprestEntity::getRequestedAmount)
                .containsExactly(new BigDecimal("800.00"), new BigDecimal("300.00"));
        verify(fixture.imprestMapper, times(2)).lockRequestNoGeneration(1L, "GI-260706-");
        verify(fixture.imprestMapper, times(2)).maxRequestNoSuffix(1L, "GI-260706-");
    }

    @Test
    void submitShouldPersistManualRequestedAmountAndOverrideCompanyMarkupRate() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(27);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "现付合计", "970.00"),
                arrangement(12L, "optional", "自费项目票", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                optionalLine(12L, "自费项目票", "200.00", "150.00", "10.00", "0")
        ));
        doAnswer(invocation -> {
            FinanceGuideImprestEntity entity = invocation.getArgument(0);
            entity.setId(7001L);
            return 1;
        }).when(fixture.imprestMapper).insert(any(FinanceGuideImprestEntity.class));

        fixture.service.submit(
                1L,
                new GuideImprestApplyRequest(1001L, 501L, new BigDecimal("100.00"), new BigDecimal("60"), "本次先批100"),
                "op01"
        );

        ArgumentCaptor<FinanceGuideImprestEntity> captor = ArgumentCaptor.forClass(FinanceGuideImprestEntity.class);
        verify(fixture.imprestMapper).insert(captor.capture());
        assertThat(captor.getValue().getRequestedAmount()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getCompanyMarkupRate()).isEqualByComparingTo("60.00");
        assertThat(captor.getValue().getOptionalDeductionAmount()).isEqualByComparingTo("648.00");
        assertThat(captor.getValue().getSuggestedImprestAmount()).isEqualByComparingTo("322.00");
    }

    @Test
    void approveShouldUseApplicationSnapshotCompanyMarkupRateWhenDetectingChanges() {
        TestFixture fixture = fixture();
        FinanceGuideImprestEntity current = existingImprest("pending_manager", "100.00", "0.00", "0.00");
        current.setCompanyMarkupRate(new BigDecimal("60.00"));
        current.setGuestCount(27);
        current.setCashCostAmount(new BigDecimal("970.00"));
        current.setOptionalDeductionAmount(new BigDecimal("648.00"));
        current.setCalculatedAmount(new BigDecimal("322.00"));
        current.setSuggestedImprestAmount(new BigDecimal("322.00"));
        current.setGuideTurnInAmount(BigDecimal.ZERO);
        when(fixture.imprestMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(27);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "现付合计", "970.00"),
                arrangement(12L, "optional", "自费项目票", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                optionalLine(12L, "自费项目票", "200.00", "150.00", "10.00", "0")
        ));
        when(fixture.configService.getCompanyMarkupRatePercent(1L)).thenReturn(new BigDecimal("70"));

        fixture.service.approve(
                1L,
                7001L,
                new GuideImprestDecisionRequest("同意"),
                "manager01",
                List.of("admin")
        );

        ArgumentCaptor<FinanceGuideImprestEntity> captor = ArgumentCaptor.forClass(FinanceGuideImprestEntity.class);
        verify(fixture.imprestMapper).update(captor.capture(), any());
        assertThat(captor.getValue().getApprovedAmount()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getStatus()).isEqualTo("manager_approved");
    }

    @Test
    void submitShouldAllowEmergencyApplicationWhenRemarkExplainsOverSuggestedTotal() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(10);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "1000.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        FinanceGuideImprestEntity existing = existingImprest("manager_approved", "800.00", "800.00", "0.00");
        when(fixture.imprestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));
        doAnswer(invocation -> {
            FinanceGuideImprestEntity entity = invocation.getArgument(0);
            entity.setId(7002L);
            return 1;
        }).when(fixture.imprestMapper).insert(any(FinanceGuideImprestEntity.class));

        fixture.service.submit(
                1L,
                new GuideImprestApplyRequest(1001L, 501L, new BigDecimal("300.00"), "现场临时现付应急备用金"),
                "op01"
        );

        ArgumentCaptor<FinanceGuideImprestEntity> captor = ArgumentCaptor.forClass(FinanceGuideImprestEntity.class);
        verify(fixture.imprestMapper).insert(captor.capture());
        assertThat(captor.getValue().getRequestedAmount()).isEqualByComparingTo("300.00");
        assertThat(captor.getValue().getSuggestedImprestAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void submitShouldRejectZeroRequestedAmount() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(10);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "1000.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> fixture.service.submit(
                1L,
                new GuideImprestApplyRequest(1001L, 501L, BigDecimal.ZERO, "0元不应生成审批单"),
                "op01"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("申请备用金必须大于0");
    }

    @Test
    void submitShouldRequireRemarkWhenApplicationMakesTotalExceedSuggestedAmount() {
        TestFixture fixture = fixture();
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(10);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "1000.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        FinanceGuideImprestEntity existing = existingImprest("manager_approved", "1000.00", "1000.00", "0.00");
        when(fixture.imprestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        assertThatThrownBy(() -> fixture.service.submit(
                1L,
                new GuideImprestApplyRequest(1001L, 501L, new BigDecimal("100.00"), " "),
                "op01"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("申请后累计备用金超过系统建议金额，请填写应急或特殊项目说明");
    }

    @Test
    void approveShouldRejectWhenCurrentArrangementDiffersFromApplicationSnapshot() {
        TestFixture fixture = fixture();
        FinanceGuideImprestEntity current = existingImprest("pending_manager", "800.00", "0.00", "0.00");
        current.setCashCostAmount(new BigDecimal("800.00"));
        current.setOptionalDeductionAmount(BigDecimal.ZERO);
        current.setCalculatedAmount(new BigDecimal("800.00"));
        current.setSuggestedImprestAmount(new BigDecimal("800.00"));
        current.setGuideTurnInAmount(BigDecimal.ZERO);
        current.setGuestCount(10);
        when(fixture.imprestMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(10);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通改成挂账", "0.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> fixture.service.approve(
                1L,
                7001L,
                new GuideImprestDecisionRequest("同意"),
                "manager01",
                List.of("admin")
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("团队安排已变化：")
                .hasMessageContaining("现付总成本从 ¥800.00 变为 ¥0.00")
                .hasMessageContaining("建议备用金从 ¥800.00 变为 ¥0.00")
                .hasMessageContaining("请计调重新计算备用金后再审批");
    }

    @Test
    void approveShouldIdentifyChangedCashCostLineWhenSnapshotSourceIsInactive() {
        TestFixture fixture = fixture();
        FinanceGuideImprestEntity current = existingImprest("pending_manager", "2105.00", "0.00", "0.00");
        current.setCashCostAmount(new BigDecimal("2105.00"));
        current.setOptionalDeductionAmount(BigDecimal.ZERO);
        current.setCalculatedAmount(new BigDecimal("2105.00"));
        current.setSuggestedImprestAmount(new BigDecimal("2105.00"));
        current.setGuideTurnInAmount(BigDecimal.ZERO);
        current.setGuestCount(10);
        when(fixture.imprestMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(10);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(22L, "other", "矿泉水及杂支", "120.00"),
                arrangement(46L, "scenic", "西湖游船", "1485.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(fixture.calcLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                calcLine(22L, null, "cash_cost", "other", "矿泉水及杂支", "120.00", 1),
                calcLine(44L, null, "cash_cost", "meal", "楼外楼团队餐", "500.00", 2),
                calcLine(46L, null, "cash_cost", "scenic", "西湖游船", "1485.00", 3)
        ));

        assertThatThrownBy(() -> fixture.service.approve(
                1L,
                7001L,
                new GuideImprestDecisionRequest("同意"),
                "manager01",
                List.of("admin")
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("楼外楼团队餐：申请时现付 ¥500.00，当前安排已删除或不再生效")
                .hasMessageContaining("现付总成本从 ¥2105.00 变为 ¥1605.00")
                .hasMessageContaining("请计调重新计算备用金后再审批");
    }

    @Test
    void registerPaymentShouldRejectWhenCurrentArrangementDiffersFromApprovedSnapshot() {
        TestFixture fixture = fixture();
        FinanceGuideImprestEntity current = existingImprest("manager_approved", "800.00", "800.00", "0.00");
        current.setCashCostAmount(new BigDecimal("800.00"));
        current.setOptionalDeductionAmount(BigDecimal.ZERO);
        current.setCalculatedAmount(new BigDecimal("800.00"));
        current.setSuggestedImprestAmount(new BigDecimal("800.00"));
        current.setGuideTurnInAmount(BigDecimal.ZERO);
        current.setGuestCount(10);
        when(fixture.imprestMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(10);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通追加现付", "1000.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> fixture.service.registerPayment(
                1L,
                7001L,
                new GuideImprestPaymentRequest(new BigDecimal("800.00"), LocalDate.of(2026, 7, 6), "bank", "工行", "付款"),
                "finance01"
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("团队安排已变化：")
                .hasMessageContaining("现付总成本从 ¥800.00 变为 ¥1000.00")
                .hasMessageContaining("建议备用金从 ¥800.00 变为 ¥1000.00")
                .hasMessageContaining("请作废旧备用金申请并重新提交后再付款");
    }

    @Test
    void cancelShouldRestoreAvailableAuthorizationForUnpaidApplication() {
        TestFixture fixture = fixture();
        FinanceGuideImprestEntity current = existingImprest("manager_approved", "800.00", "800.00", "0.00");
        current.setPaidAmount(BigDecimal.ZERO);
        when(fixture.imprestMapper.selectOne(any(Wrapper.class))).thenReturn(current);

        fixture.service.cancel(1L, 7001L, "录错现付金额", "op01");

        ArgumentCaptor<FinanceGuideImprestEntity> captor = ArgumentCaptor.forClass(FinanceGuideImprestEntity.class);
        verify(fixture.imprestMapper).update(captor.capture(), any());
        assertThat(captor.getValue().getStatus()).isEqualTo("cancelled");
        assertThat(captor.getValue().getCancelReason()).isEqualTo("录错现付金额");
    }

    @Test
    void registerPaymentShouldGeneratePaymentNoWithDatabaseLock() {
        TestFixture fixture = fixture();
        FinanceGuideImprestEntity current = existingImprest("manager_approved", "800.00", "800.00", "0.00");
        current.setCompanyMarkupRate(new BigDecimal("70.00"));
        when(fixture.imprestMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(fixture.orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(10);
        when(fixture.arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                arrangement(11L, "traffic", "大交通现付", "800.00")
        ));
        when(fixture.priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(fixture.paymentMapper.maxPaymentNoSuffix(any(), any())).thenReturn(12);

        fixture.service.registerPayment(
                1L,
                7001L,
                new GuideImprestPaymentRequest(new BigDecimal("800.00"), LocalDate.of(2026, 7, 6), "bank", "工行", "付款"),
                "finance01"
        );

        ArgumentCaptor<FinanceGuideImprestPaymentEntity> paymentCaptor = ArgumentCaptor.forClass(FinanceGuideImprestPaymentEntity.class);
        verify(fixture.paymentMapper).insert(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPaymentNo()).isEqualTo("GIP-260706-00013");
        verify(fixture.paymentMapper).lockPaymentNoGeneration(1L, "GIP-260706-");
        verify(fixture.paymentMapper).maxPaymentNoSuffix(1L, "GIP-260706-");
    }

    @Test
    void registerPaymentShouldRejectBeforeManagerApproval() {
        TestFixture fixture = fixture();
        FinanceGuideImprestEntity current = new FinanceGuideImprestEntity();
        current.setId(7001L);
        current.setTenantId(1L);
        current.setStatus("pending_manager");
        current.setRequestedAmount(new BigDecimal("800.00"));
        current.setPaidAmount(BigDecimal.ZERO);
        when(fixture.imprestMapper.selectOne(any(Wrapper.class))).thenReturn(current);

        assertThatThrownBy(() -> fixture.service.registerPayment(
                1L,
                7001L,
                new GuideImprestPaymentRequest(new BigDecimal("800.00"), LocalDate.of(2026, 7, 6), "bank", "工行", "付款"),
                "finance01"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("导游备用金需总经理审批通过后才能付款");
    }

    private static TestFixture fixture() {
        FinanceGuideImprestMapper imprestMapper = mock(FinanceGuideImprestMapper.class);
        FinanceGuideImprestCalcLineMapper calcLineMapper = mock(FinanceGuideImprestCalcLineMapper.class);
        FinanceGuideImprestPaymentMapper paymentMapper = mock(FinanceGuideImprestPaymentMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementPriceLineMapper priceLineMapper = mock(DispatchTeamArrangementPriceLineMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        GuideImprestConfigService configService = mock(GuideImprestConfigService.class);

        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team());
        when(teamGuideMapper.selectOne(any(Wrapper.class))).thenReturn(guide());
        when(configService.getCompanyMarkupRatePercent(1L)).thenReturn(new BigDecimal("70"));

        FinanceGuideImprestService service = new FinanceGuideImprestService(
                imprestMapper,
                calcLineMapper,
                paymentMapper,
                teamMapper,
                teamGuideMapper,
                arrangementMapper,
                priceLineMapper,
                orderMapper,
                configService,
                FIXED_CLOCK
        );
        return new TestFixture(
                service,
                imprestMapper,
                calcLineMapper,
                paymentMapper,
                teamMapper,
                teamGuideMapper,
                arrangementMapper,
                priceLineMapper,
                orderMapper,
                configService
        );
    }

    private static SalesTeamEntity team() {
        SalesTeamEntity entity = new SalesTeamEntity();
        entity.setId(1001L);
        entity.setTenantId(1L);
        entity.setTeamNo("CS-DJ-260706A");
        entity.setTeamType("sanpin");
        entity.setBusinessType("迪士尼");
        entity.setDepartureDate(LocalDate.of(2026, 7, 10));
        entity.setDepartmentId(8L);
        entity.setDepartmentName("销售一部");
        entity.setOperatorEmployeeId(88L);
        entity.setOperatorEmployeeName("张计调");
        return entity;
    }

    private static DispatchTeamGuideEntity guide() {
        DispatchTeamGuideEntity entity = new DispatchTeamGuideEntity();
        entity.setId(301L);
        entity.setTenantId(1L);
        entity.setTeamId(1001L);
        entity.setTeamNo("CS-DJ-260706A");
        entity.setGuideId(501L);
        entity.setGuideName("姚导");
        entity.setGuideMobile("13800000000");
        entity.setStatus("active");
        return entity;
    }

    private static DispatchTeamArrangementEntity arrangement(Long id, String type, String itemName, String cashAmount) {
        DispatchTeamArrangementEntity entity = new DispatchTeamArrangementEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setTeamId(1001L);
        entity.setTeamNo("CS-DJ-260706A");
        entity.setArrangementType(type);
        entity.setItemName(itemName);
        entity.setCashAmount(new BigDecimal(cashAmount));
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private static FinanceGuideImprestEntity existingImprest(
            String status,
            String requestedAmount,
            String approvedAmount,
            String paidAmount
    ) {
        FinanceGuideImprestEntity entity = new FinanceGuideImprestEntity();
        entity.setId(7001L);
        entity.setTenantId(1L);
        entity.setTeamId(1001L);
        entity.setTeamNo("CS-DJ-260706A");
        entity.setGuideId(501L);
        entity.setGuideName("姚导");
        entity.setGuideMobile("13800000000");
        entity.setStatus(status);
        entity.setRequestedAmount(new BigDecimal(requestedAmount));
        entity.setApprovedAmount(new BigDecimal(approvedAmount));
        entity.setPaidAmount(new BigDecimal(paidAmount));
        entity.setBalanceAmount(new BigDecimal(approvedAmount).subtract(new BigDecimal(paidAmount)));
        entity.setCashCostAmount(new BigDecimal(requestedAmount));
        entity.setOptionalDeductionAmount(BigDecimal.ZERO);
        entity.setCalculatedAmount(new BigDecimal(requestedAmount));
        entity.setSuggestedImprestAmount(new BigDecimal(requestedAmount));
        entity.setGuideTurnInAmount(BigDecimal.ZERO);
        entity.setGuestCount(10);
        return entity;
    }

    private static DispatchTeamArrangementPriceLineEntity optionalLine(
            Long arrangementId,
            String projectName,
            String salePrice,
            String costPrice,
            String guideCommissionAmount,
            String guideCommissionRate
    ) {
        DispatchTeamArrangementPriceLineEntity entity = new DispatchTeamArrangementPriceLineEntity();
        entity.setId(arrangementId * 10);
        entity.setTenantId(1L);
        entity.setArrangementId(arrangementId);
        entity.setTeamId(1001L);
        entity.setProjectName(projectName);
        entity.setSalePrice(new BigDecimal(salePrice));
        entity.setCostPrice(new BigDecimal(costPrice));
        entity.setGuideCommissionAmount(new BigDecimal(guideCommissionAmount));
        entity.setGuideCommissionRate(new BigDecimal(guideCommissionRate));
        entity.setIsDeleted(false);
        return entity;
    }

    private static FinanceGuideImprestCalcLineEntity calcLine(
            Long sourceArrangementId,
            Long sourcePriceLineId,
            String lineType,
            String arrangementType,
            String itemName,
            String amount,
            Integer sortOrder
    ) {
        FinanceGuideImprestCalcLineEntity entity = new FinanceGuideImprestCalcLineEntity();
        entity.setId(sourceArrangementId * 100 + sortOrder);
        entity.setTenantId(1L);
        entity.setImprestId(7001L);
        entity.setTeamId(1001L);
        entity.setSourceArrangementId(sourceArrangementId);
        entity.setSourcePriceLineId(sourcePriceLineId);
        entity.setLineType(lineType);
        entity.setArrangementType(arrangementType);
        entity.setItemName(itemName);
        entity.setAmount(new BigDecimal(amount));
        entity.setSortOrder(sortOrder);
        entity.setIsDeleted(false);
        return entity;
    }

    private record TestFixture(
            FinanceGuideImprestService service,
            FinanceGuideImprestMapper imprestMapper,
            FinanceGuideImprestCalcLineMapper calcLineMapper,
            FinanceGuideImprestPaymentMapper paymentMapper,
            SalesTeamMapper teamMapper,
            DispatchTeamGuideMapper teamGuideMapper,
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementPriceLineMapper priceLineMapper,
            SalesBookingOrderMapper orderMapper,
            GuideImprestConfigService configService
    ) {
    }
}
