package com.mtravel.platform.sales.team.enums;

import com.mtravel.platform.common.BizException;
import java.util.Arrays;

/**
 * 销售团队状态枚举。
 *
 * <p>状态影响团期是否继续收客以及是否允许删除。删除前必须先取消，避免误删正在收客的团队。</p>
 */
public enum SalesTeamStatus {
    /** 正常收客状态。 */
    NORMAL("normal", "正常"),
    /** 停收状态，团队暂时不继续收客。 */
    STOPPED("stopped", "停收"),
    /** 取消状态，允许执行软删除。 */
    CANCELLED("cancelled", "取消");

    private final String value;
    private final String label;

    SalesTeamStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 将状态值转换为枚举，不允许未知状态继续参与状态流转。
     */
    public static SalesTeamStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BizException("团队状态不正确"));
    }
}
