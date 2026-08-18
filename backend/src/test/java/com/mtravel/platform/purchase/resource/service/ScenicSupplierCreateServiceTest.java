package com.mtravel.platform.purchase.resource.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
import com.mtravel.platform.purchase.resource.dto.ResourceSupplierCreateRequest;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScenicSupplierCreateServiceTest {

    @Test
    void unifiedPriceShouldBeStoredOnRelationWithoutCreatingProjectPriceLines() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        SupplierResourcePriceMapper priceMapper = mock(SupplierResourcePriceMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        ScenicSupplierCreateService service = new ScenicSupplierCreateService(
                resourceMapper, supplierMapper, relationMapper, priceMapper, expenseItemMapper
        );
        ArgumentCaptor<PurchaseRelationEntity> relationCaptor = ArgumentCaptor.forClass(PurchaseRelationEntity.class);

        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setId(67L);
        resource.setTenantId(1L);
        resource.setResourceType("ground_agent");
        resource.setResourceName("华东三日游测试地接");
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource);
        when(supplierMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierMapper.insert(any(SupplierEntity.class))).thenAnswer(invocation -> {
            SupplierEntity entity = invocation.getArgument(0);
            entity.setId(70L);
            return 1;
        });
        when(relationMapper.insert(any(PurchaseRelationEntity.class))).thenAnswer(invocation -> {
            PurchaseRelationEntity entity = invocation.getArgument(0);
            entity.setId(58L);
            return 1;
        });

        service.createResourceSupplier(1L, 67L, unifiedRequest(), "admin");

        verify(relationMapper).insert(relationCaptor.capture());
        PurchaseRelationEntity relation = relationCaptor.getValue();
        assertThat(relation.getPriceMode()).isEqualTo("unified");
        assertThat(relation.getUnifiedPrice()).isEqualByComparingTo("200.00");
        assertThat(relation.getPriceRemark()).isEqualTo("30人以内参考价");
        verify(priceMapper, never()).insertBatch(any());
        verify(expenseItemMapper, never()).selectList(any(Wrapper.class));
        verify(expenseItemMapper, never()).selectOne(any(Wrapper.class));
    }

    private ResourceSupplierCreateRequest unifiedRequest() {
        return new ResourceSupplierCreateRequest(
                "测试供应商",
                "浙江省",
                "杭州市",
                "上城区",
                "华东团队地接服务",
                "张三",
                "15100001111",
                "active",
                true,
                "unified",
                new BigDecimal("200.00"),
                null,
                "30人以内参考价",
                null
        );
    }
}
