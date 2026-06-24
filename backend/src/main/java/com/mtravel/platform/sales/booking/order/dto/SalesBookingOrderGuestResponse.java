package com.mtravel.platform.sales.booking.order.dto;

import com.mtravel.platform.sales.booking.order.entity.SalesBookingOrderGuestEntity;
import java.time.LocalDate;

/**
 * 收客订单游客名单返回对象。
 */
public record SalesBookingOrderGuestResponse(
        Long id,
        Integer indexNo,
        String guestName,
        String englishName,
        String certificateNo,
        String passportNo,
        String gender,
        LocalDate birthDate,
        Integer age,
        String phone,
        String guestType,
        String roomGroup,
        String roomRemark,
        Boolean leaderFlag,
        Boolean idCardValid,
        String idCardWarning,
        String remark
) {
    /** 将游客实体转换为接口返回对象。 */
    public static SalesBookingOrderGuestResponse fromEntity(SalesBookingOrderGuestEntity entity) {
        return new SalesBookingOrderGuestResponse(
                entity.getId(),
                entity.getIndexNo(),
                entity.getGuestName(),
                entity.getEnglishName(),
                entity.getCertificateNo(),
                entity.getPassportNo(),
                entity.getGender(),
                entity.getBirthDate(),
                entity.getAge(),
                entity.getPhone(),
                entity.getGuestType(),
                entity.getRoomGroup(),
                entity.getRoomRemark(),
                entity.getLeaderFlag(),
                entity.getIdCardValid(),
                entity.getIdCardWarning(),
                entity.getRemark()
        );
    }
}
