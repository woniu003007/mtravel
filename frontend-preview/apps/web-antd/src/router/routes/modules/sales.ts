import type { RouteRecordRaw } from 'vue-router';

const ProductFormPage = () => import('#/views/sales/product/form.vue');
const ProductPage = () => import('#/views/sales/product/index.vue');
const ProductSchedulePage = () => import('#/views/sales/product/schedule.vue');
const ProductTeamArrangementPage = () => import('#/views/sales/product/team-arrangement.vue');
const SalesBookingFormPage = () => import('#/views/sales/booking/form.vue');
const SalesOrderPage = () => import('#/views/sales/order/index.vue');
const TeamPage = () => import('#/views/sales/team/index.vue');
const TeamOperationPage = () => import('#/views/sales/team/operation.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:shopping-cart', order: 3, title: '销售管理' },
    name: 'Sales',
    path: '/sales',
    redirect: '/sales/product',
    children: [
      { name: 'Product', path: '/sales/product', component: ProductPage, meta: { icon: 'lucide:package', title: '产品管理' } },
      { name: 'SalesTeam', path: '/sales/team', component: TeamPage, meta: { icon: 'lucide:users-round', title: '团队管理' } },
      { name: 'ProductCreate', path: '/sales/product/create', component: ProductFormPage, meta: { hideInMenu: true, title: '新增产品' } },
      { name: 'ProductEdit', path: '/sales/product/edit/:id', component: ProductFormPage, meta: { hideInMenu: true, title: '修改产品' } },
      { name: 'ProductSchedule', path: '/sales/product/schedule/:id', component: ProductSchedulePage, meta: { hideInMenu: true, title: '团期管理' } },
      { name: 'ProductTeamArrangement', path: '/sales/product/team-arrangement/:id', component: ProductTeamArrangementPage, meta: { hideInMenu: true, title: '产品团队安排' } },
      { name: 'SalesTeamOperation', path: '/sales/team/operation/:id', component: TeamOperationPage, meta: { hideInMenu: true, title: '团队操作' } },
      { name: 'SalesTeamBookingCreate', path: '/sales/team/booking/:teamId', component: SalesBookingFormPage, meta: { hideInMenu: true, title: '新增收客订单' } },
      { name: 'SalesTeamBookingEdit', path: '/sales/team/booking/:teamId/:orderId', component: SalesBookingFormPage, meta: { hideInMenu: true, title: '修改收客订单' } },
      { name: 'SalesOrder', path: '/sales/order', component: SalesOrderPage, meta: { hideInMenu: true, title: '订单管理' } },
    ],
  },
];

export default routes;
