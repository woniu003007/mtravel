<script lang="ts" setup>
import type { SalesProductApi } from '#/api/sales/product';
import type { RelationTicketTemplateApi } from '#/api/purchase/relation-ticket-template';
import type { RegionOption, RegionPath } from '#/utils/region';
import type {
  ArrangementEditorForm,
  ArrangementEditorMode,
  ArrangementType,
  AutoCompleteOption,
  SelectOption,
  SelectOptionWithId,
} from './arrangement-editor-model';

import { IconifyIcon } from '@vben/icons';

import {
  AutoComplete,
  Button,
  Cascader,
  Checkbox,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Spin,
  Tag,
  Textarea,
} from 'ant-design-vue';
import { computed } from 'vue';

import {
  arrangementEditorConfigs,
  breakfastOptions,
  fundOptions,
  mealTypeOptions,
  priceProjectOptionsForType,
  routeDurationText,
  shoppingCategoryOptions,
  trafficOrderOptions,
  trafficTypeOptions,
  vehicleDistanceText,
} from './arrangement-editor-model';

const props = withDefaults(defineProps<{
  activeEditorType: ArrangementType;
  arrivalRegionPath?: RegionPath;
  departureRegionPath?: RegionPath;
  driverHistoryOptions?: AutoCompleteOption[];
  editorCreditAmount: number;
  editorMode: ArrangementEditorMode;
  editorTotalAmount: number;
  editingArrangementIndex: number;
  employeeOptions?: SelectOptionWithId[];
  form: ArrangementEditorForm;
  lastVehicleQuoteResult?: {
    amount: number;
    distanceMeters: number;
    ruleName: string;
  };
  open: boolean;
  orderOptions?: SelectOption[];
  optionsLoading?: boolean;
  projectOptions?: SelectOptionWithId[];
  regionOptions?: RegionOption[];
  resourceOptions?: SelectOptionWithId[];
  saving?: boolean;
  scheduleDayOptions?: SelectOption[];
  scenicTicketTemplate?: RelationTicketTemplateApi.Template | null;
  scenicTicketTemplateLoading?: boolean;
  showMultiOrderAveragePriceNotice?: boolean;
  supplierOptions?: SelectOptionWithId[];
  vehiclePlateHistoryOptions?: AutoCompleteOption[];
  vehicleQuoteCalculating?: boolean;
  vehicleQuoteRuleOptions?: SelectOption[];
}>(), {
  arrivalRegionPath: () => [],
  departureRegionPath: () => [],
  driverHistoryOptions: () => [],
  employeeOptions: () => [],
  orderOptions: () => [],
  optionsLoading: false,
  projectOptions: () => [],
  regionOptions: () => [],
  resourceOptions: () => [],
  saving: false,
  scheduleDayOptions: () => [],
  scenicTicketTemplate: null,
  scenicTicketTemplateLoading: false,
  showMultiOrderAveragePriceNotice: false,
  supplierOptions: () => [],
  vehiclePlateHistoryOptions: () => [],
  vehicleQuoteCalculating: false,
  vehicleQuoteRuleOptions: () => [],
});

const emit = defineEmits<{
  'add-arrangement-price-line': [];
  'add-optional-summary-line': [];
  'add-shopping-consumption-line': [];
  'apply-selected-price-project': [index: number, value?: unknown];
  'apply-selected-resource': [value?: unknown];
  'apply-selected-responsible': [value?: unknown];
  'apply-selected-supplier': [value?: unknown];
  'apply-shopping-price-project': [index: number, value?: unknown];
  'apply-vehicle-quote-to-price-info': [];
  'calculate-vehicle-reference-price': [];
  'close': [];
  'download-scenic-ticket-guests': [];
  'open-hotel-create-page': [];
  'open-product-roadbook-editor': [];
  'open-resource-create-page': [];
  'open-scenic-template-config-page': [];
  'open-supplier-create-page': [];
  'remove-arrangement-price-line': [index: number];
  'remove-optional-summary-line': [index: number];
  'remove-shopping-consumption-line': [index: number];
  'save': [];
  'sync-ground-agent-days-count': [];
  'sync-hotel-nights-count': [];
  'sync-optional-line-to-form': [];
  'sync-primary-price-fields': [];
  'sync-shopping-line-to-form': [];
  'sync-vehicle-days-count': [];
  'sync-vehicle-price-project': [value?: unknown];
  'sync-vehicle-roadbook-distance': [];
  'update:arrivalRegionPath': [value: RegionPath | undefined];
  'update:departureRegionPath': [value: RegionPath | undefined];
  'update:open': [value: boolean];
  'vehicle-history-search': [historyType: SalesProductApi.VehicleUsageHistoryType, keyword?: string];
}>();

const openModel = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
});

const departureRegionPathModel = computed({
  get: () => props.departureRegionPath,
  set: (value: RegionPath | undefined) => emit('update:departureRegionPath', value),
});

const arrivalRegionPathModel = computed({
  get: () => props.arrivalRegionPath,
  set: (value: RegionPath | undefined) => emit('update:arrivalRegionPath', value),
});

const activeEditorConfig = computed(() => arrangementEditorConfigs[props.activeEditorType]);
const activeEditorTitle = computed(() => activeEditorConfig.value.title);
const mergedOrderOptions = computed<SelectOption[]>(() => {
  const defaultOption = trafficOrderOptions[0]!;
  const uniqueOptions = new Map<string, SelectOption>([[defaultOption.value, defaultOption]]);
  props.orderOptions.forEach((item) => uniqueOptions.set(item.value, item));
  return [...uniqueOptions.values()];
});
const orderInfoTip = computed(() => (
  props.editorMode === 'product'
    ? '产品模板阶段默认不关联正式订单'
    : '将此项成本归于关联订单'
));
const showProductVehicleAssistActions = computed(() => props.editorMode === 'product');
const showScenicTicketDownload = computed(() => (
  props.editorMode === 'team' && props.activeEditorType === 'scenic'
));
const submitButtonText = computed(() => {
  if (props.activeEditorType === 'optional') return '提交保存';
  if (props.editorMode === 'team') {
    return props.editingArrangementIndex >= 0 ? '保存修改' : '新增安排';
  }
  return props.editingArrangementIndex >= 0 ? '保存修改' : '新增安排';
});

function projectOptionsForCurrentType() {
  return priceProjectOptionsForType(props.activeEditorType, props.projectOptions);
}

function optionalLineCreditAmount(line: SalesProductApi.ArrangementPriceLine) {
  return Math.max(Number(line.costPrice || 0) - Number(line.cashAmount || 0), 0);
}
</script>

