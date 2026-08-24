import { requestClient } from '#/api/request';

export namespace SalesProductDesignerApi {
  export type ProcurementMode = 'not_required' | 'required';
  export type ArrangementRole = 'accommodation' | 'breakfast' | 'dinner' | 'ground_service' | 'itinerary' | 'lunch' | 'unassigned';
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

  /** 产品 Word 方案专用的自费候选，已包含介绍关联和可用建议价。 */
  export interface WordOptionalItemCandidate {
    introductionId: number;
    introductionTitle: string;
    optionalItemType: OptionalItemType;
    projectName: string;
    resourceOptionalItemId: number;
    suggestedSalePrice?: number;
    supplierOptionalItemId?: number;
  }

  export interface Supplier {
    isDefault: boolean;
    priceLines: SupplierPriceLine[];
    priceMode?: 'classified' | 'pending' | 'unified';
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
    wordOptionalItemCandidates?: WordOptionalItemCandidate[];
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
    supplierRelationId?: number;
    supplierName?: string;
    unitPrice: number;
  }

  export interface DayPlan {
    destinationCity?: string;
    destinationDistrict?: string;
    destinationProvince?: string;
    accommodationCity?: string;
    breakfastIncluded?: boolean;
    dayCostAmount: number;
    dayNo: number;
    dinnerIncluded?: boolean;
    lunchIncluded?: boolean;
    mealPlan?: BreakfastPlan;
    resources: DayResource[];
  }

  /** 早餐来源由后端按前夜住宿和当日外部餐厅统一计算。 */
  export interface BreakfastPlan {
    hotelSources: BreakfastHotel[];
    restaurant?: DayResource;
    source: 'hotel' | 'none' | 'restaurant';
  }

  export interface BreakfastHotel {
    dayResourceId: number;
    resourceId: number;
    resourceName: string;
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

  /** 当晚住宿城市快照；优先跟随已安排酒店，并作为当天地图默认范围。 */
  export interface DayDestination {
    dayNo: number;
    destinationCity: string;
    destinationDistrict?: string;
    destinationProvince?: string;
  }

  export interface DayDestinationSaveRequest {
    dayNo: number;
    destinationCity: string;
    destinationDistrict?: string;
    destinationProvince?: string;
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
    /** 每日地图资源冻结的成本合计，不含产品级全程用车。 */
    dayResourceCostAmount?: number;
    days: DayPlan[];
    productId: number;
    productName: string;
    province?: string;
    status: string;
    totalCostAmount: number;
    travelDays: number;
    /** 产品级全程用车冻结的成本合计。 */
    vehicleCostAmount?: number;
    vehicleArrangements?: VehicleArrangement[];
  }

  /** 不进入每日地图的产品级全程用车资源摘要。 */
  export interface VehicleResource {
    billingMode?: string;
    city?: string;
    id: number;
    procurementMode: ProcurementMode;
    province?: string;
    resourceName: string;
    seatCount?: number;
    vehicleType?: string;
  }

  /** 已冻结资源、供应商与报价的产品级用车安排。 */
  export interface VehicleArrangement {
    costAmount: number;
    endDayNo?: number;
    id: number;
    priceMode?: string;
    procurementStatus: 'not_required' | 'pending' | 'quoted';
    productId: number;
    quantity: number;
    remark?: string;
    resourceId?: number;
    resourceName: string;
    sortOrder: number;
    startDayNo?: number;
    supplierId?: number;
    supplierName?: string;
    supplierRelationId?: number;
    unitPrice: number;
    vehicleType: string;
  }

  export interface VehicleArrangementSaveRequest {
    endDayNo?: number;
    id?: number;
    productId: number;
    quantity?: number;
    remark?: string;
    resourceId?: number;
    sortOrder?: number;
    startDayNo?: number;
    supplierRelationId?: number;
    vehicleType?: string;
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
    supplierRelationId?: number;
    /** 早餐来源冲突时，同一后端事务内替换另一来源。 */
    replaceBreakfastSource?: boolean;
  }

  export interface DayResourceSupplierSaveRequest {
    dayResourceId: number;
    productId: number;
    supplierRelationId: number;
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
    /** 资源主档软删除后仍保留产品行程快照，此时不可再选择新的素材或图片。 */
    resourceDetail: null | ResourceDetail;
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

/** 查询产品级全程用车候选；该资源刻意不进入每天的地图资源池。 */
export function getSalesProductDesignerVehicleResources(params: {
  keyword?: string;
  page?: number;
  pageSize?: number;
}) {
  return requestClient.get<SalesProductDesignerApi.PageResult<SalesProductDesignerApi.VehicleResource>>(
    '/sales/product/designer/vehicle-resources', { params },
  );
}

export function saveSalesProductDesignerVehicleArrangement(
  data: SalesProductDesignerApi.VehicleArrangementSaveRequest,
) {
  return requestClient.post<SalesProductDesignerApi.VehicleArrangement>(
    '/sales/product/designer/vehicle-arrangement/save', data,
  );
}

export function deleteSalesProductDesignerVehicleArrangement(data: {
  productId: number;
  vehicleArrangementId: number;
}) {
  return requestClient.post<void>('/sales/product/designer/vehicle-arrangement/delete', data);
}

export function reorderSalesProductDesignerVehicleArrangements(data: {
  productId: number;
  vehicleArrangementIds: number[];
}) {
  return requestClient.post<void>('/sales/product/designer/vehicle-arrangement/reorder', data);
}

export function saveSalesProductDesignerDayResource(data: SalesProductDesignerApi.DayResourceSaveRequest) {
  return requestClient.post<SalesProductDesignerApi.DayResource>('/sales/product/designer/day-resource/save', data);
}

export function saveSalesProductDesignerDayItinerary(data: SalesProductDesignerApi.DayItinerarySaveRequest) {
  return requestClient.post<SalesProductDesignerApi.DayItinerary>('/sales/product/designer/day-itinerary/save', data);
}

export function saveSalesProductDesignerDayDestination(data: SalesProductDesignerApi.DayDestinationSaveRequest) {
  return requestClient.post<SalesProductDesignerApi.DayDestination>('/sales/product/designer/day-destination/save', data);
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

/** 仅更新已编排行资源的供应商/报价快照，不改动其它资源快照。 */
export function changeSalesProductDesignerDayResourceSupplier(
  data: SalesProductDesignerApi.DayResourceSupplierSaveRequest,
) {
  return requestClient.post<SalesProductDesignerApi.DayResource>(
    '/sales/product/designer/day-resource/supplier', data,
  );
}

export function reorderSalesProductDesignerDayResources(data: {
  arrangementRole: 'accommodation' | 'ground_service' | 'itinerary';
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
