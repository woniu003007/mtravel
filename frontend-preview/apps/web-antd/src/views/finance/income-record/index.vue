<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Card, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '收款日期', dataIndex: 'date', key: 'date' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '团号', dataIndex: 'groupNo', key: 'groupNo' },
  { title: '收款金额', dataIndex: 'amount', key: 'amount' },
  { title: '收款方式', dataIndex: 'method', key: 'method' },
  { title: '银行账号', dataIndex: 'bankAccount', key: 'bankAccount' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
];

const data = ref([
  { id: 1, date: '2024-03-01', customer: '北京阳光旅行社', groupNo: 'GL2024-0301', amount: 85000, method: '银行转账', bankAccount: '6222****8901', operator: '财务张', remark: '团款首付' },
  { id: 2, date: '2024-03-03', customer: '上海环球国旅', groupNo: 'GL2024-0302', amount: 62000, method: '银行转账', bankAccount: '6222****3456', operator: '财务李', remark: '全款' },
  { id: 3, date: '2024-03-05', customer: '广州南方旅游', groupNo: 'GL2024-0303', amount: 45000, method: '支付宝', bankAccount: '-', operator: '财务张', remark: '预付款' },
  { id: 4, date: '2024-03-08', customer: '深圳畅游旅行社', groupNo: 'GL2024-0304', amount: 120000, method: '银行转账', bankAccount: '6222****7890', operator: '财务王', remark: '团款全额' },
  { id: 5, date: '2024-03-10', customer: '北京阳光旅行社', groupNo: 'GL2024-0301', amount: 35000, method: '银行转账', bankAccount: '6222****8901', operator: '财务张', remark: '团款尾款' },
  { id: 6, date: '2024-03-12', customer: '成都天府旅行社', groupNo: 'GL2024-0305', amount: 78000, method: '银行转账', bankAccount: '6222****2345', operator: '财务李', remark: '全款' },
  { id: 7, date: '2024-03-15', customer: '杭州西湖国旅', groupNo: 'GL2024-0306', amount: 55000, method: '微信转账', bankAccount: '-', operator: '财务王', remark: '预付款50%' },
  { id: 8, date: '2024-03-18', customer: '武汉长江旅行社', groupNo: 'GL2024-0307', amount: 92000, method: '银行转账', bankAccount: '6222****6789', operator: '财务张', remark: '全款' },
]);
</script>

<template>
  <Page title="收款记录" description="查看收款流水记录">
    <Card>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id" />
    </Card>
  </Page>
</template>
