import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');

function readAppFile(path: string) {
  return readFileSync(resolve(appRoot, path), 'utf8');
}

describe('enterprise guide upload fields', () => {
  it('uses ordinary ant design tabs instead of pill tabs on guide management', () => {
    const source = readAppFile('src/views/enterprise/guide/index.vue');

    expect(source).toContain('<Tabs');
    expect(source).toContain('<Tabs.TabPane key="guides" tab="导游档案">');
    expect(source).toContain('<Tabs.TabPane key="tags" tab="标签管理">');
    expect(source).not.toContain('BusinessPillTabs');
  });

  it('uses upload controls for guide certificate and personal photo without cost or avatar fields', () => {
    const source = readAppFile('src/views/enterprise/guide/index.vue');

    expect(source).toContain('uploadAttachment');
    expect(source).toContain('beforeUploadGuideCertificate');
    expect(source).toContain('beforeUploadGuidePhoto');
    expect(source).toContain('downloadAttachment');
    expect(source).toContain('createGuideUploadFileList');
    expect(source).toContain(':on-preview="previewGuideCertificate"');
    expect(source).toContain(':on-preview="previewGuidePhoto"');
    expect(source).toContain('filePreviewOpen');
    expect(source).toContain('guide-preview-image');
    expect(source).toContain('guide_certificate');
    expect(source).toContain('guide_photo');
    expect(source).toContain('导游证书');
    expect(source).toContain('个人照片');
    expect(source).toContain('<Upload');
    expect(source).not.toContain('挂账成本');
    expect(source).not.toContain('costVisibility');
    expect(source).not.toContain('头像地址');
    expect(source).not.toContain('avatarUrl');
    expect(source).not.toContain('后续接附件上传');
  });
});
