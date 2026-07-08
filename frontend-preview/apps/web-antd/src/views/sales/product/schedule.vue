<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';
import type { Dayjs } from 'dayjs';
import type { CustomerCategoryApi } from '#/api/customer/category';
import type { EnterpriseEmployeeApi } from '#/api/enterprise/employee';
import type { SalesTeamApi } from '#/api/sales/team';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Checkbox,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getCustomerCategoryAll } from '#/api/customer/category';
import { getEnterpriseEmployeeAll } from '#/api/enterprise/employee';
import { getSalesProductDetail, type SalesProductApi } from '#/api/sales/product';
import {
  batchEditSalesTeamSchedule,
  batchCreateSalesTeamSchedule,
  changeSalesTeamStatus,
  getSalesTeamSchedulePage,
  saveSalesTeam,
  saveSalesTeamPrice,
} from '#/api/sales/team';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

type DatePickerValue = Dayjs | string | null | undefined;
type DateRangeValue = [string, string] | undefined;
type TeamRow = SalesTeamApi.Item;
type EditCategoryItem = SalesTeamApi.BatchEditCustomerCategory;
type PriceRow = SalesTeamApi.PriceItem & {
  rowKey: string;
  team: TeamRow;
  teamRowSpan: number;
};

const route = useRoute();
const router = useRouter();

const productId = computed(() => Number(Array.isArray(route.params.id) ? route.params.id[0] : route.params.id));
const product = ref<SalesProductApi.Item>();
const loading = ref(false);
const productLoading = ref(false);
const optionLoading = ref(false);
const batchModalOpen = ref(false);
const editModalOpen = ref(false);
const saving = ref(false);
const selectedTeamIds = ref<number[]>([]);
const employees = ref<EnterpriseEmployeeApi.Item[]>([]);
const categories = ref<CustomerCategoryApi.CustomerCategory[]>([]);
const teams = ref<TeamRow[]>([]);
const activeBatchCreateTab = ref<'batch' | 'specific'>('batch');
const specificDateCandidates = ref<string[]>([]);
const specificSelectedDates = ref<string[]>([]);
const checkedCandidateDates = ref<string[]>([]);
const checkedSpecificDates = ref<string[]>([]);
const selectedEditTeamIds = ref<number[]>([]);
const checkedEditCandidateTeamIds = ref<number[]>([]);
const checkedEditSelectedTeamIds = ref<number[]>([]);
const selectedEditCustomerCategories = ref<EditCategoryItem[]>([]);
const checkedEditCandidateCategoryIds = ref<number[]>([]);
const checkedEditSelectedCategoryIds = ref<number[]>([]);

const statusOptions = [
  { label: '正常', value: 'normal' },
  { label: '停收', value: 'stopped' },
  { label: '取消', value: 'cancelled' },
];

const weekdayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
];

const columns = [
  { customCell: teamCell, key: 'select', title: '全选', width: 52 },
  { customCell: teamCell, key: 'teamNo', title: '团号', width: 148 },
  { customCell: teamCell, key: 'departureDate', title: '发团日期', width: 108 },
  { customCell: teamCell, key: 'operator', title: '操作计调', width: 112 },
  { customCell: teamCell, key: 'status', title: '状态', width: 78 },
  { customCell: teamCell, key: 'seats', title: '总位 / 余位', width: 108 },
  { customCell: teamCell, key: 'singleRoomDifference', title: '单房差价格', width: 108 },
  { key: 'customerCategoryName', title: '客户类型', width: 94 },
  { key: 'adultPrice', title: '成人', width: 86 },
  { key: 'childPrice', title: '儿童', width: 86 },
  { key: 'childNoBedPrice', title: '儿童不占床', width: 104 },
  { key: 'seniorPrice', title: '老人', width: 86 },
  { key: 'extraFee', title: '附加费用', width: 92 },
];

const query = reactive<SalesTeamApi.QueryParams>({
  page: 1,
  pageSize: 20,
  productId: 0,
});

const departureDateRange = computed<DateRangeValue>({
  get(): DateRangeValue {
    if (!query.startDate || !query.endDate) return undefined;
    return [query.startDate, query.endDate];
  },
  set(value: DateRangeValue) {
    query.startDate = value?.[0];
    query.endDate = value?.[1];
  },
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 20,
  showSizeChanger: true,
  total: 0,
});

const batchForm = reactive<SalesTeamApi.BatchCreateParams>({
  adultPrice: 0,
  childNoBedPrice: 0,
  childPrice: 0,
  customerCategoryName: '默认',
  endDate: dayjs().format('YYYY-MM-DD'),
  extraFee: 0,
  seniorPrice: 0,
  singleRoomDifference: 0,
  startDate: dayjs().format('YYYY-MM-DD'),
  totalSeats: 0,
  weekdays: [1, 2, 3, 4, 5, 6, 7],
});

const batchEditForm = reactive({
  adultPrice: 0,
  childNoBedPrice: 0,
  childPrice: 0,
  deletePrice: false,
  extraFee: 0,
  seniorPrice: 0,
  singleRoomDifference: 0,
  synchronizeProduct: false,
  synchronizeProductWithoutSingleRoom: false,
  totalSeats: 0,
  updatePrice: false,
  updateSingleRoomDifference: false,
  updateTotalSeats: false,
});
function teamCell(record: Record<string, any>) {
  return {
    rowSpan: (record as PriceRow).teamRowSpan,
  };
}

const employeeOptions = computed(() => employees.value.map((item) => ({
  label: item.employeeName,
  value: item.id,
})));

const categoryOptions = computed(() => [
  { label: '默认', value: 0 },
  ...categories.value.map((item) => ({
    label: item.categoryName,
    value: item.id,
  })),
]);

const specificDateCandidateOptions = computed(() => specificDateCandidates.value.map((date) => ({
  label: date,
  value: date,
})));

const specificSelectedDateOptions = computed(() => specificSelectedDates.value.map((date) => ({
  label: date,
  value: date,
})));

const editCandidateTeamOptions = computed(() => teams.value
  .filter((team) => !selectedEditTeamIds.value.includes(team.id))
  .map((team) => ({
    label: `${team.teamNo} / ${team.departureDate}`,
    value: team.id,
  })));

const editSelectedTeamOptions = computed(() => selectedEditTeamIds.value
  .map((teamId) => teams.value.find((team) => team.id === teamId))
  .filter((team): team is TeamRow => Boolean(team))
  .map((team) => ({
    label: `${team.teamNo} / ${team.departureDate}`,
    value: team.id,
  })));

const editCandidateCategoryOptions = computed(() => {
  const selectedValues = new Set(selectedEditCustomerCategories.value.map(editCategoryValue));
  return [
    { id: undefined, name: '默认' },
    ...categories.value.map((item) => ({ id: item.id, name: item.categoryName })),
  ]
    .filter((item) => !selectedValues.has(editCategoryValue(item)))
    .map((item) => ({
      label: item.name,
      value: editCategoryValue(item),
    }));
});

const editSelectedCategoryOptions = computed(() => selectedEditCustomerCategories.value.map((item) => ({
  label: item.name,
  value: editCategoryValue(item),
})));

