<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card } from 'ant-design-vue';
const reports = [
  { id: 1, name: '团队明细表', description: '导出所有团队的详细信息，包含行程、人数、费用等' },
  { id: 2, name: '收客统计表', description: '按时间段统计收客数据，支持多维度筛选导出' },
  { id: 3, name: '利润统计表', description: '统计各团队、产品线的利润情况，含成本明细' },
  { id: 4, name: '应收账款表', description: '汇总所有客户的应收账款，含账龄分析' },
  { id: 5, name: '应付账款表', description: '汇总所有供应商的应付账款，含付款计划' },
  { id: 6, name: '导游结算表', description: '导游带团费用结算明细，含补贴和奖金' },
  { id: 7, name: '收款明细表', description: '所有收款记录明细，按客户和时间分类' },
  { id: 8, name: '付款明细表', description: '所有付款记录明细，按供应商和类别分类' },
  { id: 9, name: '资源采购表', description: '资源采购汇总数据，含供应商对比分析' },
];
</script>

<template>
  <Page title="报表中心" description="导出各类业务报表">
    <div class="grid grid-cols-3 gap-4">
      <Card v-for="report in reports" :key="report.id" :title="report.name" size="small">
        <p class="mb-4 text-gray-500">{{ report.description }}</p>
        <Button type="primary" size="small">
          <template #icon><span class="i-ant-design:export-outlined" /></template>
          导出报表
        </Button>
      </Card>
    </div>
  </Page>
</template>
