package com.mtravel.platform.auth.exception;

/**
 * 同账号重复登录异常。
 *
 * <p>当 Redis 中已经存在该用户的在线会话时抛出，用于返回“该账号已在其他设备登录”。</p>
 */
public class DuplicateLoginException extends RuntimeException {

    /**
     * 创建重复登录异常。
     *
     * @param message 返回给前端的业务提示
     */
    public DuplicateLoginException(String message) {
        super(message);
    }
}
