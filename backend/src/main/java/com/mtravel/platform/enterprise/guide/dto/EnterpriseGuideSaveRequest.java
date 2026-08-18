package com.mtravel.platform.enterprise.guide.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 企业导游档案保存请求。
 *
 * <p>用于新增和修改导游档案。导游端登录、排班、备用金和结算相关字段先作为基础档案维护，
 * 后续业务模块通过导游 ID 进行关联。</p>
 */
public record EnterpriseGuideSaveRequest(
        @Size(max = 80, message = "导游编码最多80个字符")
        String guideCode,

        @NotBlank(message = "导游名称不能为空")
        @Size(max = 80, message = "导游名称最多80个字符")
        String guideName,

        @Size(max = 80, message = "用户名最多80个字符")
        String username,

        /** 所属导管员工 ID，用于导游绩效和导管小组归属统计。 */
        Long guideManagerEmployeeId,

        /** 导游等级 ID，单选并参与导游报价规则匹配。 */
        Long guideLevelId,

        /** 导游标签 ID 集合，用于能力分类和后续排团筛选。 */
        List<Long> tagIds,

        @Pattern(regexp = "male|female|unknown", message = "导游性别不合法")
        String gender,

        @Size(max = 120, message = "证件号最多120个字符")
        String certificateNo,

        @Size(max = 120, message = "身份证号最多120个字符")
        String idCardNo,

        @Size(max = 40, message = "固定电话最多40个字符")
        String telephone,

        @Size(max = 40, message = "传真最多40个字符")
        String fax,

        @Size(max = 40, message = "手机号码最多40个字符")
        String mobilePhone,

        @Size(max = 120, message = "银行名称最多120个字符")
        String bankName,

        @Size(max = 120, message = "银行账号最多120个字符")
        String bankAccountNo,

        @Size(max = 80, message = "支付宝姓名最多80个字符")
        String alipayName,

        @Size(max = 200, message = "支付宝账号最多200个字符")
        String alipayAccount,

        @Size(max = 120, message = "企业码账号最多120个字符")
        String enterpriseCodeAccount,

        @Pattern(regexp = "not_joined|invite_link|signed_success|bound|unbound|disabled", message = "企业码状态不合法")
        String enterpriseCodeStatus,

        @Min(value = 0, message = "年龄不能小于0")
        Integer age,

        @Size(max = 120, message = "籍贯最多120个字符")
        String nativePlace,

        @Min(value = 0, message = "从业年数不能小于0")
        Integer workingYears,

        @Size(max = 200, message = "语言最多200个字符")
        String languages,

        String personalIntro,

        @Size(max = 500, message = "导游证书地址最多500个字符")
        String certificateFileUrl,

        @Size(max = 500, message = "个人照片地址最多500个字符")
        String photoUrl,

        @DecimalMin(value = "0", message = "导游评分不能小于0")
        @DecimalMax(value = "5", message = "导游评分不能大于5")
        BigDecimal rating,

        @Min(value = 0, message = "累计带团次数不能小于0")
        Integer totalTours,

        @Min(value = 0, message = "排序不能小于0")
        Integer sortOrder,

        @Pattern(regexp = "active|disabled", message = "导游状态不合法")
        String status,

        String remark
) {}
