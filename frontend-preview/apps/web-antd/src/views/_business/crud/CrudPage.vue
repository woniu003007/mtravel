<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';
import {
  Button,
  Card,
  Cascader,
  DatePicker,
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

import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  buildRegionOptions,
  buildRegionPath,
  splitRegionPath,
  type RegionPath,
} from '#/utils/region';
import { useRoute, useRouter } from 'vue-router';

export interface CrudFieldOption {
  label: string;
  value: number | string;
}

export interface CrudRegionKeys {
  city?: string;
  district?: string;
  province?: string;
}

export interface CrudField {
  key: string;
  label: string;
  required?: boolean;
  options?: CrudFieldOption[];
  placeholder?: string;
  regionKeys?: CrudRegionKeys;
  type?: 'date' | 'money' | 'number' | 'region' | 'select' | 'textarea' | 'text';
}

export interface CrudColumn {
  key: string;
  title: string;
  format?: 'area' | 'dateRange' | 'money' | 'status' | 'text';
  regionKeys?: CrudRegionKeys;
  secondaryKey?: string;
  width?: number;
}

export interface CrudPageConfig<T extends Record<string, any>> {
  actionWidth?: number;
  columns: CrudColumn[];
  create: (data: Record<string, any>) => Promise<T>;
  delete: (id: number) => Promise<void>;
  description: string;
  fields: CrudField[];
  loadOptions?: () => Promise<void>;
  pageApi: (params: Record<string, any>) => Promise<{ items: T[]; total: number }>;
  queryFields: CrudField[];
  title: string;
  update: (id: number, data: Record<string, any>) => Promise<T>;
}

const props = defineProps<{
  config: CrudPageConfig<Record<string, any>>;
}>();

