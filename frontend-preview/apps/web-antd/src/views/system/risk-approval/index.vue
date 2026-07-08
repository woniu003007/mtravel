<script lang="ts" setup>
import type { CustomerRiskApprovalApi } from '#/api/customer/risk-approval';
import type { GuideImprestApi } from '#/api/finance/guide-imprest';

import { Page } from '@vben/common-ui';

import { Alert, Button, Card, Descriptions, Form, Input, Modal, Select, Table, Tag, Textarea, message } from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  approveCustomerRiskApproval,
  getCustomerRiskApprovalDetail,
  getCustomerRiskApprovalPage,
  rejectCustomerRiskApproval,
} from '#/api/customer/risk-approval';
import {
  approveGuideImprest,
  getGuideImprestDetail,
  getGuideImprestPage,
  rejectGuideImprest,
} from '#/api/finance/guide-imprest';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

type ApprovalType = 'customer' | 'guide_imprest';

const approvalType = ref<ApprovalType>('customer');
const loading = ref(false);
const decisionLoading = ref(false);
const detailOpen = ref(false);
const decisionOpen = ref(false);
const decisionType = ref<'approve' | 'reject'>('approve');
const currentRecord = ref<any>();
const rows = ref<any[]>([]);
const total = ref(0);

const query = reactive({
  keyword: '',
  page: 1,
  pageSize: 20,
  status: undefined as string | undefined,
});

const decisionForm = reactive({
  approvalRemark: '',
});

const approvalTypeOptions = [
  { label: '客户风控审批', value: 'customer' },
  { label: '导游备用金审批', value: 'guide_imprest' },
];

const customerStatusOptions = [
  { color: 'blue', label: '待审批', value: 'pending' },
  { color: 'green', label: '已同意', value: 'approved' },
  { color: 'red', label: '已拒绝', value: 'rejected' },
  { color: 'default', label: '已取消', value: 'cancelled' },
];

const guideStatusOptions = [
  { color: 'orange', label: '待总经理审批', value: 'pending_manager' },
  { color: 'blue', label: '总经理已同意', value: 'manager_approved' },
  { color: 'red', label: '总经理已拒绝', value: 'manager_rejected' },
  { color: 'green', label: '已付款', value: 'paid' },
  { color: 'cyan', label: '已结算', value: 'settled' },
  { color: 'default', label: '已取消', value: 'cancelled' },
];

const statusOptions = computed(() => approvalType.value === 'customer' ? customerStatusOptions : guideStatusOptions);
const decisionTitle = computed(() => decisionType.value === 'approve' ? '审批通过' : '审批拒绝');
const detailTitle = computed(() => approvalType.value === 'customer' ? '客户风控审批详情' : '导游备用金审批详情');

function statusLabel(value?: string) {
  return statusOptions.value.find((item) => item.value === value)?.label || value || '--';
}

function statusColor(value?: string) {
  return statusOptions.value.find((item) => item.value === value)?.color || 'default';
}

function riskTypeText(types?: string[]) {
  const labels: Record<string, string> = {
    contract_expired: '合同到期',
    credit_over_limit: '授信超限',
  };
  return (types || []).map((type) => labels[type] || type).join('、') || '--';
}

function lineTypeText(value?: string) {
  if (value === 'cash_cost') return '现付成本';
  if (value === 'optional_deduction') return '自费抵扣';
  return value || '--';
}

function formatAmount(value?: number) {
  return value === undefined || value === null ? '--' : `¥${Number(value).toFixed(2)}`;
}

function formatDateTime(value?: string) {
  if (!value) return '--';
  return value.replace('T', ' ').slice(0, 16);
}

function guideCommissionText(record: GuideImprestApi.CalcLine) {
  if (record.guideCommissionCalcType === 'percent') {
    return `${record.guideCommissionRate || 0}%`;
  }
  return formatAmount(record.guideCommissionAmount);
}

