<script lang="ts" setup>
import type { CustomerRiskApprovalApi } from '#/api/customer/risk-approval';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, Descriptions, Form, Input, Modal, Select, Space, Table, Tag, Textarea, message } from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  approveCustomerRiskApproval,
  getCustomerRiskApprovalDetail,
  getCustomerRiskApprovalPage,
  rejectCustomerRiskApproval,
} from '#/api/customer/risk-approval';

const loading = ref(false);
const decisionLoading = ref(false);
const detailOpen = ref(false);
const decisionOpen = ref(false);
const decisionType = ref<'approve' | 'reject'>('approve');
const currentRecord = ref<CustomerRiskApprovalApi.Approval>();
const rows = ref<CustomerRiskApprovalApi.Approval[]>([]);
const total = ref(0);

const query = reactive<CustomerRiskApprovalApi.QueryParams>({
  keyword: '',
  page: 1,
  pageSize: 20,
  status: undefined,
});

const decisionForm = reactive({
  approvalRemark: '',
});

const statusOptions = [
  { color: 'blue', label: '待审批', value: 'pending' },
  { color: 'green', label: '已同意', value: 'approved' },
  { color: 'red', label: '已拒绝', value: 'rejected' },
  { color: 'default', label: '已取消', value: 'cancelled' },
];

const decisionTitle = computed(() => decisionType.value === 'approve' ? '审批通过' : '审批拒绝');

function statusLabel(value?: string) {
  return statusOptions.find((item) => item.value === value)?.label || value || '--';
}

function statusColor(value?: string) {
  return statusOptions.find((item) => item.value === value)?.color || 'default';
}

function riskTypeText(types?: string[]) {
  const labels: Record<string, string> = {
    contract_expired: '合同到期',
    credit_over_limit: '授信超限',
  };
  return (types || []).map((type) => labels[type] || type).join('、') || '--';
}

function formatAmount(value?: number) {
  return value === undefined || value === null ? '--' : `¥${Number(value).toFixed(2)}`;
}

function formatDateTime(value?: string) {
  if (!value) return '--';
  return value.replace('T', ' ').slice(0, 16);
}

