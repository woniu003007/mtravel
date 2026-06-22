import type { RouteRecordRaw } from 'vue-router';

const ProductFormPage = () => import('#/views/sales/product/form.vue');
const ProductPage = () => import('#/views/sales/product/index.vue');
const ProductTeamArrangementPage = () => import('#/views/sales/product/team-arrangement.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:shopping-cart', order: 3, title: '销售管理' },
    name: 'Sales',
    path: '/sales',
    redirect: '/sales/product',
    children: [
      { name: 'Product', path: '/sales/product', component: ProductPage, meta: { icon: 'lucide:package', title: '产品管理' } },
      { name: 'ProductCreate', path: '/sales/product/create', component: ProductFormPage, meta: { hideInMenu: true, title: '新增产品' } },
      { name: 'ProductEdit', path: '/sales/product/edit/:id', component: ProductFormPage, meta: { hideInMenu: true, title: '修改产品' } },
      { name: 'ProductTeamArrangement', path: '/sales/product/team-arrangement/:id', component: ProductTeamArrangementPage, meta: { hideInMenu: true, title: '产品团队安排' } },
    ],
  },
];

export default routes;
