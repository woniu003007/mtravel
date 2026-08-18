package com.mtravel.platform.purchase.resource.material.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mtravel.platform.common.knowledge.service.KnowledgeEmbeddingClient;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionChunkEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionChunkMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** 验证资源介绍正文和注意事项会以可辨认的语义标签分别写入向量切片。 */
class PurchaseResourceIntroductionProcessorTest {

    @Test
    void processWritesLabeledChunksForContentAndNotice() {
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        PurchaseResourceIntroductionChunkMapper chunkMapper = mock(PurchaseResourceIntroductionChunkMapper.class);
        KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
        PurchaseResourceIntroductionEntity introduction = publishedIntroduction("西湖景区介绍", "请提前预约\n雨天请注意防滑");
        when(introductionMapper.selectOne(any())).thenReturn(introduction);
        when(embeddingClient.embed(any(), any())).thenReturn(Optional.of("[0.1,0.2]"));
        when(embeddingClient.modelName()).thenReturn("text-embedding-v4");
        PurchaseResourceIntroductionProcessor processor = new PurchaseResourceIntroductionProcessor(
                introductionMapper, chunkMapper, embeddingClient
        );

        processor.processAsync(1L, 81L, 3);

        ArgumentCaptor<PurchaseResourceIntroductionChunkEntity> captor =
                ArgumentCaptor.forClass(PurchaseResourceIntroductionChunkEntity.class);
        verify(chunkMapper, times(2)).insertChunk(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(PurchaseResourceIntroductionChunkEntity::getChunkText)
                .containsExactly(
                        "【资源介绍正文】\n西湖景区介绍",
                        "【注意事项】\n请提前预约\n雨天请注意防滑"
                );
        verify(chunkMapper).deleteByIntroduction(1L, 81L);
    }

    @Test
    void processDoesNotCreateNoticeChunkWhenNoticeIsBlank() {
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        PurchaseResourceIntroductionChunkMapper chunkMapper = mock(PurchaseResourceIntroductionChunkMapper.class);
        KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
        PurchaseResourceIntroductionEntity introduction = publishedIntroduction("西湖景区介绍", "   ");
        when(introductionMapper.selectOne(any())).thenReturn(introduction);
        when(embeddingClient.embed(any(), any())).thenReturn(Optional.of("[0.1,0.2]"));
        when(embeddingClient.modelName()).thenReturn("text-embedding-v4");
        PurchaseResourceIntroductionProcessor processor = new PurchaseResourceIntroductionProcessor(
                introductionMapper, chunkMapper, embeddingClient
        );

        processor.processAsync(1L, 81L, 3);

        ArgumentCaptor<PurchaseResourceIntroductionChunkEntity> captor =
                ArgumentCaptor.forClass(PurchaseResourceIntroductionChunkEntity.class);
        verify(chunkMapper, times(1)).insertChunk(captor.capture());
        assertThat(captor.getValue().getChunkText()).isEqualTo("【资源介绍正文】\n西湖景区介绍");
    }

    @Test
    void processClearsPartialChunksBeforeMarkingFailedWhenLaterChunkThrows() {
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        PurchaseResourceIntroductionChunkMapper chunkMapper = mock(PurchaseResourceIntroductionChunkMapper.class);
        KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
        PurchaseResourceIntroductionEntity introduction = publishedIntroduction("西湖景区介绍", "请提前预约");
        when(introductionMapper.selectOne(any())).thenReturn(introduction);
        when(embeddingClient.embed(eq(1L), eq("【资源介绍正文】\n西湖景区介绍"))).thenReturn(Optional.of("[0.1,0.2]"));
        doThrow(new IllegalStateException("vector service boom"))
                .when(embeddingClient)
                .embed(eq(1L), eq("【注意事项】\n请提前预约"));
        PurchaseResourceIntroductionProcessor processor = new PurchaseResourceIntroductionProcessor(
                introductionMapper, chunkMapper, embeddingClient
        );

        processor.processAsync(1L, 81L, 3);

        verify(chunkMapper, times(1)).insertChunk(any(PurchaseResourceIntroductionChunkEntity.class));
        verify(chunkMapper, times(2)).deleteByIntroduction(1L, 81L);
        ArgumentCaptor<PurchaseResourceIntroductionEntity> entityCaptor =
                ArgumentCaptor.forClass(PurchaseResourceIntroductionEntity.class);
        verify(introductionMapper).update(entityCaptor.capture(), any());
        assertThat(entityCaptor.getValue().getIndexStatus()).isEqualTo("failed");
        assertThat(entityCaptor.getValue().getErrorMessage()).isEqualTo("vector service boom");

        InOrder inOrder = inOrder(chunkMapper, introductionMapper);
        inOrder.verify(chunkMapper).deleteByIntroduction(1L, 81L);
        inOrder.verify(chunkMapper).insertChunk(any(PurchaseResourceIntroductionChunkEntity.class));
        inOrder.verify(chunkMapper).deleteByIntroduction(1L, 81L);
        inOrder.verify(introductionMapper).update(any(PurchaseResourceIntroductionEntity.class), any());
    }

    private PurchaseResourceIntroductionEntity publishedIntroduction(String content, String noticeContent) {
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setId(81L);
        entity.setResourceId(21L);
        entity.setContent(content);
        entity.setNoticeContent(noticeContent);
        entity.setStatus("published");
        entity.setIndexVersion(3);
        entity.setIsDeleted(false);
        return entity;
    }
}
