import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

function expectTextOrder(source: string, labels: string[]) {
  let previousIndex = -1;
  for (const label of labels) {
    const currentIndex = source.indexOf(label, previousIndex + 1);
    expect(currentIndex, `missing label: ${label}`).toBeGreaterThanOrEqual(0);
    expect(currentIndex, `${label} should appear after the previous label`).toBeGreaterThan(previousIndex);
    previousIndex = currentIndex;
  }
}

describe('sales team operation layout', () => {
  it('keeps the old-system operation order while using the new-system visual shell', () => {
    const source = readAppFile('src/views/sales/team/operation.vue');

    expect(source).toContain('team-operation-header');
    expect(source).toContain('top-tool-actions');
    expect(source).toContain('operation-flow-row');
    expect(source).toContain('team-profile-block');
    expect(source).toContain('team-description-stack');
    expect(source).toContain('operation-icon-actions');
    expect(source).toContain('metric-teamNo');
    expect(source).toContain('minmax(218px, 1.5fr)');
    expect(source).toContain('display: flex;');
    expect(source).toContain('stageState');
    expect(source).toContain('price-modal-table');
    expect(source).toContain('itinerary-modal-content');
    expect(source).toContain('profileEditorOpen');
    expect(source).toContain('openProfileEditor');
    expect(source).toContain('saveProfileEditor');
    expect(source).toContain('team_type');
    expect(source).toContain('business_type');
    expect(source).toContain('department');
    expect(source).toContain('operator');
    expect(source).toContain('escort');
    expect(source).toContain('getProductDictionaryAll');
    expect(source).toContain('getEnterpriseDepartmentAll');
    expect(source).toContain('getEnterpriseEmployeeAll');
    expect(source).toContain('saveSalesTeam');
    expect(source).toContain('该团相关订单');
    expect(source).toContain('ORDER_TABLE_SCROLL_X');
    expect(source).toContain('order-cell-clamp');
    expect(source).toContain('order-edit-button');
    expect(source).toContain('修改');
    expect(source).toContain('record.status');
    expect(source).toContain('record.pickupInfo');
    expect(source).toContain('record.dropoffInfo');
    expect(source).toContain('record.originalOrderInfo');
    expect(source).toContain('record.guestCountText');
    expect(source).toContain('record.priceDetail');
    expect(source).toContain('record.feeRemark');
    expect(source).toContain('record.orderRemark');
    expect(source).toContain('Tooltip');
    expect(source).toContain('order-remark-ellipsis');
    expect(source).toContain('order-remark-tooltip');
    expect(source).toContain('原始订单信息');
    expect(source).toContain('order-original-row');
    expect(source).toContain('order-multiline-cell');
    expect(source).not.toContain('operation-tool-strip');
    expect(source).not.toContain('business-action-grid');
    expect(source).not.toContain('grid-template-columns: repeat(6, minmax(128px, 1fr))');
    expect(source).not.toContain('index <= 1');
    expect(source).not.toContain('总里程数\', value: \'0公里');
    expect(source).not.toContain('#b88207');
    expect(source).not.toContain('#087987');

    expectTextOrder(source, [
      '团队操作',
      'top-tool-actions',
      'operation-flow-row',
      'team-profile-block',
      'team-description-stack',
      'operation-icon-actions',
      '该团相关订单',
    ]);

    expectTextOrder(source, ['产品说明', '收客须知', '内部备注']);
  });
});
