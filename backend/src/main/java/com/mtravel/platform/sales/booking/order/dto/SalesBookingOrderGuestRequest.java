package com.mtravel.platform.sales.booking.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 收客订单游客名单保存请求。
 *
 * @param id 游客 ID，当前版本保存时以订单为单位重建，允许为空。
 * @param indexNo 页面序号。
 * @param guestName 游客姓名。
 * @param englishName 英文姓名或拼音名。
 * @param certificateNo 身份证号或主要证件号。
 * @param passportNo 护照号。
 * @param gender 性别。
 * @param birthDate 出生日期。
 * @param age 年龄。
 * @param phone 联系电话。
 * @param guestType 游客类型。
 * @param roomGroup 房间组号，同住一间房的游客使用同一个组号。
 * @param roomRemark 分房备注，保存房型、同住要求和特殊住宿说明。
 * @param leaderFlag 是否领队。
 * @param remark 游客备注。
 */
public record SalesBookingOrderGuestRequest(
        Long id,
        Integer indexNo,
        @Size(max = 80, message = "游客姓名不能超过80个字符")
        String guestName,
        @Size(max = 120, message = "英文姓名不能超过120个字符")
        String englishName,
        @Size(max = 80, message = "证件号不能超过80个字符")
        String certificateNo,
        @Size(max = 80, message = "护照号不能超过80个字符")
        String passportNo,
        @Size(max = 20, message = "性别不能超过20个字符")
        String gender,
        LocalDate birthDate,
        @Min(value = 0, message = "年龄不能小于0")
        Integer age,
        @Size(max = 40, message = "联系电话不能超过40个字符")
        String phone,
        @Size(max = 30, message = "游客类型不能超过30个字符")
        String guestType,
        @Size(max = 120, message = "房间组号不能超过120个字符")
        String roomGroup,
        @Size(max = 200, message = "分房备注不能超过200个字符")
        String roomRemark,
        Boolean leaderFlag,
        String remark
) {
}
