package com.mtravel.platform.enterprise.guide.enums;

import com.mtravel.platform.common.BizException;
import org.springframework.util.StringUtils;

/**
 * 导游企业码绑定状态枚举。
 *
 * <p>企业码状态只描述导游端账号或企业码绑定情况，不等同于导游档案启停状态。</p>
 */
public enum EnterpriseGuideEnterpriseCodeStatus {

    /** 未加入企业码，尚未发出签约邀请。 */
    NOT_JOINED("not_joined"),

    /** 已生成或发送签约链接，等待导游完成签约。 */
    INVITE_LINK("invite_link"),

    /** 已签约成功，导游已完成企业码加入。 */
    SIGNED_SUCCESS("signed_success"),

    /** 兼容历史数据：已绑定企业码或导游端账号。 */
    BOUND("bound"),

    /** 兼容历史数据：未绑定企业码或导游端账号。 */
    UNBOUND("unbound"),

    /** 企业码已停用，通常需要重新绑定或处理账号。 */
    DISABLED("disabled");

    private final String value;

    EnterpriseGuideEnterpriseCodeStatus(String value) {
        this.value = value;
    }

    /** 返回数据库保存值。 */
    public String getValue() {
        return value;
    }

    /**
     * 将前端传入企业码状态转换为枚举。
     *
     * @param value 前端传入企业码状态，空值按未绑定处理
     * @return 合法企业码状态
     */
    public static EnterpriseGuideEnterpriseCodeStatus fromValueOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return NOT_JOINED;
        }
        for (EnterpriseGuideEnterpriseCodeStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BizException("导游企业码状态不合法");
    }
}
