package com.mtravel.platform.sales.team.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 销售团队价格实体，对应 sales_team_prices 表。
 *
 * <p>同一团队可以有多个客户类型价格行，价格行删除不等于删除团队。</p>
 */
@TableName("sales_team_prices")
public class SalesTeamPriceEntity extends TenantSoftDeleteEntity {

    /** 所属销售团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 所属产品 ID，便于按产品团期批量查询价格。 */
    @TableField("product_id")
    private Long productId;

    /** 客户分类 ID，为空时表示默认价格。 */
    @TableField("customer_category_id")
    private Long customerCategoryId;

    /** 客户类型名称快照。 */
    @TableField("customer_category_name")
    private String customerCategoryName;

    /** 成人价格。 */
    @TableField("adult_price")
    private BigDecimal adultPrice;

    /** 儿童价格。 */
    @TableField("child_price")
    private BigDecimal childPrice;

    /** 儿童不占床价格。 */
    @TableField("child_no_bed_price")
    private BigDecimal childNoBedPrice;

    /** 老人价格。 */
    @TableField("senior_price")
    private BigDecimal seniorPrice;

    /** 附加费用。 */
    @TableField("extra_fee")
    private BigDecimal extraFee;

    /** 价格状态：active、disabled。 */
    @TableField("status")
    private String status;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getCustomerCategoryId() {
        return customerCategoryId;
    }

    public void setCustomerCategoryId(Long customerCategoryId) {
        this.customerCategoryId = customerCategoryId;
    }

    public String getCustomerCategoryName() {
        return customerCategoryName;
    }

    public void setCustomerCategoryName(String customerCategoryName) {
        this.customerCategoryName = customerCategoryName;
    }

    public BigDecimal getAdultPrice() {
        return adultPrice;
    }

    public void setAdultPrice(BigDecimal adultPrice) {
        this.adultPrice = adultPrice;
    }

    public BigDecimal getChildPrice() {
        return childPrice;
    }

    public void setChildPrice(BigDecimal childPrice) {
        this.childPrice = childPrice;
    }

    public BigDecimal getChildNoBedPrice() {
        return childNoBedPrice;
    }

    public void setChildNoBedPrice(BigDecimal childNoBedPrice) {
        this.childNoBedPrice = childNoBedPrice;
    }

    public BigDecimal getSeniorPrice() {
        return seniorPrice;
    }

    public void setSeniorPrice(BigDecimal seniorPrice) {
        this.seniorPrice = seniorPrice;
    }

    public BigDecimal getExtraFee() {
        return extraFee;
    }

    public void setExtraFee(BigDecimal extraFee) {
        this.extraFee = extraFee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
