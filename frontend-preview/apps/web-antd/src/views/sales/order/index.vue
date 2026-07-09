<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  Upload,
  message,
} from 'ant-design-vue';
import type { TableColumnsType, TablePaginationConfig, UploadProps } from 'ant-design-vue';
import dayjs, { type Dayjs } from 'dayjs';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { uploadAttachment } from '#/api/common/attachment';
import {
  type SalesBookingApi,
  exportSalesBookingGuests,
  getSalesBookingOrderPage,
  updateSalesBookingOrderTagging,
} from '#/api/sales/booking';
import {
  type BookingAiImportApi,
  recognizeBookingAiImport,
} from '#/api/sales/booking-ai-import';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import MergeOrderModal from '../components/MergeOrderModal.vue';

type DatePickerValue = Dayjs | string | undefined;
type DateRangeValue = [string, string] | undefined;
type BooleanFilterValue = 'false' | 'true' | undefined;

const router = useRouter();
const tableLoading = ref(false);
const advancedSearchOpen = ref(false);
const mergeDialogOpen = ref(false);
const selectedOrderIds = ref<number[]>([]);
const orderRows = ref<SalesBookingApi.OrderManageRow[]>([]);
const total = ref(0);
const pagination = reactive({
  current: 1,
  pageSize: 20,
});
const query = reactive({
  bookedBy: '',
  buyerOrSalespersonKeyword: '',
  customerTeamNo: '',
  endDate: undefined as DatePickerValue,
  groupNo: '',
  guestKeyword: '',
  hasOrderFile: undefined as BooleanFilterValue,
  orderByType: 'booked',
  priceAll: undefined as number | undefined,
  productKeyword: '',
  startDate: undefined as DatePickerValue,
  status: undefined as SalesBookingApi.OrderStatus | undefined,
  tagging: undefined as BooleanFilterValue,
  teamType: undefined as string | undefined,
  trafficOrPickupRemark: '',
});

const teamTypeOptions = [
  { label: '全部', value: '' },
  { label: '散拼', value: 'sanpin' },
  { label: '整团', value: 'zhengtuan' },
  { label: '散团', value: 'santuan' },
  { label: '单项', value: 'single' },
];

const statusOptions = [
  { label: '全部', value: '' },
  { label: '未处理', value: 'pending' },
  { label: '已确认', value: 'confirmed' },
  { label: '已取消', value: 'cancelled' },
];

const booleanFilterOptions = [
  { label: '全部', value: undefined },
  { label: '是', value: 'true' },
  { label: '否', value: 'false' },
];

const orderByOptions = [
  { label: '下单时间', value: 'booked' },
  { label: '出团时间', value: 'departure' },
];

const columns: TableColumnsType<SalesBookingApi.OrderManageRow> = [
  { title: '发团日期', dataIndex: 'departureDate', key: 'departureDate', width: '8%' },
  { title: '订单信息', dataIndex: 'orderInfo', key: 'orderInfo', width: '24%' },
  { title: '客户 / 游客', dataIndex: 'customerGuest', key: 'customerGuest', width: '15%' },
  { title: '价格', dataIndex: 'priceSummary', key: 'priceSummary', width: '15%' },
  { title: '备注', dataIndex: 'remarks', key: 'remarks', width: '17%' },
  { title: '日期 / 预定人', dataIndex: 'bookingInfo', key: 'bookingInfo', width: '9%' },
  { title: '状态 / 操作', dataIndex: 'status', key: 'status', width: '8%' },
];

const departureDateRange = computed<DateRangeValue>({
  get(): DateRangeValue {
    const startDate = dateParam(query.startDate);
    const endDate = dateParam(query.endDate);
    if (!startDate || !endDate) return undefined;
    return [startDate, endDate];
  },
  set(value: DateRangeValue) {
    query.startDate = value?.[0];
    query.endDate = value?.[1];
  },
});

