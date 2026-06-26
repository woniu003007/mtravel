<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { SalesTeamApi } from '#/api/sales/team';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Empty,
  Form,
  Select,
  Modal,
  Spin,
  Table,
  Tag,
  Textarea,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  type EnterpriseProductDictionaryApi as ProductDictionaryNamespace,
  getProductDictionaryAll,
} from '#/api/enterprise/product-dictionary';
import { getEnterpriseDepartmentAll } from '#/api/enterprise/department';
import { getEnterpriseEmployeeAll } from '#/api/enterprise/employee';
import { getSalesTeamOperationDetail, saveSalesTeam } from '#/api/sales/team';

type ActionInfo = SalesTeamApi.OperationActionInfo;
type ItineraryDay = SalesTeamApi.OperationItineraryDay;
type OperationDetail = SalesTeamApi.OperationDetail;
type OrderRow = SalesTeamApi.OperationOrderRow;
type DictItem = ProductDictionaryNamespace.Item;
type ProfileEditorType = 'business_type' | 'department' | 'escort' | 'internal_note' | 'operator' | 'team_type';
type SelectOption = { id?: number; label: string; value: string };

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const profileSaving = ref(false);
const profileOptionsLoading = ref(false);
const profileEditorOpen = ref(false);
const profileEditorType = ref<ProfileEditorType>('team_type');
const detail = ref<OperationDetail>();
const selectedOrderKeys = ref<number[]>([]);
const businessTypeOptions = ref<SelectOption[]>([]);
const departmentOptions = ref<SelectOption[]>([]);
const employeeOptions = ref<SelectOption[]>([]);
const profileForm = reactive({
  businessType: '',
  departmentName: '',
  escortEmployeeName: '',
  internalRemark: '',
  operatorEmployeeName: '',
  teamType: 'sanpin' as SalesTeamApi.TeamType,
});

const stageItems = [
  { key: 'receive', label: '收客' },
  { key: 'arrange', label: '排团' },
  { key: 'depart', label: '发团' },
  { key: 'settle', label: '结算' },
  { key: 'finish', label: '完成' },
];

const actionIcons: Record<string, string> = {
  bookingOrder: 'lucide:user-plus',
  cancelTeam: 'lucide:circle-x',
  copyTeam: 'lucide:copy',
  eContract: 'lucide:file-signature',
  editTeam: 'lucide:file-pen-line',
  exportPickup: 'lucide:file-down',
  guideBill: 'lucide:receipt-text',
  insideMemo: 'lucide:notebook-pen',
  mergeOrder: 'lucide:merge',
  moveOrder: 'lucide:move-right',
  orderFile: 'lucide:folder-open',
  printGuestList: 'lucide:users-round',
  printItinerary: 'lucide:printer',
  printSettlement: 'lucide:file-spreadsheet',
  stopBooking: 'lucide:pause-circle',
  teamArrangement: 'lucide:clipboard-list',
};

const ORDER_TABLE_SCROLL_X = 1840;
const orderColumns: TableColumnsType<OrderRow> = [
  { dataIndex: 'orderInfo', key: 'orderInfo', title: '订单信息', width: 310 },
  { dataIndex: 'pickupRemark', key: 'pickupRemark', title: '接送备注', width: 120 },
  { dataIndex: 'sourcePlace', key: 'sourcePlace', title: '客源地', width: 96 },
  { dataIndex: 'guestName', key: 'guestName', title: '客人', width: 90 },
  { align: 'center', dataIndex: 'guestCount', key: 'guestCount', title: '人数', width: 104 },
  { dataIndex: 'priceDetail', key: 'priceDetail', title: '价格详情', width: 220 },
  { align: 'right', dataIndex: 'receivableAmount', key: 'receivableAmount', title: '应收', width: 90 },
  { align: 'right', dataIndex: 'receivedAmount', key: 'receivedAmount', title: '已收', width: 90 },
  { align: 'right', dataIndex: 'balanceAmount', key: 'balanceAmount', title: '余额', width: 90 },
  { dataIndex: 'feeRemark', key: 'feeRemark', title: '费用说明', width: 140 },
  { dataIndex: 'orderRemark', key: 'orderRemark', title: '订单备注', width: 180 },
  { dataIndex: 'bookingInfo', key: 'bookingInfo', title: '日期/预定人', width: 130 },
  { align: 'center', dataIndex: 'status', key: 'status', title: '状态', width: 88 },
  { align: 'center', fixed: 'right', key: 'operation', title: '操作', width: 92 },
];

