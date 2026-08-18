package com.mtravel.platform.sales.team.documentimport.service;

import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 团队 Word 代录资源草稿的业务归一化组件。
 *
 * <p>AI 识别结果只能作为候选，正式匹配和写入前都需要执行稳定的业务规则：同一天的参考酒店
 * 不能被当成多家正式住宿安排，业务往来方不能混入资源候选，游船类游览项目不能误归为大交通；
 * 城际交通不进入本期计调资源代录。
 * 本组件不访问数据库，也不修改资源主档。</p>
 */
@Service
public class TeamDocumentImportResourceNormalizer {
    private static final String HOTEL = "hotel";
    private static final String TRAFFIC = "traffic";
    private static final String SCENIC = "scenic";
    private static final String REFERENCE_HOTEL = "参考酒店";
    private static final List<String> BOAT_SIGHTSEEING_TERMS = List.of(
            "船游", "游船", "夜游", "观光船", "画舫", "环湖游船"
    );

    /**
     * 归一化资源草稿，不额外排除业务方名称。
     *
     * @param draft 待处理的资源草稿
     * @return 归一化后的草稿；传入 {@code null} 时返回 {@code null}
     */
    public TeamDocumentImportDraft normalize(TeamDocumentImportDraft draft) {
        return normalize(draft, Set.of());
    }

    /**
     * 归一化资源草稿并移除明确列出的业务往来方候选。
     *
     * <p>调用方应只传入已确认的公司/业务单位名称，例如由文档的 FROM、TO、ATTN 语义解析得到的
     * 名称；普通联系人姓名不能放入排除集合，否则可能误删同名景区或酒店。</p>
     *
     * @param draft 待处理的资源草稿
     * @param excludedNames 需要从资源候选中排除的公司或业务单位名称，可传 {@code null}
     * @return 归一化后的草稿
     */
    public TeamDocumentImportDraft normalize(
            TeamDocumentImportDraft draft,
            Set<String> excludedNames
    ) {
        if (draft == null || draft.resources() == null || draft.resources().isEmpty()) {
            return draft;
        }

        Set<String> normalizedExcludedNames = normalizeExcludedNames(excludedNames);
        List<TeamDocumentImportDraft.ResourceDraft> classified = draft.resources().stream()
                .filter(resource -> resource != null && !isExcludedBusinessParty(resource, normalizedExcludedNames))
                .map(this::correctArrangementType)
                // 当前计调代录只录需要采购或安排的地接资源；航班、火车、城市间转移不生成团队安排。
                .filter(resource -> !TRAFFIC.equalsIgnoreCase(trimToEmpty(resource.arrangementType())))
                .toList();
        List<TeamDocumentImportDraft.ResourceDraft> deduplicated = collapseReferenceHotels(classified);
        if (sameResources(draft.resources(), deduplicated)) {
            return draft;
        }
        return new TeamDocumentImportDraft(
                draft.documentType(), draft.confidence(), draft.team(), draft.order(), draft.guests(),
                draft.itineraryDays(), deduplicated, draft.warnings(), draft.evidence(), draft.productDescription()
        );
    }

    /** 将游船/夜游等观光项目从大交通校正为景区游览；其余大交通会在后续过滤。 */
    private TeamDocumentImportDraft.ResourceDraft correctArrangementType(
            TeamDocumentImportDraft.ResourceDraft resource
    ) {
        if (!TRAFFIC.equalsIgnoreCase(trimToEmpty(resource.arrangementType()))) {
            return resource;
        }
        String searchableText = trimToEmpty(resource.sourceName()) + " " + trimToEmpty(resource.remark());
        String normalizedText = searchableText.toLowerCase(Locale.ROOT);
        boolean boatSightseeing = BOAT_SIGHTSEEING_TERMS.stream()
                .map(term -> term.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedText::contains);
        if (!boatSightseeing) {
            return resource;
        }
        // 分类已从大交通变为景区，旧交通资源候选不能继续作为景区安排写入。
        return new TeamDocumentImportDraft.ResourceDraft(
                resource.itemKey(), resource.dayNo(), resource.time(), SCENIC, resource.sourceName(), resource.city(),
                resource.remark(), null, null, null, null, true, List.of()
        );
    }

