import { requestClient } from '#/api/request';

export namespace PurchaseResourceApi {
  export type BusinessStatus = 'closed' | 'open' | 'suspended' | 'unmaintained';
  export type ResourceType =
    | 'ground_agent'
    | 'hotel'
    | 'other'
    | 'restaurant'
    | 'scenic'
    | 'shopping'
    | 'traffic'
    | 'vehicle';
  export type ProcurementMode = 'not_required' | 'required';
  export type ScenicLevel = '1a' | '2a' | '3a' | '4a' | '5a' | 'unrated';
  export type SiteVisitStatus = 'not_visited' | 'unmaintained' | 'visited';
  export type StarLevel = '1star' | '2star' | '3star' | '4star' | '5star' | 'unrated';
  export type Status = 'active' | 'disabled';

  export interface Item {
    address?: string;
    boundSupplierCount: number;
    businessStatus?: BusinessStatus;
    city?: string;
    closingTime?: string;
    categoryTags?: string;
    capacity?: number;
    contactName?: string;
    createdAt?: string;
    createdBy?: string;
    district?: string;
    fax?: string;
    id: number;
    introduction?: string;
    lastSiteVisitDate?: string;
    latitude?: number;
    longitude?: number;
    mealStandard?: string;
    openingTime?: string;
    phone?: string;
    procurementMode: ProcurementMode;
    province?: string;
    remark?: string;
    resourceName: string;
    resourceType: ResourceType;
    billingMode?: 'daily' | 'distance_time' | 'trip';
    excludedItems?: string;
    includedItems?: string;
    referenceDays?: number;
    resourceUnit?: string;
    scenicLevel?: ScenicLevel;
    seatCount?: number;
    serviceArea?: string;
    siteVisitNote?: string;
    siteVisitStatus?: SiteVisitStatus;
    starLevel?: StarLevel;
    status: Status;
    tableCount?: number;
    updatedAt?: string;
    vehicleType?: string;
    warmTip?: string;
  }

  export interface Binding {
    createdAt?: string;
    relationId: number;
    groupQuantity: number;
    isDefault: boolean;
    status: 'active' | 'disabled' | 'expired';
    supplierId: number;
    supplierName?: string;
    priceMode?: 'classified' | 'unified';
    unifiedPrice?: number;
    priceLines?: ResourceSupplierPriceLine[];
    optionalItems?: ResourceSupplierOptionalItemResponse[];
    priceRemark?: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface QueryParams {
    businessStatus?: BusinessStatus;
    city?: string;
    district?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
    province?: string;
    procurementMode?: ProcurementMode;
    resourceType?: ResourceType;
    scenicLevel?: ScenicLevel;
    siteVisitStatus?: SiteVisitStatus;
    status?: Status;
  }

  export interface SaveParams {
    address?: string;
    /** 新增资源时是否同时生成一个同名、无报价的默认供应商。 */
    autoCreateSupplier?: boolean;
    businessStatus?: BusinessStatus;
    city?: string;
    closingTime?: string;
    categoryTags?: string;
    capacity?: number;
    contactName?: string;
    district?: string;
    fax?: string;
    introduction?: string;
    lastSiteVisitDate?: string;
    latitude?: number;
    longitude?: number;
    mealStandard?: string;
    openingTime?: string;
    phone?: string;
    province?: string;
    procurementMode?: ProcurementMode;
    remark?: string;
    resourceName: string;
    resourceType?: ResourceType;
    billingMode?: 'daily' | 'distance_time' | 'trip';
    excludedItems?: string;
    includedItems?: string;
    referenceDays?: number;
    resourceUnit?: string;
    scenicLevel?: ScenicLevel;
    seatCount?: number;
    serviceArea?: string;
    siteVisitNote?: string;
    siteVisitStatus?: SiteVisitStatus;
    starLevel?: StarLevel;
    status?: Status;
    tableCount?: number;
    vehicleType?: string;
    warmTip?: string;
  }

  export interface AmapTip {
    address?: string;
    district?: string;
    latitude?: string;
    longitude?: string;
    name: string;
  }

  export interface AmapJsConfig {
    key: string;
    securityJsCode?: string;
  }