const route = useRoute();
const router = useRouter();
const data = ref<Record<string, any>[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();

const query = reactive<Record<string, any>>({ page: 1, pageSize: 10 });
const formState = reactive<Record<string, any>>({});
const regionOptions = buildRegionOptions();

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const tableColumns = computed(() => [
  ...props.config.columns.map((column) => ({
    dataIndex: column.key,
    key: column.key,
    title: column.title,
    width: column.width,
  })),
  {
    fixed: 'right' as const,
    key: 'action',
    title: '操作',
    width: props.config.actionWidth || 150,
  },
]);

async function loadData() {
  loading.value = true;
  try {
    const result = await props.config.pageApi(buildQueryParams());
    data.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

async function init() {
  if (props.config.loadOptions) {
    await props.config.loadOptions();
  }
  resetForm();
  applyRoutePreset();
  await loadData();
  if (route.query.create === '1') {
    openCreateModal();
    clearCreateRouteFlag();
  }
}

function applyRoutePreset() {
  for (const field of [...props.config.queryFields, ...props.config.fields]) {
    const routeValue = route.query[field.key];
    const value = Array.isArray(routeValue) ? routeValue[0] : routeValue;
    if (!value) {
      continue;
    }
    if (props.config.queryFields.some((queryField) => queryField.key === field.key)) {
      query[field.key] = value;
    }
    if (props.config.fields.some((formField) => formField.key === field.key)) {
      formState[field.key] = value;
    }
  }
}

function clearCreateRouteFlag() {
  const nextQuery = { ...route.query };
  delete nextQuery.create;
  router.replace({ path: route.path, query: nextQuery });
}

function resetQuery() {
  for (const key of Object.keys(query)) {
    if (!['page', 'pageSize'].includes(key)) {
      query[key] = undefined;
    }
  }
  query.page = 1;
  loadData();
}

function handleSearch() {
  query.page = 1;
  loadData();
}

function resetForm() {
  for (const key of Object.keys(formState)) delete formState[key];
  for (const field of props.config.fields) {
    formState[field.key] = defaultValue(field);
  }
}

function defaultValue(field: CrudField) {
  if (field.type === 'money' || field.type === 'number') return 0;
  if (field.type === 'region') return [];
  if (field.type === 'select') return field.required ? field.options?.[0]?.value : undefined;
  return undefined;
}

function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  applyRoutePreset();
  modalOpen.value = true;
}

function openEditModal(record: Record<string, any>) {
  editingId.value = record.id;
  resetForm();
  for (const field of props.config.fields) {
    if (field.type === 'region') {
      const keys = regionKeys(field);
      formState[field.key] = buildRegionPath(
        record[keys.province],
        record[keys.city],
        record[keys.district],
      );
    } else {
      formState[field.key] = record[field.key] ?? defaultValue(field);
    }
  }
  modalOpen.value = true;
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

async function saveRecord() {
  for (const field of props.config.fields) {
    if (field.required && !hasValue(formState[field.key])) {
      message.warning(`请填写${field.label}`);
      return;
    }
  }
  const payload = buildPayload();
  if (editingId.value) {
    await props.config.update(editingId.value, payload);
    message.success('记录已更新');
  } else {
    await props.config.create(payload);
    message.success('记录已新增');
  }
  modalOpen.value = false;
  loadData();
}

function buildPayload() {
  const payload: Record<string, any> = {};
  for (const field of props.config.fields) {
    const value = formState[field.key];
    if (field.type === 'region') {
      const keys = regionKeys(field);
      const regionFields = splitRegionPath(value as RegionPath);
      payload[keys.province] = regionFields.province;
      payload[keys.city] = regionFields.city;
      payload[keys.district] = regionFields.district;
    } else {
      payload[field.key] = typeof value === 'string' ? value.trim() || undefined : value;
    }
  }
  return payload;
}

function buildQueryParams() {
  const payload: Record<string, any> = {};
  for (const [key, value] of Object.entries(query)) {
    payload[key] = value;
  }
  for (const field of props.config.queryFields) {
    if (field.type !== 'region') {
      continue;
    }
    const keys = regionKeys(field);
    const regionFields = splitRegionPath(query[field.key] as RegionPath);
    delete payload[field.key];
    payload[keys.province] = regionFields.province;
    payload[keys.city] = regionFields.city;
    payload[keys.district] = regionFields.district;
  }
  return payload;
}

function hasValue(value: any) {
  if (Array.isArray(value)) {
    return value.length > 0;
  }
  return value !== undefined && value !== null && String(value).trim() !== '';
}

function regionKeys(field: CrudField | CrudColumn) {
  return {
    city: field.regionKeys?.city || 'city',
    district: field.regionKeys?.district || 'district',
    province: field.regionKeys?.province || 'province',
  };
}

function areaText(record: Record<string, any>, column: CrudColumn) {
  const keys = regionKeys(column);
  return [record[keys.province], record[keys.city], record[keys.district]]
    .filter(Boolean)
    .join(' / ') || '-';
}

function confirmDelete(record: Record<string, any>) {
  Modal.confirm({
    title: '确认删除这条记录？',
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await props.config.delete(record.id);
      message.success('记录已删除');
      loadData();
    },
  });
}

function formatMoney(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    currency: 'CNY',
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
    style: 'currency',
  }).format(Number(value || 0));
}

function statusLabel(value?: string) {
  const labels: Record<string, string> = {
    active: '启用',
    approval: '转审批',
    blacklisted: '黑名单',
    completed: '已完成',
    disabled: '停用',
    expired: '已到期',
    common: '通用',
    ground_agent: '地接',
    guide: '导游',
    hotel: '酒店',
    none: '不处理',
    other: '其它',
    restaurant: '餐厅',
    remind: '提醒',
    scenic: '景区',
    shopping: '购物',
    suspended: '暂停',
    terminated: '终止',
    ticket: '票务',
    traffic: '大交通',
    vehicle: '车队',
  };
  return labels[value || ''] || value || '-';
}

function statusColor(value?: string) {
  const colors: Record<string, string> = {
    active: 'green',
    approval: 'blue',
    blacklisted: 'red',
    completed: 'green',
    disabled: 'default',
    expired: 'orange',
    none: 'default',
    remind: 'blue',
    suspended: 'orange',
    terminated: 'red',
  };
  return colors[value || ''] || 'default';
}

