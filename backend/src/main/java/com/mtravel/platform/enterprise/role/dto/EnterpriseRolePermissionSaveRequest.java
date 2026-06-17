package com.mtravel.platform.enterprise.role.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 企业角色权限保存请求。
 *
 * <p>权限管理界面一次性提交当前角色的勾选结果，Service 会先软删除旧权限，再写入新权限。</p>
 */
public record EnterpriseRolePermissionSaveRequest(
        @Valid
        @NotNull(message = "权限列表不能为空")
        List<PermissionItem> permissions
) {

    /**
     * 单个权限勾选项。
     *
     * @param moduleCode 模块编码
     * @param moduleName 模块名称
     * @param permissionCode 权限编码
     * @param permissionName 权限名称
     * @param permissionType 权限类型：menu / button / data
     * @param sortOrder 排序值
     */
    public record PermissionItem(
            @NotBlank(message = "模块编码不能为空")
            @Size(max = 80, message = "模块编码最多80个字符")
            String moduleCode,

            @NotBlank(message = "模块名称不能为空")
            @Size(max = 120, message = "模块名称最多120个字符")
            String moduleName,

            @NotBlank(message = "权限编码不能为空")
            @Size(max = 120, message = "权限编码最多120个字符")
            String permissionCode,

            @NotBlank(message = "权限名称不能为空")
            @Size(max = 160, message = "权限名称最多160个字符")
            String permissionName,

            @Pattern(regexp = "menu|button|data", message = "权限类型不合法")
            String permissionType,

            @Min(value = 0, message = "排序不能小于0")
            Integer sortOrder
    ) {}
}
