package com.mtravel.platform.purchase.relation.price.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * 供应商资源价格保存请求。
 *
 * @param relationId 采购关系 ID
 * @param resourceProjectId 费用项目 ID
 * @param marketPrice 门市价
 * @param peerPrice 同行价
 * @param teamPrice 团队价
 * @param priceDescription 价格说明
 * @param status 状态，active 启用，disabled 停用
 * @param remark 备注
 */
@Schema(name = "SupplierResourcePriceSaveRequest", description = "供应商资源价格保存请求")
public record SupplierResourcePriceSaveRequest(
        @Schema(description = "采购关系 ID，对应供应商与资源/项目的采购关系", example = "1001")
        @NotNull(message = "采购关系不能为空")
        Long relationId,

        @Schema(description = "费用项目或资源项目 ID，例如门票、房费、车费等项目", example = "2001")
        @NotNull(message = "项目类型不能为空")
        Long resourceProjectId,

        @Schema(description = "门市价，面向终端游客或公开报价的参考价格", example = "298.00")
        @DecimalMin(value = "0.00", message = "门市价不能小于0")
        BigDecimal marketPrice,

        @Schema(description = "同行价，面向同业客户的采购或结算参考价格", example = "238.00")
        @DecimalMin(value = "0.00", message = "同行价不能小于0")
        BigDecimal peerPrice,

        @Schema(description = "团队价，团队执行时优先使用的成本或采购价格", example = "218.00")
        @DecimalMin(value = "0.00", message = "团队价不能小于0")
        BigDecimal teamPrice,

        @Schema(description = "价格说明，例如淡旺季规则、儿童价规则、节假日加价说明", example = "平日团队价，节假日另议")
        String priceDescription,

        @Schema(description = "价格状态：active 启用，disabled 停用", example = "active", allowableValues = {"active", "disabled"})
        @Pattern(regexp = "active|disabled", message = "价格状态不合法")
        String status,

        @Schema(description = "内部备注", example = "供应商 2026 年报价")
        String remark
) {}
