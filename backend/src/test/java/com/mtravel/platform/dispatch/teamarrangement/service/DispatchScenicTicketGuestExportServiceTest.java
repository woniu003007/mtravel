package com.mtravel.platform.dispatch.teamarrangement.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.tickettemplate.entity.PurchaseRelationTicketTemplateEntity;
import com.mtravel.platform.purchase.relation.tickettemplate.entity.PurchaseRelationTicketTemplateFieldEntity;
import com.mtravel.platform.purchase.relation.tickettemplate.mapper.PurchaseRelationTicketTemplateFieldMapper;
import com.mtravel.platform.purchase.relation.tickettemplate.mapper.PurchaseRelationTicketTemplateMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderGuestMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 团队景区票务游客名单导出服务测试。
 *
 * <p>该导出给景区供应商票务系统使用，必须按采购关系配置的 Excel 模板填充团队有效游客，
 * 并排除取消订单和拼团来源留痕订单，避免重复购票。</p>
 */
class DispatchScenicTicketGuestExportServiceTest {

    @Test
    void exportShouldFillGuestsIntoConfiguredTicketTemplate() throws Exception {
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseRelationTicketTemplateMapper templateMapper = mock(PurchaseRelationTicketTemplateMapper.class);
        PurchaseRelationTicketTemplateFieldMapper fieldMapper = mock(PurchaseRelationTicketTemplateFieldMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        DispatchScenicTicketGuestExportService service = service(
                relationMapper,
                templateMapper,
                fieldMapper,
                orderMapper,
                guestMapper,
                attachmentService
        );
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(relation()));
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template());
        when(fieldMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                field(1, "sequence", null, null),
                field(2, "tourist_field", "tourist_name", null),
                field(3, "tourist_field", "certificate_type", null),
                field(4, "tourist_field", "certificate_no", null),
                field(5, "constant", null, "成人票"),
                field(6, "keep_original", null, null),
                field(7, "tourist_field", "birthday", null),
                field(8, "tourist_field", "mobile", null),
                field(9, "tourist_field", "remark", null)
        ));
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(order(11L, "normal"), order(12L, "merge_child")));
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                guest(11L, 1, "张三", "330102199001010011", "男", LocalDate.of(1990, 1, 1), "13800000001", "身份证正常"),
                guest(12L, 2, "李四", "P12345678", "女", LocalDate.of(1988, 5, 6), "13800000002", "护照客人")
        ));
        byte[] templateBytes = templateWorkbookBytes();
        when(attachmentService.openStream(eq(501L), eq(1L))).thenAnswer(invocation -> new ByteArrayInputStream(templateBytes));

        DispatchScenicTicketGuestExportService.ExportResult result =
                service.export(21L, "上海豫园", 301L, 1L);

        assertThat(result.filename()).contains("上海豫园").endsWith(".xlsx");
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result.content().toByteArray()))) {
            Sheet sheet = workbook.getSheet("票务模板");
            assertThat(sheet.getRow(2).getCell(0).getNumericCellValue()).isEqualTo(1);
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("张三");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("身份证");
            assertThat(sheet.getRow(2).getCell(3).getStringCellValue()).isEqualTo("330102199001010011");
            assertThat(sheet.getRow(2).getCell(4).getStringCellValue()).isEqualTo("成人票");
            assertThat(sheet.getRow(2).getCell(5).getStringCellValue()).isEqualTo("模板原值");
            assertThat(sheet.getRow(2).getCell(6).getStringCellValue()).isEqualTo("1990-01-01");
            assertThat(sheet.getRow(2).getCell(7).getStringCellValue()).isEqualTo("13800000001");
            assertThat(sheet.getRow(2).getCell(8).getStringCellValue()).isEqualTo("身份证正常");
            assertThat(sheet.getRow(3).getCell(0).getNumericCellValue()).isEqualTo(2);
            assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("李四");
            assertThat(sheet.getRow(3).getCell(2).getStringCellValue()).isEqualTo("护照");
        }
        ArgumentCaptor<Wrapper> orderQueryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).selectList(orderQueryCaptor.capture());
        assertThat(orderQueryCaptor.getValue().getCustomSqlSegment()).contains("order_role", "status", "IS NULL");
    }

    @Test
    void exportShouldRejectWhenTemplateIsMissing() {
        DispatchScenicTicketGuestExportService service = service(
                mock(PurchaseRelationMapper.class),
                mock(PurchaseRelationTicketTemplateMapper.class),
                mock(PurchaseRelationTicketTemplateFieldMapper.class),
                mock(SalesBookingOrderMapper.class),
                mock(SalesBookingOrderGuestMapper.class),
                mock(CommonAttachmentService.class)
        );

        assertThatThrownBy(() -> service.export(21L, "上海豫园", 301L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未配置游客名单模板");
    }

    @Test
    void exportShouldRejectWhenTeamHasNoEffectiveGuests() {
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseRelationTicketTemplateMapper templateMapper = mock(PurchaseRelationTicketTemplateMapper.class);
        PurchaseRelationTicketTemplateFieldMapper fieldMapper = mock(PurchaseRelationTicketTemplateFieldMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        DispatchScenicTicketGuestExportService service = service(
                relationMapper,
                templateMapper,
                fieldMapper,
                orderMapper,
                mock(SalesBookingOrderGuestMapper.class),
                mock(CommonAttachmentService.class)
        );
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(relation()));
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template());
        when(fieldMapper.selectList(any(Wrapper.class))).thenReturn(List.of(field(1, "sequence", null, null)));
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.export(21L, "上海豫园", 301L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("暂无可导出的游客名单");
    }

    @Test
    void exportShouldRemoveUnusedTemplateDataRows() throws Exception {
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        PurchaseRelationTicketTemplateMapper templateMapper = mock(PurchaseRelationTicketTemplateMapper.class);
        PurchaseRelationTicketTemplateFieldMapper fieldMapper = mock(PurchaseRelationTicketTemplateFieldMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        SalesBookingOrderGuestMapper guestMapper = mock(SalesBookingOrderGuestMapper.class);
        CommonAttachmentService attachmentService = mock(CommonAttachmentService.class);
        DispatchScenicTicketGuestExportService service = service(
                relationMapper,
                templateMapper,
                fieldMapper,
                orderMapper,
                guestMapper,
                attachmentService
        );
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(relation()));
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template());
        when(fieldMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                field(1, "tourist_field", "tourist_name", null),
                field(2, "tourist_field", "certificate_type", null),
                field(3, "tourist_field", "certificate_no", null)
        ));
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(order(11L, "normal")));
        when(guestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                guest(11L, 1, "张三", "330102199001010011", "男", LocalDate.of(1990, 1, 1), "13800000001", "")
        ));
        byte[] templateBytes = templateWorkbookWithReservedRowsBytes();
        when(attachmentService.openStream(eq(501L), eq(1L))).thenAnswer(invocation -> new ByteArrayInputStream(templateBytes));

        DispatchScenicTicketGuestExportService.ExportResult result =
                service.export(21L, "上海豫园", 301L, 1L);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(result.content().toByteArray()))) {
            Sheet sheet = workbook.getSheet("票务模板");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("张三");
            assertThat(sheet.getRow(3)).isNull();
            assertThat(sheet.getRow(4)).isNull();
        }
    }

    private DispatchScenicTicketGuestExportService service(
            PurchaseRelationMapper relationMapper,
            PurchaseRelationTicketTemplateMapper templateMapper,
            PurchaseRelationTicketTemplateFieldMapper fieldMapper,
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderGuestMapper guestMapper,
            CommonAttachmentService attachmentService
    ) {
        return new DispatchScenicTicketGuestExportService(
                relationMapper,
                templateMapper,
                fieldMapper,
                orderMapper,
                guestMapper,
                attachmentService
        );
    }

    private PurchaseRelationEntity relation() {
        PurchaseRelationEntity entity = new PurchaseRelationEntity();
        entity.setId(401L);
        entity.setTenantId(1L);
        entity.setResourceType("scenic");
        entity.setResourceName("上海豫园");
        entity.setSupplierId(301L);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private PurchaseRelationTicketTemplateEntity template() {
        PurchaseRelationTicketTemplateEntity entity = new PurchaseRelationTicketTemplateEntity();
        entity.setId(601L);
        entity.setTenantId(1L);
        entity.setRelationId(401L);
        entity.setTemplateName("豫园票务模板");
        entity.setAttachmentId(501L);
        entity.setOriginalFilename("豫园模板.xlsx");
        entity.setSheetName("票务模板");
        entity.setHeaderRow(2);
        entity.setDataStartRow(3);
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private PurchaseRelationTicketTemplateFieldEntity field(
            int columnIndex,
            String fillMode,
            String systemField,
            String fixedValue
    ) {
        PurchaseRelationTicketTemplateFieldEntity entity = new PurchaseRelationTicketTemplateFieldEntity();
        entity.setId((long) columnIndex);
        entity.setTenantId(1L);
        entity.setTemplateId(601L);
        entity.setColumnIndex(columnIndex);
        entity.setTemplateHeader("C" + columnIndex);
        entity.setFillMode(fillMode);
        entity.setSystemField(systemField);
        entity.setFixedValue(fixedValue);
        entity.setSortOrder(columnIndex);
        entity.setIsDeleted(false);
        return entity;
    }

    private SalesBookingOrderEntity order(Long id, String role) {
        SalesBookingOrderEntity entity = new SalesBookingOrderEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setTeamId(21L);
        entity.setStatus("confirmed");
        entity.setOrderRole(role);
        entity.setIsDeleted(false);
        return entity;
    }

    private SalesBookingOrderGuestEntity guest(
            Long orderId,
            int indexNo,
            String name,
            String certificateNo,
            String gender,
            LocalDate birthDate,
            String phone,
            String remark
    ) {
        SalesBookingOrderGuestEntity entity = new SalesBookingOrderGuestEntity();
        entity.setId(100L + indexNo);
        entity.setTenantId(1L);
        entity.setTeamId(21L);
        entity.setOrderId(orderId);
        entity.setIndexNo(indexNo);
        entity.setGuestName(name);
        entity.setCertificateNo(certificateNo);
        entity.setGender(gender);
        entity.setBirthDate(birthDate);
        entity.setPhone(phone);
        entity.setRemark(remark);
        entity.setIsDeleted(false);
        return entity;
    }

    private byte[] templateWorkbookBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("票务模板");
            Row header = sheet.createRow(1);
            for (int col = 0; col < 9; col++) {
                header.createCell(col).setCellValue("C" + (col + 1));
            }
            Row sample = sheet.createRow(2);
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int col = 0; col < 9; col++) {
                sample.createCell(col).setCellStyle(style);
            }
            sample.getCell(5).setCellValue("模板原值");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] templateWorkbookWithReservedRowsBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("票务模板");
            Row header = sheet.createRow(1);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("证件类型");
            header.createCell(2).setCellValue("证件号");
            for (int rowIndex = 2; rowIndex <= 4; rowIndex++) {
                Row reserved = sheet.createRow(rowIndex);
                reserved.createCell(0).setCellValue("");
                reserved.createCell(1).setCellValue("身份证");
                reserved.createCell(2).setCellValue("");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
