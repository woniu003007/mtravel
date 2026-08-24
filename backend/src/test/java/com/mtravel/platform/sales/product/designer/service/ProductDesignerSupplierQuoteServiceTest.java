package com.mtravel.platform.sales.product.designer.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSupplierPriceRow;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 产品设计资源采购关系与报价解析测试。 */
class ProductDesignerSupplierQuoteServiceTest {

    @Test
    void resolveShouldSelectDefaultValidRelationAndPreferTeamPrice() {
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        ProductDesignerSupplierQuoteService service = new ProductDesignerSupplierQuoteService(relationMapper);
        PurchaseResourceEntity resource = requiredResource(31L);
        when(relationMapper.selectActiveResourceSupplierRows(eq(1L), anyList()))
                .thenReturn(List.of(
                        priceRow(31L, 102L, 52L, "备选车队", false, "unified",
                                new BigDecimal("680.00"), null, null, null, null),
                        priceRow(31L, 101L, 51L, "默认车队", true, "classified",
                                null, 9001L, new BigDecimal("128.00"), new BigDecimal("108.00"),
                                new BigDecimal("88.00"))
                ));

        ProductDesignerSupplierQuote quote = service.resolve(1L, resource, null, null);

        assertThat(quote.supplierRelationId()).isEqualTo(101L);
        assertThat(quote.supplierId()).isEqualTo(51L);
        assertThat(quote.supplierName()).isEqualTo("默认车队");
        assertThat(quote.priceMode()).isEqualTo("classified");
        assertThat(quote.unitPrice()).isEqualByComparingTo("88.00");
        assertThat(quote.pending()).isFalse();
    }

    @Test
    void resolveShouldReturnPendingWhenNoValidSupplierExistsAndNoneWasRequested() {
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        ProductDesignerSupplierQuoteService service = new ProductDesignerSupplierQuoteService(relationMapper);
        when(relationMapper.selectActiveResourceSupplierRows(eq(1L), anyList())).thenReturn(List.of());

        ProductDesignerSupplierQuote quote = service.resolve(1L, requiredResource(31L), null, null);

        assertThat(quote.supplierRelationId()).isNull();
        assertThat(quote.supplierId()).isNull();
        assertThat(quote.priceMode()).isEqualTo("pending");
        assertThat(quote.unitPrice()).isEqualByComparingTo("0.00");
        assertThat(quote.pending()).isTrue();
    }

    @Test
    void resolveShouldRejectExplicitRelationThatIsNotAnEligibleCandidate() {
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        ProductDesignerSupplierQuoteService service = new ProductDesignerSupplierQuoteService(relationMapper);
        when(relationMapper.selectActiveResourceSupplierRows(eq(1L), anyList()))
                .thenReturn(List.of(priceRow(31L, 101L, 51L, "默认车队", true, "unified",
                        new BigDecimal("680.00"), null, null, null, null)));

        assertThatThrownBy(() -> service.resolve(1L, requiredResource(31L), 999L, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("供应商采购关系");
    }

    @Test
    void resolveShouldUseLegacySupplierIdOnlyWhenItMatchesAnEligibleRelation() {
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        ProductDesignerSupplierQuoteService service = new ProductDesignerSupplierQuoteService(relationMapper);
        when(relationMapper.selectActiveResourceSupplierRows(eq(1L), anyList()))
                .thenReturn(List.of(
                        priceRow(31L, 101L, 51L, "默认车队", true, "unified",
                                new BigDecimal("680.00"), null, null, null, null),
                        priceRow(31L, 102L, 52L, "指定车队", false, "unified",
                                new BigDecimal("650.00"), null, null, null, null)
                ));

        ProductDesignerSupplierQuote quote = service.resolve(1L, requiredResource(31L), null, 52L);

        assertThat(quote.supplierRelationId()).isEqualTo(102L);
        assertThat(quote.unitPrice()).isEqualByComparingTo("650.00");
    }

    @Test
    void resolveShouldTreatClassifiedRowsWithoutAnyPriceAsPending() {
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        ProductDesignerSupplierQuoteService service = new ProductDesignerSupplierQuoteService(relationMapper);
        when(relationMapper.selectActiveResourceSupplierRows(eq(1L), anyList()))
                .thenReturn(List.of(priceRow(31L, 101L, 51L, "空报价车队", true, "classified",
                        null, 9001L, null, null, null)));

        ProductDesignerSupplierQuote quote = service.resolve(1L, requiredResource(31L), null, null);

        assertThat(quote.pending()).isTrue();
        assertThat(quote.unitPrice()).isEqualByComparingTo("0.00");
    }

    private PurchaseResourceEntity requiredResource(Long id) {
        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setId(id);
        resource.setResourceType("vehicle");
        resource.setProcurementMode("required");
        return resource;
    }

    private PurchaseRelationSupplierPriceRow priceRow(
            Long resourceId,
            Long relationId,
            Long supplierId,
            String supplierName,
            boolean defaultSupplier,
            String priceMode,
            BigDecimal unifiedPrice,
            Long resourceProjectId,
            BigDecimal marketPrice,
            BigDecimal peerPrice,
            BigDecimal teamPrice
    ) {
        PurchaseRelationSupplierPriceRow row = new PurchaseRelationSupplierPriceRow();
        row.setResourceId(resourceId);
        row.setRelationId(relationId);
        row.setSupplierId(supplierId);
        row.setSupplierName(supplierName);
        row.setDefaultSupplier(defaultSupplier);
        row.setPriceMode(priceMode);
        row.setUnifiedPrice(unifiedPrice);
        row.setResourceProjectId(resourceProjectId);
        row.setMarketPrice(marketPrice);
        row.setPeerPrice(peerPrice);
        row.setTeamPrice(teamPrice);
        return row;
    }
}
