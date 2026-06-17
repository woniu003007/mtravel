<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  type EnterpriseExpenseItemApi,
  getExpenseItemAll,
} from '#/api/enterprise/expense-item';
import {
  createPurchaseRelation,
  deletePurchaseRelation,
  getPurchaseRelationPage,
  type PurchaseRelationApi,
  updatePurchaseRelation,
} from '#/api/purchase/relation';
import {
  createSupplierResourcePrice,
  deleteSupplierResourcePrice,
  getSupplierResourcePricePage,
  type SupplierResourcePriceApi,
  updateSupplierResourcePrice,
} from '#/api/purchase/relation-price';
import {
  getPurchaseResourcePage,
  type PurchaseResourceApi,
} from '#/api/purchase/resource';
import {
  getSupplierAll,
  type SupplierApi,
} from '#/api/purchase/supplier';
import { purchaseRelationColumns } from './relation-columns';

type RelationRow = PurchaseRelationApi.Item;
type ResourceRow = PurchaseResourceApi.Item;
type PriceRow = SupplierResourcePriceApi.Item;

const router = useRouter();

const resourceTypeOptions = [
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '餐厅', value: 'restaurant' },
  { label: '购物', value: 'shopping' },
];

const statusOptions = [
  { label: '有效', value: 'active' },
  { label: '停用', value: 'disabled' },
  { label: '过期', value: 'expired' },
];

const priceStatusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const columns = purchaseRelationColumns;

