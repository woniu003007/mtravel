<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

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
  Radio,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  getCustomerCategoryAll,
  type CustomerCategoryApi,
} from '#/api/customer/category';
import {
  createCustomerUnit,
  deleteCustomerUnit,
  getCustomerUnitPage,
  updateCustomerUnit,
  type CustomerUnitApi,
} from '#/api/customer/unit';
import {
  getEnterpriseDepartmentAll,
  type EnterpriseDepartmentApi,
} from '#/api/enterprise/department';
import {
  getEnterpriseEmployeeAll,
  type EnterpriseEmployeeApi,
} from '#/api/enterprise/employee';
import {
  buildRegionOptions,
  buildRegionPath,
  splitRegionPath,
  type RegionPath,
} from './region';
import { buildCustomerContractRoute } from '../contract/route-query';

interface CustomerUnitFormState
  extends Omit<CustomerUnitApi.SaveParams, 'contractExpireDate'> {
  contractExpireDate?: string;
}

const columns: TableColumnsType<CustomerUnitApi.CustomerUnit> = [
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 190 },
  { title: '业务代码', dataIndex: 'customerCode', key: 'customerCode', width: 120 },
  { title: '客户分类', dataIndex: 'categoryName', key: 'categoryName', width: 120 },
  { title: '授信额度', dataIndex: 'creditLimit', key: 'creditLimit', width: 130 },
  { title: '结款方式', dataIndex: 'settlementMethod', key: 'settlementMethod', width: 110 },
  { title: '所在地', key: 'area', width: 170 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 110 },
  { title: '操作计调', dataIndex: 'dispatcherName', key: 'dispatcherName', width: 110 },
  { title: '负责人/电话', key: 'contact', width: 170 },
  { title: '登记人', dataIndex: 'registrarName', key: 'registrarName', width: 100 },
  { title: '合同有效期', dataIndex: 'contractExpireDate', key: 'contractExpireDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', fixed: 'right', width: 220 },
];

