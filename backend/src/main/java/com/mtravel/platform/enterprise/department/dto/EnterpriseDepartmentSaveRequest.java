package com.mtravel.platform.enterprise.department.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 企业部门保存请求。
 *
 * <p>新增和修改共用该对象。前端可以不传上级部门，表示创建一级部门。</p>
 */
public record EnterpriseDepartmentSaveRequest(
        Long parentId,

        @Size(max = 80, message = "部门编码最多80个字符")
        String departmentCode,

        @NotBlank(message = "部门名称不能为空")
        @Size(max = 160, message = "部门名称最多160个字符")
        String departmentName,

        @Size(max = 80, message = "负责人最多80个字符")
        String managerName,

        @Size(max = 40, message = "联系电话最多40个字符")
        String contactPhone,

        @Min(value = 0, message = "排序不能小于0")
        Integer sortOrder,

        @Pattern(regexp = "active|disabled", message = "部门状态不合法")
        String status,

        String remark
) {}
