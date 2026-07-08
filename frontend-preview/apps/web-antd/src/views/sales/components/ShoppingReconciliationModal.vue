<script lang="ts" setup>
import type { ShoppingCommissionApi } from '#/api/finance/shopping';

import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Spin,
  Table,
  Textarea,
  message,
} from 'ant-design-vue';
import { computed, reactive, ref, watch } from 'vue';

import {
  calculateShoppingSettlement,
  cancelShoppingFeedbackLine,
  getShoppingCommissionOverview,
  saveShoppingFeedbackLine,
  saveShoppingRuleOverride,
} from '#/api/finance/shopping';

import { shoppingCategoryOptions } from './arrangement-editor-model';

type ShoppingFeedbackDetailTotals = {
  cashAmount: number;
  companyRebateAmount: number;
  consumptionAmount: number;
  guideCommissionAmount: number;
  headFeeAmount: number;
};

const props = defineProps<{
  open: boolean;
  teamDepartureDate?: string;
  teamId?: number;
}>();

const emit = defineEmits<{
  recalculated: [];
  'update:open': [value: boolean];
}>();

const loading = ref(false);
const saving = ref(false);
const calculating = ref(false);
const overview = ref<ShoppingCommissionApi.Overview>();

const visible = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
});

const ruleDraft = reactive<ShoppingCommissionApi.RuleSaveParams>({
  baseCommissionRate: 8,
  overrideReason: '',
  targetCommissionRate: 10,
  thresholdPerCapitaAmount: 5000,
});

const settlementDraft = reactive<ShoppingCommissionApi.SettlementCalculateParams>({
  manualGuideBonusAmount: 0,
  manualGuideBonusRemark: '',
});

const feedbackDraft = reactive<ShoppingCommissionApi.FeedbackLineSaveParams>({
  businessDate: '',
  companyRebateAmount: 0,
  consumptionAmount: 0,
  detailLines: [],
  guideCommissionAmount: 0,
  guideId: undefined,
  guideName: '',
  headFeeAmount: 0,
  id: undefined,
  peopleCount: 0,
  rebateCalcMode: 'total',
  remark: '',
  shopName: '',
  supplierId: undefined,
});

const feedbackDetailTotals = computed(() => summarizeFeedbackDetailLines(feedbackDraft.detailLines || []));

watch(
  () => [props.open, props.teamId] as const,
  async ([open]) => {
    if (!open) return;
    resetFeedbackDraft();
    await loadOverview();
  },
);

function numericMoney(value?: number | string) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatMoney(value?: number | string) {
  return `¥${numericMoney(value).toLocaleString('zh-CN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  })}`;
}

function formatPercent(value?: number | string) {
  return `${numericMoney(value).toFixed(2)}%`;
}

function ruleSourceLabel(value?: string) {
  if (value === 'team_override') return '团队覆盖';
  if (value === 'default_rule') return '公司规则';
  if (value === 'system_default') return '系统默认';
  return '--';
}

function hydrateRuleDraft(rule?: ShoppingCommissionApi.Rule) {
  ruleDraft.thresholdPerCapitaAmount = Number(rule?.thresholdPerCapitaAmount ?? 5000);
  ruleDraft.baseCommissionRate = Number(rule?.baseCommissionRate ?? 8);
  ruleDraft.targetCommissionRate = Number(rule?.targetCommissionRate ?? 10);
  ruleDraft.overrideReason = rule?.overrideReason || '';
}

function hydrateSettlementDraft(settlement?: ShoppingCommissionApi.Settlement) {
  settlementDraft.manualGuideBonusAmount = Number(settlement?.manualGuideBonusAmount || 0);
  settlementDraft.manualGuideBonusRemark = settlement?.manualGuideBonusRemark || '';
}

function createFeedbackDetailLine(
  line?: ShoppingCommissionApi.FeedbackDetailLine,
  index = 0,
): ShoppingCommissionApi.FeedbackDetailLine {
  return {
    cashAmount: Number(line?.cashAmount || 0),
    categoryName: line?.categoryName || '茶叶',
    companyRebateAmount: Number(line?.companyRebateAmount || 0),
    companyRebateRate: Number(line?.companyRebateRate || 0),
    consumptionAmount: Number(line?.consumptionAmount || 0),
    guideCommissionAmount: Number(line?.guideCommissionAmount || 0),
    guideCommissionRate: Number(line?.guideCommissionRate || 0),
    headFeeAmount: Number(line?.headFeeAmount || 0),
    id: line?.id,
    peopleCount: Number(line?.peopleCount || 0),
    remark: line?.remark || '',
    sortOrder: line?.sortOrder || index + 1,
  };
}

