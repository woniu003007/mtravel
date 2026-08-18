package com.mtravel.platform.purchase.resource.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 景区资源页快捷新增供应商请求。
 *
 * <p>该入口只服务于景区资源页的快速录入场景，供应商分类由后端固定为 scenic，避免前端误传
 * 其它分类或把资源页入口变成通用供应商管理页面。</p>
 */
public record ScenicSupplierCreateRequest(
        @NotBlank(message = "供应商名称不能为空")
        @Size(max = 200, message = "供应商名称不能超过200个字符")
        String supplierName,

        @Size(max = 80, message = "省份不能超过80个字符")
        String province,

        @Size(max = 80, message = "城市不能超过80个字符")
        String city,

        @Size(max = 80, message = "区县不能超过80个字符")
        String district,

        @Size(max = 4000, message = "基础信息不能超过4000个字符")
        String basicInfo,

        @Size(max = 80, message = "联系人不能超过80个字符")
        String contactName,

        @Size(max = 40, message = "联系电话不能超过40个字符")
        String contactPhone,

        @Pattern(regexp = "active|disabled|blacklisted", message = "供应商状态不合法")
        String status,

        Boolean isDefault,

        @NotNull(message = "报价模式不能为空")
        @Pattern(regexp = "unified|classified", message = "报价模式不合法")
        String priceMode,

        @DecimalMin(value = "0.00", message = "统一报价不能小于0")
        BigDecimal unifiedPrice,

        @DecimalMin(value = "0.00", message = "成人票价格不能小于0")
        BigDecimal adultPrice,

        @DecimalMin(value = "0.00", message = "儿童票价格不能小于0")
        BigDecimal childPrice,

        @DecimalMin(value = "0.00", message = "学生票价格不能小于0")
        BigDecimal studentPrice,

        @DecimalMin(value = "0.00", message = "老人票价格不能小于0")
        BigDecimal seniorPrice,

        @DecimalMin(value = "0.00", message = "优待票价格不能小于0")
        BigDecimal preferentialPrice,

        @Size(max = 500, message = "报价备注不能超过500个字符")
        String priceRemark,

        @Size(max = 1000, message = "备注不能超过1000个字符")
        String remark
) {}
