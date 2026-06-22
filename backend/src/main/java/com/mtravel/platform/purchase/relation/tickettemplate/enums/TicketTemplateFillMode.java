package com.mtravel.platform.purchase.relation.tickettemplate.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 游客名单模板列填充方式。
 *
 * <p>不同景区模板不一定所有列都来自游客字段。例如序号需要自动递增，票型名称可能固定写成人，
 * 备注列可能需要保留模板原说明文字。</p>
 */
public enum TicketTemplateFillMode {
    /** 从系统游客名单字段取值。 */
    TOURIST_FIELD("tourist_field", "游客字段"),

    /** 导出时按游客顺序自动生成 1、2、3。 */
    SEQUENCE("sequence", "自动序号"),

    /** 导出时写入配置好的固定值。 */
    CONSTANT("constant", "固定值"),

    /** 导出时不改写该列，保留模板原内容。 */
    KEEP_ORIGINAL("keep_original", "不填充");

    private final String value;
    private final String label;

    TicketTemplateFillMode(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    /** 根据接口字段值查找填充方式。 */
    public static Optional<TicketTemplateFillMode> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst();
    }
}
