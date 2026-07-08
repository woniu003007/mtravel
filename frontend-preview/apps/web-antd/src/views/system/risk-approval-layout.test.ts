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

  it('shows guide imprest calculation details for manager approval', () => {
    const source = readAppFile('src/views/system/risk-approval/index.vue');

    expect(source).toContain('导游备用金审批');
    expect(source).toContain('费用明细');
    expect(source).toContain('计算说明');
    expect(source).toContain('currentRecord.calcLines');
    expect(source).toContain('lineTypeText(record.lineType)');
    expect(source).toContain('guideImprestLineFormula(record)');
    expect(source).toContain('现付成本直接计入备用金需求');
    expect(source).toContain('公司加点率');
    expect(source).toContain('导游提成');
  });

  it('blocks guide imprest approval when arrangement calculation has changed', () => {
    const source = readAppFile('src/views/system/risk-approval/index.vue');

    expect(source).toContain('calculationChanged');
    expect(source).toContain('guideImprestChangeMessage');
    expect(source).toContain('calculationChangeMessage');
    expect(source).toContain('团队安排已变化，请计调重新计算备用金');
    expect(source).toContain(':disabled="Boolean(currentRecord.calculationChanged)"');
  });
});
