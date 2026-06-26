import type { RouteRecordRaw } from 'vue-router';

const RiskApprovalPage = () => import('#/views/system/risk-approval/index.vue');
const SystemConfigPage = () => import('#/views/system/config/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:settings', order: 9, title: '系统设置' },
    name: 'System',
    path: '/system',
    redirect: '/system/config',
    children: [
      { name: 'SystemConfig', path: '/system/config', component: SystemConfigPage, meta: { icon: 'lucide:sliders-horizontal', title: '系统配置' } },
      { name: 'RiskApproval', path: '/system/risk-approval', component: RiskApprovalPage, meta: { icon: 'lucide:badge-check', title: '总经理审批' } },
    ],
  },
];

export default routes;
