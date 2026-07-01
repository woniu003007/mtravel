package com.mtravel.platform.dispatch.teamarrangement.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementPriceLineRequest;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementFlowRecordEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementOrderAllocationEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementPriceLineEntity;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementFlowRecordMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementOrderAllocationMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementPriceLineMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 正式团队安排成本服务测试。
 *
 * <p>这些用例固定旧系统已实测确认的成本分摊和导游报账同步规则。团队安排成本会支撑
 * 后续应付、导游报账、计调审核、财务审核和利润统计，不能只测试页面能保存。</p>
 */
class DispatchTeamArrangementServiceTest {

    @Test
    void saveShouldSplitMultiOrderAverageByOrderAndKeepBatchNo() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementPriceLineMapper priceLineMapper = mock(DispatchTeamArrangementPriceLineMapper.class);
        DispatchTeamArrangementOrderAllocationMapper allocationMapper = mock(DispatchTeamArrangementOrderAllocationMapper.class);
        DispatchTeamArrangementFlowRecordMapper flowMapper = mock(DispatchTeamArrangementFlowRecordMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        DispatchTeamArrangementService service = service(arrangementMapper, priceLineMapper, allocationMapper, flowMapper, orderMapper);
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                order(101L, 14),
                order(102L, 4),
                order(103L, 3)
        ));
        when(arrangementMapper.insert(any(DispatchTeamArrangementEntity.class))).thenAnswer(assignId(9001L));
        ArgumentCaptor<DispatchTeamArrangementEntity> arrangementCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementEntity.class);
        ArgumentCaptor<DispatchTeamArrangementOrderAllocationEntity> allocationCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementOrderAllocationEntity.class);

        service.save(21L, multiOrderRequest("by_order"), 1L, "dispatcher");

        verify(arrangementMapper, org.mockito.Mockito.times(3)).insert(arrangementCaptor.capture());
        verify(allocationMapper, org.mockito.Mockito.times(3)).insert(allocationCaptor.capture());
        assertThat(arrangementCaptor.getAllValues())
                .extracting(DispatchTeamArrangementEntity::getTotalAmount)
                .containsExactly(
                        BigDecimal.valueOf(100).setScale(2),
                        BigDecimal.valueOf(100).setScale(2),
                        BigDecimal.valueOf(100).setScale(2)
                );
        assertThat(allocationCaptor.getAllValues())
                .extracting(DispatchTeamArrangementOrderAllocationEntity::getOrderId)
                .containsExactly(101L, 102L, 103L);
        assertThat(allocationCaptor.getAllValues())
                .extracting(DispatchTeamArrangementOrderAllocationEntity::getAllocationAmount)
                .containsExactly(
                        BigDecimal.valueOf(100).setScale(2),
                        BigDecimal.valueOf(100).setScale(2),
                        BigDecimal.valueOf(100).setScale(2)
                );
        assertThat(allocationCaptor.getAllValues())
                .extracting(DispatchTeamArrangementOrderAllocationEntity::getSplitBatchNo)
                .doesNotContainNull();
    }

    @Test
    void saveShouldSplitMultiOrderAverageByPeople() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementPriceLineMapper priceLineMapper = mock(DispatchTeamArrangementPriceLineMapper.class);
        DispatchTeamArrangementOrderAllocationMapper allocationMapper = mock(DispatchTeamArrangementOrderAllocationMapper.class);
        DispatchTeamArrangementFlowRecordMapper flowMapper = mock(DispatchTeamArrangementFlowRecordMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        DispatchTeamArrangementService service = service(arrangementMapper, priceLineMapper, allocationMapper, flowMapper, orderMapper);
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                order(101L, 14),
                order(102L, 4),
                order(103L, 3)
        ));
        when(arrangementMapper.insert(any(DispatchTeamArrangementEntity.class))).thenAnswer(assignId(9101L));
        ArgumentCaptor<DispatchTeamArrangementOrderAllocationEntity> allocationCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementOrderAllocationEntity.class);

        service.save(21L, multiOrderRequest("by_people"), 1L, "dispatcher");

        verify(allocationMapper, org.mockito.Mockito.times(3)).insert(allocationCaptor.capture());
        assertThat(allocationCaptor.getAllValues())
                .extracting(DispatchTeamArrangementOrderAllocationEntity::getAllocationAmount)
                .containsExactly(
                        BigDecimal.valueOf(200).setScale(2),
                        BigDecimal.valueOf(57.14).setScale(2),
                        BigDecimal.valueOf(42.86).setScale(2)
                );
    }

    @Test
    void saveShouldRejectNoGuideReportWithCashAmount() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementService service = service(
                arrangementMapper,
                mock(DispatchTeamArrangementPriceLineMapper.class),
                mock(DispatchTeamArrangementOrderAllocationMapper.class),
                mock(DispatchTeamArrangementFlowRecordMapper.class),
                mock(SalesBookingOrderMapper.class)
        );

        assertThatThrownBy(() -> service.save(21L, singleOrderRequest(true, BigDecimal.TEN), 1L, "dispatcher"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("现结金额须为0");

        verify(arrangementMapper, never()).insert(any(DispatchTeamArrangementEntity.class));
    }

    @Test
    void saveShouldCreateGuideReportAndOperatorAuditFlowWhenNoGuideReport() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementFlowRecordMapper flowMapper = mock(DispatchTeamArrangementFlowRecordMapper.class);
        DispatchTeamArrangementService service = service(
                arrangementMapper,
                mock(DispatchTeamArrangementPriceLineMapper.class),
                mock(DispatchTeamArrangementOrderAllocationMapper.class),
                flowMapper,
                mock(SalesBookingOrderMapper.class)
        );
        when(arrangementMapper.insert(any(DispatchTeamArrangementEntity.class))).thenAnswer(assignId(9201L));
        ArgumentCaptor<DispatchTeamArrangementFlowRecordEntity> flowCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementFlowRecordEntity.class);

        service.save(21L, singleOrderRequest(true, BigDecimal.ZERO), 1L, "dispatcher");

        verify(flowMapper, org.mockito.Mockito.times(2)).insert(flowCaptor.capture());
        assertThat(flowCaptor.getAllValues())
                .extracting(DispatchTeamArrangementFlowRecordEntity::getFlowType)
                .containsExactly("guide_report", "operator_audit");
        assertThat(flowCaptor.getAllValues())
                .extracting(DispatchTeamArrangementFlowRecordEntity::getSyncSource)
                .containsOnly("no_guide_report");
    }

    @Test
    void saveShouldPersistCategorySpecificFieldsForNonTrafficArrangements() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementService service = service(
                arrangementMapper,
                mock(DispatchTeamArrangementPriceLineMapper.class),
                mock(DispatchTeamArrangementOrderAllocationMapper.class),
                mock(DispatchTeamArrangementFlowRecordMapper.class),
                mock(SalesBookingOrderMapper.class)
        );
        when(arrangementMapper.insert(any(DispatchTeamArrangementEntity.class))).thenAnswer(assignId(9401L));
        ArgumentCaptor<DispatchTeamArrangementEntity> arrangementCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementEntity.class);

        service.save(21L, hotelRequest(), 1L, "dispatcher");

        verify(arrangementMapper).insert(arrangementCaptor.capture());
        DispatchTeamArrangementEntity saved = arrangementCaptor.getValue();
        assertThat(saved.getArrangementType()).isEqualTo("hotel");
        assertThat(saved.getSettlementType()).isEqualTo("credit");
        assertThat(saved.getMealType()).isEqualTo("自助早");
        assertThat(saved.getFundIncluded()).isEqualTo("含");
        assertThat(saved.getConfirmed()).isTrue();
        assertThat(saved.getConfirmationNo()).isEqualTo("CN-001");
        assertThat(saved.getGuideId()).isEqualTo(77L);
        assertThat(saved.getGuideName()).isEqualTo("李导");
    }

    @Test
    void saveShouldAllowShoppingWithOrderAllocationAndNoGuideReportRules() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementOrderAllocationMapper allocationMapper = mock(DispatchTeamArrangementOrderAllocationMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        DispatchTeamArrangementService service = service(
                arrangementMapper,
                mock(DispatchTeamArrangementPriceLineMapper.class),
                allocationMapper,
                mock(DispatchTeamArrangementFlowRecordMapper.class),
                orderMapper
        );
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(order(101L, 14)));
        when(arrangementMapper.insert(any(DispatchTeamArrangementEntity.class))).thenAnswer(assignId(9501L));
        ArgumentCaptor<DispatchTeamArrangementOrderAllocationEntity> allocationCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementOrderAllocationEntity.class);

        service.save(21L, shoppingRequest(), 1L, "dispatcher");

        verify(allocationMapper).insert(allocationCaptor.capture());
        DispatchTeamArrangementOrderAllocationEntity allocation = allocationCaptor.getValue();
        assertThat(allocation.getAllocationScope()).isEqualTo("order");
        assertThat(allocation.getOrderId()).isEqualTo(101L);
        assertThat(allocation.getAllocationAmount()).isEqualByComparingTo("1200.00");
    }

    @Test
    void deleteShouldRejectArrangementWithFlowRecords() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementFlowRecordMapper flowMapper = mock(DispatchTeamArrangementFlowRecordMapper.class);
        DispatchTeamArrangementService service = service(
                arrangementMapper,
                mock(DispatchTeamArrangementPriceLineMapper.class),
                mock(DispatchTeamArrangementOrderAllocationMapper.class),
                flowMapper,
                mock(SalesBookingOrderMapper.class)
        );
        when(arrangementMapper.selectOne(any(Wrapper.class))).thenReturn(existingArrangement(9301L));
        when(flowMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(21L, 9301L, 1L, "dispatcher"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已进入人工导游报账或审核流程");

        verify(arrangementMapper, never()).update(any(DispatchTeamArrangementEntity.class), any(Wrapper.class));
    }

    @Test
    void deleteShouldAllowNoGuideReportAutoSyncedFlowRecords() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementPriceLineMapper priceLineMapper = mock(DispatchTeamArrangementPriceLineMapper.class);
        DispatchTeamArrangementOrderAllocationMapper allocationMapper = mock(DispatchTeamArrangementOrderAllocationMapper.class);
        DispatchTeamArrangementFlowRecordMapper flowMapper = mock(DispatchTeamArrangementFlowRecordMapper.class);
        DispatchTeamArrangementService service = service(
                arrangementMapper,
                priceLineMapper,
                allocationMapper,
                flowMapper,
                mock(SalesBookingOrderMapper.class)
        );
        when(arrangementMapper.selectOne(any(Wrapper.class))).thenReturn(existingArrangement(9302L));
        when(flowMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        ArgumentCaptor<Wrapper> flowLockQueryCaptor = ArgumentCaptor.forClass(Wrapper.class);

        service.delete(21L, 9302L, 1L, "dispatcher");

        verify(flowMapper).selectCount(flowLockQueryCaptor.capture());
        assertThat(flowLockQueryCaptor.getValue().getCustomSqlSegment())
                .contains("sync_source");
        assertThat(((AbstractWrapper<?, ?, ?>) flowLockQueryCaptor.getValue()).getParamNameValuePairs().values())
                .contains("no_guide_report");
        verify(arrangementMapper).update(any(DispatchTeamArrangementEntity.class), any(Wrapper.class));
        verify(priceLineMapper).update(any(DispatchTeamArrangementPriceLineEntity.class), any(Wrapper.class));
        verify(allocationMapper).update(any(DispatchTeamArrangementOrderAllocationEntity.class), any(Wrapper.class));
        verify(flowMapper).update(any(DispatchTeamArrangementFlowRecordEntity.class), any(Wrapper.class));
    }

    @Test
    void listShouldAllowDeleteWhenOnlyNoGuideReportAutoSyncedFlowsExist() {
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementPriceLineMapper priceLineMapper = mock(DispatchTeamArrangementPriceLineMapper.class);
        DispatchTeamArrangementOrderAllocationMapper allocationMapper = mock(DispatchTeamArrangementOrderAllocationMapper.class);
        DispatchTeamArrangementFlowRecordMapper flowMapper = mock(DispatchTeamArrangementFlowRecordMapper.class);
        DispatchTeamArrangementService service = service(
                arrangementMapper,
                priceLineMapper,
                allocationMapper,
                flowMapper,
                mock(SalesBookingOrderMapper.class)
        );
        when(arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existingArrangement(9303L)));
        when(priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(allocationMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(flowMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        ArgumentCaptor<Wrapper> flowListQueryCaptor = ArgumentCaptor.forClass(Wrapper.class);

        assertThat(service.list(21L, null, 1L))
                .singleElement()
                .extracting("canDelete", "deleteDisabledReason")
                .containsExactly(true, null);
        verify(flowMapper).selectList(flowListQueryCaptor.capture());
        assertThat(flowListQueryCaptor.getValue().getCustomSqlSegment())
                .contains("sync_source");
        assertThat(((AbstractWrapper<?, ?, ?>) flowListQueryCaptor.getValue()).getParamNameValuePairs().values())
                .contains("no_guide_report");
    }

    private DispatchTeamArrangementService service(
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamArrangementPriceLineMapper priceLineMapper,
            DispatchTeamArrangementOrderAllocationMapper allocationMapper,
            DispatchTeamArrangementFlowRecordMapper flowMapper,
            SalesBookingOrderMapper orderMapper
    ) {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(21L);
        team.setTenantId(1L);
        team.setProductId(88L);
        team.setTeamNo("CS-SP-BK-260701A");
        team.setTeamType("sanpin");
        team.setBusinessType("散拼");
        team.setDepartmentId(7L);
        team.setDepartmentName("计调部");
        team.setOperatorEmployeeId(9L);
        team.setOperatorEmployeeName("王计调");
        team.setDepartureDate(LocalDate.of(2026, 7, 1));
        team.setIsDeleted(false);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        return new DispatchTeamArrangementService(
                arrangementMapper,
                priceLineMapper,
                allocationMapper,
                flowMapper,
                teamMapper,
                orderMapper
        );
    }

    private TeamArrangementSaveRequest multiOrderRequest(String splitMode) {
        return new TeamArrangementSaveRequest(
                null,
                "traffic",
                "飞机-成人",
                "第1天｜飞机｜杭州｜北京｜国航供应商",
                "multi_order_average",
                List.of(101L, 102L, 103L),
                splitMode,
                "第1天",
                null,
                "杭州",
                "北京",
                0,
                null,
                501L,
                "国航供应商",
                "飞机",
                null,
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(300),
                BigDecimal.ZERO,
                BigDecimal.valueOf(300),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(300),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                List.of(priceLine(BigDecimal.valueOf(100), BigDecimal.valueOf(3))),
                "多订单均摊"
        );
    }

    private TeamArrangementSaveRequest singleOrderRequest(boolean noGuideReport, BigDecimal cashAmount) {
        BigDecimal total = BigDecimal.valueOf(300);
        return new TeamArrangementSaveRequest(
                null,
                "traffic",
                "飞机-成人",
                "第1天｜飞机｜杭州｜北京｜国航供应商",
                "group_order_average",
                List.of(),
                null,
                "第1天",
                null,
                "杭州",
                "北京",
                0,
                null,
                501L,
                "国航供应商",
                "飞机",
                null,
                null,
                null,
                null,
                null,
                total,
                cashAmount,
                total.subtract(cashAmount),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                total,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                noGuideReport,
                List.of(priceLine(BigDecimal.valueOf(100), BigDecimal.valueOf(3))),
                "单订单"
        );
    }

    private TeamArrangementSaveRequest hotelRequest() {
        return new TeamArrangementSaveRequest(
                null,
                "hotel",
                "之江饭店",
                "第1天入住",
                "group_order_average",
                List.of(),
                null,
                "第1天",
                "第2天",
                null,
                null,
                1,
                "之江饭店",
                601L,
                "之江饭店供应商",
                null,
                null,
                null,
                null,
                88L,
                "王房调",
                BigDecimal.valueOf(800),
                BigDecimal.ZERO,
                BigDecimal.valueOf(800),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(800),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                List.of(priceLine(BigDecimal.valueOf(400), BigDecimal.valueOf(2))),
                "酒店备注",
                "credit",
                "自助早",
                "含",
                true,
                "CN-001",
                77L,
                "李导"
        );
    }

    private TeamArrangementSaveRequest shoppingRequest() {
        return new TeamArrangementSaveRequest(
                null,
                "shopping",
                "丝绸店",
                "购物消费",
                "group_order_average",
                List.of(101L),
                null,
                "第2天",
                null,
                null,
                null,
                0,
                "丝绸店",
                701L,
                "丝绸店供应商",
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(1200),
                BigDecimal.ZERO,
                BigDecimal.valueOf(1200),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(14),
                false,
                List.of(priceLine(BigDecimal.ZERO, BigDecimal.ONE)),
                "购物备注",
                "credit",
                null,
                null,
                false,
                null,
                null,
                null
        );
    }

    private TeamArrangementPriceLineRequest priceLine(BigDecimal unitPrice, BigDecimal quantity) {
        return new TeamArrangementPriceLineRequest(
                301L,
                "成人",
                unitPrice,
                quantity,
                unitPrice.multiply(quantity),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                1,
                "价格备注"
        );
    }

    private SalesBookingOrderEntity order(Long orderId, int guestCount) {
        SalesBookingOrderEntity entity = new SalesBookingOrderEntity();
        entity.setId(orderId);
        entity.setTenantId(1L);
        entity.setTeamId(21L);
        entity.setOrderNo("O" + orderId);
        entity.setCustomerId(700L + orderId);
        entity.setCustomerName("客户" + orderId);
        entity.setGuestCount(guestCount);
        entity.setStatus("confirmed");
        entity.setIsDeleted(false);
        return entity;
    }

    private DispatchTeamArrangementEntity existingArrangement(Long id) {
        DispatchTeamArrangementEntity entity = new DispatchTeamArrangementEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setTeamId(21L);
        entity.setArrangementType("traffic");
        entity.setIsDeleted(false);
        return entity;
    }

    private Answer<Integer> assignId(long firstId) {
        final long[] current = {firstId};
        return invocation -> {
            DispatchTeamArrangementEntity entity = invocation.getArgument(0);
            entity.setId(current[0]++);
            return 1;
        };
    }
}