const rowSelection = computed(() => ({
  getCheckboxProps: (record: SalesBookingApi.OrderManageRow) => ({
    disabled: !canMergeOrder(record),
    title: canMergeOrder(record) ? '' : transferDisabledReason(record),
  }),
  selectedRowKeys: selectedOrderIds.value,
  onChange: (keys: Array<number | string>) => {
    selectedOrderIds.value = keys.map((key) => Number(key));
  },
}));
const selectedMergeOrders = computed(() => orderRows.value.filter((item) => selectedOrderIds.value.includes(item.id)));

const aiImportOpen = ref(false);
const aiImportLoading = ref(false);
const aiImportText = ref(sampleImportText());
const aiImportResult = ref<BookingAiImportApi.RecognizeResult>();
const aiImportAttachmentId = ref<number>();
const aiImportSourceType = ref('text');

const guestColumns = [
  { dataIndex: 'indexNo', key: 'indexNo', title: '序号', width: 58 },
  { dataIndex: 'name', key: 'name', title: '姓名', width: 90 },
  { dataIndex: 'certificateNo', key: 'certificateNo', title: '证件号', width: 180 },
  { dataIndex: 'gender', key: 'gender', title: '性别', width: 64 },
  { dataIndex: 'birthDate', key: 'birthDate', title: '出生年月', width: 110 },
  { dataIndex: 'age', key: 'age', title: '年龄', width: 64 },
  { dataIndex: 'phone', key: 'phone', title: '联系电话', width: 120 },
  { dataIndex: 'roomGroup', key: 'roomGroup', title: '分房', width: 110 },
  { dataIndex: 'leader', key: 'leader', title: '领队', width: 110 },
  { dataIndex: 'warnings', key: 'warnings', title: '身份证校验 / 提醒', width: 210 },
];

onMounted(() => {
  loadOrderPage();
});

function buildQueryParams(): SalesBookingApi.OrderManageQueryParams {
  return {
    bookedBy: clean(query.bookedBy),
    buyerOrSalespersonKeyword: clean(query.buyerOrSalespersonKeyword),
    customerTeamNo: clean(query.customerTeamNo),
    endDate: dateParam(query.endDate),
    groupNo: clean(query.groupNo),
    guestKeyword: clean(query.guestKeyword),
    hasOrderFile: parseBooleanFilter(query.hasOrderFile),
    orderByType: query.orderByType as 'booked' | 'departure',
    page: pagination.current,
    pageSize: pagination.pageSize,
    priceAll: query.priceAll,
    productKeyword: clean(query.productKeyword),
    startDate: dateParam(query.startDate),
    status: query.status,
    tagging: parseBooleanFilter(query.tagging),
    teamType: query.teamType,
    trafficOrPickupRemark: clean(query.trafficOrPickupRemark),
  };
}

function parseBooleanFilter(value: BooleanFilterValue) {
  if (value === 'true') return true;
  if (value === 'false') return false;
  return undefined;
}

