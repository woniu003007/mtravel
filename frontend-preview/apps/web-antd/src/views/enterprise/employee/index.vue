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

import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import { getEnterpriseDepartmentAll, type EnterpriseDepartmentApi } from '#/api/enterprise/department';
import {
  createEnterpriseEmployee,
  deleteEnterpriseEmployee,
  disableEnterpriseEmployee,
  getEnterpriseEmployeePage,
  resetEnterpriseEmployeePassword,
  updateEnterpriseEmployee,
  type EnterpriseEmployeeApi,
} from '#/api/enterprise/employee';
import { getEnterpriseRoleAll, type EnterpriseRoleApi } from '#/api/enterprise/role';

const columns: TableColumnsType<EnterpriseEmployeeApi.Item> = [
  { title: '业务代码', dataIndex: 'employeeCode', key: 'employeeCode', width: 120 },
  { title: '员工名称', dataIndex: 'employeeName', key: 'employeeName', width: 130 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 130 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 130 },
  { title: '角色', dataIndex: 'roleName', key: 'roleName', width: 130 },
  { title: '查看范围', key: 'scope', width: 260 },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 80 },
  { title: '电话', dataIndex: 'telephone', key: 'telephone', width: 130 },
  { title: '手机', dataIndex: 'mobilePhone', key: 'mobilePhone', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', fixed: 'right', width: 230 },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const genderOptions = [
  { label: '男', value: 'male' },
  { label: '女', value: 'female' },
  { label: '未填写', value: 'unknown' },
];

const scopeOptions = [
  { label: '全公司', value: 'company' },
  { label: '部门范围', value: 'department' },
  { label: '个人信息', value: 'personal' },
];

const data = ref<EnterpriseEmployeeApi.Item[]>([]);
const departments = ref<EnterpriseDepartmentApi.Item[]>([]);
const roles = ref<EnterpriseRoleApi.Item[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const saving = ref(false);
const editingId = ref<number>();

const query = reactive<EnterpriseEmployeeApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<EnterpriseEmployeeApi.SaveParams>({
  customerScope: 'personal',
  departmentId: undefined as unknown as number,
  email: '',
  employeeCode: '',
  employeeName: '',
  gender: 'unknown',
  infoScope: 'personal',
  mobilePhone: '',
  profitScope: 'personal',
  receptionScope: 'personal',
  remark: '',
  roleId: undefined as unknown as number,
  sortOrder: 0,
  status: 'active',
  telephone: '',
  username: '',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const departmentOptions = computed(() =>
  departments.value.map((item) => ({
    label: item.parentName
      ? `${item.parentName} / ${item.departmentName}`
      : item.departmentName,
    value: item.id,
  })),
);

const roleOptions = computed(() =>
  roles.value.map((item) => ({
    label: `${item.roleName}（${item.roleCode}）`,
    value: item.id,
  })),
);

async function loadOptions() {
  const [departmentList, roleList] = await Promise.all([
    getEnterpriseDepartmentAll(true),
    getEnterpriseRoleAll(true),
  ]);
  departments.value = departmentList;
  roles.value = roleList;
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getEnterpriseEmployeePage(query);
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
    departmentId: undefined,
    keyword: undefined,
    page: 1,
    pageSize: query.pageSize,
    roleId: undefined,
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
    customerScope: 'personal',
    departmentId: undefined,
    email: '',
    employeeCode: '',
    employeeName: '',
    gender: 'unknown',
    infoScope: 'personal',
    mobilePhone: '',
    profitScope: 'personal',
    receptionScope: 'personal',
    remark: '',
    roleId: undefined,
    sortOrder: 0,
    status: 'active',
    telephone: '',
    username: '',
  });
}

async function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  await loadOptions();
  modalOpen.value = true;
}

async function openEditModal(record: EnterpriseEmployeeApi.Item) {
  editingId.value = record.id;
  Object.assign(formState, {
    customerScope: record.customerScope,
    departmentId: record.departmentId,
    email: record.email || '',
    employeeCode: record.employeeCode || '',
    employeeName: record.employeeName,
    gender: record.gender,
    infoScope: record.infoScope,
    mobilePhone: record.mobilePhone || '',
    profitScope: record.profitScope,
    receptionScope: record.receptionScope,
    remark: record.remark || '',
    roleId: record.roleId,
    sortOrder: record.sortOrder ?? 0,
    status: record.status,
    telephone: record.telephone || '',
    username: record.username,
  });
  await loadOptions();
  modalOpen.value = true;
}

