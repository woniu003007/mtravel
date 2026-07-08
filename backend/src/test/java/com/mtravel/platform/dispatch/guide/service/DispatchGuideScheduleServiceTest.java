package com.mtravel.platform.dispatch.guide.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.dispatch.guide.dto.GuideAvailabilityResponse;
import com.mtravel.platform.dispatch.guide.dto.GuideLeaveSaveRequest;
import com.mtravel.platform.dispatch.guide.dto.GuideScheduleCalendarResponse;
import com.mtravel.platform.dispatch.guide.dto.GuideScheduleQuery;
import com.mtravel.platform.dispatch.guide.dto.TeamGuideSaveRequest;
import com.mtravel.platform.dispatch.guide.entity.DispatchGuideLeaveRecordEntity;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.mapper.DispatchGuideLeaveRecordMapper;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideEntity;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideMapper;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestEntity;
import com.mtravel.platform.finance.guideimprest.mapper.FinanceGuideImprestMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

class DispatchGuideScheduleServiceTest {

    @Test
    void createTeamGuideShouldRejectApprovedLeaveConflict() {
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchGuideLeaveRecordMapper leaveMapper = mock(DispatchGuideLeaveRecordMapper.class);
        DispatchGuideScheduleService service = service(teamGuideMapper, leaveMapper);
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 1, 8, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 3, 18, 0);

        when(teamGuideMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(leaveMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                leave(31L, 12L, LocalDateTime.of(2026, 7, 2, 0, 0), LocalDateTime.of(2026, 7, 2, 23, 59), "approved")
        ));

