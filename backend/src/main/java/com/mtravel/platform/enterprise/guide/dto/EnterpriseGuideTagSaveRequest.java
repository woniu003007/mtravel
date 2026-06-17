package com.mtravel.platform.enterprise.guide.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 导游标签保存请求。
 *
 * <p>用于新增和修改标签。标签只表达导游能力或分类，不承担导管绩效归属。</p>
 */
public record EnterpriseGuideTagSaveRequest(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 80, message = "标签名称最多80个字符")
        String tagName,

        @Min(value = 0, message = "排序不能小于0")
        Integer sortOrder,

        @Pattern(regexp = "active|disabled", message = "标签状态不合法")
        String status,

        String remark
) {}
