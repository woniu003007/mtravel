import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('sales order management layout', () => {
  it('uses the shared readable search form instead of the compressed one-line grid', () => {
    const source = readAppFile('src/views/sales/order/index.vue');
    const searchFormSource = readAppFile('src/components/business/BusinessSearchForm.vue');

    expect(source).toContain('BusinessSearchForm');
    expect(source).toContain('grid-class="order-search-grid"');
    expect(source).toContain('actions-in-grid');
    expect(source).toContain('actions-class="order-filter-actions"');
    expect(source).toContain('label-width="72px"');
    expect(source).toContain('class="order-search-item"');
    expect(source).toContain('DatePicker.RangePicker');
    expect(source).not.toContain('grid-template-columns: 0.95fr 1fr 1.18fr');
    expect(source).not.toContain('class="order-search-actions"');
    expect(source).not.toContain('class="order-filter-control"');
    expect(source).not.toContain('class="order-filter-label"');
    expect(source).not.toContain('.order-page-card :deep(.order-search-grid .ant-form-item-row) {\n  display: block;');
    expect(searchFormSource).toContain('gridClass');
    expect(searchFormSource).toContain('actionsInGrid');
  });

  it('keeps common order filters in two rows and moves low-frequency filters behind advanced search', () => {
    const source = readAppFile('src/views/sales/order/index.vue');

    expect(source).toContain('advancedSearchOpen');
    expect(source).toContain('高级筛选');
    expect(source).toContain('收起筛选');
    expect(source).toContain('<template #extraActions>');
    expect(source).toContain('<template v-if="advancedSearchOpen">');
    expect(source).toContain('class="order-search-item order-advanced-search-item" label="团队类型"');
    expect(source).toContain('class="order-search-item order-advanced-search-item" label="排序"');
    expect(source).toContain('class="order-search-item order-advanced-search-item" label="标记"');
    expect(source).toContain('class="order-search-item order-advanced-search-item" label="订单文件"');
    expect(source).toContain('grid-template-columns: repeat(7, minmax(0, 1fr));');
    expect(source).toContain('.order-advanced-search-item');
    expect(source).toContain('grid-column: 5 / 8;');
    expect(source).toContain('flex-wrap: nowrap;');
  });

  it('separates table batch actions from search actions', () => {
    const source = readAppFile('src/views/sales/order/index.vue');

    expect(source).toContain('class="order-table-toolbar"');
    expect(source).toContain('class="order-table-toolbar-main"');
    expect(source).toContain('订单列表');
    expect(source).toContain('已选 {{ selectedOrderIds.length }} 条');
    expect(source).toContain('class="order-table-actions"');
    expect(source).not.toContain('class="order-toolbar"');
  });

  it('keeps allow-clear inputs inside their wrapper so the input border remains visible', () => {
    const source = readAppFile('src/views/sales/order/index.vue');

    expect(source).toContain('.order-page-card :deep(.order-search-item .ant-input-affix-wrapper .ant-input) {');
    expect(source).toContain('height: 20px !important;');
    expect(source).toContain('min-height: 20px !important;');
    expect(source).toContain('line-height: 20px;');
  });

  it('keeps the order table in one screen by merging narrow business columns', () => {
    const source = readAppFile('src/views/sales/order/index.vue');

    expect(source).toContain("key: 'customerGuest'");
    expect(source).toContain("key: 'priceSummary'");
    expect(source).toContain("key: 'remarks'");
    expect(source).toContain('class="customer-guest-cell"');
    expect(source).toContain('class="price-summary-cell"');
    expect(source).toContain('class="remarks-cell"');
    expect(source).not.toContain(':scroll="{ x:');
  });
});