const selectedEditCategorySummary = computed(() => (selectedEditCustomerCategories.value.length
  ? `${selectedEditCustomerCategories.value.length} 个客户类型`
  : '全部客户类型'));

const flatRows = computed<PriceRow[]>(() => {
  const rows: PriceRow[] = [];
  for (const team of teams.value) {
    const prices = team.prices?.length ? team.prices : [createEmptyPrice(team)];
    prices.forEach((price, index) => {
      rows.push({
        ...price,
        rowKey: `${team.id}-${price.id || index}`,
        team,
        teamRowSpan: index === 0 ? prices.length : 0,
      });
    });
  }
  return rows;
});

function createEmptyPrice(team: TeamRow): SalesTeamApi.PriceItem {
  return {
    adultPrice: 0,
    childNoBedPrice: 0,
    childPrice: 0,
    customerCategoryName: '默认',
    extraFee: 0,
    id: 0,
    seniorPrice: 0,
    status: 'active',
    teamId: team.id,
  };
}

function dateValue(value?: string) {
  return value ? dayjs(value) : undefined;
}

function setDate(target: Record<string, any>, key: string, value: DatePickerValue) {
  target[key] = dayjs.isDayjs(value) ? value.format('YYYY-MM-DD') : value ? dayjs(value).format('YYYY-MM-DD') : undefined;
}

function statusLabel(value?: string) {
  if (value === 'stopped') return '停售';
  return statusOptions.find((item) => item.value === value)?.label || '-';
}

function statusColor(value?: string) {
  if (value === 'normal') return 'green';
  if (value === 'stopped') return 'orange';
  if (value === 'cancelled') return 'red';
  return 'default';
}

function selectedCategoryName(categoryId?: number) {
  if (!categoryId) return '默认';
  return categories.value.find((item) => item.id === categoryId)?.categoryName || '默认';
}

function editCategoryValue(category: { id?: number }) {
  return category.id || 0;
}

function buildEditCategory(value: number): EditCategoryItem {
  if (!value) {
    return { name: '默认' };
  }
  const category = categories.value.find((item) => item.id === value);
  return {
    id: value,
    name: category?.categoryName || `客户类型${value}`,
  };
}

function applyOperatorName(target: { operatorEmployeeId?: number; operatorEmployeeName?: string }) {
  const employee = employees.value.find((item) => item.id === target.operatorEmployeeId);
  target.operatorEmployeeName = employee?.employeeName;
}

function syncTeamInList(updated: TeamRow) {
  const index = teams.value.findIndex((team) => team.id === updated.id);
  if (index >= 0) {
    teams.value[index] = updated;
  }
}

function syncPriceInList(teamId: number, saved: SalesTeamApi.PriceItem) {
  const team = teams.value.find((item) => item.id === teamId);
  if (!team) return;
  const prices = team.prices || [];
  const index = prices.findIndex((item) => (
    (saved.id && item.id === saved.id)
    || item.customerCategoryId === saved.customerCategoryId
    || (!item.customerCategoryId && !saved.customerCategoryId && item.customerCategoryName === saved.customerCategoryName)
  ));
  if (index >= 0) {
    prices[index] = saved;
  } else {
    prices.push(saved);
  }
  team.prices = prices;
}

function pricePayload(record: PriceRow): SalesTeamApi.PriceSaveParams {
  return {
    adultPrice: Number(record.adultPrice || 0),
    childNoBedPrice: Number(record.childNoBedPrice || 0),
    childPrice: Number(record.childPrice || 0),
    customerCategoryId: record.customerCategoryId,
    customerCategoryName: record.customerCategoryName || '默认',
    extraFee: Number(record.extraFee || 0),
    seniorPrice: Number(record.seniorPrice || 0),
  };
}

async function saveInlineTeamField(team: TeamRow, field: 'singleRoomDifference' | 'totalSeats') {
  const payload: SalesTeamApi.TeamSaveParams = {};
  if (field === 'totalSeats') {
    payload.totalSeats = Number(team.totalSeats || 0);
  }
  if (field === 'singleRoomDifference') {
    payload.singleRoomDifference = Number(team.singleRoomDifference || 0);
  }
  const updated = await saveSalesTeam(team.id, payload);
  syncTeamInList(updated);
  message.success('团期信息已保存');
}

async function saveInlinePriceField(
  record: Record<string, any>,
  field: 'adultPrice' | 'childNoBedPrice' | 'childPrice' | 'extraFee' | 'seniorPrice',
) {
  if (!field) return;
  const priceRow = record as PriceRow;
  const saved = await saveSalesTeamPrice(priceRow.team.id, pricePayload(priceRow));
  syncPriceInList(priceRow.team.id, saved);
  message.success('价格已保存');
}

async function loadOptions() {
  optionLoading.value = true;
  try {
    const [employeeResult, categoryResult] = await Promise.all([
      getEnterpriseEmployeeAll(false),
      getCustomerCategoryAll(),
    ]);
    employees.value = employeeResult;
    categories.value = categoryResult.filter((item) => item.status === 'active');
  } finally {
    optionLoading.value = false;
  }
}

async function loadProduct() {
  productLoading.value = true;
  try {
    product.value = await getSalesProductDetail(productId.value);
    batchForm.totalSeats = product.value.plannedCapacity || 0;
    batchForm.singleRoomDifference = Number(product.value.singleRoomDifference || 0);
  } finally {
    productLoading.value = false;
  }
}

async function loadData() {
  if (!productId.value) return;
  loading.value = true;
  try {
    query.productId = productId.value;
    const result = await getSalesTeamSchedulePage(query);
    teams.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
    selectedTeamIds.value = selectedTeamIds.value.filter((id) => teams.value.some((team) => team.id === id));
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  loadData();
}

function resetQuery() {
  Object.assign(query, {
    endDate: undefined,
    keyword: undefined,
    page: 1,
    pageSize: query.pageSize,
    productId: productId.value,
    startDate: undefined,
    status: undefined,
  });
  loadData();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 20);
  loadData();
}

function rowSelectionChanged(teamId: number, checked: boolean) {
  const next = new Set(selectedTeamIds.value);
  if (checked) next.add(teamId);
  else next.delete(teamId);
  selectedTeamIds.value = [...next];
}

function allCurrentPageSelected() {
  const ids = teams.value.map((team) => team.id);
  return ids.length > 0 && ids.every((id) => selectedTeamIds.value.includes(id));
}

function toggleSelectAllCurrentPage(checked: boolean) {
  const next = new Set(selectedTeamIds.value);
  for (const team of teams.value) {
    if (checked) next.add(team.id);
    else next.delete(team.id);
  }
  selectedTeamIds.value = [...next];
}

function buildSpecificDateCandidates(start = dayjs()) {
  specificDateCandidates.value = Array.from({ length: 90 }, (_, index) => start.add(index, 'day').format('YYYY-MM-DD'));
  checkedCandidateDates.value = specificDateCandidates.value.slice(0, 1);
  specificSelectedDates.value = [];
  checkedSpecificDates.value = [];
}

