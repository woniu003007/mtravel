import { requestClient } from '#/api/request';

export namespace SupplierApi {
  export type Category =
    | 'common'
    | 'ground_agent'
    | 'hotel'
    | 'other'
    | 'restaurant'
    | 'scenic'
    | 'shopping'
    | 'traffic'
    | 'vehicle';

  export type Status = 'active' | 'blacklisted' | 'disabled';

  export interface Item {
    id: number;
    supplierCode?: string;
    supplierName: string;
    supplierCategory: Category;
    buyerId?: number;
    buyerName?: string;
    province?: string;
    city?: string;
    district?: string;
    settlementMethod?: string;
    contactName?: string;
    contactPhone?: string;
    faxNumber?: string;
    officeAddress?: string;
    agreementName?: string;
    rating: number;
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
    category?: Category;
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: Status;
  }

  export interface SaveParams {
    supplierCode?: string;
    supplierName: string;
    supplierCategory?: Category;
    buyerId?: number;
    province?: string;
    city?: string;
    district?: string;
    settlementMethod?: string;
    contactName?: string;
    contactPhone?: string;
    faxNumber?: string;
    officeAddress?: string;
    agreementName?: string;
    rating?: number;
    status?: Status;
    remark?: string;
  }
}
export function getSupplierPage(params: SupplierApi.QueryParams){return requestClient.get<SupplierApi.PageResult<SupplierApi.Item>>('/purchase/supplier/page',{params});}
export function getSupplierAll(category?: SupplierApi.Category){return requestClient.get<SupplierApi.Item[]>('/purchase/supplier/all',{params:{category}});}
export function createSupplier(data: SupplierApi.SaveParams){return requestClient.post<SupplierApi.Item>('/purchase/supplier/create',data);}
export function updateSupplier(id:number,data:SupplierApi.SaveParams){return requestClient.post<SupplierApi.Item>('/purchase/supplier/update',data,{params:{id}});}
export function deleteSupplier(id:number){return requestClient.post<void>('/purchase/supplier/delete',{}, {params:{id}});}