function clean(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

function buildSaveParams(): EnterpriseEmployeeApi.SaveParams {
  return {
    customerScope: formState.customerScope,
    departmentId: formState.departmentId,
    email: clean(formState.email),
    employeeCode: clean(formState.employeeCode),
    employeeName: formState.employeeName.trim(),
    gender: formState.gender,
    infoScope: formState.infoScope,
    mobilePhone: clean(formState.mobilePhone),
    profitScope: formState.profitScope,
    receptionScope: formState.receptionScope,
    remark: clean(formState.remark),
    roleId: formState.roleId,
    sortOrder: Number(formState.sortOrder || 0),
    status: formState.status,
    telephone: clean(formState.telephone),
    username: formState.username.trim(),
  };
}

async function saveEmployee() {
  if (!formState.employeeName?.trim()) {
    message.warning('请填写员工名称');
    return;
  }
  if (!formState.username?.trim()) {
    message.warning('请填写登录账号');
    return;
  }
  if (!formState.departmentId) {
    message.warning('请选择所属部门');
    return;
  }
  if (!formState.roleId) {
    message.warning('请选择角色');
    return;
  }

  saving.value = true;
  try {
    const params = buildSaveParams();
    if (editingId.value) {
      await updateEnterpriseEmployee(editingId.value, params);
      message.success('员工已更新');
    } else {
      await createEnterpriseEmployee(params);
      message.success('员工已新增，初始密码为 123456');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

function confirmDisable(record: EnterpriseEmployeeApi.Item) {
  Modal.confirm({
    title: `停用员工「${record.employeeName}」？`,
    content: '停用后关联登录账号不能继续登录，历史业务归属仍保留。',
    okText: '停用',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await disableEnterpriseEmployee(record.id);
      message.success('员工已停用');
      loadData();
    },
  });
}

function confirmResetPassword(record: EnterpriseEmployeeApi.Item) {
  Modal.confirm({
    title: `重置「${record.employeeName}」的登录密码？`,
    content: '密码将重置为 123456，请提醒员工登录后修改。',
    okText: '重置',
    cancelText: '取消',
    async onOk() {
      await resetEnterpriseEmployeePassword(record.id);
      message.success('密码已重置为 123456');
    },
  });
}

function confirmDelete(record: EnterpriseEmployeeApi.Item) {
  Modal.confirm({
    title: `删除员工「${record.employeeName}」？`,
    content: '删除后不会物理移除记录，会同步软删除关联登录账号。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteEnterpriseEmployee(record.id);
      message.success('员工已删除');
      loadData();
    },
  });
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as EnterpriseEmployeeApi.Item);
}

function handleDisable(record: Record<string, any>) {
  confirmDisable(record as EnterpriseEmployeeApi.Item);
}

function handleResetPassword(record: Record<string, any>) {
  confirmResetPassword(record as EnterpriseEmployeeApi.Item);
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as EnterpriseEmployeeApi.Item);
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').replace(/\.\d+.*/, '').slice(0, 19);
}

function statusLabel(status: EnterpriseEmployeeApi.Status) {
  return status === 'active' ? '启用' : '停用';
}

function genderLabel(gender: EnterpriseEmployeeApi.Gender) {
  const map = { female: '女', male: '男', unknown: '-' };
  return map[gender] || '-';
}

function scopeLabel(scope: EnterpriseEmployeeApi.Scope) {
  const map = { company: '全公司', department: '部门范围', personal: '个人信息' };
  return map[scope];
}

function scopeSummary(record: EnterpriseEmployeeApi.Item) {
  return [
    `信息:${scopeLabel(record.infoScope)}`,
    `利润:${scopeLabel(record.profitScope)}`,
    `收客:${scopeLabel(record.receptionScope)}`,
    `客户:${scopeLabel(record.customerScope)}`,
  ].join(' / ');
}

function scopeSummaryText(record: Record<string, any>) {
  return scopeSummary(record as EnterpriseEmployeeApi.Item);
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadData()]);
});
</script>

