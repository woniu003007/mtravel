<script lang="ts" setup>
import type { UploadProps } from 'ant-design-vue';
import type { CustomerUnitApi } from '#/api/customer/unit';
import type { BookingAiImportApi } from '#/api/sales/booking-ai-import';
import type { SalesBookingApi } from '#/api/sales/booking';
import type { RegionPath } from '#/utils/region';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Card,
  Cascader,
  Checkbox,
  DatePicker,
  Descriptions,
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
  Upload,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { uploadAttachment } from '#/api/common/attachment';
import { getCustomerUnitPage } from '#/api/customer/unit';
import {
  getSalesBookingOrderDetail,
  getSalesBookingTeamDraft,
  saveSalesBookingOrder,
} from '#/api/sales/booking';
import { recognizeBookingAiImport } from '#/api/sales/booking-ai-import';
import {
  buildRegionOptions,
  buildRegionPath,
  splitRegionPath,
} from '#/utils/region';

type PriceLineRow = SalesBookingApi.PriceLine & { rowKey: string };
type GuestRow = SalesBookingApi.Guest & { rowKey: string };
type AiImportFile = { fileExt?: string; fileName: string; id: number };
type ModuleKey = 'additional' | 'customer' | 'guests' | 'guide' | 'price' | 'travel';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const aiImportOpen = ref(false);
const aiImportLoading = ref(false);
const aiImportText = ref('');
const aiImportAttachmentId = ref<number>();
const aiImportFiles = ref<AiImportFile[]>([]);
const aiImportSourceType = ref('text');
const aiImportResult = ref<BookingAiImportApi.RecognizeResult>();
const aiImportTextPlaceholder = `可粘贴确认单、微信消息、游客名单或报价文本。例如：
2026年6月25日 大连-上海 CZ6533（09:10-11:20）
2026年6月30日 上海-大连 CZ6536（19:20-21:15）
客户：示例旅行社 示例联系人 138****0001
导游：示例导游 139****0002
报价：成人 3000元/人，儿童 1999元/人，单房差 580元
序号 姓名 年龄 出生日期 身份证号 电话 分房 备注
1 示例游客A 44 1982-06-21 110000********1234 138****0001 全程家庭房 重点照顾
2 示例游客B 15 2010-10-28 110000********5678 138****0001 全程家庭房 中考生
3 示例游客C 37 1988-11-03 110000********9012 138****0001 全程家庭房
4 示例游客D 47 1978-09-10 110000********3456 139****0003 1间双标间 中考生`;
const selectedModuleKeys = ref<ModuleKey[]>([
  'travel',
  'guide',
  'customer',
  'price',
  'additional',
  'guests',
]);
const teamDraft = ref<SalesBookingApi.TeamDraft>();
const customerLoading = ref(false);
const customerKeyword = ref('');
const customerUnits = ref<CustomerUnitApi.CustomerUnit[]>([]);
const sourceRegionPath = ref<RegionPath>([]);
let customerSearchTimer: number | undefined;

const teamId = computed(() => Number(route.params.teamId || 0));
const orderId = computed(() => route.params.orderId ? Number(route.params.orderId) : undefined);
const pageTitle = computed(() => (orderId.value ? '修改收客订单' : '新增收客订单'));
const team = computed(() => teamDraft.value?.team);
const product = computed(() => teamDraft.value?.product);

const form = reactive<SalesBookingApi.SaveParams>({
  contactName: '',
  contactPhone: '',
  customerName: '',
  customerTeamNo: '',
  dropoffInfo: '',
  feeRemark: '',
  guests: [],
  guideName: '',
  guidePhone: '',
  guideRemark: '',
  hotelInfo: '',
  orderRemark: '',
  pickupInfo: '',
  pickupRemark: '',
  priceLines: [],
  receivedAmount: 0,
  sourceCity: '',
  sourceDistrict: '',
  sourceProvince: '',
  status: 'pending',
  teamId: 0,
  travelDescription: '',
});

const priceLines = ref<PriceLineRow[]>([
  createPriceLine('adult', '成人', 0, 0),
]);
const guests = ref<GuestRow[]>([
  createGuest(1),
]);
const regionOptions = buildRegionOptions();
const travelInfo = reactive({
  outboundArrivalStation: '',
  outboundArrivalTime: '',
  outboundDepartureTime: '',
  outboundNo: '',
  outboundOriginCity: '',
  returnDepartureStation: '',
  returnDepartureTime: '',
  returnDestinationCity: '',
  returnNo: '',
  returnArrivalTime: '',
});

const priceTypeOptions = [
  { label: '成人', value: 'adult' },
  { label: '儿童', value: 'child' },
  { label: '儿童[不占床]', value: 'child_no_bed' },
  { label: '老人', value: 'senior' },
  { label: '全陪', value: 'escort' },
  { label: '单房差', value: 'single_room' },
  { label: '机票', value: 'flight_ticket' },
  { label: '火车票', value: 'train_ticket' },
  { label: '车费', value: 'vehicle' },
  { label: '导服', value: 'guide_service' },
  { label: '餐费', value: 'meal' },
  { label: '房费', value: 'hotel' },
  { label: '门票', value: 'ticket' },
  { label: '自费', value: 'self_pay' },
  { label: '杂费', value: 'misc_fee' },
  { label: '迪士尼门票', value: 'disney_ticket' },
  { label: '迪士尼酒店', value: 'disney_hotel' },
  { label: '景交', value: 'scenic_transport' },
  { label: '快艇', value: 'speedboat' },
  { label: '缆车', value: 'cable_car' },
  { label: '司陪费用', value: 'driver_escort' },
  { label: '综费', value: 'comprehensive' },
  { label: '接送机', value: 'airport_transfer' },
  { label: '小费', value: 'tip' },
  { label: '附加费', value: 'surcharge' },
  { label: '酒店附加费', value: 'hotel_surcharge' },
  { label: '旺季附加费', value: 'peak_surcharge' },
  { label: '夜间附加费', value: 'night_surcharge' },
  { label: '其它', value: 'misc' },
];

const costLineTypeMap: Record<string, { itemName: string; lineType: string }> = {
  导游: { itemName: '导服', lineType: 'guide_service' },
  房: { itemName: '房费', lineType: 'hotel' },
  车: { itemName: '车费', lineType: 'vehicle' },
  门: { itemName: '门票', lineType: 'ticket' },
  餐: { itemName: '餐费', lineType: 'meal' },
};

const moduleSelection: { key: ModuleKey; label: string; scoreKey: keyof BookingAiImportApi.ModuleScores }[] = [
  { key: 'travel', label: '行程说明', scoreKey: 'travelScore' },
  { key: 'guide', label: '导游相关', scoreKey: 'guideScore' },
  { key: 'customer', label: '客户信息', scoreKey: 'customerScore' },
  { key: 'price', label: '价格信息', scoreKey: 'priceScore' },
  { key: 'additional', label: '附加说明', scoreKey: 'additionalScore' },
  { key: 'guests', label: '游客名单', scoreKey: 'guestListScore' },
];

const guestGenderOptions = [
  { label: '男', value: '男' },
  { label: '女', value: '女' },
];

const guestTypeOptions = [
  { label: '成人', value: 'adult' },
  { label: '儿童', value: 'child' },
  { label: '儿童[不占床]', value: 'child_no_bed' },
  { label: '老人', value: 'senior' },
  { label: '全陪', value: 'escort' },
];

const customerOptions = computed(() =>
  customerUnits.value.map((item) => ({
    label: item.customerCode
      ? `${item.customerName}（${item.customerCode}）`
      : item.customerName,
    value: item.id,
  })),
);

const guestCount = computed(() => {
  const priceCount = priceLines.value
    .filter((line) => ['adult', 'child', 'child_no_bed', 'senior', 'escort'].includes(line.lineType || ''))
    .reduce((sum, line) => sum + money(line.quantity), 0);
  return priceCount || guests.value.filter((guest) => guest.guestName?.trim()).length;
});
const travelDays = computed(() => {
  const departure = team.value?.departureDate;
  const end = team.value?.endDate;
  if (!departure || !end) return 1;
  const startDate = new Date(departure).getTime();
  const endDate = new Date(end).getTime();
  if (Number.isNaN(startDate) || Number.isNaN(endDate) || endDate < startDate) return 1;
  return Math.floor((endDate - startDate) / 86_400_000) + 1;
});
const metricItems = computed(() => [
  { key: 'totalSeats', label: '预控人数', value: `${team.value?.totalSeats ?? 0}人` },
  { key: 'guestCount', label: '实收人数', value: `${team.value?.usedSeats ?? 0}人`, extra: guestCount.value ? `[${guestCount.value}]` : '' },
  { key: 'remainingSeats', label: '剩余人数', value: `${team.value?.remainingSeats ?? 0}人` },
  { key: 'travelDays', label: '旅游天数', value: `${travelDays.value}天` },
  { key: 'teamNo', label: '团号', value: team.value?.teamNo || '--' },
  { key: 'departureDate', label: '出团日期', value: team.value?.departureDate || '--' },
  { key: 'distance', label: '总里程数', value: '--' },
]);
const feeChangeRows = computed(() => ((form as SalesBookingApi.Detail).feeChanges || []));
const orderAmount = computed(() => priceLines.value.reduce((sum, line) => {
  return sum + money(line.unitPrice) * money(line.quantity);
}, 0));
const feeChangeAmount = computed(() => feeChangeRows.value.reduce((sum, row) => sum + money(row.amount), 0));
const totalAmount = computed(() => orderAmount.value + feeChangeAmount.value);

