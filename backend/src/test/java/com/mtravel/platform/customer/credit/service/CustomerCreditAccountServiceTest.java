package com.mtravel.platform.customer.credit.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.credit.dto.CustomerCreditAccountSaveRequest;
import com.mtravel.platform.customer.credit.entity.CustomerCreditAccountEntity;
import com.mtravel.platform.customer.credit.mapper.CustomerCreditAccountMapper;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerCreditAccountServiceTest {

    @Test
    void createShouldRejectDuplicateCreditAccountForSameCustomer() {
        CustomerCreditAccountMapper creditMapper = mock(CustomerCreditAccountMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCreditAccountService service = new CustomerCreditAccountService(creditMapper, customerMapper);
        CustomerCreditAccountSaveRequest request = new CustomerCreditAccountSaveRequest(
                8L,
                BigDecimal.valueOf(100_000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(10_000),
                "remind",
                "active",
                null
        );

        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(activeCustomer(8L));
        when(creditMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户授信账户已存在");

        verify(creditMapper, never()).insert(any(CustomerCreditAccountEntity.class));
    }

    @Test
    void createShouldSaveManualCreditAmounts() {
        CustomerCreditAccountMapper creditMapper = mock(CustomerCreditAccountMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCreditAccountService service = new CustomerCreditAccountService(creditMapper, customerMapper);
        ArgumentCaptor<CustomerCreditAccountEntity> captor = ArgumentCaptor.forClass(CustomerCreditAccountEntity.class);
        CustomerCreditAccountEntity[] inserted = new CustomerCreditAccountEntity[1];
        CustomerCreditAccountSaveRequest request = new CustomerCreditAccountSaveRequest(
                8L,
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(30_000),
                BigDecimal.valueOf(5_000),
                BigDecimal.valueOf(10_000),
                "approval",
                "active",
                "测试额度"
        );

        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(activeCustomer(8L));
        when(creditMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(creditMapper.insert(any(CustomerCreditAccountEntity.class))).thenAnswer(invocation -> {
            inserted[0] = invocation.getArgument(0);
            inserted[0].setId(11L);
            return 1;
        });
        when(creditMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> inserted[0]);

        service.create(request, 1L, "admin");

        verify(creditMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreditLimit()).isEqualByComparingTo("100000");
        assertThat(captor.getValue().getOccupiedAmount()).isEqualByComparingTo("30000");
        assertThat(captor.getValue().getPendingApprovalAmount()).isEqualByComparingTo("5000");
        assertThat(captor.getValue().getOverLimitAction()).isEqualTo("approval");
    }

    private CustomerUnitEntity activeCustomer(Long id) {
        CustomerUnitEntity entity = new CustomerUnitEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setCustomerName("测试客户");
        entity.setStatus("active");
        entity.setIsDeleted(false);
        return entity;
    }
}