  export interface AmapRegeoResult {
    address?: string;
    province?: string;
    city?: string;
    district?: string;
  }

  export interface ResourceDocumentItem {
    attachmentId: number;
    createdAt?: string;
    createdBy?: string;
    downloadUrl: string;
    errorMessage?: string;
    fileExt?: string;
    fileSha256?: string;
    fileSize: number;
    id: number;
    indexStatus: 'pending' | 'indexed' | 'failed' | 'deleted';
    indexVersion: number;
    originalFilename: string;
    processedAt?: string;
    publishedAt?: string;
    processingStatus: 'pending' | 'processing' | 'succeeded' | 'failed' | 'deleted';
    remark?: string;
    reviewStatus: 'draft' | 'published' | 'disabled';
    sourceId: number;
    status: Status;
    updatedAt?: string;
    usageProductManual: boolean;
    usageQa: boolean;
  }

  /** 可供产品文案引用的资源介绍版本。 */
  export type ResourceIntroductionExtensionBlockType =
    | 'generic'
    | 'photo_recommendation'
    | 'warm_tip';

  /** 介绍正文后的可排序内容模块，例如拍照机位或温馨提示。 */
  export interface ResourceIntroductionExtensionBlock {
    content?: string;
    contentMode: 'items' | 'multiline';
    items: string[];
    title: string;
    titleColor: string;
    type: ResourceIntroductionExtensionBlockType;
  }

  export interface ResourceIntroductionItem {
    content: string;
    createdAt?: string;
    createdBy?: string;
    errorMessage?: string;
    extensionBlocks?: ResourceIntroductionExtensionBlock[];
    id: number;
    indexStatus: 'pending' | 'indexed' | 'failed' | 'deleted';
    indexVersion: number;
    isOptionalItem: boolean;
    /** 自费介绍关联的资源级自费项目；常规介绍为空。 */
    resourceOptionalItemId?: number;
    /** 关联项目名称，仅供编辑回显。 */
    resourceOptionalItemName?: string;
    noticeContent?: string;
    publishedAt?: string;
    resourceId: number;
    /** 当前资源内的介绍素材默认输出顺序，数值越小越靠前。 */
    sortOrder: number;
    status: 'draft' | 'published' | 'disabled';
    tags: string[];
    title: string;
    updatedAt?: string;
    /** 游览时长分钟数，前端预览自动转换为小时/分钟。 */
    visitDuration?: string;
    warmTipContent?: string;
  }

  export interface ResourceIntroductionSaveParams {
    content: string;
    extensionBlocks?: ResourceIntroductionExtensionBlock[];
    isOptionalItem?: boolean;
    resourceOptionalItemId?: number;
    noticeContent?: string;
    tags?: string[];
    title: string;
    /** 游览时长分钟数字符串，留空表示不设置。 */
    visitDuration?: string;
    warmTipContent?: string;
  }

  /** 整体保存当前资源下介绍素材的默认输出顺序。 */
  export interface ResourceIntroductionReorderParams {
    introductionIds: number[];
  }

  export interface ResourceIntroductionImageSaveParams {
    imageIds: number[];
  }

  /** 资源主档下可复用的自费项目，不包含任何供应商成本或对外售价。 */
  export interface ResourceOptionalItem {
    id: number;
    optionalItemType: 'recommended_self_pay' | 'scenic_transport';
    projectName: string;
    resourceId: number;
    status: 'active' | 'disabled';
  }

  export interface ResourceOptionalItemSaveParams {
    optionalItemType: 'recommended_self_pay' | 'scenic_transport';
    projectName: string;
    status?: 'active' | 'disabled';
  }

  /** 资源主档的产品配图，不参与知识库向量化。 */
  export interface ResourceImageItem {
    attachmentId: number;
    createdAt?: string;
    createdBy?: string;
    downloadUrl: string;
    fileExt: string;
    fileSize: number;
    id: number;
    isCover: boolean;
    originalFilename: string;
    resourceId: number;
    sortOrder: number;
    status: Status;
    tags: string[];
    updatedAt?: string;
  }

  export interface ResourceImageUpdateParams {
    sortOrder?: number;
    tags?: string[];
  }

