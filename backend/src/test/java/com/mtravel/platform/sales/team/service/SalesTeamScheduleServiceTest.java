package com.mtravel.platform.sales.team.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductDescriptionMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.team.dto.SalesTeamBatchCreateRequest;
import com.mtravel.platform.sales.team.dto.SalesTeamBatchEditRequest;
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
                [[TEAM_PROFILE_JSON]]{"businessType":"亲子主题","departmentName":"计调一部","operatorName":"产品默认计调","escortName":"王全陪"}
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
        SalesTeamScheduleService service = new SalesTeamScheduleService(
                productMapper,
                teamMapper,
                priceMapper,
                mock(SalesTeamStatusLogMapper.class)
        );
        SalesTeamEntity team = existingTeam("CS-SP-BK-260701A");
        team.setId(1001L);
        team.setTotalSeats(30);
        team.setUsedSeats(4);
        team.setRemainingSeats(26);
        when(teamMapper.selectPage(any(), any(Wrapper.class))).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SalesTeamEntity>(1, 20, 1).setRecords(List.of(team)));
        when(productMapper.selectList(any(Wrapper.class))).thenReturn(List.of(product()));

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
        verify(productMapper, times(1)).selectList(any(Wrapper.class));
        verify(priceMapper, never()).selectList(any(Wrapper.class));
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
        SalesProductDescriptionEntity description = new SalesProductDescriptionEntity();
        description.setProductId(88L);
        description.setProductDescription("产品说明正文");
        description.setBookingNotice("收客须知正文");
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
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
                        "顶部信息已确认"
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
                "remark"
        );
        assertThat(response.teamType()).isEqualTo("zhengtuan");
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
                        ""
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
                "remark"
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