function resetInlineCreateForm() {
  batchForm.startDate = dayjs().format('YYYY-MM-DD');
  batchForm.endDate = dayjs().format('YYYY-MM-DD');
  batchForm.weekdays = [1, 2, 3, 4, 5, 6, 7];
  batchForm.totalSeats = product.value?.plannedCapacity || 0;
  batchForm.singleRoomDifference = Number(product.value?.singleRoomDifference || 0);
  batchForm.customerCategoryId = undefined;
  batchForm.customerCategoryName = '默认';
  batchForm.adultPrice = 0;
  batchForm.childPrice = 0;
  batchForm.childNoBedPrice = 0;
  batchForm.seniorPrice = 0;
  batchForm.extraFee = 0;
  buildSpecificDateCandidates();
}

function openBatchModal() {
  activeBatchCreateTab.value = 'batch';
  if (specificDateCandidates.value.length === 0) {
    buildSpecificDateCandidates();
  }
  batchModalOpen.value = true;
}

function openEditModal() {
  selectedEditTeamIds.value = selectedTeamIds.value.filter((id) => teams.value.some((team) => team.id === id));
  checkedEditCandidateTeamIds.value = [];
  checkedEditSelectedTeamIds.value = [];
  selectedEditCustomerCategories.value = [];
  checkedEditCandidateCategoryIds.value = [];
  checkedEditSelectedCategoryIds.value = [];
  batchEditForm.synchronizeProduct = false;
  batchEditForm.synchronizeProductWithoutSingleRoom = false;
  batchEditForm.deletePrice = false;
  batchEditForm.updateTotalSeats = false;
  batchEditForm.updateSingleRoomDifference = false;
  batchEditForm.updatePrice = false;
  batchEditForm.totalSeats = 0;
  batchEditForm.singleRoomDifference = 0;
  batchEditForm.adultPrice = 0;
  batchEditForm.childPrice = 0;
  batchEditForm.childNoBedPrice = 0;
  batchEditForm.seniorPrice = 0;
  batchEditForm.extraFee = 0;
  editModalOpen.value = true;
}

function closeBatchModalAfterCreate() {
  batchModalOpen.value = false;
  checkedCandidateDates.value = [];
  checkedSpecificDates.value = [];
  specificSelectedDates.value = [];
}

async function submitInlineBatchCreate() {
  if (!batchForm.startDate || !batchForm.endDate) {
    message.warning('请选择开始日期和结束日期');
    return;
  }
  saving.value = true;
  try {
    applyOperatorName(batchForm);
    batchForm.customerCategoryName = selectedCategoryName(batchForm.customerCategoryId);
    const created = await batchCreateSalesTeamSchedule(productId.value, batchForm);
    message.success(`已生成 ${created.length} 条团期`);
    await loadData();
    closeBatchModalAfterCreate();
  } finally {
    saving.value = false;
  }
}

function addSpecificDates() {
  if (checkedCandidateDates.value.length === 0) {
    message.warning('请选择需要添加的团期日期');
    return;
  }
  const selected = new Set(specificSelectedDates.value);
  checkedCandidateDates.value.forEach((date) => selected.add(date));
  specificSelectedDates.value = [...selected].sort();
  specificDateCandidates.value = specificDateCandidates.value.filter((date) => !selected.has(date));
  checkedCandidateDates.value = [];
  checkedSpecificDates.value = [];
}

function removeSpecificDates() {
  if (checkedSpecificDates.value.length === 0) {
    message.warning('请选择需要删除的已选日期');
    return;
  }
  const removing = new Set(checkedSpecificDates.value);
  specificSelectedDates.value = specificSelectedDates.value.filter((date) => !removing.has(date));
  specificDateCandidates.value = [...specificDateCandidates.value, ...checkedSpecificDates.value].sort();
  checkedSpecificDates.value = [];
}

function moveEditTeamsToSelected() {
  if (checkedEditCandidateTeamIds.value.length === 0) {
    message.warning('请选择可选团期');
    return;
  }
  const selected = new Set(selectedEditTeamIds.value);
  checkedEditCandidateTeamIds.value.forEach((teamId) => selected.add(teamId));
  selectedEditTeamIds.value = [...selected];
  selectedTeamIds.value = selectedEditTeamIds.value;
  checkedEditCandidateTeamIds.value = [];
  checkedEditSelectedTeamIds.value = [];
}

function removeEditTeamsFromSelected() {
  if (checkedEditSelectedTeamIds.value.length === 0) {
    message.warning('请选择已选团期');
    return;
  }
  const removing = new Set(checkedEditSelectedTeamIds.value);
  selectedEditTeamIds.value = selectedEditTeamIds.value.filter((teamId) => !removing.has(teamId));
  selectedTeamIds.value = selectedEditTeamIds.value;
  checkedEditSelectedTeamIds.value = [];
}

function moveEditCustomerCategoriesToSelected() {
  if (checkedEditCandidateCategoryIds.value.length === 0) {
    message.warning('请选择可选客户类型');
    return;
  }
  const selected = new Map(selectedEditCustomerCategories.value.map((item) => [editCategoryValue(item), item]));
  checkedEditCandidateCategoryIds.value.forEach((value) => {
    selected.set(value, buildEditCategory(value));
  });
  selectedEditCustomerCategories.value = [...selected.values()];
  checkedEditCandidateCategoryIds.value = [];
  checkedEditSelectedCategoryIds.value = [];
}

function removeEditCustomerCategoriesFromSelected() {
  if (checkedEditSelectedCategoryIds.value.length === 0) {
    message.warning('请选择已选客户类型');
    return;
  }
  const removing = new Set(checkedEditSelectedCategoryIds.value);
  selectedEditCustomerCategories.value = selectedEditCustomerCategories.value
    .filter((item) => !removing.has(editCategoryValue(item)));
  checkedEditSelectedCategoryIds.value = [];
}

async function submitSpecificDateCreate() {
  if (specificSelectedDates.value.length === 0) {
    message.warning('请先添加特定团期日期');
    return;
  }
  const dates = [...specificSelectedDates.value].sort();
  const [firstDate] = dates;
  if (!firstDate) {
    message.warning('请先添加特定团期日期');
    return;
  }
  saving.value = true;
  try {
    applyOperatorName(batchForm);
    batchForm.customerCategoryName = selectedCategoryName(batchForm.customerCategoryId);
    const created = await batchCreateSalesTeamSchedule(productId.value, {
      ...batchForm,
      dates,
      endDate: dates.at(-1) || firstDate,
      startDate: firstDate,
      weekdays: [],
    });
    message.success(`已生成 ${created.length} 条特定团期`);
    await loadData();
    closeBatchModalAfterCreate();
  } finally {
    saving.value = false;
  }
}

