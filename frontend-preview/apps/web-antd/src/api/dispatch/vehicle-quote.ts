import { requestClient } from '#/api/request';

export namespace VehicleQuoteApi {
  export type Status = 'active' | 'disabled';

  export interface Rule {
    baseKilometers: number;
    basePrice: number;
    city?: string;
    createdAt?: string;
    createdBy?: string;
    district?: string;
    extraKilometerPrice: number;
    floatRate: number;
    id: number;
    minimumPrice: number;
    province?: string;
    remark?: string;
    status: Status;
    updatedAt?: string;
    vehicleType: string;
  }

  export interface RuleSnapshot {
    baseKilometers: number;
    basePrice: number;
    city?: string;
    district?: string;
    extraKilometerPrice: number;
    floatRate: number;
    minimumPrice: number;
    province?: string;
    ruleId?: number;
    vehicleType: string;
  }

  export interface CalculateParams {
    city?: string;
    distanceMeters?: number;
    district?: string;
    province?: string;
    vehicleType: string;
  }

  export interface CalculateResult {
    calculatedAmount: number;
    distanceKilometers: number;
    distanceMeters: number;
    ruleSnapshot: RuleSnapshot;
    vehicleType: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    city?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: Status;
    vehicleType?: string;
  }

  export interface SaveParams {
    baseKilometers?: number;
    basePrice?: number;
    city?: string;
    district?: string;
    extraKilometerPrice?: number;
    floatRate?: number;
    minimumPrice?: number;
    province?: string;
    remark?: string;
    status?: Status;
    vehicleType: string;
  }
}

export function getVehicleQuoteRulePage(params: VehicleQuoteApi.QueryParams) {
  return requestClient.get<VehicleQuoteApi.PageResult<VehicleQuoteApi.Rule>>(
    '/dispatch/vehicle-quote-rules/page',
    { params },
  );
}

export function getVehicleQuoteRuleAll(vehicleType?: string) {
  return requestClient.get<VehicleQuoteApi.Rule[]>('/dispatch/vehicle-quote-rules/all', {
    params: { vehicleType },
  });
}

export function createVehicleQuoteRule(data: VehicleQuoteApi.SaveParams) {
  return requestClient.post<VehicleQuoteApi.Rule>('/dispatch/vehicle-quote-rules/create', data);
}

export function updateVehicleQuoteRule(id: number, data: VehicleQuoteApi.SaveParams) {
  return requestClient.post<VehicleQuoteApi.Rule>(
    '/dispatch/vehicle-quote-rules/update',
    data,
    { params: { id } },
  );
}

export function deleteVehicleQuoteRule(id: number) {
  return requestClient.post<void>('/dispatch/vehicle-quote-rules/delete', {}, { params: { id } });
}

export function calculateVehicleQuote(data: VehicleQuoteApi.CalculateParams) {
  return requestClient.post<VehicleQuoteApi.CalculateResult>(
    '/dispatch/vehicle-quote-rules/calculate',
    data,
  );
}
