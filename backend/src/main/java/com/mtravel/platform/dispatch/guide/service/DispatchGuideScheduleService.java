package com.mtravel.platform.dispatch.guide.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.dispatch.guide.dto.GuideAvailabilityResponse;
import com.mtravel.platform.dispatch.guide.dto.GuideLeaveResponse;
import com.mtravel.platform.dispatch.guide.dto.GuideLeaveSaveRequest;
import com.mtravel.platform.dispatch.guide.dto.GuideScheduleCalendarResponse;
import com.mtravel.platform.dispatch.guide.dto.GuideScheduleQuery;
import com.mtravel.platform.dispatch.guide.dto.TeamGuideFieldUpdateRequest;
import com.mtravel.platform.dispatch.guide.dto.TeamGuideResponse;
import com.mtravel.platform.dispatch.guide.dto.TeamGuideResponse.ImprestSummary;
import com.mtravel.platform.dispatch.guide.dto.TeamGuideSaveRequest;
import com.mtravel.platform.dispatch.guide.entity.DispatchGuideLeaveRecordEntity;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.enums.DispatchTeamGuideStatus;
import com.mtravel.platform.dispatch.guide.enums.GuideLeaveSourceType;
import com.mtravel.platform.dispatch.guide.enums.GuideLeaveStatus;
import com.mtravel.platform.dispatch.guide.mapper.DispatchGuideLeaveRecordMapper;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideEntity;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideMapper;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestEntity;
import com.mtravel.platform.finance.guideimprest.enums.GuideImprestStatus;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import com.mtravel.platform.sales.team.service.SalesTeamListSummaryRefreshService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 导游安排、导游请假和排班汇总业务服务。
 *
 * <p>本服务集中维护导游时间占用规则。团队导游安排和已通过请假都会进入排班汇总；
 * 新增安排、审批请假和计调直接设置请假时必须在这里做统一冲突判断。</p>
 */
@Service
public class DispatchGuideScheduleService {

    private static final int CALENDAR_DAYS = 37;

    private final DispatchTeamGuideMapper teamGuideMapper;
    private final DispatchGuideLeaveRecordMapper leaveRecordMapper;
    private final SalesTeamMapper teamMapper;
    private final EnterpriseGuideMapper guideMapper;
    private final FinanceGuideImprestMapper imprestMapper;
    private final SalesTeamListSummaryRefreshService teamListSummaryRefreshService;

    /**
     * 构造导游排班服务。
     *
     * @param teamGuideMapper 团队导游安排 Mapper
     * @param leaveRecordMapper 导游请假记录 Mapper
     * @param teamMapper 团队 Mapper
     * @param guideMapper 导游档案 Mapper
     */
    @Autowired
    public DispatchGuideScheduleService(
            DispatchTeamGuideMapper teamGuideMapper,
            DispatchGuideLeaveRecordMapper leaveRecordMapper,
            SalesTeamMapper teamMapper,
            EnterpriseGuideMapper guideMapper,
            FinanceGuideImprestMapper imprestMapper,
            SalesTeamListSummaryRefreshService teamListSummaryRefreshService
    ) {
        this.teamGuideMapper = teamGuideMapper;
        this.leaveRecordMapper = leaveRecordMapper;
        this.teamMapper = teamMapper;
        this.guideMapper = guideMapper;
        this.imprestMapper = imprestMapper;
        this.teamListSummaryRefreshService = teamListSummaryRefreshService;
    }

    DispatchGuideScheduleService(
            DispatchTeamGuideMapper teamGuideMapper,
            DispatchGuideLeaveRecordMapper leaveRecordMapper,
            SalesTeamMapper teamMapper,
            EnterpriseGuideMapper guideMapper,
            FinanceGuideImprestMapper imprestMapper
    ) {
        this(teamGuideMapper, leaveRecordMapper, teamMapper, guideMapper, imprestMapper, null);
    }

    DispatchGuideScheduleService(
            DispatchTeamGuideMapper teamGuideMapper,
            DispatchGuideLeaveRecordMapper leaveRecordMapper,
            SalesTeamMapper teamMapper,
            EnterpriseGuideMapper guideMapper
    ) {
        this(teamGuideMapper, leaveRecordMapper, teamMapper, guideMapper, null, null);
    }

