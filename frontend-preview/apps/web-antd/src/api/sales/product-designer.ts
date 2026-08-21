import { requestClient } from '#/api/request';

export namespace SalesProductDesignerApi {
  export type ProcurementMode = 'not_required' | 'required';
  export type ArrangementRole = 'accommodation' | 'breakfast' | 'dinner' | 'itinerary' | 'lunch' | 'unassigned';
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

  export type OptionalItemType = 'recommended_self_pay' | 'scenic_transport';
  export type IntroductionExtensionBlockType = 'generic' | 'photo_recommendation' | 'warm_tip';

  export interface IntroductionExtensionBlock {
    content?: string;
    contentMode: 'items' | 'multiline';
    items: string[];
    title: string;
    titleColor: string;
    type: IntroductionExtensionBlockType;
  }

  /** 供应商对资源自费项目的内部成本报价；不直接输出到产品 Word。 */
  export interface SupplierOptionalItem {
    costPrice: number;
    id: number;
    resourceOptionalItemId: number;
    supplierOptionalItemId: number;
    projectName: string;
    status: 'active' | 'disabled';
    suggestedSalePrice?: number;
  }

  /** 当前资源下可供产品选择的自费项目主档。 */
  export interface ResourceOptionalItem {
    id: number;
    optionalItemType: OptionalItemType;
    projectName: string;
    status: 'active' | 'disabled';
    suggestedSalePrice?: number;
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
    optionalItems?: SupplierOptionalItem[];
  }

  export interface Introduction {
    content: string;
    id: number;
    /** 当前介绍素材选用的资源图片，按产品 Word 输出顺序返回。 */
    imageIds?: number[];
    indexVersion: number;
    isOptionalItem: boolean;
    resourceOptionalItemId?: number;
    optionalItemName?: string;
    optionalItemType?: OptionalItemType;
    noticeContent?: string;
    extensionBlocks?: IntroductionExtensionBlock[];
    tags?: string;
    title: string;
    visitDuration?: string;
    warmTipContent?: string;
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
    optionalItems?: ResourceOptionalItem[];
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
    introductionVisitDuration?: string;
    introductionWarmTip?: string;
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
    arrangementRole: ArrangementRole;
    hotelBreakfastIncluded?: boolean;
    /** 当前资源在当天行程中选用的多个介绍素材，按 Word 输出顺序排列。 */
    selectedIntroductionIds?: number[];
    /** 兼容首版单选介绍字段，后端切换期间保留读取。 */
    selectedIntroductionId?: number;
    selectedImageIds: number[];
    /** 已选自费项目的产品快照，供应商成本只用于内部参考。 */
    selectedOptionalItems?: SelectedOptionalItem[];
    /**
     * 产品资源内的统一素材编排。数组顺序即介绍和自费项目在产品 Word 中的输出顺序。
     * 旧产品没有该字段时，前端使用 selectedIntroductionIds / selectedOptionalItems 兼容回显。
     */
    selectedMaterials?: SelectedMaterial[];
    sortOrder: number;
    stayMinutes: number;
    supplierId?: number;
    supplierName?: string;
    unitPrice: number;
  }

  export interface DayPlan {
    accommodationCity?: string;
    breakfastIncluded?: boolean;
    dayCostAmount: number;
    dayNo: number;
    dinnerIncluded?: boolean;
    lunchIncluded?: boolean;
    resources: DayResource[];
  }

  export interface DayItinerary {
    accommodationCity?: string;
    breakfastIncluded: boolean;
    dayNo: number;
    dinnerIncluded: boolean;
    lunchIncluded: boolean;
  }

  export interface DayItinerarySaveRequest {
    accommodationCity?: string;
    breakfastIncluded?: boolean;
    dayNo: number;
    dinnerIncluded?: boolean;
    lunchIncluded?: boolean;
    productId: number;
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
    /** 后端提供的同一版本 PDF 预览地址；前端也支持通过预览接口读取 Blob。 */
    previewUrl?: string;
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
    starLevel?: string;
  }

