package com.mtravel.platform.sales.product.designer.service;

import java.math.BigDecimal;

/**
 * 产品设计保存时冻结的供应商报价选择结果。
 *
 * <p>该对象只承载已校验的采购关系快照；待询价资源没有供应商关系，成本按零处理。</p>
 */
public record ProductDesignerSupplierQuote(
        Long supplierRelationId,
        Long supplierId,
        String supplierName,
        String priceMode,
        BigDecimal unitPrice,
        boolean pending
) {

    /** 当前资源无需采购，因而不需要供应商关系或报价。 */
    public static ProductDesignerSupplierQuote notRequired() {
        return new ProductDesignerSupplierQuote(null, null, null, "not_required", BigDecimal.ZERO.setScale(2), false);
    }

    /** 资源需要采购但尚无可计算报价时的可编排状态。 */
    public static ProductDesignerSupplierQuote pendingQuote() {
        return new ProductDesignerSupplierQuote(null, null, null, "pending", BigDecimal.ZERO.setScale(2), true);
    }
}
