<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Form, Input, InputNumber, Radio, Switch, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const platform = ref('esign');
const appKey = ref('YNITS_2024_KEY');
const appSecret = ref('••••••••••••••••');
const autoSend = ref(true);
const signValidDays = ref(7);
const remindBeforeDays = ref(2);
const smsRemind = ref(true);
const emailRemind = ref(true);

const templateColumns = [
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '适用类型', dataIndex: 'type', key: 'type' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime' },
  { title: '操作', key: 'action', width: 200 },
];

const templateData = ref([
  { key: '1', name: '国内团队旅游合同', type: '团队合同', status: '启用', updateTime: '2024-03-10' },
  { key: '2', name: '散客旅游合同', type: '散客合同', status: '启用', updateTime: '2024-03-08' },
  { key: '3', name: '包车服务协议', type: '服务协议', status: '启用', updateTime: '2024-02-28' },
  { key: '4', name: '导游服务合同', type: '劳务合同', status: '停用', updateTime: '2024-02-15' },
  { key: '5', name: '酒店预订确认书', type: '确认书', status: '启用', updateTime: '2024-03-05' },
]);
</script>

<template>
  <Page title="电子合同配置" description="配置电子合同签署参数">
    <Card title="签署平台配置" class="mb-4">
      <Form layout="vertical">
        <Form.Item label="签署平台">
          <Radio.Group v-model:value="platform">
            <Radio.Button value="esign">e签宝</Radio.Button>
            <Radio.Button value="fadada">法大大</Radio.Button>
            <Radio.Button value="junzi">君子签</Radio.Button>
          </Radio.Group>
        </Form.Item>
        <div class="grid grid-cols-2 gap-4">
          <Form.Item label="AppKey">
            <Input v-model:value="appKey" />
          </Form.Item>
          <Form.Item label="AppSecret">
            <Input.Password v-model:value="appSecret" />
          </Form.Item>
        </div>
        <Form.Item>
          <Button type="primary" class="mr-2">保存配置</Button>
          <Button>测试连接</Button>
        </Form.Item>
      </Form>
    </Card>

    <Card title="签署规则" class="mb-4">
      <Form layout="vertical">
        <div class="grid grid-cols-2 gap-4">
          <Form.Item label="合同自动发送">
            <Switch v-model:checked="autoSend" />
          </Form.Item>
          <Form.Item label="签署有效期(天)">
            <InputNumber v-model:value="signValidDays" :min="1" :max="30" class="w-full" />
          </Form.Item>
          <Form.Item label="到期前提醒(天)">
            <InputNumber v-model:value="remindBeforeDays" :min="1" :max="7" class="w-full" />
          </Form.Item>
          <Form.Item label="短信提醒">
            <Switch v-model:checked="smsRemind" />
          </Form.Item>
          <Form.Item label="邮件提醒">
            <Switch v-model:checked="emailRemind" />
          </Form.Item>
        </div>
      </Form>
    </Card>

    <Card title="模板管理">
      <div class="mb-4 flex justify-end">
        <Button type="primary">新增模板</Button>
      </div>
      <Table :columns="templateColumns" :data-source="templateData" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '启用' ? 'green' : 'red'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">编辑</Button>
            <Button type="link" size="small">预览</Button>
            <Button type="link" size="small" danger>删除</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
