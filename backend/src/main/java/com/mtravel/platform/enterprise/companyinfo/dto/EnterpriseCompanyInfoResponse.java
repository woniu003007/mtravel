package com.mtravel.platform.enterprise.companyinfo.dto;

import com.mtravel.platform.enterprise.companyinfo.entity.EnterpriseCompanyInfoEntity;
import java.time.OffsetDateTime;

/**
 * 企业公司信息返回对象。
 *
 * <p>前端公司信息页和客户合同甲方自动带入都使用该对象。返回字段保持业务口径，
 * 不暴露软删除字段，避免页面误操作历史资料。</p>
 */
public record EnterpriseCompanyInfoResponse(
        Long id,
        String companyName,
        String province,
        String city,
        String district,
        String contactName,
        String contactPhone,
        String faxNumber,
        String officeAddress,
        String alipayEnterpriseName,
        String alipayAccount,
        String alipayNickname,
        String signStatus,
        String signLink,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将数据库实体转换为接口返回对象。 */
    public static EnterpriseCompanyInfoResponse fromEntity(EnterpriseCompanyInfoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new EnterpriseCompanyInfoResponse(
                entity.getId(),
                entity.getCompanyName(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getFaxNumber(),
                entity.getOfficeAddress(),
                entity.getAlipayEnterpriseName(),
                entity.getAlipayAccount(),
                entity.getAlipayNickname(),
                entity.getSignStatus(),
                entity.getSignLink(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
