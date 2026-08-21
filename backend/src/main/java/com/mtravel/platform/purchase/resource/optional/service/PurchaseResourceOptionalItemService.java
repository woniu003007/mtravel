package com.mtravel.platform.purchase.resource.optional.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.resource.optional.dto.PurchaseResourceOptionalItemResponse;
import com.mtravel.platform.purchase.resource.optional.dto.PurchaseResourceOptionalItemSaveRequest;
import com.mtravel.platform.purchase.resource.optional.entity.PurchaseResourceOptionalItemEntity;
import com.mtravel.platform.purchase.resource.optional.mapper.PurchaseResourceOptionalItemMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 管理景区资源自费项目主档，供应商报价和介绍素材均以该主档为关联目标。 */
@Service
public class PurchaseResourceOptionalItemService {
    private final PurchaseResourceMapper resourceMapper;
    private final PurchaseResourceOptionalItemMapper optionalItemMapper;
    public PurchaseResourceOptionalItemService(PurchaseResourceMapper resourceMapper, PurchaseResourceOptionalItemMapper optionalItemMapper) {
        this.resourceMapper = resourceMapper; this.optionalItemMapper = optionalItemMapper;
    }
    public List<PurchaseResourceOptionalItemResponse> list(Long tenantId, Long resourceId) {
        requireScenic(tenantId, resourceId);
        return optionalItemMapper.selectList(new QueryWrapper<PurchaseResourceOptionalItemEntity>()
                .eq("tenant_id", tenantId).eq("resource_id", resourceId).eq("is_deleted", false)
                .orderByAsc("project_name").orderByAsc("id")).stream().map(PurchaseResourceOptionalItemResponse::fromEntity).toList();
    }
    @Transactional
    public PurchaseResourceOptionalItemResponse create(Long tenantId, Long resourceId, PurchaseResourceOptionalItemSaveRequest request, String operator) {
        requireScenic(tenantId, resourceId); assertNameAvailable(tenantId, resourceId, request.projectName(), null);
        PurchaseResourceOptionalItemEntity entity = new PurchaseResourceOptionalItemEntity();
        entity.setTenantId(tenantId); entity.setResourceId(resourceId); entity.setProjectName(cleanRequired(request.projectName()));
        entity.setItemType(normalizeType(request.optionalItemType())); entity.setPriceUnit("yuan_per_person");
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active"); entity.setCreatedBy(operator); entity.setIsDeleted(false);
        optionalItemMapper.insert(entity); return PurchaseResourceOptionalItemResponse.fromEntity(entity);
    }
    @Transactional
    public PurchaseResourceOptionalItemResponse update(Long tenantId, Long resourceId, Long id, PurchaseResourceOptionalItemSaveRequest request) {
        load(tenantId, resourceId, id); assertNameAvailable(tenantId, resourceId, request.projectName(), id);
        PurchaseResourceOptionalItemEntity entity = new PurchaseResourceOptionalItemEntity(); entity.setProjectName(cleanRequired(request.projectName()));
        entity.setItemType(normalizeType(request.optionalItemType())); entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        optionalItemMapper.update(entity, base(tenantId, resourceId, id)); return PurchaseResourceOptionalItemResponse.fromEntity(load(tenantId, resourceId, id));
    }
    @Transactional
    public void delete(Long tenantId, Long resourceId, Long id, String operator) {
        load(tenantId, resourceId, id); assertNotReferenced(tenantId, id); PurchaseResourceOptionalItemEntity entity = new PurchaseResourceOptionalItemEntity();
        entity.setIsDeleted(true); entity.setDeletedAt(OffsetDateTime.now()); entity.setDeletedBy(operator); optionalItemMapper.update(entity, base(tenantId, resourceId, id));
    }
    public PurchaseResourceOptionalItemEntity loadActive(Long tenantId, Long resourceId, Long id) {
        PurchaseResourceOptionalItemEntity entity = load(tenantId, resourceId, id);
        if (!"active".equals(entity.getStatus())) throw new BizException("自费项目已停用"); return entity;
    }
    private PurchaseResourceOptionalItemEntity load(Long tenantId, Long resourceId, Long id) {
        PurchaseResourceOptionalItemEntity entity = optionalItemMapper.selectOne(new QueryWrapper<PurchaseResourceOptionalItemEntity>()
                .eq("tenant_id", tenantId).eq("resource_id", resourceId).eq("id", id).eq("is_deleted", false).last("limit 1"));
        if (entity == null) throw new BizException("自费项目不存在或已删除"); return entity;
    }
    private UpdateWrapper<PurchaseResourceOptionalItemEntity> base(Long tenantId, Long resourceId, Long id) { return new UpdateWrapper<PurchaseResourceOptionalItemEntity>().eq("tenant_id", tenantId).eq("resource_id", resourceId).eq("id", id).eq("is_deleted", false); }
    private void requireScenic(Long tenantId, Long resourceId) { PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>().eq("tenant_id", tenantId).eq("id", resourceId).eq("is_deleted", false).last("limit 1")); if (resource == null || !"scenic".equals(resource.getResourceType())) throw new BizException("自费项目仅适用于景区资源"); }
    private void assertNameAvailable(Long tenantId, Long resourceId, String name, Long exceptId) { Long count = optionalItemMapper.selectCount(new QueryWrapper<PurchaseResourceOptionalItemEntity>().eq("tenant_id", tenantId).eq("resource_id", resourceId).eq("project_name", cleanRequired(name)).eq("is_deleted", false).ne(exceptId != null, "id", exceptId)); if (count != null && count > 0) throw new BizException("当前景区下自费项目名称不能重复"); }
    private void assertNotReferenced(Long tenantId, Long id) {
        Long quoteCount = optionalItemMapper.countReferences(tenantId, id);
        if (quoteCount != null && quoteCount > 0) throw new BizException("自费项目仍被供应商报价、介绍素材或产品设计引用，不能删除");
    }
    private String normalizeType(String type) { return "scenic_transport".equals(type) ? type : "recommended_self_pay"; }
    private String cleanRequired(String value) { String clean = value == null ? null : value.trim(); if (!StringUtils.hasText(clean)) throw new BizException("自费项目名称不能为空"); return clean; }
}
