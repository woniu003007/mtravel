<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '供应商名称', dataIndex: 'supplier', key: 'supplier' },
  { title: '应付总额', dataIndex: 'totalAmount', key: 'totalAmount' },
  { title: '已付金额', dataIndex: 'paidAmount', key: 'paidAmount' },
  { title: '未付金额', dataIndex: 'unpaid', key: 'unpaid' },
  { title: '最近付款日期', dataIndex: 'lastDate', key: 'lastDate' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, supplier: '桂林漓江大酒店', totalAmount: 86000, paidAmount: 72000, unpaid: 14000, lastDate: '2026-05-09' },
  { id: 2, supplier: '阳朔西街客栈', totalAmount: 45000, paidAmount: 45000, unpaid: 0, lastDate: '2026-05-07' },
  { id: 3, supplier: '桂林旅游大巴公司', totalAmount: 62000, paidAmount: 48000, unpaid: 14000, lastDate: '2026-05-05' },
  { id: 4, supplier: '漓江景区管理处', totalAmount: 98000, paidAmount: 75000, unpaid: 23000, lastDate: '2026-05-03' },
  { id: 5, supplier: '龙脊梯田景区', totalAmount: 54000, paidAmount: 54000, unpaid: 0, lastDate: '2026-05-08' },
  { id: 6, supplier: '桂林米粉餐饮公司', totalAmount: 38000, paidAmount: 25000, unpaid: 13000, lastDate: '2026-04-30' },
  { id: 7, supplier: '广西旅游保险公司', totalAmount: 22000, paidAmount: 22000, unpaid: 0, lastDate: '2026-05-10' },
  { id: 8, supplier: '印象刘三姐演出', totalAmount: 76000, paidAmount: 60000, unpaid: 16000, lastDate: '2026-05-06' },
]);
</script>

<template>
  <Page title="应付账款" description="管理供应商应付账款">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">付款登记</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalAmount' || column.key === 'paidAmount'">
            ¥{{ record[column.key]?.toLocaleString?.() ?? record[column.key] }}
          </template>
          <template v-if="column.key === 'unpaid'">
            <span :class="record.unpaid > 0 ? 'text-red-500' : 'text-green-600'">
              ¥{{ record.unpaid.toLocaleString() }}
            </span>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">付款</Button>
            <Button type="link" size="small">明细</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
