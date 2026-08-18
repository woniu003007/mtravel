package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveResponse;
import com.mtravel.platform.dispatch.teamarrangement.service.DispatchTeamArrangementService;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveResponse;
import com.mtravel.platform.sales.booking.order.service.SalesBookingOrderService;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportApplyRequest;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportApplyRecordEntity;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportTaskEntity;
import com.mtravel.platform.sales.team.documentimport.mapper.SalesDocumentImportApplyRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 团队文档草稿正式写入测试，固定重复保存的幂等边界。 */
class TeamDocumentImportApplyServiceTest {

    @Test
    void applyShouldRejectUsingAnEditTaskForAnotherTeam() {
        TeamDocumentImportTaskService taskService = mock(TeamDocumentImportTaskService.class);
        TeamDocumentImportCustomerResolver customerResolver = mock(TeamDocumentImportCustomerResolver.class);
        TeamDocumentImportApplyService service = new TeamDocumentImportApplyService(
                taskService,
                customerResolver,
                resourceDraftSanitizer(),
                mock(SalesDocumentImportApplyRecordMapper.class),
                mock(SalesBookingOrderService.class),
                mock(DispatchTeamArrangementService.class)
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(31L);
        task.setTargetTeamId(81L);
        when(taskService.requireTask(31L, 1L)).thenReturn(task);

        assertThatThrownBy(() -> service.apply(31L, new TeamDocumentImportApplyRequest(82L, true, true), 1L, "planner"))
                .isInstanceOf(BizException.class)
                .hasMessage("该导入任务只允许应用到原团队");
        verify(taskService, times(0)).requireDraft(any());
    }

    @Test
    void applyShouldCreateOnceAndReuseExistingRecordsOnRetry() {
        TeamDocumentImportTaskService taskService = mock(TeamDocumentImportTaskService.class);
        TeamDocumentImportCustomerResolver customerResolver = mock(TeamDocumentImportCustomerResolver.class);
        SalesDocumentImportApplyRecordMapper recordMapper = mock(SalesDocumentImportApplyRecordMapper.class);
        SalesBookingOrderService orderService = mock(SalesBookingOrderService.class);
        DispatchTeamArrangementService arrangementService = mock(DispatchTeamArrangementService.class);
        TeamDocumentImportApplyService service = new TeamDocumentImportApplyService(
                taskService,
                customerResolver,
                resourceDraftSanitizer(),
                recordMapper,
                orderService,
                arrangementService,
                teamMapper("2026-08-10")
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(31L);
        TeamDocumentImportDraft draft = draft();
        when(taskService.requireTask(31L, 1L)).thenReturn(task);
        when(taskService.requireDraft(task)).thenReturn(draft);
        when(customerResolver.requireCustomerForApplication(draft, 1L)).thenReturn(draft);
        when(orderService.save(any(), eq(1L), eq("planner"))).thenReturn(
                new SalesBookingOrderSaveResponse(41L, 81L, "SO-260101-00001", "pending", 1,
                        new BigDecimal("2999"), BigDecimal.ZERO, new BigDecimal("2999"))
        );
        when(arrangementService.save(eq(81L), any(), eq(1L), eq("planner")))
                .thenReturn(new TeamArrangementSaveResponse(91L, List.of(91L)));
        SalesDocumentImportApplyRecordEntity orderRecord = record("order", 41L, "order:1");
        SalesDocumentImportApplyRecordEntity arrangementRecord = record("arrangement", 91L, "resource:1");
        when(recordMapper.selectOne(any(Wrapper.class)))
                .thenReturn(null, null, orderRecord, arrangementRecord);
        when(recordMapper.insert(any(SalesDocumentImportApplyRecordEntity.class))).thenReturn(1);

        var first = service.apply(31L, new TeamDocumentImportApplyRequest(81L, true, true), 1L, "planner");
        var retry = service.apply(31L, new TeamDocumentImportApplyRequest(81L, true, true), 1L, "planner");

        assertThat(first.orderId()).isEqualTo(41L);
        assertThat(first.guestCount()).isEqualTo(1);
        assertThat(first.arrangementIds()).containsExactly(91L);
        assertThat(first.alreadyApplied()).isFalse();
        assertThat(retry.orderId()).isEqualTo(41L);
        assertThat(retry.arrangementIds()).isEmpty();
        assertThat(retry.alreadyApplied()).isTrue();
        verify(orderService, times(1)).save(any(), eq(1L), eq("planner"));
        verify(arrangementService, times(1)).save(eq(81L), any(), eq(1L), eq("planner"));
        verify(taskService, times(2)).markApplied(31L, 81L, 1L, "planner");

        ArgumentCaptor<com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest> requestCaptor =
                ArgumentCaptor.forClass(com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest.class);
        verify(orderService).save(requestCaptor.capture(), eq(1L), eq("planner"));
        assertThat(requestCaptor.getValue().teamId()).isEqualTo(81L);
        assertThat(requestCaptor.getValue().customerId()).isEqualTo(71L);
        assertThat(requestCaptor.getValue().guests()).hasSize(1);
        assertThat(requestCaptor.getValue().receivedAmount()).isNull();
        assertThat(requestCaptor.getValue().status()).isEqualTo("pending");
    }

    @Test
    void applyShouldRejectFreeTextCustomerBeforeCreatingAnOrder() {
        TeamDocumentImportTaskService taskService = mock(TeamDocumentImportTaskService.class);
        SalesBookingOrderService orderService = mock(SalesBookingOrderService.class);
        TeamDocumentImportApplyService service = new TeamDocumentImportApplyService(
                taskService,
                new TeamDocumentImportCustomerResolver(mock(CustomerUnitMapper.class)),
                resourceDraftSanitizer(),
                mock(SalesDocumentImportApplyRecordMapper.class),
                orderService,
                mock(DispatchTeamArrangementService.class)
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(31L);
        when(taskService.requireTask(31L, 1L)).thenReturn(task);
        when(taskService.requireDraft(task)).thenReturn(draftWithFreeTextCustomer());

        assertThatThrownBy(() -> service.apply(31L, new TeamDocumentImportApplyRequest(81L, true, true), 1L, "planner"))
                .isInstanceOf(BizException.class)
                .hasMessage("客户单位必须从系统客户主档选择，不能直接填写名称");
        verify(orderService, never()).save(any(), any(), any());
    }

    @Test
    void applyShouldRequireSelectingASystemCustomerBeforeCreatingAnOrder() {
        TeamDocumentImportTaskService taskService = mock(TeamDocumentImportTaskService.class);
        SalesBookingOrderService orderService = mock(SalesBookingOrderService.class);
        TeamDocumentImportApplyService service = new TeamDocumentImportApplyService(
                taskService,
                new TeamDocumentImportCustomerResolver(mock(CustomerUnitMapper.class)),
                resourceDraftSanitizer(),
                mock(SalesDocumentImportApplyRecordMapper.class),
                orderService,
                mock(DispatchTeamArrangementService.class)
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(31L);
        when(taskService.requireTask(31L, 1L)).thenReturn(task);
        when(taskService.requireDraft(task)).thenReturn(draftWithoutCustomer());

        assertThatThrownBy(() -> service.apply(31L, new TeamDocumentImportApplyRequest(81L, true, true), 1L, "planner"))
                .isInstanceOf(BizException.class)
                .hasMessage("请先从系统客户主档选择客户单位，再生成订单");
        verify(orderService, never()).save(any(), any(), any());
    }

    @Test
    void applyShouldCreateEditableVehicleArrangementWithoutSelectingAResource() {
        TeamDocumentImportTaskService taskService = mock(TeamDocumentImportTaskService.class);
        TeamDocumentImportCustomerResolver customerResolver = mock(TeamDocumentImportCustomerResolver.class);
        SalesDocumentImportApplyRecordMapper recordMapper = mock(SalesDocumentImportApplyRecordMapper.class);
        SalesBookingOrderService orderService = mock(SalesBookingOrderService.class);
        DispatchTeamArrangementService arrangementService = mock(DispatchTeamArrangementService.class);
        TeamDocumentImportApplyService service = new TeamDocumentImportApplyService(
                taskService,
                customerResolver,
                resourceDraftSanitizer(),
                recordMapper,
                orderService,
                arrangementService,
                teamMapper("2026-08-10")
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(31L);
        TeamDocumentImportDraft draft = draftWithUnmatchedVehicle();
        when(taskService.requireTask(31L, 1L)).thenReturn(task);
        when(taskService.requireDraft(task)).thenReturn(draft);
        when(customerResolver.requireCustomerForApplication(draft, 1L)).thenReturn(draft);
        when(orderService.save(any(), eq(1L), eq("planner"))).thenReturn(
                new SalesBookingOrderSaveResponse(41L, 81L, "SO-260101-00001", "pending", 0,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        when(arrangementService.save(eq(81L), any(), eq(1L), eq("planner")))
                .thenReturn(new TeamArrangementSaveResponse(92L, List.of(92L)));
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(recordMapper.insert(any(SalesDocumentImportApplyRecordEntity.class))).thenReturn(1);

        var result = service.apply(31L, new TeamDocumentImportApplyRequest(81L, false, true), 1L, "planner");

        assertThat(result.arrangementIds()).containsExactly(92L);
        ArgumentCaptor<com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest> requestCaptor =
                ArgumentCaptor.forClass(com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest.class);
        verify(arrangementService).save(eq(81L), requestCaptor.capture(), eq(1L), eq("planner"));
        var request = requestCaptor.getValue();
        assertThat(request.arrangementType()).isEqualTo("vehicle");
        assertThat(request.itemName()).isEqualTo("33座旅游大巴");
        assertThat(request.resourceName()).isEqualTo("33座旅游大巴");
        assertThat(request.vehicleType()).isEqualTo("旅游大巴");
        assertThat(request.supplierId()).isNull();
        assertThat(request.supplierName()).isNull();
        assertThat(request.scheduleStartDay()).isEqualTo("2026-08-10");
        assertThat(request.scheduleEndDay()).isEqualTo("2026-08-13");
        assertThat(request.daysCount()).isEqualTo(4);
        assertThat(request.remark()).contains("原行程 D1");
    }

    @Test
    void applyShouldNotCreateArrangementForLegacyNotRequiredResource() {
        TeamDocumentImportTaskService taskService = mock(TeamDocumentImportTaskService.class);
        TeamDocumentImportCustomerResolver customerResolver = mock(TeamDocumentImportCustomerResolver.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        SalesDocumentImportApplyRecordMapper recordMapper = mock(SalesDocumentImportApplyRecordMapper.class);
        SalesBookingOrderService orderService = mock(SalesBookingOrderService.class);
        DispatchTeamArrangementService arrangementService = mock(DispatchTeamArrangementService.class);
        TeamDocumentImportApplyService service = new TeamDocumentImportApplyService(
                taskService,
                customerResolver,
                resourceDraftSanitizer(resourceMapper),
                recordMapper,
                orderService,
                arrangementService,
                teamMapper("2026-08-10")
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(31L);
        TeamDocumentImportDraft draft = draft();
        PurchaseResourceEntity freeScenic = new PurchaseResourceEntity();
        freeScenic.setId(88L);
        freeScenic.setProcurementMode("not_required");
        when(taskService.requireTask(31L, 1L)).thenReturn(task);
        when(taskService.requireDraft(task)).thenReturn(draft);
        when(customerResolver.requireCustomerForApplication(draft, 1L)).thenReturn(draft);
        when(resourceMapper.selectList(any())).thenReturn(List.of(freeScenic));
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(recordMapper.insert(any(SalesDocumentImportApplyRecordEntity.class))).thenReturn(1);
        when(orderService.save(any(), eq(1L), eq("planner"))).thenReturn(
                new SalesBookingOrderSaveResponse(41L, 81L, "SO-260101-00001", "pending", 0,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );

        var result = service.apply(31L, new TeamDocumentImportApplyRequest(81L, false, true), 1L, "planner");

        assertThat(result.arrangementIds()).isEmpty();
        verify(arrangementService, never()).save(any(), any(), any(), any());
    }

    @Test
    void applyShouldConvertResourceDayNoUsingPersistedTeamDepartureDate() {
        TeamDocumentImportTaskService taskService = mock(TeamDocumentImportTaskService.class);
        TeamDocumentImportCustomerResolver customerResolver = mock(TeamDocumentImportCustomerResolver.class);
        SalesDocumentImportApplyRecordMapper recordMapper = mock(SalesDocumentImportApplyRecordMapper.class);
        SalesBookingOrderService orderService = mock(SalesBookingOrderService.class);
        DispatchTeamArrangementService arrangementService = mock(DispatchTeamArrangementService.class);
        TeamDocumentImportApplyService service = new TeamDocumentImportApplyService(
                taskService,
                customerResolver,
                resourceDraftSanitizer(),
                recordMapper,
                orderService,
                arrangementService,
                // 草稿日期故意不同，必须以团队主档日期作为换算基准。
                teamMapper("2026-06-25")
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(68L);
        TeamDocumentImportDraft draft = draftWithDatedResources();
        when(taskService.requireTask(68L, 1L)).thenReturn(task);
        when(taskService.requireDraft(task)).thenReturn(draft);
        when(customerResolver.requireCustomerForApplication(draft, 1L)).thenReturn(draft);
        when(orderService.save(any(), eq(1L), eq("planner"))).thenReturn(
                new SalesBookingOrderSaveResponse(41L, 68L, "SO-260625-00001", "pending", 0,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        when(arrangementService.save(eq(68L), any(), eq(1L), eq("planner")))
                .thenReturn(new TeamArrangementSaveResponse(91L, List.of(91L)));
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(recordMapper.insert(any(SalesDocumentImportApplyRecordEntity.class))).thenReturn(1);

        service.apply(68L, new TeamDocumentImportApplyRequest(68L, false, true), 1L, "planner");

        ArgumentCaptor<com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest> captor =
                ArgumentCaptor.forClass(com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest.class);
        verify(arrangementService, times(3)).save(eq(68L), captor.capture(), eq(1L), eq("planner"));
        var byItemName = captor.getAllValues().stream().collect(java.util.stream.Collectors.toMap(
                com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest::itemName,
                java.util.function.Function.identity()
        ));

        var scenic = byItemName.get("西湖风景名胜区");
        assertThat(scenic.scheduleStartDay()).isEqualTo("2026-06-26");
        assertThat(scenic.scheduleEndDay()).isNull();
        assertThat(scenic.remark()).contains("原行程 D2");

        var hotel = byItemName.get("杭州大酒店");
        assertThat(hotel.scheduleStartDay()).isEqualTo("2026-06-25");
        assertThat(hotel.scheduleEndDay()).isEqualTo("2026-06-26");
        assertThat(hotel.daysCount()).isEqualTo(1);

        var vehicle = byItemName.get("33座旅游大巴");
        assertThat(vehicle.scheduleStartDay()).isEqualTo("2026-06-26");
        assertThat(vehicle.scheduleEndDay()).isEqualTo("2026-06-28");
        assertThat(vehicle.daysCount()).isEqualTo(3);
    }

    @Test
    void applyShouldRejectArrangementApplicationWhenPersistedTeamHasNoDepartureDate() {
        TeamDocumentImportTaskService taskService = mock(TeamDocumentImportTaskService.class);
        TeamDocumentImportCustomerResolver customerResolver = mock(TeamDocumentImportCustomerResolver.class);
        SalesBookingOrderService orderService = mock(SalesBookingOrderService.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(68L);
        when(teamMapper.selectOne(any())).thenReturn(team);
        TeamDocumentImportApplyService service = new TeamDocumentImportApplyService(
                taskService,
                customerResolver,
                resourceDraftSanitizer(),
                mock(SalesDocumentImportApplyRecordMapper.class),
                orderService,
                mock(DispatchTeamArrangementService.class),
                teamMapper
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(68L);
        TeamDocumentImportDraft draft = draftWithDatedResources();
        when(taskService.requireTask(68L, 1L)).thenReturn(task);
        when(taskService.requireDraft(task)).thenReturn(draft);
        when(customerResolver.requireCustomerForApplication(draft, 1L)).thenReturn(draft);

        assertThatThrownBy(() -> service.apply(68L, new TeamDocumentImportApplyRequest(68L, false, true), 1L, "planner"))
                .isInstanceOf(BizException.class)
                .hasMessage("团队缺少有效发团日期，无法将 Word 行程日转换为真实服务日期");
        verify(orderService, never()).save(any(), any(), any());
    }

    private TeamDocumentImportDraft draft() {
        return new TeamDocumentImportDraft(
                "ground_confirmation", 0.9,
                new TeamDocumentImportDraft.TeamDraft("杭州二日游", "2026-08-10", 2, 1, null, "domestic", null, null),
                new TeamDocumentImportDraft.OrderDraft(71L, "杭州百缘旅行社", "叶菊莲", "13521124678", null, null, null, null, null,
                        List.of(new TeamDocumentImportDraft.OrderPriceDraft("adult", "成人", new BigDecimal("2999"), BigDecimal.ONE))),
                List.of(new TeamDocumentImportDraft.GuestDraft(1, "张三", "210204198206214832", "男", "1982-06-21", 44,
                        "13800000000", "adult", "1房", null, false, true, null)),
                List.of(),
                List.of(new TeamDocumentImportDraft.ResourceDraft("resource:1", 1, null, "scenic", "西湖", "杭州", null,
                        88L, "西湖风景名胜区", 98L, "西湖票务供应商", false, List.of())),
                List.of(), List.of()
        );
    }

    private TeamDocumentImportDraft draftWithFreeTextCustomer() {
        return new TeamDocumentImportDraft(
                "ground_confirmation", 0.9,
                new TeamDocumentImportDraft.TeamDraft("杭州二日游", "2026-08-10", 2, 1, null, "domestic", null, null),
                new TeamDocumentImportDraft.OrderDraft(null, "未建档旅行社", "叶菊莲", "13521124678", null, null, null, null, null, List.of()),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private TeamDocumentImportDraft draftWithUnmatchedVehicle() {
        return new TeamDocumentImportDraft(
                "ground_confirmation", 0.9,
                new TeamDocumentImportDraft.TeamDraft("杭州四日游", "2026-08-10", 4, 1, null, "domestic", null, null),
                new TeamDocumentImportDraft.OrderDraft(71L, "杭州百缘旅行社", "叶菊莲", "13521124678", null, null, null, null, null,
                        List.of()),
                List.of(), List.of(), List.of(
                        new TeamDocumentImportDraft.ResourceDraft("resource:vehicle", 1, null, "vehicle", "33座旅游大巴", null, null,
                                null, null, null, null, true, List.of()),
                        new TeamDocumentImportDraft.ResourceDraft("resource:unmatched-scenic", 1, null, "scenic", "西湖", "杭州", null,
                                null, null, null, null, true, List.of())
                ), List.of(), List.of()
        );
    }

    private TeamDocumentImportDraft draftWithDatedResources() {
        return new TeamDocumentImportDraft(
                "ground_confirmation", 0.9,
                // 刻意与团队主档的 2026-06-25 不同，验证不能使用 AI 草稿日期。
                new TeamDocumentImportDraft.TeamDraft("华东四日游", "2026-01-01", 4, 1, null, "domestic", null, null),
                new TeamDocumentImportDraft.OrderDraft(71L, "杭州百缘旅行社", "叶菊莲", "13521124678", null, null, null, null, null,
                        List.of()),
                List.of(),
                List.of(),
                List.of(
                        new TeamDocumentImportDraft.ResourceDraft("resource:scenic", 2, null, "scenic", "西湖", "杭州", null,
                                88L, "西湖风景名胜区", 98L, "西湖票务供应商", false, List.of()),
                        new TeamDocumentImportDraft.ResourceDraft("resource:hotel", 1, null, "hotel", "杭州大酒店", "杭州", null,
                                89L, "杭州大酒店", 99L, "杭州酒店供应商", false, List.of()),
                        new TeamDocumentImportDraft.ResourceDraft("resource:vehicle", 2, null, "vehicle", "33座旅游大巴", "杭州", null,
                                null, null, null, null, true, List.of())
                ),
                List.of(),
                List.of()
        );
    }

    private TeamDocumentImportDraft draftWithoutCustomer() {
        return new TeamDocumentImportDraft(
                "ground_confirmation", 0.9,
                new TeamDocumentImportDraft.TeamDraft("杭州二日游", "2026-08-10", 2, 1, null, "domestic", null, null),
                new TeamDocumentImportDraft.OrderDraft(null, null, "叶菊莲", "13521124678", null, null, null, null, null, List.of()),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private SalesDocumentImportApplyRecordEntity record(String targetType, Long targetId, String itemKey) {
        SalesDocumentImportApplyRecordEntity entity = new SalesDocumentImportApplyRecordEntity();
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setDraftItemKey(itemKey);
        return entity;
    }

    private TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer() {
        return resourceDraftSanitizer(mock(PurchaseResourceMapper.class));
    }

    private TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer(PurchaseResourceMapper resourceMapper) {
        return new TeamDocumentImportResourceDraftSanitizer(
                new TeamDocumentImportResourceNormalizer(),
                new TeamDocumentImportBusinessPartyNameExtractor(),
                resourceMapper
        );
    }

    private SalesTeamMapper teamMapper(String departureDate) {
        SalesTeamMapper mapper = mock(SalesTeamMapper.class);
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(68L);
        team.setDepartureDate(LocalDate.parse(departureDate));
        when(mapper.selectOne(any())).thenReturn(team);
        return mapper;
    }
}
