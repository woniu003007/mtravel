<script lang="ts" setup>
import type { SalesProductApi } from '#/api/sales/product';
import type { RegionPath } from '#/utils/region';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  AutoComplete,
  Button,
  Card,
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
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  type EnterpriseProductDictionaryApi as ProductDictionaryNamespace,
  getProductDictionaryAll,
} from '#/api/enterprise/product-dictionary';
import { getEnterpriseDepartmentAll } from '#/api/enterprise/department';
import { getEnterpriseEmployeeAll } from '#/api/enterprise/employee';
import {
  deleteSalesProductArrangement,
  getVehicleUsageHistorySuggestions,
  getSalesProductDetail,
  recordVehicleUsageHistory,
  saveSalesProductArrangement,
  updateSalesProductArrangements,
  updateSalesProduct,
} from '#/api/sales/product';
import {
  calculateVehicleQuote as calculateVehicleQuoteRule,
  getVehicleQuoteRuleAll,
} from '#/api/dispatch/vehicle-quote';
import { getExpenseItemAll } from '#/api/enterprise/expense-item';
import { getPurchaseRelationPage } from '#/api/purchase/relation';
import {
  getRelationTicketTemplateDetail,
  type RelationTicketTemplateApi,
} from '#/api/purchase/relation-ticket-template';
import { getPurchaseResourcePage } from '#/api/purchase/resource';
import { getSupplierAll, type SupplierApi } from '#/api/purchase/supplier';
import { buildRegionOptions } from '#/utils/region';

import {
  arrangementItemCash,
  arrangementItemCredit,
  arrangementItemTotal,
  buildSalesProductPayload,
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
  state: 'current' | 'pending';
};

