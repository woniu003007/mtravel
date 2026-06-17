package com.mtravel.platform.enterprise.productdictionary.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 产品字典实体，对应 product_dictionaries 表。
 *
 * <p>产品字典维护产品模板会用到的可配置选项，例如业务类型、接待标准和产品主题。
 * 后续销售产品、团期计划只选择这里的启用字典，不在产品页面临时维护选项。</p>
 */
@TableName("product_dictionaries")
public class EnterpriseProductDictionaryEntity extends TenantSoftDeleteEntity {

    /** 字典类型，例如 business_type、reception_standard、product_theme。 */
    @TableField("dict_type")
    private String dictType;

    /** 字典名称，用于产品模板页面展示和保存。 */
    @TableField("dict_name")
    private String dictName;

    /** 排序号，数字越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态。active 表示启用，disabled 表示停用。 */
    @TableField("status")
    private String status;

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
