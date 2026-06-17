<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '客户单位', dataIndex: 'customer', key: 'customer' },
  { title: '联系人', dataIndex: 'contact', key: 'contact' },
  { title: '人数(成人/儿童)', dataIndex: 'count', key: 'count' },
  { title: '总金额', dataIndex: 'amount', key: 'amount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, orderNo: 'OD20250510001', teamNo: 'HZ20250510-001', customer: '杭州阳光旅行社', contact: '陈明', count: '8/2', amount: 2980, status: '已确认' },
  { id: 2, orderNo: 'OD20250510002', teamNo: 'HZ20250510-001', customer: '上海春秋旅行社', contact: '周丽', count: '12/3', amount: 4470, status: '已确认' },
  { id: 3, orderNo: 'OD20250512001', teamNo: 'HZ20250512-002', customer: '南京中青旅', contact: '吴刚', count: '20/5', amount: 17200, status: '待付款' },
  { id: 4, orderNo: 'OD20250515001', teamNo: 'HZ20250515-003', customer: '苏州国旅', contact: '孙燕', count: '15/3', amount: 17640, status: '待确认' },
  { id: 5, orderNo: 'OD20250516001', teamNo: 'HZ20250516-004', customer: '宁波康辉旅行社', contact: '马超', count: '25/5', amount: 16740, status: '已确认' },
  { id: 6, orderNo: 'OD20250518001', teamNo: 'HZ20250518-005', customer: '温州中旅', contact: '林芳', count: '10/2', amount: 8640, status: '已取消' },
  { id: 7, orderNo: 'OD20250520001', teamNo: 'HZ20250520-006', customer: '绍兴旅游集散中心', contact: '何伟', count: '6/0', amount: 7680, status: '待确认' },
  { id: 8, orderNo: 'OD20250522001', teamNo: 'HZ20250522-007', customer: '嘉兴南湖旅行社', contact: '黄磊', count: '4/2', amount: 15480, status: '已确认' },
]);
</script>

<template>
  <Page title="订单管理" description="管理旅游订单">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加订单</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount.toLocaleString() }}
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已确认' ? 'green' : record.status === '待付款' ? 'orange' : record.status === '已取消' ? 'red' : 'blue'">{{ record.status }}</Tag>
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
