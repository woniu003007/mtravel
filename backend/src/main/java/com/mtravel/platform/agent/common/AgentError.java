package com.mtravel.platform.agent.common;

import java.util.Map;

/** Agent 接口稳定错误对象。 */
public record AgentError(
        String type,
        boolean retryable,
        Map<String, Object> details
) {
}
