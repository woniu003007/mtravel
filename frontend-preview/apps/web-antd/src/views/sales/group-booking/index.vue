<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '已报人数', dataIndex: 'bookedCount', key: 'bookedCount' },
  { title: '余位', dataIndex: 'remaining', key: 'remaining' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, teamNo: 'HZ20250520-SP01', productName: '杭州西湖一日游', departDate: '2025-05-20', bookedCount: 28, remaining: 12, status: '收客中' },
  { id: 2, teamNo: 'HZ20250522-SP02', productName: '千岛湖二日游', departDate: '2025-05-22', bookedCount: 40, remaining: 5, status: '收客中' },
  { id: 3, teamNo: 'HZ20250525-SP03', productName: '乌镇西塘三日游', departDate: '2025-05-25', bookedCount: 30, remaining: 0, status: '已满' },
  { id: 4, teamNo: 'HZ20250528-SP04', productName: '横店影视城二日游', departDate: '2025-05-28', bookedCount: 15, remaining: 35, status: '收客中' },
  { id: 5, teamNo: 'HZ20250601-SP05', productName: '普陀山祈福二日游', departDate: '2025-06-01', bookedCount: 0, remaining: 40, status: '待开放' },
]);
</script>

<template>
  <Page title="散拼团队预订" description="散客拼团预订管理">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">创建散拼团</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '收客中' ? 'green' : record.status === '已满' ? 'red' : 'orange'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">预订</Button>
            <Button type="link" size="small">查看</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
