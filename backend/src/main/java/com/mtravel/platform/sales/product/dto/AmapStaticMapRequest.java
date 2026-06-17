package com.mtravel.platform.sales.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 高德静态地图预览请求。
 *
 * @param points 路书点位，允许只传一个地点用于显示单点地图
 */
public record AmapStaticMapRequest(
        @NotNull(message = "地图点位不能为空")
        @Size(min = 1, max = 16, message = "地图点位数量必须在1到16之间")
        List<@Valid AmapRoutePointRequest> points
) {}
