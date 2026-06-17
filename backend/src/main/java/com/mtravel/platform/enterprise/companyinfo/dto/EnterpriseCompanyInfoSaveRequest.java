package com.mtravel.platform.enterprise.companyinfo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 企业公司信息保存请求。
 *
 * <p>公司信息按租户只维护一份，新增和修改共用该请求对象。客户合同会读取这些字段作为甲方默认值，
 * 但合同保存时仍会写入快照字段，避免公司资料调整影响已签合同。</p>
 */
public record EnterpriseCompanyInfoSaveRequest(
        @NotBlank(message = "公司名称不能为空")
        @Size(max = 200, message = "公司名称最多200个字符")
        String companyName,

        @Size(max = 80, message = "省份最多80个字符")
        String province,

        @Size(max = 80, message = "城市最多80个字符")
        String city,

        @Size(max = 80, message = "区县最多80个字符")
        String district,

        @Size(max = 80, message = "联系人最多80个字符")
        String contactName,

        @Size(max = 40, message = "联系电话最多40个字符")
        String contactPhone,

        @Size(max = 40, message = "传真最多40个字符")
        String faxNumber,

        @Size(max = 300, message = "办公地址最多300个字符")
        String officeAddress,

        @Size(max = 200, message = "企业支付宝主体最多200个字符")
        String alipayEnterpriseName,

        @Size(max = 160, message = "企业支付宝账号最多160个字符")
        String alipayAccount,

        @Size(max = 120, message = "企业支付宝昵称最多120个字符")
        String alipayNickname,

        @Pattern(regexp = "unsigned|signed", message = "签约状态不合法")
        String signStatus,

        String signLink,

        @Pattern(regexp = "active|disabled", message = "公司信息状态不合法")
        String status,

        String remark
) {}
