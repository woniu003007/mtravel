import { requestClient } from '#/api/request';

export namespace EnterpriseExpenseItemApi {
  export type ResourceType =
    | 'current_refund'
    | 'extra_fee'
    | 'finance_fee'
    | 'ground_agent'
    | 'guide'
    | 'hotel'
    | 'misc'
    | 'other'
    | 'restaurant'
    | 'scenic'
    | 'shopping'
    | 'ticket'
    | 'traffic'
    | 'vehicle';

  export type Status = 'active' | 'disabled';

  export interface Item {
    id: number;
    resourceType: ResourceType;
    projectName: string;
    statisticsEnabled: boolean;
    sortOrder: number;
    status: Status;
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
    keyword?: string;
    page?: number;
    pageSize?: number;
    resourceType?: ResourceType;
    status?: Status;
  }

  export interface SaveParams {
    resourceType?: ResourceType;
    projectName: string;
    statisticsEnabled?: boolean;
    sortOrder?: number;
    status?: Status;
    remark?: string;
  }
}

export function getExpenseItemPage(params: EnterpriseExpenseItemApi.QueryParams) {
  return requestClient.get<EnterpriseExpenseItemApi.PageResult<EnterpriseExpenseItemApi.Item>>(
    '/enterprise/expense-item/page',
    { params },
  );
}

export function getExpenseItemAll(resourceType?: EnterpriseExpenseItemApi.ResourceType) {
  return requestClient.get<EnterpriseExpenseItemApi.Item[]>(
    '/enterprise/expense-item/all',
    { params: { resourceType } },
  );
}

export function createExpenseItem(data: EnterpriseExpenseItemApi.SaveParams) {
  return requestClient.post<EnterpriseExpenseItemApi.Item>('/enterprise/expense-item/create', data);
}

export function updateExpenseItem(id: number, data: EnterpriseExpenseItemApi.SaveParams) {
  return requestClient.post<EnterpriseExpenseItemApi.Item>(
    '/enterprise/expense-item/update',
    data,
    { params: { id } },
  );
}

export function deleteExpenseItem(id: number) {
  return requestClient.post<void>('/enterprise/expense-item/delete', {}, { params: { id } });
}
