import type { RouteRecordRaw } from 'vue-router';

const GuideAdvancePage = () => import('#/views/finance/guide-advance/index.vue');
const TeamAuditPage = () => import('#/views/finance/team-audit/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { hideInMenu: true, icon: 'lucide:wallet-cards', order: 8, title: '财务功能' },
    name: 'Finance',
    path: '/finance',
    redirect: '/finance/guide-advance',
    children: [
      {
        name: 'FinanceGuideAdvance',
        path: '/finance/guide-advance',
        component: GuideAdvancePage,
        meta: { hideInMenu: true, title: '导游备用金' },
      },
      {
        name: 'FinanceTeamAudit',
        path: '/finance/team-audit',
        component: TeamAuditPage,
        meta: { hideInMenu: true, title: '财务团队审核' },
      },
    ],
  },
];

export default routes;
