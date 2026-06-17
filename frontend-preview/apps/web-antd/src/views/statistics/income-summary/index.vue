<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '月份', dataIndex: 'month', key: 'month' },
  { title: '现金', dataIndex: 'cash', key: 'cash' },
  { title: '银行转账', dataIndex: 'bank', key: 'bank' },
  { title: '支付宝', dataIndex: 'alipay', key: 'alipay' },
  { title: '微信', dataIndex: 'wechat', key: 'wechat' },
  { title: '合计', dataIndex: 'total', key: 'total' },
  { title: '同比', dataIndex: 'yoy', key: 'yoy' },
];

const data = ref([
  { id: 1, month: '2024-01', cash: '¥85,000', bank: '¥520,000', alipay: '¥156,000', wechat: '¥95,000', total: '¥856,000', yoy: '+12.5%' },
  { id: 2, month: '2024-02', cash: '¥72,000', bank: '¥438,000', alipay: '¥132,000', wechat: '¥78,000', total: '¥720,000', yoy: '+8.3%' },
  { id: 3, month: '2024-03', cash: '¥124,000', bank: '¥756,000', alipay: '¥228,000', wechat: '¥132,000', total: '¥1,240,000', yoy: '+15.2%' },
  { id: 4, month: '2024-04', cash: '¥158,000', bank: '¥962,000', alipay: '¥285,000', wechat: '¥175,000', total: '¥1,580,000', yoy: '+18.6%' },
  { id: 5, month: '2024-05', cash: '¥192,000', bank: '¥1,170,000', alipay: '¥348,000', wechat: '¥210,000', total: '¥1,920,000', yoy: '+22.1%' },
  { id: 6, month: '2024-06', cash: '¥228,000', bank: '¥1,390,000', alipay: '¥412,000', wechat: '¥250,000', total: '¥2,280,000', yoy: '+25.8%' },
]);
</script>

<template>
  <Page title="收款汇总统计" description="汇总统计收款情况">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <DatePicker.RangePicker />
        <Button type="primary">
          <template #icon><span class="i-ant-design:export-outlined" /></template>
          导出
        </Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'yoy'">
            <Tag :color="record.yoy.startsWith('+') ? 'green' : 'red'">
              {{ record.yoy }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
