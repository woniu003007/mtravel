package com.mtravel.platform.sales.team.grossprofit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderRole;
import com.mtravel.platform.sales.booking.order.enums.SalesBookingOrderStatus;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.grossprofit.dto.SalesTeamGrossProfitPreviewResponse;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 团队预算毛利表服务。
 *
 * <p>服务统一生成预览数据、Word 和 PDF，确保页面顶部预算利润、预览页和下载文件使用同一套旧系统公式。</p>
 */
@Service
public class SalesTeamGrossProfitService {

    private static final List<String> REGULAR_COST_TYPES = List.of(
            "traffic", "hotel", "vehicle", "scenic", "meal", "other", "ground_agent", "extra_fee"
    );
    private static final Map<String, String> COST_TYPE_LABELS = Map.of(
            "traffic", "大交通",
            "hotel", "酒店",
            "vehicle", "车队",
            "scenic", "景区",
            "meal", "餐厅",
            "other", "其它",
            "ground_agent", "地接",
            "extra_fee", "附加"
    );

    private final SalesTeamMapper teamMapper;
    private final SalesProductMapper productMapper;
    private final SalesBookingOrderMapper orderMapper;
    private final DispatchTeamArrangementMapper arrangementMapper;
    private final DispatchTeamGuideMapper guideMapper;

    /**
     * 构造团队预算毛利表服务。
     */
    public SalesTeamGrossProfitService(
            SalesTeamMapper teamMapper,
            SalesProductMapper productMapper,
            SalesBookingOrderMapper orderMapper,
            DispatchTeamArrangementMapper arrangementMapper,
            DispatchTeamGuideMapper guideMapper
    ) {
        this.teamMapper = teamMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.arrangementMapper = arrangementMapper;
        this.guideMapper = guideMapper;
    }

