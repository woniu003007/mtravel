package com.mtravel.platform.purchase.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 采购资源保存请求。
 *
 * @param resourceType 资源类型，只允许景区、酒店、餐厅、购物、用车、大交通、地接、其它资源
 * @param resourceName 资源名称
 * @param province 资源所在省份
 * @param city 资源所在城市
 * @param district 资源所在区县
 * @param phone 联系电话
 * @param contactName 资源联系人
 * @param fax 传真号码
 * @param address 详细地址
 * @param scenicLevel 景区国家 A 级，仅景区使用
 * @param starLevel 酒店或餐厅星级/档次
 * @param categoryTags 类型标签，餐厅菜系、购物商品类别或其它资源分类
 * @param longitude 高德 GCJ-02 经度
 * @param latitude 高德 GCJ-02 纬度
 * @param businessStatus 营业状态
 * @param openingTime 开始营业时间
 * @param closingTime 结束营业时间
 * @param siteVisitStatus 踩点状态
 * @param lastSiteVisitDate 最近踩点日期
 * @param siteVisitNote 踩点备注
 * @param capacity 最大接待人数或容量
 * @param tableCount 餐桌数量
 * @param mealStandard 团餐标准或资源规格说明
 * @param vehicleType 车辆类型
 * @param seatCount 座位数
 * @param billingMode 用车计费模式
 * @param serviceArea 服务地区或服务范围
 * @param referenceDays 参考天数
 * @param includedItems 包含内容
 * @param excludedItems 不包含内容
 * @param resourceUnit 默认计价单位
 * @param procurementMode 默认采购属性：required需要采购，not_required无需采购
 * @param warmTip 接待、预约或注意事项
 * @param introduction 资源简介
 * @param status 资源启停状态
 * @param autoCreateSupplier 是否自动创建同名供应商并建立采购关系
 * @param remark 资源备注
 */
public record PurchaseResourceSaveRequest(
        @Pattern(regexp = "scenic|hotel|restaurant|shopping|vehicle|traffic|ground_agent|other", message = "资源类型不合法") String resourceType,
        @NotBlank(message = "资源名称不能为空") @Size(max = 200) String resourceName,
        @Size(max = 80) String province,
        @Size(max = 80) String city,
        @Size(max = 80) String district,
        @Size(max = 40) String phone,
        @Size(max = 80) String contactName,
        @Size(max = 40) String fax,
        @Size(max = 300) String address,
        @Pattern(regexp = "unrated|1a|2a|3a|4a|5a", message = "景区等级不合法") String scenicLevel,
        @Pattern(regexp = "unrated|1star|2star|3star|4star|5star", message = "星级不合法") String starLevel,
        @Size(max = 500, message = "类型标签不能超过500个字符") String categoryTags,
        BigDecimal longitude,
        BigDecimal latitude,
        @Pattern(regexp = "unmaintained|open|suspended|closed", message = "营业状态不合法") String businessStatus,
        LocalTime openingTime,
        LocalTime closingTime,
        @Pattern(regexp = "unmaintained|not_visited|visited", message = "踩点状态不合法") String siteVisitStatus,
        LocalDate lastSiteVisitDate,
        @Size(max = 2000, message = "踩点备注不能超过2000个字符") String siteVisitNote,
        @Min(value = 0, message = "接待人数不能小于0") Integer capacity,
        @Min(value = 0, message = "餐桌数量不能小于0") Integer tableCount,
        @Size(max = 2000, message = "团餐标准不能超过2000个字符") String mealStandard,
        @Size(max = 80, message = "车辆类型不能超过80个字符") String vehicleType,
        @Min(value = 0, message = "座位数不能小于0") Integer seatCount,
        @Pattern(regexp = "daily|trip|distance_time", message = "计费模式不合法") String billingMode,
        @Size(max = 200, message = "服务范围不能超过200个字符") String serviceArea,
        @Min(value = 0, message = "参考天数不能小于0") Integer referenceDays,
        @Size(max = 4000, message = "包含内容不能超过4000个字符") String includedItems,
        @Size(max = 4000, message = "不包含内容不能超过4000个字符") String excludedItems,
        @Size(max = 40, message = "计价单位不能超过40个字符") String resourceUnit,
        String warmTip,
        String introduction,
        @Pattern(regexp = "active|disabled", message = "资源状态不合法") String status,
        Boolean autoCreateSupplier,
        String remark,
        @Pattern(regexp = "required|not_required", message = "默认采购属性不合法") String procurementMode
) {}
