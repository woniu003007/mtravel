import { requestClient } from '#/api/request';

export namespace CustomerRiskApprovalApi {
  export type ApprovalStatus = 'approved' | 'cancelled' | 'pending' | 'rejected';

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface CheckParams {
    customerId: number;
    orderId?: number;
    requestedAmount?: number;
    teamId?: number;
  }

  export interface CheckResult {
    approvalEnabled: boolean;
    availableAmount?: number;
    blocked: boolean;
    contractExpireDate?: string;
    contractExpired: boolean;
    creditLimit?: number;
    creditOverLimit: boolean;
    customerId: number;
    customerName?: string;
    occupiedAmount?: number;
    orderId?: number;
    overLimitAmount?: number;
    pendingApprovalAmount?: number;
    requestedAmount?: number;
    riskApprovalRequestId?: number;
    riskApprovalRequestNo?: string;
    riskApprovalRequestedAmount?: number;
    riskApprovalStatus?: ApprovalStatus;
    riskSummary?: string;
    riskTypes: string[];
    teamId?: number;
  }

  export interface ApplyParams {
    customerId: number;
    orderId?: number;
    remark?: string;
    requestedAmount?: number;
    teamId?: number;
  }

  export interface Approval {
    applicant?: string;
    approvalRemark?: string;
    approvedAt?: string;
    approvedBy?: string;
    availableAmount?: number;
    contractExpireDate?: string;
    createdAt?: string;
    creditLimit?: number;
    customerId: number;
    customerName?: string;
    id: number;
    occupiedAmount?: number;
    orderId?: number;
    overLimitAmount?: number;
    pendingApprovalAmount?: number;
    rejectedAt?: string;
    rejectedBy?: string;
    remark?: string;
    requestNo: string;
    requestedAmount?: number;
    riskSummary?: string;
    riskTypes: string[];
    status: ApprovalStatus;
    teamId?: number;
    updatedAt?: string;
  }

  export interface QueryParams {
    customerId?: number;
    keyword?: string;
    orderId?: number;
    page?: number;
    pageSize?: number;
    status?: ApprovalStatus;
    teamId?: number;
  }

  export interface DecisionParams {
    approvalRemark?: string;
  }
}

export function checkCustomerRisk(params: CustomerRiskApprovalApi.CheckParams) {
  return requestClient.get<CustomerRiskApprovalApi.CheckResult>(
    '/customer/risk-approval/check',
    { params },
  );
}

export function applyCustomerRiskApproval(
  data: CustomerRiskApprovalApi.ApplyParams,
) {
  return requestClient.post<CustomerRiskApprovalApi.Approval>(
    '/customer/risk-approval/apply',
    data,
  );
}

export function getCustomerRiskApprovalPage(
  params: CustomerRiskApprovalApi.QueryParams,
) {
  return requestClient.get<
    CustomerRiskApprovalApi.PageResult<CustomerRiskApprovalApi.Approval>
  >('/customer/risk-approval/page', { params });
}

export function getCustomerRiskApprovalDetail(id: number) {
  return requestClient.get<CustomerRiskApprovalApi.Approval>(
    '/customer/risk-approval/detail',
    { params: { id } },
  );
}

export function approveCustomerRiskApproval(
  id: number,
  data: CustomerRiskApprovalApi.DecisionParams,
) {
  return requestClient.post<CustomerRiskApprovalApi.Approval>(
    '/customer/risk-approval/approve',
    data,
    { params: { id } },
  );
}

export function rejectCustomerRiskApproval(
  id: number,
  data: CustomerRiskApprovalApi.DecisionParams,
) {
  return requestClient.post<CustomerRiskApprovalApi.Approval>(
    '/customer/risk-approval/reject',
    data,
    { params: { id } },
  );
}
