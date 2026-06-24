package com.mtravel.platform.sales.team.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售团队团期批量编辑请求。
 *
 * <p>用于复刻旧系统“添加/修改团期信息”弹窗：先选择一个或多个团期，再选择客户类型，
 * 最后按勾选项批量修改预控人数、单房差或客户类型价格。</p>
 *
 * @param teamIds 已选择的团队 ID 列表
 * @param customerCategories 已选择的客户类型；为空时表示处理所选团队下已有全部价格
 * @param synchronizeProduct 是否同步产品内容，本轮仅保留入口
 * @param synchronizeProductWithoutSingleRoom 是否同步产品内容但不含单房差，本轮仅保留入口
 * @param deletePrice 是否删除价格
 * @param updateTotalSeats 是否修改预控人数
 * @param updateSingleRoomDifference 是否修改单房差
 * @param totalSeats 预控人数
 * @param singleRoomDifference 单房差
 * @param adultPrice 成人价格
 * @param childPrice 儿童价格
 * @param childNoBedPrice 儿童不占床价格
 * @param seniorPrice 老人价格
 * @param extraFee 附加费用
 */
public record SalesTeamBatchEditRequest(
        @NotEmpty(message = "请选择团队") List<Long> teamIds,
        @Valid List<CustomerCategoryItem> customerCategories,
        Boolean synchronizeProduct,
        Boolean synchronizeProductWithoutSingleRoom,
        Boolean deletePrice,
        Boolean updateTotalSeats,
        Boolean updateSingleRoomDifference,
        Integer totalSeats,
        @DecimalMin(value = "0", message = "单房差不能小于0") BigDecimal singleRoomDifference,
        @DecimalMin(value = "0", message = "成人价格不能小于0") BigDecimal adultPrice,
        @DecimalMin(value = "0", message = "儿童价格不能小于0") BigDecimal childPrice,
        @DecimalMin(value = "0", message = "儿童不占床价格不能小于0") BigDecimal childNoBedPrice,
        @DecimalMin(value = "0", message = "老人价格不能小于0") BigDecimal seniorPrice,
        @DecimalMin(value = "0", message = "附加费用不能小于0") BigDecimal extraFee
) {

    /**
     * 已选择的客户类型。
     *
     * @param id 客户分类 ID；0 或空值表示默认客户类型
     * @param name 客户类型名称
     */
    public record CustomerCategoryItem(
            Long id,
            @Size(max = 120) String name
    ) {}
}
