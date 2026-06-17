import { requestClient } from '#/api/request';

export namespace EnterpriseProductDictionaryApi {
  export type DictType = 'business_type' | 'product_theme' | 'reception_standard';

  export type Status = 'active' | 'disabled';

  export interface Item {
    id: number;
    dictType: DictType;
    dictName: string;
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
    dictType?: DictType;
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: Status;
  }

  export interface SaveParams {
    dictType: DictType;
    dictName: string;
    sortOrder?: number;
    status?: Status;
    remark?: string;
  }
}

export function getProductDictionaryPage(params: EnterpriseProductDictionaryApi.QueryParams) {
  return requestClient.get<
    EnterpriseProductDictionaryApi.PageResult<EnterpriseProductDictionaryApi.Item>
  >('/enterprise/product-dictionary/page', { params });
}

export function getProductDictionaryAll(dictType?: EnterpriseProductDictionaryApi.DictType) {
  return requestClient.get<EnterpriseProductDictionaryApi.Item[]>(
    '/enterprise/product-dictionary/all',
    { params: { dictType } },
  );
}

export function createProductDictionary(data: EnterpriseProductDictionaryApi.SaveParams) {
  return requestClient.post<EnterpriseProductDictionaryApi.Item>(
    '/enterprise/product-dictionary/create',
    data,
  );
}

export function updateProductDictionary(
  id: number,
  data: EnterpriseProductDictionaryApi.SaveParams,
) {
  return requestClient.post<EnterpriseProductDictionaryApi.Item>(
    '/enterprise/product-dictionary/update',
    data,
    { params: { id } },
  );
}

export function deleteProductDictionary(id: number) {
  return requestClient.post<void>(
    '/enterprise/product-dictionary/delete',
    {},
    { params: { id } },
  );
}
