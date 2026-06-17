<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tabs, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const activeTab = ref('all');

const columns = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo' },
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '签署日期', dataIndex: 'signDate', key: 'signDate' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, contractNo: 'HT20250510001', teamNo: 'HZ20250510-001', customer: '杭州阳光旅行社', signDate: '2025-05-08', status: '已签署' },
  { id: 2, contractNo: 'HT20250510002', teamNo: 'HZ20250510-001', customer: '上海春秋旅行社', signDate: '2025-05-09', status: '已签署' },
  { id: 3, contractNo: 'HT20250512001', teamNo: 'HZ20250512-002', customer: '南京中青旅', signDate: '', status: '待签署' },
  { id: 4, contractNo: 'HT20250515001', teamNo: 'HZ20250515-003', customer: '苏州国旅', signDate: '', status: '待签署' },
  { id: 5, contractNo: 'HT20250516001', teamNo: 'HZ20250516-004', customer: '宁波康辉旅行社', signDate: '2025-05-14', status: '已签署' },
  { id: 6, contractNo: 'HT20250518001', teamNo: 'HZ20250518-005', customer: '温州中旅', signDate: '2025-05-16', status: '已签署' },
  { id: 7, contractNo: 'HT20250520001', teamNo: 'HZ20250520-006', customer: '绍兴旅游集散中心', signDate: '', status: '待签署' },
  { id: 8, contractNo: 'HT20250522001', teamNo: 'HZ20250522-007', customer: '嘉兴南湖旅行社', signDate: '2025-05-20', status: '已签署' },
]);
</script>

<template>
  <Page title="电子合同" description="管理电子合同签署">
    <Card>
      <Tabs v-model:activeKey="activeTab">
        <TabPane key="all" tab="全部" />
        <TabPane key="pending" tab="待签署" />
        <TabPane key="signed" tab="已签署" />
      </Tabs>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'signDate'">
            {{ record.signDate || '-' }}
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已签署' ? 'green' : 'orange'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">查看</Button>
            <Button v-if="record.status === '待签署'" type="link" size="small">发起签署</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
