package com.mtravel.platform.sales.product.designer.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSupplierPriceRow;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceImageEntity;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceImageMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayResourceSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayItinerarySaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayEndImageSelectionRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDayWordPlanSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedMaterialRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerSelectedOptionalItemResponse;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerAdultQuoteSaveRequest;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDraftSaveRequest;
import com.mtravel.platform.sales.product.designer.entity.SalesProductAdultQuoteEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceImageEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceIntroductionEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDocumentVersionEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductAdultQuoteMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceImageMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceIntroductionMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDocumentVersionMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产品设计工作台服务测试。
 *
 * <p>重点固定“免费资源不需要供应商、成本为0”和“同一天不能重复加入同一资源”的核心规则，
 * 防止后续把产品地图工作台又退回到只适合采购结算资源的录入方式。</p>
 */
class SalesProductDesignerServiceTest {

    @Test
    void dayEndImagesShouldPersistCrossResourceGlobalOrderAndRejectOneImage() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceImageMapper imageMapper = mock(PurchaseResourceImageMapper.class);
        SalesProductDayResourceImageMapper snapshotMapper = mock(SalesProductDayResourceImageMapper.class);
        SalesProductDesignerService service = new SalesProductDesignerService(
                mock(SalesProductMapper.class), resourceMapper, mock(PurchaseRelationMapper.class), mock(SupplierMapper.class),
                mock(SupplierResourcePriceMapper.class), mock(PurchaseResourceIntroductionMapper.class), imageMapper,
                mock(SalesProductDayResourceMapper.class), snapshotMapper,
                mock(SalesProductDayResourceIntroductionMapper.class), mock(SalesProductAdultQuoteMapper.class),
                mock(SalesProductDocumentVersionMapper.class));
        SalesProductDayResourceEntity first = existingDayResource();
        first.setId(501L);
        first.setResourceId(21L);
        SalesProductDayResourceEntity second = existingDayResource();
        second.setId(502L);
        second.setResourceId(22L);
        PurchaseResourceEntity firstResource = freeResource();
        PurchaseResourceEntity secondResource = otherFreeResource();
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(firstResource, secondResource);
        java.util.concurrent.atomic.AtomicInteger imageQueryCount = new java.util.concurrent.atomic.AtomicInteger();
        when(imageMapper.selectList(any(Wrapper.class))).thenAnswer(invocation ->
                imageQueryCount.getAndIncrement() == 0 ? List.of(resourceImage(101L)) : List.of(resourceImage(202L)));
        ProductDesignerDayWordPlanSaveRequest request = new ProductDesignerDayWordPlanSaveRequest(
                88L, 1, List.of(501L, 502L), List.of(), "day_end", null,
                List.of(new ProductDesignerDayEndImageSelectionRequest(502L, 202L),
                        new ProductDesignerDayEndImageSelectionRequest(501L, 101L)));

        ReflectionTestUtils.invokeMethod(service, "saveDayEndImageSelectionsIfProvided", 1L, request,
                List.of(first, second), "admin");

        ArgumentCaptor<SalesProductDayResourceImageEntity> captor =
                ArgumentCaptor.forClass(SalesProductDayResourceImageEntity.class);
        verify(snapshotMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(item -> item.getDayResourceId() + ":" + item.getSortOrder())
                .containsExactlyInAnyOrder("502:1", "501:2");

        ProductDesignerDayWordPlanSaveRequest oneImage = new ProductDesignerDayWordPlanSaveRequest(
                88L, 1, List.of(501L, 502L), List.of(), "day_end", null,
                List.of(new ProductDesignerDayEndImageSelectionRequest(501L, 101L)));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "saveDayEndImageSelectionsIfProvided",
                1L, oneImage, List.of(first, second), "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("当天末尾图片只能选择 0、2 或 3 张");
    }

