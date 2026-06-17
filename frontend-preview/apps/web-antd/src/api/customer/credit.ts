import { requestClient } from '#/api/request';

export namespace CustomerCreditApi {
  export interface Item {
    id: number;
    customerId: number;
    customerName?: string;
    creditLimit: number;
    occupiedAmount: number;
    pendingApprovalAmount: number;
    availableAmount: number;
    warningThreshold: number;
    overLimitAction: 'approval' | 'none' | 'remind';
    status: 'active' | 'disabled';
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }
  export interface PageResult<T> { items: T[]; total: number; }
  export interface QueryParams { keyword?: string; page?: number; pageSize?: number; status?: 'active' | 'disabled'; }
  export interface SaveParams { customerId: number; creditLimit?: number; occupiedAmount?: number; pendingApprovalAmount?: number; warningThreshold?: number; overLimitAction?: 'approval' | 'none' | 'remind'; status?: 'active' | 'disabled'; remark?: string; }
}
export function getCustomerCreditPage(params: CustomerCreditApi.QueryParams) { return requestClient.get<CustomerCreditApi.PageResult<CustomerCreditApi.Item>>('/customer/credit/page', { params }); }
export function createCustomerCredit(data: CustomerCreditApi.SaveParams) { return requestClient.post<CustomerCreditApi.Item>('/customer/credit/create', data); }
export function updateCustomerCredit(id: number, data: CustomerCreditApi.SaveParams) { return requestClient.post<CustomerCreditApi.Item>('/customer/credit/update', data, { params: { id } }); }
export function deleteCustomerCredit(id: number) { return requestClient.post<void>('/customer/credit/delete', {}, { params: { id } }); }
