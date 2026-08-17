import { requestClient } from '#/api/request';

/** 普通资源报价规则接口类型与请求封装。 */
export namespace PurchaseResourceQuoteRuleApi {
  export type ResourceType =
    | 'ground_agent'
    | 'guide'
    | 'hotel'
    | 'other'
    | 'restaurant'
    | 'scenic'
    | 'shopping'
    | 'ticket'
    | 'vehicle';
  export type Status = 'active' | 'disabled';

  /** 资源类型与客户等级组合的一条报价上浮规则。 */
  export interface Item {
    createdAt?: string;
    createdBy?: string;
    customerLevelId: null | number;
    customerLevelName?: null | string;
    id: number;
    minimumFixedAddon: number;
    minimumRate: number;
    remark?: string;
    resourceType: ResourceType;
    status: Status;
    suggestedFixedAddon: number;
    suggestedRate: number;
    updatedAt?: string;
  }

  /** 分页查询资源报价规则时允许使用的业务筛选条件。 */
  export interface QueryParams {
    customerLevelId?: number;
    page?: number;
    pageSize?: number;
    resourceType?: ResourceType;
    status?: Status;
  }

  /** 新增或修改资源报价规则的保存载荷。 */
  export interface SaveParams {
    customerLevelId: null | number;
    minimumFixedAddon: number;
    minimumRate: number;
    remark?: string;
    resourceType: ResourceType;
    status: Status;
    suggestedFixedAddon: number;
    suggestedRate: number;
  }

  /** 与后端分页列表响应保持一致的通用结构。 */
  export interface PageResult<T> {
    items: T[];
    total: number;
  }
}

/** 分页查询普通资源报价规则。 */
export function getPurchaseResourceQuoteRulePage(
  params: PurchaseResourceQuoteRuleApi.QueryParams,
) {
  return requestClient.get<
    PurchaseResourceQuoteRuleApi.PageResult<PurchaseResourceQuoteRuleApi.Item>
  >('/purchase/resource-quote-rules/page', { params });
}

/** 新增一条普通资源报价规则。 */
export function createPurchaseResourceQuoteRule(
  data: PurchaseResourceQuoteRuleApi.SaveParams,
) {
  return requestClient.post<PurchaseResourceQuoteRuleApi.Item>(
    '/purchase/resource-quote-rules/create',
    data,
  );
}

/** 修改指定普通资源报价规则。 */
export function updatePurchaseResourceQuoteRule(
  id: number,
  data: PurchaseResourceQuoteRuleApi.SaveParams,
) {
  return requestClient.post<PurchaseResourceQuoteRuleApi.Item>(
    '/purchase/resource-quote-rules/update',
    data,
    { params: { id } },
  );
}

/** 软删除指定普通资源报价规则。 */
export function deletePurchaseResourceQuoteRule(id: number) {
  return requestClient.post<void>(
    '/purchase/resource-quote-rules/delete',
    {},
    { params: { id } },
  );
}
