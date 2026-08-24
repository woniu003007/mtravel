<script lang="ts" setup>
import { computed } from 'vue';
import { Button, Checkbox, Empty, Tooltip } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import type { SalesProductDesignerApi } from '#/api/sales/product-designer';
import ProductResourceSupplierPicker from './ProductResourceSupplierPicker.vue';
import { formatProductResourceName } from './product-resource-map-marker';

export type ProductDesignerArrangementRole =
  | 'accommodation'
  | 'breakfast'
  | 'dinner'
  | 'ground_service'
  | 'itinerary'
  | 'lunch'
  | 'unassigned';
export type ProductDesignerMealRole = 'breakfast' | 'lunch' | 'dinner';

export type ProductDesignerArrangedResource = Omit<SalesProductDesignerApi.DayResource, 'arrangementRole'> & {
  arrangementRole: ProductDesignerArrangementRole;
  supplierRelationId?: number;
};

/** 后端计算早餐来源；面板只读取该结果，不根据前一天酒店重新推导。 */
export interface ProductDesignerBreakfastPlan {
  hotelResourceNames?: string[];
  source: 'hotel' | 'none' | 'restaurant';
}

const props = withDefaults(defineProps<{
  activeDayNo: number;
  breakfastPlan?: ProductDesignerBreakfastPlan;
  highlightedResourceId?: number;
  resources: ProductDesignerArrangedResource[];
  supplierCandidatesByResourceId?: Record<number, SalesProductDesignerApi.Supplier[]>;
  travelDays: number;
}>(), {
  breakfastPlan: undefined,
  highlightedResourceId: undefined,
  supplierCandidatesByResourceId: () => ({}),
});

const emit = defineEmits<{
  (event: 'change-supplier', row: ProductDesignerArrangedResource, relationId: number): void;
  (event: 'remove-day-resource', row: ProductDesignerArrangedResource): void;
  (event: 'reorder-day-resources', role: 'accommodation' | 'itinerary', resourceIds: number[]): void;
  (event: 'request-suppliers', resourceId: number): void;
  (event: 'select-hotel'): void;
  (event: 'select-itinerary-resource'): void;
  (event: 'select-meal-resource', role: ProductDesignerMealRole): void;
  (event: 'toggle-hotel-breakfast', row: ProductDesignerArrangedResource, included: boolean): void;
}>();

const meals: { label: string; role: ProductDesignerMealRole }[] = [
  { label: '早餐', role: 'breakfast' },
  { label: '中餐', role: 'lunch' },
  { label: '晚餐', role: 'dinner' },
];
type MealCardState = 'empty' | 'filled' | 'hotel';
const mealCards = computed(() => meals.map((meal) => {
  const row = mealRows.value[meal.role];
  if (row) {
    return {
      actionLabel: '更换餐厅',
      label: meal.label,
      resource: row,
      showSupplier: row.procurementMode !== 'not_required',
      state: 'filled' as MealCardState,
      subtitle: resourceCostText(row),
      title: formatProductResourceName(row.resourceName),
      role: meal.role,
    };
  }
  if (meal.role === 'breakfast' && props.breakfastPlan?.source === 'hotel') {
    return {
      actionLabel: '改选餐厅',
      label: meal.label,
      resource: undefined,
      showSupplier: false,
      state: 'hotel' as MealCardState,
      subtitle: props.breakfastPlan.hotelResourceNames?.join('、') || '前一晚酒店已包含早餐',
      title: '酒店含早',
      role: meal.role,
    };
  }
  return {
    actionLabel: '选择餐厅',
    label: meal.label,
    resource: undefined,
    showSupplier: false,
    state: 'empty' as MealCardState,
    subtitle: meal.role === 'breakfast' ? '可由酒店含早或餐厅早餐承接' : '尚未安排',
    title: '未安排',
    role: meal.role,
  };
}));
const typeLabel: Record<string, string> = {
  hotel: '酒店', other: '其它', restaurant: '餐厅', scenic: '景区', shopping: '购物',
};

const accommodations = computed(() => rowsByRole('accommodation'));
const itineraryRows = computed(() => rowsByRole('itinerary'));
const mealRows = computed<Record<ProductDesignerMealRole, ProductDesignerArrangedResource | undefined>>(() => ({
  breakfast: rowsByRole('breakfast')[0],
  lunch: rowsByRole('lunch')[0],
  dinner: rowsByRole('dinner')[0],
}));

