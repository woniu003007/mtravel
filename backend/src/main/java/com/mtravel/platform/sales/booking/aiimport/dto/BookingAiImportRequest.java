package com.mtravel.platform.sales.booking.aiimport.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 确认单 AI 辅助录入请求。
 *
 * <p>前端可以直接传粘贴文本，也可以先上传附件后传 attachmentId。两者都只是生成草稿，
 * 不会自动保存订单、游客名单或费用信息。</p>
 *
 * @param text 用户粘贴的确认单、微信消息或名单文本
 * @param attachmentId 已上传附件 ID
 * @param attachmentIds 已上传附件 ID 列表，用于 Word、图片、Excel 等多资料合并识别
 * @param sourceType 来源类型，例如 text、docx、xlsx、image、pdf
 */
public record BookingAiImportRequest(
        @Size(max = 200_000, message = "识别文本不能超过200000字符") String text,
        Long attachmentId,
        List<Long> attachmentIds,
        @Size(max = 30, message = "来源类型最多30个字符") String sourceType
) {
    /** 兼容旧前端和既有测试的单附件请求格式。 */
    public BookingAiImportRequest(String text, Long attachmentId, String sourceType) {
        this(text, attachmentId, null, sourceType);
    }
}
