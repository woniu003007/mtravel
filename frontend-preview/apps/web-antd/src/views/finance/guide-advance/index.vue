<script lang="ts" setup>
import type { GuideImprestApi } from '#/api/finance/guide-imprest';

import { Page } from '@vben/common-ui';

import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Table,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  cancelGuideImprest,
  getGuideImprestDetail,
  getGuideImprestPage,
  previewGuideImprest,
  registerGuideImprestPayment,
  submitGuideImprest,
} from '#/api/finance/guide-imprest';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

const loading = ref(false);
const previewLoading = ref(false);
const submitLoading = ref(false);
const paymentLoading = ref(false);
const cancelLoading = ref(false);
const applyOpen = ref(false);
const detailOpen = ref(false);
const paymentOpen = ref(false);
const cancelOpen = ref(false);
const rows = ref<GuideImprestApi.Imprest[]>([]);
const total = ref(0);
const currentRecord = ref<GuideImprestApi.Imprest>();
const preview = ref<GuideImprestApi.Preview>();

const query = reactive<GuideImprestApi.QueryParams>({
  keyword: '',
  page: 1,
  pageSize: 20,
  status: undefined,
});

const applyForm = reactive({
  guideId: undefined as number | undefined,
  remark: '',
  requestedAmount: undefined as number | undefined,
  teamId: undefined as number | undefined,
});

const paymentForm = reactive({
  amount: undefined as number | undefined,
  paymentAccountName: '',
  paymentDate: '',
  paymentMethod: '',
  remark: '',
});

const cancelForm = reactive({
  cancelReason: '',
});

const statusOptions = [
  { color: 'default', label: '草稿', value: 'draft' },
  { color: 'orange', label: '待总经理审批', value: 'pending_manager' },
  { color: 'blue', label: '总经理已同意', value: 'manager_approved' },
  { color: 'red', label: '总经理已拒绝', value: 'manager_rejected' },
  { color: 'green', label: '已付款', value: 'paid' },
  { color: 'cyan', label: '已结算', value: 'settled' },
  { color: 'default', label: '已取消', value: 'cancelled' },
];

const canSubmitApply = computed(() => Boolean(applyForm.teamId && applyForm.guideId));

const currentChangeMessage = computed(() => {
  if (!currentRecord.value?.calculationChanged) return '';
  return currentRecord.value.calculationChangeMessage || '团队安排已变化，请作废旧申请并重新提交';
});

function statusLabel(value?: string) {
  return statusOptions.find((item) => item.value === value)?.label || value || '--';
}

