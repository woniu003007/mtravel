import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('business list search layout', () => {
  it('uses the order-management compact search style from the shared component', () => {
    const source = readAppFile('src/components/business/BusinessSearchForm.vue');
    const actionsSource = readAppFile('src/components/business/BusinessActionButtons.vue');

    expect(source).toContain("labelWidth: '72px'");
    expect(source).toContain('actionsInGrid: true');
    expect(source).toContain('grid-template-columns: repeat(7, minmax(0, 1fr));');
    expect(source).toContain('height: 30px !important;');
    expect(source).toContain('min-height: 30px !important;');
    expect(source).toContain('font-size: 13px;');
    expect(source).toContain('.business-search-item--wide');
    expect(actionsSource).toContain('height: 30px;');
    expect(actionsSource).toContain('min-width: 64px;');
  });

  it('moves custom business list search areas onto BusinessSearchForm', () => {
    const pages = [
      'src/views/customer/contract/index.vue',
      'src/views/dispatch/guide-schedule/index.vue',
      'src/views/enterprise/bank-account/index.vue',
      'src/views/enterprise/department/index.vue',
      'src/views/enterprise/role/index.vue',
      'src/views/finance/guide-advance/index.vue',
      'src/views/finance/team-audit/index.vue',
      'src/views/sales/product/schedule.vue',
      'src/views/system/operation-log/index.vue',
      'src/views/system/risk-approval/index.vue',
    ];

    for (const path of pages) {
      const source = readAppFile(path);
      expect(source, path).toContain("BusinessSearchForm from '#/components/business/BusinessSearchForm.vue'");
      expect(source, path).not.toMatch(/class="[^"]*(?:search|query)-grid/);
      expect(source, path).not.toMatch(/class="[^"]*(?:search|query)-actions/);
      expect(source, path).not.toContain('layout="inline"');
    }
  });

  it('keeps order and team pages on the shared search layout instead of page-owned search css', () => {
    const orderSource = readAppFile('src/views/sales/order/index.vue');
    const teamSource = readAppFile('src/views/sales/team/index.vue');

    for (const [name, source] of [
      ['order', orderSource],
      ['team', teamSource],
    ] as const) {
      expect(source, name).toContain('<BusinessSearchForm');
      expect(source, name).not.toContain('grid-class=');
      expect(source, name).not.toContain('actions-class=');
      expect(source, name).not.toMatch(/\.(?:order|team)-search-grid/);
      expect(source, name).not.toMatch(/\.(?:order|team)-filter-actions/);
    }
  });
});
