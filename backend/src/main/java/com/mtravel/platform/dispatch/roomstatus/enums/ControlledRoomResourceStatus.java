package com.mtravel.platform.dispatch.roomstatus.enums;

/**
 * 自控房源批次状态。
 */
public enum ControlledRoomResourceStatus {
    /** 启用，可生成房态和参与锁房。 */
    ACTIVE("active"),
    /** 停用，不再参与锁房。 */
    DISABLED("disabled"),
    /** 已到期，不允许新增锁房但保留历史记录。 */
    EXPIRED("expired");

    private final String value;

    ControlledRoomResourceStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** 空值按启用处理，便于新增表单减少必填项。 */
    public static ControlledRoomResourceStatus fromValueOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        for (ControlledRoomResourceStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("自控房源状态不合法");
    }
}
