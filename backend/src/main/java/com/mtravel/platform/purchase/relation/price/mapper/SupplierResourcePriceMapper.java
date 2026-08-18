package com.mtravel.platform.purchase.relation.price.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 供应商资源价格 Mapper。
 *
 * <p>价格查询、保存和软删除由 Service 组合租户条件后调用 MyBatis-Plus 基础能力。</p>
 */
public interface SupplierResourcePriceMapper extends BaseMapper<SupplierResourcePriceEntity> {

    /** 单条 SQL 批量写入供应商报价明细，避免资源页快捷新增供应商时逐项目访问数据库。 */
    @Insert("""
            <script>
            INSERT INTO supplier_resource_prices (
              tenant_id, relation_id, resource_project_id, project_name,
              market_price, peer_price, team_price, price_description,
              status, created_by, is_deleted
            ) VALUES
            <foreach collection="prices" item="item" separator=",">
              (#{item.tenantId}, #{item.relationId}, #{item.resourceProjectId}, #{item.projectName},
               #{item.marketPrice}, #{item.peerPrice}, #{item.teamPrice}, #{item.priceDescription},
               #{item.status}, #{item.createdBy}, false)
            </foreach>
            </script>
            """)
    int insertBatch(@Param("prices") List<SupplierResourcePriceEntity> prices);

    /** 单条 SQL 软删除采购关系下的旧报价，供事务性整组替换使用。 */
    @Update("""
            UPDATE supplier_resource_prices
               SET is_deleted = true, deleted_at = now(), deleted_by = #{operator}
             WHERE tenant_id = #{tenantId}
               AND relation_id = #{relationId}
               AND is_deleted = false
            """)
    int softDeleteByRelation(
            @Param("tenantId") Long tenantId,
            @Param("relationId") Long relationId,
            @Param("operator") String operator
    );
}
