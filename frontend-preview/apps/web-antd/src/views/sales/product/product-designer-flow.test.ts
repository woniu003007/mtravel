import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

type WordPlanImageMode = 'follow_resource' | 'day_end' | 'hidden';

type DayEndImageSelection = {
  dayResourceId: number;
  imageId: number;
};

type WordPlanImageSavePayload = {
  dayEndImageSelections?: DayEndImageSelection[];
  imageMode?: WordPlanImageMode;
  selectedImageIdsByResource?: Record<number, number[]>;
};

type WordPlanImageValidationResult = {
  code?: 'day_end_max_3' | 'day_end_min_2';
  valid: boolean;
};

async function loadWordPlanImageHelpers(): Promise<{
  buildWordPlanImageSavePayload: (input: {
    dayEndImageSelections?: DayEndImageSelection[];
    imageMode?: WordPlanImageMode;
    selectedImageIdsByResource?: Record<number, number[]>;
  }) => WordPlanImageSavePayload;
  validateWordPlanImageSelections: (input: {
    dayEndImageSelections?: DayEndImageSelection[];
    imageMode?: WordPlanImageMode;
    selectedImageIdsByResource?: Record<number, number[]>;
  }) => WordPlanImageValidationResult;
}> {
  /**
   * 当前 designer.vue 把图片编排逻辑内联在 SFC 里，不适合做稳定的行为级单测。
   * 这里先用 RED 测试锁定纯 helper 契约，等主实现把逻辑抽出后转绿。
   */
  const modulePath = './product-designer-word-plan-image-utils';
  return import(/* @vite-ignore */ modulePath);
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

  it('uses the four-section day arrangement panel and keeps resource material editing available', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('ProductDesignerDayArrangementPanel');
    expect(workbenchSource).toContain('ProductResourceMapWorkspace');
    expect(workbenchSource).toContain('supplierCandidatesByResourceId');
    expect(workbenchSource).toContain('@change-supplier');
    expect(workbenchSource).toContain('景区素材已选');
    expect(workbenchSource).toContain('selectedMaterials: [] as MaterialEditorValue[]');
  });

  it('supports composing introduction and optional materials in one draggable output order', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');
    const apiSource = readAppFile('src/api/sales/product-designer.ts');

    expect(apiSource).toContain("export type SelectedMaterialType = 'introduction' | 'optional_item'");
    expect(apiSource).toContain('selectedMaterials?: SelectedMaterialSaveRequest[]');
    expect(workbenchSource).toContain('selectedMaterials: [] as MaterialEditorValue[]');
    expect(workbenchSource).toContain('initializeMaterialSortable');
    expect(workbenchSource).toContain("handle: '.material-drag-handle'");
    expect(workbenchSource).toContain('moveMaterial(entry.index, -1)');
    expect(workbenchSource).toContain('moveMaterial(entry.index, 1)');
    expect(workbenchSource).toContain('selectedMaterials: editor.selectedMaterials.map');
  });

  it('keeps optional-item cost internal and saves the product final self-pay price separately', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');
    const apiSource = readAppFile('src/api/sales/product-designer.ts');

    expect(apiSource).toContain("export type OptionalItemType = 'recommended_self_pay' | 'scenic_transport'");
    expect(apiSource).toContain('resourceOptionalItemId: number');
    expect(apiSource).toContain('supplierOptionalItemId?: number');
    expect(apiSource).toContain('salePrice?: number');
    expect(workbenchSource).toContain('新选择只显示当前供应商已启用报价的项目');
    expect(workbenchSource).toContain('系统默认价');
    expect(workbenchSource).toContain('本产品报价');
    expect(workbenchSource).toContain('salePrice: suggestedSalePrice');
    expect(workbenchSource).toContain('selected.salePrice = value == null ? undefined : Number(value)');
    expect(workbenchSource).toContain("materialType: 'optional_item'");
    expect(workbenchSource).toContain('item.isOptionalItem && item.resourceOptionalItemId === optionalItemId');
    expect(workbenchSource).toContain('activeIds.has(item.id)');
    expect(workbenchSource).toContain('salePriceDirty: false');
    expect(workbenchSource).toContain('if (!item.salePriceDirty) item.salePrice = nextSuggestedSalePrice');
    expect(workbenchSource).toContain("item.materialType === 'optional_item' && item.salePriceDirty ? item.salePrice : undefined");
  });

  it('does not submit a stale supplier quote after the supplier is cleared or changed', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('syncOpenWordPlanSupplier');
    expect(workbenchSource).toContain('function wordPlanCurrentSupplierOptionalItem');
    expect(workbenchSource).toContain('supplierOptionalItemId: wordPlanCurrentSupplierOptionalItem(item)?.supplierOptionalItemId');
  });

  it('uses the selected material images for a live resource preview without legacy resource controls', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('产品 Word 预览');
    expect(workbenchSource).toContain('downloadPurchaseResourceImage');
    expect(workbenchSource).toContain('productWordPreviewUrl');
    expect(workbenchSource).toContain('generateProductWordPreviewVersion');
    expect(workbenchSource).not.toContain('停留时间（分钟）');
    expect(workbenchSource).not.toContain('成本数量');
    expect(workbenchSource).not.toContain('纳入产品介绍 Word');
    expect(workbenchSource).not.toContain('产品配图');
  });

  it('uses the generated product Word PDF instead of a second client-side document renderer', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('previewSalesProductDesignerDocument');
    expect(workbenchSource).toContain('class="word-pdf-preview-frame"');
    expect(workbenchSource).toContain('title="产品 Word PDF 预览"');
    expect(workbenchSource).toContain('downloadProductWordPreview');
    expect(workbenchSource).not.toContain('selectedScenicIntroductionTitles');
  });

  it('builds the day-end image payload from global selections in mixed resource order', async () => {
    const { buildWordPlanImageSavePayload } = await loadWordPlanImageHelpers();

    expect(buildWordPlanImageSavePayload({
      dayEndImageSelections: [
        { dayResourceId: 202, imageId: 21 },
        { dayResourceId: 101, imageId: 11 },
        { dayResourceId: 202, imageId: 22 },
      ],
      imageMode: 'day_end',
      selectedImageIdsByResource: {
        101: [11, 12],
        202: [21, 22],
      },
    })).toMatchObject({
      dayEndImageSelections: [
        { dayResourceId: 202, imageId: 21 },
        { dayResourceId: 101, imageId: 11 },
        { dayResourceId: 202, imageId: 22 },
      ],
      imageMode: 'day_end',
    });
  });

  it('rejects day-end image selections when more than three images are chosen', async () => {
    const { validateWordPlanImageSelections } = await loadWordPlanImageHelpers();

    expect(validateWordPlanImageSelections({
      dayEndImageSelections: [
        { dayResourceId: 101, imageId: 11 },
        { dayResourceId: 101, imageId: 12 },
        { dayResourceId: 202, imageId: 21 },
        { dayResourceId: 202, imageId: 22 },
      ],
      imageMode: 'day_end',
    })).toEqual({
      code: 'day_end_max_3',
      valid: false,
    });
  });

  it('rejects day-end image selections when only one image is chosen', async () => {
    const { validateWordPlanImageSelections } = await loadWordPlanImageHelpers();

    expect(validateWordPlanImageSelections({
      dayEndImageSelections: [{ dayResourceId: 101, imageId: 11 }],
      imageMode: 'day_end',
    })).toEqual({
      code: 'day_end_min_2',
      valid: false,
    });
  });

  it('keeps follow-resource image payloads unchanged', async () => {
    const { buildWordPlanImageSavePayload, validateWordPlanImageSelections } = await loadWordPlanImageHelpers();

    expect(buildWordPlanImageSavePayload({
      dayEndImageSelections: [
        { dayResourceId: 202, imageId: 21 },
        { dayResourceId: 101, imageId: 11 },
      ],
      imageMode: 'follow_resource',
      selectedImageIdsByResource: {
        101: [11, 12],
        202: [21, 22, 23],
      },
    })).toEqual({
      imageMode: 'follow_resource',
      selectedImageIdsByResource: {
        101: [11, 12],
        202: [21, 22, 23],
      },
    });

    expect(validateWordPlanImageSelections({
      dayEndImageSelections: [{ dayResourceId: 202, imageId: 21 }],
      imageMode: 'follow_resource',
      selectedImageIdsByResource: {
        202: [21, 22],
      },
    })).toEqual({ valid: true });
  });
});
