package com.mtravel.platform.sales.booking.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 收客订单主表 Mapper。
 *
 * <p>除 BaseMapper 能力外，团队人数统计使用聚合 SQL，一次性按团队汇总已确认订单的有效游客数。
 * 历史 merge_source 订单按老系统复测口径仍参与来源团队人数统计。</p>
 */
@Mapper
public interface SalesBookingOrderMapper extends BaseMapper<SalesBookingOrderEntity> {

    /**
     * 获取订单编号生成的事务级数据库锁。
     *
     * <p>同一租户、同一日期前缀的订单号生成必须串行化，避免并发请求读取到相同最大后缀。</p>
     *
     * @return PostgreSQL void 锁函数的文本占位结果，业务层只依赖加锁副作用
     */
    @Select("""
            SELECT pg_advisory_xact_lock(hashtextextended(CONCAT('sales_order_no:', #{tenantId}, ':', #{prefix}), 0))::text
            """)
    String lockOrderNoGeneration(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);

    /**
     * 查询当前订单编号前缀下的最大数字后缀。
     */
    @Select("""
            SELECT COALESCE(MAX(CAST(SUBSTRING(order_no FROM LENGTH(#{prefix}) + 1) AS integer)), 0)
            FROM sales_orders
            WHERE tenant_id = #{tenantId}
              AND is_deleted = false
              AND order_no LIKE CONCAT(#{prefix}, '%')
              AND SUBSTRING(order_no FROM LENGTH(#{prefix}) + 1) ~ '^[0-9]+$'
            """)
    Integer maxOrderNoSuffix(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);

    /**
     * 汇总团队下已确认且未删除订单的游客人数。
     *
     * @param tenantId 当前租户 ID
     * @param teamId 团队 ID
     * @return 已确认订单游客人数
     */
    @Select("""
            SELECT COALESCE(COUNT(g.id), 0)
            FROM sales_order_guests g
            JOIN sales_orders o
              ON o.tenant_id = g.tenant_id
             AND o.id = g.order_id
             AND o.is_deleted = false
             AND o.status = 'confirmed'
             AND COALESCE(o.order_role, 'normal') IN ('normal', 'merge_child', 'merge_source')
            WHERE g.tenant_id = #{tenantId}
              AND g.team_id = #{teamId}
              AND g.is_deleted = false
            """)
    Integer sumGuestCountByTeam(@Param("tenantId") Long tenantId, @Param("teamId") Long teamId);

    /**
     * 查询团队操作页可见订单列表。
     *
     * <p>列表要显示拼团来源留痕订单，方便按旧系统追溯“拼团订单信息”。</p>
     */
    @Select("""
            SELECT *
            FROM sales_orders
            WHERE tenant_id = #{tenantId}
              AND team_id = #{teamId}
              AND is_deleted = false
              AND COALESCE(order_role, 'normal') IN ('normal', 'merge_child', 'merge_source')
            ORDER BY booked_at DESC, id DESC
            """)
    List<SalesBookingOrderEntity> selectActiveByTeam(@Param("tenantId") Long tenantId, @Param("teamId") Long teamId);
}
