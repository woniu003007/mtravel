package com.mtravel.platform.purchase.resource.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 资源介绍素材保存请求。 */
public record PurchaseResourceIntroductionSaveRequest(
        @NotBlank(message = "介绍名称不能为空")
        @Size(max = 160, message = "介绍名称不能超过160个字符")
        String title,
        @Size(max = 10, message = "适用标签不能超过10个")
        List<@Size(max = 20, message = "单个标签不能超过20个字符") String> tags,
        @NotBlank(message = "介绍正文不能为空")
        @Size(max = 50000, message = "介绍正文不能超过50000个字符")
        String content,
        @Size(max = 5000, message = "注意事项不能超过5000个字符")
        String noticeContent
) {
}
