import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('credit over-limit approval page', () => {
  it('supports the three user-scoped views and sequential approval details', () => {
    const source = readAppFile(
      'src/views/approval/credit-over-limit/index.vue',
    );
    const apiSource = readAppFile('src/api/customer/risk-approval.ts');

    expect(source).toContain('待我审批');
    expect(source).toContain('我发起的');
    expect(source).toContain('抄送我的');
    expect(source).toContain("key: 'to_approve'");
    expect(source).toContain("key: 'initiated'");
    expect(source).toContain("key: 'cc'");
    expect(source).toContain('view: activeView.value');
    expect(source).toContain('record.canApprove');
    expect(source).toContain('currentRecord.approvalSteps');
    expect(source).toContain('getCustomerRiskApprovalDetail');
    expect(source).toContain('approveCustomerRiskApproval');
    expect(source).toContain('rejectCustomerRiskApproval');
    expect(apiSource).toContain(
      "export type ApprovalView = 'cc' | 'initiated' | 'to_approve'",
    );
    expect(apiSource).toContain('approvalSteps?: ApprovalStep[]');
    expect(apiSource).toContain('ccUsers?: CcUser[]');
    expect(apiSource).toContain('canApprove?: boolean');
  });

  it('exposes the page under the approval management menu', () => {
    const routeSource = readAppFile('src/router/routes/modules/approval.ts');

    expect(routeSource).toContain("title: '审批管理'");
    expect(routeSource).toContain("title: '授信超额审批'");
    expect(routeSource).toContain('/approval/credit-over-limit');
  });
});
