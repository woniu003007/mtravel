package com.mtravel.platform.purchase.relation.price.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.price.dto.SupplierResourcePriceSaveRequest;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
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

class SupplierResourcePriceServiceTest {

    @Test
    void createShouldPersistPricesWhenProjectTypeMatchesRelationResourceType() {
        SupplierResourcePriceMapper mapper = mock(SupplierResourcePriceMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        SupplierResourcePriceService service = new SupplierResourcePriceService(mapper, relationMapper, expenseItemMapper);
        ArgumentCaptor<SupplierResourcePriceEntity> captor = ArgumentCaptor.forClass(SupplierResourcePriceEntity.class);
        SupplierResourcePriceEntity[] inserted = new SupplierResourcePriceEntity[1];

        when(relationMapper.selectOne(any(Wrapper.class))).thenReturn(relation(8L, "scenic"));
        when(expenseItemMapper.selectOne(any(Wrapper.class))).thenReturn(project(20L, "scenic", "成人"));
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.insert(any(SupplierResourcePriceEntity.class))).thenAnswer(invocation -> {
            SupplierResourcePriceEntity entity = invocation.getArgument(0);
            entity.setId(21L);
            inserted[0] = entity;
            return 1;
        });
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> inserted[0]);

        service.create(request(8L, 20L), 1L, "admin");

        verify(mapper).insert(captor.capture());
        SupplierResourcePriceEntity entity = captor.getValue();
        assertThat(entity.getRelationId()).isEqualTo(8L);
        assertThat(entity.getResourceProjectId()).isEqualTo(20L);
        assertThat(entity.getProjectName()).isEqualTo("成人");
        assertThat(entity.getMarketPrice()).isEqualByComparingTo("120.00");
        assertThat(entity.getPeerPrice()).isEqualByComparingTo("90.00");
        assertThat(entity.getTeamPrice()).isEqualByComparingTo("80.00");
        assertThat(entity.getPriceDescription()).isEqualTo("团队成人票");
        assertThat(entity.getStatus()).isEqualTo("active");
        assertThat(entity.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    void createShouldRejectProjectTypeFromDifferentResourceType() {
        SupplierResourcePriceMapper mapper = mock(SupplierResourcePriceMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        EnterpriseExpenseItemMapper expenseItemMapper = mock(EnterpriseExpenseItemMapper.class);
        SupplierResourcePriceService service = new SupplierResourcePriceService(mapper, relationMapper, expenseItemMapper);

        when(relationMapper.selectOne(any(Wrapper.class))).thenReturn(relation(8L, "scenic"));
        when(expenseItemMapper.selectOne(any(Wrapper.class))).thenReturn(project(20L, "hotel", "标间"));

        assertThatThrownBy(() -> service.create(request(8L, 20L), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("项目类型和采购关系资源类型不匹配");

        verify(mapper, never()).insert(any(SupplierResourcePriceEntity.class));
    }

    private SupplierResourcePriceSaveRequest request(Long relationId, Long projectId) {
        return new SupplierResourcePriceSaveRequest(
                relationId,
                projectId,
                new BigDecimal("120.00"),
                new BigDecimal("90.00"),
                new BigDecimal("80.00"),
                "团队成人票",
                "active",
                "备注"
        );
    }

    private PurchaseRelationEntity relation(Long id, String resourceType) {
        PurchaseRelationEntity entity = new PurchaseRelationEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setResourceType(resourceType);
        entity.setResourceName("苏州园林");
        entity.setSupplierId(66L);
        return entity;
    }

    private EnterpriseExpenseItemEntity project(Long id, String resourceType, String projectName) {
        EnterpriseExpenseItemEntity entity = new EnterpriseExpenseItemEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setResourceType(resourceType);
        entity.setProjectName(projectName);
        entity.setStatus("active");
        return entity;
    }
}
