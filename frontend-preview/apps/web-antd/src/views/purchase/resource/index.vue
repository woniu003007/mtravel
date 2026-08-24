<script lang="ts" setup>
import type { TablePaginationConfig, UploadProps } from 'ant-design-vue';
import type { Dayjs } from 'dayjs';
import type { Sortable } from '@vben/hooks';

import { Page } from '@vben/common-ui';
import { useSortable } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';

import {
  AutoComplete,
  Button,
  Card,
  Cascader,
  Checkbox,
  DatePicker,
  Drawer,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tabs,
  Tag,
  Textarea,
  TimePicker,
  Upload,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue';
import { useRoute } from 'vue-router';

import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import { uploadAttachment, type AttachmentApi } from '#/api/common/attachment';
import { getExpenseItemAll, type EnterpriseExpenseItemApi } from '#/api/enterprise/expense-item';
import { getProductDictionaryAll } from '#/api/enterprise/product-dictionary';
import { createPurchaseRelation, deletePurchaseRelation } from '#/api/purchase/relation';
import type { PurchaseRelationApi } from '#/api/purchase/relation';
import {
  deleteRelationTicketTemplate,
  getRelationTicketTemplateDetail,
  getRelationTicketTemplateFillModes,
  getRelationTicketTemplateHeaders,
  getRelationTicketTemplateSystemFields,
  saveRelationTicketTemplate,
  type RelationTicketTemplateApi,
} from '#/api/purchase/relation-ticket-template';
import {
  createPurchaseResource,
  createPurchaseResourceIntroduction,
  createResourceSupplierForResource,
  deletePurchaseResource,
  deletePurchaseResourceDocument,
  deletePurchaseResourceImage,
  deletePurchaseResourceIntroduction,
  disablePurchaseResourceDocument,
  downloadPurchaseResourceImage,
  getCommonAmapJsConfig,
  getPurchaseResourceBindings,
  getPurchaseResourceDetail,
  getPurchaseResourceDocuments,
  getPurchaseResourceImages,
  getPurchaseResourceIntroductionImages,
  getPurchaseResourceIntroductions,
  getPurchaseResourceOptionalItems,
  getPurchaseResourcePage,
  publishPurchaseResourceDocument,
  publishPurchaseResourceIntroduction,
  retryPurchaseResourceDocument,
  retryPurchaseResourceIntroduction,
  reorderPurchaseResourceIntroductions,
  reverseGeocodeCommonAmap,
  downloadPurchaseResourceDocument,
  searchCommonAmapTips,
  setPurchaseResourceImageCover,
  savePurchaseResourceIntroductionImages,
  updatePurchaseResourceImage,
  updatePurchaseResourceIntroduction,
  uploadPurchaseResourceImages,
  uploadPurchaseResourceDocuments,
  updateResourceSupplierForResource,
  updatePurchaseResource,
} from '#/api/purchase/resource';
import type { PurchaseResourceApi } from '#/api/purchase/resource';
import {
  getSupplierAll,
  getSupplierDetail,
} from '#/api/purchase/supplier';
import type { SupplierApi } from '#/api/purchase/supplier';
import {
  buildRegionOptions,
  buildRegionPath,
  splitRegionPath,
} from '#/utils/region';
import type { RegionOption, RegionPath } from '#/utils/region';

type ResourceRow = PurchaseResourceApi.Item;
type BindingRow = PurchaseResourceApi.Binding;
type TemplateFieldRow = RelationTicketTemplateApi.Field;
type ResourceType = PurchaseResourceApi.ResourceType;

interface MapTipOption {
  label: string;
  meta: PurchaseResourceApi.AmapTip;
  value: string;
}

interface QueryLocationOption {
  label: string;
  path: RegionPath;
  value: string;
}

const resourceTypeOptions = [
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '餐厅', value: 'restaurant' },
  { label: '购物', value: 'shopping' },
  { label: '用车', value: 'vehicle' },
  { label: '大交通', value: 'traffic' },
  { label: '地接', value: 'ground_agent' },
  { label: '其它资源', value: 'other' },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const procurementModeOptions = [
  { label: '无需采购', value: 'not_required' },
  { label: '需要采购', value: 'required' },
];

const scenicLevelOptions = [
  { label: '未评级', value: 'unrated' },
  { label: '1A', value: '1a' },
  { label: '2A', value: '2a' },
  { label: '3A', value: '3a' },
  { label: '4A', value: '4a' },
  { label: '5A', value: '5a' },
];

const businessStatusOptions = [
  { label: '未维护', value: 'unmaintained' },
  { label: '营业中', value: 'open' },
  { label: '暂停营业', value: 'suspended' },
  { label: '已停业', value: 'closed' },
];

const siteVisitStatusOptions = [
  { label: '未维护', value: 'unmaintained' },
  { label: '未踩点', value: 'not_visited' },
  { label: '已踩点', value: 'visited' },
];

const starLevelOptions = ref<{ label: string; value: string }[]>([]);

const billingModeOptions = [
  { label: '按天计费', value: 'daily' },
  { label: '按趟次/行程计费', value: 'trip' },
  { label: '按公里数+时间计费', value: 'distance_time' },
];

const restaurantCategoryOptions = ['鲁菜', '川菜', '粤菜', '苏菜', '闽菜', '浙菜', '湘菜', '徽菜'].map((value) => ({ label: value, value }));
const shoppingCategoryOptions = ['乳胶', '茶叶', '翡翠', '厨具', '黄金饰品', '丝绸', '珍珠', '土特产'].map((value) => ({ label: value, value }));
const trafficCategoryOptions = ['高铁', '飞机', '普通火车', '轮船', '票务服务'].map((value) => ({ label: value, value }));
const otherCategoryOptions = ['保险', '会务场地', '设备租赁', '专业服务', '旅游物料', '礼品特产', '其他'].map((value) => ({ label: value, value }));

const regionOptions = buildRegionOptions();
const route = useRoute();

const baseColumns = [
  {
    dataIndex: 'resourceType',
    key: 'resourceType',
    title: '资源类型',
    width: 100,
  },
  {
    dataIndex: 'procurementMode',
    key: 'procurementMode',
    title: '采购属性',
    width: 120,
  },
  { key: 'area', title: '所在地', width: 170 },
  {
    dataIndex: 'resourceName',
    key: 'resourceName',
    title: '资源名称',
    width: 260,
  },
  { dataIndex: 'phone', key: 'phone', title: '电话', width: 140 },
  { dataIndex: 'fax', key: 'fax', title: '传真', width: 130 },
  {
    dataIndex: 'boundSupplierCount',
    key: 'boundSupplierCount',
    title: '已绑定',
    width: 100,
  },
  { dataIndex: 'createdBy', key: 'createdBy', title: '登记人', width: 110 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间', width: 170 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 250 },
];

const vehicleColumns = [
  ...baseColumns.slice(0, 2),
  ...baseColumns.slice(3, 4),
  { dataIndex: 'seatCount', key: 'seatCount', title: '座位数', width: 100 },
  { dataIndex: 'vehicleType', key: 'vehicleType', title: '车型', width: 140 },
  { dataIndex: 'billingMode', key: 'billingMode', title: '计费模式', width: 130 },
  ...baseColumns.slice(6),
];

const scenicColumns = [
  {
    dataIndex: 'scenicLevel',
    key: 'scenicLevel',
    title: '国家 A 级',
    width: 100,
  },
  { key: 'mapLocation', title: '地图位置', width: 240 },
  { key: 'businessInfo', title: '营业情况', width: 150 },
  { key: 'siteVisitInfo', title: '踩点情况', width: 180 },
];

const data = ref<ResourceRow[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();
const bindingDrawerOpen = ref(false);
const supplierBindingMode = ref<'bind' | 'create'>('bind');
const documentDrawerOpen = ref(false);
const materialTab = ref<'introductions' | 'images' | 'files'>('introductions');
const materialServiceUnavailable = ref(false);
const templateDrawerOpen = ref(false);
const bindingLoading = ref(false);
const documentLoading = ref(false);
const introductionLoading = ref(false);
const imageLoading = ref(false);
const templateLoading = ref(false);
const templateSaving = ref(false);
const templateHeaderLoading = ref(false);
const uploadingDocuments = ref(false);
const uploadingImages = ref(false);
const introductionSaving = ref(false);
const introductionPublishing = ref(false);
const introductionImageSelectionLoading = ref(false);
const optionalItemMasterLoading = ref(false);
const imageEditorOpen = ref(false);
const imageSaving = ref(false);
const previewOpen = ref(false);
const previewUrl = ref('');
const currentResource = ref<ResourceRow>();
const currentBinding = ref<BindingRow>();
const currentDocument = ref<PurchaseResourceApi.ResourceDocumentItem>();
const currentImage = ref<PurchaseResourceApi.ResourceImageItem>();
const bindings = ref<PurchaseResourceApi.Binding[]>([]);
const documents = ref<PurchaseResourceApi.ResourceDocumentItem[]>([]);
const introductions = ref<PurchaseResourceApi.ResourceIntroductionItem[]>([]);
const images = ref<PurchaseResourceApi.ResourceImageItem[]>([]);
const activeIntroductionId = ref<number>();
const introductionSortableContainerRef = ref<HTMLElement>();
const introductionReordering = ref(false);
const introductionImageIds = ref<number[]>([]);
const introductionImageLoadedIntroductionId = ref<number>();
const introductionImagePickerOpen = ref(false);
const introductionLoadedResourceId = ref<number>();
const imageLoadedResourceId = ref<number>();
const documentLoadedResourceId = ref<number>();
const imagePreviewUrls = reactive<Record<number, string>>({});
const supplierOptions = ref<{ label: string; value: number }[]>([]);
/** 快速绑定框的搜索词，支持键盘输入名称直接定位已有供应商。 */
const supplierSearchKeyword = ref('');
const resourcePriceProjects = ref<EnterpriseExpenseItemApi.Item[]>([]);
const resourceOptionalItems = ref<PurchaseResourceApi.ResourceOptionalItem[]>([]);
const resourceSupplierPriceValues = reactive<Record<number, number | undefined>>({});
const systemFieldOptions = ref<{ label: string; value: string }[]>([]);
const fillModeOptions = ref<{ label: string; value: string }[]>([]);
const creatingScenicSupplier = ref(false);
const editingScenicSupplierId = ref<number>();
const editingScenicSupplierRelationId = ref<number>();
const routeBindingOpened = ref(false);
const routeTemplateOpened = ref(false);
const queryRegionPath = ref<RegionPath>([]);
const queryLocationKeyword = ref('');
const formRegionPath = ref<RegionPath>([]);
const scenicSupplierRegionPath = ref<RegionPath>([]);
const formCategoryTags = ref<string[]>([]);
const scenicMapKeyword = ref<string>();
const scenicMapTipOptions = ref<MapTipOption[]>([]);
const scenicMapTipLoading = ref(false);
const scenicMapLoading = ref(false);
const scenicMapReady = ref(false);
const scenicMapError = ref('');
const scenicMapContainerRef = ref<HTMLDivElement>();

let amapLoaderPromise: Promise<any> | undefined;
let scenicMapInstance: any;
let scenicMapMarker: any;
let scenicMapSearchTimer: number | undefined;
let scenicMapAutoLocateTimer: number | undefined;
let introductionSortableInstance: Sortable | undefined;
let introductionSortableVersion = 0;

const query = reactive<PurchaseResourceApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const queryLocationEntries = buildQueryLocationEntries(regionOptions);
const queryLocationOptions = computed(() => {
  const keyword = queryLocationKeyword.value.trim();
  if (!keyword) return queryLocationEntries.slice(0, 80);
  return queryLocationEntries
    .filter((item) => item.label.includes(keyword) || item.path.some((part) => part.includes(keyword)))
    .slice(0, 80);
});

const formState = reactive<PurchaseResourceApi.SaveParams>({
  autoCreateSupplier: false,
  businessStatus: 'unmaintained',
  procurementMode: 'required',
  resourceName: '',
  resourceType: 'scenic',
  scenicLevel: 'unrated',
  siteVisitStatus: 'unmaintained',
  status: 'active',
});

const bindForm = reactive<PurchaseRelationApi.SaveParams>({
  isDefault: false,
  resourceId: undefined as unknown as number,
  status: 'active',
  supplierId: undefined as unknown as number,
});

const scenicSupplierForm = reactive<PurchaseResourceApi.ResourceSupplierCreateParams>({
  isDefault: false,
  priceMode: 'unified',
  status: 'active',
  supplierName: '',
});
const templateStatusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];
const templateColumns = [
  { dataIndex: 'columnIndex', key: 'columnIndex', title: '列号', width: 80 },
  { dataIndex: 'templateHeader', key: 'templateHeader', title: '模板表头', width: 200 },
  { dataIndex: 'fillMode', key: 'fillMode', title: '填充方式', width: 150 },
  { dataIndex: 'systemField', key: 'systemField', title: '系统字段/固定值', width: 260 },
  { dataIndex: 'required', key: 'required', title: '必填', width: 90 },
];
const templateForm = reactive<RelationTicketTemplateApi.SaveParams>({
  attachmentId: undefined as unknown as number,
  dataStartRow: 2,
  fields: [],
  headerRow: 1,
  relationId: undefined as unknown as number,
  status: 'active',
  templateName: '',
});
const introductionForm = reactive<PurchaseResourceApi.ResourceIntroductionSaveParams>({
  content: '',
  extensionBlocks: [],
  isOptionalItem: false,
  noticeContent: '',
  tags: [],
  title: '',
  visitDuration: '',
  warmTipContent: '',
});
const imageEditForm = reactive<PurchaseResourceApi.ResourceImageUpdateParams>({
  sortOrder: 0,
  tags: [],
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const previewTitle = computed(
  () => currentResource.value?.resourceName || '资源图片',
);
const activeIntroduction = computed(() =>
  introductions.value.find((item) => item.id === activeIntroductionId.value),
);
/** 保持当前介绍素材内的图片排序，不把资源图库中未选择的图片显示成已归属。 */
const selectedIntroductionImages = computed(() =>
  introductionImageIds.value.flatMap((id) => {
    const image = images.value.find((item) => item.id === id);
    return image ? [image] : [];
  }),
);
const isEditingScenicSupplier = computed(() => Boolean(editingScenicSupplierId.value));
const resourceOptionalItemOptions = computed(() =>
  resourceOptionalItems.value
    .filter((item) => item.status === 'active')
    .map((item) => ({
      label: `${item.projectName}（${optionalItemTypeLabel(item.optionalItemType)}）`,
      value: item.id,
    })),
);
const isScenicList = computed(() => query.resourceType === 'scenic');
const isVehicleList = computed(() => query.resourceType === 'vehicle');
const isPlaceList = computed(() => Boolean(query.resourceType && isPlaceResource(query.resourceType)));
const isScenicForm = computed(() => formState.resourceType === 'scenic');
const isPlaceForm = computed(() => isPlaceResource(formState.resourceType));
const currentResourceTypeLabel = computed(() => typeLabel(currentResource.value?.resourceType));
const isVehicleResourceBinding = computed(() => currentResource.value?.resourceType === 'vehicle');
const unifiedPriceLabel = computed(() => currentResource.value?.resourceType === 'scenic'
  ? '门票统一报价（无门票可留空）'
  : `${currentResourceTypeLabel.value}统一报价`);
const supplierFormTitle = computed(() => `${isEditingScenicSupplier.value ? '编辑' : '新增'}${currentResourceTypeLabel.value}供应商`);
const supplierCreateTabTitle = computed(() => {
  if (isEditingScenicSupplier.value) {
    return supplierFormTitle.value;
  }
  return isVehicleResourceBinding.value
    ? '新增并绑定车队'
    : `新增并绑定${currentResourceTypeLabel.value}供应商`;
});
const unifiedPriceUnit = computed(() => {
  const resource = currentResource.value;
  if (!resource) return '元';
  if (resource.resourceType === 'vehicle') {
    const vehicleUnits = {
      daily: '元/天',
      distance_time: '元/公里',
      trip: '元/趟',
    } as const;
    return resource.billingMode ? vehicleUnits[resource.billingMode] : '元/趟';
  }
  if (resource.resourceType === 'hotel') return '元/间夜';
  if (resource.resourceType === 'ground_agent') return '元/团';
  if (resource.resourceType === 'other') return resource.resourceUnit || '元/项';
  return '元/人';
});
const columns = computed(() => {
  if (isVehicleList.value) {
    return vehicleColumns;
  }
  if (!isPlaceList.value) {
    return baseColumns;
  }
  return [
    ...baseColumns.slice(0, 3),
    ...scenicColumns,
    ...baseColumns.slice(3),
  ];
});

function typeLabel(value?: string) {
  return (
    resourceTypeOptions.find((item) => item.value === value)?.label ||
    value ||
    '-'
  );
}

function typeColor(value?: string) {
  const colors: Record<string, string> = {
    ground_agent: 'cyan',
    hotel: 'purple',
    other: 'default',
    restaurant: 'orange',
    scenic: 'blue',
    shopping: 'green',
    traffic: 'geekblue',
    vehicle: 'volcano',
  };
  return colors[value || ''] || 'default';
}

function billingModeLabel(value?: string) {
  return billingModeOptions.find((item) => item.value === value)?.label || '-';
}

function optionalItemTypeLabel(value?: string) {
  return value === 'scenic_transport' ? '景区小交通' : '推荐自费';
}

function statusLabel(value?: string) {
  return (
    statusOptions.find((item) => item.value === value)?.label || value || '-'
  );
}

function statusColor(value?: string) {
  return value === 'active' ? 'green' : 'default';
}

function procurementModeLabel(value?: string) {
  return procurementModeOptions.find((item) => item.value === value)?.label || '需要采购';
}

function procurementModeColor(value?: string) {
  return value === 'not_required' ? 'green' : 'blue';
}

function scenicLevelLabel(value?: string) {
  return (
    scenicLevelOptions.find((item) => item.value === value)?.label || '未评级'
  );
}

function isPlaceResource(resourceType?: string) {
  return ['hotel', 'restaurant', 'scenic', 'shopping'].includes(resourceType || '');
}

function supportsStarLevel(resourceType?: string) {
  return ['hotel', 'restaurant'].includes(resourceType || '');
}


function supportsCategoryTags(resourceType?: string) {
  return ['other', 'restaurant', 'shopping', 'traffic'].includes(resourceType || '');
}

function categoryOptions(resourceType?: string) {
  if (resourceType === 'restaurant') return restaurantCategoryOptions;
  if (resourceType === 'shopping') return shoppingCategoryOptions;
  if (resourceType === 'traffic') return trafficCategoryOptions;
  if (resourceType === 'other') return otherCategoryOptions;
  return [];
}

function businessStatusLabel(value?: string) {
  return (
    businessStatusOptions.find((item) => item.value === value)?.label ||
    '未维护'
  );
}

function businessStatusColor(value?: string) {
  const colors: Record<string, string> = {
    closed: 'default',
    open: 'green',
    suspended: 'orange',
    unmaintained: 'default',
  };
  return colors[value || ''] || 'default';
}

function siteVisitStatusLabel(value?: string) {
  return (
    siteVisitStatusOptions.find((item) => item.value === value)?.label ||
    '未维护'
  );
}

function siteVisitStatusColor(value?: string) {
  return value === 'visited'
    ? 'green'
    : value === 'not_visited'
      ? 'orange'
      : 'default';
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function formatFileSize(value?: number) {
  if (!value || value <= 0) {
    return '-';
  }
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function documentStatusColor(value?: string) {
  const colors: Record<string, string> = {
    deleted: 'default',
    failed: 'red',
    indexed: 'green',
    pending: 'gold',
    processing: 'blue',
    succeeded: 'green',
  };
  return colors[value || ''] || 'default';
}

function processingStatusLabel(value?: string) {
  const labels: Record<string, string> = {
    deleted: '已删除',
    failed: '处理失败',
    pending: '待处理',
    processing: '处理中',
    succeeded: '已处理',
  };
  return labels[value || ''] || value || '-';
}

function indexStatusLabel(value?: string) {
  const labels: Record<string, string> = {
    deleted: '已删除',
    failed: '向量失败',
    indexed: '已入库',
    pending: '待向量化',
  };
  return labels[value || ''] || value || '-';
}

function reviewStatusColor(value?: string) {
  const colors: Record<string, string> = {
    disabled: 'default',
    draft: 'gold',
    published: 'green',
  };
  return colors[value || ''] || 'default';
}

function reviewStatusLabel(value?: string) {
  const labels: Record<string, string> = {
    disabled: '已停用',
    draft: '草稿',
    published: '已发布',
  };
  return labels[value || ''] || value || '-';
}

function clean(value?: string) {
  return value?.trim() || undefined;
}

/** 将三级行政区拍平成可检索项，查询时允许停在任一级。 */
function buildQueryLocationEntries(options: RegionOption[]) {
  const entries: QueryLocationOption[] = [];
  options.forEach((province) => {
    entries.push({ label: province.label, path: [province.value], value: province.label });
    province.children?.forEach((city) => {
      const cityPath = [province.value, city.value];
      entries.push({ label: cityPath.join(' / '), path: cityPath, value: cityPath.join(' / ') });
      city.children?.forEach((district) => {
        const districtPath = [...cityPath, district.value];
        entries.push({ label: districtPath.join(' / '), path: districtPath, value: districtPath.join(' / ') });
      });
    });
  });
  return entries;
}

function setQueryLocation(entry: QueryLocationOption) {
  queryRegionPath.value = [...entry.path];
  queryLocationKeyword.value = entry.label;
}

function syncQueryLocationKeyword() {
  const keyword = clean(queryLocationKeyword.value);
  if (!keyword) {
    queryRegionPath.value = [];
    return true;
  }
  const currentPath = queryRegionPath.value;
  const currentEntry = queryLocationEntries.find((entry) => entry.path.join('|') === currentPath.join('|'));
  if (currentEntry?.label === keyword) return true;
  const matches = queryLocationEntries.filter(
    (entry) => entry.label === keyword || entry.path.at(-1) === keyword,
  );
  if (matches.length !== 1) {
    message.warning(matches.length ? '存在同名区县，请从下拉候选中选择完整所在地' : '请选择省、市或区县所在地');
    return false;
  }
  setQueryLocation(matches[0]!);
  return true;
}

function selectQueryLocation(value: unknown, option: unknown) {
  const selected = option as QueryLocationOption | undefined;
  if (selected?.path) {
    setQueryLocation(selected);
    return;
  }
  const label = typeof value === 'object' && value && 'value' in value
    ? String((value as { value: unknown }).value)
    : String(value || '');
  const fallback = queryLocationEntries.find((entry) => entry.value === label);
  if (fallback) setQueryLocation(fallback);
}

function applyQueryLocationKeyword() {
  if (syncQueryLocationKeyword()) handleSearch();
}

function isNil(value: unknown): value is null | undefined {
  return value === null || value === undefined;
}

function coordinateText(record: Record<string, any>) {
  if (isNil(record.longitude) || isNil(record.latitude)) {
    return '未维护坐标';
  }
  return `${Number(record.longitude).toFixed(7)}, ${Number(record.latitude).toFixed(7)}`;
}

function businessTimeText(record: Record<string, any>) {
  if (!record.openingTime || !record.closingTime) {
    return '未维护时间';
  }
  return `${record.openingTime.slice(0, 5)} - ${record.closingTime.slice(0, 5)}`;
}

function disableFutureDate(current: Dayjs) {
  return current.isAfter(dayjs(), 'day');
}

function areaText(record: Record<string, any>) {
  return (
    [record.province, record.city, record.district]
      .filter(Boolean)
      .join(' / ') || '-'
  );
}

function handleSearch() {
  if (!syncQueryLocationKeyword()) return;
  const regionFields = splitRegionPath(queryRegionPath.value);
  query.province = regionFields.province;
  query.city = regionFields.city;
  query.district = regionFields.district;
  query.page = 1;
  loadData();
}

function handleQueryResourceTypeChange(value: unknown) {
  if (value !== 'scenic') query.scenicLevel = undefined;
  if (!isPlaceResource(String(value || ''))) {
    query.businessStatus = undefined;
    query.siteVisitStatus = undefined;
  }
  if (value === 'vehicle') {
    queryRegionPath.value = [];
    queryLocationKeyword.value = '';
    query.province = undefined;
    query.city = undefined;
    query.district = undefined;
  }
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getPurchaseResourcePage(query);
    data.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

async function loadReceptionStandards() {
  try {
    const items = await getProductDictionaryAll('reception_standard');
    starLevelOptions.value = items
      .filter((item) => item.status === 'active')
      .sort((left, right) => left.sortOrder - right.sortOrder)
      .map((item) => ({ label: item.dictName, value: item.dictName }));
  } catch {
    starLevelOptions.value = [];
    message.warning('接待标准字典加载失败，请稍后重试');
  }
}

function resetQuery() {
  queryRegionPath.value = [];
  queryLocationKeyword.value = '';
  query.keyword = undefined;
  query.resourceType = undefined;
  query.province = undefined;
  query.city = undefined;
  query.district = undefined;
  query.status = undefined;
  query.procurementMode = undefined;
  query.scenicLevel = undefined;
  query.businessStatus = undefined;
  query.siteVisitStatus = undefined;
  query.page = 1;
  loadData();
}

function resetForm() {
  editingId.value = undefined;
  formRegionPath.value = [];
  formState.resourceType = 'scenic';
  formState.autoCreateSupplier = false;
  formState.procurementMode = 'required';
  formState.resourceName = '';
  formState.province = undefined;
  formState.city = undefined;
  formState.district = undefined;
  formState.phone = undefined;
  formState.contactName = undefined;
  formState.fax = undefined;
  formState.address = undefined;
  formState.businessStatus = 'unmaintained';
  formState.closingTime = undefined;
  formState.warmTip = undefined;
  formState.introduction = undefined;
  formState.lastSiteVisitDate = undefined;
  formState.latitude = undefined;
  formState.longitude = undefined;
  formState.openingTime = undefined;
  formState.scenicLevel = 'unrated';
  formState.starLevel = undefined;
  formState.categoryTags = undefined;
  formState.siteVisitNote = undefined;
  formState.siteVisitStatus = 'unmaintained';
  formState.capacity = undefined;
  formState.tableCount = undefined;
  formState.mealStandard = undefined;
  formState.vehicleType = undefined;
  formState.seatCount = undefined;
  formState.billingMode = undefined;
  formState.serviceArea = undefined;
  formState.referenceDays = undefined;
  formState.includedItems = undefined;
  formState.excludedItems = undefined;
  formState.resourceUnit = undefined;
  formState.status = 'active';
  formState.remark = undefined;
  scenicMapKeyword.value = undefined;
  scenicMapTipOptions.value = [];
  scenicMapError.value = '';
  formCategoryTags.value = [];
}

function resetScenicSupplierForm(resource?: ResourceRow) {
  editingScenicSupplierId.value = undefined;
  editingScenicSupplierRelationId.value = undefined;
  const location = resource || currentResource.value;
  scenicSupplierRegionPath.value = buildRegionPath(
    location?.province,
    location?.city,
    location?.district,
  );
  scenicSupplierForm.supplierName = '';
  scenicSupplierForm.basicInfo = undefined;
  scenicSupplierForm.contactName = undefined;
  scenicSupplierForm.contactPhone = undefined;
  scenicSupplierForm.status = 'active';
  scenicSupplierForm.isDefault = false;
  scenicSupplierForm.priceMode = location?.resourceType === 'vehicle' ? 'classified' : 'unified';
  scenicSupplierForm.unifiedPrice = undefined;
  scenicSupplierForm.optionalItems = [];
  scenicSupplierForm.priceLines = [];
  scenicSupplierForm.priceRemark = undefined;
  scenicSupplierForm.remark = undefined;
  Object.keys(resourceSupplierPriceValues).forEach((key) => {
    delete resourceSupplierPriceValues[Number(key)];
  });
}

function resetTemplateForm(binding?: BindingRow) {
  currentBinding.value = binding;
  templateForm.relationId = binding?.relationId ?? (undefined as unknown as number);
  templateForm.templateName = binding
    ? `${binding.supplierName || '供应商'}游客名单模板`
    : '';
  templateForm.attachmentId = undefined as unknown as number;
  templateForm.templateFileUrl = undefined;
  templateForm.originalFilename = undefined;
  templateForm.sheetName = undefined;
  templateForm.headerRow = 1;
  templateForm.dataStartRow = 2;
  templateForm.status = 'active';
  templateForm.remark = undefined;
  templateForm.fields = [];
}

async function loadSystemFields() {
  if (systemFieldOptions.value.length > 0 && fillModeOptions.value.length > 0) {
    return;
  }
  const [fields, modes] = await Promise.all([
    getRelationTicketTemplateSystemFields(),
    getRelationTicketTemplateFillModes(),
  ]);
  systemFieldOptions.value = fields.map((item) => ({
    label: item.label,
    value: item.value,
  }));
  fillModeOptions.value = modes.map((item) => ({
    label: item.label,
    value: item.value,
  }));
}

async function openTemplateDrawer(binding: BindingRow) {
  if (!binding.relationId) {
    message.warning('该供应商绑定关系缺少 relationId，无法配置模板');
    return;
  }
  currentBinding.value = binding;
  resetTemplateForm(binding);
  templateDrawerOpen.value = true;
  templateLoading.value = true;
  try {
    await loadSystemFields();
    const detail = await getRelationTicketTemplateDetail(binding.relationId);
    if (detail) {
      templateForm.relationId = detail.relationId;
      templateForm.templateName = detail.templateName;
      templateForm.attachmentId = detail.attachmentId;
      templateForm.templateFileUrl = detail.templateFileUrl;
      templateForm.originalFilename = detail.originalFilename;
      templateForm.sheetName = detail.sheetName;
      templateForm.headerRow = detail.headerRow;
      templateForm.dataStartRow = detail.dataStartRow;
      templateForm.status = detail.status;
      templateForm.remark = detail.remark;
      templateForm.fields = detail.fields || [];
    }
  } finally {
    templateLoading.value = false;
  }
}

async function handleTemplateUpload(file: File) {
  if (!currentResource.value || !currentBinding.value) {
    message.warning('请先选择资源和供应商');
    return false;
  }
  const formData = new FormData();
  formData.append('file', file);
  formData.append('businessModule', '采购管理');
  formData.append('businessType', '游客名单模板');
  formData.append('businessId', String(currentBinding.value.relationId));
  const attachment = (await uploadAttachment(formData)) as AttachmentApi.Attachment;
  templateForm.attachmentId = attachment.id;
  templateForm.templateFileUrl = attachment.fileUrl;
  templateForm.originalFilename = attachment.originalFilename;
  if (!templateForm.templateName) {
    templateForm.templateName = attachment.originalFilename.replace(/\.(xlsx|xls)$/i, '');
  }
  await loadTemplateHeaders();
  return false;
}

async function loadTemplateHeaders() {
  if (!templateForm.attachmentId) {
    message.warning('请先上传 Excel 模板');
    return;
  }
  templateHeaderLoading.value = true;
  try {
    const result = await getRelationTicketTemplateHeaders({
      attachmentId: templateForm.attachmentId,
      headerRow: templateForm.headerRow,
    });
    templateForm.sheetName = result.sheetName;
    templateForm.fields = result.headers.map((item, index) => ({
      columnIndex: item.columnIndex,
      fillMode: item.fillMode || (item.systemField ? 'tourist_field' : 'keep_original'),
      fixedValue: item.fixedValue,
      required: item.required,
      sortOrder: index,
      systemField: item.systemField || '',
      systemFieldLabel: item.systemFieldLabel,
      templateHeader: item.templateHeader,
    }));
    message.success('模板表头已读取');
  } finally {
    templateHeaderLoading.value = false;
  }
}

async function saveTemplate() {
  if (!currentBinding.value) {
    message.warning('请先选择供应商绑定关系');
    return;
  }
  if (!templateForm.templateName?.trim()) {
    message.warning('请输入模板名称');
    return;
  }
  if (!templateForm.attachmentId) {
    message.warning('请上传 Excel 模板');
    return;
  }
  if (templateForm.dataStartRow <= templateForm.headerRow) {
    message.warning('数据开始行必须大于表头行');
    return;
  }
  const fields = templateForm.fields.filter((item) => item.templateHeader && item.fillMode);
  if (fields.length === 0) {
    message.warning('请至少配置一项字段映射');
    return;
  }
  templateSaving.value = true;
  try {
    await saveRelationTicketTemplate({
      attachmentId: templateForm.attachmentId,
      dataStartRow: templateForm.dataStartRow,
      fields,
      headerRow: templateForm.headerRow,
      originalFilename: templateForm.originalFilename,
      relationId: currentBinding.value.relationId,
      remark: clean(templateForm.remark),
      sheetName: templateForm.sheetName,
      status: templateForm.status,
      templateFileUrl: templateForm.templateFileUrl,
      templateName: templateForm.templateName.trim(),
    });
    message.success('游客名单模板已保存');
    templateDrawerOpen.value = false;
  } finally {
    templateSaving.value = false;
  }
}

function confirmDeleteTemplate() {
  if (!currentBinding.value) {
    return;
  }
  Modal.confirm({
    title: `删除「${currentBinding.value.supplierName || ''}」的游客名单模板？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteRelationTicketTemplate(currentBinding.value!.relationId);
      message.success('游客名单模板已删除');
      resetTemplateForm(currentBinding.value);
    },
  });
}

async function openBindingsFromRoute() {
  if (routeBindingOpened.value) {
    return;
  }
  const resourceId = Number(route.query.resourceId || 0);
  if (!resourceId) {
    return;
  }
  routeBindingOpened.value = true;
  const resource = await getPurchaseResourceDetail(resourceId);
  currentResource.value = resource;
  supplierBindingMode.value = 'bind';
  bindingDrawerOpen.value = true;
  bindForm.resourceId = resource.id;
  bindForm.isDefault = false;
  bindForm.supplierId = undefined as unknown as number;
  bindForm.status = 'active';
  resetScenicSupplierForm(resource);
  await Promise.all([
    loadSuppliers(resource.resourceType),
    loadBindings(resource.id),
    loadResourcePriceProjects(resource.resourceType),
    loadResourceOptionalItems(resource),
  ]);

  const relationId = Number(route.query.relationId || route.query.templateRelationId || 0);
  if (route.query.editPrice === '1' && relationId) {
    const binding = bindings.value.find((item) => item.relationId === relationId);
    if (binding) {
      await editBoundSupplier(binding);
    } else {
      message.warning('未找到对应的供应商资源绑定关系');
    }
    return;
  }
  if (!routeTemplateOpened.value && route.query.openTemplate !== '0' && relationId) {
    const binding = bindings.value.find((item) => item.relationId === relationId);
    if (binding) {
      routeTemplateOpened.value = true;
      await openTemplateDrawer(binding);
    }
  }
}

function openCreateModal() {
  resetForm();
  modalOpen.value = true;
}

function openEditModal(record: Record<string, any>) {
  const row = record as ResourceRow;
  editingId.value = row.id;
  formRegionPath.value = buildRegionPath(row.province, row.city, row.district);
  formState.resourceType = row.resourceType;
  // 自动生成供应商只属于新增动作，编辑旧资源不能重复触发。
  formState.autoCreateSupplier = false;
  formState.procurementMode = row.procurementMode || 'required';
  formState.resourceName = row.resourceName;
  formState.province = row.province;
  formState.city = row.city;
  formState.district = row.district;
  formState.phone = row.phone;
  formState.contactName = row.contactName;
  formState.fax = row.fax;
  formState.address = row.address;
  formState.businessStatus = row.businessStatus || 'unmaintained';
  formState.closingTime = row.closingTime;
  formState.warmTip = row.warmTip;
  formState.introduction = row.introduction;
  formState.lastSiteVisitDate = row.lastSiteVisitDate;
  formState.latitude = row.latitude;
  formState.longitude = row.longitude;
  formState.openingTime = row.openingTime;
  formState.scenicLevel = row.scenicLevel || 'unrated';
  formState.starLevel = row.starLevel || 'unrated';
  formState.categoryTags = row.categoryTags;
  formCategoryTags.value = row.categoryTags ? row.categoryTags.split(',').filter(Boolean) : [];
  formState.siteVisitNote = row.siteVisitNote;
  formState.siteVisitStatus = row.siteVisitStatus || 'unmaintained';
  formState.capacity = row.capacity;
  formState.tableCount = row.tableCount;
  formState.mealStandard = row.mealStandard;
  formState.vehicleType = row.vehicleType;
  formState.seatCount = row.seatCount;
  formState.billingMode = row.billingMode;
  formState.serviceArea = row.serviceArea;
  formState.referenceDays = row.referenceDays;
  formState.includedItems = row.includedItems;
  formState.excludedItems = row.excludedItems;
  formState.resourceUnit = row.resourceUnit;
  formState.status = row.status;
  formState.remark = row.remark;
  scenicMapKeyword.value = undefined;
  scenicMapTipOptions.value = [];
  scenicMapError.value = '';
  modalOpen.value = true;
}

function buildPayload(): PurchaseResourceApi.SaveParams {
  const regionFields = splitRegionPath(formRegionPath.value);
  const vehicleResource = formState.resourceType === 'vehicle';
  const payload: PurchaseResourceApi.SaveParams = {
    address: vehicleResource ? undefined : clean(formState.address),
    autoCreateSupplier: !editingId.value && Boolean(formState.autoCreateSupplier),
    city: vehicleResource ? undefined : regionFields.city,
    contactName: clean(formState.contactName),
    district: vehicleResource ? undefined : regionFields.district,
    fax: clean(formState.fax),
    introduction: clean(formState.introduction),
    phone: clean(formState.phone),
    province: vehicleResource ? undefined : regionFields.province,
    procurementMode: formState.procurementMode || 'required',
    remark: clean(formState.remark),
    resourceName: formState.resourceName.trim(),
    resourceType: formState.resourceType,
    status: formState.status || 'active',
    warmTip: clean(formState.warmTip),
  };
  if (formState.resourceType === 'scenic') {
    payload.scenicLevel = formState.scenicLevel || 'unrated';
  }
  if (supportsStarLevel(formState.resourceType)) {
    payload.starLevel = formState.starLevel || 'unrated';
  }
  if (supportsCategoryTags(formState.resourceType)) {
    payload.categoryTags = formCategoryTags.value.join(',');
  }
  if (isPlaceResource(formState.resourceType)) {
    payload.longitude = formState.longitude;
    payload.latitude = formState.latitude;
    payload.businessStatus = formState.businessStatus || 'unmaintained';
    payload.openingTime = formState.openingTime;
    payload.closingTime = formState.closingTime;
    payload.siteVisitStatus = formState.siteVisitStatus || 'unmaintained';
    payload.lastSiteVisitDate =
      formState.siteVisitStatus === 'visited'
        ? formState.lastSiteVisitDate
        : undefined;
    payload.siteVisitNote = clean(formState.siteVisitNote);
  }
  if (['restaurant', 'shopping'].includes(formState.resourceType || '')) {
    payload.capacity = formState.capacity;
  }
  if (formState.resourceType === 'restaurant') {
    payload.tableCount = formState.tableCount;
    payload.mealStandard = clean(formState.mealStandard);
  }
  if (formState.resourceType === 'vehicle') {
    payload.vehicleType = clean(formState.vehicleType);
    payload.seatCount = formState.seatCount;
    payload.billingMode = formState.billingMode;
  }
  if (['ground_agent', 'traffic'].includes(formState.resourceType || '')) {
    payload.serviceArea = clean(formState.serviceArea);
  }
  if (formState.resourceType === 'ground_agent') {
    payload.referenceDays = formState.referenceDays;
    payload.includedItems = clean(formState.includedItems);
    payload.excludedItems = clean(formState.excludedItems);
  }
  if (formState.resourceType === 'traffic') {
    payload.includedItems = clean(formState.includedItems);
  }
  if (formState.resourceType === 'other') {
    payload.includedItems = clean(formState.includedItems);
    payload.resourceUnit = clean(formState.resourceUnit);
  }
  return payload;
}

function validateScenicForm() {
  if (isPlaceResource(formState.resourceType)) {
    const hasLongitude = !isNil(formState.longitude);
    const hasLatitude = !isNil(formState.latitude);
    if (hasLongitude !== hasLatitude) {
      message.warning('经度和纬度必须同时填写');
      return false;
    }
    if (
      hasLongitude &&
      (Number(formState.longitude) < -180 || Number(formState.longitude) > 180)
    ) {
      message.warning('经度必须在 -180 到 180 之间');
      return false;
    }
    if (
      hasLatitude &&
      (Number(formState.latitude) < -90 || Number(formState.latitude) > 90)
    ) {
      message.warning('纬度必须在 -90 到 90 之间');
      return false;
    }
    const hasOpeningTime = Boolean(formState.openingTime);
    const hasClosingTime = Boolean(formState.closingTime);
    if (hasOpeningTime !== hasClosingTime) {
      message.warning('开始和结束营业时间必须同时填写');
      return false;
    }
    if (
      formState.openingTime &&
      formState.closingTime &&
      formState.closingTime <= formState.openingTime
    ) {
      message.warning('结束营业时间必须晚于开始时间');
      return false;
    }
    if (formState.siteVisitStatus === 'visited' && !formState.lastSiteVisitDate) {
      message.warning('已踩点时必须填写最近踩点日期');
      return false;
    }
    if (
      formState.lastSiteVisitDate &&
      formState.lastSiteVisitDate > dayjs().format('YYYY-MM-DD')
    ) {
      message.warning('最近踩点日期不能晚于今天');
      return false;
    }
  }
  if (formState.resourceType === 'vehicle' && !formState.seatCount) {
    message.warning('用车资源必须填写座位数');
    return false;
  }
  if (formState.resourceType === 'ground_agent' && !formState.serviceArea?.trim()) {
    message.warning('地接资源必须填写服务地区');
    return false;
  }
  return true;
}

function handleSiteVisitStatusChange(value: unknown) {
  if (value !== 'visited') {
    formState.lastSiteVisitDate = undefined;
  }
}

function currentScenicCoordinates() {
  const longitude = Number(formState.longitude);
  const latitude = Number(formState.latitude);
  if (
    isNil(formState.longitude) ||
    isNil(formState.latitude) ||
    !Number.isFinite(longitude) ||
    !Number.isFinite(latitude) ||
    longitude < -180 ||
    longitude > 180 ||
    latitude < -90 ||
    latitude > 90
  ) {
    return undefined;
  }
  return { latitude, longitude };
}

function buildScenicLocateKeyword() {
  return [
    formState.address,
    formState.resourceName,
    ...formRegionPath.value,
  ]
    .map((item) => String(item || '').trim())
    .filter(Boolean)
    .join(' ');
}

function renderScenicMapMarker(recenter = true) {
  const coordinates = currentScenicCoordinates();
  const AMap = (window as any).AMap;
  if (!scenicMapInstance || !AMap) {
    return;
  }
  if (!coordinates) {
    if (scenicMapMarker) {
      scenicMapInstance.remove(scenicMapMarker);
      scenicMapMarker = undefined;
    }
    return;
  }
  const position = [coordinates.longitude, coordinates.latitude];
  if (scenicMapMarker) {
    scenicMapMarker.setPosition(position);
  } else {
    scenicMapMarker = new AMap.Marker({
      position,
      title: formState.resourceName || '资源位置',
    });
    scenicMapInstance.add(scenicMapMarker);
  }
  if (recenter) {
    scenicMapInstance.setZoomAndCenter(16, position);
  }
}

function applyScenicMapPosition(
  longitude: number,
  latitude: number,
  address?: string,
) {
  formState.longitude = Number(longitude.toFixed(7));
  formState.latitude = Number(latitude.toFixed(7));
  if (address?.trim()) {
    formState.address = address.trim();
  }
  renderScenicMapMarker();
}

function applyScenicMapReverseGeocodeResult(
  result: PurchaseResourceApi.AmapRegeoResult,
) {
  const province = clean(result.province);
  const city = clean(result.city || result.province);
  const district = clean(result.district);
  if (province || city || district) {
    formState.province = province;
    formState.city = city;
    formState.district = district;
    formRegionPath.value = buildRegionPath(province, city, district);
  }
  if (result.address?.trim()) {
    formState.address = result.address.trim();
  }
}

async function reverseGeocodeScenicMapPosition(
  longitude: number,
  latitude: number,
  showWarning = false,
) {
  try {
    const result = await reverseGeocodeCommonAmap({ latitude, longitude });
    applyScenicMapReverseGeocodeResult(result);
    return true;
  } catch {
    if (showWarning) {
      message.warning('地址解析失败，请手工填写详细地址和所在地');
    }
    return false;
  }
}

function syncScenicMapFromCoordinates() {
  renderScenicMapMarker();
}

async function locateScenicMapByKeyword(
  keyword: string,
  showWarning = true,
  applyToForm = true,
) {
  const cleanKeyword = keyword.trim();
  if (!cleanKeyword) {
    if (showWarning) {
      message.warning('请先填写详细地址或资源名称');
    }
    return false;
  }
  try {
    const tips = await searchCommonAmapTips({
      city: splitRegionPath(formRegionPath.value).city,
      keywords: cleanKeyword,
    });
    const firstTip = tips.find((item) => {
      const longitude = Number(item.longitude);
      const latitude = Number(item.latitude);
      return Number.isFinite(longitude) && Number.isFinite(latitude);
    });
    if (!firstTip) {
      if (showWarning) {
        message.warning('未找到可定位的位置，请手工填写经纬度');
      }
      return false;
    }
    const longitude = Number(firstTip.longitude);
    const latitude = Number(firstTip.latitude);
    const address =
      firstTip.address ||
      [firstTip.district, firstTip.name].filter(Boolean).join(' ');
    if (applyToForm) {
      applyScenicMapPosition(longitude, latitude, address);
      await reverseGeocodeScenicMapPosition(longitude, latitude);
    } else {
      scenicMapInstance?.setZoomAndCenter(13, [longitude, latitude]);
    }
    return true;
  } catch {
    if (showWarning) {
      message.warning('地址定位暂不可用，请手工填写经纬度');
    }
    return false;
  }
}

function locateScenicMapByAddress() {
  void locateScenicMapByKeyword(buildScenicLocateKeyword());
}

function scheduleScenicMapAutoLocate() {
  if (scenicMapAutoLocateTimer !== undefined) {
    window.clearTimeout(scenicMapAutoLocateTimer);
  }
  scenicMapAutoLocateTimer = window.setTimeout(() => {
    scenicMapAutoLocateTimer = undefined;
    if (!modalOpen.value || !isPlaceForm.value || currentScenicCoordinates()) {
      return;
    }
    void locateScenicMapByKeyword(buildScenicLocateKeyword(), false, false);
  }, 100);
}

async function loadCommonAmapScript() {
  if ((window as any).AMap) {
    return (window as any).AMap;
  }
  if (amapLoaderPromise) {
    return amapLoaderPromise;
  }
  amapLoaderPromise = getCommonAmapJsConfig().then(
    (config) =>
      new Promise((resolve, reject) => {
        if (!config.key) {
          reject(new Error('未配置高德 JS Key'));
          return;
        }
        if (config.securityJsCode) {
          (window as any)._AMapSecurityConfig = {
            securityJsCode: config.securityJsCode,
          };
        }
        const script = document.createElement('script');
        script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(config.key)}`;
        script.async = true;
        script.addEventListener('load', () => resolve((window as any).AMap), {
          once: true,
        });
        script.addEventListener(
          'error',
          () => reject(new Error('高德地图脚本加载失败')),
          { once: true },
        );
        document.head.append(script);
      }),
  );
  try {
    return await amapLoaderPromise;
  } catch (error) {
    amapLoaderPromise = undefined;
    throw error;
  }
}

async function initScenicMap() {
  if (!modalOpen.value || !isPlaceForm.value || !scenicMapContainerRef.value) {
    return;
  }
  scenicMapLoading.value = true;
  scenicMapError.value = '';
  try {
    const AMap = await loadCommonAmapScript();
    if (
      !modalOpen.value ||
      !isPlaceForm.value ||
      !scenicMapContainerRef.value
    ) {
      return;
    }
    if (!scenicMapInstance) {
      const coordinates = currentScenicCoordinates();
      scenicMapInstance = new AMap.Map(scenicMapContainerRef.value, {
        center: coordinates
          ? [coordinates.longitude, coordinates.latitude]
          : [104.195397, 35.86166],
        resizeEnable: true,
        viewMode: '2D',
        zoom: coordinates ? 16 : 4,
      });
      scenicMapInstance.on('click', handleScenicMapClick);
    } else {
      scenicMapInstance.resize?.();
    }
    scenicMapReady.value = true;
    renderScenicMapMarker(Boolean(currentScenicCoordinates()));
    if (!currentScenicCoordinates()) {
      scheduleScenicMapAutoLocate();
    }
  } catch {
    scenicMapReady.value = false;
    scenicMapError.value = '地图暂不可用，可继续手工填写详细地址和经纬度。';
  } finally {
    scenicMapLoading.value = false;
  }
}

function destroyScenicMap() {
  clearScenicMapAutoLocateTimer();
  if (scenicMapInstance) {
    scenicMapInstance.off?.('click', handleScenicMapClick);
    scenicMapInstance.destroy?.();
  }
  scenicMapInstance = undefined;
  scenicMapMarker = undefined;
  scenicMapReady.value = false;
  scenicMapLoading.value = false;
}

async function handleScenicMapClick(event: any) {
  const longitude = Number(event.lnglat.getLng());
  const latitude = Number(event.lnglat.getLat());
  applyScenicMapPosition(longitude, latitude);
  await reverseGeocodeScenicMapPosition(longitude, latitude, true);
}

function clearScenicMapSearchTimer() {
  if (scenicMapSearchTimer !== undefined) {
    window.clearTimeout(scenicMapSearchTimer);
    scenicMapSearchTimer = undefined;
  }
}

function clearScenicMapAutoLocateTimer() {
  if (scenicMapAutoLocateTimer !== undefined) {
    window.clearTimeout(scenicMapAutoLocateTimer);
    scenicMapAutoLocateTimer = undefined;
  }
}

function handleScenicMapSearch(value: string) {
  scenicMapKeyword.value = value;
  clearScenicMapSearchTimer();
  if (!value.trim()) {
    scenicMapTipOptions.value = [];
    scenicMapTipLoading.value = false;
    return;
  }
  scenicMapTipLoading.value = true;
  scenicMapSearchTimer = window.setTimeout(() => {
    void doScenicMapSearch(value);
  }, 300);
}

async function doScenicMapSearch(value: string) {
  const keyword = value.trim();
  if (!keyword || keyword !== scenicMapKeyword.value?.trim()) {
    scenicMapTipLoading.value = false;
    return;
  }
  try {
    const tips = await searchCommonAmapTips({
      city: splitRegionPath(formRegionPath.value).city,
      keywords: keyword,
    });
    if (keyword !== scenicMapKeyword.value?.trim()) {
      return;
    }
    scenicMapTipOptions.value = tips.flatMap((item, index) => {
      const longitude = Number(item.longitude);
      const latitude = Number(item.latitude);
      if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) {
        return [];
      }
      return [
        {
          label: [item.name, item.district, item.address]
            .filter(Boolean)
            .join(' / '),
          meta: item,
          value: `${index}:${longitude}:${latitude}`,
        },
      ];
    });
  } catch {
    scenicMapTipOptions.value = [];
    message.warning('地点搜索暂不可用，请手工填写地址和坐标');
  } finally {
    scenicMapTipLoading.value = false;
    scenicMapSearchTimer = undefined;
  }
}

async function handleScenicMapSelect(value: unknown) {
  if (typeof value !== 'string') {
    return;
  }
  const option = scenicMapTipOptions.value.find((item) => item.value === value);
  if (!option) {
    return;
  }
  const longitude = Number(option.meta.longitude);
  const latitude = Number(option.meta.latitude);
  const address =
    option.meta.address ||
    [option.meta.district, option.meta.name].filter(Boolean).join(' ');
  applyScenicMapPosition(longitude, latitude, address);
  await reverseGeocodeScenicMapPosition(longitude, latitude);
}

async function saveRecord() {
  if (!formState.resourceName?.trim()) {
    message.warning('请填写资源名称');
    return;
  }
  if (!validateScenicForm()) {
    return;
  }
  const payload = buildPayload();
  if (editingId.value) {
    await updatePurchaseResource(editingId.value, payload);
    message.success('资源已更新');
  } else {
    await createPurchaseResource(payload);
    message.success('资源已新增');
  }
  modalOpen.value = false;
  await loadData();
}

function confirmDelete(record: Record<string, any>) {
  const row = record as ResourceRow;
  Modal.confirm({
    title: `删除资源「${row.resourceName}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deletePurchaseResource(row.id);
      message.success('资源已删除');
      await loadData();
    },
  });
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

async function openBindingDrawer(record: Record<string, any>) {
  const row = record as ResourceRow;
  currentResource.value = row;
  supplierBindingMode.value = 'bind';
  bindingDrawerOpen.value = true;
  bindForm.resourceId = row.id;
  bindForm.isDefault = false;
  bindForm.supplierId = undefined as unknown as number;
  bindForm.status = 'active';
  resetScenicSupplierForm(row);
  await Promise.all([
    loadSuppliers(row.resourceType),
    loadBindings(row.id),
    loadResourcePriceProjects(row.resourceType),
    loadResourceOptionalItems(row),
  ]);
}

async function loadSuppliers(category: SupplierApi.Category) {
  const result = await getSupplierAll(category);
  supplierOptions.value = result.map((item) => ({
    label: item.supplierName,
    value: item.id,
  }));
}

async function loadResourcePriceProjects(resourceType: ResourceType) {
  if (resourceType === 'vehicle') {
    resourcePriceProjects.value = [];
    return;
  }
  const result = await getExpenseItemAll(resourceType as EnterpriseExpenseItemApi.ResourceType);
  resourcePriceProjects.value = result;
}

async function loadResourceOptionalItems(resource?: ResourceRow) {
  const target = resource || currentResource.value;
  if (!target || target.resourceType !== 'scenic') {
    resourceOptionalItems.value = [];
    return;
  }
  optionalItemMasterLoading.value = true;
  try {
    resourceOptionalItems.value = await getPurchaseResourceOptionalItems(target.id);
  } finally {
    optionalItemMasterLoading.value = false;
  }
}

async function loadBindings(resourceId: number) {
  bindingLoading.value = true;
  try {
    bindings.value = await getPurchaseResourceBindings(resourceId);
  } finally {
    bindingLoading.value = false;
  }
}

async function bindSupplier() {
  if (!currentResource.value) return;
  if (!bindForm.supplierId) {
    message.warning('请选择供应商');
    return;
  }
  await createPurchaseRelation({
    isDefault: Boolean(bindForm.isDefault),
    resourceId: currentResource.value.id,
    status: 'active',
    supplierId: bindForm.supplierId,
  });
  message.success('供应商已绑定');
  await Promise.all([loadBindings(currentResource.value.id), loadData()]);
}

/** 解除当前资源与供应商的关系，不删除供应商主档，其他资源仍可继续使用该供应商。 */
function confirmUnbindSupplier(binding: PurchaseResourceApi.Binding) {
  if (!currentResource.value) return;
  Modal.confirm({
    cancelText: '取消',
    content: `解除后「${binding.supplierName}」不会再作为「${currentResource.value.resourceName}」的供应商，供应商档案不会被删除。`,
    okText: '解除绑定',
    okType: 'danger',
    title: '确认解除供应商绑定？',
    async onOk() {
      await deletePurchaseRelation(binding.relationId);
      if (editingScenicSupplierRelationId.value === binding.relationId) {
        resetScenicSupplierForm(currentResource.value);
      }
      message.success('供应商绑定已解除');
      await Promise.all([loadBindings(currentResource.value!.id), loadData()]);
    },
  });
}

/** 输入全名或仅剩一个匹配项时，按 Enter 直接选中已有供应商。 */
function selectSupplierFromTypedName() {
  const supplierName = supplierSearchKeyword.value.trim();
  if (!supplierName) {
    return;
  }
  const normalized = supplierName.toLocaleLowerCase();
  const matched = supplierOptions.value.filter((item) =>
    item.label.trim().toLocaleLowerCase().includes(normalized),
  );
  const existing = supplierOptions.value.find((item) =>
    item.label.trim().toLocaleLowerCase() === normalized,
  );
  if (existing) {
    bindForm.supplierId = existing.value;
    return;
  }
  const [onlyMatch] = matched;
  if (onlyMatch) {
    bindForm.supplierId = onlyMatch.value;
    return;
  }
  if (!matched.length) message.warning('未找到匹配的已有供应商，请检查名称');
}

function hasClassifiedPrice() {
  return Object.values(resourceSupplierPriceValues).some((value) => !isNil(value));
}

/** 景区可仅维护自费项目：没有门票时，以至少一条完整的自费报价作为保存条件。 */
function hasOptionalItemQuote() {
  return (scenicSupplierForm.optionalItems || []).some(
    (item) => Boolean(item.projectName?.trim()) && !isNil(item.costPrice),
  );
}

function validateScenicSupplierForm() {
  if (!currentResource.value) {
    message.warning('请先选择资源');
    return false;
  }
  if (!scenicSupplierForm.supplierName.trim()) {
    message.warning('请填写供应商名称');
    return false;
  }
  if (scenicSupplierForm.status !== 'active' && scenicSupplierForm.isDefault) {
    message.warning('默认供应商必须是合作中状态');
    return false;
  }
  if (!validateOptionalItems()) {
    return false;
  }
  if (isVehicleResourceBinding.value) {
    return true;
  }
  if (scenicSupplierForm.priceMode === 'unified') {
    if (isNil(scenicSupplierForm.unifiedPrice) && !hasOptionalItemQuote()) {
      message.warning(currentResource.value.resourceType === 'scenic'
        ? '请填写门票统一报价，或至少维护一条自费项目报价'
        : '请填写统一报价');
      return false;
    }
    return true;
  }
  if (resourcePriceProjects.value.length === 0) {
    if (hasOptionalItemQuote()) return true;
    message.warning('当前资源类型还没有维护报价项目，请先到费用项目中维护');
    return false;
  }
  if (!hasClassifiedPrice() && !hasOptionalItemQuote()) {
    message.warning(currentResource.value.resourceType === 'scenic'
      ? '分类门票报价至少填写一种，或至少维护一条自费项目报价'
      : '分类报价至少填写一项');
    return false;
  }
  return true;
}

function validateOptionalItems() {
  if (currentResource.value?.resourceType !== 'scenic') {
    return true;
  }
  const items = scenicSupplierForm.optionalItems || [];
  for (const item of items) {
    const hasName = Boolean(item.projectName?.trim());
    const hasPrice = !isNil(item.costPrice);
    if (!hasName && !hasPrice && !item.priceDescription?.trim()) {
      continue;
    }
    if (!hasName) {
      message.warning('请填写自费项目名称');
      return false;
    }
    if (!hasPrice) {
      message.warning(`请填写「${item.projectName.trim()}」的供应商成本价`);
      return false;
    }
  }
  return true;
}

function buildScenicSupplierPayload(): PurchaseResourceApi.ResourceSupplierCreateParams {
  const supplierRegion = splitRegionPath(scenicSupplierRegionPath.value);
  const vehicleResource = isVehicleResourceBinding.value;
  const payload: PurchaseResourceApi.ResourceSupplierCreateParams = {
    basicInfo: clean(scenicSupplierForm.basicInfo),
    city: supplierRegion.city,
    contactName: clean(scenicSupplierForm.contactName),
    contactPhone: clean(scenicSupplierForm.contactPhone),
    district: supplierRegion.district,
    isDefault: Boolean(scenicSupplierForm.isDefault),
    optionalItems:
      currentResource.value?.resourceType === 'scenic'
        ? (scenicSupplierForm.optionalItems || [])
            .filter((item) => item.projectName?.trim() && !isNil(item.costPrice))
            .map((item) => ({
              costPrice: item.costPrice,
              priceDescription: clean(item.priceDescription),
              projectName: item.projectName.trim(),
              status: item.status || 'active',
              suggestedSalePrice: item.suggestedSalePrice,
            }))
        : undefined,
    priceMode: vehicleResource ? 'classified' : scenicSupplierForm.priceMode,
    priceRemark: vehicleResource ? undefined : clean(scenicSupplierForm.priceRemark),
    province: supplierRegion.province,
    remark: clean(scenicSupplierForm.remark),
    status: scenicSupplierForm.status || 'active',
    supplierName: scenicSupplierForm.supplierName.trim(),
  };
  if (vehicleResource) {
    return payload;
  }
  if (payload.priceMode === 'unified') {
    payload.unifiedPrice = scenicSupplierForm.unifiedPrice;
  } else {
    payload.priceLines = resourcePriceProjects.value
      .map((project) => ({
        resourceProjectId: project.id,
        teamPrice: resourceSupplierPriceValues[project.id],
      }))
      .filter((line) => !isNil(line.teamPrice));
  }
  return payload;
}

async function editBoundSupplier(record: PurchaseResourceApi.Binding) {
  supplierBindingMode.value = 'create';
  const supplier = await getSupplierDetail(record.supplierId);
  editingScenicSupplierId.value = supplier.id;
  editingScenicSupplierRelationId.value = record.relationId;
  scenicSupplierRegionPath.value = buildRegionPath(
    supplier.province,
    supplier.city,
    supplier.district,
  );
  scenicSupplierForm.supplierName = supplier.supplierName;
  scenicSupplierForm.basicInfo = supplier.basicInfo;
  scenicSupplierForm.contactName = supplier.contactName;
  scenicSupplierForm.contactPhone = supplier.contactPhone;
  scenicSupplierForm.status = supplier.status;
  scenicSupplierForm.isDefault = Boolean(record.isDefault);
  scenicSupplierForm.priceMode = isVehicleResourceBinding.value ? 'classified' : (record.priceMode || 'classified');
  scenicSupplierForm.unifiedPrice = isVehicleResourceBinding.value ? undefined : record.unifiedPrice;
  scenicSupplierForm.priceLines = record.priceLines?.map((line) => ({
    priceDescription: line.priceDescription,
    resourceProjectId: line.resourceProjectId,
    teamPrice: line.teamPrice,
  }));
  scenicSupplierForm.optionalItems = (record.optionalItems || []).map((item) => ({
    costPrice: item.costPrice,
    priceDescription: item.priceDescription,
    projectName: item.projectName,
    resourceOptionalItemId: item.resourceOptionalItemId,
    status: item.status || 'active',
    suggestedSalePrice: item.suggestedSalePrice,
  }));
  Object.keys(resourceSupplierPriceValues).forEach((key) => {
    delete resourceSupplierPriceValues[Number(key)];
  });
  record.priceLines?.forEach((line) => {
    resourceSupplierPriceValues[line.resourceProjectId] = line.teamPrice;
  });
  scenicSupplierForm.priceRemark = record.priceRemark;
  scenicSupplierForm.remark = supplier.remark;
}

function addOptionalItem() {
  if (!scenicSupplierForm.optionalItems) {
    scenicSupplierForm.optionalItems = [];
  }
  scenicSupplierForm.optionalItems.push({
    costPrice: undefined,
    priceDescription: '',
    projectName: '',
    resourceOptionalItemId: undefined,
    status: 'active',
    suggestedSalePrice: undefined,
  });
}

function removeOptionalItem(index: number) {
  scenicSupplierForm.optionalItems?.splice(index, 1);
}

async function createScenicSupplier() {
  if (!currentResource.value || !validateScenicSupplierForm()) {
    return;
  }
  creatingScenicSupplier.value = true;
  try {
    if (editingScenicSupplierId.value) {
      if (!editingScenicSupplierRelationId.value) {
        message.error(isVehicleResourceBinding.value
          ? '绑定关系缺少 relationId，无法保存供应商资料'
          : '绑定关系缺少 relationId，无法保存报价');
        return;
      }
      await updateResourceSupplierForResource(
        currentResource.value.id,
        editingScenicSupplierRelationId.value,
        buildScenicSupplierPayload(),
      );
      message.success(isVehicleResourceBinding.value ? '车队供应商资料已更新' : '供应商资料和报价已更新');
      await Promise.all([
        loadSuppliers(currentResource.value.resourceType),
        loadBindings(currentResource.value.id),
        loadData(),
      ]);
      return;
    }
    const created = await createResourceSupplierForResource(
      currentResource.value.id,
      buildScenicSupplierPayload(),
    );
    // 新增后保留当前录入内容，并切换为编辑状态，避免再次点击时重复创建同名供应商。
    editingScenicSupplierId.value = created.supplierId;
    editingScenicSupplierRelationId.value = created.relationId;
    message.success(isVehicleResourceBinding.value
      ? '车队供应商已新增并绑定'
      : '供应商已新增并绑定，可继续编辑');
    await Promise.all([
      loadSuppliers(currentResource.value.resourceType),
      loadBindings(currentResource.value.id),
      loadData(),
    ]);
  } finally {
    creatingScenicSupplier.value = false;
  }
}

async function openDocumentDrawer(record: Record<string, any>) {
  const row = record as ResourceRow;
  destroyIntroductionSortable();
  currentResource.value = row;
  materialTab.value = 'introductions';
  materialServiceUnavailable.value = false;
  introductions.value = [];
  images.value = [];
  documents.value = [];
  activeIntroductionId.value = undefined;
  introductionImageIds.value = [];
  introductionImageLoadedIntroductionId.value = undefined;
  introductionLoadedResourceId.value = undefined;
  imageLoadedResourceId.value = undefined;
  documentLoadedResourceId.value = undefined;
  resetIntroductionForm();
  revokeImagePreviewUrls();
  documentDrawerOpen.value = true;
  await Promise.all([
    loadResourceIntroductions(row),
    loadResourceImages(row),
    loadResourceOptionalItems(row),
  ]);
}

function isMissingMaterialService(error: unknown) {
  const requestError = error as { response?: { status?: number } };
  return requestError.response?.status === 404;
}

async function fallbackToResourceFiles(record: ResourceRow) {
  materialServiceUnavailable.value = true;
  materialTab.value = 'files';
  message.warning('介绍和图片资料服务尚未部署，当前可继续使用资源文件');
  if (documentLoadedResourceId.value !== record.id) {
    await loadResourceDocuments(record);
  }
}

async function loadResourceDocuments(record: ResourceRow) {
  documentLoading.value = true;
  try {
    documents.value = await getPurchaseResourceDocuments(record.id);
    documentLoadedResourceId.value = record.id;
  } finally {
    documentLoading.value = false;
  }
}

function resetIntroductionForm() {
  introductionForm.content = '';
  introductionForm.extensionBlocks = [];
  introductionForm.isOptionalItem = false;
  introductionForm.noticeContent = '';
  introductionForm.resourceOptionalItemId = undefined;
  introductionForm.tags = [];
  introductionForm.title = '';
  introductionForm.visitDuration = '';
  introductionForm.warmTipContent = '';
  introductionImageIds.value = [];
  introductionImageLoadedIntroductionId.value = undefined;
  introductionImagePickerOpen.value = false;
}

function selectIntroduction(record?: PurchaseResourceApi.ResourceIntroductionItem) {
  activeIntroductionId.value = record?.id;
  introductionForm.content = record?.content || '';
  introductionForm.extensionBlocks = (record?.extensionBlocks || []).map((block) => ({
    ...block,
    // 旧版“分条输入”数据在打开时合并成换行文本，避免历史素材内容丢失。
    content: block.contentMode === 'multiline'
      ? (block.content || '')
      : (block.items || []).join('\n'),
    contentMode: 'multiline',
    items: [],
    titleColor: block.titleColor
      || (block.type === 'photo_recommendation' ? '#d97706' : block.type === 'warm_tip' ? '#0070c0' : '#000000'),
    type: 'generic',
  }));
  introductionForm.isOptionalItem = Boolean(record?.isOptionalItem);
  introductionForm.noticeContent = record?.noticeContent || '';
  introductionForm.resourceOptionalItemId = record?.resourceOptionalItemId;
  introductionForm.tags = [...(record?.tags || [])];
  introductionForm.title = record?.title || '';
  introductionForm.visitDuration = record?.visitDuration || '';
  introductionForm.warmTipContent = record?.warmTipContent || '';
  introductionImageIds.value = [];
  introductionImageLoadedIntroductionId.value = undefined;
  introductionImagePickerOpen.value = false;
  if (record && currentResource.value) {
    void loadIntroductionImageIds(currentResource.value.id, record.id);
  }
}

async function loadIntroductionImageIds(resourceId: number, introductionId: number) {
  introductionImageSelectionLoading.value = true;
  try {
    const ids = await getPurchaseResourceIntroductionImages(resourceId, introductionId);
    if (activeIntroductionId.value === introductionId && currentResource.value?.id === resourceId) {
      introductionImageIds.value = ids;
      introductionImageLoadedIntroductionId.value = introductionId;
    }
  } finally {
    introductionImageSelectionLoading.value = false;
  }
}

function handleIntroductionTypeChange(isOptionalItem: boolean) {
  if (!isOptionalItem) {
    introductionForm.resourceOptionalItemId = undefined;
  }
}

/** 当前素材的图片顺序就是产品 Word 内该素材配图的输出顺序。 */
function toggleIntroductionImage(imageId: number, checked: boolean) {
  if (checked) {
    if (!introductionImageIds.value.includes(imageId)) {
      introductionImageIds.value = [...introductionImageIds.value, imageId];
    }
    return;
  }
  introductionImageIds.value = introductionImageIds.value.filter((id) => id !== imageId);
}

function moveIntroductionImage(index: number, offset: number) {
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= introductionImageIds.value.length) {
    return;
  }
  const nextIds = [...introductionImageIds.value];
  const [moved] = nextIds.splice(index, 1);
  if (moved == null) {
    return;
  }
  nextIds.splice(targetIndex, 0, moved);
  introductionImageIds.value = nextIds;
}

function openImageMaterialLibrary() {
  materialTab.value = 'images';
  if (currentResource.value && imageLoadedResourceId.value !== currentResource.value.id) {
    void loadResourceImages(currentResource.value);
  }
}

function toggleIntroductionImagePicker() {
  introductionImagePickerOpen.value = !introductionImagePickerOpen.value;
}

/** 销毁当前抽屉的排序实例，避免抽屉重开后留下重复的拖拽事件。 */
function destroyIntroductionSortable() {
  introductionSortableVersion += 1;
  introductionSortableInstance?.destroy();
  introductionSortableInstance = undefined;
}

/** 介绍素材只允许通过左侧拖拽手柄调整默认输出顺序。 */
async function initializeIntroductionSortable() {
  destroyIntroductionSortable();
  await nextTick();
  const container = introductionSortableContainerRef.value;
  if (
    !container
    || !documentDrawerOpen.value
    || materialTab.value !== 'introductions'
    || introductions.value.length < 2
  ) {
    return;
  }

  const version = ++introductionSortableVersion;
  const { initializeSortable } = useSortable(container, {
    animation: 160,
    draggable: '.resource-material-intro-row',
    dragClass: 'resource-material-intro-sort-dragging',
    ghostClass: 'resource-material-intro-sort-ghost',
    handle: '.resource-material-intro-drag-handle',
    chosenClass: 'resource-material-intro-sort-chosen',
    onEnd: handleIntroductionSortableEnd,
  });
  const sortable = await initializeSortable();
  if (
    version !== introductionSortableVersion
    || !documentDrawerOpen.value
    || materialTab.value !== 'introductions'
    || container !== introductionSortableContainerRef.value
  ) {
    sortable.destroy();
    return;
  }
  introductionSortableInstance = sortable;
}

/** 乐观调整左侧列表，再整体提交资源默认介绍顺序；失败时以服务端顺序为准恢复。 */
async function handleIntroductionSortableEnd(event: {
  newIndex?: number;
  oldIndex?: number;
}) {
  const { newIndex, oldIndex } = event;
  const resource = currentResource.value;
  if (
    introductionReordering.value
    || !resource
    || newIndex === undefined
    || oldIndex === undefined
    || newIndex === oldIndex
    || newIndex < 0
    || oldIndex < 0
  ) {
    return;
  }

  const previousItems = [...introductions.value];
  const nextItems = [...previousItems];
  const [moved] = nextItems.splice(oldIndex, 1);
  if (!moved) {
    return;
  }
  nextItems.splice(newIndex, 0, moved);
  introductions.value = nextItems;
  introductionReordering.value = true;
  introductionSortableInstance?.option('disabled', true);
  try {
    await reorderPurchaseResourceIntroductions(resource.id, {
      introductionIds: nextItems.map((item) => item.id),
    });
    message.success('介绍素材顺序已保存');
  } catch {
    try {
      if (currentResource.value?.id === resource.id) {
        await loadResourceIntroductions(resource);
      }
      message.error('排序保存失败，已恢复服务端最新顺序');
    } catch {
      introductions.value = previousItems;
      message.error('排序保存失败，暂时无法读取最新顺序，请刷新后重试');
    }
  } finally {
    introductionReordering.value = false;
    introductionSortableInstance?.option('disabled', false);
  }
}

async function loadResourceIntroductions(record: ResourceRow) {
  introductionLoading.value = true;
  try {
    const items = await getPurchaseResourceIntroductions(record.id);
    introductions.value = items;
    introductionLoadedResourceId.value = record.id;
    const selected = items.find((item) => item.id === activeIntroductionId.value) || items[0];
    selectIntroduction(selected);
  } catch (error) {
    if (isMissingMaterialService(error)) {
      await fallbackToResourceFiles(record);
      return;
    }
    throw error;
  } finally {
    introductionLoading.value = false;
    if (documentDrawerOpen.value && materialTab.value === 'introductions') {
      void initializeIntroductionSortable();
    }
  }
}

function revokeImagePreviewUrls() {
  Object.values(imagePreviewUrls).forEach((url) => URL.revokeObjectURL(url));
  Object.keys(imagePreviewUrls).forEach((id) => {
    delete imagePreviewUrls[Number(id)];
  });
}

async function loadImagePreviewUrls(items: PurchaseResourceApi.ResourceImageItem[]) {
  const resourceId = currentResource.value?.id;
  if (!resourceId) {
    return;
  }
  const results = await Promise.all(
    items.map(async (item) => {
      try {
        const blob = await downloadPurchaseResourceImage(resourceId, item.id);
        return { id: item.id, url: URL.createObjectURL(blob) };
      } catch {
        return undefined;
      }
    }),
  );
  if (currentResource.value?.id !== resourceId) {
    results.forEach((item) => item && URL.revokeObjectURL(item.url));
    return;
  }
  results.forEach((item) => {
    if (item) {
      imagePreviewUrls[item.id] = item.url;
    }
  });
}

async function loadResourceImages(record: ResourceRow) {
  imageLoading.value = true;
  try {
    revokeImagePreviewUrls();
    const items = await getPurchaseResourceImages(record.id);
    images.value = items;
    imageLoadedResourceId.value = record.id;
    void loadImagePreviewUrls(items);
  } catch (error) {
    if (isMissingMaterialService(error)) {
      await fallbackToResourceFiles(record);
      return;
    }
    throw error;
  } finally {
    imageLoading.value = false;
  }
}

async function handleMaterialTabChange(key: string | number) {
  const tab = key as 'introductions' | 'images' | 'files';
  materialTab.value = tab;
  if (tab !== 'introductions') {
    destroyIntroductionSortable();
  }
  const resource = currentResource.value;
  if (!resource) {
    return;
  }
  if (materialServiceUnavailable.value && tab !== 'files') {
    materialTab.value = 'files';
    return;
  }
  if (tab === 'introductions' && introductionLoadedResourceId.value !== resource.id) {
    await loadResourceIntroductions(resource);
  }
  if (tab === 'images' && imageLoadedResourceId.value !== resource.id) {
    await loadResourceImages(resource);
  }
  if (tab === 'files' && documentLoadedResourceId.value !== resource.id) {
    await loadResourceDocuments(resource);
  }
  if (tab === 'introductions' && introductionLoadedResourceId.value === resource.id) {
    void initializeIntroductionSortable();
  }
}

function validateIntroductionForm() {
  if (!introductionForm.title.trim()) {
    message.warning('请填写介绍名称');
    return false;
  }
  if (!introductionForm.content.trim()) {
    message.warning('请填写介绍正文');
    return false;
  }
  if (introductionForm.isOptionalItem && !introductionForm.resourceOptionalItemId) {
    message.warning('自费项目介绍必须关联一个自费项目');
    return false;
  }
  const blocks = introductionForm.extensionBlocks || [];
  if (blocks.some((block) => !block.title.trim() || !block.content?.trim())) {
    message.warning('扩展内容模块请填写标题和至少一条内容');
    return false;
  }
  return true;
}

/** 游览时间统一只保存分钟数字，输入时过滤掉文字和单位。 */
function handleVisitDurationInput(value: unknown) {
  const raw = typeof value === 'string'
    ? value
    : String((value as { target?: { value?: unknown } } | undefined)?.target?.value ?? '');
  introductionForm.visitDuration = raw.replace(/\D/g, '');
}

function addIntroductionExtensionBlock() {
  const blocks = introductionForm.extensionBlocks || (introductionForm.extensionBlocks = []);
  if (blocks.length >= 10) {
    message.warning('一份介绍素材最多10个扩展内容模块');
    return;
  }
  blocks.push({
    content: '',
    contentMode: 'multiline',
    items: [],
    title: '',
    titleColor: '#000000',
    type: 'generic',
  });
}

function removeIntroductionExtensionBlock(index: number) {
  introductionForm.extensionBlocks?.splice(index, 1);
}

function moveIntroductionExtensionBlock(index: number, direction: -1 | 1) {
  const blocks = introductionForm.extensionBlocks || [];
  const target = index + direction;
  if (target < 0 || target >= blocks.length) return;
  [blocks[index], blocks[target]] = [blocks[target]!, blocks[index]!];
}

function introductionPayload(): PurchaseResourceApi.ResourceIntroductionSaveParams {
  return {
    content: introductionForm.content.trim(),
    extensionBlocks: (introductionForm.extensionBlocks || []).map((block) => ({
      content: block.content?.trim(),
      contentMode: 'multiline',
      items: [],
      title: block.title.trim(),
      titleColor: block.titleColor,
      type: 'generic',
    })),
    isOptionalItem: Boolean(introductionForm.isOptionalItem),
    // 传空字符串才能让用户在编辑时清空已保存的温馨提示、注意事项和游览时间。
    noticeContent: (introductionForm.noticeContent || '').trim(),
    resourceOptionalItemId: introductionForm.isOptionalItem
      ? introductionForm.resourceOptionalItemId
      : undefined,
    tags: introductionForm.tags?.map((tag) => tag.trim()).filter(Boolean),
    title: introductionForm.title.trim(),
    visitDuration: (introductionForm.visitDuration || '').trim(),
    warmTipContent: (introductionForm.warmTipContent || '').trim(),
  };
}

async function saveIntroduction(showMessage = true) {
  if (!currentResource.value || !validateIntroductionForm()) {
    return undefined;
  }
  const resourceId = currentResource.value.id;
  if (
    activeIntroductionId.value
    && introductionImageLoadedIntroductionId.value !== activeIntroductionId.value
  ) {
    await loadIntroductionImageIds(resourceId, activeIntroductionId.value);
  }
  introductionSaving.value = true;
  try {
    const item = activeIntroductionId.value
      ? await updatePurchaseResourceIntroduction(
          resourceId,
          activeIntroductionId.value,
          introductionPayload(),
        )
      : await createPurchaseResourceIntroduction(
          resourceId,
          introductionPayload(),
        );
    await savePurchaseResourceIntroductionImages(resourceId, item.id, {
      imageIds: [...introductionImageIds.value],
    });
    activeIntroductionId.value = item.id;
    introductionImageLoadedIntroductionId.value = item.id;
    await loadResourceIntroductions(currentResource.value);
    selectIntroduction(introductions.value.find((value) => value.id === item.id));
    if (showMessage) {
      message.success('介绍草稿已保存');
    }
    return item;
  } finally {
    introductionSaving.value = false;
  }
}

function startNewIntroduction() {
  selectIntroduction();
}

/** 发布接口先返回，向量切片在后台完成后再刷新一次，避免页面一直显示旧的待处理状态。 */
async function refreshIntroductionIndexStatus(resourceId: number, introductionId: number) {
  for (let attempt = 0; attempt < 3; attempt += 1) {
    await new Promise((resolve) => window.setTimeout(resolve, 800));
    if (currentResource.value?.id !== resourceId) {
      return;
    }
    try {
      await loadResourceIntroductions(currentResource.value);
      const refreshed = introductions.value.find((item) => item.id === introductionId);
      if (refreshed) {
        selectIntroduction(refreshed);
      }
      if (refreshed?.indexStatus !== 'pending') {
        return;
      }
    } catch {
      return;
    }
  }
}

async function handlePublishIntroduction() {
  if (!currentResource.value) {
    return;
  }
  const resourceId = currentResource.value.id;
  introductionPublishing.value = true;
  try {
    const saved = await saveIntroduction(false);
    if (!saved) {
      return;
    }
    await publishPurchaseResourceIntroduction(resourceId, saved.id);
    message.success('介绍已发布，正在写入产品文案资料库');
    await loadResourceIntroductions(currentResource.value);
    selectIntroduction(introductions.value.find((item) => item.id === saved.id));
    void refreshIntroductionIndexStatus(resourceId, saved.id);
  } finally {
    introductionPublishing.value = false;
  }
}

async function handleRetryIntroduction(record: PurchaseResourceApi.ResourceIntroductionItem) {
  if (!currentResource.value) {
    return;
  }
  await retryPurchaseResourceIntroduction(currentResource.value.id, record.id);
  message.success('已重新提交向量化');
  await loadResourceIntroductions(currentResource.value);
}

function handleDeleteIntroduction(record: PurchaseResourceApi.ResourceIntroductionItem) {
  if (!currentResource.value) {
    return;
  }
  Modal.confirm({
    title: `删除介绍「${record.title}」？`,
    content: '删除后会同步删除这份介绍对应的向量切片，无法用于后续产品文案生成。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deletePurchaseResourceIntroduction(currentResource.value!.id, record.id);
      message.success('介绍及对应向量已删除');
      activeIntroductionId.value = undefined;
      await loadResourceIntroductions(currentResource.value!);
    },
  });
}

function handleDeleteActiveIntroduction() {
  if (activeIntroduction.value) {
    handleDeleteIntroduction(activeIntroduction.value);
  }
}

const beforeUploadImage: UploadProps['beforeUpload'] = async (file) => {
  if (!currentResource.value) {
    message.warning('请先选择资源');
    return false;
  }
  const isImage = /\.(jpe?g|png|webp)$/i.test(file.name);
  if (!isImage) {
    message.warning('仅支持 JPG、PNG、WEBP 图片');
    return false;
  }
  if (file.size > 20 * 1024 * 1024) {
    message.warning('单张图片不能超过 20MB');
    return false;
  }
  uploadingImages.value = true;
  try {
    await uploadPurchaseResourceImages(currentResource.value.id, [file as File]);
    message.success('图片素材已上传');
    await loadResourceImages(currentResource.value);
  } finally {
    uploadingImages.value = false;
  }
  return false;
};

/** 在介绍编辑区上传的新图片先进入资源图片素材库，再自动勾选到当前介绍。 */
const beforeUploadIntroductionImage: UploadProps['beforeUpload'] = async (file) => {
  if (!currentResource.value) {
    message.warning('请先选择资源');
    return false;
  }
  const isImage = /\.(jpe?g|png|webp)$/i.test(file.name);
  if (!isImage) {
    message.warning('仅支持 JPG、PNG、WEBP 图片');
    return false;
  }
  if (file.size > 20 * 1024 * 1024) {
    message.warning('单张图片不能超过 20MB');
    return false;
  }
  uploadingImages.value = true;
  try {
    const uploaded = await uploadPurchaseResourceImages(currentResource.value.id, [file as File]);
    introductionImageIds.value = [
      ...new Set([...introductionImageIds.value, ...uploaded.map((item) => item.id)]),
    ];
    await loadResourceImages(currentResource.value);
    message.success('图片已上传到素材库，并选入当前介绍');
  } finally {
    uploadingImages.value = false;
  }
  return false;
};

async function previewResourceImage(record: PurchaseResourceApi.ResourceImageItem) {
  if (!currentResource.value) {
    return;
  }
  currentDocument.value = undefined;
  currentImage.value = record;
  try {
    const blob = await downloadPurchaseResourceImage(currentResource.value.id, record.id);
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value);
    }
    previewUrl.value = URL.createObjectURL(blob);
    previewOpen.value = true;
  } catch {
    message.error('图片加载失败');
  }
}

function openImageEditor(record: PurchaseResourceApi.ResourceImageItem) {
  currentImage.value = record;
  imageEditForm.sortOrder = record.sortOrder;
  imageEditForm.tags = [...record.tags];
  imageEditorOpen.value = true;
}

async function saveImageEditor() {
  if (!currentResource.value || !currentImage.value) {
    return;
  }
  imageSaving.value = true;
  try {
    await updatePurchaseResourceImage(currentResource.value.id, currentImage.value.id, {
      sortOrder: imageEditForm.sortOrder,
      tags: imageEditForm.tags?.map((tag) => tag.trim()).filter(Boolean),
    });
    message.success('图片信息已保存');
    imageEditorOpen.value = false;
    await loadResourceImages(currentResource.value);
  } finally {
    imageSaving.value = false;
  }
}

async function handleSetImageCover(record: PurchaseResourceApi.ResourceImageItem) {
  if (!currentResource.value || record.isCover) {
    return;
  }
  await setPurchaseResourceImageCover(currentResource.value.id, record.id);
  message.success('已设为资源封面');
  await loadResourceImages(currentResource.value);
}

function handleDeleteImage(record: PurchaseResourceApi.ResourceImageItem) {
  if (!currentResource.value) {
    return;
  }
  Modal.confirm({
    title: `删除图片「${record.originalFilename}」？`,
    content: '删除后图片将不能用于产品资料和确认单展示。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deletePurchaseResourceImage(currentResource.value!.id, record.id);
      message.success('图片已删除');
      await loadResourceImages(currentResource.value!);
    },
  });
}

function isImageDocument(record: PurchaseResourceApi.ResourceDocumentItem) {
  return ['jpg', 'jpeg', 'png', 'webp'].includes((record.fileExt || '').toLowerCase());
}

const beforeUploadDocument: UploadProps['beforeUpload'] = async (file) => {
  if (!currentResource.value) {
    message.warning('请先选择资源');
    return false;
  }
  uploadingDocuments.value = true;
  try {
    await uploadPurchaseResourceDocuments(currentResource.value.id, [file as File]);
    message.success('资源资料已上传');
    await loadResourceDocuments(currentResource.value);
  } finally {
    uploadingDocuments.value = false;
  }
  return false;
};

async function previewDocument(record: PurchaseResourceApi.ResourceDocumentItem) {
  currentImage.value = undefined;
  currentDocument.value = record;
  try {
    const blob = await downloadPurchaseResourceDocument(
      currentResource.value!.id,
      record.id,
    );
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value);
    }
    previewUrl.value = URL.createObjectURL(blob);
    previewOpen.value = true;
  } catch {
    message.error('图片加载失败');
  }
}

async function downloadDocument(record: PurchaseResourceApi.ResourceDocumentItem) {
  try {
    const blob = await downloadPurchaseResourceDocument(
      currentResource.value!.id,
      record.id,
    );
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.href = url;
    link.download = record.originalFilename;
    link.click();
    URL.revokeObjectURL(url);
  } catch {
    message.error('文件下载失败');
  }
}

async function refreshDocuments() {
  if (!currentResource.value) {
    return;
  }
  await loadResourceDocuments(currentResource.value);
}

async function handlePublishDocument(record: PurchaseResourceApi.ResourceDocumentItem) {
  if (!currentResource.value) {
    return;
  }
  await publishPurchaseResourceDocument(currentResource.value.id, record.id);
  message.success('资料已发布');
  await refreshDocuments();
}

async function handleDisableDocument(record: PurchaseResourceApi.ResourceDocumentItem) {
  if (!currentResource.value) {
    return;
  }
  await disablePurchaseResourceDocument(currentResource.value.id, record.id);
  message.success('资料已停用');
  await refreshDocuments();
}

async function handleRetryDocument(record: PurchaseResourceApi.ResourceDocumentItem) {
  if (!currentResource.value) {
    return;
  }
  await retryPurchaseResourceDocument(currentResource.value.id, record.id);
  message.success('资料已重新处理');
  await refreshDocuments();
}

async function handleDeleteDocument(record: PurchaseResourceApi.ResourceDocumentItem) {
  if (!currentResource.value) {
    return;
  }
  Modal.confirm({
    title: `删除资料「${record.originalFilename}」？`,
    content: '删除后会同步删除该文件对应的知识库切片和向量，原始元数据仅保留审计记录。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deletePurchaseResourceDocument(currentResource.value!.id, record.id);
      message.success('资料已删除');
      await refreshDocuments();
    },
  });
}

watch(modalOpen, async (open) => {
  if (!open) {
    destroyScenicMap();
    return;
  }
  if (!isPlaceForm.value) {
    return;
  }
  await nextTick();
  window.requestAnimationFrame(() => {
    void initScenicMap();
  });
});

watch(
  () => formState.resourceType,
  async (resourceType) => {
    if (!modalOpen.value) {
      return;
    }
    if (!isPlaceResource(String(resourceType || ''))) {
      destroyScenicMap();
      return;
    }
    await nextTick();
    window.requestAnimationFrame(() => {
      void initScenicMap();
    });
  },
);

watch(documentDrawerOpen, (open) => {
  if (!open) {
    destroyIntroductionSortable();
    return;
  }
  if (materialTab.value === 'introductions' && introductions.value.length > 1) {
    void initializeIntroductionSortable();
  }
});

watch(materialTab, (tab) => {
  if (tab !== 'introductions') {
    destroyIntroductionSortable();
    return;
  }
  if (documentDrawerOpen.value && introductions.value.length > 1) {
    void initializeIntroductionSortable();
  }
});

watch(
  () => [route.query.resourceId, route.query.relationId, route.query.editPrice],
  async (nextRoute, previousRoute) => {
    if (nextRoute.join('|') === previousRoute?.join('|')) {
      return;
    }
    routeBindingOpened.value = false;
    routeTemplateOpened.value = false;
    await openBindingsFromRoute();
  },
);

onBeforeUnmount(() => {
  destroyIntroductionSortable();
  clearScenicMapSearchTimer();
  clearScenicMapAutoLocateTimer();
  destroyScenicMap();
  revokeImagePreviewUrls();
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
});

onMounted(async () => {
  await Promise.all([loadData(), loadReceptionStandards()]);
  await openBindingsFromRoute();
});
</script>

<template>
  <Page
    title="资源总览"
    description="维护景区、酒店、餐厅、购物、用车、大交通、地接和其它资源主档，并查看供应商绑定情况。"
  >
    <Card>
      <BusinessSearchForm
        label-width="78px"
        :model="query"
        :search-loading="loading"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="资源类型">
          <Select
            v-model:value="query.resourceType"
            allow-clear
            :options="resourceTypeOptions"
            placeholder="请选择资源类型"
            @change="handleQueryResourceTypeChange"
          />
        </Form.Item>
        <Form.Item v-if="!isVehicleList" label="所在地">
          <AutoComplete
            v-model:value="queryLocationKeyword"
            allow-clear
            :filter-option="false"
            :options="queryLocationOptions"
            placeholder="输入省 / 市 / 区县搜索"
            @press-enter="applyQueryLocationKeyword"
            @select="selectQueryLocation"
          />
        </Form.Item>
        <Form.Item label="资源名称">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="请输入资源名称"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select
            v-model:value="query.status"
            allow-clear
            :options="statusOptions"
            placeholder="请选择状态"
          />
        </Form.Item>
        <Form.Item label="采购属性">
          <Select
            v-model:value="query.procurementMode"
            allow-clear
            :options="procurementModeOptions"
            placeholder="请选择采购属性"
          />
        </Form.Item>
        <Form.Item v-if="isScenicList" label="国家 A 级">
          <Select
            v-model:value="query.scenicLevel"
            allow-clear
            :options="scenicLevelOptions"
            placeholder="请选择景区等级"
          />
        </Form.Item>
        <Form.Item v-if="isScenicList" label="营业状态">
          <Select
            v-model:value="query.businessStatus"
            allow-clear
            :options="businessStatusOptions"
            placeholder="请选择营业状态"
          />
        </Form.Item>
        <Form.Item v-if="isScenicList" label="踩点状态">
          <Select
            v-model:value="query.siteVisitStatus"
            allow-clear
            :options="siteVisitStatusOptions"
            placeholder="请选择踩点状态"
          />
        </Form.Item>
      </BusinessSearchForm>

      <Table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: isPlaceList ? 2290 : 1620 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'resourceType'">
            <Tag :color="typeColor(record.resourceType)">
              {{ typeLabel(record.resourceType) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'procurementMode'">
            <Tag :color="procurementModeColor(record.procurementMode)">
              {{ procurementModeLabel(record.procurementMode) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'area'">
            {{ areaText(record) }}
          </template>
          <template v-else-if="column.key === 'resourceName'">
            <div class="resource-name">{{ record.resourceName }}</div>
            <div v-if="!isScenicList" class="muted">
              {{ record.address || '-' }}
            </div>
            <Tag :color="statusColor(record.status)" class="mt-1">
              {{ statusLabel(record.status) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'seatCount'">
            <strong>{{ record.seatCount ? `${record.seatCount} 座` : '-' }}</strong>
          </template>
          <template v-else-if="column.key === 'vehicleType'">
            {{ record.vehicleType || '-' }}
          </template>
          <template v-else-if="column.key === 'billingMode'">
            {{ billingModeLabel(record.billingMode) }}
          </template>
          <template v-else-if="column.key === 'scenicLevel'">
            <Tag color="blue">{{ scenicLevelLabel(record.scenicLevel) }}</Tag>
          </template>
          <template v-else-if="column.key === 'mapLocation'">
            <div class="cell-primary" :title="record.address || ''">
              {{ record.address || '-' }}
            </div>
            <div class="muted">{{ coordinateText(record) }}</div>
          </template>
          <template v-else-if="column.key === 'businessInfo'">
            <Tag :color="businessStatusColor(record.businessStatus)">
              {{ businessStatusLabel(record.businessStatus) }}
            </Tag>
            <div class="muted mt-1">{{ businessTimeText(record) }}</div>
          </template>
          <template v-else-if="column.key === 'siteVisitInfo'">
            <Tag :color="siteVisitStatusColor(record.siteVisitStatus)">
              {{ siteVisitStatusLabel(record.siteVisitStatus) }}
            </Tag>
            <div class="muted mt-1">{{ record.lastSiteVisitDate || '-' }}</div>
            <div
              v-if="record.siteVisitNote"
              class="cell-note"
              :title="record.siteVisitNote"
            >
              {{ record.siteVisitNote }}
            </div>
          </template>
          <template v-else-if="column.key === 'boundSupplierCount'">
            <Button type="link" size="small" @click="openBindingDrawer(record)">
              {{ record.boundSupplierCount || 0 }} 家
            </Button>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space size="small" wrap>
              <Button
                type="link"
                size="small"
                @click="openBindingDrawer(record)"
                >绑定供应商</Button
              >
              <Button type="link" size="small" @click="openEditModal(record)"
                >编辑</Button
              >
              <Button
                type="link"
                size="small"
                @click="openDocumentDrawer(record)"
                >资源资料</Button
              >
              <Button
                danger
                type="link"
                size="small"
                @click="confirmDelete(record)"
                >删除</Button
              >
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑资源' : '新增资源'"
      width="960px"
      ok-text="保存"
      cancel-text="取消"
      @ok="saveRecord"
    >
      <Form :model="formState" class="resource-modal-form" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="资源分类" required>
            <Select
              v-model:value="formState.resourceType"
              :options="resourceTypeOptions"
            />
          </Form.Item>
          <Form.Item label="资源名称" required>
            <Input v-model:value="formState.resourceName" allow-clear />
          </Form.Item>
          <Form.Item v-if="formState.resourceType !== 'vehicle'" label="所在地">
            <Cascader
              v-model:value="formRegionPath"
              allow-clear
              change-on-select
              :options="regionOptions"
              placeholder="可选择省 / 市 / 区县"
              show-search
            />
          </Form.Item>
          <Form.Item label="联系电话">
            <Input v-model:value="formState.phone" allow-clear />
          </Form.Item>
          <Form.Item label="联系人">
            <Input v-model:value="formState.contactName" allow-clear />
          </Form.Item>
          <Form.Item label="传真号码">
            <Input v-model:value="formState.fax" allow-clear />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="formState.status" :options="statusOptions" />
          </Form.Item>
          <Form.Item label="默认采购属性" required>
            <Select
              v-model:value="formState.procurementMode"
              :options="procurementModeOptions"
            />
          </Form.Item>
          <Form.Item v-if="!editingId" label="供应商">
            <Checkbox v-model:checked="formState.autoCreateSupplier">
              同时生成默认供应商
            </Checkbox>
            <div class="muted">
              供应商名称默认使用资源名称，暂不生成报价，保存后可继续补充成本价。
            </div>
          </Form.Item>
        </div>
        <div v-if="supportsStarLevel(formState.resourceType) || supportsCategoryTags(formState.resourceType)" class="modal-grid">
          <Form.Item v-if="supportsStarLevel(formState.resourceType)" label="星级/接待标准">
            <Select v-model:value="formState.starLevel" :options="starLevelOptions" />
          </Form.Item>
          <Form.Item v-if="supportsCategoryTags(formState.resourceType)" label="分类">
            <Select
              v-model:value="formCategoryTags"
              mode="multiple"
              :options="categoryOptions(formState.resourceType)"
              placeholder="可多选"
            />
          </Form.Item>
        </div>
        <Form.Item v-if="!isPlaceForm && formState.resourceType !== 'vehicle'" label="所在地址">
          <Input v-model:value="formState.address" allow-clear />
        </Form.Item>
        <section v-if="formState.resourceType === 'restaurant'" class="scenic-section">
          <div class="section-title">餐厅信息</div>
          <div class="scenic-form-grid">
            <Form.Item label="容纳人数">
              <InputNumber v-model:value="formState.capacity" class="full-width" :min="0" :precision="0" />
            </Form.Item>
            <Form.Item label="餐桌数量">
              <InputNumber v-model:value="formState.tableCount" class="full-width" :min="0" :precision="0" />
            </Form.Item>
          </div>
          <Form.Item label="团餐标准">
            <Textarea v-model:value="formState.mealStandard" :rows="2" placeholder="例如：10人/桌，8菜1汤" />
          </Form.Item>
        </section>
        <section v-if="formState.resourceType === 'shopping'" class="scenic-section">
          <div class="section-title">购物信息</div>
          <Form.Item label="最大接待人数">
            <InputNumber v-model:value="formState.capacity" class="full-width" :min="0" :precision="0" />
          </Form.Item>
        </section>
        <section v-if="formState.resourceType === 'vehicle'" class="scenic-section">
          <div class="section-title">用车信息</div>
          <div class="scenic-form-grid">
            <Form.Item label="车辆类型">
              <Input v-model:value="formState.vehicleType" allow-clear placeholder="例如：商务车 / 中巴 / 大巴" />
            </Form.Item>
            <Form.Item label="座位数" required>
              <InputNumber v-model:value="formState.seatCount" class="full-width" :min="0" :precision="0" />
            </Form.Item>
            <Form.Item label="计费模式">
              <Select v-model:value="formState.billingMode" allow-clear :options="billingModeOptions" />
            </Form.Item>
          </div>
        </section>
        <section v-if="formState.resourceType === 'traffic'" class="scenic-section">
          <div class="section-title">大交通信息</div>
          <Form.Item label="服务范围">
            <Input v-model:value="formState.serviceArea" allow-clear placeholder="例如：全国票务、华东高铁票" />
          </Form.Item>
          <Form.Item label="服务说明">
            <Textarea v-model:value="formState.includedItems" :rows="2" />
          </Form.Item>
        </section>
        <section v-if="formState.resourceType === 'ground_agent'" class="scenic-section">
          <div class="section-title">地接信息</div>
          <div class="scenic-form-grid">
            <Form.Item label="服务地区" required>
              <Input v-model:value="formState.serviceArea" allow-clear placeholder="例如：杭州、苏州、华东" />
            </Form.Item>
            <Form.Item label="参考天数">
              <InputNumber v-model:value="formState.referenceDays" class="full-width" :min="0" :precision="0" />
            </Form.Item>
          </div>
          <Form.Item label="包含内容">
            <Textarea v-model:value="formState.includedItems" :rows="2" />
          </Form.Item>
          <Form.Item label="不包含内容">
            <Textarea v-model:value="formState.excludedItems" :rows="2" />
          </Form.Item>
        </section>
        <section v-if="formState.resourceType === 'other'" class="scenic-section">
          <div class="section-title">其它资源信息</div>
          <div class="scenic-form-grid">
            <Form.Item label="默认单位">
              <Input v-model:value="formState.resourceUnit" allow-clear placeholder="例如：元/人、元/份、元/团" />
            </Form.Item>
          </div>
          <Form.Item label="资源说明">
            <Textarea v-model:value="formState.includedItems" :rows="2" />
          </Form.Item>
        </section>
        <section v-if="isPlaceForm" class="scenic-section">
          <div class="section-title">{{ isScenicForm ? '景区信息' : '位置与营业信息' }}</div>
          <div class="scenic-form-grid">
            <Form.Item v-if="isScenicForm" label="国家 A 级">
              <Select
                v-model:value="formState.scenicLevel"
                :options="scenicLevelOptions"
              />
            </Form.Item>
            <Form.Item label="营业状态">
              <Select
                v-model:value="formState.businessStatus"
                :options="businessStatusOptions"
              />
            </Form.Item>
            <Form.Item label="开始营业时间">
              <TimePicker
                v-model:value="formState.openingTime"
                allow-clear
                class="full-width"
                format="HH:mm"
                value-format="HH:mm:ss"
              />
            </Form.Item>
            <Form.Item label="结束营业时间">
              <TimePicker
                v-model:value="formState.closingTime"
                allow-clear
                class="full-width"
                format="HH:mm"
                value-format="HH:mm:ss"
              />
            </Form.Item>
            <Form.Item label="踩点状态">
              <Select
                v-model:value="formState.siteVisitStatus"
                :options="siteVisitStatusOptions"
                @change="handleSiteVisitStatusChange"
              />
            </Form.Item>
            <Form.Item
              label="最近踩点日期"
              :required="formState.siteVisitStatus === 'visited'"
            >
              <DatePicker
                v-model:value="formState.lastSiteVisitDate"
                allow-clear
                class="full-width"
                :disabled="formState.siteVisitStatus !== 'visited'"
                :disabled-date="disableFutureDate"
                value-format="YYYY-MM-DD"
              />
            </Form.Item>
          </div>
          <Form.Item label="踩点备注">
            <Textarea v-model:value="formState.siteVisitNote" :rows="2" />
          </Form.Item>

          <div class="section-title map-section-title">地图位置</div>
          <div class="coordinate-grid">
            <Form.Item label="经度">
              <InputNumber
                v-model:value="formState.longitude"
                allow-clear
                class="full-width"
                :max="180"
                :min="-180"
                :precision="7"
                @change="syncScenicMapFromCoordinates"
              />
            </Form.Item>
            <Form.Item label="纬度">
              <InputNumber
                v-model:value="formState.latitude"
                allow-clear
                class="full-width"
                :max="90"
                :min="-90"
                :precision="7"
                @change="syncScenicMapFromCoordinates"
              />
            </Form.Item>
          </div>
          <Form.Item label="详细地址">
            <AutoComplete
              v-model:value="formState.address"
              :filter-option="false"
              :options="scenicMapTipOptions"
              @search="handleScenicMapSearch"
              @select="handleScenicMapSelect"
            >
              <Input.Search
                allow-clear
                enter-button="定位"
                :loading="scenicMapTipLoading"
                placeholder="可手写详细地址，输入后可定位"
                @search="locateScenicMapByAddress"
              />
            </AutoComplete>
          </Form.Item>
          <Spin
            :spinning="scenicMapLoading"
            wrapper-class-name="scenic-map-spin"
          >
            <div class="scenic-map-shell">
              <div
                ref="scenicMapContainerRef"
                class="scenic-map-container"
              ></div>
              <div
                v-if="scenicMapError"
                class="scenic-map-state scenic-map-error"
              >
                {{ scenicMapError }}
              </div>
              <div v-else-if="!scenicMapReady" class="scenic-map-state">
                地图加载中
              </div>
            </div>
          </Spin>
        </section>
        <Form.Item label="温馨提示">
          <Textarea v-model:value="formState.warmTip" :rows="3" />
        </Form.Item>
        <Form.Item label="简介">
          <Textarea v-model:value="formState.introduction" :rows="3" />
        </Form.Item>
        <Form.Item label="备注">
          <Textarea v-model:value="formState.remark" :rows="2" />
        </Form.Item>
      </Form>
    </Modal>

    <Drawer
      v-model:open="bindingDrawerOpen"
      width="960"
      :body-style="{ padding: '16px 20px 20px' }"
      :title="`供应商绑定 - ${currentResource?.resourceName || ''}`"
    >
      <div class="supplier-drawer-summary">
        <div class="supplier-drawer-summary-title">
          {{ currentResource?.resourceName || '-' }}
        </div>
        <Space size="small" wrap>
          <Tag :color="typeColor(currentResource?.resourceType)">
            {{ typeLabel(currentResource?.resourceType) }}
          </Tag>
          <Tag color="blue">
            已绑定 {{ currentResource?.boundSupplierCount || bindings.length || 0 }} 家
          </Tag>
          <Tag v-if="currentResource?.resourceType === 'scenic'" color="cyan">
            景区供应商支持模板配置
          </Tag>
        </Space>
      </div>

      <Card size="small" class="supplier-binding-card mb-4">
        <Tabs
          v-model:active-key="supplierBindingMode"
          :animated="false"
          class="supplier-binding-tabs"
        >
          <Tabs.TabPane key="bind" tab="绑定已有供应商">
            <div class="supplier-mode-hint">
              供应商档案已存在时使用。这里只建立当前资源与供应商的绑定关系，不会重复创建供应商。
            </div>
            <Form :model="bindForm" layout="vertical">
          <div class="bind-grid">
            <Form.Item label="供应商" required>
              <Select
                v-model:value="bindForm.supplierId"
                show-search
                :filter-option="true"
                :options="supplierOptions"
                placeholder="直接输入供应商名称搜索"
                @search="(value) => supplierSearchKeyword = value"
                @keyup.enter="selectSupplierFromTypedName"
              >
                <template #notFoundContent>
                  <span class="muted">没有匹配的已有供应商</span>
                </template>
              </Select>
              <div class="supplier-manual-entry">
                输入名称即可筛选；名称完全匹配或仅一条结果时，按 Enter 可直接选中。
              </div>
            </Form.Item>
            <Form.Item label="默认供应商">
              <Checkbox v-model:checked="bindForm.isDefault">
                设为当前资源默认供应商
              </Checkbox>
            </Form.Item>
          </div>
              <div class="bind-actions">
                <Button type="primary" @click="bindSupplier">
                  绑定已有供应商
                </Button>
              </div>
            </Form>
          </Tabs.TabPane>

          <Tabs.TabPane key="create" :tab="supplierCreateTabTitle">
            <div class="supplier-mode-hint">
              {{ isVehicleResourceBinding
                ? '车队尚未录入系统时使用。保存后会创建车队档案并绑定当前车型；价格在真实排团时确认。'
                : '供应商尚未录入系统时使用。保存档案和报价后，会自动绑定当前资源。' }}
            </div>
            <Form :model="scenicSupplierForm" layout="vertical">
          <div class="scenic-supplier-grid">
            <Form.Item label="供应商名称" required>
              <Input
                v-model:value="scenicSupplierForm.supplierName"
                allow-clear
                placeholder="请输入供应商名称"
              />
            </Form.Item>
            <Form.Item label="状态">
              <Select
                v-model:value="scenicSupplierForm.status"
                :options="[
                  { label: '合作中', value: 'active' },
                  { label: '停用', value: 'disabled' },
                  { label: '黑名单', value: 'blacklisted' },
                ]"
              />
            </Form.Item>
            <Form.Item label="所在地">
              <Cascader
                v-model:value="scenicSupplierRegionPath"
                allow-clear
                change-on-select
                :options="regionOptions"
                placeholder="默认带入资源所在地，可手动修改"
                show-search
              />
            </Form.Item>
            <Form.Item label="联系人">
              <Input
                v-model:value="scenicSupplierForm.contactName"
                allow-clear
              />
            </Form.Item>
            <Form.Item label="联系电话">
              <Input
                v-model:value="scenicSupplierForm.contactPhone"
                allow-clear
              />
            </Form.Item>
            <Form.Item label="分类">
              <Input :value="currentResourceTypeLabel" disabled />
            </Form.Item>
            <Form.Item label="默认供应商">
              <Checkbox v-model:checked="scenicSupplierForm.isDefault">
                设为当前资源默认供应商
              </Checkbox>
            </Form.Item>
          </div>
          <Form.Item label="基础信息">
            <Textarea
              v-model:value="scenicSupplierForm.basicInfo"
              :rows="2"
              placeholder="可记录供应商简介、接待能力、结算要求等"
            />
          </Form.Item>
          <div v-if="isVehicleResourceBinding" class="drawer-card-hint vehicle-pricing-hint">
            用车资源不保存固定价格。产品阶段仅选择车型和车队，真实排团时再按实际线路向车队询价。
          </div>
          <Form.Item v-if="!isVehicleResourceBinding" label="报价方式" required>
            <Select
              v-model:value="scenicSupplierForm.priceMode"
              :options="[
                { label: '统一报价', value: 'unified' },
                { label: '分类报价', value: 'classified' },
              ]"
            />
          </Form.Item>
          <Form.Item
            v-if="!isVehicleResourceBinding && scenicSupplierForm.priceMode === 'unified'"
            :label="unifiedPriceLabel"
          >
            <InputNumber
              v-model:value="scenicSupplierForm.unifiedPrice"
              :addon-after="unifiedPriceUnit"
              class="full-width"
              :min="0"
              :precision="2"
            />
          </Form.Item>
          <div
            v-else-if="!isVehicleResourceBinding"
            class="scenic-supplier-price-grid"
          >
            <Form.Item
              v-for="project in resourcePriceProjects"
              :key="project.id"
              :label="project.projectName"
            >
              <InputNumber
                v-model:value="resourceSupplierPriceValues[project.id]"
                addon-after="元"
                class="full-width"
                :min="0"
                :precision="2"
              />
            </Form.Item>
            <div v-if="resourcePriceProjects.length === 0" class="drawer-card-hint">
              当前资源类型还没有报价项目，请先到费用项目中维护。
            </div>
          </div>
          <div v-if="currentResource?.resourceType === 'scenic'" class="drawer-card-hint ticket-price-optional-hint">
            仅有自费项目、不含门票的景区：门票报价可留空，但至少要维护一条自费项目报价。
          </div>
          <div
            v-if="currentResource?.resourceType === 'scenic'"
            class="optional-items-section"
          >
            <div class="optional-items-header">
              <div>
                <div class="section-title">自费项目报价</div>
                <div class="drawer-card-hint">
                  成本价仅供内部核算；建议对外价会作为产品设计的默认值，最终 Word 价格以具体产品为准，计价单位固定为元/人。
                </div>
              </div>
              <Button type="dashed" @click="addOptionalItem">新增报价</Button>
            </div>
            <div
              v-if="scenicSupplierForm.optionalItems?.length"
              class="optional-item-list"
            >
              <div class="optional-item-row optional-item-row-header">
                <span>项目名称</span>
                <span>供应商成本价</span>
                <span>建议对外自费价</span>
                <span>价格说明</span>
                <span>状态</span>
                <span>操作</span>
              </div>
              <div
                v-for="(item, index) in scenicSupplierForm.optionalItems"
                :key="`optional-item-${index}`"
                class="optional-item-row"
              >
                <Input
                  v-model:value="item.projectName"
                  :maxlength="200"
                  placeholder="直接填写，例如：景区电瓶车"
                />
                <InputNumber
                  v-model:value="item.costPrice"
                  :addon-after="'元/人'"
                  class="full-width"
                  :min="0"
                  :precision="2"
                />
                <InputNumber
                  v-model:value="item.suggestedSalePrice"
                  :addon-after="'元/人'"
                  class="full-width"
                  :min="0"
                  :precision="2"
                  placeholder="产品默认价"
                />
                <Input
                  v-model:value="item.priceDescription"
                  allow-clear
                  placeholder="可选，例如：自愿参加"
                />
                <Select
                  v-model:value="item.status"
                  :options="[
                    { label: '启用', value: 'active' },
                    { label: '停用', value: 'disabled' },
                  ]"
                />
                <Button type="link" danger @click="removeOptionalItem(index)">
                  删除
                </Button>
              </div>
            </div>
            <div v-else class="drawer-card-hint optional-items-empty">
              暂未维护自费项目报价。点击“新增报价”后，直接填写项目名称并录入价格。
            </div>
          </div>
          <Form.Item v-if="!isVehicleResourceBinding" label="报价备注">
            <Textarea
              v-model:value="scenicSupplierForm.priceRemark"
              :rows="2"
              placeholder="例如：平日价、节假日另议、需提前预约"
            />
          </Form.Item>
          <Form.Item label="备注">
            <Textarea v-model:value="scenicSupplierForm.remark" :rows="2" />
          </Form.Item>
              <div class="bind-actions">
                <Space>
                  <Button
                    type="primary"
                    :loading="creatingScenicSupplier"
                    @click="createScenicSupplier"
                  >
                    {{ isEditingScenicSupplier ? '保存修改' : '保存并绑定' }}
                  </Button>
                  <Button
                    v-if="isEditingScenicSupplier"
                    @click="resetScenicSupplierForm(currentResource)"
                  >
                    取消编辑
                  </Button>
                </Space>
              </div>
            </Form>
          </Tabs.TabPane>
        </Tabs>
      </Card>

      <div class="supplier-binding-list-header">
        <div class="supplier-binding-list-title">当前已绑定供应商</div>
        <div class="supplier-binding-list-count">共 {{ bindings.length }} 家</div>
      </div>
      <Table
        :data-source="bindings"
        :loading="bindingLoading"
        :pagination="false"
        row-key="relationId"
        size="small"
      >
        <Table.Column
          title="供应商"
          data-index="supplierName"
          key="supplierName"
        />
        <Table.Column title="默认" data-index="isDefault" key="isDefault" width="80">
          <template #default="{ record }">
            <Tag v-if="record.isDefault" color="blue">默认</Tag>
            <span v-else class="muted">-</span>
          </template>
        </Table.Column>
        <Table.Column title="状态" data-index="status" key="status" width="90">
          <template #default="{ record }">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ record.status === 'active' ? '有效' : record.status }}
            </Tag>
          </template>
        </Table.Column>
        <Table.Column
          v-if="currentResource?.resourceType === 'scenic'"
          title="自费项目"
          key="optionalItems"
          width="240"
        >
          <template #default="{ record }">
            <Space v-if="record.optionalItems?.length" size="small" wrap>
              <Tag
                v-for="item in record.optionalItems"
                :key="item.id || item.projectName"
                color="orange"
              >
                {{ item.projectName }} 成本 {{ item.costPrice }}元/人
                <template v-if="item.suggestedSalePrice !== undefined">
                  · 建议 {{ item.suggestedSalePrice }}元/人
                </template>
              </Tag>
            </Space>
            <span v-else class="muted">-</span>
          </template>
        </Table.Column>
        <Table.Column title="操作" key="action" width="220">
          <template #default="{ record }">
            <Space size="small">
              <Button type="link" size="small" @click="editBoundSupplier(record)">
                编辑
              </Button>
              <Button
                v-if="currentResource?.resourceType === 'scenic'"
                type="link"
                size="small"
                @click="openTemplateDrawer(record)"
              >
                模板配置
              </Button>
              <Button danger type="link" size="small" @click="confirmUnbindSupplier(record)">
                解除绑定
              </Button>
            </Space>
          </template>
        </Table.Column>
      </Table>
    </Drawer>

    <Drawer
      v-model:open="templateDrawerOpen"
      width="980"
      :body-style="{ padding: '16px 20px 20px' }"
      :title="`游客名单模板配置 - ${currentResource?.resourceName || ''} / ${currentBinding?.supplierName || ''}`"
    >
      <div v-if="templateLoading" class="template-loading">正在读取模板配置...</div>
      <template v-else>
        <Card size="small" class="mb-4">
          <Form :model="templateForm" layout="vertical">
            <div class="template-grid">
              <Form.Item label="模板名称" required>
                <Input v-model:value="templateForm.templateName" placeholder="例如：景区游客名单模板" />
              </Form.Item>
              <Form.Item label="模板文件" required>
                <Upload
                  accept=".xlsx,.xls"
                  :before-upload="handleTemplateUpload"
                  :max-count="1"
                  :show-upload-list="false"
                >
                  <Button>上传 Excel 模板</Button>
                </Upload>
                <div v-if="templateForm.originalFilename" class="template-file-name">
                  {{ templateForm.originalFilename }}
                </div>
              </Form.Item>
              <Form.Item label="状态">
                <Select v-model:value="templateForm.status" :options="templateStatusOptions" />
              </Form.Item>
              <Form.Item label="表头行">
                <InputNumber v-model:value="templateForm.headerRow" class="w-full" :min="1" :precision="0" />
              </Form.Item>
              <Form.Item label="数据开始行">
                <InputNumber v-model:value="templateForm.dataStartRow" class="w-full" :min="2" :precision="0" />
              </Form.Item>
              <Form.Item label="工作表">
                <Input v-model:value="templateForm.sheetName" placeholder="读取表头后自动带出" />
              </Form.Item>
            </div>
            <Space wrap>
              <Button
                :loading="templateHeaderLoading"
                :disabled="!templateForm.attachmentId"
                @click="loadTemplateHeaders"
              >
                读取表头
              </Button>
              <Button type="primary" :loading="templateSaving" @click="saveTemplate">保存模板</Button>
              <Button danger @click="confirmDeleteTemplate">删除模板</Button>
            </Space>
          </Form>
        </Card>

        <Card size="small" title="字段映射">
          <Table
            :columns="templateColumns"
            :data-source="templateForm.fields"
            :pagination="false"
            row-key="columnIndex"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'fillMode'">
                <Select
                  v-model:value="(record as TemplateFieldRow).fillMode"
                  class="w-full"
                  :options="fillModeOptions"
                  placeholder="请选择"
                  @change="() => {
                    if ((record as TemplateFieldRow).fillMode !== 'tourist_field') {
                      (record as TemplateFieldRow).systemField = undefined;
                    }
                    if ((record as TemplateFieldRow).fillMode !== 'constant') {
                      (record as TemplateFieldRow).fixedValue = undefined;
                    }
                  }"
                />
              </template>
              <template v-else-if="column.key === 'systemField'">
                <Select
                  v-if="(record as TemplateFieldRow).fillMode === 'tourist_field'"
                  v-model:value="(record as TemplateFieldRow).systemField"
                  allow-clear
                  class="w-full"
                  :options="systemFieldOptions"
                  placeholder="请选择系统字段"
                />
                <Input
                  v-else-if="(record as TemplateFieldRow).fillMode === 'constant'"
                  v-model:value="(record as TemplateFieldRow).fixedValue"
                  placeholder="例如：成人、身份证、IC"
                />
                <Tag v-else-if="(record as TemplateFieldRow).fillMode === 'sequence'" color="blue">
                  导出时自动生成 1、2、3
                </Tag>
                <Tag v-else color="default">
                  保留模板原内容
                </Tag>
              </template>
              <template v-else-if="column.key === 'required'">
                <Switch
                  v-model:checked="(record as TemplateFieldRow).required"
                  checked-children="是"
                  un-checked-children="否"
                />
              </template>
            </template>
          </Table>
          <div class="template-help">
            填充方式支持游客字段、自动序号、固定值和不填充。未配置字段可直接保留模板原内容。
          </div>
        </Card>

        <Card size="small" class="mt-4" title="备注">
          <Textarea v-model:value="templateForm.remark" :auto-size="{ minRows: 3, maxRows: 5 }" />
        </Card>
      </template>
    </Drawer>

    <Drawer
      v-model:open="documentDrawerOpen"
      width="960"
      :title="`资源资料 - ${currentResource?.resourceName || ''}`"
    >
      <Tabs v-model:activeKey="materialTab" @change="handleMaterialTabChange">
        <Tabs.TabPane
          key="introductions"
          :disabled="materialServiceUnavailable"
          :tab="`介绍素材 (${introductions.length})`"
        >
          <Spin :spinning="introductionLoading">
            <div class="resource-material-intro-layout">
              <section class="resource-material-intro-list">
                <div class="resource-material-section-header">
                  <span>介绍版本</span>
                  <Space :size="0">
                    <Button type="link" size="small" @click="startNewIntroduction">
                      新增
                    </Button>
                    <Button
                      danger
                      type="link"
                      size="small"
                      :disabled="!activeIntroduction"
                      @click="handleDeleteActiveIntroduction"
                    >
                      删除
                    </Button>
                  </Space>
                </div>
                <div class="resource-material-intro-order-hint" role="note">
                  <IconifyIcon aria-hidden="true" icon="lucide:grip-vertical" />
                  <span>按住左侧图标拖拽排序</span>
                </div>
                <div
                  v-if="introductions.length"
                  ref="introductionSortableContainerRef"
                  class="resource-material-intro-items"
                  :class="{ 'is-reordering': introductionReordering }"
                  :aria-busy="introductionReordering"
                  role="list"
                >
                  <div
                    v-for="item in introductions"
                    :key="item.id"
                    class="resource-material-intro-row"
                    role="listitem"
                  >
                    <span
                      class="resource-material-intro-drag-handle"
                      :aria-label="`拖动「${item.title}」调整介绍顺序`"
                      role="img"
                      :title="`拖动「${item.title}」调整介绍顺序`"
                    >
                      <IconifyIcon aria-hidden="true" icon="lucide:grip-vertical" />
                    </span>
                    <button
                      class="resource-material-intro-item"
                      :class="{ active: item.id === activeIntroductionId }"
                      :aria-current="item.id === activeIntroductionId ? 'true' : undefined"
                      type="button"
                      @click="selectIntroduction(item)"
                    >
                      <span class="resource-material-intro-title" :title="item.title">
                        {{ item.title }}
                      </span>
                      <span class="resource-material-intro-item-tags">
                        <Tag
                          v-if="item.isOptionalItem"
                          color="orange"
                          :title="item.resourceOptionalItemName ? `自费项目：${item.resourceOptionalItemName}` : '自费项目'"
                        >
                          自费
                        </Tag>
                        <Tag :color="reviewStatusColor(item.status)">
                          {{ reviewStatusLabel(item.status) }}
                        </Tag>
                      </span>
                    </button>
                  </div>
                </div>
                <div v-else class="muted resource-material-empty">
                  还没有介绍素材，可新增不同用途的介绍版本。
                </div>
              </section>

              <section class="resource-material-intro-editor">
                <Form :model="introductionForm" layout="vertical">
                  <Form.Item label="介绍名称" required>
                    <Input
                      v-model:value="introductionForm.title"
                      :maxlength="160"
                      placeholder="例如：适用于秋季产品的景区介绍"
                      show-count
                    />
                  </Form.Item>
                  <Form.Item label="素材类型">
                    <Radio.Group
                      v-model:value="introductionForm.isOptionalItem"
                      @change="handleIntroductionTypeChange(Boolean(introductionForm.isOptionalItem))"
                    >
                      <Radio :value="false">常规介绍</Radio>
                      <Radio
                        :disabled="currentResource?.resourceType !== 'scenic'"
                        :value="true"
                      >
                        自费项目介绍
                      </Radio>
                    </Radio.Group>
                  </Form.Item>
                  <Form.Item
                    v-if="introductionForm.isOptionalItem"
                    label="关联自费项目"
                    required
                    extra="素材只维护介绍内容，不展示或写入供应商成本。"
                  >
                    <Select
                      v-model:value="introductionForm.resourceOptionalItemId"
                      :loading="optionalItemMasterLoading"
                      :options="resourceOptionalItemOptions"
                      placeholder="请选择当前资源的自费项目"
                      show-search
                    />
                  </Form.Item>
                  <Form.Item label="适用标签">
                    <Select
                      v-model:value="introductionForm.tags"
                      mode="tags"
                      :max-tag-count="4"
                      placeholder="例如：亲子、深度游、秋季"
                      :token-separators="[',', '，']"
                    />
                  </Form.Item>
                  <Form.Item
                    label="当前景点图片（选填）"
                    extra="只显示当前介绍素材已选图片；不同景点的图片分别维护，下方排序即产品 Word 的输出顺序。"
                  >
                    <Spin :spinning="introductionImageSelectionLoading">
                      <div class="resource-material-intro-image-toolbar">
                        <span>已选 {{ introductionImageIds.length }} 张</span>
                        <Space size="small" wrap>
                          <Button size="small" @click="toggleIntroductionImagePicker">
                            {{ introductionImagePickerOpen ? '收起图库' : '从图库添加' }}
                          </Button>
                          <Button size="small" @click="openImageMaterialLibrary">
                            管理资源图库
                          </Button>
                          <Upload
                            multiple
                            :before-upload="beforeUploadIntroductionImage"
                            :show-upload-list="false"
                          >
                            <Button size="small" :loading="uploadingImages">
                              上传并选用
                            </Button>
                          </Upload>
                        </Space>
                      </div>
                      <div
                        v-if="selectedIntroductionImages.length"
                        class="resource-material-intro-selected-images"
                      >
                        <div class="resource-material-intro-image-options">
                          <div
                            v-for="(image, index) in selectedIntroductionImages"
                            :key="image.id"
                            class="resource-material-intro-image-option selected"
                          >
                            <button
                              class="resource-material-intro-image-preview"
                              type="button"
                              @click="previewResourceImage(image)"
                            >
                              <img
                                v-if="imagePreviewUrls[image.id]"
                                :src="imagePreviewUrls[image.id]"
                                :alt="image.originalFilename"
                              />
                              <span v-else class="resource-material-intro-image-placeholder">暂无预览</span>
                            </button>
                            <div class="resource-material-intro-image-label" :title="image.originalFilename">
                              {{ image.originalFilename }}
                            </div>
                            <div class="resource-material-intro-image-order">
                              <span>第 {{ index + 1 }} 张</span>
                              <Space :size="0">
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="index === 0"
                                  @click="moveIntroductionImage(index, -1)"
                                >
                                  上移
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="index === selectedIntroductionImages.length - 1"
                                  @click="moveIntroductionImage(index, 1)"
                                >
                                  下移
                                </Button>
                                <Button
                                  danger
                                  size="small"
                                  type="link"
                                  @click="toggleIntroductionImage(image.id, false)"
                                >
                                  移除
                                </Button>
                              </Space>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div v-else class="resource-material-intro-image-empty">
                        当前景点还没有图片。可直接“上传并选用”，或从资源图库选择对应景点图片。
                      </div>
                      <div v-if="introductionImagePickerOpen" class="resource-material-intro-image-picker">
                        <div class="resource-material-intro-image-picker-head">
                          <span>从「{{ currentResource?.resourceName || '当前资源' }}」图片库添加</span>
                          <span>图片库只是存放原图；勾选后才归属当前景点</span>
                        </div>
                        <div v-if="images.length" class="resource-material-intro-image-options">
                          <div
                            v-for="image in images"
                            :key="image.id"
                            class="resource-material-intro-image-option"
                            :class="{ selected: introductionImageIds.includes(image.id) }"
                          >
                            <Checkbox
                              :checked="introductionImageIds.includes(image.id)"
                              @change="toggleIntroductionImage(image.id, $event.target.checked)"
                            />
                            <button
                              class="resource-material-intro-image-preview"
                              type="button"
                              @click="previewResourceImage(image)"
                            >
                              <img
                                v-if="imagePreviewUrls[image.id]"
                                :src="imagePreviewUrls[image.id]"
                                :alt="image.originalFilename"
                              />
                              <span v-else class="resource-material-intro-image-placeholder">暂无预览</span>
                            </button>
                            <div class="resource-material-intro-image-label" :title="image.originalFilename">
                              {{ image.originalFilename }}
                            </div>
                          </div>
                        </div>
                        <div v-else class="resource-material-intro-image-empty">
                          资源图库暂时没有图片，可直接上传当前景点图片。
                        </div>
                      </div>
                    </Spin>
                  </Form.Item>
                  <Form.Item label="游览时间（分钟）" extra="只填写分钟数字；产品预览会自动转换为“游览约 X 分钟/小时”。">
                    <Input
                      v-model:value="introductionForm.visitDuration"
                      :maxlength="6"
                      inputmode="numeric"
                      pattern="[0-9]*"
                      addon-after="分钟"
                      placeholder="例如：120"
                      @input="handleVisitDurationInput"
                      show-count
                    />
                  </Form.Item>
                  <Form.Item label="介绍正文" required>
                    <Textarea
                      v-model:value="introductionForm.content"
                      :auto-size="{ minRows: 12, maxRows: 20 }"
                      :maxlength="50000"
                      placeholder="录入可直接用于旅游产品、行程说明和确认单的介绍内容"
                      show-count
                    />
                  </Form.Item>
                  <Form.Item
                    class="resource-material-intro-form-item resource-material-intro-warm-tip-item"
                    label="温馨提示（选填）"
                    extra="内容在产品预览中固定显示为蓝色；可直接输入多行并按换行保存。"
                  >
                    <Textarea
                      v-model:value="introductionForm.warmTipContent"
                      :auto-size="{ minRows: 4, maxRows: 10 }"
                      :maxlength="5000"
                      placeholder="可直接输入完整温馨提示；换行、①②或1、2等格式按原样保留"
                      show-count
                    />
                  </Form.Item>
                  <Form.Item
                    class="resource-material-intro-form-item resource-material-intro-notice-item"
                    label="注意事项（选填）"
                    extra="一行一条，生成产品资料时会以红色强调。"
                  >
                    <Textarea
                      v-model:value="introductionForm.noticeContent"
                      :auto-size="{ minRows: 3, maxRows: 8 }"
                      :maxlength="5000"
                      placeholder="例如：\n请提前预约\n雨天请备雨具"
                      show-count
                    />
                    <div
                      v-if="introductionForm.noticeContent?.trim()"
                      class="resource-material-intro-notice-preview"
                      role="note"
                    >
                      <div class="resource-material-intro-notice-preview-title">注意事项预览</div>
                      <div class="resource-material-intro-notice-preview-content">{{ introductionForm.noticeContent }}</div>
                    </div>
                  </Form.Item>
                  <Form.Item
                    class="resource-material-intro-form-item resource-material-intro-extension-item"
                    label="扩展内容模块（选填）"
                    extra="用于产品介绍正文后的固定样式内容；内容统一使用文本框，可上下调整模块顺序。"
                  >
                    <div class="introduction-extension-toolbar">
                      <Button size="small" @click="addIntroductionExtensionBlock">+ 新增扩展模块</Button>
                    </div>
                    <div v-if="introductionForm.extensionBlocks?.length" class="introduction-extension-blocks">
                      <section
                        v-for="(block, blockIndex) in introductionForm.extensionBlocks"
                        :key="`${block.type}-${blockIndex}`"
                        class="introduction-extension-block"
                      >
                        <div class="introduction-extension-block-head introduction-extension-block-main">
                          <span class="introduction-extension-title-label">模块标题</span>
                          <Input v-model:value="block.title" :maxlength="100" class="introduction-extension-title" placeholder="自定义标题，例如：拍照机位推荐：" />
                          <label class="introduction-extension-color" title="选择标题颜色">
                            <span>颜色</span>
                            <input v-model="block.titleColor" type="color" />
                          </label>
                          <Space class="introduction-extension-actions" size="small">
                            <Button size="small" type="text" :disabled="blockIndex === 0" @click="moveIntroductionExtensionBlock(blockIndex, -1)"><IconifyIcon icon="lucide:chevron-up" /></Button>
                            <Button size="small" type="text" :disabled="blockIndex === (introductionForm.extensionBlocks?.length || 0) - 1" @click="moveIntroductionExtensionBlock(blockIndex, 1)"><IconifyIcon icon="lucide:chevron-down" /></Button>
                            <Button danger size="small" type="text" @click="removeIntroductionExtensionBlock(blockIndex)">删除模块</Button>
                          </Space>
                        </div>
                        <Textarea
                          v-model:value="block.content"
                          :auto-size="{ minRows: 4, maxRows: 10 }"
                          :maxlength="20000"
                          placeholder="按需要输入多行内容；序号和换行由你自己填写"
                          show-count
                        />
                      </section>
                    </div>
                    <div v-else class="muted introduction-extension-empty">可按需新增扩展内容；标题颜色和模块顺序可调整，正文在文本框中自行换行。</div>
                  </Form.Item>
                  <div class="resource-material-intro-footer">
                    <Space>
                      <Button :loading="introductionSaving" @click="() => saveIntroduction()">
                        保存草稿
                      </Button>
                      <Button
                        type="primary"
                        :loading="introductionPublishing"
                        @click="handlePublishIntroduction"
                      >
                        发布
                      </Button>
                    </Space>
                    <Space v-if="activeIntroduction" size="small">
                      <Tag :color="reviewStatusColor(activeIntroduction.status)">
                        {{ reviewStatusLabel(activeIntroduction.status) }}
                      </Tag>
                      <Tag :color="documentStatusColor(activeIntroduction.indexStatus)">
                        {{ indexStatusLabel(activeIntroduction.indexStatus) }}
                      </Tag>
                      <Button
                        v-if="activeIntroduction.status === 'published' && activeIntroduction.indexStatus === 'failed'"
                        type="link"
                        size="small"
                        @click="handleRetryIntroduction(activeIntroduction)"
                      >
                        重试向量化
                      </Button>
                    </Space>
                  </div>
                </Form>
              </section>
            </div>
          </Spin>
        </Tabs.TabPane>

        <Tabs.TabPane
          key="images"
          :disabled="materialServiceUnavailable"
          :tab="`资源图片库 (${images.length})`"
        >
          <Spin :spinning="imageLoading">
            <div class="resource-material-section-header">
              <span class="muted">这里存放当前资源的原始图片；每个介绍素材选择自己的图片，不会自动共用。</span>
              <Upload multiple :before-upload="beforeUploadImage" :show-upload-list="false">
                <Button type="primary" :loading="uploadingImages">上传图片</Button>
              </Upload>
            </div>
            <div v-if="images.length" class="resource-material-image-grid">
              <article v-for="item in images" :key="item.id" class="resource-material-image-card">
                <button
                  class="resource-material-image-preview"
                  type="button"
                  @click="previewResourceImage(item)"
                >
                  <img
                    v-if="imagePreviewUrls[item.id]"
                    :src="imagePreviewUrls[item.id]"
                    :alt="item.originalFilename"
                  />
                  <span v-else class="muted">正在加载图片</span>
                </button>
                <div class="resource-material-image-info">
                  <div class="resource-material-image-name" :title="item.originalFilename">
                    {{ item.originalFilename }}
                  </div>
                  <Space size="small" wrap>
                    <Tag v-if="item.isCover" color="blue">封面</Tag>
                    <Tag v-for="tag in item.tags" :key="tag">{{ tag }}</Tag>
                  </Space>
                  <div class="resource-material-image-actions">
                    <Button
                      v-if="!item.isCover"
                      type="link"
                      size="small"
                      @click="handleSetImageCover(item)"
                    >
                      设为封面
                    </Button>
                    <Button type="link" size="small" @click="openImageEditor(item)">
                      编辑
                    </Button>
                    <Button danger type="link" size="small" @click="handleDeleteImage(item)">
                      删除
                    </Button>
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="muted resource-material-empty">
              还没有产品配图。
            </div>
          </Spin>
        </Tabs.TabPane>

        <Tabs.TabPane key="files" :tab="`资源文件 (${documents.length})`">
          <Upload
            multiple
            :before-upload="beforeUploadDocument"
            :show-upload-list="false"
          >
            <Button type="primary" :loading="uploadingDocuments">上传资源文件</Button>
          </Upload>
          <Table
            class="mt-4"
            :data-source="documents"
            :loading="documentLoading"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <Table.Column
              title="文件名"
              data-index="originalFilename"
              key="originalFilename"
            />
            <Table.Column
              title="后缀"
              data-index="fileExt"
              key="fileExt"
              width="80"
            />
            <Table.Column title="大小" key="fileSize" width="90">
              <template #default="{ record }">
                {{ formatFileSize(record.fileSize) }}
              </template>
            </Table.Column>
            <Table.Column
              title="处理状态"
              data-index="processingStatus"
              key="processingStatus"
              width="110"
            >
              <template #default="{ record }">
                <Tag :color="documentStatusColor(record.processingStatus)">
                  {{ processingStatusLabel(record.processingStatus) }}
                </Tag>
              </template>
            </Table.Column>
            <Table.Column
              title="审核状态"
              data-index="reviewStatus"
              key="reviewStatus"
              width="110"
            >
              <template #default="{ record }">
                <Tag :color="reviewStatusColor(record.reviewStatus)">
                  {{ reviewStatusLabel(record.reviewStatus) }}
                </Tag>
              </template>
            </Table.Column>
            <Table.Column
              title="向量状态"
              data-index="indexStatus"
              key="indexStatus"
              width="110"
            >
              <template #default="{ record }">
                <Tag :color="documentStatusColor(record.indexStatus)">
                  {{ indexStatusLabel(record.indexStatus) }}
                </Tag>
              </template>
            </Table.Column>
            <Table.Column
              title="上传人"
              data-index="createdBy"
              key="createdBy"
              width="110"
            />
            <Table.Column
              title="上传时间"
              data-index="createdAt"
              key="createdAt"
              width="160"
            >
              <template #default="{ record }">
                {{ formatDate(record.createdAt) }}
              </template>
            </Table.Column>
            <Table.Column title="操作" key="action" width="220">
              <template #default="{ record }">
                <Button
                  v-if="isImageDocument(record)"
                  type="link"
                  size="small"
                  @click="previewDocument(record)"
                >
                  预览
                </Button>
                <Button type="link" size="small" @click="downloadDocument(record)">
                  下载
                </Button>
                <Button
                  v-if="record.reviewStatus !== 'published'"
                  type="link"
                  size="small"
                  @click="handlePublishDocument(record)"
                >
                  发布
                </Button>
                <Button
                  v-if="record.reviewStatus !== 'disabled'"
                  type="link"
                  size="small"
                  @click="handleDisableDocument(record)"
                >
                  停用
                </Button>
                <Button type="link" size="small" @click="handleRetryDocument(record)">
                  重试
                </Button>
                <Button danger type="link" size="small" @click="handleDeleteDocument(record)">
                  删除
                </Button>
              </template>
            </Table.Column>
          </Table>
        </Tabs.TabPane>
      </Tabs>
    </Drawer>

    <Modal
      v-model:open="imageEditorOpen"
      :confirm-loading="imageSaving"
      title="编辑图片素材"
      width="460px"
      @ok="saveImageEditor"
    >
      <Form :model="imageEditForm" layout="vertical">
        <Form.Item label="图片标签">
          <Select
            v-model:value="imageEditForm.tags"
            mode="tags"
            placeholder="例如：大门、夜景、亲子"
            :token-separators="[',', '，']"
          />
        </Form.Item>
        <Form.Item label="展示排序">
          <InputNumber v-model:value="imageEditForm.sortOrder" class="full-width" :min="0" :precision="0" />
        </Form.Item>
      </Form>
    </Modal>

    <Modal
      v-model:open="previewOpen"
      :footer="null"
      :title="currentDocument?.originalFilename || currentImage?.originalFilename || previewTitle"
      width="760px"
    >
      <Image v-if="previewUrl" :src="previewUrl" class="preview-image" />
    </Modal>
  </Page>
</template>

<style scoped>
.modal-grid,
.bind-grid,
.coordinate-grid,
.scenic-supplier-grid,
.scenic-supplier-price-grid,
.scenic-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.bind-grid {
  align-items: end;
}

.bind-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.cell-note,
.cell-primary {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-note {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.full-width {
  width: 100%;
}

.map-search-select {
  width: 100%;
  margin-bottom: 12px;
}

.muted {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.resource-name {
  color: #0f172a;
  font-weight: 600;
}

.resource-modal-form {
  max-height: calc(100vh - 220px);
  padding-right: 4px;
  overflow-y: auto;
}

.supplier-drawer-summary {
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.supplier-drawer-summary-title {
  margin-bottom: 6px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 600;
}

.supplier-binding-card :deep(.ant-card-body) {
  padding: 0 16px 16px;
}

.supplier-binding-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

.supplier-binding-tabs :deep(.ant-tabs-tab) {
  min-width: 168px;
  justify-content: center;
  font-weight: 500;
}

.supplier-mode-hint {
  padding: 10px 12px;
  margin-bottom: 16px;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.supplier-binding-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 0 10px;
}

.supplier-binding-list-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.supplier-binding-list-count {
  color: #64748b;
  font-size: 12px;
}

.drawer-card-hint {
  margin-bottom: 12px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.supplier-manual-entry {
  display: flex;
  align-items: center;
  gap: 2px;
  min-height: 24px;
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
}

.supplier-manual-entry :deep(.ant-btn) {
  padding-inline: 4px;
}

.scenic-supplier-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: end;
}

.optional-items-section {
  padding: 14px 0 4px;
  margin-top: 4px;
  border-top: 1px solid #e5e7eb;
}

.optional-items-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.optional-item-list {
  display: grid;
  gap: 8px;
  padding: 10px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}

.optional-item-row {
  display: grid;
  grid-template-columns: minmax(210px, 1.3fr) minmax(140px, 0.9fr) minmax(150px, 1fr) minmax(160px, 1.2fr) 96px 48px;
  align-items: center;
  gap: 8px;
}

.resource-material-optional-item-select {
  display: flex;
  align-items: center;
  gap: 4px;
}

.resource-material-optional-item-select :deep(.ant-select) {
  flex: 1;
  min-width: 0;
}

.optional-item-row-header {
  padding: 0 8px 2px;
  color: #64748b;
  font-size: 12px;
}

.optional-items-empty {
  padding: 8px 0 2px;
  margin-bottom: 0;
}

.template-grid {
  display: grid;
  grid-template-columns: 2fr 1.6fr 1fr;
  gap: 0 16px;
}

.template-file-name,
.template-help {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.template-loading {
  padding: 24px;
  color: #64748b;
}

.scenic-section {
  padding-top: 16px;
  margin-top: 4px;
  border-top: 1px solid #e5e7eb;
}

.section-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.map-section-title {
  padding-top: 2px;
}

.scenic-map-shell {
  position: relative;
  width: 100%;
  height: 320px;
  overflow: hidden;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.scenic-map-container {
  width: 100%;
  height: 100%;
}

.scenic-map-state {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #64748b;
  text-align: center;
  background: rgb(248 250 252 / 94%);
}

.scenic-map-error {
  color: #b45309;
}

:deep(.scenic-map-spin) {
  display: block;
}

.preview-image {
  max-height: 70vh;
  object-fit: contain;
  width: 100%;
}

.resource-material-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  gap: 12px;
}

.resource-material-intro-layout {
  display: grid;
  grid-template-columns: 272px minmax(0, 1fr);
  gap: 20px;
  min-height: 470px;
}

.resource-material-intro-list {
  padding-right: 20px;
  border-right: 1px solid #f0f0f0;
}

.resource-material-intro-items {
  display: grid;
  gap: 4px;
  margin-top: 10px;
}

.resource-material-intro-order-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  padding: 0 2px;
  color: #595959;
  font-size: 12px;
  line-height: 1.5;
}

.resource-material-intro-order-hint :deep(svg) {
  flex: 0 0 auto;
}

.resource-material-intro-row {
  display: flex;
  align-items: stretch;
  min-width: 0;
}

.resource-material-intro-drag-handle {
  display: inline-flex;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  align-self: stretch;
  margin-right: 6px;
  color: #8c8c8c;
  cursor: grab;
  background: #fafafa;
  border: 1px solid transparent;
  border-radius: 4px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
  touch-action: none;
  user-select: none;
}

.resource-material-intro-drag-handle:hover {
  color: #1677ff;
  background: #f0f5ff;
}

.resource-material-intro-drag-handle:active,
.resource-material-intro-sort-chosen .resource-material-intro-drag-handle {
  cursor: grabbing;
}

.resource-material-intro-item {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 6px;
  min-width: 0;
  width: 100%;
  min-height: 40px;
  padding: 4px 8px;
  color: #262626;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
}

.resource-material-intro-item:hover,
.resource-material-intro-item.active {
  background: #f5f9ff;
  border-color: #d6e4ff;
}

.resource-material-intro-sort-ghost {
  opacity: 0.45;
}

.resource-material-intro-sort-dragging .resource-material-intro-item {
  background: #f5f9ff;
  border-color: #91caff;
}

.resource-material-intro-items.is-reordering .resource-material-intro-drag-handle {
  cursor: wait;
  opacity: 0.5;
  pointer-events: none;
}

.resource-material-intro-title,
.resource-material-image-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-material-intro-title {
  flex: 1;
  width: auto;
  padding-right: 0;
  font-weight: 400;
}

.resource-material-intro-item-tags {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 3px;
}

.resource-material-intro-item-tags :deep(.ant-tag) {
  margin-inline-end: 0;
}

.resource-material-intro-editor {
  min-width: 0;
}

.resource-material-intro-image-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #595959;
}

.resource-material-intro-image-picker {
  margin-top: 12px;
  padding: 12px;
  background: #fafcff;
  border: 1px solid #e6f0ff;
  border-radius: 6px;
}

.resource-material-intro-selected-images {
  padding: 12px;
  background: #fafcff;
  border: 1px solid #e6f0ff;
  border-radius: 6px;
}

.resource-material-intro-image-picker-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #595959;
  font-size: 12px;
}

.resource-material-intro-image-picker-head span:first-child {
  color: #262626;
  font-size: 13px;
}

.resource-material-intro-image-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.resource-material-intro-image-option {
  position: relative;
  min-width: 0;
  padding: 7px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.resource-material-intro-image-option.selected {
  background: #f5f9ff;
  border-color: #91caff;
}

.resource-material-intro-image-option > .ant-checkbox-wrapper {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 1;
  width: 18px;
  height: 18px;
  margin: 0;
  padding: 2px;
  background: rgb(255 255 255 / 88%);
  border-radius: 2px;
}

.resource-material-intro-image-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 4 / 3;
  padding: 0;
  overflow: hidden;
  cursor: zoom-in;
  background: #fafafa;
  border: 0;
  border-radius: 3px;
}

.resource-material-intro-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.resource-material-intro-image-placeholder {
  color: #8c8c8c;
  font-size: 12px;
}

.resource-material-intro-image-label {
  margin-top: 6px;
  overflow: hidden;
  color: #434343;
  font-size: 12px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-material-intro-image-order {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  margin-top: 2px;
  color: #1677ff;
  font-size: 12px;
}

.resource-material-intro-image-empty {
  padding: 12px;
  color: #8c8c8c;
  line-height: 1.6;
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
}

.resource-material-intro-notice-preview {
  padding: 8px 0 0;
  margin-top: 10px;
  color: #b42318;
  line-height: 1.65;
  white-space: pre-line;
  border-top: 1px solid #f0f0f0;
}

.resource-material-intro-form-item {
  margin-bottom: 24px;
}

.resource-material-intro-form-item :deep(.ant-form-item-extra) {
  margin-top: 7px;
  color: #8c8c8c;
  line-height: 1.5;
}

.introduction-extension-toolbar,
.introduction-extension-block-head,
.introduction-extension-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.introduction-extension-toolbar {
  justify-content: flex-end;
  min-height: 32px;
  margin-bottom: 10px;
}

.introduction-extension-blocks {
  display: grid;
  gap: 12px;
}

.introduction-extension-block {
  padding: 12px 0 0;
  border-top: 1px solid #f0f0f0;
}

.introduction-extension-block-main {
  flex-wrap: wrap;
  min-height: 32px;
}

.introduction-extension-title-label {
  flex: 0 0 auto;
  color: #595959;
  font-size: 12px;
  white-space: nowrap;
}

.introduction-extension-title {
  flex: 1 1 240px;
  min-width: 180px;
}

.introduction-extension-color {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 5px;
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.introduction-extension-color input {
  width: 28px;
  height: 28px;
  padding: 1px;
  cursor: pointer;
  background: transparent;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.introduction-extension-actions {
  flex: 0 0 auto;
  margin-left: auto;
}

.introduction-extension-block > :deep(.ant-input) {
  margin-top: 10px;
}

.introduction-extension-items { display: grid; gap: 7px; margin: 10px 0; }
.introduction-extension-item { align-items: flex-start; }
.introduction-extension-item :deep(.ant-input) { flex: 1; }
.introduction-extension-empty {
  padding: 10px 12px;
  color: #8c8c8c;
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
}

.resource-material-intro-notice-preview-title {
  display: block;
  margin-bottom: 4px;
  font-weight: 600;
}

.resource-material-intro-notice-preview-content {
  white-space: pre-line;
}

.resource-material-intro-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.resource-material-empty {
  padding: 24px 0;
}

.resource-material-image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.resource-material-image-card {
  overflow: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}

.resource-material-image-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 4 / 3;
  padding: 0;
  overflow: hidden;
  cursor: zoom-in;
  background: #fafafa;
  border: 0;
}

.resource-material-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.resource-material-image-info {
  padding: 10px;
}

.resource-material-image-name {
  margin-bottom: 8px;
  color: #262626;
}

.resource-material-image-actions {
  display: flex;
  flex-wrap: wrap;
  margin-top: 8px;
}

@media (max-width: 720px) {
  .supplier-binding-tabs :deep(.ant-tabs-tab) {
    min-width: 0;
  }

  .modal-grid,
  .bind-grid,
  .coordinate-grid,
  .scenic-supplier-grid,
  .scenic-supplier-price-grid,
  .optional-item-row,
  .template-grid,
  .scenic-form-grid {
    grid-template-columns: 1fr;
  }

  .optional-item-row-header {
    display: none;
  }

  .scenic-map-shell {
    height: 260px;
  }

  .resource-material-intro-layout {
    grid-template-columns: 1fr;
  }

  .resource-material-intro-list {
    padding-right: 0;
    padding-bottom: 16px;
    border-right: 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .resource-material-image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .resource-material-intro-image-options {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .resource-material-intro-image-picker-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 2px;
  }

  .resource-material-intro-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
