import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('system config page layout', () => {
  it('uses real system config APIs for risk, ai, map and auth settings', () => {
    const source = readAppFile('src/views/system/config/index.vue');
    const apiSource = readAppFile('src/api/system/config.ts');
    const routeSource = readAppFile('src/router/routes/modules/system.ts');

    expect(source).toContain('业务风控');
    expect(source).toContain('客户风险总经理审批');
    expect(source).toContain('百炼配置');
    expect(source).toContain('高德地图');
    expect(source).toContain('登录安全');
    expect(source).toContain('getBusinessRiskConfig');
    expect(source).toContain('updateBusinessRiskConfig');
    expect(source).toContain('getAiConfig');
    expect(source).toContain('updateAiConfig');
    expect(source).toContain('getMapConfig');
    expect(source).toContain('updateMapConfig');
    expect(source).toContain('getAuthConfig');
    expect(source).toContain('updateAuthConfig');
    expect(apiSource).toContain('/system/config/business-risk');
    expect(apiSource).toContain('/system/config/map/update');
    expect(routeSource).toContain('/system/config');
    expect(routeSource).toContain('SystemConfigPage');
  });
});
