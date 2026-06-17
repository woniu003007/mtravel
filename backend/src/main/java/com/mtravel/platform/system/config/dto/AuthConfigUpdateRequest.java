package com.mtravel.platform.system.config.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AuthConfigUpdateRequest(
        @NotNull
        @Min(5)
        @Max(1440)
        Integer idleTimeoutMinutes
) {
}
