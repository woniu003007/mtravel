import type { RouteRecordRaw } from 'vue-router';

const PurchaseContractPage = () => import('#/views/customer/contract/index.vue');
const PurchaseRelationPage = () => import('#/views/purchase/relation/index.vue');
const ResourcePage = () => import('#/views/purchase/resource/index.vue');
const SupplierPage = () => import('#/views/purchase/supplier/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:shopping-bag', order: 2, title: '采购管理' },
    name: 'Purchase',
    path: '/purchase',
    redirect: '/purchase/resource',
    children: [
      { name: 'Resource', path: '/purchase/resource', component: ResourcePage, meta: { icon: 'lucide:database', title: '资源总览' } },
      { name: 'Supplier', path: '/purchase/supplier', component: SupplierPage, meta: { icon: 'lucide:truck', title: '供应商管理' } },
      { name: 'PurchaseRelation', path: '/purchase/relation', component: PurchaseRelationPage, meta: { icon: 'lucide:link', title: '采购关系管理' } },
      { name: 'PurchaseContract', path: '/purchase/contract', component: PurchaseContractPage, meta: { icon: 'lucide:file-check', title: '合同管理' } },
    ],
  },
];

export default routes;
