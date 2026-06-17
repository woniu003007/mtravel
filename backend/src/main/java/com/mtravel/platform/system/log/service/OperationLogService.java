package com.mtravel.platform.system.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.system.log.dto.OperationLogRecordCommand;
import com.mtravel.platform.system.log.dto.OperationLogResponse;
import com.mtravel.platform.system.log.entity.OperationLogEntity;
import com.mtravel.platform.system.log.mapper.OperationLogMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(OperationLogRecordCommand command) {
        try {
            OperationLogEntity entity = new OperationLogEntity();
            entity.setTenantId(command.tenantId());
            entity.setOperatorId(command.operatorId());
            entity.setOperatorName(defaultText(command.operatorName(), "anonymous"));
            entity.setModuleName(defaultText(command.moduleName(), "系统"));
            entity.setOperationType(defaultText(command.operationType(), "其他"));
            entity.setRequestPath(defaultText(command.requestPath(), ""));
            entity.setRequestMethod(defaultText(command.requestMethod(), "GET"));
            entity.setRequestParams(OperationLogSanitizer.sanitize(command.requestParams()));
            entity.setIpAddress(limit(defaultText(command.ipAddress(), ""), 80));
            entity.setUserAgent(limit(defaultText(command.userAgent(), ""), 500));
            entity.setSuccess(command.success());
            entity.setDurationMs(command.durationMs() == null ? 0L : command.durationMs());
            entity.setErrorMessage(limit(OperationLogSanitizer.sanitize(command.errorMessage()), 500));
            mapper.insert(entity);
        } catch (Exception ex) {
            log.warn("record operation log failed: {}", ex.getMessage());
        }
    }

    public PageResult<OperationLogResponse> page(
            Long tenantId,
            String operatorName,
            String moduleName,
            String requestPath,
            Boolean success,
            long page,
            long pageSize
    ) {
        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<OperationLogEntity>()
                .eq(OperationLogEntity::getTenantId, tenantId)
                .like(StringUtils.hasText(operatorName), OperationLogEntity::getOperatorName, operatorName)
                .eq(StringUtils.hasText(moduleName), OperationLogEntity::getModuleName, moduleName)
                .like(StringUtils.hasText(requestPath), OperationLogEntity::getRequestPath, requestPath)
                .eq(success != null, OperationLogEntity::getSuccess, success)
                .orderByDesc(OperationLogEntity::getCreatedAt)
                .orderByDesc(OperationLogEntity::getId);

        Page<OperationLogEntity> result = mapper.selectPage(Page.of(page, pageSize), wrapper);
        List<OperationLogResponse> items = result.getRecords().stream()
                .map(OperationLogResponse::fromEntity)
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
