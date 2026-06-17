package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 销售产品说明实体，对应 sales_product_descriptions 表。
 *
 * <p>产品说明会被报价单、行程单、合同说明等后续文档复用，因此单独存放，不作为普通备注处理。</p>
 */
@TableName("sales_product_descriptions")
public class SalesProductDescriptionEntity extends TenantSoftDeleteEntity {

    /** 所属产品 ID。 */
    @TableField("product_id")
    private Long productId;

    /** 收客须知。 */
    @TableField("booking_notice")
    private String bookingNotice;

    /** 产品说明正文。 */
    @TableField("product_description")
    private String productDescription;

    /** 费用包含。 */
    @TableField("fee_included")
    private String feeIncluded;

    /** 费用不含。 */
    @TableField("fee_excluded")
    private String feeExcluded;

    /** 儿童安排。 */
    @TableField("child_policy")
    private String childPolicy;

    /** 购物安排。 */
    @TableField("shopping_arrangement")
    private String shoppingArrangement;

    /** 自费项目。 */
    @TableField("optional_items")
    private String optionalItems;

    /** 赠送项目。 */
    @TableField("gift_items")
    private String giftItems;

    /** 注意事项。 */
    @TableField("attention_items")
    private String attentionItems;

    /** 温馨提醒。 */
    @TableField("warm_reminder")
    private String warmReminder;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getBookingNotice() {
        return bookingNotice;
    }

    public void setBookingNotice(String bookingNotice) {
        this.bookingNotice = bookingNotice;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getFeeIncluded() {
        return feeIncluded;
    }

    public void setFeeIncluded(String feeIncluded) {
        this.feeIncluded = feeIncluded;
    }

    public String getFeeExcluded() {
        return feeExcluded;
    }

    public void setFeeExcluded(String feeExcluded) {
        this.feeExcluded = feeExcluded;
    }

    public String getChildPolicy() {
        return childPolicy;
    }

    public void setChildPolicy(String childPolicy) {
        this.childPolicy = childPolicy;
    }

    public String getShoppingArrangement() {
        return shoppingArrangement;
    }

    public void setShoppingArrangement(String shoppingArrangement) {
        this.shoppingArrangement = shoppingArrangement;
    }

    public String getOptionalItems() {
        return optionalItems;
    }

    public void setOptionalItems(String optionalItems) {
        this.optionalItems = optionalItems;
    }

    public String getGiftItems() {
        return giftItems;
    }

    public void setGiftItems(String giftItems) {
        this.giftItems = giftItems;
    }

    public String getAttentionItems() {
        return attentionItems;
    }

    public void setAttentionItems(String attentionItems) {
        this.attentionItems = attentionItems;
    }

    public String getWarmReminder() {
        return warmReminder;
    }

    public void setWarmReminder(String warmReminder) {
        this.warmReminder = warmReminder;
    }
}
