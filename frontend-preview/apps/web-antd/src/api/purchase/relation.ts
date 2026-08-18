import { requestClient } from '#/api/request';

export namespace PurchaseRelationApi {
  export type ResourceType =
    | 'ground_agent'
    | 'guide'
    | 'hotel'
    | 'other'
    | 'restaurant'
    | 'scenic'
    | 'shopping'
    | 'ticket'
    | 'traffic'
    | 'vehicle';

  export type Status = 'active' | 'disabled' | 'expired';

  export interface Item {
    id: number;
    resourceType: ResourceType;
    resourceId: number;
    resourceName: string;
    supplierId: number;
    supplierName?: string;
    location?: string;
    contactName?: string;
    contactPhone?: string;
    groupQuantity: number;
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
    supplierId?: number;
  }

  export interface SaveParams {
    resourceId: number;
    supplierId: number;
    groupQuantity?: number;
    isDefault?: boolean;
    status?: Status;
    remark?: string;
  }
}

export function getPurchaseRelationPage(params: PurchaseRelationApi.QueryParams) {
  return requestClient.get<PurchaseRelationApi.PageResult<PurchaseRelationApi.Item>>(
    '/purchase/relation/page',
    { params },
  );
}

export function createPurchaseRelation(data: PurchaseRelationApi.SaveParams) {
  return requestClient.post<PurchaseRelationApi.Item>('/purchase/relation/create', data);
}

export function updatePurchaseRelation(id: number, data: PurchaseRelationApi.SaveParams) {
  return requestClient.post<PurchaseRelationApi.Item>(
    '/purchase/relation/update',
    data,
    { params: { id } },
  );
}

export function deletePurchaseRelation(id: number) {
  return requestClient.post<void>('/purchase/relation/delete', {}, { params: { id } });
}