function createPriceLine(lineType = 'misc', itemName = '其他费用', unitPrice = 0, quantity = 0): PriceLineRow {
  return {
    itemName,
    lineType,
    quantity,
    rowKey: `price-${Date.now()}-${Math.random()}`,
    unitPrice,
  };
}

function createAiCostPriceLine(lineType: string, itemName: string, unitPrice: number, remark: string): PriceLineRow {
  return {
    ...createPriceLine(lineType, itemName, unitPrice, 1),
    remark,
  };
}

function createGuest(indexNo: number): GuestRow {
  return {
    guestType: 'adult',
    indexNo,
    leaderFlag: false,
    rowKey: `guest-${Date.now()}-${Math.random()}`,
  };
}

function addGuest() {
  guests.value.push(createGuest(guests.value.length + 1));
}

function removeGuest(rowKey: string) {
  guests.value = guests.value
    .filter((guest) => guest.rowKey !== rowKey)
    .map((guest, index) => ({ ...guest, indexNo: index + 1 }));
}

function clearGuests() {
  guests.value = [createGuest(1)];
}

async function loadCustomerOptions(keyword = '') {
  customerLoading.value = true;
  try {
    const result = await getCustomerUnitPage({
      keyword: keyword || undefined,
      page: 1,
      pageSize: 50,
      status: 'active',
    });
    customerUnits.value = result.items;
    upsertCurrentCustomerOption();
  } finally {
    customerLoading.value = false;
  }
}

function handleCustomerDropdown(open: boolean) {
  if (open && customerUnits.value.length === 0) {
    loadCustomerOptions(customerKeyword.value);
  }
}

function handleCustomerSearch(value: string) {
  customerKeyword.value = value;
  if (customerSearchTimer) window.clearTimeout(customerSearchTimer);
  customerSearchTimer = window.setTimeout(() => {
    loadCustomerOptions(value);
  }, 400);
}

function handleCustomerSelect(value?: number) {
  if (!value) {
    form.customerId = undefined;
    return;
  }
  const customer = customerUnits.value.find((item) => item.id === value);
  if (!customer) return;
  form.customerId = customer.id;
  form.customerName = customer.customerName;
  form.contactName = form.contactName || customer.contactName || '';
  form.contactPhone = form.contactPhone || customer.contactPhone || '';
  form.sourceProvince = customer.province || '';
  form.sourceCity = customer.city || '';
  form.sourceDistrict = customer.district || '';
  sourceRegionPath.value = buildRegionPath(
    customer.province,
    customer.city,
    customer.district,
  );
}

function handleSourceRegionChange(value: unknown) {
  sourceRegionPath.value = Array.isArray(value) ? value as RegionPath : [];
  const regionFields = splitRegionPath(sourceRegionPath.value);
  form.sourceProvince = regionFields.province || '';
  form.sourceCity = regionFields.city || '';
  form.sourceDistrict = regionFields.district || '';
}

function syncSourceRegionPathFromForm() {
  sourceRegionPath.value = buildRegionPath(
    form.sourceProvince,
    form.sourceCity,
    form.sourceDistrict,
  );
}

function upsertCurrentCustomerOption() {
  if (!form.customerId || !form.customerName) return;
  const exists = customerUnits.value.some((item) => item.id === form.customerId);
  if (exists) return;
  customerUnits.value = [
    {
      city: form.sourceCity,
      contactName: form.contactName,
      contactPhone: form.contactPhone,
      creditLimit: 0,
      customerName: form.customerName,
      district: form.sourceDistrict,
      id: form.customerId,
      province: form.sourceProvince,
      status: 'active',
    },
    ...customerUnits.value,
  ];
}

function money(value?: number) {
  return Number(value || 0);
}

function formatAmount(value?: number) {
  return money(value).toFixed(2);
}

function formatDateTime(value?: string) {
  if (!value) return '--';
  return value.replace('T', ' ').slice(0, 16);
}

function lineTypeLabel(value?: string) {
  return priceTypeOptions.find((item) => item.value === value)?.label || value || '其它';
}

function updatePriceLineType(line: PriceLineRow, value: string) {
  line.lineType = value;
  line.itemName = lineTypeLabel(value);
}

function addPriceLine() {
  priceLines.value.push(createPriceLine());
}

function removePriceLine(rowKey: string) {
  priceLines.value = priceLines.value.filter((line) => line.rowKey !== rowKey);
}

const statusOptions = [
  { label: '未处理', value: 'pending', color: 'blue' },
  { label: '已确认', value: 'confirmed', color: 'green' },
  { label: '已取消', value: 'cancelled', color: 'red' },
];

function statusColor(status?: string) {
  return statusOptions.find((item) => item.value === status)?.color || 'blue';
}

function statusLabel(status?: string) {
  return statusOptions.find((item) => item.value === status)?.label || '未处理';
}

function teamPerson(value?: string) {
  return value || '--';
}

function bookingNoticeText() {
  return teamDraft.value?.content?.bookingNotice || '暂无收客须知，请以团队确认单和销售确认内容为准。';
}

function openAiImport() {
  aiImportOpen.value = true;
}

async function runAiImportRecognize() {
  if (!aiImportText.value.trim() && aiImportFiles.value.length === 0) {
    message.warning('请先上传确认单或粘贴文本');
    return;
  }
  aiImportLoading.value = true;
  try {
    aiImportResult.value = await recognizeBookingAiImport({
      attachmentIds: aiImportFiles.value.map((item) => item.id),
      sourceType: aiImportFiles.value.length === 1 ? aiImportFiles.value[0]?.fileExt : aiImportSourceType.value,
      text: aiImportText.value,
    });
    message.success('识别完成，请人工确认后填入表单');
  } finally {
    aiImportLoading.value = false;
  }
}

const beforeUploadAiImportFile: UploadProps['beforeUpload'] = async (file) => {
  const data = new FormData();
  data.append('file', file as File);
  data.append('businessModule', '销售收客');
  data.append('businessType', 'AI辅助录入确认单');
  try {
    const attachment = await uploadAttachment(data);
    aiImportAttachmentId.value = attachment.id;
    aiImportFiles.value = [
      ...aiImportFiles.value,
      {
        fileExt: attachment.fileExt || fileExt(file.name),
        fileName: attachment.originalFilename || file.name,
        id: attachment.id,
      },
    ];
    aiImportSourceType.value = fileExt(file.name);
    message.success('文件已上传，可以开始识别');
  } catch {
    message.error('文件上传失败');
  }
  return false;
};

function removeAiImportFile(id: number) {
  aiImportFiles.value = aiImportFiles.value.filter((item) => item.id !== id);
  if (aiImportAttachmentId.value === id) {
    aiImportAttachmentId.value = aiImportFiles.value.at(-1)?.id;
  }
}

function moduleScore(scoreKey: keyof BookingAiImportApi.ModuleScores) {
  const score = aiImportResult.value?.moduleScores?.[scoreKey] ?? 0;
  return `${Math.round(score * 100)}%`;
}

function toggleAiModule(key: ModuleKey) {
  const current = new Set(selectedModuleKeys.value);
  if (current.has(key)) {
    current.delete(key);
  } else {
    current.add(key);
  }
  selectedModuleKeys.value = moduleSelection
    .map((item) => item.key)
    .filter((item) => current.has(item));
}

function fileExt(fileName?: string) {
  const value = fileName || '';
  const index = value.lastIndexOf('.');
  return index >= 0 ? value.slice(index + 1).toLowerCase() : 'text';
}

function fillAiDraftToForm() {
  const result = aiImportResult.value;
  if (!result) return;
  const selected = new Set(selectedModuleKeys.value);
  if (selected.has('travel')) fillAiTravel(result);
  if (selected.has('guide')) fillAiGuide(result);
  if (selected.has('customer')) fillAiCustomer(result);
  if (selected.has('additional')) fillAiAdditional(result);
  const parsedGuests = selected.has('guests') ? fillAiGuests(result) : [];
  if (selected.has('price')) fillAiPrice(result, parsedGuests);
  aiImportOpen.value = false;
  message.success('AI识别结果已按所选模块填入空字段，请人工核对后保存');
}

function fillBlank<T extends Record<string, any>, K extends keyof T>(target: T, key: K, value?: T[K]) {
  if ((target[key] === undefined || target[key] === null || target[key] === '') && value) {
    target[key] = value;
  }
}

