<script lang="ts" setup>
import type { SalesProductApi } from '#/api/sales/product';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Card,
  Input,
  InputNumber,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  getSalesProductDetail,
  updateSalesProduct,
} from '#/api/sales/product';

import {
  buildSalesProductPayload,
  calculateArrangementTotal,
  createDefaultArrangementItem,
  createDefaultProductForm,
  type ProductFormState,
} from './product-form-utils';

type ArrangementShortcut = {
  icon: string;
  label: string;
  value: SalesProductApi.ArrangementType;
};

const route = useRoute();
const router = useRouter();

const arrangementTypeOptions: Array<{ label: string; value: SalesProductApi.ArrangementType }> = [
  { label: '大交通', value: 'traffic' },
  { label: '住宿', value: 'hotel' },
  { label: '用车', value: 'vehicle' },
  { label: '景区', value: 'scenic' },
  { label: '用餐', value: 'meal' },
  { label: '其它', value: 'other' },
  { label: '自费', value: 'optional' },
  { label: '购物', value: 'shopping' },
  { label: '地接', value: 'ground_agent' },
  { label: '附加费用', value: 'extra_fee' },
];

const arrangementShortcuts: ArrangementShortcut[] = [
  { icon: 'lucide:plane', label: '大交通', value: 'traffic' },
  { icon: 'lucide:building-2', label: '住宿', value: 'hotel' },
  { icon: 'lucide:car', label: '用车', value: 'vehicle' },
  { icon: 'lucide:landmark', label: '景区', value: 'scenic' },
  { icon: 'lucide:utensils', label: '用餐', value: 'meal' },
  { icon: 'lucide:grid-2x2', label: '其它', value: 'other' },
  { icon: 'lucide:ticket', label: '自费', value: 'optional' },
  { icon: 'lucide:store', label: '购物', value: 'shopping' },
  { icon: 'lucide:circle-parking', label: '地接', value: 'ground_agent' },
  { icon: 'lucide:paperclip', label: '附加费用', value: 'extra_fee' },
];

const arrangementOverviewColumns = [
  { label: '大交通', value: 'traffic' },
  { label: '住宿', value: 'hotel' },
  { label: '用车', value: 'vehicle' },
  { label: '景区', value: 'scenic' },
  { label: '用餐', value: 'meal' },
  { label: '其它', value: 'other' },
  { label: '地接', value: 'ground_agent' },
  { label: '附加', value: 'extra_fee' },
] as Array<{ label: string; value: SalesProductApi.ArrangementType }>;

const settlementTypeOptions = [
  { label: '挂账', value: 'credit' },
  { label: '现结', value: 'cash' },
];

const loading = ref(false);
const saving = ref(false);
const activeArrangementType = ref<SalesProductApi.ArrangementType>('traffic');
const formState = reactive<ProductFormState>(createDefaultProductForm());

const productId = computed(() => {
  const value = route.params.id;
  const id = Array.isArray(value) ? value[0] : value;
  return id ? Number(id) : undefined;
});
const pageTitle = computed(() => (
  formState.productName ? `团队安排 - ${formState.productName}` : '产品团队安排'
));
const arrangementTotal = computed(() => calculateArrangementTotal(formState.arrangementItems));
const arrangementCashTotal = computed(() => arrangementOverviewColumns.reduce(
  (sum, item) => sum + arrangementSettlementTotal(item.value, 'cash'),
  0,
));
const arrangementCreditTotal = computed(() => arrangementOverviewColumns.reduce(
  (sum, item) => sum + arrangementSettlementTotal(item.value, 'credit'),
  0,
));
const activeArrangementItems = computed(() => (
  formState.arrangementItems || []
).filter((item) => item.arrangementType === activeArrangementType.value));

function formatMoney(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    currency: 'CNY',
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
    style: 'currency',
  }).format(Number(value || 0));
}

function arrangementTypeLabel(value?: string) {
  return arrangementTypeOptions.find((item) => item.value === value)?.label || '-';
}

function arrangementTypeTotal(type: SalesProductApi.ArrangementType) {
  return calculateArrangementTotal(
    (formState.arrangementItems || []).filter((item) => item.arrangementType === type),
  );
}

function arrangementSettlementTotal(
  type: SalesProductApi.ArrangementType,
  settlementType: 'cash' | 'credit',
) {
  return calculateArrangementTotal(
    (formState.arrangementItems || []).filter((item) => (
      item.arrangementType === type && item.settlementType === settlementType
    )),
  );
}

function selectArrangementType(type: SalesProductApi.ArrangementType) {
  activeArrangementType.value = type;
}

