<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '开票金额', dataIndex: 'amount', key: 'amount' },
  { title: '发票类型', dataIndex: 'type', key: 'type' },
  { title: '开票日期', dataIndex: 'date', key: 'date' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, invoiceNo: 'FP2024030001', customer: '北京阳光旅行社', amount: 120000, type: '专票', date: '2024-03-02', status: '已开具' },
  { id: 2, invoiceNo: 'FP2024030002', customer: '上海环球国旅', amount: 62000, type: '普票', date: '2024-03-05', status: '已开具' },
  { id: 3, invoiceNo: 'FP2024030003', customer: '广州南方旅游', amount: 45000, type: '专票', date: '2024-03-08', status: '待开具' },
  { id: 4, invoiceNo: 'FP2024030004', customer: '深圳畅游旅行社', amount: 120000, type: '专票', date: '2024-03-10', status: '已开具' },
  { id: 5, invoiceNo: 'FP2024030005', customer: '成都天府旅行社', amount: 78000, type: '普票', date: '2024-03-13', status: '已作废' },
  { id: 6, invoiceNo: 'FP2024030006', customer: '杭州西湖国旅', amount: 55000, type: '专票', date: '2024-03-16', status: '待开具' },
]);
</script>

<template>
  <Page title="发票记录" description="管理开票记录">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">开具发票</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <Tag :color="record.type === '专票' ? 'blue' : 'default'">
              {{ record.type }}
            </Tag>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已开具' ? 'green' : record.status === '已作废' ? 'red' : 'orange'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">查看</Button>
            <Button type="link" size="small">打印</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
