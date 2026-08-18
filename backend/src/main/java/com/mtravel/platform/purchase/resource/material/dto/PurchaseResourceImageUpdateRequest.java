package com.mtravel.platform.purchase.resource.material.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/** 资源图片标签和排序更新请求。 */
public record PurchaseResourceImageUpdateRequest(
        @Size(max = 10, message = "图片标签不能超过10个")
        List<@Size(max = 20, message = "单个标签不能超过20个字符") String> tags,
        @jakarta.validation.constraints.Min(value = 0, message = "排序值不能小于0")
        Integer sortOrder
) {
}
