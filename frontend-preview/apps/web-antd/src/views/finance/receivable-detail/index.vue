<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '应收金额', dataIndex: 'receivableAmount', key: 'receivableAmount' },
  { title: '已收金额', dataIndex: 'receivedAmount', key: 'receivedAmount' },
  { title: '欠款', dataIndex: 'debt', key: 'debt' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '销售员', dataIndex: 'salesman', key: 'salesman' },
  { title: '状态', dataIndex: 'status', key: 'status' },
];

const data = ref([
  { id: 1, teamNo: 'T20260501-001', customer: '广州阳光旅行社', receivableAmount: 48000, receivedAmount: 48000, debt: 0, departDate: '2026-05-01', salesman: '王丽', status: '已结清' },
  { id: 2, teamNo: 'T20260502-002', customer: '深圳畅游国旅', receivableAmount: 32500, receivedAmount: 20000, debt: 12500, departDate: '2026-05-02', salesman: '陈明', status: '部分收款' },
  { id: 3, teamNo: 'T20260503-003', customer: '东莞康辉旅行社', receivableAmount: 36000, receivedAmount: 36000, debt: 0, departDate: '2026-05-03', salesman: '李娜', status: '已结清' },
  { id: 4, teamNo: 'T20260504-004', customer: '佛山中青旅', receivableAmount: 42000, receivedAmount: 21000, debt: 21000, departDate: '2026-05-04', salesman: '王丽', status: '部分收款' },
  { id: 5, teamNo: 'T20260505-005', customer: '广州阳光旅行社', receivableAmount: 56000, receivedAmount: 30000, debt: 26000, departDate: '2026-05-05', salesman: '张伟', status: '部分收款' },
  { id: 6, teamNo: 'T20260506-006', customer: '珠海春秋旅行社', receivableAmount: 38500, receivedAmount: 38500, debt: 0, departDate: '2026-05-06', salesman: '陈明', status: '已结清' },
  { id: 7, teamNo: 'T20260507-007', customer: '湖南环球旅行社', receivableAmount: 44000, receivedAmount: 0, debt: 44000, departDate: '2026-05-07', salesman: '李娜', status: '未收款' },
  { id: 8, teamNo: 'T20260508-008', customer: '贵州黔途国旅', receivableAmount: 54000, receivedAmount: 30000, debt: 24000, departDate: '2026-05-08', salesman: '张伟', status: '部分收款' },
  { id: 9, teamNo: 'T20260509-009', customer: '南宁青旅国际', receivableAmount: 28000, receivedAmount: 28000, debt: 0, departDate: '2026-05-09', salesman: '王丽', status: '已结清' },
  { id: 10, teamNo: 'T20260510-010', customer: '佛山中青旅', receivableAmount: 62000, receivedAmount: 40000, debt: 22000, departDate: '2026-05-10', salesman: '陈明', status: '部分收款' },
]);
</script>

<template>
  <Page title="应收账款明细" description="查看应收账款明细">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">导出明细</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receivableAmount' || column.key === 'receivedAmount'">
            ¥{{ record[column.key]?.toLocaleString?.() ?? record[column.key] }}
          </template>
          <template v-if="column.key === 'debt'">
            <span :class="record.debt > 0 ? 'text-red-500' : 'text-green-600'">
              ¥{{ record.debt.toLocaleString() }}
            </span>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已结清' ? 'green' : record.status === '部分收款' ? 'blue' : 'red'">
              {{ record.status }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
