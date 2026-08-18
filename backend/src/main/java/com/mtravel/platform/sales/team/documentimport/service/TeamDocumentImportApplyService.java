package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveRequest;
import com.mtravel.platform.dispatch.teamarrangement.dto.TeamArrangementSaveResponse;
import com.mtravel.platform.dispatch.teamarrangement.service.DispatchTeamArrangementService;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderGuestRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderPriceLineRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveResponse;
import com.mtravel.platform.sales.booking.order.service.SalesBookingOrderService;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportApplyRequest;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportApplyResponse;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportApplyRecordEntity;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportTaskEntity;
import com.mtravel.platform.sales.team.documentimport.mapper.SalesDocumentImportApplyRecordMapper;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 将计调确认后的团队文档草稿写入订单、游客和正式团队安排。 */
@Service
public class TeamDocumentImportApplyService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Pattern VEHICLE_SEAT_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,3})\\s*座");
    private final TeamDocumentImportTaskService taskService;
    private final TeamDocumentImportCustomerResolver customerResolver;
    private final TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer;
    private final SalesDocumentImportApplyRecordMapper applyRecordMapper;
    private final SalesBookingOrderService orderService;
    private final DispatchTeamArrangementService arrangementService;
    private final SalesTeamMapper teamMapper;

    /** 兼容已有内部调用；正式 Spring 注入使用带团队主档 Mapper 的构造器。 */
    public TeamDocumentImportApplyService(
            TeamDocumentImportTaskService taskService,
            TeamDocumentImportCustomerResolver customerResolver,
            TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer,
            SalesDocumentImportApplyRecordMapper applyRecordMapper,
            SalesBookingOrderService orderService,
            DispatchTeamArrangementService arrangementService
    ) {
        this(
                taskService,
                customerResolver,
                resourceDraftSanitizer,
                applyRecordMapper,
                orderService,
                arrangementService,
                null
        );
    }

    /**
     * 团队 Word 草稿正式应用服务。
     *
     * <p>资源日次必须以已保存团队的发团日期换算，不能相信识别草稿中的日期，因为计调可在创建页
     * 修正团期后再应用导入结果。</p>
     */
    @Autowired
    public TeamDocumentImportApplyService(
            TeamDocumentImportTaskService taskService,
            TeamDocumentImportCustomerResolver customerResolver,
            TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer,
            SalesDocumentImportApplyRecordMapper applyRecordMapper,
            SalesBookingOrderService orderService,
            DispatchTeamArrangementService arrangementService,
            SalesTeamMapper teamMapper
    ) {
        this.taskService = taskService;
        this.customerResolver = customerResolver;
        this.resourceDraftSanitizer = resourceDraftSanitizer;
        this.applyRecordMapper = applyRecordMapper;
        this.orderService = orderService;
        this.arrangementService = arrangementService;
        this.teamMapper = teamMapper;
    }

    /**
     * 在团队已保存后应用草稿。订单、游客和安排均写入幂等记录，重复点击不会重复生成业务数据。
     */
    @Transactional
    public TeamDocumentImportApplyResponse apply(
            Long taskId, TeamDocumentImportApplyRequest request, Long tenantId, String operator
    ) {
        SalesDocumentImportTaskEntity task = taskService.requireTask(taskId, tenantId);
        if (task.getTargetTeamId() != null && !task.getTargetTeamId().equals(request.teamId())) {
            // 修改团队时，导入草稿只能回写其创建时绑定的团队，避免任务 ID 被用于写入其它团队。
            throw new BizException("该导入任务只允许应用到原团队");
        }
        // 草稿可以暂存未匹配客户，但正式订单必须关联当前租户的有效客户主档。
        TeamDocumentImportDraft draft = resourceDraftSanitizer.sanitize(
                customerResolver.requireCustomerForApplication(taskService.requireDraft(task), tenantId), tenantId
        );
        // 先校验真实团期，再创建订单或安排。不能在团期缺失时落下 D1/D2 这种不可统计的日期文本。
        LocalDate departureDate = Boolean.TRUE.equals(request.applyArrangements())
                ? requireTeamDepartureDate(request.teamId(), tenantId)
                : null;
        SalesDocumentImportApplyRecordEntity existingOrder = activeRecord(taskId, "order", "order:1", tenantId);
        SalesBookingOrderSaveResponse order;
        boolean alreadyApplied = existingOrder != null;
        if (existingOrder != null) {
            order = new SalesBookingOrderSaveResponse(existingOrder.getTargetId(), request.teamId(), null, "pending", 0, ZERO, ZERO, ZERO);
        } else {
            order = orderService.save(orderRequest(request.teamId(), draft, Boolean.TRUE.equals(request.applyGuests())), tenantId, operator);
            record(taskId, "order", order.id(), "order:1", tenantId, operator);
        }
        List<Long> arrangementIds = Boolean.TRUE.equals(request.applyArrangements())
                ? applyArrangements(
                        taskId,
                        request.teamId(),
                        order.id(),
                        draft,
                        departureDate,
                        tenantId,
                        operator
                )
                : List.of();
        taskService.markApplied(taskId, request.teamId(), tenantId, operator);
        return new TeamDocumentImportApplyResponse(
                request.teamId(), order.id(), Boolean.TRUE.equals(request.applyGuests()) ? safe(draft.guests()).size() : 0,
                arrangementIds, alreadyApplied
        );
    }

    private SalesBookingOrderSaveRequest orderRequest(Long teamId, TeamDocumentImportDraft draft, boolean applyGuests) {
        TeamDocumentImportDraft.OrderDraft order = draft.order();
        if (order == null) throw new BizException("导入草稿缺少订单信息，请先在预览中补齐");
        return new SalesBookingOrderSaveRequest(
                null, teamId, null, order.customerId(), clean(order.customerName()), clean(order.contactName()), clean(order.contactPhone()),
                null, null, null, null, null, null, null, null, null, travelDescription(draft), clean(order.pickupInfo()),
                clean(order.dropoffInfo()), null, clean(order.guideName()), clean(order.guidePhone()), null, hotelInfo(draft), null,
                "团队 Word 智能代录", clean(order.orderRemark()), null, null, "pending", priceLines(order.priceLines()),
                applyGuests ? guests(draft.guests()) : List.of()
        );
    }

    private List<Long> applyArrangements(
            Long taskId,
            Long teamId,
            Long orderId,
            TeamDocumentImportDraft draft,
            LocalDate departureDate,
            Long tenantId,
            String operator
    ) {
        List<Long> ids = new ArrayList<>();
        for (TeamDocumentImportDraft.ResourceDraft resource : safe(draft.resources())) {
            boolean confirmedResource = resource.selectedResourceId() != null && !resource.requiresConfirmation();
            boolean unmatchedVehicle = "vehicle".equals(resource.arrangementType())
                    && StringUtils.hasText(resource.sourceName());
            // 未确认的景区、酒店等不能擅自写入正式安排；用车需要先落一条可编辑草稿，方便计调补供应商。
            if (!confirmedResource && !unmatchedVehicle) continue;
            if (activeRecord(taskId, "arrangement", resource.itemKey(), tenantId) != null) continue;
            String itemName = confirmedResource ? resource.selectedResourceName() : resource.sourceName();
            String resourceName = confirmedResource ? resource.selectedResourceName() : resource.sourceName();
            Long supplierId = confirmedResource ? resource.selectedSupplierId() : null;
            String supplierName = confirmedResource ? resource.selectedSupplierName() : null;
            String vehicleType = "vehicle".equals(resource.arrangementType())
                    ? parsedVehicleType(resource.sourceName(), resource.selectedResourceName()) : null;
            TeamArrangementSaveResponse saved = arrangementService.save(
                    teamId,
                    new TeamArrangementSaveRequest(
                            null, resource.arrangementType(), cleanRequired(itemName), clean(resource.remark()),
                            "group_order_average", List.of(orderId), null,
                            scheduleStartDay(resource, departureDate), scheduleEndDay(resource, draft, departureDate), null, resource.city(), daysCount(resource, draft),
                            resourceName, supplierId, supplierName, null, vehicleType,
                            null, null, null, null, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO,
                            ZERO, Boolean.FALSE, List.of(), generatedRemark(resource), "credit", null, null, Boolean.FALSE, null, null, null
                    ), tenantId, operator
            );
            for (Long id : saved.ids()) record(taskId, "arrangement", id, resource.itemKey(), tenantId, operator);
            ids.addAll(saved.ids());
        }
        return ids;
    }

    /** 将 Word 的 D1/D2 日次转换为团队主档真实发团日期。 */
    private String scheduleStartDay(TeamDocumentImportDraft.ResourceDraft resource, LocalDate departureDate) {
        int startDay = vehicleStartDay(resource);
        if (resource.dayNo() == null && !"vehicle".equals(resource.arrangementType())) return null;
        return departureDate.plusDays(startDay - 1L).toString();
    }

    private String scheduleEndDay(
            TeamDocumentImportDraft.ResourceDraft resource,
            TeamDocumentImportDraft draft,
            LocalDate departureDate
    ) {
        int startDay = vehicleStartDay(resource);
        if ("hotel".equals(resource.arrangementType())) {
            int nights = Math.max(1, daysCount(resource, draft));
            return resource.dayNo() == null ? null : departureDate.plusDays(startDay - 1L + nights).toString();
        }
        if ("vehicle".equals(resource.arrangementType()) && travelDays(draft) > startDay) {
            return departureDate.plusDays(travelDays(draft) - 1L).toString();
        }
        return null;
    }

    private Integer daysCount(TeamDocumentImportDraft.ResourceDraft resource, TeamDocumentImportDraft draft) {
        int startDay = vehicleStartDay(resource);
        if ("vehicle".equals(resource.arrangementType()) && travelDays(draft) >= startDay) {
            return travelDays(draft) - startDay + 1;
        }
        return 1;
    }

    private int vehicleStartDay(TeamDocumentImportDraft.ResourceDraft resource) {
        return resource.dayNo() != null && resource.dayNo() > 0 ? resource.dayNo() : 1;
    }

    private int travelDays(TeamDocumentImportDraft draft) {
        return draft.team() == null || draft.team().travelDays() == null ? 0 : draft.team().travelDays();
    }

    /** 查询已保存团队的真实发团日期；草稿日期仅用于预览，不能作为正式安排日期依据。 */
    private LocalDate requireTeamDepartureDate(Long teamId, Long tenantId) {
        if (teamMapper == null || teamId == null || tenantId == null) {
            throw new BizException("团队缺少有效发团日期，无法将 Word 行程日转换为真实服务日期");
        }
        SalesTeamEntity team = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", teamId)
                .last("limit 1"));
        if (team == null || team.getDepartureDate() == null) {
            throw new BizException("团队缺少有效发团日期，无法将 Word 行程日转换为真实服务日期");
        }
        return team.getDepartureDate();
    }

    /** 在系统自动备注中留下原始日次，方便计调从真实日期回溯 Word 的 D1/D2 行程。 */
    private String generatedRemark(TeamDocumentImportDraft.ResourceDraft resource) {
        String sourceDay = resource.dayNo() != null && resource.dayNo() > 0
                ? "，原行程 D" + resource.dayNo()
                : "";
        return "由 Word 智能代录生成" + sourceDay + "，待计调确认资源和成本";
    }

    /** 将“33座旅游大巴”转换成安排中的车型，保留原始资源名称供计调核对。 */
    private String parsedVehicleType(String sourceName, String selectedResourceName) {
        String value = StringUtils.hasText(sourceName) ? sourceName : selectedResourceName;
        if (!StringUtils.hasText(value)) return null;
        String parsed = VEHICLE_SEAT_PATTERN.matcher(value).replaceAll("")
                .replaceAll("[()（）【】\\[\\]]", " ").trim();
        return StringUtils.hasText(parsed) ? parsed : value.trim();
    }

    private List<SalesBookingOrderPriceLineRequest> priceLines(List<TeamDocumentImportDraft.OrderPriceDraft> values) {
        return safe(values).stream()
                .filter(item -> item.unitPrice() != null && item.quantity() != null)
                .map(item -> new SalesBookingOrderPriceLineRequest(null, item.lineType(), item.itemName(), item.unitPrice(), item.quantity(), true, null))
                .toList();
    }

    private List<SalesBookingOrderGuestRequest> guests(List<TeamDocumentImportDraft.GuestDraft> values) {
        return safe(values).stream().map(item -> new SalesBookingOrderGuestRequest(
                null, item.indexNo(), clean(item.guestName()), null, clean(item.certificateNo()), null, clean(item.gender()),
                parseDate(item.birthDate()), item.age(), clean(item.phone()), clean(item.guestType()), clean(item.roomGroup()),
                clean(item.roomRemark()), Boolean.TRUE.equals(item.leaderFlag()), clean(item.remark())
        )).toList();
    }

    private String travelDescription(TeamDocumentImportDraft draft) {
        return safe(draft.itineraryDays()).stream().map(item -> "D" + item.dayNo() + " " + clean(item.itineraryContent()))
                .filter(StringUtils::hasText).collect(java.util.stream.Collectors.joining("\n"));
    }

    private String hotelInfo(TeamDocumentImportDraft draft) {
        return safe(draft.itineraryDays()).stream().map(TeamDocumentImportDraft.ItineraryDraft::accommodationNote)
                .filter(StringUtils::hasText).distinct().collect(java.util.stream.Collectors.joining("；"));
    }

    private SalesDocumentImportApplyRecordEntity activeRecord(Long taskId, String type, String key, Long tenantId) {
        return applyRecordMapper.selectOne(new QueryWrapper<SalesDocumentImportApplyRecordEntity>()
                .eq("tenant_id", tenantId).eq("is_deleted", false).eq("task_id", taskId)
                .eq("target_type", type).eq("draft_item_key", key).last("limit 1"));
    }

    private void record(Long taskId, String type, Long targetId, String key, Long tenantId, String operator) {
        SalesDocumentImportApplyRecordEntity entity = new SalesDocumentImportApplyRecordEntity();
        entity.setTenantId(tenantId); entity.setTaskId(taskId); entity.setTargetType(type); entity.setTargetId(targetId);
        entity.setDraftItemKey(key); entity.setStatus("applied"); entity.setCreatedBy(operator); entity.setIsDeleted(false);
        applyRecordMapper.insert(entity);
    }

    private String cleanRequired(String value) {
        if (!StringUtils.hasText(value)) throw new BizException("已确认资源缺少名称，请重新选择");
        return value.trim();
    }
    private String clean(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private java.time.LocalDate parseDate(String value) { try { return StringUtils.hasText(value) ? java.time.LocalDate.parse(value) : null; } catch (RuntimeException ignored) { return null; } }
    private <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }
}
