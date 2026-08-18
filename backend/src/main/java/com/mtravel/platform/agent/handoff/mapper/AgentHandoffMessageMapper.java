package com.mtravel.platform.agent.handoff.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.agent.handoff.entity.AgentHandoffMessageEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Agent 转人工来源消息批量数据访问接口。 */
@Mapper
public interface AgentHandoffMessageMapper extends BaseMapper<AgentHandoffMessageEntity> {

    /** 单条 SQL 批量写入来源消息，避免逐条访问数据库。 */
    @Insert("""
            <script>
            INSERT INTO agent_handoff_messages (
              tenant_id, handoff_id, message_id, sender_name, sent_at, content,
              created_by, created_at, updated_at, is_deleted
            ) VALUES
            <foreach collection="messages" item="item" separator=",">
              (#{item.tenantId}, #{item.handoffId}, #{item.messageId}, #{item.senderName},
               #{item.sentAt}, #{item.content}, #{item.createdBy}, #{item.createdAt},
               #{item.updatedAt}, false)
            </foreach>
            </script>
            """)
    int insertBatch(@Param("messages") List<AgentHandoffMessageEntity> messages);
}
