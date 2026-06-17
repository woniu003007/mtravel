package com.mtravel.platform.purchase.groundagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.purchase.groundagent.dto.GroundAgentResponse;
import com.mtravel.platform.purchase.groundagent.dto.GroundAgentSaveRequest;
import com.mtravel.platform.purchase.groundagent.entity.GroundAgentEntity;
import com.mtravel.platform.purchase.groundagent.mapper.GroundAgentMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 地接外委服务。
 *
 * <p>地接外委记录包含地接社、一次外委任务、行程要求、预算和确认单。
 * 它不是简单供应商档案，所以单独作为业务记录维护。</p>
 */
@Service
public class GroundAgentService extends BusinessCrudService<GroundAgentEntity, GroundAgentResponse> {

    private final GroundAgentMapper mapper;
    private final CommonAttachmentService attachmentService;

    public GroundAgentService(GroundAgentMapper mapper, CommonAttachmentService attachmentService) {
        super(mapper);
        this.mapper = mapper;
        this.attachmentService = attachmentService;
    }

    /** 分页查询地接外委记录。 */
    public PageResult<GroundAgentResponse> page(
            Long tenantId,
            String keyword,
            String city,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<GroundAgentEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(city), "city", city)
                .eq(StringUtils.hasText(status), "status", status)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("ground_agent_name", keyword)
                        .or()
                        .like("task_name", keyword))
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /** 新增地接外委记录，并绑定确认单附件。 */
    public GroundAgentResponse create(GroundAgentSaveRequest request, Long tenantId, String operator) {
        GroundAgentEntity entity = new GroundAgentEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);

        attachmentService.bind(entity.getConfirmationAttachmentId(), entity.getId(), tenantId);
        return detail(entity.getId(), tenantId);
    }

    /** 修改地接外委记录。 */
    public GroundAgentResponse update(Long id, GroundAgentSaveRequest request, Long tenantId) {
        GroundAgentEntity entity = new GroundAgentEntity();
        applyFields(entity, request);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }

        attachmentService.bind(entity.getConfirmationAttachmentId(), id, tenantId);
        return detail(id, tenantId);
    }

    private void applyFields(GroundAgentEntity entity, GroundAgentSaveRequest request) {
        entity.setGroundAgentName(cleanRequired(request.groundAgentName()));
        entity.setCity(clean(request.city()));
        entity.setContactName(clean(request.contactName()));
        entity.setContactPhone(clean(request.contactPhone()));
        entity.setTaskName(clean(request.taskName()));
        entity.setItineraryRequirement(request.itineraryRequirement());
        entity.setTotalBudget(money(request.totalBudget()));
        entity.setConfirmationAttachmentId(request.confirmationAttachmentId());
        entity.setConfirmationFileUrl(clean(request.confirmationFileUrl()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(request.remark());
    }

    @Override
    protected GroundAgentEntity newEntity() {
        return new GroundAgentEntity();
    }

    @Override
    protected GroundAgentResponse toResponse(GroundAgentEntity entity) {
        return GroundAgentResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "地接外委记录不存在或已删除";
    }
}