function fillAiTravel(result: BookingAiImportApi.RecognizeResult) {
  fillBlank(form, 'travelDescription', [
    result.travelInfo?.joinDate ? `参团时间：${result.travelInfo.joinDate}` : '',
    result.travelInfo?.outboundTrafficNo ? `来程：${result.travelInfo.outboundOriginCity || ''}-${result.travelInfo.outboundArrivalCity || ''} ${result.travelInfo.outboundTrafficNo} ${result.travelInfo.outboundDepartureTime || ''}-${result.travelInfo.outboundArrivalTime || ''}` : '',
    result.travelInfo?.returnTrafficNo ? `返程：${result.travelInfo.returnDepartureCity || ''}-${result.travelInfo.returnDestinationCity || ''} ${result.travelInfo.returnTrafficNo} ${result.travelInfo.returnDepartureTime || ''}-${result.travelInfo.returnArrivalTime || ''}` : '',
  ].filter(Boolean).join('\n'));
  fillBlank(travelInfo, 'outboundOriginCity', result.travelInfo?.outboundOriginCity);
  fillBlank(travelInfo, 'outboundArrivalStation', result.travelInfo?.outboundStationName);
  fillBlank(travelInfo, 'outboundNo', result.travelInfo?.outboundTrafficNo);
  fillBlank(travelInfo, 'outboundDepartureTime', normalizeDateTimeForPicker(result.travelInfo?.outboundDepartureTime, team.value?.departureDate));
  fillBlank(travelInfo, 'outboundArrivalTime', normalizeDateTimeForPicker(result.travelInfo?.outboundArrivalTime, team.value?.departureDate));
  fillBlank(travelInfo, 'returnDepartureStation', result.travelInfo?.returnStationName);
  fillBlank(travelInfo, 'returnDestinationCity', result.travelInfo?.returnDestinationCity);
  fillBlank(travelInfo, 'returnNo', result.travelInfo?.returnTrafficNo);
  fillBlank(travelInfo, 'returnDepartureTime', normalizeDateTimeForPicker(result.travelInfo?.returnDepartureTime, team.value?.endDate || team.value?.departureDate));
  fillBlank(travelInfo, 'returnArrivalTime', normalizeDateTimeForPicker(result.travelInfo?.returnArrivalTime, team.value?.endDate || team.value?.departureDate));
}

function fillAiGuide(result: BookingAiImportApi.RecognizeResult) {
  fillBlank(form, 'guideName', result.guideInfo?.guideName);
  fillBlank(form, 'guidePhone', result.guideInfo?.guidePhone);
  fillBlank(form, 'guideRemark', result.guideInfo?.receptionRequirement);
}

function fillAiCustomer(result: BookingAiImportApi.RecognizeResult) {
  fillBlank(form, 'customerName', result.customerInfo?.customerName);
  fillBlank(form, 'contactName', result.customerInfo?.contactName);
  fillBlank(form, 'contactPhone', result.customerInfo?.contactPhone);
}

function fillAiAdditional(result: BookingAiImportApi.RecognizeResult) {
  fillBlank(form, 'orderRemark', [result.additionalInfo?.notes, result.additionalInfo?.leaderNote].filter(Boolean).join('\n'));
  fillBlank(form, 'hotelInfo', result.additionalInfo?.roomingNote);
}

function fillAiGuests(result: BookingAiImportApi.RecognizeResult) {
  const parsedGuests = (result.guests || []).map((guest, index) => ({
    age: guest.age,
    birthDate: guest.birthDate,
    certificateNo: guest.certificateNo,
    englishName: guest.englishName,
    gender: guest.gender,
    guestName: guest.name,
    guestType: mapAiGuestType(guest.customerType),
    idCardValid: guest.idCardValid,
    idCardWarning: guest.warnings?.join('；'),
    indexNo: guest.indexNo || index + 1,
    leaderFlag: Boolean(guest.leader || guest.suspectedLeader),
    phone: guest.phone,
    remark: [guest.groupRemark, guest.personalRemark, guest.leaderSourceText].filter(Boolean).join('；'),
    roomGroup: guest.roomGroup,
    roomRemark: guest.roomingRemark,
    rowKey: `guest-ai-${index}-${Date.now()}`,
  }));
  if (!parsedGuests.length) return [];
  const hasExistingGuest = guests.value.some((guest) => guest.guestName?.trim() || guest.certificateNo?.trim());
  guests.value = hasExistingGuest
    ? [...guests.value, ...parsedGuests]
    : parsedGuests;
  return parsedGuests;
}

function fillAiPrice(result: BookingAiImportApi.RecognizeResult, parsedGuests: GuestRow[]) {
  const priceRows: PriceLineRow[] = [];
  const adultPrice = parseAmount(result.priceInfo?.adultPrice);
  const childPrice = parseAmount(result.priceInfo?.childPrice);
  const seniorPrice = parseAmount(result.priceInfo?.seniorPrice);
  if (adultPrice) priceRows.push(createPriceLine('adult', '成人', adultPrice, parsedGuests.filter((guest) => guest.guestType === 'adult').length || 1));
  if (childPrice) priceRows.push(createPriceLine('child', '儿童', childPrice, parsedGuests.filter((guest) => guest.guestType === 'child').length || 1));
  if (seniorPrice) priceRows.push(createPriceLine('senior', '老人', seniorPrice, parsedGuests.filter((guest) => guest.guestType === 'senior').length || 1));
  priceRows.push(...buildAiCostPriceRows(result.priceInfo));
  const hasExistingPrice = priceLines.value.some((line) => money(line.unitPrice) > 0 || money(line.quantity) > 0);
  if (priceRows.length && !hasExistingPrice) {
    priceLines.value = priceRows;
  }
  const feeRemarkDraft = buildAiFeeRemark(result.priceInfo);
  fillBlank(form, 'feeRemark', feeRemarkDraft);
}

function buildAiCostPriceRows(priceInfo?: BookingAiImportApi.PriceInfo) {
  return (priceInfo?.priceLines || [])
    .map((line) => {
      const matched = line.match(/^(房|车|门|餐|导游)：(.+?)；金额：(\d+(?:\.\d+)?)元/);
      if (!matched) return null;
      const [, costType = '', remark = '', amount = ''] = matched;
      const config = costLineTypeMap[costType];
      if (!config) return null;
      return createAiCostPriceLine(config.lineType, config.itemName, Number(amount), remark.trim());
    })
    .filter((line): line is PriceLineRow => Boolean(line));
}

function buildAiFeeRemark(priceInfo?: BookingAiImportApi.PriceInfo) {
  const lines = priceInfo?.priceLines?.filter(Boolean) || [];
  if (!lines.length && !priceInfo?.totalAmount) return '';
  return [
    'AI识别费用说明，保存前请人工核对：',
    ...lines,
    priceInfo?.totalAmount && !lines.some((line) => line.includes('总计'))
      ? `总计：${priceInfo.totalAmount}元`
      : '',
  ].filter(Boolean).join('\n');
}

function parseAmount(value?: string) {
  if (!value) return 0;
  const matched = value.replaceAll(',', '').match(/\d+(?:\.\d+)?/);
  return matched ? Number(matched[0]) : 0;
}

function mapAiGuestType(value?: string) {
  if (value?.includes('不占')) return 'child_no_bed';
  if (value?.includes('儿童') || value?.includes('小孩')) return 'child';
  if (value?.includes('老人')) return 'senior';
  if (value?.includes('全陪')) return 'escort';
  return 'adult';
}

/**
 * 老系统收客页的出发/抵达字段是日期时间控件。
 * AI 可能只识别出“09:10”这类时间，保存到 DatePicker 前需要补上团队日期。
 */
function normalizeDateTimeForPicker(value?: string, fallbackDate?: string) {
  const rawValue = value?.trim();
  if (!rawValue) return '';
  const normalized = rawValue.replace('T', ' ').replaceAll('-', '/');
  if (/^\d{4}\/\d{2}\/\d{2}\s+\d{2}:\d{2}/.test(normalized)) {
    return normalized.slice(0, 16);
  }
  const timeMatch = normalized.match(/(\d{1,2}):(\d{2})/);
  if (!timeMatch || !fallbackDate) return rawValue;
  const date = fallbackDate.replaceAll('-', '/').slice(0, 10);
  const [, hour = '', minute = ''] = timeMatch;
  return `${date} ${hour.padStart(2, '0')}:${minute}`;
}

async function saveOrder() {
  if (!teamId.value) {
    message.warning('缺少团队ID');
    return;
  }
  saving.value = true;
  try {
    const sourceRegionFields = splitRegionPath(sourceRegionPath.value);
    const response = await saveSalesBookingOrder({
      ...form,
      dropoffInfo: composeReturnInfo(),
      guests: guests.value.map(({ rowKey, ...guest }) => ({
        ...guest,
        leaderFlag: Boolean(guest.leaderFlag),
      })),
      pickupInfo: composeOutboundInfo(),
      priceLines: priceLines.value.map(({ rowKey, ...line }) => line),
      receivedAmount: money(form.receivedAmount),
      sourceCity: sourceRegionFields.city || '',
      sourceDistrict: sourceRegionFields.district || '',
      sourceProvince: sourceRegionFields.province || '',
      teamId: teamId.value,
    });
    message.success('收客订单已保存');
    router.replace(`/sales/team/booking/${teamId.value}/${response.id}`);
  } finally {
    saving.value = false;
  }
}

