package com.mtravel.platform.purchase.relation.tickettemplate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 游客名单模板字段映射保存请求。
 *
 * @param templateHeader Excel 表头名称
 * @param columnIndex Excel 列序号，从 1 开始
 * @param systemField 系统游客字段编码
 * @param fillMode 填充方式：游客字段、自动序号、固定值或不填充
 * @param fixedValue 固定值填充内容
 * @param required 是否必填
 * @param sortOrder 排序号
 */
public record TicketTemplateFieldSaveRequest(
        @NotBlank(message = "模板表头不能为空") String templateHeader,
        @NotNull(message = "列号不能为空") @Min(value = 1, message = "列号必须从1开始") Integer columnIndex,
        String systemField,
        String fillMode,
        String fixedValue,
        Boolean required,
        Integer sortOrder
) {
}
