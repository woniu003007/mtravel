<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '证件类型', dataIndex: 'idType', key: 'idType' },
  { title: '证件号', dataIndex: 'idCard', key: 'idCard' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '标签', dataIndex: 'tags', key: 'tags' },
  { title: '出行次数', dataIndex: 'tripCount', key: 'tripCount' },
  { title: '操作', key: 'action' },
];

const data = ref([
  { id: 1, name: '王建国', idType: '身份证', idCard: '330102199001****', phone: '138****5678', tags: ['VIP', '常客'], tripCount: 12 },
  { id: 2, name: '李秀英', idType: '身份证', idCard: '310101198505****', phone: '139****2345', tags: ['常客'], tripCount: 8 },
  { id: 3, name: '张明远', idType: '身份证', idCard: '320106199203****', phone: '137****8901', tags: [], tripCount: 3 },
  { id: 4, name: '陈晓红', idType: '身份证', idCard: '330106198812****', phone: '136****4567', tags: ['VIP'], tripCount: 15 },
  { id: 5, name: '刘德华', idType: '护照', idCard: 'E12345****', phone: '135****7890', tags: ['境外'], tripCount: 2 },
  { id: 6, name: '赵丽颖', idType: '身份证', idCard: '330108199505****', phone: '158****1234', tags: [], tripCount: 1 },
  { id: 7, name: '孙志强', idType: '身份证', idCard: '330102197808****', phone: '159****5678', tags: ['常客', 'VIP'], tripCount: 20 },
  { id: 8, name: '周婷婷', idType: '身份证', idCard: '330109200001****', phone: '188****9012', tags: [], tripCount: 4 },
]);
</script>

<template>
  <Page title="客人信息" description="管理游客信息">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加客人</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'tags'">
            <Tag v-for="tag in record.tags" :key="tag" :color="tag === 'VIP' ? 'gold' : tag === '常客' ? 'blue' : 'green'">{{ tag }}</Tag>
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
