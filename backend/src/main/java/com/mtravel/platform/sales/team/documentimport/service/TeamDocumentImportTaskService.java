package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.sales.booking.aiimport.service.BookingImportAttachmentTextExtractor;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportTaskCreateRequest;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportTaskResponse;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportTaskUpdateRequest;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportTaskEntity;
import com.mtravel.platform.sales.team.documentimport.mapper.SalesDocumentImportTaskMapper;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 团队文档智能代录任务服务，负责创建、查询和保存计调修改后的草稿。 */
@Service
public class TeamDocumentImportTaskService {
    private static final Logger log = LoggerFactory.getLogger(TeamDocumentImportTaskService.class);
    private final SalesDocumentImportTaskMapper taskMapper;
    private final CommonAttachmentService attachmentService;
    private final TeamDocumentImportProcessor processor;
    private final TeamDocumentImportCustomerResolver customerResolver;
    private final TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer;
    private final BookingImportAttachmentTextExtractor textExtractor;
    private final TeamDocumentImportDraftAssembler draftAssembler;
    private final ObjectMapper objectMapper;

    public TeamDocumentImportTaskService(
            SalesDocumentImportTaskMapper taskMapper,
            CommonAttachmentService attachmentService,
            TeamDocumentImportProcessor processor,
            TeamDocumentImportCustomerResolver customerResolver,
            TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer,
            BookingImportAttachmentTextExtractor textExtractor,
            TeamDocumentImportDraftAssembler draftAssembler,
            ObjectMapper objectMapper
    ) {
        this.taskMapper = taskMapper;
        this.attachmentService = attachmentService;
        this.processor = processor;
        this.customerResolver = customerResolver;
        this.resourceDraftSanitizer = resourceDraftSanitizer;
        this.textExtractor = textExtractor;
        this.draftAssembler = draftAssembler;
        this.objectMapper = objectMapper;
    }

    /** 创建异步识别任务并立即返回，避免 Word 识别占用普通 HTTP 请求。 */
    public TeamDocumentImportTaskResponse create(TeamDocumentImportTaskCreateRequest request, Long tenantId, String operator) {
        CommonAttachmentEntity attachment = attachmentService.getEntity(request.attachmentId(), tenantId);
        String sourceType = supportedSourceType(attachment.getFileExt());
        SalesDocumentImportTaskEntity entity = new SalesDocumentImportTaskEntity();
        entity.setTenantId(tenantId);
        entity.setAttachmentId(request.attachmentId());
        entity.setTargetTeamId(request.targetTeamId());
        entity.setSourceType(sourceType);
        entity.setStatus("pending");
        entity.setProgressPercent(0);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        taskMapper.insert(entity);
        processor.processAsync(entity.getId(), tenantId);
        return toResponse(entity);
    }

    /** 查询当前租户任务，草稿和警告只返回给有权查看当前租户的用户。 */
    public TeamDocumentImportTaskResponse detail(Long taskId, Long tenantId) {
        return toResponse(normalizeLegacyDraft(requireTask(taskId, tenantId), tenantId));
    }

    /** 保存计调在预览抽屉中修正后的草稿和候选选择。 */
    public TeamDocumentImportTaskResponse updateDraft(
            Long taskId, TeamDocumentImportTaskUpdateRequest request, Long tenantId
    ) {
        SalesDocumentImportTaskEntity task = requireTask(taskId, tenantId);
        if ("applied".equals(task.getStatus())) throw new BizException("该导入任务已经应用，不能再修改草稿");
        TeamDocumentImportDraft draft = resourceDraftSanitizer.sanitize(
                customerResolver.validateForPersistence(request.draft(), tenantId), tenantId
        );
        try {
            SalesDocumentImportTaskEntity update = new SalesDocumentImportTaskEntity();
            update.setDraftJson(objectMapper.writeValueAsString(draft));
            update.setWarningsJson(objectMapper.writeValueAsString(draft.warnings()));
            update.setDocumentType(draft.documentType());
            update.setStatus("reviewing");
            update.setProgressPercent(100);
            taskMapper.update(update, baseUpdate(taskId, tenantId));
            return detail(taskId, tenantId);
        } catch (Exception ex) {
            throw new BizException("导入草稿保存失败");
        }
    }

