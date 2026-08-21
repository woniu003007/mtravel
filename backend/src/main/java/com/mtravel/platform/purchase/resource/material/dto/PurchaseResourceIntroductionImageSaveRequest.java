package com.mtravel.platform.purchase.resource.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 保存介绍素材选用图片的请求，空数组表示清空当前素材图片。 */
public record PurchaseResourceIntroductionImageSaveRequest(
        @NotNull(message = "图片ID列表不能为空")
        @Size(max = 30, message = "每份介绍素材最多选择30张图片")
        List<Long> imageIds
) {
}
