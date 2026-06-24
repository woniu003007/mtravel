package com.mtravel.platform.sales.booking.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.time.LocalDate;

/**
 * 收客订单游客名单实体，对应 sales_order_guests 表。
 *
 * <p>游客名单承载证件号、类型、分房、领队和身份证校验结果。团队实收人数由有效订单下
 * 未删除游客聚合得到，不能只依赖前端手填人数。</p>
 */
@TableName("sales_order_guests")
public class SalesBookingOrderGuestEntity extends TenantSoftDeleteEntity {

    /** 所属订单 ID。 */
    @TableField("order_id")
    private Long orderId;

    /** 所属团队 ID，冗余用于团队人数统计。 */
    @TableField("team_id")
    private Long teamId;

    /** 游客序号。 */
    @TableField("index_no")
    private Integer indexNo;

    /** 游客中文姓名。 */
    @TableField("guest_name")
    private String guestName;

    /** 英文姓名或拼音名。 */
    @TableField("english_name")
    private String englishName;

    /** 身份证号或主要证件号。 */
    @TableField("certificate_no")
    private String certificateNo;

    /** 护照号。 */
    @TableField("passport_no")
    private String passportNo;

    /** 性别。 */
    @TableField("gender")
    private String gender;

    /** 出生日期。 */
    @TableField("birth_date")
    private LocalDate birthDate;

    /** 年龄。 */
    @TableField("age")
    private Integer age;

    /** 联系电话。 */
    @TableField("phone")
    private String phone;

    /** 游客类型，例如 adult、child。 */
    @TableField("guest_type")
    private String guestType;

    /** 房间组号，例如 1房、2房。同住一间房的游客使用同一个组号。 */
    @TableField("room_group")
    private String roomGroup;

    /** 分房备注，例如房型、同住要求和特殊住宿说明。 */
    @TableField("room_remark")
    private String roomRemark;

    /** 是否领队。 */
    @TableField("leader_flag")
    private Boolean leaderFlag;

    /** 身份证程序校验是否通过。 */
    @TableField("id_card_valid")
    private Boolean idCardValid;

    /** 身份证校验提示。 */
    @TableField("id_card_warning")
    private String idCardWarning;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Integer getIndexNo() {
        return indexNo;
    }

    public void setIndexNo(Integer indexNo) {
        this.indexNo = indexNo;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getCertificateNo() {
        return certificateNo;
    }

    public void setCertificateNo(String certificateNo) {
        this.certificateNo = certificateNo;
    }

    public String getPassportNo() {
        return passportNo;
    }

    public void setPassportNo(String passportNo) {
        this.passportNo = passportNo;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGuestType() {
        return guestType;
    }

    public void setGuestType(String guestType) {
        this.guestType = guestType;
    }

    public String getRoomGroup() {
        return roomGroup;
    }

    public void setRoomGroup(String roomGroup) {
        this.roomGroup = roomGroup;
    }

    public String getRoomRemark() {
        return roomRemark;
    }

    public void setRoomRemark(String roomRemark) {
        this.roomRemark = roomRemark;
    }

    public Boolean getLeaderFlag() {
        return leaderFlag;
    }

    public void setLeaderFlag(Boolean leaderFlag) {
        this.leaderFlag = leaderFlag;
    }

    public Boolean getIdCardValid() {
        return idCardValid;
    }

    public void setIdCardValid(Boolean idCardValid) {
        this.idCardValid = idCardValid;
    }

    public String getIdCardWarning() {
        return idCardWarning;
    }

    public void setIdCardWarning(String idCardWarning) {
        this.idCardWarning = idCardWarning;
    }
}
