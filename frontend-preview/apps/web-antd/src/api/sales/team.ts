import { requestClient } from '#/api/request';

export namespace SalesTeamApi {
  export type TeamStatus = 'cancelled' | 'normal' | 'stopped';
  export type TeamType = 'sanpin' | 'santuan' | 'single' | 'zhengtuan';
  export type StatusAction = 'cancel' | 'delete' | 'recover' | 'start' | 'stop';

  export interface PriceItem {
    adultPrice: number;
    childNoBedPrice: number;
    childPrice: number;
    customerCategoryId?: number;
    customerCategoryName: string;
    extraFee: number;
    id: number;
    remark?: string;
    seniorPrice: number;
    status: 'active' | 'disabled';
    teamId: number;
  }

  export interface Item {
    businessType?: string;
    closeDaysBefore: number;
    createdAt?: string;
    createdBy?: string;
    departmentId?: number;
    departmentName?: string;
    departureDate: string;
    escortEmployeeId?: number;
    escortEmployeeName?: string;
    id: number;
    operatorEmployeeId?: number;
    operatorEmployeeName?: string;
    prices: PriceItem[];
    productId: number;
    remainingSeats: number;
    remark?: string;
    singleRoomDifference: number;
    status: TeamStatus;
    teamNo: string;
    teamType: TeamType;
    totalSeats: number;
    updatedAt?: string;
    usedSeats: number;
  }

  export interface ListItem {
    businessType?: string;
    createdAt?: string;
    customerSummary?: string;
    departureDate?: string;
    departurePlace?: string;
    endDate?: string;
    groundAgentPlan?: ArrangeStatus;
    guidePlan?: ArrangeStatus;
    guideSummary?: string;
    hotelPlan?: ArrangeStatus;
    id: number;
    mealPlan?: ArrangeStatus;
    operatorEmployeeName?: string;
    optionalPlan?: ArrangeStatus;
    otherPlan?: ArrangeStatus;
    productId?: number;
    productName?: string;
    progress?: TeamProgress;
    remainingSeats?: number;
    remark?: string;
    scenicPlan?: ArrangeStatus;
    shoppingPlan?: ArrangeStatus;
    status?: TeamStatus;
    teamNo: string;
    teamType: TeamType;
    totalSeats?: number;
    trafficPlan?: ArrangeStatus;
    travelDays?: number;
    usedSeats?: number;
    vehiclePlan?: ArrangeStatus;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    endDate?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
    productId: number;
    startDate?: string;
    status?: TeamStatus;
  }

  export type ArrangeStatus = 'confirmed' | 'none' | 'pending' | 'warning';
  export type DateTeamStatus = 'all' | 'cancelled' | 'departed' | 'not_departed' | 'stopped';
  export type TeamProgress = 'cancelled' | 'closed' | 'departed' | 'done' | 'not_departed' | 'receiving' | 'stopped';

  export interface PageQueryParams {
    addDate?: string;
    businessType?: string;
    customerKeyword?: string;
    departurePlace?: string;
    endDate?: string;
    guideKeyword?: string;
    keyword?: string;
    operatorKeyword?: string;
    orderStatus?: 'cancelled' | 'confirmed' | 'none' | 'pending';
    page?: number;
    pageSize?: number;
    startDate?: string;
    teamStatus?: DateTeamStatus;
    teamType?: TeamType;
    travelDays?: number;
  }

  export interface BatchCreateParams {
    adultPrice?: number;
    childNoBedPrice?: number;
    childPrice?: number;
    customerCategoryId?: number;
    customerCategoryName?: string;
    dates?: string[];
    endDate: string;
    extraFee?: number;
    operatorEmployeeId?: number;
    operatorEmployeeName?: string;
    seniorPrice?: number;
    singleRoomDifference?: number;
    startDate: string;
    totalSeats?: number;
    weekdays?: number[];
  }

