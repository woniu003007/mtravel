package com.mtravel.platform.purchase.relation.tickettemplate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * 游客名单模板配置保存请求。
 *
 * @param relationId 采购关系 ID
 * @param templateName 模板名称
 * @param attachmentId 模板附件 ID
 * @param templateFileUrl 模板文件访问地址
 * @param originalFilename 原始文件名
 * @param sheetName 工作表名称
 * @param headerRow 表头行号
 * @param dataStartRow 游客数据开始行号
 * @param status 模板状态
 * @param remark 备注
 * @param fields 字段映射列表
 */
public record TicketTemplateSaveRequest(
        @NotNull(message = "采购关系不能为空") Long relationId,
        @NotBlank(message = "模板名称不能为空") String templateName,
        @NotNull(message = "模板附件不能为空") Long attachmentId,
        String templateFileUrl,
        String originalFilename,
        String sheetName,
        @NotNull(message = "表头行不能为空") @Min(value = 1, message = "表头行必须从1开始") Integer headerRow,
        @NotNull(message = "数据开始行不能为空") @Min(value = 1, message = "数据开始行必须从1开始") Integer dataStartRow,
        @Pattern(regexp = "active|disabled", message = "模板状态不合法") String status,
        String remark,
        @Valid List<TicketTemplateFieldSaveRequest> fields
) {
}
