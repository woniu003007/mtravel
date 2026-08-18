package com.mtravel.platform.sales.team.documentimport.dto;

import jakarta.validation.constraints.NotNull;

/** 创建团队文档智能代录任务请求。 */
public record TeamDocumentImportTaskCreateRequest(
        @NotNull(message = "请先上传 Word 文件") Long attachmentId,
        Long targetTeamId
) {}
