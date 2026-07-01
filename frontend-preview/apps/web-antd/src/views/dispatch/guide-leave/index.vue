<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type {
  TableColumnsType,
  TablePaginationConfig,
} from 'ant-design-vue';
import type { DispatchGuideApi } from '#/api/dispatch/guide-schedule';
import type { EnterpriseGuideApi } from '#/api/enterprise/guide';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Select,
  Table,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  approveGuideLeave,
  createGuideLeaveDirect,
  getGuideLeavePage,
  rejectGuideLeave,
} from '#/api/dispatch/guide-schedule';
import { getEnterpriseGuideAll } from '#/api/enterprise/guide';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

type DatePickerValue = Dayjs | string | undefined;
type LeaveRow = DispatchGuideApi.LeaveRecord;

const leaveStatusOptions = [
  { label: '待审批', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已撤回', value: 'withdrawn' },
  { label: '已取消', value: 'cancelled' },
];

const columns: TableColumnsType<LeaveRow> = [
  { title: '导游姓名', dataIndex: 'guideName', key: 'guideName', width: 130 },
  { title: '来源', dataIndex: 'sourceType', key: 'sourceType', width: 120 },
  { title: '请假时间', key: 'leaveTime', width: 260 },
  { title: '请假原因', dataIndex: 'leaveReason', key: 'leaveReason', width: 220 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant', width: 120 },
  { title: '审批信息', key: 'approval', width: 240 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 220 },
  { title: '操作', key: 'action', fixed: 'right', width: 180 },
];

const loading = ref(false);
const saving = ref(false);
const reviewSaving = ref(false);
const directModalOpen = ref(false);
const reviewModalOpen = ref(false);
const reviewMode = ref<'approve' | 'reject'>('approve');
const reviewRow = ref<LeaveRow>();
const guideOptionsLoaded = ref(false);
const guides = ref<EnterpriseGuideApi.Item[]>([]);
const rows = ref<LeaveRow[]>([]);

const queryDates = reactive({
  endDate: undefined as DatePickerValue,
  startDate: undefined as DatePickerValue,
});
const query = reactive<DispatchGuideApi.LeaveQueryParams>({
  page: 1,
  pageSize: 10,
});
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});
const directForm = reactive<DispatchGuideApi.LeaveSaveParams>({
  endAt: '',
  guideId: 0,
  leaveReason: '',
  remark: '',
  startAt: '',
});
const directDates = reactive({
  endAt: undefined as DatePickerValue,
  startAt: undefined as DatePickerValue,
});
const reviewForm = reactive({
  approvalRemark: '',
});

const guideSelectOptions = computed(() =>
  guides.value.map((item) => ({
    label: item.mobilePhone
      ? `${item.guideName}（${item.mobilePhone}）`
      : item.guideName,
    value: item.id,
  })),
);

function formatBackendDateTime(value: DatePickerValue | null) {
  return value ? dayjs(value).format('YYYY-MM-DDTHH:mm:ss') : '';
}

function formatDate(value: DatePickerValue) {
  return value ? dayjs(value).format('YYYY-MM-DD') : undefined;
}

function formatDisplayDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--';
}

function leaveStatusLabel(value?: string) {
  return leaveStatusOptions.find((item) => item.value === value)?.label || value || '--';
}

function leaveStatusColor(value?: string) {
  const colors: Record<string, string> = {
    approved: 'green',
    cancelled: 'default',
    pending: 'orange',
    rejected: 'red',
    withdrawn: 'default',
  };
  return colors[value || ''] || 'default';
}

function sourceTypeLabel(value?: string) {
  if (value === 'dispatcher_direct') return '计调设置';
  if (value === 'guide_apply') return '导游申请';
  return value || '--';
}

function syncQueryDates() {
  query.startDate = formatDate(queryDates.startDate);
  query.endDate = formatDate(queryDates.endDate);
}

function resetDirectForm() {
  Object.assign(directForm, {
    endAt: '',
    guideId: 0,
    leaveReason: '',
    remark: '',
    startAt: '',
  });
  directDates.startAt = undefined;
  directDates.endAt = undefined;
}

async function loadGuideOptions() {
  if (guideOptionsLoaded.value) return;
  guides.value = await getEnterpriseGuideAll(false);
  guideOptionsLoaded.value = true;
}

