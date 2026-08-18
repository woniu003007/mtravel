package com.mtravel.platform.sales.team.documentimport.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtravel.platform.purchase.relation.entity.PurchaseRelationEntity;
import com.mtravel.platform.purchase.relation.mapper.PurchaseRelationMapper;
import com.mtravel.platform.purchase.resource.alias.entity.PurchaseResourceAliasEntity;
import com.mtravel.platform.purchase.resource.alias.mapper.PurchaseResourceAliasMapper;
import com.mtravel.platform.purchase.resource.entity.PurchaseResourceEntity;
import com.mtravel.platform.purchase.resource.mapper.PurchaseResourceMapper;
import com.mtravel.platform.purchase.supplier.entity.SupplierEntity;
import com.mtravel.platform.purchase.supplier.mapper.SupplierMapper;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 根据正式资源主档、资源别名和采购关系为文档中的资源生成候选。 */
@Service
public class TeamDocumentImportResourceMatcher {
    private static final Pattern VEHICLE_SEAT_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,3})\\s*座");
    private final PurchaseResourceMapper resourceMapper;
    private final PurchaseRelationMapper relationMapper;
    private final PurchaseResourceAliasMapper aliasMapper;
    private final SupplierMapper supplierMapper;

    public TeamDocumentImportResourceMatcher(
            PurchaseResourceMapper resourceMapper,
            PurchaseRelationMapper relationMapper,
            PurchaseResourceAliasMapper aliasMapper,
            SupplierMapper supplierMapper
    ) {
        this.resourceMapper = resourceMapper;
        this.relationMapper = relationMapper;
        this.aliasMapper = aliasMapper;
        this.supplierMapper = supplierMapper;
    }

    /** 匹配草稿中的每个资源；唯一正式名或别名命中才自动选中。 */
    public TeamDocumentImportDraft match(TeamDocumentImportDraft draft, Long tenantId) {
        List<TeamDocumentImportDraft.ResourceDraft> matched = draft.resources() == null
                ? List.of()
                : draft.resources().stream().map(item -> matchOne(item, tenantId))
                // 无需采购的资源仍可留在行程草稿，但不能出现在采购资源候选列表。
                .filter(Objects::nonNull).toList();
        return new TeamDocumentImportDraft(
                draft.documentType(), draft.confidence(), draft.team(), draft.order(), draft.guests(),
                draft.itineraryDays(), matched, draft.warnings(), draft.evidence(), draft.productDescription()
        );
    }

    private TeamDocumentImportDraft.ResourceDraft matchOne(
            TeamDocumentImportDraft.ResourceDraft draft,
            Long tenantId
    ) {
        String normalized = normalize(draft.sourceName());
        String resourceType = resourceType(draft.arrangementType());
        QueryWrapper<PurchaseResourceEntity> resourceQuery = new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("resource_type", resourceType);
        // 城市仅作名称候选的收窄条件，不能用“名称或城市”，否则会把同城所有资源都作为候选。
        if (StringUtils.hasText(draft.city())) {
            // 文档模型通常返回“杭州”，资源主档所在地通常保存为“杭州市”；先按名称收窄，
            // 再在 Java 中用标准化城市名过滤，避免同城后缀差异导致已有资源全部落成未匹配。
            resourceQuery.like("resource_name", draft.sourceName());
        } else {
            resourceQuery.like("resource_name", draft.sourceName());
        }
        resourceQuery.orderByAsc("resource_name").last("limit 50");
        List<PurchaseResourceEntity> resources = new ArrayList<>(resourceMapper.selectList(resourceQuery));
        if (requiresCityMatch(resourceType) && hasKnownCity(draft.city())) {
            resources = resources.stream()
                    .filter(resource -> sameCity(draft.city(), resource.getCity()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        boolean excludedNonPurchasable = resources.stream().anyMatch(this::isNotRequired);
        resources = resources.stream().filter(this::isPurchasable).collect(Collectors.toCollection(ArrayList::new));
        Map<Long, PurchaseResourceEntity> byId = resources.stream()
                .collect(Collectors.toMap(PurchaseResourceEntity::getId, Function.identity(), (left, right) -> left));
        List<PurchaseResourceAliasEntity> aliases = aliasMapper.selectList(new QueryWrapper<PurchaseResourceAliasEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("normalized_alias", normalized));
        List<Long> aliasResourceIds = aliases.stream().map(PurchaseResourceAliasEntity::getResourceId).distinct().toList();
        if (!aliasResourceIds.isEmpty()) {
            List<PurchaseResourceEntity> aliasResourcePool = resourceMapper.selectList(new QueryWrapper<PurchaseResourceEntity>()
                    .eq("tenant_id", tenantId).eq("is_deleted", false).eq("status", "active")
                    .eq("resource_type", resourceType).in("id", aliasResourceIds));
            List<PurchaseResourceEntity> aliasResources = aliasResourcePool.stream()
                    // 别名和正式名称都必须遵守文档已识别城市，避免跨城市同名资源被自动选中。
                    .filter(resource -> !requiresCityMatch(resourceType) || sameCity(draft.city(), resource.getCity()))
                    .filter(resource -> !byId.containsKey(resource.getId()))
                    .toList();
            excludedNonPurchasable = excludedNonPurchasable
                    || aliasResources.stream().anyMatch(this::isNotRequired);
            aliasResources = aliasResources.stream().filter(this::isPurchasable).toList();
            resources.addAll(0, aliasResources);
        }
        if (requiresCityMatch(resourceType) && hasKnownCity(draft.city())) {
            resources = resources.stream()
                    .filter(resource -> sameCity(draft.city(), resource.getCity()))
                    .toList();
        }
        if ("vehicle".equals(resourceType)) {
            // Word 常写“33座旅游大巴”，资源名称未必逐字相同，补充按座位数/车型的唯一候选。
            List<PurchaseResourceEntity> vehicleResources = vehicleIdentityCandidates(draft, tenantId);
            resources = mergeDistinct(resources, vehicleResources);
        }
        if (resources.isEmpty()) {
            resources = fuzzyNameCandidates(draft, tenantId, resourceType);
        }
        if (resources.isEmpty() && excludedNonPurchasable) {
            // 该条只命中了无需采购资源：从采购候选列表剔除，行程草稿由上层继续保留。
            return null;
        }
        List<PurchaseResourceEntity> matchedResources = resources.stream().distinct().limit(10).toList();

        List<TeamDocumentImportDraft.ResourceCandidate> candidates = new ArrayList<>();
        Set<Long> exactNameIds = resources.stream()
                .filter(resource -> normalize(resource.getResourceName()).equals(normalized)
                        || aliases.stream().anyMatch(alias -> alias.getResourceId().equals(resource.getId())))
                .map(PurchaseResourceEntity::getId).collect(Collectors.toSet());
        Set<Long> identityIds = "vehicle".equals(resourceType)
                ? resources.stream().filter(resource -> vehicleIdentityMatches(draft, resource))
                .map(PurchaseResourceEntity::getId).collect(Collectors.toSet()) : Set.of();
        for (PurchaseResourceEntity resource : matchedResources) {
            boolean exact = exactNameIds.contains(resource.getId()) || identityIds.contains(resource.getId());
            SupplierCandidate supplier = defaultSupplier(tenantId, resource.getId());
            candidates.add(new TeamDocumentImportDraft.ResourceCandidate(
                    resource.getId(), resource.getResourceName(), resource.getResourceType(), resource.getCity(),
                    supplier == null ? null : supplier.id(), supplier == null ? null : supplier.name(),
                    supplier != null && supplier.defaultSupplier(), exact
            ));
        }
        List<TeamDocumentImportDraft.ResourceCandidate> exactNameCandidates = candidates.stream()
                .filter(item -> exactNameIds.contains(item.resourceId())).toList();
        List<TeamDocumentImportDraft.ResourceCandidate> identityCandidates = candidates.stream()
                .filter(item -> identityIds.contains(item.resourceId())).toList();
        // 正式名称/别名命中优先；没有名称命中时，车型或座位数唯一命中才自动选择。
        TeamDocumentImportDraft.ResourceCandidate selected = exactNameCandidates.size() == 1
                ? exactNameCandidates.get(0)
                : exactNameCandidates.isEmpty() && identityCandidates.size() == 1
                ? identityCandidates.get(0) : null;
        return new TeamDocumentImportDraft.ResourceDraft(
                draft.itemKey(), draft.dayNo(), draft.time(), draft.arrangementType(), draft.sourceName(), draft.city(), draft.remark(),
                selected == null ? null : selected.resourceId(), selected == null ? null : selected.resourceName(),
                selected == null ? null : selected.supplierId(), selected == null ? null : selected.supplierName(),
                selected == null, candidates
        );
    }

    private SupplierCandidate defaultSupplier(Long tenantId, Long resourceId) {
        PurchaseRelationEntity relation = relationMapper.selectOne(new QueryWrapper<PurchaseRelationEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("resource_id", resourceId)
                .eq("status", "active")
                .orderByDesc("is_default")
                .orderByAsc("id")
                .last("limit 1"));
        if (relation == null || relation.getSupplierId() == null) return null;
        SupplierEntity supplier = supplierMapper.selectOne(new QueryWrapper<SupplierEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("id", relation.getSupplierId())
                .last("limit 1"));
        return supplier == null ? null : new SupplierCandidate(supplier.getId(), supplier.getSupplierName(), Boolean.TRUE.equals(relation.getIsDefault()));
    }

    private String resourceType(String arrangementType) {
        return switch (arrangementType) {
            case "meal" -> "restaurant";
            case "scenic", "hotel", "vehicle", "traffic", "shopping", "ground_agent", "other" -> arrangementType;
            default -> "other";
        };
    }

    /** 与保存别名时相同的标准化规则，只去空白和常见分隔符，不做模糊截断。 */
    public String normalize(String value) {
        if (!StringUtils.hasText(value)) return "";
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}，。、“”‘’（）()【】\\[\\]-]+", "");
    }

    /** 比较 Word 城市和资源所在地，兼容“杭州/杭州市”这类展示后缀差异。 */
    private boolean sameCity(String documentCity, String resourceCity) {
        if (!hasKnownCity(documentCity)) return true;
        if (!StringUtils.hasText(resourceCity)) return false;
        return normalizeCity(documentCity).equals(normalizeCity(resourceCity));
    }

    /** AI 以“未标注城市”占位时不应当成真实城市过滤用车等资源候选。 */
    private boolean hasKnownCity(String city) {
        if (!StringUtils.hasText(city)) return false;
        String normalized = normalize(city);
        return !List.of("未标注城市", "未知城市", "未注明", "不详").contains(normalized);
    }

    private String normalizeCity(String value) {
        return normalize(value).replaceAll("市$", "");
    }

    /**
     * 正式名称未命中时，仅补充名称包含关系的候选。例如文档把三家参考酒店合并成一行，
     * 或写成“全程空调旅游车”。候选仍需人工确认，不能当作精确命中自动写入安排。
     */
    private List<PurchaseResourceEntity> fuzzyNameCandidates(
            TeamDocumentImportDraft.ResourceDraft draft, Long tenantId, String resourceType
    ) {
        String sourceName = normalize(draft.sourceName());
        if (!StringUtils.hasText(sourceName)) return List.of();
        List<PurchaseResourceEntity> candidates = resourceMapper.selectList(new QueryWrapper<PurchaseResourceEntity>()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", false)
                .eq("status", "active")
                .eq("resource_type", resourceType)
                .orderByAsc("resource_name")
                .last("limit 200"));
        // 确认单常只写“南京/苏州住宿”，没有具体酒店名。此时返回同一地级市的酒店候选，
        // 因候选不属于精确名称命中，后续仍要求计调手工确认，不能擅自指定酒店。
        candidates = candidates.stream().filter(this::isPurchasable).toList();
        if ("hotel".equals(resourceType) && normalize(sourceName).equals(normalizeCity(draft.city()))) {
            return candidates.stream().filter(resource -> sameCity(draft.city(), resource.getCity())).toList();
        }
        if ("vehicle".equals(resourceType)) {
            List<PurchaseResourceEntity> identityCandidates = candidates.stream()
                    .filter(resource -> !requiresCityMatch(resourceType) || sameCity(draft.city(), resource.getCity()))
                    .filter(resource -> vehicleIdentityMatches(draft, resource)).toList();
            if (!identityCandidates.isEmpty()) return identityCandidates;
        }
        return candidates.stream()
                .filter(resource -> !requiresCityMatch(resourceType) || sameCity(draft.city(), resource.getCity()))
                .filter(resource -> containsEither(sourceName, normalize(resource.getResourceName())))
                .toList();
    }

    /** 景区等地点资源必须在同一地级市内候选；航班、火车等大交通按线路名称匹配。 */
    private boolean requiresCityMatch(String resourceType) {
        return !"traffic".equals(resourceType);
    }

    private boolean containsEither(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right)
                && (left.contains(right) || right.contains(left));
    }

    /** 仅允许需要采购的资源进入供应商匹配和采购安排。空值兼容历史主档，按需要采购处理。 */
    private boolean isPurchasable(PurchaseResourceEntity resource) {
        return resource != null && !isNotRequired(resource);
    }

    private boolean isNotRequired(PurchaseResourceEntity resource) {
        return resource != null && "not_required".equalsIgnoreCase(resource.getProcurementMode());
    }

    private List<PurchaseResourceEntity> mergeDistinct(
            List<PurchaseResourceEntity> first, List<PurchaseResourceEntity> second
    ) {
        Map<Long, PurchaseResourceEntity> merged = new java.util.LinkedHashMap<>();
        first.forEach(item -> merged.put(item.getId(), item));
        second.forEach(item -> merged.putIfAbsent(item.getId(), item));
        return new ArrayList<>(merged.values());
    }

    private List<PurchaseResourceEntity> vehicleIdentityCandidates(
            TeamDocumentImportDraft.ResourceDraft draft, Long tenantId
    ) {
        if (vehicleSeatCount(draft.sourceName()) == null && !StringUtils.hasText(vehicleType(draft.sourceName()))) {
            return List.of();
        }
        return resourceMapper.selectList(new QueryWrapper<PurchaseResourceEntity>()
                        .eq("tenant_id", tenantId).eq("is_deleted", false).eq("status", "active")
                        .eq("resource_type", "vehicle").orderByAsc("resource_name").last("limit 200"))
                .stream().filter(this::isPurchasable)
                .filter(resource -> !requiresCityMatch("vehicle") || sameCity(draft.city(), resource.getCity()))
                .filter(resource -> vehicleIdentityMatches(draft, resource)).toList();
    }

    /** 解析确认单中的车型/座位数，兼容“33座旅游大巴”“旅游大巴（33座）”。 */
    private boolean vehicleIdentityMatches(
            TeamDocumentImportDraft.ResourceDraft draft, PurchaseResourceEntity resource
    ) {
        Integer requestedSeats = vehicleSeatCount(draft.sourceName());
        boolean seatMatches = requestedSeats != null && resource.getSeatCount() != null
                && requestedSeats.equals(resource.getSeatCount());
        String requestedType = normalize(vehicleType(draft.sourceName()));
        String candidateType = normalize(StringUtils.hasText(resource.getVehicleType())
                ? resource.getVehicleType() : resource.getResourceName());
        boolean typeMatches = StringUtils.hasText(requestedType) && StringUtils.hasText(candidateType)
                && containsEither(requestedType, candidateType);
        // 已维护座位数时，文档给出明确座位数就必须一致，不能因为同为“大巴”误选 39 座等其它车型。
        if (requestedSeats != null && resource.getSeatCount() != null) return seatMatches;
        return seatMatches || typeMatches;
    }

    private Integer vehicleSeatCount(String sourceName) {
        if (!StringUtils.hasText(sourceName)) return null;
        Matcher matcher = VEHICLE_SEAT_PATTERN.matcher(sourceName);
        if (!matcher.find()) return null;
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String vehicleType(String sourceName) {
        if (!StringUtils.hasText(sourceName)) return null;
        String value = VEHICLE_SEAT_PATTERN.matcher(sourceName).replaceAll("")
                .replaceAll("[()（）【】\\[\\]]", " ").trim();
        return StringUtils.hasText(value) ? value : null;
    }

    private record SupplierCandidate(Long id, String name, boolean defaultSupplier) {}
}
