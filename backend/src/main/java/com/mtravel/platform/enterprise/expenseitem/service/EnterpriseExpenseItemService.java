package com.mtravel.platform.enterprise.expenseitem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.expenseitem.dto.EnterpriseExpenseItemResponse;
import com.mtravel.platform.enterprise.expenseitem.dto.EnterpriseExpenseItemSaveRequest;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 费用项目业务服务。
 *
 * <p>费用项目是价格管理和后续成本统计的基础字典。这里负责租户隔离、同类型项目查重、
 * 启停状态默认值和软删除，前端不再维护写死的项目类型列表。</p>
 */
@Service
public class EnterpriseExpenseItemService extends BusinessCrudService<EnterpriseExpenseItemEntity, EnterpriseExpenseItemResponse> {

    private final EnterpriseExpenseItemMapper mapper;

    public EnterpriseExpenseItemService(EnterpriseExpenseItemMapper mapper) {
        super(mapper);
        this.mapper = mapper;
    }

    /** 分页查询费用项目，支持按资源类型、状态和项目名称筛选。 */
    public PageResult<EnterpriseExpenseItemResponse> page(
            Long tenantId,
            String keyword,
            String resourceType,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<EnterpriseExpenseItemEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(resourceType), "resource_type", resourceType)
                .eq(StringUtils.hasText(status), "status", status)
                .like(StringUtils.hasText(keyword), "project_name", keyword)
                .orderByAsc("resource_type")
                .orderByAsc("sort_order")
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /** 查询某个资源类型下启用的费用项目，用于采购价格管理项目类型下拉。 */
    public List<EnterpriseExpenseItemResponse> listActiveByResourceType(Long tenantId, String resourceType) {
        return mapper.selectList(baseQuery(tenantId)
                        .eq(StringUtils.hasText(resourceType), "resource_type", resourceType)
                        .eq("status", "active")
                        .orderByAsc("sort_order")
                        .orderByAsc("project_name"))
                .stream()
                .map(EnterpriseExpenseItemResponse::fromEntity)
                .toList();
    }

    /** 新增费用项目，同一资源类型下项目名称不能重复。 */
    public EnterpriseExpenseItemResponse create(
            EnterpriseExpenseItemSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertUnique(tenantId, request.resourceType(), request.projectName(), null);
        EnterpriseExpenseItemEntity entity = new EnterpriseExpenseItemEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /** 修改费用项目，查重时排除当前记录。 */
    public EnterpriseExpenseItemResponse update(
            Long id,
            EnterpriseExpenseItemSaveRequest request,
            Long tenantId
    ) {
        assertUnique(tenantId, request.resourceType(), request.projectName(), id);
        EnterpriseExpenseItemEntity entity = new EnterpriseExpenseItemEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /** 将请求字段清洗后写入实体，并补齐默认状态和统计标记。 */
    private void applyFields(EnterpriseExpenseItemEntity entity, EnterpriseExpenseItemSaveRequest request) {
        entity.setResourceType(cleanRequired(request.resourceType()));
        entity.setProjectName(cleanRequired(request.projectName()));
        entity.setStatisticsEnabled(request.statisticsEnabled() == null ? Boolean.TRUE : request.statisticsEnabled());
        entity.setSortOrder(number(request.sortOrder()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(clean(request.remark()));
    }

    /** 同一租户、同一资源类型下的未删除项目名称不能重复。 */
    private void assertUnique(Long tenantId, String resourceType, String projectName, Long excludeId) {
        QueryWrapper<EnterpriseExpenseItemEntity> wrapper = baseQuery(tenantId)
                .eq("resource_type", cleanRequired(resourceType))
                .eq("project_name", cleanRequired(projectName))
                .ne(excludeId != null, "id", excludeId);
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("费用项目已存在");
        }
    }

    @Override
    protected EnterpriseExpenseItemEntity newEntity() {
        return new EnterpriseExpenseItemEntity();
    }

    @Override
    protected EnterpriseExpenseItemResponse toResponse(EnterpriseExpenseItemEntity entity) {
        return EnterpriseExpenseItemResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "费用项目不存在或已删除";
    }
}
