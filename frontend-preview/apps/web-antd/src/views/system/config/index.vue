<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, Form, Input, InputNumber, Space, Spin, Switch, message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';

import {
  getAiConfig,
  getAuthConfig,
  getBusinessRiskConfig,
  getMapConfig,
  updateAiConfig,
  updateAuthConfig,
  updateBusinessRiskConfig,
  updateMapConfig,
} from '#/api/system/config';

const loading = ref(false);
const riskSaving = ref(false);
const aiSaving = ref(false);
const mapSaving = ref(false);
const authSaving = ref(false);

const riskForm = reactive({
  customerRiskApprovalEnabled: false,
});

const aiForm = reactive({
  apiKey: '',
  apiKeyMasked: '',
  provider: 'aliyun_bailian' as const,
  textModel: 'qwen-plus',
  visionModel: 'qwen-vl-ocr-latest',
});

const mapForm = reactive({
  jsKey: '',
  jsKeyMasked: '',
  jsSecurityCode: '',
  jsSecurityCodeMasked: '',
  webServiceKey: '',
  webServiceKeyMasked: '',
});

const authForm = reactive({
  idleTimeoutMinutes: 120,
});

async function loadConfig() {
  loading.value = true;
  try {
    const [risk, ai, map, auth] = await Promise.all([
      getBusinessRiskConfig(),
      getAiConfig(),
      getMapConfig(),
      getAuthConfig(),
    ]);
    riskForm.customerRiskApprovalEnabled = risk.customerRiskApprovalEnabled;
    aiForm.provider = ai.provider || 'aliyun_bailian';
    aiForm.apiKeyMasked = ai.apiKeyMasked || '';
    aiForm.textModel = ai.textModel || 'qwen-plus';
    aiForm.visionModel = ai.visionModel || 'qwen-vl-ocr-latest';
    mapForm.webServiceKeyMasked = map.webServiceKeyMasked || '';
    mapForm.jsKeyMasked = map.jsKeyMasked || '';
    mapForm.jsSecurityCodeMasked = map.jsSecurityCodeMasked || '';
    authForm.idleTimeoutMinutes = auth.idleTimeoutMinutes || 120;
  } finally {
    loading.value = false;
  }
}

async function saveBusinessRiskConfig() {
  riskSaving.value = true;
  try {
    await updateBusinessRiskConfig({
      customerRiskApprovalEnabled: riskForm.customerRiskApprovalEnabled,
    });
    message.success('业务风控配置已保存');
  } finally {
    riskSaving.value = false;
  }
}

async function saveAiConfig() {
  aiSaving.value = true;
  try {
    const result = await updateAiConfig({
      apiKey: aiForm.apiKey || undefined,
      provider: aiForm.provider,
      textModel: aiForm.textModel,
      visionModel: aiForm.visionModel,
    });
    aiForm.apiKey = '';
    aiForm.apiKeyMasked = result.apiKeyMasked || aiForm.apiKeyMasked;
    message.success('百炼配置已保存');
  } finally {
    aiSaving.value = false;
  }
}

async function saveMapConfig() {
  mapSaving.value = true;
  try {
    const result = await updateMapConfig({
      jsKey: mapForm.jsKey || undefined,
      jsSecurityCode: mapForm.jsSecurityCode || undefined,
      webServiceKey: mapForm.webServiceKey || undefined,
    });
    mapForm.webServiceKey = '';
    mapForm.jsKey = '';
    mapForm.jsSecurityCode = '';
    mapForm.webServiceKeyMasked = result.webServiceKeyMasked || mapForm.webServiceKeyMasked;
    mapForm.jsKeyMasked = result.jsKeyMasked || mapForm.jsKeyMasked;
    mapForm.jsSecurityCodeMasked = result.jsSecurityCodeMasked || mapForm.jsSecurityCodeMasked;
    message.success('高德地图配置已保存');
  } finally {
    mapSaving.value = false;
  }
}

async function saveAuthConfig() {
  authSaving.value = true;
  try {
    await updateAuthConfig({
      idleTimeoutMinutes: authForm.idleTimeoutMinutes,
    });
    message.success('登录安全配置已保存');
  } finally {
    authSaving.value = false;
  }
}

onMounted(() => {
  loadConfig();
});
</script>

