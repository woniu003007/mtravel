package com.mtravel.platform.purchase.relation.tickettemplate.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 游客名单模板可映射的系统字段。
 *
 * <p>字段值会落库到映射表，后续生成不同景区游客 Excel 时按该字段从游客名单取值。</p>
 */
public enum TouristSystemField {
    /** 游客姓名。 */
    TOURIST_NAME("tourist_name", "游客姓名", "姓名", "游客", "旅客姓名"),

    /** 证件类型，例如身份证、护照。 */
    CERTIFICATE_TYPE("certificate_type", "证件类型", "证件类型", "证件类别"),

    /** 证件号码。 */
    CERTIFICATE_NO("certificate_no", "证件号码", "证件号码", "身份证号", "身份证", "证件号"),

    /** 游客手机号。 */
    MOBILE("mobile", "手机号", "手机号", "手机", "联系电话", "电话"),

    /** 性别。 */
    GENDER("gender", "性别", "性别"),

    /** 出生日期。 */
    BIRTHDAY("birthday", "出生日期", "出生日期", "生日"),

    /** 备注说明。 */
    REMARK("remark", "备注", "备注", "说明");

    private final String value;
    private final String label;
    private final String[] aliases;

    TouristSystemField(String value, String label, String... aliases) {
        this.value = value;
        this.label = label;
        this.aliases = aliases;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    /** 根据前端保存的字段值查找枚举，避免 Service 中散落裸字符串判断。 */
    public static Optional<TouristSystemField> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst();
    }

    /**
     * 根据 Excel 表头给出默认映射建议。
     *
     * <p>这里只做轻量匹配，最终映射仍由用户在页面上确认。</p>
     */
    public static Optional<TouristSystemField> suggestByHeader(String header) {
        String normalized = normalize(header);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> Arrays.stream(item.aliases).anyMatch(alias -> normalized.contains(normalize(alias))))
                .findFirst();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("(", "")
                .replace(")", "")
                .replace("（", "")
                .replace("）", "")
                .replace("必填", "")
                .replace("*", "")
                .replace(" ", "")
                .trim();
    }
}
