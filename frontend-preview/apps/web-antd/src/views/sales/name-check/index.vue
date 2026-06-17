<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Input, Table } from 'ant-design-vue';
import { ref } from 'vue';

const searchText = ref('');

const columns = [
  { title: '游客姓名', dataIndex: 'name', key: 'name' },
  { title: '证件号', dataIndex: 'idCard', key: 'idCard' },
  { title: '团号', dataIndex: 'teamNo', key: 'teamNo' },
  { title: '出发日期', dataIndex: 'departDate', key: 'departDate' },
  { title: '客户单位', dataIndex: 'customer', key: 'customer' },
];

const data = ref<any[]>([]);
</script>

<template>
  <Page title="名单查重" description="检查游客名单是否重复">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Input v-model:value="searchText" placeholder="请输入游客姓名或证件号" style="width: 300px" />
        <Button type="primary">查询</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id" />
    </Card>
  </Page>
</template>
