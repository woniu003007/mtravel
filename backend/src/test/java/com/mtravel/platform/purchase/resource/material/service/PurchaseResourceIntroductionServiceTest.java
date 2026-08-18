package com.mtravel.platform.purchase.resource.material.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionSaveRequest;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionChunkMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 验证介绍素材的草稿、发布和删除不会遗留旧向量。 */
class PurchaseResourceIntroductionServiceTest {

    @Test
    void createCreatesDraftWithoutSubmittingVectorTask() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        PurchaseResourceIntroductionChunkMapper chunkMapper = mock(PurchaseResourceIntroductionChunkMapper.class);
        PurchaseResourceIntroductionProcessor processor = mock(PurchaseResourceIntroductionProcessor.class);
        PurchaseResourceIntroductionService service = service(
                resourceMapper, introductionMapper, chunkMapper, processor
        );
        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setId(21L);
        resource.setStatus("active");
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        doAnswer(invocation -> {
            PurchaseResourceIntroductionEntity entity = invocation.getArgument(0);
            entity.setId(81L);
            return 1;
        }).when(introductionMapper).insert(any(PurchaseResourceIntroductionEntity.class));

        var response = service.create(
                1L,
                21L,
                new PurchaseResourceIntroductionSaveRequest(
                        " 秋季介绍 ", List.of(" 秋季 ", "秋季", "亲子"), " 正文 ", " 请提前预约\n雨天请备雨具 "
                ),
                "admin"
        );

        assertThat(response.id()).isEqualTo(81L);
        assertThat(response.status()).isEqualTo("draft");
        assertThat(response.indexStatus()).isEqualTo("pending");
        assertThat(response.tags()).containsExactly("秋季", "亲子");
        assertThat(response.noticeContent()).isEqualTo("请提前预约\n雨天请备雨具");
        verify(processor, never()).processAsync(any(), any(), any());
    }

    @Test
    void publishClearsOldChunksAndSubmitsTheNextVectorVersion() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        PurchaseResourceIntroductionChunkMapper chunkMapper = mock(PurchaseResourceIntroductionChunkMapper.class);
        PurchaseResourceIntroductionProcessor processor = mock(PurchaseResourceIntroductionProcessor.class);
        PurchaseResourceIntroductionService service = service(
                resourceMapper, introductionMapper, chunkMapper, processor
        );
        PurchaseResourceIntroductionEntity introduction = introduction(81L, "draft", 5);
        when(introductionMapper.selectOne(any())).thenReturn(introduction);
        doAnswer(invocation -> {
            PurchaseResourceIntroductionEntity changed = invocation.getArgument(0);
            introduction.setStatus(changed.getStatus());
            introduction.setIndexStatus(changed.getIndexStatus());
            introduction.setIndexVersion(changed.getIndexVersion());
            introduction.setPublishedAt(changed.getPublishedAt());
            return 1;
        }).when(introductionMapper).update(any(PurchaseResourceIntroductionEntity.class), any(UpdateWrapper.class));

        var response = service.publish(1L, 21L, 81L);

        assertThat(response.status()).isEqualTo("published");
        assertThat(response.indexVersion()).isEqualTo(6);
        verify(chunkMapper).deleteByIntroduction(1L, 81L);
        verify(processor).processAsync(1L, 81L, 6);
    }

    @Test
    void updateClearsBlankNoticeContentAndReturnsTheClearedValue() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        PurchaseResourceIntroductionChunkMapper chunkMapper = mock(PurchaseResourceIntroductionChunkMapper.class);
        PurchaseResourceIntroductionProcessor processor = mock(PurchaseResourceIntroductionProcessor.class);
        PurchaseResourceIntroductionService service = service(
                resourceMapper, introductionMapper, chunkMapper, processor
        );
        PurchaseResourceIntroductionEntity existing = introduction(81L, "published", 5);
        existing.setNoticeContent("请提前预约");
        when(introductionMapper.selectOne(any())).thenReturn(existing);
        ArgumentCaptor<PurchaseResourceIntroductionEntity> captor =
                ArgumentCaptor.forClass(PurchaseResourceIntroductionEntity.class);
        doAnswer(invocation -> {
            PurchaseResourceIntroductionEntity changed = invocation.getArgument(0);
            existing.setTitle(changed.getTitle());
            existing.setContent(changed.getContent());
            existing.setNoticeContent(changed.getNoticeContent());
            existing.setStatus(changed.getStatus());
            existing.setIndexStatus(changed.getIndexStatus());
            existing.setIndexVersion(changed.getIndexVersion());
            return 1;
        }).when(introductionMapper).update(captor.capture(), any(UpdateWrapper.class));

        var response = service.update(
                1L,
                21L,
                81L,
                new PurchaseResourceIntroductionSaveRequest("新介绍", List.of("通用"), "更新后的正文", "   ")
        );

        assertThat(captor.getValue().getNoticeContent()).isNull();
        assertThat(response.noticeContent()).isNull();
        verify(chunkMapper, times(1)).deleteByIntroduction(1L, 81L);
    }

    @Test
    void deleteSoftDeletesIntroductionAndPhysicallyRemovesAllChunks() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceIntroductionMapper introductionMapper = mock(PurchaseResourceIntroductionMapper.class);
        PurchaseResourceIntroductionChunkMapper chunkMapper = mock(PurchaseResourceIntroductionChunkMapper.class);
        PurchaseResourceIntroductionProcessor processor = mock(PurchaseResourceIntroductionProcessor.class);
        PurchaseResourceIntroductionService service = service(
                resourceMapper, introductionMapper, chunkMapper, processor
        );
        when(introductionMapper.selectOne(any())).thenReturn(introduction(81L, "published", 3));
        ArgumentCaptor<PurchaseResourceIntroductionEntity> captor =
                ArgumentCaptor.forClass(PurchaseResourceIntroductionEntity.class);

        service.delete(1L, 21L, 81L, "admin");

        verify(introductionMapper).update(captor.capture(), any(UpdateWrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getStatus()).isEqualTo("disabled");
        assertThat(captor.getValue().getIndexStatus()).isEqualTo("deleted");
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        verify(chunkMapper).deleteByIntroduction(1L, 81L);
    }

    private PurchaseResourceIntroductionService service(
            PurchaseResourceMapper resourceMapper,
            PurchaseResourceIntroductionMapper introductionMapper,
            PurchaseResourceIntroductionChunkMapper chunkMapper,
            PurchaseResourceIntroductionProcessor processor
    ) {
        return new PurchaseResourceIntroductionService(
                resourceMapper,
                introductionMapper,
                chunkMapper,
                processor,
                new ResourceMaterialTagCodec(new ObjectMapper())
        );
    }

    private PurchaseResourceIntroductionEntity introduction(Long id, String status, int indexVersion) {
        PurchaseResourceIntroductionEntity entity = new PurchaseResourceIntroductionEntity();
        entity.setId(id);
        entity.setResourceId(21L);
        entity.setTitle("景区介绍");
        entity.setTags("[]");
        entity.setContent("适用于产品行程的景区介绍正文");
        entity.setStatus(status);
        entity.setIndexStatus("pending");
        entity.setIndexVersion(indexVersion);
        entity.setIsDeleted(false);
        return entity;
    }
}
