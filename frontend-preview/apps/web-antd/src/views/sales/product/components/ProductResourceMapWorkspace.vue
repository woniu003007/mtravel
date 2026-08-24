<script lang="ts" setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import {
  Alert,
  AutoComplete,
  Button,
  Empty,
  Input,
  Modal,
  Select,
  Spin,
  Tag,
  Tooltip,
} from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { getAmapJsConfig } from '#/api/sales/product';
import type { SalesProductDesignerApi } from '#/api/sales/product-designer';
import {
  buildProductResourceMarkerHtml,
  productResourceMarkerZIndex,
} from './product-resource-map-marker';

type LocatedResource = SalesProductDesignerApi.MapResource & {
  latitude: number;
  longitude: number;
};

const props = defineProps<{
  activeDayNo: number;
  arrangedResourceIds?: Set<number>;
  cityOptions: { label: string; value: string }[];
  dayDestination?: Pick<SalesProductDesignerApi.DayPlan, 'destinationCity' | 'destinationDistrict' | 'destinationProvince'>;
  filters: SalesProductDesignerApi.ResourceQuery;
  mapResources: LocatedResource[];
  open: boolean;
  provinceOptions: { label: string; value: string }[];
  resourceLoading: boolean;
  resourceTotal: number;
  resources: SalesProductDesignerApi.MapResource[];
  resourceTypeOptions: { label: string; value?: string }[];
  scenicLevelOptions: { label: string; value: string }[];
  selectedResourceId?: number;
  showScenicLevelFilter: boolean;
  showStarLevelFilter: boolean;
  starLevelOptions: { label: string; value: string }[];
}>();

const emit = defineEmits<{
  (event: 'activate-resource', resource: SalesProductDesignerApi.MapResource): void;
  (event: 'city-search', value: string): void;
  (event: 'close'): void;
  (event: 'filter-change', key: keyof SalesProductDesignerApi.ResourceQuery, value: unknown): void;
  (event: 'load-more'): void;
  (event: 'reset'): void;
}>();

const mapContainer = ref<HTMLDivElement>();
const mapLoading = ref(false);
const mapError = ref('');
const fallbackMarkers = ref<any[]>([]);
let mapInstance: any;
let markerInstances: any[] = [];
let clusterInstance: any;
let amapLoader: Promise<any> | undefined;
const amapScriptSelector = 'script[data-mtravel-amap="true"]';

const typeLabel: Record<string, string> = {
  hotel: '酒店',
  other: '其它',
  restaurant: '餐厅',
  scenic: '景区',
  shopping: '购物',
  traffic: '交通',
  vehicle: '用车',
};

const locatedCount = computed(() => props.mapResources.length);
const unlocatedCount = computed(() => props.resources.length - locatedCount.value);

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function hasCoordinates(resource: SalesProductDesignerApi.MapResource) {
  return resource.longitude != null && resource.latitude != null;
}

function areaText(resource: SalesProductDesignerApi.MapResource) {
  return [resource.city, resource.district].filter(Boolean).join(' / ') || '城市未维护';
}

function activateResource(resource: SalesProductDesignerApi.MapResource) {
  emit('activate-resource', resource);
}

function clearMapOverlays() {
  if (clusterInstance?.setMap) {
    clusterInstance.setMap(null);
  }
  clusterInstance = undefined;
  if (mapInstance && markerInstances.length) {
    mapInstance.remove(markerInstances);
  }
  markerInstances = [];
  fallbackMarkers.value = [];
}

