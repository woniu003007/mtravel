package com.mtravel.platform.enterprise.productdictionary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 产品字典保存请求。
 *
 * @param dictType 字典类型，区分业务类型、接待标准和产品主题
 * @param dictName 字典名称，展示给产品模板选择
 * @param sortOrder 排序号，数字越小越靠前
 * @param status 状态，active 启用，disabled 停用
 * @param remark 备注说明
 */
public record EnterpriseProductDictionarySaveRequest(
        @NotBlank(message = "字典类型不能为空")
        @Pattern(
                regexp = "business_type|reception_standard|product_theme",
                message = "产品字典类型不合法"
        )
        String dictType,
        @NotBlank(message = "字典名称不能为空") @Size(max = 120) String dictName,
        @Min(value = 0, message = "排序号不能小于0") Integer sortOrder,
        @Pattern(regexp = "active|disabled", message = "产品字典状态不合法") String status,
        String remark
) {}
