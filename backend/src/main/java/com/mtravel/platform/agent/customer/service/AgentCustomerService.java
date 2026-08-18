package com.mtravel.platform.agent.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.customer.entity.AgentCustomerServiceSettingEntity;
import com.mtravel.platform.agent.customer.mapper.AgentCustomerServiceSettingMapper;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Agent 客户服务上下文服务。
 *
 * <p>所有下游 Agent Service 都通过这里重新校验客户状态和能力，不能依赖调用方提前查询上下文。</p>
 */
@Service
public class AgentCustomerService {

    private final CustomerUnitMapper customerMapper;
    private final CustomerCategoryMapper categoryMapper;
    private final AgentCustomerServiceSettingMapper settingMapper;

    public AgentCustomerService(
            CustomerUnitMapper customerMapper,
            CustomerCategoryMapper categoryMapper,
            AgentCustomerServiceSettingMapper settingMapper
    ) {
        this.customerMapper = customerMapper;
        this.categoryMapper = categoryMapper;
        this.settingMapper = settingMapper;
    }

    /** 查询客户的 Agent 服务上下文。 */
    public AgentCustomerApi.ServiceContext serviceContext(Long tenantId, Long customerId) {
        return accessContext(tenantId, customerId).publicContext();
    }

    /** 查询内部权限上下文并校验指定能力。 */
    public AgentCustomerAccess requireCapability(
            Long tenantId,
            Long customerId,
            AgentCustomerCapability capability
    ) {
        AgentCustomerAccess access = accessContext(tenantId, customerId);
        AgentCustomerApi.ServiceContext context = access.publicContext();
        boolean allowed = switch (capability) {
            case QUERY_PRODUCTS -> context.canQueryProducts();
            case QUERY_PRICES -> context.canQueryPrices();
            case QUERY_POLICIES -> context.canQueryPolicies();
            case CREATE_QUOTE_REQUEST -> context.canCreateQuoteRequests();
            case CREATE_HANDOFF -> context.canCreateHandoffs();
        };
        if (!allowed) {
            throw AgentException.customerRestricted("当前客户状态或服务配置不允许执行该操作");
        }
        return access;
    }

    /** 读取客户主档和专用配置，构建安全默认的权限上下文。 */
    public AgentCustomerAccess accessContext(Long tenantId, Long customerId) {
        CustomerUnitEntity customer = customerMapper.selectOne(new QueryWrapper<CustomerUnitEntity>()
                .eq("tenant_id", tenantId)
                .eq("id", customerId)
                .eq("is_deleted", false)
                .last("LIMIT 1"));
        if (customer == null) {
            throw AgentException.customerNotFound();
        }
        CustomerCategoryEntity category = customer.getCategoryId() == null ? null
                : categoryMapper.selectOne(new QueryWrapper<CustomerCategoryEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("id", customer.getCategoryId())
                        .eq("is_deleted", false)
                        .last("LIMIT 1"));
        AgentCustomerServiceSettingEntity setting = settingMapper.selectOne(
                new QueryWrapper<AgentCustomerServiceSettingEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("customer_id", customerId)
                        .eq("is_deleted", false)
                        .last("LIMIT 1")
        );

        boolean disabled = "disabled".equals(customer.getStatus());
        String serviceState = disabled ? "disabled" : setting == null ? "normal" : setting.getServiceState();
        String accessMode = setting == null ? "none" : value(setting.getProductAccessMode(), "none");
        boolean handoff = disabled
                ? setting == null || flag(setting.getCanCreateHandoffs())
                : setting == null || flag(setting.getCanCreateHandoffs());
        boolean normal = "normal".equals(serviceState);
        boolean manualReview = "manual_review_only".equals(serviceState);
        boolean canQueryProducts = !disabled && setting != null && flag(setting.getCanQueryProducts());
        boolean canQueryPrices = normal && setting != null && flag(setting.getCanQueryPrices());
        boolean canQueryPolicies = !disabled && setting != null && flag(setting.getCanQueryPolicies());
        boolean canCreateQuotes = normal && setting != null && flag(setting.getCanCreateQuoteRequests());
        if (manualReview) {
            canQueryPrices = false;
            canCreateQuotes = false;
        }
        List<String> warnings = new ArrayList<>();
        if (disabled) {
            warnings.add("客户已停用，仅允许创建内部跟进待办");
        } else if (manualReview) {
            warnings.add("客户服务需人工复核，不能返回确认价格或创建询价");
        } else if ("none".equals(accessMode)) {
            warnings.add("客户尚未配置可查询产品范围");
        }
        OffsetDateTime updatedAt = latest(customer.getUpdatedAt(), setting == null ? null : setting.getUpdatedAt());
        AgentCustomerApi.ServiceContext context = new AgentCustomerApi.ServiceContext(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getCustomerName(),
                new AgentCustomerApi.CustomerCategory(
                        customer.getCategoryId(),
                        category == null ? null : category.getCategoryName()
                ),
                serviceState,
                serviceStateLabel(serviceState),
                canQueryProducts,
                canQueryPrices,
                canQueryPolicies,
                canCreateQuotes,
                handoff,
                accessMode,
                new AgentCustomerApi.ServiceDepartment(customer.getDepartmentId(), customer.getDepartmentName()),
                new AgentCustomerApi.Dispatcher(customer.getDispatcherEmployeeId(), customer.getDispatcherName()),
                List.copyOf(warnings),
                updatedAt
        );
        return new AgentCustomerAccess(
                context,
                customer.getCategoryId(),
                setting == null ? null : setting.getDefaultTaxIncluded()
        );
    }

    private boolean flag(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String serviceStateLabel(String state) {
        return switch (state) {
            case "disabled" -> "已停用";
            case "manual_review_only" -> "仅人工复核";
            default -> "正常服务";
        };
    }

    private OffsetDateTime latest(OffsetDateTime first, OffsetDateTime second) {
        if (first == null) return second == null ? OffsetDateTime.now() : second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}
