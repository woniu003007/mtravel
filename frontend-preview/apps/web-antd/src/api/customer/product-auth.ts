import { requestClient } from '#/api/request';

export namespace CustomerProductAuthApi {
  export interface Item { id: number; customerId: number; customerName?: string; productCode?: string; productName: string; authorizedStartDate?: string; authorizedEndDate?: string; authorizationStatus: 'active' | 'expired' | 'suspended'; saleScope?: string; remark?: string; createdBy?: string; createdAt?: string; updatedAt?: string; }
  export interface PageResult<T> { items: T[]; total: number; }
  export interface QueryParams { customerId?: number; keyword?: string; page?: number; pageSize?: number; status?: 'active' | 'expired' | 'suspended'; }
  export interface SaveParams { customerId: number; productCode?: string; productName: string; authorizedStartDate?: string; authorizedEndDate?: string; authorizationStatus?: 'active' | 'expired' | 'suspended'; saleScope?: string; remark?: string; }
}
export function getCustomerProductAuthPage(params: CustomerProductAuthApi.QueryParams) { return requestClient.get<CustomerProductAuthApi.PageResult<CustomerProductAuthApi.Item>>('/customer/product-auth/page', { params }); }
export function createCustomerProductAuth(data: CustomerProductAuthApi.SaveParams) { return requestClient.post<CustomerProductAuthApi.Item>('/customer/product-auth/create', data); }
export function updateCustomerProductAuth(id: number, data: CustomerProductAuthApi.SaveParams) { return requestClient.post<CustomerProductAuthApi.Item>('/customer/product-auth/update', data, { params: { id } }); }
export function deleteCustomerProductAuth(id: number) { return requestClient.post<void>('/customer/product-auth/delete', {}, { params: { id } }); }