async function loadPage() {
  loading.value = true;
  try {
    const result = await getCustomerRiskApprovalPage({
      keyword: query.keyword || undefined,
      page: query.page,
      pageSize: query.pageSize,
      status: query.status,
    });
    rows.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = '';
  query.status = undefined;
  query.page = 1;
  loadPage();
}

async function openDetail(record: CustomerRiskApprovalApi.Approval) {
  currentRecord.value = await getCustomerRiskApprovalDetail(record.id);
  detailOpen.value = true;
}

function openDecision(record: CustomerRiskApprovalApi.Approval, type: 'approve' | 'reject') {
  currentRecord.value = record;
  decisionType.value = type;
  decisionForm.approvalRemark = type === 'approve' ? '同意' : '';
  decisionOpen.value = true;
}

async function submitDecision() {
  const record = currentRecord.value;
  if (!record) return;
  decisionLoading.value = true;
  try {
    if (decisionType.value === 'approve') {
      await approveCustomerRiskApproval(record.id, {
        approvalRemark: decisionForm.approvalRemark || '同意',
      });
      message.success('审批通过');
    } else {
      await rejectCustomerRiskApproval(record.id, {
        approvalRemark: decisionForm.approvalRemark || '拒绝',
      });
      message.success('审批拒绝');
    }
    decisionOpen.value = false;
    detailOpen.value = false;
    await loadPage();
  } finally {
    decisionLoading.value = false;
  }
}

function handleTableChange(pagination: { current?: number; pageSize?: number }) {
  query.page = pagination.current || 1;
  query.pageSize = pagination.pageSize || 20;
  loadPage();
}

onMounted(() => {
  loadPage();
});
</script>

<template>
  <Page title="总经理审批">
    <div class="risk-approval-page">
      <Card :bordered="false" class="query-card">
        <Form layout="inline">
          <Form.Item label="关键字">
            <Input v-model:value="query.keyword" allow-clear placeholder="申请编号 / 客户名称" @press-enter="loadPage" />
          </Form.Item>
          <Form.Item label="审批状态">
            <Select v-model:value="query.status" allow-clear :options="statusOptions" placeholder="全部状态" class="status-select" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button @click="resetQuery">重置</Button>
              <Button type="primary" :loading="loading" @click="loadPage">
                <IconifyIcon icon="lucide:search" />
                查询
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card :bordered="false">
        <Table
          :data-source="rows"
          :loading="loading"
          :pagination="{ current: query.page, pageSize: query.pageSize, total, showSizeChanger: true }"
          row-key="id"
          size="small"
          @change="handleTableChange"
        >
          <Table.Column data-index="requestNo" title="申请编号" width="150" />
          <Table.Column data-index="customerName" title="客户名称" />
          <Table.Column title="风险类型" width="160">
            <template #default="{ record }">{{ riskTypeText(record.riskTypes) }}</template>
          </Table.Column>
          <Table.Column data-index="riskSummary" title="风险摘要" />
          <Table.Column title="申请金额" width="120">
            <template #default="{ record }">{{ formatAmount(record.requestedAmount) }}</template>
          </Table.Column>
          <Table.Column title="授信额度" width="120">
            <template #default="{ record }">{{ formatAmount(record.creditLimit) }}</template>
          </Table.Column>
          <Table.Column title="可用额度" width="120">
            <template #default="{ record }">{{ formatAmount(record.availableAmount) }}</template>
          </Table.Column>
          <Table.Column title="超限金额" width="120">
            <template #default="{ record }">{{ formatAmount(record.overLimitAmount) }}</template>
          </Table.Column>
          <Table.Column data-index="applicant" title="申请人" width="100" />
          <Table.Column title="申请时间" width="150">
            <template #default="{ record }">{{ formatDateTime(record.createdAt) }}</template>
          </Table.Column>
          <Table.Column title="状态" width="92">
            <template #default="{ record }">
              <Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag>
            </template>
          </Table.Column>
          <Table.Column title="操作" width="190" fixed="right">
            <template #default="{ record }">
              <Button type="link" size="small" @click="openDetail(record)">查看</Button>
              <Button v-if="record.status === 'pending'" type="link" size="small" @click="openDecision(record, 'approve')">同意</Button>
              <Button v-if="record.status === 'pending'" type="link" danger size="small" @click="openDecision(record, 'reject')">拒绝</Button>
            </template>
          </Table.Column>
        </Table>
      </Card>
    </div>

    <Modal v-model:open="detailOpen" title="客户风控审批详情" width="860px" :footer="null">
      <Descriptions v-if="currentRecord" bordered size="small" :column="2">
        <Descriptions.Item label="申请编号">{{ currentRecord.requestNo }}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag :color="statusColor(currentRecord.status)">{{ statusLabel(currentRecord.status) }}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="客户名称">{{ currentRecord.customerName || '--' }}</Descriptions.Item>
        <Descriptions.Item label="申请金额">{{ formatAmount(currentRecord.requestedAmount) }}</Descriptions.Item>
        <Descriptions.Item label="风险类型">{{ riskTypeText(currentRecord.riskTypes) }}</Descriptions.Item>
        <Descriptions.Item label="合同到期日">{{ currentRecord.contractExpireDate || '--' }}</Descriptions.Item>
        <Descriptions.Item label="授信额度">{{ formatAmount(currentRecord.creditLimit) }}</Descriptions.Item>
        <Descriptions.Item label="已占用额度">{{ formatAmount(currentRecord.occupiedAmount) }}</Descriptions.Item>
        <Descriptions.Item label="审批中额度">{{ formatAmount(currentRecord.pendingApprovalAmount) }}</Descriptions.Item>
        <Descriptions.Item label="可用额度">{{ formatAmount(currentRecord.availableAmount) }}</Descriptions.Item>
        <Descriptions.Item label="超限金额">{{ formatAmount(currentRecord.overLimitAmount) }}</Descriptions.Item>
        <Descriptions.Item label="申请人">{{ currentRecord.applicant || '--' }}</Descriptions.Item>
        <Descriptions.Item label="风险摘要" :span="2">{{ currentRecord.riskSummary || '--' }}</Descriptions.Item>
        <Descriptions.Item label="申请备注" :span="2">{{ currentRecord.remark || '--' }}</Descriptions.Item>
        <Descriptions.Item label="审批意见" :span="2">{{ currentRecord.approvalRemark || '--' }}</Descriptions.Item>
      </Descriptions>
      <div v-if="currentRecord?.status === 'pending'" class="detail-actions">
        <Button type="primary" @click="openDecision(currentRecord, 'approve')">审批通过</Button>
        <Button danger @click="openDecision(currentRecord, 'reject')">审批拒绝</Button>
      </div>
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
        <Form.Item label="审批意见">
          <Textarea v-model:value="decisionForm.approvalRemark" :rows="4" placeholder="填写审批意见" />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.risk-approval-page {
  display: grid;
  gap: 12px;
}

.risk-approval-page :deep(.ant-card) {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 6px 18px rgb(15 23 42 / 4%);
}

.query-card :deep(.ant-form) {
  row-gap: 10px;
}

.status-select {
  width: 160px;
}

.risk-approval-page :deep(.ant-btn) {
  display: inline-flex;
  gap: 5px;
  align-items: center;
}

.detail-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
