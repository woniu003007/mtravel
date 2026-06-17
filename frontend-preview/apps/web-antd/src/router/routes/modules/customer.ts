import type { RouteRecordRaw } from 'vue-router';

const CustomerCategoryPage = () => import('#/views/customer/category/index.vue');
const CustomerContractPage = () => import('#/views/customer/contract/index.vue');
const CustomerCreditPage = () => import('#/views/customer/credit/index.vue');
const CustomerProductAuthPage = () => import('#/views/customer/product-auth/index.vue');
const CustomerUnitPage = () => import('#/views/customer/unit/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:contact', order: 1, title: '客户管理' },
    name: 'Customer',
    path: '/customer',
    redirect: '/customer/unit',
    children: [
      { name: 'CustomerUnit', path: '/customer/unit', component: CustomerUnitPage, meta: { icon: 'lucide:building', title: '客户单位' } },
      { name: 'CustomerCategory', path: '/customer/category', component: CustomerCategoryPage, meta: { icon: 'lucide:tags', title: '客户分类' } },
      { name: 'CustomerCredit', path: '/customer/credit', component: CustomerCreditPage, meta: { icon: 'lucide:shield-alert', title: '客户授信与实时应收' } },
      { name: 'CustomerContract', path: '/customer/contract', component: CustomerContractPage, meta: { icon: 'lucide:file-signature', title: '合同管理' } },
      { name: 'CustomerProductAuth', path: '/customer/product-auth', component: CustomerProductAuthPage, meta: { icon: 'lucide:key-round', title: '产品授权' } },
    ],
  },
];

export default routes;
