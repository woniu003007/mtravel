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
  Switch,
  Table,
  Tag,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  createCustomerCreditRule,
  deleteCustomerCreditRule,
  getCustomerCreditRulePage,
  type CustomerCreditRuleApi,
  updateCustomerCreditRule,
} from '#/api/customer/credit-rule';
import {
  getCustomerCategoryPage,
  type CustomerCategoryApi,
} from '#/api/customer/category';
import {
  getEnterpriseEmployeeAll,
  type EnterpriseEmployeeApi,
} from '#/api/enterprise/employee';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

type CreditRuleRow = CustomerCreditRuleApi.Item;

const columns: TableColumnsType<CreditRuleRow> = [
  { key: 'serial', title: '序号', width: 76 },
  { dataIndex: 'customerLevelName', key: 'customerLevelName', title: '等级名称', width: 160 },
  { dataIndex: 'creditLimit', key: 'creditLimit', title: '授信额度', width: 140, align: 'right' },
  { dataIndex: 'paymentTermDays', key: 'paymentTermDays', title: '账期天数', width: 110, align: 'right' },
  { dataIndex: 'allowOverLimit', key: 'allowOverLimit', title: '允许超额', width: 100, align: 'center' },
  { key: 'approvers', title: '审批人（按顺序）', width: 180, ellipsis: true },
  { key: 'cc', title: '抄送人', width: 180, ellipsis: true },
  { dataIndex: 'status', key: 'status', title: '状态', width: 88, align: 'center' },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 140 },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const data = ref<CreditRuleRow[]>([]);
const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();
const customerLevelLoading = ref(false);
const employeeLoading = ref(false);
const customerLevels = ref<CustomerCategoryApi.CustomerCategory[]>([]);
const employees = ref<EnterpriseEmployeeApi.Item[]>([]);

