<script lang="ts" setup>
import { computed, ref } from 'vue';
import { Button, Tag, Tooltip } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import type { SalesProductDesignerApi } from '#/api/sales/product-designer';

const props = withDefaults(defineProps<{
  arrangements: SalesProductDesignerApi.VehicleArrangement[];
  totalCost?: number;
  travelDays: number;
}>(), {
  totalCost: 0,
});

const emit = defineEmits<{
  (event: 'add'): void;
  (event: 'edit', item: SalesProductDesignerApi.VehicleArrangement): void;
  (event: 'remove', item: SalesProductDesignerApi.VehicleArrangement): void;
  (event: 'reorder', ids: number[]): void;
}>();

const expanded = ref(false);

const orderedArrangements = computed(() => [...props.arrangements].sort(
  (left, right) => left.sortOrder - right.sortOrder,
));

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function serviceRange(item: SalesProductDesignerApi.VehicleArrangement) {
  if (!item.startDayNo && !item.endDayNo) return '全程待确认';
  if (item.startDayNo === 1 && item.endDayNo === props.travelDays) return '全程服务';
  return `D${item.startDayNo || 1} - D${item.endDayNo || props.travelDays}`;
}

function arrangementText(item: SalesProductDesignerApi.VehicleArrangement) {
  const vehicleName = item.vehicleType || item.resourceName;
  return `${serviceRange(item)} · ${vehicleName} × ${item.quantity || 1}${item.supplierName ? ` · ${item.supplierName}` : ''}`;
}

const arrangementSummary = computed(() => {
  if (!orderedArrangements.value.length) return '暂未安排，可按 D1-DN 分段配置用车';
  const segments = orderedArrangements.value.map(arrangementText);
  if (segments.length === 1) return segments[0];
  return `已安排 ${segments.length} 段：${segments.join('；')}`;
});

function move(index: number, direction: -1 | 1) {
  const targetIndex = index + direction;
  if (targetIndex < 0 || targetIndex >= orderedArrangements.value.length) return;
  const next = [...orderedArrangements.value];
  const current = next[index];
  const target = next[targetIndex];
  if (!current || !target) return;
  next[index] = target;
  next[targetIndex] = current;
  emit('reorder', next.map((item) => item.id));
}
</script>

