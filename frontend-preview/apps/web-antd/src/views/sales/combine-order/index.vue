<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tabs, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const activeTab = ref('all');

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '产品', dataIndex: 'productName', key: 'productName' },
  { title: '人数', dataIndex: 'count', key: 'count' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '拼团状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, orderNo: 'PT20250510001', customer: '杭州阳光旅行社', productName: '杭州西湖一日游', count: 8, amount: 2384, status: '已拼团' },
  { id: 2, orderNo: 'PT20250510002', customer: '上海春秋旅行社', productName: '杭州西湖一日游', count: 12, amount: 3576, status: '已拼团' },
  { id: 3, orderNo: 'PT20250512001', customer: '南京中青旅', productName: '千岛湖二日游', count: 6, amount: 4128, status: '未拼团' },
  { id: 4, orderNo: 'PT20250515001', customer: '苏州国旅', productName: '乌镇西塘三日游', count: 10, amount: 9800, status: '未拼团' },
  { id: 5, orderNo: 'PT20250516001', customer: '宁波康辉旅行社', productName: '千岛湖二日游', count: 15, amount: 10320, status: '已拼团' },
  { id: 6, orderNo: 'PT20250518001', customer: '温州中旅', productName: '横店影视城二日游', count: 20, amount: 11160, status: '未拼团' },
]);
</script>

<template>
  <Page title="拼团订单" description="管理拼团订单">
    <Card>
      <Tabs v-model:activeKey="activeTab">
        <TabPane key="all" tab="全部" />
        <TabPane key="ungrouped" tab="未拼团" />
        <TabPane key="grouped" tab="已拼团" />
        <TabPane key="sub" tab="子订单" />
        <TabPane key="byTeam" tab="按团展示" />
      </Tabs>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount.toLocaleString() }}
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已拼团' ? 'green' : 'orange'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">拼团</Button>
            <Button type="link" size="small">查看</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
