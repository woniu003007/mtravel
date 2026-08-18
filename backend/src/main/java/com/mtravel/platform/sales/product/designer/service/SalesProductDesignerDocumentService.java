package com.mtravel.platform.sales.product.designer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.sales.product.designer.dto.ProductDesignerDocumentVersionResponse;
import com.mtravel.platform.sales.product.designer.entity.SalesProductAdultQuoteEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceImageEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDocumentVersionEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductAdultQuoteMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceImageMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDocumentVersionMapper;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductDescriptionMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import java.io.ByteArrayInputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLanguage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHint;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

/**
 * 产品设计工作台对外文档服务。
 *
 * <p>服务先读取产品侧快照，再生成 Word 并写入公共附件。对外文档不包含供应商名称、供应商报价和内部成本，
 * 生成完成后资源主档的后续修改不会改变历史版本。</p>
 */
@Service
public class SalesProductDesignerDocumentService {

    private static final String PRODUCT_WORD = "product_word";
    private static final String ADULT_QUOTE = "adult_quote";
    private static final String SUCCESS = "success";
    // Explicitly mark generated runs as East Asian text. A blank XWPFDocument otherwise leaves
    // LibreOffice to guess the script and it can embed a non-CJK fallback font for Chinese text.
    private static final String DOCUMENT_FONT = "Noto Sans SC";
    private static final String DOCUMENT_FONT_RESOURCE = "/fonts/NotoSansSC.ttf";
    private static final String DOCUMENT_FONT_KEY = "00112233-4455-6677-8899-aabbccddeeff";
    private static final int PAGE_USABLE_WIDTH_DXA = 9360;

    private final SalesProductMapper productMapper;
    private final SalesProductDescriptionMapper descriptionMapper;
    private final SalesProductItineraryDayMapper itineraryDayMapper;
    private final SalesProductDayResourceMapper dayResourceMapper;
    private final SalesProductDayResourceImageMapper dayResourceImageMapper;
    private final SalesProductAdultQuoteMapper adultQuoteMapper;
    private final SalesProductDocumentVersionMapper versionMapper;
    private final CommonAttachmentService attachmentService;
    private final ObjectMapper objectMapper;

