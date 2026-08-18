package com.mtravel.platform.purchase.relation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.purchase.relation.dto.PurchaseRelationSupplierPriceRow;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * PurchaseRelation Mapper，基础增删改查由 MyBatis-Plus 提供。
 */
public interface PurchaseRelationMapper extends BaseMapper<PurchaseRelationEntity> {

    /**
     * 批量读取资源的有效供应商关系及报价明细。
     *
     * <p>供应商名称、关系排序和分类报价在一条 SQL 中返回，供资源列表、产品设计等读模型使用，
     * 避免远程数据库环境中按表顺序多次往返。</p>
     */
    @Select("""
            <script>
            SELECT
              r.resource_id AS resource_id,
              r.id AS relation_id,
              r.supplier_id AS supplier_id,
              s.supplier_name AS supplier_name,
              r.is_default AS default_supplier,
              r.price_mode AS price_mode,
              r.unified_price AS unified_price,
              p.resource_project_id AS resource_project_id,
              p.project_name AS project_name,
              p.market_price AS market_price,
              p.peer_price AS peer_price,
              p.team_price AS team_price
            FROM purchase_relations r
            JOIN suppliers s
              ON s.tenant_id = r.tenant_id
             AND s.id = r.supplier_id
             AND s.is_deleted = false
            LEFT JOIN supplier_resource_prices p
              ON p.tenant_id = r.tenant_id
             AND p.relation_id = r.id
             AND p.is_deleted = false
             AND p.status = 'active'
            WHERE r.tenant_id = #{tenantId}
              AND r.is_deleted = false
              AND r.status = 'active'
              AND r.resource_id IN
              <foreach collection="resourceIds" item="resourceId" open="(" separator="," close=")">
                #{resourceId}
              </foreach>
            ORDER BY r.resource_id, r.is_default DESC, r.group_quantity ASC, r.id ASC,
                     p.resource_project_id ASC, p.id ASC
            </script>
            """)
    List<PurchaseRelationSupplierPriceRow> selectActiveResourceSupplierPriceRows(
            @Param("tenantId") Long tenantId,
            @Param("resourceIds") List<Long> resourceIds
    );
}
