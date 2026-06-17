import { describe, expect, it } from 'vitest';

import { contractTypeTabs, supplierCategoryByContractTab } from './contract-tabs';

describe('contract management tabs', () => {
  it('keeps the same tab labels and order as the legacy contract page', () => {
    expect(contractTypeTabs.map((item) => item.label)).toEqual([
      '分销商',
      '景区',
      '酒店',
      '餐厅',
      '车队',
      '大交通',
      '其它',
      '地接',
      '导游',
      '财务费用',
      '现收现退',
      '附加费用',
      '购物',
    ]);
  });

  it('maps non-customer tabs to supplier contract categories', () => {
    expect(supplierCategoryByContractTab('scenic')).toBe('scenic');
    expect(supplierCategoryByContractTab('traffic')).toBe('traffic');
    expect(supplierCategoryByContractTab('finance_fee')).toBe('finance_fee');
    expect(supplierCategoryByContractTab('customer')).toBeUndefined();
  });
});
