package com.mtravel.platform.auth.controller;

import com.mtravel.platform.auth.dto.LoginRequest;
import com.mtravel.platform.auth.dto.LoginResult;
import com.mtravel.platform.auth.service.AuthService;
import com.mtravel.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证接口控制器。
 *
 * <p>只负责暴露登录、登出、心跳和前端权限码占位接口；具体认证规则在 AuthService 和过滤器中实现。</p>
 */
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录接口。
     *
     * @param request 登录账号和密码
     * @return 登录用户信息、访问令牌和无操作超时时间
     */
    @PostMapping("/auth/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 用户退出接口。
     *
     * <p>退出时允许 Authorization 为空，避免前端重复退出或 token 已失效时产生额外错误。</p>
     */
    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(resolveBearerToken(authorization));
        return ApiResponse.ok();
    }

    /**
     * 登录心跳接口。
     *
     * <p>实际续期由 JwtAuthenticationFilter 在请求进入时完成，这里只提供前端定时心跳的稳定接口。</p>
     */
    @PostMapping("/auth/heartbeat")
    public ApiResponse<Void> heartbeat() {
        return ApiResponse.ok();
    }

    /**
     * Token 刷新占位接口。
     *
     * <p>当前阶段使用访问 token 加 Redis 会话续期，不单独签发 refresh token。</p>
     */
    @PostMapping("/auth/refresh")
    public ApiResponse<String> refresh() {
        return ApiResponse.ok("");
    }

    /**
     * 前端权限码占位接口。
     *
     * <p>完整菜单和按钮权限表落地前，先返回客户分类页面所需的固定权限码。</p>
     */
    @GetMapping("/auth/codes")
    public ApiResponse<List<String>> codes() {
        return ApiResponse.ok(List.of("CUSTOMER_CATEGORY_VIEW", "CUSTOMER_CATEGORY_EDIT"));
    }

    /**
     * 从 Authorization 请求头中解析 Bearer token。
     */
    private String resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
