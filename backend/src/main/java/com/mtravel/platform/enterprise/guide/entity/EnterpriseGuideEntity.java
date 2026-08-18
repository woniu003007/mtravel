package com.mtravel.platform.enterprise.guide.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 企业导游档案实体。
 *
 * <p>导游档案是团队安排、导游端报账、备用金核销和导游结算的独立主体。
 * 它不和员工表合并，避免后台员工账号和外部导游资料互相污染。</p>
 */
@TableName("enterprise_guides")
public class EnterpriseGuideEntity extends TenantSoftDeleteEntity {

    /** 导游编码，用于内部识别、导入匹配和团队安排引用。 */
    @TableField("guide_code")
    private String guideCode;

    /** 导游姓名，用于排团、结算、统计和前端列表展示。 */
    @TableField("guide_name")
    private String guideName;

    /** 导游端登录用户名或外部系统用户名。 */
    @TableField("username")
    private String username;

    /** 所属导管员工 ID，用于绩效归属和导管小组统计。 */
    @TableField("guide_manager_employee_id")
    private Long guideManagerEmployeeId;

    /** 所属导管员工姓名冗余，用于列表快速展示和历史归属留痕。 */
    @TableField("guide_manager_name")
    private String guideManagerName;

    /** 导游等级 ID，用于导游报价规则匹配。 */
    @TableField("guide_level_id")
    private Long guideLevelId;

    /** 导游等级名称快照，用于列表快速展示。 */
    @TableField("guide_level_name")
    private String guideLevelName;

    /** 性别。male 男，female 女，unknown 未填写。 */
    @TableField("gender")
    private String gender;

    /** 导游证件号或导游证编号。 */
    @TableField("certificate_no")
    private String certificateNo;

    /** 身份证号或身份识别号码。 */
    @TableField("id_card_no")
    private String idCardNo;

    /** 固定电话。 */
    @TableField("telephone")
    private String telephone;

    /** 传真号码。 */
    @TableField("fax")
    private String fax;

    /** 手机号码，用于计调联系和导游端身份匹配。 */
    @TableField("mobile_phone")
    private String mobilePhone;

    /** 银行名称，用于导游结算付款资料。 */
    @TableField("bank_name")
    private String bankName;

    /** 银行账号，用于导游结算付款资料。 */
    @TableField("bank_account_no")
    private String bankAccountNo;

    /** 支付宝姓名，用于备用金发放、核销退补或导游结算付款。 */
    @TableField("alipay_name")
    private String alipayName;

    /** 支付宝账号，用于备用金发放、核销退补或导游结算付款。 */
    @TableField("alipay_account")
    private String alipayAccount;

    /** 企业码账号或企业码绑定标识。 */
    @TableField("enterprise_code_account")
    private String enterpriseCodeAccount;

    /** 企业码状态。bound 已绑定，unbound 未绑定，disabled 停用。 */
    @TableField("enterprise_code_status")
    private String enterpriseCodeStatus;

    /** 企业码邀请时间，用于记录最近一次发送签约链接的时间。 */
    @TableField("enterprise_code_invited_at")
    private OffsetDateTime enterpriseCodeInvitedAt;

    /** 导游档案状态。active 启用，disabled 停用。 */
    @TableField("status")
    private String status;

    /** 年龄，用于导游展示资料和团队匹配参考。 */
    @TableField("age")
    private Integer age;

    /** 籍贯，用于导游展示资料。 */
    @TableField("native_place")
    private String nativePlace;

    /** 从业年数，用于导游展示资料和经验判断。 */
    @TableField("working_years")
    private Integer workingYears;

    /** 语言能力，用逗号或文本记录，例如普通话、英语。 */
    @TableField("languages")
    private String languages;

    /** 个人介绍，用于导游展示资料。 */
    @TableField("personal_intro")
    private String personalIntro;

    /** 导游证书附件地址。 */
    @TableField("certificate_file_url")
    private String certificateFileUrl;

    /** 个人照片地址，用于导游展示资料。 */
    @TableField("photo_url")
    private String photoUrl;

    /** 导游评分，0 到 5，用于后续排班参考和导游统计。 */
    @TableField("rating")
    private BigDecimal rating;

    /** 累计带团次数，用于后续导游统计和排班参考。 */
    @TableField("total_tours")
    private Integer totalTours;

    /** 排序值。数字越小越靠前。 */
    @TableField("sort_order")
    private Integer sortOrder;

    public String getGuideCode() {
        return guideCode;
    }

    public void setGuideCode(String guideCode) {
        this.guideCode = guideCode;
    }

    public String getGuideName() {
        return guideName;
    }

    public void setGuideName(String guideName) {
        this.guideName = guideName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getGuideManagerEmployeeId() {
        return guideManagerEmployeeId;
    }

    public void setGuideManagerEmployeeId(Long guideManagerEmployeeId) {
        this.guideManagerEmployeeId = guideManagerEmployeeId;
    }

    public String getGuideManagerName() {
        return guideManagerName;
    }

    public void setGuideManagerName(String guideManagerName) {
        this.guideManagerName = guideManagerName;
    }

    public Long getGuideLevelId() {
        return guideLevelId;
    }

    public void setGuideLevelId(Long guideLevelId) {
        this.guideLevelId = guideLevelId;
    }

    public String getGuideLevelName() {
        return guideLevelName;
    }

    public void setGuideLevelName(String guideLevelName) {
        this.guideLevelName = guideLevelName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCertificateNo() {
        return certificateNo;
    }

    public void setCertificateNo(String certificateNo) {
        this.certificateNo = certificateNo;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccountNo() {
        return bankAccountNo;
    }

    public void setBankAccountNo(String bankAccountNo) {
        this.bankAccountNo = bankAccountNo;
    }

    public String getAlipayName() {
        return alipayName;
    }

    public void setAlipayName(String alipayName) {
        this.alipayName = alipayName;
    }

    public String getAlipayAccount() {
        return alipayAccount;
    }

    public void setAlipayAccount(String alipayAccount) {
        this.alipayAccount = alipayAccount;
    }

    public String getEnterpriseCodeAccount() {
        return enterpriseCodeAccount;
    }

    public void setEnterpriseCodeAccount(String enterpriseCodeAccount) {
        this.enterpriseCodeAccount = enterpriseCodeAccount;
    }

    public String getEnterpriseCodeStatus() {
        return enterpriseCodeStatus;
    }

    public void setEnterpriseCodeStatus(String enterpriseCodeStatus) {
        this.enterpriseCodeStatus = enterpriseCodeStatus;
    }

    public OffsetDateTime getEnterpriseCodeInvitedAt() {
        return enterpriseCodeInvitedAt;
    }

    public void setEnterpriseCodeInvitedAt(OffsetDateTime enterpriseCodeInvitedAt) {
        this.enterpriseCodeInvitedAt = enterpriseCodeInvitedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getNativePlace() {
        return nativePlace;
    }

    public void setNativePlace(String nativePlace) {
        this.nativePlace = nativePlace;
    }

    public Integer getWorkingYears() {
        return workingYears;
    }

    public void setWorkingYears(Integer workingYears) {
        this.workingYears = workingYears;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public String getPersonalIntro() {
        return personalIntro;
    }

    public void setPersonalIntro(String personalIntro) {
        this.personalIntro = personalIntro;
    }

    public String getCertificateFileUrl() {
        return certificateFileUrl;
    }

    public void setCertificateFileUrl(String certificateFileUrl) {
        this.certificateFileUrl = certificateFileUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getTotalTours() {
        return totalTours;
    }

    public void setTotalTours(Integer totalTours) {
        this.totalTours = totalTours;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
