package com.mtravel.platform.sales.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.team.entity.SalesTeamEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 销售团队主表 Mapper。
 *
 * <p>只负责 sales_teams 表访问，团期生成、状态流转和删除规则由 Service 处理。</p>
 */
@Mapper
public interface SalesTeamMapper extends BaseMapper<SalesTeamEntity> {

    /**
     * 获取团队编号生成的事务级数据库锁。
     *
     * <p>同一租户、同一团号基础前缀的 A/B/C 后缀生成必须串行化。</p>
     *
     * @return PostgreSQL void 锁函数的文本占位结果，业务层只依赖加锁副作用
     */
    @Select("""
            SELECT pg_advisory_xact_lock(hashtextextended(CONCAT('sales_team_no:', #{tenantId}, ':', #{base}), 0))::text
            """)
    String lockTeamNoGeneration(@Param("tenantId") Long tenantId, @Param("base") String base);
}
