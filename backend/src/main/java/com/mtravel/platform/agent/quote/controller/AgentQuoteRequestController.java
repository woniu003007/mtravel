package com.mtravel.platform.agent.quote.controller;

import com.mtravel.platform.agent.common.AgentApiResponse;
import com.mtravel.platform.agent.common.AgentRequestContext;
import com.mtravel.platform.agent.quote.dto.AgentQuoteApi;
import com.mtravel.platform.agent.quote.service.AgentQuoteRequestService;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.agent.security.service.AgentAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Agent 非标准需求询价任务创建和客户可见结果接口。 */
@Validated
@RestController
@RequestMapping("/agent/v1")
@Tag(name = "Agent客服-询价任务")
public class AgentQuoteRequestController {

    private final AgentAccessService accessService;
    private final AgentQuoteRequestService quoteService;

    public AgentQuoteRequestController(AgentAccessService accessService, AgentQuoteRequestService quoteService) {
        this.accessService = accessService;
        this.quoteService = quoteService;
    }

    /** 幂等创建青旅、车辆、定制线路等非标询价任务。 */
    @Operation(summary = "创建非标需求询价任务")
    @PostMapping("/quote-requests")
    public AgentApiResponse<AgentQuoteApi.CreateResult> create(
            @Parameter(description = "16-128 位幂等键", required = true)
            @RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody AgentQuoteApi.CreateRequest body,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AgentServicePrincipal caller = accessService.require(authentication, "agent:write:quote-request");
        return AgentApiResponse.ok(
                quoteService.create(caller, idempotencyKey, body),
                AgentRequestContext.requestId(request)
        );
    }

    /** 查询询价任务状态和已审核的客户可见报价。 */
    @Operation(summary = "查询询价处理结果")
    @GetMapping("/quote-requests/{quoteRequestId}")
    public AgentApiResponse<AgentQuoteApi.DetailResult> detail(
            @PathVariable @NotBlank String quoteRequestId,
            @RequestParam @Positive Long customerId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AgentServicePrincipal caller = accessService.require(authentication, "agent:read:quote-request");
        return AgentApiResponse.ok(
                quoteService.detail(caller.tenantId(), customerId, quoteRequestId),
                AgentRequestContext.requestId(request)
        );
    }
}