    @Test
    void saveDayItineraryShouldPersistAccommodationCityAndMeals() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductItineraryDayMapper itineraryMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                mock(SalesProductDayResourceMapper.class)
        );
        ReflectionTestUtils.setField(service, "itineraryDayMapper", itineraryMapper);
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct());
        when(itineraryMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(itineraryMapper.insert(any(SalesProductItineraryDayEntity.class))).thenAnswer(invocation -> 1);

        var response = service.saveDayItinerary(1L, new ProductDesignerDayItinerarySaveRequest(
                88L, 1, "南京市", true, false, true
        ), "admin");

        ArgumentCaptor<SalesProductItineraryDayEntity> captor = ArgumentCaptor.forClass(SalesProductItineraryDayEntity.class);
        verify(itineraryMapper).insert(captor.capture());
        assertThat(captor.getValue().getRelatedHotel()).isEqualTo("南京市");
        assertThat(captor.getValue().getBreakfastIncluded()).isTrue();
        assertThat(captor.getValue().getLunchIncluded()).isFalse();
        assertThat(captor.getValue().getDinnerIncluded()).isTrue();
        assertThat(response.accommodationCity()).isEqualTo("南京市");
    }

    @Test
    void selectedMaterialsShouldPersistAbsoluteGlobalOrderAndReturnMixedSequence() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDayResourceIntroductionMapper introductionSnapshotMapper =
                mock(SalesProductDayResourceIntroductionMapper.class);
        SalesProductDayResourceImageMapper imageSnapshotMapper = mock(SalesProductDayResourceImageMapper.class);
        SalesProductDesignerService service = new SalesProductDesignerService(
                productMapper, resourceMapper, mock(PurchaseRelationMapper.class), mock(SupplierMapper.class),
                mock(SupplierResourcePriceMapper.class), introductionMapper, mock(PurchaseResourceImageMapper.class),
                dayResourceMapper, imageSnapshotMapper, introductionSnapshotMapper,
                mock(SalesProductAdultQuoteMapper.class), mock(SalesProductDocumentVersionMapper.class));
        SalesProductDesignerOptionalItemService optionalService = mock(SalesProductDesignerOptionalItemService.class);
        var introductionImageMapper = mock(com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionImageMapper.class);
        ReflectionTestUtils.setField(service, "optionalItemService", optionalService);
        ReflectionTestUtils.setField(service, "introductionImageMapper", introductionImageMapper);
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(freeResource());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(introductionMapper.selectList(any(Wrapper.class))).thenReturn(
                List.of(introduction(601L, "灵山介绍"), introduction(602L, "梵宫介绍")));
        when(introductionImageMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(dayResourceMapper.insert(any(SalesProductDayResourceEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SalesProductDayResourceEntity.class).setId(9010L);
            return 1;
        });
        List<SalesProductDayResourceIntroductionEntity> snapshots = new ArrayList<>();
        when(introductionSnapshotMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> snapshots);
        when(introductionSnapshotMapper.insert(any(SalesProductDayResourceIntroductionEntity.class))).thenAnswer(invocation -> {
            snapshots.add(invocation.getArgument(0));
            return 1;
        });
        when(optionalService.list(eq(1L), eq(88L), eq(9010L))).thenReturn(List.of(
                new ProductDesignerSelectedOptionalItemResponse(1L, 700L, "景交车", "traffic", null,
                        null, null, new BigDecimal("40"), null, null, 2)));

        var response = service.saveDayResource(1L, new ProductDesignerDayResourceSaveRequest(
                null, 88L, 1, 21L, null, null, null, null, null, null, null, List.of(999L), null, null,
                List.of(
                        new ProductDesignerSelectedMaterialRequest("introduction", 601L, null, null, null),
                        new ProductDesignerSelectedMaterialRequest("optional_item", null, 700L, null, new BigDecimal("40")),
                        new ProductDesignerSelectedMaterialRequest("introduction", 602L, null, null, null))), "admin");

        assertThat(snapshots).extracting(SalesProductDayResourceIntroductionEntity::getSortOrder)
                .containsExactly(1, 3);
        ArgumentCaptor<List<Integer>> sortCaptor = ArgumentCaptor.forClass(List.class);
        verify(optionalService).saveWithGlobalSortOrders(eq(1L), any(), eq("admin"), anyList(), sortCaptor.capture());
        assertThat(sortCaptor.getValue()).containsExactly(2);
        assertThat(response.selectedMaterials()).extracting(item -> item.materialType() + ":" + item.sortOrder())
                .containsExactly("introduction:1", "optional_item:2", "introduction:3");
        verify(imageSnapshotMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void createDraftShouldPersistDesignDraftAndTranslateConcurrentNameConflict() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                mock(SalesProductDayResourceMapper.class)
        );
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.insert(any(SalesProductEntity.class))).thenAnswer(invocation -> {
            SalesProductEntity entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        });
        ArgumentCaptor<SalesProductEntity> productCaptor = ArgumentCaptor.forClass(SalesProductEntity.class);

        var response = service.createDraft(1L, draftRequest(), "admin");

        assertThat(response.id()).isEqualTo(88L);
        verify(productMapper).insert(productCaptor.capture());
        assertThat(productCaptor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(productCaptor.getValue().getProductScope()).isEqualTo("design_draft");
        assertThat(productCaptor.getValue().getIsDeleted()).isFalse();

        SalesProductMapper conflictMapper = mock(SalesProductMapper.class);
        SalesProductDesignerService conflictService = service(
                conflictMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                mock(SalesProductDayResourceMapper.class)
        );
        when(conflictMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(conflictMapper.insert(any(SalesProductEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique product name"));

        assertThatThrownBy(() -> conflictService.createDraft(1L, draftRequest(), "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("同名产品或设计草稿已存在");
    }

    @Test
    void draftPageShouldQueryOnlyDesignDrafts() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                mock(SalesProductDayResourceMapper.class)
        );
        Page<SalesProductEntity> page = Page.of(1, 20);
        page.setRecords(List.of(draftProduct()));
        page.setTotal(1L);
        when(productMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        ArgumentCaptor<QueryWrapper<SalesProductEntity>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);

        var result = service.pageDrafts(1L, null, null, null, 1, 20);

        assertThat(result.total()).isEqualTo(1L);
        verify(productMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("tenant_id", "is_deleted", "product_scope");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue("design_draft");
    }

    @Test
    void publishDraftShouldRejectEmptyDraftAndSwitchScopeAfterResourcesExist() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> service.publishDraft(1L, 88L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请先编排行程资源");
        verify(productMapper, never()).update(any(SalesProductEntity.class), any(Wrapper.class));

        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.update(any(SalesProductEntity.class), any(Wrapper.class))).thenReturn(1);
        ArgumentCaptor<SalesProductEntity> productCaptor = ArgumentCaptor.forClass(SalesProductEntity.class);
        ArgumentCaptor<UpdateWrapper<SalesProductEntity>> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);

        Long result = service.publishDraft(1L, 88L);

        assertThat(result).isEqualTo(88L);
        verify(productMapper).update(productCaptor.capture(), wrapperCaptor.capture());
        assertThat(productCaptor.getValue().getProductScope()).isEqualTo("template");
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("tenant_id", "is_deleted", "product_scope", "id");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue("design_draft");
    }

    @Test
    void publishDraftShouldTranslateConcurrentNameConflict() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.update(any(SalesProductEntity.class), any(Wrapper.class)))
                .thenThrow(new DataIntegrityViolationException("unique product name"));

        assertThatThrownBy(() -> service.publishDraft(1L, 88L))
                .isInstanceOf(BizException.class)
                .hasMessage("同名产品或设计草稿已存在");
    }

    @Test
    void updateDraftShouldTranslateConcurrentNameConflict() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct());
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.update(any(SalesProductEntity.class), any(Wrapper.class)))
                .thenThrow(new DataIntegrityViolationException("unique product name"));

        assertThatThrownBy(() -> service.updateDraft(1L, 88L, draftRequest()))
                .isInstanceOf(BizException.class)
                .hasMessage("同名产品或设计草稿已存在");
    }

    @Test
    void deleteDraftShouldSoftDeleteDraftAndItsDesignChildrenOnly() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDayResourceImageMapper imageMapper = mock(SalesProductDayResourceImageMapper.class);
        SalesProductAdultQuoteMapper adultQuoteMapper = mock(SalesProductAdultQuoteMapper.class);
        SalesProductDocumentVersionMapper documentVersionMapper = mock(SalesProductDocumentVersionMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper,
                adultQuoteMapper,
                imageMapper,
                documentVersionMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(draftProduct());
        when(productMapper.update(any(SalesProductEntity.class), any(Wrapper.class))).thenReturn(1);
        ArgumentCaptor<SalesProductEntity> productCaptor = ArgumentCaptor.forClass(SalesProductEntity.class);
        ArgumentCaptor<UpdateWrapper<SalesProductEntity>> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);

        service.deleteDraft(1L, 88L, "admin");

        verify(dayResourceMapper).update(any(SalesProductDayResourceEntity.class), any(Wrapper.class));
        verify(imageMapper).update(any(), any(Wrapper.class));
        verify(adultQuoteMapper).update(any(SalesProductAdultQuoteEntity.class), any(Wrapper.class));
        verify(documentVersionMapper).update(any(SalesProductDocumentVersionEntity.class), any(Wrapper.class));
        verify(productMapper).update(productCaptor.capture(), wrapperCaptor.capture());
        assertThat(productCaptor.getValue().getIsDeleted()).isTrue();
        assertThat(productCaptor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("tenant_id", "is_deleted", "product_scope", "id");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue("design_draft");
    }

    @Test
    void saveDayResourceShouldAllowFreeResourceWithoutSupplierAndCostZero() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        SupplierResourcePriceMapper priceMapper = mock(SupplierResourcePriceMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                resourceMapper,
                relationMapper,
                priceMapper,
                dayResourceMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(freeResource());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(dayResourceMapper.insert(any(SalesProductDayResourceEntity.class))).thenAnswer(invocation -> {
            SalesProductDayResourceEntity entity = invocation.getArgument(0);
            entity.setId(9001L);
            return 1;
        });
        ArgumentCaptor<SalesProductDayResourceEntity> captor = ArgumentCaptor.forClass(SalesProductDayResourceEntity.class);

        var response = service.saveDayResource(
                1L,
                new ProductDesignerDayResourceSaveRequest(
                        null,
                        88L,
                        1,
                        21L,
                        null,
                        null,
                        90,
                        true,
                        new BigDecimal("1"),
                        null,
                        "免费外观景点",
                        null
                ),
                "admin"
        );

        assertThat(response.id()).isEqualTo(9001L);
        assertThat(response.supplierId()).isNull();
        assertThat(response.unitPrice()).isEqualByComparingTo("0.00");
        assertThat(response.costAmount()).isEqualByComparingTo("0.00");
        verify(dayResourceMapper).insert(captor.capture());
        assertThat(captor.getValue().getProcurementModeSnapshot()).isEqualTo("not_required");
        assertThat(captor.getValue().getSupplierId()).isNull();
        assertThat(captor.getValue().getCostAmountSnapshot()).isEqualByComparingTo("0.00");
        verify(relationMapper, never()).selectOne(any(Wrapper.class));
        verify(priceMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void saveDayResourceShouldAssignHotelToAccommodation() throws Exception {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                resourceMapper,
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(freeHotelResource());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(dayResourceMapper.insert(any(SalesProductDayResourceEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SalesProductDayResourceEntity.class).setId(9002L);
            return 1;
        });

        service.saveDayResource(1L, new ProductDesignerDayResourceSaveRequest(
                null, 88L, 1, 22L, null, null, null, true, null, null, null, null
        ), "admin");

        ArgumentCaptor<SalesProductDayResourceEntity> captor = ArgumentCaptor.forClass(SalesProductDayResourceEntity.class);
        verify(dayResourceMapper).insert(captor.capture());
        var arrangementGetter = java.util.Arrays.stream(SalesProductDayResourceEntity.class.getMethods())
                .filter(method -> "getArrangementRole".equals(method.getName()))
                .findFirst();
        assertThat(arrangementGetter).isPresent();
        assertThat(arrangementGetter.orElseThrow().invoke(captor.getValue())).isEqualTo("accommodation");
    }

    @Test
    void saveDayResourceShouldAllowMultipleHotelsInSameDay() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper, resourceMapper, mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class), dayResourceMapper
        );
        PurchaseResourceEntity secondHotel = freeHotelResource();
        secondHotel.setId(23L);
        secondHotel.setResourceName("南京玄武酒店");
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(freeHotelResource(), secondHotel);
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(dayResourceMapper.insert(any(SalesProductDayResourceEntity.class))).thenAnswer(invocation -> 1);

        service.saveDayResource(1L, new ProductDesignerDayResourceSaveRequest(
                null, 88L, 1, 22L, null, null, null, true, null, null, null, null
        ), "admin");
        service.saveDayResource(1L, new ProductDesignerDayResourceSaveRequest(
                null, 88L, 1, 23L, null, null, null, true, null, null, null, null
        ), "admin");

        ArgumentCaptor<SalesProductDayResourceEntity> captor = ArgumentCaptor.forClass(SalesProductDayResourceEntity.class);
        verify(dayResourceMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).allMatch(item -> "accommodation".equals(item.getArrangementRole()));
    }

    @Test
    void saveDayResourceShouldSnapshotIntroductionNoticeContent() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                resourceMapper,
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper,
                introductionMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(freeResource());
        when(introductionMapper.selectOne(any(Wrapper.class))).thenReturn(publishedIntroduction());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(dayResourceMapper.insert(any(SalesProductDayResourceEntity.class))).thenAnswer(invocation -> {
            SalesProductDayResourceEntity entity = invocation.getArgument(0);
            entity.setId(9002L);
            return 1;
        });
        ArgumentCaptor<SalesProductDayResourceEntity> captor = ArgumentCaptor.forClass(SalesProductDayResourceEntity.class);

        var response = service.saveDayResource(
                1L,
                new ProductDesignerDayResourceSaveRequest(
                        null,
                        88L,
                        1,
                        21L,
                        null,
                        601L,
                        90,
                        true,
                        BigDecimal.ONE,
                        null,
                        "选择介绍",
                        null
                ),
                "admin"
        );

        assertThat(response.introductionTitle()).isEqualTo("西湖讲解");
        assertThat(response.introductionContent()).isEqualTo("正文保持黑色。");
        assertThat(response.introductionNotice()).isEqualTo("雨天注意防滑\n请勿下水");
        assertThat(response.introductionWarmTip()).isEqualTo("建议穿舒适鞋子");
        assertThat(response.introductionVisitDuration()).isEqualTo("约 2 小时");
        verify(dayResourceMapper).insert(captor.capture());
        assertThat(captor.getValue().getSelectedIntroductionId()).isEqualTo(601L);
        assertThat(captor.getValue().getIntroductionNoticeSnapshot()).isEqualTo("雨天注意防滑\n请勿下水");
        assertThat(captor.getValue().getIntroductionWarmTipSnapshot()).isEqualTo("建议穿舒适鞋子");
        assertThat(captor.getValue().getIntroductionVisitDurationSnapshot()).isEqualTo("约 2 小时");
    }

    @Test
    void saveDayResourceShouldPersistMultipleIntroductionSnapshotsInRequestOrder() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDayResourceIntroductionMapper dayResourceIntroductionMapper =
                mock(SalesProductDayResourceIntroductionMapper.class);
        SalesProductDesignerService service = new SalesProductDesignerService(
                productMapper,
                resourceMapper,
                mock(PurchaseRelationMapper.class),
                mock(SupplierMapper.class),
                mock(SupplierResourcePriceMapper.class),
                introductionMapper,
                mock(PurchaseResourceImageMapper.class),
                dayResourceMapper,
                mock(SalesProductDayResourceImageMapper.class),
                dayResourceIntroductionMapper,
                mock(SalesProductAdultQuoteMapper.class),
                mock(SalesProductDocumentVersionMapper.class)
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(freeResource());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(introductionMapper.selectList(any(Wrapper.class))).thenReturn(
                List.of(introduction(601L, "主体介绍"), introduction(602L, "九龙灌浴"))
        );
        when(dayResourceMapper.insert(any(SalesProductDayResourceEntity.class))).thenAnswer(invocation -> {
            SalesProductDayResourceEntity entity = invocation.getArgument(0);
            entity.setId(9004L);
            return 1;
        });
        List<SalesProductDayResourceIntroductionEntity> savedSnapshots = new ArrayList<>();
        when(dayResourceIntroductionMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> savedSnapshots);
        when(dayResourceIntroductionMapper.insert(any(SalesProductDayResourceIntroductionEntity.class)))
                .thenAnswer(invocation -> {
                    SalesProductDayResourceIntroductionEntity entity = invocation.getArgument(0);
                    entity.setId(9100L + entity.getSortOrder());
                    savedSnapshots.add(entity);
                    return 1;
                });

        var response = service.saveDayResource(
                1L,
                new ProductDesignerDayResourceSaveRequest(
                        null, 88L, 1, 21L, null, null, 90, true, BigDecimal.ONE, null,
                        "组合多个介绍", null, List.of(602L, 601L)
                ),
                "admin"
        );

        assertThat(response.selectedIntroductionIds()).containsExactly(602L, 601L);
        assertThat(response.introductionSnapshots())
                .extracting(item -> item.title())
                .containsExactly("九龙灌浴", "主体介绍");
        ArgumentCaptor<SalesProductDayResourceIntroductionEntity> snapshotCaptor =
                ArgumentCaptor.forClass(SalesProductDayResourceIntroductionEntity.class);
        verify(dayResourceIntroductionMapper, org.mockito.Mockito.times(2))
                .insert(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getAllValues())
                .extracting(SalesProductDayResourceIntroductionEntity::getResourceIntroductionId)
                .containsExactly(602L, 601L);
        assertThat(snapshotCaptor.getAllValues())
                .extracting(SalesProductDayResourceIntroductionEntity::getSortOrder)
                .containsExactly(1, 2);
    }

    @Test
    void saveDayResourceShouldKeepExistingIntroductionSnapshotWhenIntroductionIdUnchanged() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                resourceMapper,
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper,
                introductionMapper
        );
        SalesProductDayResourceEntity existing = existingDayResource();
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(freeResource());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(dayResourceMapper.update(any(SalesProductDayResourceEntity.class), any(Wrapper.class))).thenReturn(1);
        ArgumentCaptor<SalesProductDayResourceEntity> captor = ArgumentCaptor.forClass(SalesProductDayResourceEntity.class);

        var response = service.saveDayResource(
                1L,
                new ProductDesignerDayResourceSaveRequest(
                        9003L,
                        88L,
                        1,
                        21L,
                        null,
                        601L,
                        150,
                        true,
                        BigDecimal.ONE,
                        3,
                        "只改停留时间和备注",
                        null
                ),
                "admin"
        );

        verify(introductionMapper, never()).selectOne(any(Wrapper.class));
        verify(dayResourceMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStayMinutes()).isEqualTo(150);
        assertThat(captor.getValue().getRemark()).isEqualTo("只改停留时间和备注");
        assertThat(captor.getValue().getSelectedIntroductionId()).isEqualTo(601L);
        assertThat(captor.getValue().getIntroductionTitleSnapshot()).isEqualTo("历史介绍标题");
        assertThat(captor.getValue().getIntroductionContentSnapshot()).isEqualTo("历史介绍正文");
        assertThat(captor.getValue().getIntroductionNoticeSnapshot()).isEqualTo("历史红字说明");
        assertThat(response.introductionTitle()).isEqualTo("历史介绍标题");
        assertThat(response.introductionContent()).isEqualTo("历史介绍正文");
        assertThat(response.introductionNotice()).isEqualTo("历史红字说明");
    }

    @Test
    void saveDayResourceShouldRevalidateIntroductionWhenResourceChangedEvenIfIntroductionIdUnchanged() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                resourceMapper,
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper,
                introductionMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(otherFreeResource());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(dayResourceMapper.selectOne(any(Wrapper.class))).thenReturn(existingDayResource());
        when(introductionMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.saveDayResource(
                1L,
                new ProductDesignerDayResourceSaveRequest(
                        9003L,
                        88L,
                        1,
                        22L,
                        null,
                        601L,
                        150,
                        true,
                        BigDecimal.ONE,
                        3,
                        "切换资源",
                        null
                ),
                "admin"
        )).isInstanceOf(BizException.class)
                .hasMessage("只能选择当前资源已发布的介绍版本");

        verify(introductionMapper).selectOne(any(Wrapper.class));
        verify(dayResourceMapper, never()).update(any(SalesProductDayResourceEntity.class), any(Wrapper.class));
    }

    @Test
    void saveDayResourceShouldRejectDuplicateResourceInSameDay() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                resourceMapper,
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(freeResource());
        when(dayResourceMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.saveDayResource(
                1L,
                new ProductDesignerDayResourceSaveRequest(null, 88L, 1, 21L, null, null, null, true, null, null, null, null),
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("同一天不能重复加入同一个资源");

        verify(dayResourceMapper, never()).insert(any(SalesProductDayResourceEntity.class));
    }

    @Test
    void saveAdultQuoteShouldRecalculateCostAndDeriveMarkupOnServer() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductAdultQuoteMapper adultQuoteMapper = mock(SalesProductAdultQuoteMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper,
                adultQuoteMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        SalesProductDayResourceEntity resource = new SalesProductDayResourceEntity();
        resource.setCostAmountSnapshot(new BigDecimal("90.00"));
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(resource));
        when(adultQuoteMapper.insert(any(SalesProductAdultQuoteEntity.class))).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, SalesProductAdultQuoteEntity.class);
            entity.setId(9100L);
            return 1;
        });

        var response = service.saveAdultQuote(
                1L,
                new ProductDesignerAdultQuoteSaveRequest(
                        null, 88L, 1, null, new BigDecimal("100.00"), null, "测试报价", null
                ),
                "admin"
        );

        assertThat(response.adultCostAmount()).isEqualByComparingTo("90.00");
        assertThat(response.markupAmount()).isEqualByComparingTo("10.00");
        assertThat(response.adultSaleAmount()).isEqualByComparingTo("100.00");
        verify(adultQuoteMapper).insert(any(SalesProductAdultQuoteEntity.class));
    }

    @Test
    void saveAdultQuoteShouldRejectSaleBelowServerCost() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductAdultQuoteMapper adultQuoteMapper = mock(SalesProductAdultQuoteMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                dayResourceMapper,
                adultQuoteMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        SalesProductDayResourceEntity resource = new SalesProductDayResourceEntity();
        resource.setCostAmountSnapshot(new BigDecimal("90.00"));
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(resource));

        assertThatThrownBy(() -> service.saveAdultQuote(
                1L,
                new ProductDesignerAdultQuoteSaveRequest(
                        null, 88L, 1, null, new BigDecimal("89.99"), null, null, null
                ),
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能低于后端成本");
        verify(adultQuoteMapper, never()).insert(any(SalesProductAdultQuoteEntity.class));
    }

    @Test
    void mapResourcesShouldReturnActiveNonTrafficResourcesWithoutCoordinates() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDesignerService service = service(
                productMapper,
                resourceMapper,
                mock(PurchaseRelationMapper.class),
                mock(SupplierResourcePriceMapper.class),
                mock(SalesProductDayResourceMapper.class),
                mock(SalesProductAdultQuoteMapper.class)
        );
        PurchaseResourceEntity resource = freeResource();
        Page<PurchaseResourceEntity> page = Page.of(1, 50);
        page.setRecords(List.of(resource));
        page.setTotal(1);
        when(resourceMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        var result = service.resources(1L, null, null, null, null, null, null, 1, 50);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.resourceName()).isEqualTo("西湖外观");
            assertThat(item.longitude()).isNotNull();
            assertThat(item.latitude()).isNotNull();
        });
        ArgumentCaptor<QueryWrapper<PurchaseResourceEntity>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(resourceMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("resource_type")
                .contains("status");
    }

    @Test
    void mapResourcesShouldMatchCityWithOrWithoutAdministrativeSuffix() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDesignerService service = service(
                mock(SalesProductMapper.class), resourceMapper,
                mock(PurchaseRelationMapper.class), mock(SupplierResourcePriceMapper.class),
                mock(SalesProductDayResourceMapper.class), mock(SalesProductAdultQuoteMapper.class)
        );
        Page<PurchaseResourceEntity> page = Page.of(1, 50);
        page.setRecords(List.of(freeResource()));
        page.setTotal(1);
        when(resourceMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        ArgumentCaptor<QueryWrapper<PurchaseResourceEntity>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);

        service.resources(1L, null, "scenic", null, "苏州", null, null, 1, 50);

        verify(resourceMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("city");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .contains("苏州市", "苏州");
    }

    @Test
    void mapResourcesShouldFilterScenicLevelAndRestaurantStarLevel() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesProductDesignerService service = service(
                mock(SalesProductMapper.class), resourceMapper,
                mock(PurchaseRelationMapper.class), mock(SupplierResourcePriceMapper.class),
                mock(SalesProductDayResourceMapper.class), mock(SalesProductAdultQuoteMapper.class)
        );
        Page<PurchaseResourceEntity> page = Page.of(1, 50);
        page.setRecords(List.of());
        page.setTotal(0);
        when(resourceMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        ArgumentCaptor<QueryWrapper<PurchaseResourceEntity>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);

        service.resources(1L, null, "scenic", null, null, "5a", null, 1, 50);
        service.resources(1L, null, "restaurant", null, null, null, "3star", 1, 50);
        verify(resourceMapper, org.mockito.Mockito.times(2)).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getAllValues().get(0).getSqlSegment()).contains("scenic_level");
        assertThat(wrapperCaptor.getAllValues().get(0).getParamNameValuePairs().values()).contains("5a");
        assertThat(wrapperCaptor.getAllValues().get(1).getSqlSegment()).contains("star_level");
        assertThat(wrapperCaptor.getAllValues().get(1).getParamNameValuePairs().values()).contains("3star");
    }

    @Test
    void mapResourcesShouldRejectLevelFilterForWrongResourceType() {
        SalesProductDesignerService service = service(
                mock(SalesProductMapper.class), mock(PurchaseResourceMapper.class),
                mock(PurchaseRelationMapper.class), mock(SupplierResourcePriceMapper.class),
                mock(SalesProductDayResourceMapper.class), mock(SalesProductAdultQuoteMapper.class)
        );

        assertThatThrownBy(() -> service.resources(1L, null, "hotel", null, null, "5a", null, 1, 50))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("景区等级");
        assertThatThrownBy(() -> service.resources(1L, null, "scenic", null, null, null, "3star", 1, 50))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("星级");
    }

    @Test
    void mapResourcesShouldUseJoinedSupplierPriceRowsForDefaultSupplier() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        SalesProductDesignerService service = service(
                mock(SalesProductMapper.class), resourceMapper, relationMapper,
                mock(SupplierResourcePriceMapper.class), mock(SalesProductDayResourceMapper.class)
        );
        Page<PurchaseResourceEntity> page = Page.of(1, 50);
        page.setRecords(List.of(freeResource()));
        page.setTotal(1);
        when(resourceMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(relationMapper.selectActiveResourceSupplierPriceRows(eq(1L), anyList()))
                .thenReturn(List.of(supplierPriceRow()));

        var result = service.resources(1L, null, null, null, null, null, null, 1, 50);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.defaultSupplierId()).isEqualTo(100L);
            assertThat(item.defaultSupplierName()).isEqualTo("西湖票务供应商");
            assertThat(item.referenceUnitPrice()).isEqualByComparingTo("68.00");
        });
        verify(relationMapper).selectActiveResourceSupplierPriceRows(eq(1L), anyList());
    }

    private SalesProductDesignerService service(
            SalesProductMapper productMapper,
            PurchaseResourceMapper resourceMapper,
            PurchaseRelationMapper relationMapper,
            SupplierResourcePriceMapper priceMapper,
            SalesProductDayResourceMapper dayResourceMapper
    ) {
        return service(productMapper, resourceMapper, relationMapper, priceMapper, dayResourceMapper,
                mock(SalesProductAdultQuoteMapper.class));
    }

    private SalesProductDesignerService service(
            SalesProductMapper productMapper,
            PurchaseResourceMapper resourceMapper,
            PurchaseRelationMapper relationMapper,
            SupplierResourcePriceMapper priceMapper,
            SalesProductDayResourceMapper dayResourceMapper,
            PurchaseResourceIntroductionMapper introductionMapper
    ) {
        return new SalesProductDesignerService(
                productMapper,
                resourceMapper,
                relationMapper,
                mock(SupplierMapper.class),
                priceMapper,
                introductionMapper,
                mock(PurchaseResourceImageMapper.class),
                dayResourceMapper,
                mock(SalesProductDayResourceImageMapper.class),
                mock(com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceIntroductionMapper.class),
                mock(SalesProductAdultQuoteMapper.class),
                mock(SalesProductDocumentVersionMapper.class)
        );
    }

    private SalesProductDesignerService service(
            SalesProductMapper productMapper,
            PurchaseResourceMapper resourceMapper,
            PurchaseRelationMapper relationMapper,
            SupplierResourcePriceMapper priceMapper,
            SalesProductDayResourceMapper dayResourceMapper,
            SalesProductAdultQuoteMapper adultQuoteMapper
    ) {
        return service(
                productMapper,
                resourceMapper,
                relationMapper,
                priceMapper,
                dayResourceMapper,
                adultQuoteMapper,
                mock(SalesProductDayResourceImageMapper.class),
                mock(SalesProductDocumentVersionMapper.class)
        );
    }

    private SalesProductDesignerService service(
            SalesProductMapper productMapper,
            PurchaseResourceMapper resourceMapper,
            PurchaseRelationMapper relationMapper,
            SupplierResourcePriceMapper priceMapper,
            SalesProductDayResourceMapper dayResourceMapper,
            SalesProductAdultQuoteMapper adultQuoteMapper,
            SalesProductDayResourceImageMapper dayResourceImageMapper,
            SalesProductDocumentVersionMapper documentVersionMapper
    ) {
        return new SalesProductDesignerService(
                productMapper,
                resourceMapper,
                relationMapper,
                mock(SupplierMapper.class),
                priceMapper,
                mock(PurchaseResourceIntroductionMapper.class),
                mock(PurchaseResourceImageMapper.class),
                dayResourceMapper,
                dayResourceImageMapper,
                mock(com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceIntroductionMapper.class),
                adultQuoteMapper,
                documentVersionMapper
        );
    }

    private ProductDesignerDraftSaveRequest draftRequest() {
        return new ProductDesignerDraftSaveRequest(
                "杭州西湖三日游", "疗休养", "domestic", "浙江省", "杭州市", "西湖区",
                "四钻", "观光", 3, "设计草稿"
        );
    }

    private SalesProductEntity draftProduct() {
        SalesProductEntity entity = product();
        entity.setProductScope("design_draft");
        return entity;
    }

    private SalesProductEntity product() {
        SalesProductEntity entity = new SalesProductEntity();
        entity.setId(88L);
        entity.setTenantId(1L);
        entity.setProductName("西湖三日产品");
        entity.setProductScope("template");
        entity.setTravelDays(3);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private PurchaseResourceEntity freeResource() {
        PurchaseResourceEntity entity = new PurchaseResourceEntity();
        entity.setId(21L);
        entity.setTenantId(1L);
        entity.setResourceType("scenic");
        entity.setResourceName("西湖外观");
        entity.setProvince("浙江省");
        entity.setCity("杭州市");
        entity.setAddress("西湖区");
        entity.setLongitude(new BigDecimal("120.1234567"));
        entity.setLatitude(new BigDecimal("30.1234567"));
        entity.setProcurementMode("not_required");
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private PurchaseResourceEntity otherFreeResource() {
        PurchaseResourceEntity entity = freeResource();
        entity.setId(22L);
        entity.setResourceName("断桥残雪");
        entity.setAddress("上城区");
        return entity;
    }

    private PurchaseResourceEntity freeHotelResource() {
        PurchaseResourceEntity entity = freeResource();
        entity.setId(22L);
        entity.setResourceType("hotel");
        entity.setResourceName("南京金陵酒店");
        entity.setCity("南京市");
        return entity;
    }

    private PurchaseRelationSupplierPriceRow supplierPriceRow() {
        PurchaseRelationSupplierPriceRow row = new PurchaseRelationSupplierPriceRow();
        row.setResourceId(21L);
        row.setRelationId(301L);
        row.setSupplierId(100L);
        row.setSupplierName("西湖票务供应商");
        row.setDefaultSupplier(true);
        row.setPriceMode("classified");
        row.setResourceProjectId(401L);
        row.setProjectName("成人票");
        row.setMarketPrice(new BigDecimal("88.00"));
        row.setPeerPrice(new BigDecimal("72.00"));
        row.setTeamPrice(new BigDecimal("68.00"));
        return row;
    }

    private PurchaseResourceIntroductionEntity publishedIntroduction() {
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setId(601L);
        entity.setTenantId(1L);
        entity.setResourceId(21L);
        entity.setTitle("西湖讲解");
        entity.setContent("正文保持黑色。");
        entity.setNoticeContent("雨天注意防滑\n请勿下水");
        entity.setWarmTipContent("建议穿舒适鞋子");
        entity.setVisitDuration("约 2 小时");
        entity.setStatus("published");
        entity.setIndexVersion(7);
        entity.setIsDeleted(false);
        return entity;
    }

    private PurchaseResourceIntroductionEntity introduction(Long id, String title) {
        PurchaseResourceIntroductionEntity entity = publishedIntroduction();
        entity.setId(id);
        entity.setTitle(title);
        entity.setContent(title + "正文");
        entity.setNoticeContent(title + "注意事项");
        entity.setWarmTipContent(title + "温馨提示");
        entity.setVisitDuration("约 1 小时");
        return entity;
    }

    private PurchaseResourceImageEntity resourceImage(Long id) {
        PurchaseResourceImageEntity entity = new PurchaseResourceImageEntity();
        entity.setId(id);
        entity.setAttachmentId(id + 1000);
        entity.setOriginalFilename("image-" + id + ".jpg");
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private SalesProductDayResourceEntity existingDayResource() {
        SalesProductDayResourceEntity entity = new SalesProductDayResourceEntity();
        entity.setId(9003L);
        entity.setTenantId(1L);
        entity.setProductId(88L);
        entity.setDayNo(1);
        entity.setResourceId(21L);
        entity.setResourceNameSnapshot("西湖外观");
        entity.setResourceTypeSnapshot("scenic");
        entity.setProcurementModeSnapshot("not_required");
        entity.setSortOrder(3);
        entity.setStayMinutes(90);
        entity.setIncludeInWord(true);
        entity.setQuantitySnapshot(BigDecimal.ONE);
        entity.setUnitPriceSnapshot(BigDecimal.ZERO.setScale(2));
        entity.setCostAmountSnapshot(BigDecimal.ZERO.setScale(2));
        entity.setSelectedIntroductionId(601L);
        entity.setIntroductionIndexVersion(4);
        entity.setIntroductionTitleSnapshot("历史介绍标题");
        entity.setIntroductionContentSnapshot("历史介绍正文");
        entity.setIntroductionNoticeSnapshot("历史红字说明");
        entity.setIsDeleted(false);
        return entity;
    }
}
