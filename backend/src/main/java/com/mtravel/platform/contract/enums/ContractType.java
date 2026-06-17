package com.mtravel.platform.contract.enums;

import com.mtravel.platform.common.BizException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 合同业务类型。
 *
 * <p>类型与合同管理页签保持一致，统一管理编号前缀和类型合法性，避免字符串散落在服务层。</p>
 */
public enum ContractType {
    CUSTOMER("customer", "分销商", ""),
    SCENIC("scenic", "景区", "JQ"),
    HOTEL("hotel", "酒店", "JD"),
    RESTAURANT("restaurant", "餐厅", "CT"),
    VEHICLE("vehicle", "车队", "CD"),
    TRAFFIC("traffic", "大交通", "JT"),
    OTHER("other", "其它", "QT"),
    GROUND_AGENT("ground_agent", "地接", "DJ"),
    GUIDE("guide", "导游", "DY"),
    FINANCE_FEE("finance_fee", "财务费用", "CWF"),
    CURRENT_REFUND("current_refund", "现收现退", "XST"),
    EXTRA_FEE("extra_fee", "附加费用", "FJF"),
    SHOPPING("shopping", "购物", "GW");

    private final String value;
    private final String label;
    private final String numberCode;

    ContractType(String value, String label, String numberCode) {
        this.value = value;
        this.label = label;
        this.numberCode = numberCode;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    /** 根据合同类型生成当天合同编号前缀。 */
    public String numberPrefix(LocalDate date) {
        String dateText = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        return this == CUSTOMER ? "HT-%s-".formatted(dateText) : "HT-%s-%s-".formatted(numberCode, dateText);
    }

    /** 解析接口传入的合同类型；非法值统一抛出业务异常。 */
    public static ContractType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new BizException("合同类型不合法"));
    }
}
