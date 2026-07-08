import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('guide advance page layout', () => {
  it('shows authorization balance and supports cancelling stale guide imprest requests', () => {
    const source = readAppFile('src/views/finance/guide-advance/index.vue');
    const apiSource = readAppFile('src/api/finance/guide-imprest.ts');

    expect(source).toContain('已占用授权');
    expect(source).toContain('当前可申请');
    expect(source).toContain('cancelGuideImprest');
    expect(source).toContain('团队安排已变化，请作废旧申请并重新提交');
    expect(source).toContain(':disabled="Boolean(currentRecord?.calculationChanged)"');
    expect(apiSource).toContain('/finance/guide-imprests/cancel');
    expect(apiSource).toContain('availableAuthorizationAmount');
    expect(apiSource).toContain('occupiedAuthorizationAmount');
  });
});
