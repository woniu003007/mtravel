<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Textarea,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  createQuoteGroundAgentRule,
  createQuoteGuideLevel,
  createQuoteGuideRule,
  createQuoteResourceRule,
  deleteQuoteGroundAgentRule,
  deleteQuoteGuideLevel,
  deleteQuoteGuideRule,
  deleteQuoteResourceRule,
  getQuoteApprovalConfig,
  getQuoteGroundAgentRulePage,
  getQuoteGuideLevelAll,
  getQuoteGuideLevelPage,
  getQuoteGuideRulePage,
  getQuoteResourceRulePage,
  saveQuoteApprovalConfig,
  updateQuoteGroundAgentRule,
  updateQuoteGuideLevel,
  updateQuoteGuideRule,
  updateQuoteResourceRule,
  type QuoteConfigApi,
} from '#/api/configuration/quote';
import { getCustomerCategoryAll, type CustomerCategoryApi } from '#/api/customer/category';
import {
  getEnterpriseEmployeePage,
  type EnterpriseEmployeeApi,
} from '#/api/enterprise/employee';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

type SelectOption<T extends number | string = string> = { label: string; value: T };

const activeTab = ref('resource');
const saving = ref(false);
const resourceLoading = ref(false);
const guideLevelLoading = ref(false);
const guideRuleLoading = ref(false);
const groundLoading = ref(false);
const approvalLoading = ref(false);
const resourceModalOpen = ref(false);
const guideLevelModalOpen = ref(false);
const guideRuleModalOpen = ref(false);
const groundModalOpen = ref(false);
const resourceEditingId = ref<number>();
const guideLevelEditingId = ref<number>();
const guideRuleEditingId = ref<number>();
const groundEditingId = ref<number>();
const customerCategoryOptions = ref<SelectOption<number>[]>([]);
const guideLevelOptions = ref<SelectOption<number>[]>([]);
const employeeOptions = ref<SelectOption<number>[]>([]);

const statusOptions: SelectOption<QuoteConfigApi.Status>[] = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];
const resourceTypeOptions: SelectOption<QuoteConfigApi.ResourceType>[] = [
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '餐饮', value: 'restaurant' },
  { label: '用车', value: 'vehicle' },
  { label: '大交通', value: 'transport' },
  { label: '其它资源', value: 'other' },
  { label: '杂费', value: 'misc' },
];
const resourceQuoteModeOptions: SelectOption<QuoteConfigApi.ResourceQuoteMode>[] = [
  { label: '按比例报价', value: 'rate' },
  { label: '按固定加价报价', value: 'fixed' },
  { label: '两种方式都可以报价', value: 'both' },
];
const approvalModeOptions: SelectOption<QuoteConfigApi.ApprovalMode>[] = [
  { label: '当前账号直属领导（部门负责人）', value: 'department_manager' },
  { label: '指定人员', value: 'specified_person' },
];

const resourceRows = ref<QuoteConfigApi.ResourceRule[]>([]);
const guideLevelRows = ref<QuoteConfigApi.GuideLevel[]>([]);
const guideRuleRows = ref<QuoteConfigApi.GuideRule[]>([]);
const groundRows = ref<QuoteConfigApi.GroundAgentRule[]>([]);

const resourceQuery = reactive<QuoteConfigApi.ResourceRuleQuery>({ page: 1, pageSize: 10 });
const guideLevelQuery = reactive<QuoteConfigApi.GuideLevelQuery>({ page: 1, pageSize: 10 });
const guideRuleQuery = reactive<QuoteConfigApi.GuideRuleQuery>({ page: 1, pageSize: 10 });
const groundQuery = reactive<QuoteConfigApi.GroundAgentRuleQuery>({ page: 1, pageSize: 10 });

const resourcePagination = pagination();
const guideLevelPagination = pagination();
const guideRulePagination = pagination();
const groundPagination = pagination();

