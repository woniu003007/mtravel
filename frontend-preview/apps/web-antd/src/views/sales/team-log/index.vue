<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '操作类型', dataIndex: 'opType', key: 'opType' },
  { title: '操作内容', dataIndex: 'content', key: 'content' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
  { title: '操作时间', dataIndex: 'opTime', key: 'opTime' },
];

const data = ref([
  { id: 1, teamNo: 'HZ20250510-001', opType: '创建', content: '创建团队，产品：杭州西湖一日游', operator: '张伟', opTime: '2025-05-08 09:30:00' },
  { id: 2, teamNo: 'HZ20250510-001', opType: '添加订单', content: '添加订单OD20250510001，客户：杭州阳光旅行社', operator: '张伟', opTime: '2025-05-08 10:15:00' },
  { id: 3, teamNo: 'HZ20250510-001', opType: '添加订单', content: '添加订单OD20250510002，客户：上海春秋旅行社', operator: '张伟', opTime: '2025-05-08 14:20:00' },
  { id: 4, teamNo: 'HZ20250512-002', opType: '创建', content: '创建团队，产品：千岛湖二日游', operator: '李娜', opTime: '2025-05-09 08:45:00' },
  { id: 5, teamNo: 'HZ20250510-001', opType: '确认', content: '团队状态变更为已确认', operator: '张伟', opTime: '2025-05-09 11:00:00' },
  { id: 6, teamNo: 'HZ20250515-003', opType: '创建', content: '创建团队，产品：乌镇西塘三日游', operator: '王芳', opTime: '2025-05-10 09:00:00' },
  { id: 7, teamNo: 'HZ20250510-001', opType: '费用变更', content: '加人变更，金额+¥596', operator: '张伟', opTime: '2025-05-10 15:30:00' },
  { id: 8, teamNo: 'HZ20250512-002', opType: '确认', content: '团队状态变更为已确认', operator: '李娜', opTime: '2025-05-11 10:00:00' },
]);
</script>

<template>
  <Page title="团号日志" description="查看团队操作日志">
    <Card>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'opType'">
            <Tag :color="record.opType === '创建' ? 'blue' : record.opType === '确认' ? 'green' : record.opType === '费用变更' ? 'orange' : 'purple'">{{ record.opType }}</Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
