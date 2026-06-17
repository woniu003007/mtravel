import { requestClient } from '#/api/request';

export namespace SupplierResourcePriceApi {
  export type Status = 'active' | 'disabled';

  export interface Item {
    id: number;
    relationId: number;
    resourceProjectId: number;
    projectName: string;
    marketPrice: number;
    peerPrice: number;
    teamPrice: number;
    priceDescription?: string;
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
    page?: number;
    pageSize?: number;
    relationId?: number;
    status?: Status;
  }

  export interface SaveParams {
    relationId: number;
    resourceProjectId: number;
    marketPrice?: number;
    peerPrice?: number;
    teamPrice?: number;
    priceDescription?: string;
    status?: Status;
    remark?: string;
  }
}

export function getSupplierResourcePricePage(params: SupplierResourcePriceApi.QueryParams) {
  return requestClient.get<SupplierResourcePriceApi.PageResult<SupplierResourcePriceApi.Item>>(
    '/purchase/relation/price/page',
    { params },
  );
}

export function createSupplierResourcePrice(data: SupplierResourcePriceApi.SaveParams) {
  return requestClient.post<SupplierResourcePriceApi.Item>('/purchase/relation/price/create', data);
}

export function updateSupplierResourcePrice(id: number, data: SupplierResourcePriceApi.SaveParams) {
  return requestClient.post<SupplierResourcePriceApi.Item>(
    '/purchase/relation/price/update',
    data,
    { params: { id } },
  );
}

export function deleteSupplierResourcePrice(id: number) {
  return requestClient.post<void>('/purchase/relation/price/delete', {}, { params: { id } });
}