const teamId = computed(() => Number(route.params.id || 0));
const team = computed(() => detail.value?.team);
const product = computed(() => detail.value?.product);
const content = computed(() => detail.value?.content);
const itineraryDays = computed(() => detail.value?.itineraryDays || []);
const orders = computed(() => detail.value?.orders || []);
const prices = computed(() => detail.value?.prices || []);
const actions = computed(() => detail.value?.actions || []);
const teamTypeOptions: SelectOption[] = [
  { label: '散拼', value: 'sanpin' },
  { label: '整团', value: 'zhengtuan' },
  { label: '散团', value: 'santuan' },
  { label: '单项', value: 'single' },
];

const toolActions = computed(() => actions.value.filter((item) => item.group === 'tool'));
const businessActions = computed(() => actions.value.filter((item) => item.group !== 'tool'));
const activeProfileTitle = computed(() => ({
  business_type: '修改业务类型',
  department: '修改业务部门',
  escort: '修改全陪',
  internal_note: '修改内部备注',
  operator: '修改操作计调',
  team_type: '修改团队类型',
})[profileEditorType.value]);
const activeProfileLabel = computed(() => ({
  business_type: '业务类型',
  department: '所属部门',
  escort: '全陪',
  internal_note: '内部备注',
  operator: '操作计调',
  team_type: '团队类型',
})[profileEditorType.value]);
const activeProfileOptions = computed(() => {
  if (profileEditorType.value === 'team_type') return teamTypeOptions;
  if (profileEditorType.value === 'business_type') return businessTypeOptions.value;
  if (profileEditorType.value === 'department') return departmentOptions.value;
  if (['operator', 'escort'].includes(profileEditorType.value)) return employeeOptions.value;
  return [];
});
const profileBadges = computed(() => [
  { color: 'blue', editorType: 'team_type' as const, label: '团队类型', value: team.value?.teamTypeLabel || '--' },
  { color: 'orange', editorType: 'business_type' as const, label: '业务类型', value: team.value?.businessType || product.value?.businessType || '--' },
  { color: 'geekblue', editorType: 'department' as const, label: '部门', value: team.value?.departmentName || '--' },
  { color: 'blue', editorType: 'operator' as const, label: '操作计调', value: team.value?.operatorEmployeeName || '--' },
  { color: 'green', label: '导游', value: team.value?.guideSummary || '--' },
  { label: '领队', value: team.value?.leaderSummary || '--' },
  { editorType: 'escort' as const, label: '全陪', value: team.value?.escortEmployeeName || team.value?.escortSummary || '--' },
]);

const pageTitle = computed(() => {
  const teamNo = team.value?.teamNo;
  return teamNo ? `团队操作 - ${teamNo}` : '团队操作';
});

const metricItems = computed(() => [
  { key: 'totalSeats', label: '预控人数', value: `${team.value?.totalSeats ?? 0}人` },
  { key: 'usedSeats', label: '实收人数', value: `${team.value?.usedSeats ?? 0}人` },
  { key: 'remainingSeats', label: '剩余人数', value: `${team.value?.remainingSeats ?? 0}人` },
  { key: 'travelDays', label: '旅游天数', value: `${team.value?.travelDays ?? 1}天` },
  { key: 'teamNo', label: '团号', value: team.value?.teamNo || '--' },
  { key: 'departureDate', label: '出团日期', value: formatDate(team.value?.departureDate) },
  { key: 'distance', label: '总里程数', value: formatDistanceMeters(detail.value?.routeSummary?.totalDistanceMeters) },
]);

const rowSelection = computed(() => ({
  onChange: (keys: (number | string)[]) => {
    selectedOrderKeys.value = keys.map((item) => Number(item));
  },
  selectedRowKeys: selectedOrderKeys.value,
}));

function formatDate(value?: string) {
  return value ? dayjs(value).format('YYYY/MM/DD') : '--';
}

function formatDayDate(dayNo?: number) {
  if (!team.value?.departureDate || !dayNo) return '--';
  return dayjs(team.value.departureDate).add(dayNo - 1, 'day').format('YYYY/MM/DD');
}

function formatDistanceMeters(value?: number) {
  if (!value || value <= 0) return '--';
  if (value < 1000) return `${value}米`;
  return `${(value / 1000).toFixed(1)}公里`;
}

function formatDurationSeconds(value?: number) {
  if (!value || value <= 0) return '--';
  const hours = Math.floor(value / 3600);
  const minutes = Math.round((value % 3600) / 60);
  if (hours > 0 && minutes > 0) return `${hours}小时${minutes}分钟`;
  if (hours > 0) return `${hours}小时`;
  return `${minutes}分钟`;
}

function formatMoney(value?: number) {
  if (value === 0) return '0';
  if (value === undefined || value === null) return '--';
  return `${value}`;
}

