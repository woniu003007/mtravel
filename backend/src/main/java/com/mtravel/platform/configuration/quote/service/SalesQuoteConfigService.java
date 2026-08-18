package com.mtravel.platform.configuration.quote.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.configuration.quote.dto.QuoteApprovalConfigRequest;
import com.mtravel.platform.configuration.quote.dto.QuoteApprovalConfigResponse;
import com.mtravel.platform.configuration.quote.dto.QuoteApprovalMemberRequest;
import com.mtravel.platform.configuration.quote.dto.QuoteApprovalMemberResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGroundAgentRuleResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGroundAgentRuleSaveRequest;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGuideLevelResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGuideLevelSaveRequest;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGuideRuleResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteGuideRuleSaveRequest;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteResourceRuleResponse;
import com.mtravel.platform.configuration.quote.dto.SalesQuoteResourceRuleSaveRequest;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteApprovalMemberEntity;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteGroundAgentRuleEntity;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteGuideLevelEntity;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteGuideRuleEntity;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteResourceRuleEntity;
import com.mtravel.platform.configuration.quote.enums.QuoteApprovalMemberType;
import com.mtravel.platform.configuration.quote.enums.QuoteConfigStatus;
import com.mtravel.platform.configuration.quote.enums.SalesQuoteResourceQuoteMode;
import com.mtravel.platform.configuration.quote.enums.SalesQuoteResourceType;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteApprovalMemberMapper;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteGroundAgentRuleMapper;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteGuideLevelMapper;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteGuideRuleMapper;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteResourceRuleMapper;
import com.mtravel.platform.customer.category.entity.CustomerCategoryEntity;
import com.mtravel.platform.customer.category.mapper.CustomerCategoryMapper;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideEntity;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideMapper;
import com.mtravel.platform.system.user.entity.SystemUserEntity;
import com.mtravel.platform.system.user.mapper.SystemUserMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 销售报价配置业务服务。
 *
 * <p>本服务只维护报价规则和审批配置。供应商采购成本不在这里保存，正式报价时应先取采购成本，
 * 再套用本模块规则计算建议销售价和最低销售价。</p>
 */
