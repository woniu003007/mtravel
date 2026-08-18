import type { CustomerCategoryApi } from '#/api/customer/category';

export interface CreditPolicyFormState {
  allowOverLimit: boolean;
  approverIds: Array<number | undefined>;
  categoryName: string;
  ccUserIds: number[];
  creditTermDays: number;
  defaultCreditLimit: number;
  remark?: string;
  sortOrder: number;
  status: 'active' | 'disabled';
}

export function buildCreditPolicyPayload(
  form: CreditPolicyFormState,
): CustomerCategoryApi.SaveParams {
  const approverIds = form.allowOverLimit
    ? form.approverIds.filter((id): id is number => typeof id === 'number')
    : [];
  const ccUserIds = form.allowOverLimit ? form.ccUserIds : [];

  return {
    allowOverLimit: form.allowOverLimit,
    approvers: approverIds.map((systemUserId) => ({ systemUserId })),
    categoryName: form.categoryName.trim(),
    ccUsers: ccUserIds.map((systemUserId) => ({ systemUserId })),
    creditTermDays: form.creditTermDays,
    defaultCreditLimit: form.defaultCreditLimit,
    remark: form.remark?.trim() || undefined,
    sortOrder: form.sortOrder,
    status: form.status,
  };
}

export function moveApprovalMember(
  ids: Array<number | undefined>,
  index: number,
  offset: -1 | 1,
) {
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= ids.length) {
    return [...ids];
  }
  const next = [...ids];
  [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
  return next;
}

export function validateCreditPolicyForm(form: CreditPolicyFormState) {
  if (!form.categoryName.trim()) return '请填写等级名称';
  if (
    !Number.isFinite(form.defaultCreditLimit) ||
    form.defaultCreditLimit < 0
  ) {
    return '授信额度不能小于0';
  }
  if (
    !Number.isInteger(form.creditTermDays) ||
    form.creditTermDays < 0 ||
    form.creditTermDays > 3650
  ) {
    return '账期天数应为0到3650之间的整数';
  }
  if (!form.allowOverLimit) return undefined;

  const approverIds = form.approverIds.filter(
    (id): id is number => typeof id === 'number',
  );
  if (
    approverIds.length === 0 ||
    approverIds.length !== form.approverIds.length
  ) {
    return '允许超额时至少需要指定一名审批人';
  }
  if (new Set(approverIds).size !== approverIds.length) {
    return '审批人不能重复';
  }
  if (new Set(form.ccUserIds).size !== form.ccUserIds.length) {
    return '抄送人不能重复';
  }
  return undefined;
}
