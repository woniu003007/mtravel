package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 团队 Word 草稿统一整理测试，覆盖历史免费资源和业务方误识别。 */
class TeamDocumentImportResourceDraftSanitizerTest {

    @Test
    void legacySelectedNotRequiredResourceShouldBeRemovedWithoutRematching() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceEntity freeScenic = resource(88L, "not_required");
        when(resourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(freeScenic));
        TeamDocumentImportResourceDraftSanitizer sanitizer = sanitizer(resourceMapper);

        TeamDocumentImportDraft result = sanitizer.sanitize(draft(selectedResource(88L)), 1L);

        assertThat(result.resources()).isEmpty();
        verify(resourceMapper).selectList(any(Wrapper.class));
    }

    @Test
    void onlyNotRequiredCandidatesShouldBeRemovedFromProcurementList() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceEntity freeScenic = resource(88L, "not_required");
        when(resourceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(freeScenic));
        TeamDocumentImportResourceDraftSanitizer sanitizer = sanitizer(resourceMapper);
        TeamDocumentImportDraft.ResourceDraft candidateOnly = new TeamDocumentImportDraft.ResourceDraft(
                "resource:free", 1, null, "scenic", "西湖", "杭州", null,
                null, null, null, null, true,
                List.of(new TeamDocumentImportDraft.ResourceCandidate(
                        88L, "西湖", "scenic", "杭州市", null, null, false, true
                ))
        );

        TeamDocumentImportDraft result = sanitizer.sanitize(draft(candidateOnly), 1L);

        assertThat(result.resources()).isEmpty();
    }

    @Test
    void persistedAttnBusinessPartyShouldNotRemainAsGroundAgent() {
        TeamDocumentImportDraft.ResourceDraft businessParty = new TeamDocumentImportDraft.ResourceDraft(
                "resource:party", 1, null, "ground_agent", "杭州百缘", "杭州", "ATTN：杭州百缘 叶菊莲",
                null, null, null, null, true, List.of()
        );

        TeamDocumentImportDraft result = sanitizer(mock(PurchaseResourceMapper.class)).sanitize(draft(businessParty), 1L);

        assertThat(result.resources()).isEmpty();
    }

    @Test
    void wordSourceTextShouldExcludeExplicitAttnCompanyFromGroundAgentCandidates() {
        TeamDocumentImportDraft.ResourceDraft businessParty = new TeamDocumentImportDraft.ResourceDraft(
                "resource:party", 1, null, "ground_agent", "杭州百缘", "杭州", null,
                null, null, null, null, true, List.of()
        );

        TeamDocumentImportDraft result = sanitizer(mock(PurchaseResourceMapper.class)).sanitize(
                draft(businessParty), 1L, "FROM：东晟假期 刘小川\nATTN：杭州百缘 叶菊莲"
        );

        assertThat(result.resources()).isEmpty();
    }

    @Test
    void contactNameShouldNotBeTreatedAsBusinessPartyName() {
        TeamDocumentImportDraft.ResourceDraft contact = new TeamDocumentImportDraft.ResourceDraft(
                "resource:contact", 1, null, "ground_agent", "叶菊莲", "杭州", "ATTN：杭州百缘 叶菊莲",
                null, null, null, null, true, List.of()
        );

        TeamDocumentImportDraft result = sanitizer(mock(PurchaseResourceMapper.class)).sanitize(draft(contact), 1L);

        assertThat(result.resources()).containsExactly(contact);
    }

    private TeamDocumentImportResourceDraftSanitizer sanitizer(PurchaseResourceMapper resourceMapper) {
        return new TeamDocumentImportResourceDraftSanitizer(
                new TeamDocumentImportResourceNormalizer(),
                new TeamDocumentImportBusinessPartyNameExtractor(),
                resourceMapper
        );
    }

    private TeamDocumentImportDraft draft(TeamDocumentImportDraft.ResourceDraft resource) {
        return new TeamDocumentImportDraft(
                "ground_confirmation", 0.9, null, null, List.of(), List.of(), List.of(resource), List.of(), List.of()
        );
    }

    private TeamDocumentImportDraft.ResourceDraft selectedResource(Long resourceId) {
        return new TeamDocumentImportDraft.ResourceDraft(
                "resource:selected", 1, null, "scenic", "西湖", "杭州", null,
                resourceId, "西湖", null, null, false, List.of()
        );
    }

    private PurchaseResourceEntity resource(Long id, String procurementMode) {
        PurchaseResourceEntity entity = new PurchaseResourceEntity();
        entity.setId(id);
        entity.setProcurementMode(procurementMode);
        return entity;
    }
}
