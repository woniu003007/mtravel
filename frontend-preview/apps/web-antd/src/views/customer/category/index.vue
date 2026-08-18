<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';

import { IconifyIcon } from '@vben/icons';
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
  Tooltip,
  message,
} from 'ant-design-vue';
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import {
  createCustomerCategory,
  deleteCustomerCategory,
  getCustomerCategoryPage,
  updateCustomerCategory,
  type CustomerCategoryApi,
} from '#/api/customer/category';
import {
  getEnterpriseEmployeePage,
  type EnterpriseEmployeeApi,
} from '#/api/enterprise/employee';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

import {
  buildCreditPolicyPayload,
  moveApprovalMember,
  validateCreditPolicyForm,
  type CreditPolicyFormState,
} from './credit-policy-model';

interface EmployeeOption {
  employeeName: string;
  label: string;
  username?: string;
  value: number;
}

const columns = [
  { dataIndex: 'id', key: 'id', title: '规则ID', width: 92 },
  {
    dataIndex: 'categoryName',
    key: 'categoryName',
    title: '等级名称',
    width: 140,
  },
  {
    align: 'right' as const,
    dataIndex: 'defaultCreditLimit',
    key: 'defaultCreditLimit',
    title: '授信额度',
    width: 140,
  },
  {
    dataIndex: 'creditTermDays',
    key: 'creditTermDays',
    title: '账期天数',
    width: 100,
  },
  {
    dataIndex: 'allowOverLimit',
    key: 'allowOverLimit',
    title: '允许超额',
    width: 104,
  },
  {
    dataIndex: 'approvers',
    key: 'approvers',
    title: '审批人（按顺序）',
    width: 240,
  },
  { dataIndex: 'ccUsers', key: 'ccUsers', title: '抄送人', width: 190 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 150 },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const data = ref<CustomerCategoryApi.CustomerCategory[]>([]);
const loading = ref(false);
const saveLoading = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();
const employeeLoading = ref(false);
const employeeOptions = ref<EmployeeOption[]>([]);
let employeeInitialLoaded = false;
let employeeSearchTimer: ReturnType<typeof setTimeout> | undefined;
let employeeSearchSequence = 0;

const query = reactive<{
  keyword?: string;
  page: number;
  pageSize: number;
  status?: 'active' | 'disabled';
}>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<CreditPolicyFormState>({
  allowOverLimit: false,
  approverIds: [],
  categoryName: '',
  ccUserIds: [],
  creditTermDays: 0,
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

const selectedEmployeeOptions = computed(() => {
  const selectedIds = new Set([
    ...formState.approverIds.filter(
      (id): id is number => typeof id === 'number',
    ),
    ...formState.ccUserIds,
  ]);
  return employeeOptions.value.filter((option) =>
    selectedIds.has(option.value),
  );
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

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

function resetForm() {
  Object.assign(formState, {
    allowOverLimit: false,
    approverIds: [],
    categoryName: '',
    ccUserIds: [],
    creditTermDays: 0,
    defaultCreditLimit: 0,
    remark: '',
    sortOrder: 0,
    status: 'active',
  });
  employeeOptions.value = [];
  employeeInitialLoaded = false;
}

function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  modalOpen.value = true;
}

function openEditModal(record: CustomerCategoryApi.CustomerCategory) {
  editingId.value = record.id;
  Object.assign(formState, {
    allowOverLimit: Boolean(record.allowOverLimit),
    approverIds: [...(record.approvers || [])]
      .sort(
        (left, right) =>
          Number(left.stepOrder || 0) - Number(right.stepOrder || 0),
      )
      .map((member) => member.systemUserId),
    categoryName: record.categoryName,
    ccUserIds: (record.ccUsers || []).map((member) => member.systemUserId),
    creditTermDays: record.creditTermDays ?? 0,
    defaultCreditLimit: Number(record.defaultCreditLimit || 0),
    remark: record.remark || '',
    sortOrder: record.sortOrder ?? 0,
    status: record.status,
  });
  employeeOptions.value = membersToOptions([
    ...(record.approvers || []),
    ...(record.ccUsers || []),
  ]);
  // 编辑时先保留已选人员用于回显，但下拉首次展开仍需加载全部启用员工。
  employeeInitialLoaded = false;
  modalOpen.value = true;
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as CustomerCategoryApi.CustomerCategory);
}

function membersToOptions(members: CustomerCategoryApi.ApprovalMember[]) {
  const options = members.map((member) => ({
    employeeName:
      member.employeeName || member.username || `用户${member.systemUserId}`,
    label: employeeLabel(member.employeeName, member.username),
    username: member.username,
    value: member.systemUserId,
  }));
  return [...new Map(options.map((option) => [option.value, option])).values()];
}

function employeeLabel(employeeName?: string, username?: string) {
  const name = employeeName || username || '未知员工';
  return username && username !== name ? `${name}（${username}）` : name;
}

function employeeToOption(
  employee: EnterpriseEmployeeApi.Item,
): EmployeeOption | undefined {
  if (!employee.systemUserId) return undefined;
  return {
    employeeName: employee.employeeName,
    label: employeeLabel(employee.employeeName, employee.username),
    username: employee.username,
    value: employee.systemUserId,
  };
}

function mergeEmployeeOptions(nextOptions: EmployeeOption[]) {
  const merged = new Map<number, EmployeeOption>();
  for (const option of [...selectedEmployeeOptions.value, ...nextOptions]) {
    merged.set(option.value, option);
  }
  employeeOptions.value = [...merged.values()];
}

async function searchEmployeesNow(keyword = '') {
  const sequence = ++employeeSearchSequence;
  employeeLoading.value = true;
  try {
    const result = await getEnterpriseEmployeePage({
      keyword: keyword.trim() || undefined,
      page: 1,
      pageSize: 20,
      status: 'active',
    });
    if (sequence !== employeeSearchSequence) return;
    if (!keyword.trim()) employeeInitialLoaded = true;
    mergeEmployeeOptions(
      result.items
        .map(employeeToOption)
        .filter((option): option is EmployeeOption => Boolean(option)),
    );
  } finally {
    if (sequence === employeeSearchSequence) employeeLoading.value = false;
  }
}

function handleEmployeeSearch(keyword: string) {
  if (employeeSearchTimer) clearTimeout(employeeSearchTimer);
  employeeSearchTimer = setTimeout(() => searchEmployeesNow(keyword), 250);
}

function handleEmployeeDropdown(open: boolean) {
  if (open && !employeeInitialLoaded && !employeeLoading.value) {
    void searchEmployeesNow();
  }
}

function approverOptions(index: number) {
  const currentId = formState.approverIds[index];
  const usedIds = new Set(
    formState.approverIds.filter(
      (id, itemIndex): id is number =>
        itemIndex !== index && typeof id === 'number',
    ),
  );
  return employeeOptions.value.filter(
    (option) => option.value === currentId || !usedIds.has(option.value),
  );
}

function addApprover() {
  formState.approverIds.push(undefined);
}

function removeApprover(index: number) {
  formState.approverIds.splice(index, 1);
}

function reorderApprover(index: number, offset: -1 | 1) {
  formState.approverIds = moveApprovalMember(
    formState.approverIds,
    index,
    offset,
  );
}

function handleAllowOverLimitChange(checked: boolean | number | string) {
  if (Boolean(checked) && formState.approverIds.length === 0) addApprover();
}

async function saveCategory() {
  const error = validateCreditPolicyForm(formState);
  if (error) {
    message.warning(error);
    return;
  }

  saveLoading.value = true;
  try {
    const payload = buildCreditPolicyPayload(formState);
    if (editingId.value) {
      await updateCustomerCategory(editingId.value, payload);
      message.success('客户等级授信规则已更新');
    } else {
      await createCustomerCategory(payload);
      message.success('客户等级授信规则已新增');
    }
    modalOpen.value = false;
    await loadData();
  } finally {
    saveLoading.value = false;
  }
}

function formatMoney(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    currency: 'CNY',
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
    style: 'currency',
  }).format(Number(value || 0));
}

function memberSummary(
  members?: CustomerCategoryApi.ApprovalMember[],
  ordered = false,
) {
  if (!members?.length) return '-';
  return members
    .map((member, index) => {
      const name =
        member.employeeName || member.username || `用户${member.systemUserId}`;
      return ordered ? `${index + 1}. ${name}` : name;
    })
    .join(ordered ? ' → ' : '、');
}

function confirmDelete(record: CustomerCategoryApi.CustomerCategory) {
  Modal.confirm({
    cancelText: '取消',
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    async onOk() {
      await deleteCustomerCategory(record.id);
      message.success('客户等级授信规则已删除');
      await loadData();
    },
    title: `删除规则「${record.categoryName}」？`,
  });
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as CustomerCategoryApi.CustomerCategory);
}