function summarizeFeedbackDetailLines(lines: ShoppingCommissionApi.FeedbackDetailLine[] = []) {
  const initial: ShoppingFeedbackDetailTotals = {
    cashAmount: 0,
    companyRebateAmount: 0,
    consumptionAmount: 0,
    guideCommissionAmount: 0,
    headFeeAmount: 0,
  };
  return lines.reduce<ShoppingFeedbackDetailTotals>(
    (summary, line) => ({
      cashAmount: summary.cashAmount + numericMoney(line.cashAmount),
      companyRebateAmount: summary.companyRebateAmount + numericMoney(line.companyRebateAmount),
      consumptionAmount: summary.consumptionAmount + numericMoney(line.consumptionAmount),
      guideCommissionAmount: summary.guideCommissionAmount + numericMoney(line.guideCommissionAmount),
      headFeeAmount: summary.headFeeAmount + numericMoney(line.headFeeAmount),
    }),
    initial,
  );
}

function syncFeedbackDetailAmount(
  line: ShoppingCommissionApi.FeedbackDetailLine,
  field: 'company' | 'guide',
) {
  const consumptionAmount = numericMoney(line.consumptionAmount);
  if (field === 'company') {
    const rate = numericMoney(line.companyRebateRate);
    if (rate > 0) {
      line.companyRebateAmount = Number(((consumptionAmount * rate) / 100).toFixed(2));
    }
    return;
  }
  const rate = numericMoney(line.guideCommissionRate);
  if (rate > 0) {
    line.guideCommissionAmount = Number(((consumptionAmount * rate) / 100).toFixed(2));
  }
}

function addFeedbackDetailLine() {
  const lines = feedbackDraft.detailLines || [];
  lines.push(createFeedbackDetailLine(undefined, lines.length));
  feedbackDraft.detailLines = lines;
}

function removeFeedbackDetailLine(index: number) {
  const lines = feedbackDraft.detailLines || [];
  if (lines.length <= 1) {
    message.warning('按品类返佣至少保留一条消费详情');
    return;
  }
  lines.splice(index, 1);
  lines.forEach((line, currentIndex) => {
    line.sortOrder = currentIndex + 1;
  });
}

function handleFeedbackModeChange() {
  if (feedbackDraft.rebateCalcMode === 'category' && !feedbackDraft.detailLines?.length) {
    feedbackDraft.detailLines = [createFeedbackDetailLine(undefined, 0)];
  }
}

function resetFeedbackDraft(line?: ShoppingCommissionApi.FeedbackLine) {
  const rebateCalcMode = line?.rebateCalcMode || 'total';
  Object.assign(feedbackDraft, {
    businessDate: line?.businessDate || props.teamDepartureDate || '',
    companyRebateAmount: Number(line?.companyRebateAmount || 0),
    consumptionAmount: Number(line?.consumptionAmount || 0),
    detailLines: (line?.detailLines || []).map((item, index) => createFeedbackDetailLine(item, index)),
    guideCommissionAmount: Number(line?.guideCommissionAmount || 0),
    guideId: line?.guideId,
    guideName: line?.guideName || '',
    headFeeAmount: Number(line?.headFeeAmount || 0),
    id: line?.id,
    peopleCount: Number(line?.peopleCount || 0),
    rebateCalcMode,
    remark: line?.remark || '',
    shopName: line?.shopName || '',
    supplierId: line?.supplierId,
  });
  if (rebateCalcMode === 'category' && !feedbackDraft.detailLines?.length) {
    feedbackDraft.detailLines = [createFeedbackDetailLine(undefined, 0)];
  }
}

async function loadOverview() {
  if (!props.teamId) return;
  loading.value = true;
  try {
    const result = await getShoppingCommissionOverview(props.teamId);
    overview.value = result;
    hydrateRuleDraft(result.rule);
    hydrateSettlementDraft(result.latestSettlement);
  } finally {
    loading.value = false;
  }
}

