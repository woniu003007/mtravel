import { requestClient } from '#/api/request';
import type { SalesProductApi } from '#/api/sales/product';

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
  export type ArrangementType =
    | 'extra_fee'
    | 'ground_agent'
    | 'hotel'
    | 'meal'
    | 'optional'
    | 'other'
    | 'scenic'
    | 'shopping'
    | 'traffic'
    | 'vehicle';
  export type TeamArrangementSectionStatus = 'done' | 'none' | 'pending';
  export type ArrangementAllocationMode = 'group_order_average' | 'multi_order_average';
  export type ArrangementSplitMode = 'by_order' | 'by_people';
  export type DateTeamStatus = 'all' | 'cancelled' | 'departed' | 'not_departed' | 'stopped';
  export type TeamProgress = 'cancelled' | 'closed' | 'departed' | 'done' | 'not_departed' | 'receiving' | 'stopped';

  export interface PageQueryParams {
    addDate?: string;
    businessType?: string;
    customerKeyword?: string;
    departmentName?: string;
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
    optionalMarkupRate?: number;
    perCapitaPitAmount?: number;
    perCapitaShoppingAmount?: number;
    remark?: string;
    singleRoomDifference?: number;
    teamType?: TeamType;
    totalSeats?: number;
  }

  export interface DirectCreateParams {
    attentionItems?: string;
    bookingNotice?: string;
    businessType?: string;
    childPolicy?: string;
    city?: string;
    closeDaysBefore?: number;
    departureDate: string;
    district?: string;
    domesticInternational?: 'domestic' | 'international';
    feeExcluded?: string;
    feeIncluded?: string;
    giftItems?: string;
    itineraryDays?: SalesProductApi.ItineraryDay[];
    productTheme?: string;
    productDescription?: string;
    province?: string;
    receptionStandard?: string;
    remark?: string;
    shoppingArrangement?: string;
    singleRoomDifference?: number;
    teamName: string;
    teamType: Exclude<TeamType, 'single'>;
    totalSeats?: number;
    travelDays?: number;
    tripType?: 'daily' | 'irregular' | 'weekly';
    optionalItems?: string;
    warmReminder?: string;
  }

  export interface DirectEditDetail extends DirectCreateParams {
    id: number;
    productId: number;
    teamNo: string;
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
    optionalMarkupRate?: number;
    perCapitaPitAmount?: number;
    perCapitaShoppingAmount?: number;
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

  export interface TeamArrangementPriceLine {
    amount?: number;
    cashAmount?: number;
    companyRebateAmount?: number;
    companyRebateRate?: number;
    consumptionAmount?: number;
    costPrice?: number;
    creditAmount?: number;
    guideCommissionAmount?: number;
    guideCommissionRate?: number;
    headFeeAmount?: number;
    id?: number;
    projectId?: number;
    projectName?: string;
    quantity?: number;
    remark?: string;
    salePrice?: number;
    sortOrder?: number;
    unitPrice?: number;
  }

  export interface TeamArrangementAllocation {
    allocationAmount?: number;
    allocationMode?: ArrangementAllocationMode;
    allocationScope?: 'order' | 'team';
    customerId?: number;
    customerName?: string;
    guestCount?: number;
    id?: number;
    orderId?: number;
    orderNo?: string;
    originalAmount?: number;
    sortOrder?: number;
    splitBatchNo?: string;
    splitMode?: ArrangementSplitMode;
  }

  export interface TeamArrangement {
    allocationMode: ArrangementAllocationMode;
    allocations?: TeamArrangementAllocation[];
    arrivalPlace?: string;
    arrangementContent?: string;
    arrangementType: ArrangementType;
    businessDate?: string;
    canDelete?: boolean;
    cashAmount?: number;
    companyRebateAmount?: number;
    consumptionAmount?: number;
    costAmount?: number;
    costStage?: string;
    createdAt?: string;
    creditAmount?: number;
    daysCount?: number;
    deleteDisabledReason?: string;
    departurePlace?: string;
    driverName?: string;
    financeAuditStatus?: string;
    fundIncluded?: string;
    guideCommissionAmount?: number;
    guideId?: number;
    guideInvolved?: boolean;
    guideName?: string;
    guideReportStatus?: string;
    headFeeAmount?: number;
    id: number;
    itemName: string;
    confirmed?: boolean;
    confirmationNo?: string;
    mealType?: string;
    noGuideReport?: boolean;
    operatorAuditStatus?: string;
    peopleCount?: number;
    prepaidAmount?: number;
    priceLines?: TeamArrangementPriceLine[];
    resourceName?: string;
    responsibleEmployeeId?: number;
    responsibleEmployeeName?: string;
    saleAmount?: number;
    scheduleEndDay?: string;
    scheduleStartDay?: string;
    settlementType?: SalesProductApi.SettlementType;
    splitBatchNo?: string;
    splitMode?: ArrangementSplitMode;
    status?: string;
    supplierId?: number;
    supplierName?: string;
    teamId?: number;
    teamNo?: string;
    totalAmount?: number;
    trafficType?: string;
    vehiclePlate?: string;
    vehicleType?: string;
  }

  export interface TeamArrangementSaveParams {
    allocationMode?: ArrangementAllocationMode;
    arrivalPlace?: string;
    arrangementContent?: string;
    arrangementId?: number;
    arrangementType: ArrangementType;
    cashAmount?: number;
    companyRebateAmount?: number;
    consumptionAmount?: number;
    costAmount?: number;
    creditAmount?: number;
    daysCount?: number;
    departurePlace?: string;
    driverName?: string;
    fundIncluded?: string;
    guideCommissionAmount?: number;
    guideId?: number;
    guideName?: string;
    headFeeAmount?: number;
    itemName: string;
    confirmed?: boolean;
    confirmationNo?: string;
    mealType?: string;
    multiOrderSplitMode?: ArrangementSplitMode;
    noGuideReport?: boolean;
    peopleCount?: number;
    prepaidAmount?: number;
    priceLines?: TeamArrangementPriceLine[];
    remark?: string;
    resourceName?: string;
    responsibleEmployeeId?: number;
    responsibleEmployeeName?: string;
    saleAmount?: number;
    scheduleEndDay?: string;
    scheduleStartDay?: string;
    selectedOrderIds?: number[];
    settlementType?: SalesProductApi.SettlementType;
    supplierId?: number;
    supplierName?: string;
    totalAmount?: number;
    trafficType?: string;
    vehiclePlate?: string;
    vehicleType?: string;
  }

  export interface TeamArrangementSaveResult {
    id: number;
    ids: number[];
  }

  export interface TeamArrangementSectionStatusItem {
    arrangementType: ArrangementType;
    id?: number;
    status: TeamArrangementSectionStatus;
    teamId?: number;
    teamNo?: string;
  }

  export interface TeamArrangementSummaryCostColumn {
    cashAmount?: number | string;
    creditAmount?: number | string;
    key: string;
    label: string;
  }

  export interface TeamArrangementSummarySection {
    arrangementType: ArrangementType;
    cashAmount?: number | string;
    costAmount?: number | string;
    count: number;
    creditAmount?: number | string;
  }

  export interface TeamArrangementSummary {
    budgetProfitAmount?: number | string;
    costColumns: TeamArrangementSummaryCostColumn[];
    guideFeeAmount?: number | string;
    guideImprestAmount?: number | string;
    guideOperationFeeAmount?: number | string;
    optionalCompanyProfitAmount?: number | string;
    orderBalanceAmount?: number | string;
    orderReceivableAmount?: number | string;
    orderReceivedAmount?: number | string;
    regularCostAmount?: number | string;
    sectionSummaries: TeamArrangementSummarySection[];
    shoppingCompanyProfitAmount?: number | string;
  }

  export interface GrossProfitTeamInfo {
    departureDate?: string;
    guestCount?: number;
    guideSummary?: string;
    operatorName?: string;
    productName?: string;
    teamId: number;
    teamNo?: string;
    travelDays?: number;
  }

  export interface GrossProfitIncomeRow {
    bookingOperatorName?: string;
    customerName?: string;
    guestCount?: number;
    receivableAmount?: number | string;
    receivableDetail?: string;
    receivedAmount?: number | string;
    salespersonName?: string;
  }

  export interface GrossProfitCostRow {
    auditorName?: string;
    cashAmount?: number | string;
    category?: string;
    costDescription?: string;
    paidCreditAmount?: number | string;
    payableAmount?: number | string;
    supplierName?: string;
  }

  export interface GrossProfitOptionalRow {
    auditorName?: string;
    companyProfit?: number | string;
    costAmount?: number | string;
    guestCount?: number | string;
    guideCommissionAmount?: number | string;
    projectName?: string;
    salesAmount?: number | string;
  }

  export interface GrossProfitShoppingRow {
    auditorName?: string;
    companyProfit?: number | string;
    companyRebateAmount?: number | string;
    consumptionAmount?: number | string;
    entryCount?: number | string;
    guideCommissionAmount?: number | string;
    headFeeAmount?: number | string;
    shopName?: string;
  }

  export interface GrossProfitSummary {
    grossProfit?: number | string;
    guideFee?: number | string;
    optionalProfit?: number | string;
    orderIncome?: number | string;
    regularCost?: number | string;
    shoppingProfit?: number | string;
  }

  export interface GrossProfitSalespersonSummary {
    grossProfit?: number | string;
    grossProfitRate?: number | string;
    receivableAmount?: number | string;
    receivedAmount?: number | string;
    salespersonName?: string;
  }

  export interface GrossProfitPreview {
    costRows: GrossProfitCostRow[];
    incomeRows: GrossProfitIncomeRow[];
    optionalRows: GrossProfitOptionalRow[];
    salespersonRows: GrossProfitSalespersonSummary[];
    shoppingRows: GrossProfitShoppingRow[];
    summary: GrossProfitSummary;
    team: GrossProfitTeamInfo;
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

/** 团队管理页直接新增散拼、整团或散团。 */
export function createSalesTeam(data: SalesTeamApi.DirectCreateParams) {
  return requestClient.post<SalesTeamApi.Item>('/sales/team/create', data);
}

/** 查询团队直接编辑页详情。 */
export function getSalesTeamEditDetail(teamId: number) {
  return requestClient.get<SalesTeamApi.DirectEditDetail>(
    `/sales/team/${teamId}/edit`,
  );
}

/** 保存团队直接编辑页。 */
export function updateSalesTeam(teamId: number, data: SalesTeamApi.DirectCreateParams) {
  return requestClient.post<SalesTeamApi.Item>(
    `/sales/team/${teamId}/edit`,
    data,
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

/** 查询正式团队安排成本。 */
export function getTeamArrangements(teamId: number, type?: SalesTeamApi.ArrangementType) {
  return requestClient.get<SalesTeamApi.TeamArrangement[]>(
    `/sales/team/${teamId}/arrangements`,
    { params: { type } },
  );
}

/** 查询正式团队安排页后端权威金额汇总。 */
export function getTeamArrangementSummary(teamId: number) {
  return requestClient.get<SalesTeamApi.TeamArrangementSummary>(
    `/sales/team/${teamId}/arrangements/summary`,
  );
}

/** 保存正式团队安排成本。 */
export function saveTeamArrangement(teamId: number, data: SalesTeamApi.TeamArrangementSaveParams) {
  return requestClient.post<SalesTeamApi.TeamArrangementSaveResult>(
    `/sales/team/${teamId}/arrangements/save`,
    data,
  );
}

/** 删除正式团队安排成本。 */
export function deleteTeamArrangement(teamId: number, arrangementId: number) {
  return requestClient.post<void>(
    `/sales/team/${teamId}/arrangements/${arrangementId}/delete`,
    {},
  );
}

/** 查询正式团队安排分类流程状态。 */
export function getTeamArrangementSectionStatuses(teamId: number) {
  return requestClient.get<SalesTeamApi.TeamArrangementSectionStatusItem[]>(
    `/sales/team/${teamId}/arrangement-section-statuses`,
  );
}

/** 保存正式团队安排分类流程状态。 */
export function saveTeamArrangementSectionStatus(
  teamId: number,
  arrangementType: SalesTeamApi.ArrangementType,
  status: SalesTeamApi.TeamArrangementSectionStatus,
) {
  return requestClient.post<SalesTeamApi.TeamArrangementSectionStatusItem>(
    `/sales/team/${teamId}/arrangement-section-statuses/${arrangementType}`,
    { status },
  );
}

/** 下载正式团队景区票务游客名单 Excel。 */
export function exportScenicTicketGuests(teamId: number, params: {
  resourceName: string;
  supplierId: number;
}) {
  return requestClient.download<Blob>(
    `/sales/team/${teamId}/arrangements/scenic-ticket-guests/export`,
    { params },
  );
}

/** 查询正式团队预算毛利表预览数据。 */
export function getSalesTeamGrossProfitPreview(teamId: number) {
  return requestClient.get<SalesTeamApi.GrossProfitPreview>(
    `/sales/team/${teamId}/gross-profit/preview`,
  );
}

/** 下载正式团队预算毛利表 Word 或 PDF。 */
export function exportSalesTeamGrossProfit(teamId: number, format: 'docx' | 'pdf') {
  return requestClient.download<Blob>(
    `/sales/team/${teamId}/gross-profit/export`,
    { params: { format } },
  );
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
