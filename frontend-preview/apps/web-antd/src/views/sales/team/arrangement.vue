<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { DispatchGuideApi } from '#/api/dispatch/guide-schedule';
import type { EnterpriseGuideApi } from '#/api/enterprise/guide';
import type { GuideImprestApi } from '#/api/finance/guide-imprest';
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
  Table,
  Textarea,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  createTeamGuide,
  deleteTeamGuide,
  getGuideAvailability,
  getTeamGuides,
  updateTeamGuideField,
} from '#/api/dispatch/guide-schedule';
import {
  getGuideImprestDetail,
  getGuideImprestPage,
  previewGuideImprest,
  submitGuideImprest,
} from '#/api/finance/guide-imprest';
import {
  calculateVehicleQuote as calculateVehicleQuoteRule,
  getVehicleQuoteRuleAll,
} from '#/api/dispatch/vehicle-quote';
import { getExpenseItemAll } from '#/api/enterprise/expense-item';
import { getEnterpriseDepartmentAll } from '#/api/enterprise/department';
import { getEnterpriseEmployeeAll } from '#/api/enterprise/employee';
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
  getTeamArrangementSummary,
  getTeamArrangementSectionStatuses,
  saveSalesTeam,
  saveTeamArrangement,
  saveTeamArrangementSectionStatus,
} from '#/api/sales/team';
import {
  getVehicleUsageHistorySuggestions,
  recordVehicleUsageHistory,
} from '#/api/sales/product';
import { buildRegionOptions } from '#/utils/region';

import ArrangementEditorModal from '../components/ArrangementEditorModal.vue';
import FormalTeamPageHeader from '../components/FormalTeamPageHeader.vue';
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
  shouldFilterSupplierByResource,
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
type TeamArrangementSummary = SalesTeamApi.TeamArrangementSummary;

const DEFAULT_INTERNAL_REMARK_TEMPLATE = [
  '>导游要求：',
  '>控房要求：',
  '>控车要求：',
  '>用餐要求：',
  '>其它要求：',
].join('\n');

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

type QuickProfileEditorType = 'business_type' | 'department' | 'escort' | 'internal_note' | 'operator';

type TeamProfileDraft = {
  businessType?: string;
  departmentId?: number;
  departmentName?: string;
  escortEmployeeName?: string;
  internalRemark?: string;
  operatorEmployeeId?: number;
  operatorEmployeeName?: string;
  optionalMarkupRate?: number;
  perCapitaPitAmount?: number;
  perCapitaShoppingAmount?: number;
};

type QuickProfileEditorField =
  | 'businessType'
  | 'departmentId'
  | 'escortEmployeeName'
  | 'internalRemark'
  | 'operatorEmployeeId';

type GuideDraft = Omit<DispatchGuideApi.TeamGuideSaveParams, 'guideId'> & {
  guideId?: number;
};

type GuideEditDraft = {
  endAt?: DatePickerValue;
  feeMemo?: string;
  guideFee?: number;
  guideMemo?: string;
  imprestAmount?: number;
  operationFee?: number;
  startAt?: DatePickerValue;
  tentative?: boolean;
};

type GuidePickerTabKey = 'all' | 'available';

type GuidePickerTab = {
  key: GuidePickerTabKey;
  label: string;
};

type TeamBadgeItem = {
  color?: string;
  editorType?: QuickProfileEditorType;
  label: string;
  value: string;
};

