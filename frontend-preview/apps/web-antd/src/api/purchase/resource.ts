import { requestClient } from '#/api/request';

export namespace PurchaseResourceApi {
  export type ResourceType = 'hotel' | 'restaurant' | 'scenic' | 'shopping';
  export type Status = 'active' | 'disabled';

  export interface Item {
    address?: string;
    boundSupplierCount: number;
    city?: string;
    createdAt?: string;
    createdBy?: string;
    district?: string;
    fax?: string;
    id: number;
    introduction?: string;
    phone?: string;
    province?: string;
    remark?: string;
    resourceName: string;
    resourceType: ResourceType;
    status: Status;
    updatedAt?: string;
    warmTip?: string;
  }

  export interface Binding {
    createdAt?: string;
    relationId: number;
    groupQuantity: number;
    status: 'active' | 'disabled' | 'expired';
    supplierId: number;
    supplierName?: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    city?: string;
    district?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
    province?: string;
    resourceType?: ResourceType;
    status?: Status;
  }

  export interface SaveParams {
    address?: string;
    autoCreateSupplier?: boolean;
    city?: string;
    district?: string;
    fax?: string;
    introduction?: string;
    phone?: string;
    province?: string;
    remark?: string;
    resourceName: string;
    resourceType?: ResourceType;
    status?: Status;
    warmTip?: string;
  }
}

export function getPurchaseResourcePage(params: PurchaseResourceApi.QueryParams) {
  return requestClient.get<PurchaseResourceApi.PageResult<PurchaseResourceApi.Item>>(
    '/purchase/resource/page',
    { params },
  );
}

export function getPurchaseResourceBindings(resourceId: number) {
  return requestClient.get<PurchaseResourceApi.Binding[]>(
    '/purchase/resource/bindings',
    { params: { resourceId } },
  );
}

export function createPurchaseResource(data: PurchaseResourceApi.SaveParams) {
  return requestClient.post<PurchaseResourceApi.Item>('/purchase/resource/create', data);
}

export function updatePurchaseResource(id: number, data: PurchaseResourceApi.SaveParams) {
  return requestClient.post<PurchaseResourceApi.Item>(
    '/purchase/resource/update',
    data,
    { params: { id } },
  );
}

export function deletePurchaseResource(id: number) {
  return requestClient.post<void>('/purchase/resource/delete', {}, { params: { id } });
}
