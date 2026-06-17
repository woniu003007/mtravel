package com.mtravel.platform.system.log.service;

import java.util.regex.Pattern;

public final class OperationLogSanitizer {

    private static final int MAX_LENGTH = 2000;
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(?i)(\"(?:password|token|authorization|accessToken|refreshToken)\"\\s*:\\s*\")([^\"]*)(\")"
    );
    private static final Pattern ESCAPED_SENSITIVE_JSON_FIELD = Pattern.compile(
            "(?i)(\\\\\"(?:password|token|authorization|accessToken|refreshToken)\\\\\"\\s*:\\s*\\\\\")([^\\\\\"]*)(\\\\\")"
    );

    private OperationLogSanitizer() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String sanitized = SENSITIVE_JSON_FIELD.matcher(raw).replaceAll("$1******$3");
        sanitized = ESCAPED_SENSITIVE_JSON_FIELD.matcher(sanitized).replaceAll("$1******$3");
        if (sanitized.length() <= MAX_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_LENGTH - 3) + "...";
    }
}
