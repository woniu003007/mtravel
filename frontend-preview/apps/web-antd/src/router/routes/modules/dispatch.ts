import type { RouteRecordRaw } from 'vue-router';

const GuideLeavePage = () => import('#/views/dispatch/guide-leave/index.vue');
const GuideSchedulePage = () => import('#/views/dispatch/guide-schedule/index.vue');
const VehicleQuotePage = () => import('#/views/dispatch/vehicle-quote/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:clipboard-check', order: 4, title: '计调操作' },
    name: 'Dispatch',
    path: '/dispatch',
    redirect: '/dispatch/vehicle-quote',
    children: [
      { name: 'VehicleQuote', path: '/dispatch/vehicle-quote', component: VehicleQuotePage, meta: { icon: 'lucide:calculator', title: '用车报价测算' } },
      { name: 'GuideSchedule', path: '/dispatch/guide-schedule', component: GuideSchedulePage, meta: { icon: 'lucide:calendar-days', title: '导游排班汇总' } },
      { name: 'GuideLeave', path: '/dispatch/guide-leave', component: GuideLeavePage, meta: { icon: 'lucide:calendar-x', title: '导游请假管理' } },
    ],
  },
];

export default routes;
