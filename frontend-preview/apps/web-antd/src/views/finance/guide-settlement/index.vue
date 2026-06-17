<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '导游姓名', dataIndex: 'guideName', key: 'guideName' },
  { title: '团号', dataIndex: 'groupNo', key: 'groupNo' },
  { title: '带团天数', dataIndex: 'days', key: 'days' },
  { title: '导服费', dataIndex: 'serviceFee', key: 'serviceFee' },
  { title: '补贴', dataIndex: 'subsidy', key: 'subsidy' },
  { title: '奖金', dataIndex: 'bonus', key: 'bonus' },
  { title: '扣款', dataIndex: 'deduction', key: 'deduction' },
  { title: '应付合计', dataIndex: 'totalPayable', key: 'totalPayable' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, guideName: '李明', groupNo: 'GL2024-0301', days: 5, serviceFee: 2500, subsidy: 500, bonus: 300, deduction: 0, totalPayable: 3300, status: '已结算' },
  { id: 2, guideName: '王芳', groupNo: 'GL2024-0302', days: 3, serviceFee: 1500, subsidy: 300, bonus: 200, deduction: 100, totalPayable: 1900, status: '待结算' },
  { id: 3, guideName: '张伟', groupNo: 'GL2024-0303', days: 7, serviceFee: 3500, subsidy: 700, bonus: 500, deduction: 0, totalPayable: 4700, status: '已结算' },
  { id: 4, guideName: '刘洋', groupNo: 'GL2024-0304', days: 4, serviceFee: 2000, subsidy: 400, bonus: 0, deduction: 200, totalPayable: 2200, status: '待审核' },
  { id: 5, guideName: '陈静', groupNo: 'GL2024-0305', days: 6, serviceFee: 3000, subsidy: 600, bonus: 400, deduction: 0, totalPayable: 4000, status: '已结算' },
  { id: 6, guideName: '赵强', groupNo: 'GL2024-0306', days: 2, serviceFee: 1000, subsidy: 200, bonus: 100, deduction: 50, totalPayable: 1250, status: '待结算' },
]);
</script>

<template>
  <Page title="导游结算" description="管理导游费用结算">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加结算</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已结算' ? 'green' : record.status === '待审核' ? 'orange' : 'blue'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">详情</Button>
            <Button type="link" size="small">结算</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
