import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import { buildDocumentImportResourceGroups, documentImportResourceTimeValue } from './document-import-resource-groups';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('sales team document import draft layout', () => {
  it('uses only a system customer selection while keeping contact fields editable', () => {
    const source = readAppFile('src/views/sales/team/create.vue');
    const api = readAppFile('src/api/sales/team-document-import.ts');

    expect(source).toContain('<span>客户单位</span>');
    expect(source).toContain('<span>联系人</span>');
    expect(source).toContain('<span>联系电话</span>');
    expect(source).toContain('getCustomerUnitPage');
    expect(source).toContain('documentImportDraft.order!.customerId');
    expect(source).toContain('搜索并选择系统客户');
    expect(source).toContain('识别到的客户单位');
    expect(source).not.toContain('<Input v-model:value="documentImportDraft.order!.customerName"');
    expect(source).toContain('请先从系统客户主档选择客户单位，再填入团队');
    expect(source).toContain('documentImportDraft.order!.contactName');
    expect(source).toContain('documentImportDraft.order!.contactPhone');
    expect(source).toContain('grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));');

    expect(api).toContain('contactPhone?: string;');
    expect(api).toContain('customerId?: number;');
  });

  it('renders Chinese labels and groups imported resources by day then source type', () => {
    const source = readAppFile('src/views/sales/team/create.vue');
    const api = readAppFile('src/api/sales/team-document-import.ts');

    expect(source).toContain("ground_confirmation: '地接确认单'");
    expect(source).toContain("scenic: '景区'");
    expect(source).toContain('buildDocumentImportResourceGroups(documentImportDraft.value?.resources)');
    expect(source).toContain('v-for="dayGroup in documentImportResourceGroups"');
    expect(source).toContain('arrangementTypeLabel(typeGroup.arrangementType)');
    expect(source).not.toContain('<Tag>{{ resource.arrangementType }}</Tag>');
    expect(source).toContain('aria-label="移除这条导入资源"');
    expect(source).toContain('removeDocumentImportResource(resource)');
    expect(source).toContain('resources.splice(index, 1)');
    expect(source).toContain('可搜索并重新选择系统资源');
    expect(source).toContain('getPurchaseResourcePage');
    expect(source).toContain('getPurchaseResourceBindings');
    expect(source).toContain('show-search');
    expect(source).toContain('搜索或选择系统资源');
    expect(source).toContain("'已预填 / 待补供应商'");
    expect(source).toContain("'已选资源 / 待补供应商'");
    expect(source).toContain('draft.productDescription');
    expect(source).toContain('formState.warmReminder');
    expect(source).toContain('formState.attentionItems');
    expect(api).toContain('time?: string;');
    expect(api).toContain('export interface ProductDescriptionDraft');
  });

  it('copies every imported product-description field into the team form and save payload', () => {
    const source = readAppFile('src/views/sales/team/create.vue');
    const mappings = [
      ['content', 'productDescription'],
      ['feeIncluded', 'feeIncluded'],
      ['feeExcluded', 'feeExcluded'],
      ['childPolicy', 'childPolicy'],
      ['shoppingArrangement', 'shoppingArrangement'],
      ['optionalItems', 'optionalItems'],
      ['giftItems', 'giftItems'],
      ['attentionItems', 'attentionItems'],
      ['warmReminder', 'warmReminder'],
    ];

    for (const [draftField, formField] of mappings) {
      expect(source).toContain(
        `formState.${formField} = importedText(productDescription.${draftField}, formState.${formField});`,
      );
      expect(source).toContain(`${formField}: cleanProductText(formState.${formField}),`);
    }
  });

  it('groups by day and type, then sorts known times without reordering unknown Word times', () => {
    const resource = (itemKey: string, dayNo: number, arrangementType: 'hotel' | 'scenic' | 'shopping', time?: string) => ({
      arrangementType,
      dayNo,
      itemKey,
      requiresConfirmation: true,
      sourceName: itemKey,
      time,
    });
    const groups = buildDocumentImportResourceGroups([
      resource('d2-scenic', 2, 'scenic', '09:30'),
      resource('d1-hotel-late', 1, 'hotel', '16:00'),
      resource('d1-scenic-afternoon', 1, 'scenic', '下午 2:00'),
      resource('d1-scenic-morning', 1, 'scenic', '08:30'),
      resource('d1-hotel-morning', 1, 'hotel', '10:00'),
      resource('d1-shopping-no-time-a', 1, 'shopping'),
      resource('d1-shopping-no-time-b', 1, 'shopping'),
    ]);

    expect(groups.map((item) => item.label)).toEqual(['第1天', '第2天']);
    const firstDay = groups[0];
    if (!firstDay) throw new Error('缺少第 1 天资源分组');
    const [hotelGroup, scenicGroup, shoppingGroup] = firstDay.typeGroups;
    if (!hotelGroup || !scenicGroup || !shoppingGroup) throw new Error('缺少预期的资源类型分组');
    expect(firstDay.typeGroups.map((item) => item.arrangementType)).toEqual(['hotel', 'scenic', 'shopping']);
    expect(hotelGroup.resources.map((item) => item.itemKey)).toEqual(['d1-hotel-morning', 'd1-hotel-late']);
    expect(scenicGroup.resources.map((item) => item.itemKey)).toEqual(['d1-scenic-morning', 'd1-scenic-afternoon']);
    expect(shoppingGroup.resources.map((item) => item.itemKey)).toEqual(['d1-shopping-no-time-a', 'd1-shopping-no-time-b']);
    expect(documentImportResourceTimeValue(resource('afternoon', 1, 'scenic', '下午 2:00'))).toBe(14 * 60);
  });
});
