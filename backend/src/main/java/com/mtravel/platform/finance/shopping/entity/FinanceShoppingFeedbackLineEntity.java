package com.mtravel.platform.finance.shopping.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 购物店业绩反馈明细实体。
 *
 * <p>该表保存购物店通过群消息、人工录入或 Excel 导入反馈的实际进店和消费数据。
 * 它是购物业绩核对和内部利润测算的事实来源，不代表银行实际收款。</p>
 */
@TableName("finance_shopping_feedback_lines")
public class FinanceShoppingFeedbackLineEntity extends TenantSoftDeleteEntity {

    /** 团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 购物店供应商 ID。 */
    @TableField("supplier_id")
    private Long supplierId;

    /** 购物店名称。 */
    @TableField("shop_name")
    private String shopName;

    /** 导游 ID。 */
    @TableField("guide_id")
    private Long guideId;

    /** 导游姓名快照。 */
    @TableField("guide_name")
    private String guideName;

    /** 消费或反馈业务日期。 */
    @TableField("business_date")
    private LocalDate businessDate;

    /** 进店人数。 */
    @TableField("people_count")
    private Integer peopleCount;

    /** 消费总额。 */
    @TableField("consumption_amount")
    private BigDecimal consumptionAmount;

    /** 公司返佣金额。 */
    @TableField("company_rebate_amount")
    private BigDecimal companyRebateAmount;

    /** 导游从购物店现场取得或应得的佣金金额，仅用于业务核对。 */
    @TableField("guide_commission_amount")
    private BigDecimal guideCommissionAmount;

    /** 人头费金额。 */
    @TableField("head_fee_amount")
    private BigDecimal headFeeAmount;

    /** 返佣计算模式。total总额返佣，category按品类返佣。 */
    @TableField("rebate_calc_mode")
    private String rebateCalcMode;

    /** 反馈来源。manual人工，excel导入，api接口。 */
    @TableField("feedback_source")
    private String feedbackSource;

    /** Excel 导入批次 ID。 */
    @TableField("import_batch_id")
    private Long importBatchId;

    /** 状态。active生效，cancelled作废。 */
    @TableField("status")
    private String status;

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamNo() { return teamNo; }
    public void setTeamNo(String teamNo) { this.teamNo = teamNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public Long getGuideId() { return guideId; }
    public void setGuideId(Long guideId) { this.guideId = guideId; }
    public String getGuideName() { return guideName; }
    public void setGuideName(String guideName) { this.guideName = guideName; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public BigDecimal getConsumptionAmount() { return consumptionAmount; }
    public void setConsumptionAmount(BigDecimal consumptionAmount) { this.consumptionAmount = consumptionAmount; }
    public BigDecimal getCompanyRebateAmount() { return companyRebateAmount; }
    public void setCompanyRebateAmount(BigDecimal companyRebateAmount) { this.companyRebateAmount = companyRebateAmount; }
    public BigDecimal getGuideCommissionAmount() { return guideCommissionAmount; }
    public void setGuideCommissionAmount(BigDecimal guideCommissionAmount) { this.guideCommissionAmount = guideCommissionAmount; }
    public BigDecimal getHeadFeeAmount() { return headFeeAmount; }
    public void setHeadFeeAmount(BigDecimal headFeeAmount) { this.headFeeAmount = headFeeAmount; }
    public String getRebateCalcMode() { return rebateCalcMode; }
    public void setRebateCalcMode(String rebateCalcMode) { this.rebateCalcMode = rebateCalcMode; }
    public String getFeedbackSource() { return feedbackSource; }
    public void setFeedbackSource(String feedbackSource) { this.feedbackSource = feedbackSource; }
    public Long getImportBatchId() { return importBatchId; }
    public void setImportBatchId(Long importBatchId) { this.importBatchId = importBatchId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
