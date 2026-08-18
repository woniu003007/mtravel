import { requestClient } from '#/api/request';

export namespace QuoteConfigApi {
  export type ResourceType =
    | 'hotel'
    | 'misc'
    | 'other'
    | 'restaurant'
    | 'scenic'
    | 'transport'
    | 'vehicle';
  export type Status = 'active' | 'disabled';
  export type ResourceQuoteMode = 'both' | 'fixed' | 'rate';

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface ResourceRule {
    id: number;
    resourceType: ResourceType;
    customerCategoryId?: number;
    customerCategoryName?: string;
    quoteMode: ResourceQuoteMode;
    suggestedMarkupRate: number;
    minimumMarkupRate: number;
    suggestedFixedMarkup: number;
    minimumFixedMarkup: number;
    status: Status;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface ResourceRuleQuery {
    customerCategoryId?: number;
    page?: number;
    pageSize?: number;
    resourceType?: ResourceType;
    status?: Status;
  }

  export interface ResourceRuleSaveParams {
    customerCategoryId?: number;
    minimumFixedMarkup?: number;
    minimumMarkupRate?: number;
    quoteMode?: ResourceQuoteMode;
    remark?: string;
    resourceType: ResourceType;
    status?: Status;
    suggestedFixedMarkup?: number;
    suggestedMarkupRate?: number;
  }

  export interface GuideLevel {
    id: number;
    levelName: string;
    sortOrder: number;
    status: Status;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface GuideLevelQuery {
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: Status;
  }

  export interface GuideLevelSaveParams {
    levelName: string;
    remark?: string;
    sortOrder?: number;
    status?: Status;
  }

  export interface GuideRule {
    id: number;
    guideLevelId: number;
    guideLevelName: string;
    language: string;
    baseDailyFee: number;
    foreignLanguageDailyMarkup: number;
    overtimeHourlyFee: number;
    status: Status;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface GuideRuleQuery {
    guideLevelId?: number;
    page?: number;
    pageSize?: number;
    status?: Status;
  }

  export interface GuideRuleSaveParams {
    baseDailyFee?: number;
    foreignLanguageDailyMarkup?: number;
    guideLevelId: number;
    language?: string;
    overtimeHourlyFee?: number;
    remark?: string;
    status?: Status;
  }

  export interface GroundAgentRule {
    id: number;
    minPeople: number;
    maxPeople: number;
    groupPackagePrice: number;
    status: Status;
    remark?: string;
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface GroundAgentRuleQuery {
    page?: number;
    pageSize?: number;
    status?: Status;
  }

  export interface GroundAgentRuleSaveParams {
    groupPackagePrice?: number;
    maxPeople: number;
    minPeople: number;
    remark?: string;
    status?: Status;
  }

  export interface ApprovalMember {
    employeeName?: string;
    id?: number;
    memberType?: 'approver' | 'cc';
    stepOrder?: number;
    systemUserId: number;
    username?: string;
  }

  export interface ApprovalConfig {
    approvers: ApprovalMember[];
    ccUsers: ApprovalMember[];
  }
}

export function getQuoteResourceRulePage(params: QuoteConfigApi.ResourceRuleQuery) {
  return requestClient.get<QuoteConfigApi.PageResult<QuoteConfigApi.ResourceRule>>(
    '/configuration/quote/resource-rules/page',
    { params },
  );
}

export function createQuoteResourceRule(data: QuoteConfigApi.ResourceRuleSaveParams) {
  return requestClient.post<QuoteConfigApi.ResourceRule>(
    '/configuration/quote/resource-rules/create',
    data,
  );
}

export function updateQuoteResourceRule(id: number, data: QuoteConfigApi.ResourceRuleSaveParams) {
  return requestClient.post<QuoteConfigApi.ResourceRule>(
    '/configuration/quote/resource-rules/update',
    data,
    { params: { id } },
  );
}

export function deleteQuoteResourceRule(id: number) {
  return requestClient.post<void>(
    '/configuration/quote/resource-rules/delete',
    {},
    { params: { id } },
  );
}

export function getQuoteGuideLevelPage(params: QuoteConfigApi.GuideLevelQuery) {
  return requestClient.get<QuoteConfigApi.PageResult<QuoteConfigApi.GuideLevel>>(
    '/configuration/quote/guide-levels/page',
    { params },
  );
}

export function getQuoteGuideLevelAll() {
  return requestClient.get<QuoteConfigApi.GuideLevel[]>(
    '/configuration/quote/guide-levels/all',
  );
}

export function createQuoteGuideLevel(data: QuoteConfigApi.GuideLevelSaveParams) {
  return requestClient.post<QuoteConfigApi.GuideLevel>(
    '/configuration/quote/guide-levels/create',
    data,
  );
}

export function updateQuoteGuideLevel(id: number, data: QuoteConfigApi.GuideLevelSaveParams) {
  return requestClient.post<QuoteConfigApi.GuideLevel>(
    '/configuration/quote/guide-levels/update',
    data,
    { params: { id } },
  );
}

export function deleteQuoteGuideLevel(id: number) {
  return requestClient.post<void>(
    '/configuration/quote/guide-levels/delete',
    {},
    { params: { id } },
  );
}

export function getQuoteGuideRulePage(params: QuoteConfigApi.GuideRuleQuery) {
  return requestClient.get<QuoteConfigApi.PageResult<QuoteConfigApi.GuideRule>>(
    '/configuration/quote/guide-rules/page',
    { params },
  );
}

export function createQuoteGuideRule(data: QuoteConfigApi.GuideRuleSaveParams) {
  return requestClient.post<QuoteConfigApi.GuideRule>(
    '/configuration/quote/guide-rules/create',
    data,
  );
}

export function updateQuoteGuideRule(id: number, data: QuoteConfigApi.GuideRuleSaveParams) {
  return requestClient.post<QuoteConfigApi.GuideRule>(
    '/configuration/quote/guide-rules/update',
    data,
    { params: { id } },
  );
}

export function deleteQuoteGuideRule(id: number) {
  return requestClient.post<void>(
    '/configuration/quote/guide-rules/delete',
    {},
    { params: { id } },
  );
}

export function getQuoteGroundAgentRulePage(params: QuoteConfigApi.GroundAgentRuleQuery) {
  return requestClient.get<QuoteConfigApi.PageResult<QuoteConfigApi.GroundAgentRule>>(
    '/configuration/quote/ground-agent-rules/page',
    { params },
  );
}

export function createQuoteGroundAgentRule(data: QuoteConfigApi.GroundAgentRuleSaveParams) {
  return requestClient.post<QuoteConfigApi.GroundAgentRule>(
    '/configuration/quote/ground-agent-rules/create',
    data,
  );
}

export function updateQuoteGroundAgentRule(id: number, data: QuoteConfigApi.GroundAgentRuleSaveParams) {
  return requestClient.post<QuoteConfigApi.GroundAgentRule>(
    '/configuration/quote/ground-agent-rules/update',
    data,
    { params: { id } },
  );
}

export function deleteQuoteGroundAgentRule(id: number) {
  return requestClient.post<void>(
    '/configuration/quote/ground-agent-rules/delete',
    {},
    { params: { id } },
  );
}

export function getQuoteApprovalConfig() {
  return requestClient.get<QuoteConfigApi.ApprovalConfig>(
    '/configuration/quote/approval-config',
  );
}

export function saveQuoteApprovalConfig(data: QuoteConfigApi.ApprovalConfig) {
  return requestClient.post<QuoteConfigApi.ApprovalConfig>(
    '/configuration/quote/approval-config/save',
    data,
  );
}
