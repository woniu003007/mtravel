package com.mtravel.platform.enterprise.expenseitem.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.expenseitem.dto.EnterpriseExpenseItemSaveRequest;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseExpenseItemServiceTest {

    @Test
    void createShouldPersistProjectForResourceType() {
        EnterpriseExpenseItemMapper mapper = mock(EnterpriseExpenseItemMapper.class);
        EnterpriseExpenseItemService service = new EnterpriseExpenseItemService(mapper);
        ArgumentCaptor<EnterpriseExpenseItemEntity> captor = ArgumentCaptor.forClass(EnterpriseExpenseItemEntity.class);
        EnterpriseExpenseItemEntity[] inserted = new EnterpriseExpenseItemEntity[1];

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer((Answer<Integer>) invocation -> {
            EnterpriseExpenseItemEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            inserted[0] = entity;
            return 1;
        }).when(mapper).insert(any(EnterpriseExpenseItemEntity.class));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> inserted[0]);

        service.create(request("scenic", "成人", true), 1L, "admin");

        verify(mapper).insert(captor.capture());
        EnterpriseExpenseItemEntity entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getResourceType()).isEqualTo("scenic");
        assertThat(entity.getProjectName()).isEqualTo("成人");
        assertThat(entity.getStatisticsEnabled()).isTrue();
        assertThat(entity.getSortOrder()).isEqualTo(10);
        assertThat(entity.getStatus()).isEqualTo("active");
        assertThat(entity.getCreatedBy()).isEqualTo("admin");
        assertThat(entity.getIsDeleted()).isFalse();
    }

    @Test
    void createShouldRejectDuplicateProjectNameInSameResourceType() {
        EnterpriseExpenseItemMapper mapper = mock(EnterpriseExpenseItemMapper.class);
        EnterpriseExpenseItemService service = new EnterpriseExpenseItemService(mapper);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request("hotel", "标间", true), 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("费用项目已存在");

        verify(mapper, never()).insert(any(EnterpriseExpenseItemEntity.class));
    }

    @Test
    void deleteShouldSoftDeleteExpenseItem() {
        EnterpriseExpenseItemMapper mapper = mock(EnterpriseExpenseItemMapper.class);
        EnterpriseExpenseItemService service = new EnterpriseExpenseItemService(mapper);
        ArgumentCaptor<EnterpriseExpenseItemEntity> captor = ArgumentCaptor.forClass(EnterpriseExpenseItemEntity.class);

        when(mapper.update(any(EnterpriseExpenseItemEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(9L, 1L, "admin");

        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    private EnterpriseExpenseItemSaveRequest request(String resourceType, String projectName, Boolean statisticsEnabled) {
        return new EnterpriseExpenseItemSaveRequest(
                resourceType,
                projectName,
                statisticsEnabled,
                10,
                "active",
                "用于采购价格管理"
        );
    }
}
