package com.mtravel.platform.purchase.relation.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.common.SupplierLookupService;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSaveRequest;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import java.util.List;
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
    void pageShouldBatchLoadResourceAndSupplierDisplayData() {
        PurchaseRelationMapper mapper = mock(PurchaseRelationMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        SupplierLookupService supplierLookup = new SupplierLookupService(supplierMapper);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseRelationService service = new PurchaseRelationService(mapper, supplierLookup, resourceMapper, supplierMapper);
        Page<PurchaseRelationEntity> pageResult = Page.of(1, 200);
        pageResult.setTotal(2);
        pageResult.setRecords(List.of(
                relation(100L, 10L, 66L, "西湖景区"),
                relation(101L, 11L, 67L, "灵隐景区")
        ));
        SupplierEntity supplierA = supplier(66L, "杭州票务A");
        SupplierEntity supplierB = supplier(67L, "杭州票务B");

        when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(pageResult);
        when(resourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                resource(10L, "scenic", "西湖景区"),
                resource(11L, "scenic", "灵隐景区")
        ));
        when(supplierMapper.selectList(any(Wrapper.class))).thenReturn(List.of(supplierA, supplierB));

        var result = service.page(1L, null, "scenic", "active", null, 1, 200);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).location()).isEqualTo("浙江省 / 杭州市 / 西湖区");
        assertThat(result.items().get(0).supplierName()).isEqualTo("杭州票务A");
        assertThat(result.items().get(1).supplierName()).isEqualTo("杭州票务B");
        verify(resourceMapper).selectList(any(Wrapper.class));
        verify(supplierMapper).selectList(any(Wrapper.class));
        verify(resourceMapper, never()).selectOne(any(Wrapper.class));
        verify(supplierMapper, never()).selectOne(any(Wrapper.class));
    }

    @Test
    void createShouldBindSupplierToResourceAndPersistGroupQuantityOnly() {
        PurchaseRelationMapper mapper = mock(PurchaseRelationMapper.class);
        SupplierLookupService supplierLookup = mock(SupplierLookupService.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationService service = new PurchaseRelationService(mapper, supplierLookup, resourceMapper, supplierMapper);
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

        service.create(new PurchaseRelationSaveRequest(88L, 66L, 0, false, "active", "散团同价"), 1L, "admin");

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
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationService service = new PurchaseRelationService(mapper, supplierLookup, resourceMapper, supplierMapper);

        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(resource(88L, "hotel", "苏州中心酒店"));
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(new PurchaseRelationSaveRequest(88L, 66L, 10, false, "active", null), 1L, "admin"))
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
        entity.setProvince("浙江省");
        entity.setCity("杭州市");
        entity.setDistrict("西湖区");
        return entity;
    }

    private PurchaseRelationEntity relation(Long id, Long resourceId, Long supplierId, String resourceName) {
        PurchaseRelationEntity entity = new PurchaseRelationEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setResourceType("scenic");
        entity.setResourceId(resourceId);
        entity.setResourceName(resourceName);
        entity.setSupplierId(supplierId);
        entity.setGroupQuantity(0);
        entity.setStatus("active");
        return entity;
    }

    private SupplierEntity supplier(Long id, String supplierName) {
        SupplierEntity entity = new SupplierEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setSupplierName(supplierName);
        entity.setContactName("张经理");
        entity.setContactPhone("13800000000");
        return entity;
    }
}
