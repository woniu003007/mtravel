<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';
import type { Dayjs } from 'dayjs';
import type { SalesTeamApi } from '#/api/sales/team';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Radio,
  Select,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getEnterpriseDepartmentAll } from '#/api/enterprise/department';
import { getSalesTeamPage } from '#/api/sales/team';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

import TeamArrangeStatusIcon from './components/TeamArrangeStatusIcon.vue';
import TeamProgressBadge from './components/TeamProgressBadge.vue';

type DatePickerValue = Dayjs | string | undefined;
type DateRangeValue = [string, string] | undefined;
type TeamListItem = SalesTeamApi.ListItem;

const createTeamButtons = [
  { icon: 'lucide:users-round', label: '散拼', type: 'sanpin' },
  { icon: 'lucide:briefcase-business', label: '整团', type: 'zhengtuan' },
  { icon: 'lucide:user-round', label: '散团', type: 'santuan' },
  { icon: 'lucide:ticket', label: '单项', type: 'single' },
] as const;

const teamTypeTabs = [
  { label: '全部', value: 'all' },
  { label: '散拼', value: 'sanpin' },
  { label: '整团', value: 'zhengtuan' },
  { label: '散团', value: 'santuan' },
  { label: '单项', value: 'single' },
] as const;

const businessTypeOptions = [
  { label: '疗休养', value: '疗休养' },
  { label: '地接团', value: '地接团' },
  { label: '研学', value: '研学' },
  { label: '会务团', value: '会务团' },
  { label: '定制团', value: '定制团' },
];

const orderStatusOptions = [
  { label: '未处理', value: 'pending' },
  { label: '已确认', value: 'confirmed' },
  { label: '已取消', value: 'cancelled' },
  { label: '无订单', value: 'none' },
];

const teamStatusOptions = [
  { label: '全部团队', value: 'all' },
  { label: '未发团', value: 'not_departed' },
  { label: '已发团', value: 'departed' },
  { label: '已停售', value: 'stopped' },
  { label: '已取消', value: 'cancelled' },
];

const arrangeColumns = [
  { key: 'guidePlan', shortTitle: '导', title: '导游安排' },
  { key: 'trafficPlan', shortTitle: '交', title: '大交通安排' },
  { key: 'hotelPlan', shortTitle: '住', title: '住宿安排' },
  { key: 'vehiclePlan', shortTitle: '车', title: '用车安排' },
  { key: 'scenicPlan', shortTitle: '景', title: '景区安排' },
  { key: 'mealPlan', shortTitle: '餐', title: '用餐安排' },
  { key: 'otherPlan', shortTitle: '其', title: '其它安排' },
  { key: 'optionalPlan', shortTitle: '自', title: '自费安排' },
  { key: 'shoppingPlan', shortTitle: '购', title: '购物安排' },
  { key: 'groundAgentPlan', shortTitle: '地', title: '地接安排' },
] as const;

const arrangementAnchorMap: Record<string, string> = {
  groundAgentPlan: 'part9',
  guidePlan: 'guide-arrangement',
  hotelPlan: 'part2',
  mealPlan: 'part5',
  optionalPlan: 'part7',
  otherPlan: 'part6',
  scenicPlan: 'part4',
  shoppingPlan: 'part8',
  trafficPlan: 'part1',
  vehiclePlan: 'part3',
};

const columns = [
  { key: 'teamType', title: '类型', width: 62 },
  { key: 'teamNo', title: '团号', width: 150 },
  { className: 'team-customer-column', key: 'customer', title: '客户', width: 170 },
  { className: 'team-name-column', key: 'teamName', title: '团队名称', width: 318 },
  { key: 'travelDays', title: '天数', width: 42 },
  { key: 'startDate', title: '开始', width: 62 },
  { key: 'endDate', title: '结束', width: 62 },
  { key: 'departurePlace', title: '出发地', width: 62 },
  { key: 'seats', title: '预控 / 实收', width: 104 },
  { className: 'team-guide-summary-column', key: 'guideSummary', title: '导游信息', width: 132 },
  ...arrangeColumns.map((item) => ({ key: item.key, title: item.shortTitle, width: 28 })),
  { key: 'progress', title: '进度', width: 42 },
];

