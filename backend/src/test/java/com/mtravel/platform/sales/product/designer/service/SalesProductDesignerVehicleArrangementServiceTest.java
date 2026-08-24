package com.mtravel.platform.sales.product.designer.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerVehicleArrangementDeleteRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerVehicleArrangementReorderRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerVehicleArrangementSaveRequest;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDesignerVehicleArrangementEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDesignerVehicleArrangementMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 产品设计产品级全程用车快照测试。 */
class SalesProductDesignerVehicleArrangementServiceTest {

    @Test
    void saveShouldSnapshotDefaultValidVehicleSupplierAndCost() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDesignerVehicleArrangementMapper vehicleMapper = mock(
                SalesProductDesignerVehicleArrangementMapper.class);
        ProductDesignerSupplierQuoteService quoteService = mock(ProductDesignerSupplierQuoteService.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                productMapper, resourceMapper, vehicleMapper, quoteService);
        PurchaseResourceEntity resource = vehicleResource(21L, "39座旅游大巴");
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct(4));
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource);
        when(quoteService.resolve(eq(1L), eq(resource), eq(null), eq(null))).thenReturn(
                new ProductDesignerSupplierQuote(301L, 71L, "南京车队", "unified",
                        new BigDecimal("500.00"), false));
        when(vehicleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(vehicleMapper.insert(any(SalesProductDesignerVehicleArrangementEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SalesProductDesignerVehicleArrangementEntity.class).setId(901L);
            return 1;
        });

        var response = service.save(1L, new ProductDesignerVehicleArrangementSaveRequest(
                null, 88L, 21L, null, 1, 4, new BigDecimal("2"), null, null, "全程包车"), "admin");

        assertThat(response.id()).isEqualTo(901L);
        assertThat(response.resourceName()).isEqualTo("39座旅游大巴");
        assertThat(response.supplierRelationId()).isEqualTo(301L);
        assertThat(response.supplierName()).isEqualTo("南京车队");
        assertThat(response.vehicleType()).isEqualTo("39座旅游大巴");
        assertThat(response.unitPrice()).isEqualByComparingTo("500.00");
        assertThat(response.costAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.procurementStatus()).isEqualTo("quoted");

        ArgumentCaptor<SalesProductDesignerVehicleArrangementEntity> captor = ArgumentCaptor.forClass(
                SalesProductDesignerVehicleArrangementEntity.class);
        verify(vehicleMapper).insert(captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo(88L);
        assertThat(captor.getValue().getSupplierRelationIdSnapshot()).isEqualTo(301L);
        assertThat(captor.getValue().getCostAmountSnapshot()).isEqualByComparingTo("1000.00");
    }

    @Test
    void saveShouldKeepVehiclePendingWhenNoValidSupplierExists() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDesignerVehicleArrangementMapper vehicleMapper = mock(
                SalesProductDesignerVehicleArrangementMapper.class);
        ProductDesignerSupplierQuoteService quoteService = mock(ProductDesignerSupplierQuoteService.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                productMapper, resourceMapper, vehicleMapper, quoteService);
        PurchaseResourceEntity resource = vehicleResource(21L, "19座中巴");
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct(2));
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource);
        when(quoteService.resolve(eq(1L), eq(resource), eq(null), eq(null)))
                .thenReturn(ProductDesignerSupplierQuote.pendingQuote());
        when(vehicleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(vehicleMapper.insert(any(SalesProductDesignerVehicleArrangementEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SalesProductDesignerVehicleArrangementEntity.class).setId(902L);
            return 1;
        });

        var response = service.save(1L, new ProductDesignerVehicleArrangementSaveRequest(
                null, 88L, 21L, null, 1, 2, BigDecimal.ONE, null, null, null), "admin");

        assertThat(response.supplierRelationId()).isNull();
        assertThat(response.supplierName()).isNull();
        assertThat(response.unitPrice()).isEqualByComparingTo("0.00");
        assertThat(response.costAmount()).isEqualByComparingTo("0.00");
        assertThat(response.procurementStatus()).isEqualTo("pending");
    }

    @Test
    void saveShouldKeepNotRequiredVehicleOutOfPendingProcurement() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDesignerVehicleArrangementMapper vehicleMapper = mock(
                SalesProductDesignerVehicleArrangementMapper.class);
        ProductDesignerSupplierQuoteService quoteService = mock(ProductDesignerSupplierQuoteService.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                productMapper, resourceMapper, vehicleMapper, quoteService);
        PurchaseResourceEntity resource = vehicleResource(21L, "摆渡车");
        resource.setProcurementMode("not_required");
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct(2));
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource);
        when(quoteService.resolve(eq(1L), eq(resource), eq(null), eq(null)))
                .thenReturn(ProductDesignerSupplierQuote.notRequired());
        when(vehicleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(vehicleMapper.insert(any(SalesProductDesignerVehicleArrangementEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SalesProductDesignerVehicleArrangementEntity.class).setId(903L);
            return 1;
        });

        var response = service.save(1L, new ProductDesignerVehicleArrangementSaveRequest(
                null, 88L, 21L, null, 1, 2, BigDecimal.ONE, null, null, null), "admin");

        assertThat(response.priceMode()).isEqualTo("not_required");
        assertThat(response.procurementStatus()).isEqualTo("not_required");
        assertThat(response.supplierRelationId()).isNull();
        assertThat(response.costAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void saveShouldRejectNonVehicleResource() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                productMapper, resourceMapper, mock(SalesProductDesignerVehicleArrangementMapper.class),
                mock(ProductDesignerSupplierQuoteService.class));
        PurchaseResourceEntity scenic = vehicleResource(21L, "夫子庙");
        scenic.setResourceType("scenic");
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct(2));
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(scenic);

        assertThatThrownBy(() -> service.save(1L, new ProductDesignerVehicleArrangementSaveRequest(
                null, 88L, 21L, null, 1, 2, BigDecimal.ONE, null, null, null), "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用车资源");
    }

    @Test
    void saveShouldRejectInvalidProductDayRange() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                productMapper, mock(PurchaseResourceMapper.class), mock(SalesProductDesignerVehicleArrangementMapper.class),
                mock(ProductDesignerSupplierQuoteService.class));
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct(3));

        assertThatThrownBy(() -> service.save(1L, new ProductDesignerVehicleArrangementSaveRequest(
                null, 88L, null, null, 1, 4, BigDecimal.ONE, "39座旅游大巴", null, null), "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("行程天数");
    }

    @Test
    void reorderShouldRejectVehicleIdsOutsideProduct() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDesignerVehicleArrangementMapper vehicleMapper = mock(
                SalesProductDesignerVehicleArrangementMapper.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                productMapper, mock(PurchaseResourceMapper.class), vehicleMapper,
                mock(ProductDesignerSupplierQuoteService.class));
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct(2));
        when(vehicleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(vehicleArrangement(501L, 88L, 1)));

        assertThatThrownBy(() -> service.reorder(1L,
                new ProductDesignerVehicleArrangementReorderRequest(88L, List.of(501L, 999L))))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不属于当前产品");
        verify(vehicleMapper, never()).update(any(SalesProductDesignerVehicleArrangementEntity.class), any(Wrapper.class));
    }

    @Test
    void softDeleteForProductShouldOnlyMarkActiveVehicleArrangementsDeleted() {
        SalesProductDesignerVehicleArrangementMapper vehicleMapper = mock(
                SalesProductDesignerVehicleArrangementMapper.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                mock(SalesProductMapper.class), mock(PurchaseResourceMapper.class), vehicleMapper,
                mock(ProductDesignerSupplierQuoteService.class));

        service.softDeleteForProduct(1L, 88L, "admin");

        ArgumentCaptor<SalesProductDesignerVehicleArrangementEntity> captor = ArgumentCaptor.forClass(
                SalesProductDesignerVehicleArrangementEntity.class);
        verify(vehicleMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
    }

    @Test
    void deleteShouldSoftDeleteOnlyRequestedVehicleArrangement() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDesignerVehicleArrangementMapper vehicleMapper = mock(
                SalesProductDesignerVehicleArrangementMapper.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                productMapper, mock(PurchaseResourceMapper.class), vehicleMapper,
                mock(ProductDesignerSupplierQuoteService.class));
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct(2));
        when(vehicleMapper.selectOne(any(Wrapper.class))).thenReturn(vehicleArrangement(501L, 88L, 1));
        when(vehicleMapper.update(any(SalesProductDesignerVehicleArrangementEntity.class), any(Wrapper.class)))
                .thenReturn(1);

        service.delete(1L, new ProductDesignerVehicleArrangementDeleteRequest(88L, 501L), "admin");

        ArgumentCaptor<SalesProductDesignerVehicleArrangementEntity> captor = ArgumentCaptor.forClass(
                SalesProductDesignerVehicleArrangementEntity.class);
        verify(vehicleMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
    }

    @Test
    void listAndCostAmountShouldUseOnlyVehicleArrangementSnapshotsInSortOrder() {
        SalesProductDesignerVehicleArrangementMapper vehicleMapper = mock(
                SalesProductDesignerVehicleArrangementMapper.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                mock(SalesProductMapper.class), mock(PurchaseResourceMapper.class), vehicleMapper,
                mock(ProductDesignerSupplierQuoteService.class));
        SalesProductDesignerVehicleArrangementEntity later = vehicleArrangement(502L, 88L, 2);
        later.setCostAmountSnapshot(new BigDecimal("300.00"));
        SalesProductDesignerVehicleArrangementEntity first = vehicleArrangement(501L, 88L, 1);
        first.setCostAmountSnapshot(new BigDecimal("200.00"));
        when(vehicleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(later, first));

        assertThat(service.list(1L, 88L)).extracting(item -> item.id()).containsExactly(501L, 502L);
        assertThat(service.costAmount(1L, 88L)).isEqualByComparingTo("500.00");
    }

    @Test
    void vehicleResourcesShouldReturnOnlyVehiclePickerFields() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDesignerVehicleArrangementService service = new SalesProductDesignerVehicleArrangementService(
                mock(SalesProductMapper.class), resourceMapper, mock(SalesProductDesignerVehicleArrangementMapper.class),
                mock(ProductDesignerSupplierQuoteService.class));
        Page<PurchaseResourceEntity> page = Page.of(1, 50);
        PurchaseResourceEntity resource = vehicleResource(21L, "39座旅游大巴");
        resource.setCity("南京市");
        resource.setSeatCount(39);
        resource.setBillingMode("trip");
        page.setRecords(List.of(resource));
        page.setTotal(1L);
        when(resourceMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        var response = service.vehicleResources(1L, null, 1, 50);

        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(21L);
            assertThat(item.vehicleType()).isEqualTo("39座旅游大巴");
            assertThat(item.seatCount()).isEqualTo(39);
            assertThat(item.billingMode()).isEqualTo("trip");
        });
        ArgumentCaptor<Wrapper<PurchaseResourceEntity>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resourceMapper).selectPage(any(Page.class), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getSqlSegment()).contains("seat_count ASC").doesNotContain("city ASC");
    }

    private SalesProductEntity draftProduct(int travelDays) {
        SalesProductEntity product = new SalesProductEntity();
        product.setId(88L);
        product.setProductScope("design_draft");
        product.setTravelDays(travelDays);
        return product;
    }

    private PurchaseResourceEntity vehicleResource(Long id, String vehicleType) {
        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setId(id);
        resource.setResourceType("vehicle");
        resource.setProcurementMode("required");
        resource.setResourceName(vehicleType);
        resource.setVehicleType(vehicleType);
        resource.setStatus("active");
        return resource;
    }

    private SalesProductDesignerVehicleArrangementEntity vehicleArrangement(Long id, Long productId, int sortOrder) {
        SalesProductDesignerVehicleArrangementEntity entity = new SalesProductDesignerVehicleArrangementEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setProductId(productId);
        entity.setResourceNameSnapshot("39座旅游大巴");
        entity.setVehicleTypeSnapshot("39座旅游大巴");
        entity.setPriceModeSnapshot("unified");
        entity.setQuantitySnapshot(BigDecimal.ONE);
        entity.setUnitPriceSnapshot(new BigDecimal("200.00"));
        entity.setCostAmountSnapshot(new BigDecimal("200.00"));
        entity.setSortOrder(sortOrder);
        return entity;
    }
}
