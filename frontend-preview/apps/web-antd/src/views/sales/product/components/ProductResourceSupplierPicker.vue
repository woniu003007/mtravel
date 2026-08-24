<script lang="ts" setup>
import { computed, ref } from 'vue';
import { Button, Empty, List, Popover, Tag, Tooltip } from 'ant-design-vue';

import type { SalesProductDesignerApi } from '#/api/sales/product-designer';

type SupplierCandidate = SalesProductDesignerApi.Supplier;
type SupplierRow = Pick<
  SalesProductDesignerApi.DayResource,
  'procurementMode' | 'supplierId' | 'supplierName'
> & {
  supplierRelationId?: number;
};

const props = withDefaults(defineProps<{
  disabled?: boolean;
  row: SupplierRow;
  suppliers: SupplierCandidate[];
}>(), {
  disabled: false,
});

const emit = defineEmits<{
  (event: 'request-suppliers'): void;
  (event: 'supplier-relation-change', relationId: number): void;
}>();

const open = ref(false);
const isNotRequired = computed(() => props.row.procurementMode === 'not_required');
const currentRelationId = computed(() => props.row.supplierRelationId);

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function supplierPriceText(supplier: SupplierCandidate) {
  return supplier.priceMode === 'pending' && supplier.referenceUnitPrice === 0
    ? '待询价'
    : formatMoney(supplier.referenceUnitPrice);
}

function selectSupplier(candidate: SupplierCandidate) {
  if (isNotRequired.value || props.disabled || candidate.relationId === currentRelationId.value) return;
  emit('supplier-relation-change', candidate.relationId);
  open.value = false;
}

function changeOpen(nextOpen: boolean) {
  if (nextOpen && !isNotRequired.value && !props.suppliers.length) emit('request-suppliers');
  open.value = nextOpen;
}
</script>

<template>
  <Popover :open="open" placement="bottomRight" trigger="click" @update:open="changeOpen">
    <template #content>
      <div class="product-resource-supplier-picker__content">
        <List v-if="suppliers.length" size="small" :data-source="suppliers">
          <template #renderItem="{ item }">
            <List.Item class="product-resource-supplier-picker__item">
                <Button
                  block
                  class="product-resource-supplier-picker__option"
                  :disabled="disabled || isNotRequired"
                  type="text"
                  @click="selectSupplier(item)"
                >
                  <span class="product-resource-supplier-picker__name">{{ item.supplierName }}</span>
                  <span class="product-resource-supplier-picker__price">{{ supplierPriceText(item) }}</span>
                  <Tag v-if="item.isDefault" color="blue">默认</Tag>
                  <Tag v-if="item.relationId === currentRelationId" color="green">当前</Tag>
                </Button>
              </List.Item>
          </template>
        </List>
        <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="暂无有效报价，请后续补充" />
      </div>
    </template>
    <Tooltip :title="isNotRequired ? '该资源无需采购，不需要供应商' : '更换供应商'">
      <Button :disabled="disabled || isNotRequired" size="small" type="link">供应商</Button>
    </Tooltip>
  </Popover>
</template>

<style scoped>
.product-resource-supplier-picker__content { width: 280px; max-height: 260px; overflow-y: auto; }
.product-resource-supplier-picker__item { padding: 0; }
.product-resource-supplier-picker__option { display: flex; align-items: center; min-height: 36px; padding: 6px 8px; text-align: left; }
.product-resource-supplier-picker__name { overflow: hidden; flex: 1; color: #334155; text-overflow: ellipsis; white-space: nowrap; }
.product-resource-supplier-picker__price { margin-left: 8px; color: #1677ff; font-variant-numeric: tabular-nums; white-space: nowrap; }
.product-resource-supplier-picker__option :deep(.ant-tag) { margin-inline-end: 0; margin-left: 6px; }
</style>
