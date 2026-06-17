<script lang="ts" setup>
import type { TablePaginationConfig, UploadProps } from 'ant-design-vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Cascader,
  Checkbox,
  Drawer,
  Form,
  Image,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Textarea,
  Upload,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';
import {
  type AttachmentApi,
  downloadAttachment,
  listAttachments,
  uploadAttachment,
} from '#/api/common/attachment';
import {
  type PurchaseRelationApi,
  createPurchaseRelation,
} from '#/api/purchase/relation';
import {
  type PurchaseResourceApi,
  createPurchaseResource,
  deletePurchaseResource,
  getPurchaseResourceBindings,
  getPurchaseResourcePage,
  updatePurchaseResource,
} from '#/api/purchase/resource';
import {
  getSupplierAll,
  type SupplierApi,
} from '#/api/purchase/supplier';
import {
  buildRegionOptions,
  buildRegionPath,
  splitRegionPath,
  type RegionPath,
} from '#/utils/region';

type ResourceRow = PurchaseResourceApi.Item;

const resourceTypeOptions = [
  { label: '景区', value: 'scenic' },
  { label: '酒店', value: 'hotel' },
  { label: '餐厅', value: 'restaurant' },
  { label: '购物', value: 'shopping' },
];

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const regionOptions = buildRegionOptions();

