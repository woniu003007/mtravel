package com.mtravel.platform.sales.team.documentimport.dto;

import jakarta.validation.constraints.NotNull;

/** 团队保存成功后应用已确认订单、游客和资源安排的请求。 */
public record TeamDocumentImportApplyRequest(
        @NotNull(message = "团队ID不能为空") Long teamId,
        Boolean applyGuests,
        Boolean applyArrangements
) {}