const loading = ref(false);
const rows = ref<TeamListItem[]>([]);
const activeTeamType = ref('all');
const advancedSearchOpen = ref(false);
const departmentOptions = ref<{ label: string; value: string }[]>([]);
const departmentOptionsLoaded = ref(false);
const departmentOptionsLoading = ref(false);
const router = useRouter();

const query = reactive<SalesTeamApi.PageQueryParams>({
  page: 1,
  pageSize: 20,
  teamStatus: 'all',
});

const queryDates = reactive({
  addDate: undefined as DatePickerValue,
  endDate: undefined as DatePickerValue,
  startDate: undefined as DatePickerValue,
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 20,
  showSizeChanger: true,
  total: 0,
});

const departureDateRange = computed<DateRangeValue>({
  get(): DateRangeValue {
    const startDate = dateParam(queryDates.startDate);
    const endDate = dateParam(queryDates.endDate);
    if (!startDate || !endDate) return undefined;
    return [startDate, endDate];
  },
  set(value: DateRangeValue) {
    queryDates.startDate = value?.[0];
    queryDates.endDate = value?.[1];
  },
});

function clean(value?: string) {
  return value?.trim() || undefined;
}

function dateParam(value: DatePickerValue) {
  if (!value) return undefined;
  return dayjs.isDayjs(value) ? value.format('YYYY-MM-DD') : dayjs(value).format('YYYY-MM-DD');
}

function buildParams(): SalesTeamApi.PageQueryParams {
  return {
    addDate: dateParam(queryDates.addDate),
    businessType: query.businessType,
    customerKeyword: clean(query.customerKeyword),
    departmentName: query.departmentName,
    departurePlace: clean(query.departurePlace),
    endDate: dateParam(queryDates.endDate),
    guideKeyword: clean(query.guideKeyword),
    keyword: clean(query.keyword),
    operatorKeyword: clean(query.operatorKeyword),
    orderStatus: query.orderStatus,
    page: query.page,
    pageSize: query.pageSize,
    startDate: dateParam(queryDates.startDate),
    teamStatus: query.teamStatus === 'all' ? undefined : query.teamStatus,
    teamType: activeTeamType.value === 'all' ? undefined : activeTeamType.value as SalesTeamApi.TeamType,
    travelDays: query.travelDays,
  };
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getSalesTeamPage(buildParams());
    rows.value = result.items || [];
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  loadData();
}

function resetQuery() {
  activeTeamType.value = 'all';
  query.keyword = undefined;
  query.customerKeyword = undefined;
  query.departmentName = undefined;
  query.guideKeyword = undefined;
  query.operatorKeyword = undefined;
  query.departurePlace = undefined;
  query.businessType = undefined;
  query.orderStatus = undefined;
  query.travelDays = undefined;
  query.teamStatus = 'all';
  queryDates.addDate = undefined;
  queryDates.startDate = undefined;
  queryDates.endDate = undefined;
  query.page = 1;
  loadData();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 20);
  loadData();
}

function handleTeamTypeChange() {
  query.page = 1;
  loadData();
}

async function loadDepartmentOptions() {
  if (departmentOptionsLoaded.value || departmentOptionsLoading.value) {
    return;
  }
  departmentOptionsLoading.value = true;
  try {
    const departments = await getEnterpriseDepartmentAll(false);
    departmentOptions.value = departments.map((item) => ({
      label: item.departmentName,
      value: item.departmentName,
    }));
    departmentOptionsLoaded.value = true;
  } finally {
    departmentOptionsLoading.value = false;
  }
}

function toggleAdvancedSearch() {
  advancedSearchOpen.value = !advancedSearchOpen.value;
  if (advancedSearchOpen.value) {
    void loadDepartmentOptions();
  }
}

function handleCreateTeam(type: SalesTeamApi.TeamType) {
  if (type === 'single') {
    message.info('单项业务包含订单、价格和名单，后续按独立入口接入。');
    return;
  }
  router.push(`/sales/team/create/${type}`);
}

function openTeamOperation(record: TeamListItem) {
  router.push(`/sales/team/operation/${record.id}`);
}

function openTeamEdit(record: TeamListItem) {
  router.push(`/sales/team/edit/${record.id}`);
}

function openArrangementSection(record: TeamListItem, key: string) {
  const anchor = arrangementAnchorMap[key];
  if (!anchor) return;
  router.push(`/sales/team/arrangement/${record.id}#${anchor}`);
}

