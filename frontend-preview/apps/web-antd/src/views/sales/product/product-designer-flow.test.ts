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

  it('groups the day plan and exposes procurement and introduction choices per resource', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('const planResourceGroups = computed<PlanResourceGroup[]>(() =>');
    expect(workbenchSource).toContain('景区组合');
    expect(workbenchSource).toContain('需采购');
    expect(workbenchSource).toContain('无需采购');
    expect(workbenchSource).toContain('entry.item.introductionTitle');
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
    expect(workbenchSource).toContain('建议对外价');
    expect(workbenchSource).toContain('最终对外价');
    expect(workbenchSource).toContain('salePrice: suggestedSalePrice ?? 0');
    expect(workbenchSource).toContain("selected.salePrice = Number(value ?? 0)");
    expect(workbenchSource).toContain("materialType: 'optional_item'");
    expect(workbenchSource).toContain('item.isOptionalItem && item.resourceOptionalItemId === optionalItemId');
    expect(workbenchSource).toContain('activeIds.has(item.id)');
    expect(workbenchSource).toContain('salePriceDirty: false');
    expect(workbenchSource).toContain('if (!item.salePriceDirty) item.salePrice = nextSuggestedSalePrice ?? 0');
    expect(workbenchSource).toContain('请填写「${name}」的本产品最终对外价');
    expect(workbenchSource).toContain("item.materialType === 'optional_item' ? item.salePrice : undefined");
  });

  it('does not submit a stale supplier quote after the supplier is cleared or changed', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('syncOpenWordPlanSupplier');
    expect(workbenchSource).toContain('function wordPlanCurrentSupplierOptionalItem');
    expect(workbenchSource).toContain('supplierOptionalItemId: wordPlanCurrentSupplierOptionalItem(item)?.supplierOptionalItemId');
  });

  it('uses the selected material images for a live resource preview without legacy resource controls', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('Word 效果预览');
    expect(workbenchSource).toContain('downloadPurchaseResourceImage');
    expect(workbenchSource).toContain('revokeMaterialPreviewUrls');
    expect(workbenchSource).toContain('实际字体和分页以后续模板为准');
    expect(workbenchSource).not.toContain('停留时间（分钟）');
    expect(workbenchSource).not.toContain('成本数量');
    expect(workbenchSource).not.toContain('纳入产品介绍 Word');
    expect(workbenchSource).not.toContain('产品配图');
  });

  it('renders each scenic introduction as one indented Word paragraph with its material warm tip', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain("entry.material.materialType === 'introduction' && entry.introduction.title");
    expect(workbenchSource).toContain('function inlinePreviewText(value?: string)');
    expect(workbenchSource).toContain('class="word-preview-material-line"');
    expect(workbenchSource).toContain('class="word-preview-attraction-title"');
    expect(workbenchSource).toContain('【{{ entry.introduction.title }}】');
    expect(workbenchSource).toContain('function formatVisitDuration(value?: string)');
    expect(workbenchSource).toContain('（游览约${totalMinutes}分钟）');
    expect(workbenchSource).toContain('（游览约${hours}小时${remainder}分钟）');
    expect(workbenchSource).toContain('const minuteMatch = raw.match');
    expect(workbenchSource).toContain('.word-preview-material-line { margin: 0; font: inherit; line-height: inherit; text-indent: 2em; }');
    expect(workbenchSource).toContain('.word-preview-attraction-title { color: #0070c0; font-weight: 700; }');
    expect(workbenchSource).toContain('.word-preview-notice, .word-preview-optional-price { color: #f00; }');
    expect(workbenchSource).toContain('warmTipPreviewText(entry.introduction.warmTipContent)');
    expect(workbenchSource).toContain('class="word-preview-warm-tip"');
    expect(workbenchSource).not.toContain('温馨提示：{{ warmTipPreviewText');
    expect(workbenchSource).toContain('font-size: 10pt;');
    expect(workbenchSource).not.toContain('selectedScenicIntroductionTitles');
    expect(workbenchSource).not.toContain('.word-preview-sheet h2');
    expect(workbenchSource).not.toContain('.word-preview-material h3');
    expect(workbenchSource.indexOf('class="word-preview-notice"'))
      .toBeLessThan(workbenchSource.indexOf('class="word-preview-content"'));
    expect(workbenchSource).toContain('v-for="(block, blockIndex) in entry.introduction.extensionBlocks || []"');
    expect(workbenchSource).not.toContain('warmTipLines(');
    expect(workbenchSource).not.toContain('resourceDetail.warmTip');
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
