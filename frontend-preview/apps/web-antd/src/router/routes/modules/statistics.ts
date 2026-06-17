import type { RouteRecordRaw } from 'vue-router';

const PrototypePage = () => import('#/views/_business/PrototypePage.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:bar-chart-2', order: 6, title: '数据统计' },
    name: 'Statistics',
    path: '/statistics',
    redirect: '/statistics/team-progress',
    children: [
      { name: 'TeamProgress', path: '/statistics/team-progress', component: PrototypePage, props: { pageKey: 'statistics-team-progress' }, meta: { icon: 'lucide:activity', title: '团队进度统计' } },
      { name: 'Reception', path: '/statistics/reception', component: PrototypePage, props: { pageKey: 'statistics-reception' }, meta: { icon: 'lucide:user-plus', title: '收客统计' } },
      { name: 'Profit', path: '/statistics/profit', component: PrototypePage, props: { pageKey: 'statistics-profit' }, meta: { icon: 'lucide:trending-up', title: '利润统计' } },
      { name: 'ResourcePurchase', path: '/statistics/resource-purchase', component: PrototypePage, props: { pageKey: 'statistics-resource-purchase' }, meta: { icon: 'lucide:package-search', title: '资源采购统计' } },
      { name: 'AccountStat', path: '/statistics/account', component: PrototypePage, props: { pageKey: 'statistics-account' }, meta: { icon: 'lucide:book-open', title: '账款统计' } },
      { name: 'GuideStat', path: '/statistics/guide-stat', component: PrototypePage, props: { pageKey: 'statistics-guide-stat' }, meta: { icon: 'lucide:compass', title: '导游统计' } },
      { name: 'IncomeSummary', path: '/statistics/income-summary', component: PrototypePage, props: { pageKey: 'statistics-income-summary' }, meta: { icon: 'lucide:arrow-down-circle', title: '收款汇总统计' } },
      { name: 'PaymentSummary', path: '/statistics/payment-summary', component: PrototypePage, props: { pageKey: 'statistics-payment-summary' }, meta: { icon: 'lucide:arrow-up-circle', title: '付款汇总统计' } },
      { name: 'ReportCenter', path: '/statistics/report-center', component: PrototypePage, props: { pageKey: 'statistics-report-center' }, meta: { icon: 'lucide:file-bar-chart', title: '报表中心' } },
    ],
  },
];

export default routes;
