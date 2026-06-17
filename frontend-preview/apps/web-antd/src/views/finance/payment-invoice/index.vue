<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo' },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '发票类型', dataIndex: 'type', key: 'type' },
  { title: '收票日期', dataIndex: 'receiveDate', key: 'receiveDate' },
  { title: '关联团号', dataIndex: 'groupNo', key: 'groupNo' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, invoiceNo: 'SP2024030001', supplier: '云南大巴车队', amount: 12000, type: '普票', receiveDate: '2024-03-05', groupNo: 'GL2024-0301', status: '已认证' },
  { id: 2, invoiceNo: 'SP2024030002', supplier: '丽江花园酒店', amount: 28000, type: '专票', receiveDate: '2024-03-07', groupNo: 'GL2024-0301', status: '已认证' },
  { id: 3, invoiceNo: 'SP2024030003', supplier: '大理古城餐厅', amount: 8500, type: '普票', receiveDate: '2024-03-09', groupNo: 'GL2024-0302', status: '待认证' },
  { id: 4, invoiceNo: 'SP2024030004', supplier: '玉龙雪山景区', amount: 15600, type: '专票', receiveDate: '2024-03-12', groupNo: 'GL2024-0303', status: '已认证' },
  { id: 5, invoiceNo: 'SP2024030005', supplier: '昆明航空票务', amount: 45000, type: '专票', receiveDate: '2024-03-14', groupNo: 'GL2024-0304', status: '待认证' },
  { id: 6, invoiceNo: 'SP2024030006', supplier: '西双版纳度假村', amount: 35000, type: '专票', receiveDate: '2024-03-17', groupNo: 'GL2024-0305', status: '已认证' },
]);
</script>

<template>
  <Page title="付款发票明细" description="管理收到的供应商发票">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">登记发票</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <Tag :color="record.type === '专票' ? 'blue' : 'default'">
              {{ record.type }}
            </Tag>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已认证' ? 'green' : 'orange'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">查看</Button>
            <Button type="link" size="small">认证</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
