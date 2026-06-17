package com.mtravel.platform.enterprise.bankaccount.dto;

import com.mtravel.platform.enterprise.bankaccount.entity.EnterpriseBankAccountEntity;
import java.time.OffsetDateTime;

/**
 * 企业银行账号返回对象。
 *
 * <p>前端银行账号列表和编辑弹窗都使用该对象。字段保持接近页面展示口径，避免前端再猜测数据库含义。</p>
 */
public record EnterpriseBankAccountResponse(
        Long id,
        String bankName,
        String accountName,
        String accountNo,
        Boolean printEnabled,
        String otherInfo,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /** 将数据库实体转换为接口返回对象。 */
    public static EnterpriseBankAccountResponse fromEntity(EnterpriseBankAccountEntity entity) {
        return new EnterpriseBankAccountResponse(
                entity.getId(),
                entity.getBankName(),
                entity.getAccountName(),
                entity.getAccountNo(),
                entity.getPrintEnabled(),
                entity.getOtherInfo(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
