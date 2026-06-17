package com.mtravel.platform.enterprise.guide.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideTagResponse;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideTagSaveRequest;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideTagEntity;
import com.mtravel.platform.enterprise.guide.enums.EnterpriseGuideTagStatus;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideTagMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 导游标签业务服务。
 *
 * <p>标签用于导游能力分类和后续排团筛选，和导管绩效归属不是同一个概念。这里集中处理租户隔离、
 * 标签名称唯一性、启停状态和软删除。</p>
 */
@Service
public class EnterpriseGuideTagService extends BusinessCrudService<EnterpriseGuideTagEntity, EnterpriseGuideTagResponse> {

    private final EnterpriseGuideTagMapper mapper;

    public EnterpriseGuideTagService(EnterpriseGuideTagMapper mapper) {
        super(mapper);
        this.mapper = mapper;
    }

    /**
     * 分页查询导游标签。
     *
     * @param tenantId 当前租户 ID
     * @param keyword 标签名称关键字
     * @param status 标签状态
     * @param page 当前页
     * @param pageSize 每页条数
     * @return 标签分页结果
     */
    public PageResult<EnterpriseGuideTagResponse> page(
            Long tenantId,
            String keyword,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<EnterpriseGuideTagEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(status), "status", EnterpriseGuideTagStatus.fromValueOrDefault(status).getValue())
                .like(StringUtils.hasText(keyword), "tag_name", keyword == null ? null : keyword.trim())
                .orderByAsc("sort_order")
                .orderByAsc("tag_name")
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询启用标签列表。
     *
     * <p>用于导游档案编辑表单的标签多选。</p>
     */
    public List<EnterpriseGuideTagResponse> listActive(Long tenantId) {
        return mapper.selectList(baseQuery(tenantId)
                        .eq("status", EnterpriseGuideTagStatus.ACTIVE.getValue())
                        .orderByAsc("sort_order")
                        .orderByAsc("tag_name"))
                .stream()
                .map(EnterpriseGuideTagResponse::fromEntity)
                .toList();
    }

    /**
     * 新增导游标签。
     *
     * <p>同一租户下未删除标签名称必须唯一，避免导游编辑时出现重复选项。</p>
     */
    public EnterpriseGuideTagResponse create(
            EnterpriseGuideTagSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertUnique(tenantId, request.tagName(), null);
        EnterpriseGuideTagEntity entity = new EnterpriseGuideTagEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改导游标签。
     *
     * @param id 标签 ID
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @return 修改后的标签详情
     */
    public EnterpriseGuideTagResponse update(Long id, EnterpriseGuideTagSaveRequest request, Long tenantId) {
        assertUnique(tenantId, request.tagName(), id);
        int updated = mapper.update(null, baseUpdate(tenantId)
                .eq("id", id)
                .set("tag_name", cleanRequired(request.tagName()))
                .set("sort_order", number(request.sortOrder()))
                .set("status", EnterpriseGuideTagStatus.fromValueOrDefault(request.status()).getValue())
                .set("remark", clean(request.remark())));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    private void applyFields(EnterpriseGuideTagEntity entity, EnterpriseGuideTagSaveRequest request) {
        entity.setTagName(cleanRequired(request.tagName()));
        entity.setSortOrder(number(request.sortOrder()));
        entity.setStatus(EnterpriseGuideTagStatus.fromValueOrDefault(request.status()).getValue());
        entity.setRemark(clean(request.remark()));
    }

    private void assertUnique(Long tenantId, String tagName, Long excludeId) {
        Long count = mapper.selectCount(baseQuery(tenantId)
                .eq("tag_name", cleanRequired(tagName))
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("导游标签已存在");
        }
    }

    @Override
    protected EnterpriseGuideTagEntity newEntity() {
        return new EnterpriseGuideTagEntity();
    }

    @Override
    protected EnterpriseGuideTagResponse toResponse(EnterpriseGuideTagEntity entity) {
        return EnterpriseGuideTagResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "导游标签不存在或已删除";
    }
}
