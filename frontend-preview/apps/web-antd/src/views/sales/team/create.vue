<script lang="ts" setup>
import type { EnterpriseProductDictionaryApi } from '#/api/enterprise/product-dictionary';
import type { SalesProductApi } from '#/api/sales/product';
import type { SalesTeamApi } from '#/api/sales/team';
import type { RegionPath } from '#/utils/region';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Cascader,
  Checkbox,
  DatePicker,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Spin,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getProductDictionaryAll } from '#/api/enterprise/product-dictionary';
import { getControlledRoomResourceAll } from '#/api/dispatch/room-status';
import { getHotelResourcePage } from '#/api/purchase/hotel';
import {
  calculateRoadbookRoute,
  getAmapJsConfig,
  searchAmapTips,
} from '#/api/sales/product';
import {
  createSalesTeam,
  getSalesTeamEditDetail,
  updateSalesTeam,
} from '#/api/sales/team';
import BusinessPillTabs from '#/components/business/BusinessPillTabs.vue';
import { buildRegionOptions, buildRegionPath, splitRegionPath } from '#/utils/region';

import {
  cleanProductText,
  createDefaultItineraryDay,
  syncItineraryDaysWithTravelDays,
} from '../product/product-form-utils';

type DictItem = EnterpriseProductDictionaryApi.Item;
type SelectOption = { label: string; value: string };
type CreateTeamType = Exclude<SalesTeamApi.TeamType, 'single'>;
type RoadbookPoint = SalesProductApi.RoadbookPoint;

const route = useRoute();
const router = useRouter();
const regionOptions = buildRegionOptions();
const ROADBOOK_SEARCH_DEBOUNCE_MS = 700;

const teamTypeOptions: Array<{ label: string; value: CreateTeamType }> = [
  { label: '散拼', value: 'sanpin' },
  { label: '整团', value: 'zhengtuan' },
  { label: '散团', value: 'santuan' },
];

const editTabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'itinerary', label: '行程内容' },
  { key: 'description', label: '产品说明' },
  { key: 'arrangement', label: '团队安排' },
];

const domesticOptions = [
  { label: '国内', value: 'domestic' },
  { label: '国际', value: 'international' },
];

const tripTypeOptions = [
  { label: '每天发', value: 'daily' },
  { label: '每周发', value: 'weekly' },
  { label: '不定期', value: 'irregular' },
];

const dictionaryLoading = ref(false);
const relatedHotelLoading = ref(false);
const roadbookCalculating = ref(false);
const saving = ref(false);
const activeEditTab = ref('basic');
const formRegionPath = ref<RegionPath>([]);
const businessTypes = ref<DictItem[]>([]);
const receptionStandards = ref<DictItem[]>([]);
const productThemes = ref<DictItem[]>([]);
const relatedHotelOptions = ref<SelectOption[]>([]);
const roadbookDrawerOpen = ref(false);
const activeRoadbookDayIndex = ref<number>();
const roadbookKeyword = ref('');
const roadbookTipOptions = ref<Array<SelectOption & { meta: SalesProductApi.AmapTip }>>([]);
const roadbookTipLoading = ref(false);
const roadbookMapLoading = ref(false);
const roadbookMapReady = ref(false);
const roadbookMapError = ref('');
const roadbookMapContainerRef = ref<HTMLDivElement>();
let amapInstance: any;
let amapPolyline: any;
let amapGeocoder: any;
let amapLoaderPromise: Promise<any> | undefined;
let roadbookSearchTimer: number | undefined;
const amapMarkers: any[] = [];

const roadbookPointTypeOptions = [
  { label: '出发', value: 'departure' },
  { label: '途经', value: 'waypoint' },
  { label: '景区', value: 'scenic' },
  { label: '用餐', value: 'meal' },
  { label: '购物', value: 'shopping' },
  { label: '酒店', value: 'hotel' },
  { label: '结束', value: 'arrival' },
];

const formState = reactive<SalesTeamApi.DirectCreateParams>({
  closeDaysBefore: 0,
  departureDate: '',
  domesticInternational: 'domestic',
  itineraryDays: [createDefaultItineraryDay(1)],
  singleRoomDifference: 0,
  teamName: '',
  teamType: normalizeTeamType(route.params.type),
  totalSeats: 0,
  travelDays: 1,
  tripType: 'irregular',
});

const editTeamId = computed(() => Number(route.params.id || 0));
const isEditMode = computed(() => editTeamId.value > 0 && route.path.includes('/sales/team/edit/'));
const pageTitle = computed(() => `${isEditMode.value ? '修改' : '新增'}${teamTypeLabel(formState.teamType)}`);
const businessTypeOptions = computed(() => dictionaryOptions(businessTypes.value));
const receptionStandardOptions = computed(() => dictionaryOptions(receptionStandards.value));
const productThemeOptions = computed(() => dictionaryOptions(productThemes.value));
const activeRoadbookDay = computed(() => (
  activeRoadbookDayIndex.value === undefined ? undefined : formState.itineraryDays?.[activeRoadbookDayIndex.value]
));
const roadbookDaySwitchOptions = computed(() => (formState.itineraryDays || []).map((day, index) => ({
  label: `第 ${day.dayNo} 天${day.dayTitle ? `｜${day.dayTitle}` : ''}`,
  value: index,
})));

function normalizeTeamType(value: unknown): CreateTeamType {
  const raw = Array.isArray(value) ? value[0] : value;
  return teamTypeOptions.some((item) => item.value === raw) ? raw as CreateTeamType : 'sanpin';
}

function teamTypeLabel(value?: string) {
  return teamTypeOptions.find((item) => item.value === value)?.label || '团队';
}

function dictionaryOptions(items: DictItem[]): SelectOption[] {
  return items.map((item) => ({
    label: item.dictName,
    value: item.dictName,
  }));
}

function formatHotelLocation(item: { city?: string; district?: string; province?: string }) {
  return [item.province, item.city, item.district].filter(Boolean).join(' / ') || '未填地区';
}

function uniqueHotelOptions(options: SelectOption[]) {
  const seen = new Set<string>();
  return options.filter((item) => {
    if (seen.has(item.value)) return false;
    seen.add(item.value);
    return true;
  });
}