function goBack() {
  router.push(`/sales/team/operation/${teamId.value}`);
}

function showPendingFeature(label: string) {
  message.info(`${label}后续接入`);
}

function composeOutboundInfo() {
  const values = [
    travelInfo.outboundOriginCity,
    travelInfo.outboundArrivalStation,
    travelInfo.outboundNo,
    travelInfo.outboundDepartureTime,
    travelInfo.outboundArrivalTime,
  ].filter(Boolean);
  return values.length ? values.join(' / ') : form.pickupInfo;
}

function composeReturnInfo() {
  const values = [
    travelInfo.returnDepartureStation,
    travelInfo.returnDestinationCity,
    travelInfo.returnNo,
    travelInfo.returnDepartureTime,
    travelInfo.returnArrivalTime,
  ].filter(Boolean);
  return values.length ? values.join(' / ') : form.dropoffInfo;
}

function applyStoredTrafficInfo() {
  if (form.pickupInfo && !travelInfo.outboundNo) {
    travelInfo.outboundNo = form.pickupInfo;
  }
  if (form.dropoffInfo && !travelInfo.returnNo) {
    travelInfo.returnNo = form.dropoffInfo;
  }
}

async function loadPage() {
  if (!teamId.value) {
    message.warning('缺少团队ID');
    return;
  }
  loading.value = true;
  try {
    teamDraft.value = await getSalesBookingTeamDraft(teamId.value);
    form.teamId = teamId.value;
    if (orderId.value) {
      const detail = await getSalesBookingOrderDetail(orderId.value);
      Object.assign(form, detail);
      priceLines.value = (detail.priceLines || []).map((line, index) => ({
        ...line,
        rowKey: `price-${line.id || index}`,
      }));
      guests.value = (detail.guests || []).map((guest, index) => ({
        ...guest,
        rowKey: `guest-${guest.id || index}`,
      }));
      syncSourceRegionPathFromForm();
      upsertCurrentCustomerOption();
      applyStoredTrafficInfo();
    } else {
      form.travelDescription = teamDraft.value.content?.productDescription || '';
      syncSourceRegionPathFromForm();
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadPage();
});
</script>

<template>
  <Page :title="pageTitle">
    <Spin :spinning="loading">
      <div class="old-system-booking-form">
        <Card class="team-operation-shell" :bordered="false">
          <div class="team-operation-header">
            <div>
              <div class="operation-title">订单处理</div>
              <div class="operation-subtitle">收客订单录入：客户信息、价格、名单和 AI 辅助录入集中处理。</div>
            </div>
            <div class="top-tool-actions">
              <Button type="primary" @click="openAiImport">
                <IconifyIcon icon="lucide:sparkles" />
                AI辅助录入
              </Button>
            </div>
          </div>

          <div class="operation-flow-row" aria-label="团队阶段">
            <template v-for="(stage, index) in ['收客', '排团', '发团', '结算', '完成']" :key="stage">
              <div class="stage-flow-item" :class="{ active: index === 0, pending: index > 0 }">
                <span class="stage-index">{{ index + 1 }}</span>
                <span class="stage-label">{{ stage }}</span>
              </div>
              <IconifyIcon v-if="index < 4" icon="lucide:chevrons-right" class="stage-arrow" />
            </template>
          </div>

          <div class="team-profile-block">
            <div class="team-name">{{ product?.productName || '收客订单' }}</div>
            <div class="team-badges">
              <Tag color="blue">{{ team?.teamTypeLabel || '团队' }}</Tag>
              <Tag color="orange">业务类型：{{ product?.businessType || '--' }}</Tag>
              <Tag color="geekblue">部门：{{ teamPerson(team?.departmentName) }}</Tag>
              <Tag color="blue">操作计调：{{ teamPerson(team?.operatorEmployeeName) }}</Tag>
              <Tag color="green">导游：{{ form.guideName || team?.guideSummary || '--' }}</Tag>
              <Tag>领队：{{ teamPerson(team?.leaderSummary) }}</Tag>
              <Tag>全陪：{{ teamPerson(team?.escortEmployeeName || team?.escortSummary) }}</Tag>
              <Tag :color="statusColor(form.status)">订单状态：{{ statusLabel(form.status) }}</Tag>
            </div>

            <div class="team-metric-panel">
              <div class="team-metric-strip">
                <span
                  v-for="item in metricItems"
                  :key="item.key"
                  class="team-metric-item"
                  :class="`metric-${item.key}`"
                >
                  <em>{{ item.label }}</em>
                  <strong>
                    {{ item.value }}
                    <small v-if="item.extra">{{ item.extra }}</small>
                  </strong>
                </span>
              </div>
              <div class="old-system-view-actions">
                <button type="button" class="view-action-tile">
                  <IconifyIcon icon="lucide:tag" />
                  <span>查看价格</span>
                </button>
                <button type="button" class="view-action-tile">
                  <IconifyIcon icon="lucide:briefcase-business" />
                  <span>查看行程</span>
                </button>
              </div>
            </div>
          </div>
        </Card>

        <div class="booking-agent-panel">
          <section class="booking-row-section">
            <div class="section-side-label">行程说明</div>
            <div class="section-main">
              <div class="traffic-form">
                <div class="traffic-line join-date-line">
                  <span class="inline-label">参团时间</span>
                  <DatePicker :value="team?.departureDate" class="full-input" disabled value-format="YYYY-MM-DD" />
                </div>
                <div class="traffic-line">
                  <span class="inline-label">来程信息</span>
                  <Input v-model:value="travelInfo.outboundOriginCity" placeholder="=出发城市=" />
                  <Input v-model:value="travelInfo.outboundArrivalStation" placeholder="=接送站=" />
                  <Input v-model:value="travelInfo.outboundNo" placeholder="航班号/车次" />
                  <DatePicker
                    v-model:value="travelInfo.outboundDepartureTime"
                    class="full-input"
                    format="YYYY/MM/DD HH:mm"
                    placeholder="出发时间"
                    show-time
                    value-format="YYYY/MM/DD HH:mm"
                  />
                  <DatePicker
                    v-model:value="travelInfo.outboundArrivalTime"
                    class="full-input"
                    format="YYYY/MM/DD HH:mm"
                    placeholder="抵达时间"
                    show-time
                    value-format="YYYY/MM/DD HH:mm"
                  />
                </div>
                <div class="traffic-line">
                  <span class="inline-label">返程信息</span>
                  <Input v-model:value="travelInfo.returnDepartureStation" placeholder="=接送站=" />
                  <Input v-model:value="travelInfo.returnDestinationCity" placeholder="=返回城市=" />
                  <Input v-model:value="travelInfo.returnNo" placeholder="航班号/车次" />
                  <DatePicker
                    v-model:value="travelInfo.returnDepartureTime"
                    class="full-input"
                    format="YYYY/MM/DD HH:mm"
                    placeholder="出发时间"
                    show-time
                    value-format="YYYY/MM/DD HH:mm"
                  />
                  <DatePicker
                    v-model:value="travelInfo.returnArrivalTime"
                    class="full-input"
                    format="YYYY/MM/DD HH:mm"
                    placeholder="抵达时间"
                    show-time
                    value-format="YYYY/MM/DD HH:mm"
                  />
                </div>
              </div>
            </div>
          </section>

          <section class="booking-row-section">
            <div class="section-side-label">导游相关</div>
            <div class="section-main">
              <div class="form-grid two compact-grid">
                <Form.Item label="接机标识">
                  <Input v-model:value="form.pickupRemark" placeholder="接机牌、接待暗号等" />
                </Form.Item>
                <Form.Item class="span-full" label="导游备注">
                  <Textarea v-model:value="form.guideRemark" :rows="3" />
                </Form.Item>
              </div>
            </div>
          </section>

          <section class="booking-row-section">
            <div class="section-side-label">客户信息</div>
            <div class="section-main">
              <div class="customer-line">
                <span class="inline-label">客户单位</span>
                <Select
                  v-model:value="form.customerId"
                  allow-clear
                  :filter-option="false"
                  :loading="customerLoading"
                  :options="customerOptions"
                  placeholder="=选择客户单位="
                  show-search
                  @change="(value) => handleCustomerSelect(value ? Number(value) : undefined)"
                  @dropdown-visible-change="handleCustomerDropdown"
                  @search="handleCustomerSearch"
                />
                <Button type="link" class="inline-link-button" @click="showPendingFeature('添加客户单位')">添加客户单位</Button>
              </div>
              <div class="form-grid three compact-grid">
                <Form.Item label="联系人">
                  <Input v-model:value="form.contactName" />
                </Form.Item>
                <Form.Item label="联系电话">
                  <Input v-model:value="form.contactPhone" />
                </Form.Item>
                <Form.Item label="客户团号">
                  <Input v-model:value="form.customerTeamNo" />
                </Form.Item>
                <Form.Item label="业务员">
                  <Input :value="team?.operatorEmployeeName || '--'" disabled />
                </Form.Item>
                <Form.Item label="收客计调">
                  <Input :value="team?.operatorEmployeeName || '--'" disabled />
                </Form.Item>
                <Form.Item label="客源地">
                  <Cascader
                    :value="sourceRegionPath"
                    allow-clear
                    change-on-select
                    class="full-input"
                    :options="regionOptions"
                    placeholder="可选择省 / 市 / 区县"
                    show-search
                    @change="handleSourceRegionChange"
                  />
                </Form.Item>
              </div>
            </div>
          </section>

          <section class="booking-row-section">
            <div class="section-side-label">价格信息</div>
            <div class="section-main">
              <div class="price-line-editor">
                <div v-for="line in priceLines" :key="line.rowKey" class="price-line-row">
                  <Select
                    :value="line.lineType"
                    :options="priceTypeOptions"
                    class="price-type-select"
                    @change="(value) => updatePriceLineType(line, String(value))"
                  />
                  <span class="money-symbol">¥</span>
                  <InputNumber v-model:value="line.unitPrice" :min="0" class="price-number" />
                  <span class="multiply-symbol">＊</span>
                  <span class="quantity-label">数量：</span>
                  <InputNumber v-model:value="line.quantity" :min="0" class="quantity-number" />
                  <span class="remark-label">备注：</span>
                  <Input v-model:value="line.remark" class="price-remark-input" />
                  <label class="occupy-checkbox">
                    <input type="checkbox" />
                    占位
                  </label>
                  <Button type="link" size="small" @click="addPriceLine">增加</Button>
                  <Button v-if="priceLines.length > 1" type="link" danger size="small" @click="removePriceLine(line.rowKey)">删除</Button>
                </div>
                <div class="price-tools">
                  <Button type="link" @click="showPendingFeature('从分项报价获取价格')">从分项报价获取价格</Button>
                  <Button type="link" @click="showPendingFeature('转到分项报价')">转到分项报价</Button>
                </div>
                <div class="booking-notice">
                  <span>收客须知：</span>
                  <p>{{ bookingNoticeText() }}</p>
                </div>
              </div>
            </div>
          </section>

          <section class="booking-row-section">
            <div class="section-side-label">附加说明</div>
            <div class="section-main">
              <div class="form-grid three compact-grid">
                <Form.Item label="费用说明">
                  <Textarea v-model:value="form.feeRemark" :rows="3" />
                </Form.Item>
                <Form.Item label="确认说明">
                  <Textarea v-model:value="form.confirmRemark" :rows="3" />
                </Form.Item>
                <Form.Item label="订单备注">
                  <Textarea v-model:value="form.orderRemark" :rows="3" />
                </Form.Item>
              </div>
            </div>
          </section>

          <section class="booking-row-section">
            <div class="section-side-label">费用变更记录</div>
            <div class="section-main">
              <div class="booking-subsection-toolbar">
                <strong>费用变更记录</strong>
                <Button type="primary" size="small" @click="showPendingFeature('添加变更费用')">
                  <IconifyIcon icon="lucide:plus-circle" />
                  添加变更费用
                </Button>
              </div>
              <Table
                :data-source="feeChangeRows"
                :pagination="false"
                row-key="id"
                size="small"
                class="fee-change-table"
                :locale="{ emptyText: '暂无费用变更记录' }"
              >
                <Table.Column title="序号" width="58">
                  <template #default="{ index }">{{ index + 1 }}</template>
                </Table.Column>
                <Table.Column data-index="customerName" title="客户名称" />
                <Table.Column data-index="feeDescription" title="费用说明" />
                <Table.Column title="金额" width="110">
                  <template #default="{ record }">¥{{ formatAmount(record.amount) }}</template>
                </Table.Column>
                <Table.Column data-index="registeredBy" title="登记人" width="110" />
                <Table.Column title="登记时间" width="160">
                  <template #default="{ record }">{{ formatDateTime(record.registeredAt) }}</template>
                </Table.Column>
                <Table.Column title="操作" width="90">
                  <template #default>--</template>
                </Table.Column>
              </Table>
              <div class="fee-total-line">
                费用合计：（订单金额 ¥{{ formatAmount(orderAmount) }}） + （变更费用 ¥{{ formatAmount(feeChangeAmount) }}） =
                <strong>¥{{ formatAmount(totalAmount) }}</strong> 元
              </div>
            </div>
          </section>

          <section class="booking-row-section">
            <div class="section-side-label">游客名单</div>
            <div class="section-main">
              <div class="booking-subsection-toolbar">
                <strong>游客名单</strong>
                <Space class="guest-list-actions" wrap>
                  <Button size="small" @click="showPendingFeature('查看电子合同')">查看电子合同</Button>
                  <Button size="small" @click="showPendingFeature('创建电子合同')">创建电子合同</Button>
                  <Button size="small" @click="addGuest">
                    <IconifyIcon icon="lucide:plus-circle" />
                    增加名单
                  </Button>
                  <Button size="small" @click="showPendingFeature('导入名单')">
                    <IconifyIcon icon="lucide:download" />
                    导入名单
                  </Button>
                  <Button size="small" @click="showPendingFeature('导出名单')">
                    <IconifyIcon icon="lucide:upload" />
                    导出名单
                  </Button>
                  <Button size="small" danger @click="clearGuests">清除名单</Button>
                </Space>
              </div>
              <div class="guest-table-scroll">
                <div class="guest-edit-table">
                  <table>
                    <colgroup>
                      <col class="guest-select-cell" />
                      <col class="guest-index-cell" />
                      <col class="guest-name-column" />
                      <col class="guest-room-group-column" />
                      <col class="guest-id-column" />
                      <col class="guest-gender-column" />
                      <col class="guest-birth-column" />
                      <col class="guest-type-column" />
                      <col class="guest-age-column" />
                      <col class="guest-phone-column" />
                      <col class="guest-remark-column" />
                      <col class="guest-leader-cell" />
                      <col class="guest-room-remark-column" />
                      <col class="guest-delete-cell" />
                    </colgroup>
                    <thead>
                      <tr>
                        <th class="guest-select-cell"><Checkbox /></th>
                        <th class="guest-index-cell">序号</th>
                        <th class="guest-name-column">客人姓名</th>
                        <th class="guest-room-group-column">房间组号</th>
                        <th class="guest-id-column">证件号</th>
                        <th class="guest-gender-column">性别</th>
                        <th class="guest-birth-column">出生年月</th>
                        <th class="guest-type-column">客户类型</th>
                        <th class="guest-age-column">年龄</th>
                        <th class="guest-phone-column">联系电话</th>
                        <th class="guest-remark-column">单人备注</th>
                        <th class="guest-leader-cell">领队</th>
                        <th class="guest-room-remark-column">分房备注</th>
                        <th>删除</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="guest in guests" :key="guest.rowKey" :class="{ 'guest-id-warning-row': guest.idCardValid === false }">
                        <td class="guest-select-cell"><Checkbox /></td>
                        <td class="guest-index-cell">{{ guest.indexNo }}</td>
                        <td class="guest-name-column">
                          <Input v-model:value="guest.guestName" placeholder="姓名" />
                        </td>
                        <td class="guest-room-group-column">
                          <Input v-model:value="guest.roomGroup" placeholder="房间组号" />
                        </td>
                        <td class="guest-id-column">
                          <Input v-model:value="guest.certificateNo" :class="{ 'id-card-error': guest.idCardValid === false }" placeholder="证件号" />
                          <span v-if="guest.idCardWarning" class="id-warning">{{ guest.idCardWarning }}</span>
                        </td>
                        <td class="guest-gender-column">
                          <Select v-model:value="guest.gender" :options="guestGenderOptions" class="full-input" />
                        </td>
                        <td class="guest-birth-column">
                          <DatePicker v-model:value="guest.birthDate" class="full-input" value-format="YYYY-MM-DD" />
                        </td>
                        <td class="guest-type-column">
                          <Select v-model:value="guest.guestType" :options="guestTypeOptions" class="full-input" />
                        </td>
                        <td class="guest-age-column">
                          <InputNumber v-model:value="guest.age" :min="0" class="full-input" />
                        </td>
                        <td class="guest-phone-column">
                          <Input v-model:value="guest.phone" placeholder="联系电话" />
                        </td>
                        <td class="guest-remark-column">
                          <Input v-model:value="guest.remark" placeholder="单人备注" />
                        </td>
                        <td class="guest-leader-cell">
                          <Checkbox v-model:checked="guest.leaderFlag" />
                        </td>
                        <td class="guest-room-remark-column">
                          <Input v-model:value="guest.roomRemark" placeholder="分房备注" />
                        </td>
                        <td class="guest-delete-cell">
                          <Button type="link" danger size="small" @click="removeGuest(guest.rowKey)">×</Button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </section>
        </div>

        <div class="booking-footer">
          <Button type="primary" :loading="saving" @click="saveOrder">提交订单</Button>
          <Button @click="goBack">返回团队</Button>
        </div>
      </div>
    </Spin>

    <Modal
      v-model:open="aiImportOpen"
      title="AI辅助录入确认单"
      width="1180px"
      destroy-on-close
      :footer="null"
      wrap-class-name="ai-import-modal-wrap"
    >
      <div class="ai-import-workbench">
        <Alert
          show-icon
          type="info"
          message="支持 Word、Excel、PDF、图片和文本确认单；识别结果只作为草稿，填入后仍需人工核对。"
        />

        <div class="ai-workbench-grid">
          <Card size="small" title="确认单来源" class="ai-source-card">
            <div class="ai-upload-zone">
              <Upload :show-upload-list="false" :before-upload="beforeUploadAiImportFile" multiple>
                <Button type="primary">
                  <IconifyIcon icon="lucide:upload-cloud" />
                  上传确认单
                </Button>
              </Upload>
              <span>可上传 Word / Excel / PDF / 图片，也可以直接粘贴微信文本。</span>
            </div>

            <div v-if="aiImportFiles.length" class="ai-file-list">
              <div v-for="file in aiImportFiles" :key="file.id" class="ai-file-item">
                <IconifyIcon icon="lucide:file-text" />
                <span class="ai-file-name">{{ file.fileName }}</span>
                <Tag>{{ file.fileExt || 'file' }}</Tag>
                <Button type="link" size="small" danger @click="removeAiImportFile(file.id)">移除</Button>
              </div>
            </div>

            <Textarea
              v-model:value="aiImportText"
              :rows="9"
              :placeholder="aiImportTextPlaceholder"
            />

            <div class="ai-actions">
              <Space>
                <Button type="primary" :loading="aiImportLoading" @click="runAiImportRecognize">
                  开始识别
                </Button>
                <Button :disabled="!aiImportResult" :loading="aiImportLoading" @click="runAiImportRecognize">
                  重新识别
                </Button>
              </Space>
              <span v-if="aiImportAttachmentId">最近附件ID：{{ aiImportAttachmentId }}，类型：{{ aiImportSourceType }}</span>
            </div>
          </Card>

          <Card size="small" title="识别结果确认" class="ai-result-card">
            <template v-if="aiImportResult">
              <div class="ai-summary-strip">
                <div>
                  <em>识别游客数</em>
                  <strong>{{ aiImportResult.guestSummary?.guestCount ?? aiImportResult.guests?.length ?? 0 }}</strong>
                </div>
                <div>
                  <em>证件异常</em>
                  <strong>{{ aiImportResult.guestSummary?.invalidIdCardCount ?? 0 }}</strong>
                </div>
                <div>
                  <em>字段缺失</em>
                  <strong>{{ aiImportResult.guestSummary?.missingRequiredCount ?? 0 }}</strong>
                </div>
                <div>
                  <em>疑似漏识别</em>
                  <strong>{{ aiImportResult.guestSummary?.suspectedMissingCount ?? 0 }}</strong>
                </div>
              </div>

              <div class="ai-module-score-grid">
                <label v-for="module in moduleSelection" :key="module.key" class="ai-module-score-card">
                  <Checkbox :checked="selectedModuleKeys.includes(module.key)" @change="toggleAiModule(module.key)" />
                  <span>{{ module.label }}</span>
                  <Tag color="blue">{{ moduleScore(module.scoreKey) }}</Tag>
                </label>
              </div>

              <Descriptions bordered size="small" :column="2" class="ai-result-descriptions">
                <Descriptions.Item label="客户">{{ aiImportResult.customerInfo.customerName || '--' }}</Descriptions.Item>
                <Descriptions.Item label="联系人">{{ aiImportResult.customerInfo.contactName || '--' }}</Descriptions.Item>
                <Descriptions.Item label="导游">{{ aiImportResult.guideInfo.guideName || '--' }}</Descriptions.Item>
                <Descriptions.Item label="导游电话">{{ aiImportResult.guideInfo.guidePhone || '--' }}</Descriptions.Item>
                <Descriptions.Item label="成人价">{{ aiImportResult.priceInfo.adultPrice || '--' }}</Descriptions.Item>
                <Descriptions.Item label="儿童价">{{ aiImportResult.priceInfo.childPrice || '--' }}</Descriptions.Item>
                <Descriptions.Item label="费用说明" :span="2">
                  <div v-if="aiImportResult.priceInfo.priceLines?.length" class="ai-price-lines">
                    <p v-for="line in aiImportResult.priceInfo.priceLines" :key="line">{{ line }}</p>
                  </div>
                  <span v-else>--</span>
                </Descriptions.Item>
              </Descriptions>

              <Table
                class="ai-guest-table"
                :columns="[
                  { title: '姓名', dataIndex: 'name' },
                  { title: '证件号', dataIndex: 'certificateNo' },
                  { title: '电话', dataIndex: 'phone' },
                  { title: '房间组号', dataIndex: 'roomGroup' },
                  { title: '分房备注', dataIndex: 'roomingRemark' },
                  { title: '领队', dataIndex: 'leader' },
                  { title: '身份证校验', dataIndex: 'idCardValid' },
                  { title: '备注', dataIndex: 'warnings' },
                ]"
                :data-source="aiImportResult.guests"
                :pagination="false"
                size="small"
                row-key="indexNo"
                :scroll="{ y: 240 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'leader'">
                    <Tag v-if="record.leader || record.suspectedLeader" color="green">领队</Tag>
                    <span v-else>--</span>
                  </template>
                  <template v-else-if="column.dataIndex === 'idCardValid'">
                    <Tag :color="record.idCardValid ? 'green' : 'red'">
                      {{ record.idCardValid ? '通过' : '异常' }}
                    </Tag>
                  </template>
                  <template v-else-if="column.dataIndex === 'warnings'">
                    {{ record.warnings?.join('；') || record.personalRemark || '--' }}
                  </template>
                </template>
              </Table>

              <div v-if="aiImportResult.warnings?.length || aiImportResult.evidence?.length" class="ai-evidence-box">
                <div v-if="aiImportResult.warnings?.length">
                  <strong>识别提醒</strong>
                  <p>{{ aiImportResult.warnings.join('；') }}</p>
                </div>
                <div v-if="aiImportResult.evidence?.length">
                  <strong>识别依据</strong>
                  <p>{{ aiImportResult.evidence.slice(0, 4).join('；') }}</p>
                </div>
              </div>
            </template>

            <div v-else class="ai-empty-preview">
              <IconifyIcon icon="lucide:scan-text" />
              <strong>上传或粘贴确认单后开始识别</strong>
              <span>系统会先生成草稿，再由人工选择模块填入表单。</span>
            </div>
          </Card>
        </div>

        <div class="ai-modal-footer">
          <div class="ai-module-checkboxes">
            <span>填入模块：</span>
            <Checkbox.Group v-model:value="selectedModuleKeys">
              <Checkbox v-for="module in moduleSelection" :key="module.key" :value="module.key">
                {{ module.label }}
              </Checkbox>
            </Checkbox.Group>
            <Tag color="green">只填空字段</Tag>
          </div>
          <Space>
            <Button @click="aiImportOpen = false">取消</Button>
            <Button type="primary" :disabled="!aiImportResult" @click="fillAiDraftToForm">
              填入所选模块
            </Button>
          </Space>
        </div>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.old-system-booking-form {
  --operation-border: #e2e8f0;
  --operation-border-soft: #edf2f7;
  --operation-heading: #0f172a;
  --operation-label: #1e293b;
  --operation-muted: #64748b;
  --operation-normal: #475569;
  --operation-primary: #1677ff;
  --operation-primary-soft: #e6f4ff;
  --operation-panel-bg: #fff;
  --operation-soft-bg: #f8fafc;
  --booking-border: var(--operation-border);
  --booking-border-soft: var(--operation-border-soft);
  --booking-heading: var(--operation-heading);
  --booking-label: var(--operation-label);
  --booking-muted: var(--operation-muted);
  --booking-primary: var(--operation-primary);
  --booking-primary-soft: var(--operation-primary-soft);
  --booking-panel-bg: var(--operation-panel-bg);
  --booking-soft-bg: var(--operation-soft-bg);

  display: grid;
  gap: 12px;
  color: var(--operation-label);
}

