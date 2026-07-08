<script lang="ts" setup>
import type {
  TableColumnsType,
  TablePaginationConfig,
  UploadFile,
  UploadProps,
} from 'ant-design-vue';

import { Page } from '@vben/common-ui';
import {
  Button,
  Card,
  DatePicker,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Upload,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import {
  downloadAttachment,
  listAttachments,
  uploadAttachment,
  type AttachmentApi,
} from '#/api/common/attachment';
import {
  createContract,
  deleteContract,
  getContractPage,
  getNextContractNo,
  updateContract,
  type ContractApi,
} from '#/api/contract';
import {
  getCustomerUnitDetail,
  getCustomerUnitPage,
  type CustomerUnitApi,
} from '#/api/customer/unit';
import {
  getEnterpriseCompanyInfoCurrent,
  type EnterpriseCompanyInfoApi,
} from '#/api/enterprise/company-info';
import {
  getSupplierAll,
  type SupplierApi,
} from '#/api/purchase/supplier';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

import {
  contractTabLabel,
  contractTypeTabs,
  supplierCategoryByContractTab,
  type ContractTabKey,
} from './contract-tabs';
import {
  attachmentDownloadPath,
  attachmentPreviewKind,
  canPreviewAttachment,
} from './attachment-preview';
import { contractQueryFromRoute } from './route-query';

type ContractStatus = ContractApi.Status;
type RawContract = ContractApi.Item;

interface ContractRow {
  createdAt?: string;
  createdBy?: string;
  endDate?: string;
  id: number;
  partyName?: string;
  raw: RawContract;
  settlementText?: string;
  startDate?: string;
  status: ContractStatus;
  title?: string;
  contractNo: string;
}

interface ContractFormState {
  agreementContent?: string;
  contractName?: string;
  contractNo: string;
  customerId?: number;
  customerName?: string;
  endDate?: string;
  invoiceSubject?: string;
  legalSubject?: string;
  otherContent?: string;
  partyAAddress?: string;
  partyAContact?: string;
  partyAFax?: string;
  partyAName?: string;
  partyAPhone?: string;
  partyBAddress?: string;
  partyBContact?: string;
  partyBFax?: string;
  partyBName?: string;
  partyBPhone?: string;
  purchasePriceSummary?: string;
  reminderDays?: number;
  remark?: string;
  settlementSubject?: string;
  settlementText?: string;
  startDate?: string;
  status: ContractStatus;
  supplierId?: number;
  supplierName?: string;
  templateName?: string;
}

const CONTRACT_BUSINESS_MODULE = '合同管理';
const CONTRACT_BUSINESS_TYPE = '合同';

const route = useRoute();

const columns: TableColumnsType<ContractRow> = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 160 },
  { title: '公司名称', dataIndex: 'partyName', key: 'partyName', width: 240 },
  { title: '合同名称', dataIndex: 'title', key: 'title', width: 200 },
  { title: '合同期限', key: 'period', width: 220 },
  { title: '结款方式', dataIndex: 'settlementText', key: 'settlementText', width: 140 },
  { title: '创建人/时间', key: 'created', width: 180 },
  { title: '有效期止', dataIndex: 'endDate', key: 'endDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', fixed: 'right', width: 230 },
];

const statusOptions: Array<{ label: string; value: ContractStatus }> = [
  { label: '正常', value: 'active' },
  { label: '停用', value: 'disabled' },
  { label: '终止', value: 'terminated' },
];

const customerSettlementOptions: Array<{
  label: string;
  value: CustomerUnitApi.SettlementMethod;
}> = [
  { label: '不限', value: 'unlimited' },
  { label: '现结', value: 'cash' },
  ...Array.from({ length: 12 }, (_, index) => ({
    label: `${index + 1}个月结`,
    value: `monthly_${index + 1}` as CustomerUnitApi.SettlementMethod,
  })),
];

const initialRouteQuery = contractQueryFromRoute(route.query as Record<string, unknown>);
const activeTab = ref<ContractTabKey>(
  (initialRouteQuery.contractType as ContractTabKey | undefined) || 'customer',
);
const data = ref<ContractRow[]>([]);
const companyInfo = ref<EnterpriseCompanyInfoApi.Item | null>(null);
const companyInfoLoaded = ref(false);
const customers = ref<CustomerUnitApi.CustomerUnit[]>([]);
const suppliers = ref<SupplierApi.Item[]>([]);
const attachments = ref<AttachmentApi.Attachment[]>([]);
const loading = ref(false);
const customerLoading = ref(false);
const supplierLoading = ref(false);
const modalOpen = ref(false);
const attachmentDrawerOpen = ref(false);
const attachmentLoading = ref(false);
const attachmentPreviewLoading = ref(false);
const attachmentPreviewObjectUrl = ref('');
const attachmentPreviewOpen = ref(false);
const modalUploading = ref(false);
const uploading = ref(false);
const saving = ref(false);
const editingId = ref<number>();
const currentContract = ref<ContractRow>();
const previewAttachment = ref<AttachmentApi.Attachment>();
const modalPendingFiles = ref<Array<{ file: File; uid: string }>>([]);
const modalUploadFileList = ref<UploadFile[]>([]);

