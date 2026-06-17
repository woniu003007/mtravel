<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Card, Progress, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '月份', dataIndex: 'month', key: 'month' },
  { title: '应收总额', dataIndex: 'receivableTotal', key: 'receivableTotal' },
  { title: '已收金额', dataIndex: 'receivedAmount', key: 'receivedAmount' },
  { title: '应付总额', dataIndex: 'payableTotal', key: 'payableTotal' },
  { title: '已付金额', dataIndex: 'paidAmount', key: 'paidAmount' },
  { title: '收款进度', dataIndex: 'receiveProgress', key: 'receiveProgress' },
  { title: '付款进度', dataIndex: 'payProgress', key: 'payProgress' },
];

const data = ref([
  { id: 1, month: '2024-01', receivableTotal: 580000, receivedAmount: 580000, payableTotal: 420000, paidAmount: 420000, receiveProgress: 100, payProgress: 100 },
  { id: 2, month: '2024-02', receivableTotal: 620000, receivedAmount: 590000, payableTotal: 450000, paidAmount: 450000, receiveProgress: 95, payProgress: 100 },
  { id: 3, month: '2024-03', receivableTotal: 750000, receivedAmount: 600000, payableTotal: 530000, paidAmount: 480000, receiveProgress: 80, payProgress: 91 },
  { id: 4, month: '2024-04', receivableTotal: 680000, receivedAmount: 450000, payableTotal: 490000, paidAmount: 350000, receiveProgress: 66, payProgress: 71 },
  { id: 5, month: '2024-05', receivableTotal: 820000, receivedAmount: 320000, payableTotal: 580000, paidAmount: 200000, receiveProgress: 39, payProgress: 34 },
  { id: 6, month: '2024-06', receivableTotal: 900000, receivedAmount: 150000, payableTotal: 640000, paidAmount: 80000, receiveProgress: 17, payProgress: 13 },
]);
</script>

<template>
  <Page title="财务收支进度" description="查看财务收支进度">
    <Card>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receiveProgress'">
            <Progress :percent="record.receiveProgress" :stroke-color="record.receiveProgress >= 80 ? '#52c41a' : record.receiveProgress >= 50 ? '#faad14' : '#ff4d4f'" size="small" />
          </template>
          <template v-if="column.key === 'payProgress'">
            <Progress :percent="record.payProgress" :stroke-color="record.payProgress >= 80 ? '#52c41a' : record.payProgress >= 50 ? '#faad14' : '#ff4d4f'" size="small" />
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