.team-operation-shell,
.booking-agent-panel {
  overflow: hidden;
  background: var(--operation-panel-bg);
  border: 1px solid var(--operation-border);
  border-radius: 8px;
  box-shadow: 0 8px 22px rgb(15 23 42 / 5%);
}

.team-operation-shell :deep(.ant-card-body) {
  padding: 16px;
  background: var(--operation-panel-bg);
}

.team-operation-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 10px;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
  border: 1px solid var(--operation-border);
  border-radius: 8px;
}

.operation-title {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.25;
  color: var(--operation-heading);
}

.operation-subtitle {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--operation-muted);
}

.top-tool-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.top-tool-actions :deep(.ant-btn) {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  height: 30px;
  font-weight: 800;
  color: #334155;
  background: #fff;
  border-color: #dbe4f0;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.top-tool-actions :deep(.ant-btn-primary) {
  color: #fff;
  background: var(--operation-primary);
  border-color: var(--operation-primary);
}

.top-tool-actions :deep(.ant-btn):hover {
  color: #0958d9;
  background: #e6f4ff;
  border-color: #69b1ff;
}

.top-tool-actions :deep(.ant-btn-primary):hover {
  color: #fff;
  background: #0958d9;
  border-color: #0958d9;
}

.operation-flow-row {
  display: flex;
  gap: 6px;
  align-items: center;
  padding: 0 0 14px;
}

