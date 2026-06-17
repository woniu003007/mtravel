<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo', width: 130 },
  { title: '接送类型', dataIndex: 'type', key: 'type', width: 90 },
  { title: '站点', dataIndex: 'station', key: 'station', width: 120 },
  { title: '航班/车次', dataIndex: 'flightTrain', key: 'flightTrain', width: 110 },
  { title: '到达时间', dataIndex: 'arrivalTime', key: 'arrivalTime', width: 150 },
  { title: '人数', dataIndex: 'people', key: 'people', width: 60 },
  { title: '接送车辆', dataIndex: 'vehicle', key: 'vehicle', width: 120 },
  { title: '司机', dataIndex: 'driver', key: 'driver', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 120 },
];

const data = ref([
  { id: 1, teamNo: 'TM2024060101', type: '接站', station: '桂林两江机场', flightTrain: 'CZ3289', arrivalTime: '2024-06-15 14:30', people: 28, vehicle: '桂B·88001 大巴', driver: '黄师傅', status: '已安排' },
  { id: 2, teamNo: 'TM2024060101', type: '送站', station: '桂林两江机场', flightTrain: 'MU5302', arrivalTime: '2024-06-17 18:00', people: 28, vehicle: '桂B·88001 大巴', driver: '黄师傅', status: '已安排' },
  { id: 3, teamNo: 'TM2024060102', type: '接站', station: '桂林北站', flightTrain: 'G2901', arrivalTime: '2024-06-16 10:15', people: 32, vehicle: '桂B·66003 大巴', driver: '韦师傅', status: '已安排' },
  { id: 4, teamNo: 'TM2024060102', type: '送站', station: '桂林北站', flightTrain: 'G2918', arrivalTime: '2024-06-19 16:45', people: 32, vehicle: '', driver: '', status: '待安排' },
  { id: 5, teamNo: 'TM2024060103', type: '接站', station: '桂林西站', flightTrain: 'D8201', arrivalTime: '2024-06-17 08:30', people: 18, vehicle: '桂B·77005 中巴', driver: '莫师傅', status: '已安排' },
  { id: 6, teamNo: 'TM2024060104', type: '接站', station: '桂林两江机场', flightTrain: 'CA1845', arrivalTime: '2024-06-18 12:00', people: 45, vehicle: '', driver: '', status: '待安排' },
  { id: 7, teamNo: 'TM2024060105', type: '接站', station: '阳朔汽车站', flightTrain: '旅游专线', arrivalTime: '2024-06-18 09:00', people: 12, vehicle: '桂B·55008 商务车', driver: '覃师傅', status: '进行中' },
  { id: 8, teamNo: 'TM2024060106', type: '接站', station: '桂林站', flightTrain: 'K956', arrivalTime: '2024-06-19 07:20', people: 22, vehicle: '桂B·66003 大巴', driver: '韦师傅', status: '已安排' },
]);
</script>

<template>
  <Page title="接送信息" description="管理团队接送站信息">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加接送</Button>
        <Button>导出列表</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <Tag :color="record.type === '接站' ? 'blue' : 'green'">{{ record.type }}</Tag>
          </template>
          <template v-if="column.key === 'status'">
            <Tag v-if="record.status === '已安排'" color="green">已安排</Tag>
            <Tag v-else-if="record.status === '进行中'" color="blue">进行中</Tag>
            <Tag v-else color="orange">待安排</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">编辑</Button>
            <Button type="link" size="small">详情</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
