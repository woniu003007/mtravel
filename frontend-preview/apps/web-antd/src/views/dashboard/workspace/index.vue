<script lang="ts" setup>
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Card,
  Descriptions,
  Progress,
  Space,
  Statistic,
  Steps,
  Table,
  Tag,
  Timeline,
} from 'ant-design-vue';

import {
  deliveryScope,
  phaseColors,
  prototypeModules,
  workbenchAlerts,
} from '#/views/_business/prototype-data';

const router = useRouter();

const storySteps = [
  {
    description: '保存正式主体、合同状态、授信额度，先判断客户能不能继续下单。',
    path: '/customer/credit',
    role: '销售 / 财务',
    title: '客户发来团队需求',
  },
  {
    description: '维护产品资料、行程、产品说明和团队安排模板，先把可售产品基础打牢。',
    path: '/sales/product',
    role: '销售',
    title: '销售维护产品资料',
  },
  {
    description: '维护外部资源、供应商、采购关系和合同，给产品和计调提供可选资源。',
    path: '/purchase/resource',
    role: '采购',
    title: '采购维护资源关系',
  },
  {
    description: '维护公司、部门、角色、员工、导游和费用项目，支撑权限和业务基础资料。',
    path: '/enterprise/employee',
    role: '管理员',
    title: '企业资料维护',
  },
];

const roleCards = [
  {
    color: '#2563eb',
    icon: 'lucide:briefcase-business',
    pages: ['客户单位', '客户分类', '客户授信', '合同管理'],
    pain: '客户主体、合同和额度如果不清楚，后面下单风险会放大。',
    path: '/customer/unit',
    role: '销售',
    value: '先把客户、合同和额度资料维护清楚。',
  },
  {
    color: '#16a34a',
    icon: 'lucide:route',
    pages: ['产品管理', '行程内容', '团队安排', '路书地图'],
    pain: '产品资料和行程不标准，后续团队生成和报价都容易重复录。',
    path: '/sales/product',
    role: '销售产品',
    value: '先把产品、行程和团队安排模板做成可复用基础。',
  },
  {
    color: '#7c3aed',
    icon: 'lucide:building-2',
    pages: ['部门', '角色', '员工', '导游'],
    pain: '组织和人员资料不完整，权限、负责人和导游归属都不好管。',
    path: '/enterprise/employee',
    role: '管理员',
    value: '先把组织、权限、员工和导游资料建完整。',
  },
  {
    color: '#0891b2',
    icon: 'lucide:shopping-bag',
    pages: ['资源总览', '供应商', '采购关系', '合同管理'],
    pain: '外部酒店、景区、餐厅、车队等资源要先有基础资料和价格关系。',
    path: '/purchase/resource',
    role: '采购',
    value: '维护外部资源和采购关系，给后续安排调用。',
  },
];

const demoColumns = [
  { dataIndex: 'scene', key: 'scene', title: '演示场景', width: 150 },
  { dataIndex: 'customerWords', key: 'customerWords', title: '讲给客户听的话' },
  { dataIndex: 'confirm', key: 'confirm', title: '请客户确认', width: 260 },
];

const demoRows = [
  {
    confirm: '授信超限是拦截还是审批？',
    customerWords: '客户同时发多个团，系统实时看欠款和可下单额度，避免最后才发现风险。',
    scene: '客户授信',
  },
  {
    confirm: '确认件、合同、附件哪些必填？',
    customerWords: '先把产品资料、行程内容和团队安排模板维护好，后面建团不用重复录。',
    scene: '产品管理',
  },
  {
    confirm: '资源、供应商、采购关系字段是否够用？',
    customerWords: '采购先维护外部资源和供应商价格关系，计调后面才能选择可用资源。',
    scene: '采购资源',
  },
  {
    confirm: '部门、角色、员工、导游归属是否清楚？',
    customerWords: '企业资料先建好，后面权限、负责人、导游绩效归属才有基础。',
    scene: '企业资料',
  },
];

