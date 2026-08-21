package com.mtravel.platform.purchase.resource.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 资源介绍素材保存请求。 */
public record PurchaseResourceIntroductionSaveRequest(
        Boolean isOptionalItem,
        Long resourceOptionalItemId,
        @NotBlank(message = "介绍名称不能为空")
        @Size(max = 160, message = "介绍名称不能超过160个字符")
        String title,
        @Size(max = 10, message = "适用标签不能超过10个")
        List<@Size(max = 20, message = "单个标签不能超过20个字符") String> tags,
        @NotBlank(message = "介绍正文不能为空")
        @Size(max = 50000, message = "介绍正文不能超过50000个字符")
        String content,
        @Size(max = 5000, message = "注意事项不能超过5000个字符")
        String noticeContent,
        @Size(max = 5000, message = "温馨提示不能超过5000个字符")
        String warmTipContent,
        List<ResourceIntroductionExtensionBlock> extensionBlocks,
        @Pattern(regexp = "^$|\\d+", message = "游览时间只能填写分钟数字")
        @Size(max = 6, message = "游览时间不能超过6位分钟数字")
        String visitDuration
) {
    /** 兼容旧客户端未提交自费项目主档 ID 的素材请求。 */
    public PurchaseResourceIntroductionSaveRequest(Boolean isOptionalItem, String title, List<String> tags, String content, String noticeContent, String warmTipContent, String visitDuration) {
        this(isOptionalItem, null, title, tags, content, noticeContent, warmTipContent, List.of(), visitDuration);
    }

    /** 兼容已携带资源级自费项目 ID 的旧客户端和定向测试。 */
    public PurchaseResourceIntroductionSaveRequest(Boolean isOptionalItem, Long resourceOptionalItemId, String title, List<String> tags, String content, String noticeContent, String warmTipContent, String visitDuration) {
        this(isOptionalItem, resourceOptionalItemId, title, tags, content, noticeContent, warmTipContent, List.of(), visitDuration);
    }
}
