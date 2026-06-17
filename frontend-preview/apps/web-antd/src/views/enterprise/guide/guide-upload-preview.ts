import type { UploadFile } from 'ant-design-vue';

export type GuideUploadFile = UploadFile & {
  /**
   * 后端附件访问路径，形如 /attachments/**。
   *
   * 不写入 UploadFile.url，避免浏览器直接跳转到前端路由或无授权静态资源导致 404/403。
   * 页面预览时通过 requestClient 下载 Blob，会自动携带登录 token。
   */
  backendFileUrl?: string;
};

const IMAGE_EXTENSIONS = new Set(['bmp', 'gif', 'jpeg', 'jpg', 'png', 'webp']);

/** 根据后端附件路径创建上传列表项，点击预览时再走授权下载。 */
export function createGuideUploadFileList(
  fileUrl?: string,
  name = '已上传文件',
): GuideUploadFile[] {
  const backendFileUrl = fileUrl?.trim();
  if (!backendFileUrl) {
    return [];
  }
  return [{
    backendFileUrl,
    name,
    status: 'done',
    uid: backendFileUrl,
  }];
}

/** 读取上传列表项对应的后端附件路径。 */
export function guideUploadBackendUrl(file: GuideUploadFile, fallbackUrl?: string) {
  return file.backendFileUrl || fallbackUrl?.trim() || '';
}

/** 判断附件是否应该使用图片弹窗预览。 */
export function isGuidePreviewImage(filenameOrUrl: string, contentType?: string) {
  if (contentType?.toLowerCase().startsWith('image/')) {
    return true;
  }
  const source = filenameOrUrl.toLowerCase();
  const index = source.lastIndexOf('.');
  if (index < 0) {
    return false;
  }
  return IMAGE_EXTENSIONS.has(source.slice(index + 1));
}
