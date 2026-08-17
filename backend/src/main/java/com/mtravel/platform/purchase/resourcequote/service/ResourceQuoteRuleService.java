package com.mtravel.platform.purchase.resourcequote.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.enums.CustomerCategoryStatus;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.purchase.resourcequote.dto.ResourceQuoteRuleResponse;
import com.mtravel.platform.purchase.resourcequote.dto.ResourceQuoteRuleSaveRequest;
import com.mtravel.platform.purchase.resourcequote.entity.ResourceQuoteRuleEntity;
import com.mtravel.platform.purchase.resourcequote.enums.ResourceQuoteRuleResourceType;
import com.mtravel.platform.purchase.resourcequote.enums.ResourceQuoteRuleStatus;
import com.mtravel.platform.purchase.resourcequote.mapper.ResourceQuoteRuleMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 普通资源报价规则服务。
 *
 * <p>本服务维护资源类型和可选客户等级对应的建议、最低加价口径。客户等级名称通过当前页批量查询补齐，
 * 列表不会逐行访问客户分类表。</p>
 */
@Service
public class ResourceQuoteRuleService extends BusinessCrudService<ResourceQuoteRuleEntity, ResourceQuoteRuleResponse> {

    private final ResourceQuoteRuleMapper mapper;
    private final CustomerCategoryMapper categoryMapper;

