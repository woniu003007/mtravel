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
    const apiSource = readAppFile('src/api/sales/team.ts');

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
    expect(source).toContain('isMergeSourceOrder(record)');
    expect(source).toContain('firstMergeTargetTeamId');
    expect(source).toContain('去目标团');
    expect(source).toContain('record.status');
    expect(source).toContain('record.pickupInfo');
    expect(source).toContain('record.dropoffInfo');
    expect(source).toContain('record.originalOrderInfo');
    expect(source).toContain('record.mergeOrderInfos');
    expect(source).toContain('record.orderRole');
    expect(source).toContain('record.guestCountText');
    expect(source).toContain('record.priceDetail');
    expect(source).toContain('record.feeRemark');
    expect(source).toContain('record.orderRemark');
    expect(source).toContain('Tooltip');
    expect(source).toContain('order-remark-ellipsis');
    expect(source).toContain('order-remark-tooltip');
    expect(source).toContain('原始订单信息');
    expect(source).toContain('已拼至');
    expect(source).toContain('来源订单');
    expect(source).toContain('sourceOrderInfos');
    expect(source).toContain('goRelatedTeam');
    expect(source).toContain('order-original-row');
    expect(source).toContain('order-merge-row');
    expect(source).toContain('order-multiline-cell');
    expect(source).toContain('title="拼团操作"');
    expect(source).toContain('merge-selected-orders');
    expect(source).toContain('merge-team-search-form');
    expect(source).toContain('merge-team-table');
    expect(source).toContain('mergeTeamColumns');
    expect(source).toContain('mergeTeamRowSelection');
    expect(source).toContain('mergeTeamPagination');
    expect(source).toContain('canTransferOrder');
    expect(source).toContain('transferDisabledReason');
    expect(source).toContain('validateSelectedTransferOrders');
    expect(source).toContain('已取消订单不能拼团或转团');
    expect(source).toContain('已拼出订单不能再次拼团或转团');
    expect(source).toContain('getCheckboxProps');
    expect(source).toContain('选择团期');
    expect(source).toContain('团号/团队名称');
    expect(source).toContain('客户单位');
    expect(source).toContain('出团日期始');
    expect(source).toContain('出团日期止');
    expect(source).toContain('天数');
    expect(source).toContain('预控/实收/余位');
    expect(source).toContain('已选目标团');
    expect(source).toContain('targetTeamId');
    expect(source).toContain('已选订单');
    expect(source).not.toContain('title="拆分拼团"');
    expect(source).not.toContain('merge-split-card');
    expect(source).not.toContain('分配游客');
    expect(source).not.toContain('已拆金额');
    expect(source).not.toContain('新增明细');
    expect(source).not.toContain('拆分金额');
    expect(source).not.toContain('operation-tool-strip');
    expect(source).not.toContain('business-action-grid');
    expect(source).not.toContain('grid-template-columns: repeat(6, minmax(128px, 1fr))');
    expect(source).not.toContain('index <= 1');
    expect(source).not.toContain('总里程数\', value: \'0公里');
    expect(source).not.toContain('#b88207');
    expect(source).not.toContain('#087987');
    expect(apiSource).toContain('targetTeamId: number;');
    expect(apiSource).not.toContain('targetTeamIds: number[];');
    expect(apiSource).not.toContain('splits: MergeOrderSplitParams[];');

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
