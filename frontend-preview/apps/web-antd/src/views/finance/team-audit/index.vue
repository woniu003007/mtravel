<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '产品', dataIndex: 'product', key: 'product' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '人数', dataIndex: 'people', key: 'people' },
  { title: '应收总额', dataIndex: 'receivable', key: 'receivable' },
  { title: '应付总额', dataIndex: 'payable', key: 'payable' },
  { title: '利润', dataIndex: 'profit', key: 'profit' },
  { title: '审核状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, teamNo: 'T20260501-001', product: '桂林三日游', departDate: '2026-05-01', people: 32, receivable: 48000, payable: 35200, profit: 12800, status: '已审核' },
  { id: 2, teamNo: 'T20260502-002', product: '阳朔两日游', departDate: '2026-05-02', people: 25, receivable: 32500, payable: 24800, profit: 7700, status: '待审核' },
  { id: 3, teamNo: 'T20260503-003', product: '龙脊梯田一日游', departDate: '2026-05-03', people: 40, receivable: 36000, payable: 28000, profit: 8000, status: '已审核' },
  { id: 4, teamNo: 'T20260504-004', product: '漓江精华游', departDate: '2026-05-04', people: 28, receivable: 42000, payable: 31500, profit: 10500, status: '审核中' },
  { id: 5, teamNo: 'T20260505-005', product: '桂林+阳朔四日游', departDate: '2026-05-05', people: 20, receivable: 56000, payable: 42000, profit: 14000, status: '已审核' },
  { id: 6, teamNo: 'T20260506-006', product: '银子岩+遇龙河', departDate: '2026-05-06', people: 35, receivable: 38500, payable: 29400, profit: 9100, status: '待审核' },
  { id: 7, teamNo: 'T20260507-007', product: '德天瀑布两日游', departDate: '2026-05-07', people: 22, receivable: 44000, payable: 33000, profit: 11000, status: '已驳回' },
  { id: 8, teamNo: 'T20260508-008', product: '北海涠洲岛三日游', departDate: '2026-05-08', people: 18, receivable: 54000, payable: 40500, profit: 13500, status: '审核中' },
]);
</script>

<template>
  <Page title="团队审核" description="财务审核团队费用">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">批量审核</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'receivable' || column.key === 'payable' || column.key === 'profit'">
            ¥{{ record[column.key]?.toLocaleString?.() ?? record[column.key] }}
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已审核' ? 'green' : record.status === '待审核' ? 'orange' : record.status === '审核中' ? 'blue' : 'red'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">审核</Button>
            <Button type="link" size="small">详情</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