function fillForm(detail: SalesProductApi.Item) {
  Object.assign(formState, {
    arrangementItems: detail.arrangementItems || [],
    attentionItems: detail.attentionItems,
    bookingNotice: detail.bookingNotice,
    businessType: detail.businessType,
    childPolicy: detail.childPolicy,
    city: detail.city,
    closeDaysBefore: detail.closeDaysBefore ?? 0,
    district: detail.district,
    domesticInternational: detail.domesticInternational || 'domestic',
    feeExcluded: detail.feeExcluded,
    feeIncluded: detail.feeIncluded,
    giftItems: detail.giftItems,
    itineraryDays: detail.itineraryDays || [],
    optionalItems: detail.optionalItems,
    plannedCapacity: detail.plannedCapacity ?? 0,
    productDescription: detail.productDescription,
    productName: detail.productName,
    productTheme: detail.productTheme,
    province: detail.province,
    receptionStandard: detail.receptionStandard,
    remark: detail.remark,
    shoppingArrangement: detail.shoppingArrangement,
    singleRoomDifference: detail.singleRoomDifference ?? 0,
    status: detail.status || 'active',
    travelDays: detail.travelDays || 1,
    tripType: detail.tripType || 'irregular',
    warmReminder: detail.warmReminder,
  });
}

async function loadDetail() {
  if (!productId.value) {
    message.warning('缺少产品ID');
    goBack();
    return;
  }
  loading.value = true;
  try {
    const detail = await getSalesProductDetail(productId.value);
    fillForm(detail);
  } finally {
    loading.value = false;
  }
}

function addArrangementItem() {
  const items = formState.arrangementItems || [];
  items.push({
    ...createDefaultArrangementItem(),
    arrangementType: activeArrangementType.value,
  });
  formState.arrangementItems = items;
}

function removeArrangementItem(index: number) {
  const target = activeArrangementItems.value[index];
  formState.arrangementItems = (formState.arrangementItems || []).filter((item) => item !== target);
}

function goBack() {
  router.push('/sales/product');
}

function goProductEdit() {
  if (!productId.value) return;
  router.push(`/sales/product/edit/${productId.value}`);
}

/**
 * 保存产品团队安排参数。
 *
 * 当前后端产品保存接口会重建产品的行程、说明和团队安排子表，所以这里必须基于详情组装完整
 * 产品保存参数，只替换 arrangementItems，不能只提交团队安排字段。
 */
async function saveArrangement() {
  if (!productId.value) {
    message.warning('缺少产品ID');
    return;
  }
  saving.value = true;
  try {
    const payload = buildSalesProductPayload(
      formState,
      [formState.province, formState.city, formState.district].filter(Boolean) as string[],
    );
    await updateSalesProduct(productId.value, payload);
    message.success('团队安排已保存');
    await loadDetail();
  } finally {
    saving.value = false;
  }
}

onMounted(loadDetail);
</script>

