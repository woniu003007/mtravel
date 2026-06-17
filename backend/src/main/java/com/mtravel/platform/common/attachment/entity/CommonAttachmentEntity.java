package com.mtravel.platform.common.attachment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 公共附件实体，对应 common_attachments 表。
 *
 * <p>该表只保存文件元数据和业务归属，不保存文件二进制内容。首版文件落本地磁盘，
 * 后续迁移 OSS 或 MinIO 时可保持业务表里的 attachment_id 不变。</p>
 */
@TableName("common_attachments")
public class CommonAttachmentEntity extends TenantSoftDeleteEntity {

    /** 业务模块名称，例如客户管理、采购管理。 */
    @TableField("business_module")
    private String businessModule;

    /** 业务类型，例如客户合同、采购合同、地接确认单。 */
    @TableField("business_type")
    private String businessType;

    /** 业务记录 ID，先上传后绑定时可为空。 */
    @TableField("business_id")
    private Long businessId;

    /** 用户上传时的原始文件名。 */
    @TableField("original_filename")
    private String originalFilename;

    /** 系统落盘保存的文件名。 */
    @TableField("stored_filename")
    private String storedFilename;

    /** 服务器本地存储路径。 */
    @TableField("storage_path")
    private String storagePath;

    /** 前端访问或下载文件使用的相对地址。 */
    @TableField("file_url")
    private String fileUrl;

    /** 文件 MIME 类型。 */
    @TableField("content_type")
    private String contentType;

    /** 文件大小，单位字节。 */
    @TableField("file_size")
    private Long fileSize;

    /** 文件扩展名。 */
    @TableField("file_ext")
    private String fileExt;

    /** 附件状态：active 可用，disabled 停用。 */
    @TableField("status")
    private String status;

    /** 上传人账号或名称。 */
    @TableField("uploaded_by")
    private String uploadedBy;

    public String getBusinessModule() { return businessModule; }
    public void setBusinessModule(String businessModule) { this.businessModule = businessModule; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileExt() { return fileExt; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
