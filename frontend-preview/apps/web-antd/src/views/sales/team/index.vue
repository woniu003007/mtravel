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
  Input,
  InputNumber,
  Radio,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getSalesTeamPage } from '#/api/sales/team';

import TeamArrangeStatusIcon from './components/TeamArrangeStatusIcon.vue';
import TeamProgressBadge from './components/TeamProgressBadge.vue';

type DatePickerValue = Dayjs | string | undefined;
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

const columns = [
  { key: 'teamType', title: '类型', width: 62 },
  { key: 'teamNo', title: '团号', width: 150 },
  { key: 'customer', title: '客户', width: 96 },
  { key: 'teamName', title: '团队名称', width: 210 },
  { key: 'travelDays', title: '天数', width: 42 },
  { key: 'startDate', title: '开始', width: 62 },
  { key: 'endDate', title: '结束', width: 62 },
  { key: 'departurePlace', title: '出发地', width: 62 },
  { key: 'seats', title: '预控 / 实收', width: 104 },
  { key: 'guideSummary', title: '导游信息', width: 90 },
  ...arrangeColumns.map((item) => ({ key: item.key, title: item.shortTitle, width: 28 })),
  { key: 'progress', title: '进度', width: 42 },
];

const loading = ref(false);
const rows = ref<TeamListItem[]>([]);
const activeTeamType = ref('all');
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

function clean(value?: string) {
  return value?.trim() || undefined;
}

function dateParam(value: DatePickerValue) {
  if (!value) return undefined;
  return dayjs.isDayjs(value) ? value.format('YYYY-MM-DD') : dayjs(value).format('YYYY-MM-DD');
}

function buildParams(): SalesTeamApi.PageQueryParams {
  return {
    businessType: query.businessType,
    departurePlace: clean(query.departurePlace),
    endDate: dateParam(queryDates.endDate),
    keyword: clean(query.keyword),
    operatorKeyword: clean(query.operatorKeyword),
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

      <div class="team-search-grid">
        <Input v-model:value="query.keyword" allow-clear placeholder="团号/团队名称/备注" />
        <Tooltip title="客户和业务员筛选待订单链路接入后启用">
          <Input v-model:value="query.customerKeyword" disabled placeholder="客户单位/业务员" />
        </Tooltip>
        <Tooltip title="导游筛选待导游安排表接入后启用">
          <Input v-model:value="query.guideKeyword" disabled placeholder="导游" />
        </Tooltip>
        <Input v-model:value="query.operatorKeyword" allow-clear placeholder="操作计调" />
        <Input v-model:value="query.departurePlace" allow-clear placeholder="出发地" />
        <Tooltip title="部门筛选待订单和员工归属统计接入后启用">
          <Select class="full-width" allow-clear placeholder="=选择部门=" disabled />
        </Tooltip>
        <Select v-model:value="query.businessType" allow-clear :options="businessTypeOptions" placeholder="业务类型" />
        <Tooltip title="订单状态筛选待订单管理接入后启用">
          <Select v-model:value="query.orderStatus" class="full-width" disabled allow-clear :options="orderStatusOptions" placeholder="订单状态" />
        </Tooltip>
        <DatePicker v-model:value="queryDates.startDate" placeholder="出团日期始" value-format="YYYY-MM-DD" />
        <DatePicker v-model:value="queryDates.endDate" placeholder="出团日期止" value-format="YYYY-MM-DD" />
        <Tooltip title="添加日期筛选待团队创建审计字段确认后启用">
          <DatePicker v-model:value="queryDates.addDate" class="full-width" disabled placeholder="添加日期" value-format="YYYY-MM-DD" />
        </Tooltip>
        <InputNumber v-model:value="query.travelDays" :min="1" class="full-width" placeholder="天数" />
        <Select v-model:value="query.teamStatus" :options="teamStatusOptions" placeholder="全部团队" />
        <Space class="team-search-actions">
          <Button @click="resetQuery">重置</Button>
          <Button type="primary" @click="handleSearch">搜索</Button>
        </Space>
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
            <span class="muted">{{ teamRow(record).customerSummary || '--' }}</span>
          </template>
          <template v-else-if="column.key === 'teamName'">
            <Button type="link" size="small" class="team-name-link" @click="openTeamEdit(teamRow(record))">
              {{ teamRow(record).productName || '--' }}
            </Button>
            <div v-if="teamRow(record).remark" class="team-remark">{{ teamRow(record).remark }}</div>
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
            <span class="muted">{{ teamRow(record).guideSummary || '--' }}</span>
          </template>
          <template v-else-if="arrangeColumns.some((item) => item.key === column.key)">
            <TeamArrangeStatusIcon :status="arrangeStatus(teamRow(record), String(column.key))" />
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

.team-search-grid {
  display: grid;
  grid-template-columns: 1.25fr 1.1fr 0.8fr 0.9fr 0.9fr 0.9fr 0.85fr 0.85fr 0.9fr 0.9fr 0.9fr 0.65fr 0.85fr auto;
  gap: 8px;
  margin-bottom: 12px;
}

.team-search-actions {
  justify-content: flex-end;
  min-width: 112px;
}

.full-width {
  width: 100%;
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

.team-business,
.team-remark,
.muted {
  font-size: 12px;
  opacity: 0.68;
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

@media (max-width: 1280px) {
  .team-search-grid {
    grid-template-columns: repeat(4, minmax(120px, 1fr));
  }

  .team-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .team-type-tabs {
    min-width: 0;
  }
}
</style>
