import type { RouteRecordRaw } from 'vue-router';

const CustomerCreditPolicyPage = () =>
  import('#/views/customer/category/index.vue');
const QuoteConfigPage = () => import('#/views/configuration/quote/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:settings-2', order: 8, title: '配置管理' },
    name: 'Configuration',
    path: '/configuration',
    redirect: '/configuration/customer-credit-policy',
    children: [
      {
        component: CustomerCreditPolicyPage,
        meta: { icon: 'lucide:shield-check', title: '客户等级授信配置' },
        name: 'CustomerCreditPolicy',
        path: '/configuration/customer-credit-policy',
      },
      {
        component: QuoteConfigPage,
        meta: { icon: 'lucide:badge-dollar-sign', title: '报价配置' },
        name: 'QuoteConfig',
        path: '/configuration/quote',
      },
    ],
  },
];

export default routes;
