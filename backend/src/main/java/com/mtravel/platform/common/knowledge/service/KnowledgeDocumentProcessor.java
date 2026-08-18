package com.mtravel.platform.common.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.common.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.mtravel.platform.common.knowledge.entity.KnowledgeDocumentEntity;
import com.mtravel.platform.common.knowledge.entity.KnowledgeProcessingTaskEntity;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeDocumentChunkMapper;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeDocumentMapper;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeProcessingTaskMapper;
import com.mtravel.platform.sales.booking.aiimport.service.BookingImportAttachmentTextExtractor;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 知识文档后台处理器。
 *
 * <p>负责抽取正文、切片和写入向量。写入前始终校验文档仍未删除且版本一致，避免删除文件后
 * 已启动的异步任务重新生成向量。</p>
 */
@Service
public class KnowledgeDocumentProcessor {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 120;

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeDocumentChunkMapper chunkMapper;
    private final KnowledgeProcessingTaskMapper taskMapper;
    private final CommonAttachmentService attachmentService;
    private final BookingImportAttachmentTextExtractor textExtractor;
    private final KnowledgeEmbeddingClient embeddingClient;

    public KnowledgeDocumentProcessor(
            KnowledgeDocumentMapper documentMapper,
            KnowledgeDocumentChunkMapper chunkMapper,
            KnowledgeProcessingTaskMapper taskMapper,
            CommonAttachmentService attachmentService,
            BookingImportAttachmentTextExtractor textExtractor,
            KnowledgeEmbeddingClient embeddingClient
    ) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.taskMapper = taskMapper;
        this.attachmentService = attachmentService;
        this.textExtractor = textExtractor;
        this.embeddingClient = embeddingClient;
    }

    /**
     * 异步处理知识文档。
     *
     * @param documentId 知识文档 ID
     * @param tenantId 当前租户 ID
     * @param indexVersion 本次任务对应的索引版本
     */
    @Async("knowledgeTaskExecutor")
    public void processAsync(Long documentId, Long tenantId, Integer indexVersion) {
        if (!markRunning(documentId, tenantId, indexVersion)) {
            return;
        }
        try {
            KnowledgeDocumentEntity document = currentDocument(documentId, tenantId, indexVersion);
            if (document == null) {
                cancelTasks(documentId, tenantId, indexVersion);
                return;
            }
            String text;
            try (InputStream inputStream = attachmentService.openStream(document.getAttachmentId(), tenantId)) {
                text = textExtractor.extract(inputStream, document.getFileExt(), tenantId);
            }
            if (!StringUtils.hasText(text)) {
                markFailed(documentId, tenantId, indexVersion, "文件未抽取到有效文本");
                return;
            }
            if (currentDocument(documentId, tenantId, indexVersion) == null) {
                cancelTasks(documentId, tenantId, indexVersion);
                return;
            }
            List<String> chunks = splitText(text);
            chunkMapper.deleteByDocument(tenantId, documentId);
            boolean allEmbedded = true;
            int chunkNo = 1;
            for (String chunk : chunks) {
                if (currentDocument(documentId, tenantId, indexVersion) == null) {
                    chunkMapper.deleteByDocument(tenantId, documentId);
                    cancelTasks(documentId, tenantId, indexVersion);
                    return;
                }
                String embedding = embeddingClient.embed(tenantId, chunk).orElse(null);
                if (embedding == null) {
                    allEmbedded = false;
                }
                KnowledgeDocumentChunkEntity entity = new KnowledgeDocumentChunkEntity();
                entity.setTenantId(tenantId);
                entity.setDocumentId(documentId);
                entity.setSourceType(document.getSourceType());
                entity.setSourceId(document.getSourceId());
                entity.setChunkNo(chunkNo);
                entity.setChunkText(chunk);
                entity.setTokenCount(estimateTokens(chunk));
                entity.setEmbeddingModel(embedding == null ? null : embeddingClient.modelName());
                entity.setEmbedding(embedding);
                entity.setIndexVersion(indexVersion);
                chunkMapper.insertChunk(entity);
                chunkNo += 1;
            }
            markSucceeded(documentId, tenantId, indexVersion, text, allEmbedded);
        } catch (RuntimeException ex) {
            markFailed(documentId, tenantId, indexVersion, safeError(ex));
        } catch (java.io.IOException ex) {
            markFailed(documentId, tenantId, indexVersion, "文件读取失败");
        }
    }

    private KnowledgeDocumentEntity currentDocument(Long documentId, Long tenantId, Integer indexVersion) {
        return documentMapper.selectOne(new QueryWrapper<KnowledgeDocumentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("id", documentId)
                .eq("index_version", indexVersion)
                .ne("processing_status", "deleted")
                .last("limit 1"));
    }

    private boolean markRunning(Long documentId, Long tenantId, Integer indexVersion) {
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setProcessingStatus("processing");
        document.setIndexStatus("pending");
        document.setErrorMessage(null);
        int updated = documentMapper.update(document, baseDocumentUpdate(tenantId, documentId, indexVersion));
        if (updated == 0) {
            return false;
        }
        KnowledgeProcessingTaskEntity task = new KnowledgeProcessingTaskEntity();
        task.setTaskStatus("running");
        task.setLockedBy("backend");
        task.setLockedAt(OffsetDateTime.now());
        taskMapper.update(task, baseTaskUpdate(tenantId, documentId, indexVersion).eq("task_status", "pending"));
        return true;
    }

    private void markSucceeded(
            Long documentId,
            Long tenantId,
            Integer indexVersion,
            String text,
            boolean allEmbedded
    ) {
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setProcessingStatus("succeeded");
        document.setIndexStatus(allEmbedded ? "indexed" : "pending");
        document.setExtractedText(text);
        document.setErrorMessage(allEmbedded ? null : "文本已抽取，向量服务未配置或调用失败，待重试向量化");
        document.setProcessedAt(OffsetDateTime.now());
        documentMapper.update(document, baseDocumentUpdate(tenantId, documentId, indexVersion));

        KnowledgeProcessingTaskEntity task = new KnowledgeProcessingTaskEntity();
        task.setTaskStatus("succeeded");
        task.setErrorMessage(null);
        taskMapper.update(task, baseTaskUpdate(tenantId, documentId, indexVersion).ne("task_status", "cancelled"));
    }

    private void markFailed(Long documentId, Long tenantId, Integer indexVersion, String errorMessage) {
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setProcessingStatus("failed");
        document.setIndexStatus("failed");
        document.setErrorMessage(errorMessage);
        document.setProcessedAt(OffsetDateTime.now());
        documentMapper.update(document, baseDocumentUpdate(tenantId, documentId, indexVersion));

        KnowledgeProcessingTaskEntity task = new KnowledgeProcessingTaskEntity();
        task.setTaskStatus("failed");
        task.setErrorMessage(errorMessage);
        taskMapper.update(task, baseTaskUpdate(tenantId, documentId, indexVersion).ne("task_status", "cancelled"));
    }

    private void cancelTasks(Long documentId, Long tenantId, Integer indexVersion) {
        KnowledgeProcessingTaskEntity task = new KnowledgeProcessingTaskEntity();
        task.setTaskStatus("cancelled");
        taskMapper.update(task, baseTaskUpdate(tenantId, documentId, indexVersion)
                .in("task_status", List.of("pending", "running")));
    }

    private UpdateWrapper<KnowledgeDocumentEntity> baseDocumentUpdate(
            Long tenantId,
            Long documentId,
            Integer indexVersion
    ) {
        return new UpdateWrapper<KnowledgeDocumentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", documentId)
                .eq("index_version", indexVersion);
    }

    private UpdateWrapper<KnowledgeProcessingTaskEntity> baseTaskUpdate(
            Long tenantId,
            Long documentId,
            Integer indexVersion
    ) {
        return new UpdateWrapper<KnowledgeProcessingTaskEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("document_id", documentId)
                .eq("index_version", indexVersion);
    }

    private List<String> splitText(String value) {
        String text = value.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + CHUNK_SIZE);
            chunks.add(text.substring(start, end).trim());
            if (end >= text.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks.stream().filter(StringUtils::hasText).toList();
    }

    private int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil(text.length() / 1.8));
    }

    private String safeError(RuntimeException ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "知识文档处理失败";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
