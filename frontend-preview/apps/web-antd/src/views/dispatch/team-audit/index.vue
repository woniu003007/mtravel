<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo', width: 130 },
  { title: '产品', dataIndex: 'product', key: 'product' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate', width: 110 },
  { title: '人数', dataIndex: 'people', key: 'people', width: 60 },
  { title: '计调员', dataIndex: 'dispatcher', key: 'dispatcher', width: 90 },
  { title: '安排进度', dataIndex: 'progress', key: 'progress', width: 100 },
  { title: '审核状态', dataIndex: 'auditStatus', key: 'auditStatus', width: 100 },
  { title: '操作', key: 'action', width: 150 },
];

const data = ref([
  { id: 1, teamNo: 'TM2024060101', product: '桂林三日精华游', departDate: '2024-06-15', people: 28, dispatcher: '李明', progress: '6/6', auditStatus: '已通过' },
  { id: 2, teamNo: 'TM2024060102', product: '阳朔漓江深度四日游', departDate: '2024-06-16', people: 32, dispatcher: '王芳', progress: '6/6', auditStatus: '待审核' },
  { id: 3, teamNo: 'TM2024060106', product: '桂林市区文化两日游', departDate: '2024-06-19', people: 22, dispatcher: '李明', progress: '6/6', auditStatus: '已通过' },
  { id: 4, teamNo: 'TM2024060105', product: '漓江竹筏一日游', departDate: '2024-06-18', people: 12, dispatcher: '张伟', progress: '5/6', auditStatus: '已驳回' },
  { id: 5, teamNo: 'TM2024060107', product: '银子岩+遇龙河三日游', departDate: '2024-06-20', people: 36, dispatcher: '王芳', progress: '6/6', auditStatus: '待审核' },
  { id: 6, teamNo: 'TM2024060103', product: '龙脊梯田两日游', departDate: '2024-06-17', people: 18, dispatcher: '张伟', progress: '6/6', auditStatus: '已通过' },
]);
</script>

<template>
  <Page title="团队审核" description="审核团队安排是否完整">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">批量审核</Button>
        <Button>导出报表</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'progress'">
            <span :class="record.progress === '6/6' ? 'text-green-500' : 'text-orange-500'">
              {{ record.progress }}
            </span>
          </template>
          <template v-if="column.key === 'auditStatus'">
            <Tag v-if="record.auditStatus === '已通过'" color="green">已通过</Tag>
            <Tag v-else-if="record.auditStatus === '待审核'" color="orange">待审核</Tag>
            <Tag v-else color="red">已驳回</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">审核</Button>
            <Button type="link" size="small">详情</Button>
            <Button type="link" size="small" danger>驳回</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