async function loadDictionaries() {
  dictionaryLoading.value = true;
  try {
    const [business, standards, themes] = await Promise.all([
      getProductDictionaryAll('business_type'),
      getProductDictionaryAll('reception_standard'),
      getProductDictionaryAll('product_theme'),
    ]);
    businessTypes.value = business;
    receptionStandards.value = standards;
    productThemes.value = themes;
  } finally {
    dictionaryLoading.value = false;
  }
}

/**
 * 加载行程关联酒店候选项。
 *
 * 团队直接创建页复用产品行程录入口径，关联酒店仍从自营房源和采购酒店资源中选择。
 */
async function loadRelatedHotelOptions() {
  relatedHotelLoading.value = true;
  try {
    const [controlledResources, purchasedResult] = await Promise.all([
      getControlledRoomResourceAll(false),
      getHotelResourcePage({ page: 1, pageSize: 200, status: 'active' }),
    ]);

    const controlledOptions = controlledResources.map((item) => ({
      label: `自营｜${item.hotelName}｜${formatHotelLocation(item)}｜${item.starStandard || '未设标准'}`,
      value: item.hotelName,
    }));
    const purchasedOptions = purchasedResult.items.map((item) => ({
      label: `采购｜${item.hotelName}｜${[item.city, item.area].filter(Boolean).join(' / ') || '未填地区'}｜${item.roomType || '未填房型'}`,
      value: item.hotelName,
    }));

    relatedHotelOptions.value = uniqueHotelOptions([...controlledOptions, ...purchasedOptions]);
  } catch (error) {
    relatedHotelOptions.value = [];
    message.warning('关联酒店候选加载失败，可先手动输入后保存');
  } finally {
    relatedHotelLoading.value = false;
  }
}

function formatDistance(meters?: number) {
  const value = Number(meters || 0);
  if (value <= 0) return '0 公里';
  return `${(value / 1000).toFixed(value >= 10_000 ? 0 : 1)} 公里`;
}

function formatDuration(seconds?: number) {
  const value = Number(seconds || 0);
  if (value <= 0) return '0 分钟';
  const minutes = Math.round(value / 60);
  if (minutes < 60) return `${minutes} 分钟`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest ? `${hours}小时${rest}分钟` : `${hours}小时`;
}

function roadbookSummaryText(day: SalesProductApi.ItineraryDay) {
  if (day.roadbookSummary) {
    return day.roadbookSummary;
  }
  if (day.roadbookPoints?.length) {
    return day.roadbookPoints.map((point) => point.placeName).join(' -> ');
  }
  return '未维护路书';
}

function roadbookPointCountText(day: SalesProductApi.ItineraryDay) {
  const pointCount = day.roadbookPoints?.length || 0;
  return pointCount ? `${pointCount} 个地点` : '未选点';
}

function roadbookDistanceText(day: SalesProductApi.ItineraryDay) {
  return formatDistance(day.roadbookTotalDistanceMeters);
}

function roadbookDurationText(day: SalesProductApi.ItineraryDay) {
  return `约 ${formatDuration(day.roadbookTotalDurationSeconds)}`;
}

function ensureRoadbookPoints(day: SalesProductApi.ItineraryDay) {
  if (!day.roadbookPoints) {
    day.roadbookPoints = [];
  }
  return day.roadbookPoints;
}

function openRoadbookDrawer(index: number) {
  activeRoadbookDayIndex.value = index;
  roadbookKeyword.value = '';
  roadbookTipOptions.value = [];
  ensureRoadbookPoints(formState.itineraryDays?.[index] || createDefaultItineraryDay(index + 1));
  roadbookDrawerOpen.value = true;
  nextTick(() => {
    initRoadbookMap();
  });
}

function switchRoadbookDay(index?: number) {
  if (index === undefined) return;
  activeRoadbookDayIndex.value = Number(index);
  roadbookKeyword.value = '';
  roadbookTipOptions.value = [];
  const day = formState.itineraryDays?.[Number(index)];
  if (day) {
    ensureRoadbookPoints(day);
  }
  nextTick(() => {
    renderRoadbookMapPoints();
  });
}

function closeRoadbookDrawer() {
  roadbookDrawerOpen.value = false;
  activeRoadbookDayIndex.value = undefined;
  clearRoadbookSearchTimer();
  destroyRoadbookMap();
}

function normalizeRoadbookOrders(points: RoadbookPoint[]) {
  points.forEach((point, index) => {
    point.pointOrder = index + 1;
  });
}

function removeRoadbookPoint(index: number) {
  const day = activeRoadbookDay.value;
  if (!day?.roadbookPoints) return;
  day.roadbookPoints.splice(index, 1);
  normalizeRoadbookOrders(day.roadbookPoints);
  updateRoadbookSummary(day);
  renderRoadbookMapPoints();
}

function moveRoadbookPoint(index: number, direction: -1 | 1) {
  const day = activeRoadbookDay.value;
  const points = day?.roadbookPoints;
  if (!points) return;
  const target = index + direction;
  if (target < 0 || target >= points.length) return;
  const current = points[index];
  const next = points[target];
  if (!current || !next) return;
  points[index] = next;
  points[target] = current;
  normalizeRoadbookOrders(points);
  updateRoadbookSummary(day);
  renderRoadbookMapPoints();
}

function updateRoadbookSummary(day: SalesProductApi.ItineraryDay) {
  const points = day.roadbookPoints || [];
  day.roadbookSummary = points.map((point) => point.placeName).filter(Boolean).join(' -> ');
  day.roadbookPlace = day.roadbookSummary;
}