const query = reactive<CustomerCreditRuleApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<CustomerCreditRuleApi.SaveParams>({
  allowOverLimit: false,
  approverEmployeeIds: [],
  ccEmployeeIds: [],
  creditLimit: 0,
  customerLevelId: undefined as unknown as number,
  paymentTermDays: 0,
  status: 'active',
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

const employeeOptions = computed(() =>
  employees.value.map((item) => ({
    label: item.departmentName
      ? `${item.employeeName}（${item.departmentName}）`
      : item.employeeName,
    value: item.id,
  })),
);

const employeeNameById = computed(
  () => new Map(employees.value.map((item) => [item.id, item.employeeName])),
);

/** 按需加载启用的客户等级，避免列表页预取无关下拉数据。 */
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

/** 按需加载可用员工，供审批人与抄送人多选。 */
async function loadEmployees() {
  if (employees.value.length || employeeLoading.value) return;
  employeeLoading.value = true;
  try {
    employees.value = await getEnterpriseEmployeeAll();
  } finally {
    employeeLoading.value = false;
  }
}

/** 在编辑弹窗首次打开时加载所需选项，并将结果缓存到页面生命周期内。 */
function loadFormOptions() {
  void Promise.all([loadCustomerLevels(), loadEmployees()]);
}

/** 列表接口由后端按授信额度升序返回，前端仅维护分页状态。 */
async function loadData() {
  loading.value = true;
  try {
    const result = await getCustomerCreditRulePage(query);
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
  query.status = undefined;
  query.page = 1;
  void loadData();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  void loadData();
}

/** 按当前分页计算列表序号，避免向用户展示技术主键。 */
function rowSerial(index: number) {
  return (Number(query.page || 1) - 1) * Number(query.pageSize || 10) + index + 1;
}

function resetForm() {
  editingId.value = undefined;
  Object.assign(formState, {
    allowOverLimit: false,
    approverEmployeeIds: [],
    ccEmployeeIds: [],
    creditLimit: 0,
    customerLevelId: undefined,
    paymentTermDays: 0,
    status: 'active',
  });
}

function openCreateModal() {
  resetForm();
  modalOpen.value = true;
  loadFormOptions();
}

function openEditModal(record: CreditRuleRow) {
  editingId.value = record.id;
  Object.assign(formState, {
    allowOverLimit: Boolean(record.allowOverLimit),
    approverEmployeeIds: [...(record.approverEmployeeIds || [])],
    ccEmployeeIds: [...(record.ccEmployeeIds || [])],
    creditLimit: Number(record.creditLimit || 0),
    customerLevelId: record.customerLevelId,
    paymentTermDays: Number(record.paymentTermDays || 0),
    status: record.status,
  });
  modalOpen.value = true;
  loadFormOptions();
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as CreditRuleRow);
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as CreditRuleRow);
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

/** 后端名称优先；本页已经加载员工后，再以员工 ID 兜底还原名称。 */
function employeeNames(names: string[] | undefined, employeeIds: number[]) {
  if (names?.length) return names.join('、');
  const resolvedNames = employeeIds
    .map((id) => employeeNameById.value.get(id))
    .filter((name): name is string => Boolean(name));
  return resolvedNames.join('、') || '-';
}

/** 保存前只发送规则本身需要的字段，审批人数组顺序原样保留。 */
function buildPayload(): CustomerCreditRuleApi.SaveParams {
  return {
    allowOverLimit: Boolean(formState.allowOverLimit),
    approverEmployeeIds: [...formState.approverEmployeeIds],
    ccEmployeeIds: [...formState.ccEmployeeIds],
    creditLimit: Number(formState.creditLimit || 0),
    customerLevelId: formState.customerLevelId,
    paymentTermDays: Number(formState.paymentTermDays || 0),
    status: formState.status || 'active',
  };
}

/** 校验必填的客户等级后新增或更新一条授信规则。 */
async function saveRecord() {
  if (!formState.customerLevelId) {
    message.warning('请选择客户等级');
    return;
  }

  saving.value = true;
  try {
    const payload = buildPayload();
    if (editingId.value) {
      await updateCustomerCreditRule(editingId.value, payload);
      message.success('客户授信规则已更新');
    } else {
      await createCustomerCreditRule(payload);
      message.success('客户授信规则已新增');
    }
    modalOpen.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

/** 确认后软删除一条客户授信规则。 */
function confirmDelete(record: CreditRuleRow) {
  Modal.confirm({
    cancelText: '取消',
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    title: `删除客户授信规则「${record.customerLevelName}」？`,
    async onOk() {
      await deleteCustomerCreditRule(record.id);
      message.success('客户授信规则已删除');
      await loadData();
    },
  });
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <Page title="客户授信规则">
    <Card>
      <BusinessSearchForm
        label-width="88px"
        :model="query"
        :search-loading="loading"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
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
        :scroll="{ x: 1180 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, index, record }">
          <template v-if="column.key === 'serial'">
            {{ rowSerial(index) }}
          </template>
          <template v-else-if="column.key === 'creditLimit'">
            {{ formatMoney(record.creditLimit) }}
          </template>
          <template v-else-if="column.key === 'allowOverLimit'">
            {{ record.allowOverLimit ? '允许' : '不允许' }}
          </template>
          <template v-else-if="column.key === 'approvers'">
            <span :title="employeeNames(record.approverNames, record.approverEmployeeIds)">
              {{ employeeNames(record.approverNames, record.approverEmployeeIds) }}
            </span>
          </template>
          <template v-else-if="column.key === 'cc'">
            <span :title="employeeNames(record.ccNames, record.ccEmployeeIds)">
              {{ employeeNames(record.ccNames, record.ccEmployeeIds) }}
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ record.status === 'active' ? '启用' : '停用' }}
            </Tag>
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
      :title="editingId ? '编辑客户授信规则' : '新增客户授信规则'"
      :width="460"
      cancel-text="取消"
      ok-text="保存"
      @ok="saveRecord"
    >
      <Form :model="formState" layout="vertical">
        <Form.Item label="客户等级" required>
          <Select
            v-model:value="formState.customerLevelId"
            :loading="customerLevelLoading"
            :options="customerLevelOptions"
            placeholder="请选择客户等级"
            show-search
            option-filter-prop="label"
          />
        </Form.Item>
        <Form.Item label="授信额度">
          <InputNumber
            v-model:value="formState.creditLimit"
            addon-before="¥"
            class="w-full"
            :min="0"
            :precision="2"
          />
        </Form.Item>
        <Form.Item label="账期天数">
          <InputNumber
            v-model:value="formState.paymentTermDays"
            addon-after="天"
            class="w-full"
            :min="0"
            :precision="0"
          />
        </Form.Item>
        <Form.Item label="允许超额">
          <Switch v-model:checked="formState.allowOverLimit" />
        </Form.Item>
        <Form.Item label="审批人（按顺序）">
          <Select
            v-model:value="formState.approverEmployeeIds"
            allow-clear
            mode="multiple"
            :loading="employeeLoading"
            :max-tag-count="3"
            :options="employeeOptions"
            placeholder="按审批顺序选择员工"
            show-search
            option-filter-prop="label"
          />
        </Form.Item>
        <Form.Item label="抄送人">
          <Select
            v-model:value="formState.ccEmployeeIds"
            allow-clear
            mode="multiple"
            :loading="employeeLoading"
            :max-tag-count="3"
            :options="employeeOptions"
            placeholder="请选择抄送员工"
            show-search
            option-filter-prop="label"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select v-model:value="formState.status" :options="statusOptions" />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>