function teamTypeLabel(value?: string) {
  const option = teamTypeTabs.find((item) => item.value === value);
  return option?.label || value || '-';
}

function teamTypeColor(value?: string) {
  const colors: Record<string, string> = {
    sanpin: 'blue',
    santuan: 'cyan',
    single: 'purple',
    zhengtuan: 'green',
  };
  return colors[value || ''] || 'default';
}

function statusProgress(record: TeamListItem) {
  if (record.status === 'cancelled') return 'cancelled';
  if (record.status === 'stopped') return 'stopped';
  if (record.departureDate && dayjs(record.departureDate).isBefore(dayjs(), 'day')) {
    return 'departed';
  }
  return 'not_departed';
}

function arrangeStatus(record: TeamListItem, key: string) {
  return (record as Record<string, any>)[key] || 'none';
}

function teamRow(record: Record<string, any>) {
  return record as TeamListItem;
}

function dateParts(value?: string) {
  if (!value) return { dateText: '--', weekText: '' };
  const date = dayjs(value);
  const weeks = ['日', '一', '二', '三', '四', '五', '六'];
  return {
    dateText: date.format('MM/DD'),
    weekText: `周${weeks[date.day()]}`,
  };
}

function shortPlace(value?: string) {
  const text = value?.trim();
  if (!text) return '--';
  const parts = text
    .split(/[,\s/，、-]+/)
    .map((item) => item.trim())
    .filter(Boolean);
  if (parts.length > 1) return parts[parts.length - 1];
  const normalized = text.replace(/\s+/g, '');
  const suffixes = ['开发区', '新区', '景区', '街道', '区', '县', '镇', '乡', '市'];
  const suffix = suffixes.find((item) => normalized.endsWith(item));
  if (!suffix) return normalized;
  const finalStart = normalized.length - suffix.length;
  const boundaries = ['省', '市', '州', '盟', '县', '区'];
  const boundaryIndex = Math.max(
    ...boundaries.map((item) => normalized.lastIndexOf(item, finalStart - 1)),
  );
  return boundaryIndex >= 0 ? normalized.slice(boundaryIndex + 1) : normalized;
}

function seatsText(record: TeamListItem) {
  const total = record.totalSeats ?? 0;
  const used = record.usedSeats ?? 0;
  const remaining = record.remainingSeats ?? Math.max(total - used, 0);
  return `${total} / ${used}｜余${remaining}`;
}

onMounted(loadData);
</script>

