package com.mtravel.platform.sales.product.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.sales.product.dto.SalesProductArrangementItemRequest;
import com.mtravel.platform.sales.product.dto.SalesProductArrangementUpsertRequest;
import com.mtravel.platform.sales.product.dto.SalesProductArrangementPriceLineRequest;
import com.mtravel.platform.sales.product.dto.SalesProductItineraryDayRequest;
import com.mtravel.platform.sales.product.dto.SalesProductRoadbookPointRequest;
import com.mtravel.platform.sales.product.dto.SalesProductResponse;
import com.mtravel.platform.sales.product.dto.SalesProductSaveRequest;
import com.mtravel.platform.sales.product.dto.SalesProductVehicleInquiryRequest;
import com.mtravel.platform.sales.product.dto.SalesProductVehicleQuoteSnapshotRequest;
import com.mtravel.platform.sales.product.designer.entity.SalesProductAdultQuoteEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceImageEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDocumentVersionEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductAdultQuoteMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceImageMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDocumentVersionMapper;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementItemEntity;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementPriceLineEntity;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.entity.SalesProductRoadbookPointEntity;
import com.mtravel.platform.sales.product.entity.SalesProductVehicleInquiryEntity;
import com.mtravel.platform.sales.product.entity.SalesProductVehicleQuoteSnapshotEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductArrangementItemMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductArrangementPriceLineMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductDescriptionMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductRoadbookPointMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductVehicleInquiryMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductVehicleQuoteSnapshotMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售产品服务测试。
 *
 * <p>产品管理是线路模板，不是轻量商品列表。本测试固定产品主档、行程、产品说明和团队安排
 * 四块数据一起保存的行为，避免后续把老系统的产品能力缩成简单名称价格表。</p>
 */
class SalesProductServiceTest {

