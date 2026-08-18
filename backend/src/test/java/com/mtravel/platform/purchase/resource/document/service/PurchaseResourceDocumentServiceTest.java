package com.mtravel.platform.purchase.resource.document.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.attachment.dto.AttachmentResponse;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.common.knowledge.entity.KnowledgeDocumentEntity;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeDocumentChunkMapper;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeDocumentMapper;
import com.mtravel.platform.common.knowledge.mapper.KnowledgeProcessingTaskMapper;
import com.mtravel.platform.common.knowledge.service.KnowledgeDocumentProcessor;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 采购资源资料服务测试。 */
class PurchaseResourceDocumentServiceTest {

    @Test
    void uploadShouldAllowHotelAndUseGenericResourceSource() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeDocumentChunkMapper chunkMapper = mock(KnowledgeDocumentChunkMapper.class);
        KnowledgeProcessingTaskMapper taskMapper = mock(KnowledgeProcessingTaskMapper.class);
        KnowledgeDocumentProcessor processor = mock(KnowledgeDocumentProcessor.class);
        PurchaseResourceDocumentService service = new PurchaseResourceDocumentService(
                resourceMapper, attachmentService, documentMapper, chunkMapper, taskMapper, processor
        );

        PurchaseResourceEntity hotel = new PurchaseResourceEntity();
        hotel.setId(21L);
        hotel.setTenantId(1L);
        hotel.setResourceType("hotel");
        hotel.setResourceName("测试酒店");
        when(resourceMapper.selectOne(any(Wrapper.class))).thenReturn(hotel);

        AttachmentResponse uploaded = new AttachmentResponse(
                31L, "采购管理", "资源资料", 21L, "hotel.pdf", "/hotel.pdf",
                "application/pdf", 7L, "pdf", "active", "admin", null
        );
        when(attachmentService.upload(
                any(), eq("采购管理"), eq("资源资料"), eq(21L), eq(1L), eq("admin")
        )).thenReturn(uploaded);
        CommonAttachmentEntity attachment = new CommonAttachmentEntity();
        attachment.setId(31L);
        attachment.setFileSize(7L);
        when(attachmentService.getEntity(31L, 1L)).thenReturn(attachment);
        when(documentMapper.insert(any(KnowledgeDocumentEntity.class))).thenAnswer(invocation -> {
            KnowledgeDocumentEntity document = invocation.getArgument(0);
            document.setId(41L);
            return 1;
        });

        var result = service.upload(
                1L,
                21L,
                List.of(new MockMultipartFile("files", "hotel.pdf", "application/pdf", "content".getBytes())),
                "admin"
        );

        assertThat(result).singleElement().satisfies(document -> {
            assertThat(document.sourceType()).isEqualTo("purchase_resource");
            assertThat(document.sourceId()).isEqualTo(21L);
            assertThat(document.usageProductManual()).isTrue();
        });
        verify(processor).processAsync(41L, 1L, 1);
    }
}
