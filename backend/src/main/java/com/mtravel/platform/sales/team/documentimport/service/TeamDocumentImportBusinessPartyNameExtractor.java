package com.mtravel.platform.sales.team.documentimport.service;

import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 从确认单中的 FROM、TO、ATTN 业务往来方信息中识别应排除的资源名称。
 *
 * <p>该解析器不会把联系人字段本身加入排除集合。它仅在资源名称以业务方标签右侧内容开头、且该条
 * 资源看起来像误识别的地接/业务单位时才返回资源名称，避免普通联系人姓名被误删。</p>
 */
@Component
public class TeamDocumentImportBusinessPartyNameExtractor {
    private static final Pattern BUSINESS_PARTY_LABEL = Pattern.compile(
            "(?im)(?:^|[\\r\\n])\\s*(?:FROM|TO|ATTN)\\s*[:：]\\s*([^\\r\\n]+)"
    );
    private static final Pattern EMBEDDED_BUSINESS_PARTY_LABEL = Pattern.compile(
            "(?i)(?:FROM|TO|ATTN)\\s*[:：]\\s*([^\\r\\n]+)"
    );
    private static final Set<String> COMPANY_LIKE_SUFFIXES = Set.of(
            "旅行社", "旅游", "假期", "国旅", "公司", "集团", "国际"
    );

    /**
     * 从草稿本身可持久化的备注和识别依据中提取业务方名称。
     *
     * @param draft 当前任务草稿
     * @return 可安全交给资源归一化器排除的资源名称
     */
    public Set<String> fromDraft(TeamDocumentImportDraft draft) {
        if (draft == null || draft.resources() == null || draft.resources().isEmpty()) {
            return Set.of();
        }
        List<String> labelledValues = new ArrayList<>();
        if (draft.order() != null) {
            labelledValues.addAll(labelledValues(draft.order().orderRemark()));
        }
        if (draft.team() != null) {
            labelledValues.addAll(labelledValues(draft.team().remark()));
        }
        if (draft.evidence() != null) {
            draft.evidence().forEach(value -> labelledValues.addAll(labelledValues(value)));
        }
        draft.resources().stream()
                .filter(resource -> resource != null)
                .forEach(resource -> labelledValues.addAll(labelledValues(resource.remark())));
        return matchingResourceNames(draft.resources(), labelledValues);
    }

    /**
     * 新任务可额外使用 Word 原文识别业务方，历史草稿仍可仅靠 {@link #fromDraft} 安全整理。
     *
     * @param draft 当前任务草稿
     * @param sourceText 已从 Word 提取的文本，可为空
     * @return 可安全排除的资源名称
     */
    public Set<String> fromDraftAndSourceText(TeamDocumentImportDraft draft, String sourceText) {
        if (draft == null || draft.resources() == null || draft.resources().isEmpty()) {
            return Set.of();
        }
        List<String> labelledValues = new ArrayList<>();
        labelledValues.addAll(labelledValues(sourceText));
        Set<String> result = new LinkedHashSet<>(fromDraft(draft));
        result.addAll(matchingResourceNames(draft.resources(), labelledValues));
        return Set.copyOf(result);
    }

    private Set<String> matchingResourceNames(
            List<TeamDocumentImportDraft.ResourceDraft> resources,
            List<String> labelledValues
    ) {
        if (labelledValues.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (TeamDocumentImportDraft.ResourceDraft resource : resources) {
            if (!isBusinessPartyCandidate(resource)) {
                continue;
            }
            String resourceName = clean(resource.sourceName());
            if (labelledValues.stream().anyMatch(value -> startsWithNormalizedName(value, resourceName))) {
                result.add(resourceName);
            }
        }
        return Set.copyOf(result);
    }

    private boolean isBusinessPartyCandidate(TeamDocumentImportDraft.ResourceDraft resource) {
        String sourceName = clean(resource.sourceName());
        if (!StringUtils.hasText(sourceName) || sourceName.codePointCount(0, sourceName.length()) < 4) {
            return false;
        }
        if ("ground_agent".equalsIgnoreCase(clean(resource.arrangementType()))) {
            return true;
        }
        return COMPANY_LIKE_SUFFIXES.stream().anyMatch(sourceName::endsWith);
    }

    private List<String> labelledValues(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        collectLabelValues(BUSINESS_PARTY_LABEL, value, result);
        collectLabelValues(EMBEDDED_BUSINESS_PARTY_LABEL, value, result);
        return List.copyOf(result);
    }

    private void collectLabelValues(Pattern pattern, String value, Set<String> target) {
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            String labelledValue = clean(matcher.group(1));
            if (StringUtils.hasText(labelledValue)) {
                target.add(labelledValue);
            }
        }
    }

    private boolean startsWithNormalizedName(String labelledValue, String resourceName) {
        String normalizedValue = normalize(labelledValue);
        String normalizedName = normalize(resourceName);
        return StringUtils.hasText(normalizedName) && normalizedValue.startsWith(normalizedName);
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。、“”‘’（）()【】\\[\\]-]+", "");
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