    /**
     * 查询团队导游安排列表。
     *
     * @param teamId 团队 ID
     * @param tenantId 当前租户 ID
     * @return 团队导游安排列表
     */
    public List<TeamGuideResponse> listTeamGuides(Long teamId, Long tenantId) {
        List<DispatchTeamGuideEntity> guides = teamGuideMapper.selectList(teamGuideQuery(tenantId)
                        .eq("team_id", teamId)
                        .orderByAsc("start_at")
                        .orderByAsc("id"));
        Map<Long, ImprestSummary> summaries = loadGuideImprestSummaries(tenantId, teamId, guides);
        return guides
                .stream()
                .map(entity -> TeamGuideResponse.fromEntity(
                        entity,
                        summaries.getOrDefault(entity.getGuideId(), ImprestSummary.empty())
                ))
                .toList();
    }

    private Map<Long, ImprestSummary> loadGuideImprestSummaries(
            Long tenantId,
            Long teamId,
            List<DispatchTeamGuideEntity> guides
    ) {
        if (imprestMapper == null || guides == null || guides.isEmpty()) {
            return Map.of();
        }
        List<Long> guideIds = guides.stream()
                .map(DispatchTeamGuideEntity::getGuideId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (guideIds.isEmpty()) {
            return Map.of();
        }
        List<FinanceGuideImprestEntity> records = imprestMapper.selectList(new QueryWrapper<FinanceGuideImprestEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .in("guide_id", guideIds));
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        Map<Long, MutableImprestSummary> mutable = new HashMap<>();
        for (FinanceGuideImprestEntity record : records) {
            Long guideId = record.getGuideId();
            if (guideId == null) {
                continue;
            }
            MutableImprestSummary summary = mutable.computeIfAbsent(guideId, ignored -> new MutableImprestSummary());
            summary.add(record);
        }
        Map<Long, ImprestSummary> result = new HashMap<>();
        for (Map.Entry<Long, MutableImprestSummary> entry : mutable.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toResponse());
        }
        return result;
    }

    /**
     * 新增团队导游安排。
     *
     * @param teamId 团队 ID
     * @param request 导游安排字段
     * @param tenantId 当前租户 ID
     * @param operator 操作人
     * @return 新增后的导游安排
     */
    @Transactional
    public TeamGuideResponse createTeamGuide(Long teamId, TeamGuideSaveRequest request, Long tenantId, String operator) {
        validateTimeRange(request.startAt(), request.endAt());
        SalesTeamEntity team = resolveTeam(teamId, tenantId);
        EnterpriseGuideEntity guide = resolveGuide(request.guideId(), tenantId);
        assertNoTeamGuideConflict(guide.getId(), request.startAt(), request.endAt(), tenantId, null);
        assertNoApprovedLeaveConflict(guide.getId(), request.startAt(), request.endAt(), tenantId);

        DispatchTeamGuideEntity entity = new DispatchTeamGuideEntity();
        entity.setTenantId(tenantId);
        entity.setTeamId(team.getId());
        entity.setTeamNo(team.getTeamNo());
        applyGuideSnapshot(entity, guide);
        entity.setGuideFee(money(request.guideFee()));
        entity.setImprestAmount(money(request.imprestAmount()));
        entity.setOperationFee(money(request.operationFee()));
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setFeeMemo(clean(request.feeMemo()));
        entity.setGuideMemo(clean(request.guideMemo()));
        entity.setIsTentative(Boolean.TRUE.equals(request.tentative()));
        entity.setStatus(DispatchTeamGuideStatus.ACTIVE.getValue());
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        teamGuideMapper.insert(entity);
        refreshTeamListSummary(team.getId(), tenantId);
        return TeamGuideResponse.fromEntity(entity);
    }

    /**
     * 按老系统单字段保存模式更新团队导游安排。
     *
     * @param teamId 团队 ID
     * @param recordId 安排记录 ID
     * @param request 字段和值
     * @param tenantId 当前租户 ID
     * @return 更新后的导游安排
     */
    @Transactional
    public TeamGuideResponse updateTeamGuideField(
            Long teamId,
            Long recordId,
            TeamGuideFieldUpdateRequest request,
            Long tenantId
    ) {
        DispatchTeamGuideEntity entity = resolveTeamGuide(recordId, teamId, tenantId);
        applyTeamGuideField(entity, request, tenantId);
        validateTimeRange(entity.getStartAt(), entity.getEndAt());
        assertNoTeamGuideConflict(entity.getGuideId(), entity.getStartAt(), entity.getEndAt(), tenantId, entity.getId());
        assertNoApprovedLeaveConflict(entity.getGuideId(), entity.getStartAt(), entity.getEndAt(), tenantId);
        int updated = teamGuideMapper.update(entity, teamGuideUpdate(tenantId).eq("id", recordId).eq("team_id", teamId));
        if (updated == 0) {
            throw new BizException("导游安排不存在或已删除");
        }
        refreshTeamListSummary(teamId, tenantId);
        return TeamGuideResponse.fromEntity(entity);
    }

