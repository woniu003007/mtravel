package com.mtravel.platform.agent.customer.controller;

import com.mtravel.platform.agent.common.AgentApiResponse;
import com.mtravel.platform.agent.common.AgentRequestContext;
import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.agent.security.service.AgentAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Agent 客户服务上下文接口。 */
@Validated
@RestController
@RequestMapping("/agent/v1")
@Tag(name = "Agent客服-客户上下文")
public class AgentCustomerController {

    private final AgentAccessService accessService;
    private final AgentCustomerService customerService;

    public AgentCustomerController(AgentAccessService accessService, AgentCustomerService customerService) {
        this.accessService = accessService;
        this.customerService = customerService;
    }

    /** 查询客户服务状态、产品访问模式和默认负责人。 */
    @Operation(summary = "获取客户服务上下文")
    @GetMapping("/customers/{customerId}/service-context")
    public AgentApiResponse<AgentCustomerApi.ServiceContext> serviceContext(
            @PathVariable @Positive Long customerId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AgentServicePrincipal caller = accessService.require(authentication, "agent:read:customer-context");
        return AgentApiResponse.ok(
                customerService.serviceContext(caller.tenantId(), customerId),
                AgentRequestContext.requestId(request)
        );
    }
}
