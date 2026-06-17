import type { RouteRecordRaw } from 'vue-router';

const PrototypePage = () => import('#/views/_business/PrototypePage.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: { icon: 'lucide:settings', order: 8, title: '系统设置' },
    name: 'System',
    path: '/system',
    redirect: '/system/params',
    children: [
      { name: 'SystemParams', path: '/system/params', component: PrototypePage, props: { pageKey: 'system-params' }, meta: { icon: 'lucide:sliders-horizontal', title: '业务参数中心' } },
      { name: 'EContractConfig', path: '/system/e-contract-config', component: PrototypePage, props: { pageKey: 'system-e-contract-config' }, meta: { icon: 'lucide:file-cog', title: '电子合同配置' } },
      { name: 'ApprovalAlert', path: '/system/approval-alert', component: PrototypePage, props: { pageKey: 'system-approval-alert' }, meta: { icon: 'lucide:bell-ring', title: '审批与预警配置' } },
      { name: 'MessageNotify', path: '/system/message-notify', component: PrototypePage, props: { pageKey: 'system-message-notify' }, meta: { icon: 'lucide:message-square', title: '消息通知配置' } },
      { name: 'OperationLog', path: '/system/operation-log', component: PrototypePage, props: { pageKey: 'system-operation-log' }, meta: { icon: 'lucide:scroll', title: '系统操作日志' } },
      { name: 'SystemTeamLog', path: '/system/team-log', component: PrototypePage, props: { pageKey: 'system-team-log' }, meta: { icon: 'lucide:scroll-text', title: '团号日志' } },
      { name: 'HeaderFooter', path: '/system/header-footer', component: PrototypePage, props: { pageKey: 'system-header-footer' }, meta: { icon: 'lucide:stamp', title: '页眉页脚印章' } },
      { name: 'SystemStyle', path: '/system/style', component: PrototypePage, props: { pageKey: 'system-style' }, meta: { icon: 'lucide:palette', title: '系统风格设置' } },
      { name: 'ShortcutMenu', path: '/system/shortcut-menu', component: PrototypePage, props: { pageKey: 'system-shortcut-menu' }, meta: { icon: 'lucide:zap', title: '快捷菜单设置' } },
    ],
  },
];

export default routes;
