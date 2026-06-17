package com.mtravel.platform.customer.unit.dto;

import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 客户单位返回对象。
 *
 * <p>接口返回客户主档可展示字段，不暴露 tenant_id 和软删除控制字段，
 * 避免前端把系统隔离字段当成普通业务字段处理。</p>
 */
public record CustomerUnitResponse(
        Long id,
        String customerCode,
        String customerName,
        Long categoryId,
        String categoryName,
        BigDecimal creditLimit,
        String province,
        String city,
        String district,
        Long departmentId,
        String departmentName,
        Long dispatcherEmployeeId,
        String dispatcherName,
        String settlementMethod,
        LocalDate billStartDate,
        Integer billDay,
        String contactName,
        String contactPhone,
        String registrarName,
        LocalDate contractExpireDate,
        String status,
        String remark,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CustomerUnitResponse fromEntity(CustomerUnitEntity entity, String categoryName) {
        return new CustomerUnitResponse(
                entity.getId(),
                entity.getCustomerCode(),
                entity.getCustomerName(),
                entity.getCategoryId(),
                categoryName,
                entity.getCreditLimit(),
                entity.getProvince(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getDepartmentId(),
                entity.getDepartmentName(),
                entity.getDispatcherEmployeeId(),
                entity.getDispatcherName(),
                entity.getSettlementMethod(),
                entity.getBillStartDate(),
                entity.getBillDay(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getRegistrarName(),
                entity.getContractExpireDate(),
                entity.getStatus(),
                entity.getRemark(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
