package com.mtravel.platform.sales.product.designer.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/** 产品每日资源选用自费项目的不可变业务快照，成本仅内部可见。 */
@TableName("sales_product_day_resource_optional_items")
public class SalesProductDayResourceOptionalItemEntity extends TenantSoftDeleteEntity {
    @TableField("product_id") private Long productId; @TableField("day_resource_id") private Long dayResourceId;
    @TableField("resource_optional_item_id") private Long resourceOptionalItemId;
    @TableField(value="supplier_optional_item_id", updateStrategy=FieldStrategy.ALWAYS) private Long supplierOptionalItemId;
    @TableField("item_type_snapshot") private String itemTypeSnapshot; @TableField("project_name_snapshot") private String projectNameSnapshot;
    @TableField("price_unit_snapshot") private String priceUnitSnapshot; @TableField("supplier_cost_price_snapshot") private BigDecimal supplierCostPriceSnapshot;
    @TableField("suggested_sale_price_snapshot") private BigDecimal suggestedSalePriceSnapshot; @TableField("final_sale_price") private BigDecimal finalSalePrice;
    @TableField(value="selected_introduction_id", updateStrategy=FieldStrategy.ALWAYS) private Long selectedIntroductionId;
    @TableField("introduction_title_snapshot") private String introductionTitleSnapshot; @TableField("introduction_content_snapshot") private String introductionContentSnapshot;
    @TableField("introduction_notice_snapshot") private String introductionNoticeSnapshot; @TableField("introduction_warm_tip_snapshot") private String introductionWarmTipSnapshot;
    @TableField("introduction_visit_duration_snapshot") private String introductionVisitDurationSnapshot; @TableField("sort_order") private Integer sortOrder;
    public Long getProductId(){return productId;} public void setProductId(Long v){productId=v;} public Long getDayResourceId(){return dayResourceId;} public void setDayResourceId(Long v){dayResourceId=v;} public Long getResourceOptionalItemId(){return resourceOptionalItemId;} public void setResourceOptionalItemId(Long v){resourceOptionalItemId=v;} public Long getSupplierOptionalItemId(){return supplierOptionalItemId;} public void setSupplierOptionalItemId(Long v){supplierOptionalItemId=v;} public String getItemTypeSnapshot(){return itemTypeSnapshot;} public void setItemTypeSnapshot(String v){itemTypeSnapshot=v;} public String getProjectNameSnapshot(){return projectNameSnapshot;} public void setProjectNameSnapshot(String v){projectNameSnapshot=v;} public String getPriceUnitSnapshot(){return priceUnitSnapshot;} public void setPriceUnitSnapshot(String v){priceUnitSnapshot=v;} public BigDecimal getSupplierCostPriceSnapshot(){return supplierCostPriceSnapshot;} public void setSupplierCostPriceSnapshot(BigDecimal v){supplierCostPriceSnapshot=v;} public BigDecimal getSuggestedSalePriceSnapshot(){return suggestedSalePriceSnapshot;} public void setSuggestedSalePriceSnapshot(BigDecimal v){suggestedSalePriceSnapshot=v;} public BigDecimal getFinalSalePrice(){return finalSalePrice;} public void setFinalSalePrice(BigDecimal v){finalSalePrice=v;} public Long getSelectedIntroductionId(){return selectedIntroductionId;} public void setSelectedIntroductionId(Long v){selectedIntroductionId=v;} public String getIntroductionTitleSnapshot(){return introductionTitleSnapshot;} public void setIntroductionTitleSnapshot(String v){introductionTitleSnapshot=v;} public String getIntroductionContentSnapshot(){return introductionContentSnapshot;} public void setIntroductionContentSnapshot(String v){introductionContentSnapshot=v;} public String getIntroductionNoticeSnapshot(){return introductionNoticeSnapshot;} public void setIntroductionNoticeSnapshot(String v){introductionNoticeSnapshot=v;} public String getIntroductionWarmTipSnapshot(){return introductionWarmTipSnapshot;} public void setIntroductionWarmTipSnapshot(String v){introductionWarmTipSnapshot=v;} public String getIntroductionVisitDurationSnapshot(){return introductionVisitDurationSnapshot;} public void setIntroductionVisitDurationSnapshot(String v){introductionVisitDurationSnapshot=v;} public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
}
