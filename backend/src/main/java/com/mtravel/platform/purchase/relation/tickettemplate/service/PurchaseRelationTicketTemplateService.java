package com.mtravel.platform.purchase.relation.tickettemplate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.attachment.entity.CommonAttachmentEntity;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateFieldResponse;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateFieldSaveRequest;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateHeaderResponse;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateResponse;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateSaveRequest;
import com.mtravel.platform.purchase.relation.tickettemplate.entity.PurchaseRelationTicketTemplateEntity;
import com.mtravel.platform.purchase.relation.tickettemplate.entity.PurchaseRelationTicketTemplateFieldEntity;
import com.mtravel.platform.purchase.relation.tickettemplate.mapper.PurchaseRelationTicketTemplateFieldMapper;
import com.mtravel.platform.purchase.relation.tickettemplate.mapper.PurchaseRelationTicketTemplateMapper;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 采购关系游客名单模板服务。
 *
 * <p>服务负责把模板配置限定到当前租户和采购关系下，并维护 Excel 表头与系统游客字段的映射。</p>
 */
@Service
public class PurchaseRelationTicketTemplateService {

    private final PurchaseRelationTicketTemplateMapper templateMapper;
    private final PurchaseRelationTicketTemplateFieldMapper fieldMapper;
    private final PurchaseRelationMapper relationMapper;
    private final CommonAttachmentService attachmentService;
    private final TicketTemplateHeaderParser headerParser;
    private final TicketTemplateFillModeValidator fillModeValidator;

    public PurchaseRelationTicketTemplateService(
            PurchaseRelationTicketTemplateMapper templateMapper,
            PurchaseRelationTicketTemplateFieldMapper fieldMapper,
            PurchaseRelationMapper relationMapper,
            CommonAttachmentService attachmentService,
            TicketTemplateHeaderParser headerParser,
            TicketTemplateFillModeValidator fillModeValidator
    ) {
        this.templateMapper = templateMapper;
        this.fieldMapper = fieldMapper;
        this.relationMapper = relationMapper;
        this.attachmentService = attachmentService;
        this.headerParser = headerParser;
        this.fillModeValidator = fillModeValidator;
    }

    /** 查询某条采购关系下的游客名单模板配置。 */
    public TicketTemplateResponse detailByRelation(Long tenantId, Long relationId) {
        relation(tenantId, relationId);
        PurchaseRelationTicketTemplateEntity template = templateByRelation(tenantId, relationId);
        if (template == null) {
            return null;
        }
        return toResponse(template);
    }

    /** 读取模板附件的 Excel 表头。 */
    public TicketTemplateHeaderResponse headers(Long tenantId, Long attachmentId, Integer headerRow) {
        try (InputStream input = attachmentService.openStream(attachmentId, tenantId)) {
            return headerParser.parse(input, headerRow);
        } catch (Exception ex) {
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException("Excel 模板读取失败");
        }
    }

    /**
     * 保存模板配置。
     *
     * <p>同一采购关系只保留一套未删除模板。保存时先更新或新增主表，再重写字段映射，避免旧列残留。</p>
     */
    @Transactional
    public TicketTemplateResponse save(TicketTemplateSaveRequest request, Long tenantId, String operator) {
        relation(tenantId, request.relationId());
        CommonAttachmentEntity attachment = attachmentService.getEntity(request.attachmentId(), tenantId);
        validateRequest(request);

        PurchaseRelationTicketTemplateEntity template = templateByRelation(tenantId, request.relationId());
        if (template == null) {
            template = new PurchaseRelationTicketTemplateEntity();
            template.setTenantId(tenantId);
            template.setRelationId(request.relationId());
            template.setCreatedBy(operator);
            template.setIsDeleted(false);
            applyFields(template, request, attachment);
            templateMapper.insert(template);
        } else {
            PurchaseRelationTicketTemplateEntity update = new PurchaseRelationTicketTemplateEntity();
            applyFields(update, request, attachment);
            int updated = templateMapper.update(update, baseTemplateUpdate(tenantId).eq("id", template.getId()));
            if (updated == 0) {
                throw new BizException("游客名单模板不存在或已删除");
            }
            template = templateMapper.selectOne(baseTemplateQuery(tenantId).eq("id", template.getId()));
        }

        rewriteFields(template.getId(), request.fields(), tenantId, operator);
        attachmentService.bind(request.attachmentId(), template.getId(), tenantId);
        return toResponse(template);
    }

    /** 软删除某条采购关系下的模板和字段映射。 */
    @Transactional
    public void deleteByRelation(Long tenantId, Long relationId, String operator) {
        PurchaseRelationTicketTemplateEntity template = templateByRelation(tenantId, relationId);
        if (template == null) {
            return;
        }
        softDeleteTemplate(template.getId(), tenantId, operator);
        softDeleteFields(template.getId(), tenantId, operator);
    }

