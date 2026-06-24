package com.mtravel.platform.sales.booking.aiimport.service;

import com.mtravel.platform.common.BizException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
        String type = StringUtils.hasText(sourceType) ? sourceType.trim().toLowerCase() : "txt";
        try {
            return switch (type) {
                case "txt", "text", "csv" -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                case "docx", "word" -> extractDocx(inputStream);
                case "xlsx", "xls", "excel" -> extractExcel(inputStream);
                case "pdf", "image", "jpg", "jpeg", "png" -> extractByVision(inputStream, type, tenantId);
                default -> throw new BizException("暂不支持该文件类型，请上传Word、Excel或粘贴文本");
            };
        } catch (IOException ex) {
            throw new BizException("确认单文件读取失败");
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

    private void addLine(List<String> lines, String value) {
        if (StringUtils.hasText(value)) {
            lines.add(value.trim());
        }
    }

    private String extractByVision(InputStream inputStream, String sourceType, Long tenantId) throws IOException {
        if (aiModelClient == null) {
            throw new BizException("当前文件需要AI视觉/OCR识别服务，请先配置百炼视觉模型后使用");
        }
        return aiModelClient.recognizeImageOrDocument(tenantId, sourceType, inputStream.readAllBytes())
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new BizException("当前文件需要AI视觉/OCR识别服务，请先配置百炼视觉模型后使用"));
    }
}
