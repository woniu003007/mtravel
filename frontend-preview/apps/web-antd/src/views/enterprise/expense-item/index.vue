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
  Switch,
  Table,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import BusinessPillTabs from '#/components/business/BusinessPillTabs.vue';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  type EnterpriseExpenseItemApi,
  createExpenseItem,
  deleteExpenseItem,
  getExpenseItemPage,
  updateExpenseItem,
} from '#/api/enterprise/expense-item';
import {
  expenseItemTabs,
  expenseItemTypeLabel,
  expenseItemTypeOptions,
  type ExpenseItemTabKey,
} from './expense-item-tabs';

type ExpenseItemRow = EnterpriseExpenseItemApi.Item;
type ResourceType = EnterpriseExpenseItemApi.ResourceType;

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const columns = [
  { dataIndex: 'resourceType', key: 'resourceType', title: '资源类型', width: 120 },
  { dataIndex: 'projectName', key: 'projectName', title: '项目名称', width: 180 },
  { dataIndex: 'statisticsEnabled', key: 'statisticsEnabled', title: '统计', width: 100 },
  { dataIndex: 'sortOrder', key: 'sortOrder', title: '排序', width: 90 },
  { dataIndex: 'createdBy', key: 'createdBy', title: '创建人', width: 110 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间', width: 170 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 150 },
];

const data = ref<ExpenseItemRow[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();
const activeTab = ref<ExpenseItemTabKey>('all');

const query = reactive<EnterpriseExpenseItemApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<EnterpriseExpenseItemApi.SaveParams>({
  projectName: '',
  resourceType: 'scenic',
  sortOrder: 0,
  statisticsEnabled: true,
  status: 'active',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const pageDescription = computed(() => (
  '维护不同资源类型下的项目类型，采购关系价格管理会按资源类型自动过滤。'
));

function resourceTypeLabel(value?: string) {
  return expenseItemTypeLabel(value);
}

function resourceTypeColor(value?: string) {
  const colors: Record<string, string> = {
    current_refund: 'cyan',
    extra_fee: 'geekblue',
    finance_fee: 'red',
    ground_agent: 'blue',
    guide: 'purple',
    hotel: 'volcano',
    misc: 'cyan',
    restaurant: 'orange',
    scenic: 'green',
    shopping: 'magenta',
    traffic: 'gold',
    vehicle: 'lime',
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
    const params = {
      ...query,
      resourceType: activeTab.value === 'all' ? undefined : activeTab.value as ResourceType,
    };
    const result = await getExpenseItemPage(params);
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

function handleTabChange() {
  query.page = 1;
  loadData();
}

function selectExpenseItemTab(tabKey: ExpenseItemTabKey) {
  if (activeTab.value === tabKey) return;
  activeTab.value = tabKey;
  handleTabChange();
}

function resetForm() {
  editingId.value = undefined;
  formState.resourceType = activeTab.value === 'all' ? 'scenic' : activeTab.value;
  formState.projectName = '';
  formState.statisticsEnabled = true;
  formState.sortOrder = 0;
  formState.status = 'active';
  formState.remark = undefined;
}

function openCreateModal() {
  resetForm();
  modalOpen.value = true;
}

function openEditModal(record: Record<string, any>) {
  const row = record as ExpenseItemRow;
  editingId.value = row.id;
  formState.resourceType = row.resourceType;
  formState.projectName = row.projectName;
  formState.statisticsEnabled = row.statisticsEnabled;
  formState.sortOrder = row.sortOrder;
  formState.status = row.status;
  formState.remark = row.remark;
  modalOpen.value = true;
}

function buildPayload(): EnterpriseExpenseItemApi.SaveParams {
  return {
    projectName: formState.projectName.trim(),
    remark: clean(formState.remark),
    resourceType: formState.resourceType,
    sortOrder: formState.sortOrder || 0,
    statisticsEnabled: Boolean(formState.statisticsEnabled),
    status: formState.status || 'active',
  };
}

async function saveRecord() {
  if (!formState.projectName?.trim()) {
    message.warning('请填写项目名称');
    return;
  }
  const payload = buildPayload();
  if (editingId.value) {
    await updateExpenseItem(editingId.value, payload);
    message.success('费用项目已更新');
  } else {
    await createExpenseItem(payload);
    message.success('费用项目已新增');
  }
  modalOpen.value = false;
  await loadData();
}

function confirmDelete(record: Record<string, any>) {
  const row = record as ExpenseItemRow;
  Modal.confirm({
    title: `删除费用项目「${row.projectName}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteExpenseItem(row.id);
      message.success('费用项目已删除');
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
  <Page title="费用项目" :description="pageDescription">
    <Card>
      <BusinessPillTabs
        :active-key="activeTab"
        aria-label="费用项目分类"
        :tabs="expenseItemTabs"
        @change="(key) => selectExpenseItemTab(key as ExpenseItemTabKey)"
      />

      <BusinessSearchForm
        label-width="78px"
        :model="query"
        :search-loading="loading"
        create-text="新增"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="项目名称">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="请输入项目名称"
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
        :scroll="{ x: 1100 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'resourceType'">
            <Tag :color="resourceTypeColor(record.resourceType)">
              {{ resourceTypeLabel(record.resourceType) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'statisticsEnabled'">
            {{ record.statisticsEnabled ? '是' : '否' }}
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
      :title="editingId ? '编辑费用项目' : '新增费用项目'"
      width="640px"
      ok-text="保存"
      cancel-text="取消"
      @ok="saveRecord"
    >
      <Form :model="formState" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="资源类型" required>
            <Select v-model:value="formState.resourceType" :options="expenseItemTypeOptions" />
          </Form.Item>
          <Form.Item label="项目名称" required>
            <Input v-model:value="formState.projectName" allow-clear />
          </Form.Item>
          <Form.Item label="统计">
            <Switch
              v-model:checked="formState.statisticsEnabled"
              checked-children="是"
              un-checked-children="否"
            />
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
