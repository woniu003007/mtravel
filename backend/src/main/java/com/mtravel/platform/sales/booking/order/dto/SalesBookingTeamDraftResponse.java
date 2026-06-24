package com.mtravel.platform.sales.booking.order.dto;

import com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse;

/**
 * 新增收客订单页面团队草稿返回对象。
 *
 * <p>前端新增订单时先读取团队、产品、价格和行程摘要，用于自动带出旧系统收客页的基础信息。</p>
 */
public record SalesBookingTeamDraftResponse(
        SalesTeamOperationResponse.TeamInfo team,
        SalesTeamOperationResponse.ProductInfo product,
        SalesTeamOperationResponse.ContentInfo content
) {
}
