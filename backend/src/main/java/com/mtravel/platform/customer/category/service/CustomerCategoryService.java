package com.mtravel.platform.customer.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.category.dto.CustomerCategoryCreateRequest;
import com.mtravel.platform.customer.category.dto.CustomerCategoryResponse;
import com.mtravel.platform.customer.category.dto.CustomerCategoryUpdateRequest;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.enums.CustomerCategoryStatus;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 客户分类业务服务。
 *
 * <p>这里集中处理客户分类的核心业务规则：租户隔离、软删除过滤、名称唯一、状态取值。
 * Controller 只负责接收请求，Mapper 只负责数据库访问，业务判断不要散落在接口层。</p>
 */
@Service
public class CustomerCategoryService {

    private final CustomerCategoryMapper mapper;

    public CustomerCategoryService(CustomerCategoryMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 分页查询客户分类。
     *
     * <p>所有查询必须带 tenantId 和 isDeleted=false，这是多租户系统的基本安全边界。
     * keyword 只查分类名称，status 为空时查全部状态。</p>
     */
    public PageResult<CustomerCategoryResponse> page(Long tenantId, String keyword, String status, long page, long pageSize) {
        LambdaQueryWrapper<CustomerCategoryEntity> wrapper = baseQuery(tenantId)
                .like(StringUtils.hasText(keyword), CustomerCategoryEntity::getCategoryName, keyword)
                .eq(StringUtils.hasText(status), CustomerCategoryEntity::getStatus, status)
                .orderByAsc(CustomerCategoryEntity::getSortOrder)
                .orderByDesc(CustomerCategoryEntity::getId);

        Page<CustomerCategoryEntity> result = mapper.selectPage(Page.of(page, pageSize), wrapper);
        List<CustomerCategoryResponse> items = result.getRecords().stream()
                .map(CustomerCategoryResponse::fromEntity)
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /**
     * 查询启用分类列表，用于客户表单下拉框。
     *
     * <p>下拉框只返回 active 分类，disabled 分类保留历史数据但不建议继续选择。</p>
     */
    public List<CustomerCategoryResponse> listActive(Long tenantId) {
        return mapper.selectList(baseQuery(tenantId)
                        .eq(CustomerCategoryEntity::getStatus, CustomerCategoryStatus.ACTIVE.getValue())
                        .orderByAsc(CustomerCategoryEntity::getSortOrder)
                        .orderByDesc(CustomerCategoryEntity::getId))
                .stream()
                .map(CustomerCategoryResponse::fromEntity)
                .toList();
    }

    /** 查询客户分类详情，查不到或已删除时返回业务异常。 */
    public CustomerCategoryResponse detail(Long id, Long tenantId) {
        CustomerCategoryEntity entity = mapper.selectOne(baseQuery(tenantId).eq(CustomerCategoryEntity::getId, id));
        if (entity == null) {
            throw new BizException("客户分类不存在或已删除");
        }
        return CustomerCategoryResponse.fromEntity(entity);
    }

    /**
     * 新增客户分类。
     *
     * <p>新增前先检查同租户、未删除分类名称是否重复。数据库已有部分唯一索引兜底，
     * 但服务层提前校验可以给前端更明确的错误提示。</p>
     */
    public CustomerCategoryResponse create(CustomerCategoryCreateRequest request, Long tenantId, String operator) {
        CustomerCategoryStatus status = CustomerCategoryStatus.fromValueOrDefault(request.status());
        assertNameNotExists(tenantId, request.categoryName(), null);

        CustomerCategoryEntity entity = new CustomerCategoryEntity();
        entity.setTenantId(tenantId);
        entity.setCategoryName(request.categoryName());
        entity.setDefaultCreditLimit(defaultCreditLimit(request.defaultCreditLimit()));
        entity.setSortOrder(defaultSortOrder(request.sortOrder()));
        entity.setStatus(status.getValue());
        entity.setCreatedBy(operator);
        entity.setRemark(request.remark());
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改客户分类。
     *
     * <p>修改名称时要排除当前记录再查重，否则原名称不变也会被误判为重复。</p>
     */
    public CustomerCategoryResponse update(Long id, CustomerCategoryUpdateRequest request, Long tenantId) {
        CustomerCategoryStatus status = CustomerCategoryStatus.fromValueOrDefault(request.status());
        assertNameNotExists(tenantId, request.categoryName(), id);

        CustomerCategoryEntity entity = new CustomerCategoryEntity();
        entity.setCategoryName(request.categoryName());
        entity.setDefaultCreditLimit(defaultCreditLimit(request.defaultCreditLimit()));
        entity.setSortOrder(defaultSortOrder(request.sortOrder()));
        entity.setStatus(status.getValue());
        entity.setRemark(request.remark());

        int updated = mapper.update(entity, baseUpdate(tenantId).eq(CustomerCategoryEntity::getId, id));
        if (updated == 0) {
            throw new BizException("客户分类不存在或已删除");
        }
        return detail(id, tenantId);
    }

    /**
     * 删除客户分类。
     *
     * <p>客户分类可能被历史客户引用，因此不能物理删除。这里统一更新软删除字段，
     * 常规查询通过 isDeleted=false 自动过滤。</p>
     */
    public void delete(Long id, Long tenantId, String operator) {
        CustomerCategoryEntity entity = new CustomerCategoryEntity();
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);

        int updated = mapper.update(entity, baseUpdate(tenantId).eq(CustomerCategoryEntity::getId, id));
        if (updated == 0) {
            throw new BizException("客户分类不存在或已删除");
        }
    }

    private LambdaQueryWrapper<CustomerCategoryEntity> baseQuery(Long tenantId) {
        return new LambdaQueryWrapper<CustomerCategoryEntity>()
                .eq(CustomerCategoryEntity::getTenantId, tenantId)
                .eq(CustomerCategoryEntity::getIsDeleted, false);
    }

    private LambdaUpdateWrapper<CustomerCategoryEntity> baseUpdate(Long tenantId) {
        return new LambdaUpdateWrapper<CustomerCategoryEntity>()
                .eq(CustomerCategoryEntity::getTenantId, tenantId)
                .eq(CustomerCategoryEntity::getIsDeleted, false);
    }

    private void assertNameNotExists(Long tenantId, String categoryName, Long excludeId) {
        LambdaQueryWrapper<CustomerCategoryEntity> wrapper = baseQuery(tenantId)
                .eq(CustomerCategoryEntity::getCategoryName, categoryName)
                .ne(excludeId != null, CustomerCategoryEntity::getId, excludeId);
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("客户分类名称已存在");
        }
    }

    private Integer defaultSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private BigDecimal defaultCreditLimit(BigDecimal creditLimit) {
        return creditLimit == null ? BigDecimal.ZERO : creditLimit;
    }
}
