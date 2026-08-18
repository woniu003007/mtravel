package com.mtravel.platform.sales.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementOrderAllocationEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementPriceLineEntity;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementSectionStatusEntity;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementOrderAllocationMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementPriceLineMapper;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementSectionStatusMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementItemEntity;
import com.mtravel.platform.sales.product.entity.SalesProductArrangementPriceLineEntity;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.entity.SalesProductRoadbookPointEntity;
import com.mtravel.platform.sales.product.dto.SalesProductItineraryDayRequest;
import com.mtravel.platform.sales.product.dto.SalesProductRoadbookPointRequest;
import com.mtravel.platform.sales.product.mapper.SalesProductArrangementItemMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductArrangementPriceLineMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductDescriptionMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductRoadbookPointMapper;
import com.mtravel.platform.sales.team.dto.SalesTeamBatchCreateRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamBatchEditRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamDirectCreateRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamDirectEditResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamListResponse;
import com.mtravel.platform.sales.team.dto.SalesTeamPriceSaveRequest;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.entity.SalesTeamPriceEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamPriceMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamStatusLogMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 销售团期业务服务测试。
 *
 * <p>团期按老系统逻辑落为正式散拼团队，价格按客户分类拆到明细表。这里先固定团号生成、
 * 状态限制和价格明细行为，避免实现时把团期做成产品模板上的孤立字段。</p>
 */
class SalesTeamScheduleServiceTest {

