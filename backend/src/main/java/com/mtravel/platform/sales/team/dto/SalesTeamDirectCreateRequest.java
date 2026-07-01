package com.mtravel.platform.sales.team.dto;

import com.mtravel.platform.sales.product.dto.SalesProductItineraryDayRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 团队管理页直接创建团队请求。
 *
 * <p>对应老系统团队管理里的散拼、整团、散团新增入口。页面看起来像产品基础信息，
 * 但保存结果必须同时形成一个可被正式团队引用的产品快照和一条销售团队记录。</p>
 *
 * @param teamType 团队类型，散拼、整团或散团
 * @param teamName 团队名称，作为产品快照名称和团队列表名称来源
 * @param businessType 业务类型快照
 * @param domesticInternational 国内国际标记
 * @param province 接团省份
 * @param city 接团城市
 * @param district 接团区县
 * @param departureDate 发团日期
 * @param tripType 出团类型
 * @param receptionStandard 接待标准
 * @param productTheme 产品主题
 * @param travelDays 旅游天数
 * @param closeDaysBefore 截止收客天数
 * @param singleRoomDifference 单人房差
 * @param totalSeats 预控人数
 * @param itineraryDays 行程内容页签中的每日行程
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
 * @param remark 操作备注
 */
public record SalesTeamDirectCreateRequest(
        @NotBlank(message = "团队类型不能为空")
        @Pattern(regexp = "sanpin|zhengtuan|santuan", message = "团队类型不合法")
        String teamType,
        @NotBlank(message = "团队名称不能为空") @Size(max = 200) String teamName,
        @Size(max = 120) String businessType,
        @Pattern(regexp = "domestic|international", message = "国内国际类型不合法") String domesticInternational,
        @Size(max = 80) String province,
        @Size(max = 80) String city,
        @Size(max = 80) String district,
        @NotNull(message = "发团日期不能为空") LocalDate departureDate,
        @Pattern(regexp = "daily|weekly|irregular", message = "出团类型不合法") String tripType,
        @Size(max = 120) String receptionStandard,
        @Size(max = 120) String productTheme,
        @Min(value = 1, message = "旅游天数不能小于1") Integer travelDays,
        @Min(value = 0, message = "截止收客天数不能小于0") Integer closeDaysBefore,
        @DecimalMin(value = "0", message = "单房差不能小于0") BigDecimal singleRoomDifference,
        @Min(value = 0, message = "预控人数不能小于0") Integer totalSeats,
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
        @Size(max = 500) String remark
) {}
