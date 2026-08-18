package com.mtravel.platform.sales.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "SalesProductSaveRequest", description = "销售产品模板保存请求")
public record SalesProductSaveRequest(
        @Schema(description = "产品名称或线路名称", example = "华东五市四日游", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "产品名称不能为空") @Size(max = 200)
        String productName,

        @Schema(description = "业务类型，按企业产品字典或页面选项传值", example = "domestic")
        @Size(max = 120)
        String businessType,

        @Schema(description = "国内国际标记：domestic 国内，international 国际", example = "domestic", allowableValues = {"domestic", "international"})
        @Pattern(regexp = "domestic|international", message = "国内国际类型不合法")
        String domesticInternational,

        @Schema(description = "接团省份", example = "江苏省")
        @Size(max = 80)
        String province,

        @Schema(description = "接团城市", example = "南京市")
        @Size(max = 80)
        String city,

        @Schema(description = "接团区县", example = "秦淮区")
        @Size(max = 80)
        String district,

        @Schema(description = "出团类型：daily 天天发，weekly 周期发，irregular 不定期", example = "daily", allowableValues = {"daily", "weekly", "irregular"})
        @Pattern(regexp = "daily|weekly|irregular", message = "出团类型不合法")
        String tripType,

        @Schema(description = "接待标准，例如舒适型、品质型、豪华型", example = "舒适型")
        @Size(max = 120)
        String receptionStandard,

        @Schema(description = "产品主题或线路主题", example = "华东常规散拼")
        @Size(max = 120)
        String productTheme,

        @Schema(description = "旅游天数", example = "4", minimum = "1")
        @Min(value = 1, message = "旅游天数不能小于1")
        Integer travelDays,

        @Schema(description = "截止收客天数，表示出团前几天停止收客", example = "2", minimum = "0")
        @Min(value = 0, message = "截止收客天数不能小于0")
        Integer closeDaysBefore,

        @Schema(description = "单人房差金额", example = "300.00", minimum = "0")
        @DecimalMin(value = "0", message = "单人房差不能小于0")
        BigDecimal singleRoomDifference,

        @Schema(description = "预控人数或计划收客上限", example = "30", minimum = "0")
        @Min(value = 0, message = "预控人数不能小于0")
        Integer plannedCapacity,

        @Schema(description = "产品状态：active 启用，disabled 停用", example = "active", allowableValues = {"active", "disabled"})
        @Pattern(regexp = "active|disabled", message = "产品状态不合法")
        String status,

        @Schema(description = "每日行程列表；创建接口示例只展示 1 天，实际可按 travelDays 传多天")
        List<@Valid SalesProductItineraryDayRequest> itineraryDays,

        @Schema(description = "收客须知", example = "请提前提供游客姓名、手机号和证件信息。")
        String bookingNotice,

        @Schema(description = "产品说明", example = "适合常规华东散拼接待的四日游产品模板。")
        String productDescription,

        @Schema(description = "费用包含", example = "住宿、用车、导游服务、行程内首道门票。")
        String feeIncluded,

        @Schema(description = "费用不含", example = "单房差、个人消费、自费项目。")
        String feeExcluded,

        @Schema(description = "儿童安排", example = "儿童默认不占床，门票按景区政策现询。")
        String childPolicy,

        @Schema(description = "购物安排", example = "全程无强制购物。")
        String shoppingArrangement,

        @Schema(description = "自费项目", example = "夜游项目自愿参加。")
        String optionalItems,

        @Schema(description = "赠送项目", example = "每人每天一瓶矿泉水。")
        String giftItems,

        @Schema(description = "注意事项", example = "请游客携带有效身份证件。")
        String attentionItems,

        @Schema(description = "温馨提醒", example = "旺季酒店和用车以最终确认为准。")
        String warmReminder,

        @Schema(description = "团队安排参数。字段较多，创建产品时可为空；需要预设住宿、用车、门票、用餐等安排时再传。")
        List<@Valid SalesProductArrangementItemRequest> arrangementItems,

        @Schema(description = "内部备注", example = "团队安排后续由计调补充。")
        String remark
) {}
