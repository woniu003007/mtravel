package com.mtravel.platform.agent.product.controller;

import com.mtravel.platform.agent.common.AgentApiResponse;
import com.mtravel.platform.agent.common.AgentRequestContext;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.agent.product.service.AgentProductService;
import com.mtravel.platform.agent.product.service.AgentScheduleService;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.agent.security.service.AgentAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Agent 产品、行程、团期、余位和客户适用价格接口。 */
@Validated
@RestController
@RequestMapping("/agent/v1")
@Tag(name = "Agent客服-产品与团期")
public class AgentProductController {

    private final AgentAccessService accessService;
    private final AgentProductService productService;
    private final AgentScheduleService scheduleService;

    public AgentProductController(
            AgentAccessService accessService,
            AgentProductService productService,
            AgentScheduleService scheduleService
    ) {
        this.accessService = accessService;
        this.productService = productService;
        this.scheduleService = scheduleService;
    }

    /** 搜索当前客户可查询的启用产品和最近团期摘要。 */
    @Operation(summary = "搜索客户可销售产品")
    @PostMapping("/products/search")
    public AgentApiResponse<AgentProductApi.SearchResult> search(
            @Valid @RequestBody AgentProductApi.SearchRequest body,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AgentServicePrincipal caller = accessService.require(authentication, "agent:read:product");
        return AgentApiResponse.ok(productService.search(caller.tenantId(), body), AgentRequestContext.requestId(request));
    }

    /** 查询允许对当前客户展示的产品详情。 */
    @Operation(summary = "获取对外产品详情")
    @GetMapping("/products/{productId}")
    public AgentApiResponse<AgentProductApi.ProductDetail> detail(
            @PathVariable @Positive Long productId,
            @RequestParam @Positive Long customerId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AgentServicePrincipal caller = accessService.require(authentication, "agent:read:product");
        return AgentApiResponse.ok(
                productService.detail(caller.tenantId(), customerId, productId),
                AgentRequestContext.requestId(request)
        );
    }

    /** 查询实时团期余位和后端计算的客户适用价格。 */
    @Operation(summary = "获取实时团期余位和客户适用价格")
    @GetMapping("/products/{productId}/schedules")
    public AgentApiResponse<AgentProductApi.ScheduleResult> schedules(
            @PathVariable @Positive Long productId,
            @RequestParam @Positive Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) Integer adults,
            @RequestParam(defaultValue = "0") @Min(0) Integer children,
            @RequestParam(defaultValue = "0") @Min(0) Integer childrenNoBed,
            @RequestParam(defaultValue = "0") @Min(0) Integer seniors,
            @RequestParam(defaultValue = "0") @Min(0) Integer singleRooms,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AgentServicePrincipal caller = accessService.require(authentication, "agent:read:schedule");
        AgentProductApi.Party party = new AgentProductApi.Party(adults, children, childrenNoBed, seniors);
        return AgentApiResponse.ok(
                scheduleService.schedules(
                        caller.tenantId(), customerId, productId, from, to, party, singleRooms, page, pageSize
                ),
                AgentRequestContext.requestId(request)
        );
    }
}
