<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { DispatchGuideApi } from '#/api/dispatch/guide-schedule';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Card, DatePicker, Empty, Form, Input, Spin, Tag, message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getGuideScheduleCalendar } from '#/api/dispatch/guide-schedule';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

const router = useRouter();
const loading = ref(false);
const calendar = ref<DispatchGuideApi.CalendarResponse>();
const query = reactive({
  guideName: '',
  startDate: dayjs(),
});

const dates = computed(() => calendar.value?.dates || []);
const rows = computed(() => calendar.value?.rows || []);

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('MM-DD HH:mm') : '--';
}

function formatTime(value?: string) {
  return value ? dayjs(value).format('HH:mm') : '--';
}

function dateValue(date: string) {
  return dayjs(date).startOf('day');
}

function isBlockOnDate(block: DispatchGuideApi.ScheduleBlock, date: string) {
  const dayStart = dateValue(date);
  const dayEnd = dayStart.add(1, 'day');
  return dayjs(block.startAt).isBefore(dayEnd) && dayjs(block.endAt).isAfter(dayStart);
}

function blocksForDate(row: DispatchGuideApi.GuideRow, date: string) {
  return row.blocks.filter((block) => isBlockOnDate(block, date));
}

function blockLabel(block: DispatchGuideApi.ScheduleBlock) {
  if (block.sourceType === 'leave') {
    return block.description || '请假';
  }
  return `${formatTime(block.startAt)}上团 ${block.teamNo || ''} ${formatTime(block.endAt)}下团`;
}

function blockTooltip(block: DispatchGuideApi.ScheduleBlock) {
  if (block.sourceType === 'leave') {
    return `请假：${formatDateTime(block.startAt)} 至 ${formatDateTime(block.endAt)}；${block.description || ''}`;
  }
  return `团队：${block.teamNo || ''}；${formatDateTime(block.startAt)} 至 ${formatDateTime(block.endAt)}`;
}

async function loadCalendar() {
  loading.value = true;
  try {
    calendar.value = await getGuideScheduleCalendar({
      guideName: query.guideName || undefined,
      startDate: query.startDate?.format('YYYY-MM-DD'),
    });
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.guideName = '';
  query.startDate = dayjs();
  loadCalendar();
}

function handleDateChange(value: Dayjs | string) {
  query.startDate = dayjs(value);
}

function openBlock(block: DispatchGuideApi.ScheduleBlock) {
  if (block.sourceType !== 'team' || !block.teamId) {
    message.info(blockTooltip(block));
    return;
  }
  router.push({ hash: '#guide-arrangement', path: `/sales/team/arrangement/${block.teamId}` });
}

onMounted(loadCalendar);
</script>

<template>
  <Page title="导游排班汇总" description="查看导游团队占用和请假不可上团时间">
    <Card class="guide-schedule-card">
      <BusinessSearchForm
        :model="query"
        search-text="搜索"
        :search-loading="loading"
        :show-create="false"
        @reset="resetQuery"
        @search="loadCalendar"
      >
        <Form.Item label="导游">
          <Input
            v-model:value="query.guideName"
            allow-clear
            placeholder="导游名称"
            @press-enter="loadCalendar"
          />
        </Form.Item>
        <Form.Item label="开始日期">
          <DatePicker
            :value="query.startDate"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </Form.Item>
      </BusinessSearchForm>
      <div class="schedule-range-tip">统计区间为开始日期后一个月，并额外显示后一周跨期占用</div>

      <Spin :spinning="loading">
        <div v-if="rows.length" class="schedule-table-wrap">
          <table class="schedule-table">
            <thead>
              <tr>
                <th class="guide-name-col">导游</th>
                <th v-for="date in dates" :key="date.date" class="date-col">
                  <strong>{{ date.label }}</strong>
                  <span>[{{ date.weekLabel }}]</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="row.guideId">
                <th class="guide-name-col">
                  <div class="guide-name-main">{{ row.guideName }}</div>
                  <div class="guide-mobile">{{ row.guideMobile || '--' }}</div>
                </th>
                <td v-for="date in dates" :key="`${row.guideId}-${date.date}`" class="schedule-cell">
                  <span v-if="blocksForDate(row, date.date).length === 0" class="schedule-free-marker">空闲</span>
                  <button
                    v-for="block in blocksForDate(row, date.date)"
                    :key="`${block.sourceType}-${block.sourceId}-${date.date}`"
                    type="button"
                    class="schedule-block"
                    :class="block.sourceType"
                    :title="blockTooltip(block)"
                    @click="openBlock(block)"
                  >
                    <IconifyIcon :icon="block.sourceType === 'leave' ? 'lucide:calendar-x' : 'lucide:flag'" />
                    <span>{{ blockLabel(block) }}</span>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Empty v-else description="暂无导游排班数据" />
      </Spin>

      <div class="schedule-legend">
        <Tag color="blue">团队上团</Tag>
        <Tag color="red">请假 / 不可上团</Tag>
        <Tag>空闲可派</Tag>
      </div>
    </Card>
  </Page>
</template>

<style scoped>
.guide-schedule-card {
  border-radius: 6px;
}

.schedule-range-tip {
  margin: -2px 0 10px;
  font-size: 13px;
  color: #64748b;
}

.schedule-table-wrap {
  width: 100%;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.schedule-table {
  min-width: 2600px;
  border-collapse: collapse;
  background: #fff;
}

.schedule-table th,
.schedule-table td {
  height: 54px;
  padding: 5px;
  font-size: 12px;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
}

.schedule-table thead th {
  position: sticky;
  top: 0;
  z-index: 2;
  color: #475569;
  text-align: center;
  background: #f8fafc;
}

.guide-name-col {
  position: sticky;
  left: 0;
  z-index: 3;
  width: 124px;
  min-width: 124px;
  text-align: left;
  background: #fff;
}

thead .guide-name-col {
  background: #f8fafc;
}

.date-col {
  width: 78px;
  min-width: 78px;
}

.date-col strong {
  display: block;
  font-size: 13px;
  color: #1f2937;
}

.date-col span {
  font-size: 11px;
  color: #64748b;
}

.guide-name-main {
  font-weight: 700;
  color: #1f2937;
}

.guide-mobile {
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}

.schedule-cell {
  vertical-align: top;
  background: #fff;
}

.schedule-free-marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 22px;
  font-size: 11px;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 4px;
}

.schedule-block {
  display: flex;
  gap: 3px;
  align-items: center;
  width: 100%;
  min-height: 24px;
  padding: 2px 4px;
  margin-bottom: 3px;
  overflow: hidden;
  font-size: 11px;
  line-height: 1.2;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 4px;
}

.schedule-block svg {
  flex: 0 0 auto;
  width: 12px;
  height: 12px;
}

.schedule-block.team {
  color: #1554ad;
  background: #eaf4ff;
  border-color: #bfdbfe;
}

.schedule-block.leave {
  color: #b42318;
  background: #fff1f0;
  border-color: #ffccc7;
}

.schedule-block:hover {
  filter: brightness(0.98);
}

.schedule-legend {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 12px;
}
</style>