async function saveRuleDraft() {
  if (!props.teamId) return;
  saving.value = true;
  try {
    await saveShoppingRuleOverride(props.teamId, {
      baseCommissionRate: Number(ruleDraft.baseCommissionRate || 0),
      overrideReason: ruleDraft.overrideReason || undefined,
      targetCommissionRate: Number(ruleDraft.targetCommissionRate || 0),
      thresholdPerCapitaAmount: Number(ruleDraft.thresholdPerCapitaAmount || 0),
    });
    message.success('购物阶梯规则已保存');
    await loadOverview();
  } finally {
    saving.value = false;
  }
}

async function saveFeedbackDraft() {
  if (!props.teamId) return;
  if (!feedbackDraft.shopName?.trim()) {
    message.warning('请填写购物店名称');
    return;
  }
  if (feedbackDraft.rebateCalcMode === 'category' && !feedbackDraft.detailLines?.length) {
    message.warning('请至少填写一条品类消费详情');
    return;
  }
  const detailTotals = feedbackDetailTotals.value;
  saving.value = true;
  try {
    await saveShoppingFeedbackLine(props.teamId, {
      ...feedbackDraft,
      companyRebateAmount: feedbackDraft.rebateCalcMode === 'category'
        ? detailTotals.companyRebateAmount
        : Number(feedbackDraft.companyRebateAmount || 0),
      consumptionAmount: feedbackDraft.rebateCalcMode === 'category'
        ? detailTotals.consumptionAmount
        : Number(feedbackDraft.consumptionAmount || 0),
      detailLines: feedbackDraft.rebateCalcMode === 'category'
        ? (feedbackDraft.detailLines || []).map((item, index) => ({
          ...item,
          sortOrder: index + 1,
        }))
        : [],
      guideCommissionAmount: feedbackDraft.rebateCalcMode === 'category'
        ? detailTotals.guideCommissionAmount
        : Number(feedbackDraft.guideCommissionAmount || 0),
      headFeeAmount: feedbackDraft.rebateCalcMode === 'category'
        ? detailTotals.headFeeAmount
        : Number(feedbackDraft.headFeeAmount || 0),
      shopName: feedbackDraft.shopName.trim(),
    });
    message.success('购物反馈已保存');
    resetFeedbackDraft();
    await loadOverview();
  } finally {
    saving.value = false;
  }
}

function editFeedbackLine(line: ShoppingCommissionApi.FeedbackLine) {
  resetFeedbackDraft(line);
}

function feedbackCategoryText(line: ShoppingCommissionApi.FeedbackLine) {
  const categories = (line.detailLines || [])
    .map((item) => item.categoryName)
    .filter(Boolean);
  return categories.length ? categories.join('、') : '综合';
}

async function removeFeedbackLine(line: ShoppingCommissionApi.FeedbackLine) {
  if (!props.teamId || !line.id) return;
  Modal.confirm({
    async onOk() {
      await cancelShoppingFeedbackLine(props.teamId!, line.id!);
      message.success('购物反馈已作废');
      await loadOverview();
    },
    content: `确认作废 ${line.shopName || '该购物反馈'}？`,
    title: '作废购物反馈',
  });
}

async function calculateAction() {
  if (!props.teamId) return;
  calculating.value = true;
  try {
    await calculateShoppingSettlement(props.teamId, {
      manualGuideBonusAmount: Number(settlementDraft.manualGuideBonusAmount || 0),
      manualGuideBonusRemark: settlementDraft.manualGuideBonusRemark || undefined,
    });
    message.success('购物核对结算已重新计算');
    await loadOverview();
    emit('recalculated');
  } finally {
    calculating.value = false;
  }
}

function formulaText(settlement?: ShoppingCommissionApi.Settlement) {
  if (!settlement) return '暂无结算结果';
  return `${formatMoney(settlement.headFeeAmount)} + ${formatMoney(settlement.companyRebateAmount)} - ${formatMoney(settlement.manualGuideBonusAmount)} = ${formatMoney(settlement.companyProfitAmount)}`;
}
</script>