    /** 重试失败任务。 */
    public TeamDocumentImportTaskResponse retry(Long taskId, Long tenantId) {
        SalesDocumentImportTaskEntity task = requireTask(taskId, tenantId);
        if (!"failed".equals(task.getStatus())) throw new BizException("只有失败任务可以重试");
        SalesDocumentImportTaskEntity update = new SalesDocumentImportTaskEntity();
        update.setStatus("pending"); update.setProgressPercent(0); update.setErrorMessage(null);
        // MyBatis-Plus 默认忽略 null；重试必须显式清空旧错误，避免页面展示过期失败原因。
        taskMapper.update(update, baseUpdate(taskId, tenantId).set("error_message", null));
        processor.processAsync(taskId, tenantId);
        return detail(taskId, tenantId);
    }

    /** 供确认写入服务读取已完成的任务。 */
    public SalesDocumentImportTaskEntity requireTask(Long taskId, Long tenantId) {
        SalesDocumentImportTaskEntity task = taskMapper.selectOne(new QueryWrapper<SalesDocumentImportTaskEntity>()
                .eq("tenant_id", tenantId).eq("is_deleted", false).eq("id", taskId).last("limit 1"));
        if (task == null) throw new BizException("导入任务不存在或无权访问");
        return task;
    }

    /** 从持久化 JSON 读取草稿，解析失败时不允许错误数据进入正式写入。 */
    public TeamDocumentImportDraft requireDraft(SalesDocumentImportTaskEntity task) {
        if (!"reviewing".equals(task.getStatus()) && !"applied".equals(task.getStatus())) {
            throw new BizException("导入任务尚未识别完成");
        }
        if (!StringUtils.hasText(task.getDraftJson())) throw new BizException("导入草稿为空，请重新识别");
        try {
            return objectMapper.readValue(task.getDraftJson(), TeamDocumentImportDraft.class);
        } catch (Exception ex) {
            throw new BizException("导入草稿格式异常，请重新识别");
        }
    }

    /** 标记任务已经由计调确认应用到正式团队。 */
    public void markApplied(Long taskId, Long teamId, Long tenantId, String operator) {
        SalesDocumentImportTaskEntity update = new SalesDocumentImportTaskEntity();
        update.setStatus("applied"); update.setAppliedTeamId(teamId); update.setAppliedBy(operator);
        update.setAppliedAt(java.time.OffsetDateTime.now()); update.setProgressPercent(100);
        taskMapper.update(update, baseUpdate(taskId, tenantId));
    }

    private UpdateWrapper<SalesDocumentImportTaskEntity> baseUpdate(Long taskId, Long tenantId) {
        return new UpdateWrapper<SalesDocumentImportTaskEntity>().eq("tenant_id", tenantId).eq("is_deleted", false).eq("id", taskId);
    }