async function submitSelectedTeamBatchEdit() {
  if (selectedEditTeamIds.value.length === 0) {
    message.warning('请先勾选需要修改的团期');
    return;
  }
  if (
    !batchEditForm.updateTotalSeats
    && !batchEditForm.updateSingleRoomDifference
    && !batchEditForm.updatePrice
    && !batchEditForm.deletePrice
  ) {
    if (batchEditForm.synchronizeProduct || batchEditForm.synchronizeProductWithoutSingleRoom) {
      message.info('同步产品内容入口已保留，本轮请同时选择人数、单房差、价格或删除价格');
      return;
    }
    message.warning('请选择需要修改的内容');
    return;
  }
  if (batchEditForm.updatePrice && batchEditForm.deletePrice) {
    message.warning('修改价格和删除价格不能同时勾选');
    return;
  }
  saving.value = true;
  try {
    const payload: SalesTeamApi.BatchEditParams = {
      customerCategories: selectedEditCustomerCategories.value,
      deletePrice: batchEditForm.deletePrice,
      synchronizeProduct: batchEditForm.synchronizeProduct,
      synchronizeProductWithoutSingleRoom: batchEditForm.synchronizeProductWithoutSingleRoom,
      teamIds: selectedEditTeamIds.value,
      updateSingleRoomDifference: batchEditForm.updateSingleRoomDifference,
      updateTotalSeats: batchEditForm.updateTotalSeats,
    };
    if (batchEditForm.updateTotalSeats) {
      payload.totalSeats = batchEditForm.totalSeats;
    }
    if (batchEditForm.updateSingleRoomDifference) {
      payload.singleRoomDifference = batchEditForm.singleRoomDifference;
    }
    if (batchEditForm.updatePrice) {
      payload.adultPrice = batchEditForm.adultPrice;
      payload.childPrice = batchEditForm.childPrice;
      payload.childNoBedPrice = batchEditForm.childNoBedPrice;
      payload.seniorPrice = batchEditForm.seniorPrice;
      payload.extraFee = batchEditForm.extraFee;
    }
    await batchEditSalesTeamSchedule(payload);
    message.success(`已批量修改 ${selectedEditTeamIds.value.length} 个团期`);
    await loadData();
    editModalOpen.value = false;
  } finally {
    saving.value = false;
  }
}

function showPendingSync(name: string) {
  message.info(`${name}需要先确认覆盖范围，后续接入同步接口`);
}

function changeSelectedStatus(action: SalesTeamApi.StatusAction, label: string) {
  if (selectedTeamIds.value.length === 0) {
    message.warning('请先勾选团队');
    return;
  }
  Modal.confirm({
    cancelText: '取消',
    content: action === 'delete' ? '只有取消状态的团队可以删除，删除后将软删除团队和价格。' : `确认对已选团队执行「${label}」？`,
    okText: label,
    okType: action === 'delete' ? 'danger' : 'primary',
    title: `${label}已选团队`,
    async onOk() {
      await changeSalesTeamStatus({ action, teamIds: selectedTeamIds.value });
      message.success('状态已更新');
      selectedTeamIds.value = [];
      await loadData();
    },
  });
}

function openProductTab(tab: string) {
  router.push({
    path: `/sales/product/edit/${productId.value}`,
    query: { tab },
  });
}

onMounted(async () => {
  buildSpecificDateCandidates();
  await Promise.all([loadOptions(), loadProduct()]);
  await loadData();
});
</script>

