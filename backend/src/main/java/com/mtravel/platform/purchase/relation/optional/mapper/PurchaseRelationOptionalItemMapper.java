package com.mtravel.platform.purchase.relation.optional.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.purchase.relation.optional.entity.PurchaseRelationOptionalItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 采购关系自费项目报价数据访问接口。 */
public interface PurchaseRelationOptionalItemMapper extends BaseMapper<PurchaseRelationOptionalItemEntity> {

    /** 以单条 INSERT 批量保存自费项目，避免资源页逐项目写库。 */
    @Insert("""
            <script>
            INSERT INTO purchase_relation_optional_items (
              tenant_id, relation_id, resource_optional_item_id, project_name, cost_price, suggested_sale_price, price_unit,
              price_description, status, created_by, is_deleted
            ) VALUES
            <foreach collection="items" item="item" separator=",">
              (#{item.tenantId}, #{item.relationId}, #{item.resourceOptionalItemId}, #{item.projectName}, #{item.costPrice}, #{item.suggestedSalePrice},
               'yuan_per_person', #{item.priceDescription}, #{item.status}, #{item.createdBy}, false)
            </foreach>
            </script>
            """)
    int insertBatch(@Param("items") List<PurchaseRelationOptionalItemEntity> items);

    /** 软删除采购关系下的旧自费项目，供资源页更新时事务性整组替换。 */
    @Update("""
            UPDATE purchase_relation_optional_items
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