const columns = [
  { dataIndex: 'resourceType', key: 'resourceType', title: '资源类型', width: 100 },
  { key: 'area', title: '所在地', width: 170 },
  { dataIndex: 'resourceName', key: 'resourceName', title: '资源名称', width: 260 },
  { dataIndex: 'phone', key: 'phone', title: '电话', width: 140 },
  { dataIndex: 'fax', key: 'fax', title: '传真', width: 130 },
  { dataIndex: 'boundSupplierCount', key: 'boundSupplierCount', title: '已绑定', width: 100 },
  { dataIndex: 'createdBy', key: 'createdBy', title: '登记人', width: 110 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间', width: 170 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 250 },
];

const data = ref<ResourceRow[]>([]);
const loading = ref(false);
const modalOpen = ref(false);
const editingId = ref<number>();
const bindingDrawerOpen = ref(false);
const imageDrawerOpen = ref(false);
const bindingLoading = ref(false);
const imageLoading = ref(false);
const uploadingImage = ref(false);
const previewOpen = ref(false);
const previewUrl = ref('');
const currentResource = ref<ResourceRow>();
const bindings = ref<PurchaseResourceApi.Binding[]>([]);
const attachments = ref<AttachmentApi.Attachment[]>([]);
const supplierOptions = ref<{ label: string; value: number }[]>([]);
const queryRegionPath = ref<RegionPath>([]);
const formRegionPath = ref<RegionPath>([]);

const query = reactive<PurchaseResourceApi.QueryParams>({
  page: 1,
  pageSize: 10,
});

const formState = reactive<PurchaseResourceApi.SaveParams>({
  autoCreateSupplier: false,
  resourceName: '',
  resourceType: 'scenic',
  status: 'active',
});

const bindForm = reactive<PurchaseRelationApi.SaveParams>({
  resourceId: undefined as unknown as number,
  status: 'active',
  supplierId: undefined as unknown as number,
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  showSizeChanger: true,
  total: 0,
});

const previewTitle = computed(() => currentResource.value?.resourceName || '资源图片');

function typeLabel(value?: string) {
  return resourceTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

function typeColor(value?: string) {
  const colors: Record<string, string> = {
    hotel: 'purple',
    restaurant: 'orange',
    scenic: 'blue',
    shopping: 'green',
  };
  return colors[value || ''] || 'default';
}

function statusLabel(value?: string) {
  return statusOptions.find((item) => item.value === value)?.label || value || '-';
}

function statusColor(value?: string) {
  return value === 'active' ? 'green' : 'default';
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function clean(value?: string) {
  return value?.trim() || undefined;
}

function areaText(record: Record<string, any>) {
  return [record.province, record.city, record.district]
    .filter(Boolean)
    .join(' / ') || '-';
}

function handleSearch() {
  const regionFields = splitRegionPath(queryRegionPath.value);
  query.province = regionFields.province;
  query.city = regionFields.city;
  query.district = regionFields.district;
  query.page = 1;
  loadData();
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
  query.page = 1;
  loadData();
}

function resetForm() {
  editingId.value = undefined;
  formRegionPath.value = [];
  formState.resourceType = 'scenic';
  formState.resourceName = '';
  formState.province = undefined;
  formState.city = undefined;
  formState.district = undefined;
  formState.phone = undefined;
  formState.fax = undefined;
  formState.address = undefined;
  formState.warmTip = undefined;
  formState.introduction = undefined;
  formState.status = 'active';
  formState.autoCreateSupplier = false;
  formState.remark = undefined;
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
  formState.resourceName = row.resourceName;
  formState.province = row.province;
  formState.city = row.city;
  formState.district = row.district;
  formState.phone = row.phone;
  formState.fax = row.fax;
  formState.address = row.address;
  formState.warmTip = row.warmTip;
  formState.introduction = row.introduction;
  formState.status = row.status;
  formState.autoCreateSupplier = false;
  formState.remark = row.remark;
  modalOpen.value = true;
}

function buildPayload(): PurchaseResourceApi.SaveParams {
  const regionFields = splitRegionPath(formRegionPath.value);
  return {
    address: clean(formState.address),
    autoCreateSupplier: Boolean(formState.autoCreateSupplier),
    city: regionFields.city,
    district: regionFields.district,
    fax: clean(formState.fax),
    introduction: clean(formState.introduction),
    phone: clean(formState.phone),
    province: regionFields.province,
    remark: clean(formState.remark),
    resourceName: formState.resourceName.trim(),
    resourceType: formState.resourceType,
    status: formState.status || 'active',
    warmTip: clean(formState.warmTip),
  };
}

async function saveRecord() {
  if (!formState.resourceName?.trim()) {
    message.warning('请填写资源名称');
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
  bindForm.supplierId = undefined as unknown as number;
  bindForm.status = 'active';
  await Promise.all([loadSuppliers(row.resourceType), loadBindings(row.id)]);
}

async function loadSuppliers(category: SupplierApi.Category) {
  const result = await getSupplierAll(category);
  supplierOptions.value = result.map((item) => ({
    label: item.supplierName,
    value: item.id,
  }));
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
    resourceId: currentResource.value.id,
    status: 'active',
    supplierId: bindForm.supplierId,
  });
  message.success('供应商已绑定');
  await Promise.all([loadBindings(currentResource.value.id), loadData()]);
}

async function openImageDrawer(record: Record<string, any>) {
  const row = record as ResourceRow;
  currentResource.value = row;
  imageDrawerOpen.value = true;
  await loadResourceImages(row);
}

async function loadResourceImages(record: ResourceRow) {
  imageLoading.value = true;
  try {
    attachments.value = await listAttachments({
      businessId: record.id,
      businessModule: '采购管理',
      businessType: '资源图片',
      page: 1,
      pageSize: 100,
    });
  } finally {
    imageLoading.value = false;
  }
}

const beforeUploadImage: UploadProps['beforeUpload'] = async (file) => {
  if (!currentResource.value) {
    message.warning('请先选择资源');
    return false;
  }
  if (!file.type?.startsWith('image/')) {
    message.warning('资源图片只支持图片文件');
    return false;
  }
  const formData = new FormData();
  formData.append('file', file as File);
  formData.append('businessModule', '采购管理');
  formData.append('businessType', '资源图片');
  formData.append('businessId', String(currentResource.value.id));

  uploadingImage.value = true;
  try {
    await uploadAttachment(formData);
    message.success('资源图片已上传');
    await loadResourceImages(currentResource.value);
  } finally {
    uploadingImage.value = false;
  }
  return false;
};

async function previewAttachment(record: AttachmentApi.Attachment) {
  try {
    const blob = await downloadAttachment(record.fileUrl);
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value);
    }
    previewUrl.value = URL.createObjectURL(blob);
    previewOpen.value = true;
  } catch {
    message.error('图片加载失败');
  }
}

onMounted(loadData);
</script>