<template>
  <Page title="团队管理">
    <Card class="team-card" :bordered="false">
      <div class="team-toolbar">
        <div class="team-type-tabs">
          <Radio.Group v-model:value="activeTeamType" button-style="solid" @change="handleTeamTypeChange">
            <Radio.Button v-for="item in teamTypeTabs" :key="item.value" :value="item.value">
              {{ item.label }}
            </Radio.Button>
          </Radio.Group>
        </div>
        <div class="team-create-buttons">
          <Button
            v-for="item in createTeamButtons"
            :key="item.type"
            class="team-create-button"
            type="primary"
            @click="handleCreateTeam(item.type)"
          >
            <IconifyIcon :icon="item.icon" />
            {{ item.label }}
          </Button>
        </div>
      </div>

      <BusinessSearchForm
        actions-in-grid
        :model="query"
        :search-loading="loading"
        search-text="搜索"
        :show-create="false"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item class="team-search-item" label="团号">
          <Input v-model:value="query.keyword" allow-clear placeholder="团号/团队名称/备注" @press-enter="handleSearch" />
        </Form.Item>
        <Form.Item class="team-search-item" label="客户">
          <Input v-model:value="query.customerKeyword" allow-clear placeholder="客户单位/业务员" @press-enter="handleSearch" />
        </Form.Item>
        <Form.Item class="team-search-item" label="计调">
          <Input v-model:value="query.operatorKeyword" allow-clear placeholder="操作计调" @press-enter="handleSearch" />
        </Form.Item>
        <Form.Item class="team-search-item" label="出发地">
          <Input v-model:value="query.departurePlace" allow-clear placeholder="请输入出发地" @press-enter="handleSearch" />
        </Form.Item>
        <Form.Item class="team-search-item" label="业务类型">
          <Select v-model:value="query.businessType" allow-clear :options="businessTypeOptions" placeholder="业务类型" />
        </Form.Item>
        <Form.Item class="business-search-item--wide" label="出团日期">
          <DatePicker.RangePicker
            v-model:value="departureDateRange"
            value-format="YYYY-MM-DD"
            :placeholder="['开始日期', '结束日期']"
          />
        </Form.Item>
        <Form.Item class="team-search-item" label="天数">
          <InputNumber v-model:value="query.travelDays" :min="1" placeholder="天数" />
        </Form.Item>
        <Form.Item class="team-search-item" label="状态">
          <Select v-model:value="query.teamStatus" :options="teamStatusOptions" placeholder="全部团队" />
        </Form.Item>
        <template v-if="advancedSearchOpen">
          <Form.Item class="team-search-item team-advanced-search-item" label="导游">
            <Input v-model:value="query.guideKeyword" allow-clear placeholder="导游姓名/手机号" @press-enter="handleSearch" />
          </Form.Item>
          <Form.Item class="team-search-item team-advanced-search-item" label="部门">
            <Select
              v-model:value="query.departmentName"
              allow-clear
              :loading="departmentOptionsLoading"
              :options="departmentOptions"
              placeholder="请选择部门"
              @dropdown-visible-change="(open) => open && loadDepartmentOptions()"
            />
          </Form.Item>
          <Form.Item class="team-search-item team-advanced-search-item" label="订单状态">
            <Select v-model:value="query.orderStatus" allow-clear :options="orderStatusOptions" placeholder="订单状态" />
          </Form.Item>
          <Form.Item class="team-search-item team-advanced-search-item" label="添加日期">
            <DatePicker v-model:value="queryDates.addDate" placeholder="添加日期" value-format="YYYY-MM-DD" />
          </Form.Item>
        </template>
        <template #extraActions>
          <Button @click="toggleAdvancedSearch">
            {{ advancedSearchOpen ? '收起筛选' : '高级筛选' }}
          </Button>
        </template>
      </BusinessSearchForm>

      <div class="team-table-toolbar">
        <div class="team-table-toolbar-main">
          <Typography.Text strong class="team-table-title">团队列表</Typography.Text>
          <Typography.Text type="secondary" class="team-table-meta">
            共 {{ pagination.total || 0 }} 条
          </Typography.Text>
        </div>
      </div>

      <Table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="small"
        class="team-table"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'teamType'">
            <Tag :color="teamTypeColor(teamRow(record).teamType)">
              {{ teamTypeLabel(teamRow(record).teamType) }}
            </Tag>
            <div class="team-business">{{ teamRow(record).businessType || '--' }}</div>
          </template>
          <template v-else-if="column.key === 'teamNo'">
            <Button type="link" size="small" class="team-link" @click="openTeamOperation(teamRow(record))">
              {{ teamRow(record).teamNo }}
            </Button>
          </template>
          <template v-else-if="column.key === 'customer'">
            <Tooltip :title="teamRow(record).customerSummary || '--'">
              <span class="team-summary-cell team-customer-cell">{{ teamRow(record).customerSummary || '--' }}</span>
            </Tooltip>
          </template>
          <template v-else-if="column.key === 'teamName'">
            <div class="team-name-cell">
              <Tooltip :title="teamRow(record).productName || '--'">
                <Button type="link" size="small" class="team-name-link" @click="openTeamEdit(teamRow(record))">
                  {{ teamRow(record).productName || '--' }}
                </Button>
              </Tooltip>
              <Tooltip v-if="teamRow(record).remark" :title="teamRow(record).remark">
                <div class="team-remark">{{ teamRow(record).remark }}</div>
              </Tooltip>
            </div>
          </template>
          <template v-else-if="column.key === 'travelDays'">
            {{ teamRow(record).travelDays || 1 }}天
          </template>
          <template v-else-if="column.key === 'startDate'">
            <div class="date-cell">
              <span>{{ dateParts(teamRow(record).departureDate).dateText }}</span>
              <span>{{ dateParts(teamRow(record).departureDate).weekText }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'endDate'">
            <div class="date-cell">
              <span>{{ dateParts(teamRow(record).endDate).dateText }}</span>
              <span>{{ dateParts(teamRow(record).endDate).weekText }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'departurePlace'">
            {{ shortPlace(teamRow(record).departurePlace) }}
          </template>
          <template v-else-if="column.key === 'seats'">
            <span class="seats-text">{{ seatsText(teamRow(record)) }}</span>
          </template>
          <template v-else-if="column.key === 'guideSummary'">
            <Tooltip :title="teamRow(record).guideSummary || '--'">
              <span class="team-summary-cell team-guide-cell">{{ teamRow(record).guideSummary || '--' }}</span>
            </Tooltip>
          </template>
          <template v-else-if="arrangeColumns.some((item) => item.key === column.key)">
            <Tooltip :title="`${String(column.title)}：点击进入团队安排`">
              <button
                class="arrange-jump-button"
                type="button"
                @click="openArrangementSection(teamRow(record), String(column.key))"
              >
                <TeamArrangeStatusIcon :status="arrangeStatus(teamRow(record), String(column.key))" />
              </button>
            </Tooltip>
          </template>
          <template v-else-if="column.key === 'progress'">
            <TeamProgressBadge :status="statusProgress(teamRow(record))" />
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>

<style scoped>
.team-card {
  border-radius: 8px;
}

.team-card :deep(.ant-card-body) {
  padding: 14px 16px;
}

.team-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.team-create-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.team-create-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 13px;
  font-weight: 600;
}

.team-type-tabs {
  min-width: 360px;
}

.team-type-tabs :deep(.ant-radio-button-wrapper) {
  height: 34px;
  padding: 0 20px;
  font-weight: 600;
  line-height: 32px;
}

.team-card :deep(.business-search-form) {
  margin-bottom: 8px;
}

.team-table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 10px 0 8px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}

