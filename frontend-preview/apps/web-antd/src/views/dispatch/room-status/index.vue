<script lang="ts" setup>
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Cascader,
  DatePicker,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import dayjs, { type Dayjs } from 'dayjs';
import { computed, onMounted, reactive, ref, watch } from 'vue';

import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  type ControlledRoomStatusApi,
  createControlledRoom,
  createControlledRoomResource,
  createControlledRoomType,
  createRoomInventoryLock,
  deleteControlledRoom,
  deleteControlledRoomResource,
  deleteControlledRoomType,
  generateRoomInventory,
  getControlledRoomLockPage,
  getControlledRoomPage,
  getControlledRoomResourceAll,
  getControlledRoomResourcePage,
  getControlledRoomTypeAll,
  getControlledRoomTypePage,
  getRoomInventoryCalendar,
  getRoomInventoryOccupancy,
  releaseControlledRoomLock,
  updateControlledRoom,
  updateControlledRoomResource,
  updateControlledRoomType,
} from '#/api/dispatch/room-status';
import { getPurchaseRelationPage, type PurchaseRelationApi } from '#/api/purchase/relation';
import {
  buildRegionOptions,
  buildRegionPath,
  splitRegionPath,
  type RegionPath,
} from '#/utils/region';

type ResourceRow = ControlledRoomStatusApi.Resource;
type RoomTypeRow = ControlledRoomStatusApi.RoomType;
type RoomRow = ControlledRoomStatusApi.Room;
type InventoryRow = ControlledRoomStatusApi.Inventory;
type LockRow = ControlledRoomStatusApi.LockRecord;
type OccupancyRow = ControlledRoomStatusApi.InventoryOccupancy;
type ResourceForm = ControlledRoomStatusApi.ResourceSaveParams;
type RoomTypeForm = Omit<ControlledRoomStatusApi.RoomTypeSaveParams, 'resourceId'> & { resourceId?: number };
type RoomForm = Omit<ControlledRoomStatusApi.RoomSaveParams, 'resourceId'> & { resourceId?: number };
type InventoryGenerateForm = Omit<ControlledRoomStatusApi.InventoryGenerateParams, 'sourceId' | 'sourceType'> & {
  purchasedRelationId?: number;
  selfResourceId?: number;
  sourceType: ControlledRoomStatusApi.SourceType;
};
type InventoryLockForm = Omit<ControlledRoomStatusApi.InventoryLockParams, 'sourceId' | 'sourceType'> & {
  purchasedRelationId?: number;
  selfResourceId?: number;
  sourceType: ControlledRoomStatusApi.SourceType;
};
type DateCarrier = Partial<Record<
  'checkInDate' | 'checkOutDate' | 'endDate' | 'startDate' | 'validFrom' | 'validTo',
  string
>>;
type DatePickerValue = Dayjs | string | null | undefined;

const activeTab = ref('resources');
const regionOptions = buildRegionOptions();
const resourceQueryRegionPath = ref<RegionPath>([]);
const resourceFormRegionPath = ref<RegionPath>([]);

const resourceLoading = ref(false);
const roomTypeLoading = ref(false);
const roomLoading = ref(false);
const inventoryLoading = ref(false);
const lockLoading = ref(false);
const occupancyLoading = ref(false);
const optionLoading = ref(false);

const resourceRows = ref<ResourceRow[]>([]);
const roomTypeRows = ref<RoomTypeRow[]>([]);
const roomRows = ref<RoomRow[]>([]);
const inventoryRows = ref<InventoryRow[]>([]);
const lockRows = ref<LockRow[]>([]);
const occupancyRows = ref<OccupancyRow[]>([]);
const resourceOptions = ref<Array<{ label: string; value: number }>>([]);
const roomTypeOptions = ref<Array<{ label: string; meta: RoomTypeRow; value: number }>>([]);
const purchaseRelationOptions = ref<Array<{ label: string; meta: PurchaseRelationApi.Item; value: number }>>([]);

const resourceModalOpen = ref(false);
const roomTypeModalOpen = ref(false);
const roomModalOpen = ref(false);
const generateModalOpen = ref(false);
const lockModalOpen = ref(false);
const occupancyDrawerOpen = ref(false);
const editingResourceId = ref<number>();
const editingRoomTypeId = ref<number>();
const editingRoomId = ref<number>();

const resourceQuery = reactive<ControlledRoomStatusApi.ResourceQuery>({ page: 1, pageSize: 10 });
const roomTypeQuery = reactive<ControlledRoomStatusApi.RoomTypeQuery>({ page: 1, pageSize: 10 });
const roomQuery = reactive<ControlledRoomStatusApi.RoomQuery>({ page: 1, pageSize: 10 });
const inventoryQuery = reactive<ControlledRoomStatusApi.InventoryQuery>({
  endDate: dayjs().add(7, 'day').format('YYYY-MM-DD'),
  sourceType: undefined,
  startDate: dayjs().format('YYYY-MM-DD'),
});
const lockQuery = reactive<ControlledRoomStatusApi.LockQuery>({ page: 1, pageSize: 10 });

const resourcePagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, showSizeChanger: true, total: 0 });
const roomTypePagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, showSizeChanger: true, total: 0 });
const roomPagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, showSizeChanger: true, total: 0 });
const lockPagination = reactive<TablePaginationConfig>({ current: 1, pageSize: 10, showSizeChanger: true, total: 0 });

