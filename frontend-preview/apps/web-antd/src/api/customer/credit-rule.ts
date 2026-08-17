import { requestClient } from '#/api/request';

/** 客户授信规则接口类型与请求封装。 */
export namespace CustomerCreditRuleApi {
  export type Status = 'active' | 'disabled';

  /** 客户等级对应的一条授信审批规则。 */
  export interface Item {
    allowOverLimit: boolean;
    approverEmployeeIds: number[];
    approverNames?: string[];
    ccEmployeeIds: number[];
    ccNames?: string[];
    createdAt?: string;
    createdBy?: string;
    creditLimit: number;
    customerLevelId: number;
    customerLevelName: string;
    id: number;
    paymentTermDays: number;
    status: Status;
    updatedAt?: string;
  }

  /** 分页查询授信规则时允许使用的业务筛选条件。 */
  export interface QueryParams {
    customerLevelId?: number;
    page?: number;
    pageSize?: number;
    status?: Status;
  }

  /** 新增或修改授信规则的保存载荷。 */
  export interface SaveParams {
    allowOverLimit: boolean;
    approverEmployeeIds: number[];
    ccEmployeeIds: number[];
    creditLimit: number;
    customerLevelId: number;
    paymentTermDays: number;
    status: Status;
  }

  /** 与后端分页列表响应保持一致的通用结构。 */
  export interface PageResult<T> {
    items: T[];
    total: number;
  }
}

/** 分页查询客户授信规则，后端按授信额度升序返回。 */
export function getCustomerCreditRulePage(
  params: CustomerCreditRuleApi.QueryParams,
) {
  return requestClient.get<
    CustomerCreditRuleApi.PageResult<CustomerCreditRuleApi.Item>
  >('/customer/credit-rules/page', { params });
}

/** 新增一条客户授信规则。 */
export function createCustomerCreditRule(
  data: CustomerCreditRuleApi.SaveParams,
) {
  return requestClient.post<CustomerCreditRuleApi.Item>(
    '/customer/credit-rules/create',
    data,
  );
}

/** 修改指定客户授信规则。 */
export function updateCustomerCreditRule(
  id: number,
  data: CustomerCreditRuleApi.SaveParams,
) {
  return requestClient.post<CustomerCreditRuleApi.Item>(
    '/customer/credit-rules/update',
    data,
    { params: { id } },
  );
}

/** 软删除指定客户授信规则。 */
export function deleteCustomerCreditRule(id: number) {
  return requestClient.post<void>(
    '/customer/credit-rules/delete',
    {},
    { params: { id } },
  );
}