const resourceForm = reactive<QuoteConfigApi.ResourceRuleSaveParams>({
  minimumFixedMarkup: 0,
  minimumMarkupRate: 0,
  quoteMode: 'both',
  resourceType: 'scenic',
  status: 'active',
  suggestedFixedMarkup: 0,
  suggestedMarkupRate: 0,
});
const guideLevelForm = reactive<QuoteConfigApi.GuideLevelSaveParams>({
  levelName: '',
  sortOrder: 0,
  status: 'active',
});
const guideRuleForm = reactive<QuoteConfigApi.GuideRuleSaveParams>({
  baseDailyFee: 0,
  foreignLanguageDailyMarkup: 0,
  guideLevelId: undefined as unknown as number,
  language: '普通话',
  overtimeHourlyFee: 0,
  status: 'active',
});
const groundForm = reactive<QuoteConfigApi.GroundAgentRuleSaveParams>({
  groupPackagePrice: 0,
  maxPeople: 10,
  minPeople: 1,
  status: 'active',
});
const approvalForm = reactive<QuoteConfigApi.ApprovalConfig>({
  approvalMode: 'specified_person',
  approvers: [],
  ccUsers: [],
});
const approvalApproverValues = ref<Array<{ label: string; value: number }>>([]);
const approvalCcValues = ref<Array<{ label: string; value: number }>>([]);

const resourceModalTitle = computed(() => (resourceEditingId.value ? '修改普通资源报价规则' : '新增普通资源报价规则'));
const guideLevelModalTitle = computed(() => (guideLevelEditingId.value ? '修改导游等级' : '新增导游等级'));
const guideRuleModalTitle = computed(() => (guideRuleEditingId.value ? '修改导游报价规则' : '新增导游报价规则'));
const groundModalTitle = computed(() => (groundEditingId.value ? '修改地接报价规则' : '新增地接报价规则'));

function pagination(): TablePaginationConfig {
  return { current: 1, pageSize: 10, showSizeChanger: true, total: 0 };
}

function statusLabel(status?: QuoteConfigApi.Status) {
  return status === 'disabled' ? '停用' : '启用';
}

function resourceTypeLabel(type?: QuoteConfigApi.ResourceType) {
  return resourceTypeOptions.find((item) => item.value === type)?.label || '-';
}

function resourceQuoteModeLabel(mode?: QuoteConfigApi.ResourceQuoteMode) {
  return resourceQuoteModeOptions.find((item) => item.value === mode)?.label || '两种方式都可以报价';
}

function allowsRateQuote(mode?: QuoteConfigApi.ResourceQuoteMode) {
  return mode !== 'fixed';
}

function allowsFixedQuote(mode?: QuoteConfigApi.ResourceQuoteMode) {
  return mode !== 'rate';
}

