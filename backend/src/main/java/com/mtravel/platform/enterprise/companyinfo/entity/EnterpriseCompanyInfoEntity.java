package com.mtravel.platform.enterprise.companyinfo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 企业公司信息实体。
 *
 * <p>该表保存当前租户自己的公司主体、联系人、办公地址和企业支付宝资料。客户合同生成时，
 * 甲方信息优先从这里带出，但允许业务人员在合同表单中手工修正快照，避免历史合同受主档变化影响。</p>
 */
@TableName("enterprise_company_infos")
public class EnterpriseCompanyInfoEntity extends TenantSoftDeleteEntity {

    /** 公司正式名称，用于合同甲方、确认件、企业资料展示。 */
    @TableField("company_name")
    private String companyName;

    /** 公司所在省份。 */
    @TableField("province")
    private String province;

    /** 公司所在城市。 */
    @TableField("city")
    private String city;

    /** 公司所在区县。 */
    @TableField("district")
    private String district;

    /** 公司联系人姓名。 */
    @TableField("contact_name")
    private String contactName;

    /** 公司联系电话。 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 公司传真号码。 */
    @TableField("fax_number")
    private String faxNumber;

    /** 公司办公地址。 */
    @TableField("office_address")
    private String officeAddress;

    /** 企业支付宝认证主体名称。 */
    @TableField("alipay_enterprise_name")
    private String alipayEnterpriseName;

    /** 企业支付宝账号。 */
    @TableField("alipay_account")
    private String alipayAccount;

    /** 企业支付宝昵称。 */
    @TableField("alipay_nickname")
    private String alipayNickname;

    /** 签约状态。unsigned 表示未签约，signed 表示已签约。 */
    @TableField("sign_status")
    private String signStatus;

    /** 签约链接或签约资料地址。 */
    @TableField("sign_link")
    private String signLink;

    /** 资料状态。active 表示启用，disabled 表示停用。 */
    @TableField("status")
    private String status;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getFaxNumber() {
        return faxNumber;
    }

    public void setFaxNumber(String faxNumber) {
        this.faxNumber = faxNumber;
    }

    public String getOfficeAddress() {
        return officeAddress;
    }

    public void setOfficeAddress(String officeAddress) {
        this.officeAddress = officeAddress;
    }

    public String getAlipayEnterpriseName() {
        return alipayEnterpriseName;
    }

    public void setAlipayEnterpriseName(String alipayEnterpriseName) {
        this.alipayEnterpriseName = alipayEnterpriseName;
    }

    public String getAlipayAccount() {
        return alipayAccount;
    }

    public void setAlipayAccount(String alipayAccount) {
        this.alipayAccount = alipayAccount;
    }

    public String getAlipayNickname() {
        return alipayNickname;
    }

    public void setAlipayNickname(String alipayNickname) {
        this.alipayNickname = alipayNickname;
    }

    public String getSignStatus() {
        return signStatus;
    }

    public void setSignStatus(String signStatus) {
        this.signStatus = signStatus;
    }

    public String getSignLink() {
        return signLink;
    }

    public void setSignLink(String signLink) {
        this.signLink = signLink;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
