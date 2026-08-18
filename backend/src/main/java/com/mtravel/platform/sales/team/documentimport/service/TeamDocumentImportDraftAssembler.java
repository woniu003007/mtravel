package com.mtravel.platform.sales.team.documentimport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportResponse;
import com.mtravel.platform.sales.booking.aiimport.service.LocalBookingImportParser;
import com.mtravel.platform.sales.team.documentimport.dto.TeamDocumentImportDraft;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 将本地可校验解析和百炼结构化候选合并为团队代录草稿。 */
@Component
public class TeamDocumentImportDraftAssembler {

    private static final Pattern DAY_HEADER = Pattern.compile("(?m)^(?:D|第\\s*)(\\d{1,2})(?:天)?[：:、.\\s-]*(.*)$");
    private static final Pattern LOCAL_RESOURCE_TIME = Pattern.compile(
            "(?<!\\d)((?:(?:上午|下午|中午|晚上|早上)\\s*)?(?:[01]?\\d|2[0-3])(?:[:：][0-5]\\d|点(?:[0-5]\\d)?分?)(?:\\s*(?:[-~～]|至|到)\\s*(?:(?:上午|下午|中午|晚上|早上)\\s*)?(?:[01]?\\d|2[0-3])(?:[:：][0-5]\\d|点(?:[0-5]\\d)?分?))?)(?!\\d)"
    );
    private static final Pattern GENERIC_TEAM_TITLE = Pattern.compile(
            "(确认单|确认件|地接确认|接待协议|委托接待协议|行程单|名单|报价单|基本信息)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern META_TITLE_LINE = Pattern.compile(
            "^(FROM|ATTN|TO|客户|客户单位|联系人|电话|手机|甲方|乙方|委托方|接待方)\\s*[:：].*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EXPLICIT_CUSTOMER_LABEL = Pattern.compile("(?m)(客户单位|客户|组团社|委托方|甲方)\\s*[:：]");
    private static final Pattern LOCAL_RESOURCE_MARKER = Pattern.compile("^【([^】\\r\\n]{2,60})】\\s*(.*)$");
    private static final Pattern LOCAL_RESOURCE_ALLOWED_PREFIX = Pattern.compile(
            "^(?:(?:早餐|午餐|晚餐)后|随后|之后|抵达后|到达后|后)?(?:游览|参观|前往|车赴)?$"
    );
    private static final Pattern LOCAL_RESOURCE_EXCLUDED_CONTEXT = Pattern.compile(
            "(参考酒店|温馨提示|远观|路过|途经|车览|外观|自费|另行付费|费用自理)"
    );
    private static final Pattern LOCAL_RESOURCE_GENERIC_NAME = Pattern.compile(
            "^(接待标准|接待标准及注意事项|参考酒店|温馨提示|注意事项|特别说明|报价不含|汉服体验|自由活动)$"
    );
    private static final Pattern LOCAL_RESOURCE_SECTION_STOP = Pattern.compile(
            "^(?:【)?(?:接待标准|接待标准及注意事项|费用说明|游客名单|团队名单)(?:】)?$"
    );
    private static final Pattern PRODUCT_DESCRIPTION_SECTION_HEADER = Pattern.compile(
            "^(?:[【\\[（(]\\s*)?(产品说明|费用包含|包含费用|费用不含|报价不含|儿童安排|儿童说明|儿童|购物安排|购物项目|自费项目|自费说明|赠送项目|特别说明|注意事项|温馨提示|温馨提醒|说明)(?:\\s*[】\\]）)])?\\s*(?:[：:]\\s*(.*))?$"
    );
    private static final Pattern PRODUCT_DESCRIPTION_SECTION_END = Pattern.compile(
            "^(?:[【\\[（(]\\s*)?(?:行程安排|每日行程|游客名单|团队名单|成本明细|结算明细|报价明细|接待标准)(?:\\s*[】\\]）)])?\\s*$"
    );
    private static final Pattern COMPANY_SUFFIX = Pattern.compile(
            "(有限责任公司|股份有限公司|国际旅行社有限公司|国际旅行社|旅行社有限公司|旅游发展有限公司|旅游有限公司|旅行社|旅游公司|有限公司|集团|公司|分公司)$"
    );
    private final LocalBookingImportParser localParser;
    private final ObjectMapper objectMapper;

    public TeamDocumentImportDraftAssembler(LocalBookingImportParser localParser, ObjectMapper objectMapper) {
        this.localParser = localParser;
        this.objectMapper = objectMapper;
    }

    /**
     * 组装草稿。客户主体优先结合 AI parties 与当前企业校验；游客、价格和交通优先使用本地规则结果，保留可验证性。
     */
    public TeamDocumentImportDraft assemble(String text, String aiJson, String sourceType) {
        return assemble(text, aiJson, sourceType, null, null, Map.of());
    }

    /**
     * 组装草稿，并结合文件名、当前企业信息和脱敏 token 还原更准确的客户主体。
     */
    public TeamDocumentImportDraft assemble(
            String text,
            String aiJson,
            String sourceType,
            String sourceFileName,
            String currentCompanyName,
            Map<String, String> phoneTokens
    ) {
        BookingAiImportResponse local = localParser.parse(text, sourceType);
        JsonNode ai = readJson(aiJson);
        List<String> warnings = new ArrayList<>(safe(local.warnings()));
        warnings.addAll(stringArray(ai.path("warnings")));
        String documentType = allowedDocumentType(text(ai, "documentType"));
        List<TeamDocumentImportDraft.ItineraryDraft> itineraryDays = itineraryDays(ai, text);
        List<TeamDocumentImportDraft.ItineraryDraft> originalItineraryDays = sourceItineraryDays(text);
        List<TeamDocumentImportDraft.ItineraryDraft> resourceTimeItineraryDays = originalItineraryDays.isEmpty()
                ? itineraryDays
                : originalItineraryDays;
        List<TeamDocumentImportDraft.ResourceDraft> resources = resourceDrafts(
                ai, resourceTimeItineraryDays, businessPartyCompanyNames(ai)
        );
        if (resources.isEmpty()) {
            resources = localResourceDrafts(resourceTimeItineraryDays);
            if (!resources.isEmpty()) {
                warnings.add("AI未返回资源，已从每日行程的明确景点标记生成待确认候选");
            }
        }
        BookingAiImportResponse.TravelInfo travel = local.travelInfo();
        BookingAiImportResponse.CustomerInfo customer = resolveCustomerInfo(
                text, ai, currentCompanyName, phoneTokens, local.customerInfo(), warnings
        );
        BookingAiImportResponse.GuideInfo guide = local.guideInfo();
        BookingAiImportResponse.PriceInfo price = local.priceInfo();
        BookingAiImportResponse.AdditionalInfo additional = local.additionalInfo();
        TeamDocumentImportDraft.ProductDescriptionDraft productDescription = productDescriptionDraft(ai, text);
        String departureDate = firstText(text(ai, "departureDate"), travel == null ? null : travel.joinDate());
        if (!isDate(departureDate)) {
            departureDate = null;
            warnings.add("未识别到可确认的发团日期，请计调手工填写");
        }
        List<TeamDocumentImportDraft.GuestDraft> guests = guests(local.guests());
        Integer travelDays = positiveInteger(ai.path("travelDays"));
        if (travelDays == null) travelDays = itineraryDays.isEmpty() ? 1 : itineraryDays.size();
        Integer totalSeats = guests.isEmpty() ? null : guests.size();
        TeamDocumentImportDraft.TeamDraft team = new TeamDocumentImportDraft.TeamDraft(
                resolveTeamName(ai, text, sourceFileName, currentCompanyName, warnings),
                departureDate,
                travelDays,
                totalSeats,
                null,
                "domestic",
                additional == null ? null : additional.receptionStandard(),
                additional == null ? null : additional.notes()
        );
        TeamDocumentImportDraft.OrderDraft order = new TeamDocumentImportDraft.OrderDraft(
                null,
                customer == null ? null : customer.customerName(),
                customer == null ? null : customer.contactName(),
                customer == null ? null : customer.contactPhone(),
                trafficText(travel, true),
                trafficText(travel, false),
                guide == null ? null : guide.guideName(),
                guide == null ? null : guide.guidePhone(),
                customer == null ? null : customer.remark(),
                orderPriceDrafts(price, guests.size())
        );
        return new TeamDocumentImportDraft(
                documentType,
                local.confidence(),
                team,
                order,
                guests,
                itineraryDays,
                resources,
                distinct(warnings),
                distinct(mergeEvidence(local.evidence(), stringArray(ai.path("evidence")))),
                productDescription
        );
    }

    /**
     * 将模型结构化结果与 Word 标题段落兜底合并。
     *
     * <p>模型字段优先，标题兜底只补空字段。这样既能保留模型对复杂版式的理解，也不会因为
     * “温馨提示/温馨提醒/特别说明”写法不同而让整组产品说明为空。</p>
     */
    private TeamDocumentImportDraft.ProductDescriptionDraft productDescriptionDraft(JsonNode ai, String sourceText) {
        JsonNode model = firstObject(ai.path("productDescription"), ai.path("productDetails"), ai.path("descriptionDraft"));
        TeamDocumentImportDraft.ProductDescriptionDraft modelDraft = productDescriptionFromJson(model, ai);
        TeamDocumentImportDraft.ProductDescriptionDraft sourceDraft = extractProductDescriptionFromSource(sourceText);
        TeamDocumentImportDraft.ProductDescriptionDraft merged = new TeamDocumentImportDraft.ProductDescriptionDraft(
                firstText(modelDraft == null ? null : modelDraft.content(), sourceDraft == null ? null : sourceDraft.content()),
                firstText(modelDraft == null ? null : modelDraft.feeIncluded(), sourceDraft == null ? null : sourceDraft.feeIncluded()),
                firstText(modelDraft == null ? null : modelDraft.feeExcluded(), sourceDraft == null ? null : sourceDraft.feeExcluded()),
                firstText(modelDraft == null ? null : modelDraft.childPolicy(), sourceDraft == null ? null : sourceDraft.childPolicy()),
                firstText(modelDraft == null ? null : modelDraft.shoppingArrangement(), sourceDraft == null ? null : sourceDraft.shoppingArrangement()),
                firstText(modelDraft == null ? null : modelDraft.optionalItems(), sourceDraft == null ? null : sourceDraft.optionalItems()),
                firstText(modelDraft == null ? null : modelDraft.giftItems(), sourceDraft == null ? null : sourceDraft.giftItems()),
                firstText(modelDraft == null ? null : modelDraft.attentionItems(), sourceDraft == null ? null : sourceDraft.attentionItems()),
                firstText(modelDraft == null ? null : modelDraft.warmReminder(), sourceDraft == null ? null : sourceDraft.warmReminder())
        );
        return hasProductDescriptionContent(merged) ? merged : null;
    }

    /**
     * 仅从 Word 已提取的原文中回填产品说明，不调用模型，也不生成或匹配资源。
     *
     * <p>供历史导入草稿在详情读取时做一次兼容补齐；新导入仍由 {@link #assemble(String, String, String)}
     * 在模型结果与本地标题段落之间合并。</p>
     */
    public TeamDocumentImportDraft.ProductDescriptionDraft extractProductDescriptionFromSource(String sourceText) {
        return productDescriptionFromSource(sourceText);
    }

    /** 兼容提示词历史版本、中文字段名和模型惯用的英文别名。 */
    private TeamDocumentImportDraft.ProductDescriptionDraft productDescriptionFromJson(JsonNode model, JsonNode root) {
        return new TeamDocumentImportDraft.ProductDescriptionDraft(
                descriptionText(model, root, "content", "productDescription", "productIntro", "productIntroduction", "description", "产品说明"),
                descriptionText(model, root, "feeIncluded", "includedFees", "costIncluded", "includeFees", "费用包含", "包含费用"),
                descriptionText(model, root, "feeExcluded", "excludedFees", "costExcluded", "excludeFees", "费用不含", "报价不含"),
                descriptionText(model, root, "childPolicy", "childArrangement", "childrenArrangement", "childItems", "儿童安排", "儿童说明"),
                descriptionText(model, root, "shoppingArrangement", "shoppingItems", "shopping", "购物安排", "购物项目"),
                descriptionText(model, root, "optionalItems", "optional", "selfPaidItems", "selfExpenseItems", "自费项目", "自费说明"),
                descriptionText(model, root, "giftItems", "gift", "gifts", "complimentaryItems", "赠送项目"),
                descriptionText(model, root, "attentionItems", "specialInstructions", "specialInstruction", "specialNotes", "noticeItems", "特别说明", "注意事项"),
                descriptionText(model, root, "warmReminder", "warmTips", "warmTip", "warmPrompt", "warmNotice", "温馨提示", "温馨提醒")
        );
    }

    private String descriptionText(JsonNode primary, JsonNode fallback, String... fields) {
        for (String field : fields) {
            String value = text(primary, field);
            if (StringUtils.hasText(value)) return value;
        }
        for (String field : fields) {
            String value = text(fallback, field);
            if (StringUtils.hasText(value)) return value;
        }
        return null;
    }

    private JsonNode firstObject(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && node.isObject()) return node;
        }
        return objectMapper.createObjectNode();
    }

    /**
     * 本地只提取有明确标题的说明段，不把普通行程、景点介绍或游客名单误填到产品说明页。
     */
    private TeamDocumentImportDraft.ProductDescriptionDraft productDescriptionFromSource(String sourceText) {
        if (!StringUtils.hasText(sourceText)) return null;
        Map<DescriptionField, List<String>> sections = new java.util.EnumMap<>(DescriptionField.class);
        DescriptionField current = null;
        String[] lines = sourceText.replace('\r', '\n').split("\\n");
        for (int index = 0; index < lines.length; index += 1) {
            String line = normalizeDescriptionLine(lines[index]);
            if (!StringUtils.hasText(line)) continue;
            SectionHeader header = descriptionSectionHeader(line);
            // Word 表格常将“特别 / 说明”“温馨 / 提示”拆到相邻单元格；只在拼接后能成为
            // 已知标题时才合并，避免把普通正文误当作产品说明分段。
            if (header == null && index + 1 < lines.length) {
                String next = normalizeDescriptionLine(lines[index + 1]);
                if (StringUtils.hasText(next)) {
                    header = descriptionSectionHeader(line + next);
                    if (header != null) {
                        index += 1;
                    }
                }
            }
            if (header != null) {
                current = header.field();
                appendDescriptionSection(sections, current, header.inlineContent());
                continue;
            }
            if (PRODUCT_DESCRIPTION_SECTION_END.matcher(line).matches()) {
                current = null;
                continue;
            }
            appendDescriptionSection(sections, current, line);
        }
        TeamDocumentImportDraft.ProductDescriptionDraft result = new TeamDocumentImportDraft.ProductDescriptionDraft(
                sectionText(sections, DescriptionField.CONTENT),
                sectionText(sections, DescriptionField.FEE_INCLUDED),
                sectionText(sections, DescriptionField.FEE_EXCLUDED),
                sectionText(sections, DescriptionField.CHILD_POLICY),
                sectionText(sections, DescriptionField.SHOPPING_ARRANGEMENT),
                sectionText(sections, DescriptionField.OPTIONAL_ITEMS),
                sectionText(sections, DescriptionField.GIFT_ITEMS),
                sectionText(sections, DescriptionField.ATTENTION_ITEMS),
                sectionText(sections, DescriptionField.WARM_REMINDER)
        );
        return hasProductDescriptionContent(result) ? result : null;
    }

    private SectionHeader descriptionSectionHeader(String line) {
        Matcher matcher = PRODUCT_DESCRIPTION_SECTION_HEADER.matcher(line);
        if (!matcher.matches()) return null;
        DescriptionField field = switch (matcher.group(1)) {
            case "产品说明" -> DescriptionField.CONTENT;
            case "费用包含", "包含费用" -> DescriptionField.FEE_INCLUDED;
            case "费用不含", "报价不含" -> DescriptionField.FEE_EXCLUDED;
            case "儿童安排", "儿童说明", "儿童" -> DescriptionField.CHILD_POLICY;
            case "购物安排", "购物项目" -> DescriptionField.SHOPPING_ARRANGEMENT;
            case "自费项目", "自费说明" -> DescriptionField.OPTIONAL_ITEMS;
            case "赠送项目" -> DescriptionField.GIFT_ITEMS;
            case "特别说明", "注意事项", "说明" -> DescriptionField.ATTENTION_ITEMS;
            case "温馨提示", "温馨提醒" -> DescriptionField.WARM_REMINDER;
            default -> null;
        };
        return field == null ? null : new SectionHeader(field, blankToNull(matcher.group(2)));
    }

    private String normalizeDescriptionLine(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.replaceAll("^[\\p{Cf}\\s\\u00A0•·*\\-]+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void appendDescriptionSection(
            Map<DescriptionField, List<String>> sections,
            DescriptionField field,
            String value
    ) {
        if (field == null || !StringUtils.hasText(value)) return;
        sections.computeIfAbsent(field, ignored -> new ArrayList<>()).add(value.trim());
    }

    private String sectionText(Map<DescriptionField, List<String>> sections, DescriptionField field) {
        return sections.getOrDefault(field, List.of()).stream().filter(StringUtils::hasText).distinct()
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private boolean hasProductDescriptionContent(TeamDocumentImportDraft.ProductDescriptionDraft value) {
        return value != null && java.util.stream.Stream.of(
                value.content(), value.feeIncluded(), value.feeExcluded(), value.childPolicy(), value.shoppingArrangement(),
                value.optionalItems(), value.giftItems(), value.attentionItems(), value.warmReminder()
        ).anyMatch(StringUtils::hasText);
    }

    private JsonNode readJson(String value) {
        if (!StringUtils.hasText(value)) return objectMapper.createObjectNode();
        String trimmed = value.trim().replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        // 部分模型会在 JSON 前后附带一句说明；只截取最外层对象，仍由 Jackson 严格校验字段结构。
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            return node != null && node.isObject() ? node : objectMapper.createObjectNode();
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private List<TeamDocumentImportDraft.ItineraryDraft> itineraryDays(JsonNode ai, String source) {
        List<TeamDocumentImportDraft.ItineraryDraft> days = new ArrayList<>();
        JsonNode modelDays = ai.path("itineraryDays");
        if (modelDays.isArray()) {
            for (JsonNode item : modelDays) {
                Integer dayNo = positiveInteger(item.path("dayNo"));
                if (dayNo == null) continue;
                days.add(new TeamDocumentImportDraft.ItineraryDraft(
                        dayNo, text(item, "dayTitle"), text(item, "content"), text(item, "accommodation"),
                        item.path("breakfast").asBoolean(false), item.path("lunch").asBoolean(false), item.path("dinner").asBoolean(false)
                ));
            }
        }
        if (!days.isEmpty()) return days;
        return sourceItineraryDays(source);
    }

    /** 从 Word 原文划分每日行程，供本地资源和时间兜底关联使用。 */
    private List<TeamDocumentImportDraft.ItineraryDraft> sourceItineraryDays(String source) {
        List<TeamDocumentImportDraft.ItineraryDraft> days = new ArrayList<>();
        String sourceText = source == null ? "" : source;
        Matcher matcher = DAY_HEADER.matcher(sourceText);
        List<DayHeader> headers = new ArrayList<>();
        while (matcher.find()) {
            headers.add(new DayHeader(Integer.parseInt(matcher.group(1)), blankToNull(matcher.group(2)), matcher.end(), matcher.start()));
        }
        for (int index = 0; index < headers.size(); index += 1) {
            DayHeader header = headers.get(index);
            int end = index + 1 < headers.size() ? headers.get(index + 1).start() : sourceText.length();
            String content = sourceText.substring(header.contentStart(), end).trim();
            days.add(new TeamDocumentImportDraft.ItineraryDraft(
                    header.dayNo(), header.title(), blankToNull(content), null, false, false, false
            ));
        }
        return days;
    }

    private List<TeamDocumentImportDraft.ResourceDraft> resourceDrafts(
            JsonNode ai,
            List<TeamDocumentImportDraft.ItineraryDraft> originalItineraryDays,
            Set<String> businessPartyCompanyNames
    ) {
        List<TeamDocumentImportDraft.ResourceDraft> result = new ArrayList<>();
        JsonNode resources = ai.path("resources");
        int[] index = {1};
        if (resources.isArray()) {
            resources.forEach(item -> addResource(
                    result, item, null, originalItineraryDays, businessPartyCompanyNames, index
            ));
        }
        // 兼容模型把资源放在每日行程下的返回格式，避免顶层数组缺失时整份文档变成“资源 0”。
        JsonNode itineraryDays = ai.path("itineraryDays");
        if (itineraryDays.isArray()) {
            itineraryDays.forEach(day -> {
                Integer dayNo = positiveInteger(day.path("dayNo"));
                JsonNode dayResources = day.path("resources");
                if (dayResources.isArray()) {
                    dayResources.forEach(item -> addResource(
                            result, item, dayNo, originalItineraryDays, businessPartyCompanyNames, index
                    ));
                }
            });
        }
        return result;
    }

    /**
     * 百炼不可用时，只从每日行程中独立标记的【景点名】生成候选。
     *
     * <p>普通段落、参考酒店、温馨提示以及远观/路过/自费项目均不参与兜底，避免把说明文字误当成
     * 正式资源。候选保留待确认状态，后续仍需计调核对资源主档和供应商。</p>
     */
    private List<TeamDocumentImportDraft.ResourceDraft> localResourceDrafts(
            List<TeamDocumentImportDraft.ItineraryDraft> itineraryDays
    ) {
        Map<String, TeamDocumentImportDraft.ResourceDraft> resources = new LinkedHashMap<>();
        int index = 1;
        for (TeamDocumentImportDraft.ItineraryDraft day : safe(itineraryDays)) {
            if (!StringUtils.hasText(day.itineraryContent())) continue;
            for (String rawLine : day.itineraryContent().split("\\R")) {
                String line = normalizeLocalResourceLine(rawLine);
                if (!StringUtils.hasText(line)) continue;
                if (LOCAL_RESOURCE_SECTION_STOP.matcher(line).matches()) break;

                int markerStart = line.indexOf('【');
                if (markerStart < 0) continue;
                String prefix = line.substring(0, markerStart).trim();
                if (LOCAL_RESOURCE_EXCLUDED_CONTEXT.matcher(prefix).find()
                        || !LOCAL_RESOURCE_ALLOWED_PREFIX.matcher(prefix).matches()) {
                    continue;
                }
                Matcher marker = LOCAL_RESOURCE_MARKER.matcher(line.substring(markerStart));
                if (!marker.matches()) continue;
                String resourceName = blankToNull(marker.group(1));
                String qualifier = leadingQualifier(marker.group(2));
                if (!StringUtils.hasText(resourceName)
                        || LOCAL_RESOURCE_GENERIC_NAME.matcher(resourceName).matches()
                        || LOCAL_RESOURCE_EXCLUDED_CONTEXT.matcher(resourceName).find()
                        || LOCAL_RESOURCE_EXCLUDED_CONTEXT.matcher(qualifier).find()) {
                    continue;
                }
                String key = day.dayNo() + "|" + resourceName.toLowerCase(Locale.ROOT)
                        .replaceAll("[\\s\\p{Punct}，。、“”‘’（）()【】\\[\\]-]+", "");
                if (resources.containsKey(key)) continue;
                resources.put(key, new TeamDocumentImportDraft.ResourceDraft(
                        "local-resource:" + index++, day.dayNo(), localResourceTime(rawLine), "scenic", resourceName, null, null,
                        null, null, null, null, true, List.of()
                ));
            }
        }
        return new ArrayList<>(resources.values());
    }

    private String normalizeLocalResourceLine(String value) {
        if (!StringUtils.hasText(value)) return "";
        return value.replaceFirst("^[\\p{Cf}\\s\\u00A0•·*\\-]+", "").trim();
    }

    /** 只检查资源标记后的开头限定语，正文后段偶然出现“远观”等词不影响主景点。 */
    private String leadingQualifier(String value) {
        if (!StringUtils.hasText(value)) return "";
        String trimmed = value.trim();
        int maxLength = Math.min(trimmed.length(), 18);
        return trimmed.substring(0, maxLength)
                .replaceAll("^[\\s（()）【】\\[\\]：:，,。；;、-]+", "");
    }

    /**
     * 仅在同一资源行只有一个明确时间表达式时回填，避免把同一天不同安排的时刻错配给资源。
     * 时间区间会整体保留为原文，单个时刻也保留其原文格式。
     */
    private String localResourceTime(String line) {
        if (!StringUtils.hasText(line)) return null;
        List<String> values = new ArrayList<>();
        Matcher matcher = LOCAL_RESOURCE_TIME.matcher(line);
        while (matcher.find()) {
            String value = blankToNull(matcher.group(1));
            if (value != null) values.add(value);
        }
        List<String> distinctValues = values.stream().distinct().toList();
        return distinctValues.size() == 1 ? distinctValues.getFirst() : null;
    }

    /**
     * 模型漏填时间时，按资源所在日和资源名称从 Word 原文中补一次。多条不同时刻或没有日次都视为不可靠。
     */
    private String timeFromOriginalItinerary(
            List<TeamDocumentImportDraft.ItineraryDraft> itineraryDays, Integer dayNo, String resourceName
    ) {
        if (dayNo == null || !StringUtils.hasText(resourceName)) return null;
        List<String> values = new ArrayList<>();
        for (TeamDocumentImportDraft.ItineraryDraft day : safe(itineraryDays)) {
            if (!Objects.equals(dayNo, day.dayNo()) || !StringUtils.hasText(day.itineraryContent())) continue;
            for (String line : day.itineraryContent().split("\\R")) {
                if (!itineraryLineMentionsResource(line, resourceName)) continue;
                String time = localResourceTime(line);
                if (time != null) values.add(time);
            }
        }
        List<String> distinctValues = values.stream().distinct().toList();
        return distinctValues.size() == 1 ? distinctValues.getFirst() : null;
    }

    private boolean itineraryLineMentionsResource(String line, String resourceName) {
        String normalizedName = normalizeResourceText(resourceName);
        return normalizedName.length() >= 2 && normalizeResourceText(line).contains(normalizedName);
    }

    private String normalizeResourceText(String value) {
        if (!StringUtils.hasText(value)) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。、“”‘’（）()【】\\[\\]-]+", "");
    }

    private BookingAiImportResponse.CustomerInfo resolveCustomerInfo(
            String sourceText,
            JsonNode ai,
            String currentCompanyName,
            Map<String, String> phoneTokens,
            BookingAiImportResponse.CustomerInfo localCustomer,
            List<String> warnings
    ) {
        List<PartyCandidate> parties = partyCandidates(ai);
        ResolvedCustomer resolved = resolveCustomerFromParties(parties, currentCompanyName, phoneTokens);
        if (resolved.customer() != null) {
            return resolved.customer();
        }
        if (StringUtils.hasText(resolved.warning())) {
            warnings.add(resolved.warning());
        }
        if (hasExplicitCustomerLabel(sourceText)
                && localCustomer != null
                && StringUtils.hasText(localCustomer.customerName())
                && !matchesCurrentCompany(localCustomer.customerName(), currentCompanyName)) {
            return localCustomer;
        }
        warnings.add("客户单位未能自动判断，请人工确认委托方后填写");
        return new BookingAiImportResponse.CustomerInfo(
                null,
                null,
                null,
                localCustomer == null ? null : localCustomer.sourcePlace(),
                localCustomer == null ? null : localCustomer.remark(),
                List.of("客户单位需人工确认")
        );
    }

    private ResolvedCustomer resolveCustomerFromParties(
            List<PartyCandidate> parties,
            String currentCompanyName,
            Map<String, String> phoneTokens
    ) {
        if (parties.isEmpty()) {
            return new ResolvedCustomer(null, null);
        }
        List<PartyCandidate> populated = parties.stream()
                .filter(item -> StringUtils.hasText(item.companyName()) || StringUtils.hasText(item.contactName()))
                .toList();
        if (populated.isEmpty()) {
            return new ResolvedCustomer(null, null);
        }
        List<PartyCandidate> internalMatches = populated.stream()
                .filter(item -> isInternalParty(item, currentCompanyName))
                .toList();
        if (internalMatches.size() == 1) {
            List<PartyCandidate> externals = populated.stream()
                    .filter(item -> !Objects.equals(item, internalMatches.getFirst()))
                    .filter(item -> !isInternalRole(item.businessRole()))
                    .sorted(partyComparator())
                    .toList();
            if (externals.size() == 1 && hasStrongExternalSignal(externals.getFirst())) {
                return new ResolvedCustomer(toCustomerInfo(externals.getFirst(), phoneTokens), null);
            }
            if (!externals.isEmpty()) {
                return new ResolvedCustomer(null, "识别到多个外部主体，无法自动确定客户单位，请人工确认");
            }
        }
        List<PartyCandidate> explicitCustomers = populated.stream()
                .filter(item -> !matchesCurrentCompany(item.companyName(), currentCompanyName))
                .filter(item -> isExternalCustomerRole(item.businessRole(), item.direction()))
                .sorted(partyComparator())
                .toList();
        if (explicitCustomers.size() == 1) {
            return new ResolvedCustomer(toCustomerInfo(explicitCustomers.getFirst(), phoneTokens), null);
        }
        if (explicitCustomers.size() > 1) {
            return new ResolvedCustomer(null, "识别到多个可能的委托方，无法自动确定客户单位，请人工确认");
        }
        if (internalMatches.size() == populated.size()) {
            return new ResolvedCustomer(null, "识别到的主体均像本方信息，未找到明确客户单位，请人工确认");
        }
        return new ResolvedCustomer(null, null);
    }

    private BookingAiImportResponse.CustomerInfo toCustomerInfo(PartyCandidate party, Map<String, String> phoneTokens) {
        return new BookingAiImportResponse.CustomerInfo(
                blankToNull(party.companyName()),
                blankToNull(party.contactName()),
                resolvePhoneToken(party.phoneToken(), phoneTokens),
                null,
                blankToNull(party.evidence()),
                List.of()
        );
    }

    private List<PartyCandidate> partyCandidates(JsonNode ai) {
        Map<String, PartyCandidate> parties = new LinkedHashMap<>();
        JsonNode array = ai.path("parties");
        if (array.isArray()) {
            array.forEach(item -> addPartyCandidate(parties, item));
        }
        addPartyCandidate(parties, ai.path("senderParty"));
        addPartyCandidate(parties, ai.path("receiverParty"));
        addPartyCandidate(parties, ai.path("customerParty"));
        addPartyCandidate(parties, ai.path("ourParty"));
        return new ArrayList<>(parties.values());
    }

    private void addPartyCandidate(Map<String, PartyCandidate> parties, JsonNode node) {
        if (node == null || !node.isObject()) return;
        String companyName = firstText(text(node, "companyName"), text(node, "company"), text(node, "name"));
        String contactName = firstText(text(node, "contactName"), text(node, "contact"));
        String phoneToken = firstText(text(node, "phoneToken"), text(node, "contactPhoneToken"), text(node, "phone"));
        String direction = firstText(text(node, "direction"), text(node, "side"));
        String businessRole = firstText(text(node, "businessRole"), text(node, "role"));
        String evidence = text(node, "evidence");
        Double confidence = decimalNumber(node.path("confidence"));
        if (!StringUtils.hasText(companyName) && !StringUtils.hasText(contactName)) {
            return;
        }
        String key = normalizeCompanyCore(companyName) + "|" + blankToNull(contactName) + "|" + blankToNull(direction);
        PartyCandidate candidate = new PartyCandidate(companyName, contactName, phoneToken, direction, businessRole, evidence, confidence);
        PartyCandidate existing = parties.get(key);
        if (existing == null || score(candidate) >= score(existing)) {
            parties.put(key, candidate);
        }
    }

    private Comparator<PartyCandidate> partyComparator() {
        return Comparator.<PartyCandidate>comparingDouble(this::score).reversed();
    }

    private double score(PartyCandidate value) {
        double score = value.confidence() == null ? 0D : value.confidence();
        if (StringUtils.hasText(value.companyName())) score += 0.3D;
        if (StringUtils.hasText(value.contactName())) score += 0.1D;
        if (isExternalCustomerRole(value.businessRole(), value.direction())) score += 0.5D;
        return score;
    }

    private boolean hasExplicitCustomerLabel(String text) {
        return StringUtils.hasText(text) && EXPLICIT_CUSTOMER_LABEL.matcher(text).find();
    }

    private boolean isInternalParty(PartyCandidate party, String currentCompanyName) {
        return matchesCurrentCompany(party.companyName(), currentCompanyName) || isInternalRole(party.businessRole());
    }

    private boolean isInternalRole(String value) {
        if (!StringUtils.hasText(value)) return false;
        String normalized = normalizeRole(value);
        return normalized.contains("our")
                || normalized.contains("receiver")
                || normalized.contains("receiving")
                || normalized.contains("ground")
                || normalized.contains("service_provider")
                || normalized.contains("attn")
                || normalized.contains("接待")
                || normalized.contains("乙方");
    }

    private boolean isExternalCustomerRole(String businessRole, String direction) {
        String normalizedRole = normalizeRole(businessRole);
        String normalizedDirection = normalizeRole(direction);
        return normalizedRole.contains("customer")
                || normalizedRole.contains("client")
                || normalizedRole.contains("entrust")
                || normalizedRole.contains("sender")
                || normalizedRole.contains("from")
                || normalizedRole.contains("甲方")
                || normalizedRole.contains("委托")
                || normalizedRole.contains("组团")
                || (!StringUtils.hasText(normalizedRole)
                && (normalizedDirection.contains("sender") || normalizedDirection.contains("from")));
    }

    private boolean hasStrongExternalSignal(PartyCandidate party) {
        if (isExternalCustomerRole(party.businessRole(), party.direction())) {
            return true;
        }
        String normalizedDirection = normalizeRole(party.direction());
        return party.confidence() != null
                && party.confidence() >= 0.85D
                && (normalizedDirection.contains("sender") || normalizedDirection.contains("from"));
    }

    private String normalizeRole(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String resolvePhoneToken(String tokenOrPhone, Map<String, String> phoneTokens) {
        if (!StringUtils.hasText(tokenOrPhone)) return null;
        String trimmed = tokenOrPhone.trim();
        return phoneTokens.get(trimmed);
    }

    private String resolveTeamName(
            JsonNode ai,
            String sourceText,
            String sourceFileName,
            String currentCompanyName,
            List<String> warnings
    ) {
        String aiTeamName = validTeamName(text(ai, "teamName"));
        String fileNameTeam = teamNameFromFileName(sourceFileName, currentCompanyName);
        String textTitle = validTeamName(title(sourceText));
        String result = firstText(aiTeamName, fileNameTeam, textTitle);
        if (!StringUtils.hasText(result)) {
            warnings.add("团队名称未能自动确认，请人工填写");
            return "Word导入团队";
        }
        if (!StringUtils.hasText(aiTeamName) && StringUtils.hasText(fileNameTeam)) {
            warnings.add("团队名称已结合文件名语义推断，请人工确认");
        }
        return result;
    }

    private String teamNameFromFileName(String sourceFileName, String currentCompanyName) {
        if (!StringUtils.hasText(sourceFileName)) return null;
        String normalized = sourceFileName.trim().replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(slashIndex + 1);
        }
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex > 0) {
            normalized = normalized.substring(0, dotIndex);
        }
        normalized = normalized
                .replaceAll("^\\d{1,2}[._/-]\\d{1,2}[-_—\\s]*", "")
                .replaceAll("【[A-Za-z0-9\\-_.]+】", " ")
                .replaceAll("\\[[A-Za-z0-9\\-_.]+\\]", " ")
                .replaceAll("[（(](地接确认|确认单|确认件|接待协议|委托接待协议)[）)]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (StringUtils.hasText(currentCompanyName)) {
            normalized = normalized
                    .replace(currentCompanyName, " ")
                    .replace(normalizeCompanyCore(currentCompanyName), " ")
                    .trim();
        }
        String normalizedFileName = normalized;
        List<String> candidates = new ArrayList<>();
        for (String segment : normalizedFileName.split("[-_\\s]+")) {
            String value = segment
                    .replaceAll("[（(][^）)]{1,12}[）)]$", "")
                    .replaceAll("^(确认单|确认件|地接确认|接待协议)+", "")
                    .trim();
            if (StringUtils.hasText(value)) {
                candidates.add(value);
            }
        }
        return candidates.stream()
                .map(this::validTeamName)
                .filter(StringUtils::hasText)
                .filter(item -> !looksLikeCompanyName(item))
                .max(Comparator.comparingInt(String::length))
                .orElseGet(() -> validTeamName(normalizedFileName));
    }

    private String validTeamName(String value) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim()
                .replaceAll("^[\\-—_\\s]+", "")
                .replaceAll("[\\-—_\\s]+$", "")
                .replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)
                || GENERIC_TEAM_TITLE.matcher(normalized).find()
                || META_TITLE_LINE.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    private boolean looksLikeCompanyName(String value) {
        if (!StringUtils.hasText(value)) return false;
        return value.endsWith("旅行社")
                || value.endsWith("假期")
                || value.endsWith("旅游")
                || value.endsWith("国旅")
                || value.endsWith("国际")
                || value.endsWith("公司");
    }

    private boolean matchesCurrentCompany(String companyName, String currentCompanyName) {
        if (!StringUtils.hasText(companyName) || !StringUtils.hasText(currentCompanyName)) return false;
        String left = normalizeCompanyCore(companyName);
        String right = normalizeCompanyCore(currentCompanyName);
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) return false;
        return left.equals(right)
                || (left.length() >= 4 && right.contains(left))
                || (right.length() >= 4 && left.contains(right));
    }

    private String normalizeCompanyCore(String value) {
        if (!StringUtils.hasText(value)) return "";
        String normalized = value.trim()
                .replaceAll("[\\s·•,，。、“”\"'`~!@#$%^&*()（）【】\\[\\]<>《》?:：;；/\\\\|+_=]+", "");
        String previous;
        do {
            previous = normalized;
            normalized = COMPANY_SUFFIX.matcher(normalized).replaceFirst("");
        } while (!previous.equals(normalized));
        return normalized;
    }

    private void addResource(
            List<TeamDocumentImportDraft.ResourceDraft> result,
            JsonNode item,
            Integer defaultDayNo,
            List<TeamDocumentImportDraft.ItineraryDraft> originalItineraryDays,
            Set<String> businessPartyCompanyNames,
            int[] index
    ) {
        if (item == null || !item.isObject()) return;
        String name = firstText(text(item, "resourceName"), text(item, "name"), text(item, "resource"));
        String type = arrangementType(firstText(text(item, "resourceType"), text(item, "type"), text(item, "category")));
        if (!StringUtils.hasText(name) || type == null) return;
        // FROM、TO、ATTN 等业务往来方不是行程资源。只过滤被模型错误分类为地接的公司，
        // 不以联系人姓名匹配，避免同名人员或资源被误删。
        if ("ground_agent".equals(type) && businessPartyCompanyNames.contains(normalizeCompanyCore(name))) return;
        Integer dayNo = positiveInteger(item.path("dayNo"));
        Integer resolvedDayNo = dayNo == null ? defaultDayNo : dayNo;
        String time = firstText(resourceTime(item), timeFromOriginalItinerary(originalItineraryDays, resolvedDayNo, name));
        result.add(new TeamDocumentImportDraft.ResourceDraft(
                "resource:" + index[0]++, resolvedDayNo, time, type, name,
                firstText(text(item, "city"), text(item, "location")), text(item, "remark"),
                null, null, null, null, true, List.of()
        ));
    }

    /** 从 AI parties 中提取明确公司主体，供资源草稿排除误判的地接服务。 */
    private Set<String> businessPartyCompanyNames(JsonNode ai) {
        return partyCandidates(ai).stream()
                .map(PartyCandidate::companyName)
                .filter(StringUtils::hasText)
                .map(this::normalizeCompanyCore)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** 兼容模型在不同提示词版本中使用的资源时间字段，模型原文时间表述不做破坏性改写。 */
    private String resourceTime(JsonNode item) {
        return firstText(
                text(item, "time"),
                text(item, "visitTime"),
                text(item, "startTime"),
                text(item, "scheduleTime"),
                text(item, "arrangementTime"),
                text(item, "departureTime"),
                text(item, "arrivalTime"),
                text(item, "visit_time"),
                text(item, "start_time"),
                text(item, "schedule_time")
        );
    }

    private List<TeamDocumentImportDraft.GuestDraft> guests(List<BookingAiImportResponse.GuestInfo> source) {
        List<TeamDocumentImportDraft.GuestDraft> result = new ArrayList<>();
        for (BookingAiImportResponse.GuestInfo item : safe(source)) {
            result.add(new TeamDocumentImportDraft.GuestDraft(
                    item.indexNo(), item.name(), item.certificateNo(), item.gender(), item.birthDate(), item.age(), item.phone(),
                    guestType(item.customerType(), item.leader()), item.roomGroup(), item.roomingRemark(), Boolean.TRUE.equals(item.leader()),
                    item.idCardValid(), firstText(item.personalRemark(), item.groupRemark())
            ));
        }
        return result;
    }

    private List<TeamDocumentImportDraft.OrderPriceDraft> orderPriceDrafts(
            BookingAiImportResponse.PriceInfo price, int guestCount
    ) {
        if (price == null) return List.of();
        List<TeamDocumentImportDraft.OrderPriceDraft> lines = new ArrayList<>();
        addPrice(lines, "adult", "成人", price.adultPrice(), guestCount);
        addPrice(lines, "child", "儿童", price.childPrice(), 0);
        addPrice(lines, "senior", "老人", price.seniorPrice(), 0);
        return lines;
    }

    private void addPrice(List<TeamDocumentImportDraft.OrderPriceDraft> target, String type, String name, String value, int quantity) {
        BigDecimal amount = decimal(value);
        if (amount != null) target.add(new TeamDocumentImportDraft.OrderPriceDraft(type, name, amount, BigDecimal.valueOf(quantity)));
    }

    private String trafficText(BookingAiImportResponse.TravelInfo value, boolean outbound) {
        if (value == null) return null;
        return outbound
                ? join(value.outboundOriginCity(), value.outboundArrivalCity(), value.outboundTrafficNo(), value.outboundDepartureTime(), value.outboundArrivalTime())
                : join(value.returnDepartureCity(), value.returnDestinationCity(), value.returnTrafficNo(), value.returnDepartureTime(), value.returnArrivalTime());
    }

    private String join(String... values) {
        return java.util.Arrays.stream(values).filter(StringUtils::hasText).collect(java.util.stream.Collectors.joining(" "));
    }

    private String title(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.lines()
                .map(String::trim)
                .filter(item -> item.length() >= 4)
                .map(this::validTeamName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String allowedDocumentType(String value) {
        return StringUtils.hasText(value)
                && List.of("ground_confirmation", "product_itinerary", "quotation", "guest_list", "mixed").contains(value)
                ? value
                : "mixed";
    }

    private String arrangementType(String value) {
        if (!StringUtils.hasText(value)) return null;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "scenic", "scenic_spot", "景区", "景点", "风景区" -> "scenic";
            case "hotel", "住宿", "酒店" -> "hotel";
            case "vehicle", "交通车", "车辆", "用车", "旅游车" -> "vehicle";
            case "traffic", "airport", "station", "大交通", "机场", "车站" -> "traffic";
            case "shopping", "购物", "购物店" -> "shopping";
            case "ground_agent", "地接", "地接社" -> "ground_agent";
            case "other", "其他", "杂费" -> "other";
            case "optional", "自费" -> "optional";
            case "extra_fee", "附加费" -> "extra_fee";
            case "meal", "restaurant", "餐厅", "餐饮", "用餐" -> "meal";
            default -> null;
        };
    }

    private String guestType(String value, Boolean leader) {
        if (Boolean.TRUE.equals(leader)) return "escort";
        // 本地规则未识别到游客类型时为 null；普通游客按成人草稿处理，不能因此中断整个导入任务。
        return StringUtils.hasText(value)
                && List.of("adult", "child", "child_no_bed", "senior", "escort").contains(value)
                ? value
                : "adult";
    }

    private BigDecimal decimal(String value) {
        if (!StringUtils.hasText(value)) return null;
        try { return new BigDecimal(value.replaceAll("[^0-9.]", "")); } catch (RuntimeException ignored) { return null; }
    }

    private boolean isDate(String value) {
        if (!StringUtils.hasText(value)) return false;
        try { LocalDate.parse(value); return true; } catch (RuntimeException ignored) { return false; }
    }

    private Double decimalNumber(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return null;
        if (value.isNumber()) return value.asDouble();
        if (!value.isTextual() || !StringUtils.hasText(value.asText())) return null;
        try {
            return Double.parseDouble(value.asText().trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<String> mergeEvidence(List<String> localEvidence, List<String> aiEvidence) {
        List<String> merged = new ArrayList<>(safe(localEvidence));
        merged.addAll(safe(aiEvidence));
        return merged;
    }

    private Integer positiveInteger(JsonNode value) {
        return value != null && value.canConvertToInt() && value.asInt() > 0 ? value.asInt() : null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && StringUtils.hasText(value.asText()) && !"null".equalsIgnoreCase(value.asText()) ? value.asText().trim() : null;
    }

    private List<String> stringArray(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(item -> { if (item.isTextual() && StringUtils.hasText(item.asText())) values.add(item.asText().trim()); });
        return values;
    }

    private String firstText(String... values) {
        for (String value : values) if (StringUtils.hasText(value)) return value.trim();
        return null;
    }

    private String blankToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private List<String> distinct(List<String> values) { return values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList(); }
    private <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }
    private enum DescriptionField {
        CONTENT,
        FEE_INCLUDED,
        FEE_EXCLUDED,
        CHILD_POLICY,
        SHOPPING_ARRANGEMENT,
        OPTIONAL_ITEMS,
        GIFT_ITEMS,
        ATTENTION_ITEMS,
        WARM_REMINDER
    }
    private record SectionHeader(DescriptionField field, String inlineContent) {}
    private record DayHeader(int dayNo, String title, int contentStart, int start) {}
    private record PartyCandidate(
            String companyName,
            String contactName,
            String phoneToken,
            String direction,
            String businessRole,
            String evidence,
            Double confidence
    ) {}
    private record ResolvedCustomer(BookingAiImportResponse.CustomerInfo customer, String warning) {}
}
