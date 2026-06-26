package com.mtravel.platform.sales.booking.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.risk.dto.CustomerRiskApprovalResponse;
import com.mtravel.platform.customer.risk.service.CustomerRiskApprovalService;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import com.mtravel.platform.sales.booking.aiimport.service.IdCardValidationResult;
import com.mtravel.platform.sales.booking.aiimport.service.IdCardValidator;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingFeeChangeCreateRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingFeeChangeResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingGuestImportPreviewResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderGuestRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderGuestResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderPriceLineRequest;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderPriceLineResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderResponse;
import com.mtravel.platform.sales.booking.order.dto.SalesBookingOrderSaveRequest;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderChargeLineEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderChargeLineMapper;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingGuestType;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderRole;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderStatus;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderGuestMapper;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.ordertransfer.entity.SalesOrderTransferLogEntity;
import com.mtravel.platform.sales.ordertransfer.mapper.SalesOrderTransferLogMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.enums.SalesTeamStatus;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 销售收客订单业务服务。
 *
 * <p>服务层负责旧系统收客页主链路：保存订单主信息、价格明细、游客名单，并根据已确认订单
 * 聚合刷新团队实收和余位。Controller 不处理这些业务规则。</p>
 */
@Service
public class SalesBookingOrderService {

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private final SalesBookingOrderMapper orderMapper;
    private final SalesBookingOrderChargeLineMapper chargeLineMapper;
    private final SalesBookingOrderGuestMapper guestMapper;
    private final SalesTeamMapper teamMapper;
    private final IdCardValidator idCardValidator;
    private final EnterpriseExpenseItemMapper expenseItemMapper;
    private final CustomerRiskApprovalService riskApprovalService;
    private final SalesOrderTransferLogMapper transferLogMapper;

    /**
     * 单元测试兼容构造器。测试只验证主链路，可不显式传身份证校验器。
     */
    public SalesBookingOrderService(
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderChargeLineMapper chargeLineMapper,
            SalesBookingOrderGuestMapper guestMapper,
            SalesTeamMapper teamMapper
    ) {
        this(orderMapper, chargeLineMapper, guestMapper, teamMapper, new IdCardValidator(), null, null, null);
    }

    /**
     * 风控测试构造器，允许注入客户风险审批服务。
     */
    public SalesBookingOrderService(
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderChargeLineMapper chargeLineMapper,
            SalesBookingOrderGuestMapper guestMapper,
            SalesTeamMapper teamMapper,
            CustomerRiskApprovalService riskApprovalService
    ) {
        this(orderMapper, chargeLineMapper, guestMapper, teamMapper, new IdCardValidator(), null, riskApprovalService, null);
    }

    /**
     * 费用变更测试构造器，允许注入费用项目 Mapper 校验附加费用项目。
     */
    public SalesBookingOrderService(
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderChargeLineMapper chargeLineMapper,
            SalesBookingOrderGuestMapper guestMapper,
            SalesTeamMapper teamMapper,
            EnterpriseExpenseItemMapper expenseItemMapper
    ) {
        this(orderMapper, chargeLineMapper, guestMapper, teamMapper, new IdCardValidator(), expenseItemMapper, null, null);
    }

    /**
     * 运行时构造器，注入订单、价格、游客、费用变更、团队 Mapper 及身份证校验器。
     */
    @Autowired
    public SalesBookingOrderService(
            SalesBookingOrderMapper orderMapper,
            SalesBookingOrderChargeLineMapper chargeLineMapper,
            SalesBookingOrderGuestMapper guestMapper,
            SalesTeamMapper teamMapper,
            IdCardValidator idCardValidator,
            EnterpriseExpenseItemMapper expenseItemMapper,
            CustomerRiskApprovalService riskApprovalService,
            SalesOrderTransferLogMapper transferLogMapper
    ) {
        this.orderMapper = orderMapper;
        this.chargeLineMapper = chargeLineMapper;
        this.guestMapper = guestMapper;
        this.teamMapper = teamMapper;
        this.idCardValidator = idCardValidator == null ? new IdCardValidator() : idCardValidator;
        this.expenseItemMapper = expenseItemMapper;
        this.riskApprovalService = riskApprovalService;
        this.transferLogMapper = transferLogMapper;
    }

    /**
     * 保存收客订单。
     *
     * <p>新增订单时自动生成订单编号；修改订单时校验订单属于当前租户和团队。价格明细与游客名单
     * 按当前订单重建为未删除数据，避免逐行匹配导致历史脏数据残留。保存完成后按团队聚合刷新实收人数。</p>
     *
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 保存后的订单详情
     */
    @Transactional
    public SalesBookingOrderResponse save(SalesBookingOrderSaveRequest request, Long tenantId, String operator) {
        SalesTeamEntity team = requireTeam(request.teamId(), tenantId);
        SalesBookingOrderStatus status = parseStatus(request.status());
        SalesBookingOrderEntity current = request.id() == null ? null : requireOrder(request.id(), tenantId);
        assertOrderEditable(current);
        assertTeamCanReceive(team, status, current == null);
        BigDecimal requestedReceivable = sumReceivable(request.priceLines());
        if (current != null && current.getId() != null) {
            requestedReceivable = requestedReceivable.add(activeFeeChangeTotal(current.getId(), tenantId)).setScale(2, RoundingMode.HALF_UP);
        }
        assertCustomerRiskApproved(request, current, tenantId, requestedReceivable);

        SalesBookingOrderEntity entity = current == null ? new SalesBookingOrderEntity() : new SalesBookingOrderEntity();
        applyOrderFields(entity, request, team, status, tenantId, operator, current);
        if (current == null) {
            orderMapper.insert(entity);
        } else {
            orderMapper.update(entity, baseOrderUpdate(tenantId).eq("id", current.getId()));
            entity.setId(current.getId());
        }

        rebuildPriceLines(entity, request.priceLines(), tenantId, operator);
        rebuildGuests(entity, request.guests(), tenantId, operator);
        bindRiskApprovalRequest(request, entity, tenantId);
        refreshTeamSeats(team, tenantId);

        return toResponse(entity);
    }

    /**
     * 查询收客订单详情。
     *
     * @param orderId 订单 ID
     * @param tenantId 当前租户 ID
     * @return 订单详情
     */
    public SalesBookingOrderResponse detail(Long orderId, Long tenantId) {
        SalesBookingOrderEntity order = requireOrder(orderId, tenantId);
        return withRiskApprovalRequestId(toResponse(order), order);
    }

    /**
     * 查询团队下订单列表。
     *
     * <p>供团队操作页展示订单行使用，只加载当前团队订单，不附带游客名单和价格明细大对象。</p>
     */
    public List<SalesBookingOrderEntity> listOrdersByTeam(Long teamId, Long tenantId) {
        return orderMapper.selectActiveByTeam(tenantId, teamId);
    }

