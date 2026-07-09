<script lang="ts" setup>
import type { SalesProductApi } from '#/api/sales/product';
import type { RegionPath } from '#/utils/region';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Form,
  InputNumber,
  Modal,
  Select,
  Spin,
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

import ArrangementEditorModal from '../components/ArrangementEditorModal.vue';
import FormalTeamPageHeader from '../components/FormalTeamPageHeader.vue';
import { showTeamItineraryModal } from '../components/itinerary-modal';
import {
  arrangementEditorConfigs,
  createGroundAgentPackagePriceLine,
  createDefaultArrangementEditorForm,
  createDefaultArrangementPriceLine,
  defaultProjectName,
  ensureSelectOption,
  priceProjectOptionsForType as sharedPriceProjectOptionsForType,
  resolveArrangementResourceName,
  resolveGroundAgentPackageAmount,
  resolveSupplierOptionsForResource,
  routeDurationText,
  scheduleExclusiveNightsCount,
  scheduleInclusiveDaysCount,
  parseScheduleDayNo,
  vehicleDistanceText,
  type ArrangementEditorForm,
  type AutoCompleteOption,
  type SelectOption,
  type SelectOptionWithId,
} from '../components/arrangement-editor-model';

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

import '../team-arrangement-layout.css';

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
  key: string;
  label: string;
  value: string;
};

type ArrangementSection = ArrangementCategoryShortcut & {
  columns: string[];
  documentAction?: string;
};

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
  optionalMarkupRate?: number;
  operatorName?: string;
  perCapitaPitAmount?: number;
  perCapitaShoppingAmount?: number;
  receptionStandard?: string;
  totalDistanceText?: string;
};

type QuickProfileEditorType = 'business_type' | 'department' | 'escort' | 'internal_note' | 'operator';
type TeamProfileTextField = 'businessType' | 'departmentName' | 'escortName' | 'internalNote' | 'operatorName';

type QuickProfileEditorConfig = {
  buttonText: string;
  field: TeamProfileTextField;
  inputType: 'select' | 'textarea';
  label: string;
  optionsType?: 'business_type' | 'department' | 'employee';
  placeholder: string;
  required?: boolean;
  title: string;
};

const DEFAULT_INTERNAL_REMARK_TEMPLATE = [
  '>导游要求：',
  '>控房要求：',
  '>控车要求：',
  '>用餐要求：',
  '>其它要求：',
].join('\n');

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
    columns: ['开始', '结束', '天数', '供应商', '备注', '成本合计', '现结', '挂账', '操作'],
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

