package com.mtravel.platform.sales.booking.aiimport.dto;

import java.util.List;

/**
 * 确认单 AI 辅助录入结果。
 *
 * <p>本对象是“可编辑草稿”，前端展示后由用户人工确认并填入表单。后端不得把该结果直接写入
 * 正式订单、游客或费用表。</p>
 */
public record BookingAiImportResponse(
        String sourceType,
        double confidence,
        List<String> warnings,
        TravelInfo travelInfo,
        GuideInfo guideInfo,
        CustomerInfo customerInfo,
        PriceInfo priceInfo,
        AdditionalInfo additionalInfo,
        List<GuestInfo> guests,
        ModuleScores moduleScores,
        GuestSummary guestSummary,
        List<String> evidence
) {

    /** 行程说明草稿，对应老系统收客页来程、返程和参团时间字段。 */
    public record TravelInfo(
            String joinDate,
            String outboundOriginCity,
            String outboundArrivalCity,
            String outboundStationName,
            String outboundTrafficNo,
            String outboundDepartureTime,
            String outboundArrivalTime,
            String returnStationName,
            String returnDepartureCity,
            String returnDestinationCity,
            String returnTrafficNo,
            String returnDepartureTime,
            String returnArrivalTime,
            List<String> warnings
    ) {
    }

    /** 导游相关草稿，作为导游、全陪、接待要求等人工录入线索。 */
    public record GuideInfo(
            String guideName,
            String guidePhone,
            String escortName,
            String receptionRequirement,
            List<String> warnings
    ) {
    }

    /** 客户信息草稿，辅助销售选择客户单位和联系人。 */
    public record CustomerInfo(
            String customerName,
            String contactName,
            String contactPhone,
            String sourcePlace,
            String remark,
            List<String> warnings
    ) {
    }

    /** 价格信息草稿，辅助录入成人、儿童、房差和报价说明。 */
    public record PriceInfo(
            String adultPrice,
            String childPrice,
            String seniorPrice,
            String singleRoomDifference,
            String totalAmount,
            List<String> priceLines,
            List<String> warnings
    ) {
    }

    /** 附加说明草稿，保存确认单里的特殊要求、接待标准和备注。 */
    public record AdditionalInfo(
            String notes,
            String receptionStandard,
            String roomingNote,
            String leaderNote,
            List<String> warnings
    ) {
    }

    /** 游客名单草稿，包含身份证程序校验、分房和领队识别结果。 */
    public record GuestInfo(
            Integer indexNo,
            String name,
            String englishName,
            String certificateNo,
            String gender,
            String birthDate,
            Integer age,
            String phone,
            String customerType,
            String birthplace,
            String issueDate,
            String expiryDate,
            String issuePlace,
            String roomGroup,
            String roomingRemark,
            Boolean leader,
            Boolean suspectedLeader,
            String leaderSourceText,
            String groupRemark,
            String personalRemark,
            Boolean idCardValid,
            List<String> warnings
    ) {
    }

    /** 六个识别模块的完整度评分，供前端展示和真实样例验收。 */
    public record ModuleScores(
            double travelScore,
            double guideScore,
            double customerScore,
            double priceScore,
            double additionalScore,
            double guestListScore
    ) {
    }

    /** 游客名单识别摘要，用于提示是否疑似漏识别或字段缺失。 */
    public record GuestSummary(
            int guestCount,
            int invalidIdCardCount,
            int missingRequiredCount,
            int suspectedMissingCount
    ) {
    }
}
