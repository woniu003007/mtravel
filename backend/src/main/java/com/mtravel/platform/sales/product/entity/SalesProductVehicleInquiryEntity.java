package com.mtravel.platform.sales.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 销售产品用车询价记录实体。
 *
 * <p>记录业务人员向车队询价后的回复结果，常见来源是微信群或电话。该表只保存询价过程和选定结果，
 * 不代表车队已经正式派车。</p>
 */
@TableName("sales_product_vehicle_inquiries")
public class SalesProductVehicleInquiryEntity extends TenantSoftDeleteEntity {

    @TableField("product_id") private Long productId;
    @TableField("arrangement_item_id") private Long arrangementItemId;
    @TableField("sort_order") private Integer sortOrder;
    @TableField("inquiry_method") private String inquiryMethod;
    @TableField("inquiry_person") private String inquiryPerson;
    @TableField("inquiry_time") private OffsetDateTime inquiryTime;
    @TableField("group_name") private String groupName;
    @TableField("supplier_id") private Long supplierId;
    @TableField("supplier_name") private String supplierName;
    @TableField("quoted_amount") private BigDecimal quotedAmount;
    @TableField("includes_toll") private Boolean includesToll;
    @TableField("includes_parking") private Boolean includesParking;
    @TableField("includes_driver_meal") private Boolean includesDriverMeal;
    @TableField("includes_driver_lodging") private Boolean includesDriverLodging;
    @TableField("available_vehicle_count") private Integer availableVehicleCount;
    @TableField("reply_person") private String replyPerson;
    @TableField("reply_time") private OffsetDateTime replyTime;
    @TableField("attachment_id") private Long attachmentId;
    @TableField("attachment_url") private String attachmentUrl;
    @TableField("selected") private Boolean selected;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getArrangementItemId() { return arrangementItemId; }
    public void setArrangementItemId(Long arrangementItemId) { this.arrangementItemId = arrangementItemId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getInquiryMethod() { return inquiryMethod; }
    public void setInquiryMethod(String inquiryMethod) { this.inquiryMethod = inquiryMethod; }
    public String getInquiryPerson() { return inquiryPerson; }
    public void setInquiryPerson(String inquiryPerson) { this.inquiryPerson = inquiryPerson; }
    public OffsetDateTime getInquiryTime() { return inquiryTime; }
    public void setInquiryTime(OffsetDateTime inquiryTime) { this.inquiryTime = inquiryTime; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public BigDecimal getQuotedAmount() { return quotedAmount; }
    public void setQuotedAmount(BigDecimal quotedAmount) { this.quotedAmount = quotedAmount; }
    public Boolean getIncludesToll() { return includesToll; }
    public void setIncludesToll(Boolean includesToll) { this.includesToll = includesToll; }
    public Boolean getIncludesParking() { return includesParking; }
    public void setIncludesParking(Boolean includesParking) { this.includesParking = includesParking; }
    public Boolean getIncludesDriverMeal() { return includesDriverMeal; }
    public void setIncludesDriverMeal(Boolean includesDriverMeal) { this.includesDriverMeal = includesDriverMeal; }
    public Boolean getIncludesDriverLodging() { return includesDriverLodging; }
    public void setIncludesDriverLodging(Boolean includesDriverLodging) { this.includesDriverLodging = includesDriverLodging; }
    public Integer getAvailableVehicleCount() { return availableVehicleCount; }
    public void setAvailableVehicleCount(Integer availableVehicleCount) { this.availableVehicleCount = availableVehicleCount; }
    public String getReplyPerson() { return replyPerson; }
    public void setReplyPerson(String replyPerson) { this.replyPerson = replyPerson; }
    public OffsetDateTime getReplyTime() { return replyTime; }
    public void setReplyTime(OffsetDateTime replyTime) { this.replyTime = replyTime; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public Boolean getSelected() { return selected; }
    public void setSelected(Boolean selected) { this.selected = selected; }
}
