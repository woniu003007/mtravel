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
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceIntroductionEntity;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDocumentVersionEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductAdultQuoteMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceImageMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceIntroductionMapper;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDayResourceOptionalItemMapper;
import com.mtravel.platform.sales.product.designer.entity.SalesProductDayResourceOptionalItemEntity;
import com.mtravel.platform.sales.product.designer.mapper.SalesProductDocumentVersionMapper;
import com.mtravel.platform.purchase.resource.material.dto.ResourceIntroductionExtensionBlock;
import com.mtravel.platform.purchase.resource.material.service.ResourceIntroductionExtensionBlockCodec;
import com.mtravel.platform.sales.product.entity.SalesProductDescriptionEntity;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.entity.SalesProductItineraryDayEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductDescriptionMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductItineraryDayMapper;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import java.io.ByteArrayInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.springframework.core.io.InputStreamResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHint;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
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
    private static final String PRODUCT_TEMPLATE_RESOURCE = "/templates/product/产品模版.docx";
    private static final int PAGE_USABLE_WIDTH_DXA = 9360;
    // Keep wrapped lines of numbered introduction items aligned after their marker.
    private static final int INTRODUCTION_LIST_INDENT_TWIPS = 420;
    // Product template uses a two-character first-line indent for itinerary prose.
    private static final int INTRODUCTION_FIRST_LINE_INDENT_TWIPS = 420;
    private static final String INTRODUCTION_LINE_PREFIX = "\u3000\u3000";
    private static final Pattern INTRODUCTION_LIST_LINE = Pattern.compile(
            "^\\s*(?:\\d{1,3}[、.．]|[（(]\\d{1,3}[）)]|[一二三四五六七八九十]+、)\\s*\\S.*$"
    );

    private final SalesProductMapper productMapper;
    private final SalesProductDescriptionMapper descriptionMapper;
    private final SalesProductItineraryDayMapper itineraryDayMapper;
    private final SalesProductDayResourceMapper dayResourceMapper;
    private final SalesProductDayResourceImageMapper dayResourceImageMapper;
    private final SalesProductDayResourceIntroductionMapper dayResourceIntroductionMapper;
    private final SalesProductAdultQuoteMapper adultQuoteMapper;
    private final SalesProductDocumentVersionMapper versionMapper;
    private final CommonAttachmentService attachmentService;
    private final ObjectMapper objectMapper;
    private final SalesProductDayResourceOptionalItemMapper optionalItemMapper;
    private final ResourceIntroductionExtensionBlockCodec extensionBlockCodec;

    @Value("${mtravel.document.office-command:soffice}")
    private String officeCommand = "soffice";

    @Value("${mtravel.document.office-timeout-seconds:30}")
    private long officeTimeoutSeconds = 30;

    @Autowired
    public SalesProductDesignerDocumentService(
            SalesProductMapper productMapper,
            SalesProductDescriptionMapper descriptionMapper,
            SalesProductItineraryDayMapper itineraryDayMapper,
            SalesProductDayResourceMapper dayResourceMapper,
            SalesProductDayResourceImageMapper dayResourceImageMapper,
            SalesProductDayResourceIntroductionMapper dayResourceIntroductionMapper,
            SalesProductAdultQuoteMapper adultQuoteMapper,
            SalesProductDocumentVersionMapper versionMapper,
            CommonAttachmentService attachmentService,
            ObjectMapper objectMapper,
            SalesProductDayResourceOptionalItemMapper optionalItemMapper,
            ResourceIntroductionExtensionBlockCodec extensionBlockCodec
    ) {
        this.productMapper = productMapper;
        this.descriptionMapper = descriptionMapper;
        this.itineraryDayMapper = itineraryDayMapper;
        this.dayResourceMapper = dayResourceMapper;
        this.dayResourceImageMapper = dayResourceImageMapper;
        this.dayResourceIntroductionMapper = dayResourceIntroductionMapper;
        this.adultQuoteMapper = adultQuoteMapper;
        this.versionMapper = versionMapper;
        this.attachmentService = attachmentService;
        this.objectMapper = objectMapper;
        this.optionalItemMapper = optionalItemMapper;
        this.extensionBlockCodec = extensionBlockCodec;
    }
    /** 兼容现有文档服务构造入口。 */
    public SalesProductDesignerDocumentService(SalesProductMapper productMapper, SalesProductDescriptionMapper descriptionMapper, SalesProductItineraryDayMapper itineraryDayMapper, SalesProductDayResourceMapper dayResourceMapper, SalesProductDayResourceImageMapper dayResourceImageMapper, SalesProductDayResourceIntroductionMapper dayResourceIntroductionMapper, SalesProductAdultQuoteMapper adultQuoteMapper, SalesProductDocumentVersionMapper versionMapper, CommonAttachmentService attachmentService, ObjectMapper objectMapper) { this(productMapper,descriptionMapper,itineraryDayMapper,dayResourceMapper,dayResourceImageMapper,dayResourceIntroductionMapper,adultQuoteMapper,versionMapper,attachmentService,objectMapper,null,new ResourceIntroductionExtensionBlockCodec(objectMapper)); }

    /** 生成产品介绍 Word，并保留本次资源介绍正文快照。 */
    @Transactional
    public ProductDesignerDocumentVersionResponse productWord(Long tenantId, Long productId, String operator) {
        SalesProductEntity product = loadProduct(tenantId, productId);
        List<SalesProductDayResourceEntity> resources = resources(tenantId, productId);
        SalesProductDescriptionEntity description = description(tenantId, productId);
        List<SalesProductItineraryDayEntity> itineraryDays = itineraryDays(tenantId, productId);
        List<SalesProductDayResourceImageEntity> selectedImages = selectedImages(tenantId, productId);
        List<SalesProductDayResourceIntroductionEntity> selectedIntroductions =
                selectedIntroductions(tenantId, productId);
        List<SalesProductDayResourceOptionalItemEntity> selectedOptionalItems = optionalItems(tenantId, productId);
        int versionNo = nextVersion(tenantId, productId, PRODUCT_WORD);
        String fileName = safeFileName(product.getProductName()) + "-产品介绍-v" + versionNo + ".docx";
        String snapshot = snapshot(
                product, resources, null, description, itineraryDays, selectedImages, selectedIntroductions
        );
        byte[] bytes = buildProductWord(
                tenantId, product, resources, description, itineraryDays, selectedImages, selectedIntroductions, selectedOptionalItems
        );
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
        String snapshot = snapshot(product, List.of(), quote, null, List.of(), List.of(), List.of());
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

    /** 将同一已生成的 DOCX 版本转换为 PDF，用于页面预览；不会重新生成 Word。 */
    public ResponseEntity<InputStreamResource> preview(Long tenantId, Long versionId) {
        SalesProductDocumentVersionEntity version = versionMapper.selectOne(new QueryWrapper<SalesProductDocumentVersionEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("generate_status", SUCCESS)
                .eq("id", versionId));
        if (version == null || version.getAttachmentId() == null) {
            throw new BizException("文档版本不存在或尚未生成");
        }
        CommonAttachmentEntity attachment = attachmentService.getEntity(version.getAttachmentId(), tenantId);
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("mtravel-word-preview-");
            Path docx = workDir.resolve("source.docx");
            try (InputStream input = attachmentService.openStream(attachment.getId(), tenantId)) {
                Files.copy(input, docx, StandardCopyOption.REPLACE_EXISTING);
            }
            Process process = new ProcessBuilder(
                    officeCommand, "--headless", "--convert-to", "pdf:writer_pdf_Export",
                    "--outdir", workDir.toString(), docx.toString()
            ).redirectErrorStream(true).start();
            boolean finished = process.waitFor(Math.max(1, officeTimeoutSeconds), TimeUnit.SECONDS);
            String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                throw new BizException("Word 预览转换超时，请稍后重试");
            }
            if (process.exitValue() != 0) {
                throw new BizException("Word 预览转换失败" + (StringUtils.hasText(processOutput) ? "：" + processOutput.trim() : ""));
            }
            Path pdf = workDir.resolve("source.pdf");
            if (!Files.exists(pdf)) throw new BizException("Word 预览转换失败，未生成 PDF");
            byte[] bytes = Files.readAllBytes(pdf);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                            .filename(version.getFileNameSnapshot().replaceAll("\\.docx$", ".pdf"), StandardCharsets.UTF_8)
                            .build().toString())
                    .body(new InputStreamResource(new ByteArrayInputStream(bytes)));
        } catch (IOException ex) {
            throw new BizException("Word 预览转换失败：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("Word 预览转换被中断");
        } finally {
            deletePreviewDirectory(workDir);
        }
    }

    private void deletePreviewDirectory(Path workDir) {
        if (workDir == null) return;
        try (var paths = Files.walk(workDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
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
            List<SalesProductDayResourceImageEntity> selectedImages,
            List<SalesProductDayResourceIntroductionEntity> selectedIntroductions,
            List<SalesProductDayResourceOptionalItemEntity> selectedOptionalItems
    ) {
        try (InputStream template = getClass().getResourceAsStream(PRODUCT_TEMPLATE_RESOURCE);
             XWPFDocument document = template == null ? new XWPFDocument() : new XWPFDocument(template);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (template == null) {
                configureDocument(document);
            }
            replaceTemplateTitle(document, product.getProductName());
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
            Map<Integer, String> imageModeByDay = itineraryDays.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            SalesProductItineraryDayEntity::getDayNo,
                            item -> normalizeImageMode(item.getWordImageMode()),
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
            Map<Long, List<SalesProductDayResourceImageEntity>> imagesByResource = selectedImages.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            SalesProductDayResourceImageEntity::getDayResourceId,
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()
                    ));
            Map<Long, SalesProductDayResourceEntity> resourceById = resources.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            SalesProductDayResourceEntity::getId, item -> item, (left, right) -> left
                    ));
            Map<Integer, List<SalesProductDayResourceImageEntity>> dayEndImagesByDay = selectedImages.stream()
                    .filter(image -> resourceById.containsKey(image.getDayResourceId()))
                    .sorted(java.util.Comparator.comparing((SalesProductDayResourceImageEntity image) ->
                                    image.getSortOrder() == null ? Integer.MAX_VALUE : image.getSortOrder())
                            .thenComparing(SalesProductDayResourceImageEntity::getId,
                                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .collect(java.util.stream.Collectors.groupingBy(
                            image -> resourceById.get(image.getDayResourceId()).getDayNo(),
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()
                    ));
            Map<Long, List<SalesProductDayResourceIntroductionEntity>> introductionsByResource =
                    selectedIntroductions.stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    SalesProductDayResourceIntroductionEntity::getDayResourceId,
                                    LinkedHashMap::new,
                                    java.util.stream.Collectors.toList()
                            ));
            Map<Long, List<SalesProductDayResourceOptionalItemEntity>> optionalByResource = selectedOptionalItems.stream().collect(java.util.stream.Collectors.groupingBy(SalesProductDayResourceOptionalItemEntity::getDayResourceId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
            int days = product.getTravelDays() == null ? 1 : product.getTravelDays();
            fillOverviewTable(document, product, resources, itineraryByDay, days);
            fillDetailTable(document, tenantId, product, description, resources, itineraryByDay,
                    imageModeByDay, imagesByResource, dayEndImagesByDay, introductionsByResource, optionalByResource, days);
            // 保留旧消费端按正文段落读取的兼容层；内容隐藏，不会在模板页面重复显示。
            appendLegacyParagraphMirror(document, description, itineraryDays, resources, introductionsByResource, optionalByResource);
            document.write(output);
            return withEmbeddedDocumentFont(output.toByteArray());
        } catch (IOException | InvalidFormatException ex) {
            throw new BizException("产品介绍 Word 生成失败");
        }
    }

    /** 用模板第一段作为产品标题，避免把母版的示例产品名带入新文档。 */
    private void replaceTemplateTitle(XWPFDocument document, String productName) {
        if (document.getParagraphs().isEmpty()) {
            title(document, productName);
            return;
        }
        XWPFParagraph paragraph = document.getParagraphs().getFirst();
        for (int index = paragraph.getRuns().size() - 1; index >= 0; index--) {
            paragraph.removeRun(index);
        }
        XWPFRun run = textRun(paragraph, productName == null ? "" : productName, 20, true);
        run.setColor("000000");
    }

    private void fillOverviewTable(
            XWPFDocument document,
            SalesProductEntity product,
            List<SalesProductDayResourceEntity> resources,
            Map<Integer, SalesProductItineraryDayEntity> itineraryByDay,
            int days
    ) {
        if (document.getTables().isEmpty()) return;
        XWPFTable table = document.getTables().getFirst();
        CTRow headerStyle = table.getRows().size() > 1 ? copyRow(table.getRow(1)) : null;
        CTRow dayStyle = table.getRows().size() > 2 ? copyRow(table.getRow(2)) : headerStyle;
        CTRow footerStyle = table.getRows().size() > 3 ? copyRow(table.getRow(table.getRows().size() - 1)) : null;
        while (table.getNumberOfRows() > 2) table.removeRow(table.getNumberOfRows() - 1);
        for (int day = 1; day <= days; day++) {
            final int dayNo = day;
            XWPFTableRow row = addClonedRow(table, dayStyle);
            SalesProductItineraryDayEntity itinerary = itineraryByDay.get(day);
            List<SalesProductDayResourceEntity> dayResources = resources.stream()
                    .filter(item -> dayNo == item.getDayNo() && Boolean.TRUE.equals(item.getIncludeInWord()))
                    .toList();
            String route = StringUtils.hasText(itinerary == null ? null : itinerary.getDayTitle())
                    ? itinerary.getDayTitle()
                    : dayResources.stream()
                    .filter(item -> isItineraryResource(item))
                    .map(SalesProductDayResourceEntity::getResourceNameSnapshot)
                    .filter(StringUtils::hasText).reduce((left, right) -> left + "-" + right).orElse("待完善");
            String hotel = accommodationText(dayResources, itinerary);
            rowText(row, 0, "D" + day);
            rowText(row, 1, route);
            rowText(row, 2, mealText(dayResources, "breakfast", itinerary == null ? null : itinerary.getBreakfastIncluded(),
                    previousNightAccommodation(resources, dayNo)));
            rowText(row, 3, mealText(dayResources, "lunch", itinerary == null ? null : itinerary.getLunchIncluded()));
            rowText(row, 4, mealText(dayResources, "dinner", itinerary == null ? null : itinerary.getDinnerIncluded()));
            rowText(row, 5, hotel);
        }
        if (footerStyle != null) {
            XWPFTableRow footer = addClonedRow(table, footerStyle);
            rowText(footer, 0, "导游在不减少任何景点情况下，有权调整行程前后游览顺序，景区、住宿标准不变。");
        }
    }

    private String mealMark(Boolean included) {
        return Boolean.TRUE.equals(included) ? "√" : "×";
    }

    private void fillDetailTable(
            XWPFDocument document,
            Long tenantId,
            SalesProductEntity product,
            SalesProductDescriptionEntity description,
            List<SalesProductDayResourceEntity> resources,
            Map<Integer, SalesProductItineraryDayEntity> itineraryByDay,
            Map<Integer, String> imageModeByDay,
            Map<Long, List<SalesProductDayResourceImageEntity>> imagesByResource,
            Map<Integer, List<SalesProductDayResourceImageEntity>> dayEndImagesByDay,
            Map<Long, List<SalesProductDayResourceIntroductionEntity>> introductionsByResource,
            Map<Long, List<SalesProductDayResourceOptionalItemEntity>> optionalByResource,
            int days
    ) throws IOException, InvalidFormatException {
        if (document.getTables().size() < 2) return;
        XWPFTable table = document.getTables().get(1);
        CTRow dayStyle = table.getRows().size() > 1 ? copyRow(table.getRow(1)) : null;
        CTRow mealStyle = table.getRows().size() > 2 ? copyRow(table.getRow(2)) : dayStyle;
        CTRow contentStyle = table.getRows().size() > 3 ? copyRow(table.getRow(3)) : mealStyle;
        CTRow pairStyle = table.getRows().size() > 19 ? copyRow(table.getRow(19)) : contentStyle;
        CTRow headingStyle = table.getRows().size() > 27 ? copyRow(table.getRow(27)) : contentStyle;
        CTRow bodyStyle = table.getRows().size() > 28 ? copyRow(table.getRow(28)) : contentStyle;
        while (table.getNumberOfRows() > 1) table.removeRow(table.getNumberOfRows() - 1);
        for (int day = 1; day <= days; day++) {
            final int dayNo = day;
            SalesProductItineraryDayEntity itinerary = itineraryByDay.get(day);
            XWPFTableRow dayRow = addClonedRow(table, dayStyle);
            rowText(dayRow, 0, "D" + day);
            rowText(dayRow, 1, itinerary == null || !StringUtils.hasText(itinerary.getDayTitle()) ? "" : itinerary.getDayTitle());
            XWPFTableRow mealRow = addClonedRow(table, mealStyle);
            List<SalesProductDayResourceEntity> dayResources = resources.stream()
                    .filter(item -> dayNo == item.getDayNo() && Boolean.TRUE.equals(item.getIncludeInWord()))
                    .toList();
            fillMealRow(mealRow, itinerary, dayResources, previousNightAccommodation(resources, dayNo));
            XWPFTableRow contentRow = addClonedRow(table, contentStyle);
            fillContentCell(contentRow.getCell(0), itineraryText(itinerary));
            List<SalesProductDayResourceEntity> itineraryResources = dayResources.stream()
                    .filter(this::isItineraryResource)
                    .toList();
            String imageMode = imageModeByDay.getOrDefault(dayNo, "follow_resource");
            for (SalesProductDayResourceEntity resource : itineraryResources) {
                appendResourceToCell(document, tenantId, contentRow.getCell(0), resource,
                        introductionsByResource.getOrDefault(resource.getId(), List.of()),
                        optionalByResource.getOrDefault(resource.getId(), List.of()),
                        "follow_resource".equals(imageMode)
                                ? imagesByResource.getOrDefault(resource.getId(), List.of())
                                : List.of());
            }
            if ("day_end".equals(imageMode)) {
                // day_end 是当天统一图片组，排序允许跨景区混排，不能按资源再次拆开。
                appendImageGridToCell(document, tenantId, contentRow.getCell(0),
                        dayEndImagesByDay.getOrDefault(dayNo, List.of()));
            }
        }
        addDetailSection(table, pairStyle, "【费用包含】", description == null ? null : description.getFeeIncluded());
        addDetailSection(table, pairStyle, "【费用不含】", description == null ? null : description.getFeeExcluded());
        addDetailSection(table, pairStyle, "【自费项目】", description == null ? null : description.getOptionalItems());
        addDetailSection(table, pairStyle, "【购物安排】", description == null ? null : description.getShoppingArrangement());
        addDetailSection(table, pairStyle, "【温馨提醒】", description == null ? null : description.getWarmReminder());
        addDetailSection(table, pairStyle, "【注意事项】", description == null ? null : description.getAttentionItems());
        addDetailSection(table, headingStyle, "不含项目", description == null ? null : description.getFeeExcluded());
        addDetailSection(table, bodyStyle, "特别申明", description == null ? null : description.getBookingNotice());
        addDetailSection(table, bodyStyle, "其他事项", description == null ? null : description.getAttentionItems());
    }

    private XWPFTableRow cloneRow(XWPFTable table, CTRow style) {
        return style == null ? table.createRow() : new XWPFTableRow((CTRow) style.copy(), table);
    }

    private XWPFTableRow addClonedRow(XWPFTable table, CTRow style) {
        XWPFTableRow row = cloneRow(table, style);
        table.addRow(row);
        int lastIndex = table.getCTTbl().sizeOfTrArray() - 1;
        return new XWPFTableRow(table.getCTTbl().getTrArray(lastIndex), table);
    }

    private CTRow copyRow(XWPFTableRow row) {
        return row == null ? null : (CTRow) row.getCtRow().copy();
    }

    private void rowText(XWPFTableRow row, int cellIndex, String value) {
        if (row == null || cellIndex >= row.getTableCells().size()) return;
        fillContentCell(row.getCell(cellIndex), value);
    }

    private void fillMealRow(
            XWPFTableRow row,
            SalesProductItineraryDayEntity itinerary,
            List<SalesProductDayResourceEntity> dayResources,
            SalesProductDayResourceEntity previousNightAccommodation
    ) {
        rowText(row, 1, "早餐：" + mealText(dayResources, "breakfast", itinerary == null ? null : itinerary.getBreakfastIncluded(), previousNightAccommodation));
        rowText(row, 2, "午餐：" + mealText(dayResources, "lunch", itinerary == null ? null : itinerary.getLunchIncluded()));
        rowText(row, 3, "晚餐：" + mealText(dayResources, "dinner", itinerary == null ? null : itinerary.getDinnerIncluded()));
        rowText(row, 4, "住宿：" + accommodationText(dayResources, itinerary));
    }

    private boolean isItineraryResource(SalesProductDayResourceEntity resource) {
        return !StringUtils.hasText(resource.getArrangementRole()) || "itinerary".equals(resource.getArrangementRole());
    }

    private String accommodationText(
            List<SalesProductDayResourceEntity> dayResources,
            SalesProductItineraryDayEntity itinerary
    ) {
        String hotels = dayResources.stream()
                .filter(item -> "accommodation".equals(item.getArrangementRole()))
                .map(SalesProductDayResourceEntity::getResourceNameSnapshot)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.joining("、"));
        return StringUtils.hasText(hotels)
                ? hotels
                : itinerary == null ? "" : firstText(itinerary.getRelatedHotel(), itinerary.getAccommodationNote());
    }

    private String mealText(
            List<SalesProductDayResourceEntity> dayResources,
            String arrangementRole,
            Boolean legacyIncluded
    ) {
        return mealText(dayResources, arrangementRole, legacyIncluded, null);
    }

    private String mealText(
            List<SalesProductDayResourceEntity> dayResources,
            String arrangementRole,
            Boolean legacyIncluded,
            SalesProductDayResourceEntity previousNightAccommodation
    ) {
        return dayResources.stream()
                .filter(item -> arrangementRole.equals(item.getArrangementRole()))
                .map(SalesProductDayResourceEntity::getResourceNameSnapshot)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("breakfast".equals(arrangementRole)
                        && previousNightAccommodation != null
                        && Boolean.TRUE.equals(previousNightAccommodation.getHotelBreakfastIncluded())
                        ? "酒店含早"
                        : Boolean.TRUE.equals(legacyIncluded) ? "已含" : "×");
    }

    private SalesProductDayResourceEntity previousNightAccommodation(
            List<SalesProductDayResourceEntity> resources,
            int dayNo
    ) {
        if (dayNo <= 1) return null;
        return resources.stream()
                .filter(item -> item.getDayNo() == dayNo - 1)
                .filter(item -> "accommodation".equals(item.getArrangementRole()))
                .filter(item -> Boolean.TRUE.equals(item.getHotelBreakfastIncluded()))
                .findFirst()
                .orElse(null);
    }

    private String itineraryText(SalesProductItineraryDayEntity itinerary) {
        if (itinerary == null) return "";
        return firstText(itinerary.getItineraryContent(), firstText(itinerary.getRoadbookPlace(), itinerary.getRoadbookSummary()));
    }

    private void addDetailSection(XWPFTable table, CTRow style, String title, String content) {
        if (!StringUtils.hasText(content)) return;
        XWPFTableRow row = addClonedRow(table, style);
        if (row.getTableCells().size() == 1) {
            fillContentCell(row.getCell(0), title + "\n" + content);
        } else {
            rowText(row, 0, title);
            rowText(row, 1, content);
        }
    }

    private void appendResourceToCell(
            XWPFDocument document,
            Long tenantId,
            XWPFTableCell cell,
            SalesProductDayResourceEntity resource,
            List<SalesProductDayResourceIntroductionEntity> introductions,
            List<SalesProductDayResourceOptionalItemEntity> optionalItems,
            List<SalesProductDayResourceImageEntity> images
    ) throws IOException, InvalidFormatException {
        XWPFParagraph resourceParagraph = null;
        List<SalesProductDayResourceIntroductionEntity> fragments = introductions;
        if (fragments.isEmpty() && StringUtils.hasText(resource.getIntroductionTitleSnapshot())) {
            resourceParagraph = cell.addParagraph();
            prepareInlineResourceParagraph(resourceParagraph);
            appendRichIntroduction(resourceParagraph, true, resource.getIntroductionTitleSnapshot(), resource.getIntroductionContentSnapshot(),
                    resource.getIntroductionNoticeSnapshot(), resource.getIntroductionWarmTipSnapshot(), resource.getIntroductionVisitDurationSnapshot(), null);
        } else {
            for (int index = 0; index < fragments.size(); index++) {
                SalesProductDayResourceIntroductionEntity fragment = fragments.get(index);
                if (resourceParagraph == null) {
                    resourceParagraph = cell.addParagraph();
                    prepareInlineResourceParagraph(resourceParagraph);
                }
                appendRichIntroduction(resourceParagraph, index == 0, fragment.getTitleSnapshot(), fragment.getContentSnapshot(),
                        fragment.getNoticeSnapshot(), fragment.getWarmTipSnapshot(), fragment.getVisitDurationSnapshot(),
                        fragment.getExtensionBlocksSnapshot());
            }
        }
        for (SalesProductDayResourceOptionalItemEntity item : optionalItems) {
            if (resourceParagraph == null) {
                resourceParagraph = cell.addParagraph();
                prepareInlineResourceParagraph(resourceParagraph);
            } else if (resourceParagraph.getRuns().stream().anyMatch(run -> StringUtils.hasText(run.text()))) {
                // 每个自费项目从新行开始，避免接在景点正文或上一个项目后面。
                appendLineBreak(resourceParagraph);
            }
            appendRun(resourceParagraph, ("scenic_transport".equals(item.getItemTypeSnapshot()) ? "景区小交通：【" : "推荐自费：【")
                    + item.getProjectNameSnapshot() + "-" + item.getFinalSalePrice().stripTrailingZeros().toPlainString() + "元/人】", "C00000", true);
            appendInlineText(resourceParagraph, item.getIntroductionContentSnapshot(), null, false);
        }
        appendImageGridToCell(document, tenantId, cell, images);
    }

    private void fillContentCell(XWPFTableCell cell, String content) {
        clearCell(cell);
        if (StringUtils.hasText(content)) addRichParagraphs(cell, content, null);
    }

    private void clearCell(XWPFTableCell cell) {
        CTTc ctCell = cell.getCTTc();
        while (ctCell.sizeOfPArray() > 0) ctCell.removeP(0);
        ctCell.addNewP();
    }

    /** 同一景区的多个介绍素材共用一个 Word 段落，只有切换到下一个景区才换段。 */
    private void appendRichIntroduction(XWPFParagraph paragraph, boolean first, String title, String content, String notice,
                                        String warmTip, String duration, String extensionBlocksSnapshot) {
        boolean hasPreviousContent = paragraph.getRuns().stream().anyMatch(run -> StringUtils.hasText(run.text()));
        if (StringUtils.hasText(title)) {
            if (hasPreviousContent) appendInlineSeparator(paragraph);
            if (first) {
                // “游览：”是行程结构提示，保持黑色；只有景点名称使用模板蓝色标题样式。
                appendRun(paragraph, "游览： ", null, false);
            }
            appendRun(paragraph, "【" + title.trim() + "】", "0070C0", true);
            if (StringUtils.hasText(duration)) {
                appendRun(paragraph, formatVisitDuration(duration), null, false);
            }
        } else if (StringUtils.hasText(duration)) {
            appendInlineSeparator(paragraph);
            appendRun(paragraph, formatVisitDuration(duration), null, false);
        }
        if (StringUtils.hasText(notice)) {
            appendInlineSeparator(paragraph);
            appendInlineText(paragraph, notice, "C00000", false);
        }
        if (StringUtils.hasText(content)) {
            appendInlineSeparator(paragraph);
            appendInlineText(paragraph, content, null, false);
        }
        if (StringUtils.hasText(warmTip)) {
            appendInlineSeparator(paragraph);
            appendInlineText(paragraph, warmTip, "0070C0", false);
        }
        appendExtensionBlocks(paragraph, extensionBlocksSnapshot);
    }

    /** 将素材快照中的扩展内容按用户配置的标题颜色和录入顺序写入同一景区段落。 */
    private void appendExtensionBlocks(XWPFParagraph paragraph, String snapshot) {
        if (extensionBlockCodec == null) return;
        List<ResourceIntroductionExtensionBlock> blocks = extensionBlockCodec.decode(snapshot);
        for (ResourceIntroductionExtensionBlock block : blocks) {
            if (block == null || !StringUtils.hasText(block.title())) continue;
            // 扩展模块是独立内容块，前面保留一个空行，不能与上一段正文或温馨提示粘在一起。
            if (paragraph.getRuns().stream().anyMatch(run -> StringUtils.hasText(run.text()))) {
                appendLineBreak(paragraph);
                appendLineBreak(paragraph);
            }
            appendRun(paragraph, INTRODUCTION_LINE_PREFIX + block.title().trim(), colorHex(block.titleColor()), true);
            List<String> lines = extensionLines(block);
            for (String line : lines) {
                XWPFRun breakRun = paragraph.createRun();
                breakRun.addBreak();
                appendRun(paragraph, INTRODUCTION_LINE_PREFIX + line, null, false);
            }
        }
    }

    private List<String> extensionLines(ResourceIntroductionExtensionBlock block) {
        if (ResourceIntroductionExtensionBlockCodec.MULTILINE.equals(block.contentMode())) {
            if (!StringUtils.hasText(block.content())) return List.of();
            return java.util.Arrays.stream(block.content().split("\\R", -1))
                    .map(String::strip)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return block.items() == null ? List.of() : block.items().stream()
                .filter(StringUtils::hasText)
                .map(String::strip)
                .toList();
    }

    private String colorHex(String color) {
        if (!StringUtils.hasText(color)) return "0070C0";
        String value = color.trim();
        return value.startsWith("#") ? value.substring(1) : value;
    }

    private void appendLineBreak(XWPFParagraph paragraph) {
        XWPFRun breakRun = paragraph.createRun();
        breakRun.addBreak();
    }

    private void prepareInlineResourceParagraph(XWPFParagraph paragraph) {
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);
        paragraph.setIndentationFirstLine(INTRODUCTION_FIRST_LINE_INDENT_TWIPS);
    }

    private void appendInlineSeparator(XWPFParagraph paragraph) {
        appendRun(paragraph, " ", null, false);
    }

    private void appendInlineText(XWPFParagraph paragraph, String content, String color, boolean bold) {
        if (content == null) return;
        String[] lines = content.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                XWPFRun breakRun = paragraph.createRun();
                breakRun.addBreak();
            }
            appendRun(paragraph, lines[index].strip(), color, bold);
        }
    }

    private void appendRun(XWPFParagraph paragraph, String value, String color, boolean bold) {
        XWPFRun run = textRun(paragraph, value == null ? "" : value, 10, bold);
        if (color != null) run.setColor(color);
    }

    private void addRichParagraphs(XWPFTableCell cell, String content, String color) {
        addRichParagraphs(cell, content, color, false);
    }

    private void addRichParagraphs(XWPFTableCell cell, String content, String color, boolean keepWithNext) {
        String[] lines = content.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            addRichParagraph(cell, lines[index], color, false, keepWithNext || index < lines.length - 1);
        }
    }

    private void addRichParagraph(XWPFTableCell cell, String value, String color, boolean bold) {
        addRichParagraph(cell, value, color, bold, false);
    }

    private void addRichParagraph(XWPFTableCell cell, String value, String color, boolean bold, boolean keepWithNext) {
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);
        paragraph.setKeepNext(keepWithNext);
        String text = value == null ? "" : value.strip();
        if (INTRODUCTION_LIST_LINE.matcher(text).matches()) {
            paragraph.setIndentationLeft(INTRODUCTION_LIST_INDENT_TWIPS);
            paragraph.setIndentationHanging(INTRODUCTION_LIST_INDENT_TWIPS);
        } else {
            paragraph.setIndentationFirstLine(INTRODUCTION_FIRST_LINE_INDENT_TWIPS);
        }
        XWPFRun run = textRun(paragraph, text, 10, bold);
        if (color != null) run.setColor(color);
    }

    /** 将素材保存的分钟数字转换为 Word 中的中文游览时长。 */
    private String formatVisitDuration(String value) {
        String raw = value == null ? "" : value.trim();
        java.util.regex.Matcher direct = Pattern.compile("^(\\d+)$").matcher(raw);
        int minutes;
        if (direct.matches()) {
            minutes = Integer.parseInt(direct.group(1));
        } else {
            java.util.regex.Matcher hours = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*小时").matcher(raw);
            java.util.regex.Matcher mins = Pattern.compile("(\\d+)\\s*分钟").matcher(raw);
            if (!hours.find() && !mins.find()) return "（游览约" + raw + "）";
            double hourValue = hours.reset().find() ? Double.parseDouble(hours.group(1)) : 0;
            int minuteValue = mins.reset().find() ? Integer.parseInt(mins.group(1)) : 0;
            minutes = (int) Math.round(hourValue * 60 + minuteValue);
        }
        if (minutes <= 0) return "（游览约" + raw + "）";
        int hours = minutes / 60;
        int remainder = minutes % 60;
        String duration = minutes <= 60 ? minutes + "分钟" : hours + "小时" + (remainder == 0 ? "" : remainder + "分钟");
        return "（游览约" + duration + "）";
    }

    /**
     * 以景区为单位输出图片：两张一排两列，三张一排三列。
     * 历史数据若超过三张则拆成多个 2/3 图组；单张不输出，避免破坏 Word 版式。
     */
    private void appendImageGridToCell(XWPFDocument document, Long tenantId, XWPFTableCell cell,
                                       List<SalesProductDayResourceImageEntity> images) throws IOException, InvalidFormatException {
        if (images == null || images.size() < 2) return;
        XWPFParagraph spacer = cell.addParagraph();
        spacer.setSpacingAfter(60);
        for (List<SalesProductDayResourceImageEntity> gallery : imageGalleries(images)) {
            appendImageGallery(document, tenantId, cell, gallery);
        }
    }

    private List<List<SalesProductDayResourceImageEntity>> imageGalleries(List<SalesProductDayResourceImageEntity> images) {
        List<List<SalesProductDayResourceImageEntity>> galleries = new ArrayList<>();
        for (int cursor = 0; cursor < images.size();) {
            int remaining = images.size() - cursor;
            if (remaining == 1) break;
            int size = remaining == 4 ? 2 : Math.min(3, remaining);
            galleries.add(images.subList(cursor, cursor + size));
            cursor += size;
        }
        return galleries;
    }

    private void appendImageGallery(XWPFDocument document, Long tenantId, XWPFTableCell cell,
                                    List<SalesProductDayResourceImageEntity> images) throws IOException, InvalidFormatException {
        if (images.size() == 3) {
            appendImageRow(document, tenantId, cell, images, 2.5, 1.875);
            return;
        }
        appendImageRow(document, tenantId, cell, images, 3.75, 2.8125);
    }

    private void appendImageRow(XWPFDocument document, Long tenantId, XWPFTableCell cell,
                                List<SalesProductDayResourceImageEntity> images,
                                double maxWidthInches, double maxHeightInches) throws IOException, InvalidFormatException {
        int columns = images.size();
        CTTbl tableXml = cell.getCTTc().addNewTbl();
        XWPFTable table = new XWPFTable(tableXml, cell, 1, columns);
        table.setWidth("100%");
        table.setTableAlignment(TableRowAlign.CENTER);
        table.removeBorders();
        for (int index = 0; index < columns; index++) {
            SalesProductDayResourceImageEntity image = images.get(index);
            XWPFTableCell imageCell = table.getRow(0).getCell(index);
            imageCell.setWidth((100 / columns) + "%");
            imageCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            setImageCellBorder(imageCell);
            appendImageToCell(document, tenantId, imageCell, image, maxWidthInches, maxHeightInches);
        }
    }

    /** 为图片组提供克制的浅灰描边，保留 Word 模板中的干净留白感。 */
    private void setImageCellBorder(XWPFTableCell cell) {
        CTTc cellXml = cell.getCTTc();
        var properties = cellXml.isSetTcPr() ? cellXml.getTcPr() : cellXml.addNewTcPr();
        CTTcBorders borders = properties.isSetTcBorders() ? properties.getTcBorders() : properties.addNewTcBorders();
        setImageBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop());
        setImageBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight());
        setImageBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom());
        setImageBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft());
    }

    private void setImageBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(6));
        border.setColor("D9D9D9");
        border.setSpace(BigInteger.ZERO);
    }

    private void appendImageToCell(XWPFDocument document, Long tenantId, XWPFTableCell cell,
                                   SalesProductDayResourceImageEntity image, double widthInches,
                                   double heightInches) throws IOException, InvalidFormatException {
        CommonAttachmentEntity attachment = attachmentService.getEntity(image.getAttachmentId(), tenantId);
        try (InputStream input = attachmentService.openStream(attachment.getId(), tenantId)) {
            byte[] imageBytes = input.readAllBytes();
            PreparedImage preparedImage = prepareCoverImage(
                    imageBytes, widthInches, heightInches,
                    pictureType(attachment.getContentType(), image.getOriginalFilenameSnapshot()),
                    image.getOriginalFilenameSnapshot()
            );
            XWPFParagraph paragraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().getFirst();
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = paragraph.createRun();
            try (ByteArrayInputStream pictureInput = new ByteArrayInputStream(preparedImage.bytes())) {
                run.addPicture(pictureInput, preparedImage.pictureType(), preparedImage.filename(),
                        inchesToEmu(widthInches), inchesToEmu(heightInches));
            }
        }
    }

    /** Apache POI 的 toEMU 参数单位是磅，Word 版式尺寸在本服务中使用英寸。 */
    private int inchesToEmu(double inches) {
        return Units.toEMU(inches * 72d);
    }

    /**
     * 将原图居中裁切成固定图片框比例：图片始终铺满框体，且不拉伸变形。
     * 先统一转为 PNG，可避免 Word/PDF 转换时因原图格式和声明类型不一致而出现空白图。
     */
    private PreparedImage prepareCoverImage(
            byte[] imageBytes,
            double frameWidthInches,
            double frameHeightInches,
            int originalPictureType,
            String originalFilename
    ) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return new PreparedImage(imageBytes, originalPictureType, originalFilename);
            }
            int targetHeight = 900;
            int targetWidth = Math.max(1, (int) Math.round(targetHeight * frameWidthInches / frameHeightInches));
            double scale = Math.max((double) targetWidth / image.getWidth(), (double) targetHeight / image.getHeight());
            int renderedWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int renderedHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
            int offsetX = (targetWidth - renderedWidth) / 2;
            int offsetY = (targetHeight - renderedHeight) / 2;
            BufferedImage cropped = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = cropped.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, targetWidth, targetHeight);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(image, offsetX, offsetY, renderedWidth, renderedHeight, null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(cropped, "png", output)) {
                return new PreparedImage(imageBytes, originalPictureType, originalFilename);
            }
            return new PreparedImage(output.toByteArray(), XWPFDocument.PICTURE_TYPE_PNG, coverFilename(originalFilename));
        } catch (IOException ignored) {
            return new PreparedImage(imageBytes, originalPictureType, originalFilename);
        }
    }

    private String coverFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) return "scenic-cover.png";
        int extension = originalFilename.lastIndexOf('.');
        String baseName = extension > 0 ? originalFilename.substring(0, extension) : originalFilename;
        return baseName + "-cover.png";
    }

    private record PreparedImage(byte[] bytes, int pictureType, String filename) {}

    private String normalizeImageMode(String value) {
        return value != null && Set.of("follow_resource", "day_end", "hidden").contains(value)
                ? value
                : "follow_resource";
    }

    private void appendLegacyParagraphMirror(
            XWPFDocument document,
            SalesProductDescriptionEntity description,
            List<SalesProductItineraryDayEntity> itineraryDays,
            List<SalesProductDayResourceEntity> resources,
            Map<Long, List<SalesProductDayResourceIntroductionEntity>> introductionsByResource,
            Map<Long, List<SalesProductDayResourceOptionalItemEntity>> optionalByResource
    ) {
        for (SalesProductItineraryDayEntity itinerary : itineraryDays) {
            if (StringUtils.hasText(itinerary.getDayTitle())) addHiddenParagraph(document, itinerary.getDayTitle(), null, false);
            if (StringUtils.hasText(itinerary.getItineraryContent())) addHiddenParagraph(document, itinerary.getItineraryContent(), null, false);
        }
        for (SalesProductDayResourceEntity resource : resources) {
            if (!Boolean.TRUE.equals(resource.getIncludeInWord())) continue;
            List<SalesProductDayResourceIntroductionEntity> fragments = introductionsByResource.getOrDefault(resource.getId(), List.of());
            if (fragments.isEmpty()) {
                appendLegacyIntroduction(document, resource.getIntroductionTitleSnapshot(), resource.getIntroductionContentSnapshot(),
                        resource.getIntroductionNoticeSnapshot(), resource.getIntroductionWarmTipSnapshot(), resource.getIntroductionVisitDurationSnapshot());
            } else {
                for (SalesProductDayResourceIntroductionEntity fragment : fragments) {
                    appendLegacyIntroduction(document, fragment.getTitleSnapshot(), fragment.getContentSnapshot(), fragment.getNoticeSnapshot(),
                            fragment.getWarmTipSnapshot(), fragment.getVisitDurationSnapshot());
                }
            }
            for (SalesProductDayResourceOptionalItemEntity item : optionalByResource.getOrDefault(resource.getId(), List.of())) {
                addHiddenParagraph(document, ("scenic_transport".equals(item.getItemTypeSnapshot()) ? "景区小交通：【" : "推荐自费：【")
                        + item.getProjectNameSnapshot() + "-" + item.getFinalSalePrice().stripTrailingZeros().toPlainString() + "元/人】", "C00000", true);
            }
        }
        if (description != null) {
            appendLegacySection(document, "产品说明", description.getProductDescription());
            appendLegacySection(document, "费用包含", description.getFeeIncluded());
            appendLegacySection(document, "费用不含", description.getFeeExcluded());
        }
    }

    private void appendLegacyIntroduction(XWPFDocument document, String title, String content, String notice, String warmTip, String duration) {
        if (StringUtils.hasText(title)) addHiddenParagraph(document, title, null, false);
        if (content != null) for (String line : content.split("\\R", -1)) addHiddenParagraph(document, line, null, false);
        if (StringUtils.hasText(duration)) addHiddenParagraph(document, "游览时间：" + duration, null, false);
        if (warmTip != null) for (String line : warmTip.split("\\R", -1)) addHiddenParagraph(document, line, null, false);
        if (StringUtils.hasText(notice)) for (String line : notice.split("\\R")) addHiddenParagraph(document, line.trim(), "C00000", false);
    }

    private void appendLegacySection(XWPFDocument document, String title, String content) {
        if (!StringUtils.hasText(content)) return;
        addHiddenParagraph(document, title, null, true);
        for (String line : content.split("\\R", -1)) addHiddenParagraph(document, line, null, false);
    }

    private void addHiddenParagraph(XWPFDocument document, String value, String color, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        if (value != null && INTRODUCTION_LIST_LINE.matcher(value.strip()).matches()) {
            paragraph.setIndentationLeft(INTRODUCTION_LIST_INDENT_TWIPS);
            paragraph.setIndentationHanging(INTRODUCTION_LIST_INDENT_TWIPS);
        }
        XWPFRun run = textRun(paragraph, value == null ? "" : value, 10, bold);
        run.setVanish(true);
        if (color != null) run.setColor(color);
    }

    private List<SalesProductDayResourceOptionalItemEntity> optionalItems(Long tenantId, Long productId) {
        if (optionalItemMapper == null) return List.of();
        return optionalItemMapper.selectList(new QueryWrapper<SalesProductDayResourceOptionalItemEntity>()
                .eq("tenant_id", tenantId).eq("product_id", productId).eq("is_deleted", false)
                .orderByAsc("day_resource_id").orderByAsc("sort_order").orderByAsc("id"));
    }

    /** 对外 Word 只输出最终游客价及介绍素材，不暴露供应商、成本或建议价。 */
    private void addOptionalItemFragment(XWPFDocument document, SalesProductDayResourceOptionalItemEntity item) {
        XWPFParagraph paragraph = document.createParagraph(); paragraph.setSpacingBefore(80); paragraph.setSpacingAfter(80);
        XWPFRun run = textRun(paragraph, ("scenic_transport".equals(item.getItemTypeSnapshot()) ? "景区小交通：【" : "推荐自费：【") + item.getProjectNameSnapshot() + "-" + item.getFinalSalePrice().stripTrailingZeros().toPlainString() + "元/人】", 10, true); run.setColor("C00000");
        introductionParagraphs(document, item.getIntroductionContentSnapshot());
        introductionParagraphs(document, item.getIntroductionWarmTipSnapshot()); noticeParagraphs(document, item.getIntroductionNoticeSnapshot());
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

    private void addIntroductionFragment(
            XWPFDocument document,
            String title,
            String content,
            String notice,
            String warmTip,
            String visitDuration
    ) {
        if (StringUtils.hasText(title)) {
            paragraph(document, title);
        }
        if (StringUtils.hasText(content)) {
            introductionParagraphs(document, content);
        }
        if (StringUtils.hasText(visitDuration)) {
            paragraph(document, "游览时间：" + visitDuration);
        }
        introductionParagraphs(document, warmTip);
        noticeParagraphs(document, notice);
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

    /** 将介绍正文的逻辑换行转换为真正的 Word 段落，保留空行和编号行的悬挂缩进。 */
    private void introductionParagraphs(XWPFDocument document, String value) {
        if (value == null) return;
        for (String line : value.split("\\R", -1)) {
            String text = line == null ? "" : line.strip();
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setSpacingAfter(80);
            if (INTRODUCTION_LIST_LINE.matcher(text).matches()) {
                paragraph.setIndentationLeft(INTRODUCTION_LIST_INDENT_TWIPS);
                paragraph.setIndentationHanging(INTRODUCTION_LIST_INDENT_TWIPS);
            }
            textRun(paragraph, text, 10, false);
        }
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
            List<SalesProductDayResourceImageEntity> selectedImages,
            List<SalesProductDayResourceIntroductionEntity> selectedIntroductions
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
            resource.put("introductionWarmTip", item.getIntroductionWarmTipSnapshot() == null ? "" : item.getIntroductionWarmTipSnapshot());
            resource.put("introductionVisitDuration", item.getIntroductionVisitDurationSnapshot() == null ? "" : item.getIntroductionVisitDurationSnapshot());
            resource.put("introductionFragments", selectedIntroductions.stream()
                    .filter(fragment -> java.util.Objects.equals(fragment.getDayResourceId(), item.getId()))
                    .map(fragment -> Map.of(
                            "resourceIntroductionId", fragment.getResourceIntroductionId(),
                            "title", safe(fragment.getTitleSnapshot()),
                            "content", safe(fragment.getContentSnapshot()),
                            "notice", safe(fragment.getNoticeSnapshot()),
                            "warmTip", safe(fragment.getWarmTipSnapshot()),
                            "visitDuration", safe(fragment.getVisitDurationSnapshot()),
                            "sortOrder", fragment.getSortOrder()
                    ))
                    .toList());
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
                .orderByAsc("sort_order")
                .orderByAsc("id"));
    }

    private List<SalesProductDayResourceIntroductionEntity> selectedIntroductions(
            Long tenantId,
            Long productId
    ) {
        return dayResourceIntroductionMapper.selectList(
                        new QueryWrapper<SalesProductDayResourceIntroductionEntity>()
                                .eq("tenant_id", tenantId)
                                .eq("is_deleted", false)
                                .eq("product_id", productId)
                                .orderByAsc("day_resource_id")
                                .orderByAsc("sort_order")
                                .orderByAsc("id"))
                .stream()
                .toList();
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
