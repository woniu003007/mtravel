<script lang="ts" setup>
import type { Sortable } from '@vben/hooks';

import { Page } from '@vben/common-ui';
import { useSortable } from '@vben/hooks';
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
  Radio,
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
import { getProductDictionaryAll } from '#/api/enterprise/product-dictionary';
import { downloadPurchaseResourceImage } from '#/api/purchase/resource';

import {
  getAmapJsConfig,
} from '#/api/sales/product';
import {
  changeSalesProductDesignerDayResourceSupplier,
  deleteSalesProductDesignerDayResource,
  deleteSalesProductDesignerVehicleArrangement,
  getSalesProductDesignerDetail,
  getSalesProductDesignerDayWordPlan,
  getSalesProductDesignerDocuments,
  getSalesProductDesignerResourceDetail,
  getSalesProductDesignerResources,
  getSalesProductDesignerVehicleResources,
  downloadSalesProductDesignerDocument,
  generateSalesProductDesignerAdultQuote,
  generateSalesProductDesignerProductWord,
  previewSalesProductDesignerDocument,
  publishSalesProductDesignerDraft,
  reorderSalesProductDesignerDayResources,
  reorderSalesProductDesignerVehicleArrangements,
  saveSalesProductDesignerAdultQuote,
  saveSalesProductDesignerDayResource,
  saveSalesProductDesignerDayDestination,
  saveSalesProductDesignerVehicleArrangement,
  saveSalesProductDesignerDayWordPlan,
  type SalesProductDesignerApi,
} from '#/api/sales/product-designer';
import ProductResourceMapWorkspace from './components/ProductResourceMapWorkspace.vue';
import ProductDesignerDayArrangementPanel from './components/ProductDesignerDayArrangementPanel.vue';
import ProductDesignerVehicleArrangementPanel from './components/ProductDesignerVehicleArrangementPanel.vue';
import { resolveArrangementTarget } from './components/product-designer-arrangement-utils';
import {
  buildProductResourceMarkerHtml,
  productResourceMarkerZIndex,
} from './components/product-resource-map-marker';
import {
  buildWordPlanImageSavePayload,
  validateWordPlanImageSelections,
  type DayEndImageSelection,
} from './product-designer-word-plan-image-utils';

const route = useRoute();
const router = useRouter();
const productId = computed(() => Number(Array.isArray(route.params.id) ? route.params.id[0] : route.params.id));

const resourceTypeOptions = [
  { label: '全部资源', value: undefined },
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '餐厅', value: 'restaurant' },
  { label: '购物', value: 'shopping' },
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
const starLevelOptions = ref<{ label: string; value: string }[]>([]);
const optionalItemTypeLabel: Record<SalesProductDesignerApi.OptionalItemType, string> = {
  scenic_transport: '景区小交通',
  recommended_self_pay: '推荐自费',
};
const typeLabel: Record<string, string> = Object.fromEntries(resourceTypeOptions.filter((item) => item.value).map((item) => [item.value, item.label]));
const regionOptions = buildRegionOptions();
const provinceOptions = regionOptions.map(({ label, value }) => ({ label, value }));

const detail = ref<SalesProductDesignerApi.Detail>();
const documents = ref<SalesProductDesignerApi.DocumentVersion[]>([]);
const documentHistoryOpen = ref(false);
const documentPreviewOpen = ref(false);
const productWordPreviewUrl = ref('');
const productWordPreviewVersion = ref<SalesProductDesignerApi.DocumentVersion>();
const productWordPreviewLoading = ref(false);
const productWordPreviewError = ref('');
const productWordGenerating = ref(false);
const productWordPreviewDirty = ref(false);
const resources = ref<SalesProductDesignerApi.MapResource[]>([]);
const supplierCandidatesByResourceId = ref<Record<number, SalesProductDesignerApi.Supplier[]>>({});
const selectedResource = ref<SalesProductDesignerApi.ResourceDetail>();
const selectedDayResource = ref<SalesProductDesignerApi.DayResource>();
const openingResourceId = ref<number>();
const loading = ref(false);
const resourceLoading = ref(false);
const addingResourceIds = ref<Set<number>>(new Set());
const detailLoading = ref(false);
const saving = ref(false);
const publishing = ref(false);
const mapLoading = ref(false);
const mapError = ref('');
const mapFullscreenOpen = ref(false);
const selectedMapResourceId = ref<number>();
const highlightedDayResourceId = ref<number>();
const loadError = ref('');
const drawerOpen = ref(false);
const wordPlanOpen = ref(false);
const wordPlanLoading = ref(false);
const wordPlanSaving = ref(false);
const wordPlanEditorMode = ref<'select' | 'sort'>('select');
const wordPlanImageSettingsOpen = ref(false);
const hotelBreakfastSaving = ref(false);
const dayDestinationSaving = ref(false);
const dayDestinationCityInput = ref('');
const hotelSelectionTarget = ref<number>();
const mealSelectionTarget = ref<'breakfast' | 'lunch' | 'dinner'>();
const restaurantMealPickerOpen = ref(false);
const restaurantMealPickerResource = ref<SalesProductDesignerApi.MapResource>();
const restaurantMealPickerRole = ref<'breakfast' | 'lunch' | 'dinner'>();
const vehicleArrangementOpen = ref(false);
const vehicleArrangementSaving = ref(false);
const vehicleResourceLoading = ref(false);
const vehicleResourceKeyword = ref('');
const vehicleResources = ref<SalesProductDesignerApi.VehicleResource[]>([]);
const vehicleSupplierCandidates = ref<SalesProductDesignerApi.Supplier[]>([]);
const vehicleForm = reactive({
  endDayNo: undefined as number | undefined,
  id: undefined as number | undefined,
  quantity: 1,
  remark: '',
  resourceId: undefined as number | undefined,
  startDayNo: undefined as number | undefined,
  supplierRelationId: undefined as number | undefined,
  vehicleType: '',
});
const quoteOpen = ref(false);
const previewOpen = ref(false);
const introPreview = ref<SalesProductDesignerApi.Introduction>();
const activeDayNo = ref(1);
const resourcePage = ref(1);
const resourceTotal = ref(0);
const draggedResource = ref<SalesProductDesignerApi.MapResource>();
const mapContainer = ref<HTMLDivElement>();
let amap: any;
let amapMarkers: any[] = [];
let amapLoader: Promise<any> | undefined;
let mapResizeTimer: number | undefined;
const amapScriptSelector = 'script[data-mtravel-amap="true"]';
const materialSortableContainerRef = ref<HTMLElement>();
const wordPlanSortableContainerRef = ref<HTMLElement>();
let materialSortableInstance: Sortable | undefined;
let wordPlanSortableInstances: Sortable[] = [];
let materialSortableVersion = 0;
let wordPlanSortableVersion = 0;
let productWordPreviewLoadVersion = 0;

const latestProductDocument = computed(() => documents.value.find(
  (item) => item.documentType === 'product_word' && item.generateStatus === 'success',
) || documents.value.find((item) => item.documentType === 'product_word'));
const historicalDocumentCount = computed(() => Math.max(
  0,
  documents.value.length - (latestProductDocument.value ? 1 : 0),
));

const filters = reactive<SalesProductDesignerApi.ResourceQuery>({ page: 1, pageSize: 100 });
let previousProvince = filters.province;
const citySearch = ref('');
type MaterialEditorValue = SalesProductDesignerApi.SelectedMaterialSaveRequest & {
  /** 仅用于工作台内部展示，不提交到统一素材接口。 */
  costPrice?: number;
  /** 用户改过最终对外价后，切换供应商不再覆盖该值。 */
  salePriceDirty: boolean;
  /** 仅用于工作台内部展示，不提交到统一素材接口。 */
  suggestedSalePrice?: number;
};
const editor = reactive({
  remark: '',
  selectedMaterials: [] as MaterialEditorValue[],
  supplierId: undefined as number | undefined,
});
const editorPriceEditingIds = ref<Set<number>>(new Set());
const wordPlan = ref<SalesProductDesignerApi.DayWordPlan>();
const wordPlanSelected = ref<SalesProductDesignerApi.DayWordPlanMaterialSaveRequest[]>([]);
const wordPlanImageMode = ref<'follow_resource' | 'day_end' | 'hidden'>('follow_resource');
const wordPlanImagePickerOpen = ref(false);
const wordPlanImagePickerScope = ref<'resource' | 'day_end'>('resource');
const wordPlanImagePickerResource = ref<SalesProductDesignerApi.DayWordPlanResource>();
const wordPlanImagePickerSelected = ref<number[]>([]);
const wordPlanDayEndImagePickerSelected = ref<DayEndImageSelection[]>([]);
const wordPlanImagePickerLoading = ref(false);
const wordPlanImageUrls = reactive<Record<number, string>>({});
const wordPlanSelectedImageIds = reactive<Record<number, number[]>>({});
const wordPlanDayEndImageSelections = ref<DayEndImageSelection[]>([]);
const WORD_IMAGE_MIN_COUNT = 2;
const WORD_IMAGE_MAX_COUNT = 3;
const savedWordPlanFingerprint = ref('');
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
const activeDayDestination = computed(() => {
  const day = activeDay.value;
  if (!day?.destinationCity) return undefined;
  return {
    destinationCity: day.destinationCity,
    destinationDistrict: day.destinationDistrict,
    destinationProvince: day.destinationProvince,
  };
});
const activeDayDestinationLabel = computed(() => activeDayDestination.value?.destinationCity || '未设置城市');
const activeDayUsesProductCityFallback = computed(() => !activeDayDestination.value?.destinationCity && Boolean(detail.value?.city));
const hasTemporaryMapCity = computed(() => Boolean(
  activeDayDestination.value?.destinationCity && !sameCityName(filters.city, activeDayDestination.value.destinationCity),
));
const activeAccommodations = computed(() => activeDay.value?.resources
  .filter((item) => item.arrangementRole === 'accommodation') || []);
const activeAccommodation = computed(() => activeAccommodations.value[0]);
const activeAccommodationHasCity = computed(() => Boolean(activeAccommodation.value?.city));
const dayDestinationHint = computed(() => {
  if (activeAccommodationHasCity.value) return '已跟随当晚酒店所在地自动设置';
  if (activeDayUsesProductCityFallback.value) return `未安排酒店，地图暂按接团城市 ${detail.value?.city} 显示`;
  return '未安排酒店时可手动填写';
});
const activeMealResources = computed<Record<'breakfast' | 'lunch' | 'dinner', SalesProductDesignerApi.DayResource | undefined>>(() => ({
  breakfast: activeDay.value?.resources.find((item) => item.arrangementRole === 'breakfast'),
  lunch: activeDay.value?.resources.find((item) => item.arrangementRole === 'lunch'),
  dinner: activeDay.value?.resources.find((item) => item.arrangementRole === 'dinner'),
}));
const activeBreakfastPlan = computed(() => {
  const mealPlan = activeDay.value?.mealPlan;
  if (!mealPlan) return undefined;
  return {
    hotelResourceNames: mealPlan.hotelSources.map((item) => item.resourceName),
    source: mealPlan.source,
  };
});
const activeArrangedResourceIds = computed(() => new Set(
  (activeDay.value?.resources || []).map((item) => item.resourceId),
));
const mealRoleLabel: Record<'breakfast' | 'lunch' | 'dinner', string> = {
  breakfast: '早餐',
  dinner: '晚餐',
  lunch: '中餐',
};
const scenicWordPlanSummary = computed(() => {
  const scenic = activeDay.value?.resources.filter((item) => item.resourceType === 'scenic') || [];
  const selectedCount = scenic.reduce((total, item) => total + (item.selectedMaterials?.length || 0), 0);
  const unconfiguredCount = scenic.filter((item) => !(item.selectedMaterials?.length)).length;
  return { selectedCount, unconfiguredCount };
});
const showScenicLevelFilter = computed(() => filters.resourceType === 'scenic');
const showStarLevelFilter = computed(() => filters.resourceType === 'hotel' || filters.resourceType === 'restaurant');
const mapResources = computed(() => resources.value.filter((item): item is SalesProductDesignerApi.MapResource & { latitude: number; longitude: number } => item.longitude != null && item.latitude != null));
const unlocatedResources = computed(() => resources.value.filter((item) => item.longitude == null || item.latitude == null));
const currentCost = computed(() => detail.value?.totalCostAmount || 0);
const standardIntroductions = computed(() => selectedResource.value?.introductions.filter((item) => !item.isOptionalItem) || []);
const selectedSupplierOptionalItems = computed(() => {
  const supplier = selectedResource.value?.suppliers.find((item) => item.supplierId === editor.supplierId);
  return supplier?.optionalItems?.filter((item) => item.status === 'active') || [];
});
/** 候选自费必须来自当前供应商的有效报价，避免把资源主档里无报价的项目误带入产品。 */
const optionalItems = computed(() => {
  const source = selectedResource.value?.optionalItems || [];
  const activeIds = new Set(selectedSupplierOptionalItems.value.map((item) => item.resourceOptionalItemId));
  editor.selectedMaterials
    .filter((item) => item.materialType === 'optional_item' && item.resourceOptionalItemId)
    .forEach((item) => activeIds.add(item.resourceOptionalItemId!));
  return source.filter((item) => item.status === 'active' && activeIds.has(item.id));
});
const selectedMaterialEntries = computed(() => editor.selectedMaterials.map((material, index) => ({
  index,
  introduction: material.introductionId ? introductionById(material.introductionId) : undefined,
  material,
  optionalItem: material.resourceOptionalItemId ? optionalItemById(material.resourceOptionalItemId) : undefined,
})));

function introductionById(introductionId: number) {
  return selectedResource.value?.introductions.find((item) => item.id === introductionId);
}

function optionalItemById(optionalItemId: number) {
  const resourceItem = selectedResource.value?.optionalItems?.find((item) => item.id === optionalItemId);
  if (resourceItem) return resourceItem;
  const supplierItem = selectedSupplierOptionalItems.value.find((item) => item.resourceOptionalItemId === optionalItemId);
  if (!supplierItem) return undefined;
  return {
    id: supplierItem.resourceOptionalItemId,
    optionalItemType: 'recommended_self_pay' as const,
    projectName: supplierItem.projectName,
    status: 'active' as const,
  };
}

function introductionOptionsForOptionalItem(optionalItemId: number) {
  return selectedResource.value?.introductions.filter(
    (item) => item.isOptionalItem && item.resourceOptionalItemId === optionalItemId,
  ) || [];
}

function selectedIntroductionMaterial(introductionId: number) {
  return editor.selectedMaterials.find((item) => item.materialType === 'introduction' && item.introductionId === introductionId);
}

function selectedOptionalItem(optionalItemId: number) {
  return editor.selectedMaterials.find((item) => item.materialType === 'optional_item' && item.resourceOptionalItemId === optionalItemId);
}

function suggestedOptionalSalePrice(optionalItem: SalesProductDesignerApi.ResourceOptionalItem) {
  return selectedSupplierOptionalItems.value.find((item) => item.resourceOptionalItemId === optionalItem.id)?.suggestedSalePrice
    ?? optionalItem.suggestedSalePrice;
}

function optionalReferenceCost(optionalItemId: number) {
  return selectedSupplierOptionalItems.value.find((item) => item.resourceOptionalItemId === optionalItemId)?.costPrice;
}

function toggleOptionalItem(optionalItem: SalesProductDesignerApi.ResourceOptionalItem, checked: boolean) {
  if (!checked) {
    editor.selectedMaterials = editor.selectedMaterials.filter((item) => !(item.materialType === 'optional_item' && item.resourceOptionalItemId === optionalItem.id));
    return;
  }
  if (selectedOptionalItem(optionalItem.id)) return;
  const introduction = introductionOptionsForOptionalItem(optionalItem.id)[0];
  const suggestedSalePrice = suggestedOptionalSalePrice(optionalItem);
  editor.selectedMaterials.push({
    introductionId: introduction?.id,
    materialType: 'optional_item',
    resourceOptionalItemId: optionalItem.id,
    salePrice: suggestedSalePrice,
    salePriceDirty: false,
    suggestedSalePrice,
    supplierOptionalItemId: selectedSupplierOptionalItems.value.find((item) => item.resourceOptionalItemId === optionalItem.id)?.supplierOptionalItemId,
  });
}

function updateOptionalIntroduction(optionalItemId: number, introductionId?: number) {
  const selected = selectedOptionalItem(optionalItemId);
  if (selected) selected.introductionId = introductionId;
}

function toggleIntroduction(introductionId: number, checked: boolean) {
  if (checked) {
    if (!selectedIntroductionMaterial(introductionId)) {
      editor.selectedMaterials.push({ introductionId, materialType: 'introduction', salePriceDirty: false });
    }
    return;
  }
  editor.selectedMaterials = editor.selectedMaterials.filter(
    (item) => !(item.materialType === 'introduction' && item.introductionId === introductionId),
  );
}

function moveMaterial(index: number, offset: number) {
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= editor.selectedMaterials.length) return;
  const nextItems = [...editor.selectedMaterials];
  const [moved] = nextItems.splice(index, 1);
  if (!moved) return;
  nextItems.splice(targetIndex, 0, moved);
  editor.selectedMaterials = nextItems;
}

function removeSelectedMaterial(index: number) {
  editor.selectedMaterials = editor.selectedMaterials.filter((_, currentIndex) => currentIndex !== index);
}

function optionalItemHasCurrentSupplierQuote(optionalItemId?: number) {
  return Boolean(optionalItemId && selectedSupplierOptionalItems.value.some((item) => item.resourceOptionalItemId === optionalItemId));
}

function refreshOptionalSupplierReferences() {
  editor.selectedMaterials.filter((item) => item.materialType === 'optional_item').forEach((item) => {
    if (!item.resourceOptionalItemId) return;
    const supplierItem = selectedSupplierOptionalItems.value.find((candidate) => candidate.resourceOptionalItemId === item.resourceOptionalItemId);
    const resourceItem = selectedResource.value?.optionalItems?.find((candidate) => candidate.id === item.resourceOptionalItemId);
    const nextSuggestedSalePrice = supplierItem?.suggestedSalePrice ?? resourceItem?.suggestedSalePrice;
    if (!item.salePriceDirty) item.salePrice = nextSuggestedSalePrice;
    item.supplierOptionalItemId = supplierItem?.supplierOptionalItemId;
    item.costPrice = supplierItem?.costPrice;
    item.suggestedSalePrice = nextSuggestedSalePrice;
  });
}

function isEditorOptionalPriceEditing(optionalItemId: number) {
  return editorPriceEditingIds.value.has(optionalItemId);
}

function editEditorOptionalPrice(optionalItemId: number) {
  const next = new Set(editorPriceEditingIds.value);
  next.add(optionalItemId);
  editorPriceEditingIds.value = next;
}

function resetEditorOptionalPrice(optionalItemId: number) {
  const selected = selectedOptionalItem(optionalItemId);
  const resourceItem = optionalItemById(optionalItemId);
  if (!selected || !resourceItem) return;
  selected.salePrice = suggestedOptionalSalePrice(resourceItem);
  selected.salePriceDirty = false;
  const next = new Set(editorPriceEditingIds.value);
  next.delete(optionalItemId);
  editorPriceEditingIds.value = next;
}

