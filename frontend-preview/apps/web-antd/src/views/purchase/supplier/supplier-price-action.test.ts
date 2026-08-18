import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(import.meta.dirname, '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('supplier price shortcut', () => {
  it('exposes an edit-price action and routes by resource binding relation', () => {
    const supplierSource = readAppFile('src/views/purchase/supplier/index.vue');
    const crudSource = readAppFile('src/views/_business/crud/CrudPage.vue');
    const resourceSource = readAppFile('src/views/purchase/resource/index.vue');

    expect(crudSource).toContain('<slot name="row-actions" :record="record" />');
    expect(supplierSource).toContain('<div class="supplier-page">');
    expect(supplierSource).toContain('编辑报价');
    expect(supplierSource).toContain('该供应商绑定了多个资源');
    expect(supplierSource).toContain('selectedRelationId.value = undefined');
    expect(supplierSource).toContain("editPrice: '1'");
    expect(supplierSource).toContain('relationId: String(relation.id)');
    expect(supplierSource).toContain('resourceId: String(relation.resourceId)');
    expect(resourceSource).toContain("route.query.editPrice === '1'");
    expect(resourceSource).toContain('await editBoundSupplier(binding)');
  });
});
