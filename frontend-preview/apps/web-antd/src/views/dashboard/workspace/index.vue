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
    description: '销售确认订单、人数、价格和确认件，系统同步生成应收和授信占用。',
    path: '/sales/order',
    role: '销售',
    title: '销售报价并确认订单',
  },
  {
    description: '计调集中安排导游、车辆、酒店、景区、餐饮和外委地接，异常直接暴露。',
    path: '/dispatch/team-arrange',
    role: '计调',
    title: '计调安排团队资源',
  },
  {
    description: '财务审核收入、成本、毛利、备用金和凭证，审核通过后进入收付款。',
    path: '/finance/team-audit',
    role: '财务',
    title: '财务审核成本账款',
  },
  {
    description: '老板查看团队利润、收客人数、欠款客户和异常团队，统一经营口径。',
    path: '/statistics/reception',
    role: '老板',
    title: '老板查看经营结果',
  },
];

const roleCards = [
  {
    color: '#2563eb',
    icon: 'lucide:briefcase-business',
    pages: ['客户授信', '订单管理', '费用变更', '电子合同'],
    pain: '客户需求多、确认件散、费用变更容易漏同步。',
    path: '/sales/order',
    role: '销售',
    value: '录单后自动关联客户、团队、应收和授信。',
  },
  {
    color: '#16a34a',
    icon: 'lucide:route',
    pages: ['团队安排总控', '导游排班', '房态库存', '车调询价'],
    pain: '导游、车、房、票、餐分散安排，冲突靠人工发现。',
    path: '/dispatch/team-arrange',
    role: '计调',
    value: '一个团缺什么、冲突在哪、成本多少，一页看清。',
  },
  {
    color: '#dc2626',
    icon: 'lucide:wallet-cards',
    pages: ['财务审核', '实时应收', '应付管理', '备用金闭环'],
    pain: '团队结束后才算账，应收、应付和凭证来源不清。',
    path: '/finance/team-audit',
    role: '财务',
    value: '订单确认即有应收，资源安排即有成本，报账可追溯。',
  },
  {
    color: '#7c3aed',
    icon: 'lucide:chart-no-axes-combined',
    pages: ['业务工作台', '利润统计', '账款统计', '收客统计'],
    pain: '老板要靠问人和看表格，不能实时看到风险和利润。',
    path: '/statistics/reception',
    role: '老板',
    value: '看订单、排团、账款、利润和异常的统一结果。',
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
    customerWords: '销售确认订单后，应收、人数、团队和授信都会同步，不再靠人工补账。',
    scene: '订单确认',
  },
  {
    confirm: '哪些资源没排完不能提交审核？',
    customerWords: '计调在一个页面看导游、车、房、票、餐、地接，哪里缺、哪里冲突一眼看到。',
    scene: '团队安排',
  },
  {
    confirm: '成本超预算谁审批？备用金怎么核销？',
    customerWords: '财务提前看到团队收入、成本、毛利和凭证，问题及时退回。',
    scene: '财务审核',
  },
  {
    confirm: '有效人数、利润、账龄口径怎么定？',
    customerWords: '老板看经营结果，不用再从多个表里拼数据。',
    scene: '经营统计',
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
            <Button type="primary" @click="navTo('/sales/order')">
              从销售下单开始演示
            </Button>
            <Button @click="navTo('/dispatch/team-arrange')">
              看计调怎么排团
            </Button>
            <Button @click="navTo('/finance/receivable')">
              看财务怎么管应收
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
