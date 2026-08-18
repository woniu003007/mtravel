<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';
import {
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Timeline,
  Tooltip,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  approveCustomerRiskApproval,
  getCustomerRiskApprovalDetail,
  getCustomerRiskApprovalPage,
  rejectCustomerRiskApproval,
  type CustomerRiskApprovalApi,
} from '#/api/customer/risk-approval';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

const viewTabs: Array<{
  key: CustomerRiskApprovalApi.ApprovalView;
  label: string;
}> = [
  { key: 'to_approve', label: '待我审批' },
  { key: 'initiated', label: '我发起的' },
  { key: 'cc', label: '抄送我的' },
];

const statusOptions = [
  { label: '待审批', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已拒绝', value: 'rejected' },
  { label: '已取消', value: 'cancelled' },
];

const columns = [
  { dataIndex: 'requestNo', key: 'requestNo', title: '审批编号', width: 150 },
  {
    dataIndex: 'customerName',
    key: 'customerName',
    title: '客户名称',
    width: 180,
  },
  {
    dataIndex: 'categoryName',
    key: 'categoryName',
    title: '客户等级',
    width: 110,
  },
  {
    align: 'right' as const,
    dataIndex: 'requestedAmount',
    key: 'requestedAmount',
    title: '申请金额',
    width: 125,
  },
  {
    align: 'right' as const,
    dataIndex: 'creditLimit',
    key: 'creditLimit',
    title: '授信额度',
    width: 125,
  },
  {
    align: 'right' as const,
    dataIndex: 'overLimitAmount',
    key: 'overLimitAmount',
    title: '超额金额',
    width: 125,
  },
  { dataIndex: 'applicant', key: 'applicant', title: '申请人', width: 100 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '申请时间', width: 165 },
  {
    dataIndex: 'currentApprovalStep',
    key: 'currentApprovalStep',
    title: '当前环节',
    width: 105,
  },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 190 },
];

const activeView = ref<CustomerRiskApprovalApi.ApprovalView>('to_approve');
const loading = ref(false);
const detailLoading = ref(false);
const decisionLoading = ref(false);
const detailOpen = ref(false);
const decisionOpen = ref(false);
const decisionType = ref<'approve' | 'reject'>('approve');
const rows = ref<CustomerRiskApprovalApi.Approval[]>([]);
const currentRecord = ref<CustomerRiskApprovalApi.Approval>();

const query = reactive<{
  keyword?: string;
  page: number;
  pageSize: number;
  status?: CustomerRiskApprovalApi.ApprovalStatus;
}>({
  page: 1,
  pageSize: 20,
});

const decisionForm = reactive({ approvalRemark: '' });
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 20,
  showSizeChanger: true,
  total: 0,
});

const decisionTitle = computed(() =>
  decisionType.value === 'approve' ? '审批通过' : '审批拒绝',
);

