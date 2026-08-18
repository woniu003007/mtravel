import type { SalesProductApi } from '#/api/sales/product';

export type ArrangementType = SalesProductApi.ArrangementType;

export type SelectOption = {
  label: string;
  value: string;
};

export type SelectOptionWithId = SelectOption & {
  id?: number;
};

export type AutoCompleteOption = SelectOption;

export type ArrangementEditorMode = 'product' | 'team';

export type ArrangementEditorConfig = {
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

export type ArrangementEditorForm = {
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
  guideId?: number;
  guideName?: string;
  headFeeAmount: number;
  mealType?: string;
  multiOrderSplitMode: 'by_order' | 'by_people';
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
  selectedOrderIds: number[];
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

export const arrangementEditorConfigs: Record<ArrangementType, ArrangementEditorConfig> = {
  extra_fee: {
    noGuideReport: true,
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
    showOrderInfo: true,
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

const RESOURCE_BOUND_SUPPLIER_TYPES = new Set<ArrangementType>([
  'hotel',
  'meal',
  'optional',
  'scenic',
  'shopping',
]);

/** 这些资源类型的供应商必须来自资源采购关系，不能直接展示同分类全部供应商。 */
export function shouldFilterSupplierByResource(type: ArrangementType) {
  return RESOURCE_BOUND_SUPPLIER_TYPES.has(type);
}

export const trafficTypeOptions: SelectOption[] = ['飞机', '高铁', '火车', '邮轮']
  .map((item) => ({ label: item, value: item }));

export const mealTypeOptions: SelectOption[] = ['早餐', '中餐', '晚餐']
  .map((item) => ({ label: item, value: item }));

export const breakfastOptions: SelectOption[] = ['桌早', '自助早', '打包早', '不含']
  .map((item) => ({ label: item, value: item }));

export const fundOptions: SelectOption[] = ['不含', '含']
  .map((item) => ({ label: item, value: item }));

export const trafficOrderOptions: SelectOption[] = [
  { label: '=不关联订单=', value: '=不关联订单=' },
];

export const standardMealProjectOptions: SelectOptionWithId[] = ['标准餐', '豪华餐', '餐券', '其它']
  .map((item) => ({ label: item, value: item }));

export const otherProjectOptions: SelectOptionWithId[] = ['礼品', '特产', '预收款', '保险', '购物返佣', '购物人头', '停车费', '房租', '水电', '其它']
  .map((item) => ({ label: item, value: item }));

export const groundAgentProjectOptions: SelectOptionWithId[] = ['成人', '儿童', '车费', '综费', '接送费', '代收团款', '定金对公', '团费', '成本', '其它']
  .map((item) => ({ label: item, value: item }));

export const groundAgentPackageProjectName = '地接结算价';

export const shoppingCategoryOptions: SelectOptionWithId[] = ['乳胶', '茶叶', '翡翠', '厨具', '黄金饰品', '茶多酚', '土特产', '丝绸', '珍珠', '唐卡', '藏药', '其它']
  .map((item) => ({ label: item, value: item }));

export const defaultPriceQuantity = 0;

export function defaultProjectName(type: ArrangementType) {
  const defaults: Record<ArrangementType, string> = {
    extra_fee: '保险',
    ground_agent: groundAgentPackageProjectName,
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

export function createDefaultArrangementPriceLine(projectName = ''): SalesProductApi.ArrangementPriceLine {
  return {
    projectName,
    quantity: defaultPriceQuantity,
    sortOrder: 1,
    unitPrice: 0,
  };
}

function numericPackageMoney(value?: number | string) {
  const amount = Number(value || 0);
  return Number.isFinite(amount) ? amount : 0;
}

function arrangementLineAmount(line: Partial<SalesProductApi.ArrangementPriceLine>) {
  const explicitAmount = numericPackageMoney(line.amount);
  if (explicitAmount > 0) return explicitAmount;
  return numericPackageMoney(line.unitPrice) * numericPackageMoney(line.quantity);
}

/**
 * 地接按外包地接社一口总价录入；旧数据可能仍有多行价格明细，编辑时折算为一个总价显示。
 */
export function resolveGroundAgentPackageAmount(input: {
  costAmount?: number | string;
  priceLines?: Partial<SalesProductApi.ArrangementPriceLine>[];
  totalAmount?: number | string;
}) {
  const costAmount = numericPackageMoney(input.costAmount);
  if (costAmount > 0) return costAmount;
  const totalAmount = numericPackageMoney(input.totalAmount);
  if (totalAmount > 0) return totalAmount;
  return (input.priceLines || []).reduce((sum, line) => sum + arrangementLineAmount(line), 0);
}

/**
 * 地接不展示价格明细，但保存一条系统兼容明细，支撑现有成本归属和统计链路。
 */
export function createGroundAgentPackagePriceLine(
  amount: number,
  remark?: string,
): SalesProductApi.ArrangementPriceLine {
  return {
    amount,
    projectName: groundAgentPackageProjectName,
    quantity: 1,
    remark,
    sortOrder: 1,
    unitPrice: amount,
  };
}

export function createDefaultArrangementEditorForm(type: ArrangementType): ArrangementEditorForm {
  const projectName = defaultProjectName(type);
  return {
    allocationMode: 'group_order_average',
    arrivalPlace: '',
    cashAmount: 0,
    companyRebateAmount: 0,
    companyRebateRate: 0,
    confirmed: false,
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
    multiOrderSplitMode: 'by_order',
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
    selectedOrderIds: [],
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

/** 部分老系统弹窗的费用项目是固定选项，不依赖后台费用项目字典。 */
export function priceProjectOptionsForType(
  type: ArrangementType,
  projectOptions: SelectOptionWithId[],
) {
  if (type === 'meal') return standardMealProjectOptions;
  if (type === 'other') return otherProjectOptions;
  if (type === 'ground_agent') return groundAgentProjectOptions;
  if (type === 'shopping') return shoppingCategoryOptions;
  return projectOptions;
}

export function parseScheduleDayNo(value?: string) {
  if (!value) return undefined;
  const matched = value.match(/第\s*(\d+)\s*天/);
  return matched?.[1] ? Number(matched[1]) : undefined;
}

/** 回显历史记录时，优先使用正式资源名；老数据缺失时退回到表单里原本保存的名称。 */
export function resolveArrangementResourceName(resourceName?: string, itemName?: string) {
  return resourceName?.trim() || itemName?.trim() || '';
}

/** 下拉选项如果缺少历史值，补一条历史值进去，避免编辑态显示空白。 */
export function ensureSelectOption(
  options: SelectOptionWithId[],
  value?: string,
  id?: number,
) {
  const normalizedValue = value?.trim();
  if (!normalizedValue) return [...options];
  const exists = options.some((item) => item.value === normalizedValue || (id !== undefined && item.id === id));
  if (exists) return [...options];
  return [{ id, label: normalizedValue, value: normalizedValue }, ...options];
}

type SupplierOptionResolutionInput = {
  currentSupplierId?: number;
  currentSupplierName?: string;
  nextOptions: SelectOptionWithId[];
};

type SupplierOptionResolutionResult = {
  options: SelectOptionWithId[];
  selectedSupplierId?: number;
  selectedSupplierName?: string;
};

/**
 * 资源绑定的供应商列表如果没有包含历史值，也要把历史值保留在下拉里，
 * 这样旧数据在编辑态不会被前端误清空。
 */
export function resolveSupplierOptionsForResource(
  input: SupplierOptionResolutionInput,
): SupplierOptionResolutionResult {
  const historicalName = input.currentSupplierName?.trim();
  const mergedOptions = ensureSelectOption(input.nextOptions, historicalName, input.currentSupplierId);
  const currentOptionById = input.currentSupplierId !== undefined
    ? mergedOptions.find((item) => item.id === input.currentSupplierId)
    : undefined;
  const currentOptionByName = historicalName
    ? mergedOptions.find((item) => item.value === historicalName)
    : undefined;
  const selectedOption = currentOptionById
    || currentOptionByName
    || mergedOptions[0];

  return {
    options: mergedOptions,
    selectedSupplierId: input.currentSupplierId ?? selectedOption?.id,
    selectedSupplierName: historicalName || selectedOption?.value,
  };
}

/** 按开始/结束天数计算跨几天，老系统口径包含首尾日期。 */
export function scheduleInclusiveDaysCount(startValue?: string, endValue?: string) {
  const start = parseScheduleDayNo(startValue) || 1;
  const end = Math.max(start, parseScheduleDayNo(endValue) || start);
  return Math.max(1, end - start + 1);
}

/** 住宿晚数按退房日减入住日计算，例如第1天入住、第3天退房等于2晚。 */
export function scheduleExclusiveNightsCount(startValue?: string, endValue?: string) {
  const start = parseScheduleDayNo(startValue) || 1;
  const end = Math.max(start, parseScheduleDayNo(endValue) || start + 1);
  return Math.max(0, end - start);
}

export function routeDurationText(seconds?: number) {
  const totalSeconds = Number(seconds || 0);
  if (totalSeconds <= 0) return '0分钟';
  const minutes = Math.round(totalSeconds / 60);
  if (minutes < 60) return `${minutes}分钟`;
  return `${Math.floor(minutes / 60)}小时${minutes % 60}分钟`;
}

export function vehicleDistanceText(meters?: number) {
  const value = Number(meters || 0);
  return `${(value / 1000).toFixed(1)}公里`;
}