function formatMeal(day: ItineraryDay) {
  const meals = [
    day.breakfastIncluded ? '早' : '',
    day.lunchIncluded ? '中' : '',
    day.dinnerIncluded ? '晚' : '',
  ].filter(Boolean);
  return meals.length ? meals.join(' / ') : '--';
}

function emptyText(value?: string | number | null) {
  if (value === 0) return '0';
  return value ? String(value) : '--';
}

function orderStatusColor(status?: string) {
  if (status === '已确认' || status === 'confirmed') return 'green';
  if (status === '已取消' || status === 'cancelled') return 'red';
  if (status === '未处理' || status === 'pending') return 'gold';
  return 'default';
}

function stageState(stageKey: string) {
  if (stageKey === 'receive') return 'active';
  if (stageKey === 'arrange' && (team.value?.usedSeats || 0) > 0) return 'active';
  return 'pending';
}

function actionIcon(code: string) {
  return actionIcons[code] || 'lucide:circle-dot';
}

function actionDanger(action: ActionInfo) {
  return action.group === 'danger';
}

function handleAction(action: ActionInfo) {
  if (['mergeOrder', 'moveOrder'].includes(action.code) && selectedOrderKeys.value.length === 0) {
    message.warning('请先选择订单');
    return;
  }
  if (action.code === 'teamArrangement' && team.value?.productId) {
    router.push(`/sales/product/team-arrangement/${team.value.productId}`);
    return;
  }
  if (action.code === 'editTeam' && team.value?.productId) {
    router.push(`/sales/product/schedule/${team.value.productId}`);
    return;
  }
  if (action.code === 'bookingOrder' && team.value?.id) {
    router.push(`/sales/team/booking/${team.value.id}`);
    return;
  }
  if (['cancelTeam', 'stopBooking'].includes(action.code)) {
    Modal.info({
      content: '该状态动作后续接入正式审批和状态流转后再开放，当前页面只做团队操作总览。',
      title: action.label,
    });
    return;
  }
  if (action.code === 'insideMemo') {
    openProfileEditor('internal_note');
    return;
  }
  message.info(action.note || `${action.label}待接入`);
}

function openOrder(row: { id?: null | number | string }) {
  const orderId = Number(row.id || 0);
  if (!team.value?.id || !orderId) return;
  router.push(`/sales/team/booking/${team.value.id}/${orderId}`);
}

function dictionaryOptions(items: DictItem[]) {
  return items
    .filter((item) => item.status === 'active')
    .map((item) => ({
      label: item.dictName,
      value: item.dictName,
    }));
}

async function ensureProfileOptions(type: ProfileEditorType) {
  if (type === 'internal_note') return;
  if (
    (type === 'team_type')
    || (type === 'business_type' && businessTypeOptions.value.length > 0)
    || (type === 'department' && departmentOptions.value.length > 0)
    || (['operator', 'escort'].includes(type) && employeeOptions.value.length > 0)
  ) {
    return;
  }
  profileOptionsLoading.value = true;
  try {
    if (type === 'business_type') {
      businessTypeOptions.value = dictionaryOptions(await getProductDictionaryAll('business_type'));
    } else if (type === 'department') {
      departmentOptions.value = (await getEnterpriseDepartmentAll(false)).map((item) => ({
        id: item.id,
        label: item.departmentName,
        value: item.departmentName,
      }));
    } else if (['operator', 'escort'].includes(type)) {
      employeeOptions.value = (await getEnterpriseEmployeeAll(false)).map((item) => ({
        id: item.id,
        label: item.employeeName,
        value: item.employeeName,
      }));
    }
  } finally {
    profileOptionsLoading.value = false;
  }
}

async function openProfileEditor(type: ProfileEditorType) {
  profileEditorType.value = type;
  profileForm.teamType = team.value?.teamType || 'sanpin';
  profileForm.businessType = team.value?.businessType || product.value?.businessType || '';
  profileForm.departmentName = team.value?.departmentName || '';
  profileForm.operatorEmployeeName = team.value?.operatorEmployeeName || '';
  profileForm.escortEmployeeName = team.value?.escortEmployeeName || team.value?.escortSummary || '';
  profileForm.internalRemark = content.value?.internalRemark || '';
  profileEditorOpen.value = true;
  await ensureProfileOptions(type);
}

function optionIdByValue(options: SelectOption[], value?: string) {
  return options.find((item) => item.value === value)?.id;
}

