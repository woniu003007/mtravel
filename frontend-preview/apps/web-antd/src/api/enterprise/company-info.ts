import { requestClient } from '#/api/request';

export namespace EnterpriseCompanyInfoApi {
  export type SignStatus = 'signed' | 'unsigned';
  export type Status = 'active' | 'disabled';

  export interface Item {
    id: number;
    companyName: string;
    province?: string;
    city?: string;
    district?: string;
    contactName?: string;
    contactPhone?: string;
    faxNumber?: string;
    officeAddress?: string;
    alipayEnterpriseName?: string;
    alipayAccount?: string;
    alipayNickname?: string;
    signStatus: SignStatus;
    signLink?: string;
    status: Status;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface SaveParams {
    alipayAccount?: string;
    alipayEnterpriseName?: string;
    alipayNickname?: string;
    city?: string;
    companyName: string;
    contactName?: string;
    contactPhone?: string;
    district?: string;
    faxNumber?: string;
    officeAddress?: string;
    province?: string;
    remark?: string;
    signLink?: string;
    signStatus?: SignStatus;
    status?: Status;
  }
}

/** 查询当前企业公司信息；未维护时后端返回 null。 */
export function getEnterpriseCompanyInfoCurrent() {
  return requestClient.get<EnterpriseCompanyInfoApi.Item | null>(
    '/enterprise/company-info/current',
  );
}

/** 保存当前企业公司信息，同一租户只维护一份未删除记录。 */
export function saveEnterpriseCompanyInfo(
  data: EnterpriseCompanyInfoApi.SaveParams,
) {
  return requestClient.post<EnterpriseCompanyInfoApi.Item>(
    '/enterprise/company-info/save',
    data,
  );
}