async function loadAmap() {
  // 与紧凑地图保持一致：禁用高德 WebGL 路径，避免部分浏览器只绘制底图而丢失覆盖物。
  (window as any).forbidenWebGL = true;
  if ((window as any).AMap) return (window as any).AMap;
  if (amapLoader) return amapLoader;
  amapLoader = getAmapJsConfig().then((config) => new Promise((resolve, reject) => {
    if (!config?.key) {
      reject(new Error('未配置高德地图 Key'));
      return;
    }
    (window as any)._AMapSecurityConfig = config.securityJsCode
      ? { securityJsCode: config.securityJsCode }
      : undefined;
    document.querySelector(amapScriptSelector)?.remove();
    const script = document.createElement('script');
    script.dataset.mtravelAmap = 'true';
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(config.key)}`;
    script.onload = () => {
      const amap = (window as any).AMap;
      if (amap) resolve(amap);
      else reject(new Error('高德地图脚本加载失败'));
    };
    script.onerror = () => reject(new Error('高德地图脚本加载失败，请检查网络或代理后重试'));
    document.head.append(script);
  }));
  try {
    return await amapLoader;
  } catch (error) {
    amapLoader = undefined;
    throw error;
  }
}

async function loadMarkerClusterPlugin(AMap: any) {
  if (AMap.MarkerClusterer || AMap.MarkerCluster || typeof AMap.plugin !== 'function') return;
  await new Promise<void>((resolve) => {
    let settled = false;
    const finish = () => {
      if (settled) return;
      settled = true;
      resolve();
    };
    window.setTimeout(finish, 800);
    try {
      AMap.plugin(['AMap.MarkerClusterer'], finish);
    } catch {
      finish();
    }
  });
}

function createMarker(AMap: any, resource: LocatedResource) {
  const selected = resource.id === props.selectedResourceId;
  const arranged = props.arrangedResourceIds?.has(resource.id) || false;
  const marker = new AMap.Marker({
    content: buildProductResourceMarkerHtml(resource, {
      arranged,
      pending: resource.procurementMode !== 'not_required' && !resource.defaultSupplierId,
      selected,
    }),
    offset: new AMap.Pixel(-12, -30),
    position: [resource.longitude, resource.latitude],
    title: resource.resourceName,
    zIndex: productResourceMarkerZIndex({ arranged, selected }),
  });
  marker.on('click', () => activateResource(resource));
  return marker;
}

async function renderMarkers(AMap: any) {
  if (!mapInstance) return;
  clearMapOverlays();
  markerInstances = props.mapResources.map((resource) => createMarker(AMap, resource));
  if (!markerInstances.length) return;

  // 先按原始资源标记调整视口，再交给聚合器接管；否则聚合器尚未生成覆盖物时，
  // setFitView 可能只看到一个聚合点，导致所有资源缩成一个大圆。
  mapInstance.setFitView(markerInstances, false, [32, 32, 32, 32]);

  await loadMarkerClusterPlugin(AMap);
  const ClusterConstructor = AMap.MarkerClusterer || AMap.MarkerCluster;
  if (ClusterConstructor) {
    try {
      // AMap 2.x 聚合器接收带 lnglat 的数据项，而不是 Marker 实例；
      // originMarker 让单点继续复用带资源名称和点击事件的原始标记。
      const clusterData = props.mapResources.map((resource, index) => ({
        lnglat: [resource.longitude, resource.latitude],
        originMarker: markerInstances[index],
      }));
      clusterInstance = new ClusterConstructor(mapInstance, clusterData, {
        gridSize: 60,
        maxZoom: 17,
      });
    } catch {
      // 聚合插件版本不兼容时仍显示普通点位，不能阻塞资源列表。
      fallbackMarkers.value = [...markerInstances];
      mapInstance.add(fallbackMarkers.value);
    }
  } else {
    fallbackMarkers.value = [...markerInstances];
    mapInstance.add(fallbackMarkers.value);
  }
}

async function initializeMap() {
  if (!props.open || !mapContainer.value) return;
  mapLoading.value = true;
  mapError.value = '';
  try {
    const AMap = await loadAmap();
    if (!props.open || !mapContainer.value) return;
    if (!mapInstance) {
      mapInstance = new AMap.Map(mapContainer.value, {
        center: [120.15, 30.28],
        resizeEnable: true,
        viewMode: '2D',
        zoom: 8,
      });
    } else {
      mapInstance.resize?.();
    }
    await renderMarkers(AMap);
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : '地图加载失败，请使用右侧资源列表继续编排';
  } finally {
    mapLoading.value = false;
  }
}

function destroyMap() {
  clearMapOverlays();
  mapInstance?.destroy?.();
  mapInstance = undefined;
  mapError.value = '';
  mapLoading.value = false;
}

function handleFilterChange(key: keyof SalesProductDesignerApi.ResourceQuery, value: unknown) {
  emit('filter-change', key, value);
}

watch(() => props.open, async (open) => {
  if (!open) {
    destroyMap();
    return;
  }
  await nextTick();
  await initializeMap();
});

watch(
  () => [props.mapResources, props.selectedResourceId, props.arrangedResourceIds],
  async () => {
    if (!props.open || !mapInstance) return;
    const AMap = await loadAmap();
    await renderMarkers(AMap);
  },
  { deep: true },
);

onBeforeUnmount(destroyMap);
</script>

<template>
  <Modal
    :open="open"
    :footer="null"
    :closable="false"
    :mask-closable="false"
    :destroy-on-close="true"
    :width="'100vw'"
    wrap-class-name="resource-map-fullscreen-modal"
    @cancel="emit('close')"
  >
    <div class="resource-map-workspace">
      <header class="resource-map-toolbar">
        <div class="resource-map-heading">
          <div class="resource-map-title">资源地图 · D{{ activeDayNo }} · {{ dayDestination?.destinationCity || '未设置城市' }}</div>
          <div class="resource-map-stats">
            <Tag color="blue">{{ locatedCount }} 个点位</Tag>
            <Tag v-if="unlocatedCount > 0" color="orange">{{ unlocatedCount }} 个未定位</Tag>
          </div>
        </div>
        <div class="resource-map-destination">
          <span>住宿城市</span>
          <Tag color="blue">{{ dayDestination?.destinationCity || '未设置' }}</Tag>
        </div>
        <div class="resource-map-filters">
          <Input
            :value="filters.keyword"
            allow-clear
            placeholder="搜索资源名称"
            @press-enter="handleFilterChange('keyword', filters.keyword)"
            @update:value="handleFilterChange('keyword', $event)"
          >
            <template #prefix><IconifyIcon icon="lucide:search" /></template>
          </Input>
          <Select
            :value="filters.resourceType"
            allow-clear
            :options="resourceTypeOptions"
            placeholder="资源类型"
            @change="handleFilterChange('resourceType', $event)"
          />
          <Select
            v-if="showScenicLevelFilter"
            :value="filters.scenicLevel"
            allow-clear
            :options="scenicLevelOptions"
            placeholder="景区等级"
            @change="handleFilterChange('scenicLevel', $event)"
          />
          <Select
            v-else-if="showStarLevelFilter"
            :value="filters.starLevel"
            allow-clear
            :options="starLevelOptions"
            placeholder="酒店/餐厅接待标准"
            @change="handleFilterChange('starLevel', $event)"
          />
          <Select
            :value="filters.province"
            allow-clear
            show-search
            :options="provinceOptions"
            placeholder="先选择省份"
            @change="handleFilterChange('province', $event)"
          />
          <AutoComplete
            :value="filters.city"
            allow-clear
            :filter-option="false"
            :options="cityOptions"
            placeholder="输入或选择城市"
            @change="handleFilterChange('city', $event)"
            @search="emit('city-search', $event)"
          />
        </div>
        <div class="resource-map-toolbar-actions">
          <Button @click="emit('reset')">重置</Button>
          <Button type="primary" @click="emit('close')">关闭全屏</Button>
        </div>
      </header>

      <div class="resource-map-body">
        <main class="resource-map-canvas-wrap">
          <Spin :spinning="mapLoading">
            <div ref="mapContainer" class="resource-map-canvas">
              <Alert
                v-if="mapError"
                :message="mapError"
                description="地图不可用时，可直接从右侧资源列表加入行程。"
                show-icon
                type="warning"
              />
              <div v-else-if="!mapResources.length && !resourceLoading" class="resource-map-empty">
                <Empty description="没有符合条件的地图点位" />
              </div>
            </div>
          </Spin>
          <div v-if="fallbackMarkers.length" class="map-cluster-fallback">
            地图聚合不可用，已切换为普通点位展示
          </div>
        </main>

        <aside class="resource-map-list-panel">
          <div class="resource-map-list-heading">
            <div>
              <strong>资源列表</strong>
              <span>当前显示 {{ resources.length }} / {{ resourceTotal }} 项</span>
            </div>
            <Tag color="blue">{{ dayDestination?.destinationCity || `D${activeDayNo}` }}</Tag>
          </div>
          <div class="resource-map-list">
            <div
              v-for="resource in resources"
              :key="resource.id"
              class="resource-map-row"
              :class="{ 'is-selected': resource.id === selectedResourceId }"
              role="button"
              tabindex="0"
              @click="activateResource(resource)"
              @keydown.enter="activateResource(resource)"
            >
              <div class="resource-map-row-dot" :class="`type-${resource.resourceType}`"></div>
              <div class="resource-map-row-main">
                <strong>{{ resource.resourceName }}</strong>
                <span>{{ typeLabel[resource.resourceType] || resource.resourceType }} · {{ areaText(resource) }}</span>
              </div>
              <div class="resource-map-row-actions">
                <Tooltip v-if="!hasCoordinates(resource)" title="该资源尚未维护地图点位">
                  <Tag color="orange">未定位</Tag>
                </Tooltip>
                <span v-else class="resource-map-price">{{ formatMoney(resource.referenceUnitPrice) }}</span>
                <Tag v-if="arrangedResourceIds?.has(resource.id)" color="blue">已安排</Tag>
              </div>
            </div>
            <div v-if="resourceLoading" class="resource-map-list-state"><Spin size="small" /> 正在加载资源</div>
            <Empty v-else-if="!resources.length" description="没有符合条件的资源" />
            <div v-if="resources.length < resourceTotal && !resourceLoading" class="resource-map-load-more">
              <Button type="link" @click="emit('load-more')">加载更多</Button>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.resource-map-workspace { display: flex; height: calc(100vh - 48px); min-height: 520px; flex-direction: column; background: #f5f7fa; }
.resource-map-toolbar { display: flex; align-items: center; gap: 16px; min-height: 64px; padding: 10px 18px; background: #fff; border-bottom: 1px solid #e5e7eb; }
.resource-map-heading { display: flex; flex: 0 0 auto; align-items: center; gap: 10px; min-width: 220px; }
.resource-map-title { color: #1f2937; font-size: 16px; font-weight: 650; white-space: nowrap; }
.resource-map-stats { display: flex; gap: 4px; white-space: nowrap; }
.resource-map-destination { display: flex; min-width: 150px; align-items: center; gap: 7px; color: #64748b; font-size: 12px; white-space: nowrap; }
.resource-map-destination :deep(.ant-tag) { margin-inline-end: 0; }
.resource-map-filters { display: grid; min-width: 0; flex: 1; grid-template-columns: minmax(200px, 1.45fr) 130px 160px 140px 160px; gap: 8px; }
.resource-map-toolbar-actions { display: flex; flex: 0 0 auto; gap: 8px; }
.resource-map-body { display: grid; min-height: 0; flex: 1; grid-template-columns: minmax(0, 1fr) 292px; gap: 10px; padding: 10px; }
.resource-map-canvas-wrap { position: relative; min-width: 0; min-height: 0; overflow: hidden; border: 1px solid #d9d9d9; border-radius: 6px; background: #f5f7fa; }
.resource-map-canvas-wrap :deep(.ant-spin-nested-loading), .resource-map-canvas-wrap :deep(.ant-spin-container) { width: 100%; height: 100%; }
.resource-map-canvas { position: relative; width: 100%; height: 100%; min-height: 420px; }
.resource-map-canvas :deep(.ant-alert) { position: absolute; top: 20px; right: 20px; left: 20px; z-index: 3; }
.resource-map-empty { display: grid; height: 100%; place-items: center; }
.resource-map-list-panel { display: flex; min-width: 0; min-height: 0; flex-direction: column; overflow: hidden; border: 1px solid #e5e7eb; border-radius: 6px; background: #fff; }
.resource-map-list-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 10px 12px 9px; border-bottom: 1px solid #e5e7eb; }
.resource-map-list-heading strong, .resource-map-list-heading span { display: block; }
.resource-map-list-heading strong { color: #1f2937; font-size: 14px; }
.resource-map-list-heading span { margin-top: 3px; color: #8c8c8c; font-size: 12px; }
.resource-map-list { min-height: 0; flex: 1; overflow-y: auto; }
.resource-map-row { display: flex; align-items: center; gap: 6px; min-height: 50px; padding: 6px 10px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background .18s ease, border-color .18s ease; }
.resource-map-row:hover, .resource-map-row.is-selected { background: #f6faff; }
.resource-map-row.is-selected { border-left: 3px solid #1677ff; padding-left: 7px; }
.resource-map-row-dot { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: #1677ff; }
.resource-map-row-dot.type-hotel { background: #722ed1; }.resource-map-row-dot.type-restaurant { background: #fa8c16; }.resource-map-row-dot.type-shopping { background: #13c2c2; }.resource-map-row-dot.type-vehicle { background: #52c41a; }
.resource-map-row-main { min-width: 0; flex: 1; }
.resource-map-row-main strong, .resource-map-row-main span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.resource-map-row-main strong { color: #334155; font-size: 12px; }
.resource-map-row-main span { margin-top: 2px; color: #64748b; font-size: 11px; }
.resource-map-row-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 6px; }
.resource-map-row-actions :deep(.ant-tag) { margin-inline-end: 0; }
.resource-map-price { color: #1677ff; font-size: 12px; white-space: nowrap; }
.resource-map-list-state, .resource-map-load-more { padding: 14px; color: #64748b; text-align: center; }
.map-cluster-fallback { position: absolute; right: 16px; bottom: 16px; padding: 6px 10px; color: #8c5a00; font-size: 12px; background: #fffbe6; border: 1px solid #ffe58f; border-radius: 4px; }
:global(.resource-map-fullscreen-modal .ant-modal) { top: 0; width: 100vw !important; max-width: none; padding-bottom: 0; }
:global(.resource-map-fullscreen-modal .ant-modal-content) { height: 100vh; overflow: hidden; border-radius: 0; }
:global(.resource-map-fullscreen-modal .ant-modal-body) { height: 100%; padding: 0; }
@media (max-width: 1280px) {
  .resource-map-toolbar { align-items: stretch; flex-wrap: wrap; }
  .resource-map-heading { min-width: 210px; }
  .resource-map-destination { flex: 1; }
  .resource-map-filters { order: 3; flex-basis: 100%; }
}
@media (max-width: 900px) {
  .resource-map-body { grid-template-columns: 1fr; grid-template-rows: minmax(360px, 1fr) minmax(240px, 40%); }
  .resource-map-list-panel { min-height: 240px; }
  .resource-map-toolbar { gap: 10px; padding: 10px 12px; }
  .resource-map-destination { min-width: 150px; }
  .resource-map-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .resource-map-toolbar-actions { margin-left: auto; }
}
@media (max-width: 560px) {
  .resource-map-heading { width: 100%; justify-content: space-between; }
  .resource-map-destination { width: 100%; }
  .resource-map-filters { grid-template-columns: 1fr; }
  .resource-map-toolbar-actions { width: 100%; justify-content: flex-end; }
  .resource-map-body { padding: 8px; gap: 8px; }
  .resource-map-row { align-items: flex-start; }
  .resource-map-row-actions { flex-direction: column; align-items: flex-end; }
}
</style>