.stage-flow-item {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-width: 126px;
  height: 34px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #dbe4f0;
  border-radius: 5px;
}

.stage-flow-item.active {
  color: #fff;
  background: var(--operation-primary);
  border-color: var(--operation-primary);
  box-shadow: 0 4px 10px rgb(22 119 255 / 18%);
}

.stage-flow-item.pending {
  color: #334155;
  background: linear-gradient(180deg, #fff 0%, #f1f5f9 100%);
  border-color: #d7dee8;
}

.stage-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: 11px;
  font-weight: 900;
  color: currentcolor;
  background: rgb(255 255 255 / 70%);
  border: 1px solid currentcolor;
  border-radius: 999px;
}

.stage-flow-item.active .stage-index {
  color: #fff;
  background: rgb(255 255 255 / 16%);
  border-color: rgb(255 255 255 / 72%);
}

.stage-label {
  font-size: 12.5px;
  font-weight: 800;
  letter-spacing: 0;
}

.stage-arrow {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  color: #b8c2d0;
}

.team-name {
  margin-bottom: 10px;
  overflow: hidden;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.35;
  color: var(--operation-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 12px;
}

.team-badges :deep(.ant-tag) {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  margin-inline-end: 0;
  font-weight: 600;
  color: #334155 !important;
  background: var(--operation-soft-bg) !important;
  border-color: #dbe4f0 !important;
  border-radius: 4px;
}

.team-badges :deep(.ant-tag:nth-child(1)) {
  color: #0958d9 !important;
  background: #e6f4ff !important;
  border-color: #91caff !important;
}

.team-badges :deep(.ant-tag:nth-child(2)) {
  color: #ad6800 !important;
  background: #fff7e6 !important;
  border-color: #ffd591 !important;
}

.team-badges :deep(.ant-tag:nth-child(4)) {
  color: #1554ad !important;
  background: #eef6ff !important;
  border-color: #b7d7ff !important;
}

.team-badges :deep(.ant-tag:nth-child(5)) {
  color: #237804 !important;
  background: #f6ffed !important;
  border-color: #b7eb8f !important;
}

.team-metric-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 176px;
  gap: 8px;
  align-items: stretch;
}