onMounted(init);
</script>

<template>
  <Page :title="config.title" :description="config.description">
    <Card>
      <BusinessSearchForm
        :model="query"
        :search-loading="loading"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item
          v-for="field in config.queryFields"
          :key="field.key"
          :label="field.label"
        >
          <Input
            v-if="!field.type || field.type === 'text'"
            v-model:value="query[field.key]"
            allow-clear
            :placeholder="field.placeholder || `请输入${field.label}`"
            @press-enter="handleSearch"
          />
          <Select
            v-else-if="field.type === 'select'"
            v-model:value="query[field.key]"
            allow-clear
            :options="field.options"
            :placeholder="field.placeholder || `请选择${field.label}`"
          />
          <Cascader
            v-else-if="field.type === 'region'"
            v-model:value="query[field.key]"
            allow-clear
            change-on-select
            :options="regionOptions"
            :placeholder="field.placeholder || '可选择省 / 市 / 区县'"
            show-search
          />
        </Form.Item>
      </BusinessSearchForm>

      <Table
        :columns="tableColumns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1280 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <Space>
              <slot name="row-actions" :record="record" />
              <Button type="link" size="small" @click="openEditModal(record)">
                编辑
              </Button>
              <Button type="link" size="small" danger @click="confirmDelete(record)">
                删除
              </Button>
            </Space>
          </template>
          <template v-else>
            <template v-for="col in config.columns" :key="col.key">
              <template v-if="column.key === col.key">
                <Tag v-if="col.format === 'status'" :color="statusColor(record[col.key])">
                  {{ statusLabel(record[col.key]) }}
                </Tag>
                <span v-else-if="col.format === 'money'">
                  {{ formatMoney(record[col.key]) }}
                </span>
                <span v-else-if="col.format === 'dateRange'">
                  {{ record[col.key] || '-' }} ~ {{ record[col.secondaryKey || ''] || '-' }}
                </span>
                <span v-else-if="col.format === 'area'">
                  {{ areaText(record, col) }}
                </span>
                <span v-else>
                  {{ record[col.key] || '-' }}
                </span>
              </template>
            </template>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalOpen"
      :title="editingId ? `编辑${config.title}` : `新增${config.title}`"
      width="760px"
      ok-text="保存"
      cancel-text="取消"
      @ok="saveRecord"
    >
      <Form :model="formState" layout="vertical">
        <div class="crud-modal-grid">
          <Form.Item
            v-for="field in config.fields"
            :key="field.key"
            :label="field.label"
            :required="field.required"
          >
            <Input
              v-if="!field.type || field.type === 'text'"
              v-model:value="formState[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`"
            />
            <InputNumber
              v-else-if="field.type === 'money' || field.type === 'number'"
              v-model:value="formState[field.key]"
              :min="0"
              :precision="field.type === 'money' ? 2 : 0"
              class="crud-number-input"
              :placeholder="field.placeholder || `请输入${field.label}`"
            />
            <Select
              v-else-if="field.type === 'select'"
              v-model:value="formState[field.key]"
              allow-clear
              :options="field.options"
              :placeholder="field.placeholder || `请选择${field.label}`"
            />
            <DatePicker
              v-else-if="field.type === 'date'"
              v-model:value="formState[field.key]"
              value-format="YYYY-MM-DD"
              class="crud-number-input"
            />
            <Cascader
              v-else-if="field.type === 'region'"
              v-model:value="formState[field.key]"
              allow-clear
              change-on-select
              :options="regionOptions"
              :placeholder="field.placeholder || '可选择省 / 市 / 区县'"
              show-search
            />
            <Textarea
              v-else-if="field.type === 'textarea'"
              v-model:value="formState[field.key]"
              :auto-size="{ minRows: 3, maxRows: 5 }"
              :placeholder="field.placeholder || `请输入${field.label}`"
            />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.crud-modal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.crud-number-input {
  width: 100%;
}

@media (max-width: 900px) {
  .crud-modal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