const scopeColumns = [
  { title: '模块', dataIndex: 'module', key: 'module', width: 120 },
  { title: '一期主线', dataIndex: 'p0', key: 'p0', width: 100 },
  { title: '增强/候选', dataIndex: 'p1', key: 'p1', width: 110 },
  { title: '智能规划', dataIndex: 'p2', key: 'p2', width: 110 },
  { title: '确认重点', dataIndex: 'focus', key: 'focus' },
];

function navTo(path: string) {
  router.push(path).catch(() => {});
}
</script>

<template>
  <Page
    description="用一个真实团队案例，让客户看懂系统每天怎么帮销售、计调、财务和老板工作。"
    title="旅游地接业务全流程演示"
  >
    <div class="workspace">
      <section class="story-hero">
        <div>
          <div class="hero-label">
            <IconifyIcon icon="lucide:map-pinned" />
            <span>客户演示版</span>
            <Tag color="blue">按业务故事讲</Tag>
          </div>
          <h1>用一个团讲清楚：从客户发需求到老板看利润</h1>
          <p>
            现场不要从菜单开始讲。先拿“杭州远行国旅 · 西湖宋城二日游”这个团队案例，
            讲清楚客户、销售、计调、财务和老板分别怎么用系统。
          </p>
          <Space wrap>
            <Button type="primary" @click="navTo('/sales/product')">
              从产品管理开始演示
            </Button>
            <Button @click="navTo('/purchase/resource')">
              看资源采购
            </Button>
          </Space>
        </div>
        <Card class="case-card" title="本次演示团队">
          <Descriptions :column="1" size="small">
            <Descriptions.Item label="客户">
              杭州远行国旅
            </Descriptions.Item>
            <Descriptions.Item label="团队">
              HZ20260518-003 西湖宋城二日游
            </Descriptions.Item>
            <Descriptions.Item label="订单金额">
              ¥86,400，应收已生成
            </Descriptions.Item>
            <Descriptions.Item label="当前状态">
              <Tag color="orange">计调安排中</Tag>
              <Tag color="red">授信预警</Tag>
            </Descriptions.Item>
          </Descriptions>
        </Card>
      </section>

      <div class="risk-grid">
        <button
          v-for="item in workbenchAlerts"
          :key="item.label"
          class="risk-tile"
          type="button"
          @click="navTo(item.path)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.count }}</strong>
          <Tag :color="item.status === 'P0' ? 'red' : 'blue'">
            {{ item.status }}
          </Tag>
        </button>
      </div>

      <section class="content-grid">
        <Card title="现场演示主线：一个团队怎么走完">
          <Steps
            :current="2"
            :items="storySteps.map((item) => ({ title: item.title, description: item.role }))"
          />
          <Timeline class="story-timeline">
            <Timeline.Item v-for="item in storySteps" :key="item.title">
              <button class="timeline-link" type="button" @click="navTo(item.path)">
                <strong>{{ item.title }}</strong>
                <span>{{ item.description }}</span>
              </button>
            </Timeline.Item>
          </Timeline>
          <Alert
            show-icon
            type="info"
            message="讲解口径"
            description="每个页面只讲三句话：现在业务痛点是什么、系统帮谁做什么、这个规则请客户怎么确认。"
          />
        </Card>

        <Card title="客户真正关心的结果">
          <div class="result-grid">
            <Card>
              <Statistic title="订单确认后立即生成应收" value="86,400" prefix="¥" />
            </Card>
            <Card>
              <Statistic title="计调资源安排进度" value="82" suffix="%" />
            </Card>
            <Card>
              <Statistic title="团队预计毛利" value="5.4" suffix="万" />
            </Card>
            <Card>
              <Statistic title="待客户确认规则" value="6" suffix="项" />
            </Card>
          </div>
          <Alert
            class="mt-16"
            show-icon
            type="warning"
            message="客户确认重点"
            description="一期不是把所有菜单都做深，而是先把订单、排团、成本、账款和统计这条主线跑通。"
          />
        </Card>
      </section>

      <Card title="按岗位讲功能：客户更容易代入">
        <div class="role-grid">
          <button
            v-for="role in roleCards"
            :key="role.role"
            class="role-card"
            type="button"
            @click="navTo(role.path)"
          >
            <IconifyIcon :icon="role.icon" :style="{ color: role.color }" />
            <h3>{{ role.role }}</h3>
            <p class="pain">{{ role.pain }}</p>
            <p class="value">{{ role.value }}</p>
            <div>
              <Tag v-for="page in role.pages" :key="page">{{ page }}</Tag>
            </div>
          </button>
        </div>
      </Card>

      <section class="content-grid">
        <Card title="页面怎么讲，不让客户迷路">
          <Table
            :columns="demoColumns"
            :data-source="demoRows"
            :pagination="false"
            row-key="scene"
            size="small"
          />
        </Card>

        <Card title="一期/后续边界">
          <Table
            :columns="scopeColumns"
            :data-source="deliveryScope"
            :pagination="false"
            row-key="module"
            size="small"
          />
        </Card>
      </section>

      <Card title="全部功能入口放最后：客户需要时再看">
        <div class="module-grid">
          <button
            v-for="module in prototypeModules.filter((item) => item.key !== 'dashboard')"
            :key="module.key"
            class="module-card"
            type="button"
            @click="navTo(module.path)"
          >
            <IconifyIcon :icon="module.icon" />
            <span>{{ module.title }}</span>
            <Progress
              :percent="module.key === 'sales' || module.key === 'finance' ? 92 : 76"
              :show-info="false"
              :stroke-color="module.key === 'sales' || module.key === 'finance' ? '#dc2626' : '#2563eb'"
            />
            <Tag :color="phaseColors.P0">含一期确认项</Tag>
          </button>
        </div>
      </Card>
    </div>
  </Page>
