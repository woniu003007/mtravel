package com.mtravel.platform.purchase.resource.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.dto.PurchaseResourceSaveRequest;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseResourceServiceTest {

    @Test
    void pageShouldBatchLoadBoundSupplierCountsInsteadOfCountingEachResource() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);
        Page<PurchaseResourceEntity> pageResult = Page.of(1, 200);
        pageResult.setTotal(2);
        pageResult.setRecords(List.of(
                resource(10L, "scenic", "西湖景区"),
                resource(11L, "scenic", "灵隐景区")
        ));
        PurchaseRelationEntity relationOne = new PurchaseRelationEntity();
        relationOne.setResourceId(10L);
        PurchaseRelationEntity relationTwo = new PurchaseRelationEntity();
        relationTwo.setResourceId(10L);
        PurchaseRelationEntity relationThree = new PurchaseRelationEntity();
        relationThree.setResourceId(11L);

        when(resourceMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(relationOne, relationTwo, relationThree));

        var result = service.page(1L, null, "scenic", null, null, null, "active", 1, 200);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).boundSupplierCount()).isEqualTo(2L);
        assertThat(result.items().get(1).boundSupplierCount()).isEqualTo(1L);
        verify(resourceMapper).selectPage(any(Page.class), any(Wrapper.class));
        verify(relationMapper).selectList(any(Wrapper.class));
        verify(relationMapper, never()).selectCount(any(Wrapper.class));
    }

    @Test
    void createShouldAutoCreateSameNameSupplierAndBindPurchaseRelation() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);
        ArgumentCaptor<PurchaseResourceEntity> resourceCaptor = ArgumentCaptor.forClass(PurchaseResourceEntity.class);
        ArgumentCaptor<SupplierEntity> supplierCaptor = ArgumentCaptor.forClass(SupplierEntity.class);
        ArgumentCaptor<PurchaseRelationEntity> relationCaptor = ArgumentCaptor.forClass(PurchaseRelationEntity.class);

        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(relationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer((Answer<Integer>) invocation -> {
            PurchaseResourceEntity entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        }).when(resourceMapper).insert(any(PurchaseResourceEntity.class));
        doAnswer((Answer<Integer>) invocation -> {
            SupplierEntity entity = invocation.getArgument(0);
            entity.setId(66L);
            return 1;
        }).when(supplierMapper).insert(any(SupplierEntity.class));

        service.create(request(true), 1L, "admin");

        verify(resourceMapper).insert(resourceCaptor.capture());
        verify(supplierMapper).insert(supplierCaptor.capture());
        verify(relationMapper).insert(relationCaptor.capture());

        PurchaseResourceEntity resource = resourceCaptor.getValue();
        assertThat(resource.getResourceType()).isEqualTo("scenic");
        assertThat(resource.getResourceName()).isEqualTo("苏州园林");
        assertThat(resource.getCity()).isEqualTo("苏州市");
        assertThat(resource.getIsDeleted()).isFalse();

        SupplierEntity supplier = supplierCaptor.getValue();
        assertThat(supplier.getSupplierName()).isEqualTo("苏州园林");
        assertThat(supplier.getSupplierCategory()).isEqualTo("scenic");
        assertThat(supplier.getStatus()).isEqualTo("active");

        PurchaseRelationEntity relation = relationCaptor.getValue();
        assertThat(relation.getResourceType()).isEqualTo("scenic");
        assertThat(relation.getResourceId()).isEqualTo(88L);
        assertThat(relation.getResourceName()).isEqualTo("苏州园林");
        assertThat(relation.getSupplierId()).isEqualTo(66L);
        assertThat(relation.getGroupQuantity()).isZero();
        assertThat(relation.getPurchasePrice()).isNull();
        assertThat(relation.getStatus()).isEqualTo("active");
    }

    @Test
    void createShouldReuseExistingSameNameSupplierWhenAutoCreateEnabled() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);
        ArgumentCaptor<PurchaseRelationEntity> relationCaptor = ArgumentCaptor.forClass(PurchaseRelationEntity.class);
        SupplierEntity existing = new SupplierEntity();
        existing.setId(12L);
        existing.setSupplierName("苏州园林");
        existing.setSupplierCategory("scenic");

        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(supplierMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(relationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer((Answer<Integer>) invocation -> {
            PurchaseResourceEntity entity = invocation.getArgument(0);
            entity.setId(90L);
            return 1;
        }).when(resourceMapper).insert(any(PurchaseResourceEntity.class));

        service.create(request(true), 1L, "admin");

        verify(supplierMapper, never()).insert(any(SupplierEntity.class));
        verify(relationMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getSupplierId()).isEqualTo(12L);
    }

    @Test
    void deleteShouldSoftDeleteResource() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);
        ArgumentCaptor<PurchaseResourceEntity> captor = ArgumentCaptor.forClass(PurchaseResourceEntity.class);

        when(resourceMapper.update(any(PurchaseResourceEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(9L, 1L, "admin");

        verify(resourceMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    private PurchaseResourceSaveRequest request(boolean autoCreateSupplier) {
        return new PurchaseResourceSaveRequest(
                "scenic",
                "苏州园林",
                "江苏省",
                "苏州市",
                "姑苏区",
                "0512-00000000",
                "0512-00000001",
                "苏州市姑苏区测试路",
                "团队入园需提前预约",
                "江南园林资源",
                "active",
                autoCreateSupplier,
                "备注"
        );
    }

    private PurchaseResourceEntity resource(Long id, String resourceType, String resourceName) {
        PurchaseResourceEntity entity = new PurchaseResourceEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setResourceType(resourceType);
        entity.setResourceName(resourceName);
        entity.setStatus("active");
        return entity;
    }
}
