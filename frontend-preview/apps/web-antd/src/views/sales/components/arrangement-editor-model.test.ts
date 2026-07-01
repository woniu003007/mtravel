import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import * as arrangementEditorModel from './arrangement-editor-model';
import {
  ensureSelectOption,
  resolveArrangementResourceName,
  resolveSupplierOptionsForResource,
  type SelectOptionWithId,
} from './arrangement-editor-model';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('arrangement editor model helpers', () => {
  it('uses itemName as resource fallback when old arrangement data has no resourceName', () => {
    expect(resolveArrangementResourceName(undefined, '丝绸购物店')).toBe('丝绸购物店');
    expect(resolveArrangementResourceName('杭州丝绸购物中心', '丝绸购物店')).toBe('杭州丝绸购物中心');
  });

  it('keeps historical supplier option visible when resource binding options do not contain it', () => {
    const nextOptions: SelectOptionWithId[] = [
      { id: 39, label: '华东购物点合作中心', value: '华东购物点合作中心' },
    ];

    const result = resolveSupplierOptionsForResource({
      currentSupplierId: 22,
      currentSupplierName: '杭州丝绸购物中心',
      nextOptions,
    });

    expect(result.selectedSupplierId).toBe(22);
    expect(result.selectedSupplierName).toBe('杭州丝绸购物中心');
    expect(result.options).toEqual([
      { id: 22, label: '杭州丝绸购物中心', value: '杭州丝绸购物中心' },
      { id: 39, label: '华东购物点合作中心', value: '华东购物点合作中心' },
    ]);
  });

  it('does not duplicate select options when the historical value already exists', () => {
    const options = ensureSelectOption(
      [{ id: 4, label: '杭州丝绸购物中心', value: '杭州丝绸购物中心' }],
      '杭州丝绸购物中心',
      4,
    );

    expect(options).toHaveLength(1);
  });

  it('does not force team editor submit button text to add mode when editing', () => {
    const modalSource = readAppFile('src/views/sales/components/ArrangementEditorModal.vue');

    expect(modalSource).not.toContain("if (props.editorMode === 'team') return '新增安排';");
    expect(modalSource).toContain("props.editorMode === 'team'");
    expect(modalSource).toContain("props.editingArrangementIndex >= 0 ? '保存修改' : '新增安排'");
  });

  it('derives ground-agent package amount from saved totals before old price lines', () => {
    const helper = (arrangementEditorModel as Record<string, any>).resolveGroundAgentPackageAmount;

    expect(typeof helper).toBe('function');
    expect(helper({
      costAmount: 880,
      priceLines: [
        { quantity: 2, unitPrice: 100 },
        { amount: 300 },
      ],
      totalAmount: 660,
    })).toBe(880);
    expect(helper({
      priceLines: [
        { quantity: 2, unitPrice: 100 },
        { amount: 300 },
      ],
    })).toBe(500);
    expect(helper({
      costAmount: 0,
      priceLines: [],
      totalAmount: 0,
    })).toBe(0);
  });

  it('creates a hidden compatibility price line for ground-agent package price', () => {
    const helper = (arrangementEditorModel as Record<string, any>).createGroundAgentPackagePriceLine;

    expect(typeof helper).toBe('function');
    expect(helper(1280, '报价备注')).toMatchObject({
      amount: 1280,
      projectName: '地接结算价',
      quantity: 1,
      remark: '报价备注',
      sortOrder: 1,
      unitPrice: 1280,
    });
  });
});
