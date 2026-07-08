package com.mtravel.platform.sales.booking.order.dto;

import java.time.LocalDate;

/**
 * 订单管理列表行响应。
 *
 * <p>该 DTO 面向销售管理 / 订单管理全局列表，只返回表格展示和跳转需要的字段，
 * 不携带订单详情、游客明细和完整价格明细对象。</p>
 *
 * @param id 订单 ID
 * @param teamId 所属团队 ID
 * @param orderNo 订单编号
 * @param teamNo 团号
 * @param teamType 团队类型值
 * @param teamTypeLabel 团队类型中文
 * @param departureDate 发团日期
 * @param productName 产品或团队名称
 * @param orderRole 订单角色
 * @param orderRoleLabel 订单角色中文
 * @param orderInfo 订单信息摘要
 * @param hotelInfo 酒店信息
 * @param pickupInfo 接站信息
 * @param dropoffInfo 送站信息
 * @param pickupRemark 接送备注
 * @param guideRemark 导游备注
 * @param sourcePlace 客源地
 * @param guestName 客人代表姓名
 * @param guestCount 订单人数
 * @param guestCountText 订单人数字符串
 * @param priceDetail 价格详情
 * @param receivableAmount 应收金额文本
 * @param receivedAmount 已收金额文本
 * @param balanceAmount 余额文本
 * @param feeRemark 费用说明
 * @param orderRemark 订单备注
 * @param bookingInfo 日期和预订人
 * @param status 订单状态中文
 * @param statusValue 订单状态值
 * @param tagging 是否已标记
 * @param hasOrderFile 是否有订单文件
 */
public record SalesBookingOrderManageRowResponse(
        Long id,
        Long teamId,
        String orderNo,
        String teamNo,
        String teamType,
        String teamTypeLabel,
        LocalDate departureDate,
        String productName,
        String orderRole,
        String orderRoleLabel,
        String orderInfo,
        String hotelInfo,
        String pickupInfo,
        String dropoffInfo,
        String pickupRemark,
        String guideRemark,
        String sourcePlace,
        String guestName,
        Integer guestCount,
        String guestCountText,
        String priceDetail,
        String receivableAmount,
        String receivedAmount,
        String balanceAmount,
        String feeRemark,
        String orderRemark,
        String bookingInfo,
        String status,
        String statusValue,
        Boolean tagging,
        Boolean hasOrderFile
) {
}
