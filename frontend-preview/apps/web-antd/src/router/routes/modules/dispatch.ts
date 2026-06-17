import type { RouteRecordRaw } from 'vue-router';

const PrototypePage = () => import('#/views/_business/PrototypePage.vue');
const RoomStatusPage = () => import('#/views/dispatch/room-status/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:clipboard-check', order: 4, title: '计调操作' },
    name: 'Dispatch',
    path: '/dispatch',
    redirect: '/dispatch/team-arrange',
    children: [
      { name: 'TeamArrange', path: '/dispatch/team-arrange', component: PrototypePage, props: { pageKey: 'dispatch-team-arrange' }, meta: { icon: 'lucide:layout-list', title: '团队安排总控台' } },
      { name: 'DispatchAudit', path: '/dispatch/team-audit', component: PrototypePage, props: { pageKey: 'dispatch-team-audit' }, meta: { icon: 'lucide:check-circle', title: '计调团队审核' } },
      { name: 'GuideSchedule', path: '/dispatch/guide-schedule', component: PrototypePage, props: { pageKey: 'dispatch-guide-schedule' }, meta: { icon: 'lucide:calendar-days', title: '导游排班汇总' } },
      { name: 'TransferInfo', path: '/dispatch/transfer-info', component: PrototypePage, props: { pageKey: 'dispatch-transfer-info' }, meta: { icon: 'lucide:bus', title: '接送信息' } },
      { name: 'RoomStatus', path: '/dispatch/room-status', component: RoomStatusPage, meta: { icon: 'lucide:bed', title: '自控房源与房态库存' } },
      { name: 'GuideExpense', path: '/dispatch/guide-expense', component: PrototypePage, props: { pageKey: 'dispatch-guide-expense' }, meta: { icon: 'lucide:receipt-text', title: '导游报账' } },
      { name: 'CarInquiry', path: '/dispatch/car-inquiry', component: PrototypePage, props: { pageKey: 'dispatch-car-inquiry' }, meta: { icon: 'lucide:route', title: '车调询价与派车' } },
    ],
  },
];

export default routes;
