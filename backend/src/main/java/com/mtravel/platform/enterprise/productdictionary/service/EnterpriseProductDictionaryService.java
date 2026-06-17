package com.mtravel.platform.enterprise.productdictionary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.common.BusinessCrudService;
import com.mtravel.platform.common.PageResult;
import com.mtravel.platform.enterprise.productdictionary.dto.EnterpriseProductDictionaryResponse;
import com.mtravel.platform.enterprise.productdictionary.dto.EnterpriseProductDictionarySaveRequest;
import com.mtravel.platform.enterprise.productdictionary.entity.EnterpriseProductDictionaryEntity;
import com.mtravel.platform.enterprise.productdictionary.enums.EnterpriseProductDictionaryStatus;
import com.mtravel.platform.enterprise.productdictionary.enums.EnterpriseProductDictionaryType;
import com.mtravel.platform.enterprise.productdictionary.mapper.EnterpriseProductDictionaryMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 产品字典业务服务。
 *
 * <p>产品字典是销售产品模板的基础选项。这里集中处理租户隔离、字典类型校验、
 * 同类型名称唯一性、启停状态默认值和软删除，避免后续产品页面出现各自维护选项的情况。</p>
 */
@Service
public class EnterpriseProductDictionaryService
        extends BusinessCrudService<EnterpriseProductDictionaryEntity, EnterpriseProductDictionaryResponse> {

    private final EnterpriseProductDictionaryMapper mapper;

    public EnterpriseProductDictionaryService(EnterpriseProductDictionaryMapper mapper) {
        super(mapper);
        this.mapper = mapper;
    }

    /**
     * 分页查询产品字典。
     *
     * @param tenantId 当前租户ID
     * @param dictType 字典类型筛选，为空时查全部类型
     * @param keyword 字典名称模糊搜索
     * @param status 启停状态筛选
     * @param page 当前页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    public PageResult<EnterpriseProductDictionaryResponse> page(
            Long tenantId,
            String dictType,
            String keyword,
            String status,
            long page,
            long pageSize
    ) {
        QueryWrapper<EnterpriseProductDictionaryEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(dictType), "dict_type", cleanType(dictType))
                .eq(StringUtils.hasText(status), "status", EnterpriseProductDictionaryStatus.fromValueOrDefault(status).getValue())
                .like(StringUtils.hasText(keyword), "dict_name", keyword == null ? null : keyword.trim())
                .orderByAsc("dict_type")
                .orderByAsc("sort_order")
                .orderByAsc("dict_name")
                .orderByDesc("id");
        return pageByWrapper(wrapper, page, pageSize);
    }

    /**
     * 查询启用产品字典。
     *
     * <p>用于产品模板下拉选择。只返回 active 状态，避免停用选项继续进入新增产品。</p>
     *
     * @param tenantId 当前租户ID
     * @param dictType 可选字典类型
     * @return 启用字典列表
     */
    public List<EnterpriseProductDictionaryResponse> listActive(Long tenantId, String dictType) {
        QueryWrapper<EnterpriseProductDictionaryEntity> wrapper = baseQuery(tenantId)
                .eq(StringUtils.hasText(dictType), "dict_type", cleanType(dictType))
                .eq("status", EnterpriseProductDictionaryStatus.ACTIVE.getValue())
                .orderByAsc("dict_type")
                .orderByAsc("sort_order")
                .orderByAsc("dict_name");
        return mapper.selectList(wrapper).stream()
                .map(EnterpriseProductDictionaryResponse::fromEntity)
                .toList();
    }

    /**
     * 新增产品字典。
     *
     * <p>同一租户、同一字典类型下的字典名称不能重复，避免产品页面出现两个同名选项。</p>
     *
     * @param request 保存请求
     * @param tenantId 当前租户ID
     * @param operator 当前操作人
     * @return 新增后的字典详情
     */
    public EnterpriseProductDictionaryResponse create(
            EnterpriseProductDictionarySaveRequest request,
            Long tenantId,
            String operator
    ) {
        assertUnique(tenantId, request.dictType(), request.dictName(), null);
        EnterpriseProductDictionaryEntity entity = new EnterpriseProductDictionaryEntity();
        entity.setTenantId(tenantId);
        applyFields(entity, request);
        entity.setCreatedBy(operator);
        entity.setIsDeleted(false);
        mapper.insert(entity);
        return detail(entity.getId(), tenantId);
    }

    /**
     * 修改产品字典。
     *
     * @param id 字典ID
     * @param request 保存请求
     * @param tenantId 当前租户ID
     * @return 修改后的字典详情
     */
    public EnterpriseProductDictionaryResponse update(
            Long id,
            EnterpriseProductDictionarySaveRequest request,
            Long tenantId
    ) {
        assertUnique(tenantId, request.dictType(), request.dictName(), id);
        EnterpriseProductDictionaryStatus status = EnterpriseProductDictionaryStatus.fromValueOrDefault(request.status());
        int updated = mapper.update(null, baseUpdate(tenantId)
                .eq("id", id)
                .set("dict_type", cleanType(request.dictType()))
                .set("dict_name", cleanRequired(request.dictName()))
                .set("sort_order", number(request.sortOrder()))
                .set("status", status.getValue())
                .set("remark", clean(request.remark())));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
        return detail(id, tenantId);
    }

    /** 将保存请求写入实体，并补齐默认排序和状态。 */
    private void applyFields(EnterpriseProductDictionaryEntity entity, EnterpriseProductDictionarySaveRequest request) {
        EnterpriseProductDictionaryStatus status = EnterpriseProductDictionaryStatus.fromValueOrDefault(request.status());
        entity.setDictType(cleanType(request.dictType()));
        entity.setDictName(cleanRequired(request.dictName()));
        entity.setSortOrder(number(request.sortOrder()));
        entity.setStatus(status.getValue());
        entity.setRemark(clean(request.remark()));
    }

    /** 校验同一租户、同一字典类型下未删除字典名称唯一。 */
    private void assertUnique(Long tenantId, String dictType, String dictName, Long excludeId) {
        String cleanedType = cleanType(dictType);
        String cleanedName = cleanRequired(dictName);
        Long count = mapper.selectCount(baseQuery(tenantId)
                .eq("dict_type", cleanedType)
                .eq("dict_name", cleanedName)
                .ne(excludeId != null, "id", excludeId));
        if (count != null && count > 0) {
            throw new BizException("产品字典已存在");
        }
    }

    /** 清洗并校验字典类型，保证 Service 层不信任前端字符串。 */
    private String cleanType(String value) {
        return EnterpriseProductDictionaryType.fromValue(value).getValue();
    }

    @Override
    protected EnterpriseProductDictionaryEntity newEntity() {
        return new EnterpriseProductDictionaryEntity();
    }

    @Override
    protected EnterpriseProductDictionaryResponse toResponse(EnterpriseProductDictionaryEntity entity) {
        return EnterpriseProductDictionaryResponse.fromEntity(entity);
    }

    @Override
    protected String notFoundMessage() {
        return "产品字典不存在或已删除";
    }
}