async function loadAmapScript() {
  if ((window as any).AMap) {
    return (window as any).AMap;
  }
  if (amapLoaderPromise) {
    return amapLoaderPromise;
  }
  amapLoaderPromise = getAmapJsConfig().then((config) => new Promise((resolve, reject) => {
    if (!config.key) {
      reject(new Error('未配置高德 JS Key'));
      return;
    }
    if (config.securityJsCode) {
      (window as any)._AMapSecurityConfig = { securityJsCode: config.securityJsCode };
    }
    const script = document.createElement('script');
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(config.key)}&plugin=AMap.Geocoder`;
    script.async = true;
    script.onload = () => resolve((window as any).AMap);
    script.onerror = () => reject(new Error('高德地图脚本加载失败'));
    document.head.append(script);
  }));
  return amapLoaderPromise;
}

async function initRoadbookMap() {
  if (!roadbookMapContainerRef.value) return;
  roadbookMapLoading.value = true;
  roadbookMapError.value = '';
  try {
    const AMap = await loadAmapScript();
    if (!amapInstance) {
      amapInstance = new AMap.Map(roadbookMapContainerRef.value, {
        center: [120.14895, 30.24490],
        resizeEnable: true,
        viewMode: '2D',
        zoom: 11,
      });
      amapGeocoder = new AMap.Geocoder();
      amapInstance.on('click', handleRoadbookMapClick);
    }
    roadbookMapReady.value = true;
    renderRoadbookMapPoints();
  } catch (error) {
    roadbookMapReady.value = false;
    roadbookMapError.value = '地图加载失败，请检查高德 Web端 JS API Key 和安全密钥配置。';
  } finally {
    roadbookMapLoading.value = false;
  }
}

function clearRoadbookMapOverlays() {
  if (!amapInstance) return;
  amapMarkers.forEach((marker) => amapInstance.remove(marker));
  amapMarkers.splice(0);
  if (amapPolyline) {
    amapInstance.remove(amapPolyline);
    amapPolyline = undefined;
  }
}

function destroyRoadbookMap() {
  if (!amapInstance) {
    return;
  }
  clearRoadbookMapOverlays();
  amapInstance.off?.('click', handleRoadbookMapClick);
  amapInstance.destroy?.();
  amapInstance = undefined;
  amapGeocoder = undefined;
  roadbookMapReady.value = false;
  roadbookMapLoading.value = false;
}

function renderRoadbookMapPoints() {
  if (!amapInstance || !(window as any).AMap) return;
  const AMap = (window as any).AMap;
  const points = (activeRoadbookDay.value?.roadbookPoints || []).filter((point) => point.longitude && point.latitude);
  clearRoadbookMapOverlays();
  if (!points.length) return;
  const positions = points.map((point) => [Number(point.longitude), Number(point.latitude)]);
  points.forEach((point, index) => {
    const marker = new AMap.Marker({
      label: {
        content: `${index + 1}. ${point.placeName}`,
        direction: 'top',
      },
      position: positions[index],
      title: point.placeName,
    });
    amapMarkers.push(marker);
    amapInstance.add(marker);
  });
  if (positions.length >= 2) {
    amapPolyline = new AMap.Polyline({
      path: positions,
      strokeColor: '#1677ff',
      strokeOpacity: 0.9,
      strokeWeight: 6,
    });
    amapInstance.add(amapPolyline);
  }
  amapInstance.setFitView(amapMarkers);
}

function addMapPointFromLngLat(lng: number, lat: number, placeName: string, address?: string) {
  const day = activeRoadbookDay.value;
  if (!day) return;
  const points = ensureRoadbookPoints(day);
  points.push({
    address,
    latitude: String(lat),
    longitude: String(lng),
    placeName,
    pointOrder: points.length + 1,
    pointType: points.length === 0 ? 'departure' : 'arrival',
    stayMinutes: 0,
  });
  if (points.length > 2) {
    const previousPoint = points[points.length - 2];
    if (previousPoint?.pointType === 'arrival') {
      previousPoint.pointType = 'waypoint';
    }
  }
  normalizeRoadbookOrders(points);
  updateRoadbookSummary(day);
  renderRoadbookMapPoints();
}

function handleRoadbookMapClick(event: any) {
  const lng = event.lnglat.getLng();
  const lat = event.lnglat.getLat();
  if (!amapGeocoder) {
    addMapPointFromLngLat(lng, lat, `地图选点 ${lng.toFixed(5)},${lat.toFixed(5)}`);
    return;
  }
  amapGeocoder.getAddress([lng, lat], (status: string, result: any) => {
    const address = status === 'complete' ? result?.regeocode?.formattedAddress : '';
    addMapPointFromLngLat(lng, lat, address || `地图选点 ${lng.toFixed(5)},${lat.toFixed(5)}`, address);
  });
}

async function handleRoadbookSearch(value: string) {
  roadbookKeyword.value = value;
  clearRoadbookSearchTimer();
  if (!value?.trim()) {
    roadbookTipOptions.value = [];
    roadbookTipLoading.value = false;
    return;
  }
  roadbookTipLoading.value = true;
  roadbookSearchTimer = window.setTimeout(() => {
    doRoadbookSearch(value);
  }, ROADBOOK_SEARCH_DEBOUNCE_MS);
}

function clearRoadbookSearchTimer() {
  if (roadbookSearchTimer !== undefined) {
    window.clearTimeout(roadbookSearchTimer);
    roadbookSearchTimer = undefined;
  }
}

async function doRoadbookSearch(value: string) {
  const keyword = value.trim();
  if (!keyword || keyword !== roadbookKeyword.value.trim()) {
    roadbookTipLoading.value = false;
    return;
  }
  roadbookTipLoading.value = true;
  try {
    const tips = await searchAmapTips({
      city: formState.city,
      keywords: keyword,
    });
    if (keyword !== roadbookKeyword.value.trim()) {
      return;
    }
    roadbookTipOptions.value = tips.map((item) => ({
      label: `${item.name}${item.district ? ` / ${item.district}` : ''}${item.address ? ` / ${item.address}` : ''}`,
      meta: item,
      value: `${item.longitude},${item.latitude},${item.name}`,
    }));
  } catch (error) {
    roadbookTipOptions.value = [];
    message.warning('地点搜索太频繁或暂时不可用，请稍后再试');
  } finally {
    roadbookTipLoading.value = false;
    roadbookSearchTimer = undefined;
  }
}

function addRoadbookPoint(value: string) {
  const day = activeRoadbookDay.value;
  if (!day) return;
  const option = roadbookTipOptions.value.find((item) => item.value === value);
  const tip = option?.meta;
  if (!tip?.longitude || !tip.latitude) {
    message.warning('请选择带经纬度的地图地点');
    return;
  }
  const points = ensureRoadbookPoints(day);
  points.push({
    address: tip.address,
    latitude: tip.latitude,
    longitude: tip.longitude,
    placeName: tip.name,
    pointOrder: points.length + 1,
    pointType: points.length === 0 ? 'departure' : 'waypoint',
    stayMinutes: 0,
  });
  if (points.length > 1) {
    const lastPoint = points[points.length - 1];
    if (lastPoint) {
      lastPoint.pointType = 'arrival';
    }
    if (points.length > 2) {
      const previousPoint = points[points.length - 2];
      if (previousPoint?.pointType === 'arrival') {
        previousPoint.pointType = 'waypoint';
      }
    }
  }
  roadbookKeyword.value = '';
  roadbookTipOptions.value = [];
  updateRoadbookSummary(day);
  renderRoadbookMapPoints();
}

async function calculateActiveRoadbookRoute() {
  const day = activeRoadbookDay.value;
  if (!day) return;
  const points = day?.roadbookPoints || [];
  if (points.length < 2) {
    message.warning('至少选择两个地点才能计算路线');
    return;
  }
  if (points.some((point) => !point.longitude || !point.latitude)) {
    message.warning('路书地点缺少经纬度，无法计算路线');
    return;
  }
  roadbookCalculating.value = true;
  try {
    const result = await calculateRoadbookRoute({
      points: points.map((point) => ({
        latitude: point.latitude || '',
        longitude: point.longitude || '',
      })),
    });
    points.forEach((point, index) => {
      const segment = result.segments[index];
      point.distanceToNextMeters = segment?.distanceMeters || 0;
      point.durationToNextSeconds = segment?.durationSeconds || 0;
    });
    day.roadbookTotalDistanceMeters = result.totalDistanceMeters;
    day.roadbookTotalDurationSeconds = result.totalDurationSeconds;
    updateRoadbookSummary(day);
    renderRoadbookMapPoints();
    message.success('路书公里和车程已计算');
  } finally {
    roadbookCalculating.value = false;
  }
}

function handleRoadbookSelect(value: unknown) {
  if (typeof value === 'string') {
    addRoadbookPoint(value);
  }
}

function syncItineraryDays() {
  formState.itineraryDays = syncItineraryDaysWithTravelDays(
    formState.itineraryDays,
    formState.travelDays,
  );
}

function applyEditDetail(detail: SalesTeamApi.DirectEditDetail) {
  formRegionPath.value = buildRegionPath(detail.province, detail.city, detail.district);
  Object.assign(formState, {
    attentionItems: detail.attentionItems,
    bookingNotice: detail.bookingNotice,
    businessType: detail.businessType,
    childPolicy: detail.childPolicy,
    city: detail.city,
    closeDaysBefore: detail.closeDaysBefore ?? 0,
    departureDate: detail.departureDate || '',
    district: detail.district,
    domesticInternational: detail.domesticInternational || 'domestic',
    feeExcluded: detail.feeExcluded,
    feeIncluded: detail.feeIncluded,
    giftItems: detail.giftItems,
    itineraryDays: detail.itineraryDays?.length ? detail.itineraryDays : [createDefaultItineraryDay(1)],
    optionalItems: detail.optionalItems,
    productDescription: detail.productDescription,
    productTheme: detail.productTheme,
    province: detail.province,
    receptionStandard: detail.receptionStandard,
    remark: detail.remark,
    shoppingArrangement: detail.shoppingArrangement,
    singleRoomDifference: detail.singleRoomDifference ?? 0,
    teamName: detail.teamName || '',
    teamType: normalizeTeamType(detail.teamType),
    totalSeats: detail.totalSeats ?? 0,
    travelDays: detail.travelDays || 1,
    tripType: detail.tripType || 'irregular',
    warmReminder: detail.warmReminder,
  });
  syncItineraryDays();
}

async function loadEditDetail() {
  if (!isEditMode.value) return;
  const detail = await getSalesTeamEditDetail(editTeamId.value);
  applyEditDetail(detail);
}

function handleTravelDaysChange() {
  syncItineraryDays();
}

function toggleSeasonalSurcharge(day: SalesProductApi.ItineraryDay, checked: boolean) {
  day.seasonalSurcharge = checked ? Math.max(Number(day.seasonalSurcharge || 0), 1) : 0;
}

function handleEditTabChange(key: string) {
  if (key === 'arrangement') {
    saveTeam({ openArrangement: true });
  } else {
    activeEditTab.value = key;
  }
}

function validateForm() {
  if (!formState.teamName?.trim()) {
    activeEditTab.value = 'basic';
    message.warning('请填写团队名称');
    return false;
  }
  if (!formState.departureDate) {
    activeEditTab.value = 'basic';
    message.warning('请选择发团日期');
    return false;
  }
  if (!formState.travelDays || formState.travelDays < 1) {
    activeEditTab.value = 'basic';
    message.warning('旅游天数不能小于1');
    return false;
  }
  return true;
}

function cleanItineraryDays(days?: SalesProductApi.ItineraryDay[]) {
  return (days || []).map((day, index) => ({
    accommodationNote: cleanProductText(day.accommodationNote),
    breakfastIncluded: Boolean(day.breakfastIncluded),
    dayNo: index + 1,
    dayTitle: cleanProductText(day.dayTitle),
    dinnerIncluded: Boolean(day.dinnerIncluded),
    itineraryContent: cleanProductText(day.itineraryContent),
    lunchIncluded: Boolean(day.lunchIncluded),
    relatedHotel: cleanProductText(day.relatedHotel),
    roadbookPlace: cleanProductText(day.roadbookPlace),
    roadbookPoints: (day.roadbookPoints || [])
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
    roadbookSummary: cleanProductText(day.roadbookSummary),
    roadbookTotalDistanceMeters: Number(day.roadbookTotalDistanceMeters || 0),
    roadbookTotalDurationSeconds: Number(day.roadbookTotalDurationSeconds || 0),
    seasonalSurcharge: Number(day.seasonalSurcharge || 0),
  }));
}

async function saveTeam(options: { openArrangement?: boolean } = {}) {
  if (!validateForm()) return;
  syncItineraryDays();
  const regionFields = splitRegionPath(formRegionPath.value);
  const payload: SalesTeamApi.DirectCreateParams = {
    ...formState,
    attentionItems: cleanProductText(formState.attentionItems),
    bookingNotice: cleanProductText(formState.bookingNotice),
    childPolicy: cleanProductText(formState.childPolicy),
    feeExcluded: cleanProductText(formState.feeExcluded),
    feeIncluded: cleanProductText(formState.feeIncluded),
    giftItems: cleanProductText(formState.giftItems),
    itineraryDays: cleanItineraryDays(formState.itineraryDays),
    optionalItems: cleanProductText(formState.optionalItems),
    productDescription: cleanProductText(formState.productDescription),
    city: regionFields.city,
    district: regionFields.district,
    province: regionFields.province,
    remark: cleanProductText(formState.remark),
    shoppingArrangement: cleanProductText(formState.shoppingArrangement),
    teamName: formState.teamName.trim(),
    warmReminder: cleanProductText(formState.warmReminder),
  };
  saving.value = true;
  try {
    const result = isEditMode.value
      ? await updateSalesTeam(editTeamId.value, payload)
      : await createSalesTeam(payload);
    message.success(isEditMode.value ? '团队修改成功' : `${teamTypeLabel(formState.teamType)}创建成功`);
    if (options.openArrangement && isEditMode.value) {
      router.push(`/sales/team/arrangement/${editTeamId.value}`);
      return;
    }
    if (options.openArrangement && result.id) {
      router.push(`/sales/team/arrangement/${result.id}`);
      return;
    }
    if (isEditMode.value) {
      router.push(`/sales/team/operation/${editTeamId.value}`);
      return;
    }
    router.push(result.id ? `/sales/team/operation/${result.id}` : '/sales/team');
  } finally {
    saving.value = false;
  }
}

function goBack() {
  router.push('/sales/team');
}

onMounted(async () => {
  await Promise.all([loadDictionaries(), loadRelatedHotelOptions(), loadEditDetail()]);
});
</script>

<template>
  <Page :title="pageTitle">
    <Spin :spinning="dictionaryLoading">
      <Card class="team-create-card" :bordered="false">
        <div class="form-header">
          <div class="form-title">添加/修改团队</div>
          <Space>
            <Button @click="goBack">返回列表</Button>
            <Button type="primary" :loading="saving" @click="saveTeam()">保存团队</Button>
          </Space>
        </div>

        <BusinessPillTabs
          :active-key="activeEditTab"
          aria-label="团队创建页签"
          :tabs="editTabs"
          @change="handleEditTabChange"
        />

        <Form :model="formState" layout="vertical">
          <div v-show="activeEditTab === 'basic'" class="form-panel">
            <div class="matrix-layout">
              <aside class="side-label">基本信息</aside>
              <div class="matrix-main">
                <div class="matrix-row">
                  <div class="matrix-label required">团队类型</div>
                  <div class="matrix-field">
                    <Select v-model:value="formState.teamType" :options="teamTypeOptions" />
                  </div>
                  <div class="matrix-label">业务类型</div>
                  <div class="matrix-field">
                    <Select
                      v-model:value="formState.businessType"
                      allow-clear
                      :options="businessTypeOptions"
                      placeholder="请选择业务类型"
                    />
                  </div>
                </div>

                <div class="matrix-row single">
                  <div class="matrix-label required">线路名称</div>
                  <div class="matrix-field">
                    <Input v-model:value="formState.teamName" allow-clear placeholder="例如：杭州西湖二日游" />
                  </div>
                </div>

                <div class="matrix-row">
                  <div class="matrix-label">国内/国际</div>
                  <div class="matrix-field">
                    <Select v-model:value="formState.domesticInternational" :options="domesticOptions" />
                  </div>
                  <div class="matrix-label">接团城市</div>
                  <div class="matrix-field">
                    <Cascader
                      v-model:value="formRegionPath"
                      allow-clear
                      change-on-select
                      :options="regionOptions"
                      placeholder="可选择省 / 市 / 区县"
                      show-search
                    />
                  </div>
                </div>

                <div class="matrix-row">
                  <div class="matrix-label required">发团日期</div>
                  <div class="matrix-field">
                    <DatePicker v-model:value="formState.departureDate" class="w-full" value-format="YYYY-MM-DD" />
                  </div>
                  <div class="matrix-label">出团类型</div>
                  <div class="matrix-field">
                    <Select v-model:value="formState.tripType" :options="tripTypeOptions" />
                  </div>
                </div>

                <div class="matrix-row">
                  <div class="matrix-label">接待标准</div>
                  <div class="matrix-field">
                    <Select
                      v-model:value="formState.receptionStandard"
                      allow-clear
                      :options="receptionStandardOptions"
                      placeholder="请选择接待标准"
                    />
                  </div>
                  <div class="matrix-label">产品主题</div>
                  <div class="matrix-field">
                    <Select
                      v-model:value="formState.productTheme"
                      allow-clear
                      :options="productThemeOptions"
                      placeholder="请选择产品主题"
                    />
                  </div>
                </div>

                <div class="matrix-row quarters">
                  <div class="matrix-label required">旅游天数</div>
                  <div class="matrix-field">
                    <InputNumber
                      v-model:value="formState.travelDays"
                      class="w-full"
                      :min="1"
                      addon-after="天"
                      @change="handleTravelDaysChange"
                    />
                  </div>
                  <div class="matrix-label">截止收客</div>
                  <div class="matrix-field">
                    <InputNumber v-model:value="formState.closeDaysBefore" class="w-full" :min="0" addon-after="天" />
                  </div>
                  <div class="matrix-label">单人房差</div>
                  <div class="matrix-field">
                    <InputNumber v-model:value="formState.singleRoomDifference" class="w-full" :min="0" addon-before="¥" />
                  </div>
                  <div class="matrix-label">预控人数</div>
                  <div class="matrix-field">
                    <InputNumber v-model:value="formState.totalSeats" class="w-full" :min="0" addon-after="人" />
                  </div>
                </div>

                <div class="matrix-row single">
                  <div class="matrix-label">备注</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.remark" :rows="3" :maxlength="500" show-count />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeEditTab === 'itinerary'" class="form-panel">
            <div class="itinerary-matrix-layout">
              <aside class="itinerary-side-label">
                <div>线路行程</div>
              </aside>
              <div class="itinerary-main">
                <div class="section-toolbar">
                  <div>
                    <div class="section-title">行程内容</div>
                    <div class="muted">按产品行程表维护每天行程、住宿、关联酒店、用餐和路书公里。</div>
                  </div>
                </div>

                <div class="itinerary-table-wrap">
                  <table class="itinerary-table">
                    <thead>
                      <tr>
                        <th class="day-col">行程</th>
                        <th class="content-col">行程内容</th>
                        <th class="stay-col">住宿</th>
                        <th class="hotel-col">关联酒店</th>
                        <th class="meal-col">用餐</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template
                        v-for="(day, index) in formState.itineraryDays"
                        :key="index"
                      >
                        <tr>
                          <td class="day-cell">第 {{ index + 1 }} 天</td>
                          <td>
                            <Input
                              v-model:value="day.dayTitle"
                              allow-clear
                              class="mb-8"
                              placeholder="行程标题"
                            />
                            <Textarea
                              v-model:value="day.itineraryContent"
                              :auto-size="{ minRows: 4, maxRows: 8 }"
                              placeholder="当日行程内容"
                            />
                          </td>
                          <td>
                            <Textarea
                              v-model:value="day.accommodationNote"
                              :auto-size="{ minRows: 4, maxRows: 6 }"
                              placeholder="住宿说明"
                            />
                          </td>
                          <td>
                            <Select
                              v-model:value="day.relatedHotel"
                              allow-clear
                              class="w-full"
                              :filter-option="true"
                              :loading="relatedHotelLoading"
                              :options="relatedHotelOptions"
                              placeholder="选择关联酒店"
                              show-search
                            />
                            <Checkbox
                              class="mt-3"
                              :checked="Number(day.seasonalSurcharge || 0) > 0"
                              @change="(event) => toggleSeasonalSurcharge(day, event.target.checked)"
                            >
                              旺季附加费
                            </Checkbox>
                          </td>
                          <td>
                            <Space direction="vertical">
                              <Checkbox v-model:checked="day.breakfastIncluded">早餐</Checkbox>
                              <Checkbox v-model:checked="day.lunchIncluded">中餐</Checkbox>
                              <Checkbox v-model:checked="day.dinnerIncluded">晚餐</Checkbox>
                            </Space>
                          </td>
                        </tr>
                        <tr class="roadbook-detail-row">
                          <td class="roadbook-detail-label">路书信息</td>
                          <td colspan="4">
                            <div class="roadbook-detail-card">
                              <div class="roadbook-detail-main">
                                <div class="roadbook-summary-status">
                                  {{ day.roadbookPoints?.length ? '已维护路书' : '未维护路书' }}
                                </div>
                                <div class="roadbook-summary-route" :title="roadbookSummaryText(day)">
                                  {{ roadbookSummaryText(day) }}
                                </div>
                              </div>
                              <div class="roadbook-detail-stats">
                                <span>{{ roadbookPointCountText(day) }}</span>
                                <span>{{ roadbookDistanceText(day) }}</span>
                                <span>{{ roadbookDurationText(day) }}</span>
                              </div>
                              <div class="roadbook-detail-actions">
                                <Button
                                  class="roadbook-edit-button"
                                  size="small"
                                  type="primary"
                                  @click="openRoadbookDrawer(index)"
                                >
                                  编辑路书
                                </Button>
                              </div>
                            </div>
                          </td>
                        </tr>
                      </template>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeEditTab === 'description'" class="form-panel">
            <div class="matrix-layout description-layout">
              <aside class="side-label">产品说明</aside>
              <div class="matrix-main">
                <div class="description-row">
                  <div class="matrix-label">收客须知</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.bookingNotice" :auto-size="{ minRows: 5, maxRows: 8 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">产品说明</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.productDescription" :auto-size="{ minRows: 5, maxRows: 8 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">费用包含</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.feeIncluded" :auto-size="{ minRows: 5, maxRows: 8 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">费用不含</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.feeExcluded" :auto-size="{ minRows: 4, maxRows: 8 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">儿童安排</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.childPolicy" :auto-size="{ minRows: 3, maxRows: 6 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">购物安排</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.shoppingArrangement" :auto-size="{ minRows: 3, maxRows: 6 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">自费项目</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.optionalItems" :auto-size="{ minRows: 3, maxRows: 6 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">赠送项目</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.giftItems" :auto-size="{ minRows: 3, maxRows: 6 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">注意事项</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.attentionItems" :auto-size="{ minRows: 3, maxRows: 6 }" />
                  </div>
                </div>
                <div class="description-row">
                  <div class="matrix-label">温馨提醒</div>
                  <div class="matrix-field">
                    <Textarea v-model:value="formState.warmReminder" :auto-size="{ minRows: 3, maxRows: 6 }" />
                  </div>
                </div>
              </div>
            </div>
          </div>

        </Form>
      </Card>
    </Spin>
    <Drawer
      v-model:open="roadbookDrawerOpen"
      class="roadbook-workspace-drawer"
      destroy-on-close
      placement="right"
      root-class-name="roadbook-workspace-drawer-root"
      title="编辑路书地图"
      width="calc(100vw - 32px)"
      @close="closeRoadbookDrawer"
    >
      <div v-if="activeRoadbookDay" class="roadbook-workspace">
        <div class="roadbook-workspace-toolbar">
          <div class="roadbook-toolbar-title">
            <div class="section-title">第 {{ activeRoadbookDay.dayNo }} 天路书摘要</div>
            <div class="roadbook-toolbar-meta">
              <span>{{ roadbookSummaryText(activeRoadbookDay) }}</span>
              <span>{{ formatDistance(activeRoadbookDay.roadbookTotalDistanceMeters) }}</span>
              <span>约 {{ formatDuration(activeRoadbookDay.roadbookTotalDurationSeconds) }}</span>
            </div>
          </div>
          <div class="roadbook-toolbar-actions">
            <Select
              v-model:value="activeRoadbookDayIndex"
              class="roadbook-day-switch"
              :options="roadbookDaySwitchOptions"
              @change="(value) => switchRoadbookDay(Number(value))"
            />
            <Tag color="blue">{{ activeRoadbookDay.roadbookPoints?.length || 0 }} 个地点</Tag>
            <Button type="primary" :loading="roadbookCalculating" @click="calculateActiveRoadbookRoute">
              计算路线
            </Button>
          </div>
        </div>

        <div class="roadbook-workspace-main">
          <Spin :spinning="roadbookMapLoading" wrapper-class-name="roadbook-map-spin">
            <div class="roadbook-map-shell">
              <div ref="roadbookMapContainerRef" class="roadbook-map-container"></div>
              <div class="roadbook-map-search">
                <div class="roadbook-search-title">搜索地点或直接点地图</div>
                <Select
                  v-model:value="roadbookKeyword"
                  allow-clear
                  class="roadbook-search-select"
                  :filter-option="false"
                  :loading="roadbookTipLoading"
                  :options="roadbookTipOptions"
                  placeholder="输入地点名称，例如 苏州站、拙政园、平江路"
                  show-search
                  @search="handleRoadbookSearch"
                  @select="handleRoadbookSelect"
                />
              </div>
              <div v-if="roadbookMapError" class="roadbook-map-error">
                {{ roadbookMapError }}
              </div>
              <div v-else-if="!roadbookMapReady" class="roadbook-map-empty">
                地图加载中，加载完成后可以搜索地址或直接点击地图选点。
              </div>
            </div>
          </Spin>

          <aside class="roadbook-side-panel">
            <div class="roadbook-side-header">
              <div>
                <div class="section-title">路线点位</div>
                <div class="muted">按真实游览顺序维护；到下一站距离和车程来自高德驾车路线，不是直线距离。</div>
              </div>
              <Button size="small" :loading="roadbookCalculating" @click="calculateActiveRoadbookRoute">
                重算
              </Button>
            </div>

            <div v-if="!(activeRoadbookDay.roadbookPoints || []).length" class="roadbook-empty">
              先在左侧地图搜索或点击地点，系统会按顺序生成当天出发点、途经点和结束点。
            </div>

            <div class="roadbook-point-list">
              <div
                v-for="(point, index) in activeRoadbookDay.roadbookPoints || []"
                :key="`${point.pointOrder}-${point.placeName}`"
                class="roadbook-point-card"
              >
                <div class="roadbook-point-card-head">
                  <div class="roadbook-point-order">{{ index + 1 }}</div>
                  <Input v-model:value="point.placeName" class="roadbook-point-name" @change="updateRoadbookSummary(activeRoadbookDay)" />
                  <Select v-model:value="point.pointType" class="roadbook-point-type" :options="roadbookPointTypeOptions" />
                </div>
                <div class="roadbook-point-address">{{ point.address || '未记录地址' }}</div>
                <div class="roadbook-point-grid">
                  <label class="roadbook-metric-field">
                    <span>停留(分钟)</span>
                    <InputNumber v-model:value="point.stayMinutes" class="w-full" :min="0" />
                  </label>
                  <label class="roadbook-metric-field">
                    <span>到下一站(公里)</span>
                    <InputNumber
                      class="w-full"
                      :min="0"
                      :precision="1"
                      :value="Number(((point.distanceToNextMeters || 0) / 1000).toFixed(1))"
                      @change="(value) => { point.distanceToNextMeters = Math.round(Number(value || 0) * 1000); }"
                    />
                  </label>
                  <label class="roadbook-metric-field">
                    <span>车程(分钟)</span>
                    <InputNumber
                      class="w-full"
                      :min="0"
                      :value="Math.round((point.durationToNextSeconds || 0) / 60)"
                      @change="(value) => { point.durationToNextSeconds = Math.round(Number(value || 0) * 60); }"
                    />
                  </label>
                </div>
                <Input v-model:value="point.remark" allow-clear placeholder="备注，例如接站、游览、入住" />
                <div class="roadbook-point-actions">
                  <Button size="small" :disabled="index === 0" @click="moveRoadbookPoint(index, -1)">上移</Button>
                  <Button size="small" :disabled="index === (activeRoadbookDay.roadbookPoints?.length || 0) - 1" @click="moveRoadbookPoint(index, 1)">下移</Button>
                  <Button danger size="small" @click="removeRoadbookPoint(index)">删除</Button>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </Drawer>
  </Page>
</template>

<style scoped>
.team-create-card {
  border-radius: 8px;
}

.form-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.form-title {
  color: #172033;
  font-size: 18px;
  font-weight: 700;
}

.muted {
  color: #64748b;
  font-size: 13px;
}

.form-panel {
  margin-top: 10px;
}

.matrix-layout {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #e5eaf2;
  border-radius: 6px;
}

.side-label {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  padding: 12px;
  color: #475569;
  font-weight: 700;
  background: #f8fafc;
  border-right: 1px solid #e5eaf2;
  writing-mode: vertical-rl;
  letter-spacing: 0;
}

.matrix-main {
  min-width: 0;
}

.matrix-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) 112px minmax(0, 1fr);
  border-bottom: 1px solid #edf1f7;
}

.matrix-row.single {
  grid-template-columns: 112px minmax(0, 1fr);
}

.matrix-row.quarters {
  grid-template-columns: 112px minmax(120px, 1fr) 112px minmax(120px, 1fr) 112px minmax(120px, 1fr) 112px minmax(120px, 1fr);
}

.matrix-row:last-child,
.description-row:last-child {
  border-bottom: 0;
}

.matrix-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 54px;
  padding: 10px 12px;
  color: #526070;
  font-weight: 600;
  background: #fbfcff;
  border-right: 1px solid #edf1f7;
}

.matrix-label.required::before {
  margin-right: 4px;
  color: #dc2626;
  content: '*';
}

.matrix-field {
  min-width: 0;
  padding: 10px 12px;
  border-right: 1px solid #edf1f7;
}

.matrix-row > .matrix-field:last-child {
  border-right: 0;
}

.description-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  border-bottom: 1px solid #edf1f7;
}

.description-layout .side-label {
  min-height: 680px;
}

.mb-8 {
  margin-bottom: 8px;
}

.mt-3 {
  margin-top: 12px;
}

.w-full {
  width: 100%;
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.itinerary-matrix-layout {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  border: 1px solid #e5e7eb;
}

.itinerary-side-label {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  padding: 18px;
  color: #334155;
  font-size: 15px;
  font-weight: 700;
  background: #fff;
  border-right: 1px solid #e5e7eb;
}

.itinerary-main {
  min-width: 0;
  padding: 14px;
  background: #fff;
}

.itinerary-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.itinerary-table {
  width: 100%;
  min-width: 1000px;
  border-collapse: collapse;
  table-layout: fixed;
}

.itinerary-table th,
.itinerary-table td {
  padding: 12px;
  vertical-align: middle;
  border: 1px solid #e5e7eb;
}

.itinerary-table th {
  height: 44px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  text-align: center;
  background: #f8fafc;
}

.itinerary-table .day-col {
  width: 86px;
}

.itinerary-table .content-col {
  width: 420px;
}

.itinerary-table .stay-col {
  width: 200px;
}

.itinerary-table .hotel-col {
  width: 210px;
}

.itinerary-table .meal-col {
  width: 84px;
}

.day-cell {
  color: #334155;
  font-weight: 700;
  text-align: center;
}

.roadbook-detail-row td {
  padding: 10px 12px 14px;
  background: #f8fafc;
}

.roadbook-detail-label {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.roadbook-detail-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px 112px;
  gap: 12px;
  align-items: center;
  min-height: 72px;
  padding: 12px;
  background: linear-gradient(180deg, #fff, #f8fbff);
  border: 1px solid #dbeafe;
  border-radius: 8px;
  box-shadow: 0 8px 20px rgb(37 99 235 / 8%);
}

.roadbook-detail-main {
  min-width: 0;
}

.roadbook-summary-status {
  width: fit-content;
  padding: 2px 8px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  background: #eff6ff;
  border-radius: 999px;
}

.roadbook-summary-route {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 8px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.roadbook-detail-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.roadbook-detail-stats span {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 4px 6px;
  overflow: hidden;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f1f5f9;
  border-radius: 6px;
}

.roadbook-detail-actions {
  display: flex;
  justify-content: flex-end;
}

.roadbook-edit-button {
  min-width: 96px;
}

:global(.roadbook-workspace-drawer-root .ant-drawer-body) {
  padding: 0;
  overflow: hidden;
  background: #eef4f8;
}

:global(.roadbook-workspace-drawer-root .ant-drawer-header) {
  padding: 14px 20px;
  border-bottom: 1px solid #dbeafe;
}

.roadbook-workspace {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 58px);
  min-height: 640px;
  background:
    linear-gradient(180deg, rgb(239 246 255 / 88%), rgb(248 250 252 / 100%)),
    #f8fafc;
}

.roadbook-workspace-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.roadbook-toolbar-title {
  min-width: 0;
}

.roadbook-toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 5px;
  color: #475569;
  font-size: 13px;
}

.roadbook-toolbar-meta span {
  padding-right: 8px;
  border-right: 1px solid #cbd5e1;
}

.roadbook-toolbar-meta span:last-child {
  border-right: 0;
}

.roadbook-toolbar-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
  align-items: center;
}

.roadbook-day-switch {
  min-width: 180px;
}

.roadbook-workspace-main {
  position: relative;
  display: block;
  min-height: 0;
  padding: 12px;
  flex: 1;
}

.roadbook-map-spin,
:deep(.roadbook-map-spin .ant-spin-container) {
  min-width: 0;
  min-height: 0;
  height: 100%;
}

.roadbook-map-shell {
  position: relative;
  height: 100%;
  min-height: 560px;
  overflow: hidden;
  background: #e0f2fe;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  box-shadow: 0 16px 38px rgb(15 23 42 / 10%);
}

.roadbook-map-container {
  width: 100%;
  height: 100%;
}

.roadbook-map-search {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 5;
  width: min(460px, calc(100% - 32px));
  padding: 12px;
  background: rgb(255 255 255 / 96%);
  border: 1px solid #dbeafe;
  border-radius: 10px;
  box-shadow: 0 14px 32px rgb(15 23 42 / 16%);
}

.roadbook-search-title {
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.roadbook-search-select {
  width: 100%;
}

.roadbook-map-empty,
.roadbook-map-error {
  position: absolute;
  inset: auto 24px 24px 24px;
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  color: #475569;
  font-size: 14px;
  text-align: center;
  background: rgb(255 255 255 / 92%);
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  pointer-events: none;
}

.roadbook-map-error {
  color: #b42318;
  border-color: #fda29b;
}

.roadbook-side-panel {
  position: absolute;
  top: 28px;
  right: 28px;
  bottom: 28px;
  z-index: 6;
  display: flex;
  flex-direction: column;
  width: min(420px, calc(100% - 56px));
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: rgb(255 255 255 / 96%);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 18px 46px rgb(15 23 42 / 18%);
  backdrop-filter: blur(8px);
}

.roadbook-side-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
}

.roadbook-point-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
}

.roadbook-point-card {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.roadbook-point-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 10px 24px rgb(37 99 235 / 10%);
}

.roadbook-point-card-head {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 106px;
  gap: 8px;
  align-items: center;
}

.roadbook-point-order {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: #1677ff;
  font-weight: 700;
  background: #eff6ff;
  border-radius: 999px;
}

.roadbook-point-address {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.roadbook-point-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.roadbook-metric-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.roadbook-metric-field span {
  overflow: hidden;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.roadbook-point-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.roadbook-empty {
  margin: 12px;
  padding: 24px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

@media (max-width: 1180px) {
  .matrix-row,
  .matrix-row.quarters {
    grid-template-columns: 108px minmax(0, 1fr);
  }

  .matrix-field {
    border-right: 0;
  }
}

@media (max-width: 900px) {
  .form-header {
    flex-direction: column;
  }

  .itinerary-matrix-layout {
    grid-template-columns: 1fr;
  }

  .itinerary-side-label {
    min-height: auto;
    padding: 14px;
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }

  .roadbook-workspace {
    height: auto;
    min-height: calc(100vh - 58px);
  }

  .roadbook-workspace-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .roadbook-workspace-main {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .roadbook-map-shell {
    height: 62vh;
    min-height: 420px;
  }

  .roadbook-side-panel {
    position: static;
    width: auto;
    max-height: none;
  }
}
</style>
