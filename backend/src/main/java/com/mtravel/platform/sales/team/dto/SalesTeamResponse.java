package com.mtravel.platform.sales.team.dto;

import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 销售团队团期接口返回对象。
 *
 * <p>列表返回团队主信息和当前团队下价格行，前端据此按老系统团期表格展开展示。</p>
 */
public record SalesTeamResponse(
        Long id,
        Long productId,
        String teamNo,
        String teamType,
        String businessType,
        LocalDate departureDate,
        Long departmentId,
        String departmentName,
        Long operatorEmployeeId,
        String operatorEmployeeName,
        Long escortEmployeeId,
        String escortEmployeeName,
        String status,
        Integer totalSeats,
        Integer usedSeats,
        Integer remainingSeats,
        BigDecimal singleRoomDifference,
        Integer closeDaysBefore,
        BigDecimal perCapitaPitAmount,
        BigDecimal optionalMarkupRate,
        BigDecimal perCapitaShoppingAmount,
        List<SalesTeamPriceResponse> prices,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /** 构建团期列表返回对象。 */
    public static SalesTeamResponse fromEntity(SalesTeamEntity entity, List<SalesTeamPriceResponse> prices) {
        return new SalesTeamResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getTeamNo(),
                entity.getTeamType(),
                entity.getBusinessType(),
                entity.getDepartureDate(),
                entity.getDepartmentId(),
                entity.getDepartmentName(),
                entity.getOperatorEmployeeId(),
                entity.getOperatorEmployeeName(),
                entity.getEscortEmployeeId(),
                entity.getEscortEmployeeName(),
                entity.getStatus(),
                entity.getTotalSeats(),
                entity.getUsedSeats(),
                entity.getRemainingSeats(),
                entity.getSingleRoomDifference(),
                entity.getCloseDaysBefore(),
                entity.getPerCapitaPitAmount(),
                entity.getOptionalMarkupRate(),
                entity.getPerCapitaShoppingAmount(),
                prices,
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
