package com.mtravel.platform.sales.product.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 销售产品团队安排轻量保存请求。
 *
 * <p>团队安排独立页面只提交安排明细，不要求产品名称、行程内容和产品说明，避免弹窗保存被完整产品 DTO 校验拦截。</p>
 *
 * @param arrangementItems 团队安排明细
 */
public record SalesProductArrangementUpdateRequest(
        List<@Valid SalesProductArrangementItemRequest> arrangementItems
) {}
