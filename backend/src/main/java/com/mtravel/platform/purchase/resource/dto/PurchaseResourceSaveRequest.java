package com.mtravel.platform.purchase.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 采购资源保存请求。
 *
 * @param resourceType 资源类型，只允许景区、酒店、餐厅、购物四类
 * @param resourceName 资源名称
 * @param province 资源所在省份
 * @param city 资源所在城市
 * @param district 资源所在区县
 * @param phone 联系电话
 * @param fax 传真号码
 * @param address 详细地址
 * @param warmTip 接待、预约或注意事项
 * @param introduction 资源简介
 * @param status 资源启停状态
 * @param autoCreateSupplier 是否自动创建同名供应商并建立采购关系
 * @param remark 资源备注
 */
public record PurchaseResourceSaveRequest(
        @Pattern(regexp = "scenic|hotel|restaurant|shopping", message = "资源类型不合法") String resourceType,
        @NotBlank(message = "资源名称不能为空") @Size(max = 200) String resourceName,
        @Size(max = 80) String province,
        @Size(max = 80) String city,
        @Size(max = 80) String district,
        @Size(max = 40) String phone,
        @Size(max = 40) String fax,
        @Size(max = 300) String address,
        String warmTip,
        String introduction,
        @Pattern(regexp = "active|disabled", message = "资源状态不合法") String status,
        Boolean autoCreateSupplier,
        String remark
) {}
