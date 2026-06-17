package com.mtravel.platform.sales.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售产品模板保存请求。
 *
 * <p>前端产品编辑页一次保存基本信息、行程内容、产品说明和团队安排参数，避免四个 tab
 * 单独保存时出现模板数据半成品。</p>
 *
 * @param productName 产品名称或线路名称
 * @param businessType 业务类型
 * @param domesticInternational 国内国际标记
 * @param province 接团省份
 * @param city 接团城市
 * @param district 接团区县
 * @param tripType 出团类型
 * @param receptionStandard 接待标准
 * @param productTheme 产品主题
 * @param travelDays 旅游天数
 * @param closeDaysBefore 截止收客天数
 * @param singleRoomDifference 单人房差
 * @param plannedCapacity 预控人数
 * @param status 产品状态
 * @param itineraryDays 每日行程
 * @param bookingNotice 收客须知
 * @param productDescription 产品说明
 * @param feeIncluded 费用包含
 * @param feeExcluded 费用不含
 * @param childPolicy 儿童安排
 * @param shoppingArrangement 购物安排
 * @param optionalItems 自费项目
 * @param giftItems 赠送项目
 * @param attentionItems 注意事项
 * @param warmReminder 温馨提醒
 * @param arrangementItems 团队安排参数
 * @param remark 备注
 */
public record SalesProductSaveRequest(
        @NotBlank(message = "产品名称不能为空") @Size(max = 200) String productName,
        @Size(max = 120) String businessType,
        @Pattern(regexp = "domestic|international", message = "国内国际类型不合法") String domesticInternational,
        @Size(max = 80) String province,
        @Size(max = 80) String city,
        @Size(max = 80) String district,
        @Pattern(regexp = "daily|weekly|irregular", message = "出团类型不合法") String tripType,
        @Size(max = 120) String receptionStandard,
        @Size(max = 120) String productTheme,
        @Min(value = 1, message = "旅游天数不能小于1") Integer travelDays,
        @Min(value = 0, message = "截止收客天数不能小于0") Integer closeDaysBefore,
        @DecimalMin(value = "0", message = "单人房差不能小于0") BigDecimal singleRoomDifference,
        @Min(value = 0, message = "预控人数不能小于0") Integer plannedCapacity,
        @Pattern(regexp = "active|disabled", message = "产品状态不合法") String status,
        List<@Valid SalesProductItineraryDayRequest> itineraryDays,
        String bookingNotice,
        String productDescription,
        String feeIncluded,
        String feeExcluded,
        String childPolicy,
        String shoppingArrangement,
        String optionalItems,
        String giftItems,
        String attentionItems,
        String warmReminder,
        List<@Valid SalesProductArrangementItemRequest> arrangementItems,
        String remark
) {}
