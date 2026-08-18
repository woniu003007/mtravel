package com.mtravel.platform.purchase.relation.price.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.expenseitem.entity.EnterpriseExpenseItemEntity;
import com.mtravel.platform.enterprise.expenseitem.mapper.EnterpriseExpenseItemMapper;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.relation.price.dto.SupplierResourcePriceResponse;
import com.mtravel.platform.purchase.relation.price.dto.SupplierResourcePriceSaveRequest;
import com.mtravel.platform.purchase.relation.price.entity.SupplierResourcePriceEntity;
import com.mtravel.platform.purchase.relation.price.mapper.SupplierResourcePriceMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 供应商资源价格业务服务。
 *
 * <p>服务负责把价格记录限定在当前租户、当前采购关系下，并校验费用项目资源类型必须和采购关系一致。
 * 这样可以避免在景区关系下误选酒店房型等不相关项目。</p>
 */
@Service
public class SupplierResourcePriceService extends BusinessCrudService<SupplierResourcePriceEntity, SupplierResourcePriceResponse> {

    private final SupplierResourcePriceMapper mapper;
    private final PurchaseRelationMapper relationMapper;
    private final EnterpriseExpenseItemMapper expenseItemMapper;

    public SupplierResourcePriceService(
            SupplierResourcePriceMapper mapper,
            PurchaseRelationMapper relationMapper,
            EnterpriseExpenseItemMapper expenseItemMapper
    ) {
        super(mapper);
        this.mapper = mapper;
        this.relationMapper = relationMapper;
        this.expenseItemMapper = expenseItemMapper;
    }

    /** 分页查询某条采购关系下的价格明细。 */
    public PageResult<SupplierResourcePriceResponse> page(
            Long tenantId,
            Long relationId,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<SupplierResourcePriceEntity> wrapper = baseQuery(tenantId)
                .eq(relationId != null, "relation_id", relationId)
                .eq(StringUtils.hasText(status), "status", status)
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /** 新增价格明细，项目类型必须和采购关系的资源类型一致。 */
    public SupplierResourcePriceResponse create(
            SupplierResourcePriceSaveRequest request,
            Long tenantId,
            String operator
    ) {
        PurchaseRelationEntity relation = relation(tenantId, request.relationId());
        assertClassifiedPriceMode(relation);
        EnterpriseExpenseItemEntity project = project(tenantId, request.resourceProjectId());
        assertProjectMatchesRelation(relation, project);
        assertUnique(tenantId, request.relationId(), request.resourceProjectId(), null);

        SupplierResourcePriceEntity entity = new SupplierResourcePriceEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request, project);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /** 修改价格明细，查重时排除当前记录。 */
    public SupplierResourcePriceResponse update(
            Long id,
            SupplierResourcePriceSaveRequest request,
            Long tenantId
    ) {
        PurchaseRelationEntity relation = relation(tenantId, request.relationId());
        assertClassifiedPriceMode(relation);
        EnterpriseExpenseItemEntity project = project(tenantId, request.resourceProjectId());
        assertProjectMatchesRelation(relation, project);
        assertUnique(tenantId, request.relationId(), request.resourceProjectId(), id);

        SupplierResourcePriceEntity entity = new SupplierResourcePriceEntity();
        applyFields(entity, request, project);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /** 查询同租户未删除采购关系。 */
    private PurchaseRelationEntity relation(Long tenantId, Long relationId) {
        PurchaseRelationEntity entity = relationMapper.selectOne(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", relationId));
        if (entity == null) {
            throw new BizException("采购关系不存在或已删除");
        }
        return entity;
    }

    /** 统一报价不能再追加分类明细，避免同一关系出现两套金额口径。 */
    private void assertClassifiedPriceMode(PurchaseRelationEntity relation) {
        if ("unified".equals(relation.getPriceMode())) {
            throw new BizException("统一报价请到资源页编辑，不能追加分类价格明细");
        }
    }

    /** 查询同租户未删除费用项目。 */
    private EnterpriseExpenseItemEntity project(Long tenantId, Long projectId) {
        EnterpriseExpenseItemEntity entity = expenseItemMapper.selectOne(new QueryWrapper<EnterpriseExpenseItemEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", projectId));
        if (entity == null) {
            throw new BizException("费用项目不存在或已删除");
        }
        return entity;
    }

    /** 防止在某类资源关系下选择其它资源类型的费用项目。 */
    private void assertProjectMatchesRelation(PurchaseRelationEntity relation, EnterpriseExpenseItemEntity project) {
        if (!relation.getResourceType().equals(project.getResourceType())) {
            throw new BizException("项目类型和采购关系资源类型不匹配");
        }
    }

    /** 同一采购关系下，同一个费用项目只能保留一条未删除价格。 */
    private void assertUnique(Long tenantId, Long relationId, Long projectId, Long excludeId) {
        QueryWrapper<SupplierResourcePriceEntity> wrapper = baseQuery(tenantId)
                .eq("relation_id", relationId)
                .eq("resource_project_id", projectId)
                .ne(excludeId != null, "id", excludeId);
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("该项目类型价格已存在");
        }
    }

    /** 将价格请求写入实体，并保存项目名称快照。 */
    private void applyFields(
            SupplierResourcePriceEntity entity,
            SupplierResourcePriceSaveRequest request,
            EnterpriseExpenseItemEntity project
    ) {
        entity.setRelationId(request.relationId());
        entity.setResourceProjectId(request.resourceProjectId());
        entity.setProjectName(project.getProjectName());
        entity.setMarketPrice(money(request.marketPrice()));
        entity.setPeerPrice(money(request.peerPrice()));
        entity.setTeamPrice(money(request.teamPrice()));
        entity.setPriceDescription(clean(request.priceDescription()));
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        entity.setRemark(clean(request.remark()));
    }

    @Override
    protected SupplierResourcePriceEntity newEntity() {
        return new SupplierResourcePriceEntity();
    }

    @Override
    protected SupplierResourcePriceResponse toResponse(SupplierResourcePriceEntity entity) {
        return SupplierResourcePriceResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "供应商资源价格不存在或已删除";
    }
}