        assertThatThrownBy(() -> service.createTeamGuide(21L, teamGuideRequest(12L, startAt, endAt), 1L, "dispatcher"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("导游请假");

        verify(teamGuideMapper, never()).insert(any(DispatchTeamGuideEntity.class));
    }

    @Test
    void approveLeaveShouldRejectExistingTeamGuideConflict() {
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchGuideLeaveRecordMapper leaveMapper = mock(DispatchGuideLeaveRecordMapper.class);
        DispatchGuideScheduleService service = service(teamGuideMapper, leaveMapper);
        DispatchGuideLeaveRecordEntity pending = leave(
                31L,
                12L,
                LocalDateTime.of(2026, 7, 2, 9, 0),
                LocalDateTime.of(2026, 7, 2, 18, 0),
                "pending"
        );

        when(leaveMapper.selectOne(any(Wrapper.class))).thenReturn(pending);
        when(teamGuideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamGuide(41L, 21L, 12L, "CS-SP-BK-260701A",
                        LocalDateTime.of(2026, 7, 1, 8, 0),
                        LocalDateTime.of(2026, 7, 3, 18, 0))
        ));

        assertThatThrownBy(() -> service.approveLeave(31L, "同意", 1L, "dispatcher"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("CS-SP-BK-260701A");

        verify(leaveMapper, never()).update(any(DispatchGuideLeaveRecordEntity.class), any(Wrapper.class));
    }

    @Test
    void directCreateLeaveShouldPersistApprovedLeaveWhenNoTeamConflict() {
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchGuideLeaveRecordMapper leaveMapper = mock(DispatchGuideLeaveRecordMapper.class);
        DispatchGuideScheduleService service = service(teamGuideMapper, leaveMapper);
        ArgumentCaptor<DispatchGuideLeaveRecordEntity> captor =
                ArgumentCaptor.forClass(DispatchGuideLeaveRecordEntity.class);

        when(teamGuideMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.directCreateLeave(
                new GuideLeaveSaveRequest(
                        12L,
                        LocalDateTime.of(2026, 7, 6, 9, 0),
                        LocalDateTime.of(2026, 7, 6, 18, 0),
                        "家中有事",
                        "计调直接设置"
                ),
                1L,
                "dispatcher"
        );

        verify(leaveMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("approved");
        assertThat(captor.getValue().getSourceType()).isEqualTo("dispatcher_direct");
        assertThat(captor.getValue().getApprovedBy()).isEqualTo("dispatcher");
    }

    @Test
    void calendarShouldCombineTeamGuideAndApprovedLeaveBlocks() {
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchGuideLeaveRecordMapper leaveMapper = mock(DispatchGuideLeaveRecordMapper.class);
        DispatchGuideScheduleService service = service(teamGuideMapper, leaveMapper);

        when(teamGuideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamGuide(41L, 21L, 12L, "CS-SP-BK-260701A",
                        LocalDateTime.of(2026, 7, 1, 8, 0),
                        LocalDateTime.of(2026, 7, 3, 18, 0))
        ));
        when(leaveMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                leave(31L, 12L, LocalDateTime.of(2026, 7, 5, 9, 0), LocalDateTime.of(2026, 7, 5, 18, 0), "approved")
        ));

        GuideScheduleCalendarResponse response =
                service.calendar(new GuideScheduleQuery("陈导", LocalDate.of(2026, 7, 1)), 1L);

        assertThat(response.rows()).hasSize(1);
        assertThat(response.rows().get(0).guideId()).isEqualTo(12L);
        assertThat(response.rows().get(0).blocks())
                .extracting(GuideScheduleCalendarResponse.ScheduleBlock::sourceType)
                .containsExactlyInAnyOrder("team", "leave");
    }

    @Test
    void calendarShouldShowActiveGuidesWithoutScheduleBlocks() {
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchGuideLeaveRecordMapper leaveMapper = mock(DispatchGuideLeaveRecordMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        EnterpriseGuideMapper guideMapper = mock(EnterpriseGuideMapper.class);
        DispatchGuideScheduleService service =
                new DispatchGuideScheduleService(teamGuideMapper, leaveMapper, teamMapper, guideMapper);

        when(guideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                guide(12L, "陈导", "13900000000"),
                guide(13L, "李导", "13900000001")
        ));
        when(teamGuideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamGuide(41L, 21L, 12L, "CS-SP-BK-260701A",
                        LocalDateTime.of(2026, 7, 1, 8, 0),
                        LocalDateTime.of(2026, 7, 3, 18, 0))
        ));
        when(leaveMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        GuideScheduleCalendarResponse response =
                service.calendar(new GuideScheduleQuery(null, LocalDate.of(2026, 7, 1)), 1L);

        assertThat(response.rows()).hasSize(2);
        assertThat(response.rows())
                .extracting(GuideScheduleCalendarResponse.GuideRow::guideName)
                .containsExactly("李导", "陈导");
        assertThat(response.rows().stream()
                .filter(row -> row.guideId().equals(13L))
                .findFirst()
                .orElseThrow()
                .blocks()).isEmpty();
    }

    @Test
    void guideAvailabilityShouldReturnUnavailableReasonsForTeamAndLeaveConflicts() {
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchGuideLeaveRecordMapper leaveMapper = mock(DispatchGuideLeaveRecordMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        EnterpriseGuideMapper guideMapper = mock(EnterpriseGuideMapper.class);
        DispatchGuideScheduleService service =
                new DispatchGuideScheduleService(teamGuideMapper, leaveMapper, teamMapper, guideMapper);
        LocalDateTime startAt = LocalDateTime.of(2026, 8, 15, 8, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 8, 19, 18, 0);

        when(guideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                guide(12L, "李晓琴", "13957100002"),
                guide(13L, "王建国", "13857100001"),
                guide(14L, "测试导游", "13900000001")
        ));
        when(teamGuideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamGuide(41L, 21L, 13L, "CS-SP-BK-260815A",
                        LocalDateTime.of(2026, 8, 16, 8, 0),
                        LocalDateTime.of(2026, 8, 18, 18, 0))
        ));
        when(leaveMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                leave(31L, 12L, LocalDateTime.of(2026, 8, 17, 9, 0),
                        LocalDateTime.of(2026, 8, 17, 18, 0), "approved")
        ));

        PageResult<GuideAvailabilityResponse> result =
                service.guideAvailability(startAt, endAt, null, false, 1, 20, 1L);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.items()).extracting(GuideAvailabilityResponse::guideName)
                .containsExactly("李晓琴", "王建国", "测试导游");
        assertThat(result.items().get(0).available()).isFalse();
        assertThat(result.items().get(0).unavailableType()).isEqualTo("leave");
        assertThat(result.items().get(0).unavailableReason()).contains("请假").contains("2026-08-17");
        assertThat(result.items().get(1).available()).isFalse();
        assertThat(result.items().get(1).unavailableType()).isEqualTo("team");
        assertThat(result.items().get(1).unavailableReason()).contains("CS-SP-BK-260815A");
        assertThat(result.items().get(2).available()).isTrue();
    }

    @Test
    void guideAvailabilityShouldFilterUnavailableGuidesWhenAvailableOnly() {
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchGuideLeaveRecordMapper leaveMapper = mock(DispatchGuideLeaveRecordMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        EnterpriseGuideMapper guideMapper = mock(EnterpriseGuideMapper.class);
        DispatchGuideScheduleService service =
                new DispatchGuideScheduleService(teamGuideMapper, leaveMapper, teamMapper, guideMapper);
        LocalDateTime startAt = LocalDateTime.of(2026, 8, 15, 8, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 8, 19, 18, 0);

        when(guideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                guide(12L, "李晓琴", "13957100002"),
                guide(13L, "王建国", "13857100001")
        ));
        when(teamGuideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamGuide(41L, 21L, 13L, "CS-SP-BK-260815A",
                        LocalDateTime.of(2026, 8, 16, 8, 0),
                        LocalDateTime.of(2026, 8, 18, 18, 0))
        ));
        when(leaveMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        PageResult<GuideAvailabilityResponse> result =
                service.guideAvailability(startAt, endAt, null, true, 1, 20, 1L);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).extracting(GuideAvailabilityResponse::guideName)
                .containsExactly("李晓琴");
        assertThat(result.items().get(0).available()).isTrue();
    }

    @Test
    void listTeamGuidesShouldReturnApprovedImprestSummaryAndStatus() {
        DispatchTeamGuideMapper teamGuideMapper = mock(DispatchTeamGuideMapper.class);
        DispatchGuideLeaveRecordMapper leaveMapper = mock(DispatchGuideLeaveRecordMapper.class);
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        EnterpriseGuideMapper guideMapper = mock(EnterpriseGuideMapper.class);
        FinanceGuideImprestMapper imprestMapper = mock(FinanceGuideImprestMapper.class);
        DispatchGuideScheduleService service =
                new DispatchGuideScheduleService(teamGuideMapper, leaveMapper, teamMapper, guideMapper, imprestMapper);
        when(teamGuideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                teamGuide(41L, 21L, 12L, "CS-SP-BK-260701A",
                        LocalDateTime.of(2026, 7, 1, 8, 0),
                        LocalDateTime.of(2026, 7, 3, 18, 0))
        ));
        when(imprestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                imprest(12L, "pending_manager", "300.00", "0.00", "0.00", "0.00"),
                imprest(12L, "manager_approved", "500.00", "500.00", "0.00", "500.00"),
                imprest(12L, "paid", "200.00", "200.00", "200.00", "0.00"),
                imprest(12L, "cancelled", "900.00", "0.00", "0.00", "0.00")
        ));

        var result = service.listTeamGuides(21L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).approvedImprestAmount()).isEqualByComparingTo("700.00");
        assertThat(result.get(0).pendingImprestAmount()).isEqualByComparingTo("300.00");
        assertThat(result.get(0).paidImprestAmount()).isEqualByComparingTo("200.00");
        assertThat(result.get(0).imprestBalanceAmount()).isEqualByComparingTo("500.00");
        assertThat(result.get(0).imprestApprovalStatus()).isEqualTo("pending");
    }

    private DispatchGuideScheduleService service(
            DispatchTeamGuideMapper teamGuideMapper,
            DispatchGuideLeaveRecordMapper leaveMapper
    ) {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        EnterpriseGuideMapper guideMapper = mock(EnterpriseGuideMapper.class);
        SalesTeamEntity team = new SalesTeamEntity();
        team.setId(21L);
        team.setTenantId(1L);
        team.setTeamNo("CS-SP-BK-260701A");
        team.setIsDeleted(false);
        EnterpriseGuideEntity guide = new EnterpriseGuideEntity();
        guide.setId(12L);
        guide.setTenantId(1L);
        guide.setGuideName("陈导");
        guide.setMobilePhone("13900000000");
        guide.setStatus("active");
        guide.setIsDeleted(false);

        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team);
        when(guideMapper.selectOne(any(Wrapper.class))).thenReturn(guide);
        when(guideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(guide));
        return new DispatchGuideScheduleService(teamGuideMapper, leaveMapper, teamMapper, guideMapper);
    }

    private TeamGuideSaveRequest teamGuideRequest(Long guideId, LocalDateTime startAt, LocalDateTime endAt) {
        return new TeamGuideSaveRequest(
                guideId,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(200),
                BigDecimal.ZERO,
                startAt,
                endAt,
                "费用说明",
                "导游备注",
                false
        );
    }

    private DispatchTeamGuideEntity teamGuide(
            Long id,
            Long teamId,
            Long guideId,
            String teamNo,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        DispatchTeamGuideEntity entity = new DispatchTeamGuideEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setTeamId(teamId);
        entity.setTeamNo(teamNo);
        entity.setGuideId(guideId);
        entity.setGuideName("陈导");
        entity.setGuideMobile("13900000000");
        entity.setStartAt(startAt);
        entity.setEndAt(endAt);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private FinanceGuideImprestEntity imprest(
            Long guideId,
            String status,
            String requestedAmount,
            String approvedAmount,
            String paidAmount,
            String balanceAmount
    ) {
        FinanceGuideImprestEntity entity = new FinanceGuideImprestEntity();
        entity.setTenantId(1L);
        entity.setTeamId(21L);
        entity.setGuideId(guideId);
        entity.setStatus(status);
        entity.setRequestedAmount(new BigDecimal(requestedAmount));
        entity.setApprovedAmount(new BigDecimal(approvedAmount));
        entity.setPaidAmount(new BigDecimal(paidAmount));
        entity.setBalanceAmount(new BigDecimal(balanceAmount));
        entity.setIsDeleted(false);
        return entity;
    }

    private DispatchGuideLeaveRecordEntity leave(
            Long id,
            Long guideId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String status
    ) {
        DispatchGuideLeaveRecordEntity entity = new DispatchGuideLeaveRecordEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setGuideId(guideId);
        entity.setGuideName("陈导");
        entity.setGuideMobile("13900000000");
        entity.setStartAt(startAt);
        entity.setEndAt(endAt);
        entity.setLeaveReason("家中有事");
        entity.setSourceType("guide_apply");
        entity.setStatus(status);
        entity.setIsDeleted(false);
        return entity;
    }

    private EnterpriseGuideEntity guide(Long guideId, String guideName, String mobilePhone) {
        EnterpriseGuideEntity entity = new EnterpriseGuideEntity();
        entity.setId(guideId);
        entity.setTenantId(1L);
        entity.setGuideName(guideName);
        entity.setMobilePhone(mobilePhone);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }
}