  export interface TeamSaveParams {
    businessType?: string;
    departmentId?: number;
    departmentName?: string;
    departureDate?: string;
    escortEmployeeId?: number;
    escortEmployeeName?: string;
    operatorEmployeeId?: number;
    operatorEmployeeName?: string;
    remark?: string;
    singleRoomDifference?: number;
    teamType?: TeamType;
    totalSeats?: number;
  }

  export interface PriceSaveParams {
    adultPrice?: number;
    childNoBedPrice?: number;
    childPrice?: number;
    customerCategoryId?: number;
    customerCategoryName?: string;
    extraFee?: number;
    seniorPrice?: number;
  }

  export interface BatchEditCustomerCategory {
    id?: number;
    name: string;
  }

  export interface BatchEditParams {
    adultPrice?: number;
    childNoBedPrice?: number;
    childPrice?: number;
    customerCategories?: BatchEditCustomerCategory[];
    deletePrice?: boolean;
    extraFee?: number;
    seniorPrice?: number;
    singleRoomDifference?: number;
    synchronizeProduct?: boolean;
    synchronizeProductWithoutSingleRoom?: boolean;
    teamIds: number[];
    totalSeats?: number;
    updateSingleRoomDifference?: boolean;
    updateTotalSeats?: boolean;
  }

  export interface OperationTeamInfo {
    businessType?: string;
    closeDaysBefore?: number;
    departmentId?: number;
    departmentName?: string;
    departureDate?: string;
    endDate?: string;
    escortEmployeeId?: number;
    escortEmployeeName?: string;
    escortSummary?: string;
    guideSummary?: string;
    id: number;
    leaderSummary?: string;
    operatorEmployeeId?: number;
    operatorEmployeeName?: string;
    productId: number;
    remainingSeats?: number;
    status?: TeamStatus;
    statusLabel?: string;
    teamNo: string;
    teamType?: TeamType;
    teamTypeLabel?: string;
    totalSeats?: number;
    travelDays?: number;
    usedSeats?: number;
  }

  export interface OperationProductInfo {
    businessType?: string;
    departurePlace?: string;
    domesticInternational?: string;
    id: number;
    productName?: string;
    productTheme?: string;
    receptionStandard?: string;
  }

  export interface OperationContentInfo {
    bookingNotice?: string;
    internalRemark?: string;
    productDescription?: string;
  }

  export interface OperationRouteSummary {
    totalDistanceMeters?: number;
    totalDurationSeconds?: number;
  }

  export interface OperationItineraryDay {
    accommodationNote?: string;
    breakfastIncluded?: boolean;
    dayNo?: number;
    dayTitle?: string;
    dinnerIncluded?: boolean;
    id?: number;
    itineraryContent?: string;
    lunchIncluded?: boolean;
    relatedHotel?: string;
    remark?: string;
    roadbookSummary?: string;
    roadbookTotalDistanceMeters?: number;
    roadbookTotalDurationSeconds?: number;
  }

  export interface OperationOrderRow {
    balanceAmount?: string;
    bookingInfo?: string;
    dropoffInfo?: string;
    feeRemark?: string;
    guestCount?: number;
    guestCountText?: string;
    guestName?: string;
    id: number;
    mergeOrderInfos?: OperationOrderRelationInfo[];
    orderInfo?: string;
    orderNo?: string;
    orderRole?: 'merge_child' | 'merge_source' | 'normal' | string;
    orderRoleLabel?: string;
    orderRemark?: string;
    originalOrderInfo?: string;
    pickupInfo?: string;
    pickupRemark?: string;
    priceDetail?: string;
    receivableAmount?: string;
    receivedAmount?: string;
    sourcePlace?: string;
    sourceOrderInfos?: OperationOrderRelationInfo[];
    status?: string;
  }

  export interface OperationOrderRelationInfo {
    orderId?: number;
    summary?: string;
    teamId?: number;
  }

  export interface OperationActionInfo {
    code: string;
    enabled: boolean;
    group: 'business' | 'danger' | 'tool';
    label: string;
    note?: string;
    target?: string;
  }

