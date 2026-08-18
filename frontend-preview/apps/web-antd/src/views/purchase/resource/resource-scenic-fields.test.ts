import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

function sourceBetween(source: string, start: string, end: string) {
  const startIndex = source.indexOf(start);
  const endIndex = source.indexOf(end, startIndex + start.length);
  expect(startIndex).toBeGreaterThanOrEqual(0);
  expect(endIndex).toBeGreaterThan(startIndex);
  return source.slice(startIndex, endIndex);
}

describe('purchase resource place fields', () => {
  it('defines resource typed fields, filters and common map endpoints', () => {
    const apiSource = readAppFile('src/api/purchase/resource.ts');

    for (const type of [
      'scenic',
      'hotel',
      'restaurant',
      'shopping',
      'vehicle',
      'traffic',
      'ground_agent',
      'other',
    ]) {
      expect(apiSource).toContain(`'${type}'`);
    }

    for (const field of [
      'businessStatus',
      'closingTime',
      'lastSiteVisitDate',
      'latitude',
      'longitude',
      'openingTime',
      'scenicLevel',
      'siteVisitNote',
      'siteVisitStatus',
    ]) {
      expect(apiSource).toContain(field);
    }
    expect(apiSource).toContain("'/common/map/amap/tips'");
    expect(apiSource).toContain("'/common/map/amap/js-config'");
    expect(apiSource).toContain("'/common/map/amap/regeo'");
    expect(apiSource).not.toContain("'/sales/product/roadbook/amap");
    expect(apiSource).toContain("export type ProcurementMode = 'not_required' | 'required';");
    expect(apiSource).toContain('procurementMode: ProcurementMode;');
    expect(apiSource).toContain('procurementMode?: ProcurementMode;');
  });

  it('lets users maintain and filter the two default procurement attributes', () => {
    const source = readAppFile('src/views/purchase/resource/index.vue');
    const payloadSource = sourceBetween(
      source,
      'function buildPayload()',
      'function validateScenicForm()',
    );

    expect(source).toContain("{ label: '无需采购', value: 'not_required' }");
    expect(source).toContain("{ label: '需要采购', value: 'required' }");
    expect(source).toContain('<Form.Item label="采购属性">');
    expect(source).toContain('<Form.Item label="默认采购属性" required>');
    expect(source).toContain("formState.procurementMode = row.procurementMode || 'required'");
    expect(payloadSource).toContain("procurementMode: formState.procurementMode || 'required'");
    expect(payloadSource).toContain("formState.procurementMode === 'not_required'");
    expect(source).toContain("formState.procurementMode !== 'not_required'");
  });

  it('exposes resource documents for every resource type', () => {
    const apiSource = readAppFile('src/api/purchase/resource.ts');
    const source = readAppFile('src/views/purchase/resource/index.vue');
    const documentButtonIndex = source.indexOf(
      '@click="openDocumentDrawer(record)"',
    );

    expect(documentButtonIndex).toBeGreaterThanOrEqual(0);
    expect(source.slice(documentButtonIndex - 160, documentButtonIndex + 100)).not.toContain(
      "record.resourceType === 'scenic'",
    );
    expect(source).toContain('>资源资料</Button');
    expect(source).toContain('`资源资料 - ${currentResource?.resourceName || \'\'}`');
    expect(source).toContain('>上传资源文件</Button>');
    expect(source).not.toContain('景区资料');
    expect(apiSource).toContain('export interface ResourceDocumentItem');
    expect(apiSource).not.toContain('ScenicDocumentItem');
  });

  it('keeps optional introduction notices in the resource material lifecycle', () => {
    const apiSource = readAppFile('src/api/purchase/resource.ts');
    const source = readAppFile('src/views/purchase/resource/index.vue');
    const selectSource = sourceBetween(
      source,
      'function selectIntroduction',
      'async function loadResourceIntroductions',
    );
    const payloadSource = sourceBetween(
      source,
      'function introductionPayload()',
      'async function saveIntroduction',
    );

    expect(apiSource).toContain('noticeContent?: string;');
    expect(source).toContain("introductionForm.noticeContent = ''");
    expect(selectSource).toContain('record?.noticeContent || \'\'');
    expect(payloadSource).toContain('noticeContent: (introductionForm.noticeContent || \'\').trim()');
    expect(source).toContain('<Form.Item\n                    label="注意事项（选填）"');
    expect(source).toContain('一行一条，生成产品资料时会以红色强调。');
    expect(source).toContain('resource-material-intro-notice-preview');
    expect(source).toContain('color: #cf1322;');
  });

  it('uses the current resource pricing unit for unified supplier quotes', () => {
    const source = readAppFile('src/views/purchase/resource/index.vue');

    expect(source).toContain('const unifiedPriceUnit = computed(() => {');
    expect(source).toContain("daily: '元/天'");
    expect(source).toContain("trip: '元/趟'");
    expect(source).toContain("if (resource.resourceType === 'hotel') return '元/间夜'");
    expect(source).toContain("if (resource.resourceType === 'ground_agent') return '元/团'");
    expect(source).toContain(":addon-after=\"unifiedPriceUnit\"");
    expect(source).not.toContain('addon-after="元/人"');
  });

  it('shows scenic filters and place columns/form fields in the right resource mode', () => {
    const source = readAppFile('src/views/purchase/resource/index.vue');

    expect(source).toContain(
      "const isScenicList = computed(() => query.resourceType === 'scenic')",
    );
    expect(source).toContain('const isPlaceList = computed(');
    expect(source).toContain('const isPlaceForm = computed(');
    expect(source).toContain(
      '<Form.Item v-if="isScenicList" label="国家 A 级">',
    );
    expect(source).toContain(
      '<Form.Item v-if="isScenicList" label="营业状态">',
    );
    expect(source).toContain(
      '<Form.Item v-if="isScenicList" label="踩点状态">',
    );
    expect(source).toContain(
      '<section v-if="isPlaceForm" class="scenic-section">',
    );
    expect(source).toContain(
      '<Form.Item v-if="isScenicForm" label="国家 A 级">',
    );
    expect(source).toContain('...scenicColumns');
    expect(source).toContain('query.scenicLevel = undefined');
    expect(source).toContain('query.businessStatus = undefined');
    expect(source).toContain('query.siteVisitStatus = undefined');
  });

  it('keeps scenic-only fields scenic and place fields limited to place resource save payloads', () => {
    const source = readAppFile('src/views/purchase/resource/index.vue');
    const buildPayloadSource = sourceBetween(
      source,
      'function buildPayload()',
      'function validateScenicForm()',
    );
    const scenicGuardIndex = buildPayloadSource.indexOf(
      "if (formState.resourceType === 'scenic')",
    );
    const placeGuardIndex = buildPayloadSource.indexOf(
      'if (isPlaceResource(formState.resourceType))',
    );

    expect(scenicGuardIndex).toBeGreaterThanOrEqual(0);
    expect(placeGuardIndex).toBeGreaterThan(scenicGuardIndex);
    const commonPayloadSource = buildPayloadSource.slice(0, scenicGuardIndex);
    expect(commonPayloadSource).not.toContain('payload.scenicLevel');
    expect(commonPayloadSource).not.toContain('payload.businessStatus');
    expect(commonPayloadSource).not.toContain('payload.siteVisitStatus');
    expect(buildPayloadSource.slice(scenicGuardIndex)).toContain(
      'payload.scenicLevel',
    );
    expect(buildPayloadSource.slice(placeGuardIndex)).toContain(
      'payload.longitude',
    );
    expect(buildPayloadSource.slice(placeGuardIndex)).toContain(
      'payload.latitude',
    );
    expect(buildPayloadSource.slice(placeGuardIndex)).toContain(
      'payload.businessStatus',
    );
    expect(buildPayloadSource.slice(placeGuardIndex)).toContain(
      'payload.siteVisitStatus',
    );
  });

  it('restores place edit data and keeps map selection away from province fields', () => {
    const source = readAppFile('src/views/purchase/resource/index.vue');
    const editSource = sourceBetween(
      source,
      'function openEditModal',
      'function buildPayload',
    );
    const clickSource = sourceBetween(
      source,
      'async function handleScenicMapClick',
      'function clearScenicMapSearchTimer',
    );

    expect(editSource).toContain('formState.scenicLevel = row.scenicLevel');
    expect(editSource).toContain(
      'formState.businessStatus = row.businessStatus',
    );
    expect(editSource).toContain(
      'formState.siteVisitStatus = row.siteVisitStatus',
    );
    expect(editSource).toContain('formState.longitude = row.longitude');
    expect(editSource).toContain('formState.latitude = row.latitude');
    expect(clickSource).toContain(
      'applyScenicMapPosition(longitude, latitude)',
    );
    expect(clickSource).toContain('formState.address = result.address.trim()');
    expect(clickSource).not.toContain('formState.province');
    expect(clickSource).not.toContain('formState.city');
    expect(clickSource).not.toContain('formState.district');
    expect(source).toContain('地图暂不可用，可继续手工填写详细地址和经纬度。');
  });

  it('supports hand-written place address location without forcing province fields', () => {
    const source = readAppFile('src/views/purchase/resource/index.vue');
    const locateSource = sourceBetween(
      source,
      'async function locateScenicMapByKeyword',
      'function locateScenicMapByAddress',
    );

    expect(source).toContain('<Input.Search');
    expect(source).toContain('placeholder="可手写详细地址，输入后可定位"');
    expect(source).toContain('@search="locateScenicMapByAddress"');
    expect(source).toContain(
      'locateScenicMapByKeyword(buildScenicLocateKeyword(), false, false)',
    );
    expect(locateSource).toContain('applyScenicMapPosition');
    expect(locateSource).toContain('scenicMapInstance?.setZoomAndCenter');
    expect(locateSource).not.toContain('formState.province');
    expect(locateSource).not.toContain('formState.city');
    expect(locateSource).not.toContain('formState.district');
  });
});