    /**
     * 软删除团队导游安排。
     */
    @Transactional
    public void deleteTeamGuide(Long teamId, Long recordId, Long tenantId, String operator) {
        DispatchTeamGuideEntity entity = new DispatchTeamGuideEntity();
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);
        int updated = teamGuideMapper.update(entity, teamGuideUpdate(tenantId).eq("id", recordId).eq("team_id", teamId));
        if (updated == 0) {
            throw new BizException("导游安排不存在或已删除");
        }
        refreshTeamListSummary(teamId, tenantId);
    }

    /**
     * 刷新团队列表缓存中的导游摘要和导游安排状态。
     *
     * <p>同一团队可有多个导游，刷新服务会重新读取该团所有有效导游后覆盖汇总字段，避免单条增量维护残留旧导游。</p>
     */
    private void refreshTeamListSummary(Long teamId, Long tenantId) {
        if (teamListSummaryRefreshService != null) {
            teamListSummaryRefreshService.refresh(teamId, tenantId);
        }
    }

    /**
     * 计调直接设置导游请假，保存后直接生效。
     *
     * @param request 请假内容
     * @param tenantId 当前租户 ID
     * @param operator 操作人
     * @return 请假记录
     */
    @Transactional
    public GuideLeaveResponse directCreateLeave(GuideLeaveSaveRequest request, Long tenantId, String operator) {
        validateTimeRange(request.startAt(), request.endAt());
        EnterpriseGuideEntity guide = resolveGuide(request.guideId(), tenantId);
        assertNoTeamGuideConflict(guide.getId(), request.startAt(), request.endAt(), tenantId, null);

        DispatchGuideLeaveRecordEntity entity = newLeaveEntity(
                request,
                guide,
                tenantId,
                operator,
                GuideLeaveSourceType.DISPATCHER_DIRECT.getValue(),
                GuideLeaveStatus.APPROVED.getValue()
        );
        entity.setApprovedBy(operator);
        entity.setApprovedAt(OffsetDateTime.now());
        leaveRecordMapper.insert(entity);
        return GuideLeaveResponse.fromEntity(entity);
    }

    /**
     * 导游自己提交请假申请。
     *
     * @param request 请假内容
     * @param tenantId 当前租户 ID
     * @param operator 当前导游账号
     * @return 请假记录
     */
    @Transactional
    public GuideLeaveResponse submitLeaveByGuide(GuideLeaveSaveRequest request, Long tenantId, String operator) {
        validateTimeRange(request.startAt(), request.endAt());
        EnterpriseGuideEntity guide = request.guideId() == null
                ? resolveGuideByUsername(operator, tenantId)
                : resolveGuide(request.guideId(), tenantId);
        DispatchGuideLeaveRecordEntity entity = newLeaveEntity(
                request,
                guide,
                tenantId,
                operator,
                GuideLeaveSourceType.GUIDE_APPLY.getValue(),
                GuideLeaveStatus.PENDING.getValue()
        );
        leaveRecordMapper.insert(entity);
        return GuideLeaveResponse.fromEntity(entity);
    }

    /**
     * 审批通过导游请假。通过前必须再次检查团队安排冲突。
     */
    @Transactional
    public GuideLeaveResponse approveLeave(Long leaveId, String approvalRemark, Long tenantId, String operator) {
        DispatchGuideLeaveRecordEntity entity = resolveLeave(leaveId, tenantId);
        assertLeavePending(entity);
        assertNoTeamGuideConflict(entity.getGuideId(), entity.getStartAt(), entity.getEndAt(), tenantId, null);
        entity.setStatus(GuideLeaveStatus.APPROVED.getValue());
        entity.setApprovedBy(operator);
        entity.setApprovedAt(OffsetDateTime.now());
        entity.setApprovalRemark(clean(approvalRemark));
        int updated = leaveRecordMapper.update(entity, leaveUpdate(tenantId).eq("id", leaveId));
        if (updated == 0) {
            throw new BizException("导游请假记录不存在或已删除");
        }
        return GuideLeaveResponse.fromEntity(entity);
    }

    /**
     * 驳回导游请假申请。
     */
    @Transactional
    public GuideLeaveResponse rejectLeave(Long leaveId, String approvalRemark, Long tenantId, String operator) {
        DispatchGuideLeaveRecordEntity entity = resolveLeave(leaveId, tenantId);
        assertLeavePending(entity);
        entity.setStatus(GuideLeaveStatus.REJECTED.getValue());
        entity.setRejectedBy(operator);
        entity.setRejectedAt(OffsetDateTime.now());
        entity.setApprovalRemark(clean(approvalRemark));
        int updated = leaveRecordMapper.update(entity, leaveUpdate(tenantId).eq("id", leaveId));
        if (updated == 0) {
            throw new BizException("导游请假记录不存在或已删除");
        }
        return GuideLeaveResponse.fromEntity(entity);
    }

    /**
     * 撤回待审批请假申请。
     */
    @Transactional
    public GuideLeaveResponse withdrawLeave(Long leaveId, Long tenantId, String operator) {
        DispatchGuideLeaveRecordEntity entity = resolveLeave(leaveId, tenantId);
        assertLeavePending(entity);
        entity.setStatus(GuideLeaveStatus.WITHDRAWN.getValue());
        entity.setWithdrawnBy(operator);
        entity.setWithdrawnAt(OffsetDateTime.now());
        int updated = leaveRecordMapper.update(entity, leaveUpdate(tenantId).eq("id", leaveId));
        if (updated == 0) {
            throw new BizException("导游请假记录不存在或已删除");
        }
        return GuideLeaveResponse.fromEntity(entity);
    }

    /**
     * 分页查询后台导游请假记录。
     */
    public PageResult<GuideLeaveResponse> pageLeaves(
            String guideName,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            long page,
            long pageSize,
            Long tenantId
    ) {
        QueryWrapper<DispatchGuideLeaveRecordEntity> wrapper = leaveQuery(tenantId)
                .like(StringUtils.hasText(guideName), "guide_name", guideName)
                .eq(StringUtils.hasText(status), "status", status)
                .lt(endDate != null, "start_at", endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .gt(startDate != null, "end_at", startDate == null ? null : startDate.atStartOfDay())
                .orderByDesc("created_at")
                .orderByDesc("id");
        Page<DispatchGuideLeaveRecordEntity> result = leaveRecordMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResult<>(result.getRecords().stream().map(GuideLeaveResponse::fromEntity).toList(), result.getTotal());
    }

    /**
     * 查询导游本人请假记录。
     */
    public List<GuideLeaveResponse> myLeaves(Long tenantId, String operator) {
        EnterpriseGuideEntity guide = resolveGuideByUsername(operator, tenantId);
        return leaveRecordMapper.selectList(leaveQuery(tenantId)
                        .eq("guide_id", guide.getId())
                        .orderByDesc("created_at")
                        .orderByDesc("id"))
                .stream()
                .map(GuideLeaveResponse::fromEntity)
                .toList();
    }

    /**
     * 查询指定时间段内导游是否可出团。
     *
     * @param startAt 上团时间
     * @param endAt 下团时间
     * @param keyword 导游姓名、手机号、账号或编码关键字
     * @param availableOnly 是否只返回可用导游
     * @param page 当前页，从 1 开始
     * @param pageSize 每页条数
     * @param tenantId 当前租户 ID
     * @return 导游可用性分页结果
     */
    public PageResult<GuideAvailabilityResponse> guideAvailability(
            LocalDateTime startAt,
            LocalDateTime endAt,
            String keyword,
            boolean availableOnly,
            long page,
            long pageSize,
            Long tenantId
    ) {
        validateTimeRange(startAt, endAt);
        String cleanKeyword = clean(keyword);
        List<EnterpriseGuideEntity> guides = guideMapper.selectList(new QueryWrapper<EnterpriseGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .and(StringUtils.hasText(cleanKeyword), wrapper -> wrapper
                        .like("guide_name", cleanKeyword)
                        .or()
                        .like("mobile_phone", cleanKeyword)
                        .or()
                        .like("telephone", cleanKeyword)
                        .or()
                        .like("guide_code", cleanKeyword)
                        .or()
                        .like("username", cleanKeyword))
                .orderByAsc("guide_name")
                .orderByAsc("id"));
        List<Long> guideIds = guides.stream().map(EnterpriseGuideEntity::getId).toList();
        Map<Long, DispatchTeamGuideEntity> teamConflictMap = new HashMap<>();
        Map<Long, DispatchGuideLeaveRecordEntity> leaveConflictMap = new HashMap<>();
        if (!guideIds.isEmpty()) {
            List<DispatchTeamGuideEntity> teamConflicts = teamGuideMapper.selectList(teamGuideQuery(tenantId)
                    .in("guide_id", guideIds)
                    .eq("status", DispatchTeamGuideStatus.ACTIVE.getValue())
                    .lt("start_at", endAt)
                    .gt("end_at", startAt)
                    .orderByAsc("start_at")
                    .orderByAsc("id"));
            for (DispatchTeamGuideEntity item : teamConflicts) {
                teamConflictMap.putIfAbsent(item.getGuideId(), item);
            }
            List<DispatchGuideLeaveRecordEntity> leaveConflicts = leaveRecordMapper.selectList(leaveQuery(tenantId)
                    .in("guide_id", guideIds)
                    .eq("status", GuideLeaveStatus.APPROVED.getValue())
                    .lt("start_at", endAt)
                    .gt("end_at", startAt)
                    .orderByAsc("start_at")
                    .orderByAsc("id"));
            for (DispatchGuideLeaveRecordEntity item : leaveConflicts) {
                leaveConflictMap.putIfAbsent(item.getGuideId(), item);
            }
        }
        List<GuideAvailabilityResponse> rows = guides.stream()
                .map(guide -> toAvailabilityRow(guide, teamConflictMap.get(guide.getId()), leaveConflictMap.get(guide.getId())))
                .filter(row -> !availableOnly || Boolean.TRUE.equals(row.available()))
                .toList();
        long safePage = Math.max(page, 1L);
        long safePageSize = Math.max(1L, Math.min(pageSize, 200L));
        int fromIndex = (int) Math.min((safePage - 1L) * safePageSize, rows.size());
        int toIndex = (int) Math.min(fromIndex + safePageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), rows.size());
    }

    /**
     * 查询导游排班日历。结果包含团队占用和已通过请假占用。
     */
    public GuideScheduleCalendarResponse calendar(GuideScheduleQuery query, Long tenantId) {
        LocalDate startDate = query.startDate() == null ? LocalDate.now() : query.startDate();
        LocalDate endDate = startDate.plusDays(CALENDAR_DAYS - 1L);
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();
        String guideName = clean(query.guideName());

        List<EnterpriseGuideEntity> guides = guideMapper.selectList(new QueryWrapper<EnterpriseGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .like(StringUtils.hasText(guideName), "guide_name", guideName)
                .orderByAsc("guide_name")
                .orderByAsc("id"));
        List<DispatchTeamGuideEntity> teamGuides = teamGuideMapper.selectList(teamGuideQuery(tenantId)
                .eq("status", DispatchTeamGuideStatus.ACTIVE.getValue())
                .like(StringUtils.hasText(guideName), "guide_name", guideName)
                .lt("start_at", endAt)
                .gt("end_at", startAt)
                .orderByAsc("guide_name")
                .orderByAsc("start_at"));
        List<DispatchGuideLeaveRecordEntity> leaves = leaveRecordMapper.selectList(leaveQuery(tenantId)
                .eq("status", GuideLeaveStatus.APPROVED.getValue())
                .like(StringUtils.hasText(guideName), "guide_name", guideName)
                .lt("start_at", endAt)
                .gt("end_at", startAt)
                .orderByAsc("guide_name")
                .orderByAsc("start_at"));

        Map<Long, MutableGuideRow> rows = new LinkedHashMap<>();
        for (EnterpriseGuideEntity guide : guides) {
            rows.put(guide.getId(), new MutableGuideRow(guide.getId(), guide.getGuideName(), guide.getMobilePhone()));
        }
        for (DispatchTeamGuideEntity item : teamGuides) {
            MutableGuideRow row = rows.computeIfAbsent(item.getGuideId(),
                    key -> new MutableGuideRow(item.getGuideId(), item.getGuideName(), item.getGuideMobile()));
            row.blocks.add(new GuideScheduleCalendarResponse.ScheduleBlock(
                    "team",
                    item.getId(),
                    item.getTeamId(),
                    item.getTeamNo(),
                    item.getGuideId(),
                    item.getTeamNo(),
                    timeLabel(item.getStartAt()) + "上团 " + item.getTeamNo() + " " + timeLabel(item.getEndAt()) + "下团",
                    item.getStartAt(),
                    item.getEndAt(),
                    item.getStatus()
            ));
        }
        for (DispatchGuideLeaveRecordEntity item : leaves) {
            MutableGuideRow row = rows.computeIfAbsent(item.getGuideId(),
                    key -> new MutableGuideRow(item.getGuideId(), item.getGuideName(), item.getGuideMobile()));
            row.blocks.add(new GuideScheduleCalendarResponse.ScheduleBlock(
                    "leave",
                    item.getId(),
                    null,
                    null,
                    item.getGuideId(),
                    "请假",
                    item.getLeaveReason(),
                    item.getStartAt(),
                    item.getEndAt(),
                    item.getStatus()
            ));
        }

        List<GuideScheduleCalendarResponse.GuideRow> guideRows = rows.values().stream()
                .sorted(Comparator.comparing(MutableGuideRow::guideName, Comparator.nullsLast(String::compareTo)))
                .map(row -> new GuideScheduleCalendarResponse.GuideRow(
                        row.guideId,
                        row.guideName,
                        row.guideMobile,
                        row.blocks.stream().sorted(Comparator.comparing(GuideScheduleCalendarResponse.ScheduleBlock::startAt)).toList()
                ))
                .toList();
        return new GuideScheduleCalendarResponse(startDate, endDate, buildDates(startDate), guideRows);
    }

    private GuideAvailabilityResponse toAvailabilityRow(
            EnterpriseGuideEntity guide,
            DispatchTeamGuideEntity teamConflict,
            DispatchGuideLeaveRecordEntity leaveConflict
    ) {
        if (teamConflict != null) {
            return new GuideAvailabilityResponse(
                    guide.getId(),
                    guide.getGuideName(),
                    guide.getMobilePhone(),
                    guide.getGender(),
                    false,
                    "team",
                    "已有团队安排：" + teamConflict.getTeamNo() + " "
                            + dateTimeLabel(teamConflict.getStartAt()) + " 至 " + dateTimeLabel(teamConflict.getEndAt())
            );
        }
        if (leaveConflict != null) {
            String reason = StringUtils.hasText(leaveConflict.getLeaveReason())
                    ? leaveConflict.getLeaveReason()
                    : "请假";
            return new GuideAvailabilityResponse(
                    guide.getId(),
                    guide.getGuideName(),
                    guide.getMobilePhone(),
                    guide.getGender(),
                    false,
                    "leave",
                    "请假：" + reason + " "
                            + dateTimeLabel(leaveConflict.getStartAt()) + " 至 " + dateTimeLabel(leaveConflict.getEndAt())
            );
        }
        return new GuideAvailabilityResponse(
                guide.getId(),
                guide.getGuideName(),
                guide.getMobilePhone(),
                guide.getGender(),
                true,
                null,
                null
        );
    }

    private List<GuideScheduleCalendarResponse.ScheduleDate> buildDates(LocalDate startDate) {
        List<GuideScheduleCalendarResponse.ScheduleDate> dates = new ArrayList<>();
        String[] weeks = {"日", "一", "二", "三", "四", "五", "六"};
        for (int i = 0; i < CALENDAR_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            dates.add(new GuideScheduleCalendarResponse.ScheduleDate(
                    date,
                    date.format(DateTimeFormatter.ofPattern("MM.dd", Locale.CHINA)),
                    weeks[date.getDayOfWeek().getValue() % 7]
            ));
        }
        return dates;
    }

    private DispatchGuideLeaveRecordEntity newLeaveEntity(
            GuideLeaveSaveRequest request,
            EnterpriseGuideEntity guide,
            Long tenantId,
            String operator,
            String sourceType,
            String status
    ) {
        DispatchGuideLeaveRecordEntity entity = new DispatchGuideLeaveRecordEntity();
        entity.setTenantId(tenantId);
        entity.setGuideId(guide.getId());
        entity.setGuideName(guide.getGuideName());
        entity.setGuideMobile(guide.getMobilePhone());
        entity.setSourceType(sourceType);
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setLeaveReason(clean(request.leaveReason()));
        entity.setStatus(status);
        entity.setApplicant(operator);
        entity.setAppliedAt(OffsetDateTime.now());
        entity.setCreatedBy(operator);
        entity.setRemark(clean(request.remark()));
        entity.setIsDeleted(false);
        return entity;
    }

    private void applyTeamGuideField(DispatchTeamGuideEntity entity, TeamGuideFieldUpdateRequest request, Long tenantId) {
        String field = request.field();
        String value = request.value();
        switch (field) {
            case "guideId" -> applyGuideSnapshot(entity, resolveGuide(parseLong(value, "请选择导游"), tenantId));
            case "guideFee" -> entity.setGuideFee(parseMoney(value));
            case "imprestAmount" -> entity.setImprestAmount(parseMoney(value));
            case "operationFee" -> entity.setOperationFee(parseMoney(value));
            case "startAt" -> entity.setStartAt(parseDateTime(value, "上团时间格式不正确"));
            case "endAt" -> entity.setEndAt(parseDateTime(value, "下团时间格式不正确"));
            case "feeMemo" -> entity.setFeeMemo(clean(value));
            case "guideMemo" -> entity.setGuideMemo(clean(value));
            case "tentative" -> entity.setIsTentative(Boolean.parseBoolean(value));
            default -> throw new BizException("不支持修改该导游安排字段");
        }
    }

    private void applyGuideSnapshot(DispatchTeamGuideEntity entity, EnterpriseGuideEntity guide) {
        entity.setGuideId(guide.getId());
        entity.setGuideName(guide.getGuideName());
        entity.setGuideMobile(guide.getMobilePhone());
    }

    private void assertNoTeamGuideConflict(
            Long guideId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long tenantId,
            Long excludeRecordId
    ) {
        List<DispatchTeamGuideEntity> conflicts = teamGuideMapper.selectList(teamGuideQuery(tenantId)
                .eq("guide_id", guideId)
                .eq("status", DispatchTeamGuideStatus.ACTIVE.getValue())
                .ne(excludeRecordId != null, "id", excludeRecordId)
                .lt("start_at", endAt)
                .gt("end_at", startAt));
        if (!conflicts.isEmpty()) {
            DispatchTeamGuideEntity conflict = conflicts.get(0);
            throw new BizException("导游已有团队安排冲突：" + conflict.getTeamNo() + " "
                    + conflict.getStartAt() + " 至 " + conflict.getEndAt());
        }
    }

    private void assertNoApprovedLeaveConflict(Long guideId, LocalDateTime startAt, LocalDateTime endAt, Long tenantId) {
        List<DispatchGuideLeaveRecordEntity> conflicts = leaveRecordMapper.selectList(leaveQuery(tenantId)
                .eq("guide_id", guideId)
                .eq("status", GuideLeaveStatus.APPROVED.getValue())
                .lt("start_at", endAt)
                .gt("end_at", startAt));
        if (!conflicts.isEmpty()) {
            DispatchGuideLeaveRecordEntity conflict = conflicts.get(0);
            throw new BizException("导游请假冲突：" + conflict.getGuideName() + " "
                    + conflict.getStartAt() + " 至 " + conflict.getEndAt());
        }
    }

    private SalesTeamEntity resolveTeam(Long teamId, Long tenantId) {
        SalesTeamEntity team = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", teamId));
        if (team == null) {
            throw new BizException("团队不存在或已删除");
        }
        return team;
    }

    private EnterpriseGuideEntity resolveGuide(Long guideId, Long tenantId) {
        EnterpriseGuideEntity guide = guideMapper.selectOne(new QueryWrapper<EnterpriseGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("id", guideId));
        if (guide == null) {
            throw new BizException("导游不存在、已删除或已停用");
        }
        return guide;
    }

    private EnterpriseGuideEntity resolveGuideByUsername(String username, Long tenantId) {
        EnterpriseGuideEntity guide = guideMapper.selectOne(new QueryWrapper<EnterpriseGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("username", username));
        if (guide == null) {
            throw new BizException("当前账号未绑定启用导游档案");
        }
        return guide;
    }

    private DispatchTeamGuideEntity resolveTeamGuide(Long recordId, Long teamId, Long tenantId) {
        DispatchTeamGuideEntity entity = teamGuideMapper.selectOne(teamGuideQuery(tenantId)
                .eq("id", recordId)
                .eq("team_id", teamId));
        if (entity == null) {
            throw new BizException("导游安排不存在或已删除");
        }
        return entity;
    }

    private DispatchGuideLeaveRecordEntity resolveLeave(Long leaveId, Long tenantId) {
        DispatchGuideLeaveRecordEntity entity = leaveRecordMapper.selectOne(leaveQuery(tenantId).eq("id", leaveId));
        if (entity == null) {
            throw new BizException("导游请假记录不存在或已删除");
        }
        return entity;
    }

    private void assertLeavePending(DispatchGuideLeaveRecordEntity entity) {
        if (!GuideLeaveStatus.PENDING.getValue().equals(entity.getStatus())) {
            throw new BizException("只有待审批请假可以执行该操作");
        }
    }

    private QueryWrapper<DispatchTeamGuideEntity> teamGuideQuery(Long tenantId) {
        return new QueryWrapper<DispatchTeamGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<DispatchGuideLeaveRecordEntity> leaveQuery(Long tenantId) {
        return new QueryWrapper<DispatchGuideLeaveRecordEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<DispatchTeamGuideEntity> teamGuideUpdate(Long tenantId) {
        return new UpdateWrapper<DispatchTeamGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<DispatchGuideLeaveRecordEntity> leaveUpdate(Long tenantId) {
        return new UpdateWrapper<DispatchGuideLeaveRecordEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private void validateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new BizException("开始时间和结束时间不能为空");
        }
        if (!endAt.isAfter(startAt)) {
            throw new BizException("结束时间必须晚于开始时间");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal parseMoney(String value) {
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal result = new BigDecimal(value.trim());
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("金额不能小于0");
            }
            return result;
        } catch (NumberFormatException ex) {
            throw new BizException("金额请填写数字");
        }
    }

    private Long parseLong(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(message);
        }
    }

    private LocalDateTime parseDateTime(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (RuntimeException ex) {
            throw new BizException(message);
        }
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String timeLabel(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String dateTimeLabel(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /** 团队导游列表中的备用金申请汇总，按“累计已审批金额”展示真实备用金口径。 */
    private class MutableImprestSummary {
        private BigDecimal approvedAmount = BigDecimal.ZERO.setScale(2);
        private BigDecimal pendingAmount = BigDecimal.ZERO.setScale(2);
        private BigDecimal paidAmount = BigDecimal.ZERO.setScale(2);
        private BigDecimal balanceAmount = BigDecimal.ZERO.setScale(2);
        private boolean hasPending;
        private boolean hasApprovedUnpaid;
        private boolean hasPartialPaid;
        private boolean hasPaid;

        private void add(FinanceGuideImprestEntity record) {
            String status = record.getStatus();
            if (GuideImprestStatus.PENDING_MANAGER.value().equals(status)) {
                pendingAmount = pendingAmount.add(money(record.getRequestedAmount()));
                hasPending = true;
                return;
            }
            if (!isApprovedStatus(status)) {
                return;
            }
            BigDecimal approved = approvedAmount(record);
            BigDecimal paid = money(record.getPaidAmount());
            BigDecimal balance = money(record.getBalanceAmount());
            approvedAmount = approvedAmount.add(approved);
            paidAmount = paidAmount.add(paid);
            balanceAmount = balanceAmount.add(balance);
            if (paid.compareTo(BigDecimal.ZERO) > 0 && balance.compareTo(BigDecimal.ZERO) > 0) {
                hasPartialPaid = true;
            } else if (balance.compareTo(BigDecimal.ZERO) > 0) {
                hasApprovedUnpaid = true;
            } else if (approved.compareTo(BigDecimal.ZERO) > 0) {
                hasPaid = true;
            }
        }

        private boolean isApprovedStatus(String status) {
            return GuideImprestStatus.MANAGER_APPROVED.value().equals(status)
                    || GuideImprestStatus.PAID.value().equals(status)
                    || GuideImprestStatus.SETTLED.value().equals(status);
        }

        private BigDecimal approvedAmount(FinanceGuideImprestEntity record) {
            BigDecimal approved = money(record.getApprovedAmount());
            return approved.compareTo(BigDecimal.ZERO) > 0 ? approved : money(record.getRequestedAmount());
        }

        private ImprestSummary toResponse() {
            return new ImprestSummary(
                    money(approvedAmount),
                    money(pendingAmount),
                    money(paidAmount),
                    money(balanceAmount),
                    status()
            );
        }

        private String status() {
            if (hasPending) {
                return "pending";
            }
            if (hasPartialPaid) {
                return "partial_paid";
            }
            if (hasApprovedUnpaid) {
                return "approved_unpaid";
            }
            if (hasPaid) {
                return "paid";
            }
            return "none";
        }
    }

    private record MutableGuideRow(Long guideId, String guideName, String guideMobile, List<GuideScheduleCalendarResponse.ScheduleBlock> blocks) {
        private MutableGuideRow(Long guideId, String guideName, String guideMobile) {
            this(guideId, guideName, guideMobile, new ArrayList<>());
        }
    }
}