const regionOptions = buildRegionOptions();
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
    placeholder: '填写导游、控房、控车、用餐和其它要求',
    title: '内部备注',
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
const arrangementCostSummaryItems = computed(() => {
  const cashTotal = arrangementCashTotal.value;
  const creditTotal = arrangementCreditTotal.value;
  const extraColumns = overviewSummary.value.extraColumns;
  return [
    { key: 'cost_total', label: '成本合计', tone: 'primary', value: cashTotal + creditTotal },
    { key: 'cash_total', label: '现结合计', tone: 'strong', value: cashTotal },
    { key: 'credit_total', label: '挂账合计', tone: 'strong', value: creditTotal },
    { key: 'reserve_fund', label: '备用金', tone: 'strong', value: extraColumns.reserveFund },
    { key: 'guide_service', label: '导服', value: extraColumns.guideService },
    { key: 'operation_fee', label: '操作费', value: extraColumns.operationFee },
    { key: 'self_pay_income', label: '自费收入', value: extraColumns.selfPayIncome },
  ];
});
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
  if (activeEditorType.value === 'ground_agent') {
    return Number(arrangementForm.costAmount || 0);
  }
  return editorTotalAmount.value;
});
const editorCreditAmount = computed(() => Math.max(
  activeEditorTotalAmount.value - Number(arrangementForm.cashAmount || 0) - Number(arrangementForm.prepaidAmount || 0),
  0,
));
const activeSection = computed(() => arrangementSections.find((item) => item.value === activeEditorType.value));
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
const quickProfileEditorTextMaxLength = computed(() => (quickProfileEditorType.value === 'internal_note' ? 500 : 100));
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
const formalHeaderBadges = computed(() => teamBadges.value.map((badge) => ({
  color: badge.color,
  editable: Boolean(badge.editorType),
  key: badge.label,
  label: badge.label,
  value: badge.value,
})));
const teamMetricItems = computed<TeamMetricItem[]>(() => [
  {
    key: 'travelDays',
    label: '旅游天数',
    value: `${formState.travelDays || 1} 天`,
  },
  {
    key: 'standard',
    label: '接待标准',
    value: teamProfile.receptionStandard || formState.receptionStandard || '未设置',
  },
  {
    key: 'distance',
    label: '总里程数',
    value: teamProfile.totalDistanceText || `${totalRoadbookDistanceKilometers.value}公里`,
  },
  {
    key: 'receivable',
    label: '应收/已收/余额',
    value: '0 | 0 | 0',
  },
  {
    key: 'orderStatus',
    label: '订单已确认/未处理/已取消',
    value: '0 | 0 | 0',
  },
  {
    key: 'paid',
    label: '已付',
    value: '--',
  },
  {
    key: 'profit',
    label: '预算利润',
    value: `${formatPlainMoney(arrangementTotal.value)}`,
  },
]);
const formalHeaderActions = computed(() => [
  { icon: 'lucide:clipboard-list', key: 'productEdit', label: '团队管理' },
  { icon: 'lucide:briefcase', key: 'itinerary', label: '查看行程' },
]);
const formalHeaderNoteMetrics = computed(() => [
  `人均坑位：${formatMoney(teamProfile.perCapitaPitAmount)}`,
  `自费加点率：${Number(teamProfile.optionalMarkupRate || 0)}%`,
  `人均购物：${formatMoney(teamProfile.perCapitaShoppingAmount)}`,
]);
const internalNoteDisplay = computed(() => teamProfile.internalNote || DEFAULT_INTERNAL_REMARK_TEMPLATE);

function formatMoney(value?: number) {
  return new Intl.NumberFormat('zh-CN', {
    currency: 'CNY',
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
    style: 'currency',
  }).format(Number(value || 0));
}

function formatCostCashMoney(value?: number) {
  return formatMoney(value);
}

function formatCostDetailMoney(value?: number) {
  return formatMoney(value);
}

