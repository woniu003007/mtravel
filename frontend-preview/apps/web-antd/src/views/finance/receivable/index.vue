<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '客户名称', dataIndex: 'customer', key: 'customer' },
  { title: '应收总额', dataIndex: 'totalAmount', key: 'totalAmount' },
  { title: '已收金额', dataIndex: 'receivedAmount', key: 'receivedAmount' },
  { title: '未收金额', dataIndex: 'unreceived', key: 'unreceived' },
  { title: '最近收款日期', dataIndex: 'lastDate', key: 'lastDate' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, customer: '广州阳光旅行社', totalAmount: 128000, receivedAmount: 98000, unreceived: 30000, lastDate: '2026-05-08' },
  { id: 2, customer: '深圳畅游国旅', totalAmount: 95000, receivedAmount: 95000, unreceived: 0, lastDate: '2026-05-06' },
  { id: 3, customer: '东莞康辉旅行社', totalAmount: 76000, receivedAmount: 50000, unreceived: 26000, lastDate: '2026-05-03' },
  { id: 4, customer: '佛山中青旅', totalAmount: 112000, receivedAmount: 80000, unreceived: 32000, lastDate: '2026-04-28' },
  { id: 5, customer: '珠海春秋旅行社', totalAmount: 64000, receivedAmount: 64000, unreceived: 0, lastDate: '2026-05-10' },
  { id: 6, customer: '湖南环球旅行社', totalAmount: 88000, receivedAmount: 55000, unreceived: 33000, lastDate: '2026-04-25' },
  { id: 7, customer: '贵州黔途国旅', totalAmount: 52000, receivedAmount: 30000, unreceived: 22000, lastDate: '2026-05-01' },
  { id: 8, customer: '南宁青旅国际', totalAmount: 145000, receivedAmount: 120000, unreceived: 25000, lastDate: '2026-05-09' },
]);
</script>

<template>
  <Page title="应收账款" description="管理客户应收账款">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">收款登记</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalAmount' || column.key === 'receivedAmount'">
            ¥{{ record[column.key]?.toLocaleString?.() ?? record[column.key] }}
          </template>
          <template v-if="column.key === 'unreceived'">
            <span :class="record.unreceived > 0 ? 'text-red-500' : 'text-green-600'">
              ¥{{ record.unreceived.toLocaleString() }}
            </span>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">收款</Button>
            <Button type="link" size="small">明细</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
