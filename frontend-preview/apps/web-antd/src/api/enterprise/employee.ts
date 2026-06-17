import { requestClient } from '#/api/request';

export namespace EnterpriseEmployeeApi {
  export type Gender = 'female' | 'male' | 'unknown';
  export type Scope = 'company' | 'department' | 'personal';
  export type Status = 'active' | 'disabled';

  export interface Item {
    id: number;
    systemUserId?: number;
    employeeCode?: string;
    employeeName: string;
    username: string;
    departmentId: number;
    departmentName?: string;
    roleId: number;
    roleCode?: string;
    roleName?: string;
    gender: Gender;
    telephone?: string;
    mobilePhone?: string;
    email?: string;
    infoScope: Scope;
    profitScope: Scope;
    receptionScope: Scope;
    customerScope: Scope;
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
    departmentId?: number;
    keyword?: string;
    page?: number;
    pageSize?: number;
    roleId?: number;
    status?: Status;
  }

  export interface SaveParams {
    customerScope?: Scope;
    departmentId: number;
    email?: string;
    employeeCode?: string;
    employeeName: string;
    gender?: Gender;
    infoScope?: Scope;
    mobilePhone?: string;
    profitScope?: Scope;
    receptionScope?: Scope;
    remark?: string;
    roleId: number;
    sortOrder?: number;
    status?: Status;
    telephone?: string;
    username: string;
  }
}

/** 分页查询企业员工。 */
export function getEnterpriseEmployeePage(
  params: EnterpriseEmployeeApi.QueryParams,
) {
  return requestClient.get<
    EnterpriseEmployeeApi.PageResult<EnterpriseEmployeeApi.Item>
  >('/enterprise/employee/page', { params });
}

/** 查询员工下拉列表，用于客户单位操作计调、业务员工等业务选择。 */
export function getEnterpriseEmployeeAll(includeDisabled = false) {
  return requestClient.get<EnterpriseEmployeeApi.Item[]>(
    '/enterprise/employee/all',
    { params: { includeDisabled } },
  );
}

/** 新增员工并创建登录账号。 */
export function createEnterpriseEmployee(
  data: EnterpriseEmployeeApi.SaveParams,
) {
  return requestClient.post<EnterpriseEmployeeApi.Item>(
    '/enterprise/employee/create',
    data,
  );
}

/** 修改员工资料并同步登录账号基础信息。 */
export function updateEnterpriseEmployee(
  id: number,
  data: EnterpriseEmployeeApi.SaveParams,
) {
  return requestClient.post<EnterpriseEmployeeApi.Item>(
    '/enterprise/employee/update',
    data,
    { params: { id } },
  );
}

/** 停用员工并停用登录账号。 */
export function disableEnterpriseEmployee(id: number) {
  return requestClient.post<void>(
    '/enterprise/employee/disable',
    {},
    { params: { id } },
  );
}

/** 重置员工登录密码为默认初始密码。 */
export function resetEnterpriseEmployeePassword(id: number) {
  return requestClient.post<void>(
    '/enterprise/employee/reset-password',
    {},
    { params: { id } },
  );
}

/** 软删除员工并同步软删除登录账号。 */
export function deleteEnterpriseEmployee(id: number) {
  return requestClient.post<void>(
    '/enterprise/employee/delete',
    {},
    { params: { id } },
  );
}
