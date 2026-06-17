import { requestClient } from '#/api/request';

export namespace SalesProductApi {
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

  export type DomesticType = 'domestic' | 'international';
  export type SettlementType = 'cash' | 'credit';
  export type Status = 'active' | 'disabled';
  export type TripType = 'daily' | 'irregular' | 'weekly';
  export type AllocationMode = 'group_order_average' | 'multi_order_average';

  export interface ItineraryDay {
    accommodationNote?: string;
    breakfastIncluded?: boolean;
    dayNo: number;
    dayTitle?: string;
    dinnerIncluded?: boolean;
    id?: number;
    itineraryContent?: string;
    lunchIncluded?: boolean;
    relatedHotel?: string;
    remark?: string;
    roadbookPlace?: string;
    roadbookPoints?: RoadbookPoint[];
    roadbookSummary?: string;
    roadbookTotalDistanceMeters?: number;
    roadbookTotalDurationSeconds?: number;
    seasonalSurcharge?: number;
  }

  export interface RoadbookPoint {
    address?: string;
    distanceToNextMeters?: number;
    durationToNextSeconds?: number;
    latitude?: string;
    longitude?: string;
    placeName: string;
    pointOrder: number;
    pointType?: 'arrival' | 'departure' | 'hotel' | 'meal' | 'scenic' | 'shopping' | 'waypoint';
    remark?: string;
    stayMinutes?: number;
  }

  export interface AmapTip {
    address?: string;
    district?: string;
    latitude?: string;
    longitude?: string;
    name: string;
  }

  export interface AmapJsConfig {
    key: string;
    securityJsCode?: string;
  }

  export interface AmapRouteCalculateParams {
    points: Array<{
      latitude: string;
      longitude: string;
    }>;
  }

  export interface AmapStaticMapParams {
    points: Array<{
      latitude: string;
      longitude: string;
    }>;
  }

  export interface AmapRouteSegment {
    distanceMeters: number;
    durationSeconds: number;
  }

  export interface AmapRouteCalculateResult {
    segments: AmapRouteSegment[];
    totalDistanceMeters: number;
    totalDurationSeconds: number;
  }

  export interface ArrangementItem {
    allocationMode?: AllocationMode;
    arrivalPlace?: string;
    arrangementContent?: string;
    arrangementType: ArrangementType;
    cashAmount?: number;
    companyRebateAmount?: number;
    confirmed?: boolean;
    confirmationNo?: string;
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
    id?: number;
    itemName: string;
    mealType?: string;
    noGuideReport?: boolean;
    orderScope?: string;
    peopleCount?: number;
    prepaidAmount?: number;
    projectName?: string;
    priceLines?: ArrangementPriceLine[];
    quantity?: number;
    remark?: string;
    resourceName?: string;
    responsibleEmployeeId?: number;
    responsibleEmployeeName?: string;
    saleAmount?: number;
    scheduleEndDay?: string;
    scheduleStartDay?: string;
    settlementType?: SettlementType;
    supplierId?: number;
    supplierName?: string;
    totalAmount?: number;
    trafficType?: string;
    unitName?: string;
    unitPrice?: number;
    vehiclePlate?: string;
    vehicleType?: string;
  }

  export interface ArrangementPriceLine {
    amount?: number;
    cashAmount?: number;
    companyRebateAmount?: number;
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

  export interface Item {
    arrangementItems?: ArrangementItem[];
    attentionItems?: string;
    bookingNotice?: string;
    businessType?: string;
    childPolicy?: string;
    city?: string;
    closeDaysBefore?: number;
    createdAt?: string;
    createdBy?: string;
    district?: string;
    domesticInternational: DomesticType;
    feeExcluded?: string;
    feeIncluded?: string;
    giftItems?: string;
    id: number;
    itineraryDays?: ItineraryDay[];
    optionalItems?: string;
    plannedCapacity?: number;
    productDescription?: string;
    productName: string;
    productTheme?: string;
    province?: string;
    receptionStandard?: string;
    remark?: string;
    shoppingArrangement?: string;
    singleRoomDifference?: number;
    status: Status;
    travelDays: number;
    tripType: TripType;
    updatedAt?: string;
    warmReminder?: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    businessType?: string;
    domesticInternational?: DomesticType;
    keyword?: string;
    page?: number;
    pageSize?: number;
    receptionStandard?: string;
    status?: Status;
  }

  export interface SaveParams {
    arrangementItems?: ArrangementItem[];
    attentionItems?: string;
    bookingNotice?: string;
    businessType?: string;
    childPolicy?: string;
    city?: string;
    closeDaysBefore?: number;
    district?: string;
    domesticInternational?: DomesticType;
    feeExcluded?: string;
    feeIncluded?: string;
    giftItems?: string;
    itineraryDays?: ItineraryDay[];
    optionalItems?: string;
    plannedCapacity?: number;
    productDescription?: string;
    productName: string;
    productTheme?: string;
    province?: string;
    receptionStandard?: string;
    remark?: string;
    shoppingArrangement?: string;
    singleRoomDifference?: number;
    status?: Status;
    travelDays?: number;
    tripType?: TripType;
    warmReminder?: string;
  }
}

export function getSalesProductPage(params: SalesProductApi.QueryParams) {
  return requestClient.get<SalesProductApi.PageResult<SalesProductApi.Item>>(
    '/sales/product/page',
    { params },
  );
}

export function getSalesProductDetail(id: number) {
  return requestClient.get<SalesProductApi.Item>('/sales/product/detail', {
    params: { id },
  });
}

export function createSalesProduct(data: SalesProductApi.SaveParams) {
  return requestClient.post<SalesProductApi.Item>('/sales/product/create', data);
}

export function updateSalesProduct(id: number, data: SalesProductApi.SaveParams) {
  return requestClient.post<SalesProductApi.Item>(
    '/sales/product/update',
    data,
    { params: { id } },
  );
}

export function deleteSalesProduct(id: number) {
  return requestClient.post<void>('/sales/product/delete', {}, { params: { id } });
}

export function searchAmapTips(params: { city?: string; keywords: string }) {
  return requestClient.get<SalesProductApi.AmapTip[]>('/sales/product/roadbook/amap/tips', {
    params,
  });
}

export function getAmapJsConfig() {
  return requestClient.get<SalesProductApi.AmapJsConfig>('/sales/product/roadbook/amap/js-config');
}

export function calculateRoadbookRoute(data: SalesProductApi.AmapRouteCalculateParams) {
  return requestClient.post<SalesProductApi.AmapRouteCalculateResult>(
    '/sales/product/roadbook/amap/driving',
    data,
  );
}

export function getRoadbookStaticMapUrl(data: SalesProductApi.AmapStaticMapParams) {
  return requestClient.post<string>('/sales/product/roadbook/amap/static-map', data);
}