<template>
  <Modal
    v-model:open="openModel"
    centered
    class="traffic-arrangement-modal"
    :footer="null"
    :title="activeEditorTitle"
    width="920px"
  >
    <Spin :spinning="optionsLoading">
      <div class="traffic-modal-body">
        <Radio.Group
          v-model:value="props.form.allocationMode"
          class="traffic-cost-mode-tabs"
          option-type="button"
          button-style="solid"
        >
          <Radio.Button value="group_order_average">全团/订单均摊</Radio.Button>
          <Radio.Button value="multi_order_average">多订单均摊成本</Radio.Button>
        </Radio.Group>

        <Form class="traffic-form" layout="vertical">
          <template v-if="activeEditorType === 'hotel'">
            <div class="hotel-old-system-layout">
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:database" />
                  <span>酒店名称</span>
                </div>
                <div class="traffic-form-row hotel-name-row">
                  <Form.Item label="酒店名称">
                    <Select
                      v-model:value="props.form.resourceName"
                      allow-clear
                      show-search
                      :options="resourceOptions"
                      @change="(value) => emit('apply-selected-resource', value)"
                    />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-hotel-create-page')">添加酒店</Button>
                  </Form.Item>
                  <Form.Item label="已确认">
                    <Checkbox v-model:checked="props.form.confirmed">已确认</Checkbox>
                  </Form.Item>
                  <Form.Item label="确认号">
                    <Input v-model:value="props.form.confirmationNo" placeholder="确认号" />
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:coffee" />
                  <span>早餐基金</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="早餐">
                    <Select v-model:value="props.form.mealType" :options="breakfastOptions" />
                  </Form.Item>
                  <Form.Item label="基金">
                    <Select v-model:value="props.form.fundIncluded" :options="fundOptions" />
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:route" />
                  <span>入住退房</span>
                </div>
                <div class="traffic-form-row three-columns">
                  <Form.Item label="入住" required>
                    <Select v-model:value="props.form.scheduleStartDay" :options="scheduleDayOptions" @change="emit('sync-hotel-nights-count')" />
                  </Form.Item>
                  <Form.Item label="退房">
                    <Select v-model:value="props.form.scheduleEndDay" :options="scheduleDayOptions" @change="emit('sync-hotel-nights-count')" />
                  </Form.Item>
                  <Form.Item label="共几晚">
                    <InputNumber v-model:value="props.form.daysCount" disabled :min="0" :precision="0" />
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:users" />
                  <span>供应商</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="供应商" required>
                    <Select
                      v-model:value="props.form.supplierName"
                      allow-clear
                      show-search
                      :options="supplierOptions"
                      @change="(value) => emit('apply-selected-supplier', value)"
                    />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>价格信息</span>
                </div>
                <div class="traffic-price-list">
                  <div
                    v-for="(line, index) in props.form.priceLines"
                    :key="index"
                    class="traffic-price-line"
                  >
                    <Select
                      v-model:value="line.projectName"
                      show-search
                      :options="projectOptions"
                      @change="(value) => emit('apply-selected-price-project', index, value)"
                    />
                    <InputNumber
                      v-model:value="line.unitPrice"
                      addon-before="¥"
                      :min="0"
                      :precision="2"
                      @change="emit('sync-primary-price-fields')"
                    />
                    <div class="traffic-inline-number">
                      <span>*数量:</span>
                      <InputNumber
                        v-model:value="line.quantity"
                        :min="0"
                        :precision="0"
                        @change="emit('sync-primary-price-fields')"
                      />
                    </div>
                    <div class="traffic-price-remark">
                      <span>备注:</span>
                      <Textarea
                        v-model:value="line.remark"
                        :auto-size="{ minRows: 1, maxRows: 2 }"
                        placeholder="价格备注"
                        @change="emit('sync-primary-price-fields')"
                      />
                    </div>
                    <button
                      class="traffic-remove-line-button"
                      :class="{ disabled: props.form.priceLines.length <= 1 }"
                      :disabled="props.form.priceLines.length <= 1"
                      title="删除价格信息"
                      type="button"
                      @click="emit('remove-arrangement-price-line', index)"
                    >
                      <IconifyIcon icon="lucide:minus" />
                    </button>
                    <button
                      v-if="index === props.form.priceLines.length - 1"
                      class="traffic-add-line-button"
                      :class="{ disabled: showMultiOrderAveragePriceNotice }"
                      :disabled="showMultiOrderAveragePriceNotice"
                      :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'"
                      type="button"
                      @click="emit('add-arrangement-price-line')"
                    >
                      <IconifyIcon icon="lucide:plus" />
                    </button>
                  </div>
                </div>
                <div v-if="showMultiOrderAveragePriceNotice" class="traffic-price-lock-tip">
                  多订单均摊成本时，价格信息组成只能统一写成一条记录，点击 ⊕ 失效
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:wallet-cards" />
                  <span>结算方式</span>
                </div>
                <div class="traffic-settlement-grid">
                  <Form.Item label="合计">
                    <InputNumber :value="editorTotalAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="现结">
                    <InputNumber v-model:value="props.form.cashAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                  <Form.Item label="挂账">
                    <InputNumber :value="editorCreditAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="预付款">
                    <InputNumber v-model:value="props.form.prepaidAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                </div>
              </div>

              <Form.Item label="责任房调">
                <Select
                  v-model:value="props.form.responsibleEmployeeName"
                  allow-clear
                  show-search
                  :options="employeeOptions"
                  @change="(value) => emit('apply-selected-responsible', value)"
                />
              </Form.Item>

              <Form.Item label="订单信息">
                <Select
                  v-if="props.form.allocationMode === 'group_order_average'"
                  v-model:value="props.form.orderScope"
                  :options="mergedOrderOptions"
                />
                <Select
                  v-else
                  v-model:value="props.form.selectedOrderIds"
                  mode="multiple"
                  :options="orderOptions"
                  placeholder="请选择需要均摊的订单"
                />
                <Radio.Group
                  v-if="props.form.allocationMode === 'multi_order_average'"
                  v-model:value="props.form.multiOrderSplitMode"
                  class="order-split-mode"
                  option-type="button"
                  button-style="solid"
                >
                  <Radio.Button value="by_order">按订单均摊</Radio.Button>
                  <Radio.Button value="by_people">按人数均摊</Radio.Button>
                </Radio.Group>
                <div class="traffic-field-tip">{{ orderInfoTip }}</div>
              </Form.Item>

              <Form.Item label="备注信息">
                <Textarea v-model:value="props.form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" />
              </Form.Item>
            </div>
          </template>

          <template v-else-if="activeEditorType === 'vehicle'">
            <div class="vehicle-old-system-layout">
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:route" />
                  <span>用车时间</span>
                </div>
                <div class="traffic-form-row vehicle-time-row">
                  <Form.Item label="开始" required>
                    <Select v-model:value="props.form.scheduleStartDay" :options="scheduleDayOptions" @change="emit('sync-vehicle-days-count')" />
                  </Form.Item>
                  <Form.Item label="结束">
                    <Select v-model:value="props.form.scheduleEndDay" :options="scheduleDayOptions" @change="emit('sync-vehicle-days-count')" />
                  </Form.Item>
                  <Form.Item label="共几天">
                    <InputNumber v-model:value="props.form.daysCount" disabled :min="0" :precision="0" />
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group vehicle-quote-panel">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:map" />
                  <span>路书公里</span>
                </div>
                <div class="vehicle-roadbook-summary">
                  <div>
                    <span>同步公里</span>
                    <strong>{{ vehicleDistanceText(props.form.vehicleQuoteSnapshot?.syncedDistanceMeters) }}</strong>
                  </div>
                  <div>
                    <span>预计车程</span>
                    <strong>{{ routeDurationText(props.form.vehicleQuoteSnapshot?.syncedDurationSeconds) }}</strong>
                  </div>
                  <div class="vehicle-roadbook-actions">
                    <Space>
                      <Button @click="emit('sync-vehicle-roadbook-distance')">同步路书公里</Button>
                      <Button v-if="showProductVehicleAssistActions" type="primary" ghost @click="emit('open-product-roadbook-editor')">编辑路书地图</Button>
                    </Space>
                  </div>
                </div>
                <div class="vehicle-route-summary">
                  {{ props.form.vehicleQuoteSnapshot?.routeSummary || '先在产品行程里维护每天路书，再同步到用车报价。' }}
                </div>
              </div>

              <div class="traffic-field-group vehicle-quote-panel">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:calculator" />
                  <span>报价测算</span>
                </div>
                <div class="traffic-form-row three-columns">
                  <Form.Item label="规则座位数" required>
                    <Select
                      v-model:value="props.form.vehicleType"
                      allow-clear
                      show-search
                      :options="vehicleQuoteRuleOptions"
                      placeholder="请选择座位数规则"
                      @change="(value) => emit('sync-vehicle-price-project', value)"
                    />
                  </Form.Item>
                  <Form.Item label="测算参考价">
                    <InputNumber v-model:value="props.form.vehicleQuoteSnapshot.confirmedAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <Form.Item label="操作">
                    <Space>
                      <Button :loading="vehicleQuoteCalculating" @click="emit('calculate-vehicle-reference-price')">测算报价</Button>
                      <Button v-if="showProductVehicleAssistActions" type="primary" ghost @click="emit('apply-vehicle-quote-to-price-info')">应用到价格信息</Button>
                    </Space>
                  </Form.Item>
                </div>
                <div class="traffic-field-tip">
                  {{ lastVehicleQuoteResult ? `命中规则：${lastVehicleQuoteResult.ruleName}，距离 ${vehicleDistanceText(lastVehicleQuoteResult.distanceMeters)}` : '测算价只是询价参考，正式派车成本以后按实际确认。' }}
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:id-card" />
                  <span>司机车号</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="司机信息">
                    <AutoComplete
                      v-model:value="props.form.driverName"
                      allow-clear
                      :options="driverHistoryOptions"
                      placeholder="手动输入司机姓名/电话"
                      @search="(value) => emit('vehicle-history-search', 'driver_info', value)"
                    />
                  </Form.Item>
                  <Form.Item label="车牌号">
                    <AutoComplete
                      v-model:value="props.form.vehiclePlate"
                      allow-clear
                      :options="vehiclePlateHistoryOptions"
                      placeholder="手动输入车牌号"
                      @search="(value) => emit('vehicle-history-search', 'vehicle_plate', value)"
                    />
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:users" />
                  <span>供应商</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="供应商" required>
                    <Select
                      v-model:value="props.form.supplierName"
                      allow-clear
                      show-search
                      :options="supplierOptions"
                      @change="(value) => emit('apply-selected-supplier', value)"
                    />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>价格信息</span>
                </div>
                <div class="traffic-price-list">
                  <div v-for="(line, index) in props.form.priceLines" :key="index" class="traffic-price-line">
                    <Select v-model:value="line.projectName" show-search :options="projectOptions" @change="(value) => emit('apply-selected-price-project', index, value)" />
                    <InputNumber v-model:value="line.unitPrice" addon-before="¥" :min="0" :precision="2" @change="emit('sync-primary-price-fields')" />
                    <div class="traffic-inline-number">
                      <span>*数量:</span>
                      <InputNumber v-model:value="line.quantity" :min="0" :precision="0" @change="emit('sync-primary-price-fields')" />
                    </div>
                    <div class="traffic-price-remark">
                      <span>备注:</span>
                      <Textarea v-model:value="line.remark" :auto-size="{ minRows: 1, maxRows: 2 }" placeholder="价格备注" @change="emit('sync-primary-price-fields')" />
                    </div>
                    <button class="traffic-remove-line-button" :class="{ disabled: props.form.priceLines.length <= 1 }" :disabled="props.form.priceLines.length <= 1" title="删除价格信息" type="button" @click="emit('remove-arrangement-price-line', index)">
                      <IconifyIcon icon="lucide:minus" />
                    </button>
                    <button v-if="index === props.form.priceLines.length - 1" class="traffic-add-line-button" :class="{ disabled: showMultiOrderAveragePriceNotice }" :disabled="showMultiOrderAveragePriceNotice" :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'" type="button" @click="emit('add-arrangement-price-line')">
                      <IconifyIcon icon="lucide:plus" />
                    </button>
                  </div>
                </div>
                <div v-if="showMultiOrderAveragePriceNotice" class="traffic-price-lock-tip">
                  多订单均摊成本时，价格信息组成只能统一写成一条记录，点击 ⊕ 失效
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:wallet-cards" />
                  <span>结算方式</span>
                </div>
                <div class="traffic-settlement-grid">
                  <Form.Item label="合计">
                    <InputNumber :value="editorTotalAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="现结">
                    <InputNumber v-model:value="props.form.cashAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                  <Form.Item label="挂账">
                    <InputNumber :value="editorCreditAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="预付款">
                    <InputNumber v-model:value="props.form.prepaidAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                </div>
              </div>

              <Form.Item label="责任车调">
                <Select v-model:value="props.form.responsibleEmployeeName" allow-clear show-search :options="employeeOptions" @change="(value) => emit('apply-selected-responsible', value)" />
              </Form.Item>

              <Form.Item label="订单信息">
                <Select
                  v-if="props.form.allocationMode === 'group_order_average'"
                  v-model:value="props.form.orderScope"
                  :options="mergedOrderOptions"
                />
                <Select
                  v-else
                  v-model:value="props.form.selectedOrderIds"
                  mode="multiple"
                  :options="orderOptions"
                  placeholder="请选择需要均摊的订单"
                />
                <Radio.Group
                  v-if="props.form.allocationMode === 'multi_order_average'"
                  v-model:value="props.form.multiOrderSplitMode"
                  class="order-split-mode"
                  option-type="button"
                  button-style="solid"
                >
                  <Radio.Button value="by_order">按订单均摊</Radio.Button>
                  <Radio.Button value="by_people">按人数均摊</Radio.Button>
                </Radio.Group>
                <div class="traffic-field-tip">{{ orderInfoTip }}</div>
              </Form.Item>

              <Form.Item label="备注信息">
                <Textarea v-model:value="props.form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" />
              </Form.Item>
            </div>
          </template>

          <template v-else-if="activeEditorType === 'meal'">
            <div class="meal-old-system-layout">
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:utensils" />
                  <span>餐厅名称</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="餐厅名称">
                    <Select v-model:value="props.form.resourceName" allow-clear show-search :options="resourceOptions" @change="(value) => emit('apply-selected-resource', value)" />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:calendar-days" />
                  <span>用餐日期</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="用餐日期" required>
                    <Select v-model:value="props.form.scheduleStartDay" :options="scheduleDayOptions" />
                  </Form.Item>
                  <Form.Item label="时间">
                    <Select v-model:value="props.form.mealType" :options="mealTypeOptions" />
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:users" />
                  <span>供应商</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="供应商" required>
                    <Select v-model:value="props.form.supplierName" allow-clear show-search :options="supplierOptions" @change="(value) => emit('apply-selected-supplier', value)" />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>价格信息</span>
                </div>
                <div class="traffic-price-list">
                  <div v-for="(line, index) in props.form.priceLines" :key="index" class="traffic-price-line">
                    <Select v-model:value="line.projectName" show-search :options="projectOptionsForCurrentType()" @change="(value) => emit('apply-selected-price-project', index, value)" />
                    <InputNumber v-model:value="line.unitPrice" addon-before="¥" :min="0" :precision="2" @change="emit('sync-primary-price-fields')" />
                    <div class="traffic-inline-number">
                      <span>*数量:</span>
                      <InputNumber v-model:value="line.quantity" :min="0" :precision="0" @change="emit('sync-primary-price-fields')" />
                    </div>
                    <div class="traffic-price-remark">
                      <span>备注:</span>
                      <Textarea v-model:value="line.remark" :auto-size="{ minRows: 1, maxRows: 2 }" placeholder="价格备注" @change="emit('sync-primary-price-fields')" />
                    </div>
                    <button class="traffic-remove-line-button" :class="{ disabled: props.form.priceLines.length <= 1 }" :disabled="props.form.priceLines.length <= 1" title="删除价格信息" type="button" @click="emit('remove-arrangement-price-line', index)">
                      <IconifyIcon icon="lucide:minus" />
                    </button>
                    <button v-if="index === props.form.priceLines.length - 1" class="traffic-add-line-button" :class="{ disabled: showMultiOrderAveragePriceNotice }" :disabled="showMultiOrderAveragePriceNotice" :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'" type="button" @click="emit('add-arrangement-price-line')">
                      <IconifyIcon icon="lucide:plus" />
                    </button>
                  </div>
                </div>
                <div v-if="showMultiOrderAveragePriceNotice" class="traffic-price-lock-tip">
                  多订单均摊成本时，价格信息组成只能统一写成一条记录，点击 ⊕ 失效
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:wallet-cards" />
                  <span>结算方式</span>
                </div>
                <div class="traffic-settlement-grid">
                  <Form.Item label="合计">
                    <InputNumber :value="editorTotalAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="现结">
                    <InputNumber v-model:value="props.form.cashAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                  <Form.Item label="挂账">
                    <InputNumber :value="editorCreditAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="预付款">
                    <InputNumber v-model:value="props.form.prepaidAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                </div>
              </div>

              <Form.Item label="订单信息">
                <Select
                  v-if="props.form.allocationMode === 'group_order_average'"
                  v-model:value="props.form.orderScope"
                  :options="mergedOrderOptions"
                />
                <Select
                  v-else
                  v-model:value="props.form.selectedOrderIds"
                  mode="multiple"
                  :options="orderOptions"
                  placeholder="请选择需要均摊的订单"
                />
                <Radio.Group
                  v-if="props.form.allocationMode === 'multi_order_average'"
                  v-model:value="props.form.multiOrderSplitMode"
                  class="order-split-mode"
                  option-type="button"
                  button-style="solid"
                >
                  <Radio.Button value="by_order">按订单均摊</Radio.Button>
                  <Radio.Button value="by_people">按人数均摊</Radio.Button>
                </Radio.Group>
                <div class="traffic-field-tip">{{ orderInfoTip }}</div>
              </Form.Item>

              <Form.Item label="备注信息">
                <Textarea v-model:value="props.form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" />
              </Form.Item>
            </div>
          </template>

          <template v-else-if="activeEditorType === 'other'">
            <div class="other-old-system-layout">
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:calendar-days" />
                  <span>使用日期</span>
                </div>
                <div class="traffic-form-row one-column">
                  <Form.Item label="使用日期" required>
                    <Select v-model:value="props.form.scheduleStartDay" :options="scheduleDayOptions" />
                  </Form.Item>
                </div>
              </div>
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:users" />
                  <span>供应商</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="供应商" required>
                    <Select v-model:value="props.form.supplierName" allow-clear show-search :options="supplierOptions" @change="(value) => emit('apply-selected-supplier', value)" />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                  </Form.Item>
                </div>
              </div>
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>价格信息</span>
                </div>
                <div class="traffic-price-list">
                  <div v-for="(line, index) in props.form.priceLines" :key="index" class="traffic-price-line">
                    <Select v-model:value="line.projectName" show-search :options="projectOptionsForCurrentType()" @change="(value) => emit('apply-selected-price-project', index, value)" />
                    <InputNumber v-model:value="line.unitPrice" addon-before="¥" :min="0" :precision="2" @change="emit('sync-primary-price-fields')" />
                    <div class="traffic-inline-number">
                      <span>*数量:</span>
                      <InputNumber v-model:value="line.quantity" :min="0" :precision="0" @change="emit('sync-primary-price-fields')" />
                    </div>
                    <div class="traffic-price-remark">
                      <span>备注:</span>
                      <Textarea v-model:value="line.remark" :auto-size="{ minRows: 1, maxRows: 2 }" placeholder="价格备注" @change="emit('sync-primary-price-fields')" />
                    </div>
                    <button class="traffic-remove-line-button" :class="{ disabled: props.form.priceLines.length <= 1 }" :disabled="props.form.priceLines.length <= 1" title="删除价格信息" type="button" @click="emit('remove-arrangement-price-line', index)">
                      <IconifyIcon icon="lucide:minus" />
                    </button>
                    <button v-if="index === props.form.priceLines.length - 1" class="traffic-add-line-button" :class="{ disabled: showMultiOrderAveragePriceNotice }" :disabled="showMultiOrderAveragePriceNotice" :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'" type="button" @click="emit('add-arrangement-price-line')">
                      <IconifyIcon icon="lucide:plus" />
                    </button>
                  </div>
                </div>
              </div>
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:wallet-cards" />
                  <span>结算方式</span>
                </div>
                <div class="traffic-settlement-grid">
                  <Form.Item label="合计">
                    <InputNumber :value="editorTotalAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="现结">
                    <InputNumber v-model:value="props.form.cashAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                  <Form.Item label="挂账">
                    <InputNumber :value="editorCreditAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="预付款">
                    <InputNumber v-model:value="props.form.prepaidAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                </div>
              </div>
              <Form.Item label="订单信息">
                <Select
                  v-if="props.form.allocationMode === 'group_order_average'"
                  v-model:value="props.form.orderScope"
                  :options="mergedOrderOptions"
                />
                <Select
                  v-else
                  v-model:value="props.form.selectedOrderIds"
                  mode="multiple"
                  :options="orderOptions"
                  placeholder="请选择需要均摊的订单"
                />
                <Radio.Group
                  v-if="props.form.allocationMode === 'multi_order_average'"
                  v-model:value="props.form.multiOrderSplitMode"
                  class="order-split-mode"
                  option-type="button"
                  button-style="solid"
                >
                  <Radio.Button value="by_order">按订单均摊</Radio.Button>
                  <Radio.Button value="by_people">按人数均摊</Radio.Button>
                </Radio.Group>
                <div class="traffic-field-tip">{{ orderInfoTip }}</div>
              </Form.Item>
              <Form.Item label="备注信息">
                <Textarea v-model:value="props.form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" />
              </Form.Item>
            </div>
          </template>

          <template v-else-if="activeEditorType === 'optional'">
            <div class="optional-old-system-layout">
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:landmark" />
                  <span>景区/项目名称</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="景区/项目名称">
                    <Select v-model:value="props.form.resourceName" allow-clear show-search :options="resourceOptions" @change="(value) => emit('apply-selected-resource', value)" />
                  </Form.Item>
                  <Form.Item label="游玩日期" required>
                    <Select v-model:value="props.form.scheduleStartDay" :options="scheduleDayOptions" />
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:users" />
                  <span>供应商</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="供应商" required>
                    <Select v-model:value="props.form.supplierName" allow-clear show-search :options="supplierOptions" @change="(value) => emit('apply-selected-supplier', value)" />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                  </Form.Item>
                </div>
                <div class="scenic-template-status">
                  <div>
                    <span>游客名单模板：</span>
                    <Tag v-if="scenicTicketTemplateLoading" color="blue">读取中</Tag>
                    <Tag v-else-if="scenicTicketTemplate" color="green">已配置</Tag>
                    <Tag v-else color="orange">未配置</Tag>
                  <strong v-if="scenicTicketTemplate">{{ scenicTicketTemplate.templateName }}</strong>
                </div>
                  <Space size="small">
                    <Button v-if="showScenicTicketDownload" size="small" @click="emit('download-scenic-ticket-guests')">
                      下载游客Excel
                    </Button>
                    <Button size="small" @click="emit('open-scenic-template-config-page')">配置模板</Button>
                  </Space>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>价格信息</span>
                </div>
                <div class="traffic-form-row three-columns">
                  <Form.Item label="销售价">
                    <InputNumber v-model:value="props.form.saleAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <Form.Item label="成本价">
                    <InputNumber v-model:value="props.form.costAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <div class="old-system-combined-field">
                    <div class="old-system-combined-title">导游提成</div>
                    <div class="old-system-combined-controls optional-commission-controls">
                      <InputNumber v-model:value="props.form.guideCommissionAmount" :min="0" :precision="2" />
                      <span>元/人</span>
                      <span>或</span>
                      <InputNumber v-model:value="props.form.guideCommissionRate" :min="0" :precision="2" />
                      <span>%</span>
                    </div>
                  </div>
                </div>
                <div class="optional-fee-summary">
                  <div class="optional-fee-summary-title">费用合计</div>
                  <div v-for="(line, index) in props.form.priceLines" :key="`optional-${index}`" class="optional-fee-summary-line optional-fee-summary-row">
                    <label>人数：</label>
                    <InputNumber v-model:value="line.quantity" :min="0" :precision="0" @change="emit('sync-optional-line-to-form')" />
                    <label>收入：</label>
                    <InputNumber v-model:value="line.salePrice" :min="0" :precision="2" @change="emit('sync-optional-line-to-form')" />
                    <label>成本：</label>
                    <InputNumber v-model:value="line.costPrice" :min="0" :precision="2" @change="emit('sync-optional-line-to-form')" />
                    <label>现结：</label>
                    <InputNumber v-model:value="line.cashAmount" :min="0" :precision="2" @change="emit('sync-optional-line-to-form')" />
                    <label>挂账：</label>
                    <InputNumber :value="optionalLineCreditAmount(line)" disabled :precision="2" />
                    <label>提成：</label>
                    <InputNumber v-model:value="line.guideCommissionAmount" :min="0" :precision="2" @change="emit('sync-optional-line-to-form')" />
                    <button class="traffic-remove-line-button" :class="{ disabled: props.form.priceLines.length <= 1 }" :disabled="props.form.priceLines.length <= 1" title="删除费用合计" type="button" @click="emit('remove-optional-summary-line', index)">
                      <IconifyIcon icon="lucide:minus" />
                    </button>
                    <button v-if="index === props.form.priceLines.length - 1" class="optional-add-summary-button" title="添加费用合计" type="button" @click="emit('add-optional-summary-line')">
                      <IconifyIcon icon="lucide:plus" />
                    </button>
                  </div>
                </div>
              </div>

              <Form.Item label="订单信息">
                <Select
                  v-if="props.form.allocationMode === 'group_order_average'"
                  v-model:value="props.form.orderScope"
                  :options="mergedOrderOptions"
                />
                <Select
                  v-else
                  v-model:value="props.form.selectedOrderIds"
                  mode="multiple"
                  :options="orderOptions"
                  placeholder="请选择需要均摊的订单"
                />
                <Radio.Group
                  v-if="props.form.allocationMode === 'multi_order_average'"
                  v-model:value="props.form.multiOrderSplitMode"
                  class="order-split-mode"
                  option-type="button"
                  button-style="solid"
                >
                  <Radio.Button value="by_order">按订单均摊</Radio.Button>
                  <Radio.Button value="by_people">按人数均摊</Radio.Button>
                </Radio.Group>
                <div class="traffic-field-tip">{{ orderInfoTip }}</div>
              </Form.Item>
              <Form.Item label="备注信息">
                <Textarea v-model:value="props.form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" />
              </Form.Item>
            </div>
          </template>

          <template v-else-if="activeEditorType === 'shopping'">
            <div class="shopping-old-system-layout">
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:store" />
                  <span>购物店</span>
                </div>
                <div class="shopping-compact-grid shopping-shop-row">
                  <Form.Item label="购物店">
                    <Select v-model:value="props.form.resourceName" allow-clear show-search :options="resourceOptions" @change="(value) => emit('apply-selected-resource', value)" />
                  </Form.Item>
                  <Form.Item label="购物日期" required>
                    <Select v-model:value="props.form.scheduleStartDay" :options="scheduleDayOptions" />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-resource-create-page')">添加购物店</Button>
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:users" />
                  <span>供应商</span>
                </div>
                <div class="shopping-compact-grid shopping-supplier-row">
                  <Form.Item label="供应商" required>
                    <Select v-model:value="props.form.supplierName" allow-clear show-search :options="supplierOptions" @change="(value) => emit('apply-selected-supplier', value)" />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:settings-2" />
                  <span>费用设置</span>
                </div>
                <div class="shopping-fee-row">
                  <Form.Item label="人数">
                    <InputNumber v-model:value="props.form.peopleCount" :min="0" :precision="0" />
                  </Form.Item>
                  <Form.Item label="人头费">
                    <InputNumber v-model:value="props.form.headFeeAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <Form.Item label="公司返佣">
                    <InputNumber v-model:value="props.form.companyRebateAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <Form.Item label="导游提成">
                    <InputNumber v-model:value="props.form.guideCommissionAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>消费详情</span>
                </div>
                <div v-for="(line, index) in props.form.priceLines" :key="`shopping-${index}`" class="shopping-consumption-line">
                  <div class="shopping-consumption-main-row">
                    <Form.Item label="品类">
                      <Select v-model:value="line.projectName" show-search :options="shoppingCategoryOptions" @change="(value) => emit('apply-shopping-price-project', index, value)" />
                    </Form.Item>
                    <Form.Item label="消费金额">
                      <InputNumber v-model:value="line.consumptionAmount" addon-before="¥" :min="0" :precision="2" @change="emit('sync-shopping-line-to-form')" />
                    </Form.Item>
                    <div class="shopping-formula-field">
                      <div class="shopping-formula-label">公司返佣</div>
                      <div class="shopping-formula-controls">
                        <InputNumber v-model:value="line.companyRebateRate" :min="0" :precision="2" @change="emit('sync-shopping-line-to-form')" />
                        <span>% =</span>
                        <InputNumber v-model:value="line.companyRebateAmount" addon-before="¥" :min="0" :precision="2" @change="emit('sync-shopping-line-to-form')" />
                      </div>
                    </div>
                    <div class="shopping-formula-field">
                      <div class="shopping-formula-label">导游提成</div>
                      <div class="shopping-formula-controls">
                        <InputNumber v-model:value="line.guideCommissionRate" :min="0" :precision="2" @change="emit('sync-shopping-line-to-form')" />
                        <span>%销售额 =</span>
                        <InputNumber v-model:value="line.guideCommissionAmount" addon-before="¥" :min="0" :precision="2" @change="emit('sync-shopping-line-to-form')" />
                      </div>
                    </div>
                  </div>
                  <div class="shopping-consumption-extra-row">
                    <Form.Item label="现结">
                      <InputNumber v-model:value="line.cashAmount" addon-before="¥" :min="0" :precision="2" @change="emit('sync-shopping-line-to-form')" />
                    </Form.Item>
                    <Form.Item label="备注">
                      <Input v-model:value="line.remark" placeholder="消费备注" @change="emit('sync-shopping-line-to-form')" />
                    </Form.Item>
                    <button class="traffic-remove-line-button" :class="{ disabled: props.form.priceLines.length <= 1 }" :disabled="props.form.priceLines.length <= 1" title="删除消费详情" type="button" @click="emit('remove-shopping-consumption-line', index)">
                      <IconifyIcon icon="lucide:minus" />
                    </button>
                    <button
                      v-if="index === props.form.priceLines.length - 1"
                      class="shopping-add-detail-button"
                      :class="{ disabled: showMultiOrderAveragePriceNotice }"
                      :disabled="showMultiOrderAveragePriceNotice"
                      :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条消费详情' : '添加消费详情'"
                      type="button"
                      @click="emit('add-shopping-consumption-line')"
                    >
                      <IconifyIcon icon="lucide:plus" />
                    </button>
                  </div>
                </div>
                <div v-if="showMultiOrderAveragePriceNotice" class="traffic-price-lock-tip">
                  多订单均摊成本时，价格信息组成只能统一写成一条记录，点击 ⊕ 失效
                </div>
              </div>

              <Form.Item label="订单信息">
                <Select
                  v-if="props.form.allocationMode === 'group_order_average'"
                  v-model:value="props.form.orderScope"
                  :options="mergedOrderOptions"
                />
                <Select
                  v-else
                  v-model:value="props.form.selectedOrderIds"
                  mode="multiple"
                  :options="orderOptions"
                  placeholder="请选择需要均摊的订单"
                />
                <Radio.Group
                  v-if="props.form.allocationMode === 'multi_order_average'"
                  v-model:value="props.form.multiOrderSplitMode"
                  class="order-split-mode"
                  option-type="button"
                  button-style="solid"
                >
                  <Radio.Button value="by_order">按订单均摊</Radio.Button>
                  <Radio.Button value="by_people">按人数均摊</Radio.Button>
                </Radio.Group>
                <div class="traffic-field-tip">{{ orderInfoTip }}</div>
              </Form.Item>

              <Form.Item label="备注信息">
                <Textarea v-model:value="props.form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" />
              </Form.Item>
            </div>
          </template>

          <template v-else-if="activeEditorType === 'ground_agent'">
            <div class="ground-agent-old-system-layout">
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:calendar-days" />
                  <span>拼团日期</span>
                </div>
                <div class="traffic-form-row three-columns">
                  <Form.Item label="开始" required>
                    <Select v-model:value="props.form.scheduleStartDay" :options="scheduleDayOptions" @change="emit('sync-ground-agent-days-count')" />
                  </Form.Item>
                  <Form.Item label="结束">
                    <Select v-model:value="props.form.scheduleEndDay" :options="scheduleDayOptions" @change="emit('sync-ground-agent-days-count')" />
                  </Form.Item>
                  <Form.Item label="共几天">
                    <InputNumber v-model:value="props.form.daysCount" disabled :min="0" :precision="0" />
                  </Form.Item>
                </div>
              </div>
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:users" />
                  <span>供应商</span>
                </div>
                <div class="traffic-form-row two-columns">
                  <Form.Item label="供应商" required>
                    <Select v-model:value="props.form.supplierName" allow-clear show-search :options="supplierOptions" @change="(value) => emit('apply-selected-supplier', value)" />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                  </Form.Item>
                </div>
              </div>
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>地接结算价</span>
                </div>
                <div class="traffic-form-row one-column">
                  <Form.Item label="地接结算价" required>
                    <InputNumber v-model:value="props.form.costAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                </div>
              </div>
              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:wallet-cards" />
                  <span>结算方式</span>
                </div>
                <div class="traffic-settlement-grid">
                  <Form.Item label="合计">
                    <InputNumber :value="editorTotalAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="现结">
                    <InputNumber v-model:value="props.form.cashAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                  <Form.Item label="挂账">
                    <InputNumber :value="editorCreditAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="预付款">
                    <InputNumber v-model:value="props.form.prepaidAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                  </Form.Item>
                </div>
              </div>
              <Form.Item label="订单信息">
                <Select
                  v-if="props.form.allocationMode === 'group_order_average'"
                  v-model:value="props.form.orderScope"
                  :options="mergedOrderOptions"
                />
                <Select
                  v-else
                  v-model:value="props.form.selectedOrderIds"
                  mode="multiple"
                  :options="orderOptions"
                  placeholder="请选择需要均摊的订单"
                />
                <Radio.Group
                  v-if="props.form.allocationMode === 'multi_order_average'"
                  v-model:value="props.form.multiOrderSplitMode"
                  class="order-split-mode"
                  option-type="button"
                  button-style="solid"
                >
                  <Radio.Button value="by_order">按订单均摊</Radio.Button>
                  <Radio.Button value="by_people">按人数均摊</Radio.Button>
                </Radio.Group>
                <div class="traffic-field-tip">{{ orderInfoTip }}</div>
              </Form.Item>
              <Form.Item label="备注信息">
                <Textarea v-model:value="props.form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" />
              </Form.Item>
            </div>
          </template>

          <template v-else>
            <div v-if="activeEditorConfig.showTrafficType" class="traffic-form-row one-column">
              <Form.Item label="交通类型" required>
                <Select v-model:value="props.form.trafficType" :options="trafficTypeOptions" />
              </Form.Item>
            </div>

            <div class="traffic-field-group">
              <div class="traffic-group-title">
                <IconifyIcon icon="lucide:route" />
                <span>{{ activeEditorConfig.scheduleGroupLabel }}</span>
              </div>
              <div class="traffic-form-row three-columns">
                <Form.Item :label="activeEditorConfig.startLabel" required>
                  <Select v-model:value="props.form.scheduleStartDay" :options="scheduleDayOptions" />
                </Form.Item>
                <Form.Item v-if="activeEditorConfig.showEndDate" :label="activeEditorConfig.endLabel">
                  <Select v-model:value="props.form.scheduleEndDay" :options="scheduleDayOptions" />
                </Form.Item>
                <Form.Item v-if="activeEditorConfig.showArrivalPlace" label="出发地" required>
                  <Cascader v-model:value="departureRegionPathModel" allow-clear change-on-select :options="regionOptions" placeholder="请选择出发地" show-search />
                </Form.Item>
                <Form.Item v-if="activeEditorConfig.showArrivalPlace" label="抵达地" required>
                  <Cascader v-model:value="arrivalRegionPathModel" allow-clear change-on-select :options="regionOptions" placeholder="请选择抵达地" show-search />
                </Form.Item>
                <Form.Item v-if="activeEditorConfig.showDaysCount" :label="activeEditorConfig.daysLabel">
                  <InputNumber v-model:value="props.form.daysCount" :min="0" :precision="0" />
                </Form.Item>
              </div>
            </div>

            <div v-if="activeEditorConfig.resourceMode === 'select'" class="traffic-field-group">
              <div class="traffic-group-title">
                <IconifyIcon icon="lucide:database" />
                <span>{{ activeEditorConfig.resourceLabel }}</span>
              </div>
              <div class="traffic-form-row two-columns">
                <Form.Item :label="activeEditorConfig.resourceLabel">
                  <Select
                    v-model:value="props.form.resourceName"
                    allow-clear
                    show-search
                    :options="resourceOptions"
                    @change="(value) => emit('apply-selected-resource', value)"
                  />
                </Form.Item>
                <Form.Item label="添加">
                  <Button @click="emit('open-resource-create-page')">添加资源</Button>
                </Form.Item>
              </div>
            </div>

            <div class="traffic-field-group">
              <div class="traffic-group-title">
                <IconifyIcon icon="lucide:users" />
                <span>供应商</span>
              </div>
              <div class="traffic-form-row two-columns">
                <Form.Item label="供应商" required>
                  <Select v-model:value="props.form.supplierName" allow-clear show-search :options="supplierOptions" @change="(value) => emit('apply-selected-supplier', value)" />
                </Form.Item>
                <Form.Item label="添加">
                  <Button @click="emit('open-supplier-create-page')">添加供应商</Button>
                </Form.Item>
              </div>
              <div v-if="activeEditorType === 'scenic'" class="scenic-template-status">
                <div>
                  <span>游客名单模板：</span>
                  <Tag v-if="scenicTicketTemplateLoading" color="blue">读取中</Tag>
                  <Tag v-else-if="scenicTicketTemplate" color="green">已配置</Tag>
                  <Tag v-else color="orange">未配置</Tag>
                  <strong v-if="scenicTicketTemplate">{{ scenicTicketTemplate.templateName }}</strong>
                </div>
                <Space size="small">
                  <Button v-if="showScenicTicketDownload" size="small" @click="emit('download-scenic-ticket-guests')">
                    下载游客Excel
                  </Button>
                  <Button size="small" @click="emit('open-scenic-template-config-page')">配置模板</Button>
                </Space>
              </div>
            </div>

            <div class="traffic-field-group">
              <div class="traffic-group-title">
                <IconifyIcon icon="lucide:receipt-text" />
                <span>价格信息</span>
              </div>
              <div class="traffic-price-list">
                <div v-for="(line, index) in props.form.priceLines" :key="index" class="traffic-price-line">
                  <Select v-model:value="line.projectName" show-search :options="projectOptions" @change="(value) => emit('apply-selected-price-project', index, value)" />
                  <InputNumber v-model:value="line.unitPrice" addon-before="¥" :min="0" :precision="2" @change="emit('sync-primary-price-fields')" />
                  <div class="traffic-inline-number">
                    <span>*数量:</span>
                    <InputNumber v-model:value="line.quantity" :min="0" :precision="0" @change="emit('sync-primary-price-fields')" />
                  </div>
                  <div class="traffic-price-remark">
                    <span>备注:</span>
                    <Textarea v-model:value="line.remark" :auto-size="{ minRows: 1, maxRows: 2 }" placeholder="价格备注" @change="emit('sync-primary-price-fields')" />
                  </div>
                  <button class="traffic-remove-line-button" :class="{ disabled: props.form.priceLines.length <= 1 }" :disabled="props.form.priceLines.length <= 1" title="删除价格信息" type="button" @click="emit('remove-arrangement-price-line', index)">
                    <IconifyIcon icon="lucide:minus" />
                  </button>
                  <button v-if="index === props.form.priceLines.length - 1" class="traffic-add-line-button" :class="{ disabled: showMultiOrderAveragePriceNotice }" :disabled="showMultiOrderAveragePriceNotice" :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'" type="button" @click="emit('add-arrangement-price-line')">
                    <IconifyIcon icon="lucide:plus" />
                  </button>
                </div>
              </div>
              <div v-if="showMultiOrderAveragePriceNotice" class="traffic-price-lock-tip">
                多订单均摊成本时，价格信息组成只能统一写成一条记录，点击 ⊕ 失效
              </div>
            </div>

            <div v-if="activeEditorConfig.settlement" class="traffic-field-group">
              <div class="traffic-group-title">
                <IconifyIcon icon="lucide:wallet-cards" />
                <span>结算方式</span>
              </div>
              <div class="traffic-settlement-grid">
                <Form.Item label="合计">
                  <InputNumber :value="editorTotalAmount" disabled addon-before="¥" :precision="2" />
                </Form.Item>
                <Form.Item label="现结">
                  <InputNumber v-model:value="props.form.cashAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                </Form.Item>
                <Form.Item label="挂账">
                  <InputNumber :value="editorCreditAmount" disabled addon-before="¥" :precision="2" />
                </Form.Item>
                <Form.Item label="预付款">
                  <InputNumber v-model:value="props.form.prepaidAmount" addon-before="¥" :min="0" :max="editorTotalAmount" :precision="2" />
                </Form.Item>
              </div>
            </div>

            <Form.Item v-if="activeEditorConfig.showOrderInfo" label="订单信息">
              <Select
                v-if="props.form.allocationMode === 'group_order_average'"
                v-model:value="props.form.orderScope"
                :options="mergedOrderOptions"
              />
              <Select
                v-else
                v-model:value="props.form.selectedOrderIds"
                mode="multiple"
                :options="orderOptions"
                placeholder="请选择需要均摊的订单"
              />
              <Radio.Group
                v-if="props.form.allocationMode === 'multi_order_average'"
                v-model:value="props.form.multiOrderSplitMode"
                class="order-split-mode"
                option-type="button"
                button-style="solid"
              >
                <Radio.Button value="by_order">按订单均摊</Radio.Button>
                <Radio.Button value="by_people">按人数均摊</Radio.Button>
              </Radio.Group>
              <div class="traffic-field-tip">{{ orderInfoTip }}</div>
            </Form.Item>

            <Form.Item label="备注信息">
              <Textarea v-model:value="props.form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" />
            </Form.Item>
          </template>
        </Form>

        <div class="traffic-modal-footer">
          <Button @click="emit('close')">取消</Button>
          <Checkbox v-if="activeEditorConfig.noGuideReport" v-model:checked="props.form.noGuideReport" class="traffic-sync-checkbox">
            无需导游报账，同步更新导游报账和计调审核数据
          </Checkbox>
          <Button type="primary" :loading="saving" @click="emit('save')">
            {{ submitButtonText }}
          </Button>
        </div>
      </div>
    </Spin>
  </Modal>
