<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '人数', dataIndex: 'count', key: 'count' },
  { title: '销售员', dataIndex: 'salesman', key: 'salesman' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, teamNo: 'HZ20250510-001', productName: '杭州西湖一日游', type: '散拼', departDate: '2025-05-10', count: 32, salesman: '张伟', status: '已出行' },
  { id: 2, teamNo: 'HZ20250512-002', productName: '千岛湖二日游', type: '整团', departDate: '2025-05-12', count: 45, salesman: '李娜', status: '已确认' },
  { id: 3, teamNo: 'HZ20250515-003', productName: '乌镇西塘三日游', type: '散拼', departDate: '2025-05-15', count: 28, salesman: '王芳', status: '待确认' },
  { id: 4, teamNo: 'HZ20250516-004', productName: '横店影视城二日游', type: '整团', departDate: '2025-05-16', count: 50, salesman: '赵强', status: '已确认' },
  { id: 5, teamNo: 'HZ20250518-005', productName: '普陀山祈福二日游', type: '散团', departDate: '2025-05-18', count: 20, salesman: '张伟', status: '待确认' },
  { id: 6, teamNo: 'HZ20250520-006', productName: '莫干山度假三日游', type: '单项', departDate: '2025-05-20', count: 12, salesman: '刘洋', status: '已取消' },
  { id: 7, teamNo: 'HZ20250522-007', productName: '杭州深度四日定制游', type: '整团', departDate: '2025-05-22', count: 8, salesman: '李娜', status: '待确认' },
  { id: 8, teamNo: 'HZ20250525-008', productName: '千岛湖二日游', type: '散拼', departDate: '2025-05-25', count: 36, salesman: '王芳', status: '已确认' },
  { id: 9, teamNo: 'HZ20250528-009', productName: '杭州西湖一日游', type: '散拼', departDate: '2025-05-28', count: 40, salesman: '赵强', status: '待确认' },
  { id: 10, teamNo: 'HZ20250530-010', productName: '浙东大峡谷探险二日游', type: '整团', departDate: '2025-05-30', count: 25, salesman: '刘洋', status: '已确认' },
]);
</script>

<template>
  <Page title="团队管理" description="管理旅游团队信息">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加团队</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <Tag :color="record.type === '散拼' ? 'blue' : record.type === '整团' ? 'green' : record.type === '散团' ? 'orange' : 'purple'">{{ record.type }}</Tag>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已出行' ? 'green' : record.status === '已确认' ? 'blue' : record.status === '已取消' ? 'red' : 'orange'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">查看</Button>
            <Button type="link" size="small">编辑</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
