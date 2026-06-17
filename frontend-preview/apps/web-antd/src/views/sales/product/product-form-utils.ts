import type { SalesProductApi } from '#/api/sales/product';
import type { RegionPath } from '#/utils/region';

import { splitRegionPath } from '#/utils/region';

export type ProductFormState = SalesProductApi.SaveParams;

export type ArrangementCostBucket = {
  cash: number;
  credit: number;
  total: number;
};

export type ArrangementOverviewSummary = {
  byType: Record<SalesProductApi.ArrangementType, ArrangementCostBucket>;
  extraColumns: {
    guideService: number;
    operationFee: number;
    reserveFund: number;
    selfPayIncome: number;
  };
  total: ArrangementCostBucket;
};

export const oldSystemArrangementTypes: SalesProductApi.ArrangementType[] = [
  'traffic',
  'hotel',
  'vehicle',
  'scenic',
  'meal',
  'other',
  'ground_agent',
  'extra_fee',
  'optional',
  'shopping',
];

/**
 * 创建产品表单默认值。
 *
 * 产品新增和修改页共用这份默认值，避免列表页、编辑页各自维护一套初始字段后保存口径不一致。
 */
export function createDefaultProductForm(): ProductFormState {
  return {
    arrangementItems: createDefaultArrangementTemplate(),
    domesticInternational: 'domestic',
    itineraryDays: [createDefaultItineraryDay(1)],
    productName: '',
    status: 'active',
    travelDays: 1,
    tripType: 'irregular',
  };
}

/**
 * 新增产品时默认展示的团队安排模板行。
 *
 * 这些行只是帮助用户理解该页签应维护什么内容；保存时仍会过滤项目名称为空的行。
 */
export function createDefaultArrangementTemplate(): SalesProductApi.ArrangementItem[] {
  return [
    {
      arrangementContent: '维护默认大交通安排参考',
      arrangementType: 'traffic',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '项',
      unitPrice: 0,
    },
    {
      arrangementContent: '按接待标准选择酒店和房型',
      arrangementType: 'hotel',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '晚',
      unitPrice: 0,
    },
    {
      arrangementContent: '按团队人数和路线选择车型',
      arrangementType: 'vehicle',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '辆',
      unitPrice: 0,
    },
    {
      arrangementContent: '维护默认景区和票种参考',
      arrangementType: 'scenic',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '人',
      unitPrice: 0,
    },
    {
      arrangementContent: '维护默认用餐标准参考',
      arrangementType: 'meal',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '餐',
      unitPrice: 0,
    },
    {
      arrangementContent: '维护其它杂项费用参考',
      arrangementType: 'other',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '项',
      unitPrice: 0,
    },
    {
      arrangementContent: '维护自费项目和销售价参考',
      arrangementType: 'optional',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '人',
      unitPrice: 0,
    },
    {
      arrangementContent: '维护购物店和进店参考',
      arrangementType: 'shopping',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '次',
      unitPrice: 0,
    },
    {
      arrangementContent: '维护地接供应商确认参考',
      arrangementType: 'ground_agent',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '团',
      unitPrice: 0,
    },
    {
      arrangementContent: '维护附加费用参考',
      arrangementType: 'extra_fee',
      itemName: '',
      quantity: 1,
      settlementType: 'credit',
      unitName: '项',
      unitPrice: 0,
    },
  ];
}

/**
 * 创建默认行程天。
 *
 * @param dayNo 行程序号，从 1 开始。
 */
export function createDefaultItineraryDay(dayNo: number): SalesProductApi.ItineraryDay {
  return {
    breakfastIncluded: false,
    dayNo,
    dinnerIncluded: false,
    lunchIncluded: false,
    seasonalSurcharge: 0,
  };
}

/**
 * 按旅游天数补齐行程天数。
 *
 * 老系统录入 3 天产品后，行程内容会直接出现第 1-3 天。这里也保持这个操作口径：
 * 天数增加时自动追加空白行程；天数减少时截到对应天数。行程页不提供单独新增/删除，
 * 旅游天数就是唯一控制入口，避免行程天数和基本信息不一致。
 */
export function syncItineraryDaysWithTravelDays(
  itineraryDays: SalesProductApi.ItineraryDay[] | undefined,
  travelDays: number | undefined,
) {
  const normalizedTravelDays = Math.max(1, Math.floor(Number(travelDays || 1)));
  const days = [...(itineraryDays?.length ? itineraryDays : [createDefaultItineraryDay(1)])]
    .slice(0, normalizedTravelDays)
    .map((item, index) => ({
      ...item,
      dayNo: index + 1,
    }));

  for (let dayNo = days.length + 1; dayNo <= normalizedTravelDays; dayNo += 1) {
    days.push(createDefaultItineraryDay(dayNo));
  }

  return days.map((item, index) => ({
    ...item,
    dayNo: index + 1,
  }));
}

/**
 * 创建默认团队安排项。
 *
 * 这里是产品模板中的参考安排，不代表正式计调排房、派车或订票已经履约。
 */
export function createDefaultArrangementItem(): SalesProductApi.ArrangementItem {
  return {
    arrangementType: 'hotel',
    itemName: '',
    quantity: 1,
    settlementType: 'credit',
    unitPrice: 0,
  };
}

/**
 * 清理字符串字段。
 *
 * 空字符串统一保存为 undefined，避免后端收到一堆没有业务意义的空白文本。
 */
