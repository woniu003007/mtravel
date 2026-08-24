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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.isNull;
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

        var result = service.page(
                1L, null, "scenic", null, null, null, "active", "required", "4a", "open", "visited", 1, 200
        );

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).boundSupplierCount()).isEqualTo(2L);
        assertThat(result.items().get(1).boundSupplierCount()).isEqualTo(1L);
        verify(resourceMapper).selectPage(any(Page.class), any(Wrapper.class));
        verify(relationMapper).selectList(any(Wrapper.class));
        verify(relationMapper, never()).selectCount(any(Wrapper.class));
    }

    @Test
    void createShouldCreateDefaultSupplierForAnyResourceTypeWhenRequested() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);
        ArgumentCaptor<PurchaseResourceEntity> resourceCaptor = ArgumentCaptor.forClass(PurchaseResourceEntity.class);
        ArgumentCaptor<SupplierEntity> supplierCaptor = ArgumentCaptor.forClass(SupplierEntity.class);
        ArgumentCaptor<PurchaseRelationEntity> relationCaptor = ArgumentCaptor.forClass(PurchaseRelationEntity.class);

        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ((PurchaseResourceEntity) invocation.getArgument(0)).setId(101L);
            return 1;
        }).when(resourceMapper).insert(any(PurchaseResourceEntity.class));
        doAnswer(invocation -> {
            ((SupplierEntity) invocation.getArgument(0)).setId(202L);
            return 1;
        }).when(supplierMapper).insert(any(SupplierEntity.class));

        service.create(request(true), 1L, "admin");

        verify(resourceMapper).insert(resourceCaptor.capture());
        verify(supplierMapper).insert(supplierCaptor.capture());
        verify(relationMapper).insert(relationCaptor.capture());

        SupplierEntity supplier = supplierCaptor.getValue();
        assertThat(supplier.getSupplierName()).isEqualTo("苏州园林");
        assertThat(supplier.getSupplierCategory()).isEqualTo("scenic");
        assertThat(supplier.getProvince()).isEqualTo("江苏省");
        assertThat(supplier.getCity()).isEqualTo("苏州市");
        assertThat(supplier.getDistrict()).isEqualTo("姑苏区");
        assertThat(supplier.getContactName()).isEqualTo("资源联系人");
        assertThat(supplier.getContactPhone()).isEqualTo("0512-00000000");
        assertThat(supplier.getOfficeAddress()).isEqualTo("苏州市姑苏区测试路");

        PurchaseRelationEntity relation = relationCaptor.getValue();
        assertThat(relation.getResourceId()).isEqualTo(101L);
        assertThat(relation.getSupplierId()).isEqualTo(202L);
        assertThat(relation.getResourceType()).isEqualTo("scenic");
        assertThat(relation.getResourceName()).isEqualTo("苏州园林");
        assertThat(relation.getGroupQuantity()).isZero();
        assertThat(relation.getIsDefault()).isTrue();
        assertThat(relation.getStatus()).isEqualTo("active");

        PurchaseResourceEntity resource = resourceCaptor.getValue();
        assertThat(resource.getResourceType()).isEqualTo("scenic");
        assertThat(resource.getResourceName()).isEqualTo("苏州园林");
        assertThat(resource.getCity()).isEqualTo("苏州市");
        assertThat(resource.getScenicLevel()).isEqualTo("4a");
        assertThat(resource.getLongitude()).isEqualByComparingTo("120.6212345");
        assertThat(resource.getLatitude()).isEqualByComparingTo("31.3212345");
        assertThat(resource.getBusinessStatus()).isEqualTo("open");
        assertThat(resource.getOpeningTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(resource.getClosingTime()).isEqualTo(LocalTime.of(17, 30));
        assertThat(resource.getSiteVisitStatus()).isEqualTo("visited");
        assertThat(resource.getLastSiteVisitDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(resource.getSiteVisitNote()).isEqualTo("入口和停车场已核实");
        assertThat(resource.getIsDeleted()).isFalse();
    }

    @Test
    void createShouldNotCreateSupplierWhenAutoCreateIsDisabled() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);

        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.create(request(false), 1L, "admin");

        verify(resourceMapper).insert(any(PurchaseResourceEntity.class));
        verify(supplierMapper, never()).insert(any(SupplierEntity.class));
        verify(relationMapper, never()).insert(any(PurchaseRelationEntity.class));
    }

    @Test
    void deleteShouldSoftDeleteResource() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);

        when(resourceMapper.update(any(PurchaseResourceEntity.class), any(Wrapper.class))).thenReturn(1);
        when(resourceMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        service.delete(9L, 1L, "admin");

        verify(resourceMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void createScenicShouldUseUnmaintainedDefaultsWhenSpecialFieldsAreAbsent() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);
        ArgumentCaptor<PurchaseResourceEntity> captor = ArgumentCaptor.forClass(PurchaseResourceEntity.class);

        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.create(requestWithoutScenicDetails(), 1L, "admin");

        verify(resourceMapper).insert(captor.capture());
        assertThat(captor.getValue().getScenicLevel()).isEqualTo("unrated");
        assertThat(captor.getValue().getBusinessStatus()).isEqualTo("unmaintained");
        assertThat(captor.getValue().getSiteVisitStatus()).isEqualTo("unmaintained");
        assertThat(captor.getValue().getLastSiteVisitDate()).isNull();
        assertThat(captor.getValue().getProcurementMode()).isEqualTo("required");
    }

    @Test
    void updateToVehicleShouldClearAllPlaceFields() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);
        ArgumentCaptor<PurchaseResourceEntity> captor = ArgumentCaptor.forClass(PurchaseResourceEntity.class);
        PurchaseResourceEntity updated = resource(88L, "vehicle", "33座旅游大巴");

        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(resourceMapper.update(any(PurchaseResourceEntity.class), any(Wrapper.class))).thenReturn(1);
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(updated);

        service.update(88L, nonScenicRequest(), 1L);

        verify(resourceMapper).update(captor.capture(), any(Wrapper.class));
        PurchaseResourceEntity changed = captor.getValue();
        assertThat(changed.getProvince()).isNull();
        assertThat(changed.getCity()).isNull();
        assertThat(changed.getDistrict()).isNull();
        assertThat(changed.getAddress()).isNull();
        assertThat(changed.getScenicLevel()).isNull();
        assertThat(changed.getLongitude()).isNull();
        assertThat(changed.getLatitude()).isNull();
        assertThat(changed.getBusinessStatus()).isNull();
        assertThat(changed.getOpeningTime()).isNull();
        assertThat(changed.getClosingTime()).isNull();
        assertThat(changed.getSiteVisitStatus()).isNull();
        assertThat(changed.getLastSiteVisitDate()).isNull();
        assertThat(changed.getSiteVisitNote()).isNull();
    }

    @Test
    void createScenicShouldRejectIncompleteCoordinatesAndFutureVisitDate() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseResourceService service = new PurchaseResourceService(resourceMapper, supplierMapper, relationMapper);

        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> service.create(requestWithInvalidCoordinates(), 1L, "admin"))
                .hasMessage("经度和纬度必须同时填写");
        assertThatThrownBy(() -> service.create(requestWithFutureVisitDate(), 1L, "admin"))
                .hasMessage("最近踩点日期不能晚于当天");
    }

    @Test
    void pageShouldRejectUnknownScenicFilterValues() {
        PurchaseResourceService service = new PurchaseResourceService(
                mock(PurchaseResourceMapper.class),
                mock(SupplierMapper.class),
                mock(PurchaseRelationMapper.class)
        );

        assertThatThrownBy(() -> service.page(
                1L, null, "scenic", null, null, null, null, null, "6a", null, null, 1, 20
        )).hasMessage("景区等级筛选值不合法");
    }

    @Test
    void pageShouldRejectUnknownProcurementMode() {
        PurchaseResourceService service = new PurchaseResourceService(
                mock(PurchaseResourceMapper.class),
                mock(SupplierMapper.class),
                mock(PurchaseRelationMapper.class)
        );

        assertThatThrownBy(() -> service.page(
                1L, null, "scenic", null, null, null, null, "free", null, null, null, 1, 20
        )).hasMessage("默认采购属性筛选值不合法");
    }

    private PurchaseResourceSaveRequest request(boolean autoCreateSupplier) {
        return new PurchaseResourceSaveRequest(
                "scenic",
                "苏州园林",
                "江苏省",
                "苏州市",
                "姑苏区",
                "0512-00000000",
                "资源联系人",
                "0512-00000001",
                "苏州市姑苏区测试路",
                "4a",
                null,
                null,
                new BigDecimal("120.6212345"),
                new BigDecimal("31.3212345"),
                "open",
                LocalTime.of(8, 30),
                LocalTime.of(17, 30),
                "visited",
                LocalDate.of(2026, 8, 1),
                "入口和停车场已核实",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "团队入园需提前预约",
                "江南园林资源",
                "active",
                autoCreateSupplier,
                "备注",
                "required"
        );
    }

    private PurchaseResourceSaveRequest requestWithoutScenicDetails() {
        return requestBase("scenic", "历史景区");
    }

    private PurchaseResourceSaveRequest nonScenicRequest() {
        return new PurchaseResourceSaveRequest(
                "vehicle", "33座旅游大巴", "江苏省", "苏州市", "姑苏区", null, null, null, "测试地址",
                "5a", null, null, new BigDecimal("120.1"), new BigDecimal("31.1"), "open",
                LocalTime.of(8, 0), LocalTime.of(18, 0), "visited", LocalDate.of(2026, 8, 1), "旧踩点",
                null, null, null, "旅游大巴", 33, "daily", null, null, null, null, null,
                null, null, "active", false, null, null
        );
    }

    private PurchaseResourceSaveRequest requestWithInvalidCoordinates() {
        return new PurchaseResourceSaveRequest(
                "scenic", "单坐标景区", null, null, null, null, null, null, null,
                "unrated", null, null, new BigDecimal("120.1"), null, "unmaintained",
                null, null, "unmaintained", null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, "active", false, null, null
        );
    }

    private PurchaseResourceSaveRequest requestWithFutureVisitDate() {
        return new PurchaseResourceSaveRequest(
                "scenic", "未来踩点景区", null, null, null, null, null, null, null,
                "unrated", null, null, null, null, "unmaintained",
                null, null, "visited", LocalDate.now().plusDays(1), null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, "active", false, null, null
        );
    }

    private PurchaseResourceSaveRequest requestBase(String resourceType, String resourceName) {
        return new PurchaseResourceSaveRequest(
                resourceType, resourceName, "浙江省", "杭州市", null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, "active", false, null, null
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
