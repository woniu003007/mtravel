<script lang="ts" setup>
import type { TablePaginationConfig, UploadProps } from 'ant-design-vue';
import type { Dayjs } from 'dayjs';

import { Page } from '@vben/common-ui';

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
import { createPurchaseRelation } from '#/api/purchase/relation';
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
  getPurchaseResourceIntroductions,
  getPurchaseResourcePage,
  publishPurchaseResourceDocument,
  publishPurchaseResourceIntroduction,
  retryPurchaseResourceDocument,
  retryPurchaseResourceIntroduction,
  reverseGeocodeCommonAmap,
  downloadPurchaseResourceDocument,
  searchCommonAmapTips,
  setPurchaseResourceImageCover,
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
import type { RegionPath } from '#/utils/region';

type ResourceRow = PurchaseResourceApi.Item;
type BindingRow = PurchaseResourceApi.Binding;
type TemplateFieldRow = RelationTicketTemplateApi.Field;
type ResourceType = PurchaseResourceApi.ResourceType;

interface MapTipOption {
  label: string;
  meta: PurchaseResourceApi.AmapTip;
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

const starLevelOptions = [
  { label: '未评级', value: 'unrated' },
  { label: '一星', value: '1star' },
  { label: '二星', value: '2star' },
  { label: '三星', value: '3star' },
  { label: '四星', value: '4star' },
  { label: '五星', value: '5star' },
];

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
const documentDrawerOpen = ref(false);
const materialTab = ref<'introductions' | 'images' | 'files'>('files');
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
const introductionLoadedResourceId = ref<number>();
const imageLoadedResourceId = ref<number>();
const documentLoadedResourceId = ref<number>();
const imagePreviewUrls = reactive<Record<number, string>>({});
const supplierOptions = ref<{ label: string; value: number }[]>([]);
const resourcePriceProjects = ref<EnterpriseExpenseItemApi.Item[]>([]);
const resourceSupplierPriceValues = reactive<Record<number, number | undefined>>({});
const systemFieldOptions = ref<{ label: string; value: string }[]>([]);
const fillModeOptions = ref<{ label: string; value: string }[]>([]);
const creatingScenicSupplier = ref(false);
const editingScenicSupplierId = ref<number>();
const editingScenicSupplierRelationId = ref<number>();
const routeBindingOpened = ref(false);
const routeTemplateOpened = ref(false);
const queryRegionPath = ref<RegionPath>([]);
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

const query = reactive<PurchaseResourceApi.QueryParams>({
  page: 1,
  pageSize: 10,
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
  noticeContent: '',
  tags: [],
  title: '',
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
const isEditingScenicSupplier = computed(() => Boolean(editingScenicSupplierId.value));
const isScenicList = computed(() => query.resourceType === 'scenic');
const isPlaceList = computed(() => Boolean(query.resourceType && isPlaceResource(query.resourceType)));
const isScenicForm = computed(() => formState.resourceType === 'scenic');
const isPlaceForm = computed(() => isPlaceResource(formState.resourceType));
const currentResourceTypeLabel = computed(() => typeLabel(currentResource.value?.resourceType));
const supplierFormTitle = computed(() => `${isEditingScenicSupplier.value ? '编辑' : '新增'}${currentResourceTypeLabel.value}供应商`);
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
}

function handleProcurementModeChange(value: unknown) {
  if (value === 'not_required') {
    formState.autoCreateSupplier = false;
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

function resetQuery() {
  queryRegionPath.value = [];
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
  formState.starLevel = 'unrated';
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
  formState.autoCreateSupplier = false;
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
  scenicSupplierForm.priceMode = 'unified';
  scenicSupplierForm.unifiedPrice = undefined;
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
  formState.autoCreateSupplier = false;
  formState.remark = row.remark;
  scenicMapKeyword.value = undefined;
  scenicMapTipOptions.value = [];
  scenicMapError.value = '';
  modalOpen.value = true;
}

function buildPayload(): PurchaseResourceApi.SaveParams {
  const regionFields = splitRegionPath(formRegionPath.value);
  const payload: PurchaseResourceApi.SaveParams = {
    address: clean(formState.address),
    autoCreateSupplier: formState.procurementMode === 'not_required'
      ? false
      : Boolean(formState.autoCreateSupplier),
    city: regionFields.city,
    contactName: clean(formState.contactName),
    district: regionFields.district,
    fax: clean(formState.fax),
    introduction: clean(formState.introduction),
    phone: clean(formState.phone),
    province: regionFields.province,
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
  try {
    const result = await reverseGeocodeCommonAmap({ latitude, longitude });
    if (result.address?.trim()) {
      formState.address = result.address.trim();
    }
  } catch {
    message.warning('地址解析失败，请手工填写详细地址');
  }
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

function handleScenicMapSelect(value: unknown) {
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
  const result = await getExpenseItemAll(resourceType as EnterpriseExpenseItemApi.ResourceType);
  resourcePriceProjects.value = result;
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

function hasClassifiedPrice() {
  return Object.values(resourceSupplierPriceValues).some((value) => !isNil(value));
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
  if (scenicSupplierForm.priceMode === 'unified') {
    if (isNil(scenicSupplierForm.unifiedPrice)) {
      message.warning('请填写统一报价');
      return false;
    }
    return true;
  }
  if (resourcePriceProjects.value.length === 0) {
    message.warning('当前资源类型还没有维护报价项目，请先到费用项目中维护');
    return false;
  }
  if (!hasClassifiedPrice()) {
    message.warning('分类报价至少填写一种');
    return false;
  }
  return true;
}

function buildScenicSupplierPayload(): PurchaseResourceApi.ResourceSupplierCreateParams {
  const supplierRegion = splitRegionPath(scenicSupplierRegionPath.value);
  const payload: PurchaseResourceApi.ResourceSupplierCreateParams = {
    basicInfo: clean(scenicSupplierForm.basicInfo),
    city: supplierRegion.city,
    contactName: clean(scenicSupplierForm.contactName),
    contactPhone: clean(scenicSupplierForm.contactPhone),
    district: supplierRegion.district,
    isDefault: Boolean(scenicSupplierForm.isDefault),
    priceMode: scenicSupplierForm.priceMode,
    priceRemark: clean(scenicSupplierForm.priceRemark),
    province: supplierRegion.province,
    remark: clean(scenicSupplierForm.remark),
    status: scenicSupplierForm.status || 'active',
    supplierName: scenicSupplierForm.supplierName.trim(),
  };
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
  scenicSupplierForm.priceMode = record.priceMode || 'classified';
  scenicSupplierForm.unifiedPrice = record.unifiedPrice;
  scenicSupplierForm.priceLines = record.priceLines?.map((line) => ({
    priceDescription: line.priceDescription,
    resourceProjectId: line.resourceProjectId,
    teamPrice: line.teamPrice,
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

async function createScenicSupplier() {
  if (!currentResource.value || !validateScenicSupplierForm()) {
    return;
  }
  creatingScenicSupplier.value = true;
  try {
    if (editingScenicSupplierId.value) {
      if (!editingScenicSupplierRelationId.value) {
        message.error('绑定关系缺少 relationId，无法保存报价');
        return;
      }
      await updateResourceSupplierForResource(
        currentResource.value.id,
        editingScenicSupplierRelationId.value,
        buildScenicSupplierPayload(),
      );
      message.success('供应商资料和报价已更新');
      resetScenicSupplierForm(currentResource.value);
      await Promise.all([
        loadSuppliers(currentResource.value.resourceType),
        loadBindings(currentResource.value.id),
        loadData(),
      ]);
      return;
    }
    await createResourceSupplierForResource(
      currentResource.value.id,
      buildScenicSupplierPayload(),
    );
    message.success('供应商已新增并绑定');
    resetScenicSupplierForm();
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
  currentResource.value = row;
  materialTab.value = 'files';
  materialServiceUnavailable.value = false;
  introductions.value = [];
  images.value = [];
  documents.value = [];
  activeIntroductionId.value = undefined;
  introductionLoadedResourceId.value = undefined;
  imageLoadedResourceId.value = undefined;
  documentLoadedResourceId.value = undefined;
  resetIntroductionForm();
  revokeImagePreviewUrls();
  documentDrawerOpen.value = true;
  await loadResourceDocuments(row);
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
  introductionForm.noticeContent = '';
  introductionForm.tags = [];
  introductionForm.title = '';
}

function selectIntroduction(record?: PurchaseResourceApi.ResourceIntroductionItem) {
  activeIntroductionId.value = record?.id;
  introductionForm.content = record?.content || '';
  introductionForm.noticeContent = record?.noticeContent || '';
  introductionForm.tags = [...(record?.tags || [])];
  introductionForm.title = record?.title || '';
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
  return true;
}

function introductionPayload(): PurchaseResourceApi.ResourceIntroductionSaveParams {
  return {
    content: introductionForm.content.trim(),
    // 传空字符串才能让用户在编辑时清空已保存的注意事项。
    noticeContent: (introductionForm.noticeContent || '').trim(),
    tags: introductionForm.tags?.map((tag) => tag.trim()).filter(Boolean),
    title: introductionForm.title.trim(),
  };
}

async function saveIntroduction(showMessage = true) {
  if (!currentResource.value || !validateIntroductionForm()) {
    return undefined;
  }
  introductionSaving.value = true;
  try {
    const item = activeIntroductionId.value
      ? await updatePurchaseResourceIntroduction(
          currentResource.value.id,
          activeIntroductionId.value,
          introductionPayload(),
        )
      : await createPurchaseResourceIntroduction(
          currentResource.value.id,
          introductionPayload(),
        );
    activeIntroductionId.value = item.id;
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

async function handlePublishIntroduction() {
  if (!currentResource.value) {
    return;
  }
  introductionPublishing.value = true;
  try {
    const saved = await saveIntroduction(false);
    if (!saved) {
      return;
    }
    await publishPurchaseResourceIntroduction(currentResource.value.id, saved.id);
    message.success('介绍已发布，正在写入产品文案资料库');
    await loadResourceIntroductions(currentResource.value);
    selectIntroduction(introductions.value.find((item) => item.id === saved.id));
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
  clearScenicMapSearchTimer();
  clearScenicMapAutoLocateTimer();
  destroyScenicMap();
  revokeImagePreviewUrls();
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
});

onMounted(async () => {
  await loadData();
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
        <Form.Item label="所在地">
          <Cascader
            v-model:value="queryRegionPath"
            allow-clear
            change-on-select
            :options="regionOptions"
            placeholder="可选择省 / 市 / 区县"
            show-search
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
          <Form.Item label="所在地">
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
              @change="handleProcurementModeChange"
            />
          </Form.Item>
        </div>
        <div v-if="supportsStarLevel(formState.resourceType) || supportsCategoryTags(formState.resourceType)" class="modal-grid">
          <Form.Item v-if="supportsStarLevel(formState.resourceType)" label="星级">
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
        <Form.Item v-if="!isPlaceForm" label="所在地址">
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
        <Form.Item v-if="!editingId && formState.procurementMode !== 'not_required'">
          <Checkbox v-model:checked="formState.autoCreateSupplier">
            自动创建同名供应商并建立对应关系
          </Checkbox>
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

      <Card size="small" class="mb-4" title="快速绑定已有供应商">
        <Form :model="bindForm" layout="vertical">
          <div class="bind-grid">
            <Form.Item label="供应商" required>
              <Select
                v-model:value="bindForm.supplierId"
                show-search
                :filter-option="true"
                :options="supplierOptions"
                placeholder="选择供应商"
              />
            </Form.Item>
            <Form.Item label="默认供应商">
              <Checkbox v-model:checked="bindForm.isDefault">
                设为当前资源默认供应商
              </Checkbox>
            </Form.Item>
          </div>
          <div class="bind-actions">
            <Button type="primary" @click="bindSupplier">绑定供应商</Button>
          </div>
        </Form>
      </Card>

      <Card
        size="small"
        class="mb-4"
        :title="supplierFormTitle"
      >
        <div class="drawer-card-hint">
          这里直接维护当前资源的供应商档案和报价。资料保存后会自动绑定当前资源，不用再切到供应商页来回跳转。
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
          <Form.Item label="报价方式" required>
            <Select
              v-model:value="scenicSupplierForm.priceMode"
              :options="[
                { label: '统一报价', value: 'unified' },
                { label: '分类报价', value: 'classified' },
              ]"
            />
          </Form.Item>
          <Form.Item
            v-if="scenicSupplierForm.priceMode === 'unified'"
            label="统一报价"
            required
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
            v-else
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
          <Form.Item label="报价备注">
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
      </Card>

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
        <Table.Column title="操作" key="action" width="160">
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
                <div v-if="introductions.length" class="resource-material-intro-items">
                  <button
                    v-for="item in introductions"
                    :key="item.id"
                    class="resource-material-intro-item"
                    :class="{ active: item.id === activeIntroductionId }"
                    type="button"
                    @click="selectIntroduction(item)"
                  >
                    <span class="resource-material-intro-title">{{ item.title }}</span>
                    <Tag :color="reviewStatusColor(item.status)">
                      {{ reviewStatusLabel(item.status) }}
                    </Tag>
                  </button>
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
                  <Form.Item label="适用标签">
                    <Select
                      v-model:value="introductionForm.tags"
                      mode="tags"
                      :max-tag-count="4"
                      placeholder="例如：亲子、深度游、秋季"
                      :token-separators="[',', '，']"
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
                  </Form.Item>
                  <div
                    v-if="introductionForm.noticeContent?.trim()"
                    class="resource-material-intro-notice-preview"
                    role="note"
                  >
                    <strong>注意事项预览</strong>
                    <div>{{ introductionForm.noticeContent }}</div>
                  </div>
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
          :tab="`图片素材 (${images.length})`"
        >
          <Spin :spinning="imageLoading">
            <div class="resource-material-section-header">
              <span class="muted">产品配图不进入知识库向量，封面可用于产品和确认单展示。</span>
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

.drawer-card-hint {
  margin-bottom: 12px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.scenic-supplier-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: end;
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
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
  min-height: 470px;
}

.resource-material-intro-list {
  padding-right: 16px;
  border-right: 1px solid #f0f0f0;
}

.resource-material-intro-items {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.resource-material-intro-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 38px;
  padding: 6px 8px;
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

.resource-material-intro-title,
.resource-material-image-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-material-intro-title {
  padding-right: 6px;
}

.resource-material-intro-editor {
  min-width: 0;
}

.resource-material-intro-notice-preview {
  padding: 8px 12px;
  margin: -4px 0 16px;
  color: #cf1322;
  line-height: 1.7;
  white-space: pre-line;
  background: #fff2f0;
  border-left: 3px solid #ff4d4f;
}

.resource-material-intro-notice-preview strong {
  display: block;
  margin-bottom: 2px;
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
  .modal-grid,
  .bind-grid,
  .coordinate-grid,
  .scenic-supplier-grid,
  .scenic-supplier-price-grid,
  .template-grid,
  .scenic-form-grid {
    grid-template-columns: 1fr;
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

  .resource-material-intro-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
