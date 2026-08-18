package com.mtravel.platform.common;

import java.util.List;

/**
 * 后端统一分页结果。
 *
 * @param items 当前页数据
 * @param total 符合查询条件的总记录数
 * @param <T> 列表元素类型
 */
public record PageResult<T>(
        List<T> items,
        long total
) {
}