<template>
  <Page title="员工管理" description="维护员工名录、登录账号、部门角色和数据查看范围">
    <Card>
      <BusinessSearchForm
        label-width="76px"
        :model="query"
        :search-loading="loading"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="关键词">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="业务代码 / 员工名称 / 用户名 / 电话"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="部门">
          <Select
            v-model:value="query.departmentId"
            allow-clear
            :options="departmentOptions"
            placeholder="请选择部门"
          />
        </Form.Item>
        <Form.Item label="角色">
          <Select
            v-model:value="query.roleId"
            allow-clear
            :options="roleOptions"
            placeholder="请选择角色"
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
        :scroll="{ x: 1750 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'employeeCode'">
            {{ record.employeeCode || '-' }}
          </template>
          <template v-else-if="column.key === 'departmentName'">
            {{ record.departmentName || '-' }}
          </template>
          <template v-else-if="column.key === 'roleName'">
            <Tag color="blue">{{ record.roleName || record.roleCode || '-' }}</Tag>
          </template>
          <template v-else-if="column.key === 'scope'">
            <Tooltip :title="scopeSummaryText(record)">
              <span class="employee-ellipsis">{{ scopeSummaryText(record) }}</span>
            </Tooltip>
          </template>
          <template v-else-if="column.key === 'gender'">
            {{ genderLabel(record.gender) }}
          </template>
          <template v-else-if="column.key === 'telephone'">
            {{ record.telephone || '-' }}
          </template>
          <template v-else-if="column.key === 'mobilePhone'">
            {{ record.mobilePhone || '-' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ statusLabel(record.status) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handleEdit(record)">
                编辑
              </Button>
              <Button type="link" size="small" @click="handleResetPassword(record)">
                重置密码
              </Button>
              <Button
                v-if="record.status === 'active'"
                type="link"
                size="small"
                danger
                @click="handleDisable(record)"
              >
                停用
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
      :title="editingId ? '编辑员工' : '新增员工'"
      ok-text="保存"
      cancel-text="取消"
      width="920px"
      @ok="saveEmployee"
    >
      <Form :model="formState" layout="vertical">
        <div class="employee-form-row">
          <Form.Item label="员工名称" required>
            <Input
              v-model:value="formState.employeeName"
              :maxlength="80"
              placeholder="请输入员工姓名"
            />
          </Form.Item>
          <Form.Item label="登录账号" required>
            <Input
              v-model:value="formState.username"
              :maxlength="80"
              placeholder="新增员工初始密码为 123456"
            />
          </Form.Item>
          <Form.Item label="业务代码">
            <Input
              v-model:value="formState.employeeCode"
              :maxlength="80"
              placeholder="例如：OP001"
            />
          </Form.Item>
        </div>
        <div class="employee-form-row">
          <Form.Item label="所属部门" required>
            <Select
              v-model:value="formState.departmentId"
              :options="departmentOptions"
              placeholder="请选择部门"
            />
          </Form.Item>
          <Form.Item label="角色" required>
            <Select
              v-model:value="formState.roleId"
              :options="roleOptions"
              placeholder="请选择角色"
            />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="formState.status" :options="statusOptions" />
          </Form.Item>
        </div>
        <div class="employee-form-row">
          <Form.Item label="性别">
            <Select v-model:value="formState.gender" :options="genderOptions" />
          </Form.Item>
          <Form.Item label="固定电话">
            <Input
              v-model:value="formState.telephone"
              :maxlength="40"
              placeholder="请输入固定电话"
            />
          </Form.Item>
          <Form.Item label="手机">
            <Input
              v-model:value="formState.mobilePhone"
              :maxlength="40"
              placeholder="请输入手机号"
            />
          </Form.Item>
        </div>
        <div class="employee-form-row">
          <Form.Item label="邮箱">
            <Input
              v-model:value="formState.email"
              :maxlength="120"
              placeholder="请输入邮箱"
            />
          </Form.Item>
          <Form.Item label="排序">
            <InputNumber
              v-model:value="formState.sortOrder"
              :min="0"
              style="width: 100%"
            />
          </Form.Item>
          <Form.Item label="信息范围">
            <Select v-model:value="formState.infoScope" :options="scopeOptions" />
          </Form.Item>
        </div>
        <div class="employee-form-row">
          <Form.Item label="利润范围">
            <Select v-model:value="formState.profitScope" :options="scopeOptions" />
          </Form.Item>
          <Form.Item label="收客范围">
            <Select v-model:value="formState.receptionScope" :options="scopeOptions" />
          </Form.Item>
          <Form.Item label="客户范围">
            <Select v-model:value="formState.customerScope" :options="scopeOptions" />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="填写员工职责或内部管理说明"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.employee-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
}

.employee-ellipsis {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .employee-form-row {
    display: block;
  }
}
</style>
