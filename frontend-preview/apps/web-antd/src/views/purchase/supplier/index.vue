<script lang="ts" setup>
import { Button, Modal, Select, message } from 'ant-design-vue';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getPurchaseRelationPage } from '#/api/purchase/relation';
import type { PurchaseRelationApi } from '#/api/purchase/relation';
import CrudPage from '#/views/_business/crud/CrudPage.vue';
import { supplierConfig } from '#/views/_business/crud/module-config';

const router = useRouter();
const relationModalOpen = ref(false);
const priceLoadingSupplierId = ref<number>();
const relationOptions = ref<PurchaseRelationApi.Item[]>([]);
const selectedRelationId = ref<number>();

const relationSelectOptions = computed(() =>
  relationOptions.value.map((relation) => ({
    label: `${relation.resourceName} / ${resourceTypeLabel(relation.resourceType)}`,
    value: relation.id,
  })),
);

function resourceTypeLabel(type: PurchaseRelationApi.ResourceType) {
  const labels: Record<string, string> = {
    ground_agent: '地接',
    guide: '导游',
    hotel: '酒店',
    other: '其它资源',
    restaurant: '餐厅',
    scenic: '景区',
    shopping: '购物',
    ticket: '票务',
    traffic: '大交通',
    vehicle: '用车',
  };
  return labels[type] || type;
}

function openPriceEditor(relation: PurchaseRelationApi.Item) {
  relationModalOpen.value = false;
  return router.push({
    path: '/purchase/resource',
    query: {
      editPrice: '1',
      relationId: String(relation.id),
      resourceId: String(relation.resourceId),
    },
  });
}

async function editSupplierPrice(record: Record<string, any>) {
  priceLoadingSupplierId.value = Number(record.id);
  try {
    const result = await getPurchaseRelationPage({
      page: 1,
      pageSize: 200,
      supplierId: Number(record.id),
    });
    const relations = result.items.filter((item) => Boolean(item.resourceId));
    if (relations.length === 0) {
      message.warning('该供应商尚未绑定资源，请先到资源总览绑定');
      return;
    }
    if (relations.length === 1) {
      await openPriceEditor(relations[0]!);
      return;
    }
    relationOptions.value = relations;
    // 多资源供应商不能默认选中第一条，避免用户误编辑错误的资源报价。
    selectedRelationId.value = undefined;
    relationModalOpen.value = true;
  } finally {
    priceLoadingSupplierId.value = undefined;
  }
}

async function confirmRelationSelection() {
  const relation = relationOptions.value.find(
    (item) => item.id === selectedRelationId.value,
  );
  if (!relation) {
    message.warning('请选择要编辑报价的资源');
    return;
  }
  await openPriceEditor(relation);
}
</script>

<template>
  <div class="supplier-page">
    <CrudPage :config="supplierConfig">
      <template #row-actions="{ record }">
        <Button
          type="link"
          size="small"
          :loading="priceLoadingSupplierId === Number(record.id)"
          @click="editSupplierPrice(record)"
        >
          编辑报价
        </Button>
      </template>
    </CrudPage>

    <Modal
      v-model:open="relationModalOpen"
      title="该供应商绑定了多个资源，请选择要编辑报价的资源"
      width="460px"
      ok-text="继续"
      cancel-text="取消"
      @ok="confirmRelationSelection"
    >
      <Select
        v-model:value="selectedRelationId"
        class="relation-select"
        :options="relationSelectOptions"
        placeholder="请选择资源"
      />
    </Modal>
  </div>
</template>

<style scoped>
.relation-select {
  width: 100%;
}
</style>