<template>
  <Modal
    v-model:open="visible"
    class="shopping-reconciliation-modal"
    destroy-on-close
    title="购物核对/补佣"
    width="1120px"
    :footer="null"
  >
    <Spin :spinning="loading">
      <section class="shopping-summary">
        <div class="shopping-summary-card">
          <span>规则来源</span>
          <strong>{{ ruleSourceLabel(overview?.rule?.ruleSource) }}</strong>
        </div>
        <div class="shopping-summary-card">
          <span>人均门槛</span>
          <strong>{{ formatMoney(overview?.rule?.thresholdPerCapitaAmount) }}</strong>
        </div>
        <div class="shopping-summary-card">
          <span>基础/达标比例</span>
          <strong>{{ formatPercent(overview?.rule?.baseCommissionRate) }} / {{ formatPercent(overview?.rule?.targetCommissionRate) }}</strong>
        </div>
        <div class="shopping-summary-card">
          <span>公司补佣</span>
          <strong>{{ formatMoney(overview?.latestSettlement?.manualGuideBonusAmount) }}</strong>
        </div>
        <div class="shopping-summary-card highlight">
          <span>内账购物利润</span>
          <strong>{{ formatMoney(overview?.latestSettlement?.companyProfitAmount) }}</strong>
        </div>
      </section>

      <section class="shopping-section">
        <div class="shopping-section-title">
          <span>参考阶梯测算规则</span>
          <small>只用于测算是否超额和参考补差，不自动计入团队账单</small>
        </div>
        <Form layout="vertical" class="shopping-rule-grid">
          <Form.Item label="人均消费门槛">
            <InputNumber v-model:value="ruleDraft.thresholdPerCapitaAmount" addon-before="¥" :min="0" :precision="2" class="full-width" />
          </Form.Item>
          <Form.Item label="基础佣金比例">
            <InputNumber v-model:value="ruleDraft.baseCommissionRate" addon-after="%" :min="0" :precision="2" class="full-width" />
          </Form.Item>
          <Form.Item label="达标目标比例">
            <InputNumber v-model:value="ruleDraft.targetCommissionRate" addon-after="%" :min="0" :precision="2" class="full-width" />
          </Form.Item>
          <Form.Item label="调整原因">
            <Input v-model:value="ruleDraft.overrideReason" allow-clear placeholder="例如：本团协议比例 / 团质较好" />
          </Form.Item>
        </Form>
        <div class="shopping-actions">
          <Button :loading="saving" @click="saveRuleDraft">保存规则</Button>
        </div>
      </section>

      <section class="shopping-section">
        <div class="shopping-section-title">
          <span>公司补佣</span>
          <small>正式影响内账购物利润；导游现场从购物店拿走的佣金只做核对</small>
        </div>
        <Form layout="vertical" class="shopping-bonus-grid">
          <Form.Item label="补佣金额">
            <InputNumber
              v-model:value="settlementDraft.manualGuideBonusAmount"
              addon-before="¥"
              :min="0"
              :precision="2"
              class="full-width"
            />
          </Form.Item>
          <Form.Item label="补佣说明">
            <Textarea
              v-model:value="settlementDraft.manualGuideBonusRemark"
              :auto-size="{ minRows: 1, maxRows: 3 }"
              :maxlength="1000"
              placeholder="例如：全团购物超额完成，公司补导游差额"
              show-count
            />
          </Form.Item>
        </Form>
        <div class="shopping-actions">
          <Button type="primary" :loading="calculating" @click="calculateAction">
            重新计算并保存结算
          </Button>
        </div>
      </section>

      <section class="shopping-section">
        <div class="shopping-section-title">
          <span>购物店反馈</span>
          <small>同一家店可按不同品类分行核对返点和导游报账</small>
        </div>
        <Form layout="vertical" class="shopping-feedback-grid">
          <Form.Item label="购物店">
            <Input v-model:value="feedbackDraft.shopName" allow-clear placeholder="购物店名称" />
          </Form.Item>
          <Form.Item label="消费日期">
            <Input v-model:value="feedbackDraft.businessDate" allow-clear placeholder="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item label="进店人数">
            <InputNumber v-model:value="feedbackDraft.peopleCount" :min="0" class="full-width" />
          </Form.Item>
          <Form.Item label="返佣模式">
            <Radio.Group v-model:value="feedbackDraft.rebateCalcMode" button-style="solid" @change="handleFeedbackModeChange">
              <Radio.Button value="total">总额返佣</Radio.Button>
              <Radio.Button value="category">按品类返佣</Radio.Button>
            </Radio.Group>
          </Form.Item>
          <Form.Item label="备注">
            <Input v-model:value="feedbackDraft.remark" allow-clear placeholder="群反馈、核对说明等" />
          </Form.Item>
        </Form>

        <Form v-if="feedbackDraft.rebateCalcMode !== 'category'" layout="vertical" class="shopping-feedback-grid">
          <Form.Item label="消费总额">
            <InputNumber v-model:value="feedbackDraft.consumptionAmount" addon-before="¥" :min="0" :precision="2" class="full-width" />
          </Form.Item>
          <Form.Item label="公司返佣">
            <InputNumber v-model:value="feedbackDraft.companyRebateAmount" addon-before="¥" :min="0" :precision="2" class="full-width" />
          </Form.Item>
          <Form.Item label="导游现场佣金">
            <InputNumber v-model:value="feedbackDraft.guideCommissionAmount" addon-before="¥" :min="0" :precision="2" class="full-width" />
          </Form.Item>
          <Form.Item label="人头费">
            <InputNumber v-model:value="feedbackDraft.headFeeAmount" addon-before="¥" :min="0" :precision="2" class="full-width" />
          </Form.Item>
        </Form>

        <div v-else class="shopping-detail-panel">
          <div class="shopping-detail-summary">
            <span>消费合计：{{ formatMoney(feedbackDetailTotals.consumptionAmount) }}</span>
            <span>公司返佣：{{ formatMoney(feedbackDetailTotals.companyRebateAmount) }}</span>
            <span>现场佣金：{{ formatMoney(feedbackDetailTotals.guideCommissionAmount) }}</span>
            <span>人头费：{{ formatMoney(feedbackDetailTotals.headFeeAmount) }}</span>
          </div>
          <div
            v-for="(line, index) in feedbackDraft.detailLines || []"
            :key="`shopping-feedback-detail-${index}`"
            class="shopping-detail-line"
          >
            <Form layout="vertical" class="shopping-detail-grid">
              <Form.Item label="品类">
                <Select v-model:value="line.categoryName" show-search :options="shoppingCategoryOptions" />
              </Form.Item>
              <Form.Item label="消费金额">
                <InputNumber
                  v-model:value="line.consumptionAmount"
                  addon-before="¥"
                  :min="0"
                  :precision="2"
                  class="full-width"
                  @change="() => { syncFeedbackDetailAmount(line, 'company'); syncFeedbackDetailAmount(line, 'guide'); }"
                />
              </Form.Item>
              <Form.Item label="公司返佣">
                <div class="shopping-rate-field">
                  <InputNumber v-model:value="line.companyRebateRate" :min="0" :precision="2" @change="() => syncFeedbackDetailAmount(line, 'company')" />
                  <span>%</span>
                  <InputNumber v-model:value="line.companyRebateAmount" addon-before="¥" :min="0" :precision="2" />
                </div>
              </Form.Item>
              <Form.Item label="导游提成">
                <div class="shopping-rate-field">
                  <InputNumber v-model:value="line.guideCommissionRate" :min="0" :precision="2" @change="() => syncFeedbackDetailAmount(line, 'guide')" />
                  <span>%</span>
                  <InputNumber v-model:value="line.guideCommissionAmount" addon-before="¥" :min="0" :precision="2" />
                </div>
              </Form.Item>
              <Form.Item label="人头费">
                <InputNumber v-model:value="line.headFeeAmount" addon-before="¥" :min="0" :precision="2" class="full-width" />
              </Form.Item>
              <Form.Item label="现结">
                <InputNumber v-model:value="line.cashAmount" addon-before="¥" :min="0" :precision="2" class="full-width" />
              </Form.Item>
              <Form.Item label="备注">
                <Input v-model:value="line.remark" allow-clear placeholder="品类核对说明" />
              </Form.Item>
              <Form.Item label="操作">
                <Button danger size="small" @click="removeFeedbackDetailLine(index)">删除</Button>
              </Form.Item>
            </Form>
          </div>
          <Button size="small" @click="addFeedbackDetailLine">新增品类明细</Button>
        </div>
        <div class="shopping-actions">
          <Button @click="resetFeedbackDraft()">清空</Button>
          <Button type="primary" :loading="saving" @click="saveFeedbackDraft">
            {{ feedbackDraft.id ? '保存修改' : '新增反馈' }}
          </Button>
        </div>

        <Table
          :data-source="overview?.feedbackLines || []"
          :pagination="false"
          row-key="id"
          size="small"
          class="shopping-feedback-table"
        >
          <Table.Column data-index="shopName" title="购物店" width="160" />
          <Table.Column data-index="businessDate" title="日期" width="110" />
          <Table.Column data-index="peopleCount" title="人数" width="80" />
          <Table.Column title="返佣模式" width="100">
            <template #default="{ record }">{{ record.rebateCalcMode === 'category' ? '按品类' : '总额' }}</template>
          </Table.Column>
          <Table.Column title="品类" width="160">
            <template #default="{ record }">
              {{ feedbackCategoryText(record) }}
            </template>
          </Table.Column>
          <Table.Column title="消费总额" width="120">
            <template #default="{ record }">{{ formatMoney(record.consumptionAmount) }}</template>
          </Table.Column>
          <Table.Column title="公司返佣" width="120">
            <template #default="{ record }">{{ formatMoney(record.companyRebateAmount) }}</template>
          </Table.Column>
          <Table.Column title="现场佣金" width="120">
            <template #default="{ record }">{{ formatMoney(record.guideCommissionAmount) }}</template>
          </Table.Column>
          <Table.Column title="人头费" width="110">
            <template #default="{ record }">{{ formatMoney(record.headFeeAmount) }}</template>
          </Table.Column>
          <Table.Column data-index="remark" title="备注" />
          <Table.Column title="操作" width="120" fixed="right">
            <template #default="{ record }">
              <Button type="link" size="small" @click="editFeedbackLine(record)">修改</Button>
              <Button type="link" danger size="small" @click="removeFeedbackLine(record)">作废</Button>
            </template>
          </Table.Column>
        </Table>
      </section>

      <section class="shopping-section">
        <div class="shopping-section-title">
          <span>结算结果</span>
          <small>{{ formulaText(overview?.latestSettlement) }}</small>
        </div>
        <div class="shopping-settlement-grid">
          <span>实收人数：{{ overview?.latestSettlement?.guestCount ?? 0 }} 人</span>
          <span>消费总额：{{ formatMoney(overview?.latestSettlement?.totalConsumptionAmount) }}</span>
          <span>人均消费：{{ formatMoney(overview?.latestSettlement?.perCapitaConsumptionAmount) }}</span>
          <span>是否达标：{{ overview?.latestSettlement?.thresholdReached ? '是' : '否' }}</span>
          <span>参考阶梯补差：{{ formatMoney(overview?.latestSettlement?.ladderExtraCommissionAmount) }}</span>
          <span>导游现场佣金 + 参考补差：{{ formatMoney(overview?.latestSettlement?.guideCommissionTotalAmount) }}</span>
          <span>公司补佣：{{ formatMoney(overview?.latestSettlement?.manualGuideBonusAmount) }}</span>
        </div>
      </section>
    </Spin>
  </Modal>