  export interface ScenicSupplierCreateParams {
    adultPrice?: number;
    basicInfo?: string;
    childPrice?: number;
    contactName?: string;
    contactPhone?: string;
    city?: string;
    district?: string;
    isDefault?: boolean;
    optionalItems?: ResourceSupplierOptionalItemParams[];
    preferentialPrice?: number;
    priceMode: 'classified' | 'unified';
    priceRemark?: string;
    province?: string;
    remark?: string;
    seniorPrice?: number;
    status?: 'active' | 'blacklisted' | 'disabled';
    studentPrice?: number;
    supplierName: string;
    unifiedPrice?: number;
  }

  export interface ResourceSupplierPriceLine {
    priceDescription?: string;
    projectName: string;
    resourceProjectId: number;
    teamPrice?: number;
  }

  export interface ResourceSupplierPriceLineParams {
    priceDescription?: string;
    resourceProjectId: number;
    teamPrice?: number;
  }

  /** 供应商针对景区自费项目的成本报价，单位固定为元/人。 */
  export interface ResourceSupplierOptionalItemParams {
    costPrice?: number;
    priceDescription?: string;
    projectName: string;
    resourceOptionalItemId?: number;
    /** 产品设计时的默认对外售价；最终售价仍由具体产品保存。 */
    suggestedSalePrice?: number;
    status?: 'active' | 'disabled';
  }

  /** 资源供应商绑定回显的自费项目报价。 */
  export interface ResourceSupplierOptionalItemResponse {
    costPrice: number;
    id: number;
    priceDescription?: string;
    projectName: string;
    priceUnit: 'yuan_per_person';
    resourceOptionalItemId?: number;
    suggestedSalePrice?: number;
    status?: 'active' | 'disabled';
  }

  export interface ResourceSupplierCreateParams {
    basicInfo?: string;
    city?: string;
    contactName?: string;
    contactPhone?: string;
    district?: string;
    isDefault?: boolean;
    optionalItems?: ResourceSupplierOptionalItemParams[];
    priceLines?: ResourceSupplierPriceLineParams[];
    priceMode: 'classified' | 'unified';
    priceRemark?: string;
    province?: string;
    remark?: string;
    status?: 'active' | 'blacklisted' | 'disabled';
    supplierName: string;
    unifiedPrice?: number;
  }

  export type ResourceSupplierUpdateParams = ResourceSupplierCreateParams;

  export interface ScenicSupplierCreateResult {
    relationId: number;
    supplierId: number;
  }
}

export function getPurchaseResourcePage(
  params: PurchaseResourceApi.QueryParams,
) {
  return requestClient.get<
    PurchaseResourceApi.PageResult<PurchaseResourceApi.Item>
  >('/purchase/resource/page', { params });
}

export function getPurchaseResourceDetail(id: number) {
  return requestClient.get<PurchaseResourceApi.Item>(
    '/purchase/resource/detail',
    { params: { id } },
  );
}

export function getPurchaseResourceBindings(resourceId: number) {
  return requestClient.get<PurchaseResourceApi.Binding[]>(
    '/purchase/resource/bindings',
    { params: { resourceId } },
  );
}

export function createScenicSupplierForResource(
  resourceId: number,
  data: PurchaseResourceApi.ScenicSupplierCreateParams,
) {
  return requestClient.post<PurchaseResourceApi.ScenicSupplierCreateResult>(
    `/purchase/resource/${resourceId}/scenic-suppliers/create`,
    data,
  );
}

export function createResourceSupplierForResource(
  resourceId: number,
  data: PurchaseResourceApi.ResourceSupplierCreateParams,
) {
  return requestClient.post<PurchaseResourceApi.ScenicSupplierCreateResult>(
    `/purchase/resource/${resourceId}/suppliers/create`,
    data,
  );
}

export function updateResourceSupplierForResource(
  resourceId: number,
  relationId: number,
  data: PurchaseResourceApi.ResourceSupplierUpdateParams,
) {
  return requestClient.post<PurchaseResourceApi.ScenicSupplierCreateResult>(
    `/purchase/resource/${resourceId}/suppliers/${relationId}/update`,
    data,
  );
}

export function createPurchaseResource(data: PurchaseResourceApi.SaveParams) {
  return requestClient.post<PurchaseResourceApi.Item>(
    '/purchase/resource/create',
    data,
  );
}

export function updatePurchaseResource(
  id: number,
  data: PurchaseResourceApi.SaveParams,
) {
  return requestClient.post<PurchaseResourceApi.Item>(
    '/purchase/resource/update',
    data,
    { params: { id } },
  );
}

export function deletePurchaseResource(id: number) {
  return requestClient.post<void>(
    '/purchase/resource/delete',
    {},
    { params: { id } },
  );
}

export function getPurchaseResourceDocuments(resourceId: number) {
  return requestClient.get<PurchaseResourceApi.ResourceDocumentItem[]>(
    `/purchase/resource/${resourceId}/documents`,
  );
}

export function uploadPurchaseResourceDocuments(
  resourceId: number,
  files: File[],
) {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });
  return requestClient.post<PurchaseResourceApi.ResourceDocumentItem[]>(
    `/purchase/resource/${resourceId}/documents/upload`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}

