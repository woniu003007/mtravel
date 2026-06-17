<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '购物店', dataIndex: 'shopName', key: 'shopName' },
  { title: '团号', dataIndex: 'groupNo', key: 'groupNo' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '进店人数', dataIndex: 'visitors', key: 'visitors' },
  { title: '消费金额', dataIndex: 'amount', key: 'amount' },
  { title: '返佣比例', dataIndex: 'rebateRate', key: 'rebateRate' },
  { title: '返佣金额', dataIndex: 'rebateAmount', key: 'rebateAmount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', dataIndex: 'action', key: 'action' },
];

const data = ref([
  { id: 1, shopName: '翡翠王朝珠宝店', groupNo: 'GL2024-0301', departDate: '2024-03-01', visitors: 28, amount: 56000, rebateRate: '12%', rebateAmount: 6720, status: '已到账' },
  { id: 2, shopName: '银饰工坊', groupNo: 'GL2024-0302', departDate: '2024-03-05', visitors: 32, amount: 24000, rebateRate: '10%', rebateAmount: 2400, status: '待确认' },
  { id: 3, shopName: '茶叶专卖店', groupNo: 'GL2024-0303', departDate: '2024-03-08', visitors: 25, amount: 18500, rebateRate: '15%', rebateAmount: 2775, status: '已到账' },
  { id: 4, shopName: '丝绸之路旗舰店', groupNo: 'GL2024-0304', departDate: '2024-03-12', visitors: 30, amount: 42000, rebateRate: '8%', rebateAmount: 3360, status: '待结算' },
  { id: 5, shopName: '玉器天堂', groupNo: 'GL2024-0305', departDate: '2024-03-15', visitors: 22, amount: 68000, rebateRate: '10%', rebateAmount: 6800, status: '已到账' },
  { id: 6, shopName: '特产超市', groupNo: 'GL2024-0306', departDate: '2024-03-18', visitors: 35, amount: 12000, rebateRate: '5%', rebateAmount: 600, status: '待确认' },
]);
</script>

<template>
  <Page title="购物返佣" description="管理购物店返佣记录">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加返佣记录</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '已到账' ? 'green' : record.status === '待确认' ? 'orange' : 'blue'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">详情</Button>
            <Button type="link" size="small">确认</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
