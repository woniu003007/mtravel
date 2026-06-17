package com.mtravel.platform.dispatch.roomstatus.enums;

/**
 * 自控房间档案状态。
 */
public enum ControlledRoomUnitStatus {
    /** 启用，可以生成房态。 */
    ACTIVE("active"),
    /** 停用，不再生成新房态。 */
    DISABLED("disabled"),
    /** 房间维修，房态应按维修停用处理。 */
    MAINTENANCE("maintenance");

    private final String value;

    ControlledRoomUnitStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
