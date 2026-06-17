<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, DatePicker, Select, Table, Tag } from 'ant-design-vue';
import { ref } from 'vue';

const columns = [
  { title: '操作时间', dataIndex: 'time', key: 'time', width: 180 },
  { title: '操作人', dataIndex: 'operator', key: 'operator', width: 120 },
  { title: '操作模块', dataIndex: 'module', key: 'module', width: 120 },
  { title: '操作类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '操作内容', dataIndex: 'content', key: 'content' },
  { title: 'IP地址', dataIndex: 'ip', key: 'ip', width: 140 },
];

const data = ref([
  { key: '1', time: '2024-03-15 09:12:33', operator: '张明', module: '系统', type: '登录', content: '用户登录系统', ip: '192.168.1.101' },
  { key: '2', time: '2024-03-15 09:25:18', operator: '张明', module: '团队管理', type: '新增', content: '新增团队：YN20240315-001 昆大丽6日游', ip: '192.168.1.101' },
  { key: '3', time: '2024-03-15 10:03:45', operator: '李婷', module: '订单管理', type: '修改', content: '修改订单 ORD20240312-005 人数由12人改为15人', ip: '192.168.1.102' },
  { key: '4', time: '2024-03-15 10:30:22', operator: '王强', module: '财务管理', type: '审核', content: '审核通过付款申请 PAY20240315-003 金额¥28,500', ip: '192.168.1.105' },
  { key: '5', time: '2024-03-15 11:15:07', operator: '赵丽', module: '导游管理', type: '修改', content: '修改导游排班：李导 3月18日-3月23日 昆大丽线路', ip: '192.168.1.103' },
  { key: '6', time: '2024-03-15 13:42:51', operator: '张明', module: '资源管理', type: '删除', content: '删除酒店资源：丽江古城假日酒店（已停业）', ip: '192.168.1.101' },
  { key: '7', time: '2024-03-15 14:08:33', operator: '陈伟', module: '客户管理', type: '新增', content: '新增客户：上海春秋国际旅行社', ip: '192.168.1.108' },
  { key: '8', time: '2024-03-15 14:55:19', operator: '李婷', module: '合同管理', type: '审核', content: '审核通过合同 CON20240315-002 云南6日游包团合同', ip: '192.168.1.102' },
  { key: '9', time: '2024-03-15 15:30:44', operator: '王强', module: '财务管理', type: '新增', content: '新增收款记录 REC20240315-001 收到预付款¥50,000', ip: '192.168.1.105' },
  { key: '10', time: '2024-03-15 16:20:11', operator: '张明', module: '系统', type: '修改', content: '修改系统参数：订单自动锁定天数由5天改为3天', ip: '192.168.1.101' },
]);

const moduleOptions = ref(['全部', '系统', '团队管理', '订单管理', '财务管理', '导游管理', '资源管理', '客户管理', '合同管理']);
const selectedModule = ref('全部');
</script>

<template>
  <Page title="系统操作日志" description="查看系统操作日志记录">
    <Card>
      <div class="mb-4 flex items-center gap-4">
        <DatePicker.RangePicker style="width: 260px" :placeholder="['开始日期', '结束日期']" />
        <Select v-model:value="selectedModule" style="width: 150px" placeholder="操作模块">
          <Select.Option v-for="item in moduleOptions" :key="item" :value="item">{{ item }}</Select.Option>
        </Select>
        <Button type="primary">查询</Button>
        <Button>重置</Button>
        <div class="flex-1"></div>
        <Button>导出日志</Button>
      </div>
      <Table :columns="columns" :data-source="data" :pagination="{ pageSize: 10, total: 156, showTotal: (total: number) => `共 ${total} 条记录` }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <Tag v-if="record.type === '登录'" color="blue">{{ record.type }}</Tag>
            <Tag v-else-if="record.type === '新增'" color="green">{{ record.type }}</Tag>
            <Tag v-else-if="record.type === '修改'" color="orange">{{ record.type }}</Tag>
            <Tag v-else-if="record.type === '删除'" color="red">{{ record.type }}</Tag>
            <Tag v-else-if="record.type === '审核'" color="purple">{{ record.type }}</Tag>
            <Tag v-else>{{ record.type }}</Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