function money(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function rate(value?: number) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`;
}

function rateForForm(value?: number) {
  return Number((Number(value || 0) * 100).toFixed(2));
}

function rateForApi(value?: number) {
  return Number((Number(value || 0) / 100).toFixed(4));
}

function memberLabel(member: QuoteConfigApi.ApprovalMember) {
  return member.employeeName || member.username || `用户${member.systemUserId}`;
}

function employeeToOption(employee: EnterpriseEmployeeApi.Item): SelectOption<number> | undefined {
  if (!employee.systemUserId) return undefined;
  const name = employee.employeeName || employee.username;
  return {
    label: employee.username && employee.username !== name ? `${name}（${employee.username}）` : name,
    value: employee.systemUserId,
  };
}

async function loadBaseOptions() {
  const [categories, levels, employees] = await Promise.all([
    getCustomerCategoryAll(),
    getQuoteGuideLevelAll(),
    getEnterpriseEmployeePage({ page: 1, pageSize: 200, status: 'active' }),
  ]);
  customerCategoryOptions.value = categories.map((item: CustomerCategoryApi.CustomerCategory) => ({
    label: item.categoryName,
    value: item.id,
  }));
  guideLevelOptions.value = levels.map((item) => ({ label: item.levelName, value: item.id }));
  employeeOptions.value = employees.items.map(employeeToOption).filter(Boolean) as SelectOption<number>[];
}

async function loadResourceRules() {
  resourceLoading.value = true;
  try {
    const result = await getQuoteResourceRulePage(resourceQuery);
    resourceRows.value = result.items;
    Object.assign(resourcePagination, {
      current: resourceQuery.page,
      pageSize: resourceQuery.pageSize,
      total: result.total,
    });
  } finally {
    resourceLoading.value = false;
  }
}

async function loadGuideLevels() {
  guideLevelLoading.value = true;
  try {
    const result = await getQuoteGuideLevelPage(guideLevelQuery);
    guideLevelRows.value = result.items;
    Object.assign(guideLevelPagination, {
      current: guideLevelQuery.page,
      pageSize: guideLevelQuery.pageSize,
      total: result.total,
    });
    guideLevelOptions.value = (await getQuoteGuideLevelAll()).map((item) => ({
      label: item.levelName,
      value: item.id,
    }));
  } finally {
    guideLevelLoading.value = false;
  }
}

async function loadGuideRules() {
  guideRuleLoading.value = true;
  try {
    const result = await getQuoteGuideRulePage(guideRuleQuery);
    guideRuleRows.value = result.items;
    Object.assign(guideRulePagination, {
      current: guideRuleQuery.page,
      pageSize: guideRuleQuery.pageSize,
      total: result.total,
    });
  } finally {
    guideRuleLoading.value = false;
  }
}

async function loadGroundRules() {
  groundLoading.value = true;
  try {
    const result = await getQuoteGroundAgentRulePage(groundQuery);
    groundRows.value = result.items;
    Object.assign(groundPagination, {
      current: groundQuery.page,
      pageSize: groundQuery.pageSize,
      total: result.total,
    });
  } finally {
    groundLoading.value = false;
  }
}

async function loadApprovalConfig() {
  approvalLoading.value = true;
  try {
    const result = await getQuoteApprovalConfig();
    approvalForm.approvalMode = result.approvalMode || 'specified_person';
    approvalForm.approvers = result.approvers || [];
    approvalForm.ccUsers = result.ccUsers || [];
    mergeApprovalEmployeeOptions(result);
    approvalApproverValues.value = approvalForm.approvers.map((item) => ({
      label: memberLabel(item),
      value: item.systemUserId,
    }));
    approvalCcValues.value = approvalForm.ccUsers.map((item) => ({
      label: memberLabel(item),
      value: item.systemUserId,
    }));
  } finally {
    approvalLoading.value = false;
  }
}

function mergeApprovalEmployeeOptions(config: QuoteConfigApi.ApprovalConfig) {
  const merged = new Map(employeeOptions.value.map((item) => [item.value, item]));
  for (const member of [...(config.approvers || []), ...(config.ccUsers || [])]) {
    merged.set(member.systemUserId, { label: memberLabel(member), value: member.systemUserId });
  }
  employeeOptions.value = [...merged.values()];
}

function resetResourceForm() {
  Object.assign(resourceForm, {
    customerCategoryId: undefined,
    minimumFixedMarkup: 0,
    minimumMarkupRate: 0,
    quoteMode: 'both',
    remark: undefined,
    resourceType: 'scenic',
    status: 'active',
    suggestedFixedMarkup: 0,
    suggestedMarkupRate: 0,
  });
}

function openCreateResourceRule() {
  resourceEditingId.value = undefined;
  resetResourceForm();
  resourceModalOpen.value = true;
}

function openEditResourceRule(record: QuoteConfigApi.ResourceRule) {
  resourceEditingId.value = record.id;
  Object.assign(resourceForm, {
    customerCategoryId: record.customerCategoryId,
    minimumFixedMarkup: Number(record.minimumFixedMarkup || 0),
    minimumMarkupRate: rateForForm(record.minimumMarkupRate),
    quoteMode: record.quoteMode || 'both',
    remark: record.remark,
    resourceType: record.resourceType,
    status: record.status,
    suggestedFixedMarkup: Number(record.suggestedFixedMarkup || 0),
    suggestedMarkupRate: rateForForm(record.suggestedMarkupRate),
  });
  resourceModalOpen.value = true;
}

async function saveResourceRule() {
  saving.value = true;
  try {
    const payload = {
      ...resourceForm,
      minimumMarkupRate: rateForApi(resourceForm.minimumMarkupRate),
      suggestedMarkupRate: rateForApi(resourceForm.suggestedMarkupRate),
    };
    if (resourceEditingId.value) {
      await updateQuoteResourceRule(resourceEditingId.value, payload);
    } else {
      await createQuoteResourceRule(payload);
    }
    message.success('保存成功');
    resourceModalOpen.value = false;
    await loadResourceRules();
  } finally {
    saving.value = false;
  }
}

function resetGuideLevelForm() {
  Object.assign(guideLevelForm, { levelName: '', remark: undefined, sortOrder: 0, status: 'active' });
}

function openCreateGuideLevel() {
  guideLevelEditingId.value = undefined;
  resetGuideLevelForm();
  guideLevelModalOpen.value = true;
}

function openEditGuideLevel(record: QuoteConfigApi.GuideLevel) {
  guideLevelEditingId.value = record.id;
  Object.assign(guideLevelForm, {
    levelName: record.levelName,
    remark: record.remark,
    sortOrder: record.sortOrder,
    status: record.status,
  });
  guideLevelModalOpen.value = true;
}

async function saveGuideLevel() {
  saving.value = true;
  try {
    if (guideLevelEditingId.value) {
      await updateQuoteGuideLevel(guideLevelEditingId.value, guideLevelForm);
    } else {
      await createQuoteGuideLevel(guideLevelForm);
    }
    message.success('保存成功');
    guideLevelModalOpen.value = false;
    await loadGuideLevels();
  } finally {
    saving.value = false;
  }
}

function resetGuideRuleForm() {
  Object.assign(guideRuleForm, {
    baseDailyFee: 0,
    foreignLanguageDailyMarkup: 0,
    guideLevelId: undefined,
    language: '普通话',
    overtimeHourlyFee: 0,
    remark: undefined,
    status: 'active',
  });
}

function openCreateGuideRule() {
  guideRuleEditingId.value = undefined;
  resetGuideRuleForm();
  guideRuleModalOpen.value = true;
}

function openEditGuideRule(record: QuoteConfigApi.GuideRule) {
  guideRuleEditingId.value = record.id;
  Object.assign(guideRuleForm, {
    baseDailyFee: Number(record.baseDailyFee || 0),
    foreignLanguageDailyMarkup: Number(record.foreignLanguageDailyMarkup || 0),
    guideLevelId: record.guideLevelId,
    language: record.language,
    overtimeHourlyFee: Number(record.overtimeHourlyFee || 0),
    remark: record.remark,
    status: record.status,
  });
  guideRuleModalOpen.value = true;
}

async function saveGuideRule() {
  saving.value = true;
  try {
    if (guideRuleEditingId.value) {
      await updateQuoteGuideRule(guideRuleEditingId.value, guideRuleForm);
    } else {
      await createQuoteGuideRule(guideRuleForm);
    }
    message.success('保存成功');
    guideRuleModalOpen.value = false;
    await loadGuideRules();
  } finally {
    saving.value = false;
  }
}

function resetGroundForm() {
  Object.assign(groundForm, {
    groupPackagePrice: 0,
    maxPeople: 10,
    minPeople: 1,
    remark: undefined,
    status: 'active',
  });
}

function openCreateGroundRule() {
  groundEditingId.value = undefined;
  resetGroundForm();
  groundModalOpen.value = true;
}

function openEditGroundRule(record: QuoteConfigApi.GroundAgentRule) {
  groundEditingId.value = record.id;
  Object.assign(groundForm, {
    groupPackagePrice: Number(record.groupPackagePrice || 0),
    maxPeople: record.maxPeople,
    minPeople: record.minPeople,
    remark: record.remark,
    status: record.status,
  });
  groundModalOpen.value = true;
}

async function saveGroundRule() {
  saving.value = true;
  try {
    if (groundEditingId.value) {
      await updateQuoteGroundAgentRule(groundEditingId.value, groundForm);
    } else {
      await createQuoteGroundAgentRule(groundForm);
    }
    message.success('保存成功');
    groundModalOpen.value = false;
    await loadGroundRules();
  } finally {
    saving.value = false;
  }
}

async function deleteWithConfirm(type: string, id: number) {
  Modal.confirm({
    title: `删除${type}？`,
    content: '删除后不会影响历史报价快照，但不再用于新报价。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      if (type === '普通资源报价规则') await deleteQuoteResourceRule(id);
      if (type === '导游等级') await deleteQuoteGuideLevel(id);
      if (type === '导游报价规则') await deleteQuoteGuideRule(id);
      if (type === '地接报价规则') await deleteQuoteGroundAgentRule(id);
      message.success('删除成功');
      await refreshCurrentTab();
    },
  });
}

async function saveApproval() {
  saving.value = true;
  try {
    await saveQuoteApprovalConfig({
      approvalMode: approvalForm.approvalMode,
      approvers: approvalForm.approvalMode === 'specified_person'
        ? approvalApproverValues.value.map((item) => ({ systemUserId: item.value }))
        : [],
      ccUsers: approvalCcValues.value.map((item) => ({ systemUserId: item.value })),
    });
    message.success('保存成功');
    await loadApprovalConfig();
  } finally {
    saving.value = false;
  }
}

function handleTableChange(
  query: { page?: number; pageSize?: number },
  loader: () => Promise<void>,
  nextPagination: TablePaginationConfig,
) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loader();
}

function resetResourceQuery() {
  Object.assign(resourceQuery, { customerCategoryId: undefined, page: 1, resourceType: undefined, status: undefined });
  loadResourceRules();
}

function resetGuideLevelQuery() {
  Object.assign(guideLevelQuery, { keyword: undefined, page: 1, status: undefined });
  loadGuideLevels();
}

function resetGuideRuleQuery() {
  Object.assign(guideRuleQuery, { guideLevelId: undefined, page: 1, status: undefined });
  loadGuideRules();
}

function resetGroundQuery() {
  Object.assign(groundQuery, { page: 1, status: undefined });
  loadGroundRules();
}

async function refreshCurrentTab() {
  if (activeTab.value === 'resource') await loadResourceRules();
  if (activeTab.value === 'guide-level') await loadGuideLevels();
  if (activeTab.value === 'guide-rule') await loadGuideRules();
  if (activeTab.value === 'ground') await loadGroundRules();
  if (activeTab.value === 'approval') await loadApprovalConfig();
}

onMounted(async () => {
  await loadBaseOptions();
  await Promise.all([
    loadResourceRules(),
    loadGuideLevels(),
    loadGuideRules(),
    loadGroundRules(),
    loadApprovalConfig(),
  ]);
});
</script>

<template>
  <Page title="报价配置">
    <Card>
      <Tabs v-model:active-key="activeTab">
        <Tabs.TabPane key="resource" tab="普通资源报价">
          <BusinessSearchForm :model="resourceQuery" @create="openCreateResourceRule" @search="loadResourceRules" @reset="resetResourceQuery">
            <Form.Item label="资源类型">
              <Select v-model:value="resourceQuery.resourceType" allow-clear :options="resourceTypeOptions" />
            </Form.Item>
            <Form.Item label="客户等级">
              <Select v-model:value="resourceQuery.customerCategoryId" allow-clear :options="customerCategoryOptions" />
            </Form.Item>
            <Form.Item label="状态">
              <Select v-model:value="resourceQuery.status" allow-clear :options="statusOptions" />
            </Form.Item>
          </BusinessSearchForm>

          <Table :data-source="resourceRows" :loading="resourceLoading" :pagination="resourcePagination" row-key="id" size="small" @change="(page) => handleTableChange(resourceQuery, loadResourceRules, page)">
            <Table.Column title="资源类型" data-index="resourceType" width="110">
              <template #default="{ record }">{{ resourceTypeLabel(record.resourceType) }}</template>
            </Table.Column>
            <Table.Column title="客户等级" data-index="customerCategoryName" width="140">
              <template #default="{ record }">{{ record.customerCategoryName || '默认规则' }}</template>
            </Table.Column>
            <Table.Column title="报价方式" data-index="quoteMode" width="150">
              <template #default="{ record }">{{ resourceQuoteModeLabel(record.quoteMode) }}</template>
            </Table.Column>
            <Table.Column title="建议比例" data-index="suggestedMarkupRate" align="right" width="110">
              <template #default="{ record }">{{ allowsRateQuote(record.quoteMode) ? rate(record.suggestedMarkupRate) : '-' }}</template>
            </Table.Column>
            <Table.Column title="最低比例" data-index="minimumMarkupRate" align="right" width="110">
              <template #default="{ record }">{{ allowsRateQuote(record.quoteMode) ? rate(record.minimumMarkupRate) : '-' }}</template>
            </Table.Column>
            <Table.Column title="建议加价" data-index="suggestedFixedMarkup" align="right" width="120">
              <template #default="{ record }">{{ allowsFixedQuote(record.quoteMode) ? money(record.suggestedFixedMarkup) : '-' }}</template>
            </Table.Column>
            <Table.Column title="最低加价" data-index="minimumFixedMarkup" align="right" width="120">
              <template #default="{ record }">{{ allowsFixedQuote(record.quoteMode) ? money(record.minimumFixedMarkup) : '-' }}</template>
            </Table.Column>
            <Table.Column title="状态" data-index="status" width="90">
              <template #default="{ record }"><Tag :color="record.status === 'active' ? 'green' : 'default'">{{ statusLabel(record.status) }}</Tag></template>
            </Table.Column>
            <Table.Column title="操作" fixed="right" width="130">
              <template #default="{ record }">
                <Space>
                  <Button type="link" size="small" @click="openEditResourceRule(record)">编辑</Button>
                  <Button type="link" size="small" danger @click="deleteWithConfirm('普通资源报价规则', record.id)">删除</Button>
                </Space>
              </template>
            </Table.Column>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="guide-level" tab="导游等级">
          <BusinessSearchForm :model="guideLevelQuery" @create="openCreateGuideLevel" @search="loadGuideLevels" @reset="resetGuideLevelQuery">
            <Form.Item label="关键词">
              <Input v-model:value="guideLevelQuery.keyword" allow-clear placeholder="等级名称" />
            </Form.Item>
            <Form.Item label="状态">
              <Select v-model:value="guideLevelQuery.status" allow-clear :options="statusOptions" />
            </Form.Item>
          </BusinessSearchForm>
          <Table :data-source="guideLevelRows" :loading="guideLevelLoading" :pagination="guideLevelPagination" row-key="id" size="small" @change="(page) => handleTableChange(guideLevelQuery, loadGuideLevels, page)">
            <Table.Column title="等级名称" data-index="levelName" />
            <Table.Column title="排序" data-index="sortOrder" width="100" />
            <Table.Column title="状态" data-index="status" width="90">
              <template #default="{ record }"><Tag :color="record.status === 'active' ? 'green' : 'default'">{{ statusLabel(record.status) }}</Tag></template>
            </Table.Column>
            <Table.Column title="备注" data-index="remark" ellipsis />
            <Table.Column title="操作" fixed="right" width="130">
              <template #default="{ record }">
                <Space>
                  <Button type="link" size="small" @click="openEditGuideLevel(record)">编辑</Button>
                  <Button type="link" size="small" danger @click="deleteWithConfirm('导游等级', record.id)">删除</Button>
                </Space>
              </template>
            </Table.Column>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="guide-rule" tab="导游报价">
          <BusinessSearchForm :model="guideRuleQuery" @create="openCreateGuideRule" @search="loadGuideRules" @reset="resetGuideRuleQuery">
            <Form.Item label="导游等级">
              <Select v-model:value="guideRuleQuery.guideLevelId" allow-clear :options="guideLevelOptions" />
            </Form.Item>
            <Form.Item label="状态">
              <Select v-model:value="guideRuleQuery.status" allow-clear :options="statusOptions" />
            </Form.Item>
          </BusinessSearchForm>
          <Table :data-source="guideRuleRows" :loading="guideRuleLoading" :pagination="guideRulePagination" row-key="id" size="small" @change="(page) => handleTableChange(guideRuleQuery, loadGuideRules, page)">
            <Table.Column title="导游等级" data-index="guideLevelName" width="120" />
            <Table.Column title="语种" data-index="language" width="120" />
            <Table.Column title="基础导服费" data-index="baseDailyFee" align="right" width="130">
              <template #default="{ record }">{{ money(record.baseDailyFee) }}/天</template>
            </Table.Column>
            <Table.Column title="外语加价" data-index="foreignLanguageDailyMarkup" align="right" width="130">
              <template #default="{ record }">{{ money(record.foreignLanguageDailyMarkup) }}/天</template>
            </Table.Column>
            <Table.Column title="超时费" data-index="overtimeHourlyFee" align="right" width="120">
              <template #default="{ record }">{{ money(record.overtimeHourlyFee) }}/小时</template>
            </Table.Column>
            <Table.Column title="状态" data-index="status" width="90">
              <template #default="{ record }"><Tag :color="record.status === 'active' ? 'green' : 'default'">{{ statusLabel(record.status) }}</Tag></template>
            </Table.Column>
            <Table.Column title="操作" fixed="right" width="130">
              <template #default="{ record }">
                <Space>
                  <Button type="link" size="small" @click="openEditGuideRule(record)">编辑</Button>
                  <Button type="link" size="small" danger @click="deleteWithConfirm('导游报价规则', record.id)">删除</Button>
                </Space>
              </template>
            </Table.Column>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="ground" tab="地接报价">
          <BusinessSearchForm :model="groundQuery" @create="openCreateGroundRule" @search="loadGroundRules" @reset="resetGroundQuery">
            <Form.Item label="状态">
              <Select v-model:value="groundQuery.status" allow-clear :options="statusOptions" />
            </Form.Item>
          </BusinessSearchForm>
          <Table :data-source="groundRows" :loading="groundLoading" :pagination="groundPagination" row-key="id" size="small" @change="(page) => handleTableChange(groundQuery, loadGroundRules, page)">
            <Table.Column title="人数区间" width="150">
              <template #default="{ record }">{{ record.minPeople }}-{{ record.maxPeople }} 人</template>
            </Table.Column>
            <Table.Column title="整团打包价" data-index="groupPackagePrice" align="right" width="150">
              <template #default="{ record }">{{ money(record.groupPackagePrice) }}/团</template>
            </Table.Column>
            <Table.Column title="状态" data-index="status" width="90">
              <template #default="{ record }"><Tag :color="record.status === 'active' ? 'green' : 'default'">{{ statusLabel(record.status) }}</Tag></template>
            </Table.Column>
            <Table.Column title="备注" data-index="remark" ellipsis />
            <Table.Column title="操作" fixed="right" width="130">
              <template #default="{ record }">
                <Space>
                  <Button type="link" size="small" @click="openEditGroundRule(record)">编辑</Button>
                  <Button type="link" size="small" danger @click="deleteWithConfirm('地接报价规则', record.id)">删除</Button>
                </Space>
              </template>
            </Table.Column>
          </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="approval" tab="审批配置">
          <Form class="quote-approval-form" layout="vertical">
            <Form.Item label="审批方式" required>
              <Radio.Group v-model:value="approvalForm.approvalMode">
                <Radio v-for="option in approvalModeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </Radio>
              </Radio.Group>
            </Form.Item>
            <div v-if="approvalForm.approvalMode === 'department_manager'" class="quote-approval-help quote-approval-manager-hint">
              提交报价审批时，系统自动取当前登录账号所属部门的负责人；这里不需要设置审批级数，也不填写固定姓名。
            </div>
            <Form.Item v-else label="指定审批人员（按顺序）" required>
              <Select
                v-model:value="approvalApproverValues"
                mode="multiple"
                label-in-value
                :options="employeeOptions"
                :field-names="{ label: 'label', value: 'value' }"
                :loading="approvalLoading"
                placeholder="请选择具体审批人员"
              />
            </Form.Item>
            <Form.Item label="固定抄送人">
              <Select
                v-model:value="approvalCcValues"
                mode="multiple"
                label-in-value
                :options="employeeOptions"
                :field-names="{ label: 'label', value: 'value' }"
                :loading="approvalLoading"
                placeholder="请选择审批通过后固定抄送的人员"
              />
            </Form.Item>
            <Space>
              <Button type="primary" :loading="saving" @click="saveApproval">保存审批配置</Button>
            </Space>
          </Form>
        </Tabs.TabPane>
      </Tabs>
    </Card>

    <Modal v-model:open="resourceModalOpen" :confirm-loading="saving" :title="resourceModalTitle" width="760px" ok-text="保存" cancel-text="取消" @ok="saveResourceRule">
      <Form :model="resourceForm" layout="vertical">
        <div class="quote-form-grid">
          <Form.Item label="资源类型" required>
            <Select v-model:value="resourceForm.resourceType" :options="resourceTypeOptions" />
          </Form.Item>
          <Form.Item label="客户等级">
            <Select v-model:value="resourceForm.customerCategoryId" allow-clear :options="customerCategoryOptions" placeholder="不选表示默认规则" />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="resourceForm.status" :options="statusOptions" />
          </Form.Item>
          <Form.Item class="quote-form-grid-span-all" label="报价方式" required>
            <Radio.Group v-model:value="resourceForm.quoteMode" class="quote-mode-radio-group">
              <Radio v-for="option in resourceQuoteModeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item label="建议比例上浮">
            <InputNumber v-model:value="resourceForm.suggestedMarkupRate" :min="0" :step="0.01" addon-after="%" style="width: 100%" />
          </Form.Item>
          <Form.Item label="最低比例上浮">
            <InputNumber v-model:value="resourceForm.minimumMarkupRate" :min="0" :step="0.01" addon-after="%" style="width: 100%" />
          </Form.Item>
          <Form.Item label="建议固定加价">
            <InputNumber v-model:value="resourceForm.suggestedFixedMarkup" :min="0" addon-before="¥" style="width: 100%" />
          </Form.Item>
          <Form.Item label="最低固定加价">
            <InputNumber v-model:value="resourceForm.minimumFixedMarkup" :min="0" addon-before="¥" style="width: 100%" />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Textarea v-model:value="resourceForm.remark" :rows="3" :maxlength="1000" />
        </Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="guideLevelModalOpen" :confirm-loading="saving" :title="guideLevelModalTitle" width="460px" ok-text="保存" cancel-text="取消" @ok="saveGuideLevel">
      <Form :model="guideLevelForm" layout="vertical">
        <Form.Item label="等级名称" required>
          <Input v-model:value="guideLevelForm.levelName" :maxlength="80" />
        </Form.Item>
        <Form.Item label="排序">
          <InputNumber v-model:value="guideLevelForm.sortOrder" :min="0" style="width: 100%" />
        </Form.Item>
        <Form.Item label="状态">
          <Select v-model:value="guideLevelForm.status" :options="statusOptions" />
        </Form.Item>
        <Form.Item label="备注">
          <Textarea v-model:value="guideLevelForm.remark" :rows="3" :maxlength="1000" />
        </Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="guideRuleModalOpen" :confirm-loading="saving" :title="guideRuleModalTitle" width="760px" ok-text="保存" cancel-text="取消" @ok="saveGuideRule">
      <Form :model="guideRuleForm" layout="vertical">
        <div class="quote-form-grid">
          <Form.Item label="导游等级" required>
            <Select v-model:value="guideRuleForm.guideLevelId" :options="guideLevelOptions" />
          </Form.Item>
          <Form.Item label="服务语种">
            <Input v-model:value="guideRuleForm.language" :maxlength="80" placeholder="普通话 / 英语 / 日语" />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="guideRuleForm.status" :options="statusOptions" />
          </Form.Item>
          <Form.Item label="基础导服费">
            <InputNumber v-model:value="guideRuleForm.baseDailyFee" :min="0" addon-before="¥" addon-after="/天" style="width: 100%" />
          </Form.Item>
          <Form.Item label="外语服务加价">
            <InputNumber v-model:value="guideRuleForm.foreignLanguageDailyMarkup" :min="0" addon-before="¥" addon-after="/天" style="width: 100%" />
          </Form.Item>
          <Form.Item label="超时费">
            <InputNumber v-model:value="guideRuleForm.overtimeHourlyFee" :min="0" addon-before="¥" addon-after="/小时" style="width: 100%" />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Textarea v-model:value="guideRuleForm.remark" :rows="3" :maxlength="1000" />
        </Form.Item>
      </Form>
    </Modal>

    <Modal v-model:open="groundModalOpen" :confirm-loading="saving" :title="groundModalTitle" width="560px" ok-text="保存" cancel-text="取消" @ok="saveGroundRule">
      <Form :model="groundForm" layout="vertical">
        <div class="quote-form-grid two">
          <Form.Item label="最小人数" required>
            <InputNumber v-model:value="groundForm.minPeople" :min="1" addon-after="人" style="width: 100%" />
          </Form.Item>
          <Form.Item label="最大人数" required>
            <InputNumber v-model:value="groundForm.maxPeople" :min="1" addon-after="人" style="width: 100%" />
          </Form.Item>
          <Form.Item label="整团打包价">
            <InputNumber v-model:value="groundForm.groupPackagePrice" :min="0" addon-before="¥" addon-after="/团" style="width: 100%" />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="groundForm.status" :options="statusOptions" />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Textarea v-model:value="groundForm.remark" :rows="3" :maxlength="1000" />
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
.quote-form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
}

.quote-form-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.quote-form-grid-span-all {
  grid-column: 1 / -1;
}

.quote-mode-radio-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 24px;
  min-height: 32px;
}

.quote-mode-radio-group :deep(.ant-radio-wrapper) {
  margin-inline-end: 0;
  color: #262626;
  font-weight: 400;
}

.quote-approval-form {
  max-width: 760px;
}

.quote-approval-help {
  margin-top: 6px;
  color: #8c8c8c;
  font-size: 12px;
}

.quote-approval-manager-hint {
  margin: -4px 0 20px;
}
</style>
