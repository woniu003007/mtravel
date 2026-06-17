import { describe, expect, it } from 'vitest';

import { purchaseRelationColumns } from './relation-columns';

describe('purchase relation columns', () => {
  it('matches the old system list fields', () => {
    expect(purchaseRelationColumns.map((column) => column.title)).toEqual([
      '所在地',
      '资源名称',
      '供应商',
      '负责人',
      '电话',
      '创建人',
      '创建时间',
      '操作',
    ]);
  });
});
