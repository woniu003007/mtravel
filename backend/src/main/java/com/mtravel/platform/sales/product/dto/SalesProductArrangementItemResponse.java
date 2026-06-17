package com.mtravel.platform.sales.product.dto;

import com.mtravel.platform.sales.product.entity.SalesProductArrangementItemEntity;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售产品团队安排参数返回对象。
 *
 * <p>用于产品编辑页团队安排 tab 回显默认安排和费用参考。</p>
 */
public record SalesProductArrangementItemResponse(
        Long id,
        String arrangementType,
        String itemName,
        String arrangementContent,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String unitName,
        String settlementType,
        String remark,
        String allocationMode,
        String scheduleStartDay,
        String scheduleEndDay,
        String departurePlace,
        String arrivalPlace,
        Integer daysCount,
        String resourceName,
        Long supplierId,
        String supplierName,
        String driverName,
        String vehiclePlate,
        String trafficType,
        String vehicleType,
        String mealType,
        String fundIncluded,
        Boolean confirmed,
        String confirmationNo,
        Long guideId,
        String guideName,
        Long responsibleEmployeeId,
        String responsibleEmployeeName,
        String orderScope,
        BigDecimal totalAmount,
        BigDecimal cashAmount,
        BigDecimal creditAmount,
        BigDecimal prepaidAmount,
        BigDecimal saleAmount,
        BigDecimal costAmount,
        BigDecimal guideCommissionAmount,
        BigDecimal companyRebateAmount,
        BigDecimal headFeeAmount,
        BigDecimal consumptionAmount,
        BigDecimal peopleCount,
        Boolean noGuideReport,
        List<SalesProductArrangementPriceLineResponse> priceLines
) {
    /** 将团队安排参数实体转换为接口响应。 */
    public static SalesProductArrangementItemResponse fromEntity(SalesProductArrangementItemEntity entity) {
        return fromEntity(entity, List.of());
    }

    /**
     * 将团队安排参数实体和价格明细转换为接口响应。
     *
     * @param entity 团队安排参数实体
     * @param priceLines 该安排项下的价格明细，按排序号和主键升序
     * @return 前端团队安排模板页使用的响应对象
     */
    public static SalesProductArrangementItemResponse fromEntity(
            SalesProductArrangementItemEntity entity,
            List<SalesProductArrangementPriceLineResponse> priceLines
    ) {
        return new SalesProductArrangementItemResponse(
                entity.getId(),
                entity.getArrangementType(),
                entity.getItemName(),
                entity.getArrangementContent(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getUnitName(),
                entity.getSettlementType(),
                entity.getRemark(),
                entity.getAllocationMode(),
                entity.getScheduleStartDay(),
                entity.getScheduleEndDay(),
                entity.getDeparturePlace(),
                entity.getArrivalPlace(),
                entity.getDaysCount(),
                entity.getResourceName(),
                entity.getSupplierId(),
                entity.getSupplierName(),
                entity.getDriverName(),
                entity.getVehiclePlate(),
                entity.getTrafficType(),
                entity.getVehicleType(),
                entity.getMealType(),
                entity.getFundIncluded(),
                entity.getConfirmed(),
                entity.getConfirmationNo(),
                entity.getGuideId(),
                entity.getGuideName(),
                entity.getResponsibleEmployeeId(),
                entity.getResponsibleEmployeeName(),
                entity.getOrderScope(),
                entity.getTotalAmount(),
                entity.getCashAmount(),
                entity.getCreditAmount(),
                entity.getPrepaidAmount(),
                entity.getSaleAmount(),
                entity.getCostAmount(),
                entity.getGuideCommissionAmount(),
                entity.getCompanyRebateAmount(),
                entity.getHeadFeeAmount(),
                entity.getConsumptionAmount(),
                entity.getPeopleCount(),
                entity.getNoGuideReport(),
                priceLines == null ? List.of() : priceLines
        );
    }
}
