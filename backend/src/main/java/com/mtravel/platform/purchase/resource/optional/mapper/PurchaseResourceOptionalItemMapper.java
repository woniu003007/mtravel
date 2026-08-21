package com.mtravel.platform.purchase.resource.optional.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 资源级自费项目主档 Mapper。 */
@Mapper
public interface PurchaseResourceOptionalItemMapper extends BaseMapper<PurchaseResourceOptionalItemEntity> {
    /** 有有效引用时不允许删除主档，避免供应商报价、素材和产品快照失去归属。 */
    @Select("""
            SELECT (SELECT count(*) FROM purchase_relation_optional_items WHERE tenant_id=#{tenantId} AND resource_optional_item_id=#{id} AND is_deleted=false)
                 + (SELECT count(*) FROM purchase_resource_introductions WHERE tenant_id=#{tenantId} AND resource_optional_item_id=#{id} AND is_deleted=false)
                 + (SELECT count(*) FROM sales_product_day_resource_optional_items WHERE tenant_id=#{tenantId} AND resource_optional_item_id=#{id} AND is_deleted=false)
            """)
    Long countReferences(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
