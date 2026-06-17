<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '人数', dataIndex: 'count', key: 'count' },
  { title: '销售员', dataIndex: 'salesman', key: 'salesman' },
  { title: '利润', dataIndex: 'profit', key: 'profit' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, teamNo: 'HZ20250401-001', productName: '杭州西湖一日游', departDate: '2025-04-01', count: 38, salesman: '张伟', profit: 3420 },
  { id: 2, teamNo: 'HZ20250405-002', productName: '千岛湖二日游', departDate: '2025-04-05', count: 42, salesman: '李娜', profit: 5880 },
  { id: 3, teamNo: 'HZ20250410-003', productName: '乌镇西塘三日游', departDate: '2025-04-10', count: 30, salesman: '王芳', profit: 4500 },
  { id: 4, teamNo: 'HZ20250420-004', productName: '横店影视城二日游', departDate: '2025-04-20', count: 50, salesman: '赵强', profit: 7500 },
  { id: 5, teamNo: 'HZ20250425-005', productName: '普陀山祈福二日游', departDate: '2025-04-25', count: 25, salesman: '刘洋', profit: 3750 },
]);
</script>

<template>
  <Page title="历史团队" description="查询历史团队记录">
    <Card>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'profit'">
            <span class="text-green-600">¥{{ record.profit.toLocaleString() }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">查看详情</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
