package com.mtravel.platform.system.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import org.apache.ibatis.annotations.Insert;

public interface SystemConfigMapper extends BaseMapper<SystemConfigEntity> {

    @Insert("""
            INSERT INTO system_configs (tenant_id, config_key, config_value, remark)
            VALUES (#{tenantId}, #{configKey}, #{configValue}, #{remark})
            ON CONFLICT (tenant_id, config_key)
            DO UPDATE SET
              config_value = EXCLUDED.config_value,
              remark = EXCLUDED.remark,
              updated_at = now()
            """)
    int upsert(SystemConfigEntity entity);
}
