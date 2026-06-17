<script lang="ts" setup>
import type { PrototypeRecord } from './prototype-data';

import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  Progress,
  Select,
  Space,
  Steps,
  Table,
  Tag,
  Timeline,
} from 'ant-design-vue';

import {
  getPrototypePage,
  phaseColors,
  phaseLabels,
  statusColors,
} from './prototype-data';

const props = defineProps<{
  pageKey: string;
}>();

const router = useRouter();
const drawerOpen = ref(false);
const currentRecord = ref<PrototypeRecord>();

const page = computed(() => getPrototypePage(props.pageKey));
const description = computed(
  () =>
    `${phaseLabels[page.value.phase]} · ${page.value.buildMode} · 来源菜单：${page.value.oldMenu}`,
);

function openRecord(record: Record<string, any>) {
  currentRecord.value = record as PrototypeRecord;
  drawerOpen.value = true;
}

function goNext() {
  const firstPath = {
    customer: '/sales/order',
    dispatch: '/finance/team-audit',
    enterprise: '/system/params',
    finance: '/statistics/reception',
    purchase: '/dispatch/team-arrange',
    sales: '/dispatch/team-arrange',
    statistics: '/finance/receivable',
    system: '/workspace',
  }[page.value.module];

  if (firstPath) {
    router.push(firstPath).catch(() => {});
  }
}

function progressColor(progress = 0) {
  if (progress >= 85) return '#16a34a';
  if (progress >= 60) return '#2563eb';
  if (progress >= 40) return '#d97706';
  return '#dc2626';
}
</script>

<template>
  <Page :description="description" :title="page.title">
    <div class="prototype-page">
      <section class="prototype-hero">
        <div class="hero-copy">
          <div class="hero-eyebrow">
            <IconifyIcon :icon="page.icon" />
            <span>{{ page.module }}</span>
            <Tag :color="phaseColors[page.phase]">{{ page.phase }} {{ phaseLabels[page.phase] }}</Tag>
          </div>
          <h1>{{ page.objective }}</h1>
          <p>{{ page.problem }}</p>
          <Space wrap>
            <Button type="primary" @click="goNext">查看下游链路</Button>
            <Button>导出本页确认点</Button>
            <Button>记录甲方意见</Button>
          </Space>
        </div>
        <div class="hero-panel">
          <div
            v-for="metric in page.metrics"
            :key="metric.label"
            class="metric-card"
          >
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <em>{{ metric.trend }}</em>
          </div>
        </div>
      </section>

      <div class="prototype-grid">
        <Card class="main-card" title="业务查询与操作">
          <Form class="filter-form" layout="vertical">
            <Form.Item
              v-for="filter in page.filters"
              :key="filter.label"
              :label="filter.label"
            >
              <Input
                v-if="filter.type === 'input'"
                :placeholder="filter.placeholder"
              />
              <Select
                v-else
                :options="filter.options?.map((item) => ({ label: item, value: item }))"
                :placeholder="filter.placeholder"
              />
            </Form.Item>
            <Form.Item label="操作">
              <Space wrap>
                <Button
                  v-for="action in page.actions"
                  :key="action"
                  :type="action.includes('新建') || action.includes('提交') ? 'primary' : 'default'"
                >
                  {{ action }}
                </Button>
              </Space>
            </Form.Item>
          </Form>

          <Table
            :columns="page.columns"
            :data-source="page.rows"
            :pagination="{ pageSize: 5 }"
            row-key="id"
            :scroll="{ x: 1120 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'title'">
                <div class="table-title">{{ record.title }}</div>
                <div class="table-sub">{{ record.risk }}</div>
              </template>
              <template v-else-if="column.key === 'progress'">
                <Progress
                  :percent="record.progress"
                  :stroke-color="progressColor(record.progress)"
                  size="small"
                />
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="statusColors[record.status] || 'default'">
                  {{ record.status }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button size="small" type="link" @click="openRecord(record)">
                    详情
                  </Button>
                  <Button size="small" type="link">审批</Button>
                </Space>
              </template>
            </template>
          </Table>
        </Card>

        <aside class="side-stack">
          <Card title="流程状态">
            <Steps
              direction="vertical"
              size="small"
              :current="page.phase === 'P0' ? 2 : 1"
              :items="page.steps.map((title) => ({ title }))"
            />
          </Card>

          <Card title="甲方确认点">
            <Timeline
              :items="page.confirmPoints.map((point) => ({ children: point, color: page.phase === 'P0' ? 'red' : 'blue' }))"
            />
          </Card>

          <Alert
            show-icon
            type="info"
            message="原型说明"
            :description="page.delivery"
          />
        </aside>
      </div>
    </div>

    <Drawer
      v-model:open="drawerOpen"
      width="560"
      :title="currentRecord?.title || page.title"
    >
      <Descriptions
        v-if="currentRecord"
        bordered
        :column="1"
        size="small"
      >
        <Descriptions.Item label="业务编号">{{ currentRecord.id }}</Descriptions.Item>
        <Descriptions.Item label="团队/对象">{{ currentRecord.team }}</Descriptions.Item>
        <Descriptions.Item label="客户/主体">{{ currentRecord.customer }}</Descriptions.Item>
        <Descriptions.Item label="金额/指标">{{ currentRecord.amount }}</Descriptions.Item>
        <Descriptions.Item label="负责人">{{ currentRecord.owner }}</Descriptions.Item>
        <Descriptions.Item label="当前阶段">{{ currentRecord.stage }}</Descriptions.Item>
        <Descriptions.Item label="风险提示">{{ currentRecord.risk }}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag :color="statusColors[currentRecord.status] || 'default'">
            {{ currentRecord.status }}
          </Tag>
        </Descriptions.Item>
      </Descriptions>
      <Empty v-else description="暂无详情" />

      <Alert
        class="mt-4"
        show-icon
        type="warning"
        message="演示动作"
        description="此处用于商务原型演示：展示详情、审批、退回、生成账款等关键动作，不接真实后端。"
      />
    </Drawer>
  </Page>
</template>

<style scoped>
.prototype-page {
  padding: 20px;
}

.prototype-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(360px, 0.85fr);
  gap: 20px;
  margin-bottom: 20px;
  padding: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fafc 0%, #eef6ff 42%, #f2f7f1 100%);
  border: 1px solid #dbe5ef;
  border-radius: 8px;
}

.hero-copy h1 {
  max-width: 900px;
  margin: 12px 0 10px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.35;
}

.hero-copy p {
  max-width: 860px;
  margin-bottom: 18px;
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.hero-eyebrow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
}

.hero-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 92px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 8px;
}

.metric-card span,
.metric-card em {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.metric-card strong {
  display: block;
  margin: 8px 0;
  color: #111827;
  font-size: 20px;
}

.prototype-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.main-card,
.side-stack :deep(.ant-card) {
  border-radius: 8px;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) minmax(260px, 1.3fr);
  gap: 12px;
  align-items: end;
  margin-bottom: 6px;
}

.filter-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.side-stack {
  display: grid;
  gap: 16px;
}

.table-title {
  color: #111827;
  font-weight: 600;
  line-height: 1.5;
}

.table-sub {
  max-width: 420px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1180px) {
  .prototype-hero,
  .prototype-grid {
    grid-template-columns: 1fr;
  }

  .filter-form {
    grid-template-columns: repeat(2, minmax(150px, 1fr));
  }
}

@media (max-width: 720px) {
  .prototype-page {
    padding: 12px;
  }

  .prototype-hero {
    padding: 18px;
  }

  .hero-panel,
  .filter-form {
    grid-template-columns: 1fr;
  }
}
</style>
