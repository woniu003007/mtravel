package com.mtravel.platform.purchase.resource.dto;

/**
 * 景区资源页快捷新增供应商返回对象。
 *
 * @param supplierId 供应商 ID
 * @param relationId 采购关系 ID
 */
public record ScenicSupplierCreateResponse(Long supplierId, Long relationId) {}
