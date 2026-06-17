package com.mtravel.platform.customer.unit.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.customer.unit.dto.CustomerUnitCreateRequest;
import com.mtravel.platform.customer.unit.dto.CustomerUnitUpdateRequest;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerUnitServiceTest {

    @Test
    void createShouldRejectDuplicateCustomerCodeInSameTenant() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerUnitService service = new CustomerUnitService(customerMapper, categoryMapper);
        CustomerUnitCreateRequest request = new CustomerUnitCreateRequest(
                "BY-001",
                "杭州百缘测试客户",
                null,
                BigDecimal.valueOf(500_000),
                "浙江省",
                "杭州市",
                "西湖区",
                "销售部",
                "计调A",
                "王经理",
                "13800000000",
                "登记A",
                null,
                "active",
                "测试备注",
                null,
                null,
                null,
                null,
                null
        );

        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户编码已存在");

        verify(customerMapper, never()).insert(any(CustomerUnitEntity.class));
    }

    @Test
    void createShouldRejectDisabledCategory() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerUnitService service = new CustomerUnitService(customerMapper, categoryMapper);
        CustomerUnitCreateRequest request = new CustomerUnitCreateRequest(
                "BY-002",
                "杭州百缘测试客户",
                9L,
                BigDecimal.valueOf(300_000),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                null,
                null,
                null,
                null,
                null,
                null
        );
        CustomerCategoryEntity category = new CustomerCategoryEntity();
        category.setId(9L);
        category.setTenantId(1L);
        category.setCategoryName("停用分类");
        category.setStatus("disabled");
        category.setIsDeleted(false);

        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(category);

        assertThatThrownBy(() -> service.create(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户分类已停用");

        verify(customerMapper, never()).insert(any(CustomerUnitEntity.class));
    }

    @Test
    void createShouldSaveCustomerCreditLimit() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerUnitService service = new CustomerUnitService(customerMapper, categoryMapper);
        ArgumentCaptor<CustomerUnitEntity> entityCaptor = ArgumentCaptor.forClass(CustomerUnitEntity.class);
        CustomerUnitEntity[] insertedEntity = new CustomerUnitEntity[1];
        CustomerUnitCreateRequest request = new CustomerUnitCreateRequest(
                "BY-003",
                "杭州百缘测试客户",
                null,
                BigDecimal.valueOf(600_000),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            insertedEntity[0] = invocation.getArgument(0);
            return 1;
        }).when(customerMapper).insert(any(CustomerUnitEntity.class));
        when(customerMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> insertedEntity[0]);

        service.create(request, 1L, "admin");

        verify(customerMapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getCreditLimit()).isEqualByComparingTo("600000");
    }

    @Test
    void createShouldSaveDepartmentDispatcherAndSettlementFields() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerUnitService service = new CustomerUnitService(customerMapper, categoryMapper);
        ArgumentCaptor<CustomerUnitEntity> entityCaptor = ArgumentCaptor.forClass(CustomerUnitEntity.class);
        CustomerUnitEntity[] insertedEntity = new CustomerUnitEntity[1];
        CustomerUnitCreateRequest request = new CustomerUnitCreateRequest(
                "BY-SETTLE",
                "杭州百缘结款客户",
                null,
                BigDecimal.valueOf(900_000),
                "浙江省",
                "杭州市",
                "西湖区",
                "计调部",
                "计调A",
                "王经理",
                "13800000000",
                "登记A",
                LocalDate.of(2026, 12, 31),
                "active",
                "结款备注",
                11L,
                22L,
                "monthly_2",
                LocalDate.of(2026, 6, 1),
                15
        );

        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            insertedEntity[0] = invocation.getArgument(0);
            insertedEntity[0].setId(100L);
            return 1;
        }).when(customerMapper).insert(any(CustomerUnitEntity.class));
        when(customerMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> insertedEntity[0]);

        service.create(request, 1L, "admin");

        verify(customerMapper).insert(entityCaptor.capture());
        CustomerUnitEntity saved = entityCaptor.getValue();
        assertThat(saved.getDepartmentId()).isEqualTo(11L);
        assertThat(saved.getDispatcherEmployeeId()).isEqualTo(22L);
        assertThat(saved.getSettlementMethod()).isEqualTo("monthly_2");
        assertThat(saved.getBillStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(saved.getBillDay()).isEqualTo(15);
    }

    @Test
    void updateShouldKeepExplicitCustomerCreditLimitIndependentFromCategoryDefault() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerUnitService service = new CustomerUnitService(customerMapper, categoryMapper);
        ArgumentCaptor<CustomerUnitEntity> entityCaptor = ArgumentCaptor.forClass(CustomerUnitEntity.class);
        CustomerUnitEntity[] updatedEntity = new CustomerUnitEntity[1];
        CustomerUnitUpdateRequest request = new CustomerUnitUpdateRequest(
                "BY-004",
                "杭州百缘测试客户",
                9L,
                BigDecimal.valueOf(880_000),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "active",
                null,
                null,
                null,
                null,
                null,
                null
        );
        CustomerCategoryEntity category = new CustomerCategoryEntity();
        category.setId(9L);
        category.setTenantId(1L);
        category.setCategoryName("A类客户");
        category.setDefaultCreditLimit(BigDecimal.valueOf(500_000));
        category.setStatus("active");
        category.setIsDeleted(false);

        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(categoryMapper.selectOne(any(Wrapper.class))).thenReturn(category);
        doAnswer(invocation -> {
            updatedEntity[0] = invocation.getArgument(0);
            return 1;
        }).when(customerMapper).update(any(CustomerUnitEntity.class), any(Wrapper.class));
        when(customerMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            CustomerUnitEntity entity = updatedEntity[0];
            entity.setId(7L);
            entity.setTenantId(1L);
            return entity;
        });

        service.update(7L, request, 1L);

        verify(customerMapper).update(entityCaptor.capture(), any(Wrapper.class));
        assertThat(entityCaptor.getValue().getCreditLimit()).isEqualByComparingTo("880000");
    }

    @Test
    void deleteShouldUseSoftDeleteFields() {
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        CustomerCategoryMapper categoryMapper = mock(CustomerCategoryMapper.class);
        CustomerUnitService service = new CustomerUnitService(customerMapper, categoryMapper);
        ArgumentCaptor<CustomerUnitEntity> entityCaptor = ArgumentCaptor.forClass(CustomerUnitEntity.class);

        when(customerMapper.update(any(CustomerUnitEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(9L, 1L, "admin");

        verify(customerMapper).update(entityCaptor.capture(), any(Wrapper.class));
        CustomerUnitEntity updateEntity = entityCaptor.getValue();
        assertThat(updateEntity.getIsDeleted()).isTrue();
        assertThat(updateEntity.getDeletedBy()).isEqualTo("admin");
        assertThat(updateEntity.getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