</template>

<style scoped>
.workspace {
  padding: 20px;
}

.story-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(340px, 0.7fr);
  gap: 20px;
  padding: 26px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 54%, #ecfdf5 100%);
  border: 1px solid #dbe5ef;
  border-radius: 8px;
}

.hero-label {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
}

.story-hero h1 {
  max-width: 900px;
  margin: 12px 0;
  color: #0f172a;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.28;
}

.story-hero p {
  max-width: 860px;
  margin-bottom: 18px;
  color: #475569;
  line-height: 1.8;
}

.case-card {
  align-self: stretch;
}

.risk-grid,
.role-grid,
.module-grid,
.result-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.risk-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
  margin-bottom: 20px;
}

.risk-tile,
.role-card,
.module-card {
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dbe5ef;
  border-radius: 8px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.risk-tile {
  min-height: 104px;
  padding: 14px;
}

.risk-tile:hover,
.role-card:hover,
.module-card:hover,
.timeline-link:hover {
  border-color: #2563eb;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.risk-tile span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.risk-tile strong {
  display: block;
  margin: 8px 0;
  color: #111827;
  font-size: 28px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.85fr);
  gap: 20px;
  margin-bottom: 20px;
}

.story-timeline {
  margin-top: 24px;
}

.timeline-link {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 10px 12px;
  text-align: left;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.timeline-link strong {
  color: #111827;
}

.timeline-link span {
  color: #64748b;
  line-height: 1.6;
}

.mt-16 {
  margin-top: 16px;
}

.role-grid {
  margin-bottom: 20px;
}

.role-card {
  display: grid;
  gap: 10px;
  min-height: 224px;
  padding: 16px;
}

.role-card svg,
.module-card svg {
  font-size: 22px;
}

.role-card h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.role-card p {
  margin: 0;
  line-height: 1.65;
}

.role-card .pain {
  color: #64748b;
}

.role-card .value {
  color: #0f172a;
  font-weight: 600;
}

.module-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.module-card {
  display: grid;
  gap: 10px;
  min-height: 128px;
  padding: 16px;
}

.module-card svg {
  color: #2563eb;
}

.module-card span {
  color: #111827;
  font-size: 16px;
  font-weight: 600;
}

@media (max-width: 1180px) {
  .story-hero,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .risk-grid,
  .role-grid,
  .module-grid,
  .result-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .workspace {
    padding: 12px;
  }

  .story-hero,
  .risk-grid,
  .role-grid,
  .module-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
