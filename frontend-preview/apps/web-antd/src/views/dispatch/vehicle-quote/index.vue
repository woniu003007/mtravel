<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';
import type { VehicleQuoteApi } from '#/api/dispatch/vehicle-quote';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import {
  calculateVehicleQuote,
  createVehicleQuoteRule,
  deleteVehicleQuoteRule,
  getVehicleQuoteRuleAll,
  getVehicleQuoteRulePage,
  updateVehicleQuoteRule,
} from '#/api/dispatch/vehicle-quote';
import {
  calculateRoadbookRoute,
  getAmapJsConfig,
  searchAmapTips,
  type SalesProductApi,
} from '#/api/sales/product';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

type RuleRow = VehicleQuoteApi.Rule;
type RuleForm = VehicleQuoteApi.SaveParams;
type SelectOption = { label: string; value: string };
type MapQuotePoint = SalesProductApi.RoadbookPoint;

const MAP_QUOTE_SEARCH_DEBOUNCE_MS = 700;

const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const mapQuoteDrawerOpen = ref(false);
const mapQuoteLoading = ref(false);
const mapQuoteCalculating = ref(false);
const mapQuoteMapReady = ref(false);
const mapQuoteMapError = ref('');
const editingId = ref<number>();
const ruleRows = ref<RuleRow[]>([]);
const quoteRuleOptions = ref<SelectOption[]>([]);
const calcResult = ref<VehicleQuoteApi.CalculateResult>();
const mapQuoteResult = ref<VehicleQuoteApi.CalculateResult>();
const querySeatCount = ref<number>();
const calcVehicleType = ref<string>();
const formSeatCount = ref<number>();
const mapQuoteVehicleType = ref<string>();
const mapQuoteKeyword = ref('');
const mapQuoteTipOptions = ref<Array<SelectOption & { meta: SalesProductApi.AmapTip }>>([]);
const mapQuoteTipLoading = ref(false);
const mapQuoteMapContainerRef = ref<HTMLDivElement>();
let mapQuoteMapInstance: any;
let mapQuotePolyline: any;
let mapQuoteGeocoder: any;
let mapQuoteLoaderPromise: Promise<any> | undefined;
let mapQuoteSearchTimer: number | undefined;
const mapQuoteMarkers: any[] = [];
const mapQuotePoints = ref<MapQuotePoint[]>([]);

const query = reactive<VehicleQuoteApi.QueryParams>({
  page: 1,
  pageSize: 10,
});
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});
const form = reactive<RuleForm>({
  baseKilometers: 100,
  basePrice: 0,
  extraKilometerPrice: 0,
  floatRate: 1,
  minimumPrice: 0,
  status: 'active',
  vehicleType: '',
});
const calcForm = reactive<VehicleQuoteApi.CalculateParams>({
  distanceMeters: 0,
  vehicleType: '',
});
const mapQuoteRoute = reactive({
  distanceMeters: 0,
  durationSeconds: 0,
});

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const modalTitle = computed(() => (editingId.value ? '修改座位数报价规则' : '新增座位数报价规则'));

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

/** 页面只让用户录数字；接口仍沿用既有 `39座` 规则值，保证老数据和测算匹配不受影响。 */
function vehicleTypeFromSeatCount(value?: number | string) {
  const numericText = String(value ?? '').replace(/\D/g, '');
  return numericText ? `${Number(numericText)}座` : '';
}

/** 编辑旧数据时从 `39座` 等历史规则值里提取数字，回填到数字输入框。 */
function seatCountFromVehicleType(value?: string) {
  const matched = String(value || '').match(/\d+/);
  return matched?.[0] ? Number(matched[0]) : undefined;
}

function syncQueryVehicleType() {
  query.vehicleType = vehicleTypeFromSeatCount(querySeatCount.value) || undefined;
}

function formatDistance(meters?: number) {
  const value = Number(meters || 0);
  if (value <= 0) return '0 公里';
  return `${(value / 1000).toFixed(1)} 公里`;
}

function formatDuration(seconds?: number) {
  const totalSeconds = Number(seconds || 0);
  if (totalSeconds <= 0) return '0 分钟';
  const minutes = Math.round(totalSeconds / 60);
  if (minutes < 60) return `${minutes} 分钟`;
  return `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分钟`;
}

function mapQuoteSummaryText() {
  return mapQuotePoints.value.map((point) => point.placeName).filter(Boolean).join(' -> ') || '还未选择路线点位';
}

