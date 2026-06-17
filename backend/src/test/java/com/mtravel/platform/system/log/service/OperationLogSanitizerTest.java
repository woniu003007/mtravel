package com.mtravel.platform.system.log.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLogSanitizerTest {

    @Test
    void sanitizeShouldMaskSensitiveFields() {
        String raw = """
                {"username":"demo","password":"123456","accessToken":"abc","nested":{"Authorization":"Bearer token"}}
                """;

        String sanitized = OperationLogSanitizer.sanitize(raw);

        assertThat(sanitized).contains("\"username\":\"demo\"");
        assertThat(sanitized).doesNotContain("123456");
        assertThat(sanitized).doesNotContain("Bearer token");
        assertThat(sanitized).doesNotContain("abc");
        assertThat(sanitized).contains("\"password\":\"******\"");
        assertThat(sanitized).contains("\"accessToken\":\"******\"");
        assertThat(sanitized).contains("\"Authorization\":\"******\"");
    }

    @Test
    void sanitizeShouldTruncateLongText() {
        String sanitized = OperationLogSanitizer.sanitize("x".repeat(5000));

        assertThat(sanitized).hasSizeLessThanOrEqualTo(2000);
        assertThat(sanitized).endsWith("...");
    }

    @Test
    void sanitizeShouldMaskSensitiveFieldsInEscapedJsonText() {
        String raw = "{\"body\":\"{\\\"username\\\":\\\"demo\\\",\\\"password\\\":\\\"123456\\\"}\"}";

        String sanitized = OperationLogSanitizer.sanitize(raw);

        assertThat(sanitized).doesNotContain("123456");
        assertThat(sanitized).contains("\\\"password\\\":\\\"******\\\"");
    }
}