type TeamBadgeItem = {
  color?: string;
  editorType?: QuickProfileEditorType;
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
type AutoCompleteOption = { label: string; value: string };
type DictItem = ProductDictionaryNamespace.Item;

type ScenicResourceRelationOption = {
  relationId: number;
  resourceId: number;
  resourceName: string;
  supplierId: number;
  supplierName?: string;
};

type ResourceRelationOption = ScenicResourceRelationOption;

type EditorOptionsCacheEntry = {
  employeeOptions: SelectOptionWithId[];
  projectOptions: SelectOptionWithId[];
  resourceOptions: SelectOptionWithId[];
  resourceRelationOptions: ResourceRelationOption[];
  scenicResourceRelationOptions: ScenicResourceRelationOption[];
  supplierOptions: SelectOptionWithId[];
  vehicleQuoteRuleOptions: SelectOption[];
};

type TeamProfile = {
  businessType?: string;
  departmentName?: string;
  escortName?: string;
  internalNote?: string;
  operatorName?: string;
  receptionStandard?: string;
  totalDistanceText?: string;
};

type QuickProfileEditorType = 'business_type' | 'department' | 'escort' | 'internal_note' | 'operator';

type QuickProfileEditorConfig = {
  buttonText: string;
  field: keyof TeamProfile;
  inputType: 'select' | 'textarea';
  label: string;
  optionsType?: 'business_type' | 'department' | 'employee';
  placeholder: string;
  required?: boolean;
  title: string;
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
  companyRebateRate: number;
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
  guideCommissionRate: number;
  guideName?: string;
  headFeeAmount: number;
  mealType?: string;
  noGuideReport: boolean;
  orderScope?: string;
  peopleCount: number;
  prepaidAmount: number;
  priceLines: SalesProductApi.ArrangementPriceLine[];
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
  vehicleInquiryRecords: SalesProductApi.VehicleInquiryRecord[];
  vehicleQuoteSnapshot: SalesProductApi.VehicleQuoteSnapshot;
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
  { label: '产品模板', state: 'current' },
  { label: '安排配置', state: 'pending' },
  { label: '生成团队', state: 'pending' },
  { label: '团队执行', state: 'pending' },
  { label: '财务结算', state: 'pending' },
];

const trafficTypeOptions: SelectOption[] = [
  { label: '飞机', value: '飞机' },
  { label: '高铁', value: '高铁' },
  { label: '火车', value: '火车' },
  { label: '邮轮', value: '邮轮' },
];

const mealTypeOptions: SelectOption[] = ['早餐', '中餐', '晚餐']
  .map((item) => ({ label: item, value: item }));

const breakfastOptions: SelectOption[] = ['桌早', '自助早', '打包早', '不含']
  .map((item) => ({ label: item, value: item }));

const fundOptions: SelectOption[] = ['不含', '含']
  .map((item) => ({ label: item, value: item }));

const trafficOrderOptions: SelectOption[] = [
  { label: '=不关联订单=', value: '=不关联订单=' },
];
const standardMealProjectOptions: SelectOptionWithId[] = ['标准餐', '豪华餐', '餐券', '其它']
  .map((item) => ({ label: item, value: item }));
const otherProjectOptions: SelectOptionWithId[] = ['礼品', '特产', '预收款', '保险', '购物返佣', '购物人头', '停车费', '房租', '水电', '其它']
  .map((item) => ({ label: item, value: item }));
const groundAgentProjectOptions: SelectOptionWithId[] = ['成人', '儿童', '车费', '综费', '接送费', '代收团款', '定金对公', '团费', '成本', '其它']
  .map((item) => ({ label: item, value: item }));
const shoppingCategoryOptions: SelectOptionWithId[] = ['乳胶', '茶叶', '翡翠', '厨具', '黄金饰品', '茶多酚', '土特产', '丝绸', '珍珠', '唐卡', '藏药', '其它']
  .map((item) => ({ label: item, value: item }));
const regionOptions = buildRegionOptions();
const defaultPriceQuantity = 0;
const TEAM_PROFILE_MARKER = '[[TEAM_PROFILE_JSON]]';

const quickProfileEditorConfigs: Record<QuickProfileEditorType, QuickProfileEditorConfig> = {
  business_type: {
    buttonText: '提交保存',
    field: 'businessType',
    inputType: 'select',
    label: '选择业务类型',
    optionsType: 'business_type',
    placeholder: '=业务类型=',
    required: true,
    title: '修改业务类型',
  },
  department: {
    buttonText: '提交保存',
    field: 'departmentName',
    inputType: 'select',
    label: '所属部门',
    optionsType: 'department',
    placeholder: '=选择部门=',
    required: true,
    title: '修改业务部门',
  },
  operator: {
    buttonText: '提交保存',
    field: 'operatorName',
    inputType: 'select',
    label: '操作计调',
    optionsType: 'employee',
    placeholder: '=选择操作计调=',
    required: true,
    title: '修改操作计调',
  },
  escort: {
    buttonText: '保存信息',
    field: 'escortName',
    inputType: 'textarea',
    label: '团队全陪信息',
    placeholder: '最多输入100个汉字',
    title: '团队全陪信息',
  },
  internal_note: {
    buttonText: '保存信息',
    field: 'internalNote',
    inputType: 'textarea',
    label: '内部备注',
    placeholder: '最多输入100个汉字',
    title: '内部备注',
  },
};

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
    title: '添加/修改地接信息',
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
    title: '添加/修改用餐信息',
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
    noGuideReport: true,
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
const editingArrangementIndex = ref(-1);
const optionsLoading = ref(false);
const quickProfileEditorOpen = ref(false);
const quickProfileEditorType = ref<QuickProfileEditorType>('business_type');
const teamProfileOptionsLoading = ref(false);
const vehicleQuoteCalculating = ref(false);
const activeEditorType = ref<SalesProductApi.ArrangementType>('traffic');
const formState = reactive<ProductFormState>(createDefaultProductForm());
const arrangementForm = reactive<ArrangementEditorForm>(createDefaultArrangementEditorForm('traffic'));
const teamProfile = reactive<TeamProfile>({});
const departureRegionPath = ref<RegionPath | undefined>([]);
const arrivalRegionPath = ref<RegionPath | undefined>([]);
const businessTypeOptions = ref<SelectOption[]>([]);
const departmentOptions = ref<SelectOption[]>([]);
const supplierOptions = ref<SelectOptionWithId[]>([]);
const projectOptions = ref<SelectOptionWithId[]>([]);
const resourceOptions = ref<SelectOptionWithId[]>([]);
const resourceRelationOptions = ref<ResourceRelationOption[]>([]);
const scenicResourceRelationOptions = ref<ScenicResourceRelationOption[]>([]);
const scenicTicketTemplate = ref<RelationTicketTemplateApi.Template | null>();
const scenicTicketTemplateLoading = ref(false);
const employeeOptions = ref<SelectOptionWithId[]>([]);
const profileEmployeeOptions = ref<SelectOption[]>([]);
const vehicleQuoteRuleOptions = ref<SelectOption[]>([]);
const driverHistoryOptions = ref<AutoCompleteOption[]>([]);
const vehiclePlateHistoryOptions = ref<AutoCompleteOption[]>([]);
const lastVehicleQuoteResult = ref<{
  amount: number;
  distanceMeters: number;
  ruleName: string;
}>();
const editorOptionsCache = new Map<SalesProductApi.ArrangementType, EditorOptionsCacheEntry>();

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
const totalRoadbookDistanceKilometers = computed(() => (
  ((formState.itineraryDays || []).reduce((sum, item) => (
    sum + Number(item.roadbookTotalDistanceMeters || 0)
  ), 0) / 1000).toFixed(1)
));
const editorTotalAmount = computed(() => (
  arrangementForm.priceLines.reduce((sum, line) => (
    sum + Number(line.unitPrice || 0) * Number(line.quantity || 0)
  ), 0)
));
const activeEditorTotalAmount = computed(() => {
  if (activeEditorType.value === 'optional') {
    return Number(arrangementForm.costAmount || 0);
  }
  if (activeEditorType.value === 'shopping') {
    return Number(arrangementForm.consumptionAmount || 0);
  }
  return editorTotalAmount.value;
});
const editorCreditAmount = computed(() => Math.max(
  activeEditorTotalAmount.value - Number(arrangementForm.cashAmount || 0) - Number(arrangementForm.prepaidAmount || 0),
  0,
));
const activeSection = computed(() => arrangementSections.find((item) => item.value === activeEditorType.value));
const activeEditorConfig = computed(() => arrangementEditorConfigs[activeEditorType.value]);
const activeEditorTitle = computed(() => activeEditorConfig.value.title);
const scheduleDayOptions = computed<SelectOption[]>(() => {
  const travelDays = Math.max(1, Number(formState.travelDays || 1));
  return [
    { label: '=出发日期=', value: '=出发日期=' },
    ...Array.from({ length: travelDays }, (_, index) => {
      const day = index + 1;
      return { label: `第${day}天`, value: `第${day}天` };
    }),
  ];
});
const selectedScenicResourceRelation = computed(() => (
  scenicResourceRelationOptions.value.find((item) => (
    item.resourceName === arrangementForm.resourceName
    && item.supplierId === arrangementForm.supplierId
  ))
));
const activeQuickProfileEditor = computed(() => quickProfileEditorConfigs[quickProfileEditorType.value]);
const quickProfileEditorModel = computed({
  get: () => teamProfile[activeQuickProfileEditor.value.field],
  set: (value?: string) => {
    teamProfile[activeQuickProfileEditor.value.field] = value;
  },
});
const showMultiOrderAveragePriceNotice = computed(() => (
  arrangementForm.allocationMode === 'multi_order_average'
));
watch(() => arrangementForm.allocationMode, (value) => {
  if (value === 'multi_order_average') {
    shrinkPriceLinesForMultiOrderAverage();
  }
});
const teamBadges = computed<TeamBadgeItem[]>(() => [
  { color: 'orange', editorType: 'business_type', label: '业务类型', value: teamProfile.businessType || formState.businessType || '未设置' },
  { color: 'blue', editorType: 'department', label: '部门', value: teamProfile.departmentName || '未设置' },
  { color: 'blue', editorType: 'operator', label: '操作计调', value: teamProfile.operatorName || '未设置' },
  { color: 'orange', label: '导游', value: '--' },
  { color: 'green', label: '领队', value: '--' },
  { color: 'default', editorType: 'escort', label: '全陪', value: teamProfile.escortName || '未设置' },
]);
const teamMetricItems = computed<TeamMetricItem[]>(() => [
  {
    label: '旅游天数',
    value: `${formState.travelDays || 1} 天`,
  },
  {
    label: '接待标准',
    value: teamProfile.receptionStandard || formState.receptionStandard || '未设置',
  },
  {
    label: '总里程数',
    value: teamProfile.totalDistanceText || `${totalRoadbookDistanceKilometers.value}公里`,
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

function dictionaryOptions(items: DictItem[]) {
  return items
    .filter((item) => item.status === 'active')
    .map((item) => ({
      label: item.dictName,
      value: item.dictName,
    }));
}

function encodeTeamProfileRemark(rawRemark?: string) {
  const text = String(rawRemark || '');
  const markerIndex = text.indexOf(TEAM_PROFILE_MARKER);
  const cleanRemark = (markerIndex >= 0 ? text.slice(0, markerIndex) : text).trim();
  const profile = JSON.stringify(teamProfile);
  return [cleanRemark, `${TEAM_PROFILE_MARKER}${profile}`].filter(Boolean).join('\n');
}

function decodeTeamProfileRemark(rawRemark?: string) {
  const text = String(rawRemark || '');
  const markerIndex = text.indexOf(TEAM_PROFILE_MARKER);
  const plainRemark = markerIndex >= 0 ? text.slice(0, markerIndex).trim() : text.trim();
  const profileText = markerIndex >= 0 ? text.slice(markerIndex + TEAM_PROFILE_MARKER.length).trim() : '';
  Object.assign(teamProfile, {
    businessType: undefined,
    departmentName: undefined,
    escortName: undefined,
    internalNote: undefined,
    operatorName: undefined,
    receptionStandard: undefined,
    totalDistanceText: undefined,
  });
  if (profileText) {
    try {
      Object.assign(teamProfile, JSON.parse(profileText));
    } catch {
      // 历史备注不是合法扩展 JSON 时，只保留原始备注，不影响页面打开。
    }
  }
  return plainRemark || undefined;
}

async function loadTeamProfileOptions() {
  teamProfileOptionsLoading.value = true;
  try {
    const [business, departments, employees] = await Promise.all([
      getProductDictionaryAll('business_type'),
      getEnterpriseDepartmentAll(false),
      getEnterpriseEmployeeAll(false),
    ]);
    businessTypeOptions.value = dictionaryOptions(business);
    departmentOptions.value = departments.map((item) => ({
      label: item.departmentName,
      value: item.departmentName,
    }));
    profileEmployeeOptions.value = employees.map((item) => ({
      label: item.employeeName,
      value: item.employeeName,
    }));
  } finally {
    teamProfileOptionsLoading.value = false;
  }
}

function quickProfileEditorOptions(config: QuickProfileEditorConfig) {
  if (config.optionsType === 'business_type') return businessTypeOptions.value;
  if (config.optionsType === 'department') return departmentOptions.value;
  if (config.optionsType === 'employee') return profileEmployeeOptions.value;
  return [];
}

async function openTeamProfileEditor(editorType: QuickProfileEditorType) {
  teamProfile.businessType ||= formState.businessType;
  quickProfileEditorType.value = editorType;
  quickProfileEditorOpen.value = true;
  if (activeQuickProfileEditor.value.inputType === 'select') {
    await loadTeamProfileOptions();
  }
}

async function saveTeamProfileEditor() {
  const editor = activeQuickProfileEditor.value;
  const currentValue = String(teamProfile[editor.field] || '').trim();
  if (editor.required && !currentValue) {
    message.warning(`请选择${editor.label}`);
    return;
  }
  formState.businessType = teamProfile.businessType || formState.businessType;
  formState.remark = encodeTeamProfileRemark(formState.remark);
  const saved = await persistArrangementChanges(`${editor.title}已保存`, { saveMode: 'full' });
  if (saved) {
    quickProfileEditorOpen.value = false;
  }
}

/** 把 `39座` 里的数字取出，仅用于报价规则下拉排序。 */
function seatCountFromVehicleType(value?: string) {
  const matched = String(value || '').match(/\d+/);
  return matched?.[0] ? Number(matched[0]) : undefined;
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
  const projectName = defaultProjectName(type);
  return {
    allocationMode: 'group_order_average',
    arrivalPlace: '',
    cashAmount: 0,
    companyRebateAmount: 0,
    companyRebateRate: 0,
    confirmed: type === 'hotel',
    consumptionAmount: 0,
    costAmount: 0,
    creditAmount: 0,
    daysCount: type === 'hotel' || type === 'vehicle' || type === 'ground_agent' ? 1 : 0,
    departurePlace: '',
    fundIncluded: '不含',
    guideCommissionAmount: 0,
    guideCommissionRate: 0,
    headFeeAmount: 0,
    mealType: type === 'meal' ? '中餐' : undefined,
    noGuideReport: false,
    orderScope: '=不关联订单=',
    peopleCount: 0,
    prepaidAmount: 0,
    priceLines: [createDefaultArrangementPriceLine(projectName)],
    projectName,
    quantity: defaultPriceQuantity,
    saleAmount: 0,
    scheduleEndDay: type === 'hotel' || type === 'vehicle' || type === 'ground_agent' ? '第2天' : undefined,
    scheduleStartDay: type === 'traffic' ? '=出发日期=' : '第1天',
    settlementType: 'credit',
    trafficType: type === 'traffic' ? '飞机' : undefined,
    unitPrice: 0,
    vehicleInquiryRecords: [],
    vehicleQuoteSnapshot: {
      calculatedAmount: 0,
      confirmedAmount: 0,
      syncedDistanceMeters: 0,
      syncedDurationSeconds: 0,
    },
    vehicleType: type === 'vehicle' ? '39座' : undefined,
  };
}

function resetArrangementForm(type: SalesProductApi.ArrangementType) {
  Object.assign(arrangementForm, createDefaultArrangementEditorForm(type));
  departureRegionPath.value = [];
  arrivalRegionPath.value = [];
}

/** 把表格中已有的安排记录回填到弹窗表单，用于修改已保存的产品模板安排。 */
function hydrateArrangementFormFromItem(item: SalesProductApi.ArrangementItem) {
  Object.assign(arrangementForm, createDefaultArrangementEditorForm(item.arrangementType));
  Object.assign(arrangementForm, {
    allocationMode: item.allocationMode || 'group_order_average',
    arrivalPlace: item.arrivalPlace,
    cashAmount: Number(item.cashAmount || 0),
    companyRebateAmount: Number(item.companyRebateAmount || 0),
    companyRebateRate: Number(item.priceLines?.[0]?.companyRebateRate || 0),
    confirmed: Boolean(item.confirmed),
    confirmationNo: item.confirmationNo,
    consumptionAmount: Number(item.consumptionAmount || 0),
    costAmount: Number(item.costAmount || 0),
    creditAmount: Number(item.creditAmount || 0),
    daysCount: Number(item.daysCount || 0),
    departurePlace: item.departurePlace,
    driverName: item.driverName,
    fundIncluded: item.fundIncluded,
    guideCommissionAmount: Number(item.guideCommissionAmount || 0),
    guideCommissionRate: Number(item.priceLines?.[0]?.guideCommissionRate || 0),
    headFeeAmount: Number(item.headFeeAmount || 0),
    mealType: item.mealType,
    noGuideReport: Boolean(item.noGuideReport),
    orderScope: item.orderScope || '=不关联订单=',
    peopleCount: Number(item.peopleCount || 0),
    prepaidAmount: Number(item.prepaidAmount || 0),
    priceLines: item.priceLines?.length
      ? item.priceLines.map((line, index) => ({
        amount: Number(line.amount || 0),
        projectId: line.projectId,
        projectName: line.projectName || item.projectName || defaultProjectName(item.arrangementType),
        quantity: Number(line.quantity || 0),
        remark: line.remark,
        salePrice: Number(line.salePrice || 0),
        sortOrder: line.sortOrder || index + 1,
        unitPrice: Number(line.unitPrice || 0),
        cashAmount: Number(line.cashAmount || 0),
        companyRebateAmount: Number(line.companyRebateAmount || 0),
        companyRebateRate: Number(line.companyRebateRate || 0),
        consumptionAmount: Number(line.consumptionAmount || 0),
        costPrice: Number(line.costPrice || 0),
        creditAmount: Number(line.creditAmount || 0),
        guideCommissionAmount: Number(line.guideCommissionAmount || 0),
        guideCommissionRate: Number(line.guideCommissionRate || 0),
        headFeeAmount: Number(line.headFeeAmount || 0),
      }))
      : [createDefaultArrangementPriceLine(item.projectName || defaultProjectName(item.arrangementType))],
    projectName: item.projectName,
    quantity: Number(item.quantity || 0),
    remark: manualRemarkText(item.remark),
    resourceName: item.resourceName,
    responsibleEmployeeId: item.responsibleEmployeeId,
    responsibleEmployeeName: item.responsibleEmployeeName,
    saleAmount: Number(item.saleAmount || 0),
    scheduleEndDay: item.scheduleEndDay,
    scheduleStartDay: item.scheduleStartDay,
    settlementType: item.settlementType || 'credit',
    supplierId: item.supplierId,
    supplierName: item.supplierName,
    trafficType: item.trafficType,
    unitPrice: Number(item.unitPrice || 0),
    vehicleInquiryRecords: item.vehicleInquiryRecords || [],
    vehiclePlate: item.vehiclePlate,
    vehicleQuoteSnapshot: item.vehicleQuoteSnapshot || {
      calculatedAmount: 0,
      confirmedAmount: 0,
      syncedDistanceMeters: 0,
      syncedDurationSeconds: 0,
    },
    vehicleType: item.vehicleType,
  });
  if (item.arrangementType === 'traffic') {
    departureRegionPath.value = [];
    arrivalRegionPath.value = [];
  }
  if (item.arrangementType === 'vehicle') {
    syncVehicleDaysCount();
  }
  if (item.arrangementType === 'hotel') {
    syncHotelNightsCount();
  }
  if (item.arrangementType === 'ground_agent') {
    syncGroundAgentDaysCount();
  }
  normalizeArrangementPriceLines();
}

function createDefaultArrangementPriceLine(projectName = ''): SalesProductApi.ArrangementPriceLine {
  return {
    projectName,
    quantity: defaultPriceQuantity,
    sortOrder: 1,
    unitPrice: 0,
  };
}

function normalizeArrangementPriceLines() {
  if (!arrangementForm.priceLines.length) {
    arrangementForm.priceLines.push(createDefaultArrangementPriceLine(defaultProjectName(activeEditorType.value)));
  }
  arrangementForm.priceLines.forEach((line, index) => {
    line.sortOrder = index + 1;
  });
  syncPrimaryPriceFields();
}

/** 老系统多订单均摊只能维护一条统一价格；切换后收回到第一条，避免保存出多行。 */
function shrinkPriceLinesForMultiOrderAverage() {
  if (arrangementForm.priceLines.length > 1) {
    arrangementForm.priceLines.splice(1);
  }
  normalizeArrangementPriceLines();
}

/** 保留旧表格列需要的单价、数量、项目名字段，真实明细以 priceLines 为准。 */
function syncPrimaryPriceFields() {
  const firstLine = arrangementForm.priceLines[0] || createDefaultArrangementPriceLine(defaultProjectName(activeEditorType.value));
  arrangementForm.projectId = firstLine.projectId;
  arrangementForm.projectName = firstLine.projectName || defaultProjectName(activeEditorType.value);
  arrangementForm.unitPrice = Number(firstLine.unitPrice || 0);
  arrangementForm.quantity = Number(firstLine.quantity || 0);
  arrangementForm.priceRemark = firstLine.remark;
}

function applySelectedPriceProject(index: number, value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  const project = priceProjectOptionsForType(activeEditorType.value).find((item) => item.value === selectedValue);
  const line = arrangementForm.priceLines[index];
  if (!line) return;
  line.projectId = project?.id;
  line.projectName = selectedValue;
  syncPrimaryPriceFields();
}

/** 部分老系统弹窗的费用项目是固定选项，不依赖后台费用项目字典。 */
function priceProjectOptionsForType(type: SalesProductApi.ArrangementType) {
  if (type === 'meal') return standardMealProjectOptions;
  if (type === 'other') return otherProjectOptions;
  if (type === 'ground_agent') return groundAgentProjectOptions;
  if (type === 'shopping') return shoppingCategoryOptions;
  return projectOptions.value;
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

function parseScheduleDayNo(value?: string) {
  if (!value) return undefined;
  const matched = value.match(/第\s*(\d+)\s*天/);
  return matched?.[1] ? Number(matched[1]) : undefined;
}

/** 按开始/结束天数计算跨几天，老系统口径包含首尾日期，例如第1天到第3天等于3天。 */
function scheduleInclusiveDaysCount(startValue?: string, endValue?: string) {
  const start = parseScheduleDayNo(startValue) || 1;
  const end = Math.max(start, parseScheduleDayNo(endValue) || start);
  return Math.max(1, end - start + 1);
}

/** 住宿晚数按退房日减入住日计算，例如第1天入住、第3天退房等于2晚。 */
function scheduleExclusiveNightsCount(startValue?: string, endValue?: string) {
  const start = parseScheduleDayNo(startValue) || 1;
  const end = Math.max(start, parseScheduleDayNo(endValue) || start + 1);
  return Math.max(0, end - start);
}

function routeDurationText(seconds?: number) {
  const totalSeconds = Number(seconds || 0);
  if (totalSeconds <= 0) return '0分钟';
  const minutes = Math.round(totalSeconds / 60);
  if (minutes < 60) return `${minutes}分钟`;
  return `${Math.floor(minutes / 60)}小时${minutes % 60}分钟`;
}

function vehicleDistanceText(meters?: number) {
  const value = Number(meters || 0);
  return `${(value / 1000).toFixed(1)}公里`;
}

function selectedVehicleDayRange() {
  const start = parseScheduleDayNo(arrangementForm.scheduleStartDay) || 1;
  const end = parseScheduleDayNo(arrangementForm.scheduleEndDay) || start;
  return {
    end: Math.max(start, end),
    start,
  };
}

/** 用车天数按开始/结束天数自动计算，包含首尾两天，避免用户手工填错。 */
function syncVehicleDaysCount() {
  const { end, start } = selectedVehicleDayRange();
  const days = end - start + 1;
  arrangementForm.daysCount = Math.max(1, days);
}

/** 住宿“共几晚”由入住/退房自动推算，避免手工填错。 */
function syncHotelNightsCount() {
  arrangementForm.daysCount = scheduleExclusiveNightsCount(
    arrangementForm.scheduleStartDay,
    arrangementForm.scheduleEndDay,
  );
}

/** 地接拼团日期的“共几天”由开始/结束自动推算，避免第1天到第3天仍保存为1天。 */
function syncGroundAgentDaysCount() {
  const days = scheduleInclusiveDaysCount(
    arrangementForm.scheduleStartDay,
    arrangementForm.scheduleEndDay,
  );
  arrangementForm.daysCount = days;
}

function syncVehicleRoadbookDistance() {
  syncVehicleDaysCount();
  const { end, start } = selectedVehicleDayRange();
  const selectedDays = (formState.itineraryDays || []).filter((day) => day.dayNo >= start && day.dayNo <= end);
  const syncedDistanceMeters = selectedDays.reduce((sum, day) => sum + Number(day.roadbookTotalDistanceMeters || 0), 0);
  const syncedDurationSeconds = selectedDays.reduce((sum, day) => sum + Number(day.roadbookTotalDurationSeconds || 0), 0);
  const routeSummary = selectedDays
    .map((day) => `第${day.dayNo}天：${day.roadbookSummary || day.roadbookPlace || day.dayTitle || '未维护路书'}`)
    .join('；');
  arrangementForm.vehicleQuoteSnapshot = {
    ...(arrangementForm.vehicleQuoteSnapshot || {}),
    endDayNo: end,
    routeSummary,
    scheduleEndDay: arrangementForm.scheduleEndDay,
    scheduleStartDay: arrangementForm.scheduleStartDay,
    startDayNo: start,
    syncedDistanceMeters,
    syncedDurationSeconds,
  };
  if (!selectedDays.length || syncedDistanceMeters <= 0) {
    message.warning('当前用车日期范围内还没有路书公里');
    return;
  }
  message.success(`已同步 ${vehicleDistanceText(syncedDistanceMeters)}，约 ${routeDurationText(syncedDurationSeconds)}`);
}

async function calculateVehicleReferencePrice() {
  const distanceMeters = arrangementForm.vehicleQuoteSnapshot?.syncedDistanceMeters || 0;
  const vehicleType = arrangementForm.vehicleType;
  if (!vehicleType) {
    message.warning('请先选择座位数规则');
    return;
  }
  if (!distanceMeters) {
    syncVehicleRoadbookDistance();
  }
  const finalDistanceMeters = arrangementForm.vehicleQuoteSnapshot?.syncedDistanceMeters || 0;
  if (!finalDistanceMeters) return;
  vehicleQuoteCalculating.value = true;
  try {
    const result = await calculateVehicleQuoteRule({
      distanceMeters: finalDistanceMeters,
      vehicleType,
    });
    arrangementForm.vehicleQuoteSnapshot = {
      ...(arrangementForm.vehicleQuoteSnapshot || {}),
      calculatedAmount: Number(result.calculatedAmount || 0),
      confirmedAmount: Number(result.calculatedAmount || 0),
      quoteRuleId: result.ruleSnapshot.ruleId,
      ruleBaseKilometers: Number(result.ruleSnapshot.baseKilometers || 0),
      ruleBasePrice: Number(result.ruleSnapshot.basePrice || 0),
      ruleCity: result.ruleSnapshot.city,
      ruleDistrict: result.ruleSnapshot.district,
      ruleExtraKilometerPrice: Number(result.ruleSnapshot.extraKilometerPrice || 0),
      ruleFloatRate: Number(result.ruleSnapshot.floatRate || 1),
      ruleMinimumPrice: Number(result.ruleSnapshot.minimumPrice || 0),
      ruleProvince: result.ruleSnapshot.province,
      ruleVehicleType: result.ruleSnapshot.vehicleType,
    };
    lastVehicleQuoteResult.value = {
      amount: Number(result.calculatedAmount || 0),
      distanceMeters: Number(result.distanceMeters || 0),
      ruleName: `${result.ruleSnapshot.vehicleType} 通用规则`,
    };
    message.success(`测算参考价 ${formatMoney(Number(result.calculatedAmount || 0))}`);
  } finally {
    vehicleQuoteCalculating.value = false;
  }
}

function applyVehicleQuoteToPriceInfo(amount?: number) {
  const finalAmount = Number(amount ?? arrangementForm.vehicleQuoteSnapshot?.confirmedAmount ?? 0);
  if (finalAmount <= 0) {
    message.warning('没有可应用的报价金额');
    return;
  }
  const firstLine = arrangementForm.priceLines[0] || createDefaultArrangementPriceLine('车费');
  if (!arrangementForm.priceLines.length) {
    arrangementForm.priceLines.push(firstLine);
  }
  firstLine.projectName = '车费';
  firstLine.quantity = 1;
  firstLine.unitPrice = finalAmount;
  arrangementForm.cashAmount = 0;
  arrangementForm.prepaidAmount = 0;
  syncPrimaryPriceFields();
  message.success('已应用到价格信息');
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

/** 备注只保留用户手写内容；兼容清理旧逻辑曾拼入的费用归属、订单信息等系统字段。 */
function manualRemarkText(value?: string) {
  const text = String(value || '').trim();
  if (!text) return undefined;
  const segments = text.split('；').map((segment) => segment.trim()).filter(Boolean);
  const hasGeneratedSegments = segments.some((segment) => (
    segment.startsWith('订单信息：')
    || segment.startsWith('价格备注：')
    || segment === '无需导游报账，同步更新导游报账和计调审核数据'
    || segment.startsWith('费用归属：全团/订单均摊')
    || segment.startsWith('费用归属：多订单均摊成本')
  ));
  if (!hasGeneratedSegments) return text;
  const manualSegments = segments
    .map((segment) => {
      if (
        segment.startsWith('费用归属：')
        || segment.startsWith('订单信息：')
        || segment.startsWith('价格备注：')
        || segment === '无需导游报账，同步更新导游报账和计调审核数据'
      ) {
        return '';
      }
      return segment.startsWith('备注：') ? segment.slice(3).trim() : segment;
    })
    .filter(Boolean);
  return manualSegments.join('；') || undefined;
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
  if (column === '备注') return manualRemarkText(item.remark) || '';
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
  return item.arrangementContent || '';
}

function showStaticFeatureTip() {
  message.info('正式团队安排模块接入后可用');
}

/** 普通费用归属下允许继续追加价格组成，形成旧系统同款多行价格信息。 */
function addArrangementPriceLine() {
  if (showMultiOrderAveragePriceNotice.value) return;
  arrangementForm.priceLines.push(createDefaultArrangementPriceLine(defaultProjectName(activeEditorType.value)));
  normalizeArrangementPriceLines();
}

function addOptionalSummaryLine() {
  if (showMultiOrderAveragePriceNotice.value) return;
  const line = createDefaultArrangementPriceLine('成人');
  line.quantity = Number(arrangementForm.peopleCount || 0);
  line.salePrice = 0;
  line.costPrice = 0;
  line.cashAmount = 0;
  line.guideCommissionAmount = 0;
  arrangementForm.priceLines.push(line);
  normalizeArrangementPriceLines();
  syncOptionalLineToForm();
}

function addShoppingConsumptionLine() {
  const line = createDefaultArrangementPriceLine('乳胶');
  line.consumptionAmount = 0;
  line.companyRebateRate = Number(arrangementForm.companyRebateRate || 0);
  line.companyRebateAmount = 0;
  line.guideCommissionRate = Number(arrangementForm.guideCommissionRate || 0);
  line.guideCommissionAmount = 0;
  line.cashAmount = 0;
  arrangementForm.priceLines.push(line);
  normalizeArrangementPriceLines();
  syncShoppingLineToForm();
}

function optionalLineCreditAmount(line: SalesProductApi.ArrangementPriceLine) {
  return Math.max(Number(line.costPrice || 0) - Number(line.cashAmount || 0), 0);
}

function applyShoppingPriceProject(index: number, value?: unknown) {
  applySelectedPriceProject(index, value);
  syncShoppingLineToForm();
}

function removeOptionalSummaryLine(index: number) {
  removeArrangementPriceLine(index);
  syncOptionalLineToForm();
}

function removeShoppingConsumptionLine(index: number) {
  removeArrangementPriceLine(index);
  syncShoppingLineToForm();
}

/** 自费弹窗支持多条费用合计，汇总字段用于表格列和保存总额。 */
function syncOptionalLineToForm() {
  normalizeArrangementPriceLines();
  let peopleCount = 0;
  let saleAmount = 0;
  let costAmount = 0;
  let cashAmount = 0;
  let guideCommissionAmount = 0;
  arrangementForm.priceLines.forEach((line) => {
    const quantity = Number(line.quantity || 0);
    const salePrice = Number(line.salePrice || 0);
    const costPrice = Number(line.costPrice || 0);
    peopleCount += quantity;
    saleAmount += salePrice;
    costAmount += costPrice;
    cashAmount += Number(line.cashAmount || 0);
    guideCommissionAmount += Number(line.guideCommissionAmount || 0);
  });
  arrangementForm.peopleCount = peopleCount;
  arrangementForm.saleAmount = saleAmount;
  arrangementForm.costAmount = costAmount;
  arrangementForm.cashAmount = cashAmount;
  arrangementForm.guideCommissionAmount = guideCommissionAmount;
  syncPrimaryPriceFields();
}

/** 购物消费详情按多品类录入，汇总到进店人数、消费额、返佣和提成。 */
function syncShoppingLineToForm() {
  normalizeArrangementPriceLines();
  let consumptionAmount = 0;
  let cashAmount = 0;
  let companyRebateAmount = 0;
  let guideCommissionAmount = 0;
  arrangementForm.priceLines.forEach((line) => {
    const lineConsumption = Number(line.consumptionAmount || 0);
    const lineCompanyRebateRate = Number(line.companyRebateRate || 0);
    const lineGuideCommissionRate = Number(line.guideCommissionRate || 0);
    if (!Number(line.companyRebateAmount || 0) && lineConsumption && lineCompanyRebateRate) {
      line.companyRebateAmount = Number((lineConsumption * lineCompanyRebateRate / 100).toFixed(2));
    }
    if (!Number(line.guideCommissionAmount || 0) && lineConsumption && lineGuideCommissionRate) {
      line.guideCommissionAmount = Number((lineConsumption * lineGuideCommissionRate / 100).toFixed(2));
    }
    consumptionAmount += lineConsumption;
    cashAmount += Number(line.cashAmount || 0);
    companyRebateAmount += Number(line.companyRebateAmount || 0);
    guideCommissionAmount += Number(line.guideCommissionAmount || 0);
  });
  arrangementForm.consumptionAmount = consumptionAmount;
  arrangementForm.cashAmount = cashAmount;
  arrangementForm.companyRebateAmount = companyRebateAmount;
  arrangementForm.guideCommissionAmount = guideCommissionAmount;
  syncPrimaryPriceFields();
}

/** 至少保留一条价格组成；删除后重新整理排序和旧字段同步。 */
function removeArrangementPriceLine(index: number) {
  if (arrangementForm.priceLines.length <= 1) {
    message.warning('价格信息至少保留一条');
    return;
  }
  arrangementForm.priceLines.splice(index, 1);
  normalizeArrangementPriceLines();
}

async function deleteArrangementItem(item: SalesProductApi.ArrangementItem) {
  const items = formState.arrangementItems || [];
  const index = items.indexOf(item);
  if (index < 0) return;
  const previousItems = [...items];
  const nextItems = [...items];
  nextItems.splice(index, 1);
  formState.arrangementItems = nextItems;
  if (!item.id) {
    message.success('安排信息已删除');
    return;
  }
  if (!productId.value) {
    formState.arrangementItems = previousItems;
    message.warning('缺少产品ID');
    return;
  }
  saving.value = true;
  try {
    await deleteSalesProductArrangement(productId.value, item.id);
    message.success('安排信息已删除');
  } catch (error) {
    formState.arrangementItems = previousItems;
    throw error;
  } finally {
    saving.value = false;
  }
}

/** 删除团队安排会直接改产品模板，必须二次确认，避免列表行误点造成数据丢失。 */
function confirmDeleteArrangementItem(item: SalesProductApi.ArrangementItem) {
  Modal.confirm({
    cancelText: '取消',
    content: `删除后会立即保存到产品团队安排，当前记录为：${item.resourceName || item.itemName || item.arrangementContent || '未命名安排'}。`,
    okButtonProps: { danger: true },
    okText: '确认删除',
    title: '确认删除这条安排？',
    async onOk() {
      await deleteArrangementItem(item);
    },
  });
}

function openSupplierCreatePage() {
  const routeInfo = router.resolve({
    path: '/purchase/supplier',
    query: {
      category: supplierCategoryMap[activeEditorType.value],
      create: '1',
    },
  });
  window.open(routeInfo.href, '_blank', 'noopener,noreferrer');
}

function openHotelCreatePage() {
  const routeInfo = router.resolve({
    path: '/purchase/resource',
    query: {
      create: '1',
      resourceType: 'hotel',
    },
  });
  window.open(routeInfo.href, '_blank', 'noopener,noreferrer');
}

function openResourceCreatePage() {
  const routeInfo = router.resolve({
    path: '/purchase/resource',
    query: {
      create: '1',
      resourceType: expenseResourceTypeMap[activeEditorType.value],
    },
  });
  window.open(routeInfo.href, '_blank', 'noopener,noreferrer');
}

/** 关闭弹窗时清理编辑位置，避免从“修改”切回“新增”时误替换上一条记录。 */
function closeArrangementEditor() {
  arrangementModalOpen.value = false;
  editingArrangementIndex.value = -1;
}

async function openArrangementEditor(
  type: SalesProductApi.ArrangementType,
  item?: SalesProductApi.ArrangementItem,
) {
  activeEditorType.value = type;
  if (item) {
    editingArrangementIndex.value = (formState.arrangementItems || []).indexOf(item);
    hydrateArrangementFormFromItem(item);
  } else {
    editingArrangementIndex.value = -1;
    resetArrangementForm(type);
  }
  if (type === 'vehicle') {
    syncVehicleDaysCount();
  }
  if (type === 'hotel') {
    syncHotelNightsCount();
  }
  if (type === 'ground_agent') {
    syncGroundAgentDaysCount();
  }
  if (type === 'optional') {
    syncOptionalLineToForm();
  }
  if (type === 'shopping') {
    syncShoppingLineToForm();
  }
  arrangementModalOpen.value = true;
  await loadEditorOptions(type);
  if (type === 'vehicle') {
    await Promise.all([
      loadVehicleHistoryOptions('driver_info'),
      loadVehicleHistoryOptions('vehicle_plate'),
    ]);
  }
}

/** 从用车安排直接打开产品行程里的路书地图，方便修改地址后再回到弹窗同步公里。 */
function openProductRoadbookEditor() {
  if (!productId.value) {
    message.warning('请先保存产品后再编辑路书地图');
    return;
  }
  const { start } = selectedVehicleDayRange();
  const routeInfo = router.resolve({
    path: `/sales/product/edit/${productId.value}`,
    query: {
      roadbookDay: String(start),
      tab: 'itinerary',
    },
  });
  window.open(routeInfo.href, '_blank', 'noopener,noreferrer');
}

function scrollToArrangementAnchor(anchor: string) {
  document.getElementById(anchor)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

/** 将缓存的下拉数据回填到弹窗，数组做浅拷贝，避免后续联动过滤直接污染缓存。 */
function applyEditorOptionsCache(type: SalesProductApi.ArrangementType, cacheEntry: EditorOptionsCacheEntry) {
  supplierOptions.value = [...cacheEntry.supplierOptions];
  projectOptions.value = [...cacheEntry.projectOptions];
  employeeOptions.value = [...cacheEntry.employeeOptions];
  resourceOptions.value = [...cacheEntry.resourceOptions];
  resourceRelationOptions.value = [...cacheEntry.resourceRelationOptions];
  scenicResourceRelationOptions.value = [...cacheEntry.scenicResourceRelationOptions];
  vehicleQuoteRuleOptions.value = [...cacheEntry.vehicleQuoteRuleOptions];
  if (type === 'scenic' || type === 'optional' || type === 'meal' || type === 'shopping') {
    loadResourceSupplierOptions(arrangementForm.resourceName);
  } else {
    scenicTicketTemplate.value = null;
  }
}

/** 只有住宿、用车等需要选择责任人的弹窗才加载员工，避免用餐/景区弹窗产生无关慢请求。 */
function editorNeedsEmployeeOptions(type: SalesProductApi.ArrangementType) {
  return Boolean(arrangementEditorConfigs[type]?.showResponsible);
}

async function loadEditorOptions(type: SalesProductApi.ArrangementType, force = false) {
  const cachedOptions = editorOptionsCache.get(type);
  if (cachedOptions && !force) {
    applyEditorOptionsCache(type, cachedOptions);
    return;
  }
  optionsLoading.value = true;
  try {
    const supplierCategory = supplierCategoryMap[type];
    const [suppliers, projects, employees, resources, quoteRules] = await Promise.all([
      type === 'scenic' || type === 'optional' || type === 'meal' || type === 'shopping'
        ? Promise.resolve([])
        : getSupplierAll(supplierCategory),
      getExpenseItemAll(expenseResourceTypeMap[type] as never),
      editorNeedsEmployeeOptions(type) ? getEnterpriseEmployeeAll(false) : Promise.resolve([]),
      loadResourceOptions(type),
      type === 'vehicle' ? loadVehicleQuoteRuleOptions() : Promise.resolve([]),
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
    if (type === 'scenic' || type === 'optional' || type === 'meal' || type === 'shopping') {
      loadResourceSupplierOptions(arrangementForm.resourceName);
    } else {
      resourceRelationOptions.value = [];
      scenicResourceRelationOptions.value = [];
      scenicTicketTemplate.value = null;
    }
    if (type === 'vehicle') {
      vehicleQuoteRuleOptions.value = quoteRules;
    }
    editorOptionsCache.set(type, {
      employeeOptions: [...employeeOptions.value],
      projectOptions: [...projectOptions.value],
      resourceOptions: [...resourceOptions.value],
      resourceRelationOptions: [...resourceRelationOptions.value],
      scenicResourceRelationOptions: [...scenicResourceRelationOptions.value],
      supplierOptions: [...supplierOptions.value],
      vehicleQuoteRuleOptions: [...vehicleQuoteRuleOptions.value],
    });
  } finally {
    optionsLoading.value = false;
  }
}

/** 加载已启用的车型/座位报价规则，供用车安排选择，不在安排弹窗手填座位数。 */
async function loadVehicleQuoteRuleOptions(): Promise<SelectOption[]> {
  const rules = await getVehicleQuoteRuleAll();
  return rules
    .filter((rule) => rule.status === 'active')
    .map((rule) => ({
      label: rule.vehicleType,
      value: rule.vehicleType,
    }))
    .sort((current, next) => (
      (seatCountFromVehicleType(current.value) || 0) - (seatCountFromVehicleType(next.value) || 0)
    ));
}

async function loadResourceOptions(type: SalesProductApi.ArrangementType): Promise<SelectOptionWithId[]> {
  resourceRelationOptions.value = [];
  scenicResourceRelationOptions.value = [];
  if (type === 'hotel') {
    const result = await getPurchaseResourcePage({
      page: 1,
      pageSize: 200,
      resourceType: 'hotel',
      status: 'active',
    });
    return result.items.map((item) => ({
      id: item.id,
      label: [item.resourceName, item.city, item.district].filter(Boolean).join(' / '),
      value: item.resourceName,
    }));
  }
  if (type === 'scenic' || type === 'optional') {
    const [resources, relations] = await Promise.all([
      getPurchaseResourcePage({
        page: 1,
        pageSize: 200,
        resourceType: 'scenic',
        status: 'active',
      }),
      getPurchaseRelationPage({
        page: 1,
        pageSize: 200,
        resourceType: 'scenic',
        status: 'active',
      }),
    ]);
    resourceRelationOptions.value = relations.items.map((item) => ({
      relationId: item.id,
      resourceId: item.resourceId,
      resourceName: item.resourceName,
      supplierId: item.supplierId,
      supplierName: item.supplierName,
    }));
    scenicResourceRelationOptions.value = resourceRelationOptions.value;
    return resources.items.map((item) => ({
      id: item.id,
      label: [
        item.resourceName,
        item.city,
        item.district,
        item.boundSupplierCount ? `${item.boundSupplierCount}家供应商` : '',
      ].filter(Boolean).join(' / '),
      value: item.resourceName,
    }));
  }
  if (type === 'meal') {
    const [resources, relations] = await Promise.all([
      getPurchaseResourcePage({
        page: 1,
        pageSize: 200,
        resourceType: 'restaurant',
        status: 'active',
      }),
      getPurchaseRelationPage({
        page: 1,
        pageSize: 200,
        resourceType: 'restaurant',
        status: 'active',
      }),
    ]);
    resourceRelationOptions.value = relations.items.map((item) => ({
      relationId: item.id,
      resourceId: item.resourceId,
      resourceName: item.resourceName,
      supplierId: item.supplierId,
      supplierName: item.supplierName,
    }));
    return resources.items.map((item) => ({
      id: item.id,
      label: [
        item.resourceName,
        item.city,
        item.district,
        item.boundSupplierCount ? `${item.boundSupplierCount}家供应商` : '',
      ].filter(Boolean).join(' / '),
      value: item.resourceName,
    }));
  }
  if (type === 'shopping') {
    const [resources, relations] = await Promise.all([
      getPurchaseResourcePage({
        page: 1,
        pageSize: 200,
        resourceType: 'shopping',
        status: 'active',
      }),
      getPurchaseRelationPage({
        page: 1,
        pageSize: 200,
        resourceType: 'shopping',
        status: 'active',
      }),
    ]);
    resourceRelationOptions.value = relations.items.map((item) => ({
      relationId: item.id,
      resourceId: item.resourceId,
      resourceName: item.resourceName,
      supplierId: item.supplierId,
      supplierName: item.supplierName,
    }));
    return resources.items.map((item) => ({
      id: item.id,
      label: [
        item.resourceName,
        item.city,
        item.district,
        item.boundSupplierCount ? `${item.boundSupplierCount}家供应商` : '',
      ].filter(Boolean).join(' / '),
      value: item.resourceName,
    }));
  }
  return [];
}

/** 加载司机或车牌历史候选；这些候选来自历史手动输入，不是固定档案。 */
async function loadVehicleHistoryOptions(
  historyType: SalesProductApi.VehicleUsageHistoryType,
  keyword?: string,
) {
  const result = await getVehicleUsageHistorySuggestions({
    historyType,
    keyword,
    limit: 20,
  });
  const options = result.map((item) => ({
    label: `${item.content} · 使用 ${item.usageCount} 次`,
    value: item.content,
  }));
  if (historyType === 'driver_info') {
    driverHistoryOptions.value = options;
  } else {
    vehiclePlateHistoryOptions.value = options;
  }
}

/** 保存成功后沉淀用车历史候选，空值不提交。 */
async function recordVehicleHistoryUsage(
  historyType: SalesProductApi.VehicleUsageHistoryType,
  content?: string,
) {
  if (!content?.trim()) return;
  await recordVehicleUsageHistory({
    content: content.trim(),
    historyType,
  });
}

function normalizeSelectValue(value: unknown) {
  return typeof value === 'string' ? value : undefined;
}

function resourceRelationSupplierOptions(selectedResourceName?: string) {
  const selectedResource = resourceOptions.value.find((item) => item.value === selectedResourceName);
  const filteredRelations = resourceRelationOptions.value.filter((item) => (
    selectedResource?.id ? item.resourceId === selectedResource.id : item.resourceName === selectedResourceName
  ));
  const uniqueSupplierMap = new Map<number, SelectOptionWithId>();
  filteredRelations.forEach((item) => {
    uniqueSupplierMap.set(item.supplierId, {
      id: item.supplierId,
      label: item.supplierName || `供应商${item.supplierId}`,
      value: item.supplierName || `供应商${item.supplierId}`,
    });
  });
  return [...uniqueSupplierMap.values()];
}

/** 有资源主档的团队安排必须按采购关系过滤供应商，避免资源和供应商错配。 */
function loadResourceSupplierOptions(value?: unknown) {
  const selectedResourceName = normalizeSelectValue(value) || arrangementForm.resourceName;
  if (!selectedResourceName) {
    supplierOptions.value = [];
    arrangementForm.supplierId = undefined;
    arrangementForm.supplierName = undefined;
    scenicTicketTemplate.value = null;
    return;
  }

  const nextSupplierOptions = resourceRelationSupplierOptions(selectedResourceName);
  supplierOptions.value = nextSupplierOptions;
  const currentSupplier = nextSupplierOptions.find((item) => item.value === arrangementForm.supplierName);
  const defaultSupplier = currentSupplier || nextSupplierOptions[0];
  arrangementForm.supplierId = defaultSupplier?.id;
  arrangementForm.supplierName = defaultSupplier?.value;
  if (activeEditorType.value === 'scenic' || activeEditorType.value === 'optional') {
    void loadSelectedScenicTicketTemplate();
  } else {
    scenicTicketTemplate.value = null;
  }
}

/** 选择资源后立即刷新供应商下拉，保持老系统资源名称联动供应商的操作习惯。 */
function applySelectedResource(value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  arrangementForm.resourceName = selectedValue;
  if (
    activeEditorType.value === 'scenic'
    || activeEditorType.value === 'optional'
    || activeEditorType.value === 'meal'
    || activeEditorType.value === 'shopping'
  ) {
    loadResourceSupplierOptions(selectedValue);
  }
}

function applySelectedSupplier(value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  const supplier = supplierOptions.value.find((item) => item.value === selectedValue);
  arrangementForm.supplierId = supplier?.id;
  arrangementForm.supplierName = selectedValue;
  if (activeEditorType.value === 'scenic' || activeEditorType.value === 'optional') {
    void loadSelectedScenicTicketTemplate();
  }
}

/** 查询当前景区采购关系上的游客名单模板，供弹窗提示后续能否生成预约 Excel。 */
async function loadSelectedScenicTicketTemplate() {
  const relation = selectedScenicResourceRelation.value;
  scenicTicketTemplate.value = null;
  if (!relation?.relationId) return;
  scenicTicketTemplateLoading.value = true;
  try {
    scenicTicketTemplate.value = await getRelationTicketTemplateDetail(relation.relationId);
  } finally {
    scenicTicketTemplateLoading.value = false;
  }
}

function openScenicTemplateConfigPage() {
  const routeInfo = router.resolve({
    path: '/purchase/relation',
    query: {
      resourceType: 'scenic',
      templateRelationId: selectedScenicResourceRelation.value?.relationId,
    },
  });
  window.open(routeInfo.href, '_blank', 'noopener,noreferrer');
}

function applySelectedResponsible(value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  const employee = employeeOptions.value.find((item) => item.value === selectedValue);
  arrangementForm.responsibleEmployeeId = employee?.id;
  arrangementForm.responsibleEmployeeName = selectedValue;
}

function formatTrafficRegionPath(path?: RegionPath) {
  return (path || []).filter(Boolean).join(' / ');
}

/** 保存产品团队安排参数；核心明细保存后默认刷新详情，避免本地临时数据和数据库状态不一致。 */
async function persistArrangementChanges(
  successText: string,
  options: { reloadAfterSave?: boolean; saveMode?: 'arrangements' | 'full' } = {},
) {
  if (!productId.value) {
    message.warning('缺少产品ID');
    return false;
  }
  saving.value = true;
  try {
    const payload = buildSalesProductPayload(
      formState,
      [formState.province, formState.city, formState.district].filter(Boolean) as string[],
    );
    payload.remark = encodeTeamProfileRemark(formState.remark);
    if (options.saveMode === 'full') {
      await updateSalesProduct(productId.value, payload);
    } else {
      await updateSalesProductArrangements(productId.value, {
        arrangementItems: payload.arrangementItems,
      });
    }
    message.success(successText);
    if (options.reloadAfterSave ?? true) {
      await loadDetail();
    }
    return true;
  } catch (error) {
    if (options.reloadAfterSave ?? true) {
      await loadDetail();
    }
    throw error;
  } finally {
    saving.value = false;
  }
}

/**
 * 保存当前弹窗中的单条团队安排。
 *
 * <p>新增和修改都只提交当前安排，避免一次车调修改重建整个产品的全部团队安排。</p>
 */
async function persistSingleArrangementChange(
  arrangementItem: SalesProductApi.ArrangementItem,
  successText: string,
  arrangementId?: number,
) {
  if (!productId.value) {
    message.warning('缺少产品ID');
    return undefined;
  }
  saving.value = true;
  try {
    const saved = await saveSalesProductArrangement(productId.value, {
      arrangementId,
      item: arrangementItem,
    });
    message.success(successText);
    return saved.id;
  } finally {
    saving.value = false;
  }
}

/** 保存当前分类安排到产品模板明细并立即写入后端。 */
async function saveArrangementEditor() {
  const type = activeEditorType.value;
  if (type === 'vehicle') {
    if (!arrangementForm.vehicleType) {
      message.warning('请先选择座位数规则');
      return;
    }
  }
  if (type === 'hotel') {
    syncHotelNightsCount();
  }
  if (type === 'ground_agent') {
    syncGroundAgentDaysCount();
  }
  if (type === 'traffic') {
    arrangementForm.departurePlace = formatTrafficRegionPath(departureRegionPath.value) || arrangementForm.departurePlace;
    arrangementForm.arrivalPlace = formatTrafficRegionPath(arrivalRegionPath.value) || arrangementForm.arrivalPlace;
  }
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

  normalizeArrangementPriceLines();
  const priceLines = arrangementForm.priceLines.map((line, index) => {
    const quantity = Number(line.quantity || 0);
    const unitPrice = Number(line.unitPrice || 0);
    const amount = unitPrice * quantity;
    return {
      amount,
      cashAmount: Number(line.cashAmount ?? ((index === 0 ? arrangementForm.cashAmount : 0) || 0)),
      companyRebateAmount: Number(line.companyRebateAmount ?? (arrangementForm.companyRebateAmount || 0)),
      companyRebateRate: Number(line.companyRebateRate ?? (arrangementForm.companyRebateRate || 0)),
      consumptionAmount: Number(line.consumptionAmount ?? (arrangementForm.consumptionAmount || 0)),
      costPrice: Number(line.costPrice ?? (arrangementForm.costAmount || 0)),
      creditAmount: index === 0 ? editorCreditAmount.value : 0,
      guideCommissionAmount: Number(line.guideCommissionAmount ?? (arrangementForm.guideCommissionAmount || 0)),
      guideCommissionRate: Number(line.guideCommissionRate ?? (arrangementForm.guideCommissionRate || 0)),
      headFeeAmount: Number(line.headFeeAmount ?? (arrangementForm.headFeeAmount || 0)),
      projectId: line.projectId,
      projectName: line.projectName || defaultProjectName(type),
      quantity,
      remark: line.remark,
      salePrice: Number(line.salePrice ?? (arrangementForm.saleAmount || 0)),
      sortOrder: index + 1,
      unitPrice,
    };
  });
  const firstPriceLine = priceLines[0] || createDefaultArrangementPriceLine(defaultProjectName(type));
  const totalAmount = activeEditorTotalAmount.value;
  const creditAmount = editorCreditAmount.value;
  const prepaidAmount = Number(arrangementForm.prepaidAmount || 0);
  const projectName = firstPriceLine.projectName || defaultProjectName(type);
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
    priceLines,
    projectName,
    quantity: Number(firstPriceLine.quantity || 0),
    remark: manualRemarkText(arrangementForm.remark),
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
    unitPrice: Number(firstPriceLine.unitPrice || 0),
    vehiclePlate: arrangementForm.vehiclePlate,
    vehicleInquiryRecords: type === 'vehicle' ? arrangementForm.vehicleInquiryRecords.map((item, index) => ({
      ...item,
      quotedAmount: Number(item.quotedAmount || 0),
      sortOrder: index + 1,
    })) : undefined,
    vehicleQuoteSnapshot: type === 'vehicle' ? arrangementForm.vehicleQuoteSnapshot : undefined,
    vehicleType: arrangementForm.vehicleType,
  };

  const previousItems = [...(formState.arrangementItems || [])];
  const nextItems = [...previousItems];
  const currentArrangementId = editingArrangementIndex.value >= 0
    ? previousItems[editingArrangementIndex.value]?.id
    : undefined;
  if (editingArrangementIndex.value >= 0) {
    nextItems[editingArrangementIndex.value] = arrangementItem;
  } else {
    nextItems.push(arrangementItem);
  }
  formState.arrangementItems = nextItems.filter((item) => item.itemName?.trim());
  const savedArrangementId = await persistSingleArrangementChange(
    arrangementItem,
    editingArrangementIndex.value >= 0 ? '安排信息已修改' : '安排信息已保存',
    currentArrangementId,
  );
  if (!savedArrangementId) {
    formState.arrangementItems = previousItems;
    return;
  }
  arrangementItem.id = savedArrangementId;
  if (editingArrangementIndex.value >= 0) {
    formState.arrangementItems[editingArrangementIndex.value] = arrangementItem;
  } else {
    formState.arrangementItems[formState.arrangementItems.length - 1] = arrangementItem;
  }
  if (savedArrangementId) {
    if (type === 'vehicle') {
      await Promise.all([
        recordVehicleHistoryUsage('driver_info', arrangementForm.driverName),
        recordVehicleHistoryUsage('vehicle_plate', arrangementForm.vehiclePlate),
      ]);
    }
    closeArrangementEditor();
  }
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
    remark: decodeTeamProfileRemark(detail.remark),
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
  await persistArrangementChanges('团队安排已保存', { reloadAfterSave: true });
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
                  :class="{ editable: item.editorType }"
                  @click="item.editorType && openTeamProfileEditor(item.editorType)"
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
              <button type="button" class="compact-action" @click="goProductEdit">
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
              <span>内部备注：{{ teamProfile.internalNote || '未填写' }}</span>
            </div>
            <Button type="link" size="small" @click="openTeamProfileEditor('internal_note')">
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
                      <template v-if="column === '操作'">
                        <Button
                          size="small"
                          type="link"
                          @click="openArrangementEditor(section.value, item)"
                        >
                          修改
                        </Button>
                        <Button danger size="small" type="link" @click="confirmDeleteArrangementItem(item)">
                          删除
                        </Button>
                      </template>
                      <template v-else>
                        {{ arrangementCellText(item, column) }}
                      </template>
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
        v-model:open="quickProfileEditorOpen"
        centered
        destroy-on-close
        :title="activeQuickProfileEditor.title"
        :width="460"
        :footer="null"
      >
        <Spin :spinning="teamProfileOptionsLoading || saving">
          <Form layout="vertical" class="quick-profile-form">
            <Form.Item :label="activeQuickProfileEditor.label" :required="activeQuickProfileEditor.required">
              <template v-if="activeQuickProfileEditor.inputType === 'select'">
                <Select
                  v-model:value="quickProfileEditorModel"
                  allow-clear
                  show-search
                  :options="quickProfileEditorOptions(activeQuickProfileEditor)"
                  :placeholder="activeQuickProfileEditor.placeholder"
                />
              </template>
              <Textarea
                v-else
                v-model:value="quickProfileEditorModel"
                :auto-size="{ minRows: 4, maxRows: 6 }"
                :maxlength="100"
                :placeholder="activeQuickProfileEditor.placeholder"
                show-count
              />
            </Form.Item>
            <div class="traffic-modal-footer">
              <Button @click="quickProfileEditorOpen = false">取消</Button>
              <Button type="primary" :loading="saving" @click="saveTeamProfileEditor">
                {{ activeQuickProfileEditor.buttonText }}
              </Button>
            </div>
          </Form>
        </Spin>
      </Modal>

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
                          v-model:value="arrangementForm.resourceName"
                          allow-clear
                          show-search
                          :options="resourceOptions"
                          @change="applySelectedResource"
                        />
                      </Form.Item>
                      <Form.Item label="添加">
                        <Button @click="openHotelCreatePage">添加酒店</Button>
                      </Form.Item>
                      <Form.Item label="已确认">
                        <Checkbox v-model:checked="arrangementForm.confirmed">已确认</Checkbox>
                      </Form.Item>
                      <Form.Item label="确认号">
                        <Input v-model:value="arrangementForm.confirmationNo" placeholder="确认号" />
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
                        <Select v-model:value="arrangementForm.mealType" :options="breakfastOptions" />
                      </Form.Item>
                      <Form.Item label="基金">
                        <Select v-model:value="arrangementForm.fundIncluded" :options="fundOptions" />
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
                        <Select v-model:value="arrangementForm.scheduleStartDay" :options="scheduleDayOptions" @change="syncHotelNightsCount" />
                      </Form.Item>
                      <Form.Item label="退房">
                        <Select v-model:value="arrangementForm.scheduleEndDay" :options="scheduleDayOptions" @change="syncHotelNightsCount" />
                      </Form.Item>
                      <Form.Item label="共几晚">
                        <InputNumber v-model:value="arrangementForm.daysCount" disabled :min="0" :precision="0" />
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
                        <Button @click="openSupplierCreatePage">添加供应商</Button>
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
                        v-for="(line, index) in arrangementForm.priceLines"
                        :key="index"
                        class="traffic-price-line"
                      >
                        <Select
                          v-model:value="line.projectName"
                          show-search
                          :options="projectOptions"
                          @change="(value) => applySelectedPriceProject(index, value)"
                        />
                        <InputNumber
                          v-model:value="line.unitPrice"
                          addon-before="¥"
                          :min="0"
                          :precision="2"
                          @change="syncPrimaryPriceFields"
                        />
                        <div class="traffic-inline-number">
                          <span>*数量:</span>
                          <InputNumber
                            v-model:value="line.quantity"
                            :min="0"
                            :precision="0"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <div class="traffic-price-remark">
                          <span>备注:</span>
                          <Textarea
                            v-model:value="line.remark"
                            :auto-size="{ minRows: 1, maxRows: 2 }"
                            placeholder="价格备注"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <button
                          class="traffic-remove-line-button"
                          :class="{ disabled: arrangementForm.priceLines.length <= 1 }"
                          :disabled="arrangementForm.priceLines.length <= 1"
                          title="删除价格信息"
                          type="button"
                          @click="removeArrangementPriceLine(index)"
                        >
                          <IconifyIcon icon="lucide:minus" />
                        </button>
                        <button
                          v-if="index === arrangementForm.priceLines.length - 1"
                          class="traffic-add-line-button"
                          :class="{ disabled: showMultiOrderAveragePriceNotice }"
                          :disabled="showMultiOrderAveragePriceNotice"
                          :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'"
                          type="button"
                          @click="addArrangementPriceLine"
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

                  <Form.Item label="责任房调">
                    <Select
                      v-model:value="arrangementForm.responsibleEmployeeName"
                      allow-clear
                      show-search
                      :options="employeeOptions"
                      @change="applySelectedResponsible"
                    />
                  </Form.Item>

                  <Form.Item label="订单信息">
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
                </div>
              </template>

              <template v-else-if="activeEditorType === 'vehicle'">
                <div class="vehicle-old-system-layout">
                  <div class="traffic-field-group">
                    <div class="traffic-group-title">
                      <IconifyIcon icon="lucide:car" />
                      <span>座位数</span>
                    </div>
                    <div class="traffic-form-row one-column">
                      <Form.Item label="座位数" required>
                        <Select
                          v-model:value="arrangementForm.vehicleType"
                          allow-clear
                          show-search
                          :options="vehicleQuoteRuleOptions"
                          placeholder="请选择座位数规则"
                        />
                      </Form.Item>
                    </div>
                  </div>

                  <div class="traffic-field-group">
                    <div class="traffic-group-title">
                      <IconifyIcon icon="lucide:route" />
                      <span>用车时间</span>
                    </div>
                    <div class="traffic-form-row vehicle-time-row">
                      <Form.Item label="开始" required>
                        <Select v-model:value="arrangementForm.scheduleStartDay" :options="scheduleDayOptions" @change="syncVehicleDaysCount" />
                      </Form.Item>
                      <Form.Item label="结束">
                        <Select v-model:value="arrangementForm.scheduleEndDay" :options="scheduleDayOptions" @change="syncVehicleDaysCount" />
                      </Form.Item>
                      <Form.Item label="共几天">
                        <InputNumber v-model:value="arrangementForm.daysCount" disabled :min="0" :precision="0" />
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
                        <strong>{{ vehicleDistanceText(arrangementForm.vehicleQuoteSnapshot?.syncedDistanceMeters) }}</strong>
                      </div>
                      <div>
                        <span>预计车程</span>
                        <strong>{{ routeDurationText(arrangementForm.vehicleQuoteSnapshot?.syncedDurationSeconds) }}</strong>
                      </div>
                      <div class="vehicle-roadbook-actions">
                        <Space>
                          <Button @click="syncVehicleRoadbookDistance">同步路书公里</Button>
                          <Button type="primary" ghost @click="openProductRoadbookEditor">编辑路书地图</Button>
                        </Space>
                      </div>
                    </div>
                    <div class="vehicle-route-summary">
                      {{ arrangementForm.vehicleQuoteSnapshot?.routeSummary || '先在产品行程里维护每天路书，再同步到用车报价。' }}
                    </div>
                  </div>

                  <div class="traffic-field-group vehicle-quote-panel">
                    <div class="traffic-group-title">
                      <IconifyIcon icon="lucide:calculator" />
                      <span>报价测算</span>
                    </div>
                    <div class="traffic-form-row three-columns">
                      <Form.Item label="规则座位数">
                        <Input :value="arrangementForm.vehicleType || '未选择'" disabled />
                      </Form.Item>
                      <Form.Item label="测算参考价">
                        <InputNumber
                          v-model:value="arrangementForm.vehicleQuoteSnapshot.confirmedAmount"
                          addon-before="¥"
                          :min="0"
                          :precision="2"
                        />
                      </Form.Item>
                      <Form.Item label="操作">
                        <Space>
                          <Button :loading="vehicleQuoteCalculating" @click="calculateVehicleReferencePrice">测算报价</Button>
                          <Button type="primary" ghost @click="applyVehicleQuoteToPriceInfo()">应用到价格信息</Button>
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
                          v-model:value="arrangementForm.driverName"
                          allow-clear
                          :options="driverHistoryOptions"
                          placeholder="手动输入司机姓名/电话"
                          @search="(value) => loadVehicleHistoryOptions('driver_info', value)"
                        />
                      </Form.Item>
                      <Form.Item label="车牌号">
                        <AutoComplete
                          v-model:value="arrangementForm.vehiclePlate"
                          allow-clear
                          :options="vehiclePlateHistoryOptions"
                          placeholder="手动输入车牌号"
                          @search="(value) => loadVehicleHistoryOptions('vehicle_plate', value)"
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
                        <Button @click="openSupplierCreatePage">添加供应商</Button>
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
                        v-for="(line, index) in arrangementForm.priceLines"
                        :key="index"
                        class="traffic-price-line"
                      >
                        <Select
                          v-model:value="line.projectName"
                          show-search
                          :options="projectOptions"
                          @change="(value) => applySelectedPriceProject(index, value)"
                        />
                        <InputNumber
                          v-model:value="line.unitPrice"
                          addon-before="¥"
                          :min="0"
                          :precision="2"
                          @change="syncPrimaryPriceFields"
                        />
                        <div class="traffic-inline-number">
                          <span>*数量:</span>
                          <InputNumber
                            v-model:value="line.quantity"
                            :min="0"
                            :precision="0"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <div class="traffic-price-remark">
                          <span>备注:</span>
                          <Textarea
                            v-model:value="line.remark"
                            :auto-size="{ minRows: 1, maxRows: 2 }"
                            placeholder="价格备注"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <button
                          class="traffic-remove-line-button"
                          :class="{ disabled: arrangementForm.priceLines.length <= 1 }"
                          :disabled="arrangementForm.priceLines.length <= 1"
                          title="删除价格信息"
                          type="button"
                          @click="removeArrangementPriceLine(index)"
                        >
                          <IconifyIcon icon="lucide:minus" />
                        </button>
                        <button
                          v-if="index === arrangementForm.priceLines.length - 1"
                          class="traffic-add-line-button"
                          :class="{ disabled: showMultiOrderAveragePriceNotice }"
                          :disabled="showMultiOrderAveragePriceNotice"
                          :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'"
                          type="button"
                          @click="addArrangementPriceLine"
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

                  <Form.Item label="责任车调">
                    <Select
                      v-model:value="arrangementForm.responsibleEmployeeName"
                      allow-clear
                      show-search
                      :options="employeeOptions"
                      @change="applySelectedResponsible"
                    />
                  </Form.Item>

                  <Form.Item label="订单信息">
                    <Select v-model:value="arrangementForm.orderScope" :options="trafficOrderOptions" />
                    <div class="traffic-field-tip">将此项成本归于关联订单</div>
                  </Form.Item>

                  <Form.Item label="备注信息">
                    <Textarea
                      v-model:value="arrangementForm.remark"
                      :auto-size="{ minRows: 2, maxRows: 4 }"
                      placeholder="备注信息"
                    />
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
                        <Select
                          v-model:value="arrangementForm.resourceName"
                          allow-clear
                          show-search
                          :options="resourceOptions"
                          @change="applySelectedResource"
                        />
                      </Form.Item>
                      <Form.Item label="添加">
                        <Button @click="openSupplierCreatePage">添加供应商</Button>
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
                        <Select v-model:value="arrangementForm.scheduleStartDay" :options="scheduleDayOptions" />
                      </Form.Item>
                      <Form.Item label="时间">
                        <Select v-model:value="arrangementForm.mealType" :options="mealTypeOptions" />
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
                        <Button @click="openSupplierCreatePage">添加供应商</Button>
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
                        v-for="(line, index) in arrangementForm.priceLines"
                        :key="index"
                        class="traffic-price-line"
                      >
                        <Select
                          v-model:value="line.projectName"
                          show-search
                          :options="priceProjectOptionsForType(activeEditorType)"
                          @change="(value) => applySelectedPriceProject(index, value)"
                        />
                        <InputNumber
                          v-model:value="line.unitPrice"
                          addon-before="¥"
                          :min="0"
                          :precision="2"
                          @change="syncPrimaryPriceFields"
                        />
                        <div class="traffic-inline-number">
                          <span>*数量:</span>
                          <InputNumber
                            v-model:value="line.quantity"
                            :min="0"
                            :precision="0"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <div class="traffic-price-remark">
                          <span>备注:</span>
                          <Textarea
                            v-model:value="line.remark"
                            :auto-size="{ minRows: 1, maxRows: 2 }"
                            placeholder="价格备注"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <button
                          class="traffic-remove-line-button"
                          :class="{ disabled: arrangementForm.priceLines.length <= 1 }"
                          :disabled="arrangementForm.priceLines.length <= 1"
                          title="删除价格信息"
                          type="button"
                          @click="removeArrangementPriceLine(index)"
                        >
                          <IconifyIcon icon="lucide:minus" />
                        </button>
                        <button
                          v-if="index === arrangementForm.priceLines.length - 1"
                          class="traffic-add-line-button"
                          :class="{ disabled: showMultiOrderAveragePriceNotice }"
                          :disabled="showMultiOrderAveragePriceNotice"
                          :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'"
                          type="button"
                          @click="addArrangementPriceLine"
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

                  <Form.Item label="订单信息">
                    <Select v-model:value="arrangementForm.orderScope" :options="trafficOrderOptions" />
                    <div class="traffic-field-tip">将此项成本归于关联订单</div>
                  </Form.Item>

                  <Form.Item label="备注信息">
                    <Textarea
                      v-model:value="arrangementForm.remark"
                      :auto-size="{ minRows: 2, maxRows: 4 }"
                      placeholder="备注信息"
                    />
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
                        <Select v-model:value="arrangementForm.scheduleStartDay" :options="scheduleDayOptions" />
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
                        <Button @click="openSupplierCreatePage">添加供应商</Button>
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
                        v-for="(line, index) in arrangementForm.priceLines"
                        :key="index"
                        class="traffic-price-line"
                      >
                        <Select
                          v-model:value="line.projectName"
                          show-search
                          :options="priceProjectOptionsForType(activeEditorType)"
                          @change="(value) => applySelectedPriceProject(index, value)"
                        />
                        <InputNumber
                          v-model:value="line.unitPrice"
                          addon-before="¥"
                          :min="0"
                          :precision="2"
                          @change="syncPrimaryPriceFields"
                        />
                        <div class="traffic-inline-number">
                          <span>*数量:</span>
                          <InputNumber
                            v-model:value="line.quantity"
                            :min="0"
                            :precision="0"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <div class="traffic-price-remark">
                          <span>备注:</span>
                          <Textarea
                            v-model:value="line.remark"
                            :auto-size="{ minRows: 1, maxRows: 2 }"
                            placeholder="价格备注"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <button
                          class="traffic-remove-line-button"
                          :class="{ disabled: arrangementForm.priceLines.length <= 1 }"
                          :disabled="arrangementForm.priceLines.length <= 1"
                          title="删除价格信息"
                          type="button"
                          @click="removeArrangementPriceLine(index)"
                        >
                          <IconifyIcon icon="lucide:minus" />
                        </button>
                        <button
                          v-if="index === arrangementForm.priceLines.length - 1"
                          class="traffic-add-line-button"
                          :class="{ disabled: showMultiOrderAveragePriceNotice }"
                          :disabled="showMultiOrderAveragePriceNotice"
                          :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'"
                          type="button"
                          @click="addArrangementPriceLine"
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

                  <Form.Item label="订单信息">
                    <Select v-model:value="arrangementForm.orderScope" :options="trafficOrderOptions" />
                    <div class="traffic-field-tip">将此项成本归于关联订单</div>
                  </Form.Item>

                  <Form.Item label="备注信息">
                    <Textarea
                      v-model:value="arrangementForm.remark"
                      :auto-size="{ minRows: 2, maxRows: 4 }"
                      placeholder="备注信息"
                    />
                  </Form.Item>
                </div>
              </template>

              <template v-else-if="activeEditorType === 'optional'">
                <div class="optional-old-system-layout">
                  <div class="traffic-field-group">
                    <div class="traffic-group-title">
                      <IconifyIcon icon="lucide:landmark" />
                      <span>景区名称</span>
                    </div>
                    <div class="traffic-form-row two-columns">
                      <Form.Item label="景区名称">
                        <Select
                          v-model:value="arrangementForm.resourceName"
                          allow-clear
                          show-search
                          :options="resourceOptions"
                          @change="applySelectedResource"
                        />
                      </Form.Item>
                      <Form.Item label="游玩日期" required>
                        <Select v-model:value="arrangementForm.scheduleStartDay" :options="scheduleDayOptions" />
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
                        <Button @click="openSupplierCreatePage">添加供应商</Button>
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
                      <Button size="small" @click="openScenicTemplateConfigPage">配置模板</Button>
                    </div>
                  </div>

                  <div class="traffic-field-group">
                    <div class="traffic-group-title">
                      <IconifyIcon icon="lucide:receipt-text" />
                      <span>价格信息</span>
                    </div>
                    <div class="traffic-form-row three-columns">
                      <Form.Item label="销售价">
                        <InputNumber v-model:value="arrangementForm.saleAmount" addon-before="¥" :min="0" :precision="2" />
                      </Form.Item>
                      <Form.Item label="成本价">
                        <InputNumber v-model:value="arrangementForm.costAmount" addon-before="¥" :min="0" :precision="2" />
                      </Form.Item>
                      <div class="old-system-combined-field">
                        <div class="old-system-combined-title">导游提成</div>
                        <div class="old-system-combined-controls optional-commission-controls">
                          <InputNumber v-model:value="arrangementForm.guideCommissionAmount" :min="0" :precision="2" />
                          <span>元/人</span>
                          <span>或</span>
                          <InputNumber v-model:value="arrangementForm.guideCommissionRate" :min="0" :precision="2" />
                          <span>%</span>
                        </div>
                      </div>
                    </div>
                    <div class="optional-fee-summary">
                      <div class="optional-fee-summary-title">费用合计</div>
                      <div
                        v-for="(line, index) in arrangementForm.priceLines"
                        :key="`optional-${index}`"
                        class="optional-fee-summary-line optional-fee-summary-row"
                      >
                        <label>人数：</label>
                        <InputNumber v-model:value="line.quantity" :min="0" :precision="0" @change="syncOptionalLineToForm" />
                        <label>收入：</label>
                        <InputNumber v-model:value="line.salePrice" :min="0" :precision="2" @change="syncOptionalLineToForm" />
                        <label>成本：</label>
                        <InputNumber v-model:value="line.costPrice" :min="0" :precision="2" @change="syncOptionalLineToForm" />
                        <label>现结：</label>
                        <InputNumber
                          v-model:value="line.cashAmount"
                          :min="0"
                          :precision="2"
                          @change="syncOptionalLineToForm"
                        />
                        <label>挂账：</label>
                        <InputNumber :value="optionalLineCreditAmount(line)" disabled :precision="2" />
                        <label>提成：</label>
                        <InputNumber v-model:value="line.guideCommissionAmount" :min="0" :precision="2" @change="syncOptionalLineToForm" />
                        <button
                          class="traffic-remove-line-button"
                          :class="{ disabled: arrangementForm.priceLines.length <= 1 }"
                          :disabled="arrangementForm.priceLines.length <= 1"
                          title="删除费用合计"
                          type="button"
                          @click="removeOptionalSummaryLine(index)"
                        >
                          <IconifyIcon icon="lucide:minus" />
                        </button>
                        <button
                          v-if="index === arrangementForm.priceLines.length - 1"
                          class="optional-add-summary-button"
                          title="添加费用合计"
                          type="button"
                          @click="addOptionalSummaryLine"
                        >
                          <IconifyIcon icon="lucide:plus" />
                        </button>
                      </div>
                    </div>
                  </div>

                  <Form.Item label="订单信息">
                    <Select v-model:value="arrangementForm.orderScope" :options="trafficOrderOptions" />
                    <div class="traffic-field-tip">将此项成本归于关联订单</div>
                  </Form.Item>

                  <Form.Item label="备注信息">
                    <Textarea
                      v-model:value="arrangementForm.remark"
                      :auto-size="{ minRows: 2, maxRows: 4 }"
                      placeholder="备注信息"
                    />
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
                        <Select
                          v-model:value="arrangementForm.resourceName"
                          allow-clear
                          show-search
                          :options="resourceOptions"
                          @change="applySelectedResource"
                        />
                      </Form.Item>
                      <Form.Item label="购物日期" required>
                        <Select v-model:value="arrangementForm.scheduleStartDay" :options="scheduleDayOptions" />
                      </Form.Item>
                      <Form.Item label="添加">
                        <Button @click="openResourceCreatePage">添加购物店</Button>
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
                        <Select
                          v-model:value="arrangementForm.supplierName"
                          allow-clear
                          show-search
                          :options="supplierOptions"
                          @change="applySelectedSupplier"
                        />
                      </Form.Item>
                      <Form.Item label="添加">
                        <Button @click="openSupplierCreatePage">添加供应商</Button>
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
                        <InputNumber v-model:value="arrangementForm.peopleCount" :min="0" :precision="0" />
                      </Form.Item>
                      <Form.Item label="人头费">
                        <InputNumber v-model:value="arrangementForm.headFeeAmount" addon-before="¥" :min="0" :precision="2" />
                      </Form.Item>
                      <Form.Item label="公司返佣">
                        <InputNumber v-model:value="arrangementForm.companyRebateAmount" addon-before="¥" :min="0" :precision="2" />
                      </Form.Item>
                      <Form.Item label="导游提成">
                        <InputNumber v-model:value="arrangementForm.guideCommissionAmount" addon-before="¥" :min="0" :precision="2" />
                      </Form.Item>
                    </div>
                  </div>

                  <div class="traffic-field-group">
                    <div class="traffic-group-title">
                      <IconifyIcon icon="lucide:receipt-text" />
                      <span>消费详情</span>
                    </div>
                    <div
                      v-for="(line, index) in arrangementForm.priceLines"
                      :key="`shopping-${index}`"
                      class="shopping-consumption-line"
                    >
                      <div class="shopping-consumption-main-row">
                        <Form.Item label="品类">
                          <Select
                            v-model:value="line.projectName"
                            show-search
                            :options="shoppingCategoryOptions"
                            @change="(value) => applyShoppingPriceProject(index, value)"
                          />
                        </Form.Item>
                        <Form.Item label="消费金额">
                          <InputNumber v-model:value="line.consumptionAmount" addon-before="¥" :min="0" :precision="2" @change="syncShoppingLineToForm" />
                        </Form.Item>
                        <div class="shopping-formula-field">
                          <div class="shopping-formula-label">公司返佣</div>
                          <div class="shopping-formula-controls">
                            <InputNumber v-model:value="line.companyRebateRate" :min="0" :precision="2" @change="syncShoppingLineToForm" />
                            <span>% =</span>
                            <InputNumber v-model:value="line.companyRebateAmount" addon-before="¥" :min="0" :precision="2" @change="syncShoppingLineToForm" />
                          </div>
                        </div>
                        <div class="shopping-formula-field">
                          <div class="shopping-formula-label">导游提成</div>
                          <div class="shopping-formula-controls">
                            <InputNumber v-model:value="line.guideCommissionRate" :min="0" :precision="2" @change="syncShoppingLineToForm" />
                            <span>%销售额 =</span>
                            <InputNumber v-model:value="line.guideCommissionAmount" addon-before="¥" :min="0" :precision="2" @change="syncShoppingLineToForm" />
                          </div>
                        </div>
                      </div>
                      <div class="shopping-consumption-extra-row">
                        <Form.Item label="现结">
                          <InputNumber
                            v-model:value="line.cashAmount"
                            addon-before="¥"
                            :min="0"
                            :precision="2"
                            @change="syncShoppingLineToForm"
                          />
                        </Form.Item>
                        <Form.Item label="备注">
                          <Input v-model:value="line.remark" placeholder="消费备注" @change="syncShoppingLineToForm" />
                        </Form.Item>
                        <button
                          class="traffic-remove-line-button"
                          :class="{ disabled: arrangementForm.priceLines.length <= 1 }"
                          :disabled="arrangementForm.priceLines.length <= 1"
                          title="删除消费详情"
                          type="button"
                          @click="removeShoppingConsumptionLine(index)"
                        >
                          <IconifyIcon icon="lucide:minus" />
                        </button>
                        <button
                          v-if="index === arrangementForm.priceLines.length - 1"
                          class="shopping-add-detail-button"
                          title="添加消费详情"
                          type="button"
                          @click="addShoppingConsumptionLine"
                        >
                          <IconifyIcon icon="lucide:plus" />
                        </button>
                      </div>
                    </div>
                  </div>

                  <Form.Item label="备注信息">
                    <Textarea
                      v-model:value="arrangementForm.remark"
                      :auto-size="{ minRows: 2, maxRows: 4 }"
                      placeholder="备注信息"
                    />
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
                        <Select v-model:value="arrangementForm.scheduleStartDay" :options="scheduleDayOptions" @change="syncGroundAgentDaysCount" />
                      </Form.Item>
                      <Form.Item label="结束">
                        <Select v-model:value="arrangementForm.scheduleEndDay" :options="scheduleDayOptions" @change="syncGroundAgentDaysCount" />
                      </Form.Item>
                      <Form.Item label="共几天">
                        <InputNumber v-model:value="arrangementForm.daysCount" disabled :min="0" :precision="0" />
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
                        <Button @click="openSupplierCreatePage">添加供应商</Button>
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
                        v-for="(line, index) in arrangementForm.priceLines"
                        :key="index"
                        class="traffic-price-line"
                      >
                        <Select
                          v-model:value="line.projectName"
                          show-search
                          :options="priceProjectOptionsForType(activeEditorType)"
                          @change="(value) => applySelectedPriceProject(index, value)"
                        />
                        <InputNumber
                          v-model:value="line.unitPrice"
                          addon-before="¥"
                          :min="0"
                          :precision="2"
                          @change="syncPrimaryPriceFields"
                        />
                        <div class="traffic-inline-number">
                          <span>*数量:</span>
                          <InputNumber
                            v-model:value="line.quantity"
                            :min="0"
                            :precision="0"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <div class="traffic-price-remark">
                          <span>备注:</span>
                          <Textarea
                            v-model:value="line.remark"
                            :auto-size="{ minRows: 1, maxRows: 2 }"
                            placeholder="价格备注"
                            @change="syncPrimaryPriceFields"
                          />
                        </div>
                        <button
                          class="traffic-remove-line-button"
                          :class="{ disabled: arrangementForm.priceLines.length <= 1 }"
                          :disabled="arrangementForm.priceLines.length <= 1"
                          title="删除价格信息"
                          type="button"
                          @click="removeArrangementPriceLine(index)"
                        >
                          <IconifyIcon icon="lucide:minus" />
                        </button>
                        <button
                          v-if="index === arrangementForm.priceLines.length - 1"
                          class="traffic-add-line-button"
                          :class="{ disabled: showMultiOrderAveragePriceNotice }"
                          :disabled="showMultiOrderAveragePriceNotice"
                          :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'"
                          type="button"
                          @click="addArrangementPriceLine"
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

                  <Form.Item label="订单信息">
                    <Select v-model:value="arrangementForm.orderScope" :options="trafficOrderOptions" />
                    <div class="traffic-field-tip">将此项成本归于关联订单</div>
                  </Form.Item>

                  <Form.Item label="备注信息">
                    <Textarea
                      v-model:value="arrangementForm.remark"
                      :auto-size="{ minRows: 2, maxRows: 4 }"
                      placeholder="备注信息"
                    />
                  </Form.Item>
                </div>
              </template>

              <template v-else>
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
                    <Cascader
                      v-model:value="departureRegionPath"
                      allow-clear
                      change-on-select
                      :options="regionOptions"
                      placeholder="请选择出发地"
                      show-search
                    />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showArrivalPlace" label="抵达地" required>
                    <Cascader
                      v-model:value="arrivalRegionPath"
                      allow-clear
                      change-on-select
                      :options="regionOptions"
                      placeholder="请选择抵达地"
                      show-search
                    />
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
                      @change="applySelectedResource"
                    />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showConfirmed" label="已确认">
                    <Checkbox v-model:checked="arrangementForm.confirmed">已确认</Checkbox>
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showConfirmed" label="确认号">
                    <Input v-model:value="arrangementForm.confirmationNo" placeholder="确认号" />
                  </Form.Item>
                  <Form.Item v-if="activeEditorConfig.showVehicleType" label="座位数">
                    <Select
                      v-model:value="arrangementForm.vehicleType"
                      allow-clear
                      show-search
                      :options="vehicleQuoteRuleOptions"
                      placeholder="请选择座位数规则"
                    />
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
                    <Button @click="openSupplierCreatePage">添加供应商</Button>
                  </Form.Item>
                </div>
                <div
                  v-if="activeEditorType === 'scenic'"
                  class="scenic-template-status"
                >
                  <div>
                    <span>游客名单模板：</span>
                    <Tag v-if="scenicTicketTemplateLoading" color="blue">读取中</Tag>
                    <Tag v-else-if="scenicTicketTemplate" color="green">已配置</Tag>
                    <Tag v-else color="orange">未配置</Tag>
                    <strong v-if="scenicTicketTemplate">{{ scenicTicketTemplate.templateName }}</strong>
                  </div>
                  <Button size="small" @click="openScenicTemplateConfigPage">配置模板</Button>
                </div>
              </div>

              <div class="traffic-field-group">
                <div class="traffic-group-title">
                  <IconifyIcon icon="lucide:receipt-text" />
                  <span>价格信息</span>
                </div>
                <div class="traffic-price-list">
                  <div
                    v-for="(line, index) in arrangementForm.priceLines"
                    :key="index"
                    class="traffic-price-line"
                  >
                    <Select
                      v-model:value="line.projectName"
                      show-search
                      :options="projectOptions"
                      @change="(value) => applySelectedPriceProject(index, value)"
                    />
                    <InputNumber
                      v-model:value="line.unitPrice"
                      addon-before="¥"
                      :min="0"
                      :precision="2"
                      @change="syncPrimaryPriceFields"
                    />
                    <div class="traffic-inline-number">
                      <span>*数量:</span>
                      <InputNumber
                        v-model:value="line.quantity"
                        :min="0"
                        :precision="0"
                        @change="syncPrimaryPriceFields"
                      />
                    </div>
                    <div class="traffic-price-remark">
                      <span>备注:</span>
                      <Textarea
                        v-model:value="line.remark"
                        :auto-size="{ minRows: 1, maxRows: 2 }"
                        placeholder="价格备注"
                        @change="syncPrimaryPriceFields"
                      />
                    </div>
                    <button
                      class="traffic-remove-line-button"
                      :class="{ disabled: arrangementForm.priceLines.length <= 1 }"
                      :disabled="arrangementForm.priceLines.length <= 1"
                      title="删除价格信息"
                      type="button"
                      @click="removeArrangementPriceLine(index)"
                    >
                      <IconifyIcon icon="lucide:minus" />
                    </button>
                    <button
                      v-if="index === arrangementForm.priceLines.length - 1"
                      class="traffic-add-line-button"
                      :class="{ disabled: showMultiOrderAveragePriceNotice }"
                      :disabled="showMultiOrderAveragePriceNotice"
                      :title="showMultiOrderAveragePriceNotice ? '多订单均摊成本时只能保留一条价格信息' : '添加价格信息'"
                      type="button"
                      @click="addArrangementPriceLine"
                    >
                      <IconifyIcon icon="lucide:plus" />
                    </button>
                  </div>
                </div>
                <div v-if="showMultiOrderAveragePriceNotice" class="traffic-price-lock-tip">
                  多订单均摊成本时，价格信息组成只能统一写成一条记录，点击 ⊕ 失效
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
              </template>
            </Form>

            <div class="traffic-modal-footer">
              <Button @click="closeArrangementEditor">取消</Button>
              <Checkbox v-if="activeEditorConfig.noGuideReport" v-model:checked="arrangementForm.noGuideReport" class="traffic-sync-checkbox">
                无需导游报账，同步更新导游报账和计调审核数据
              </Checkbox>
              <Button type="primary" :loading="saving" @click="saveArrangementEditor">
                {{ activeEditorType === 'optional' ? '提交保存' : (editingArrangementIndex >= 0 ? '保存修改' : '新增安排') }}
              </Button>
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

.team-badges :deep(.ant-tag.editable) {
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.team-badges :deep(.ant-tag.editable:hover) {
  color: #0958d9;
  border-color: #91caff;
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

.quick-profile-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.quick-profile-form :deep(.ant-form-item-label > label) {
  font-size: 13px;
  font-weight: 800;
  color: #334155;
}

.quick-profile-form :deep(.ant-select-selector),
.quick-profile-form :deep(.ant-input) {
  border-color: #dbe4f0;
  border-radius: 5px;
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

.traffic-form-row.four-columns {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.traffic-form-row.supplier-row {
  grid-template-columns: minmax(0, 1fr) 70px;
  align-items: end;
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

.optional-add-summary-button {
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

.optional-add-summary-button svg {
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

.shopping-add-detail-button svg {
  width: 17px;
  height: 17px;
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
  grid-template-columns: 150px 150px 140px minmax(180px, 1fr) 32px 32px;
  gap: 10px;
  align-items: center;
}

.traffic-price-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
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
  .traffic-form-row.four-columns,
  .traffic-form-row.hotel-name-row,
  .traffic-form-row.vehicle-time-row,
  .traffic-form-row.supplier-row,
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