@Service
public class SalesQuoteConfigService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String DEFAULT_LANGUAGE = "普通话";

    private final SalesQuoteResourceRuleMapper resourceRuleMapper;
    private final SalesQuoteGuideLevelMapper guideLevelMapper;
    private final SalesQuoteGuideRuleMapper guideRuleMapper;
    private final SalesQuoteGroundAgentRuleMapper groundAgentRuleMapper;
    private final SalesQuoteApprovalMemberMapper approvalMemberMapper;
    private final CustomerCategoryMapper customerCategoryMapper;
    private final SystemUserMapper systemUserMapper;
    private final EnterpriseGuideMapper enterpriseGuideMapper;

    public SalesQuoteConfigService(
            SalesQuoteResourceRuleMapper resourceRuleMapper,
            SalesQuoteGuideLevelMapper guideLevelMapper,
            SalesQuoteGuideRuleMapper guideRuleMapper,
            SalesQuoteGroundAgentRuleMapper groundAgentRuleMapper,
            SalesQuoteApprovalMemberMapper approvalMemberMapper,
            CustomerCategoryMapper customerCategoryMapper,
            SystemUserMapper systemUserMapper,
            EnterpriseGuideMapper enterpriseGuideMapper
    ) {
        this.resourceRuleMapper = resourceRuleMapper;
        this.guideLevelMapper = guideLevelMapper;
        this.guideRuleMapper = guideRuleMapper;
        this.groundAgentRuleMapper = groundAgentRuleMapper;
        this.approvalMemberMapper = approvalMemberMapper;
        this.customerCategoryMapper = customerCategoryMapper;
        this.systemUserMapper = systemUserMapper;
        this.enterpriseGuideMapper = enterpriseGuideMapper;
    }

    /**
     * 分页查询普通资源报价规则。
     */
    public PageResult<SalesQuoteResourceRuleResponse> resourceRulePage(
            Long tenantId,
            String resourceType,
            Long customerCategoryId,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<SalesQuoteResourceRuleEntity> wrapper = resourceRuleQuery(tenantId)
                .eq(StringUtils.hasText(resourceType), "resource_type", resourceType)
                .eq(customerCategoryId != null, "customer_category_id", customerCategoryId)
                .eq(StringUtils.hasText(status), "status", status)
                .orderByAsc("resource_type")
                .orderByAsc("customer_category_id")
                .orderByDesc("id");
        Page<SalesQuoteResourceRuleEntity> result = resourceRuleMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResult<>(result.getRecords().stream().map(SalesQuoteResourceRuleResponse::fromEntity).toList(), result.getTotal());
    }

    /**
     * 新增普通资源报价规则。
     */
    @Transactional
    public SalesQuoteResourceRuleResponse createResourceRule(
            SalesQuoteResourceRuleSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertDuplicateResourceRule(tenantId, request, null);
        CustomerCategoryEntity category = resolveCustomerCategory(tenantId, request.customerCategoryId());
        SalesQuoteResourceRuleEntity entity = new SalesQuoteResourceRuleEntity();
        entity.setTenantId(tenantId);
        applyResourceRuleFields(entity, request, category);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        resourceRuleMapper.insert(entity);
        return SalesQuoteResourceRuleResponse.fromEntity(requireResourceRule(entity.getId(), tenantId));
    }

    /**
     * 修改普通资源报价规则。
     */
    @Transactional
    public SalesQuoteResourceRuleResponse updateResourceRule(
            Long id,
            SalesQuoteResourceRuleSaveRequest request,
            Long tenantId
    ) {
        assertDuplicateResourceRule(tenantId, request, id);
        CustomerCategoryEntity category = resolveCustomerCategory(tenantId, request.customerCategoryId());
        SalesQuoteResourceRuleEntity entity = new SalesQuoteResourceRuleEntity();
        applyResourceRuleFields(entity, request, category);
        int updated = resourceRuleMapper.update(entity, resourceRuleUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("普通资源报价规则不存在或已删除");
        }
        return SalesQuoteResourceRuleResponse.fromEntity(requireResourceRule(id, tenantId));
    }

    /**
     * 软删除普通资源报价规则。
     */
    public void deleteResourceRule(Long id, Long tenantId, String operator) {
        SalesQuoteResourceRuleEntity entity = new SalesQuoteResourceRuleEntity();
        fillSoftDelete(entity, operator);
        int updated = resourceRuleMapper.update(entity, resourceRuleUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("普通资源报价规则不存在或已删除");
        }
    }

    /**
     * 分页查询导游等级。
     */
    public PageResult<SalesQuoteGuideLevelResponse> guideLevelPage(
            Long tenantId,
            String keyword,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<SalesQuoteGuideLevelEntity> wrapper = guideLevelQuery(tenantId)
                .like(StringUtils.hasText(keyword), "level_name", clean(keyword))
                .eq(StringUtils.hasText(status), "status", status)
                .orderByAsc("sort_order")
                .orderByAsc("id");
        Page<SalesQuoteGuideLevelEntity> result = guideLevelMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResult<>(result.getRecords().stream().map(SalesQuoteGuideLevelResponse::fromEntity).toList(), result.getTotal());
    }

    /**
     * 查询启用导游等级，用于导游档案和报价规则下拉。
     */
    public List<SalesQuoteGuideLevelResponse> activeGuideLevels(Long tenantId) {
        return guideLevelMapper.selectList(guideLevelQuery(tenantId)
                        .eq("status", QuoteConfigStatus.ACTIVE.getValue())
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream()
                .map(SalesQuoteGuideLevelResponse::fromEntity)
                .toList();
    }

    /**
     * 新增导游等级。
     */
    @Transactional
    public SalesQuoteGuideLevelResponse createGuideLevel(
            SalesQuoteGuideLevelSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertDuplicateGuideLevel(tenantId, request.levelName(), null);
        SalesQuoteGuideLevelEntity entity = new SalesQuoteGuideLevelEntity();
        entity.setTenantId(tenantId);
        applyGuideLevelFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        guideLevelMapper.insert(entity);
        return SalesQuoteGuideLevelResponse.fromEntity(requireGuideLevel(entity.getId(), tenantId));
    }

    /**
     * 修改导游等级。
     */
    @Transactional
    public SalesQuoteGuideLevelResponse updateGuideLevel(
            Long id,
            SalesQuoteGuideLevelSaveRequest request,
            Long tenantId
    ) {
        assertDuplicateGuideLevel(tenantId, request.levelName(), id);
        SalesQuoteGuideLevelEntity entity = new SalesQuoteGuideLevelEntity();
        applyGuideLevelFields(entity, request);
        int updated = guideLevelMapper.update(entity, guideLevelUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("导游等级不存在或已删除");
        }
        syncGuideLevelNameSnapshot(id, tenantId, cleanRequired(request.levelName()));
        return SalesQuoteGuideLevelResponse.fromEntity(requireGuideLevel(id, tenantId));
    }

    /**
     * 软删除导游等级。已被导游档案或导游报价规则使用时禁止删除，避免报价匹配丢失含义。
     */
    public void deleteGuideLevel(Long id, Long tenantId, String operator) {
        Long guideRuleCount = guideRuleMapper.selectCount(guideRuleQuery(tenantId).eq("guide_level_id", id));
        if (guideRuleCount != null && guideRuleCount > 0) {
            throw new BizException("导游等级已配置报价规则，不能删除");
        }
        Long guideCount = enterpriseGuideMapper.selectCount(new QueryWrapper<EnterpriseGuideEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("guide_level_id", id));
        if (guideCount != null && guideCount > 0) {
            throw new BizException("导游等级已被导游档案使用，不能删除");
        }
        SalesQuoteGuideLevelEntity entity = new SalesQuoteGuideLevelEntity();
        fillSoftDelete(entity, operator);
        int updated = guideLevelMapper.update(entity, guideLevelUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("导游等级不存在或已删除");
        }
    }

    /**
     * 分页查询导游报价规则。
     */
    public PageResult<SalesQuoteGuideRuleResponse> guideRulePage(
            Long tenantId,
            Long guideLevelId,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<SalesQuoteGuideRuleEntity> wrapper = guideRuleQuery(tenantId)
                .eq(guideLevelId != null, "guide_level_id", guideLevelId)
                .eq(StringUtils.hasText(status), "status", status)
                .orderByAsc("guide_level_id")
                .orderByAsc("language")
                .orderByDesc("id");
        Page<SalesQuoteGuideRuleEntity> result = guideRuleMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResult<>(result.getRecords().stream().map(SalesQuoteGuideRuleResponse::fromEntity).toList(), result.getTotal());
    }

    /**
     * 新增导游报价规则。
     */
    @Transactional
    public SalesQuoteGuideRuleResponse createGuideRule(
            SalesQuoteGuideRuleSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertDuplicateGuideRule(tenantId, request, null);
        SalesQuoteGuideLevelEntity level = requireGuideLevel(request.guideLevelId(), tenantId);
        SalesQuoteGuideRuleEntity entity = new SalesQuoteGuideRuleEntity();
        entity.setTenantId(tenantId);
        applyGuideRuleFields(entity, request, level);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        guideRuleMapper.insert(entity);
        return SalesQuoteGuideRuleResponse.fromEntity(requireGuideRule(entity.getId(), tenantId));
    }

    /**
     * 修改导游报价规则。
     */
    @Transactional
    public SalesQuoteGuideRuleResponse updateGuideRule(
            Long id,
            SalesQuoteGuideRuleSaveRequest request,
            Long tenantId
    ) {
        assertDuplicateGuideRule(tenantId, request, id);
        SalesQuoteGuideLevelEntity level = requireGuideLevel(request.guideLevelId(), tenantId);
        SalesQuoteGuideRuleEntity entity = new SalesQuoteGuideRuleEntity();
        applyGuideRuleFields(entity, request, level);
        int updated = guideRuleMapper.update(entity, guideRuleUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("导游报价规则不存在或已删除");
        }
        return SalesQuoteGuideRuleResponse.fromEntity(requireGuideRule(id, tenantId));
    }

    /**
     * 软删除导游报价规则。
     */
    public void deleteGuideRule(Long id, Long tenantId, String operator) {
        SalesQuoteGuideRuleEntity entity = new SalesQuoteGuideRuleEntity();
        fillSoftDelete(entity, operator);
        int updated = guideRuleMapper.update(entity, guideRuleUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("导游报价规则不存在或已删除");
        }
    }

    /**
     * 分页查询地接报价规则。
     */
    public PageResult<SalesQuoteGroundAgentRuleResponse> groundAgentRulePage(
            Long tenantId,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<SalesQuoteGroundAgentRuleEntity> wrapper = groundAgentQuery(tenantId)
                .eq(StringUtils.hasText(status), "status", status)
                .orderByAsc("min_people")
                .orderByAsc("max_people")
                .orderByDesc("id");
        Page<SalesQuoteGroundAgentRuleEntity> result = groundAgentRuleMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResult<>(result.getRecords().stream().map(SalesQuoteGroundAgentRuleResponse::fromEntity).toList(), result.getTotal());
    }

    /**
     * 新增地接人数区间整团价规则。
     */
    @Transactional
    public SalesQuoteGroundAgentRuleResponse createGroundAgentRule(
            SalesQuoteGroundAgentRuleSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertGroundAgentRange(request);
        assertGroundAgentRangeNotOverlap(tenantId, request, null);
        SalesQuoteGroundAgentRuleEntity entity = new SalesQuoteGroundAgentRuleEntity();
        entity.setTenantId(tenantId);
        applyGroundAgentFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        groundAgentRuleMapper.insert(entity);
        return SalesQuoteGroundAgentRuleResponse.fromEntity(requireGroundAgentRule(entity.getId(), tenantId));
    }

    /**
     * 修改地接人数区间整团价规则。
     */
    @Transactional
    public SalesQuoteGroundAgentRuleResponse updateGroundAgentRule(
            Long id,
            SalesQuoteGroundAgentRuleSaveRequest request,
            Long tenantId
    ) {
        assertGroundAgentRange(request);
        assertGroundAgentRangeNotOverlap(tenantId, request, id);
        SalesQuoteGroundAgentRuleEntity entity = new SalesQuoteGroundAgentRuleEntity();
        applyGroundAgentFields(entity, request);
        int updated = groundAgentRuleMapper.update(entity, groundAgentUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("地接报价规则不存在或已删除");
        }
        return SalesQuoteGroundAgentRuleResponse.fromEntity(requireGroundAgentRule(id, tenantId));
    }

    /**
     * 软删除地接报价规则。
     */
    public void deleteGroundAgentRule(Long id, Long tenantId, String operator) {
        SalesQuoteGroundAgentRuleEntity entity = new SalesQuoteGroundAgentRuleEntity();
        fillSoftDelete(entity, operator);
        int updated = groundAgentRuleMapper.update(entity, groundAgentUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException("地接报价规则不存在或已删除");
        }
    }

    /**
     * 查询销售报价统一低价审批配置。
     */
    public QuoteApprovalConfigResponse approvalConfig(Long tenantId) {
        List<SalesQuoteApprovalMemberEntity> members = approvalMemberMapper.selectList(approvalMemberQuery(tenantId)
                .orderByAsc("member_type")
                .orderByAsc("step_order")
                .orderByAsc("id"));
        List<QuoteApprovalMemberResponse> responses = buildApprovalMemberResponses(members, tenantId);
        List<QuoteApprovalMemberResponse> approvers = responses.stream()
                .filter(item -> QuoteApprovalMemberType.APPROVER.getValue().equals(item.memberType()))
                .sorted(Comparator.comparing(QuoteApprovalMemberResponse::stepOrder))
                .toList();
        List<QuoteApprovalMemberResponse> ccUsers = responses.stream()
                .filter(item -> QuoteApprovalMemberType.CC.getValue().equals(item.memberType()))
                .toList();
        return new QuoteApprovalConfigResponse(approvers, ccUsers);
    }

    /**
     * 保存销售报价统一低价审批配置。
     */
    @Transactional
    public QuoteApprovalConfigResponse saveApprovalConfig(
            QuoteApprovalConfigRequest request,
            Long tenantId,
            String operator
    ) {
        List<Long> approverIds = cleanIds(request == null ? null : request.approvers());
        if (approverIds.isEmpty()) {
            throw new BizException("报价低价审批人不能为空");
        }
        List<Long> ccIds = cleanIds(request.ccUsers());
        assertSystemUsersActive(tenantId, concat(approverIds, ccIds));

        SalesQuoteApprovalMemberEntity deleted = new SalesQuoteApprovalMemberEntity();
        fillSoftDelete(deleted, operator);
        approvalMemberMapper.update(deleted, approvalMemberUpdate(tenantId));
        insertApprovalMembers(tenantId, approverIds, QuoteApprovalMemberType.APPROVER.getValue(), operator);
        insertApprovalMembers(tenantId, ccIds, QuoteApprovalMemberType.CC.getValue(), operator);
        return approvalConfig(tenantId);
    }

    private void applyResourceRuleFields(
            SalesQuoteResourceRuleEntity entity,
            SalesQuoteResourceRuleSaveRequest request,
            CustomerCategoryEntity category
    ) {
        entity.setResourceType(SalesQuoteResourceType.requireValid(cleanRequired(request.resourceType())));
        entity.setCustomerCategoryId(category == null ? null : category.getId());
        entity.setCustomerCategoryName(category == null ? null : category.getCategoryName());
        entity.setQuoteMode(SalesQuoteResourceQuoteMode.fromValueOrDefault(request.quoteMode()));
        entity.setSuggestedMarkupRate(nonNegative(request.suggestedMarkupRate(), "建议比例上浮不能小于0"));
        entity.setMinimumMarkupRate(nonNegative(request.minimumMarkupRate(), "最低比例上浮不能小于0"));
        entity.setSuggestedFixedMarkup(nonNegative(request.suggestedFixedMarkup(), "建议固定加价不能小于0"));
        entity.setMinimumFixedMarkup(nonNegative(request.minimumFixedMarkup(), "最低固定加价不能小于0"));
        entity.setStatus(QuoteConfigStatus.fromValueOrDefault(request.status()).getValue());
        entity.setRemark(clean(request.remark()));
    }

    private void applyGuideLevelFields(SalesQuoteGuideLevelEntity entity, SalesQuoteGuideLevelSaveRequest request) {
        entity.setLevelName(cleanRequired(request.levelName()));
        entity.setSortOrder(number(request.sortOrder()));
        entity.setStatus(QuoteConfigStatus.fromValueOrDefault(request.status()).getValue());
        entity.setRemark(clean(request.remark()));
    }

    private void applyGuideRuleFields(
            SalesQuoteGuideRuleEntity entity,
            SalesQuoteGuideRuleSaveRequest request,
            SalesQuoteGuideLevelEntity level
    ) {
        entity.setGuideLevelId(level.getId());
        entity.setGuideLevelName(level.getLevelName());
        entity.setLanguage(StringUtils.hasText(request.language()) ? clean(request.language()) : DEFAULT_LANGUAGE);
        entity.setBaseDailyFee(nonNegative(request.baseDailyFee(), "基础导服费不能小于0"));
        entity.setForeignLanguageDailyMarkup(nonNegative(request.foreignLanguageDailyMarkup(), "外语服务加价不能小于0"));
        entity.setOvertimeHourlyFee(nonNegative(request.overtimeHourlyFee(), "超时费不能小于0"));
        entity.setStatus(QuoteConfigStatus.fromValueOrDefault(request.status()).getValue());
        entity.setRemark(clean(request.remark()));
    }

    private void applyGroundAgentFields(
            SalesQuoteGroundAgentRuleEntity entity,
            SalesQuoteGroundAgentRuleSaveRequest request
    ) {
        entity.setMinPeople(request.minPeople());
        entity.setMaxPeople(request.maxPeople());
        entity.setGroupPackagePrice(nonNegative(request.groupPackagePrice(), "整团打包价不能小于0"));
        entity.setStatus(QuoteConfigStatus.fromValueOrDefault(request.status()).getValue());
        entity.setRemark(clean(request.remark()));
    }

    private void assertDuplicateResourceRule(Long tenantId, SalesQuoteResourceRuleSaveRequest request, Long excludeId) {
        String resourceType = SalesQuoteResourceType.requireValid(cleanRequired(request.resourceType()));
        QueryWrapper<SalesQuoteResourceRuleEntity> wrapper = resourceRuleQuery(tenantId)
                .eq("resource_type", resourceType)
                .ne(excludeId != null, "id", excludeId);
        if (request.customerCategoryId() == null) {
            wrapper.isNull("customer_category_id");
        } else {
            wrapper.eq("customer_category_id", request.customerCategoryId());
        }
        Long count = resourceRuleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("同资源类型和客户等级的报价规则已存在");
        }
    }

    private void assertDuplicateGuideLevel(Long tenantId, String levelName, Long excludeId) {
        Long count = guideLevelMapper.selectCount(guideLevelQuery(tenantId)
                .eq("level_name", cleanRequired(levelName))
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("导游等级名称已存在");
        }
    }

    private void assertDuplicateGuideRule(Long tenantId, SalesQuoteGuideRuleSaveRequest request, Long excludeId) {
        Long count = guideRuleMapper.selectCount(guideRuleQuery(tenantId)
                .eq("guide_level_id", request.guideLevelId())
                .eq("language", StringUtils.hasText(request.language()) ? clean(request.language()) : DEFAULT_LANGUAGE)
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("同导游等级和语种的报价规则已存在");
        }
    }

    private void assertGroundAgentRange(SalesQuoteGroundAgentRuleSaveRequest request) {
        if (request.minPeople() == null || request.maxPeople() == null) {
            throw new BizException("地接报价人数区间不能为空");
        }
        if (request.minPeople() < 1 || request.maxPeople() < request.minPeople()) {
            throw new BizException("地接报价人数区间不合法");
        }
    }

    private void assertGroundAgentRangeNotOverlap(
            Long tenantId,
            SalesQuoteGroundAgentRuleSaveRequest request,
            Long excludeId
    ) {
        Long count = groundAgentRuleMapper.selectCount(groundAgentQuery(tenantId)
                .le("min_people", request.maxPeople())
                .ge("max_people", request.minPeople())
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("地接报价人数区间不能重叠");
        }
    }

    private CustomerCategoryEntity resolveCustomerCategory(Long tenantId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        CustomerCategoryEntity category = customerCategoryMapper.selectOne(new QueryWrapper<CustomerCategoryEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", QuoteConfigStatus.ACTIVE.getValue())
                .eq("id", categoryId));
        if (category == null) {
            throw new BizException("客户等级不存在或已停用");
        }
        return category;
    }

    private SalesQuoteResourceRuleEntity requireResourceRule(Long id, Long tenantId) {
        SalesQuoteResourceRuleEntity entity = resourceRuleMapper.selectOne(resourceRuleQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException("普通资源报价规则不存在或已删除");
        }
        return entity;
    }

    private SalesQuoteGuideLevelEntity requireGuideLevel(Long id, Long tenantId) {
        SalesQuoteGuideLevelEntity entity = guideLevelMapper.selectOne(guideLevelQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException("导游等级不存在或已删除");
        }
        return entity;
    }

    private SalesQuoteGuideRuleEntity requireGuideRule(Long id, Long tenantId) {
        SalesQuoteGuideRuleEntity entity = guideRuleMapper.selectOne(guideRuleQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException("导游报价规则不存在或已删除");
        }
        return entity;
    }

    private SalesQuoteGroundAgentRuleEntity requireGroundAgentRule(Long id, Long tenantId) {
        SalesQuoteGroundAgentRuleEntity entity = groundAgentRuleMapper.selectOne(groundAgentQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException("地接报价规则不存在或已删除");
        }
        return entity;
    }

    private void syncGuideLevelNameSnapshot(Long guideLevelId, Long tenantId, String levelName) {
        SalesQuoteGuideRuleEntity guideRule = new SalesQuoteGuideRuleEntity();
        guideRule.setGuideLevelName(levelName);
        guideRuleMapper.update(guideRule, guideRuleUpdate(tenantId).eq("guide_level_id", guideLevelId));
    }

    private void assertSystemUsersActive(Long tenantId, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        List<SystemUserEntity> users = systemUserMapper.selectList(new QueryWrapper<SystemUserEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", QuoteConfigStatus.ACTIVE.getValue())
                .in("id", userIds));
        Set<Long> found = users.stream().map(SystemUserEntity::getId).collect(Collectors.toSet());
        if (found.size() != new LinkedHashSet<>(userIds).size()) {
            throw new BizException("审批或抄送人员不存在或已停用");
        }
    }

    private List<QuoteApprovalMemberResponse> buildApprovalMemberResponses(
            List<SalesQuoteApprovalMemberEntity> members,
            Long tenantId
    ) {
        if (members.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = members.stream()
                .map(SalesQuoteApprovalMemberEntity::getSystemUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SystemUserEntity> userMap = systemUserMapper.selectList(new QueryWrapper<SystemUserEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("id", userIds))
                .stream()
                .collect(Collectors.toMap(SystemUserEntity::getId, item -> item));
        return members.stream()
                .map(member -> {
                    SystemUserEntity user = userMap.get(member.getSystemUserId());
                    return QuoteApprovalMemberResponse.fromEntity(
                            member,
                            user == null ? null : user.getUsername(),
                            user == null ? null : user.getRealName()
                    );
                })
                .toList();
    }

    private List<Long> cleanIds(List<QuoteApprovalMemberRequest> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream()
                .filter(Objects::nonNull)
                .map(QuoteApprovalMemberRequest::systemUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private List<Long> concat(List<Long> left, List<Long> right) {
        Map<Long, Long> merged = new LinkedHashMap<>();
        for (Long id : left) {
            merged.put(id, id);
        }
        for (Long id : right) {
            merged.put(id, id);
        }
        return new ArrayList<>(merged.values());
    }

    private void insertApprovalMembers(Long tenantId, List<Long> userIds, String memberType, String operator) {
        int order = 1;
        for (Long userId : userIds) {
            SalesQuoteApprovalMemberEntity entity = new SalesQuoteApprovalMemberEntity();
            entity.setTenantId(tenantId);
            entity.setMemberType(memberType);
            entity.setSystemUserId(userId);
            entity.setStepOrder(QuoteApprovalMemberType.APPROVER.getValue().equals(memberType) ? order++ : 0);
            entity.setCreatedBy(operator);
            entity.setIsDeleted(false);
            approvalMemberMapper.insert(entity);
        }
    }

    private QueryWrapper<SalesQuoteResourceRuleEntity> resourceRuleQuery(Long tenantId) {
        return new QueryWrapper<SalesQuoteResourceRuleEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesQuoteResourceRuleEntity> resourceRuleUpdate(Long tenantId) {
        return new UpdateWrapper<SalesQuoteResourceRuleEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesQuoteGuideLevelEntity> guideLevelQuery(Long tenantId) {
        return new QueryWrapper<SalesQuoteGuideLevelEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesQuoteGuideLevelEntity> guideLevelUpdate(Long tenantId) {
        return new UpdateWrapper<SalesQuoteGuideLevelEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesQuoteGuideRuleEntity> guideRuleQuery(Long tenantId) {
        return new QueryWrapper<SalesQuoteGuideRuleEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesQuoteGuideRuleEntity> guideRuleUpdate(Long tenantId) {
        return new UpdateWrapper<SalesQuoteGuideRuleEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesQuoteGroundAgentRuleEntity> groundAgentQuery(Long tenantId) {
        return new QueryWrapper<SalesQuoteGroundAgentRuleEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesQuoteGroundAgentRuleEntity> groundAgentUpdate(Long tenantId) {
        return new UpdateWrapper<SalesQuoteGroundAgentRuleEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private QueryWrapper<SalesQuoteApprovalMemberEntity> approvalMemberQuery(Long tenantId) {
        return new QueryWrapper<SalesQuoteApprovalMemberEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private UpdateWrapper<SalesQuoteApprovalMemberEntity> approvalMemberUpdate(Long tenantId) {
        return new UpdateWrapper<SalesQuoteApprovalMemberEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    private void fillSoftDelete(com.mtravel.platform.common.TenantSoftDeleteEntity entity, String operator) {
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);
    }

    private BigDecimal nonNegative(BigDecimal value, String message) {
        BigDecimal actual = value == null ? ZERO : value;
        if (actual.compareTo(ZERO) < 0) {
            throw new BizException(message);
        }
        return actual;
    }

    private Integer number(Integer value) {
        return value == null ? 0 : value;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }
}
