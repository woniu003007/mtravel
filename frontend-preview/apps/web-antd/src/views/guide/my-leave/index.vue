<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { TableColumnsType } from 'ant-design-vue';
import type { DispatchGuideApi } from '#/api/dispatch/guide-schedule';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Table,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { onMounted, reactive, ref } from 'vue';

import {
  getMyGuideLeaves,
  submitMyGuideLeave,
  withdrawMyGuideLeave,
} from '#/api/dispatch/guide-schedule';

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
  { title: '请假时间', key: 'leaveTime', width: 260 },
  { title: '请假原因', dataIndex: 'leaveReason', key: 'leaveReason', width: 220 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '审批信息', key: 'approval', width: 260 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 240 },
  { title: '操作', key: 'action', fixed: 'right', width: 120 },
];

const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const rows = ref<LeaveRow[]>([]);
const form = reactive<DispatchGuideApi.SelfLeaveSaveParams>({
  endAt: '',
  leaveReason: '',
  remark: '',
  startAt: '',
});
const leaveDates = reactive({
  endAt: undefined as DatePickerValue,
  startAt: undefined as DatePickerValue,
});

function formatBackendDateTime(value: DatePickerValue | null) {
  return value ? dayjs(value).format('YYYY-MM-DDTHH:mm:ss') : '';
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

function resetForm() {
  Object.assign(form, {
    endAt: '',
    leaveReason: '',
    remark: '',
    startAt: '',
  });
  leaveDates.startAt = undefined;
  leaveDates.endAt = undefined;
}

async function loadRows() {
  loading.value = true;
  try {
    rows.value = await getMyGuideLeaves();
  } finally {
    loading.value = false;
  }
}

function openCreateModal() {
  resetForm();
  modalOpen.value = true;
}

async function saveLeave() {
  form.startAt = formatBackendDateTime(leaveDates.startAt);
  form.endAt = formatBackendDateTime(leaveDates.endAt);
  if (!form.startAt || !form.endAt) {
    message.warning('请填写请假开始和结束时间');
    return;
  }
  if (!form.leaveReason?.trim()) {
    message.warning('请填写请假原因');
    return;
  }
  saving.value = true;
  try {
    await submitMyGuideLeave({
      ...form,
      leaveReason: form.leaveReason.trim(),
      remark: form.remark?.trim(),
    });
    message.success('请假申请已提交');
    modalOpen.value = false;
    await loadRows();
  } finally {
    saving.value = false;
  }
}

async function withdrawLeave(row: LeaveRow) {
  await withdrawMyGuideLeave(row.id);
  message.success('请假申请已撤回');
  await loadRows();
}

function asLeaveRow(record: Record<string, any>) {
  return record as LeaveRow;
}

onMounted(loadRows);
</script>

<template>
  <Page title="我的请假" description="导游提交请假申请并查看审批结果">
    <Card>
      <div class="table-toolbar">
        <Button type="primary" @click="openCreateModal">申请请假</Button>
      </div>

      <Table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'leaveTime'">
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
              danger
              size="small"
              type="link"
              @click="withdrawLeave(asLeaveRow(record))"
            >
              撤回
            </Button>
            <span v-else class="muted-text">无</span>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalOpen"
      destroy-on-close
      title="申请请假"
      :confirm-loading="saving"
      @ok="saveLeave"
    >
      <Form layout="vertical" :model="form">
        <Form.Item label="开始时间" required>
          <DatePicker
            v-model:value="leaveDates.startAt"
            class="w-full"
            format="YYYY-MM-DD HH:mm"
            show-time
          />
        </Form.Item>
        <Form.Item label="结束时间" required>
          <DatePicker
            v-model:value="leaveDates.endAt"
            class="w-full"
            format="YYYY-MM-DD HH:mm"
            show-time
          />
        </Form.Item>
        <Form.Item label="请假原因" required>
          <Input v-model:value="form.leaveReason" :maxlength="300" />
        </Form.Item>
        <Form.Item label="备注">
          <Textarea v-model:value="form.remark" :rows="3" :maxlength="1000" />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

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
