import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('risk approval page layout', () => {
  it('shows manager approval workflow and calls real approval APIs', () => {
    const source = readAppFile('src/views/system/risk-approval/index.vue');
    const apiSource = readAppFile('src/api/customer/risk-approval.ts');
    const routeSource = readAppFile('src/router/routes/modules/system.ts');

    expect(source).toContain('总经理审批');
    expect(source).toContain('getCustomerRiskApprovalPage');
    expect(source).toContain('approveCustomerRiskApproval');
    expect(source).toContain('rejectCustomerRiskApproval');
    expect(source).toContain('审批通过');
    expect(source).toContain('审批拒绝');
    expect(source).toContain('风险摘要');
    expect(source).toContain('授信额度');
    expect(source).toContain('可用额度');
    expect(source).toContain('超限金额');
    expect(apiSource).toContain('/customer/risk-approval/page');
    expect(apiSource).toContain('/customer/risk-approval/approve');
    expect(apiSource).toContain('/customer/risk-approval/reject');
    expect(routeSource).toContain('/system/risk-approval');
    expect(routeSource).toContain('RiskApprovalPage');
  });
});
