package com.mtravel.platform.sales.product.designer.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.attachment.dto.AttachmentResponse;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.sales.product.designer.entity.SalesProductAdultQuoteEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDocumentVersionEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductAdultQuoteMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceImageMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDocumentVersionMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductDescriptionMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Assumptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 产品设计工作台对外文档生成测试。 */
class SalesProductDesignerDocumentServiceTest {

    @Test
    void productWordShouldUseProductIntroductionSnapshotAndPersistVersion() throws Exception {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDayResourceImageMapper dayResourceImageMapper = mock(SalesProductDayResourceImageMapper.class);
        SalesProductAdultQuoteMapper adultQuoteMapper = mock(SalesProductAdultQuoteMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductItineraryDayMapper itineraryDayMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductDocumentVersionMapper versionMapper = mock(SalesProductDocumentVersionMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        SalesProductDesignerDocumentService service = service(
                productMapper, descriptionMapper, itineraryDayMapper, dayResourceMapper,
                dayResourceImageMapper, adultQuoteMapper, versionMapper, attachmentService
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(resource()));
        SalesProductDescriptionEntity description = new SalesProductDescriptionEntity();
        description.setProductDescription("适合常规团队的西湖线路。");
        description.setFeeIncluded("行程内首道门票");
        description.setWarmReminder("旺季请提前确认");
        when(descriptionMapper.selectOne(any(Wrapper.class))).thenReturn(description);
        SalesProductItineraryDayEntity itinerary = new SalesProductItineraryDayEntity();
        itinerary.setDayNo(1);
        itinerary.setDayTitle("杭州接团游览西湖");
        itinerary.setItineraryContent("沿湖游览并安排自由活动。");
        itinerary.setBreakfastIncluded(true);
        when(itineraryDayMapper.selectList(any(Wrapper.class))).thenReturn(List.of(itinerary));
        when(dayResourceImageMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(versionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(attachmentService.uploadBytes(
                any(byte[].class), anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong(), anyString()
        )).thenReturn(uploadedAttachment());
        when(versionMapper.insert(any(SalesProductDocumentVersionEntity.class))).thenAnswer(invocation -> {
            SalesProductDocumentVersionEntity entity = invocation.getArgument(0);
            entity.setId(701L);
            return 1;
        });

        var response = service.productWord(1L, 88L, "admin");

        assertThat(response.documentType()).isEqualTo("product_word");
        assertThat(response.versionNo()).isEqualTo(1);
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(attachmentService).uploadBytes(
                bytesCaptor.capture(), eq("西湖三日产品-产品介绍-v1.docx"), anyString(), anyString(), anyString(), eq(88L), eq(1L), eq("admin")
        );
        assertThat(bytesCaptor.getValue()).isNotEmpty();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytesCaptor.getValue()))) {
            String text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (left, right) -> left + right);
            assertThat(text).contains("漫步西湖", "费用包含", "行程内首道门票", "杭州接团游览西湖", "雨天注意防滑", "请勿下水");
            assertThat(text).doesNotContain("供应商内部成本");
            assertThat(document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> "雨天注意防滑".equals(run.text()) || "请勿下水".equals(run.text()))
                    .map(org.apache.poi.xwpf.usermodel.XWPFRun::getColor))
                    .containsOnly("C00000");
            assertThat(document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> "沿湖步行，感受杭州的城市风景。".equals(run.text()))
                    .map(org.apache.poi.xwpf.usermodel.XWPFRun::getColor))
                    .containsOnlyNulls();
            assertThat(document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .map(run -> run.getFontFamily(org.apache.poi.xwpf.usermodel.XWPFRun.FontCharRange.eastAsia)))
                    .contains("Noto Sans SC");
            assertThat(document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .map(run -> run.getCTR().getRPr().getRFontsArray(0).getHint().toString()))
                    .contains("eastAsia");
        }
        try (ZipFile zipFile = new ZipFile(Files.write(Files.createTempFile("product-word-", ".docx"), bytesCaptor.getValue()).toFile())) {
            assertThat(zipFile.getEntry("word/fontTable.xml")).isNotNull();
            assertThat(zipFile.getEntry("word/fonts/NotoSansSC.odttf")).isNotNull();
        }

        ArgumentCaptor<SalesProductDocumentVersionEntity> versionCaptor =
                ArgumentCaptor.forClass(SalesProductDocumentVersionEntity.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getSourceSnapshot()).contains("漫步西湖", "雨天注意防滑");
        assertThat(versionCaptor.getValue().getSourceSnapshot()).doesNotContain("供应商内部成本");
    }

    @Test
    void adultQuoteShouldOnlyExposeExternalPrice() throws Exception {
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDayResourceImageMapper dayResourceImageMapper = mock(SalesProductDayResourceImageMapper.class);
        SalesProductAdultQuoteMapper adultQuoteMapper = mock(SalesProductAdultQuoteMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductItineraryDayMapper itineraryDayMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductDocumentVersionMapper versionMapper = mock(SalesProductDocumentVersionMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        SalesProductDesignerDocumentService service = service(
                productMapper, descriptionMapper, itineraryDayMapper, dayResourceMapper,
                dayResourceImageMapper, adultQuoteMapper, versionMapper, attachmentService
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(adultQuoteMapper.selectList(any(Wrapper.class))).thenReturn(List.of(quote()));
        when(versionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(attachmentService.uploadBytes(
                any(byte[].class), anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong(), anyString()
        )).thenReturn(uploadedAttachment());
        when(versionMapper.insert(any(SalesProductDocumentVersionEntity.class))).thenAnswer(invocation -> {
            SalesProductDocumentVersionEntity entity = invocation.getArgument(0);
            entity.setId(702L);
            return 1;
        });

        var response = service.adultQuote(1L, 88L, "admin");

        assertThat(response.documentType()).isEqualTo("adult_quote");
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(attachmentService).uploadBytes(
                bytesCaptor.capture(), eq("西湖三日产品-成人报价单-v1.docx"), anyString(), anyString(), anyString(), eq(88L), eq(1L), eq("admin")
        );
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytesCaptor.getValue()))) {
            String text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (left, right) -> left + right);
            String tableText = document.getTables().stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(row -> row.getTableCells().stream())
                    .map(cell -> cell.getText())
                    .reduce("", (left, right) -> left + right);
            assertThat(text + tableText).contains("成人对外价", "120.00");
            assertThat(text + tableText).doesNotContain("供应商", "内部成本", "成本金额");
            assertThat(document.getTables().stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(row -> row.getTableCells().stream())
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .map(run -> run.getFontFamily(org.apache.poi.xwpf.usermodel.XWPFRun.FontCharRange.eastAsia)))
                    .contains("Noto Sans SC");
            assertThat(document.getTables().getFirst().getWidth()).isEqualTo(9360);
        }
    }

    @Test
    void writeRenderSamplesWhenOutputDirProvided() throws Exception {
        String outputDir = System.getProperty("productDesignerDocxOutputDir");
        Assumptions.assumeTrue(outputDir != null && !outputDir.isBlank());

        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesProductDayResourceMapper dayResourceMapper = mock(SalesProductDayResourceMapper.class);
        SalesProductDayResourceImageMapper dayResourceImageMapper = mock(SalesProductDayResourceImageMapper.class);
        SalesProductAdultQuoteMapper adultQuoteMapper = mock(SalesProductAdultQuoteMapper.class);
        SalesProductDescriptionMapper descriptionMapper = mock(SalesProductDescriptionMapper.class);
        SalesProductItineraryDayMapper itineraryDayMapper = mock(SalesProductItineraryDayMapper.class);
        SalesProductDocumentVersionMapper versionMapper = mock(SalesProductDocumentVersionMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        SalesProductDesignerDocumentService service = service(
                productMapper, descriptionMapper, itineraryDayMapper, dayResourceMapper,
                dayResourceImageMapper, adultQuoteMapper, versionMapper, attachmentService
        );
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(dayResourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(resource()));
        SalesProductDescriptionEntity description = new SalesProductDescriptionEntity();
        description.setProductDescription("适合常规团队的西湖线路，包含经典景区与城市文化体验。");
        description.setFeeIncluded("行程内首道门票、当地用车、导游服务。");
        description.setFeeExcluded("个人消费、单房差及合同未列明项目。");
        description.setWarmReminder("旺季请提前确认酒店与景区预约名额。");
        when(descriptionMapper.selectOne(any(Wrapper.class))).thenReturn(description);
        SalesProductItineraryDayEntity itinerary = new SalesProductItineraryDayEntity();
        itinerary.setDayNo(1);
        itinerary.setDayTitle("杭州接团游览西湖");
        itinerary.setItineraryContent("沿湖游览并安排自由活动，傍晚返回酒店休息。");
        itinerary.setBreakfastIncluded(true);
        itinerary.setLunchIncluded(true);
        itinerary.setRelatedHotel("杭州西湖武林四钻酒店");
        itinerary.setRoadbookPlace("杭州西湖风景名胜区 -> 浙大森林");
        when(itineraryDayMapper.selectList(any(Wrapper.class))).thenReturn(List.of(itinerary));
        when(dayResourceImageMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(adultQuoteMapper.selectList(any(Wrapper.class))).thenReturn(List.of(quote()));
        when(versionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        Path targetDir = Path.of(outputDir);
        Files.createDirectories(targetDir);
        when(attachmentService.uploadBytes(
                any(byte[].class), anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong(), anyString()
        )).thenAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            String fileName = invocation.getArgument(1);
            Files.write(targetDir.resolve(fileName), bytes);
            return new AttachmentResponse(
                    501L, "销售管理", "产品对外文档", 88L, fileName, "/" + fileName,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    (long) bytes.length, "docx", "active", "admin", null
            );
        });
        when(versionMapper.insert(any(SalesProductDocumentVersionEntity.class))).thenAnswer(invocation -> {
            SalesProductDocumentVersionEntity entity = invocation.getArgument(0);
            entity.setId(700L + entity.getVersionNo());
            return 1;
        });

        service.productWord(1L, 88L, "admin");
        service.adultQuote(1L, 88L, "admin");

        assertThat(Files.list(targetDir).filter(path -> path.toString().endsWith(".docx")).count()).isEqualTo(2);
    }

    private SalesProductDesignerDocumentService service(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesProductDayResourceMapper dayResourceMapper,
            SalesProductDayResourceImageMapper dayResourceImageMapper,
            SalesProductAdultQuoteMapper adultQuoteMapper,
            SalesProductDocumentVersionMapper versionMapper,
            CommonAttachmentService attachmentService
    ) {
        return new SalesProductDesignerDocumentService(
                productMapper, descriptionMapper, itineraryDayMapper, dayResourceMapper,
                dayResourceImageMapper, adultQuoteMapper, versionMapper,
                attachmentService, new ObjectMapper()
        );
    }

    private SalesProductEntity product() {
        SalesProductEntity entity = new SalesProductEntity();
        entity.setId(88L);
        entity.setTenantId(1L);
        entity.setProductName("西湖三日产品");
        entity.setProductScope("template");
        entity.setProvince("浙江省");
        entity.setCity("杭州市");
        entity.setTravelDays(3);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private SalesProductDayResourceEntity resource() {
        SalesProductDayResourceEntity entity = new SalesProductDayResourceEntity();
        entity.setId(301L);
        entity.setProductId(88L);
        entity.setDayNo(1);
        entity.setResourceNameSnapshot("西湖景区");
        entity.setIncludeInWord(true);
        entity.setStayMinutes(120);
        entity.setIntroductionTitleSnapshot("漫步西湖");
        entity.setIntroductionContentSnapshot("沿湖步行，感受杭州的城市风景。");
        entity.setIntroductionNoticeSnapshot("雨天注意防滑\n\n请勿下水");
        entity.setSupplierNameSnapshot("供应商内部成本");
        entity.setCostAmountSnapshot(new BigDecimal("88.00"));
        return entity;
    }

    private SalesProductAdultQuoteEntity quote() {
        SalesProductAdultQuoteEntity entity = new SalesProductAdultQuoteEntity();
        entity.setId(401L);
        entity.setProductId(88L);
        entity.setPlannedAdultCount(30);
        entity.setAdultCostAmount(new BigDecimal("88.00"));
        entity.setMarkupAmount(new BigDecimal("32.00"));
        entity.setAdultSaleAmount(new BigDecimal("120.00"));
        entity.setValidUntil(LocalDate.of(2026, 12, 31));
        entity.setQuoteRemark("以最终确认单为准");
        entity.setStatus("draft");
        return entity;
    }

    private AttachmentResponse uploadedAttachment() {
        return new AttachmentResponse(
                501L, "销售管理", "产品对外文档", 88L, "generated.docx", "/generated.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 10L, "docx", "active", "admin", null
        );
    }
}