function normalizeMapQuoteOrders() {
  mapQuotePoints.value.forEach((point, index) => {
    point.pointOrder = index + 1;
    if (index === 0) {
      point.pointType = 'departure';
    } else if (index === mapQuotePoints.value.length - 1) {
      point.pointType = 'arrival';
    } else {
      point.pointType = 'waypoint';
    }
  });
}

function resetForm() {
  Object.assign(form, {
    baseKilometers: 100,
    basePrice: 0,
    extraKilometerPrice: 0,
    floatRate: 1,
    minimumPrice: 0,
    remark: undefined,
    status: 'active',
    vehicleType: '',
  });
  formSeatCount.value = undefined;
  editingId.value = undefined;
}

async function loadRules() {
  syncQueryVehicleType();
  loading.value = true;
  try {
    const result = await getVehicleQuoteRulePage(query);
    ruleRows.value = result.items;
    pagination.total = result.total;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
  } finally {
    loading.value = false;
  }
}

/** 报价使用区只能选择已配置的启用规则，避免用户填写一个数据库不存在的座位数。 */
async function loadQuoteRuleOptions() {
  const rules = await getVehicleQuoteRuleAll();
  quoteRuleOptions.value = rules
    .map((rule) => ({
      label: rule.vehicleType,
      value: rule.vehicleType,
    }))
    .sort((current, next) => (seatCountFromVehicleType(current.value) || 0) - (seatCountFromVehicleType(next.value) || 0));
}

function openCreateModal() {
  resetForm();
  modalOpen.value = true;
}

function openEditModal(row: RuleRow) {
  resetForm();
  editingId.value = row.id;
  Object.assign(form, {
    baseKilometers: row.baseKilometers,
    basePrice: row.basePrice,
    extraKilometerPrice: row.extraKilometerPrice,
    floatRate: row.floatRate,
    minimumPrice: row.minimumPrice,
    remark: row.remark,
    status: row.status,
    vehicleType: row.vehicleType,
  });
  formSeatCount.value = seatCountFromVehicleType(row.vehicleType);
  modalOpen.value = true;
}

async function saveRule() {
  const vehicleType = vehicleTypeFromSeatCount(formSeatCount.value);
  if (!vehicleType) {
    message.warning('请填写座位数');
    return;
  }
  const payload = { ...form, vehicleType };
  saving.value = true;
  try {
    if (editingId.value) {
      await updateVehicleQuoteRule(editingId.value, payload);
    } else {
      await createVehicleQuoteRule(payload);
    }
    modalOpen.value = false;
    message.success('报价规则已保存');
    await Promise.all([loadRules(), loadQuoteRuleOptions()]);
  } finally {
    saving.value = false;
  }
}

async function removeRule(row: RuleRow) {
  await deleteVehicleQuoteRule(row.id);
  message.success('报价规则已删除');
  await Promise.all([loadRules(), loadQuoteRuleOptions()]);
}

async function runCalculate() {
  const vehicleType = calcVehicleType.value;
  if (!vehicleType) {
    message.warning('请选择座位数规则');
    return;
  }
  calcResult.value = await calculateVehicleQuote({ ...calcForm, vehicleType });
}

