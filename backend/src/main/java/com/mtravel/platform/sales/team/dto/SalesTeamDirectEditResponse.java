package com.mtravel.platform.sales.team.dto;

import com.mtravel.platform.sales.product.dto.SalesProductItineraryDayResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 团队直接编辑页详情返回对象。
 *
 * <p>对应老系统“修改团队”跳回添加/修改产品团队页面的回显数据。团队本身仍以 sales_teams 为主，
 * 线路名称、行程和产品说明来自团队关联的产品快照。</p>
 *
 * @param id 团队 ID
 * @param productId 团队关联的产品快照 ID
 * @param teamNo 团号，编辑时不重新生成
 * @param teamType 团队类型
 * @param teamName 页面展示的团队名称，不包含为产品快照唯一性追加的团号后缀
 * @param businessType 业务类型
 * @param domesticInternational 国内国际标记
 * @param province 接团省份
 * @param city 接团城市
 * @param district 接团区县
 * @param departureDate 出团日期
 * @param tripType 出团类型
 * @param receptionStandard 接待标准
 * @param productTheme 产品主题
 * @param travelDays 旅游天数
 * @param closeDaysBefore 截止收客天数
 * @param singleRoomDifference 单人房差
 * @param totalSeats 预控人数
 * @param itineraryDays 每日行程和路书点位
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
 * @param remark 团队备注
 */
public record SalesTeamDirectEditResponse(
        Long id,
        Long productId,
        String teamNo,
        String teamType,
        String teamName,
        String businessType,
        String domesticInternational,
        String province,
        String city,
        String district,
        LocalDate departureDate,
        String tripType,
        String receptionStandard,
        String productTheme,
        Integer travelDays,
        Integer closeDaysBefore,
        BigDecimal singleRoomDifference,
        Integer totalSeats,
        List<SalesProductItineraryDayResponse> itineraryDays,
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
        String remark
) {}
