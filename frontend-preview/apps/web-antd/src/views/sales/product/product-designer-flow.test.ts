import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

/** 仅检查“制作 Word 方案”工作台，供应商配置等外层能力仍会保留在产品设计页。 */
function getDayWordPlanWorkspaceSource(source: string) {
  const workspaceStart = source.indexOf('class="day-word-plan-drawer"');
  expect(workspaceStart).toBeGreaterThanOrEqual(0);

  // 图片设置是独立二级弹窗，不把它混入工作台本体的断言范围。
  const imageSettingModalStart = source.indexOf('<Modal', workspaceStart + 1);
  expect(imageSettingModalStart).toBeGreaterThan(workspaceStart);
  return source.slice(workspaceStart, imageSettingModalStart);
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

  it('uses the system default self-pay price in the Word workbench without editing it there', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');
    const apiSource = readAppFile('src/api/sales/product-designer.ts');
    const wordPlanWorkspaceSource = getDayWordPlanWorkspaceSource(workbenchSource);

    expect(apiSource).toContain("export type OptionalItemType = 'recommended_self_pay' | 'scenic_transport'");
    expect(apiSource).toContain('resourceOptionalItemId: number');
    expect(apiSource).toContain('supplierOptionalItemId?: number');
    expect(apiSource).toContain('salePrice?: number');
    expect(wordPlanWorkspaceSource).toContain('系统默认');
    expect(wordPlanWorkspaceSource).not.toContain('editWordPlanPrice');
    expect(wordPlanWorkspaceSource).not.toContain('updateWordPlanSalePrice');
    expect(wordPlanWorkspaceSource).not.toContain('本产品报价');
    expect(wordPlanWorkspaceSource).not.toContain('最终对外价');
  });

  it('uses the current external supplier quote when saving a Word plan', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

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
    const previewStart = workbenchSource.indexOf('async function loadProductWordPreview');
    const previewEnd = workbenchSource.indexOf('function updateDocumentHistory', previewStart);
    const previewSource = workbenchSource.slice(previewStart, previewEnd);

    expect(workbenchSource).toContain('previewSalesProductDesignerDocument');
    expect(workbenchSource).toContain('class="word-pdf-preview-frame"');
    expect(workbenchSource).toContain('title="产品 Word PDF 预览"');
    expect(workbenchSource).toContain('downloadProductWordPreview');
    expect(workbenchSource).not.toContain('selectedScenicIntroductionTitles');
    expect(previewSource).toContain("!contentType.includes('application/pdf')");
    expect(previewSource.indexOf('productWordPreviewUrl.value = nextUrl')).toBeLessThan(
      previewSource.indexOf('URL.revokeObjectURL(previousUrl)'),
    );
  });

  it('uses a focused two-column Word workbench without supplier configuration controls', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');
    const wordPlanWorkspaceSource = getDayWordPlanWorkspaceSource(workbenchSource);

    expect(wordPlanWorkspaceSource).toContain('选择内容');
    expect(wordPlanWorkspaceSource).toContain('调整顺序');
    expect(wordPlanWorkspaceSource).toContain('保存并刷新预览');
    expect(wordPlanWorkspaceSource).not.toContain('实时预览');
    expect(wordPlanWorkspaceSource).not.toContain('保存当天方案');
    expect(wordPlanWorkspaceSource).toContain('图片设置');
    expect(wordPlanWorkspaceSource).toContain('wordPlanImagePickerOpen');
    expect(wordPlanWorkspaceSource).toContain('class="word-pdf-preview-frame"');
    expect(wordPlanWorkspaceSource).toContain('downloadProductWordPreview');
    expect(wordPlanWorkspaceSource).not.toContain('openSupplierConfig(resource.dayResource)');
    expect(wordPlanWorkspaceSource).not.toContain('未配置供应商');
    expect(wordPlanWorkspaceSource).not.toContain('供应商配置');
  });

  it('prefetches and reuses the current day Word plan instead of opening on a blank request', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(workbenchSource).toContain('const wordPlanCache = new Map');
    expect(workbenchSource).toContain('const wordPlanRequests = new Map');
    expect(workbenchSource).toContain('function scheduleDayWordPlanPrefetch()');
    expect(workbenchSource).toContain("day.resources.some((item) => item.resourceType === 'scenic')");
    expect(workbenchSource).toContain('dayNos.forEach((dayNo) =>');
    expect(workbenchSource).toContain('void fetchDayWordPlan(dayNo)');
    expect(workbenchSource).toContain('const data = await fetchDayWordPlan(activeDayNo.value)');
    expect(workbenchSource).toContain('正在准备 D{{ activeDayNo }} 的 Word 内容');
  });

  it('keeps a deleted resource snapshot from crashing the day Word workbench', () => {
    const apiSource = readAppFile('src/api/sales/product-designer.ts');
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');

    expect(apiSource).toContain('resourceDetail: null | ResourceDetail');
    expect(workbenchSource).toContain('resource.resourceDetail?.introductions.filter');
    expect(workbenchSource).toContain('resource.resourceDetail?.images || []');
    expect(workbenchSource).toContain('资源资料已删除，仅保留当天行程快照');
  });

  it('keeps scenic groups isolated by the day resource id and explains incompatible legacy image counts', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');
    const groupKeyStart = workbenchSource.indexOf('function wordPlanScenicGroupKey');
    const groupKeyEnd = workbenchSource.indexOf('const wordPlanSelectedGroups', groupKeyStart);
    const groupKeySource = workbenchSource.slice(groupKeyStart, groupKeyEnd);

    expect(groupKeySource).toContain('return String(item.dayResourceId)');
    expect(groupKeySource).not.toContain('resourceName');
    expect(workbenchSource).toContain('历史方案已选');
    expect(workbenchSource).toContain('超过当前 3 张上限');
  });

  it('keeps optional-item checkbox keys stable with resourceOptionalItemId', () => {
    const workbenchSource = readAppFile('src/views/sales/product/designer.vue');
    const keyStart = workbenchSource.indexOf('function wordPlanMaterialKey');
    const keyEnd = workbenchSource.indexOf('function wordPlanFingerprint', keyStart);
    const keySource = workbenchSource.slice(keyStart, keyEnd);

    expect(keySource).toContain("item.materialType === 'optional_item'");
    expect(keySource).toContain('? item.resourceOptionalItemId');
    expect(keySource).not.toContain("? item.introductionId");
    expect(workbenchSource).toContain(
      ":checked=\"isWordPlanSelected({ dayResourceId: resource.dayResource.id, materialType: 'optional_item', resourceOptionalItemId: candidate.optionalItem.id })\"",
    );
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