const resourceForm = reactive<ResourceForm>({
  agreementPrice: 0,
  hotelName: '',
  priceUnit: '间夜',
  purchasePrice: 0,
  status: 'active',
});
const roomTypeForm = reactive<RoomTypeForm>({
  agreementPrice: 0,
  capacity: 2,
  priceUnit: '间夜',
  purchasePrice: 0,
  resourceId: undefined,
  roomType: '',
  status: 'active',
});
const roomForm = reactive<RoomForm>({
  capacity: 2,
  resourceId: undefined,
  roomNo: '',
  status: 'active',
});
const generateForm = reactive<InventoryGenerateForm>({
  endDate: dayjs().add(30, 'day').format('YYYY-MM-DD'),
  roomType: '',
  sourceType: 'self_owned',
  startDate: dayjs().format('YYYY-MM-DD'),
  status: 'active',
  totalQuantity: 0,
});
const lockForm = reactive<InventoryLockForm>({
  checkInDate: dayjs().format('YYYY-MM-DD'),
  checkOutDate: dayjs().add(1, 'day').format('YYYY-MM-DD'),
  quantity: 1,
  roomType: '',
  sourceType: 'self_owned',
});

const pageDescription = '计调按酒店、来源、房型和日期查看总量、已锁、已占、余量；自营房源可维护房型和房号，资源采购房源只进库存数量。';

const resourceStatusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
  { label: '到期', value: 'expired' },
];
const enabledStatusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];
const roomStatusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
  { label: '维修', value: 'maintenance' },
];
const inventoryStatusOptions = [
  { label: '有效', value: 'active' },
  { label: '停售', value: 'stopped' },
];
const lockStatusOptions = [
  { label: '已锁定', value: 'locked' },
  { label: '已占用', value: 'occupied' },
  { label: '已释放', value: 'released' },
];
const sourceTypeOptions = [
  { label: '自营房源', value: 'self_owned' },
  { label: '资源采购房源', value: 'purchased_resource' },
];
const standardOptions = [
  { label: '经济型', value: '经济型' },
  { label: '舒适型', value: '舒适型' },
  { label: '四钻', value: '四钻' },
  { label: '五钻', value: '五钻' },
  { label: '四星', value: '四星' },
  { label: '五星', value: '五星' },
];

