package com.mtravel.platform.dispatch.teamarrangement.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.tickettemplate.entity.PurchaseRelationTicketTemplateEntity;
import com.mtravel.platform.purchase.relation.tickettemplate.entity.PurchaseRelationTicketTemplateFieldEntity;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TicketTemplateFillMode;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TicketTemplateStatus;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TouristSystemField;
import com.mtravel.platform.purchase.relation.tickettemplate.mapper.PurchaseRelationTicketTemplateFieldMapper;
import com.mtravel.platform.purchase.relation.tickettemplate.mapper.PurchaseRelationTicketTemplateMapper;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceType;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderRole;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderStatus;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderGuestMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 团队景区票务游客名单导出服务。
 *
 * <p>景区供应商通常通过自己的票务系统 Excel 模板批量购票。本服务按“景区资源 + 供应商”
 * 的采购关系读取已配置模板，并把当前团队有效游客写入模板，不把票务模板规则混入成本安排表。</p>
 */
@Service
public class DispatchScenicTicketGuestExportService {

    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PurchaseRelationMapper relationMapper;
    private final PurchaseRelationTicketTemplateMapper templateMapper;
    private final PurchaseRelationTicketTemplateFieldMapper fieldMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final SalesBookingOrderGuestMapper guestMapper;
    private final CommonAttachmentService attachmentService;

    /**
     * 构造团队景区票务游客名单导出服务。
     */
    public DispatchScenicTicketGuestExportService(
            PurchaseRelationMapper relationMapper,
            PurchaseRelationTicketTemplateMapper templateMapper,
            PurchaseRelationTicketTemplateFieldMapper fieldMapper,
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderGuestMapper guestMapper,
            CommonAttachmentService attachmentService
    ) {
        this.relationMapper = relationMapper;
        this.templateMapper = templateMapper;
        this.fieldMapper = fieldMapper;
        this.orderMapper = orderMapper;
        this.guestMapper = guestMapper;
        this.attachmentService = attachmentService;
    }

    /**
     * 生成景区票务系统游客 Excel。
     *
     * @param teamId 团队 ID
     * @param resourceName 景区资源名称
     * @param supplierId 供应商 ID
     * @param tenantId 当前租户 ID
     * @return 导出文件名和 Excel 二进制内容
     */
    public ExportResult export(Long teamId, String resourceName, Long supplierId, Long tenantId) {
        PurchaseRelationEntity relation = requireRelation(resourceName, supplierId, tenantId);
        PurchaseRelationTicketTemplateEntity template = requireTemplate(relation.getId(), tenantId);
        List<PurchaseRelationTicketTemplateFieldEntity> fields = loadFields(template.getId(), tenantId);
        List<SalesBookingOrderGuestEntity> guests = loadEffectiveGuests(teamId, tenantId);
        if (guests.isEmpty()) {
            throw new BizException("当前团队暂无可导出的游客名单");
        }
        ByteArrayOutputStream content = fillWorkbook(template, fields, guests, tenantId);
        String filename = "%s游客名单%s.%s".formatted(
                cleanFilenamePart(resourceName, "景区"),
                LocalDateTime.now().format(EXPORT_TIME_FORMATTER),
                extension(template.getOriginalFilename())
        );
        return new ExportResult(filename, content);
    }

    /** 导出结果对象，供 Controller 写入 HTTP 下载响应。 */
    public record ExportResult(String filename, ByteArrayOutputStream content) {
    }

    /** 查询景区资源与供应商的启用采购关系。 */
    private PurchaseRelationEntity requireRelation(String resourceName, Long supplierId, Long tenantId) {
        if (!StringUtils.hasText(resourceName) || supplierId == null) {
            throw new BizException("请选择景区和供应商后再下载游客名单");
        }
        List<PurchaseRelationEntity> relations = relationMapper.selectList(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_type", PurchaseResourceType.SCENIC.value())
                .eq("status", "active")
                .eq("resource_name", resourceName.trim())
                .eq("supplier_id", supplierId)
                .orderByDesc("id"));
        if (CollectionUtils.isEmpty(relations)) {
            throw new BizException("当前景区供应商未配置游客名单模板，请先配置模板");
        }
        return relations.getFirst();
    }

