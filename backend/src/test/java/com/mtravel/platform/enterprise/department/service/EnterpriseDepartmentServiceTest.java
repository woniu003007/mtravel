package com.mtravel.platform.enterprise.department.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.enterprise.department.dto.EnterpriseDepartmentSaveRequest;
import com.mtravel.platform.enterprise.department.entity.EnterpriseDepartmentEntity;
import com.mtravel.platform.enterprise.department.mapper.EnterpriseDepartmentMapper;
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

class EnterpriseDepartmentServiceTest {

    @Test
    void createShouldRejectDuplicateDepartmentName() {
        EnterpriseDepartmentMapper mapper = mock(EnterpriseDepartmentMapper.class);
        EnterpriseDepartmentService service = new EnterpriseDepartmentService(mapper);
        EnterpriseDepartmentSaveRequest request = request(null, "OP", "计调部");

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(request, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("部门名称已存在");

        verify(mapper, never()).insert(any(EnterpriseDepartmentEntity.class));
    }

    @Test
    void createShouldPersistDepartmentFields() {
        EnterpriseDepartmentMapper mapper = mock(EnterpriseDepartmentMapper.class);
        EnterpriseDepartmentService service = new EnterpriseDepartmentService(mapper);
        EnterpriseDepartmentSaveRequest request = request(null, "SALES", "销售部");
        ArgumentCaptor<EnterpriseDepartmentEntity> captor = ArgumentCaptor.forClass(EnterpriseDepartmentEntity.class);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer((Answer<Integer>) invocation -> {
            EnterpriseDepartmentEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return 1;
        }).when(mapper).insert(any(EnterpriseDepartmentEntity.class));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            verify(mapper).insert(captor.capture());
            EnterpriseDepartmentEntity entity = captor.getValue();
            entity.setTenantId(1L);
            return entity;
        });

        service.create(request, 1L, "admin");

        EnterpriseDepartmentEntity entity = captor.getValue();
        assertThat(entity.getDepartmentCode()).isEqualTo("SALES");
        assertThat(entity.getDepartmentName()).isEqualTo("销售部");
        assertThat(entity.getManagerName()).isEqualTo("王经理");
        assertThat(entity.getContactPhone()).isEqualTo("13800000000");
        assertThat(entity.getSortOrder()).isEqualTo(10);
        assertThat(entity.getStatus()).isEqualTo("active");
        assertThat(entity.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    void updateShouldRejectSelfAsParent() {
        EnterpriseDepartmentMapper mapper = mock(EnterpriseDepartmentMapper.class);
        EnterpriseDepartmentService service = new EnterpriseDepartmentService(mapper);
        EnterpriseDepartmentSaveRequest request = request(9L, "FIN", "财务部");

        assertThatThrownBy(() -> service.update(9L, request, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("上级部门不能选择自己");
    }

    @Test
    void updateShouldRejectChildAsParent() {
        EnterpriseDepartmentMapper mapper = mock(EnterpriseDepartmentMapper.class);
        EnterpriseDepartmentService service = new EnterpriseDepartmentService(mapper);
        EnterpriseDepartmentSaveRequest request = request(12L, "FIN", "财务部");
        EnterpriseDepartmentEntity child = new EnterpriseDepartmentEntity();
        child.setId(12L);
        child.setTenantId(1L);
        child.setParentId(9L);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(child);

        assertThatThrownBy(() -> service.update(9L, request, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("上级部门不能选择自己的下级部门");
    }

    @Test
    void deleteShouldRejectDepartmentWithChildren() {
        EnterpriseDepartmentMapper mapper = mock(EnterpriseDepartmentMapper.class);
        EnterpriseDepartmentService service = new EnterpriseDepartmentService(mapper);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(9L, 1L, "admin"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请先处理下级部门");

        verify(mapper, never()).update(any(EnterpriseDepartmentEntity.class), any(Wrapper.class));
    }

    @Test
    void deleteShouldSoftDeleteDepartmentWithoutChildren() {
        EnterpriseDepartmentMapper mapper = mock(EnterpriseDepartmentMapper.class);
        EnterpriseDepartmentService service = new EnterpriseDepartmentService(mapper);
        ArgumentCaptor<EnterpriseDepartmentEntity> captor = ArgumentCaptor.forClass(EnterpriseDepartmentEntity.class);

        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapper.update(any(EnterpriseDepartmentEntity.class), any(Wrapper.class))).thenReturn(1);

        service.delete(9L, 1L, "admin");

        verify(mapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    private EnterpriseDepartmentSaveRequest request(Long parentId, String code, String name) {
        return new EnterpriseDepartmentSaveRequest(
                parentId,
                code,
                name,
                "王经理",
                "13800000000",
                10,
                "active",
                "企业组织架构部门"
        );
    }
}
