<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Table, Tabs, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const activeTab = ref('receivable');

const columns = [
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '应收总额', dataIndex: 'totalAmount', key: 'totalAmount' },
  { title: '已收', dataIndex: 'received', key: 'received' },
  { title: '未收', dataIndex: 'unreceived', key: 'unreceived' },
  { title: '账龄(天)', dataIndex: 'agingDays', key: 'agingDays' },
  { title: '风险等级', dataIndex: 'riskLevel', key: 'riskLevel' },
];

const data = ref([
  { id: 1, customer: '北京阳光旅行社', totalAmount: '¥580,000', received: '¥450,000', unreceived: '¥130,000', agingDays: 15, riskLevel: '低' },
  { id: 2, customer: '上海春秋国旅', totalAmount: '¥920,000', received: '¥780,000', unreceived: '¥140,000', agingDays: 22, riskLevel: '低' },
  { id: 3, customer: '广州南湖国旅', totalAmount: '¥450,000', received: '¥280,000', unreceived: '¥170,000', agingDays: 45, riskLevel: '中' },
  { id: 4, customer: '深圳华侨城旅行社', totalAmount: '¥680,000', received: '¥400,000', unreceived: '¥280,000', agingDays: 60, riskLevel: '中' },
  { id: 5, customer: '杭州西湖国旅', totalAmount: '¥320,000', received: '¥120,000', unreceived: '¥200,000', agingDays: 90, riskLevel: '高' },
  { id: 6, customer: '重庆中旅', totalAmount: '¥260,000', received: '¥200,000', unreceived: '¥60,000', agingDays: 10, riskLevel: '低' },
  { id: 7, customer: '武汉长江国旅', totalAmount: '¥410,000', received: '¥210,000', unreceived: '¥200,000', agingDays: 75, riskLevel: '高' },
  { id: 8, customer: '南京金陵旅行社', totalAmount: '¥350,000', received: '¥300,000', unreceived: '¥50,000', agingDays: 8, riskLevel: '低' },
]);
</script>

<template>
  <Page title="账款统计" description="统计应收应付账款">
    <Card>
      <Tabs v-model:activeKey="activeTab">
        <Tabs.TabPane key="receivable" tab="应收汇总" />
        <Tabs.TabPane key="payable" tab="应付汇总" />
        <Tabs.TabPane key="aging" tab="账龄分析" />
        <Tabs.TabPane key="badDebt" tab="坏账预警" />
        <Tabs.TabPane key="receiveTrend" tab="收款趋势" />
        <Tabs.TabPane key="payTrend" tab="付款趋势" />
      </Tabs>
      <div class="mb-4 flex items-center gap-3">
        <DatePicker.RangePicker />
        <Button type="primary">
          <template #icon><span class="i-ant-design:export-outlined" /></template>
          导出
        </Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'riskLevel'">
            <Tag :color="record.riskLevel === '低' ? 'green' : record.riskLevel === '中' ? 'orange' : 'red'">
              {{ record.riskLevel }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
