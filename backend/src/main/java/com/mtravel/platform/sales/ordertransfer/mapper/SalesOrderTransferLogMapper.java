package com.mtravel.platform.sales.ordertransfer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.sales.ordertransfer.entity.SalesOrderTransferLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;

/**
 * 销售订单团队流转日志 Mapper。
 */
@Mapper
public interface SalesOrderTransferLogMapper extends BaseMapper<SalesOrderTransferLogEntity> {

    /**
     * 按来源订单批量查询已完成拼团关系。
     *
     * @param tenantId 当前租户 ID
     * @param sourceOrderIds 来源订单 ID 列表
     * @return 来源订单对应的目标团队和目标订单信息
     */
    @Lang(XMLLanguageDriver.class)
    @Select("""
            <script>
            SELECT l.*
            FROM sales_order_transfer_logs l
            WHERE l.tenant_id = #{tenantId}
              AND l.is_deleted = false
              AND l.transfer_type = 'merge'
              AND l.transfer_status = 'completed'
              AND l.source_order_id IN
              <foreach collection="sourceOrderIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            ORDER BY l.source_order_id ASC, l.operated_at ASC, l.id ASC
            </script>
            """)
    List<SalesOrderTransferLogEntity> selectCompletedMergeBySourceOrders(
            @Param("tenantId") Long tenantId,
            @Param("sourceOrderIds") List<Long> sourceOrderIds
    );

    /**
     * 按目标拼入订单批量查询已完成拼团关系。
     *
     * @param tenantId 当前租户 ID
     * @param childOrderIds 目标团队下生成的拼入订单 ID 列表
     * @return 拼入订单对应的来源团队和来源订单信息
     */
    @Lang(XMLLanguageDriver.class)
    @Select("""
            <script>
            SELECT l.*
            FROM sales_order_transfer_logs l
            WHERE l.tenant_id = #{tenantId}
              AND l.is_deleted = false
              AND l.transfer_type = 'merge'
              AND l.transfer_status = 'completed'
              AND l.child_order_id IN
              <foreach collection="childOrderIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            ORDER BY l.child_order_id ASC, l.operated_at ASC, l.id ASC
            </script>
            """)
    List<SalesOrderTransferLogEntity> selectCompletedMergeByChildOrders(
            @Param("tenantId") Long tenantId,
            @Param("childOrderIds") List<Long> childOrderIds
    );
}
