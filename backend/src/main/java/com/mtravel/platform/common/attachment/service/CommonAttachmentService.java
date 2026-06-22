package com.mtravel.platform.common.attachment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.dto.AttachmentResponse;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.mapper.CommonAttachmentMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公共附件服务。
 *
 * <p>首版采用本地磁盘存储。业务表只保存 attachment_id 或 file_url，后续切换对象存储时，
 * 只需要替换本服务的存储实现，不影响客户合同、采购合同和地接确认单等业务接口。</p>
 */
@Service
public class CommonAttachmentService {

    private final CommonAttachmentMapper mapper;
    private final Path uploadRoot;

    public CommonAttachmentService(
            CommonAttachmentMapper mapper,
            @Value("${mtravel.upload.root:./data/uploads}") String uploadRoot
    ) {
        this.mapper = mapper;
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
    }

    /**
     * 上传附件并保存元数据。
     */
    public AttachmentResponse upload(
            MultipartFile file,
            String businessModule,
            String businessType,
            Long businessId,
            Long tenantId,
            String operator
    ) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        String originalName = cleanFilename(file.getOriginalFilename());
        String extension = extension(originalName);
        String storedName = UUID.randomUUID() + (extension == null ? "" : "." + extension);
        LocalDate today = LocalDate.now();
        Path directory = uploadRoot
                .resolve(String.valueOf(tenantId))
                .resolve(today.toString());
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BizException("文件路径不合法");
        }
        try {
            Files.createDirectories(directory);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BizException("文件保存失败");
        }

        CommonAttachmentEntity entity = new CommonAttachmentEntity();
        entity.setTenantId(tenantId);
        entity.setBusinessModule(cleanRequired(businessModule));
        entity.setBusinessType(cleanRequired(businessType));
        entity.setBusinessId(businessId);
        entity.setOriginalFilename(originalName);
        entity.setStoredFilename(storedName);
        entity.setStoragePath(target.toString());
        entity.setFileUrl("/attachments/" + tenantId + "/" + today + "/" + storedName);
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setFileExt(extension);
        entity.setStatus("active");
        entity.setUploadedBy(operator);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return AttachmentResponse.fromEntity(entity);
    }

    /**
     * 查询业务记录附件列表。
     */
    public List<AttachmentResponse> listByBusiness(
            Long tenantId,
            String businessModule,
            String businessType,
            Long businessId
    ) {
        return mapper.selectList(baseQuery(tenantId)
                        .eq("business_module", businessModule)
                        .eq("business_type", businessType)
                        .eq(businessId != null, "business_id", businessId)
                        .orderByDesc("id"))
                .stream()
                .map(AttachmentResponse::fromEntity)
                .toList();
    }

    /**
     * 将附件绑定到具体业务记录，适用于先上传文件后保存表单的流程。
     */
    public void bind(Long attachmentId, Long businessId, Long tenantId) {
        if (attachmentId == null || businessId == null) {
            return;
        }
        CommonAttachmentEntity entity = new CommonAttachmentEntity();
        entity.setBusinessId(businessId);
        mapper.update(entity, new UpdateWrapper<CommonAttachmentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", attachmentId));
    }

    /**
     * 查询当前租户下的附件元数据。
     *
     * <p>业务模块读取上传文件时必须通过本方法带上租户边界，避免用 attachment_id 读取到其它租户文件。</p>
     */
    public CommonAttachmentEntity getEntity(Long attachmentId, Long tenantId) {
        CommonAttachmentEntity entity = mapper.selectOne(baseQuery(tenantId)
                .eq("id", attachmentId)
                .eq("status", "active"));
        if (entity == null) {
            throw new BizException("附件不存在或已停用");
        }
        return entity;
    }

    /**
     * 打开附件本地文件输入流。
     *
     * <p>首版附件存在本地磁盘。读取前再次校验路径仍在 uploadRoot 下，防止异常路径穿越。</p>
     */
    public InputStream openStream(Long attachmentId, Long tenantId) {
        CommonAttachmentEntity entity = getEntity(attachmentId, tenantId);
        Path path = Path.of(entity.getStoragePath()).toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new BizException("附件路径不合法");
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException ex) {
            throw new BizException("附件文件读取失败");
        }
    }

    private QueryWrapper<CommonAttachmentEntity> baseQuery(Long tenantId) {
        return new QueryWrapper<CommonAttachmentEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private String cleanFilename(String filename) {
        String value = StringUtils.hasText(filename) ? filename.trim() : "upload.bin";
        return Path.of(value).getFileName().toString();
    }

    private String cleanRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("附件业务归属不能为空");
        }
        return value.trim();
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return null;
        }
        return filename.substring(index + 1).toLowerCase();
    }
}
