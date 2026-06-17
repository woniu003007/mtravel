<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '月份', dataIndex: 'month', key: 'month' },
  { title: '景区', dataIndex: 'scenic', key: 'scenic' },
  { title: '酒店', dataIndex: 'hotel', key: 'hotel' },
  { title: '餐厅', dataIndex: 'restaurant', key: 'restaurant' },
  { title: '车队', dataIndex: 'transport', key: 'transport' },
  { title: '大交通', dataIndex: 'majorTransport', key: 'majorTransport' },
  { title: '其它', dataIndex: 'other', key: 'other' },
  { title: '合计', dataIndex: 'total', key: 'total' },
];

const data = ref([
  { id: 1, month: '2024-01', scenic: '¥180,000', hotel: '¥210,000', restaurant: '¥95,000', transport: '¥88,000', majorTransport: '¥45,000', other: '¥24,000', total: '¥642,000' },
  { id: 2, month: '2024-02', scenic: '¥150,000', hotel: '¥176,000', restaurant: '¥80,000', transport: '¥74,000', majorTransport: '¥38,000', other: '¥22,000', total: '¥540,000' },
  { id: 3, month: '2024-03', scenic: '¥256,000', hotel: '¥302,000', restaurant: '¥138,000', transport: '¥126,000', majorTransport: '¥62,000', other: '¥34,000', total: '¥918,000' },
  { id: 4, month: '2024-04', scenic: '¥322,000', hotel: '¥380,000', restaurant: '¥172,000', transport: '¥158,000', majorTransport: '¥78,000', other: '¥44,000', total: '¥1,154,000' },
  { id: 5, month: '2024-05', scenic: '¥386,000', hotel: '¥456,000', restaurant: '¥208,000', transport: '¥190,000', majorTransport: '¥92,000', other: '¥50,000', total: '¥1,382,000' },
  { id: 6, month: '2024-06', scenic: '¥452,000', hotel: '¥534,000', restaurant: '¥242,000', transport: '¥222,000', majorTransport: '¥108,000', other: '¥60,000', total: '¥1,618,000' },
]);
</script>

<template>
  <Page title="付款汇总统计" description="汇总统计付款情况">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <DatePicker.RangePicker />
        <Button type="primary">
          <template #icon><span class="i-ant-design:export-outlined" /></template>
          导出
        </Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id" />
    </Card>
  </Page>
</template>
