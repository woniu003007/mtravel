import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('dispatch guide scheduling feature', () => {
  it('exposes dispatcher leave management with approval and direct unavailable actions', () => {
    const source = readAppFile('src/views/dispatch/guide-leave/index.vue');

    expect(source).toContain('导游请假管理');
    expect(source).toContain('getGuideLeavePage');
    expect(source).toContain('createGuideLeaveDirect');
    expect(source).toContain('approveGuideLeave');
    expect(source).toContain('rejectGuideLeave');
    expect(source).toContain('直接设置请假');
    expect(source).toContain('审批通过');
    expect(source).toContain('驳回申请');
    expect(source).toContain("formatBackendDateTime");
    expect(source).toContain("YYYY-MM-DDTHH:mm:ss");
  });

  it('exposes guide self-service leave application and pending withdraw', () => {
    const source = readAppFile('src/views/guide/my-leave/index.vue');

    expect(source).toContain('我的请假');
    expect(source).toContain('getMyGuideLeaves');
    expect(source).toContain('submitMyGuideLeave');
    expect(source).toContain('withdrawMyGuideLeave');
    expect(source).toContain('申请请假');
    expect(source).toContain('撤回');
    expect(source).toContain("formatBackendDateTime");
    expect(source).toContain("YYYY-MM-DDTHH:mm:ss");
  });

  it('integrates team guide arrangement into the standalone team arrangement page', () => {
    const source = readAppFile('src/views/sales/team/arrangement.vue');

    expect(source).toContain("anchor: 'guide-arrangement'");
    expect(source).toContain('getTeamGuides');
    expect(source).toContain('createTeamGuide');
    expect(source).toContain('updateTeamGuideField');
    expect(source).toContain('deleteTeamGuide');
    expect(source).toContain('添加导游');
    expect(source).toContain('导服费');
    expect(source).toContain('备用金');
    expect(source).toContain('操作费');
    expect(source).toContain('待定中');
    expect(source).toContain("formatBackendDateTime");
    expect(source).toContain("YYYY-MM-DDTHH:mm:ss");
  });

  it('marks free guide dates as available without using strong warning colors', () => {
    const source = readAppFile('src/views/dispatch/guide-schedule/index.vue');

    expect(source).toContain('blocksForDate(row, date.date).length === 0');
    expect(source).toContain('schedule-free-marker');
    expect(source).toContain('空闲');
    expect(source).toContain('<Tag>空闲可派</Tag>');
  });
});
