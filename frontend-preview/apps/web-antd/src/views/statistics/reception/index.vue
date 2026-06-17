<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Table, Tabs, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const activeTab = ref('monthly');

const columns = [
  { title: '月份', dataIndex: 'month', key: 'month' },
  { title: '团数', dataIndex: 'teams', key: 'teams' },
  { title: '人数', dataIndex: 'people', key: 'people' },
  { title: '人天数', dataIndex: 'personDays', key: 'personDays' },
  { title: '收入', dataIndex: 'income', key: 'income' },
  { title: '同比增长', dataIndex: 'yoy', key: 'yoy' },
];

const data = ref([
  { id: 1, month: '2024-01', teams: 45, people: 1280, personDays: 3840, income: '¥856,000', yoy: '+12.5%' },
  { id: 2, month: '2024-02', teams: 38, people: 1050, personDays: 3150, income: '¥720,000', yoy: '+8.3%' },
  { id: 3, month: '2024-03', teams: 62, people: 1860, personDays: 5580, income: '¥1,240,000', yoy: '+15.2%' },
  { id: 4, month: '2024-04', teams: 78, people: 2340, personDays: 7020, income: '¥1,580,000', yoy: '+18.6%' },
  { id: 5, month: '2024-05', teams: 95, people: 2850, personDays: 8550, income: '¥1,920,000', yoy: '+22.1%' },
  { id: 6, month: '2024-06', teams: 110, people: 3300, personDays: 9900, income: '¥2,280,000', yoy: '+25.8%' },
]);
</script>

<template>
  <Page title="收客统计" description="按多维度统计收客情况">
    <Card>
      <Tabs v-model:activeKey="activeTab">
        <Tabs.TabPane key="monthly" tab="按月统计" />
        <Tabs.TabPane key="customer" tab="按客户" />
        <Tabs.TabPane key="sales" tab="按销售员" />
        <Tabs.TabPane key="product" tab="按产品" />
        <Tabs.TabPane key="region" tab="按区域" />
        <Tabs.TabPane key="type" tab="按类型" />
        <Tabs.TabPane key="department" tab="按部门" />
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
