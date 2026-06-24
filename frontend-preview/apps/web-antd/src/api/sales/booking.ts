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
    id?: number;
    registeredAt?: string;
    registeredBy?: string;
    remark?: string;
    status?: string;
  }

  export interface SaveParams {
    confirmRemark?: string;
    contactName?: string;
    contactPhone?: string;
    customerId?: number;
    customerName?: string;
    customerTeamNo?: string;
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
    pickupInfo?: string;
    pickupRemark?: string;
    priceLines?: PriceLine[];
    receivedAmount?: number;
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