    /**
     * 同一天的参考酒店只保留一条；明确酒店存在时删除当天全部参考酒店。
     *
     * <p>使用原始列表顺序作为稳定排序依据，并只按有明确日次的资源合并；日次为空的参考酒店
     * 不与任何其它记录合并，避免把不同未知日期错误合并到一起。</p>
     */
    private List<TeamDocumentImportDraft.ResourceDraft> collapseReferenceHotels(
            List<TeamDocumentImportDraft.ResourceDraft> resources
    ) {
        Set<Integer> daysWithExplicitHotel = resources.stream()
                .filter(this::isHotel)
                .filter(resource -> !isReferenceHotel(resource))
                .map(TeamDocumentImportDraft.ResourceDraft::dayNo)
                .filter(dayNo -> dayNo != null)
                .collect(Collectors.toSet());

        Set<Integer> retainedReferenceDays = new HashSet<>();
        List<TeamDocumentImportDraft.ResourceDraft> result = new ArrayList<>();
        for (TeamDocumentImportDraft.ResourceDraft resource : resources) {
            if (!isReferenceHotel(resource) || resource.dayNo() == null) {
                result.add(resource);
                continue;
            }
            Integer dayNo = resource.dayNo();
            if (daysWithExplicitHotel.contains(dayNo)) {
                continue;
            }
            if (!retainedReferenceDays.contains(dayNo)) {
                result.add(resource);
                retainedReferenceDays.add(dayNo);
                continue;
            }
            // 当前已保留的是原顺序第一条；若后续条目是唯一已匹配资源，则用它替换第一条。
            int retainedIndex = findReferenceHotelIndex(result, dayNo);
            if (retainedIndex >= 0 && isConfirmed(resource) && !isConfirmed(result.get(retainedIndex))) {
                result.set(retainedIndex, resource);
            }
        }
        return List.copyOf(result);
    }

    private int findReferenceHotelIndex(
            List<TeamDocumentImportDraft.ResourceDraft> resources,
            Integer dayNo
    ) {
        for (int index = 0; index < resources.size(); index++) {
            TeamDocumentImportDraft.ResourceDraft resource = resources.get(index);
            if (isReferenceHotel(resource) && dayNo.equals(resource.dayNo())) {
                return index;
            }
        }
        return -1;
    }

    private boolean isHotel(TeamDocumentImportDraft.ResourceDraft resource) {
        return resource != null && HOTEL.equalsIgnoreCase(trimToEmpty(resource.arrangementType()));
    }

    private boolean isReferenceHotel(TeamDocumentImportDraft.ResourceDraft resource) {
        return isHotel(resource) && trimToEmpty(resource.remark()).contains(REFERENCE_HOTEL);
    }

    private boolean isConfirmed(TeamDocumentImportDraft.ResourceDraft resource) {
        return resource.selectedResourceId() != null && !resource.requiresConfirmation();
    }

    private boolean isExcludedBusinessParty(
            TeamDocumentImportDraft.ResourceDraft resource,
            Set<String> normalizedExcludedNames
    ) {
        // 只处理被错误识别为地接服务的业务主体；同名景区、酒店等不能仅因名称相同被删除。
        return "ground_agent".equalsIgnoreCase(trimToEmpty(resource.arrangementType()))
                && normalizedExcludedNames.contains(normalizeName(resource.sourceName()));
    }

    private Set<String> normalizeExcludedNames(Set<String> excludedNames) {
        if (excludedNames == null || excludedNames.isEmpty()) {
            return Set.of();
        }
        return excludedNames.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 与资源名称候选一致的轻量规范化，仅用于精确排除业务方名称。 */
    private String normalizeName(String value) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。、“”‘’（）()【】\\[\\]-]+", "");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean sameResources(
            List<TeamDocumentImportDraft.ResourceDraft> original,
            List<TeamDocumentImportDraft.ResourceDraft> normalized
    ) {
        return original.size() == normalized.size()
                && original.equals(normalized);
    }
}
