<script lang="ts" setup>
import type { SalesProductApi } from '#/api/sales/product';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Checkbox,
  Form,
  InputNumber,
  Input,
  Modal,
  Radio,
  Select,
  Spin,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  getSalesProductDetail,
  updateSalesProduct,
} from '#/api/sales/product';
import { getExpenseItemAll } from '#/api/enterprise/expense-item';
import { getEnterpriseEmployeeAll } from '#/api/enterprise/employee';
import { getGroundAgentPage } from '#/api/purchase/ground-agent';
import { getHotelResourcePage } from '#/api/purchase/hotel';
import { getPurchaseResourcePage } from '#/api/purchase/resource';
import { getScenicResourcePage } from '#/api/purchase/scenic';
import { getSupplierAll, type SupplierApi } from '#/api/purchase/supplier';

import {
  buildSalesProductPayload,
  arrangementItemCash,
  arrangementItemCredit,
  arrangementItemTotal,
  calculateArrangementTotal,
  createArrangementOverviewSummary,
  createDefaultProductForm,
  type ProductFormState,
} from './product-form-utils';

type ArrangementCategoryShortcut = {
  icon: string;
  label: string;
  anchor: string;
  value: SalesProductApi.ArrangementType;
};

type ArrangementShortcut =
  | ArrangementCategoryShortcut
  | {
    icon: string;
    label: string;
    anchor: string;
    value: 'overview';
  };

type ArrangementStage = {
  label: string;
  state: 'done' | 'pending' | 'template';
};

type TeamBadgeItem = {
  color?: string;
  label: string;
  value: string;
};

type TeamMetricItem = {
  label: string;
  value: string;
};

type ArrangementSection = ArrangementCategoryShortcut & {
  columns: string[];
  documentAction?: string;
};

type SelectOption = {
  label: string;
  value: string;
};

type SelectOptionWithId = {
  id?: number;
  label: string;
  value: string;
};

type ArrangementEditorConfig = {
  daysLabel?: string;
  endLabel?: string;
  noGuideReport: boolean;
  peopleLabel?: string;
  resourceLabel?: string;
  resourceMode: 'none' | 'select';
  responsibleLabel?: string;
  scheduleGroupLabel: string;
  settlement: boolean;
  showArrivalPlace?: boolean;
  showBreakfastFund?: boolean;
  showConfirmed?: boolean;
  showDaysCount?: boolean;
  showDriver?: boolean;
  showEndDate?: boolean;
  showMealTime?: boolean;
  showOptionalAmounts?: boolean;
  showOrderInfo: boolean;
  showPeople?: boolean;
  showResponsible?: boolean;
  showShoppingAmounts?: boolean;
  showTrafficType?: boolean;
  showVehicleType?: boolean;
  startLabel: string;
  title: string;
};

type ArrangementEditorForm = {
  allocationMode: SalesProductApi.AllocationMode;
  arrivalPlace?: string;
  cashAmount: number;
  companyRebateAmount: number;
  confirmed: boolean;
  confirmationNo?: string;
  consumptionAmount: number;
  costAmount: number;
  creditAmount: number;
  daysCount: number;
  departurePlace?: string;
  driverName?: string;
  fundIncluded?: string;
  guideCommissionAmount: number;
  guideName?: string;
  headFeeAmount: number;
  mealType?: string;
  noGuideReport: boolean;
  orderScope?: string;
  peopleCount: number;
  prepaidAmount: number;
  priceRemark?: string;
  projectId?: number;
  projectName?: string;
  quantity: number;
  remark?: string;
  resourceName?: string;
  responsibleEmployeeId?: number;
  responsibleEmployeeName?: string;
  saleAmount: number;
  scheduleEndDay?: string;
  scheduleStartDay?: string;
  settlementType: SalesProductApi.SettlementType;
  supplierId?: number;
  supplierName?: string;
  trafficType?: string;
  unitPrice: number;
  vehiclePlate?: string;
  vehicleType?: string;
};

const route = useRoute();
const router = useRouter();

const arrangementShortcuts: ArrangementCategoryShortcut[] = [
  { anchor: 'part1', icon: 'lucide:plane', label: '大交通', value: 'traffic' },
  { anchor: 'part2', icon: 'lucide:building-2', label: '住宿', value: 'hotel' },
  { anchor: 'part3', icon: 'lucide:car', label: '用车', value: 'vehicle' },
  { anchor: 'part4', icon: 'lucide:landmark', label: '景区', value: 'scenic' },
  { anchor: 'part5', icon: 'lucide:utensils', label: '用餐', value: 'meal' },
  { anchor: 'part6', icon: 'lucide:grid-2x2', label: '其它', value: 'other' },
  { anchor: 'part7', icon: 'lucide:ticket', label: '自费', value: 'optional' },
  { anchor: 'part8', icon: 'lucide:store', label: '购物', value: 'shopping' },
  { anchor: 'part9', icon: 'lucide:circle-parking', label: '地接', value: 'ground_agent' },
  { anchor: 'part10', icon: 'lucide:paperclip', label: '附加', value: 'extra_fee' },
];

const arrangementOverviewTabs: ArrangementShortcut[] = [
  { anchor: 'arrangement-menu', icon: 'lucide:layout-grid', label: '总览', value: 'overview' },
  ...arrangementShortcuts,
];

