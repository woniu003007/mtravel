import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('product designer daily itinerary editor', () => {
  it('keeps day switching separate from inline accommodation and meal resource arranging', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');

    expect(source).toContain('当天住宿');
    expect(source).toContain('选择酒店');
    expect(source).toContain('选择餐厅');
    expect(source).toContain('hotelBreakfastIncluded');
    expect(source).toContain('nextDayBreakfast');
    expect(source).toContain("arrangementRole: 'accommodation'");
    expect(source).not.toContain('v-model:open="itineraryDrawerOpen"');
  });

  it('treats hotels and restaurants as day arrangements instead of free-text meal flags', () => {
    const source = readAppFile('src/views/sales/product/designer.vue');

    expect(source).toContain('当天住宿');
    expect(source).toContain('hotelSelectionTarget');
    expect(source).toContain('mealSelectionTarget');
    expect(source).toContain('选择餐厅');
    expect(source).toContain('activateMapResource');
    expect(source).toContain("resource.resourceType === 'hotel'");
    expect(source).toContain('mealSelectionTarget.value');
    expect(source).toContain('restaurantMealPickerOpen');
    expect(source).toContain('activeAccommodations');
    expect(source).not.toContain('Form.Item label="住宿城市"');
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
  });
});
