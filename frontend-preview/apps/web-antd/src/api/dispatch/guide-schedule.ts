import { requestClient } from '#/api/request';

export namespace DispatchGuideApi {
  export type LeaveSourceType = 'dispatcher_direct' | 'guide_apply';
  export type LeaveStatus =
    | 'approved'
    | 'cancelled'
    | 'pending'
    | 'rejected'
    | 'withdrawn';
  export type ScheduleSourceType = 'leave' | 'team';
  export type TeamGuideStatus = 'active' | 'cancelled';

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface TeamGuide {
    createdAt?: string;
    endAt: string;
    feeMemo?: string;
    guideFee: number;
    guideId: number;
    guideMemo?: string;
    guideMobile?: string;
    guideName: string;
    id: number;
    imprestAmount: number;
    operationFee: number;
    startAt: string;
    status: TeamGuideStatus;
    teamId: number;
    teamNo: string;
    tentative: boolean;
  }

  export interface TeamGuideSaveParams {
    endAt: string;
    feeMemo?: string;
    guideFee?: number;
    guideId: number;
    guideMemo?: string;
    imprestAmount?: number;
    operationFee?: number;
    startAt: string;
    tentative?: boolean;
  }

  export interface TeamGuideFieldUpdateParams {
    field: string;
    value?: string;
  }

  export interface LeaveRecord {
    applicant?: string;
    appliedAt?: string;
    approvalRemark?: string;
    approvedAt?: string;
    approvedBy?: string;
    endAt: string;
    guideId: number;
    guideMobile?: string;
    guideName: string;
    id: number;
    leaveReason: string;
    rejectedAt?: string;
    rejectedBy?: string;
    remark?: string;
    sourceType: LeaveSourceType;
    startAt: string;
    status: LeaveStatus;
  }

  export interface LeaveSaveParams {
    endAt: string;
    guideId: number;
    leaveReason: string;
    remark?: string;
    startAt: string;
  }

  export interface SelfLeaveSaveParams {
    endAt: string;
    leaveReason: string;
    remark?: string;
    startAt: string;
  }

  export interface LeaveQueryParams {
    endDate?: string;
    guideName?: string;
    page?: number;
    pageSize?: number;
    startDate?: string;
    status?: LeaveStatus;
  }

  export interface ScheduleDate {
    date: string;
    label: string;
    weekLabel: string;
  }

  export interface ScheduleBlock {
    description?: string;
    endAt: string;
    guideId: number;
    sourceId: number;
    sourceType: ScheduleSourceType;
    startAt: string;
    status: string;
    teamId?: number;
    teamNo?: string;
    title: string;
  }

  export interface GuideRow {
    blocks: ScheduleBlock[];
    guideId: number;
    guideMobile?: string;
    guideName: string;
  }

  export interface CalendarResponse {
    dates: ScheduleDate[];
    endDate: string;
    rows: GuideRow[];
    startDate: string;
  }
}

/** 查询团队导游安排。 */
export function getTeamGuides(teamId: number) {
  return requestClient.get<DispatchGuideApi.TeamGuide[]>(
    `/sales/team/${teamId}/guides`,
  );
}

/** 新增团队导游安排。 */
export function createTeamGuide(
  teamId: number,
  data: DispatchGuideApi.TeamGuideSaveParams,
) {
  return requestClient.post<DispatchGuideApi.TeamGuide>(
    `/sales/team/${teamId}/guides/create`,
    data,
  );
}

/** 单字段保存团队导游安排。 */
export function updateTeamGuideField(
  teamId: number,
  recordId: number,
  data: DispatchGuideApi.TeamGuideFieldUpdateParams,
) {
  return requestClient.post<DispatchGuideApi.TeamGuide>(
    `/sales/team/${teamId}/guides/${recordId}/field`,
    data,
  );
}

/** 删除团队导游安排。 */
export function deleteTeamGuide(teamId: number, recordId: number) {
  return requestClient.post<void>(
    `/sales/team/${teamId}/guides/${recordId}/delete`,
    {},
  );
}

/** 查询导游排班日历。 */
export function getGuideScheduleCalendar(params: {
  guideName?: string;
  startDate?: string;
}) {
  return requestClient.get<DispatchGuideApi.CalendarResponse>(
    '/dispatch/guide-schedule/calendar',
    { params },
  );
}

/** 分页查询导游请假。 */
export function getGuideLeavePage(params: DispatchGuideApi.LeaveQueryParams) {
  return requestClient.get<
    DispatchGuideApi.PageResult<DispatchGuideApi.LeaveRecord>
  >('/dispatch/guide-leaves/page', { params });
}

/** 计调直接设置导游请假。 */
export function createGuideLeaveDirect(
  data: DispatchGuideApi.LeaveSaveParams,
) {
  return requestClient.post<DispatchGuideApi.LeaveRecord>(
    '/dispatch/guide-leaves/direct',
    data,
  );
}

/** 审批通过导游请假。 */
export function approveGuideLeave(id: number, approvalRemark?: string) {
  return requestClient.post<DispatchGuideApi.LeaveRecord>(
    `/dispatch/guide-leaves/${id}/approve`,
    { approvalRemark },
  );
}

/** 驳回导游请假。 */
export function rejectGuideLeave(id: number, approvalRemark?: string) {
  return requestClient.post<DispatchGuideApi.LeaveRecord>(
    `/dispatch/guide-leaves/${id}/reject`,
    { approvalRemark },
  );
}

/** 查询导游本人的请假。 */
export function getMyGuideLeaves() {
  return requestClient.get<DispatchGuideApi.LeaveRecord[]>('/guide/my-leave');
}

/** 导游本人提交请假申请。 */
export function submitMyGuideLeave(data: DispatchGuideApi.SelfLeaveSaveParams) {
  return requestClient.post<DispatchGuideApi.LeaveRecord>(
    '/guide/my-leave',
    data,
  );
}

/** 导游本人撤回待审批请假。 */
export function withdrawMyGuideLeave(id: number) {
  return requestClient.post<DispatchGuideApi.LeaveRecord>(
    `/guide/my-leave/${id}/withdraw`,
    {},
  );
}
