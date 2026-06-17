import { requestClient } from '#/api/request';

export namespace CustomerUnitApi {
  export type CustomerUnitStatus = 'active' | 'disabled';
  export type SettlementMethod =
    | 'cash'
    | 'monthly_1'
    | 'monthly_10'
    | 'monthly_11'
    | 'monthly_12'
    | 'monthly_2'
    | 'monthly_3'
    | 'monthly_4'
    | 'monthly_5'
    | 'monthly_6'
    | 'monthly_7'
    | 'monthly_8'
    | 'monthly_9'
    | 'unlimited';

  export interface CustomerUnit {
    id: number;
    customerCode?: string;
    customerName: string;
    categoryId?: number;
    categoryName?: string;
    creditLimit: number;
    province?: string;
    city?: string;
    district?: string;
    departmentId?: number;
    departmentName?: string;
    dispatcherEmployeeId?: number;
    dispatcherName?: string;
    settlementMethod?: SettlementMethod;
    billStartDate?: string;
    billDay?: number;
    contactName?: string;
    contactPhone?: string;
    registrarName?: string;
    contractExpireDate?: string;
    status: CustomerUnitStatus;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    categoryId?: number;
    city?: string;
    customerCode?: string;
    departmentName?: string;
    district?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
    province?: string;
    status?: CustomerUnitStatus;
  }

  export interface SaveParams {
    categoryId?: number;
    city?: string;
    contactName?: string;
    contactPhone?: string;
    contractExpireDate?: string;
    customerCode?: string;
    customerName: string;
    creditLimit?: number;
    departmentId?: number;
    departmentName?: string;
    dispatcherEmployeeId?: number;
    dispatcherName?: string;
    district?: string;
    province?: string;
    registrarName?: string;
    remark?: string;
    settlementMethod?: SettlementMethod;
    billStartDate?: string;
    billDay?: number;
    status?: CustomerUnitStatus;
  }
}

export function getCustomerUnitPage(params: CustomerUnitApi.QueryParams) {
  return requestClient.get<
    CustomerUnitApi.PageResult<CustomerUnitApi.CustomerUnit>
  >('/customer/unit/page', { params });
}

export function getCustomerUnitDetail(id: number) {
  return requestClient.get<CustomerUnitApi.CustomerUnit>(
    '/customer/unit/detail',
    { params: { id } },
  );
}

export function createCustomerUnit(data: CustomerUnitApi.SaveParams) {
  return requestClient.post<CustomerUnitApi.CustomerUnit>(
    '/customer/unit/create',
    data,
  );
}

export function updateCustomerUnit(
  id: number,
  data: CustomerUnitApi.SaveParams,
) {
  return requestClient.post<CustomerUnitApi.CustomerUnit>(
    '/customer/unit/update',
    data,
    { params: { id } },
  );
}

export function deleteCustomerUnit(id: number) {
  return requestClient.post<void>(
    '/customer/unit/delete',
    {},
    { params: { id } },
  );
}