async function loadData() {
  loading.value = true;
  try {
    const result = await getCustomerRiskApprovalPage({
      ...query,
      keyword: query.keyword?.trim() || undefined,
      view: activeView.value,
    });
    rows.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

function changeView(view: string | number) {
  activeView.value = view as CustomerRiskApprovalApi.ApprovalView;
  query.page = 1;
  query.status = undefined;
  loadData();
}

function search() {
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
  query.pageSize = Number(nextPagination.pageSize || 20);
  loadData();
}

async function openDetail(record: Record<string, any>) {
  const approval = record as CustomerRiskApprovalApi.Approval;
  detailOpen.value = true;
  detailLoading.value = true;
  currentRecord.value = approval;
  try {
    currentRecord.value = await getCustomerRiskApprovalDetail(approval.id);
  } finally {
    detailLoading.value = false;
  }
}

function openDecision(record: Record<string, any>, type: 'approve' | 'reject') {
  currentRecord.value = record as CustomerRiskApprovalApi.Approval;
  decisionType.value = type;
  decisionForm.approvalRemark = '';
  decisionOpen.value = true;
}

async function submitDecision() {
  if (!currentRecord.value) return;
  if (decisionType.value === 'reject' && !decisionForm.approvalRemark.trim()) {
    message.warning('请填写拒绝原因');
    return;
  }
  decisionLoading.value = true;
  try {
    const payload = {
      approvalRemark: decisionForm.approvalRemark.trim() || undefined,
    };
    if (decisionType.value === 'approve') {
      await approveCustomerRiskApproval(currentRecord.value.id, payload);
      message.success('审批已通过');
    } else {
      await rejectCustomerRiskApproval(currentRecord.value.id, payload);
      message.success('审批已拒绝');
    }
    decisionOpen.value = false;
    detailOpen.value = false;
    await loadData();
  } finally {
    decisionLoading.value = false;
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

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}

function statusLabel(status?: string) {
  return (
    statusOptions.find((option) => option.value === status)?.label ||
    status ||
    '-'
  );
}

function statusColor(status?: string) {
  return (
    {
      approved: 'green',
      cancelled: 'default',
      pending: 'blue',
      rejected: 'red',
    }[status || ''] || 'default'
  );
}

function stepStatusLabel(status?: string) {
  return (
    {
      approved: '已通过',
      cancelled: '已取消',
      pending: '待审批',
      rejected: '已拒绝',
    }[status || ''] || '未开始'
  );
}

function stepColor(status?: string) {
  return (
    {
      approved: 'green',
      cancelled: 'gray',
      pending: 'blue',
      rejected: 'red',
    }[status || ''] || 'gray'
  );
}

function currentStepText(record: Record<string, any>) {
  if (record.status !== 'pending') return '-';
  const step = record.currentApprovalStep || 1;
  const total = record.approvalSteps?.length;
  return total ? `第 ${step}/${total} 级` : `第 ${step} 级`;
}

function canDecide(record: Record<string, any>) {
  return (
    activeView.value === 'to_approve' &&
    record.status === 'pending' &&
    record.canApprove
  );
}

function ccSummary(record: Record<string, any>) {
  const ccUsers = record.ccUsers as
    | CustomerRiskApprovalApi.CcUser[]
    | undefined;
  if (!ccUsers?.length) return '-';
  return ccUsers
    .map((item) => item.ccName || `用户${item.ccUserId}`)
    .join('、');
}

onMounted(loadData);
</script>

<template>
  <Page title="授信超额审批">
    <Card>
      <Tabs :active-key="activeView" :items="viewTabs" @change="changeView" />

      <BusinessSearchForm
        :model="query"
        :search-loading="loading"
        :show-create="false"
        @reset="resetQuery"
        @search="search"
      >
        <Form.Item class="business-search-item--wide" label="关键字">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="客户名称 / 审批编号"
            @press-enter="search"
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
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1440 }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'requestedAmount'">
            {{ formatMoney(record.requestedAmount) }}
          </template>
          <template v-else-if="column.key === 'creditLimit'">
            {{ formatMoney(record.creditLimit) }}
          </template>
          <template v-else-if="column.key === 'overLimitAmount'">
            <span class="over-limit-amount">{{
              formatMoney(record.overLimitAmount)
            }}</span>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'currentApprovalStep'">
            {{ currentStepText(record) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusColor(record.status)">{{
              statusLabel(record.status)
            }}</Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space :size="2">
              <Button type="link" size="small" @click="openDetail(record)"
                >查看</Button
              >
              <Button
                v-if="canDecide(record)"
                type="link"
                size="small"
                @click="openDecision(record, 'approve')"
              >
                通过
              </Button>
              <Button
                v-if="canDecide(record)"
                type="link"
                danger
                size="small"
                @click="openDecision(record, 'reject')"
              >
                拒绝
              </Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="detailOpen"
      title="授信超额审批详情"
      width="960px"
      :footer="null"
    >
      <div v-if="detailLoading" class="detail-loading">正在加载...</div>
      <template v-else-if="currentRecord">
        <Descriptions bordered size="small" :column="3">
          <Descriptions.Item label="审批编号">{{
            currentRecord.requestNo
          }}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag :color="statusColor(currentRecord.status)">{{
              statusLabel(currentRecord.status)
            }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="当前环节">{{
            currentStepText(currentRecord)
          }}</Descriptions.Item>
          <Descriptions.Item label="客户名称">{{
            currentRecord.customerName || '-'
          }}</Descriptions.Item>
          <Descriptions.Item label="客户等级">{{
            currentRecord.categoryName || '-'
          }}</Descriptions.Item>
          <Descriptions.Item label="账期"
            >{{ currentRecord.creditTermDays ?? 0 }} 天</Descriptions.Item
          >
          <Descriptions.Item label="申请金额">{{
            formatMoney(currentRecord.requestedAmount)
          }}</Descriptions.Item>
          <Descriptions.Item label="授信额度">{{
            formatMoney(currentRecord.creditLimit)
          }}</Descriptions.Item>
          <Descriptions.Item label="超额金额">
            <span class="over-limit-amount">{{
              formatMoney(currentRecord.overLimitAmount)
            }}</span>
          </Descriptions.Item>
          <Descriptions.Item label="已占用额度">{{
            formatMoney(currentRecord.occupiedAmount)
          }}</Descriptions.Item>
          <Descriptions.Item label="审批中额度">{{
            formatMoney(currentRecord.pendingApprovalAmount)
          }}</Descriptions.Item>
          <Descriptions.Item label="可用额度">{{
            formatMoney(currentRecord.availableAmount)
          }}</Descriptions.Item>
          <Descriptions.Item label="申请人">{{
            currentRecord.applicant || '-'
          }}</Descriptions.Item>
          <Descriptions.Item label="申请时间">{{
            formatDateTime(currentRecord.createdAt)
          }}</Descriptions.Item>
          <Descriptions.Item label="抄送人">
            <Tooltip :title="ccSummary(currentRecord)">
              <span>{{ ccSummary(currentRecord) }}</span>
            </Tooltip>
          </Descriptions.Item>
          <Descriptions.Item label="风险摘要" :span="3">
            {{ currentRecord.riskSummary || '-' }}
          </Descriptions.Item>
          <Descriptions.Item label="申请备注" :span="3">
            {{ currentRecord.remark || '-' }}
          </Descriptions.Item>
        </Descriptions>

        <div class="workflow-section">
          <div class="section-title">审批流程</div>
          <Timeline v-if="currentRecord.approvalSteps?.length">
            <Timeline.Item
              v-for="step in currentRecord.approvalSteps"
              :key="step.stepOrder"
              :color="stepColor(step.status)"
            >
              <div class="step-title">
                第 {{ step.stepOrder }} 级 ·
                {{ step.approverName || `用户${step.approverUserId}` }}
                <Tag :color="stepColor(step.status)">{{
                  stepStatusLabel(step.status)
                }}</Tag>
              </div>
              <div
                v-if="step.decidedAt || step.decisionRemark"
                class="step-detail"
              >
                {{ formatDateTime(step.decidedAt) }}
                <span v-if="step.decisionRemark"
                  >：{{ step.decisionRemark }}</span
                >
              </div>
            </Timeline.Item>
          </Timeline>
          <div v-else class="empty-workflow">暂无审批流程</div>
        </div>

        <div v-if="canDecide(currentRecord)" class="detail-actions">
          <Button type="primary" @click="openDecision(currentRecord, 'approve')"
            >审批通过</Button
          >
          <Button danger @click="openDecision(currentRecord, 'reject')"
            >审批拒绝</Button
          >
        </div>
      </template>
    </Modal>

    <Modal
      v-model:open="decisionOpen"
      :title="decisionTitle"
      :confirm-loading="decisionLoading"
      ok-text="确认"
      cancel-text="取消"
      @ok="submitDecision"
    >
      <Form layout="vertical">
        <Form.Item
          :label="decisionType === 'reject' ? '拒绝原因' : '审批意见'"
          :required="decisionType === 'reject'"
        >
          <Input.TextArea
            v-model:value="decisionForm.approvalRemark"
            :maxlength="500"
            :rows="4"
            :placeholder="
              decisionType === 'reject' ? '请填写拒绝原因' : '可填写审批意见'
            "
          />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.over-limit-amount {
  color: #cf1322;
  font-variant-numeric: tabular-nums;
}

.detail-loading,
.empty-workflow {
  padding: 32px 0;
  color: #64748b;
  text-align: center;
}

.workflow-section {
  margin-top: 20px;
}

.section-title {
  margin-bottom: 14px;
  color: #1e293b;
  font-weight: 600;
}

.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-detail {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}
</style>
