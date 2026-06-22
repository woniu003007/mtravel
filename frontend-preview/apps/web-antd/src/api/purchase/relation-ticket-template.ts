import { requestClient } from '#/api/request';

export namespace RelationTicketTemplateApi {
  export type Status = 'active' | 'disabled';

  export interface Field {
    id?: number;
    templateId?: number;
    templateHeader: string;
    columnIndex: number;
    systemField?: string;
    systemFieldLabel?: string;
    fillMode?: string;
    fillModeLabel?: string;
    fixedValue?: string;
    required: boolean;
    sortOrder?: number;
  }

  export interface Template {
    id?: number;
    relationId: number;
    templateName: string;
    attachmentId: number;
    templateFileUrl?: string;
    originalFilename?: string;
    sheetName?: string;
    headerRow: number;
    dataStartRow: number;
    status: Status;
    remark?: string;
    fields: Field[];
    createdAt?: string;
    updatedAt?: string;
  }

  export interface Header {
    columnIndex: number;
    templateHeader: string;
    systemField?: string;
    systemFieldLabel?: string;
    fillMode?: string;
    fixedValue?: string;
    required: boolean;
  }

  export interface HeaderResponse {
    sheetName?: string;
    headerRow: number;
    headers: Header[];
  }

  export interface SystemField {
    value: string;
    label: string;
  }

  export interface FillMode {
    value: string;
    label: string;
  }

  export interface SaveParams {
    relationId: number;
    templateName: string;
    attachmentId: number;
    templateFileUrl?: string;
    originalFilename?: string;
    sheetName?: string;
    headerRow: number;
    dataStartRow: number;
    status: Status;
    remark?: string;
    fields: Field[];
  }
}

export function getRelationTicketTemplateDetail(relationId: number) {
  return requestClient.get<RelationTicketTemplateApi.Template | null>(
    '/purchase/relation/ticket-template/detail',
    { params: { relationId } },
  );
}

export function getRelationTicketTemplateHeaders(params: {
  attachmentId: number;
  headerRow?: number;
}) {
  return requestClient.get<RelationTicketTemplateApi.HeaderResponse>(
    '/purchase/relation/ticket-template/headers',
    { params },
  );
}

export function getRelationTicketTemplateSystemFields() {
  return requestClient.get<RelationTicketTemplateApi.SystemField[]>(
    '/purchase/relation/ticket-template/system-fields',
  );
}

export function getRelationTicketTemplateFillModes() {
  return requestClient.get<RelationTicketTemplateApi.FillMode[]>(
    '/purchase/relation/ticket-template/fill-modes',
  );
}

export function saveRelationTicketTemplate(data: RelationTicketTemplateApi.SaveParams) {
  return requestClient.post<RelationTicketTemplateApi.Template>(
    '/purchase/relation/ticket-template/save',
    data,
  );
}

export function deleteRelationTicketTemplate(relationId: number) {
  return requestClient.post<void>(
    '/purchase/relation/ticket-template/delete',
    {},
    { params: { relationId } },
  );
}