    private TeamDocumentImportTaskResponse toResponse(SalesDocumentImportTaskEntity entity) {
        return new TeamDocumentImportTaskResponse(
                entity.getId(), entity.getAttachmentId(), entity.getTargetTeamId(), entity.getAppliedTeamId(), entity.getSourceType(),
                entity.getDocumentType(), entity.getStatus(), entity.getProgressPercent(), parseDraft(entity.getDraftJson()),
                parseWarnings(entity.getWarningsJson()), entity.getErrorMessage(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    /**
     * 读取历史未应用任务时同步清理旧版仅存客户名称的草稿。
     *
     * <p>这样任务详情不会继续返回可被误认为正式客户的自由文本；规范化结果同时回写，
     * 后续保存和应用仍会再做严格校验。</p>
     */
    private SalesDocumentImportTaskEntity normalizeLegacyDraft(SalesDocumentImportTaskEntity task, Long tenantId) {
        // 仅允许在人工确认前补齐旧草稿；已应用任务必须保持当时的历史快照不被详情接口改写。
        if (!"reviewing".equals(task.getStatus())) {
            return task;
        }
        TeamDocumentImportDraft draft = parseDraft(task.getDraftJson());
        if (draft == null) {
            return task;
        }
        TeamDocumentImportDraft withBackfilledDescription = backfillProductDescription(task, draft, tenantId);
        TeamDocumentImportDraft normalized = resourceDraftSanitizer.sanitize(
                customerResolver.normalizeForPreview(withBackfilledDescription, tenantId), tenantId
        );
        persistNormalizedDraft(task, normalized);
        return task;
    }

    /**
     * 为旧版 reviewing 草稿补齐产品说明。
     *
     * <p>这里只读取原始 Word 并执行本地标题段落提取，不调用百炼、不会重新运行资源匹配，避免详情
     * 请求改变已人工核对的资源候选。补齐成功后由同一次详情请求统一回写，后续读取因已有内容不会再读文件。</p>
     */
    private TeamDocumentImportDraft backfillProductDescription(
            SalesDocumentImportTaskEntity task, TeamDocumentImportDraft draft, Long tenantId
    ) {
        if (hasProductDescriptionContent(draft.productDescription()) || task.getAttachmentId() == null) {
            return draft;
        }
        try {
            CommonAttachmentEntity attachment = attachmentService.getEntity(task.getAttachmentId(), tenantId);
            if (attachment == null || !StringUtils.hasText(attachment.getFileExt())) {
                return draft;
            }
            String sourceText;
            try (InputStream input = attachmentService.openStream(task.getAttachmentId(), tenantId)) {
                sourceText = textExtractor.extract(input, attachment.getFileExt(), tenantId);
            }
            TeamDocumentImportDraft.ProductDescriptionDraft description =
                    draftAssembler.extractProductDescriptionFromSource(sourceText);
            if (!hasProductDescriptionContent(description)) {
                return draft;
            }
            return new TeamDocumentImportDraft(
                    draft.documentType(), draft.confidence(), draft.team(), draft.order(), draft.guests(),
                    draft.itineraryDays(), draft.resources(), draft.warnings(), draft.evidence(), description
            );
        } catch (Exception ex) {
            // 兼容补齐不能阻断已经可用的旧草稿详情；不记录 Word 原文或附件路径，避免日志泄露业务内容。
            log.warn("team document product-description backfill skipped: taskId={}, tenantId={}, exceptionType={}",
                    task.getId(), tenantId, ex.getClass().getSimpleName());
            return draft;
        }
    }

    private boolean hasProductDescriptionContent(TeamDocumentImportDraft.ProductDescriptionDraft value) {
        return value != null && Stream.of(
                value.content(), value.feeIncluded(), value.feeExcluded(), value.childPolicy(), value.shoppingArrangement(),
                value.optionalItems(), value.giftItems(), value.attentionItems(), value.warmReminder()
        ).anyMatch(StringUtils::hasText);
    }

    /**
     * 将历史草稿的客户、资源分类和采购资格收敛结果回写。
     *
     * <p>已应用任务保持历史快照不被详情接口改写；正式应用前的 reviewing 任务则必须持久化，
     * 防止旧页面或重复请求重新带回已清除的资源行。</p>
     */
    private void persistNormalizedDraft(SalesDocumentImportTaskEntity task, TeamDocumentImportDraft normalized) {
        if (normalized == null || "applied".equals(task.getStatus())) {
            return;
        }
        TeamDocumentImportDraft current = parseDraft(task.getDraftJson());
        if (normalized.equals(current)) {
            return;
        }
        try {
            String draftJson = objectMapper.writeValueAsString(normalized);
            String warningsJson = objectMapper.writeValueAsString(normalized.warnings());
            SalesDocumentImportTaskEntity update = new SalesDocumentImportTaskEntity();
            update.setDraftJson(draftJson);
            update.setWarningsJson(warningsJson);
            taskMapper.update(update, baseUpdate(task.getId(), task.getTenantId()));
            task.setDraftJson(draftJson);
            task.setWarningsJson(warningsJson);
        } catch (Exception ex) {
            throw new BizException("导入草稿规范化失败，请稍后重试");
        }
    }

    private TeamDocumentImportDraft parseDraft(String value) {
        if (!StringUtils.hasText(value)) return null;
        try { return objectMapper.readValue(value, TeamDocumentImportDraft.class); } catch (Exception ignored) { return null; }
    }

    private List<String> parseWarnings(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try { return objectMapper.readValue(value, new TypeReference<List<String>>() {}); } catch (Exception ignored) { return List.of(); }
    }

    private String supportedSourceType(String fileExt) {
        if (!StringUtils.hasText(fileExt)) throw new BizException("请上传 doc 或 docx 文件");
        String normalized = fileExt.trim().toLowerCase();
        if (!List.of("doc", "docx").contains(normalized)) throw new BizException("当前智能代录仅支持 doc 和 docx 文件");
        return normalized;
    }
}
