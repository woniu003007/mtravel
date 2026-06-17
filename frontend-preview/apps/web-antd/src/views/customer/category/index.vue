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
  message,
} from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';

import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  createCustomerCategory,
  deleteCustomerCategory,
  getCustomerCategoryPage,
  updateCustomerCategory,
  type CustomerCategoryApi,
} from '#/api/customer/category';

const columns = [
  { title: '分类名称', dataIndex: 'categoryName', key: 'categoryName' },
  { title: '默认授信额度', dataIndex: 'defaultCreditLimit', key: 'defaultCreditLimit', width: 150 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '创建人', dataIndex: 'createdBy', key: 'createdBy', width: 120 },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action', width: 180 },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const data = ref<CustomerCategoryApi.CustomerCategory[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();

const query = reactive<{
  keyword?: string;
  page: number;
  pageSize: number;
  status?: 'active' | 'disabled';
}>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<CustomerCategoryApi.SaveParams>({
  categoryName: '',
  defaultCreditLimit: 0,
  remark: '',
  sortOrder: 0,
  status: 'active',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

async function loadData() {
  loading.value = true;
  try {
    const result = await getCustomerCategoryPage(query);
    data.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = undefined;
  query.status = undefined;
  query.page = 1;
  loadData();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

function openCreateModal() {
  editingId.value = undefined;
  Object.assign(formState, {
    categoryName: '',
    defaultCreditLimit: 0,
    remark: '',
    sortOrder: 0,
    status: 'active',
  });
  modalOpen.value = true;
}

function openEditModal(record: CustomerCategoryApi.CustomerCategory) {
  editingId.value = record.id;
  Object.assign(formState, {
    categoryName: record.categoryName,
    defaultCreditLimit: record.defaultCreditLimit ?? 0,
    remark: record.remark || '',
    sortOrder: record.sortOrder ?? 0,
    status: record.status,
  });
  modalOpen.value = true;
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as CustomerCategoryApi.CustomerCategory);
}

async function saveCategory() {
  if (!formState.categoryName?.trim()) {
    message.warning('请填写分类名称');
    return;
  }

  if (editingId.value) {
    await updateCustomerCategory(editingId.value, formState);
    message.success('客户分类已更新');
  } else {
    await createCustomerCategory(formState);
    message.success('客户分类已新增');
  }
  modalOpen.value = false;
  loadData();
}

function formatMoney(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
    style: 'currency',
    currency: 'CNY',
  }).format(Number(value || 0));
}

function confirmDelete(record: CustomerCategoryApi.CustomerCategory) {
  Modal.confirm({
    title: `删除客户分类「${record.categoryName}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteCustomerCategory(record.id);
      message.success('客户分类已删除');
      loadData();
    },
  });
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as CustomerCategoryApi.CustomerCategory);
}

onMounted(loadData);
</script>

<template>
  <Page title="客户分类" description="维护客户分类字典，供客户主档、筛选和统计使用">
    <Card>
      <BusinessSearchForm
        label-width="88px"
        :model="query"
        :search-loading="loading"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="() => { query.page = 1; loadData(); }"
      >
        <Form.Item label="分类名称">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="请输入分类名称"
            @press-enter="() => { query.page = 1; loadData(); }"
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
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ record.status === 'active' ? '启用' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'defaultCreditLimit'">
            {{ formatMoney(record.defaultCreditLimit) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handleEdit(record)">
                编辑
              </Button>
              <Button
                type="link"
                size="small"
                danger
                @click="handleDelete(record)"
              >
                删除
              </Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>
    <Modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑客户分类' : '新增客户分类'"
      ok-text="保存"
      cancel-text="取消"
      @ok="saveCategory"
    >
      <Form :model="formState" layout="vertical">
        <Form.Item label="分类名称" required>
          <Input
            v-model:value="formState.categoryName"
            :maxlength="100"
            placeholder="例如：A类客户、B类客户、旅行社"
          />
        </Form.Item>
        <Form.Item label="排序">
          <InputNumber
            v-model:value="formState.sortOrder"
            class="w-full"
            :min="0"
          />
        </Form.Item>
        <Form.Item label="默认授信额度">
          <InputNumber
            v-model:value="formState.defaultCreditLimit"
            class="w-full"
            :min="0"
            :precision="2"
            placeholder="例如：500000"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select v-model:value="formState.status" :options="statusOptions" />
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :rows="3"
            placeholder="填写分类规则或内部说明"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
</style>
