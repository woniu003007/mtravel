package com.mtravel.platform.sales.team.documentimport.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** 团队文档智能代录任务查询响应。 */
public record TeamDocumentImportTaskResponse(
        Long id, Long attachmentId, Long targetTeamId, Long appliedTeamId, String sourceType,
        String documentType, String status, Integer progressPercent, TeamDocumentImportDraft draft,
        List<String> warnings, String errorMessage, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {}
