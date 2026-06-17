package com.mtravel.platform.system.log.service;

import com.mtravel.platform.system.log.dto.OperationLogRecordCommand;
import com.mtravel.platform.system.log.entity.OperationLogEntity;
import com.mtravel.platform.system.log.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OperationLogServiceTest {

    @Test
    void recordShouldPersistTenantOperatorAndSanitizedParams() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        OperationLogService service = new OperationLogService(mapper);

        service.record(new OperationLogRecordCommand(
                1L,
                9L,
                "demo01",
                "客户管理",
                "新增",
                "/customer/category/create",
                "POST",
                "{\"categoryName\":\"A类客户\",\"token\":\"secret\"}",
                "127.0.0.1",
                "Chrome",
                true,
                35L,
                null
        ));

        ArgumentCaptor<OperationLogEntity> captor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(mapper).insert(captor.capture());
        OperationLogEntity entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getOperatorId()).isEqualTo(9L);
        assertThat(entity.getOperatorName()).isEqualTo("demo01");
        assertThat(entity.getModuleName()).isEqualTo("客户管理");
        assertThat(entity.getOperationType()).isEqualTo("新增");
        assertThat(entity.getRequestPath()).isEqualTo("/customer/category/create");
        assertThat(entity.getRequestMethod()).isEqualTo("POST");
        assertThat(entity.getRequestParams()).contains("A类客户");
        assertThat(entity.getRequestParams()).doesNotContain("secret");
        assertThat(entity.getSuccess()).isTrue();
        assertThat(entity.getDurationMs()).isEqualTo(35L);
    }

    @Test
    void recordShouldNotThrowWhenMapperFails() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(mapper)
                .insert(any(OperationLogEntity.class));
        OperationLogService service = new OperationLogService(mapper);

        service.record(new OperationLogRecordCommand(
                1L,
                null,
                "system",
                "系统",
                "查询",
                "/actuator/health",
                "GET",
                "",
                "127.0.0.1",
                "curl",
                false,
                1L,
                "error"
        ));
    }
}
