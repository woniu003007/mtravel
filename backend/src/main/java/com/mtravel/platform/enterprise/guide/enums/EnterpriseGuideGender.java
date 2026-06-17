package com.mtravel.platform.enterprise.guide.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 企业导游性别枚举。
 *
 * <p>性别用于导游档案展示和后续导游端资料补全，不参与权限判断。</p>
 */
public enum EnterpriseGuideGender {

    /** 男。 */
    MALE("male"),

    /** 女。 */
    FEMALE("female"),

    /** 未填写或未知。 */
    UNKNOWN("unknown");

    private final String value;

    EnterpriseGuideGender(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /**
     * 将前端传入性别转换为枚举。
     *
     * @param value 前端传入性别，空值按 unknown 处理
     * @return 合法性别
     */
    public static EnterpriseGuideGender fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return UNKNOWN;
        }
        for (EnterpriseGuideGender gender : values()) {
            if (gender.value.equals(value)) {
                return gender;
            }
        }
        throw new BizException("导游性别不合法");
    }
}
