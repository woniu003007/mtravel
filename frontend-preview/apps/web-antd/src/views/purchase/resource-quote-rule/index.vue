<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';
import {
  Button,
  Card,
  Form,
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

import {
  getCustomerCategoryPage,
  type CustomerCategoryApi,
} from '#/api/customer/category';
import {
  createPurchaseResourceQuoteRule,
  deletePurchaseResourceQuoteRule,
  getPurchaseResourceQuoteRulePage,
  type PurchaseResourceQuoteRuleApi,
  updatePurchaseResourceQuoteRule,
} from '#/api/purchase/resource-quote-rule';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  buildResourceQuoteRulePayload,
  type ResourceQuoteRuleFormState,
} from './form-payload';

type ResourceQuoteRuleRow = PurchaseResourceQuoteRuleApi.Item;

const resourceTypeOptions: Array<{
  label: string;
  value: PurchaseResourceQuoteRuleApi.ResourceType;
}> = [
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '用车', value: 'vehicle' },
  { label: '餐厅', value: 'restaurant' },
  { label: '导游', value: 'guide' },
  { label: '地接', value: 'ground_agent' },
  { label: '票务', value: 'ticket' },
  { label: '购物', value: 'shopping' },
  { label: '其他', value: 'other' },
];

const statusOptions: Array<{
  label: string;
  value: PurchaseResourceQuoteRuleApi.Status;
}> = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const columns: TableColumnsType<ResourceQuoteRuleRow> = [
  { dataIndex: 'resourceType', key: 'resourceType', title: '资源类型', width: 100 },
  { key: 'customerLevel', title: '客户等级', width: 150, ellipsis: true },
  { dataIndex: 'status', key: 'status', title: '状态', width: 88, align: 'center' },
  { dataIndex: 'suggestedRate', key: 'suggestedRate', title: '建议比例上浮', width: 130, align: 'right' },
  { dataIndex: 'minimumRate', key: 'minimumRate', title: '最低比例上浮', width: 130, align: 'right' },
  { dataIndex: 'suggestedFixedAddon', key: 'suggestedFixedAddon', title: '建议固定加价', width: 130, align: 'right' },
  { dataIndex: 'minimumFixedAddon', key: 'minimumFixedAddon', title: '最低固定加价', width: 130, align: 'right' },
  { dataIndex: 'remark', key: 'remark', title: '备注', width: 220, ellipsis: true },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 140 },
];

const data = ref<ResourceQuoteRuleRow[]>([]);
const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();
const customerLevelLoading = ref(false);
const customerLevels = ref<CustomerCategoryApi.CustomerCategory[]>([]);

const query = reactive<PurchaseResourceQuoteRuleApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<ResourceQuoteRuleFormState>({
  customerLevelId: null,
  minimumFixedAddon: 0,
  minimumRate: 0,
  remark: '',
  resourceType: 'scenic',
  status: 'active',
  suggestedFixedAddon: 0,
  suggestedRate: 0,
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const customerLevelOptions = computed(() =>
  customerLevels.value.map((item) => ({
    label: item.categoryName,
    value: item.id,
  })),
);

/** Ant Select 用 undefined 表示清空，表单状态仍用 null 表示默认规则。 */
const formCustomerLevelId = computed<number | undefined>({
  get: () => formState.customerLevelId ?? undefined,
  set: (value) => {
    formState.customerLevelId = value ?? null;
  },
});

/** 按需加载启用客户等级，供查询条件和弹窗共用。 */
async function loadCustomerLevels() {
  if (customerLevels.value.length || customerLevelLoading.value) return;
  customerLevelLoading.value = true;
  try {
    const result = await getCustomerCategoryPage({
      page: 1,
      pageSize: 200,
      status: 'active',
    });
    customerLevels.value = result.items;
  } finally {
    customerLevelLoading.value = false;
  }
}

/** 分页查询普通资源报价规则。 */
async function loadData() {
  loading.value = true;
  try {
    const result = await getPurchaseResourceQuoteRulePage(query);
    data.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  void loadData();
}

function resetQuery() {
  query.customerLevelId = undefined;
  query.resourceType = undefined;
  query.status = undefined;
  query.page = 1;
  void loadData();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  void loadData();
}

function resetForm() {
  editingId.value = undefined;
  Object.assign(formState, {
    customerLevelId: null,
    minimumFixedAddon: 0,
    minimumRate: 0,
    remark: '',
    resourceType: 'scenic',
    status: 'active',
    suggestedFixedAddon: 0,
    suggestedRate: 0,
  });
}

function openCreateModal() {
  resetForm();
  modalOpen.value = true;
  void loadCustomerLevels();
}

function openEditModal(record: ResourceQuoteRuleRow) {
  editingId.value = record.id;
  Object.assign(formState, {
    customerLevelId: record.customerLevelId ?? null,
    minimumFixedAddon: Number(record.minimumFixedAddon || 0),
    minimumRate: Number(record.minimumRate || 0),
    remark: record.remark || '',
    resourceType: record.resourceType,
    status: record.status,
    suggestedFixedAddon: Number(record.suggestedFixedAddon || 0),
    suggestedRate: Number(record.suggestedRate || 0),
  });
  modalOpen.value = true;
  void loadCustomerLevels();
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as ResourceQuoteRuleRow);
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as ResourceQuoteRuleRow);
}

function resourceTypeLabel(value?: string) {
  return resourceTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

function formatPercent(value?: number) {
  return value === null || value === undefined ? '-' : `${Number(value)}%`;
}

function formatMoney(value?: number) {
  return value === null || value === undefined ? '-' : `¥${Number(value).toFixed(2)}`;
}

/** 保存时统一通过 payload 构建函数，确保清空等级仍传递 customerLevelId: null。 */
async function saveRecord() {
  saving.value = true;
  try {
    const payload = buildResourceQuoteRulePayload(formState);
    if (editingId.value) {
      await updatePurchaseResourceQuoteRule(editingId.value, payload);
      message.success('普通资源报价规则已更新');
    } else {
      await createPurchaseResourceQuoteRule(payload);
      message.success('普通资源报价规则已新增');
    }
    modalOpen.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

/** 确认后软删除一条普通资源报价规则。 */
function confirmDelete(record: ResourceQuoteRuleRow) {
  Modal.confirm({
    cancelText: '取消',
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    title: `删除普通资源报价规则「${resourceTypeLabel(record.resourceType)}」？`,
    async onOk() {
      await deletePurchaseResourceQuoteRule(record.id);
      message.success('普通资源报价规则已删除');
      await loadData();
    },
  });
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <Page title="普通资源报价规则">
    <Card>
      <BusinessSearchForm
        label-width="88px"
        :model="query"
        :search-loading="loading"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="资源类型">
          <Select
            v-model:value="query.resourceType"
            allow-clear
            :options="resourceTypeOptions"
            placeholder="请选择资源类型"
          />
        </Form.Item>
        <Form.Item label="客户等级">
          <Select
            v-model:value="query.customerLevelId"
            allow-clear
            :loading="customerLevelLoading"
            :options="customerLevelOptions"
            placeholder="请选择客户等级"
            show-search
            option-filter-prop="label"
            @focus="loadCustomerLevels"
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
        :scroll="{ x: 1240 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'resourceType'">
            {{ resourceTypeLabel(record.resourceType) }}
          </template>
          <template v-else-if="column.key === 'customerLevel'">
            {{ record.customerLevelId === null ? '默认规则' : record.customerLevelName || '-' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ record.status === 'active' ? '启用' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'suggestedRate'">
            {{ formatPercent(record.suggestedRate) }}
          </template>
          <template v-else-if="column.key === 'minimumRate'">
            {{ formatPercent(record.minimumRate) }}
          </template>
          <template v-else-if="column.key === 'suggestedFixedAddon'">
            {{ formatMoney(record.suggestedFixedAddon) }}
          </template>
          <template v-else-if="column.key === 'minimumFixedAddon'">
            {{ formatMoney(record.minimumFixedAddon) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handleEdit(record)">
                编辑
              </Button>
              <Button type="link" size="small" danger @click="handleDelete(record)">
                删除
              </Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="editingId ? '编辑普通资源报价规则' : '新增普通资源报价规则'"
      :width="460"
      cancel-text="取消"
      ok-text="保存"
      @ok="saveRecord"
    >
      <Form :model="formState" layout="vertical">
        <Form.Item label="资源类型" required>
          <Select v-model:value="formState.resourceType" :options="resourceTypeOptions" />
        </Form.Item>
        <Form.Item label="客户等级">
          <Select
            v-model:value="formCustomerLevelId"
            allow-clear
            :loading="customerLevelLoading"
            :options="customerLevelOptions"
            placeholder="不选择时作为默认规则"
            show-search
            option-filter-prop="label"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select v-model:value="formState.status" :options="statusOptions" />
        </Form.Item>
        <Form.Item label="建议比例上浮">
          <InputNumber
            v-model:value="formState.suggestedRate"
            addon-after="%"
            class="w-full"
            :min="0"
            :precision="2"
          />
        </Form.Item>
        <Form.Item label="最低比例上浮">
          <InputNumber
            v-model:value="formState.minimumRate"
            addon-after="%"
            class="w-full"
            :min="0"
            :precision="2"
          />
        </Form.Item>
        <Form.Item label="建议固定加价">
          <InputNumber
            v-model:value="formState.suggestedFixedAddon"
            addon-before="¥"
            class="w-full"
            :min="0"
            :precision="2"
          />
        </Form.Item>
        <Form.Item label="最低固定加价">
          <InputNumber
            v-model:value="formState.minimumFixedAddon"
            addon-before="¥"
            class="w-full"
            :min="0"
            :precision="2"
          />
        </Form.Item>
        <Form.Item label="备注">
          <Textarea v-model:value="formState.remark" :maxlength="500" :rows="3" />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>
