package com.mtravel.platform.sales.team.documentimport.service;

import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.alias.entity.PurchaseResourceAliasEntity;
import com.mtravel.platform.purchase.resource.alias.mapper.PurchaseResourceAliasMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 团队 Word 资源候选匹配测试，避免资源别名跨城市自动选中。 */
class TeamDocumentImportResourceMatcherTest {

    @Test
    void aliasCandidateShouldRespectDocumentCity() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceAliasMapper aliasMapper = mock(PurchaseResourceAliasMapper.class);
        TeamDocumentImportResourceMatcher matcher = new TeamDocumentImportResourceMatcher(
                resourceMapper, mock(PurchaseRelationMapper.class), aliasMapper, mock(SupplierMapper.class)
        );
        PurchaseResourceAliasEntity alias = new PurchaseResourceAliasEntity();
        alias.setResourceId(11L);
        PurchaseResourceEntity otherCityResource = resource(11L, "灵隐寺", "上海");
        when(resourceMapper.selectList(any())).thenReturn(List.of(), List.of(otherCityResource));
        when(aliasMapper.selectList(any())).thenReturn(List.of(alias));

        TeamDocumentImportDraft.ResourceDraft result = matcher.match(draft("杭州"), 1L).resources().getFirst();

        assertThat(result.candidates()).isEmpty();
        assertThat(result.selectedResourceId()).isNull();
        assertThat(result.requiresConfirmation()).isTrue();
    }

    @Test
    void uniqueAliasInSameCityShouldSelectResourceAndDefaultSupplier() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        PurchaseResourceAliasMapper aliasMapper = mock(PurchaseResourceAliasMapper.class);
        PurchaseRelationMapper relationMapper = mock(PurchaseRelationMapper.class);
        SupplierMapper supplierMapper = mock(SupplierMapper.class);
        TeamDocumentImportResourceMatcher matcher = new TeamDocumentImportResourceMatcher(
                resourceMapper, relationMapper, aliasMapper, supplierMapper
        );
        PurchaseResourceAliasEntity alias = new PurchaseResourceAliasEntity();
        alias.setResourceId(11L);
        PurchaseResourceEntity resource = resource(11L, "杭州灵隐寺景区", "杭州");
        PurchaseRelationEntity relation = new PurchaseRelationEntity();
        relation.setSupplierId(21L);
        relation.setIsDefault(true);
        SupplierEntity supplier = new SupplierEntity();
        supplier.setId(21L);
        supplier.setSupplierName("灵隐寺票务供应商");
        when(resourceMapper.selectList(any())).thenReturn(List.of(), List.of(resource));
        when(aliasMapper.selectList(any())).thenReturn(List.of(alias));
        when(relationMapper.selectOne(any())).thenReturn(relation);
        when(supplierMapper.selectOne(any())).thenReturn(supplier);

        TeamDocumentImportDraft.ResourceDraft result = matcher.match(draft("杭州"), 1L).resources().getFirst();

        assertThat(result.selectedResourceId()).isEqualTo(11L);
        assertThat(result.selectedSupplierId()).isEqualTo(21L);
        assertThat(result.selectedSupplierName()).isEqualTo("灵隐寺票务供应商");
        assertThat(result.time()).isEqualTo("08:30");
        assertThat(result.requiresConfirmation()).isFalse();
    }

    @Test
    void cityOnlyHotelShouldOfferSameCityCandidatesWithoutAutoSelection() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        TeamDocumentImportResourceMatcher matcher = new TeamDocumentImportResourceMatcher(
                resourceMapper, mock(PurchaseRelationMapper.class), mock(PurchaseResourceAliasMapper.class), mock(SupplierMapper.class)
        );
        PurchaseResourceEntity first = resource(31L, "兰欧国际酒店", "南京市");
        first.setResourceType("hotel");
        PurchaseResourceEntity second = resource(32L, "锦江都城酒店", "南京市");
        second.setResourceType("hotel");
        when(resourceMapper.selectList(any())).thenReturn(List.of(), List.of(first, second));

        TeamDocumentImportDraft.ResourceDraft input = new TeamDocumentImportDraft.ResourceDraft(
                "resource:hotel", 1, null, "hotel", "南京", "南京", null,
                null, null, null, null, true, List.of()
        );
        TeamDocumentImportDraft.ResourceDraft result = matcher.match(new TeamDocumentImportDraft(
                "ground_confirmation", 0.8, null, null, List.of(), List.of(), List.of(input), List.of(), List.of()
        ), 1L).resources().getFirst();

        assertThat(result.candidates()).hasSize(2);
        assertThat(result.selectedResourceId()).isNull();
        assertThat(result.requiresConfirmation()).isTrue();
    }

    @Test
    void notRequiredResourceShouldNotEnterProcurementCandidates() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        TeamDocumentImportResourceMatcher matcher = new TeamDocumentImportResourceMatcher(
                resourceMapper, mock(PurchaseRelationMapper.class), mock(PurchaseResourceAliasMapper.class), mock(SupplierMapper.class)
        );
        PurchaseResourceEntity freeScenic = resource(51L, "西湖", "杭州");
        freeScenic.setProcurementMode("not_required");
        when(resourceMapper.selectList(any())).thenReturn(List.of(freeScenic), List.of(freeScenic));

        TeamDocumentImportDraft result = matcher.match(draft("杭州"), 1L);

        assertThat(result.resources()).isEmpty();
    }

    @Test
    void uniqueVehicleSeatAndTypeCandidateShouldBeSelected() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        TeamDocumentImportResourceMatcher matcher = new TeamDocumentImportResourceMatcher(
                resourceMapper, mock(PurchaseRelationMapper.class), mock(PurchaseResourceAliasMapper.class), mock(SupplierMapper.class)
        );
        PurchaseResourceEntity vehicle = resource(61L, "苏州33座旅游大巴", "苏州市");
        vehicle.setResourceType("vehicle");
        vehicle.setVehicleType("旅游大巴");
        vehicle.setSeatCount(33);
        when(resourceMapper.selectList(any())).thenReturn(List.of(), List.of(vehicle));
        TeamDocumentImportDraft.ResourceDraft input = new TeamDocumentImportDraft.ResourceDraft(
                "resource:vehicle", 1, null, "vehicle", "33座旅游大巴", "未标注城市", null,
                null, null, null, null, true, List.of()
        );

        TeamDocumentImportDraft.ResourceDraft result = matcher.match(new TeamDocumentImportDraft(
                "ground_confirmation", 0.8, null, null, List.of(), List.of(), List.of(input), List.of(), List.of()
        ), 1L).resources().getFirst();

        assertThat(result.selectedResourceId()).isEqualTo(61L);
        assertThat(result.selectedResourceName()).isEqualTo("苏州33座旅游大巴");
        assertThat(result.requiresConfirmation()).isFalse();
        assertThat(result.candidates()).singleElement().extracting(TeamDocumentImportDraft.ResourceCandidate::exactMatch)
                .isEqualTo(true);
    }

    @Test
    void multipleVehicleSeatCandidatesShouldRemainForManualSelection() {
        PurchaseResourceMapper resourceMapper = mock(PurchaseResourceMapper.class);
        TeamDocumentImportResourceMatcher matcher = new TeamDocumentImportResourceMatcher(
                resourceMapper, mock(PurchaseRelationMapper.class), mock(PurchaseResourceAliasMapper.class), mock(SupplierMapper.class)
        );
        PurchaseResourceEntity first = resource(71L, "苏州33座旅游大巴A", null);
        first.setResourceType("vehicle");
        first.setSeatCount(33);
        PurchaseResourceEntity second = resource(72L, "苏州33座旅游大巴B", null);
        second.setResourceType("vehicle");
        second.setSeatCount(33);
        when(resourceMapper.selectList(any())).thenReturn(List.of(), List.of(first, second));
        TeamDocumentImportDraft.ResourceDraft input = new TeamDocumentImportDraft.ResourceDraft(
                "resource:vehicle", 1, null, "vehicle", "33座旅游大巴", null, null,
                null, null, null, null, true, List.of()
        );

        TeamDocumentImportDraft.ResourceDraft result = matcher.match(new TeamDocumentImportDraft(
                "ground_confirmation", 0.8, null, null, List.of(), List.of(), List.of(input), List.of(), List.of()
        ), 1L).resources().getFirst();

        assertThat(result.candidates()).hasSize(2);
        assertThat(result.selectedResourceId()).isNull();
        assertThat(result.requiresConfirmation()).isTrue();
    }

    private TeamDocumentImportDraft draft(String city) {
        return new TeamDocumentImportDraft(
                "ground_confirmation", 0.8, null, null, List.of(), List.of(),
                List.of(new TeamDocumentImportDraft.ResourceDraft(
                        "resource:1", 1, "08:30", "scenic", "灵隐寺", city, null,
                        null, null, null, null, true, List.of()
                )),
                List.of(), List.of()
        );
    }

    private PurchaseResourceEntity resource(Long id, String name, String city) {
        PurchaseResourceEntity resource = new PurchaseResourceEntity();
        resource.setId(id);
        resource.setResourceName(name);
        resource.setResourceType("scenic");
        resource.setCity(city);
        return resource;
    }
}
