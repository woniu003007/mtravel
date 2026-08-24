package com.mtravel.platform.sales.product.designer.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSupplierPriceRow;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 为产品设计工作台解析可冻结的供应商报价。
 *
 * <p>日资源与产品级用车都必须经过同一入口，确保默认供应商、显式关系校验、待询价和分类报价的
 * 判定完全一致。</p>
 */
@Service
public class ProductDesignerSupplierQuoteService {

    private static final String PROCUREMENT_NOT_REQUIRED = "not_required";
    private static final String PRICE_MODE_UNIFIED = "unified";
    private static final String PRICE_MODE_CLASSIFIED = "classified";
    private static final String PRICE_MODE_PENDING = "pending";

    private final PurchaseRelationMapper relationMapper;

    private record SupplierCandidate(ProductDesignerSupplierQuote quote, boolean defaultSupplier) {}

    public ProductDesignerSupplierQuoteService(PurchaseRelationMapper relationMapper) {
        this.relationMapper = relationMapper;
    }

    /**
     * 解析资源当前可用的供应商报价。
     *
     * <p>未显式指定供应商时优先选择启用的默认关系；没有合格关系则返回待询价。显式提交的采购
     * 关系或兼容供应商 ID 不能回退到其它关系，避免用户以为已选中的供应商被静默替换。</p>
     */
    public ProductDesignerSupplierQuote resolve(
            Long tenantId,
            PurchaseResourceEntity resource,
            Long supplierRelationId,
            Long supplierId
    ) {
        if (resource == null || resource.getId() == null) {
            throw new BizException("采购资源不能为空");
        }
        if (PROCUREMENT_NOT_REQUIRED.equals(resource.getProcurementMode())) {
            if (supplierRelationId != null || supplierId != null) {
                throw new BizException("无需采购的资源不能选择供应商");
            }
            return ProductDesignerSupplierQuote.notRequired();
        }

        List<ProductDesignerSupplierQuote> candidates = candidates(tenantId, resource);
        if (supplierRelationId != null) {
            return candidates.stream()
                    .filter(candidate -> Objects.equals(candidate.supplierRelationId(), supplierRelationId))
                    .findFirst()
                    .orElseThrow(() -> new BizException("供应商采购关系无效、已停用或没有可计算报价"));
        }
        if (supplierId != null) {
            return candidates.stream()
                    .filter(candidate -> Objects.equals(candidate.supplierId(), supplierId))
                    .findFirst()
                    .orElseThrow(() -> new BizException("供应商未绑定当前资源、已停用或没有可计算报价"));
        }
        return candidates.isEmpty() ? ProductDesignerSupplierQuote.pendingQuote() : candidates.getFirst();
    }

    private List<ProductDesignerSupplierQuote> candidates(Long tenantId, PurchaseResourceEntity resource) {
        Long resourceId = resource.getId();
        List<PurchaseRelationSupplierPriceRow> rows = "vehicle".equals(resource.getResourceType())
                ? relationMapper.selectActiveResourceSupplierRows(tenantId, List.of(resourceId))
                : relationMapper.selectActiveResourceSupplierPriceRows(tenantId, List.of(resourceId));
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<PurchaseRelationSupplierPriceRow>> byRelation = new LinkedHashMap<>();
        for (PurchaseRelationSupplierPriceRow row : rows) {
            if (row == null || !Objects.equals(resourceId, row.getResourceId()) || row.getRelationId() == null) {
                continue;
            }
            byRelation.computeIfAbsent(row.getRelationId(), ignored -> new ArrayList<>()).add(row);
        }
        List<SupplierCandidate> candidates = new ArrayList<>();
        for (List<PurchaseRelationSupplierPriceRow> relationRows : byRelation.values()) {
            toCandidate(relationRows).ifPresent(candidates::add);
        }
        candidates.sort((left, right) -> Boolean.compare(right.defaultSupplier(), left.defaultSupplier()));
        return candidates.stream().map(SupplierCandidate::quote).toList();
    }

    private java.util.Optional<SupplierCandidate> toCandidate(
            List<PurchaseRelationSupplierPriceRow> relationRows
    ) {
        if (relationRows == null || relationRows.isEmpty()) {
            return java.util.Optional.empty();
        }
        PurchaseRelationSupplierPriceRow relation = relationRows.getFirst();
        BigDecimal unitPrice;
        if (PRICE_MODE_UNIFIED.equals(relation.getPriceMode())) {
            if (!nonNegative(relation.getUnifiedPrice())) {
                return java.util.Optional.of(new SupplierCandidate(
                        new ProductDesignerSupplierQuote(
                                relation.getRelationId(),
                                relation.getSupplierId(),
                                relation.getSupplierName(),
                                PRICE_MODE_PENDING,
                                BigDecimal.ZERO,
                                true
                        ),
                        Boolean.TRUE.equals(relation.getDefaultSupplier())
                ));
            }
            unitPrice = money(relation.getUnifiedPrice());
        } else if (PRICE_MODE_CLASSIFIED.equals(relation.getPriceMode())) {
            unitPrice = relationRows.stream()
                    .map(this::bestLinePrice)
                    .filter(Objects::nonNull)
                    .filter(this::nonNegative)
                    .findFirst()
                    .map(this::money)
                    .orElse(null);
            if (unitPrice == null) {
                return java.util.Optional.of(new SupplierCandidate(
                        new ProductDesignerSupplierQuote(
                                relation.getRelationId(),
                                relation.getSupplierId(),
                                relation.getSupplierName(),
                                PRICE_MODE_PENDING,
                                BigDecimal.ZERO,
                                true
                        ),
                        Boolean.TRUE.equals(relation.getDefaultSupplier())
                ));
            }
        } else {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new SupplierCandidate(
                new ProductDesignerSupplierQuote(
                        relation.getRelationId(),
                        relation.getSupplierId(),
                        relation.getSupplierName(),
                        relation.getPriceMode(),
                        unitPrice,
                        false
                ),
                Boolean.TRUE.equals(relation.getDefaultSupplier())
        ));
    }

    private BigDecimal bestLinePrice(PurchaseRelationSupplierPriceRow row) {
        if (row.getTeamPrice() != null) {
            return row.getTeamPrice();
        }
        if (row.getPeerPrice() != null) {
            return row.getPeerPrice();
        }
        return row.getMarketPrice();
    }

    private boolean nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
