package com.mtravel.platform.dispatch.roomstatus.enums;

/**
 * 自控房间每日房态。
 */
public enum ControlledRoomDayStatus {
    /** 可用，可以被锁定或排房。 */
    AVAILABLE("available"),
    /** 已锁定，表示团队排房前预占。 */
    LOCKED("locked"),
    /** 已占用，表示排房已确认。 */
    OCCUPIED("occupied"),
    /** 维修停用，不能锁房或排房。 */
    MAINTENANCE("maintenance"),
    /** 保留房，人工保留，默认不参与自动推荐。 */
    RESERVED("reserved");

    private final String value;

    ControlledRoomDayStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
