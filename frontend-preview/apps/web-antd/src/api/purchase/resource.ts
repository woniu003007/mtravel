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
  export interface ResourceIntroductionItem {
    content: string;
    createdAt?: string;
    createdBy?: string;
    errorMessage?: string;
    id: number;
    indexStatus: 'pending' | 'indexed' | 'failed' | 'deleted';
    indexVersion: number;
    noticeContent?: string;
    publishedAt?: string;
    resourceId: number;
    status: 'draft' | 'published' | 'disabled';
    tags: string[];
    title: string;
    updatedAt?: string;
  }

  export interface ResourceIntroductionSaveParams {
    content: string;
    noticeContent?: string;
    tags?: string[];
    title: string;
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

  export interface ResourceSupplierCreateParams {
    basicInfo?: string;
    city?: string;
    contactName?: string;
    contactPhone?: string;
    district?: string;
    isDefault?: boolean;
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
