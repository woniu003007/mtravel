import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('product designer daily itinerary editor', () => {
  it('uses the persisted daily destination to drive day cards and both map views', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');
    const workspaceSource = readAppFile('src/views/sales/product/components/ProductResourceMapWorkspace.vue');

    expect(source).toContain('saveSalesProductDesignerDayDestination');
    expect(source).toContain('const activeDayDestination');
    expect(source).toContain('destinationCity');
    expect(source).toContain('saveManualDayDestination');
    expect(source).toContain('syncDayDestinationFromHotel');
    expect(source).toContain('已跟随当晚酒店所在地自动设置');
    expect(source).toContain('输入城市，如无锡');
    expect(source).not.toContain('@change-day-destination');
    expect(workspaceSource).toContain('住宿城市');
    expect(workspaceSource).not.toContain('当天主城市');
    expect(workspaceSource).toContain('dayDestination');
  });

  it('keeps day switching separate from inline accommodation and meal resource arranging', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');

    expect(source).toContain('ProductDesignerDayArrangementPanel');
    expect(source).toContain('select-hotel');
    expect(source).toContain('select-meal-resource');
    expect(source).toContain('hotelBreakfastIncluded');
    expect(source).toContain('replaceBreakfastSource');
    expect(source).toContain("arrangementRole: 'accommodation'");
    expect(source).not.toContain('v-model:open="itineraryDrawerOpen"');
  });

  it('treats hotels and restaurants as day arrangements instead of free-text meal flags', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');
    const panelSource = readAppFile('src/views/sales/product/components/ProductDesignerDayArrangementPanel.vue');

    expect(source).toContain('ProductDesignerDayArrangementPanel');
    expect(source).toContain('hotelSelectionTarget');
    expect(source).toContain('mealSelectionTarget');
    expect(source).toContain('select-meal-resource');
    expect(source).toContain('activateMapResource');
    expect(source).toContain('resolveArrangementTarget(resource.resourceType)');
    expect(source).toContain('mealSelectionTarget.value');
    expect(source).toContain('restaurantMealPickerOpen');
    expect(source).not.toContain('startGroundServiceSelection');
    expect(source).not.toContain("{ label: '地接', value: 'ground_agent' }");
    expect(panelSource).not.toContain('data-arrangement-section="ground-service"');
    expect(panelSource).not.toContain('select-ground-service-resource');
    expect(source).not.toContain('Form.Item label="住宿城市"');
  });

  it('replaces the current hotel from the map instead of attempting a second accommodation insert', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');
    const panelSource = readAppFile('src/views/sales/product/components/ProductDesignerDayArrangementPanel.vue');

    expect(source).toContain('const activeAccommodation = computed');
    expect(source).toContain("target.role === 'accommodation' ? activeAccommodation.value : undefined");
    expect(source).toContain("return activeAccommodation.value ? '更换住宿' : '安排住宿'");
    expect(panelSource).toContain('仅安排一家酒店');
    expect(panelSource).toContain("accommodations.length ? '更换酒店' : '选择酒店'");
  });

  it('exposes resource arrangement roles without changing the legacy itinerary contract', () => {
    const apiSource = readAppFile('src/api/sales/product-designer.ts');

    expect(apiSource).toContain('DayItinerary');
    expect(apiSource).toContain("'/sales/product/designer/day-itinerary/save'");
    expect(apiSource).toContain('accommodationCity');
    expect(apiSource).toContain('breakfastIncluded');
    expect(apiSource).toContain('lunchIncluded');
    expect(apiSource).toContain('dinnerIncluded');
    expect(apiSource).toContain('arrangementRole');
    expect(apiSource).toContain("'ground_service'");
    expect(apiSource).toContain('BreakfastPlan');
  });
});