    /**
     * 查询团队预算毛利表预览数据。
     *
     * @param teamId 团队 ID
     * @param tenantId 当前租户 ID
     * @return 预算毛利表结构化数据
     */
    public SalesTeamGrossProfitPreviewResponse preview(Long teamId, Long tenantId) {
        SalesTeamEntity team = requireTeam(teamId, tenantId);
        SalesProductEntity product = product(team.getProductId(), tenantId);
        List<SalesBookingOrderEntity> orders = effectiveOrders(teamId, tenantId);
        List<DispatchTeamArrangementEntity> arrangements = arrangements(teamId, tenantId);
        List<DispatchTeamGuideEntity> guides = guides(teamId, tenantId);

        List<SalesTeamGrossProfitPreviewResponse.IncomeRow> incomeRows = orders.stream()
                .map(this::incomeRow)
                .toList();
        List<SalesTeamGrossProfitPreviewResponse.CostRow> costRows = arrangements.stream()
                .filter(item -> REGULAR_COST_TYPES.contains(item.getArrangementType()))
                .map(this::costRow)
                .toList();
        List<SalesTeamGrossProfitPreviewResponse.OptionalRow> optionalRows = arrangements.stream()
                .filter(item -> Objects.equals(item.getArrangementType(), "optional"))
                .map(this::optionalRow)
                .toList();
        List<SalesTeamGrossProfitPreviewResponse.ShoppingRow> shoppingRows = arrangements.stream()
                .filter(item -> Objects.equals(item.getArrangementType(), "shopping"))
                .map(this::shoppingRow)
                .toList();

        BigDecimal orderIncome = incomeRows.stream()
                .map(SalesTeamGrossProfitPreviewResponse.IncomeRow::receivableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal regularCost = costRows.stream()
                .map(SalesTeamGrossProfitPreviewResponse.CostRow::payableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal optionalProfit = optionalRows.stream()
                .map(SalesTeamGrossProfitPreviewResponse.OptionalRow::companyProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shoppingProfit = shoppingRows.stream()
                .map(SalesTeamGrossProfitPreviewResponse.ShoppingRow::companyProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal guideFee = guides.stream()
                .map(DispatchTeamGuideEntity::getGuideFee)
                .map(SalesTeamGrossProfitService::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossProfit = orderIncome
                .add(optionalProfit)
                .add(shoppingProfit)
                .subtract(regularCost)
                .subtract(guideFee)
                .setScale(2, RoundingMode.HALF_UP);

        SalesTeamGrossProfitPreviewResponse.Summary summary =
                new SalesTeamGrossProfitPreviewResponse.Summary(
                        orderIncome.setScale(2, RoundingMode.HALF_UP),
                        shoppingProfit.setScale(2, RoundingMode.HALF_UP),
                        optionalProfit.setScale(2, RoundingMode.HALF_UP),
                        regularCost.setScale(2, RoundingMode.HALF_UP),
                        guideFee.setScale(2, RoundingMode.HALF_UP),
                        grossProfit
                );

        return new SalesTeamGrossProfitPreviewResponse(
                new SalesTeamGrossProfitPreviewResponse.TeamInfo(
                        team.getId(),
                        text(product == null ? null : product.getProductName(), team.getTeamNo()),
                        team.getTeamNo(),
                        team.getDepartureDate(),
                        product == null ? null : product.getTravelDays(),
                        orders.stream().map(SalesBookingOrderEntity::getGuestCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum(),
                        guideSummary(guides),
                        text(team.getOperatorEmployeeName(), "--")
                ),
                incomeRows,
                costRows,
                optionalRows,
                shoppingRows,
                summary,
                salespersonRows(incomeRows, summary)
        );
    }

    /**
     * 导出团队预算毛利表文件。
     *
     * @param teamId 团队 ID
     * @param format docx 或 pdf
     * @param tenantId 当前租户 ID
     * @return 文件内容和响应类型
     */
    public ExportResult export(Long teamId, String format, Long tenantId) {
        SalesTeamGrossProfitPreviewResponse data = preview(teamId, tenantId);
        String normalized = StringUtils.hasText(format) ? format.trim().toLowerCase() : "docx";
        return switch (normalized) {
            case "docx", "word" -> exportDocx(data);
            case "pdf" -> exportPdf(data);
            default -> throw new BizException("不支持的毛利表导出格式");
        };
    }

    /** 导出结果。 */
    public record ExportResult(String filename, String contentType, ByteArrayOutputStream content) {
    }

    private SalesTeamGrossProfitPreviewResponse.IncomeRow incomeRow(SalesBookingOrderEntity order) {
        return new SalesTeamGrossProfitPreviewResponse.IncomeRow(
                text(order.getCustomerName(), "--"),
                text(order.getSalespersonEmployeeName(), "--"),
                number(order.getGuestCount()),
                text(order.getFeeRemark(), ""),
                money(order.getReceivableAmount()),
                money(order.getReceivedAmount()),
                text(order.getBookingOperatorEmployeeName(), "--")
        );
    }

    private SalesTeamGrossProfitPreviewResponse.CostRow costRow(DispatchTeamArrangementEntity item) {
        BigDecimal payable = money(firstPositive(item.getTotalAmount(), item.getCostAmount()));
        return new SalesTeamGrossProfitPreviewResponse.CostRow(
                COST_TYPE_LABELS.getOrDefault(item.getArrangementType(), item.getArrangementType()),
                text(item.getSupplierName(), text(item.getResourceName(), "--")),
                text(item.getArrangementContent(), text(item.getItemName(), "--")),
                payable,
                money(item.getCashAmount()),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                text(item.getCreatedBy(), "--")
        );
    }

    private SalesTeamGrossProfitPreviewResponse.OptionalRow optionalRow(DispatchTeamArrangementEntity item) {
        BigDecimal sales = money(item.getSaleAmount());
        BigDecimal cost = money(item.getCostAmount());
        BigDecimal guideCommission = money(item.getGuideCommissionAmount());
        return new SalesTeamGrossProfitPreviewResponse.OptionalRow(
                text(item.getResourceName(), text(item.getItemName(), "--")),
                money(item.getPeopleCount()),
                sales,
                cost,
                guideCommission,
                sales.subtract(cost).subtract(guideCommission).setScale(2, RoundingMode.HALF_UP),
                text(item.getCreatedBy(), "--")
        );
    }

    private SalesTeamGrossProfitPreviewResponse.ShoppingRow shoppingRow(DispatchTeamArrangementEntity item) {
        BigDecimal headFee = money(item.getHeadFeeAmount());
        BigDecimal companyRebate = money(item.getCompanyRebateAmount());
        BigDecimal guideCommission = money(item.getGuideCommissionAmount());
        return new SalesTeamGrossProfitPreviewResponse.ShoppingRow(
                text(item.getResourceName(), text(item.getItemName(), "--")),
                money(item.getPeopleCount()),
                headFee,
                money(item.getConsumptionAmount()),
                companyRebate,
                guideCommission,
                headFee.add(companyRebate).subtract(guideCommission).setScale(2, RoundingMode.HALF_UP),
                text(item.getCreatedBy(), "--")
        );
    }

    private List<SalesTeamGrossProfitPreviewResponse.SalespersonSummary> salespersonRows(
            List<SalesTeamGrossProfitPreviewResponse.IncomeRow> incomeRows,
            SalesTeamGrossProfitPreviewResponse.Summary summary
    ) {
        Map<String, BigDecimal[]> amounts = new LinkedHashMap<>();
        for (SalesTeamGrossProfitPreviewResponse.IncomeRow row : incomeRows) {
            BigDecimal[] values = amounts.computeIfAbsent(row.salespersonName(), ignored -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            values[0] = values[0].add(row.receivableAmount());
            values[1] = values[1].add(row.receivedAmount());
        }
        BigDecimal totalIncome = summary.orderIncome();
        List<SalesTeamGrossProfitPreviewResponse.SalespersonSummary> rows = new ArrayList<>();
        amounts.forEach((name, values) -> {
            BigDecimal rate = totalIncome.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : values[0].divide(totalIncome, 8, RoundingMode.HALF_UP);
            BigDecimal profit = summary.grossProfit().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profitRate = values[0].compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : profit.multiply(BigDecimal.valueOf(100)).divide(values[0], 2, RoundingMode.HALF_UP);
            rows.add(new SalesTeamGrossProfitPreviewResponse.SalespersonSummary(
                    name,
                    values[0].setScale(2, RoundingMode.HALF_UP),
                    values[1].setScale(2, RoundingMode.HALF_UP),
                    profit,
                    profitRate
            ));
        });
        return rows;
    }

    private ExportResult exportDocx(SalesTeamGrossProfitPreviewResponse data) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            title(document, "团队毛利表(预算)");
            paragraph(document, data.team().productName());
            keyValueTable(document, List.of(
                    List.of("团号", data.team().teamNo(), "出团日期", dateText(data.team().departureDate())),
                    List.of("旅游天数", data.team().travelDays() + " 天", "接待人数", data.team().guestCount() + "人"),
                    List.of("导游", data.team().guideSummary(), "操作计调", data.team().operatorName())
            ));
            rowsTable(document, "收入", List.of("客户单位", "业务员", "人数", "应收明细", "应收金额", "已收金额", "收客计调"),
                    data.incomeRows().stream()
                            .map(row -> List.of(row.customerName(), row.salespersonName(), row.guestCount() + "人",
                                    row.receivableDetail(), moneyText(row.receivableAmount()), moneyText(row.receivedAmount()), row.bookingOperatorName()))
                            .toList());
            rowsTable(document, "支出", List.of("类别", "供应商", "费用说明", "应付金额", "现付", "挂账已付", "审核人"),
                    data.costRows().stream()
                            .map(row -> List.of(row.category(), row.supplierName(), row.costDescription(), moneyText(row.payableAmount()),
                                    moneyText(row.cashAmount()), moneyText(row.paidCreditAmount()), row.auditorName()))
                            .toList());
            rowsTable(document, "自费", List.of("景区/项目", "人数", "销售额", "成本", "导游提成", "公司利润", "审核人"),
                    data.optionalRows().stream()
                            .map(row -> List.of(row.projectName(), moneyTextNoSymbol(row.guestCount()) + "人", moneyText(row.salesAmount()),
                                    moneyText(row.costAmount()), moneyText(row.guideCommissionAmount()), moneyText(row.companyProfit()), row.auditorName()))
                            .toList());
            rowsTable(document, "购物", List.of("购物店", "进店人数", "人头费", "销售额", "公司返佣", "导游返佣", "公司利润", "审核人"),
                    data.shoppingRows().stream()
                            .map(row -> List.of(row.shopName(), moneyTextNoSymbol(row.entryCount()) + "人", moneyText(row.headFeeAmount()),
                                    moneyText(row.consumptionAmount()), moneyText(row.companyRebateAmount()), moneyText(row.guideCommissionAmount()),
                                    moneyText(row.companyProfit()), row.auditorName()))
                            .toList());
            rowsTable(document, "毛利", List.of("订单收入", "购物反佣", "加点利润", "成本支出", "导服费", "合计毛利"),
                    List.of(List.of(moneyText(data.summary().orderIncome()), moneyText(data.summary().shoppingProfit()),
                            moneyText(data.summary().optionalProfit()), moneyText(data.summary().regularCost()),
                            moneyText(data.summary().guideFee()), moneyText(data.summary().grossProfit()))));
            document.write(output);
            return new ExportResult(filename(data, ".docx"),
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    output);
        } catch (IOException ex) {
            throw new BizException("生成团队毛利表 Word 失败");
        }
    }

    private ExportResult exportPdf(SalesTeamGrossProfitPreviewResponse data) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document);
            writer.line("团队毛利表(预算)", 18);
            writer.line(data.team().productName(), 11);
            writer.line("团号：" + data.team().teamNo() + "    出团日期：" + dateText(data.team().departureDate())
                    + "    旅游天数：" + data.team().travelDays() + " 天    接待人数：" + data.team().guestCount() + "人", 10);
            writer.line("导游：" + data.team().guideSummary() + "    操作计调：" + data.team().operatorName(), 10);
            writer.section("收入");
            for (SalesTeamGrossProfitPreviewResponse.IncomeRow row : data.incomeRows()) {
                writer.line(row.customerName() + "  " + row.guestCount() + "人  应收：" + moneyText(row.receivableAmount())
                        + "  已收：" + moneyText(row.receivedAmount()), 9);
            }
            writer.section("支出");
            for (SalesTeamGrossProfitPreviewResponse.CostRow row : data.costRows()) {
                writer.line(row.category() + "  " + row.supplierName() + "  " + row.costDescription()
                        + "  应付：" + moneyText(row.payableAmount()), 9);
            }
            writer.section("自费");
            for (SalesTeamGrossProfitPreviewResponse.OptionalRow row : data.optionalRows()) {
                writer.line(row.projectName() + "  公司利润：" + moneyText(row.companyProfit()), 9);
            }
            writer.section("购物");
            for (SalesTeamGrossProfitPreviewResponse.ShoppingRow row : data.shoppingRows()) {
                writer.line(row.shopName() + "  公司利润：" + moneyText(row.companyProfit()), 9);
            }
            writer.section("毛利");
            writer.line("订单收入 " + moneyText(data.summary().orderIncome())
                    + "  购物反佣 " + moneyText(data.summary().shoppingProfit())
                    + "  加点利润 " + moneyText(data.summary().optionalProfit())
                    + "  成本支出 " + moneyText(data.summary().regularCost())
                    + "  导服费 " + moneyText(data.summary().guideFee())
                    + "  合计毛利 " + moneyText(data.summary().grossProfit()), 10);
            writer.close();
            document.save(output);
            return new ExportResult(filename(data, ".pdf"), "application/pdf", output);
        } catch (IOException ex) {
            throw new BizException("生成团队毛利表 PDF 失败：" + ex.getMessage());
        }
    }

    private void title(XWPFDocument document, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(18);
        run.setText(value);
    }

    private void paragraph(XWPFDocument document, String value) {
        XWPFRun run = document.createParagraph().createRun();
        run.setBold(true);
        run.setText(value);
    }

    private void keyValueTable(XWPFDocument document, List<List<String>> rows) {
        rowsTable(document, null, List.of("项目", "内容", "项目", "内容"), rows);
    }

    private void rowsTable(XWPFDocument document, String title, List<String> headers, List<List<String>> rows) {
        if (StringUtils.hasText(title)) {
            paragraph(document, title);
        }
        XWPFTable table = document.createTable(Math.max(rows.size() + 1, 2), headers.size());
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);
        fillRow(table.getRow(0), headers, true);
        for (int index = 0; index < rows.size(); index += 1) {
            fillRow(table.getRow(index + 1), rows.get(index), false);
        }
    }

    private void fillRow(XWPFTableRow row, List<String> values, boolean bold) {
        for (int index = 0; index < values.size(); index += 1) {
            XWPFTableCell cell = row.getCell(index);
            cell.removeParagraph(0);
            XWPFRun run = cell.addParagraph().createRun();
            run.setFontSize(9);
            run.setBold(bold);
            run.setText(values.get(index));
        }
    }

    private SalesTeamEntity requireTeam(Long teamId, Long tenantId) {
        SalesTeamEntity team = teamMapper.selectOne(new QueryWrapper<SalesTeamEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", teamId)
                .eq("is_deleted", false));
        if (team == null) {
            throw new BizException("团队不存在");
        }
        return team;
    }

    private SalesProductEntity product(Long productId, Long tenantId) {
        if (productId == null) return null;
        return productMapper.selectOne(new QueryWrapper<SalesProductEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", productId)
                .eq("is_deleted", false));
    }

    private List<SalesBookingOrderEntity> effectiveOrders(Long teamId, Long tenantId) {
        return orderMapper.selectList(new QueryWrapper<SalesBookingOrderEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .orderByAsc("id"))
                .stream()
                .filter(this::isEffectiveOrder)
                .toList();
    }

    private boolean isEffectiveOrder(SalesBookingOrderEntity order) {
        String status = order.getStatus();
        boolean activeStatus = Objects.equals(status, SalesBookingOrderStatus.PENDING.value())
                || Objects.equals(status, SalesBookingOrderStatus.CONFIRMED.value());
        String role = StringUtils.hasText(order.getOrderRole()) ? order.getOrderRole() : SalesBookingOrderRole.NORMAL.value();
        boolean activeRole = Objects.equals(role, SalesBookingOrderRole.NORMAL.value())
                || Objects.equals(role, SalesBookingOrderRole.MERGE_CHILD.value())
                || Objects.equals(role, SalesBookingOrderRole.MERGE_SOURCE.value());
        return activeStatus && activeRole;
    }

    private List<DispatchTeamArrangementEntity> arrangements(Long teamId, Long tenantId) {
        return arrangementMapper.selectList(new QueryWrapper<DispatchTeamArrangementEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .orderByAsc("business_date")
                .orderByAsc("id"));
    }

    private List<DispatchTeamGuideEntity> guides(Long teamId, Long tenantId) {
        return guideMapper.selectList(new QueryWrapper<DispatchTeamGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("team_id", teamId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .orderByAsc("id"));
    }

    private String guideSummary(List<DispatchTeamGuideEntity> guides) {
        if (guides.isEmpty()) return "--";
        return guides.stream()
                .map(item -> item.getGuideMobile() == null
                        ? text(item.getGuideName(), "--")
                        : text(item.getGuideName(), "--") + "[Tel：" + item.getGuideMobile() + "]")
                .reduce((left, right) -> left + "、" + right)
                .orElse("--");
    }

    private static BigDecimal firstPositive(BigDecimal first, BigDecimal second) {
        BigDecimal value = money(first);
        return value.compareTo(BigDecimal.ZERO) != 0 ? value : money(second);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static int number(Integer value) {
        return value == null ? 0 : value;
    }

    private static String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static String moneyText(BigDecimal value) {
        return "¥" + money(value).toPlainString();
    }

    private static String moneyTextNoSymbol(BigDecimal value) {
        return money(value).stripTrailingZeros().toPlainString();
    }

    private static String dateText(java.time.LocalDate value) {
        return value == null ? "--" : value.format(DateTimeFormatter.ISO_DATE);
    }

    private static String filename(SalesTeamGrossProfitPreviewResponse data, String suffix) {
        return "团队毛利表" + text(data.team().teamNo(), "团队") + suffix;
    }

    /** 简单 PDF 文本写入器，按 A4 分页写入中文内容。 */
    private static final class PdfWriter {
        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y = 800;

        private PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            this.font = loadFont(document);
            newPage();
        }

        private void section(String title) throws IOException {
            line(title, 12);
        }

        private void line(String value, int fontSize) throws IOException {
            if (y < 48) {
                newPage();
            }
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(36, y);
            stream.showText(clip(value == null ? "" : value.replace("\n", " "), 95));
            stream.endText();
            y -= fontSize + 8;
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = 800;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private static PDType0Font loadFont(PDDocument document) throws IOException {
            java.io.File fontFile = resolveFontFile();
            String filename = fontFile.getName().toLowerCase();
            if (filename.endsWith(".ttc")) {
                try (TrueTypeCollection collection = new TrueTypeCollection(fontFile)) {
                    TrueTypeFont font = collection.getFontByName("Songti SC");
                    if (font == null) {
                        font = collection.getFontByName("STSongti-SC-Regular");
                    }
                    if (font == null) {
                        font = collection.getFontByName("Noto Sans CJK SC");
                    }
                    if (font == null) {
                        final TrueTypeFont[] firstFont = new TrueTypeFont[1];
                        collection.processAllFonts(candidate -> {
                            if (firstFont[0] == null) {
                                firstFont[0] = candidate;
                            }
                        });
                        font = firstFont[0];
                    }
                    if (font == null) {
                        throw new BizException("未找到可用于 PDF 的中文字体，请配置 PRINT_CJK_FONT_PATH");
                    }
                    return PDType0Font.load(document, font, true);
                }
            }
            return PDType0Font.load(document, fontFile);
        }

        private static java.io.File resolveFontFile() {
            String configured = System.getenv("PRINT_CJK_FONT_PATH");
            if (StringUtils.hasText(configured)) {
                java.io.File file = new java.io.File(configured);
                if (file.exists()) return file;
            }
            List<String> candidates = List.of(
                    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                    "/System/Library/Fonts/Supplemental/Songti.ttc",
                    "/System/Library/Fonts/STHeiti Medium.ttc",
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
            );
            return candidates.stream()
                    .map(java.io.File::new)
                    .filter(java.io.File::exists)
                    .findFirst()
                    .orElseThrow(() -> new BizException("未找到可用于 PDF 的中文字体，请配置 PRINT_CJK_FONT_PATH"));
        }

        private static String clip(String value, int maxLength) {
            if (value.length() <= maxLength) return value;
            return value.substring(0, Math.max(0, maxLength - 1)) + "…";
        }
    }
}
