<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Table, Tabs } from 'ant-design-vue';
import { ref } from 'vue';

const activeTab = ref('arrangement');

const columns = [
  { title: '资源名称', dataIndex: 'name', key: 'name' },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier' },
  { title: '采购次数', dataIndex: 'count', key: 'count' },
  { title: '采购金额', dataIndex: 'amount', key: 'amount' },
  { title: '人次', dataIndex: 'personCount', key: 'personCount' },
  { title: '均价', dataIndex: 'avgPrice', key: 'avgPrice' },
];

const data = ref([
  { id: 1, name: '九寨沟门票', supplier: '九寨沟景区管理局', count: 86, amount: '¥430,000', personCount: 2150, avgPrice: '¥200' },
  { id: 2, name: '峨眉山门票', supplier: '峨眉山旅游公司', count: 72, amount: '¥252,000', personCount: 1680, avgPrice: '¥150' },
  { id: 3, name: '成都锦江酒店', supplier: '锦江酒店集团', count: 120, amount: '¥720,000', personCount: 3600, avgPrice: '¥200' },
  { id: 4, name: '都江堰景区', supplier: '都江堰管理处', count: 58, amount: '¥145,000', personCount: 1450, avgPrice: '¥100' },
  { id: 5, name: '旅游大巴(45座)', supplier: '蜀都客运公司', count: 95, amount: '¥570,000', personCount: 4275, avgPrice: '¥133' },
  { id: 6, name: '川菜团餐', supplier: '巴蜀餐饮集团', count: 180, amount: '¥540,000', personCount: 5400, avgPrice: '¥100' },
  { id: 7, name: '乐山大佛门票', supplier: '乐山大佛景区', count: 45, amount: '¥135,000', personCount: 1350, avgPrice: '¥100' },
  { id: 8, name: '青城山门票', supplier: '青城山管理处', count: 52, amount: '¥130,000', personCount: 1300, avgPrice: '¥100' },
]);
</script>

<template>
  <Page title="资源采购统计" description="统计资源采购情况">
    <Card>
      <Tabs v-model:activeKey="activeTab">
        <Tabs.TabPane key="arrangement" tab="排团统计" />
        <Tabs.TabPane key="guide" tab="导游统计" />
        <Tabs.TabPane key="coordinator" tab="计调统计" />
        <Tabs.TabPane key="finance" tab="财务统计" />
      </Tabs>
      <div class="mb-4 flex items-center gap-3">
        <DatePicker.RangePicker />
        <Button type="primary">
          <template #icon><span class="i-ant-design:export-outlined" /></template>
          导出
        </Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id" />
    </Card>
  </Page>
</template>
