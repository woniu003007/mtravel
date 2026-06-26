import { requestClient } from '#/api/request';

export namespace SalesBookingApi {
  export type OrderStatus = 'cancelled' | 'confirmed' | 'pending';

  export interface TeamDraft {
    content?: {
      bookingNotice?: string;
      internalRemark?: string;
      productDescription?: string;
    };
    product?: {
      businessType?: string;
      departurePlace?: string;
      id: number;
      productName?: string;
      receptionStandard?: string;
    };
    team?: {
      closeDaysBefore?: number;
      departmentName?: string;
      departureDate?: string;
      endDate?: string;
      escortEmployeeName?: string;
      escortSummary?: string;
      guideSummary?: string;
      id: number;
      leaderSummary?: string;
      operatorEmployeeName?: string;
      remainingSeats?: number;
      status?: string;
      statusLabel?: string;
      teamNo?: string;
      teamTypeLabel?: string;
      totalSeats?: number;
      usedSeats?: number;
    };
  }

  export interface PriceLine {
    id?: number;
    itemName?: string;
    lineType?: string;
    quantity?: number;
    remark?: string;
    subtotalAmount?: number;
    unitPrice?: number;
  }

  export interface Guest {
    age?: number;
    birthDate?: string;
    certificateNo?: string;
    englishName?: string;
    gender?: string;
    guestName?: string;
    guestType?: string;
    id?: number;
    idCardValid?: boolean;
    idCardWarning?: string;
    indexNo?: number;
    leaderFlag?: boolean;
    passportNo?: string;
    phone?: string;
    remark?: string;
    roomGroup?: string;
    roomRemark?: string;
  }

  export interface FeeChange {
    amount?: number;
    changeType?: string;
    feeDescription?: string;
    feeProjectId?: number;
    feeProjectName?: string;
    id?: number;
    registeredAt?: string;
    registeredBy?: string;
    remark?: string;
    status?: string;
  }

  export interface FeeChangeCreateParams {
    amount: number;
    changeType: 'decrease' | 'increase';
    feeDescription: string;
    feeProjectId: number;
    remark?: string;
  }

  export interface SaveParams {
    confirmRemark?: string;
    contactName?: string;
    contactPhone?: string;
    customerId?: number;
    customerName?: string;
    customerTeamNo?: string;
    bookingOperatorEmployeeId?: number;
    bookingOperatorEmployeeName?: string;
    dropoffInfo?: string;
    feeRemark?: string;
    guests?: Guest[];
    guideName?: string;
    guidePhone?: string;
    guideRemark?: string;
    hotelInfo?: string;
    id?: number;
    orderNo?: string;
    orderRemark?: string;
    originalOrderInfo?: string;
    pickupInfo?: string;
    pickupRemark?: string;
    priceLines?: PriceLine[];
    receivedAmount?: number;
    riskApprovalRequestId?: number;
    salespersonEmployeeId?: number;
    salespersonEmployeeName?: string;
    sourceCity?: string;
    sourceDistrict?: string;
    sourceProvince?: string;
    status?: OrderStatus;
    teamId: number;
    travelDescription?: string;
  }

  export interface Detail extends SaveParams {
    adultCount?: number;
    balanceAmount?: number;
    bookedAt?: string;
    bookedBy?: string;
    childCount?: number;
    childNoBedCount?: number;
    escortCount?: number;
    feeChanges?: FeeChange[];
    guestCount?: number;
    id: number;
    orderNo: string;
    receivableAmount?: number;
    seniorCount?: number;
  }

  export interface GuestImportPreview {
    duplicateCount?: number;
    guests: Guest[];
    importedCount?: number;
    invalidCount?: number;
    validCount?: number;
    warnings?: string[];
  }
}

/** 查询新增收客订单需要的团队草稿。 */
export function getSalesBookingTeamDraft(teamId: number) {
  return requestClient.get<SalesBookingApi.TeamDraft>(
    `/sales/booking/team/${teamId}`,
  );
}

/** 查询团队下订单行，供团队操作页刷新使用。 */
export function getSalesBookingTeamOrders(teamId: number) {
  return requestClient.get(`/sales/booking/team/${teamId}/orders`);
}

/** 查询收客订单详情。 */
export function getSalesBookingOrderDetail(id: number) {
  return requestClient.get<SalesBookingApi.Detail>('/sales/booking/detail', {
    params: { id },
  });
}

/** 保存收客订单。 */
export function saveSalesBookingOrder(data: SalesBookingApi.SaveParams) {
  return requestClient.post<SalesBookingApi.Detail>('/sales/booking/save', data);
}

/** 新增订单费用变更。 */
export function createSalesBookingFeeChange(
  orderId: number,
  data: SalesBookingApi.FeeChangeCreateParams,
) {
  return requestClient.post<SalesBookingApi.FeeChange>(
    '/sales/booking/fee-change/create',
    data,
    { params: { orderId } },
  );
}

/** 作废订单费用变更。 */
export function cancelSalesBookingFeeChange(id: number) {
  return requestClient.post<void>(
    '/sales/booking/fee-change/cancel',
    {},
    { params: { id } },
  );
}

/** 导出订单游客名单 xls。 */
export function exportSalesBookingGuests(orderId: number) {
  return requestClient.download<Blob>('/sales/booking/guest-export', {
    params: { id: orderId },
  });
}

/** 下载空白游客名单导入模板 xls。 */
export function downloadSalesBookingGuestImportTemplate() {
  return requestClient.download<Blob>('/sales/booking/guest-import/template');
}

/** 预览导入游客名单 Excel，前端确认保存订单后才正式落库。 */
export function importSalesBookingGuestsPreview(data: FormData) {
  return requestClient.post<SalesBookingApi.GuestImportPreview>(
    '/sales/booking/guest-import/preview',
    data,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}
