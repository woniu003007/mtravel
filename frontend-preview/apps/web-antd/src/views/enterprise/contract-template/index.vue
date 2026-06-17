<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '合同类型', dataIndex: 'type', key: 'type' },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 200 },
];

const data = ref([
  { id: 1, name: '地接合同模板', type: '地接服务', updateTime: '2025-03-15 10:30:00', status: '启用' },
  { id: 2, name: '分销商合同模板', type: '分销合作', updateTime: '2025-03-10 14:20:00', status: '启用' },
  { id: 3, name: '景区合作合同模板', type: '景区合作', updateTime: '2025-02-28 09:15:00', status: '启用' },
  { id: 4, name: '酒店协议价合同模板', type: '酒店合作', updateTime: '2025-02-20 16:45:00', status: '启用' },
  { id: 5, name: '车队租赁合同模板', type: '车队合作', updateTime: '2025-01-18 11:00:00', status: '停用' },
]);
</script>

<template>
  <Page title="合同模板管理" description="管理各类合同模板">
    <Card>
      <div class="mb-4 flex items-center gap-3">
        <Button type="primary">添加模板</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10 }" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <Tag color="blue">{{ record.type }}</Tag>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === '启用' ? 'success' : 'default'">
              {{ record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button type="link" size="small">编辑</Button>
            <Button type="link" size="small">预览</Button>
            <Button type="link" size="small" danger>删除</Button>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
