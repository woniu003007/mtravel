package com.mtravel.platform.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 基础资料 CRUD 服务基类。
 *
 * <p>本基类只封装多租户、软删除、分页和通用清洗逻辑。每个业务模块仍需在自己的 Service
 * 中实现字段赋值、状态校验、业务唯一性、外键有效性等规则，避免把业务差异塞进通用层。</p>
 *
 * @param <E> 数据库实体类型
 * @param <R> 接口返回 DTO 类型
 */
public abstract class BusinessCrudService<E extends TenantSoftDeleteEntity, R> {

    private final BaseMapper<E> mapper;

    protected BusinessCrudService(BaseMapper<E> mapper) {
        this.mapper = mapper;
    }

    /**
     * 按租户查询未删除记录。
     */
    protected QueryWrapper<E> baseQuery(Long tenantId) {
        return new QueryWrapper<E>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    /**
     * 按租户更新未删除记录，所有修改和软删除都必须从这里带上租户边界。
     */
    protected UpdateWrapper<E> baseUpdate(Long tenantId) {
        return new UpdateWrapper<E>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false);
    }

    /**
     * 查询详情。未找到时抛业务异常，防止前端把空对象当成有效数据。
     */
    public R detail(Long id, Long tenantId) {
        E entity = mapper.selectOne(baseQuery(tenantId).eq("id", id));
        if (entity == null) {
            throw new BizException(notFoundMessage());
        }
        return toResponse(entity);
    }

    /**
     * 通用分页方法。模块 Service 传入已补充筛选条件的 wrapper，本方法负责执行分页和 DTO 转换。
     */
    protected PageResult<R> pageByWrapper(QueryWrapper<E> wrapper, long page, long pageSize) {
        Page<E> result = mapper.selectPage(Page.of(page, pageSize), wrapper);
        List<R> items = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(items, result.getTotal());
    }

    /**
     * 通用软删除。业务资料可能被历史订单、合同、账款引用，因此删除只标记状态，不做物理删除。
     */
    public void delete(Long id, Long tenantId, String operator) {
        E entity = newEntity();
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(operator);
        int updated = mapper.update(entity, baseUpdate(tenantId).eq("id", id));
        if (updated == 0) {
            throw new BizException(notFoundMessage());
        }
    }

    /**
     * 检查同租户未删除记录是否存在重复值。
     */
    protected void assertValueNotExists(
            Long tenantId,
            String column,
            Object value,
            Long excludeId,
            String message
    ) {
        if (value == null || (value instanceof String text && !StringUtils.hasText(text))) {
            return;
        }
        QueryWrapper<E> wrapper = baseQuery(tenantId)
                .eq(column, value)
                .ne(excludeId != null, "id", excludeId);
        Long count = mapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException(message);
        }
    }

    protected BaseMapper<E> mapper() {
        return mapper;
    }

    protected String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    protected String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }

    protected BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    protected Integer number(Integer value) {
        return value == null ? 0 : value;
    }

    protected abstract E newEntity();

    protected abstract R toResponse(E entity);

    protected abstract String notFoundMessage();
}
