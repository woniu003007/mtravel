import { requestClient } from '#/api/request';

export namespace AttachmentApi {
  export interface Attachment {
    id: number;
    businessModule: string;
    businessType: string;
    businessId?: number;
    originalFilename: string;
    fileUrl: string;
    contentType?: string;
    fileSize: number;
    fileExt?: string;
    status: 'active' | 'disabled';
    uploadedBy?: string;
    createdAt?: string;
  }
}

export function uploadAttachment(
  data: FormData,
) {
  return requestClient.post<AttachmentApi.Attachment>(
    '/common/attachment/upload',
    data,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}

export function listAttachments(params: {
  businessId?: number;
  businessModule: string;
  businessType: string;
  page?: number;
  pageSize?: number;
}) {
  return requestClient.get<AttachmentApi.Attachment[]>(
    '/common/attachment/list',
    { params },
  );
}

export function downloadAttachment(fileUrl: string) {
  return requestClient.download<Blob>(fileUrl);
}
