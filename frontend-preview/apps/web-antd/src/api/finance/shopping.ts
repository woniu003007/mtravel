import { requestClient } from '#/api/request';

export namespace ShoppingCommissionApi {
  export interface Rule {
    baseCommissionRate?: number;
    ladderCalcMode?: 'full_amount_diff';
    overrideReason?: string;
    ruleSource?: 'default_rule' | 'system_default' | 'team_override';
    targetCommissionRate?: number;
    thresholdPerCapitaAmount?: number;
  }

  export interface FeedbackLine {
    businessDate?: string;
    companyRebateAmount?: number;
    consumptionAmount?: number;
    detailLines?: FeedbackDetailLine[];
    feedbackSource?: 'api' | 'excel' | 'manual';
    guideCommissionAmount?: number;
    guideId?: number;
    guideName?: string;
    headFeeAmount?: number;
    id?: number;
    peopleCount?: number;
    rebateCalcMode?: RebateCalcMode;
    remark?: string;
    shopName?: string;
    supplierId?: number;
  }

  export type RebateCalcMode = 'category' | 'total';

  export interface FeedbackDetailLine {
    cashAmount?: number;
    categoryName?: string;
    companyRebateAmount?: number;
    companyRebateRate?: number;
    consumptionAmount?: number;
    guideCommissionAmount?: number;
    guideCommissionRate?: number;
    headFeeAmount?: number;
    id?: number;
    peopleCount?: number;
    remark?: string;
    sortOrder?: number;
  }

  export interface FeedbackLineSaveParams {
    businessDate?: string;
    companyRebateAmount?: number;
    consumptionAmount?: number;
    detailLines?: FeedbackDetailLine[];
    guideCommissionAmount?: number;
    guideId?: number;
    guideName?: string;
    headFeeAmount?: number;
    id?: number;
    peopleCount?: number;
    rebateCalcMode?: RebateCalcMode;
    remark?: string;
    shopName: string;
    supplierId?: number;
  }

  export interface RuleSaveParams {
    baseCommissionRate?: number;
    overrideReason?: string;
    targetCommissionRate?: number;
    thresholdPerCapitaAmount?: number;
  }

  export interface SettlementCalculateParams {
    manualGuideBonusAmount?: number;
    manualGuideBonusRemark?: string;
  }

  export interface SettlementLine {
    businessDate?: string;
    companyRebateAmount?: number;
    consumptionAmount?: number;
    feedbackLineId?: number;
    guideCommissionAmount?: number;
    headFeeAmount?: number;
    lineCompanyProfitAmount?: number;
    peopleCount?: number;
    shopName?: string;
  }

  export interface Settlement {
    baseCommissionRate?: number;
    baseGuideCommissionAmount?: number;
    calculatedAt?: string;
    calculatedBy?: string;
    companyProfitAmount?: number;
    companyRebateAmount?: number;
    externalCompanyProfitAmount?: number;
    guestCount?: number;
    guideCommissionTotalAmount?: number;
    headFeeAmount?: number;
    id?: number;
    ladderExtraCommissionAmount?: number;
    lines?: SettlementLine[];
    manualGuideBonusAmount?: number;
    manualGuideBonusRemark?: string;
    perCapitaConsumptionAmount?: number;
    ruleSource?: string;
    targetCommissionRate?: number;
    teamId?: number;
    teamNo?: string;
    thresholdPerCapitaAmount?: number;
    thresholdReached?: boolean;
    totalConsumptionAmount?: number;
  }

  export interface Overview {
    feedbackLines: FeedbackLine[];
    latestSettlement?: Settlement;
    rule: Rule;
  }
}

export function getShoppingCommissionOverview(teamId: number) {
  return requestClient.get<ShoppingCommissionApi.Overview>(
    `/finance/shopping/team/${teamId}/overview`,
  );
}

export function saveShoppingRuleOverride(
  teamId: number,
  data: ShoppingCommissionApi.RuleSaveParams,
) {
  return requestClient.post<ShoppingCommissionApi.Rule>(
    `/finance/shopping/team/${teamId}/rule-override`,
    data,
  );
}

export function saveShoppingFeedbackLine(
  teamId: number,
  data: ShoppingCommissionApi.FeedbackLineSaveParams,
) {
  return requestClient.post<ShoppingCommissionApi.FeedbackLine>(
    `/finance/shopping/team/${teamId}/feedback-lines`,
    data,
  );
}

export function cancelShoppingFeedbackLine(teamId: number, id: number) {
  return requestClient.post<void>(
    `/finance/shopping/team/${teamId}/feedback-lines/${id}/cancel`,
  );
}

export function calculateShoppingSettlement(
  teamId: number,
  data?: ShoppingCommissionApi.SettlementCalculateParams,
) {
  return requestClient.post<ShoppingCommissionApi.Settlement>(
    `/finance/shopping/team/${teamId}/calculate`,
    data,
  );
}
