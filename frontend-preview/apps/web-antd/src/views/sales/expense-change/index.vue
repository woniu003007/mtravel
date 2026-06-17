<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '变更单号', dataIndex: 'changeNo', key: 'changeNo' },
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '变更类型', dataIndex: 'changeType', key: 'changeType' },
  { title: '变更金额', dataIndex: 'amount', key: 'amount' },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, changeNo: 'BG20250510001', teamNo: 'HZ20250510-001', customer: '杭州阳光旅行社', changeType: '加人', amount: 596, applicant: '张伟', status: '已审批' },
  { id: 2, changeNo: 'BG20250512001', teamNo: 'HZ20250512-002', customer: '南京中青旅', changeType: '减人', amount: -1376, applicant: '李娜', status: '待审批' },
  { id: 3, changeNo: 'BG20250515001', teamNo: 'HZ20250515-003', customer: '苏州国旅', changeType: '升级房型', amount: 1200, applicant: '王芳', status: '已审批' },
  { id: 4, changeNo: 'BG20250516001', teamNo: 'HZ20250516-004', customer: '宁波康辉旅行社', changeType: '取消', amount: -5580, applicant: '赵强', status: '待审批' },
  { id: 5, changeNo: 'BG20250518001', teamNo: 'HZ20250518-005', customer: '温州中旅', changeType: '加项目', amount: 800, applicant: '刘洋', status: '已驳回' },
  { id: 6, changeNo: 'BG20250520001', teamNo: 'HZ20250520-006', customer: '绍兴旅游集散中心', changeType: '改期', amount: 0, applicant: '张伟', status: '已审批' },
]);
</script>

<template>
  <Page title="订单费用变更" description="管理订单费用变更记录">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">申请变更</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'changeType'">
            <Tag :color="record.changeType === '加人' ? 'blue' : record.changeType === '减人' ? 'orange' : record.changeType === '取消' ? 'red' : 'purple'">{{ record.changeType }}</Tag>
          </template>
          <template v-if="column.key === 'amount'">
            <span :class="record.amount >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ record.amount >= 0 ? '+' : '' }}¥{{ record.amount }}
            </span>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已审批' ? 'green' : record.status === '待审批' ? 'orange' : 'red'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">查看</Button>
            <Button type="link" size="small">审批</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