async function loadOrderPage() {
  tableLoading.value = true;
  try {
    const result = await getSalesBookingOrderPage(buildQueryParams());
    orderRows.value = result.items ?? [];
    total.value = result.total ?? 0;
    selectedOrderIds.value = selectedOrderIds.value.filter((id) =>
      orderRows.value.some((item) => item.id === id),
    );
  } finally {
    tableLoading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  loadOrderPage();
}

function resetSearch() {
  query.bookedBy = '';
  query.buyerOrSalespersonKeyword = '';
  query.customerTeamNo = '';
  query.endDate = undefined;
  query.groupNo = '';
  query.guestKeyword = '';
  query.hasOrderFile = undefined;
  query.orderByType = 'booked';
  query.priceAll = undefined;
  query.productKeyword = '';
  query.startDate = undefined;
  query.status = undefined;
  query.tagging = undefined;
  query.teamType = undefined;
  query.trafficOrPickupRemark = '';
  handleSearch();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  pagination.current = nextPagination.current ?? 1;
  pagination.pageSize = nextPagination.pageSize ?? 20;
  loadOrderPage();
}

async function toggleTagging(record: SalesBookingApi.OrderManageRow, checked: boolean) {
  await updateSalesBookingOrderTagging(record.id, checked);
  record.tagging = checked;
  message.success(checked ? '已标记订单' : '已取消标记');
}

function openOrder(record: SalesBookingApi.OrderManageRow) {
  if (!record.teamId) {
    message.warning('该订单缺少团队信息，无法打开收客页');
    return;
  }
  router.push(`/sales/team/booking/${record.teamId}/${record.id}`);
}

async function exportNameList() {
  if (selectedOrderIds.value.length === 0) {
    message.warning('请先选择订单');
    return;
  }
  if (selectedOrderIds.value.length > 1) {
    message.info('批量名单压缩导出待接入，请先选择单个订单导出名单');
    return;
  }
  await exportSalesBookingGuests(selectedOrderIds.value[0]!);
}

function exportReport() {
  if (!query.startDate) {
    message.warning('出团日期不能为空!');
    return;
  }
  message.info('订单报表导出待接入');
}

function openMergeGroup() {
  if (selectedOrderIds.value.length === 0) {
    message.warning('请先选择订单');
    return;
  }
  const invalidOrder = selectedMergeOrders.value.find((item) => !canMergeOrder(item));
  if (invalidOrder) {
    message.warning(transferDisabledReason(invalidOrder));
    selectedOrderIds.value = selectedOrderIds.value.filter((id) => {
      const order = orderRows.value.find((item) => item.id === id);
      return order ? canMergeOrder(order) : false;
    });
    return;
  }
  mergeDialogOpen.value = true;
}

async function handleMergeSuccess() {
  selectedOrderIds.value = [];
  await loadOrderPage();
}

function canMergeOrder(record: SalesBookingApi.OrderManageRow) {
  return !isCancelledOrder(record) && !isMergeChildOrder(record);
}

function isCancelledOrder(record: SalesBookingApi.OrderManageRow) {
  return record.statusValue === 'cancelled' || record.status === '已取消' || record.status === 'cancelled';
}

function isMergeChildOrder(record: SalesBookingApi.OrderManageRow) {
  return record.orderRole === 'merge_child';
}

function transferDisabledReason(record: SalesBookingApi.OrderManageRow) {
  if (isCancelledOrder(record)) return '已取消订单不能拼团';
  if (isMergeChildOrder(record)) return '拼入订单不能再次拼团';
  return '';
}

function openOrderFile(record: SalesBookingApi.OrderManageRow) {
  message.info(record.hasOrderFile ? '订单文件页待接入' : '该订单暂未上传订单文件');
}

async function runAiImportRecognize() {
  if (!aiImportText.value.trim() && !aiImportAttachmentId.value) {
    message.warning('请先上传确认单或粘贴文本');
    return;
  }
  aiImportLoading.value = true;
  try {
    aiImportResult.value = await recognizeBookingAiImport({
      attachmentId: aiImportAttachmentId.value,
      sourceType: aiImportSourceType.value,
      text: aiImportText.value,
    });
    message.success('识别完成，请人工确认后再填入表单');
  } finally {
    aiImportLoading.value = false;
  }
}

const beforeUploadAiImportFile: UploadProps['beforeUpload'] = async (file) => {
  const formData = new FormData();
  formData.append('file', file as File);
  formData.append('businessModule', '销售收客');
  formData.append('businessType', 'AI辅助录入确认单');
  try {
    const attachment = await uploadAttachment(formData);
    aiImportAttachmentId.value = attachment.id;
    aiImportSourceType.value = fileExt(file.name);
    aiImportText.value = '';
    message.success('文件已上传，可以开始识别');
  } catch {
    message.error('文件上传失败');
  }
  return false;
};

function fileExt(fileName?: string) {
  const value = fileName || '';
  const index = value.lastIndexOf('.');
  return index >= 0 ? value.slice(index + 1).toLowerCase() : 'text';
}

function fillDraftToForm() {
  message.info('已填入当前表单草稿；只填入当前表单，不会自动保存订单');
}

function valueText(value?: number | string) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function warningColor(record: BookingAiImportApi.GuestInfo) {
  if (record.warnings?.length) return 'red';
  if (record.suspectedLeader) return 'orange';
  if (record.leader) return 'green';
  return 'default';
}

function statusColor(status?: string) {
  if (status === '已确认') return 'green';
  if (status === '已取消') return 'red';
  if (status === '未处理') return 'blue';
  return 'default';
}

function asOrderRow(record: unknown) {
  return record as SalesBookingApi.OrderManageRow;
}

function clean(value?: string) {
  const text = value?.trim();
  return text || undefined;
}

function dateParam(value: DatePickerValue) {
  if (!value) return undefined;
  return dayjs.isDayjs(value) ? value.format('YYYY-MM-DD') : dayjs(value).format('YYYY-MM-DD');
}

function displayText(value?: string) {
  return value && value.trim() ? value : '-';
}

function remarksTooltip(record: SalesBookingApi.OrderManageRow) {
  return [
    `导游备注：${displayText(record.guideRemark)}`,
    `费用说明：${displayText(record.feeRemark)}`,
    `订单备注：${displayText(record.orderRemark)}`,
  ].join('\n');
}

function sampleImportText() {
  return `航班时间：
2026年6月25日 大连-上海CZ6533（0910-1120）
2026年6月30日 上海-大连CZ6536（1920-2115）
导游：王导 13800000000
客户：杭州百缘 叶菊莲 13521124678
报价：成人 2999 元/人，儿童 1999 元/人，单房差 580 元
附加说明：张三、李四住一间，王五为领队。
序号 姓名 年龄 出生日期 身份证号 电话 分房 备注
1 张三 44 1982-06-21 210204198206214832 13521124678 1房 领队
2 李四 15 2010-10-28 21020420101028741X 13521124678 1房`;
}
</script>

<template>
  <Page title="订单管理">
    <Card class="order-page-card">
      <BusinessSearchForm
        actions-in-grid
        :model="query"
        :search-loading="tableLoading"
        :show-create="false"
        @reset="resetSearch"
        @search="handleSearch"
      >
        <Form.Item class="order-search-item" label="团号">
          <Input v-model:value="query.groupNo" allow-clear placeholder="请输入团号" @press-enter="handleSearch" />
        </Form.Item>
        <Form.Item class="order-search-item" label="客户团号">
          <Input v-model:value="query.customerTeamNo" allow-clear placeholder="请输入客户团号" @press-enter="handleSearch" />
        </Form.Item>
        <Form.Item class="order-search-item" label="预订单位">
          <Input
            v-model:value="query.buyerOrSalespersonKeyword"
            allow-clear
            placeholder="预订单位 / 业务员"
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
        <Form.Item class="order-search-item" label="状态">
          <Select v-model:value="query.status" :options="statusOptions" allow-clear placeholder="请选择状态" />
        </Form.Item>
        <Form.Item class="order-search-item" label="产品名称">
          <Input v-model:value="query.productKeyword" allow-clear placeholder="请输入产品名称" @press-enter="handleSearch" />
        </Form.Item>
        <Form.Item class="order-search-item" label="交通接送">
          <Input
            v-model:value="query.trafficOrPickupRemark"
            allow-clear
            placeholder="航班号 / 接送备注"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item class="order-search-item" label="金额">
          <InputNumber v-model:value="query.priceAll" :min="0" placeholder="请输入金额" />
        </Form.Item>
        <Form.Item class="order-search-item" label="下单人">
          <Input v-model:value="query.bookedBy" allow-clear placeholder="请输入下单人" @press-enter="handleSearch" />
        </Form.Item>
        <Form.Item class="order-search-item" label="游客">
          <Input v-model:value="query.guestKeyword" allow-clear placeholder="游客名称 / 证件号" @press-enter="handleSearch" />
        </Form.Item>
        <template v-if="advancedSearchOpen">
          <Form.Item class="order-search-item order-advanced-search-item" label="团队类型">
            <Select v-model:value="query.teamType" :options="teamTypeOptions" allow-clear placeholder="请选择类型" />
          </Form.Item>
          <Form.Item class="order-search-item order-advanced-search-item" label="排序">
            <Select v-model:value="query.orderByType" :options="orderByOptions" placeholder="请选择排序" />
          </Form.Item>
          <Form.Item class="order-search-item order-advanced-search-item" label="标记">
            <Select v-model:value="query.tagging" :options="booleanFilterOptions" placeholder="是否标记" />
          </Form.Item>
          <Form.Item class="order-search-item order-advanced-search-item" label="订单文件">
            <Select v-model:value="query.hasOrderFile" :options="booleanFilterOptions" placeholder="是否有文件" />
          </Form.Item>
        </template>
        <template #extraActions>
          <Button @click="advancedSearchOpen = !advancedSearchOpen">
            {{ advancedSearchOpen ? '收起筛选' : '高级筛选' }}
          </Button>
        </template>
      </BusinessSearchForm>

      <div class="order-table-toolbar">
        <div class="order-table-toolbar-main">
          <Typography.Text strong class="order-table-title">订单列表</Typography.Text>
          <Typography.Text type="secondary" class="order-table-meta">
            共 {{ total }} 条
            <template v-if="selectedOrderIds.length">，已选 {{ selectedOrderIds.length }} 条</template>
          </Typography.Text>
        </div>
        <Space class="order-table-actions" size="small">
          <Button @click="exportNameList">导出名单</Button>
          <Button @click="exportReport">导出报表</Button>
          <Button @click="openMergeGroup">拼团操作</Button>
        </Space>
      </div>

      <Table
        class="order-table"
        :columns="columns"
        :data-source="orderRows"
        :loading="tableLoading"
        :pagination="{
          current: pagination.current,
          pageSize: pagination.pageSize,
          showSizeChanger: true,
          showTotal: (count: number) => `共 ${count} 条记录`,
          total,
        }"
        :row-selection="rowSelection"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'departureDate'">
            <div class="date-cell">{{ displayText(record.departureDate) }}</div>
            <Tag v-if="record.teamTypeLabel" color="blue">{{ record.teamTypeLabel }}</Tag>
          </template>
          <template v-else-if="column.key === 'orderInfo'">
            <div class="order-info-cell">
              <Button type="link" size="small" class="order-link" @click="openOrder(asOrderRow(record))">
                {{ displayText(record.teamNo) }}
              </Button>
              <div class="order-product">{{ displayText(record.productName) }}</div>
              <Typography.Text
                class="muted-line"
                :content="displayText(record.orderInfo)"
                :ellipsis="{ tooltip: record.orderInfo }"
              />
              <div class="order-badges">
                <Tag v-if="record.orderRole && record.orderRole !== 'normal'" color="purple">
                  {{ record.orderRoleLabel }}
                </Tag>
                <Tag v-if="record.hasOrderFile" color="cyan" @click="openOrderFile(asOrderRow(record))">订单文件</Tag>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'customerGuest'">
            <div class="customer-guest-cell">
              <div class="field-line">
                <span class="field-label">客源</span>
                <Tooltip :title="record.sourcePlace">
                  <span class="ellipsis-cell">{{ displayText(record.sourcePlace) }}</span>
                </Tooltip>
              </div>
              <div class="field-line">
                <span class="field-label">客人</span>
                <Tooltip :title="record.guestName">
                  <span class="ellipsis-cell">{{ displayText(record.guestName) }}</span>
                </Tooltip>
              </div>
              <div class="guest-meta-line">
                <span class="guest-count">{{ displayText(record.guestCountText) }}</span>
                <Checkbox
                  :checked="record.tagging"
                  @change="(event) => toggleTagging(asOrderRow(record), event.target.checked)"
                >
                  标记
                </Checkbox>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'priceSummary'">
            <div class="price-summary-cell">
              <Tooltip :title="record.priceDetail">
                <div class="price-detail-line">{{ displayText(record.priceDetail) }}</div>
              </Tooltip>
              <div class="amount-row">
                <span>应收</span>
                <strong>{{ displayText(record.receivableAmount) }}</strong>
              </div>
              <div class="amount-row">
                <span>已收</span>
                <strong>{{ displayText(record.receivedAmount) }}</strong>
              </div>
              <div class="amount-row balance">
                <span>余额</span>
                <strong>{{ displayText(record.balanceAmount) }}</strong>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'remarks'">
            <Tooltip :title="remarksTooltip(asOrderRow(record))">
              <div class="remarks-cell">
                <div class="field-line">
                  <span class="field-label">导游</span>
                  <span class="ellipsis-cell">{{ displayText(record.guideRemark) }}</span>
                </div>
                <div class="field-line">
                  <span class="field-label">费用</span>
                  <span class="ellipsis-cell">{{ displayText(record.feeRemark) }}</span>
                </div>
                <div class="field-line">
                  <span class="field-label">订单</span>
                  <span class="ellipsis-cell">{{ displayText(record.orderRemark) }}</span>
                </div>
              </div>
            </Tooltip>
          </template>
          <template v-else-if="column.key === 'bookingInfo'">
            <Tooltip :title="record.bookingInfo">
              <span class="ellipsis-cell">{{ displayText(record.bookingInfo) }}</span>
            </Tooltip>
          </template>
          <template v-else-if="column.key === 'status'">
            <div class="status-action-cell">
              <Tag :color="statusColor(record.status)">{{ displayText(record.status) }}</Tag>
              <Button type="link" size="small" @click="openOrder(asOrderRow(record))">修改</Button>
            </div>
          </template>
        </template>
      </Table>
    </Card>

    <MergeOrderModal
      v-model:open="mergeDialogOpen"
      :orders="selectedMergeOrders"
      @success="handleMergeSuccess"
    />

    <Drawer
      v-model:open="aiImportOpen"
      title="AI辅助录入"
      width="980"
      destroy-on-close
    >
      <div class="ai-import-layout">
        <Alert
          class="ai-import-alert"
          show-icon
          type="info"
          message="AI 只辅助整理确认单资料，识别结果需要人工确认。点击填入只填入当前表单，不会自动保存订单。"
        />
        <Card size="small" title="资料来源">
          <Input.TextArea
            v-model:value="aiImportText"
            :rows="8"
            placeholder="粘贴确认单、微信消息、游客名单或报价文本"
          />
          <div class="ai-import-actions">
            <Space>
              <Upload :show-upload-list="false" :before-upload="beforeUploadAiImportFile">
                <Button>上传确认单</Button>
              </Upload>
              <Button type="primary" :loading="aiImportLoading" @click="runAiImportRecognize">
                开始识别
              </Button>
              <Button :disabled="!aiImportResult" @click="fillDraftToForm">
                填入表单
              </Button>
            </Space>
            <Typography.Text type="secondary">
              支持 Word / Excel / 文本；PDF / 图片需配置百炼视觉/OCR后使用。
            </Typography.Text>
          </div>
          <Typography.Text v-if="aiImportAttachmentId" type="secondary">
            已上传附件 ID：{{ aiImportAttachmentId }}，类型：{{ aiImportSourceType }}
          </Typography.Text>
        </Card>

        <Card v-if="aiImportResult" size="small" title="识别结果预览">
          <Alert
            v-if="aiImportResult.warnings?.length"
            class="ai-import-alert"
            type="warning"
            show-icon
            :message="aiImportResult.warnings.join('；')"
          />
          <Tabs>
            <Tabs.TabPane key="travel" tab="行程说明">
              <Descriptions bordered size="small" :column="2">
                <Descriptions.Item label="参团时间">{{ valueText(aiImportResult.travelInfo.joinDate) }}</Descriptions.Item>
                <Descriptions.Item label="来程">{{ valueText(aiImportResult.travelInfo.outboundOriginCity) }} -> {{ valueText(aiImportResult.travelInfo.outboundArrivalCity) }}</Descriptions.Item>
                <Descriptions.Item label="来程航班/车次">{{ valueText(aiImportResult.travelInfo.outboundTrafficNo) }}</Descriptions.Item>
                <Descriptions.Item label="来程时间">{{ valueText(aiImportResult.travelInfo.outboundDepartureTime) }} / {{ valueText(aiImportResult.travelInfo.outboundArrivalTime) }}</Descriptions.Item>
                <Descriptions.Item label="返程">{{ valueText(aiImportResult.travelInfo.returnDepartureCity) }} -> {{ valueText(aiImportResult.travelInfo.returnDestinationCity) }}</Descriptions.Item>
                <Descriptions.Item label="返程航班/车次">{{ valueText(aiImportResult.travelInfo.returnTrafficNo) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="guide" tab="导游相关">
              <Descriptions bordered size="small" :column="2">
                <Descriptions.Item label="导游">{{ valueText(aiImportResult.guideInfo.guideName) }}</Descriptions.Item>
                <Descriptions.Item label="导游电话">{{ valueText(aiImportResult.guideInfo.guidePhone) }}</Descriptions.Item>
                <Descriptions.Item label="全陪">{{ valueText(aiImportResult.guideInfo.escortName) }}</Descriptions.Item>
                <Descriptions.Item label="接待要求">{{ valueText(aiImportResult.guideInfo.receptionRequirement) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="customer" tab="客户信息">
              <Descriptions bordered size="small" :column="2">
                <Descriptions.Item label="客户单位">{{ valueText(aiImportResult.customerInfo.customerName) }}</Descriptions.Item>
                <Descriptions.Item label="联系人">{{ valueText(aiImportResult.customerInfo.contactName) }}</Descriptions.Item>
                <Descriptions.Item label="联系电话">{{ valueText(aiImportResult.customerInfo.contactPhone) }}</Descriptions.Item>
                <Descriptions.Item label="备注">{{ valueText(aiImportResult.customerInfo.remark) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="price" tab="价格信息">
              <Descriptions bordered size="small" :column="2">
                <Descriptions.Item label="成人价">{{ valueText(aiImportResult.priceInfo.adultPrice) }}</Descriptions.Item>
                <Descriptions.Item label="儿童价">{{ valueText(aiImportResult.priceInfo.childPrice) }}</Descriptions.Item>
                <Descriptions.Item label="老人价">{{ valueText(aiImportResult.priceInfo.seniorPrice) }}</Descriptions.Item>
                <Descriptions.Item label="单房差">{{ valueText(aiImportResult.priceInfo.singleRoomDifference) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="additional" tab="附加说明">
              <Descriptions bordered size="small" :column="1">
                <Descriptions.Item label="附加说明">{{ valueText(aiImportResult.additionalInfo.notes) }}</Descriptions.Item>
                <Descriptions.Item label="接待标准">{{ valueText(aiImportResult.additionalInfo.receptionStandard) }}</Descriptions.Item>
                <Descriptions.Item label="分房说明">{{ valueText(aiImportResult.additionalInfo.roomingNote) }}</Descriptions.Item>
                <Descriptions.Item label="领队说明">{{ valueText(aiImportResult.additionalInfo.leaderNote) }}</Descriptions.Item>
              </Descriptions>
            </Tabs.TabPane>
            <Tabs.TabPane key="guests" tab="游客名单">
              <Table
                :columns="guestColumns"
                :data-source="aiImportResult.guests"
                :pagination="false"
                row-key="indexNo"
                size="small"
                :row-class-name="(record) => record.warnings?.length ? 'guest-warning-row' : ''"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'leader'">
                    <Space size="small">
                      <Tag v-if="record.leader" color="green">领队</Tag>
                      <Tag v-else-if="record.suspectedLeader" color="orange">疑似领队</Tag>
                      <span v-else>-</span>
                      <Typography.Text v-if="record.leaderSourceText" type="secondary">
                        {{ record.leaderSourceText }}
                      </Typography.Text>
                    </Space>
                  </template>
                  <template v-else-if="column.key === 'warnings'">
                    <Tag :color="warningColor(record)">
                      {{ record.warnings?.length ? record.warnings.join('；') : record.idCardValid === false ? '身份证校验失败' : '身份证校验通过' }}
                    </Tag>
                  </template>
                </template>
              </Table>
            </Tabs.TabPane>
          </Tabs>
        </Card>
      </div>
    </Drawer>
  </Page>
</template>

<style scoped>
.order-page-card {
  min-height: 100%;
  border-radius: 8px;
}

.order-page-card :deep(.business-search-form) {
  margin-bottom: 8px;
}

.order-page-card :deep(.ant-card-body) {
  padding: 14px 16px;
}

.order-table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 10px 0 8px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}

.order-table-toolbar-main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.order-table-title {
  flex: 0 0 auto;
  color: #1f2937;
  font-size: 14px;
}

.order-table-meta {
  min-width: 0;
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-table-actions {
  flex: 0 0 auto;
}

.order-table-actions :deep(.ant-btn) {
  height: 30px;
  padding-inline: 12px;
  border-radius: 6px;
}

.order-table :deep(.ant-table) {
  font-size: 13px;
}

.order-table :deep(.ant-table table) {
  table-layout: fixed !important;
  width: 100% !important;
}

.order-table :deep(.ant-table-thead > tr > th) {
  padding: 9px 8px;
  font-weight: 700;
  text-align: center;
}

.order-table :deep(.ant-table-tbody > tr > td) {
  padding: 9px 8px;
  line-height: 1.45;
  vertical-align: middle;
}

.order-table :deep(.ant-table-cell) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-table :deep(.ant-checkbox-wrapper) {
  font-size: 13px;
}

.date-cell {
  margin-bottom: 4px;
  font-weight: 500;
  white-space: nowrap;
}

.order-info-cell {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.order-link {
  display: block;
  max-width: 100%;
  height: auto;
  padding: 0;
  overflow: hidden;
  font-weight: 600;
  line-height: 1.35;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-link :deep(span) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-product {
  overflow: hidden;
  color: rgb(17 24 39);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-line {
  display: block;
  max-width: 100%;
  overflow: hidden;
  color: rgb(100 116 139);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-badges {
  display: flex;
  overflow: hidden;
  flex-wrap: nowrap;
  gap: 4px;
}

.customer-guest-cell,
.price-summary-cell,
.remarks-cell {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
  white-space: normal;
}

.field-line {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  line-height: 1.4;
}

.field-line .ellipsis-cell {
  flex: 1 1 auto;
  min-width: 0;
}

.field-label {
  flex: 0 0 auto;
  padding: 0 5px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  line-height: 20px;
}

.guest-meta-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #475569;
}

.guest-meta-line :deep(.ant-checkbox-wrapper) {
  flex: 0 0 auto;
  font-size: 13px;
}

.guest-count {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-detail-line {
  overflow: hidden;
  color: #1f2937;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  line-height: 1.35;
}

.amount-row span {
  color: #64748b;
}

.amount-row strong {
  color: #1f2937;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.amount-row.balance strong {
  color: #d46b08;
}

.status-action-cell {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  white-space: nowrap;
}

.status-action-cell :deep(.ant-tag) {
  flex: 0 0 auto;
  margin-inline-end: 0;
}

.ellipsis-cell {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-table :deep(.ant-btn-link) {
  padding: 0 4px;
}

.ai-import-layout {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ai-import-alert {
  margin-bottom: 12px;
}

.ai-import-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

:deep(.guest-warning-row) td {
  background: #fff1f0;
}

</style>
