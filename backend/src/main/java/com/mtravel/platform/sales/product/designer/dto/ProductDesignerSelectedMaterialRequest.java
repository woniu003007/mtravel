package com.mtravel.platform.sales.product.designer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 产品每日资源中一个按最终 Word 顺序选用的素材。
 *
 * <p>数组位置即全局素材顺序；普通介绍和自费项目共用这一顺序，避免前端分别排序后丢失交叉顺序。</p>
 */
public record ProductDesignerSelectedMaterialRequest(
        @NotBlank String materialType,
        Long introductionId,
        Long resourceOptionalItemId,
        Long supplierOptionalItemId,
        @DecimalMin(value = "0", message = "自费项目对外价不能小于0") BigDecimal salePrice
) {}
