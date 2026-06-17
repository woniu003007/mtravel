<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '车牌号', dataIndex: 'plateNo', key: 'plateNo' },
  { title: '车型', dataIndex: 'model', key: 'model' },
  { title: '座位数', dataIndex: 'seats', key: 'seats' },
  { title: '所属车队', dataIndex: 'fleet', key: 'fleet' },
  { title: '备案状态', dataIndex: 'recordStatus', key: 'recordStatus' },
  { title: '保险到期', dataIndex: 'insuranceExpire', key: 'insuranceExpire' },
  { title: '年审到期', dataIndex: 'inspectionExpire', key: 'inspectionExpire' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, plateNo: '浙A12345', model: '金龙大巴', seats: 53, fleet: '浙江安顺车队', recordStatus: '已备案', insuranceExpire: '2025-09-15', inspectionExpire: '2025-11-20' },
  { id: 2, plateNo: '浙A23456', model: '宇通中巴', seats: 35, fleet: '浙江安顺车队', recordStatus: '已备案', insuranceExpire: '2025-08-10', inspectionExpire: '2025-10-05' },
  { id: 3, plateNo: '浙A34567', model: '丰田考斯特', seats: 22, fleet: '杭州畅游车队', recordStatus: '已备案', insuranceExpire: '2025-12-01', inspectionExpire: '2026-01-15' },
  { id: 4, plateNo: '浙A45678', model: '别克GL8', seats: 7, fleet: '杭州畅游车队', recordStatus: '已备案', insuranceExpire: '2025-07-20', inspectionExpire: '2025-09-30' },
  { id: 5, plateNo: '浙A56789', model: '金龙大巴', seats: 49, fleet: '浙江安顺车队', recordStatus: '已备案', insuranceExpire: '2025-11-08', inspectionExpire: '2026-02-28' },
  { id: 6, plateNo: '浙A67890', model: '奔驰商务', seats: 7, fleet: '杭州畅游车队', recordStatus: '未备案', insuranceExpire: '2025-06-15', inspectionExpire: '2025-08-20' },
  { id: 7, plateNo: '浙A78901', model: '宇通大巴', seats: 55, fleet: '浙江顺达运输', recordStatus: '已备案', insuranceExpire: '2025-10-22', inspectionExpire: '2025-12-10' },
  { id: 8, plateNo: '浙A89012', model: '丰田海狮', seats: 13, fleet: '浙江顺达运输', recordStatus: '已备案', insuranceExpire: '2026-01-05', inspectionExpire: '2026-03-18' },
  { id: 9, plateNo: '浙A90123', model: '金龙中巴', seats: 37, fleet: '浙江安顺车队', recordStatus: '已过期', insuranceExpire: '2025-04-30', inspectionExpire: '2025-05-15' },
]);
</script>

<template>
  <Page title="车辆管理" description="管理旅游车辆信息">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加车辆</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'seats'">
            <span>{{ record.seats }}座</span>
          </template>
          <template v-if="column.key === 'recordStatus'">
            <Tag :color="record.recordStatus === '已备案' ? 'success' : record.recordStatus === '未备案' ? 'warning' : 'error'">
              {{ record.recordStatus }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">编辑</Button>
            <Button type="link" size="small">调度</Button>
            <Button type="link" size="small" danger>停用</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
