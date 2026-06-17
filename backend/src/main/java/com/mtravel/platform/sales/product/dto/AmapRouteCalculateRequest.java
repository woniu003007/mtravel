package com.mtravel.platform.sales.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 高德驾车路线计算请求。
 *
 * @param points 路线点位，至少包含起点和终点，中间点按顺序作为途经点
 */
public record AmapRouteCalculateRequest(
        @NotNull(message = "路线点不能为空")
        @Size(min = 2, max = 16, message = "路线点数量必须在2到16之间")
        List<@Valid AmapRoutePointRequest> points
) {}
