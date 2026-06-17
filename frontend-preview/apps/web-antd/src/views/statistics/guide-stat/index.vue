<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Table, Tabs, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const activeTab = ref('teams');

const columns = [
  { title: '导游姓名', dataIndex: 'name', key: 'name' },
  { title: '带团数', dataIndex: 'teamCount', key: 'teamCount' },
  { title: '带团天数', dataIndex: 'days', key: 'days' },
  { title: '服务人次', dataIndex: 'servedPeople', key: 'servedPeople' },
  { title: '好评率', dataIndex: 'rating', key: 'rating' },
  { title: '投诉数', dataIndex: 'complaints', key: 'complaints' },
];

const data = ref([
  { id: 1, name: '张明', teamCount: 28, days: 84, servedPeople: 840, rating: '98%', complaints: 0 },
  { id: 2, name: '李芳', teamCount: 25, days: 75, servedPeople: 750, rating: '96%', complaints: 1 },
  { id: 3, name: '王强', teamCount: 22, days: 66, servedPeople: 660, rating: '94%', complaints: 1 },
  { id: 4, name: '刘婷', teamCount: 30, days: 90, servedPeople: 900, rating: '99%', complaints: 0 },
  { id: 5, name: '陈伟', teamCount: 18, days: 54, servedPeople: 540, rating: '88%', complaints: 3 },
  { id: 6, name: '赵丽', teamCount: 24, days: 72, servedPeople: 720, rating: '95%', complaints: 1 },
]);
</script>

<template>
  <Page title="导游统计" description="统计导游带团情况">
    <Card>
      <Tabs v-model:activeKey="activeTab">
        <Tabs.TabPane key="teams" tab="带团统计" />
        <Tabs.TabPane key="score" tab="评分统计" />
        <Tabs.TabPane key="income" tab="收入统计" />
        <Tabs.TabPane key="attendance" tab="出勤统计" />
        <Tabs.TabPane key="complaint" tab="投诉统计" />
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
          <template v-if="column.key === 'rating'">
            <Tag :color="parseInt(record.rating) >= 95 ? 'green' : parseInt(record.rating) >= 90 ? 'blue' : 'orange'">
              {{ record.rating }}
            </Tag>
          </template>
          <template v-if="column.key === 'complaints'">
            <Tag :color="record.complaints === 0 ? 'green' : record.complaints <= 1 ? 'orange' : 'red'">
              {{ record.complaints }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
