type AttachmentPreviewSource = {
  contentType?: string;
  fileExt?: string;
  originalFilename?: string;
};

export type AttachmentPreviewKind = 'download-only' | 'image' | 'pdf';

const IMAGE_EXTENSIONS = new Set(['bmp', 'gif', 'jpeg', 'jpg', 'png', 'webp']);

/**
 * 返回附件下载接口路径。
 *
 * 合同附件的 fileUrl 由后端保存为 /attachments/**。前端请求时仍交给
 * requestClient 处理，这样能自动带上登录 token，并通过 /api 代理到后端。
 */
export function attachmentDownloadPath(fileUrl?: string) {
  return fileUrl?.trim() || '';
}

/**
 * 判断合同附件在当前页面内应该采用哪种预览方式。
 */
export function attachmentPreviewKind(
  attachment: AttachmentPreviewSource,
): AttachmentPreviewKind {
  const contentType = attachment.contentType?.toLowerCase() || '';
  const extension = attachmentExtension(attachment);

  if (contentType.includes('pdf') || extension === 'pdf') {
    return 'pdf';
  }
  if (contentType.startsWith('image/') || IMAGE_EXTENSIONS.has(extension)) {
    return 'image';
  }
  return 'download-only';
}

export function canPreviewAttachment(attachment: AttachmentPreviewSource) {
  return attachmentPreviewKind(attachment) !== 'download-only';
}

function attachmentExtension(attachment: AttachmentPreviewSource) {
  if (attachment.fileExt) {
    return attachment.fileExt.trim().replace(/^\./, '').toLowerCase();
  }
  const filename = attachment.originalFilename || '';
  const index = filename.lastIndexOf('.');
  return index >= 0 ? filename.slice(index + 1).toLowerCase() : '';
}
