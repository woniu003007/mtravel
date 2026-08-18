package com.mtravel.platform.enterprise.guide.dto;

import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 企业导游档案返回对象。
 *
 * <p>导游列表和详情共用该对象，字段覆盖旧系统列表字段和后续导游端报账、备用金、结算所需基础信息。</p>
 */
public record EnterpriseGuideResponse(
        Long id,
        String guideCode,
        String guideName,
        String username,
        Long guideManagerEmployeeId,
        String guideManagerName,
        Long guideLevelId,
        String guideLevelName,
        List<EnterpriseGuideTagResponse> tags,
        List<Long> tagIds,
        String gender,
        String certificateNo,
        String idCardNo,
        String telephone,
        String fax,
        String mobilePhone,
        String bankName,
        String bankAccountNo,
        String alipayName,
        String alipayAccount,
        String enterpriseCodeAccount,
        String enterpriseCodeStatus,
        OffsetDateTime enterpriseCodeInvitedAt,
        String status,
        Integer age,
        String nativePlace,
        Integer workingYears,
        String languages,
        String personalIntro,
        String certificateFileUrl,
        String photoUrl,
        BigDecimal rating,
        Integer totalTours,
        Integer sortOrder,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将导游实体转换为接口返回对象。 */
    public static EnterpriseGuideResponse fromEntity(EnterpriseGuideEntity entity) {
        return fromEntity(entity, List.of());
    }

    /** 将导游实体和标签列表转换为接口返回对象。 */
    public static EnterpriseGuideResponse fromEntity(
            EnterpriseGuideEntity entity,
            List<EnterpriseGuideTagResponse> tags
    ) {
        List<EnterpriseGuideTagResponse> safeTags = tags == null ? List.of() : tags;
        return new EnterpriseGuideResponse(
                entity.getId(),
                entity.getGuideCode(),
                entity.getGuideName(),
                entity.getUsername(),
                entity.getGuideManagerEmployeeId(),
                entity.getGuideManagerName(),
                entity.getGuideLevelId(),
                entity.getGuideLevelName(),
                safeTags,
                safeTags.stream().map(EnterpriseGuideTagResponse::id).toList(),
                entity.getGender(),
                entity.getCertificateNo(),
                entity.getIdCardNo(),
                entity.getTelephone(),
                entity.getFax(),
                entity.getMobilePhone(),
                entity.getBankName(),
                entity.getBankAccountNo(),
                entity.getAlipayName(),
                entity.getAlipayAccount(),
                entity.getEnterpriseCodeAccount(),
                entity.getEnterpriseCodeStatus(),
                entity.getEnterpriseCodeInvitedAt(),
                entity.getStatus(),
                entity.getAge(),
                entity.getNativePlace(),
                entity.getWorkingYears(),
                entity.getLanguages(),
                entity.getPersonalIntro(),
                entity.getCertificateFileUrl(),
                entity.getPhotoUrl(),
                entity.getRating(),
                entity.getTotalTours(),
                entity.getSortOrder(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
