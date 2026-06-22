package com.mtravel.platform.purchase.relation.tickettemplate.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 采购关系游客名单模板实体，对应 purchase_relation_ticket_templates 表。
 *
 * <p>模板挂在采购关系上，表示某个“景区资源 + 供应商/票务渠道”使用哪份游客名单 Excel 模板。</p>
 */
@TableName("purchase_relation_ticket_templates")
public class PurchaseRelationTicketTemplateEntity extends TenantSoftDeleteEntity {

    /** 采购关系 ID。 */
    @TableField("relation_id")
    private Long relationId;

    /** 模板名称，便于页面识别。 */
    @TableField("template_name")
    private String templateName;

    /** 上传到公共附件表后的附件 ID。 */
    @TableField("attachment_id")
    private Long attachmentId;

    /** 模板文件访问地址快照。 */
    @TableField("template_file_url")
    private String templateFileUrl;

    /** 用户上传时的文件名快照。 */
    @TableField("original_filename")
    private String originalFilename;

    /** Excel 工作表名称。 */
    @TableField("sheet_name")
    private String sheetName;

    /** 表头行号，按 Excel 习惯从 1 开始。 */
    @TableField("header_row")
    private Integer headerRow;

    /** 游客数据开始行号，按 Excel 习惯从 1 开始。 */
    @TableField("data_start_row")
    private Integer dataStartRow;

    /** 状态：active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    public Long getRelationId() { return relationId; }
    public void setRelationId(Long relationId) { this.relationId = relationId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getTemplateFileUrl() { return templateFileUrl; }
    public void setTemplateFileUrl(String templateFileUrl) { this.templateFileUrl = templateFileUrl; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public Integer getHeaderRow() { return headerRow; }
    public void setHeaderRow(Integer headerRow) { this.headerRow = headerRow; }
    public Integer getDataStartRow() { return dataStartRow; }
    public void setDataStartRow(Integer dataStartRow) { this.dataStartRow = dataStartRow; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
