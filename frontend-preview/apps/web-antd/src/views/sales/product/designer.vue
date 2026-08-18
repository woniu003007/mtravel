<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import {
  Alert,
  AutoComplete,
  Button,
  Card,
  Checkbox,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Spin,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { buildRegionOptions } from '#/utils/region';

import {
  getAmapJsConfig,
} from '#/api/sales/product';
import {
  deleteSalesProductDesignerDayResource,
  getSalesProductDesignerDetail,
  getSalesProductDesignerDocuments,
  getSalesProductDesignerResourceDetail,
  getSalesProductDesignerResources,
  downloadSalesProductDesignerDocument,
  generateSalesProductDesignerAdultQuote,
  generateSalesProductDesignerProductWord,
  publishSalesProductDesignerDraft,
  reorderSalesProductDesignerDayResources,
  saveSalesProductDesignerAdultQuote,
  saveSalesProductDesignerDayResource,
  type SalesProductDesignerApi,
} from '#/api/sales/product-designer';

const route = useRoute();
const router = useRouter();
const productId = computed(() => Number(Array.isArray(route.params.id) ? route.params.id[0] : route.params.id));

const resourceTypeOptions = [
  { label: '全部资源', value: undefined },
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '餐厅', value: 'restaurant' },
  { label: '购物', value: 'shopping' },
  { label: '用车', value: 'vehicle' },
  { label: '地接', value: 'ground_agent' },
  { label: '其它', value: 'other' },
];
const scenicLevelOptions = [
  { label: '未评级', value: 'unrated' },
  { label: '1A', value: '1a' },
  { label: '2A', value: '2a' },
  { label: '3A', value: '3a' },
  { label: '4A', value: '4a' },
  { label: '5A', value: '5a' },
];
const starLevelOptions = [
  { label: '未评级', value: 'unrated' },
  { label: '一星', value: '1star' },
  { label: '二星', value: '2star' },
  { label: '三星', value: '3star' },
  { label: '四星', value: '4star' },
  { label: '五星', value: '5star' },
];
const typeLabel: Record<string, string> = Object.fromEntries(resourceTypeOptions.filter((item) => item.value).map((item) => [item.value, item.label]));
const regionOptions = buildRegionOptions();
const provinceOptions = regionOptions.map(({ label, value }) => ({ label, value }));

const detail = ref<SalesProductDesignerApi.Detail>();
const documents = ref<SalesProductDesignerApi.DocumentVersion[]>([]);
const resources = ref<SalesProductDesignerApi.MapResource[]>([]);
const selectedResource = ref<SalesProductDesignerApi.ResourceDetail>();
const selectedDayResource = ref<SalesProductDesignerApi.DayResource>();
const loading = ref(false);
const resourceLoading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const publishing = ref(false);
const mapLoading = ref(false);
const mapError = ref('');
const loadError = ref('');
const drawerOpen = ref(false);
const quoteOpen = ref(false);
const previewOpen = ref(false);
const introPreview = ref<SalesProductDesignerApi.Introduction>();
const activeDayNo = ref(1);
const resourcePage = ref(1);
const resourceTotal = ref(0);
const draggedResource = ref<SalesProductDesignerApi.MapResource>();
const draggedPlanIndex = ref<number>();
const mapContainer = ref<HTMLDivElement>();
let amap: any;
let amapMarkers: any[] = [];
let amapLoader: Promise<any> | undefined;
let mapResizeTimer: number | undefined;
const amapScriptSelector = 'script[data-mtravel-amap="true"]';

const filters = reactive<SalesProductDesignerApi.ResourceQuery>({ city: '杭州市', page: 1, pageSize: 100, province: '浙江省' });
let previousProvince = filters.province;
const citySearch = ref('');
const editor = reactive({
  includeInWord: true,
  quantity: 1,
  remark: '',
  selectedIntroductionId: undefined as number | undefined,
  selectedImageIds: [] as number[],
  stayMinutes: 0,
  supplierId: undefined as number | undefined,
});
const quoteForm = reactive({
  adultSaleAmount: undefined as number | undefined,
  id: undefined as number | undefined,
  markupAmount: 0,
  plannedAdultCount: 30,
  quoteRemark: '',
  validUntil: undefined as string | undefined,
});

