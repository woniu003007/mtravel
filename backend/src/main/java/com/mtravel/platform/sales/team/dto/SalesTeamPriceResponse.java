package com.mtravel.platform.sales.team.dto;

import com.mtravel.platform.sales.team.entity.SalesTeamPriceEntity;
import java.math.BigDecimal;

/**
 * 销售团队价格接口返回对象。
 *
 * <p>用于团期列表展示成人、儿童、老人和附加费等客户类型价格。</p>
 */
public record SalesTeamPriceResponse(
        Long id,
        Long teamId,
        Long customerCategoryId,
        String customerCategoryName,
        BigDecimal adultPrice,
        BigDecimal childPrice,
        BigDecimal childNoBedPrice,
        BigDecimal seniorPrice,
        BigDecimal extraFee,
        String status,
        String remark
) {
    /** 将价格实体转换为接口返回对象。 */
    public static SalesTeamPriceResponse fromEntity(SalesTeamPriceEntity entity) {
        return new SalesTeamPriceResponse(
                entity.getId(),
                entity.getTeamId(),
                entity.getCustomerCategoryId(),
                entity.getCustomerCategoryName(),
                entity.getAdultPrice(),
                entity.getChildPrice(),
                entity.getChildNoBedPrice(),
                entity.getSeniorPrice(),
                entity.getExtraFee(),
                entity.getStatus(),
                entity.getRemark()
        );
    }
}