<template>
  <Page class="schedule-page">
    <div class="schedule-shell">
      <Card class="schedule-shell-card">
        <div class="schedule-header">
          <div>
            <div class="schedule-title">团期管理 <span>Group management</span></div>
            <div v-if="product" class="schedule-product-name">{{ product.productName }}</div>
          </div>
          <Button class="top-batch-button" type="primary" :loading="productLoading" @click="openBatchModal">
            批量添加团队
          </Button>
        </div>

        <div class="schedule-tabs">
          <Button @click="openProductTab('basic')">基本信息</Button>
          <Button @click="openProductTab('itinerary')">行程内容</Button>
          <Button @click="openProductTab('description')">产品说明</Button>
          <Button @click="openProductTab('arrangement')">团队安排</Button>
          <Button @click="openProductTab('businessType')">业务类型管理</Button>
          <Button type="primary">团期管理</Button>
        </div>
      </Card>

      <Card class="schedule-list-card">
        <div class="schedule-section-title">团期信息列表</div>
        <BusinessSearchForm
          :model="query"
          :search-loading="loading"
          search-text="搜索"
          :show-create="false"
          @reset="resetQuery"
          @search="handleSearch"
        >
          <Form.Item label="团号">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="团号 / 操作计调"
            @press-enter="handleSearch"
          />
          </Form.Item>
          <Form.Item class="business-search-item--wide" label="出团日期">
            <DatePicker.RangePicker
              v-model:value="departureDateRange"
              value-format="YYYY-MM-DD"
              :placeholder="['开始日期', '结束日期']"
            />
          </Form.Item>
          <Form.Item label="状态">
          <Select
            v-model:value="query.status"
            allow-clear
            :options="statusOptions"
            placeholder="全部"
          />
          </Form.Item>
        </BusinessSearchForm>

      <Table
        :columns="columns"
        :data-source="flatRows"
        :loading="loading"
        :pagination="pagination"
        row-key="rowKey"
        :scroll="{ x: 1260 }"
        size="middle"
        bordered
        @change="handleTableChange"
      >
        <template #headerCell="{ column }">
          <template v-if="column.key === 'select'">
            <Checkbox
              :checked="allCurrentPageSelected()"
              :indeterminate="selectedTeamIds.length > 0 && !allCurrentPageSelected()"
              @change="(event) => toggleSelectAllCurrentPage(event.target.checked)"
            />
          </template>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'select'">
            <div v-if="record.teamRowSpan" :rowspan="record.teamRowSpan">
              <Checkbox
                :checked="selectedTeamIds.includes(record.team.id)"
                @change="(event) => rowSelectionChanged(record.team.id, event.target.checked)"
              />
            </div>
          </template>
          <template v-else-if="column.key === 'teamNo'">
            <div v-if="record.teamRowSpan" class="team-no">{{ record.team.teamNo }}</div>
          </template>
          <template v-else-if="column.key === 'departureDate'">
            <span v-if="record.teamRowSpan">{{ record.team.departureDate }}</span>
          </template>
          <template v-else-if="column.key === 'operator'">
            <span v-if="record.teamRowSpan">{{ record.team.operatorEmployeeName || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag v-if="record.teamRowSpan" :color="statusColor(record.team.status)">
              {{ statusLabel(record.team.status) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'seats'">
            <span v-if="record.teamRowSpan" class="editable-number">
              <InputNumber v-model:value="record.team.totalSeats" :controls="false" size="small" :min="0" @blur="() => saveInlineTeamField(record.team, 'totalSeats')" />
              <span>/{{ record.team.remainingSeats }}</span>
            </span>
          </template>
          <template v-else-if="column.key === 'singleRoomDifference'">
            <span v-if="record.teamRowSpan" class="editable-money">
              <InputNumber v-model:value="record.team.singleRoomDifference" :controls="false" size="small" :min="0" :precision="0" @blur="() => saveInlineTeamField(record.team, 'singleRoomDifference')" />
              <span>元</span>
            </span>
          </template>
          <template v-else-if="column.key === 'customerCategoryName'">{{ record.customerCategoryName || '默认' }}</template>
          <template v-else-if="column.key === 'adultPrice'"><span class="editable-money"><InputNumber v-model:value="record.adultPrice" :controls="false" size="small" :min="0" :precision="0" @blur="() => saveInlinePriceField(record, 'adultPrice')" /><span>元</span></span></template>
          <template v-else-if="column.key === 'childPrice'"><span class="editable-money"><InputNumber v-model:value="record.childPrice" :controls="false" size="small" :min="0" :precision="0" @blur="() => saveInlinePriceField(record, 'childPrice')" /><span>元</span></span></template>
          <template v-else-if="column.key === 'childNoBedPrice'"><span class="editable-money"><InputNumber v-model:value="record.childNoBedPrice" :controls="false" size="small" :min="0" :precision="0" @blur="() => saveInlinePriceField(record, 'childNoBedPrice')" /><span>元</span></span></template>
          <template v-else-if="column.key === 'seniorPrice'"><span class="editable-money"><InputNumber v-model:value="record.seniorPrice" :controls="false" size="small" :min="0" :precision="0" @blur="() => saveInlinePriceField(record, 'seniorPrice')" /><span>元</span></span></template>
          <template v-else-if="column.key === 'extraFee'"><span class="editable-money"><InputNumber v-model:value="record.extraFee" :controls="false" size="small" :min="0" :precision="0" @blur="() => saveInlinePriceField(record, 'extraFee')" /><span>元</span></span></template>
        </template>
      </Table>

      <div class="schedule-bottom-actions">
        <Space wrap>
          <Button @click="changeSelectedStatus('cancel', '取消团队')">取消团队</Button>
          <Button @click="changeSelectedStatus('recover', '恢复取消团队')">恢复取消团队</Button>
          <Button @click="changeSelectedStatus('stop', '团队停售')">团队停售</Button>
          <Button @click="changeSelectedStatus('start', '团队恢复收客')">团队恢复收客</Button>
          <Button danger @click="changeSelectedStatus('delete', '删除团队')">删除团队</Button>
          <Button @click="showPendingSync('同步产品内容')">同步产品内容</Button>
          <Button @click="showPendingSync('同步产品内容(不含单房差)')">同步产品内容(不含单房差)</Button>
          <Button type="primary" @click="openEditModal">添加/修改团期信息</Button>
        </Space>
        <div class="selected-tip">已选 {{ selectedTeamIds.length }} 个团队</div>
      </div>
    </Card>

    <Modal
      v-model:open="batchModalOpen"
      class="schedule-batch-modal"
      title="批量添加团队"
      width="1320px"
      :footer="null"
    >
      <div class="schedule-batch-modal-body">
        <Radio.Group v-model:value="activeBatchCreateTab" class="schedule-cost-mode-tabs" button-style="solid">
          <Radio.Button value="batch">批量团期</Radio.Button>
          <Radio.Button value="specific">特定团期</Radio.Button>
        </Radio.Group>

        <Form v-if="activeBatchCreateTab === 'batch'" class="schedule-batch-form" layout="vertical">
          <div class="schedule-field-group">
            <div class="schedule-group-title">
              <IconifyIcon icon="lucide:calendar-range" />
              <span>批量团期</span>
            </div>
            <div class="schedule-form-row six-columns">
                <Form.Item label="开始日期" required>
                  <DatePicker class="w-full" :value="dateValue(batchForm.startDate)" @update:value="(value) => setDate(batchForm, 'startDate', value)" />
                </Form.Item>
                <Form.Item label="结束日期" required>
                  <DatePicker class="w-full" :value="dateValue(batchForm.endDate)" @update:value="(value) => setDate(batchForm, 'endDate', value)" />
                </Form.Item>
                <Form.Item label="操作计调">
                  <Select
                    v-model:value="batchForm.operatorEmployeeId"
                    allow-clear
                    :loading="optionLoading"
                    :options="employeeOptions"
                    placeholder="请选择"
                  />
                </Form.Item>
                <Form.Item label="总位">
                  <InputNumber v-model:value="batchForm.totalSeats" class="w-full" :min="0" />
                </Form.Item>
                <Form.Item label="单房差">
                  <InputNumber v-model:value="batchForm.singleRoomDifference" class="w-full" :min="0" :precision="2" prefix="¥" />
                </Form.Item>
                <Form.Item label="客户类型">
                  <Select
                    v-model:value="batchForm.customerCategoryId"
                    :loading="optionLoading"
                    :options="categoryOptions"
                    placeholder="默认"
                    @change="batchForm.customerCategoryName = selectedCategoryName(batchForm.customerCategoryId)"
                  />
                </Form.Item>
            </div>
          </div>
          <div class="schedule-field-group">
            <div class="schedule-group-title">
              <IconifyIcon icon="lucide:calendar-days" />
              <span>生成星期</span>
            </div>
            <Form.Item label="生成星期">
              <Checkbox.Group v-model:value="batchForm.weekdays" :options="weekdayOptions" />
            </Form.Item>
          </div>
          <div class="schedule-field-group">
            <div class="schedule-group-title">
              <IconifyIcon icon="lucide:receipt-text" />
              <span>价格信息</span>
            </div>
            <div class="schedule-form-row five-columns">
              <Form.Item label="成人"><InputNumber v-model:value="batchForm.adultPrice" class="w-full" :min="0" :precision="2" prefix="¥" /></Form.Item>
              <Form.Item label="儿童"><InputNumber v-model:value="batchForm.childPrice" class="w-full" :min="0" :precision="2" prefix="¥" /></Form.Item>
              <Form.Item label="儿童不占床"><InputNumber v-model:value="batchForm.childNoBedPrice" class="w-full" :min="0" :precision="2" prefix="¥" /></Form.Item>
              <Form.Item label="老人"><InputNumber v-model:value="batchForm.seniorPrice" class="w-full" :min="0" :precision="2" prefix="¥" /></Form.Item>
              <Form.Item label="附加费用"><InputNumber v-model:value="batchForm.extraFee" class="w-full" :min="0" :precision="2" prefix="¥" /></Form.Item>
            </div>
          </div>
          <div class="schedule-modal-footer">
            <Button @click="resetInlineCreateForm">重置</Button>
            <Button type="primary" :loading="saving" @click="submitInlineBatchCreate">添加团期</Button>
          </div>
        </Form>

        <Form v-else class="schedule-batch-form" layout="vertical">
          <div class="schedule-field-group">
            <div class="schedule-group-title">
              <IconifyIcon icon="lucide:list-checks" />
              <span>特定团期添加</span>
              <small>添加某几天，支持多选后一次生成</small>
            </div>
            <div class="specific-date-transfer">
              <div class="specific-date-panel">
                <div class="specific-date-panel-title">可选团期</div>
                <div class="specific-date-list">
                  <Checkbox.Group v-model:value="checkedCandidateDates" :options="specificDateCandidateOptions" />
                </div>
              </div>
              <div class="specific-date-actions">
                <Button @click="addSpecificDates">添加&gt;&gt;</Button>
                <Button @click="removeSpecificDates">&lt;&lt;删除</Button>
              </div>
              <div class="specific-date-panel">
                <div class="specific-date-panel-title">已选团期</div>
                <div class="specific-date-list selected">
                  <Checkbox.Group
                    v-if="specificSelectedDateOptions.length"
                    v-model:value="checkedSpecificDates"
                    :options="specificSelectedDateOptions"
                  />
                  <div v-else class="specific-date-empty">未选择特定团期</div>
                </div>
              </div>
              <div class="specific-date-side-form">
                <Form.Item label="客户类型">
                  <Select
                    v-model:value="batchForm.customerCategoryId"
                    :loading="optionLoading"
                    :options="categoryOptions"
                    placeholder="默认"
                    @change="batchForm.customerCategoryName = selectedCategoryName(batchForm.customerCategoryId)"
                  />
                </Form.Item>
                <Form.Item label="操作计调">
                  <Select
                    v-model:value="batchForm.operatorEmployeeId"
                    allow-clear
                    :loading="optionLoading"
                    :options="employeeOptions"
                    placeholder="请选择"
                  />
                </Form.Item>
              </div>
            </div>
          </div>
          <div class="schedule-field-group">
            <div class="schedule-group-title">
              <IconifyIcon icon="lucide:receipt-text" />
              <span>价格信息</span>
            </div>
            <div class="specific-price-grid">
              <Form.Item label="预控人数">
                <InputNumber v-model:value="batchForm.totalSeats" class="w-full" :min="0" />
              </Form.Item>
              <Form.Item label="单房差">
                <InputNumber v-model:value="batchForm.singleRoomDifference" class="w-full" :min="0" :precision="2" />
              </Form.Item>
              <Form.Item label="附加费用">
                <InputNumber v-model:value="batchForm.extraFee" class="w-full" :min="0" :precision="2" />
              </Form.Item>
              <Form.Item label="成人价格">
                <InputNumber v-model:value="batchForm.adultPrice" class="w-full" :min="0" :precision="2" />
              </Form.Item>
              <Form.Item label="儿童价格">
                <InputNumber v-model:value="batchForm.childPrice" class="w-full" :min="0" :precision="2" />
              </Form.Item>
              <Form.Item label="儿童不占床价格">
                <InputNumber v-model:value="batchForm.childNoBedPrice" class="w-full" :min="0" :precision="2" />
              </Form.Item>
              <Form.Item label="老人价格">
                <InputNumber v-model:value="batchForm.seniorPrice" class="w-full" :min="0" :precision="2" />
              </Form.Item>
            </div>
          </div>
          <div class="schedule-modal-footer">
            <Button type="primary" :loading="saving" @click="submitSpecificDateCreate">提交保存</Button>
          </div>
        </Form>
      </div>
    </Modal>

    <Modal
      v-model:open="editModalOpen"
      title="添加/修改团期信息"
      width="1080px"
      :footer="null"
    >
      <div class="schedule-edit-layout">
        <section class="modal-form-section">
          <div class="modal-section-title">
            批量修改已选团期
            <span>按老系统规则：先选团期，再选客户类型，最后勾选要修改的项目。</span>
          </div>
          <Form class="schedule-batch-form" layout="vertical">
            <div class="schedule-field-group">
              <div class="schedule-group-title">
                <IconifyIcon icon="lucide:list-checks" />
                <span>团期选择</span>
                <small>可从当前列表继续添加或删除本次要修改的团期</small>
              </div>
              <div class="edit-transfer-grid">
                <div class="specific-date-panel">
                  <div class="specific-date-panel-title">可选团期</div>
                  <div class="specific-date-list edit-list">
                    <Checkbox.Group
                      v-if="editCandidateTeamOptions.length"
                      v-model:value="checkedEditCandidateTeamIds"
                      :options="editCandidateTeamOptions"
                    />
                    <div v-else class="specific-date-empty">当前列表没有可选团期</div>
                  </div>
                </div>
                <div class="specific-date-actions edit-actions">
                  <Button @click="moveEditTeamsToSelected">添加&gt;&gt;</Button>
                  <Button @click="removeEditTeamsFromSelected">&lt;&lt;删除</Button>
                </div>
                <div class="specific-date-panel">
                  <div class="specific-date-panel-title">已选团期</div>
                  <div class="specific-date-list edit-list selected">
                    <Checkbox.Group
                      v-if="editSelectedTeamOptions.length"
                      v-model:value="checkedEditSelectedTeamIds"
                      :options="editSelectedTeamOptions"
                    />
                    <div v-else class="specific-date-empty">未选择团期</div>
                  </div>
                </div>
              </div>
            </div>

            <div class="schedule-field-group">
              <div class="schedule-group-title">
                <IconifyIcon icon="lucide:users" />
                <span>客户类型选择</span>
                <small>未选择客户分类时，将修改原有的所有价格；若原先无价格，则添加为默认价格</small>
              </div>
              <div class="edit-transfer-grid category-transfer">
                <div class="specific-date-panel">
                  <div class="specific-date-panel-title">可选客户类型</div>
                  <div class="specific-date-list edit-list short">
                    <Checkbox.Group
                      v-if="editCandidateCategoryOptions.length"
                      v-model:value="checkedEditCandidateCategoryIds"
                      :options="editCandidateCategoryOptions"
                    />
                    <div v-else class="specific-date-empty">没有可选客户类型</div>
                  </div>
                </div>
                <div class="specific-date-actions edit-actions short">
                  <Button @click="moveEditCustomerCategoriesToSelected">添加&gt;&gt;</Button>
                  <Button @click="removeEditCustomerCategoriesFromSelected">&lt;&lt;删除</Button>
                </div>
                <div class="specific-date-panel">
                  <div class="specific-date-panel-title">已选客户类型</div>
                  <div class="specific-date-list edit-list short selected">
                    <Checkbox.Group
                      v-if="editSelectedCategoryOptions.length"
                      v-model:value="checkedEditSelectedCategoryIds"
                      :options="editSelectedCategoryOptions"
                    />
                    <div v-else class="specific-date-empty">未选择客户分类时，将修改全部价格</div>
                  </div>
                </div>
              </div>
            </div>

            <div class="schedule-field-group">
              <div class="schedule-group-title">
                <IconifyIcon icon="lucide:settings-2" />
                <span>修改项目</span>
              </div>
              <div class="edit-check-grid">
                <Checkbox v-model:checked="batchEditForm.synchronizeProduct">同步产品内容</Checkbox>
                <Checkbox v-model:checked="batchEditForm.synchronizeProductWithoutSingleRoom">同步产品内容（不含单房差）</Checkbox>
                <Checkbox v-model:checked="batchEditForm.deletePrice">删除价格（未选择客户分类时，将删除全部价格）</Checkbox>
                <div class="edit-check-field">
                  <Checkbox v-model:checked="batchEditForm.updateTotalSeats" aria-label="修改预控人数">预控人数</Checkbox>
                  <InputNumber v-model:value="batchEditForm.totalSeats" class="w-full" :disabled="!batchEditForm.updateTotalSeats" :min="0" />
                </div>
                <div class="edit-check-field">
                  <Checkbox v-model:checked="batchEditForm.updateSingleRoomDifference" aria-label="修改单房差">单 房 差</Checkbox>
                  <InputNumber v-model:value="batchEditForm.singleRoomDifference" class="w-full" :disabled="!batchEditForm.updateSingleRoomDifference" :min="0" :precision="2" prefix="¥" />
                </div>
                <Checkbox v-model:checked="batchEditForm.updatePrice">成人/儿童/儿童不占床/老人/附加费用</Checkbox>
              </div>
              <div class="edit-price-hint">
                ( 未选择客户分类时，将修改原有的所有价格; 若原先无价格，则添加为默认价格 )
              </div>
              <div class="inline-grid price edit-price-grid">
                <Form.Item label="成人价格"><InputNumber v-model:value="batchEditForm.adultPrice" class="w-full" :disabled="!batchEditForm.updatePrice" :min="0" :precision="2" prefix="¥" /></Form.Item>
                <Form.Item label="儿童价格"><InputNumber v-model:value="batchEditForm.childPrice" class="w-full" :disabled="!batchEditForm.updatePrice" :min="0" :precision="2" prefix="¥" /></Form.Item>
                <Form.Item label="儿童[不占床]价格"><InputNumber v-model:value="batchEditForm.childNoBedPrice" class="w-full" :disabled="!batchEditForm.updatePrice" :min="0" :precision="2" prefix="¥" /></Form.Item>
                <Form.Item label="老人价格"><InputNumber v-model:value="batchEditForm.seniorPrice" class="w-full" :disabled="!batchEditForm.updatePrice" :min="0" :precision="2" prefix="¥" /></Form.Item>
                <Form.Item label="附加费用"><InputNumber v-model:value="batchEditForm.extraFee" class="w-full" :disabled="!batchEditForm.updatePrice" :min="0" :precision="2" prefix="¥" /></Form.Item>
              </div>
            </div>
            <div class="schedule-modal-footer">
              <div class="selected-tip">本次已选 {{ selectedEditTeamIds.length }} 个团期、{{ selectedEditCategorySummary }}</div>
              <Button type="primary" :loading="saving" @click="submitSelectedTeamBatchEdit">保存批量修改</Button>
            </div>
          </Form>
        </section>

      </div>
    </Modal>
    </div>
  </Page>
</template>

<style scoped>
.schedule-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.schedule-shell-card,
.schedule-list-card {
  overflow: hidden;
}

.schedule-shell-card :deep(.ant-card-body),
.schedule-list-card :deep(.ant-card-body) {
  padding: 16px;
}

.schedule-header {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
}

.schedule-title {
  margin-bottom: 2px;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.25;
  color: #1f2937;
}

.schedule-title span {
  margin-left: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #94a3b8;
}

.schedule-product-name {
  max-width: 720px;
  overflow: hidden;
  font-size: 13px;
  color: #64748b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-tabs {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-top: 12px;
  margin-top: 10px;
  overflow-x: auto;
  border-top: 1px solid #eef2f7;
}

.schedule-tabs :deep(.ant-btn) {
  flex: 0 0 auto;
}

.schedule-section-title {
  padding-bottom: 10px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 650;
  color: #1f2937;
  border-bottom: 1px solid #eef2f7;
}

.top-batch-button,
.schedule-bottom-actions :deep(.ant-btn) {
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
}

.schedule-list-card :deep(.ant-table) {
  font-size: 13px;
}

.schedule-list-card :deep(.ant-table-thead > tr > th),
.schedule-list-card :deep(.ant-table-tbody > tr > td) {
  padding: 8px 6px;
  text-align: center;
  white-space: nowrap;
}

.schedule-bottom-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #eef2f7;
}

.schedule-bottom-actions :deep(.ant-space) {
  row-gap: 8px;
}

.selected-tip {
  flex: 0 0 auto;
  font-size: 13px;
  color: #64748b;
}

.team-no {
  font-weight: 600;
  color: #1677ff;
}

.inline-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 10px 12px;
}

.inline-grid.price {
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  margin-bottom: 12px;
}

.inline-grid.selected-date {
  grid-template-columns: 220px minmax(260px, 1fr);
}

.inline-grid :deep(.ant-form-item),
.inline-grid.price :deep(.ant-form-item),
.inline-grid.selected-date :deep(.ant-form-item) {
  margin-bottom: 0;
}

.inline-edit-selected,
.inline-edit-empty {
  padding: 8px 10px;
  margin-bottom: 12px;
  font-size: 13px;
  border-radius: 6px;
}

.inline-edit-selected {
  color: #0f6c7a;
  background: #eaf8fb;
}

.inline-edit-empty {
  color: #777;
  background: #f5f7fa;
}

.field-below {
  margin-top: 8px;
}

:global(.schedule-batch-modal .ant-modal-content) {
  overflow: hidden;
  background: #fff;
  border-radius: 8px;
}

:global(.schedule-batch-modal .ant-modal-header) {
  padding: 16px 20px 12px;
  margin-bottom: 0;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
  border-bottom: 1px solid #e2e8f0;
}

:global(.schedule-batch-modal .ant-modal-title) {
  font-size: 17px;
  font-weight: 900;
  color: #0f172a;
}

:global(.schedule-batch-modal .ant-modal-close) {
  top: 14px;
  color: #475569;
}

:global(.schedule-batch-modal .ant-modal-close:hover) {
  color: #0f172a;
  background: #f1f5f9;
}

:global(.schedule-batch-modal .ant-modal-close-x) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  font-size: 15px;
}

:global(.schedule-batch-modal .ant-modal-body) {
  padding: 16px 20px 18px;
  background: #fff;
}

.schedule-batch-modal-body {
  color: #0f172a;
}

.schedule-cost-mode-tabs {
  margin-bottom: 14px;
}

.schedule-cost-mode-tabs :deep(.ant-radio-button-wrapper) {
  height: 34px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 800;
  line-height: 32px;
  color: #334155;
  background: #fff;
  border-color: #cbd5e1;
  box-shadow: none;
}

.schedule-cost-mode-tabs :deep(.ant-radio-button-wrapper:not(.ant-radio-button-wrapper-checked):hover) {
  color: #1677ff;
  border-color: #91caff;
}

.schedule-cost-mode-tabs :deep(.ant-radio-button-wrapper:not(.ant-radio-button-wrapper-checked)) {
  color: #334155;
  background: #fff;
  border-color: #cbd5e1;
}

.schedule-cost-mode-tabs :deep(.ant-radio-button-wrapper::before) {
  background-color: #cbd5e1;
}

.schedule-cost-mode-tabs :deep(.ant-radio-button-wrapper-checked) {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
}

.schedule-batch-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.schedule-batch-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.schedule-batch-form :deep(.ant-form-item-label) {
  padding-bottom: 5px;
}

.schedule-batch-form :deep(.ant-form-item-label > label) {
  height: auto;
  font-size: 12.5px;
  font-weight: 800;
  color: #334155;
}

.schedule-batch-form :deep(.ant-select-selector),
.schedule-batch-form :deep(.ant-picker),
.schedule-batch-form :deep(.ant-input-number),
.schedule-batch-form :deep(.ant-input-number-affix-wrapper),
.schedule-batch-form :deep(.ant-input) {
  width: 100%;
  min-height: 32px;
  color: #0f172a;
  background: #fff;
  border-color: #dbe4f0;
  border-radius: 5px;
}

.schedule-batch-form :deep(.ant-select-selector:hover),
.schedule-batch-form :deep(.ant-picker:hover),
.schedule-batch-form :deep(.ant-input-number:hover),
.schedule-batch-form :deep(.ant-input-number-affix-wrapper:hover),
.schedule-batch-form :deep(.ant-input:hover) {
  border-color: #91caff;
}

.schedule-batch-form :deep(.ant-input-number-focused),
.schedule-batch-form :deep(.ant-input-number-affix-wrapper-focused),
.schedule-batch-form :deep(.ant-picker-focused),
.schedule-batch-form :deep(.ant-select-focused .ant-select-selector),
.schedule-batch-form :deep(.ant-input:focus) {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}

.schedule-batch-form :deep(.ant-input-number-affix-wrapper .ant-input-number) {
  min-height: 28px;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.schedule-batch-form :deep(.ant-input-number-prefix),
.schedule-batch-form :deep(.ant-input-number-group-addon) {
  color: #64748b;
  background: #f8fafc;
  border-color: #dbe4f0;
}

.schedule-batch-form :deep(.ant-input-number-input),
.schedule-batch-form :deep(.ant-picker-input > input),
.schedule-batch-form :deep(.ant-select-selection-item) {
  color: #0f172a;
}

.schedule-batch-form :deep(.ant-select-selection-placeholder),
.schedule-batch-form :deep(.ant-picker-input > input::placeholder),
.schedule-batch-form :deep(.ant-input::placeholder) {
  color: #94a3b8;
}

.schedule-batch-form :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.schedule-batch-form :deep(.ant-checkbox + span) {
  color: #334155;
}

.schedule-batch-form :deep(.ant-checkbox-checked .ant-checkbox-inner) {
  background-color: #1677ff;
  border-color: #1677ff;
}

.schedule-field-group {
  padding: 12px;
  background: #fbfdff;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.schedule-group-title {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
}

.schedule-group-title svg {
  width: 16px;
  height: 16px;
  color: #1677ff;
}

.schedule-group-title small {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.schedule-form-row {
  display: grid;
  gap: 12px;
  align-items: end;
}

.schedule-form-row.six-columns {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.schedule-form-row.five-columns {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.schedule-modal-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
  padding-top: 14px;
  margin-top: 4px;
  border-top: 1px solid #e2e8f0;
}

.schedule-edit-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.modal-form-section {
  padding: 14px;
  background: var(--ant-color-bg-container);
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 6px;
}

.modal-section-title {
  display: flex;
  gap: 12px;
  align-items: baseline;
  padding-bottom: 10px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 650;
  color: var(--ant-color-text);
  border-bottom: 1px solid var(--ant-color-border-secondary);
}

.modal-section-title span {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
}

.batch-create-tabs {
  display: flex;
  justify-content: flex-start;
  gap: 0;
  padding-bottom: 0;
  margin-bottom: 16px;
  overflow: hidden;
  border-bottom: 1px solid var(--ant-color-border-secondary);
}

.batch-create-tabs :deep(.ant-btn) {
  min-width: 118px;
  height: 34px;
  margin-bottom: -1px;
  font-weight: 500;
  border-radius: 6px 6px 0 0;
}

.batch-create-tabs :deep(.ant-btn + .ant-btn) {
  margin-left: 6px;
}

.specific-date-transfer {
  display: grid;
  grid-template-columns: 260px 96px 260px minmax(260px, 1fr);
  gap: 12px;
  align-items: start;
}

.specific-date-panel {
  min-width: 0;
}

.specific-date-panel-title {
  height: 28px;
  padding: 5px 8px;
  font-size: 13px;
  font-weight: 800;
  line-height: 18px;
  color: #334155;
  background: #f8fafc;
  border: 1px solid #dbe4f0;
  border-bottom: 0;
  border-radius: 6px 6px 0 0;
}

.specific-date-list {
  height: 258px;
  padding: 8px;
  overflow: auto;
  background: #fff;
  border: 1px solid #dbe4f0;
  border-radius: 0 0 6px 6px;
}

.specific-date-list.selected {
  background: #fff;
}

.specific-date-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 13px;
  font-weight: 700;
  color: #94a3b8;
  border: 1px dashed #cbd5e1;
  border-radius: 4px;
}

.specific-date-list :deep(.ant-checkbox-group) {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.specific-date-list :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
  font-size: 13px;
}

.specific-date-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 92px;
}

.specific-date-actions :deep(.ant-btn) {
  min-width: 86px;
  height: 32px;
  font-size: 13px;
  font-weight: 800;
}

.specific-date-actions :deep(.ant-btn:not(.ant-btn-primary)) {
  color: #334155;
  background: #fff;
  border-color: #cbd5e1;
  box-shadow: none;
}

.specific-date-actions :deep(.ant-btn:not(.ant-btn-primary):hover) {
  color: #1677ff;
  background: #f8fbff;
  border-color: #91caff;
}

.edit-transfer-grid {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 96px minmax(260px, 1fr);
  gap: 12px;
  align-items: start;
}

.edit-transfer-grid.category-transfer {
  max-width: 720px;
}

.specific-date-list.edit-list {
  height: 196px;
}

.specific-date-list.edit-list.short {
  height: 148px;
}

.specific-date-actions.edit-actions {
  padding-top: 64px;
}

.specific-date-actions.edit-actions.short {
  padding-top: 42px;
}

.edit-check-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
  align-items: center;
}

.edit-check-field {
  display: grid;
  grid-template-columns: 128px minmax(120px, 1fr);
  gap: 8px;
  align-items: center;
}

.edit-price-hint {
  padding: 8px 10px;
  margin: 12px 0;
  font-size: 12.5px;
  font-weight: 700;
  color: #64748b;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 5px;
}

.edit-price-grid {
  margin-bottom: 0;
}

.specific-date-side-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.specific-date-side-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.specific-price-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 12px 16px;
}

.specific-price-grid :deep(.ant-form-item) {
  margin-bottom: 0;
}

.editable-number,
.editable-money {
  display: inline-flex;
  gap: 3px;
  align-items: center;
  color: #1677ff;
}

.editable-number :deep(.ant-input-number),
.editable-money :deep(.ant-input-number) {
  width: 54px;
}

.editable-number :deep(.ant-input-number-input),
.editable-money :deep(.ant-input-number-input) {
  height: 30px;
  padding-inline: 8px;
}

.modal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.modal-grid.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.modal-grid.five {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.modal-grid :deep(.ant-form-item) {
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .schedule-header,
  .schedule-bottom-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .inline-grid,
  .inline-grid.price,
  .inline-grid.selected-date,
  .specific-date-transfer,
  .edit-transfer-grid,
  .edit-check-grid,
  .specific-price-grid {
    grid-template-columns: 1fr;
  }

  .edit-check-field {
    grid-template-columns: 1fr;
  }

  .modal-grid,
  .modal-grid.three,
  .modal-grid.five {
    grid-template-columns: 1fr;
  }
}
</style>
