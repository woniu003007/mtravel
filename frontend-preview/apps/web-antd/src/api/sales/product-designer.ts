import { requestClient } from '#/api/request';

export namespace SalesProductDesignerApi {
  export type ProcurementMode = 'not_required' | 'required';
  export type QuoteStatus = 'confirmed' | 'draft';
  export type ResourceType =
    | 'ground_agent'
    | 'hotel'
    | 'other'
    | 'restaurant'
    | 'scenic'
    | 'shopping'
    | 'traffic'
    | 'vehicle';

  export interface SupplierPriceLine {
    marketPrice?: number;
    peerPrice?: number;
    projectName: string;
    teamPrice?: number;
  }

  export interface Supplier {
    isDefault: boolean;
    priceLines: SupplierPriceLine[];
    priceMode?: 'classified' | 'unified';
    referenceUnitPrice: number;
    relationId: number;
    supplierId: number;
    supplierName: string;
    unifiedPrice?: number;
  }

  export interface Introduction {
    content: string;
    id: number;
    indexVersion: number;
    noticeContent?: string;
    tags?: string;
    title: string;
  }

  export interface ResourceImage {
    attachmentId: number;
    fileExt?: string;
    id: number;
    isCover: boolean;
    originalFilename: string;
    sortOrder: number;
  }

  export interface MapResource {
    address?: string;
    city?: string;
    defaultSupplierId?: number;
    defaultSupplierName?: string;
    district?: string;
    id: number;
    latitude?: number;
    longitude?: number;
    procurementMode: ProcurementMode;
    province?: string;
    referenceUnitPrice: number;
    resourceName: string;
    resourceType: ResourceType;
    status: string;
  }

  export interface ResourceDetail extends Omit<MapResource, 'defaultSupplierName' | 'referenceUnitPrice' | 'status'> {
    images: ResourceImage[];
    introduction?: string;
    introductions: Introduction[];
    suppliers: Supplier[];
    warmTip?: string;
  }

  export interface DayResource {
    address?: string;
    city?: string;
    costAmount: number;
    dayNo: number;
    district?: string;
    id: number;
    includeInWord: boolean;
    introductionContent?: string;
    introductionIndexVersion?: number;
    introductionNotice?: string;
    introductionTitle?: string;
    latitude?: number;
    longitude?: number;
    productId: number;
    procurementMode: ProcurementMode;
    province?: string;
    quantity: number;
    remark?: string;
    resourceId: number;
    resourceName: string;
    resourceType: ResourceType;
    selectedIntroductionId?: number;
    selectedImageIds: number[];
    sortOrder: number;
    stayMinutes: number;
    supplierId?: number;
    supplierName?: string;
    unitPrice: number;
  }

  export interface DayPlan {
    dayCostAmount: number;
    dayNo: number;
    resources: DayResource[];
  }

  export interface AdultQuote {
    adultCostAmount: number;
    adultSaleAmount: number;
    id: number;
    markupAmount: number;
    plannedAdultCount: number;
    productId: number;
    quoteRemark?: string;
    status: QuoteStatus;
    validUntil?: string;
  }

  export interface Detail {
    adultQuote?: AdultQuote;
    city?: string;
    days: DayPlan[];
    productId: number;
    productName: string;
    province?: string;
    status: string;
    totalCostAmount: number;
    travelDays: number;
  }

  export interface Draft {
    businessType?: string;
    city?: string;
    createdAt?: string;
    createdBy?: string;
    designStatus: 'designing';
    district?: string;
    domesticInternational: 'domestic' | 'international';
    id: number;
    productName: string;
    productTheme?: string;
    province?: string;
    receptionStandard?: string;
    remark?: string;
    travelDays: number;
    updatedAt?: string;
  }

  export interface DraftQuery {
    businessType?: string;
    city?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
  }

  export interface DraftSaveRequest {
    businessType?: string;
    city?: string;
    district?: string;
    domesticInternational?: 'domestic' | 'international';
    productName: string;
    productTheme?: string;
    province?: string;
    receptionStandard?: string;
    remark?: string;
    travelDays: number;
  }

  export interface DocumentVersion {
    documentType: 'adult_quote' | 'product_word';
    downloadUrl: string;
    fileName: string;
    generateStatus: 'failed' | 'pending' | 'success';
    id: number;
    productId: number;
    versionNo: number;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface ResourceQuery {
    city?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
    province?: string;
    resourceType?: ResourceType;
    scenicLevel?: 'unrated' | '1a' | '2a' | '3a' | '4a' | '5a';
    starLevel?: 'unrated' | '1star' | '2star' | '3star' | '4star' | '5star';
  }

