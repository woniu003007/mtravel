<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '站点名称', dataIndex: 'name', key: 'name' },
  { title: '站点类型', dataIndex: 'type', key: 'type' },
  { title: '城市', dataIndex: 'city', key: 'city' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 180 },
];

const data = ref([
  { id: 1, name: '杭州东站', type: '火车站', city: '杭州', status: '启用' },
  { id: 2, name: '萧山国际机场', type: '机场', city: '杭州', status: '启用' },
  { id: 3, name: '杭州城站', type: '火车站', city: '杭州', status: '启用' },
  { id: 4, name: '杭州西站', type: '火车站', city: '杭州', status: '启用' },
  { id: 5, name: '杭州汽车客运中心', type: '汽车站', city: '杭州', status: '启用' },
  { id: 6, name: '杭州汽车南站', type: '汽车站', city: '杭州', status: '停用' },
  { id: 7, name: '宁波栎社机场', type: '机场', city: '宁波', status: '启用' },
  { id: 8, name: '宁波站', type: '火车站', city: '宁波', status: '启用' },
  { id: 9, name: '温州龙湾机场', type: '机场', city: '温州', status: '启用' },
  { id: 10, name: '温州南站', type: '火车站', city: '温州', status: '启用' },
  { id: 11, name: '义乌站', type: '火车站', city: '金华', status: '启用' },
  { id: 12, name: '金华站', type: '火车站', city: '金华', status: '启用' },
  { id: 13, name: '嘉兴南站', type: '火车站', city: '嘉兴', status: '启用' },
  { id: 14, name: '湖州站', type: '火车站', city: '湖州', status: '启用' },
  { id: 15, name: '绍兴北站', type: '火车站', city: '绍兴', status: '启用' },
  { id: 16, name: '衢州站', type: '火车站', city: '衢州', status: '停用' },
  { id: 17, name: '台州站', type: '火车站', city: '台州', status: '启用' },
  { id: 18, name: '丽水站', type: '火车站', city: '丽水', status: '启用' },
  { id: 19, name: '舟山普陀山机场', type: '机场', city: '舟山', status: '启用' },
  { id: 20, name: '杭州汽车北站', type: '汽车站', city: '杭州', status: '启用' },
  { id: 21, name: '杭州汽车西站', type: '汽车站', city: '杭州', status: '启用' },
  { id: 22, name: '千岛湖站', type: '火车站', city: '杭州', status: '启用' },
  { id: 23, name: '桐庐站', type: '火车站', city: '杭州', status: '启用' },
  { id: 24, name: '建德站', type: '火车站', city: '杭州', status: '停用' },
]);
</script>

<template>
  <Page title="接送站管理" description="管理旅游接送站点信息">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加站点</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <Tag :color="record.type === '机场' ? 'blue' : record.type === '火车站' ? 'green' : 'orange'">
              {{ record.type }}
            </Tag>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '启用' ? 'success' : 'default'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">编辑</Button>
            <Button type="link" size="small" danger>删除</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
