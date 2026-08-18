<script lang="ts" setup>
import type {
  TableColumnsType,
  TablePaginationConfig,
  UploadProps,
} from 'ant-design-vue';

import { Page } from '@vben/common-ui';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Upload,
  message,
} from 'ant-design-vue';
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { downloadAttachment, uploadAttachment } from '#/api/common/attachment';
import { getQuoteGuideLevelAll } from '#/api/configuration/quote';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import { getEnterpriseEmployeeAll, type EnterpriseEmployeeApi } from '#/api/enterprise/employee';
import {
  createEnterpriseGuide,
  createEnterpriseGuideTag,
  deleteEnterpriseGuide,
  deleteEnterpriseGuideTag,
  disableEnterpriseGuide,
  getEnterpriseGuidePage,
  getEnterpriseGuideTagAll,
  getEnterpriseGuideTagPage,
  sendEnterpriseGuideCodeInvite,
  updateEnterpriseGuide,
  updateEnterpriseGuideTag,
  type EnterpriseGuideApi,
} from '#/api/enterprise/guide';

import {
  createGuideUploadFileList,
  guideUploadBackendUrl,
  isGuidePreviewImage,
  type GuideUploadFile,
} from './guide-upload-preview';

type GuideRow = EnterpriseGuideApi.Item;
type GuideTagRow = EnterpriseGuideApi.TagItem;

