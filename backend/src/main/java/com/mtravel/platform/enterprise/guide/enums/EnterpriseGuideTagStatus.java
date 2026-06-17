package com.mtravel.platform.enterprise.guide.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 导游标签状态枚举。
 *
 * <p>停用标签不会删除历史导游标签关系，但新增或筛选时默认只展示启用标签。</p>
 */
public enum EnterpriseGuideTagStatus {

    /** 启用标签，可被导游档案选择。 */
    ACTIVE("active"),

    /** 停用标签，保留历史关系但不再作为新增可选项。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseGuideTagStatus(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /**
     * 将前端传入标签状态转换为枚举，空值按启用处理。
     *
     * @param value 前端传入状态
     * @return 合法标签状态
     */
    public static EnterpriseGuideTagStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (EnterpriseGuideTagStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("导游标签状态不合法");
    }
}
