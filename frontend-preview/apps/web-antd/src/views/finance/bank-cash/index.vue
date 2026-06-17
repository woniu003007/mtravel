<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tabs } from 'ant-design-vue';
import { ref } from 'vue';

const activeTab = ref('cash');
const tabs = [
  { key: 'cash', label: '现金' },
  { key: 'abc', label: '农行' },
  { key: 'ccb', label: '建行' },
  { key: 'alipay', label: '支付宝' },
  { key: 'wechat', label: '微信' },
  { key: 'card', label: '公务卡' },
];

const columns = [
  { title: '日期', dataIndex: 'date', key: 'date' },
  { title: '摘要', dataIndex: 'summary', key: 'summary' },
  { title: '收入', dataIndex: 'income', key: 'income' },
  { title: '支出', dataIndex: 'expense', key: 'expense' },
  { title: '余额', dataIndex: 'balance', key: 'balance' },
  { title: '关联团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
];

const data = ref([
  { id: 1, date: '2026-05-10', summary: '收桂林三日游团款', income: 48000, expense: 0, balance: 156000, teamNo: 'T20260501-001', operator: '张会计' },
  { id: 2, date: '2026-05-09', summary: '付酒店住宿费', income: 0, expense: 12600, balance: 108000, teamNo: 'T20260502-002', operator: '李出纳' },
  { id: 3, date: '2026-05-08', summary: '收阳朔两日游团款', income: 32500, expense: 0, balance: 120600, teamNo: 'T20260502-002', operator: '张会计' },
  { id: 4, date: '2026-05-07', summary: '付大巴车费', income: 0, expense: 8500, balance: 88100, teamNo: 'T20260503-003', operator: '李出纳' },
  { id: 5, date: '2026-05-06', summary: '付景区门票款', income: 0, expense: 16000, balance: 96600, teamNo: 'T20260503-003', operator: '李出纳' },
  { id: 6, date: '2026-05-05', summary: '收龙脊梯田团款', income: 36000, expense: 0, balance: 112600, teamNo: 'T20260503-003', operator: '张会计' },
  { id: 7, date: '2026-05-04', summary: '付导游服务费', income: 0, expense: 3200, balance: 76600, teamNo: 'T20260504-004', operator: '李出纳' },
  { id: 8, date: '2026-05-03', summary: '付餐饮费', income: 0, expense: 9600, balance: 79800, teamNo: 'T20260504-004', operator: '李出纳' },
  { id: 9, date: '2026-05-02', summary: '收漓江精华游定金', income: 21000, expense: 0, balance: 89400, teamNo: 'T20260504-004', operator: '张会计' },
  { id: 10, date: '2026-05-01', summary: '付保险费', income: 0, expense: 2800, balance: 68400, teamNo: 'T20260505-005', operator: '李出纳' },
]);
</script>

<template>
  <Page title="银行现金账" description="管理银行和现金账户流水">
    <Card>
      <Tabs v-model:activeKey="activeTab">
        <TabPane v-for="tab in tabs" :key="tab.key" :tab="tab.label" />
      </Tabs>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">新增流水</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'income'">
            <span v-if="record.income" class="text-green-600">+¥{{ record.income.toLocaleString() }}</span>
            <span v-else>-</span>
          </template>
          <template v-if="column.key === 'expense'">
            <span v-if="record.expense" class="text-red-500">-¥{{ record.expense.toLocaleString() }}</span>
            <span v-else>-</span>
          </template>
          <template v-if="column.key === 'balance'">
            ¥{{ record.balance.toLocaleString() }}
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
