package com.mtravel.platform.purchase.resource.material.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.knowledge.service.KnowledgeEmbeddingClient;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionChunkEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.enums.PurchaseResourceIntroductionIndexStatus;
import com.mtravel.platform.purchase.resource.material.enums.PurchaseResourceIntroductionStatus;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionChunkMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 采购资源介绍素材异步向量处理器。 */
@Service
public class PurchaseResourceIntroductionProcessor {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 120;
    private static final String CONTENT_LABEL = "【资源介绍正文】";
    private static final String NOTICE_LABEL = "【注意事项】";

    private final PurchaseResourceIntroductionMapper introductionMapper;
    private final PurchaseResourceIntroductionChunkMapper chunkMapper;
    private final KnowledgeEmbeddingClient embeddingClient;

    public PurchaseResourceIntroductionProcessor(
            PurchaseResourceIntroductionMapper introductionMapper,
            PurchaseResourceIntroductionChunkMapper chunkMapper,
            KnowledgeEmbeddingClient embeddingClient
    ) {
        this.introductionMapper = introductionMapper;
        this.chunkMapper = chunkMapper;
        this.embeddingClient = embeddingClient;
    }

    /** 异步切片并向量化已发布的介绍正文和注意事项。过期版本不会继续写入向量表。 */
    @Async("knowledgeTaskExecutor")
    public void processAsync(Long tenantId, Long introductionId, Integer indexVersion) {
        try {
            PurchaseResourceIntroductionEntity introduction = current(tenantId, introductionId, indexVersion);
            if (introduction == null) {
                return;
            }
            List<String> chunks = splitIntroduction(introduction);
            chunkMapper.deleteByIntroduction(tenantId, introductionId);
            boolean allEmbedded = true;
            int chunkNo = 1;
            for (String chunk : chunks) {
                if (current(tenantId, introductionId, indexVersion) == null) {
                    chunkMapper.deleteByIntroduction(tenantId, introductionId);
                    return;
                }
                String embedding = embeddingClient.embed(tenantId, chunk).orElse(null);
                if (embedding == null) {
                    allEmbedded = false;
                }
                PurchaseResourceIntroductionChunkEntity entity = new PurchaseResourceIntroductionChunkEntity();
                entity.setTenantId(tenantId);
                entity.setIntroductionId(introductionId);
                entity.setResourceId(introduction.getResourceId());
                entity.setChunkNo(chunkNo);
                entity.setChunkText(chunk);
                entity.setTokenCount(estimateTokens(chunk));
                entity.setEmbeddingModel(embedding == null ? null : embeddingClient.modelName());
                entity.setEmbedding(embedding);
                entity.setIndexVersion(indexVersion);
                chunkMapper.insertChunk(entity);
                chunkNo += 1;
            }
            updateIndexStatus(
                    tenantId,
                    introductionId,
                    indexVersion,
                    allEmbedded
                            ? PurchaseResourceIntroductionIndexStatus.INDEXED.value()
                            : PurchaseResourceIntroductionIndexStatus.PENDING.value(),
                    allEmbedded ? null : "介绍正文和注意事项已切片，向量服务未配置或调用失败，待重试"
            );
        } catch (RuntimeException ex) {
            chunkMapper.deleteByIntroduction(tenantId, introductionId);
            updateIndexStatus(tenantId, introductionId, indexVersion,
                    PurchaseResourceIntroductionIndexStatus.FAILED.value(), safeError(ex));
        }
    }

    private PurchaseResourceIntroductionEntity current(Long tenantId, Long introductionId, Integer indexVersion) {
        return introductionMapper.selectOne(new QueryWrapper<PurchaseResourceIntroductionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", PurchaseResourceIntroductionStatus.PUBLISHED.value())
                .eq("id", introductionId)
                .eq("index_version", indexVersion)
                .last("limit 1"));
    }

    private void updateIndexStatus(
            Long tenantId,
            Long introductionId,
            Integer indexVersion,
            String indexStatus,
            String errorMessage
    ) {
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setIndexStatus(indexStatus);
        entity.setErrorMessage(errorMessage);
        introductionMapper.update(entity, new UpdateWrapper<PurchaseResourceIntroductionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", introductionId)
                .eq("status", PurchaseResourceIntroductionStatus.PUBLISHED.value())
                .eq("index_version", indexVersion));
    }

    private List<String> splitText(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
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

    /**
     * 正文和注意事项分别切片，并为每段保留来源标签。
     * 不能把两类文本拼成一段后再切，否则检索命中时无法判断内容应作为宣传文案还是执行提醒。
     */
    private List<String> splitIntroduction(PurchaseResourceIntroductionEntity introduction) {
        List<String> chunks = new ArrayList<>();
        appendLabeledChunks(chunks, CONTENT_LABEL, introduction.getContent());
        appendLabeledChunks(chunks, NOTICE_LABEL, introduction.getNoticeContent());
        return chunks;
    }

    private void appendLabeledChunks(List<String> chunks, String label, String value) {
        splitText(value).forEach(chunk -> chunks.add(label + "\n" + chunk));
    }

    private int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil(text.length() / 1.8));
    }

    private String safeError(RuntimeException ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "介绍素材向量化失败";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
