package com.mtravel.platform.agent;

import com.mtravel.platform.common.ApiResponse;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Agent P0 接口合同测试。
 *
 * <p>Agent 接口必须使用独立路径和响应对象，同时锁定旧 ApiResponse 协议，防止接入服务调用方时
 * 破坏现有管理后台接口。</p>
 */
class AgentApiContractTest {

    @Test
    void shouldExposeAllP0RoutesUnderIsolatedAgentPrefix() throws Exception {
        assertRoute(
                "com.mtravel.platform.agent.customer.controller.AgentCustomerController",
                "serviceContext",
                GetMapping.class,
                "/customers/{customerId}/service-context"
        );
        assertRoute(
                "com.mtravel.platform.agent.product.controller.AgentProductController",
                "search",
                PostMapping.class,
                "/products/search"
        );
        assertRoute(
                "com.mtravel.platform.agent.product.controller.AgentProductController",
                "detail",
                GetMapping.class,
                "/products/{productId}"
        );
        assertRoute(
                "com.mtravel.platform.agent.product.controller.AgentProductController",
                "schedules",
                GetMapping.class,
                "/products/{productId}/schedules"
        );
        assertRoute(
                "com.mtravel.platform.agent.policy.controller.AgentPolicyController",
                "search",
                GetMapping.class,
                "/policies/search"
        );
        assertRoute(
                "com.mtravel.platform.agent.quote.controller.AgentQuoteRequestController",
                "create",
                PostMapping.class,
                "/quote-requests"
        );
        assertRoute(
                "com.mtravel.platform.agent.quote.controller.AgentQuoteRequestController",
                "detail",
                GetMapping.class,
                "/quote-requests/{quoteRequestId}"
        );
        assertRoute(
                "com.mtravel.platform.agent.handoff.controller.AgentHandoffController",
                "create",
                PostMapping.class,
                "/handoffs"
        );
    }

    @Test
    void shouldKeepExistingApiResponseContractUnchanged() {
        List<String> components = Arrays.stream(ApiResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(components).containsExactly("code", "data", "error", "message");
    }

    private void assertRoute(
            String className,
            String methodName,
            Class<?> mappingType,
            String expectedPath
    ) throws Exception {
        Class<?> controllerClass = Class.forName(className);
        RequestMapping root = controllerClass.getAnnotation(RequestMapping.class);
        assertThat(root).isNotNull();
        assertThat(root.value()).containsExactly("/agent/v1");
        Method method = Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        if (mappingType == GetMapping.class) {
            assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly(expectedPath);
        } else {
            assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly(expectedPath);
        }
    }
}
