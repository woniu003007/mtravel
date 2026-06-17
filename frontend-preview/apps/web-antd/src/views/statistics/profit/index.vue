<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Table, Tabs, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const activeTab = ref('monthly');

const columns = [
  { title: '月份', dataIndex: 'month', key: 'month' },
  { title: '收入', dataIndex: 'income', key: 'income' },
  { title: '成本', dataIndex: 'cost', key: 'cost' },
  { title: '利润', dataIndex: 'profit', key: 'profit' },
  { title: '利润率', dataIndex: 'profitRate', key: 'profitRate' },
  { title: '人均利润', dataIndex: 'perCapita', key: 'perCapita' },
];

const data = ref([
  { id: 1, month: '2024-01', income: '¥856,000', cost: '¥642,000', profit: '¥214,000', profitRate: '25.0%', perCapita: '¥167' },
  { id: 2, month: '2024-02', income: '¥720,000', cost: '¥540,000', profit: '¥180,000', profitRate: '25.0%', perCapita: '¥171' },
  { id: 3, month: '2024-03', income: '¥1,240,000', cost: '¥918,000', profit: '¥322,000', profitRate: '26.0%', perCapita: '¥173' },
  { id: 4, month: '2024-04', income: '¥1,580,000', cost: '¥1,154,000', profit: '¥426,000', profitRate: '27.0%', perCapita: '¥182' },
  { id: 5, month: '2024-05', income: '¥1,920,000', cost: '¥1,382,000', profit: '¥538,000', profitRate: '28.0%', perCapita: '¥189' },
  { id: 6, month: '2024-06', income: '¥2,280,000', cost: '¥1,618,000', profit: '¥662,000', profitRate: '29.0%', perCapita: '¥201' },
]);
</script>

<template>
  <Page title="利润统计" description="按多维度统计利润">
    <Card>
      <Tabs v-model:activeKey="activeTab">
        <Tabs.TabPane key="monthly" tab="按月" />
        <Tabs.TabPane key="customer" tab="按客户" />
        <Tabs.TabPane key="sales" tab="按销售员" />
        <Tabs.TabPane key="product" tab="按产品" />
        <Tabs.TabPane key="region" tab="按区域" />
        <Tabs.TabPane key="type" tab="按类型" />
        <Tabs.TabPane key="department" tab="按部门" />
        <Tabs.TabPane key="guide" tab="按导游" />
        <Tabs.TabPane key="coordinator" tab="按计调" />
        <Tabs.TabPane key="team" tab="按团队" />
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
          <template v-if="column.key === 'profitRate'">
            <Tag :color="parseFloat(record.profitRate) >= 27 ? 'green' : parseFloat(record.profitRate) >= 25 ? 'blue' : 'orange'">
              {{ record.profitRate }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