const priceColumns = [
  { dataIndex: 'projectName', key: 'projectName', title: '项目类型', width: 130 },
  { dataIndex: 'marketPrice', key: 'marketPrice', title: '门市', width: 110 },
  { dataIndex: 'peerPrice', key: 'peerPrice', title: '同行', width: 110 },
  { dataIndex: 'teamPrice', key: 'teamPrice', title: '团队', width: 110 },
  { dataIndex: 'priceDescription', key: 'priceDescription', title: '价格说明', width: 180 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { key: 'action', title: '操作', width: 130 },
];

const data = ref<RelationRow[]>([]);
const resources = ref<ResourceRow[]>([]);
const supplierOptions = ref<{ label: string; value: number }[]>([]);
const resourceOptions = computed(() => resources.value.map((item) => ({
  label: `${item.resourceName}（${typeLabel(item.resourceType)}）`,
  value: item.id,
})));
const priceProjectOptions = ref<{ label: string; value: number }[]>([]);
const prices = ref<PriceRow[]>([]);
const currentRelation = ref<RelationRow>();
const loading = ref(false);
const modalOpen = ref(false);
const priceDrawerOpen = ref(false);
const priceLoading = ref(false);
const editingId = ref<number>();
const editingPriceId = ref<number>();

const query = reactive<PurchaseRelationApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<PurchaseRelationApi.SaveParams>({
  resourceId: undefined as unknown as number,
  status: 'active',
  supplierId: undefined as unknown as number,
});

const priceForm = reactive<SupplierResourcePriceApi.SaveParams>({
  marketPrice: 0,
  peerPrice: 0,
  relationId: undefined as unknown as number,
  resourceProjectId: undefined as unknown as number,
  status: 'active',
  teamPrice: 0,
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

function typeLabel(value?: string) {
  return resourceTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function clean(value?: string) {
  return value?.trim() || undefined;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getPurchaseRelationPage(query);
    data.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

async function loadResources() {
  // 后端单页上限为 200；采购关系新增弹窗只加载可用资源作为下拉选项。
  const result = await getPurchaseResourcePage({ page: 1, pageSize: 200, status: 'active' });
  resources.value = result.items;
}

async function loadSuppliersByResource(resourceId?: number) {
  const resource = resources.value.find((item) => item.id === resourceId);
  const category = resource?.resourceType as SupplierApi.Category | undefined;
  const result = await getSupplierAll(category);
  supplierOptions.value = result.map((item) => ({
    label: item.supplierName,
    value: item.id,
  }));
}

function handleSearch() {
  query.page = 1;
  loadData();
}

function resetQuery() {
  query.keyword = undefined;
  query.resourceType = undefined;
  query.status = undefined;
  query.supplierId = undefined;
  query.page = 1;
  loadData();
}

function resetForm() {
  editingId.value = undefined;
  formState.resourceId = undefined as unknown as number;
  formState.supplierId = undefined as unknown as number;
  formState.status = 'active';
  formState.remark = undefined;
  supplierOptions.value = [];
}

async function openCreateModal() {
  resetForm();
  await loadResources();
  modalOpen.value = true;
}

async function openEditModal(record: Record<string, any>) {
  const row = record as RelationRow;
  editingId.value = row.id;
  await loadResources();
  formState.resourceId = row.resourceId;
  formState.supplierId = row.supplierId;
  formState.status = row.status;
  formState.remark = row.remark;
  await loadSuppliersByResource(row.resourceId);
  modalOpen.value = true;
}

async function handleResourceChange(value: any) {
  formState.supplierId = undefined as unknown as number;
  await loadSuppliersByResource(Number(value));
}

function buildPayload(): PurchaseRelationApi.SaveParams {
  return {
    remark: clean(formState.remark),
    resourceId: formState.resourceId,
    status: formState.status || 'active',
    supplierId: formState.supplierId,
  };
}

async function saveRelation() {
  if (!formState.resourceId) {
    message.warning('请选择绑定资源');
    return;
  }
  if (!formState.supplierId) {
    message.warning('请选择供应商');
    return;
  }
  const payload = buildPayload();
  if (editingId.value) {
    await updatePurchaseRelation(editingId.value, payload);
    message.success('采购关系已更新');
  } else {
    await createPurchaseRelation(payload);
    message.success('采购关系已新增');
  }
  modalOpen.value = false;
  await loadData();
}

function confirmDelete(record: Record<string, any>) {
  const row = record as RelationRow;
  Modal.confirm({
    title: `删除采购关系「${row.resourceName} - ${row.supplierName || ''}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deletePurchaseRelation(row.id);
      message.success('采购关系已删除');
      await loadData();
    },
  });
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

async function openPriceDrawer(record: Record<string, any>) {
  const row = record as RelationRow;
  currentRelation.value = row;
  priceForm.relationId = row.id;
  resetPriceForm(false);
  priceDrawerOpen.value = true;
  await Promise.all([loadPriceProjects(row.resourceType), loadPrices()]);
}

async function loadPriceProjects(resourceType: string) {
  const result = await getExpenseItemAll(resourceType as EnterpriseExpenseItemApi.ResourceType);
  priceProjectOptions.value = result.map((item) => ({
    label: item.projectName,
    value: item.id,
  }));
}

async function loadPrices() {
  if (!currentRelation.value) return;
  priceLoading.value = true;
  try {
    const result = await getSupplierResourcePricePage({
      page: 1,
      pageSize: 100,
      relationId: currentRelation.value.id,
    });
    prices.value = result.items;
  } finally {
    priceLoading.value = false;
  }
}

function resetPriceForm(clearRelation = true) {
  editingPriceId.value = undefined;
  if (clearRelation) {
    priceForm.relationId = undefined as unknown as number;
  }
  priceForm.resourceProjectId = undefined as unknown as number;
  priceForm.marketPrice = 0;
  priceForm.peerPrice = 0;
  priceForm.teamPrice = 0;
  priceForm.priceDescription = undefined;
  priceForm.status = 'active';
  priceForm.remark = undefined;
}

function editPrice(record: Record<string, any>) {
  const row = record as PriceRow;
  editingPriceId.value = row.id;
  priceForm.relationId = row.relationId;
  priceForm.resourceProjectId = row.resourceProjectId;
  priceForm.marketPrice = row.marketPrice;
  priceForm.peerPrice = row.peerPrice;
  priceForm.teamPrice = row.teamPrice;
  priceForm.priceDescription = row.priceDescription;
  priceForm.status = row.status;
  priceForm.remark = row.remark;
}

async function savePrice() {
  if (!currentRelation.value) return;
  if (!priceForm.resourceProjectId) {
    message.warning('请选择项目类型');
    return;
  }
  const payload: SupplierResourcePriceApi.SaveParams = {
    marketPrice: priceForm.marketPrice || 0,
    peerPrice: priceForm.peerPrice || 0,
    priceDescription: clean(priceForm.priceDescription),
    relationId: currentRelation.value.id,
    remark: clean(priceForm.remark),
    resourceProjectId: priceForm.resourceProjectId,
    status: priceForm.status || 'active',
    teamPrice: priceForm.teamPrice || 0,
  };
  if (editingPriceId.value) {
    await updateSupplierResourcePrice(editingPriceId.value, payload);
    message.success('价格已更新');
  } else {
    await createSupplierResourcePrice(payload);
    message.success('价格已新增');
  }
  resetPriceForm(false);
  priceForm.relationId = currentRelation.value.id;
  await loadPrices();
}

function confirmDeletePrice(record: Record<string, any>) {
  const row = record as PriceRow;
  Modal.confirm({
    title: `删除价格「${row.projectName}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteSupplierResourcePrice(row.id);
      message.success('价格已删除');
      await loadPrices();
    },
  });
}

function priceMoney(record: Record<string, any>, key: unknown) {
  return formatMoney(record[String(key)] as number);
}

function goContract(record: Record<string, any>) {
  const row = record as RelationRow;
  router.push({
    path: '/purchase/contract',
    query: {
      category: row.resourceType,
      supplierId: row.supplierId,
    },
  });
}

onMounted(loadData);
</script>

<template>
  <Page title="采购关系管理" description="维护供应商与资源的绑定关系；具体价格从行内价格管理进入。">
    <Card>
      <BusinessSearchForm
        label-width="78px"
        :model="query"
        :search-loading="loading"
        create-text="新增"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="资源名称">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="请输入资源名称"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="资源类型">
          <Select
            v-model:value="query.resourceType"
            allow-clear
            :options="resourceTypeOptions"
            placeholder="请选择资源类型"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select
            v-model:value="query.status"
            allow-clear
            :options="statusOptions"
            placeholder="请选择状态"
          />
        </Form.Item>
      </BusinessSearchForm>

      <Table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space size="small" wrap>
              <Button type="link" size="small" @click="openPriceDrawer(record)">价格管理</Button>
              <Button type="link" size="small" @click="goContract(record)">合同管理</Button>
              <Button type="link" size="small" @click="openEditModal(record)">修改</Button>
              <Button danger type="link" size="small" @click="confirmDelete(record)">删除</Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalOpen"
      :title="editingId ? '修改采购关系' : '新增采购关系'"
      width="680px"
      ok-text="保存"
      cancel-text="取消"
      @ok="saveRelation"
    >
      <Form :model="formState" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="绑定资源" required>
            <Select
              v-model:value="formState.resourceId"
              show-search
              :filter-option="true"
              :options="resourceOptions"
              placeholder="请选择资源"
              @change="handleResourceChange"
            />
          </Form.Item>
          <Form.Item label="供应商" required>
            <Select
              v-model:value="formState.supplierId"
              show-search
              :filter-option="true"
              :options="supplierOptions"
              placeholder="先选资源，再选供应商"
            />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="formState.status" :options="statusOptions" />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Textarea v-model:value="formState.remark" :auto-size="{ minRows: 3, maxRows: 5 }" />
        </Form.Item>
      </Form>
    </Modal>

    <Drawer
      v-model:open="priceDrawerOpen"
      width="920"
      :title="`价格管理 - ${currentRelation?.resourceName || ''} / ${currentRelation?.supplierName || ''}`"
    >
      <Card size="small" class="mb-4">
        <Form :model="priceForm" layout="vertical">
          <div class="price-grid">
            <Form.Item label="项目类型" required>
              <Select
                v-model:value="priceForm.resourceProjectId"
                show-search
                :filter-option="true"
                :options="priceProjectOptions"
                placeholder="按资源类型自动过滤"
              />
            </Form.Item>
            <Form.Item label="门市">
              <InputNumber v-model:value="priceForm.marketPrice" class="w-full" :min="0" :precision="2" />
            </Form.Item>
            <Form.Item label="同行">
              <InputNumber v-model:value="priceForm.peerPrice" class="w-full" :min="0" :precision="2" />
            </Form.Item>
            <Form.Item label="团队">
              <InputNumber v-model:value="priceForm.teamPrice" class="w-full" :min="0" :precision="2" />
            </Form.Item>
            <Form.Item label="状态">
              <Select v-model:value="priceForm.status" :options="priceStatusOptions" />
            </Form.Item>
          </div>
          <Form.Item label="价格说明">
            <Textarea v-model:value="priceForm.priceDescription" :auto-size="{ minRows: 2, maxRows: 4 }" />
          </Form.Item>
          <Space>
            <Button type="primary" @click="savePrice">
              {{ editingPriceId ? '保存价格' : '新增价格' }}
            </Button>
            <Button @click="resetPriceForm(false)">清空</Button>
          </Space>
        </Form>
      </Card>

      <Table
        :columns="priceColumns"
        :data-source="prices"
        :loading="priceLoading"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ x: 860 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="['marketPrice', 'peerPrice', 'teamPrice'].includes(String(column.key))">
            {{ priceMoney(record, column.key) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ record.status === 'active' ? '启用' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="editPrice(record)">修改</Button>
              <Button danger type="link" size="small" @click="confirmDeletePrice(record)">删除</Button>
            </Space>
          </template>
        </template>
      </Table>
    </Drawer>
  </Page>
</template>

<style scoped>
.modal-grid,
.price-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.price-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 900px) {
  .modal-grid,
  .price-grid {
    grid-template-columns: 1fr;
  }
}
</style>
