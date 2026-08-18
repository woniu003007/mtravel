package com.mtravel.platform.agent.customer;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.agent.customer.entity.AgentCustomerServiceSettingEntity;
import com.mtravel.platform.agent.customer.mapper.AgentCustomerServiceSettingMapper;
import com.mtravel.platform.agent.customer.service.AgentCustomerService;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Agent 客户服务上下文测试。 */
class AgentCustomerServiceTest {

    @Test
    void activeCustomerWithoutAgentSettingsShouldDefaultToNoProductAccess() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        AgentCustomerServiceSettingMapper settingMapper = mock(AgentCustomerServiceSettingMapper.class);
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(customer("active"));
        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(category());
        when(settingMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        var context = new AgentCustomerService(customerMapper, categoryMapper, settingMapper)
                .serviceContext(1L, 13L);

        assertThat(context.serviceState()).isEqualTo("normal");
        assertThat(context.productAccessMode()).isEqualTo("none");
        assertThat(context.canQueryProducts()).isFalse();
        assertThat(context.canQueryPrices()).isFalse();
        assertThat(context.canCreateHandoffs()).isTrue();
        assertThat(context.customerCategory().name()).isEqualTo("组团旅行社");
    }

    @Test
    void disabledCustomerShouldOnlyKeepHandoffCapability() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        AgentCustomerServiceSettingMapper settingMapper = mock(AgentCustomerServiceSettingMapper.class);
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(customer("disabled"));
        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(category());
        when(settingMapper.selectOne(any(Wrapper.class))).thenReturn(allEnabledSetting());

        var context = new AgentCustomerService(customerMapper, categoryMapper, settingMapper)
                .serviceContext(1L, 13L);

        assertThat(context.serviceState()).isEqualTo("disabled");
        assertThat(context.canQueryProducts()).isFalse();
        assertThat(context.canQueryPrices()).isFalse();
        assertThat(context.canQueryPolicies()).isFalse();
        assertThat(context.canCreateQuoteRequests()).isFalse();
        assertThat(context.canCreateHandoffs()).isTrue();
    }

    private CustomerUnitEntity customer(String status) {
        CustomerUnitEntity entity = new CustomerUnitEntity();
        entity.setId(13L);
        entity.setTenantId(1L);
        entity.setCustomerCode("CU-013");
        entity.setCustomerName("南京金陵假日旅行社");
        entity.setCategoryId(6L);
        entity.setDepartmentId(2L);
        entity.setDepartmentName("华东销售部");
        entity.setDispatcherEmployeeId(7L);
        entity.setDispatcherName("计调A");
        entity.setStatus(status);
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setIsDeleted(false);
        return entity;
    }

    private CustomerCategoryEntity category() {
        CustomerCategoryEntity entity = new CustomerCategoryEntity();
        entity.setId(6L);
        entity.setTenantId(1L);
        entity.setCategoryName("组团旅行社");
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }

    private AgentCustomerServiceSettingEntity allEnabledSetting() {
        AgentCustomerServiceSettingEntity entity = new AgentCustomerServiceSettingEntity();
        entity.setTenantId(1L);
        entity.setCustomerId(13L);
        entity.setServiceState("normal");
        entity.setProductAccessMode("authorized_only");
        entity.setCanQueryProducts(true);
        entity.setCanQueryPrices(true);
        entity.setCanQueryPolicies(true);
        entity.setCanCreateQuoteRequests(true);
        entity.setCanCreateHandoffs(true);
        entity.setIsDeleted(false);
        return entity;
    }
}
