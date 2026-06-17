<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
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

import BusinessPillTabs from '#/components/business/BusinessPillTabs.vue';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  type EnterpriseProductDictionaryApi,
  createProductDictionary,
  deleteProductDictionary,
  getProductDictionaryPage,
  updateProductDictionary,
} from '#/api/enterprise/product-dictionary';

import {
  productDictionaryTabs,
  productDictionaryTypeDescription,
  productDictionaryTypeLabel,
  productDictionaryTypeOptions,
} from './product-dictionary-tabs';

type ProductDictionaryRow = EnterpriseProductDictionaryApi.Item;
type DictType = EnterpriseProductDictionaryApi.DictType;

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const columns = [
  { dataIndex: 'dictType', key: 'dictType', title: '字典类型', width: 130 },
  { dataIndex: 'dictName', key: 'dictName', title: '字典名称', width: 180 },
  { dataIndex: 'sortOrder', key: 'sortOrder', title: '排序', width: 90 },
  { dataIndex: 'remark', key: 'remark', title: '备注', width: 260 },
  { dataIndex: 'createdBy', key: 'createdBy', title: '创建人', width: 110 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间', width: 170 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 150 },
];

const activeTab = ref<DictType>('business_type');
const data = ref<ProductDictionaryRow[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();

const query = reactive<EnterpriseProductDictionaryApi.QueryParams>({
  dictType: activeTab.value,
  page: 1,
  pageSize: 10,
});

const formState = reactive<EnterpriseProductDictionaryApi.SaveParams>({
  dictName: '',
  dictType: activeTab.value,
  sortOrder: 0,
  status: 'active',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const pageDescription = computed(() => (
  '维护产品模板可选择的业务类型、接待标准和产品主题，后续产品管理只从这里选择。'
));

const activeDescription = computed(() => productDictionaryTypeDescription(activeTab.value));

function dictTypeLabel(value?: string) {
  return productDictionaryTypeLabel(value);
}

function dictTypeColor(value?: string) {
  const colors: Record<string, string> = {
    business_type: 'blue',
    product_theme: 'purple',
    reception_standard: 'green',
  };
  return colors[value || ''] || 'default';
}

function statusLabel(value?: string) {
  return value === 'disabled' ? '停用' : '启用';
}

function statusColor(value?: string) {
  return value === 'disabled' ? 'default' : 'green';
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function clean(value?: string) {
  return value?.trim() || undefined;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getProductDictionaryPage({
      ...query,
      dictType: activeTab.value,
    });
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
  loadData();
}

function resetQuery() {
  query.keyword = undefined;
  query.status = undefined;
  query.page = 1;
  loadData();
}

function selectProductDictionaryTab(tabKey: DictType) {
  if (activeTab.value === tabKey) return;
  activeTab.value = tabKey;
  query.dictType = tabKey;
  query.page = 1;
  loadData();
}

function resetForm() {
  editingId.value = undefined;
  formState.dictType = activeTab.value;
  formState.dictName = '';
  formState.sortOrder = 0;
  formState.status = 'active';
  formState.remark = undefined;
}

function openCreateModal() {
  resetForm();
  modalOpen.value = true;
}

function openEditModal(record: Record<string, any>) {
  const row = record as ProductDictionaryRow;
  editingId.value = row.id;
  formState.dictType = row.dictType;
  formState.dictName = row.dictName;
  formState.sortOrder = row.sortOrder;
  formState.status = row.status;
  formState.remark = row.remark;
  modalOpen.value = true;
}

function buildPayload(): EnterpriseProductDictionaryApi.SaveParams {
  return {
    dictName: formState.dictName.trim(),
    dictType: formState.dictType,
    remark: clean(formState.remark),
    sortOrder: formState.sortOrder || 0,
    status: formState.status || 'active',
  };
}

async function saveRecord() {
  if (!formState.dictName?.trim()) {
    message.warning('请填写字典名称');
    return;
  }
  const payload = buildPayload();
  if (editingId.value) {
    await updateProductDictionary(editingId.value, payload);
    message.success('产品字典已更新');
  } else {
    await createProductDictionary(payload);
    message.success('产品字典已新增');
  }
  modalOpen.value = false;
  activeTab.value = payload.dictType;
  query.dictType = payload.dictType;
  await loadData();
}

function confirmDelete(record: Record<string, any>) {
  const row = record as ProductDictionaryRow;
  Modal.confirm({
    title: `删除产品字典「${row.dictName}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteProductDictionary(row.id);
      message.success('产品字典已删除');
      await loadData();
    },
  });
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

onMounted(loadData);
</script>

<template>
  <Page title="产品字典" :description="pageDescription">
    <Card>
      <BusinessPillTabs
        :active-key="activeTab"
        aria-label="产品字典分类"
        :tabs="productDictionaryTabs"
        @change="(key) => selectProductDictionaryTab(key as DictType)"
      />

      <div class="dictionary-hint">
        {{ activeDescription }}
      </div>

      <BusinessSearchForm
        label-width="78px"
        :model="query"
        :search-loading="loading"
        create-text="新增"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="字典名称">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="请输入字典名称"
            @press-enter="handleSearch"
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
        :scroll="{ x: 1150 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dictType'">
            <Tag :color="dictTypeColor(record.dictType)">
              {{ dictTypeLabel(record.dictType) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusColor(record.status)">
              {{ statusLabel(record.status) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="openEditModal(record)">修改</Button>
              <Button danger type="link" size="small" @click="confirmDelete(record)">删除</Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑产品字典' : '新增产品字典'"
      width="640px"
      ok-text="保存"
      cancel-text="取消"
      @ok="saveRecord"
    >
      <Form :model="formState" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="字典类型" required>
            <Select v-model:value="formState.dictType" :options="productDictionaryTypeOptions" />
          </Form.Item>
          <Form.Item label="字典名称" required>
            <Input v-model:value="formState.dictName" allow-clear />
          </Form.Item>
          <Form.Item label="排序">
            <InputNumber v-model:value="formState.sortOrder" class="w-full" :min="0" />
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
  </Page>
</template>

<style scoped>
.dictionary-hint {
  padding: 0 2px 14px;
  font-size: 13px;
  color: #64748b;
}

.modal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (max-width: 768px) {
  .modal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
