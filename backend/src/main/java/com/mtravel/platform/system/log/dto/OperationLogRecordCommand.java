package com.mtravel.platform.system.log.dto;

public record OperationLogRecordCommand(
        Long tenantId,
        Long operatorId,
        String operatorName,
        String moduleName,
        String operationType,
        String requestPath,
        String requestMethod,
        String requestParams,
        String ipAddress,
        String userAgent,
        boolean success,
        Long durationMs,
        String errorMessage
) {
}