async function loadRows() {
  syncQueryDates();
  loading.value = true;
  try {
    const result = await getGuideLeavePage(query);
    rows.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  loadRows();
}

function resetQuery() {
  Object.assign(query, {
    endDate: undefined,
    guideName: undefined,
    page: 1,
    pageSize: query.pageSize,
    startDate: undefined,
    status: undefined,
  });
  queryDates.startDate = undefined;
  queryDates.endDate = undefined;
  loadRows();
}

function handleTableChange(pageConfig: TablePaginationConfig) {
  query.page = pageConfig.current || 1;
  query.pageSize = pageConfig.pageSize || query.pageSize;
  loadRows();
}

async function openDirectModal() {
  resetDirectForm();
  await loadGuideOptions();
  directModalOpen.value = true;
}

async function saveDirectLeave() {
  if (!directForm.guideId) {
    message.warning('请选择导游');
    return;
  }
  directForm.startAt = formatBackendDateTime(directDates.startAt);
  directForm.endAt = formatBackendDateTime(directDates.endAt);
  if (!directForm.startAt || !directForm.endAt) {
    message.warning('请填写请假开始和结束时间');
    return;
  }
  if (!directForm.leaveReason?.trim()) {
    message.warning('请填写请假原因');
    return;
  }
  saving.value = true;
  try {
    await createGuideLeaveDirect({
      ...directForm,
      leaveReason: directForm.leaveReason.trim(),
      remark: directForm.remark?.trim(),
    });
    message.success('导游不可上团时间已设置');
    directModalOpen.value = false;
    await loadRows();
  } finally {
    saving.value = false;
  }
}

function openReviewModal(row: LeaveRow, mode: 'approve' | 'reject') {
  reviewRow.value = row;
  reviewMode.value = mode;
  reviewForm.approvalRemark = '';
  reviewModalOpen.value = true;
}

function asLeaveRow(record: Record<string, any>) {
  return record as LeaveRow;
}

async function submitReview() {
  if (!reviewRow.value) return;
  reviewSaving.value = true;
  try {
    if (reviewMode.value === 'approve') {
      await approveGuideLeave(reviewRow.value.id, reviewForm.approvalRemark?.trim());
      message.success('请假申请已通过');
    } else {
      await rejectGuideLeave(reviewRow.value.id, reviewForm.approvalRemark?.trim());
      message.success('请假申请已驳回');
    }
    reviewModalOpen.value = false;
    await loadRows();
  } finally {
    reviewSaving.value = false;
  }
}

onMounted(loadRows);
</script>

<template>
  <Page title="导游请假管理" description="审批导游请假，并设置导游不可上团时间">
    <Card>
      <BusinessSearchForm
        create-text="新增"
        :model="query"
        :search-loading="loading"
        @create="openDirectModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="导游名称">
          <Input
            v-model:value="query.guideName"
            allow-clear
            placeholder="导游名称"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select
            v-model:value="query.status"
            allow-clear
            placeholder="全部状态"
            :options="leaveStatusOptions"
          />
        </Form.Item>
        <Form.Item label="开始日期">
          <DatePicker
            v-model:value="queryDates.startDate"
            class="w-full"
            value-format="YYYY-MM-DD"
          />
        </Form.Item>
        <Form.Item label="结束日期">
          <DatePicker
            v-model:value="queryDates.endDate"
            class="w-full"
            value-format="YYYY-MM-DD"
          />
        </Form.Item>
        <template #extraActions>
          <Button type="primary" @click="openDirectModal">直接设置请假</Button>
        </template>
      </BusinessSearchForm>

      <Table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1470 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sourceType'">
            {{ sourceTypeLabel(record.sourceType) }}
          </template>
          <template v-else-if="column.key === 'leaveTime'">
            {{ formatDisplayDateTime(record.startAt) }} 至 {{ formatDisplayDateTime(record.endAt) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="leaveStatusColor(record.status)">
              {{ leaveStatusLabel(record.status) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'approval'">
            <div class="approval-info">
              <span v-if="record.approvedBy">通过：{{ record.approvedBy }} {{ formatDisplayDateTime(record.approvedAt) }}</span>
              <span v-else-if="record.rejectedBy">驳回：{{ record.rejectedBy }} {{ formatDisplayDateTime(record.rejectedAt) }}</span>
              <span v-else>待审批</span>
              <small v-if="record.approvalRemark">{{ record.approvalRemark }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <Button
              v-if="record.status === 'pending'"
              size="small"
              type="link"
              @click="openReviewModal(asLeaveRow(record), 'approve')"
            >
              审批通过
            </Button>
            <Button
              v-if="record.status === 'pending'"
              danger
              size="small"
              type="link"
              @click="openReviewModal(asLeaveRow(record), 'reject')"
            >
              驳回申请
            </Button>
            <span v-if="record.status !== 'pending'" class="muted-text">无</span>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="directModalOpen"
      destroy-on-close
      title="直接设置请假"
      :confirm-loading="saving"
      @ok="saveDirectLeave"
    >
      <Form layout="vertical" :model="directForm">
        <Form.Item label="导游" required>
          <Select
            v-model:value="directForm.guideId"
            show-search
            :filter-option="false"
            placeholder="选择导游"
            :options="guideSelectOptions"
          />
        </Form.Item>
        <Form.Item label="开始时间" required>
          <DatePicker
            v-model:value="directDates.startAt"
            class="w-full"
            format="YYYY-MM-DD HH:mm"
            show-time
          />
        </Form.Item>
        <Form.Item label="结束时间" required>
          <DatePicker
            v-model:value="directDates.endAt"
            class="w-full"
            format="YYYY-MM-DD HH:mm"
            show-time
          />
        </Form.Item>
        <Form.Item label="请假原因" required>
          <Input v-model:value="directForm.leaveReason" :maxlength="300" />
        </Form.Item>
        <Form.Item label="备注">
          <Textarea v-model:value="directForm.remark" :rows="3" :maxlength="1000" />
        </Form.Item>
      </Form>
    </Modal>

    <Modal
      v-model:open="reviewModalOpen"
      destroy-on-close
      :title="reviewMode === 'approve' ? '审批通过' : '驳回申请'"
      :confirm-loading="reviewSaving"
      @ok="submitReview"
    >
      <Form layout="vertical" :model="reviewForm">
        <Form.Item label="审批意见">
          <Textarea
            v-model:value="reviewForm.approvalRemark"
            :rows="4"
            :maxlength="1000"
            placeholder="填写审批意见"
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.w-full {
  width: 100%;
}

.approval-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.approval-info small,
.muted-text {
  color: #64748b;
}
</style>
