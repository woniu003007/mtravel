package com.mtravel.platform.enterprise.guide.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.configuration.quote.entity.SalesQuoteGuideLevelEntity;
import com.mtravel.platform.configuration.quote.mapper.SalesQuoteGuideLevelMapper;
import com.mtravel.platform.enterprise.employee.entity.EnterpriseEmployeeEntity;
import com.mtravel.platform.enterprise.employee.mapper.EnterpriseEmployeeMapper;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideResponse;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideSaveRequest;
import com.mtravel.platform.enterprise.guide.dto.EnterpriseGuideTagResponse;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideEntity;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideTagEntity;
import com.mtravel.platform.enterprise.guide.entity.EnterpriseGuideTagRelationEntity;
import com.mtravel.platform.enterprise.guide.enums.EnterpriseGuideEnterpriseCodeStatus;
import com.mtravel.platform.enterprise.guide.enums.EnterpriseGuideGender;
import com.mtravel.platform.enterprise.guide.enums.EnterpriseGuideStatus;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideMapper;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideTagMapper;
import com.mtravel.platform.enterprise.guide.mapper.EnterpriseGuideTagRelationMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
 * 企业导游档案业务服务。
 *
 * <p>本服务集中处理导游档案的租户隔离、软删除、编码/用户名/证件号查重、所属导管、
 * 导游标签关系、企业码邀请状态和导游展示资料。导游被团队安排、备用金和结算引用，
 * 因此删除只做软删除，不做物理删除。</p>
 */
@Service
public class EnterpriseGuideService extends BusinessCrudService<EnterpriseGuideEntity, EnterpriseGuideResponse> {

    private static final BigDecimal MIN_RATING = BigDecimal.ZERO;
    private static final BigDecimal MAX_RATING = BigDecimal.valueOf(5);

    private final EnterpriseGuideMapper guideMapper;
    private final EnterpriseGuideTagMapper tagMapper;
    private final EnterpriseGuideTagRelationMapper relationMapper;
    private final EnterpriseEmployeeMapper employeeMapper;
    private final SalesQuoteGuideLevelMapper guideLevelMapper;

    public EnterpriseGuideService(
            EnterpriseGuideMapper guideMapper,
            EnterpriseGuideTagMapper tagMapper,
            EnterpriseGuideTagRelationMapper relationMapper,
            EnterpriseEmployeeMapper employeeMapper,
            SalesQuoteGuideLevelMapper guideLevelMapper
    ) {
        super(guideMapper);
        this.guideMapper = guideMapper;
        this.tagMapper = tagMapper;
        this.relationMapper = relationMapper;
        this.employeeMapper = employeeMapper;
        this.guideLevelMapper = guideLevelMapper;
    }