.team-metric-strip {
  display: grid;
  grid-template-columns:
    minmax(104px, 0.86fr)
    minmax(104px, 0.86fr)
    minmax(104px, 0.86fr)
    minmax(104px, 0.86fr)
    minmax(218px, 1.5fr)
    minmax(132px, 1fr)
    minmax(110px, 0.86fr);
  min-width: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--operation-border);
  border-radius: 8px;
}

.team-metric-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  min-height: 58px;
  padding: 9px 12px;
  background: #fff;
  border-right: 1px solid var(--operation-border-soft);
}

.team-metric-item:last-child {
  border-right: 0;
}

.team-metric-item em {
  display: block;
  margin-bottom: 5px;
  overflow: hidden;
  font-size: 11.5px;
  font-style: normal;
  font-weight: 700;
  color: var(--operation-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-metric-item strong {
  display: block;
  overflow: hidden;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--operation-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-metric-item.metric-teamNo strong {
  overflow: visible;
  font-size: 15px;
  text-overflow: clip;
  letter-spacing: 0;
}

.team-metric-item small {
  margin-left: 4px;
  font-size: 13px;
  color: var(--ant-color-success);
}

.old-system-view-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
  overflow: hidden;
  background: transparent;
  border: 0;
}

.view-action-tile {
  display: flex;
  flex-direction: row;
  gap: 6px;
  align-items: center;
  justify-content: flex-start;
  min-height: 26px;
  padding: 6px 10px;
  color: #1554ad;
  cursor: pointer;
  background: #eef6ff;
  border: 1px solid #b7d7ff;
  border-radius: 5px;
}

.view-action-tile:hover,
.view-action-tile:focus {
  color: #0958d9;
  background: #f0f7ff;
}

.view-action-tile svg {
  flex: 0 0 auto;
  width: 16px;
  height: 16px;
}

.view-action-tile span {
  font-size: 12.5px;
  font-weight: 800;
}

.booking-row-section {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  background: #fff;
  border-bottom: 1px solid var(--booking-border);
}

.booking-row-section:last-child {
  border-bottom: 0;
}

.section-side-label {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 58px;
  padding: 14px 10px;
  font-size: 14px;
  font-weight: 800;
  color: #1554ad;
  text-align: center;
  background:
    linear-gradient(90deg, rgb(22 119 255 / 10%) 0 3px, transparent 3px),
    linear-gradient(180deg, #f8fbff 0%, #f5f9ff 100%);
  border-right: 1px solid var(--booking-border);
}

.section-main {
  min-width: 0;
  padding: 13px 16px;
}

.form-grid,
.travel-grid {
  display: grid;
  gap: 12px 14px;
}

.form-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-grid.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.form-grid.four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.travel-grid {
  grid-template-columns: 180px minmax(240px, 1fr) minmax(240px, 1fr) 220px;
}

.traffic-form {
  display: grid;
  gap: 8px;
  padding: 10px;
  background: #fbfdff;
  border: 1px solid var(--booking-border-soft);
  border-radius: 6px;
}

.traffic-line,
.customer-line,
.price-line-row {
  display: grid;
  grid-template-columns: 78px repeat(5, minmax(110px, 1fr));
  gap: 8px;
  align-items: center;
}

.join-date-line {
  grid-template-columns: 78px 220px;
}

.customer-line {
  grid-template-columns: 78px minmax(260px, 1fr) auto;
  margin-bottom: 12px;
}

.inline-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--booking-muted);
  white-space: nowrap;
}

.inline-link-button {
  padding-inline: 0;
}

.compact-grid :deep(.ant-form-item),
.travel-grid :deep(.ant-form-item) {
  margin-bottom: 0;
}

.span-full {
  grid-column: 1 / -1;
}

.wide-field {
  grid-column: span 1;
}

.full-input {
  width: 100%;
}

.price-line-editor {
  display: grid;
  gap: 10px;
}

.price-line-row {
  grid-template-columns: 150px 18px 110px 18px 42px 90px 42px minmax(150px, 1fr) 64px auto auto;
  padding: 8px 10px;
  background: #fbfdff;
  border: 1px solid var(--booking-border-soft);
  border-radius: 6px;
}

.money-symbol,
.multiply-symbol,
.quantity-label,
.remark-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--booking-muted);
  white-space: nowrap;
}

.price-type-select,
.price-number,
.quantity-number,
.price-remark-input {
  width: 100%;
}

.occupy-checkbox {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  font-size: 13px;
  color: var(--booking-heading);
  white-space: nowrap;
}

.price-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-left: 2px;
}

.booking-notice {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 8px;
  padding: 10px 12px;
  background: #fffaf0;
  border: 1px solid #ffe1a6;
  border-radius: 6px;
}

.booking-notice span {
  font-weight: 800;
  color: #ad6800;
}

.booking-notice p {
  min-width: 0;
  margin: 0;
  color: var(--booking-heading);
  white-space: pre-wrap;
}

.booking-subsection-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.guest-list-actions {
  justify-content: flex-end;
}

.booking-subsection-toolbar strong {
  font-size: 16px;
  font-weight: 800;
  color: var(--booking-heading);
}

.booking-subsection-toolbar :deep(.ant-btn) {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  font-weight: 700;
  color: #1554ad;
  background: #eef6ff;
  border-color: #b7d7ff;
}

.booking-subsection-toolbar :deep(.ant-btn-primary) {
  color: #fff;
  background: var(--booking-primary);
  border-color: var(--booking-primary);
}

.booking-subsection-toolbar :deep(.ant-btn-dangerous) {
  color: #cf1322;
  background: #fff1f0;
  border-color: #ffa39e;
}

.guest-list-actions :deep(.ant-btn) {
  height: 28px;
  color: #1554ad !important;
  background: #eef6ff !important;
  border-color: #b7d7ff !important;
  box-shadow: none;
}

.guest-list-actions :deep(.ant-btn-dangerous) {
  color: #cf1322 !important;
  background: #fff1f0 !important;
  border-color: #ffa39e !important;
}

.fee-total-line {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--booking-heading);
  text-align: right;
  border: 1px solid var(--booking-border);
  border-top: 0;
  border-radius: 0 0 6px 6px;
}

.fee-total-line strong {
  font-size: 16px;
  color: #cf1322;
}

