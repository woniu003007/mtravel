package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 团队 Word 资源草稿的统一安全整理入口。
 *
 * <p>除纯文本业务归一化外，本服务会按资源主档采购属性清除无需采购资源。它被新识别、历史草稿读取、
 * 草稿保存和正式应用共同调用，避免仅靠前端隐藏字段造成采购安排误写入。</p>
 */
@Service
public class TeamDocumentImportResourceDraftSanitizer {
    private final TeamDocumentImportResourceNormalizer resourceNormalizer;
    private final TeamDocumentImportBusinessPartyNameExtractor businessPartyNameExtractor;
    private final PurchaseResourceMapper resourceMapper;

    public TeamDocumentImportResourceDraftSanitizer(
            TeamDocumentImportResourceNormalizer resourceNormalizer,
            TeamDocumentImportBusinessPartyNameExtractor businessPartyNameExtractor,
            PurchaseResourceMapper resourceMapper
    ) {
        this.resourceNormalizer = resourceNormalizer;
        this.businessPartyNameExtractor = businessPartyNameExtractor;
        this.resourceMapper = resourceMapper;
    }

    /** 规范化历史草稿、页面保存草稿和正式应用前草稿。 */
    public TeamDocumentImportDraft sanitize(TeamDocumentImportDraft draft, Long tenantId) {
        return sanitize(draft, tenantId, null);
    }

    /** 规范化新识别草稿，并将 Word 正文中的明确业务往来方排除在资源候选之外。 */
    public TeamDocumentImportDraft sanitize(TeamDocumentImportDraft draft, Long tenantId, String sourceText) {
        if (draft == null) {
            return null;
        }
        Set<String> businessPartyNames = sourceText == null
                ? businessPartyNameExtractor.fromDraft(draft)
                : businessPartyNameExtractor.fromDraftAndSourceText(draft, sourceText);
        TeamDocumentImportDraft normalized = resourceNormalizer.normalize(draft, businessPartyNames);
        return removeNotRequiredResources(normalized, tenantId);
    }

    /**
     * 已选择或仅候选到“无需采购”的资源不能进入采购安排。查询按 ID 批量执行，避免资源行 N+1 查询。
     */
    private TeamDocumentImportDraft removeNotRequiredResources(TeamDocumentImportDraft draft, Long tenantId) {
        if (draft == null || draft.resources() == null || draft.resources().isEmpty() || tenantId == null) {
            return draft;
        }
        Set<Long> resourceIds = draft.resources().stream()
                .flatMap(resource -> referencedResourceIds(resource).stream())
                .collect(Collectors.toSet());
        if (resourceIds.isEmpty()) {
            return draft;
        }
        List<PurchaseResourceEntity> resources = resourceMapper.selectList(new QueryWrapper<PurchaseResourceEntity>()
                .select("id", "procurement_mode")
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .in("id", resourceIds));
        Set<Long> notRequiredIds = safe(resources).stream()
                .filter(resource -> "not_required".equalsIgnoreCase(resource.getProcurementMode()))
                .map(PurchaseResourceEntity::getId)
                .collect(Collectors.toSet());
        if (notRequiredIds.isEmpty()) {
            return draft;
        }
        List<TeamDocumentImportDraft.ResourceDraft> filtered = new ArrayList<>();
        for (TeamDocumentImportDraft.ResourceDraft resource : draft.resources()) {
            TeamDocumentImportDraft.ResourceDraft retained = removeNotRequiredCandidates(resource, notRequiredIds);
            if (retained != null) {
                filtered.add(retained);
            }
        }
        if (filtered.equals(draft.resources())) {
            return draft;
        }
        return new TeamDocumentImportDraft(
                draft.documentType(), draft.confidence(), draft.team(), draft.order(), draft.guests(),
                draft.itineraryDays(), List.copyOf(filtered), draft.warnings(), draft.evidence(), draft.productDescription()
        );
    }

    private Set<Long> referencedResourceIds(TeamDocumentImportDraft.ResourceDraft resource) {
        if (resource == null) {
            return Set.of();
        }
        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        if (resource.selectedResourceId() != null) {
            ids.add(resource.selectedResourceId());
        }
        safe(resource.candidates()).stream()
                .map(TeamDocumentImportDraft.ResourceCandidate::resourceId)
                .filter(java.util.Objects::nonNull)
                .forEach(ids::add);
        return Set.copyOf(ids);
    }

    private TeamDocumentImportDraft.ResourceDraft removeNotRequiredCandidates(
            TeamDocumentImportDraft.ResourceDraft resource,
            Set<Long> notRequiredIds
    ) {
        if (resource == null || notRequiredIds.contains(resource.selectedResourceId())) {
            return null;
        }
        List<TeamDocumentImportDraft.ResourceCandidate> originalCandidates = safe(resource.candidates());
        List<TeamDocumentImportDraft.ResourceCandidate> candidates = originalCandidates.stream()
                .filter(candidate -> candidate.resourceId() == null || !notRequiredIds.contains(candidate.resourceId()))
                .toList();
        // 候选原本都来自无需采购主档时，这一行也不应继续让计调选择供应商。
        if (resource.selectedResourceId() == null && !originalCandidates.isEmpty() && candidates.isEmpty()) {
            return null;
        }
        if (candidates.equals(originalCandidates)) {
            return resource;
        }
        return new TeamDocumentImportDraft.ResourceDraft(
                resource.itemKey(), resource.dayNo(), resource.time(), resource.arrangementType(), resource.sourceName(), resource.city(),
                resource.remark(), resource.selectedResourceId(), resource.selectedResourceName(), resource.selectedSupplierId(),
                resource.selectedSupplierName(), resource.requiresConfirmation(), candidates
        );
    }

    private <T> List<T> safe(Collection<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
