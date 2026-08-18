package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.enterprise.companyinfo.dto.EnterpriseCompanyInfoResponse;
import com.mtravel.platform.enterprise.companyinfo.service.EnterpriseCompanyInfoService;
import com.mtravel.platform.sales.booking.aiimport.service.BookingImportAttachmentTextExtractor;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportTaskEntity;
import com.mtravel.platform.sales.team.documentimport.mapper.SalesDocumentImportTaskMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 团队 Word 智能代录处理器测试，保护模型侧脱敏边界。 */
class TeamDocumentImportProcessorTest {

    @Test
    void processAsyncShouldRedactRawIdCardAndPhoneBeforeSendingTextToModel() throws Exception {
        SalesDocumentImportTaskMapper taskMapper = mock(SalesDocumentImportTaskMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        BookingImportAttachmentTextExtractor textExtractor = mock(BookingImportAttachmentTextExtractor.class);
        TeamDocumentImportDraftAssembler assembler = mock(TeamDocumentImportDraftAssembler.class);
        TeamDocumentImportResourceMatcher resourceMatcher = mock(TeamDocumentImportResourceMatcher.class);
        TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer = mock(TeamDocumentImportResourceDraftSanitizer.class);
        TeamDocumentImportCustomerResolver customerResolver = mock(TeamDocumentImportCustomerResolver.class);
        EnterpriseCompanyInfoService companyInfoService = mock(EnterpriseCompanyInfoService.class);
        AtomicReference<String> modelInput = new AtomicReference<>();
        var aiModelClient = new com.mtravel.platform.sales.booking.aiimport.service.AiModelClient() {
            @Override
            public Optional<String> recognize(Long tenantId, String sourceText) {
                modelInput.set(sourceText);
                return Optional.empty();
            }

            @Override
            public Optional<String> recognizeImageOrDocument(Long tenantId, String sourceType, byte[] content) {
                return Optional.empty();
            }
        };
        TeamDocumentImportProcessor processor = new TeamDocumentImportProcessor(
                taskMapper,
                attachmentService,
                textExtractor,
                aiModelClient,
                assembler,
                customerResolver,
                resourceMatcher,
                resourceDraftSanitizer,
                new ObjectMapper(),
                companyInfoService
        );
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(51L);
        task.setTenantId(1L);
        task.setIsDeleted(false);
        task.setStatus("pending");
        task.setAttachmentId(91L);
        task.setSourceType("docx");
        CommonAttachmentEntity attachment = new CommonAttachmentEntity();
        attachment.setFileExt("docx");
        attachment.setOriginalFilename("06.25-悦色江南（地接确认）杭州百缘.docx");
        String extractedText = """
                2026年6月25日 大连-上海 CZ6533（09:10-11:20）
                FROM：东晟假期 刘小川 13800000001
                ATTN：杭州百缘 叶菊莲 13800000002
                游客：张三 210204198206214832
                """;
        TeamDocumentImportDraft assembledDraft = new TeamDocumentImportDraft(
                "ground_confirmation",
                0.9,
                new TeamDocumentImportDraft.TeamDraft("悦色江南", "2026-06-25", 1, 1, null, "domestic", null, null),
                new TeamDocumentImportDraft.OrderDraft(null, null, null, null, null, null, null, null, null, List.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(attachmentService.getEntity(91L, 1L)).thenReturn(attachment);
        when(attachmentService.openStream(91L, 1L)).thenReturn(new ByteArrayInputStream("stub".getBytes(StandardCharsets.UTF_8)));
        when(textExtractor.extract(any(), any(), anyLong())).thenReturn(extractedText);
        when(companyInfoService.current(1L)).thenReturn(new EnterpriseCompanyInfoResponse(
                1L, "杭州百缘国际旅行社有限公司", null, null, null, null, null, null, null,
                null, null, null, null, null, "active", null, "admin", null, null
        ));
        when(assembler.assemble(
                extractedText,
                null,
                "docx",
                "06.25-悦色江南（地接确认）杭州百缘.docx",
                "杭州百缘国际旅行社有限公司",
                java.util.Map.of("[PHONE_1]", "13800000001", "[PHONE_2]", "13800000002")
        )).thenReturn(assembledDraft);
        when(customerResolver.resolveRecognizedCustomer(assembledDraft, 1L)).thenReturn(assembledDraft);
        when(resourceMatcher.match(assembledDraft, 1L)).thenReturn(assembledDraft);
        when(resourceDraftSanitizer.sanitize(assembledDraft, 1L, extractedText)).thenReturn(assembledDraft);

        processor.processAsync(51L, 1L);

        assertThat(modelInput.get()).isNotBlank();
        assertThat(modelInput.get()).doesNotContain("13800000001", "13800000002", "210204198206214832");
        assertThat(modelInput.get()).contains("[PHONE_1]", "[PHONE_2]", "[身份证号]", "【当前企业】杭州百缘国际旅行社有限公司", "【原始文件名】06.25-悦色江南（地接确认）杭州百缘.docx");
        verify(assembler).assemble(
                extractedText,
                null,
                "docx",
                "06.25-悦色江南（地接确认）杭州百缘.docx",
                "杭州百缘国际旅行社有限公司",
                java.util.Map.of("[PHONE_1]", "13800000001", "[PHONE_2]", "13800000002")
        );
        verify(resourceDraftSanitizer).sanitize(assembledDraft, 1L, extractedText);
    }
}
