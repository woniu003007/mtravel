package com.mtravel.platform.sales.team.grossprofit.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.dispatch.guide.entity.DispatchTeamGuideEntity;
import com.mtravel.platform.dispatch.guide.mapper.DispatchTeamGuideMapper;
import com.mtravel.platform.dispatch.teamarrangement.entity.DispatchTeamArrangementEntity;
import com.mtravel.platform.dispatch.teamarrangement.mapper.DispatchTeamArrangementMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import com.mtravel.platform.sales.booking.order.mapper.SalesBookingOrderMapper;
import com.mtravel.platform.sales.product.entity.SalesProductEntity;
import com.mtravel.platform.sales.product.mapper.SalesProductMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import com.mtravel.platform.sales.team.grossprofit.dto.SalesTeamGrossProfitPreviewResponse;
import com.mtravel.platform.sales.team.mapper.SalesTeamMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 团队预算毛利表服务测试。
 *
 * <p>这些用例固定旧系统 `团队毛利表(预算)` 的团队级预算利润口径，避免把毛利简化为
 * 订单应收减所有成本，或者把自费、购物成本重复扣减。</p>
 */
class SalesTeamGrossProfitServiceTest {

    @Test
    void previewShouldCalculateBudgetProfitWithOldSystemFormula() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamGuideMapper guideMapper = mock(DispatchTeamGuideMapper.class);
        SalesTeamGrossProfitService service = new SalesTeamGrossProfitService(
                teamMapper,
                productMapper,
                orderMapper,
                arrangementMapper,
                guideMapper
        );
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team());
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                order(11L, "之江饭店工会", "老板号", 28, "成人：452 * 21 = 9492\n成人：563 * 7 = 3941", "13433", "normal", "confirmed"),
                order(12L, "无锡新旅程旅行社", "老板号", 1, "成人：56243 * 1 = 56243", "56243", "normal", "confirmed"),
                order(13L, "建德市欢乐旅行社有限公司", "老板号", 11, "成人：5650 * 11 = 62150", "62150", "merge_child", "confirmed"),
                order(14L, "已拼出来源订单", "老板号", 2, "成人：1 * 2 = 2", "2", "merge_source", "confirmed"),
                order(15L, "已取消订单", "老板号", 1, "成人：999 * 1 = 999", "999", "normal", "cancelled")
        ));
        when(arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                cost("scenic", "雷峰塔", "★成人：10 * 1.0 [531]", "10"),
                cost("hotel", "之江饭店", "★其它：18750 * 1.0", "18750"),
                cost("traffic", "团队票中心", "飞机：340 * 23.0", "7820"),
                cost("vehicle", "阳光车队", "★车费：196.36 * 1.0", "196.36"),
                cost("optional", "廿八都景区", "自费项目", "40000", "80000", "1400", "0", "0"),
                cost("shopping", "车购测试", "购物店", "0", "0", "1800", "2700", "1600")
        ));
        when(guideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(guide("0")));

        SalesTeamGrossProfitPreviewResponse preview = service.preview(253683L, 1L);

        assertThat(preview.summary().orderIncome()).isEqualByComparingTo("131828.00");
        assertThat(preview.summary().regularCost()).isEqualByComparingTo("26776.36");
        assertThat(preview.summary().optionalProfit()).isEqualByComparingTo("38600.00");
        assertThat(preview.summary().shoppingProfit()).isEqualByComparingTo("2500.00");
        assertThat(preview.summary().guideFee()).isEqualByComparingTo("0.00");
        assertThat(preview.summary().grossProfit()).isEqualByComparingTo("146151.64");
        assertThat(preview.team().guestCount()).isEqualTo(42);
        assertThat(preview.incomeRows()).hasSize(4);
        assertThat(preview.costRows()).hasSize(4);
        assertThat(preview.optionalRows()).hasSize(1);
        assertThat(preview.shoppingRows()).hasSize(1);
    }

    @Test
    void exportShouldGenerateDocxAndPdfFiles() {
        SalesTeamGrossProfitService service = serviceForSmallExport();

        SalesTeamGrossProfitService.ExportResult docx = service.export(253683L, "docx", 1L);
        SalesTeamGrossProfitService.ExportResult pdf = service.export(253683L, "pdf", 1L);

        assertThat(docx.filename()).endsWith(".docx");
        assertThat(docx.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(docx.content().size()).isGreaterThan(1000);
        assertThat(pdf.filename()).endsWith(".pdf");
        assertThat(pdf.contentType()).isEqualTo("application/pdf");
        assertThat(pdf.content().size()).isGreaterThan(1000);
    }

    private SalesTeamGrossProfitService serviceForSmallExport() {
        SalesTeamMapper teamMapper = mock(SalesTeamMapper.class);
        SalesProductMapper productMapper = mock(SalesProductMapper.class);
        SalesBookingOrderMapper orderMapper = mock(SalesBookingOrderMapper.class);
        DispatchTeamArrangementMapper arrangementMapper = mock(DispatchTeamArrangementMapper.class);
        DispatchTeamGuideMapper guideMapper = mock(DispatchTeamGuideMapper.class);
        when(teamMapper.selectOne(any(Wrapper.class))).thenReturn(team());
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(product());
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                order(11L, "之江饭店工会", "老板号", 28, "成人：452 * 21 = 9492", "9492", "normal", "confirmed")
        ));
        when(arrangementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                cost("hotel", "之江饭店", "标间：325 * 1.0", "1300")
        ));
        when(guideMapper.selectList(any(Wrapper.class))).thenReturn(List.of(guide("0")));
        return new SalesTeamGrossProfitService(teamMapper, productMapper, orderMapper, arrangementMapper, guideMapper);
    }

    private SalesTeamEntity team() {
        SalesTeamEntity entity = new SalesTeamEntity();
        entity.setId(253683L);
        entity.setTenantId(1L);
        entity.setProductId(88L);
        entity.setTeamNo("CS-FX-N01-251230A");
        entity.setTeamType("zhengtuan");
        entity.setDepartureDate(LocalDate.of(2025, 12, 30));
        entity.setTotalSeats(40);
        entity.setUsedSeats(40);
        entity.setOperatorEmployeeName("陈爱晚");
        return entity;
    }

    private SalesProductEntity product() {
        SalesProductEntity entity = new SalesProductEntity();
        entity.setId(88L);
        entity.setProductName("奇趣梦幻漂流9");
        entity.setTravelDays(8);
        return entity;
    }

    private SalesBookingOrderEntity order(
            Long id,
            String customerName,
            String salesperson,
            int guestCount,
            String priceDetail,
            String receivable,
            String role,
            String status
    ) {
        SalesBookingOrderEntity entity = new SalesBookingOrderEntity();
        entity.setId(id);
        entity.setTeamId(253683L);
        entity.setCustomerName(customerName);
        entity.setSalespersonEmployeeName(salesperson);
        entity.setBookingOperatorEmployeeName(salesperson);
        entity.setGuestCount(guestCount);
        entity.setFeeRemark(priceDetail);
        entity.setReceivableAmount(new BigDecimal(receivable));
        entity.setReceivedAmount(BigDecimal.ZERO);
        entity.setOrderRole(role);
        entity.setStatus(status);
        return entity;
    }

    private DispatchTeamArrangementEntity cost(String type, String supplier, String content, String total) {
        return cost(type, supplier, content, total, "0", "0", "0", "0");
    }

    private DispatchTeamArrangementEntity cost(
            String type,
            String supplier,
            String content,
            String total,
            String sale,
            String guideCommission,
            String companyRebate,
            String headFee
    ) {
        DispatchTeamArrangementEntity entity = new DispatchTeamArrangementEntity();
        entity.setArrangementType(type);
        entity.setSupplierName(supplier);
        entity.setResourceName(supplier);
        entity.setArrangementContent(content);
        entity.setTotalAmount(new BigDecimal(total));
        entity.setCostAmount(new BigDecimal(total));
        entity.setSaleAmount(new BigDecimal(sale));
        entity.setGuideCommissionAmount(new BigDecimal(guideCommission));
        entity.setCompanyRebateAmount(new BigDecimal(companyRebate));
        entity.setHeadFeeAmount(new BigDecimal(headFee));
        entity.setCashAmount(BigDecimal.ZERO);
        entity.setCreditAmount(new BigDecimal(total));
        return entity;
    }

    private DispatchTeamGuideEntity guide(String guideFee) {
        DispatchTeamGuideEntity entity = new DispatchTeamGuideEntity();
        entity.setTeamId(253683L);
        entity.setGuideName("陈婷-女");
        entity.setGuideMobile("13778567212");
        entity.setGuideFee(new BigDecimal(guideFee));
        entity.setOperationFee(BigDecimal.ZERO);
        entity.setImprestAmount(BigDecimal.ZERO);
        entity.setStatus("active");
        return entity;
    }
}
