package com.mtravel.platform.sales.booking.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 收客订单保存请求。
 *
 * <p>字段顺序按旧系统收客页主要分区组织：订单信息、行程说明、导游相关、客户信息、
 * 酒店信息、附加说明、价格明细和游客名单。</p>
 *
 * @param id 订单 ID，新增为空，修改传值。
 * @param teamId 所属团队 ID。
 * @param orderNo 订单编号，可为空；新增为空时系统自动生成。
 * @param customerId 客户单位 ID。
 * @param customerName 客户单位名称快照。
 * @param contactName 客户联系人。
 * @param contactPhone 联系电话。
 * @param customerTeamNo 客户方团队编号。
 * @param originalOrderInfo 原始订单摘要，用于拼团、转团或来源订单追溯。
 * @param salespersonEmployeeId 业务员员工 ID。
 * @param salespersonEmployeeName 业务员姓名快照。
 * @param bookingOperatorEmployeeId 收客计调员工 ID。
 * @param bookingOperatorEmployeeName 收客计调姓名快照。
 * @param sourceProvince 客源地省份。
 * @param sourceCity 客源地城市。
 * @param sourceDistrict 客源地区县。
 * @param travelDescription 行程说明。
 * @param pickupInfo 接站或接机信息。
 * @param dropoffInfo 送站或送机信息。
 * @param pickupRemark 接送备注。
 * @param guideName 导游姓名。
 * @param guidePhone 导游电话。
 * @param guideRemark 导游相关备注。
 * @param hotelInfo 酒店信息。
 * @param feeRemark 费用说明。
 * @param confirmRemark 确认说明。
 * @param orderRemark 订单备注。
 * @param receivedAmount 已收金额。
 * @param riskApprovalRequestId 客户风控审批申请 ID。合同到期或授信超限且系统开启强制审批时必填。
 * @param status 订单状态，pending、confirmed、cancelled。
 * @param priceLines 价格明细。
 * @param guests 游客名单。
 */
public record SalesBookingOrderSaveRequest(
        Long id,
        @NotNull(message = "团队ID不能为空")
        Long teamId,
        @Size(max = 80, message = "订单编号不能超过80个字符")
        String orderNo,
        Long customerId,
        @Size(max = 200, message = "客户名称不能超过200个字符")
        String customerName,
        @Size(max = 80, message = "联系人不能超过80个字符")
        String contactName,
        @Size(max = 40, message = "联系电话不能超过40个字符")
        String contactPhone,
        @Size(max = 120, message = "客户团号不能超过120个字符")
        String customerTeamNo,
        String originalOrderInfo,
        Long salespersonEmployeeId,
        @Size(max = 100, message = "业务员姓名不能超过100个字符")
        String salespersonEmployeeName,
        Long bookingOperatorEmployeeId,
        @Size(max = 100, message = "收客计调姓名不能超过100个字符")
        String bookingOperatorEmployeeName,
        @Size(max = 80, message = "省份不能超过80个字符")
        String sourceProvince,
        @Size(max = 80, message = "城市不能超过80个字符")
        String sourceCity,
        @Size(max = 80, message = "区县不能超过80个字符")
        String sourceDistrict,
        String travelDescription,
        String pickupInfo,
        String dropoffInfo,
        String pickupRemark,
        @Size(max = 80, message = "导游姓名不能超过80个字符")
        String guideName,
        @Size(max = 40, message = "导游电话不能超过40个字符")
        String guidePhone,
        String guideRemark,
        String hotelInfo,
        String feeRemark,
        String confirmRemark,
        String orderRemark,
        @DecimalMin(value = "0.00", message = "已收金额不能小于0")
        BigDecimal receivedAmount,
        Long riskApprovalRequestId,
        String status,
        @Valid
        List<SalesBookingOrderPriceLineRequest> priceLines,
        @Valid
        List<SalesBookingOrderGuestRequest> guests
) {
}
