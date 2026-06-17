import type { RouteRecordRaw } from 'vue-router';

const PrototypePage = () => import('#/views/_business/PrototypePage.vue');
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
      { name: 'Schedule', path: '/sales/schedule', component: PrototypePage, props: { pageKey: 'sales-schedule' }, meta: { icon: 'lucide:calendar-plus', title: '团期管理' } },
      { name: 'Team', path: '/sales/team', component: PrototypePage, props: { pageKey: 'sales-team' }, meta: { icon: 'lucide:users-round', title: '团队管理' } },
      { name: 'Order', path: '/sales/order', component: PrototypePage, props: { pageKey: 'sales-order' }, meta: { icon: 'lucide:clipboard-list', title: '订单管理' } },
      { name: 'GroupBooking', path: '/sales/group-booking', component: PrototypePage, props: { pageKey: 'sales-group-booking' }, meta: { icon: 'lucide:calendar-clock', title: '散拼团队预订' } },
      { name: 'CombineOrder', path: '/sales/combine-order', component: PrototypePage, props: { pageKey: 'sales-combine-order' }, meta: { icon: 'lucide:merge', title: '拼团订单' } },
      { name: 'SharedCarCost', path: '/sales/shared-car-cost', component: PrototypePage, props: { pageKey: 'sales-shared-car-cost' }, meta: { icon: 'lucide:split', title: '共车成本分摊' } },
      { name: 'ExpenseChange', path: '/sales/expense-change', component: PrototypePage, props: { pageKey: 'sales-expense-change' }, meta: { icon: 'lucide:diff', title: '订单费用变更' } },
      { name: 'EContract', path: '/sales/e-contract', component: PrototypePage, props: { pageKey: 'sales-e-contract' }, meta: { icon: 'lucide:file-pen', title: '电子合同' } },
      { name: 'TicketBooking', path: '/sales/ticket-booking', component: PrototypePage, props: { pageKey: 'sales-ticket-booking' }, meta: { icon: 'lucide:ticket-check', title: '票务系统下单' } },
      { name: 'Tourist', path: '/sales/tourist', component: PrototypePage, props: { pageKey: 'sales-tourist' }, meta: { icon: 'lucide:user-check', title: '游客信息中心' } },
      { name: 'NameCheck', path: '/sales/name-check', component: PrototypePage, props: { pageKey: 'sales-name-check' }, meta: { icon: 'lucide:search-check', title: '名单查重' } },
      { name: 'AiService', path: '/sales/ai-service', component: PrototypePage, props: { pageKey: 'sales-ai-service' }, meta: { icon: 'lucide:bot', title: '企微AI客服' } },
      { name: 'SmartQuote', path: '/sales/smart-quote', component: PrototypePage, props: { pageKey: 'sales-smart-quote' }, meta: { icon: 'lucide:sparkles', title: '智能行程报价' } },
      { name: 'KnowledgeBase', path: '/sales/knowledge-base', component: PrototypePage, props: { pageKey: 'sales-knowledge-base' }, meta: { icon: 'lucide:library', title: 'AI知识库' } },
    ],
  },
];

export default routes;