</template>

<style scoped>
.traffic-modal-body {
  color: #0f172a;
}

.traffic-arrangement-modal :deep(.ant-modal-content) {
  overflow: hidden;
  border-radius: 8px;
}

.traffic-arrangement-modal :deep(.ant-modal-header) {
  padding: 16px 20px 12px;
  margin-bottom: 0;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
  border-bottom: 1px solid #e2e8f0;
}

.traffic-arrangement-modal :deep(.ant-modal-title) {
  font-size: 17px;
  font-weight: 900;
  color: #0f172a;
}

.traffic-arrangement-modal :deep(.ant-modal-body) {
  padding: 16px 20px 18px;
}

.traffic-cost-mode-tabs {
  margin-bottom: 14px;
}

.traffic-cost-mode-tabs :deep(.ant-radio-button-wrapper) {
  height: 34px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 800;
  line-height: 32px;
}

.traffic-cost-mode-tabs :deep(.ant-radio-button-wrapper-checked) {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
}

.traffic-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.traffic-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.traffic-form :deep(.ant-form-item-label) {
  padding-bottom: 5px;
}

.traffic-form :deep(.ant-form-item-label > label) {
  height: auto;
  font-size: 12.5px;
  font-weight: 800;
  color: #334155;
}

.traffic-form :deep(.ant-select-selector),
.traffic-form :deep(.ant-input-number),
.traffic-form :deep(.ant-input) {
  border-color: #dbe4f0;
  border-radius: 5px;
}

