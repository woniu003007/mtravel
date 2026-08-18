package com.mtravel.platform.agent.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.agent.handoff.dto.AgentHandoffApi;
import com.mtravel.platform.agent.product.dto.AgentProductApi;
import com.mtravel.platform.agent.quote.dto.AgentQuoteApi;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Agent 请求对象未知字段严格拒绝测试。 */
class AgentStrictJsonRequestBodyAdviceTest {

    private final AgentStrictJsonRequestBodyAdvice advice =
            new AgentStrictJsonRequestBodyAdvice(new ObjectMapper());

    @Test
    void handoffRelatedShouldRejectUnknownAssigneeField() {
        String json = """
                {
                  "conversationId":"conv-1",
                  "customerId":13,
                  "reasonCode":"policy_review",
                  "priority":"normal",
                  "summary":"需人工处理",
                  "sourceMessages":[{
                    "messageId":"m-1","senderName":"孙经理",
                    "sentAt":"2026-07-10T16:40:00+08:00","content":"请确认"
                  }],
                  "related":{"productId":32,"assigneeId":9}
                }
                """;

        assertThatThrownBy(() -> read(json, AgentHandoffApi.CreateRequest.class))
                .isInstanceOfSatisfying(AgentException.class, error ->
                        assertThat(error.errorType()).isEqualTo("VALIDATION_FAILED")
                );
    }

    @Test
    void quoteRootShouldRejectUnknownFinalPriceField() {
        String json = """
                {
                  "conversationId":"conv-1","customerId":13,"quoteType":"other",
                  "sourceMessage":"请询价","requirements":{"notes":"请人工确认"},
                  "finalPrice":"1.00"
                }
                """;

        assertThatThrownBy(() -> read(json, AgentQuoteApi.CreateRequest.class))
                .isInstanceOf(AgentException.class);
    }

    @Test
    void validProductSearchBodyShouldRemainReadable() throws Exception {
        String json = "{\"customerId\":13,\"party\":{\"adults\":2}}";

        var wrapped = read(json, AgentProductApi.SearchRequest.class);

        assertThat(new String(wrapped.getBody().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    private org.springframework.http.HttpInputMessage read(String json, Class<?> targetType) throws Exception {
        MockHttpInputMessage input = new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8));
        return advice.beforeBodyRead(input, null, targetType, null);
    }
}
