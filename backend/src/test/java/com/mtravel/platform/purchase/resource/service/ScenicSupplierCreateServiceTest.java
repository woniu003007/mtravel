package com.mtravel.platform.purchase.resource.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.optional.entity.PurchaseRelationOptionalItemEntity;
import com.mtravel.platform.purchase.relation.optional.mapper.PurchaseRelationOptionalItemMapper;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
import com.mtravel.platform.purchase.resource.dto.ResourceSupplierCreateRequest;
import com.mtravel.platform.purchase.resource.dto.ResourceSupplierOptionalItemRequest;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;
import com.mtravel.platform.purchase.resource.optional.mapper.PurchaseResourceOptionalItemMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScenicSupplierCreateServiceTest {

    @Test
    void vehicleSupplierShouldBindWithoutFixedResourcePrice() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        SupplierResourcePriceMapper priceMapper = mock(SupplierResourcePriceMapper.class);
        ScenicSupplierCreateService service = new ScenicSupplierCreateService(
                resourceMapper, supplierMapper, relationMapper, priceMapper,
                mock(EnterpriseExpenseItemMapper.class)
        );
        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setId(179L);
        resource.setTenantId(1L);
        resource.setResourceType("vehicle");
        resource.setResourceName("7座商务车");
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource);
        when(supplierMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierMapper.insert(any(SupplierEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SupplierEntity.class).setId(66L);
            return 1;
        });
        when(relationMapper.insert(any(PurchaseRelationEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseRelationEntity.class).setId(164L);
            return 1;
        });
        ArgumentCaptor<PurchaseRelationEntity> relationCaptor = ArgumentCaptor.forClass(PurchaseRelationEntity.class);

        service.createResourceSupplier(1L, 179L, new ResourceSupplierCreateRequest(
                "测试商务车队", null, null, null, null, null, null, "active", true,
                "classified", null, null, null, null, null
        ), "admin");

        verify(relationMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getPriceMode()).isEqualTo("classified");
        assertThat(relationCaptor.getValue().getUnifiedPrice()).isNull();
        verify(priceMapper, never()).insertBatch(any());
    }

    @Test
    void scenicSupplierShouldPersistMultipleOptionalItemsAtFixedUnit() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        SupplierResourcePriceMapper priceMapper = mock(SupplierResourcePriceMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        PurchaseRelationOptionalItemMapper optionalMapper = mock(PurchaseRelationOptionalItemMapper.class);
        PurchaseResourceOptionalItemMapper resourceOptionalItemMapper = mock(PurchaseResourceOptionalItemMapper.class);
        ScenicSupplierCreateService service = new ScenicSupplierCreateService(
                resourceMapper, supplierMapper, relationMapper, priceMapper, expenseItemMapper, optionalMapper,
                resourceOptionalItemMapper
        );
        ArgumentCaptor<List<PurchaseRelationOptionalItemEntity>> optionalCaptor = ArgumentCaptor.forClass(List.class);

        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setId(67L);
        resource.setTenantId(1L);
        resource.setResourceType("scenic");
        resource.setResourceName("灵山测试景区");
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
        PurchaseResourceOptionalItemEntity existingMaster = new PurchaseResourceOptionalItemEntity();
        existingMaster.setId(101L);
        existingMaster.setProjectName("九龙灌浴");
        when(resourceOptionalItemMapper.selectOne(any(Wrapper.class))).thenReturn(existingMaster, null);
        when(resourceOptionalItemMapper.insert(any(PurchaseResourceOptionalItemEntity.class))).thenAnswer(invocation -> {
            PurchaseResourceOptionalItemEntity entity = invocation.getArgument(0);
            entity.setId(102L);
            return 1;
        });

        service.createResourceSupplier(1L, 67L, new ResourceSupplierCreateRequest(
                "景区供应商", "江苏省", "无锡市", "滨湖区", null, "张三", "15100001111", "active",
                false, "unified", new BigDecimal("100.00"), null,
                List.of(
                        new ResourceSupplierOptionalItemRequest("九龙灌浴", new BigDecimal("30.00"), "景区赠送项目", "active"),
                        new ResourceSupplierOptionalItemRequest("灵山电瓶车", new BigDecimal("20.00"), null, null)
                ),
                null, null
        ), "admin");

        verify(optionalMapper).insertBatch(optionalCaptor.capture());
        assertThat(optionalCaptor.getValue()).hasSize(2);
        assertThat(optionalCaptor.getValue().get(0).getProjectName()).isEqualTo("九龙灌浴");
        assertThat(optionalCaptor.getValue().get(0).getResourceOptionalItemId()).isEqualTo(101L);
        assertThat(optionalCaptor.getValue().get(0).getCostPrice()).isEqualByComparingTo("30.00");
        assertThat(optionalCaptor.getValue().get(0).getPriceUnit()).isEqualTo("yuan_per_person");
        assertThat(optionalCaptor.getValue().get(1).getResourceOptionalItemId()).isEqualTo(102L);
        assertThat(optionalCaptor.getValue().get(1).getStatus()).isEqualTo("active");
        verify(resourceOptionalItemMapper).insert(any(PurchaseResourceOptionalItemEntity.class));
    }

    @Test
    void scenicSupplierWithOnlyOptionalItemsShouldNotRequireTicketPrice() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        SupplierResourcePriceMapper priceMapper = mock(SupplierResourcePriceMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        PurchaseRelationOptionalItemMapper optionalMapper = mock(PurchaseRelationOptionalItemMapper.class);
        PurchaseResourceOptionalItemMapper resourceOptionalItemMapper = mock(PurchaseResourceOptionalItemMapper.class);
        ScenicSupplierCreateService service = new ScenicSupplierCreateService(
                resourceMapper, supplierMapper, relationMapper, priceMapper, expenseItemMapper, optionalMapper,
                resourceOptionalItemMapper
        );
        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setId(68L);
        resource.setTenantId(1L);
        resource.setResourceType("scenic");
        resource.setResourceName("仅自费测试景区");
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource);
        when(supplierMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierMapper.insert(any(SupplierEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SupplierEntity.class).setId(71L);
            return 1;
        });
        when(relationMapper.insert(any(PurchaseRelationEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseRelationEntity.class).setId(59L);
            return 1;
        });
        when(resourceOptionalItemMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(resourceOptionalItemMapper.insert(any(PurchaseResourceOptionalItemEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseResourceOptionalItemEntity.class).setId(103L);
            return 1;
        });

        service.createResourceSupplier(1L, 68L, new ResourceSupplierCreateRequest(
                "仅自费供应商", null, null, null, null, null, null, "active", false,
                "unified", null, null,
                List.of(new ResourceSupplierOptionalItemRequest("景区电瓶车", new BigDecimal("40.00"), null, "active")),
                null, null
        ), "admin");

        ArgumentCaptor<PurchaseRelationEntity> relationCaptor = ArgumentCaptor.forClass(PurchaseRelationEntity.class);
        verify(relationMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getPriceMode()).isEqualTo("unified");
        assertThat(relationCaptor.getValue().getUnifiedPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(optionalMapper).insertBatch(any());
        verify(priceMapper, never()).insertBatch(any());
    }

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
                null,
                "30人以内参考价",
                null
        );
    }
}
