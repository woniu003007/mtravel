package com.mtravel.platform.customer.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.category.dto.CustomerCategoryCreateRequest;
import com.mtravel.platform.customer.category.dto.CustomerCategoryApprovalMemberRequest;
import com.mtravel.platform.customer.category.dto.CustomerCategoryApprovalMemberResponse;
import com.mtravel.platform.customer.category.dto.CustomerCategoryResponse;
import com.mtravel.platform.customer.category.dto.CustomerCategoryUpdateRequest;
import com.mtravel.platform.customer.category.entity.CustomerCategoryApprovalMemberEntity;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.enums.CustomerCategoryStatus;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryApprovalMemberMapper;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final CustomerCategoryApprovalMemberMapper memberMapper;
    private final SystemUserMapper userMapper;

    public CustomerCategoryService(CustomerCategoryMapper mapper) {
        this(mapper, null, null);
    }

    @Autowired
    public CustomerCategoryService(
            CustomerCategoryMapper mapper,
            CustomerCategoryApprovalMemberMapper memberMapper,
            SystemUserMapper userMapper
    ) {
        this.mapper = mapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
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
        List<CustomerCategoryResponse> items = toResponses(result.getRecords(), tenantId);
        return new PageResult<>(items, result.getTotal());
    }

    /**
     * 查询启用分类列表，用于客户表单下拉框。
     *
     * <p>下拉框只返回 active 分类，disabled 分类保留历史数据但不建议继续选择。</p>
     */
    public List<CustomerCategoryResponse> listActive(Long tenantId) {
        List<CustomerCategoryEntity> entities = mapper.selectList(baseQuery(tenantId)
                        .eq(CustomerCategoryEntity::getStatus, CustomerCategoryStatus.ACTIVE.getValue())
                        .orderByAsc(CustomerCategoryEntity::getSortOrder)
                        .orderByDesc(CustomerCategoryEntity::getId));
        return toResponses(entities, tenantId);
    }

    /** 查询客户分类详情，查不到或已删除时返回业务异常。 */
    public CustomerCategoryResponse detail(Long id, Long tenantId) {
        CustomerCategoryEntity entity = mapper.selectOne(baseQuery(tenantId).eq(CustomerCategoryEntity::getId, id));
        if (entity == null) {
            throw new BizException("客户分类不存在或已删除");
        }
        return toResponses(List.of(entity), tenantId).getFirst();
    }

    /**
     * 新增客户分类。
     *
     * <p>新增前先检查同租户、未删除分类名称是否重复。数据库已有部分唯一索引兜底，
     * 但服务层提前校验可以给前端更明确的错误提示。</p>
     */
    @Transactional
    public CustomerCategoryResponse create(CustomerCategoryCreateRequest request, Long tenantId, String operator) {
        CustomerCategoryStatus status = CustomerCategoryStatus.fromValueOrDefault(request.status());
        assertNameNotExists(tenantId, request.categoryName(), null);

        CustomerCategoryEntity entity = new CustomerCategoryEntity();
        entity.setTenantId(tenantId);
        entity.setCategoryName(request.categoryName());
        entity.setDefaultCreditLimit(defaultCreditLimit(request.defaultCreditLimit()));
        entity.setCreditTermDays(defaultCreditTermDays(request.creditTermDays()));
        entity.setAllowOverLimit(Boolean.TRUE.equals(request.allowOverLimit()));
        entity.setSortOrder(defaultSortOrder(request.sortOrder()));
        entity.setStatus(status.getValue());
        entity.setCreatedBy(operator);
        entity.setRemark(request.remark());
        entity.setIsDeleted(false);
        mapper.insert(entity);
        replaceApprovalMembers(
                tenantId,
                entity.getId(),
                entity.getAllowOverLimit(),
                request.approvers(),
                request.ccUsers(),
                operator
        );
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改客户分类。
     *
     * <p>修改名称时要排除当前记录再查重，否则原名称不变也会被误判为重复。</p>
     */
    @Transactional
    public CustomerCategoryResponse update(Long id, CustomerCategoryUpdateRequest request, Long tenantId, String operator) {
        CustomerCategoryStatus status = CustomerCategoryStatus.fromValueOrDefault(request.status());
        assertNameNotExists(tenantId, request.categoryName(), id);

        CustomerCategoryEntity entity = new CustomerCategoryEntity();
        entity.setCategoryName(request.categoryName());
        entity.setDefaultCreditLimit(defaultCreditLimit(request.defaultCreditLimit()));
        entity.setCreditTermDays(defaultCreditTermDays(request.creditTermDays()));
        entity.setAllowOverLimit(Boolean.TRUE.equals(request.allowOverLimit()));
        entity.setSortOrder(defaultSortOrder(request.sortOrder()));
        entity.setStatus(status.getValue());
        entity.setRemark(request.remark());

        int updated = mapper.update(entity, baseUpdate(tenantId).eq(CustomerCategoryEntity::getId, id));
        if (updated == 0) {
            throw new BizException("客户分类不存在或已删除");
        }
        replaceApprovalMembers(
                tenantId,
                id,
                Boolean.TRUE.equals(request.allowOverLimit()),
                request.approvers(),
                request.ccUsers(),
                operator
        );
        return detail(id, tenantId);
    }

    /** 兼容内部测试和旧调用，正式接口应传入当前操作人。 */
    public CustomerCategoryResponse update(Long id, CustomerCategoryUpdateRequest request, Long tenantId) {
        return update(id, request, tenantId, "system");
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

    private Integer defaultCreditTermDays(Integer creditTermDays) {
        return creditTermDays == null ? 0 : creditTermDays;
    }

    /**
     * 批量组装等级及审批人员，避免列表按等级逐条查询人员和账号形成 N+1。
     */
    private List<CustomerCategoryResponse> toResponses(List<CustomerCategoryEntity> entities, Long tenantId) {
        if (entities.isEmpty()) {
            return List.of();
        }
        if (memberMapper == null || userMapper == null) {
            return entities.stream().map(CustomerCategoryResponse::fromEntity).toList();
        }
        List<Long> categoryIds = entities.stream()
                .map(CustomerCategoryEntity::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (categoryIds.isEmpty()) {
            return entities.stream().map(CustomerCategoryResponse::fromEntity).toList();
        }
        List<CustomerCategoryApprovalMemberEntity> members = memberMapper.selectList(
                new LambdaQueryWrapper<CustomerCategoryApprovalMemberEntity>()
                        .eq(CustomerCategoryApprovalMemberEntity::getTenantId, tenantId)
                        .in(CustomerCategoryApprovalMemberEntity::getCategoryId, categoryIds)
                        .eq(CustomerCategoryApprovalMemberEntity::getIsDeleted, false)
                        .orderByAsc(CustomerCategoryApprovalMemberEntity::getCategoryId)
                        .orderByAsc(CustomerCategoryApprovalMemberEntity::getMemberType)
                        .orderByAsc(CustomerCategoryApprovalMemberEntity::getStepOrder)
                        .orderByAsc(CustomerCategoryApprovalMemberEntity::getId)
        );
        if (members.isEmpty()) {
            return entities.stream().map(CustomerCategoryResponse::fromEntity).toList();
        }
        Set<Long> userIds = members.stream()
                .map(CustomerCategoryApprovalMemberEntity::getSystemUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SystemUserEntity> users = userMapper.selectList(
                        new LambdaQueryWrapper<SystemUserEntity>()
                                .eq(SystemUserEntity::getTenantId, tenantId)
                                .eq(SystemUserEntity::getIsDeleted, false)
                                .in(SystemUserEntity::getId, userIds)
                ).stream()
                .collect(Collectors.toMap(SystemUserEntity::getId, Function.identity()));
        Map<Long, List<CustomerCategoryApprovalMemberEntity>> membersByCategory = members.stream()
                .collect(Collectors.groupingBy(CustomerCategoryApprovalMemberEntity::getCategoryId));
        return entities.stream().map(entity -> {
            List<CustomerCategoryApprovalMemberEntity> categoryMembers = membersByCategory
                    .getOrDefault(entity.getId(), List.of());
            List<CustomerCategoryApprovalMemberResponse> approvers = memberResponses(
                    categoryMembers, users, "approver");
            List<CustomerCategoryApprovalMemberResponse> ccUsers = memberResponses(categoryMembers, users, "cc");
            return CustomerCategoryResponse.fromEntity(entity, approvers, ccUsers);
        }).toList();
    }

    private List<CustomerCategoryApprovalMemberResponse> memberResponses(
            List<CustomerCategoryApprovalMemberEntity> members,
            Map<Long, SystemUserEntity> users,
            String memberType
    ) {
        return members.stream()
                .filter(member -> memberType.equals(member.getMemberType()))
                .map(member -> {
                    SystemUserEntity user = users.get(member.getSystemUserId());
                    return new CustomerCategoryApprovalMemberResponse(
                            member.getSystemUserId(),
                            user == null ? "已停用人员" : user.getRealName(),
                            user == null ? null : user.getUsername(),
                            member.getStepOrder()
                    );
                })
                .toList();
    }

    private void replaceApprovalMembers(
            Long tenantId,
            Long categoryId,
            boolean allowOverLimit,
            List<CustomerCategoryApprovalMemberRequest> approvers,
            List<CustomerCategoryApprovalMemberRequest> ccUsers,
            String operator
    ) {
        if (memberMapper == null || userMapper == null) {
            return;
        }
        List<CustomerCategoryApprovalMemberRequest> safeApprovers = approvers == null ? List.of() : approvers;
        List<CustomerCategoryApprovalMemberRequest> safeCcUsers = ccUsers == null ? List.of() : ccUsers;
        if (allowOverLimit && safeApprovers.isEmpty()) {
            throw new BizException("允许授信超额时至少需要指定一名审批人");
        }
        Set<Long> approverIds = uniqueIds(safeApprovers, "审批人不能重复");
        Set<Long> ccIds = uniqueIds(safeCcUsers, "抄送人不能重复");
        Set<Long> allIds = new LinkedHashSet<>(approverIds);
        allIds.addAll(ccIds);
        if (!allIds.isEmpty()) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<SystemUserEntity>()
                    .eq(SystemUserEntity::getTenantId, tenantId)
                    .eq(SystemUserEntity::getIsDeleted, false)
                    .eq(SystemUserEntity::getStatus, "active")
                    .in(SystemUserEntity::getId, allIds));
            if (count == null || count != allIds.size()) {
                throw new BizException("审批人或抄送人不存在、已停用或不属于当前企业");
            }
        }
        CustomerCategoryApprovalMemberEntity deleted = new CustomerCategoryApprovalMemberEntity();
        deleted.setIsDeleted(true);
        deleted.setDeletedAt(OffsetDateTime.now());
        deleted.setDeletedBy(operator);
        memberMapper.update(deleted, new LambdaUpdateWrapper<CustomerCategoryApprovalMemberEntity>()
                .eq(CustomerCategoryApprovalMemberEntity::getTenantId, tenantId)
                .eq(CustomerCategoryApprovalMemberEntity::getCategoryId, categoryId)
                .eq(CustomerCategoryApprovalMemberEntity::getIsDeleted, false));
        if (!allowOverLimit) {
            return;
        }
        int step = 1;
        for (Long userId : approverIds) {
            insertMember(tenantId, categoryId, "approver", userId, step++, operator);
        }
        for (Long userId : ccIds) {
            insertMember(tenantId, categoryId, "cc", userId, 0, operator);
        }
    }

    private Set<Long> uniqueIds(List<CustomerCategoryApprovalMemberRequest> members, String duplicateMessage) {
        Set<Long> ids = new LinkedHashSet<>();
        for (CustomerCategoryApprovalMemberRequest member : members) {
            if (member == null || member.systemUserId() == null) {
                throw new BizException("审批人员不能为空");
            }
            if (!ids.add(member.systemUserId())) {
                throw new BizException(duplicateMessage);
            }
        }
        return ids;
    }

    private void insertMember(
            Long tenantId,
            Long categoryId,
            String memberType,
            Long userId,
            int stepOrder,
            String operator
    ) {
        CustomerCategoryApprovalMemberEntity member = new CustomerCategoryApprovalMemberEntity();
        member.setTenantId(tenantId);
        member.setCategoryId(categoryId);
        member.setMemberType(memberType);
        member.setSystemUserId(userId);
        member.setStepOrder(stepOrder);
        member.setCreatedBy(operator);
        member.setIsDeleted(false);
        memberMapper.insert(member);
    }
}
