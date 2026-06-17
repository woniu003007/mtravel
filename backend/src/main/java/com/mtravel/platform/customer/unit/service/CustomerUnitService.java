package com.mtravel.platform.customer.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.enums.CustomerCategoryStatus;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.customer.unit.dto.CustomerUnitCreateRequest;
import com.mtravel.platform.customer.unit.dto.CustomerUnitResponse;
import com.mtravel.platform.customer.unit.dto.CustomerUnitUpdateRequest;
import com.mtravel.platform.customer.unit.entity.CustomerUnitEntity;
import com.mtravel.platform.customer.unit.enums.CustomerSettlementMethod;
import com.mtravel.platform.customer.unit.enums.CustomerUnitStatus;
import com.mtravel.platform.customer.unit.mapper.CustomerUnitMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 客户单位业务服务。
 *
 * <p>这里集中处理客户单位主档的核心业务规则：租户隔离、软删除过滤、客户编码唯一、
 * 客户分类有效性和状态取值。客户合同、授信额度和应收账款属于后续独立业务模块，
 * 本服务只维护客户主档本身。</p>
 */
@Service
public class CustomerUnitService {

    private final CustomerUnitMapper customerMapper;
    private final CustomerCategoryMapper categoryMapper;

    public CustomerUnitService(CustomerUnitMapper customerMapper, CustomerCategoryMapper categoryMapper) {
        this.customerMapper = customerMapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * 分页查询客户单位。
     *
     * <p>所有查询必须带 tenantId 和 isDeleted=false，避免多租户数据串读。
     * keyword 覆盖客户名称、负责人和联系电话，其他筛选项按字段精确匹配。</p>
     */
    public PageResult<CustomerUnitResponse> page(
            Long tenantId,
            String keyword,
            String customerCode,
            Long categoryId,
            String status,
            String province,
            String city,
            String district,
            String departmentName,
            long page,
            long pageSize
    ) {
        CustomerUnitStatus parsedStatus = parseNullableStatus(status);
        LambdaQueryWrapper<CustomerUnitEntity> wrapper = baseQuery(tenantId)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(CustomerUnitEntity::getCustomerName, keyword)
                        .or()
                        .like(CustomerUnitEntity::getContactName, keyword)
                        .or()
                        .like(CustomerUnitEntity::getContactPhone, keyword))
                .like(StringUtils.hasText(customerCode), CustomerUnitEntity::getCustomerCode, customerCode)
                .eq(categoryId != null, CustomerUnitEntity::getCategoryId, categoryId)
                .eq(parsedStatus != null, CustomerUnitEntity::getStatus, parsedStatus == null ? null : parsedStatus.getValue())
                .eq(StringUtils.hasText(province), CustomerUnitEntity::getProvince, province)
                .eq(StringUtils.hasText(city), CustomerUnitEntity::getCity, city)
                .eq(StringUtils.hasText(district), CustomerUnitEntity::getDistrict, district)
                .eq(StringUtils.hasText(departmentName), CustomerUnitEntity::getDepartmentName, departmentName)
                .orderByDesc(CustomerUnitEntity::getId);

        Page<CustomerUnitEntity> result = customerMapper.selectPage(Page.of(page, pageSize), wrapper);
        Map<Long, String> categoryNames = categoryNameMap(tenantId, result.getRecords());
        List<CustomerUnitResponse> items = result.getRecords().stream()
                .map(entity -> CustomerUnitResponse.fromEntity(entity, categoryNames.get(entity.getCategoryId())))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /** 查询客户单位详情，查不到或已删除时返回业务异常。 */
    public CustomerUnitResponse detail(Long id, Long tenantId) {
        CustomerUnitEntity entity = customerMapper.selectOne(baseQuery(tenantId).eq(CustomerUnitEntity::getId, id));
        if (entity == null) {
            throw new BizException("客户单位不存在或已删除");
        }
        return CustomerUnitResponse.fromEntity(entity, categoryName(tenantId, entity.getCategoryId()));
    }

    /**
     * 新增客户单位。
     *
     * <p>客户编码允许为空；非空时需要先做服务层查重，数据库部分唯一索引用于兜底。
     * 分类允许为空；传入分类时必须确认属于当前租户、未删除且启用。</p>
     */
    public CustomerUnitResponse create(CustomerUnitCreateRequest request, Long tenantId, String operator) {
        CustomerUnitStatus status = CustomerUnitStatus.fromValueOrDefault(request.status());
        String customerCode = clean(request.customerCode());
        assertCustomerCodeNotExists(tenantId, customerCode, null);
        assertCategoryActive(tenantId, request.categoryId());

        CustomerUnitEntity entity = new CustomerUnitEntity();
        entity.setTenantId(tenantId);
        applyCreateFields(entity, request, status, customerCode);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        customerMapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改客户单位。
     *
     * <p>客户编码查重时排除当前记录，避免原编码不变时被误判重复。</p>
     */
    public CustomerUnitResponse update(Long id, CustomerUnitUpdateRequest request, Long tenantId) {
        CustomerUnitStatus status = CustomerUnitStatus.fromValueOrDefault(request.status());
        String customerCode = clean(request.customerCode());
        assertCustomerCodeNotExists(tenantId, customerCode, id);
        assertCategoryActive(tenantId, request.categoryId());

        CustomerUnitEntity entity = new CustomerUnitEntity();
        applyUpdateFields(entity, request, status, customerCode);

        int updated = customerMapper.update(entity, baseUpdate(tenantId).eq(CustomerUnitEntity::getId, id));
        if (updated == 0) {
            throw new BizException("客户单位不存在或已删除");
        }
        return detail(id, tenantId);
    }

    /**
     * 软删除客户单位。
     *
     * <p>客户单位可能被订单、合同、应收等历史数据引用，因此不能物理删除。
     * 常规列表通过 isDeleted=false 过滤已删除客户。</p>
     */
    public void delete(Long id, Long tenantId, String operator) {
        CustomerUnitEntity entity = new CustomerUnitEntity();
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);

        int updated = customerMapper.update(entity, baseUpdate(tenantId).eq(CustomerUnitEntity::getId, id));
        if (updated == 0) {
            throw new BizException("客户单位不存在或已删除");
        }
    }

    private void applyCreateFields(
            CustomerUnitEntity entity,
            CustomerUnitCreateRequest request,
            CustomerUnitStatus status,
            String customerCode
    ) {
        entity.setCustomerCode(customerCode);
        entity.setCustomerName(cleanRequired(request.customerName()));
        entity.setCategoryId(request.categoryId());
        entity.setCreditLimit(defaultCreditLimit(request.creditLimit()));
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setDepartmentId(request.departmentId());
        entity.setDepartmentName(clean(request.departmentName()));
        entity.setDispatcherEmployeeId(request.dispatcherEmployeeId());
        entity.setDispatcherName(clean(request.dispatcherName()));
        entity.setSettlementMethod(CustomerSettlementMethod.fromValueOrDefault(request.settlementMethod()).getValue());
        entity.setBillStartDate(request.billStartDate());
        entity.setBillDay(request.billDay());
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setRegistrarName(clean(request.registrarName()));
        entity.setContractExpireDate(request.contractExpireDate());
        entity.setStatus(status.getValue());
        entity.setRemark(request.remark());
    }

    private void applyUpdateFields(
            CustomerUnitEntity entity,
            CustomerUnitUpdateRequest request,
            CustomerUnitStatus status,
            String customerCode
    ) {
        entity.setCustomerCode(customerCode);
        entity.setCustomerName(cleanRequired(request.customerName()));
        entity.setCategoryId(request.categoryId());
        entity.setCreditLimit(defaultCreditLimit(request.creditLimit()));
        entity.setProvince(clean(request.province()));
        entity.setCity(clean(request.city()));
        entity.setDistrict(clean(request.district()));
        entity.setDepartmentId(request.departmentId());
        entity.setDepartmentName(clean(request.departmentName()));
        entity.setDispatcherEmployeeId(request.dispatcherEmployeeId());
        entity.setDispatcherName(clean(request.dispatcherName()));
        entity.setSettlementMethod(CustomerSettlementMethod.fromValueOrDefault(request.settlementMethod()).getValue());
        entity.setBillStartDate(request.billStartDate());
        entity.setBillDay(request.billDay());
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setRegistrarName(clean(request.registrarName()));
        entity.setContractExpireDate(request.contractExpireDate());
        entity.setStatus(status.getValue());
        entity.setRemark(request.remark());
    }

    private LambdaQueryWrapper<CustomerUnitEntity> baseQuery(Long tenantId) {
        return new LambdaQueryWrapper<CustomerUnitEntity>()
                .eq(CustomerUnitEntity::getTenantId, tenantId)
                .eq(CustomerUnitEntity::getIsDeleted, false);
    }

    private LambdaUpdateWrapper<CustomerUnitEntity> baseUpdate(Long tenantId) {
        return new LambdaUpdateWrapper<CustomerUnitEntity>()
                .eq(CustomerUnitEntity::getTenantId, tenantId)
                .eq(CustomerUnitEntity::getIsDeleted, false);
    }

    private LambdaQueryWrapper<CustomerCategoryEntity> categoryBaseQuery(Long tenantId) {
        return new LambdaQueryWrapper<CustomerCategoryEntity>()
                .eq(CustomerCategoryEntity::getTenantId, tenantId)
                .eq(CustomerCategoryEntity::getIsDeleted, false);
    }

    private void assertCustomerCodeNotExists(Long tenantId, String customerCode, Long excludeId) {
        if (!StringUtils.hasText(customerCode)) {
            return;
        }
        LambdaQueryWrapper<CustomerUnitEntity> wrapper = baseQuery(tenantId)
                .eq(CustomerUnitEntity::getCustomerCode, customerCode)
                .ne(excludeId != null, CustomerUnitEntity::getId, excludeId);
        Long count = customerMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("客户编码已存在");
        }
    }