type QuickProfileEditorConfig = {
  buttonText: string;
  field: QuickProfileEditorField;
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
  internal_note: {
    buttonText: '保存信息',
    field: 'internalRemark',
    inputType: 'textarea',
    label: '内部备注',
    placeholder: '填写导游、控房、控车、用餐和其它要求',
    title: '内部备注',
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
const guideModalEditingRecord = ref<TeamGuideRow>();
const guideEditOpen = ref(false);
const guideEditSaving = ref(false);
const guideEditRecord = ref<TeamGuideRow>();
const guideImprestModalOpen = ref(false);
const guideImprestLoading = ref(false);
const guideImprestSubmitting = ref(false);
const guideImprestCurrentRecord = ref<TeamGuideRow>();
const guideImprestPreview = ref<GuideImprestApi.Preview>();
const guideImprestApplyRemark = ref('');
const guideImprestCompanyMarkupRate = ref<number | undefined>();
const guideImprestRequestedAmount = ref<number | undefined>();
const guideImprestRecordsOpen = ref(false);
const guideImprestRecordsLoading = ref(false);
const guideImprestRecordDetailOpen = ref(false);
const guideImprestRecordDetailLoading = ref(false);
const guideImprestRecordsCurrentRecord = ref<TeamGuideRow>();
const guideImprestRecordRows = ref<GuideImprestApi.Imprest[]>([]);
const guideImprestRecordDetail = ref<GuideImprestApi.Imprest>();
const quickProfileEditorOpen = ref(false);
const quickProfileEditorType = ref<QuickProfileEditorType>('business_type');
const quickProfileSaving = ref(false);
const teamProfileOptionsLoading = ref(false);
const vehicleQuoteCalculating = ref(false);
const activeEditorType = ref<ArrangementType>('traffic');
const guidePickerActiveTab = ref<GuidePickerTabKey>('available');
const guidePickerKeyword = ref('');
const guideAvailabilityLoading = ref(false);
const sectionLocalStates = reactive<Record<string, SalesTeamApi.TeamArrangementSectionStatus | undefined>>({});
const detail = ref<OperationDetail>();
const teamGuides = ref<TeamGuideRow[]>([]);
const teamArrangements = ref<TeamArrangementRow[]>([]);
const teamArrangementSummary = ref<TeamArrangementSummary>();
const guideAvailabilityRows = ref<DispatchGuideApi.GuideAvailability[]>([]);
const guideAvailabilityTotal = ref(0);
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
const guideDraft = reactive<GuideDraft>({
  endAt: '',
  feeMemo: '',
  guideFee: 0,
  guideId: undefined,
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
const guideEditDraft = reactive<GuideEditDraft>({
  endAt: undefined,
  feeMemo: '',
  guideFee: 0,
  guideMemo: '',
  imprestAmount: 0,
  operationFee: 0,
  startAt: undefined,
  tentative: false,
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

const guidePickerTabs: GuidePickerTab[] = [
  { key: 'available', label: '可用导游' },
  { key: 'all', label: '全部导游' },
];

const teamId = computed(() => Number(route.params.id || 0));
const team = computed(() => detail.value?.team);
const product = computed(() => detail.value?.product);
const content = computed(() => detail.value?.content);
const teamItineraryDays = computed(() => detail.value?.itineraryDays || []);
const orders = computed(() => detail.value?.orders || []);
const orderOptions = computed<SelectOption[]>(() => {
  const uniqueOptions = new Map<string, SelectOption>();
  const addOption = (value?: number | string, label?: string) => {
    if (value === undefined || value === null) return;
    const key = String(value);
    if (!uniqueOptions.has(key)) {
      uniqueOptions.set(key, { label: label || `订单${key}`, value: key });
    }
  };
  orders.value
    .filter((item) => item.orderRole !== 'merge_source')
    .forEach((item) => {
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
      addOption(item.id, label);
      (item.sourceOrderInfos || []).forEach((source) => {
        addOption(source.orderId, [
          '来源订单',
          source.summary,
          source.orderId ? `订单${source.orderId}` : '',
          guestText,
        ].filter(Boolean).join(' / '));
      });
    });
  return [...uniqueOptions.values()];
});
const pageTitle = computed(() => (team.value?.teamNo ? `团队安排 - ${team.value.teamNo}` : '团队安排'));
const teamDisplayName = computed(() => product.value?.productName || team.value?.teamNo || '--');
const orderReceivable = computed(() => numericMoney(teamArrangementSummary.value?.orderReceivableAmount));
const orderReceived = computed(() => numericMoney(teamArrangementSummary.value?.orderReceivedAmount));
const orderBalance = computed(() => numericMoney(teamArrangementSummary.value?.orderBalanceAmount));
const orderStatusSummary = computed(() => {
  const confirmed = orders.value.filter((item) => isOrderStatus(item, ['已确认', 'confirmed'])).length;
  const pending = orders.value.filter((item) => isOrderStatus(item, ['未处理', 'pending'])).length;
  const cancelled = orders.value.filter((item) => isOrderStatus(item, ['已取消', 'cancelled'])).length;
  return `${confirmed} | ${pending} | ${cancelled}`;
});
const guideModalEditing = computed(() => !!guideModalEditingRecord.value);
const selectedGuideDraft = computed(() => (
  guideAvailabilityRows.value.find((item) => item.guideId === guideDraft.guideId)
  || (
    guideModalEditingRecord.value?.guideId === guideDraft.guideId
      ? guideModalEditingRecord.value
      : undefined
  )
));
const selectedScenicResourceRelation = computed(() => (
  resourceRelationOptions.value.find((item) => (
    item.resourceName === arrangementForm.resourceName
    && item.supplierId === arrangementForm.supplierId
  ))
));
const guideFeeTotal = computed(() => numericMoney(teamArrangementSummary.value?.guideFeeAmount));
const guideOperationFeeTotal = computed(() => numericMoney(teamArrangementSummary.value?.guideOperationFeeAmount));
const guideImprestTotal = computed(() => numericMoney(teamArrangementSummary.value?.guideImprestAmount));
const guideApprovedImprestTotal = computed(() => teamGuides.value.reduce(
  (sum, item) => sum + numericMoney(item.approvedImprestAmount),
  0,
));
const guidePendingImprestTotal = computed(() => teamGuides.value.reduce(
  (sum, item) => sum + numericMoney(item.pendingImprestAmount),
  0,
));
const budgetProfitAmount = computed(() => numericMoney(teamArrangementSummary.value?.budgetProfitAmount));
const teamTravelDays = computed(() => Math.max(Number(team.value?.travelDays || 1), 1));

/** 只接受完整 ISO 日期，避免把“第1天”等历史相对日期误当作真实日期。 */
function parseScheduleIsoDate(value?: string) {
  const normalized = value?.trim();
  if (!normalized || !/^\d{4}-\d{2}-\d{2}$/.test(normalized)) return undefined;
  const parsed = dayjs(normalized);
  return parsed.isValid() && parsed.format('YYYY-MM-DD') === normalized ? parsed : undefined;
}

function teamDepartureScheduleDate() {
  return parseScheduleIsoDate(team.value?.departureDate);
}

/** 将真实日期或历史“第N天”统一换算为团队第几天，供路书和天数计算使用。 */
function scheduleDayNo(value?: string) {
  const date = parseScheduleIsoDate(value);
  const departureDate = teamDepartureScheduleDate();
  if (date && departureDate) {
    const dayNo = date.diff(departureDate, 'day') + 1;
    return dayNo >= 1 ? dayNo : undefined;
  }
  return parseScheduleDayNo(value);
}

/** 酒店按退房日减入住日计算，兼容 Word 导入的真实日期和历史相对日期。 */
function scheduleNightsCount(startValue?: string, endValue?: string) {
  const startDate = parseScheduleIsoDate(startValue);
  const endDate = parseScheduleIsoDate(endValue);
  if (startDate && endDate) {
    return Math.max(0, endDate.diff(startDate, 'day'));
  }
  const start = scheduleDayNo(startValue) || 1;
  const end = Math.max(start, scheduleDayNo(endValue) || start + 1);
  return Math.max(0, end - start);
}

/** 用车和地接按首尾日期都计入天数，兼容 Word 导入的真实日期和历史相对日期。 */
function scheduleDaysCount(startValue?: string, endValue?: string) {
  const startDate = parseScheduleIsoDate(startValue);
  const endDate = parseScheduleIsoDate(endValue);
  if (startDate && endDate) {
    return Math.max(1, endDate.diff(startDate, 'day') + 1);
  }
  const start = scheduleDayNo(startValue) || 1;
  const end = Math.max(start, scheduleDayNo(endValue) || start);
  return Math.max(1, end - start + 1);
}

/** 历史安排仍可能保存“第N天”；下拉同时提供历史和真实日期，编辑时不丢失原值。 */
const scheduleDayOptions = computed<SelectOption[]>(() => {
  const options = new Map<string, SelectOption>();
  const addOption = (value: string, label: string) => {
    if (!options.has(value)) options.set(value, { label, value });
  };
  addOption('=出发日期=', '=出发日期=');
  const departureDate = teamDepartureScheduleDate();
  if (departureDate) {
    Array.from({ length: teamTravelDays.value }, (_, index) => {
      const dayNo = index + 1;
      const value = departureDate.add(index, 'day').format('YYYY-MM-DD');
      addOption(value, `${value}（第${dayNo}天）`);
    });
  }
  Array.from({ length: teamTravelDays.value }, (_, index) => {
    const dayNo = index + 1;
    addOption(`第${dayNo}天`, `第${dayNo}天（历史）`);
  });
  // 旧数据可能超出当前行程天数，仍应保留在编辑下拉中，不能因前端选项缺失而被误改。
  [arrangementForm.scheduleStartDay, arrangementForm.scheduleEndDay]
    .filter((value): value is string => Boolean(value?.trim()))
    .forEach((value) => {
      const dayNo = scheduleDayNo(value);
      addOption(value, parseScheduleIsoDate(value)
        ? `${value}${dayNo ? `（第${dayNo}天）` : ''}`
        : parseScheduleDayNo(value) ? `${value}（历史）` : value);
    });
  return [...options.values()];
});
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
  return (teamArrangementSummary.value?.costColumns || []).map((item) => ({
    cash: numericMoney(item.cashAmount),
    credit: numericMoney(item.creditAmount),
    key: item.key,
    label: item.label,
  }));
});
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
  { key: 'profit', label: '预算利润', value: formatPlainMoney(budgetProfitAmount.value) },
]);
const formalHeaderMetrics = computed(() => metricItems.value.map((item) => ({
  ...item,
  clickable: item.key === 'profit',
})));
const arrangedGuideSummary = computed(() => {
  const summaries = teamGuides.value.map((item) => {
    const name = item.guideName?.trim();
    if (!name) return '';
    const mobile = item.guideMobile?.trim();
    return mobile ? `${name}[Tel:${mobile}]` : name;
  }).filter(Boolean);
  return summaries.length > 0 ? summaries.join('、') : team.value?.guideSummary || '--';
});
const teamBadges = computed<TeamBadgeItem[]>(() => [
  { color: 'orange', editorType: 'business_type', label: '业务类型', value: team.value?.businessType || product.value?.businessType || '未设置' },
  { color: 'blue', editorType: 'department', label: '部门', value: team.value?.departmentName || '未设置' },
  { color: 'blue', editorType: 'operator', label: '操作计调', value: team.value?.operatorEmployeeName || '未设置' },
  { color: 'orange', label: '导游', value: arrangedGuideSummary.value },
  { color: 'green', label: '领队', value: team.value?.leaderSummary || '--' },
  { color: 'default', editorType: 'escort', label: '全陪', value: team.value?.escortEmployeeName || team.value?.escortSummary || '未设置' },
]);
const formalHeaderBadges = computed(() => [
  {
    color: 'blue',
    key: 'team_type',
    label: '团队类型',
    value: teamTypeLabel(team.value?.teamType),
  },
  ...teamBadges.value.map((badge) => ({
    color: badge.color,
    editable: Boolean(badge.editorType),
    key: badge.label,
    label: badge.label,
    value: badge.value,
  })),
]);
const formalHeaderActions = computed(() => [
  { icon: 'lucide:clipboard-list', key: 'teamOperation', label: '团队管理' },
  { icon: 'lucide:briefcase', key: 'itinerary', label: '查看行程' },
]);
const formalHeaderToolActions = computed(() => arrangementToolActions.map((action) => ({
  icon: action.icon,
  key: action.label,
  label: action.label,
})));
const formalHeaderNoteMetrics = computed(() => [
  `人均坑位：${formatMoney(content.value?.perCapitaPitAmount)}`,
  `自费加点率：${Number(content.value?.optionalMarkupRate || 0).toFixed(0)}%`,
  `人均购物：${formatMoney(content.value?.perCapitaShoppingAmount)}`,
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

function formatMoney(value?: number | string) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function formatCostCashMoney(value?: number | string) {
  return formatMoney(value);
}

function formatCostDetailMoney(value?: number | string) {
  return formatMoney(value);
}

function costAmountClass(value?: number | string, extraClass?: string) {
  const classes = [numericMoney(value) === 0 ? 'cost-amount-zero' : 'cost-amount-nonzero'];
  if (extraClass) {
    classes.push(extraClass);
  }
  return classes;
}

function formatDateTime(value?: string) {
  if (!value) return '--';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : value;
}

function formatPlainMoney(value?: number | string) {
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

function numericMoney(value?: number | string) {
  const result = Number(value || 0);
  return Number.isFinite(result) ? result : 0;
}

function arrangementsByType(type: ArrangementType) {
  return teamArrangements.value.filter((item) => item.arrangementType === type);
}

function summaryByType(type: ArrangementType) {
  return teamArrangementSummary.value?.sectionSummaries?.find((item) => item.arrangementType === type);
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
  const summary = summaryByType(type);
  return {
    cash: numericMoney(summary?.cashAmount),
    cost: numericMoney(summary?.costAmount),
    count: summary?.count || 0,
    credit: numericMoney(summary?.creditAmount),
  };
}

function costColumnAmount(key: string, field: 'cash' | 'credit') {
  return costColumns.value.find((item) => item.key === key)?.[field] || 0;
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
    internalRemark: content.value?.internalRemark || DEFAULT_INTERNAL_REMARK_TEMPLATE,
    operatorEmployeeId: team.value?.operatorEmployeeId,
    operatorEmployeeName: team.value?.operatorEmployeeName,
    optionalMarkupRate: Number(content.value?.optionalMarkupRate ?? 0),
    perCapitaPitAmount: Number(content.value?.perCapitaPitAmount ?? 0),
    perCapitaShoppingAmount: Number(content.value?.perCapitaShoppingAmount ?? 0),
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
  if (editorType === 'internal_note') {
    return {
      optionalMarkupRate: teamProfileDraft.optionalMarkupRate,
      perCapitaPitAmount: teamProfileDraft.perCapitaPitAmount,
      perCapitaShoppingAmount: teamProfileDraft.perCapitaShoppingAmount,
      remark: String(teamProfileDraft.internalRemark || '').trim(),
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
      if (quantity <= 0) {
        return `${name} 参考单价 ¥${unitPrice}`;
      }
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
      return Number(record.peopleCount || 0) > 0 ? String(record.peopleCount) : '--';
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

function guideGenderText(gender?: EnterpriseGuideApi.Gender | string) {
  if (gender === 'male') return '男';
  if (gender === 'female') return '女';
  return '未填';
}

function guideDisplayPhone(guide?: DispatchGuideApi.GuideAvailability | EnterpriseGuideApi.Item | TeamGuideRow) {
  if (!guide) return '--';
  if ('guideMobile' in guide) return guide.guideMobile || '--';
  return (guide as EnterpriseGuideApi.Item).mobilePhone
    || (guide as EnterpriseGuideApi.Item).telephone
    || '--';
}

function selectGuideForDraft(item: DispatchGuideApi.GuideAvailability) {
  if (!item.available) return;
  guideDraft.guideId = item.guideId;
}

function isOrderStatus(order: OperationOrderRow, statuses: string[]) {
  return statuses.includes(order.status || '');
}

function scrollToArrangementAnchor(anchor: string) {
  document.getElementById(anchor)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function scrollToRouteHashAnchor() {
  const anchor = route.hash?.replace('#', '');
  if (!anchor) return;
  await nextTick();
  requestAnimationFrame(() => scrollToArrangementAnchor(anchor));
}

async function setSectionLocalState(type: ArrangementType, state: Exclude<SalesTeamApi.TeamArrangementSectionStatus, 'pending'>) {
  if (!teamId.value) return;
  const saved = await saveTeamArrangementSectionStatus(teamId.value, type, state);
  sectionLocalStates[type] = saved.status;
  message.success(state === 'done' ? '分类状态已标记为完成' : '分类状态已标记为无需');
}

async function openArrangementEditor(type: ArrangementType) {
  activeEditorType.value = type;
  editingArrangementId.value = undefined;
  Object.assign(arrangementForm, createDefaultArrangementEditorForm(type));
  if (type === 'scenic') {
    prefillScenicTicketQuantity();
  }
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
      shouldFilterSupplierByResource(type)
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
    if (shouldFilterSupplierByResource(type)) {
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
  const relationResourceTypeMap: Partial<Record<ArrangementType, 'hotel' | 'restaurant' | 'scenic' | 'shopping'>> = {
    hotel: 'hotel',
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

/** 新增景区安排时按当前未取消订单人数预填门票数量，计调仍可按免票等情况调整。 */
function prefillScenicTicketQuantity() {
  const receivedGuestCount = orders.value
    .filter((order) => !isOrderStatus(order, ['已取消', 'cancelled']))
    .reduce((sum, order) => sum + Math.max(0, Number(order.guestCount || 0)), 0);
  const firstLine = arrangementForm.priceLines[0];
  if (!firstLine) return;
  firstLine.quantity = receivedGuestCount;
  syncPrimaryPriceFields();
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
    shouldFilterSupplierByResource(activeEditorType.value)
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
  arrangementForm.daysCount = scheduleNightsCount(
    arrangementForm.scheduleStartDay,
    arrangementForm.scheduleEndDay,
  );
}

function syncVehicleDaysCount() {
  const days = scheduleDaysCount(
    arrangementForm.scheduleStartDay,
    arrangementForm.scheduleEndDay,
  );
  arrangementForm.daysCount = days;
}

function selectedVehicleDayRange() {
  const start = scheduleDayNo(arrangementForm.scheduleStartDay) || 1;
  const end = scheduleDayNo(arrangementForm.scheduleEndDay) || start;
  return {
    end: Math.max(start, end),
    start,
  };
}

function syncGroundAgentDaysCount() {
  const days = scheduleDaysCount(
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
      path: '/purchase/resource',
      query: {
        openTemplate: '1',
        relationId: selectedScenicResourceRelation.value?.relationId,
        resourceId: selectedScenicResourceRelation.value?.resourceId,
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
    guideId: undefined,
    guideMemo: '',
    imprestAmount: 0,
    operationFee: 0,
    startAt: '',
    tentative: false,
  });
  guidePickerActiveTab.value = 'available';
  guidePickerKeyword.value = '';
  guideDraftDates.startAt = defaultGuideStartAt();
  guideDraftDates.endAt = defaultGuideEndAt();
}

function guideRemarkSummary(record: TeamGuideRow) {
  const parts = [
    record.feeMemo ? `费用：${record.feeMemo}` : '',
    record.guideMemo ? `备注：${record.guideMemo}` : '',
  ].filter(Boolean);
  return parts.length ? parts.join('；') : '--';
}

function resetGuideEditDraft(record: TeamGuideRow) {
  Object.assign(guideEditDraft, {
    endAt: dateTimeValue(record.endAt),
    feeMemo: record.feeMemo || '',
    guideFee: numericMoney(record.guideFee),
    guideMemo: record.guideMemo || '',
    imprestAmount: numericMoney(record.imprestAmount),
    operationFee: numericMoney(record.operationFee),
    startAt: dateTimeValue(record.startAt),
    tentative: !!record.tentative,
  });
}

function openGuideEditModal(record: TeamGuideRow) {
  guideEditRecord.value = record;
  resetGuideEditDraft(record);
  guideEditOpen.value = true;
}

async function submitGuideEditDraft() {
  const record = guideEditRecord.value;
  if (!record) return;
  const startAt = formatBackendDateTime(guideEditDraft.startAt);
  const endAt = formatBackendDateTime(guideEditDraft.endAt);
  if (!startAt || !endAt) {
    message.warning('请填写上团和下团时间');
    return;
  }
  const fields = [
    ['guideFee', guideEditDraft.guideFee],
    ['operationFee', guideEditDraft.operationFee],
    ['startAt', startAt],
    ['endAt', endAt],
    ['feeMemo', guideEditDraft.feeMemo?.trim()],
    ['guideMemo', guideEditDraft.guideMemo?.trim()],
    ['tentative', guideEditDraft.tentative],
  ] as Array<[string, boolean | number | string | undefined]>;
  guideEditSaving.value = true;
  try {
    let latest = record;
    for (const [field, value] of fields) {
      latest = await updateTeamGuideField(teamId.value, record.id, {
        field,
        value: value === undefined || value === null ? '' : String(value),
      });
    }
    const index = teamGuides.value.findIndex((item) => item.id === record.id);
    if (index >= 0) {
      teamGuides.value[index] = latest;
    }
    guideEditOpen.value = false;
    await loadArrangementSummary();
    message.success('导游安排已保存');
  } finally {
    guideEditSaving.value = false;
  }
}

async function loadGuideAvailability() {
  const startAt = formatBackendDateTime(guideDraftDates.startAt);
  const endAt = formatBackendDateTime(guideDraftDates.endAt);
  if (!startAt || !endAt) return;
  guideAvailabilityLoading.value = true;
  try {
    const result = await getGuideAvailability({
      availableOnly: guidePickerActiveTab.value === 'available',
      endAt,
      keyword: guidePickerKeyword.value.trim() || undefined,
      page: 1,
      pageSize: 50,
      startAt,
    });
    guideAvailabilityRows.value = result.items || [];
    guideAvailabilityTotal.value = Number(result.total || 0);
    const selectedRow = guideAvailabilityRows.value.find((item) => item.guideId === guideDraft.guideId);
    const selectedIsCurrentEditingGuide = guideModalEditingRecord.value?.guideId === guideDraft.guideId;
    if (selectedRow && !selectedRow.available && !selectedIsCurrentEditingGuide) {
      guideDraft.guideId = undefined;
    }
  } finally {
    guideAvailabilityLoading.value = false;
  }
}

async function setGuidePickerTab(tab: GuidePickerTabKey) {
  guidePickerActiveTab.value = tab;
  await loadGuideAvailability();
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
  guideModalEditingRecord.value = undefined;
  resetGuideDraft();
  await loadGuideAvailability();
  guideModalOpen.value = true;
}

async function openGuidePickerForRow(record: TeamGuideRow) {
  guideModalEditingRecord.value = record;
  resetGuideDraft();
  Object.assign(guideDraft, {
    guideId: record.guideId,
  });
  guideDraftDates.startAt = dateTimeValue(record.startAt) || defaultGuideStartAt();
  guideDraftDates.endAt = dateTimeValue(record.endAt) || defaultGuideEndAt();
  await loadGuideAvailability();
  guideModalOpen.value = true;
}

async function submitGuideDraft() {
  const selectedGuideId = guideDraft.guideId;
  if (!selectedGuideId) {
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
    if (guideModalEditingRecord.value) {
      const result = await updateTeamGuideField(teamId.value, guideModalEditingRecord.value.id, {
        field: 'guideId',
        value: String(selectedGuideId),
      });
      const index = teamGuides.value.findIndex((item) => item.id === result.id);
      if (index >= 0) {
        teamGuides.value[index] = result;
      }
      message.success('导游已更换');
    } else {
      await createTeamGuide(teamId.value, {
        ...guideDraft,
        feeMemo: guideDraft.feeMemo?.trim(),
        guideId: selectedGuideId,
        guideMemo: guideDraft.guideMemo?.trim(),
      });
      message.success('导游安排已保存，可点击“备用金测算”继续');
    }
    guideModalOpen.value = false;
    await loadTeamGuides();
    await loadArrangementSummary();
  } finally {
    guideSaving.value = false;
  }
}

function guideImprestEntryTooltip() {
  if (!teamGuides.value.length) return '请先添加导游后再测算备用金';
  if (teamGuides.value.length > 1) return '本团有多位导游，请在对应导游行测算备用金';
  return '根据团队现付成本和自费抵扣测算备用金';
}

async function openGuideImprestEntry() {
  if (!teamGuides.value.length) return;
  if (teamGuides.value.length > 1) {
    message.info('本团有多位导游，请在对应导游行点击“备用金测算”');
    return;
  }
  await openGuideImprestCalculator(teamGuides.value[0]!);
}

async function saveGuideField(row: TeamGuideRow, field: string, value?: number | string | boolean) {
  const stringValue = value === undefined || value === null ? '' : String(value);
  const result = await updateTeamGuideField(teamId.value, row.id, { field, value: stringValue });
  const index = teamGuides.value.findIndex((item) => item.id === row.id);
  if (index >= 0) {
    teamGuides.value[index] = result;
  }
  await loadArrangementSummary();
  message.success('已保存');
}

function guideImprestLineTypeLabel(type?: string) {
  if (type === 'cash_cost') return '现付成本';
  if (type === 'optional_deduction') return '自费抵扣';
  return '明细';
}

function formatPercent(value?: number) {
  if (value === undefined || value === null) return '--';
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return '--';
  return `${(Math.abs(numeric) <= 1 ? numeric * 100 : numeric).toFixed(0)}%`;
}

function guideImprestApprovalStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    approved_unpaid: '已批未付',
    none: '未申请',
    paid: '已付清',
    partial_paid: '部分付款',
    pending: '待审批',
  };
  return labels[status || 'none'] || status || '未申请';
}

function guideImprestApprovalStatusTone(status?: string) {
  const tones: Record<string, string> = {
    approved_unpaid: 'blue',
    none: 'default',
    paid: 'green',
    partial_paid: 'cyan',
    pending: 'orange',
  };
  return tones[status || 'none'] || 'default';
}

function guideImprestRequestStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    cancelled: '已作废',
    draft: '草稿',
    manager_approved: '总经理已同意',
    manager_rejected: '总经理已拒绝',
    paid: '已付款',
    pending_manager: '待总经理审批',
    settled: '已结算',
  };
  return labels[status || ''] || status || '--';
}

function guideImprestRequestStatusTone(status?: string) {
  const tones: Record<string, string> = {
    cancelled: 'default',
    draft: 'default',
    manager_approved: 'blue',
    manager_rejected: 'red',
    paid: 'green',
    pending_manager: 'orange',
    settled: 'cyan',
  };
  return tones[status || ''] || 'default';
}

function guideImprestRequestedAmountInvalid() {
  if (!guideImprestPreview.value) {
    return false;
  }
  const amount = Number(guideImprestRequestedAmount.value);
  return !Number.isFinite(amount) || amount <= 0;
}

function guideImprestAfterApplyAmount(preview?: GuideImprestApi.Preview) {
  if (!preview) return 0;
  return numericMoney(preview.occupiedAuthorizationAmount) + numericMoney(guideImprestRequestedAmount.value);
}

function guideImprestNeedsEmergencyRemark(preview?: GuideImprestApi.Preview) {
  if (!preview) return false;
  return guideImprestAfterApplyAmount(preview) > numericMoney(preview.suggestedImprestAmount);
}

function guideImprestRemarkMissing() {
  return guideImprestNeedsEmergencyRemark(guideImprestPreview.value)
    && !guideImprestApplyRemark.value.trim();
}

function guideImprestLineFormula(line: GuideImprestApi.CalcLine) {
  if (line.lineType === 'cash_cost') {
    return `现付成本直接计入备用金需求：${formatMoney(line.amount)}`;
  }
  if (line.lineType === 'optional_deduction') {
    const salePrice = Number(line.salePrice || 0);
    const costPrice = Number(line.costPrice || 0);
    const guideCommission = Number(line.guideCommissionAmount || 0);
    const guestCount = Number(line.guestCount || 0);
    const commissionTip = line.guideCommissionCalcType === 'percent'
      ? `导游提成按比例折算为 ${formatMoney(guideCommission)}/人，`
      : '';
    return `${commissionTip}(${formatMoney(salePrice)} - ${formatMoney(costPrice)} - ${formatMoney(guideCommission)}) × ${formatPercent(line.companyMarkupRate)} × ${guestCount}人 = ${formatMoney(line.amount)}`;
  }
  return `本行金额：${formatMoney(line.amount)}`;
}

function guideImprestSummaryFormula(preview?: GuideImprestApi.Preview) {
  if (!preview) return '';
  const cashCostAmount = Number(preview.cashCostAmount || 0);
  const optionalDeductionAmount = Number(preview.optionalDeductionAmount || 0);
  const calculatedAmount = cashCostAmount - optionalDeductionAmount;
  if (calculatedAmount >= 0) {
    return `建议备用金 = 现付总成本 ${formatMoney(cashCostAmount)} - 自费抵扣 ${formatMoney(optionalDeductionAmount)} = ${formatMoney(preview.suggestedImprestAmount)}`;
  }
  return `现付总成本 ${formatMoney(cashCostAmount)} - 自费抵扣 ${formatMoney(optionalDeductionAmount)} = -${formatMoney(Math.abs(calculatedAmount))}，所以建议备用金为 ${formatMoney(0)}，导游应上交 ${formatMoney(preview.guideTurnInAmount)}`;
}

function guideImprestReferenceBalanceFormula(preview?: GuideImprestApi.Preview) {
  if (!preview) return '';
  return `建议余额参考 = 建议备用金 ${formatMoney(preview.suggestedImprestAmount)} - 已申请/已批 ${formatMoney(preview.occupiedAuthorizationAmount)} = ${formatMoney(preview.availableAuthorizationAmount)}，仅作风险提示，不限制本次申请`;
}

function guideImprestEmergencyWarning(preview?: GuideImprestApi.Preview) {
  if (!guideImprestNeedsEmergencyRemark(preview)) {
    return '';
  }
  return `本次申请后累计备用金 ${formatMoney(guideImprestAfterApplyAmount(preview))} 已超过系统建议金额 ${formatMoney(preview?.suggestedImprestAmount)}，请在申请备注说明应急或特殊项目原因。`;
}

function teamOptionalMarkupRateAnchor() {
  const rate = numericMoney(content.value?.optionalMarkupRate);
  return rate > 0 ? rate : undefined;
}

async function openGuideImprestCalculator(record: TeamGuideRow) {
  if (!teamId.value || !record.guideId) {
    message.warning('请先选择导游');
    return;
  }
  guideImprestCurrentRecord.value = record;
  guideImprestPreview.value = undefined;
  guideImprestApplyRemark.value = '';
  guideImprestCompanyMarkupRate.value = teamOptionalMarkupRateAnchor();
  guideImprestRequestedAmount.value = undefined;
  guideImprestModalOpen.value = true;
  await refreshGuideImprestPreview();
}

async function refreshGuideImprestPreview() {
  const currentRecord = guideImprestCurrentRecord.value;
  if (!teamId.value || !currentRecord?.guideId) {
    message.warning('请先选择导游');
    return;
  }
  guideImprestLoading.value = true;
  try {
    const preview = await previewGuideImprest({
      companyMarkupRate: guideImprestCompanyMarkupRate.value,
      guideId: currentRecord.guideId,
      teamId: teamId.value,
    });
    guideImprestPreview.value = preview;
    guideImprestCompanyMarkupRate.value = numericMoney(
      preview.companyMarkupRate ?? guideImprestCompanyMarkupRate.value,
    );
    guideImprestRequestedAmount.value = numericMoney(
      preview.suggestedImprestAmount,
    );
  } finally {
    guideImprestLoading.value = false;
  }
}

async function useGuideImprestSuggestedAmount() {
  const currentRecord = guideImprestCurrentRecord.value;
  const preview = guideImprestPreview.value;
  if (!currentRecord || !preview) {
    message.warning('请先计算备用金');
    return;
  }
  const suggestedAmount = numericMoney(preview.suggestedImprestAmount);
  currentRecord.imprestAmount = suggestedAmount;
  await saveGuideField(currentRecord, 'imprestAmount', suggestedAmount);
}

async function submitGuideImprestApplication() {
  const currentRecord = guideImprestCurrentRecord.value;
  const preview = guideImprestPreview.value;
  if (!currentRecord || !preview) {
    message.warning('请先计算备用金');
    return;
  }
  guideImprestSubmitting.value = true;
  try {
    if (guideImprestRequestedAmountInvalid()) {
      message.warning('本次申请金额必须大于0');
      return;
    }
    if (guideImprestRemarkMissing()) {
      message.warning('申请后累计备用金超过系统建议金额，请填写应急或特殊项目说明');
      return;
    }
    await submitGuideImprest({
      companyMarkupRate: guideImprestCompanyMarkupRate.value,
      guideId: currentRecord.guideId,
      remark: guideImprestApplyRemark.value.trim() || undefined,
      requestedAmount: guideImprestRequestedAmount.value,
      teamId: teamId.value,
    });
    message.success('备用金申请已提交');
    guideImprestModalOpen.value = false;
    await loadTeamGuides();
    await loadArrangementSummary();
  } finally {
    guideImprestSubmitting.value = false;
  }
}

async function openGuideImprestRecords(record: TeamGuideRow) {
  if (!record.guideId) {
    message.warning('请先选择导游');
    return;
  }
  guideImprestRecordsCurrentRecord.value = record;
  guideImprestRecordRows.value = [];
  guideImprestRecordDetail.value = undefined;
  guideImprestRecordDetailOpen.value = false;
  guideImprestRecordsOpen.value = true;
  await loadGuideImprestRecords(record);
}

async function loadGuideImprestRecords(record = guideImprestRecordsCurrentRecord.value) {
  if (!record?.guideId) {
    return;
  }
  guideImprestRecordsLoading.value = true;
  try {
    const result = await getGuideImprestPage({
      guideId: record.guideId,
      page: 1,
      pageSize: 50,
      teamId: team.value?.id || teamId.value,
    });
    guideImprestRecordRows.value = result.items || [];
  } finally {
    guideImprestRecordsLoading.value = false;
  }
}

async function openGuideImprestRecordDetail(record: GuideImprestApi.Imprest) {
  guideImprestRecordDetailOpen.value = true;
  guideImprestRecordDetail.value = undefined;
  guideImprestRecordDetailLoading.value = true;
  try {
    guideImprestRecordDetail.value = await getGuideImprestDetail(record.id);
  } finally {
    guideImprestRecordDetailLoading.value = false;
  }
}

async function removeTeamGuide(row: TeamGuideRow) {
  Modal.confirm({
    content: `确认删除 ${row.guideName} 的导游安排？`,
    title: '删除导游安排',
    async onOk() {
      await deleteTeamGuide(teamId.value, row.id);
      message.success('导游安排已删除');
      await loadTeamGuides();
      await loadArrangementSummary();
    },
  });
}

function handleToolAction(action: { label: string }) {
  if (action.label === '编辑内部备注') {
    openTeamProfileEditor('internal_note');
    return;
  }
  if (action.label === '打印毛利表') {
    openGrossProfitPreview();
    return;
  }
  Modal.info({
    content: '该动作后续接入正式文件、打印或成本导入接口。',
    title: action.label,
  });
}

function handleFormalHeaderAction(action: { key: string }) {
  if (action.key === 'teamOperation') {
    goTeamOperation();
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
    return Number(line.quantity || 0) * Number(line.costPrice || 0);
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
    await loadArrangementSummary();
  } finally {
    arrangementSaving.value = false;
  }
}

async function loadTeamArrangements() {
  if (!teamId.value) return;
  teamArrangements.value = await getTeamArrangements(teamId.value);
}

async function loadArrangementSummary() {
  if (!teamId.value) return;
  teamArrangementSummary.value = await getTeamArrangementSummary(teamId.value);
}

async function loadSectionStatuses() {
  if (!teamId.value) return;
  Object.keys(sectionLocalStates).forEach((key) => {
    delete sectionLocalStates[key];
  });
  const rows = await getTeamArrangementSectionStatuses(teamId.value);
  rows.forEach((item) => {
    sectionLocalStates[item.arrangementType] = item.status;
  });
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
      await loadArrangementSummary();
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
      loadArrangementSummary(),
      loadSectionStatuses(),
    ]);
    detail.value = operationDetail;
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.id,
  async (currentTeamId, previousTeamId) => {
    // 团队安排页会被多标签缓存复用；切换到另一团时必须重新读取安排、汇总和分类状态。
    if (!currentTeamId || currentTeamId === previousTeamId) return;
    await loadDetail();
    await scrollToRouteHashAnchor();
  },
);

watch(
  () => route.hash,
  () => {
    scrollToRouteHashAnchor();
  },
);

onMounted(async () => {
  await loadDetail();
  await scrollToRouteHashAnchor();
});
</script>

<template>
  <Page :title="pageTitle">
    <Spin :spinning="loading">
      <Card class="team-arrangement-card formal-team-arrangement-card">
        <FormalTeamPageHeader
          :actions="formalHeaderActions"
          :badges="formalHeaderBadges"
          :metrics="formalHeaderMetrics"
          :note="content?.internalRemark || DEFAULT_INTERNAL_REMARK_TEMPLATE"
          :note-metrics="formalHeaderNoteMetrics"
          :stages="arrangementStages"
          :title="teamDisplayName"
          :tool-actions="formalHeaderToolActions"
          tool-title="团队工具"
          @action-click="handleFormalHeaderAction"
          @badge-click="handleFormalHeaderBadge"
          @metric-click="handleMetricClick"
          @note-edit="handleToolAction({ label: '编辑内部备注' })"
          @tool-click="handleToolAction"
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
                  <td :class="costAmountClass(item.cash, 'cost-amount-cash')">{{ formatCostCashMoney(item.cash) }}</td>
                  <td :class="costAmountClass(item.credit, 'cost-amount-credit')">{{ formatCostDetailMoney(item.credit) }}</td>
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
            <div class="arrangement-section-heading">
              <div class="arrangement-section-title">
                <IconifyIcon icon="lucide:badge" />
                <span>导游</span>
                <span
                  class="arrangement-status-badge"
                  :class="teamGuides.length ? 'status-arranged' : 'status-pending'"
                >
                  {{ teamGuides.length ? '已安排' : '未安排' }}
                </span>
              </div>
              <div class="guide-summary-chips">
                <span class="arrangement-summary-chip">安排 {{ teamGuides.length }} 位</span>
                <span class="arrangement-summary-chip">导服费 {{ formatMoney(guideFeeTotal) }}</span>
                <span class="arrangement-summary-chip">操作费 {{ formatMoney(guideOperationFeeTotal) }}</span>
                <span class="arrangement-summary-chip">计划备用金 {{ formatMoney(guideImprestTotal) }}</span>
                <span class="arrangement-summary-chip">累计已批备用金 {{ formatMoney(guideApprovedImprestTotal) }}</span>
                <span class="arrangement-summary-chip">待审批备用金 {{ formatMoney(guidePendingImprestTotal) }}</span>
              </div>
            </div>
            <div class="arrangement-section-actions">
              <Tooltip :title="guideImprestEntryTooltip()">
                <span>
                  <Button
                    size="small"
                    :disabled="!teamGuides.length"
                    @click="openGuideImprestEntry"
                  >
                    备用金测算
                  </Button>
                </span>
              </Tooltip>
              <Button size="small" type="primary" @click="openGuideModal">
                添加导游
              </Button>
              <Button size="small" :loading="guideLoading" @click="loadTeamGuides">刷新</Button>
            </div>
          </div>
          <div class="guide-arrangement-table-wrap">
            <table class="guide-arrangement-table">
              <thead>
                <tr>
                  <th>导游信息</th>
                  <th>带团时间</th>
                  <th>费用</th>
                  <th>备用金</th>
                  <th>备注摘要</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!teamGuides.length">
                  <td colspan="7" class="arrangement-empty-cell guide-table-empty">暂无导游安排</td>
                </tr>
                <tr v-for="record in teamGuides" :key="record.id">
                  <td>
                    <div class="guide-name-line">{{ record.guideName || '未选择导游' }}</div>
                    <div class="guide-phone-line">Tel：{{ record.guideMobile || '--' }}</div>
                  </td>
                  <td>
                    <div class="guide-time-range">
                      <span>上团 {{ formatDateTime(record.startAt) }}</span>
                      <span>下团 {{ formatDateTime(record.endAt) }}</span>
                    </div>
                  </td>
                  <td>
                    <div class="guide-money-stack">
                      <span>导服费 {{ formatMoney(record.guideFee) }}</span>
                      <span>操作费 {{ formatMoney(record.operationFee) }}</span>
                    </div>
                  </td>
                  <td>
                    <div class="guide-money-stack guide-imprest-stack">
                      <span>计划 {{ formatMoney(record.imprestAmount) }}</span>
                      <span>已批 {{ formatMoney(record.approvedImprestAmount) }}</span>
                      <span>待审 {{ formatMoney(record.pendingImprestAmount) }}</span>
                    </div>
                    <div class="guide-imprest-inline-actions">
                      <Tag :color="guideImprestApprovalStatusTone(record.imprestApprovalStatus)">
                        {{ guideImprestApprovalStatusLabel(record.imprestApprovalStatus) }}
                      </Tag>
                      <Button
                        size="small"
                        type="link"
                        :loading="guideImprestLoading && guideImprestCurrentRecord?.id === record.id"
                        @click="openGuideImprestCalculator(record)"
                      >
                        备用金测算
                      </Button>
                      <Button size="small" type="link" @click="openGuideImprestRecords(record)">
                        审批记录
                      </Button>
                    </div>
                  </td>
                  <td>
                    <Tooltip :title="guideRemarkSummary(record)">
                      <div class="guide-remark-summary">{{ guideRemarkSummary(record) }}</div>
                    </Tooltip>
                  </td>
                  <td>
                    <Tag :color="record.tentative ? 'orange' : 'green'">
                      {{ record.tentative ? '待定中' : '已确定' }}
                    </Tag>
                  </td>
                  <td>
                    <div class="guide-row-actions">
                      <Button size="small" type="link" @click="openGuideEditModal(record)">修改</Button>
                      <Button size="small" type="link" @click="openGuidePickerForRow(record)">更换导游</Button>
                      <Button danger size="small" type="link" @click="removeTeamGuide(record)">删除</Button>
                    </div>
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
      :width="quickProfileEditorType === 'internal_note' ? 760 : 460"
      :footer="null"
    >
      <Spin :spinning="teamProfileOptionsLoading || quickProfileSaving">
        <Form layout="vertical" class="quick-profile-form">
          <template v-if="quickProfileEditorType === 'internal_note'">
            <Form.Item label="内部备注">
              <Textarea
                v-model:value="teamProfileDraft.internalRemark"
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
                  v-model:value="teamProfileDraft.perCapitaPitAmount"
                  :min="0"
                  :precision="2"
                  addon-before="¥"
                  class="inside-memo-number-input"
                />
              </Form.Item>
              <Form.Item label="自费加点率">
                <InputNumber
                  v-model:value="teamProfileDraft.optionalMarkupRate"
                  :min="0"
                  :precision="2"
                  addon-after="%"
                  class="inside-memo-number-input"
                />
              </Form.Item>
              <Form.Item label="人均购物">
                <InputNumber
                  v-model:value="teamProfileDraft.perCapitaShoppingAmount"
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
      v-model:open="guideImprestModalOpen"
      class="guide-imprest-modal"
      destroy-on-close
      title="计算导游备用金"
      width="820px"
      @cancel="guideImprestModalOpen = false"
    >
      <Spin :spinning="guideImprestLoading">
        <div class="guide-imprest-summary">
          <div class="guide-imprest-summary-item">
            <span>现付总成本</span>
            <strong>{{ formatMoney(guideImprestPreview?.cashCostAmount) }}</strong>
          </div>
          <div class="guide-imprest-summary-item">
            <span>自费抵扣</span>
            <strong>{{ formatMoney(guideImprestPreview?.optionalDeductionAmount) }}</strong>
          </div>
          <div class="guide-imprest-summary-item">
            <span>实收人数</span>
            <strong>{{ guideImprestPreview?.guestCount || 0 }} 人</strong>
          </div>
          <div class="guide-imprest-summary-item highlight">
            <span>建议备用金</span>
            <strong>{{ formatMoney(guideImprestPreview?.suggestedImprestAmount) }}</strong>
          </div>
          <div class="guide-imprest-summary-item">
            <span>已申请/已批</span>
            <strong>{{ formatMoney(guideImprestPreview?.occupiedAuthorizationAmount) }}</strong>
          </div>
          <div class="guide-imprest-summary-item">
            <span>建议余额参考</span>
            <strong>{{ formatMoney(guideImprestPreview?.availableAuthorizationAmount) }}</strong>
          </div>
          <div class="guide-imprest-summary-item">
            <span>导游应上交</span>
            <strong>{{ formatMoney(guideImprestPreview?.guideTurnInAmount) }}</strong>
          </div>
        </div>
        <div v-if="guideImprestPreview" class="guide-imprest-formula-panel">
          <div class="guide-imprest-formula-line">
            <span>总公式</span>
            <strong>{{ guideImprestSummaryFormula(guideImprestPreview) }}</strong>
          </div>
          <div class="guide-imprest-formula-line">
            <span>风险提示</span>
            <strong>{{ guideImprestReferenceBalanceFormula(guideImprestPreview) }}</strong>
          </div>
          <div v-if="guideImprestEmergencyWarning(guideImprestPreview)" class="guide-imprest-warning">
            {{ guideImprestEmergencyWarning(guideImprestPreview) }}
          </div>
        </div>

        <div class="guide-imprest-lines-title">明细行</div>
        <div class="guide-imprest-table-wrap">
          <table class="guide-imprest-table">
            <thead>
              <tr>
                <th>类型</th>
                <th>项目</th>
                <th>人数</th>
                <th>售价</th>
                <th>成本</th>
                <th>导游提成</th>
                <th>公司加点</th>
                <th>金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!(guideImprestPreview?.calcLines || []).length">
                <td colspan="8" class="guide-imprest-empty">暂无计算明细</td>
              </tr>
              <template
                v-for="(line, index) in guideImprestPreview?.calcLines || []"
                :key="`${line.lineType || 'line'}-${line.itemName || index}-${index}`"
              >
                <tr>
                  <td>{{ guideImprestLineTypeLabel(line.lineType) }}</td>
                  <td class="guide-imprest-item-name">{{ line.itemName || '--' }}</td>
                  <td>{{ line.guestCount ?? '--' }}</td>
                  <td>{{ line.salePrice === undefined ? '--' : formatMoney(line.salePrice) }}</td>
                  <td>{{ line.costPrice === undefined ? '--' : formatMoney(line.costPrice) }}</td>
                  <td>{{ line.guideCommissionAmount === undefined ? '--' : formatMoney(line.guideCommissionAmount) }}</td>
                  <td>{{ line.lineType === 'optional_deduction' ? formatPercent(line.companyMarkupRate) : '--' }}</td>
                  <td class="guide-imprest-amount">{{ formatMoney(line.amount) }}</td>
                </tr>
                <tr class="guide-imprest-formula-row">
                  <td colspan="8">
                    <span>计算：</span>{{ guideImprestLineFormula(line) }}
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <Form layout="vertical" class="guide-imprest-remark-form">
          <div class="guide-imprest-apply-grid">
            <Form.Item label="本次公司加点率">
              <InputNumber
                v-model:value="guideImprestCompanyMarkupRate"
                class="guide-imprest-input"
                :min="0"
                :precision="2"
                addon-after="%"
                @blur="refreshGuideImprestPreview"
              />
            </Form.Item>
            <Form.Item
              label="本次申请金额"
              :validate-status="guideImprestRequestedAmountInvalid() ? 'error' : undefined"
              :help="guideImprestRequestedAmountInvalid() ? '必须大于0' : undefined"
            >
              <InputNumber
                v-model:value="guideImprestRequestedAmount"
                class="guide-imprest-input"
                :min="0"
                :precision="2"
                addon-before="¥"
              />
            </Form.Item>
            <Form.Item label="重新计算">
              <Button :loading="guideImprestLoading" @click="refreshGuideImprestPreview">
                按本次加点率计算
              </Button>
            </Form.Item>
          </div>
          <Form.Item
            label="申请备注"
            :validate-status="guideImprestRemarkMissing() ? 'error' : undefined"
            :help="guideImprestRemarkMissing() ? '超过系统建议金额时必须填写应急或特殊项目说明' : undefined"
          >
            <Textarea
              v-model:value="guideImprestApplyRemark"
              :auto-size="{ minRows: 3, maxRows: 5 }"
              :maxlength="500"
              placeholder="可填写备用金申请原因或特殊说明"
              show-count
            />
          </Form.Item>
        </Form>
      </Spin>

      <template #footer>
        <Button @click="guideImprestModalOpen = false">取消</Button>
        <Button
          :disabled="!guideImprestPreview"
          :loading="guideImprestLoading"
          @click="useGuideImprestSuggestedAmount"
        >
          使用此金额
        </Button>
        <Button
          type="primary"
          :disabled="!guideImprestPreview || guideImprestRequestedAmountInvalid() || guideImprestRemarkMissing()"
          :loading="guideImprestSubmitting"
          @click="submitGuideImprestApplication"
        >
          提交备用金申请
        </Button>
      </template>
    </Modal>

    <Modal
      v-model:open="guideImprestRecordsOpen"
      destroy-on-close
      title="导游备用金审批记录"
      width="1080px"
      :footer="null"
      @cancel="guideImprestRecordDetail = undefined"
    >
      <Spin :spinning="guideImprestRecordsLoading">
        <div class="guide-imprest-record-head">
          <div>
            <strong>{{ guideImprestRecordsCurrentRecord?.guideName || '--' }}</strong>
            <span>本团备用金申请单记录</span>
          </div>
          <Button size="small" :loading="guideImprestRecordsLoading" @click="loadGuideImprestRecords()">
            刷新
          </Button>
        </div>
        <Table
          :data-source="guideImprestRecordRows"
          :pagination="false"
          :scroll="{ x: 980 }"
          row-key="id"
          size="small"
        >
          <Table.Column data-index="requestNo" title="申请编号" width="150" />
          <Table.Column title="申请金额" width="110">
            <template #default="{ record }">{{ formatMoney(record.requestedAmount) }}</template>
          </Table.Column>
          <Table.Column title="审批状态" width="130">
            <template #default="{ record }">
              <Tag :color="guideImprestRequestStatusTone(record.status)">
                {{ guideImprestRequestStatusLabel(record.status) }}
              </Tag>
            </template>
          </Table.Column>
          <Table.Column title="申请人 / 时间" width="180">
            <template #default="{ record }">
              <div class="guide-imprest-record-stack">
                <span>{{ record.applicant || '--' }}</span>
                <small>{{ formatDateTime(record.appliedAt) }}</small>
              </div>
            </template>
          </Table.Column>
          <Table.Column title="审批人 / 时间" width="190">
            <template #default="{ record }">
              <div class="guide-imprest-record-stack">
                <span>{{ record.approvedBy || record.rejectedBy || '--' }}</span>
                <small>{{ formatDateTime(record.approvedAt || record.rejectedAt) }}</small>
              </div>
            </template>
          </Table.Column>
          <Table.Column data-index="approvalRemark" title="审批意见" width="170">
            <template #default="{ record }">{{ record.approvalRemark || '--' }}</template>
          </Table.Column>
          <Table.Column title="已付金额" width="110">
            <template #default="{ record }">{{ formatMoney(record.paidAmount) }}</template>
          </Table.Column>
          <Table.Column title="余额" width="100">
            <template #default="{ record }">{{ formatMoney(record.balanceAmount) }}</template>
          </Table.Column>
          <Table.Column title="操作" width="100" fixed="right">
            <template #default="{ record }">
              <Button
                size="small"
                type="link"
                :loading="guideImprestRecordDetailLoading && guideImprestRecordDetail?.id === record.id"
                @click="openGuideImprestRecordDetail(record)"
              >
                查看详情
              </Button>
            </template>
          </Table.Column>
        </Table>
      </Spin>
    </Modal>

    <Modal
      v-model:open="guideImprestRecordDetailOpen"
      destroy-on-close
      title="备用金申请详情"
      width="860px"
      :footer="null"
    >
      <Spin :spinning="guideImprestRecordDetailLoading">
        <section v-if="guideImprestRecordDetail" class="guide-imprest-record-detail">
          <div class="guide-imprest-record-detail-title">
            {{ guideImprestRecordDetail.requestNo || '申请详情' }}
          </div>
          <div class="guide-imprest-record-summary">
            <span>现付总成本 {{ formatMoney(guideImprestRecordDetail.cashCostAmount) }}</span>
            <span>自费抵扣 {{ formatMoney(guideImprestRecordDetail.optionalDeductionAmount) }}</span>
            <span>建议备用金 {{ formatMoney(guideImprestRecordDetail.suggestedImprestAmount) }}</span>
            <span>申请金额 {{ formatMoney(guideImprestRecordDetail.requestedAmount) }}</span>
            <span>已付金额 {{ formatMoney(guideImprestRecordDetail.paidAmount) }}</span>
            <span>余额 {{ formatMoney(guideImprestRecordDetail.balanceAmount) }}</span>
          </div>
          <div v-if="guideImprestRecordDetail.calculationChanged" class="guide-imprest-record-warning">
            {{ guideImprestRecordDetail.calculationChangeMessage || '团队安排已变化，请计调重新计算备用金' }}
          </div>
          <div class="guide-imprest-record-remark">
            <span>申请备注：{{ guideImprestRecordDetail.remark || '--' }}</span>
            <span>审批意见：{{ guideImprestRecordDetail.approvalRemark || '--' }}</span>
          </div>
          <div class="guide-imprest-table-wrap guide-imprest-record-lines">
            <table class="guide-imprest-table">
              <thead>
                <tr>
                  <th>类型</th>
                  <th>项目</th>
                  <th>人数</th>
                  <th>售价</th>
                  <th>成本</th>
                  <th>导游提成</th>
                  <th>公司加点</th>
                  <th>金额</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!(guideImprestRecordDetail.calcLines || []).length">
                  <td colspan="8" class="guide-imprest-empty">暂无计算明细</td>
                </tr>
                <tr
                  v-for="(line, index) in guideImprestRecordDetail.calcLines || []"
                  :key="`${line.lineType || 'line'}-${line.itemName || index}-${index}`"
                >
                  <td>{{ guideImprestLineTypeLabel(line.lineType) }}</td>
                  <td class="guide-imprest-item-name">{{ line.itemName || '--' }}</td>
                  <td>{{ line.guestCount ?? '--' }}</td>
                  <td>{{ line.salePrice === undefined ? '--' : formatMoney(line.salePrice) }}</td>
                  <td>{{ line.costPrice === undefined ? '--' : formatMoney(line.costPrice) }}</td>
                  <td>{{ line.guideCommissionAmount === undefined ? '--' : formatMoney(line.guideCommissionAmount) }}</td>
                  <td>{{ line.lineType === 'optional_deduction' ? formatPercent(line.companyMarkupRate) : '--' }}</td>
                  <td class="guide-imprest-amount">{{ formatMoney(line.amount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </Spin>
    </Modal>

    <Modal
      v-model:open="guideEditOpen"
      centered
      destroy-on-close
      title="修改导游安排"
      width="720px"
      ok-text="保存"
      :confirm-loading="guideEditSaving"
      @ok="submitGuideEditDraft"
    >
      <Form layout="vertical" class="guide-modal-form guide-edit-form">
        <section class="guide-modal-section">
          <div class="guide-modal-section-title">费用信息</div>
          <div class="guide-modal-grid guide-modal-grid-fees">
            <Form.Item label="导服费">
              <InputNumber v-model:value="guideEditDraft.guideFee" class="w-full" :min="0" addon-before="¥" />
            </Form.Item>
            <Form.Item label="计划备用金">
              <Input
                :value="formatMoney(guideEditDraft.imprestAmount)"
                disabled
                class="w-full"
                title="计划备用金由备用金测算结果写入"
              />
            </Form.Item>
            <Form.Item label="操作费">
              <InputNumber v-model:value="guideEditDraft.operationFee" class="w-full" :min="0" addon-before="¥" />
            </Form.Item>
          </div>
        </section>

        <section class="guide-modal-section">
          <div class="guide-modal-section-title">带团时间</div>
          <div class="guide-modal-grid">
            <Form.Item label="上团时间" required>
              <DatePicker
                v-model:value="guideEditDraft.startAt"
                class="w-full"
                format="YYYY-MM-DD HH:mm"
                show-time
              />
            </Form.Item>
            <Form.Item label="下团时间" required>
              <DatePicker
                v-model:value="guideEditDraft.endAt"
                class="w-full"
                format="YYYY-MM-DD HH:mm"
                show-time
              />
            </Form.Item>
          </div>
        </section>

        <section class="guide-modal-section">
          <div class="guide-modal-section-title">状态</div>
          <Checkbox v-model:checked="guideEditDraft.tentative">待定中</Checkbox>
        </section>

        <section class="guide-modal-section guide-modal-section-last">
          <div class="guide-modal-section-title">备注信息</div>
          <Form.Item label="费用说明">
            <Textarea
              v-model:value="guideEditDraft.feeMemo"
              class="guide-edit-textarea"
              :auto-size="{ minRows: 3, maxRows: 5 }"
              :maxlength="1000"
            />
          </Form.Item>
          <Form.Item label="导游备注">
            <Textarea
              v-model:value="guideEditDraft.guideMemo"
              class="guide-edit-textarea"
              :auto-size="{ minRows: 3, maxRows: 5 }"
              :maxlength="1000"
            />
          </Form.Item>
        </section>
      </Form>
    </Modal>

    <Modal
      v-model:open="guideModalOpen"
      class="guide-arrangement-modal"
      destroy-on-close
      :title="guideModalEditing ? '更换导游' : '选择导游'"
      width="920px"
      ok-text="提交保存"
      :confirm-loading="guideSaving"
      @ok="submitGuideDraft"
    >
      <Form layout="vertical" :model="guideDraft" class="guide-modal-form">
        <section class="guide-modal-section guide-picker-section">
          <div class="guide-modal-title-row">
            <div class="guide-modal-section-title">
              <span>选择导游</span>
              <span class="guide-modal-title-en">Choose Guide</span>
            </div>
            <div v-if="selectedGuideDraft" class="guide-selected-pill">
              已选 {{ selectedGuideDraft.guideName }} / {{ guideDisplayPhone(selectedGuideDraft) }}
            </div>
          </div>
          <div class="guide-picker-tabs">
            <button
              v-for="item in guidePickerTabs"
              :key="item.key"
              type="button"
              class="guide-picker-tab"
              :class="{ active: guidePickerActiveTab === item.key }"
              @click="setGuidePickerTab(item.key)"
            >
              {{ item.label }}
            </button>
          </div>
          <div class="guide-picker-search">
            <Input
              v-model:value="guidePickerKeyword"
              allow-clear
              class="guide-picker-search-input"
              placeholder="导游名称/手机号"
            />
            <Button type="primary" class="guide-picker-search-button" :loading="guideAvailabilityLoading" @click="loadGuideAvailability">
              <IconifyIcon icon="lucide:search" />
              <span>搜索</span>
            </Button>
            <span class="guide-picker-tip">默认最多显示50人，全部导游会标记不能出团原因</span>
          </div>
          <Spin :spinning="guideAvailabilityLoading">
            <div class="guide-picker-table-wrap">
              <table class="guide-picker-table">
                <thead>
                  <tr>
                    <th>选择</th>
                    <th>导游姓名</th>
                    <th>手机号</th>
                    <th>性别</th>
                    <th>状态</th>
                    <th>不能出团原因</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!guideAvailabilityRows.length">
                    <td colspan="6" class="guide-picker-empty">暂无匹配导游</td>
                  </tr>
                  <tr
                    v-for="item in guideAvailabilityRows"
                    :key="item.guideId"
                    :class="{ selected: guideDraft.guideId === item.guideId, disabled: !item.available }"
                  >
                    <td>
                      <Button
                        size="small"
                        type="link"
                        :disabled="!item.available"
                        @click="selectGuideForDraft(item)"
                      >
                        {{ guideDraft.guideId === item.guideId ? '已选' : '选择' }}
                      </Button>
                    </td>
                    <td class="guide-picker-name-cell">{{ item.guideName }}</td>
                    <td class="guide-picker-phone-cell">{{ guideDisplayPhone(item) }}</td>
                    <td>{{ guideGenderText(item.gender) }}</td>
                    <td>
                      <Tag :color="item.available ? 'green' : 'orange'">
                        {{ item.available ? '可出团' : '不能出团' }}
                      </Tag>
                    </td>
                    <td class="guide-picker-reason-cell">{{ item.unavailableReason || '--' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Spin>
        </section>

        <section v-if="!guideModalEditing" class="guide-modal-section">
          <div class="guide-modal-section-title">费用信息</div>
          <div class="guide-modal-grid guide-modal-grid-fees">
            <Form.Item label="导服费">
              <InputNumber v-model:value="guideDraft.guideFee" class="w-full" :min="0" addon-before="¥" />
            </Form.Item>
            <Form.Item label="备用金">
              <Input
                value="保存导游后进行备用金测算"
                disabled
                class="w-full"
              />
            </Form.Item>
            <Form.Item label="操作费">
              <InputNumber v-model:value="guideDraft.operationFee" class="w-full" :min="0" addon-before="¥" />
            </Form.Item>
          </div>
        </section>

        <section v-if="!guideModalEditing" class="guide-modal-section">
          <div class="guide-modal-section-title">带团时间</div>
          <div class="guide-modal-grid">
            <Form.Item label="上团时间" required>
              <DatePicker
                v-model:value="guideDraftDates.startAt"
                class="w-full"
                format="YYYY-MM-DD HH:mm"
                show-time
                @change="loadGuideAvailability"
              />
            </Form.Item>
            <Form.Item label="下团时间" required>
              <DatePicker
                v-model:value="guideDraftDates.endAt"
                class="w-full"
                format="YYYY-MM-DD HH:mm"
                show-time
                @change="loadGuideAvailability"
              />
            </Form.Item>
          </div>
        </section>

        <section v-if="!guideModalEditing" class="guide-modal-section">
          <div class="guide-modal-section-title">状态</div>
          <Checkbox v-model:checked="guideDraft.tentative">待定中</Checkbox>
        </section>

        <section v-if="!guideModalEditing" class="guide-modal-section guide-modal-section-last">
          <div class="guide-modal-section-title">备注信息</div>
          <Form.Item label="费用说明">
            <Textarea v-model:value="guideDraft.feeMemo" :rows="3" :maxlength="1000" />
          </Form.Item>
          <Form.Item label="导游备注">
            <Textarea v-model:value="guideDraft.guideMemo" :rows="3" :maxlength="1000" />
          </Form.Item>
        </section>
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

.traffic-modal-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
  padding-top: 14px;
  margin-top: 16px;
  border-top: 1px solid #e2e8f0;
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

.guide-arrangement-section .arrangement-section-header {
  align-items: center;
}

.guide-summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
  margin-top: 1px;
}

.guide-arrangement-table-wrap {
  overflow-x: auto;
  background: #fff;
  border: 1px solid #e4ecf7;
  border-radius: 7px;
}

.guide-arrangement-table {
  width: 100%;
  min-width: 1180px;
  border-collapse: collapse;
  table-layout: fixed;
}

.guide-arrangement-table th,
.guide-arrangement-table td {
  min-width: 0;
  padding: 9px 10px;
  font-size: 12.5px;
  color: #334155;
  text-align: left;
  vertical-align: middle;
  border-bottom: 1px solid #edf2f7;
}

.guide-arrangement-table th {
  height: 34px;
  font-size: 12px;
  font-weight: 850;
  color: #52657a;
  background: #f8fafc;
}

.guide-arrangement-table tr:last-child td {
  border-bottom: 0;
}

.guide-arrangement-table th:nth-child(1),
.guide-arrangement-table td:nth-child(1) {
  width: 180px;
}

.guide-arrangement-table th:nth-child(2),
.guide-arrangement-table td:nth-child(2) {
  width: 220px;
}

.guide-arrangement-table th:nth-child(3),
.guide-arrangement-table td:nth-child(3) {
  width: 150px;
}

.guide-arrangement-table th:nth-child(4),
.guide-arrangement-table td:nth-child(4) {
  width: 245px;
}

.guide-arrangement-table th:nth-child(6),
.guide-arrangement-table td:nth-child(6) {
  width: 82px;
  text-align: center;
}

.guide-arrangement-table th:nth-child(7),
.guide-arrangement-table td:nth-child(7) {
  width: 170px;
}

.guide-table-empty {
  height: 72px;
  text-align: center !important;
}

.guide-name-line,
.guide-phone-line,
.guide-remark-summary {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-name-line {
  margin-bottom: 3px;
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
}

.guide-phone-line {
  font-size: 12px;
  font-weight: 750;
  color: #64748b;
}

.guide-time-range {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.guide-time-range span {
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-money-stack {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.guide-money-stack span {
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  color: #475569;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-imprest-stack {
  gap: 3px;
}

.guide-imprest-stack span {
  padding: 0;
  color: #475569;
}

.guide-imprest-inline-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  align-items: center;
  min-width: 0;
  margin-top: 6px;
  overflow: hidden;
  white-space: nowrap;
}

.guide-imprest-inline-actions :deep(.ant-tag) {
  flex: 0 0 auto;
  margin-inline-end: 0;
}

.guide-imprest-inline-actions :deep(.ant-btn) {
  height: 20px;
  padding: 0 2px;
  font-size: 12px;
  font-weight: 850;
}

.guide-remark-summary {
  max-width: 100%;
  font-weight: 760;
  color: #64748b;
}

.guide-row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 4px;
  align-items: center;
}

.guide-row-actions :deep(.ant-btn) {
  height: 22px;
  padding: 0 2px;
  font-size: 12px;
  font-weight: 850;
}

.guide-imprest-record-head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.guide-imprest-record-head strong {
  margin-right: 10px;
  font-size: 15px;
  font-weight: 900;
  color: #0f172a;
}

.guide-imprest-record-head span {
  font-size: 12.5px;
  font-weight: 800;
  color: #64748b;
}

.guide-imprest-record-stack {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.guide-imprest-record-stack span,
.guide-imprest-record-stack small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-imprest-record-stack span {
  font-weight: 800;
  color: #334155;
}

.guide-imprest-record-stack small {
  font-size: 12px;
  color: #64748b;
}

.guide-imprest-record-detail {
  padding: 12px;
  margin-top: 14px;
  background: #fbfdff;
  border: 1px solid #e4ecf7;
  border-radius: 6px;
}

.guide-imprest-record-detail-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
}

.guide-imprest-record-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.guide-imprest-record-summary span {
  min-width: 0;
  padding: 7px 9px;
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 800;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fff;
  border: 1px solid #edf2f7;
  border-radius: 5px;
}

.guide-imprest-record-warning {
  padding: 8px 10px;
  margin-bottom: 10px;
  font-size: 12.5px;
  font-weight: 800;
  line-height: 1.5;
  color: #9a3412;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 5px;
}

.guide-imprest-record-remark {
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
  font-size: 12.5px;
  font-weight: 800;
  color: #475569;
}

.guide-imprest-record-lines {
  max-height: 230px;
  margin-bottom: 0;
}

.guide-imprest-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 14px;
}

.guide-imprest-summary-item {
  min-width: 0;
  padding: 10px 12px;
  background: #fbfdff;
  border: 1px solid #e4ecf7;
  border-radius: 6px;
}

.guide-imprest-summary-item span,
.guide-imprest-lines-title {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 850;
  line-height: 1;
  color: #52657a;
}

.guide-imprest-summary-item strong {
  display: block;
  overflow: hidden;
  font-size: 16px;
  font-weight: 900;
  line-height: 1.2;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-imprest-summary-item.highlight {
  background: #eef6ff;
  border-color: #b7d7ff;
}

.guide-imprest-summary-item.highlight strong {
  color: #1554ad;
}

.guide-imprest-formula-panel {
  padding: 10px 12px;
  margin: 12px 0 14px;
  font-size: 13px;
  line-height: 1.6;
  color: #334155;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 6px;
}

.guide-imprest-formula-panel span {
  margin-right: 10px;
  font-weight: 850;
  color: #1554ad;
}

.guide-imprest-formula-panel strong {
  font-weight: 800;
}

.guide-imprest-formula-line + .guide-imprest-formula-line {
  margin-top: 4px;
}

.guide-imprest-warning {
  padding: 7px 9px;
  margin-top: 9px;
  font-size: 12.5px;
  font-weight: 800;
  line-height: 1.5;
  color: #9a3412;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 5px;
}

.guide-imprest-lines-title {
  margin-bottom: 8px;
}

.guide-imprest-table-wrap {
  max-height: 260px;
  margin-bottom: 12px;
  overflow: auto;
  background: #fff;
  border: 1px solid #e4ecf7;
  border-radius: 6px;
}

.guide-imprest-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
  table-layout: fixed;
}

.guide-imprest-table th,
.guide-imprest-table td {
  height: 36px;
  padding: 8px 10px;
  font-size: 12.5px;
  color: #334155;
  text-align: left;
  border-bottom: 1px solid #eef2f7;
}

.guide-imprest-table th {
  font-weight: 850;
  color: #52657a;
  background: #f8fafc;
}

.guide-imprest-table th:nth-child(2),
.guide-imprest-table td:nth-child(2) {
  width: 180px;
}

.guide-imprest-item-name {
  overflow: hidden;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-imprest-amount {
  font-weight: 850;
  color: #1554ad;
}

.guide-imprest-formula-row td {
  height: auto;
  padding: 8px 10px 10px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #64748b;
  background: #fbfdff;
}

.guide-imprest-formula-row span {
  font-weight: 850;
  color: #334155;
}

.guide-imprest-empty {
  padding: 28px 0;
  font-size: 13px;
  font-weight: 800;
  color: #94a3b8;
  text-align: center;
}

.guide-imprest-apply-grid {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 10px 12px;
  align-items: start;
}

.guide-imprest-input {
  width: 100%;
}

.guide-imprest-remark-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.guide-imprest-apply-grid :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.guide-imprest-remark-form :deep(.ant-form-item-label > label) {
  font-size: 12.5px;
  font-weight: 800;
  color: #475569;
}

.guide-modal-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.guide-modal-form :deep(.ant-form-item-label > label) {
  font-size: 12.5px;
  font-weight: 800;
  color: #475569;
}

.guide-picker-section {
  padding-bottom: 14px;
}

.guide-modal-title-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.guide-modal-section {
  padding: 12px 14px 2px;
  margin-bottom: 12px;
  background: #fbfdff;
  border: 1px solid #e4ecf7;
  border-radius: 6px;
}

.guide-modal-section.guide-picker-section {
  padding-bottom: 14px;
}

.guide-modal-section-last {
  margin-bottom: 0;
}

.guide-modal-section-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 900;
  line-height: 1;
  color: #0f172a;
}

.guide-modal-title-row .guide-modal-section-title {
  display: inline-flex;
  gap: 10px;
  align-items: baseline;
  margin-bottom: 0;
  font-size: 20px;
  line-height: 1.2;
}

.guide-modal-title-en {
  font-size: 18px;
  font-weight: 800;
  color: #d1d5db;
}

.guide-selected-pill {
  max-width: 360px;
  padding: 4px 10px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  color: #1554ad;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #eef6ff;
  border: 1px solid #b7d7ff;
  border-radius: 999px;
}

.guide-picker-tabs {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding-bottom: 0;
  margin-bottom: 12px;
  border-bottom: 2px solid #1677ff;
}

.guide-picker-tab {
  min-width: 108px;
  height: 36px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 850;
  color: #334155;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dbe4f0;
  border-bottom: 0;
  border-radius: 6px 6px 0 0;
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.guide-picker-tab.active {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
}

.guide-picker-search {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.guide-picker-search-input {
  width: 210px;
}

.guide-picker-search-button {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  font-weight: 800;
}

.guide-picker-search-button svg {
  width: 15px;
  height: 15px;
}

.guide-picker-tip {
  font-size: 12px;
  font-weight: 800;
  color: #15803d;
}

.guide-picker-table-wrap {
  min-height: 168px;
  max-height: 300px;
  overflow: auto;
  background: #fff;
  border: 1px solid #e4ecf7;
  border-radius: 6px;
}

.guide-picker-table {
  width: 100%;
  min-width: 820px;
  border-collapse: collapse;
  table-layout: fixed;
}

.guide-picker-table th,
.guide-picker-table td {
  height: 38px;
  padding: 8px 10px;
  font-size: 12.5px;
  color: #334155;
  text-align: left;
  border-bottom: 1px solid #eef2f7;
}

.guide-picker-table th {
  font-weight: 850;
  color: #52657a;
  background: #f8fafc;
}

.guide-picker-table th:nth-child(1),
.guide-picker-table td:nth-child(1) {
  width: 68px;
  text-align: center;
}

.guide-picker-table th:nth-child(2),
.guide-picker-table td:nth-child(2) {
  width: 128px;
}

.guide-picker-table th:nth-child(3),
.guide-picker-table td:nth-child(3) {
  width: 128px;
}

.guide-picker-table th:nth-child(4),
.guide-picker-table td:nth-child(4) {
  width: 72px;
  text-align: center;
}

.guide-picker-table th:nth-child(5),
.guide-picker-table td:nth-child(5) {
  width: 92px;
  text-align: center;
}

.guide-picker-table tr.selected td {
  background: #eef6ff;
}

.guide-picker-table tr.disabled td {
  color: #94a3b8;
  background: #fbfdff;
}

.guide-picker-name-cell,
.guide-picker-phone-cell,
.guide-picker-reason-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-picker-name-cell {
  font-weight: 850;
  color: #0f172a;
}

.guide-picker-phone-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 700;
  color: #64748b;
}

.guide-picker-empty {
  padding: 32px 0;
  font-size: 13px;
  font-weight: 800;
  color: #94a3b8;
  text-align: center;
}

.guide-modal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 14px;
}

.guide-modal-grid-main {
  grid-template-columns: minmax(0, 1fr) 120px;
}

.guide-modal-grid-fees {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.guide-modal-section :deep(.ant-checkbox-wrapper) {
  height: 32px;
  font-weight: 700;
  color: #334155;
}

.guide-modal-section :deep(.ant-input),
.guide-modal-section :deep(.ant-input-number),
.guide-modal-section :deep(.ant-select-selector),
.guide-modal-section :deep(.ant-picker) {
  border-color: #dbe4f0;
  border-radius: 5px;
}

.shopping-commission-summary,
.shopping-rule-grid,
.shopping-bonus-grid,
.shopping-feedback-grid,
.shopping-feedback-detail-grid,
.shopping-feedback-detail-summary,
.shopping-settlement-grid {
  display: grid;
  gap: 12px;
}

.shopping-commission-summary {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin-bottom: 14px;
}

.shopping-commission-card {
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.shopping-commission-card span {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 800;
  color: #64748b;
}

.shopping-commission-card strong {
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
}

.shopping-commission-card.highlight {
  background: #ecfdf5;
  border-color: #bbf7d0;
}

.shopping-commission-card.highlight strong {
  color: #047857;
}

.shopping-commission-section {
  padding: 14px;
  margin-top: 12px;
  border: 1px solid #e4ecf7;
  border-radius: 6px;
}

.shopping-commission-section-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.shopping-commission-section-title span {
  font-size: 14px;
  font-weight: 900;
  color: #1e293b;
}

.shopping-commission-section-title small {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.shopping-rule-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.shopping-bonus-grid {
  grid-template-columns: minmax(180px, 240px) minmax(0, 1fr);
}

.shopping-feedback-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.shopping-commission-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin: 4px 0 12px;
}

.shopping-feedback-table {
  margin-top: 4px;
}

.shopping-feedback-detail-panel {
  padding: 12px;
  margin-top: 4px;
  background: #fbfdff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.shopping-feedback-detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.shopping-feedback-detail-summary span {
  padding: 7px 9px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
}

.shopping-feedback-detail-line {
  padding: 10px;
  margin-bottom: 10px;
  background: #ffffff;
  border: 1px solid #e8eef7;
  border-radius: 5px;
}

.shopping-feedback-detail-grid {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(130px, 1fr) minmax(220px, 1.6fr) minmax(220px, 1.6fr);
  gap: 10px 12px;
}

.shopping-feedback-rate-field {
  display: grid;
  grid-template-columns: minmax(72px, 0.7fr) 18px minmax(110px, 1fr);
  gap: 6px;
  align-items: center;
}

.shopping-feedback-rate-field span {
  font-size: 12px;
  font-weight: 800;
  color: #64748b;
  text-align: center;
}

.shopping-settlement-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.shopping-settlement-grid span {
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 800;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
}

@media (max-width: 1080px) {
  .formal-team-arrangement-card :deep(.arrangement-command-bar) {
    grid-template-columns: 1fr;
  }

  .traffic-modal-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .guide-modal-grid,
  .guide-modal-grid-fees,
  .guide-modal-grid-main {
    grid-template-columns: 1fr;
  }

  .guide-imprest-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .shopping-commission-summary,
  .shopping-rule-grid,
  .shopping-bonus-grid,
  .shopping-feedback-grid,
  .shopping-feedback-detail-grid,
  .shopping-feedback-detail-summary,
  .shopping-settlement-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .guide-modal-title-row,
  .guide-picker-search {
    align-items: stretch;
    flex-direction: column;
  }

  .guide-picker-search-input {
    width: 100%;
  }
}

@media (max-width: 680px) {
  .shopping-commission-summary,
  .shopping-rule-grid,
  .shopping-bonus-grid,
  .shopping-feedback-grid,
  .shopping-feedback-detail-grid,
  .shopping-feedback-detail-summary,
  .shopping-settlement-grid {
    grid-template-columns: 1fr;
  }
}
</style>
