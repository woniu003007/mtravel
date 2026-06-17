<script lang="ts" setup>
import type { SalesProductApi } from '#/api/sales/product';
import type { EnterpriseProductDictionaryApi } from '#/api/enterprise/product-dictionary';
import type { RegionPath } from '#/utils/region';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Cascader,
  Checkbox,
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

import {
  type EnterpriseProductDictionaryApi as ProductDictionaryNamespace,
  getProductDictionaryAll,
} from '#/api/enterprise/product-dictionary';
import { getControlledRoomResourceAll } from '#/api/dispatch/room-status';
import { getHotelResourcePage } from '#/api/purchase/hotel';
import {
  calculateRoadbookRoute,
  createSalesProduct,
  getAmapJsConfig,
  getSalesProductDetail,
  searchAmapTips,
  updateSalesProduct,
} from '#/api/sales/product';
import BusinessPillTabs from '#/components/business/BusinessPillTabs.vue';
import {
  buildRegionOptions,
  buildRegionPath,
} from '#/utils/region';

import {
  buildSalesProductPayload,
  createDefaultItineraryDay,
  createDefaultProductForm,
  syncItineraryDaysWithTravelDays,
  type ProductFormState,
} from './product-form-utils';

type DictItem = EnterpriseProductDictionaryApi.Item;
type SelectOption = { label: string; value: string };
type RoadbookPoint = SalesProductApi.RoadbookPoint;

const route = useRoute();
const router = useRouter();
const regionOptions = buildRegionOptions();
const ROADBOOK_SEARCH_DEBOUNCE_MS = 700;

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
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

const editTabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'itinerary', label: '行程内容' },
  { key: 'description', label: '产品说明' },
  { key: 'arrangement', label: '团队安排' },
  { key: 'schedule', label: '团期管理' },
];

function normalizeEditTab(key: string) {
  return editTabs.some((item) => item.key === key) ? key : 'basic';
}

const loading = ref(false);
const dictionaryLoading = ref(false);
const relatedHotelLoading = ref(false);
const roadbookCalculating = ref(false);
const saving = ref(false);
const formRegionPath = ref<RegionPath>([]);
const activeEditTab = ref(normalizeEditTab(String(route.query.tab || 'basic')));
const formState = reactive<ProductFormState>(createDefaultProductForm());
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

const productId = computed(() => {
  const value = route.params.id;
  const id = Array.isArray(value) ? value[0] : value;
  return id ? Number(id) : undefined;
});

const isEdit = computed(() => Boolean(productId.value));
const pageTitle = computed(() => (isEdit.value ? '修改产品' : '添加产品'));
const pageDescription = computed(() => (
  '按老系统产品维护逻辑拆成独立页面，行程内容维护线路行程，产品说明页维护收客须知和费用说明。'
));
const businessTypeOptions = computed(() => dictionaryOptions(businessTypes.value));
const receptionStandardOptions = computed(() => dictionaryOptions(receptionStandards.value));
const productThemeOptions = computed(() => dictionaryOptions(productThemes.value));

