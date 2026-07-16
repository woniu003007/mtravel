<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';
import type { SalesTeamApi } from '#/api/sales/team';

import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Checkbox,
  DatePicker,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Table,
  Tabs,
  Tag,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, reactive, ref, watch } from 'vue';

import {
  getSalesTeamPage,
  mergeSalesOrders,
  mergeSalesTeamOrders,
} from '#/api/sales/team';

type TeamListItem = SalesTeamApi.ListItem;
type DateRangeValue = [string, string] | undefined;

export interface MergeSourceOrder {
  customerName?: string;
  guestCount?: number;
  guestCountText?: string;
  guestName?: string;
  id: number;
  orderInfo?: string;
  orderNo?: string;
  receivableAmount?: string;
  teamId?: number;
}

const props = defineProps<{
  excludeTeamId?: number;
  open: boolean;
  orders: MergeSourceOrder[];
  sourceTeamId?: number;
}>();

const emit = defineEmits<{
  success: [];
  'update:open': [value: boolean];
}>();

const activeTab = ref('team');
const targetTeamLoading = ref(false);
const submitting = ref(false);
const targetTeams = ref<TeamListItem[]>([]);
const targetTeamTotal = ref(0);
const targetTeamPage = ref(1);
const targetTeamPageSize = ref(10);
const selectedTargetTeamIds = ref<number[]>([]);
const selectedTargetTeams = ref<TeamListItem[]>([]);
const mergeForm = reactive({
  tagFlag: false,
});
const mergeTeamSearchForm = reactive({
  customerKeyword: '',
  dateRange: undefined as DateRangeValue,
  keyword: '',
  travelDays: undefined as number | undefined,
});
const itemState = reactive<Record<string, {
  priceType: string;
  remark: string;
  unitPrice: number;
}>>({});

const mergeTeamColumns: TableColumnsType<TeamListItem> = [
  { align: 'center', dataIndex: 'teamType', key: 'teamType', title: '类型', width: 76 },
  { dataIndex: 'teamNo', key: 'teamNo', title: '团号', width: 154 },
  { dataIndex: 'productName', key: 'productName', title: '团队名称', width: 320 },
  { align: 'center', dataIndex: 'travelDays', key: 'travelDays', title: '天数', width: 72 },
  { align: 'center', dataIndex: 'departureDate', key: 'departureDate', title: '开始', width: 96 },
  { align: 'center', dataIndex: 'endDate', key: 'endDate', title: '结束', width: 96 },
  { align: 'center', dataIndex: 'departurePlace', key: 'departurePlace', title: '出发地', width: 88 },
  { align: 'center', key: 'seats', title: '预控/实收', width: 128 },
  { align: 'center', dataIndex: 'progress', key: 'progress', title: '进度', width: 82 },
];

const priceTypeOptions = [
  { label: '成人', value: '成人' },
  { label: '儿童', value: '儿童' },
  { label: '儿童不占床', value: '儿童不占床' },
  { label: '老人', value: '老人' },
  { label: '全陪', value: '全陪' },
  { label: '车费', value: '车费' },
  { label: '综费', value: '综费' },
  { label: '接送费', value: '接送费' },
  { label: '代收团款', value: '代收团款' },
  { label: '定金对公', value: '定金对公' },
  { label: '团费', value: '团费' },
  { label: '成本', value: '成本' },
  { label: '其它', value: '其它' },
];

