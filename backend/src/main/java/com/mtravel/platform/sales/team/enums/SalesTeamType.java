package com.mtravel.platform.sales.team.enums;

import com.mtravel.platform.common.BizException;
import java.util.Arrays;
import org.springframework.util.StringUtils;

/**
 * 销售团队类型枚举。
 *
 * <p>团期管理当前默认生成散拼团队，后续整团、散团和单项仍共用销售团队主链路。</p>
 */
public enum SalesTeamType {
    /** 散拼团队，面向多个客户订单拼在同一发团日期下收客。 */
    SANPIN("sanpin", "散拼"),
    /** 整团团队，通常由一个客户主体包团。 */
    ZHENGTUAN("zhengtuan", "整团"),
    /** 散团团队，通常为单独客户订单形成的团队。 */
    SANTUAN("santuan", "散团"),
    /** 单项业务团队，只承载某一项服务或资源安排。 */
    SINGLE("single", "单项");

    private final String value;
    private final String label;

    SalesTeamType(String value, String label) {
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
     * 将接口传入值转换为枚举，空值默认按团期管理的散拼处理。
     */
    public static SalesTeamType fromValueOrDefault(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElse(SANPIN);
    }

    /**
     * 将接口传入值严格转换为团队类型。
     *
     * <p>直接创建团队、修改团队等正式写入场景不能静默兜底，避免把整团、散团误保存为散拼。</p>
     */
    public static SalesTeamType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("团队类型不能为空");
        }
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BizException("团队类型不合法"));
    }
}
