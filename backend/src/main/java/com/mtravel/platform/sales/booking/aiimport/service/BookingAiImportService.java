package com.mtravel.platform.sales.booking.aiimport.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportRequest;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 确认单 AI 辅助录入服务。
 *
 * <p>服务只负责把外部资料转成可编辑草稿。即使模型识别成功，也不能在这里自动保存订单、游客名单、
 * 价格或导游安排，必须由前端展示后让用户人工确认。</p>
 */
@Service
public class BookingAiImportService {

    private final LocalBookingImportParser localParser;
    private final AiModelClient aiModelClient;
    private final CommonAttachmentService attachmentService;
    private final BookingImportAttachmentTextExtractor attachmentTextExtractor;

    @Autowired
    public BookingAiImportService(
            LocalBookingImportParser localParser,
            AiModelClient aiModelClient,
            CommonAttachmentService attachmentService,
            BookingImportAttachmentTextExtractor attachmentTextExtractor
    ) {
        this.localParser = localParser;
        this.aiModelClient = aiModelClient;
        this.attachmentService = attachmentService;
        this.attachmentTextExtractor = attachmentTextExtractor;
    }

    /** 测试构造器，使用文本识别时不依赖附件服务。 */
    BookingAiImportService(LocalBookingImportParser localParser, AiModelClient aiModelClient) {
        this(localParser, aiModelClient, null, new BookingImportAttachmentTextExtractor());
    }

    /** 测试构造器，验证附件读取流程。 */
    BookingAiImportService(
            LocalBookingImportParser localParser,
            AiModelClient aiModelClient,
            CommonAttachmentService attachmentService
    ) {
        this(localParser, aiModelClient, attachmentService, new BookingImportAttachmentTextExtractor());
    }

    /**
     * 识别用户上传或粘贴的确认单资料。
     *
     * <p>首版在模型未配置或模型失败时使用本地规则兜底，保证 Word/文本中的明显字段可以辅助录入。</p>
     */
    public BookingAiImportResponse recognize(BookingAiImportRequest request, Long tenantId, String operator) {
        String text = resolveSourceText(request, tenantId);
        if (!StringUtils.hasText(text)) {
            throw new BizException("请上传确认单或粘贴需要识别的内容");
        }
        // 当前先用规则解析可验证字段；模型返回 JSON 的合并逻辑后续在这里扩展，仍不改变前端协议。
        aiModelClient.recognize(tenantId, text).ifPresent(ignored -> {
            // 真实模型结果合并时必须保留程序校验结果，不能覆盖身份证合法性判断。
        });
        return localParser.parse(text, request.sourceType());
    }

    private String resolveSourceText(BookingAiImportRequest request, Long tenantId) {
        if (request == null) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(request.text())) {
            segments.add("【粘贴文本】\n" + request.text().trim());
        }
        for (Long attachmentId : attachmentIds(request)) {
            segments.add(resolveAttachmentText(attachmentId, request.sourceType(), tenantId));
        }
        return String.join("\n\n", segments).trim();
    }

    private List<Long> attachmentIds(BookingAiImportRequest request) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (request.attachmentId() != null) {
            ids.add(request.attachmentId());
        }
        if (request.attachmentIds() != null) {
            request.attachmentIds().stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    private String resolveAttachmentText(Long attachmentId, String fallbackSourceType, Long tenantId) {
        if (attachmentService == null) {
            throw new BizException("附件识别服务未启用");
        }
        CommonAttachmentEntity entity = attachmentService.getEntity(attachmentId, tenantId);
        // 兼容旧接口和单元测试：部分调用只传附件流和 sourceType，没有可读取的附件元数据。
        String sourceType = entity != null && StringUtils.hasText(entity.getFileExt())
                ? entity.getFileExt()
                : fallbackSourceType;
        String filename = entity != null && StringUtils.hasText(entity.getOriginalFilename())
                ? entity.getOriginalFilename()
                : "附件" + attachmentId;
        try (var input = attachmentService.openStream(attachmentId, tenantId)) {
            return "【附件：" + filename + "】\n"
                    + attachmentTextExtractor.extract(input, sourceType, tenantId);
        } catch (java.io.IOException ex) {
            throw new BizException("确认单文件读取失败");
        }
    }
}