export function downloadPurchaseResourceDocument(
  resourceId: number,
  documentId: number,
) {
  return requestClient.download<Blob>(
    `/purchase/resource/${resourceId}/documents/${documentId}/download`,
  );
}

export function deletePurchaseResourceDocument(
  resourceId: number,
  documentId: number,
) {
  return requestClient.post<void>(
    `/purchase/resource/${resourceId}/documents/${documentId}/delete`,
    {},
  );
}

export function retryPurchaseResourceDocument(
  resourceId: number,
  documentId: number,
) {
  return requestClient.post<PurchaseResourceApi.ResourceDocumentItem>(
    `/purchase/resource/${resourceId}/documents/${documentId}/retry`,
    {},
  );
}

export function publishPurchaseResourceDocument(
  resourceId: number,
  documentId: number,
) {
  return requestClient.post<PurchaseResourceApi.ResourceDocumentItem>(
    `/purchase/resource/${resourceId}/documents/${documentId}/publish`,
    {},
  );
}

export function disablePurchaseResourceDocument(
  resourceId: number,
  documentId: number,
) {
  return requestClient.post<PurchaseResourceApi.ResourceDocumentItem>(
    `/purchase/resource/${resourceId}/documents/${documentId}/disable`,
    {},
  );
}

export function getPurchaseResourceIntroductions(resourceId: number) {
  return requestClient.get<PurchaseResourceApi.ResourceIntroductionItem[]>(
    `/purchase/resource/${resourceId}/materials/introductions`,
    { headers: { 'X-Suppress-Error-Message': 'true' } },
  );
}

/** 保存资源介绍素材的默认输出顺序。 */
export function reorderPurchaseResourceIntroductions(
  resourceId: number,
  data: PurchaseResourceApi.ResourceIntroductionReorderParams,
) {
  return requestClient.post<PurchaseResourceApi.ResourceIntroductionItem[]>(
    `/purchase/resource/${resourceId}/materials/introductions/reorder`,
    data,
    { headers: { 'X-Suppress-Error-Message': 'true' } },
  );
}

/** 获取当前资源可供供应商报价和介绍素材复用的自费项目。 */
export function getPurchaseResourceOptionalItems(resourceId: number) {
  return requestClient.get<PurchaseResourceApi.ResourceOptionalItem[]>(
    `/purchase/resource/${resourceId}/optional-items`,
  );
}

/** 快捷新建当前资源的自费项目主档。 */
export function createPurchaseResourceOptionalItem(
  resourceId: number,
  data: PurchaseResourceApi.ResourceOptionalItemSaveParams,
) {
  return requestClient.post<PurchaseResourceApi.ResourceOptionalItem>(
    `/purchase/resource/${resourceId}/optional-items`,
    data,
  );
}

export function createPurchaseResourceIntroduction(
  resourceId: number,
  data: PurchaseResourceApi.ResourceIntroductionSaveParams,
) {
  return requestClient.post<PurchaseResourceApi.ResourceIntroductionItem>(
    `/purchase/resource/${resourceId}/materials/introductions`,
    data,
  );
}

