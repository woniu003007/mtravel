<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { SalesTeamApi } from '#/api/sales/team';

import { Page } from '@vben/common-ui';

import { Button, Card, Form, Input, Select, Table, Tag, message } from 'ant-design-vue';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getSalesTeamPage } from '#/api/sales/team';
import BusinessSearchForm from '#/components/business/BusinessSearchForm.vue';

import ShoppingReconciliationModal from '../../sales/components/ShoppingReconciliationModal.vue';

type TeamListItem = SalesTeamApi.ListItem;

const router = useRouter();
const loading = ref(false);
const rows = ref<TeamListItem[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const shoppingReconciliationOpen = ref(false);
const shoppingReconciliationTeam = ref<TeamListItem>();

const query = reactive({
  keyword: '',
  teamStatus: 'all' as SalesTeamApi.DateTeamStatus,
});

const columns: TableColumnsType<TeamListItem> = [
  { dataIndex: 'teamNo', key: 'teamNo', title: '团号', width: 170 },
  { dataIndex: 'productName', key: 'productName', title: '团队名称', width: 320 },
  { align: 'center', dataIndex: 'teamType', key: 'teamType', title: '类型', width: 84 },
  { align: 'center', dataIndex: 'departureDate', key: 'departureDate', title: '出团日期', width: 110 },
  { align: 'center', key: 'seats', title: '预控/实收/余位', width: 140 },
  { dataIndex: 'operatorEmployeeName', key: 'operatorEmployeeName', title: '计调', width: 100 },
  { dataIndex: 'guideSummary', key: 'guideSummary', title: '导游', width: 140 },
  { align: 'center', dataIndex: 'progress', key: 'progress', title: '团队进度', width: 100 },
  { align: 'center', key: 'action', title: '操作', width: 210 },
];

const pagination = () => ({
  current: page.value,
  pageSize: pageSize.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共${value}条记录`,
  total: total.value,
});

function teamTypeLabel(value?: string) {
  if (value === 'sanpin') return '散拼';
  if (value === 'zhengtuan') return '整团';
  if (value === 'santuan') return '散团';
  if (value === 'single') return '单项';
  return '--';
}

function progressLabel(value?: string) {
  if (value === 'cancelled') return '已取消';
  if (value === 'closed') return '已结算';
  if (value === 'departed') return '已发团';
  if (value === 'done') return '已完成';
  if (value === 'not_departed') return '未发团';
  if (value === 'receiving') return '收客中';
  if (value === 'stopped') return '已停收';
  return '--';
}

function progressColor(value?: string) {
  if (['done', 'closed'].includes(value || '')) return 'green';
  if (value === 'departed') return 'blue';
  if (value === 'cancelled' || value === 'stopped') return 'red';
  return 'orange';
}

function teamRow(record: Record<string, any>) {
  return record as TeamListItem;
}

async function loadPage() {
  loading.value = true;
  try {
    const result = await getSalesTeamPage({
      keyword: query.keyword?.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value,
      teamStatus: query.teamStatus === 'all' ? undefined : query.teamStatus,
    });
    rows.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = '';
  query.teamStatus = 'all';
  page.value = 1;
  void loadPage();
}

function search() {
  page.value = 1;
  void loadPage();
}

function handleTableChange(paginationInfo: { current?: number; pageSize?: number }) {
  page.value = paginationInfo.current || 1;
  pageSize.value = paginationInfo.pageSize || 20;
  void loadPage();
}

function openShoppingReconciliationModal(record: TeamListItem) {
  shoppingReconciliationTeam.value = record;
  shoppingReconciliationOpen.value = true;
}

function openArrangement(record: TeamListItem) {
  router.push(`/sales/team/arrangement/${record.id}`);
}

function openOperation(record: TeamListItem) {
  router.push(`/sales/team/operation/${record.id}`);
}

function afterShoppingRecalculated() {
  message.success('购物核对已更新，可继续进行团队审核');
}

onMounted(loadPage);
</script>

<template>
  <Page title="财务团队审核" description="按单团核对团队应收、成本、报账、购物补佣和利润口径">
    <Card>
      <BusinessSearchForm
        :model="query"
        :search-loading="loading"
        :show-create="false"
        @reset="resetQuery"
        @search="search"
      >
        <Form.Item label="关键词">
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="团号/团队名称"
            @press-enter="search"
          />
        </Form.Item>
        <Form.Item label="团队状态">
          <Select
            v-model:value="query.teamStatus"
            :options="[
              { label: '全部', value: 'all' },
              { label: '未发团', value: 'not_departed' },
              { label: '已发团', value: 'departed' },
              { label: '已结算', value: 'closed' },
              { label: '已完成', value: 'done' },
            ]"
          />
        </Form.Item>
      </BusinessSearchForm>

      <Table
        class="team-audit-table"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination()"
        row-key="id"
        :scroll="{ x: 1260 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'teamNo'">
            <Button type="link" size="small" @click="openOperation(teamRow(record))">
              {{ record.teamNo }}
            </Button>
          </template>
          <template v-else-if="column.key === 'teamType'">
            {{ teamTypeLabel(record.teamType) }}
          </template>
          <template v-else-if="column.key === 'seats'">
            <span class="team-audit-seats">
              {{ record.totalSeats ?? 0 }} / {{ record.usedSeats ?? 0 }} / {{ record.remainingSeats ?? 0 }}
            </span>
          </template>
          <template v-else-if="column.key === 'progress'">
            <Tag :color="progressColor(record.progress)">
              {{ progressLabel(record.progress) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Button type="link" size="small" @click="openShoppingReconciliationModal(teamRow(record))">
              购物核对/补佣
            </Button>
            <Button type="link" size="small" @click="openArrangement(teamRow(record))">
              团队安排
            </Button>
            <Button type="link" size="small" @click="openOperation(teamRow(record))">
              详情
            </Button>
          </template>
        </template>
      </Table>
    </Card>

    <ShoppingReconciliationModal
      v-model:open="shoppingReconciliationOpen"
      :team-departure-date="shoppingReconciliationTeam?.departureDate"
      :team-id="shoppingReconciliationTeam?.id"
      @recalculated="afterShoppingRecalculated"
    />
  </Page>
</template>

<style scoped>
.team-audit-table :deep(.ant-table-cell) {
  vertical-align: middle;
}

.team-audit-seats {
  font-weight: 800;
  color: #0f766e;
}
</style>