.team-table-toolbar-main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.team-table-title {
  flex: 0 0 auto;
  color: #1f2937;
  font-size: 14px;
}

.team-table-meta {
  min-width: 0;
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-link {
  height: auto;
  padding: 0;
  white-space: nowrap;
  font-weight: 600;
  line-height: 1.35;
}

.team-name {
  color: inherit;
  font-weight: 600;
  line-height: 1.45;
}

.team-table :deep(.team-name-column) {
  width: 318px !important;
  max-width: 318px !important;
}

.team-table :deep(.team-customer-column) {
  width: 170px !important;
  max-width: 170px !important;
}

.team-table :deep(.team-guide-summary-column) {
  width: 132px !important;
  max-width: 132px !important;
}

.team-name-cell {
  width: 318px;
  max-width: 100%;
  overflow: hidden;
}

.team-name-link {
  display: block;
  width: 100%;
  max-width: 100%;
  height: auto;
  padding: 0;
  overflow: hidden;
  font-weight: 600;
  line-height: 1.45;
  text-align: left;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.team-name-link :deep(span) {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-business,
.team-remark,
.muted {
  font-size: 12px;
  opacity: 0.68;
}

.team-summary-cell {
  display: block;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  color: rgb(31 41 55 / 72%);
  font-size: 12px;
  vertical-align: middle;
  word-break: break-all;
}

.team-customer-cell {
  display: -webkit-box;
  max-height: 38px;
  line-height: 19px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.team-guide-cell {
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: normal;
}

.arrange-jump-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.arrange-jump-button:hover {
  filter: brightness(0.96);
}

.team-remark {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seats-text {
  color: inherit;
  font-weight: 600;
  white-space: nowrap;
}

.date-cell {
  display: inline-flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
  line-height: 1.25;
  white-space: nowrap;
}

.team-table :deep(.ant-table) {
  font-size: 13px;
}

.team-table :deep(table) {
  table-layout: fixed;
  width: 100%;
}

.team-table :deep(.ant-table-thead > tr > th) {
  padding: 8px 6px;
  font-weight: 700;
  text-align: center;
}

.team-table :deep(.ant-table-tbody > tr > td) {
  padding: 8px 4px;
  line-height: 1.45;
  vertical-align: middle;
}

.team-table :deep(.ant-table-cell) {
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
}

.team-table :deep(.ant-table-tbody > tr > td:nth-child(2)) {
  white-space: nowrap;
  word-break: normal;
}

.team-table :deep(.ant-table-thead > tr > th:nth-last-child(-n + 11)),
.team-table :deep(.ant-table-tbody > tr > td:nth-last-child(-n + 11)) {
  padding-right: 2px;
  padding-left: 2px;
  text-align: center;
}

@media (max-width: 1600px) {
  .team-type-tabs {
    min-width: 0;
  }
}

@media (max-width: 1100px) {
  .team-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
