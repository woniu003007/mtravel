import { describe, expect, it } from 'vitest';

import {
  createGuideUploadFileList,
  guideUploadBackendUrl,
  isGuidePreviewImage,
} from './guide-upload-preview';

describe('enterprise guide upload preview helpers', () => {
  it('keeps backend attachment paths out of UploadFile.url to avoid frontend 404 links', () => {
    const file = createGuideUploadFileList(
      '/attachments/1/2026-06-17/photo.jpg',
      '导游照片.jpg',
    )[0]!;

    expect(file).toMatchObject({
      backendFileUrl: '/attachments/1/2026-06-17/photo.jpg',
      name: '导游照片.jpg',
      status: 'done',
    });
    expect(file.url).toBeUndefined();
    expect(guideUploadBackendUrl(file)).toBe('/attachments/1/2026-06-17/photo.jpg');
  });

  it('detects guide photos as image previews', () => {
    expect(isGuidePreviewImage('导游照片.JPG', '')).toBe(true);
    expect(isGuidePreviewImage('导游证书.pdf', 'application/pdf')).toBe(false);
    expect(isGuidePreviewImage('上传图片', 'image/png')).toBe(true);
  });
});
