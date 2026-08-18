package com.mtravel.platform.sales.booking.aiimport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 确认单 AI 辅助录入结果。
 *
 * <p>本对象是“可编辑草稿”，前端展示后由用户人工确认并填入表单。后端不得把该结果直接写入
 * 正式订单、游客或费用表。</p>
 */
@Schema(name = "BookingAiImportResponse", description = "确认单 AI 辅助录入识别结果")
public record BookingAiImportResponse(
        @Schema(description = "识别来源类型，例如 text、docx、xlsx、image、pdf", example = "text")
        String sourceType,

        @Schema(description = "整体识别置信度，0 到 1 之间，越高表示越完整", example = "0.86")
        double confidence,

        @Schema(description = "整体识别警告或需要人工核对的问题")
        List<String> warnings,

        @Schema(description = "出行交通信息草稿")
        TravelInfo travelInfo,

        @Schema(description = "导游、全陪和接待要求草稿")
        GuideInfo guideInfo,

        @Schema(description = "客户单位、联系人和来源地草稿")
        CustomerInfo customerInfo,

        @Schema(description = "价格、房差和报价明细草稿")
        PriceInfo priceInfo,

        @Schema(description = "附加说明、接待标准和分房说明草稿")
        AdditionalInfo additionalInfo,

        @Schema(description = "游客名单草稿")
        List<GuestInfo> guests,

        @Schema(description = "各识别模块完整度评分")
        ModuleScores moduleScores,

        @Schema(description = "游客名单识别摘要")
        GuestSummary guestSummary,

        @Schema(description = "识别依据文本片段，帮助人工复核")
        List<String> evidence
) {

    /** 行程说明草稿，对应老系统收客页来程、返程和参团时间字段。 */
    @Schema(name = "BookingAiImportTravelInfo", description = "AI 识别出的出行交通信息草稿")
    public record TravelInfo(
            @Schema(description = "参团或抵达日期", example = "2026-07-20")
            String joinDate,

            @Schema(description = "去程出发城市", example = "上海")
            String outboundOriginCity,

            @Schema(description = "去程抵达城市", example = "南京")
            String outboundArrivalCity,

            @Schema(description = "去程出发站点或机场", example = "上海虹桥站")
            String outboundStationName,

            @Schema(description = "去程车次、航班号或交通编号", example = "G7001")
            String outboundTrafficNo,

            @Schema(description = "去程出发时间", example = "08:00")
            String outboundDepartureTime,

            @Schema(description = "去程抵达时间", example = "09:00")
            String outboundArrivalTime,

            @Schema(description = "返程站点或机场", example = "南京南站")
            String returnStationName,

            @Schema(description = "返程出发城市", example = "南京")
            String returnDepartureCity,

            @Schema(description = "返程抵达城市", example = "上海")
            String returnDestinationCity,

            @Schema(description = "返程车次、航班号或交通编号", example = "G7020")
            String returnTrafficNo,

            @Schema(description = "返程出发时间", example = "17:30")
            String returnDepartureTime,

            @Schema(description = "返程抵达时间", example = "19:00")
            String returnArrivalTime,

            @Schema(description = "交通信息识别警告")
            List<String> warnings
    ) {
    }

    /** 导游相关草稿，作为导游、全陪、接待要求等人工录入线索。 */
    @Schema(name = "BookingAiImportGuideInfo", description = "AI 识别出的导游和接待要求草稿")
    public record GuideInfo(
            @Schema(description = "导游姓名", example = "王导")
            String guideName,

            @Schema(description = "导游电话", example = "13800000000")
            String guidePhone,

            @Schema(description = "全陪或领队姓名", example = "李全陪")
            String escortName,

            @Schema(description = "接待要求", example = "安排南京本地中文导游")
            String receptionRequirement,

            @Schema(description = "导游信息识别警告")
            List<String> warnings
    ) {
    }

    /** 客户信息草稿，辅助销售选择客户单位和联系人。 */
    @Schema(name = "BookingAiImportCustomerInfo", description = "AI 识别出的客户信息草稿")
    public record CustomerInfo(
            @Schema(description = "客户单位名称", example = "南京某旅行社")
            String customerName,

            @Schema(description = "联系人姓名", example = "王经理")
            String contactName,

            @Schema(description = "联系人电话", example = "13800000000")
            String contactPhone,

            @Schema(description = "客源地或来源城市", example = "上海")
            String sourcePlace,

            @Schema(description = "客户相关备注", example = "老客户，走月结")
            String remark,

            @Schema(description = "客户信息识别警告")
            List<String> warnings
    ) {
    }

    /** 价格信息草稿，辅助录入成人、儿童、房差和报价说明。 */
    @Schema(name = "BookingAiImportPriceInfo", description = "AI 识别出的价格信息草稿")
    public record PriceInfo(
            @Schema(description = "成人单价，保留原文本金额", example = "1000")
            String adultPrice,

            @Schema(description = "儿童单价，保留原文本金额", example = "800")
            String childPrice,

            @Schema(description = "老人单价，保留原文本金额", example = "900")
            String seniorPrice,

            @Schema(description = "单人房差，保留原文本金额", example = "300")
            String singleRoomDifference,

            @Schema(description = "订单总金额，保留原文本金额", example = "2000")
            String totalAmount,

            @Schema(description = "价格明细原文")
            List<String> priceLines,

            @Schema(description = "价格信息识别警告")
            List<String> warnings
    ) {
    }

    /** 附加说明草稿，保存确认单里的特殊要求、接待标准和备注。 */
    @Schema(name = "BookingAiImportAdditionalInfo", description = "AI 识别出的附加说明草稿")
    public record AdditionalInfo(
            @Schema(description = "附加备注", example = "游客需安排无烟房")
            String notes,

            @Schema(description = "接待标准", example = "舒适型")
            String receptionStandard,

            @Schema(description = "分房说明", example = "2人1间")
            String roomingNote,

            @Schema(description = "领队说明", example = "张三为领队")
            String leaderNote,

            @Schema(description = "附加说明识别警告")
            List<String> warnings
    ) {
    }

    /** 游客名单草稿，包含身份证程序校验、分房和领队识别结果。 */
    @Schema(name = "BookingAiImportGuestInfo", description = "AI 识别出的游客名单草稿")
    public record GuestInfo(
            @Schema(description = "游客序号", example = "1")
            Integer indexNo,

            @Schema(description = "游客姓名", example = "张三")
            String name,

            @Schema(description = "英文名", example = "ZHANG SAN")
            String englishName,

            @Schema(description = "证件号码", example = "320102199001011234")
            String certificateNo,

            @Schema(description = "性别", example = "男")
            String gender,

            @Schema(description = "出生日期", example = "1990-01-01")
            String birthDate,

            @Schema(description = "年龄", example = "36")
            Integer age,

            @Schema(description = "手机号", example = "13900000000")
            String phone,

            @Schema(description = "游客类型，例如 adult 成人、child 儿童、leader 领队", example = "adult")
            String customerType,

            @Schema(description = "出生地", example = "江苏南京")
            String birthplace,

            @Schema(description = "证件签发日期", example = "2015-01-01")
            String issueDate,

            @Schema(description = "证件有效期", example = "2035-01-01")
            String expiryDate,

            @Schema(description = "证件签发地", example = "南京市公安局")
            String issuePlace,

            @Schema(description = "房间分组", example = "A房")
            String roomGroup,

            @Schema(description = "分房备注", example = "与李四同住")
            String roomingRemark,

            @Schema(description = "是否领队", example = "false")
            Boolean leader,

            @Schema(description = "是否疑似领队，需人工确认", example = "false")
            Boolean suspectedLeader,

            @Schema(description = "领队识别来源文本", example = "备注：张三带队")
            String leaderSourceText,

            @Schema(description = "团队备注", example = "同一客户单位")
            String groupRemark,

            @Schema(description = "个人备注", example = "素食")
            String personalRemark,

            @Schema(description = "身份证号码是否通过格式校验", example = "true")
            Boolean idCardValid,

            @Schema(description = "游客信息识别警告")
            List<String> warnings
    ) {
    }

    /** 六个识别模块的完整度评分，供前端展示和真实样例验收。 */
    @Schema(name = "BookingAiImportModuleScores", description = "AI 识别模块完整度评分")
    public record ModuleScores(
            @Schema(description = "交通信息完整度评分", example = "0.80")
            double travelScore,

            @Schema(description = "导游信息完整度评分", example = "0.60")
            double guideScore,

            @Schema(description = "客户信息完整度评分", example = "0.90")
            double customerScore,

            @Schema(description = "价格信息完整度评分", example = "0.85")
            double priceScore,

            @Schema(description = "附加说明完整度评分", example = "0.70")
            double additionalScore,

            @Schema(description = "游客名单完整度评分", example = "0.95")
            double guestListScore
    ) {
    }

    /** 游客名单识别摘要，用于提示是否疑似漏识别或字段缺失。 */
    @Schema(name = "BookingAiImportGuestSummary", description = "AI 识别游客名单摘要")
    public record GuestSummary(
            @Schema(description = "识别到的游客人数", example = "2")
            int guestCount,

            @Schema(description = "身份证校验失败人数", example = "0")
            int invalidIdCardCount,

            @Schema(description = "缺少必填信息人数", example = "0")
            int missingRequiredCount,

            @Schema(description = "疑似漏识别人数", example = "0")
            int suspectedMissingCount
    ) {
    }
}
