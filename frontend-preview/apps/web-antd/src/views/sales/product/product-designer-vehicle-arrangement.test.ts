import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('product designer product-level vehicle arrangement', () => {
  it('keeps vehicles out of the day map and persists them at product scope', () => {
    const apiSource = readAppFile('src/api/sales/product-designer.ts');
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');
    const panelSource = readAppFile('src/views/sales/product/components/ProductDesignerVehicleArrangementPanel.vue');

    expect(apiSource).toContain("'/sales/product/designer/vehicle-resources'");
    expect(apiSource).toContain("'/sales/product/designer/vehicle-arrangement/save'");
    expect(apiSource).toContain("'/sales/product/designer/vehicle-arrangement/delete'");
    expect(apiSource).toContain("'/sales/product/designer/vehicle-arrangement/reorder'");
    expect(workbenchSource).toContain('ProductDesignerVehicleArrangementPanel');
    expect(workbenchSource).toContain('openVehicleArrangement');
    expect(workbenchSource).toContain('saveVehicleArrangement');
    expect(workbenchSource).toContain('vehicleArrangementOpen');
    expect(panelSource).toContain('不进入每天地图或当天行程');
    expect(workbenchSource.indexOf('<ProductDesignerVehicleArrangementPanel')).toBeLessThan(
      workbenchSource.indexOf('<div class="day-switcher">'),
    );
    expect(panelSource).toContain('管理安排');
    expect(panelSource).toContain('已安排 ${segments.length} 段');
  });

  it('renders quoted, pending and not-required vehicle procurement states distinctly', () => {
    const apiSource = readAppFile('src/api/sales/product-designer.ts');
    const panelSource = readAppFile('src/views/sales/product/components/ProductDesignerVehicleArrangementPanel.vue');

    expect(apiSource).toContain("'not_required' | 'pending' | 'quoted'");
    expect(panelSource).toContain("item.procurementStatus === 'pending'");
    expect(panelSource).toContain("item.procurementStatus === 'not_required'");
    expect(panelSource).toContain('无需采购');
  });

  it('keeps only the latest product document on the page and moves history into a drawer', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('title="最新产品文件"');
    expect(workbenchSource).toContain('v-model:open="documentHistoryOpen"');
    expect(workbenchSource).toContain('title="历史文件"');
    expect(workbenchSource).toContain('v-if="latestProductDocument"');
    expect(workbenchSource).not.toContain('class="document-history"');
  });
});