    @Test
    void batchCreateShouldGenerateSanpinTeamsAndDefaultPriceRows() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamStatusLogMapper statusLogMapper = mock(SalesTeamStatusLogMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                teamMapper,
                priceMapper,
                statusLogMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existingTeam("CS-SP-BK-260701A")));
        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);
        ArgumentCaptor<SalesTeamPriceEntity> priceCaptor = ArgumentCaptor.forClass(SalesTeamPriceEntity.class);

        service.batchCreate(
                88L,
                new SalesTeamBatchCreateRequest(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 2),
                        List.of(3, 4),
                        6383L,
                        "老板账号",
                        77,
                        new BigDecimal("701"),
                        new BigDecimal("1701"),
                        new BigDecimal("1201"),
                        new BigDecimal("901"),
                        new BigDecimal("1601"),
                        new BigDecimal("301"),
                        null,
                        "默认",
                        null
                ),
                1L,
                "admin"
        );

        verify(teamMapper, org.mockito.Mockito.times(2)).insert(teamCaptor.capture());
        verify(teamMapper).lockTeamNoGeneration(1L, "CS-SP-BK-260701");
        verify(teamMapper).lockTeamNoGeneration(1L, "CS-SP-BK-260702");
        assertThat(teamCaptor.getAllValues()).extracting(SalesTeamEntity::getTeamNo)
                .containsExactly("CS-SP-BK-260701B", "CS-SP-BK-260702A");
        assertThat(teamCaptor.getAllValues()).allSatisfy(team -> {
            assertThat(team.getTeamType()).isEqualTo("sanpin");
            assertThat(team.getStatus()).isEqualTo("normal");
            assertThat(team.getTotalSeats()).isEqualTo(77);
            assertThat(team.getRemainingSeats()).isEqualTo(77);
            assertThat(team.getOperatorEmployeeName()).isEqualTo("老板账号");
        });
        verify(priceMapper, org.mockito.Mockito.times(2)).insert(priceCaptor.capture());
        assertThat(priceCaptor.getAllValues()).allSatisfy(price -> {
            assertThat(price.getCustomerCategoryName()).isEqualTo("默认");
            assertThat(price.getAdultPrice()).isEqualByComparingTo("1701");
            assertThat(price.getExtraFee()).isEqualByComparingTo("301");
        });
    }

    @Test
    void batchCreateShouldCopyProductTeamProfileToTeamSnapshot() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesProductEntity product = product();
        product.setBusinessType("疗休养");
        product.setRemark("""
                产品内部备注
                [[TEAM_PROFILE_JSON]]{"businessType":"亲子主题","departmentName":"计调一部","operatorName":"产品默认计调","escortName":"王全陪","perCapitaPitAmount":40,"optionalMarkupRate":70,"perCapitaShoppingAmount":500}
                """);
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product);
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);

        service.batchCreate(
                88L,
                new SalesTeamBatchCreateRequest(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 1),
                        List.of(),
                        null,
                        null,
                        30,
                        new BigDecimal("300"),
                        new BigDecimal("1680"),
                        new BigDecimal("1280"),
                        new BigDecimal("980"),
                        new BigDecimal("1580"),
                        new BigDecimal("100"),
                        null,
                        "默认",
                        null
                ),
                1L,
                "admin"
        );

        verify(teamMapper).insert(teamCaptor.capture());
        SalesTeamEntity created = teamCaptor.getValue();
        assertThat(created.getTeamType()).isEqualTo("sanpin");
        assertThat(created.getBusinessType()).isEqualTo("亲子主题");
        assertThat(created.getDepartmentName()).isEqualTo("计调一部");
        assertThat(created.getOperatorEmployeeName()).isEqualTo("产品默认计调");
        assertThat(created.getEscortEmployeeName()).isEqualTo("王全陪");
        assertThat(created.getRemark()).isEqualTo("产品内部备注");
        assertThat(created.getPerCapitaPitAmount()).isEqualByComparingTo("40");
        assertThat(created.getOptionalMarkupRate()).isEqualByComparingTo("70");
        assertThat(created.getPerCapitaShoppingAmount()).isEqualByComparingTo("500");
    }

    @Test
    void batchCreateShouldCopyProductArrangementTemplateAsReferenceWithoutPeopleOrAmount() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesProductArrangementItemMapper productArrangementMapper = mock(SalesProductArrangementItemMapper.class);
        SalesProductArrangementPriceLineMapper productPriceLineMapper = mock(SalesProductArrangementPriceLineMapper.class);
        DispatchTeamArrangementMapper teamArrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementPriceLineMapper teamPriceLineMapper = mock(DispatchTeamArrangementPriceLineMapper.class);
        DispatchTeamArrangementOrderAllocationMapper allocationMapper = mock(DispatchTeamArrangementOrderAllocationMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                null,
                null,
                null,
                productArrangementMapper,
                productPriceLineMapper,
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class),
                null,
                null,
                teamArrangementMapper,
                teamPriceLineMapper,
                allocationMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(productArrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(productTrafficArrangement()));
        when(productPriceLineMapper.selectList(any(Wrapper.class))).thenReturn(List.of(productTrafficPriceLine()));
        when(teamMapper.insert(any(SalesTeamEntity.class))).thenAnswer(invocation -> {
            SalesTeamEntity team = invocation.getArgument(0);
            team.setId(3001L);
            return 1;
        });
        when(teamArrangementMapper.insert(any(DispatchTeamArrangementEntity.class))).thenAnswer(invocation -> {
            DispatchTeamArrangementEntity arrangement = invocation.getArgument(0);
            arrangement.setId(9001L);
            return 1;
        });
        ArgumentCaptor<DispatchTeamArrangementEntity> arrangementCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementEntity.class);
        ArgumentCaptor<DispatchTeamArrangementPriceLineEntity> priceLineCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementPriceLineEntity.class);
        ArgumentCaptor<DispatchTeamArrangementOrderAllocationEntity> allocationCaptor =
                ArgumentCaptor.forClass(DispatchTeamArrangementOrderAllocationEntity.class);

        service.batchCreate(
                88L,
                new SalesTeamBatchCreateRequest(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 1),
                        List.of(3),
                        6383L,
                        "老板账号",
                        77,
                        new BigDecimal("701"),
                        new BigDecimal("1701"),
                        new BigDecimal("1201"),
                        new BigDecimal("901"),
                        new BigDecimal("1601"),
                        new BigDecimal("301"),
                        null,
                        "默认",
                        null
                ),
                1L,
                "admin"
        );

        verify(teamArrangementMapper).insert(arrangementCaptor.capture());
        DispatchTeamArrangementEntity arrangement = arrangementCaptor.getValue();
        assertThat(arrangement.getTeamId()).isEqualTo(3001L);
        assertThat(arrangement.getArrangementType()).isEqualTo("traffic");
        assertThat(arrangement.getItemName()).isEqualTo("飞机票");
        assertThat(arrangement.getTrafficType()).isEqualTo("飞机");
        assertThat(arrangement.getDeparturePlace()).isEqualTo("杭州");
        assertThat(arrangement.getArrivalPlace()).isEqualTo("北京");
        assertThat(arrangement.getSupplierName()).isEqualTo("国航供应商");
        assertThat(arrangement.getAllocationMode()).isEqualTo("group_order_average");
        assertThat(arrangement.getSettlementType()).isEqualTo("credit");
        assertThat(arrangement.getMealType()).isEqualTo("自助早");
        assertThat(arrangement.getFundIncluded()).isEqualTo("含");
        assertThat(arrangement.getConfirmed()).isFalse();
        assertThat(arrangement.getConfirmationNo()).isEqualTo("CN-PRODUCT-1");
        assertThat(arrangement.getGuideId()).isEqualTo(66L);
        assertThat(arrangement.getGuideName()).isEqualTo("产品导游");
        assertThat(arrangement.getTotalAmount()).isEqualByComparingTo("0.00");
        assertThat(arrangement.getCashAmount()).isEqualByComparingTo("0.00");
        assertThat(arrangement.getCreditAmount()).isEqualByComparingTo("0.00");
        assertThat(arrangement.getCostAmount()).isEqualByComparingTo("0.00");
        assertThat(arrangement.getPeopleCount()).isEqualByComparingTo("0.00");
        assertThat(arrangement.getNoGuideReport()).isFalse();

        verify(teamPriceLineMapper).insert(priceLineCaptor.capture());
        assertThat(priceLineCaptor.getValue().getArrangementId()).isEqualTo(9001L);
        assertThat(priceLineCaptor.getValue().getProjectName()).isEqualTo("成人机票");
        assertThat(priceLineCaptor.getValue().getUnitPrice()).isEqualByComparingTo("120.00");
        assertThat(priceLineCaptor.getValue().getQuantity()).isEqualByComparingTo("0.00");
        assertThat(priceLineCaptor.getValue().getAmount()).isEqualByComparingTo("0.00");
        assertThat(priceLineCaptor.getValue().getCashAmount()).isEqualByComparingTo("0.00");
        assertThat(priceLineCaptor.getValue().getCreditAmount()).isEqualByComparingTo("0.00");

        verify(allocationMapper).insert(allocationCaptor.capture());
        assertThat(allocationCaptor.getValue().getAllocationScope()).isEqualTo("team");
        assertThat(allocationCaptor.getValue().getOrderId()).isNull();
        assertThat(allocationCaptor.getValue().getAllocationAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void batchCreateShouldGenerateSpecificNonContinuousDates() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamStatusLogMapper statusLogMapper = mock(SalesTeamStatusLogMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                teamMapper,
                priceMapper,
                statusLogMapper
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);

        service.batchCreate(
                88L,
                new SalesTeamBatchCreateRequest(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 1),
                        List.of(),
                        6383L,
                        "老板账号",
                        30,
                        new BigDecimal("300"),
                        new BigDecimal("1680"),
                        new BigDecimal("1280"),
                        new BigDecimal("980"),
                        new BigDecimal("1580"),
                        new BigDecimal("100"),
                        null,
                        "默认",
                        List.of(
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 3),
                                LocalDate.of(2026, 7, 5)
                        )
                ),
                1L,
                "admin"
        );

        verify(teamMapper, org.mockito.Mockito.times(3)).insert(teamCaptor.capture());
        assertThat(teamCaptor.getAllValues()).extracting(SalesTeamEntity::getDepartureDate)
                .containsExactly(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 3),
                        LocalDate.of(2026, 7, 5)
                );
        assertThat(teamCaptor.getAllValues()).extracting(SalesTeamEntity::getTeamNo)
                .containsExactly("CS-SP-BK-260701A", "CS-SP-BK-260703A", "CS-SP-BK-260705A");
    }

    @Test
    void directCreateShouldCreateProductAndTeamWithoutDefaultPriceForZhengtuan() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamStatusLogMapper statusLogMapper = mock(SalesTeamStatusLogMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                teamMapper,
                priceMapper,
                statusLogMapper
        );
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        ArgumentCaptor<SalesProductEntity> productCaptor = ArgumentCaptor.forClass(SalesProductEntity.class);
        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);

        var response = service.directCreate(
                new SalesTeamDirectCreateRequest(
                        "zhengtuan",
                        "HD3流浪地球计划",
                        "红色培训",
                        "domestic",
                        "重庆市",
                        "重庆市",
                        null,
                        LocalDate.of(2026, 6, 23),
                        "daily",
                        "准四星",
                        "亲子游",
                        3,
                        2,
                        new BigDecimal("199"),
                        500,
                        List.of(),
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
                        "操作备注"
                ),
                1L,
                "admin"
        );

        verify(productMapper).insert(productCaptor.capture());
        SalesProductEntity product = productCaptor.getValue();
        assertThat(product.getProductName()).isEqualTo("HD3流浪地球计划");
        assertThat(product.getProductScope()).isEqualTo("team_snapshot");
        assertThat(product.getBusinessType()).isEqualTo("红色培训");
        assertThat(product.getDomesticInternational()).isEqualTo("domestic");
        assertThat(product.getProvince()).isEqualTo("重庆市");
        assertThat(product.getTravelDays()).isEqualTo(3);
        assertThat(product.getPlannedCapacity()).isEqualTo(500);
        assertThat(product.getStatus()).isEqualTo("active");

        verify(teamMapper).insert(teamCaptor.capture());
        SalesTeamEntity team = teamCaptor.getValue();
        assertThat(team.getTeamNo()).isEqualTo("CS-BK-260623A");
        assertThat(team.getTeamType()).isEqualTo("zhengtuan");
        assertThat(team.getBusinessType()).isEqualTo("红色培训");
        assertThat(team.getDepartureDate()).isEqualTo(LocalDate.of(2026, 6, 23));
        assertThat(team.getTotalSeats()).isEqualTo(500);
        assertThat(team.getRemainingSeats()).isEqualTo(500);
        assertThat(team.getSingleRoomDifference()).isEqualByComparingTo("199");
        assertThat(team.getRemark()).isEqualTo("操作备注");

        verify(priceMapper, never()).insert(any(SalesTeamPriceEntity.class));
        assertThat(response.prices()).isEmpty();
        assertThat(response.teamType()).isEqualTo("zhengtuan");
    }

    @Test
    void directCreateShouldPersistProductDescriptionAndItineraryTabs() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductItineraryDayMapper itineraryDayMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductRoadbookPointMapper roadbookPointMapper = mock(SalesProductRoadbookPointMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                descriptionMapper,
                itineraryDayMapper,
                roadbookPointMapper,
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        when(productMapper.insert(any(SalesProductEntity.class))).thenAnswer(invocation -> {
            SalesProductEntity product = invocation.getArgument(0);
            product.setId(9001L);
            return 1;
        });
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        ArgumentCaptor<SalesProductDescriptionEntity> descriptionCaptor = ArgumentCaptor.forClass(SalesProductDescriptionEntity.class);
        ArgumentCaptor<SalesProductItineraryDayEntity> itineraryCaptor = ArgumentCaptor.forClass(SalesProductItineraryDayEntity.class);
        ArgumentCaptor<SalesProductRoadbookPointEntity> roadbookPointCaptor = ArgumentCaptor.forClass(SalesProductRoadbookPointEntity.class);

        service.directCreate(
                new SalesTeamDirectCreateRequest(
                        "sanpin",
                        "杭州西湖二日游",
                        "疗休养",
                        "domestic",
                        "浙江省",
                        "杭州市",
                        "西湖区",
                        LocalDate.of(2026, 7, 1),
                        "irregular",
                        "准四星",
                        "山水",
                        2,
                        1,
                        BigDecimal.ZERO,
                        40,
                        List.of(new SalesProductItineraryDayRequest(
                                1,
                                "杭州集合",
                                "西湖游览",
                                "住杭州",
                                "西湖酒店",
                                BigDecimal.ZERO,
                                true,
                                true,
                                false,
                                null,
                                "酒店 -> 西湖",
                                12_000,
                                1_800,
                                List.of(new SalesProductRoadbookPointRequest(
                                        1,
                                        "西湖酒店",
                                        "杭州市西湖区",
                                        "120.145",
                                        "30.245",
                                        "departure",
                                        10,
                                        12_000,
                                        1_800,
                                        "集合点"
                                ))
                        )),
                        "学生团队提前核对名单",
                        "产品说明正文",
                        "含车含导游",
                        "不含单房差",
                        "儿童不占床",
                        "无购物",
                        "无自费",
                        "赠送矿泉水",
                        "注意证件",
                        "带好雨具",
                        "团队备注"
                ),
                1L,
                "admin"
        );

        verify(descriptionMapper).insert(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().getProductId()).isEqualTo(9001L);
        assertThat(descriptionCaptor.getValue().getBookingNotice()).isEqualTo("学生团队提前核对名单");
        assertThat(descriptionCaptor.getValue().getProductDescription()).isEqualTo("产品说明正文");
        assertThat(descriptionCaptor.getValue().getFeeIncluded()).isEqualTo("含车含导游");
        assertThat(descriptionCaptor.getValue().getWarmReminder()).isEqualTo("带好雨具");

        verify(itineraryDayMapper).insert(itineraryCaptor.capture());
        assertThat(itineraryCaptor.getValue().getProductId()).isEqualTo(9001L);
        assertThat(itineraryCaptor.getValue().getDayNo()).isEqualTo(1);
        assertThat(itineraryCaptor.getValue().getDayTitle()).isEqualTo("杭州集合");
        assertThat(itineraryCaptor.getValue().getItineraryContent()).isEqualTo("西湖游览");
        assertThat(itineraryCaptor.getValue().getBreakfastIncluded()).isTrue();
        assertThat(itineraryCaptor.getValue().getRoadbookSummary()).isEqualTo("酒店 -> 西湖");

        verify(roadbookPointMapper).insert(roadbookPointCaptor.capture());
        assertThat(roadbookPointCaptor.getValue().getProductId()).isEqualTo(9001L);
        assertThat(roadbookPointCaptor.getValue().getDayNo()).isEqualTo(1);
        assertThat(roadbookPointCaptor.getValue().getPointOrder()).isEqualTo(1);
        assertThat(roadbookPointCaptor.getValue().getPlaceName()).isEqualTo("西湖酒店");
        assertThat(roadbookPointCaptor.getValue().getPointType()).isEqualTo("departure");
        assertThat(roadbookPointCaptor.getValue().getDistanceToNextMeters()).isEqualTo(12000);
    }

    @Test
    void directCreateShouldAvoidTeamNoCollisionAcrossProductSnapshots() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity existing = existingTeam("CS-BK-260623A");
        existing.setDepartureDate(LocalDate.of(2026, 6, 23));
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));
        ArgumentCaptor<SalesTeamEntity> teamCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);

        service.directCreate(
                new SalesTeamDirectCreateRequest(
                        "zhengtuan",
                        "重庆三日整团",
                        "定制团",
                        "domestic",
                        "重庆市",
                        "重庆市",
                        null,
                        LocalDate.of(2026, 6, 23),
                        "daily",
                        null,
                        null,
                        3,
                        0,
                        BigDecimal.ZERO,
                        30,
                        List.of(),
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
                        null
                ),
                1L,
                "admin"
        );

        verify(teamMapper).insert(teamCaptor.capture());
        assertThat(teamCaptor.getValue().getTeamNo()).isEqualTo("CS-BK-260623B");
    }

    @Test
    void directCreateShouldUseTeamNoInProductSnapshotName() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity existing = existingTeam("CS-BK-260623A");
        existing.setDepartureDate(LocalDate.of(2026, 6, 23));
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));
        ArgumentCaptor<SalesProductEntity> productCaptor = ArgumentCaptor.forClass(SalesProductEntity.class);

        service.directCreate(
                new SalesTeamDirectCreateRequest(
                        "zhengtuan",
                        "重庆三日整团",
                        "定制团",
                        "domestic",
                        "重庆市",
                        "重庆市",
                        null,
                        LocalDate.of(2026, 6, 23),
                        "daily",
                        null,
                        null,
                        3,
                        0,
                        BigDecimal.ZERO,
                        30,
                        List.of(),
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
                        null
                ),
                1L,
                "admin"
        );

        verify(productMapper).insert(productCaptor.capture());
        assertThat(productCaptor.getValue().getProductName()).isEqualTo("重庆三日整团");
        assertThat(productCaptor.getValue().getProductScope()).isEqualTo("team_snapshot");
    }

    @Test
    void directEditDetailShouldLoadTeamProductDescriptionAndRoadbook() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductItineraryDayMapper itineraryDayMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductRoadbookPointMapper roadbookPointMapper = mock(SalesProductRoadbookPointMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                descriptionMapper,
                itineraryDayMapper,
                roadbookPointMapper,
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-BK-260628A");
        team.setId(31L);
        team.setProductId(15L);
        team.setTeamType("santuan");
        team.setBusinessType("地接团");
        team.setDepartureDate(LocalDate.of(2026, 6, 28));
        team.setTotalSeats(20);
        team.setSingleRoomDifference(new BigDecimal("180"));
        team.setCloseDaysBefore(2);
        team.setRemark("团队备注");
        SalesProductEntity product = product();
        product.setId(15L);
        product.setProductName("西湖-CS-BK-260628A");
        product.setTravelDays(2);
        product.setPlannedCapacity(20);
        product.setTripType("irregular");
        product.setRemark("产品备注不直接显示成团队名称");
        SalesProductDescriptionEntity description = new SalesProductDescriptionEntity();
        description.setProductDescription("产品说明正文");
        description.setBookingNotice("收客须知");
        description.setFeeIncluded("费用包含");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product);
        when(descriptionMapper.selectOne(any(Wrapper.class))).thenReturn(description);
        when(itineraryDayMapper.selectList(any(Wrapper.class))).thenReturn(List.of(itineraryDay(1, "第1天", "游西湖", 12_000, 1_800)));
        when(roadbookPointMapper.selectList(any(Wrapper.class))).thenReturn(List.of(roadbookPoint(1, "西湖酒店")));

        SalesTeamDirectEditResponse result = service.directEditDetail(31L, 1L);

        assertThat(result.id()).isEqualTo(31L);
        assertThat(result.productId()).isEqualTo(15L);
        assertThat(result.teamNo()).isEqualTo("CS-BK-260628A");
        assertThat(result.teamName()).isEqualTo("西湖");
        assertThat(result.teamType()).isEqualTo("santuan");
        assertThat(result.businessType()).isEqualTo("地接团");
        assertThat(result.departureDate()).isEqualTo(LocalDate.of(2026, 6, 28));
        assertThat(result.travelDays()).isEqualTo(2);
        assertThat(result.bookingNotice()).isEqualTo("收客须知");
        assertThat(result.feeIncluded()).isEqualTo("费用包含");
        assertThat(result.itineraryDays()).hasSize(1);
        assertThat(result.itineraryDays().getFirst().roadbookPoints()).hasSize(1);
        assertThat(result.itineraryDays().getFirst().roadbookPoints().getFirst().placeName()).isEqualTo("西湖酒店");
    }

    @Test
    void directUpdateShouldUpdateExistingTeamAndProductSnapshotWithoutCreatingNewTeam() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductItineraryDayMapper itineraryDayMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductRoadbookPointMapper roadbookPointMapper = mock(SalesProductRoadbookPointMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                descriptionMapper,
                itineraryDayMapper,
                roadbookPointMapper,
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity current = existingTeam("CS-BK-260628A");
        current.setId(31L);
        current.setProductId(15L);
        current.setUsedSeats(3);
        current.setTotalSeats(20);
        SalesProductEntity product = product();
        product.setId(15L);
        product.setProductName("西湖-CS-BK-260628A");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product);
        when(productMapper.update(any(SalesProductEntity.class), any(Wrapper.class))).thenReturn(1);
        when(teamMapper.update(any(SalesTeamEntity.class), any(Wrapper.class))).thenReturn(1);
        ArgumentCaptor<SalesProductEntity> productUpdateCaptor = ArgumentCaptor.forClass(SalesProductEntity.class);
        ArgumentCaptor<SalesTeamEntity> teamUpdateCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);
        ArgumentCaptor<SalesProductDescriptionEntity> descriptionInsertCaptor = ArgumentCaptor.forClass(SalesProductDescriptionEntity.class);
        ArgumentCaptor<SalesProductItineraryDayEntity> itineraryInsertCaptor = ArgumentCaptor.forClass(SalesProductItineraryDayEntity.class);
        ArgumentCaptor<SalesProductRoadbookPointEntity> roadbookInsertCaptor = ArgumentCaptor.forClass(SalesProductRoadbookPointEntity.class);

        service.directUpdate(
                31L,
                new SalesTeamDirectCreateRequest(
                        "zhengtuan",
                        "西湖深度游",
                        "定制团",
                        "domestic",
                        "浙江省",
                        "杭州市",
                        "西湖区",
                        LocalDate.of(2026, 7, 2),
                        "daily",
                        "准四星",
                        "亲子游",
                        2,
                        1,
                        new BigDecimal("220"),
                        18,
                        List.of(new SalesProductItineraryDayRequest(
                                1,
                                "杭州集合",
                                "西湖游览",
                                "住杭州",
                                "西湖酒店",
                                BigDecimal.ZERO,
                                true,
                                true,
                                false,
                                null,
                                "西湖酒店 -> 西湖",
                                12_000,
                                1_800,
                                List.of(new SalesProductRoadbookPointRequest(
                                        1,
                                        "西湖酒店",
                                        "杭州市西湖区",
                                        "120.145",
                                        "30.245",
                                        "departure",
                                        10,
                                        12_000,
                                        1_800,
                                        "集合点"
                                ))
                        )),
                        "收客须知更新",
                        "产品说明更新",
                        "含车含导游",
                        "不含单房差",
                        "儿童不占床",
                        "无购物",
                        "无自费",
                        "赠送矿泉水",
                        "注意证件",
                        "带好雨具",
                        "团队备注更新"
                ),
                1L,
                "admin"
        );

        verify(productMapper, never()).insert(any(SalesProductEntity.class));
        verify(teamMapper, never()).insert(any(SalesTeamEntity.class));
        verify(productMapper).update(productUpdateCaptor.capture(), any(Wrapper.class));
        assertThat(productUpdateCaptor.getValue().getProductName()).isEqualTo("西湖深度游");
        assertThat(productUpdateCaptor.getValue().getTravelDays()).isEqualTo(2);
        verify(teamMapper).update(teamUpdateCaptor.capture(), any(Wrapper.class));
        assertThat(teamUpdateCaptor.getValue().getTeamNo()).isNull();
        assertThat(teamUpdateCaptor.getValue().getTeamType()).isEqualTo("zhengtuan");
        assertThat(teamUpdateCaptor.getValue().getDepartureDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(teamUpdateCaptor.getValue().getTotalSeats()).isEqualTo(18);
        assertThat(teamUpdateCaptor.getValue().getRemainingSeats()).isEqualTo(15);
        assertThat(teamUpdateCaptor.getValue().getRemark()).isEqualTo("团队备注更新");
        verify(descriptionMapper).update(any(SalesProductDescriptionEntity.class), any(Wrapper.class));
        verify(itineraryDayMapper).update(any(SalesProductItineraryDayEntity.class), any(Wrapper.class));
        verify(roadbookPointMapper).update(any(SalesProductRoadbookPointEntity.class), any(Wrapper.class));
        verify(descriptionMapper).insert(descriptionInsertCaptor.capture());
        assertThat(descriptionInsertCaptor.getValue().getBookingNotice()).isEqualTo("收客须知更新");
        verify(itineraryDayMapper).insert(itineraryInsertCaptor.capture());
        assertThat(itineraryInsertCaptor.getValue().getProductId()).isEqualTo(15L);
        verify(roadbookPointMapper).insert(roadbookInsertCaptor.capture());
        assertThat(roadbookInsertCaptor.getValue().getPlaceName()).isEqualTo("西湖酒店");
    }

    @Test
    void deleteTeamShouldRejectNormalStatus() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-SP-BK-260701A");
        team.setId(1001L);
        team.setStatus("normal");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);

        assertThatThrownBy(() -> service.deleteTeam(1001L, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessage("只有取消状态的团队可以删除");

        verify(teamMapper, never()).update(any(SalesTeamEntity.class), any(Wrapper.class));
        verify(priceMapper, never()).update(any(SalesTeamPriceEntity.class), any(Wrapper.class));
    }

    @Test
    void savePriceShouldCreateSecondCustomerCategoryPriceRow() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                mock(SalesProductMapper.class),
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-SP-BK-260701A");
        team.setId(1001L);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(priceMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        ArgumentCaptor<SalesTeamPriceEntity> priceCaptor = ArgumentCaptor.forClass(SalesTeamPriceEntity.class);

        service.savePrice(
                1001L,
                new SalesTeamPriceSaveRequest(
                        1L,
                        "A类客户",
                        new BigDecimal("3911"),
                        new BigDecimal("3411"),
                        new BigDecimal("3111"),
                        new BigDecimal("3811"),
                        new BigDecimal("511")
                ),
                1L,
                "admin"
        );

        verify(priceMapper).insert(priceCaptor.capture());
        assertThat(priceCaptor.getValue().getTeamId()).isEqualTo(1001L);
        assertThat(priceCaptor.getValue().getCustomerCategoryId()).isEqualTo(1L);
        assertThat(priceCaptor.getValue().getCustomerCategoryName()).isEqualTo("A类客户");
        assertThat(priceCaptor.getValue().getSeniorPrice()).isEqualByComparingTo("3811");
    }

    @Test
    void batchEditShouldCreateSelectedCustomerCategoryPricesForSelectedTeams() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                mock(SalesProductMapper.class),
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-SP-BK-260701A");
        team.setId(1001L);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(priceMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        ArgumentCaptor<SalesTeamPriceEntity> priceCaptor = ArgumentCaptor.forClass(SalesTeamPriceEntity.class);

        service.batchEdit(
                new SalesTeamBatchEditRequest(
                        List.of(1001L),
                        List.of(
                                new SalesTeamBatchEditRequest.CustomerCategoryItem(1L, "A类客户"),
                                new SalesTeamBatchEditRequest.CustomerCategoryItem(3L, "B类客户")
                        ),
                        false,
                        false,
                        false,
                        false,
                        false,
                        null,
                        null,
                        new BigDecimal("2000"),
                        new BigDecimal("1500"),
                        new BigDecimal("1200"),
                        new BigDecimal("1800"),
                        new BigDecimal("100")
                ),
                1L,
                "admin"
        );

        verify(priceMapper, times(2)).insert(priceCaptor.capture());
        assertThat(priceCaptor.getAllValues()).extracting(SalesTeamPriceEntity::getCustomerCategoryName)
                .containsExactly("A类客户", "B类客户");
        assertThat(priceCaptor.getAllValues()).allSatisfy(price -> {
            assertThat(price.getTeamId()).isEqualTo(1001L);
            assertThat(price.getAdultPrice()).isEqualByComparingTo("2000");
            assertThat(price.getExtraFee()).isEqualByComparingTo("100");
        });
    }

    @Test
    void batchEditWithoutSelectedCustomerCategoryShouldUpdateAllExistingPrices() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                mock(SalesProductMapper.class),
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-SP-BK-260701A");
        team.setId(1001L);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(priceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                existingPrice(11L, 1001L, 1L, "A类客户"),
                existingPrice(12L, 1001L, 3L, "B类客户")
        ));
        ArgumentCaptor<SalesTeamPriceEntity> priceCaptor = ArgumentCaptor.forClass(SalesTeamPriceEntity.class);

        service.batchEdit(
                new SalesTeamBatchEditRequest(
                        List.of(1001L),
                        List.of(),
                        false,
                        false,
                        false,
                        false,
                        false,
                        null,
                        null,
                        new BigDecimal("2200"),
                        new BigDecimal("1600"),
                        new BigDecimal("1300"),
                        new BigDecimal("1900"),
                        new BigDecimal("120")
                ),
                1L,
                "admin"
        );

        verify(priceMapper, times(2)).update(priceCaptor.capture(), any(Wrapper.class));
        assertThat(priceCaptor.getAllValues()).allSatisfy(price -> {
            assertThat(price.getAdultPrice()).isEqualByComparingTo("2200");
            assertThat(price.getSeniorPrice()).isEqualByComparingTo("1900");
            assertThat(price.getExtraFee()).isEqualByComparingTo("120");
        });
    }

    @Test
    void batchEditDeletePriceShouldDeleteSelectedCustomerCategoryOnly() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                mock(SalesProductMapper.class),
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-SP-BK-260701A");
        team.setId(1001L);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        ArgumentCaptor<SalesTeamPriceEntity> updateCaptor = ArgumentCaptor.forClass(SalesTeamPriceEntity.class);
        ArgumentCaptor<Wrapper<SalesTeamPriceEntity>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);

        service.batchEdit(
                new SalesTeamBatchEditRequest(
                        List.of(1001L),
                        List.of(new SalesTeamBatchEditRequest.CustomerCategoryItem(1L, "A类客户")),
                        false,
                        false,
                        true,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                1L,
                "admin"
        );

        verify(priceMapper).update(updateCaptor.capture(), wrapperCaptor.capture());
        assertThat(updateCaptor.getValue().getIsDeleted()).isTrue();
        assertThat(updateCaptor.getValue().getDeletedBy()).isEqualTo("admin");
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("team_id", "customer_category_id");
    }

    @Test
    void batchEditDeletePriceWithoutCustomerCategoryShouldDeleteAllTeamPrices() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                mock(SalesProductMapper.class),
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-SP-BK-260701A");
        team.setId(1001L);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        ArgumentCaptor<SalesTeamPriceEntity> updateCaptor = ArgumentCaptor.forClass(SalesTeamPriceEntity.class);
        ArgumentCaptor<Wrapper<SalesTeamPriceEntity>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);

        service.batchEdit(
                new SalesTeamBatchEditRequest(
                        List.of(1001L),
                        List.of(),
                        false,
                        false,
                        true,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                1L,
                "admin"
        );

        verify(priceMapper).update(updateCaptor.capture(), wrapperCaptor.capture());
        assertThat(updateCaptor.getValue().getIsDeleted()).isTrue();
        assertThat(updateCaptor.getValue().getDeletedBy()).isEqualTo("admin");
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("team_id");
        assertThat(sqlSegment).doesNotContain("customer_category_id");
    }

    @Test
    void globalTeamPageShouldReturnTeamsWithProductInfoWithoutNPlusOne() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        DispatchTeamArrangementMapper teamArrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamArrangementSectionStatusMapper sectionStatusMapper = mock(DispatchTeamArrangementSectionStatusMapper.class);
        DispatchTeamGuideMapper guideMapper = mock(DispatchTeamGuideMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                null,
                null,
                null,
                null,
                null,
                teamMapper,
                null,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class),
                null,
                null,
                orderMapper,
                teamArrangementMapper,
                null,
                null,
                sectionStatusMapper,
                guideMapper
        );
        SalesTeamEntity team = existingTeam("CS-SP-BK-260701A");
        team.setId(1001L);
        team.setTotalSeats(30);
        team.setUsedSeats(4);
        team.setRemainingSeats(26);
        when(teamMapper.selectPage(any(), any(Wrapper.class))).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SalesTeamEntity>(1, 20, 1).setRecords(List.of(team)));
        SalesProductEntity product = product();
        product.setProductName("测试产品-CS-SP-BK-260701A");
        when(productMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product));
        when(teamArrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamArrangement(1001L, "traffic", true, "active"),
                teamArrangement(1001L, "hotel", false, "active"),
                teamArrangement(1001L, "vehicle", true, "cancelled")
        ));
        when(sectionStatusMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamSectionStatus(1001L, "traffic", "done"),
                teamSectionStatus(1001L, "hotel", "pending"),
                teamSectionStatus(1001L, "scenic", "none")
        ));
        when(guideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamGuide(1001L, "测试导游", "13800138000", false, "active"),
                teamGuide(1001L, "备用导游", "13900139000", true, "active")
        ));
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamOrder(1001L, "测试地接社", "confirmed", "normal"),
                teamOrder(1001L, "游天下", "pending", "merge_child"),
                teamOrder(1001L, "测试地接社", "confirmed", "normal"),
                teamOrder(1001L, "已取消客户", "cancelled", "normal"),
                teamOrder(1001L, "拼团来源客户", "confirmed", "merge_source")
        ));

        var result = service.globalPage(
                1L,
                "sanpin",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                20
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        SalesTeamListResponse item = result.items().getFirst();
        assertThat(item.teamNo()).isEqualTo("CS-SP-BK-260701A");
        assertThat(item.teamType()).isEqualTo("sanpin");
        assertThat(item.productName()).isEqualTo("测试产品");
        assertThat(item.businessType()).isEqualTo("疗休养");
        assertThat(item.travelDays()).isEqualTo(3);
        assertThat(item.departurePlace()).isEqualTo("浙江省杭州市西湖区");
        assertThat(item.totalSeats()).isEqualTo(30);
        assertThat(item.usedSeats()).isEqualTo(4);
        assertThat(item.customerSummary()).isEqualTo("测试地接社、游天下、拼团来源客户");
        assertThat(item.guideSummary()).isEqualTo("测试导游[Tel:13800138000]、备用导游[Tel:13900139000]");
        assertThat(item.guidePlan()).isEqualTo("confirmed");
        assertThat(item.trafficPlan()).isEqualTo("confirmed");
        assertThat(item.hotelPlan()).isEqualTo("pending");
        assertThat(item.vehiclePlan()).isEqualTo("none");
        assertThat(item.scenicPlan()).isEqualTo("none");
        verify(productMapper, times(1)).selectList(any(Wrapper.class));
        verify(orderMapper, times(1)).selectList(any(Wrapper.class));
        verify(priceMapper, never()).selectList(any(Wrapper.class));
        verify(teamArrangementMapper, times(1)).selectList(any(Wrapper.class));
        verify(sectionStatusMapper, times(1)).selectList(any(Wrapper.class));
        verify(guideMapper, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    void globalTeamPageShouldApplyAdvancedSearchFilters() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                mock(SalesProductMapper.class),
                null,
                null,
                null,
                null,
                null,
                teamMapper,
                null,
                mock(SalesTeamPriceMapper.class),
                mock(SalesTeamStatusLogMapper.class),
                null,
                null,
                mock(SalesBookingOrderMapper.class),
                mock(DispatchTeamArrangementMapper.class),
                null,
                null,
                mock(DispatchTeamArrangementSectionStatusMapper.class),
                mock(DispatchTeamGuideMapper.class)
        );
        ArgumentCaptor<Wrapper<SalesTeamEntity>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        when(teamMapper.selectPage(any(), any(Wrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SalesTeamEntity>(1, 20, 0));

        service.globalPage(
                1L,
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
                "王导",
                "计调一部",
                "confirmed",
                LocalDate.of(2026, 7, 8),
                1,
                20
        );

        verify(teamMapper).selectPage(any(), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("dispatch_team_guides");
        assertThat(sqlSegment).contains("guide_name");
        assertThat(sqlSegment).contains("guide_mobile");
        assertThat(sqlSegment).contains("department_name");
        assertThat(sqlSegment).contains("sales_orders");
        assertThat(sqlSegment).contains("status =");
        assertThat(sqlSegment).contains("created_at");
    }

    @Test
    void operationDetailShouldAggregateTeamProductDescriptionsAndPrices() {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductItineraryDayMapper itineraryMapper = mock(SalesProductItineraryDayMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                descriptionMapper,
                itineraryMapper,
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-SP-CS-260706A");
        team.setId(260561L);
        team.setTeamType("zhengtuan");
        team.setBusinessType("亲子主题");
        team.setDepartmentId(19L);
        team.setDepartmentName("计调一部");
        team.setOperatorEmployeeId(6383L);
        team.setOperatorEmployeeName("老板号");
        team.setEscortEmployeeId(998L);
        team.setEscortEmployeeName("王全陪");
        team.setTotalSeats(31);
        team.setUsedSeats(11);
        team.setRemainingSeats(20);
        team.setRemark("导游确认酒店和用车要求");
        team.setPerCapitaPitAmount(new BigDecimal("10"));
        team.setOptionalMarkupRate(new BigDecimal("70"));
        team.setPerCapitaShoppingAmount(new BigDecimal("600"));
        SalesProductDescriptionEntity description = new SalesProductDescriptionEntity();
        description.setProductId(88L);
        description.setProductDescription("产品说明正文");
        description.setBookingNotice("收客须知正文");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        SalesProductEntity product = product();
        product.setProductName("测试产品-CS-SP-CS-260706A");
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product);
        when(descriptionMapper.selectOne(any(Wrapper.class))).thenReturn(description);
        when(itineraryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                itineraryDay(1, "DAY-1：杭州接站", "抵达杭州，入住酒店。", 12_300, 1_800),
                itineraryDay(2, "DAY-2：西湖游览", "游览西湖和灵隐寺。", 45_700, 5_400),
                itineraryDay(3, "DAY-3：乌镇返程", "乌镇游览后返程。", null, null)
        ));
        when(priceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                existingPrice(11L, 260561L, null, "默认"),
                existingPrice(12L, 260561L, 3L, "B类客户")
        ));

        var result = service.operationDetail(260561L, 1L);

        assertThat(result.team().id()).isEqualTo(260561L);
        assertThat(result.team().teamNo()).isEqualTo("CS-SP-CS-260706A");
        assertThat(result.team().teamType()).isEqualTo("zhengtuan");
        assertThat(result.team().teamTypeLabel()).isEqualTo("整团");
        assertThat(result.team().businessType()).isEqualTo("亲子主题");
        assertThat(result.team().departmentId()).isEqualTo(19L);
        assertThat(result.team().departmentName()).isEqualTo("计调一部");
        assertThat(result.team().escortEmployeeId()).isEqualTo(998L);
        assertThat(result.team().escortEmployeeName()).isEqualTo("王全陪");
        assertThat(result.team().statusLabel()).isEqualTo("正常");
        assertThat(result.team().travelDays()).isEqualTo(3);
        assertThat(result.team().endDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(result.team().totalSeats()).isEqualTo(31);
        assertThat(result.team().usedSeats()).isEqualTo(11);
        assertThat(result.team().remainingSeats()).isEqualTo(20);
        assertThat(result.product().productName()).isEqualTo("测试产品");
        assertThat(result.product().departurePlace()).isEqualTo("浙江省杭州市西湖区");
        assertThat(result.content().productDescription()).isEqualTo("产品说明正文");
        assertThat(result.content().bookingNotice()).isEqualTo("收客须知正文");
        assertThat(result.content().internalRemark()).isEqualTo("导游确认酒店和用车要求");
        assertThat(result.content().perCapitaPitAmount()).isEqualByComparingTo("10");
        assertThat(result.content().optionalMarkupRate()).isEqualByComparingTo("70");
        assertThat(result.content().perCapitaShoppingAmount()).isEqualByComparingTo("600");
        assertThat(result.prices()).extracting("customerCategoryName").containsExactly("默认", "B类客户");
        assertThat(result.itineraryDays()).extracting("dayNo").containsExactly(1, 2, 3);
        assertThat(result.itineraryDays().getFirst().dayTitle()).isEqualTo("DAY-1：杭州接站");
        assertThat(result.routeSummary().totalDistanceMeters()).isEqualTo(58_000);
        assertThat(result.routeSummary().totalDurationSeconds()).isEqualTo(7_200);
        assertThat(result.orders()).isEmpty();
        assertThat(result.actions()).extracting("code").contains("orderFile", "mergeOrder", "moveOrder", "guideBill");
    }

    @Test
    void saveTeamShouldPersistOperationDictionaryFields() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamPriceMapper priceMapper = mock(SalesTeamPriceMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                mock(SalesProductMapper.class),
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity current = existingTeam("CS-SP-BK-260701A");
        current.setId(1001L);
        current.setUsedSeats(2);
        current.setTotalSeats(30);
        current.setRemainingSeats(28);
        SalesTeamEntity latest = existingTeam("CS-SP-BK-260701A");
        latest.setId(1001L);
        latest.setTeamType("zhengtuan");
        latest.setBusinessType("亲子主题");
        latest.setDepartmentId(19L);
        latest.setDepartmentName("计调一部");
        latest.setOperatorEmployeeId(6383L);
        latest.setOperatorEmployeeName("老板号");
        latest.setEscortEmployeeId(998L);
        latest.setEscortEmployeeName("王全陪");
        latest.setTotalSeats(30);
        latest.setUsedSeats(2);
        latest.setRemainingSeats(28);
        latest.setRemark("顶部信息已确认");
        latest.setPerCapitaPitAmount(new BigDecimal("10"));
        latest.setOptionalMarkupRate(new BigDecimal("70"));
        latest.setPerCapitaShoppingAmount(new BigDecimal("600"));
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(current, latest);
        when(teamMapper.update(any(SalesTeamEntity.class), any(Wrapper.class))).thenReturn(1);
        ArgumentCaptor<SalesTeamEntity> updateCaptor = ArgumentCaptor.forClass(SalesTeamEntity.class);
        ArgumentCaptor<Wrapper<SalesTeamEntity>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);

        var response = service.saveTeam(
                1001L,
                new com.mtravel.platform.sales.team.dto.SalesTeamSaveRequest(
                        null,
                        "zhengtuan",
                        "亲子主题",
                        19L,
                        "计调一部",
                        6383L,
                        "老板号",
                        998L,
                        "王全陪",
                        null,
                        null,
                        "顶部信息已确认",
                        new BigDecimal("10"),
                        new BigDecimal("70"),
                        new BigDecimal("600")
                ),
                1L,
                "admin"
        );

        verify(teamMapper).update(updateCaptor.capture(), wrapperCaptor.capture());
        assertThat(updateCaptor.getValue().getTeamType()).isEqualTo("zhengtuan");
        String sqlSet = ((UpdateWrapper<SalesTeamEntity>) wrapperCaptor.getValue()).getSqlSet();
        assertThat(sqlSet).contains(
                "business_type",
                "department_id",
                "department_name",
                "operator_employee_id",
                "operator_employee_name",
                "escort_employee_id",
                "escort_employee_name",
                "remark",
                "per_capita_pit_amount",
                "optional_markup_rate",
                "per_capita_shopping_amount"
        );
        assertThat(response.teamType()).isEqualTo("zhengtuan");
        assertThat(response.perCapitaPitAmount()).isEqualByComparingTo("10");
        assertThat(response.optionalMarkupRate()).isEqualByComparingTo("70");
        assertThat(response.perCapitaShoppingAmount()).isEqualByComparingTo("600");
    }

    @Test
    void saveTeamShouldClearProfileFieldsWhenBlankValueIsSubmitted() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                mock(SalesProductMapper.class),
                teamMapper,
                mock(SalesTeamPriceMapper.class),
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity current = existingTeam("CS-SP-BK-260701A");
        current.setId(1001L);
        current.setBusinessType("亲子主题");
        current.setDepartmentName("计调一部");
        current.setOperatorEmployeeName("老板号");
        current.setEscortEmployeeName("王全陪");
        SalesTeamEntity latest = existingTeam("CS-SP-BK-260701A");
        latest.setId(1001L);
        latest.setUsedSeats(0);
        latest.setTotalSeats(30);
        latest.setRemainingSeats(30);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(current, latest);
        when(teamMapper.update(any(SalesTeamEntity.class), any(Wrapper.class))).thenReturn(1);
        ArgumentCaptor<Wrapper<SalesTeamEntity>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);

        service.saveTeam(
                1001L,
                new com.mtravel.platform.sales.team.dto.SalesTeamSaveRequest(
                        null,
                        null,
                        "",
                        null,
                        "",
                        null,
                        "",
                        null,
                        "",
                        null,
                        null,
                        "",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                1L,
                "admin"
        );

        verify(teamMapper).update(any(SalesTeamEntity.class), wrapperCaptor.capture());
        String sqlSet = ((UpdateWrapper<SalesTeamEntity>) wrapperCaptor.getValue()).getSqlSet();
        assertThat(sqlSet).contains(
                "business_type",
                "department_id",
                "department_name",
                "operator_employee_id",
                "operator_employee_name",
                "escort_employee_id",
                "escort_employee_name",
                "remark",
                "per_capita_pit_amount",
                "optional_markup_rate",
                "per_capita_shopping_amount"
        );
    }

    private SalesProductItineraryDayEntity itineraryDay(
            Integer dayNo,
            String title,
            String content,
            Integer distanceMeters,
            Integer durationSeconds
    ) {
        SalesProductItineraryDayEntity entity = new SalesProductItineraryDayEntity();
        entity.setProductId(88L);
        entity.setDayNo(dayNo);
        entity.setDayTitle(title);
        entity.setItineraryContent(content);
        entity.setRelatedHotel(dayNo == 1 ? "杭州西湖边酒店" : null);
        entity.setBreakfastIncluded(dayNo > 1);
        entity.setLunchIncluded(true);
        entity.setDinnerIncluded(dayNo < 3);
        entity.setRoadbookSummary(dayNo == 2 ? "酒店 -> 西湖 -> 灵隐寺 -> 酒店" : null);
        entity.setRoadbookTotalDistanceMeters(distanceMeters);
        entity.setRoadbookTotalDurationSeconds(durationSeconds);
        return entity;
    }

    private SalesProductRoadbookPointEntity roadbookPoint(Integer pointOrder, String placeName) {
        SalesProductRoadbookPointEntity entity = new SalesProductRoadbookPointEntity();
        entity.setProductId(88L);
        entity.setDayNo(1);
        entity.setPointOrder(pointOrder);
        entity.setPlaceName(placeName);
        entity.setAddress("杭州市西湖区");
        entity.setLongitude("120.145");
        entity.setLatitude("30.245");
        entity.setPointType("departure");
        entity.setStayMinutes(10);
        entity.setDistanceToNextMeters(12000);
        entity.setDurationToNextSeconds(1800);
        entity.setRemark("集合点");
        return entity;
    }

    private SalesProductEntity product() {
        SalesProductEntity product = new SalesProductEntity();
        product.setId(88L);
        product.setTenantId(1L);
        product.setProductName("测试产品");
        product.setBusinessType("疗休养");
        product.setDomesticInternational("domestic");
        product.setProvince("浙江省");
        product.setCity("杭州市");
        product.setDistrict("西湖区");
        product.setReceptionStandard("商务/快捷");
        product.setProductTheme("观光");
        product.setTravelDays(3);
        product.setCloseDaysBefore(1);
        product.setSingleRoomDifference(new BigDecimal("701"));
        product.setPlannedCapacity(77);
        product.setStatus("active");
        product.setIsDeleted(false);
        return product;
    }

    private SalesProductArrangementItemEntity productTrafficArrangement() {
        SalesProductArrangementItemEntity item = new SalesProductArrangementItemEntity();
        item.setId(7001L);
        item.setTenantId(1L);
        item.setProductId(88L);
        item.setArrangementType("traffic");
        item.setItemName("飞机票");
        item.setArrangementContent("第1天杭州到北京");
        item.setAllocationMode("group_order_average");
        item.setScheduleStartDay("第1天");
        item.setScheduleEndDay("第1天");
        item.setDeparturePlace("杭州");
        item.setArrivalPlace("北京");
        item.setResourceName("国航航班");
        item.setSupplierId(601L);
        item.setSupplierName("国航供应商");
        item.setTrafficType("飞机");
        item.setSettlementType("credit");
        item.setMealType("自助早");
        item.setFundIncluded("含");
        item.setConfirmed(true);
        item.setConfirmationNo("CN-PRODUCT-1");
        item.setGuideId(66L);
        item.setGuideName("产品导游");
        item.setTotalAmount(new BigDecimal("1200"));
        item.setCashAmount(BigDecimal.ZERO);
        item.setCreditAmount(new BigDecimal("1200"));
        item.setCostAmount(new BigDecimal("1200"));
        item.setNoGuideReport(false);
        item.setIsDeleted(false);
        return item;
    }

    private SalesProductArrangementPriceLineEntity productTrafficPriceLine() {
        SalesProductArrangementPriceLineEntity line = new SalesProductArrangementPriceLineEntity();
        line.setId(7101L);
        line.setTenantId(1L);
        line.setProductId(88L);
        line.setArrangementItemId(7001L);
        line.setProjectName("成人机票");
        line.setUnitPrice(new BigDecimal("120"));
        line.setQuantity(new BigDecimal("10"));
        line.setAmount(new BigDecimal("1200"));
        line.setCashAmount(BigDecimal.ZERO);
        line.setCreditAmount(new BigDecimal("1200"));
        line.setSortOrder(1);
        line.setIsDeleted(false);
        return line;
    }

    private DispatchTeamArrangementEntity teamArrangement(Long teamId, String arrangementType, boolean confirmed, String status) {
        DispatchTeamArrangementEntity entity = new DispatchTeamArrangementEntity();
        entity.setTenantId(1L);
        entity.setTeamId(teamId);
        entity.setArrangementType(arrangementType);
        entity.setConfirmed(confirmed);
        entity.setStatus(status);
        entity.setIsDeleted(false);
        return entity;
    }

    private DispatchTeamGuideEntity teamGuide(Long teamId, boolean tentative, String status) {
        return teamGuide(teamId, "测试导游", null, tentative, status);
    }

    private DispatchTeamGuideEntity teamGuide(Long teamId, String guideName, String guideMobile, boolean tentative, String status) {
        DispatchTeamGuideEntity entity = new DispatchTeamGuideEntity();
        entity.setTenantId(1L);
        entity.setTeamId(teamId);
        entity.setGuideId(66L);
        entity.setGuideName(guideName);
        entity.setGuideMobile(guideMobile);
        entity.setIsTentative(tentative);
        entity.setStatus(status);
        entity.setIsDeleted(false);
        return entity;
    }

    private SalesBookingOrderEntity teamOrder(Long teamId, String customerName, String status, String orderRole) {
        SalesBookingOrderEntity entity = new SalesBookingOrderEntity();
        entity.setTenantId(1L);
        entity.setTeamId(teamId);
        entity.setCustomerName(customerName);
        entity.setStatus(status);
        entity.setOrderRole(orderRole);
        entity.setIsDeleted(false);
        return entity;
    }

    private DispatchTeamArrangementSectionStatusEntity teamSectionStatus(Long teamId, String arrangementType, String status) {
        DispatchTeamArrangementSectionStatusEntity entity = new DispatchTeamArrangementSectionStatusEntity();
        entity.setTenantId(1L);
        entity.setTeamId(teamId);
        entity.setArrangementType(arrangementType);
        entity.setStatus(status);
        entity.setIsDeleted(false);
        return entity;
    }

    private SalesTeamEntity existingTeam(String teamNo) {
        SalesTeamEntity team = new SalesTeamEntity();
        team.setTenantId(1L);
        team.setProductId(88L);
        team.setTeamNo(teamNo);
        team.setTeamType("sanpin");
        team.setStatus("normal");
        team.setDepartureDate(LocalDate.of(2026, 7, 1));
        team.setIsDeleted(false);
        return team;
    }

    private SalesTeamPriceEntity existingPrice(Long id, Long teamId, Long categoryId, String categoryName) {
        SalesTeamPriceEntity price = new SalesTeamPriceEntity();
        price.setId(id);
        price.setTenantId(1L);
        price.setTeamId(teamId);
        price.setProductId(88L);
        price.setCustomerCategoryId(categoryId);
        price.setCustomerCategoryName(categoryName);
        price.setAdultPrice(BigDecimal.ZERO);
        price.setChildPrice(BigDecimal.ZERO);
        price.setChildNoBedPrice(BigDecimal.ZERO);
        price.setSeniorPrice(BigDecimal.ZERO);
        price.setExtraFee(BigDecimal.ZERO);
        price.setStatus("active");
        price.setIsDeleted(false);
        return price;
    }
}
