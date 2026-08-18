<script lang="ts" setup>
import type { TablePaginationConfig } from 'ant-design-vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getProductDictionaryAll } from '#/api/enterprise/product-dictionary';
import {
  deleteSalesProductDesignerDraft,
  getSalesProductDesignerDraftPage,
  type SalesProductDesignerApi,
} from '#/api/sales/product-designer';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

const router = useRouter();
const loading = ref(false);
const dictionaryLoading = ref(false);
const drafts = ref<SalesProductDesignerApi.Draft[]>([]);
const businessTypes = ref<Array<{ label: string; value: string }>>([]);

const query = reactive<SalesProductDesignerApi.DraftQuery>({
  page: 1,
  pageSize: 20,
});

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 20,
  showSizeChanger: true,
  total: 0,
});

const columns = computed(() => [
  { dataIndex: 'productName', key: 'productName', title: '设计名称', width: 260 },
  { dataIndex: 'city', key: 'area', title: '接团城市', width: 190 },
  { dataIndex: 'travelDays', key: 'travelDays', title: '天数', width: 90 },
  { dataIndex: 'businessType', key: 'businessType', title: '业务类型', width: 130 },
  { dataIndex: 'createdBy', key: 'createdBy', title: '创建人', width: 110 },
  { dataIndex: 'updatedAt', key: 'updatedAt', title: '最后修改', width: 170 },
  { dataIndex: 'designStatus', key: 'designStatus', title: '设计状态', width: 100 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 260 },
]);

function areaText(item: Record<string, any>) {
  return [item.province, item.city, item.district].filter(Boolean).join(' / ') || '-';
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

async function loadDictionaries() {
  dictionaryLoading.value = true;
  try {
    const items = await getProductDictionaryAll('business_type');
    businessTypes.value = items.map((item) => ({ label: item.dictName, value: item.dictName }));
  } finally {
    dictionaryLoading.value = false;
  }
}

async function loadDrafts() {
  loading.value = true;
  try {
    const result = await getSalesProductDesignerDraftPage(query);
    drafts.value = result.items;
    pagination.current = query.page;
    pagination.pageSize = query.pageSize;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  loadDrafts();
}

function reset() {
  Object.assign(query, {
    businessType: undefined,
    city: undefined,
    keyword: undefined,
    page: 1,
    pageSize: query.pageSize,
  });
  loadDrafts();
}

function changePage(next: TablePaginationConfig) {
  query.page = Number(next.current || 1);
  query.pageSize = Number(next.pageSize || 20);
  loadDrafts();
}

function createDraft() {
  router.push('/sales/product/designer/create');
}

function editDraft(id: number) {
  router.push(`/sales/product/designer/edit/${id}`);
}

function openWorkbench(id: number) {
  router.push(`/sales/product/designer/${id}`);
}

function confirmDeleteDraft(record: Record<string, any>) {
  const draft = record as SalesProductDesignerApi.Draft;
  Modal.confirm({
    cancelText: '取消',
    content: '删除后，该草稿及尚未完成的设计内容将不再显示。正式产品不会受到影响。',
    okText: '删除草稿',
    okType: 'danger',
    title: `删除产品设计草稿「${draft.productName}」？`,
    async onOk() {
      await deleteSalesProductDesignerDraft(draft.id);
      message.success('产品设计草稿已删除');
      if (drafts.value.length === 1 && Number(query.page || 1) > 1) {
        query.page = Number(query.page) - 1;
      }
      await loadDrafts();
    },
  });
}

onMounted(() => {
  loadDictionaries();
  loadDrafts();
});
</script>

<template>
  <Page title="产品设计" description="管理未完成的产品设计草稿，完成设计后才进入产品管理。">
    <Card>
      <BusinessSearchForm
        label-width="86px"
        :model="query"
        :search-loading="loading"
        show-create
        @create="createDraft"
        @reset="reset"
        @search="search"
      >
        <Form.Item label="产品名称">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="产品名称 / 接团城市"
            @press-enter="search"
          />
        </Form.Item>
        <Form.Item label="业务类型">
          <Select
            v-model:value="query.businessType"
            allow-clear
            :loading="dictionaryLoading"
            :options="businessTypes"
            placeholder="请选择业务类型"
          />
        </Form.Item>
        <Form.Item label="接团城市">
          <Input v-model:value="query.city" allow-clear placeholder="请输入城市" @press-enter="search" />
        </Form.Item>
      </BusinessSearchForm>

      <Table
        :columns="columns"
        :data-source="drafts"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1260 }"
        size="small"
        @change="changePage"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'productName'">
            <div class="draft-name">{{ record.productName }}</div>
            <div class="muted">{{ record.productTheme || '主题未设置' }}</div>
          </template>
          <template v-else-if="column.key === 'area'">
            {{ areaText(record) }}
          </template>
          <template v-else-if="column.key === 'travelDays'">
            {{ record.travelDays }} 天
          </template>
          <template v-else-if="column.key === 'businessType'">
            <Tag v-if="record.businessType" color="purple">{{ record.businessType }}</Tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'updatedAt'">
            {{ formatDate(record.updatedAt) }}
          </template>
          <template v-else-if="column.key === 'designStatus'">
            <Tag color="blue">设计中</Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space size="small">
              <Button type="link" size="small" @click="openWorkbench(record.id)">进入工作台</Button>
              <Button type="link" size="small" @click="editDraft(record.id)">修改基本信息</Button>
              <Button danger type="link" size="small" @click="confirmDeleteDraft(record)">删除</Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>

<style scoped>
.draft-name { color: #0f172a; font-weight: 600; }
.muted { margin-top: 2px; color: #64748b; font-size: 12px; }
</style>
