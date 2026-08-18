import { requestClient } from '#/api/request';

/** 团队 Word 智能代录接口模型，任务阶段只保存可编辑草稿，不直接写入正式业务数据。 */
export namespace TeamDocumentImportApi {
  export type DocumentType = 'ground_confirmation' | 'guest_list' | 'mixed' | 'product_itinerary' | 'quotation';
  export type TaskStatus = 'applied' | 'extracting' | 'failed' | 'matching' | 'pending' | 'recognizing' | 'reviewing';
  export type ArrangementType =
    | 'extra_fee'
    | 'ground_agent'
    | 'hotel'
    | 'meal'
    | 'optional'
    | 'other'
    | 'scenic'
    | 'shopping'
    | 'traffic'
    | 'vehicle';

  export interface TeamDraft {
    businessType?: string;
    departureDate?: string;
    domesticInternational?: 'domestic' | 'international';
    receptionStandard?: string;
    remark?: string;
    teamName?: string;
    totalSeats?: number;
    travelDays?: number;
  }

  export interface OrderPriceDraft {
    itemName?: string;
    lineType?: string;
    quantity?: number;
    unitPrice?: number;
  }

  export interface OrderDraft {
    contactName?: string;
    contactPhone?: string;
    customerId?: number;
    customerName?: string;
    dropoffInfo?: string;
    guideName?: string;
    guidePhone?: string;
    orderRemark?: string;
    pickupInfo?: string;
    priceLines?: OrderPriceDraft[];
  }

  export interface GuestDraft {
    age?: number;
    birthDate?: string;
    certificateNo?: string;
    gender?: string;
    guestName?: string;
    guestType?: string;
    idCardValid?: boolean;
    indexNo?: number;
    leaderFlag?: boolean;
    phone?: string;
    remark?: string;
    roomGroup?: string;
    roomRemark?: string;
  }

  export interface ItineraryDraft {
    accommodationNote?: string;
    breakfastIncluded?: boolean;
    dayNo?: number;
    dayTitle?: string;
    dinnerIncluded?: boolean;
    itineraryContent?: string;
    lunchIncluded?: boolean;
  }

  /** 从 Word 识别出的产品说明候选，填入团队前仍可由计调核对和修改。 */
  export interface ProductDescriptionDraft {
    attentionItems?: string;
    childPolicy?: string;
    content?: string;
    feeExcluded?: string;
    feeIncluded?: string;
    giftItems?: string;
    optionalItems?: string;
    shoppingArrangement?: string;
    warmReminder?: string;
  }

  export interface ResourceCandidate {
    city?: string;
    defaultSupplier?: boolean;
    exactMatch?: boolean;
    resourceId: number;
    resourceName: string;
    resourceType: string;
    supplierId?: number;
    supplierName?: string;
  }

  export interface ResourceDraft {
    arrangementType: ArrangementType;
    candidates?: ResourceCandidate[];
    city?: string;
    dayNo?: number;
    itemKey: string;
    remark?: string;
    requiresConfirmation: boolean;
    selectedResourceId?: number;
    selectedResourceName?: string;
    selectedSupplierId?: number;
    selectedSupplierName?: string;
    sourceName: string;
    /** 可选的原文时间；旧接口没有该字段时由前端保持原始顺序。 */
    time?: string;
  }

  export interface Draft {
    confidence?: number;
    documentType?: DocumentType;
    evidence?: string[];
    guests?: GuestDraft[];
    itineraryDays?: ItineraryDraft[];
    order?: OrderDraft;
    productDescription?: ProductDescriptionDraft;
    resources?: ResourceDraft[];
    team?: TeamDraft;
    warnings?: string[];
  }

  export interface Task {
    appliedTeamId?: number;
    attachmentId: number;
    createdAt?: string;
    documentType?: DocumentType;
    draft?: Draft;
    errorMessage?: string;
    id: number;
    progressPercent: number;
    sourceType: string;
    status: TaskStatus;
    targetTeamId?: number;
    updatedAt?: string;
    warnings?: string[];
  }

  export interface ApplyResult {
    alreadyApplied: boolean;
    arrangementIds: number[];
    guestCount: number;
    orderId: number;
    teamId: number;
  }
}

/** 创建团队 Word 识别任务。 */
export function createTeamDocumentImportTask(data: { attachmentId: number; targetTeamId?: number }) {
  return requestClient.post<TeamDocumentImportApi.Task>('/sales/team/document-import/tasks', data);
}

/** 查询团队 Word 识别任务进度和草稿。 */
export function getTeamDocumentImportTask(taskId: number) {
  return requestClient.get<TeamDocumentImportApi.Task>(`/sales/team/document-import/tasks/${taskId}`);
}

/** 保存计调确认后的文档草稿和资源候选选择。 */
export function updateTeamDocumentImportDraft(taskId: number, draft: TeamDocumentImportApi.Draft) {
  return requestClient.put<TeamDocumentImportApi.Task>(`/sales/team/document-import/tasks/${taskId}/draft`, { draft });
}

/** 重试失败的文档识别任务。 */
export function retryTeamDocumentImportTask(taskId: number) {
  return requestClient.post<TeamDocumentImportApi.Task>(`/sales/team/document-import/tasks/${taskId}/retry`);
}

/** 在团队保存后幂等写入订单、游客和已确认资源安排。 */
export function applyTeamDocumentImportTask(
  taskId: number,
  data: { applyArrangements: boolean; applyGuests: boolean; teamId: number },
) {
  return requestClient.post<TeamDocumentImportApi.ApplyResult>(
    `/sales/team/document-import/tasks/${taskId}/apply`,
    data,
  );
}
