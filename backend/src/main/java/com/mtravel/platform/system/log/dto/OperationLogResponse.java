package com.mtravel.platform.system.log.dto;

import com.mtravel.platform.system.log.entity.OperationLogEntity;
import java.time.OffsetDateTime;

public record OperationLogResponse(
        Long id,
        String operatorName,
        String moduleName,
        String operationType,
        String requestPath,
        String requestMethod,
        String requestParams,
        String ipAddress,
        Boolean success,
        Long durationMs,
        String errorMessage,
        OffsetDateTime createdAt
) {
    public static OperationLogResponse fromEntity(OperationLogEntity entity) {
        return new OperationLogResponse(
                entity.getId(),
                entity.getOperatorName(),
                entity.getModuleName(),
                entity.getOperationType(),
                entity.getRequestPath(),
                entity.getRequestMethod(),
                entity.getRequestParams(),
                entity.getIpAddress(),
                entity.getSuccess(),
                entity.getDurationMs(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }
}
