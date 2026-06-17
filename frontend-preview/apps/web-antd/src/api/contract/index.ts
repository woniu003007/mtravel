import { requestClient } from '#/api/request';

export namespace ContractApi {
  export type ContractType =
    | 'customer'
    | 'current_refund'
    | 'extra_fee'
    | 'finance_fee'
    | 'ground_agent'
    | 'guide'
    | 'hotel'
    | 'other'
    | 'restaurant'
    | 'scenic'
    | 'shopping'
    | 'traffic'
    | 'vehicle';
  export type Status = 'active' | 'disabled' | 'terminated';

  export interface Item {
    agreementContent?: string;
    attachmentId?: number;
    contractFileUrl?: string;
    contractName: string;
    contractNo: string;
    contractType: ContractType;
    counterpartyName?: string;
    createdAt?: string;
    createdBy?: string;
    customerId?: number;
    endDate?: string;
    id: number;
    invoiceSubject?: string;
    legalSubject?: string;
    otherContent?: string;
    partyAAddress?: string;
    partyAContact?: string;
    partyAFax?: string;
    partyAName?: string;
    partyAPhone?: string;
    partyBAddress?: string;
    partyBContact?: string;
    partyBFax?: string;
    partyBName?: string;
    partyBPhone?: string;
    printStatus?: string;
    purchasePriceSummary?: string;
    reminderDays?: number;
    remark?: string;
    settlementSubject?: string;
    settlementTerms?: string;
    startDate?: string;
    status: Status;
    supplierId?: number;
    templateName?: string;
    updatedAt?: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    contractType?: ContractType;
    customerId?: number;
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: Status;
    supplierId?: number;
  }

  export type SaveParams = Omit<Item, 'createdAt' | 'createdBy' | 'id' | 'updatedAt'>;
}

/** 查询统一合同台账。 */
export function getContractPage(params: ContractApi.QueryParams) {
  return requestClient.get<ContractApi.PageResult<ContractApi.Item>>(
    '/contracts/page',
    { params },
  );
}

/** 按合同类型生成下一合同编号。 */
export function getNextContractNo(contractType: ContractApi.ContractType) {
  return requestClient.get<string>('/contracts/next-no', {
    params: { contractType },
  });
}

/** 新增合同。 */
export function createContract(data: ContractApi.SaveParams) {
  return requestClient.post<ContractApi.Item>('/contracts/create', data);
}

/** 修改合同。 */
export function updateContract(id: number, data: ContractApi.SaveParams) {
  return requestClient.post<ContractApi.Item>('/contracts/update', data, {
    params: { id },
  });
}

/** 软删除合同。 */
export function deleteContract(id: number) {
  return requestClient.post<void>('/contracts/delete', {}, { params: { id } });
}
