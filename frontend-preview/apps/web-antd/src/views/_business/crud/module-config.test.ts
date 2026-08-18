import { describe, expect, it } from 'vitest';

import { supplierConfig } from './module-config';

describe('supplier form fields', () => {
  it('keeps the supplier add/edit form aligned with the legacy supplier modal', () => {
    expect(supplierConfig.fields.map((field) => field.label)).toEqual([
      '所在地',
      '公司名称',
      '负责人',
      '商家分类',
      '基础信息',
      '联系电话',
      '传真号码',
      '办公地址',
      '备注信息',
      '客户状态',
    ]);
  });
});