.traffic-form-row {
  display: grid;
  gap: 12px;
  align-items: end;
}

.traffic-form-row.two-columns {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.traffic-form-row.one-column {
  grid-template-columns: minmax(0, 1fr);
}

.traffic-form-row.three-columns {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.traffic-form-row.hotel-name-row {
  grid-template-columns: minmax(0, 1fr) 96px 140px minmax(180px, 1fr);
}

.traffic-form-row.vehicle-time-row {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 120px;
}

.old-system-combined-field {
  min-width: 0;
}

.old-system-combined-title {
  margin-bottom: 5px;
  font-size: 12.5px;
  font-weight: 800;
  color: #334155;
}

.old-system-combined-controls {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 6px;
  align-items: center;
}

.old-system-combined-controls.optional-commission-controls {
  grid-template-columns: minmax(0, 1fr) auto auto minmax(0, 1fr) auto;
}

.old-system-combined-controls > span {
  font-size: 12.5px;
  font-weight: 800;
  color: #475569;
}

.traffic-field-group {
  padding: 12px;
  background: #fbfdff;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.traffic-group-title {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
}

.traffic-group-title svg {
  width: 16px;
  height: 16px;
  color: #1677ff;
}

.traffic-price-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.traffic-price-line {
  display: grid;
  grid-template-columns: 150px 150px 140px minmax(180px, 1fr) 32px 32px;
  gap: 10px;
  align-items: center;
}

.traffic-price-line :deep(.ant-input-number) {
  width: 100%;
}

.traffic-inline-number,
.traffic-price-remark {
  display: flex;
  gap: 6px;
  align-items: center;
}

.traffic-inline-number > span,
.traffic-price-remark > span {
  flex: 0 0 auto;
  font-size: 12.5px;
  font-weight: 800;
  color: #475569;
}

.traffic-add-line-button,
.traffic-remove-line-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: #1677ff;
  cursor: pointer;
  background: #fff;
  border: 1px solid #91caff;
  border-radius: 50%;
}

.traffic-remove-line-button {
  color: #ea580c;
  border-color: #fdba74;
}

.traffic-add-line-button.disabled,
.traffic-add-line-button:disabled,
.traffic-remove-line-button.disabled,
.traffic-remove-line-button:disabled {
  color: #94a3b8;
  cursor: not-allowed;
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.traffic-add-line-button svg,
.traffic-remove-line-button svg {
  width: 16px;
  height: 16px;
}

.traffic-price-lock-tip {
  display: inline-flex;
  align-items: center;
  padding: 7px 10px;
  margin-top: 8px;
  font-size: 12.5px;
  font-weight: 800;
  color: #b42318;
  background: #fff1f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
}

.traffic-settlement-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.traffic-settlement-grid :deep(.ant-input-number) {
  width: 100%;
}

.traffic-field-tip {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #16a34a;
}

.order-split-mode {
  display: block;
  margin-top: 8px;
}

.order-split-mode :deep(.ant-radio-button-wrapper) {
  height: 30px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 800;
  line-height: 28px;
}

.scenic-template-status {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  margin-top: 10px;
  font-size: 12.5px;
  font-weight: 700;
  color: #475569;
  background: #fff;
  border: 1px dashed #bfdbfe;
  border-radius: 6px;
}

.scenic-template-status strong {
  margin-left: 6px;
  color: #0f172a;
}

.vehicle-roadbook-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  gap: 10px;
  align-items: center;
}

.vehicle-roadbook-summary > div {
  padding: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.vehicle-roadbook-summary span {
  display: block;
  font-size: 12px;
  font-weight: 800;
  color: #64748b;
}

.vehicle-roadbook-summary strong {
  font-size: 16px;
  color: #0f172a;
}

.vehicle-roadbook-actions {
  padding: 0 !important;
  background: transparent !important;
  border: 0 !important;
}

.vehicle-route-summary {
  padding: 8px 10px;
  margin-top: 8px;
  font-size: 12.5px;
  font-weight: 700;
  color: #475569;
  background: #f8fafc;
  border-radius: 6px;
}

.optional-fee-summary {
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px dashed #cbd5e1;
}

.optional-fee-summary-title {
  margin-bottom: 9px;
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
}

.optional-fee-summary-row {
  display: grid;
  grid-template-columns:
    auto minmax(72px, 1fr)
    auto minmax(72px, 1fr)
    auto minmax(72px, 1fr)
    auto minmax(72px, 1fr)
    auto minmax(72px, 1fr)
    auto minmax(72px, 1fr)
    34px
    34px;
  gap: 8px;
  align-items: center;
}

.optional-fee-summary-row label {
  font-size: 12.5px;
  font-weight: 800;
  color: #475569;
  white-space: nowrap;
}

.optional-fee-summary-row :deep(.ant-input-number) {
  width: 100%;
}

.optional-add-summary-button,
.shopping-add-detail-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #1677ff;
  cursor: pointer;
  background: #fff;
  border: 1px solid #91caff;
  border-radius: 50%;
}

.optional-add-summary-button svg,
.shopping-add-detail-button svg {
  width: 17px;
  height: 17px;
}

.shopping-compact-grid,
.shopping-fee-row,
.shopping-consumption-main-row,
.shopping-consumption-extra-row {
  display: grid;
  gap: 12px;
  align-items: end;
}

.shopping-shop-row {
  grid-template-columns: minmax(0, 1fr) 180px 120px;
}

.shopping-supplier-row {
  grid-template-columns: minmax(0, 1fr) 120px;
}

.shopping-fee-row {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.shopping-consumption-main-row {
  grid-template-columns: minmax(120px, 0.9fr) minmax(120px, 0.9fr) minmax(210px, 1.35fr) minmax(240px, 1.55fr);
}

.shopping-consumption-extra-row {
  grid-template-columns: minmax(120px, 0.8fr) minmax(0, 1fr) 34px 34px;
  margin-top: 12px;
}

.shopping-consumption-line + .shopping-consumption-line {
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px dashed #cbd5e1;
}

.shopping-formula-field {
  min-width: 0;
}

.shopping-formula-label {
  margin-bottom: 5px;
  font-size: 12.5px;
  font-weight: 800;
  color: #334155;
}

.shopping-formula-controls {
  display: grid;
  grid-template-columns: minmax(58px, 0.7fr) auto minmax(80px, 1fr);
  gap: 6px;
  align-items: center;
}

.shopping-formula-controls span {
  font-size: 12.5px;
  font-weight: 800;
  color: #475569;
  white-space: nowrap;
}

.shopping-formula-controls :deep(.ant-input-number),
.shopping-consumption-main-row :deep(.ant-input-number),
.shopping-consumption-extra-row :deep(.ant-input-number),
.shopping-fee-row :deep(.ant-input-number) {
  width: 100%;
}

.shopping-consumption-extra-row :deep(.ant-input) {
  width: 100%;
}

.traffic-modal-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
  padding-top: 14px;
  margin-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.traffic-sync-checkbox {
  margin-right: auto;
  font-size: 12.5px;
  font-weight: 700;
  color: #16a34a;
}

.traffic-sync-checkbox :deep(.ant-checkbox + span) {
  color: #16a34a;
}

@media (max-width: 900px) {
  .traffic-form-row.two-columns,
  .traffic-form-row.three-columns,
  .traffic-form-row.hotel-name-row,
  .traffic-form-row.vehicle-time-row,
  .traffic-settlement-grid,
  .traffic-price-line,
  .vehicle-roadbook-summary,
  .old-system-combined-controls,
  .old-system-combined-controls.optional-commission-controls,
  .optional-fee-summary-row,
  .shopping-compact-grid,
  .shopping-fee-row,
  .shopping-consumption-main-row,
  .shopping-consumption-extra-row,
  .shopping-formula-controls {
    grid-template-columns: 1fr;
  }

  .traffic-modal-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .traffic-sync-checkbox {
    margin-right: 0;
  }
}
</style>