<template>
  <Page :title="pageTitle" description="维护产品生成团队时要带入的默认团队安排参数，正式排房、派车、订票仍在后续团队安排和计调模块处理。">
    <Spin :spinning="loading">
      <Card class="team-arrangement-card">
        <div class="form-header">
          <div>
            <div class="form-title">产品团队安排</div>
            <div class="form-subtitle">
              这里只维护产品生成团队时的默认安排参数，不处理正式订单、单据、导游报账、计调审核和真实团队成本。
            </div>
          </div>
          <Space>
            <Button @click="goBack">返回列表</Button>
            <Button @click="goProductEdit">修改产品</Button>
            <Button type="primary" :loading="saving" @click="saveArrangement">保存团队安排</Button>
          </Space>
        </div>

        <Alert
          class="arrangement-alert"
          message="该页对应老系统产品里的“团队安排”页签。它是产品模板参数，不是计调操作里的正式团队安排执行页。"
          show-icon
          type="info"
        />

        <div class="arrangement-overview-table">
          <table>
            <thead>
              <tr>
                <th
                  v-for="item in arrangementOverviewColumns"
                  :key="item.value"
                  colspan="2"
                >
                  {{ item.label }}
                </th>
                <th colspan="2">合计</th>
                <th rowspan="2">自费收入</th>
              </tr>
              <tr>
                <template
                  v-for="item in arrangementOverviewColumns"
                  :key="`${item.value}-settlement`"
                >
                  <th>现结</th>
                  <th>挂账</th>
                </template>
                <th>现结</th>
                <th>挂账</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <template
                  v-for="item in arrangementOverviewColumns"
                  :key="`${item.value}-amount`"
                >
                  <td>{{ formatMoney(arrangementSettlementTotal(item.value, 'cash')) }}</td>
                  <td>{{ formatMoney(arrangementSettlementTotal(item.value, 'credit')) }}</td>
                </template>
                <td>{{ formatMoney(arrangementCashTotal) }}</td>
                <td>{{ formatMoney(arrangementCreditTotal) }}</td>
                <td>{{ formatMoney(arrangementTypeTotal('optional')) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="arrangement-icon-grid" role="tablist" aria-label="团队安排分类">
          <button
            v-for="item in arrangementShortcuts"
            :key="item.value"
            type="button"
            class="arrangement-icon-button"
            :class="{ active: activeArrangementType === item.value }"
            role="tab"
            :aria-selected="activeArrangementType === item.value"
            @click="selectArrangementType(item.value)"
          >
            <IconifyIcon :icon="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </div>

        <div class="section-toolbar">
          <div>
            <div class="section-title">{{ arrangementTypeLabel(activeArrangementType) }}安排</div>
            <div class="muted">按老系统团队安排分类维护产品默认参考；真实团队履约要到团队安排页按具体团号、日期、人数和供应商逐项落地。</div>
          </div>
          <div class="toolbar-actions">
            <Tag color="blue">产品参数</Tag>
            <Button type="primary" @click="addArrangementItem">新增安排</Button>
          </div>
        </div>

        <Table
          :data-source="activeArrangementItems"
          :pagination="false"
          row-key="id"
          size="small"
          :scroll="{ x: 980 }"
        >
          <Table.Column title="项目名称" data-index="itemName" key="itemName" width="170">
            <template #default="{ record }">
              <Input v-model:value="record.itemName" allow-clear />
            </template>
          </Table.Column>
          <Table.Column title="安排内容" data-index="arrangementContent" key="arrangementContent">
            <template #default="{ record }">
              <Input v-model:value="record.arrangementContent" allow-clear />
            </template>
          </Table.Column>
          <Table.Column title="数量" data-index="quantity" key="quantity" width="100">
            <template #default="{ record }">
              <InputNumber v-model:value="record.quantity" class="w-full" :min="0" />
            </template>
          </Table.Column>
          <Table.Column title="单价" data-index="unitPrice" key="unitPrice" width="120">
            <template #default="{ record }">
              <InputNumber v-model:value="record.unitPrice" class="w-full" :min="0" :precision="2" />
            </template>
          </Table.Column>
          <Table.Column title="单位" data-index="unitName" key="unitName" width="100">
            <template #default="{ record }">
              <Input v-model:value="record.unitName" allow-clear />
            </template>
          </Table.Column>
          <Table.Column title="结算" data-index="settlementType" key="settlementType" width="110">
            <template #default="{ record }">
              <Select
                v-model:value="record.settlementType"
                :options="settlementTypeOptions"
                class="w-full"
              />
            </template>
          </Table.Column>
          <Table.Column title="操作" key="action" width="80">
            <template #default="{ index }">
              <Button danger type="link" size="small" @click="removeArrangementItem(index)">删除</Button>
            </template>
          </Table.Column>
        </Table>

        <div class="arrangement-preview">
          <span>费用参考合计</span>
          <strong>{{ formatMoney(arrangementTotal) }}</strong>
        </div>
      </Card>
    </Spin>
  </Page>
</template>

<style scoped>
.team-arrangement-card {
  margin-bottom: 72px;
}

.form-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.form-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.form-subtitle,
.muted {
  font-size: 13px;
  color: #64748b;
}

.form-subtitle {
  margin-top: 4px;
}

.arrangement-alert {
  margin-bottom: 14px;
}

.arrangement-overview-table {
  margin-bottom: 18px;
  overflow-x: auto;
  border: 1px solid #e5e7eb;
}

.arrangement-overview-table table {
  width: 100%;
  min-width: 1260px;
  border-collapse: collapse;
  background: #fff;
}

.arrangement-overview-table th,
.arrangement-overview-table td {
  height: 42px;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  text-align: center;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.arrangement-overview-table th {
  color: #64748b;
  background: #f8fafc;
}

.arrangement-overview-table td {
  color: #0f172a;
}

.arrangement-icon-grid {
  display: grid;
  grid-template-columns: repeat(10, minmax(76px, 1fr));
  gap: 10px;
  padding: 14px 4px 18px;
  margin-bottom: 18px;
  border-bottom: 2px solid #0f9aaa;
}

.arrangement-icon-button {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-height: 72px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.arrangement-icon-button:hover,
.arrangement-icon-button.active {
  color: #0f9aaa;
  background: #ecfeff;
  border-color: #a5f3fc;
}

.arrangement-icon-button svg {
  width: 28px;
  height: 28px;
}

.arrangement-icon-button span {
  font-size: 13px;
  font-weight: 600;
}

.section-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.toolbar-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
  align-items: center;
}

.arrangement-preview {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 0 0;
  color: #475569;
}

.arrangement-preview strong {
  font-size: 19px;
  color: #1677ff;
}

@media (max-width: 900px) {
  .form-header,
  .section-toolbar {
    flex-direction: column;
  }

  .arrangement-icon-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
