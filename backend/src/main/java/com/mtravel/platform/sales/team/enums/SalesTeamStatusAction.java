package com.mtravel.platform.sales.team.enums;

/**
 * 销售团队状态动作枚举。
 *
 * <p>日志使用动作枚举记录用户实际点击行为，状态值只表达结果，二者分开便于后续审计。</p>
 */
public enum SalesTeamStatusAction {
    /** 批量新增团期时创建团队。 */
    CREATE("create"),
    /** 正常团队停收。 */
    STOP("stop"),
    /** 停收团队重新启用。 */
    START("start"),
    /** 正常或停收团队取消。 */
    CANCEL("cancel"),
    /** 取消团队恢复为正常。 */
    RECOVER("recover"),
    /** 取消团队执行软删除。 */
    DELETE("delete");

    private final String value;

    SalesTeamStatusAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
