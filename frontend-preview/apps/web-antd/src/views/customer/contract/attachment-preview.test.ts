import { describe, expect, it } from 'vitest';

import {
  attachmentDownloadPath,
  attachmentPreviewKind,
  canPreviewAttachment,
} from './attachment-preview';

describe('contract attachment preview helpers', () => {
  it('keeps backend attachment paths on the API client path instead of frontend routes', () => {
    expect(
      attachmentDownloadPath('/attachments/1/2026-06-10/demo.pdf'),
    ).toBe('/attachments/1/2026-06-10/demo.pdf');
    expect(attachmentDownloadPath('')).toBe('');
  });

  it('detects pdf and image files as inline previewable attachments', () => {
    expect(
      attachmentPreviewKind({
        contentType: 'application/pdf',
        fileExt: undefined,
        originalFilename: '合同.PDF',
      }),
    ).toBe('pdf');
    expect(
      attachmentPreviewKind({
        contentType: 'image/png',
        fileExt: 'png',
        originalFilename: '合同扫描件.png',
      }),
    ).toBe('image');
  });

  it('does not promise inline preview for office files', () => {
    const officeAttachment = {
      contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      fileExt: 'docx',
      originalFilename: '合同.docx',
    };

    expect(attachmentPreviewKind(officeAttachment)).toBe('download-only');
    expect(canPreviewAttachment(officeAttachment)).toBe(false);
  });
});
