package com.mtravel.platform.sales.product.designer.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 产品设计工作台的产品级全程用车快照。
 *
 * <p>用车不归属某一天；资源、供应商和报价均在保存时冻结，避免资源主档后续调整反向改写产品成本。</p>
 */
@TableName("sales_product_designer_vehicle_arrangements")
public class SalesProductDesignerVehicleArrangementEntity extends TenantSoftDeleteEntity {

    @TableField("product_id")
    private Long productId;

    @TableField(value = "resource_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long resourceId;

    @TableField(value = "resource_name_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private String resourceNameSnapshot;

    @TableField(value = "supplier_relation_id_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierRelationIdSnapshot;

    @TableField(value = "supplier_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierId;

    @TableField(value = "supplier_name_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private String supplierNameSnapshot;

    @TableField("price_mode_snapshot")
    private String priceModeSnapshot;

    @TableField("vehicle_type_snapshot")
    private String vehicleTypeSnapshot;

    @TableField(value = "start_day_no", updateStrategy = FieldStrategy.ALWAYS)
    private Integer startDayNo;

    @TableField(value = "end_day_no", updateStrategy = FieldStrategy.ALWAYS)
    private Integer endDayNo;

    @TableField("quantity_snapshot")
    private BigDecimal quantitySnapshot;

    @TableField("unit_price_snapshot")
    private BigDecimal unitPriceSnapshot;

    @TableField("cost_amount_snapshot")
    private BigDecimal costAmountSnapshot;

    @TableField("sort_order")
    private Integer sortOrder;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getResourceNameSnapshot() { return resourceNameSnapshot; }
    public void setResourceNameSnapshot(String resourceNameSnapshot) { this.resourceNameSnapshot = resourceNameSnapshot; }
    public Long getSupplierRelationIdSnapshot() { return supplierRelationIdSnapshot; }
    public void setSupplierRelationIdSnapshot(Long supplierRelationIdSnapshot) { this.supplierRelationIdSnapshot = supplierRelationIdSnapshot; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierNameSnapshot() { return supplierNameSnapshot; }
    public void setSupplierNameSnapshot(String supplierNameSnapshot) { this.supplierNameSnapshot = supplierNameSnapshot; }
    public String getPriceModeSnapshot() { return priceModeSnapshot; }
    public void setPriceModeSnapshot(String priceModeSnapshot) { this.priceModeSnapshot = priceModeSnapshot; }
    public String getVehicleTypeSnapshot() { return vehicleTypeSnapshot; }
    public void setVehicleTypeSnapshot(String vehicleTypeSnapshot) { this.vehicleTypeSnapshot = vehicleTypeSnapshot; }
    public Integer getStartDayNo() { return startDayNo; }
    public void setStartDayNo(Integer startDayNo) { this.startDayNo = startDayNo; }
    public Integer getEndDayNo() { return endDayNo; }
    public void setEndDayNo(Integer endDayNo) { this.endDayNo = endDayNo; }
    public BigDecimal getQuantitySnapshot() { return quantitySnapshot; }
    public void setQuantitySnapshot(BigDecimal quantitySnapshot) { this.quantitySnapshot = quantitySnapshot; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }
    public BigDecimal getCostAmountSnapshot() { return costAmountSnapshot; }
    public void setCostAmountSnapshot(BigDecimal costAmountSnapshot) { this.costAmountSnapshot = costAmountSnapshot; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