    public ResourceQuoteRuleService(ResourceQuoteRuleMapper mapper, CustomerCategoryMapper categoryMapper) {
        super(mapper);
        this.mapper = mapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * 分页查询普通资源报价规则。
     *
     * @param tenantId 当前租户 ID
     * @param keyword 资源类型或备注关键字，可为空
     * @param resourceType 资源类型筛选，可为空
     * @param customerLevelId 客户等级筛选，可为空
     * @param status 状态筛选，可为空
     * @param page 当前页
     * @param pageSize 每页数量
     * @return 报价规则分页结果
     */
    public PageResult<ResourceQuoteRuleResponse> page(
            Long tenantId,
            String keyword,
            String resourceType,
            Long customerLevelId,
            String status,
            long page,
            long pageSize
    ) {
        String normalizedResourceType = StringUtils.hasText(resourceType)
                ? ResourceQuoteRuleResourceType.fromValue(resourceType).getValue()
                : null;
        ResourceQuoteRuleStatus parsedStatus = parseNullableStatus(status);
        QueryWrapper<ResourceQuoteRuleEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(normalizedResourceType), "resource_type", normalizedResourceType)
                .eq(customerLevelId != null, "customer_level_id", customerLevelId)
                .eq(parsedStatus != null, "status", parsedStatus == null ? null : parsedStatus.getValue())
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("resource_type", clean(keyword))
                        .or()
                        .like("remark", clean(keyword)))
                .orderByAsc("resource_type")
                .orderByAsc("customer_level_id")
                .orderByAsc("id");
        Page<ResourceQuoteRuleEntity> result = mapper.selectPage(Page.of(page, pageSize), wrapper);
        return toPageResponse(tenantId, result);
    }

    /**
     * 查询单条报价规则详情。
     *
     * @param id 规则 ID
     * @param tenantId 当前租户 ID
     * @return 带客户等级名称的规则
     */
    @Override
    public ResourceQuoteRuleResponse detail(Long id, Long tenantId) {
        ResourceQuoteRuleEntity entity = mapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException(notFoundMessage());
        }
        return toResponse(entity, customerLevelNameMap(tenantId, List.of(entity)));
    }

    /**
     * 新增普通资源报价规则。
     *
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @param operator 当前操作人
     * @return 新增后的规则
     */
    public ResourceQuoteRuleResponse create(ResourceQuoteRuleSaveRequest request, Long tenantId, String operator) {
        QuoteRuleFields fields = fields(request);
        assertCustomerLevelActive(tenantId, fields.customerLevelId());
        assertDuplicateRule(tenantId, fields.resourceType(), fields.customerLevelId(), null);

        ResourceQuoteRuleEntity entity = new ResourceQuoteRuleEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, fields);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改普通资源报价规则。
     *
     * <p>客户等级是可清空字段，因此不能使用实体更新。这里通过 {@link UpdateWrapper#set(String, Object)}
     * 显式设置 customer_level_id；即使请求值为 null，MyBatis-Plus 也会生成该列的 NULL 更新。</p>
     *
     * @param id 规则 ID
     * @param request 保存请求
     * @param tenantId 当前租户 ID
     * @return 修改后的规则
     */
    public ResourceQuoteRuleResponse update(Long id, ResourceQuoteRuleSaveRequest request, Long tenantId) {
        QuoteRuleFields fields = fields(request);
        assertCustomerLevelActive(tenantId, fields.customerLevelId());
        assertDuplicateRule(tenantId, fields.resourceType(), fields.customerLevelId(), id);

        UpdateWrapper<ResourceQuoteRuleEntity> updateWrapper = baseUpdate(tenantId)
                .eq("id", id)
                .set("resource_type", fields.resourceType())
                // 不通过实体更新，确保 customerLevelId:null 会实际写入 customer_level_id = NULL。
                .set("customer_level_id", fields.customerLevelId())
                .set("suggested_markup_rate", fields.suggestedRate())
                .set("minimum_markup_rate", fields.minimumRate())
                .set("suggested_fixed_markup", fields.suggestedFixedAddon())
                .set("minimum_fixed_markup", fields.minimumFixedAddon())
                .set("status", fields.status().getValue())
                .set("remark", fields.remark());
        int updated = mapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /** 将经过校验的保存字段写入新增实体。 */
    private void applyFields(ResourceQuoteRuleEntity entity, QuoteRuleFields fields) {
        entity.setResourceType(fields.resourceType());
        entity.setCustomerLevelId(fields.customerLevelId());
        entity.setSuggestedMarkupRate(fields.suggestedRate());
        entity.setMinimumMarkupRate(fields.minimumRate());
        entity.setSuggestedFixedMarkup(fields.suggestedFixedAddon());
        entity.setMinimumFixedMarkup(fields.minimumFixedAddon());
        entity.setStatus(fields.status().getValue());
        entity.setRemark(fields.remark());
    }

    /** 规范保存字段；比例值保持百分数，不在此处换算为小数。 */
    private QuoteRuleFields fields(ResourceQuoteRuleSaveRequest request) {
        BigDecimal suggestedRate = nonNegativeMoney(request.suggestedRate(), "建议比例不能小于0");
        BigDecimal minimumRate = nonNegativeMoney(request.minimumRate(), "最低比例不能小于0");
        BigDecimal suggestedFixedAddon = nonNegativeMoney(request.suggestedFixedAddon(), "建议固定加价不能小于0");
        BigDecimal minimumFixedAddon = nonNegativeMoney(request.minimumFixedAddon(), "最低固定加价不能小于0");
        assertMinimumDoesNotExceedSuggested(minimumRate, suggestedRate, "比例");
        assertMinimumDoesNotExceedSuggested(minimumFixedAddon, suggestedFixedAddon, "固定加价");
        return new QuoteRuleFields(
                ResourceQuoteRuleResourceType.fromValue(cleanRequired(request.resourceType())).getValue(),
                request.customerLevelId(),
                suggestedRate,
                minimumRate,
                suggestedFixedAddon,
                minimumFixedAddon,
                ResourceQuoteRuleStatus.fromValueOrDefault(request.status()),
                clean(request.remark())
        );
    }

    /** 最低口径不能高于建议口径，避免生成无法解释的报价规则。 */
    private void assertMinimumDoesNotExceedSuggested(
            BigDecimal minimum,
            BigDecimal suggested,
            String fieldName
    ) {
        if (minimum.compareTo(suggested) > 0) {
            throw new BizException("最低" + fieldName + "不能高于建议" + fieldName);
        }
    }

    /** 非空客户等级必须属于当前租户、未删除且启用。默认规则不绑定客户等级，因此允许 null。 */
    private void assertCustomerLevelActive(Long tenantId, Long customerLevelId) {
        if (customerLevelId == null) {
            return;
        }
        CustomerCategoryEntity level = categoryMapper.selectOne(categoryBaseQuery(tenantId)
                .eq("id", customerLevelId));
        if (level == null) {
            throw new BizException("客户等级不存在或已删除");
        }
        if (!CustomerCategoryStatus.ACTIVE.getValue().equals(level.getStatus())) {
            throw new BizException("客户等级已停用");
        }
    }

    /** 同一租户、资源类型和客户等级组合只能存在一条未删除规则，与数据库部分唯一索引保持一致。 */
    private void assertDuplicateRule(Long tenantId, String resourceType, Long customerLevelId, Long excludeId) {
        QueryWrapper<ResourceQuoteRuleEntity> wrapper = baseQuery(tenantId)
                .eq("resource_type", resourceType)
                .ne(excludeId != null, "id", excludeId);
        if (customerLevelId == null) {
            wrapper.isNull("customer_level_id");
        } else {
            wrapper.eq("customer_level_id", customerLevelId);
        }
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("该资源类型和客户等级的报价规则已存在");
        }
    }

    /** 为当前页规则一次性查询客户等级名称。 */
    private Map<Long, String> customerLevelNameMap(Long tenantId, List<ResourceQuoteRuleEntity> rules) {
        List<Long> levelIds = rules.stream()
                .map(ResourceQuoteRuleEntity::getCustomerLevelId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (levelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectList(categoryBaseQuery(tenantId).in("id", levelIds))
                .stream()
                .collect(Collectors.toMap(
                        CustomerCategoryEntity::getId,
                        CustomerCategoryEntity::getCategoryName,
                        (first, ignored) -> first
                ));
    }

    /** 将分页记录和批量等级名称映射为接口响应。 */
    private PageResult<ResourceQuoteRuleResponse> toPageResponse(Long tenantId, Page<ResourceQuoteRuleEntity> result) {
        List<ResourceQuoteRuleEntity> rules = result.getRecords();
        Map<Long, String> levelNames = customerLevelNameMap(tenantId, rules);
        List<ResourceQuoteRuleResponse> items = rules.stream()
                .map(rule -> toResponse(rule, levelNames))
                .toList();
        return new PageResult<>(items, result.getTotal());
    }

    /** 构造单条响应。 */
    private ResourceQuoteRuleResponse toResponse(ResourceQuoteRuleEntity entity, Map<Long, String> levelNames) {
        return ResourceQuoteRuleResponse.fromEntity(entity, levelNames.get(entity.getCustomerLevelId()));
    }

    /** 客户分类查询必须始终带租户和软删除边界。 */
    private QueryWrapper<CustomerCategoryEntity> categoryBaseQuery(Long tenantId) {
        return new QueryWrapper<CustomerCategoryEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    /** 页面状态参数为空时不筛选，非空时由枚举校验。 */
    private ResourceQuoteRuleStatus parseNullableStatus(String status) {
        return StringUtils.hasText(status) ? ResourceQuoteRuleStatus.fromValueOrDefault(status) : null;
    }

    /** 统一处理空金额为零，同时拦截绕过 Controller 的负数请求。 */
    private BigDecimal nonNegativeMoney(BigDecimal value, String message) {
        BigDecimal amount = money(value);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(message);
        }
        return amount;
    }

    @Override
    protected ResourceQuoteRuleEntity newEntity() {
        return new ResourceQuoteRuleEntity();
    }

    @Override
    protected ResourceQuoteRuleResponse toResponse(ResourceQuoteRuleEntity entity) {
        return toResponse(entity, customerLevelNameMap(entity.getTenantId(), List.of(entity)));
    }

    @Override
    protected String notFoundMessage() {
        return "普通资源报价规则不存在或已删除";
    }

    /** 保存字段内部快照，保证新增和 UpdateWrapper 更新采用同一套默认值。 */
    private record QuoteRuleFields(
            String resourceType,
            Long customerLevelId,
            BigDecimal suggestedRate,
            BigDecimal minimumRate,
            BigDecimal suggestedFixedAddon,
            BigDecimal minimumFixedAddon,
            ResourceQuoteRuleStatus status,
            String remark
    ) {
    }
}
