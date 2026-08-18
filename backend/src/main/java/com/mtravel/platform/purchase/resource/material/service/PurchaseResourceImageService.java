package com.mtravel.platform.purchase.resource.material.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.dto.AttachmentResponse;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.enums.PurchaseResourceStatus;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceImageResponse;
import com.mtravel.platform.purchase.resource.material.dto.PurchaseResourceImageUpdateRequest;
import com.mtravel.platform.purchase.resource.material.entity.PurchaseResourceImageEntity;
import com.mtravel.platform.purchase.resource.material.mapper.PurchaseResourceImageMapper;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 采购资源图片素材服务，负责图片上传、标签、封面和删除。 */
@Service
public class PurchaseResourceImageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_IMAGE_SIZE = 20L * 1024 * 1024;

    private final PurchaseResourceMapper resourceMapper;
    private final PurchaseResourceImageMapper imageMapper;
    private final CommonAttachmentService attachmentService;
    private final ResourceMaterialTagCodec tagCodec;

    public PurchaseResourceImageService(
            PurchaseResourceMapper resourceMapper,
            PurchaseResourceImageMapper imageMapper,
            CommonAttachmentService attachmentService,
            ResourceMaterialTagCodec tagCodec
    ) {
        this.resourceMapper = resourceMapper;
        this.imageMapper = imageMapper;
        this.attachmentService = attachmentService;
        this.tagCodec = tagCodec;
    }

    /** 懒加载当前资源的有效图片素材。 */
    public List<PurchaseResourceImageResponse> list(Long tenantId, Long resourceId) {
        validateResource(tenantId, resourceId);
        return imageMapper.selectList(new QueryWrapper<PurchaseResourceImageEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("resource_id", resourceId)
                        .eq("status", PurchaseResourceStatus.ACTIVE.value())
                        .orderByDesc("is_cover")
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 上传图片并创建归属当前资源的图片素材记录。 */
    @Transactional
    public List<PurchaseResourceImageResponse> upload(
            Long tenantId,
            Long resourceId,
            List<MultipartFile> files,
            String operator
    ) {
        validateResource(tenantId, resourceId);
        if (files == null || files.isEmpty()) {
            throw new BizException("请先选择要上传的图片");
        }
        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (validFiles.isEmpty()) {
            throw new BizException("请先选择要上传的图片");
        }
        int sortOrder = nextSortOrder(tenantId, resourceId);
        return validFiles.stream()
                .map(file -> uploadOne(tenantId, resourceId, file, operator, sortOrder))
                .toList();
    }

    /** 修改图片标签和展示排序。 */
    @Transactional
    public PurchaseResourceImageResponse update(
            Long tenantId,
            Long resourceId,
            Long imageId,
            PurchaseResourceImageUpdateRequest request
    ) {
        PurchaseResourceImageEntity existing = load(tenantId, resourceId, imageId);
        PurchaseResourceImageEntity entity = new PurchaseResourceImageEntity();
        entity.setTags(tagCodec.encode(request.tags()));
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        imageMapper.update(entity, baseUpdate(tenantId, resourceId, imageId));
        return toResponse(load(tenantId, resourceId, imageId));
    }

    /** 将某张图片设置为资源唯一封面图。 */
    @Transactional
    public PurchaseResourceImageResponse setCover(Long tenantId, Long resourceId, Long imageId) {
        load(tenantId, resourceId, imageId);
        PurchaseResourceImageEntity unsetCover = new PurchaseResourceImageEntity();
        unsetCover.setIsCover(false);
        imageMapper.update(unsetCover, new UpdateWrapper<PurchaseResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("status", PurchaseResourceStatus.ACTIVE.value())
                .eq("is_cover", true));

        PurchaseResourceImageEntity setCover = new PurchaseResourceImageEntity();
        setCover.setIsCover(true);
        imageMapper.update(setCover, baseUpdate(tenantId, resourceId, imageId));
        return toResponse(load(tenantId, resourceId, imageId));
    }

    /** 软删除图片素材和附件元数据，并在提交后清理原始图片文件。 */
    @Transactional
    public void delete(Long tenantId, Long resourceId, Long imageId, String operator) {
        PurchaseResourceImageEntity existing = load(tenantId, resourceId, imageId);
        PurchaseResourceImageEntity entity = new PurchaseResourceImageEntity();
        entity.setStatus(PurchaseResourceStatus.DISABLED.value());
        entity.setIsCover(false);
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);
        imageMapper.update(entity, baseUpdate(tenantId, resourceId, imageId));
        CommonAttachmentEntity attachment = attachmentService.softDelete(existing.getAttachmentId(), tenantId, operator);
        attachmentService.deletePhysicalFileAfterCommit(attachment);
    }

    /** 下载原始图片，读取前始终校验租户、资源和图片归属。 */
    public ResponseEntity<InputStreamResource> download(Long tenantId, Long resourceId, Long imageId) {
        PurchaseResourceImageEntity image = load(tenantId, resourceId, imageId);
        CommonAttachmentEntity attachment = attachmentService.getEntity(image.getAttachmentId(), tenantId);
        String contentType = StringUtils.hasText(attachment.getContentType())
                ? attachment.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        InputStream inputStream = attachmentService.openStream(image.getAttachmentId(), tenantId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(image.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(inputStream));
    }

    private PurchaseResourceImageResponse uploadOne(
            Long tenantId,
            Long resourceId,
            MultipartFile file,
            String operator,
            int sortOrder
    ) {
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException("图片素材只支持 JPG、JPEG、PNG、WEBP 格式");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BizException("单张图片不能超过20MB");
        }
        AttachmentResponse uploaded = attachmentService.upload(
                file, "采购管理", "资源图片", resourceId, tenantId, operator
        );
        PurchaseResourceImageEntity entity = new PurchaseResourceImageEntity();
        entity.setTenantId(tenantId);
        entity.setResourceId(resourceId);
        entity.setAttachmentId(uploaded.id());
        entity.setOriginalFilename(uploaded.originalFilename());
        entity.setFileExt(extension);
        entity.setFileSize(uploaded.fileSize());
        entity.setTags(tagCodec.encode(List.of()));
        entity.setIsCover(false);
        entity.setSortOrder(sortOrder);
        entity.setStatus(PurchaseResourceStatus.ACTIVE.value());
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        imageMapper.insert(entity);
        return toResponse(entity);
    }

    private int nextSortOrder(Long tenantId, Long resourceId) {
        PurchaseResourceImageEntity latest = imageMapper.selectOne(new QueryWrapper<PurchaseResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("status", PurchaseResourceStatus.ACTIVE.value())
                .orderByDesc("sort_order")
                .orderByDesc("id")
                .last("limit 1"));
        return latest == null || latest.getSortOrder() == null ? 0 : latest.getSortOrder() + 1;
    }

    private PurchaseResourceImageEntity load(Long tenantId, Long resourceId, Long imageId) {
        PurchaseResourceImageEntity entity = imageMapper.selectOne(new QueryWrapper<PurchaseResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("status", PurchaseResourceStatus.ACTIVE.value())
                .eq("id", imageId)
                .last("limit 1"));
        if (entity == null) {
            throw new BizException("图片素材不存在或已删除");
        }
        return entity;
    }

    private PurchaseResourceEntity validateResource(Long tenantId, Long resourceId) {
        PurchaseResourceEntity resource = resourceMapper.selectOne(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", resourceId)
                .eq("status", PurchaseResourceStatus.ACTIVE.value())
                .last("limit 1"));
        if (resource == null) {
            throw new BizException("资源不存在、已删除或已停用");
        }
        return resource;
    }

    private UpdateWrapper<PurchaseResourceImageEntity> baseUpdate(Long tenantId, Long resourceId, Long imageId) {
        return new UpdateWrapper<PurchaseResourceImageEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("status", PurchaseResourceStatus.ACTIVE.value())
                .eq("id", imageId);
    }

    private PurchaseResourceImageResponse toResponse(PurchaseResourceImageEntity entity) {
        return PurchaseResourceImageResponse.fromEntity(entity, tagCodec.decode(entity.getTags()));
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).trim().toLowerCase(Locale.ROOT);
    }
}
