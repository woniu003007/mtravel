package com.mtravel.platform.dispatch.guide.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 团队导游安排实体。
 *
 * <p>该表保存正式团队的带团导游、上团下团时间、导服费、备用金、操作费和备注。
 * 导游排班汇总中的团队占用块直接来自该表。</p>
 */
@TableName("dispatch_team_guides")
public class DispatchTeamGuideEntity extends TenantSoftDeleteEntity {

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团队编号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 导游档案 ID。 */
    @TableField("guide_id")
    private Long guideId;

    /** 导游姓名快照。 */
    @TableField("guide_name")
    private String guideName;

    /** 导游手机号快照。 */
    @TableField("guide_mobile")
    private String guideMobile;

    /** 导服费。 */
    @TableField("guide_fee")
    private BigDecimal guideFee;

    /** 导游备用金金额。 */
    @TableField("imprest_amount")
    private BigDecimal imprestAmount;

    /** 操作费。 */
    @TableField("operation_fee")
    private BigDecimal operationFee;

    /** 上团时间。 */
    @TableField("start_at")
    private LocalDateTime startAt;

    /** 下团时间。 */
    @TableField("end_at")
    private LocalDateTime endAt;

    /** 费用说明。 */
    @TableField("fee_memo")
    private String feeMemo;

    /** 导游备注。 */
    @TableField("guide_memo")
    private String guideMemo;

    /** 是否待定中。 */
    @TableField("is_tentative")
    private Boolean isTentative;

    /** 安排状态。active 生效，cancelled 已取消。 */
    @TableField("status")
    private String status;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamNo() {
        return teamNo;
    }

    public void setTeamNo(String teamNo) {
        this.teamNo = teamNo;
    }

    public Long getGuideId() {
        return guideId;
    }

    public void setGuideId(Long guideId) {
        this.guideId = guideId;
    }

    public String getGuideName() {
        return guideName;
    }

    public void setGuideName(String guideName) {
        this.guideName = guideName;
    }

    public String getGuideMobile() {
        return guideMobile;
    }

    public void setGuideMobile(String guideMobile) {
        this.guideMobile = guideMobile;
    }

    public BigDecimal getGuideFee() {
        return guideFee;
    }

    public void setGuideFee(BigDecimal guideFee) {
        this.guideFee = guideFee;
    }

    public BigDecimal getImprestAmount() {
        return imprestAmount;
    }

    public void setImprestAmount(BigDecimal imprestAmount) {
        this.imprestAmount = imprestAmount;
    }

    public BigDecimal getOperationFee() {
        return operationFee;
    }

    public void setOperationFee(BigDecimal operationFee) {
        this.operationFee = operationFee;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getFeeMemo() {
        return feeMemo;
    }

    public void setFeeMemo(String feeMemo) {
        this.feeMemo = feeMemo;
    }

    public String getGuideMemo() {
        return guideMemo;
    }

    public void setGuideMemo(String guideMemo) {
        this.guideMemo = guideMemo;
    }

    public Boolean getIsTentative() {
        return isTentative;
    }

    public void setIsTentative(Boolean tentative) {
        isTentative = tentative;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
