package com.mtravel.platform.purchase.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import org.springframework.stereotype.Service;

/**
 * 采购侧供应商查询与校验服务。
 *
 * <p>采购关系、采购合同、酒店资源、景区资源都需要校验 supplier_id 是否属于当前租户且未删除。
 * 集中处理可以避免各模块写出不一致的租户过滤条件。</p>
 */
@Service
public class SupplierLookupService {

    private final SupplierMapper supplierMapper;

    public SupplierLookupService(SupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    /** 校验供应商存在。supplierId 为空时直接跳过，适用于暂未绑定供应商的基础资源。 */
    public void assertSupplierIfPresent(Long tenantId, Long supplierId) {
        if (supplierId == null) {
            return;
        }
        if (find(tenantId, supplierId) == null) {
            throw new BizException("供应商不存在或已删除");
        }
    }

    /** 查询供应商名称，用于接口返回展示。 */
    public String supplierName(Long tenantId, Long supplierId) {
        SupplierEntity entity = find(tenantId, supplierId);
        return entity == null ? null : entity.getSupplierName();
    }

    /**
     * 查询供应商完整档案，用于采购关系等列表展示负责人和联系电话。
     *
     * @return 当前租户下未删除的供应商；不存在时返回 {@code null}
     */
    public SupplierEntity supplier(Long tenantId, Long supplierId) {
        return find(tenantId, supplierId);
    }

    private SupplierEntity find(Long tenantId, Long supplierId) {
        return supplierMapper.selectOne(new QueryWrapper<SupplierEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", supplierId));
    }
}
