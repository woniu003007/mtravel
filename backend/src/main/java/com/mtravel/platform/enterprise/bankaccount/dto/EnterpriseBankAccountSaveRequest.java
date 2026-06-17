package com.mtravel.platform.enterprise.bankaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 企业银行账号保存请求。
 *
 * <p>新增和修改共用该请求对象。银行卡、支付宝、微信、房券、客户现付等账户类型差异较大，
 * 因此首版不强行拆账户类型枚举，统一维护开户行、户名、账号和其它说明。</p>
 */
public record EnterpriseBankAccountSaveRequest(
        @NotBlank(message = "开户行不能为空")
        @Size(max = 200, message = "开户行最多200个字符")
        String bankName,

        @NotBlank(message = "户名不能为空")
        @Size(max = 200, message = "户名最多200个字符")
        String accountName,

        @NotBlank(message = "账号不能为空")
        @Size(max = 200, message = "账号最多200个字符")
        String accountNo,

        Boolean printEnabled,

        String otherInfo,

        @Pattern(regexp = "active|disabled", message = "银行账号状态不合法")
        String status,

        String remark
) {}