    /** 查询采购关系下的启用游客名单模板。 */
    private PurchaseRelationTicketTemplateEntity requireTemplate(Long relationId, Long tenantId) {
        PurchaseRelationTicketTemplateEntity template = templateMapper.selectOne(new QueryWrapper<PurchaseRelationTicketTemplateEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("relation_id", relationId)
                .eq("status", TicketTemplateStatus.ACTIVE.value()));
        if (template == null || template.getAttachmentId() == null) {
            throw new BizException("当前景区供应商未配置游客名单模板，请先配置模板");
        }
        return template;
    }

    /** 查询模板字段映射，按页面排序和列序号稳定输出。 */
    private List<PurchaseRelationTicketTemplateFieldEntity> loadFields(Long templateId, Long tenantId) {
        List<PurchaseRelationTicketTemplateFieldEntity> fields = fieldMapper.selectList(new QueryWrapper<PurchaseRelationTicketTemplateFieldEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("template_id", templateId)
                .orderByAsc("sort_order")
                .orderByAsc("column_index"));
        if (CollectionUtils.isEmpty(fields)) {
            throw new BizException("游客名单模板字段映射为空，请先配置模板");
        }
        return fields;
    }

    /** 加载当前团队需要实际购票的游客，排除取消订单和拼团来源留痕订单。 */
    private List<SalesBookingOrderGuestEntity> loadEffectiveGuests(Long teamId, Long tenantId) {
        List<SalesBookingOrderEntity> orders = orderMapper.selectList(new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .in("status", List.of(
                        SalesBookingOrderStatus.PENDING.value(),
                        SalesBookingOrderStatus.CONFIRMED.value()
                ))
                .and(roleWrapper -> roleWrapper
                        .in("order_role", List.of(
                                SalesBookingOrderRole.NORMAL.value(),
                                SalesBookingOrderRole.MERGE_CHILD.value()
                        ))
                        .or()
                        .isNull("order_role"))
                .orderByAsc("id"));
        if (CollectionUtils.isEmpty(orders)) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(SalesBookingOrderEntity::getId).toList();
        List<SalesBookingOrderGuestEntity> guests = guestMapper.selectList(new QueryWrapper<SalesBookingOrderGuestEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .in("order_id", orderIds)
                .orderByAsc("order_id")
                .orderByAsc("index_no")
                .orderByAsc("id"));
        return guests == null ? List.of() : guests;
    }

