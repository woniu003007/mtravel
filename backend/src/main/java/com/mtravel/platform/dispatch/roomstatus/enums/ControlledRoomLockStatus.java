package com.mtravel.platform.dispatch.roomstatus.enums;

/**
 * 自控房源锁房流水状态。
 */
public enum ControlledRoomLockStatus {
    /** 已锁定，等待后续排房确认。 */
    LOCKED("locked"),
    /** 已转占用，表示排房已确认。 */
    OCCUPIED("occupied"),
    /** 已释放，房态已恢复可用。 */
    RELEASED("released");

    private final String value;

    ControlledRoomLockStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
