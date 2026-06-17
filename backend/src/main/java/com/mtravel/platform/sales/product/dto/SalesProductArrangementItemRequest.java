package com.mtravel.platform.sales.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 销售产品团队安排参数保存请求。
 *
 * @param arrangementType 安排类型，例如住宿、用车、景区
 * @param itemName 安排项目名称
 * @param arrangementContent 安排内容或默认说明
 * @param quantity 默认数量
 * @param unitPrice 默认单价或费用参考
 * @param unitName 计量单位
 * @param settlementType 结算类型，cash 现结，credit 挂账
 * @param remark 备注
 */
public record SalesProductArrangementItemRequest(
        @NotBlank(message = "团队安排类型不能为空")
        @Pattern(
                regexp = "traffic|hotel|vehicle|scenic|meal|other|optional|shopping|ground_agent|extra_fee",
                message = "团队安排类型不合法"
        )
        String arrangementType,
        @NotBlank(message = "安排项目名称不能为空") @Size(max = 160) String itemName,
        String arrangementContent,
        @DecimalMin(value = "0", message = "数量不能小于0") BigDecimal quantity,
        @DecimalMin(value = "0", message = "单价不能小于0") BigDecimal unitPrice,
        @Size(max = 40) String unitName,
        @Pattern(regexp = "cash|credit", message = "结算类型不合法") String settlementType,
        String remark
) {}
