package com.mtravel.platform.agent;

import com.mtravel.platform.agent.customer.dto.AgentCustomerApi;
import com.mtravel.platform.agent.handoff.dto.AgentHandoffApi;
import com.mtravel.platform.agent.policy.dto.AgentPolicyApi;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.agent.quote.dto.AgentQuoteApi;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Agent 专用 DTO 禁用敏感字段名泄漏扫描。 */
class AgentResponseLeakageTest {

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "purchaseprice", "costamount", "creditlimit", "settlementtype", "settlementmethod",
            "suppliercontact", "contactphone", "mobilephone", "email", "certificateNo",
            "profit", "grossprofit", "commission", "internalremark", "approvedby"
    ).stream().map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());

    @Test
    void agentProtocolRecordsShouldNotDeclareForbiddenInternalFields() {
        Set<String> fieldNames = Arrays.stream(new Class<?>[] {
                        AgentCustomerApi.class,
                        AgentProductApi.class,
                        AgentPolicyApi.class,
                        AgentQuoteApi.class,
                        AgentHandoffApi.class
                })
                .flatMap(container -> Arrays.stream(container.getDeclaredClasses()))
                .filter(Class::isRecord)
                .flatMap(type -> Arrays.stream(type.getRecordComponents()))
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        assertThat(fieldNames).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);
    }
}
