import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      hideInMenu: true,
      icon: 'lucide:layout-dashboard',
      order: -1,
      title: '业务工作台',
    },
    name: 'Dashboard',
    path: '/dashboard',
    redirect: '/workspace',
    children: [
      {
        name: 'Workspace',
        path: '/workspace',
        component: () => import('#/views/dashboard/workspace/index.vue'),
        meta: {
          affixTab: true,
          icon: 'carbon:workspace',
          title: '业务工作台',
        },
      },
    ],
  },
];

export default routes;
