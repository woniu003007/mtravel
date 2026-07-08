<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';
import {
  Button,
  Card,
  Checkbox,
  Drawer,
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
  createEnterpriseRole,
  deleteEnterpriseRole,
  getEnterpriseRolePage,
  getEnterpriseRolePermissions,
  saveEnterpriseRolePermissions,
  updateEnterpriseRole,
  type EnterpriseRoleApi,
} from '#/api/enterprise/role';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

const columns: TableColumnsType<EnterpriseRoleApi.Item> = [
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 150 },
  { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode', width: 150 },
  { title: '员工数', dataIndex: 'employeeCount', key: 'employeeCount', width: 90 },
  { title: '内置', dataIndex: 'systemBuiltin', key: 'systemBuiltin', width: 90 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'createdBy', key: 'createdBy', width: 110 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 220 },
  { title: '操作', key: 'action', fixed: 'right', width: 220 },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const permissionCatalog: EnterpriseRoleApi.PermissionItem[] = [
  { moduleCode: 'customer', moduleName: '客户管理', permissionCode: 'customer.view', permissionName: '客户资料查看', permissionType: 'menu', sortOrder: 10 },
  { moduleCode: 'customer', moduleName: '客户管理', permissionCode: 'customer.edit', permissionName: '客户资料维护', permissionType: 'button', sortOrder: 20 },
  { moduleCode: 'purchase', moduleName: '采购管理', permissionCode: 'purchase.view', permissionName: '采购资源查看', permissionType: 'menu', sortOrder: 30 },
  { moduleCode: 'sales', moduleName: '销售管理', permissionCode: 'sales.view', permissionName: '销售订单查看', permissionType: 'menu', sortOrder: 40 },
  { moduleCode: 'dispatch', moduleName: '计调操作', permissionCode: 'dispatch.view', permissionName: '计调安排查看', permissionType: 'menu', sortOrder: 50 },
  { moduleCode: 'finance', moduleName: '财务管理', permissionCode: 'finance.view', permissionName: '财务数据查看', permissionType: 'menu', sortOrder: 60 },
  { moduleCode: 'statistics', moduleName: '统计分析', permissionCode: 'statistics.view', permissionName: '经营统计查看', permissionType: 'menu', sortOrder: 70 },
  { moduleCode: 'enterprise', moduleName: '企业资料', permissionCode: 'enterprise.manage', permissionName: '企业资料维护', permissionType: 'button', sortOrder: 80 },
];

const data = ref<EnterpriseRoleApi.Item[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const saving = ref(false);
const editingId = ref<number>();
const permissionOpen = ref(false);
const permissionSaving = ref(false);
const permissionRole = ref<EnterpriseRoleApi.Item>();
const checkedPermissionCodes = ref<string[]>([]);

const query = reactive<EnterpriseRoleApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<EnterpriseRoleApi.SaveParams>({
  remark: '',
  roleCode: '',
  roleName: '',
  sortOrder: 0,
  status: 'active',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const permissionGroups = computed(() => {
  const groups = new Map<string, EnterpriseRoleApi.PermissionItem[]>();
  for (const item of permissionCatalog) {
    const key = `${item.moduleCode}::${item.moduleName}`;
    groups.set(key, [...(groups.get(key) || []), item]);
  }
  return [...groups.entries()].map(([key, items]) => {
    const [, moduleName] = key.split('::');
    return { items, moduleName };
  });
});

async function loadData() {
  loading.value = true;
  try {
    const result = await getEnterpriseRolePage(query);
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
    remark: '',
    roleCode: '',
    roleName: '',
    sortOrder: 0,
    status: 'active',
  });
}

function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  modalOpen.value = true;
}

function openEditModal(record: EnterpriseRoleApi.Item) {
  editingId.value = record.id;
  Object.assign(formState, {
    remark: record.remark || '',
    roleCode: record.roleCode,
    roleName: record.roleName,
    sortOrder: record.sortOrder ?? 0,
    status: record.status,
  });
  modalOpen.value = true;
}

function clean(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

function buildSaveParams(): EnterpriseRoleApi.SaveParams {
  return {
    remark: clean(formState.remark),
    roleCode: formState.roleCode.trim(),
    roleName: formState.roleName.trim(),
    sortOrder: Number(formState.sortOrder || 0),
    status: formState.status,
  };
}

async function saveRole() {
  if (!formState.roleName?.trim()) {
    message.warning('请填写角色名称');
    return;
  }
  if (!formState.roleCode?.trim()) {
    message.warning('请填写角色编码');
    return;
  }

  saving.value = true;
  try {
    const params = buildSaveParams();
    if (editingId.value) {
      await updateEnterpriseRole(editingId.value, params);
      message.success('角色已更新');
    } else {
      await createEnterpriseRole(params);
      message.success('角色已新增');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

async function openPermissionDrawer(record: EnterpriseRoleApi.Item) {
  permissionRole.value = record;
  checkedPermissionCodes.value = [];
  const permissions = await getEnterpriseRolePermissions(record.id);
  checkedPermissionCodes.value = permissions.map((item) => item.permissionCode);
  permissionOpen.value = true;
}

async function savePermissions() {
  if (!permissionRole.value) {
    return;
  }
  permissionSaving.value = true;
  try {
    const selected = permissionCatalog.filter((item) =>
      checkedPermissionCodes.value.includes(item.permissionCode),
    );
    await saveEnterpriseRolePermissions(permissionRole.value.id, selected);
    message.success('权限已保存');
    permissionOpen.value = false;
  } finally {
    permissionSaving.value = false;
  }
}

function confirmDelete(record: EnterpriseRoleApi.Item) {
  Modal.confirm({
    title: `删除角色「${record.roleName}」？`,
    content: record.employeeCount > 0
      ? '该角色已被员工使用，后端会禁止删除。请先调整员工角色。'
      : '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteEnterpriseRole(record.id);
      message.success('角色已删除');
      loadData();
    },
  });
}

function handlePermission(record: Record<string, any>) {
  openPermissionDrawer(record as EnterpriseRoleApi.Item);
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as EnterpriseRoleApi.Item);
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as EnterpriseRoleApi.Item);
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').replace(/\.\d+.*/, '').slice(0, 19);
}

function statusLabel(status: EnterpriseRoleApi.Status) {
  return status === 'active' ? '启用' : '停用';
}

function permissionTypeLabel(type: EnterpriseRoleApi.PermissionType) {
  const map = { button: '按钮', data: '数据', menu: '菜单' };
  return map[type];
}

onMounted(loadData);
</script>

<template>
  <Page title="角色管理" description="维护企业角色，并提供菜单、按钮和数据权限配置入口">
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
            placeholder="角色名称 / 编码"
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
        :scroll="{ x: 1450 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'roleCode'">
            <Tag color="blue">{{ record.roleCode }}</Tag>
          </template>
          <template v-else-if="column.key === 'systemBuiltin'">
            <Tag :color="record.systemBuiltin ? 'purple' : 'default'">
              {{ record.systemBuiltin ? '内置' : '自定义' }}
            </Tag>
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
              <span class="role-ellipsis">{{ record.remark }}</span>
            </Tooltip>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handlePermission(record)">
                权限
              </Button>
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
      :title="editingId ? '编辑角色' : '新增角色'"
      ok-text="保存"
      cancel-text="取消"
      width="680px"
      @ok="saveRole"
    >
      <Form :model="formState" layout="vertical">
        <div class="role-form-row">
          <Form.Item label="角色名称" required>
            <Input
              v-model:value="formState.roleName"
              :maxlength="160"
              placeholder="例如：计调经理、财务、销售"
            />
          </Form.Item>
          <Form.Item label="角色编码" required>
            <Input
              v-model:value="formState.roleCode"
              :maxlength="80"
              placeholder="例如：dispatch_manager"
            />
          </Form.Item>
        </div>
        <div class="role-form-row">
          <Form.Item label="排序">
            <InputNumber
              v-model:value="formState.sortOrder"
              :min="0"
              style="width: 100%"
            />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="formState.status" :options="statusOptions" />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="填写角色职责或内部管理说明"
          />
        </Form.Item>
      </Form>
    </Modal>

    <Drawer
      v-model:open="permissionOpen"
      :title="`权限管理：${permissionRole?.roleName || ''}`"
      width="560px"
    >
      <div class="permission-note">
        首版先保存权限配置入口；后续菜单、按钮和数据范围拦截会复用这些权限编码。
      </div>
      <div
        v-for="group in permissionGroups"
        :key="group.moduleName"
        class="permission-group"
      >
        <div class="permission-group-title">{{ group.moduleName }}</div>
        <Checkbox.Group v-model:value="checkedPermissionCodes" class="permission-list">
          <Checkbox
            v-for="item in group.items"
            :key="item.permissionCode"
            :value="item.permissionCode"
          >
            {{ item.permissionName }}
            <Tag class="permission-type">{{ permissionTypeLabel(item.permissionType) }}</Tag>
          </Checkbox>
        </Checkbox.Group>
      </div>
      <template #footer>
        <Space>
          <Button @click="permissionOpen = false">取消</Button>
          <Button type="primary" :loading="permissionSaving" @click="savePermissions">
            保存权限
          </Button>
        </Space>
      </template>
    </Drawer>
  </Page>
</template>

<style scoped>
.role-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.role-ellipsis {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.permission-note {
  margin-bottom: 16px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.permission-group {
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid #eef2f7;
}

.permission-group-title {
  margin-bottom: 10px;
  color: #0f172a;
  font-weight: 600;
}

.permission-list {
  display: grid;
  gap: 10px;
}

.permission-type {
  margin-left: 8px;
}

@media (max-width: 768px) {
  .role-form-row {
    display: block;
  }
}
</style>