onMounted(loadData);
onBeforeUnmount(() => {
  if (employeeSearchTimer) clearTimeout(employeeSearchTimer);
});
</script>

<template>
  <Page title="客户等级授信配置">
    <Card>
      <BusinessSearchForm
        label-width="88px"
        :model="query"
        :search-loading="loading"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="等级名称">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="请输入等级名称"
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
        <template #extraActions>
          <Tooltip title="RFM 规则暂未开放">
            <Button disabled>
              <template #icon
                ><IconifyIcon icon="lucide:chart-no-axes-combined"
              /></template>
              RFM 规则
            </Button>
          </Tooltip>
        </template>
      </BusinessSearchForm>

      <Table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1280 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'">{{ record.id }}</template>
          <template v-else-if="column.key === 'defaultCreditLimit'">
            {{ formatMoney(record.defaultCreditLimit) }}
          </template>
          <template v-else-if="column.key === 'creditTermDays'">
            {{ record.creditTermDays ?? 0 }} 天
          </template>
          <template v-else-if="column.key === 'allowOverLimit'">
            <Tag :color="record.allowOverLimit ? 'blue' : 'default'">
              {{ record.allowOverLimit ? '允许' : '不允许' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'approvers'">
            <Tooltip :title="memberSummary(record.approvers, true)">
              <span class="member-summary">{{
                memberSummary(record.approvers, true)
              }}</span>
            </Tooltip>
          </template>
          <template v-else-if="column.key === 'ccUsers'">
            <Tooltip :title="memberSummary(record.ccUsers)">
              <span class="member-summary">{{
                memberSummary(record.ccUsers)
              }}</span>
            </Tooltip>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ record.status === 'active' ? '启用' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handleEdit(record)"
                >编辑</Button
              >
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
      :title="editingId ? '编辑客户等级授信规则' : '新增客户等级授信规则'"
      width="760px"
      :confirm-loading="saveLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="saveCategory"
    >
      <Form :model="formState" layout="vertical">
        <div class="credit-form-grid">
          <Form.Item label="等级名称" required>
            <Input
              v-model:value="formState.categoryName"
              :maxlength="100"
              placeholder="例如：A级客户"
            />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="formState.status" :options="statusOptions" />
          </Form.Item>
          <Form.Item label="授信额度">
            <InputNumber
              v-model:value="formState.defaultCreditLimit"
              class="w-full"
              :min="0"
              :precision="2"
              addon-before="¥"
              placeholder="0.00"
            />
          </Form.Item>
          <Form.Item label="账期天数">
            <InputNumber
              v-model:value="formState.creditTermDays"
              class="w-full"
              :min="0"
              :max="3650"
              :precision="0"
              addon-after="天"
            />
          </Form.Item>
          <Form.Item label="排序">
            <InputNumber
              v-model:value="formState.sortOrder"
              class="w-full"
              :min="0"
              :precision="0"
            />
          </Form.Item>
          <Form.Item label="允许超额">
            <Switch
              v-model:checked="formState.allowOverLimit"
              checked-children="允许"
              un-checked-children="不允许"
              @change="handleAllowOverLimitChange"
            />
          </Form.Item>
        </div>

        <template v-if="formState.allowOverLimit">
          <Form.Item label="审批人（按顺序）" required>
            <div class="approver-list">
              <div
                v-for="(approverId, index) in formState.approverIds"
                :key="`${index}-${approverId ?? 'empty'}`"
                class="approver-row"
              >
                <span class="step-index">{{ index + 1 }}</span>
                <Select
                  v-model:value="formState.approverIds[index]"
                  class="employee-select"
                  :filter-option="false"
                  :loading="employeeLoading"
                  :options="approverOptions(index)"
                  placeholder="输入姓名或账号搜索"
                  show-search
                  @dropdown-visible-change="handleEmployeeDropdown"
                  @search="handleEmployeeSearch"
                />
                <Space :size="2">
                  <Tooltip title="上移">
                    <Button
                      aria-label="上移审批人"
                      :disabled="index === 0"
                      size="small"
                      type="text"
                      @click="reorderApprover(index, -1)"
                    >
                      <IconifyIcon icon="lucide:arrow-up" />
                    </Button>
                  </Tooltip>
                  <Tooltip title="下移">
                    <Button
                      aria-label="下移审批人"
                      :disabled="index === formState.approverIds.length - 1"
                      size="small"
                      type="text"
                      @click="reorderApprover(index, 1)"
                    >
                      <IconifyIcon icon="lucide:arrow-down" />
                    </Button>
                  </Tooltip>
                  <Tooltip title="移除">
                    <Button
                      aria-label="移除审批人"
                      danger
                      size="small"
                      type="text"
                      @click="removeApprover(index)"
                    >
                      <IconifyIcon icon="lucide:trash-2" />
                    </Button>
                  </Tooltip>
                </Space>
              </div>
              <Button type="dashed" class="add-approver" @click="addApprover">
                <template #icon><IconifyIcon icon="lucide:plus" /></template>
                添加审批人
              </Button>
            </div>
          </Form.Item>

          <Form.Item label="抄送人">
            <Select
              v-model:value="formState.ccUserIds"
              :filter-option="false"
              :loading="employeeLoading"
              :options="employeeOptions"
              mode="multiple"
              placeholder="输入姓名或账号搜索"
              show-search
              @dropdown-visible-change="handleEmployeeDropdown"
              @search="handleEmployeeSearch"
            />
          </Form.Item>
        </template>

        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :maxlength="500"
            :rows="3"
            placeholder="填写等级规则备注"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.member-summary {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credit-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.approver-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.approver-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.step-index {
  display: inline-flex;
  width: 26px;
  height: 26px;
  align-items: center;
  justify-content: center;
  border: 1px solid #d9d9d9;
  border-radius: 50%;
  color: #475569;
  font-size: 12px;
}

.employee-select,
.add-approver {
  width: 100%;
}

@media (max-width: 768px) {
  .credit-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