    private SalesBookingOrderResponse toResponse(SalesBookingOrderEntity order) {
        Long tenantId = order.getTenantId();
        Long orderId = order.getId();
        List<SalesBookingOrderChargeLineEntity> priceLineEntities = Objects.requireNonNullElse(
                chargeLineMapper.selectList(baseChargeLineQuery(tenantId)
                        .eq("order_id", orderId)
                        .eq("line_kind", "base_price")
                        .orderByAsc("sort_order")
                        .orderByAsc("id")),
                List.of()
        );
        List<SalesBookingOrderPriceLineResponse> priceLines = priceLineEntities
                .stream()
                .map(SalesBookingOrderPriceLineResponse::fromEntity)
                .toList();
        List<SalesBookingOrderGuestEntity> guestEntities = Objects.requireNonNullElse(
                guestMapper.selectList(baseGuestQuery(tenantId)
                        .eq("order_id", orderId)
                        .orderByAsc("index_no")
                        .orderByAsc("id")),
                List.of()
        );
        List<SalesBookingOrderGuestResponse> guests = guestEntities
                .stream()
                .map(SalesBookingOrderGuestResponse::fromEntity)
                .toList();
        List<SalesBookingOrderChargeLineEntity> feeChangeEntities = Objects.requireNonNullElse(
                chargeLineMapper.selectList(baseChargeLineQuery(tenantId)
                        .eq("order_id", orderId)
                        .eq("line_kind", "adjustment")
                        .orderByDesc("registered_at")
                        .orderByDesc("id")),
                List.of()
        );
        List<SalesBookingFeeChangeResponse> feeChanges = feeChangeEntities
                .stream()
                .map(SalesBookingFeeChangeResponse::fromEntity)
                .toList();
        return SalesBookingOrderResponse.fromEntity(order, priceLines, guests, feeChanges);
    }

    private SalesBookingOrderResponse withRiskApprovalRequestId(SalesBookingOrderResponse response, SalesBookingOrderEntity order) {
        if (riskApprovalService == null) {
            return response;
        }
        CustomerRiskApprovalResponse approval = riskApprovalService.latestApprovedForOrder(
                order.getTenantId(),
                order.getCustomerId(),
                order.getTeamId(),
                order.getId()
        );
        return approval == null ? response : response.withRiskApprovalRequestId(approval.id());
    }

