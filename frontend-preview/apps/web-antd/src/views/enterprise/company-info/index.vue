<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Alert, Button, Card, Cascader, Form, Input, Select, Space, message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';

import {
  getEnterpriseCompanyInfoCurrent,
  saveEnterpriseCompanyInfo,
  type EnterpriseCompanyInfoApi,
} from '#/api/enterprise/company-info';
import {
  buildRegionOptions,
  buildRegionPath,
  splitRegionPath,
  type RegionPath,
} from '#/utils/region';

const statusOptions: Array<{
  label: string;
  value: EnterpriseCompanyInfoApi.Status;
}> = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'disabled' },
];

const signStatusOptions: Array<{
  label: string;
  value: EnterpriseCompanyInfoApi.SignStatus;
}> = [
  { label: '未签约', value: 'unsigned' },
  { label: '已签约', value: 'signed' },
];

const loading = ref(false);
const saving = ref(false);
const hasCompanyInfo = ref(false);
const regionOptions = buildRegionOptions();
const formRegionPath = ref<RegionPath>([]);

const formState = reactive<EnterpriseCompanyInfoApi.SaveParams>({
  alipayAccount: '',
  alipayEnterpriseName: '',
  alipayNickname: '',
  city: '',
  companyName: '',
  contactName: '',
  contactPhone: '',
  district: '',
  faxNumber: '',
  officeAddress: '',
  province: '',
  remark: '',
  signLink: '',
  signStatus: 'unsigned',
  status: 'active',
});

async function loadCompanyInfo() {
  loading.value = true;
  try {
    const detail = await getEnterpriseCompanyInfoCurrent();
    hasCompanyInfo.value = Boolean(detail?.id);
    if (!detail) {
      formRegionPath.value = [];
      return;
    }
    formRegionPath.value = buildRegionPath(
      detail.province,
      detail.city,
      detail.district,
    );
    Object.assign(formState, {
      alipayAccount: detail.alipayAccount || '',
      alipayEnterpriseName: detail.alipayEnterpriseName || '',
      alipayNickname: detail.alipayNickname || '',
      city: detail.city || '',
      companyName: detail.companyName || '',
      contactName: detail.contactName || '',
      contactPhone: detail.contactPhone || '',
      district: detail.district || '',
      faxNumber: detail.faxNumber || '',
      officeAddress: detail.officeAddress || '',
      province: detail.province || '',
      remark: detail.remark || '',
      signLink: detail.signLink || '',
      signStatus: detail.signStatus || 'unsigned',
      status: detail.status || 'active',
    });
  } finally {
    loading.value = false;
  }
}

function clean(value?: string) {
  const result = value?.trim();
  return result || undefined;
}

function buildSaveParams(): EnterpriseCompanyInfoApi.SaveParams {
  const regionFields = splitRegionPath(formRegionPath.value);
  return {
    alipayAccount: clean(formState.alipayAccount),
    alipayEnterpriseName: clean(formState.alipayEnterpriseName),
    alipayNickname: clean(formState.alipayNickname),
    city: regionFields.city,
    companyName: formState.companyName.trim(),
    contactName: clean(formState.contactName),
    contactPhone: clean(formState.contactPhone),
    district: regionFields.district,
    faxNumber: clean(formState.faxNumber),
    officeAddress: clean(formState.officeAddress),
    province: regionFields.province,
    remark: clean(formState.remark),
    signLink: clean(formState.signLink),
    signStatus: formState.signStatus || 'unsigned',
    status: formState.status || 'active',
  };
}

async function saveCompanyInfo() {
  if (!formState.companyName?.trim()) {
    message.warning('请填写公司名称');
    return;
  }
  saving.value = true;
  try {
    await saveEnterpriseCompanyInfo(buildSaveParams());
    hasCompanyInfo.value = true;
    message.success('公司信息已保存');
  } finally {
    saving.value = false;
  }
}

onMounted(loadCompanyInfo);
</script>

<template>
  <Page
    title="公司信息"
    description="维护本企业正式主体资料，客户合同甲方信息会优先从这里带出"
  >
    <Card :loading="loading">
      <Alert
        class="company-info-alert"
        message="合同表单会默认带入公司信息；如果这里还没维护，合同甲方仍可手工填写，不会阻断保存。"
        show-icon
        type="info"
      />

      <Form
        class="company-info-form"
        :model="formState"
        layout="vertical"
      >
        <div class="company-info-section-title">基础主体信息</div>
        <div class="company-info-grid">
          <Form.Item label="公司名称" required>
            <Input
              v-model:value="formState.companyName"
              :maxlength="200"
              placeholder="请输入公司正式名称"
            />
          </Form.Item>
          <Form.Item label="资料状态">
            <Select
              v-model:value="formState.status"
              :options="statusOptions"
              placeholder="请选择状态"
            />
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
          <Form.Item label="联系人">
            <Input v-model:value="formState.contactName" :maxlength="80" />
          </Form.Item>
          <Form.Item label="联系电话">
            <Input v-model:value="formState.contactPhone" :maxlength="40" />
          </Form.Item>
          <Form.Item label="传真">
            <Input v-model:value="formState.faxNumber" :maxlength="40" />
          </Form.Item>
        </div>
        <Form.Item label="办公地址">
          <Input
            v-model:value="formState.officeAddress"
            :maxlength="300"
            placeholder="请输入公司办公地址"
          />
        </Form.Item>

        <div class="company-info-section-title">企业支付宝资料</div>
        <div class="company-info-grid">
          <Form.Item label="企业支付宝主体">
            <Input
              v-model:value="formState.alipayEnterpriseName"
              :maxlength="200"
            />
          </Form.Item>
          <Form.Item label="企业支付宝账号">
            <Input
              v-model:value="formState.alipayAccount"
              :maxlength="160"
            />
          </Form.Item>
          <Form.Item label="企业支付宝昵称">
            <Input
              v-model:value="formState.alipayNickname"
              :maxlength="120"
            />
          </Form.Item>
          <Form.Item label="签约状态">
            <Select
              v-model:value="formState.signStatus"
              :options="signStatusOptions"
            />
          </Form.Item>
        </div>
        <Form.Item label="签约链接">
          <Input
            v-model:value="formState.signLink"
            placeholder="可填写签约页面或资料地址"
          />
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 3, maxRows: 5 }"
            placeholder="填写公司资料维护说明"
          />
        </Form.Item>

        <div class="company-info-actions">
          <Space>
            <Button @click="loadCompanyInfo">重新加载</Button>
            <Button type="primary" :loading="saving" @click="saveCompanyInfo">
              {{ hasCompanyInfo ? '保存公司信息' : '新增公司信息' }}
            </Button>
          </Space>
        </div>
      </Form>
    </Card>
  </Page>
</template>

<style scoped>
.company-info-alert {
  margin-bottom: 16px;
}

.company-info-form {
  max-width: 1100px;
}

.company-info-section-title {
  padding-left: 10px;
  margin: 18px 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  border-left: 3px solid #1677ff;
}

.company-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  column-gap: 18px;
}

.company-info-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .company-info-grid {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 640px) {
  .company-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
