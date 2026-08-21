package com.mtravel.platform.purchase.resource.material.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceStatus;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceIntroductionImageSaveRequest;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceImageEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionEntity;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceIntroductionImageEntity;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceImageMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionImageMapper;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceIntroductionMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理介绍素材与资源图片素材库之间的选用关系和输出排序。 */
@Service
public class PurchaseResourceIntroductionImageService {

    private final PurchaseResourceIntroductionMapper introductionMapper;
    private final PurchaseResourceImageMapper imageMapper;
    private final PurchaseResourceIntroductionImageMapper introductionImageMapper;

    public PurchaseResourceIntroductionImageService(
            PurchaseResourceIntroductionMapper introductionMapper,
            PurchaseResourceImageMapper imageMapper,
            PurchaseResourceIntroductionImageMapper introductionImageMapper
    ) {
        this.introductionMapper = introductionMapper;
        this.imageMapper = imageMapper;
        this.introductionImageMapper = introductionImageMapper;
    }

    /** 返回当前介绍素材仍有效的图片素材 ID，按素材内排序。 */
    public List<Long> listImageIds(Long tenantId, Long resourceId, Long introductionId) {
        requireIntroduction(tenantId, resourceId, introductionId);
        List<PurchaseResourceIntroductionImageEntity> links = introductionImageMapper.selectList(
                new QueryWrapper<PurchaseResourceIntroductionImageEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("introduction_id", introductionId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id")
        );
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> imageIds = links.stream().map(PurchaseResourceIntroductionImageEntity::getResourceImageId).toList();
        List<Long> activeImageIds = imageMapper.selectList(new QueryWrapper<PurchaseResourceImageEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("resource_id", resourceId)
                        .eq("is_deleted", false)
                        .eq("status", PurchaseResourceStatus.ACTIVE.value())
                        .in("id", imageIds))
                .stream()
                .map(PurchaseResourceImageEntity::getId)
                .toList();
        return imageIds.stream().filter(activeImageIds::contains).toList();
    }

    /** 用当前勾选结果整体替换介绍素材图片；所有图片必须属于当前资源。 */
    @Transactional
    public List<Long> save(
            Long tenantId,
            Long resourceId,
            Long introductionId,
            PurchaseResourceIntroductionImageSaveRequest request,
            String operator
    ) {
        requireIntroduction(tenantId, resourceId, introductionId);
        List<Long> imageIds = request.imageIds().stream().filter(Objects::nonNull).distinct().toList();
        if (!imageIds.isEmpty()) {
            Long activeCount = imageMapper.selectCount(new QueryWrapper<PurchaseResourceImageEntity>()
                    .eq("tenant_id", tenantId)
                    .eq("resource_id", resourceId)
                    .eq("is_deleted", false)
                    .eq("status", PurchaseResourceStatus.ACTIVE.value())
                    .in("id", imageIds));
            if (activeCount == null || activeCount != imageIds.size()) {
                throw new BizException("所选图片不属于当前资源、已删除或已停用");
            }
        }
        softDeleteByIntroduction(tenantId, introductionId, operator, OffsetDateTime.now());
        for (int index = 0; index < imageIds.size(); index += 1) {
            PurchaseResourceIntroductionImageEntity link = new PurchaseResourceIntroductionImageEntity();
            link.setTenantId(tenantId);
            link.setIntroductionId(introductionId);
            link.setResourceImageId(imageIds.get(index));
            link.setSortOrder(index + 1);
            link.setCreatedBy(operator);
            link.setIsDeleted(false);
            introductionImageMapper.insert(link);
        }
        return imageIds;
    }

    private void softDeleteByIntroduction(Long tenantId, Long introductionId, String operator, OffsetDateTime now) {
        PurchaseResourceIntroductionImageEntity deleted = new PurchaseResourceIntroductionImageEntity();
        deleted.setIsDeleted(true);
        deleted.setDeletedAt(now);
        deleted.setDeletedBy(operator);
        introductionImageMapper.update(deleted, new UpdateWrapper<PurchaseResourceIntroductionImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("introduction_id", introductionId)
                .eq("is_deleted", false));
    }

    private void requireIntroduction(Long tenantId, Long resourceId, Long introductionId) {
        PurchaseResourceIntroductionEntity introduction = introductionMapper.selectOne(
                new QueryWrapper<PurchaseResourceIntroductionEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("resource_id", resourceId)
                        .eq("is_deleted", false)
                        .eq("id", introductionId)
                        .last("limit 1")
        );
        if (introduction == null) {
            throw new BizException("介绍素材不存在或已删除");
        }
    }
}
