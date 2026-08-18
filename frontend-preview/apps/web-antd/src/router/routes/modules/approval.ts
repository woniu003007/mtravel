import type { RouteRecordRaw } from 'vue-router';

const CreditOverLimitApprovalPage = () =>
  import('#/views/approval/credit-over-limit/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:clipboard-check', order: 9, title: '审批管理' },
    name: 'Approval',
    path: '/approval',
    redirect: '/approval/credit-over-limit',
    children: [
      {
        component: CreditOverLimitApprovalPage,
        meta: { icon: 'lucide:badge-check', title: '授信超额审批' },
        name: 'CreditOverLimitApproval',
        path: '/approval/credit-over-limit',
      },
    ],
  },
];

export default routes;
