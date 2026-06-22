package com.mtravel.platform.purchase.relation.tickettemplate.dto;

import java.util.List;

/**
 * 游客名单 Excel 模板表头解析结果。
 *
 * <p>前端上传模板后先读取表头，再由用户确认每一列对应系统里的游客字段。</p>
 */
public record TicketTemplateHeaderResponse(
        String sheetName,
        Integer headerRow,
        List<Header> headers
) {
    /** 单个 Excel 表头列及系统字段建议映射。 */
    public record Header(
            Integer columnIndex,
            String templateHeader,
            String systemField,
            String systemFieldLabel,
            String fillMode,
            String fixedValue,
            Boolean required
    ) {
    }
}