    private void applyOrderFields(
            SalesBookingOrderEntity entity,
            SalesBookingOrderSaveRequest request,
            SalesTeamEntity team,
            SalesBookingOrderStatus status,
            Long tenantId,
            String operator,
            SalesBookingOrderEntity current
    ) {
        List<SalesBookingOrderGuestRequest> guests = Objects.requireNonNullElse(request.guests(), List.of());
        List<SalesBookingOrderPriceLineRequest> priceLines = Objects.requireNonNullElse(request.priceLines(), List.of());
        entity.setTenantId(tenantId);
        entity.setTeamId(team.getId());
        entity.setOrderNo(resolveOrderNo(current));
        entity.setCustomerId(request.customerId());
        entity.setCustomerName(clean(request.customerName()));
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setCustomerTeamNo(clean(request.customerTeamNo()));
        entity.setOriginalOrderInfo(resolveOriginalOrderInfo(request, current));
        entity.setSalespersonEmployeeId(request.salespersonEmployeeId());
        entity.setSalespersonEmployeeName(clean(request.salespersonEmployeeName()));
        entity.setBookingOperatorEmployeeId(request.bookingOperatorEmployeeId());
        entity.setBookingOperatorEmployeeName(clean(request.bookingOperatorEmployeeName()));
        entity.setSourceProvince(clean(request.sourceProvince()));
        entity.setSourceCity(clean(request.sourceCity()));
        entity.setSourceDistrict(clean(request.sourceDistrict()));
        entity.setTravelDescription(clean(request.travelDescription()));
        entity.setPickupInfo(clean(request.pickupInfo()));
        entity.setDropoffInfo(clean(request.dropoffInfo()));
        entity.setPickupRemark(clean(request.pickupRemark()));
        entity.setGuideName(clean(request.guideName()));
        entity.setGuidePhone(clean(request.guidePhone()));
        entity.setGuideRemark(clean(request.guideRemark()));
        entity.setHotelInfo(clean(request.hotelInfo()));
        entity.setAdultCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.ADULT));
        entity.setChildCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.CHILD));
        entity.setChildNoBedCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.CHILD_NO_BED));
        entity.setSeniorCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.SENIOR));
        entity.setEscortCount(countByPriceOrGuests(priceLines, guests, SalesBookingGuestType.ESCORT));
        entity.setGuestCount(resolveOrderGuestCount(priceLines, guests));
        BigDecimal receivable = sumReceivable(priceLines);
        if (current != null && current.getId() != null) {
            receivable = receivable.add(activeFeeChangeTotal(current.getId(), tenantId)).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal received = money(request.receivedAmount());
        entity.setReceivableAmount(receivable);
        entity.setReceivedAmount(received);
        entity.setBalanceAmount(receivable.subtract(received).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        entity.setFeeRemark(clean(request.feeRemark()));
        entity.setConfirmRemark(clean(request.confirmRemark()));
        entity.setOrderRemark(clean(request.orderRemark()));
        entity.setStatus(status.value());
        entity.setBookedBy(current == null || !StringUtils.hasText(current.getBookedBy()) ? operator : current.getBookedBy());
        entity.setBookedAt(current == null || current.getBookedAt() == null ? OffsetDateTime.now() : current.getBookedAt());
        entity.setCreatedBy(current == null ? operator : current.getCreatedBy());
        entity.setIsDeleted(false);
    }

    /**
     * 保存订单前校验客户风控审批。
     *
     * <p>风险服务只在运行时注入；单元测试的轻量构造器未注入时跳过，避免旧测试链路必须构造整个客户模块。</p>
     */
    private void assertCustomerRiskApproved(
            SalesBookingOrderSaveRequest request,
            SalesBookingOrderEntity current,
            Long tenantId,
            BigDecimal requestedReceivable
    ) {
        if (riskApprovalService == null || request.customerId() == null) {
            return;
        }
        riskApprovalService.assertOrderCanSave(
                tenantId,
                request.customerId(),
                request.teamId(),
                current == null ? request.id() : current.getId(),
                requestedReceivable,
                request.riskApprovalRequestId()
        );
    }

    /** 保存成功后回填风控审批单订单 ID，便于审批页和订单详情互相追溯。 */
    private void bindRiskApprovalRequest(SalesBookingOrderSaveRequest request, SalesBookingOrderEntity entity, Long tenantId) {
        if (riskApprovalService == null) {
            return;
        }
        riskApprovalService.bindOrder(tenantId, request.riskApprovalRequestId(), entity.getId(), entity.getTeamId());
    }

    /**
     * 新增订单费用变更并立即刷新订单应收。
     *
     * <p>费用项目必须来自企业资料中的附加费用项目；金额按方向转成正负数，状态直接记为 approved，
     * 因为收客页录入的变更当前按业务要求立即生效。</p>
     */
    @Transactional
    public SalesBookingFeeChangeResponse createFeeChange(
            Long orderId,
            SalesBookingFeeChangeCreateRequest request,
            Long tenantId,
            String operator
    ) {
        SalesBookingOrderEntity order = requireOrder(orderId, tenantId);
        assertOrderEditable(order);
        EnterpriseExpenseItemEntity project = requireExtraFeeProject(request.feeProjectId(), tenantId);
        SalesBookingOrderChargeLineEntity entity = new SalesBookingOrderChargeLineEntity();
        entity.setTenantId(tenantId);
        entity.setOrderId(order.getId());
        entity.setTeamId(order.getTeamId());
        entity.setLineKind("adjustment");
        entity.setLineType("extra_fee");
        entity.setItemName(project.getProjectName());
        entity.setChangeType(clean(request.changeType()));
        entity.setFeeProjectId(project.getId());
        entity.setFeeProjectName(project.getProjectName());
        entity.setFeeDescription(clean(request.feeDescription()));
        entity.setAmount(signedFeeChangeAmount(request.changeType(), request.amount()));
        entity.setStatus("approved");
        entity.setRegisteredBy(operator);
        entity.setRegisteredAt(OffsetDateTime.now());
        entity.setCreatedBy(operator);
        entity.setRemark(clean(request.remark()));
        entity.setIsDeleted(false);
        chargeLineMapper.insert(entity);
        applyOrderReceivableDelta(order, entity.getAmount(), tenantId);
        return SalesBookingFeeChangeResponse.fromEntity(entity);
    }

    /**
     * 作废订单费用变更并刷新订单应收。
     *
     * <p>历史费用变更不物理删除，使用 cancelled 状态保留登记记录，统计和订单应收只汇总 approved 记录。</p>
     */
    @Transactional
    public void cancelFeeChange(Long feeChangeId, Long tenantId, String operator) {
        SalesBookingOrderChargeLineEntity current = chargeLineMapper.selectOne(baseChargeLineQuery(tenantId)
                .eq("line_kind", "adjustment")
                .eq("id", feeChangeId));
        if (current == null) {
            throw new BizException("费用变更不存在或已删除");
        }
        SalesBookingOrderEntity order = requireOrder(current.getOrderId(), tenantId);
        assertOrderEditable(order);
        SalesBookingOrderChargeLineEntity update = new SalesBookingOrderChargeLineEntity();
        update.setStatus("cancelled");
        update.setRemark(joinCancelRemark(current.getRemark(), operator));
        int updated = chargeLineMapper.update(update, baseChargeLineUpdate(tenantId)
                .eq("line_kind", "adjustment")
                .eq("id", feeChangeId));
        if (updated == 0) {
            throw new BizException("费用变更不存在或已删除");
        }
        applyOrderReceivableDelta(order, money(current.getAmount()).negate(), tenantId);
    }

    /**
     * 导出老系统样式游客名单 Excel。
     *
     * <p>使用 Excel 97-2003 xls 格式，保持老系统模板中的列、边框、合并区域和文本型证件号/手机号。</p>
     */
    public ByteArrayOutputStream exportGuestWorkbook(Long orderId, Long tenantId) {
        SalesBookingOrderEntity order = requireOrder(orderId, tenantId);
        List<SalesBookingOrderGuestEntity> guests = Objects.requireNonNullElse(
                guestMapper.selectList(baseGuestQuery(tenantId)
                        .eq("order_id", orderId)
                        .orderByAsc("index_no")
                        .orderByAsc("id")),
                List.of()
        );
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            applyGuestExportColumnWidths(sheet);
            CellStyle style = guestExportCellStyle(workbook);
            String[] headers = {"序号", "客人姓名", "组号", "证件号", "性别", "出生年月", "客户类型", "年龄", "联系电话", "单人备注", "领队", "组备注"};
            writeRow(sheet, 0, style, headers);
            int rowIndex = 1;
            for (SalesBookingOrderGuestEntity guest : guests) {
                writeGuestRow(sheet, rowIndex, style, guest);
                rowIndex += 1;
            }
            writeMergedInfoRow(sheet, rowIndex, style, "行程", order.getTravelDescription());
            writeMergedInfoRow(sheet, rowIndex + 1, style, "去程", order.getPickupInfo());
            writeMergedInfoRow(sheet, rowIndex + 2, style, "回程", order.getDropoffInfo());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output;
        } catch (IOException exception) {
            throw new BizException("游客名单导出失败");
        }
    }

    /**
     * 生成空白游客名单导入模板。
     *
     * <p>模板字段和导入解析字段保持一致，用户无需先从某个订单导出再清空游客信息。</p>
     */
    public ByteArrayOutputStream guestImportTemplateWorkbook() {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            applyGuestExportColumnWidths(sheet);
            CellStyle style = guestExportCellStyle(workbook);
            String[] headers = {"序号", "客人姓名", "组号", "证件号", "性别", "出生年月", "客户类型", "年龄", "联系电话", "单人备注", "领队", "组备注"};
            writeRow(sheet, 0, style, headers);
            for (int rowIndex = 1; rowIndex <= 5; rowIndex++) {
                writeRow(sheet, rowIndex, style, new String[]{"", "", "", "", "", "", "", "", "", "", "", ""});
            }
            writeMergedInfoRow(sheet, 6, style, "行程", "");
            writeMergedInfoRow(sheet, 7, style, "去程", "");
            writeMergedInfoRow(sheet, 8, style, "回程", "");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output;
        } catch (IOException exception) {
            throw new BizException("游客名单模板生成失败");
        }
    }

    /** 返回游客名单导入模板文件名。 */
    public String guestImportTemplateFilename() {
        return "游客名单导入模板.xls";
    }

    /** 生成仿老系统的游客名单文件名。 */
    public String guestExportFilename(Long orderId, Long tenantId) {
        SalesBookingOrderEntity order = requireOrder(orderId, tenantId);
        List<SalesBookingOrderGuestEntity> guests = Objects.requireNonNullElse(
                guestMapper.selectList(baseGuestQuery(tenantId)
                        .eq("order_id", orderId)
                        .orderByAsc("index_no")
                        .orderByAsc("id")),
                List.of()
        );
        String firstGuestName = guests.isEmpty() ? "游客" : cleanFilenamePart(guests.get(0).getGuestName(), "游客");
        String orderNo = cleanFilenamePart(order.getOrderNo(), "订单");
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "名单导出%s_%s_%d人_%s.xls".formatted(orderNo, firstGuestName, guests.size(), timestamp);
    }

    /**
     * 预览导入老系统样式游客名单 Excel。
     *
     * <p>本方法只解析第一张工作表并返回游客草稿，不写入订单表。身份证相关字段由程序校验推导，
     * 客户类型按当前门票年龄规则计算：18 岁以下儿童、60 岁及以上老人，其余成人。</p>
     *
     * @param inputStream Excel 文件输入流
     * @param filename 文件名，用于错误提示
     * @return 导入预览结果
     */
    public SalesBookingGuestImportPreviewResponse importGuestWorkbookPreview(InputStream inputStream, String filename) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BizException("游客名单 Excel 没有工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = findGuestImportHeaderRow(sheet, formatter);
            if (headerRowIndex < 0) {
                throw new BizException("未识别到游客名单表头，请使用包含客人姓名、证件号等列的 Excel");
            }
            Map<String, Integer> columns = guestImportColumns(sheet.getRow(headerRowIndex), formatter);
            List<SalesBookingOrderGuestResponse> guests = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            Set<String> duplicateKeys = new HashSet<>();
            int duplicateCount = 0;
            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isGuestImportIgnoredRow(row, formatter)) {
                    continue;
                }
                SalesBookingOrderGuestEntity guest = parseGuestImportRow(row, formatter, columns, guests.size() + 1);
                if (!StringUtils.hasText(guest.getGuestName()) && !StringUtils.hasText(guest.getCertificateNo())) {
                    continue;
                }
                String key = guestImportDuplicateKey(guest);
                if (StringUtils.hasText(key) && !duplicateKeys.add(key)) {
                    duplicateCount += 1;
                    warnings.add("第%d行与文件内已有游客重复，已跳过".formatted(rowIndex + 1));
                    continue;
                }
                if (Boolean.FALSE.equals(guest.getIdCardValid())) {
                    warnings.add("第%d行%s身份证校验异常：%s".formatted(
                            rowIndex + 1,
                            StringUtils.hasText(guest.getGuestName()) ? "（" + guest.getGuestName() + "）" : "",
                            guest.getIdCardWarning()
                    ));
                }
                guests.add(SalesBookingOrderGuestResponse.fromEntity(guest));
            }
            int validCount = (int) guests.stream().filter(item -> Boolean.TRUE.equals(item.idCardValid())).count();
            int invalidCount = (int) guests.stream().filter(item -> Boolean.FALSE.equals(item.idCardValid())).count();
            return new SalesBookingGuestImportPreviewResponse(
                    guests,
                    guests.size(),
                    validCount,
                    invalidCount,
                    duplicateCount,
                    List.copyOf(warnings)
            );
        } catch (IOException exception) {
            throw new BizException("游客名单导入失败：" + cleanFilenamePart(filename, "Excel"));
        }
    }

    private void rebuildPriceLines(
            SalesBookingOrderEntity order,
            List<SalesBookingOrderPriceLineRequest> requests,
            Long tenantId,
            String operator
    ) {
        softDeletePriceLines(order.getId(), tenantId, operator);
        List<SalesBookingOrderPriceLineRequest> lines = Objects.requireNonNullElse(requests, List.of());
        for (int index = 0; index < lines.size(); index++) {
            SalesBookingOrderPriceLineRequest request = lines.get(index);
            if (isBlankPriceLine(request)) {
                continue;
            }
            SalesBookingOrderChargeLineEntity entity = new SalesBookingOrderChargeLineEntity();
            entity.setTenantId(tenantId);
            entity.setOrderId(order.getId());
            entity.setTeamId(order.getTeamId());
            entity.setLineKind("base_price");
            entity.setLineType(resolveLineType(request.lineType()));
            entity.setItemName(resolveItemName(request.itemName(), entity.getLineType()));
            entity.setUnitPrice(money(request.unitPrice()));
            entity.setQuantity(money(request.quantity()));
            entity.setAmount(entity.getUnitPrice().multiply(entity.getQuantity()).setScale(2, RoundingMode.HALF_UP));
            entity.setStatus("effective");
            entity.setRegisteredBy(operator);
            entity.setRegisteredAt(OffsetDateTime.now());
            entity.setSortOrder(index + 1);
            entity.setRemark(clean(request.remark()));
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            chargeLineMapper.insert(entity);
        }
    }

    private void rebuildGuests(
            SalesBookingOrderEntity order,
            List<SalesBookingOrderGuestRequest> requests,
            Long tenantId,
            String operator
    ) {
        softDeleteGuests(order.getId(), tenantId, operator);
        List<SalesBookingOrderGuestRequest> guests = Objects.requireNonNullElse(requests, List.of());
        for (int index = 0; index < guests.size(); index++) {
            SalesBookingOrderGuestRequest request = guests.get(index);
            if (!StringUtils.hasText(request.guestName())) {
                continue;
            }
            SalesBookingOrderGuestEntity entity = new SalesBookingOrderGuestEntity();
            entity.setTenantId(tenantId);
            entity.setOrderId(order.getId());
            entity.setTeamId(order.getTeamId());
            entity.setIndexNo(request.indexNo() == null ? index + 1 : request.indexNo());
            entity.setGuestName(clean(request.guestName()));
            entity.setEnglishName(clean(request.englishName()));
            entity.setCertificateNo(clean(request.certificateNo()));
            entity.setPassportNo(clean(request.passportNo()));
            entity.setPhone(clean(request.phone()));
            entity.setGuestType(resolveGuestType(request.guestType()));
            entity.setRoomGroup(clean(request.roomGroup()));
            entity.setRoomRemark(clean(request.roomRemark()));
            entity.setLeaderFlag(Boolean.TRUE.equals(request.leaderFlag()));
            entity.setRemark(clean(request.remark()));
            applyIdentityFields(entity, request);
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            guestMapper.insert(entity);
        }
    }

    private void applyIdentityFields(SalesBookingOrderGuestEntity entity, SalesBookingOrderGuestRequest request) {
        if (StringUtils.hasText(request.certificateNo())) {
            IdCardValidationResult result = idCardValidator.validate(request.certificateNo());
            entity.setIdCardValid(result.valid());
            entity.setIdCardWarning(String.join("；", result.warnings()));
            entity.setBirthDate(request.birthDate() == null && result.birthDate() != null
                    ? LocalDate.parse(result.birthDate())
                    : request.birthDate());
            entity.setGender(StringUtils.hasText(request.gender()) ? clean(request.gender()) : result.gender());
            entity.setAge(request.age() == null ? result.age() : request.age());
            return;
        }
        entity.setIdCardValid(null);
        entity.setIdCardWarning(null);
        entity.setBirthDate(request.birthDate());
        entity.setGender(clean(request.gender()));
        entity.setAge(request.age());
    }

    private void refreshTeamSeats(SalesTeamEntity team, Long tenantId) {
        int totalSeats = number(team.getTotalSeats());
        int usedSeats = number(orderMapper.sumGuestCountByTeam(tenantId, team.getId()));
        SalesTeamEntity update = new SalesTeamEntity();
        update.setUsedSeats(usedSeats);
        update.setRemainingSeats(Math.max(totalSeats - usedSeats, 0));
        teamMapper.update(update, baseTeamUpdate(tenantId).eq("id", team.getId()));
    }

    private void softDeletePriceLines(Long orderId, Long tenantId, String operator) {
        SalesBookingOrderChargeLineEntity update = new SalesBookingOrderChargeLineEntity();
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(operator);
        chargeLineMapper.update(update, baseChargeLineUpdate(tenantId)
                .eq("order_id", orderId)
                .eq("line_kind", "base_price"));
    }

    private void softDeleteGuests(Long orderId, Long tenantId, String operator) {
        SalesBookingOrderGuestEntity update = new SalesBookingOrderGuestEntity();
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(operator);
        guestMapper.update(update, baseGuestUpdate(tenantId).eq("order_id", orderId));
    }

    private void applyOrderReceivableDelta(SalesBookingOrderEntity order, BigDecimal delta, Long tenantId) {
        BigDecimal receivable = money(order.getReceivableAmount()).add(money(delta)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal received = money(order.getReceivedAmount());
        SalesBookingOrderEntity update = new SalesBookingOrderEntity();
        update.setReceivableAmount(receivable);
        update.setBalanceAmount(receivable.subtract(received).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        orderMapper.update(update, baseOrderUpdate(tenantId).eq("id", order.getId()));
    }

    private BigDecimal activeFeeChangeTotal(Long orderId, Long tenantId) {
        return Objects.requireNonNullElse(
                chargeLineMapper.selectList(baseChargeLineQuery(tenantId)
                        .eq("order_id", orderId)
                        .eq("line_kind", "adjustment")
                        .eq("status", "approved")),
                List.<SalesBookingOrderChargeLineEntity>of()
        ).stream()
                .map(SalesBookingOrderChargeLineEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private SalesTeamEntity requireTeam(Long teamId, Long tenantId) {
        SalesTeamEntity team = teamMapper.selectOne(baseTeamQuery(tenantId).eq("id", teamId));
        if (team == null) {
            throw new BizException("团队不存在或已删除");
        }
        return team;
    }

    private SalesBookingOrderEntity requireOrder(Long orderId, Long tenantId) {
        SalesBookingOrderEntity order = orderMapper.selectOne(baseOrderQuery(tenantId).eq("id", orderId));
        if (order == null) {
            throw new BizException("收客订单不存在或已删除");
        }
        return order;
    }

    /**
     * 校验当前订单是否允许在收客页继续修改。
     *
     * <p>拼团来源订单只是来源团队的留痕，不再参与来源团队人数、收入和毛利统计；继续修改价格、
     * 游客或费用变更会造成来源团留痕和目标团拼入订单不一致，因此必须到目标团队处理拼入订单。</p>
     */
    private void assertOrderEditable(SalesBookingOrderEntity order) {
        if (order != null && SalesBookingOrderRole.MERGE_SOURCE.value().equals(orderRole(order))) {
            throw new BizException("已拼出的来源订单不能修改，请到目标团队处理拼入订单");
        }
    }

    private EnterpriseExpenseItemEntity requireExtraFeeProject(Long projectId, Long tenantId) {
        if (expenseItemMapper == null) {
            throw new BizException("费用项目服务未初始化");
        }
        EnterpriseExpenseItemEntity project = expenseItemMapper.selectOne(new QueryWrapper<EnterpriseExpenseItemEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("resource_type", "extra_fee")
                .eq("id", projectId));
        if (project == null) {
            throw new BizException("费用项目不存在或已停用");
        }
        return project;
    }

    private void assertTeamCanReceive(SalesTeamEntity team, SalesBookingOrderStatus status, boolean creating) {
        if (SalesTeamStatus.CANCELLED.getValue().equals(team.getStatus())) {
            throw new BizException("团队已取消，不能新增或确认订单");
        }
        if (SalesTeamStatus.STOPPED.getValue().equals(team.getStatus())
                && (creating || status == SalesBookingOrderStatus.CONFIRMED)) {
            throw new BizException("团队已暂停收客，不能新增或确认订单");
        }
    }

    private SalesBookingOrderStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return SalesBookingOrderStatus.PENDING;
        }
        for (SalesBookingOrderStatus status : SalesBookingOrderStatus.values()) {
            if (status.value().equals(value)) {
                return status;
            }
        }
        throw new BizException("订单状态不合法");
    }

    private BigDecimal sumReceivable(List<SalesBookingOrderPriceLineRequest> priceLines) {
        return Objects.requireNonNullElse(priceLines, List.<SalesBookingOrderPriceLineRequest>of()).stream()
                .filter(request -> !isBlankPriceLine(request))
                .map(request -> money(request.unitPrice()).multiply(money(request.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int countByPriceOrGuests(
            List<SalesBookingOrderPriceLineRequest> priceLines,
            List<SalesBookingOrderGuestRequest> guests,
            SalesBookingGuestType type
    ) {
        int priceCount = priceLineQuantity(priceLines, type);
        if (priceCount > 0) {
            return priceCount;
        }
        return countGuests(guests, type);
    }

    private int countGuests(List<SalesBookingOrderGuestRequest> guests, SalesBookingGuestType type) {
        return (int) guests.stream()
                .filter(request -> StringUtils.hasText(request.guestName()))
                .filter(request -> type.value().equals(resolveGuestType(request.guestType())))
                .count();
    }

    private int resolveOrderGuestCount(
            List<SalesBookingOrderPriceLineRequest> priceLines,
            List<SalesBookingOrderGuestRequest> guests
    ) {
        int pricePassengerCount = passengerPriceQuantity(priceLines);
        if (pricePassengerCount > 0) {
            return pricePassengerCount;
        }
        return (int) guests.stream()
                .filter(request -> StringUtils.hasText(request.guestName()))
                .count();
    }

    private int passengerPriceQuantity(List<SalesBookingOrderPriceLineRequest> priceLines) {
        return priceLineQuantity(priceLines, SalesBookingGuestType.ADULT)
                + priceLineQuantity(priceLines, SalesBookingGuestType.CHILD)
                + priceLineQuantity(priceLines, SalesBookingGuestType.CHILD_NO_BED)
                + priceLineQuantity(priceLines, SalesBookingGuestType.SENIOR)
                + priceLineQuantity(priceLines, SalesBookingGuestType.ESCORT);
    }

    private int priceLineQuantity(List<SalesBookingOrderPriceLineRequest> priceLines, SalesBookingGuestType type) {
        return Objects.requireNonNullElse(priceLines, List.<SalesBookingOrderPriceLineRequest>of()).stream()
                .filter(request -> request != null && type.value().equals(request.lineType()))
                .map(SalesBookingOrderPriceLineRequest::quantity)
                .filter(Objects::nonNull)
                .mapToInt(BigDecimal::intValue)
                .sum();
    }

    private boolean isBlankPriceLine(SalesBookingOrderPriceLineRequest request) {
        return request == null
                || (!StringUtils.hasText(request.itemName())
                && request.unitPrice() == null
                && request.quantity() == null
                && !StringUtils.hasText(request.lineType()));
    }

    private String resolveOrderNo(SalesBookingOrderEntity current) {
        if (current != null && StringUtils.hasText(current.getOrderNo())) {
            return current.getOrderNo();
        }
        return "SO-" + LocalDate.now().format(ORDER_DATE_FORMATTER) + "-" + System.currentTimeMillis() % 100_000;
    }

    /**
     * 原始订单摘要多来自拼团、转团或历史迁移。旧前端不传该字段时，修改订单不能把已有来源信息清空。
     */
    private String resolveOriginalOrderInfo(SalesBookingOrderSaveRequest request, SalesBookingOrderEntity current) {
        if (StringUtils.hasText(request.originalOrderInfo())) {
            return clean(request.originalOrderInfo());
        }
        return current == null ? null : current.getOriginalOrderInfo();
    }

    private String resolveGuestType(String value) {
        if (!StringUtils.hasText(value)) {
            return SalesBookingGuestType.ADULT.value();
        }
        return SalesBookingGuestType.valid(value) ? value : SalesBookingGuestType.ADULT.value();
    }

    private String resolveLineType(String value) {
        if (!StringUtils.hasText(value)) {
            return "misc";
        }
        return clean(value);
    }

    private String resolveItemName(String itemName, String lineType) {
        if (StringUtils.hasText(itemName)) {
            return clean(itemName);
        }
        return switch (lineType) {
            case "adult" -> "成人";
            case "child" -> "儿童";
            case "child_no_bed" -> "儿童不占床";
            case "senior" -> "老人";
            case "escort" -> "全陪";
            default -> "其他费用";
        };
    }

    private QueryWrapper<SalesBookingOrderEntity> baseOrderQuery(Long tenantId) {
        return new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesBookingOrderEntity> baseOrderUpdate(Long tenantId) {
        return new UpdateWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesBookingOrderChargeLineEntity> baseChargeLineQuery(Long tenantId) {
        return new QueryWrapper<SalesBookingOrderChargeLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesBookingOrderGuestEntity> baseGuestQuery(Long tenantId) {
        return new QueryWrapper<SalesBookingOrderGuestEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesBookingOrderGuestEntity> baseGuestUpdate(Long tenantId) {
        return new UpdateWrapper<SalesBookingOrderGuestEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesBookingOrderChargeLineEntity> baseChargeLineUpdate(Long tenantId) {
        return new UpdateWrapper<SalesBookingOrderChargeLineEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesTeamEntity> baseTeamQuery(Long tenantId) {
        return new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesTeamEntity> baseTeamUpdate(Long tenantId) {
        return new UpdateWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal signedFeeChangeAmount(String changeType, BigDecimal amount) {
        BigDecimal normalized = money(amount).abs();
        return "decrease".equals(changeType) ? normalized.negate() : normalized;
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String joinCancelRemark(String oldRemark, String operator) {
        String cancelText = "作废人：" + (StringUtils.hasText(operator) ? operator : "system");
        if (!StringUtils.hasText(oldRemark)) {
            return cancelText;
        }
        return oldRemark + "；" + cancelText;
    }

    private void applyGuestExportColumnWidths(Sheet sheet) {
        int[] widths = {2252, 2218, 2252, 6246, 1297, 2525, 2218, 2491, 4573, 2218, 1297, 1774};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private CellStyle guestExportCellStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 9);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void writeRow(Sheet sheet, int rowIndex, CellStyle style, String[] values) {
        Row row = sheet.createRow(rowIndex);
        row.setHeight((short) 400);
        for (int col = 0; col < values.length; col++) {
            Cell cell = row.createCell(col);
            cell.setCellStyle(style);
            cell.setCellValue(values[col] == null ? "" : values[col]);
        }
    }

    private void writeGuestRow(Sheet sheet, int rowIndex, CellStyle style, SalesBookingOrderGuestEntity guest) {
        writeRow(sheet, rowIndex, style, new String[]{
                textPrefix(guest.getIndexNo()),
                nullToBlank(guest.getGuestName()),
                textPrefix(guest.getRoomGroup()),
                textPrefix(guest.getCertificateNo()),
                nullToBlank(guest.getGender()),
                guest.getBirthDate() == null ? "" : guest.getBirthDate().toString(),
                SalesBookingGuestType.labelOf(guest.getGuestType()),
                textPrefix(guest.getAge()),
                textPrefix(guest.getPhone()),
                nullToBlank(guest.getRemark()),
                Boolean.TRUE.equals(guest.getLeaderFlag()) ? "是" : "否",
                nullToBlank(guest.getRoomRemark())
        });
    }

    private void writeMergedInfoRow(Sheet sheet, int rowIndex, CellStyle style, String title, String value) {
        writeRow(sheet, rowIndex, style, new String[]{title, nullToBlank(value), "", "", "", "", "", "", "", "", "", ""});
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 11));
        for (int col = 2; col <= 11; col++) {
            sheet.getRow(rowIndex).getCell(col).setCellStyle(style);
        }
    }

    private int findGuestImportHeaderRow(Sheet sheet, DataFormatter formatter) {
        int maxRow = Math.min(sheet.getLastRowNum(), 20);
        for (int rowIndex = 0; rowIndex <= maxRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> columns = guestImportColumns(row, formatter);
            if (columns.containsKey("guestName") && columns.containsKey("certificateNo")) {
                return rowIndex;
            }
        }
        return -1;
    }

    private Map<String, Integer> guestImportColumns(Row row, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        if (row == null) {
            return columns;
        }
        for (int col = 0; col < row.getLastCellNum(); col++) {
            String header = normalizeGuestImportHeader(cellText(row, col, formatter));
            switch (header) {
                case "序号" -> columns.putIfAbsent("indexNo", col);
                case "客人姓名", "姓名", "游客姓名", "客户姓名" -> columns.putIfAbsent("guestName", col);
                case "组号", "房间组号", "房号", "分房" -> columns.putIfAbsent("roomGroup", col);
                case "证件号", "身份证号", "身份证", "证件号码" -> columns.putIfAbsent("certificateNo", col);
                case "性别" -> columns.putIfAbsent("gender", col);
                case "出生年月", "出生日期", "生日" -> columns.putIfAbsent("birthDate", col);
                case "客户类型", "游客类型", "客人类型", "类型" -> columns.putIfAbsent("guestType", col);
                case "年龄" -> columns.putIfAbsent("age", col);
                case "联系电话", "电话", "手机号", "手机号码" -> columns.putIfAbsent("phone", col);
                case "单人备注", "个人备注", "备注" -> columns.putIfAbsent("remark", col);
                case "领队" -> columns.putIfAbsent("leaderFlag", col);
                case "组备注", "分房备注", "房间备注" -> columns.putIfAbsent("roomRemark", col);
                default -> {
                }
            }
        }
        return columns;
    }

    private SalesBookingOrderGuestEntity parseGuestImportRow(
            Row row,
            DataFormatter formatter,
            Map<String, Integer> columns,
            int defaultIndexNo
    ) {
        SalesBookingOrderGuestEntity guest = new SalesBookingOrderGuestEntity();
        guest.setIndexNo(parseInteger(columnText(row, columns, "indexNo", formatter), defaultIndexNo));
        guest.setGuestName(clean(columnText(row, columns, "guestName", formatter)));
        guest.setRoomGroup(clean(columnText(row, columns, "roomGroup", formatter)));
        guest.setCertificateNo(clean(stripTextPrefix(columnText(row, columns, "certificateNo", formatter)).toUpperCase()));
        guest.setPhone(clean(stripTextPrefix(columnText(row, columns, "phone", formatter))));
        guest.setLeaderFlag(parseBoolean(columnText(row, columns, "leaderFlag", formatter)));
        guest.setRemark(clean(columnText(row, columns, "remark", formatter)));
        guest.setRoomRemark(clean(columnText(row, columns, "roomRemark", formatter)));
        String inputGender = clean(columnText(row, columns, "gender", formatter));
        LocalDate inputBirthDate = parseLocalDate(columnText(row, columns, "birthDate", formatter));
        Integer inputAge = parseInteger(columnText(row, columns, "age", formatter), null);
        applyImportedIdentityFields(guest, inputGender, inputBirthDate, inputAge);
        guest.setGuestType(resolveImportedGuestType(columnText(row, columns, "guestType", formatter), guest.getAge()));
        return guest;
    }

    private void applyImportedIdentityFields(
            SalesBookingOrderGuestEntity guest,
            String inputGender,
            LocalDate inputBirthDate,
            Integer inputAge
    ) {
        if (StringUtils.hasText(guest.getCertificateNo())) {
            IdCardValidationResult result = idCardValidator.validate(guest.getCertificateNo());
            guest.setIdCardValid(result.valid());
            guest.setIdCardWarning(String.join("；", result.warnings()));
            guest.setBirthDate(result.birthDate() == null ? inputBirthDate : LocalDate.parse(result.birthDate()));
            guest.setGender(StringUtils.hasText(inputGender) ? inputGender : result.gender());
            guest.setAge(inputAge == null ? result.age() : inputAge);
            return;
        }
        guest.setIdCardValid(null);
        guest.setIdCardWarning(null);
        guest.setBirthDate(inputBirthDate);
        guest.setGender(inputGender);
        guest.setAge(inputAge);
    }

    private boolean isGuestImportIgnoredRow(Row row, DataFormatter formatter) {
        String first = cellText(row, 0, formatter);
        if (List.of("行程", "去程", "回程", "来程", "返程").contains(first.trim())) {
            return true;
        }
        boolean allBlank = true;
        for (int col = 0; col < row.getLastCellNum(); col++) {
            if (StringUtils.hasText(cellText(row, col, formatter))) {
                allBlank = false;
                break;
            }
        }
        return allBlank;
    }

    private String columnText(Row row, Map<String, Integer> columns, String key, DataFormatter formatter) {
        Integer columnIndex = columns.get(key);
        return columnIndex == null ? "" : cellText(row, columnIndex, formatter);
    }

    private String cellText(Row row, int columnIndex, DataFormatter formatter) {
        if (row == null || columnIndex < 0) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        return stripTextPrefix(formatter.formatCellValue(cell)).trim();
    }

    private String stripTextPrefix(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceFirst("^\\t+", "").trim();
    }

    private String normalizeGuestImportHeader(String value) {
        return stripTextPrefix(value)
                .replaceAll("\\s+", "")
                .replace("：", "")
                .replace(":", "");
    }

    private Integer parseInteger(String value, Integer fallback) {
        String cleaned = stripTextPrefix(value);
        if (!StringUtils.hasText(cleaned)) {
            return fallback;
        }
        try {
            return new BigDecimal(cleaned.replaceAll("[^0-9.]", "")).intValue();
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean parseBoolean(String value) {
        String cleaned = stripTextPrefix(value);
        return List.of("是", "Y", "YES", "TRUE", "1", "领队").contains(cleaned.toUpperCase());
    }

    private LocalDate parseLocalDate(String value) {
        String cleaned = stripTextPrefix(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        String normalized = cleaned.replace(".", "-").replace("/", "-");
        try {
            if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}.*")) {
                String[] parts = normalized.substring(0, 10).split("-");
                return LocalDate.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                );
            }
        } catch (RuntimeException exception) {
            return null;
        }
        return null;
    }

    private String resolveImportedGuestType(String inputType, Integer age) {
        if (age != null) {
            if (age < 18) {
                return SalesBookingGuestType.CHILD.value();
            }
            if (age >= 60) {
                return SalesBookingGuestType.SENIOR.value();
            }
        }
        String normalized = stripTextPrefix(inputType);
        if (normalized.contains("不占")) {
            return SalesBookingGuestType.CHILD_NO_BED.value();
        }
        if (normalized.contains("全陪")) {
            return SalesBookingGuestType.ESCORT.value();
        }
        if (normalized.contains("儿童") || normalized.contains("小孩")) {
            return SalesBookingGuestType.CHILD.value();
        }
        if (normalized.contains("老人") || normalized.contains("老年")) {
            return SalesBookingGuestType.SENIOR.value();
        }
        return SalesBookingGuestType.ADULT.value();
    }

    private String guestImportDuplicateKey(SalesBookingOrderGuestEntity guest) {
        if (StringUtils.hasText(guest.getCertificateNo())) {
            return "cert:" + guest.getCertificateNo().trim().toUpperCase();
        }
        if (StringUtils.hasText(guest.getGuestName()) && StringUtils.hasText(guest.getPhone())) {
            return "name-phone:" + guest.getGuestName().trim() + ":" + guest.getPhone().trim();
        }
        return "";
    }

    private String textPrefix(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        return StringUtils.hasText(text) ? "\t" + text : "";
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String cleanFilenamePart(String value, String fallback) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            return fallback;
        }
        return cleaned.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    /**
     * 将订单列表转换为团队操作页订单行。
     *
     * <p>该方法集中处理金额和状态展示，避免团队服务重复了解订单状态细节。</p>
     */
    public List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRow> toOperationRows(
            List<SalesBookingOrderEntity> orders
    ) {
        if (CollectionUtils.isEmpty(orders)) {
            return List.of();
        }
        Map<Long, List<SalesBookingOrderChargeLineEntity>> priceLinesByOrderId = loadBasePriceLinesByOrderId(orders);
        Map<Long, List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRelationInfo>> mergeInfosBySourceOrderId =
                loadMergeInfosBySourceOrderId(orders);
        Map<Long, List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRelationInfo>> sourceInfosByChildOrderId =
                loadSourceInfosByChildOrderId(orders);
        List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRow> rows = new ArrayList<>();
        for (SalesBookingOrderEntity order : orders) {
            String orderRole = orderRole(order);
            rows.add(new com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRow(
                    order.getId(),
                    order.getOrderNo(),
                    orderRole,
                    orderRoleLabel(orderRole),
                    joinNonBlank(order.getCustomerName(), order.getCustomerTeamNo()),
                    order.getPickupInfo(),
                    order.getDropoffInfo(),
                    order.getOriginalOrderInfo(),
                    mergeInfosBySourceOrderId.getOrDefault(order.getId(), List.of()),
                    sourceInfosByChildOrderId.getOrDefault(order.getId(), List.of()),
                    order.getPickupRemark(),
                    joinNonBlank(order.getSourceProvince(), order.getSourceCity(), order.getSourceDistrict()),
                    firstLeaderOrCustomer(order),
                    order.getGuestCount(),
                    operationGuestCountText(order),
                    formatPriceDetail(priceLinesByOrderId.get(order.getId())),
                    moneyText(order.getReceivableAmount()),
                    moneyText(order.getReceivedAmount()),
                    moneyText(order.getBalanceAmount()),
                    order.getFeeRemark(),
                    order.getOrderRemark(),
                    joinNonBlank(order.getBookedAt() == null ? null : order.getBookedAt().toLocalDate().toString(), order.getBookedBy()),
                    SalesBookingOrderStatus.labelOf(order.getStatus())
            ));
        }
        return rows;
    }

    private Map<Long, List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRelationInfo>> loadSourceInfosByChildOrderId(
            List<SalesBookingOrderEntity> orders
    ) {
        if (transferLogMapper == null) {
            return Map.of();
        }
        Map<Long, List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRelationInfo>> result = new HashMap<>();
        Map<Long, List<SalesBookingOrderEntity>> ordersByTenantId = orders.stream()
                .filter(order -> SalesBookingOrderRole.MERGE_CHILD.value().equals(orderRole(order)))
                .filter(order -> order.getTenantId() != null && order.getId() != null)
                .collect(Collectors.groupingBy(SalesBookingOrderEntity::getTenantId));
        for (Map.Entry<Long, List<SalesBookingOrderEntity>> entry : ordersByTenantId.entrySet()) {
            Long tenantId = entry.getKey();
            List<Long> childOrderIds = entry.getValue().stream()
                    .map(SalesBookingOrderEntity::getId)
                    .toList();
            if (childOrderIds.isEmpty()) {
                continue;
            }
            List<SalesOrderTransferLogEntity> logs = Objects.requireNonNullElse(
                    transferLogMapper.selectCompletedMergeByChildOrders(tenantId, childOrderIds),
                    List.of()
            );
            Map<Long, SalesTeamEntity> teamsById = loadTeamsById(tenantId, logs.stream()
                    .map(SalesOrderTransferLogEntity::getSourceTeamId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());
            for (SalesOrderTransferLogEntity log : logs) {
                result.computeIfAbsent(log.getChildOrderId(), key -> new ArrayList<>())
                        .add(new com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRelationInfo(
                                log.getSourceOrderId(),
                                log.getSourceTeamId(),
                                sourceOrderSummary(teamsById.get(log.getSourceTeamId()), log)
                        ));
            }
        }
        return result;
    }

    private Map<Long, List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRelationInfo>> loadMergeInfosBySourceOrderId(
            List<SalesBookingOrderEntity> orders
    ) {
        if (transferLogMapper == null) {
            return Map.of();
        }
        Map<Long, List<com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRelationInfo>> result = new HashMap<>();
        Map<Long, List<SalesBookingOrderEntity>> ordersByTenantId = orders.stream()
                .filter(order -> SalesBookingOrderRole.MERGE_SOURCE.value().equals(orderRole(order)))
                .filter(order -> order.getTenantId() != null && order.getId() != null)
                .collect(Collectors.groupingBy(SalesBookingOrderEntity::getTenantId));
        for (Map.Entry<Long, List<SalesBookingOrderEntity>> entry : ordersByTenantId.entrySet()) {
            Long tenantId = entry.getKey();
            List<Long> sourceOrderIds = entry.getValue().stream()
                    .map(SalesBookingOrderEntity::getId)
                    .toList();
            if (sourceOrderIds.isEmpty()) {
                continue;
            }
            List<SalesOrderTransferLogEntity> logs = Objects.requireNonNullElse(
                    transferLogMapper.selectCompletedMergeBySourceOrders(tenantId, sourceOrderIds),
                    List.of()
            );
            Map<Long, SalesTeamEntity> teamsById = loadTeamsById(tenantId, logs.stream()
                    .map(SalesOrderTransferLogEntity::getTargetTeamId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());
            for (SalesOrderTransferLogEntity log : logs) {
                result.computeIfAbsent(log.getSourceOrderId(), key -> new ArrayList<>())
                        .add(new com.mtravel.platform.sales.team.dto.SalesTeamOperationResponse.OrderRelationInfo(
                                log.getChildOrderId(),
                                log.getTargetTeamId(),
                                mergeTargetSummary(teamsById.get(log.getTargetTeamId()), log)
                        ));
            }
        }
        return result;
    }

    private Map<Long, SalesTeamEntity> loadTeamsById(Long tenantId, List<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return Objects.requireNonNullElse(teamMapper.selectList(baseTeamQuery(tenantId).in("id", teamIds)), List.<SalesTeamEntity>of())
                .stream()
                .filter(team -> team.getId() != null)
                .collect(Collectors.toMap(SalesTeamEntity::getId, team -> team, (left, right) -> left));
    }

    private String mergeTargetSummary(SalesTeamEntity team, SalesOrderTransferLogEntity log) {
        if (team == null) {
            return "目标团队：" + log.getTargetTeamId();
        }
        return joinNonBlank(
                team.getDepartureDate() == null ? null : team.getDepartureDate().toString(),
                team.getTeamNo()
        );
    }

    private String sourceOrderSummary(SalesTeamEntity team, SalesOrderTransferLogEntity log) {
        if (team == null) {
            return "来源团队：" + log.getSourceTeamId();
        }
        return joinNonBlank(
                team.getDepartureDate() == null ? null : team.getDepartureDate().toString(),
                team.getTeamNo()
        );
    }

    private String orderRole(SalesBookingOrderEntity order) {
        return StringUtils.hasText(order.getOrderRole()) ? order.getOrderRole() : SalesBookingOrderRole.NORMAL.value();
    }

    private String orderRoleLabel(String orderRole) {
        return java.util.Arrays.stream(SalesBookingOrderRole.values())
                .filter(item -> item.value().equals(orderRole))
                .map(SalesBookingOrderRole::label)
                .findFirst()
                .orElse(orderRole);
    }

    private String operationGuestCountText(SalesBookingOrderEntity order) {
        int total = number(order.getGuestCount());
        int adult = number(order.getAdultCount());
        int child = number(order.getChildCount()) + number(order.getChildNoBedCount());
        int senior = number(order.getSeniorCount());
        return total + "人[" + adult + "/" + child + "/" + senior + "]";
    }

    private Map<Long, List<SalesBookingOrderChargeLineEntity>> loadBasePriceLinesByOrderId(
            List<SalesBookingOrderEntity> orders
    ) {
        Map<Long, List<SalesBookingOrderChargeLineEntity>> priceLinesByOrderId = new HashMap<>();
        Map<Long, List<SalesBookingOrderEntity>> ordersByTenantId = orders.stream()
                .filter(order -> order.getTenantId() != null && order.getId() != null)
                .collect(Collectors.groupingBy(SalesBookingOrderEntity::getTenantId));
        for (Map.Entry<Long, List<SalesBookingOrderEntity>> entry : ordersByTenantId.entrySet()) {
            List<Long> orderIds = entry.getValue().stream()
                    .map(SalesBookingOrderEntity::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (orderIds.isEmpty()) {
                continue;
            }
            List<SalesBookingOrderChargeLineEntity> priceLines = Objects.requireNonNullElse(
                    chargeLineMapper.selectList(baseChargeLineQuery(entry.getKey())
                            .in("order_id", orderIds)
                            .eq("line_kind", "base_price")
                            .orderByAsc("order_id")
                            .orderByAsc("sort_order")
                            .orderByAsc("id")),
                    List.of()
            );
            for (SalesBookingOrderChargeLineEntity line : priceLines) {
                priceLinesByOrderId.computeIfAbsent(line.getOrderId(), key -> new ArrayList<>()).add(line);
            }
        }
        return priceLinesByOrderId;
    }

    private String formatPriceDetail(List<SalesBookingOrderChargeLineEntity> priceLines) {
        if (CollectionUtils.isEmpty(priceLines)) {
            return "";
        }
        return priceLines.stream()
                .map(this::formatPriceLine)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    private String formatPriceLine(SalesBookingOrderChargeLineEntity line) {
        if (line == null) {
            return "";
        }
        String itemName = StringUtils.hasText(line.getItemName()) ? line.getItemName() : "价格项";
        return itemName + "：" + compactMoneyText(line.getUnitPrice())
                + " * " + compactMoneyText(line.getQuantity())
                + " = " + compactMoneyText(line.getAmount());
    }

    private String firstLeaderOrCustomer(SalesBookingOrderEntity order) {
        if (StringUtils.hasText(order.getContactName())) {
            return order.getContactName();
        }
        return order.getCustomerName();
    }

    private String compactMoneyText(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String moneyText(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String joinNonBlank(String... values) {
        return java.util.Arrays.stream(values)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" / "));
    }
}
