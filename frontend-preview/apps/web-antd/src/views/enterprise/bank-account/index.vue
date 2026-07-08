<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';

import {
  createEnterpriseBankAccount,
  deleteEnterpriseBankAccount,
  getEnterpriseBankAccountPage,
  updateEnterpriseBankAccount,
  updateEnterpriseBankAccountPrintEnabled,
  type EnterpriseBankAccountApi,
} from '#/api/enterprise/bank-account';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

const columns: TableColumnsType<EnterpriseBankAccountApi.Item> = [
  { title: '开户行', dataIndex: 'bankName', key: 'bankName', width: 210 },
  { title: '户名', dataIndex: 'accountName', key: 'accountName', width: 190 },
  { title: '账号', dataIndex: 'accountNo', key: 'accountNo', width: 220 },
  { title: '打印', dataIndex: 'printEnabled', key: 'printEnabled', width: 90 },
  { title: '其它', dataIndex: 'otherInfo', key: 'otherInfo', width: 260 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'createdBy', key: 'createdBy', width: 110 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const data = ref<EnterpriseBankAccountApi.Item[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const saving = ref(false);
const editingId = ref<number>();

const query = reactive<EnterpriseBankAccountApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<EnterpriseBankAccountApi.SaveParams>({
  accountName: '',
  accountNo: '',
  bankName: '',
  otherInfo: '',
  printEnabled: false,
  remark: '',
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
    const result = await getEnterpriseBankAccountPage(query);
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
    accountName: '',
    accountNo: '',
    bankName: '',
    otherInfo: '',
    printEnabled: false,
    remark: '',
    status: 'active',
  });
}

function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  modalOpen.value = true;
}

function openEditModal(record: EnterpriseBankAccountApi.Item) {
  editingId.value = record.id;
  Object.assign(formState, {
    accountName: record.accountName,
    accountNo: record.accountNo,
    bankName: record.bankName,
    otherInfo: record.otherInfo || '',
    printEnabled: record.printEnabled,
    remark: record.remark || '',
    status: record.status,
  });
  modalOpen.value = true;
}

function clean(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

function buildSaveParams(): EnterpriseBankAccountApi.SaveParams {
  return {
    accountName: formState.accountName.trim(),
    accountNo: formState.accountNo.trim(),
    bankName: formState.bankName.trim(),
    otherInfo: clean(formState.otherInfo),
    printEnabled: Boolean(formState.printEnabled),
    remark: clean(formState.remark),
    status: formState.status,
  };
}

async function saveAccount() {
  if (!formState.bankName?.trim()) {
    message.warning('请填写开户行');
    return;
  }
  if (!formState.accountName?.trim()) {
    message.warning('请填写户名');
    return;
  }
  if (!formState.accountNo?.trim()) {
    message.warning('请填写账号');
    return;
  }

  saving.value = true;
  try {
    const params = buildSaveParams();
    if (editingId.value) {
      await updateEnterpriseBankAccount(editingId.value, params);
      message.success('银行账号已更新');
    } else {
      await createEnterpriseBankAccount(params);
      message.success('银行账号已新增');
    }
    modalOpen.value = false;
    loadData();
  } finally {
    saving.value = false;
  }
}

async function handlePrintChange(
  record: EnterpriseBankAccountApi.Item,
  checked: boolean,
) {
  const previous = record.printEnabled;
  record.printEnabled = checked;
  try {
    await updateEnterpriseBankAccountPrintEnabled(record.id, checked);
    message.success('打印设置已更新');
  } catch (error) {
    record.printEnabled = previous;
    throw error;
  }
}

function handlePrintSwitch(record: Record<string, any>, checked: boolean) {
  return handlePrintChange(record as EnterpriseBankAccountApi.Item, checked);
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as EnterpriseBankAccountApi.Item);
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as EnterpriseBankAccountApi.Item);
}

function confirmDelete(record: EnterpriseBankAccountApi.Item) {
  Modal.confirm({
    title: `删除银行账号「${record.bankName} / ${record.accountName}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除，历史收付款记录仍可保留引用。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteEnterpriseBankAccount(record.id);
      message.success('银行账号已删除');
      loadData();
    },
  });
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').replace(/\.\d+.*/, '').slice(0, 19);
}

function statusLabel(status: EnterpriseBankAccountApi.Status) {
  return status === 'active' ? '启用' : '停用';
}

onMounted(loadData);
</script>

<template>
  <Page title="银行账号" description="维护企业收付款账户，供收款、付款、备用金和打印单据使用">
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
            placeholder="开户行 / 户名 / 账号 / 其它说明"
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
        :scroll="{ x: 1500 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'printEnabled'">
            <Switch
              :checked="record.printEnabled"
              checked-children="是"
              un-checked-children="否"
              @change="(checked) => handlePrintSwitch(record, Boolean(checked))"
            />
          </template>
          <template v-else-if="column.key === 'otherInfo'">
            <Tooltip v-if="record.otherInfo" :title="record.otherInfo">
              <span class="bank-account-ellipsis">{{ record.otherInfo }}</span>
            </Tooltip>
            <span v-else>-</span>
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
      :title="editingId ? '编辑银行账号' : '新增银行账号'"
      ok-text="保存"
      cancel-text="取消"
      width="680px"
      @ok="saveAccount"
    >
      <Form :model="formState" layout="vertical">
        <Form.Item label="开户行" required>
          <Input
            v-model:value="formState.bankName"
            :maxlength="200"
            placeholder="例如：中国农业银行湖墅支行、支付宝、微信"
          />
        </Form.Item>
        <Form.Item label="户名" required>
          <Input
            v-model:value="formState.accountName"
            :maxlength="200"
            placeholder="请输入户名或收款主体"
          />
        </Form.Item>
        <Form.Item label="账号" required>
          <Input
            v-model:value="formState.accountNo"
            :maxlength="200"
            placeholder="请输入银行账号、支付宝账号或内部结算标识"
          />
        </Form.Item>
        <Form.Item label="其它说明">
          <Input.TextArea
            v-model:value="formState.otherInfo"
            :auto-size="{ minRows: 3, maxRows: 5 }"
            placeholder="可填写银行地址、SWIFT、联行号、境外汇款资料等"
          />
        </Form.Item>
        <div class="bank-account-form-row">
          <Form.Item label="打印展示">
            <Switch
              v-model:checked="formState.printEnabled"
              checked-children="是"
              un-checked-children="否"
            />
          </Form.Item>
          <Form.Item label="状态">
            <Select
              v-model:value="formState.status"
              :options="statusOptions"
              style="width: 180px"
            />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="填写账户使用说明或内部备注"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.bank-account-form-row {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.bank-account-ellipsis {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .bank-account-form-row {
    display: block;
  }
}
</style>