    /** 按模板字段映射填充 Excel，保留模板原有表头和样式。 */
    private ByteArrayOutputStream fillWorkbook(
            PurchaseRelationTicketTemplateEntity template,
            List<PurchaseRelationTicketTemplateFieldEntity> fields,
            List<SalesBookingOrderGuestEntity> guests,
            Long tenantId
    ) {
        try (InputStream input = attachmentService.openStream(template.getAttachmentId(), tenantId);
            Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = resolveSheet(workbook, template.getSheetName());
            int startRowIndex = Math.max(number(template.getDataStartRow()), 1) - 1;
            Row styleRow = sheet.getRow(startRowIndex);
            Map<Integer, TemplateCellSnapshot> styleSnapshots = snapshotTemplateRow(styleRow);
            removeReservedDataRows(sheet, startRowIndex);
            for (int index = 0; index < guests.size(); index++) {
                Row row = sheet.getRow(startRowIndex + index);
                if (row == null) {
                    row = sheet.createRow(startRowIndex + index);
                }
                writeGuest(row, styleSnapshots, fields, guests.get(index), index + 1);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output;
        } catch (IOException exception) {
            throw new BizException("游客名单导出失败");
        }
    }

    /** 清理模板数据区的预留空行，避免导出后出现多余的“身份证”等默认内容。 */
    private void removeReservedDataRows(Sheet sheet, int startRowIndex) {
        for (int rowIndex = sheet.getLastRowNum(); rowIndex >= startRowIndex; rowIndex--) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    /** 选择模板配置的工作表，配置缺失或不存在时使用第一张表。 */
    private Sheet resolveSheet(Workbook workbook, String sheetName) {
        if (StringUtils.hasText(sheetName)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet != null) {
                return sheet;
            }
        }
        if (workbook.getNumberOfSheets() == 0) {
            throw new BizException("游客名单模板没有工作表");
        }
        return workbook.getSheetAt(0);
    }

    /** 写入单个游客的一行模板字段。 */
    private void writeGuest(
            Row row,
            Map<Integer, TemplateCellSnapshot> styleSnapshots,
            List<PurchaseRelationTicketTemplateFieldEntity> fields,
            SalesBookingOrderGuestEntity guest,
            int sequence
    ) {
        for (PurchaseRelationTicketTemplateFieldEntity field : fields) {
            int columnIndex = Math.max(number(field.getColumnIndex()), 1) - 1;
            TicketTemplateFillMode fillMode = TicketTemplateFillMode.fromValue(field.getFillMode())
                    .orElse(TicketTemplateFillMode.TOURIST_FIELD);
            Cell cell = row.getCell(columnIndex);
            if (cell == null) {
                cell = row.createCell(columnIndex);
                copyStyle(cell, styleSnapshots, columnIndex);
            }
            if (fillMode == TicketTemplateFillMode.KEEP_ORIGINAL) {
                copyOriginalValue(cell, styleSnapshots, columnIndex);
                continue;
            }
            if (fillMode == TicketTemplateFillMode.SEQUENCE) {
                cell.setCellValue(sequence);
            } else {
                cell.setCellValue(resolveCellValue(fillMode, field, guest));
            }
        }
    }

    /** 复制模板数据首行的样式和原值，清理预留行后仍可复用模板格式。 */
    private Map<Integer, TemplateCellSnapshot> snapshotTemplateRow(Row styleRow) {
        Map<Integer, TemplateCellSnapshot> snapshots = new HashMap<>();
        if (styleRow == null) {
            return snapshots;
        }
        for (Cell sourceCell : styleRow) {
            snapshots.put(sourceCell.getColumnIndex(), new TemplateCellSnapshot(
                    sourceCell.getCellStyle(),
                    cellText(sourceCell)
            ));
        }
        return snapshots;
    }

    /** 新增单元格时复制模板数据行同列样式，避免破坏供应商要求的表格格式。 */
    private void copyStyle(Cell cell, Map<Integer, TemplateCellSnapshot> styleSnapshots, int columnIndex) {
        TemplateCellSnapshot snapshot = styleSnapshots.get(columnIndex);
        CellStyle style = snapshot == null ? null : snapshot.style();
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    /** 保留模板列原值，适配固定说明列或供应商要求的默认值列。 */
    private void copyOriginalValue(Cell cell, Map<Integer, TemplateCellSnapshot> styleSnapshots, int columnIndex) {
        TemplateCellSnapshot snapshot = styleSnapshots.get(columnIndex);
        cell.setCellValue(snapshot == null ? "" : snapshot.value());
    }

    /** 将模板单元格按文本快照保存，导出时用于 keep_original 列。 */
    private String cellText(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /** 根据字段映射取游客资料或固定值。 */
    private String resolveCellValue(
            TicketTemplateFillMode fillMode,
            PurchaseRelationTicketTemplateFieldEntity field,
            SalesBookingOrderGuestEntity guest
    ) {
        if (fillMode == TicketTemplateFillMode.CONSTANT) {
            return nullToBlank(field.getFixedValue());
        }
        TouristSystemField systemField = TouristSystemField.fromValue(field.getSystemField()).orElse(null);
        if (systemField == null) {
            return "";
        }
        return switch (systemField) {
            case TOURIST_NAME -> nullToBlank(guest.getGuestName());
            case CERTIFICATE_TYPE -> certificateType(guest);
            case CERTIFICATE_NO -> nullToBlank(guest.getCertificateNo());
            case MOBILE -> nullToBlank(guest.getPhone());
            case GENDER -> nullToBlank(guest.getGender());
            case BIRTHDAY -> guest.getBirthDate() == null ? "" : guest.getBirthDate().toString();
            case REMARK -> nullToBlank(guest.getRemark());
        };
    }

    /** 按游客证件号推断票务模板常用证件类型。 */
    private String certificateType(SalesBookingOrderGuestEntity guest) {
        String certificateNo = guest.getCertificateNo();
        if (StringUtils.hasText(certificateNo) && certificateNo.trim().matches("^[0-9Xx]{18}$")) {
            return "身份证";
        }
        if (StringUtils.hasText(guest.getPassportNo())) {
            return "护照";
        }
        return StringUtils.hasText(certificateNo) ? "护照" : "";
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "xlsx";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "xlsx";
        }
        String extension = filename.substring(index + 1).toLowerCase();
        return List.of("xls", "xlsx").contains(extension) ? extension : "xlsx";
    }

    private String cleanFilenamePart(String value, String fallback) {
        String text = StringUtils.hasText(value) ? value.trim() : fallback;
        return text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    /** 模板数据首行的单元格样式和原值快照。 */
    private record TemplateCellSnapshot(CellStyle style, String value) {
    }
}
