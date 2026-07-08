package com.mtravel.platform.sales.booking.aiimport.service;

import com.mtravel.platform.common.BizException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 确认单附件文本提取器。
 *
 * <p>负责把 Word、Excel、文本附件提取成统一文本，再交给 AI/规则解析。PDF 和图片走 OCR 或多模态
 * 模型；未配置视觉模型或模型无返回时返回明确业务提示。</p>
 */
@Component
public class BookingImportAttachmentTextExtractor {

    private final AiModelClient aiModelClient;

    public BookingImportAttachmentTextExtractor() {
        this(null);
    }

    @Autowired
    public BookingImportAttachmentTextExtractor(AiModelClient aiModelClient) {
        this.aiModelClient = aiModelClient;
    }

    /**
     * 按来源类型提取附件文本。
     */
    public String extract(InputStream inputStream, String sourceType) {
        return extract(inputStream, sourceType, null);
    }

    /**
     * 按来源类型提取附件文本。
     *
     * @param tenantId 当前租户 ID。图片/PDF 需要用它读取租户级百炼配置。
     */
    public String extract(InputStream inputStream, String sourceType, Long tenantId) {
        String type = normalizeSourceType(sourceType);
        try {
            return switch (type) {
                case "txt", "text", "csv" -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                case "doc" -> extractDoc(inputStream);
                case "docx", "word" -> extractDocx(inputStream);
                case "xlsx", "xls", "excel" -> extractExcel(inputStream);
                case "pdf" -> extractPdfTextOrVision(inputStream, tenantId);
                case "image", "jpg", "jpeg", "png", "webp", "bmp" -> extractByVision(inputStream, type, tenantId);
                default -> throw new BizException("暂不支持该文件类型，请上传 Word、Excel、PDF、图片或粘贴文本");
            };
        } catch (IOException ex) {
            throw new BizException("确认单文件读取失败，请检查文件是否加密或损坏");
        } catch (IllegalArgumentException | org.apache.poi.EncryptedDocumentException ex) {
            throw new BizException("确认单文件读取失败，请检查文件是否加密或损坏");
        }
    }

    private String normalizeSourceType(String sourceType) {
        String type = StringUtils.hasText(sourceType) ? sourceType.trim().toLowerCase() : "txt";
        return type.startsWith(".") ? type.substring(1) : type;
    }

    private String extractDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            List<String> lines = new ArrayList<>();
            for (String paragraph : extractor.getParagraphText()) {
                addLine(lines, paragraph);
            }
            return String.join("\n", lines);
        }
    }

    private String extractDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<String> lines = new ArrayList<>();
            document.getParagraphs().forEach(paragraph -> addLine(lines, paragraph.getText()));
            document.getTables().forEach(table -> table.getRows().forEach(row -> {
                List<String> cells = row.getTableCells().stream()
                        .map(cell -> cell.getText().replace('\n', ' ').trim())
                        .filter(StringUtils::hasText)
                        .toList();
                addLine(lines, String.join(" ", cells));
            }));
            return String.join("\n", lines);
        }
    }

    private String extractExcel(InputStream inputStream) throws IOException {
        try (var workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            List<String> lines = new ArrayList<>();
            workbook.forEach(sheet -> sheet.forEach(row -> {
                List<String> cells = new ArrayList<>();
                row.forEach(cell -> {
                    String value = formatter.formatCellValue(cell).trim();
                    if (StringUtils.hasText(value)) {
                        cells.add(value);
                    }
                });
                addLine(lines, String.join(" ", cells));
            }));
            return String.join("\n", lines);
        }
    }

    private String extractPdfTextOrVision(InputStream inputStream, Long tenantId) throws IOException {
        byte[] content = inputStream.readAllBytes();
        try (var document = Loader.loadPDF(content)) {
            String text = new PDFTextStripper().getText(document);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return extractByVision(content, "pdf", tenantId);
    }

    private void addLine(List<String> lines, String value) {
        if (StringUtils.hasText(value)) {
            lines.add(value.trim());
        }
    }

    private String extractByVision(InputStream inputStream, String sourceType, Long tenantId) throws IOException {
        return extractByVision(inputStream.readAllBytes(), sourceType, tenantId);
    }

    private String extractByVision(byte[] content, String sourceType, Long tenantId) {
        if (aiModelClient == null) {
            throw new BizException("当前文件需要AI视觉/OCR识别服务，识别服务未启用");
        }
        return aiModelClient.recognizeImageOrDocument(tenantId, sourceType, content)
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new BizException("百炼视觉/OCR识别未返回内容，请检查API Key、模型名称或稍后重试"));
    }
}
