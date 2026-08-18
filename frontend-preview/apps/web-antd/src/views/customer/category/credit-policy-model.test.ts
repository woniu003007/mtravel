import { describe, expect, it } from 'vitest';

import {
  buildCreditPolicyPayload,
  moveApprovalMember,
  validateCreditPolicyForm,
  type CreditPolicyFormState,
} from './credit-policy-model';

function form(
  overrides: Partial<CreditPolicyFormState> = {},
): CreditPolicyFormState {
  return {
    allowOverLimit: true,
    approverIds: [31, 42],
    categoryName: ' A级客户 ',
    ccUserIds: [53, 64],
    creditTermDays: 30,
    defaultCreditLimit: 500_000,
    remark: ' 重点客户 ',
    sortOrder: 10,
    status: 'active',
    ...overrides,
  };
}

describe('customer credit policy form', () => {
  it('keeps ordered approvers and system user ids in the backend payload', () => {
    expect(buildCreditPolicyPayload(form())).toEqual({
      allowOverLimit: true,
      approvers: [{ systemUserId: 31 }, { systemUserId: 42 }],
      categoryName: 'A级客户',
      ccUsers: [{ systemUserId: 53 }, { systemUserId: 64 }],
      creditTermDays: 30,
      defaultCreditLimit: 500_000,
      remark: '重点客户',
      sortOrder: 10,
      status: 'active',
    });
  });

  it('clears workflow members when over-limit approval is disabled', () => {
    const payload = buildCreditPolicyPayload(form({ allowOverLimit: false }));

    expect(payload.approvers).toEqual([]);
    expect(payload.ccUsers).toEqual([]);
  });

  it('requires a unique, complete ordered approver list', () => {
    expect(validateCreditPolicyForm(form({ approverIds: [] }))).toContain(
      '至少需要指定一名审批人',
    );
    expect(
      validateCreditPolicyForm(form({ approverIds: [31, undefined] })),
    ).toContain('至少需要指定一名审批人');
    expect(validateCreditPolicyForm(form({ approverIds: [31, 31] }))).toBe(
      '审批人不能重复',
    );
  });

  it('allows the same user to be an approver and a cc recipient', () => {
    expect(
      validateCreditPolicyForm(form({ approverIds: [31], ccUserIds: [31] })),
    ).toBeUndefined();
  });

  it('moves approval members without mutating the original list', () => {
    const original = [31, 42, 53];

    expect(moveApprovalMember(original, 1, -1)).toEqual([42, 31, 53]);
    expect(original).toEqual([31, 42, 53]);
  });
});