async function saveProfileEditor() {
  if (!team.value?.id) return;
  profileSaving.value = true;
  try {
    const departmentId = optionIdByValue(departmentOptions.value, profileForm.departmentName);
    const operatorEmployeeId = optionIdByValue(employeeOptions.value, profileForm.operatorEmployeeName);
    const escortEmployeeId = optionIdByValue(employeeOptions.value, profileForm.escortEmployeeName);
    await saveSalesTeam(team.value.id, {
      businessType: profileForm.businessType || undefined,
      departmentId,
      departmentName: profileForm.departmentName || undefined,
      escortEmployeeId,
      escortEmployeeName: profileForm.escortEmployeeName || undefined,
      operatorEmployeeId,
      operatorEmployeeName: profileForm.operatorEmployeeName || undefined,
      remark: profileForm.internalRemark || undefined,
      teamType: profileForm.teamType,
    });
    profileEditorOpen.value = false;
    message.success('团队属性已保存');
    await loadDetail();
  } finally {
    profileSaving.value = false;
  }
}

function showPriceModal() {
  Modal.info({
    content: prices.value.length
      ? h('div', { class: 'price-modal-table' }, [
          h('table', [
            h('thead', [
              h('tr', [
                h('th', '客户类型'),
                h('th', '成人价'),
                h('th', '儿童占床'),
                h('th', '儿童不占床'),
                h('th', '老人价'),
                h('th', '附加费'),
                h('th', '备注'),
              ]),
            ]),
            h('tbody', prices.value.map((item) => h('tr', { key: item.id }, [
              h('td', item.customerCategoryName || '默认'),
              h('td', formatMoney(item.adultPrice)),
              h('td', formatMoney(item.childPrice)),
              h('td', formatMoney(item.childNoBedPrice)),
              h('td', formatMoney(item.seniorPrice)),
              h('td', formatMoney(item.extraFee)),
              h('td', item.remark || '--'),
            ]))),
          ]),
        ])
      : h('div', { class: 'price-modal-empty' }, '当前团队暂无客户类型价格。'),
    title: '查看价格',
    width: 860,
  });
}

function showItineraryModal() {
  Modal.info({
    content: itineraryDays.value.length
      ? h('div', { class: 'itinerary-modal-content' }, itineraryDays.value.map((day) => h('section', { class: 'itinerary-day-card', key: day.id || day.dayNo }, [
          h('div', { class: 'itinerary-day-head' }, [
            h('span', { class: 'itinerary-day-badge' }, `D${day.dayNo || '-'}`),
            h('div', { class: 'itinerary-day-title' }, [
              h('strong', day.dayTitle || `第${day.dayNo || '-'}天行程`),
              h('span', formatDayDate(day.dayNo)),
            ]),
          ]),
          h('div', { class: 'itinerary-day-body' }, day.itineraryContent || '暂无行程内容'),
          h('div', { class: 'itinerary-day-meta' }, [
            h('span', `用餐：${formatMeal(day)}`),
            h('span', `住宿：${day.relatedHotel || day.accommodationNote || '--'}`),
            h('span', `路程：${formatDistanceMeters(day.roadbookTotalDistanceMeters)}`),
            h('span', `车程：${formatDurationSeconds(day.roadbookTotalDurationSeconds)}`),
          ]),
          day.roadbookSummary
            ? h('div', { class: 'itinerary-roadbook' }, `路书：${day.roadbookSummary}`)
            : null,
          day.remark ? h('div', { class: 'itinerary-remark' }, `备注：${day.remark}`) : null,
        ])))
      : h('div', { class: 'itinerary-modal-content' }, [
          h('div', { class: 'itinerary-day-body' }, content.value?.productDescription || '当前产品暂无行程说明。'),
        ]),
    title: '查看行程',
    width: 920,
  });
}

function goBack() {
  router.push('/sales/team');
}

