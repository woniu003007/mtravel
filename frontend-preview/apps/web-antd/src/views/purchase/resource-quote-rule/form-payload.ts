import type { PurchaseResourceQuoteRuleApi } from '#/api/purchase/resource-quote-rule';

/** 资源报价规则弹窗中可编辑的字段状态。 */
export interface ResourceQuoteRuleFormState {
  customerLevelId?: null | number;
  minimumFixedAddon: number;
  minimumRate: number;
  remark?: string;
  resourceType: PurchaseResourceQuoteRuleApi.ResourceType;
  status: PurchaseResourceQuoteRuleApi.Status;
  suggestedFixedAddon: number;
  suggestedRate: number;
}

/**
 * 将报价规则表单状态转换成保存载荷。
 *
 * 客户等级被清空时必须明确传递 null，后端据此把规则恢复为资源类型的默认规则。
 */
export function buildResourceQuoteRulePayload(
  form: ResourceQuoteRuleFormState,
): PurchaseResourceQuoteRuleApi.SaveParams {
  return {
    customerLevelId: form.customerLevelId ?? null,
    minimumFixedAddon: Number(form.minimumFixedAddon || 0),
    minimumRate: Number(form.minimumRate || 0),
    remark: form.remark?.trim() || undefined,
    resourceType: form.resourceType,
    status: form.status || 'active',
    suggestedFixedAddon: Number(form.suggestedFixedAddon || 0),
    suggestedRate: Number(form.suggestedRate || 0),
  };
}
