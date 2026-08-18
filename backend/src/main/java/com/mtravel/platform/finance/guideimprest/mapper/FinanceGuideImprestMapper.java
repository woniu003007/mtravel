package com.mtravel.platform.finance.guideimprest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.finance.guideimprest.entity.FinanceGuideImprestEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 导游备用金申请主表 Mapper。
 */
@Mapper
public interface FinanceGuideImprestMapper extends BaseMapper<FinanceGuideImprestEntity> {

    /**
     * 获取导游备用金申请编号生成的事务级数据库锁。
     *
     * @return PostgreSQL void 锁函数的文本占位结果，业务层只依赖加锁副作用
     */
    @Select("""
            SELECT pg_advisory_xact_lock(hashtextextended(CONCAT('finance_guide_imprest_no:', #{tenantId}, ':', #{prefix}), 0))::text
            """)
    String lockRequestNoGeneration(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);

    /**
     * 查询当前备用金申请编号前缀下的最大数字后缀。
     */
    @Select("""
            SELECT COALESCE(MAX(CAST(SUBSTRING(request_no FROM LENGTH(#{prefix}) + 1) AS integer)), 0)
            FROM finance_guide_imprests
            WHERE tenant_id = #{tenantId}
              AND is_deleted = false
              AND request_no LIKE CONCAT(#{prefix}, '%')
              AND SUBSTRING(request_no FROM LENGTH(#{prefix}) + 1) ~ '^[0-9]+$'
            """)
    Integer maxRequestNoSuffix(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);
}
