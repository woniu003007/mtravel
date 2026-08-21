package com.mtravel.platform.sales.product.designer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 单独保存每日资源介绍版本选择的请求。 */
public record ProductDesignerIntroductionSaveRequest(
        @NotNull(message = "产品ID不能为空") Long productId,
        @NotNull(message = "每日资源ID不能为空") Long dayResourceId,
        Long selectedIntroductionId,
        @JsonAlias({"selectedIntroductionIds", "introductionIds"}) List<Long> introductionIds
) {
    /** 兼容旧的单介绍保存接口调用。 */
    public ProductDesignerIntroductionSaveRequest(
            Long productId,
            Long dayResourceId,
            Long selectedIntroductionId
    ) {
        this(productId, dayResourceId, selectedIntroductionId, null);
    }
}
