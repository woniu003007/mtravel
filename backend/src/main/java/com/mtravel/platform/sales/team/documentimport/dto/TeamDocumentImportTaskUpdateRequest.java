package com.mtravel.platform.sales.team.documentimport.dto;

import jakarta.validation.constraints.NotNull;

/** 保存计调已修改的团队文档导入草稿请求。 */
public record TeamDocumentImportTaskUpdateRequest(@NotNull TeamDocumentImportDraft draft) {}