<template>
  <section class="product-designer-vehicle-panel">
    <header class="product-designer-vehicle-panel__heading">
      <div class="product-designer-vehicle-panel__icon" aria-hidden="true">
        <IconifyIcon icon="lucide:bus-front" />
      </div>
      <div class="product-designer-vehicle-panel__summary">
        <div class="product-designer-vehicle-panel__title-line">
          <strong>全程用车</strong>
          <span>产品级跨日安排，不进入每天地图或当天行程</span>
        </div>
        <Tooltip :title="arrangementSummary">
          <span class="product-designer-vehicle-panel__summary-text">{{ arrangementSummary }}</span>
        </Tooltip>
      </div>
      <div class="product-designer-vehicle-panel__heading-actions">
        <Tag v-if="orderedArrangements.length" color="blue">{{ orderedArrangements.length }} 段</Tag>
        <span class="product-designer-vehicle-panel__cost">{{ formatMoney(totalCost) }}</span>
        <Button
          v-if="orderedArrangements.length"
          size="small"
          @click="expanded = !expanded"
        >
          {{ expanded ? '收起' : '管理安排' }}
          <IconifyIcon :icon="expanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
        </Button>
        <Button size="small" type="primary" @click="emit('add')">{{ orderedArrangements.length ? '增加用车段' : '安排用车' }}</Button>
      </div>
    </header>

    <div v-if="expanded && orderedArrangements.length" class="product-designer-vehicle-panel__list">
      <article v-for="(item, index) in orderedArrangements" :key="item.id" class="product-designer-vehicle-panel__row">
        <span class="product-designer-vehicle-panel__range">{{ serviceRange(item) }}</span>
        <div class="product-designer-vehicle-panel__main">
          <Tooltip :title="item.resourceName"><strong>{{ item.resourceName }}</strong></Tooltip>
          <span>{{ item.vehicleType }} · {{ item.quantity }} 辆</span>
        </div>
        <div class="product-designer-vehicle-panel__quote">
          <span>{{ item.supplierName || '待询价' }}</span>
          <strong>{{ formatMoney(item.costAmount) }}</strong>
        </div>
        <Tag v-if="item.procurementStatus === 'pending'" color="orange">待询价</Tag>
        <Tag v-else-if="item.procurementStatus === 'not_required'" color="green">无需采购</Tag>
        <div class="product-designer-vehicle-panel__actions">
          <Button :disabled="index === 0" size="small" type="link" @click="move(index, -1)"><IconifyIcon icon="lucide:chevron-up" /></Button>
          <Button :disabled="index === orderedArrangements.length - 1" size="small" type="link" @click="move(index, 1)"><IconifyIcon icon="lucide:chevron-down" /></Button>
          <Button size="small" type="link" @click="emit('edit', item)">编辑</Button>
          <Button danger size="small" type="link" @click="emit('remove', item)">删除</Button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.product-designer-vehicle-panel { margin-bottom: 12px; overflow: hidden; border: 1px solid #dbe4ee; border-radius: 6px; background: #fff; }
.product-designer-vehicle-panel__heading { display: flex; min-height: 58px; align-items: center; gap: 12px; padding: 9px 12px; }
.product-designer-vehicle-panel__icon { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 6px; background: #eaf3ff; color: #1677ff; font-size: 18px; }
.product-designer-vehicle-panel__summary { display: flex; min-width: 0; flex: 1; align-items: center; gap: 18px; }
.product-designer-vehicle-panel__title-line { flex: 0 0 auto; }
.product-designer-vehicle-panel__title-line strong, .product-designer-vehicle-panel__title-line span { display: block; }
.product-designer-vehicle-panel__title-line strong { color: #1f2937; font-size: 14px; font-weight: 650; }
.product-designer-vehicle-panel__title-line span { margin-top: 2px; color: #8c8c8c; font-size: 11px; }
.product-designer-vehicle-panel__summary-text { min-width: 0; overflow: hidden; color: #475569; font-size: 13px; line-height: 20px; text-overflow: ellipsis; white-space: nowrap; }
.product-designer-vehicle-panel__heading-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 8px; }
.product-designer-vehicle-panel__heading-actions :deep(.ant-tag) { margin-inline-end: 0; }
.product-designer-vehicle-panel__cost { color: #1677ff; font-weight: 600; font-variant-numeric: tabular-nums; white-space: nowrap; }
.product-designer-vehicle-panel__list { display: grid; border-top: 1px solid #edf2f7; background: #fafcff; }
.product-designer-vehicle-panel__row { display: flex; min-height: 50px; align-items: center; gap: 10px; padding: 7px 12px; border-bottom: 1px solid #edf2f7; }
.product-designer-vehicle-panel__row:last-child { border-bottom: 0; }
.product-designer-vehicle-panel__range { width: 68px; flex: 0 0 auto; color: #0958d9; font-size: 12px; font-weight: 600; }
.product-designer-vehicle-panel__main { min-width: 0; flex: 1; }
.product-designer-vehicle-panel__main strong, .product-designer-vehicle-panel__main span, .product-designer-vehicle-panel__quote span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-designer-vehicle-panel__main strong { color: #334155; font-size: 13px; }
.product-designer-vehicle-panel__main span, .product-designer-vehicle-panel__quote span { margin-top: 2px; color: #64748b; font-size: 12px; }
.product-designer-vehicle-panel__quote { width: 116px; flex: 0 0 auto; text-align: right; }
.product-designer-vehicle-panel__quote strong { color: #334155; font-size: 13px; font-variant-numeric: tabular-nums; }
.product-designer-vehicle-panel__actions { display: flex; flex: 0 0 auto; align-items: center; }
.product-designer-vehicle-panel__actions :deep(.ant-btn) { padding-inline: 4px; }
@media (max-width: 1040px) { .product-designer-vehicle-panel__summary { display: block; }.product-designer-vehicle-panel__summary-text { display: block; margin-top: 4px; } }
@media (max-width: 760px) { .product-designer-vehicle-panel__heading { align-items: flex-start; flex-wrap: wrap; }.product-designer-vehicle-panel__summary { min-width: calc(100% - 46px); }.product-designer-vehicle-panel__heading-actions { width: 100%; padding-left: 46px; flex-wrap: wrap; }.product-designer-vehicle-panel__row { flex-wrap: wrap; }.product-designer-vehicle-panel__range { width: 62px; }.product-designer-vehicle-panel__main { min-width: calc(100% - 72px); }.product-designer-vehicle-panel__quote { width: auto; margin-left: 72px; text-align: left; }.product-designer-vehicle-panel__actions { margin-left: auto; } }
</style>
