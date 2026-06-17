package com.mtravel.platform.purchase.groundagent.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 地接外委保存请求。 */
public record GroundAgentSaveRequest(
        @NotBlank(message = "地接社名称不能为空") @Size(max = 200) String groundAgentName,
        @Size(max = 80) String city,
        @Size(max = 80) String contactName,
        @Size(max = 40) String contactPhone,
        @Size(max = 200) String taskName,
        String itineraryRequirement,
        @DecimalMin(value = "0.00", message = "总预算不能小于0") BigDecimal totalBudget,
        Long confirmationAttachmentId,
        String confirmationFileUrl,
        @Pattern(regexp = "active|disabled|completed", message = "地接外委状态不合法") String status,
        String remark
) {}