async function loadMapQuoteScript() {
  if ((window as any).AMap) {
    return (window as any).AMap;
  }
  if (mapQuoteLoaderPromise) {
    return mapQuoteLoaderPromise;
  }
  mapQuoteLoaderPromise = getAmapJsConfig().then((config) => new Promise((resolve, reject) => {
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
  return mapQuoteLoaderPromise;
}

async function initMapQuoteMap() {
  if (!mapQuoteMapContainerRef.value) return;
  mapQuoteLoading.value = true;
  mapQuoteMapError.value = '';
  try {
    const AMap = await loadMapQuoteScript();
    if (!mapQuoteMapInstance) {
      mapQuoteMapInstance = new AMap.Map(mapQuoteMapContainerRef.value, {
        center: [120.14895, 30.24490],
        resizeEnable: true,
        viewMode: '2D',
        zoom: 11,
      });
      mapQuoteGeocoder = new AMap.Geocoder();
      mapQuoteMapInstance.on('click', handleMapQuoteMapClick);
    }
    mapQuoteMapReady.value = true;
    renderMapQuotePoints();
  } catch (error) {
    mapQuoteMapReady.value = false;
    mapQuoteMapError.value = '地图加载失败，请检查高德 Web端 JS API Key 和安全密钥配置。';
  } finally {
    mapQuoteLoading.value = false;
  }
}

function clearMapQuoteOverlays() {
  if (!mapQuoteMapInstance) return;
  mapQuoteMarkers.forEach((marker) => mapQuoteMapInstance.remove(marker));
  mapQuoteMarkers.splice(0);
  if (mapQuotePolyline) {
    mapQuoteMapInstance.remove(mapQuotePolyline);
    mapQuotePolyline = undefined;
  }
}

/** 全屏抽屉关闭时销毁地图实例，避免下一次打开复用旧 DOM 导致地图空白。 */
function destroyMapQuoteMap() {
  if (!mapQuoteMapInstance) return;
  clearMapQuoteOverlays();
  mapQuoteMapInstance.off?.('click', handleMapQuoteMapClick);
  mapQuoteMapInstance.destroy?.();
  mapQuoteMapInstance = undefined;
  mapQuoteGeocoder = undefined;
  mapQuoteMapReady.value = false;
  mapQuoteLoading.value = false;
}

function renderMapQuotePoints() {
  if (!mapQuoteMapInstance || !(window as any).AMap) return;
  const AMap = (window as any).AMap;
  const points = mapQuotePoints.value.filter((point) => point.longitude && point.latitude);
  clearMapQuoteOverlays();
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
    mapQuoteMarkers.push(marker);
    mapQuoteMapInstance.add(marker);
  });
  if (positions.length >= 2) {
    mapQuotePolyline = new AMap.Polyline({
      path: positions,
      strokeColor: '#1677ff',
      strokeOpacity: 0.9,
      strokeWeight: 6,
    });
    mapQuoteMapInstance.add(mapQuotePolyline);
  }
  mapQuoteMapInstance.setFitView(mapQuoteMarkers);
}

function resetMapQuoteRouteResult() {
  mapQuoteRoute.distanceMeters = 0;
  mapQuoteRoute.durationSeconds = 0;
  mapQuoteResult.value = undefined;
}

function addMapQuotePointFromLngLat(lng: number, lat: number, placeName: string, address?: string) {
  mapQuotePoints.value.push({
    address,
    latitude: String(lat),
    longitude: String(lng),
    placeName,
    pointOrder: mapQuotePoints.value.length + 1,
    pointType: mapQuotePoints.value.length === 0 ? 'departure' : 'arrival',
    stayMinutes: 0,
  });
  normalizeMapQuoteOrders();
  resetMapQuoteRouteResult();
  renderMapQuotePoints();
}

function handleMapQuoteMapClick(event: any) {
  const lng = event.lnglat.getLng();
  const lat = event.lnglat.getLat();
  if (!mapQuoteGeocoder) {
    addMapQuotePointFromLngLat(lng, lat, `地图选点 ${lng.toFixed(5)},${lat.toFixed(5)}`);
    return;
  }
  mapQuoteGeocoder.getAddress([lng, lat], (status: string, result: any) => {
    const address = status === 'complete' ? result?.regeocode?.formattedAddress : '';
    addMapQuotePointFromLngLat(lng, lat, address || `地图选点 ${lng.toFixed(5)},${lat.toFixed(5)}`, address);
  });
}

function clearMapQuoteSearchTimer() {
  if (mapQuoteSearchTimer !== undefined) {
    window.clearTimeout(mapQuoteSearchTimer);
    mapQuoteSearchTimer = undefined;
  }
}

async function handleMapQuoteSearch(value: string) {
  mapQuoteKeyword.value = value;
  clearMapQuoteSearchTimer();
  if (!value?.trim()) {
    mapQuoteTipOptions.value = [];
    mapQuoteTipLoading.value = false;
    return;
  }
  mapQuoteTipLoading.value = true;
  mapQuoteSearchTimer = window.setTimeout(() => {
    doMapQuoteSearch(value);
  }, MAP_QUOTE_SEARCH_DEBOUNCE_MS);
}

async function doMapQuoteSearch(value: string) {
  const keyword = value.trim();
  if (!keyword || keyword !== mapQuoteKeyword.value.trim()) {
    mapQuoteTipLoading.value = false;
    return;
  }
  mapQuoteTipLoading.value = true;
  try {
    const tips = await searchAmapTips({ keywords: keyword });
    if (keyword !== mapQuoteKeyword.value.trim()) return;
    mapQuoteTipOptions.value = tips.map((item) => ({
      label: `${item.name}${item.district ? ` / ${item.district}` : ''}${item.address ? ` / ${item.address}` : ''}`,
      meta: item,
      value: `${item.longitude},${item.latitude},${item.name}`,
    }));
  } catch (error) {
    mapQuoteTipOptions.value = [];
    message.warning('地点搜索太频繁或暂时不可用，请稍后再试');
  } finally {
    mapQuoteTipLoading.value = false;
    mapQuoteSearchTimer = undefined;
  }
}

function addMapQuotePoint(value: string) {
  const option = mapQuoteTipOptions.value.find((item) => item.value === value);
  const tip = option?.meta;
  if (!tip?.longitude || !tip.latitude) {
    message.warning('请选择带经纬度的地图地点');
    return;
  }
  addMapQuotePointFromLngLat(Number(tip.longitude), Number(tip.latitude), tip.name, tip.address);
  mapQuoteKeyword.value = '';
  mapQuoteTipOptions.value = [];
}

function handleMapQuoteSelect(value: unknown) {
  if (typeof value === 'string') {
    addMapQuotePoint(value);
  }
}

function removeMapQuotePoint(index: number) {
  mapQuotePoints.value.splice(index, 1);
  normalizeMapQuoteOrders();
  resetMapQuoteRouteResult();
  renderMapQuotePoints();
}

function moveMapQuotePoint(index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= mapQuotePoints.value.length) return;
  const current = mapQuotePoints.value[index];
  const next = mapQuotePoints.value[target];
  if (!current || !next) return;
  mapQuotePoints.value[index] = next;
  mapQuotePoints.value[target] = current;
  normalizeMapQuoteOrders();
  resetMapQuoteRouteResult();
  renderMapQuotePoints();
}

