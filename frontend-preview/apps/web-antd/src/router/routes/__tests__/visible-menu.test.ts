import { readFileSync, readdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');
const routeModuleDir = resolve(appRoot, 'src/router/routes/modules');
const undevelopedPrefixes = [
  '/finance',
  '/statistics',
  '/sales/schedule',
  '/sales/group-booking',
  '/sales/combine-order',
  '/sales/shared-car-cost',
  '/sales/expense-change',
  '/sales/e-contract',
  '/sales/ticket-booking',
  '/sales/tourist',
  '/sales/name-check',
  '/sales/ai-service',
  '/sales/smart-quote',
  '/sales/knowledge-base',
  '/dispatch/team-arrange',
  '/dispatch/team-audit',
  '/dispatch/transfer-info',
  '/dispatch/guide-expense',
  '/dispatch/car-inquiry',
  '/enterprise/contract-template',
  '/enterprise/station',
];

function readRouteModule(fileName: string) {
  return readFileSync(resolve(routeModuleDir, fileName), 'utf8');
}

function visibleRouteChunk(source: string) {
  return source
    .split('\n')
    .filter((line) => !line.includes('hideInMenu: true'))
    .join('\n');
}

describe('visible business menu routes', () => {
  it('does not expose undeveloped prototype pages as visible menus', () => {
    const businessRouteFiles = readdirSync(routeModuleDir)
      .filter((fileName) => fileName.endsWith('.ts'))
      .filter((fileName) => !fileName.endsWith('.test.ts'))
      .filter((fileName) => !['dashboard.ts', 'demos.ts', 'vben.ts'].includes(fileName));

    for (const fileName of businessRouteFiles) {
      const source = readRouteModule(fileName);

      // 可见菜单只能指向真实业务页面；PrototypePage 是原型占位页，不能继续出现在侧边栏。
      expect(source, `${fileName} still references PrototypePage`).not.toContain(
        'PrototypePage',
      );
    }
  });

  it('keeps only the currently developed sidebar menu entries', () => {
    const allBusinessRoutes = readdirSync(routeModuleDir)
      .filter((fileName) => fileName.endsWith('.ts'))
      .filter((fileName) => !fileName.endsWith('.test.ts'))
      .map(readRouteModule)
      .map(visibleRouteChunk)
      .join('\n');

    expect(allBusinessRoutes).toContain("title: '客户单位'");
    expect(allBusinessRoutes).toContain("title: '产品管理'");
    expect(allBusinessRoutes).toContain("title: '团队管理'");
    expect(allBusinessRoutes).toContain("title: '订单管理'");
    expect(allBusinessRoutes).toContain("title: '导游排班汇总'");
    expect(allBusinessRoutes).toContain("title: '导游请假管理'");
    expect(allBusinessRoutes).toContain("title: '导游管理'");
    expect(allBusinessRoutes).toContain("title: '我的请假'");
    expect(allBusinessRoutes).toContain("title: '产品字典'");
    expect(allBusinessRoutes).toContain("title: '系统配置'");
    expect(allBusinessRoutes).toContain("title: '总经理审批'");

    expect(allBusinessRoutes).not.toContain("title: '团期管理'");
    expect(allBusinessRoutes).not.toContain("title: '财务管理'");
    expect(allBusinessRoutes).not.toContain("title: '数据统计'");
    expect(allBusinessRoutes).not.toContain("title: '合同模板管理'");
    expect(allBusinessRoutes).not.toContain("title: '接送站管理'");
    expect(allBusinessRoutes).not.toContain("title: '客户授信规则'");
    expect(allBusinessRoutes).not.toContain("title: '普通资源报价规则'");
  });

  it('does not link dashboard shortcuts to undeveloped pages', () => {
    const workspaceSource = readFileSync(
      resolve(appRoot, 'src/views/dashboard/workspace/index.vue'),
      'utf8',
    );
    const prototypeDataSource = readFileSync(
      resolve(appRoot, 'src/views/_business/prototype-data.ts'),
      'utf8',
    );
    const prototypeModuleChunk = prototypeDataSource.match(
      /export const prototypeModules[\s\S]*?\n\];/,
    )?.[0] ?? '';
    const workbenchAlertChunk = prototypeDataSource.match(
      /export const workbenchAlerts[\s\S]*?\n\];/,
    )?.[0] ?? '';

    for (const prefix of undevelopedPrefixes) {
      expect(workspaceSource, `workspace still links ${prefix}`).not.toContain(
        `navTo('${prefix}`,
      );
      expect(
        prototypeModuleChunk,
        `prototypeModules still links ${prefix}`,
      ).not.toContain(`path: '${prefix}`);
      expect(
        workbenchAlertChunk,
        `workbenchAlerts still links ${prefix}`,
      ).not.toContain(`path: '${prefix}`);
    }
  });
});
