package com.mtravel.platform.customer.category.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.category.dto.CustomerCategoryCreateRequest;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import java.math.BigDecimal;
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

class CustomerCategoryServiceTest {

    @Test
    void createShouldRejectDuplicateNameInSameTenant() {
        CustomerCategoryMapper mapper = mock(CustomerCategoryMapper.class);
        CustomerCategoryService service = new CustomerCategoryService(mapper);
        CustomerCategoryCreateRequest request = new CustomerCategoryCreateRequest(
                "A类客户",
                BigDecimal.valueOf(500_000),
                10,
                "active",
                "重要客户"
        );

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("客户分类名称已存在");

        verify(mapper, never()).insert(any(CustomerCategoryEntity.class));
    }

    @Test
    void createShouldSaveDefaultCreditLimit() {
        CustomerCategoryMapper mapper = mock(CustomerCategoryMapper.class);
        CustomerCategoryService service = new CustomerCategoryService(mapper);
        ArgumentCaptor<CustomerCategoryEntity> entityCaptor = ArgumentCaptor.forClass(CustomerCategoryEntity.class);
        CustomerCategoryEntity[] insertedEntity = new CustomerCategoryEntity[1];
        CustomerCategoryCreateRequest request = new CustomerCategoryCreateRequest(
                "A类客户",
                BigDecimal.valueOf(800_000),
                10,
                "active",
                "重点客户"
        );

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            insertedEntity[0] = invocation.getArgument(0);
            return 1;
        }).when(mapper).insert(any(CustomerCategoryEntity.class));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> insertedEntity[0]);

        service.create(request, 1L, "admin");

        verify(mapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getDefaultCreditLimit()).isEqualByComparingTo("800000");
    }

    @Test
    void createShouldDefaultEmptyCreditLimitToZero() {
        CustomerCategoryMapper mapper = mock(CustomerCategoryMapper.class);
        CustomerCategoryService service = new CustomerCategoryService(mapper);
        ArgumentCaptor<CustomerCategoryEntity> entityCaptor = ArgumentCaptor.forClass(CustomerCategoryEntity.class);
        CustomerCategoryEntity[] insertedEntity = new CustomerCategoryEntity[1];
        CustomerCategoryCreateRequest request = new CustomerCategoryCreateRequest(
                "B类客户",
                null,
                20,
                "active",
                null
        );

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            insertedEntity[0] = invocation.getArgument(0);
            return 1;
        }).when(mapper).insert(any(CustomerCategoryEntity.class));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> insertedEntity[0]);

        service.create(request, 1L, "admin");

        verify(mapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getDefaultCreditLimit()).isEqualByComparingTo("0");
    }

    @Test
    void deleteShouldUseSoftDeleteFields() {
        CustomerCategoryMapper mapper = mock(CustomerCategoryMapper.class);
        CustomerCategoryService service = new CustomerCategoryService(mapper);
        ArgumentCaptor<CustomerCategoryEntity> entityCaptor = ArgumentCaptor.forClass(CustomerCategoryEntity.class);

        when(mapper.update(any(CustomerCategoryEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(9L, 1L, "admin");

        verify(mapper).update(entityCaptor.capture(), any(Wrapper.class));
        CustomerCategoryEntity updateEntity = entityCaptor.getValue();
        assertThat(updateEntity.getIsDeleted()).isTrue();
        assertThat(updateEntity.getDeletedBy()).isEqualTo("admin");
        assertThat(updateEntity.getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
