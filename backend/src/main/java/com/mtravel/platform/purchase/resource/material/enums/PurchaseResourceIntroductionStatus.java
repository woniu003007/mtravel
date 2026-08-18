package com.mtravel.platform.purchase.resource.material.enums;

/** 资源介绍素材发布状态。 */
public enum PurchaseResourceIntroductionStatus {
    /** 草稿，不参与产品生成和向量检索。 */
    DRAFT("draft"),
    /** 已发布，可被产品生成器使用。 */
    PUBLISHED("published"),
    /** 已停用，保留维护记录但不再使用。 */
    DISABLED("disabled");

    private final String value;

    PurchaseResourceIntroductionStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
