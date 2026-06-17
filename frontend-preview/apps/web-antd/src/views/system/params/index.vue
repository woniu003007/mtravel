<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Form, Input, InputNumber, Switch } from 'ant-design-vue';
import { ref } from 'vue';

const formState = ref({
  companyName: '云南国际旅行社有限公司',
  contactPhone: '0871-63516888',
  address: '云南省昆明市官渡区春城路289号',
  fax: '0871-63516999',
  email: 'service@ynits.com',
  website: 'www.ynits.com',
  defaultProfitRate: 15,
  orderLockDays: 3,
  groupPrefix: 'YN',
  defaultCurrency: 'CNY',
  priceDecimalPlaces: 2,
  autoConfirmHours: 24,
  invoiceTaxRate: 6,
  approvalAmount: 10000,
  paymentDueDays: 30,
  invoicePrefix: 'INV',
  costAlertRate: 90,
  budgetWarningRate: 80,
  smsNotify: true,
  emailNotify: true,
  wechatNotify: false,
  orderNotify: true,
  paymentNotify: true,
  approvalNotify: true,
  dailyReport: true,
});
</script>

<template>
  <Page title="系统参数配置" description="配置系统运行参数">
    <Form layout="vertical" :model="formState">
      <Card title="基本设置" class="mb-4">
        <div class="grid grid-cols-2 gap-4">
          <Form.Item label="公司名称">
            <Input v-model:value="formState.companyName" />
          </Form.Item>
          <Form.Item label="联系电话">
            <Input v-model:value="formState.contactPhone" />
          </Form.Item>
          <Form.Item label="公司地址">
            <Input v-model:value="formState.address" />
          </Form.Item>
          <Form.Item label="传真号码">
            <Input v-model:value="formState.fax" />
          </Form.Item>
          <Form.Item label="邮箱地址">
            <Input v-model:value="formState.email" />
          </Form.Item>
          <Form.Item label="公司网站">
            <Input v-model:value="formState.website" />
          </Form.Item>
        </div>
      </Card>

      <Card title="业务设置" class="mb-4">
        <div class="grid grid-cols-2 gap-4">
          <Form.Item label="默认利润率(%)">
            <InputNumber v-model:value="formState.defaultProfitRate" :min="0" :max="100" class="w-full" />
          </Form.Item>
          <Form.Item label="订单自动锁定天数">
            <InputNumber v-model:value="formState.orderLockDays" :min="1" class="w-full" />
          </Form.Item>
          <Form.Item label="团号前缀">
            <Input v-model:value="formState.groupPrefix" />
          </Form.Item>
          <Form.Item label="默认币种">
            <Input v-model:value="formState.defaultCurrency" />
          </Form.Item>
          <Form.Item label="价格小数位数">
            <InputNumber v-model:value="formState.priceDecimalPlaces" :min="0" :max="4" class="w-full" />
          </Form.Item>
          <Form.Item label="自动确认时限(小时)">
            <InputNumber v-model:value="formState.autoConfirmHours" :min="1" class="w-full" />
          </Form.Item>
        </div>
      </Card>

      <Card title="财务设置" class="mb-4">
        <div class="grid grid-cols-2 gap-4">
          <Form.Item label="发票税率(%)">
            <InputNumber v-model:value="formState.invoiceTaxRate" :min="0" :max="100" class="w-full" />
          </Form.Item>
          <Form.Item label="付款审批金额(元)">
            <InputNumber v-model:value="formState.approvalAmount" :min="0" class="w-full" />
          </Form.Item>
          <Form.Item label="付款账期(天)">
            <InputNumber v-model:value="formState.paymentDueDays" :min="1" class="w-full" />
          </Form.Item>
          <Form.Item label="发票编号前缀">
            <Input v-model:value="formState.invoicePrefix" />
          </Form.Item>
          <Form.Item label="成本预警比例(%)">
            <InputNumber v-model:value="formState.costAlertRate" :min="0" :max="100" class="w-full" />
          </Form.Item>
          <Form.Item label="预算预警比例(%)">
            <InputNumber v-model:value="formState.budgetWarningRate" :min="0" :max="100" class="w-full" />
          </Form.Item>
        </div>
      </Card>

      <Card title="通知设置" class="mb-4">
        <div class="grid grid-cols-2 gap-4">
          <Form.Item label="短信通知">
            <Switch v-model:checked="formState.smsNotify" />
          </Form.Item>
          <Form.Item label="邮件通知">
            <Switch v-model:checked="formState.emailNotify" />
          </Form.Item>
          <Form.Item label="微信通知">
            <Switch v-model:checked="formState.wechatNotify" />
          </Form.Item>
          <Form.Item label="订单变更通知">
            <Switch v-model:checked="formState.orderNotify" />
          </Form.Item>
          <Form.Item label="付款到期通知">
            <Switch v-model:checked="formState.paymentNotify" />
          </Form.Item>
          <Form.Item label="审批提醒通知">
            <Switch v-model:checked="formState.approvalNotify" />
          </Form.Item>
          <Form.Item label="每日报表推送">
            <Switch v-model:checked="formState.dailyReport" />
          </Form.Item>
        </div>
      </Card>

      <div class="text-right">
        <Button type="primary" size="large">保存配置</Button>
      </div>
    </Form>
  </Page>
</template>