    /**
     * 分页查询导游档案。
     *
     * @param tenantId 当前租户 ID
     * @param keyword 导游名称、用户名、证件号、电话或手机关键字
     * @param enterpriseCodeStatus 企业码状态筛选
     * @param status 导游档案状态筛选
     * @param guideManagerEmployeeId 所属导管员工 ID 筛选
     * @param tagId 导游标签 ID 筛选
     * @param page 当前页，从 1 开始
     * @param pageSize 每页条数
     * @return 导游分页结果
     */
    public PageResult<EnterpriseGuideResponse> page(
            Long tenantId,
            String keyword,
            String enterpriseCodeStatus,
            String status,
            Long guideManagerEmployeeId,
            Long tagId,
            long page,
            long pageSize
    ) {
        QueryWrapper<EnterpriseGuideEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(enterpriseCodeStatus), "enterprise_code_status", enterpriseCodeStatus)
                .eq(StringUtils.hasText(status), "status", status)
                .eq(guideManagerEmployeeId != null, "guide_manager_employee_id", guideManagerEmployeeId)
                .in(tagId != null, "id", guideIdsByTag(tenantId, tagId))
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("guide_code", keyword)
                        .or()
                        .like("guide_name", keyword)
                        .or()
                        .like("username", keyword)
                        .or()
                        .like("certificate_no", keyword)
                        .or()
                        .like("telephone", keyword)
                        .or()
                        .like("mobile_phone", keyword)
                        .or()
                        .like("guide_manager_name", keyword))
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return enrichPage(pageByWrapper(wrapper, page, pageSize), tenantId);
    }

    /**
     * 查询全部未删除导游。
     *
     * <p>用于团队安排、导游选择下拉和后续导游排班模块。includeDisabled=false 时只返回启用导游。</p>
     */
    public List<EnterpriseGuideResponse> listAll(Long tenantId, boolean includeDisabled) {
        List<EnterpriseGuideEntity> entities = guideMapper.selectList(baseQuery(tenantId)
                .eq(!includeDisabled, "status", EnterpriseGuideStatus.ACTIVE.getValue())
                .orderByAsc("sort_order")
                .orderByAsc("id"));
        return enrichList(entities, tenantId);
    }

    /**
     * 查询单个导游详情。
     *
     * <p>详情会附带导游标签，便于前端编辑表单回显多选标签。</p>
     */
    @Override
    public EnterpriseGuideResponse detail(Long id, Long tenantId) {
        EnterpriseGuideEntity entity = guideMapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException(notFoundMessage());
        }
        return EnterpriseGuideResponse.fromEntity(entity, tagsByGuideIds(tenantId, List.of(id)).getOrDefault(id, List.of()));
    }

    /**
     * 新增导游档案。
     *
     * <p>新增前会校验导游编码、用户名、证件号在同一租户未删除数据中不重复，同时校验导管员工和标签有效。</p>
     */
    @Transactional
    public EnterpriseGuideResponse create(
            EnterpriseGuideSaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertGuideUnique(request, tenantId, null);
        EnterpriseEmployeeEntity manager = resolveGuideManager(request.guideManagerEmployeeId(), tenantId);
        SalesQuoteGuideLevelEntity guideLevel = resolveGuideLevel(request.guideLevelId(), tenantId);
        List<Long> tagIds = assertTagsActive(request.tagIds(), tenantId);
        EnterpriseGuideEntity entity = new EnterpriseGuideEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request, manager, guideLevel);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        guideMapper.insert(entity);
        replaceTagRelations(entity.getId(), tagIds, tenantId, operator);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改导游档案。
     *
     * <p>更新使用显式 set，保证前端清空手机号、账号、备注等可选字段时能够写回数据库。</p>
     */
    @Transactional
    public EnterpriseGuideResponse update(Long id, EnterpriseGuideSaveRequest request, Long tenantId) {
        assertGuideUnique(request, tenantId, id);
        assertGuideExists(id, tenantId);
        EnterpriseEmployeeEntity manager = resolveGuideManager(request.guideManagerEmployeeId(), tenantId);
        SalesQuoteGuideLevelEntity guideLevel = resolveGuideLevel(request.guideLevelId(), tenantId);
        List<Long> tagIds = assertTagsActive(request.tagIds(), tenantId);
        assertRatingValid(request.rating());
        assertTotalToursValid(request.totalTours());
        assertNonNegative(request.age(), "导游年龄不能小于0");
        assertNonNegative(request.workingYears(), "导游从业年数不能小于0");
        int updated = guideMapper.update(null, baseUpdate(tenantId)
                .eq("id", id)
                .set("guide_code", clean(request.guideCode()))
                .set("guide_name", cleanRequired(request.guideName()))
                .set("username", clean(request.username()))
                .set("guide_manager_employee_id", manager == null ? null : manager.getId())
                .set("guide_manager_name", manager == null ? null : manager.getEmployeeName())
                .set("guide_level_id", guideLevel == null ? null : guideLevel.getId())
                .set("guide_level_name", guideLevel == null ? null : guideLevel.getLevelName())
                .set("gender", EnterpriseGuideGender.fromValueOrDefault(request.gender()).getValue())
                .set("certificate_no", clean(request.certificateNo()))
                .set("id_card_no", clean(request.idCardNo()))
                .set("telephone", clean(request.telephone()))
                .set("fax", clean(request.fax()))
                .set("mobile_phone", clean(request.mobilePhone()))
                .set("bank_name", clean(request.bankName()))
                .set("bank_account_no", clean(request.bankAccountNo()))
                .set("alipay_name", clean(request.alipayName()))
                .set("alipay_account", clean(request.alipayAccount()))
                .set("enterprise_code_account", clean(request.enterpriseCodeAccount()))
                .set("enterprise_code_status", EnterpriseGuideEnterpriseCodeStatus.fromValueOrDefault(request.enterpriseCodeStatus()).getValue())
                .set("status", EnterpriseGuideStatus.fromValueOrDefault(request.status()).getValue())
                .set("age", request.age())
                .set("native_place", clean(request.nativePlace()))
                .set("working_years", request.workingYears())
                .set("languages", clean(request.languages()))
                .set("personal_intro", clean(request.personalIntro()))
                .set("certificate_file_url", clean(request.certificateFileUrl()))
                .set("photo_url", clean(request.photoUrl()))
                .set("rating", rating(request.rating()))
                .set("total_tours", number(request.totalTours()))
                .set("sort_order", number(request.sortOrder()))
                .set("remark", clean(request.remark())));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        replaceTagRelations(id, tagIds, tenantId, null);
        return detail(id, tenantId);
    }

    /**
     * 停用导游档案。
     *
     * <p>停用后导游不再进入新团队安排选择，历史团队和结算引用仍保留。</p>
     */
    public void disable(Long id, Long tenantId) {
        EnterpriseGuideEntity entity = new EnterpriseGuideEntity();
        entity.setStatus(EnterpriseGuideStatus.DISABLED.getValue());
        int updated = guideMapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
    }

    /**
     * 发送企业码邀请。
     *
     * <p>当前版本只记录“已获取签约链接”和邀请时间，不在这里对接真实企业码或短信网关。</p>
     */
    public void sendEnterpriseCodeInvite(Long id, Long tenantId) {
        EnterpriseGuideEntity entity = new EnterpriseGuideEntity();
        entity.setEnterpriseCodeStatus(EnterpriseGuideEnterpriseCodeStatus.INVITE_LINK.getValue());
        entity.setEnterpriseCodeInvitedAt(OffsetDateTime.now());
        int updated = guideMapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
    }

    /**
     * 软删除导游档案。
     *
     * <p>删除只更新软删除字段，避免历史排团、备用金和结算记录失去导游归属。</p>
     */
    @Override
    @Transactional
    public void delete(Long id, Long tenantId, String operator) {
        EnterpriseGuideEntity entity = new EnterpriseGuideEntity();
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);
        int updated = guideMapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        softDeleteTagRelations(id, tenantId, operator);
    }

    private void assertGuideUnique(EnterpriseGuideSaveRequest request, Long tenantId, Long excludeId) {
        assertValueNotExists(tenantId, "guide_code", clean(request.guideCode()), excludeId, "导游编码已存在");
        assertValueNotExists(tenantId, "username", clean(request.username()), excludeId, "导游用户名已存在");
        assertValueNotExists(tenantId, "certificate_no", clean(request.certificateNo()), excludeId, "导游证件号已存在");
    }

    private EnterpriseGuideEntity assertGuideExists(Long id, Long tenantId) {
        EnterpriseGuideEntity entity = guideMapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException(notFoundMessage());
        }
        return entity;
    }

    private void applyFields(
            EnterpriseGuideEntity entity,
            EnterpriseGuideSaveRequest request,
            EnterpriseEmployeeEntity manager,
            SalesQuoteGuideLevelEntity guideLevel
    ) {
        assertRatingValid(request.rating());
        assertTotalToursValid(request.totalTours());
        assertNonNegative(request.age(), "导游年龄不能小于0");
        assertNonNegative(request.workingYears(), "导游从业年数不能小于0");
        entity.setGuideCode(clean(request.guideCode()));
        entity.setGuideName(cleanRequired(request.guideName()));
        entity.setUsername(clean(request.username()));
        entity.setGuideManagerEmployeeId(manager == null ? null : manager.getId());
        entity.setGuideManagerName(manager == null ? null : manager.getEmployeeName());
        entity.setGuideLevelId(guideLevel == null ? null : guideLevel.getId());
        entity.setGuideLevelName(guideLevel == null ? null : guideLevel.getLevelName());
        entity.setGender(EnterpriseGuideGender.fromValueOrDefault(request.gender()).getValue());
        entity.setCertificateNo(clean(request.certificateNo()));
        entity.setIdCardNo(clean(request.idCardNo()));
        entity.setTelephone(clean(request.telephone()));
        entity.setFax(clean(request.fax()));
        entity.setMobilePhone(clean(request.mobilePhone()));
        entity.setBankName(clean(request.bankName()));
        entity.setBankAccountNo(clean(request.bankAccountNo()));
        entity.setAlipayName(clean(request.alipayName()));
        entity.setAlipayAccount(clean(request.alipayAccount()));
        entity.setEnterpriseCodeAccount(clean(request.enterpriseCodeAccount()));
        entity.setEnterpriseCodeStatus(EnterpriseGuideEnterpriseCodeStatus.fromValueOrDefault(request.enterpriseCodeStatus()).getValue());
        entity.setStatus(EnterpriseGuideStatus.fromValueOrDefault(request.status()).getValue());
        entity.setAge(request.age());
        entity.setNativePlace(clean(request.nativePlace()));
        entity.setWorkingYears(request.workingYears());
        entity.setLanguages(clean(request.languages()));
        entity.setPersonalIntro(clean(request.personalIntro()));
        entity.setCertificateFileUrl(clean(request.certificateFileUrl()));
        entity.setPhotoUrl(clean(request.photoUrl()));
        entity.setRating(rating(request.rating()));
        entity.setTotalTours(number(request.totalTours()));
        entity.setSortOrder(number(request.sortOrder()));
        entity.setRemark(clean(request.remark()));
    }

    private EnterpriseEmployeeEntity resolveGuideManager(Long employeeId, Long tenantId) {
        if (employeeId == null) {
            return null;
        }
        EnterpriseEmployeeEntity employee = employeeMapper.selectOne(new QueryWrapper<EnterpriseEmployeeEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("id", employeeId));
        if (employee == null) {
            throw new BizException("所属导管员工不存在或已停用");
        }
        return employee;
    }

    private SalesQuoteGuideLevelEntity resolveGuideLevel(Long guideLevelId, Long tenantId) {
        if (guideLevelId == null) {
            return null;
        }
        SalesQuoteGuideLevelEntity level = guideLevelMapper.selectOne(new QueryWrapper<SalesQuoteGuideLevelEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("id", guideLevelId));
        if (level == null) {
            throw new BizException("导游等级不存在或已停用");
        }
        return level;
    }

    private List<Long> assertTagsActive(List<Long> tagIds, Long tenantId) {
        List<Long> cleaned = cleanIds(tagIds);
        if (cleaned.isEmpty()) {
            return List.of();
        }
        List<EnterpriseGuideTagEntity> tags = tagMapper.selectList(new QueryWrapper<EnterpriseGuideTagEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .in("id", cleaned));
        Set<Long> found = tags.stream().map(EnterpriseGuideTagEntity::getId).collect(Collectors.toSet());
        if (found.size() != cleaned.size()) {
            throw new BizException("导游标签不存在或已停用");
        }
        return cleaned;
    }

    private List<Long> cleanIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private void replaceTagRelations(Long guideId, List<Long> tagIds, Long tenantId, String operator) {
        softDeleteTagRelations(guideId, tenantId, operator);
        for (Long tagId : tagIds) {
            EnterpriseGuideTagRelationEntity relation = new EnterpriseGuideTagRelationEntity();
            relation.setTenantId(tenantId);
            relation.setGuideId(guideId);
            relation.setTagId(tagId);
            relation.setCreatedBy(operator);
            relation.setIsDeleted(false);
            relationMapper.insert(relation);
        }
    }

    private void softDeleteTagRelations(Long guideId, Long tenantId, String operator) {
        EnterpriseGuideTagRelationEntity relation = new EnterpriseGuideTagRelationEntity();
        relation.setIsDeleted(true);
        relation.setDeletedAt(OffsetDateTime.now());
        relation.setDeletedBy(operator);
        relationMapper.update(relation, new UpdateWrapper<EnterpriseGuideTagRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("guide_id", guideId)
                .eq("is_deleted", false));
    }

    private List<Long> guideIdsByTag(Long tenantId, Long tagId) {
        if (tagId == null) {
            return List.of();
        }
        List<Long> ids = relationMapper.selectList(new QueryWrapper<EnterpriseGuideTagRelationEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .eq("tag_id", tagId))
                .stream()
                .map(EnterpriseGuideTagRelationEntity::getGuideId)
                .toList();
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    private PageResult<EnterpriseGuideResponse> enrichPage(PageResult<EnterpriseGuideResponse> page, Long tenantId) {
        List<Long> guideIds = page.items().stream().map(EnterpriseGuideResponse::id).toList();
        Map<Long, List<EnterpriseGuideTagResponse>> tagMap = tagsByGuideIds(tenantId, guideIds);
        List<EnterpriseGuideResponse> items = page.items().stream()
                .map(item -> EnterpriseGuideResponse.fromEntity(entityFromResponse(item), tagMap.getOrDefault(item.id(), List.of())))
                .toList();
        return new PageResult<>(items, page.total());
    }

    private List<EnterpriseGuideResponse> enrichList(List<EnterpriseGuideEntity> entities, Long tenantId) {
        List<Long> guideIds = entities.stream().map(EnterpriseGuideEntity::getId).toList();
        Map<Long, List<EnterpriseGuideTagResponse>> tagMap = tagsByGuideIds(tenantId, guideIds);
        return entities.stream()
                .map(entity -> EnterpriseGuideResponse.fromEntity(entity, tagMap.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    private Map<Long, List<EnterpriseGuideTagResponse>> tagsByGuideIds(Long tenantId, List<Long> guideIds) {
        if (guideIds == null || guideIds.isEmpty()) {
            return Map.of();
        }
        List<EnterpriseGuideTagRelationEntity> relations = relationMapper.selectList(new QueryWrapper<EnterpriseGuideTagRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("guide_id", guideIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        List<Long> tagIds = relations.stream()
                .map(EnterpriseGuideTagRelationEntity::getTagId)
                .distinct()
                .toList();
        Map<Long, EnterpriseGuideTagResponse> tags = tagMapper.selectList(new QueryWrapper<EnterpriseGuideTagEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", false)
                        .in("id", tagIds))
                .stream()
                .map(EnterpriseGuideTagResponse::fromEntity)
                .collect(Collectors.toMap(EnterpriseGuideTagResponse::id, tag -> tag));
        return relations.stream()
                .filter(relation -> tags.containsKey(relation.getTagId()))
                .collect(Collectors.groupingBy(
                        EnterpriseGuideTagRelationEntity::getGuideId,
                        Collectors.mapping(relation -> tags.get(relation.getTagId()), Collectors.toList())
                ));
    }

    private EnterpriseGuideEntity entityFromResponse(EnterpriseGuideResponse response) {
        EnterpriseGuideEntity entity = new EnterpriseGuideEntity();
        entity.setId(response.id());
        entity.setGuideCode(response.guideCode());
        entity.setGuideName(response.guideName());
        entity.setUsername(response.username());
        entity.setGuideManagerEmployeeId(response.guideManagerEmployeeId());
        entity.setGuideManagerName(response.guideManagerName());
        entity.setGuideLevelId(response.guideLevelId());
        entity.setGuideLevelName(response.guideLevelName());
        entity.setGender(response.gender());
        entity.setCertificateNo(response.certificateNo());
        entity.setIdCardNo(response.idCardNo());
        entity.setTelephone(response.telephone());
        entity.setFax(response.fax());
        entity.setMobilePhone(response.mobilePhone());
        entity.setBankName(response.bankName());
        entity.setBankAccountNo(response.bankAccountNo());
        entity.setAlipayName(response.alipayName());
        entity.setAlipayAccount(response.alipayAccount());
        entity.setEnterpriseCodeAccount(response.enterpriseCodeAccount());
        entity.setEnterpriseCodeStatus(response.enterpriseCodeStatus());
        entity.setEnterpriseCodeInvitedAt(response.enterpriseCodeInvitedAt());
        entity.setStatus(response.status());
        entity.setAge(response.age());
        entity.setNativePlace(response.nativePlace());
        entity.setWorkingYears(response.workingYears());
        entity.setLanguages(response.languages());
        entity.setPersonalIntro(response.personalIntro());
        entity.setCertificateFileUrl(response.certificateFileUrl());
        entity.setPhotoUrl(response.photoUrl());
        entity.setRating(response.rating());
        entity.setTotalTours(response.totalTours());
        entity.setSortOrder(response.sortOrder());
        entity.setRemark(response.remark());
        entity.setCreatedBy(response.createdBy());
        entity.setCreatedAt(response.createdAt());
        entity.setUpdatedAt(response.updatedAt());
        return entity;
    }

    private BigDecimal rating(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void assertRatingValid(BigDecimal value) {
        if (value != null && (value.compareTo(MIN_RATING) < 0 || value.compareTo(MAX_RATING) > 0)) {
            throw new BizException("导游评分必须在0到5之间");
        }
    }

    private void assertTotalToursValid(Integer value) {
        assertNonNegative(value, "累计带团次数不能小于0");
    }

    private void assertNonNegative(Integer value, String message) {
        if (value != null && value < 0) {
            throw new BizException(message);
        }
    }

    @Override
    protected EnterpriseGuideEntity newEntity() {
        return new EnterpriseGuideEntity();
    }

    @Override
    protected EnterpriseGuideResponse toResponse(EnterpriseGuideEntity entity) {
        return EnterpriseGuideResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "导游不存在或已删除";
    }
}