async function loadDetail() {
  if (!teamId.value) {
    message.warning('缺少团队ID');
    goBack();
    return;
  }
  loading.value = true;
  try {
    detail.value = await getSalesTeamOperationDetail(teamId.value);
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
</script>

<template>
  <Page :title="pageTitle">
    <Spin :spinning="loading">
      <Card class="team-operation-shell" :bordered="false">
        <div class="team-operation-header">
          <div>
            <div class="operation-title">团队操作</div>
            <div class="operation-subtitle">团队业务操作台：收客、排团、发团、结算入口集中跟进。</div>
          </div>
          <div class="top-tool-actions">
            <Button
              v-for="action in toolActions"
              :key="action.code"
              size="small"
              @click="handleAction(action)"
            >
              <IconifyIcon :icon="actionIcon(action.code)" />
              <span>{{ action.label }}</span>
            </Button>
          </div>
        </div>

        <div class="operation-flow-row" aria-label="团队阶段">
          <template v-for="(stage, index) in stageItems" :key="stage.key">
            <div class="stage-flow-item" :class="stageState(stage.key)">
              <span class="stage-index">{{ index + 1 }}</span>
              <span class="stage-label">{{ stage.label }}</span>
            </div>
            <IconifyIcon v-if="index < stageItems.length - 1" icon="lucide:chevrons-right" class="stage-arrow" />
          </template>
        </div>

        <div class="team-profile-block">
          <div class="team-name">{{ product?.productName || '未命名团队' }}</div>
          <div class="team-badges">
            <Tag
              v-for="badge in profileBadges"
              :key="badge.label"
              :color="badge.color"
              class="profile-edit-tag"
              @click="badge.editorType && openProfileEditor(badge.editorType)"
            >
              {{ badge.label }}：{{ badge.value }}
              <IconifyIcon v-if="badge.editorType" icon="lucide:pencil" />
            </Tag>
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
                <strong>{{ item.value }}</strong>
              </span>
            </div>
            <div class="old-system-view-actions">
              <button type="button" class="view-action-tile" @click="showPriceModal">
                <IconifyIcon icon="lucide:tag" />
                <span>查看价格</span>
              </button>
              <button type="button" class="view-action-tile" @click="showItineraryModal">
                <IconifyIcon icon="lucide:briefcase-business" />
                <span>查看行程</span>
              </button>
            </div>
          </div>
        </div>

        <div class="team-description-stack">
          <div class="description-row">
            <div class="description-title">产品说明</div>
            <div class="description-text">{{ content?.productDescription || '无' }}</div>
          </div>
          <div class="description-row">
            <div class="description-title">收客须知</div>
            <div class="description-text">{{ content?.bookingNotice || '无' }}</div>
          </div>
          <div class="description-row internal">
            <div class="description-title">
              <IconifyIcon icon="lucide:info" />
              <span>内部备注</span>
            </div>
            <div class="description-text">{{ content?.internalRemark || '无' }}</div>
            <Button type="link" size="small" @click="openProfileEditor('internal_note')">
              <IconifyIcon icon="lucide:file-pen-line" />
              <span>编辑</span>
            </Button>
          </div>
        </div>

        <div class="operation-icon-actions">
          <button
            v-for="action in businessActions"
            :key="action.code"
            type="button"
            class="operation-icon-button"
            :class="{ danger: actionDanger(action) }"
            @click="handleAction(action)"
          >
            <span class="operation-icon-circle">
              <IconifyIcon :icon="actionIcon(action.code)" />
            </span>
            <span class="operation-icon-label">{{ action.label }}</span>
          </button>
          <button type="button" class="operation-icon-button" @click="goBack">
            <span class="operation-icon-circle">
              <IconifyIcon icon="lucide:undo-2" />
            </span>
            <span class="operation-icon-label">返回上页</span>
          </button>
        </div>

        <div class="order-section">
          <div class="section-heading">
            <IconifyIcon icon="lucide:list-checks" />
            <span>该团相关订单</span>
          </div>
          <Table
            :columns="orderColumns"
            :data-source="orders"
            :pagination="false"
            :row-selection="rowSelection"
            :scroll="{ x: ORDER_TABLE_SCROLL_X }"
            row-key="id"
            size="small"
            class="order-table"
            @row="(record: OrderRow) => ({ onClick: () => openOrder(record) })"
          >
            <template #emptyText>
              <Empty description="订单模块未接入，当前暂无订单数据" />
            </template>
            <template #bodyCell="{ column, record, text }">
              <template v-if="column.key === 'orderInfo'">
                <div
                  class="order-info-cell"
                  :title="[
                    emptyText(record.orderNo),
                    emptyText(record.orderInfo),
                    emptyText(record.pickupInfo),
                    emptyText(record.dropoffInfo),
                    emptyText(record.originalOrderInfo),
                  ].join('\n')"
                >
                  <div class="order-info-no">{{ emptyText(record.orderNo) }}</div>
                  <div class="order-cell-secondary">{{ emptyText(record.orderInfo) }}</div>
                  <div v-if="record.pickupInfo" class="order-multiline-cell">{{ record.pickupInfo }}</div>
                  <div v-if="record.dropoffInfo" class="order-multiline-cell">{{ record.dropoffInfo }}</div>
                  <div v-if="record.originalOrderInfo" class="order-original-row">
                    原始订单信息：{{ record.originalOrderInfo }}
                  </div>
                </div>
              </template>
              <template v-else-if="column.key === 'guestCount'">
                <div class="order-count-cell">{{ emptyText(record.guestCountText || text) }}</div>
              </template>
              <template v-else-if="column.key === 'priceDetail'">
                <div class="order-multiline-cell" :title="emptyText(record.priceDetail)">
                  {{ emptyText(record.priceDetail) }}
                </div>
              </template>
              <template v-else-if="column.key === 'feeRemark'">
                <div class="order-multiline-cell" :title="emptyText(record.feeRemark)">
                  {{ emptyText(record.feeRemark) }}
                </div>
              </template>
              <template v-else-if="column.key === 'orderRemark'">
                <Tooltip
                  :title="emptyText(record.orderRemark)"
                  overlay-class-name="order-remark-tooltip"
                  placement="topLeft"
                >
                  <div class="order-remark-ellipsis">
                    {{ emptyText(record.orderRemark) }}
                  </div>
                </Tooltip>
              </template>
              <template v-else-if="['receivableAmount', 'receivedAmount', 'balanceAmount'].includes(String(column.key))">
                <div class="order-money-cell">{{ emptyText(text) }}</div>
              </template>
              <template v-else-if="column.key === 'status'">
                <div class="order-status-cell">
                  <Tag :color="orderStatusColor(record.status)">{{ emptyText(record.status) }}</Tag>
                </div>
              </template>
              <template v-else-if="column.key === 'operation'">
                <Button type="link" size="small" class="order-edit-button" @click.stop="openOrder(record)">
                  <IconifyIcon icon="lucide:file-pen-line" />
                  <span>修改</span>
                </Button>
              </template>
              <template v-else>
                <div class="order-cell-clamp" :title="emptyText(text)">{{ emptyText(text) }}</div>
              </template>
            </template>
          </Table>
        </div>
      </Card>
    </Spin>
    <Modal
      v-model:open="profileEditorOpen"
      :title="activeProfileTitle"
      :footer="null"
      width="520px"
      destroy-on-close
    >
      <Spin :spinning="profileOptionsLoading || profileSaving">
        <Form layout="vertical" class="profile-editor-form">
          <Form.Item :label="activeProfileLabel">
            <Textarea
              v-if="profileEditorType === 'internal_note'"
              v-model:value="profileForm.internalRemark"
              :rows="4"
              placeholder="填写团队内部备注"
              :maxlength="500"
              show-count
            />
            <Select
              v-else-if="profileEditorType === 'team_type'"
              v-model:value="profileForm.teamType"
              :options="activeProfileOptions"
              placeholder="请选择团队类型"
            />
            <Select
              v-else-if="profileEditorType === 'business_type'"
              v-model:value="profileForm.businessType"
              :options="activeProfileOptions"
              allow-clear
              placeholder="请选择业务类型"
            />
            <Select
              v-else-if="profileEditorType === 'department'"
              v-model:value="profileForm.departmentName"
              :options="activeProfileOptions"
              allow-clear
              show-search
              placeholder="请选择所属部门"
            />
            <Select
              v-else-if="profileEditorType === 'operator'"
              v-model:value="profileForm.operatorEmployeeName"
              :options="activeProfileOptions"
              allow-clear
              show-search
              placeholder="请选择操作计调"
            />
            <Select
              v-else
              v-model:value="profileForm.escortEmployeeName"
              :options="activeProfileOptions"
              allow-clear
              show-search
              placeholder="请选择全陪"
            />
          </Form.Item>
          <div class="profile-editor-actions">
            <Button @click="profileEditorOpen = false">取消</Button>
            <Button type="primary" :loading="profileSaving" @click="saveProfileEditor">保存</Button>
          </div>
        </Form>
      </Spin>
    </Modal>
  </Page>
</template>

<style scoped>
.team-operation-shell {
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

  overflow: hidden;
  color: var(--operation-label);
  background: #fff;
  border: 1px solid var(--operation-border);
  border-radius: 8px;
  box-shadow: 0 8px 22px rgb(15 23 42 / 5%);
}

.team-operation-shell :deep(.ant-card-body) {
  padding: 16px;
  background: #fff;
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

.top-tool-actions :deep(.ant-btn),
.description-row :deep(.ant-btn) {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-weight: 800;
}

.top-tool-actions :deep(.ant-btn) {
  height: 30px;
  color: #334155;
  background: #fff;
  border-color: #dbe4f0;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.top-tool-actions :deep(.ant-btn):hover {
  color: #0958d9;
  background: #e6f4ff;
  border-color: #69b1ff;
}

.operation-flow-row {
  display: flex;
  align-items: center;
  gap: 6px;
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
  background: #1677ff;
  border-color: #1677ff;
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
  background: #f8fafc !important;
  border-color: #dbe4f0 !important;
  border-radius: 4px;
}

.profile-edit-tag {
  cursor: pointer;
}

.profile-edit-tag:hover {
  color: #0958d9 !important;
  background: #e6f4ff !important;
  border-color: #91caff !important;
}

.profile-edit-tag svg {
  width: 12px;
  height: 12px;
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
  letter-spacing: 0;
  text-overflow: clip;
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

.team-description-stack {
  margin-top: 10px;
  border: 1px solid var(--operation-border);
  border-radius: 8px;
}

.description-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  min-height: 42px;
  padding: 10px 14px;
  background: #fff;
  border-bottom: 1px solid var(--operation-border-soft);
}

.description-row:first-child {
  border-radius: 8px 8px 0 0;
}

.description-row.internal {
  min-height: 58px;
  padding: 0;
  overflow: hidden;
  background: #f8fbff;
  border-bottom: 0;
  border-radius: 0 0 8px 8px;
}

.description-row.internal .description-title {
  display: flex;
  gap: 8px;
  align-items: center;
  height: 100%;
  padding: 14px 16px;
  color: #1e293b;
  background: #eef6ff;
}

.description-row.internal .description-title svg {
  width: 18px;
  height: 18px;
  color: var(--operation-primary);
}

.description-row.internal .description-text {
  padding: 14px 0;
}

.description-row.internal :deep(.ant-btn) {
  margin: 10px 10px 0 0;
}

.description-title {
  font-size: 14px;
  font-weight: 800;
  color: var(--operation-label);
}

.description-text {
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--operation-normal);
  word-break: break-word;
}

.operation-icon-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 10px 0 12px;
}

