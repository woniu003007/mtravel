package com.mtravel.platform.purchase.supplier.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import com.mtravel.platform.purchase.supplier.dto.SupplierSaveRequest;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierServiceTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void supplierCategoryShouldMatchOldSystemOptions() {
        Set<String> validCategories = Set.of(
                "scenic", "hotel", "restaurant", "vehicle", "traffic",
                "other", "ground_agent", "shopping", "common"
        );

        for (String category : validCategories) {
            assertThat(validator.validate(request("供应商-" + category, category)))
                    .as("分类 %s 应允许保存", category)
                    .isEmpty();
        }

        assertThat(validator.validate(request("导游不属于供应商分类", "guide")))
                .as("导游应走企业资料 / 导游管理，不作为供应商分类")
                .isNotEmpty();
        assertThat(validator.validate(request("票务不属于老系统供应商分类", "ticket")))
                .as("老系统供应商分类没有票务，票务能力走景区或票务系统下单")
                .isNotEmpty();
    }

    @Test
    void createShouldRejectDuplicateSupplierName() {
        SupplierMapper mapper = mock(SupplierMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        SupplierService service = new SupplierService(mapper, customerMapper);
        SupplierSaveRequest request = request("杭州测试酒店", "hotel");

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("供应商名称已存在");

        verify(mapper, never()).insert(any(SupplierEntity.class));
    }

    @Test
    void deleteShouldSoftDeleteSupplier() {
        SupplierMapper mapper = mock(SupplierMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        SupplierService service = new SupplierService(mapper, customerMapper);
        ArgumentCaptor<SupplierEntity> captor = ArgumentCaptor.forClass(SupplierEntity.class);

        when(mapper.update(any(SupplierEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(9L, 1L, "admin");

        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    void createShouldSaveOldSystemBuyerAndFaxFields() {
        SupplierMapper mapper = mock(SupplierMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        SupplierService service = new SupplierService(mapper, customerMapper);
        ArgumentCaptor<SupplierEntity> captor = ArgumentCaptor.forClass(SupplierEntity.class);
        CustomerUnitEntity buyer = new CustomerUnitEntity();
        buyer.setId(3847L);
        buyer.setTenantId(1L);
        buyer.setCustomerName("之江饭店工会");
        SupplierEntity[] insertedEntity = new SupplierEntity[1];

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(buyer);
        when(mapper.insert(any(SupplierEntity.class))).thenAnswer(invocation -> {
            SupplierEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            insertedEntity[0] = entity;
            return 1;
        });
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> insertedEntity[0]);

        service.create(request("之江饭店", "hotel"), 1L, "admin");

        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getBuyerId()).isEqualTo(3847L);
        assertThat(captor.getValue().getFaxNumber()).isEqualTo("0571-88888888");
        assertThat(captor.getValue().getOfficeAddress()).isEqualTo("杭州市西湖区测试路 1 号");
    }

    @Test
    void createShouldRejectMissingBuyer() {
        SupplierMapper mapper = mock(SupplierMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        SupplierService service = new SupplierService(mapper, customerMapper);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.create(request("之江饭店", "hotel"), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("关联采购商不存在或已删除");

        verify(mapper, never()).insert(any(SupplierEntity.class));
    }

    @Test
    void createShouldDefaultEmptyCategoryToCommon() {
        SupplierMapper mapper = mock(SupplierMapper.class);
        CustomerUnitMapper customerMapper = mock(CustomerUnitMapper.class);
        SupplierService service = new SupplierService(mapper, customerMapper);
        ArgumentCaptor<SupplierEntity> captor = ArgumentCaptor.forClass(SupplierEntity.class);
        SupplierEntity[] insertedEntity = new SupplierEntity[1];

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(customerMapper.selectOne(any(Wrapper.class))).thenReturn(activeBuyer());
        when(mapper.insert(any(SupplierEntity.class))).thenAnswer(invocation -> {
            SupplierEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            insertedEntity[0] = entity;
            return 1;
        });
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> insertedEntity[0]);

        service.create(request("未指定分类供应商", null), 1L, "admin");

        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getSupplierCategory()).isEqualTo("common");
    }

    private CustomerUnitEntity activeBuyer() {
        CustomerUnitEntity buyer = new CustomerUnitEntity();
        buyer.setId(3847L);
        buyer.setTenantId(1L);
        buyer.setCustomerName("之江饭店工会");
        return buyer;
    }

    private SupplierSaveRequest request(String name, String category) {
        return new SupplierSaveRequest(
                "SUP-001",
                name,
                category,
                3847L,
                "浙江省",
                "杭州市",
                "西湖区",
                "月结",
                "张经理",
                "13800000000",
                "0571-88888888",
                "杭州市西湖区测试路 1 号",
                "年度合作协议",
                4,
                "active",
                null
        );
    }
}
