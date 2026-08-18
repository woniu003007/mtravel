package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.sales.booking.aiimport.service.BookingImportAttachmentTextExtractor;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportTaskResponse;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportTaskUpdateRequest;
import com.mtravel.platform.sales.team.documentimport.entity.SalesDocumentImportTaskEntity;
import com.mtravel.platform.sales.team.documentimport.mapper.SalesDocumentImportTaskMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 团队 Word 任务草稿保存测试，覆盖旧草稿的客户主档迁移和保存拦截。 */
class TeamDocumentImportTaskServiceTest {

    @Test
    void detailShouldNormalizeLegacyUnappliedDraftSoItDoesNotReturnFreeTextCustomerName() throws Exception {
        SalesDocumentImportTaskMapper taskMapper = mock(SalesDocumentImportTaskMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TeamDocumentImportTaskService service = new TeamDocumentImportTaskService(
                taskMapper,
                mock(CommonAttachmentService.class),
                mock(TeamDocumentImportProcessor.class),
                new TeamDocumentImportCustomerResolver(customerMapper),
                resourceDraftSanitizer(mock(PurchaseResourceMapper.class)),
                mock(BookingImportAttachmentTextExtractor.class),
                mock(TeamDocumentImportDraftAssembler.class),
                objectMapper
        );
        // 数据库中任务 26 这类旧草稿没有 customerId 字段，必须仍能被安全解析和规范化。
        SalesDocumentImportTaskEntity task = task(26L, legacyDraftJson());
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(customerMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.update(any(SalesDocumentImportTaskEntity.class), any(Wrapper.class))).thenReturn(1);

        TeamDocumentImportTaskResponse response = service.detail(26L, 1L);

        assertThat(response.draft().order().customerId()).isNull();
        assertThat(response.draft().order().customerName()).isNull();
        assertThat(response.draft().warnings()).anyMatch(item -> item.contains("东晟假期") && item.contains("客户主档"));
        verify(taskMapper).update(any(SalesDocumentImportTaskEntity.class), any(Wrapper.class));
    }

    @Test
    void detailShouldBackfillEmptyProductDescriptionFromAttachmentOnlyOnceWithoutReprocessing() throws Exception {
        SalesDocumentImportTaskMapper taskMapper = mock(SalesDocumentImportTaskMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        TeamDocumentImportProcessor processor = mock(TeamDocumentImportProcessor.class);
        BookingImportAttachmentTextExtractor textExtractor = mock(BookingImportAttachmentTextExtractor.class);
        TeamDocumentImportDraftAssembler assembler = mock(TeamDocumentImportDraftAssembler.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TeamDocumentImportTaskService service = new TeamDocumentImportTaskService(
                taskMapper,
                attachmentService,
                processor,
                new TeamDocumentImportCustomerResolver(mock(CustomerUnitMapper.class)),
                resourceDraftSanitizer(mock(PurchaseResourceMapper.class)),
                textExtractor,
                assembler,
                objectMapper
        );
        TeamDocumentImportDraft legacyDraft = new TeamDocumentImportDraft(
                "ground_confirmation", 0.9, null, null, List.of(), List.of(), List.of(), List.of(), List.of(),
                new TeamDocumentImportDraft.ProductDescriptionDraft(null, " ", null, null, null, null, null, null, null)
        );
        SalesDocumentImportTaskEntity task = task(29L, objectMapper.writeValueAsString(legacyDraft));
        task.setAttachmentId(91L);
        CommonAttachmentEntity attachment = new CommonAttachmentEntity();
        attachment.setFileExt("docx");
        TeamDocumentImportDraft.ProductDescriptionDraft extracted = new TeamDocumentImportDraft.ProductDescriptionDraft(
                null, null, null, null, null, null, null, null, "请携带身份证原件"
        );
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(attachmentService.getEntity(91L, 1L)).thenReturn(attachment);
        when(attachmentService.openStream(91L, 1L)).thenReturn(
                new ByteArrayInputStream("温馨提醒\n请携带身份证原件".getBytes(StandardCharsets.UTF_8))
        );
        when(textExtractor.extract(any(), any(), any())).thenReturn("温馨提醒\n请携带身份证原件");
        when(assembler.extractProductDescriptionFromSource("温馨提醒\n请携带身份证原件")).thenReturn(extracted);
        when(taskMapper.update(any(SalesDocumentImportTaskEntity.class), any(Wrapper.class))).thenReturn(1);

        TeamDocumentImportTaskResponse first = service.detail(29L, 1L);
        TeamDocumentImportTaskResponse second = service.detail(29L, 1L);

        assertThat(first.draft().productDescription().warmReminder()).isEqualTo("请携带身份证原件");
        assertThat(second.draft().productDescription().warmReminder()).isEqualTo("请携带身份证原件");
        ArgumentCaptor<SalesDocumentImportTaskEntity> updateCaptor = ArgumentCaptor.forClass(SalesDocumentImportTaskEntity.class);
        verify(taskMapper).update(updateCaptor.capture(), any(Wrapper.class));
        TeamDocumentImportDraft persisted = objectMapper.readValue(
                updateCaptor.getValue().getDraftJson(), TeamDocumentImportDraft.class
        );
        assertThat(persisted.productDescription().warmReminder()).isEqualTo("请携带身份证原件");
        verify(attachmentService).openStream(91L, 1L);
        verify(textExtractor).extract(any(), any(), any());
        verify(assembler).extractProductDescriptionFromSource("温馨提醒\n请携带身份证原件");
        verifyNoInteractions(processor);
    }

    @Test
    void detailShouldNeverBackfillProductDescriptionForAppliedTask() throws Exception {
        SalesDocumentImportTaskMapper taskMapper = mock(SalesDocumentImportTaskMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        BookingImportAttachmentTextExtractor textExtractor = mock(BookingImportAttachmentTextExtractor.class);
        TeamDocumentImportDraftAssembler assembler = mock(TeamDocumentImportDraftAssembler.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TeamDocumentImportTaskService service = new TeamDocumentImportTaskService(
                taskMapper,
                attachmentService,
                mock(TeamDocumentImportProcessor.class),
                new TeamDocumentImportCustomerResolver(mock(CustomerUnitMapper.class)),
                resourceDraftSanitizer(mock(PurchaseResourceMapper.class)),
                textExtractor,
                assembler,
                objectMapper
        );
        SalesDocumentImportTaskEntity task = task(30L, objectMapper.writeValueAsString(new TeamDocumentImportDraft(
                "ground_confirmation", 0.9, null, null, List.of(), List.of(), List.of(), List.of(), List.of()
        )));
        task.setStatus("applied");
        task.setAttachmentId(91L);
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);

        TeamDocumentImportTaskResponse response = service.detail(30L, 1L);

        assertThat(response.draft().productDescription()).isNull();
        verifyNoInteractions(attachmentService, textExtractor, assembler);
        verify(taskMapper, never()).update(any(SalesDocumentImportTaskEntity.class), any(Wrapper.class));
    }

    @Test
    void updateDraftShouldRejectFreeTextCustomerNameBeforePersistingTheDraft() {
        SalesDocumentImportTaskMapper taskMapper = mock(SalesDocumentImportTaskMapper.class);
        TeamDocumentImportTaskService service = new TeamDocumentImportTaskService(
                taskMapper,
                mock(CommonAttachmentService.class),
                mock(TeamDocumentImportProcessor.class),
                new TeamDocumentImportCustomerResolver(mock(CustomerUnitMapper.class)),
                resourceDraftSanitizer(mock(PurchaseResourceMapper.class)),
                mock(BookingImportAttachmentTextExtractor.class),
                mock(TeamDocumentImportDraftAssembler.class),
                new ObjectMapper()
        );
        SalesDocumentImportTaskEntity task = task(26L, null);
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);

        assertThatThrownBy(() -> service.updateDraft(26L, new TeamDocumentImportTaskUpdateRequest(draft(null, "东晟假期")), 1L))
                .isInstanceOf(BizException.class)
                .hasMessage("客户单位必须从系统客户主档选择，不能直接填写名称");
        verify(taskMapper, never()).update(any(SalesDocumentImportTaskEntity.class), any(Wrapper.class));
    }

    @Test
    void detailShouldRemoveLegacySelectedNotRequiredResourceAndPersistTheSanitizedDraft() throws Exception {
        SalesDocumentImportTaskMapper taskMapper = mock(SalesDocumentImportTaskMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TeamDocumentImportTaskService service = new TeamDocumentImportTaskService(
                taskMapper,
                mock(CommonAttachmentService.class),
                mock(TeamDocumentImportProcessor.class),
                new TeamDocumentImportCustomerResolver(mock(CustomerUnitMapper.class)),
                resourceDraftSanitizer(resourceMapper),
                mock(BookingImportAttachmentTextExtractor.class),
                mock(TeamDocumentImportDraftAssembler.class),
                objectMapper
        );
        TeamDocumentImportDraft legacyDraft = new TeamDocumentImportDraft(
                "ground_confirmation", 0.9, null, null, List.of(), List.of(),
                List.of(new TeamDocumentImportDraft.ResourceDraft(
                        "resource:free", 1, null, "scenic", "西湖", "杭州", null,
                        88L, "西湖", null, null, false, List.of()
                )), List.of(), List.of()
        );
        SalesDocumentImportTaskEntity task = task(29L, objectMapper.writeValueAsString(legacyDraft));
        PurchaseResourceEntity freeScenic = new PurchaseResourceEntity();
        freeScenic.setId(88L);
        freeScenic.setProcurementMode("not_required");
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(resourceMapper.selectList(any())).thenReturn(List.of(freeScenic));
        when(taskMapper.update(any(SalesDocumentImportTaskEntity.class), any(Wrapper.class))).thenReturn(1);

        TeamDocumentImportTaskResponse response = service.detail(29L, 1L);

        assertThat(response.draft().resources()).isEmpty();
        verify(taskMapper).update(any(SalesDocumentImportTaskEntity.class), any(Wrapper.class));
    }

    @Test
    void updateDraftShouldRemoveNotRequiredResourceBeforePersisting() throws Exception {
        SalesDocumentImportTaskMapper taskMapper = mock(SalesDocumentImportTaskMapper.class);
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TeamDocumentImportTaskService service = new TeamDocumentImportTaskService(
                taskMapper,
                mock(CommonAttachmentService.class),
                mock(TeamDocumentImportProcessor.class),
                new TeamDocumentImportCustomerResolver(mock(CustomerUnitMapper.class)),
                resourceDraftSanitizer(resourceMapper),
                mock(BookingImportAttachmentTextExtractor.class),
                mock(TeamDocumentImportDraftAssembler.class),
                objectMapper
        );
        SalesDocumentImportTaskEntity task = task(29L, null);
        TeamDocumentImportDraft submitted = new TeamDocumentImportDraft(
                "ground_confirmation", 0.9, null, null, List.of(), List.of(),
                List.of(new TeamDocumentImportDraft.ResourceDraft(
                        "resource:free", 1, null, "scenic", "西湖", "杭州", null,
                        88L, "西湖", null, null, false, List.of()
                )), List.of(), List.of()
        );
        PurchaseResourceEntity freeScenic = new PurchaseResourceEntity();
        freeScenic.setId(88L);
        freeScenic.setProcurementMode("not_required");
        when(taskMapper.selectOne(any(Wrapper.class))).thenReturn(task);
        when(resourceMapper.selectList(any())).thenReturn(List.of(freeScenic));
        when(taskMapper.update(any(SalesDocumentImportTaskEntity.class), any(Wrapper.class))).thenReturn(1);

        service.updateDraft(29L, new TeamDocumentImportTaskUpdateRequest(submitted), 1L);

        ArgumentCaptor<SalesDocumentImportTaskEntity> captor = ArgumentCaptor.forClass(SalesDocumentImportTaskEntity.class);
        verify(taskMapper).update(captor.capture(), any(Wrapper.class));
        TeamDocumentImportDraft persisted = objectMapper.readValue(captor.getValue().getDraftJson(), TeamDocumentImportDraft.class);
        assertThat(persisted.resources()).isEmpty();
    }

    private SalesDocumentImportTaskEntity task(Long id, String draftJson) {
        SalesDocumentImportTaskEntity task = new SalesDocumentImportTaskEntity();
        task.setId(id);
        task.setTenantId(1L);
        task.setStatus("reviewing");
        task.setDraftJson(draftJson);
        task.setIsDeleted(false);
        return task;
    }

    private String legacyDraftJson() {
        return """
                {
                  "documentType":"ground_confirmation",
                  "confidence":0.9,
                  "order":{
                    "customerName":"东晟假期",
                    "contactName":"刘小川",
                    "contactPhone":"13840835417",
                    "priceLines":[]
                  },
                  "guests":[],
                  "itineraryDays":[],
                  "resources":[],
                  "warnings":[],
                  "evidence":[]
                }
                """;
    }

    private TeamDocumentImportDraft draft(Long customerId, String customerName) {
        return new TeamDocumentImportDraft(
                "ground_confirmation",
                0.9,
                new TeamDocumentImportDraft.TeamDraft("悦色江南", "2026-06-25", 6, 17, null, "domestic", null, null),
                new TeamDocumentImportDraft.OrderDraft(
                        customerId,
                        customerName,
                        "刘小川",
                        "13840835417",
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of()
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private TeamDocumentImportResourceDraftSanitizer resourceDraftSanitizer(PurchaseResourceMapper resourceMapper) {
        return new TeamDocumentImportResourceDraftSanitizer(
                new TeamDocumentImportResourceNormalizer(),
                new TeamDocumentImportBusinessPartyNameExtractor(),
                resourceMapper
        );
    }
}