    public SalesProductDesignerDocumentService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesProductDayResourceMapper dayResourceMapper,
            SalesProductDayResourceImageMapper dayResourceImageMapper,
            SalesProductAdultQuoteMapper adultQuoteMapper,
            SalesProductDocumentVersionMapper versionMapper,
            CommonAttachmentService attachmentService,
            ObjectMapper objectMapper
    ) {
        this.productMapper = productMapper;
        this.descriptionMapper = descriptionMapper;
        this.itineraryDayMapper = itineraryDayMapper;
        this.dayResourceMapper = dayResourceMapper;
        this.dayResourceImageMapper = dayResourceImageMapper;
        this.adultQuoteMapper = adultQuoteMapper;
        this.versionMapper = versionMapper;
        this.attachmentService = attachmentService;
        this.objectMapper = objectMapper;
    }

    /** 生成产品介绍 Word，并保留本次资源介绍正文快照。 */
    @Transactional
    public ProductDesignerDocumentVersionResponse productWord(Long tenantId, Long productId, String operator) {
        SalesProductEntity product = loadProduct(tenantId, productId);
        List<SalesProductDayResourceEntity> resources = resources(tenantId, productId);
        SalesProductDescriptionEntity description = description(tenantId, productId);
        List<SalesProductItineraryDayEntity> itineraryDays = itineraryDays(tenantId, productId);
        List<SalesProductDayResourceImageEntity> selectedImages = selectedImages(tenantId, productId);
        int versionNo = nextVersion(tenantId, productId, PRODUCT_WORD);
        String fileName = safeFileName(product.getProductName()) + "-产品介绍-v" + versionNo + ".docx";
        String snapshot = snapshot(product, resources, null, description, itineraryDays, selectedImages);
        byte[] bytes = buildProductWord(tenantId, product, resources, description, itineraryDays, selectedImages);
        ProductDesignerDocumentVersionResponse result = persistGenerated(
                tenantId, productId, PRODUCT_WORD, versionNo, fileName, snapshot, bytes, operator
        );
        return result;
    }

    /** 生成成人报价单，只输出成人数、成人对外价和对外备注。 */
    @Transactional
    public ProductDesignerDocumentVersionResponse adultQuote(Long tenantId, Long productId, String operator) {
        SalesProductEntity product = loadProduct(tenantId, productId);
        SalesProductAdultQuoteEntity quote = adultQuoteMapper.selectList(new QueryWrapper<SalesProductAdultQuoteEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", productId)
                        .in("status", List.of("draft", "confirmed"))
                        .orderByDesc("id")
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BizException("请先保存成人报价草稿"));
        int versionNo = nextVersion(tenantId, productId, ADULT_QUOTE);
        String fileName = safeFileName(product.getProductName()) + "-成人报价单-v" + versionNo + ".docx";
        String snapshot = snapshot(product, List.of(), quote, null, List.of(), List.of());
        byte[] bytes = buildAdultQuoteWord(product, quote);
        return persistGenerated(tenantId, productId, ADULT_QUOTE, versionNo, fileName, snapshot, bytes, operator);
    }

    /** 查询产品的 Word 和成人报价单版本。 */
    public List<ProductDesignerDocumentVersionResponse> list(Long tenantId, Long productId) {
        loadProduct(tenantId, productId);
        return versionMapper.selectList(new QueryWrapper<SalesProductDocumentVersionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", productId)
                        .orderByDesc("id"))
                .stream()
                .map(ProductDesignerDocumentVersionResponse::fromEntity)
                .toList();
    }

    /** 下载当前租户下的已生成文档版本。 */
    public ResponseEntity<InputStreamResource> download(Long tenantId, Long versionId) {
        SalesProductDocumentVersionEntity version = versionMapper.selectOne(new QueryWrapper<SalesProductDocumentVersionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("generate_status", SUCCESS)
                .eq("id", versionId));
        if (version == null || version.getAttachmentId() == null) {
            throw new BizException("文档版本不存在或尚未生成");
        }
        CommonAttachmentEntity attachment = attachmentService.getEntity(version.getAttachmentId(), tenantId);
        InputStream input = attachmentService.openStream(attachment.getId(), tenantId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(StringUtils.hasText(attachment.getContentType())
                        ? attachment.getContentType()
                        : "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(version.getFileNameSnapshot(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(input));
    }

    private ProductDesignerDocumentVersionResponse persistGenerated(
            Long tenantId,
            Long productId,
            String documentType,
            int versionNo,
            String fileName,
            String snapshot,
            byte[] bytes,
            String operator
    ) {
        var attachment = attachmentService.uploadBytes(
                bytes,
                fileName,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "销售管理",
                "产品对外文档",
                productId,
                tenantId,
                operator
        );
        SalesProductDocumentVersionEntity entity = new SalesProductDocumentVersionEntity();
        entity.setTenantId(tenantId);
        entity.setProductId(productId);
        entity.setDocumentType(documentType);
        entity.setVersionNo(versionNo);
        entity.setSourceSnapshot(snapshot);
        entity.setAttachmentId(attachment.id());
        entity.setFileNameSnapshot(fileName);
        entity.setGenerateStatus(SUCCESS);
        entity.setGeneratedBy(operator);
        entity.setGeneratedAt(OffsetDateTime.now());
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        versionMapper.insert(entity);
        return ProductDesignerDocumentVersionResponse.fromEntity(entity);
    }

    private byte[] buildProductWord(
            Long tenantId,
            SalesProductEntity product,
            List<SalesProductDayResourceEntity> resources,
            SalesProductDescriptionEntity description,
            List<SalesProductItineraryDayEntity> itineraryDays,
            List<SalesProductDayResourceImageEntity> selectedImages
    ) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configureDocument(document);
            title(document, product.getProductName());
            paragraph(document, area(product) + " · " + (product.getTravelDays() == null ? 1 : product.getTravelDays()) + "天产品介绍");
            addDescription(document, description);
            Map<Integer, List<SalesProductDayResourceEntity>> byDay = resources.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            SalesProductDayResourceEntity::getDayNo,
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()
                    ));
            Map<Integer, SalesProductItineraryDayEntity> itineraryByDay = itineraryDays.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            SalesProductItineraryDayEntity::getDayNo,
                            item -> item,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
            Map<Long, List<SalesProductDayResourceImageEntity>> imagesByResource = selectedImages.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            SalesProductDayResourceImageEntity::getDayResourceId,
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()
                    ));
            int days = product.getTravelDays() == null ? 1 : product.getTravelDays();
            for (int day = 1; day <= days; day++) {
                heading(document, "D" + day + " 行程");
                addItineraryDay(document, itineraryByDay.get(day));
                List<SalesProductDayResourceEntity> dayResources = byDay.getOrDefault(day, List.of());
                if (dayResources.isEmpty()) {
                    paragraph(document, "本日行程待完善。");
                    continue;
                }
                for (SalesProductDayResourceEntity resource : dayResources) {
                    if (!Boolean.TRUE.equals(resource.getIncludeInWord())) continue;
                    heading(document, resource.getResourceNameSnapshot());
                    if (StringUtils.hasText(resource.getIntroductionTitleSnapshot())) {
                        paragraph(document, resource.getIntroductionTitleSnapshot());
                    }
                    if (StringUtils.hasText(resource.getIntroductionContentSnapshot())) {
                        paragraph(document, resource.getIntroductionContentSnapshot());
                    }
                    noticeParagraphs(document, resource.getIntroductionNoticeSnapshot());
                    paragraph(document, "建议停留：" + (resource.getStayMinutes() == null ? 0 : resource.getStayMinutes()) + " 分钟");
                    for (SalesProductDayResourceImageEntity image : imagesByResource.getOrDefault(resource.getId(), List.of())) {
                        appendImage(document, tenantId, image);
                    }
                }
            }
            document.write(output);
            return withEmbeddedDocumentFont(output.toByteArray());
        } catch (IOException | InvalidFormatException ex) {
            throw new BizException("产品介绍 Word 生成失败");
        }
    }

    private void addDescription(XWPFDocument document, SalesProductDescriptionEntity description) {
        if (description == null) return;
        section(document, "产品说明", description.getProductDescription());
        section(document, "费用包含", description.getFeeIncluded());
        section(document, "费用不含", description.getFeeExcluded());
        section(document, "儿童安排", description.getChildPolicy());
        section(document, "购物安排", description.getShoppingArrangement());
        section(document, "自费项目", description.getOptionalItems());
        section(document, "赠送项目", description.getGiftItems());
        section(document, "注意事项", description.getAttentionItems());
        section(document, "温馨提醒", description.getWarmReminder());
        section(document, "收客须知", description.getBookingNotice());
    }

    private void addItineraryDay(XWPFDocument document, SalesProductItineraryDayEntity itinerary) {
        if (itinerary == null) return;
        section(document, "当日安排", itinerary.getDayTitle());
        section(document, "行程内容", itinerary.getItineraryContent());
        section(document, "住宿", firstText(itinerary.getRelatedHotel(), itinerary.getAccommodationNote()));
        String meals = mealText(itinerary);
        section(document, "用餐", meals);
        section(document, "路书", firstText(itinerary.getRoadbookPlace(), itinerary.getRoadbookSummary()));
    }

    private void section(XWPFDocument document, String title, String content) {
        if (!StringUtils.hasText(content)) return;
        heading(document, title);
        paragraph(document, content);
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String mealText(SalesProductItineraryDayEntity itinerary) {
        List<String> meals = new java.util.ArrayList<>();
        if (Boolean.TRUE.equals(itinerary.getBreakfastIncluded())) meals.add("早餐");
        if (Boolean.TRUE.equals(itinerary.getLunchIncluded())) meals.add("中餐");
        if (Boolean.TRUE.equals(itinerary.getDinnerIncluded())) meals.add("晚餐");
        return String.join("、", meals);
    }

    private void appendImage(
            XWPFDocument document,
            Long tenantId,
            SalesProductDayResourceImageEntity image
    ) throws IOException, InvalidFormatException {
        CommonAttachmentEntity attachment = attachmentService.getEntity(image.getAttachmentId(), tenantId);
        try (InputStream input = attachmentService.openStream(attachment.getId(), tenantId)) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.addPicture(
                    input,
                    pictureType(attachment.getContentType(), image.getOriginalFilenameSnapshot()),
                    image.getOriginalFilenameSnapshot(),
                    Units.toEMU(5.0),
                    Units.toEMU(3.0)
            );
        }
    }

    private int pictureType(String contentType, String fileName) {
        String value = (StringUtils.hasText(contentType) ? contentType : fileName).toLowerCase();
        if (value.endsWith("png") || value.contains("png")) return XWPFDocument.PICTURE_TYPE_PNG;
        if (value.endsWith("gif") || value.contains("gif")) return XWPFDocument.PICTURE_TYPE_GIF;
        if (value.endsWith("bmp") || value.contains("bmp")) return XWPFDocument.PICTURE_TYPE_BMP;
        return XWPFDocument.PICTURE_TYPE_JPEG;
    }

    private byte[] buildAdultQuoteWord(SalesProductEntity product, SalesProductAdultQuoteEntity quote) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configureDocument(document);
            title(document, product.getProductName() + " 成人报价单");
            XWPFTable table = document.createTable(4, 2);
            configureQuoteTable(table);
            row(table, 0, "产品名称", product.getProductName());
            row(table, 1, "计划成人数", String.valueOf(quote.getPlannedAdultCount()));
            row(table, 2, "成人对外价", "¥" + quote.getAdultSaleAmount().setScale(2));
            row(table, 3, "报价有效期", quote.getValidUntil() == null ? "未设置" : quote.getValidUntil().toString());
            if (StringUtils.hasText(quote.getQuoteRemark())) paragraph(document, "报价备注：" + quote.getQuoteRemark());
            document.write(output);
            return withEmbeddedDocumentFont(output.toByteArray());
        } catch (IOException ex) {
            throw new BizException("成人报价单生成失败");
        }
    }

    private byte[] withEmbeddedDocumentFont(byte[] docxBytes) throws IOException {
        try (InputStream fontInput = getClass().getResourceAsStream(DOCUMENT_FONT_RESOURCE)) {
            if (fontInput == null) {
                return docxBytes;
            }
            byte[] fontBytes = fontInput.readAllBytes();
            byte[] embeddedFontBytes = obfuscateFont(fontBytes, UUID.fromString(DOCUMENT_FONT_KEY));
            return addFontParts(docxBytes, embeddedFontBytes);
        }
    }

    private byte[] addFontParts(byte[] docxBytes, byte[] embeddedFontBytes) throws IOException {
        Set<String> copiedEntries = new HashSet<>();
        try (
                ByteArrayInputStream input = new ByteArrayInputStream(docxBytes);
                ZipInputStream zipInput = new ZipInputStream(input);
                ByteArrayOutputStream output = new ByteArrayOutputStream(docxBytes.length + embeddedFontBytes.length);
                ZipOutputStream zipOutput = new ZipOutputStream(output)
        ) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("word/fontTable.xml")
                        || name.equals("word/_rels/fontTable.xml.rels")
                        || name.equals("word/fonts/NotoSansSC.odttf")) {
                    continue;
                }
                byte[] entryBytes = zipInput.readAllBytes();
                if (name.equals("[Content_Types].xml")) {
                    entryBytes = addFontContentTypes(entryBytes);
                } else if (name.equals("word/_rels/document.xml.rels")) {
                    entryBytes = addFontTableRelationship(entryBytes);
                }
                zipOutput.putNextEntry(new ZipEntry(name));
                zipOutput.write(entryBytes);
                zipOutput.closeEntry();
                copiedEntries.add(name);
            }
            if (!copiedEntries.contains("word/_rels/document.xml.rels")) {
                writeEntry(zipOutput, "word/_rels/document.xml.rels", fontTableDocumentRelationship().getBytes(StandardCharsets.UTF_8));
            }
            writeEntry(zipOutput, "word/fontTable.xml", fontTableXml().getBytes(StandardCharsets.UTF_8));
            writeEntry(zipOutput, "word/_rels/fontTable.xml.rels", fontTableRelationshipXml().getBytes(StandardCharsets.UTF_8));
            writeEntry(zipOutput, "word/fonts/NotoSansSC.odttf", embeddedFontBytes);
            zipOutput.finish();
            return output.toByteArray();
        }
    }

    private byte[] addFontContentTypes(byte[] xmlBytes) {
        String xml = new String(xmlBytes, StandardCharsets.UTF_8);
        if (!xml.contains("Extension=\"odttf\"")) {
            xml = xml.replace("</Types>",
                    "<Default Extension=\"odttf\" ContentType=\"application/vnd.openxmlformats-officedocument.obfuscatedFont\"/></Types>");
        }
        if (!xml.contains("PartName=\"/word/fontTable.xml\"")) {
            xml = xml.replace("</Types>",
                    "<Override PartName=\"/word/fontTable.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml\"/></Types>");
        }
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] addFontTableRelationship(byte[] xmlBytes) {
        String xml = new String(xmlBytes, StandardCharsets.UTF_8);
        if (xml.contains("relationships/fontTable")) {
            return xmlBytes;
        }
        String relationship = "<Relationship Id=\"rIdMtravelFontTable\" "
                + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable\" "
                + "Target=\"fontTable.xml\"/>";
        return xml.replace("</Relationships>", relationship + "</Relationships>").getBytes(StandardCharsets.UTF_8);
    }

    private void writeEntry(ZipOutputStream output, String name, byte[] bytes) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(bytes);
        output.closeEntry();
    }

    private byte[] obfuscateFont(byte[] fontBytes, UUID fontKey) {
        byte[] result = fontBytes.clone();
        byte[] keyBytes = fontKeyBytes(fontKey);
        for (int index = 0; index < Math.min(32, result.length); index++) {
            result[index] = (byte) (result[index] ^ keyBytes[15 - (index % 16)]);
        }
        return result;
    }

    private byte[] fontKeyBytes(UUID fontKey) {
        byte[] bytes = new byte[16];
        long most = fontKey.getMostSignificantBits();
        long least = fontKey.getLeastSignificantBits();
        for (int index = 0; index < 8; index++) {
            bytes[index] = (byte) (most >>> (8 * (7 - index)));
            bytes[index + 8] = (byte) (least >>> (8 * (7 - index)));
        }
        return bytes;
    }

    private String fontTableXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:fonts xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:font w:name="Noto Sans SC">
                    <w:charset w:val="86"/>
                    <w:family w:val="swiss"/>
                    <w:pitch w:val="variable"/>
                    <w:embedRegular r:id="rId1" w:fontKey="{%s}" w:subsetted="0"/>
                  </w:font>
                </w:fonts>
                """.formatted(DOCUMENT_FONT_KEY.toUpperCase());
    }

    private String fontTableRelationshipXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/font" Target="fonts/NotoSansSC.odttf"/>
                </Relationships>
                """;
    }

    private String fontTableDocumentRelationship() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rIdMtravelFontTable" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable" Target="fontTable.xml"/>
                </Relationships>
                """;
    }

    private void configureDocument(XWPFDocument document) {
        var body = document.getDocument().getBody();
        var section = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        var pageSize = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(12240));
        pageSize.setH(BigInteger.valueOf(15840));
        var margins = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
        margins.setTop(BigInteger.valueOf(1440));
        margins.setRight(BigInteger.valueOf(1440));
        margins.setBottom(BigInteger.valueOf(1440));
        margins.setLeft(BigInteger.valueOf(1440));
    }

    private void configureQuoteTable(XWPFTable table) {
        table.setWidth(PAGE_USABLE_WIDTH_DXA);
        table.setWidthType(TableWidthType.DXA);
        table.setTableAlignment(TableRowAlign.CENTER);
        table.setCellMargins(120, 180, 120, 180);
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 6, 0, "D9E2EF");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 6, 0, "D9E2EF");
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 8, 0, "9BB5D3");
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 8, 0, "9BB5D3");
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 8, 0, "9BB5D3");
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 8, 0, "9BB5D3");

        CTTblGrid grid = table.getCTTbl().getTblGrid() == null
                ? table.getCTTbl().addNewTblGrid()
                : table.getCTTbl().getTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        addGridColumn(grid, 2600);
        addGridColumn(grid, PAGE_USABLE_WIDTH_DXA - 2600);

        CTTblPr tableProperties = table.getCTTbl().getTblPr() == null
                ? table.getCTTbl().addNewTblPr()
                : table.getCTTbl().getTblPr();
        CTTblWidth width = tableProperties.isSetTblW() ? tableProperties.getTblW() : tableProperties.addNewTblW();
        width.setType(STTblWidth.DXA);
        width.setW(BigInteger.valueOf(PAGE_USABLE_WIDTH_DXA));
    }

    private void addGridColumn(CTTblGrid grid, int width) {
        CTTblGridCol column = grid.addNewGridCol();
        column.setW(BigInteger.valueOf(width));
    }

    private void row(XWPFTable table, int index, String key, String value) {
        XWPFTableRow row = table.getRow(index);
        writeCell(row.getCell(0), key, true);
        writeCell(row.getCell(1), value == null ? "" : value, false);
        row.getCell(0).setWidth("2600");
        row.getCell(1).setWidth(String.valueOf(PAGE_USABLE_WIDTH_DXA - 2600));
    }

    private void title(XWPFDocument document, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(240);
        textRun(paragraph, value, 20, true);
    }

    private void heading(XWPFDocument document, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(160);
        paragraph.setSpacingAfter(80);
        XWPFRun run = textRun(paragraph, value, 14, true);
        run.setColor("1F4E78");
    }

    private void paragraph(XWPFDocument document, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(80);
        textRun(paragraph, value == null ? "" : value, 10, false);
    }

    private void noticeParagraphs(XWPFDocument document, String value) {
        if (!StringUtils.hasText(value)) return;
        for (String line : value.split("\\R")) {
            String text = line == null ? "" : line.trim();
            if (!StringUtils.hasText(text)) continue;
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setSpacingAfter(80);
            XWPFRun run = textRun(paragraph, text, 10, false);
            run.setColor("C00000");
        }
    }

    private void writeCell(XWPFTableCell cell, String value, boolean keyCell) {
        XWPFParagraph paragraph = cell.getParagraphArray(0);
        for (int index = paragraph.getRuns().size() - 1; index >= 0; index--) {
            paragraph.removeRun(index);
        }
        paragraph.setSpacingAfter(0);
        XWPFRun run = textRun(paragraph, value, 10, keyCell);
        if (keyCell) {
            run.setColor("1F4E78");
            cell.setColor("F3F6FA");
        }
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    }

    private XWPFRun textRun(XWPFParagraph paragraph, String value, int fontSize, boolean bold) {
        XWPFRun run = paragraph.createRun();
        run.setFontFamily(DOCUMENT_FONT);
        run.setFontFamily(DOCUMENT_FONT, XWPFRun.FontCharRange.eastAsia);
        run.setFontSize(fontSize);
        run.setBold(bold);
        markRunAsEastAsian(run);
        run.setText(value);
        return run;
    }

    private void markRunAsEastAsian(XWPFRun run) {
        CTRPr properties = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTFonts fonts = properties.sizeOfRFontsArray() == 0 ? properties.addNewRFonts() : properties.getRFontsArray(0);
        fonts.setHint(STHint.EAST_ASIA);
        fonts.setAscii(DOCUMENT_FONT);
        fonts.setHAnsi(DOCUMENT_FONT);
        fonts.setCs(DOCUMENT_FONT);
        fonts.setEastAsia(DOCUMENT_FONT);
        CTLanguage language = properties.sizeOfLangArray() == 0 ? properties.addNewLang() : properties.getLangArray(0);
        language.setVal("zh-CN");
        language.setEastAsia("zh-CN");

        CTPPr paragraphProperties = run.getParagraph().getCTP().isSetPPr()
                ? run.getParagraph().getCTP().getPPr()
                : run.getParagraph().getCTP().addNewPPr();
        var runProperties = paragraphProperties.isSetRPr()
                ? paragraphProperties.getRPr()
                : paragraphProperties.addNewRPr();
        CTFonts paragraphFonts = runProperties.sizeOfRFontsArray() == 0
                ? runProperties.addNewRFonts()
                : runProperties.getRFontsArray(0);
        paragraphFonts.setHint(STHint.EAST_ASIA);
        paragraphFonts.setAscii(DOCUMENT_FONT);
        paragraphFonts.setHAnsi(DOCUMENT_FONT);
        paragraphFonts.setCs(DOCUMENT_FONT);
        paragraphFonts.setEastAsia(DOCUMENT_FONT);
    }

    private String snapshot(
            SalesProductEntity product,
            List<SalesProductDayResourceEntity> resources,
            SalesProductAdultQuoteEntity quote,
            SalesProductDescriptionEntity description,
            List<SalesProductItineraryDayEntity> itineraryDays,
            List<SalesProductDayResourceImageEntity> selectedImages
    ) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("productId", product.getId());
        source.put("productName", product.getProductName());
        source.put("travelDays", product.getTravelDays());
        source.put("resources", resources.stream().map(item -> {
            Map<String, Object> resource = new LinkedHashMap<>();
            resource.put("dayNo", item.getDayNo());
            resource.put("resourceName", item.getResourceNameSnapshot() == null ? "" : item.getResourceNameSnapshot());
            resource.put("introductionTitle", item.getIntroductionTitleSnapshot() == null ? "" : item.getIntroductionTitleSnapshot());
            resource.put("introductionContent", item.getIntroductionContentSnapshot() == null ? "" : item.getIntroductionContentSnapshot());
            resource.put("introductionNotice", item.getIntroductionNoticeSnapshot() == null ? "" : item.getIntroductionNoticeSnapshot());
            return resource;
        }).toList());
        if (description != null) {
            Map<String, String> descriptionSnapshot = new LinkedHashMap<>();
            descriptionSnapshot.put("bookingNotice", safe(description.getBookingNotice()));
            descriptionSnapshot.put("productDescription", safe(description.getProductDescription()));
            descriptionSnapshot.put("feeIncluded", safe(description.getFeeIncluded()));
            descriptionSnapshot.put("feeExcluded", safe(description.getFeeExcluded()));
            descriptionSnapshot.put("childPolicy", safe(description.getChildPolicy()));
            descriptionSnapshot.put("shoppingArrangement", safe(description.getShoppingArrangement()));
            descriptionSnapshot.put("optionalItems", safe(description.getOptionalItems()));
            descriptionSnapshot.put("giftItems", safe(description.getGiftItems()));
            descriptionSnapshot.put("attentionItems", safe(description.getAttentionItems()));
            descriptionSnapshot.put("warmReminder", safe(description.getWarmReminder()));
            source.put("description", descriptionSnapshot);
        }
        source.put("itineraryDays", itineraryDays.stream().map(item -> {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("dayNo", item.getDayNo());
            day.put("title", safe(item.getDayTitle()));
            day.put("content", safe(item.getItineraryContent()));
            day.put("hotel", safe(firstText(item.getRelatedHotel(), item.getAccommodationNote())));
            day.put("meals", mealText(item));
            day.put("roadbook", safe(firstText(item.getRoadbookPlace(), item.getRoadbookSummary())));
            return day;
        }).toList());
        source.put("images", selectedImages.stream().map(item -> Map.of(
                "dayResourceId", item.getDayResourceId(),
                "resourceImageId", item.getResourceImageId(),
                "fileName", safe(item.getOriginalFilenameSnapshot())
        )).toList());
        if (quote != null) {
            source.put("adultCount", quote.getPlannedAdultCount());
            source.put("adultSaleAmount", quote.getAdultSaleAmount());
            source.put("validUntil", quote.getValidUntil() == null ? null : quote.getValidUntil().toString());
            source.put("quoteRemark", quote.getQuoteRemark() == null ? "" : quote.getQuoteRemark());
        }
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException ex) {
            throw new BizException("文档来源快照生成失败");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<SalesProductDayResourceEntity> resources(Long tenantId, Long productId) {
        return dayResourceMapper.selectList(new QueryWrapper<SalesProductDayResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId)
                .orderByAsc("day_no")
                .orderByAsc("sort_order")
                .orderByAsc("id"));
    }

    private SalesProductDescriptionEntity description(Long tenantId, Long productId) {
        return descriptionMapper.selectOne(new QueryWrapper<SalesProductDescriptionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId));
    }

    private List<SalesProductItineraryDayEntity> itineraryDays(Long tenantId, Long productId) {
        return itineraryDayMapper.selectList(new QueryWrapper<SalesProductItineraryDayEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId)
                .orderByAsc("day_no")
                .orderByAsc("id"));
    }

    private List<SalesProductDayResourceImageEntity> selectedImages(Long tenantId, Long productId) {
        return dayResourceImageMapper.selectList(new QueryWrapper<SalesProductDayResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_id", productId)
                .orderByAsc("day_resource_id")
                .orderByAsc("sort_order")
                .orderByAsc("id"));
    }

    private int nextVersion(Long tenantId, Long productId, String documentType) {
        SalesProductDocumentVersionEntity latest = versionMapper.selectList(new QueryWrapper<SalesProductDocumentVersionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("product_id", productId)
                        .eq("document_type", documentType)
                        .orderByDesc("version_no")
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    private SalesProductEntity loadProduct(Long tenantId, Long productId) {
        SalesProductEntity product = productMapper.selectOne(new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("product_scope", "design_draft")
                .eq("id", productId));
        if (product == null) throw new BizException("销售产品不存在或已删除");
        return product;
    }

    private String area(SalesProductEntity product) {
        return java.util.stream.Stream.of(product.getProvince(), product.getCity(), product.getDistrict())
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.joining(" / "));
    }

    private String safeFileName(String value) {
        String cleaned = StringUtils.hasText(value) ? value.trim() : "未命名产品";
        return cleaned.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
