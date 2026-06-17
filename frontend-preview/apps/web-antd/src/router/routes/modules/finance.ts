import type { RouteRecordRaw } from 'vue-router';

const PrototypePage = () => import('#/views/_business/PrototypePage.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:wallet', order: 5, title: '财务管理' },
    name: 'Finance',
    path: '/finance',
    redirect: '/finance/team-audit',
    children: [
      { name: 'FinanceAudit', path: '/finance/team-audit', component: PrototypePage, props: { pageKey: 'finance-team-audit' }, meta: { icon: 'lucide:badge-check', title: '财务团队审核' } },
      { name: 'Prepay', path: '/finance/prepay', component: PrototypePage, props: { pageKey: 'finance-prepay' }, meta: { icon: 'lucide:banknote', title: '团队预付款' } },
      { name: 'BankCash', path: '/finance/bank-cash', component: PrototypePage, props: { pageKey: 'finance-bank-cash' }, meta: { icon: 'lucide:landmark', title: '银行现金账' } },
      { name: 'Receivable', path: '/finance/receivable', component: PrototypePage, props: { pageKey: 'finance-receivable' }, meta: { icon: 'lucide:arrow-down-to-line', title: '实时应收管理' } },
      { name: 'ReceivableDetail', path: '/finance/receivable-detail', component: PrototypePage, props: { pageKey: 'finance-receivable-detail' }, meta: { icon: 'lucide:list', title: '应收账款明细' } },
      { name: 'Payable', path: '/finance/payable', component: PrototypePage, props: { pageKey: 'finance-payable' }, meta: { icon: 'lucide:arrow-up-from-line', title: '应付管理' } },
      { name: 'PayableDetail', path: '/finance/payable-detail', component: PrototypePage, props: { pageKey: 'finance-payable-detail' }, meta: { icon: 'lucide:list-ordered', title: '应付账款明细' } },
      { name: 'CostPreview', path: '/finance/cost-preview', component: PrototypePage, props: { pageKey: 'finance-cost-preview' }, meta: { icon: 'lucide:eye', title: '预算成本与实际成本' } },
      { name: 'GuideAdvance', path: '/finance/guide-advance', component: PrototypePage, props: { pageKey: 'finance-guide-advance' }, meta: { icon: 'lucide:hand-coins', title: '导游备用金闭环' } },
      { name: 'GuideSettlement', path: '/finance/guide-settlement', component: PrototypePage, props: { pageKey: 'finance-guide-settlement' }, meta: { icon: 'lucide:calculator', title: '导游结算' } },
      { name: 'ShopRebate', path: '/finance/shop-rebate', component: PrototypePage, props: { pageKey: 'finance-shop-rebate' }, meta: { icon: 'lucide:percent', title: '购物返佣' } },
      { name: 'IncomeRecord', path: '/finance/income-record', component: PrototypePage, props: { pageKey: 'finance-income-record' }, meta: { icon: 'lucide:plus-circle', title: '收款记录' } },
      { name: 'PaymentRecord', path: '/finance/payment-record', component: PrototypePage, props: { pageKey: 'finance-payment-record' }, meta: { icon: 'lucide:minus-circle', title: '付款记录' } },
      { name: 'Invoice', path: '/finance/invoice', component: PrototypePage, props: { pageKey: 'finance-invoice' }, meta: { icon: 'lucide:file-text', title: '发票记录' } },
      { name: 'PaymentInvoice', path: '/finance/payment-invoice', component: PrototypePage, props: { pageKey: 'finance-payment-invoice' }, meta: { icon: 'lucide:file-stack', title: '付款发票明细' } },
      { name: 'PaymentProgress', path: '/finance/payment-progress', component: PrototypePage, props: { pageKey: 'finance-payment-progress' }, meta: { icon: 'lucide:bar-chart-3', title: '财务收支进度' } },
      { name: 'Offset', path: '/finance/offset', component: PrototypePage, props: { pageKey: 'finance-offset' }, meta: { icon: 'lucide:arrow-left-right', title: '应收应付冲抵' } },
    ],
  },
];

export default routes;
