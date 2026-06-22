package com.mtravel.platform.dispatch.vehicleusage.enums;

import com.mtravel.platform.common.BizException;

/**
 * 用车历史候选类型。
 *
 * <p>司机信息和车牌号都来自业务人员手动输入，系统只按类型沉淀常用候选，不代表正式档案。</p>
 */
public enum VehicleUsageHistoryType {
    /** 司机姓名、电话或司机备注信息。 */
    DRIVER_INFO("driver_info"),
    /** 车辆车牌号。 */
    VEHICLE_PLATE("vehicle_plate");

    private final String value;

    VehicleUsageHistoryType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 校验并解析历史候选类型。
     *
     * @param value 前端传入的类型值
     * @return 合法类型
     * @throws BizException 类型不在允许范围内时抛出
     */
    public static VehicleUsageHistoryType fromValue(String value) {
        for (VehicleUsageHistoryType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new BizException("用车历史类型不正确");
    }
}