function clearMapQuotePoints() {
  mapQuotePoints.value = [];
  resetMapQuoteRouteResult();
  renderMapQuotePoints();
}

async function calculateMapQuotePrice() {
  const vehicleType = mapQuoteVehicleType.value;
  if (!vehicleType) {
    message.warning('请选择座位数规则');
    return;
  }
  if (!mapQuoteRoute.distanceMeters) {
    message.warning('请先计算地图路线');
    return;
  }
  mapQuoteResult.value = await calculateVehicleQuote({
    distanceMeters: mapQuoteRoute.distanceMeters,
    vehicleType,
  });
}

async function calculateMapQuoteRoute() {
  if (mapQuotePoints.value.length < 2) {
    message.warning('至少选择两个地点才能计算路线');
    return;
  }
  if (mapQuotePoints.value.some((point) => !point.longitude || !point.latitude)) {
    message.warning('路线点位缺少经纬度，无法计算路线');
    return;
  }
  mapQuoteCalculating.value = true;
  try {
    const result = await calculateRoadbookRoute({
      points: mapQuotePoints.value.map((point) => ({
        latitude: point.latitude || '',
        longitude: point.longitude || '',
      })),
    });
    mapQuotePoints.value.forEach((point, index) => {
      const segment = result.segments[index];
      point.distanceToNextMeters = segment?.distanceMeters || 0;
      point.durationToNextSeconds = segment?.durationSeconds || 0;
    });
    mapQuoteRoute.distanceMeters = result.totalDistanceMeters;
    mapQuoteRoute.durationSeconds = result.totalDurationSeconds;
    renderMapQuotePoints();
    await calculateMapQuotePrice();
    message.success('地图路线和参考报价已计算');
  } finally {
    mapQuoteCalculating.value = false;
  }
}

async function openMapQuoteDrawer() {
  mapQuoteVehicleType.value = calcVehicleType.value;
  mapQuoteDrawerOpen.value = true;
  await nextTick();
  await initMapQuoteMap();
}

function closeMapQuoteDrawer() {
  clearMapQuoteSearchTimer();
  destroyMapQuoteMap();
}

function applyMapQuoteToQuickQuote() {
  if (!mapQuoteRoute.distanceMeters) {
    message.warning('没有可回填的地图路线');
    return;
  }
  calcVehicleType.value = mapQuoteVehicleType.value;
  calcForm.distanceMeters = mapQuoteRoute.distanceMeters;
  calcResult.value = mapQuoteResult.value;
  mapQuoteDrawerOpen.value = false;
  closeMapQuoteDrawer();
  message.success('地图报价已回填到快速测算');
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = nextPagination.current || 1;
  query.pageSize = nextPagination.pageSize || 10;
  loadRules();
}

onMounted(async () => {
  await Promise.all([loadRules(), loadQuoteRuleOptions()]);
});
</script>

