package com.mtravel.platform.purchase.resource.material.enums;

/** 资源介绍素材向量索引状态。 */
public enum PurchaseResourceIntroductionIndexStatus {
    /** 等待向量化或等待配置向量服务。 */
    PENDING("pending"),
    /** 所有正文切片均已写入向量库。 */
    INDEXED("indexed"),
    /** 最近一次向量化失败。 */
    FAILED("failed"),
    /** 索引已随素材删除。 */
    DELETED("deleted");

    private final String value;

    PurchaseResourceIntroductionIndexStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