const resourceColumns: TableColumnsType<ResourceRow> = [
  { key: 'hotel', title: '自营酒店', width: 230 },
  { key: 'region', title: '所在地', width: 190 },
  { dataIndex: 'starStandard', key: 'starStandard', title: '星钻标准', width: 110 },
  { dataIndex: 'sourceName', key: 'sourceName', title: '房源来源', width: 160 },
  { key: 'period', title: '使用权有效期', width: 190 },
  { dataIndex: 'contactName', key: 'contactName', title: '联系人', width: 120 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { fixed: 'right', key: 'action', title: '操作', width: 150 },
];
const roomTypeColumns: TableColumnsType<RoomTypeRow> = [
  { dataIndex: 'hotelName', key: 'hotelName', title: '自营酒店', width: 190 },
  { dataIndex: 'roomType', key: 'roomType', title: '房型', width: 130 },
  { key: 'bed', title: '床型/人数', width: 130 },
  { key: 'price', title: '采购/协议价', width: 160 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'remark', key: 'remark', title: '备注', width: 180 },
  { fixed: 'right', key: 'action', title: '操作', width: 150 },
];
const roomColumns: TableColumnsType<RoomRow> = [
  { dataIndex: 'hotelName', key: 'hotelName', title: '自营酒店', width: 190 },
  { dataIndex: 'roomNo', key: 'roomNo', title: '房号', width: 100 },
  { key: 'building', title: '楼栋/楼层', width: 130 },
  { dataIndex: 'roomType', key: 'roomType', title: '房型', width: 120 },
  { key: 'capacity', title: '床型/人数', width: 130 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { fixed: 'right', key: 'action', title: '操作', width: 150 },
];
const inventoryColumns: TableColumnsType<InventoryRow> = [
  { fixed: 'left', key: 'source', title: '酒店/来源', width: 240 },
  { dataIndex: 'roomType', key: 'roomType', title: '房型', width: 120 },
  { dataIndex: 'stayDate', key: 'stayDate', title: '日期', width: 120 },
  { key: 'quantity', title: '总 / 锁 / 占 / 余', width: 210 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { key: 'action', title: '明细', width: 100 },
];
const lockColumns: TableColumnsType<LockRow> = [
  { key: 'team', title: '团队', width: 190 },
  { key: 'room', title: '酒店/房型', width: 230 },
  { key: 'period', title: '入住/退房', width: 190 },
  { dataIndex: 'requiredStandard', key: 'requiredStandard', title: '团队标准', width: 110 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'createdBy', key: 'createdBy', title: '创建人', width: 100 },
  { fixed: 'right', key: 'action', title: '操作', width: 100 },
];
const occupancyColumns: TableColumnsType<OccupancyRow> = [
  { key: 'team', title: '团队', width: 180 },
  { dataIndex: 'quantity', key: 'quantity', title: '数量', width: 80 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '锁房时间', width: 160 },
];

const selectedGenerateRoomType = computed(() => roomTypeOptions.value.find((item) => item.value === generateForm.roomTypeId)?.meta);
const selectedLockRoomType = computed(() => roomTypeOptions.value.find((item) => item.value === lockForm.roomTypeId)?.meta);

function clean(value?: string) {
  return value?.trim() || undefined;
}

function dateValue(value?: string) {
  return value ? dayjs(value) : undefined;
}

function setDate(target: DateCarrier, key: keyof DateCarrier, value: DatePickerValue) {
  target[key] = dayjs.isDayjs(value) ? value.format('YYYY-MM-DD') : value ? dayjs(value).format('YYYY-MM-DD') : undefined;
}

function formatMoney(value?: number) {
  return typeof value === 'number' ? `¥${value.toFixed(2)}` : '-';
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function regionText(record: Pick<ResourceRow, 'city' | 'district' | 'province'>) {
  return [record.province, record.city, record.district].filter(Boolean).join(' / ') || '-';
}

function statusLabel(value?: string) {
  const allOptions = [...resourceStatusOptions, ...enabledStatusOptions, ...roomStatusOptions, ...inventoryStatusOptions, ...lockStatusOptions];
  return allOptions.find((item) => item.value === value)?.label || value || '-';
}

function statusColor(value?: string) {
  const colors: Record<string, string> = {
    active: 'green',
    disabled: 'default',
    expired: 'red',
    locked: 'blue',
    maintenance: 'orange',
    occupied: 'purple',
    released: 'default',
    stopped: 'red',
  };
  return colors[value || ''] || 'default';
}

function sourceTypeLabel(value?: string) {
  return sourceTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

async function loadOptions() {
  optionLoading.value = true;
  try {
    const [resources, roomTypes, relationResult] = await Promise.all([
      getControlledRoomResourceAll(),
      getControlledRoomTypeAll(undefined),
      getPurchaseRelationPage({ page: 1, pageSize: 200, resourceType: 'hotel', status: 'active' }),
    ]);
    resourceOptions.value = resources.map((item) => ({
      label: `${item.hotelName} / ${regionText(item)} / ${item.starStandard || '未设标准'}`,
      value: item.id,
    }));
    roomTypeOptions.value = roomTypes.map((item) => ({
      label: `${item.hotelName || ''} / ${item.roomType} / ${item.bedType || '-'} / ${formatMoney(item.agreementPrice)}`,
      meta: item,
      value: item.id,
    }));
    purchaseRelationOptions.value = relationResult.items.map((item) => ({
      label: `${item.resourceName} / ${item.supplierName || '未填供应商'} / ${item.location || '-'}`,
      meta: item,
      value: item.id,
    }));
  } finally {
    optionLoading.value = false;
  }
}

async function loadResources() {
  resourceLoading.value = true;
  try {
    const result = await getControlledRoomResourcePage(resourceQuery);
    resourceRows.value = result.items;
    resourcePagination.current = resourceQuery.page;
    resourcePagination.pageSize = resourceQuery.pageSize;
    resourcePagination.total = result.total;
    await loadOptions();
  } finally {
    resourceLoading.value = false;
  }
}

async function loadRoomTypes() {
  roomTypeLoading.value = true;
  try {
    const result = await getControlledRoomTypePage(roomTypeQuery);
    roomTypeRows.value = result.items;
    roomTypePagination.current = roomTypeQuery.page;
    roomTypePagination.pageSize = roomTypeQuery.pageSize;
    roomTypePagination.total = result.total;
  } finally {
    roomTypeLoading.value = false;
  }
}

async function loadRooms() {
  roomLoading.value = true;
  try {
    const result = await getControlledRoomPage(roomQuery);
    roomRows.value = result.items;
    roomPagination.current = roomQuery.page;
    roomPagination.pageSize = roomQuery.pageSize;
    roomPagination.total = result.total;
  } finally {
    roomLoading.value = false;
  }
}

async function loadInventories() {
  inventoryLoading.value = true;
  try {
    inventoryRows.value = await getRoomInventoryCalendar(inventoryQuery);
  } finally {
    inventoryLoading.value = false;
  }
}

async function loadLocks() {
  lockLoading.value = true;
  try {
    const result = await getControlledRoomLockPage(lockQuery);
    lockRows.value = result.items;
    lockPagination.current = lockQuery.page;
    lockPagination.pageSize = lockQuery.pageSize;
    lockPagination.total = result.total;
  } finally {
    lockLoading.value = false;
  }
}

async function refreshActiveTab() {
  if (activeTab.value === 'resources') await loadResources();
  if (activeTab.value === 'types') await loadRoomTypes();
  if (activeTab.value === 'rooms') await loadRooms();
  if (activeTab.value === 'inventories') await loadInventories();
  if (activeTab.value === 'locks') await loadLocks();
}

function resetResourceQuery() {
  resourceQueryRegionPath.value = [];
  Object.assign(resourceQuery, { city: undefined, district: undefined, keyword: undefined, page: 1, province: undefined, starStandard: undefined, status: undefined });
  loadResources();
}

function handleResourceSearch() {
  const regionFields = splitRegionPath(resourceQueryRegionPath.value);
  Object.assign(resourceQuery, { ...regionFields, page: 1 });
  loadResources();
}

function resetRoomTypeQuery() {
  Object.assign(roomTypeQuery, { keyword: undefined, page: 1, resourceId: undefined, status: undefined });
  loadRoomTypes();
}

function resetRoomQuery() {
  Object.assign(roomQuery, { keyword: undefined, page: 1, resourceId: undefined, status: undefined });
  loadRooms();
}

function resetInventoryQuery() {
  Object.assign(inventoryQuery, {
    endDate: dayjs().add(7, 'day').format('YYYY-MM-DD'),
    roomTypeId: undefined,
    sourceId: undefined,
    sourceType: undefined,
    startDate: dayjs().format('YYYY-MM-DD'),
    status: undefined,
  });
  loadInventories();
}

function resetLockQuery() {
  Object.assign(lockQuery, { page: 1, resourceId: undefined, status: undefined, teamNo: undefined });
  loadLocks();
}

function resetResourceForm() {
  editingResourceId.value = undefined;
  resourceFormRegionPath.value = [];
  Object.assign(resourceForm, {
    address: undefined,
    agreementPrice: 0,
    area: undefined,
    city: undefined,
    contactName: undefined,
    contactPhone: undefined,
    district: undefined,
    hotelName: '',
    priceUnit: '间夜',
    province: undefined,
    purchasePrice: 0,
    remark: undefined,
    roomType: undefined,
    sourceName: undefined,
    starStandard: undefined,
    status: 'active',
    validFrom: undefined,
    validTo: undefined,
  });
}

function openCreateResource() {
  resetResourceForm();
  resourceModalOpen.value = true;
}

function openEditResource(row: ResourceRow) {
  editingResourceId.value = row.id;
  resourceFormRegionPath.value = buildRegionPath(row.province, row.city, row.district);
  Object.assign(resourceForm, { ...row, priceUnit: row.priceUnit || '间夜' });
  resourceModalOpen.value = true;
}

async function saveResource() {
  if (!resourceForm.hotelName?.trim()) {
    message.warning('请填写酒店名称');
    return;
  }
  const regionFields = splitRegionPath(resourceFormRegionPath.value);
  const payload: ResourceForm = {
    ...resourceForm,
    ...regionFields,
    address: clean(resourceForm.address),
    hotelName: resourceForm.hotelName.trim(),
    remark: clean(resourceForm.remark),
    roomType: clean(resourceForm.roomType),
    sourceName: clean(resourceForm.sourceName),
    starStandard: clean(resourceForm.starStandard),
  };
  if (editingResourceId.value) {
    await updateControlledRoomResource(editingResourceId.value, payload);
    message.success('自营酒店已更新');
  } else {
    await createControlledRoomResource(payload);
    message.success('自营酒店已新增');
  }
  resourceModalOpen.value = false;
  await loadResources();
}

function confirmDeleteResource(row: ResourceRow) {
  Modal.confirm({
    title: `删除自营酒店「${row.hotelName}」？`,
    content: '删除只做软删除，已有库存和锁房数据不会物理移除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteControlledRoomResource(row.id);
      message.success('自营酒店已删除');
      await loadResources();
    },
  });
}

function resetRoomTypeForm() {
  editingRoomTypeId.value = undefined;
  Object.assign(roomTypeForm, {
    agreementPrice: 0,
    bedType: undefined,
    capacity: 2,
    priceUnit: '间夜',
    purchasePrice: 0,
    remark: undefined,
    resourceId: roomTypeQuery.resourceId,
    roomType: '',
    status: 'active',
  });
}

function openCreateRoomType() {
  resetRoomTypeForm();
  roomTypeModalOpen.value = true;
}

function openEditRoomType(row: RoomTypeRow) {
  editingRoomTypeId.value = row.id;
  Object.assign(roomTypeForm, { ...row, priceUnit: row.priceUnit || '间夜' });
  roomTypeModalOpen.value = true;
}

async function saveRoomType() {
  if (!roomTypeForm.resourceId || !roomTypeForm.roomType?.trim()) {
    message.warning('请选择自营酒店并填写房型');
    return;
  }
  const payload: ControlledRoomStatusApi.RoomTypeSaveParams = {
    agreementPrice: roomTypeForm.agreementPrice,
    bedType: clean(roomTypeForm.bedType),
    capacity: roomTypeForm.capacity,
    priceUnit: clean(roomTypeForm.priceUnit) || '间夜',
    purchasePrice: roomTypeForm.purchasePrice,
    remark: clean(roomTypeForm.remark),
    resourceId: roomTypeForm.resourceId,
    roomType: roomTypeForm.roomType.trim(),
    status: roomTypeForm.status,
  };
  if (editingRoomTypeId.value) {
    await updateControlledRoomType(editingRoomTypeId.value, payload);
    message.success('房型已更新');
  } else {
    await createControlledRoomType(payload);
    message.success('房型已新增');
  }
  roomTypeModalOpen.value = false;
  await Promise.all([loadRoomTypes(), loadOptions()]);
}

function confirmDeleteRoomType(row: RoomTypeRow) {
  Modal.confirm({
    title: `删除房型「${row.hotelName} / ${row.roomType}」？`,
    content: '已有锁定或占用库存的房型不能删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteControlledRoomType(row.id);
      message.success('房型已删除');
      await Promise.all([loadRoomTypes(), loadOptions()]);
    },
  });
}

function resetRoomForm() {
  editingRoomId.value = undefined;
  Object.assign(roomForm, {
    bedType: undefined,
    buildingName: undefined,
    capacity: 2,
    floorNo: undefined,
    remark: undefined,
    resourceId: roomQuery.resourceId,
    roomNo: '',
    roomType: undefined,
    status: 'active',
  });
}

function openCreateRoom() {
  resetRoomForm();
  roomModalOpen.value = true;
}

function openEditRoom(row: RoomRow) {
  editingRoomId.value = row.id;
  Object.assign(roomForm, { ...row });
  roomModalOpen.value = true;
}

async function saveRoom() {
  if (!roomForm.resourceId || !roomForm.roomNo?.trim()) {
    message.warning('请选择自营酒店并填写房号');
    return;
  }
  const payload: ControlledRoomStatusApi.RoomSaveParams = {
    bedType: clean(roomForm.bedType),
    buildingName: clean(roomForm.buildingName),
    capacity: roomForm.capacity,
    floorNo: clean(roomForm.floorNo),
    remark: clean(roomForm.remark),
    resourceId: roomForm.resourceId,
    roomNo: roomForm.roomNo.trim(),
    roomType: clean(roomForm.roomType),
    status: roomForm.status,
  };
  if (editingRoomId.value) {
    await updateControlledRoom(editingRoomId.value, payload);
    message.success('房号已更新');
  } else {
    await createControlledRoom(payload);
    message.success('房号已新增');
  }
  roomModalOpen.value = false;
  await loadRooms();
}

function confirmDeleteRoom(row: RoomRow) {
  Modal.confirm({
    title: `删除房号「${row.roomNo}」？`,
    content: '已有锁定或占用记录的房号不能删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteControlledRoom(row.id);
      message.success('房号已删除');
      await loadRooms();
    },
  });
}

function openGenerateModal() {
  Object.assign(generateForm, {
    endDate: dayjs(inventoryQuery.startDate).add(30, 'day').format('YYYY-MM-DD'),
    purchasedRelationId: undefined,
    remark: undefined,
    roomType: '',
    roomTypeId: undefined,
    selfResourceId: inventoryQuery.sourceType === 'self_owned' ? inventoryQuery.sourceId : undefined,
    sourceType: inventoryQuery.sourceType || 'self_owned',
    startDate: inventoryQuery.startDate || dayjs().format('YYYY-MM-DD'),
    status: 'active',
    totalQuantity: 0,
  });
  generateModalOpen.value = true;
}

async function submitGenerate() {
  const sourceId = generateForm.sourceType === 'self_owned' ? generateForm.selfResourceId : generateForm.purchasedRelationId;
  if (!sourceId || !generateForm.roomType?.trim() || !generateForm.startDate || !generateForm.endDate) {
    message.warning('请选择来源、房型和日期范围');
    return;
  }
  const count = await generateRoomInventory({
    endDate: generateForm.endDate,
    roomType: generateForm.roomType.trim(),
    roomTypeId: generateForm.sourceType === 'self_owned' ? generateForm.roomTypeId : undefined,
    sourceId,
    sourceType: generateForm.sourceType,
    startDate: generateForm.startDate,
    status: generateForm.status,
    totalQuantity: Number(generateForm.totalQuantity || 0),
  });
  message.success(`已生成 ${count} 条房态库存`);
  generateModalOpen.value = false;
  await loadInventories();
}

function openLockModal(row?: InventoryRow) {
  Object.assign(lockForm, {
    checkInDate: row?.stayDate || inventoryQuery.startDate || dayjs().format('YYYY-MM-DD'),
    checkOutDate: dayjs(row?.stayDate || inventoryQuery.startDate || dayjs()).add(1, 'day').format('YYYY-MM-DD'),
    purchasedRelationId: row?.sourceType === 'purchased_resource' ? row.sourceId : undefined,
    quantity: 1,
    remark: undefined,
    requiredStandard: undefined,
    roomType: row?.roomType || '',
    roomTypeId: row?.roomTypeId,
    selfResourceId: row?.sourceType === 'self_owned' ? row.sourceId : undefined,
    sourceType: row?.sourceType || inventoryQuery.sourceType || 'self_owned',
    teamName: undefined,
    teamNo: undefined,
  });
  lockModalOpen.value = true;
}

async function submitLock() {
  const sourceId = lockForm.sourceType === 'self_owned' ? lockForm.selfResourceId : lockForm.purchasedRelationId;
  if (!sourceId || !lockForm.roomType?.trim()) {
    message.warning('请选择来源和房型');
    return;
  }
  await createRoomInventoryLock({
    checkInDate: lockForm.checkInDate,
    checkOutDate: lockForm.checkOutDate,
    quantity: Number(lockForm.quantity || 1),
    remark: clean(lockForm.remark),
    requiredStandard: clean(lockForm.requiredStandard),
    roomType: lockForm.roomType.trim(),
    roomTypeId: lockForm.sourceType === 'self_owned' ? lockForm.roomTypeId : undefined,
    sourceId,
    sourceType: lockForm.sourceType,
    teamName: clean(lockForm.teamName),
    teamNo: clean(lockForm.teamNo),
  });
  message.success('锁房已完成');
  lockModalOpen.value = false;
  await Promise.all([loadInventories(), loadLocks()]);
}

async function openOccupancyDrawer(row: InventoryRow) {
  occupancyDrawerOpen.value = true;
  occupancyLoading.value = true;
  try {
    occupancyRows.value = await getRoomInventoryOccupancy({
      roomType: row.roomType,
      roomTypeId: row.roomTypeId,
      sourceId: row.sourceId,
      sourceType: row.sourceType,
      stayDate: row.stayDate,
    });
  } finally {
    occupancyLoading.value = false;
  }
}

function confirmReleaseLock(row: LockRow) {
  Modal.confirm({
    title: `释放锁房「${row.teamNo || row.id}」？`,
    content: '释放后对应日期的已锁数量会回到余量。',
    okText: '释放',
    cancelText: '取消',
    async onOk() {
      await releaseControlledRoomLock(row.id);
      message.success('锁房已释放');
      await Promise.all([loadInventories(), loadLocks()]);
    },
  });
}

function handleResourceTableChange(next: TablePaginationConfig) {
  Object.assign(resourceQuery, { page: Number(next.current || 1), pageSize: Number(next.pageSize || 10) });
  loadResources();
}

function handleRoomTypeTableChange(next: TablePaginationConfig) {
  Object.assign(roomTypeQuery, { page: Number(next.current || 1), pageSize: Number(next.pageSize || 10) });
  loadRoomTypes();
}

function handleRoomTableChange(next: TablePaginationConfig) {
  Object.assign(roomQuery, { page: Number(next.current || 1), pageSize: Number(next.pageSize || 10) });
  loadRooms();
}

function handleLockTableChange(next: TablePaginationConfig) {
  Object.assign(lockQuery, { page: Number(next.current || 1), pageSize: Number(next.pageSize || 10) });
  loadLocks();
}

watch(() => generateForm.roomTypeId, () => {
  if (generateForm.sourceType !== 'self_owned') return;
  const selected = selectedGenerateRoomType.value;
  generateForm.roomType = selected?.roomType || '';
});

watch(() => lockForm.roomTypeId, () => {
  if (lockForm.sourceType !== 'self_owned') return;
  const selected = selectedLockRoomType.value;
  lockForm.roomType = selected?.roomType || '';
});

onMounted(async () => {
  await loadOptions();
  await loadResources();
});
</script>

<template>
  <Page title="房源与房态库存" :description="pageDescription">
    <Card>
      <Tabs v-model:active-key="activeTab" @change="refreshActiveTab">
        <Tabs.TabPane key="resources" tab="自营酒店">
          <BusinessSearchForm
            label-width="78px"
            :model="resourceQuery"
            :search-loading="resourceLoading"
            create-text="新增"
            @create="openCreateResource"
            @reset="resetResourceQuery"
            @search="handleResourceSearch"
          >
            <Form.Item label="关键词"><Input v-model:value="resourceQuery.keyword" allow-clear placeholder="酒店/区域/来源" /></Form.Item>
            <Form.Item label="所在地">
              <Cascader v-model:value="resourceQueryRegionPath" allow-clear change-on-select :options="regionOptions" placeholder="可选择省 / 市 / 区县" show-search />
            </Form.Item>
            <Form.Item label="星钻标准"><Select v-model:value="resourceQuery.starStandard" allow-clear :options="standardOptions" /></Form.Item>
            <Form.Item label="状态"><Select v-model:value="resourceQuery.status" allow-clear :options="resourceStatusOptions" /></Form.Item>
          </BusinessSearchForm>
          <Table :columns="resourceColumns" :data-source="resourceRows" :loading="resourceLoading" :pagination="resourcePagination" row-key="id" :scroll="{ x: 1240 }" @change="handleResourceTableChange">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'hotel'">
                <div class="font-medium">{{ record.hotelName }}</div>
                <div class="text-xs text-gray-500">{{ record.address || record.sourceName || '-' }}</div>
              </template>
              <template v-else-if="column.key === 'region'">{{ regionText(record) }}</template>
              <template v-else-if="column.key === 'period'">{{ record.validFrom || '-' }} 至 {{ record.validTo || '-' }}</template>
              <template v-else-if="column.key === 'status'"><Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag></template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button type="link" size="small" @click="openEditResource(record as ResourceRow)">修改</Button>
                  <Button danger type="link" size="small" @click="confirmDeleteResource(record as ResourceRow)">删除</Button>
                </Space>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="types" tab="房型管理">
          <BusinessSearchForm
            label-width="78px"
            :model="roomTypeQuery"
            :search-loading="roomTypeLoading"
            create-text="新增"
            @create="openCreateRoomType"
            @reset="resetRoomTypeQuery"
            @search="() => { roomTypeQuery.page = 1; loadRoomTypes(); }"
          >
            <Form.Item label="自营酒店"><Select v-model:value="roomTypeQuery.resourceId" allow-clear show-search :options="resourceOptions" :loading="optionLoading" /></Form.Item>
            <Form.Item label="关键词"><Input v-model:value="roomTypeQuery.keyword" allow-clear placeholder="房型/床型" /></Form.Item>
            <Form.Item label="状态"><Select v-model:value="roomTypeQuery.status" allow-clear :options="enabledStatusOptions" /></Form.Item>
          </BusinessSearchForm>
          <Table :columns="roomTypeColumns" :data-source="roomTypeRows" :loading="roomTypeLoading" :pagination="roomTypePagination" row-key="id" :scroll="{ x: 1060 }" @change="handleRoomTypeTableChange">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'bed'">{{ record.bedType || '-' }} / {{ record.capacity || 0 }}人</template>
              <template v-else-if="column.key === 'price'">
                <div>{{ formatMoney(record.purchasePrice) }} 采购</div>
                <div class="text-xs text-gray-500">{{ formatMoney(record.agreementPrice) }} 协议</div>
              </template>
              <template v-else-if="column.key === 'status'"><Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag></template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button type="link" size="small" @click="openEditRoomType(record as RoomTypeRow)">修改</Button>
                  <Button danger type="link" size="small" @click="confirmDeleteRoomType(record as RoomTypeRow)">删除</Button>
                </Space>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="rooms" tab="房号管理">
          <BusinessSearchForm
            label-width="78px"
            :model="roomQuery"
            :search-loading="roomLoading"
            create-text="新增"
            @create="openCreateRoom"
            @reset="resetRoomQuery"
            @search="() => { roomQuery.page = 1; loadRooms(); }"
          >
            <Form.Item label="自营酒店"><Select v-model:value="roomQuery.resourceId" allow-clear show-search :options="resourceOptions" :loading="optionLoading" /></Form.Item>
            <Form.Item label="关键词"><Input v-model:value="roomQuery.keyword" allow-clear placeholder="房号/楼栋/房型" /></Form.Item>
            <Form.Item label="状态"><Select v-model:value="roomQuery.status" allow-clear :options="roomStatusOptions" /></Form.Item>
          </BusinessSearchForm>
          <Table :columns="roomColumns" :data-source="roomRows" :loading="roomLoading" :pagination="roomPagination" row-key="id" :scroll="{ x: 1060 }" @change="handleRoomTableChange">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'building'">{{ [record.buildingName, record.floorNo].filter(Boolean).join(' / ') || '-' }}</template>
              <template v-else-if="column.key === 'capacity'">{{ record.bedType || '-' }} / {{ record.capacity || 0 }}人</template>
              <template v-else-if="column.key === 'status'"><Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag></template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button type="link" size="small" @click="openEditRoom(record as RoomRow)">修改</Button>
                  <Button danger type="link" size="small" @click="confirmDeleteRoom(record as RoomRow)">删除</Button>
                </Space>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="inventories" tab="房态库存">
          <BusinessSearchForm
            label-width="78px"
            :model="inventoryQuery"
            :search-loading="inventoryLoading"
            create-text="锁房"
            @create="() => openLockModal()"
            @reset="resetInventoryQuery"
            @search="loadInventories"
          >
            <Form.Item label="来源"><Select v-model:value="inventoryQuery.sourceType" allow-clear :options="sourceTypeOptions" /></Form.Item>
            <Form.Item label="来源ID"><InputNumber v-model:value="inventoryQuery.sourceId" class="w-full" :min="1" placeholder="可按来源ID筛选" /></Form.Item>
            <Form.Item label="开始"><DatePicker class="w-full" :value="dateValue(inventoryQuery.startDate)" @update:value="(value) => setDate(inventoryQuery, 'startDate', value)" /></Form.Item>
            <Form.Item label="结束"><DatePicker class="w-full" :value="dateValue(inventoryQuery.endDate)" @update:value="(value) => setDate(inventoryQuery, 'endDate', value)" /></Form.Item>
            <Form.Item label="状态"><Select v-model:value="inventoryQuery.status" allow-clear :options="inventoryStatusOptions" /></Form.Item>
            <template #extraActions><Button @click="openGenerateModal">生成库存</Button></template>
          </BusinessSearchForm>
          <Table :columns="inventoryColumns" :data-source="inventoryRows" :loading="inventoryLoading" :pagination="false" row-key="id" :scroll="{ x: 980 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'source'">
                <div class="font-medium">{{ record.hotelName }}</div>
                <div class="text-xs text-gray-500">{{ sourceTypeLabel(record.sourceType) }} / {{ record.supplierName || '自营' }}</div>
              </template>
              <template v-else-if="column.key === 'quantity'">
                <Space>
                  <Tag>总 {{ record.totalQuantity }}</Tag>
                  <Tag color="blue">锁 {{ record.lockedQuantity }}</Tag>
                  <Tag color="purple">占 {{ record.occupiedQuantity }}</Tag>
                  <Button type="link" size="small" @click="openOccupancyDrawer(record as InventoryRow)">余 {{ record.remainingQuantity }}</Button>
                </Space>
              </template>
              <template v-else-if="column.key === 'status'"><Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag></template>
              <template v-else-if="column.key === 'action'"><Button type="link" size="small" @click="openLockModal(record as InventoryRow)">锁房</Button></template>
            </template>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="locks" tab="锁房记录">
          <BusinessSearchForm
            label-width="78px"
            :model="lockQuery"
            :search-loading="lockLoading"
            :show-create="false"
            @reset="resetLockQuery"
            @search="() => { lockQuery.page = 1; loadLocks(); }"
          >
            <Form.Item label="自营酒店"><Select v-model:value="lockQuery.resourceId" allow-clear :options="resourceOptions" /></Form.Item>
            <Form.Item label="团队号"><Input v-model:value="lockQuery.teamNo" allow-clear placeholder="请输入团队号" /></Form.Item>
            <Form.Item label="状态"><Select v-model:value="lockQuery.status" allow-clear :options="lockStatusOptions" /></Form.Item>
          </BusinessSearchForm>
          <Table :columns="lockColumns" :data-source="lockRows" :loading="lockLoading" :pagination="lockPagination" row-key="id" :scroll="{ x: 1120 }" @change="handleLockTableChange">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'team'">
                <div>{{ record.teamNo || '-' }}</div>
                <div class="text-xs text-gray-500">{{ record.teamName || '' }}</div>
              </template>
              <template v-else-if="column.key === 'room'">
                <div>{{ record.hotelName || '-' }}</div>
                <div class="text-xs text-gray-500">{{ record.roomType || '-' }}{{ record.roomNo ? ` / ${record.roomNo}` : '' }}</div>
              </template>
              <template v-else-if="column.key === 'period'">{{ record.checkInDate }} 至 {{ record.checkOutDate }}</template>
              <template v-else-if="column.key === 'status'"><Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag></template>
              <template v-else-if="column.key === 'action'">
                <Button v-if="record.status === 'locked'" type="link" size="small" @click="confirmReleaseLock(record as LockRow)">释放</Button>
                <span v-else class="text-gray-400">-</span>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>
      </Tabs>
    </Card>

    <Modal v-model:open="resourceModalOpen" :title="editingResourceId ? '修改自营酒店' : '新增自营酒店'" width="860px" ok-text="保存" cancel-text="取消" @ok="saveResource">
      <Form :model="resourceForm" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="酒店名称" required><Input v-model:value="resourceForm.hotelName" allow-clear /></Form.Item>
          <Form.Item label="所在地"><Cascader v-model:value="resourceFormRegionPath" allow-clear change-on-select :options="regionOptions" placeholder="可选择省 / 市 / 区县" show-search /></Form.Item>
          <Form.Item label="星钻标准"><Select v-model:value="resourceForm.starStandard" allow-clear :options="standardOptions" /></Form.Item>
          <Form.Item label="房源来源/酒店方"><Input v-model:value="resourceForm.sourceName" allow-clear /></Form.Item>
          <Form.Item label="有效期开始"><DatePicker class="w-full" :value="dateValue(resourceForm.validFrom)" @update:value="(value) => setDate(resourceForm, 'validFrom', value)" /></Form.Item>
          <Form.Item label="有效期结束"><DatePicker class="w-full" :value="dateValue(resourceForm.validTo)" @update:value="(value) => setDate(resourceForm, 'validTo', value)" /></Form.Item>
          <Form.Item label="联系人"><Input v-model:value="resourceForm.contactName" allow-clear /></Form.Item>
          <Form.Item label="联系电话"><Input v-model:value="resourceForm.contactPhone" allow-clear /></Form.Item>
          <Form.Item label="状态"><Select v-model:value="resourceForm.status" :options="resourceStatusOptions" /></Form.Item>
        </div>
        <Form.Item label="地址"><Input v-model:value="resourceForm.address" allow-clear /></Form.Item>
        <Form.Item label="备注"><Textarea v-model:value="resourceForm.remark" :auto-size="{ minRows: 3, maxRows: 5 }" /></Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="roomTypeModalOpen" :title="editingRoomTypeId ? '修改房型' : '新增房型'" width="760px" ok-text="保存" cancel-text="取消" @ok="saveRoomType">
      <Form :model="roomTypeForm" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="自营酒店" required><Select v-model:value="roomTypeForm.resourceId" show-search :options="resourceOptions" :loading="optionLoading" /></Form.Item>
          <Form.Item label="房型" required><Input v-model:value="roomTypeForm.roomType" allow-clear placeholder="如标间 / 大床房 / 三人间" /></Form.Item>
          <Form.Item label="床型"><Input v-model:value="roomTypeForm.bedType" allow-clear /></Form.Item>
          <Form.Item label="可住人数"><InputNumber v-model:value="roomTypeForm.capacity" class="w-full" :min="0" /></Form.Item>
          <Form.Item label="采购价"><InputNumber v-model:value="roomTypeForm.purchasePrice" class="w-full" :min="0" /></Form.Item>
          <Form.Item label="协议价"><InputNumber v-model:value="roomTypeForm.agreementPrice" class="w-full" :min="0" /></Form.Item>
          <Form.Item label="价格单位"><Input v-model:value="roomTypeForm.priceUnit" allow-clear /></Form.Item>
          <Form.Item label="状态"><Select v-model:value="roomTypeForm.status" :options="enabledStatusOptions" /></Form.Item>
        </div>
        <Form.Item label="备注"><Textarea v-model:value="roomTypeForm.remark" :auto-size="{ minRows: 3, maxRows: 5 }" /></Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="roomModalOpen" :title="editingRoomId ? '修改房号' : '新增房号'" width="760px" ok-text="保存" cancel-text="取消" @ok="saveRoom">
      <Form :model="roomForm" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="自营酒店" required><Select v-model:value="roomForm.resourceId" show-search :options="resourceOptions" :loading="optionLoading" /></Form.Item>
          <Form.Item label="房号" required><Input v-model:value="roomForm.roomNo" allow-clear /></Form.Item>
          <Form.Item label="楼栋"><Input v-model:value="roomForm.buildingName" allow-clear /></Form.Item>
          <Form.Item label="楼层"><Input v-model:value="roomForm.floorNo" allow-clear /></Form.Item>
          <Form.Item label="房型"><Input v-model:value="roomForm.roomType" allow-clear /></Form.Item>
          <Form.Item label="床型"><Input v-model:value="roomForm.bedType" allow-clear /></Form.Item>
          <Form.Item label="可住人数"><InputNumber v-model:value="roomForm.capacity" class="w-full" :min="0" /></Form.Item>
          <Form.Item label="状态"><Select v-model:value="roomForm.status" :options="roomStatusOptions" /></Form.Item>
        </div>
        <Form.Item label="备注"><Textarea v-model:value="roomForm.remark" :auto-size="{ minRows: 3, maxRows: 5 }" /></Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="generateModalOpen" title="生成房态库存" width="760px" ok-text="生成" cancel-text="取消" @ok="submitGenerate">
      <Form :model="generateForm" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="来源类型" required><Select v-model:value="generateForm.sourceType" :options="sourceTypeOptions" /></Form.Item>
          <Form.Item v-if="generateForm.sourceType === 'self_owned'" label="自营酒店" required><Select v-model:value="generateForm.selfResourceId" show-search :options="resourceOptions" :loading="optionLoading" /></Form.Item>
          <Form.Item v-else label="采购关系" required><Select v-model:value="generateForm.purchasedRelationId" show-search :options="purchaseRelationOptions" :loading="optionLoading" /></Form.Item>
          <Form.Item v-if="generateForm.sourceType === 'self_owned'" label="房型" required><Select v-model:value="generateForm.roomTypeId" show-search :options="roomTypeOptions" :loading="optionLoading" /></Form.Item>
          <Form.Item v-else label="房型" required><Input v-model:value="generateForm.roomType" allow-clear placeholder="资源采购房源手填房型" /></Form.Item>
          <Form.Item label="总量" required><InputNumber v-model:value="generateForm.totalQuantity" class="w-full" :min="0" /></Form.Item>
          <Form.Item label="开始日期" required><DatePicker class="w-full" :value="dateValue(generateForm.startDate)" @update:value="(value) => setDate(generateForm, 'startDate', value)" /></Form.Item>
          <Form.Item label="结束日期" required><DatePicker class="w-full" :value="dateValue(generateForm.endDate)" @update:value="(value) => setDate(generateForm, 'endDate', value)" /></Form.Item>
        </div>
      </Form>
    </Modal>

    <Modal v-model:open="lockModalOpen" title="按房型数量锁房" width="760px" ok-text="锁房" cancel-text="取消" @ok="submitLock">
      <Form :model="lockForm" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="来源类型" required><Select v-model:value="lockForm.sourceType" :options="sourceTypeOptions" /></Form.Item>
          <Form.Item v-if="lockForm.sourceType === 'self_owned'" label="自营酒店" required><Select v-model:value="lockForm.selfResourceId" show-search :options="resourceOptions" :loading="optionLoading" /></Form.Item>
          <Form.Item v-else label="采购关系" required><Select v-model:value="lockForm.purchasedRelationId" show-search :options="purchaseRelationOptions" :loading="optionLoading" /></Form.Item>
          <Form.Item v-if="lockForm.sourceType === 'self_owned'" label="房型" required><Select v-model:value="lockForm.roomTypeId" show-search :options="roomTypeOptions" :loading="optionLoading" /></Form.Item>
          <Form.Item v-else label="房型" required><Input v-model:value="lockForm.roomType" allow-clear /></Form.Item>
          <Form.Item label="数量" required><InputNumber v-model:value="lockForm.quantity" class="w-full" :min="1" /></Form.Item>
          <Form.Item label="入住日期" required><DatePicker class="w-full" :value="dateValue(lockForm.checkInDate)" @update:value="(value) => setDate(lockForm, 'checkInDate', value)" /></Form.Item>
          <Form.Item label="退房日期" required><DatePicker class="w-full" :value="dateValue(lockForm.checkOutDate)" @update:value="(value) => setDate(lockForm, 'checkOutDate', value)" /></Form.Item>
          <Form.Item label="团队号"><Input v-model:value="lockForm.teamNo" allow-clear /></Form.Item>
          <Form.Item label="团队名称"><Input v-model:value="lockForm.teamName" allow-clear /></Form.Item>
          <Form.Item label="团队标准"><Select v-model:value="lockForm.requiredStandard" allow-clear :options="standardOptions" /></Form.Item>
        </div>
        <Form.Item label="备注"><Textarea v-model:value="lockForm.remark" :auto-size="{ minRows: 3, maxRows: 5 }" /></Form.Item>
      </Form>
    </Modal>

    <Drawer v-model:open="occupancyDrawerOpen" title="库存占用明细" width="560px">
      <Table :columns="occupancyColumns" :data-source="occupancyRows" :loading="occupancyLoading" :pagination="false" row-key="lockRecordId">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'team'">
            <div>{{ record.teamNo || '-' }}</div>
            <div class="text-xs text-gray-500">{{ record.teamName || '' }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag></template>
          <template v-else-if="column.key === 'createdAt'">{{ formatDateTime(record.createdAt) }}</template>
        </template>
      </Table>
    </Drawer>
  </Page>
</template>

<style scoped>
.modal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (max-width: 768px) {
  .modal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
