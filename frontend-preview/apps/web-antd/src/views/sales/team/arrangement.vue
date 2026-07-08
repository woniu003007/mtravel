<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { DispatchGuideApi } from '#/api/dispatch/guide-schedule';
import type { EnterpriseGuideApi } from '#/api/enterprise/guide';
import type { SalesProductApi } from '#/api/sales/product';
import type { SalesTeamApi } from '#/api/sales/team';
import type { RegionPath } from '#/utils/region';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Checkbox,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Spin,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  createTeamGuide,
  deleteTeamGuide,
  getTeamGuides,
  updateTeamGuideField,
} from '#/api/dispatch/guide-schedule';
import {
  calculateVehicleQuote as calculateVehicleQuoteRule,
  getVehicleQuoteRuleAll,
} from '#/api/dispatch/vehicle-quote';
import { getExpenseItemAll } from '#/api/enterprise/expense-item';
import { getEnterpriseDepartmentAll } from '#/api/enterprise/department';
import { getEnterpriseEmployeeAll } from '#/api/enterprise/employee';
import { getEnterpriseGuideAll } from '#/api/enterprise/guide';
import {
  type EnterpriseProductDictionaryApi as ProductDictionaryNamespace,
  getProductDictionaryAll,
} from '#/api/enterprise/product-dictionary';
import { getPurchaseRelationPage } from '#/api/purchase/relation';
import {
  getRelationTicketTemplateDetail,
  type RelationTicketTemplateApi,
} from '#/api/purchase/relation-ticket-template';
import { getPurchaseResourcePage } from '#/api/purchase/resource';
import { getSupplierAll, type SupplierApi } from '#/api/purchase/supplier';
import {
  deleteTeamArrangement,
  exportScenicTicketGuests,
  getSalesTeamOperationDetail,
  getTeamArrangements,
  saveSalesTeam,
  saveTeamArrangement,
} from '#/api/sales/team';
import {
  getVehicleUsageHistorySuggestions,
  recordVehicleUsageHistory,
} from '#/api/sales/product';
import { buildRegionOptions } from '#/utils/region';

import ArrangementEditorModal from '../components/ArrangementEditorModal.vue';
import { showTeamItineraryModal } from '../components/itinerary-modal';
import {
  createGroundAgentPackagePriceLine,
  createDefaultArrangementEditorForm,
  createDefaultArrangementPriceLine,
  defaultProjectName,
  ensureSelectOption,
  arrangementEditorConfigs,
  priceProjectOptionsForType,
  resolveArrangementResourceName,
  resolveGroundAgentPackageAmount,
  resolveSupplierOptionsForResource,
  routeDurationText,
  parseScheduleDayNo,
  scheduleExclusiveNightsCount,
  scheduleInclusiveDaysCount,
  vehicleDistanceText,
  type ArrangementEditorForm,
  type AutoCompleteOption,
  type SelectOption,
  type SelectOptionWithId,
} from '../components/arrangement-editor-model';

import '../team-arrangement-layout.css';

type OperationDetail = SalesTeamApi.OperationDetail;
type OperationOrderRow = SalesTeamApi.OperationOrderRow;
type DatePickerValue = Dayjs | string | undefined;
type TeamGuideRow = DispatchGuideApi.TeamGuide;
type TeamArrangementRow = SalesTeamApi.TeamArrangement;

type ArrangementType =
  | 'extra_fee'
  | 'ground_agent'
  | 'hotel'
  | 'meal'
  | 'optional'
  | 'other'
  | 'scenic'
  | 'shopping'
  | 'traffic'
  | 'vehicle';

type ArrangementShortcut = {
  anchor: string;
  icon: string;
  label: string;
  value: ArrangementType | 'guide' | 'overview';
};

type ArrangementSection = {
  anchor: string;
  columns: string[];
  documentAction?: string;
  icon: string;
  label: string;
  value: ArrangementType;
};

type CostColumn = {
  cash: number;
  credit: number;
  key: string;
  label: string;
};

type SectionSummary = {
  cash: number;
  cost: number;
  count: number;
  credit: number;
};

type ArrangementToolAction = {
  icon: string;
  label: string;
};

type DictItem = ProductDictionaryNamespace.Item;

type ResourceRelationOption = {
  relationId: number;
  resourceId: number;
  resourceName: string;
  supplierId: number;
  supplierName?: string;
};

type TeamProfileSelectOption = {
  label: string;
  value: number | string;
};

type QuickProfileEditorType = 'business_type' | 'department' | 'escort' | 'operator';

type TeamProfileDraft = {
  businessType?: string;
  departmentId?: number;
  departmentName?: string;
  escortEmployeeName?: string;
  operatorEmployeeId?: number;
  operatorEmployeeName?: string;
};

type TeamBadgeItem = {
  color?: string;
  editorType?: QuickProfileEditorType;
  label: string;
  value: string;
};

type QuickProfileEditorConfig = {
  buttonText: string;
  field: keyof TeamProfileDraft;
  inputType: 'select' | 'textarea';
  label: string;
  optionsType?: 'business_type' | 'department' | 'employee';
  placeholder: string;
  required?: boolean;
  title: string;
};

const route = useRoute();
const router = useRouter();

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
    field: 'departmentId',
    inputType: 'select',
    label: '所属部门',
    optionsType: 'department',
    placeholder: '=选择部门=',
    required: true,
    title: '修改业务部门',
  },
  operator: {
    buttonText: '提交保存',
    field: 'operatorEmployeeId',
    inputType: 'select',
    label: '操作计调',
    optionsType: 'employee',
    placeholder: '=选择操作计调=',
    required: true,
    title: '修改操作计调',
  },
  escort: {
    buttonText: '保存信息',
    field: 'escortEmployeeName',
    inputType: 'textarea',
    label: '团队全陪信息',
    placeholder: '最多输入100个汉字',
    title: '团队全陪信息',
  },
};

