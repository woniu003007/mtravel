<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier' },
  { title: '费用类型', dataIndex: 'costType', key: 'costType' },
  { title: '应付金额', dataIndex: 'payableAmount', key: 'payableAmount' },
  { title: '已付金额', dataIndex: 'paidAmount', key: 'paidAmount' },
  { title: '欠款', dataIndex: 'debt', key: 'debt' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '状态', dataIndex: 'status', key: 'status' },
];

const data = ref([
  { id: 1, teamNo: 'T20260501-001', supplier: '桂林漓江大酒店', costType: '住宿', payableAmount: 12800, paidAmount: 12800, debt: 0, departDate: '2026-05-01', status: '已结清' },
  { id: 2, teamNo: 'T20260501-001', supplier: '桂林旅游大巴公司', costType: '交通', payableAmount: 8500, paidAmount: 8500, debt: 0, departDate: '2026-05-01', status: '已结清' },
  { id: 3, teamNo: 'T20260502-002', supplier: '阳朔西街客栈', costType: '住宿', payableAmount: 9600, paidAmount: 5000, debt: 4600, departDate: '2026-05-02', status: '部分付款' },
  { id: 4, teamNo: 'T20260502-002', supplier: '漓江景区管理处', costType: '门票', payableAmount: 15000, paidAmount: 15000, debt: 0, departDate: '2026-05-02', status: '已结清' },
  { id: 5, teamNo: 'T20260503-003', supplier: '龙脊梯田景区', costType: '门票', payableAmount: 16000, paidAmount: 10000, debt: 6000, departDate: '2026-05-03', status: '部分付款' },
  { id: 6, teamNo: 'T20260504-004', supplier: '桂林米粉餐饮公司', costType: '餐饮', payableAmount: 9600, paidAmount: 0, debt: 9600, departDate: '2026-05-04', status: '未付款' },
  { id: 7, teamNo: 'T20260505-005', supplier: '印象刘三姐演出', costType: '演出', payableAmount: 14000, paidAmount: 14000, debt: 0, departDate: '2026-05-05', status: '已结清' },
  { id: 8, teamNo: 'T20260506-006', supplier: '桂林旅游大巴公司', costType: '交通', payableAmount: 7200, paidAmount: 4000, debt: 3200, departDate: '2026-05-06', status: '部分付款' },
  { id: 9, teamNo: 'T20260507-007', supplier: '广西旅游保险公司', costType: '保险', payableAmount: 2800, paidAmount: 2800, debt: 0, departDate: '2026-05-07', status: '已结清' },
  { id: 10, teamNo: 'T20260508-008', supplier: '桂林漓江大酒店', costType: '住宿', payableAmount: 18000, paidAmount: 10000, debt: 8000, departDate: '2026-05-08', status: '部分付款' },
]);
</script>

<template>
  <Page title="应付账款明细" description="查看应付账款明细">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">导出明细</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'payableAmount' || column.key === 'paidAmount'">
            ¥{{ record[column.key]?.toLocaleString?.() ?? record[column.key] }}
          </template>
          <template v-if="column.key === 'debt'">
            <span :class="record.debt > 0 ? 'text-red-500' : 'text-green-600'">
              ¥{{ record.debt.toLocaleString() }}
            </span>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已结清' ? 'green' : record.status === '部分付款' ? 'blue' : 'red'">
              {{ record.status }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
