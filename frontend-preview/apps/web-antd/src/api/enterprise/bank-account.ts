import { requestClient } from '#/api/request';

export namespace EnterpriseBankAccountApi {
  export type Status = 'active' | 'disabled';

  export interface Item {
    id: number;
    bankName: string;
    accountName: string;
    accountNo: string;
    printEnabled: boolean;
    otherInfo?: string;
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
    status?: Status;
  }

  export interface SaveParams {
    bankName: string;
    accountName: string;
    accountNo: string;
    printEnabled?: boolean;
    otherInfo?: string;
    status?: Status;
    remark?: string;
  }
}

/** 分页查询企业银行账号。 */
export function getEnterpriseBankAccountPage(
  params: EnterpriseBankAccountApi.QueryParams,
) {
  return requestClient.get<
    EnterpriseBankAccountApi.PageResult<EnterpriseBankAccountApi.Item>
  >('/enterprise/bank-account/page', { params });
}

/** 查询启用银行账号，用于业务单据下拉框。 */
export function getEnterpriseBankAccountAll() {
  return requestClient.get<EnterpriseBankAccountApi.Item[]>(
    '/enterprise/bank-account/all',
  );
}

/** 新增企业银行账号。 */
export function createEnterpriseBankAccount(
  data: EnterpriseBankAccountApi.SaveParams,
) {
  return requestClient.post<EnterpriseBankAccountApi.Item>(
    '/enterprise/bank-account/create',
    data,
  );
}

/** 修改企业银行账号。 */
export function updateEnterpriseBankAccount(
  id: number,
  data: EnterpriseBankAccountApi.SaveParams,
) {
  return requestClient.post<EnterpriseBankAccountApi.Item>(
    '/enterprise/bank-account/update',
    data,
    { params: { id } },
  );
}

/** 修改银行账号是否参与打印展示。 */
export function updateEnterpriseBankAccountPrintEnabled(
  id: number,
  printEnabled: boolean,
) {
  return requestClient.post<void>(
    '/enterprise/bank-account/print-enabled',
    {},
    { params: { id, printEnabled } },
  );
}

/** 软删除企业银行账号。 */
export function deleteEnterpriseBankAccount(id: number) {
  return requestClient.post<void>(
    '/enterprise/bank-account/delete',
    {},
    { params: { id } },
  );
}