const provinceCityOptions = computed(() => regionOptions.find((province) => province.value === filters.province)?.children || []);
const allCityOptions = regionOptions.flatMap((province) => province.children || []);
const cityOptions = computed(() => {
  const keyword = citySearch.value.trim().replace(/市$/, '').toLowerCase();
  if (!keyword) return provinceCityOptions.value;
  return allCityOptions.filter((option) => option.value.replace(/市$/, '').toLowerCase().includes(keyword));
});
const activeDay = computed(() => detail.value?.days.find((item) => item.dayNo === activeDayNo.value));
const showScenicLevelFilter = computed(() => filters.resourceType === 'scenic');
const showStarLevelFilter = computed(() => filters.resourceType === 'hotel' || filters.resourceType === 'restaurant');
const mapResources = computed(() => resources.value.filter((item): item is SalesProductDesignerApi.MapResource & { latitude: number; longitude: number } => item.longitude != null && item.latitude != null));
const unlocatedResources = computed(() => resources.value.filter((item) => item.longitude == null || item.latitude == null));
const currentCost = computed(() => detail.value?.totalCostAmount || 0);
const selectedIntroduction = computed(() => selectedResource.value?.introductions.find((item) => item.id === editor.selectedIntroductionId));

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function noticeLines(value?: string) {
  return (value || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

function areaText(item: { province?: string; city?: string; district?: string }) {
  return [item.province, item.city, item.district].filter(Boolean).join(' / ') || '所在地未维护';
}

function hasCoordinates(item: SalesProductDesignerApi.MapResource) {
  return item.longitude != null && item.latitude != null;
}

function sameCityName(left?: string, right?: string) {
  return left?.trim().replace(/市$/, '') === right?.trim().replace(/市$/, '');
}

function onProvinceChange() {
  const previousCityOptions = regionOptions.find((province) => province.value === previousProvince)?.children || [];
  const cityWasSelectedFromPreviousProvince = previousCityOptions.some((option) => option.value === filters.city);
  if (cityWasSelectedFromPreviousProvince && !provinceCityOptions.value.some((option) => option.value === filters.city)) {
    filters.city = undefined;
  }
  previousProvince = filters.province;
  citySearch.value = '';
}

function syncProvinceFromCity(city?: string) {
  const matchedProvince = regionOptions.find((province) =>
    province.children?.some((option) => sameCityName(option.value, city)),
  );
  if (matchedProvince && matchedProvince.value !== filters.province) {
    filters.province = matchedProvince.value;
    previousProvince = matchedProvince.value;
  }
}

function onCitySearch(value: string) {
  citySearch.value = value;
  syncProvinceFromCity(value);
}

function onCityChange(value: unknown) {
  syncProvinceFromCity(typeof value === 'string' ? value : undefined);
}

function onResourceTypeChange() {
  // 等级字段只属于对应资源类型，切换类型时不能把旧筛选条件带到下一次查询。
  filters.scenicLevel = undefined;
  filters.starLevel = undefined;
}

function designerErrorMessage(error: unknown, fallback: string) {
  const errorRecord = error && typeof error === 'object' ? error as {
    message?: unknown;
    response?: { data?: { error?: unknown; message?: unknown } };
  } : undefined;
  const errorMessage = error instanceof Error
    ? error.message
    : typeof error === 'string'
      ? error
      : String(errorRecord?.response?.data?.error ?? errorRecord?.response?.data?.message ?? errorRecord?.message ?? '');
  if (/服务器内部错误|internal server error/i.test(errorMessage)) {
    return '产品设计工作台暂不可用，请确认后端数据库初始化已完成后重试';
  }
  return errorMessage || fallback;
}

async function loadDetail() {
  if (!productId.value) return;
  loading.value = true;
  loadError.value = '';
  try {
    detail.value = await getSalesProductDesignerDetail(productId.value);
    if (!detail.value.days.some((item) => item.dayNo === activeDayNo.value)) activeDayNo.value = 1;
    syncQuoteForm();
  } catch (error) {
    detail.value = undefined;
    loadError.value = designerErrorMessage(error, '工作台数据加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

async function loadResources(reset = false) {
  const keyword = filters.keyword?.trim() || '';
  if (keyword && keyword.length < 2) {
    message.warning({
      content: '资源关键词至少输入 2 个字符',
      duration: 2,
      key: 'product-designer-resource-warning',
    });
    return;
  }
  if (reset) resourcePage.value = 1;
  resourceLoading.value = true;
  try {
    const result = await getSalesProductDesignerResources({ ...filters, page: resourcePage.value });
    resources.value = reset || resourcePage.value === 1 ? result.items : [...resources.value, ...result.items];
    resourceTotal.value = result.total;
  } catch (error) {
    resources.value = [];
    resourceTotal.value = 0;
    message.error({
      content: designerErrorMessage(error, '资源加载失败，请稍后重试'),
      duration: 3,
      key: 'product-designer-resource-error',
    });
  } finally {
    resourceLoading.value = false;
  }
  await nextTick();
  renderMap();
}

function syncQuoteForm() {
  const quote = detail.value?.adultQuote;
  quoteForm.id = quote?.id;
  quoteForm.plannedAdultCount = quote?.plannedAdultCount || 30;
  quoteForm.markupAmount = quote?.markupAmount || 0;
  quoteForm.adultSaleAmount = quote?.adultSaleAmount;
  quoteForm.validUntil = quote?.validUntil;
  quoteForm.quoteRemark = quote?.quoteRemark || '';
}

async function loadAmap() {
  if (!mapContainer.value) return;
  mapLoading.value = true;
  // 当前工作台需要兼容禁用硬件加速的浏览器；高德 WebGL 底图在这类环境会只留下透明画布和标记。
  (window as any).forbidenWebGL = true;
  try {
    let AMap = (window as any).AMap;
    if (!AMap) {
      if (!amapLoader) {
        amapLoader = getAmapJsConfig().then((config) => new Promise((resolve, reject) => {
          if (!config?.key) return reject(new Error('未配置高德地图 Key'));
          (window as any)._AMapSecurityConfig = config.securityJsCode ? { securityJsCode: config.securityJsCode } : undefined;
          document.querySelector(amapScriptSelector)?.remove();
          const script = document.createElement('script');
          script.dataset.mtravelAmap = 'true';
          script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(config.key)}`;
          script.onload = () => {
            const loadedAmap = (window as any).AMap;
            if (loadedAmap) resolve(loadedAmap);
            else {
              script.remove();
              reject(new Error('高德地图脚本加载失败'));
            }
          };
          script.onerror = () => {
            script.remove();
            reject(new Error('高德地图脚本加载失败，请检查网络或代理后重试'));
          };
          document.head.appendChild(script);
        }));
      }
      AMap = await amapLoader;
    }
    if (!amap) {
      amap = new AMap.Map(mapContainer.value, {
        center: [120.15, 30.28],
        resizeEnable: true,
        viewMode: '2D',
        zoom: 8,
      });
    }
    mapError.value = '';
    return AMap;
  } catch (error) {
    amapLoader = undefined;
    document.querySelector(amapScriptSelector)?.remove();
    mapError.value = error instanceof Error ? error.message : '地图加载失败，请使用下方资源列表继续编排';
    return undefined;
  } finally {
    mapLoading.value = false;
  }
}

async function renderMap() {
  try {
    const AMap = await loadAmap();
    if (!AMap || !amap) return;
    // 筛选条件改变后，卡片宽度可能刚完成布局；先刷新地图视口，避免画布保留空白尺寸。
    amap.resize();
    amapMarkers.forEach((marker) => amap.remove(marker));
    amapMarkers = mapResources.value.map((item) => {
      const marker = new AMap.Marker({
        position: [item.longitude, item.latitude],
        title: item.resourceName,
        label: { content: `<span class="designer-map-label">${item.resourceName}</span>`, direction: 'top' },
      });
      marker.on('click', () => openResource(item));
      amap.add(marker);
      return marker;
    });
    if (amapMarkers.length) {
      amap.setFitView(amapMarkers, false, [28, 28, 28, 28]);
      if (mapResizeTimer) window.clearTimeout(mapResizeTimer);
      mapResizeTimer = window.setTimeout(() => {
        if (!amap || !mapContainer.value) return;
        amap.resize();
        amap.setFitView(amapMarkers, false, [28, 28, 28, 28]);
      }, 120);
    }
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : '地图加载失败，请使用下方资源列表继续编排';
  }
}

async function retryMap() {
  mapError.value = '';
  amapLoader = undefined;
  if (!(window as any).AMap) document.querySelector(amapScriptSelector)?.remove();
  await nextTick();
  await renderMap();
}

async function openResource(resource: SalesProductDesignerApi.MapResource) {
  detailLoading.value = true;
  drawerOpen.value = true;
  selectedDayResource.value = activeDay.value?.resources.find((item) => item.resourceId === resource.id);
  try {
    selectedResource.value = await getSalesProductDesignerResourceDetail(resource.id);
    const existing = selectedDayResource.value;
    editor.includeInWord = existing?.includeInWord ?? true;
    editor.quantity = existing?.quantity ?? 1;
    editor.remark = existing?.remark || '';
    editor.stayMinutes = existing?.stayMinutes || 0;
    editor.supplierId = existing?.supplierId ?? selectedResource.value.defaultSupplierId;
    editor.selectedIntroductionId = existing?.selectedIntroductionId ?? selectedResource.value.introductions[0]?.id;
    editor.selectedImageIds = existing?.selectedImageIds ?? [];
  } catch {
    drawerOpen.value = false;
  } finally {
    detailLoading.value = false;
  }
}

async function addSelectedResource(dayNo = activeDayNo.value) {
  if (!selectedResource.value) return;
  saving.value = true;
  try {
    await saveSalesProductDesignerDayResource({
      dayNo,
      id: selectedDayResource.value?.id,
      includeInWord: editor.includeInWord,
      productId: productId.value,
      quantity: editor.quantity,
      remark: editor.remark,
      resourceId: selectedResource.value.id,
      selectedIntroductionId: editor.selectedIntroductionId,
      selectedImageIds: editor.selectedImageIds,
      stayMinutes: editor.stayMinutes,
      supplierId: editor.supplierId,
    });
    message.success(selectedDayResource.value ? '资源编排已更新' : `已加入第 ${dayNo} 天`);
    drawerOpen.value = false;
    await loadDetail();
  } finally {
    saving.value = false;
  }
}

async function addResource(resource: SalesProductDesignerApi.MapResource, dayNo = activeDayNo.value) {
  await openResource(resource);
  await addSelectedResource(dayNo);
}

async function deletePlan(item: SalesProductDesignerApi.DayResource) {
  await deleteSalesProductDesignerDayResource({ id: item.id, productId: productId.value });
  message.success('已从产品中移除');
  await loadDetail();
}

async function reorderPlan(from: number, to: number) {
  const items = activeDay.value?.resources ? [...activeDay.value.resources] : [];
  if (to < 0 || to >= items.length || from === to) return;
  const [moved] = items.splice(from, 1);
  if (!moved) return;
  items.splice(to, 0, moved);
  await reorderSalesProductDesignerDayResources({
    dayNo: activeDayNo.value,
    dayResourceIds: items.map((item) => item.id),
    productId: productId.value,
  });
  await loadDetail();
}

function showPreview(introduction?: SalesProductDesignerApi.Introduction) {
  if (!introduction) return;
  introPreview.value = introduction;
  previewOpen.value = true;
}

function openQuote() {
  syncQuoteForm();
  quoteOpen.value = true;
}

async function saveQuote() {
  saving.value = true;
  try {
    await saveSalesProductDesignerAdultQuote({ ...quoteForm, productId: productId.value });
    quoteOpen.value = false;
    message.success('成人报价草稿已保存');
    await loadDetail();
  } finally {
    saving.value = false;
  }
}

async function generateDocument(documentType: 'adult_quote' | 'product_word') {
  try {
    const version = documentType === 'product_word'
      ? await generateSalesProductDesignerProductWord(productId.value)
      : await generateSalesProductDesignerAdultQuote(productId.value);
    documents.value = [version, ...documents.value.filter((item) => item.id !== version.id)];
    const blob = await downloadSalesProductDesignerDocument(version.id);
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = version.fileName;
    anchor.click();
    URL.revokeObjectURL(url);
    message.success(`${documentType === 'product_word' ? '产品介绍 Word' : '成人报价单'}已生成`);
  } catch {
    // request interceptor already reports the actionable server error.
  }
}

async function loadDocuments() {
  try {
    documents.value = await getSalesProductDesignerDocuments(productId.value);
  } catch {
    documents.value = [];
  }
}

async function downloadDocument(version: SalesProductDesignerApi.DocumentVersion) {
  const blob = await downloadSalesProductDesignerDocument(version.id);
  const url = URL.createObjectURL(blob);
  const anchor = window.document.createElement('a');
  anchor.href = url;
  anchor.download = version.fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}

function onResourceDragStart(resource: SalesProductDesignerApi.MapResource) {
  draggedResource.value = resource;
}

async function onDayDrop(dayNo: number) {
  if (draggedResource.value) {
    activeDayNo.value = dayNo;
    await addResource(draggedResource.value, dayNo);
    draggedResource.value = undefined;
  }
}

async function onPlanDrop(index: number) {
  if (draggedPlanIndex.value != null) {
    await reorderPlan(draggedPlanIndex.value, index);
    draggedPlanIndex.value = undefined;
  }
}

function back() {
  router.push('/sales/product/designer');
}

function editBasicInfo() {
  router.push(`/sales/product/designer/edit/${productId.value}`);
}

function completeDesign() {
  Modal.confirm({
    cancelText: '取消',
    content: '完成后，该数据将从产品设计列表移入产品管理。',
    okText: '完成设计',
    title: '确认完成当前产品设计？',
    async onOk() {
      publishing.value = true;
      try {
        await publishSalesProductDesignerDraft(productId.value);
        message.success('产品设计已完成，已进入产品管理');
        await router.push('/sales/product');
      } finally {
        publishing.value = false;
      }
    },
  });
}

watch(() => [
  filters.keyword,
  filters.resourceType,
  filters.province,
  filters.city,
  filters.scenicLevel,
  filters.starLevel,
], () => loadResources(true));
watch(activeDayNo, () => { selectedDayResource.value = undefined; });

onMounted(async () => {
  await loadDetail();
  if (detail.value?.city) {
    const matched = regionOptions
      .flatMap((province) => (province.children || []).map((city) => ({ province: province.value, city: city.value })))
      .find((option) => sameCityName(option.city, detail.value?.city));
    if (matched) {
      filters.province = matched.province;
      filters.city = matched.city;
    }
  }
  if (detail.value) await loadDocuments();
  await loadResources(true);
  await nextTick();
  renderMap();
});

onBeforeUnmount(() => {
  if (mapResizeTimer) window.clearTimeout(mapResizeTimer);
  amapMarkers.forEach((marker) => amap?.remove(marker));
  amap?.destroy?.();
  amap = undefined;
});
</script>

<template>
  <Page :title="detail?.productName || '产品设计'" description="从资源地图编排每日行程，介绍和报价保存后可生成对外产品资料。">
    <template #extra>
      <Space>
        <Button @click="back"><IconifyIcon icon="lucide:arrow-left" />返回设计列表</Button>
        <Button @click="editBasicInfo"><IconifyIcon icon="lucide:pencil" />基本信息</Button>
        <Button @click="openQuote"><IconifyIcon icon="lucide:badge-dollar-sign" />成人报价</Button>
        <Button @click="generateDocument('product_word')"><IconifyIcon icon="lucide:file-text" />产品 Word</Button>
        <Button @click="generateDocument('adult_quote')"><IconifyIcon icon="lucide:file-spreadsheet" />成人报价单</Button>
        <Button type="primary" :loading="publishing" @click="completeDesign"><IconifyIcon icon="lucide:circle-check" />完成设计</Button>
      </Space>
    </template>

    <Spin :spinning="loading">
      <Alert
        v-if="loadError"
        type="error"
        show-icon
        message="产品设计工作台暂时无法加载"
        :description="loadError"
      />
      <div v-if="detail" class="designer-shell">
        <div class="designer-summary">
          <div class="summary-item"><span class="summary-label">设计状态</span><Tag color="blue">设计中</Tag></div>
          <div class="summary-item"><span class="summary-label">行程天数</span><strong>{{ detail.travelDays }} 天</strong></div>
          <div class="summary-item"><span class="summary-label">资源成本</span><strong class="cost">{{ formatMoney(currentCost) }}</strong></div>
          <div class="summary-item summary-location"><span class="summary-label">接团城市</span>{{ areaText(detail) }}</div>
        </div>

        <div class="day-switcher">
          <div class="day-switcher-label">
            <span>行程天数</span>
            <strong>{{ detail.travelDays }} 天</strong>
          </div>
          <div class="day-tabs" role="tablist" aria-label="每日行程">
            <Button
              v-for="day in detail.days"
              :key="day.dayNo"
              :aria-pressed="activeDayNo === day.dayNo"
              :class="{ active: activeDayNo === day.dayNo }"
              class="day-tab"
              type="text"
              @dragover.prevent
              @drop="onDayDrop(day.dayNo)"
              @click="activeDayNo = day.dayNo"
            >
              <span class="day-no">D{{ day.dayNo }}</span>
              <span class="day-count">{{ day.resources.length }} 项</span>
              <span class="day-cost">{{ formatMoney(day.dayCostAmount) }}</span>
            </Button>
          </div>
        </div>

        <div class="designer-grid">

          <Card class="map-panel" :title="`资源地图 · D${activeDayNo}`">
            <template #extra>
              <Space size="small">
                <Tag color="blue">{{ mapResources.length }} 个点位</Tag>
                <Tag v-if="unlocatedResources.length" color="orange">{{ unlocatedResources.length }} 个未定位资源</Tag>
              </Space>
            </template>
            <div class="resource-filters">
              <Input v-model:value="filters.keyword" allow-clear placeholder="搜索资源名称" @press-enter="loadResources(true)">
                <template #prefix><IconifyIcon icon="lucide:search" /></template>
              </Input>
              <Select
                v-model:value="filters.resourceType"
                allow-clear
                :options="resourceTypeOptions"
                placeholder="资源类型"
                @change="onResourceTypeChange"
              />
              <Select
                v-if="showScenicLevelFilter"
                v-model:value="filters.scenicLevel"
                allow-clear
                :options="scenicLevelOptions"
                placeholder="景区等级"
              />
              <Select
                v-else-if="showStarLevelFilter"
                v-model:value="filters.starLevel"
                allow-clear
                :options="starLevelOptions"
                :placeholder="filters.resourceType === 'hotel' ? '酒店星级' : '餐厅星级'"
              />
              <Select
                v-model:value="filters.province"
                allow-clear
                show-search
                :filter-option="(input, option) => String(option?.label || '').toLowerCase().includes(input.toLowerCase())"
                :options="provinceOptions"
                placeholder="先选择省份"
                @change="onProvinceChange"
              />
              <AutoComplete
                v-model:value="filters.city"
                allow-clear
                :filter-option="false"
                :options="cityOptions"
                placeholder="输入或选择城市"
                @change="onCityChange"
                @search="onCitySearch"
              />
            </div>
            <div ref="mapContainer" class="map-container">
              <Spin v-if="mapLoading" class="map-loading" />
              <Alert
                v-if="mapError"
                :message="mapError"
                closable
                description="地图不可用时，可直接从下方资源列表加入行程。"
                show-icon
                type="warning"
                @close="mapError = ''"
              >
                <template #action><Button size="small" type="link" @click="retryMap">重新加载地图</Button></template>
              </Alert>
              <div v-if="!mapResources.length && !resourceLoading" class="map-empty">
                <Alert
                  v-if="unlocatedResources.length"
                  message="找到资源，但尚未维护地图点位"
                  description="这些资源仍会显示在下方列表，可以直接加入行程；完成地图点位后才会出现在地图上。"
                  show-icon
                  type="info"
                />
                <Empty v-else description="没有符合条件的资源" />
              </div>
            </div>
            <div class="resource-list-header">
              <strong>资源列表</strong>
              <span>当前显示 {{ resources.length }} 项</span>
            </div>
            <div class="resource-list" @scroll="undefined">
              <div
                v-for="resource in resources"
                :key="resource.id"
                class="resource-row"
                draggable="true"
                @dragstart="onResourceDragStart(resource)"
                @dblclick="addResource(resource)"
                @click="openResource(resource)"
              >
                <div class="resource-dot" :class="`type-${resource.resourceType}`"></div>
                <div class="resource-main">
                  <strong>{{ resource.resourceName }}</strong>
                  <span>{{ typeLabel[resource.resourceType] || resource.resourceType }} · {{ resource.city || '城市未维护' }}</span>
                </div>
                <div class="resource-meta">
                  <Tag v-if="!hasCoordinates(resource)" color="orange">未完成地图点位</Tag>
                  <Tag v-if="resource.procurementMode === 'not_required'" color="green">无需采购</Tag>
                  <span v-else>{{ formatMoney(resource.referenceUnitPrice) }}</span>
                  <Tooltip title="加入当前天"><Button type="text" size="small" @click.stop="addResource(resource)"><IconifyIcon icon="lucide:plus" /></Button></Tooltip>
                </div>
              </div>
              <div v-if="resourceLoading" class="list-loading"><Spin size="small" /> 正在加载资源</div>
              <div v-if="resources.length < resourceTotal && !resourceLoading" class="load-more"><Button type="link" @click="resourcePage += 1; loadResources()">加载更多</Button></div>
            </div>
          </Card>

          <Card class="plan-panel" :title="`第 ${activeDayNo} 天行程`">
            <template #extra><span class="plan-cost">{{ formatMoney(activeDay?.dayCostAmount) }}</span></template>
            <div class="drop-zone" @dragover.prevent @drop="onDayDrop(activeDayNo)">
              <div v-if="!activeDay?.resources.length" class="plan-empty"><Empty description="把资源拖到这里开始编排" /></div>
              <div
                v-for="(item, index) in activeDay?.resources"
                :key="item.id"
                class="plan-row"
                draggable="true"
                @dragstart="draggedPlanIndex = index"
                @dragover.prevent
                @drop.stop="onPlanDrop(index)"
              >
                <span class="plan-index">{{ index + 1 }}</span>
                <div class="plan-main" @click="openResource(resources.find((resource) => resource.id === item.resourceId) || { id: item.resourceId, resourceName: item.resourceName, resourceType: item.resourceType, procurementMode: item.procurementMode, latitude: item.latitude || 0, longitude: item.longitude || 0, referenceUnitPrice: item.unitPrice, status: 'active' })">
                  <strong>{{ item.resourceName }}</strong>
                  <span>{{ typeLabel[item.resourceType] || item.resourceType }} · {{ item.stayMinutes }} 分钟</span>
                </div>
                <div class="plan-actions">
                  <Tooltip title="上移"><Button type="text" size="small" :disabled="index === 0" @click="reorderPlan(index, index - 1)"><IconifyIcon icon="lucide:chevron-up" /></Button></Tooltip>
                  <Tooltip title="下移"><Button type="text" size="small" :disabled="index === (activeDay?.resources.length || 1) - 1" @click="reorderPlan(index, index + 1)"><IconifyIcon icon="lucide:chevron-down" /></Button></Tooltip>
                  <Tooltip title="移除"><Button danger type="text" size="small" @click="deletePlan(item)"><IconifyIcon icon="lucide:trash-2" /></Button></Tooltip>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </Spin>

    <Drawer v-model:open="drawerOpen" :destroy-on-close="true" :width="520" title="资源详情" @close="selectedResource = undefined">
      <Spin :spinning="detailLoading">
        <template v-if="selectedResource">
          <div class="resource-detail-head">
            <div><Tag color="blue">{{ typeLabel[selectedResource.resourceType] || selectedResource.resourceType }}</Tag><h3>{{ selectedResource.resourceName }}</h3><p>{{ areaText(selectedResource) }} · {{ selectedResource.address || '地址未维护' }}</p></div>
          </div>
          <Alert v-if="selectedResource.procurementMode === 'not_required'" message="无需采购资源，成本按 0 元处理" type="success" show-icon />
          <Form layout="vertical" class="detail-form">
            <Form.Item label="供应商">
              <Select v-model:value="editor.supplierId" :disabled="selectedResource.procurementMode === 'not_required'" allow-clear placeholder="选择当前资源的有效供应商">
                <Select.Option v-for="supplier in selectedResource.suppliers" :key="supplier.supplierId" :value="supplier.supplierId">{{ supplier.supplierName }} · {{ formatMoney(supplier.referenceUnitPrice) }}<template v-if="supplier.isDefault">（默认）</template></Select.Option>
              </Select>
            </Form.Item>
            <Form.Item label="景点介绍版本">
              <Select v-model:value="editor.selectedIntroductionId" allow-clear placeholder="选择已发布介绍">
                <Select.Option v-for="intro in selectedResource.introductions" :key="intro.id" :value="intro.id">{{ intro.title }} · v{{ intro.indexVersion }}<Button type="link" size="small" @click.stop="showPreview(intro)">预览</Button></Select.Option>
              </Select>
            </Form.Item>
            <div v-if="selectedIntroduction" class="intro-preview-inline">
              <div class="intro-title">{{ selectedIntroduction.title }}</div>
              <p>{{ selectedIntroduction.content }}</p>
              <div v-if="noticeLines(selectedIntroduction.noticeContent).length" class="intro-notice">
                <div
                  v-for="(line, index) in noticeLines(selectedIntroduction.noticeContent)"
                  :key="`${index}-${line}`"
                >
                  {{ line }}
                </div>
              </div>
            </div>
            <Form.Item v-if="selectedResource.images.length" label="产品配图">
              <Checkbox.Group v-model:value="editor.selectedImageIds">
                <Space direction="vertical">
                  <Checkbox v-for="image in selectedResource.images" :key="image.id" :value="image.id">
                    {{ image.originalFilename }}<span class="image-meta"> · {{ image.isCover ? '封面' : '图片' }}</span>
                  </Checkbox>
                </Space>
              </Checkbox.Group>
            </Form.Item>
            <Form.Item label="停留时间（分钟）"><InputNumber v-model:value="editor.stayMinutes" :min="0" :max="1440" style="width: 100%" /></Form.Item>
            <Form.Item label="成本数量"><InputNumber v-model:value="editor.quantity" :min="0" :precision="2" style="width: 100%" /></Form.Item>
            <Form.Item label="行程备注"><Input v-model:value="editor.remark" allow-clear /></Form.Item>
            <Form.Item><Checkbox v-model:checked="editor.includeInWord">纳入产品介绍 Word</Checkbox></Form.Item>
          </Form>
          <div class="drawer-actions">
            <Button @click="drawerOpen = false">取消</Button>
            <Button type="primary" :loading="saving" @click="addSelectedResource()">{{ selectedDayResource ? '保存编排' : `加入第 ${activeDayNo} 天` }}</Button>
          </div>
        </template>
      </Spin>
    </Drawer>

    <Card v-if="detail" class="document-history" title="已生成文件">
      <div v-if="documents.length" class="document-list">
        <div v-for="item in documents" :key="item.id" class="document-row">
          <div class="document-main"><strong>{{ item.fileName }}</strong><span>{{ item.documentType === 'product_word' ? '产品介绍 Word' : '成人报价单' }} · v{{ item.versionNo }}</span></div>
          <Button v-if="item.generateStatus === 'success'" type="link" size="small" @click="downloadDocument(item)">下载</Button>
          <Tag v-else color="red">生成失败</Tag>
        </div>
      </div>
      <Empty v-else description="暂无生成文件" />
    </Card>

    <Modal v-model:open="previewOpen" :footer="null" title="介绍正文预览" width="720px">
      <div v-if="introPreview" class="intro-preview-modal">
        <Tag color="blue">已发布 · v{{ introPreview.indexVersion }}</Tag>
        <h3>{{ introPreview.title }}</h3>
        <p>{{ introPreview.content }}</p>
        <div v-if="noticeLines(introPreview.noticeContent).length" class="intro-notice">
          <div
            v-for="(line, index) in noticeLines(introPreview.noticeContent)"
            :key="`${index}-${line}`"
          >
            {{ line }}
          </div>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="quoteOpen" :confirm-loading="saving" title="成人报价草稿" width="520px" ok-text="保存" cancel-text="取消" @ok="saveQuote">
      <Alert message="成本由后端按已选资源重新计算，前端不能直接改成本。" type="info" show-icon />
      <div class="quote-cost"><span>当前资源总成本</span><strong>{{ formatMoney(currentCost) }}</strong></div>
      <Form layout="vertical" class="quote-form">
        <Form.Item label="计划成人数"><InputNumber v-model:value="quoteForm.plannedAdultCount" :min="1" style="width: 100%" /></Form.Item>
        <Form.Item label="人工加价（元/人）"><InputNumber v-model:value="quoteForm.markupAmount" :min="0" :precision="2" style="width: 100%" /></Form.Item>
        <Form.Item label="成人对外价（元/人）"><InputNumber v-model:value="quoteForm.adultSaleAmount" :min="0" :precision="2" style="width: 100%" /></Form.Item>
        <Form.Item label="报价有效期"><Input v-model:value="quoteForm.validUntil" placeholder="YYYY-MM-DD" /></Form.Item>
        <Form.Item label="报价备注"><Input.TextArea v-model:value="quoteForm.quoteRemark" :rows="3" /></Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.designer-shell {
  --designer-map-height: clamp(460px, calc(100vh - 470px), 680px);
  min-height: calc(100vh - 172px);
}
.designer-summary { display: flex; align-items: stretch; gap: 0; padding: 0 16px; margin-bottom: 12px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; color: #334155; }
.summary-item { min-width: 118px; padding: 11px 20px 11px 0; margin-right: 20px; border-right: 1px solid #f0f0f0; }
.summary-label { display: block; margin-bottom: 3px; color: #64748b; font-size: 12px; }
.summary-location { min-width: 160px; padding-right: 0; margin-right: 0; margin-left: auto; border-right: 0; text-align: right; }
.cost, .plan-cost { color: #1677ff; }
.day-switcher { display: flex; align-items: center; gap: 12px; padding: 8px 12px; margin-bottom: 12px; overflow: hidden; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
.day-switcher-label { display: grid; min-width: 82px; padding-right: 12px; border-right: 1px solid #f0f0f0; }
.day-switcher-label span { color: #64748b; font-size: 12px; }
.day-switcher-label strong { margin-top: 2px; color: #1f2937; }
.day-tabs { display: flex; min-width: 0; flex: 1; gap: 8px; overflow-x: auto; scrollbar-width: thin; }
.day-tabs :deep(.day-tab.ant-btn) { display: grid; grid-template-columns: 34px minmax(42px, 1fr); grid-template-rows: 1fr 1fr; min-width: 126px; height: 52px; padding: 6px 10px; border: 1px solid #e5e7eb; border-radius: 4px; background: #fff; color: #475569; text-align: left; }
.day-tabs :deep(.day-tab.ant-btn:hover) { border-color: #91caff; background: #f6faff; color: #0958d9; }
.day-tabs :deep(.day-tab.ant-btn.active) { border-color: #69b1ff; background: #eaf3ff; color: #0958d9; box-shadow: inset 3px 0 0 #1677ff; }
.day-no { grid-row: span 2; align-self: center; font-size: 16px; font-weight: 700; }
.day-count { align-self: end; font-size: 12px; line-height: 18px; }
.day-cost { align-self: start; overflow: hidden; color: #64748b; font-size: 12px; line-height: 18px; text-overflow: ellipsis; }
.designer-grid { display: grid; grid-template-columns: minmax(760px, 1fr) minmax(340px, 390px); gap: 12px; align-items: start; }
.plan-panel { position: sticky; top: 12px; }
.map-panel :deep(.ant-card-body) { padding: 16px; }
.resource-filters { display: grid; grid-template-columns: minmax(220px, 1fr) 140px 140px 160px 120px; gap: 8px; margin-bottom: 12px; }
.map-container { position: relative; height: var(--designer-map-height); overflow: hidden; border: 1px solid #d9d9d9; border-radius: 4px; background: #f5f7fa; }
.map-container :deep(.amap-marker-label) { padding: 3px 6px; border: 1px solid #91caff; border-radius: 3px; background: rgb(255 255 255 / 94%); color: #1f2937; font-size: 12px; line-height: 18px; box-shadow: 0 1px 4px rgb(15 23 42 / 14%); }
.map-loading { position: absolute; inset: 45% auto auto 50%; z-index: 2; }
.map-empty { display: grid; height: 100%; place-items: center; }
.resource-list-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 2px 9px; color: #1f2937; }
.resource-list-header span { color: #8c8c8c; font-size: 12px; }
.resource-list { max-height: 248px; overflow-y: auto; border: 1px solid #f0f0f0; border-radius: 4px; }
.resource-row { display: flex; align-items: center; gap: 9px; min-height: 52px; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; cursor: grab; }
.resource-row:last-child { border-bottom: 0; }
.resource-row:hover { background: #fafafa; }
.resource-dot { width: 9px; height: 9px; flex: 0 0 auto; border-radius: 50%; background: #1677ff; }
.type-hotel { background: #722ed1; }.type-restaurant { background: #fa8c16; }.type-shopping { background: #13c2c2; }.type-vehicle { background: #52c41a; }
.resource-main, .plan-main { min-width: 0; flex: 1; }
.resource-main strong, .resource-main span, .plan-main strong, .plan-main span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.resource-main span, .plan-main span { margin-top: 2px; color: #64748b; font-size: 12px; }
.resource-meta { display: flex; align-items: center; gap: 4px; color: #1677ff; font-size: 12px; }
.list-loading, .load-more { padding: 10px; text-align: center; color: #64748b; }
.plan-panel { max-height: calc(100vh - 190px); overflow: hidden; }
.plan-panel :deep(.ant-card-body) { max-height: calc(100vh - 248px); overflow-y: auto; padding: 12px 16px; }
.drop-zone { min-height: calc(var(--designer-map-height) + 24px); }
.plan-empty { display: grid; min-height: 320px; place-items: center; }
.plan-row { display: flex; align-items: center; gap: 8px; padding: 10px 0; border-bottom: 1px solid #f0f0f0; cursor: grab; }
.plan-index { display: grid; width: 24px; height: 24px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: #eaf3ff; color: #0958d9; font-size: 12px; font-weight: 600; }
.plan-actions { display: flex; flex: 0 0 auto; }
.resource-detail-head h3 { margin: 7px 0 2px; font-size: 20px; }
.resource-detail-head p { margin: 0 0 18px; color: #64748b; }
.detail-form { margin-top: 18px; }
.intro-preview-inline { max-height: 150px; overflow: auto; padding: 10px; margin: -6px 0 16px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 4px; }
.intro-title { margin-bottom: 5px; font-weight: 600; }
.intro-preview-inline p, .intro-preview-modal p { margin: 0; white-space: pre-wrap; line-height: 1.75; }
.intro-notice { display: grid; gap: 4px; margin-top: 8px; color: #cf1322; white-space: pre-wrap; line-height: 1.75; }
.drawer-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 14px; border-top: 1px solid #f0f0f0; }
.intro-preview-modal h3 { margin: 10px 0; }
.quote-cost { display: flex; justify-content: space-between; padding: 12px 0; margin: 12px 0; border-bottom: 1px solid #f0f0f0; }
.quote-cost strong { color: #1677ff; font-size: 18px; }
.quote-form { margin-top: 14px; }
.document-history { margin-top: 12px; }
.document-list { display: grid; gap: 6px; }
.document-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.document-main { min-width: 0; flex: 1; }
.document-main strong, .document-main span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.document-main span, .image-meta { color: #64748b; font-size: 12px; }
@media (max-width: 1180px) {
  .designer-grid { grid-template-columns: minmax(0, 1fr); }
  .plan-panel { position: static; grid-column: 1 / -1; max-height: none; }
  .plan-panel :deep(.ant-card-body) { max-height: none; }
  .drop-zone { min-height: 180px; }
  .plan-empty { min-height: 160px; }
}
@media (max-width: 900px) {
  .designer-shell { --designer-map-height: 430px; }
  .resource-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .designer-shell { --designer-map-height: 380px; }
  .designer-summary { flex-wrap: wrap; padding: 4px 12px; }
  .summary-item { min-width: calc(50% - 12px); padding: 8px 12px 8px 0; margin-right: 12px; border-right: 0; }
  .summary-location { margin-left: 0; text-align: left; }
  .day-switcher { align-items: stretch; flex-direction: column; gap: 8px; }
  .day-switcher-label { display: flex; align-items: center; justify-content: space-between; padding-right: 0; padding-bottom: 6px; border-right: 0; border-bottom: 1px solid #f0f0f0; }
  .designer-grid { display: block; }
  .map-panel, .plan-panel { position: static; margin-bottom: 12px; }
  .resource-filters { grid-template-columns: 1fr; }
  .resource-filters > :last-child { grid-column: auto; }
  .resource-list { max-height: 220px; }
}
</style>