function rowsByRole(role: ProductDesignerArrangementRole) {
  return props.resources
    .filter((item) => item.arrangementRole === role)
    .sort((left, right) => left.sortOrder - right.sortOrder);
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function resourceCostText(row: ProductDesignerArrangedResource) {
  if (row.procurementMode === 'not_required') return '无需采购 · 成本 ¥0.00';
  if (!row.supplierName) return '待询价 · ¥0.00';
  return `${row.supplierName} · 成本 ${formatMoney(row.costAmount)}`;
}

function moveRow(
  role: 'accommodation' | 'itinerary',
  rows: ProductDesignerArrangedResource[],
  index: number,
  direction: -1 | 1,
) {
  const targetIndex = index + direction;
  if (targetIndex < 0 || targetIndex >= rows.length) return;
  const ordered = [...rows];
  const current = ordered[index];
  const target = ordered[targetIndex];
  if (!current || !target) return;
  ordered[index] = target;
  ordered[targetIndex] = current;
  emit('reorder-day-resources', role, ordered.map((item) => item.id));
}

function isHighlighted(row: ProductDesignerArrangedResource) {
  return row.id === props.highlightedResourceId;
}
</script>

<template>
  <div class="product-designer-day-arrangement-panel">
    <section class="day-arrangement-section" data-arrangement-section="accommodation">
      <header class="day-arrangement-section__heading">
        <div><strong>当天住宿</strong><span>D{{ activeDayNo }} 晚，仅安排一家酒店</span></div>
        <Button size="small" type="primary" @click="emit('select-hotel')">{{ accommodations.length ? '更换酒店' : '选择酒店' }}</Button>
      </header>
      <div v-if="accommodations.length" class="day-arrangement-list">
        <div v-for="(row, index) in accommodations" :key="row.id" class="day-arrangement-row day-arrangement-row--accommodation" :class="{ 'is-highlighted': isHighlighted(row) }">
          <span class="day-arrangement-row__index">{{ index + 1 }}</span>
          <div class="day-arrangement-row__main">
            <Tooltip :title="formatProductResourceName(row.resourceName)"><strong>{{ formatProductResourceName(row.resourceName) }}</strong></Tooltip>
            <span>{{ resourceCostText(row) }}</span>
          </div>
          <div class="day-arrangement-row__accommodation-meta">
            <Checkbox :checked="row.hotelBreakfastIncluded" :disabled="activeDayNo >= travelDays" @change="emit('toggle-hotel-breakfast', row, Boolean($event.target?.checked))">含次日早餐</Checkbox>
            <div class="day-arrangement-row__actions">
              <ProductResourceSupplierPicker :row="row" :suppliers="supplierCandidatesByResourceId[row.resourceId] || []" @request-suppliers="emit('request-suppliers', row.resourceId)" @supplier-relation-change="emit('change-supplier', row, $event)" />
              <Button danger size="small" type="link" @click="emit('remove-day-resource', row)">移除</Button>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="day-arrangement-empty"><span>尚未安排酒店。</span><Button size="small" type="link" @click="emit('select-hotel')">选择酒店</Button></div>
    </section>

    <section class="day-arrangement-section" data-arrangement-section="meals">
      <header class="day-arrangement-section__heading"><div><strong>当天用餐</strong><span>早、中、晚餐分别安排；每餐仅保留一项</span></div></header>
      <div class="day-arrangement-meal-grid">
        <article v-for="meal in mealCards" :key="meal.label" class="day-arrangement-meal-card" :class="`is-${meal.state}`">
          <div class="day-arrangement-meal-card__head">
            <span class="day-arrangement-meal-card__label">{{ meal.label }}</span>
            <span class="day-arrangement-meal-card__status">{{ meal.state === 'filled' ? '已安排' : meal.state === 'hotel' ? '酒店含早' : '未安排' }}</span>
          </div>
          <div class="day-arrangement-meal-card__main">
            <Tooltip :title="meal.title">
              <strong>{{ meal.title }}</strong>
            </Tooltip>
            <span :title="meal.subtitle">{{ meal.subtitle }}</span>
          </div>
          <div class="day-arrangement-meal-card__actions">
            <template v-if="meal.state === 'filled'">
              <ProductResourceSupplierPicker
                v-if="meal.showSupplier && meal.resource"
                :row="meal.resource"
                :suppliers="supplierCandidatesByResourceId[meal.resource.resourceId] || []"
                @request-suppliers="emit('request-suppliers', meal.resource.resourceId)"
                @supplier-relation-change="emit('change-supplier', meal.resource, $event)"
              />
              <Button danger size="small" type="link" @click="emit('remove-day-resource', meal.resource!)">清除</Button>
              <Button size="small" type="link" @click="emit('select-meal-resource', meal.role)">更换餐厅</Button>
            </template>
            <template v-else>
              <Button size="small" type="link" @click="emit('select-meal-resource', meal.role)">{{ meal.actionLabel }}</Button>
            </template>
          </div>
        </article>
      </div>
    </section>

    <section class="day-arrangement-section" data-arrangement-section="itinerary">
      <header class="day-arrangement-section__heading"><div><strong>当日行程</strong><span>景区、购物及其它资源按顺序编排</span></div><Button size="small" type="primary" @click="emit('select-itinerary-resource')">从地图选择</Button></header>
      <div v-if="itineraryRows.length" class="day-arrangement-list">
        <div v-for="(row, index) in itineraryRows" :key="row.id" class="day-arrangement-row" :class="{ 'is-highlighted': isHighlighted(row) }">
          <span class="day-arrangement-row__index">{{ index + 1 }}</span>
          <div class="day-arrangement-row__main"><Tooltip :title="formatProductResourceName(row.resourceName)"><strong>{{ formatProductResourceName(row.resourceName) }}</strong></Tooltip><span>{{ typeLabel[row.resourceType] || row.resourceType }} · {{ resourceCostText(row) }}</span></div>
          <ProductResourceSupplierPicker :row="row" :suppliers="supplierCandidatesByResourceId[row.resourceId] || []" @request-suppliers="emit('request-suppliers', row.resourceId)" @supplier-relation-change="emit('change-supplier', row, $event)" />
          <div class="day-arrangement-row__actions"><Button :disabled="index === 0" size="small" type="link" @click="moveRow('itinerary', itineraryRows, index, -1)"><IconifyIcon icon="lucide:chevron-up" /></Button><Button :disabled="index === itineraryRows.length - 1" size="small" type="link" @click="moveRow('itinerary', itineraryRows, index, 1)"><IconifyIcon icon="lucide:chevron-down" /></Button><Button danger size="small" type="link" @click="emit('remove-day-resource', row)">删除</Button></div>
        </div>
      </div>
      <div v-else class="day-arrangement-empty"><Empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="尚未安排当日行程"><Button size="small" type="link" @click="emit('select-itinerary-resource')">从地图选择</Button></Empty></div>
    </section>

  </div>
</template>

<style scoped>
.product-designer-day-arrangement-panel { display: grid; gap: 10px; }
.day-arrangement-section { overflow: hidden; border: 1px solid #f0f0f0; border-radius: 6px; }
.day-arrangement-section__heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 46px; padding: 8px 12px; background: #fafafa; border-bottom: 1px solid #f0f0f0; }
.day-arrangement-section__heading strong, .day-arrangement-section__heading span { display: block; }
.day-arrangement-section__heading strong { color: #262626; font-size: 14px; }.day-arrangement-section__heading span { margin-top: 2px; color: #8c8c8c; font-size: 12px; }
.day-arrangement-list { display: grid; }
.day-arrangement-row { display: flex; align-items: center; gap: 8px; min-height: 48px; padding: 6px 10px; border-bottom: 1px solid #f5f5f5; transition: background-color .2s ease; }.day-arrangement-row:last-child { border-bottom: 0; }.day-arrangement-row.is-highlighted { background: #e6f4ff; }
.day-arrangement-row__index { display: grid; width: 20px; height: 20px; flex: 0 0 auto; color: #1677ff; font-size: 12px; background: #e6f4ff; border-radius: 50%; place-items: center; }
.day-arrangement-row__main { min-width: 0; flex: 1; }.day-arrangement-row__main strong, .day-arrangement-row__main span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.day-arrangement-row__main strong { color: #334155; font-size: 13px; }.day-arrangement-row__main span { margin-top: 2px; color: #64748b; font-size: 12px; }
.day-arrangement-row__actions { display: flex; flex: 0 0 auto; align-items: center; }.day-arrangement-row__actions :deep(.ant-btn) { padding-inline: 4px; }
.day-arrangement-row--accommodation { display: grid; grid-template-columns: 20px minmax(0, 1fr); align-items: start; column-gap: 8px; row-gap: 6px; padding-block: 8px; }
.day-arrangement-row--accommodation .day-arrangement-row__index { margin-top: 3px; }
.day-arrangement-row--accommodation .day-arrangement-row__main { min-width: 0; }
.day-arrangement-row__accommodation-meta { display: flex; grid-column: 2; align-items: center; justify-content: space-between; gap: 8px; min-width: 0; }
.day-arrangement-row__accommodation-meta :deep(.ant-checkbox-wrapper) { flex: 0 0 auto; margin-inline-start: 0; color: #595959; font-size: 12px; white-space: nowrap; }
.day-arrangement-empty { display: flex; align-items: center; justify-content: space-between; min-height: 52px; padding: 8px 12px; color: #8c8c8c; font-size: 12px; }
.day-arrangement-empty :deep(.ant-empty) { width: 100%; margin-block: 8px; }
.day-arrangement-meal-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; padding: 8px; }
.day-arrangement-meal-card { display: flex; min-width: 0; min-height: 112px; flex-direction: column; gap: 8px; padding: 8px; border: 1px solid #edf0f5; border-radius: 8px; background: #fff; transition: border-color .2s ease, background-color .2s ease, box-shadow .2s ease, transform .2s ease; }
.day-arrangement-meal-card:hover { border-color: #dbe3ef; box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04); transform: translateY(-1px); }
.day-arrangement-meal-card.is-empty { background: #fbfdff; }
.day-arrangement-meal-card.is-hotel { background: #fffaf0; }
.day-arrangement-meal-card__head { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-width: 0; }
.day-arrangement-meal-card__label { color: #64748b; font-size: 12px; font-weight: 600; }
.day-arrangement-meal-card__status { display: inline-flex; flex: 0 0 auto; align-items: center; min-height: 20px; padding: 0 7px; border: 1px solid #d9e2ec; border-radius: 999px; color: #64748b; font-size: 11px; line-height: 18px; white-space: nowrap; background: #f8fafc; }
.day-arrangement-meal-card.is-filled .day-arrangement-meal-card__status { border-color: #bae0ff; color: #1677ff; background: #e6f4ff; }
.day-arrangement-meal-card.is-hotel .day-arrangement-meal-card__status { border-color: #ffd591; color: #d46b08; background: #fff7e6; }
.day-arrangement-meal-card__main { display: grid; min-width: 0; align-content: start; gap: 2px; }
.day-arrangement-meal-card__main strong, .day-arrangement-meal-card__main span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.day-arrangement-meal-card__main strong { color: #334155; font-size: 13px; line-height: 18px; }
.day-arrangement-meal-card__main span { color: #64748b; font-size: 12px; line-height: 18px; }
.day-arrangement-meal-card__actions { display: flex; flex-wrap: wrap; align-items: center; gap: 2px 4px; margin-top: auto; }
.day-arrangement-meal-card__actions :deep(.ant-btn) { padding-inline: 0; }
.day-arrangement-meal-card__actions :deep(.ant-btn-link) { height: 22px; color: #1677ff; }
.day-arrangement-meal-card__actions :deep(.ant-btn-dangerous) { color: #ff4d4f; }
@media (max-width: 900px) { .day-arrangement-row:not(.day-arrangement-row--accommodation) { flex-wrap: wrap; }.day-arrangement-row:not(.day-arrangement-row--accommodation) .day-arrangement-row__main { min-width: calc(100% - 28px); }.day-arrangement-row:not(.day-arrangement-row--accommodation) .day-arrangement-row__actions { margin-left: auto; } }
@media (max-width: 900px) { .day-arrangement-meal-grid { grid-template-columns: 1fr; } }
@media (max-width: 420px) { .day-arrangement-row__accommodation-meta { align-items: flex-start; flex-direction: column; gap: 2px; }.day-arrangement-row__accommodation-meta .day-arrangement-row__actions { margin-left: -4px; }.day-arrangement-meal-grid { padding: 8px 8px 6px; } .day-arrangement-meal-card { min-height: 96px; } }
</style>
