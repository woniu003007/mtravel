package com.mtravel.platform.purchase.resource.material.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.resource.material.dto.ResourceIntroductionExtensionBlock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 资源介绍扩展内容块的 JSON 编解码及业务边界校验。 */
@Component
public class ResourceIntroductionExtensionBlockCodec {

    public static final String PHOTO_RECOMMENDATION = "photo_recommendation";
    public static final String WARM_TIP = "warm_tip";
    public static final String GENERIC = "generic";
    public static final String MULTILINE = "multiline";
    public static final String ITEMS = "items";
    private static final int MAX_BLOCKS = 10;
    private static final int MAX_ITEMS_PER_BLOCK = 20;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_ITEM_LENGTH = 1000;
    private static final Set<String> SUPPORTED_TYPES = Set.of(GENERIC, PHOTO_RECOMMENDATION, WARM_TIP);
    private static final Set<String> SUPPORTED_MODES = Set.of(MULTILINE, ITEMS);
    private static final TypeReference<List<ResourceIntroductionExtensionBlock>> BLOCK_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ResourceIntroductionExtensionBlockCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 对用户输入做清洗，并拒绝不支持的模块类型或超限内容。 */
    public List<ResourceIntroductionExtensionBlock> normalize(Collection<ResourceIntroductionExtensionBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) return List.of();
        if (blocks.size() > MAX_BLOCKS) throw new BizException("扩展内容模块不能超过10个");
        List<ResourceIntroductionExtensionBlock> result = new ArrayList<>();
        for (ResourceIntroductionExtensionBlock block : blocks) {
            if (block == null || !StringUtils.hasText(block.type())) {
                throw new BizException("扩展内容模块类型不能为空");
            }
            String type = block.type().trim();
            if (!SUPPORTED_TYPES.contains(type)) throw new BizException("不支持的扩展内容模块类型");
            String title = StringUtils.hasText(block.title()) ? block.title().trim() : defaultTitle(type);
            if (title.length() > MAX_TITLE_LENGTH) throw new BizException("扩展内容模块标题不能超过100个字符");
            String titleColor = normalizeColor(block.titleColor(), type);
            String contentMode = StringUtils.hasText(block.contentMode()) && SUPPORTED_MODES.contains(block.contentMode())
                    ? block.contentMode() : ITEMS;
            if (MULTILINE.equals(contentMode)) {
                String content = StringUtils.hasText(block.content())
                        ? block.content().strip() : joinItems(block.items());
                if (!StringUtils.hasText(content)) throw new BizException("扩展内容模块至少填写一条内容");
                if (content.length() > 20000) throw new BizException("扩展内容模块文本不能超过20000个字符");
                result.add(new ResourceIntroductionExtensionBlock(GENERIC, title, titleColor, MULTILINE, content, List.of()));
                continue;
            }
            if (block.items() != null && block.items().size() > MAX_ITEMS_PER_BLOCK) {
                throw new BizException("每个扩展内容模块最多20条内容");
            }
            List<String> items = block.items() == null ? List.of() : block.items().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .peek(item -> {
                        if (item.length() > MAX_ITEM_LENGTH) throw new BizException("扩展内容条目不能超过1000个字符");
                    })
                    .toList();
            if (items.isEmpty()) throw new BizException("扩展内容模块至少填写一条内容");
            result.add(new ResourceIntroductionExtensionBlock(GENERIC, title, titleColor, ITEMS, null, items));
        }
        return result;
    }

    public String encode(Collection<ResourceIntroductionExtensionBlock> blocks) {
        try {
            return objectMapper.writeValueAsString(normalize(blocks));
        } catch (JsonProcessingException ex) {
            throw new BizException("扩展内容模块保存失败");
        }
    }

    /** 异常历史数据按空模块处理，避免资料页整体无法打开。 */
    public List<ResourceIntroductionExtensionBlock> decode(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try {
            return normalize(objectMapper.readValue(value, BLOCK_LIST));
        } catch (JsonProcessingException | BizException ex) {
            return List.of();
        }
    }

    private String defaultTitle(String type) {
        if (PHOTO_RECOMMENDATION.equals(type)) return "拍照机位推荐：";
        if (WARM_TIP.equals(type)) return "温馨提示：";
        return "扩展内容：";
    }

    private String normalizeColor(String value, String legacyType) {
        if (StringUtils.hasText(value) && value.trim().matches("#[0-9a-fA-F]{6}")) {
            return value.trim().toLowerCase();
        }
        // 未指定颜色时沿用产品 Word 母版的扩展模块标题橙色。
        if (PHOTO_RECOMMENDATION.equals(legacyType)) return "#e36c09";
        if (WARM_TIP.equals(legacyType)) return "#0070c0";
        return "#e36c09";
    }

    private String joinItems(List<String> items) {
        if (items == null) return null;
        return items.stream().filter(StringUtils::hasText).map(String::trim)
                .reduce((left, right) -> left + "\n" + right).orElse(null);
    }
}
