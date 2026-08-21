package com.mtravel.platform.purchase.resource.material.dto;

import java.util.List;

/**
 * 资源介绍中可按顺序输出的扩展内容块。
 *
 * <p>模块标题、标题颜色和录入形式由用户配置；旧类型字段只用于兼容历史数据。</p>
 */
public record ResourceIntroductionExtensionBlock(
        String type,
        String title,
        String titleColor,
        String contentMode,
        String content,
        List<String> items
) {}
