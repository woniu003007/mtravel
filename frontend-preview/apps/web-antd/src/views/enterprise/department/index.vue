<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

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
  Tooltip,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  createEnterpriseDepartment,
  deleteEnterpriseDepartment,
  getEnterpriseDepartmentAll,
  getEnterpriseDepartmentPage,
  updateEnterpriseDepartment,
  type EnterpriseDepartmentApi,
} from '#/api/enterprise/department';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

const columns: TableColumnsType<EnterpriseDepartmentApi.Item> = [
  { title: '部门名称', dataIndex: 'departmentName', key: 'departmentName', width: 180 },
  { title: '部门编码', dataIndex: 'departmentCode', key: 'departmentCode', width: 130 },
  { title: '上级部门', dataIndex: 'parentName', key: 'parentName', width: 140 },
  { title: '负责人', dataIndex: 'managerName', key: 'managerName', width: 120 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'createdBy', key: 'createdBy', width: 110 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 220 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const data = ref<EnterpriseDepartmentApi.Item[]>([]);
const departmentOptions = ref<EnterpriseDepartmentApi.Item[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const saving = ref(false);
const editingId = ref<number>();

const query = reactive<EnterpriseDepartmentApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<EnterpriseDepartmentApi.SaveParams>({
  contactPhone: '',
  departmentCode: '',
  departmentName: '',
  managerName: '',
  parentId: undefined,
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

const parentOptions = computed(() => {
  return departmentOptions.value
    .filter((item) => item.id !== editingId.value)
    .map((item) => ({
      label: item.parentName
        ? `${item.parentName} / ${item.departmentName}`
        : item.departmentName,
      value: item.id,
    }));
});

async function loadDepartmentOptions() {
  departmentOptions.value = await getEnterpriseDepartmentAll(true);
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getEnterpriseDepartmentPage(query);
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
  Object.assign(query, {
    keyword: undefined,
    page: 1,
    pageSize: query.pageSize,
    parentId: undefined,
    status: undefined,
  });
  loadData();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

function resetForm() {
  Object.assign(formState, {
    contactPhone: '',
    departmentCode: '',
    departmentName: '',
    managerName: '',
    parentId: undefined,
    remark: '',
    sortOrder: 0,
    status: 'active',
  });
}

async function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  await loadDepartmentOptions();
  modalOpen.value = true;
}

async function openEditModal(record: EnterpriseDepartmentApi.Item) {
  editingId.value = record.id;
  Object.assign(formState, {
    contactPhone: record.contactPhone || '',
    departmentCode: record.departmentCode || '',
    departmentName: record.departmentName,
    managerName: record.managerName || '',
    parentId: record.parentId,
    remark: record.remark || '',
    sortOrder: record.sortOrder ?? 0,
    status: record.status,
  });
  await loadDepartmentOptions();
  modalOpen.value = true;
}

function clean(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

function buildSaveParams(): EnterpriseDepartmentApi.SaveParams {
  return {
    contactPhone: clean(formState.contactPhone),
    departmentCode: clean(formState.departmentCode),
    departmentName: formState.departmentName.trim(),
    managerName: clean(formState.managerName),
    parentId: formState.parentId,
    remark: clean(formState.remark),
    sortOrder: Number(formState.sortOrder || 0),
    status: formState.status,
  };
}

async function saveDepartment() {
  if (!formState.departmentName?.trim()) {
    message.warning('请填写部门名称');
    return;
  }

  saving.value = true;
  try {
    const params = buildSaveParams();
    if (editingId.value) {
      await updateEnterpriseDepartment(editingId.value, params);
      message.success('部门已更新');
    } else {
      await createEnterpriseDepartment(params);
      message.success('部门已新增');
    }
    modalOpen.value = false;
    await Promise.all([loadDepartmentOptions(), loadData()]);
  } finally {
    saving.value = false;
  }
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as EnterpriseDepartmentApi.Item);
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as EnterpriseDepartmentApi.Item);
}

function confirmDelete(record: EnterpriseDepartmentApi.Item) {
  Modal.confirm({
    title: `删除部门「${record.departmentName}」？`,
    content: '删除后不会物理移除记录；如存在下级部门，需要先处理下级部门。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteEnterpriseDepartment(record.id);
      message.success('部门已删除');
      await Promise.all([loadDepartmentOptions(), loadData()]);
    },
  });
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').replace(/\.\d+.*/, '').slice(0, 19);
}

function statusLabel(status: EnterpriseDepartmentApi.Status) {
  return status === 'active' ? '启用' : '停用';
}

onMounted(async () => {
  await Promise.all([loadDepartmentOptions(), loadData()]);
});
</script>

<template>
  <Page title="部门管理" description="维护企业组织架构，供员工、角色权限、计调归属和统计使用">
    <Card>
      <BusinessSearchForm
        :model="query"
        :search-loading="loading"
        create-text="新增"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="关键词">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="部门名称 / 编码 / 负责人 / 电话"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="上级部门">
          <Select
            v-model:value="query.parentId"
            allow-clear
            :options="parentOptions"
            placeholder="请选择上级部门"
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
        :scroll="{ x: 1500 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'departmentCode'">
            {{ record.departmentCode || '-' }}
          </template>
          <template v-else-if="column.key === 'parentName'">
            {{ record.parentName || '一级部门' }}
          </template>
          <template v-else-if="column.key === 'managerName'">
            {{ record.managerName || '-' }}
          </template>
          <template v-else-if="column.key === 'contactPhone'">
            {{ record.contactPhone || '-' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ statusLabel(record.status) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'remark'">
            <Tooltip v-if="record.remark" :title="record.remark">
              <span class="department-ellipsis">{{ record.remark }}</span>
            </Tooltip>
            <span v-else>-</span>
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
      :confirm-loading="saving"
      :title="editingId ? '编辑部门' : '新增部门'"
      ok-text="保存"
      cancel-text="取消"
      width="720px"
      @ok="saveDepartment"
    >
      <Form :model="formState" layout="vertical">
        <div class="department-form-row">
          <Form.Item label="部门名称" required>
            <Input
              v-model:value="formState.departmentName"
              :maxlength="160"
              placeholder="例如：销售部、计调部、财务部"
            />
          </Form.Item>
          <Form.Item label="部门编码">
            <Input
              v-model:value="formState.departmentCode"
              :maxlength="80"
              placeholder="可选，例如：SALES、OP、FIN"
            />
          </Form.Item>
        </div>
        <div class="department-form-row">
          <Form.Item label="上级部门">
            <Select
              v-model:value="formState.parentId"
              allow-clear
              :options="parentOptions"
              placeholder="不选表示一级部门"
            />
          </Form.Item>
          <Form.Item label="排序">
            <InputNumber
              v-model:value="formState.sortOrder"
              :min="0"
              style="width: 100%"
            />
          </Form.Item>
        </div>
        <div class="department-form-row">
          <Form.Item label="负责人">
            <Input
              v-model:value="formState.managerName"
              :maxlength="80"
              placeholder="请输入部门负责人"
            />
          </Form.Item>
          <Form.Item label="联系电话">
            <Input
              v-model:value="formState.contactPhone"
              :maxlength="40"
              placeholder="请输入联系电话"
            />
          </Form.Item>
        </div>
        <Form.Item label="状态">
          <Select
            v-model:value="formState.status"
            :options="statusOptions"
            style="width: 180px"
          />
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="填写部门职责或内部管理说明"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.department-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.department-ellipsis {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .department-form-row {
    display: block;
  }
}
</style>
