package com.mtravel.platform.agent.customer.service;

import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;

/** Agent 内部使用的客户权限上下文。 */
public record AgentCustomerAccess(
        AgentCustomerApi.ServiceContext publicContext,
        Long categoryId,
        Boolean defaultTaxIncluded
) {
}
