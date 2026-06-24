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
 * <p>除 BaseMapper 能力外，团队人数统计使用聚合 SQL，一次性按团队汇总已确认订单的有效游客数。</p>
 */
@Mapper
public interface SalesBookingOrderMapper extends BaseMapper<SalesBookingOrderEntity> {

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
            WHERE g.tenant_id = #{tenantId}
              AND g.team_id = #{teamId}
              AND g.is_deleted = false
            """)
    Integer sumGuestCountByTeam(@Param("tenantId") Long tenantId, @Param("teamId") Long teamId);

    /**
     * 查询团队下订单列表，供团队操作页批量展示。
     */
    @Select("""
            SELECT *
            FROM sales_orders
            WHERE tenant_id = #{tenantId}
              AND team_id = #{teamId}
              AND is_deleted = false
            ORDER BY booked_at DESC, id DESC
            """)
    List<SalesBookingOrderEntity> selectActiveByTeam(@Param("tenantId") Long tenantId, @Param("teamId") Long teamId);
}
