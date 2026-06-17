<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Card, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '付款日期', dataIndex: 'date', key: 'date' },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier' },
  { title: '团号', dataIndex: 'groupNo', key: 'groupNo' },
  { title: '付款金额', dataIndex: 'amount', key: 'amount' },
  { title: '付款方式', dataIndex: 'method', key: 'method' },
  { title: '银行账号', dataIndex: 'bankAccount', key: 'bankAccount' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
];

const data = ref([
  { id: 1, date: '2024-03-02', supplier: '云南大巴车队', groupNo: 'GL2024-0301', amount: 12000, method: '银行转账', bankAccount: '6225****1234', operator: '财务张', remark: '用车费' },
  { id: 2, date: '2024-03-04', supplier: '丽江花园酒店', groupNo: 'GL2024-0301', amount: 28000, method: '银行转账', bankAccount: '6225****5678', operator: '财务李', remark: '住宿费' },
  { id: 3, date: '2024-03-06', supplier: '大理古城餐厅', groupNo: 'GL2024-0302', amount: 8500, method: '银行转账', bankAccount: '6225****9012', operator: '财务张', remark: '餐费' },
  { id: 4, date: '2024-03-09', supplier: '玉龙雪山景区', groupNo: 'GL2024-0303', amount: 15600, method: '对公转账', bankAccount: '6225****3456', operator: '财务王', remark: '门票费' },
  { id: 5, date: '2024-03-11', supplier: '昆明航空票务', groupNo: 'GL2024-0304', amount: 45000, method: '银行转账', bankAccount: '6225****7890', operator: '财务李', remark: '机票费' },
  { id: 6, date: '2024-03-13', supplier: '西双版纳度假村', groupNo: 'GL2024-0305', amount: 35000, method: '银行转账', bankAccount: '6225****2345', operator: '财务张', remark: '住宿费' },
  { id: 7, date: '2024-03-16', supplier: '丽江演艺公司', groupNo: 'GL2024-0306', amount: 6800, method: '支付宝', bankAccount: '-', operator: '财务王', remark: '演出票' },
  { id: 8, date: '2024-03-19', supplier: '香格里拉车队', groupNo: 'GL2024-0307', amount: 18000, method: '银行转账', bankAccount: '6225****6789', operator: '财务李', remark: '包车费' },
]);
</script>

<template>
  <Page title="付款记录" description="查看付款流水记录">
    <Card>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id" />
    </Card>
  </Page>
</template>