const arrangementSections: ArrangementSection[] = [
  {
    ...arrangementShortcuts[0]!,
    columns: ['日期', '类型', '出发地', '目的地', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
  },
  {
    ...arrangementShortcuts[1]!,
    columns: ['入住', '退房', '几晚', '酒店名称', '早餐', '基金', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    documentAction: '订房单',
  },
  {
    ...arrangementShortcuts[2]!,
    columns: ['开始', '结束', '车型', '天数', '司机', '车牌', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    documentAction: '订车单',
  },
  {
    ...arrangementShortcuts[3]!,
    columns: ['日期', '景区名称', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    documentAction: '预订单',
  },
  {
    ...arrangementShortcuts[4]!,
    columns: ['日期', '时间', '餐厅名称', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
  },
  {
    ...arrangementShortcuts[5]!,
    columns: ['日期', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
  },
  {
    ...arrangementShortcuts[6]!,
    columns: ['日期', '景区/项目名称', '供应商', '备注', '人数', '销售价', '成本价', '收入合计', '成本合计', '导游提成', '现结', '挂账', '操作'],
  },
  {
    ...arrangementShortcuts[7]!,
    columns: ['日期', '购物店', '供应商', '备注', '品类', '进店人数', '人头费', '消费总额', '导游提成', '操作'],
  },
  {
    ...arrangementShortcuts[8]!,
    columns: ['开始', '结束', '天数', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    documentAction: '确认单',
  },
  {
    ...arrangementShortcuts[9]!,
    columns: ['日期', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
  },
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
  { label: '自费', value: 'optional' },
  { label: '购物', value: 'shopping' },
] as Array<{ label: string; value: SalesProductApi.ArrangementType }>;

const arrangementStages: ArrangementStage[] = [
  { label: '收客', state: 'done' },
  { label: '排团', state: 'template' },
  { label: '发团', state: 'pending' },
  { label: '结算', state: 'pending' },
  { label: '完成', state: 'pending' },
];

const trafficTypeOptions: SelectOption[] = [
  { label: '飞机', value: '飞机' },
  { label: '高铁', value: '高铁' },
  { label: '火车', value: '火车' },
  { label: '邮轮', value: '邮轮' },
];

const vehicleTypeOptions: SelectOption[] = ['5座', '7座', '9座', '19座', '33座', '34座', '39座', '45座', '54座', '63座']
  .map((item) => ({ label: item, value: item }));

const mealTypeOptions: SelectOption[] = ['早餐', '中餐', '晚餐']
  .map((item) => ({ label: item, value: item }));

const breakfastOptions: SelectOption[] = ['桌早', '自助早', '打包早', '不含']
  .map((item) => ({ label: item, value: item }));

const fundOptions: SelectOption[] = ['不含', '含']
  .map((item) => ({ label: item, value: item }));

const scheduleDayOptions: SelectOption[] = [
  { label: '=出发日期=', value: '=出发日期=' },
  { label: '第1天', value: '第1天' },
  { label: '第2天', value: '第2天' },
  { label: '第3天', value: '第3天' },
];

const trafficOrderOptions: SelectOption[] = [
  { label: '=不关联订单=', value: '=不关联订单=' },
];

const arrangementEditorConfigs: Record<SalesProductApi.ArrangementType, ArrangementEditorConfig> = {
  extra_fee: {
    noGuideReport: false,
    resourceMode: 'none',
    scheduleGroupLabel: '日期',
    settlement: true,
    showOrderInfo: true,
    startLabel: '日期',
    title: '添加/修改附加费用',
  },
  ground_agent: {
    daysLabel: '天数',
    endLabel: '结束',
    noGuideReport: true,
    resourceMode: 'none',
    scheduleGroupLabel: '拼团日期',
    settlement: true,
    showDaysCount: true,
    showEndDate: true,
    showOrderInfo: true,
    startLabel: '开始',
    title: '添加/修改拼团信息',
  },
  hotel: {
    daysLabel: '共几晚',
    endLabel: '退房',
    noGuideReport: true,
    resourceLabel: '酒店名称',
    resourceMode: 'select',
    responsibleLabel: '责任房调',
    scheduleGroupLabel: '入住退房',
    settlement: true,
    showBreakfastFund: true,
    showConfirmed: true,
    showDaysCount: true,
    showEndDate: true,
    showOrderInfo: true,
    showResponsible: true,
    startLabel: '入住',
    title: '添加/修改酒店信息',
  },
  meal: {
    noGuideReport: true,
    resourceLabel: '餐厅名称',
    resourceMode: 'select',
    scheduleGroupLabel: '用餐日期',
    settlement: true,
    showMealTime: true,
    showOrderInfo: true,
    startLabel: '日期',
    title: '添加/修改餐厅信息',
  },
  optional: {
    noGuideReport: true,
    peopleLabel: '人数',
    resourceLabel: '景区/项目名称',
    resourceMode: 'select',
    scheduleGroupLabel: '自费日期',
    settlement: true,
    showOptionalAmounts: true,
    showOrderInfo: true,
    showPeople: true,
    startLabel: '日期',
    title: '添加/修改自费信息',
  },
  other: {
    noGuideReport: true,
    resourceMode: 'none',
    scheduleGroupLabel: '日期',
    settlement: true,
    showOrderInfo: true,
    startLabel: '日期',
    title: '添加/修改其它信息',
  },
  scenic: {
    noGuideReport: true,
    resourceLabel: '景区名称',
    resourceMode: 'select',
    scheduleGroupLabel: '游览日期',
    settlement: true,
    showOrderInfo: true,
    startLabel: '日期',
    title: '添加/修改景区信息',
  },
  shopping: {
    noGuideReport: false,
    peopleLabel: '进店人数',
    resourceLabel: '购物店',
    resourceMode: 'select',
    scheduleGroupLabel: '购物日期',
    settlement: false,
    showOrderInfo: false,
    showPeople: true,
    showShoppingAmounts: true,
    startLabel: '日期',
    title: '添加/修改购物信息',
  },
  traffic: {
    noGuideReport: true,
    resourceMode: 'none',
    scheduleGroupLabel: '日期行程',
    settlement: true,
    showArrivalPlace: true,
    showOrderInfo: true,
    showTrafficType: true,
    startLabel: '日期',
    title: '添加/修改大交通信息',
  },
  vehicle: {
    daysLabel: '天数',
    endLabel: '结束',
    noGuideReport: true,
    resourceMode: 'none',
    responsibleLabel: '责任车调',
    scheduleGroupLabel: '开始结束',
    settlement: true,
    showDaysCount: true,
    showDriver: true,
    showEndDate: true,
    showOrderInfo: true,
    showResponsible: true,
    showVehicleType: true,
    startLabel: '开始',
    title: '添加/修改用车信息',
  },
};

const supplierCategoryMap: Record<SalesProductApi.ArrangementType, SupplierApi.Category | undefined> = {
  extra_fee: 'other',
  ground_agent: 'ground_agent',
  hotel: 'hotel',
  meal: 'restaurant',
  optional: 'scenic',
  other: 'other',
  scenic: 'scenic',
  shopping: 'shopping',
  traffic: 'traffic',
  vehicle: 'vehicle',
};

const expenseResourceTypeMap: Record<SalesProductApi.ArrangementType, string> = {
  extra_fee: 'extra_fee',
  ground_agent: 'ground_agent',
  hotel: 'hotel',
  meal: 'restaurant',
  optional: 'scenic',
  other: 'other',
  scenic: 'scenic',
  shopping: 'shopping',
  traffic: 'traffic',
  vehicle: 'vehicle',
};

const loading = ref(false);
const saving = ref(false);
const arrangementModalOpen = ref(false);
const optionsLoading = ref(false);
const activeEditorType = ref<SalesProductApi.ArrangementType>('traffic');
const formState = reactive<ProductFormState>(createDefaultProductForm());
const arrangementForm = reactive<ArrangementEditorForm>(createDefaultArrangementEditorForm('traffic'));
const supplierOptions = ref<SelectOptionWithId[]>([]);
const projectOptions = ref<SelectOptionWithId[]>([]);
const resourceOptions = ref<SelectOptionWithId[]>([]);
const employeeOptions = ref<SelectOptionWithId[]>([]);

const productId = computed(() => {
  const value = route.params.id;
  const id = Array.isArray(value) ? value[0] : value;
  return id ? Number(id) : undefined;
});
const pageTitle = computed(() => (
  formState.productName ? `团队安排 - ${formState.productName}` : '产品团队安排'
));
const arrangementTotal = computed(() => calculateArrangementTotal(formState.arrangementItems));
const overviewSummary = computed(() => createArrangementOverviewSummary(formState.arrangementItems));
const arrangementCashTotal = computed(() => overviewSummary.value.total.cash);
const arrangementCreditTotal = computed(() => overviewSummary.value.total.credit);
const editorTotalAmount = computed(() => (
  Number(arrangementForm.unitPrice || 0) * Number(arrangementForm.quantity || 0)
));
const editorCreditAmount = computed(() => Math.max(
  editorTotalAmount.value - Number(arrangementForm.cashAmount || 0) - Number(arrangementForm.prepaidAmount || 0),
  0,
));
const activeSection = computed(() => arrangementSections.find((item) => item.value === activeEditorType.value));
const activeEditorConfig = computed(() => arrangementEditorConfigs[activeEditorType.value]);
const activeEditorTitle = computed(() => activeEditorConfig.value.title);
const teamBadges = computed<TeamBadgeItem[]>(() => [
  { color: 'orange', label: '业务类型', value: formState.businessType || '未设置' },
  { color: 'blue', label: '部门', value: '总部' },
  { color: 'blue', label: '操作计调', value: '团队阶段指定' },
  { color: 'orange', label: '导游', value: '--' },
  { color: 'green', label: '领队', value: '--' },
  { color: 'default', label: '全陪', value: '--' },
]);
const teamMetricItems = computed<TeamMetricItem[]>(() => [
  {
    label: '旅游天数',
    value: `${formState.travelDays || 1} 天`,
  },
  {
    label: '接待标准',
    value: formState.receptionStandard || '未设置',
  },
  {
    label: '总里程数',
    value: '0公里',
  },
  {
    label: '应收/已收/余额',
    value: '0 | 0 | 0',
  },
  {
    label: '订单已确认/未处理/已取消',
    value: '0 | 0 | 0',
  },
  {
    label: '已付',
    value: '--',
  },
  {
    label: '预算利润',
    value: `${formatPlainMoney(arrangementTotal.value)}`,
  },
]);

function formatMoney(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    currency: 'CNY',
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
    style: 'currency',
  }).format(Number(value || 0));
}

function formatPlainMoney(value?: number) {
  return `${Number(value || 0).toFixed(0)} 元`;
}

function arrangementLineTotal(item: SalesProductApi.ArrangementItem) {
  return arrangementItemTotal(item);
}

function arrangementSettlementTotal(
  type: SalesProductApi.ArrangementType,
  settlementType: 'cash' | 'credit',
) {
  return overviewSummary.value.byType[type]?.[settlementType] || 0;
}

function sectionItems(type: SalesProductApi.ArrangementType) {
  return (formState.arrangementItems || []).filter((item) => item.arrangementType === type);
}

function createDefaultArrangementEditorForm(type: SalesProductApi.ArrangementType): ArrangementEditorForm {
  return {
    allocationMode: 'group_order_average',
    arrivalPlace: '',
    cashAmount: 0,
    companyRebateAmount: 0,
    confirmed: type === 'hotel',
    consumptionAmount: 0,
    costAmount: 0,
    creditAmount: 0,
    daysCount: type === 'hotel' || type === 'vehicle' || type === 'ground_agent' ? 1 : 0,
    departurePlace: '',
    fundIncluded: '不含',
    guideCommissionAmount: 0,
    headFeeAmount: 0,
    mealType: type === 'meal' ? '中餐' : undefined,
    noGuideReport: false,
    orderScope: '=不关联订单=',
    peopleCount: 0,
    prepaidAmount: 0,
    projectName: defaultProjectName(type),
    quantity: 0,
    saleAmount: 0,
    scheduleEndDay: type === 'hotel' || type === 'vehicle' || type === 'ground_agent' ? '第2天' : undefined,
    scheduleStartDay: type === 'traffic' ? '=出发日期=' : '第1天',
    settlementType: 'credit',
    trafficType: type === 'traffic' ? '飞机' : undefined,
    unitPrice: 0,
    vehicleType: type === 'vehicle' ? '39座' : undefined,
  };
}

function resetArrangementForm(type: SalesProductApi.ArrangementType) {
  Object.assign(arrangementForm, createDefaultArrangementEditorForm(type));
}

function defaultProjectName(type: SalesProductApi.ArrangementType) {
  const defaults: Record<SalesProductApi.ArrangementType, string> = {
    extra_fee: '保险',
    ground_agent: '成人',
    hotel: '标间',
    meal: '标准餐',
    optional: '成人',
    other: '礼品',
    scenic: '成人',
    shopping: '乳胶',
    traffic: '飞机',
    vehicle: '车费',
  };
  return defaults[type];
}

function allocationLabel(value?: SalesProductApi.AllocationMode) {
  return value === 'multi_order_average' ? '多订单均摊成本' : '全团/订单均摊';
}

function priceInfoText(item: SalesProductApi.ArrangementItem) {
  if (item.priceLines?.length) {
    return item.priceLines
      .map((line) => `${line.projectName || '费用'} ${formatMoney(line.unitPrice)} × ${line.quantity || 0}`)
      .join('；');
  }
  if (item.projectName) {
    return `${item.projectName} ${formatMoney(item.unitPrice)} × ${item.quantity || 0}`;
  }
  return item.itemName || item.arrangementContent || '';
}

function arrangementCellText(item: SalesProductApi.ArrangementItem, column: string) {
  if (column === '日期' || column === '入住' || column === '开始') return item.scheduleStartDay || '';
  if (column === '退房' || column === '结束') return item.scheduleEndDay || '';
  if (column === '几晚' || column === '天数') return item.daysCount ? String(item.daysCount) : '';
  if (column === '类型') return item.trafficType || item.itemName || '';
  if (column === '出发地') return item.departurePlace || '';
  if (column === '目的地') return item.arrivalPlace || '';
  if (column === '酒店名称' || column === '景区名称' || column === '餐厅名称' || column === '购物店' || column === '景区/项目名称') return item.resourceName || item.itemName || '';
  if (column === '早餐' || column === '时间') return item.mealType || '';
  if (column === '基金') return item.fundIncluded || '';
  if (column === '车型') return item.vehicleType || '';
  if (column === '司机') return item.driverName || '';
  if (column === '车牌') return item.vehiclePlate || '';
  if (column === '供应商') return item.supplierName || '';
  if (column === '备注') return item.remark || '';
  if (column === '价格信息') return priceInfoText(item);
  if (column === '人数' || column === '进店人数') return item.peopleCount ? String(item.peopleCount) : '';
  if (column === '销售价') return formatMoney(item.priceLines?.[0]?.salePrice || item.unitPrice || 0);
  if (column === '成本价') return formatMoney(item.priceLines?.[0]?.costPrice || item.costAmount || 0);
  if (column === '收入合计') return formatMoney(item.saleAmount || 0);
  if (column === '品类') return item.projectName || item.priceLines?.[0]?.projectName || '';
  if (column === '人头费') return formatMoney(item.headFeeAmount || 0);
  if (column === '消费总额') return formatMoney(item.consumptionAmount || 0);
  if (column === '导游提成') return formatMoney(item.guideCommissionAmount || 0);
  if (column === '成本合计') return formatMoney(arrangementLineTotal(item));
  if (column === '现结' || column === '现付') return formatMoney(arrangementItemCash(item));
  if (column === '挂账') return formatMoney(arrangementItemCredit(item));
  if (column === '操作') return '--';
  return item.arrangementContent || '';
}

function showStaticFeatureTip() {
  message.info('正式团队安排模块接入后可用');
}

async function openArrangementEditor(type: SalesProductApi.ArrangementType) {
  activeEditorType.value = type;
  resetArrangementForm(type);
  arrangementModalOpen.value = true;
  await loadEditorOptions(type);
}

function scrollToArrangementAnchor(anchor: string) {
  document.getElementById(anchor)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function loadEditorOptions(type: SalesProductApi.ArrangementType) {
  optionsLoading.value = true;
  try {
    const supplierCategory = supplierCategoryMap[type];
    const [suppliers, projects, employees, resources] = await Promise.all([
      getSupplierAll(supplierCategory),
      getExpenseItemAll(expenseResourceTypeMap[type] as never),
      getEnterpriseEmployeeAll(false),
      loadResourceOptions(type),
    ]);
    supplierOptions.value = suppliers.map((item) => ({
      id: item.id,
      label: item.supplierName,
      value: item.supplierName,
    }));
    projectOptions.value = projects.map((item) => ({
      id: item.id,
      label: item.projectName,
      value: item.projectName,
    }));
    employeeOptions.value = employees.map((item) => ({
      id: item.id,
      label: item.employeeName,
      value: item.employeeName,
    }));
    resourceOptions.value = resources;
  } finally {
    optionsLoading.value = false;
  }
}

async function loadResourceOptions(type: SalesProductApi.ArrangementType): Promise<SelectOptionWithId[]> {
  if (type === 'hotel') {
    const result = await getHotelResourcePage({ page: 1, pageSize: 200, status: 'active' });
    return result.items.map((item) => ({
      id: item.id,
      label: `${item.hotelName} / ${item.roomType}`,
      value: item.hotelName,
    }));
  }
  if (type === 'scenic' || type === 'optional') {
    const result = await getScenicResourcePage({ page: 1, pageSize: 200, status: 'active' });
    return result.items.map((item) => ({
      id: item.id,
      label: `${item.scenicName} / ${item.ticketType}`,
      value: item.scenicName,
    }));
  }
  if (type === 'meal' || type === 'shopping') {
    const result = await getPurchaseResourcePage({
      page: 1,
      pageSize: 200,
      resourceType: type === 'meal' ? 'restaurant' : 'shopping',
      status: 'active',
    });
    return result.items.map((item) => ({
      id: item.id,
      label: item.resourceName,
      value: item.resourceName,
    }));
  }
  if (type === 'ground_agent') {
    const result = await getGroundAgentPage({ page: 1, pageSize: 200, status: 'active' });
    return result.items.map((item) => ({
      id: item.id,
      label: item.groundAgentName,
      value: item.groundAgentName,
    }));
  }
  return [];
}

function normalizeSelectValue(value: unknown) {
  return typeof value === 'string' ? value : undefined;
}

function applySelectedSupplier(value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  const supplier = supplierOptions.value.find((item) => item.value === selectedValue);
  arrangementForm.supplierId = supplier?.id;
  arrangementForm.supplierName = selectedValue;
}

function applySelectedProject(value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  const project = projectOptions.value.find((item) => item.value === selectedValue);
  arrangementForm.projectId = project?.id;
  arrangementForm.projectName = selectedValue;
}

function applySelectedResponsible(value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  const employee = employeeOptions.value.find((item) => item.value === selectedValue);
  arrangementForm.responsibleEmployeeId = employee?.id;
  arrangementForm.responsibleEmployeeName = selectedValue;
}

/** 保存当前分类安排到产品模板明细。 */
function saveArrangementEditor() {
  const type = activeEditorType.value;
  if (type === 'traffic' && !arrangementForm.trafficType) {
    message.warning('请选择交通类型');
    return;
  }
  if (type === 'traffic' && (!arrangementForm.scheduleStartDay || !arrangementForm.departurePlace || !arrangementForm.arrivalPlace)) {
    message.warning('请补全日期行程');
    return;
  }
  if (!arrangementForm.supplierName) {
    message.warning('请选择供应商');
    return;
  }

  const totalAmount = editorTotalAmount.value;
  const creditAmount = editorCreditAmount.value;
  const prepaidAmount = Number(arrangementForm.prepaidAmount || 0);
  const projectName = arrangementForm.projectName || defaultProjectName(type);
  const itemName = buildArrangementItemName(type, projectName);
  const arrangementItem: SalesProductApi.ArrangementItem = {
    allocationMode: arrangementForm.allocationMode,
    arrivalPlace: arrangementForm.arrivalPlace,
    arrangementContent: buildArrangementContent(type),
    arrangementType: type,
    cashAmount: Number(arrangementForm.cashAmount || 0),
    companyRebateAmount: Number(arrangementForm.companyRebateAmount || 0),
    confirmed: arrangementForm.confirmed,
    confirmationNo: arrangementForm.confirmationNo,
    consumptionAmount: Number(arrangementForm.consumptionAmount || 0),
    costAmount: Number(arrangementForm.costAmount || totalAmount),
    creditAmount,
    daysCount: Number(arrangementForm.daysCount || 0),
    departurePlace: arrangementForm.departurePlace,
    driverName: arrangementForm.driverName,
    fundIncluded: arrangementForm.fundIncluded,
    guideCommissionAmount: Number(arrangementForm.guideCommissionAmount || 0),
    headFeeAmount: Number(arrangementForm.headFeeAmount || 0),
    itemName,
    mealType: arrangementForm.mealType,
    noGuideReport: arrangementForm.noGuideReport,
    orderScope: arrangementForm.orderScope || '=不关联订单=',
    peopleCount: Number(arrangementForm.peopleCount || 0),
    prepaidAmount,
    priceLines: [{
      amount: totalAmount,
      cashAmount: Number(arrangementForm.cashAmount || 0),
      companyRebateAmount: Number(arrangementForm.companyRebateAmount || 0),
      consumptionAmount: Number(arrangementForm.consumptionAmount || 0),
      costPrice: Number(arrangementForm.costAmount || 0),
      creditAmount,
      guideCommissionAmount: Number(arrangementForm.guideCommissionAmount || 0),
      guideCommissionRate: 0,
      headFeeAmount: Number(arrangementForm.headFeeAmount || 0),
      projectId: arrangementForm.projectId,
      projectName,
      quantity: Number(arrangementForm.quantity || 0),
      remark: arrangementForm.priceRemark,
      salePrice: Number(arrangementForm.saleAmount || 0),
      sortOrder: 1,
      unitPrice: Number(arrangementForm.unitPrice || 0),
    }],
    quantity: Number(arrangementForm.quantity || 0),
    remark: [
      `费用归属：${allocationLabel(arrangementForm.allocationMode)}`,
      `订单信息：${arrangementForm.orderScope || '=不关联订单='}`,
      arrangementForm.priceRemark ? `价格备注：${arrangementForm.priceRemark}` : '',
      arrangementForm.remark ? `备注：${arrangementForm.remark}` : '',
      arrangementForm.noGuideReport ? '无需导游报账，同步更新导游报账和计调审核数据' : '',
    ].filter(Boolean).join('；'),
    resourceName: arrangementForm.resourceName,
    responsibleEmployeeId: arrangementForm.responsibleEmployeeId,
    responsibleEmployeeName: arrangementForm.responsibleEmployeeName,
    saleAmount: Number(arrangementForm.saleAmount || 0),
    scheduleEndDay: arrangementForm.scheduleEndDay,
    scheduleStartDay: arrangementForm.scheduleStartDay,
    settlementType: arrangementForm.settlementType,
    supplierId: arrangementForm.supplierId,
    supplierName: arrangementForm.supplierName,
    totalAmount,
    trafficType: arrangementForm.trafficType,
    unitName: unitNameForType(type),
    unitPrice: Number(arrangementForm.unitPrice || 0),
    vehiclePlate: arrangementForm.vehiclePlate,
    vehicleType: arrangementForm.vehicleType,
  };

  formState.arrangementItems = [
    ...(formState.arrangementItems || []).filter((item) => item.itemName?.trim()),
    arrangementItem,
  ];
  arrangementModalOpen.value = false;
  message.success(`${activeSection.value?.label || '安排'}信息已加入当前明细，请点击“保存团队安排”写入产品`);
}

function unitNameForType(type: SalesProductApi.ArrangementType) {
  const units: Record<SalesProductApi.ArrangementType, string> = {
    extra_fee: '项',
    ground_agent: '团',
    hotel: '间夜',
    meal: '餐',
    optional: '人',
    other: '项',
    scenic: '人',
    shopping: '次',
    traffic: '张',
    vehicle: '辆',
  };
  return units[type];
}

function buildArrangementItemName(type: SalesProductApi.ArrangementType, projectName: string) {
  if (type === 'traffic') return `${arrangementForm.trafficType || '大交通'}-${projectName}`;
  if (type === 'vehicle') return `${arrangementForm.vehicleType || '用车'}-${projectName}`;
  return `${arrangementForm.resourceName || activeSection.value?.label || '安排'}-${projectName}`;
}

function buildArrangementContent(type: SalesProductApi.ArrangementType) {
  if (type === 'traffic') {
    return [arrangementForm.scheduleStartDay, arrangementForm.trafficType, arrangementForm.departurePlace, arrangementForm.arrivalPlace, arrangementForm.supplierName]
      .filter(Boolean)
      .join('｜');
  }
  return [
    arrangementForm.scheduleStartDay,
    arrangementForm.scheduleEndDay,
    arrangementForm.resourceName,
    arrangementForm.supplierName,
  ].filter(Boolean).join('｜');
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
  <Page :title="pageTitle">
    <Spin :spinning="loading">
      <Card class="team-arrangement-card">
        <div class="arrangement-command-bar">
          <div class="command-main">
            <div class="form-kicker">团队安排总览 · Group Arrange</div>
            <div class="team-title-line">
              <div class="team-name">{{ formState.productName || '未命名产品' }}</div>
              <div class="team-badges">
                <Tag
                  v-for="item in teamBadges"
                  :key="item.label"
                  :color="item.color"
                >
                  {{ item.label }}：{{ item.value }}
                </Tag>
              </div>
            </div>
            <div class="team-metric-strip">
              <span
                v-for="item in teamMetricItems"
                :key="item.label"
                class="team-metric-item"
              >
                <em>{{ item.label }}</em>
                <strong>{{ item.value }}</strong>
              </span>
            </div>
          </div>

          <div class="command-side">
            <div class="workflow-rail" aria-label="团队阶段">
              <div
                v-for="(stage, index) in arrangementStages"
                :key="stage.label"
                class="stage-flow-item"
                :class="stage.state"
              >
                <span class="stage-index">{{ index + 1 }}</span>
                <span class="stage-label">{{ stage.label }}</span>
              </div>
            </div>
            <div class="team-profile-actions">
              <button type="button" class="compact-action" @click="showStaticFeatureTip">
                <IconifyIcon icon="lucide:clipboard-list" />
                <span>团队管理</span>
              </button>
              <button type="button" class="compact-action" @click="showStaticFeatureTip">
                <IconifyIcon icon="lucide:briefcase" />
                <span>查看行程</span>
              </button>
            </div>
          </div>
        </div>

        <div class="team-note-row">
          <div class="internal-note-title">
            <div>
              <IconifyIcon icon="lucide:info" />
              <span>内部备注</span>
            </div>
            <Button type="link" size="small" @click="showStaticFeatureTip">
              <IconifyIcon icon="lucide:file-pen-line" />
              <span>编辑</span>
            </Button>
          </div>
          <div class="internal-note-metrics">
            <span>人均坑位：¥0</span>
            <span>自费加点率：0%</span>
            <span>人均购物：¥0</span>
          </div>
        </div>

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
                <th rowspan="2">导服</th>
                <th rowspan="2">操作费</th>
                <th rowspan="2">备用金</th>
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
                <td>{{ formatMoney(overviewSummary.extraColumns.selfPayIncome) }}</td>
                <td>{{ formatMoney(overviewSummary.extraColumns.guideService) }}</td>
                <td>{{ formatMoney(overviewSummary.extraColumns.operationFee) }}</td>
                <td>{{ formatMoney(overviewSummary.extraColumns.reserveFund) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div id="arrangement-menu" class="arrangement-overview-tabs" aria-label="团队安排总览分类">
          <button
            v-for="item in arrangementOverviewTabs"
            :key="item.value"
            type="button"
            class="arrangement-overview-tab"
            :class="{ active: item.value === 'overview' }"
            @click="scrollToArrangementAnchor(item.anchor)"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="arrangement-icon-grid compact-category-strip" aria-label="团队安排分类快捷入口">
          <button
            v-for="item in arrangementShortcuts"
            :key="item.value"
            type="button"
            class="arrangement-icon-button"
            @click="scrollToArrangementAnchor(item.anchor)"
          >
            <IconifyIcon :icon="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </div>

        <div class="arrangement-overview-sections">
          <section
            v-for="section in arrangementSections"
            :id="section.anchor"
            :key="section.value"
            class="arrangement-section-card"
          >
            <div class="arrangement-section-header">
              <div class="arrangement-section-title">
                <IconifyIcon :icon="section.icon" />
                <span>{{ section.label }}</span>
                <Button
                  v-if="section.documentAction"
                  class="arrangement-document-button"
                  size="small"
                  type="primary"
                  @click="showStaticFeatureTip"
                >
                  <IconifyIcon icon="lucide:printer" />
                  <span>{{ section.documentAction }}</span>
                </Button>
              </div>
              <div class="arrangement-section-actions">
                <Button size="small" @click="showStaticFeatureTip">无需</Button>
                <Button size="small" @click="showStaticFeatureTip">完成</Button>
                <button
                  type="button"
                  class="arrangement-add-button"
                  aria-label="新增安排"
                  @click="openArrangementEditor(section.value)"
                >
                  <IconifyIcon icon="lucide:plus" />
                </button>
              </div>
            </div>

            <div class="arrangement-section-table-wrap">
              <table class="arrangement-section-table">
                <thead>
                  <tr>
                    <th
                      v-for="column in section.columns"
                      :key="`${section.value}-${column}`"
                    >
                      {{ column }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!sectionItems(section.value).length">
                    <td :colspan="section.columns.length" class="arrangement-empty-cell">暂无安排</td>
                  </tr>
                  <tr
                    v-for="item in sectionItems(section.value)"
                    :key="`${section.value}-${item.id || item.itemName || item.arrangementContent}`"
                  >
                    <td
                      v-for="column in section.columns"
                      :key="`${section.value}-${item.id || item.itemName}-${column}`"
                    >
                      {{ arrangementCellText(item, column) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <div class="arrangement-footer-actions">
          <Button @click="goBack">返回列表</Button>
          <Button @click="goProductEdit">修改产品</Button>
          <Button type="primary" :loading="saving" @click="saveArrangement">保存团队安排</Button>
        </div>
      </Card>

      <Modal
        v-model:open="arrangementModalOpen"
        centered
        class="traffic-arrangement-modal"
        :footer="null"
        :title="activeEditorTitle"
        width="920px"
      >
        <Spin :spinning="optionsLoading">
          <div class="traffic-modal-body">
            <Radio.Group
              v-model:value="arrangementForm.allocationMode"
              class="traffic-cost-mode-tabs"
              option-type="button"
              button-style="solid"
            >
              <Radio.Button value="group_order_average">全团/订单均摊</Radio.Button>
              <Radio.Button value="multi_order_average">多订单均摊成本</Radio.Button>
            </Radio.Group>

            <Form class="traffic-form" layout="vertical">
              <div v-if="activeEditorConfig.showTrafficType" class="traffic-form-row one-column">
                <Form.Item label="交通类型" required>
                  <Select v-model:value="arrangementForm.trafficType" :options="trafficTypeOptions" />
                </Form.Item>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:route" />
                  <span>{{ activeEditorConfig.scheduleGroupLabel }}</span>
                </div>
                <div class="traffic-form-row three-columns">
                  <Form.Item :label="activeEditorConfig.startLabel" required>
                    <Select v-model:value="arrangementForm.scheduleStartDay" :options="scheduleDayOptions" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showEndDate" :label="activeEditorConfig.endLabel">
                    <Select v-model:value="arrangementForm.scheduleEndDay" :options="scheduleDayOptions" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showArrivalPlace" label="出发地" required>
                    <Input v-model:value="arrangementForm.departurePlace" placeholder="请输入出发地" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showArrivalPlace" label="抵达地" required>
                    <Input v-model:value="arrangementForm.arrivalPlace" placeholder="请输入目的地" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showDaysCount" :label="activeEditorConfig.daysLabel">
                    <InputNumber v-model:value="arrangementForm.daysCount" :min="0" :precision="0" />
                  </Form.Item>
                </div>
              </div>

              <div v-if="activeEditorConfig.resourceMode === 'select' || activeEditorConfig.showVehicleType || activeEditorConfig.showDriver || activeEditorConfig.showMealTime || activeEditorConfig.showBreakfastFund || activeEditorConfig.showResponsible || activeEditorConfig.showConfirmed" class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:database" />
                  <span>{{ activeEditorConfig.resourceLabel || activeSection?.label || '安排信息' }}</span>
                </div>
                <div class="traffic-form-row three-columns">
                  <Form.Item v-if="activeEditorConfig.resourceMode === 'select'" :label="activeEditorConfig.resourceLabel">
                    <Select
                      v-model:value="arrangementForm.resourceName"
                      allow-clear
                      show-search
                      :options="resourceOptions"
                    />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showConfirmed" label="已确认">
                    <Checkbox v-model:checked="arrangementForm.confirmed">已确认</Checkbox>
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showConfirmed" label="确认号">
                    <Input v-model:value="arrangementForm.confirmationNo" placeholder="确认号" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showVehicleType" label="车型">
                    <Select v-model:value="arrangementForm.vehicleType" :options="vehicleTypeOptions" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showDriver" label="司机">
                    <Input v-model:value="arrangementForm.driverName" placeholder="司机姓名/电话" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showDriver" label="车牌">
                    <Input v-model:value="arrangementForm.vehiclePlate" placeholder="车牌号" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showMealTime" label="时间">
                    <Select v-model:value="arrangementForm.mealType" :options="mealTypeOptions" />
                  </Form.Item>
                  <template v-if="activeEditorConfig.showBreakfastFund">
                    <div class="old-system-combined-field">
                      <div class="old-system-combined-title">早餐基金</div>
                      <div class="old-system-combined-controls">
                        <span>早餐：</span>
                        <Select v-model:value="arrangementForm.mealType" :options="breakfastOptions" />
                        <span>基金：</span>
                        <Select v-model:value="arrangementForm.fundIncluded" :options="fundOptions" />
                      </div>
                    </div>
                  </template>
                  <Form.Item v-if="activeEditorConfig.showResponsible" :label="activeEditorConfig.responsibleLabel">
                    <Select
                      v-model:value="arrangementForm.responsibleEmployeeName"
                      allow-clear
                      show-search
                      :options="employeeOptions"
                      @change="applySelectedResponsible"
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
                      v-model:value="arrangementForm.supplierName"
                      allow-clear
                      show-search
                      :options="supplierOptions"
                      @change="applySelectedSupplier"
                    />
                  </Form.Item>
                  <Form.Item label="添加">
                    <Button @click="showStaticFeatureTip">添加供应商</Button>
                  </Form.Item>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>价格信息</span>
                </div>
                <div class="traffic-price-line">
                  <Select
                    v-model:value="arrangementForm.projectName"
                    show-search
                    :options="projectOptions"
                    @change="applySelectedProject"
                  />
                  <InputNumber
                    v-model:value="arrangementForm.unitPrice"
                    addon-before="¥"
                    :min="0"
                    :precision="2"
                  />
                  <div class="traffic-inline-number">
                    <span>*数量:</span>
                    <InputNumber v-model:value="arrangementForm.quantity" :min="0" :precision="0" />
                  </div>
                  <div class="traffic-price-remark">
                    <span>备注:</span>
                    <Textarea
                      v-model:value="arrangementForm.priceRemark"
                      :auto-size="{ minRows: 1, maxRows: 2 }"
                      placeholder="价格备注"
                    />
                  </div>
                </div>
                <div v-if="activeEditorConfig.showPeople || activeEditorConfig.showOptionalAmounts || activeEditorConfig.showShoppingAmounts" class="traffic-form-row three-columns extra-amount-row">
                  <Form.Item v-if="activeEditorConfig.showPeople" :label="activeEditorConfig.peopleLabel">
                    <InputNumber v-model:value="arrangementForm.peopleCount" :min="0" :precision="0" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showOptionalAmounts" label="销售价">
                    <InputNumber v-model:value="arrangementForm.saleAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showOptionalAmounts" label="成本价">
                    <InputNumber v-model:value="arrangementForm.costAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showShoppingAmounts" label="人头费">
                    <InputNumber v-model:value="arrangementForm.headFeeAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showShoppingAmounts" label="消费总额">
                    <InputNumber v-model:value="arrangementForm.consumptionAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showOptionalAmounts || activeEditorConfig.showShoppingAmounts" label="导游提成">
                    <InputNumber v-model:value="arrangementForm.guideCommissionAmount" addon-before="¥" :min="0" :precision="2" />
                  </Form.Item>
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
                    <InputNumber
                      v-model:value="arrangementForm.cashAmount"
                      addon-before="¥"
                      :min="0"
                      :max="editorTotalAmount"
                      :precision="2"
                    />
                  </Form.Item>
                  <Form.Item label="挂账">
                    <InputNumber :value="editorCreditAmount" disabled addon-before="¥" :precision="2" />
                  </Form.Item>
                  <Form.Item label="预付款">
                    <InputNumber
                      v-model:value="arrangementForm.prepaidAmount"
                      addon-before="¥"
                      :min="0"
                      :max="editorTotalAmount"
                      :precision="2"
                    />
                  </Form.Item>
                </div>
              </div>

              <Form.Item v-if="activeEditorConfig.showOrderInfo" label="订单信息">
                <Select v-model:value="arrangementForm.orderScope" :options="trafficOrderOptions" />
                <div class="traffic-field-tip">产品模板阶段默认不关联正式订单</div>
              </Form.Item>

              <Form.Item label="备注信息">
                <Textarea
                  v-model:value="arrangementForm.remark"
                  :auto-size="{ minRows: 2, maxRows: 4 }"
                  placeholder="备注信息"
                />
              </Form.Item>
            </Form>

            <div class="traffic-modal-footer">
              <Button @click="arrangementModalOpen = false">取消</Button>
              <Checkbox v-if="activeEditorConfig.noGuideReport" v-model:checked="arrangementForm.noGuideReport" class="traffic-sync-checkbox">
                无需导游报账，同步更新导游报账和计调审核数据
              </Checkbox>
              <Button type="primary" @click="saveArrangementEditor">提交保存</Button>
            </div>
          </div>
        </Spin>
      </Modal>
    </Spin>
  </Page>
</template>

<style scoped>
.team-arrangement-card {
  margin-bottom: 56px;
  overflow: hidden;
  color: #0f172a;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 22px rgb(15 23 42 / 5%);
}

.team-arrangement-card :deep(.ant-card-body) {
  padding: 16px;
  background: #fff;
}

.team-arrangement-card :deep(.ant-btn-default) {
  color: #334155;
  background: #fff;
  border-color: #dbe4f0;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.team-arrangement-card :deep(.ant-btn-default:hover) {
  color: #1677ff;
  border-color: #91caff;
}

.form-kicker {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 700;
  color: #1677ff;
  letter-spacing: 0;
}

.arrangement-command-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 520px;
  gap: 14px;
  align-items: stretch;
  padding: 14px 16px;
  margin-bottom: 10px;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.command-main,
.command-side {
  min-width: 0;
}

.command-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
  justify-content: space-between;
}

.team-title-line {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.team-name {
  flex: 0 0 auto;
  max-width: 360px;
  overflow: hidden;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.team-badges :deep(.ant-tag) {
  margin-inline-end: 0;
  font-weight: 600;
  border-radius: 4px;
}

.team-metric-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

.team-metric-item {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  min-height: 30px;
  padding: 4px 9px;
  border-right: 1px solid #e2e8f0;
}

.team-metric-item:last-child {
  border-right: 0;
}

.team-metric-item em,
.team-metric-item strong {
  overflow: hidden;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-metric-item em {
  font-size: 11.5px;
  font-weight: 700;
  color: #64748b;
}

.team-metric-item strong {
  font-size: 12.5px;
  font-weight: 800;
  color: #0f172a;
}

.team-profile-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.compact-action {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  min-width: 92px;
  height: 34px;
  padding: 0 10px;
  color: #1554ad;
  cursor: pointer;
  background: #eef6ff;
  border: 1px solid #b7d7ff;
  border-radius: 5px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.compact-action:hover,
.compact-action:focus {
  color: #0958d9;
  background: #e6f4ff;
  border-color: #69b1ff;
}

.compact-action svg {
  width: 16px;
  height: 16px;
}

.compact-action span {
  font-size: 12.5px;
  font-weight: 800;
}

.team-note-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  padding: 6px 12px;
  margin-bottom: 10px;
  background: #fbfdff;
  border: 1px solid #e2e8f0;
  border-left: 3px solid #91caff;
  border-radius: 6px;
}

.internal-note-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 0;
}

.internal-note-title > div,
.internal-note-title :deep(.ant-btn) {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.internal-note-title > div {
  font-size: 12.5px;
  font-weight: 800;
  color: #334155;
}

.internal-note-title svg {
  width: 15px;
  height: 15px;
}

.internal-note-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.internal-note-metrics span {
  padding: 3px 8px;
  font-size: 11.5px;
  font-weight: 700;
  color: #475569;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
}

.workflow-rail {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  align-items: center;
}

.stage-flow-item {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 5px 8px;
  color: #64748b;
  background: #fff;
  border: 1px solid #dbe4f0;
  border-radius: 5px;
}

.stage-flow-item.done {
  color: #0958d9;
  background: #e6f4ff;
  border-color: #91caff;
}

.stage-flow-item.template {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
  box-shadow: 0 4px 10px rgb(22 119 255 / 18%);
}

.stage-flow-item.pending {
  color: #475569;
  background: linear-gradient(180deg, #fff 0%, #f1f5f9 100%);
  border-color: #d7dee8;
}

.stage-index {
  display: flex;
  flex: 0 0 18px;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 11px;
  font-weight: 800;
  color: currentcolor;
  background: rgb(255 255 255 / 78%);
  border: 1px solid currentcolor;
  border-radius: 50%;
}

.stage-flow-item.done .stage-index {
  color: #fff;
  background: rgb(255 255 255 / 18%);
  border-color: rgb(255 255 255 / 70%);
}

.stage-flow-item.template .stage-index {
  color: #fff;
  background: rgb(255 255 255 / 18%);
  border-color: rgb(255 255 255 / 70%);
}

.stage-label {
  font-size: 12.5px;
  font-weight: 800;
  color: currentcolor;
}

.arrangement-overview-table {
  margin-bottom: 10px;
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.arrangement-overview-table table {
  width: 100%;
  min-width: 1320px;
  border-collapse: collapse;
  background: #fff;
}

.arrangement-overview-table th,
.arrangement-overview-table td {
  height: 34px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  color: #334155;
  text-align: center;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.arrangement-overview-table th {
  color: #52657a;
  background: #f6f8fb;
}

.arrangement-overview-table td {
  color: #0f172a;
}

.arrangement-overview-tabs {
  display: flex;
  gap: 2px;
  align-items: flex-end;
  padding-left: 1px;
  margin-bottom: 0;
  overflow-x: auto;
  background: #fff;
  border-bottom: 1px solid #91caff;
}

.arrangement-overview-tab {
  flex: 0 0 auto;
  min-width: 78px;
  height: 32px;
  padding: 0 14px;
  margin-bottom: -1px;
  font-size: 13px;
  font-weight: 800;
  color: #475569;
  cursor: pointer;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border: 1px solid #d9e2ec;
  border-bottom-color: #91caff;
  border-radius: 4px 4px 0 0;
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 80%);
}

.arrangement-overview-tab.active {
  color: #0958d9;
  background: #fff;
  border-color: #91caff;
  border-bottom-color: #fff;
  box-shadow:
    inset 0 3px 0 #1677ff,
    0 -1px 0 rgb(22 119 255 / 8%);
}

.arrangement-overview-tab:hover {
  color: #0958d9;
  background: #eef6ff;
  border-color: #91caff;
  border-bottom-color: #91caff;
}

.arrangement-overview-tab.active:hover {
  color: #0958d9;
  background: #fff;
  border-bottom-color: #fff;
}

.arrangement-icon-grid {
  display: grid;
  grid-template-columns: repeat(10, minmax(82px, 1fr));
  gap: 4px;
  padding: 12px 6px 14px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-top: 0;
  border-radius: 0 0 6px 6px;
}

.arrangement-icon-button {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  justify-content: center;
  min-height: 54px;
  padding: 6px;
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

.arrangement-icon-button:hover {
  color: #0958d9;
  background: #eef6ff;
  border-color: #91caff;
}

.arrangement-icon-button svg {
  width: 24px;
  height: 24px;
}

.arrangement-icon-button span {
  font-size: 12px;
  font-weight: 800;
}

.arrangement-overview-sections {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.arrangement-section-card {
  scroll-margin-top: 88px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.arrangement-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 38px;
  margin-bottom: 4px;
}

.arrangement-section-title {
  display: flex;
  gap: 7px;
  align-items: center;
  min-width: 0;
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
}

.arrangement-section-title svg {
  width: 21px;
  height: 21px;
  color: #1677ff;
}

.arrangement-document-button {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  margin-left: 4px;
  font-weight: 800;
  background: #1677ff;
  border-color: #1677ff;
}

.arrangement-document-button svg {
  width: 14px;
  height: 14px;
  color: #fff;
}

.arrangement-section-actions {
  display: flex;
  flex-shrink: 0;
  gap: 6px;
  align-items: center;
}

.arrangement-section-actions :deep(.ant-btn) {
  min-width: 48px;
  color: #64748b;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.arrangement-add-button {
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

.arrangement-add-button svg {
  width: 18px;
  height: 18px;
}

.arrangement-section-table-wrap {
  overflow-x: auto;
  border-top: 1px solid #eef2f7;
}

.arrangement-section-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  table-layout: fixed;
}

.arrangement-section-table th,
.arrangement-section-table td {
  height: 38px;
  padding: 7px 9px;
  font-size: 12.5px;
  color: #334155;
  text-align: center;
  border-bottom: 1px solid #eef2f7;
}

.arrangement-section-table th {
  font-weight: 800;
  color: #52657a;
  background: #f6f8fb;
}

.arrangement-section-table td {
  color: #0f172a;
  background: #fff;
}

.arrangement-empty-cell {
  height: 60px;
  font-weight: 700;
  color: #64748b !important;
}

.arrangement-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #e2e8f0;
}

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

.traffic-form-row.supplier-row {
  grid-template-columns: minmax(0, 1fr) 70px;
  align-items: end;
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

.traffic-inline-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0;
  font-weight: 800;
}

.traffic-price-line {
  display: grid;
  grid-template-columns: 150px 150px 140px minmax(180px, 1fr) 34px;
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

.traffic-add-line-button {
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

.traffic-add-line-button svg {
  width: 16px;
  height: 16px;
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
  .arrangement-command-bar {
    grid-template-columns: 1fr;
  }

  .team-title-line {
    align-items: flex-start;
    flex-direction: column;
  }

  .team-profile-actions {
    align-items: stretch;
    justify-content: flex-start;
  }

  .arrangement-icon-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .workflow-rail {
    grid-template-columns: repeat(5, minmax(72px, 1fr));
  }

  .traffic-form-row.two-columns,
  .traffic-form-row.three-columns,
  .traffic-form-row.supplier-row,
  .traffic-settlement-grid,
  .traffic-price-line,
  .old-system-combined-controls {
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

@media (max-width: 640px) {
  .team-arrangement-card :deep(.ant-card-body) {
    padding: 14px;
  }

  .workflow-rail {
    align-items: stretch;
    overflow-x: auto;
  }

  .stage-flow-item {
    flex: 0 0 92px;
  }

  .team-profile-actions {
    flex-direction: column;
  }

  .compact-action {
    min-height: 40px;
  }
}
</style>