function statusColor(value?: string) {
  return statusOptions.find((item) => item.value === value)?.color || 'default';
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

function canCancel(record?: GuideImprestApi.Imprest) {
  if (!record) return false;
  if (Number(record.paidAmount || 0) > 0) return false;
  return record.status === 'pending_manager' || record.status === 'manager_approved';
}

async function loadPage() {
  loading.value = true;
  try {
    const result = await getGuideImprestPage({
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

function openApply() {
  applyForm.teamId = undefined;
  applyForm.guideId = undefined;
  applyForm.requestedAmount = undefined;
  applyForm.remark = '';
  preview.value = undefined;
  applyOpen.value = true;
}

async function loadPreview() {
  if (!applyForm.teamId || !applyForm.guideId) {
    message.warning('请先填写团队ID和导游ID');
    return;
  }
  previewLoading.value = true;
  try {
    preview.value = await previewGuideImprest({
      guideId: applyForm.guideId,
      teamId: applyForm.teamId,
    });
    applyForm.requestedAmount = Number(preview.value.availableAuthorizationAmount || 0);
  } finally {
    previewLoading.value = false;
  }
}

async function submitApply() {
  if (!applyForm.teamId || !applyForm.guideId) {
    message.warning('请先填写团队ID和导游ID');
    return;
  }
  submitLoading.value = true;
  try {
    await submitGuideImprest({
      guideId: applyForm.guideId,
      remark: applyForm.remark || undefined,
      requestedAmount: Number(applyForm.requestedAmount || 0),
      teamId: applyForm.teamId,
    });
    message.success('备用金申请已提交');
    applyOpen.value = false;
    await loadPage();
  } finally {
    submitLoading.value = false;
  }
}

async function openDetail(record: GuideImprestApi.Imprest) {
  currentRecord.value = await getGuideImprestDetail(record.id);
  detailOpen.value = true;
}

async function openPayment(record: GuideImprestApi.Imprest) {
  const detail = await getGuideImprestDetail(record.id);
  currentRecord.value = detail;
  paymentForm.amount = Number(
    detail.balanceAmount ||
      detail.approvedAmount ||
      detail.requestedAmount ||
      0,
  );
  paymentForm.paymentDate = new Date().toISOString().slice(0, 10);
  paymentForm.paymentMethod = 'bank';
  paymentForm.paymentAccountName = '';
  paymentForm.remark = '';
  paymentOpen.value = true;
}

async function submitPayment() {
  if (!currentRecord.value || !paymentForm.amount || !paymentForm.paymentDate) {
    message.warning('请填写付款金额和付款日期');
    return;
  }
  if (currentRecord.value.calculationChanged) {
    message.warning('团队安排已变化，请作废旧申请并重新提交');
    return;
  }
  paymentLoading.value = true;
  try {
    await registerGuideImprestPayment(currentRecord.value.id, {
      amount: Number(paymentForm.amount),
      paymentAccountName: paymentForm.paymentAccountName || undefined,
      paymentDate: paymentForm.paymentDate,
      paymentMethod: paymentForm.paymentMethod || undefined,
      remark: paymentForm.remark || undefined,
    });
    message.success('付款已登记');
    paymentOpen.value = false;
    detailOpen.value = false;
    await loadPage();
  } finally {
    paymentLoading.value = false;
  }
}

async function openCancel(record: GuideImprestApi.Imprest) {
  const detail = await getGuideImprestDetail(record.id);
  currentRecord.value = detail;
  cancelForm.cancelReason = detail.calculationChanged
    ? '团队安排已变化，作废旧备用金申请'
    : '';
  cancelOpen.value = true;
}

async function submitCancel() {
  if (!currentRecord.value || !cancelForm.cancelReason.trim()) {
    message.warning('请填写作废原因');
    return;
  }
  cancelLoading.value = true;
  try {
    await cancelGuideImprest(currentRecord.value.id, {
      cancelReason: cancelForm.cancelReason.trim(),
    });
    message.success('备用金申请已作废');
    cancelOpen.value = false;
    detailOpen.value = false;
    paymentOpen.value = false;
    await loadPage();
  } finally {
    cancelLoading.value = false;
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
  <Page title="导游备用金">
    <div class="guide-imprest-page">
      <Card :bordered="false">
        <BusinessSearchForm
          :model="query"
          :search-loading="loading"
          create-text="新增"
          @create="openApply"
          @reset="resetQuery"
          @search="loadPage"
        >
          <Form.Item label="关键字">
            <Input v-model:value="query.keyword" allow-clear placeholder="申请编号 / 团号 / 导游" @press-enter="loadPage" />
          </Form.Item>
          <Form.Item label="状态">
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
          <Table.Column data-index="teamNo" title="团号" width="150" />
          <Table.Column data-index="guideName" title="导游" width="110" />
          <Table.Column data-index="operatorEmployeeName" title="计调" width="110" />
          <Table.Column title="合计现付" width="110">
            <template #default="{ record }">{{ formatAmount(record.cashCostAmount) }}</template>
          </Table.Column>
          <Table.Column title="自费抵扣" width="110">
            <template #default="{ record }">{{ formatAmount(record.optionalDeductionAmount) }}</template>
          </Table.Column>
          <Table.Column title="申请金额" width="110">
            <template #default="{ record }">{{ formatAmount(record.requestedAmount) }}</template>
          </Table.Column>
          <Table.Column title="已付" width="100">
            <template #default="{ record }">{{ formatAmount(record.paidAmount) }}</template>
          </Table.Column>
          <Table.Column title="余额" width="100">
            <template #default="{ record }">{{ formatAmount(record.balanceAmount) }}</template>
          </Table.Column>
          <Table.Column title="状态" width="128">
            <template #default="{ record }">
              <Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag>
            </template>
          </Table.Column>
          <Table.Column title="申请时间" width="150">
            <template #default="{ record }">{{ formatDateTime(record.appliedAt || record.createdAt) }}</template>
          </Table.Column>
          <Table.Column title="操作" width="150" fixed="right">
            <template #default="{ record }">
              <Button type="link" size="small" @click="openDetail(record)">查看</Button>
              <Button v-if="record.status === 'manager_approved'" type="link" size="small" @click="openPayment(record)">付款</Button>
              <Button v-if="canCancel(record)" type="link" danger size="small" @click="openCancel(record)">作废</Button>
            </template>
          </Table.Column>
        </Table>
      </Card>
    </div>

    <Modal
      v-model:open="applyOpen"
      title="新增导游备用金申请"
      width="960px"
      :confirm-loading="submitLoading"
      ok-text="提交申请"
      cancel-text="取消"
      :ok-button-props="{ disabled: !canSubmitApply }"
      @ok="submitApply"
    >
      <Form layout="vertical">
        <div class="form-grid">
          <Form.Item label="团队ID">
            <InputNumber v-model:value="applyForm.teamId" :min="1" class="full-width" />
          </Form.Item>
          <Form.Item label="导游ID">
            <InputNumber v-model:value="applyForm.guideId" :min="1" class="full-width" />
          </Form.Item>
          <Form.Item label="申请金额">
            <InputNumber v-model:value="applyForm.requestedAmount" :min="0" :precision="2" class="full-width" />
          </Form.Item>
          <Form.Item label="操作">
            <Button :loading="previewLoading" @click="loadPreview">计算预览</Button>
          </Form.Item>
        </div>
        <Form.Item label="申请备注">
          <Textarea v-model:value="applyForm.remark" :rows="3" placeholder="填写备用金申请说明" />
        </Form.Item>
      </Form>

      <Descriptions v-if="preview" bordered size="small" :column="3" class="preview-summary">
        <Descriptions.Item label="团号">{{ preview.teamNo || '--' }}</Descriptions.Item>
        <Descriptions.Item label="导游">{{ preview.guideName || '--' }}</Descriptions.Item>
        <Descriptions.Item label="实收人数">{{ preview.guestCount ?? 0 }}</Descriptions.Item>
        <Descriptions.Item label="现付总成本">{{ formatAmount(preview.cashCostAmount) }}</Descriptions.Item>
        <Descriptions.Item label="自费抵扣">{{ formatAmount(preview.optionalDeductionAmount) }}</Descriptions.Item>
        <Descriptions.Item label="计算结果">{{ formatAmount(preview.calculatedAmount) }}</Descriptions.Item>
        <Descriptions.Item label="建议备用金">{{ formatAmount(preview.suggestedImprestAmount) }}</Descriptions.Item>
        <Descriptions.Item label="导游应上交">{{ formatAmount(preview.guideTurnInAmount) }}</Descriptions.Item>
        <Descriptions.Item label="公司加点率">{{ preview.companyMarkupRate ?? 70 }}%</Descriptions.Item>
        <Descriptions.Item label="已占用授权">{{ formatAmount(preview.occupiedAuthorizationAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前可申请">{{ formatAmount(preview.availableAuthorizationAmount) }}</Descriptions.Item>
        <Descriptions.Item label="申请规则">本次申请不能超过当前可申请金额</Descriptions.Item>
      </Descriptions>

      <Table
        v-if="preview"
        :data-source="preview.calcLines"
        :pagination="false"
        row-key="sortOrder"
        size="small"
        class="calc-table"
      >
        <Table.Column title="类型" width="100">
          <template #default="{ record }">{{ lineTypeText(record.lineType) }}</template>
        </Table.Column>
        <Table.Column data-index="itemName" title="项目" />
        <Table.Column title="售价" width="90">
          <template #default="{ record }">{{ formatAmount(record.salePrice) }}</template>
        </Table.Column>
        <Table.Column title="成本" width="90">
          <template #default="{ record }">{{ formatAmount(record.costPrice) }}</template>
        </Table.Column>
        <Table.Column title="导游提成" width="110">
          <template #default="{ record }">
            <span v-if="record.guideCommissionCalcType === 'percent'">{{ record.guideCommissionRate || 0 }}%</span>
            <span v-else>{{ formatAmount(record.guideCommissionAmount) }}</span>
          </template>
        </Table.Column>
        <Table.Column title="人数" width="70">
          <template #default="{ record }">{{ record.guestCount ?? '--' }}</template>
        </Table.Column>
        <Table.Column title="金额" width="110">
          <template #default="{ record }">{{ formatAmount(record.amount) }}</template>
        </Table.Column>
      </Table>
    </Modal>

    <Modal v-model:open="detailOpen" title="导游备用金详情" width="960px" :footer="null">
      <Alert
        v-if="currentRecord?.calculationChanged"
        show-icon
        type="warning"
        class="change-alert"
        :message="currentChangeMessage"
        description="当前团队安排或实收人数已和申请快照不一致。请作废旧申请，重新计算并提交新的备用金申请。"
      />
      <Descriptions v-if="currentRecord" bordered size="small" :column="3">
        <Descriptions.Item label="申请编号">{{ currentRecord.requestNo }}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag :color="statusColor(currentRecord.status)">{{ statusLabel(currentRecord.status) }}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="申请人">{{ currentRecord.applicant || '--' }}</Descriptions.Item>
        <Descriptions.Item label="团号">{{ currentRecord.teamNo || '--' }}</Descriptions.Item>
        <Descriptions.Item label="导游">{{ currentRecord.guideName || '--' }}</Descriptions.Item>
        <Descriptions.Item label="计调">{{ currentRecord.operatorEmployeeName || '--' }}</Descriptions.Item>
        <Descriptions.Item label="实收人数">{{ currentRecord.guestCount ?? 0 }}</Descriptions.Item>
        <Descriptions.Item label="合计现付">{{ formatAmount(currentRecord.cashCostAmount) }}</Descriptions.Item>
        <Descriptions.Item label="自费抵扣">{{ formatAmount(currentRecord.optionalDeductionAmount) }}</Descriptions.Item>
        <Descriptions.Item label="建议备用金">{{ formatAmount(currentRecord.suggestedImprestAmount) }}</Descriptions.Item>
        <Descriptions.Item label="申请金额">{{ formatAmount(currentRecord.requestedAmount) }}</Descriptions.Item>
        <Descriptions.Item label="已付/余额">
          {{ formatAmount(currentRecord.paidAmount) }} / {{ formatAmount(currentRecord.balanceAmount) }}
        </Descriptions.Item>
        <Descriptions.Item label="已占用授权">{{ formatAmount(currentRecord.occupiedAuthorizationAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前可申请">{{ formatAmount(currentRecord.availableAuthorizationAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前建议备用金">{{ formatAmount(currentRecord.currentSuggestedImprestAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前实收人数">{{ currentRecord.currentGuestCount ?? currentRecord.guestCount ?? 0 }}</Descriptions.Item>
        <Descriptions.Item label="当前现付总成本">{{ formatAmount(currentRecord.currentCashCostAmount) }}</Descriptions.Item>
        <Descriptions.Item label="当前自费抵扣">{{ formatAmount(currentRecord.currentOptionalDeductionAmount) }}</Descriptions.Item>
        <Descriptions.Item label="审批意见" :span="3">{{ currentRecord.approvalRemark || '--' }}</Descriptions.Item>
        <Descriptions.Item v-if="currentRecord.cancelReason" label="作废原因" :span="3">{{ currentRecord.cancelReason }}</Descriptions.Item>
        <Descriptions.Item label="申请备注" :span="3">{{ currentRecord.remark || '--' }}</Descriptions.Item>
      </Descriptions>

      <Table
        v-if="currentRecord?.calcLines?.length"
        :data-source="currentRecord.calcLines"
        :pagination="false"
        row-key="sortOrder"
        size="small"
        class="calc-table"
      >
        <Table.Column title="类型" width="100">
          <template #default="{ record }">{{ lineTypeText(record.lineType) }}</template>
        </Table.Column>
        <Table.Column data-index="itemName" title="项目" />
        <Table.Column title="金额" width="110">
          <template #default="{ record }">{{ formatAmount(record.amount) }}</template>
        </Table.Column>
      </Table>
      <div v-if="currentRecord && canCancel(currentRecord)" class="detail-actions">
        <Button danger @click="openCancel(currentRecord)">作废申请</Button>
        <Button v-if="currentRecord.status === 'manager_approved'" type="primary" @click="openPayment(currentRecord)">登记付款</Button>
      </div>
    </Modal>

    <Modal
      v-model:open="paymentOpen"
      title="登记备用金付款"
      :confirm-loading="paymentLoading"
    >
      <Alert
        v-if="currentRecord?.calculationChanged"
        show-icon
        type="warning"
        class="change-alert"
        message="团队安排已变化，请作废旧申请并重新提交"
        description="当前申请的计算快照已经失效，不能继续登记付款。"
      />
      <Form layout="vertical">
        <Form.Item label="付款金额">
          <InputNumber v-model:value="paymentForm.amount" :min="0.01" :precision="2" class="full-width" />
        </Form.Item>
        <Form.Item label="付款日期">
          <Input v-model:value="paymentForm.paymentDate" placeholder="YYYY-MM-DD" />
        </Form.Item>
        <Form.Item label="付款方式">
          <Input v-model:value="paymentForm.paymentMethod" placeholder="例如 bank / alipay / cash" />
        </Form.Item>
        <Form.Item label="付款账户">
          <Input v-model:value="paymentForm.paymentAccountName" allow-clear />
        </Form.Item>
        <Form.Item label="备注">
          <Textarea v-model:value="paymentForm.remark" :rows="3" />
        </Form.Item>
      </Form>
      <template #footer>
        <Button @click="paymentOpen = false">取消</Button>
        <Button
          type="primary"
          :loading="paymentLoading"
          :disabled="Boolean(currentRecord?.calculationChanged)"
          @click="submitPayment"
        >
          确认付款
        </Button>
      </template>
    </Modal>

    <Modal
      v-model:open="cancelOpen"
      title="作废导游备用金申请"
      :confirm-loading="cancelLoading"
      ok-text="确认作废"
      cancel-text="取消"
      @ok="submitCancel"
    >
      <Alert
        show-icon
        type="warning"
        class="change-alert"
        message="作废只释放后续可申请额度，不删除历史申请和计算快照。"
      />
      <Form layout="vertical">
        <Form.Item label="作废原因" required>
          <Textarea v-model:value="cancelForm.cancelReason" :rows="4" placeholder="请填写作废原因，便于后续财务追溯" />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.guide-imprest-page {
  display: grid;
  gap: 12px;
}

.guide-imprest-page :deep(.ant-card) {
  border-radius: 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.full-width {
  width: 100%;
}

.preview-summary,
.calc-table {
  margin-top: 12px;
}

.change-alert {
  margin-bottom: 12px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
