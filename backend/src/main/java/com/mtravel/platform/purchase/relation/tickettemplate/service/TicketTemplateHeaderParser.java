package com.mtravel.platform.purchase.relation.tickettemplate.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateHeaderResponse;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TicketTemplateFillMode;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TouristSystemField;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 游客名单 Excel 模板表头解析器。
 *
 * <p>只读取模板表头，不保存游客数据。解析结果用于配置“模板列 -> 系统游客字段”的映射。</p>
 */
@Component
public class TicketTemplateHeaderParser {

    private final DataFormatter dataFormatter = new DataFormatter();

    /**
     * 读取指定表头行。
     *
     * @param input Excel 文件输入流
     * @param headerRow 表头行号，按 Excel 习惯从 1 开始
     * @return 解析出的表头和系统字段建议映射
     */
    public TicketTemplateHeaderResponse parse(InputStream input, Integer headerRow) {
        int rowNumber = headerRow == null || headerRow < 1 ? 1 : headerRow;
        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BizException("Excel 模板没有工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(rowNumber - 1);
            if (row == null) {
                throw new BizException("未读取到模板表头，请检查表头行");
            }
            List<TicketTemplateHeaderResponse.Header> headers = readHeaders(row);
            if (headers.isEmpty()) {
                throw new BizException("模板表头不能为空");
            }
            return new TicketTemplateHeaderResponse(sheet.getSheetName(), rowNumber, headers);
        } catch (IOException ex) {
            throw new BizException("Excel 模板读取失败");
        }
    }

    private List<TicketTemplateHeaderResponse.Header> readHeaders(Row row) {
        List<TicketTemplateHeaderResponse.Header> headers = new ArrayList<>();
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return headers;
        }
        for (int i = 0; i < lastCellNum; i++) {
            String header = cellText(row.getCell(i));
            if (!StringUtils.hasText(header)) {
                continue;
            }
            TouristSystemField suggested = TouristSystemField.suggestByHeader(header).orElse(null);
            TicketTemplateFillMode fillMode = suggested == null
                    ? TicketTemplateFillMode.KEEP_ORIGINAL
                    : TicketTemplateFillMode.TOURIST_FIELD;
            headers.add(new TicketTemplateHeaderResponse.Header(
                    i + 1,
                    header.trim(),
                    suggested == null ? null : suggested.value(),
                    suggested == null ? null : suggested.label(),
                    fillMode.value(),
                    null,
                    header.contains("必填") || header.contains("*")
            ));
        }
        return headers;
    }

    private String cellText(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        return dataFormatter.formatCellValue(cell);
    }
}