    @Test
    void createShouldPersistProductTemplateWithItineraryDescriptionAndArrangement() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductItineraryDayMapper itineraryMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductArrangementItemMapper arrangementMapper = mock(SalesProductArrangementItemMapper.class);
        SalesProductArrangementPriceLineMapper priceLineMapper = mock(SalesProductArrangementPriceLineMapper.class);
        SalesProductRoadbookPointMapper roadbookMapper = mock(SalesProductRoadbookPointMapper.class);
        SalesProductVehicleQuoteSnapshotMapper vehicleQuoteMapper = mock(SalesProductVehicleQuoteSnapshotMapper.class);
        SalesProductVehicleInquiryMapper vehicleInquiryMapper = mock(SalesProductVehicleInquiryMapper.class);
        SalesProductService service = service(
                productMapper,
                itineraryMapper,
                descriptionMapper,
                arrangementMapper,
                priceLineMapper,
                roadbookMapper,
                vehicleQuoteMapper,
                vehicleInquiryMapper
        );
        ArgumentCaptor<SalesProductEntity> productCaptor = ArgumentCaptor.forClass(SalesProductEntity.class);
        ArgumentCaptor<SalesProductItineraryDayEntity> itineraryCaptor = ArgumentCaptor.forClass(SalesProductItineraryDayEntity.class);
        ArgumentCaptor<SalesProductDescriptionEntity> descriptionCaptor = ArgumentCaptor.forClass(SalesProductDescriptionEntity.class);
        ArgumentCaptor<SalesProductArrangementItemEntity> arrangementCaptor = ArgumentCaptor.forClass(SalesProductArrangementItemEntity.class);
        ArgumentCaptor<SalesProductArrangementPriceLineEntity> priceLineCaptor = ArgumentCaptor.forClass(SalesProductArrangementPriceLineEntity.class);
        ArgumentCaptor<SalesProductRoadbookPointEntity> roadbookCaptor = ArgumentCaptor.forClass(SalesProductRoadbookPointEntity.class);
        ArgumentCaptor<SalesProductVehicleQuoteSnapshotEntity> vehicleQuoteCaptor = ArgumentCaptor.forClass(SalesProductVehicleQuoteSnapshotEntity.class);
        ArgumentCaptor<SalesProductVehicleInquiryEntity> vehicleInquiryCaptor = ArgumentCaptor.forClass(SalesProductVehicleInquiryEntity.class);
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.insert(any(SalesProductEntity.class))).thenAnswer((Answer<Integer>) invocation -> {
            SalesProductEntity entity = invocation.getArgument(0);
            entity.setId(88L);
            return 1;
        });
        when(productMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            SalesProductEntity entity = new SalesProductEntity();
            entity.setId(88L);
            entity.setTenantId(1L);
            entity.setProductName("苏州园林二日游");
            entity.setTravelDays(2);
            entity.setStatus("active");
            entity.setIsDeleted(false);
            return entity;
        });
        when(itineraryMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(descriptionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(priceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(vehicleQuoteMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(vehicleInquiryMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(arrangementMapper.insert(any(SalesProductArrangementItemEntity.class))).thenAnswer((Answer<Integer>) invocation -> {
            SalesProductArrangementItemEntity entity = invocation.getArgument(0);
            entity.setId(9901L);
            return 1;
        });

        SalesProductResponse response = service.create(request(), 1L, "admin");

        assertThat(response.id()).isEqualTo(88L);
        verify(productMapper).insert(productCaptor.capture());
        assertThat(productCaptor.getValue().getProductName()).isEqualTo("苏州园林二日游");
        assertThat(productCaptor.getValue().getProductScope()).isEqualTo("template");
        assertThat(productCaptor.getValue().getTripType()).isEqualTo("daily");
        verify(itineraryMapper).insert(itineraryCaptor.capture());
        assertThat(itineraryCaptor.getValue().getProductId()).isEqualTo(88L);
        assertThat(itineraryCaptor.getValue().getDayNo()).isEqualTo(1);
        assertThat(itineraryCaptor.getValue().getRoadbookSummary()).isEqualTo("苏州站 -> 拙政园");
        assertThat(itineraryCaptor.getValue().getRoadbookTotalDistanceMeters()).isEqualTo(12_300);
        verify(roadbookMapper).insert(roadbookCaptor.capture());
        assertThat(roadbookCaptor.getValue().getProductId()).isEqualTo(88L);
        assertThat(roadbookCaptor.getValue().getDayNo()).isEqualTo(1);
        assertThat(roadbookCaptor.getValue().getPlaceName()).isEqualTo("苏州站");
        assertThat(roadbookCaptor.getValue().getDistanceToNextMeters()).isEqualTo(12_300);
        verify(descriptionMapper).insert(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().getFeeIncluded()).contains("首道门票");
        verify(arrangementMapper).insert(arrangementCaptor.capture());
        assertThat(arrangementCaptor.getValue().getArrangementType()).isEqualTo("hotel");
        assertThat(arrangementCaptor.getValue().getSettlementType()).isEqualTo("credit");
        assertThat(arrangementCaptor.getValue().getScheduleStartDay()).isEqualTo("第1天");
        assertThat(arrangementCaptor.getValue().getScheduleEndDay()).isEqualTo("第2天");
        assertThat(arrangementCaptor.getValue().getSupplierName()).isEqualTo("苏州酒店供应商");
        assertThat(arrangementCaptor.getValue().getCashAmount()).isEqualByComparingTo("20.00");
        assertThat(arrangementCaptor.getValue().getCreditAmount()).isEqualByComparingTo("200.00");
        verify(priceLineMapper).insert(priceLineCaptor.capture());
        assertThat(priceLineCaptor.getValue().getArrangementItemId()).isEqualTo(9901L);
        assertThat(priceLineCaptor.getValue().getProjectName()).isEqualTo("标间");
        assertThat(priceLineCaptor.getValue().getAmount()).isEqualByComparingTo("220.00");
        verify(vehicleQuoteMapper).insert(vehicleQuoteCaptor.capture());
        assertThat(vehicleQuoteCaptor.getValue().getArrangementItemId()).isEqualTo(9901L);
        assertThat(vehicleQuoteCaptor.getValue().getSyncedDistanceMeters()).isEqualTo(123_000);
        assertThat(vehicleQuoteCaptor.getValue().getCalculatedAmount()).isEqualByComparingTo("1450.00");
        verify(vehicleInquiryMapper).insert(vehicleInquiryCaptor.capture());
        assertThat(vehicleInquiryCaptor.getValue().getSupplierName()).isEqualTo("苏州车队");
        assertThat(vehicleInquiryCaptor.getValue().getQuotedAmount()).isEqualByComparingTo("1380.00");
        assertThat(vehicleInquiryCaptor.getValue().getSelected()).isTrue();
    }

    @Test
    void createShouldRejectDuplicateProductNameInSameTenant() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductService service = service(
                productMapper,
                mock(SalesProductItineraryDayMapper.class),
                mock(SalesProductDescriptionMapper.class),
                mock(SalesProductArrangementItemMapper.class),
                mock(SalesProductArrangementPriceLineMapper.class),
                mock(SalesProductRoadbookPointMapper.class),
                mock(SalesProductVehicleQuoteSnapshotMapper.class),
                mock(SalesProductVehicleInquiryMapper.class)
        );
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request(), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("同名产品或设计草稿已存在");
    }

    @Test
    void createShouldTranslateConcurrentNameConstraintViolation() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductService service = service(
                productMapper,
                mock(SalesProductItineraryDayMapper.class),
                mock(SalesProductDescriptionMapper.class),
                mock(SalesProductArrangementItemMapper.class),
                mock(SalesProductArrangementPriceLineMapper.class),
                mock(SalesProductRoadbookPointMapper.class),
                mock(SalesProductVehicleQuoteSnapshotMapper.class),
                mock(SalesProductVehicleInquiryMapper.class)
        );
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.insert(any(SalesProductEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique product name"));

        assertThatThrownBy(() -> service.create(request(), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("同名产品或设计草稿已存在");
    }

    @Test
    void updateShouldTranslateConcurrentNameConstraintViolation() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductService service = service(
                productMapper,
                mock(SalesProductItineraryDayMapper.class),
                mock(SalesProductDescriptionMapper.class),
                mock(SalesProductArrangementItemMapper.class),
                mock(SalesProductArrangementPriceLineMapper.class),
                mock(SalesProductRoadbookPointMapper.class),
                mock(SalesProductVehicleQuoteSnapshotMapper.class),
                mock(SalesProductVehicleInquiryMapper.class)
        );
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.update(any(SalesProductEntity.class), any(Wrapper.class)))
                .thenThrow(new DataIntegrityViolationException("unique product name"));

        assertThatThrownBy(() -> service.update(88L, request(), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("同名产品或设计草稿已存在");
    }

    @Test
    void officialProductDetailShouldQueryOnlyTemplateScope() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductService service = service(
                productMapper,
                mock(SalesProductItineraryDayMapper.class),
                mock(SalesProductDescriptionMapper.class),
                mock(SalesProductArrangementItemMapper.class),
                mock(SalesProductArrangementPriceLineMapper.class),
                mock(SalesProductRoadbookPointMapper.class),
                mock(SalesProductVehicleQuoteSnapshotMapper.class),
                mock(SalesProductVehicleInquiryMapper.class)
        );
        ArgumentCaptor<QueryWrapper<SalesProductEntity>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);

        assertThatThrownBy(() -> service.detail(88L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessage("产品不存在或已删除");

        verify(productMapper).selectOne(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("tenant_id", "is_deleted", "product_scope", "id");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue("template");
    }

    @Test
    void deleteShouldSoftDeleteDesignerChildrenAfterDraftWasPublished() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDayResourceImageMapper imageMapper = mock(SalesProductDayResourceImageMapper.class);
        SalesProductAdultQuoteMapper quoteMapper = mock(SalesProductAdultQuoteMapper.class);
        SalesProductDocumentVersionMapper documentMapper = mock(SalesProductDocumentVersionMapper.class);
        SalesProductService service = new SalesProductService(
                productMapper,
                mock(SalesProductItineraryDayMapper.class),
                mock(SalesProductDescriptionMapper.class),
                mock(SalesProductArrangementItemMapper.class),
                mock(SalesProductArrangementPriceLineMapper.class),
                mock(SalesProductRoadbookPointMapper.class),
                mock(SalesProductVehicleQuoteSnapshotMapper.class),
                mock(SalesProductVehicleInquiryMapper.class),
                dayResourceMapper,
                imageMapper,
                quoteMapper,
                documentMapper
        );
        when(productMapper.update(any(SalesProductEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(88L, 1L, "admin");

        verify(imageMapper).update(any(SalesProductDayResourceImageEntity.class), any(Wrapper.class));
        verify(dayResourceMapper).update(any(SalesProductDayResourceEntity.class), any(Wrapper.class));
        verify(quoteMapper).update(any(SalesProductAdultQuoteEntity.class), any(Wrapper.class));
        verify(documentMapper).update(any(SalesProductDocumentVersionEntity.class), any(Wrapper.class));
    }

    @Test
    void updateArrangementsShouldOnlyReplaceArrangementChildren() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductItineraryDayMapper itineraryMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductArrangementItemMapper arrangementMapper = mock(SalesProductArrangementItemMapper.class);
        SalesProductArrangementPriceLineMapper priceLineMapper = mock(SalesProductArrangementPriceLineMapper.class);
        SalesProductRoadbookPointMapper roadbookMapper = mock(SalesProductRoadbookPointMapper.class);
        SalesProductVehicleQuoteSnapshotMapper vehicleQuoteMapper = mock(SalesProductVehicleQuoteSnapshotMapper.class);
        SalesProductVehicleInquiryMapper vehicleInquiryMapper = mock(SalesProductVehicleInquiryMapper.class);
        SalesProductService service = service(
                productMapper,
                itineraryMapper,
                descriptionMapper,
                arrangementMapper,
                priceLineMapper,
                roadbookMapper,
                vehicleQuoteMapper,
                vehicleInquiryMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(existingProduct(88L));
        when(arrangementMapper.insert(any(SalesProductArrangementItemEntity.class))).thenAnswer((Answer<Integer>) invocation -> {
            SalesProductArrangementItemEntity entity = invocation.getArgument(0);
            entity.setId(9901L);
            return 1;
        });
        service.updateArrangements(88L, request().arrangementItems(), 1L, "admin");

        verify(productMapper, never()).update(any(SalesProductEntity.class), any(Wrapper.class));
        verify(descriptionMapper, never()).selectOne(any(Wrapper.class));
        verify(itineraryMapper, never()).selectList(any(Wrapper.class));
        verify(roadbookMapper, never()).selectList(any(Wrapper.class));
        verify(arrangementMapper, never()).selectList(any(Wrapper.class));
        verify(priceLineMapper, never()).selectList(any(Wrapper.class));
        verify(vehicleQuoteMapper, never()).selectList(any(Wrapper.class));
        verify(vehicleInquiryMapper, never()).selectList(any(Wrapper.class));
        verify(descriptionMapper, never()).insert(any(SalesProductDescriptionEntity.class));
        verify(itineraryMapper, never()).insert(any(SalesProductItineraryDayEntity.class));
        verify(roadbookMapper, never()).insert(any(SalesProductRoadbookPointEntity.class));
        verify(arrangementMapper).insert(any(SalesProductArrangementItemEntity.class));
        verify(priceLineMapper).insert(any(SalesProductArrangementPriceLineEntity.class));
    }

    @Test
    void upsertArrangementShouldOnlyReplaceCurrentArrangementItem() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductItineraryDayMapper itineraryMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductArrangementItemMapper arrangementMapper = mock(SalesProductArrangementItemMapper.class);
        SalesProductArrangementPriceLineMapper priceLineMapper = mock(SalesProductArrangementPriceLineMapper.class);
        SalesProductRoadbookPointMapper roadbookMapper = mock(SalesProductRoadbookPointMapper.class);
        SalesProductVehicleQuoteSnapshotMapper vehicleQuoteMapper = mock(SalesProductVehicleQuoteSnapshotMapper.class);
        SalesProductVehicleInquiryMapper vehicleInquiryMapper = mock(SalesProductVehicleInquiryMapper.class);
        SalesProductService service = service(
                productMapper,
                itineraryMapper,
                descriptionMapper,
                arrangementMapper,
                priceLineMapper,
                roadbookMapper,
                vehicleQuoteMapper,
                vehicleInquiryMapper
        );
        SalesProductArrangementItemEntity existing = new SalesProductArrangementItemEntity();
        existing.setId(9901L);
        existing.setTenantId(1L);
        existing.setProductId(88L);
        existing.setIsDeleted(false);
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(existingProduct(88L));
        when(arrangementMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(arrangementMapper.insert(any(SalesProductArrangementItemEntity.class))).thenAnswer((Answer<Integer>) invocation -> {
            SalesProductArrangementItemEntity entity = invocation.getArgument(0);
            entity.setId(9902L);
            return 1;
        });

        Long savedId = service.upsertArrangement(
                88L,
                new SalesProductArrangementUpsertRequest(9901L, request().arrangementItems().get(0)),
                1L,
                "admin"
        );

        assertThat(savedId).isEqualTo(9902L);
        verify(descriptionMapper, never()).selectOne(any(Wrapper.class));
        verify(itineraryMapper, never()).selectList(any(Wrapper.class));
        verify(roadbookMapper, never()).selectList(any(Wrapper.class));
        verify(productMapper, never()).update(any(SalesProductEntity.class), any(Wrapper.class));
        verify(descriptionMapper, never()).insert(any(SalesProductDescriptionEntity.class));
        verify(itineraryMapper, never()).insert(any(SalesProductItineraryDayEntity.class));
        verify(roadbookMapper, never()).insert(any(SalesProductRoadbookPointEntity.class));
        verify(arrangementMapper).selectOne(any(Wrapper.class));
        verify(arrangementMapper).update(any(SalesProductArrangementItemEntity.class), any(Wrapper.class));
        verify(priceLineMapper).update(any(SalesProductArrangementPriceLineEntity.class), any(Wrapper.class));
        verify(vehicleQuoteMapper).update(any(SalesProductVehicleQuoteSnapshotEntity.class), any(Wrapper.class));
        verify(vehicleInquiryMapper).update(any(SalesProductVehicleInquiryEntity.class), any(Wrapper.class));
        verify(arrangementMapper).insert(any(SalesProductArrangementItemEntity.class));
    }

    private SalesProductService service(
            SalesProductMapper productMapper,
            SalesProductItineraryDayMapper itineraryMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductArrangementItemMapper arrangementMapper,
            SalesProductArrangementPriceLineMapper priceLineMapper,
            SalesProductRoadbookPointMapper roadbookMapper,
            SalesProductVehicleQuoteSnapshotMapper vehicleQuoteMapper,
            SalesProductVehicleInquiryMapper vehicleInquiryMapper
    ) {
        return new SalesProductService(
                productMapper,
                itineraryMapper,
                descriptionMapper,
                arrangementMapper,
                priceLineMapper,
                roadbookMapper,
                vehicleQuoteMapper,
                vehicleInquiryMapper,
                mock(SalesProductDayResourceMapper.class),
                mock(SalesProductDayResourceImageMapper.class),
                mock(SalesProductAdultQuoteMapper.class),
                mock(SalesProductDocumentVersionMapper.class)
        );
    }

    private SalesProductSaveRequest request() {
        return new SalesProductSaveRequest(
                "苏州园林二日游",
                "疗休养",
                "domestic",
                "江苏省",
                "苏州市",
                "姑苏区",
                "daily",
                "携程四钻",
                "观光",
                2,
                1,
                new BigDecimal("120.00"),
                40,
                "active",
                List.of(new SalesProductItineraryDayRequest(
                        1,
                        "各地赴苏州",
                        "集合后游览苏州园林。",
                        "苏州市区四钻酒店",
                        "苏州酒店",
                        new BigDecimal("0"),
                        true,
                        true,
                        true,
                        "拙政园",
                        "苏州站 -> 拙政园",
                        12_300,
                        1_800,
                        List.of(new SalesProductRoadbookPointRequest(
                                1,
                                "苏州站",
                                "江苏省苏州市姑苏区苏站路",
                                "120.617367",
                                "31.335374",
                                "departure",
                                0,
                                12_300,
                                1_800,
                                "接站"
                        ))
                )),
                "报名需提供游客名单。",
                "苏州园林二日标准产品。",
                "含首道门票和住宿。",
                "不含个人消费。",
                "儿童按不占床安排。",
                "无购物。",
                "自费项目自愿参加。",
                "无赠送。",
                "请携带身份证。",
                "以确认件为准。",
                List.of(new SalesProductArrangementItemRequest(
                        "hotel",
                        "住宿",
                        "苏州市区四钻酒店",
                        new BigDecimal("1"),
                        new BigDecimal("220.00"),
                        "间夜",
                        "credit",
                        "按确认占房执行",
                        "group_order_average",
                        "第1天",
                        "第2天",
                        null,
                        null,
                        2,
                        "苏州四钻酒店",
                        1001L,
                        "苏州酒店供应商",
                        null,
                        null,
                        null,
                        null,
                        "桌早",
                        "不含",
                        true,
                        "HT20260617001",
                        null,
                        null,
                        3001L,
                        "张房调",
                        "=不关联订单=",
                        new BigDecimal("220.00"),
                        new BigDecimal("20.00"),
                        new BigDecimal("200.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false,
                        List.of(new SalesProductArrangementPriceLineRequest(
                                501L,
                                "标间",
                                new BigDecimal("220.00"),
                                BigDecimal.ONE,
                                new BigDecimal("220.00"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                new BigDecimal("20.00"),
                                new BigDecimal("200.00"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                1,
                                "按确认占房执行"
                        )),
                        new SalesProductVehicleQuoteSnapshotRequest(
                                "第1天",
                                "第2天",
                                1,
                                2,
                                123_000,
                                7_200,
                                "苏州站 -> 拙政园 -> 酒店",
                                9L,
                                "39座大巴",
                                "江苏省",
                                "苏州市",
                                null,
                                new BigDecimal("1000.00"),
                                new BigDecimal("100.00"),
                                new BigDecimal("5.00"),
                                new BigDecimal("900.00"),
                                BigDecimal.ONE,
                                new BigDecimal("1450.00"),
                                new BigDecimal("1380.00"),
                                "按路书测算"
                        ),
                        List.of(new SalesProductVehicleInquiryRequest(
                                1,
                                "wechat_group",
                                "张车调",
                                null,
                                "苏州车队询价群",
                                2001L,
                                "苏州车队",
                                new BigDecimal("1380.00"),
                                true,
                                true,
                                false,
                                false,
                                2,
                                "王经理",
                                null,
                                null,
                                null,
                                true,
                                "可接"
                        ))
                )),
                "测试产品"
        );
    }

    private SalesProductEntity existingProduct(Long id) {
        SalesProductEntity entity = new SalesProductEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setProductName("苏州园林二日游");
        entity.setTravelDays(2);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }
}
