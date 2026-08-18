package com.mtravel.platform.purchase.resource.dto;

import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import java.math.BigDecimal;
import java.util.List;
import java.time.OffsetDateTime;

/**
 * 资源已绑定供应商返回对象。
 *
 * <p>用于资源总览点击“已绑定 X 家”时查看绑定关系，同时返回资源页编辑所需的报价快照。</p>
 */
public record PurchaseResourceBindingResponse(
        Long relationId,
        Long supplierId,
        String supplierName,
        Boolean isDefault,
        Integer groupQuantity,
        String status,
        OffsetDateTime createdAt,
        String priceMode,
        BigDecimal unifiedPrice,
        List<ResourceSupplierPriceLineResponse> priceLines,
        String priceRemark
) {
    /** 将采购关系和供应商名称组装为资源绑定行。 */
    public static PurchaseResourceBindingResponse fromEntity(PurchaseRelationEntity entity, String supplierName) {
        return fromEntity(entity, supplierName, List.of());
    }

    /** 将采购关系、供应商和报价明细组装为资源绑定行。 */
    public static PurchaseResourceBindingResponse fromEntity(
            PurchaseRelationEntity entity,
            String supplierName,
            List<ResourceSupplierPriceLineResponse> priceLines
    ) {
        List<ResourceSupplierPriceLineResponse> lines = priceLines == null ? List.of() : List.copyOf(priceLines);
        String mode = entity.getPriceMode();
        if (mode == null || mode.isBlank()) {
            mode = inferPriceMode(lines);
        }
        BigDecimal unifiedPrice = "unified".equals(mode) ? entity.getUnifiedPrice() : null;
        List<String> remarks = lines.stream()
                .map(ResourceSupplierPriceLineResponse::priceDescription)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        String priceRemark = "unified".equals(mode)
                ? entity.getPriceRemark()
                : remarks.size() == 1 ? remarks.get(0) : null;
        return new PurchaseResourceBindingResponse(
                entity.getId(),
                entity.getSupplierId(),
                supplierName,
                Boolean.TRUE.equals(entity.getIsDefault()),
                entity.getGroupQuantity(),
                entity.getStatus(),
                entity.getCreatedAt(),
                mode,
                unifiedPrice,
                lines,
                priceRemark
        );
    }

    /** 兼容旧关系：所有明细团队价相同时按统一报价回显，否则按分类报价回显。 */
    private static String inferPriceMode(List<ResourceSupplierPriceLineResponse> lines) {
        if (lines.isEmpty()) {
            return "classified";
        }
        BigDecimal first = lines.get(0).teamPrice();
        return first != null && lines.stream().allMatch(line -> line.teamPrice() != null
                && first.compareTo(line.teamPrice()) == 0)
                ? "unified"
                : "classified";
    }
}