<template>
  <Page title="用车报价测算" description="按车辆座位数维护公里报价规则，计调可以按路书公里快速测算外部车队参考价。">
    <Card class="vehicle-quote-page">
      <BusinessSearchForm
        :model="query"
        @create="openCreateModal"
        @reset="querySeatCount = undefined; Object.assign(query, { keyword: undefined, status: undefined, vehicleType: undefined, page: 1 }); loadRules()"
        @search="query.page = 1; loadRules()"
      >
        <Form.Item label="关键词">
          <Input v-model:value="query.keyword" allow-clear placeholder="座位数 / 状态" />
        </Form.Item>
        <Form.Item label="座位数">
          <InputNumber v-model:value="querySeatCount" allow-clear addon-after="座" :min="1" :precision="0" placeholder="请输入座位数" />
        </Form.Item>
        <Form.Item label="状态">
          <Select v-model:value="query.status" allow-clear :options="statusOptions" />
        </Form.Item>
      </BusinessSearchForm>

      <div class="quote-calc-panel">
        <div class="quote-calc-title">快速测算</div>
        <div class="quote-calc-grid">
          <Select
            v-model:value="calcVehicleType"
            allow-clear
            show-search
            :options="quoteRuleOptions"
            placeholder="请选择座位数规则"
          />
          <InputNumber v-model:value="calcForm.distanceMeters" addon-after="米" :min="0" :precision="0" />
          <Button type="primary" @click="runCalculate">测算价格</Button>
          <Button @click="openMapQuoteDrawer">打开地图报价</Button>
          <div class="quote-calc-result">
            {{ calcResult ? `${formatMoney(calcResult.calculatedAmount)} / ${calcResult.distanceKilometers} 公里` : '选择座位数规则和公里后测算' }}
          </div>
        </div>
      </div>

      <Table
        class="vehicle-quote-table"
        row-key="id"
        :data-source="ruleRows"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <Table.Column title="座位数" data-index="vehicleType" key="vehicleType" width="120" />
        <Table.Column title="基础价" data-index="basePrice" key="basePrice" width="110">
          <template #default="{ record }">{{ formatMoney(record.basePrice) }}</template>
        </Table.Column>
        <Table.Column title="基础公里" data-index="baseKilometers" key="baseKilometers" width="100" />
        <Table.Column title="超公里单价" data-index="extraKilometerPrice" key="extraKilometerPrice" width="120">
          <template #default="{ record }">{{ formatMoney(record.extraKilometerPrice) }}</template>
        </Table.Column>
        <Table.Column title="最低价" data-index="minimumPrice" key="minimumPrice" width="110">
          <template #default="{ record }">{{ formatMoney(record.minimumPrice) }}</template>
        </Table.Column>
        <Table.Column title="浮动系数" data-index="floatRate" key="floatRate" width="100" />
        <Table.Column title="状态" data-index="status" key="status" width="90">
          <template #default="{ record }">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">{{ record.status === 'active' ? '启用' : '停用' }}</Tag>
          </template>
        </Table.Column>
        <Table.Column title="操作" key="action" fixed="right" width="130">
          <template #default="{ record }">
            <Button type="link" size="small" @click="openEditModal(record)">编辑</Button>
            <Button type="link" size="small" danger @click="removeRule(record)">删除</Button>
          </template>
        </Table.Column>
      </Table>
    </Card>

    <Modal v-model:open="modalOpen" :title="modalTitle" :confirm-loading="saving" width="960px" @ok="saveRule">
      <div class="rule-modal-layout">
        <Form layout="vertical" class="rule-form">
          <div class="rule-form-grid">
            <Form.Item label="座位数" required>
              <InputNumber v-model:value="formSeatCount" allow-clear addon-after="座" :min="1" :precision="0" placeholder="请输入座位数" />
              <div class="field-help">只填写数字，例如 7、19、39；系统保存时会自动按“座”作为单位。</div>
            </Form.Item>
            <Form.Item label="基础价">
              <InputNumber v-model:value="form.basePrice" addon-before="¥" :min="0" :precision="2" />
              <div class="field-help">基础公里以内的起步参考价。</div>
            </Form.Item>
            <Form.Item label="基础公里">
              <InputNumber v-model:value="form.baseKilometers" addon-after="公里" :min="0" :precision="2" />
              <div class="field-help">多少公里以内先按基础价计算。</div>
            </Form.Item>
            <Form.Item label="超公里单价">
              <InputNumber v-model:value="form.extraKilometerPrice" addon-before="¥" :min="0" :precision="2" />
              <div class="field-help">超过基础公里后，每多 1 公里增加的参考费用。</div>
            </Form.Item>
            <Form.Item label="最低价">
              <InputNumber v-model:value="form.minimumPrice" addon-before="¥" :min="0" :precision="2" />
              <div class="field-help">测算金额低于最低价时，按最低价展示。</div>
            </Form.Item>
            <Form.Item label="浮动系数">
              <InputNumber v-model:value="form.floatRate" :min="0.01" :precision="4" />
              <div class="field-help">1.00 不浮动，1.10 表示上浮 10%。</div>
            </Form.Item>
            <Form.Item label="状态">
              <Select v-model:value="form.status" :options="statusOptions" />
              <div class="field-help">停用后不参与后续测算。</div>
            </Form.Item>
          </div>
          <Form.Item label="备注">
            <Textarea v-model:value="form.remark" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="可填写这条规则适合哪些线路或车队报价口径" />
          </Form.Item>
        </Form>

        <aside class="rule-help-card">
          <div class="rule-help-title">计算说明</div>
          <div class="rule-formula">参考价 = max(基础价 + 超出公里 × 超公里单价, 最低价) × 浮动系数</div>
          <div class="rule-help-block">
            <div class="rule-help-label">当前口径</div>
            <p>当前先不按地区区分报价，只按座位数和路书公里测算。</p>
          </div>
          <div class="rule-help-block">
            <div class="rule-help-label">举例</div>
            <p>39座，基础价 900，基础公里 100，超公里单价 8，最低价 900，浮动系数 1.00。</p>
            <p>如果路书 120 公里，超出 20 公里，参考价为 900 + 20 × 8 = 1060。</p>
          </div>
          <div class="rule-help-block">
            <div class="rule-help-label">用途</div>
            <p>这个价格只做外部车队询价前的参考价，不等于最终派车成本。</p>
          </div>
        </aside>
      </div>
    </Modal>

    <Drawer
      v-model:open="mapQuoteDrawerOpen"
      class="vehicle-map-quote-drawer"
      destroy-on-close
      placement="right"
      root-class-name="vehicle-map-quote-drawer-root"
      title="地图报价"
      width="calc(100vw - 32px)"
      @close="closeMapQuoteDrawer"
    >
      <div class="vehicle-map-quote-workspace">
        <div class="vehicle-map-toolbar">
          <div class="vehicle-map-toolbar-main">
            <div class="vehicle-map-title">地图报价</div>
            <div class="vehicle-map-route-line">路线：{{ mapQuoteSummaryText() }}</div>
            <div class="vehicle-map-metric-strip">
              <div class="vehicle-map-metric-item">
                <span class="vehicle-map-metric-label">总里程</span>
                <span class="vehicle-map-metric-value">{{ formatDistance(mapQuoteRoute.distanceMeters) }}</span>
              </div>
              <div class="vehicle-map-metric-item">
                <span class="vehicle-map-metric-label">预计车程</span>
                <span class="vehicle-map-metric-value">{{ formatDuration(mapQuoteRoute.durationSeconds) }}</span>
              </div>
              <div class="vehicle-map-metric-item price">
                <span class="vehicle-map-metric-label">参考报价</span>
                <span class="vehicle-map-metric-value">{{ mapQuoteResult ? formatMoney(mapQuoteResult.calculatedAmount) : '未测算' }}</span>
              </div>
            </div>
          </div>
          <div class="vehicle-map-toolbar-actions">
            <Select
              v-model:value="mapQuoteVehicleType"
              allow-clear
              show-search
              :options="quoteRuleOptions"
              placeholder="请选择座位数规则"
            />
            <Tag color="blue">{{ mapQuotePoints.length }} 个地点</Tag>
            <Button :loading="mapQuoteCalculating" @click="calculateMapQuoteRoute">计算路线</Button>
            <Button type="primary" :disabled="!mapQuoteResult" @click="applyMapQuoteToQuickQuote">回填报价</Button>
          </div>
        </div>

        <div class="vehicle-map-quote-main">
          <Spin :spinning="mapQuoteLoading" wrapper-class-name="vehicle-map-spin">
            <div class="vehicle-map-shell">
              <div ref="mapQuoteMapContainerRef" class="vehicle-map-container"></div>
              <div class="vehicle-map-search">
                <div class="vehicle-map-search-title">搜索地址或直接点地图</div>
                <Select
                  v-model:value="mapQuoteKeyword"
                  allow-clear
                  class="vehicle-map-search-select"
                  :filter-option="false"
                  :loading="mapQuoteTipLoading"
                  :options="mapQuoteTipOptions"
                  placeholder="输入地址，例如 杭州东站、西湖、宋城"
                  show-search
                  @search="handleMapQuoteSearch"
                  @select="handleMapQuoteSelect"
                />
              </div>
              <div v-if="mapQuoteMapError" class="vehicle-map-error">
                {{ mapQuoteMapError }}
              </div>
              <div v-else-if="!mapQuoteMapReady" class="vehicle-map-empty">
                地图加载中，加载完成后可以搜索地址或直接点地图选点。
              </div>
            </div>
          </Spin>

          <aside class="vehicle-map-side-panel">
            <div class="vehicle-map-side-header">
              <div>
                <div class="vehicle-map-side-title">路线点位</div>
                <div class="vehicle-map-side-tip">这是高德驾车路线距离，不是直线距离。</div>
              </div>
              <Space>
                <Button size="small" @click="clearMapQuotePoints">清空</Button>
                <Button size="small" :loading="mapQuoteCalculating" @click="calculateMapQuoteRoute">重算</Button>
              </Space>
            </div>

            <div v-if="!mapQuotePoints.length" class="vehicle-map-empty-list">
              先在左侧地图搜索地址，或直接点击地图生成出发地、途经地和目的地。
            </div>

            <div class="vehicle-map-point-list">
              <div
                v-for="(point, index) in mapQuotePoints"
                :key="`${point.pointOrder}-${point.placeName}`"
                class="vehicle-map-point-card"
              >
                <div class="vehicle-map-point-head">
                  <div class="vehicle-map-point-order">{{ index + 1 }}</div>
                  <Input v-model:value="point.placeName" class="vehicle-map-point-name" />
                  <Tag :color="index === 0 ? 'green' : index === mapQuotePoints.length - 1 ? 'red' : 'blue'">
                    {{ index === 0 ? '出发' : index === mapQuotePoints.length - 1 ? '抵达' : '途经' }}
                  </Tag>
                </div>
                <div class="vehicle-map-point-address">{{ point.address || '未记录地址' }}</div>
                <div class="vehicle-map-point-metrics">
                  <span>到下一站：{{ formatDistance(point.distanceToNextMeters) }}</span>
                  <span>车程：{{ formatDuration(point.durationToNextSeconds) }}</span>
                </div>
                <div class="vehicle-map-point-actions">
                  <Button size="small" :disabled="index === 0" @click="moveMapQuotePoint(index, -1)">上移</Button>
                  <Button size="small" :disabled="index === mapQuotePoints.length - 1" @click="moveMapQuotePoint(index, 1)">下移</Button>
                  <Button danger size="small" @click="removeMapQuotePoint(index)">删除</Button>
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
.vehicle-quote-page :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.quote-calc-panel {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.quote-calc-title {
  margin-bottom: 10px;
  font-weight: 900;
  color: #0f172a;
}

.quote-calc-grid {
  display: grid;
  grid-template-columns: 180px 160px 100px 120px 1fr;
  gap: 10px;
  align-items: center;
}

.quote-calc-result {
  font-weight: 900;
  color: #1677ff;
}

.vehicle-quote-table {
  overflow: hidden;
  border: 1px solid #edf1f7;
  border-radius: 8px;
}

.vehicle-quote-table :deep(.ant-table) {
  background: #ffffff;
}

.vehicle-quote-table :deep(.ant-table-thead > tr > th) {
  color: #374151;
  font-weight: 800;
  background: #fafafa;
  border-bottom: 1px solid #e5e7eb;
}

.vehicle-quote-table :deep(.ant-table-tbody > tr > td) {
  color: #4b5563;
  border-bottom: 1px solid #edf1f7;
}

.vehicle-quote-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fbff;
}