const statusOptions = [
  { label: '有效', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const settlementOptions: Array<{
  label: string;
  value: CustomerUnitApi.SettlementMethod;
}> = [
  { label: '不限', value: 'unlimited' },
  { label: '现结', value: 'cash' },
  ...Array.from({ length: 12 }, (_, index) => ({
    label: `${index + 1}个月结`,
    value: `monthly_${index + 1}` as CustomerUnitApi.SettlementMethod,
  })),
];

const regionOptions = buildRegionOptions();

const data = ref<CustomerUnitApi.CustomerUnit[]>([]);
const categories = ref<CustomerCategoryApi.CustomerCategory[]>([]);
const departments = ref<EnterpriseDepartmentApi.Item[]>([]);
const employees = ref<EnterpriseEmployeeApi.Item[]>([]);
const loading = ref(false);
const categoryLoading = ref(false);
const departmentLoading = ref(false);
const employeeLoading = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();
const queryRegionPath = ref<RegionPath>([]);
const formRegionPath = ref<RegionPath>([]);
const router = useRouter();

const query = reactive<CustomerUnitApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<CustomerUnitFormState>({
  categoryId: undefined,
  city: '',
  contactName: '',
  contactPhone: '',
  contractExpireDate: undefined,
  creditLimit: 0,
  customerCode: '',
  customerName: '',
  departmentId: undefined,
  departmentName: '',
  dispatcherEmployeeId: undefined,
  dispatcherName: '',
  district: '',
  province: '',
  registrarName: '',
  remark: '',
  settlementMethod: 'unlimited',
  billStartDate: undefined,
  billDay: undefined,
  status: 'active',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const categoryOptions = computed(() =>
  categories.value.map((item) => ({
    label: item.categoryName,
    value: item.id,
  })),
);

const departmentOptions = computed(() =>
  departments.value.map((item) => ({
    label: item.departmentName,
    value: item.id,
  })),
);

const departmentSearchOptions = computed(() =>
  departments.value.map((item) => ({
    label: item.departmentName,
    value: item.departmentName,
  })),
);

const employeeOptions = computed(() =>
  employees.value.map((item) => ({
    label: item.employeeName,
    value: item.id,
  })),
);

async function loadCategories() {
  categoryLoading.value = true;
  try {
    categories.value = await getCustomerCategoryAll();
  } finally {
    categoryLoading.value = false;
  }
}

async function loadDepartments() {
  departmentLoading.value = true;
  try {
    departments.value = await getEnterpriseDepartmentAll(true);
  } finally {
    departmentLoading.value = false;
  }
}

async function loadEmployees() {
  employeeLoading.value = true;
  try {
    employees.value = await getEnterpriseEmployeeAll(true);
  } finally {
    employeeLoading.value = false;
  }
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getCustomerUnitPage(query);
    data.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  queryRegionPath.value = [];
  Object.assign(query, {
    categoryId: undefined,
    city: undefined,
    customerCode: undefined,
    departmentName: undefined,
    district: undefined,
    keyword: undefined,
    page: 1,
    pageSize: query.pageSize,
    province: undefined,
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
  formRegionPath.value = [];
  Object.assign(formState, {
    categoryId: undefined,
    city: '',
    contactName: '',
    contactPhone: '',
    contractExpireDate: undefined,
    creditLimit: 0,
    customerCode: '',
    customerName: '',
    departmentId: undefined,
    departmentName: '',
    dispatcherEmployeeId: undefined,
    dispatcherName: '',
    district: '',
    province: '',
    registrarName: '',
    remark: '',
    settlementMethod: 'unlimited',
    billStartDate: undefined,
    billDay: undefined,
    status: 'active',
  });
}

function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  modalOpen.value = true;
}

function openEditModal(record: CustomerUnitApi.CustomerUnit) {
  editingId.value = record.id;
  formRegionPath.value = buildRegionPath(
    record.province,
    record.city,
    record.district,
  );
  Object.assign(formState, {
    categoryId: record.categoryId,
    city: record.city || '',
    contactName: record.contactName || '',
    contactPhone: record.contactPhone || '',
    contractExpireDate: record.contractExpireDate,
    creditLimit: record.creditLimit ?? 0,
    customerCode: record.customerCode || '',
    customerName: record.customerName,
    departmentId: record.departmentId,
    departmentName: record.departmentName || '',
    dispatcherEmployeeId: record.dispatcherEmployeeId,
    dispatcherName: record.dispatcherName || '',
    district: record.district || '',
    province: record.province || '',
    registrarName: record.registrarName || '',
    remark: record.remark || '',
    settlementMethod: record.settlementMethod || 'unlimited',
    billStartDate: record.billStartDate,
    billDay: record.billDay,
    status: record.status,
  });
  modalOpen.value = true;
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as CustomerUnitApi.CustomerUnit);
}

function buildSaveParams(): CustomerUnitApi.SaveParams {
  const regionFields = splitRegionPath(formRegionPath.value);
  return {
    categoryId: formState.categoryId,
    city: regionFields.city,
    contactName: clean(formState.contactName),
    contactPhone: clean(formState.contactPhone),
    contractExpireDate: formState.contractExpireDate,
    creditLimit: formState.creditLimit ?? 0,
    customerCode: clean(formState.customerCode),
    customerName: formState.customerName.trim(),
    departmentId: formState.departmentId,
    departmentName: clean(formState.departmentName),
    dispatcherEmployeeId: formState.dispatcherEmployeeId,
    dispatcherName: clean(formState.dispatcherName),
    district: regionFields.district,
    province: regionFields.province,
    registrarName: clean(formState.registrarName),
    remark: formState.remark,
    settlementMethod: formState.settlementMethod || 'unlimited',
    billStartDate: formState.billStartDate,
    billDay: formState.billDay,
    status: formState.status,
  };
}

async function saveCustomerUnit() {
  if (!formState.customerName?.trim()) {
    message.warning('请填写客户名称');
    return;
  }

  const params = buildSaveParams();
  if (editingId.value) {
    await updateCustomerUnit(editingId.value, params);
    message.success('客户单位已更新');
  } else {
    await createCustomerUnit(params);
    message.success('客户单位已新增');
  }
  modalOpen.value = false;
  loadData();
}

function confirmDelete(record: CustomerUnitApi.CustomerUnit) {
  Modal.confirm({
    title: `删除客户单位「${record.customerName}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteCustomerUnit(record.id);
      message.success('客户单位已删除');
      loadData();
    },
  });
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as CustomerUnitApi.CustomerUnit);
}

function handleContracts(record: Record<string, any>) {
  const customer = record as CustomerUnitApi.CustomerUnit;
  router.push(buildCustomerContractRoute({ customerId: customer.id }));
}

function clean(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

function areaText(record: Record<string, any>) {
  return [record.province, record.city, record.district]
    .filter(Boolean)
    .join(' / ') || '-';
}

function formatMoney(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
    style: 'currency',
    currency: 'CNY',
  }).format(Number(value || 0));
}

function settlementLabel(value?: string) {
  return settlementOptions.find((item) => item.value === value)?.label || '不限';
}

function handleDepartmentChange(value?: unknown) {
  if (typeof value !== 'number') {
    formState.departmentName = '';
    return;
  }
  const department = departments.value.find((item) => item.id === value);
  formState.departmentName = department?.departmentName || '';
}

function handleDispatcherChange(value?: unknown) {
  if (typeof value !== 'number') {
    formState.dispatcherName = '';
    return;
  }
  const employee = employees.value.find((item) => item.id === value);
  formState.dispatcherName = employee?.employeeName || '';
}

function handleSearch() {
  const regionFields = splitRegionPath(queryRegionPath.value);
  query.province = regionFields.province;
  query.city = regionFields.city;
  query.district = regionFields.district;
  query.page = 1;
  loadData();
}

onMounted(() => {
  loadCategories();
  loadDepartments();
  loadEmployees();
  loadData();
});

watch(
  () => formState.categoryId,
  (categoryId) => {
    if (!modalOpen.value || !categoryId || editingId.value) {
      return;
    }
    const category = categories.value.find((item) => item.id === categoryId);
    formState.creditLimit = category?.defaultCreditLimit ?? 0;
  },
);
</script>

<template>
  <Page title="客户单位" description="维护客户单位主档，供订单、合同、应收和排团提醒使用">
    <Card>
      <BusinessSearchForm
        label-width="88px"
        :model="query"
        :search-loading="loading"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="客户">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="客户名称 / 负责人 / 电话"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="业务代码">
          <Input
            v-model:value="query.customerCode"
            allow-clear
            placeholder="请输入业务代码"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="客户分类">
          <Select
            v-model:value="query.categoryId"
            allow-clear
            :loading="categoryLoading"
            :options="categoryOptions"
            placeholder="请选择客户分类"
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
        <Form.Item label="所在地">
          <Cascader
            v-model:value="queryRegionPath"
            allow-clear
            change-on-select
            :options="regionOptions"
            placeholder="可选择省 / 市 / 区县"
            show-search
          />
        </Form.Item>
        <Form.Item label="部门">
          <Select
            v-model:value="query.departmentName"
            allow-clear
            :loading="departmentLoading"
            :options="departmentSearchOptions"
            placeholder="请选择部门"
          />
        </Form.Item>
      </BusinessSearchForm>
      <Table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1560 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'categoryName'">
            <Tag v-if="record.categoryName" color="blue">
              {{ record.categoryName }}
            </Tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'area'">
            {{ areaText(record) }}
          </template>
          <template v-else-if="column.key === 'creditLimit'">
            {{ formatMoney(record.creditLimit) }}
          </template>
          <template v-else-if="column.key === 'settlementMethod'">
            {{ settlementLabel(record.settlementMethod) }}
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contactName || '-' }}</div>
            <div class="text-xs text-gray-500">{{ record.contactPhone || '' }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ record.status === 'active' ? '启用' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handleContracts(record)">
                合同管理
              </Button>
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
      :title="editingId ? '编辑客户单位' : '新增客户单位'"
      ok-text="保存"
      cancel-text="取消"
      width="820px"
      @ok="saveCustomerUnit"
    >
      <Form :model="formState" layout="vertical">
        <div class="grid grid-cols-2 gap-x-4">
          <Form.Item label="客户名称" required>
            <Input
              v-model:value="formState.customerName"
              :maxlength="200"
              placeholder="请输入客户单位名称"
            />
          </Form.Item>
          <Form.Item label="业务代码">
            <Input
              v-model:value="formState.customerCode"
              :maxlength="64"
              placeholder="同一租户下非空代码不可重复"
            />
          </Form.Item>
          <Form.Item label="客户分类">
            <Select
              v-model:value="formState.categoryId"
              allow-clear
              :loading="categoryLoading"
              :options="categoryOptions"
              placeholder="请选择客户分类"
            />
          </Form.Item>
          <Form.Item label="授信额度（限额）">
            <InputNumber
              v-model:value="formState.creditLimit"
              class="w-full"
              :min="0"
              :precision="2"
              placeholder="选择分类后自动带出，可手动修改"
            />
          </Form.Item>
          <Form.Item label="客户状态">
            <Radio.Group v-model:value="formState.status">
              <Radio.Button
                v-for="option in statusOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </Radio.Button>
            </Radio.Group>
          </Form.Item>
          <Form.Item label="所在地">
            <Cascader
              v-model:value="formRegionPath"
              allow-clear
              change-on-select
              :options="regionOptions"
              placeholder="可选择省 / 市 / 区县"
              show-search
            />
          </Form.Item>
          <Form.Item label="归属部门">
            <Select
              v-model:value="formState.departmentId"
              allow-clear
              :loading="departmentLoading"
              :options="departmentOptions"
              placeholder="请选择企业部门"
              @change="handleDepartmentChange"
            />
          </Form.Item>
          <Form.Item label="操作计调">
            <Select
              v-model:value="formState.dispatcherEmployeeId"
              allow-clear
              :loading="employeeLoading"
              not-found-content="暂无员工，请先到企业资料 / 员工管理新增"
              :options="employeeOptions"
              placeholder="请选择员工"
              show-search
              :filter-option="(input, option) => String(option?.label || '').includes(input)"
              @change="handleDispatcherChange"
            />
          </Form.Item>
          <Form.Item label="结款方式">
            <Select
              v-model:value="formState.settlementMethod"
              :options="settlementOptions"
              placeholder="请选择结款方式"
            />
          </Form.Item>
          <Form.Item label="账单起始日期">
            <DatePicker
              v-model:value="formState.billStartDate"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
          </Form.Item>
          <Form.Item label="结款日">
            <InputNumber
              v-model:value="formState.billDay"
              class="w-full"
              :min="1"
              :max="31"
              placeholder="每月几号结款"
            />
          </Form.Item>
          <Form.Item label="负责人">
            <Input v-model:value="formState.contactName" :maxlength="80" />
          </Form.Item>
          <Form.Item label="联系电话">
            <Input v-model:value="formState.contactPhone" :maxlength="40" />
          </Form.Item>
          <Form.Item label="登记人">
            <Input v-model:value="formState.registrarName" :maxlength="80" />
          </Form.Item>
          <Form.Item label="合同有效期">
            <DatePicker
              v-model:value="formState.contractExpireDate"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :rows="3"
            placeholder="填写合作说明、特殊结算要求或内部备注"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
</style>