    private void assertCategoryActive(Long tenantId, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        CustomerCategoryEntity category = categoryMapper.selectOne(categoryBaseQuery(tenantId)
                .eq(CustomerCategoryEntity::getId, categoryId));
        if (category == null) {
            throw new BizException("客户分类不存在或已删除");
        }
        if (!CustomerCategoryStatus.ACTIVE.getValue().equals(category.getStatus())) {
            throw new BizException("客户分类已停用");
        }
    }

    private Map<Long, String> categoryNameMap(Long tenantId, List<CustomerUnitEntity> customers) {
        List<Long> categoryIds = customers.stream()
                .map(CustomerUnitEntity::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectList(categoryBaseQuery(tenantId).in(CustomerCategoryEntity::getId, categoryIds))
                .stream()
                .collect(Collectors.toMap(CustomerCategoryEntity::getId, CustomerCategoryEntity::getCategoryName));
    }

    private String categoryName(Long tenantId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Map<Long, String> names = categoryMapper.selectList(categoryBaseQuery(tenantId)
                        .eq(CustomerCategoryEntity::getId, categoryId))
                .stream()
                .collect(Collectors.toMap(CustomerCategoryEntity::getId, CustomerCategoryEntity::getCategoryName));
        return names.get(categoryId);
    }

    private CustomerUnitStatus parseNullableStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return CustomerUnitStatus.fromValueOrDefault(status);
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal defaultCreditLimit(BigDecimal creditLimit) {
        return creditLimit == null ? BigDecimal.ZERO : creditLimit;
    }
}
