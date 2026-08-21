package com.mtravel.platform.purchase.resource.material.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 当前资源全部未删除介绍素材的目标排序。 */
public record PurchaseResourceIntroductionReorderRequest(
        @NotNull(message = "介绍素材排序不能为空")
        List<@NotNull(message = "介绍素材ID不能为空") Long> introductionIds
) {
}
