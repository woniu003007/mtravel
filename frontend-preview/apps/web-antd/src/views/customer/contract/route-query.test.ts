import { describe, expect, it } from 'vitest';

import {
  buildCustomerContractRoute,
  contractQueryFromRoute,
  parseCustomerId,
} from './route-query';

describe('customer contract route query helpers', () => {
  it('builds the row action route for one customer unit', () => {
    expect(buildCustomerContractRoute({ customerId: 61802 })).toEqual({
      path: '/customer/contract',
      query: { customerId: '61802' },
    });
  });

  it('keeps a valid route customerId as the initial contract filter', () => {
    expect(contractQueryFromRoute({ customerId: '61802' })).toEqual({
      customerId: 61_802,
    });
  });

  it('ignores missing or invalid route customerId values', () => {
    expect(parseCustomerId(undefined)).toBeUndefined();
    expect(parseCustomerId('abc')).toBeUndefined();
    expect(parseCustomerId('-1')).toBeUndefined();
  });
});
