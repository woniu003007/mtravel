package com.mtravel.platform.sales.team.documentimport.dto;

import java.math.BigDecimal;
import java.util.List;

/** 团队 Word 智能代录的可编辑结构化草稿。 */
public record TeamDocumentImportDraft(
        String documentType,
        double confidence,
        TeamDraft team,
        OrderDraft order,
        List<GuestDraft> guests,
        List<ItineraryDraft> itineraryDays,
        List<ResourceDraft> resources,
        List<String> warnings,
        List<String> evidence,
        ProductDescriptionDraft productDescription
) {
    /**
     * 兼容已保存的旧草稿和既有调用方；旧 JSON 中没有产品说明时按空草稿处理。
     */
    public TeamDocumentImportDraft(
            String documentType,
            double confidence,
            TeamDraft team,
            OrderDraft order,
            List<GuestDraft> guests,
            List<ItineraryDraft> itineraryDays,
            List<ResourceDraft> resources,
            List<String> warnings,
            List<String> evidence
    ) {
        this(documentType, confidence, team, order, guests, itineraryDays, resources, warnings, evidence, null);
    }

    /** 团队主档和产品快照候选字段。 */
    public record TeamDraft(
            String teamName,
            String departureDate,
            Integer travelDays,
            Integer totalSeats,
            String businessType,
            String domesticInternational,
            String receptionStandard,
            String remark
    ) {}

    /** 一个团队下的订单候选。首期默认一个 Word 生成一个主订单草稿。 */
    public record OrderDraft(
            Long customerId,
            String customerName,
            String contactName,
            String contactPhone,
            String pickupInfo,
            String dropoffInfo,
            String guideName,
            String guidePhone,
            String orderRemark,
            List<OrderPriceDraft> priceLines
    ) {}

    /** 订单价格候选，不代表收款或已收金额。 */
    public record OrderPriceDraft(String lineType, String itemName, BigDecimal unitPrice, BigDecimal quantity) {}

    /** 游客候选。证件字段在外部 AI 调用前脱敏，回填来自本地解析和校验。 */
    public record GuestDraft(
            Integer indexNo, String guestName, String certificateNo, String gender, String birthDate,
            Integer age, String phone, String guestType, String roomGroup, String roomRemark,
            Boolean leaderFlag, Boolean idCardValid, String remark
    ) {}

    /** 每日行程候选。 */
    public record ItineraryDraft(
            Integer dayNo, String dayTitle, String itineraryContent, String accommodationNote,
            Boolean breakfastIncluded, Boolean lunchIncluded, Boolean dinnerIncluded
    ) {}

    /**
     * Word 中可复用到团队产品说明页的文本候选。
     *
     * <p>该对象只承载待人工确认的原文或忠实摘要；实际保存团队时仍由团队创建接口写入产品快照。</p>
     */
    public record ProductDescriptionDraft(
            String content,
            String feeIncluded,
            String feeExcluded,
            String childPolicy,
            String shoppingArrangement,
            String optionalItems,
            String giftItems,
            String attentionItems,
            String warmReminder
    ) {}

    /** 资源候选及系统匹配结果；time 是已识别的时刻或原文时间表述，无法可靠识别时为空。 */
    public record ResourceDraft(
            String itemKey, Integer dayNo, String time, String arrangementType, String sourceName, String city,
            String remark, Long selectedResourceId, String selectedResourceName, Long selectedSupplierId,
            String selectedSupplierName, boolean requiresConfirmation, List<ResourceCandidate> candidates
    ) {}

    /** 一个资源主档与默认供应商候选。 */
    public record ResourceCandidate(
            Long resourceId, String resourceName, String resourceType, String city, Long supplierId,
            String supplierName, boolean defaultSupplier, boolean exactMatch
    ) {}
}
