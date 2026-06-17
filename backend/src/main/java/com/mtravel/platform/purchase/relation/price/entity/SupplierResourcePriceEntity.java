package com.mtravel.platform.purchase.relation.price.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/**
 * 供应商资源价格实体，对应 supplier_resource_prices 表。
 *
 * <p>一条价格记录挂在采购关系下，并对应一个费用项目，例如苏州园林供应商关系下的成人票、
 * 儿童票价格。这样采购关系只负责绑定，价格管理负责明细。</p>
 */
@TableName("supplier_resource_prices")
public class SupplierResourcePriceEntity extends TenantSoftDeleteEntity {

    /** 采购关系 ID。 */
    @TableField("relation_id")
    private Long relationId;

    /** 费用项目 ID。 */
    @TableField("resource_project_id")
    private Long resourceProjectId;

    /** 项目名称快照，便于历史展示。 */
    @TableField("project_name")
    private String projectName;

    /** 门市价。 */
    @TableField("market_price")
    private BigDecimal marketPrice;

    /** 同行价。 */
    @TableField("peer_price")
    private BigDecimal peerPrice;

    /** 团队价。 */
    @TableField("team_price")
    private BigDecimal teamPrice;

    /** 价格说明。 */
    @TableField("price_description")
    private String priceDescription;

    /** 状态：active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public Long getRelationId() {
        return relationId;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
    }

    public Long getResourceProjectId() {
        return resourceProjectId;
    }

    public void setResourceProjectId(Long resourceProjectId) {
        this.resourceProjectId = resourceProjectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(BigDecimal marketPrice) {
        this.marketPrice = marketPrice;
    }

    public BigDecimal getPeerPrice() {
        return peerPrice;
    }

    public void setPeerPrice(BigDecimal peerPrice) {
        this.peerPrice = peerPrice;
    }

    public BigDecimal getTeamPrice() {
        return teamPrice;
    }

    public void setTeamPrice(BigDecimal teamPrice) {
        this.teamPrice = teamPrice;
    }

    public String getPriceDescription() {
        return priceDescription;
    }

    public void setPriceDescription(String priceDescription) {
        this.priceDescription = priceDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
