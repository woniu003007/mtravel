import { describe, expect, it } from 'vitest';

import { buildResourceQuoteRulePayload } from './form-payload';

describe('resource quote rule save payload', () => {
  it('keeps a cleared customer level as an explicit default-rule null value', () => {
    const payload = buildResourceQuoteRulePayload({
      customerLevelId: undefined,
      minimumFixedAddon: 0,
      minimumRate: 5,
      remark: '  默认报价  ',
      resourceType: 'hotel',
      status: 'active',
      suggestedFixedAddon: 20,
      suggestedRate: 10,
    });

    expect(payload.customerLevelId).toBeNull();
    expect(Object.prototype.hasOwnProperty.call(payload, 'customerLevelId')).toBe(true);
    expect(payload.remark).toBe('默认报价');
  });
});
