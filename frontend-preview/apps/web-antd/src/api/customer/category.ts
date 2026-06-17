import { requestClient } from '#/api/request';

export namespace CustomerCategoryApi {
  export interface CustomerCategory {
    id: number;
    categoryName: string;
    defaultCreditLimit: number;
    sortOrder: number;
    status: 'active' | 'disabled';
    createdBy?: string;
    remark?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: 'active' | 'disabled';
  }

  export interface SaveParams {
    categoryName: string;
    defaultCreditLimit?: number;
    remark?: string;
    sortOrder?: number;
    status?: 'active' | 'disabled';
  }
}

export function getCustomerCategoryPage(params: CustomerCategoryApi.QueryParams) {
  return requestClient.get<
    CustomerCategoryApi.PageResult<CustomerCategoryApi.CustomerCategory>
  >('/customer/category/page', { params });
}

export function getCustomerCategoryAll() {
  return requestClient.get<CustomerCategoryApi.CustomerCategory[]>(
    '/customer/category/all',
  );
}

export function createCustomerCategory(data: CustomerCategoryApi.SaveParams) {
  return requestClient.post<CustomerCategoryApi.CustomerCategory>(
    '/customer/category/create',
    data,
  );
}

export function updateCustomerCategory(
  id: number,
  data: CustomerCategoryApi.SaveParams,
) {
  return requestClient.post<CustomerCategoryApi.CustomerCategory>(
    '/customer/category/update',
    data,
    { params: { id } },
  );
}

export function deleteCustomerCategory(id: number) {
  return requestClient.post<void>(
    '/customer/category/delete',
    {},
    { params: { id } },
  );
}