.guest-table-scroll {
  overflow-x: auto;
  border: 1px solid var(--booking-border);
  border-radius: 6px;
}

.guest-edit-table {
  width: 100%;
  min-width: 1180px;
}

.guest-edit-table table {
  width: 100%;
  table-layout: fixed;
  border-spacing: 0;
  border-collapse: collapse;
  background: #fff;
}

.guest-edit-table th,
.guest-edit-table td {
  padding: 5px 3px;
  font-size: 12px;
  color: var(--booking-heading);
  text-align: center;
  vertical-align: top;
  border-right: 1px solid var(--booking-border);
  border-bottom: 1px solid var(--booking-border);
}

.guest-edit-table th {
  font-weight: 800;
  color: var(--booking-muted);
  background: var(--booking-soft-bg);
}

.guest-edit-table tr:last-child td {
  border-bottom: 0;
}

.guest-edit-table th:last-child,
.guest-edit-table td:last-child {
  border-right: 0;
}

.guest-select-cell,
.guest-leader-cell,
.guest-delete-cell {
  width: 2.4%;
  white-space: nowrap;
}

.guest-index-cell {
  width: 3%;
  white-space: nowrap;
}

.guest-name-column {
  width: 8.4%;
}

.guest-room-group-column {
  width: 5.4%;
}

.guest-id-column {
  width: 14.4%;
}

.guest-gender-column {
  width: 4.2%;
}

.guest-birth-column {
  width: 8.4%;
}

.guest-type-column {
  width: 5.8%;
}

.guest-age-column {
  width: 4.4%;
}

.guest-phone-column {
  width: 8.2%;
}

.guest-remark-column {
  width: 10%;
}

.guest-room-remark-column {
  width: 11.8%;
}

.guest-edit-table :deep(.ant-input),
.guest-edit-table :deep(.ant-select-selector),
.guest-edit-table :deep(.ant-input-number),
.guest-edit-table :deep(.ant-picker) {
  height: 30px;
  padding-inline: 6px;
  font-size: 13px;
}

.guest-edit-table :deep(.ant-select-selection-item),
.guest-edit-table :deep(.ant-picker-input > input),
.guest-edit-table :deep(.ant-input-number-input) {
  font-size: 13px;
}

.guest-age-column :deep(.ant-input-number-input) {
  padding-inline: 4px;
  text-align: center;
}

.guest-id-column :deep(.ant-input),
.guest-name-column :deep(.ant-input) {
  color: #0f172a;
  font-weight: 600;
}

.guest-id-warning-row td {
  background: #fff1f0;
}

.id-card-error {
  color: #cf1322;
}

.id-warning {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--ant-color-error);
}

.booking-agent-panel :deep(.ant-table-wrapper) {
  border: 1px solid var(--booking-border);
  border-radius: 6px;
}

.booking-agent-panel :deep(.ant-table),
.booking-agent-panel :deep(.ant-table-container),
.booking-agent-panel :deep(.ant-table-content),
.booking-agent-panel :deep(.ant-table-body),
.booking-agent-panel :deep(.ant-table-tbody),
.booking-agent-panel :deep(.ant-table-tbody > tr > td),
.booking-agent-panel :deep(.ant-table-placeholder),
.booking-agent-panel :deep(.ant-table-placeholder:hover > td) {
  color: var(--booking-heading);
  background: #fff !important;
}

.booking-agent-panel :deep(.ant-table-placeholder .ant-table-cell) {
  height: 96px;
  color: var(--booking-muted);
  background: #fff !important;
}

.booking-agent-panel :deep(.ant-empty) {
  color: var(--booking-muted);
  background: #fff !important;
}

.booking-agent-panel :deep(.ant-empty-description) {
  color: var(--booking-muted);
}

.booking-agent-panel :deep(.ant-table-thead > tr > th) {
  font-size: 12px;
  font-weight: 800;
  color: var(--booking-muted);
  background: var(--booking-soft-bg) !important;
}

.booking-agent-panel :deep(.ant-input),
.booking-agent-panel :deep(.ant-input-number),
.booking-agent-panel :deep(.ant-select-selector),
.booking-agent-panel :deep(.ant-picker),
.booking-agent-panel :deep(.ant-picker-input),
.booking-agent-panel :deep(textarea.ant-input) {
  color: var(--booking-heading);
  background: #fff !important;
  border-color: #dbe4f0;
  border-radius: 6px;
}

.booking-agent-panel :deep(.ant-input-number-input),
.booking-agent-panel :deep(.ant-select-selection-item),
.booking-agent-panel :deep(.ant-select-selection-placeholder),
.booking-agent-panel :deep(.ant-picker-input > input),
.booking-agent-panel :deep(.ant-input::placeholder),
.booking-agent-panel :deep(textarea.ant-input::placeholder) {
  color: var(--booking-muted);
}

.booking-agent-panel :deep(.ant-input[disabled]),
.booking-agent-panel :deep(.ant-input-number-disabled),
.booking-agent-panel :deep(.ant-picker-disabled),
.booking-agent-panel :deep(.ant-select-disabled .ant-select-selector) {
  color: #94a3b8;
  background: #f8fafc !important;
}

.booking-agent-panel :deep(.ant-input:hover),
.booking-agent-panel :deep(.ant-input-number:hover),
.booking-agent-panel :deep(.ant-select-selector:hover),
.booking-agent-panel :deep(.ant-picker:hover),
.booking-agent-panel :deep(textarea.ant-input:hover) {
  border-color: #91caff;
}

.booking-agent-panel :deep(.ant-form-item-label > label) {
  font-size: 13px;
  font-weight: 700;
  color: var(--booking-muted);
}

.booking-footer {
  position: sticky;
  bottom: 0;
  z-index: 2;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 12px 0 2px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--ant-color-bg-layout) 70%, transparent), var(--ant-color-bg-layout) 40%);
}

.ai-import-workbench {
  display: grid;
  gap: 12px;
}

.ai-workbench-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.85fr) minmax(0, 1.15fr);
  gap: 12px;
}

.ai-source-card,
.ai-result-card {
  border-color: #dbe4f0;
  border-radius: 8px;
}

.ai-upload-zone {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 12px;
  margin-bottom: 10px;
  color: #64748b;
  background: #f8fbff;
  border: 1px dashed #91caff;
  border-radius: 8px;
}

.ai-upload-zone :deep(.ant-btn) {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-weight: 700;
}

.ai-file-list {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}

.ai-file-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
  padding: 7px 8px;
  color: #334155;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.ai-file-name {
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  color: var(--ant-color-text-secondary);
}

.ai-summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.ai-summary-strip > div {
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.ai-summary-strip em {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  color: #64748b;
}

.ai-summary-strip strong {
  font-size: 20px;
  font-weight: 800;
  color: #1677ff;
}

.ai-module-score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.ai-module-score-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 7px;
  align-items: center;
  padding: 8px 9px;
  cursor: pointer;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.ai-module-score-card span {
  overflow: hidden;
  font-size: 12.5px;
  font-weight: 700;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-result-descriptions {
  margin-bottom: 10px;
}

.ai-price-lines {
  display: grid;
  gap: 4px;
  max-height: 128px;
  overflow-y: auto;
  font-size: 12.5px;
  line-height: 1.55;
  color: #334155;
}

.ai-price-lines p {
  margin: 0;
  word-break: break-word;
}

.ai-guest-table {
  margin-top: 12px;
}

.ai-evidence-box {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  padding: 10px;
  background: #fffdf5;
  border: 1px solid #ffe58f;
  border-radius: 8px;
}

.ai-evidence-box strong {
  display: block;
  margin-bottom: 4px;
  color: #ad6800;
}

.ai-evidence-box p {
  margin: 0;
  color: #475569;
}

.ai-empty-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-height: 430px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.ai-empty-preview svg {
  width: 40px;
  height: 40px;
  color: #1677ff;
}

.ai-empty-preview strong {
  font-size: 16px;
  color: #0f172a;
}

.ai-modal-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.ai-module-checkboxes {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  color: #475569;
}

.ai-module-checkboxes > span {
  font-weight: 800;
  color: #334155;
}

@media (max-width: 1200px) {
  .team-operation-header,
  .team-metric-panel {
    grid-template-columns: 1fr;
  }

  .top-tool-actions {
    justify-content: flex-start;
  }

  .team-metric-strip {
    grid-template-columns: repeat(4, minmax(120px, 1fr));
  }

  .old-system-view-actions {
    grid-template-columns: repeat(2, minmax(0, 180px));
  }

  .ai-workbench-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .booking-row-section {
    grid-template-columns: 1fr;
  }

  .section-side-label {
    justify-content: flex-start;
    border-right: 0;
    border-bottom: 1px solid var(--ant-color-border-secondary);
  }

  .form-grid.three,
  .form-grid.four,
  .travel-grid,
  .traffic-line,
  .customer-line,
  .price-line-row,
  .booking-subsection-toolbar {
    grid-template-columns: 1fr;
  }

  .booking-subsection-toolbar {
    display: grid;
    justify-items: start;
  }

  .team-metric-strip {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
