<script lang="ts" setup>
import type { RegionPath } from '#/utils/region';

import { Page } from '@vben/common-ui';

import { Button, Card, Cascader, Form, Input, InputNumber, Select, Space, Spin, message } from 'ant-design-vue';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getProductDictionaryAll } from '#/api/enterprise/product-dictionary';
import {
  createSalesProductDesignerDraft,
  getSalesProductDesignerDraftDetail,
  type SalesProductDesignerApi,
  updateSalesProductDesignerDraft,
} from '#/api/sales/product-designer';
import { buildRegionOptions, buildRegionPath, splitRegionPath } from '#/utils/region';

type Option = { label: string; value: string };

const route = useRoute();
const router = useRouter();
const regionOptions = buildRegionOptions();
const draftId = computed(() => {
  const value = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id;
  return value ? Number(value) : undefined;
});
const pageTitle = computed(() => (draftId.value ? '修改产品设计信息' : '新建产品设计'));

const loading = ref(false);
const saving = ref(false);
const dictionaryLoading = ref(false);
const regionPath = ref<RegionPath>([]);
const businessTypeOptions = ref<Option[]>([]);
const receptionStandardOptions = ref<Option[]>([]);
const productThemeOptions = ref<Option[]>([]);
const formState = reactive<SalesProductDesignerApi.DraftSaveRequest>({
  domesticInternational: 'domestic',
  productName: '',
  travelDays: 1,
});

async function loadDictionaries() {
  dictionaryLoading.value = true;
  try {
    const [businessTypes, standards, themes] = await Promise.all([
      getProductDictionaryAll('business_type'),
      getProductDictionaryAll('reception_standard'),
      getProductDictionaryAll('product_theme'),
    ]);
    businessTypeOptions.value = businessTypes.map((item) => ({ label: item.dictName, value: item.dictName }));
    receptionStandardOptions.value = standards.map((item) => ({ label: item.dictName, value: item.dictName }));
    productThemeOptions.value = themes.map((item) => ({ label: item.dictName, value: item.dictName }));
  } finally {
    dictionaryLoading.value = false;
  }
}

async function loadDraft() {
  if (!draftId.value) return;
  loading.value = true;
  try {
    const draft = await getSalesProductDesignerDraftDetail(draftId.value);
    Object.assign(formState, {
      businessType: draft.businessType,
      domesticInternational: draft.domesticInternational || 'domestic',
      productName: draft.productName,
      productTheme: draft.productTheme,
      receptionStandard: draft.receptionStandard,
      remark: draft.remark,
      travelDays: draft.travelDays || 1,
    });
    regionPath.value = buildRegionPath(draft.province, draft.city, draft.district);
  } finally {
    loading.value = false;
  }
}

function back() {
  router.push('/sales/product/designer');
}

async function save() {
  if (!formState.productName.trim()) {
    message.warning('请填写产品名称');
    return;
  }
  if (!formState.travelDays || formState.travelDays < 1) {
    message.warning('旅游天数不能小于1');
    return;
  }
  saving.value = true;
  try {
    const payload: SalesProductDesignerApi.DraftSaveRequest = {
      ...formState,
      ...splitRegionPath(regionPath.value),
      productName: formState.productName.trim(),
    };
    const saved = draftId.value
      ? await updateSalesProductDesignerDraft(draftId.value, payload)
      : await createSalesProductDesignerDraft(payload);
    message.success(draftId.value ? '设计基本信息已更新' : '产品设计草稿已创建');
    await router.push(`/sales/product/designer/${saved.id}`);
  } finally {
    saving.value = false;
  }
}

onMounted(() => Promise.all([loadDictionaries(), loadDraft()]));
</script>

<template>
  <Page :title="pageTitle" description="先建立产品设计草稿，再进入地图工作台编排资源。">
    <Spin :spinning="loading">
      <Card class="draft-form-card">
        <div class="form-header">
          <div>
            <div class="form-title">设计基本信息</div>
            <div class="form-subtitle">未完成设计前，该数据不会进入产品管理。</div>
          </div>
          <Space>
            <Button @click="back">返回</Button>
            <Button type="primary" :loading="saving" @click="save">保存并进入工作台</Button>
          </Space>
        </div>

        <Form :model="formState" layout="vertical" class="draft-form">
          <div class="form-grid">
            <Form.Item label="产品名称" required class="full-row">
              <Input v-model:value="formState.productName" :maxlength="200" placeholder="例如：杭州西湖乌镇三日游" />
            </Form.Item>
            <Form.Item label="业务类型">
              <Select
                v-model:value="formState.businessType"
                allow-clear
                :loading="dictionaryLoading"
                :options="businessTypeOptions"
                placeholder="请选择业务类型"
              />
            </Form.Item>
            <Form.Item label="国内/国际">
              <Select
                v-model:value="formState.domesticInternational"
                :options="[
                  { label: '国内', value: 'domestic' },
                  { label: '国际', value: 'international' },
                ]"
              />
            </Form.Item>
            <Form.Item label="接团城市">
              <Cascader
                v-model:value="regionPath"
                allow-clear
                change-on-select
                :options="regionOptions"
                placeholder="可选择省 / 市 / 区县"
              />
            </Form.Item>
            <Form.Item label="旅游天数" required>
              <InputNumber v-model:value="formState.travelDays" :min="1" :max="60" style="width: 100%" />
            </Form.Item>
            <Form.Item label="接待标准">
              <Select
                v-model:value="formState.receptionStandard"
                allow-clear
                :loading="dictionaryLoading"
                :options="receptionStandardOptions"
                placeholder="请选择接待标准"
              />
            </Form.Item>
            <Form.Item label="产品主题">
              <Select
                v-model:value="formState.productTheme"
                allow-clear
                :loading="dictionaryLoading"
                :options="productThemeOptions"
                placeholder="请选择产品主题"
              />
            </Form.Item>
            <Form.Item label="设计备注" class="full-row">
              <Input.TextArea v-model:value="formState.remark" :rows="4" :maxlength="1000" placeholder="记录这份产品设计的内部思路" />
            </Form.Item>
          </div>
        </Form>
      </Card>
    </Spin>
  </Page>
</template>

<style scoped>
.draft-form-card { max-width: 1040px; margin: 0 auto; }
.form-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.form-title { color: #0f172a; font-size: 16px; font-weight: 600; }
.form-subtitle { margin-top: 4px; color: #64748b; font-size: 12px; }
.draft-form { margin-top: 20px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 20px; }
.full-row { grid-column: 1 / -1; }
@media (max-width: 760px) {
  .form-header { align-items: stretch; flex-direction: column; }
  .form-grid { grid-template-columns: 1fr; }
  .full-row { grid-column: auto; }
}
</style>