function formatMoney(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function supplierPriceText(supplier: SalesProductDesignerApi.Supplier) {
  return supplier.priceMode === 'pending' && supplier.referenceUnitPrice === 0
    ? '待询价'
    : formatMoney(supplier.referenceUnitPrice);
}

function formatYuanPerPerson(value?: number) {
  return `${Number(value || 0).toFixed(2)}元/人`;
}

/** 将素材保存的分钟数按 Word 习惯格式化为小时和分钟。 */
function formatVisitDuration(value?: string) {
  const raw = value?.trim() || '';
  const directMinutes = /^\d+$/.test(raw) ? Number(raw) : undefined;
  const hourMatch = raw.match(/(\d+(?:\.\d+)?)\s*小时/);
  const minuteMatch = raw.match(/(\d+)\s*分钟/);
  const legacyMinutes = hourMatch || minuteMatch
    ? Math.round(Number(hourMatch?.[1] || 0) * 60 + Number(minuteMatch?.[1] || 0))
    : undefined;
  const minutes = directMinutes ?? legacyMinutes;
  if (minutes == null || !Number.isInteger(minutes) || minutes <= 0) {
    return value ? `（${value}）` : '';
  }
  const totalMinutes = minutes;
  const hours = Math.floor(totalMinutes / 60);
  const remainder = totalMinutes % 60;
  if (hours === 0) {
    return `（游览约${totalMinutes}分钟）`;
  }
  return remainder === 0
    ? `（游览约${hours}小时）`
    : `（游览约${hours}小时${remainder}分钟）`;
}

function noticeLines(value?: string) {
  return (value || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

/** 同一景点的素材在 Word 里连续成段；资料录入时的换行只保留为阅读间隔。 */
/** 温馨提示只展示素材字段本身，不由预览层自动补标题或改写内容。 */
function warmTipPreviewText(value?: string) {
  return value || '';
}

function areaText(item: { province?: string; city?: string; district?: string }) {
  return [item.province, item.city, item.district].filter(Boolean).join(' / ') || '所在地未维护';
}

function mapResourceAreaText(item: { city?: string; district?: string }) {
  return [item.city, item.district].filter(Boolean).join(' / ') || '城市未维护';
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

/** 将当晚住宿城市同步为默认地图范围；用户仍可用下方筛选临时查看其它城市。 */
function syncMapFiltersToActiveDay() {
  const destination = activeDayDestination.value;
  const province = destination?.destinationProvince || detail.value?.province;
  const city = destination?.destinationCity || detail.value?.city;
  if (filters.province === province && sameCityName(filters.city, city)) return;
  filters.province = province;
  filters.city = city;
  previousProvince = province;
  citySearch.value = '';
}

/** 保存当晚住宿城市；异地已编排资源只保留快照，不会随着地图范围切换被删除。 */
async function persistDayDestination(
  destination: { city?: string; district?: string; province?: string },
  options: { dayNo?: number; showSuccess?: boolean } = {},
) {
  if (!destination.city) return;
  if (!productId.value || dayDestinationSaving.value) return;
  const targetDayNo = options.dayNo || activeDayNo.value;
  dayDestinationSaving.value = true;
  try {
    const saved = await saveSalesProductDesignerDayDestination({
      dayNo: targetDayNo,
      destinationCity: destination.city,
      destinationDistrict: destination.district,
      destinationProvince: destination.province,
      productId: productId.value,
    });
    const day = detail.value?.days.find((item) => item.dayNo === targetDayNo);
    if (day) {
      day.destinationProvince = saved.destinationProvince;
      day.destinationCity = saved.destinationCity;
      day.destinationDistrict = saved.destinationDistrict;
    }
    if (targetDayNo === activeDayNo.value) dayDestinationCityInput.value = saved.destinationCity;
    syncMapFiltersToActiveDay();
    if (options.showSuccess !== false) message.success(`已将 D${targetDayNo} 当晚住宿城市设为${saved.destinationCity}`);
    return true;
  } catch (error) {
    message.error(designerErrorMessage(error, '当晚住宿城市保存失败，请稍后重试'));
    return false;
  } finally {
    dayDestinationSaving.value = false;
  }
}

/** 工作台允许直接输入城市；命中地区库时自动补齐标准城市名和省份。 */
async function saveManualDayDestination() {
  if (activeAccommodationHasCity.value) return;
  const inputCity = dayDestinationCityInput.value.trim();
  if (!inputCity) {
    dayDestinationCityInput.value = activeDayDestination.value?.destinationCity || '';
    message.warning('请输入当晚住宿城市');
    return;
  }
  const matchedProvince = regionOptions.find((province) =>
    province.children?.some((city) => sameCityName(city.value, inputCity)),
  );
  const matchedCity = matchedProvince?.children?.find((city) => sameCityName(city.value, inputCity));
  const destinationCity = matchedCity?.value || inputCity;
  if (
    sameCityName(destinationCity, activeDayDestination.value?.destinationCity)
    && matchedProvince?.value === activeDayDestination.value?.destinationProvince
  ) {
    dayDestinationCityInput.value = activeDayDestination.value?.destinationCity || destinationCity;
    return;
  }
  await persistDayDestination({
    city: destinationCity,
    province: matchedProvince?.value,
  });
}

/** 安排或更换酒店后，住宿城市始终跟随酒店资源主档。 */
async function syncDayDestinationFromHotel(resource: Pick<SalesProductDesignerApi.MapResource, 'city' | 'district' | 'province'>, dayNo: number) {
  if (!resource.city) return false;
  return Boolean(await persistDayDestination({
    city: resource.city,
    district: resource.district,
    province: resource.province,
  }, { dayNo, showSuccess: false }));
}

function onResourceTypeChange() {
  // 等级字段只属于对应资源类型，切换类型时不能把旧筛选条件带到下一次查询。
  filters.scenicLevel = undefined;
  filters.starLevel = undefined;
}

function selectMapResource(resourceId: number) {
  selectedMapResourceId.value = resourceId;
  if (!mapFullscreenOpen.value) {
    void renderMap();
  }
}

/** 点击资源即进入对应编排区；餐厅额外弹出餐次选择，避免系统猜测早中晚。 */
function activateMapResource(resource: SalesProductDesignerApi.MapResource) {
  selectMapResource(resource.id);
  const target = resolveArrangementTarget(resource.resourceType);
  if ('unsupportedInDayMap' in target) {
    message.info(resource.resourceType === 'ground_agent'
      ? '地接服务请在真实团队安排阶段配置'
      : '用车请在产品级全程用车区域安排，不进入当天地图');
    return;
  }
  if ('requiresMealSelection' in target) {
    if (mealSelectionTarget.value) {
      void addResource(resource, activeDayNo.value, mealSelectionTarget.value);
      return;
    }
    restaurantMealPickerResource.value = resource;
    restaurantMealPickerRole.value = undefined;
    restaurantMealPickerOpen.value = true;
    return;
  }
  const alreadyArranged = activeDay.value?.resources.some(
    (item) => item.resourceId === resource.id && item.arrangementRole === target.role,
  );
  if (alreadyArranged && !target.allowRepeat) {
    highlightedDayResourceId.value = activeDay.value?.resources.find(
      (item) => item.resourceId === resource.id && item.arrangementRole === target.role,
    )?.id;
    message.info('该资源已安排到当前区块');
    return;
  }
  void addResource(
    resource,
    activeDayNo.value,
    target.role,
    target.role === 'accommodation' ? activeAccommodation.value : undefined,
  );
}

function confirmRestaurantMeal() {
  const resource = restaurantMealPickerResource.value;
  const role = restaurantMealPickerRole.value;
  if (!resource || !role) {
    message.info('请选择要安排的餐次');
    return;
  }
  mealSelectionTarget.value = role;
  restaurantMealPickerOpen.value = false;
  restaurantMealPickerResource.value = undefined;
  void addResource(resource, activeDayNo.value, role);
}

function openMapFullscreen() {
  mapFullscreenOpen.value = true;
}

function closeMapFullscreen() {
  mapFullscreenOpen.value = false;
  void nextTick().then(() => renderMap());
}

function handleMapFilterChange(
  key: keyof SalesProductDesignerApi.ResourceQuery,
  value: unknown,
) {
  switch (key) {
    case 'keyword':
      filters.keyword = typeof value === 'string' ? value : undefined;
      break;
    case 'resourceType':
      filters.resourceType = value as SalesProductDesignerApi.ResourceType | undefined;
      onResourceTypeChange();
      break;
    case 'scenicLevel':
      filters.scenicLevel = value as SalesProductDesignerApi.ResourceQuery['scenicLevel'];
      break;
    case 'starLevel':
      filters.starLevel = typeof value === 'string' ? value : undefined;
      break;
    case 'province':
      filters.province = typeof value === 'string' ? value : undefined;
      onProvinceChange();
      break;
    case 'city':
      filters.city = typeof value === 'string' ? value : undefined;
      onCityChange(value);
      break;
    default:
      break;
  }
}

function handleMapCitySearch(value: string) {
  onCitySearch(value);
}

function resetResourceFilters() {
  filters.keyword = undefined;
  filters.resourceType = undefined;
  filters.scenicLevel = undefined;
  filters.starLevel = undefined;
  filters.province = undefined;
  filters.city = undefined;
  citySearch.value = '';
  previousProvince = undefined;
}

function handleMapAddResource(resource: SalesProductDesignerApi.MapResource) {
  activateMapResource(resource);
}

function loadMoreMapResources() {
  resourcePage.value += 1;
  void loadResources();
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
    syncMapFiltersToActiveDay();
    syncQuoteForm();
    await nextTick();
    void renderMap();
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

async function loadStarLevelOptions() {
  try {
    const items = await getProductDictionaryAll('reception_standard');
    starLevelOptions.value = items
      .filter((item) => item.status === 'active')
      .sort((left, right) => left.sortOrder - right.sortOrder)
      .map((item) => ({ label: item.dictName, value: item.dictName }));
  } catch {
    starLevelOptions.value = [];
  }
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
      const selected = item.id === selectedMapResourceId.value;
      const arranged = activeArrangedResourceIds.value.has(item.id);
      const marker = new AMap.Marker({
        content: buildProductResourceMarkerHtml(item, {
          arranged,
          pending: item.procurementMode !== 'not_required' && !item.defaultSupplierId,
          selected,
        }),
        offset: new AMap.Pixel(-12, -30),
        position: [item.longitude, item.latitude],
        title: item.resourceName,
        zIndex: productResourceMarkerZIndex({ arranged, selected }),
      });
      marker.on('click', () => activateMapResource(item));
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

function resetResourceEditor() {
  destroyMaterialSortable();
  openingResourceId.value = undefined;
  selectedResource.value = undefined;
  editor.remark = '';
  editor.selectedMaterials = [];
  editor.supplierId = undefined;
  editorPriceEditingIds.value = new Set();
}

function handleResourceDrawerClose() {
  resetResourceEditor();
  selectedDayResource.value = undefined;
}

function hydrateSelectedMaterials(existing?: SalesProductDesignerApi.DayResource): MaterialEditorValue[] {
  if (!existing) return [];
  if (existing.selectedMaterials?.length) {
    return existing.selectedMaterials.map((item) => ({
      introductionId: item.introductionId,
      materialType: item.materialType,
      resourceOptionalItemId: item.resourceOptionalItemId,
      salePrice: item.salePrice,
      salePriceDirty: false,
      supplierOptionalItemId: item.supplierOptionalItemId,
    }));
  }
  const introductions = (existing.selectedIntroductionIds?.length
    ? existing.selectedIntroductionIds
    : existing.selectedIntroductionId ? [existing.selectedIntroductionId] : [])
    .map((introductionId) => ({ introductionId, materialType: 'introduction' as const, salePriceDirty: true }));
  const optionalItems = (existing.selectedOptionalItems || []).map((item) => ({
    costPrice: item.referenceCostPrice,
    introductionId: item.introductionId,
    materialType: 'optional_item' as const,
    resourceOptionalItemId: item.resourceOptionalItemId,
    salePrice: item.salePrice,
    salePriceDirty: true,
    suggestedSalePrice: item.suggestedSalePrice,
    supplierOptionalItemId: item.supplierOptionalItemId,
  }));
  return [...introductions, ...optionalItems];
}

function destroyMaterialSortable() {
  materialSortableVersion += 1;
  materialSortableInstance?.destroy();
  materialSortableInstance = undefined;
}

async function initializeMaterialSortable() {
  destroyMaterialSortable();
  await nextTick();
  const container = materialSortableContainerRef.value;
  if (!container || !drawerOpen.value || editor.selectedMaterials.length < 2) return;
  const version = ++materialSortableVersion;
  const { initializeSortable } = useSortable(container, {
    animation: 160,
    chosenClass: 'material-sort-chosen',
    dragClass: 'material-sort-dragging',
    draggable: '.material-selected-row',
    ghostClass: 'material-sort-ghost',
    handle: '.material-drag-handle',
    onEnd: handleMaterialSortableEnd,
  });
  const sortable = await initializeSortable();
  if (version !== materialSortableVersion || container !== materialSortableContainerRef.value || !drawerOpen.value) {
    sortable.destroy();
    return;
  }
  materialSortableInstance = sortable;
}

function handleMaterialSortableEnd(event: { newIndex?: number; oldIndex?: number }) {
  const { newIndex, oldIndex } = event;
  if (newIndex == null || oldIndex == null || newIndex === oldIndex || newIndex < 0 || oldIndex < 0) return;
  const nextItems = [...editor.selectedMaterials];
  const [moved] = nextItems.splice(oldIndex, 1);
  if (!moved) return;
  nextItems.splice(newIndex, 0, moved);
  editor.selectedMaterials = nextItems;
}

async function openResource(resource: SalesProductDesignerApi.MapResource) {
  resetResourceEditor();
  detailLoading.value = true;
  drawerOpen.value = true;
  openingResourceId.value = resource.id;
  selectedDayResource.value = activeDay.value?.resources.find((item) => item.resourceId === resource.id);
  try {
    const resourceDetail = await getSalesProductDesignerResourceDetail(resource.id);
    // 用户在加载期间切换了另一条资源时，不能让旧请求覆盖当前抽屉。
    if (openingResourceId.value !== resource.id || !drawerOpen.value) return;
    selectedResource.value = resourceDetail;
    const existing = selectedDayResource.value;
    editor.remark = existing?.remark || '';
    editor.supplierId = existing?.supplierId ?? selectedResource.value.defaultSupplierId;
    editor.selectedMaterials = hydrateSelectedMaterials(existing);
    await nextTick();
    productWordPreviewDirty.value = false;
    void initializeMaterialSortable();
  } catch {
    resetResourceEditor();
    drawerOpen.value = false;
  } finally {
    detailLoading.value = false;
  }
}

function wordPlanMaterialKey(item: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  return `${item.dayResourceId}:${item.materialType}:${item.introductionId || item.resourceOptionalItemId || ''}`;
}

function wordPlanFingerprint(items = wordPlanSelected.value) {
  return JSON.stringify({
    items: items.map((item) => ({
    dayResourceId: item.dayResourceId,
    introductionId: item.introductionId,
    materialType: item.materialType,
    resourceOptionalItemId: item.resourceOptionalItemId,
    })),
    imageMode: wordPlanImageMode.value,
    images: Object.entries(wordPlanSelectedImageIds)
      .sort(([left], [right]) => Number(left) - Number(right)),
    dayEndImages: wordPlanDayEndImageSelections.value,
  });
}

function wordPlanResource(dayResourceId: number) {
  return wordPlan.value?.resources.find((item) => item.dayResource.id === dayResourceId);
}

function wordPlanIntroduction(item: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  return wordPlanResource(item.dayResourceId)?.resourceDetail.introductions
    .find((introduction) => introduction.id === item.introductionId);
}

function wordPlanOptionalItem(item: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  return wordPlanResource(item.dayResourceId)?.resourceDetail.optionalItems
    ?.find((optionalItem) => optionalItem.id === item.resourceOptionalItemId);
}

function wordPlanSupplierOptionalItems(resource: SalesProductDesignerApi.DayWordPlanResource) {
  return resource.resourceDetail.suppliers.find(
    (supplier) => supplier.supplierId === resource.dayResource.supplierId,
  )?.optionalItems?.filter((item) => item.status === 'active') || [];
}

/** 只提交当前已选供应商名下的报价，旧供应商遗留的报价 ID 不能再次写回。 */
function wordPlanCurrentSupplierOptionalItem(item: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  if (item.materialType !== 'optional_item' || !item.resourceOptionalItemId) return;
  const resource = wordPlanResource(item.dayResourceId);
  return resource && wordPlanSupplierOptionalItems(resource)
    .find((candidate) => candidate.resourceOptionalItemId === item.resourceOptionalItemId);
}

function wordPlanSelectedImages(resource: SalesProductDesignerApi.DayWordPlanResource) {
  return wordPlanSelectedImageIds[resource.dayResource.id] || [];
}

function wordPlanImageSelectionState(resource: SalesProductDesignerApi.DayWordPlanResource) {
  const count = wordPlanSelectedImages(resource).length;
  if (count === 0) return 'empty';
  if (count < WORD_IMAGE_MIN_COUNT || count > WORD_IMAGE_MAX_COUNT) return 'invalid';
  return 'valid';
}

function wordPlanImageTagText(resource: SalesProductDesignerApi.DayWordPlanResource) {
  const count = wordPlanSelectedImages(resource).length;
  return count > 0 && (count < WORD_IMAGE_MIN_COUNT || count > WORD_IMAGE_MAX_COUNT)
    ? '图片需选 2-3 张'
    : `图片 ${count} 张`;
}

function wordPlanSelectedCountForResource(resource: SalesProductDesignerApi.DayWordPlanResource) {
  return wordPlanSelected.value.filter((item) => item.dayResourceId === resource.dayResource.id).length;
}

function wordPlanIntroductionOptions(resource: SalesProductDesignerApi.DayWordPlanResource) {
  return resource.resourceDetail.introductions.filter((item) => !item.isOptionalItem);
}

function formatWordPlanTags(tags?: string) {
  if (!tags?.trim()) return '';
  try {
    const parsed = JSON.parse(tags);
    return Array.isArray(parsed) ? parsed.filter(Boolean).join('、') : String(parsed || '');
  } catch {
    return tags;
  }
}

const wordPlanImageConfigurationInvalid = computed(() => {
  if (!wordPlan.value || wordPlanImageMode.value === 'hidden') return false;
  if (wordPlanImageMode.value === 'day_end') return wordPlanDayEndImageSelectionState() !== 'valid';
  return wordPlan.value.resources.some((resource) => wordPlanImageSelectionState(resource) === 'invalid');
});

const wordPlanImageSummary = computed(() => {
  if (wordPlanImageMode.value === 'hidden') return '不展示图片';
  if (wordPlanImageMode.value === 'day_end') {
    const count = wordPlanDayEndImageSelections.value.length;
    if (count === 1 || count > WORD_IMAGE_MAX_COUNT) return `当天末尾 · 需调整（${count} 张）`;
    return count ? `当天末尾 · ${count} 张` : '当天末尾 · 未选图片';
  }
  const count = Object.values(wordPlanSelectedImageIds).reduce((sum, imageIds) => sum + imageIds.length, 0);
  return count ? `跟随景区 · ${count} 张` : '跟随景区 · 未选图片';
});

function wordPlanDayEndImageSelectionState() {
  const { valid, code } = validateWordPlanImageSelections({
    dayEndImageSelections: wordPlanDayEndImageSelections.value,
    imageMode: 'day_end',
  });
  return valid ? 'valid' : code === 'day_end_min_2' ? 'min' : 'max';
}

function wordPlanDayEndImageTagText() {
  const count = wordPlanDayEndImageSelections.value.length;
  const state = wordPlanDayEndImageSelectionState();
  if (state === 'min') return '需选 2 或 3 张';
  if (state === 'max') return '最多 3 张';
  return `已选 ${count} / ${WORD_IMAGE_MAX_COUNT} 张`;
}

function validateWordPlanImages() {
  if (!wordPlan.value) return false;
  if (wordPlanImageMode.value === 'hidden') return true;
  if (wordPlanImageMode.value === 'day_end') {
    const result = validateWordPlanImageSelections({
      dayEndImageSelections: wordPlanDayEndImageSelections.value,
      imageMode: 'day_end',
    });
    if (result.valid) return true;
    message.error(result.code === 'day_end_min_2'
      ? '当天末尾已选 1 张图片，请补充至 2 或 3 张，或清空图片后保存'
      : '当天末尾最多只能选择 3 张图片');
    return false;
  }
  const invalidResource = wordPlan.value.resources.find(
    (resource) => wordPlanImageSelectionState(resource) === 'invalid',
  );
  if (!invalidResource) return true;
  message.error(`${invalidResource.dayResource.resourceName} 已选 1 张图片，请补充至 2 或 3 张，或清空图片后保存`);
  return false;
}

type WordPlanImagePickerItem = {
  image: SalesProductDesignerApi.ResourceImage;
  key: string;
  resource: SalesProductDesignerApi.DayWordPlanResource;
};

function wordPlanImagePickerSelectedItems(): WordPlanImagePickerItem[] {
  if (wordPlanImagePickerScope.value === 'day_end') {
    return wordPlanDayEndImagePickerSelected.value.flatMap((selection) => {
      const resource = wordPlanResource(selection.dayResourceId);
      const image = resource?.resourceDetail.images.find((candidate) => candidate.id === selection.imageId);
      return resource && image ? [{ image, key: `${selection.dayResourceId}:${selection.imageId}`, resource }] : [];
    });
  }
  const resource = wordPlanImagePickerResource.value;
  if (!resource) return [];
  const byId = new Map(resource.resourceDetail.images.map((image) => [image.id, image]));
  return wordPlanImagePickerSelected.value.flatMap((id) => {
    const image = byId.get(id);
    return image ? [{ image, key: `${resource.dayResource.id}:${image.id}`, resource }] : [];
  });
}

function wordPlanImagePickerSelectionCount() {
  return wordPlanImagePickerScope.value === 'day_end'
    ? wordPlanDayEndImagePickerSelected.value.length
    : wordPlanImagePickerSelected.value.length;
}

function isWordPlanImagePickerSelected(resource: SalesProductDesignerApi.DayWordPlanResource, imageId: number) {
  return wordPlanImagePickerScope.value === 'day_end'
    ? wordPlanDayEndImagePickerSelected.value.some(
      (selection) => selection.dayResourceId === resource.dayResource.id && selection.imageId === imageId,
    )
    : wordPlanImagePickerSelected.value.includes(imageId);
}

function wordPlanImageUrl(image: SalesProductDesignerApi.ResourceImage) {
  return wordPlanImageUrls[image.id];
}

function toggleWordPlanImage(
  resource: SalesProductDesignerApi.DayWordPlanResource,
  imageId: number,
  checked: boolean,
) {
  if (wordPlanImagePickerScope.value === 'day_end') {
    const current = [...wordPlanDayEndImagePickerSelected.value];
    const selected = current.some(
      (selection) => selection.dayResourceId === resource.dayResource.id && selection.imageId === imageId,
    );
    if (checked && !selected && current.length >= WORD_IMAGE_MAX_COUNT) {
      message.warning('当天末尾最多选择 3 张图片');
      return;
    }
    const next = checked
      ? [...current, { dayResourceId: resource.dayResource.id, imageId }]
      : current.filter((selection) => selection.dayResourceId !== resource.dayResource.id || selection.imageId !== imageId);
    wordPlanDayEndImagePickerSelected.value = next;
    wordPlanDayEndImageSelections.value = next;
    return;
  }
  const current = [...wordPlanImagePickerSelected.value];
  if (checked && !current.includes(imageId) && current.length >= WORD_IMAGE_MAX_COUNT) {
    message.warning('每个景区最多选择 3 张图片');
    return;
  }
  const next = checked
    ? [...current, imageId].filter((id, index, ids) => ids.indexOf(id) === index)
    : current.filter((id) => id !== imageId);
  wordPlanImagePickerSelected.value = next;
  const resourceId = wordPlanImagePickerResource.value?.dayResource.id;
  if (resourceId != null) wordPlanSelectedImageIds[resourceId] = next;
}

function moveWordPlanImage(from: number, to: number) {
  if (wordPlanImagePickerScope.value === 'day_end') {
    const next = [...wordPlanDayEndImagePickerSelected.value];
    if (from < 0 || to < 0 || from >= next.length || to >= next.length || from === to) return;
    const [moved] = next.splice(from, 1);
    if (!moved) return;
    next.splice(to, 0, moved);
    wordPlanDayEndImagePickerSelected.value = next;
    wordPlanDayEndImageSelections.value = next;
    return;
  }
  const next = [...wordPlanImagePickerSelected.value];
  if (from < 0 || to < 0 || from >= next.length || to >= next.length || from === to) return;
  const [moved] = next.splice(from, 1);
  if (moved == null) return;
  next.splice(to, 0, moved);
  wordPlanImagePickerSelected.value = next;
  const resourceId = wordPlanImagePickerResource.value?.dayResource.id;
  if (resourceId != null) wordPlanSelectedImageIds[resourceId] = next;
}

function openWordPlanImagePicker(resource: SalesProductDesignerApi.DayWordPlanResource) {
  wordPlanImagePickerScope.value = 'resource';
  wordPlanImagePickerResource.value = resource;
  wordPlanImagePickerSelected.value = [...wordPlanSelectedImages(resource)];
  openWordPlanImagePickerModal([resource]);
}

function openDayEndWordPlanImagePicker() {
  if (!wordPlan.value) return;
  wordPlanImagePickerScope.value = 'day_end';
  wordPlanImagePickerResource.value = undefined;
  wordPlanDayEndImagePickerSelected.value = [...wordPlanDayEndImageSelections.value];
  openWordPlanImagePickerModal(wordPlan.value.resources);
}

function openWordPlanImagePickerModal(resourcesToLoad: SalesProductDesignerApi.DayWordPlanResource[]) {
  wordPlanImagePickerOpen.value = true;
  wordPlanImagePickerLoading.value = true;
  void Promise.all(resourcesToLoad.flatMap((resource) => resource.resourceDetail.images.map(async (image) => {
    if (wordPlanImageUrls[image.id]) return;
    try {
      const blob = await downloadPurchaseResourceImage(resource.dayResource.resourceId, image.id);
      wordPlanImageUrls[image.id] = URL.createObjectURL(blob);
    } catch {
      // 图片不可预览时仍保留文件名和排序能力，Word 输出不受影响。
    }
  }))).finally(() => {
    wordPlanImagePickerLoading.value = false;
  });
}

function closeWordPlanImagePicker() {
  wordPlanImagePickerOpen.value = false;
  wordPlanImagePickerScope.value = 'resource';
  wordPlanImagePickerResource.value = undefined;
  wordPlanImagePickerSelected.value = [];
  wordPlanDayEndImagePickerSelected.value = [];
}

function clearWordPlanImages() {
  if (wordPlanImagePickerScope.value === 'day_end') {
    wordPlanDayEndImagePickerSelected.value = [];
    wordPlanDayEndImageSelections.value = [];
    return;
  }
  const resourceId = wordPlanImagePickerResource.value?.dayResource.id;
  if (resourceId == null) return;
  wordPlanImagePickerSelected.value = [];
  wordPlanSelectedImageIds[resourceId] = [];
}

function initializeWordPlanImages(data: SalesProductDesignerApi.DayWordPlan) {
  Object.keys(wordPlanSelectedImageIds).forEach((key) => delete wordPlanSelectedImageIds[Number(key)]);
  data.resources.forEach((resource) => {
    wordPlanSelectedImageIds[resource.dayResource.id] = [...(resource.dayResource.selectedImageIds || [])];
  });
  const responseSelections = (data.dayEndImageSelections || []) as DayEndImageSelection[];
  wordPlanDayEndImageSelections.value = responseSelections.length
    ? responseSelections.map((selection) => ({
      dayResourceId: selection.dayResourceId,
      imageId: selection.imageId,
    }))
    : data.resources.flatMap((resource) => (resource.dayResource.selectedImageIds || []).map((imageId) => ({
      dayResourceId: resource.dayResource.id,
      imageId,
    })));
}

/** Word 素材以当天行程资源 ID 分组，避免两条同名景区被误合并。 */
function wordPlanScenicGroupKey(item: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  return String(item.dayResourceId);
}

const wordPlanSelectedGroups = computed(() => {
  const groups: {
    dayResourceId: number;
    items: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest[];
    key: string;
    resourceName: string;
  }[] = [];
  wordPlanSelected.value.forEach((item) => {
    const key = wordPlanScenicGroupKey(item);
    const current = groups.at(-1);
    if (current?.key === key) {
      current.items.push(item);
      return;
    }
    groups.push({
      dayResourceId: item.dayResourceId,
      items: [item],
      key,
      resourceName: wordPlanResource(item.dayResourceId)?.dayResource.resourceName || '未命名景区',
    });
  });
  return groups;
});

function wordPlanScenicGroupRange(index: number) {
  const item = wordPlanSelected.value[index];
  if (!item) return;
  const groupKey = wordPlanScenicGroupKey(item);
  let start = index;
  let end = index;
  while (start > 0 && wordPlanScenicGroupKey(wordPlanSelected.value[start - 1]!) === groupKey) start -= 1;
  while (end < wordPlanSelected.value.length - 1 && wordPlanScenicGroupKey(wordPlanSelected.value[end + 1]!) === groupKey) end += 1;
  return { end, start };
}

/** 新勾选的素材自动放到同景区组的末尾，避免用户无法跨组拖动后出现散组。 */
function addWordPlanMaterial(candidate: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  const groupKey = wordPlanScenicGroupKey(candidate);
  const next = [...wordPlanSelected.value];
  const lastSameGroupIndex = next.reduce(
    (lastIndex, item, index) => wordPlanScenicGroupKey(item) === groupKey ? index : lastIndex,
    -1,
  );
  if (lastSameGroupIndex >= 0) next.splice(lastSameGroupIndex + 1, 0, candidate);
  else next.push(candidate);
  wordPlanSelected.value = next;
}

function isWordPlanSelected(item: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  const key = wordPlanMaterialKey(item);
  return wordPlanSelected.value.some((selected) => wordPlanMaterialKey(selected) === key);
}

function toggleWordPlanIntroduction(resource: SalesProductDesignerApi.DayWordPlanResource, introduction: SalesProductDesignerApi.Introduction) {
  const candidate: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest = {
    dayResourceId: resource.dayResource.id,
    introductionId: introduction.id,
    materialType: 'introduction',
  };
  const key = wordPlanMaterialKey(candidate);
  const existingIndex = wordPlanSelected.value.findIndex((item) => wordPlanMaterialKey(item) === key);
  if (existingIndex >= 0) wordPlanSelected.value = wordPlanSelected.value.filter((_, index) => index !== existingIndex);
  else addWordPlanMaterial(candidate);
}

function toggleWordPlanOptionalItem(
  resource: SalesProductDesignerApi.DayWordPlanResource,
  optionalItem: SalesProductDesignerApi.ResourceOptionalItem,
) {
  const supplierItem = wordPlanSupplierOptionalItems(resource)
    .find((item) => item.resourceOptionalItemId === optionalItem.id);
  if (!supplierItem) {
    message.warning('请先在“供应商配置”中选择已报价的供应商');
    return;
  }
  const candidate: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest = {
    dayResourceId: resource.dayResource.id,
    introductionId: resource.resourceDetail.introductions.find(
      (item) => item.isOptionalItem && item.resourceOptionalItemId === optionalItem.id,
    )?.id,
    materialType: 'optional_item',
    resourceOptionalItemId: optionalItem.id,
    salePrice: supplierItem.suggestedSalePrice,
    supplierOptionalItemId: supplierItem.supplierOptionalItemId,
  };
  const key = wordPlanMaterialKey(candidate);
  const existingIndex = wordPlanSelected.value.findIndex((item) => wordPlanMaterialKey(item) === key);
  if (existingIndex >= 0) wordPlanSelected.value = wordPlanSelected.value.filter((_, index) => index !== existingIndex);
  else addWordPlanMaterial(candidate);
}

function toggleWordPlanSupplierOptionalItem(
  resource: SalesProductDesignerApi.DayWordPlanResource,
  supplierItem: SalesProductDesignerApi.SupplierOptionalItem,
) {
  const optionalItem = resource.resourceDetail.optionalItems
    ?.find((item) => item.id === supplierItem.resourceOptionalItemId);
  if (!optionalItem) {
    message.warning('该自费项目已停用或不存在，请刷新后重试');
    return;
  }
  toggleWordPlanOptionalItem(resource, optionalItem);
}

function moveWordPlanGroup(from: number, to: number) {
  if (from === to || from < 0 || to < 0) return;
  const groups = wordPlanSelectedGroups.value.map((group) => [...group.items]);
  const [moved] = groups.splice(from, 1);
  if (!moved) return;
  groups.splice(to, 0, moved);
  wordPlanSelected.value = groups.flat();
}

function moveWordPlanMaterialWithinGroup(groupKey: string, from: number, to: number) {
  if (from === to || from < 0 || to < 0) return;
  const group = wordPlanSelectedGroups.value.find((item) => item.key === groupKey);
  if (!group) return;
  const range = wordPlanScenicGroupRange(
    wordPlanSelected.value.findIndex((item) => wordPlanScenicGroupKey(item) === groupKey),
  );
  if (!range) return;
  const next = [...wordPlanSelected.value];
  const [moved] = next.splice(range.start + from, 1);
  if (!moved) return;
  next.splice(range.start + to, 0, moved);
  wordPlanSelected.value = next;
}

function wordPlanSuggestedSalePrice(item: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  if (item.materialType !== 'optional_item') return undefined;
  const resource = wordPlanResource(item.dayResourceId);
  if (!resource) return undefined;
  return wordPlanSupplierOptionalItems(resource)
    .find((candidate) => candidate.resourceOptionalItemId === item.resourceOptionalItemId)?.suggestedSalePrice;
}

function wordPlanDisplaySalePrice(item: SalesProductDesignerApi.DayWordPlanMaterialSaveRequest) {
  const value = wordPlanSuggestedSalePrice(item);
  return value == null ? '未配置默认价' : formatYuanPerPerson(value);
}

function destroyWordPlanSortable() {
  wordPlanSortableVersion += 1;
  wordPlanSortableInstances.forEach((instance) => instance.destroy());
  wordPlanSortableInstances = [];
}

async function initializeWordPlanSortable() {
  destroyWordPlanSortable();
  await nextTick();
  const container = wordPlanSortableContainerRef.value;
  if (!container || !wordPlanOpen.value || wordPlanEditorMode.value !== 'sort' || !wordPlanSelected.value.length) return;
  const version = ++wordPlanSortableVersion;
  const initialized: Sortable[] = [];

  if (wordPlanSelectedGroups.value.length > 1) {
    const { initializeSortable } = useSortable(container, {
      animation: 160,
      chosenClass: 'material-sort-chosen',
      dragClass: 'material-sort-dragging',
      draggable: '.word-plan-selected-group',
      ghostClass: 'material-sort-ghost',
      handle: '.word-plan-group-drag-handle',
      onEnd: (event: { newIndex?: number; oldIndex?: number }) => {
        if (event.newIndex == null || event.oldIndex == null) return;
        moveWordPlanGroup(event.oldIndex, event.newIndex);
      },
    });
    initialized.push(await initializeSortable());
  }

  const groupLists = Array.from(container.querySelectorAll<HTMLElement>('.word-plan-selected-group-list'));
  for (const groupList of groupLists) {
    if (groupList.querySelectorAll('.word-plan-selected-row').length < 2) continue;
    const groupKey = groupList.dataset.scenicGroup;
    if (!groupKey) continue;
    const { initializeSortable } = useSortable(groupList, {
      animation: 160,
      chosenClass: 'material-sort-chosen',
      dragClass: 'material-sort-dragging',
      draggable: '.word-plan-selected-row',
      ghostClass: 'material-sort-ghost',
      handle: '.material-drag-handle',
      onEnd: (event: { newIndex?: number; oldIndex?: number }) => {
        if (event.newIndex == null || event.oldIndex == null) return;
        moveWordPlanMaterialWithinGroup(groupKey, event.oldIndex, event.newIndex);
      },
    });
    initialized.push(await initializeSortable());
  }

  if (version !== wordPlanSortableVersion || container !== wordPlanSortableContainerRef.value || !wordPlanOpen.value) {
    initialized.forEach((instance) => instance.destroy());
    return;
  }
  wordPlanSortableInstances = initialized;
}

async function openDayWordPlan() {
  wordPlanOpen.value = true;
  wordPlanLoading.value = true;
  wordPlanEditorMode.value = 'select';
  wordPlanImageSettingsOpen.value = false;
  try {
    const data = await getSalesProductDesignerDayWordPlan(productId.value, activeDayNo.value);
    wordPlan.value = data;
    wordPlanImageMode.value = data.imageMode || 'follow_resource';
    initializeWordPlanImages(data);
    wordPlanSelected.value = data.selectedMaterials.map((item) => ({
      dayResourceId: item.dayResourceId,
      introductionId: item.material.introductionId,
      materialType: item.material.materialType,
      resourceOptionalItemId: item.material.resourceOptionalItemId,
      salePrice: item.material.salePrice,
      supplierOptionalItemId: item.material.supplierOptionalItemId,
    }));
    savedWordPlanFingerprint.value = wordPlanFingerprint();
    await nextTick();
    productWordPreviewDirty.value = false;
    void initializeWordPlanSortable();
  } catch (error) {
    wordPlanOpen.value = false;
    message.error(designerErrorMessage(error, 'Word 方案加载失败'));
  } finally {
    wordPlanLoading.value = false;
  }
}

function closeDayWordPlan() {
  destroyWordPlanSortable();
  wordPlanImageSettingsOpen.value = false;
  wordPlanOpen.value = false;
  wordPlan.value = undefined;
  wordPlanSelected.value = [];
  wordPlanImageMode.value = 'follow_resource';
  Object.keys(wordPlanSelectedImageIds).forEach((key) => delete wordPlanSelectedImageIds[Number(key)]);
  wordPlanDayEndImageSelections.value = [];
  wordPlanDayEndImagePickerSelected.value = [];
  wordPlanEditorMode.value = 'select';
}

function requestCloseDayWordPlan() {
  if (wordPlanFingerprint() === savedWordPlanFingerprint.value) {
    closeDayWordPlan();
    return;
  }
  Modal.confirm({
    cancelText: '继续编辑',
    content: '当前内容编排或图片设置尚未保存，关闭后将丢失这些调整。',
    okText: '放弃修改',
    okType: 'danger',
    title: '确认关闭 Word 方案？',
    onOk: closeDayWordPlan,
  });
}

async function saveDayWordPlan() {
  if (!wordPlan.value) return;
  if (!validateWordPlanImages()) return;
  wordPlanSaving.value = true;
  try {
    const imagePayload = buildWordPlanImageSavePayload({
      dayEndImageSelections: wordPlanDayEndImageSelections.value,
      imageMode: wordPlanImageMode.value,
      selectedImageIdsByResource: Object.fromEntries(
        wordPlan.value.resources.map((resource) => [
          resource.dayResource.id,
          wordPlanSelectedImageIds[resource.dayResource.id] || [],
        ]),
      ),
    });
    const saved = await saveSalesProductDesignerDayWordPlan({
      dayNo: wordPlan.value.dayNo,
      dayResourceIds: wordPlan.value.resources.map((item) => item.dayResource.id),
      productId: wordPlan.value.productId,
      ...imagePayload,
      selectedMaterials: wordPlanSelected.value.map((item) => ({
        ...item,
        supplierOptionalItemId: wordPlanCurrentSupplierOptionalItem(item)?.supplierOptionalItemId,
        salePrice: item.materialType === 'optional_item' ? undefined : item.salePrice,
      })),
    });
    wordPlan.value = saved;
    wordPlanImageMode.value = saved.imageMode || wordPlanImageMode.value;
    initializeWordPlanImages(saved);
    wordPlanSelected.value = saved.selectedMaterials.map((item) => ({
      dayResourceId: item.dayResourceId,
      introductionId: item.material.introductionId,
      materialType: item.material.materialType,
      resourceOptionalItemId: item.material.resourceOptionalItemId,
      salePrice: item.material.salePrice,
      supplierOptionalItemId: item.material.supplierOptionalItemId,
    }));
    savedWordPlanFingerprint.value = wordPlanFingerprint();
    await loadDetail();
    const version = await generateProductWordPreviewVersion();
    if (version?.generateStatus === 'success') message.success('当天 Word 方案已保存，预览已刷新');
    else message.warning('当天 Word 方案已保存，预览生成失败，可稍后重试');
  } catch (error) {
    message.error(designerErrorMessage(error, 'Word 方案保存失败，请稍后重试'));
  } finally {
    wordPlanSaving.value = false;
  }
}

async function addSelectedResource(dayNo = activeDayNo.value) {
  if (!selectedResource.value) return;
  const arrangementRole = selectedResource.value.resourceType === 'hotel'
    ? 'accommodation'
    : selectedResource.value.resourceType === 'restaurant'
      ? mealSelectionTarget.value
      : 'itinerary';
  if (!arrangementRole) {
    message.info('请先在右侧选择要安排的早餐、中餐或晚餐');
    return;
  }
  const replacing = arrangementRole === 'breakfast' || arrangementRole === 'lunch' || arrangementRole === 'dinner'
      ? activeMealResources.value[arrangementRole]
      : undefined;
  saving.value = true;
  try {
    await saveSalesProductDesignerDayResource({
      arrangementRole,
      dayNo,
      hotelBreakfastIncluded: replacing?.hotelBreakfastIncluded,
      id: selectedDayResource.value?.id || replacing?.id,
      productId: productId.value,
      remark: editor.remark,
      resourceId: selectedResource.value.id,
      selectedMaterials: editor.selectedMaterials.map((item) => ({
        introductionId: item.introductionId,
        materialType: item.materialType,
        resourceOptionalItemId: item.resourceOptionalItemId,
        salePrice: item.materialType === 'optional_item' && item.salePriceDirty ? item.salePrice : undefined,
        supplierOptionalItemId: item.supplierOptionalItemId,
      })),
      supplierId: editor.supplierId,
    });
    const citySynced = arrangementRole === 'accommodation'
      ? await syncDayDestinationFromHotel(selectedResource.value, dayNo)
      : false;
    message.success(selectedDayResource.value
      ? `资源编排已更新${citySynced ? `，住宿城市已同步为${selectedResource.value.city}` : ''}`
      : `已安排${resourceArrangementLabel(selectedResource.value.resourceType, arrangementRole)}${citySynced ? `，住宿城市已同步为${selectedResource.value.city}` : ''}`);
    if (arrangementRole === 'accommodation' && !selectedResource.value.city) {
      message.warning('该酒店未维护所在城市，请手动填写住宿城市');
    }
    drawerOpen.value = false;
    clearArrangementSelection();
    await loadDetail();
    await generateProductWordPreviewVersion();
  } finally {
    saving.value = false;
  }
}

function resourceArrangementLabel(
  resourceType: SalesProductDesignerApi.ResourceType,
  arrangementRole?: SalesProductDesignerApi.ArrangementRole,
) {
  if (resourceType === 'hotel' || arrangementRole === 'accommodation') return '当晚住宿';
  if (resourceType === 'restaurant' && arrangementRole && arrangementRole !== 'unassigned') {
    return mealRoleLabel[arrangementRole as 'breakfast' | 'lunch' | 'dinner'];
  }
  return `第 ${activeDayNo.value} 天行程`;
}

function resourceActionLabel(resource: SalesProductDesignerApi.MapResource) {
  if (resource.resourceType === 'hotel') return activeAccommodation.value ? '更换住宿' : '安排住宿';
  if (resource.resourceType === 'restaurant') {
    return mealSelectionTarget.value ? `安排${mealRoleLabel[mealSelectionTarget.value]}` : '安排用餐';
  }
  return `加入 D${activeDayNo.value}`;
}

function clearArrangementSelection() {
  hotelSelectionTarget.value = undefined;
  mealSelectionTarget.value = undefined;
}

async function startHotelSelection() {
  hotelSelectionTarget.value = activeDayNo.value;
  mealSelectionTarget.value = undefined;
  filters.resourceType = 'hotel';
  await loadResources(true);
}

async function startMealSelection(role: 'breakfast' | 'lunch' | 'dinner') {
  hotelSelectionTarget.value = undefined;
  mealSelectionTarget.value = role;
  filters.resourceType = 'restaurant';
  await loadResources(true);
}

async function startItinerarySelection() {
  clearArrangementSelection();
  filters.resourceType = undefined;
  await loadResources(true);
}

async function addResourceForArrangement(resource: SalesProductDesignerApi.MapResource) {
  const target = resolveArrangementTarget(resource.resourceType);
  if ('unsupportedInDayMap' in target) {
    message.info(resource.resourceType === 'ground_agent'
      ? '地接服务请在真实团队安排阶段配置'
      : '用车请在产品级全程用车区域安排');
    return;
  }
  if ('requiresMealSelection' in target) {
    if (!mealSelectionTarget.value) {
      restaurantMealPickerResource.value = resource;
      restaurantMealPickerRole.value = undefined;
      restaurantMealPickerOpen.value = true;
      return;
    }
    await addResource(resource, activeDayNo.value, mealSelectionTarget.value);
    return;
  }
  await addResource(
    resource,
    hotelSelectionTarget.value || activeDayNo.value,
    target.role,
    target.role === 'accommodation' ? activeAccommodation.value : undefined,
  );
}

async function addResource(
  resource: SalesProductDesignerApi.MapResource,
  dayNo = activeDayNo.value,
  arrangementRole: SalesProductDesignerApi.ArrangementRole = 'itinerary',
  replacing?: SalesProductDesignerApi.DayResource,
  replaceBreakfastSource = false,
) {
  if (addingResourceIds.value.has(resource.id)) return;
  const resolvedReplacing = replacing || (
    arrangementRole === 'breakfast' || arrangementRole === 'lunch' || arrangementRole === 'dinner'
      ? activeMealResources.value[arrangementRole]
      : undefined
  );
  if (arrangementRole === 'breakfast' && activeBreakfastPlan.value?.source === 'hotel' && !replaceBreakfastSource) {
    Modal.confirm({
      content: '将改为外部早餐，并取消前一晚酒店的含次日早餐。是否继续？',
      okText: '确认替换',
      onOk: () => addResource(resource, dayNo, arrangementRole, resolvedReplacing, true),
      title: '确认替换早餐来源',
    });
    return;
  }
  addingResourceIds.value = new Set(addingResourceIds.value).add(resource.id);
  try {
    await saveSalesProductDesignerDayResource({
      arrangementRole,
      dayNo,
      hotelBreakfastIncluded: resolvedReplacing?.hotelBreakfastIncluded,
      id: resolvedReplacing?.id,
      productId: productId.value,
      replaceBreakfastSource,
      resourceId: resource.id,
      selectedMaterials: [],
      supplierId: resource.defaultSupplierId,
    });
    const citySynced = arrangementRole === 'accommodation'
      ? await syncDayDestinationFromHotel(resource, dayNo)
      : false;
    message.success(`已${resolvedReplacing ? '更换' : '安排'}${resourceArrangementLabel(resource.resourceType, arrangementRole)}${citySynced ? `，住宿城市已同步为${resource.city}` : ''}`);
    if (arrangementRole === 'accommodation' && !resource.city) {
      message.warning('该酒店未维护所在城市，请手动填写住宿城市');
    }
    clearArrangementSelection();
    await loadDetail();
    highlightedDayResourceId.value = undefined;
    await generateProductWordPreviewVersion();
  } catch {
    // 请求层已统一展示接口错误；这里不再重复弹出同一条提示。
  } finally {
    const nextIds = new Set(addingResourceIds.value);
    nextIds.delete(resource.id);
    addingResourceIds.value = nextIds;
  }
}

async function deletePlan(item: SalesProductDesignerApi.DayResource) {
  await deleteSalesProductDesignerDayResource({ id: item.id, productId: productId.value });
  message.success('已从产品中移除');
  await loadDetail();
  await generateProductWordPreviewVersion();
}

async function changeDayResourceSupplier(row: SalesProductDesignerApi.DayResource, supplierRelationId: number) {
  await changeSalesProductDesignerDayResourceSupplier({
    dayResourceId: row.id,
    productId: productId.value,
    supplierRelationId,
  });
  message.success(`已更新“${row.resourceName}”的供应商和成本快照`);
  await loadDetail();
}

/** 只在用户点开“更换供应商”时读取该资源候选，避免打开产品页就批量请求。 */
async function loadDayResourceSupplierCandidates(resourceId: number) {
  if (supplierCandidatesByResourceId.value[resourceId]) return;
  const resource = await getSalesProductDesignerResourceDetail(resourceId);
  supplierCandidatesByResourceId.value = {
    ...supplierCandidatesByResourceId.value,
    [resourceId]: resource.suppliers,
  };
}

async function reorderDayArrangement(
  arrangementRole: 'accommodation' | 'ground_service' | 'itinerary',
  dayResourceIds: number[],
) {
  await reorderSalesProductDesignerDayResources({
    arrangementRole,
    dayNo: activeDayNo.value,
    dayResourceIds,
    productId: productId.value,
  });
  await loadDetail();
}

function resetVehicleForm(item?: SalesProductDesignerApi.VehicleArrangement) {
  const travelDays = detail.value?.travelDays || 1;
  vehicleForm.id = item?.id;
  vehicleForm.resourceId = item?.resourceId;
  vehicleForm.supplierRelationId = item?.supplierRelationId;
  vehicleForm.vehicleType = item?.vehicleType || '';
  vehicleForm.startDayNo = item?.startDayNo ?? 1;
  vehicleForm.endDayNo = item?.endDayNo ?? travelDays;
  vehicleForm.quantity = item?.quantity || 1;
  vehicleForm.remark = item?.remark || '';
  vehicleSupplierCandidates.value = [];
}

async function loadVehicleResources(keyword = vehicleResourceKeyword.value) {
  const cleanKeyword = keyword.trim();
  // 服务端同样限制 2 个字符；保留已有候选，避免输入第一个字时清空选择器。
  if (cleanKeyword && cleanKeyword.length < 2) return;
  vehicleResourceLoading.value = true;
  try {
    const result = await getSalesProductDesignerVehicleResources({
      keyword: cleanKeyword || undefined,
      page: 1,
      pageSize: 50,
    });
    vehicleResources.value = result.items;
    const selected = vehicleResources.value.find((item) => item.id === vehicleForm.resourceId);
    if (selected) vehicleForm.vehicleType = selected.resourceName;
  } finally {
    vehicleResourceLoading.value = false;
  }
}

async function loadVehicleSupplierCandidates(resourceId?: number) {
  if (!resourceId) {
    vehicleSupplierCandidates.value = [];
    return;
  }
  const resource = await getSalesProductDesignerResourceDetail(resourceId);
  vehicleSupplierCandidates.value = resource.suppliers || [];
}

async function handleVehicleResourceChange(resourceId?: number) {
  vehicleForm.supplierRelationId = undefined;
  if (!resourceId) {
    vehicleForm.vehicleType = '';
    vehicleSupplierCandidates.value = [];
    return;
  }
  const selected = vehicleResources.value.find((item) => item.id === resourceId);
  vehicleForm.vehicleType = selected?.resourceName || '';
  await loadVehicleSupplierCandidates(resourceId);
}

async function openVehicleArrangement(item?: SalesProductDesignerApi.VehicleArrangement) {
  resetVehicleForm(item);
  vehicleResourceKeyword.value = '';
  vehicleArrangementOpen.value = true;
  await loadVehicleResources();
  if (item?.resourceId) await loadVehicleSupplierCandidates(item.resourceId);
}

async function saveVehicleArrangement() {
  if (!vehicleForm.resourceId) {
    message.warning('请选择用车资源');
    return;
  }
  if (!vehicleForm.vehicleType.trim()) {
    message.warning('请填写车型');
    return;
  }
  vehicleArrangementSaving.value = true;
  try {
    await saveSalesProductDesignerVehicleArrangement({
      endDayNo: vehicleForm.endDayNo,
      id: vehicleForm.id,
      productId: productId.value,
      quantity: vehicleForm.quantity,
      remark: vehicleForm.remark.trim() || undefined,
      resourceId: vehicleForm.resourceId,
      startDayNo: vehicleForm.startDayNo,
      supplierRelationId: vehicleForm.supplierRelationId,
      vehicleType: vehicleForm.vehicleType.trim(),
    });
    vehicleArrangementOpen.value = false;
    message.success(vehicleForm.id ? '全程用车已更新' : '全程用车已安排');
    await loadDetail();
  } finally {
    vehicleArrangementSaving.value = false;
  }
}

function deleteVehicleArrangement(item: SalesProductDesignerApi.VehicleArrangement) {
  Modal.confirm({
    content: `将移除“${item.resourceName}”这条全程用车安排，是否继续？`,
    okButtonProps: { danger: true },
    okText: '移除',
    onOk: async () => {
      await deleteSalesProductDesignerVehicleArrangement({
        productId: productId.value,
        vehicleArrangementId: item.id,
      });
      message.success('已移除全程用车');
      await loadDetail();
    },
    title: '确认移除全程用车',
  });
}

async function reorderVehicleArrangements(vehicleArrangementIds: number[]) {
  await reorderSalesProductDesignerVehicleArrangements({ productId: productId.value, vehicleArrangementIds });
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

async function saveHotelBreakfast(hotel: SalesProductDesignerApi.DayResource, included: boolean) {
  hotelBreakfastSaving.value = true;
  try {
    await saveSalesProductDesignerDayResource({
      arrangementRole: 'accommodation',
      dayNo: activeDayNo.value,
      hotelBreakfastIncluded: included,
      id: hotel.id,
      productId: productId.value,
      replaceBreakfastSource: false,
      resourceId: hotel.resourceId,
      supplierId: hotel.supplierId,
    });
    message.success(included ? '已标记为含次日早餐' : '已取消次日酒店早餐');
    await loadDetail();
  } catch (error) {
    if (included) {
      Modal.confirm({
        content: `将标记酒店含次日早餐，并清除 D${activeDayNo.value + 1} 已选外部早餐餐厅。是否继续？`,
        okText: '确认替换',
        onOk: async () => {
          await saveSalesProductDesignerDayResource({
            arrangementRole: 'accommodation', dayNo: activeDayNo.value, hotelBreakfastIncluded: true,
            id: hotel.id, productId: productId.value, replaceBreakfastSource: true,
            resourceId: hotel.resourceId, supplierId: hotel.supplierId,
          });
          message.success('已改为酒店含次日早餐');
          await loadDetail();
        },
        title: '确认替换早餐来源',
      });
    }
  } finally {
    hotelBreakfastSaving.value = false;
  }
}

function revokeProductWordPreviewUrl() {
  productWordPreviewLoadVersion += 1;
  if (productWordPreviewUrl.value) {
    URL.revokeObjectURL(productWordPreviewUrl.value);
    productWordPreviewUrl.value = '';
  }
}

/**
 * 右侧预览只读取后端生成的同一版本 PDF，不再在浏览器重新拼装 Word 排版。
 * 通过 Blob 读取可复用现有鉴权请求，避免把带权限的接口地址直接交给 iframe。
 */
async function loadProductWordPreview(version?: SalesProductDesignerApi.DocumentVersion) {
  const requestVersion = ++productWordPreviewLoadVersion;
  productWordPreviewError.value = '';
  if (!version || version.documentType !== 'product_word') {
    revokeProductWordPreviewUrl();
    productWordPreviewVersion.value = undefined;
    return;
  }
  if (version.generateStatus !== 'success') {
    productWordPreviewLoading.value = false;
    revokeProductWordPreviewUrl();
    productWordPreviewError.value = version.generateStatus === 'pending'
      ? 'Word 正在生成，完成后可预览。'
      : 'Word 生成失败，请重新生成。';
    return;
  }
  productWordPreviewLoading.value = true;
  const previousUrl = productWordPreviewUrl.value;
  try {
    const blob = await previewSalesProductDesignerDocument(version.id);
    if (requestVersion !== productWordPreviewLoadVersion) return;
    const contentType = blob.type.toLowerCase();
    if (!blob.size) throw new Error('Word 预览文件为空');
    if (contentType && contentType !== 'application/octet-stream' && !contentType.includes('application/pdf')) {
      throw new Error('Word 预览接口未返回 PDF 文件');
    }
    const pdfBlob = contentType.includes('application/pdf')
      ? blob
      : new Blob([blob], { type: 'application/pdf' });
    const nextUrl = URL.createObjectURL(pdfBlob);
    productWordPreviewUrl.value = nextUrl;
    productWordPreviewVersion.value = version;
    if (previousUrl && previousUrl !== nextUrl) {
      window.setTimeout(() => URL.revokeObjectURL(previousUrl), 0);
    }
  } catch (error) {
    if (requestVersion === productWordPreviewLoadVersion) {
      productWordPreviewError.value = designerErrorMessage(error, 'Word 预览加载失败，请稍后重试');
    }
  } finally {
    if (requestVersion === productWordPreviewLoadVersion) productWordPreviewLoading.value = false;
  }
}

function updateDocumentHistory(version: SalesProductDesignerApi.DocumentVersion) {
  documents.value = [version, ...documents.value.filter((item) => item.id !== version.id)];
}

/** 保存产品资料后生成新版本，并让预览和下载共同指向这个 versionId。 */
async function generateProductWordPreviewVersion(showError = false) {
  productWordGenerating.value = true;
  try {
    const version = await generateSalesProductDesignerProductWord(productId.value);
    updateDocumentHistory(version);
    productWordPreviewDirty.value = false;
    await loadProductWordPreview(version);
    if (showError && version.generateStatus !== 'success') {
      message.error(productWordPreviewError.value || '产品介绍 Word 生成失败');
    }
    return version;
  } catch (error) {
    productWordPreviewError.value = designerErrorMessage(error, '产品介绍 Word 生成失败，请稍后重试');
    if (showError) message.error(productWordPreviewError.value);
    return undefined;
  } finally {
    productWordGenerating.value = false;
  }
}

async function generateDocument(documentType: 'adult_quote' | 'product_word') {
  try {
    const version = documentType === 'product_word'
      ? await generateProductWordPreviewVersion(true)
      : await generateSalesProductDesignerAdultQuote(productId.value);
    if (!version) return;
    updateDocumentHistory(version);
    if (documentType === 'product_word' && version.generateStatus !== 'success') return;
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
    const latestProductWord = documents.value.find(
      (item) => item.documentType === 'product_word' && item.generateStatus === 'success',
    );
    if (latestProductWord) await loadProductWordPreview(latestProductWord);
    else {
      revokeProductWordPreviewUrl();
      productWordPreviewVersion.value = undefined;
    }
  } catch {
    documents.value = [];
    revokeProductWordPreviewUrl();
    productWordPreviewVersion.value = undefined;
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

async function downloadProductWordPreview() {
  const version = productWordPreviewVersion.value;
  if (!version || version.documentType !== 'product_word' || version.generateStatus !== 'success') {
    message.warning('请先生成产品介绍 Word');
    return;
  }
  try {
    const blob = await downloadSalesProductDesignerDocument(version.id);
    const url = URL.createObjectURL(blob);
    const anchor = window.document.createElement('a');
    anchor.href = url;
    anchor.download = version.fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  } catch {
    // request interceptor already reports the actionable server error.
  }
}

async function openDocumentPreview(version: SalesProductDesignerApi.DocumentVersion) {
  if (version.documentType !== 'product_word' || version.generateStatus !== 'success') {
    message.warning('只有生成成功的产品 Word 可在页面中预览');
    return;
  }
  await loadProductWordPreview(version);
  if (!productWordPreviewUrl.value) return;
  documentHistoryOpen.value = false;
  documentPreviewOpen.value = true;
}

function documentTypeText(version: SalesProductDesignerApi.DocumentVersion) {
  return version.documentType === 'product_word' ? '产品介绍 Word' : '成人报价单';
}

function onResourceDragStart(resource: SalesProductDesignerApi.MapResource) {
  draggedResource.value = resource;
}

async function onDayDrop(dayNo: number) {
  if (draggedResource.value) {
    activeDayNo.value = dayNo;
    await addResourceForArrangement(draggedResource.value);
    draggedResource.value = undefined;
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
watch(activeDayNo, () => {
  selectedDayResource.value = undefined;
  syncMapFiltersToActiveDay();
});
watch(
  () => [activeDayNo.value, activeDay.value?.destinationCity] as const,
  ([, city]) => {
    dayDestinationCityInput.value = city || '';
  },
  { immediate: true },
);
watch(
  () => editor.selectedMaterials.map((item) => `${item.materialType}:${item.introductionId || item.resourceOptionalItemId}:${item.salePrice ?? ''}`).join('|'),
  () => {
    if (!drawerOpen.value || !selectedResource.value) return;
    productWordPreviewDirty.value = true;
    void initializeMaterialSortable();
  },
);
watch(
  () => wordPlanFingerprint(),
  () => {
    if (!wordPlanOpen.value) return;
    productWordPreviewDirty.value = true;
    void initializeWordPlanSortable();
  },
);
watch(wordPlanEditorMode, () => {
  if (!wordPlanOpen.value) return;
  void initializeWordPlanSortable();
});

onMounted(async () => {
  await loadStarLevelOptions();
  await loadDetail();
  if (detail.value) await loadDocuments();
  await loadResources(true);
  await nextTick();
  renderMap();
});

onBeforeUnmount(() => {
  destroyMaterialSortable();
  destroyWordPlanSortable();
  revokeProductWordPreviewUrl();
  Object.values(wordPlanImageUrls).forEach((url) => URL.revokeObjectURL(url));
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
          <div class="summary-item"><span class="summary-label">日程资源成本</span><strong class="cost">{{ formatMoney(detail.dayResourceCostAmount) }}</strong></div>
          <div class="summary-item"><span class="summary-label">全程用车成本</span><strong class="cost">{{ formatMoney(detail.vehicleCostAmount) }}</strong></div>
          <div class="summary-item"><span class="summary-label">资源总成本</span><strong class="cost">{{ formatMoney(currentCost) }}</strong></div>
          <div class="summary-item summary-location"><span class="summary-label">接团城市</span>{{ areaText(detail) }}</div>
        </div>

        <ProductDesignerVehicleArrangementPanel
          :arrangements="detail.vehicleArrangements || []"
          :total-cost="detail.vehicleCostAmount"
          :travel-days="detail.travelDays"
          @add="openVehicleArrangement()"
          @edit="openVehicleArrangement"
          @remove="deleteVehicleArrangement"
          @reorder="reorderVehicleArrangements"
        />

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
              <span class="day-city" :class="{ 'is-empty': !day.destinationCity }">{{ day.destinationCity || '未设置城市' }}</span>
              <span class="day-cost">{{ formatMoney(day.dayCostAmount) }}</span>
            </Button>
          </div>
          <div class="day-destination-editor">
            <div class="day-destination-editor-heading">
              <span class="day-destination-title">
                <IconifyIcon icon="lucide:map-pin" />
                D{{ activeDayNo }} 住宿城市
              </span>
              <span class="day-destination-hint">{{ dayDestinationHint }}</span>
            </div>
            <div class="day-destination-editor-control">
              <Input
                v-model:value="dayDestinationCityInput"
                aria-label="设置当晚住宿城市"
                :disabled="dayDestinationSaving || activeAccommodationHasCity"
                :maxlength="80"
                :placeholder="activeAccommodationHasCity ? '跟随当晚酒店' : '输入城市，如无锡'"
                :title="activeAccommodationHasCity ? '城市已跟随当晚酒店自动设置' : '输入城市后按回车保存'"
                @blur="saveManualDayDestination"
                @press-enter="saveManualDayDestination"
              >
                <template #suffix><Spin v-if="dayDestinationSaving" size="small" /></template>
              </Input>
              <Button v-if="hasTemporaryMapCity" size="small" type="link" @click="syncMapFiltersToActiveDay">恢复</Button>
            </div>
          </div>
        </div>

        <div class="designer-grid">

          <Card class="map-panel" :title="`资源地图 · D${activeDayNo} · ${activeDayDestinationLabel}`">
            <template #extra>
              <Space size="small">
                <Tag color="blue">{{ mapResources.length }} 个点位</Tag>
                <Tag v-if="unlocatedResources.length" color="orange">{{ unlocatedResources.length }} 个未定位资源</Tag>
                <Button type="link" size="small" @click="openMapFullscreen">全屏地图</Button>
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
                placeholder="酒店/餐厅接待标准"
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
              <span>当前显示 {{ resources.length }} / {{ resourceTotal }} 项</span>
            </div>
            <div class="resource-list" @scroll="undefined">
              <div
                v-for="resource in resources"
                :key="resource.id"
                class="resource-row"
                :class="{ 'is-selected': resource.id === selectedMapResourceId }"
                role="button"
                tabindex="0"
                draggable="true"
                @dragstart="onResourceDragStart(resource)"
                @click="activateMapResource(resource)"
                @keydown.enter="activateMapResource(resource)"
              >
                <div class="resource-dot" :class="`type-${resource.resourceType}`"></div>
                <div class="resource-main">
                  <strong>{{ resource.resourceName }}</strong>
                  <span>{{ typeLabel[resource.resourceType] || resource.resourceType }} · {{ mapResourceAreaText(resource) }}</span>
                </div>
                <div class="resource-meta">
                  <Tag v-if="!hasCoordinates(resource)" color="orange">未定位</Tag>
                  <Tag v-if="resource.procurementMode === 'not_required'" color="green">无需采购</Tag>
                  <span v-else class="resource-price">{{ formatMoney(resource.referenceUnitPrice) }}</span>
                  <Tooltip :title="resourceActionLabel(resource)"><Button type="primary" size="small" :loading="addingResourceIds.has(resource.id)" @click.stop="handleMapAddResource(resource)">{{ resourceActionLabel(resource) }}</Button></Tooltip>
                  <Tooltip title="查看详情并配置"><Button type="text" size="small" @click.stop="openResource(resource)"><IconifyIcon icon="lucide:settings-2" /></Button></Tooltip>
                </div>
              </div>
              <div v-if="resourceLoading" class="list-loading"><Spin size="small" /> 正在加载资源</div>
              <div v-if="resources.length < resourceTotal && !resourceLoading" class="load-more"><Button type="link" @click="resourcePage += 1; loadResources()">加载更多</Button></div>
            </div>
          </Card>

          <ProductResourceMapWorkspace
            :active-day-no="activeDayNo"
            :arranged-resource-ids="activeArrangedResourceIds"
            :city-options="cityOptions"
            :day-destination="activeDayDestination"
            :filters="filters"
            :map-resources="mapResources"
            :open="mapFullscreenOpen"
            :province-options="provinceOptions"
            :resource-loading="resourceLoading"
            :resource-total="resourceTotal"
            :resources="resources"
            :resource-type-options="resourceTypeOptions"
            :scenic-level-options="scenicLevelOptions"
            :selected-resource-id="selectedMapResourceId"
            :show-scenic-level-filter="showScenicLevelFilter"
            :show-star-level-filter="showStarLevelFilter"
            :star-level-options="starLevelOptions"
            @activate-resource="activateMapResource"
            @city-search="handleMapCitySearch"
            @close="closeMapFullscreen"
            @filter-change="handleMapFilterChange"
            @load-more="loadMoreMapResources"
            @reset="resetResourceFilters"
          />

          <div class="plan-panel-cell">
            <Card class="plan-panel" :title="`第 ${activeDayNo} 天行程 · ${activeDayDestinationLabel}`">
              <template #extra><span class="plan-cost">{{ formatMoney(activeDay?.dayCostAmount) }}</span></template>
              <ProductDesignerDayArrangementPanel
                :active-day-no="activeDayNo"
                :breakfast-plan="activeBreakfastPlan"
                :highlighted-resource-id="highlightedDayResourceId"
                :resources="activeDay?.resources || []"
                :supplier-candidates-by-resource-id="supplierCandidatesByResourceId"
                :travel-days="detail?.travelDays || 1"
                @change-supplier="changeDayResourceSupplier"
                @request-suppliers="loadDayResourceSupplierCandidates"
                @remove-day-resource="deletePlan"
                @reorder-day-resources="reorderDayArrangement"
                @select-hotel="startHotelSelection"
                @select-itinerary-resource="startItinerarySelection"
                @select-meal-resource="startMealSelection"
                @toggle-hotel-breakfast="saveHotelBreakfast"
              />
              <div v-if="scenicWordPlanSummary.selectedCount || scenicWordPlanSummary.unconfiguredCount" class="plan-word-action-row">
                <span>景区素材已选 {{ scenicWordPlanSummary.selectedCount }} 项<template v-if="scenicWordPlanSummary.unconfiguredCount"> · {{ scenicWordPlanSummary.unconfiguredCount }} 个景区未配置</template></span>
                <Button size="small" type="primary" @click.stop="openDayWordPlan">制作 Word 方案</Button>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </Spin>

    <Modal
      v-model:open="restaurantMealPickerOpen"
      :destroy-on-close="true"
      :ok-button-props="{ disabled: !restaurantMealPickerRole }"
      cancel-text="取消"
      ok-text="安排餐厅"
      title="安排用餐"
      width="460px"
      @ok="confirmRestaurantMeal"
    >
      <p class="meal-picker-description">{{ restaurantMealPickerResource?.resourceName }} 将安排到当天哪个餐次？</p>
      <Radio.Group v-model:value="restaurantMealPickerRole" class="meal-picker-options">
        <Radio.Button value="breakfast">早餐</Radio.Button>
        <Radio.Button value="lunch">中餐</Radio.Button>
        <Radio.Button value="dinner">晚餐</Radio.Button>
      </Radio.Group>
    </Modal>

    <Modal
      v-model:open="vehicleArrangementOpen"
      :confirm-loading="vehicleArrangementSaving"
      :destroy-on-close="true"
      cancel-text="取消"
      ok-text="保存用车"
      title="安排全程用车"
      width="560px"
      @ok="saveVehicleArrangement"
    >
      <Alert class="vehicle-form-tip" message="这里选择车型和数量，也可预选意向车队；最终车队与价格在真实排团时按日期、路线和公里数确认。" show-icon type="info" />
      <Form layout="vertical" class="vehicle-arrangement-form">
        <Form.Item label="用车资源" required>
          <Select
            v-model:value="vehicleForm.resourceId"
            allow-clear
            show-search
            :filter-option="false"
            :loading="vehicleResourceLoading"
            placeholder="输入至少 2 个字搜索座位数或车型"
            @change="(value) => void handleVehicleResourceChange(typeof value === 'number' ? value : undefined)"
            @search="(value) => { vehicleResourceKeyword = value; void loadVehicleResources(value); }"
          >
            <Select.Option v-for="item in vehicleResources" :key="item.id" :value="item.id">
              {{ item.resourceName }}<template v-if="item.seatCount"> · {{ item.seatCount }} 座</template><template v-if="item.vehicleType"> · {{ item.vehicleType }}</template>
            </Select.Option>
          </Select>
        </Form.Item>
        <div class="vehicle-form-grid">
          <Form.Item label="车型" required><Input v-model:value="vehicleForm.vehicleType" allow-clear placeholder="如：39座大巴" /></Form.Item>
          <Form.Item label="数量" required><InputNumber v-model:value="vehicleForm.quantity" :min="1" :precision="2" style="width: 100%" /></Form.Item>
        </div>
        <div class="vehicle-form-grid">
          <Form.Item label="起始天"><InputNumber v-model:value="vehicleForm.startDayNo" :max="detail?.travelDays || 1" :min="1" style="width: 100%" /></Form.Item>
          <Form.Item label="结束天"><InputNumber v-model:value="vehicleForm.endDayNo" :max="detail?.travelDays || 1" :min="vehicleForm.startDayNo || 1" style="width: 100%" /></Form.Item>
        </div>
        <Form.Item label="意向车队（可选）">
          <Select v-model:value="vehicleForm.supplierRelationId" allow-clear :disabled="!vehicleForm.resourceId || !vehicleSupplierCandidates.length" placeholder="真实排团时仍需按实际路线重新询价确认">
            <Select.Option v-for="supplier in vehicleSupplierCandidates" :key="supplier.relationId" :value="supplier.relationId">
              {{ supplier.supplierName }} · {{ supplierPriceText(supplier) }}<template v-if="supplier.isDefault">（默认）</template>
            </Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="备注"><Input v-model:value="vehicleForm.remark" allow-clear :maxlength="500" placeholder="如：含司机、过路费或特殊车辆要求" /></Form.Item>
      </Form>
    </Modal>

    <Drawer
      v-model:open="drawerOpen"
      :destroy-on-close="true"
      :width="'min(1360px, 94vw)'"
      class="resource-detail-drawer"
      title="资源详情"
      @close="handleResourceDrawerClose"
    >
      <Spin :spinning="detailLoading">
        <template v-if="selectedResource">
          <div class="resource-detail-head">
            <div><Tag color="blue">{{ typeLabel[selectedResource.resourceType] || selectedResource.resourceType }}</Tag><h3>{{ selectedResource.resourceName }}</h3><p>{{ areaText(selectedResource) }} · {{ selectedResource.address || '地址未维护' }}</p></div>
          </div>
          <Alert v-if="selectedResource.procurementMode === 'not_required'" message="无需采购资源，成本按 0 元处理" type="success" show-icon />
          <Form layout="vertical" class="detail-form">
            <section class="detail-section detail-config-section">
              <div class="detail-config-grid">
                <Form.Item label="供应商">
                  <Select v-model:value="editor.supplierId" :disabled="selectedResource.procurementMode === 'not_required'" allow-clear placeholder="选择当前资源的有效供应商" @change="refreshOptionalSupplierReferences">
                    <Select.Option v-for="supplier in selectedResource.suppliers" :key="supplier.supplierId" :value="supplier.supplierId">{{ supplier.supplierName }} · {{ supplierPriceText(supplier) }}<template v-if="supplier.isDefault">（默认）</template></Select.Option>
                  </Select>
                </Form.Item>
                <Form.Item label="行程备注">
                  <Input v-model:value="editor.remark" allow-clear />
                </Form.Item>
              </div>
            </section>

            <section class="material-editor-layout">
              <div class="material-editor-pane">
                <div class="material-pane-heading">
                  <div>
                    <strong>素材编排</strong>
                    <span>勾选后按顺序输出；拖动可调整介绍与自费项目的先后。</span>
                  </div>
                  <span class="material-count">已选 {{ selectedMaterialEntries.length }} 项</span>
                </div>

                <div class="material-selection-section">
                  <div class="material-section-heading">已选内容</div>
                  <div class="material-sort-hint" role="note">
                    <IconifyIcon aria-hidden="true" icon="lucide:grip-vertical" />
                    <span>按住拖动图标调整输出顺序，也可用上下按钮操作</span>
                  </div>
                  <div
                    v-if="selectedMaterialEntries.length"
                    ref="materialSortableContainerRef"
                    class="material-selected-list"
                    role="list"
                  >
                    <div v-for="entry in selectedMaterialEntries" :key="`${entry.material.materialType}-${entry.material.introductionId || entry.material.resourceOptionalItemId}`" class="material-selected-row" role="listitem">
                      <span class="material-order">{{ entry.index + 1 }}</span>
                      <button class="material-drag-handle" type="button" :aria-label="`拖动「${entry.introduction?.title || entry.optionalItem?.projectName || '素材'}」调整顺序`" title="按住拖动排序">
                        <IconifyIcon aria-hidden="true" icon="lucide:grip-vertical" />
                      </button>
                      <div class="material-selected-main">
                        <div class="material-selected-title">
                          <strong :title="entry.introduction?.title || entry.optionalItem?.projectName">{{ entry.introduction?.title || entry.optionalItem?.projectName || '未找到素材' }}</strong>
                          <Tag v-if="entry.material.materialType === 'optional_item'" color="orange">自费</Tag>
                          <Tag v-if="entry.material.materialType === 'optional_item' && !optionalItemHasCurrentSupplierQuote(entry.material.resourceOptionalItemId)" color="warning">当前供应商未报价</Tag>
                        </div>
                        <span v-if="entry.material.materialType === 'optional_item'">对外价 {{ formatMoney(entry.material.salePrice) }}/人</span>
                        <span v-else>介绍素材 · v{{ entry.introduction?.indexVersion || 1 }}</span>
                      </div>
                      <Space class="material-row-actions" :size="0">
                        <Tooltip title="上移"><Button type="text" size="small" :disabled="entry.index === 0" @click="moveMaterial(entry.index, -1)"><IconifyIcon icon="lucide:chevron-up" /></Button></Tooltip>
                        <Tooltip title="下移"><Button type="text" size="small" :disabled="entry.index === selectedMaterialEntries.length - 1" @click="moveMaterial(entry.index, 1)"><IconifyIcon icon="lucide:chevron-down" /></Button></Tooltip>
                        <Button danger type="link" size="small" @click="removeSelectedMaterial(entry.index)">移除</Button>
                      </Space>
                    </div>
                  </div>
                  <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="先从下方勾选介绍素材或自费项目" />
                </div>

                <div class="material-selection-section material-candidate-section">
                  <div class="material-section-heading">可选介绍素材</div>
                  <div v-if="standardIntroductions.length" class="material-candidate-list">
                    <div v-for="intro in standardIntroductions" :key="intro.id" class="material-candidate-row" :class="{ 'is-selected': Boolean(selectedIntroductionMaterial(intro.id)) }">
                      <Checkbox :checked="Boolean(selectedIntroductionMaterial(intro.id))" @change="toggleIntroduction(intro.id, $event.target.checked)">
                        <span class="material-candidate-title" :title="intro.title">{{ intro.title }}</span>
                        <span class="material-candidate-meta">v{{ intro.indexVersion }}</span>
                      </Checkbox>
                      <Button type="link" size="small" @click.stop="showPreview(intro)">查看</Button>
                    </div>
                  </div>
                  <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="暂无已发布介绍素材" />
                </div>

                <div class="material-selection-section material-candidate-section">
                  <div class="material-section-heading">自费项目</div>
                  <Alert class="optional-item-alert" message="新选择只显示当前供应商已启用报价的项目；已选项目不会因切换供应商而消失。" show-icon type="info" />
                  <div v-if="optionalItems.length" class="material-candidate-list optional-candidate-list">
                    <div v-for="optionalItem in optionalItems" :key="optionalItem.id" class="material-candidate-row optional-candidate-row" :class="{ 'is-selected': Boolean(selectedOptionalItem(optionalItem.id)) }">
                      <div class="optional-candidate-title-line">
                        <Checkbox :checked="Boolean(selectedOptionalItem(optionalItem.id))" @change="toggleOptionalItem(optionalItem, $event.target.checked)">
                          <span class="material-candidate-title" :title="optionalItem.projectName">{{ optionalItem.projectName }}</span>
                        </Checkbox>
                        <Tag color="orange">{{ optionalItemTypeLabel[optionalItem.optionalItemType] }}</Tag>
                      </div>
                      <template v-if="selectedOptionalItem(optionalItem.id)">
                        <div class="optional-item-form">
                          <div class="optional-item-field"><span>介绍版本</span><Select :value="selectedOptionalItem(optionalItem.id)?.introductionId" allow-clear placeholder="可不选" @change="updateOptionalIntroduction(optionalItem.id, typeof $event === 'number' ? $event : undefined)"><Select.Option v-for="intro in introductionOptionsForOptionalItem(optionalItem.id)" :key="intro.id" :value="intro.id">{{ intro.title }} · v{{ intro.indexVersion }}</Select.Option></Select></div>
                          <div class="optional-item-field"><span>内部参考成本</span><strong class="optional-cost">{{ optionalReferenceCost(optionalItem.id) == null ? '未维护' : `${formatMoney(optionalReferenceCost(optionalItem.id))}/人` }}</strong></div>
                          <div class="optional-item-field"><span>系统默认价</span><strong>{{ suggestedOptionalSalePrice(optionalItem) == null ? '未维护' : `${formatMoney(suggestedOptionalSalePrice(optionalItem))}/人` }}</strong></div>
                          <div class="optional-item-field optional-sale-price">
                            <span>{{ selectedOptionalItem(optionalItem.id)?.salePriceDirty ? '本产品报价' : '系统默认价' }}</span>
                            <template v-if="isEditorOptionalPriceEditing(optionalItem.id)">
                              <InputNumber :value="selectedOptionalItem(optionalItem.id)?.salePrice" :min="0" :precision="2" addon-after="元/人" placeholder="使用系统默认价" @update:value="(value) => { const selected = selectedOptionalItem(optionalItem.id); if (selected) { selected.salePrice = value == null ? undefined : Number(value); selected.salePriceDirty = value != null; } }" />
                              <Button type="link" size="small" @click="resetEditorOptionalPrice(optionalItem.id)">恢复默认</Button>
                            </template>
                            <template v-else>
                              <strong>{{ selectedOptionalItem(optionalItem.id)?.salePrice == null || selectedOptionalItem(optionalItem.id)?.salePrice === 0 ? '未维护' : `${formatMoney(selectedOptionalItem(optionalItem.id)?.salePrice)}/人` }}</strong>
                              <Button type="link" size="small" @click="editEditorOptionalPrice(optionalItem.id)">修改</Button>
                            </template>
                          </div>
                        </div>
                      </template>
                    </div>
                  </div>
                  <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="当前供应商暂无可选自费项目" />
                </div>
              </div>

              <aside class="material-preview-pane" aria-label="当前资源 Word 效果预览">
                <div class="material-pane-heading">
                  <div><strong>产品 Word 预览</strong><span>与下载文件使用同一版本</span></div>
                  <Space size="small">
                    <Tag v-if="productWordPreviewVersion" color="blue">v{{ productWordPreviewVersion.versionNo }}</Tag>
                    <Button v-if="productWordPreviewVersion?.generateStatus === 'success'" size="small" @click="downloadProductWordPreview">下载 Word</Button>
                  </Space>
                </div>
                <div class="product-word-preview-status">
                  <span v-if="productWordGenerating">正在生成最新 Word 版本，请稍候…</span>
                  <span v-else-if="productWordPreviewDirty">存在未保存修改，当前显示最近生成的版本。</span>
                  <span v-else-if="productWordPreviewError" class="is-error">{{ productWordPreviewError }}</span>
                  <span v-else-if="productWordPreviewVersion">生成时间以版本记录为准 · 下载与此预览一致</span>
                  <span v-else>尚未生成产品介绍 Word</span>
                  <Button v-if="productWordPreviewError && !productWordGenerating" type="link" size="small" @click="generateProductWordPreviewVersion(true)">重新生成</Button>
                </div>
                <div class="word-pdf-preview-scroll">
                  <Spin :spinning="productWordPreviewLoading || productWordGenerating">
                    <iframe v-if="productWordPreviewUrl" class="word-pdf-preview-frame" :src="productWordPreviewUrl" title="产品 Word PDF 预览" />
                    <Empty v-else description="保存产品资料并生成 Word 后，这里显示真实模板预览" />
                  </Spin>
                </div>
              </aside>
            </section>
          </Form>
        </template>
      </Spin>
      <template #footer>
        <div v-if="selectedResource" class="drawer-actions">
          <Button @click="drawerOpen = false">取消</Button>
          <Button type="primary" :loading="saving" @click="addSelectedResource()">{{ selectedDayResource ? '保存编排' : `加入第 ${activeDayNo} 天` }}</Button>
        </div>
      </template>
    </Drawer>

    <Drawer
      :destroy-on-close="true"
      :open="wordPlanOpen"
      :width="'100vw'"
      class="day-word-plan-drawer"
      title="制作 Word 方案"
      @close="requestCloseDayWordPlan"
    >
      <Spin :spinning="wordPlanLoading">
        <template v-if="wordPlan">
          <div class="word-plan-workspace">
            <header class="word-plan-toolbar">
              <div class="word-plan-toolbar-title">
                <strong>D{{ wordPlan.dayNo }} · Word 内容编排</strong>
                <span>{{ wordPlan.resources.length }} 个景区 · 已选 {{ wordPlanSelected.length }} 项内容</span>
              </div>
              <div class="word-plan-toolbar-actions">
                <button
                  type="button"
                  class="word-plan-image-summary"
                  :class="{ 'is-invalid': wordPlanImageConfigurationInvalid }"
                  :aria-expanded="wordPlanImageSettingsOpen || wordPlanImagePickerOpen"
                  @click="wordPlanImageSettingsOpen = true"
                >
                  <IconifyIcon icon="lucide:images" />
                  <span><strong>图片设置</strong><small>{{ wordPlanImageSummary }}</small></span>
                  <IconifyIcon icon="lucide:chevron-right" />
                </button>
                <Button
                  type="primary"
                  :loading="wordPlanSaving || productWordGenerating"
                  @click="saveDayWordPlan"
                >
                  <IconifyIcon icon="lucide:save" />保存并刷新预览
                </Button>
              </div>
            </header>

            <div class="day-word-plan-layout">
              <section class="word-plan-editor-pane" aria-label="Word 内容编排">
                <div class="word-plan-editor-switcher">
                  <Radio.Group v-model:value="wordPlanEditorMode" size="small" button-style="solid">
                    <Radio.Button value="select">选择内容</Radio.Button>
                    <Radio.Button value="sort">调整顺序</Radio.Button>
                  </Radio.Group>
                  <span>{{ wordPlanEditorMode === 'select' ? '勾选要写入当天行程的素材' : '拖动景区或素材调整 Word 输出顺序' }}</span>
                </div>

                <div v-if="wordPlanEditorMode === 'select'" class="word-plan-source-scroll">
                  <section v-for="resource in wordPlan.resources" :key="resource.dayResource.id" class="word-plan-resource-group">
                    <header>
                      <strong>{{ resource.dayResource.resourceName }}</strong>
                      <Tag :color="wordPlanSelectedCountForResource(resource) ? 'blue' : 'default'">
                        已选 {{ wordPlanSelectedCountForResource(resource) }} 项
                      </Tag>
                    </header>

                    <div class="word-plan-candidate-section">
                      <div class="word-plan-candidate-title">介绍素材</div>
                      <div v-if="wordPlanIntroductionOptions(resource).length" class="word-plan-candidate-list">
                        <Checkbox
                          v-for="introduction in wordPlanIntroductionOptions(resource)"
                          :key="introduction.id"
                          :checked="isWordPlanSelected({ dayResourceId: resource.dayResource.id, introductionId: introduction.id, materialType: 'introduction' })"
                          @change="toggleWordPlanIntroduction(resource, introduction)"
                        >
                          <span class="word-plan-candidate-copy">
                            <strong class="word-plan-candidate-name">{{ introduction.title }}</strong>
                            <small v-if="formatWordPlanTags(introduction.tags)" class="word-plan-candidate-meta">{{ formatWordPlanTags(introduction.tags) }}</small>
                          </span>
                        </Checkbox>
                      </div>
                      <div v-else-if="!wordPlanSupplierOptionalItems(resource).length" class="word-plan-candidate-empty">暂无可写入 Word 的内容</div>
                    </div>

                    <div v-if="wordPlanSupplierOptionalItems(resource).length" class="word-plan-candidate-section word-plan-optional-section">
                      <div class="word-plan-candidate-title">自费项目</div>
                      <div class="word-plan-candidate-list">
                        <Checkbox
                          v-for="supplierItem in wordPlanSupplierOptionalItems(resource)"
                          :key="supplierItem.supplierOptionalItemId"
                          :checked="isWordPlanSelected({ dayResourceId: resource.dayResource.id, materialType: 'optional_item', resourceOptionalItemId: supplierItem.resourceOptionalItemId })"
                          @change="toggleWordPlanSupplierOptionalItem(resource, supplierItem)"
                        >
                          <span class="word-plan-candidate-copy">
                            <strong class="word-plan-candidate-name">{{ supplierItem.projectName }}</strong>
                            <small class="word-plan-candidate-meta">系统默认 {{ formatYuanPerPerson(supplierItem.suggestedSalePrice) }}</small>
                          </span>
                        </Checkbox>
                      </div>
                    </div>
                  </section>
                </div>

                <div v-else ref="wordPlanSortableContainerRef" class="word-plan-selected-list">
                  <Empty v-if="!wordPlanSelected.length" description="请先到“选择内容”勾选素材" />
                  <section
                    v-for="group in wordPlanSelectedGroups"
                    v-else
                    :key="group.key"
                    :data-scenic-group="group.key"
                    class="word-plan-selected-group"
                  >
                    <header class="word-plan-selected-group-heading">
                      <button class="word-plan-group-drag-handle" type="button" :aria-label="`拖动「${group.resourceName}」调整景区顺序`" title="拖动整个景区">
                        <IconifyIcon icon="lucide:grip-vertical" />
                      </button>
                      <div class="word-plan-group-title">
                        <strong>{{ group.resourceName }}</strong>
                        <span>整组拖动，不会把其他景区插入本组</span>
                      </div>
                      <Tag>{{ group.items.length }} 项</Tag>
                    </header>
                    <div class="word-plan-selected-group-list" :data-scenic-group="group.key">
                      <div v-for="item in group.items" :key="wordPlanMaterialKey(item)" class="word-plan-selected-row">
                        <button class="material-drag-handle" type="button" aria-label="拖动素材排序" title="在当前景区内拖动排序"><IconifyIcon icon="lucide:grip-vertical" /></button>
                        <div class="word-plan-selected-main">
                          <div class="word-plan-selected-line">
                            <Tag :color="item.materialType === 'optional_item' ? 'red' : 'blue'">{{ item.materialType === 'optional_item' ? '自费' : '介绍' }}</Tag>
                            <strong :title="item.materialType === 'optional_item' ? wordPlanOptionalItem(item)?.projectName : wordPlanIntroduction(item)?.title">{{ item.materialType === 'optional_item' ? wordPlanOptionalItem(item)?.projectName : wordPlanIntroduction(item)?.title }}</strong>
                          </div>
                          <span v-if="item.materialType === 'optional_item'" class="word-plan-sale-price">系统默认 {{ wordPlanDisplaySalePrice(item) }}</span>
                        </div>
                        <Button danger type="text" size="small" aria-label="移除" @click="wordPlanSelected = wordPlanSelected.filter((candidate) => wordPlanMaterialKey(candidate) !== wordPlanMaterialKey(item))"><IconifyIcon icon="lucide:x" /></Button>
                      </div>
                    </div>
                  </section>
                </div>
              </section>

              <aside class="word-plan-preview-pane" aria-label="Word 真实预览">
              <div class="word-plan-pane-heading">
                <div><strong>产品 Word 预览</strong><span>与下载文件使用同一版本</span></div>
                <div class="word-plan-preview-actions">
                  <Tag v-if="productWordPreviewVersion" color="blue">v{{ productWordPreviewVersion.versionNo }}</Tag>
                  <Button v-if="productWordPreviewVersion?.generateStatus === 'success'" size="small" @click="downloadProductWordPreview">下载 Word</Button>
                </div>
              </div>
              <div class="product-word-preview-status">
                <span v-if="productWordGenerating">正在生成最新 Word 版本，请稍候…</span>
                <span v-else-if="productWordPreviewDirty">存在未保存修改，当前显示最近生成的版本。</span>
                <span v-else-if="productWordPreviewError" class="is-error">{{ productWordPreviewError }}</span>
                <span v-else-if="productWordPreviewVersion">当前预览与下载 Word 一致</span>
                <span v-else>尚未生成产品介绍 Word</span>
                <Button v-if="productWordPreviewError && !productWordGenerating" type="link" size="small" @click="generateProductWordPreviewVersion(true)">重新生成</Button>
              </div>
              <div class="word-pdf-preview-scroll word-plan-preview-scroll">
                <Spin :spinning="productWordPreviewLoading || productWordGenerating">
                  <iframe v-if="productWordPreviewUrl" class="word-pdf-preview-frame" :src="productWordPreviewUrl" title="产品 Word PDF 预览" />
                  <Empty v-else description="点击“保存并刷新预览”后，这里显示真实模板效果" />
                </Spin>
              </div>
              </aside>
            </div>
          </div>
        </template>
      </Spin>
    </Drawer>

    <Modal
      v-model:open="wordPlanImageSettingsOpen"
      title="Word 图片设置"
      width="760px"
      :footer="null"
    >
      <div class="word-plan-image-settings-modal">
        <div class="word-plan-image-mode-row">
          <div><strong>图片展示方式</strong><span>只影响 Word 输出，不修改资源图片库。</span></div>
          <Radio.Group v-model:value="wordPlanImageMode" button-style="solid">
            <Radio.Button value="follow_resource">跟随景区</Radio.Button>
            <Radio.Button value="day_end">当天末尾</Radio.Button>
            <Radio.Button value="hidden">不展示</Radio.Button>
          </Radio.Group>
        </div>

        <div v-if="wordPlanImageMode === 'follow_resource'" class="word-plan-image-resource-settings">
          <div v-for="resource in wordPlan?.resources || []" :key="resource.dayResource.id" class="word-plan-image-resource-row">
            <div><strong>{{ resource.dayResource.resourceName }}</strong><span>图片紧跟该景区介绍输出</span></div>
            <Space size="small">
              <Tag :color="wordPlanImageSelectionState(resource) === 'invalid' ? 'error' : wordPlanSelectedImages(resource).length ? 'blue' : 'default'">
                {{ wordPlanImageTagText(resource) }}
              </Tag>
              <Button size="small" @click="openWordPlanImagePicker(resource)">选择图片</Button>
            </Space>
          </div>
        </div>

        <div v-else-if="wordPlanImageMode === 'day_end'" class="word-plan-day-end-setting">
          <div><strong>当天末尾统一图片</strong><span>从当天所有景区中选择 0、2 或 3 张，按拖动顺序输出。</span></div>
          <Space size="small">
            <Tag :color="wordPlanDayEndImageSelectionState() === 'valid' ? (wordPlanDayEndImageSelections.length ? 'blue' : 'default') : 'error'">
              {{ wordPlanDayEndImageTagText() }}
            </Tag>
            <Button type="primary" @click="openDayEndWordPlanImagePicker">选择当天图片</Button>
          </Space>
        </div>

        <Alert v-else message="本天行程只输出文字内容，不展示景区图片。" type="info" show-icon />
        <div class="word-plan-image-settings-actions">
          <Button type="primary" @click="wordPlanImageSettingsOpen = false">完成</Button>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="wordPlanImagePickerOpen"
      :footer="null"
      :title="wordPlanImagePickerScope === 'day_end'
        ? `选择当天末尾图片 · D${wordPlan?.dayNo || ''}`
        : `配置图片 · ${wordPlanImagePickerResource?.dayResource.resourceName || ''}`"
      :width="wordPlanImagePickerScope === 'day_end' ? '960px' : '820px'"
      @cancel="closeWordPlanImagePicker"
    >
      <Spin :spinning="wordPlanImagePickerLoading">
        <div v-if="wordPlanImagePickerScope === 'day_end' || wordPlanImagePickerResource" class="word-image-picker">
          <div class="word-image-picker-toolbar">
            <div>
              <strong>{{ wordPlanImagePickerScope === 'day_end' ? '当天末尾图片' : '选择要放进产品 Word 的图片' }}</strong>
              <span>{{ wordPlanImagePickerScope === 'day_end'
                ? '从当天各景区图片中统一选择；可不选，如需展示请选择 2 或 3 张，拖动可调整最终输出顺序。'
                : '可不选；如需展示请选 2 或 3 张，勾选后可在“已选图片”中拖动排序。' }}</span>
            </div>
            <Space size="small">
              <Tag :color="wordPlanImagePickerSelectionCount() === 1 || wordPlanImagePickerSelectionCount() > WORD_IMAGE_MAX_COUNT ? 'error' : 'blue'">已选 {{ wordPlanImagePickerSelectionCount() }} / {{ WORD_IMAGE_MAX_COUNT }} 张</Tag>
              <Button size="small" @click="clearWordPlanImages">清空选择</Button>
            </Space>
          </div>
          <div v-if="wordPlanImagePickerSelectionCount() === 1 || wordPlanImagePickerSelectionCount() > WORD_IMAGE_MAX_COUNT" class="word-image-selection-warning">
            <template v-if="wordPlanImagePickerSelectionCount() > WORD_IMAGE_MAX_COUNT">
              历史方案已选 {{ wordPlanImagePickerSelectionCount() }} 张图片，超过当前 3 张上限。请精简到 2 或 3 张，或清空后再保存。
            </template>
            <template v-else>
              单张图片无法保存到 Word 方案，请再选择 1 至 2 张图片，或清空当前选择。
            </template>
          </div>
          <div v-if="wordPlanImagePickerSelectedItems().length" class="word-image-selected-list">
            <div class="word-image-section-label">已选图片顺序</div>
            <div
              v-for="(item, index) in wordPlanImagePickerSelectedItems()"
              :key="item.key"
              class="word-image-selected-item"
              draggable="true"
              @dragstart="($event) => { ($event.dataTransfer as DataTransfer).setData('text/plain', String(index)); }"
              @dragover.prevent
              @drop="($event) => moveWordPlanImage(Number(($event.dataTransfer as DataTransfer).getData('text/plain')), index)"
            >
              <span class="word-image-order">{{ index + 1 }}</span>
              <img v-if="wordPlanImageUrl(item.image)" :src="wordPlanImageUrl(item.image)" :alt="item.image.originalFilename" />
              <div v-else class="word-image-fallback"><IconifyIcon icon="lucide:image" /></div>
              <div class="word-image-selected-copy">
                <span v-if="wordPlanImagePickerScope === 'day_end'" class="word-image-resource-name" :title="item.resource.dayResource.resourceName">{{ item.resource.dayResource.resourceName }}</span>
                <span class="word-image-filename" :title="item.image.originalFilename">{{ item.image.originalFilename }}</span>
              </div>
              <IconifyIcon class="word-image-drag-icon" icon="lucide:grip-vertical" />
            </div>
          </div>
          <div class="word-image-section-label">{{ wordPlanImagePickerScope === 'day_end' ? '按景区选择图片' : '图片素材库' }}</div>
          <div v-if="wordPlanImagePickerScope === 'day_end'" class="word-image-resource-list">
            <section v-for="resource in wordPlan?.resources || []" :key="resource.dayResource.id" class="word-image-resource-group">
              <header>
                <strong>{{ resource.dayResource.resourceName }}</strong>
                <span>{{ resource.resourceDetail.images.length }} 张可选</span>
              </header>
              <div v-if="resource.resourceDetail.images.length" class="word-image-grid">
                <label
                  v-for="image in resource.resourceDetail.images"
                  :key="image.id"
                  class="word-image-card"
                  :class="{ 'is-selected': isWordPlanImagePickerSelected(resource, image.id) }"
                >
                  <Checkbox
                    :checked="isWordPlanImagePickerSelected(resource, image.id)"
                    @change="(event) => toggleWordPlanImage(resource, image.id, Boolean(event.target?.checked))"
                  />
                  <img v-if="wordPlanImageUrl(image)" :src="wordPlanImageUrl(image)" :alt="image.originalFilename" />
                  <div v-else class="word-image-card-placeholder"><IconifyIcon icon="lucide:image" /><span>预览不可用</span></div>
                  <div class="word-image-card-name" :title="image.originalFilename">{{ image.originalFilename }}</div>
                </label>
              </div>
              <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="该景区还没有图片素材" class="word-image-resource-empty" />
            </section>
          </div>
          <template v-else-if="wordPlanImagePickerResource">
            <div v-if="wordPlanImagePickerResource.resourceDetail.images.length" class="word-image-grid">
              <label
                v-for="image in wordPlanImagePickerResource.resourceDetail.images"
                :key="image.id"
                class="word-image-card"
                :class="{ 'is-selected': isWordPlanImagePickerSelected(wordPlanImagePickerResource, image.id) }"
              >
                <Checkbox
                  :checked="isWordPlanImagePickerSelected(wordPlanImagePickerResource, image.id)"
                  @change="(event) => toggleWordPlanImage(wordPlanImagePickerResource!, image.id, Boolean(event.target?.checked))"
                />
                <img v-if="wordPlanImageUrl(image)" :src="wordPlanImageUrl(image)" :alt="image.originalFilename" />
                <div v-else class="word-image-card-placeholder"><IconifyIcon icon="lucide:image" /><span>预览不可用</span></div>
                <div class="word-image-card-name" :title="image.originalFilename">{{ image.originalFilename }}</div>
              </label>
            </div>
            <Empty v-else description="当前景区还没有图片素材" />
          </template>
        </div>
      </Spin>
    </Modal>

    <Card v-if="detail" class="latest-document-card" title="最新产品文件">
      <template #extra>
        <Button size="small" type="link" @click="documentHistoryOpen = true">
          历史版本{{ historicalDocumentCount ? ` ${historicalDocumentCount}` : '' }}
          <IconifyIcon icon="lucide:chevron-right" />
        </Button>
      </template>
      <div v-if="latestProductDocument" class="latest-document-row">
        <div class="latest-document-icon"><IconifyIcon icon="lucide:file-text" /></div>
        <div class="document-main">
          <strong :title="latestProductDocument.fileName">{{ latestProductDocument.fileName }}</strong>
          <span>产品介绍 Word · v{{ latestProductDocument.versionNo }} · 预览与下载为同一版本</span>
        </div>
        <Tag v-if="latestProductDocument.generateStatus === 'pending'" color="orange">生成中</Tag>
        <Tag v-else-if="latestProductDocument.generateStatus === 'failed'" color="red">生成失败</Tag>
        <Space v-else size="small">
          <Button size="small" @click="openDocumentPreview(latestProductDocument)">预览</Button>
          <Button size="small" type="primary" @click="downloadDocument(latestProductDocument)">下载 Word</Button>
        </Space>
      </div>
      <div v-else class="latest-document-empty">
        <span>尚未生成产品 Word</span>
        <Button size="small" type="primary" :loading="productWordGenerating" @click="generateProductWordPreviewVersion(true)">生成产品 Word</Button>
      </div>
    </Card>

    <Drawer
      v-model:open="documentHistoryOpen"
      title="历史文件"
      width="560px"
    >
      <div v-if="documents.length" class="document-list">
        <div v-for="item in documents" :key="item.id" class="document-row">
          <div class="document-main"><strong>{{ item.fileName }}</strong><span>{{ documentTypeText(item) }} · v{{ item.versionNo }}</span></div>
          <Button v-if="item.documentType === 'product_word' && item.generateStatus === 'success'" type="link" size="small" @click="openDocumentPreview(item)">预览</Button>
          <Button v-if="item.generateStatus === 'success'" type="link" size="small" @click="downloadDocument(item)">下载</Button>
          <Tag v-else-if="item.generateStatus === 'pending'" color="orange">生成中</Tag>
          <Tag v-else color="red">生成失败</Tag>
        </div>
      </div>
      <Empty v-else description="暂无历史文件" />
    </Drawer>

    <Drawer
      v-model:open="documentPreviewOpen"
      :title="productWordPreviewVersion ? `产品 Word 预览 · v${productWordPreviewVersion.versionNo}` : '产品 Word 预览'"
      :width="'min(920px, 96vw)'"
      class="product-document-preview-drawer"
    >
      <Spin :spinning="productWordPreviewLoading">
        <iframe v-if="productWordPreviewUrl" class="document-preview-frame" :src="productWordPreviewUrl" title="产品 Word PDF 预览" />
        <Empty v-else description="预览加载失败，请重试" />
      </Spin>
      <template #footer>
        <div class="drawer-actions">
          <Button @click="documentPreviewOpen = false">关闭</Button>
          <Button v-if="productWordPreviewVersion?.generateStatus === 'success'" type="primary" @click="downloadProductWordPreview">下载 Word</Button>
        </div>
      </template>
    </Drawer>

    <Modal v-model:open="previewOpen" :footer="null" title="介绍正文预览" width="720px">
      <div v-if="introPreview" class="intro-preview-modal">
        <Tag color="blue">已发布 · v{{ introPreview.indexVersion }}</Tag>
        <h3>{{ introPreview.title }}</h3>
        <p>{{ introPreview.content }}</p>
        <p v-if="introPreview.visitDuration">{{ formatVisitDuration(introPreview.visitDuration) }}</p>
        <p v-if="warmTipPreviewText(introPreview.warmTipContent).trim()" class="intro-warm-tip">{{ warmTipPreviewText(introPreview.warmTipContent) }}</p>
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
.day-tabs :deep(.day-tab.ant-btn) { display: grid; grid-template-columns: 34px minmax(42px, 1fr); grid-template-rows: 16px 18px 16px; min-width: 132px; height: 62px; padding: 6px 10px; border: 1px solid #e5e7eb; border-radius: 4px; background: #fff; color: #475569; text-align: left; }
.day-tabs :deep(.day-tab.ant-btn:hover) { border-color: #91caff; background: #f6faff; color: #0958d9; }
.day-tabs :deep(.day-tab.ant-btn.active) { border-color: #69b1ff; background: #eaf3ff; color: #0958d9; box-shadow: inset 3px 0 0 #1677ff; }
.day-no { grid-row: span 3; align-self: center; font-size: 16px; font-weight: 700; }
.day-count { align-self: start; font-size: 11px; line-height: 16px; }
.day-city { overflow: hidden; color: #334155; font-size: 12px; font-weight: 600; line-height: 18px; text-overflow: ellipsis; white-space: nowrap; }.day-city.is-empty { color: #ad6800; font-weight: 400; }
.day-cost { align-self: end; overflow: hidden; color: #64748b; font-size: 11px; line-height: 16px; text-overflow: ellipsis; }
.day-destination-editor { display: grid; min-width: 238px; gap: 5px; padding-left: 12px; border-left: 1px solid #f0f0f0; }
.day-destination-editor-heading { display: flex; min-width: 0; align-items: center; justify-content: space-between; gap: 8px; }
.day-destination-title { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 5px; color: #334155; font-size: 12px; font-weight: 600; }
.day-destination-title :deep(svg) { color: #1677ff; }
.day-destination-hint { min-width: 0; overflow: hidden; color: #ad6800; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.day-destination-editor-control { display: flex; min-width: 0; align-items: center; gap: 2px; }
.day-destination-editor-control :deep(.ant-input-affix-wrapper) { min-width: 0; flex: 1; }
.day-destination-editor-control :deep(.ant-btn) { padding-inline: 6px; }
.designer-grid { display: grid; grid-template-columns: minmax(760px, 1fr) minmax(340px, 390px); gap: 12px; align-items: stretch; }
.plan-panel-cell { position: relative; min-width: 0; min-height: 0; }
.plan-panel {
  position: absolute;
  inset: 0;
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
}
.plan-panel :deep(.ant-card-head) { flex: 0 0 auto; }
.map-panel :deep(.ant-card-body) { padding: 16px; }
.resource-filters { display: grid; grid-template-columns: minmax(220px, 1fr) 140px 140px 160px 120px; gap: 8px; margin-bottom: 12px; }
.map-container { position: relative; height: var(--designer-map-height); overflow: hidden; border: 1px solid #d9d9d9; border-radius: 4px; background: #f5f7fa; }
.map-container :deep(.amap-marker-label) { padding: 0; border: 0; background: transparent; box-shadow: none; }
.map-container :deep(.designer-map-label) { display: inline-block; padding: 2px 5px; border: 1px solid #d9d9d9; border-radius: 3px; background: rgb(255 255 255 / 94%); color: #334155; font-size: 11px; line-height: 16px; box-shadow: 0 1px 4px rgb(15 23 42 / 14%); white-space: nowrap; }
.map-container :deep(.designer-map-label.is-selected) { padding: 3px 7px; border-color: #69b1ff; background: #eaf3ff; color: #0958d9; font-size: 12px; font-weight: 600; box-shadow: 0 2px 8px rgb(22 119 255 / 18%); }
.map-loading { position: absolute; inset: 45% auto auto 50%; z-index: 2; }
.map-empty { display: grid; height: 100%; place-items: center; }
.resource-list-header { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px 9px; border-bottom: 1px solid #e5e7eb; color: #1f2937; }
.resource-list-header span { color: #8c8c8c; font-size: 12px; }
.resource-list { max-height: 248px; overflow-y: auto; border: 1px solid #f0f0f0; border-radius: 4px; }
.resource-row { display: flex; align-items: center; gap: 6px; min-height: 56px; padding: 7px 10px; border-bottom: 1px solid #f0f0f0; cursor: grab; }
.resource-row:last-child { border-bottom: 0; }
.resource-row:hover, .resource-row.is-selected { background: #f6faff; }
.resource-row.is-selected { box-shadow: inset 3px 0 0 #1677ff; }
.resource-dot { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: #1677ff; }
.type-hotel { background: #722ed1; }.type-restaurant { background: #fa8c16; }.type-shopping { background: #13c2c2; }.type-vehicle { background: #52c41a; }
.resource-main, .plan-main { min-width: 0; flex: 1; }
.resource-main strong, .resource-main span, .plan-main strong, .plan-main span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.resource-main span, .plan-main span { margin-top: 2px; color: #64748b; font-size: 11px; }
.resource-meta { display: flex; align-items: center; gap: 6px; color: #1677ff; font-size: 12px; }
.resource-meta :deep(.ant-tag) { margin-inline-end: 0; }
.resource-price { color: #1677ff; font-size: 12px; white-space: nowrap; }
.list-loading, .load-more { padding: 10px; text-align: center; color: #64748b; }
.plan-panel { overflow: hidden; }
.plan-panel :deep(.ant-card-body) { min-height: 0; flex: 1; overflow-y: auto; padding: 12px 16px; }
.day-itinerary-section { margin-bottom: 12px; border: 1px solid #e5e7eb; border-radius: 4px; background: #fafcff; }
.day-itinerary-summary { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 52px; padding: 8px 10px; }
.day-itinerary-summary-main { display: grid; min-width: 0; gap: 3px; }
.day-itinerary-summary-main > div { display: flex; min-width: 0; align-items: center; gap: 6px; }
.day-itinerary-summary-main strong, .day-itinerary-summary-main > div > span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.day-itinerary-label { flex: 0 0 32px; color: #64748b; font-size: 12px; }
.day-arrangement-block { padding: 10px; border-top: 1px solid #e5e7eb; background: #fff; }
.day-arrangement-block + .day-arrangement-block { border-top-color: #edf0f5; }
.day-arrangement-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; color: #1f2937; font-size: 13px; }
.day-arrangement-heading span { color: #8c8c8c; font-size: 12px; }
.day-arrangement-item { display: flex; align-items: center; gap: 7px; min-height: 34px; padding: 5px 0; }
.day-arrangement-main { min-width: 0; flex: 1; }
.day-arrangement-main strong, .day-arrangement-main span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.day-arrangement-main strong { color: #334155; font-size: 12px; }
.day-arrangement-main span, .day-arrangement-muted { color: #64748b; font-size: 11px; }
.day-arrangement-empty { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 34px; color: #64748b; font-size: 12px; }
.meal-picker-description { margin: 0 0 14px; color: #64748b; }
.meal-picker-options { display: flex; width: 100%; }
.meal-picker-options :deep(.ant-radio-button-wrapper) { flex: 1; text-align: center; }
.vehicle-form-tip { margin-bottom: 14px; }
.vehicle-arrangement-form { padding-top: 2px; }
.vehicle-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
@media (max-width: 560px) { .vehicle-form-grid { grid-template-columns: 1fr; gap: 0; } }
.meal-slot-label { width: 30px; flex: 0 0 30px; color: #64748b; font-size: 12px; }
.drop-zone { min-height: calc(var(--designer-map-height) + 24px); }
.plan-empty { display: grid; min-height: 320px; place-items: center; }
.plan-group { margin-bottom: 12px; border: 1px solid #e5e7eb; border-radius: 4px; overflow: hidden; }
.plan-group:last-child { margin-bottom: 0; }
.plan-group-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 38px; padding: 8px 10px; background: #f8fafc; border-bottom: 1px solid #e5e7eb; }
.plan-group-title, .plan-group-meta { display: flex; align-items: center; gap: 6px; min-width: 0; }
.plan-group-title strong { overflow: hidden; color: #1f2937; text-overflow: ellipsis; white-space: nowrap; }
.plan-group-count { color: #64748b; font-size: 12px; }
.plan-group-meta { flex: 0 0 auto; color: #1677ff; font-size: 12px; }
.plan-word-action-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 7px 10px; color: #64748b; font-size: 12px; line-height: 20px; background: #fff; border-bottom: 1px solid #e5e7eb; }
.plan-word-action-row > span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.plan-group-marker { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: #1677ff; }
.plan-group-marker.type-hotel { background: #722ed1; }.plan-group-marker.type-restaurant { background: #fa8c16; }.plan-group-marker.type-shopping { background: #13c2c2; }.plan-group-marker.type-vehicle { background: #52c41a; }
.plan-row { display: flex; align-items: flex-start; gap: 8px; padding: 10px; border-bottom: 1px solid #f0f0f0; cursor: grab; }
.plan-group .plan-row:last-child { border-bottom: 0; }
.plan-index { display: grid; width: 24px; height: 24px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: #eaf3ff; color: #0958d9; font-size: 12px; font-weight: 600; }
.plan-actions { display: flex; flex: 0 0 auto; align-items: center; }
.plan-name-line { display: flex; align-items: center; gap: 6px; min-width: 0; }
.plan-name-line strong { min-width: 0; }
.plan-introduction { color: #475569 !important; }
.plan-introduction.is-empty { color: #b45309 !important; }
.resource-detail-head h3 { margin: 7px 0 2px; font-size: 20px; }
.resource-detail-head p { margin: 0 0 18px; color: #64748b; }
.detail-form { margin-top: 18px; }
.resource-detail-drawer :deep(.ant-drawer-content-wrapper) { max-width: 100vw; }
.resource-detail-drawer :deep(.ant-drawer-body) { padding: 20px 24px; }
.detail-section + .detail-section { padding-top: 18px; margin-top: 18px; border-top: 1px solid #f0f0f0; }
.detail-section-title { margin-bottom: 12px; color: #1f2937; font-size: 14px; font-weight: 600; }
.detail-config-grid { display: grid; grid-template-columns: minmax(260px, 1fr) minmax(300px, 1.2fr); gap: 0 16px; }
.detail-config-grid :deep(.ant-form-item) { margin-bottom: 16px; }
.intro-notice { display: grid; gap: 4px; margin-top: 8px; color: #cf1322; white-space: pre-wrap; line-height: 1.75; }
.material-editor-layout { display: grid; grid-template-columns: minmax(480px, 1.08fr) minmax(380px, 0.92fr); gap: 20px; align-items: start; padding-top: 4px; border-top: 1px solid #f0f0f0; }
.material-editor-pane, .material-preview-pane { min-width: 0; }
.material-preview-pane { position: sticky; top: 0; }
.material-pane-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; min-height: 40px; margin-bottom: 10px; }
.material-pane-heading strong, .material-pane-heading span { display: block; }
.material-pane-heading strong { color: #1f2937; font-size: 14px; font-weight: 600; }
.material-pane-heading span { margin-top: 3px; color: #64748b; font-size: 12px; line-height: 18px; }
.material-pane-heading .material-count { flex: 0 0 auto; margin-top: 0; color: #1677ff; }
.material-selection-section { padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: #fff; }
.material-selection-section + .material-selection-section { margin-top: 12px; }
.material-section-heading { margin-bottom: 8px; color: #334155; font-size: 13px; font-weight: 600; }
.material-sort-hint { display: flex; align-items: center; gap: 4px; margin-bottom: 8px; color: #64748b; font-size: 12px; }
.material-sort-hint :deep(svg) { color: #8c8c8c; }
.material-selected-list { display: grid; gap: 4px; max-height: 300px; overflow: auto; }
.material-selected-row { display: flex; align-items: center; gap: 7px; min-height: 48px; padding: 6px 4px; border-bottom: 1px solid #f0f0f0; background: #fff; }
.material-selected-row:last-child { border-bottom: 0; }
.material-order { display: grid; width: 20px; height: 20px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: #e6f4ff; color: #1677ff; font-size: 12px; font-variant-numeric: tabular-nums; }
.material-drag-handle { display: grid; width: 26px; height: 30px; flex: 0 0 auto; place-items: center; padding: 0; border: 0; border-radius: 4px; background: transparent; color: #8c8c8c; cursor: grab; }
.material-drag-handle:hover, .material-drag-handle:focus-visible { background: #f0f7ff; color: #1677ff; outline: 0; }
.material-drag-handle:active { cursor: grabbing; }
.material-selected-main { min-width: 0; flex: 1; }
.material-selected-title { display: flex; align-items: center; gap: 4px; min-width: 0; }
.material-selected-title strong { overflow: hidden; color: #334155; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.material-selected-main > span { display: block; margin-top: 2px; overflow: hidden; color: #64748b; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.material-row-actions { flex: 0 0 auto; }
.material-sort-ghost { border: 1px dashed #69b1ff !important; background: #f0f7ff !important; opacity: 0.8; }
.material-sort-chosen, .material-sort-dragging { background: #fafcff; }
.material-candidate-section { background: #fafcff; }
.material-candidate-list { display: grid; gap: 4px; }
.material-candidate-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 38px; padding: 4px 6px; border: 1px solid transparent; border-radius: 4px; }
.material-candidate-row:hover, .material-candidate-row.is-selected { border-color: #91caff; background: #f6faff; }
.material-candidate-row :deep(.ant-checkbox-wrapper) { min-width: 0; flex: 1; }
.material-candidate-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.material-candidate-meta { margin-left: 6px; color: #8c8c8c; font-size: 12px; }
.optional-item-alert { margin: -2px 0 8px; }
.optional-candidate-row { display: block; padding: 8px; }
.optional-candidate-title-line { display: flex; align-items: center; gap: 6px; }
.optional-candidate-title-line :deep(.ant-checkbox-wrapper) { min-width: 0; flex: 1; }
.optional-item-form { display: grid; grid-template-columns: minmax(150px, 1.2fr) minmax(126px, 0.9fr) minmax(118px, 0.8fr) minmax(150px, 1fr); gap: 10px; margin-top: 10px; padding-top: 10px; border-top: 1px dashed #d9e2ec; }
.optional-item-field { display: grid; min-width: 0; gap: 4px; color: #64748b; font-size: 12px; }
.optional-item-field > strong { overflow: hidden; color: #334155; font-size: 13px; line-height: 32px; text-overflow: ellipsis; white-space: nowrap; }
.optional-item-field.optional-sale-price > strong { color: #1677ff; }
.optional-item-field :deep(.ant-input-number) { width: 100%; }
.optional-cost { color: #8c5a00 !important; }
.product-word-preview-status { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 32px; padding: 5px 8px; margin: 0 0 8px; color: #64748b; font-size: 12px; line-height: 20px; }
.product-word-preview-status > span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-word-preview-status .is-error { color: #cf1322; }
.word-pdf-preview-scroll { min-height: 520px; padding: 8px; overflow: auto; background: #eef2f6; border: 1px solid #dbe4ee; border-radius: 8px; }
.word-pdf-preview-scroll :deep(.ant-spin-nested-loading), .word-pdf-preview-scroll :deep(.ant-spin-container) { min-height: 500px; }
.word-pdf-preview-frame { display: block; width: 100%; min-height: 760px; border: 0; border-radius: 3px; background: #fff; }
.word-preview-scroll { max-height: calc(100vh - 294px); min-height: 520px; padding: 14px; overflow: auto; background: #f5f5f5; border: 1px solid #e5e7eb; border-radius: 6px; }
.word-preview-sheet { width: min(100%, 540px); min-height: 700px; padding: 30px 34px; margin: 0 auto; color: #000; font-family: "Noto Sans SC", "Microsoft YaHei", sans-serif; font-size: 10pt; line-height: 1.72; background: #fff; box-shadow: 0 1px 4px rgb(0 0 0 / 12%); }
.word-preview-material + .word-preview-material { margin-top: 6px; }
.word-preview-material-line { margin: 0; font: inherit; line-height: inherit; text-indent: 2em; }
.word-preview-material--inline { display: inline; margin-top: 0 !important; }
.word-preview-material--inline .word-preview-material-line { display: inline; text-indent: 0; }
.word-preview-material--inline-start .word-preview-material-line::before { content: '\3000'; }
.word-preview-material--inline .word-preview-images { margin-top: 10px; }
.word-preview-attraction-title { color: #0070c0; font-weight: 700; }
.word-preview-content, .word-preview-duration, .word-preview-empty { margin: 0; font: inherit; line-height: inherit; }
.word-preview-empty { color: #8c8c8c; text-align: center; }
.word-preview-extension-block { display: block; margin: 0; padding-left: 2em; font: inherit; line-height: inherit; }
.word-preview-extension-title { display: block; margin: 0; font: inherit; font-weight: 700; }
.word-preview-extension-items { display: block; margin: 0; padding-left: 0; list-style: none; }
.word-preview-extension-items li { padding-left: 0; }
.word-preview-extension-multiline { white-space: pre-line; }
.word-preview-notice, .word-preview-optional-price { color: #f00; }
.word-preview-warm-tip, .intro-warm-tip { color: #0070c0; white-space: pre-line; }
.word-preview-warm-tip { display: block; padding-left: 2em; margin: 4px 0 0; font: inherit; line-height: inherit; }
.word-preview-optional-price { margin: 0 0 4px; font: inherit; font-weight: 700; line-height: inherit; }
.word-preview-images { display: none; }
.word-preview-images img, .word-preview-image-placeholder { width: 100%; min-height: 108px; object-fit: cover; border: 1px solid #f0f0f0; }
.word-preview-image-placeholder { display: grid; place-items: center; gap: 6px; padding: 10px; color: #8c8c8c; font-size: 12px; text-align: center; }
.word-preview-disclaimer { margin: 8px 0 0; color: #8c8c8c; font-size: 12px; line-height: 1.6; }
.drawer-actions { display: flex; justify-content: flex-end; gap: 8px; }
.day-word-plan-drawer :deep(.ant-drawer-content-wrapper) { width: 100vw !important; max-width: 100vw; }
.day-word-plan-drawer :deep(.ant-drawer-header) { min-height: 54px; padding: 13px 20px; border-bottom-color: #e5e7eb; }
.day-word-plan-drawer :deep(.ant-drawer-title) { color: #1f2937; font-size: 16px; font-weight: 650; }
.day-word-plan-drawer :deep(.ant-drawer-body) { padding: 0; overflow: hidden; background: #f4f6f8; }
.day-word-plan-drawer :deep(.ant-spin-nested-loading), .day-word-plan-drawer :deep(.ant-spin-container) { height: 100%; }
.word-plan-workspace { display: flex; height: calc(100vh - 55px); min-height: 0; flex-direction: column; }
.word-plan-toolbar { display: flex; min-height: 66px; flex: 0 0 auto; align-items: center; justify-content: space-between; gap: 20px; padding: 10px 20px; background: #fff; border-bottom: 1px solid #e5e7eb; }
.word-plan-toolbar-title { min-width: 0; }
.word-plan-toolbar-title strong, .word-plan-toolbar-title span { display: block; }
.word-plan-toolbar-title strong { color: #1f2937; font-size: 15px; font-weight: 650; }
.word-plan-toolbar-title span { margin-top: 3px; color: #64748b; font-size: 12px; }
.word-plan-toolbar-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 10px; }
.word-plan-image-summary { display: flex; min-width: 204px; align-items: center; gap: 9px; padding: 6px 10px; color: #475569; text-align: left; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; cursor: pointer; transition: border-color .16s ease, color .16s ease, background .16s ease; }
.word-plan-image-summary:hover, .word-plan-image-summary:focus-visible { color: #1677ff; background: #f7fbff; border-color: #4096ff; outline: none; }
.word-plan-image-summary.is-invalid { color: #cf1322; background: #fff7f6; border-color: #ffccc7; }
.word-plan-image-summary.is-invalid small { color: #cf1322; }
.word-plan-image-summary > svg:first-child { flex: 0 0 auto; font-size: 17px; }
.word-plan-image-summary > svg:last-child { flex: 0 0 auto; margin-left: auto; color: #94a3b8; }
.word-plan-image-summary span { display: grid; min-width: 0; flex: 1; gap: 1px; }
.word-plan-image-summary strong { color: #334155; font-size: 12px; font-weight: 600; }
.word-plan-image-summary small { overflow: hidden; color: #64748b; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.day-word-plan-layout { display: grid; min-height: 0; flex: 1; grid-template-columns: minmax(460px, 42%) minmax(620px, 58%); }
.word-plan-editor-pane, .word-plan-preview-pane { display: flex; min-width: 0; min-height: 0; flex-direction: column; }
.word-plan-editor-pane { padding: 14px 16px 16px; background: #fff; border-right: 1px solid #dfe4ea; }
.word-plan-preview-pane { padding: 14px 18px 16px; background: #f4f6f8; }
.word-plan-editor-switcher { display: flex; min-height: 40px; flex: 0 0 auto; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 12px; border-bottom: 1px solid #e5e7eb; }
.word-plan-editor-switcher > span { overflow: hidden; color: #64748b; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.word-plan-editor-switcher :deep(.ant-radio-button-wrapper) { min-width: 92px; text-align: center; }
.word-plan-pane-heading { display: flex; min-height: 40px; flex: 0 0 auto; align-items: center; justify-content: space-between; gap: 8px; padding: 0 2px 10px; border-bottom: 1px solid #dbe4ee; }
.word-plan-pane-heading > div { min-width: 0; }
.word-plan-pane-heading > div strong, .word-plan-pane-heading > div span { display: block; }
.word-plan-pane-heading strong { color: #1e293b; font-size: 14px; font-weight: 650; }
.word-plan-pane-heading > div span { margin-top: 3px; color: #64748b; font-size: 12px; line-height: 1.45; }
.word-plan-preview-actions { display: flex; align-items: center; gap: 8px; }
.word-plan-source-scroll, .word-plan-selected-list { min-height: 0; flex: 1; padding: 0 4px 12px 0; overflow: auto; background: #fff; scrollbar-color: #cbd5e1 transparent; scrollbar-width: thin; }
.word-plan-source-scroll { padding-top: 2px; }
.word-plan-resource-group { padding: 16px 4px; border-bottom: 1px solid #e8eef5; }
.word-plan-resource-group:last-child { border-bottom: 0; }
.word-plan-resource-group > header { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 0 7px 10px; }
.word-plan-resource-group > header :deep(.ant-tag) { flex: 0 0 auto; margin-inline-end: 0; font-size: 11px; }
.word-plan-resource-group > header strong { overflow: hidden; color: #1e293b; font-size: 14px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.word-plan-candidate-section { padding: 0 2px; }
.word-plan-optional-section { padding-top: 10px; margin-top: 8px; border-top: 1px dashed #e1e7ee; }
.word-plan-candidate-title { margin: 0 5px 4px; color: #64748b; font-size: 12px; font-weight: 600; }
.word-plan-candidate-list { display: grid; gap: 3px; }
.word-plan-candidate-list :deep(.ant-checkbox-wrapper) { display: flex; min-width: 0; align-items: flex-start; padding: 7px; border: 1px solid transparent; border-radius: 5px; }
.word-plan-candidate-list :deep(.ant-checkbox-wrapper:hover) { border-color: #d6e7ff; background: #f6faff; }
.word-plan-candidate-list :deep(.ant-checkbox + span) { min-width: 0; flex: 1; }
.word-plan-candidate-copy { display: flex; min-width: 0; align-items: baseline; gap: 7px; }
.word-plan-candidate-name { min-width: 0; overflow: hidden; color: #334155; font-size: 13px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.word-plan-candidate-meta { flex: 0 0 auto; color: #8c8c8c; font-size: 11px; }
.word-plan-candidate-empty { padding: 5px 7px; color: #94a3b8; font-size: 12px; }
.word-plan-selected-list { display: block; padding-top: 12px; }
.word-plan-selected-group { margin-bottom: 12px; overflow: hidden; border: 1px solid #dfe5ec; border-radius: 7px; background: #fff; }
.word-plan-selected-group:last-child { margin-bottom: 0; }
.word-plan-selected-group-heading { display: flex; min-height: 46px; align-items: center; gap: 8px; padding: 7px 9px; background: #f7f9fb; border-bottom: 1px solid #e5eaf0; }
.word-plan-selected-group-heading :deep(.ant-tag) { flex: 0 0 auto; margin-inline-end: 0; }
.word-plan-group-title { display: grid; min-width: 0; flex: 1; gap: 1px; }
.word-plan-group-title strong { overflow: hidden; color: #334155; font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.word-plan-group-title span { color: #7b8794; font-size: 11px; }
.word-plan-group-drag-handle, .material-drag-handle { display: grid; width: 26px; height: 28px; flex: 0 0 auto; place-items: center; padding: 0; color: #94a3b8; background: transparent; border: 0; border-radius: 4px; cursor: grab; }
.word-plan-group-drag-handle:hover, .material-drag-handle:hover { color: #1677ff; background: #eaf3ff; }
.word-plan-group-drag-handle:active, .material-drag-handle:active { cursor: grabbing; }
.word-plan-selected-group-list { padding: 3px 9px; }
.word-plan-selected-row { display: flex; min-height: 48px; align-items: center; gap: 7px; padding: 6px 2px; border-bottom: 1px solid #edf0f3; background: #fff; transition: background .16s ease; }
.word-plan-selected-row:last-child { border-bottom: 0; }
.word-plan-selected-row:hover { background: #f8fbff; }
.word-plan-selected-main { min-width: 0; flex: 1; }
.word-plan-selected-line { display: flex; min-width: 0; align-items: center; gap: 7px; }
.word-plan-selected-line :deep(.ant-tag) { flex: 0 0 auto; margin-inline-end: 0; font-size: 11px; line-height: 20px; }
.word-plan-selected-main strong { display: block; overflow: hidden; color: #334155; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.word-plan-sale-price { display: block; margin-top: 2px; color: #64748b; font-size: 11px; line-height: 18px; }
.word-plan-selected-row > :deep(.ant-btn) { flex: 0 0 auto; color: #94a3b8; }
.word-plan-preview-scroll { min-height: 0; flex: 1; margin-top: 0; border-color: #dbe4ee; border-radius: 6px; background: #e9edf2; }
.word-plan-preview-scroll.word-pdf-preview-scroll { margin-top: 0; }
.word-plan-preview-scroll :deep(.ant-spin-nested-loading), .word-plan-preview-scroll :deep(.ant-spin-container) { height: 100%; min-height: 0; }
.word-plan-preview-pane .word-pdf-preview-frame { height: 100%; min-height: 100%; }
.word-plan-image-settings-modal { display: grid; gap: 16px; }
.word-plan-image-mode-row, .word-plan-day-end-setting { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding-bottom: 16px; border-bottom: 1px solid #edf0f3; }
.word-plan-image-mode-row > div, .word-plan-day-end-setting > div, .word-plan-image-resource-row > div { min-width: 0; }
.word-plan-image-mode-row strong, .word-plan-image-mode-row span, .word-plan-day-end-setting strong, .word-plan-day-end-setting span, .word-plan-image-resource-row strong, .word-plan-image-resource-row span { display: block; }
.word-plan-image-mode-row strong, .word-plan-day-end-setting strong, .word-plan-image-resource-row strong { color: #334155; font-size: 13px; font-weight: 650; }
.word-plan-image-mode-row span, .word-plan-day-end-setting span, .word-plan-image-resource-row span { margin-top: 3px; color: #64748b; font-size: 12px; }
.word-plan-image-resource-settings { display: grid; max-height: 430px; overflow: auto; border-top: 1px solid #edf0f3; }
.word-plan-image-resource-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 12px 2px; border-bottom: 1px solid #edf0f3; }
.word-plan-image-resource-row :deep(.ant-tag) { margin-inline-end: 0; }
.word-plan-image-settings-actions { display: flex; justify-content: flex-end; padding-top: 4px; }
.word-image-picker-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 0 14px; margin-bottom: 2px; border-bottom: 1px solid #e8eef5; }
.word-image-picker-toolbar strong, .word-image-picker-toolbar span { display: block; }
.word-image-picker-toolbar strong { color: #1f2937; font-size: 14px; }
.word-image-picker-toolbar span { margin-top: 4px; color: #64748b; font-size: 12px; }
.word-image-selection-warning { padding: 8px 10px; margin: -4px 0 12px; color: #cf1322; font-size: 12px; line-height: 1.6; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; }
.word-image-section-label { margin: 14px 0 8px; color: #334155; font-size: 13px; font-weight: 650; }
.word-image-selected-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 8px; min-height: 84px; padding: 10px; border: 1px dashed #b9d7ff; border-radius: 8px; background: #f6faff; }
.word-image-selected-list .word-image-section-label { grid-column: 1 / -1; margin: 0; }
.word-image-selected-item { position: relative; display: flex; min-width: 0; align-items: center; gap: 7px; padding: 6px 7px; border: 1px solid #d6e7ff; border-radius: 6px; background: #fff; cursor: grab; }
.word-image-selected-item:active { cursor: grabbing; }
.word-image-selected-item img, .word-image-fallback { width: 42px; height: 42px; flex: 0 0 auto; border-radius: 4px; object-fit: cover; }
.word-image-fallback { display: grid; place-items: center; color: #94a3b8; background: #eef2f6; }
.word-image-order { position: absolute; top: 3px; left: 3px; display: grid; width: 16px; height: 16px; place-items: center; border-radius: 50%; background: #1677ff; color: #fff; font-size: 10px; }
.word-image-selected-copy { display: grid; min-width: 0; gap: 2px; }
.word-image-resource-name { overflow: hidden; color: #64748b; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.word-image-filename { min-width: 0; overflow: hidden; color: #475569; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.word-image-drag-icon { flex: 0 0 auto; color: #94a3b8; }
.word-image-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; max-height: 380px; padding: 2px; overflow: auto; }
.word-image-resource-list { display: grid; gap: 0; max-height: 460px; overflow: auto; border-top: 1px solid #eef2f7; }
.word-image-resource-group { padding: 14px 2px; border-bottom: 1px solid #eef2f7; }
.word-image-resource-group:last-child { border-bottom: 0; }
.word-image-resource-group > header { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; margin-bottom: 9px; }
.word-image-resource-group > header strong { overflow: hidden; color: #334155; font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.word-image-resource-group > header span { flex: 0 0 auto; color: #8c8c8c; font-size: 12px; }
.word-image-resource-group .word-image-grid { max-height: none; padding: 0; overflow: visible; }
.word-image-resource-empty { margin: 4px 0; }
.word-image-card { position: relative; display: grid; grid-template-rows: 112px auto; gap: 6px; padding: 7px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; cursor: pointer; transition: border-color .16s ease, box-shadow .16s ease, background .16s ease; }
.word-image-card:hover, .word-image-card.is-selected { border-color: #91caff; background: #f6faff; box-shadow: 0 2px 8px rgb(22 119 255 / 10%); }
.word-image-card > :deep(.ant-checkbox-wrapper) { position: absolute; top: 8px; left: 8px; z-index: 1; }
.word-image-card img, .word-image-card-placeholder { width: 100%; height: 112px; border-radius: 5px; object-fit: cover; }
.word-image-card-placeholder { display: grid; place-items: center; gap: 2px; color: #94a3b8; background: #eef2f6; font-size: 12px; }
.word-image-card-placeholder span { font-size: 11px; }
.word-image-card-name { overflow: hidden; color: #475569; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.intro-preview-modal h3 { margin: 10px 0; }
.quote-cost { display: flex; justify-content: space-between; padding: 12px 0; margin: 12px 0; border-bottom: 1px solid #f0f0f0; }
.quote-cost strong { color: #1677ff; font-size: 18px; }
.quote-form { margin-top: 14px; }
.latest-document-card { margin-top: 12px; }
.latest-document-card :deep(.ant-card-body) { padding-block: 12px; }
.latest-document-row { display: flex; min-width: 0; align-items: center; gap: 12px; }
.latest-document-icon { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 6px; background: #eaf3ff; color: #1677ff; font-size: 18px; }
.latest-document-empty { display: flex; min-height: 40px; align-items: center; justify-content: space-between; gap: 12px; color: #64748b; }
.document-list { display: grid; gap: 6px; }
.document-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.document-row:last-child { border-bottom: 0; }
.document-main { min-width: 0; flex: 1; }
.document-main strong, .document-main span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.document-main span, .image-meta { color: #64748b; font-size: 12px; }
.product-document-preview-drawer :deep(.ant-drawer-body) { padding: 12px; background: #eef2f6; }
.product-document-preview-drawer :deep(.ant-spin-nested-loading), .product-document-preview-drawer :deep(.ant-spin-container) { min-height: calc(100vh - 150px); }
.document-preview-frame { display: block; width: 100%; min-height: calc(100vh - 150px); border: 0; border-radius: 4px; background: #fff; }
@media (max-width: 1320px) {
  .designer-grid { grid-template-columns: minmax(0, 1fr); }
  .plan-panel-cell { position: static; grid-column: 1 / -1; }
  .plan-panel { position: relative; inset: auto; height: auto; }
  .plan-panel :deep(.ant-card-body) { flex: initial; max-height: none; }
  .drop-zone { min-height: 180px; }
  .plan-empty { min-height: 160px; }
  .day-word-plan-layout { grid-template-columns: minmax(390px, 44%) minmax(0, 56%); }
}
@media (max-width: 900px) {
  .designer-shell { --designer-map-height: 430px; }
  .resource-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .material-editor-layout { grid-template-columns: 1fr; }
  .material-preview-pane { position: static; }
  .word-preview-scroll { max-height: 620px; min-height: 420px; }
  .word-pdf-preview-scroll { min-height: 420px; }
  .word-pdf-preview-scroll :deep(.ant-spin-nested-loading), .word-pdf-preview-scroll :deep(.ant-spin-container) { min-height: 400px; }
  .word-pdf-preview-frame { min-height: 620px; }
  .optional-item-form { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .word-plan-workspace { overflow: auto; }
  .word-plan-toolbar { align-items: flex-start; flex-wrap: wrap; }
  .word-plan-toolbar-actions { width: 100%; justify-content: space-between; }
  .day-word-plan-layout { display: flex; flex: initial; flex-direction: column; }
  .word-plan-editor-pane { min-height: 560px; border-right: 0; border-bottom: 1px solid #dfe4ea; }
  .word-plan-preview-pane { min-height: 680px; }
  .word-plan-image-mode-row, .word-plan-day-end-setting, .word-image-picker-toolbar { align-items: flex-start; flex-direction: column; }
  .word-image-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .designer-shell { --designer-map-height: 380px; }
  .designer-summary { flex-wrap: wrap; padding: 4px 12px; }
  .summary-item { min-width: calc(50% - 12px); padding: 8px 12px 8px 0; margin-right: 12px; border-right: 0; }
  .summary-location { margin-left: 0; text-align: left; }
  .day-switcher { align-items: stretch; flex-direction: column; gap: 8px; }
  .day-switcher-label { display: flex; align-items: center; justify-content: space-between; padding-right: 0; padding-bottom: 6px; border-right: 0; border-bottom: 1px solid #f0f0f0; }
  .day-destination-editor { min-width: 0; padding-top: 8px; padding-left: 0; border-top: 1px solid #f0f0f0; border-left: 0; }
  .day-destination-editor-control :deep(.ant-input-affix-wrapper) { width: 100%; }
  .designer-grid { display: block; }
  .map-panel, .plan-panel-cell { margin-bottom: 12px; }
  .plan-panel { position: static; }
  .resource-filters { grid-template-columns: 1fr; }
  .resource-filters > :last-child { grid-column: auto; }
  .resource-list { max-height: 220px; }
  .detail-config-grid { grid-template-columns: 1fr; }
  .optional-item-form { grid-template-columns: 1fr; }
  .word-preview-sheet { min-height: 0; padding: 28px 24px; }
  .word-plan-toolbar-actions { align-items: stretch; flex-direction: column; }
  .word-plan-image-summary { width: 100%; }
  .word-plan-editor-switcher { align-items: flex-start; flex-direction: column; }
  .word-plan-editor-switcher :deep(.ant-radio-group) { display: flex; width: 100%; }
  .word-plan-editor-switcher :deep(.ant-radio-button-wrapper) { flex: 1; }
  .word-plan-image-resource-row { align-items: flex-start; flex-direction: column; }
  .word-image-selected-list { grid-template-columns: 1fr; }
  .word-image-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .latest-document-row { align-items: flex-start; flex-wrap: wrap; }
  .latest-document-row .document-main { min-width: calc(100% - 48px); }
  .latest-document-row > :deep(.ant-space) { width: 100%; padding-left: 48px; }
}
</style>