.vehicle-quote-table :deep(.ant-btn-link) {
  padding-inline: 4px;
  font-weight: 700;
}

.rule-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}

.rule-modal-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.rule-form {
  min-width: 0;
}

.field-help {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.rule-help-card {
  padding: 14px;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.rule-help-title {
  margin-bottom: 10px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 900;
}

.rule-formula {
  padding: 10px;
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.6;
  background: #ffffff;
  border: 1px dashed #94a3b8;
  border-radius: 8px;
}

.rule-help-block {
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px solid #e2e8f0;
}

.rule-help-block p {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.65;
}

.rule-help-label {
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 900;
}

.rule-form-grid :deep(.ant-input-number),
.quote-calc-grid :deep(.ant-select),
.quote-calc-grid :deep(.ant-input-number) {
  width: 100%;
}

:global(.vehicle-map-quote-drawer-root .ant-drawer-body) {
  padding: 0;
  overflow: hidden;
  background: #eef4f8;
}

:global(.vehicle-map-quote-drawer-root .ant-drawer-header) {
  padding: 14px 20px;
  border-bottom: 1px solid #dbeafe;
}

.vehicle-map-quote-workspace {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 58px);
  min-height: 640px;
  background:
    linear-gradient(180deg, rgb(239 246 255 / 88%), rgb(248 250 252 / 100%)),
    #f8fafc;
}

.vehicle-map-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 10px 18px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.vehicle-map-toolbar-main {
  display: grid;
  grid-template-columns: 96px minmax(260px, 1fr) auto;
  gap: 10px 14px;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.vehicle-map-title,
.vehicle-map-side-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 900;
}

.vehicle-map-route-line {
  min-width: 0;
  overflow: hidden;
  color: #475569;
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vehicle-map-metric-strip {
  display: flex;
  gap: 8px;
  align-items: center;
}

.vehicle-map-metric-item {
  display: flex;
  gap: 7px;
  align-items: baseline;
  min-height: 36px;
  padding: 7px 10px;
  background: #f8fafc;
  border: 1px solid #dbe4ee;
  border-radius: 8px;
}

.vehicle-map-metric-item.price {
  background: #eff6ff;
  border-color: #93c5fd;
}

.vehicle-map-metric-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.vehicle-map-metric-value {
  color: #0f172a;
  font-size: 15px;
  font-weight: 900;
  line-height: 1;
  white-space: nowrap;
}

.vehicle-map-metric-item.price .vehicle-map-metric-value {
  color: #0b63ce;
  font-size: 18px;
}

.vehicle-map-toolbar-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
  align-items: center;
}

.vehicle-map-toolbar-actions :deep(.ant-select) {
  width: 180px;
}

.vehicle-map-quote-main {
  position: relative;
  flex: 1;
  min-height: 0;
  padding: 12px;
}

.vehicle-map-spin,
:deep(.vehicle-map-spin .ant-spin-container) {
  min-width: 0;
  min-height: 0;
  height: 100%;
}

.vehicle-map-shell {
  position: relative;
  height: 100%;
  min-height: 560px;
  overflow: hidden;
  background: #e0f2fe;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  box-shadow: 0 16px 38px rgb(15 23 42 / 10%);
}

.vehicle-map-container {
  width: 100%;
  height: 100%;
}

.vehicle-map-search {
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

.vehicle-map-search-title {
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.vehicle-map-search-select {
  width: 100%;
}

.vehicle-map-empty,
.vehicle-map-error {
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

.vehicle-map-error {
  color: #b42318;
  border-color: #fda29b;
}

.vehicle-map-side-panel {
  position: absolute;
  top: 28px;
  right: 28px;
  bottom: 28px;
  z-index: 6;
  display: flex;
  flex-direction: column;
  width: min(440px, calc(100% - 56px));
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: rgb(255 255 255 / 96%);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 18px 46px rgb(15 23 42 / 18%);
  backdrop-filter: blur(8px);
}

.vehicle-map-side-header {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
}

.vehicle-map-side-tip {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.vehicle-map-empty-list {
  margin: 12px;
  padding: 24px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.vehicle-map-point-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
}

.vehicle-map-point-card {
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

.vehicle-map-point-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 10px 24px rgb(37 99 235 / 10%);
}

.vehicle-map-point-head {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 56px;
  gap: 8px;
  align-items: center;
}

.vehicle-map-point-order {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: #1677ff;
  font-weight: 800;
  background: #eff6ff;
  border-radius: 999px;
}

.vehicle-map-point-address {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vehicle-map-point-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.vehicle-map-point-metrics span {
  padding: 7px 8px;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.vehicle-map-point-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 900px) {
  .rule-modal-layout {
    grid-template-columns: 1fr;
  }

  .quote-calc-grid,
  .vehicle-map-toolbar,
  .vehicle-map-toolbar-main,
  .vehicle-map-toolbar-actions,
  .vehicle-map-point-metrics {
    grid-template-columns: 1fr;
  }

  .quote-calc-grid {
    display: grid;
  }

  .vehicle-map-toolbar,
  .vehicle-map-toolbar-actions {
    align-items: stretch;
  }

  .vehicle-map-metric-strip {
    flex-direction: column;
    align-items: stretch;
  }

  .vehicle-map-side-panel {
    left: 28px;
    width: auto;
  }
}
</style>
