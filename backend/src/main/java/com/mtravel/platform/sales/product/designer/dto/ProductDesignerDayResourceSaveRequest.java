package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 产品设计工作台单条每日资源保存请求。
 *
 * @param id 已存在的每日资源 ID，空表示新增
 * @param productId 产品 ID
 * @param dayNo 第几天，从 1 开始
 * @param resourceId 资源主档 ID
 * @param supplierId 供应商 ID，可为空；免费资源不需要供应商
 * @param selectedIntroductionId 选择的已发布资源介绍 ID，可为空
 * @param stayMinutes 停留分钟数
 * @param includeInWord 是否进入产品 Word
 * @param quantity 成本数量，默认 1
 * @param sortOrder 当天排序，空时自动排到最后
 * @param remark 备注
 * @param selectedImageIds 产品 Word 使用的资源图片 ID，空列表表示不选图；空值表示修改时保持原选择
 */
public record ProductDesignerDayResourceSaveRequest(
        Long id,
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "行程天数不能为空") @Min(value = 1, message = "行程天数必须从1开始") Integer dayNo,
        @NotNull(message = "资源ID不能为空") Long resourceId,
        Long supplierId,
        Long selectedIntroductionId,
        @Min(value = 0, message = "停留时长不能小于0") Integer stayMinutes,
        Boolean includeInWord,
        @DecimalMin(value = "0", message = "数量不能小于0") BigDecimal quantity,
        @Min(value = 1, message = "排序必须从1开始") Integer sortOrder,
        String remark,
        List<Long> selectedImageIds
) {}