function guideImprestLineFormula(record: GuideImprestApi.CalcLine) {
  if (record.lineType === 'cash_cost') {
    return `现付成本直接计入备用金需求：${formatAmount(record.amount)}`;
  }
  const salePrice = formatAmount(record.salePrice);
  const costPrice = formatAmount(record.costPrice);
  const commission = guideCommissionText(record);
  const rate = `${record.companyMarkupRate ?? 70}%`;
  const guestCount = record.guestCount ?? 0;
  return `（${salePrice} - ${costPrice} - ${commission}）× ${rate} × ${guestCount}人 = ${formatAmount(record.amount)}`;
}

function guideImprestSummaryFormula(record?: GuideImprestApi.Imprest) {
  if (!record) return '--';
  const cashCostAmount = Number(record.cashCostAmount || 0);
  const optionalDeductionAmount = Number(record.optionalDeductionAmount || 0);
  const calculatedAmount = Number(record.calculatedAmount || 0);
  if (calculatedAmount < 0) {
    return `现付总成本 ${formatAmount(cashCostAmount)} - 自费抵扣 ${formatAmount(optionalDeductionAmount)} = -${formatAmount(Math.abs(calculatedAmount))}，建议备用金为 ¥0.00，导游应上交 ${formatAmount(record.guideTurnInAmount)}`;
  }
  return `建议备用金 = 现付总成本 ${formatAmount(cashCostAmount)} - 自费抵扣 ${formatAmount(optionalDeductionAmount)} = ${formatAmount(record.suggestedImprestAmount)}`;
}

function guideImprestChangeMessage(record?: GuideImprestApi.Imprest) {
  return record?.calculationChangeMessage || '团队安排已变化，请计调重新计算备用金';
}

function isPending(record: any) {
  return approvalType.value === 'customer'
    ? record.status === 'pending'
    : record.status === 'pending_manager';
}

