import type { RouteRecordRaw } from 'vue-router';

const RoomStatusPage = () => import('#/views/dispatch/room-status/index.vue');
const VehicleQuotePage = () => import('#/views/dispatch/vehicle-quote/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:clipboard-check', order: 4, title: '计调操作' },
    name: 'Dispatch',
    path: '/dispatch',
    redirect: '/dispatch/room-status',
    children: [
      { name: 'RoomStatus', path: '/dispatch/room-status', component: RoomStatusPage, meta: { icon: 'lucide:bed', title: '自控房源与房态库存' } },
      { name: 'VehicleQuote', path: '/dispatch/vehicle-quote', component: VehicleQuotePage, meta: { icon: 'lucide:calculator', title: '用车报价测算' } },
    ],
  },
];

export default routes;