const query = reactive({
  customerId: initialRouteQuery.customerId,
  keyword: undefined as string | undefined,
  page: 1,
  pageSize: 10,
  status: undefined as ContractStatus | undefined,
  supplierId: initialRouteQuery.supplierId,
});

const formState = reactive<ContractFormState>({
  agreementContent: '',
  contractName: '',
  contractNo: '',
  customerId: undefined,
  customerName: '',
  endDate: undefined,
  invoiceSubject: '',
  legalSubject: '',
  otherContent: '',
  partyAAddress: '',
  partyAContact: '',
  partyAFax: '',
  partyAName: '',
  partyAPhone: '',
  partyBAddress: '',
  partyBContact: '',
  partyBFax: '',
  partyBName: '',
  partyBPhone: '',
  purchasePriceSummary: '',
  reminderDays: 30,
  remark: '',
  settlementSubject: '',
  settlementText: '',
  startDate: undefined,
  status: 'active',
  supplierId: undefined,
  supplierName: '',
  templateName: '',
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const isCustomerTab = computed(() => activeTab.value === 'customer');
const supplierRequired = computed(() =>
  [
    'scenic',
    'hotel',
    'restaurant',
    'vehicle',
    'traffic',
    'ground_agent',
    'guide',
    'shopping',
  ].includes(activeTab.value),
);

const customerOptions = computed(() =>
  customers.value.map((item) => ({
    label: item.customerName,
    value: item.id,
  })),
);

const supplierOptions = computed(() =>
  suppliers.value.map((item) => ({
    label: item.supplierName,
    value: item.id,
  })),
);

const selectedCustomer = computed(() =>
  query.customerId
    ? customers.value.find((item) => item.id === query.customerId)
    : undefined,
);

const currentTabLabel = computed(() => contractTabLabel(activeTab.value));

const previewKind = computed(() =>
  previewAttachment.value
    ? attachmentPreviewKind(previewAttachment.value)
    : 'download-only',
);

const previewTitle = computed(() =>
  previewAttachment.value?.originalFilename || '合同文件预览',
);

async function loadCustomers() {
  customerLoading.value = true;
  try {
    const result = await getCustomerUnitPage({ page: 1, pageSize: 200 });
    customers.value = result.items;
  } finally {
    customerLoading.value = false;
  }
}

async function loadCompanyInfo() {
  if (companyInfoLoaded.value) {
    return;
  }
  companyInfo.value = await getEnterpriseCompanyInfoCurrent();
  companyInfoLoaded.value = true;
}

async function loadSuppliers() {
  supplierLoading.value = true;
  try {
    const category = supplierLookupCategory();
    suppliers.value = await getSupplierAll(category);
  } finally {
    supplierLoading.value = false;
  }
}

async function ensureCustomerLoaded(customerId?: number) {
  if (!customerId || customers.value.some((item) => item.id === customerId)) {
    return;
  }
  const customer = await getCustomerUnitDetail(customerId);
  customers.value = [customer, ...customers.value];
}

async function loadData() {
  loading.value = true;
  try {
    if (isCustomerTab.value) {
      await ensureCustomerLoaded(query.customerId);
    }
    const result = await getContractPage({
      contractType: activeTab.value,
      customerId: isCustomerTab.value ? query.customerId : undefined,
      keyword: query.keyword,
      page: query.page,
      pageSize: query.pageSize,
      status: query.status,
      supplierId: isCustomerTab.value ? undefined : query.supplierId,
    });
    data.value = result.items.map(contractRow);
    pagination.total = result.total;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
  } finally {
    loading.value = false;
  }
}

function contractRow(item: ContractApi.Item): ContractRow {
  return {
    contractNo: item.contractNo,
    createdAt: item.createdAt,
    createdBy: item.createdBy,
    endDate: item.endDate,
    id: item.id,
    partyName: item.partyBName || item.counterpartyName,
    raw: item,
    settlementText: item.settlementTerms,
    startDate: item.startDate,
    status: item.status,
    title: item.contractName || item.templateName,
  };
}

async function handleTabChange() {
  query.keyword = undefined;
  query.page = 1;
  query.status = undefined;
  query.supplierId = undefined;
  if (!isCustomerTab.value) {
    query.customerId = undefined;
    await loadSuppliers();
  }
  await loadData();
}

function selectContractTab(tabKey: ContractTabKey) {
  if (activeTab.value === tabKey) {
    return;
  }
  activeTab.value = tabKey;
  handleTabChange();
}

function resetQuery() {
  Object.assign(query, {
    customerId: isCustomerTab.value ? undefined : query.customerId,
    keyword: undefined,
    page: 1,
    pageSize: query.pageSize,
    status: undefined,
    supplierId: undefined,
  });
  loadData();
}

function handleSearch() {
  query.page = 1;
  loadData();
}

function handleTableChange(nextPagination: TablePaginationConfig) {
  query.page = Number(nextPagination.current || 1);
  query.pageSize = Number(nextPagination.pageSize || 10);
  loadData();
}

function resetForm() {
  const startDate = today();
  Object.assign(formState, {
    agreementContent: '',
    contractName: isCustomerTab.value ? '' : `${currentTabLabel.value}合同`,
    contractNo: '',
    customerId: query.customerId,
    customerName: customerNameById(query.customerId),
    endDate: addOneYearMinusOneDay(startDate),
    invoiceSubject: '',
    legalSubject: '',
    otherContent: '',
    partyAAddress: '',
    partyAContact: '',
    partyAFax: '',
    partyAName: '',
    partyAPhone: '',
    partyBAddress: '',
    partyBContact: '',
    partyBFax: '',
    partyBName: '',
    partyBPhone: '',
    purchasePriceSummary: '',
    reminderDays: 30,
    remark: '',
    settlementSubject: '',
    settlementText: '',
    startDate,
    status: 'active',
    supplierId: query.supplierId,
    supplierName: supplierNameById(query.supplierId),
    templateName: '',
  });
  fillPartyAFromCompanyInfo();
  if (isCustomerTab.value) {
    fillPartyBFromCustomerId(query.customerId);
  } else {
    fillPartyBFromSupplierId(query.supplierId);
  }
}

function resetModalContractFiles() {
  modalPendingFiles.value = [];
  modalUploadFileList.value = [];
}

async function openCreateModal() {
  editingId.value = undefined;
  await loadCompanyInfo();
  resetModalContractFiles();
  resetForm();
  try {
    formState.contractNo = await getNextContractNo(activeTab.value);
  } catch {
    message.warning('合同编号自动生成失败，请手工填写');
  }
  modalOpen.value = true;
}

function openEditModal(record: Record<string, any>) {
  const row = record as ContractRow;
  editingId.value = row.id;
  resetModalContractFiles();
  const contract = row.raw;
  Object.assign(formState, {
    contractName: contract.contractName || '',
    contractNo: contract.contractNo,
    customerId: contract.customerId,
    customerName: contract.counterpartyName || '',
    endDate: contract.endDate,
    invoiceSubject: contract.invoiceSubject || '',
    legalSubject: contract.legalSubject || '',
    agreementContent: contract.agreementContent || '',
    otherContent: contract.otherContent || '',
    partyAAddress: contract.partyAAddress || '',
    partyAContact: contract.partyAContact || '',
    partyAFax: contract.partyAFax || '',
    partyAName: contract.partyAName || '',
    partyAPhone: contract.partyAPhone || '',
    partyBAddress: contract.partyBAddress || '',
    partyBContact: contract.partyBContact || '',
    partyBFax: contract.partyBFax || '',
    partyBName: contract.partyBName || contract.counterpartyName || '',
    partyBPhone: contract.partyBPhone || '',
    purchasePriceSummary: contract.purchasePriceSummary || '',
    reminderDays: contract.reminderDays ?? 30,
    remark: contract.remark || '',
    settlementSubject: contract.settlementSubject || '',
    settlementText: contract.settlementTerms || '',
    startDate: contract.startDate,
    status: contract.status || 'active',
    supplierId: contract.supplierId,
    supplierName: contract.counterpartyName || '',
    templateName: contract.templateName || '',
  });
  modalOpen.value = true;
}

function handleCustomerChange(value?: unknown) {
  formState.customerName = typeof value === 'number' ? customerNameById(value) : '';
  fillPartyBFromCustomerId(typeof value === 'number' ? value : undefined, true);
}

function handleSupplierChange(value?: unknown) {
  formState.supplierName = typeof value === 'number' ? supplierNameById(value) : '';
  fillPartyBFromSupplierId(typeof value === 'number' ? value : undefined, true);
}

function customerNameById(customerId?: number) {
  return customers.value.find((item) => item.id === customerId)?.customerName;
}

function customerById(customerId?: number) {
  return customers.value.find((item) => item.id === customerId);
}

function customerAddress(customer?: CustomerUnitApi.CustomerUnit) {
  return [customer?.province, customer?.city, customer?.district]
    .filter(Boolean)
    .join('');
}

function fillPartyAFromCompanyInfo(force = false) {
  const info = companyInfo.value;
  if (!info) {
    return;
  }
  if (force || !formState.partyAName) {
    formState.partyAName = info.companyName || '';
  }
  if (force || !formState.partyAPhone) {
    formState.partyAPhone = info.contactPhone || '';
  }
  if (force || !formState.partyAFax) {
    formState.partyAFax = info.faxNumber || '';
  }
  if (force || !formState.partyAAddress) {
    formState.partyAAddress = info.officeAddress || '';
  }
  if (force || !formState.partyAContact) {
    formState.partyAContact = info.contactName || '';
  }
  if (force || !formState.legalSubject) {
    formState.legalSubject = info.companyName || '';
  }
  if (force || !formState.invoiceSubject) {
    formState.invoiceSubject = info.companyName || '';
  }
  if (force || !formState.settlementSubject) {
    formState.settlementSubject = info.companyName || '';
  }
}

function fillPartyBFromCustomerId(customerId?: number, force = false) {
  const customer = customerById(customerId);
  if (!customer) {
    if (force) {
      formState.partyBName = '';
      formState.partyBPhone = '';
      formState.partyBAddress = '';
      formState.partyBContact = '';
      formState.settlementText = '';
    }
    return;
  }
  if (force || !formState.partyBName) {
    formState.partyBName = customer.customerName;
  }
  if (force || !formState.partyBPhone) {
    formState.partyBPhone = customer.contactPhone || '';
  }
  if (force || !formState.partyBContact) {
    formState.partyBContact = customer.contactName || '';
  }
  if (force || !formState.partyBAddress) {
    formState.partyBAddress = customerAddress(customer);
  }
  if (force || !formState.settlementText) {
    formState.settlementText = customerSettlementLabel(customer.settlementMethod);
  }
}

function supplierNameById(supplierId?: number) {
  return suppliers.value.find((item) => item.id === supplierId)?.supplierName;
}

function supplierById(supplierId?: number) {
  return suppliers.value.find((item) => item.id === supplierId);
}

function supplierAddress(supplier?: SupplierApi.Item) {
  return [supplier?.province, supplier?.city, supplier?.district]
    .filter(Boolean)
    .join('');
}

function fillPartyBFromSupplierId(supplierId?: number, force = false) {
  const supplier = supplierById(supplierId);
  if (!supplier) {
    if (force) {
      formState.partyBName = '';
      formState.partyBPhone = '';
      formState.partyBAddress = '';
      formState.partyBContact = '';
      formState.settlementText = '';
    }
    return;
  }
  if (force || !formState.partyBName) {
    formState.partyBName = supplier.supplierName;
  }
  if (force || !formState.partyBPhone) {
    formState.partyBPhone = supplier.contactPhone || '';
  }
  if (force || !formState.partyBContact) {
    formState.partyBContact = supplier.contactName || '';
  }
  if (force || !formState.partyBAddress) {
    formState.partyBAddress = supplierAddress(supplier);
  }
  if (force || !formState.settlementText) {
    formState.settlementText = supplier.settlementMethod || '';
  }
}

async function saveContract() {
  if (isCustomerTab.value && !formState.customerId) {
    message.warning('请选择客户单位');
    return;
  }
  if (supplierRequired.value && !formState.supplierId) {
    message.warning(`请选择${currentTabLabel.value}供应商`);
    return;
  }
  if (!isCustomerTab.value && !formState.contractName?.trim()) {
    message.warning('请填写合同名称');
    return;
  }

  saving.value = true;
  try {
    const savedRow = await saveUnifiedContract();
    try {
      await uploadPendingContractFiles(savedRow);
    } catch {
      message.error('合同已保存，但合同文件上传失败，请在合同文件中补传');
    }
    resetModalContractFiles();
    modalOpen.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

async function saveUnifiedContract(): Promise<ContractRow> {
  const payload: ContractApi.SaveParams = {
    contractType: activeTab.value,
    contractName: formState.contractName?.trim() || `${currentTabLabel.value}合同`,
    contractNo: formState.contractNo.trim(),
    counterpartyName: isCustomerTab.value
      ? clean(formState.customerName) || customerNameById(formState.customerId)
      : clean(formState.supplierName) || supplierNameById(formState.supplierId),
    customerId: isCustomerTab.value ? formState.customerId : undefined,
    endDate: formState.endDate,
    invoiceSubject: clean(formState.invoiceSubject),
    legalSubject: clean(formState.legalSubject),
    partyAAddress: clean(formState.partyAAddress),
    partyAContact: clean(formState.partyAContact),
    partyAFax: clean(formState.partyAFax),
    partyAName: clean(formState.partyAName),
    partyAPhone: clean(formState.partyAPhone),
    partyBAddress: clean(formState.partyBAddress),
    partyBContact: clean(formState.partyBContact),
    partyBFax: clean(formState.partyBFax),
    partyBName: clean(formState.partyBName),
    partyBPhone: clean(formState.partyBPhone),
    agreementContent: clean(formState.agreementContent),
    otherContent: clean(formState.otherContent),
    reminderDays: formState.reminderDays ?? 30,
    remark: formState.remark,
    settlementTerms: clean(formState.settlementText),
    settlementSubject: clean(formState.settlementSubject),
    startDate: formState.startDate,
    status: formState.status || 'active',
    supplierId: isCustomerTab.value ? undefined : formState.supplierId,
    templateName: clean(formState.templateName),
    purchasePriceSummary: clean(formState.purchasePriceSummary),
  };
  let saved: ContractApi.Item;
  if (editingId.value) {
    saved = await updateContract(editingId.value, payload);
    message.success('合同已更新');
  } else {
    saved = await createContract(payload);
    message.success('合同已新增');
  }
  return contractRow(saved);
}

function confirmDelete(record: Record<string, any>) {
  const row = record as ContractRow;
  Modal.confirm({
    title: `删除合同「${row.contractNo}」？`,
    content: '删除后不会物理移除记录，只会标记为已删除。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteContract(row.id);
      message.success('合同已删除');
      loadData();
    },
  });
}

async function openAttachmentDrawer(record: Record<string, any>) {
  const row = record as ContractRow;
  currentContract.value = row;
  attachmentDrawerOpen.value = true;
  await loadAttachments(row);
}

async function loadAttachments(row: ContractRow) {
  attachmentLoading.value = true;
  try {
    attachments.value = await listAttachments({
      businessId: row.id,
      businessModule: businessModule(row),
      businessType: businessType(row),
      page: 1,
      pageSize: 100,
    });
  } finally {
    attachmentLoading.value = false;
  }
}

async function openAttachmentPreview(record: AttachmentApi.Attachment) {
  previewAttachment.value = record;
  attachmentPreviewOpen.value = true;
  revokeAttachmentPreviewObjectUrl();
  if (!canPreviewAttachment(record)) {
    return;
  }

  attachmentPreviewLoading.value = true;
  try {
    const blob = await loadAttachmentBlob(record);
    attachmentPreviewObjectUrl.value = URL.createObjectURL(blob);
  } catch {
    message.error('合同文件加载失败，请稍后再试');
    attachmentPreviewOpen.value = false;
  } finally {
    attachmentPreviewLoading.value = false;
  }
}

async function downloadContractAttachment(record: AttachmentApi.Attachment) {
  try {
    const blob = await loadAttachmentBlob(record);
    triggerBlobDownload(blob, record.originalFilename);
  } catch {
    message.error('合同文件下载失败，请稍后再试');
  }
}

async function loadAttachmentBlob(record: AttachmentApi.Attachment) {
  const filePath = attachmentDownloadPath(record.fileUrl);
  if (!filePath) {
    throw new Error('附件地址为空');
  }
  return await downloadAttachment(filePath);
}

function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || '合同文件';
  link.style.display = 'none';
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 100);
}

function revokeAttachmentPreviewObjectUrl() {
  if (attachmentPreviewObjectUrl.value) {
    URL.revokeObjectURL(attachmentPreviewObjectUrl.value);
    attachmentPreviewObjectUrl.value = '';
  }
}

const beforeUploadContractFile: UploadProps['beforeUpload'] = async (file) => {
  if (!currentContract.value) {
    message.warning('请先选择合同');
    return false;
  }
  const row = currentContract.value;
  const formData = new FormData();
  formData.append('file', file as File);
  formData.append('businessModule', businessModule(row));
  formData.append('businessType', businessType(row));
  formData.append('businessId', String(row.id));

  uploading.value = true;
  try {
    await uploadAttachment(formData);
    message.success('合同文件已上传');
    await loadAttachments(row);
  } finally {
    uploading.value = false;
  }
  return false;
};

const beforeUploadModalContractFile: UploadProps['beforeUpload'] = (file) => {
  const rawFile = file as File & { uid?: string };
  const uid = rawFile.uid || `${Date.now()}-${rawFile.name}`;
  modalPendingFiles.value = [...modalPendingFiles.value, { file: rawFile, uid }];
  modalUploadFileList.value = [
    ...modalUploadFileList.value,
    {
      name: rawFile.name,
      size: rawFile.size,
      status: 'done',
      type: rawFile.type,
      uid,
    },
  ];
  return false;
};

const removeModalContractFile: UploadProps['onRemove'] = (file) => {
  modalPendingFiles.value = modalPendingFiles.value.filter((item) => item.uid !== file.uid);
  modalUploadFileList.value = modalUploadFileList.value.filter((item) => item.uid !== file.uid);
  return true;
};

async function uploadPendingContractFiles(row: ContractRow) {
  if (modalPendingFiles.value.length === 0) {
    return;
  }
  modalUploading.value = true;
  try {
    for (const item of modalPendingFiles.value) {
      const formData = new FormData();
      formData.append('file', item.file);
      formData.append('businessModule', businessModule(row));
      formData.append('businessType', businessType(row));
      formData.append('businessId', String(row.id));
      await uploadAttachment(formData);
    }
    message.success('合同文件已上传');
  } finally {
    modalUploading.value = false;
  }
}

function businessModule(_row: ContractRow) {
  return CONTRACT_BUSINESS_MODULE;
}

function businessType(_row: ContractRow) {
  return CONTRACT_BUSINESS_TYPE;
}

function supplierLookupCategory(): SupplierApi.Category | undefined {
  const category = supplierCategoryByContractTab(activeTab.value);
  if (
    category === 'hotel'
    || category === 'scenic'
    || category === 'vehicle'
    || category === 'restaurant'
    || category === 'traffic'
    || category === 'ground_agent'
    || category === 'shopping'
    || category === 'other'
  ) {
    return category;
  }
  return undefined;
}

function clean(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

function periodText(record: Record<string, any>) {
  const row = record as ContractRow;
  return `${row.startDate || '-'} 至 ${row.endDate || '-'}`;
}

function createdText(record: Record<string, any>) {
  const row = record as ContractRow;
  return [row.createdBy || '-', row.createdAt?.slice(0, 10) || '-'].join(' / ');
}

function effectiveStatus(record: Record<string, any>) {
  const row = record as ContractRow;
  if (row.status === 'active' && row.endDate && row.endDate < today()) {
    return 'expired';
  }
  return row.status;
}

function statusLabel(value?: string) {
  const labels: Record<string, string> = {
    active: '正常',
    disabled: '停用',
    expired: '过期',
    terminated: '终止',
  };
  return labels[value || ''] || '-';
}

function statusColor(value?: string) {
  const colors: Record<string, string> = {
    active: 'green',
    disabled: 'default',
    expired: 'red',
    terminated: 'orange',
  };
  return colors[value || ''] || 'default';
}

function today() {
  return formatLocalDate(new Date());
}

function formatLocalDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function addOneYearMinusOneDay(dateText: string) {
  const [year, month, day] = dateText.split('-').map(Number);
  if (!year || !month || !day) {
    return today();
  }
  const date = new Date(year, month - 1, day);
  date.setFullYear(date.getFullYear() + 1);
  date.setDate(date.getDate() - 1);
  return formatLocalDate(date);
}

function customerSettlementLabel(value?: CustomerUnitApi.SettlementMethod) {
  return customerSettlementOptions.find((item) => item.value === value)?.label || '';
}

onMounted(async () => {
  await loadCompanyInfo();
  await loadCustomers();
  if (!isCustomerTab.value) {
    await loadSuppliers();
  }
  await loadData();
});

watch(
  () => [route.query.category, route.query.contractType, route.query.customerId, route.query.supplierId],
  () => {
    const routeQuery = contractQueryFromRoute(route.query as Record<string, unknown>);
    activeTab.value = (routeQuery.contractType as ContractTabKey | undefined)
      || (routeQuery.customerId ? 'customer' : activeTab.value);
    Object.assign(query, {
      customerId: routeQuery.customerId,
      page: 1,
      supplierId: routeQuery.supplierId,
    });
    if (!isCustomerTab.value) {
      loadSuppliers().then(loadData);
      return;
    }
    loadData();
  },
);

watch(attachmentPreviewOpen, (open) => {
  if (open) {
    return;
  }
  revokeAttachmentPreviewObjectUrl();
  previewAttachment.value = undefined;
  attachmentPreviewLoading.value = false;
});
</script>

<template>
  <Page
    title="合同管理"
    description="按老系统合同类型切换，统一维护客户单位和采购侧合同。"
  >
    <Card>
      <div class="contract-tab-bar" role="tablist" aria-label="合同分类">
        <button
          v-for="tab in contractTypeTabs"
          :key="tab.key"
          type="button"
          class="contract-tab-pill"
          :class="{ 'is-active': activeTab === tab.key }"
          role="tab"
          :aria-selected="activeTab === tab.key"
          @click="selectContractTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="selectedCustomer && isCustomerTab" class="contract-context">
        当前客户：{{ selectedCustomer.customerName }}
      </div>

      <BusinessSearchForm
        :model="query"
        :search-loading="loading"
        create-text="新增"
        @create="openCreateModal"
        @reset="resetQuery"
        @search="handleSearch"
      >
        <Form.Item label="合同/公司">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="合同编号 / 公司名称"
            @press-enter="handleSearch"
          />
        </Form.Item>
        <Form.Item v-if="isCustomerTab" label="客户单位">
          <Select
            v-model:value="query.customerId"
            allow-clear
            :loading="customerLoading"
            :options="customerOptions"
            placeholder="请选择客户单位"
            show-search
            :filter-option="(input, option) => String(option?.label || '').includes(input)"
          />
        </Form.Item>
        <Form.Item v-else label="供应商" :required="supplierRequired">
          <Select
            v-model:value="query.supplierId"
            allow-clear
            :loading="supplierLoading"
            :options="supplierOptions"
            placeholder="请选择供应商"
            show-search
            :filter-option="(input, option) => String(option?.label || '').includes(input)"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Select
            v-model:value="query.status"
            allow-clear
            :options="statusOptions"
            placeholder="请选择合同状态"
          />
        </Form.Item>
      </BusinessSearchForm>

      <Table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1600 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'period'">
            {{ periodText(record) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusColor(effectiveStatus(record))">
              {{ statusLabel(effectiveStatus(record)) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'created'">
            {{ createdText(record) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="openEditModal(record)">
                修改合同
              </Button>
              <Button type="link" size="small" @click="openAttachmentDrawer(record)">
                合同文件
              </Button>
              <Button
                type="link"
                size="small"
                danger
                @click="confirmDelete(record)"
              >
                删除
              </Button>
            </Space>
          </template>
          <template v-else>
            {{ record[column.key as keyof ContractRow] || '-' }}
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalOpen"
      :title="editingId ? '修改合同' : `新增${currentTabLabel}合同`"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="saving || modalUploading"
      width="1080px"
      @ok="saveContract"
    >
      <Form :model="formState" layout="vertical">
        <div class="contract-modal-section-title">基础信息</div>
        <div class="contract-modal-grid">
          <Form.Item label="合同编号" required>
            <Input
              v-model:value="formState.contractNo"
              :maxlength="80"
              placeholder="系统自动生成，可手工调整"
            />
          </Form.Item>
          <Form.Item v-if="isCustomerTab" label="客户单位" required>
            <Select
              v-model:value="formState.customerId"
              allow-clear
              :loading="customerLoading"
              :options="customerOptions"
              placeholder="请选择客户单位"
              show-search
              :filter-option="(input, option) => String(option?.label || '').includes(input)"
              @change="handleCustomerChange"
            />
          </Form.Item>
          <Form.Item v-else label="供应商">
            <Select
              v-model:value="formState.supplierId"
              allow-clear
              :loading="supplierLoading"
              :options="supplierOptions"
              :placeholder="supplierRequired ? '请选择供应商' : '可选供应商'"
              show-search
              :filter-option="(input, option) => String(option?.label || '').includes(input)"
              @change="handleSupplierChange"
            />
          </Form.Item>
          <Form.Item v-if="!isCustomerTab" label="合同名称" required>
            <Input
              v-model:value="formState.contractName"
              :maxlength="200"
              placeholder="请输入合同名称"
            />
          </Form.Item>
          <Form.Item label="合同开始日期">
            <DatePicker
              v-model:value="formState.startDate"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
          </Form.Item>
          <Form.Item label="合同到期日期">
            <DatePicker
              v-model:value="formState.endDate"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
          </Form.Item>
          <Form.Item label="结款方式">
            <Input
              v-model:value="formState.settlementText"
              :maxlength="100"
              placeholder="例如：月结 / 现结 / 按协议"
            />
          </Form.Item>
          <Form.Item label="到期提醒天数">
            <InputNumber
              v-model:value="formState.reminderDays"
              class="w-full"
              :min="0"
              :precision="0"
            />
          </Form.Item>
          <template v-if="isCustomerTab">
            <Form.Item label="合同主体">
              <Input v-model:value="formState.legalSubject" :maxlength="200" />
            </Form.Item>
            <Form.Item label="开票主体">
              <Input v-model:value="formState.invoiceSubject" :maxlength="200" />
            </Form.Item>
            <Form.Item label="结算主体">
              <Input v-model:value="formState.settlementSubject" :maxlength="200" />
            </Form.Item>
            <Form.Item label="合同模板">
              <Input v-model:value="formState.templateName" :maxlength="120" />
            </Form.Item>
          </template>
          <Form.Item v-else label="采购价格说明">
            <Input.TextArea
              v-model:value="formState.purchasePriceSummary"
              :rows="2"
            />
          </Form.Item>
          <Form.Item label="状态">
            <Select
              v-model:value="formState.status"
              :options="statusOptions"
              placeholder="请选择合同状态"
            />
          </Form.Item>
        </div>

        <div class="contract-party-sections">
          <div class="contract-modal-section-head">
            <div class="contract-modal-section-title">甲方信息</div>
            <Button size="small" @click="fillPartyAFromCompanyInfo(true)">
              从公司信息带入
            </Button>
          </div>
          <div class="contract-modal-hint">
            默认读取企业资料 / 公司信息；未维护公司信息时，可直接手工填写。
          </div>
          <div class="contract-modal-grid">
            <Form.Item label="甲方名称">
              <Input
                v-model:value="formState.partyAName"
                :maxlength="200"
                placeholder="请输入甲方公司名称"
              />
            </Form.Item>
            <Form.Item label="甲方联系人">
              <Input
                v-model:value="formState.partyAContact"
                :maxlength="80"
              />
            </Form.Item>
            <Form.Item label="甲方电话">
              <Input
                v-model:value="formState.partyAPhone"
                :maxlength="40"
              />
            </Form.Item>
            <Form.Item label="甲方传真">
              <Input
                v-model:value="formState.partyAFax"
                :maxlength="40"
              />
            </Form.Item>
          </div>
          <Form.Item label="甲方地址">
            <Input
              v-model:value="formState.partyAAddress"
              :maxlength="300"
            />
          </Form.Item>

          <div class="contract-modal-section-head">
            <div class="contract-modal-section-title">乙方信息</div>
            <Button
              v-if="isCustomerTab"
              size="small"
              :disabled="!formState.customerId"
              @click="fillPartyBFromCustomerId(formState.customerId, true)"
            >
              从客户单位带入
            </Button>
            <Button
              v-else
              size="small"
              :disabled="!formState.supplierId"
              @click="fillPartyBFromSupplierId(formState.supplierId, true)"
            >
              从供应商带入
            </Button>
          </div>
          <div v-if="isCustomerTab" class="contract-modal-hint">
            乙方是当前分销商 / 客户单位，选择客户后会自动带出名称、负责人、电话和地区。
          </div>
          <div v-else class="contract-modal-hint">
            乙方是当前供应商，选择供应商后会自动带出名称、负责人、电话、地区和结款方式。
          </div>
          <div class="contract-modal-grid">
            <Form.Item label="乙方名称">
              <Input
                v-model:value="formState.partyBName"
                :maxlength="200"
                placeholder="请输入乙方客户名称"
              />
            </Form.Item>
            <Form.Item label="乙方联系人">
              <Input
                v-model:value="formState.partyBContact"
                :maxlength="80"
              />
            </Form.Item>
            <Form.Item label="乙方电话">
              <Input
                v-model:value="formState.partyBPhone"
                :maxlength="40"
              />
            </Form.Item>
            <Form.Item label="乙方传真">
              <Input
                v-model:value="formState.partyBFax"
                :maxlength="40"
              />
            </Form.Item>
          </div>
          <Form.Item label="乙方地址">
            <Input
              v-model:value="formState.partyBAddress"
              :maxlength="300"
            />
          </Form.Item>

          <div class="contract-modal-section-title">合同内容</div>
          <Form.Item label="主要约定">
            <Input.TextArea
              v-model:value="formState.agreementContent"
              :auto-size="{ minRows: 3, maxRows: 6 }"
              placeholder="填写双方按订单、确认单、结算方式执行的主要约定"
            />
          </Form.Item>
          <Form.Item label="其它条款">
            <Input.TextArea
              v-model:value="formState.otherContent"
              :auto-size="{ minRows: 2, maxRows: 5 }"
              placeholder="填写补充条款、特殊约定或其它说明"
            />
          </Form.Item>
        </div>

        <div class="contract-modal-section-title">合同文件上传</div>
        <div class="contract-modal-hint">
          可先选择合同文件，保存合同后系统自动绑定；保存后也可以在列表“合同文件”中继续补传。
        </div>
        <Upload
          :before-upload="beforeUploadModalContractFile"
          :file-list="modalUploadFileList"
          :on-remove="removeModalContractFile"
          multiple
          accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.xls,.xlsx"
        >
          <Button :loading="modalUploading">选择合同文件</Button>
        </Upload>

        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :rows="3"
            placeholder="填写合作约定、特殊结算说明或内部备注"
          />
        </Form.Item>
      </Form>
    </Modal>

    <Drawer
      v-model:open="attachmentDrawerOpen"
      width="720"
      title="合同文件"
      destroy-on-close
    >
      <div v-if="currentContract" class="attachment-title">
        {{ currentContract.contractNo }} / {{ currentContract.partyName || currentContract.title || '-' }}
      </div>
      <Upload
        :before-upload="beforeUploadContractFile"
        :show-upload-list="false"
        accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.xls,.xlsx"
      >
        <Button type="primary" :loading="uploading">上传合同文件</Button>
      </Upload>

      <Table
        class="attachment-table"
        :data-source="attachments"
        :loading="attachmentLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <Table.Column title="文件名" data-index="originalFilename" key="originalFilename">
          <template #default="{ record }">
            <Button
              type="link"
              size="small"
              class="attachment-file-button"
              @click="openAttachmentPreview(record)"
            >
              {{ record.originalFilename }}
            </Button>
          </template>
        </Table.Column>
        <Table.Column title="上传人" data-index="uploadedBy" key="uploadedBy" width="100" />
        <Table.Column title="上传时间" data-index="createdAt" key="createdAt" width="120">
          <template #default="{ record }">
            {{ record.createdAt?.slice(0, 10) || '-' }}
          </template>
        </Table.Column>
        <Table.Column title="操作" key="action" width="120">
          <template #default="{ record }">
            <Space>
              <Button type="link" size="small" @click="openAttachmentPreview(record)">
                预览
              </Button>
              <Button type="link" size="small" @click="downloadContractAttachment(record)">
                下载
              </Button>
            </Space>
          </template>
        </Table.Column>
      </Table>
    </Drawer>

    <Modal
      v-model:open="attachmentPreviewOpen"
      :title="previewTitle"
      width="920px"
      :footer="null"
      destroy-on-close
    >
      <div v-if="attachmentPreviewLoading" class="attachment-preview-placeholder">
        文件加载中...
      </div>
      <iframe
        v-else-if="previewKind === 'pdf' && attachmentPreviewObjectUrl"
        class="attachment-preview-frame"
        :src="attachmentPreviewObjectUrl"
        title="合同文件预览"
      />
      <img
        v-else-if="previewKind === 'image' && attachmentPreviewObjectUrl"
        class="attachment-preview-image"
        :src="attachmentPreviewObjectUrl"
        alt="合同文件预览"
      >
      <div v-else class="attachment-preview-placeholder">
        <div class="attachment-preview-title">当前文件类型暂不支持在线预览</div>
        <div class="attachment-preview-desc">
          Word、Excel 等文件请下载后查看，PDF 和图片可直接在页面内预览。
        </div>
        <Button
          v-if="previewAttachment"
          type="primary"
          @click="downloadContractAttachment(previewAttachment)"
        >
          下载文件
        </Button>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.contract-tab-bar {
  display: flex;
  gap: 8px;
  padding: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
  background: #f6f8fb;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.contract-tab-bar::-webkit-scrollbar {
  height: 6px;
}

.contract-tab-bar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

.contract-tab-pill {
  flex: 0 0 auto;
  min-width: 72px;
  height: 34px;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 500;
  line-height: 32px;
  color: #334155;
  text-align: center;
  cursor: pointer;
  background: #fff;
  border: 1px solid #d8dee8;
  border-radius: 999px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.contract-tab-pill:hover {
  color: #1677ff;
  border-color: #91caff;
}

.contract-tab-pill:focus-visible {
  outline: 2px solid #91caff;
  outline-offset: 2px;
}

.contract-tab-pill.is-active {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
  box-shadow: 0 6px 14px rgb(22 119 255 / 22%);
}

.contract-context {
  padding: 10px 12px;
  margin-bottom: 14px;
  color: #0f766e;
  background: #ecfdf5;
  border: 1px solid #99f6e4;
  border-radius: 6px;
}

.contract-modal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.contract-modal-section-head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.contract-modal-section-title {
  padding-left: 10px;
  margin: 18px 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  border-left: 3px solid #1677ff;
}

.contract-modal-section-head .contract-modal-section-title {
  margin: 14px 0 10px;
}

.contract-modal-hint {
  padding: 8px 10px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.attachment-title {
  margin-bottom: 14px;
  font-weight: 600;
}

.attachment-table {
  margin-top: 16px;
}

.attachment-file-button {
  height: auto;
  padding: 0;
  white-space: normal;
  text-align: left;
}

.attachment-preview-frame {
  width: 100%;
  height: 72vh;
  min-height: 520px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.attachment-preview-image {
  display: block;
  max-width: 100%;
  max-height: 72vh;
  margin: 0 auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.attachment-preview-placeholder {
  display: flex;
  min-height: 260px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.attachment-preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
}

.attachment-preview-desc {
  font-size: 13px;
  color: #64748b;
}

@media (max-width: 768px) {
  .contract-modal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
