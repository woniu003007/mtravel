<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  type EnterpriseProductDictionaryApi,
  getProductDictionaryAll,
} from '#/api/enterprise/product-dictionary';
import {
  deleteSalesProduct,
  getSalesProductPage,
  type SalesProductApi,
} from '#/api/sales/product';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

type ProductRow = SalesProductApi.Item;

const router = useRouter();

const columns = computed(() => [
  { dataIndex: 'city', key: 'departure', title: '出发地/接团城市', width: 170 },
  { dataIndex: 'productName', key: 'productName', title: '产品名称', width: 260 },
  { dataIndex: 'receptionStandard', key: 'receptionStandard', title: '标准', width: 130 },
  { dataIndex: 'travelDays', key: 'travelDays', title: '天数', width: 90 },
  { dataIndex: 'businessType', key: 'businessType', title: '业务类型', width: 120 },
  { dataIndex: 'createdBy', key: 'createdBy', title: '创建人', width: 110 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间', width: 170 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  {
    fixed: 'right' as const,
    key: 'action',
    title: '操作',
    width: 390,
  },
]);

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const domesticOptions = [
  { label: '国内', value: 'domestic' },
  { label: '国际', value: 'international' },
];

const tripTypeOptions = [
  { label: '每天发', value: 'daily' },
  { label: '每周发', value: 'weekly' },
  { label: '不定期', value: 'irregular' },
];

const data = ref<ProductRow[]>([]);
const loading = ref(false);
const dictionaryLoading = ref(false);
const businessTypes = ref<EnterpriseProductDictionaryApi.Item[]>([]);
const receptionStandards = ref<EnterpriseProductDictionaryApi.Item[]>([]);

const query = reactive<SalesProductApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const businessTypeOptions = computed(() => dictionaryOptions(businessTypes.value));
const receptionStandardOptions = computed(() => dictionaryOptions(receptionStandards.value));

function dictionaryOptions(items: EnterpriseProductDictionaryApi.Item[]) {
  return items.map((item) => ({
    label: item.dictName,
    value: item.dictName,
  }));
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function areaText(record: Record<string, any>) {
  return [record.province, record.city, record.district].filter(Boolean).join(' / ') || '-';
}

function statusLabel(value?: string) {
  return statusOptions.find((item) => item.value === value)?.label || '-';
}

function statusColor(value?: string) {
  return value === 'active' ? 'green' : 'default';
}

function domesticLabel(value?: string) {
  return domesticOptions.find((item) => item.value === value)?.label || '-';
}

function tripTypeLabel(value?: string) {
  return tripTypeOptions.find((item) => item.value === value)?.label || '-';
}

async function loadDictionaries() {
  dictionaryLoading.value = true;
  try {
    const [business, standard] = await Promise.all([
      getProductDictionaryAll('business_type'),
      getProductDictionaryAll('reception_standard'),
    ]);
    businessTypes.value = business;
    receptionStandards.value = standard;
  } finally {
    dictionaryLoading.value = false;
  }
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getSalesProductPage(query);
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
    businessType: undefined,
    domesticInternational: undefined,
    keyword: undefined,
    page: 1,
    pageSize: query.pageSize,
    receptionStandard: undefined,
    status: undefined,
  });
  loadData();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

function openCreatePage() {
  router.push('/sales/product/create');
}

function openEditPage(record: Record<string, any>, tab = 'basic') {
  const row = record as ProductRow;
  router.push({
    path: `/sales/product/edit/${row.id}`,
    query: { tab },
  });
}

function openTeamArrangementPage(record: Record<string, any>) {
  const row = record as ProductRow;
  router.push(`/sales/product/team-arrangement/${row.id}`);
}

function openSchedulePage(record: Record<string, any>) {
  const row = record as ProductRow;
  router.push(`/sales/product/schedule/${row.id}`);
}

function confirmDelete(record: Record<string, any>) {
  const row = record as ProductRow;
  Modal.confirm({
    cancelText: '取消',
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    title: `删除产品「${row.productName}」？`,
    async onOk() {
      await deleteSalesProduct(row.id);
      message.success('产品已删除');
      await loadData();
    },
  });
}

function showPendingAction(name: string) {
  message.info(`${name} 将在对应业务模块接入后开放`);
}

function openProductAuthorization(record: Record<string, any>) {
  const row = record as ProductRow;
  router.push({
    path: '/customer/product-auth',
    query: {
      productName: row.productName,
    },
  });
}

onMounted(() => {
  loadDictionaries();
  loadData();
});
</script>

<template>
  <Page
    title="产品管理"
    description="维护已完成设计的正式产品模板，新增和修改进入独立页面，避免复杂产品信息挤在弹窗里。"
  >
    <Card>
      <BusinessSearchForm
        label-width="86px"
        :model="query"
        :search-loading="loading"
        show-create
        @create="openCreatePage"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="产品名称">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="产品名称 / 接团城市"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="业务类型">
          <Select
            v-model:value="query.businessType"
            allow-clear
            :loading="dictionaryLoading"
            :options="businessTypeOptions"
            placeholder="请选择业务类型"
          />
        </Form.Item>
        <Form.Item label="接待标准">
          <Select
            v-model:value="query.receptionStandard"
            allow-clear
            :loading="dictionaryLoading"
            :options="receptionStandardOptions"
            placeholder="请选择标准"
          />
        </Form.Item>
        <Form.Item label="国内/国际">
          <Select
            v-model:value="query.domesticInternational"
            allow-clear
            :options="domesticOptions"
            placeholder="请选择"
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
        :scroll="{ x: 1580 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'departure'">
            <div>{{ areaText(record) }}</div>
            <div class="muted">{{ domesticLabel(record.domesticInternational) }} / {{ tripTypeLabel(record.tripType) }}</div>
          </template>
          <template v-else-if="column.key === 'productName'">
            <div class="product-name">{{ record.productName }}</div>
            <div class="muted">{{ record.productTheme || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'receptionStandard'">
            <Tag v-if="record.receptionStandard" color="blue">
              {{ record.receptionStandard }}
            </Tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'travelDays'">
            {{ record.travelDays }} 天
          </template>
          <template v-else-if="column.key === 'businessType'">
            <Tag v-if="record.businessType" color="purple">
              {{ record.businessType }}
            </Tag>
            <span v-else>-</span>
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
            <Space size="small" wrap>
              <Button type="link" size="small" @click="showPendingAction('创建团队')">创建团队</Button>
              <Button type="link" size="small" @click="openSchedulePage(record)">团期管理</Button>
              <Button type="link" size="small" @click="openEditPage(record)">修改产品</Button>
              <Button type="link" size="small" @click="openTeamArrangementPage(record)">团队安排</Button>
              <Button type="link" size="small" @click="showPendingAction('返点规则')">返点规则</Button>
              <Button type="link" size="small" @click="openProductAuthorization(record)">产品授权</Button>
              <Button danger type="link" size="small" @click="confirmDelete(record)">删除产品</Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>

<style scoped>
.product-name {
  font-weight: 600;
  color: #0f172a;
}

.muted {
  font-size: 12px;
  color: #64748b;
}
</style>
