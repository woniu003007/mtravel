<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '产品', dataIndex: 'product', key: 'product' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '预付金额', dataIndex: 'prepayAmount', key: 'prepayAmount' },
  { title: '已付金额', dataIndex: 'paidAmount', key: 'paidAmount' },
  { title: '付款方式', dataIndex: 'payMethod', key: 'payMethod' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, teamNo: 'T20260501-001', product: '桂林三日游', departDate: '2026-05-01', prepayAmount: 20000, paidAmount: 20000, payMethod: '银行转账', status: '已付清' },
  { id: 2, teamNo: 'T20260502-002', product: '阳朔两日游', departDate: '2026-05-02', prepayAmount: 15000, paidAmount: 10000, payMethod: '支付宝', status: '部分付款' },
  { id: 3, teamNo: 'T20260503-003', product: '龙脊梯田一日游', departDate: '2026-05-03', prepayAmount: 12000, paidAmount: 12000, payMethod: '微信', status: '已付清' },
  { id: 4, teamNo: 'T20260504-004', product: '漓江精华游', departDate: '2026-05-04', prepayAmount: 18000, paidAmount: 0, payMethod: '银行转账', status: '待付款' },
  { id: 5, teamNo: 'T20260505-005', product: '桂林+阳朔四日游', departDate: '2026-05-05', prepayAmount: 25000, paidAmount: 15000, payMethod: '公务卡', status: '部分付款' },
  { id: 6, teamNo: 'T20260506-006', product: '银子岩+遇龙河', departDate: '2026-05-06', prepayAmount: 16000, paidAmount: 16000, payMethod: '现金', status: '已付清' },
]);
</script>

<template>
  <Page title="团队预付款" description="管理团队预付款">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加预付款</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'prepayAmount' || column.key === 'paidAmount'">
            ¥{{ record[column.key]?.toLocaleString?.() ?? record[column.key] }}
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已付清' ? 'green' : record.status === '部分付款' ? 'blue' : 'orange'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">付款</Button>
            <Button type="link" size="small">详情</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
