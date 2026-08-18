package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.enterprise.companyinfo.service.EnterpriseCompanyInfoService;
import com.mtravel.platform.sales.booking.aiimport.service.AiModelClient;
import com.mtravel.platform.sales.booking.aiimport.service.BookingImportAttachmentTextExtractor;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportTaskEntity;
import com.mtravel.platform.sales.team.documentimport.mapper.SalesDocumentImportTaskMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 团队 Word 智能代录后台处理器，异步执行文本提取、百炼识别和资源候选匹配。 */
@Service
public class TeamDocumentImportProcessor {
    private static final Logger log = LoggerFactory.getLogger(TeamDocumentImportProcessor.class);
    private static final Pattern ID_CARD = Pattern.compile("\\b\\d{17}[0-9Xx]\\b");
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private final SalesDocumentImportTaskMapper taskMapper;
    private final CommonAttachmentService attachmentService;
    private final BookingImportAttachmentTextExtractor textExtractor;
    private final AiModelClient aiModelClient;
    private final TeamDocumentImportDraftAssembler assembler;
    private final TeamDocumentImportCustomerResolver customerResolver;
    private final TeamDocumentImportResourceMatcher resourceMatcher;
    private final TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer;
    private final ObjectMapper objectMapper;
    private final EnterpriseCompanyInfoService enterpriseCompanyInfoService;

    public TeamDocumentImportProcessor(
            SalesDocumentImportTaskMapper taskMapper,
            CommonAttachmentService attachmentService,
            BookingImportAttachmentTextExtractor textExtractor,
            AiModelClient aiModelClient,
            TeamDocumentImportDraftAssembler assembler,
            TeamDocumentImportCustomerResolver customerResolver,
            TeamDocumentImportResourceMatcher resourceMatcher,
            TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer,
            ObjectMapper objectMapper,
            EnterpriseCompanyInfoService enterpriseCompanyInfoService
    ) {
        this.taskMapper = taskMapper;
        this.attachmentService = attachmentService;
        this.textExtractor = textExtractor;
        this.aiModelClient = aiModelClient;
        this.assembler = assembler;
        this.customerResolver = customerResolver;
        this.resourceMatcher = resourceMatcher;
        this.resourceDraftSanitizer = resourceDraftSanitizer;
        this.objectMapper = objectMapper;
        this.enterpriseCompanyInfoService = enterpriseCompanyInfoService;
    }

    /** 后台执行一次导入任务。任务状态始终落库，前端只轮询轻量状态接口。 */
    @Async("teamDocumentImportTaskExecutor")
    public void processAsync(Long taskId, Long tenantId) {
        try {
            SalesDocumentImportTaskEntity task = currentTask(taskId, tenantId);
            if (task == null || !"pending".equals(task.getStatus())) return;
            updateState(taskId, tenantId, "extracting", 15, null, null, null, null);
            CommonAttachmentEntity attachment = attachmentService.getEntity(task.getAttachmentId(), tenantId);
            String text;
            try (InputStream input = attachmentService.openStream(task.getAttachmentId(), tenantId)) {
                text = textExtractor.extract(input, attachment.getFileExt(), tenantId);
            }
            if (!StringUtils.hasText(text)) {
                throw new IllegalArgumentException("Word 未提取到有效文字，请检查文件是否损坏或加密");
            }
            String sourceFileName = attachment == null ? null : attachment.getOriginalFilename();
            String currentCompanyName = resolveCurrentCompanyName(tenantId);
            RedactionResult redaction = redact(text);
            updateState(taskId, tenantId, "recognizing", 45, null, null, null, null);
            Optional<String> aiJson = aiModelClient.recognize(
                    tenantId,
                    buildModelInput(redaction.redactedText(), sourceFileName, currentCompanyName)
            );
            logModelShape(taskId, aiJson.orElse(null));
            TeamDocumentImportDraft draft = assembler.assemble(
                    text,
                    aiJson.orElse(null),
                    task.getSourceType(),
                    sourceFileName,
                    currentCompanyName,
                    redaction.phoneTokens()
            );
            // 模型识别到的客户名称只作为候选，必须唯一关联当前租户有效主档后才能进入订单草稿。
            draft = customerResolver.resolveRecognizedCustomer(draft, tenantId);
            updateState(taskId, tenantId, "matching", 72, null, null, null, null);
            TeamDocumentImportDraft matched = resourceMatcher.match(draft, tenantId);
            // 匹配完成后仍需按 Word 明确业务方、资源分类和采购属性统一收敛，不能让模型候选直接落库。
            TeamDocumentImportDraft sanitized = resourceDraftSanitizer.sanitize(matched, tenantId, text);
            String serialized = objectMapper.writeValueAsString(sanitized);
            String warnings = objectMapper.writeValueAsString(sanitized.warnings());
            updateState(taskId, tenantId, "reviewing", 100, serialized, warnings, sanitized.documentType(), null);
        } catch (Exception ex) {
            // 只记录任务标识和异常堆栈，不记录上传原文、游客信息或模型鉴权数据，便于定位异步失败。
            log.error("team document import failed: taskId={}, tenantId={}", taskId, tenantId, ex);
            updateState(taskId, tenantId, "failed", 100, null, null, null, safeError(ex));
        }
    }

