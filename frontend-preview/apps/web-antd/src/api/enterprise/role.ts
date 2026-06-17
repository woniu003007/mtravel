import { requestClient } from '#/api/request';

export namespace EnterpriseRoleApi {
  export type PermissionType = 'button' | 'data' | 'menu';
  export type Status = 'active' | 'disabled';

  export interface Item {
    id: number;
    roleCode: string;
    roleName: string;
    sortOrder: number;
    systemBuiltin: boolean;
    status: Status;
    employeeCount: number;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface PermissionItem {
    id?: number;
    roleId?: number;
    moduleCode: string;
    moduleName: string;
    permissionCode: string;
    permissionName: string;
    permissionType: PermissionType;
    sortOrder?: number;
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
    remark?: string;
    roleCode: string;
    roleName: string;
    sortOrder?: number;
    status?: Status;
  }
}

/** 分页查询企业角色。 */
export function getEnterpriseRolePage(params: EnterpriseRoleApi.QueryParams) {
  return requestClient.get<
    EnterpriseRoleApi.PageResult<EnterpriseRoleApi.Item>
  >('/enterprise/role/page', { params });
}

/** 查询角色列表，用于员工角色下拉选择。 */
export function getEnterpriseRoleAll(includeDisabled = false) {
  return requestClient.get<EnterpriseRoleApi.Item[]>('/enterprise/role/all', {
    params: { includeDisabled },
  });
}

/** 新增企业角色。 */
export function createEnterpriseRole(data: EnterpriseRoleApi.SaveParams) {
  return requestClient.post<EnterpriseRoleApi.Item>(
    '/enterprise/role/create',
    data,
  );
}

/** 修改企业角色。 */
export function updateEnterpriseRole(
  id: number,
  data: EnterpriseRoleApi.SaveParams,
) {
  return requestClient.post<EnterpriseRoleApi.Item>(
    '/enterprise/role/update',
    data,
    { params: { id } },
  );
}

/** 软删除企业角色。 */
export function deleteEnterpriseRole(id: number) {
  return requestClient.post<void>(
    '/enterprise/role/delete',
    {},
    { params: { id } },
  );
}

/** 查询角色已分配权限。 */
export function getEnterpriseRolePermissions(id: number) {
  return requestClient.get<EnterpriseRoleApi.PermissionItem[]>(
    '/enterprise/role/permissions',
    { params: { id } },
  );
}

/** 保存角色权限。 */
export function saveEnterpriseRolePermissions(
  id: number,
  permissions: EnterpriseRoleApi.PermissionItem[],
) {
  return requestClient.post<void>(
    '/enterprise/role/permissions',
    { permissions },
    { params: { id } },
  );
}
