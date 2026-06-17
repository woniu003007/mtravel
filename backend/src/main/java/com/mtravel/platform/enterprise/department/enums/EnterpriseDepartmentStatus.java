package com.mtravel.platform.enterprise.department.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 企业部门状态。
 *
 * <p>停用部门不再建议作为新员工或新业务归属选择，但历史员工和业务数据可以继续保留引用。</p>
 */
public enum EnterpriseDepartmentStatus {

    /** 启用，可作为员工、角色和业务归属部门。 */
    ACTIVE("active"),

    /** 停用，保留历史归属但不进入常规选择。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseDepartmentStatus(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /**
     * 解析前端状态值。新增部门未传状态时默认启用。
     */
    public static EnterpriseDepartmentStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return ACTIVE;
        }
        for (EnterpriseDepartmentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("部门状态不合法");
    }
}
