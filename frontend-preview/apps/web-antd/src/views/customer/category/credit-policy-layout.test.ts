import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('customer credit policy page', () => {
  it('uses the category credit workflow contract and lazy employee search', () => {
    const source = readAppFile('src/views/customer/category/index.vue');
    const apiSource = readAppFile('src/api/customer/category.ts');

    expect(source).toContain('客户等级授信配置');
    expect(source).toContain('规则ID');
    expect(source).toContain('账期天数');
    expect(source).toContain('审批人（按顺序）');
    expect(source).toContain('getEnterpriseEmployeePage');
    expect(source).toContain('pageSize: 20');
    expect(source).toContain("status: 'active'");
    expect(source).toContain(':filter-option="false"');
    expect(source).toContain('let employeeInitialLoaded = false;');
    expect(source).toContain(
      'if (open && !employeeInitialLoaded && !employeeLoading.value)',
    );
    expect(source).toContain(
      'new Map(options.map((option) => [option.value, option]))',
    );
    expect(source).not.toContain('getEnterpriseEmployeeAll');
    expect(source).toContain('RFM 规则暂未开放');
    expect(apiSource).toContain('creditTermDays');
    expect(apiSource).toContain('allowOverLimit');
    expect(apiSource).toContain('approvers');
    expect(apiSource).toContain('ccUsers');
  });

  it('exposes the page under the configuration management menu', () => {
    const routeSource = readAppFile(
      'src/router/routes/modules/configuration.ts',
    );

    expect(routeSource).toContain("title: '配置管理'");
    expect(routeSource).toContain("title: '客户等级授信配置'");
    expect(routeSource).toContain('/configuration/customer-credit-policy');
  });
});
