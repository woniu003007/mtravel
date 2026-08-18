package com.mtravel.platform.sales.team.documentimport.dto;

import java.util.List;

/** 团队文档草稿应用结果。 */
public record TeamDocumentImportApplyResponse(
        Long teamId, Long orderId, int guestCount, List<Long> arrangementIds, boolean alreadyApplied
) {}
