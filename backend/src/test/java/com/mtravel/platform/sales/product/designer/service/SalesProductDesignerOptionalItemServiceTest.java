package com.mtravel.platform.sales.product.designer.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.optional.entity.PurchaseRelationOptionalItemEntity;
import com.mtravel.platform.purchase.relation.optional.mapper.PurchaseRelationOptionalItemMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;
import com.mtravel.platform.purchase.resource.optional.mapper.PurchaseResourceOptionalItemMapper;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerOptionalItemsSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemRequest;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceOptionalItemEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceOptionalItemMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 自费项目默认对外价规则测试。 */
class SalesProductDesignerOptionalItemServiceTest {

    @Test
    void shouldUseCurrentSupplierSuggestedPriceWhenSalePriceIsOmitted() {
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDayResourceOptionalItemMapper snapshotMapper = mock(SalesProductDayResourceOptionalItemMapper.class);
        PurchaseResourceOptionalItemMapper masterMapper = mock(PurchaseResourceOptionalItemMapper.class);
        PurchaseRelationOptionalItemMapper supplierMapper = mock(PurchaseRelationOptionalItemMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        SalesProductDesignerOptionalItemService service = new SalesProductDesignerOptionalItemService(
                dayResourceMapper, snapshotMapper, masterMapper, supplierMapper, relationMapper,
                mock(PurchaseResourceIntroductionMapper.class));
        SalesProductDayResourceEntity dayResource = new SalesProductDayResourceEntity();
        dayResource.setId(11L);
        dayResource.setProductId(8L);
        dayResource.setResourceId(21L);
        dayResource.setSupplierId(31L);
        when(dayResourceMapper.selectOne(any(Wrapper.class))).thenReturn(dayResource);
        PurchaseResourceOptionalItemEntity master = new PurchaseResourceOptionalItemEntity();
        master.setId(51L);
        master.setItemType("traffic");
        master.setProjectName("景交车");
        when(masterMapper.selectOne(any(Wrapper.class))).thenReturn(master);
        PurchaseRelationOptionalItemEntity quote = new PurchaseRelationOptionalItemEntity();
        quote.setId(61L);
        quote.setRelationId(71L);
        quote.setSuggestedSalePrice(new BigDecimal("40.00"));
        quote.setCostPrice(new BigDecimal("20.00"));
        when(supplierMapper.selectList(any(Wrapper.class))).thenReturn(List.of(quote));
        when(relationMapper.selectOne(any(Wrapper.class))).thenReturn(new PurchaseRelationEntity());
        when(snapshotMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.save(1L, new ProductDesignerOptionalItemsSaveRequest(8L, 11L, List.of(
                new ProductDesignerSelectedOptionalItemRequest(51L, null, null, null, null, null))), "admin");

        ArgumentCaptor<SalesProductDayResourceOptionalItemEntity> captor =
                ArgumentCaptor.forClass(SalesProductDayResourceOptionalItemEntity.class);
        verify(snapshotMapper).insert(captor.capture());
        assertThat(captor.getValue().getFinalSalePrice()).isEqualByComparingTo("40.00");
        assertThat(captor.getValue().getSupplierOptionalItemId()).isEqualTo(61L);
    }

    @Test
    void shouldRejectOmittedSalePriceWhenCurrentSupplierHasNoSuggestedPrice() {
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        PurchaseResourceOptionalItemMapper masterMapper = mock(PurchaseResourceOptionalItemMapper.class);
        PurchaseRelationOptionalItemMapper supplierMapper = mock(PurchaseRelationOptionalItemMapper.class);
        SalesProductDesignerOptionalItemService service = new SalesProductDesignerOptionalItemService(
                dayResourceMapper, mock(SalesProductDayResourceOptionalItemMapper.class), masterMapper,
                supplierMapper, mock(PurchaseRelationMapper.class), mock(PurchaseResourceIntroductionMapper.class));
        SalesProductDayResourceEntity dayResource = new SalesProductDayResourceEntity();
        dayResource.setId(11L);
        dayResource.setProductId(8L);
        dayResource.setResourceId(21L);
        dayResource.setSupplierId(31L);
        when(dayResourceMapper.selectOne(any(Wrapper.class))).thenReturn(dayResource);
        PurchaseResourceOptionalItemEntity master = new PurchaseResourceOptionalItemEntity();
        master.setId(51L);
        when(masterMapper.selectOne(any(Wrapper.class))).thenReturn(master);
        when(supplierMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.save(1L, new ProductDesignerOptionalItemsSaveRequest(8L, 11L, List.of(
                new ProductDesignerSelectedOptionalItemRequest(51L, null, null, null, null, null))), "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("当前供应商未维护该自费项目建议价，请填写对外价");
    }
}