export function cleanProductText(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

/**
 * 计算团队安排参考费用合计。
 *
 * 该金额只用于产品模板参考，不写入团队真实成本；真实成本以后由计调、财务链路落地。
 */
export function calculateArrangementTotal(items?: SalesProductApi.ArrangementItem[]) {
  return (items || []).reduce(
    (sum, item) => sum + Number(item.quantity || 0) * Number(item.unitPrice || 0),
    0,
  );
}

/**
 * 按老系统团队安排总览口径汇总产品模板费用。
 *
 * 老系统总览会区分每类资源的现付和挂账，并单独展示自费收入、导服、操作费、备用金等列。
 * 产品模板阶段没有导服/操作费/备用金字段，所以这些列固定为 0。
 */
export function createArrangementOverviewSummary(
  items?: SalesProductApi.ArrangementItem[],
): ArrangementOverviewSummary {
  const emptyBucket = (): ArrangementCostBucket => ({ cash: 0, credit: 0, total: 0 });
  const byType = oldSystemArrangementTypes.reduce(
    (result, type) => {
      result[type] = emptyBucket();
      return result;
    },
    {} as Record<SalesProductApi.ArrangementType, ArrangementCostBucket>,
  );

  for (const item of items || []) {
    const type = item.arrangementType;
    if (!byType[type]) {
      byType[type] = emptyBucket();
    }
    const amount = Number(item.quantity || 0) * Number(item.unitPrice || 0);
    if (item.settlementType === 'cash') {
      byType[type].cash += amount;
    } else {
      byType[type].credit += amount;
    }
    byType[type].total += amount;
  }

  const total = Object.values(byType).reduce(
    (result, item) => ({
      cash: result.cash + item.cash,
      credit: result.credit + item.credit,
      total: result.total + item.total,
    }),
    emptyBucket(),
  );

  return {
    byType,
    extraColumns: {
      guideService: 0,
      operationFee: 0,
      reserveFund: 0,
      selfPayIncome: byType.optional?.total || 0,
    },
    total,
  };
}

/**
 * 将产品编辑表单转换为后端保存参数。
 *
 * @param formState 页面表单状态。
 * @param regionPath 所在地级联选择器路径，可只选省、市或区县。
 */
export function buildSalesProductPayload(
  formState: ProductFormState,
  regionPath: RegionPath,
): SalesProductApi.SaveParams {
  const regionFields = splitRegionPath(regionPath);
  return {
    arrangementItems: (formState.arrangementItems || [])
      .filter((item) => item.itemName?.trim())
      .map((item) => ({
        arrangementContent: cleanProductText(item.arrangementContent),
        arrangementType: item.arrangementType,
        itemName: item.itemName.trim(),
        quantity: Number(item.quantity || 0),
        remark: cleanProductText(item.remark),
        settlementType: item.settlementType || 'credit',
        unitName: cleanProductText(item.unitName),
        unitPrice: Number(item.unitPrice || 0),
      })),
    attentionItems: cleanProductText(formState.attentionItems),
    bookingNotice: cleanProductText(formState.bookingNotice),
    businessType: cleanProductText(formState.businessType),
    childPolicy: cleanProductText(formState.childPolicy),
    city: regionFields.city,
    closeDaysBefore: Number(formState.closeDaysBefore || 0),
    district: regionFields.district,
    domesticInternational: formState.domesticInternational || 'domestic',
    feeExcluded: cleanProductText(formState.feeExcluded),
    feeIncluded: cleanProductText(formState.feeIncluded),
    giftItems: cleanProductText(formState.giftItems),
    itineraryDays: (formState.itineraryDays || []).map((item, index) => ({
      accommodationNote: cleanProductText(item.accommodationNote),
      breakfastIncluded: Boolean(item.breakfastIncluded),
      dayNo: index + 1,
      dayTitle: cleanProductText(item.dayTitle),
      dinnerIncluded: Boolean(item.dinnerIncluded),
      itineraryContent: cleanProductText(item.itineraryContent),
      lunchIncluded: Boolean(item.lunchIncluded),
      relatedHotel: cleanProductText(item.relatedHotel),
      roadbookPlace: cleanProductText(item.roadbookPlace),
      roadbookPoints: (item.roadbookPoints || [])
        .filter((point) => point.placeName?.trim())
        .map((point, pointIndex) => ({
          address: cleanProductText(point.address),
          distanceToNextMeters: Number(point.distanceToNextMeters || 0),
          durationToNextSeconds: Number(point.durationToNextSeconds || 0),
          latitude: cleanProductText(point.latitude),
          longitude: cleanProductText(point.longitude),
          placeName: point.placeName.trim(),
          pointOrder: pointIndex + 1,
          pointType: point.pointType || 'waypoint',
          remark: cleanProductText(point.remark),
          stayMinutes: Number(point.stayMinutes || 0),
        })),
      roadbookSummary: cleanProductText(item.roadbookSummary),
      roadbookTotalDistanceMeters: Number(item.roadbookTotalDistanceMeters || 0),
      roadbookTotalDurationSeconds: Number(item.roadbookTotalDurationSeconds || 0),
      seasonalSurcharge: Number(item.seasonalSurcharge || 0),
    })),
    optionalItems: cleanProductText(formState.optionalItems),
    plannedCapacity: Number(formState.plannedCapacity || 0),
    productDescription: cleanProductText(formState.productDescription),
    productName: formState.productName.trim(),
    productTheme: cleanProductText(formState.productTheme),
    province: regionFields.province,
    receptionStandard: cleanProductText(formState.receptionStandard),
    remark: cleanProductText(formState.remark),
    shoppingArrangement: cleanProductText(formState.shoppingArrangement),
    singleRoomDifference: Number(formState.singleRoomDifference || 0),
    status: formState.status || 'active',
    travelDays: Number(formState.travelDays || 1),
    tripType: formState.tripType || 'irregular',
    warmReminder: cleanProductText(formState.warmReminder),
  };
}