export function updatePurchaseResourceIntroduction(
  resourceId: number,
  introductionId: number,
  data: PurchaseResourceApi.ResourceIntroductionSaveParams,
) {
  return requestClient.post<PurchaseResourceApi.ResourceIntroductionItem>(
    `/purchase/resource/${resourceId}/materials/introductions/${introductionId}`,
    data,
  );
}

export function publishPurchaseResourceIntroduction(
  resourceId: number,
  introductionId: number,
) {
  return requestClient.post<PurchaseResourceApi.ResourceIntroductionItem>(
    `/purchase/resource/${resourceId}/materials/introductions/${introductionId}/publish`,
    {},
  );
}

export function retryPurchaseResourceIntroduction(
  resourceId: number,
  introductionId: number,
) {
  return requestClient.post<PurchaseResourceApi.ResourceIntroductionItem>(
    `/purchase/resource/${resourceId}/materials/introductions/${introductionId}/retry`,
    {},
  );
}

export function deletePurchaseResourceIntroduction(
  resourceId: number,
  introductionId: number,
) {
  return requestClient.post<void>(
    `/purchase/resource/${resourceId}/materials/introductions/${introductionId}/delete`,
    {},
  );
}

/** 查询一份介绍素材从当前资源图片素材库选用的图片。 */
export function getPurchaseResourceIntroductionImages(
  resourceId: number,
  introductionId: number,
) {
  return requestClient.get<number[]>(
    `/purchase/resource/${resourceId}/materials/introductions/${introductionId}/images`,
  );
}

/** 整体保存一份介绍素材选用的图片和顺序；空数组表示清空。 */
export function savePurchaseResourceIntroductionImages(
  resourceId: number,
  introductionId: number,
  data: PurchaseResourceApi.ResourceIntroductionImageSaveParams,
) {
  return requestClient.post<number[]>(
    `/purchase/resource/${resourceId}/materials/introductions/${introductionId}/images`,
    data,
  );
}

export function getPurchaseResourceImages(resourceId: number) {
  return requestClient.get<PurchaseResourceApi.ResourceImageItem[]>(
    `/purchase/resource/${resourceId}/materials/images`,
    { headers: { 'X-Suppress-Error-Message': 'true' } },
  );
}

export function uploadPurchaseResourceImages(
  resourceId: number,
  files: File[],
) {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });
  return requestClient.post<PurchaseResourceApi.ResourceImageItem[]>(
    `/purchase/resource/${resourceId}/materials/images/upload`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
}

export function updatePurchaseResourceImage(
  resourceId: number,
  imageId: number,
  data: PurchaseResourceApi.ResourceImageUpdateParams,
) {
  return requestClient.post<PurchaseResourceApi.ResourceImageItem>(
    `/purchase/resource/${resourceId}/materials/images/${imageId}`,
    data,
  );
}

export function setPurchaseResourceImageCover(
  resourceId: number,
  imageId: number,
) {
  return requestClient.post<PurchaseResourceApi.ResourceImageItem>(
    `/purchase/resource/${resourceId}/materials/images/${imageId}/cover`,
    {},
  );
}

export function deletePurchaseResourceImage(
  resourceId: number,
  imageId: number,
) {
  return requestClient.post<void>(
    `/purchase/resource/${resourceId}/materials/images/${imageId}/delete`,
    {},
  );
}

export function downloadPurchaseResourceImage(
  resourceId: number,
  imageId: number,
) {
  return requestClient.download<Blob>(
    `/purchase/resource/${resourceId}/materials/images/${imageId}/download`,
    { headers: { 'X-Suppress-Error-Message': 'true' } },
  );
}

export function searchCommonAmapTips(params: {
  city?: string;
  keywords: string;
}) {
  return requestClient.get<PurchaseResourceApi.AmapTip[]>(
    '/common/map/amap/tips',
    {
      params,
    },
  );
}

export function getCommonAmapJsConfig() {
  return requestClient.get<PurchaseResourceApi.AmapJsConfig>(
    '/common/map/amap/js-config',
  );
}

export function reverseGeocodeCommonAmap(params: {
  latitude: number;
  longitude: number;
}) {
  return requestClient.get<PurchaseResourceApi.AmapRegeoResult>(
    '/common/map/amap/regeo',
    { params },
  );
}
