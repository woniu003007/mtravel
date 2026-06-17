package com.mtravel.platform.user;

import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.common.ApiResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

    @GetMapping("/user/info")
    public ApiResponse<UserInfoResponse> info(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(new UserInfoResponse(user.id(), user.username(), user.realName(), user.roles(), "/workspace"));
    }

    public record UserInfoResponse(Long id, String username, String realName, List<String> roles, String homePath) {
    }
}
