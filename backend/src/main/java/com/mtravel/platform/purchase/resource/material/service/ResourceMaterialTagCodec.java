package com.mtravel.platform.purchase.resource.material.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.BizException;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 资源介绍和图片标签的结构化 JSON 编解码器。 */
@Component
public class ResourceMaterialTagCodec {

    private static final TypeReference<List<String>> TAG_LIST = new TypeReference<>() {};
    private final ObjectMapper objectMapper;

    public ResourceMaterialTagCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 清理空标签、重复标签和首尾空白，保持接口返回顺序稳定。 */
    public List<String> normalize(Collection<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(10)
                .toList();
    }

    /** 将标签列表编码为数据库 JSON 数组。 */
    public String encode(Collection<String> tags) {
        try {
            return objectMapper.writeValueAsString(normalize(tags));
        } catch (JsonProcessingException ex) {
            throw new BizException("资源标签保存失败");
        }
    }

    /** 将数据库 JSON 数组解码为接口标签列表。 */
    public List<String> decode(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return normalize(objectMapper.readValue(value, TAG_LIST));
        } catch (JsonProcessingException ex) {
            throw new BizException("资源标签数据格式错误");
        }
    }
}
