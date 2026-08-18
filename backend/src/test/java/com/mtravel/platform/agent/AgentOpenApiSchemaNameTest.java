package com.mtravel.platform.agent;

import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.handoff.dto.AgentHandoffApi;
import com.mtravel.platform.agent.policy.dto.AgentPolicyApi;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.agent.quote.dto.AgentQuoteApi;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Agent 嵌套 DTO 必须声明唯一 OpenAPI Schema 名，避免同名记录覆盖。 */
class AgentOpenApiSchemaNameTest {

    @Test
    void everyAgentProtocolRecordShouldHaveUniqueExplicitSchemaName() {
        Set<String> names = new HashSet<>();
        Arrays.stream(new Class<?>[] {
                        AgentCustomerApi.class,
                        AgentProductApi.class,
                        AgentPolicyApi.class,
                        AgentQuoteApi.class,
                        AgentHandoffApi.class
                })
                .flatMap(container -> Arrays.stream(container.getDeclaredClasses()))
                .filter(Class::isRecord)
                .forEach(type -> {
                    Schema schema = type.getAnnotation(Schema.class);
                    assertThat(schema)
                            .as("%s must declare @Schema(name=...)", type.getName())
                            .isNotNull();
                    assertThat(schema.name()).isNotBlank();
                    assertThat(names.add(schema.name()))
                            .as("OpenAPI schema name %s must be unique", schema.name())
                            .isTrue();
                });
    }
}
