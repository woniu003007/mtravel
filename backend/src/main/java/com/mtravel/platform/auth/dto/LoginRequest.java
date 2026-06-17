package com.mtravel.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数。
 *
 * @param username 登录账号
 * @param password 登录密码明文，仅用于本次请求校验，不能入库或写日志
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
