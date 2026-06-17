<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier' },
  { title: '费用项目', dataIndex: 'costItem', key: 'costItem' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '成本阶段', dataIndex: 'costStage', key: 'costStage' },
  { title: '团队阶段', dataIndex: 'teamStage', key: 'teamStage' },
  { title: '计调员', dataIndex: 'operator', key: 'operator' },
  { title: '日期', dataIndex: 'date', key: 'date' },
];

const data = ref([
  { id: 1, teamNo: 'T20260501-001', supplier: '桂林漓江大酒店', costItem: '标准双人房×16间×2晚', amount: 12800, costStage: '已确认', teamStage: '已出团', operator: '刘计调', date: '2026-04-28' },
  { id: 2, teamNo: 'T20260501-001', supplier: '桂林旅游大巴公司', costItem: '45座大巴×3天', amount: 8500, costStage: '已确认', teamStage: '已出团', operator: '刘计调', date: '2026-04-28' },
  { id: 3, teamNo: 'T20260501-001', supplier: '漓江景区管理处', costItem: '漓江游船票×32人', amount: 9600, costStage: '已确认', teamStage: '已出团', operator: '刘计调', date: '2026-04-29' },
  { id: 4, teamNo: 'T20260502-002', supplier: '阳朔西街客栈', costItem: '豪华标间×13间×1晚', amount: 9600, costStage: '预估', teamStage: '进行中', operator: '赵计调', date: '2026-04-30' },
  { id: 5, teamNo: 'T20260502-002', supplier: '遇龙河景区', costItem: '竹筏漂流×25人', amount: 5000, costStage: '已确认', teamStage: '进行中', operator: '赵计调', date: '2026-04-30' },
  { id: 6, teamNo: 'T20260503-003', supplier: '龙脊梯田景区', costItem: '景区门票×40人', amount: 16000, costStage: '已确认', teamStage: '已出团', operator: '刘计调', date: '2026-05-01' },
  { id: 7, teamNo: 'T20260503-003', supplier: '桂林米粉餐饮公司', costItem: '团队午餐×40人×1餐', amount: 4800, costStage: '预估', teamStage: '已出团', operator: '刘计调', date: '2026-05-01' },
  { id: 8, teamNo: 'T20260504-004', supplier: '印象刘三姐演出', costItem: '甲等座×28人', amount: 14000, costStage: '已确认', teamStage: '计划中', operator: '赵计调', date: '2026-05-02' },
  { id: 9, teamNo: 'T20260505-005', supplier: '广西旅游保险公司', costItem: '旅游意外险×20人', amount: 2800, costStage: '已确认', teamStage: '计划中', operator: '赵计调', date: '2026-05-03' },
  { id: 10, teamNo: 'T20260506-006', supplier: '银子岩景区', costItem: '景区门票×35人', amount: 7000, costStage: '预估', teamStage: '计划中', operator: '刘计调', date: '2026-05-04' },
]);
</script>

<template>
  <Page title="成本明细预览" description="预览团队成本明细">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">导出成本表</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount.toLocaleString() }}
          </template>
          <template v-if="column.key === 'costStage'">
            <Tag :color="record.costStage === '已确认' ? 'green' : 'orange'">
              {{ record.costStage }}
            </Tag>
          </template>
          <template v-if="column.key === 'teamStage'">
            <Tag :color="record.teamStage === '已出团' ? 'blue' : record.teamStage === '进行中' ? 'cyan' : 'default'">
              {{ record.teamStage }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
