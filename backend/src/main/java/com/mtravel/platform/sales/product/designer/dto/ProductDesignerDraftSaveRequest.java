package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 产品设计草稿基础信息保存请求。
 *
 * <p>草稿只保存地图编排必需的产品基础信息，不直接进入产品管理。</p>
 *
 * @param productName 产品名称
 * @param businessType 业务类型
 * @param domesticInternational 国内国际标记
 * @param province 接团省份
 * @param city 接团城市
 * @param district 接团区县
 * @param receptionStandard 接待标准
 * @param productTheme 产品主题
 * @param travelDays 旅游天数
 * @param remark 设计备注
 */
public record ProductDesignerDraftSaveRequest(
        @NotBlank(message = "产品名称不能为空") @Size(max = 200)
        String productName,

        @Size(max = 120)
        String businessType,

        @Pattern(regexp = "domestic|international", message = "国内国际类型不合法")
        String domesticInternational,

        @Size(max = 80)
        String province,

        @Size(max = 80)
        String city,

        @Size(max = 80)
        String district,

        @Size(max = 120)
        String receptionStandard,

        @Size(max = 120)
        String productTheme,

        @Min(value = 1, message = "旅游天数不能小于1")
        Integer travelDays,

        String remark
) {}
