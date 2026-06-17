<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '导游姓名', dataIndex: 'guideName', key: 'guideName' },
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '发放金额', dataIndex: 'issuedAmount', key: 'issuedAmount' },
  { title: '已用金额', dataIndex: 'usedAmount', key: 'usedAmount' },
  { title: '归还金额', dataIndex: 'returnedAmount', key: 'returnedAmount' },
  { title: '发放日期', dataIndex: 'issueDate', key: 'issueDate' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, guideName: '黄志强', teamNo: 'T20260501-001', issuedAmount: 5000, usedAmount: 4200, returnedAmount: 800, issueDate: '2026-04-30', status: '已归还' },
  { id: 2, guideName: '韦小兰', teamNo: 'T20260502-002', issuedAmount: 3500, usedAmount: 2800, returnedAmount: 700, issueDate: '2026-05-01', status: '已归还' },
  { id: 3, guideName: '覃大伟', teamNo: 'T20260503-003', issuedAmount: 6000, usedAmount: 5100, returnedAmount: 0, issueDate: '2026-05-02', status: '待归还' },
  { id: 4, guideName: '黄志强', teamNo: 'T20260504-004', issuedAmount: 4500, usedAmount: 3000, returnedAmount: 0, issueDate: '2026-05-03', status: '使用中' },
  { id: 5, guideName: '莫丽华', teamNo: 'T20260505-005', issuedAmount: 5500, usedAmount: 0, returnedAmount: 0, issueDate: '2026-05-04', status: '已发放' },
  { id: 6, guideName: '韦小兰', teamNo: 'T20260506-006', issuedAmount: 4000, usedAmount: 3800, returnedAmount: 200, issueDate: '2026-05-05', status: '已归还' },
]);
</script>

<template>
  <Page title="导游备用金" description="管理导游备用金发放和归还">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">发放备用金</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'issuedAmount' || column.key === 'usedAmount' || column.key === 'returnedAmount'">
            ¥{{ record[column.key]?.toLocaleString?.() ?? record[column.key] }}
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已归还' ? 'green' : record.status === '使用中' ? 'blue' : record.status === '已发放' ? 'cyan' : 'orange'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">归还</Button>
            <Button type="link" size="small">详情</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