.operation-icon-button {
  display: flex;
  flex-direction: row;
  gap: 6px;
  align-items: center;
  justify-content: flex-start;
  min-width: 0;
  min-height: 32px;
  padding: 5px 10px;
  color: #334155;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dbe4f0;
  border-radius: 5px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 3%);
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.operation-icon-button:hover,
.operation-icon-button:focus {
  color: #0958d9;
  background: #f8fbff;
  border-color: #91caff;
  box-shadow: 0 4px 12px rgb(22 119 255 / 10%);
}

.operation-icon-button.danger {
  color: #b42318;
}

.operation-icon-button.danger:hover,
.operation-icon-button.danger:focus {
  color: #ff4d4f;
}

.operation-icon-circle {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: currentcolor;
  background: #f1f5f9;
  border: 1px solid #dbe4f0;
  border-radius: 5px;
  transition: all 0.16s ease;
}

.operation-icon-button:hover .operation-icon-circle,
.operation-icon-button:focus .operation-icon-circle {
  background: #e6f4ff;
  border-color: #91caff;
  box-shadow: 0 4px 12px rgb(22 119 255 / 14%);
}

.operation-icon-button.danger .operation-icon-circle {
  background: #fff5f5;
  border-color: #ffd6d6;
}

.operation-icon-button.danger:hover .operation-icon-circle,
.operation-icon-button.danger:focus .operation-icon-circle {
  background: #fff1f0;
  border-color: #ffccc7;
}

