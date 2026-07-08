package com.mtravel.platform.sales.ordertransfer.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderChargeLineEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderChargeLineMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderGuestMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.ordertransfer.dto.SalesOrderTransferMergeRequest;
import com.mtravel.platform.sales.ordertransfer.dto.SalesOrderTransferMoveRequest;
import com.mtravel.platform.sales.ordertransfer.dto.SalesOrderTransferRemarkRequest;
import com.mtravel.platform.sales.ordertransfer.entity.SalesOrderTransferLogEntity;
import com.mtravel.platform.sales.ordertransfer.mapper.SalesOrderTransferLogMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamPriceMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售订单拼团和转团服务测试。
 *
 * <p>拼团、转团会改变订单与团队的业务关系，必须保留独立流转日志并刷新团队人数。
 * 测试固定旧系统团队操作页按钮的核心写入语义，避免只做前端按钮提示。</p>
 */
class SalesOrderTransferServiceTest {

    @Test
    void mergeOrdersShouldMergeMultipleSourceOrdersToOneTargetTeam() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferLogMapper logMapper = mock(SalesOrderTransferLogMapper.class);
        SalesOrderTransferService service = new SalesOrderTransferService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                logMapper
        );
        SalesTeamEntity sourceTeam = team(1001L, "CS-SP-BK-260625A");
        SalesTeamEntity targetTeam = team(1002L, "CS-SP-BK-260626A");
        SalesBookingOrderEntity sourceOrder = order(2001L, 1001L, "SO-260625-001");
        sourceOrder.setTravelDescription("宁波方特二日游");
        SalesBookingOrderEntity sourceOrder2 = order(2002L, 1001L, "SO-260625-002");
        sourceOrder2.setTravelDescription("宁波方特二日游");
        sourceOrder2.setAdultCount(1);
        sourceOrder2.setGuestCount(1);
        sourceOrder2.setReceivableAmount(new BigDecimal("3000.00"));
        sourceOrder2.setReceivedAmount(new BigDecimal("300.00"));
        sourceOrder2.setBalanceAmount(new BigDecimal("2700.00"));
        SalesBookingOrderGuestEntity guest1 = guest(sourceOrder, 501L, "李四", "adult");
        SalesBookingOrderGuestEntity guest2 = guest(sourceOrder, 502L, "王五", "adult");
        SalesBookingOrderGuestEntity guest3 = guest(sourceOrder2, 503L, "赵六", "adult");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(
                sourceTeam,
                targetTeam,
                sourceTeam,
                targetTeam
        );
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(sourceOrder, sourceOrder2);
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(guest1, guest2), List.of(guest3));
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(chargeLine(sourceOrder)), List.of(chargeLine(sourceOrder2)));
        when(orderMapper.update(any(SalesBookingOrderEntity.class), any(UpdateWrapper.class))).thenReturn(1);
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(0);
        when(orderMapper.sumGuestCountByTeam(1L, 1002L)).thenReturn(3);
        AtomicLong childId = new AtomicLong(3000L);
        org.mockito.Mockito.doAnswer(invocation -> {
            SalesBookingOrderEntity child = invocation.getArgument(0);
            child.setId(childId.incrementAndGet());
            return 1;
        }).when(orderMapper).insert(any(SalesBookingOrderEntity.class));

        service.mergeOrders(
                1001L,
                new SalesOrderTransferMergeRequest(
                        List.of(2001L, 2002L),
                        1002L,
                        true,
                        "统一备注",
                        List.of(new SalesOrderTransferRemarkRequest(2001L, 1002L, "目标团备注"))
                ),
                1L,
                "admin"
        );

        ArgumentCaptor<SalesBookingOrderEntity> childCaptor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper, org.mockito.Mockito.times(2)).insert(childCaptor.capture());
        List<SalesBookingOrderEntity> children = childCaptor.getAllValues();
        assertThat(children).extracting(SalesBookingOrderEntity::getTeamId).containsExactly(1002L, 1002L);
        assertThat(children).extracting(SalesBookingOrderEntity::getOrderNo)
                .containsExactly("SO-260625-001-PT-CS-SP-BK-260626A", "SO-260625-002-PT-CS-SP-BK-260626A");
        assertThat(children).extracting(SalesBookingOrderEntity::getOriginalOrderInfo)
                .containsExactly(
                        "[四]2026-06-25 宁波方特二日游 杭州百缘旅行社",
                        "[四]2026-06-25 宁波方特二日游 杭州百缘旅行社"
                );
        assertThat(children).extracting(SalesBookingOrderEntity::getOrderRole)
                .containsExactly("merge_child", "merge_child");
        assertThat(children).extracting(SalesBookingOrderEntity::getGuestCount).containsExactly(2, 1);
        assertThat(children).extracting(SalesBookingOrderEntity::getAdultCount).containsExactly(2, 1);
        assertThat(children).extracting(SalesBookingOrderEntity::getReceivableAmount)
                .containsExactly(new BigDecimal("6000.00"), new BigDecimal("3000.00"));
        assertThat(children).extracting(SalesBookingOrderEntity::getReceivedAmount)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("0.00"));
        assertThat(children).extracting(SalesBookingOrderEntity::getBalanceAmount)
                .containsExactly(new BigDecimal("6000.00"), new BigDecimal("3000.00"));

        ArgumentCaptor<SalesBookingOrderEntity> orderUpdateCaptor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper, org.mockito.Mockito.times(2)).update(orderUpdateCaptor.capture(), any(UpdateWrapper.class));
        assertThat(orderUpdateCaptor.getAllValues()).extracting(SalesBookingOrderEntity::getOrderRole)
                .containsExactly("merge_source", "merge_source");

        ArgumentCaptor<SalesBookingOrderChargeLineEntity> chargeCaptor = ArgumentCaptor.forClass(SalesBookingOrderChargeLineEntity.class);
        verify(chargeLineMapper, org.mockito.Mockito.times(2)).insert(chargeCaptor.capture());
        assertThat(chargeCaptor.getAllValues()).extracting(SalesBookingOrderChargeLineEntity::getOrderId)
                .containsExactly(3001L, 3002L);
        assertThat(chargeCaptor.getAllValues()).extracting(SalesBookingOrderChargeLineEntity::getTeamId)
                .containsExactly(1002L, 1002L);
        assertThat(chargeCaptor.getAllValues()).extracting(SalesBookingOrderChargeLineEntity::getAmount)
                .containsExactly(new BigDecimal("6000.00"), new BigDecimal("3000.00"));

        ArgumentCaptor<SalesBookingOrderGuestEntity> guestCaptor = ArgumentCaptor.forClass(SalesBookingOrderGuestEntity.class);
        verify(guestMapper, org.mockito.Mockito.times(3)).insert(guestCaptor.capture());
        assertThat(guestCaptor.getAllValues()).extracting(SalesBookingOrderGuestEntity::getOrderId)
                .containsExactly(3001L, 3001L, 3002L);
        assertThat(guestCaptor.getAllValues()).extracting(SalesBookingOrderGuestEntity::getTeamId)
                .containsExactly(1002L, 1002L, 1002L);
        assertThat(guestCaptor.getAllValues()).extracting(SalesBookingOrderGuestEntity::getGuestName)
                .containsExactly("李四", "王五", "赵六");

        ArgumentCaptor<SalesOrderTransferLogEntity> logCaptor = ArgumentCaptor.forClass(SalesOrderTransferLogEntity.class);
        verify(logMapper, org.mockito.Mockito.times(2)).insert(logCaptor.capture());
        assertThat(logCaptor.getAllValues()).extracting(SalesOrderTransferLogEntity::getTransferType)
                .containsExactly("merge", "merge");
        assertThat(logCaptor.getAllValues()).extracting(SalesOrderTransferLogEntity::getSourceOrderId)
                .containsExactly(2001L, 2002L);
        assertThat(logCaptor.getAllValues()).extracting(SalesOrderTransferLogEntity::getTargetTeamId)
                .containsExactly(1002L, 1002L);
        assertThat(logCaptor.getAllValues()).extracting(SalesOrderTransferLogEntity::getChildOrderId)
                .containsExactly(3001L, 3002L);
        assertThat(logCaptor.getAllValues()).extracting(SalesOrderTransferLogEntity::getRemark)
                .containsExactly("目标团备注", "统一备注");

        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);
        verify(teamMapper, org.mockito.Mockito.times(2)).update(teamCaptor.capture(), any(UpdateWrapper.class));
        assertThat(teamCaptor.getAllValues()).extracting(SalesTeamEntity::getUsedSeats).containsExactly(0, 3);
    }

    @Test
    void mergeOrdersShouldRejectMergingToCurrentTeam() {
        SalesOrderTransferService service = new SalesOrderTransferService(
                mock(SalesBookingOrderMapper.class),
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class),
                mock(SalesTeamPriceMapper.class),
                mock(SalesOrderTransferLogMapper.class)
        );

        assertThatThrownBy(() -> service.mergeOrders(
                1001L,
                new SalesOrderTransferMergeRequest(
                        List.of(2001L),
                        1001L,
                        false,
                        "",
                        List.of()
                ),
                1L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("不能拼到当前团队");
    }

    @Test
    void mergeOrdersShouldRejectNonSanpinTargetTeam() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferService service = new SalesOrderTransferService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                mock(SalesOrderTransferLogMapper.class)
        );
        SalesTeamEntity sourceTeam = team(1001L, "CS-SP-BK-260625A");
        SalesTeamEntity targetTeam = team(1002L, "CS-BK-260626A");
        targetTeam.setTeamType("zhengtuan");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(sourceTeam, targetTeam);

        assertThatThrownBy(() -> service.mergeOrders(
                1001L,
                new SalesOrderTransferMergeRequest(
                        List.of(2001L),
                        1002L,
                        false,
                        "",
                        List.of()
                ),
                1L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("拼团目标团队必须是散拼团队");

        verify(orderMapper, never()).selectOne(any(Wrapper.class));
        verify(orderMapper, never()).insert(any(SalesBookingOrderEntity.class));
    }

    @Test
    void mergeOrdersShouldRejectAlreadyMergedSourceOrder() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferService service = new SalesOrderTransferService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                mock(SalesOrderTransferLogMapper.class)
        );
        SalesTeamEntity sourceTeam = team(1001L, "CS-SP-BK-260625A");
        SalesTeamEntity targetTeam = team(1002L, "CS-SP-BK-260626A");
        SalesBookingOrderEntity sourceOrder = order(2001L, 1001L, "SO-260625-001");
        sourceOrder.setOrderRole("merge_source");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(sourceTeam, targetTeam);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(sourceOrder);

        assertThatThrownBy(() -> service.mergeOrders(
                1001L,
                new SalesOrderTransferMergeRequest(
                        List.of(2001L),
                        1002L,
                        false,
                        "",
                        List.of()
                ),
                1L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("该订单已执行过拼团，不能重复拼团");

        verify(orderMapper, never()).insert(any(SalesBookingOrderEntity.class));
    }

    @Test
    void mergeOrdersShouldRejectCancelledSourceOrder() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferService service = new SalesOrderTransferService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                mock(SalesOrderTransferLogMapper.class)
        );
        SalesTeamEntity sourceTeam = team(1001L, "CS-SP-BK-260625A");
        SalesTeamEntity targetTeam = team(1002L, "CS-SP-BK-260626A");
        SalesBookingOrderEntity sourceOrder = order(2001L, 1001L, "SO-260625-001");
        sourceOrder.setStatus("cancelled");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(sourceTeam, targetTeam);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(sourceOrder);

        assertThatThrownBy(() -> service.mergeOrders(
                1001L,
                new SalesOrderTransferMergeRequest(
                        List.of(2001L),
                        1002L,
                        false,
                        "",
                        List.of()
                ),
                1L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("已取消订单不能拼团");

        verify(orderMapper, never()).insert(any(SalesBookingOrderEntity.class));
    }

    @Test
    void mergeOrdersShouldRejectSourceOrderWithoutGuestList() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferService service = new SalesOrderTransferService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                guestMapper,
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                mock(SalesOrderTransferLogMapper.class)
        );
        SalesTeamEntity sourceTeam = team(1001L, "CS-SP-BK-260625A");
        SalesTeamEntity targetTeam = team(1002L, "CS-SP-BK-260626A");
        SalesBookingOrderEntity sourceOrder = order(2001L, 1001L, "SO-260625-001");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(sourceTeam, targetTeam);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(sourceOrder);
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.mergeOrders(
                1001L,
                new SalesOrderTransferMergeRequest(
                        List.of(2001L),
                        1002L,
                        false,
                        "",
                        List.of()
                ),
                1L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("拼团订单必须先维护游客名单");

        verify(orderMapper, never()).insert(any(SalesBookingOrderEntity.class));
    }

    @Test
    void moveOrdersShouldMoveOrderAndChildrenToTargetTeamWithTransferLog() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferLogMapper logMapper = mock(SalesOrderTransferLogMapper.class);
        SalesOrderTransferService service = new SalesOrderTransferService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                logMapper
        );
        SalesTeamEntity sourceTeam = team(1001L, "CS-SP-BK-260625A");
        SalesTeamEntity targetTeam = team(1002L, "CS-SP-BK-260626A");
        SalesBookingOrderEntity order = order(2001L, 1001L, "SO-260625-001");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(sourceTeam, targetTeam);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        when(orderMapper.update(any(SalesBookingOrderEntity.class), any(UpdateWrapper.class))).thenReturn(1);
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(0);
        when(orderMapper.sumGuestCountByTeam(1L, 1002L)).thenReturn(2);

        service.moveOrders(
                1001L,
                new SalesOrderTransferMoveRequest(
                        List.of(2001L),
                        1002L,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "客户改到目标团"
                ),
                1L,
                "admin"
        );

        ArgumentCaptor<SalesBookingOrderEntity> orderCaptor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper).update(orderCaptor.capture(), any(UpdateWrapper.class));
        assertThat(orderCaptor.getValue().getTeamId()).isEqualTo(1002L);
        assertThat(orderCaptor.getValue().getOriginalOrderInfo()).contains("CS-SP-BK-260625A", "CS-SP-BK-260626A");

        ArgumentCaptor<SalesBookingOrderChargeLineEntity> chargeCaptor = ArgumentCaptor.forClass(SalesBookingOrderChargeLineEntity.class);
        verify(chargeLineMapper).update(chargeCaptor.capture(), any(UpdateWrapper.class));
        assertThat(chargeCaptor.getValue().getTeamId()).isEqualTo(1002L);

        ArgumentCaptor<SalesBookingOrderGuestEntity> guestCaptor = ArgumentCaptor.forClass(SalesBookingOrderGuestEntity.class);
        verify(guestMapper).update(guestCaptor.capture(), any(UpdateWrapper.class));
        assertThat(guestCaptor.getValue().getTeamId()).isEqualTo(1002L);

        ArgumentCaptor<SalesOrderTransferLogEntity> logCaptor = ArgumentCaptor.forClass(SalesOrderTransferLogEntity.class);
        verify(logMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getTransferType()).isEqualTo("move");
        assertThat(logCaptor.getValue().getSourceTeamId()).isEqualTo(1001L);
        assertThat(logCaptor.getValue().getTargetTeamId()).isEqualTo(1002L);
        assertThat(logCaptor.getValue().getChildOrderId()).isEqualTo(2001L);

        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);
        verify(teamMapper, org.mockito.Mockito.times(2)).update(teamCaptor.capture(), any(UpdateWrapper.class));
        assertThat(teamCaptor.getAllValues()).extracting(SalesTeamEntity::getUsedSeats).containsExactly(0, 2);
    }

    @Test
    void moveOrdersShouldRejectMovingToCurrentTeam() {
        SalesOrderTransferService service = new SalesOrderTransferService(
                mock(SalesBookingOrderMapper.class),
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class),
                mock(SalesTeamPriceMapper.class),
                mock(SalesOrderTransferLogMapper.class)
        );

        assertThatThrownBy(() -> service.moveOrders(
                1001L,
                new SalesOrderTransferMoveRequest(
                        List.of(2001L),
                        1001L,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "不能转回自己"
                ),
                1L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("不能转到当前团队");
    }

    private SalesTeamEntity team(Long id, String teamNo) {
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(id);
        team.setTenantId(1L);
        team.setProductId(88L);
        team.setTeamNo(teamNo);
        team.setTeamType("sanpin");
        team.setBusinessType("疗休养");
        team.setDepartureDate(LocalDate.of(2026, 6, 25));
        team.setStatus("normal");
        team.setTotalSeats(30);
        team.setUsedSeats(0);
        team.setRemainingSeats(30);
        team.setSingleRoomDifference(new BigDecimal("300.00"));
        team.setCloseDaysBefore(1);
        team.setIsDeleted(false);
        return team;
    }

    private SalesBookingOrderEntity order(Long id, Long teamId, String orderNo) {
        SalesBookingOrderEntity order = new SalesBookingOrderEntity();
        order.setId(id);
        order.setTenantId(1L);
        order.setTeamId(teamId);
        order.setOrderNo(orderNo);
        order.setCustomerId(3001L);
        order.setCustomerName("杭州百缘旅行社");
        order.setContactName("张三");
        order.setContactPhone("13521124678");
        order.setAdultCount(2);
        order.setGuestCount(2);
        order.setReceivableAmount(new BigDecimal("6000.00"));
        order.setReceivedAmount(new BigDecimal("1000.00"));
        order.setBalanceAmount(new BigDecimal("5000.00"));
        order.setStatus("confirmed");
        order.setBookedBy("张三");
        order.setIsDeleted(false);
        return order;
    }

    private SalesBookingOrderChargeLineEntity chargeLine(SalesBookingOrderEntity order) {
        SalesBookingOrderChargeLineEntity line = new SalesBookingOrderChargeLineEntity();
        line.setTenantId(1L);
        line.setOrderId(order.getId());
        line.setTeamId(order.getTeamId());
        line.setLineKind("base_price");
        line.setLineType("adult");
        line.setItemName("成人");
        line.setUnitPrice(new BigDecimal("3000.00"));
        line.setQuantity(new BigDecimal(String.valueOf(order.getGuestCount())));
        line.setAmount(order.getReceivableAmount());
        line.setStatus("effective");
        line.setSortOrder(1);
        return line;
    }

    private SalesBookingOrderGuestEntity guest(SalesBookingOrderEntity order) {
        return guest(order, 5001L, "李四", "adult");
    }

    private SalesBookingOrderGuestEntity guest(SalesBookingOrderEntity order, Long id, String name, String guestType) {
        SalesBookingOrderGuestEntity guest = new SalesBookingOrderGuestEntity();
        guest.setId(id);
        guest.setTenantId(1L);
        guest.setOrderId(order.getId());
        guest.setTeamId(order.getTeamId());
        guest.setIndexNo(id.intValue());
        guest.setGuestName(name);
        guest.setGuestType(guestType);
        guest.setLeaderFlag(false);
        guest.setIsDeleted(false);
        return guest;
    }

}
