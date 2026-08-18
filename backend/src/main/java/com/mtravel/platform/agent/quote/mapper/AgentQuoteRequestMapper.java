package com.mtravel.platform.agent.quote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.agent.quote.entity.AgentQuoteRequestEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/** Agent 询价任务数据访问接口。 */
@Mapper
public interface AgentQuoteRequestMapper extends BaseMapper<AgentQuoteRequestEntity> {

    /** 依靠数据库唯一索引原子写入，并发重复时返回 0。 */
    @Insert("""
            INSERT INTO agent_quote_requests (
              tenant_id, request_no, service_token_id, idempotency_key, request_hash,
              conversation_id, customer_id, quote_type, source_message, requirements_json,
              related_product_id, related_schedule_id, assigned_employee_id,
              assigned_employee_name, assigned_department_name, status, customer_visible,
              currency, created_by, created_at, updated_at, is_deleted
            ) VALUES (
              #{tenantId}, #{requestNo}, #{serviceTokenId}, #{idempotencyKey}, #{requestHash},
              #{conversationId}, #{customerId}, #{quoteType}, #{sourceMessage}, #{requirementsJson},
              #{relatedProductId}, #{relatedScheduleId}, #{assignedEmployeeId},
              #{assignedEmployeeName}, #{assignedDepartmentName}, #{status}, #{customerVisible},
              #{currency}, #{createdBy}, #{createdAt}, #{updatedAt}, false
            )
            ON CONFLICT (tenant_id, service_token_id, idempotency_key)
            WHERE is_deleted = false
            DO NOTHING
            """)
    int insertIdempotent(AgentQuoteRequestEntity entity);
}
