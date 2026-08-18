package com.mtravel.platform.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** OpenAPI 必须区分后台用户 JWT 和 Agent 服务令牌。 */
class AgentOpenApiSecurityTest {

    @Test
    void openApiShouldDeclareDedicatedAgentServiceTokenScheme() {
        var openApi = new OpenApiConfig().mtravelOpenAPI();

        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKeys("bearerAuth", "agentServiceToken");
        assertThat(openApi.getComponents().getSecuritySchemes().get("agentServiceToken").getDescription())
                .contains("Agent");
    }
}
