package com.mtravel.platform.sales.product.designer.dto;

import java.math.BigDecimal;

/**
 * 产品 Word 编排使用的自费项目候选。
 *
 * <p>候选以资源自费主档和已发布的关联介绍为基础，不依赖当天是否已选择供应商；
 * 如存在有效关系报价，则附带一个优先建议价和对应报价 ID 供保存时复用。</p>
 */
public record ProductDesignerWordOptionalItemCandidateResponse(
        Long resourceOptionalItemId,
        String projectName,
        String optionalItemType,
        Long introductionId,
        String introductionTitle,
        Long supplierOptionalItemId,
        BigDecimal suggestedSalePrice
) {}