<template>
  <Page title="资源总览" description="维护景区、酒店、餐厅、购物等资源主档，并查看供应商绑定情况。">
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
      </BusinessSearchForm>

      <Table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1500 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'resourceType'">
            <Tag :color="typeColor(record.resourceType)">
              {{ typeLabel(record.resourceType) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'area'">
            {{ areaText(record) }}
          </template>
          <template v-else-if="column.key === 'resourceName'">
            <div class="resource-name">{{ record.resourceName }}</div>
            <div class="muted">{{ record.address || '-' }}</div>
            <Tag :color="statusColor(record.status)" class="mt-1">
              {{ statusLabel(record.status) }}
            </Tag>
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
              <Button type="link" size="small" @click="openBindingDrawer(record)">绑定供应商</Button>
              <Button type="link" size="small" @click="openEditModal(record)">编辑</Button>
              <Button type="link" size="small" @click="openImageDrawer(record)">资源图片</Button>
              <Button danger type="link" size="small" @click="confirmDelete(record)">删除</Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑资源' : '新增资源'"
      width="760px"
      ok-text="保存"
      cancel-text="取消"
      @ok="saveRecord"
    >
      <Form :model="formState" layout="vertical">
        <div class="modal-grid">
          <Form.Item label="资源分类" required>
            <Select v-model:value="formState.resourceType" :options="resourceTypeOptions" />
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
          <Form.Item label="传真号码">
            <Input v-model:value="formState.fax" allow-clear />
          </Form.Item>
          <Form.Item label="状态">
            <Select v-model:value="formState.status" :options="statusOptions" />
          </Form.Item>
        </div>
        <Form.Item label="所在地址">
          <Input v-model:value="formState.address" allow-clear />
        </Form.Item>
        <Form.Item label="温馨提示">
          <Textarea v-model:value="formState.warmTip" :rows="3" />
        </Form.Item>
        <Form.Item label="简介">
          <Textarea v-model:value="formState.introduction" :rows="3" />
        </Form.Item>
        <Form.Item label="备注">
          <Textarea v-model:value="formState.remark" :rows="2" />
        </Form.Item>
        <Form.Item v-if="!editingId">
          <Checkbox v-model:checked="formState.autoCreateSupplier">
            自动创建同名供应商并建立对应关系
          </Checkbox>
        </Form.Item>
      </Form>
    </Modal>

    <Drawer
      v-model:open="bindingDrawerOpen"
      width="760"
      :title="`供应商绑定 - ${currentResource?.resourceName || ''}`"
    >
      <Card size="small" class="mb-4">
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
          </div>
          <Button type="primary" @click="bindSupplier">绑定供应商</Button>
        </Form>
      </Card>

      <Table
        :data-source="bindings"
        :loading="bindingLoading"
        :pagination="false"
        row-key="relationId"
        size="small"
      >
        <Table.Column title="供应商" data-index="supplierName" key="supplierName" />
        <Table.Column title="状态" data-index="status" key="status" width="90">
          <template #default="{ record }">
            <Tag :color="record.status === 'active' ? 'green' : 'default'">
              {{ record.status === 'active' ? '有效' : record.status }}
            </Tag>
          </template>
        </Table.Column>
      </Table>
    </Drawer>

    <Drawer
      v-model:open="imageDrawerOpen"
      width="680"
      :title="`资源图片 - ${currentResource?.resourceName || ''}`"
    >
      <Upload :before-upload="beforeUploadImage" :show-upload-list="false">
        <Button type="primary" :loading="uploadingImage">上传资源图片</Button>
      </Upload>
      <Table
        class="mt-4"
        :data-source="attachments"
        :loading="imageLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <Table.Column title="文件名" data-index="originalFilename" key="originalFilename" />
        <Table.Column title="上传人" data-index="uploadedBy" key="uploadedBy" width="110" />
        <Table.Column title="上传时间" data-index="createdAt" key="createdAt" width="160">
          <template #default="{ record }">
            {{ formatDate(record.createdAt) }}
          </template>
        </Table.Column>
        <Table.Column title="操作" key="action" width="90">
          <template #default="{ record }">
            <Button type="link" size="small" @click="previewAttachment(record)">预览</Button>
          </template>
        </Table.Column>
      </Table>
    </Drawer>

    <Modal v-model:open="previewOpen" :footer="null" :title="previewTitle" width="760px">
      <Image v-if="previewUrl" :src="previewUrl" class="preview-image" />
    </Modal>
  </Page>
</template>

<style scoped>
.modal-grid,
.bind-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
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

.preview-image {
  max-height: 70vh;
  object-fit: contain;
  width: 100%;
}

@media (max-width: 720px) {
  .modal-grid,
  .bind-grid {
    grid-template-columns: 1fr;
  }
}
</style>
