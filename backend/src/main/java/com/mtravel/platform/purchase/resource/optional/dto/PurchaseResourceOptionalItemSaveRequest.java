package com.mtravel.platform.purchase.resource.optional.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 资源级自费项目保存请求。 */
public record PurchaseResourceOptionalItemSaveRequest(
        @NotBlank(message = "自费项目名称不能为空") @Size(max = 200) String projectName,
        @Pattern(regexp = "scenic_transport|recommended_self_pay", message = "自费项目类型不合法") String optionalItemType,
        @Pattern(regexp = "active|disabled", message = "自费项目状态不合法") String status
) {}
