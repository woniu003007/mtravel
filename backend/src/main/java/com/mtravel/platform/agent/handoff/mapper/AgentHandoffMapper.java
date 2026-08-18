package com.mtravel.platform.agent.handoff.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.agent.handoff.entity.AgentHandoffEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Agent 转人工待办数据访问接口。 */
@Mapper
public interface AgentHandoffMapper extends BaseMapper<AgentHandoffEntity> {

    /** 原子写入待办并返回主键；并发幂等重复时返回 null。 */
    @Select(value = """
            INSERT INTO agent_handoffs (
              tenant_id, handoff_no, service_token_id, idempotency_key, request_hash,
              conversation_id, customer_id, reason_code, priority, summary,
              related_product_id, related_schedule_id, related_team_no, related_quote_request_no,
              assigned_employee_id, assigned_employee_name, assigned_department_name,
              status, created_by, created_at, updated_at, is_deleted
            ) VALUES (
              #{entity.tenantId}, #{entity.handoffNo}, #{entity.serviceTokenId},
              #{entity.idempotencyKey}, #{entity.requestHash}, #{entity.conversationId},
              #{entity.customerId}, #{entity.reasonCode}, #{entity.priority}, #{entity.summary},
              #{entity.relatedProductId}, #{entity.relatedScheduleId}, #{entity.relatedTeamNo},
              #{entity.relatedQuoteRequestNo}, #{entity.assignedEmployeeId},
              #{entity.assignedEmployeeName}, #{entity.assignedDepartmentName}, #{entity.status},
              #{entity.createdBy}, #{entity.createdAt}, #{entity.updatedAt}, false
            )
            ON CONFLICT (tenant_id, service_token_id, idempotency_key)
            WHERE is_deleted = false
            DO NOTHING
            RETURNING id
            """, affectData = true)
    Long insertIdempotentReturningId(@Param("entity") AgentHandoffEntity entity);
}
