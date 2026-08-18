package com.mtravel.platform.sales.booking.order.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.risk.service.CustomerRiskApprovalService;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalResponse;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingFeeChangeCreateRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderGuestRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderPriceLineRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderChargeLineEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderChargeLineMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderGuestMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.mapper.CommonAttachmentMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.ordertransfer.entity.SalesOrderTransferLogEntity;
import com.mtravel.platform.sales.ordertransfer.mapper.SalesOrderTransferLogMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper
        );
        SalesTeamEntity team = team(1001L, 20, 0, 20);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(orderMapper.maxOrderNoSuffix(any(), any())).thenReturn(41);
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(3);
        doAnswer(invocation -> {
            SalesBookingOrderEntity order = invocation.getArgument(0);
            order.setId(2001L);
            return 1;
        }).when(orderMapper).insert(any(SalesBookingOrderEntity.class));

        var response = service.save(request(null, "confirmed"), 1L, "admin");

        assertThat(response.id()).isEqualTo(2001L);
        assertThat(response.guestCount()).isEqualTo(3);
        assertThat(response.receivableAmount()).isEqualByComparingTo("9540.00");
        assertThat(response.receivedAmount()).isEqualByComparingTo("0.00");
        assertThat(response.balanceAmount()).isEqualByComparingTo("9540.00");

        ArgumentCaptor<SalesBookingOrderEntity> orderCaptor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper).insert(orderCaptor.capture());
        SalesBookingOrderEntity savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getTeamId()).isEqualTo(1001L);
        assertThat(savedOrder.getOrderNo()).matches("SO-\\d{6}-00042");
        verify(orderMapper).lockOrderNoGeneration(any(), any());
        verify(orderMapper).maxOrderNoSuffix(any(), any());
        assertThat(savedOrder.getTravelDescription()).contains("大连-上海");
        assertThat(savedOrder.getGuideName()).isEqualTo("王导");
        assertThat(savedOrder.getCustomerName()).isEqualTo("杭州百缘旅行社");
        assertThat(savedOrder.getSalespersonEmployeeId()).isEqualTo(31L);
        assertThat(savedOrder.getSalespersonEmployeeName()).isEqualTo("张业务");
        assertThat(savedOrder.getBookingOperatorEmployeeId()).isEqualTo(32L);
        assertThat(savedOrder.getBookingOperatorEmployeeName()).isEqualTo("王计调");
        assertThat(savedOrder.getSourceProvince()).isEqualTo("浙江省");
        assertThat(savedOrder.getSourceCity()).isEqualTo("杭州市");
        assertThat(savedOrder.getSourceDistrict()).isEqualTo("余杭区");
        assertThat(savedOrder.getHotelInfo()).contains("双床");
        assertThat(savedOrder.getStatus()).isEqualTo("confirmed");
        assertThat(savedOrder.getGuestCount()).isEqualTo(3);
        assertThat(savedOrder.getAdultCount()).isEqualTo(1);
        assertThat(savedOrder.getChildCount()).isEqualTo(1);
        assertThat(savedOrder.getReceivedAmount()).isEqualByComparingTo("0.00");
        assertThat(savedOrder.getBalanceAmount()).isEqualByComparingTo("9540.00");

        ArgumentCaptor<SalesBookingOrderChargeLineEntity> chargeCaptor = ArgumentCaptor.forClass(SalesBookingOrderChargeLineEntity.class);
        verify(chargeLineMapper, org.mockito.Mockito.times(2)).insert(chargeCaptor.capture());
        assertThat(chargeCaptor.getAllValues()).extracting(SalesBookingOrderChargeLineEntity::getLineKind)
                .containsExactly("base_price", "base_price");
        assertThat(chargeCaptor.getAllValues()).extracting(SalesBookingOrderChargeLineEntity::getLineType)
                .containsExactly("adult", "surcharge");
        assertThat(chargeCaptor.getAllValues()).extracting(SalesBookingOrderChargeLineEntity::getOccupySeat)
                .containsExactly(true, false);
        assertThat(chargeCaptor.getAllValues()).extracting(SalesBookingOrderChargeLineEntity::getAmount)
                .containsExactly(new BigDecimal("9000.00"), new BigDecimal("540.00"));

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
    void updateOrderShouldPreserveExistingReceivedAmountInsteadOfRequestValue() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper
        );
        SalesBookingOrderEntity current = existingOrder(2001L);
        current.setReceivedAmount(new BigDecimal("1200.00"));
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 3, 17));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(3);

        var response = service.save(request(2001L, "confirmed"), 1L, "admin");

        assertThat(response.receivableAmount()).isEqualByComparingTo("9540.00");
        assertThat(response.receivedAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.balanceAmount()).isEqualByComparingTo("8340.00");
        ArgumentCaptor<SalesBookingOrderEntity> orderCaptor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper).update(orderCaptor.capture(), any(UpdateWrapper.class));
        SalesBookingOrderEntity updatedOrder = orderCaptor.getValue();
        assertThat(updatedOrder.getReceivedAmount()).isEqualByComparingTo("1200.00");
        assertThat(updatedOrder.getBalanceAmount()).isEqualByComparingTo("8340.00");
    }

    @Test
    void updateOrderShouldSyncGuestsIncrementally() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper
        );
        SalesBookingOrderEntity current = existingOrder(2001L);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 2, 18));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(2);
        SalesBookingOrderGuestEntity retained = existingGuest(11L, "张三", "adult");
        SalesBookingOrderGuestEntity removed = existingGuest(12L, "李四", "child");
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(retained, removed), List.of(retained));

        service.save(requestWithGuests(2001L, "confirmed", List.of(
                guestRequest(11L, 1, "张三", "adult"),
                guestRequest(null, 2, "王五", "child")
        )), 1L, "admin");

        ArgumentCaptor<SalesBookingOrderGuestEntity> insertCaptor = ArgumentCaptor.forClass(SalesBookingOrderGuestEntity.class);
        verify(guestMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getGuestName()).isEqualTo("王五");
        ArgumentCaptor<SalesBookingOrderGuestEntity> deleteCaptor = ArgumentCaptor.forClass(SalesBookingOrderGuestEntity.class);
        verify(guestMapper).update(deleteCaptor.capture(), any(UpdateWrapper.class));
        assertThat(deleteCaptor.getValue().getIsDeleted()).isTrue();
    }

    @Test
    void updateOrderShouldUpdateChangedExistingGuestWithoutReinserting() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper
        );
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 1, 19));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(existingOrder(2001L));
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(1);
        SalesBookingOrderGuestEntity existing = existingGuest(11L, "张三", "adult");
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing), List.of(existing));

        service.save(requestWithGuests(2001L, "confirmed", List.of(
                guestRequest(11L, 1, "张三修改", "adult")
        )), 1L, "admin");

        verify(guestMapper, never()).insert(any(SalesBookingOrderGuestEntity.class));
        verify(guestMapper).update(any(SalesBookingOrderGuestEntity.class), any(UpdateWrapper.class));
    }

    @Test
    void updateOrderShouldRejectGuestIdFromOtherOrder() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper
        );
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 2, 18));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(existingOrder(2001L));
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.save(requestWithGuests(2001L, "confirmed", List.of(
                guestRequest(999L, 1, "张三", "adult")
        )), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("游客名单记录不存在或不属于当前订单，请刷新后重试");

        verify(guestMapper, never()).insert(any(SalesBookingOrderGuestEntity.class));
    }

    @Test
    void saveOrderShouldRejectStoppedTeam() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
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
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
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

    @Test
    void saveOrderShouldClearSourcePlaceWhenFirstGuestIdCardCannotResolveEvenIfSecondGuestCanResolve() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper
        );
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 0, 20));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(orderMapper.sumGuestCountByTeam(1L, 1001L)).thenReturn(3);
        doAnswer(invocation -> {
            SalesBookingOrderEntity order = invocation.getArgument(0);
            order.setId(2001L);
            return 1;
        }).when(orderMapper).insert(any(SalesBookingOrderEntity.class));

        service.save(requestWithGuestCertificates("990000198206214839", "310101201010287414"), 1L, "admin");

        ArgumentCaptor<SalesBookingOrderEntity> orderCaptor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getSourceProvince()).isNull();
        assertThat(orderCaptor.getValue().getSourceCity()).isNull();
        assertThat(orderCaptor.getValue().getSourceDistrict()).isNull();
    }

    @Test
    void saveOrderShouldRejectMergeSourceOrderModification() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                teamMapper
        );
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setOrderRole("merge_source");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 3, 17));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);

        assertThatThrownBy(() -> service.save(request(2001L, "confirmed"), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("已拼出的来源订单不能修改，请到目标团队处理拼入订单");

        verify(orderMapper, never()).update(any(SalesBookingOrderEntity.class), any(UpdateWrapper.class));
    }

    @Test
    void createFeeChangeShouldRejectMergeSourceOrder() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class),
                expenseItemMapper
        );
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setOrderRole("merge_source");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);

        assertThatThrownBy(() -> service.createFeeChange(
                2001L,
                new SalesBookingFeeChangeCreateRequest(
                        "increase",
                        501L,
                        "补费用",
                        new BigDecimal("100.00"),
                        null
                ),
                1L,
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("已拼出的来源订单不能修改，请到目标团队处理拼入订单");

        verify(expenseItemMapper, never()).selectOne(any(Wrapper.class));
        verify(chargeLineMapper, never()).insert(any(SalesBookingOrderChargeLineEntity.class));
    }

    @Test
    void cancelFeeChangeShouldRejectMergeSourceOrder() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class)
        );
        SalesBookingOrderChargeLineEntity feeChange = new SalesBookingOrderChargeLineEntity();
        feeChange.setId(801L);
        feeChange.setOrderId(2001L);
        feeChange.setAmount(new BigDecimal("100.00"));
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setOrderRole("merge_source");
        when(chargeLineMapper.selectOne(any(Wrapper.class))).thenReturn(feeChange);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);

        assertThatThrownBy(() -> service.cancelFeeChange(801L, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("已拼出的来源订单不能修改，请到目标团队处理拼入订单");

        verify(chargeLineMapper, never()).update(any(SalesBookingOrderChargeLineEntity.class), any(UpdateWrapper.class));
    }

    @Test
    void saveOrderShouldRejectRiskyCustomerWithoutApprovedApprovalWhenApprovalEnabled() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        CustomerRiskApprovalService riskApprovalService = mock(CustomerRiskApprovalService.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                teamMapper,
                riskApprovalService
        );
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team(1001L, 20, 0, 20));
        doThrow(new BizException("客户合同或授信风险需要总经理审批后才能提交订单"))
                .when(riskApprovalService)
                .assertOrderCanSave(1L, 3001L, 1001L, null, new BigDecimal("9540.00"), null);

        assertThatThrownBy(() -> service.save(request(null, "confirmed"), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("客户合同或授信风险需要总经理审批后才能提交订单");

        verify(orderMapper, never()).insert(any(SalesBookingOrderEntity.class));
    }

    @Test
    void detailShouldReturnApprovedRiskApprovalRequestIdForEditingRiskyOrder() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        CustomerRiskApprovalService riskApprovalService = mock(CustomerRiskApprovalService.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                mock(SalesTeamMapper.class),
                riskApprovalService
        );
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setCustomerId(3001L);
        order.setCustomerName("杭州百缘旅行社");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(riskApprovalService.latestApprovedForOrder(1L, 3001L, 1001L, 2001L))
                .thenReturn(new CustomerRiskApprovalResponse(
                        77L,
                        3001L,
                        "杭州百缘旅行社",
                        1001L,
                        2001L,
                        "RA-260626-00001",
                        new BigDecimal("9540.00"),
                        List.of("credit_over_limit"),
                        "授信超限",
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "approved",
                        "sales01",
                        "boss01",
                        OffsetDateTime.now(),
                        null,
                        null,
                        "同意",
                        null,
                        "sales01",
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                ));

        var response = service.detail(2001L, 1L);

        assertThat(response.riskApprovalRequestId()).isEqualTo(77L);
    }

    @Test
    void createFeeChangeShouldSaveSignedAmountAndRefreshReceivable() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class),
                expenseItemMapper
        );
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setReceivableAmount(new BigDecimal("9000.00"));
        order.setReceivedAmount(new BigDecimal("1000.00"));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        EnterpriseExpenseItemEntity project = new EnterpriseExpenseItemEntity();
        project.setId(501L);
        project.setTenantId(1L);
        project.setResourceType("extra_fee");
        project.setProjectName("退餐费");
        project.setStatus("active");
        project.setIsDeleted(false);
        when(expenseItemMapper.selectOne(any(Wrapper.class))).thenReturn(project);
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.createFeeChange(
                2001L,
                new SalesBookingFeeChangeCreateRequest(
                        "decrease",
                        501L,
                        "客人临时不吃晚餐",
                        new BigDecimal("720.00"),
                        "按客户确认退减"
                ),
                1L,
                "老板账号"
        );

        ArgumentCaptor<SalesBookingOrderChargeLineEntity> feeCaptor = ArgumentCaptor.forClass(SalesBookingOrderChargeLineEntity.class);
        verify(chargeLineMapper).insert(feeCaptor.capture());
        assertThat(feeCaptor.getValue().getLineKind()).isEqualTo("adjustment");
        assertThat(feeCaptor.getValue().getChangeType()).isEqualTo("decrease");
        assertThat(feeCaptor.getValue().getFeeProjectId()).isEqualTo(501L);
        assertThat(feeCaptor.getValue().getFeeProjectName()).isEqualTo("退餐费");
        assertThat(feeCaptor.getValue().getAmount()).isEqualByComparingTo("-720.00");
        assertThat(feeCaptor.getValue().getStatus()).isEqualTo("approved");

        ArgumentCaptor<SalesBookingOrderEntity> orderCaptor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper).update(orderCaptor.capture(), any(UpdateWrapper.class));
        assertThat(orderCaptor.getValue().getReceivableAmount()).isEqualByComparingTo("8280.00");
        assertThat(orderCaptor.getValue().getBalanceAmount()).isEqualByComparingTo("7280.00");
    }

    @Test
    void operationRowsShouldUseChargeLinesForPriceDetailWithoutMixingRemarks() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class)
        );
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setCustomerName("杭州百缘旅行社");
        order.setCustomerTeamNo("客户团号-A");
        order.setContactName("张三");
        order.setPickupInfo("接站：[大连/上海虹桥站 CZ123 2026-06-25 08:00 -- 2026-06-25 10:00]");
        order.setDropoffInfo("送站：[上海虹桥站/大连 CZ124 2026-06-30 18:00 -- 2026-06-30 20:00]");
        order.setOriginalOrderInfo("[四]2026-06-25 宁波方特二日游 无锡新旅程旅行社");
        order.setAdultCount(2);
        order.setChildCount(0);
        order.setChildNoBedCount(0);
        order.setSeniorCount(0);
        order.setGuestCount(2);
        order.setReceivableAmount(new BigDecimal("6360.00"));
        order.setReceivedAmount(new BigDecimal("1000.00"));
        order.setBalanceAmount(new BigDecimal("5360.00"));
        order.setFeeRemark("费用说明");
        order.setOrderRemark("订单备注");
        SalesBookingOrderChargeLineEntity adult = chargeLine(order, "adult", "成人", "3000", "2", "6000", 1);
        SalesBookingOrderChargeLineEntity surcharge = chargeLine(order, "surcharge", "附加费", "180", "2", "360", 2);
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(adult, surcharge));

        var rows = service.toOperationRows(List.of(order));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).pickupInfo()).isEqualTo("接站：[大连/上海虹桥站 CZ123 2026-06-25 08:00 -- 2026-06-25 10:00]");
        assertThat(rows.get(0).dropoffInfo()).isEqualTo("送站：[上海虹桥站/大连 CZ124 2026-06-30 18:00 -- 2026-06-30 20:00]");
        assertThat(rows.get(0).originalOrderInfo()).isEqualTo("[四]2026-06-25 宁波方特二日游 无锡新旅程旅行社");
        assertThat(rows.get(0).guestCountText()).isEqualTo("2人[2/0/0]");
        assertThat(rows.get(0).priceDetail()).isEqualTo("成人：3000 * 2 = 6000\n附加费：180 * 2 = 360");
        assertThat(rows.get(0).feeRemark()).isEqualTo("费用说明");
        assertThat(rows.get(0).orderRemark()).isEqualTo("订单备注");
        verify(chargeLineMapper).selectList(any(Wrapper.class));
    }

    @Test
    void operationRowsShouldShowOccupySeatTotalAndRealGuestTypeCounts() {
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                mock(SalesBookingOrderMapper.class),
                chargeLineMapper,
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class)
        );
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setGuestCount(3);
        order.setAdultCount(1);
        order.setChildCount(2);
        order.setChildNoBedCount(0);
        order.setSeniorCount(0);
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                chargeLine(order, "adult", "成人", "980", "3", "2940", 1)
        ));

        var rows = service.toOperationRows(List.of(order));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).guestCountText()).isEqualTo("3人[1/2/0]");
    }

    @Test
    void operationRowsShouldShowMergeTargetInfoForSourceOrder() {
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferLogMapper transferLogMapper = mock(SalesOrderTransferLogMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                mock(SalesBookingOrderMapper.class),
                chargeLineMapper,
                mock(SalesBookingOrderGuestMapper.class),
                teamMapper,
                new com.mtravel.platform.sales.booking.aiimport.service.IdCardValidator(),
                null,
                null,
                transferLogMapper
        );
        SalesBookingOrderEntity sourceOrder = existingOrder(2001L);
        sourceOrder.setOrderRole("merge_source");
        sourceOrder.setOrderNo("SO-260625-0001");
        SalesOrderTransferLogEntity log = new SalesOrderTransferLogEntity();
        log.setTenantId(1L);
        log.setSourceOrderId(2001L);
        log.setSourceTeamId(1001L);
        log.setTargetTeamId(1002L);
        log.setChildOrderId(3001L);
        SalesBookingOrderEntity childOrder = existingOrder(3001L);
        childOrder.setTeamId(1002L);
        childOrder.setOrderNo("SO-260625-0001-PT-CS-SP-BK-260626A");
        childOrder.setOrderRole("merge_child");
        childOrder.setCustomerName("无锡新旅程旅行社");
        SalesTeamEntity targetTeam = team(1002L, 30, 3, 27);
        targetTeam.setTeamNo("CS-SP-BK-260626A");
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(transferLogMapper.selectCompletedMergeBySourceOrders(1L, List.of(2001L))).thenReturn(List.of(log));
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of(targetTeam));

        var rows = service.toOperationRows(List.of(sourceOrder));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).orderRole()).isEqualTo("merge_source");
        assertThat(rows.get(0).orderRoleLabel()).isEqualTo("已拼出");
        assertThat(rows.get(0).mergeOrderInfos()).hasSize(1);
        assertThat(rows.get(0).mergeOrderInfos().get(0).orderId()).isEqualTo(3001L);
        assertThat(rows.get(0).mergeOrderInfos().get(0).teamId()).isEqualTo(1002L);
        assertThat(rows.get(0).mergeOrderInfos().get(0).summary()).contains("CS-SP-BK-260626A");
    }

    @Test
    void operationRowsShouldShowMergeTargetInfoForNormalSourceOrderAfterOldSystemAlignedMerge() {
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferLogMapper transferLogMapper = mock(SalesOrderTransferLogMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                mock(SalesBookingOrderMapper.class),
                chargeLineMapper,
                mock(SalesBookingOrderGuestMapper.class),
                teamMapper,
                new com.mtravel.platform.sales.booking.aiimport.service.IdCardValidator(),
                null,
                null,
                transferLogMapper
        );
        SalesBookingOrderEntity sourceOrder = existingOrder(2001L);
        sourceOrder.setOrderRole("normal");
        sourceOrder.setOrderNo("SO-260625-0001");
        SalesOrderTransferLogEntity log = new SalesOrderTransferLogEntity();
        log.setTenantId(1L);
        log.setSourceOrderId(2001L);
        log.setSourceTeamId(1001L);
        log.setTargetTeamId(1002L);
        log.setChildOrderId(3001L);
        SalesTeamEntity targetTeam = team(1002L, 30, 3, 27);
        targetTeam.setTeamNo("CS-SP-BK-260626A");
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(transferLogMapper.selectCompletedMergeBySourceOrders(1L, List.of(2001L))).thenReturn(List.of(log));
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of(targetTeam));

        var rows = service.toOperationRows(List.of(sourceOrder));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).orderRole()).isEqualTo("normal");
        assertThat(rows.get(0).mergeOrderInfos()).hasSize(1);
        assertThat(rows.get(0).mergeOrderInfos().get(0).orderId()).isEqualTo(3001L);
        assertThat(rows.get(0).mergeOrderInfos().get(0).teamId()).isEqualTo(1002L);
        assertThat(rows.get(0).mergeOrderInfos().get(0).summary()).contains("CS-SP-BK-260626A");
    }

    @Test
    void operationRowsShouldShowSourceInfoForMergeChildOrder() {
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesOrderTransferLogMapper transferLogMapper = mock(SalesOrderTransferLogMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                mock(SalesBookingOrderMapper.class),
                chargeLineMapper,
                mock(SalesBookingOrderGuestMapper.class),
                teamMapper,
                new com.mtravel.platform.sales.booking.aiimport.service.IdCardValidator(),
                null,
                null,
                transferLogMapper
        );
        SalesBookingOrderEntity childOrder = existingOrder(3001L);
        childOrder.setOrderRole("merge_child");
        childOrder.setOrderNo("SO-260625-0001-PT-CS-SP-BK-260626A");
        SalesOrderTransferLogEntity log = new SalesOrderTransferLogEntity();
        log.setTenantId(1L);
        log.setSourceOrderId(2001L);
        log.setSourceTeamId(1001L);
        log.setTargetTeamId(1002L);
        log.setChildOrderId(3001L);
        SalesTeamEntity sourceTeam = team(1001L, 30, 3, 27);
        sourceTeam.setTeamNo("CS-SP-BK-260625A");
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(transferLogMapper.selectCompletedMergeByChildOrders(1L, List.of(3001L))).thenReturn(List.of(log));
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sourceTeam));

        var rows = service.toOperationRows(List.of(childOrder));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).orderRole()).isEqualTo("merge_child");
        assertThat(rows.get(0).orderRoleLabel()).isEqualTo("拼入订单");
        assertThat(rows.get(0).sourceOrderInfos()).hasSize(1);
        assertThat(rows.get(0).sourceOrderInfos().get(0).orderId()).isEqualTo(2001L);
        assertThat(rows.get(0).sourceOrderInfos().get(0).teamId()).isEqualTo(1001L);
        assertThat(rows.get(0).sourceOrderInfos().get(0).summary()).contains("CS-SP-BK-260625A");
    }

    @Test
    void exportGuestWorkbookShouldFollowOldXlsTemplate() throws Exception {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                guestMapper,
                mock(SalesTeamMapper.class)
        );
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setCustomerName("测试地接社");
        order.setTravelDescription("宁波方特二日游");
        order.setPickupInfo("2026-06-15/--－-- [ 出发：14:31　抵达：14:31]");
        order.setDropoffInfo("2026-06-15/--－-- [  出发：14:31　抵达：14:31]");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        SalesBookingOrderGuestEntity guest = new SalesBookingOrderGuestEntity();
        guest.setId(1L);
        guest.setIndexNo(1);
        guest.setGuestName("张三");
        guest.setRoomGroup("0");
        guest.setCertificateNo("330105198201010319");
        guest.setGender("男");
        guest.setBirthDate(LocalDate.of(1982, 1, 1));
        guest.setGuestType("adult");
        guest.setAge(44);
        guest.setPhone("13521124678");
        guest.setRemark("W");
        guest.setLeaderFlag(false);
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(guest));

        ByteArrayOutputStream output = service.exportGuestWorkbook(2001L, 1L);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Sheet1");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("序号");
            assertThat(sheet.getRow(0).getCell(11).getStringCellValue()).isEqualTo("组备注");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("\t1");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("\t330105198201010319");
            assertThat(sheet.getRow(1).getCell(8).getStringCellValue()).isEqualTo("\t13521124678");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("行程");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("宁波方特二日游");
            assertThat(sheet.getNumMergedRegions()).isEqualTo(3);
        assertThat(sheet.getMergedRegion(0).formatAsString()).isEqualTo("B3:L3");
        }
    }

    @Test
    void importGuestWorkbookPreviewShouldParseOldExportTemplateAndDeriveIdentityFields() throws Exception {
        SalesBookingOrderService service = new SalesBookingOrderService(
                mock(SalesBookingOrderMapper.class),
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class)
        );
        byte[] workbookBytes = guestImportWorkbook();

        var response = service.importGuestWorkbookPreview(
                new ByteArrayInputStream(workbookBytes),
                "名单导入测试.xls"
        );

        assertThat(response.importedCount()).isEqualTo(2);
        assertThat(response.validCount()).isEqualTo(1);
        assertThat(response.invalidCount()).isEqualTo(1);
        assertThat(response.guests()).hasSize(2);
        assertThat(response.guests().get(0).guestName()).isEqualTo("张三");
        assertThat(response.guests().get(0).roomGroup()).isEqualTo("1房");
        assertThat(response.guests().get(0).certificateNo()).isEqualTo("11010519491231002X");
        assertThat(response.guests().get(0).gender()).isEqualTo("女");
        assertThat(response.guests().get(0).birthDate()).isEqualTo(LocalDate.of(1949, 12, 31));
        assertThat(response.guests().get(0).guestType()).isEqualTo("senior");
        assertThat(response.guests().get(0).idCardValid()).isTrue();
        assertThat(response.guests().get(1).guestName()).isEqualTo("李四");
        assertThat(response.guests().get(1).guestType()).isEqualTo("child");
        assertThat(response.guests().get(1).idCardValid()).isFalse();
        assertThat(response.guests().get(1).idCardWarning()).contains("身份证校验位不正确");
        assertThat(response.warnings()).anyMatch(warning -> warning.contains("第3行"));
    }

    @Test
    void guestImportTemplateWorkbookShouldProvideBlankOldSystemHeaders() throws Exception {
        SalesBookingOrderService service = new SalesBookingOrderService(
                mock(SalesBookingOrderMapper.class),
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class)
        );

        ByteArrayOutputStream output = service.guestImportTemplateWorkbook();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Sheet1");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("序号");
            assertThat(sheet.getRow(0).getCell(11).getStringCellValue()).isEqualTo("组备注");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("");
            assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("行程");
            assertThat(sheet.getRow(7).getCell(0).getStringCellValue()).isEqualTo("去程");
            assertThat(sheet.getRow(8).getCell(0).getStringCellValue()).isEqualTo("回程");
            assertThat(sheet.getNumMergedRegions()).isEqualTo(3);
        }
        assertThat(service.guestImportTemplateFilename()).isEqualTo("游客名单导入模板.xls");
    }

    @Test
    void orderManagePageShouldReturnOldSystemRowsWithTeamProductGuestsAndFiles() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderChargeLineMapper chargeLineMapper = mock(SalesBookingOrderChargeLineMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        CommonAttachmentMapper attachmentMapper = mock(CommonAttachmentMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                chargeLineMapper,
                guestMapper,
                teamMapper,
                new com.mtravel.platform.sales.booking.aiimport.service.IdCardValidator(),
                null,
                null,
                null,
                productMapper,
                attachmentMapper
        );
        SalesBookingOrderEntity order = existingOrder(2001L);
        order.setCustomerName("杭州百缘旅行社");
        order.setCustomerTeamNo("客户团号-A");
        order.setContactName("张三");
        order.setPickupInfo("接站：[大连/上海虹桥站 CZ123 2026-06-25 08:00 -- 2026-06-25 10:00]");
        order.setDropoffInfo("送站：[上海虹桥站/大连 CZ124 2026-06-30 18:00 -- 2026-06-30 20:00]");
        order.setPickupRemark("接送备注");
        order.setGuideRemark("导游备注");
        order.setSourceProvince("浙江省");
        order.setSourceCity("杭州市");
        order.setSourceDistrict("拱墅区");
        order.setHotelInfo("06-30 双床/大床 1");
        order.setFeeRemark("费用说明");
        order.setOrderRemark("订单备注");
        order.setBookedBy("老板账号");
        order.setBookedAt(OffsetDateTime.parse("2026-06-26T15:04:37+08:00"));
        order.setReceivableAmount(new BigDecimal("6360.00"));
        order.setReceivedAmount(new BigDecimal("1000.00"));
        order.setBalanceAmount(new BigDecimal("5360.00"));
        order.setAdultCount(2);
        order.setGuestCount(2);
        order.setTagging(true);
        when(orderMapper.selectPage(any(), any(Wrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SalesBookingOrderEntity>(1, 20, 1).setRecords(List.of(order)));
        SalesTeamEntity team = team(1001L, 20, 2, 18);
        team.setTeamNo("CS-SP-BK-260625A");
        team.setTeamType("sanpin");
        team.setDepartureDate(LocalDate.of(2026, 6, 25));
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of(team));
        SalesProductEntity product = new SalesProductEntity();
        product.setId(88L);
        product.setTenantId(1L);
        product.setProductName("宁波方特二日游");
        when(productMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product));
        SalesBookingOrderChargeLineEntity adult = chargeLine(order, "adult", "成人", "3000", "2", "6000", 1);
        SalesBookingOrderChargeLineEntity surcharge = chargeLine(order, "surcharge", "附加费", "180", "2", "360", 2);
        when(chargeLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(adult, surcharge));
        SalesBookingOrderGuestEntity guest = new SalesBookingOrderGuestEntity();
        guest.setOrderId(2001L);
        guest.setGuestName("张三");
        guest.setCertificateNo("330105198201010319");
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(guest));
        CommonAttachmentEntity attachment = new CommonAttachmentEntity();
        attachment.setBusinessId(2001L);
        when(attachmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(attachment));

        var result = service.orderManagePage(
                1L,
                "CS-SP",
                "客户团号-A",
                "杭州百缘",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "宁波方特",
                "接送备注",
                new BigDecimal("6360.00"),
                "老板账号",
                "张三",
                "sanpin",
                "confirmed",
                "booked",
                true,
                true,
                1,
                20
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        var row = result.items().get(0);
        assertThat(row.id()).isEqualTo(2001L);
        assertThat(row.teamId()).isEqualTo(1001L);
        assertThat(row.teamNo()).isEqualTo("CS-SP-BK-260625A");
        assertThat(row.teamType()).isEqualTo("sanpin");
        assertThat(row.teamTypeLabel()).isEqualTo("散拼");
        assertThat(row.departureDate()).isEqualTo(LocalDate.of(2026, 6, 25));
        assertThat(row.productName()).isEqualTo("宁波方特二日游");
        assertThat(row.orderInfo()).contains("杭州百缘旅行社", "客户团号-A");
        assertThat(row.pickupRemark()).isEqualTo("接送备注");
        assertThat(row.guideRemark()).isEqualTo("导游备注");
        assertThat(row.sourcePlace()).isEqualTo("浙江省杭州市拱墅区");
        assertThat(row.guestName()).isEqualTo("张三");
        assertThat(row.priceDetail()).contains("成人：3000 * 2 = 6000");
        assertThat(row.receivableAmount()).isEqualTo("¥6360");
        assertThat(row.status()).isEqualTo("已确认");
        assertThat(row.tagging()).isTrue();
        assertThat(row.hasOrderFile()).isTrue();
    }

    @Test
    void orderManagePageShouldUseExactGuestCertificateFilterForIdCardKeyword() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class)
        );
        when(orderMapper.selectPage(any(), any(Wrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SalesBookingOrderEntity>(1, 20, 0));

        service.orderManagePage(
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "210281201012158614",
                null,
                null,
                "booked",
                null,
                null,
                1,
                20
        );

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).selectPage(any(), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment)
                .contains("g.certificate_no =")
                .doesNotContain("g.certificate_no ILIKE")
                .doesNotContain("g.passport_no ILIKE")
                .doesNotContain("g.phone ILIKE");
    }

    @Test
    void orderManagePageShouldKeepFuzzyGuestNameFilterForNameKeyword() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class)
        );
        when(orderMapper.selectPage(any(), any(Wrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SalesBookingOrderEntity>(1, 20, 0));

        service.orderManagePage(
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "张三",
                null,
                null,
                "booked",
                null,
                null,
                1,
                20
        );

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).selectPage(any(), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment)
                .contains("g.guest_name ILIKE")
                .contains("g.certificate_no =")
                .doesNotContain("g.certificate_no ILIKE")
                .doesNotContain("g.passport_no ILIKE")
                .doesNotContain("g.phone ILIKE");
    }

    @Test
    void operationLeaderSummaryShouldUseLeaderGuestsFromEffectiveOrders() {
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                mock(SalesBookingOrderMapper.class),
                mock(SalesBookingOrderChargeLineMapper.class),
                guestMapper,
                mock(SalesTeamMapper.class)
        );
        SalesBookingOrderEntity normalOrder = existingOrder(2001L);
        normalOrder.setOrderRole("normal");
        SalesBookingOrderEntity mergeChildOrder = existingOrder(3001L);
        mergeChildOrder.setOrderRole("merge_child");
        SalesBookingOrderEntity mergeSourceOrder = existingOrder(4001L);
        mergeSourceOrder.setOrderRole("merge_source");
        SalesBookingOrderEntity cancelledOrder = existingOrder(5001L);
        cancelledOrder.setStatus("cancelled");
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                leaderGuest(2001L, "张三", "13800138000"),
                leaderGuest(3001L, "李四", null),
                leaderGuest(4001L, "来源领队", "13600136000"),
                leaderGuest(5001L, "取消领队", "13700137000")
        ));

        String summary = service.operationLeaderSummary(
                1L,
                List.of(normalOrder, mergeChildOrder, mergeSourceOrder, cancelledOrder)
        );

        assertThat(summary).isEqualTo("张三[Tel:13800138000]、李四、来源领队[Tel:13600136000]");
    }

    @Test
    void updateOrderTaggingShouldOnlyChangeTaggingFlag() {
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderService service = new SalesBookingOrderService(
                orderMapper,
                mock(SalesBookingOrderChargeLineMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(SalesTeamMapper.class)
        );
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(existingOrder(2001L));

        service.updateOrderTagging(2001L, true, 1L);

        ArgumentCaptor<SalesBookingOrderEntity> captor = ArgumentCaptor.forClass(SalesBookingOrderEntity.class);
        verify(orderMapper).update(captor.capture(), any(UpdateWrapper.class));
        assertThat(captor.getValue().getTagging()).isTrue();
        assertThat(captor.getValue().getStatus()).isNull();
        assertThat(captor.getValue().getTeamId()).isNull();
    }

    private byte[] guestImportWorkbook() throws Exception {
        try (var workbook = new HSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            var header = sheet.createRow(0);
            String[] headers = {"序号", "客人姓名", "组号", "证件号", "性别", "出生年月", "客户类型", "年龄", "联系电话", "单人备注", "领队", "组备注"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            var row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("1");
            row1.createCell(1).setCellValue("张三");
            row1.createCell(2).setCellValue("1房");
            row1.createCell(3).setCellValue("\t11010519491231002X");
            row1.createCell(6).setCellValue("成人");
            row1.createCell(8).setCellValue("\t13521124678");
            row1.createCell(10).setCellValue("是");
            row1.createCell(11).setCellValue("大床");

            var row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("2");
            row2.createCell(1).setCellValue("李四");
            row2.createCell(2).setCellValue("1房");
            row2.createCell(3).setCellValue("\t110105201010280011");
            row2.createCell(6).setCellValue("成人");
            row2.createCell(8).setCellValue("\t13521124679");

            var tripRow = sheet.createRow(3);
            tripRow.createCell(0).setCellValue("行程");
            tripRow.createCell(1).setCellValue("上海五日游");
            var outboundRow = sheet.createRow(4);
            outboundRow.createCell(0).setCellValue("去程");
            outboundRow.createCell(1).setCellValue("大连 / 上海 / CZ6533");
            var returnRow = sheet.createRow(5);
            returnRow.createCell(0).setCellValue("回程");
            returnRow.createCell(1).setCellValue("上海 / 大连 / CZ6536");

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private SalesBookingOrderChargeLineEntity chargeLine(
            SalesBookingOrderEntity order,
            String lineType,
            String itemName,
            String unitPrice,
            String quantity,
            String amount,
            int sortOrder
    ) {
        SalesBookingOrderChargeLineEntity line = new SalesBookingOrderChargeLineEntity();
        line.setTenantId(order.getTenantId());
        line.setOrderId(order.getId());
        line.setTeamId(order.getTeamId());
        line.setLineKind("base_price");
        line.setLineType(lineType);
        line.setItemName(itemName);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setQuantity(new BigDecimal(quantity));
        line.setOccupySeat("adult".equals(lineType) || "child".equals(lineType) || "senior".equals(lineType) || "escort".equals(lineType));
        line.setAmount(new BigDecimal(amount));
        line.setStatus("effective");
        line.setSortOrder(sortOrder);
        return line;
    }

    private SalesBookingOrderSaveRequest request(Long id, String status) {
        return request(id, status, "330110198206214839", "310101201010287414");
    }

    private SalesBookingOrderSaveRequest requestWithGuests(
            Long id,
            String status,
            List<SalesBookingOrderGuestRequest> guests
    ) {
        return request(id, status, "330110198206214839", "310101201010287414", guests);
    }

    private SalesBookingOrderSaveRequest requestWithGuestCertificates(String firstCertificateNo, String secondCertificateNo) {
        return request(null, "confirmed", firstCertificateNo, secondCertificateNo);
    }

    private SalesBookingOrderSaveRequest request(
            Long id,
            String status,
            String firstCertificateNo,
            String secondCertificateNo
    ) {
        return request(id, status, firstCertificateNo, secondCertificateNo, List.of(
                new SalesBookingOrderGuestRequest(
                        null,
                        1,
                        "张三",
                        null,
                        firstCertificateNo,
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
                        secondCertificateNo,
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
        ));
    }

    private SalesBookingOrderSaveRequest request(
            Long id,
            String status,
            String firstCertificateNo,
            String secondCertificateNo,
            List<SalesBookingOrderGuestRequest> guests
    ) {
        return new SalesBookingOrderSaveRequest(
                id,
                1001L,
                "BY-ORDER-001",
                3001L,
                "杭州百缘旅行社",
                "叶菊莲",
                "13521124678",
                "客户团号-A",
                "原始订单信息：历史来源订单",
                31L,
                "张业务",
                32L,
                "王计调",
                null,
                null,
                null,
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
                null,
                status,
                List.of(new SalesBookingOrderPriceLineRequest(
                        null,
                        "adult",
                        "成人",
                        new BigDecimal("3000.00"),
                        new BigDecimal("3"),
                        true,
                        "成人价"
                ), new SalesBookingOrderPriceLineRequest(
                        null,
                        "surcharge",
                        "附加费",
                        new BigDecimal("180.00"),
                        new BigDecimal("3"),
                        false,
                        "按占位游客数量自动计入"
                )),
                guests
        );
    }

    private SalesBookingOrderGuestRequest guestRequest(Long id, Integer indexNo, String guestName, String guestType) {
        return new SalesBookingOrderGuestRequest(
                id,
                indexNo,
                guestName,
                null,
                null,
                null,
                "男",
                LocalDate.of(1982, 6, 21),
                44,
                "13521124678",
                guestType,
                "1房",
                "1大床（必须保证大床）",
                false,
                null
        );
    }

    private SalesBookingOrderGuestEntity existingGuest(Long id, String guestName, String guestType) {
        SalesBookingOrderGuestEntity guest = new SalesBookingOrderGuestEntity();
        guest.setId(id);
        guest.setTenantId(1L);
        guest.setOrderId(2001L);
        guest.setTeamId(1001L);
        guest.setIndexNo(id.equals(11L) ? 1 : 2);
        guest.setGuestName(guestName);
        guest.setGender("男");
        guest.setBirthDate(LocalDate.of(1982, 6, 21));
        guest.setAge(44);
        guest.setPhone("13521124678");
        guest.setGuestType(guestType);
        guest.setRoomGroup("1房");
        guest.setRoomRemark("1大床（必须保证大床）");
        guest.setLeaderFlag(false);
        guest.setIsDeleted(false);
        return guest;
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

    private SalesBookingOrderGuestEntity leaderGuest(Long orderId, String guestName, String phone) {
        SalesBookingOrderGuestEntity guest = new SalesBookingOrderGuestEntity();
        guest.setTenantId(1L);
        guest.setTeamId(1001L);
        guest.setOrderId(orderId);
        guest.setGuestName(guestName);
        guest.setPhone(phone);
        guest.setLeaderFlag(true);
        return guest;
    }
}
