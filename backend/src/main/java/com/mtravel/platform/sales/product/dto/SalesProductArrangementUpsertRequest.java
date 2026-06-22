package com.mtravel.platform.sales.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 销售产品单条团队安排保存请求。
 *
 * <p>用于团队安排弹窗的新增或修改，只处理当前一条安排及其价格明细、用车报价快照、询价记录。</p>
 *
 * @param arrangementId 当前安排 ID。为空表示新增，有值表示替换该安排
 * @param item 安排明细
 */
public record SalesProductArrangementUpsertRequest(
        Long arrangementId,
        @NotNull(message = "团队安排明细不能为空") @Valid SalesProductArrangementItemRequest item
) {}