  export interface DayResourceSaveRequest {
    arrangementRole?: ArrangementRole;
    dayNo: number;
    hotelBreakfastIncluded?: boolean;
    id?: number;
    includeInWord?: boolean;
    productId: number;
    quantity?: number;
    remark?: string;
    resourceId: number;
    /** 多个介绍素材 ID，数组顺序就是当天行程介绍的拼接顺序。 */
    selectedIntroductionIds?: number[];
    /** 兼容首版接口，取 selectedIntroductionIds 的第一项。 */
    selectedIntroductionId?: number;
    selectedImageIds?: number[];
    /** 普通介绍和自费项目的统一有序选择结果。 */
    selectedMaterials?: SelectedMaterialSaveRequest[];
    /** 仅将勾选的自费项目写入产品和 Word；salePrice 为本产品最终对外价。 */
    selectedOptionalItems?: SelectedOptionalItemSaveRequest[];
    sortOrder?: number;
    stayMinutes?: number;
    supplierId?: number;
  }

  export interface SelectedOptionalItem {
    introductionContent?: string;
    introductionId?: number;
    introductionTitle?: string;
    optionalItemType: OptionalItemType;
    resourceOptionalItemId: number;
    projectName: string;
    referenceCostPrice?: number;
    salePrice: number;
    suggestedSalePrice?: number;
    supplierOptionalItemId?: number;
    supplierName?: string;
  }

  export interface SelectedOptionalItemSaveRequest {
    costPrice?: number;
    introductionId?: number;
    resourceOptionalItemId: number;
    /** 可选的本产品覆盖价；为空时由后端采用供应商建议对外价。 */
    salePrice?: number;
    suggestedSalePrice?: number;
    supplierOptionalItemId?: number;
  }

  export type SelectedMaterialType = 'introduction' | 'optional_item';

  export interface SelectedMaterial {
    content?: string;
    introductionId?: number;
    materialType: SelectedMaterialType;
    projectName?: string;
    resourceOptionalItemId?: number;
    salePrice?: number;
    sortOrder?: number;
    supplierOptionalItemId?: number;
    title?: string;
  }

  export type SelectedMaterialSaveRequest = SelectedMaterial;

  /** 当天景区组合 Word 方案中的一个素材，顺序即预览顺序。 */
  export interface DayWordPlanMaterial {
    dayResourceId: number;
    material: SelectedMaterial;
    resourceId: number;
    resourceName: string;
  }

  /** 一个景区及其当前可选素材和供应商报价。 */
  export interface DayWordPlanResource {
    dayResource: DayResource;
    resourceDetail: ResourceDetail;
  }

  /** 当天末尾图片的统一选择项，顺序即 Word 输出顺序。 */
  export interface DayEndImageSelection {
    dayResourceId: number;
    imageId: number;
  }

  export interface DayWordPlan {
    dayEndImageSelections?: DayEndImageSelection[];
    dayNo: number;
    imageMode?: 'follow_resource' | 'day_end' | 'hidden';
    productId: number;
    resources: DayWordPlanResource[];
    selectedMaterials: DayWordPlanMaterial[];
  }

  export interface DayWordPlanMaterialSaveRequest extends SelectedMaterialSaveRequest {
    dayResourceId: number;
  }

  export interface DayWordPlanSaveRequest {
    dayEndImageSelections?: DayEndImageSelection[];
    dayNo: number;
    dayResourceIds: number[];
    imageMode?: 'follow_resource' | 'day_end' | 'hidden';
    productId: number;
    selectedImageIdsByResource?: Record<number, number[]>;
    selectedMaterials: DayWordPlanMaterialSaveRequest[];
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

export function saveSalesProductDesignerDayItinerary(data: SalesProductDesignerApi.DayItinerarySaveRequest) {
  return requestClient.post<SalesProductDesignerApi.DayItinerary>('/sales/product/designer/day-itinerary/save', data);
}

export function getSalesProductDesignerDayWordPlan(productId: number, dayNo: number) {
  return requestClient.get<SalesProductDesignerApi.DayWordPlan>('/sales/product/designer/day-word-plan', {
    params: { productId, dayNo },
  });
}

export function saveSalesProductDesignerDayWordPlan(data: SalesProductDesignerApi.DayWordPlanSaveRequest) {
  return requestClient.post<SalesProductDesignerApi.DayWordPlan>(
    '/sales/product/designer/day-word-plan/save', data,
  );
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

/** 读取同一 Word 版本转换出的 PDF，避免前端另行拼装一套排版。 */
export function previewSalesProductDesignerDocument(versionId: number) {
  return requestClient.download<Blob>(`/sales/product/designer/documents/${versionId}/preview`);
}
