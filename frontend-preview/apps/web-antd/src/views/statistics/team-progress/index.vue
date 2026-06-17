<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '月份', dataIndex: 'month', key: 'month' },
  { title: '总团数', dataIndex: 'total', key: 'total' },
  { title: '已排团', dataIndex: 'arranged', key: 'arranged' },
  { title: '已出行', dataIndex: 'departed', key: 'departed' },
  { title: '已结算', dataIndex: 'settled', key: 'settled' },
  { title: '未结算', dataIndex: 'unsettled', key: 'unsettled' },
  { title: '完成率', dataIndex: 'completionRate', key: 'completionRate' },
];

const data = ref([
  { id: 1, month: '2024-01', total: 45, arranged: 45, departed: 43, settled: 40, unsettled: 3, completionRate: '93%' },
  { id: 2, month: '2024-02', total: 38, arranged: 38, departed: 36, settled: 34, unsettled: 2, completionRate: '94%' },
  { id: 3, month: '2024-03', total: 62, arranged: 60, departed: 58, settled: 52, unsettled: 6, completionRate: '90%' },
  { id: 4, month: '2024-04', total: 78, arranged: 75, departed: 72, settled: 65, unsettled: 7, completionRate: '90%' },
  { id: 5, month: '2024-05', total: 95, arranged: 90, departed: 85, settled: 70, unsettled: 15, completionRate: '82%' },
  { id: 6, month: '2024-06', total: 110, arranged: 100, departed: 88, settled: 60, unsettled: 28, completionRate: '68%' },
]);
</script>

<template>
  <Page title="团队进度统计" description="统计团队各阶段进度">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <DatePicker.RangePicker />
        <Button type="primary">
          <template #icon><span class="i-ant-design:export-outlined" /></template>
          导出
        </Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'completionRate'">
            <Tag :color="parseInt(record.completionRate) >= 90 ? 'green' : parseInt(record.completionRate) >= 80 ? 'orange' : 'red'">
              {{ record.completionRate }}
            </Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
