import { describe, expect, it } from 'vitest';

import { expenseItemTabs } from './expense-item-tabs';

describe('expense item tabs', () => {
  it('keeps the same tab labels and order as the legacy resource project page', () => {
    expect(expenseItemTabs.map((item) => item.label)).toEqual([
      '全部',
      '景区',
      '酒店',
      '餐厅',
      '车队',
      '大交通',
      '其它',
      '杂费',
      '地接',
      '附加费用',
      '购物',
    ]);
  });
});
