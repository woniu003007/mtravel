<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Switch, Table } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '菜单名称', dataIndex: 'name', key: 'name' },
  { title: '图标', dataIndex: 'icon', key: 'icon' },
  { title: '是否显示', dataIndex: 'visible', key: 'visible', width: 120 },
  { title: '操作', key: 'action', width: 150 },
];

const data = ref([
  { key: '1', sort: 1, name: '团队管理', icon: 'TeamOutlined', visible: true },
  { key: '2', sort: 2, name: '订单管理', icon: 'FileTextOutlined', visible: true },
  { key: '3', sort: 3, name: '导游排班', icon: 'ScheduleOutlined', visible: true },
  { key: '4', sort: 4, name: '应收账款', icon: 'AccountBookOutlined', visible: true },
  { key: '5', sort: 5, name: '应付账款', icon: 'MoneyCollectOutlined', visible: true },
  { key: '6', sort: 6, name: '资源管理', icon: 'AppstoreOutlined', visible: false },
  { key: '7', sort: 7, name: '数据报表', icon: 'BarChartOutlined', visible: true },
  { key: '8', sort: 8, name: '客户管理', icon: 'UserOutlined', visible: false },
]);
</script>

<template>
  <Page title="快捷菜单设置" description="配置首页快捷菜单入口">
    <Card>
      <div class="mb-4 flex justify-between">
        <span class="text-gray-500">拖拽行可调整排序，最多显示8个快捷菜单</span>
        <Button type="primary">保存排序</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sort'">
            <span class="cursor-move text-gray-400">☰ {{ record.sort }}</span>
          </template>
          <template v-if="column.key === 'visible'">
            <Switch v-model:checked="record.visible" />
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">上移</Button>
            <Button type="link" size="small">下移</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
