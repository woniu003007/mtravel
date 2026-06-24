package com.mtravel.platform.sales.booking.order.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderGuestRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderPriceLineRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderPriceLineEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderFeeChangeMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderGuestMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderPriceLineMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收客订单主链路服务测试。
 *
 * <p>订单保存会影响团队实收和余位，是销售、计调、财务共用的主数据入口。测试先固定
 * 保存订单、价格明细、游客名单和团队人数联动，避免实现成只保存静态页面草稿。</p>
 */
class SalesBookingOrderServiceTest {

    @Test
    void createOrderShouldSaveOldSystemSectionsAndRefreshTeamSeats() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderPriceLineMapper priceLineMapper = mock(SalesBookingOrderPriceLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                priceLineMapper,
                guestMapper,
                mock(SalesBookingOrderFeeChangeMapper.class),
                teamMapper
        );
        SalesTeamEntity team = team(1001L, 20, 0, 20);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(3);
        doAnswer(invocation -> {
            SalesBookingOrderEntity order = invocation.getArgument(0);
            order.setId(2001L);
            return 1;
        }).when(orderMapper).insert(any(SalesBookingOrderEntity.class));

        var response = service.save(request(null, "confirmed"), 1L, "admin");

        assertThat(response.id()).isEqualTo(2001L);
        assertThat(response.guestCount()).isEqualTo(3);
        assertThat(response.receivableAmount()).isEqualByComparingTo("9000.00");
        assertThat(response.balanceAmount()).isEqualByComparingTo("8000.00");

        ArgumentCaptor<SalesBookingOrderEntity> orderCaptor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper).insert(orderCaptor.capture());
        SalesBookingOrderEntity savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getTeamId()).isEqualTo(1001L);
        assertThat(savedOrder.getOrderNo()).startsWith("SO-");
        assertThat(savedOrder.getTravelDescription()).contains("大连-上海");
        assertThat(savedOrder.getGuideName()).isEqualTo("王导");
        assertThat(savedOrder.getCustomerName()).isEqualTo("杭州百缘旅行社");
        assertThat(savedOrder.getHotelInfo()).contains("双床");
        assertThat(savedOrder.getStatus()).isEqualTo("confirmed");

        ArgumentCaptor<SalesBookingOrderPriceLineEntity> priceCaptor = ArgumentCaptor.forClass(SalesBookingOrderPriceLineEntity.class);
        verify(priceLineMapper).insert(priceCaptor.capture());
        assertThat(priceCaptor.getValue().getLineType()).isEqualTo("adult");
        assertThat(priceCaptor.getValue().getSubtotalAmount()).isEqualByComparingTo("9000.00");

        ArgumentCaptor<SalesBookingOrderGuestEntity> guestCaptor = ArgumentCaptor.forClass(SalesBookingOrderGuestEntity.class);
        verify(guestMapper, org.mockito.Mockito.times(2)).insert(guestCaptor.capture());
        assertThat(guestCaptor.getAllValues()).extracting(SalesBookingOrderGuestEntity::getGuestName)
                .containsExactly("张三", "李四");
        assertThat(guestCaptor.getAllValues().get(0).getLeaderFlag()).isTrue();
        assertThat(guestCaptor.getAllValues().get(0).getRoomGroup()).isEqualTo("1房");
        assertThat(guestCaptor.getAllValues()).extracting(SalesBookingOrderGuestEntity::getRoomRemark)
                .containsExactly("1大床（必须保证大床）", "1大床（必须保证大床）");

        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);
        verify(teamMapper).update(teamCaptor.capture(), any(UpdateWrapper.class));
        assertThat(teamCaptor.getValue().getUsedSeats()).isEqualTo(3);
        assertThat(teamCaptor.getValue().getRemainingSeats()).isEqualTo(17);
    }

    @Test
    void saveOrderShouldRejectStoppedTeam() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderPriceLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesBookingOrderFeeChangeMapper.class),
                teamMapper
        );
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 0, 20, "stopped"));

        assertThatThrownBy(() -> service.save(request(null, "confirmed"), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("团队已暂停收客，不能新增或确认订单");

        verify(orderMapper, never()).insert(any(SalesBookingOrderEntity.class));
    }

    @Test
    void cancelledOrderShouldNotOccupyTeamSeats() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderPriceLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesBookingOrderFeeChangeMapper.class),
                teamMapper
        );
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 3, 17));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(existingOrder(2001L));
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(0);

        service.save(request(2001L, "cancelled"), 1L, "admin");

        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);
        verify(teamMapper).update(teamCaptor.capture(), any(UpdateWrapper.class));
        assertThat(teamCaptor.getValue().getUsedSeats()).isZero();
        assertThat(teamCaptor.getValue().getRemainingSeats()).isEqualTo(20);
    }

    private SalesBookingOrderSaveRequest request(Long id, String status) {
        return new SalesBookingOrderSaveRequest(
                id,
                1001L,
                "BY-ORDER-001",
                3001L,
                "杭州百缘旅行社",
                "叶菊莲",
                "13521124678",
                "客户团号-A",
                "浙江省",
                "杭州市",
                "西湖区",
                "2026年6月25日 大连-上海 CZ6533，2026年6月30日 上海-大连 CZ6536",
                "接站：杭州东站",
                "送站：萧山机场",
                "接送备注",
                "王导",
                "13800000000",
                "导游备注",
                "双床 1 间，家庭同住",
                "费用说明",
                "确认说明",
                "订单备注",
                new BigDecimal("1000.00"),
                status,
                List.of(new SalesBookingOrderPriceLineRequest(
                        null,
                        "adult",
                        "成人",
                        new BigDecimal("3000.00"),
                        new BigDecimal("3"),
                        "成人价"
                )),
                List.of(
                        new SalesBookingOrderGuestRequest(
                                null,
                                1,
                                "张三",
                                null,
                                "210204198206214832",
                                null,
                                "男",
                                LocalDate.of(1982, 6, 21),
                                44,
                                "13521124678",
                                "adult",
                                "1房",
                                "1大床（必须保证大床）",
                                true,
                                "领队"
                        ),
                        new SalesBookingOrderGuestRequest(
                                null,
                                2,
                                "李四",
                                null,
                                "21020420101028741X",
                                null,
                                "女",
                                LocalDate.of(2010, 10, 28),
                                15,
                                "13521124678",
                                "child",
                                "1房",
                                "1大床（必须保证大床）",
                                false,
                                null
                        )
                )
        );
    }

    private SalesTeamEntity team(Long id, int total, int used, int remaining) {
        return team(id, total, used, remaining, "normal");
    }

    private SalesTeamEntity team(Long id, int total, int used, int remaining, String status) {
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(id);
        team.setTenantId(1L);
        team.setProductId(88L);
        team.setTeamNo("CS-SP-BK-260625A");
        team.setStatus(status);
        team.setTotalSeats(total);
        team.setUsedSeats(used);
        team.setRemainingSeats(remaining);
        return team;
    }

    private SalesBookingOrderEntity existingOrder(Long id) {
        SalesBookingOrderEntity order = new SalesBookingOrderEntity();
        order.setId(id);
        order.setTenantId(1L);
        order.setTeamId(1001L);
        order.setOrderNo("SO-260625-0001");
        order.setStatus("confirmed");
        return order;
    }
}