.operation-icon-circle svg {
  width: 13px;
  height: 13px;
}

.operation-icon-label {
  display: block;
  width: auto;
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.2;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-section {
  padding-top: 0;
}

.section-heading {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 0;
  font-size: 16px;
  font-weight: 800;
  color: #1554ad;
  background: #eef6ff;
  border: 1px solid #b7d7ff;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.section-heading svg {
  width: 18px;
  height: 18px;
}

.order-table :deep(.ant-table) {
  table-layout: fixed;
  font-size: 12.5px;
  background: #fff;
  border: 1px solid #d7dee8;
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.order-table :deep(.ant-table-thead > tr > th) {
  font-size: 12px;
  font-weight: 900;
  color: #475569;
  background: #f8fafc;
  border-bottom: 1px solid #dbe4f0 !important;
}

.order-table :deep(.ant-table-cell) {
  padding: 7px 9px !important;
  vertical-align: top;
  border-color: #e2e8f0 !important;
}

.order-table :deep(.ant-table-tbody > tr > td) {
  color: #334155;
  cursor: pointer;
  background: #fff;
}

.order-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fbff;
}

.order-table :deep(.ant-table-placeholder > td) {
  background: #fff !important;
  border-bottom: 0 !important;
}

.order-table :deep(.ant-table-container::before),
.order-table :deep(.ant-table-container::after) {
  display: none;
}

.order-table :deep(.ant-empty-description) {
  color: #64748b;
}

.order-info-cell {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.order-info-no {
  overflow: hidden;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.35;
  color: #1554ad;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-cell-secondary {
  overflow: hidden;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-multiline-cell {
  overflow: hidden;
  font-size: 12px;
  line-height: 1.45;
  color: #334155;
  word-break: break-word;
  white-space: pre-line;
}

.order-original-row {
  padding-top: 4px;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.45;
  color: #64748b;
  word-break: break-word;
  border-top: 1px dashed #cbd5e1;
}

.order-remark-ellipsis {
  display: -webkit-box;
  max-height: 58px;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.45;
  color: #334155;
  word-break: break-word;
  cursor: help;
  white-space: pre-line;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.order-cell-clamp {
  display: -webkit-box;
  max-height: 58px;
  overflow: hidden;
  line-height: 1.5;
  color: #334155;
  word-break: break-word;
  white-space: pre-line;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.order-count-cell,
.order-money-cell {
  overflow: hidden;
  font-weight: 900;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-count-cell {
  text-align: center;
}

.order-money-cell {
  color: #0f766e;
  text-align: right;
}

.order-status-cell {
  display: flex;
  justify-content: center;
}

.order-status-cell :deep(.ant-tag) {
  margin-inline-end: 0;
  font-size: 12px;
  font-weight: 800;
}

.order-edit-button {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 24px;
  padding: 0 2px;
  font-size: 12px;
  font-weight: 800;
}

.order-edit-button svg {
  width: 14px;
  height: 14px;
}

.muted {
  color: #94a3b8;
}

.profile-editor-form {
  padding-top: 6px;
}

.profile-editor-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 8px;
}

:global(.price-modal-table) {
  max-width: 100%;
  overflow: auto;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

:global(.price-modal-table table) {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
  background: #fff;
}

:global(.price-modal-table th),
:global(.price-modal-table td) {
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.45;
  text-align: left;
  border-bottom: 1px solid #edf2f7;
}

:global(.price-modal-table th) {
  font-weight: 800;
  color: #334155;
  background: #f8fafc;
}

:global(.price-modal-table tr:last-child td) {
  border-bottom: 0;
}

:global(.price-modal-empty) {
  padding: 18px;
  font-size: 14px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

:global(.itinerary-modal-content) {
  display: grid;
  gap: 12px;
  max-height: 62vh;
  padding-right: 4px;
  overflow: auto;
}

:global(.itinerary-day-card) {
  padding: 14px;
  background: #fff;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

:global(.itinerary-day-head) {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

:global(.itinerary-day-badge) {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 28px;
  font-size: 13px;
  font-weight: 900;
  color: #1554ad;
  background: #eef6ff;
  border: 1px solid #b7d7ff;
  border-radius: 6px;
}

:global(.itinerary-day-title) {
  display: flex;
  flex: 1 1 auto;
  gap: 10px;
  align-items: baseline;
  justify-content: space-between;
  min-width: 0;
}

:global(.itinerary-day-title strong) {
  overflow: hidden;
  font-size: 15px;
  font-weight: 900;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.itinerary-day-title span) {
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

:global(.itinerary-day-body) {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
}

:global(.itinerary-day-meta) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

:global(.itinerary-day-meta span) {
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
}

:global(.itinerary-roadbook),
:global(.itinerary-remark) {
  padding: 8px 10px;
  margin-top: 10px;
  font-size: 12.5px;
  font-weight: 700;
  line-height: 1.6;
  color: #475569;
  background: #f8fbff;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
}

:global(.order-remark-tooltip) {
  max-width: 520px;
}

:global(.order-remark-tooltip .ant-tooltip-inner) {
  max-height: 360px;
  overflow: auto;
  line-height: 1.6;
  white-space: pre-wrap;
}

@media (width <= 1440px) {
  .team-metric-panel {
    grid-template-columns: minmax(0, 1fr) 168px;
  }

  .team-metric-strip {
    grid-template-columns:
      minmax(92px, 1fr)
      minmax(92px, 1fr)
      minmax(92px, 1fr)
      minmax(92px, 1fr)
      minmax(210px, 1.6fr)
      minmax(118px, 1fr)
      minmax(92px, 1fr);
  }
}

@media (width <= 1024px) {
  .team-operation-header,
  .operation-flow-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-tool-actions {
    justify-content: flex-start;
  }

  .operation-flow-row {
    gap: 8px;
  }

  .stage-flow-item {
    width: 100%;
  }

  .stage-arrow {
    display: none;
  }

  .team-metric-panel {
    grid-template-columns: 1fr;
  }

  .operation-icon-actions {
    gap: 6px;
  }

  .operation-icon-button {
    flex: 1 1 128px;
  }
}
</style>