async function loadPage() {
  loading.value = true;
  try {
    if (approvalType.value === 'customer') {
      const result = await getCustomerRiskApprovalPage({
        keyword: query.keyword || undefined,
        page: query.page,
        pageSize: query.pageSize,
        status: query.status as CustomerRiskApprovalApi.ApprovalStatus | undefined,
      });
      rows.value = result.items;
      total.value = result.total;
    } else {
      const result = await getGuideImprestPage({
        keyword: query.keyword || undefined,
        page: query.page,
        pageSize: query.pageSize,
        status: query.status as GuideImprestApi.Status | undefined,
      });
      rows.value = result.items;
      total.value = result.total;
    }
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

function changeApprovalType(value: unknown) {
  approvalType.value = value === 'guide_imprest' ? 'guide_imprest' : 'customer';
  query.keyword = '';
  query.status = undefined;
  query.page = 1;
  loadPage();
}

async function openDetail(record: any) {
  currentRecord.value = approvalType.value === 'customer'
    ? await getCustomerRiskApprovalDetail(record.id)
    : await getGuideImprestDetail(record.id);
  detailOpen.value = true;
}

function openDecision(record: any, type: 'approve' | 'reject') {
  if (approvalType.value === 'guide_imprest' && type === 'approve' && record.calculationChanged) {
    message.warning(guideImprestChangeMessage(record));
    return;
  }
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
    if (approvalType.value === 'customer') {
      if (decisionType.value === 'approve') {
        await approveCustomerRiskApproval(record.id, {
          approvalRemark: decisionForm.approvalRemark || '同意',
        });
      } else {
        await rejectCustomerRiskApproval(record.id, {
          approvalRemark: decisionForm.approvalRemark || '拒绝',
        });
      }
    } else if (decisionType.value === 'approve') {
      if (record.calculationChanged) {
        message.warning(guideImprestChangeMessage(record));
        return;
      }
      await approveGuideImprest(record.id, {
        approvalRemark: decisionForm.approvalRemark || '同意',
      });
    } else {
      await rejectGuideImprest(record.id, {
        approvalRemark: decisionForm.approvalRemark || '拒绝',
      });
    }
    message.success(decisionType.value === 'approve' ? '审批通过' : '审批拒绝');
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
        <BusinessSearchForm
          :model="query"
          :search-loading="loading"
          :show-create="false"
          @reset="resetQuery"
          @search="loadPage"
        >
          <Form.Item label="审批类型">
            <Select :value="approvalType" :options="approvalTypeOptions" @change="changeApprovalType" />
          </Form.Item>
          <Form.Item label="关键字">
            <Input
              v-model:value="query.keyword"
              allow-clear
              :placeholder="approvalType === 'customer' ? '申请编号 / 客户名称' : '申请编号 / 团号 / 导游'"
              @press-enter="loadPage"
            />
          </Form.Item>
          <Form.Item label="审批状态">
            <Select v-model:value="query.status" allow-clear :options="statusOptions" placeholder="全部状态" />
          </Form.Item>
        </BusinessSearchForm>
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

          <template v-if="approvalType === 'customer'">
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
          </template>

          <template v-else>
            <Table.Column data-index="teamNo" title="团号" width="150" />
            <Table.Column data-index="guideName" title="导游" width="100" />
            <Table.Column data-index="operatorEmployeeName" title="计调" width="110" />
            <Table.Column title="现付总成本" width="120">
              <template #default="{ record }">{{ formatAmount(record.cashCostAmount) }}</template>
            </Table.Column>
            <Table.Column title="自费抵扣" width="110">
              <template #default="{ record }">{{ formatAmount(record.optionalDeductionAmount) }}</template>
            </Table.Column>
            <Table.Column title="申请金额" width="110">
              <template #default="{ record }">{{ formatAmount(record.requestedAmount) }}</template>
            </Table.Column>
            <Table.Column title="导游应上交" width="120">
              <template #default="{ record }">{{ formatAmount(record.guideTurnInAmount) }}</template>
            </Table.Column>
          </template>

          <Table.Column data-index="applicant" title="申请人" width="100" />
          <Table.Column title="申请时间" width="150">
            <template #default="{ record }">{{ formatDateTime(record.appliedAt || record.createdAt) }}</template>
          </Table.Column>
          <Table.Column title="状态" width="128">
            <template #default="{ record }">
              <Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag>
            </template>
          </Table.Column>
          <Table.Column title="操作" width="190" fixed="right">
            <template #default="{ record }">
              <Button type="link" size="small" @click="openDetail(record)">查看</Button>
              <Button v-if="isPending(record)" type="link" size="small" @click="openDecision(record, 'approve')">同意</Button>
              <Button v-if="isPending(record)" type="link" danger size="small" @click="openDecision(record, 'reject')">拒绝</Button>
            </template>
          </Table.Column>
        </Table>
      </Card>
    </div>

    <Modal v-model:open="detailOpen" :title="detailTitle" width="1100px" :footer="null">
      <Descriptions v-if="currentRecord && approvalType === 'customer'" bordered size="small" :column="2">
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

      <Alert
        v-if="currentRecord && approvalType === 'guide_imprest' && currentRecord.calculationChanged"
        show-icon
        type="warning"
        class="change-alert"
        message="团队安排已变化"
        :description="`${guideImprestChangeMessage(currentRecord)} 总经理不能继续审批通过，需要计调作废旧申请后重新提交。`"
      />

      <Descriptions v-if="currentRecord && approvalType === 'guide_imprest'" bordered size="small" :column="3">
        <Descriptions.Item label="申请编号">{{ currentRecord.requestNo }}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag :color="statusColor(currentRecord.status)">{{ statusLabel(currentRecord.status) }}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="申请人">{{ currentRecord.applicant || '--' }}</Descriptions.Item>
        <Descriptions.Item label="团号">{{ currentRecord.teamNo || '--' }}</Descriptions.Item>
        <Descriptions.Item label="导游">{{ currentRecord.guideName || '--' }}</Descriptions.Item>
        <Descriptions.Item label="计调">{{ currentRecord.operatorEmployeeName || '--' }}</Descriptions.Item>
        <Descriptions.Item label="实收人数">{{ currentRecord.guestCount ?? 0 }}</Descriptions.Item>
        <Descriptions.Item label="现付总成本">{{ formatAmount(currentRecord.cashCostAmount) }}</Descriptions.Item>
        <Descriptions.Item label="自费抵扣">{{ formatAmount(currentRecord.optionalDeductionAmount) }}</Descriptions.Item>
        <Descriptions.Item label="建议备用金">{{ formatAmount(currentRecord.suggestedImprestAmount) }}</Descriptions.Item>
        <Descriptions.Item label="申请金额">{{ formatAmount(currentRecord.requestedAmount) }}</Descriptions.Item>
        <Descriptions.Item label="导游应上交">{{ formatAmount(currentRecord.guideTurnInAmount) }}</Descriptions.Item>
        <Descriptions.Item label="已占用授权">{{ formatAmount(currentRecord.occupiedAuthorizationAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前可申请">{{ formatAmount(currentRecord.availableAuthorizationAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前建议备用金">{{ formatAmount(currentRecord.currentSuggestedImprestAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前实收人数">{{ currentRecord.currentGuestCount ?? currentRecord.guestCount ?? 0 }}</Descriptions.Item>
        <Descriptions.Item label="当前现付总成本">{{ formatAmount(currentRecord.currentCashCostAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前自费抵扣">{{ formatAmount(currentRecord.currentOptionalDeductionAmount) }}</Descriptions.Item>
        <Descriptions.Item label="申请备注" :span="3">{{ currentRecord.remark || '--' }}</Descriptions.Item>
        <Descriptions.Item label="审批意见" :span="3">{{ currentRecord.approvalRemark || '--' }}</Descriptions.Item>
      </Descriptions>

      <template v-if="currentRecord && approvalType === 'guide_imprest'">
        <div class="guide-imprest-section">
          <div class="section-title">计算说明</div>
          <div class="formula-box">{{ guideImprestSummaryFormula(currentRecord) }}</div>
        </div>

        <div class="guide-imprest-section">
          <div class="section-title">费用明细</div>
          <Table
            v-if="currentRecord.calcLines?.length"
            :data-source="currentRecord.calcLines"
            :pagination="false"
            :scroll="{ x: 1060 }"
            row-key="sortOrder"
            size="small"
            class="calc-table"
          >
            <Table.Column title="类型" width="96">
              <template #default="{ record }">
                <Tag :color="record.lineType === 'cash_cost' ? 'red' : 'green'">{{ lineTypeText(record.lineType) }}</Tag>
              </template>
            </Table.Column>
            <Table.Column data-index="itemName" title="项目名称" width="180" />
            <Table.Column title="售价" width="96">
              <template #default="{ record }">{{ formatAmount(record.salePrice) }}</template>
            </Table.Column>
            <Table.Column title="成本" width="96">
              <template #default="{ record }">{{ formatAmount(record.costPrice) }}</template>
            </Table.Column>
            <Table.Column title="导游提成" width="110">
              <template #default="{ record }">{{ guideCommissionText(record) }}</template>
            </Table.Column>
            <Table.Column title="公司加点率" width="110">
              <template #default="{ record }">{{ record.companyMarkupRate ?? 70 }}%</template>
            </Table.Column>
            <Table.Column title="实收人数" width="90">
              <template #default="{ record }">{{ record.guestCount ?? '--' }}</template>
            </Table.Column>
            <Table.Column title="本行金额" width="110">
              <template #default="{ record }">
                <span :class="record.lineType === 'cash_cost' ? 'amount-cost' : 'amount-deduction'">
                  {{ formatAmount(record.amount) }}
                </span>
              </template>
            </Table.Column>
            <Table.Column title="计算公式" width="280">
              <template #default="{ record }">{{ guideImprestLineFormula(record) }}</template>
            </Table.Column>
          </Table>
          <div v-else class="empty-detail">暂无计算明细</div>
        </div>
      </template>

      <div v-if="currentRecord && isPending(currentRecord)" class="detail-actions">
        <Button
          type="primary"
          :disabled="Boolean(currentRecord.calculationChanged)"
          @click="openDecision(currentRecord, 'approve')"
        >
          审批通过
        </Button>
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

.guide-imprest-section {
  margin-top: 14px;
}

.section-title {
  margin-bottom: 8px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
}

.formula-box {
  padding: 10px 12px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.change-alert {
  margin-bottom: 12px;
}

.calc-table :deep(.ant-table-cell) {
  vertical-align: top;
}

.amount-cost {
  color: #dc2626;
  font-weight: 600;
}

.amount-deduction {
  color: #16a34a;
  font-weight: 600;
}

.empty-detail {
  padding: 16px;
  color: #94a3b8;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}
</style>
