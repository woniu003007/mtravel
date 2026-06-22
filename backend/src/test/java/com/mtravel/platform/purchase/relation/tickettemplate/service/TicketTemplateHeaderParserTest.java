package com.mtravel.platform.purchase.relation.tickettemplate.service;

import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateHeaderResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTemplateHeaderParserTest {

    @Test
    void parseShouldReadHeaderRowAndMapCommonTouristFields() throws Exception {
        byte[] workbookBytes = workbookWithHeaders("姓名(必填)", "证件类型(必填)", "证件号码(必填)", "手机号");
        TicketTemplateHeaderParser parser = new TicketTemplateHeaderParser();

        TicketTemplateHeaderResponse response = parser.parse(new ByteArrayInputStream(workbookBytes), 1);

        assertThat(response.sheetName()).isEqualTo("团单快捷购票游客证件信息模板");
        assertThat(response.headers()).extracting(TicketTemplateHeaderResponse.Header::templateHeader)
                .containsExactly("姓名(必填)", "证件类型(必填)", "证件号码(必填)", "手机号");
        assertThat(response.headers()).extracting(TicketTemplateHeaderResponse.Header::systemField)
                .containsExactly("tourist_name", "certificate_type", "certificate_no", "mobile");
        assertThat(response.headers()).extracting(TicketTemplateHeaderResponse.Header::required)
                .containsExactly(true, true, true, false);
    }

    private byte[] workbookWithHeaders(String... headers) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("团单快捷购票游客证件信息模板");
            Row row = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                row.createCell(i).setCellValue(headers[i]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
