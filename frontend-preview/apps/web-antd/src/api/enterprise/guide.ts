import { requestClient } from '#/api/request';

export namespace EnterpriseGuideApi {
  export type EnterpriseCodeStatus =
    | 'bound'
    | 'disabled'
    | 'invite_link'
    | 'not_joined'
    | 'signed_success'
    | 'unbound';
  export type Gender = 'female' | 'male' | 'unknown';
  export type Status = 'active' | 'disabled';
  export type TagStatus = 'active' | 'disabled';

  export interface TagItem {
    createdAt?: string;
    createdBy?: string;
    id: number;
    remark?: string;
    sortOrder: number;
    status: TagStatus;
    tagName: string;
    updatedAt?: string;
  }

  export interface Item {
    age?: number;
    alipayAccount?: string;
    alipayName?: string;
    bankAccountNo?: string;
    bankName?: string;
    certificateFileUrl?: string;
    certificateNo?: string;
    createdAt?: string;
    createdBy?: string;
    enterpriseCodeAccount?: string;
    enterpriseCodeInvitedAt?: string;
    enterpriseCodeStatus: EnterpriseCodeStatus;
    fax?: string;
    gender: Gender;
    guideCode?: string;
    guideLevelId?: number;
    guideLevelName?: string;
    guideManagerEmployeeId?: number;
    guideManagerName?: string;
    guideName: string;
    id: number;
    idCardNo?: string;
    languages?: string;
    mobilePhone?: string;
    nativePlace?: string;
    personalIntro?: string;
    photoUrl?: string;
    rating: number;
    remark?: string;
    sortOrder: number;
    status: Status;
    tagIds: number[];
    tags: TagItem[];
    telephone?: string;
    totalTours: number;
    updatedAt?: string;
    username?: string;
    workingYears?: number;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    enterpriseCodeStatus?: EnterpriseCodeStatus;
    guideManagerEmployeeId?: number;
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: Status;
    tagId?: number;
  }

  export interface SaveParams {
    age?: number;
    alipayAccount?: string;
    alipayName?: string;
    bankAccountNo?: string;
    bankName?: string;
    certificateFileUrl?: string;
    certificateNo?: string;
    enterpriseCodeAccount?: string;
    enterpriseCodeStatus?: EnterpriseCodeStatus;
    fax?: string;
    gender?: Gender;
    guideCode?: string;
    guideLevelId?: number;
    guideManagerEmployeeId?: number;
    guideName: string;
    idCardNo?: string;
    languages?: string;
    mobilePhone?: string;
    nativePlace?: string;
    personalIntro?: string;
    photoUrl?: string;
    rating?: number;
    remark?: string;
    sortOrder?: number;
    status?: Status;
    tagIds?: number[];
    telephone?: string;
    totalTours?: number;
    username?: string;
    workingYears?: number;
  }

  export interface TagQueryParams {
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: TagStatus;
  }

  export interface TagSaveParams {
    remark?: string;
    sortOrder?: number;
    status?: TagStatus;
    tagName: string;
  }
}

/** 分页查询企业导游档案。 */
export function getEnterpriseGuidePage(params: EnterpriseGuideApi.QueryParams) {
  return requestClient.get<
    EnterpriseGuideApi.PageResult<EnterpriseGuideApi.Item>
  >('/enterprise/guide/page', { params });
}

/** 查询导游列表，用于团队安排和导游排班下拉选择。 */
export function getEnterpriseGuideAll(includeDisabled = false) {
  return requestClient.get<EnterpriseGuideApi.Item[]>('/enterprise/guide/all', {
    params: { includeDisabled },
  });
}

/** 新增导游档案。 */
export function createEnterpriseGuide(data: EnterpriseGuideApi.SaveParams) {
  return requestClient.post<EnterpriseGuideApi.Item>(
    '/enterprise/guide/create',
    data,
  );
}

/** 修改导游档案。 */
export function updateEnterpriseGuide(
  id: number,
  data: EnterpriseGuideApi.SaveParams,
) {
  return requestClient.post<EnterpriseGuideApi.Item>(
    '/enterprise/guide/update',
    data,
    { params: { id } },
  );
}

/** 发送企业码邀请，后台记录已获取签约链接状态。 */
export function sendEnterpriseGuideCodeInvite(id: number) {
  return requestClient.post<void>(
    '/enterprise/guide/send-enterprise-code-invite',
    {},
    { params: { id } },
  );
}

/** 停用导游档案。 */
export function disableEnterpriseGuide(id: number) {
  return requestClient.post<void>(
    '/enterprise/guide/disable',
    {},
    { params: { id } },
  );
}

/** 软删除导游档案。 */
export function deleteEnterpriseGuide(id: number) {
  return requestClient.post<void>(
    '/enterprise/guide/delete',
    {},
    { params: { id } },
  );
}

/** 分页查询导游标签。 */
export function getEnterpriseGuideTagPage(
  params: EnterpriseGuideApi.TagQueryParams,
) {
  return requestClient.get<
    EnterpriseGuideApi.PageResult<EnterpriseGuideApi.TagItem>
  >('/enterprise/guide/tags/page', { params });
}

/** 查询启用导游标签，用于导游档案多选。 */
export function getEnterpriseGuideTagAll() {
  return requestClient.get<EnterpriseGuideApi.TagItem[]>(
    '/enterprise/guide/tags/all',
  );
}

/** 新增导游标签。 */
export function createEnterpriseGuideTag(
  data: EnterpriseGuideApi.TagSaveParams,
) {
  return requestClient.post<EnterpriseGuideApi.TagItem>(
    '/enterprise/guide/tags/create',
    data,
  );
}

/** 修改导游标签。 */
export function updateEnterpriseGuideTag(
  id: number,
  data: EnterpriseGuideApi.TagSaveParams,
) {
  return requestClient.post<EnterpriseGuideApi.TagItem>(
    '/enterprise/guide/tags/update',
    data,
    { params: { id } },
  );
}

/** 软删除导游标签。 */
export function deleteEnterpriseGuideTag(id: number) {
  return requestClient.post<void>(
    '/enterprise/guide/tags/delete',
    {},
    { params: { id } },
  );
}
