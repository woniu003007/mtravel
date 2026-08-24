package com.mtravel.platform.sales.product.designer.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 产品设计工作台每日资源实体，对应 sales_product_day_resources 表。
 *
 * <p>该表保存产品某一天选用了哪个资源、当时选择的供应商报价和介绍正文快照。产品 Word
 * 生成依赖快照，不能被后续资源主档或资源介绍修改反向影响。</p>
 */
@TableName("sales_product_day_resources")
public class SalesProductDayResourceEntity extends TenantSoftDeleteEntity {

    @TableField("product_id") private Long productId;
    @TableField("day_no") private Integer dayNo;
    @TableField("resource_id") private Long resourceId;
    @TableField("resource_name_snapshot") private String resourceNameSnapshot;
    @TableField("resource_type_snapshot") private String resourceTypeSnapshot;
    /** 资源在当天行程中的业务归属，例如住宿、早餐、中餐或晚餐。 */
    @TableField("arrangement_role") private String arrangementRole;
    /** 当晚酒店是否包含次日早餐，仅住宿资源可设置。 */
    @TableField("hotel_breakfast_included") private Boolean hotelBreakfastIncluded;
    @TableField("province_snapshot") private String provinceSnapshot;
    @TableField("city_snapshot") private String citySnapshot;
    @TableField("district_snapshot") private String districtSnapshot;
    @TableField("address_snapshot") private String addressSnapshot;
    @TableField("longitude_snapshot") private BigDecimal longitudeSnapshot;
    @TableField("latitude_snapshot") private BigDecimal latitudeSnapshot;
    @TableField("procurement_mode_snapshot") private String procurementModeSnapshot;
    @TableField("sort_order") private Integer sortOrder;
    @TableField("stay_minutes") private Integer stayMinutes;
    @TableField("include_in_word") private Boolean includeInWord;
    @TableField(value = "supplier_id", updateStrategy = FieldStrategy.ALWAYS) private Long supplierId;
    /** 加入产品时采用的精确采购关系，避免同供应商多报价关系无法回溯。 */
    @TableField(value = "supplier_relation_id_snapshot", updateStrategy = FieldStrategy.ALWAYS)
    private Long supplierRelationIdSnapshot;
    @TableField(value = "supplier_name_snapshot", updateStrategy = FieldStrategy.ALWAYS) private String supplierNameSnapshot;
    /** 报价快照模式：unified、classified 或 pending。 */
    @TableField(value = "price_mode_snapshot", updateStrategy = FieldStrategy.ALWAYS) private String priceModeSnapshot;
    @TableField("unit_price_snapshot") private BigDecimal unitPriceSnapshot;
    @TableField("quantity_snapshot") private BigDecimal quantitySnapshot;
    @TableField("cost_amount_snapshot") private BigDecimal costAmountSnapshot;
    @TableField(value = "selected_introduction_id", updateStrategy = FieldStrategy.ALWAYS) private Long selectedIntroductionId;
    @TableField(value = "introduction_index_version", updateStrategy = FieldStrategy.ALWAYS) private Integer introductionIndexVersion;
    @TableField(value = "introduction_title_snapshot", updateStrategy = FieldStrategy.ALWAYS) private String introductionTitleSnapshot;
    @TableField(value = "introduction_content_snapshot", updateStrategy = FieldStrategy.ALWAYS) private String introductionContentSnapshot;
    @TableField(value = "introduction_notice_snapshot", updateStrategy = FieldStrategy.ALWAYS) private String introductionNoticeSnapshot;
    @TableField(value = "introduction_warm_tip_snapshot", updateStrategy = FieldStrategy.ALWAYS) private String introductionWarmTipSnapshot;
    @TableField(value = "introduction_visit_duration_snapshot", updateStrategy = FieldStrategy.ALWAYS) private String introductionVisitDurationSnapshot;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getDayNo() { return dayNo; }
    public void setDayNo(Integer dayNo) { this.dayNo = dayNo; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getResourceNameSnapshot() { return resourceNameSnapshot; }
    public void setResourceNameSnapshot(String resourceNameSnapshot) { this.resourceNameSnapshot = resourceNameSnapshot; }
    public String getResourceTypeSnapshot() { return resourceTypeSnapshot; }
    public void setResourceTypeSnapshot(String resourceTypeSnapshot) { this.resourceTypeSnapshot = resourceTypeSnapshot; }
    public String getArrangementRole() { return arrangementRole; }
    public void setArrangementRole(String arrangementRole) { this.arrangementRole = arrangementRole; }
    public Boolean getHotelBreakfastIncluded() { return hotelBreakfastIncluded; }
    public void setHotelBreakfastIncluded(Boolean hotelBreakfastIncluded) { this.hotelBreakfastIncluded = hotelBreakfastIncluded; }
    public String getProvinceSnapshot() { return provinceSnapshot; }
    public void setProvinceSnapshot(String provinceSnapshot) { this.provinceSnapshot = provinceSnapshot; }
    public String getCitySnapshot() { return citySnapshot; }
    public void setCitySnapshot(String citySnapshot) { this.citySnapshot = citySnapshot; }
    public String getDistrictSnapshot() { return districtSnapshot; }
    public void setDistrictSnapshot(String districtSnapshot) { this.districtSnapshot = districtSnapshot; }
    public String getAddressSnapshot() { return addressSnapshot; }
    public void setAddressSnapshot(String addressSnapshot) { this.addressSnapshot = addressSnapshot; }
    public BigDecimal getLongitudeSnapshot() { return longitudeSnapshot; }
    public void setLongitudeSnapshot(BigDecimal longitudeSnapshot) { this.longitudeSnapshot = longitudeSnapshot; }
    public BigDecimal getLatitudeSnapshot() { return latitudeSnapshot; }
    public void setLatitudeSnapshot(BigDecimal latitudeSnapshot) { this.latitudeSnapshot = latitudeSnapshot; }
    public String getProcurementModeSnapshot() { return procurementModeSnapshot; }
    public void setProcurementModeSnapshot(String procurementModeSnapshot) { this.procurementModeSnapshot = procurementModeSnapshot; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStayMinutes() { return stayMinutes; }
    public void setStayMinutes(Integer stayMinutes) { this.stayMinutes = stayMinutes; }
    public Boolean getIncludeInWord() { return includeInWord; }
    public void setIncludeInWord(Boolean includeInWord) { this.includeInWord = includeInWord; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getSupplierRelationIdSnapshot() { return supplierRelationIdSnapshot; }
    public void setSupplierRelationIdSnapshot(Long supplierRelationIdSnapshot) { this.supplierRelationIdSnapshot = supplierRelationIdSnapshot; }
    public String getSupplierNameSnapshot() { return supplierNameSnapshot; }
    public void setSupplierNameSnapshot(String supplierNameSnapshot) { this.supplierNameSnapshot = supplierNameSnapshot; }
    public String getPriceModeSnapshot() { return priceModeSnapshot; }
    public void setPriceModeSnapshot(String priceModeSnapshot) { this.priceModeSnapshot = priceModeSnapshot; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }
    public BigDecimal getQuantitySnapshot() { return quantitySnapshot; }
    public void setQuantitySnapshot(BigDecimal quantitySnapshot) { this.quantitySnapshot = quantitySnapshot; }
    public BigDecimal getCostAmountSnapshot() { return costAmountSnapshot; }
    public void setCostAmountSnapshot(BigDecimal costAmountSnapshot) { this.costAmountSnapshot = costAmountSnapshot; }
    public Long getSelectedIntroductionId() { return selectedIntroductionId; }
    public void setSelectedIntroductionId(Long selectedIntroductionId) { this.selectedIntroductionId = selectedIntroductionId; }
    public Integer getIntroductionIndexVersion() { return introductionIndexVersion; }
    public void setIntroductionIndexVersion(Integer introductionIndexVersion) { this.introductionIndexVersion = introductionIndexVersion; }
    public String getIntroductionTitleSnapshot() { return introductionTitleSnapshot; }
    public void setIntroductionTitleSnapshot(String introductionTitleSnapshot) { this.introductionTitleSnapshot = introductionTitleSnapshot; }
    public String getIntroductionContentSnapshot() { return introductionContentSnapshot; }
    public void setIntroductionContentSnapshot(String introductionContentSnapshot) { this.introductionContentSnapshot = introductionContentSnapshot; }
    public String getIntroductionNoticeSnapshot() { return introductionNoticeSnapshot; }
    public void setIntroductionNoticeSnapshot(String introductionNoticeSnapshot) { this.introductionNoticeSnapshot = introductionNoticeSnapshot; }
    public String getIntroductionWarmTipSnapshot() { return introductionWarmTipSnapshot; }
    public void setIntroductionWarmTipSnapshot(String introductionWarmTipSnapshot) { this.introductionWarmTipSnapshot = introductionWarmTipSnapshot; }
    public String getIntroductionVisitDurationSnapshot() { return introductionVisitDurationSnapshot; }
    public void setIntroductionVisitDurationSnapshot(String introductionVisitDurationSnapshot) { this.introductionVisitDurationSnapshot = introductionVisitDurationSnapshot; }
}