const supplierCategoryMap: Record<ArrangementType, SupplierApi.Category | undefined> = {
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

const expenseResourceTypeMap: Record<ArrangementType, string> = {
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
const arrangementModalOpen = ref(false);
const optionsLoading = ref(false);
const guideModalOpen = ref(false);
const guideSaving = ref(false);
const guideLoading = ref(false);
const guideOptionsLoaded = ref(false);
const quickProfileEditorOpen = ref(false);
const quickProfileEditorType = ref<QuickProfileEditorType>('business_type');
const quickProfileSaving = ref(false);
const teamProfileOptionsLoading = ref(false);
const vehicleQuoteCalculating = ref(false);
const activeEditorType = ref<ArrangementType>('traffic');
const sectionLocalStates = reactive<Record<string, 'done' | 'none' | undefined>>({});
const detail = ref<OperationDetail>();
const teamGuides = ref<TeamGuideRow[]>([]);
const teamArrangements = ref<TeamArrangementRow[]>([]);
const guideOptions = ref<EnterpriseGuideApi.Item[]>([]);
const arrangementForm = reactive<ArrangementEditorForm>(createDefaultArrangementEditorForm('traffic'));
const teamProfileDraft = reactive<TeamProfileDraft>({});
const editingArrangementId = ref<number | undefined>();
const arrangementSaving = ref(false);
const departureRegionPath = ref<RegionPath | undefined>([]);
const arrivalRegionPath = ref<RegionPath | undefined>([]);
const businessTypeOptions = ref<TeamProfileSelectOption[]>([]);
const departmentOptions = ref<TeamProfileSelectOption[]>([]);
const supplierOptions = ref<SelectOptionWithId[]>([]);
const projectOptions = ref<SelectOptionWithId[]>([]);
const resourceOptions = ref<SelectOptionWithId[]>([]);
const resourceRelationOptions = ref<ResourceRelationOption[]>([]);
const scenicTicketTemplate = ref<RelationTicketTemplateApi.Template | null>(null);
const scenicTicketTemplateLoading = ref(false);
const employeeOptions = ref<SelectOptionWithId[]>([]);
const profileEmployeeOptions = ref<TeamProfileSelectOption[]>([]);
const vehicleQuoteRuleOptions = ref<SelectOption[]>([]);
const driverHistoryOptions = ref<AutoCompleteOption[]>([]);
const vehiclePlateHistoryOptions = ref<AutoCompleteOption[]>([]);
const lastVehicleQuoteResult = ref<{
  amount: number;
  distanceMeters: number;
  ruleName: string;
}>();
const regionOptions = buildRegionOptions();
const guideDraft = reactive<DispatchGuideApi.TeamGuideSaveParams>({
  endAt: '',
  feeMemo: '',
  guideFee: 0,
  guideId: 0,
  guideMemo: '',
  imprestAmount: 0,
  operationFee: 0,
  startAt: '',
  tentative: false,
});
const guideDraftDates = reactive({
  endAt: undefined as DatePickerValue,
  startAt: undefined as DatePickerValue,
});

const arrangementShortcuts: ArrangementShortcut[] = [
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
  { anchor: 'guide-arrangement', icon: 'lucide:badge', label: '导游', value: 'guide' },
  ...arrangementShortcuts,
];

const arrangementSections: ArrangementSection[] = [
  {
    ...arrangementShortcuts[0]!,
    columns: ['日期', '类型', '出发地', '目的地', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    value: 'traffic',
  },
  {
    ...arrangementShortcuts[1]!,
    columns: ['入住', '退房', '几晚', '酒店名称', '早餐', '基金', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    documentAction: '订房单',
    value: 'hotel',
  },
  {
    ...arrangementShortcuts[2]!,
    columns: ['开始', '结束', '车型', '天数', '司机', '车牌', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    documentAction: '订车单',
    value: 'vehicle',
  },
  {
    ...arrangementShortcuts[3]!,
    columns: ['日期', '景区名称', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    documentAction: '预订单',
    value: 'scenic',
  },
  {
    ...arrangementShortcuts[4]!,
    columns: ['日期', '时间', '餐厅名称', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    value: 'meal',
  },
  {
    ...arrangementShortcuts[5]!,
    columns: ['日期', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    value: 'other',
  },
  {
    ...arrangementShortcuts[6]!,
    columns: ['日期', '景区/项目名称', '供应商', '备注', '人数', '销售价', '成本价', '收入合计', '成本合计', '导游提成', '现结', '挂账', '操作'],
    value: 'optional',
  },
  {
    ...arrangementShortcuts[7]!,
    columns: ['日期', '购物店', '供应商', '备注', '品类', '进店人数', '人头费', '消费总额', '导游提成', '操作'],
    value: 'shopping',
  },
  {
    ...arrangementShortcuts[8]!,
    columns: ['开始', '结束', '天数', '供应商', '备注', '成本合计', '现结', '挂账', '操作'],
    documentAction: '确认单',
    value: 'ground_agent',
  },
  {
    ...arrangementShortcuts[9]!,
    columns: ['日期', '供应商', '备注', '价格信息', '成本合计', '现结', '挂账', '操作'],
    value: 'extra_fee',
  },
];

const arrangementStages = [
  { label: '团队信息', state: 'done' },
  { label: '安排配置', state: 'template' },
  { label: '发团', state: 'pending' },
  { label: '结算', state: 'pending' },
  { label: '完成', state: 'pending' },
];

const arrangementToolActions: ArrangementToolAction[] = [
  { icon: 'lucide:circle-plus', label: '票据库' },
  { icon: 'lucide:upload', label: '订单文件' },
  { icon: 'lucide:wallet-cards', label: '备用金请款单' },
  { icon: 'lucide:printer', label: '打印付款单' },
  { icon: 'lucide:printer', label: '打印行程单' },
  { icon: 'lucide:printer', label: '打印团队名单' },
  { icon: 'lucide:printer', label: '打印计划单' },
  { icon: 'lucide:printer', label: '打印结算单' },
  { icon: 'lucide:printer', label: '打印毛利表' },
];

const teamId = computed(() => Number(route.params.id || 0));
const team = computed(() => detail.value?.team);
const product = computed(() => detail.value?.product);
const content = computed(() => detail.value?.content);
const teamItineraryDays = computed(() => detail.value?.itineraryDays || []);
const orders = computed(() => detail.value?.orders || []);
const orderOptions = computed<SelectOption[]>(() => [
  ...orders.value
    .filter((item) => item.orderRole !== 'merge_source')
    .map((item) => {
      const customerText = item.orderInfo || item.guestName || '未命名订单';
      const contactText = item.orderInfo ? item.guestName : '';
      const bookingText = item.bookingInfo;
      const guestText = item.guestCountText || (item.guestCount ? `${item.guestCount}人` : '');
      const label = [
        item.orderNo || `订单${item.id}`,
        customerText,
        contactText,
        bookingText,
        guestText,
      ].filter(Boolean).join(' / ');
      return {
        label,
        value: String(item.id),
      };
    }),
]);
const pageTitle = computed(() => (team.value?.teamNo ? `团队安排 - ${team.value.teamNo}` : '团队安排'));
const teamDisplayName = computed(() => product.value?.productName || team.value?.teamNo || '--');
const orderReceivable = computed(() => sumOrderMoney('receivableAmount'));
const orderReceived = computed(() => sumOrderMoney('receivedAmount'));
const orderBalance = computed(() => sumOrderMoney('balanceAmount'));
const orderStatusSummary = computed(() => {
  const confirmed = orders.value.filter((item) => isOrderStatus(item, ['已确认', 'confirmed'])).length;
  const pending = orders.value.filter((item) => isOrderStatus(item, ['未处理', 'pending'])).length;
  const cancelled = orders.value.filter((item) => isOrderStatus(item, ['已取消', 'cancelled'])).length;
  return `${confirmed} | ${pending} | ${cancelled}`;
});
const guideSelectOptions = computed(() =>
  guideOptions.value.map((item) => ({
    label: item.mobilePhone
      ? `${item.guideName}（${item.mobilePhone}）`
      : item.guideName,
    value: item.id,
  })),
);
const selectedScenicResourceRelation = computed(() => (
  resourceRelationOptions.value.find((item) => (
    item.resourceName === arrangementForm.resourceName
    && item.supplierId === arrangementForm.supplierId
  ))
));
const guideFeeTotal = computed(() => teamGuides.value.reduce((sum, item) => sum + Number(item.guideFee || 0), 0));
const guideOperationFeeTotal = computed(() => teamGuides.value.reduce((sum, item) => sum + Number(item.operationFee || 0), 0));
const guideImprestTotal = computed(() => teamGuides.value.reduce((sum, item) => sum + Number(item.imprestAmount || 0), 0));
const regularArrangementCostTotal = computed(() => (
  teamArrangements.value
    .filter((item) => !['optional', 'shopping'].includes(item.arrangementType))
    .reduce((sum, item) => sum + numericMoney(item.totalAmount || item.costAmount), 0)
));
const optionalCompanyProfitTotal = computed(() => (
  teamArrangements.value
    .filter((item) => item.arrangementType === 'optional')
    .reduce((sum, item) => (
      sum
      + numericMoney(item.saleAmount)
      - numericMoney(item.costAmount)
      - numericMoney(item.guideCommissionAmount)
    ), 0)
));
const shoppingCompanyProfitTotal = computed(() => (
  teamArrangements.value
    .filter((item) => item.arrangementType === 'shopping')
    .reduce((sum, item) => (
      sum
      + numericMoney(item.headFeeAmount)
      + numericMoney(item.companyRebateAmount)
      - numericMoney(item.guideCommissionAmount)
    ), 0)
));
const budgetProfit = computed(() => (
  orderReceivable.value
  + optionalCompanyProfitTotal.value
  + shoppingCompanyProfitTotal.value
  - regularArrangementCostTotal.value
  - guideFeeTotal.value
));
const teamTravelDays = computed(() => Math.max(Number(team.value?.travelDays || 1), 1));
const scheduleDayOptions = computed<SelectOption[]>(() => [
  { label: '=出发日期=', value: '=出发日期=' },
  ...Array.from({ length: teamTravelDays.value }, (_, index) => {
    const day = index + 1;
    return { label: `第${day}天`, value: `第${day}天` };
  }),
]);
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
const showMultiOrderAveragePriceNotice = computed(() => (
  arrangementForm.allocationMode === 'multi_order_average'
));
const costColumns = computed<CostColumn[]>(() => {
  const base = [
    { key: 'traffic', label: '大交通' },
    { key: 'hotel', label: '住宿' },
    { key: 'vehicle', label: '用车' },
    { key: 'scenic', label: '景区' },
    { key: 'meal', label: '用餐' },
    { key: 'other', label: '其它' },
    { key: 'ground_agent', label: '地接' },
    { key: 'extra_fee', label: '附加' },
    { key: 'optional', label: '自费' },
    { key: 'shopping', label: '购物' },
  ].map((item) => {
    const records = arrangementsByType(item.key as ArrangementType);
    return {
      ...item,
      cash: sumArrangementAmount(records, 'cashAmount'),
      credit: sumArrangementAmount(records, 'creditAmount'),
    };
  });
  const cashTotal = base.reduce((sum, item) => sum + item.cash, 0);
  const creditTotal = base.reduce((sum, item) => sum + item.credit, 0);
  return [
    ...base,
    { cash: cashTotal, credit: creditTotal, key: 'total', label: '合计' },
    { cash: guideFeeTotal.value, credit: 0, key: 'guide_service', label: '导服' },
    { cash: guideOperationFeeTotal.value, credit: 0, key: 'operation_fee', label: '操作费' },
    { cash: guideImprestTotal.value, credit: 0, key: 'reserve_fund', label: '备用金' },
  ];
});
function costColumnAmount(key: string, field: 'cash' | 'credit') {
  return costColumns.value.find((item) => item.key === key)?.[field] || 0;
}

const costOverviewSummaryItems = computed(() => {
  const cashTotal = costColumnAmount('total', 'cash');
  const creditTotal = costColumnAmount('total', 'credit');
  return [
    { key: 'cost_total', label: '成本合计', tone: 'primary', value: cashTotal + creditTotal },
    { key: 'cash_total', label: '现结合计', tone: 'strong', value: cashTotal },
    { key: 'credit_total', label: '挂账合计', tone: 'strong', value: creditTotal },
    { key: 'reserve_fund', label: '备用金', tone: 'strong', value: costColumnAmount('reserve_fund', 'cash') },
    { key: 'guide_service', label: '导服', value: costColumnAmount('guide_service', 'cash') },
    { key: 'operation_fee', label: '操作费', value: costColumnAmount('operation_fee', 'cash') },
    { key: 'self_pay_income', label: '自费收入', value: 0 },
  ];
});

const metricItems = computed(() => [
  { key: 'travelDays', label: '旅游天数', value: `${team.value?.travelDays ?? 1} 天` },
  { key: 'standard', label: '接待标准', value: product.value?.receptionStandard || '--' },
  { key: 'distance', label: '总里程数', value: formatDistanceMeters(detail.value?.routeSummary?.totalDistanceMeters) },
  { key: 'receivable', label: '应收/已收/余额', value: `${formatPlainMoney(orderReceivable.value)} | ${formatPlainMoney(orderReceived.value)} | ${formatPlainMoney(orderBalance.value)}` },
  { key: 'orderStatus', label: '订单已确认/未处理/已取消', value: orderStatusSummary.value },
  { key: 'paid', label: '已付', value: '--' },
  { key: 'profit', label: '预算利润', value: formatPlainMoney(budgetProfit.value) },
]);
const teamBadges = computed<TeamBadgeItem[]>(() => [
  { color: 'orange', editorType: 'business_type', label: '业务类型', value: team.value?.businessType || product.value?.businessType || '未设置' },
  { color: 'blue', editorType: 'department', label: '部门', value: team.value?.departmentName || '未设置' },
  { color: 'blue', editorType: 'operator', label: '操作计调', value: team.value?.operatorEmployeeName || '未设置' },
  { color: 'orange', label: '导游', value: team.value?.guideSummary || '--' },
  { color: 'green', label: '领队', value: team.value?.leaderSummary || '--' },
  { color: 'default', editorType: 'escort', label: '全陪', value: team.value?.escortEmployeeName || team.value?.escortSummary || '未设置' },
]);
const activeQuickProfileEditor = computed(() => quickProfileEditorConfigs[quickProfileEditorType.value]);
const quickProfileEditorModel = computed({
  get: () => teamProfileDraft[activeQuickProfileEditor.value.field],
  set: (value?: number | string) => {
    const field = activeQuickProfileEditor.value.field;
    if (field === 'departmentId' || field === 'operatorEmployeeId') {
      teamProfileDraft[field] = typeof value === 'number' ? value : undefined;
      return;
    }
    teamProfileDraft[field] = typeof value === 'string' ? value : undefined;
  },
});

function teamTypeLabel(value?: string) {
  const labels: Record<string, string> = {
    sanpin: '散拼',
    santuan: '散团',
    single: '单项',
    zhengtuan: '整团',
  };
  return value ? labels[value] || value : '--';
}

function formatDistanceMeters(value?: number) {
  if (!value || value <= 0) return '0.0公里';
  if (value < 1000) return `${value}米`;
  return `${(value / 1000).toFixed(1)}公里`;
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
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

function dictionaryOptions(items: DictItem[]): TeamProfileSelectOption[] {
  return items
    .filter((item) => item.status === 'active')
    .map((item) => ({
      label: item.dictName,
      value: item.dictName,
    }));
}

function numericMoney(value?: number) {
  const result = Number(value || 0);
  return Number.isFinite(result) ? result : 0;
}

function sumArrangementAmount(records: TeamArrangementRow[], field: 'cashAmount' | 'creditAmount') {
  return records.reduce((sum, item) => sum + numericMoney(item[field]), 0);
}

function arrangementsByType(type: ArrangementType) {
  return teamArrangements.value.filter((item) => item.arrangementType === type);
}

function sectionStatusText(type: ArrangementType) {
  const localState = sectionLocalStates[type];
  if (localState === 'none') return '无需';
  if (localState === 'done') return '完成';
  return arrangementsByType(type).length ? '已安排' : '未安排';
}

function sectionStatusTone(type: ArrangementType) {
  const status = sectionStatusText(type);
  if (status === '完成') return 'done';
  if (status === '无需') return 'none';
  if (status === '已安排') return 'arranged';
  return 'pending';
}

function sectionSummary(type: ArrangementType): SectionSummary {
  const records = arrangementsByType(type);
  return {
    cash: sumArrangementAmount(records, 'cashAmount'),
    cost: records.reduce((sum, item) => sum + numericMoney(item.totalAmount || item.costAmount), 0),
    count: records.length,
    credit: sumArrangementAmount(records, 'creditAmount'),
  };
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
      value: item.id,
    }));
    profileEmployeeOptions.value = employees.map((item) => ({
      label: item.employeeName,
      value: item.id,
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

function resetTeamProfileDraft() {
  Object.assign(teamProfileDraft, {
    businessType: team.value?.businessType || product.value?.businessType,
    departmentId: team.value?.departmentId,
    departmentName: team.value?.departmentName,
    escortEmployeeName: team.value?.escortEmployeeName || team.value?.escortSummary,
    operatorEmployeeId: team.value?.operatorEmployeeId,
    operatorEmployeeName: team.value?.operatorEmployeeName,
  });
}

async function openTeamProfileEditor(editorType: QuickProfileEditorType) {
  resetTeamProfileDraft();
  quickProfileEditorType.value = editorType;
  quickProfileEditorOpen.value = true;
  if (activeQuickProfileEditor.value.inputType === 'select') {
    await loadTeamProfileOptions();
    hydrateTeamProfileDraftSelection(editorType);
  }
}

function selectedTeamProfileOption(options: TeamProfileSelectOption[], value?: number | string) {
  return options.find((item) => item.value === value);
}

function showItineraryModal() {
  showTeamItineraryModal({
    departureDate: team.value?.departureDate,
    fallbackDescription: content.value?.productDescription,
    itineraryDays: teamItineraryDays.value,
  });
}

function hydrateTeamProfileDraftSelection(editorType: QuickProfileEditorType) {
  if (editorType === 'department' && !teamProfileDraft.departmentId && teamProfileDraft.departmentName) {
    const matched = departmentOptions.value.find((item) => item.label === teamProfileDraft.departmentName);
    teamProfileDraft.departmentId = typeof matched?.value === 'number' ? matched.value : undefined;
  }
  if (editorType === 'operator' && !teamProfileDraft.operatorEmployeeId && teamProfileDraft.operatorEmployeeName) {
    const matched = profileEmployeeOptions.value.find((item) => item.label === teamProfileDraft.operatorEmployeeName);
    teamProfileDraft.operatorEmployeeId = typeof matched?.value === 'number' ? matched.value : undefined;
  }
}

function buildTeamProfilePayload(editorType: QuickProfileEditorType): SalesTeamApi.TeamSaveParams {
  if (editorType === 'business_type') {
    return { businessType: String(teamProfileDraft.businessType || '').trim() };
  }
  if (editorType === 'department') {
    const selected = selectedTeamProfileOption(departmentOptions.value, teamProfileDraft.departmentId);
    return {
      departmentId: typeof teamProfileDraft.departmentId === 'number' ? teamProfileDraft.departmentId : undefined,
      departmentName: selected?.label || teamProfileDraft.departmentName || '',
    };
  }
  if (editorType === 'operator') {
    const selected = selectedTeamProfileOption(profileEmployeeOptions.value, teamProfileDraft.operatorEmployeeId);
    return {
      operatorEmployeeId: typeof teamProfileDraft.operatorEmployeeId === 'number' ? teamProfileDraft.operatorEmployeeId : undefined,
      operatorEmployeeName: selected?.label || teamProfileDraft.operatorEmployeeName || '',
    };
  }
  return {
    escortEmployeeName: String(teamProfileDraft.escortEmployeeName || '').trim(),
  };
}

async function saveTeamProfileEditor() {
  const editor = activeQuickProfileEditor.value;
  const currentValue = teamProfileDraft[editor.field];
  if (editor.required && (currentValue === undefined || currentValue === '')) {
    message.warning(`请选择${editor.label}`);
    return;
  }
  quickProfileSaving.value = true;
  try {
    await saveSalesTeam(teamId.value, buildTeamProfilePayload(quickProfileEditorType.value));
    message.success(`${editor.title}已保存`);
    quickProfileEditorOpen.value = false;
    detail.value = await getSalesTeamOperationDetail(teamId.value);
  } finally {
    quickProfileSaving.value = false;
  }
}

function formatPriceLines(record: TeamArrangementRow) {
  const lines = record.priceLines || [];
  if (!lines.length) return '--';
  return lines
    .map((line) => {
      const name = line.projectName || record.itemName;
      const unitPrice = Number(line.unitPrice || 0).toFixed(2);
      const quantity = Number(line.quantity || 0);
      return `${name} ¥${unitPrice} × ${quantity}`;
    })
    .join('；');
}

function arrangementCell(record: TeamArrangementRow, section: ArrangementSection, column: string) {
  switch (column) {
    case '入住':
    case '开始':
    case '日期':
      return record.scheduleStartDay || record.businessDate || '--';
    case '退房':
    case '结束':
      return record.scheduleEndDay || '--';
    case '几晚':
    case '天数':
      return String(record.daysCount || 0);
    case '类型':
      return record.trafficType || '--';
    case '出发地':
      return record.departurePlace || '--';
    case '目的地':
      return record.arrivalPlace || '--';
    case '酒店名称':
    case '餐厅名称':
    case '景区名称':
    case '景区/项目名称':
    case '购物店':
      return record.resourceName || record.itemName || '--';
    case '车型':
      return record.vehicleType || '--';
    case '司机':
      return record.driverName || '--';
    case '车牌':
      return record.vehiclePlate || '--';
    case '供应商':
      return record.supplierName || '--';
    case '备注':
      return record.arrangementContent || '--';
    case '价格信息':
    case '品类':
      return formatPriceLines(record);
    case '成本合计':
      return formatMoney(record.totalAmount || record.costAmount);
    case '现结':
      return formatMoney(record.cashAmount);
    case '挂账':
      return formatMoney(record.creditAmount);
    case '早餐':
      return record.mealType || '--';
    case '基金':
      return record.fundIncluded || '--';
    case '时间':
      return record.mealType || '--';
    case '人数':
    case '进店人数':
      return String(record.peopleCount || 0);
    case '销售价':
      return formatMoney(record.saleAmount);
    case '成本价':
      return formatMoney(record.costAmount);
    case '收入合计':
    case '消费总额':
      return formatMoney(record.saleAmount || record.consumptionAmount);
    case '导游提成':
      return formatMoney(record.guideCommissionAmount);
    case '人头费':
      return formatMoney(record.headFeeAmount);
    default:
      return section.value === 'traffic' ? record.itemName || '--' : '--';
  }
}

function seatCountFromVehicleType(value?: string) {
  const matched = String(value || '').match(/\d+/);
  return matched?.[0] ? Number(matched[0]) : undefined;
}

function formatBackendDateTime(value: DatePickerValue | null) {
  return value ? dayjs(value).format('YYYY-MM-DDTHH:mm:ss') : '';
}

function dateTimeValue(value?: string) {
  return value ? dayjs(value) : undefined;
}

function defaultGuideStartAt() {
  const departureDate = team.value?.departureDate || dayjs().format('YYYY-MM-DD');
  return dayjs(`${departureDate} 08:00`, 'YYYY-MM-DD HH:mm');
}

function defaultGuideEndAt() {
  const travelDays = Math.max(Number(team.value?.travelDays || 1), 1);
  return defaultGuideStartAt().add(travelDays - 1, 'day').hour(18).minute(0).second(0);
}

function guideSelectOptionsForRecord(record: TeamGuideRow) {
  if (guideSelectOptions.value.some((item) => item.value === record.guideId)) {
    return guideSelectOptions.value;
  }
  return [
    {
      label: record.guideMobile
        ? `${record.guideName}（${record.guideMobile}）`
        : record.guideName,
      value: record.guideId,
    },
    ...guideSelectOptions.value,
  ];
}

function parseMoney(value?: string) {
  if (!value) return 0;
  const normalized = String(value).replace(/[^\d.-]/g, '');
  const result = Number(normalized);
  return Number.isFinite(result) ? result : 0;
}

function sumOrderMoney(field: 'balanceAmount' | 'receivableAmount' | 'receivedAmount') {
  return orders.value.reduce((sum, item) => sum + parseMoney(item[field]), 0);
}

function isOrderStatus(order: OperationOrderRow, statuses: string[]) {
  return statuses.includes(order.status || '');
}

function scrollToArrangementAnchor(anchor: string) {
  document.getElementById(anchor)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function setSectionLocalState(type: ArrangementType, state: 'done' | 'none') {
  sectionLocalStates[type] = state;
  message.info('分类状态后续接入团队流程状态表');
}

async function openArrangementEditor(type: ArrangementType) {
  activeEditorType.value = type;
  editingArrangementId.value = undefined;
  Object.assign(arrangementForm, createDefaultArrangementEditorForm(type));
  departureRegionPath.value = [];
  arrivalRegionPath.value = [];
  supplierOptions.value = [];
  projectOptions.value = [];
  resourceOptions.value = [];
  resourceRelationOptions.value = [];
  scenicTicketTemplate.value = null;
  employeeOptions.value = [];
  vehicleQuoteRuleOptions.value = [];
  driverHistoryOptions.value = [];
  vehiclePlateHistoryOptions.value = [];
  arrangementModalOpen.value = true;
  await loadEditorOptions(type);
  if (type === 'scenic') {
    await loadSelectedScenicTicketTemplate();
  }
  if (type === 'vehicle') {
    syncVehiclePriceProject(arrangementForm.vehicleType);
    await Promise.all([
      loadVehicleHistoryOptions('driver_info'),
      loadVehicleHistoryOptions('vehicle_plate'),
    ]);
  }
}

async function editArrangement(record: TeamArrangementRow) {
  activeEditorType.value = record.arrangementType as ArrangementType;
  editingArrangementId.value = record.id;
  Object.assign(arrangementForm, arrangementToForm(record));
  departureRegionPath.value = record.departurePlace ? [record.departurePlace] : [];
  arrivalRegionPath.value = record.arrivalPlace ? [record.arrivalPlace] : [];
  arrangementModalOpen.value = true;
  await loadEditorOptions(activeEditorType.value);
  if (activeEditorType.value === 'scenic') {
    await loadSelectedScenicTicketTemplate();
  }
  if (activeEditorType.value === 'vehicle') {
    syncVehiclePriceProject(arrangementForm.vehicleType);
    await Promise.all([
      loadVehicleHistoryOptions('driver_info'),
      loadVehicleHistoryOptions('vehicle_plate'),
    ]);
  }
}

function arrangementToForm(record: TeamArrangementRow): ArrangementEditorForm {
  const form = createDefaultArrangementEditorForm(record.arrangementType as ArrangementType);
  const selectedOrderIds = (record.allocations || [])
    .filter((item) => item.allocationScope === 'order' && item.orderId)
    .map((item) => Number(item.orderId));
  const isGroundAgent = record.arrangementType === 'ground_agent';
  const groundAgentPackageAmount = isGroundAgent
    ? resolveGroundAgentPackageAmount({
      costAmount: record.costAmount,
      priceLines: record.priceLines,
      totalAmount: record.totalAmount,
    })
    : 0;
  return {
    ...form,
    allocationMode: record.allocationMode || 'group_order_average',
    arrivalPlace: record.arrivalPlace || '',
    cashAmount: numericMoney(record.cashAmount),
    companyRebateAmount: numericMoney(record.companyRebateAmount),
    confirmed: Boolean(record.confirmed),
    confirmationNo: record.confirmationNo || '',
    consumptionAmount: numericMoney(record.consumptionAmount),
    costAmount: isGroundAgent ? groundAgentPackageAmount : numericMoney(record.costAmount),
    creditAmount: numericMoney(record.creditAmount),
    daysCount: Number(record.daysCount || 0),
    departurePlace: record.departurePlace || '',
    driverName: record.driverName || '',
    fundIncluded: record.fundIncluded || form.fundIncluded,
    guideId: record.guideId,
    guideCommissionAmount: numericMoney(record.guideCommissionAmount),
    guideName: record.guideName || '',
    headFeeAmount: numericMoney(record.headFeeAmount),
    mealType: record.mealType || form.mealType,
    multiOrderSplitMode: record.splitMode || 'by_order',
    noGuideReport: Boolean(record.noGuideReport),
    orderScope: selectedOrderIds[0] ? String(selectedOrderIds[0]) : '=不关联订单=',
    peopleCount: numericMoney(record.peopleCount),
    prepaidAmount: numericMoney(record.prepaidAmount),
    priceLines: isGroundAgent
      ? [createGroundAgentPackagePriceLine(groundAgentPackageAmount, record.priceLines?.[0]?.remark)]
      : (record.priceLines?.length ? record.priceLines : form.priceLines).map((line, index) => ({
        ...line,
        projectName: line.projectName || defaultProjectName(record.arrangementType as ArrangementType),
        quantity: Number(line.quantity || 0),
        sortOrder: line.sortOrder || index + 1,
        unitPrice: Number(line.unitPrice || 0),
      })),
    resourceName: resolveArrangementResourceName(record.resourceName, record.itemName),
    responsibleEmployeeId: record.responsibleEmployeeId,
    responsibleEmployeeName: record.responsibleEmployeeName || '',
    saleAmount: numericMoney(record.saleAmount),
    scheduleEndDay: record.scheduleEndDay || form.scheduleEndDay,
    scheduleStartDay: record.scheduleStartDay || form.scheduleStartDay,
    selectedOrderIds,
    settlementType: record.settlementType || form.settlementType,
    supplierId: record.supplierId,
    supplierName: record.supplierName || '',
    trafficType: record.trafficType || form.trafficType,
    vehiclePlate: record.vehiclePlate || '',
    vehicleType: record.vehicleType || form.vehicleType,
  };
}

function editorNeedsEmployeeOptions(type: ArrangementType) {
  return Boolean(arrangementEditorConfigs[type]?.showResponsible);
}

async function loadEditorOptions(type: ArrangementType) {
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
    }
    if (type === 'vehicle') {
      vehicleQuoteRuleOptions.value = quoteRules;
    }
  } finally {
    optionsLoading.value = false;
  }
}

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

async function loadResourceOptions(type: ArrangementType): Promise<SelectOptionWithId[]> {
  resourceRelationOptions.value = [];
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
  const relationResourceTypeMap: Partial<Record<ArrangementType, 'restaurant' | 'scenic' | 'shopping'>> = {
    meal: 'restaurant',
    optional: 'scenic',
    scenic: 'scenic',
    shopping: 'shopping',
  };
  const relationResourceType = relationResourceTypeMap[type];
  if (!relationResourceType) return [];
  const [resources, relations] = await Promise.all([
    getPurchaseResourcePage({
      page: 1,
      pageSize: 200,
      resourceType: relationResourceType,
      status: 'active',
    }),
    getPurchaseRelationPage({
      page: 1,
      pageSize: 200,
      resourceType: relationResourceType,
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

function normalizeSelectValue(value: unknown) {
  return typeof value === 'string' ? value : undefined;
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

function syncPrimaryPriceFields() {
  const firstLine = arrangementForm.priceLines[0] || createDefaultArrangementPriceLine(defaultProjectName(activeEditorType.value));
  arrangementForm.projectId = firstLine.projectId;
  arrangementForm.projectName = firstLine.projectName || defaultProjectName(activeEditorType.value);
  arrangementForm.unitPrice = Number(firstLine.unitPrice || 0);
  arrangementForm.quantity = Number(firstLine.quantity || 0);
  arrangementForm.priceRemark = firstLine.remark;
}

function addArrangementPriceLine() {
  if (showMultiOrderAveragePriceNotice.value) return;
  arrangementForm.priceLines.push(createDefaultArrangementPriceLine(defaultProjectName(activeEditorType.value)));
  normalizeArrangementPriceLines();
}

function removeArrangementPriceLine(index: number) {
  if (arrangementForm.priceLines.length <= 1) {
    message.warning('价格信息至少保留一条');
    return;
  }
  arrangementForm.priceLines.splice(index, 1);
  normalizeArrangementPriceLines();
}

function applySelectedPriceProject(index: number, value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  const project = priceProjectOptionsForType(activeEditorType.value, projectOptions.value).find((item) => item.value === selectedValue);
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
  const project = priceProjectOptionsForType('vehicle', projectOptions.value).find((item) => item.value === selectedValue);
  firstLine.projectId = project?.id;
  firstLine.projectName = selectedValue;
  syncPrimaryPriceFields();
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
  if (activeEditorType.value === 'scenic') {
    void loadSelectedScenicTicketTemplate();
  } else {
    scenicTicketTemplate.value = null;
  }
}

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
  if (activeEditorType.value === 'scenic') {
    void loadSelectedScenicTicketTemplate();
  }
}

/** 查询当前景区采购关系上的游客名单模板，供团队票务 Excel 下载前校验。 */
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

function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.style.display = 'none';
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 100);
}

async function downloadScenicTicketGuests() {
  if (activeEditorType.value !== 'scenic') return;
  if (!arrangementForm.resourceName || !arrangementForm.supplierId) {
    message.warning('请选择景区和供应商后再下载游客名单');
    return;
  }
  if (!selectedScenicResourceRelation.value) {
    message.warning('当前景区和供应商未匹配到启用采购关系，请重新选择后再下载游客名单');
    return;
  }
  if (!scenicTicketTemplate.value) {
    message.warning('当前景区供应商未配置游客名单模板，请先配置模板');
    return;
  }
  const blob = await exportScenicTicketGuests(teamId.value, {
    resourceName: arrangementForm.resourceName,
    supplierId: arrangementForm.supplierId,
  });
  const safeResourceName = arrangementForm.resourceName.replace(/[\\/:*?"<>|\s]+/g, '_');
  triggerBlobDownload(blob, `${safeResourceName}游客名单${dayjs().format('YYYYMMDDHHmmss')}.xlsx`);
}

function applySelectedResponsible(value?: unknown) {
  const selectedValue = normalizeSelectValue(value);
  const employee = employeeOptions.value.find((item) => item.value === selectedValue);
  arrangementForm.responsibleEmployeeId = employee?.id;
  arrangementForm.responsibleEmployeeName = selectedValue;
}

function syncHotelNightsCount() {
  arrangementForm.daysCount = scheduleExclusiveNightsCount(
    arrangementForm.scheduleStartDay,
    arrangementForm.scheduleEndDay,
  );
}

function syncVehicleDaysCount() {
  const days = scheduleInclusiveDaysCount(
    arrangementForm.scheduleStartDay,
    arrangementForm.scheduleEndDay,
  );
  arrangementForm.daysCount = days;
}

function selectedVehicleDayRange() {
  const start = parseScheduleDayNo(arrangementForm.scheduleStartDay) || 1;
  const end = parseScheduleDayNo(arrangementForm.scheduleEndDay) || start;
  return {
    end: Math.max(start, end),
    start,
  };
}

function syncGroundAgentDaysCount() {
  const days = scheduleInclusiveDaysCount(
    arrangementForm.scheduleStartDay,
    arrangementForm.scheduleEndDay,
  );
  arrangementForm.daysCount = days;
}

function syncOptionalLineToForm() {
  normalizeArrangementPriceLines();
  let peopleCount = 0;
  let saleAmount = 0;
  let costAmount = 0;
  let cashAmount = 0;
  let guideCommissionAmount = 0;
  arrangementForm.priceLines.forEach((line) => {
    peopleCount += Number(line.quantity || 0);
    saleAmount += Number(line.salePrice || 0);
    costAmount += Number(line.costPrice || 0);
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

function addOptionalSummaryLine() {
  if (showMultiOrderAveragePriceNotice.value) return;
  const line = createDefaultArrangementPriceLine('成人');
  line.quantity = Number(arrangementForm.peopleCount || 0);
  line.salePrice = 0;
  line.costPrice = 0;
  line.cashAmount = 0;
  line.guideCommissionAmount = 0;
  arrangementForm.priceLines.push(line);
  syncOptionalLineToForm();
}

function removeOptionalSummaryLine(index: number) {
  removeArrangementPriceLine(index);
  syncOptionalLineToForm();
}

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

function addShoppingConsumptionLine() {
  if (showMultiOrderAveragePriceNotice.value) return;
  const line = createDefaultArrangementPriceLine('乳胶');
  line.consumptionAmount = 0;
  line.companyRebateRate = Number(arrangementForm.companyRebateRate || 0);
  line.companyRebateAmount = 0;
  line.guideCommissionRate = Number(arrangementForm.guideCommissionRate || 0);
  line.guideCommissionAmount = 0;
  line.cashAmount = 0;
  arrangementForm.priceLines.push(line);
  syncShoppingLineToForm();
}

function applyShoppingPriceProject(index: number, value?: unknown) {
  applySelectedPriceProject(index, value);
  syncShoppingLineToForm();
}

function removeShoppingConsumptionLine(index: number) {
  removeArrangementPriceLine(index);
  syncShoppingLineToForm();
}

function syncVehicleRoadbookDistance() {
  syncVehicleDaysCount();
  const { end, start } = selectedVehicleDayRange();
  const selectedDays = teamItineraryDays.value.filter((day) => Number(day.dayNo || 0) >= start && Number(day.dayNo || 0) <= end);
  const syncedDistanceMeters = selectedDays.reduce((sum, day) => sum + Number(day.roadbookTotalDistanceMeters || 0), 0);
  const syncedDurationSeconds = selectedDays.reduce((sum, day) => sum + Number(day.roadbookTotalDurationSeconds || 0), 0);
  const routeSummary = selectedDays
    .map((day) => `第${day.dayNo || ''}天：${day.roadbookSummary || day.dayTitle || '未维护路书'}`)
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
  const project = priceProjectOptionsForType('vehicle', projectOptions.value).find((item) => item.value === selectedValue);
  firstLine.projectId = project?.id;
  firstLine.projectName = selectedValue;
  firstLine.quantity = 1;
  firstLine.unitPrice = finalAmount;
  arrangementForm.cashAmount = 0;
  arrangementForm.prepaidAmount = 0;
  syncPrimaryPriceFields();
  message.success('已应用到价格信息');
}

/** 加载用车司机和车牌历史候选，正式排团页与产品排团页保持同一输入体验。 */
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

/** 保存用车安排后沉淀手工输入，后续打开正式排团用车弹窗可直接搜索复用。 */
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

function openArrangementRelationPage() {
  if (activeEditorType.value === 'scenic') {
    const routeInfo = router.resolve({
      path: '/purchase/relation',
      query: {
        resourceType: 'scenic',
        templateRelationId: selectedScenicResourceRelation.value?.relationId,
      },
    });
    window.open(routeInfo.href, '_blank', 'noopener,noreferrer');
    return;
  }
  const resourceType = activeEditorType.value === 'hotel'
    ? 'hotel'
    : activeEditorType.value === 'meal'
      ? 'restaurant'
      : activeEditorType.value === 'shopping'
        ? 'shopping'
        : undefined;
  if (resourceType) {
    const routeInfo = router.resolve({
      path: '/purchase/resource',
      query: { create: '1', resourceType },
    });
    window.open(routeInfo.href, '_blank', 'noopener,noreferrer');
    return;
  }
  message.info('该类型资源档案后续接入对应基础资料页面');
}

function resetGuideDraft() {
  Object.assign(guideDraft, {
    endAt: '',
    feeMemo: '',
    guideFee: 0,
    guideId: 0,
    guideMemo: '',
    imprestAmount: 0,
    operationFee: 0,
    startAt: '',
    tentative: false,
  });
  guideDraftDates.startAt = defaultGuideStartAt();
  guideDraftDates.endAt = defaultGuideEndAt();
}

async function loadGuideOptions() {
  if (guideOptionsLoaded.value) return;
  guideOptions.value = await getEnterpriseGuideAll(false);
  guideOptionsLoaded.value = true;
}

async function loadTeamGuides() {
  if (!teamId.value) return;
  guideLoading.value = true;
  try {
    teamGuides.value = await getTeamGuides(teamId.value);
  } finally {
    guideLoading.value = false;
  }
}

async function openGuideModal() {
  resetGuideDraft();
  await loadGuideOptions();
  guideModalOpen.value = true;
}

async function submitGuideDraft() {
  if (!guideDraft.guideId) {
    message.warning('请选择导游');
    return;
  }
  guideDraft.startAt = formatBackendDateTime(guideDraftDates.startAt);
  guideDraft.endAt = formatBackendDateTime(guideDraftDates.endAt);
  if (!guideDraft.startAt || !guideDraft.endAt) {
    message.warning('请填写上团和下团时间');
    return;
  }
  guideSaving.value = true;
  try {
    await createTeamGuide(teamId.value, {
      ...guideDraft,
      feeMemo: guideDraft.feeMemo?.trim(),
      guideMemo: guideDraft.guideMemo?.trim(),
    });
    message.success('导游安排已保存');
    guideModalOpen.value = false;
    await loadTeamGuides();
  } finally {
    guideSaving.value = false;
  }
}

async function saveGuideField(row: TeamGuideRow, field: string, value?: number | string | boolean) {
  const stringValue = value === undefined || value === null ? '' : String(value);
  const result = await updateTeamGuideField(teamId.value, row.id, { field, value: stringValue });
  const index = teamGuides.value.findIndex((item) => item.id === row.id);
  if (index >= 0) {
    teamGuides.value[index] = result;
  }
  message.success('已保存');
}

async function saveGuideDateField(row: TeamGuideRow, field: 'endAt' | 'startAt', value: DatePickerValue) {
  await saveGuideField(row, field, formatBackendDateTime(value));
}

async function saveGuideTentative(row: TeamGuideRow, checked: boolean) {
  await saveGuideField(row, 'tentative', checked);
}

async function removeTeamGuide(row: TeamGuideRow) {
  Modal.confirm({
    content: `确认删除 ${row.guideName} 的导游安排？`,
    title: '删除导游安排',
    async onOk() {
      await deleteTeamGuide(teamId.value, row.id);
      message.success('导游安排已删除');
      await loadTeamGuides();
    },
  });
}

function handleToolAction(action: { label: string }) {
  if (action.label === '打印毛利表') {
    openGrossProfitPreview();
    return;
  }
  Modal.info({
    content: '该动作后续接入正式文件、打印或成本导入接口。',
    title: action.label,
  });
}

function openGrossProfitPreview() {
  if (!teamId.value) return;
  router.push(`/sales/team/gross-profit/${teamId.value}`);
}

function handleMetricClick(item: { key: string }) {
  if (item.key === 'profit') {
    openGrossProfitPreview();
  }
}

function selectedOrderIdsForSubmit() {
  if (arrangementForm.allocationMode === 'multi_order_average') {
    return arrangementForm.selectedOrderIds.map(Number).filter(Boolean);
  }
  const orderId = Number(arrangementForm.orderScope);
  return Number.isFinite(orderId) && orderId > 0 ? [orderId] : [];
}

function priceLineAmountForSubmit(line: SalesTeamApi.TeamArrangementPriceLine) {
  if (activeEditorType.value === 'shopping') {
    return Number(line.consumptionAmount || 0);
  }
  if (activeEditorType.value === 'optional') {
    return Number(line.costPrice || 0);
  }
  return Number(line.amount || Number(line.unitPrice || 0) * Number(line.quantity || 0));
}

function buildArrangementPayload(): SalesTeamApi.TeamArrangementSaveParams {
  const totalAmount = activeEditorTotalAmount.value;
  const priceLines = activeEditorType.value === 'ground_agent'
    ? [createGroundAgentPackagePriceLine(totalAmount, arrangementForm.priceRemark)]
    : arrangementForm.priceLines.map((line, index) => ({
      ...line,
      amount: priceLineAmountForSubmit(line),
      quantity: Number(line.quantity || 0),
      sortOrder: index + 1,
      unitPrice: Number(line.unitPrice || 0),
    }));
  return {
    allocationMode: arrangementForm.allocationMode,
    arrangementContent: arrangementForm.remark || arrangementForm.resourceName || arrangementForm.supplierName,
    arrangementId: editingArrangementId.value,
    arrangementType: activeEditorType.value,
    cashAmount: numericMoney(arrangementForm.cashAmount),
    companyRebateAmount: numericMoney(arrangementForm.companyRebateAmount),
    confirmed: arrangementForm.confirmed,
    confirmationNo: arrangementForm.confirmationNo,
    consumptionAmount: numericMoney(arrangementForm.consumptionAmount),
    costAmount: activeEditorType.value === 'optional'
      ? numericMoney(arrangementForm.costAmount)
      : totalAmount,
    creditAmount: editorCreditAmount.value,
    daysCount: Number(arrangementForm.daysCount || 0),
    departurePlace: arrangementForm.departurePlace || departureRegionPath.value?.join('/'),
    arrivalPlace: arrangementForm.arrivalPlace || arrivalRegionPath.value?.join('/'),
    driverName: arrangementForm.driverName,
    fundIncluded: arrangementForm.fundIncluded,
    guideId: arrangementForm.guideId,
    guideCommissionAmount: numericMoney(arrangementForm.guideCommissionAmount),
    guideName: arrangementForm.guideName,
    headFeeAmount: numericMoney(arrangementForm.headFeeAmount),
    itemName: arrangementForm.resourceName
      || arrangementForm.supplierName
      || arrangementForm.trafficType
      || defaultProjectName(activeEditorType.value),
    multiOrderSplitMode: arrangementForm.multiOrderSplitMode,
    mealType: arrangementForm.mealType,
    noGuideReport: arrangementForm.noGuideReport,
    peopleCount: numericMoney(arrangementForm.peopleCount),
    prepaidAmount: numericMoney(arrangementForm.prepaidAmount),
    priceLines,
    remark: arrangementForm.remark,
    resourceName: arrangementForm.resourceName,
    responsibleEmployeeId: arrangementForm.responsibleEmployeeId,
    responsibleEmployeeName: arrangementForm.responsibleEmployeeName,
    saleAmount: numericMoney(arrangementForm.saleAmount),
    scheduleEndDay: arrangementForm.scheduleEndDay,
    scheduleStartDay: arrangementForm.scheduleStartDay,
    selectedOrderIds: selectedOrderIdsForSubmit(),
    settlementType: arrangementForm.settlementType,
    supplierId: arrangementForm.supplierId,
    supplierName: arrangementForm.supplierName,
    totalAmount,
    trafficType: arrangementForm.trafficType,
    vehiclePlate: arrangementForm.vehiclePlate,
    vehicleType: arrangementForm.vehicleType,
  };
}

function activeArrangementLabel() {
  return arrangementShortcuts.find((item) => item.value === activeEditorType.value)?.label || '团队';
}

function validateArrangementDraft() {
  const payload = buildArrangementPayload();
  const type = activeEditorType.value;
  const requiresResource = ['hotel', 'meal', 'optional', 'scenic', 'shopping'].includes(type);
  if (!arrangementForm.trafficType) {
    if (type === 'traffic') {
      message.warning('请选择交通类型');
      return false;
    }
  }
  if (type === 'vehicle' && !arrangementForm.vehicleType) {
    message.warning('请选择车型');
    return false;
  }
  if (requiresResource && !arrangementForm.resourceName) {
    message.warning('请选择资源名称');
    return false;
  }
  if (!arrangementForm.scheduleStartDay) {
    message.warning('请选择日期');
    return false;
  }
  if (type === 'traffic' && (!payload.departurePlace || !payload.arrivalPlace)) {
    message.warning('请填写出发地和抵达地');
    return false;
  }
  if ((type === 'hotel' || type === 'vehicle' || type === 'ground_agent') && !arrangementForm.scheduleEndDay) {
    message.warning('请选择结束日期');
    return false;
  }
  if (!arrangementForm.supplierName) {
    message.warning('请选择供应商');
    return false;
  }
  if (type === 'ground_agent' && Number(arrangementForm.costAmount || 0) <= 0) {
    message.warning('请填写地接结算价');
    return false;
  }
  if (arrangementForm.noGuideReport && Number(arrangementForm.cashAmount || 0) > 0) {
    message.warning('已选择“无需导游报账”，现结金额须为0！');
    return false;
  }
  if (arrangementForm.allocationMode === 'multi_order_average') {
    if (arrangementForm.selectedOrderIds.length < 2) {
      message.warning('多订单均摊成本至少选择两个订单');
      return false;
    }
    if (type !== 'ground_agent' && arrangementForm.priceLines.length !== 1) {
      message.warning('多订单均摊成本时，价格信息只能保留一条记录');
      return false;
    }
  }
  return true;
}

async function submitArrangementDraft() {
  if (!validateArrangementDraft()) return;
  arrangementSaving.value = true;
  try {
    await saveTeamArrangement(teamId.value, buildArrangementPayload());
    if (activeEditorType.value === 'vehicle') {
      await Promise.all([
        recordVehicleHistoryUsage('driver_info', arrangementForm.driverName),
        recordVehicleHistoryUsage('vehicle_plate', arrangementForm.vehiclePlate),
      ]);
    }
    message.success(`${activeArrangementLabel()}安排已保存`);
    arrangementModalOpen.value = false;
    await loadTeamArrangements();
  } finally {
    arrangementSaving.value = false;
  }
}

async function loadTeamArrangements() {
  if (!teamId.value) return;
  teamArrangements.value = await getTeamArrangements(teamId.value);
}

async function removeArrangement(record: TeamArrangementRow) {
  if (!record.canDelete) {
    message.warning(record.deleteDisabledReason || '该安排已进入后续流程，不能直接删除');
    return;
  }
  Modal.confirm({
    content: `确认删除 ${record.itemName || '该安排'}？`,
    title: '删除团队安排',
    async onOk() {
      await deleteTeamArrangement(teamId.value, record.id);
      message.success('团队安排已删除');
      await loadTeamArrangements();
    },
  });
}

function goTeamOperation() {
  if (!team.value?.id) return;
  router.push(`/sales/team/operation/${team.value.id}`);
}

async function loadDetail() {
  if (!teamId.value) {
    router.push('/sales/team');
    return;
  }
  loading.value = true;
  try {
    const [operationDetail] = await Promise.all([
      getSalesTeamOperationDetail(teamId.value),
      loadTeamGuides(),
      loadTeamArrangements(),
    ]);
    detail.value = operationDetail;
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
</script>

<template>
  <Page :title="pageTitle">
    <Spin :spinning="loading">
      <Card class="team-arrangement-card formal-team-arrangement-card">
        <div class="formal-arrangement-tool-strip" aria-label="团队安排工具">
          <div class="formal-tool-strip-title">
            <IconifyIcon icon="lucide:wrench" />
            <span>团队工具</span>
          </div>
          <div class="formal-tool-strip-actions">
            <button
              v-for="action in arrangementToolActions"
              :key="action.label"
              type="button"
              class="formal-arrangement-tool-button"
              @click="handleToolAction(action)"
            >
              <IconifyIcon :icon="action.icon" />
              <span>{{ action.label }}</span>
            </button>
          </div>
        </div>

        <div class="arrangement-command-bar">
          <div class="command-main">
            <div class="form-kicker">团队安排总览 · Group Arrange</div>
            <div class="team-title-line formal-team-title-line">
              <div class="team-name">{{ teamDisplayName }}</div>
              <div class="team-badges formal-team-badges">
                <Tag color="blue">团队类型：{{ teamTypeLabel(team?.teamType) }}</Tag>
                <Tag
                  v-for="badge in teamBadges"
                  :key="badge.label"
                  :color="badge.color"
                  :class="{ editable: badge.editorType }"
                  @click="badge.editorType && openTeamProfileEditor(badge.editorType)"
                >
                  {{ badge.label }}：{{ badge.value }}
                </Tag>
              </div>
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
              <button type="button" class="compact-action" @click="goTeamOperation">
                <IconifyIcon icon="lucide:clipboard-list" />
                <span>团队管理</span>
              </button>
              <button type="button" class="compact-action" @click="showItineraryModal">
                <IconifyIcon icon="lucide:briefcase" />
                <span>查看行程</span>
              </button>
            </div>
          </div>

          <div class="team-metric-strip formal-team-metric-strip">
            <span
              v-for="item in metricItems"
              :key="item.key"
              class="team-metric-item clickable"
              :class="`metric-${item.key}`"
              @click="handleMetricClick(item)"
            >
              <em>{{ item.label }}</em>
              <strong>{{ item.value }}</strong>
            </span>
          </div>
        </div>

        <div class="team-note-row">
          <div class="internal-note-title">
            <div>
              <IconifyIcon icon="lucide:info" />
              <span>内部备注：{{ content?.internalRemark || '未填写' }}</span>
            </div>
            <Button type="link" size="small" @click="handleToolAction({ label: '编辑内部备注' })">
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

        <div class="formal-cost-overview-title">
          <div>
            <IconifyIcon icon="lucide:table-properties" />
            <span>成本总览</span>
          </div>
          <small>现结 / 挂账 / 收入摘要</small>
        </div>
        <div class="cost-overview-summary" aria-label="成本总览重点金额">
          <div
            v-for="item in costOverviewSummaryItems"
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
                  v-for="item in costColumns.slice(0, 10)"
                  :key="item.key"
                  colspan="2"
                >
                  {{ item.label }}
                </th>
              </tr>
              <tr>
                <template
                  v-for="item in costColumns.slice(0, 10)"
                  :key="`${item.key}-settlement`"
                >
                  <th>现结</th>
                  <th>挂账</th>
                </template>
              </tr>
            </thead>
            <tbody>
              <tr>
                <template
                  v-for="item in costColumns.slice(0, 10)"
                  :key="`${item.key}-amount`"
                >
                  <td :class="costAmountClass(item.cash)">{{ formatMoney(item.cash) }}</td>
                  <td :class="costAmountClass(item.credit)">{{ formatMoney(item.credit) }}</td>
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

        <section id="guide-arrangement" class="arrangement-section-card guide-arrangement-section">
          <div class="arrangement-section-header">
            <div class="arrangement-section-title">
              <IconifyIcon icon="lucide:badge" />
              <span>导游</span>
            </div>
            <div class="arrangement-section-actions">
              <Button size="small" @click="loadTeamGuides">刷新</Button>
              <Button size="small" type="primary" @click="openGuideModal">添加导游</Button>
            </div>
          </div>
          <div class="arrangement-section-state">
            已安排 {{ teamGuides.length }} 位导游，导服费 {{ formatMoney(guideFeeTotal) }}，操作费 {{ formatMoney(guideOperationFeeTotal) }}，备用金 {{ formatMoney(guideImprestTotal) }}
          </div>
          <div class="arrangement-section-table-wrap">
            <table class="arrangement-section-table guide-arrangement-table">
              <thead>
                <tr>
                  <th>导游</th>
                  <th>导服费</th>
                  <th>备用金</th>
                  <th>操作费</th>
                  <th>上团时间</th>
                  <th>下团时间</th>
                  <th>费用说明</th>
                  <th>导游备注</th>
                  <th>待定中</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!teamGuides.length">
                  <td colspan="10" class="arrangement-empty-cell">暂无导游安排</td>
                </tr>
                <tr v-for="record in teamGuides" :key="record.id">
                  <td>
                    <Select
                      :value="record.guideId"
                      class="guide-table-control"
                      show-search
                      :filter-option="false"
                      :options="guideSelectOptionsForRecord(record)"
                      @focus="loadGuideOptions"
                      @change="(value) => saveGuideField(record, 'guideId', value as number)"
                    />
                    <div class="guide-table-subtext">{{ record.guideMobile || '--' }}</div>
                  </td>
                  <td>
                    <InputNumber
                      v-model:value="record.guideFee"
                      class="guide-table-money"
                      :min="0"
                      @blur="() => saveGuideField(record, 'guideFee', record.guideFee)"
                    />
                  </td>
                  <td>
                    <InputNumber
                      v-model:value="record.imprestAmount"
                      class="guide-table-money"
                      :min="0"
                      @blur="() => saveGuideField(record, 'imprestAmount', record.imprestAmount)"
                    />
                  </td>
                  <td>
                    <InputNumber
                      v-model:value="record.operationFee"
                      class="guide-table-money"
                      :min="0"
                      @blur="() => saveGuideField(record, 'operationFee', record.operationFee)"
                    />
                  </td>
                  <td>
                    <DatePicker
                      class="guide-table-date"
                      format="YYYY-MM-DD HH:mm"
                      show-time
                      :value="dateTimeValue(record.startAt)"
                      @change="(value) => saveGuideDateField(record, 'startAt', value)"
                    />
                  </td>
                  <td>
                    <DatePicker
                      class="guide-table-date"
                      format="YYYY-MM-DD HH:mm"
                      show-time
                      :value="dateTimeValue(record.endAt)"
                      @change="(value) => saveGuideDateField(record, 'endAt', value)"
                    />
                  </td>
                  <td>
                    <Input
                      v-model:value="record.feeMemo"
                      class="guide-table-control"
                      @blur="() => saveGuideField(record, 'feeMemo', record.feeMemo)"
                    />
                  </td>
                  <td>
                    <Input
                      v-model:value="record.guideMemo"
                      class="guide-table-control"
                      @blur="() => saveGuideField(record, 'guideMemo', record.guideMemo)"
                    />
                  </td>
                  <td>
                    <Checkbox
                      :checked="record.tentative"
                      @change="(event) => saveGuideTentative(record, event.target.checked)"
                    >
                      待定中
                    </Checkbox>
                  </td>
                  <td>
                    <Button danger size="small" type="link" @click="removeTeamGuide(record)">删除</Button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <div class="arrangement-overview-sections">
          <section
            v-for="section in arrangementSections"
            :id="section.anchor"
            :key="section.value"
            class="arrangement-section-card"
          >
            <div class="arrangement-section-header">
              <div class="arrangement-section-heading">
                <div class="arrangement-section-title">
                  <IconifyIcon :icon="section.icon" />
                  <span>{{ section.label }}</span>
                  <span
                    class="arrangement-status-badge"
                    :class="`status-${sectionStatusTone(section.value)}`"
                  >
                    {{ sectionStatusText(section.value) }}
                  </span>
                  <Button
                    v-if="section.documentAction"
                    class="arrangement-document-button"
                    size="small"
                    type="primary"
                    @click="handleToolAction({ label: section.documentAction })"
                  >
                    <IconifyIcon icon="lucide:printer" />
                    <span>{{ section.documentAction }}</span>
                  </Button>
                </div>
                <div class="arrangement-section-summary">
                  <span class="arrangement-summary-chip">安排 {{ sectionSummary(section.value).count }} 条</span>
                  <span class="arrangement-summary-chip">成本 {{ formatMoney(sectionSummary(section.value).cost) }}</span>
                  <span class="arrangement-summary-chip">现结 {{ formatMoney(sectionSummary(section.value).cash) }}</span>
                  <span class="arrangement-summary-chip">挂账 {{ formatMoney(sectionSummary(section.value).credit) }}</span>
                </div>
              </div>
              <div class="arrangement-section-action-group">
                <Button size="small" @click="setSectionLocalState(section.value, 'none')">无需</Button>
                <Button size="small" @click="setSectionLocalState(section.value, 'done')">完成</Button>
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
                  <tr v-if="!arrangementsByType(section.value).length">
                    <td :colspan="section.columns.length" class="arrangement-empty-cell">暂无安排</td>
                  </tr>
                  <tr
                    v-for="record in arrangementsByType(section.value)"
                    :key="record.id"
                  >
                    <td
                      v-for="column in section.columns"
                      :key="`${section.value}-${record.id}-${column}`"
                    >
                      <template v-if="column === '操作'">
                        <Button
                          size="small"
                          type="link"
                          @click="editArrangement(record)"
                        >
                          修改
                        </Button>
                        <Button
                          danger
                          size="small"
                          type="link"
                          :disabled="!record.canDelete"
                          @click="removeArrangement(record)"
                        >
                          删除
                        </Button>
                      </template>
                      <template v-else>
                        {{ arrangementCell(record, section, column) }}
                      </template>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <div class="arrangement-footer-actions">
          <Button @click="router.push('/sales/team')">返回列表</Button>
          <Button @click="goTeamOperation">团队管理</Button>
          <Button type="primary" @click="submitArrangementDraft">保存团队安排</Button>
        </div>
      </Card>
    </Spin>

    <Modal
      v-model:open="quickProfileEditorOpen"
      centered
      destroy-on-close
      :title="activeQuickProfileEditor.title"
      :width="460"
      :footer="null"
    >
      <Spin :spinning="teamProfileOptionsLoading || quickProfileSaving">
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
            <Button type="primary" :loading="quickProfileSaving" @click="saveTeamProfileEditor">
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
      editorMode="team"
      :editor-total-amount="activeEditorTotalAmount"
      :editing-arrangement-index="editingArrangementId ? 0 : -1"
      :employee-options="employeeOptions"
      :form="arrangementForm"
      :last-vehicle-quote-result="lastVehicleQuoteResult"
      :options-loading="optionsLoading"
      :order-options="orderOptions"
      :project-options="projectOptions"
      :region-options="regionOptions"
      :resource-options="resourceOptions"
      :saving="arrangementSaving"
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
      @close="arrangementModalOpen = false"
      @download-scenic-ticket-guests="downloadScenicTicketGuests"
      @open-hotel-create-page="openArrangementRelationPage"
      @open-product-roadbook-editor="syncVehicleRoadbookDistance"
      @open-resource-create-page="openArrangementRelationPage"
      @open-scenic-template-config-page="openArrangementRelationPage"
      @open-supplier-create-page="openArrangementRelationPage"
      @remove-arrangement-price-line="removeArrangementPriceLine"
      @remove-optional-summary-line="removeOptionalSummaryLine"
      @remove-shopping-consumption-line="removeShoppingConsumptionLine"
      @save="submitArrangementDraft"
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

    <Modal
      v-model:open="guideModalOpen"
      destroy-on-close
      title="添加导游"
      width="720px"
      :confirm-loading="guideSaving"
      @ok="submitGuideDraft"
    >
      <Form layout="vertical" :model="guideDraft">
        <div class="editor-grid">
          <Form.Item label="导游" required>
            <Select
              v-model:value="guideDraft.guideId"
              show-search
              :filter-option="false"
              placeholder="选择导游"
              :options="guideSelectOptions"
            />
          </Form.Item>
          <Form.Item label="待定中">
            <Checkbox v-model:checked="guideDraft.tentative">待定中</Checkbox>
          </Form.Item>
          <Form.Item label="导服费">
            <InputNumber v-model:value="guideDraft.guideFee" class="w-full" :min="0" addon-before="¥" />
          </Form.Item>
          <Form.Item label="备用金">
            <InputNumber v-model:value="guideDraft.imprestAmount" class="w-full" :min="0" addon-before="¥" />
          </Form.Item>
          <Form.Item label="操作费">
            <InputNumber v-model:value="guideDraft.operationFee" class="w-full" :min="0" addon-before="¥" />
          </Form.Item>
          <Form.Item label="上团时间" required>
            <DatePicker
              v-model:value="guideDraftDates.startAt"
              class="w-full"
              format="YYYY-MM-DD HH:mm"
              show-time
            />
          </Form.Item>
          <Form.Item label="下团时间" required>
            <DatePicker
              v-model:value="guideDraftDates.endAt"
              class="w-full"
              format="YYYY-MM-DD HH:mm"
              show-time
            />
          </Form.Item>
        </div>
        <Form.Item label="费用说明">
          <Textarea v-model:value="guideDraft.feeMemo" :rows="3" :maxlength="1000" />
        </Form.Item>
        <Form.Item label="导游备注">
          <Textarea v-model:value="guideDraft.guideMemo" :rows="3" :maxlength="1000" />
        </Form.Item>
      </Form>
    </Modal>
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

.traffic-modal-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
  padding-top: 14px;
  margin-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.formal-arrangement-tool-strip {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 5px 7px;
  margin-bottom: 8px;
  background: #fbfdff;
  border: 1px solid #e4ecf7;
  border-radius: 6px;
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 90%);
}

.formal-tool-strip-title {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  min-height: 27px;
  padding: 0 6px 0 3px;
  font-size: 11.8px;
  font-weight: 800;
  color: #475569;
  white-space: nowrap;
  border-right: 1px solid #e2e8f0;
}

.formal-tool-strip-title svg,
.formal-arrangement-tool-button svg {
  width: 15px;
  height: 15px;
}

.formal-tool-strip-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 5px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: thin;
}

.formal-arrangement-tool-button {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 4px;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 8px;
  font-size: 11.5px;
  font-weight: 700;
  color: #334155;
  white-space: nowrap;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dbe5f2;
  border-radius: 5px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.formal-arrangement-tool-button:hover,
.formal-arrangement-tool-button:focus {
  color: var(--formal-blue-strong);
  background: #f3f8ff;
  border-color: #91caff;
  box-shadow: 0 2px 5px rgb(22 119 255 / 10%);
}

.formal-arrangement-tool-button svg {
  color: #1677ff;
}

.save-placeholder-alert {
  padding: 10px 12px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 800;
  color: #1554ad;
  background: #eef6ff;
  border: 1px solid #b7d7ff;
  border-radius: 6px;
}

.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}

.w-full {
  width: 100%;
}

.guide-arrangement-section {
  margin-bottom: 10px;
}

.guide-arrangement-table {
  min-width: 1520px;
}

.guide-arrangement-table th:nth-child(1),
.guide-arrangement-table td:nth-child(1) {
  width: 170px;
}

.guide-arrangement-table th:nth-child(2),
.guide-arrangement-table td:nth-child(2),
.guide-arrangement-table th:nth-child(3),
.guide-arrangement-table td:nth-child(3),
.guide-arrangement-table th:nth-child(4),
.guide-arrangement-table td:nth-child(4) {
  width: 116px;
}

.guide-arrangement-table th:nth-child(5),
.guide-arrangement-table td:nth-child(5),
.guide-arrangement-table th:nth-child(6),
.guide-arrangement-table td:nth-child(6) {
  width: 190px;
}

.guide-arrangement-table th:nth-child(9),
.guide-arrangement-table td:nth-child(9),
.guide-arrangement-table th:nth-child(10),
.guide-arrangement-table td:nth-child(10) {
  width: 92px;
}

.guide-table-control,
.guide-table-date {
  width: 100%;
}

.guide-table-money {
  width: 96px;
}

.guide-table-subtext {
  margin-top: 3px;
  overflow: hidden;
  font-size: 11px;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1080px) {
  .formal-team-arrangement-card :deep(.arrangement-command-bar) {
    grid-template-columns: 1fr;
  }

  .traffic-modal-footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