const visibleOrders = computed(() => props.orders || []);
const excludedTargetTeamIds = computed(() => {
  const ids = new Set<number>();
  [props.excludeTeamId, props.sourceTeamId].forEach((id) => {
    if (id) ids.add(id);
  });
  visibleOrders.value.forEach((order) => {
    if (order.teamId) ids.add(order.teamId);
  });
  return ids;
});
const mergeTeamPagination = computed(() => ({
  current: targetTeamPage.value,
  pageSize: targetTeamPageSize.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共${total}条记录`,
  total: targetTeamTotal.value,
}));
const mergeItems = computed(() => selectedTargetTeams.value.flatMap((team) =>
  visibleOrders.value.map((order) => {
    const state = ensureItemState(order.id, team.id);
    return {
      order,
      priceType: state.priceType,
      remark: state.remark,
      targetTeam: team,
      unitPrice: state.unitPrice,
    };
  }),
));

watch(
  () => props.open,
  async (open) => {
    if (open) {
      resetDialog();
      await loadMergeTargetTeams();
    }
  },
);

function updateOpen(value: boolean) {
  emit('update:open', value);
}

function resetDialog() {
  activeTab.value = 'team';
  mergeForm.tagFlag = false;
  selectedTargetTeamIds.value = [];
  selectedTargetTeams.value = [];
  targetTeamPage.value = 1;
  Object.keys(itemState).forEach((key) => delete itemState[key]);
}

function ensureItemState(orderId: number, targetTeamId: number) {
  const key = itemKey(orderId, targetTeamId);
  itemState[key] ||= {
    priceType: '成人',
    remark: '',
    unitPrice: 0,
  };
  return itemState[key];
}

function itemKey(orderId: number, targetTeamId: number) {
  return `${targetTeamId}:${orderId}`;
}

function itemValue(orderId: number, targetTeamId: number) {
  return ensureItemState(orderId, targetTeamId);
}

async function loadMergeTargetTeams() {
  targetTeamLoading.value = true;
  try {
    const [startDate, endDate] = mergeTeamSearchForm.dateRange || [];
    const result = await getSalesTeamPage({
      customerKeyword: mergeTeamSearchForm.customerKeyword || undefined,
      endDate: endDate || undefined,
      keyword: mergeTeamSearchForm.keyword || undefined,
      page: targetTeamPage.value,
      pageSize: targetTeamPageSize.value,
      startDate: startDate || undefined,
      // 拼团目标团队只显示散拼，和老系统 MergeGroup.aspx 目标团列表保持一致。
      teamStatus: 'not_departed',
      teamType: 'sanpin',
      travelDays: mergeTeamSearchForm.travelDays,
    });
    targetTeams.value = result.items.filter((item) => !excludedTargetTeamIds.value.has(item.id));
    pruneExcludedSelectedTargets();
    targetTeamTotal.value = result.total;
  } finally {
    targetTeamLoading.value = false;
  }
}

async function searchMergeTargetTeams() {
  targetTeamPage.value = 1;
  await loadMergeTargetTeams();
}

async function resetMergeTargetTeamSearch() {
  mergeTeamSearchForm.customerKeyword = '';
  mergeTeamSearchForm.dateRange = undefined;
  mergeTeamSearchForm.keyword = '';
  mergeTeamSearchForm.travelDays = undefined;
  targetTeamPage.value = 1;
  await loadMergeTargetTeams();
}

async function handleMergeTeamTableChange(pagination: TablePaginationConfig) {
  targetTeamPage.value = pagination.current || 1;
  targetTeamPageSize.value = pagination.pageSize || 10;
  await loadMergeTargetTeams();
}

function toggleMergeTargetTeam(record: TeamListItem) {
  if (excludedTargetTeamIds.value.has(record.id)) {
    message.warning('来源订单所在团队不能作为拼团目标团');
    return;
  }
  const exists = selectedTargetTeamIds.value.includes(record.id);
  selectedTargetTeamIds.value = exists
    ? selectedTargetTeamIds.value.filter((id) => id !== record.id)
    : [...selectedTargetTeamIds.value, record.id];
  const teamMap = new Map(selectedTargetTeams.value.map((item) => [item.id, item]));
  if (exists) {
    teamMap.delete(record.id);
  } else {
    teamMap.set(record.id, record);
  }
  selectedTargetTeams.value = selectedTargetTeamIds.value
    .map((id) => teamMap.get(id))
    .filter(Boolean) as TeamListItem[];
}

function pruneExcludedSelectedTargets() {
  const excludedIds = excludedTargetTeamIds.value;
  if (selectedTargetTeamIds.value.every((id) => !excludedIds.has(id))) return;
  selectedTargetTeamIds.value = selectedTargetTeamIds.value.filter((id) => !excludedIds.has(id));
  selectedTargetTeams.value = selectedTargetTeams.value.filter((team) => !excludedIds.has(team.id));
}

function clearSelectedTargets() {
  selectedTargetTeamIds.value = [];
  selectedTargetTeams.value = [];
}

async function submitMerge() {
  if (visibleOrders.value.length === 0) {
    message.warning('请先选择订单');
    return;
  }
  if (selectedTargetTeamIds.value.length === 0) {
    message.warning('请选择目标团队');
    return;
  }
  if (selectedTargetTeamIds.value.some((id) => excludedTargetTeamIds.value.has(id))) {
    message.warning('来源订单所在团队不能作为拼团目标团');
    pruneExcludedSelectedTargets();
    return;
  }
  submitting.value = true;
  try {
    const payload: SalesTeamApi.MergeOrderParams = {
      items: mergeItems.value.map((item) => ({
        orderId: item.order.id,
        priceType: item.priceType || '成人',
        remark: item.remark || undefined,
        targetTeamId: item.targetTeam.id,
        unitPrice: Number(item.unitPrice || 0),
      })),
      orderIds: visibleOrders.value.map((order) => order.id),
      tagFlag: mergeForm.tagFlag,
      targetTeamIds: selectedTargetTeamIds.value,
    };
    const result = props.sourceTeamId
      ? await mergeSalesTeamOrders(props.sourceTeamId, payload)
      : await mergeSalesOrders(payload);
    const skipped = result?.skippedCount || 0;
    message.success(skipped > 0
      ? `拼团完成，生成 ${result?.createdCount || 0} 条，跳过 ${skipped} 条`
      : `拼团完成，生成 ${result?.createdCount || 0} 条`);
    updateOpen(false);
    emit('success');
  } catch {
    // requestClient 已统一展示后端错误。
  } finally {
    submitting.value = false;
  }
}

function formatDate(value?: string) {
  return value ? dayjs(value).format('YYYY/MM/DD') : '--';
}

function teamTypeLabel(value?: string) {
  const labels: Record<string, string> = {
    sanpin: '散拼',
    santuan: '散团',
    single: '单项',
    zhengtuan: '整团',
  };
  return value ? labels[value] || value : '--';
}

function progressLabel(value?: string) {
  const labels: Record<string, string> = {
    cancelled: '已取消',
    closed: '已关团',
    departed: '已出团',
    done: '已完成',
    not_departed: '未出团',
    receiving: '收客',
    stopped: '停收',
  };
  return value ? labels[value] || value : '--';
}

function emptyText(value?: null | number | string) {
  if (value === 0) return '0';
  return value ? String(value) : '--';
}

function orderCustomerText(order: MergeSourceOrder) {
  return order.orderNo || order.customerName || order.orderInfo || `订单 ${order.id}`;
}

function orderPeopleText(order: MergeSourceOrder) {
  return order.guestCountText || `${order.guestCount ?? 0}人`;
}

function orderPeopleCount(order: MergeSourceOrder) {
  return Math.max(0, Number(order.guestCount || 0));
}

function targetSeatSummary(record: Partial<TeamListItem>) {
  const total = record.totalSeats ?? 0;
  const used = record.usedSeats ?? 0;
  return `${total}人 | [${used}/${Math.max(0, total - used)}/0]`;
}
</script>

<template>
  <Modal
    :open="open"
    title="拼团操作"
    width="1280px"
    destroy-on-close
    :footer="null"
    @update:open="updateOpen"
  >
    <div class="merge-modal-toolbar">
      <div class="merge-modal-summary">
        <span>已选订单 {{ visibleOrders.length }} 个</span>
        <span>已选目标团 {{ selectedTargetTeamIds.length }} 个</span>
        <Button type="link" size="small" :disabled="selectedTargetTeamIds.length === 0" @click="clearSelectedTargets">
          清空选择
        </Button>
      </div>
      <div class="merge-modal-actions">
        <Checkbox v-model:checked="mergeForm.tagFlag">标记</Checkbox>
        <Button type="primary" :loading="submitting" @click="submitMerge">
          <IconifyIcon icon="lucide:merge" />
          <span>执行拼团</span>
        </Button>
      </div>
    </div>

    <Tabs v-model:activeKey="activeTab" class="merge-operation-tabs">
      <Tabs.TabPane key="team" tab="选择团期">
        <Form layout="inline" class="merge-team-search-form">
          <Form.Item>
            <Input
              v-model:value="mergeTeamSearchForm.keyword"
              placeholder="团号/团队名称"
              allow-clear
              @press-enter="searchMergeTargetTeams"
            />
          </Form.Item>
          <Form.Item>
            <Input
              v-model:value="mergeTeamSearchForm.customerKeyword"
              placeholder="客户单位"
              allow-clear
              @press-enter="searchMergeTargetTeams"
            />
          </Form.Item>
          <Form.Item>
            <DatePicker.RangePicker
              v-model:value="mergeTeamSearchForm.dateRange"
              value-format="YYYY-MM-DD"
              :placeholder="['出团日期始', '出团日期止']"
            />
          </Form.Item>
          <Form.Item>
            <InputNumber
              v-model:value="mergeTeamSearchForm.travelDays"
              :min="1"
              :precision="0"
              placeholder="天数"
              class="merge-team-days-input"
            />
          </Form.Item>
          <Form.Item>
            <Button @click="resetMergeTargetTeamSearch">重置</Button>
          </Form.Item>
          <Form.Item>
            <Button type="primary" :loading="targetTeamLoading" @click="searchMergeTargetTeams">
              <IconifyIcon icon="lucide:search" />
              <span>搜索</span>
            </Button>
          </Form.Item>
        </Form>
        <div v-if="targetTeams.length === 0" class="merge-team-empty">
          <Empty description="暂无可拼入团队" />
        </div>
        <Table
          v-else
          row-key="id"
          size="small"
          class="merge-team-table"
          :columns="mergeTeamColumns"
          :data-source="targetTeams"
          :loading="targetTeamLoading"
          :pagination="mergeTeamPagination"
          :row-class-name="(record) => selectedTargetTeamIds.includes(record.id) ? 'merge-team-row-selected' : ''"
          :scroll="{ x: 1120 }"
          @change="handleMergeTeamTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'teamType'">
              <div class="merge-team-select-cell">
                <Checkbox
                  :checked="selectedTargetTeamIds.includes(record.id)"
                  @click.stop
                  @update:checked="() => toggleMergeTargetTeam(record as TeamListItem)"
                />
                <Tag color="blue" class="merge-team-type-tag">{{ teamTypeLabel(record.teamType) }}</Tag>
              </div>
            </template>
            <template v-else-if="column.key === 'teamNo'">
              <span class="merge-team-no">{{ emptyText(record.teamNo) }}</span>
            </template>
            <template v-else-if="column.key === 'productName'">
              <div class="merge-team-name" :title="emptyText(record.productName || record.remark)">
                {{ emptyText(record.productName || record.remark) }}
              </div>
            </template>
            <template v-else-if="column.key === 'travelDays'">
              {{ record.travelDays ? `${record.travelDays}天` : '--' }}
            </template>
            <template v-else-if="column.key === 'departureDate'">
              {{ formatDate(record.departureDate) }}
            </template>
            <template v-else-if="column.key === 'endDate'">
              {{ formatDate(record.endDate) }}
            </template>
            <template v-else-if="column.key === 'departurePlace'">
              {{ emptyText(record.departurePlace) }}
            </template>
            <template v-else-if="column.key === 'seats'">
              <span class="merge-team-seats">{{ targetSeatSummary(record) }}</span>
            </template>
            <template v-else-if="column.key === 'progress'">
              {{ progressLabel(record.progress) }}
            </template>
          </template>
        </Table>
      </Tabs.TabPane>

      <Tabs.TabPane key="operation" tab="拼团操作">
        <div class="merge-operation-panel">
          <Empty v-if="selectedTargetTeams.length === 0" description="请先在选择团期中选择目标团" />
          <section
            v-for="target in selectedTargetTeams"
            :key="target.id"
            class="merge-target-group"
          >
            <div class="merge-operation-table-wrap">
              <table class="merge-operation-table">
                <colgroup>
                  <col class="col-type" />
                  <col class="col-team-no" />
                  <col class="col-team-name" />
                  <col class="col-days" />
                  <col class="col-date" />
                  <col class="col-date" />
                  <col class="col-departure" />
                  <col class="col-seat" />
                  <col class="col-progress" />
                </colgroup>
                <thead>
                  <tr>
                    <th>类型</th>
                    <th>团号</th>
                    <th>团队名称</th>
                    <th>天数</th>
                    <th>开始</th>
                    <th>结束</th>
                    <th>出发地</th>
                    <th>预控/实收</th>
                    <th>进度</th>
                  </tr>
                </thead>
                <tbody>
                  <tr class="merge-target-row">
                    <td><Tag color="blue" class="merge-target-type-tag">{{ teamTypeLabel(target.teamType) }}</Tag></td>
                    <td class="merge-target-team-no">{{ emptyText(target.teamNo) }}</td>
                    <td>
                      <div class="merge-target-team-name" :title="emptyText(target.productName || target.remark)">
                        {{ emptyText(target.productName || target.remark) }}
                      </div>
                    </td>
                    <td>{{ target.travelDays ? `${target.travelDays}天` : '--' }}</td>
                    <td>{{ formatDate(target.departureDate) }}</td>
                    <td>{{ formatDate(target.endDate) }}</td>
                    <td>{{ emptyText(target.departurePlace) }}</td>
                    <td class="merge-team-seats">{{ targetSeatSummary(target) }}</td>
                    <td>{{ progressLabel(target.progress) }}</td>
                  </tr>
                </tbody>
              </table>

              <table class="merge-operation-detail-table">
                <colgroup>
                  <col class="col-customer" />
                  <col class="col-remark" />
                  <col class="col-people" />
                  <col class="col-price" />
                  <col class="col-price-type" />
                </colgroup>
                <thead>
                  <tr>
                    <th>客户单位</th>
                    <th>拼团备注</th>
                    <th>人数摘要</th>
                    <th>拼团单价</th>
                    <th>价格类型</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="order in visibleOrders"
                    :key="itemKey(order.id, target.id)"
                    class="merge-order-row"
                  >
                    <td>
                      <div class="merge-item-customer" :title="orderCustomerText(order)">
                        {{ orderCustomerText(order) }}
                      </div>
                    </td>
                    <td>
                      <Input
                        v-model:value="itemValue(order.id, target.id).remark"
                        :maxlength="1000"
                        placeholder="拼团备注"
                        class="merge-item-remark-input"
                      />
                    </td>
                    <td class="merge-order-people">{{ orderPeopleText(order) }}</td>
                    <td>
                      <div class="merge-item-inline-price">
                        <span class="merge-item-inline-label">成人：</span>
                        <span class="merge-item-inline-symbol">¥</span>
                        <InputNumber
                          v-model:value="itemValue(order.id, target.id).unitPrice"
                          :min="0"
                          :precision="2"
                          placeholder="默认价格 0"
                          class="merge-item-inline-price-input"
                        />
                        <span class="merge-item-inline-suffix">元 * {{ orderPeopleCount(order) }}人</span>
                      </div>
                    </td>
                    <td>
                      <Select
                        v-model:value="itemValue(order.id, target.id).priceType"
                        :options="priceTypeOptions"
                        class="merge-item-price-type"
                      />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </Tabs.TabPane>
    </Tabs>
  </Modal>
</template>

<style scoped>
.merge-modal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf1f7;
}

.merge-modal-summary,
.merge-modal-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.merge-modal-summary {
  min-width: 0;
  color: #475569;
  font-size: 13px;
}

.merge-team-search-form {
  gap: 8px 10px;
  margin-bottom: 12px;
}

.merge-team-days-input {
  width: 96px;
}

.merge-team-table :deep(.ant-table) {
  font-size: 12.5px;
}

.merge-team-table :deep(.ant-table-cell) {
  padding: 7px 8px !important;
}

.merge-team-table {
  overflow: hidden;
}

.merge-team-table :deep(.ant-table-row) {
  cursor: pointer;
}

.merge-team-table :deep(.merge-team-row-selected > td) {
  background: #e6f4ff !important;
}

.merge-team-empty {
  padding: 12px 0;
}

.merge-team-select-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.merge-team-no {
  font-weight: 700;
  color: #2563eb;
}

.merge-team-name {
  overflow: hidden;
  color: #1f2937;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.merge-team-table :deep(.ant-table-cell:nth-child(3)) {
  overflow: hidden;
}

.merge-team-type-tag,
.merge-target-type-tag {
  margin-inline-end: 0;
}

.merge-team-seats {
  font-variant-numeric: tabular-nums;
}

.merge-operation-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 520px;
  overflow-x: hidden;
  overflow-y: auto;
}

.merge-target-group {
  flex-shrink: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #dbe5f2;
  border-radius: 8px;
}

.merge-operation-table-wrap {
  overflow: hidden;
}

.merge-operation-table,
.merge-operation-detail-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}

.merge-operation-table th,
.merge-operation-table td,
.merge-operation-detail-table th,
.merge-operation-detail-table td {
  padding: 9px 10px;
  color: #1f2937;
  font-size: 12.5px;
  line-height: 1.4;
  text-align: left;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
  vertical-align: middle;
}

.merge-operation-table th,
.merge-operation-detail-table th {
  color: #64748b;
  font-weight: 600;
  background: #fafcff;
}

.merge-operation-table th:last-child,
.merge-operation-table td:last-child,
.merge-operation-detail-table th:last-child,
.merge-operation-detail-table td:last-child {
  border-right: 0;
}

.merge-operation-detail-table tr:last-child td {
  border-bottom: 0;
}

.merge-target-row td {
  background: #fcfeff;
}

.merge-target-team-no {
  font-weight: 700;
  color: #2563eb;
}

.merge-target-team-name {
  overflow: hidden;
  font-weight: 600;
  color: #1f2937;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.merge-item-customer {
  min-width: 0;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.merge-order-people {
  color: #334155;
  font-weight: 600;
  white-space: nowrap;
}

.merge-item-remark-input,
.merge-item-price-type {
  width: 100%;
}

.merge-item-inline-price {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.merge-item-inline-label,
.merge-item-inline-symbol,
.merge-item-inline-suffix {
  color: #475569;
  white-space: nowrap;
}

.merge-item-inline-price-input {
  width: 124px;
}

.merge-item-inline-price-input :deep(.ant-input-number) {
  width: 100%;
}

.merge-operation-table .col-type {
  width: 72px;
}

.merge-operation-table .col-team-no {
  width: 170px;
}

.merge-operation-table .col-team-name {
  width: auto;
}

.merge-operation-table .col-days {
  width: 72px;
}

.merge-operation-table .col-date {
  width: 96px;
}

.merge-operation-table .col-departure {
  width: 96px;
}

.merge-operation-table .col-seat {
  width: 150px;
}

.merge-operation-table .col-progress {
  width: 82px;
}

.merge-operation-detail-table .col-customer {
  width: 240px;
}

.merge-operation-detail-table .col-remark {
  width: auto;
}

.merge-operation-detail-table .col-people {
  width: 112px;
}

.merge-operation-detail-table .col-price {
  width: 296px;
}

.merge-operation-detail-table .col-price-type {
  width: 160px;
}
</style>
