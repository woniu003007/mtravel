import type { RouteRecordRaw } from 'vue-router';

const MyGuideLeavePage = () => import('#/views/guide/my-leave/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:badge', order: 8, title: '导游端' },
    name: 'GuidePortal',
    path: '/guide',
    redirect: '/guide/my-leave',
    children: [
      { name: 'MyGuideLeave', path: '/guide/my-leave', component: MyGuideLeavePage, meta: { icon: 'lucide:calendar-clock', title: '我的请假' } },
    ],
  },
];

export default routes;
