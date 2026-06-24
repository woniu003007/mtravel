package com.mtravel.platform.sales.team.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 销售团队价格保存请求。
 *
 * <p>同一团队同一客户类型只保留一条未删除价格行；再次保存时更新原价格行。</p>
 *
 * @param customerCategoryId 客户分类 ID，空值表示默认客户类型
 * @param customerCategoryName 客户类型名称
 * @param adultPrice 成人价格
 * @param childPrice 儿童价格
 * @param childNoBedPrice 儿童不占床价格
 * @param seniorPrice 老人价格
 * @param extraFee 附加费用
 */
public record SalesTeamPriceSaveRequest(
        Long customerCategoryId,
        @Size(max = 120) String customerCategoryName,
        @DecimalMin(value = "0", message = "成人价格不能小于0") BigDecimal adultPrice,
        @DecimalMin(value = "0", message = "儿童价格不能小于0") BigDecimal childPrice,
        @DecimalMin(value = "0", message = "儿童不占床价格不能小于0") BigDecimal childNoBedPrice,
        @DecimalMin(value = "0", message = "老人价格不能小于0") BigDecimal seniorPrice,
        @DecimalMin(value = "0", message = "附加费用不能小于0") BigDecimal extraFee
) {}
