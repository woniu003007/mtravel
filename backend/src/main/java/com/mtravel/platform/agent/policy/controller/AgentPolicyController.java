package com.mtravel.platform.agent.policy.controller;

import com.mtravel.platform.agent.common.AgentApiResponse;
import com.mtravel.platform.agent.common.AgentRequestContext;
import com.mtravel.platform.agent.policy.dto.AgentPolicyApi;
import com.mtravel.platform.agent.policy.service.AgentPolicyService;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.agent.security.service.AgentAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Agent 结构化业务政策查询接口。 */
@Validated
@RestController
@RequestMapping("/agent/v1")
@Tag(name = "Agent客服-业务政策")
public class AgentPolicyController {

    private final AgentAccessService accessService;
    private final AgentPolicyService policyService;

    public AgentPolicyController(AgentAccessService accessService, AgentPolicyService policyService) {
        this.accessService = accessService;
        this.policyService = policyService;
    }

    /** 查询指定主题在业务日期生效的最高优先级政策。 */
    @Operation(summary = "查询结构化业务政策")
    @GetMapping("/policies/search")
    public AgentApiResponse<AgentPolicyApi.SearchResult> search(
            @RequestParam @Positive Long customerId,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) @Positive Long scheduleId,
            @Parameter(description = "政策主题编码", example = "minor_without_guardian")
            @RequestParam @NotBlank String topic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AgentServicePrincipal caller = accessService.require(authentication, "agent:read:policy");
        return AgentApiResponse.ok(
                policyService.search(caller.tenantId(), customerId, productId, scheduleId, topic, onDate),
                AgentRequestContext.requestId(request)
        );
    }
}