function dictionaryOptions(items: ProductDictionaryNamespace.Item[]) {
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

/**
 * 加载行程关联酒店候选项。
 *
 * 产品行程阶段只保存酒店名称文本，但选择项要来自现有自营房源和采购酒店资源，避免用户手写后与后续排房资源脱节。
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

const activeRoadbookDay = computed(() => (
  activeRoadbookDayIndex.value === undefined ? undefined : formState.itineraryDays?.[activeRoadbookDayIndex.value]
));

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

/**
 * 销毁路书地图实例。
 *
 * 抽屉使用 destroy-on-close，关闭后地图 DOM 会被移除；如果继续复用旧 AMap 实例，
 * 下一次打开时实例仍挂在旧容器上，地图瓦片会变成空白背景。
 */
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

async function loadDictionaries() {
  dictionaryLoading.value = true;
  try {
    const [business, standard, theme] = await Promise.all([
      getProductDictionaryAll('business_type'),
      getProductDictionaryAll('reception_standard'),
      getProductDictionaryAll('product_theme'),
    ]);
    businessTypes.value = business;
    receptionStandards.value = standard;
    productThemes.value = theme;
  } finally {
    dictionaryLoading.value = false;
  }
}

function fillForm(detail: SalesProductApi.Item) {
  formRegionPath.value = buildRegionPath(detail.province, detail.city, detail.district);
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
    itineraryDays: detail.itineraryDays?.length ? detail.itineraryDays : [createDefaultItineraryDay(1)],
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
  formState.itineraryDays = syncItineraryDaysWithTravelDays(
    formState.itineraryDays,
    formState.travelDays,
  );
}

async function loadDetail() {
  if (!productId.value) return;
  loading.value = true;
  try {
    const detail = await getSalesProductDetail(productId.value);
    fillForm(detail);
  } finally {
    loading.value = false;
  }
}

function importItinerary() {
  message.info('导入行程后续接入');
}

function syncItineraryDaysForTravelDays() {
  const nextTravelDays = Math.max(1, Math.floor(Number(formState.travelDays || 1)));
  formState.travelDays = nextTravelDays;
  formState.itineraryDays = syncItineraryDaysWithTravelDays(formState.itineraryDays, nextTravelDays);
}

function handleTravelDaysChange() {
  syncItineraryDaysForTravelDays();
}

function toggleSeasonalSurcharge(day: SalesProductApi.ItineraryDay, checked: boolean) {
  day.seasonalSurcharge = checked ? Math.max(Number(day.seasonalSurcharge || 0), 1) : 0;
}

function handleEditTabChange(key: string) {
  if (key === 'arrangement') {
    if (!productId.value) {
      message.info('请先保存产品后再维护团队安排');
      return;
    }
    router.push(`/sales/product/team-arrangement/${productId.value}`);
    return;
  }
  activeEditTab.value = normalizeEditTab(key);
  if (key === 'schedule') {
    message.info('团期管理将在团期模块接入后开放');
  }
}

function goBack() {
  router.push('/sales/product');
}

async function saveProduct() {
  if (!formState.productName?.trim()) {
    activeEditTab.value = 'basic';
    message.warning('请填写产品名称');
    return;
  }
  if (!formState.travelDays || formState.travelDays < 1) {
    activeEditTab.value = 'basic';
    message.warning('请填写旅游天数');
    return;
  }
  syncItineraryDaysForTravelDays();
  if (activeEditTab.value === 'basic') {
    activeEditTab.value = 'itinerary';
    message.info('请继续维护行程内容');
    return;
  }
  saving.value = true;
  try {
    const payload = buildSalesProductPayload(formState, formRegionPath.value);
    if (productId.value) {
      await updateSalesProduct(productId.value, payload);
      message.success('产品已更新');
    } else {
      await createSalesProduct(payload);
      message.success('产品已新增');
    }
    goBack();
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  // 团队安排保留为老系统 tab 入口，但实际维护在独立页面，避免产品表单里内嵌复杂安排表。
  if (activeEditTab.value === 'arrangement') {
    if (productId.value) {
      router.replace(`/sales/product/team-arrangement/${productId.value}`);
      return;
    }
    activeEditTab.value = 'basic';
  }
  await Promise.all([loadDictionaries(), loadDetail(), loadRelatedHotelOptions()]);
});
</script>

<template>
  <Page :title="pageTitle" :description="pageDescription">
    <Spin :spinning="loading">
      <Card class="product-form-card">
        <div class="form-header">
          <div>
            <div class="form-title">添加/修改产品</div>
            <div class="form-subtitle">
              产品是后续团期、团队和订单的模板，正式排团履约仍在销售和计调模块处理。
            </div>
          </div>
          <Space>
            <Button @click="goBack">返回列表</Button>
            <Button type="primary" :loading="saving" @click="saveProduct">保存产品</Button>
          </Space>
        </div>

        <BusinessPillTabs
          :active-key="activeEditTab"
          aria-label="产品编辑页签"
          :tabs="editTabs"
          @change="handleEditTabChange"
        />

        <Form :model="formState" layout="vertical">
          <div v-show="activeEditTab === 'basic'" class="form-panel">
            <div class="product-basic-matrix-layout">
              <aside class="product-basic-side-label">基本信息</aside>
              <div class="product-basic-main">
                <div class="product-basic-row">
                  <div class="product-basic-label">业务类型</div>
                  <div class="product-basic-field">
                    <Select
                      v-model:value="formState.businessType"
                      allow-clear
                      :loading="dictionaryLoading"
                      :options="businessTypeOptions"
                      placeholder="请选择业务类型"
                    />
                  </div>
                  <div class="product-basic-label">国内/国际</div>
                  <div class="product-basic-field">
                    <Select v-model:value="formState.domesticInternational" :options="domesticOptions" />
                  </div>
                </div>

                <div class="product-basic-row single">
                  <div class="product-basic-label required">线路名称</div>
                  <div class="product-basic-field">
                    <Input v-model:value="formState.productName" allow-clear placeholder="例如：杭州西湖二日游" />
                  </div>
                </div>

                <div class="product-basic-row">
                  <div class="product-basic-label">接团城市</div>
                  <div class="product-basic-field">
                    <Cascader
                      v-model:value="formRegionPath"
                      allow-clear
                      change-on-select
                      :options="regionOptions"
                      placeholder="可选择省 / 市 / 区县"
                      show-search
                    />
                  </div>
                  <div class="product-basic-label">出团类型</div>
                  <div class="product-basic-field">
                    <Select v-model:value="formState.tripType" :options="tripTypeOptions" />
                  </div>
                </div>

                <div class="product-basic-row">
                  <div class="product-basic-label">接待标准</div>
                  <div class="product-basic-field">
                    <Select
                      v-model:value="formState.receptionStandard"
                      allow-clear
                      :loading="dictionaryLoading"
                      :options="receptionStandardOptions"
                      placeholder="请选择接待标准"
                    />
                  </div>
                  <div class="product-basic-label">产品主题</div>
                  <div class="product-basic-field">
                    <Select
                      v-model:value="formState.productTheme"
                      allow-clear
                      :loading="dictionaryLoading"
                      :options="productThemeOptions"
                      placeholder="请选择产品主题"
                    />
                  </div>
                </div>

                <div class="product-basic-row quarters">
                  <div class="product-basic-label required">旅游天数</div>
                  <div class="product-basic-field">
                    <InputNumber
                      v-model:value="formState.travelDays"
                      class="w-full"
                      :min="1"
                      @change="handleTravelDaysChange"
                    />
                  </div>
                  <div class="product-basic-label">截止收客</div>
                  <div class="product-basic-field">
                    <InputNumber
                      v-model:value="formState.closeDaysBefore"
                      addon-after="天"
                      class="w-full"
                      :min="0"
                    />
                  </div>
                </div>

                <div class="product-basic-row quarters">
                  <div class="product-basic-label">单人房差</div>
                  <div class="product-basic-field">
                    <InputNumber
                      v-model:value="formState.singleRoomDifference"
                      class="w-full"
                      :min="0"
                      :precision="2"
                      prefix="¥"
                    />
                  </div>
                  <div class="product-basic-label">预控人数</div>
                  <div class="product-basic-field">
                    <InputNumber v-model:value="formState.plannedCapacity" class="w-full" :min="0" />
                  </div>
                </div>

                <div class="product-basic-row">
                  <div class="product-basic-label">状态</div>
                  <div class="product-basic-field">
                    <Select v-model:value="formState.status" :options="statusOptions" />
                  </div>
                  <div class="product-basic-label">备注</div>
                  <div class="product-basic-field">
                    <Textarea v-model:value="formState.remark" :auto-size="{ minRows: 2, maxRows: 4 }" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeEditTab === 'itinerary'" class="form-panel">
            <div class="itinerary-matrix-layout">
              <aside class="itinerary-side-label">
                <div>线路行程</div>
                <Button type="primary" @click="importItinerary">导入行程</Button>
              </aside>
              <div class="itinerary-main">
                <div class="section-toolbar">
                  <div>
                    <div class="section-title">行程内容</div>
                    <div class="muted">按老系统线路行程表维护每天行程、住宿、关联酒店、用餐和路书公里。</div>
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
                              class="mb-2"
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
            <div class="product-description-matrix-layout">
              <aside class="product-description-side-label">
                <div>产品说明</div>
              </aside>
              <div class="product-description-main">
                <div class="product-description-row">
                  <div class="product-description-label">收客须知</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.bookingNotice"
                      :auto-size="{ minRows: 5, maxRows: 8 }"
                      placeholder="请填写收客须知"
                    />
                    <div class="product-description-hint">
                      如果该行程对收客有特殊需要，比如学生、儿童、老人、老师等价格区分的人群，请在此处注明，不再允许写在其它项目中。
                    </div>
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">产品说明</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.productDescription"
                      :auto-size="{ minRows: 5, maxRows: 8 }"
                      placeholder="请填写产品整体说明"
                    />
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">费用包含</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.feeIncluded"
                      :auto-size="{ minRows: 5, maxRows: 8 }"
                      placeholder="请填写报价包含项目"
                    />
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">费用不含</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.feeExcluded"
                      :auto-size="{ minRows: 4, maxRows: 8 }"
                      placeholder="请填写报价不包含项目"
                    />
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">儿童安排</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.childPolicy"
                      :auto-size="{ minRows: 3, maxRows: 6 }"
                      placeholder="请填写儿童价格、占床和服务安排"
                    />
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">购物安排</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.shoppingArrangement"
                      :auto-size="{ minRows: 3, maxRows: 6 }"
                      placeholder="请填写购物点或无购物说明"
                    />
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">自费项目</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.optionalItems"
                      :auto-size="{ minRows: 3, maxRows: 6 }"
                      placeholder="请填写自费项目、价格和说明"
                    />
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">赠送项目</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.giftItems"
                      :auto-size="{ minRows: 3, maxRows: 6 }"
                      placeholder="请填写赠送项目说明"
                    />
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">注意事项</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.attentionItems"
                      :auto-size="{ minRows: 3, maxRows: 6 }"
                      placeholder="请填写报名、出行、退改等注意事项"
                    />
                  </div>
                </div>

                <div class="product-description-row">
                  <div class="product-description-label">温馨提醒</div>
                  <div class="product-description-body">
                    <Textarea
                      v-model:value="formState.warmReminder"
                      :auto-size="{ minRows: 3, maxRows: 6 }"
                      placeholder="请填写温馨提醒"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeEditTab === 'schedule'" class="placeholder-panel">
            <Card>
              <div class="section-title">团期管理后续接入</div>
              <div class="muted mt-2">
                本页先保留老系统页签位置。团期正式开发后，会从产品模板生成可售团期，并进入团队、订单和游客名单链路。
              </div>
            </Card>
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
.product-form-card {
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

.form-panel {
  min-height: 560px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.product-basic-matrix-layout {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.product-basic-side-label {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 430px;
  padding: 26px 18px;
  font-size: 15px;
  font-weight: 700;
  color: #334155;
  background: #fff;
  border-right: 1px solid #e5e7eb;
}

.product-basic-main {
  min-width: 0;
  background: #fff;
}

.product-basic-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) 120px minmax(0, 1fr);
  min-height: 58px;
  border-bottom: 1px solid #e5e7eb;
}

.product-basic-row:last-child {
  border-bottom: 0;
}

.product-basic-row.single {
  grid-template-columns: 120px minmax(0, 1fr);
}

.product-basic-label {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 10px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
  text-align: center;
  background: #f8fafc;
  border-right: 1px solid #e5e7eb;
}

.product-basic-label.required::after {
  margin-left: 4px;
  color: #f5222d;
  content: '*';
}

.product-basic-field {
  min-width: 0;
  padding: 11px 12px;
  border-right: 1px solid #e5e7eb;
}

.product-basic-field:last-child {
  border-right: 0;
}

.product-basic-field :deep(.ant-input),
.product-basic-field :deep(.ant-input-number),
.product-basic-field :deep(.ant-select-selector),
.product-basic-field :deep(.ant-cascader-picker),
.product-basic-field :deep(.ant-input-affix-wrapper) {
  width: 100%;
}

.product-basic-field :deep(.ant-input),
.product-basic-field :deep(.ant-input-number),
.product-basic-field :deep(.ant-select-selector),
.product-basic-field :deep(.ant-input-affix-wrapper) {
  border-color: #dbe4f0;
  border-radius: 4px;
}

.product-basic-field :deep(textarea.ant-input) {
  min-height: 34px;
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.itinerary-matrix-layout {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  border: 1px solid #e5e7eb;
}

.itinerary-side-label {
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  padding: 18px;
  font-size: 15px;
  font-weight: 700;
  color: #334155;
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
  font-size: 14px;
  font-weight: 700;
  color: #334155;
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
  font-weight: 700;
  color: #334155;
  text-align: center;
}

.roadbook-detail-row td {
  padding: 10px 12px 14px;
  background: #f8fafc;
}

.roadbook-detail-label {
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
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
  font-size: 12px;
  font-weight: 700;
  color: #1d4ed8;
  background: #eff6ff;
  border-radius: 999px;
}

.roadbook-summary-route {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
  color: #0f172a;
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
  font-size: 12px;
  font-weight: 600;
  color: #475569;
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
  font-size: 13px;
  color: #475569;
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
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
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
  font-size: 14px;
  color: #475569;
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
  font-weight: 700;
  color: #1677ff;
  background: #eff6ff;
  border-radius: 999px;
}

.roadbook-point-address {
  overflow: hidden;
  font-size: 12px;
  color: #64748b;
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
  font-size: 12px;
  font-weight: 600;
  color: #475569;
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

.product-description-matrix-layout {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  border: 1px solid #e5e7eb;
}

.product-description-side-label {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 620px;
  padding: 26px 18px;
  font-size: 15px;
  font-weight: 700;
  color: #334155;
  background: #fff;
  border-right: 1px solid #e5e7eb;
}

.product-description-main {
  min-width: 0;
  background: #fff;
}

.product-description-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  border-bottom: 1px solid #e5e7eb;
}

.product-description-row:last-child {
  border-bottom: 0;
}

.product-description-label {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 118px;
  padding: 14px 12px;
  font-size: 15px;
  font-weight: 700;
  color: #334155;
  text-align: center;
  background: #f8fafc;
  border-right: 1px solid #e5e7eb;
}

.product-description-body {
  min-width: 0;
  background: #fff;
}

.product-description-body textarea {
  border: 0;
  border-radius: 0;
}

.product-description-hint {
  padding: 0 12px 10px;
  font-size: 12px;
  color: #2563eb;
}

.placeholder-panel {
  max-width: 760px;
}

@media (max-width: 900px) {
  .form-header {
    flex-direction: column;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .product-basic-matrix-layout {
    grid-template-columns: 1fr;
  }

  .product-basic-side-label {
    min-height: auto;
    padding: 14px;
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }

  .product-basic-row,
  .product-basic-row.single {
    grid-template-columns: 110px minmax(0, 1fr);
  }

  .product-basic-row .product-basic-label:nth-of-type(2) {
    border-left: 0;
  }

  .product-basic-field {
    border-right: 0;
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
