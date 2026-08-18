package com.mtravel.platform.agent.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** Agent 客户服务上下文协议对象。 */
public final class AgentCustomerApi {

    private AgentCustomerApi() {
    }

    /** 客户类别白名单信息。 */
    @Schema(name = "AgentCustomerCategory")
    public record CustomerCategory(Long id, String name) { }

    /** 可展示的客户服务部门。 */
    @Schema(name = "AgentCustomerServiceDepartment")
    public record ServiceDepartment(Long id, String name) { }

    /** 可展示的默认计调。 */
    @Schema(name = "AgentCustomerDispatcher")
    public record Dispatcher(Long id, String name) { }

    /** 客户服务上下文，不包含授信、账期、联系方式和内部备注。 */
    @Schema(name = "AgentCustomerServiceContext")
    public record ServiceContext(
            Long customerId,
            String customerCode,
            String customerName,
            CustomerCategory customerCategory,
            String serviceState,
            String serviceStateLabel,
            boolean canQueryProducts,
            boolean canQueryPrices,
            boolean canQueryPolicies,
            boolean canCreateQuoteRequests,
            boolean canCreateHandoffs,
            String productAccessMode,
            ServiceDepartment serviceDepartment,
            Dispatcher dispatcher,
            List<String> warnings,
            OffsetDateTime updatedAt
    ) { }
}