    private void validateRequest(TicketTemplateSaveRequest request) {
        if (!StringUtils.hasText(request.status()) || !("active".equals(request.status()) || "disabled".equals(request.status()))) {
            throw new BizException("模板状态不合法");
        }
        if (request.dataStartRow() <= request.headerRow()) {
            throw new BizException("数据开始行必须大于表头行");
        }
        if (request.fields() == null || request.fields().isEmpty()) {
            throw new BizException("字段映射不能为空");
        }
        for (TicketTemplateFieldSaveRequest field : request.fields()) {
            fillModeValidator.validate(field);
        }
    }

    private void applyFields(
            PurchaseRelationTicketTemplateEntity entity,
            TicketTemplateSaveRequest request,
            CommonAttachmentEntity attachment
    ) {
        entity.setTemplateName(clean(request.templateName()));
        entity.setAttachmentId(request.attachmentId());
        entity.setTemplateFileUrl(StringUtils.hasText(request.templateFileUrl())
                ? request.templateFileUrl().trim()
                : attachment.getFileUrl());
        entity.setOriginalFilename(StringUtils.hasText(request.originalFilename())
                ? request.originalFilename().trim()
                : attachment.getOriginalFilename());
        entity.setSheetName(clean(request.sheetName()));
        entity.setHeaderRow(request.headerRow());
        entity.setDataStartRow(request.dataStartRow());
        entity.setStatus(request.status());
        entity.setRemark(clean(request.remark()));
    }

    private void rewriteFields(
            Long templateId,
            List<TicketTemplateFieldSaveRequest> fields,
            Long tenantId,
            String operator
    ) {
        softDeleteFields(templateId, tenantId, operator);
        int index = 0;
        for (TicketTemplateFieldSaveRequest field : fields) {
            PurchaseRelationTicketTemplateFieldEntity entity = new PurchaseRelationTicketTemplateFieldEntity();
            entity.setTenantId(tenantId);
            entity.setTemplateId(templateId);
            entity.setTemplateHeader(clean(field.templateHeader()));
            entity.setColumnIndex(field.columnIndex());
            entity.setSystemField(clean(field.systemField()));
            entity.setFillMode(fillModeValidator.defaultFillMode(field.fillMode()));
            entity.setFixedValue(clean(field.fixedValue()));
            entity.setRequired(Boolean.TRUE.equals(field.required()));
            entity.setSortOrder(field.sortOrder() == null ? index : field.sortOrder());
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            fieldMapper.insert(entity);
            index++;
        }
    }

    private void softDeleteTemplate(Long templateId, Long tenantId, String operator) {
        PurchaseRelationTicketTemplateEntity update = new PurchaseRelationTicketTemplateEntity();
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(operator);
        templateMapper.update(update, baseTemplateUpdate(tenantId).eq("id", templateId));
    }

    private void softDeleteFields(Long templateId, Long tenantId, String operator) {
        PurchaseRelationTicketTemplateFieldEntity update = new PurchaseRelationTicketTemplateFieldEntity();
        update.setIsDeleted(true);
        update.setDeletedAt(OffsetDateTime.now());
        update.setDeletedBy(operator);
        fieldMapper.update(update, new UpdateWrapper<PurchaseRelationTicketTemplateFieldEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("template_id", templateId));
    }

    private TicketTemplateResponse toResponse(PurchaseRelationTicketTemplateEntity template) {
        List<TicketTemplateFieldResponse> fields = fieldMapper.selectList(new QueryWrapper<PurchaseRelationTicketTemplateFieldEntity>()
                        .eq("tenant_id", template.getTenantId())
                        .eq("is_deleted", false)
                        .eq("template_id", template.getId())
                        .orderByAsc("sort_order")
                        .orderByAsc("column_index"))
                .stream()
                .map(TicketTemplateFieldResponse::fromEntity)
                .toList();
        return TicketTemplateResponse.fromEntity(template, fields);
    }

    private PurchaseRelationTicketTemplateEntity templateByRelation(Long tenantId, Long relationId) {
        return templateMapper.selectOne(baseTemplateQuery(tenantId).eq("relation_id", relationId));
    }

    private PurchaseRelationEntity relation(Long tenantId, Long relationId) {
        PurchaseRelationEntity entity = relationMapper.selectOne(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", relationId));
        if (entity == null) {
            throw new BizException("采购关系不存在或已删除");
        }
        return entity;
    }

    private QueryWrapper<PurchaseRelationTicketTemplateEntity> baseTemplateQuery(Long tenantId) {
        return new QueryWrapper<PurchaseRelationTicketTemplateEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<PurchaseRelationTicketTemplateEntity> baseTemplateUpdate(Long tenantId) {
        return new UpdateWrapper<PurchaseRelationTicketTemplateEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
