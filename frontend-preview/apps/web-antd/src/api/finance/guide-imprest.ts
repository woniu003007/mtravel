import { requestClient } from '#/api/request';

export namespace GuideImprestApi {
  export type Status =
    | 'cancelled'
    | 'draft'
    | 'manager_approved'
    | 'manager_rejected'
    | 'paid'
    | 'pending_manager'
    | 'settled';

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface CalcLine {
    amount?: number;
    arrangementType?: string;
    companyMarkupRate?: number;
    costPrice?: number;
    guestCount?: number;
    guideCommissionAmount?: number;
    guideCommissionCalcType?: string;
    guideCommissionRate?: number;
    itemName?: string;
    lineType?: 'cash_cost' | 'optional_deduction';
    salePrice?: number;
    sortOrder?: number;
  }

  export interface Imprest {
    applicant?: string;
    appliedAt?: string;
    approvalRemark?: string;
    approvedAmount?: number;
    approvedAt?: string;
    approvedBy?: string;
    balanceAmount?: number;
    businessType?: string;
    calculatedAmount?: number;
    calculationChanged?: boolean;
    calculationChangeMessage?: string;
    cancelledAt?: string;
    cancelledBy?: string;
    cancelReason?: string;
    cashCostAmount?: number;
    calcLines?: CalcLine[];
    createdAt?: string;
    currentCalculatedAmount?: number;
    currentCashCostAmount?: number;
    currentGuestCount?: number;
    currentGuideTurnInAmount?: number;
    currentOptionalDeductionAmount?: number;
    currentSuggestedImprestAmount?: number;
    departmentId?: number;
    departmentName?: string;
    departureDate?: string;
    guestCount?: number;
    guideId: number;
    guideMobile?: string;
    guideName?: string;
    guideTurnInAmount?: number;
    id: number;
    operatorEmployeeId?: number;
    operatorEmployeeName?: string;
    optionalDeductionAmount?: number;
    availableAuthorizationAmount?: number;
    occupiedAuthorizationAmount?: number;
    paidAmount?: number;
    rejectedAt?: string;
    rejectedBy?: string;
    remark?: string;
    requestNo: string;
    requestedAmount?: number;
    status: Status;
    suggestedImprestAmount?: number;
    teamId: number;
    teamNo?: string;
    teamType?: string;
  }

  export interface Preview {
    calculatedAmount?: number;
    cashCostAmount?: number;
    calcLines: CalcLine[];
    companyMarkupRate?: number;
    guestCount?: number;
    guideId: number;
    guideName?: string;
    guideTurnInAmount?: number;
    optionalDeductionAmount?: number;
    availableAuthorizationAmount?: number;
    occupiedAuthorizationAmount?: number;
    suggestedImprestAmount?: number;
    teamId: number;
    teamNo?: string;
  }

  export interface QueryParams {
    guideId?: number;
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: Status;
    teamId?: number;
  }

  export interface ApplyParams {
    companyMarkupRate?: number;
    guideId: number;
    remark?: string;
    requestedAmount?: number;
    teamId: number;
  }

  export interface DecisionParams {
    approvalRemark?: string;
  }

  export interface PaymentParams {
    amount: number;
    paymentAccountName?: string;
    paymentDate: string;
    paymentMethod?: string;
    remark?: string;
  }

  export interface CancelParams {
    cancelReason: string;
  }
}

export function previewGuideImprest(params: { companyMarkupRate?: number; guideId: number; teamId: number }) {
  return requestClient.get<GuideImprestApi.Preview>(
    '/finance/guide-imprests/preview',
    { params },
  );
}

export function submitGuideImprest(data: GuideImprestApi.ApplyParams) {
  return requestClient.post<GuideImprestApi.Imprest>(
    '/finance/guide-imprests/submit',
    data,
  );
}

export function getGuideImprestPage(params: GuideImprestApi.QueryParams) {
  return requestClient.get<
    GuideImprestApi.PageResult<GuideImprestApi.Imprest>
  >('/finance/guide-imprests/page', { params });
}

export function getGuideImprestDetail(id: number) {
  return requestClient.get<GuideImprestApi.Imprest>(
    '/finance/guide-imprests/detail',
    { params: { id } },
  );
}

export function approveGuideImprest(
  id: number,
  data: GuideImprestApi.DecisionParams,
) {
  return requestClient.post<GuideImprestApi.Imprest>(
    '/finance/guide-imprests/approve',
    data,
    { params: { id } },
  );
}

export function rejectGuideImprest(
  id: number,
  data: GuideImprestApi.DecisionParams,
) {
  return requestClient.post<GuideImprestApi.Imprest>(
    '/finance/guide-imprests/reject',
    data,
    { params: { id } },
  );
}

export function registerGuideImprestPayment(
  id: number,
  data: GuideImprestApi.PaymentParams,
) {
  return requestClient.post<GuideImprestApi.Imprest>(
    '/finance/guide-imprests/payment',
    data,
    { params: { id } },
  );
}

export function cancelGuideImprest(
  id: number,
  data: GuideImprestApi.CancelParams,
) {
  return requestClient.post<GuideImprestApi.Imprest>(
    '/finance/guide-imprests/cancel',
    data,
    { params: { id } },
  );
}
