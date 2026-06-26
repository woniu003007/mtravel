package com.mtravel.platform.system.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.system.config.dto.BusinessRiskConfigResponse;
import com.mtravel.platform.system.config.dto.BusinessRiskConfigUpdateRequest;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 业务风控配置服务。
 *
 * <p>当前负责客户合同到期、授信超限时是否强制总经理审批的租户级开关。
 * 保存为 system_configs 中的稳定配置键，订单保存服务只读取布尔结果，不解析页面参数。</p>
 */
@Service
public class BusinessRiskConfigService {

    /** 客户风控总经理审批开关。true 表示风险订单必须审批通过后才能保存。 */
    public static final String CUSTOMER_RISK_APPROVAL_ENABLED = "customer_risk_approval_enabled";

    private final SystemConfigMapper mapper;

    public BusinessRiskConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    /** 查询业务风控配置。默认关闭，只做提醒，不阻断历史流程。 */
    public BusinessRiskConfigResponse getBusinessRiskConfig(Long tenantId) {
        return new BusinessRiskConfigResponse(isCustomerRiskApprovalEnabled(tenantId));
    }

    /** 保存业务风控配置。 */
    public BusinessRiskConfigResponse updateBusinessRiskConfig(Long tenantId, BusinessRiskConfigUpdateRequest request) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setTenantId(tenantId);
        entity.setConfigKey(CUSTOMER_RISK_APPROVAL_ENABLED);
        entity.setConfigValue(String.valueOf(Boolean.TRUE.equals(request.customerRiskApprovalEnabled())));
        entity.setRemark("客户合同到期或授信超限时是否强制总经理审批");
        mapper.upsert(entity);
        return getBusinessRiskConfig(tenantId);
    }

    /** 判断客户风控总经理审批是否启用。 */
    public boolean isCustomerRiskApprovalEnabled(Long tenantId) {
        SystemConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getTenantId, tenantId)
                .eq(SystemConfigEntity::getConfigKey, CUSTOMER_RISK_APPROVAL_ENABLED));
        if (entity == null || !StringUtils.hasText(entity.getConfigValue())) {
            return false;
        }
        return Boolean.parseBoolean(entity.getConfigValue().trim());
    }
}