</template>

<style scoped>
.shopping-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.shopping-summary-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  background: #fff;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

.shopping-summary-card span {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.shopping-summary-card strong {
  overflow: hidden;
  font-size: 18px;
  font-weight: 900;
  color: #1554ad;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shopping-summary-card.highlight {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.shopping-summary-card.highlight strong {
  color: #15803d;
}

.shopping-section {
  padding: 14px;
  margin-top: 12px;
  background: #fff;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

.shopping-section-title {
  display: flex;
  gap: 10px;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.shopping-section-title span {
  font-size: 15px;
  font-weight: 900;
  color: #1e293b;
}

.shopping-section-title small {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.shopping-rule-grid,
.shopping-feedback-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0 12px;
}

.shopping-bonus-grid {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 0 12px;
}

.full-width {
  width: 100%;
}

.shopping-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 8px;
}

.shopping-detail-panel {
  display: grid;
  gap: 10px;
}

.shopping-detail-summary,
.shopping-settlement-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.shopping-detail-summary span,
.shopping-settlement-grid span {
  padding: 5px 8px;
  font-size: 12px;
  font-weight: 800;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
}

.shopping-detail-line {
  padding: 10px;
  background: #f8fbff;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.shopping-detail-grid {
  display: grid;
  grid-template-columns: 130px 140px 210px 210px 120px 120px minmax(150px, 1fr) 70px;
  gap: 0 8px;
}

.shopping-rate-field {
  display: grid;
  grid-template-columns: 72px 18px minmax(0, 1fr);
  gap: 5px;
  align-items: center;
}

.shopping-feedback-table {
  margin-top: 12px;
}

@media (width <= 1200px) {
  .shopping-summary,
  .shopping-rule-grid,
  .shopping-feedback-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .shopping-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