  export interface OperationDetail {
    actions: OperationActionInfo[];
    content?: OperationContentInfo;
    itineraryDays: OperationItineraryDay[];
    orders: OperationOrderRow[];
    prices: PriceItem[];
    product?: OperationProductInfo;
    routeSummary?: OperationRouteSummary;
    team: OperationTeamInfo;
  }

  export interface TransferRemarkParams {
    orderId: number;
    remark?: string;
    targetTeamId: number;
  }

  export interface MergeOrderParams {
    orderIds: number[];
    remark?: string;
    remarks?: TransferRemarkParams[];
    tagFlag: boolean;
    targetTeamId: number;
  }

  export interface MoveOrderParams {
    allNum?: number;
    createNewTeam: boolean;
    lineName?: string;
    lineType?: TeamType;
    memo?: string;
    orderIds: number[];
    remark?: string;
    targetTeamId?: number;
    tourDate?: string;
  }
}

/** 分页查询销售团队管理全局列表。 */
export function getSalesTeamPage(params: SalesTeamApi.PageQueryParams) {
  return requestClient.get<SalesTeamApi.PageResult<SalesTeamApi.ListItem>>(
    '/sales/team/page',
    { params },
  );
}

/** 查询团队操作页只读详情。 */
export function getSalesTeamOperationDetail(teamId: number) {
  return requestClient.get<SalesTeamApi.OperationDetail>(
    `/sales/team/${teamId}/operation`,
  );
}

/** 执行团队操作页拼团。 */
export function mergeSalesTeamOrders(teamId: number, data: SalesTeamApi.MergeOrderParams) {
  return requestClient.post<void>(`/sales/team/${teamId}/operation/merge`, data);
}

/** 执行团队操作页转团。 */
export function moveSalesTeamOrders(teamId: number, data: SalesTeamApi.MoveOrderParams) {
  return requestClient.post<void>(`/sales/team/${teamId}/operation/move`, data);
}

/** 分页查询产品团期。 */
export function getSalesTeamSchedulePage(params: SalesTeamApi.QueryParams) {
  return requestClient.get<SalesTeamApi.PageResult<SalesTeamApi.Item>>(
    '/sales/team/schedule/page',
    { params },
  );
}

/** 按日期范围批量生成产品团期。 */
export function batchCreateSalesTeamSchedule(productId: number, data: SalesTeamApi.BatchCreateParams) {
  return requestClient.post<SalesTeamApi.Item[]>(
    '/sales/team/schedule/batch-create',
    data,
    { params: { productId } },
  );
}

/** 保存团队主信息。 */
export function saveSalesTeam(teamId: number, data: SalesTeamApi.TeamSaveParams) {
  return requestClient.post<SalesTeamApi.Item>(
    '/sales/team/schedule/team/save',
    data,
    { params: { teamId } },
  );
}

/** 保存团队客户类型价格。 */
export function saveSalesTeamPrice(teamId: number, data: SalesTeamApi.PriceSaveParams) {
  return requestClient.post<SalesTeamApi.PriceItem>(
    '/sales/team/schedule/price/save',
    data,
    { params: { teamId } },
  );
}

/** 删除团队客户类型价格行。 */
export function deleteSalesTeamPrice(priceId: number) {
  return requestClient.post<void>(
    '/sales/team/schedule/price/delete',
    {},
    { params: { priceId } },
  );
}

/** 按旧系统“添加/修改团期信息”规则批量修改团队和客户类型价格。 */
export function batchEditSalesTeamSchedule(data: SalesTeamApi.BatchEditParams) {
  return requestClient.post<void>('/sales/team/schedule/batch-edit', data);
}

/** 批量变更团队状态。 */
export function changeSalesTeamStatus(data: {
  action: SalesTeamApi.StatusAction;
  remark?: string;
  teamIds: number[];
}) {
  return requestClient.post<void>('/sales/team/schedule/status/change', data);
}
