<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo', width: 120 },
  { title: '产品', dataIndex: 'product', key: 'product' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate', width: 110 },
  { title: '人数', dataIndex: 'people', key: 'people', width: 60 },
  { title: '导游', dataIndex: 'guide', key: 'guide', width: 70 },
  { title: '交通', dataIndex: 'transport', key: 'transport', width: 70 },
  { title: '住宿', dataIndex: 'hotel', key: 'hotel', width: 70 },
  { title: '车辆', dataIndex: 'vehicle', key: 'vehicle', width: 70 },
  { title: '景区', dataIndex: 'scenic', key: 'scenic', width: 70 },
  { title: '餐厅', dataIndex: 'restaurant', key: 'restaurant', width: 70 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 120 },
];

const data = ref([
  { id: 1, teamNo: 'TM2024060101', product: '桂林三日精华游', departDate: '2024-06-15', people: 28, guide: true, transport: true, hotel: true, vehicle: true, scenic: true, restaurant: true, status: '已完成' },
  { id: 2, teamNo: 'TM2024060102', product: '阳朔漓江深度四日游', departDate: '2024-06-16', people: 32, guide: true, transport: true, hotel: true, vehicle: false, scenic: true, restaurant: false, status: '安排中' },
  { id: 3, teamNo: 'TM2024060103', product: '龙脊梯田两日游', departDate: '2024-06-17', people: 18, guide: true, transport: false, hotel: true, vehicle: false, scenic: false, restaurant: false, status: '安排中' },
  { id: 4, teamNo: 'TM2024060104', product: '桂林+阳朔五日豪华游', departDate: '2024-06-18', people: 45, guide: false, transport: false, hotel: false, vehicle: false, scenic: false, restaurant: false, status: '待安排' },
  { id: 5, teamNo: 'TM2024060105', product: '漓江竹筏一日游', departDate: '2024-06-18', people: 12, guide: true, transport: true, hotel: false, vehicle: true, scenic: true, restaurant: true, status: '安排中' },
  { id: 6, teamNo: 'TM2024060106', product: '桂林市区文化两日游', departDate: '2024-06-19', people: 22, guide: true, transport: true, hotel: true, vehicle: true, scenic: true, restaurant: true, status: '已完成' },
  { id: 7, teamNo: 'TM2024060107', product: '银子岩+遇龙河三日游', departDate: '2024-06-20', people: 36, guide: true, transport: false, hotel: true, vehicle: false, scenic: false, restaurant: false, status: '安排中' },
  { id: 8, teamNo: 'TM2024060108', product: '桂林亲子研学四日游', departDate: '2024-06-21', people: 40, guide: false, transport: false, hotel: false, vehicle: false, scenic: false, restaurant: false, status: '待安排' },
]);
</script>

<template>
  <Page title="团队安排" description="计调人员安排团队资源">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加团队</Button>
        <Button>批量安排</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="['guide', 'transport', 'hotel', 'vehicle', 'scenic', 'restaurant'].includes(column.key as string)">
            <span v-if="record[column.key as string]" class="text-green-500">✓</span>
            <span v-else class="text-gray-400">○</span>
          </template>
          <template v-if="column.key === 'status'">
            <Tag v-if="record.status === '已完成'" color="green">已完成</Tag>
            <Tag v-else-if="record.status === '安排中'" color="blue">安排中</Tag>
            <Tag v-else color="default">待安排</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">安排</Button>
            <Button type="link" size="small">详情</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
