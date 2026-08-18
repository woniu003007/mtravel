import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('sales product designer flow', () => {
  it('keeps product design drafts separate from formal product management', () => {
    const routeSource = readAppFile('src/router/routes/modules/sales.ts');
    const draftListSource = readAppFile('src/views/sales/product/designer-index.vue');
    const formalListSource = readAppFile('src/views/sales/product/index.vue');
    const apiSource = readAppFile('src/api/sales/product-designer.ts');

    expect(routeSource).toContain("path: '/sales/product/designer', component: ProductDesignerListPage");
    expect(routeSource).toContain("path: '/sales/product', component: ProductPage");
    expect(draftListSource).toContain('getSalesProductDesignerDraftPage');
    expect(draftListSource).toContain('deleteSalesProductDesignerDraft');
    expect(apiSource).toContain("'/sales/product/designer/draft/delete'");
    expect(formalListSource).not.toContain('designerSelectorMode');
    expect(formalListSource).not.toContain('openDesignerPage');
    expect(formalListSource).not.toContain('>产品设计</Button>');
  });

  it('moves a saved draft through the workbench before publishing it', () => {
    const formSource = readAppFile('src/views/sales/product/designer-form.vue');
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(formSource).toContain("router.push(`/sales/product/designer/${saved.id}`)");
    expect(workbenchSource).toContain('publishSalesProductDesignerDraft(productId.value)');
    expect(workbenchSource).toContain("router.push('/sales/product')");
    expect(workbenchSource).toContain('完成后，该数据将从产品设计列表移入产品管理。');
  });
});
