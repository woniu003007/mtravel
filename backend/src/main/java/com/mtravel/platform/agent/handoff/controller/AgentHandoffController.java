package com.mtravel.platform.agent.handoff.controller;

import com.mtravel.platform.agent.common.AgentApiResponse;
import com.mtravel.platform.agent.common.AgentRequestContext;
import com.mtravel.platform.agent.handoff.dto.AgentHandoffApi;
import com.mtravel.platform.agent.handoff.service.AgentHandoffService;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.agent.security.service.AgentAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Agent 转人工待办创建接口。 */
@Validated
@RestController
@RequestMapping("/agent/v1")
@Tag(name = "Agent客服-转人工")
public class AgentHandoffController {

    private final AgentAccessService accessService;
    private final AgentHandoffService handoffService;

    public AgentHandoffController(AgentAccessService accessService, AgentHandoffService handoffService) {
        this.accessService = accessService;
        this.handoffService = handoffService;
    }

    /** 幂等创建转人工待办并保存必要聊天上下文。 */
    @Operation(summary = "创建转人工待办")
    @PostMapping("/handoffs")
    public AgentApiResponse<AgentHandoffApi.CreateResult> create(
            @Parameter(description = "16-128 位幂等键", required = true)
            @RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody AgentHandoffApi.CreateRequest body,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AgentServicePrincipal caller = accessService.require(authentication, "agent:write:handoff");
        return AgentApiResponse.ok(
                handoffService.create(caller, idempotencyKey, body),
                AgentRequestContext.requestId(request)
        );
    }
}
