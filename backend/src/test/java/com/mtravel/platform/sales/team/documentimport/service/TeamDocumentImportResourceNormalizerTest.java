package com.mtravel.platform.sales.team.documentimport.service;

import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 团队 Word 资源草稿归一化规则测试。 */
class TeamDocumentImportResourceNormalizerTest {
    private final TeamDocumentImportResourceNormalizer normalizer = new TeamDocumentImportResourceNormalizer();

    @Test
    void sameDayReferenceHotelsShouldKeepFirstConfirmedOne() {
        TeamDocumentImportDraft.ResourceDraft first = resource(1, "hotel", "苏州酒店A", "参考酒店", null, true);
        TeamDocumentImportDraft.ResourceDraft second = resource(1, "hotel", "苏州酒店B", "参考酒店", 12L, false);
        TeamDocumentImportDraft.ResourceDraft third = resource(1, "hotel", "苏州酒店C", "参考酒店", 13L, false);

        TeamDocumentImportDraft result = normalizer.normalize(draft(first, second, third));

        assertThat(result.resources()).containsExactly(second);
    }

    @Test
    void explicitHotelShouldRemoveAllReferenceHotelsOnSameDay() {
        TeamDocumentImportDraft.ResourceDraft referenceA = resource(2, "hotel", "南京参考A", "参考酒店", null, true);
        TeamDocumentImportDraft.ResourceDraft explicit = resource(2, "hotel", "南京已指定酒店", "特别升级一晚", 22L, false);
        TeamDocumentImportDraft.ResourceDraft referenceB = resource(2, "hotel", "南京参考B", "参考酒店", 23L, false);

        TeamDocumentImportDraft result = normalizer.normalize(draft(referenceA, explicit, referenceB));

        assertThat(result.resources()).containsExactly(explicit);
    }

    @Test
    void referenceHotelsOnDifferentDaysShouldEachKeepOne() {
        TeamDocumentImportDraft.ResourceDraft dayOne = resource(1, "hotel", "杭州参考", "参考酒店", null, true);
        TeamDocumentImportDraft.ResourceDraft dayTwo = resource(2, "hotel", "苏州参考A", "参考酒店", null, true);
        TeamDocumentImportDraft.ResourceDraft dayTwoSecond = resource(2, "hotel", "苏州参考B", "参考酒店", null, true);

        TeamDocumentImportDraft result = normalizer.normalize(draft(dayOne, dayTwo, dayTwoSecond));

        assertThat(result.resources()).containsExactly(dayOne, dayTwo);
    }

    @Test
    void unknownDayReferenceHotelsShouldNotBeMerged() {
        TeamDocumentImportDraft.ResourceDraft first = resource(null, "hotel", "未知住宿A", "参考酒店", null, true);
        TeamDocumentImportDraft.ResourceDraft second = resource(null, "hotel", "未知住宿B", "参考酒店", null, true);

        TeamDocumentImportDraft result = normalizer.normalize(draft(first, second));

        assertThat(result.resources()).containsExactly(first, second);
    }

    @Test
    void excludedBusinessPartyShouldBeRemovedByExactNormalizedName() {
        TeamDocumentImportDraft.ResourceDraft party = resource(1, "ground_agent", "杭州百缘", "ATTN：杭州百缘 叶菊莲", null, true);
        TeamDocumentImportDraft.ResourceDraft scenic = resource(1, "scenic", "西湖", null, null, true);

        TeamDocumentImportDraft result = normalizer.normalize(draft(party, scenic), Set.of(" 杭州百缘 "));

        assertThat(result.resources()).containsExactly(scenic);
    }

    @Test
    void excludedBusinessPartyNameShouldNotRemoveNonGroundAgentResource() {
        TeamDocumentImportDraft.ResourceDraft scenic = resource(1, "scenic", "杭州百缘", null, null, true);

        TeamDocumentImportDraft result = normalizer.normalize(draft(scenic), Set.of("杭州百缘"));

        assertThat(result.resources()).containsExactly(scenic);
    }

    @Test
    void ordinaryContactNameIsNotRemovedUnlessExplicitlyExcluded() {
        TeamDocumentImportDraft.ResourceDraft resource = resource(1, "scenic", "叶菊莲", null, null, true);

        TeamDocumentImportDraft result = normalizer.normalize(draft(resource), Set.of());

        assertThat(result.resources()).containsExactly(resource);
    }

    @Test
    void boatSightseeingTrafficShouldBeCorrectedToScenic() {
        TeamDocumentImportDraft.ResourceDraft boat = resource(1, "traffic", "船游浦江", null, 55L, false);

        TeamDocumentImportDraft result = normalizer.normalize(draft(boat));

        assertThat(result.resources().getFirst().arrangementType()).isEqualTo("scenic");
        assertThat(result.resources().getFirst().sourceName()).isEqualTo("船游浦江");
        assertThat(result.resources().getFirst().selectedResourceId()).isNull();
        assertThat(result.resources().getFirst().requiresConfirmation()).isTrue();
    }

    @Test
    void boatSightseeingRemarkShouldAlsoCorrectTrafficToScenic() {
        TeamDocumentImportDraft.ResourceDraft boat = resource(1, "traffic", "浦江", "夜游观光船", null, true);

        TeamDocumentImportDraft result = normalizer.normalize(draft(boat));

        assertThat(result.resources().getFirst().arrangementType()).isEqualTo("scenic");
    }

    @Test
    void ordinaryTrafficShouldBeRemovedFromDocumentImport() {
        TeamDocumentImportDraft.ResourceDraft flight = resource(1, "traffic", "上海虹桥-杭州萧山航班", null, null, true);

        TeamDocumentImportDraft result = normalizer.normalize(draft(flight));

        assertThat(result.resources()).isEmpty();
    }

    private TeamDocumentImportDraft draft(TeamDocumentImportDraft.ResourceDraft... resources) {
        return new TeamDocumentImportDraft(
                "ground_confirmation", 0.9, null, null, List.of(), List.of(), List.of(resources), List.of(), List.of()
        );
    }

    private TeamDocumentImportDraft.ResourceDraft resource(
            Integer dayNo,
            String arrangementType,
            String sourceName,
            String remark,
            Long selectedResourceId,
            boolean requiresConfirmation
    ) {
        return new TeamDocumentImportDraft.ResourceDraft(
                "resource:" + sourceName, dayNo, null, arrangementType, sourceName, "杭州", remark,
                selectedResourceId, selectedResourceId == null ? null : sourceName,
                null, null, requiresConfirmation, List.of()
        );
    }
}
