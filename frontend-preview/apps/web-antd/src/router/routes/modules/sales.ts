import type { RouteRecordRaw } from 'vue-router';

const ProductFormPage = () => import('#/views/sales/product/form.vue');
const ProductDesignerFormPage = () => import('#/views/sales/product/designer-form.vue');
const ProductDesignerListPage = () => import('#/views/sales/product/designer-index.vue');
const ProductDesignerPage = () => import('#/views/sales/product/designer.vue');
const ProductPage = () => import('#/views/sales/product/index.vue');
const ProductSchedulePage = () => import('#/views/sales/product/schedule.vue');
const ProductTeamArrangementPage = () => import('#/views/sales/product/team-arrangement.vue');
const SalesBookingFormPage = () => import('#/views/sales/booking/form.vue');
const SalesOrderPage = () => import('#/views/sales/order/index.vue');
const TeamArrangementPage = () => import('#/views/sales/team/arrangement.vue');
const TeamCreatePage = () => import('#/views/sales/team/create.vue');
const TeamGrossProfitPage = () => import('#/views/sales/team/gross-profit.vue');
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
      { name: 'ProductDesignerHome', path: '/sales/product/designer', component: ProductDesignerListPage, meta: { icon: 'lucide:map', title: '产品设计' } },
      { name: 'SalesTeam', path: '/sales/team', component: TeamPage, meta: { icon: 'lucide:users-round', title: '团队管理' } },
      { name: 'SalesOrder', path: '/sales/order', component: SalesOrderPage, meta: { icon: 'lucide:file-text', title: '订单管理' } },
      { name: 'ProductCreate', path: '/sales/product/create', component: ProductFormPage, meta: { hideInMenu: true, title: '新增产品' } },
      { name: 'ProductEdit', path: '/sales/product/edit/:id', component: ProductFormPage, meta: { hideInMenu: true, title: '修改产品' } },
      { name: 'ProductDesignerCreate', path: '/sales/product/designer/create', component: ProductDesignerFormPage, meta: { hideInMenu: true, title: '新建产品设计' } },
      { name: 'ProductDesignerEdit', path: '/sales/product/designer/edit/:id', component: ProductDesignerFormPage, meta: { hideInMenu: true, title: '修改产品设计' } },
      { name: 'ProductDesigner', path: '/sales/product/designer/:id', component: ProductDesignerPage, meta: { hideInMenu: true, title: '产品设计' } },
      { name: 'ProductSchedule', path: '/sales/product/schedule/:id', component: ProductSchedulePage, meta: { hideInMenu: true, title: '团期管理' } },
      { name: 'ProductTeamArrangement', path: '/sales/product/team-arrangement/:id', component: ProductTeamArrangementPage, meta: { hideInMenu: true, title: '产品团队安排' } },
      { name: 'SalesTeamCreate', path: '/sales/team/create/:type', component: TeamCreatePage, meta: { hideInMenu: true, title: '新增团队' } },
      { name: 'SalesTeamEdit', path: '/sales/team/edit/:id', component: TeamCreatePage, meta: { hideInMenu: true, title: '修改团队' } },
      { name: 'SalesTeamArrangement', path: '/sales/team/arrangement/:id', component: TeamArrangementPage, meta: { hideInMenu: true, title: '团队安排总览' } },
      { name: 'SalesTeamGrossProfit', path: '/sales/team/gross-profit/:id', component: TeamGrossProfitPage, meta: { hideInMenu: true, title: '团队毛利表' } },
      { name: 'SalesTeamOperation', path: '/sales/team/operation/:id', component: TeamOperationPage, meta: { hideInMenu: true, title: '团队操作' } },
      { name: 'SalesTeamBookingCreate', path: '/sales/team/booking/:teamId', component: SalesBookingFormPage, meta: { hideInMenu: true, title: '新增收客订单' } },
      { name: 'SalesTeamBookingEdit', path: '/sales/team/booking/:teamId/:orderId', component: SalesBookingFormPage, meta: { hideInMenu: true, title: '修改收客订单' } },
    ],
  },
];

export default routes;
