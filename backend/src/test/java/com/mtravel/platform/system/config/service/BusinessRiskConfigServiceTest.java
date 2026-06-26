package com.mtravel.platform.system.config.service;

import com.mtravel.platform.system.config.dto.BusinessRiskConfigUpdateRequest;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务风控配置测试。
 *
 * <p>总经理审批开关直接决定收客订单是否被合同到期、授信超限阻断，默认值和持久化必须明确。</p>
 */
class BusinessRiskConfigServiceTest {

    @Test
    void riskApprovalShouldBeDisabledByDefault() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        BusinessRiskConfigService service = new BusinessRiskConfigService(mapper);

        assertThat(service.isCustomerRiskApprovalEnabled(1L)).isFalse();
        assertThat(service.getBusinessRiskConfig(1L).customerRiskApprovalEnabled()).isFalse();
    }

    @Test
    void updateShouldPersistCustomerRiskApprovalSwitch() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        BusinessRiskConfigService service = new BusinessRiskConfigService(mapper);

        service.updateBusinessRiskConfig(1L, new BusinessRiskConfigUpdateRequest(true));

        ArgumentCaptor<SystemConfigEntity> captor = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(mapper).upsert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getConfigKey()).isEqualTo(BusinessRiskConfigService.CUSTOMER_RISK_APPROVAL_ENABLED);
        assertThat(captor.getValue().getConfigValue()).isEqualTo("true");
    }
}
