package com.mtravel.platform.enterprise.employee.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 企业员工保存请求。
 *
 * <p>员工和登录账号一体管理。新增员工时 username 会创建登录账号；修改员工时会同步账号姓名、手机号、邮箱和角色。</p>
 */
public record EnterpriseEmployeeSaveRequest(
        @Size(max = 80, message = "员工编码最多80个字符")
        String employeeCode,

        @NotBlank(message = "员工名称不能为空")
        @Size(max = 80, message = "员工名称最多80个字符")
        String employeeName,

        @NotBlank(message = "登录账号不能为空")
        @Size(max = 80, message = "登录账号最多80个字符")
        String username,

        @NotNull(message = "所属部门不能为空")
        Long departmentId,

        @NotNull(message = "角色不能为空")
        Long roleId,

        @Pattern(regexp = "male|female|unknown", message = "员工性别不合法")
        String gender,

        @Size(max = 40, message = "固定电话最多40个字符")
        String telephone,

        @Size(max = 40, message = "手机号码最多40个字符")
        String mobilePhone,

        @Size(max = 120, message = "邮箱最多120个字符")
        String email,

        @Pattern(regexp = "company|department|personal", message = "信息查看范围不合法")
        String infoScope,

        @Pattern(regexp = "company|department|personal", message = "利润查看范围不合法")
        String profitScope,

        @Pattern(regexp = "company|department|personal", message = "收客查看范围不合法")
        String receptionScope,

        @Pattern(regexp = "company|department|personal", message = "客户查看范围不合法")
        String customerScope,

        @Min(value = 0, message = "排序不能小于0")
        Integer sortOrder,

        @Pattern(regexp = "active|disabled", message = "员工状态不合法")
        String status,

        String remark
) {}
