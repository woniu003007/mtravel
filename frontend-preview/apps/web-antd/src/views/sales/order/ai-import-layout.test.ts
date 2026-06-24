import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('sales order ai import assistant', () => {
  it('exposes ai-assisted draft import without auto-saving business data', () => {
    const source = readAppFile('src/views/sales/order/index.vue');
    const api = readAppFile('src/api/sales/booking-ai-import.ts');

    expect(source).toContain('AI辅助录入');
    expect(source).toContain('aiImportOpen');
    expect(source).toContain('recognizeBookingAiImport');
    expect(source).toContain('uploadAttachment');
    expect(source).toContain('beforeUploadAiImportFile');
    expect(source).toContain('attachmentId');
    expect(source).toContain('行程说明');
    expect(source).toContain('导游相关');
    expect(source).toContain('客户信息');
    expect(source).toContain('价格信息');
    expect(source).toContain('附加说明');
    expect(source).toContain('游客名单');
    expect(source).toContain('身份证校验');
    expect(source).toContain('疑似领队');
    expect(source).toContain('分房');
    expect(source).toContain('只填入当前表单，不会自动保存订单');
    expect(source).toContain('guest-warning-row');
    expect(source).toContain('leaderSourceText');

    expect(api).toContain('/sales/booking/ai-import/recognize');
    expect(api).toContain('BookingAiImportApi');
    expect(api).toContain('roomGroup');
    expect(api).toContain('idCardValid');
    expect(api).toContain('suspectedLeader');
  });
});