    private SalesDocumentImportTaskEntity currentTask(Long taskId, Long tenantId) {
        return taskMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SalesDocumentImportTaskEntity>()
                .eq("tenant_id", tenantId).eq("is_deleted", false).eq("id", taskId).last("limit 1"));
    }

    private void updateState(
            Long taskId, Long tenantId, String status, int progress, String draftJson, String warningsJson,
            String documentType, String errorMessage
    ) {
        SalesDocumentImportTaskEntity update = new SalesDocumentImportTaskEntity();
        update.setStatus(status);
        update.setProgressPercent(progress);
        if (draftJson != null) update.setDraftJson(draftJson);
        if (warningsJson != null) update.setWarningsJson(warningsJson);
        if (documentType != null) update.setDocumentType(documentType);
        UpdateWrapper<SalesDocumentImportTaskEntity> wrapper = new UpdateWrapper<SalesDocumentImportTaskEntity>()
                .eq("tenant_id", tenantId).eq("is_deleted", false).eq("id", taskId)
                // 成功路径也要清除历史错误摘要；其它 null 字段仍保持原草稿不变。
                .set("error_message", errorMessage);
        taskMapper.update(update, wrapper);
    }

    /** 外部模型只接收脱敏文本，最终游客证件和电话仍以本地解析结果回填。 */
    private RedactionResult redact(String text) {
        String withoutIds = ID_CARD.matcher(text).replaceAll("[身份证号]");
        Map<String, String> phoneTokens = new LinkedHashMap<>();
        AtomicInteger counter = new AtomicInteger(1);
        String redacted = MOBILE.matcher(withoutIds).replaceAll(matchResult -> {
            String token = "[PHONE_%d]".formatted(counter.getAndIncrement());
            phoneTokens.put(token, matchResult.group());
            return token;
        });
        return new RedactionResult(redacted, Map.copyOf(phoneTokens));
    }

    private String buildModelInput(String redactedText, String sourceFileName, String currentCompanyName) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(currentCompanyName)) {
            builder.append("【当前企业】").append(currentCompanyName.trim()).append('\n');
        }
        if (StringUtils.hasText(sourceFileName)) {
            builder.append("【原始文件名】").append(sourceFileName.trim()).append('\n');
        }
        builder.append("【文档正文】\n").append(redactedText);
        return builder.toString();
    }

    private String resolveCurrentCompanyName(Long tenantId) {
        if (enterpriseCompanyInfoService == null) {
            return null;
        }
        try {
            var current = enterpriseCompanyInfoService.current(tenantId);
            return current == null ? null : current.companyName();
        } catch (RuntimeException ex) {
            // 公司资料只是双方角色识别的辅助信息，查询失败时仍允许模型识别并交给人工确认。
            log.warn("team document company lookup failed: tenantId={}, exceptionType={}",
                    tenantId, ex.getClass().getSimpleName());
            return null;
        }
    }

    private String safeError(Exception exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) return "文档识别失败，请稍后重试或手工录入";
        return message.length() > 450 ? message.substring(0, 450) : message;
    }

    /** 只记录模型结构摘要，便于定位字段兼容问题，绝不记录识别原文或游客隐私。 */
    private void logModelShape(Long taskId, String aiJson) {
        if (!StringUtils.hasText(aiJson)) {
            log.info("team document model output: taskId={}, content=empty", taskId);
            return;
        }
        try {
            var root = objectMapper.readTree(aiJson);
            var resources = root.path("resources");
            List<String> fields = new ArrayList<>();
            if (root.isObject()) root.fieldNames().forEachRemaining(fields::add);
            log.info("team document model output: taskId={}, topLevelFields={}, resourcesNode={}, resourceCount={}",
                    taskId, fields,
                    resources.getNodeType(), resources.isArray() ? resources.size() : 0);
        } catch (Exception ignored) {
            log.info("team document model output: taskId={}, content=invalid_json", taskId);
        }
    }

    private record RedactionResult(String redactedText, Map<String, String> phoneTokens) {}
}
