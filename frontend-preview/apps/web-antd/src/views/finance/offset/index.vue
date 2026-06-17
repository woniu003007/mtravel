<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '关联公司', dataIndex: 'company', key: 'company' },
  { title: '应收金额', dataIndex: 'receivable', key: 'receivable' },
  { title: '应付金额', dataIndex: 'payable', key: 'payable' },
  { title: '冲抵金额', dataIndex: 'offsetAmount', key: 'offsetAmount' },
  { title: '余额', dataIndex: 'balance', key: 'balance' },
  { title: '最近操作日期', dataIndex: 'lastDate', key: 'lastDate' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, company: '北京阳光旅行社', receivable: 250000, payable: 80000, offsetAmount: 80000, balance: 170000, lastDate: '2024-03-15' },
  { id: 2, company: '上海环球国旅', receivable: 180000, payable: 120000, offsetAmount: 120000, balance: 60000, lastDate: '2024-03-12' },
  { id: 3, company: '广州南方旅游', receivable: 95000, payable: 95000, offsetAmount: 95000, balance: 0, lastDate: '2024-03-10' },
  { id: 4, company: '深圳畅游旅行社', receivable: 320000, payable: 150000, offsetAmount: 100000, balance: 170000, lastDate: '2024-03-08' },
  { id: 5, company: '成都天府旅行社', receivable: 60000, payable: 180000, offsetAmount: 60000, balance: -120000, lastDate: '2024-03-05' },
]);
</script>

<template>
  <Page title="应收应付冲抵" description="管理公司关联及应收应付冲抵">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">新建冲抵</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'balance'">
            <span :style="{ color: record.balance < 0 ? '#ff4d4f' : '#52c41a' }">
              {{ record.balance }}
            </span>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">冲抵</Button>
            <Button type="link" size="small">明细</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