<template>
  <Page title="系统配置">
    <Spin :spinning="loading">
      <div class="system-config-page">
        <Card title="业务风控" :bordered="false">
          <Form layout="vertical">
            <Form.Item label="客户授信审批">
              <Switch v-model:checked="riskForm.customerRiskApprovalEnabled" checked-children="开启" un-checked-children="关闭" />
            </Form.Item>
            <div class="config-action-row">
              <Button type="primary" :loading="riskSaving" @click="saveBusinessRiskConfig">
                <IconifyIcon icon="lucide:save" />
                保存业务风控
              </Button>
            </div>
          </Form>
        </Card>

        <Card title="百炼配置" :bordered="false">
          <Form layout="vertical" class="config-grid">
            <Form.Item label="服务商">
              <Input v-model:value="aiForm.provider" disabled />
            </Form.Item>
            <Form.Item label="当前 API Key">
              <Input :value="aiForm.apiKeyMasked || '未配置'" disabled />
            </Form.Item>
            <Form.Item label="新 API Key">
              <Input.Password v-model:value="aiForm.apiKey" placeholder="留空则不覆盖已有 Key" />
            </Form.Item>
            <Form.Item label="文本模型">
              <Input v-model:value="aiForm.textModel" />
            </Form.Item>
            <Form.Item label="视觉模型">
              <Input v-model:value="aiForm.visionModel" />
            </Form.Item>
            <div class="config-action-row">
              <Button type="primary" :loading="aiSaving" @click="saveAiConfig">
                <IconifyIcon icon="lucide:save" />
                保存百炼配置
              </Button>
            </div>
          </Form>
        </Card>

        <Card title="高德地图" :bordered="false">
          <Form layout="vertical" class="config-grid">
            <Form.Item label="当前 Web 服务 Key">
              <Input :value="mapForm.webServiceKeyMasked || '未配置'" disabled />
            </Form.Item>
            <Form.Item label="新 Web 服务 Key">
              <Input.Password v-model:value="mapForm.webServiceKey" placeholder="留空则不覆盖已有 Key" />
            </Form.Item>
            <Form.Item label="当前 JS Key">
              <Input :value="mapForm.jsKeyMasked || '未配置'" disabled />
            </Form.Item>
            <Form.Item label="新 JS Key">
              <Input.Password v-model:value="mapForm.jsKey" placeholder="留空则不覆盖已有 Key" />
            </Form.Item>
            <Form.Item label="当前安全密钥">
              <Input :value="mapForm.jsSecurityCodeMasked || '未配置'" disabled />
            </Form.Item>
            <Form.Item label="新安全密钥">
              <Input.Password v-model:value="mapForm.jsSecurityCode" placeholder="留空则不覆盖已有 Key" />
            </Form.Item>
            <div class="config-action-row">
              <Button type="primary" :loading="mapSaving" @click="saveMapConfig">
                <IconifyIcon icon="lucide:save" />
                保存高德配置
              </Button>
            </div>
          </Form>
        </Card>

        <Card title="登录安全" :bordered="false">
          <Form layout="vertical" class="config-grid">
            <Form.Item label="无操作自动退出时间（分钟）">
              <InputNumber v-model:value="authForm.idleTimeoutMinutes" :min="5" :max="1440" class="full-input" />
            </Form.Item>
            <div class="config-action-row">
              <Space>
                <Button @click="loadConfig">刷新</Button>
                <Button type="primary" :loading="authSaving" @click="saveAuthConfig">
                  <IconifyIcon icon="lucide:save" />
                  保存登录安全
                </Button>
              </Space>
            </div>
          </Form>
        </Card>
      </div>
    </Spin>
  </Page>
</template>

<style scoped>
.system-config-page {
  display: grid;
  gap: 12px;
}

.system-config-page :deep(.ant-card) {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 6px 18px rgb(15 23 42 / 4%);
}

.system-config-page :deep(.ant-card-head) {
  min-height: 44px;
  background: #f8fafc;
  border-bottom-color: #e2e8f0;
}

.system-config-page :deep(.ant-card-head-title) {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
}

.config-grid :deep(.ant-form-item) {
  margin-bottom: 0;
}

.config-action-row {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  min-height: 32px;
}

.config-action-row :deep(.ant-btn) {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.full-input {
  width: 100%;
}

@media (max-width: 1000px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
