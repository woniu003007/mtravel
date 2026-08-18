package com.mtravel.platform.purchase.resource.material.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceImageEntity;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceImageMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

/** 验证资源图片的格式校验、封面切换和附件清理规则。 */
class PurchaseResourceImageServiceTest {

    @Test
    void uploadRejectsNonImageFilesBeforeCreatingAnAttachment() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceImageMapper imageMapper = mock(PurchaseResourceImageMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        PurchaseResourceImageService service = service(resourceMapper, imageMapper, attachmentService);
        when(resourceMapper.selectOne(any())).thenReturn(activeResource());

        assertThatThrownBy(() -> service.upload(
                1L,
                21L,
                List.of(new MockMultipartFile("files", "介绍.pdf", "application/pdf", new byte[] {1})),
                "admin"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("图片素材只支持 JPG、JPEG、PNG、WEBP 格式");
        verify(attachmentService, never()).upload(any(), any(), any(), any(), any(), any());
    }

    @Test
    void setCoverUnsetsTheExistingCoverBeforeSettingTheSelectedImage() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceImageMapper imageMapper = mock(PurchaseResourceImageMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        PurchaseResourceImageService service = service(resourceMapper, imageMapper, attachmentService);
        PurchaseResourceImageEntity image = image(41L, 101L, false);
        when(imageMapper.selectOne(any())).thenReturn(image);
        doAnswer(invocation -> {
            PurchaseResourceImageEntity changed = invocation.getArgument(0);
            if (Boolean.TRUE.equals(changed.getIsCover())) {
                image.setIsCover(true);
            }
            return 1;
        }).when(imageMapper).update(any(PurchaseResourceImageEntity.class), any(UpdateWrapper.class));

        var response = service.setCover(1L, 21L, 41L);

        ArgumentCaptor<PurchaseResourceImageEntity> captor =
                ArgumentCaptor.forClass(PurchaseResourceImageEntity.class);
        verify(imageMapper, times(2)).update(captor.capture(), any(UpdateWrapper.class));
        assertThat(captor.getAllValues()).extracting(PurchaseResourceImageEntity::getIsCover)
                .containsExactly(false, true);
        assertThat(response.isCover()).isTrue();
    }

    @Test
    void deleteSoftDeletesTheImageAndItsAttachment() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceImageMapper imageMapper = mock(PurchaseResourceImageMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        PurchaseResourceImageService service = service(resourceMapper, imageMapper, attachmentService);
        when(imageMapper.selectOne(any())).thenReturn(image(41L, 101L, true));
        CommonAttachmentEntity attachment = new CommonAttachmentEntity();
        attachment.setId(101L);
        when(attachmentService.softDelete(101L, 1L, "admin")).thenReturn(attachment);

        service.delete(1L, 21L, 41L, "admin");

        ArgumentCaptor<PurchaseResourceImageEntity> captor =
                ArgumentCaptor.forClass(PurchaseResourceImageEntity.class);
        verify(imageMapper).update(captor.capture(), any(UpdateWrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getIsCover()).isFalse();
        assertThat(captor.getValue().getStatus()).isEqualTo("disabled");
        verify(attachmentService).softDelete(101L, 1L, "admin");
        verify(attachmentService).deletePhysicalFileAfterCommit(attachment);
    }

    private PurchaseResourceImageService service(
            PurchaseResourceMapper resourceMapper,
            PurchaseResourceImageMapper imageMapper,
            CommonAttachmentService attachmentService
    ) {
        return new PurchaseResourceImageService(
                resourceMapper,
                imageMapper,
                attachmentService,
                new ResourceMaterialTagCodec(new ObjectMapper())
        );
    }

    private PurchaseResourceEntity activeResource() {
        PurchaseResourceEntity entity = new PurchaseResourceEntity();
        entity.setId(21L);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private PurchaseResourceImageEntity image(Long id, Long attachmentId, boolean isCover) {
        PurchaseResourceImageEntity entity = new PurchaseResourceImageEntity();
        entity.setId(id);
        entity.setResourceId(21L);
        entity.setAttachmentId(attachmentId);
        entity.setOriginalFilename("景区大门.jpg");
        entity.setFileExt("jpg");
        entity.setFileSize(1024L);
        entity.setTags("[]");
        entity.setIsCover(isCover);
        entity.setSortOrder(0);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }
}