function costAmountClass(value?: number, extraClass?: string) {
  const classes = [Number(value || 0) === 0 ? 'cost-amount-zero' : 'cost-amount-nonzero'];
  if (extraClass) {
    classes.push(extraClass);
  }
  return classes;
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
    optionalMarkupRate: undefined,
    operatorName: undefined,
    perCapitaPitAmount: undefined,
    perCapitaShoppingAmount: undefined,
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
  if (editorType === 'internal_note') {
    teamProfile.internalNote ||= DEFAULT_INTERNAL_REMARK_TEMPLATE;
  }
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

function resetArrangementForm(type: SalesProductApi.ArrangementType) {
  Object.assign(arrangementForm, createDefaultArrangementEditorForm(type));
  departureRegionPath.value = [];
  arrivalRegionPath.value = [];
}

/** 把表格中已有的安排记录回填到弹窗表单，用于修改已保存的产品模板安排。 */
function hydrateArrangementFormFromItem(item: SalesProductApi.ArrangementItem) {
  const isGroundAgent = item.arrangementType === 'ground_agent';
  const groundAgentPackageAmount = isGroundAgent
    ? resolveGroundAgentPackageAmount({
      costAmount: item.costAmount,
      priceLines: item.priceLines,
      totalAmount: item.totalAmount,
    })
    : 0;
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
    costAmount: isGroundAgent ? groundAgentPackageAmount : Number(item.costAmount || 0),
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
    priceLines: isGroundAgent
      ? [createGroundAgentPackagePriceLine(groundAgentPackageAmount, item.priceLines?.[0]?.remark)]
      : (item.priceLines?.length
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
        : [createDefaultArrangementPriceLine(item.projectName || defaultProjectName(item.arrangementType))]),
    projectName: item.projectName,
    quantity: Number(item.quantity || 0),
    remark: manualRemarkText(item.remark),
    resourceName: resolveArrangementResourceName(item.resourceName, item.itemName),
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

function syncVehiclePriceProject(value?: unknown) {
  const selectedValue = normalizeSelectValue(value) || arrangementForm.vehicleType;
  if (!selectedValue) return;
  const firstLine = arrangementForm.priceLines[0] || createDefaultArrangementPriceLine(selectedValue);
  if (!arrangementForm.priceLines.length) {
    arrangementForm.priceLines.push(firstLine);
  }
  const project = priceProjectOptionsForType('vehicle').find((item) => item.value === selectedValue);
  firstLine.projectId = project?.id;
  firstLine.projectName = selectedValue;
  syncPrimaryPriceFields();
}

function priceProjectOptionsForType(type: SalesProductApi.ArrangementType) {
  return sharedPriceProjectOptionsForType(type, projectOptions.value);
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
  const selectedValue = arrangementForm.vehicleType || '车费';
  const firstLine = arrangementForm.priceLines[0] || createDefaultArrangementPriceLine(selectedValue);
  if (!arrangementForm.priceLines.length) {
    arrangementForm.priceLines.push(firstLine);
  }
  const project = priceProjectOptionsForType('vehicle').find((item) => item.value === selectedValue);
  firstLine.projectId = project?.id;
  firstLine.projectName = selectedValue;
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

function showItineraryModal() {
  showTeamItineraryModal({
    fallbackDescription: formState.productDescription,
    itineraryDays: formState.itineraryDays,
  });
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
    const guideCommissionRate = Number(line.guideCommissionRate || 0);
    const guideCommissionFixedAmount = Number(line.guideCommissionAmount || 0);
    const guideCommissionUnitAmount = guideCommissionFixedAmount > 0
      ? guideCommissionFixedAmount
      : Math.max(salePrice - costPrice, 0) * guideCommissionRate / 100;
    peopleCount += quantity;
    saleAmount += quantity * salePrice;
    costAmount += quantity * costPrice;
    cashAmount += Number(line.cashAmount || 0);
    guideCommissionAmount += quantity * guideCommissionUnitAmount;
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
    syncVehiclePriceProject(arrangementForm.vehicleType);
  }
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
    resourceOptions.value = ensureSelectOption(
      resources,
      arrangementForm.resourceName,
    );
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

  const resolved = resolveSupplierOptionsForResource({
    currentSupplierId: arrangementForm.supplierId,
    currentSupplierName: arrangementForm.supplierName,
    nextOptions: resourceRelationSupplierOptions(selectedResourceName),
  });
  supplierOptions.value = resolved.options;
  arrangementForm.supplierId = resolved.selectedSupplierId;
  arrangementForm.supplierName = resolved.selectedSupplierName;
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
  if (type === 'ground_agent' && Number(arrangementForm.costAmount || 0) <= 0) {
    message.warning('请填写地接结算价');
    return;
  }

  normalizeArrangementPriceLines();
  const priceLines = type === 'ground_agent'
    ? [createGroundAgentPackagePriceLine(Number(arrangementForm.costAmount || 0), arrangementForm.priceRemark)]
    : arrangementForm.priceLines.map((line, index) => {
      const quantity = Number(line.quantity || 0);
      const unitPrice = Number(line.unitPrice || 0);
      const amount = type === 'optional'
        ? Number(line.costPrice || 0) * quantity
        : unitPrice * quantity;
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
    costAmount: type === 'ground_agent'
      ? totalAmount
      : Number(arrangementForm.costAmount || totalAmount),
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

function handleFormalHeaderAction(action: { key: string }) {
  if (action.key === 'productEdit') {
    goProductEdit();
    return;
  }
  if (action.key === 'itinerary') {
    showItineraryModal();
  }
}

function handleFormalHeaderBadge(badge: { label: string }) {
  const target = teamBadges.value.find((item) => item.label === badge.label);
  if (target?.editorType) {
    openTeamProfileEditor(target.editorType);
  }
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
      <Card class="team-arrangement-card formal-team-arrangement-card">
        <FormalTeamPageHeader
          :actions="formalHeaderActions"
          :badges="formalHeaderBadges"
          :metrics="teamMetricItems"
          :note="internalNoteDisplay"
          :note-metrics="formalHeaderNoteMetrics"
          :stages="arrangementStages"
          :title="formState.productName || '未命名产品'"
          @action-click="handleFormalHeaderAction"
          @badge-click="handleFormalHeaderBadge"
          @note-edit="openTeamProfileEditor('internal_note')"
        />

        <div class="formal-cost-overview-title">
          <div>
            <IconifyIcon icon="lucide:table-properties" />
            <span>成本总览</span>
          </div>
          <small>现结 / 挂账 / 收入摘要</small>
        </div>
        <div class="cost-overview-summary" aria-label="成本总览重点金额">
          <div
            v-for="item in arrangementCostSummaryItems"
            :key="item.key"
            :class="['cost-summary-card', `cost-summary-card--${item.tone || 'normal'}`]"
          >
            <span class="cost-summary-label">{{ item.label }}</span>
            <strong
              class="cost-summary-amount"
              :class="costAmountClass(item.value, item.tone === 'primary' ? 'cost-summary-total' : undefined)"
            >
              {{ formatMoney(item.value) }}
            </strong>
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
              </tr>
              <tr>
                <template
                  v-for="item in arrangementOverviewColumns"
                  :key="`${item.value}-settlement`"
                >
                  <th>现结</th>
                  <th>挂账</th>
                </template>
              </tr>
            </thead>
            <tbody>
              <tr>
                <template
                  v-for="item in arrangementOverviewColumns"
                  :key="`${item.value}-amount`"
                >
                  <td :class="costAmountClass(arrangementSettlementTotal(item.value, 'cash'), 'cost-amount-cash')">
                    {{ formatCostCashMoney(arrangementSettlementTotal(item.value, 'cash')) }}
                  </td>
                  <td :class="costAmountClass(arrangementSettlementTotal(item.value, 'credit'), 'cost-amount-credit')">
                    {{ formatCostDetailMoney(arrangementSettlementTotal(item.value, 'credit')) }}
                  </td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="arrangement-tabs-block">
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
        :width="quickProfileEditorType === 'internal_note' ? 760 : 460"
        :footer="null"
      >
        <Spin :spinning="teamProfileOptionsLoading || saving">
          <Form layout="vertical" class="quick-profile-form">
            <template v-if="quickProfileEditorType === 'internal_note'">
              <Form.Item label="内部备注">
                <Textarea
                  v-model:value="teamProfile.internalNote"
                  class="inside-memo-textarea"
                  :auto-size="{ minRows: 10, maxRows: 16 }"
                  :maxlength="500"
                  :placeholder="activeQuickProfileEditor.placeholder"
                  show-count
                />
              </Form.Item>
              <div class="inside-memo-edit-grid">
                <Form.Item label="人均坑位">
                  <InputNumber
                    v-model:value="teamProfile.perCapitaPitAmount"
                    :min="0"
                    :precision="2"
                    addon-before="¥"
                    class="inside-memo-number-input"
                  />
                </Form.Item>
                <Form.Item label="自费加点率">
                  <InputNumber
                    v-model:value="teamProfile.optionalMarkupRate"
                    :min="0"
                    :precision="2"
                    addon-after="%"
                    class="inside-memo-number-input"
                  />
                </Form.Item>
                <Form.Item label="人均购物">
                  <InputNumber
                    v-model:value="teamProfile.perCapitaShoppingAmount"
                    :min="0"
                    :precision="2"
                    addon-before="¥"
                    class="inside-memo-number-input"
                  />
                </Form.Item>
              </div>
            </template>
            <Form.Item v-else :label="activeQuickProfileEditor.label" :required="activeQuickProfileEditor.required">
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
                :maxlength="quickProfileEditorTextMaxLength"
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

      <ArrangementEditorModal
        v-model:open="arrangementModalOpen"
        v-model:arrival-region-path="arrivalRegionPath"
        v-model:departure-region-path="departureRegionPath"
        :active-editor-type="activeEditorType"
        :driver-history-options="driverHistoryOptions"
        :editor-credit-amount="editorCreditAmount"
        editorMode="product"
        :editor-total-amount="activeEditorTotalAmount"
        :editing-arrangement-index="editingArrangementIndex"
        :employee-options="employeeOptions"
        :form="arrangementForm"
        :last-vehicle-quote-result="lastVehicleQuoteResult"
        :options-loading="optionsLoading"
        :project-options="projectOptions"
        :region-options="regionOptions"
        :resource-options="resourceOptions"
        :saving="saving"
        :schedule-day-options="scheduleDayOptions"
        :scenic-ticket-template="scenicTicketTemplate"
        :scenic-ticket-template-loading="scenicTicketTemplateLoading"
        :show-multi-order-average-price-notice="showMultiOrderAveragePriceNotice"
        :supplier-options="supplierOptions"
        :vehicle-plate-history-options="vehiclePlateHistoryOptions"
        :vehicle-quote-calculating="vehicleQuoteCalculating"
        :vehicle-quote-rule-options="vehicleQuoteRuleOptions"
        @add-arrangement-price-line="addArrangementPriceLine"
        @add-optional-summary-line="addOptionalSummaryLine"
        @add-shopping-consumption-line="addShoppingConsumptionLine"
        @apply-selected-price-project="applySelectedPriceProject"
        @apply-selected-resource="applySelectedResource"
        @apply-selected-responsible="applySelectedResponsible"
        @apply-selected-supplier="applySelectedSupplier"
        @apply-shopping-price-project="applyShoppingPriceProject"
        @apply-vehicle-quote-to-price-info="applyVehicleQuoteToPriceInfo"
        @calculate-vehicle-reference-price="calculateVehicleReferencePrice"
        @close="closeArrangementEditor"
        @open-hotel-create-page="openHotelCreatePage"
        @open-product-roadbook-editor="openProductRoadbookEditor"
        @open-resource-create-page="openResourceCreatePage"
        @open-scenic-template-config-page="openScenicTemplateConfigPage"
        @open-supplier-create-page="openSupplierCreatePage"
        @remove-arrangement-price-line="removeArrangementPriceLine"
        @remove-optional-summary-line="removeOptionalSummaryLine"
        @remove-shopping-consumption-line="removeShoppingConsumptionLine"
        @save="saveArrangementEditor"
        @sync-ground-agent-days-count="syncGroundAgentDaysCount"
        @sync-hotel-nights-count="syncHotelNightsCount"
        @sync-optional-line-to-form="syncOptionalLineToForm"
        @sync-primary-price-fields="syncPrimaryPriceFields"
        @sync-shopping-line-to-form="syncShoppingLineToForm"
        @sync-vehicle-days-count="syncVehicleDaysCount"
        @sync-vehicle-price-project="syncVehiclePriceProject"
        @sync-vehicle-roadbook-distance="syncVehicleRoadbookDistance"
        @vehicle-history-search="loadVehicleHistoryOptions"
      />
    </Spin>
  </Page>
</template>

<style scoped>
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

.inside-memo-edit-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.inside-memo-textarea {
  line-height: 1.7;
}

.inside-memo-number-input {
  width: 100%;
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
</style>
