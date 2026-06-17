package com.mtravel.platform.enterprise.role.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 企业角色保存请求。
 *
 * <p>新增和修改角色共用该对象。角色编码用于权限判断，创建后仍允许修改，但 Service 会保证同租户下唯一。</p>
 */
public record EnterpriseRoleSaveRequest(
        @NotBlank(message = "角色编码不能为空")
        @Size(max = 80, message = "角色编码最多80个字符")
        String roleCode,

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 160, message = "角色名称最多160个字符")
        String roleName,

        @Min(value = 0, message = "排序不能小于0")
        Integer sortOrder,

        @Pattern(regexp = "active|disabled", message = "角色状态不合法")
        String status,

        String remark
) {}