  export interface DayResourceSaveRequest {
    dayNo: number;
    id?: number;
    includeInWord?: boolean;
    productId: number;
    quantity?: number;
    remark?: string;
    resourceId: number;
    selectedIntroductionId?: number;
    selectedImageIds?: number[];
    sortOrder?: number;
    stayMinutes?: number;
    supplierId?: number;
  }

  export interface AdultQuoteSaveRequest {
    adultSaleAmount?: number;
    id?: number;
    markupAmount?: number;
    plannedAdultCount: number;
    productId: number;
    quoteRemark?: string;
    status?: QuoteStatus;
    validUntil?: string;
  }
}

export function getSalesProductDesignerDraftPage(params: SalesProductDesignerApi.DraftQuery) {
  return requestClient.get<SalesProductDesignerApi.PageResult<SalesProductDesignerApi.Draft>>(
    '/sales/product/designer/draft/page',
    { params },
  );
}

export function getSalesProductDesignerDraftDetail(id: number) {
  return requestClient.get<SalesProductDesignerApi.Draft>('/sales/product/designer/draft/detail', {
    params: { id },
  });
}

export function createSalesProductDesignerDraft(data: SalesProductDesignerApi.DraftSaveRequest) {
  return requestClient.post<SalesProductDesignerApi.Draft>('/sales/product/designer/draft/create', data);
}

export function updateSalesProductDesignerDraft(id: number, data: SalesProductDesignerApi.DraftSaveRequest) {
  return requestClient.post<SalesProductDesignerApi.Draft>(
    '/sales/product/designer/draft/update',
    data,
    { params: { id } },
  );
}

export function deleteSalesProductDesignerDraft(id: number) {
  return requestClient.post<void>('/sales/product/designer/draft/delete', {}, { params: { id } });
}

export function publishSalesProductDesignerDraft(id: number) {
  return requestClient.post<number>('/sales/product/designer/draft/publish', {}, { params: { id } });
}

export function getSalesProductDesignerDetail(id: number) {
  return requestClient.get<SalesProductDesignerApi.Detail>('/sales/product/designer/detail', { params: { id } });
}

export function getSalesProductDesignerResources(params: SalesProductDesignerApi.ResourceQuery) {
  return requestClient.get<SalesProductDesignerApi.PageResult<SalesProductDesignerApi.MapResource>>(
    '/sales/product/designer/resources',
    { params },
  );
}

export function getSalesProductDesignerResourceDetail(resourceId: number) {
  return requestClient.get<SalesProductDesignerApi.ResourceDetail>('/sales/product/designer/resource-detail', {
    params: { resourceId },
  });
}

export function saveSalesProductDesignerDayResource(data: SalesProductDesignerApi.DayResourceSaveRequest) {
  return requestClient.post<SalesProductDesignerApi.DayResource>('/sales/product/designer/day-resource/save', data);
}

export function deleteSalesProductDesignerDayResource(data: { id: number; productId: number }) {
  return requestClient.post<void>('/sales/product/designer/day-resource/delete', data);
}

export function reorderSalesProductDesignerDayResources(data: {
  dayNo: number;
  dayResourceIds: number[];
  productId: number;
}) {
  return requestClient.post<void>('/sales/product/designer/day-resource/reorder', data);
}

export function saveSalesProductDesignerIntroduction(data: {
  dayResourceId: number;
  productId: number;
  selectedIntroductionId?: number;
}) {
  return requestClient.post<SalesProductDesignerApi.DayResource>('/sales/product/designer/day-resource/intro', data);
}

export function saveSalesProductDesignerAdultQuote(data: SalesProductDesignerApi.AdultQuoteSaveRequest) {
  return requestClient.post<SalesProductDesignerApi.AdultQuote>('/sales/product/designer/adult-quote/save', data);
}

export function generateSalesProductDesignerProductWord(productId: number) {
  return requestClient.post<SalesProductDesignerApi.DocumentVersion>(
    '/sales/product/designer/documents/product-word',
    {},
    { params: { productId } },
  );
}

export function generateSalesProductDesignerAdultQuote(productId: number) {
  return requestClient.post<SalesProductDesignerApi.DocumentVersion>(
    '/sales/product/designer/documents/adult-quote',
    {},
    { params: { productId } },
  );
}

export function getSalesProductDesignerDocuments(productId: number) {
  return requestClient.get<SalesProductDesignerApi.DocumentVersion[]>('/sales/product/designer/documents', {
    params: { productId },
  });
}

export function downloadSalesProductDesignerDocument(versionId: number) {
  // Binary downloads must bypass the shared JSON response interceptor.
  return requestClient.download<Blob>(`/sales/product/designer/documents/${versionId}/download`);
}
