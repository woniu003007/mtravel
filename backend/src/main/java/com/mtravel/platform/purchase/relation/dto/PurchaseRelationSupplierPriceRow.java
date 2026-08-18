package com.mtravel.platform.purchase.relation.dto;

import java.math.BigDecimal;

/**
 * 采购资源供应商报价联查行。
 *
 * <p>用于资源消费方批量读取有效供应商、默认关系和分类报价，避免按关系、供应商、报价明细分段查询。
 * 一条分类报价会返回一行；统一报价或尚无分类明细时，报价项目字段为空。</p>
 */
public class PurchaseRelationSupplierPriceRow {

    /** 资源主档 ID。 */
    private Long resourceId;

    /** 资源与供应商绑定关系 ID。 */
    private Long relationId;

    /** 供应商 ID。 */
    private Long supplierId;

    /** 供应商名称。 */
    private String supplierName;

    /** 是否为资源默认供应商。 */
    private Boolean defaultSupplier;

    /** 报价模式：unified 或 classified。 */
    private String priceMode;

    /** 统一报价金额。 */
    private BigDecimal unifiedPrice;

    /** 分类报价费用项目 ID。 */
    private Long resourceProjectId;

    /** 分类报价费用项目名称。 */
    private String projectName;

    /** 分类报价门市价。 */
    private BigDecimal marketPrice;

    /** 分类报价同行价。 */
    private BigDecimal peerPrice;

    /** 分类报价团队价。 */
    private BigDecimal teamPrice;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getRelationId() {
        return relationId;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Boolean getDefaultSupplier() {
        return defaultSupplier;
    }

    public void setDefaultSupplier(Boolean defaultSupplier) {
        this.defaultSupplier = defaultSupplier;
    }

    public String getPriceMode() {
        return priceMode;
    }

    public void setPriceMode(String priceMode) {
        this.priceMode = priceMode;
    }

    public BigDecimal getUnifiedPrice() {
        return unifiedPrice;
    }

    public void setUnifiedPrice(BigDecimal unifiedPrice) {
        this.unifiedPrice = unifiedPrice;
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
}