const guideColumns: TableColumnsType<GuideRow> = [
  { title: '导游名称', dataIndex: 'guideName', key: 'guideName', width: 130 },
  { title: '所属导管', dataIndex: 'guideManagerName', key: 'guideManagerName', width: 120 },
  { title: '导游等级', dataIndex: 'guideLevelName', key: 'guideLevelName', width: 110 },
  { title: '标签', key: 'tags', width: 220 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 80 },
  { title: '证件号', dataIndex: 'certificateNo', key: 'certificateNo', width: 150 },
  { title: '手机', dataIndex: 'mobilePhone', key: 'mobilePhone', width: 130 },
  { title: '收款账号', key: 'account', width: 230 },
  { title: '企业码状态', dataIndex: 'enterpriseCodeStatus', key: 'enterpriseCodeStatus', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', fixed: 'right', width: 230 },
];

const tagColumns: TableColumnsType<GuideTagRow> = [
  { title: '标签名称', dataIndex: 'tagName', key: 'tagName', width: 180 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 260 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const genderOptions = [
  { label: '男', value: 'male' },
  { label: '女', value: 'female' },
  { label: '未填写', value: 'unknown' },
];

const enterpriseCodeStatusOptions = [
  { label: '未加入企业码', value: 'not_joined' },
  { label: '已获取签约链接', value: 'invite_link' },
  { label: '已签约成功', value: 'signed_success' },
  { label: '已绑定', value: 'bound' },
  { label: '未绑定', value: 'unbound' },
  { label: '停用', value: 'disabled' },
];

const activeTab = ref('guides');
const guides = ref<GuideRow[]>([]);
const guideTags = ref<GuideTagRow[]>([]);
const employees = ref<EnterpriseEmployeeApi.Item[]>([]);
const guideLevelOptions = ref<Array<{ label: string; value: number }>>([]);
const loading = ref(false);
const tagLoading = ref(false);
const modalOpen = ref(false);
const tagModalOpen = ref(false);
const saving = ref(false);
const tagSaving = ref(false);
const certificateUploading = ref(false);
const photoUploading = ref(false);
const editingId = ref<number>();
const tagEditingId = ref<number>();
const certificateFileList = ref<GuideUploadFile[]>([]);
const photoFileList = ref<GuideUploadFile[]>([]);
const filePreviewOpen = ref(false);
const filePreviewTitle = ref('');
const filePreviewUrl = ref('');
let filePreviewObjectUrl = '';

const query = reactive<EnterpriseGuideApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const tagQuery = reactive<EnterpriseGuideApi.TagQueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<EnterpriseGuideApi.SaveParams>({
  alipayAccount: '',
  alipayName: '',
  bankAccountNo: '',
  bankName: '',
  certificateNo: '',
  enterpriseCodeAccount: '',
  enterpriseCodeStatus: 'not_joined',
  fax: '',
  gender: 'unknown',
  guideCode: '',
  guideLevelId: undefined,
  guideName: '',
  idCardNo: '',
  languages: '',
  mobilePhone: '',
  nativePlace: '',
  personalIntro: '',
  rating: 0,
  remark: '',
  sortOrder: 0,
  status: 'active',
  tagIds: [],
  telephone: '',
  totalTours: 0,
  username: '',
});

const tagFormState = reactive<EnterpriseGuideApi.TagSaveParams>({
  remark: '',
  sortOrder: 0,
  status: 'active',
  tagName: '',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const tagPagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const employeeOptions = computed(() =>
  employees.value.map((item) => ({
    label: item.departmentName
      ? `${item.employeeName}（${item.departmentName}）`
      : item.employeeName,
    value: item.id,
  })),
);

const tagOptions = computed(() =>
  guideTags.value
    .filter((item) => item.status === 'active')
    .map((item) => ({
      label: item.tagName,
      value: item.id,
    })),
);

async function loadOptions() {
  const [employeeList, tagList, guideLevels] = await Promise.all([
    getEnterpriseEmployeeAll(true),
    getEnterpriseGuideTagAll(),
    getQuoteGuideLevelAll(),
  ]);
  employees.value = employeeList;
  guideTags.value = tagList;
  guideLevelOptions.value = guideLevels.map((item) => ({
    label: item.levelName,
    value: item.id,
  }));
}

async function loadGuides() {
  loading.value = true;
  try {
    const result = await getEnterpriseGuidePage(query);
    guides.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

async function loadTags() {
  tagLoading.value = true;
  try {
    const result = await getEnterpriseGuideTagPage(tagQuery);
    guideTags.value = result.items;
    tagPagination.current = tagQuery.page;
    tagPagination.pageSize = tagQuery.pageSize;
    tagPagination.total = result.total;
  } finally {
    tagLoading.value = false;
  }
}

function selectTab(key: number | string) {
  const nextKey = String(key);
  activeTab.value = nextKey;
  if (nextKey === 'tags') {
    loadTags();
  } else {
    Promise.all([loadOptions(), loadGuides()]);
  }
}

function handleSearch() {
  query.page = 1;
  loadGuides();
}

function resetQuery() {
  Object.assign(query, {
    enterpriseCodeStatus: undefined,
    guideManagerEmployeeId: undefined,
    keyword: undefined,
    page: 1,
    pageSize: query.pageSize,
    status: undefined,
    tagId: undefined,
  });
  loadGuides();
}

function handleTagSearch() {
  tagQuery.page = 1;
  loadTags();
}

function resetTagQuery() {
  Object.assign(tagQuery, {
    keyword: undefined,
    page: 1,
    pageSize: tagQuery.pageSize,
    status: undefined,
  });
  loadTags();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadGuides();
}

function handleTagTableChange(nextPagination: TablePaginationConfig) {
  tagQuery.page = Number(nextPagination.current || 1);
  tagQuery.pageSize = Number(nextPagination.pageSize || 10);
  loadTags();
}

function resetForm() {
  Object.assign(formState, {
    age: undefined,
    alipayAccount: '',
    alipayName: '',
    bankAccountNo: '',
    bankName: '',
    certificateFileUrl: '',
    certificateNo: '',
    enterpriseCodeAccount: '',
    enterpriseCodeStatus: 'not_joined',
    fax: '',
    gender: 'unknown',
    guideCode: '',
    guideLevelId: undefined,
    guideManagerEmployeeId: undefined,
    guideName: '',
    idCardNo: '',
    languages: '',
    mobilePhone: '',
    nativePlace: '',
    personalIntro: '',
    photoUrl: '',
    rating: 0,
    remark: '',
    sortOrder: 0,
    status: 'active',
    tagIds: [],
    telephone: '',
    totalTours: 0,
    username: '',
    workingYears: undefined,
  });
  certificateFileList.value = [];
  photoFileList.value = [];
}

function resetTagForm() {
  Object.assign(tagFormState, {
    remark: '',
    sortOrder: 0,
    status: 'active',
    tagName: '',
  });
}

async function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  await loadOptions();
  modalOpen.value = true;
}

async function openEditModal(record: GuideRow) {
  editingId.value = record.id;
  Object.assign(formState, {
    age: record.age,
    alipayAccount: record.alipayAccount || '',
    alipayName: record.alipayName || '',
    bankAccountNo: record.bankAccountNo || '',
    bankName: record.bankName || '',
    certificateFileUrl: record.certificateFileUrl || '',
    certificateNo: record.certificateNo || '',
    enterpriseCodeAccount: record.enterpriseCodeAccount || '',
    enterpriseCodeStatus: record.enterpriseCodeStatus || 'not_joined',
    fax: record.fax || '',
    gender: record.gender,
    guideCode: record.guideCode || '',
    guideLevelId: record.guideLevelId,
    guideManagerEmployeeId: record.guideManagerEmployeeId,
    guideName: record.guideName,
    idCardNo: record.idCardNo || '',
    languages: record.languages || '',
    mobilePhone: record.mobilePhone || '',
    nativePlace: record.nativePlace || '',
    personalIntro: record.personalIntro || '',
    photoUrl: record.photoUrl || '',
    rating: Number(record.rating || 0),
    remark: record.remark || '',
    sortOrder: record.sortOrder ?? 0,
    status: record.status,
    tagIds: record.tagIds || [],
    telephone: record.telephone || '',
    totalTours: record.totalTours ?? 0,
    username: record.username || '',
    workingYears: record.workingYears,
  });
  certificateFileList.value = createGuideUploadFileList(record.certificateFileUrl, '导游证书');
  photoFileList.value = createGuideUploadFileList(record.photoUrl, '个人照片');
  await loadOptions();
  modalOpen.value = true;
}

function openCreateTagModal() {
  tagEditingId.value = undefined;
  resetTagForm();
  tagModalOpen.value = true;
}

function openEditTagModal(record: GuideTagRow) {
  tagEditingId.value = record.id;
  Object.assign(tagFormState, {
    remark: record.remark || '',
    sortOrder: record.sortOrder ?? 0,
    status: record.status,
    tagName: record.tagName,
  });
  tagModalOpen.value = true;
}

function clean(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

function buildSaveParams(): EnterpriseGuideApi.SaveParams {
  return {
    age: formState.age,
    alipayAccount: clean(formState.alipayAccount),
    alipayName: clean(formState.alipayName),
    bankAccountNo: clean(formState.bankAccountNo),
    bankName: clean(formState.bankName),
    certificateFileUrl: clean(formState.certificateFileUrl),
    certificateNo: clean(formState.certificateNo),
    enterpriseCodeAccount: clean(formState.enterpriseCodeAccount),
    enterpriseCodeStatus: formState.enterpriseCodeStatus,
    fax: clean(formState.fax),
    gender: formState.gender,
    guideCode: clean(formState.guideCode),
    guideLevelId: formState.guideLevelId,
    guideManagerEmployeeId: formState.guideManagerEmployeeId,
    guideName: formState.guideName.trim(),
    idCardNo: clean(formState.idCardNo),
    languages: clean(formState.languages),
    mobilePhone: clean(formState.mobilePhone),
    nativePlace: clean(formState.nativePlace),
    personalIntro: clean(formState.personalIntro),
    photoUrl: clean(formState.photoUrl),
    rating: Number(formState.rating || 0),
    remark: clean(formState.remark),
    sortOrder: Number(formState.sortOrder || 0),
    status: formState.status,
    tagIds: formState.tagIds || [],
    telephone: clean(formState.telephone),
    totalTours: Number(formState.totalTours || 0),
    username: clean(formState.username),
    workingYears: formState.workingYears,
  };
}

function buildTagPayload(): EnterpriseGuideApi.TagSaveParams {
  return {
    remark: clean(tagFormState.remark),
    sortOrder: Number(tagFormState.sortOrder || 0),
    status: tagFormState.status,
    tagName: tagFormState.tagName.trim(),
  };
}

async function saveGuide() {
  if (!formState.guideName?.trim()) {
    message.warning('请填写导游名称');
    return;
  }
  if (Number(formState.rating || 0) < 0 || Number(formState.rating || 0) > 5) {
    message.warning('导游评分必须在 0 到 5 之间');
    return;
  }

  saving.value = true;
  try {
    const params = buildSaveParams();
    if (editingId.value) {
      await updateEnterpriseGuide(editingId.value, params);
      message.success('导游档案已更新');
    } else {
      await createEnterpriseGuide(params);
      message.success('导游档案已新增');
    }
    modalOpen.value = false;
    await loadGuides();
  } finally {
    saving.value = false;
  }
}

function isAllowedGuideCertificate(file: File) {
  return /\.(pdf|jpg|jpeg|png)$/i.test(file.name);
}

function isAllowedGuidePhoto(file: File) {
  return /\.(jpg|jpeg|png)$/i.test(file.name);
}

async function uploadGuideFile(
  file: File,
  type: 'guide_certificate' | 'guide_photo',
) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('businessModule', 'enterprise');
  formData.append('businessType', type);
  if (editingId.value) {
    formData.append('businessId', String(editingId.value));
  }
  return await uploadAttachment(formData);
}

const beforeUploadGuideCertificate: UploadProps['beforeUpload'] = async (file) => {
  const rawFile = file as File;
  if (!isAllowedGuideCertificate(rawFile)) {
    message.warning('导游证书仅支持 PDF、JPG、PNG');
    return false;
  }
  certificateUploading.value = true;
  try {
    const attachment = await uploadGuideFile(rawFile, 'guide_certificate');
    formState.certificateFileUrl = attachment.fileUrl;
    certificateFileList.value = createGuideUploadFileList(attachment.fileUrl, attachment.originalFilename);
    message.success('导游证书已上传');
  } finally {
    certificateUploading.value = false;
  }
  return false;
};

const beforeUploadGuidePhoto: UploadProps['beforeUpload'] = async (file) => {
  const rawFile = file as File;
  if (!isAllowedGuidePhoto(rawFile)) {
    message.warning('个人照片仅支持 JPG、PNG');
    return false;
  }
  photoUploading.value = true;
  try {
    const attachment = await uploadGuideFile(rawFile, 'guide_photo');
    formState.photoUrl = attachment.fileUrl;
    photoFileList.value = createGuideUploadFileList(attachment.fileUrl, attachment.originalFilename);
    message.success('个人照片已上传');
  } finally {
    photoUploading.value = false;
  }
  return false;
};

const removeGuideCertificate: UploadProps['onRemove'] = () => {
  formState.certificateFileUrl = '';
  certificateFileList.value = [];
  return true;
};

const removeGuidePhoto: UploadProps['onRemove'] = () => {
  formState.photoUrl = '';
  photoFileList.value = [];
  return true;
};

function revokeGuideFilePreview() {
  if (filePreviewObjectUrl) {
    URL.revokeObjectURL(filePreviewObjectUrl);
    filePreviewObjectUrl = '';
  }
}

function closeGuideFilePreview() {
  filePreviewOpen.value = false;
  filePreviewTitle.value = '';
  filePreviewUrl.value = '';
  revokeGuideFilePreview();
}

async function previewGuideFile(file: GuideUploadFile, fallbackUrl?: string) {
  const backendFileUrl = guideUploadBackendUrl(file, fallbackUrl);
  if (!backendFileUrl) {
    message.warning('附件地址为空');
    return;
  }

  const blob = await downloadAttachment(backendFileUrl);
  const objectUrl = URL.createObjectURL(blob);
  const filename = file.name || backendFileUrl;
  if (isGuidePreviewImage(filename, blob.type)) {
    revokeGuideFilePreview();
    filePreviewObjectUrl = objectUrl;
    filePreviewTitle.value = filename;
    filePreviewUrl.value = objectUrl;
    filePreviewOpen.value = true;
    return;
  }

  window.open(objectUrl, '_blank', 'noopener,noreferrer');
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 30_000);
}

const previewGuideCertificate: UploadProps['onPreview'] = async (file) => {
  await previewGuideFile(file as GuideUploadFile, formState.certificateFileUrl);
};

const previewGuidePhoto: UploadProps['onPreview'] = async (file) => {
  await previewGuideFile(file as GuideUploadFile, formState.photoUrl);
};

async function saveTag() {
  if (!tagFormState.tagName?.trim()) {
    message.warning('请填写标签名称');
    return;
  }
  tagSaving.value = true;
  try {
    const payload = buildTagPayload();
    if (tagEditingId.value) {
      await updateEnterpriseGuideTag(tagEditingId.value, payload);
      message.success('导游标签已更新');
    } else {
      await createEnterpriseGuideTag(payload);
      message.success('导游标签已新增');
    }
    tagModalOpen.value = false;
    await Promise.all([loadTags(), loadOptions()]);
  } finally {
    tagSaving.value = false;
  }
}

function confirmDisable(record: GuideRow) {
  Modal.confirm({
    title: `停用导游「${record.guideName}」？`,
    content: '停用后不再进入新团队安排选择，历史排团、报账和结算记录仍保留引用。',
    okText: '停用',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await disableEnterpriseGuide(record.id);
      message.success('导游已停用');
      await loadGuides();
    },
  });
}

function confirmDelete(record: GuideRow) {
  Modal.confirm({
    title: `删除导游「${record.guideName}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除，历史业务记录仍可保留引用。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteEnterpriseGuide(record.id);
      message.success('导游已删除');
      await loadGuides();
    },
  });
}

function confirmDeleteTag(record: GuideTagRow) {
  Modal.confirm({
    title: `删除标签「${record.tagName}」？`,
    content: '删除后不会物理移除记录，已绑定导游的历史关系也会保留软删除痕迹。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteEnterpriseGuideTag(record.id);
      message.success('导游标签已删除');
      await Promise.all([loadTags(), loadOptions()]);
    },
  });
}

function confirmInvite(record: GuideRow) {
  Modal.confirm({
    title: `发送「${record.guideName}」企业码邀请？`,
    content: '当前版本会记录为已获取签约链接，真实企业码发送接口后续再对接。',
    okText: '发送邀请',
    cancelText: '取消',
    async onOk() {
      await sendEnterpriseGuideCodeInvite(record.id);
      message.success('企业码邀请状态已更新');
      await loadGuides();
    },
  });
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').replace(/\.\d+.*/, '').slice(0, 19);
}

function statusLabel(status?: EnterpriseGuideApi.Status | EnterpriseGuideApi.TagStatus) {
  return status === 'disabled' ? '停用' : '启用';
}

function genderLabel(gender?: EnterpriseGuideApi.Gender) {
  const map = { female: '女', male: '男', unknown: '-' };
  return map[gender || 'unknown'] || '-';
}

function enterpriseCodeStatusLabel(status?: EnterpriseGuideApi.EnterpriseCodeStatus) {
  const map: Record<string, string> = {
    bound: '已绑定',
    disabled: '停用',
    invite_link: '已获取签约链接',
    not_joined: '未加入企业码',
    signed_success: '已签约成功',
    unbound: '未绑定',
  };
  return map[status || 'not_joined'] || '-';
}

function enterpriseCodeStatusColor(status?: EnterpriseGuideApi.EnterpriseCodeStatus) {
  const map: Record<string, string> = {
    bound: 'blue',
    disabled: 'default',
    invite_link: 'orange',
    not_joined: 'default',
    signed_success: 'green',
    unbound: 'orange',
  };
  return map[status || 'not_joined'] || 'default';
}

function accountSummary(record: GuideRow) {
  const bank = [record.bankName, record.bankAccountNo].filter(Boolean).join(' ');
  const alipay = [record.alipayName, record.alipayAccount].filter(Boolean).join(' ');
  return `银行:${bank || '-'} / 支付宝:${alipay || '-'}`;
}

function tagNames(record: GuideRow) {
  return record.tags?.map((tag) => tag.tagName).join('、') || '';
}

function tagNamesText(record: Record<string, any>) {
  return tagNames(record as GuideRow);
}

function accountSummaryText(record: Record<string, any>) {
  return accountSummary(record as GuideRow);
}

function handleEdit(record: Record<string, any>) {
  openEditModal(record as GuideRow);
}

function handleInvite(record: Record<string, any>) {
  confirmInvite(record as GuideRow);
}

function handleDisable(record: Record<string, any>) {
  confirmDisable(record as GuideRow);
}

function handleDelete(record: Record<string, any>) {
  confirmDelete(record as GuideRow);
}

function handleEditTag(record: Record<string, any>) {
  openEditTagModal(record as GuideTagRow);
}

function handleDeleteTag(record: Record<string, any>) {
  confirmDeleteTag(record as GuideTagRow);
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadGuides()]);
});

onBeforeUnmount(() => {
  revokeGuideFilePreview();
});
</script>

<template>
  <Page title="导游管理" description="维护导游档案、所属导管、导游标签、企业码状态和结算资料">
    <Card>
      <Tabs v-model:active-key="activeTab" class="guide-tabs" @change="selectTab">
        <Tabs.TabPane key="guides" tab="导游档案">
        <BusinessSearchForm
          label-width="88px"
          :model="query"
          :search-loading="loading"
          create-text="新增"
          @create="openCreateModal"
          @reset="resetQuery"
          @search="handleSearch"
        >
          <Form.Item label="关键词">
            <Input
              v-model:value="query.keyword"
              allow-clear
              placeholder="导游名称 / 用户名 / 证件号 / 电话"
              @press-enter="handleSearch"
            />
          </Form.Item>
          <Form.Item label="所属导管">
            <Select
              v-model:value="query.guideManagerEmployeeId"
              allow-clear
              :options="employeeOptions"
              placeholder="请选择导管"
            />
          </Form.Item>
          <Form.Item label="导游标签">
            <Select
              v-model:value="query.tagId"
              allow-clear
              :options="tagOptions"
              placeholder="请选择标签"
            />
          </Form.Item>
          <Form.Item label="企业码状态">
            <Select
              v-model:value="query.enterpriseCodeStatus"
              allow-clear
              :options="enterpriseCodeStatusOptions"
              placeholder="请选择企业码状态"
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
        </BusinessSearchForm>

        <Table
          :columns="guideColumns"
          :data-source="guides"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          :scroll="{ x: 1950 }"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'guideManagerName'">
              {{ record.guideManagerName || '-' }}
            </template>
            <template v-else-if="column.key === 'guideLevelName'">
              {{ record.guideLevelName || '-' }}
            </template>
            <template v-else-if="column.key === 'tags'">
              <Tooltip :title="tagNamesText(record)">
                <Space :size="[4, 4]" wrap>
                  <Tag v-for="tag in record.tags" :key="tag.id" color="blue">
                    {{ tag.tagName }}
                  </Tag>
                  <span v-if="!record.tags?.length">-</span>
                </Space>
              </Tooltip>
            </template>
            <template v-else-if="column.key === 'username'">
              {{ record.username || '-' }}
            </template>
            <template v-else-if="column.key === 'gender'">
              {{ genderLabel(record.gender) }}
            </template>
            <template v-else-if="column.key === 'certificateNo'">
              {{ record.certificateNo || '-' }}
            </template>
            <template v-else-if="column.key === 'mobilePhone'">
              {{ record.mobilePhone || '-' }}
            </template>
            <template v-else-if="column.key === 'account'">
              <Tooltip :title="accountSummaryText(record)">
                <span class="guide-ellipsis">{{ accountSummaryText(record) }}</span>
              </Tooltip>
            </template>
            <template v-else-if="column.key === 'enterpriseCodeStatus'">
              <Tag :color="enterpriseCodeStatusColor(record.enterpriseCodeStatus)">
                {{ enterpriseCodeStatusLabel(record.enterpriseCodeStatus) }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="record.status === 'active' ? 'green' : 'default'">
                {{ statusLabel(record.status) }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'createdAt'">
              {{ formatDateTime(record.createdAt) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <Space>
                <Button type="link" size="small" @click="handleEdit(record)">
                  编辑
                </Button>
                <Button type="link" size="small" @click="handleInvite(record)">
                  企业码邀请
                </Button>
                <Button
                  v-if="record.status === 'active'"
                  type="link"
                  size="small"
                  danger
                  @click="handleDisable(record)"
                >
                  停用
                </Button>
                <Button type="link" size="small" danger @click="handleDelete(record)">
                  删除
                </Button>
              </Space>
            </template>
          </template>
        </Table>
        </Tabs.TabPane>

        <Tabs.TabPane key="tags" tab="标签管理">
        <BusinessSearchForm
          label-width="78px"
          :model="tagQuery"
          :search-loading="tagLoading"
          create-text="新增"
          @create="openCreateTagModal"
          @reset="resetTagQuery"
          @search="handleTagSearch"
        >
          <Form.Item label="标签名称">
            <Input
              v-model:value="tagQuery.keyword"
              allow-clear
              placeholder="请输入标签名称"
              @press-enter="handleTagSearch"
            />
          </Form.Item>
          <Form.Item label="状态">
            <Select
              v-model:value="tagQuery.status"
              allow-clear
              :options="statusOptions"
              placeholder="请选择状态"
            />
          </Form.Item>
        </BusinessSearchForm>

        <Table
          :columns="tagColumns"
          :data-source="guideTags"
          :loading="tagLoading"
          :pagination="tagPagination"
          row-key="id"
          :scroll="{ x: 980 }"
          @change="handleTagTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'remark'">
              {{ record.remark || '-' }}
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="record.status === 'active' ? 'green' : 'default'">
                {{ statusLabel(record.status) }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'createdAt'">
              {{ formatDateTime(record.createdAt) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <Space>
                <Button type="link" size="small" @click="handleEditTag(record)">
                  编辑
                </Button>
                <Button type="link" size="small" danger @click="handleDeleteTag(record)">
                  删除
                </Button>
              </Space>
            </template>
          </template>
        </Table>
        </Tabs.TabPane>
      </Tabs>
    </Card>

    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="editingId ? '编辑导游' : '新增导游'"
      ok-text="保存"
      cancel-text="取消"
      width="1080px"
      @ok="saveGuide"
    >
      <Form :model="formState" layout="vertical">
        <div class="guide-section-title">基础信息</div>
        <div class="guide-form-row">
          <Form.Item label="导游名称" required>
            <Input v-model:value="formState.guideName" :maxlength="80" placeholder="请输入导游姓名" />
          </Form.Item>
          <Form.Item label="导游编码">
            <Input v-model:value="formState.guideCode" :maxlength="80" placeholder="例如：GD001" />
          </Form.Item>
          <Form.Item label="用户名">
            <Input v-model:value="formState.username" :maxlength="80" placeholder="导游端用户名" />
          </Form.Item>
        </div>
        <div class="guide-form-row">
          <Form.Item label="所属导管">
            <Select
              v-model:value="formState.guideManagerEmployeeId"
              allow-clear
              :options="employeeOptions"
              placeholder="请选择负责导管"
            />
          </Form.Item>
          <Form.Item label="导游等级">
            <Select
              v-model:value="formState.guideLevelId"
              allow-clear
              :options="guideLevelOptions"
              placeholder="请选择参与报价的等级"
            />
          </Form.Item>
          <Form.Item label="导游标签">
            <Select
              v-model:value="formState.tagIds"
              mode="multiple"
              allow-clear
              :options="tagOptions"
              placeholder="请选择导游标签"
            />
          </Form.Item>
        </div>
        <div class="guide-form-row">
          <Form.Item label="性别">
            <Select v-model:value="formState.gender" :options="genderOptions" />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="formState.status" :options="statusOptions" />
          </Form.Item>
          <Form.Item label="排序">
            <InputNumber v-model:value="formState.sortOrder" :min="0" style="width: 100%" />
          </Form.Item>
        </div>

        <div class="guide-section-title">证件与联系方式</div>
        <div class="guide-form-row">
          <Form.Item label="导游证号">
            <Input v-model:value="formState.certificateNo" :maxlength="120" placeholder="请输入导游证号" />
          </Form.Item>
          <Form.Item label="身份证号">
            <Input v-model:value="formState.idCardNo" :maxlength="120" placeholder="请输入身份证号" />
          </Form.Item>
          <Form.Item label="手机">
            <Input v-model:value="formState.mobilePhone" :maxlength="40" placeholder="请输入手机号" />
          </Form.Item>
        </div>
        <div class="guide-form-row">
          <Form.Item label="固定电话">
            <Input v-model:value="formState.telephone" :maxlength="40" placeholder="请输入固定电话" />
          </Form.Item>
          <Form.Item label="传真">
            <Input v-model:value="formState.fax" :maxlength="40" placeholder="请输入传真号码" />
          </Form.Item>
          <Form.Item label="企业码状态">
            <Select v-model:value="formState.enterpriseCodeStatus" :options="enterpriseCodeStatusOptions" />
          </Form.Item>
        </div>

        <div class="guide-section-title">收款账号</div>
        <div class="guide-form-row">
          <Form.Item label="银行名称">
            <Input v-model:value="formState.bankName" :maxlength="120" placeholder="例如：招商银行杭州分行" />
          </Form.Item>
          <Form.Item label="银行账号">
            <Input v-model:value="formState.bankAccountNo" :maxlength="120" placeholder="请输入银行账号" />
          </Form.Item>
          <Form.Item label="支付宝姓名">
            <Input v-model:value="formState.alipayName" :maxlength="80" placeholder="请输入支付宝实名" />
          </Form.Item>
        </div>
        <div class="guide-form-row">
          <Form.Item label="支付宝账号">
            <Input v-model:value="formState.alipayAccount" :maxlength="200" placeholder="备用金或结算使用" />
          </Form.Item>
          <Form.Item label="企业码账号">
            <Input v-model:value="formState.enterpriseCodeAccount" :maxlength="120" placeholder="企业码或导游端账号标识" />
          </Form.Item>
          <Form.Item label="评分">
            <InputNumber
              v-model:value="formState.rating"
              :max="5"
              :min="0"
              :precision="1"
              :step="0.1"
              style="width: 100%"
            />
          </Form.Item>
        </div>

        <div class="guide-section-title">导游展示资料</div>
        <div class="guide-form-row">
          <Form.Item label="年龄">
            <InputNumber v-model:value="formState.age" :min="0" :max="120" style="width: 100%" />
          </Form.Item>
          <Form.Item label="籍贯">
            <Input v-model:value="formState.nativePlace" :maxlength="120" placeholder="例如：浙江杭州" />
          </Form.Item>
          <Form.Item label="从业年数">
            <InputNumber v-model:value="formState.workingYears" :min="0" style="width: 100%" />
          </Form.Item>
        </div>
        <div class="guide-form-row">
          <Form.Item label="语言">
            <Input v-model:value="formState.languages" :maxlength="200" placeholder="例如：普通话、英语" />
          </Form.Item>
          <Form.Item label="累计带团次数">
            <InputNumber v-model:value="formState.totalTours" :min="0" style="width: 100%" />
          </Form.Item>
        </div>
        <div class="guide-form-row">
          <Form.Item label="导游证书">
            <Upload
              accept=".pdf,.jpg,.jpeg,.png"
              :before-upload="beforeUploadGuideCertificate"
              :file-list="certificateFileList"
              :max-count="1"
              :on-preview="previewGuideCertificate"
              :on-remove="removeGuideCertificate"
            >
              <Button :loading="certificateUploading">上传导游证书</Button>
            </Upload>
            <div class="guide-upload-hint">支持 PDF、JPG、PNG，保存后随导游档案留存。</div>
          </Form.Item>
          <Form.Item label="个人照片">
            <Upload
              accept=".jpg,.jpeg,.png"
              :before-upload="beforeUploadGuidePhoto"
              :file-list="photoFileList"
              :max-count="1"
              :on-preview="previewGuidePhoto"
              :on-remove="removeGuidePhoto"
              list-type="picture"
            >
              <Button :loading="photoUploading">上传个人照片</Button>
            </Upload>
            <div class="guide-upload-hint">支持 JPG、PNG，用于导游资料展示。</div>
          </Form.Item>
        </div>
        <Form.Item label="个人介绍">
          <Input.TextArea
            v-model:value="formState.personalIntro"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="填写导游擅长线路、带团风格或展示说明"
          />
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="填写内部管理说明"
          />
        </Form.Item>
      </Form>
    </Modal>

    <Modal
      v-model:open="tagModalOpen"
      :confirm-loading="tagSaving"
      :title="tagEditingId ? '编辑导游标签' : '新增导游标签'"
      ok-text="保存"
      cancel-text="取消"
      width="560px"
      @ok="saveTag"
    >
      <Form :model="tagFormState" layout="vertical">
        <Form.Item label="标签名称" required>
          <Input v-model:value="tagFormState.tagName" :maxlength="80" placeholder="例如：金牌导游、研学、英语" />
        </Form.Item>
        <div class="guide-form-row-two">
          <Form.Item label="排序">
            <InputNumber v-model:value="tagFormState.sortOrder" :min="0" style="width: 100%" />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="tagFormState.status" :options="statusOptions" />
          </Form.Item>
        </div>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="tagFormState.remark"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="填写标签使用说明"
          />
        </Form.Item>
      </Form>
    </Modal>

    <Modal
      v-model:open="filePreviewOpen"
      :footer="null"
      :title="filePreviewTitle"
      width="720px"
      @cancel="closeGuideFilePreview"
    >
      <img
        v-if="filePreviewUrl"
        alt="导游附件预览"
        class="guide-preview-image"
        :src="filePreviewUrl"
      />
    </Modal>
  </Page>
</template>

<style scoped>
.guide-form-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.guide-form-row-two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.guide-section-title {
  margin: 18px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.guide-section-title:first-child {
  margin-top: 0;
}

.guide-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 18px;
}

.guide-upload-hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 18px;
  color: #64748b;
}

.guide-ellipsis {
  display: inline-block;
  max-width: 210px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.guide-preview-image {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: 0 auto;
  object-fit: contain;
}

@media (max-width: 900px) {
  .guide-form-row,
  .guide-form-row-two {
    display: block;
  }
}
</style>
