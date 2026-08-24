import { requestClient } from '#/api/request';

export namespace EnterpriseDepartmentApi {
  export type Status = 'active' | 'disabled';

  export interface Item {
    id: number;
    parentId?: number;
    parentName?: string;
    departmentCode?: string;
    departmentName: string;
    managerEmployeeId?: number;
    managerName?: string;
    contactPhone?: string;
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
    parentId?: number;
    status?: Status;
  }

  export interface SaveParams {
    contactPhone?: string;
    departmentCode?: string;
    departmentName: string;
    managerEmployeeId?: number;
    managerName?: string;
    parentId?: number;
    remark?: string;
    sortOrder?: number;
    status?: Status;
  }
}

/** 分页查询企业部门。 */
export function getEnterpriseDepartmentPage(
  params: EnterpriseDepartmentApi.QueryParams,
) {
  return requestClient.get<
    EnterpriseDepartmentApi.PageResult<EnterpriseDepartmentApi.Item>
  >('/enterprise/department/page', { params });
}

/** 查询企业部门列表，用于上级部门和员工所属部门下拉选择。 */
export function getEnterpriseDepartmentAll(includeDisabled = false) {
  return requestClient.get<EnterpriseDepartmentApi.Item[]>(
    '/enterprise/department/all',
    { params: { includeDisabled } },
  );
}

/** 新增企业部门。 */
export function createEnterpriseDepartment(
  data: EnterpriseDepartmentApi.SaveParams,
) {
  return requestClient.post<EnterpriseDepartmentApi.Item>(
    '/enterprise/department/create',
    data,
  );
}

/** 修改企业部门。 */
export function updateEnterpriseDepartment(
  id: number,
  data: EnterpriseDepartmentApi.SaveParams,
) {
  return requestClient.post<EnterpriseDepartmentApi.Item>(
    '/enterprise/department/update',
    data,
    { params: { id } },
  );
}

/** 软删除企业部门。 */
export function deleteEnterpriseDepartment(id: number) {
  return requestClient.post<void>(
    '/enterprise/department/delete',
    {},
    { params: { id } },
  );
}
