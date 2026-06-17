package com.mtravel.platform.purchase.relation.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.common.SupplierLookupService;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSaveRequest;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseRelationServiceTest {

    @Test
    void createShouldBindSupplierToResourceAndPersistGroupQuantityOnly() {
        PurchaseRelationMapper mapper = mock(PurchaseRelationMapper.class);
        SupplierLookupService supplierLookup = mock(SupplierLookupService.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseRelationService service = new PurchaseRelationService(mapper, supplierLookup, resourceMapper);
        ArgumentCaptor<PurchaseRelationEntity> captor = ArgumentCaptor.forClass(PurchaseRelationEntity.class);
        PurchaseRelationEntity[] inserted = new PurchaseRelationEntity[1];

        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource(88L, "scenic", "苏州园林"));
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(PurchaseRelationEntity.class))).thenAnswer(invocation -> {
            PurchaseRelationEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            inserted[0] = entity;
            return 1;
        });
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> inserted[0]);

        service.create(new PurchaseRelationSaveRequest(88L, 66L, 0, "active", "散团同价"), 1L, "admin");

        verify(supplierLookup).assertSupplierIfPresent(1L, 66L);
        verify(mapper).insert(captor.capture());
        PurchaseRelationEntity entity = captor.getValue();
        assertThat(entity.getResourceId()).isEqualTo(88L);
        assertThat(entity.getResourceType()).isEqualTo("scenic");
        assertThat(entity.getResourceName()).isEqualTo("苏州园林");
        assertThat(entity.getSupplierId()).isEqualTo(66L);
        assertThat(entity.getGroupQuantity()).isZero();
        assertThat(entity.getPurchasePrice()).isNull();
        assertThat(entity.getStatus()).isEqualTo("active");
        assertThat(entity.getRemark()).isEqualTo("散团同价");
    }

    @Test
    void createShouldRejectDuplicateSupplierResourceBinding() {
        PurchaseRelationMapper mapper = mock(PurchaseRelationMapper.class);
        SupplierLookupService supplierLookup = mock(SupplierLookupService.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseRelationService service = new PurchaseRelationService(mapper, supplierLookup, resourceMapper);

        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource(88L, "hotel", "苏州中心酒店"));
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(new PurchaseRelationSaveRequest(88L, 66L, 10, "active", null), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("采购关系已存在");

        verify(mapper, never()).insert(any(PurchaseRelationEntity.class));
    }

    private PurchaseResourceEntity resource(Long id, String resourceType, String resourceName) {
        PurchaseResourceEntity entity = new PurchaseResourceEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setResourceType(resourceType);
        entity.setResourceName(resourceName);
        return entity;
    }
}
