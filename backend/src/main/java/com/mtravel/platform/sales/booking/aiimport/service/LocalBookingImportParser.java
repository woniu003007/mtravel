package com.mtravel.platform.sales.booking.aiimport.service;

import com.mtravel.platform.sales.booking.aiimport.dto.BookingAiImportResponse;
import java.util.ArrayList;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 确认单本地规则解析器。
 *
 * <p>规则解析用于两类场景：没有模型 Key 时提供可用草稿；有模型时对关键字段做兜底和校验。这里只做
 * 明显字段提取，不把不确定内容强行写入正式字段。</p>
 */
@Component
public class LocalBookingImportParser {

    private static final Pattern FLIGHT_LINE = Pattern.compile(
            "(\\d{4})年(\\d{1,2})月(\\d{1,2})日\\s*([\\p{IsHan}A-Za-z]+?)\\s*[-—→到至]\\s*([\\p{IsHan}A-Za-z]+?)\\s*([A-Z]{2}(?:\\d{3}\\s+\\d{1,2}|\\d{3,5})|[GDCKZT]\\d{1,5})\\s*[（(](\\d{2})(\\d{2})\\s*[-—]\\s*(\\d{2})(\\d{2})[）)]"
    );
    private static final Pattern SHORT_FLIGHT_LINE = Pattern.compile(
            "(\\d{1,2})[./-](\\d{1,2})\\s*([\\p{IsHan}A-Za-z]+?)\\s*[-—→到至]\\s*([\\p{IsHan}A-Za-z]+?)\\s*([A-Z]{2}(?:\\d{3}\\s+\\d{1,2}|\\d{3,5})|[GDCKZT]\\d{1,5})\\s*[（(]?\\s*(\\d{1,2}:?\\d{2})\\s*[-—]\\s*(\\d{1,2}:?\\d{2})[）)]?"
    );
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern GUIDE = Pattern.compile("导游[:：\\s]*([\\p{IsHan}A-Za-z]{2,8})\\s*(1[3-9]\\d{9})?");
    private static final Pattern CUSTOMER = Pattern.compile("(?:客户|ATTN|联系人)[:：\\s]*([\\p{IsHan}A-Za-z0-9（）()·]{2,20})\\s*([\\p{IsHan}A-Za-z]{2,8})?\\s*(1[3-9]\\d{9})?");
    private static final Pattern PRICE = Pattern.compile("(成人|儿童|老人|单房差)\\s*[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*元");
    private static final Pattern COST_TABLE_LINE = Pattern.compile("^(房|车|门|餐|导游)\\s+(.+)\\s+(\\d+(?:\\.\\d+)?)\\s*元\\s*$");
    private static final Pattern COST_TOTAL_LINE = Pattern.compile("^总计[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*元(?:\\s*(.*))?$");
    private static final Pattern GUEST_LINE = Pattern.compile(
            "^\\s*(\\d{1,3})\\s+([\\p{IsHan}A-Za-z·]{2,20})\\s+(\\d{1,3})\\s+(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})\\s+(\\d{17}[0-9Xx])(?:\\s+(1[3-9]\\d{9}))?(?:\\s+([^\\s]+))?(?:\\s+(.+))?\\s*$"
    );
    private static final Pattern COMPACT_GUEST_LINE = Pattern.compile(
            "^\\s*(领队|\\d{1,3})\\s+([\\p{IsHan}A-Za-z·]{2,20})\\s+(\\d{17}[0-9Xx])(?:\\s+(1[3-9]\\d{9}))?(?:\\s+(\\d{1,3}))?(?:\\s+([^\\s]+))?(?:\\s+(.+))?\\s*$"
    );
    private static final Pattern GROUPED_COMPACT_GUEST_LINE = Pattern.compile(
            "^\\s*\\d{1,3}\\s+(\\d{1,3})\\s+([\\p{IsHan}A-Za-z·]{2,20})\\s+(\\d{17}[0-9Xx])(?:\\s+(1[3-9]\\d{9}))?(?:\\s+(\\d{1,3}))?(?:\\s+([^\\s]+))?(?:\\s+(.+))?\\s*$"
    );
    private static final Pattern INDEX_ONLY = Pattern.compile("^(\\d{1,3})$");
    private static final Pattern AGE_ONLY = Pattern.compile("^\\d{1,3}$");
    private static final Pattern ID_CARD_ONLY = Pattern.compile("^\\d{17}[0-9Xx]$");
    private static final Pattern DATE_ONLY = Pattern.compile("^\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}$");

    private final IdCardValidator idCardValidator;

    public LocalBookingImportParser(IdCardValidator idCardValidator) {
        this.idCardValidator = idCardValidator;
    }

    /**
     * 从原始文本解析确认单草稿。
     */
    public BookingAiImportResponse parse(String text, String sourceType) {
        String normalized = normalize(text);
        List<String> warnings = new ArrayList<>();
        List<String> evidence = evidenceLines(normalized);
        BookingAiImportResponse.TravelInfo travelInfo = travelInfo(normalized);
        BookingAiImportResponse.GuideInfo guideInfo = guideInfo(normalized);
        BookingAiImportResponse.CustomerInfo customerInfo = customerInfo(normalized);
        BookingAiImportResponse.PriceInfo priceInfo = priceInfo(normalized);
        List<BookingAiImportResponse.GuestInfo> guests = guests(normalized);
        BookingAiImportResponse.AdditionalInfo additionalInfo = additionalInfo(normalized, guests);
        if (guests.stream().anyMatch(item -> !Boolean.TRUE.equals(item.idCardValid()))) {
            warnings.add("游客名单存在证件异常，请人工确认");
        }
        BookingAiImportResponse.GuestSummary guestSummary = guestSummary(guests);
        if (guestSummary.suspectedMissingCount() > 0) {
            warnings.add("游客名单疑似存在漏识别，请核对原始确认单");
        }
        if (travelInfo.outboundStationName() == null || travelInfo.returnStationName() == null) {
            warnings.add("接送站未自动匹配，请人工选择");
        }
        BookingAiImportResponse.ModuleScores moduleScores = moduleScores(
                travelInfo,
                guideInfo,
                customerInfo,
                priceInfo,
                additionalInfo,
                guestSummary
        );
        double confidence = averageScore(moduleScores);
        return new BookingAiImportResponse(
                StringUtils.hasText(sourceType) ? sourceType : "text",
                confidence,
                List.copyOf(warnings),
                travelInfo,
                guideInfo,
                customerInfo,
                priceInfo,
                additionalInfo,
                guests,
                moduleScores,
                guestSummary,
                evidence
        );
    }

    private BookingAiImportResponse.TravelInfo travelInfo(String text) {
        Matcher matcher = FLIGHT_LINE.matcher(text);
        List<FlightLine> flights = new ArrayList<>();
        while (matcher.find()) {
            flights.add(new FlightLine(
                    date(matcher.group(1), matcher.group(2), matcher.group(3)),
                    matcher.group(4),
                    matcher.group(5),
                    normalizeTrafficNo(matcher.group(6)),
                    time(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(7), matcher.group(8)),
                    time(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(9), matcher.group(10))
            ));
        }
        Matcher shortMatcher = SHORT_FLIGHT_LINE.matcher(text);
        String defaultYear = String.valueOf(Year.now().getValue());
        while (shortMatcher.find()) {
            flights.add(new FlightLine(
                    date(defaultYear, shortMatcher.group(1), shortMatcher.group(2)),
                    shortMatcher.group(3),
                    shortMatcher.group(4),
                    normalizeTrafficNo(shortMatcher.group(5)),
                    time(defaultYear, shortMatcher.group(1), shortMatcher.group(2), normalizeTimePart(shortMatcher.group(6), 0), normalizeTimePart(shortMatcher.group(6), 1)),
                    time(defaultYear, shortMatcher.group(1), shortMatcher.group(2), normalizeTimePart(shortMatcher.group(7), 0), normalizeTimePart(shortMatcher.group(7), 1))
            ));
        }
        flights = flights.stream()
                .sorted(Comparator.comparing(FlightLine::date)
                        .thenComparing(FlightLine::departureTime)
                        .thenComparing(FlightLine::trafficNo))
                .toList();
        FlightLine outbound = flights.isEmpty() ? null : flights.get(0);
        FlightLine returnTrip = flights.size() < 2 ? null : flights.get(flights.size() - 1);
        List<String> warnings = new ArrayList<>();
        if (outbound == null) {
            warnings.add("未识别到来程航班/车次");
        }
        if (returnTrip == null) {
            warnings.add("未识别到返程航班/车次");
        }
        if (outbound != null || returnTrip != null) {
            warnings.add("已按城市填入接送站，具体机场/车站请人工确认");
        }
        return new BookingAiImportResponse.TravelInfo(
                outbound == null ? null : outbound.date(),
                outbound == null ? null : outbound.originCity(),
                outbound == null ? null : outbound.destinationCity(),
                outbound == null ? null : outbound.destinationCity(),
                outbound == null ? null : outbound.trafficNo(),
                outbound == null ? null : outbound.departureTime(),
                outbound == null ? null : outbound.arrivalTime(),
                returnTrip == null ? null : returnTrip.originCity(),
                returnTrip == null ? null : returnTrip.originCity(),
                returnTrip == null ? null : returnTrip.destinationCity(),
                returnTrip == null ? null : returnTrip.trafficNo(),
                returnTrip == null ? null : returnTrip.departureTime(),
                returnTrip == null ? null : returnTrip.arrivalTime(),
                List.copyOf(warnings)
        );
    }

    private BookingAiImportResponse.GuideInfo guideInfo(String text) {
        Matcher matcher = GUIDE.matcher(text);
        if (matcher.find()) {
            return new BookingAiImportResponse.GuideInfo(
                    matcher.group(1),
                    matcher.group(2),
                    null,
                    extractSentence(text, "导游"),
                    List.of()
            );
        }
        return new BookingAiImportResponse.GuideInfo(null, null, null, extractSentence(text, "导游"), List.of("导游姓名需人工确认"));
    }

    private BookingAiImportResponse.CustomerInfo customerInfo(String text) {
        Matcher matcher = CUSTOMER.matcher(text);
        if (matcher.find()) {
            String phone = matcher.group(3);
            if (phone == null) {
                Matcher phoneMatcher = PHONE.matcher(matcher.group(0));
                phone = phoneMatcher.find() ? phoneMatcher.group() : null;
            }
            return new BookingAiImportResponse.CustomerInfo(
                    matcher.group(1),
                    matcher.group(2),
                    phone,
                    null,
                    extractSentence(text, "客户"),
                    List.of()
            );
        }
        Matcher phoneMatcher = PHONE.matcher(text);
        return new BookingAiImportResponse.CustomerInfo(null, null, phoneMatcher.find() ? phoneMatcher.group() : null, null, null, List.of("客户单位需人工选择"));
    }

    private BookingAiImportResponse.PriceInfo priceInfo(String text) {
        Matcher matcher = PRICE.matcher(text);
        String adult = null;
        String child = null;
        String senior = null;
        String singleRoom = null;
        List<String> lines = new ArrayList<>();
        while (matcher.find()) {
            String type = matcher.group(1);
            String amount = matcher.group(2);
            lines.add(type + " " + amount + "元");
            if ("成人".equals(type)) {
                adult = amount;
            } else if ("儿童".equals(type)) {
                child = amount;
            } else if ("老人".equals(type)) {
                senior = amount;
            } else if ("单房差".equals(type)) {
                singleRoom = amount;
            }
        }
        CostTableParseResult costTable = costTablePriceLines(text);
        lines.addAll(costTable.lines());
        return new BookingAiImportResponse.PriceInfo(adult, child, senior, singleRoom, costTable.totalAmount(), lines, List.of());
    }

    /**
     * 提取确认单“接待标准及注意事项”里的费用表。
     *
     * <p>房、车、门、餐、导游通常是地接成本或接待标准说明，不等同于客户应收单价，
     * 因此只作为 priceLines 草稿回填到前端费用说明，不能自动生成订单价格行。</p>
     */
    private CostTableParseResult costTablePriceLines(String text) {
        List<String> lines = new ArrayList<>();
        String totalAmount = null;
        for (String rawLine : text.split("\\R")) {
            String line = normalizeCostLine(rawLine);
            Matcher costMatcher = COST_TABLE_LINE.matcher(line);
            if (costMatcher.matches()) {
                String item = costMatcher.group(1);
                String standard = costMatcher.group(2).trim();
                String amount = costMatcher.group(3);
                lines.add("%s：%s；金额：%s元".formatted(
                        item,
                        standard,
                        amount
                ));
                continue;
            }
            Matcher totalMatcher = COST_TOTAL_LINE.matcher(line);
            if (totalMatcher.matches()) {
                totalAmount = totalMatcher.group(1);
                String remark = normalizeBlank(totalMatcher.group(2));
                lines.add("总计：%s元%s".formatted(
                        totalAmount,
                        StringUtils.hasText(remark) ? "；备注：" + remark : ""
                ));
            }
        }
        return new CostTableParseResult(totalAmount, lines);
    }

    private String normalizeCostLine(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .replace("总计 ：", "总计：");
    }

    private BookingAiImportResponse.AdditionalInfo additionalInfo(String text, List<BookingAiImportResponse.GuestInfo> guests) {
        String rooming = extractRoomingText(text);
        String leaderNote = guests.stream()
                .filter(guest -> Boolean.TRUE.equals(guest.leader()) || Boolean.TRUE.equals(guest.suspectedLeader()))
                .map(BookingAiImportResponse.GuestInfo::leaderSourceText)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(extractLeaderText(text));
        return new BookingAiImportResponse.AdditionalInfo(
                extractSentence(text, "附加说明"),
                extractSentence(text, "接待标准"),
                rooming,
                leaderNote,
                List.of()
        );
    }

    private List<BookingAiImportResponse.GuestInfo> guests(String text) {
        List<ParsedGuest> parsedGuests = new ArrayList<>();
        String[] lines = text.split("\\R");
        String lastMergedPhone = null;
        String lastMergedRooming = null;
        String lastMergedRemark = null;
        for (String line : lines) {
            Matcher matcher = GUEST_LINE.matcher(line.trim());
            if (matcher.matches()) {
                String explicitPhone = normalizeBlank(matcher.group(6));
                String explicitRooming = normalizeBlank(matcher.group(7));
                String explicitRemark = normalizeBlank(matcher.group(8));
                if (StringUtils.hasText(explicitPhone)) {
                    lastMergedPhone = explicitPhone;
                }
                if (StringUtils.hasText(explicitRooming)) {
                    lastMergedRooming = explicitRooming;
                }
                if (StringUtils.hasText(explicitPhone) || StringUtils.hasText(explicitRooming) || matcher.group(8) != null) {
                    lastMergedRemark = explicitRemark;
                }
                parsedGuests.add(new ParsedGuest(
                        Integer.parseInt(matcher.group(1)),
                        matcher.group(2),
                        matcher.group(5),
                        matcher.group(3),
                        matcher.group(4),
                        StringUtils.hasText(explicitPhone) ? explicitPhone : lastMergedPhone,
                        StringUtils.hasText(explicitRooming) ? explicitRooming : lastMergedRooming,
                        matcher.group(8) != null ? explicitRemark : lastMergedRemark,
                        StringUtils.hasText(explicitPhone) || StringUtils.hasText(explicitRooming),
                        false
                ));
            }
            Matcher compactMatcher = COMPACT_GUEST_LINE.matcher(line.trim());
            if (compactMatcher.matches()) {
                String indexText = compactMatcher.group(1);
                boolean leader = "领队".equals(indexText);
                String explicitRooming = normalizeBlank(compactMatcher.group(6));
                if (StringUtils.hasText(explicitRooming)) {
                    lastMergedRooming = explicitRooming;
                }
                parsedGuests.add(new ParsedGuest(
                        leader ? 0 : Integer.parseInt(indexText),
                        compactMatcher.group(2),
                        compactMatcher.group(3),
                        compactMatcher.group(5),
                        null,
                        compactMatcher.group(4),
                        StringUtils.hasText(explicitRooming) ? explicitRooming : lastMergedRooming,
                        compactMatcher.group(7),
                        StringUtils.hasText(explicitRooming),
                        leader
                ));
            }
            Matcher groupedCompactMatcher = GROUPED_COMPACT_GUEST_LINE.matcher(line.trim());
            if (groupedCompactMatcher.matches()) {
                String explicitRooming = normalizeBlank(groupedCompactMatcher.group(6));
                if (StringUtils.hasText(explicitRooming)) {
                    lastMergedRooming = explicitRooming;
                }
                parsedGuests.add(new ParsedGuest(
                        Integer.parseInt(groupedCompactMatcher.group(1)),
                        groupedCompactMatcher.group(2),
                        groupedCompactMatcher.group(3),
                        groupedCompactMatcher.group(5),
                        null,
                        groupedCompactMatcher.group(4),
                        StringUtils.hasText(explicitRooming) ? explicitRooming : lastMergedRooming,
                        groupedCompactMatcher.group(7),
                        StringUtils.hasText(explicitRooming),
                        false
                ));
            }
        }
        List<BookingAiImportResponse.GuestInfo> guests = buildGuestsWithRoomGroups(parsedGuests);
        guests.addAll(columnarGuests(lines, guests.size()));
        return guests;
    }

    private List<BookingAiImportResponse.GuestInfo> buildGuestsWithRoomGroups(List<ParsedGuest> parsedGuests) {
        List<BookingAiImportResponse.GuestInfo> guests = new ArrayList<>();
        int roomIndex = 0;
        int index = 0;
        while (index < parsedGuests.size()) {
            ParsedGuest firstGuest = parsedGuests.get(index);
            String rooming = normalizeBlank(firstGuest.rooming());
            if (!StringUtils.hasText(rooming)) {
                guests.add(buildGuest(
                        firstGuest.indexNo(),
                        firstGuest.name(),
                        firstGuest.certificateNo(),
                        firstGuest.aiAge(),
                        firstGuest.aiBirthDate(),
                        firstGuest.phone(),
                        null,
                        null,
                        firstGuest.remark(),
                        firstGuest.forcedLeader()
                ));
                index += 1;
                continue;
            }
            int blockEnd = index + 1;
            while (blockEnd < parsedGuests.size() && !parsedGuests.get(blockEnd).newRoomingBlock()) {
                blockEnd += 1;
            }
            List<ParsedGuest> block = parsedGuests.subList(index, blockEnd);
            int roomCount = estimateRoomCount(rooming, block.size());
            for (int offset = 0; offset < block.size(); offset += 1) {
                ParsedGuest parsedGuest = block.get(offset);
                int roomOffset = roomCount <= 1 ? 0 : Math.min(roomCount - 1, offset * roomCount / block.size());
                String roomGroup = (roomIndex + roomOffset + 1) + "房";
                guests.add(buildGuest(
                        parsedGuest.indexNo(),
                        parsedGuest.name(),
                        parsedGuest.certificateNo(),
                        parsedGuest.aiAge(),
                        parsedGuest.aiBirthDate(),
                        parsedGuest.phone(),
                        roomGroup,
                        rooming,
                        parsedGuest.remark(),
                        parsedGuest.forcedLeader()
                ));
            }
            roomIndex += roomCount;
            index = blockEnd;
        }
        return guests;
    }

    /**
     * 根据分房备注估算一个原始分房块需要拆成几间。
     *
     * <p>确认单里的“分房”通常是备注，不一定是严格房号。能识别到“2间/3间”时按明确间数拆；
     * “家庭房”但人数超过三人时保守拆成两间，避免把四五个人静默塞进同一个同房组。</p>
     */
    private int estimateRoomCount(String rooming, int guestCount) {
        if (!StringUtils.hasText(rooming) || guestCount <= 0) {
            return 0;
        }
        Matcher matcher = Pattern.compile("(\\d+)\\s*间").matcher(rooming);
        if (matcher.find()) {
            return Math.max(1, Math.min(Integer.parseInt(matcher.group(1)), guestCount));
        }
        if (rooming.contains("家庭房") && guestCount > 3) {
            return 2;
        }
        return 1;
    }

    private List<BookingAiImportResponse.GuestInfo> columnarGuests(String[] lines, int existingCount) {
        List<BookingAiImportResponse.GuestInfo> guests = new ArrayList<>();
        for (int index = 0; index < lines.length; index += 1) {
            String value = lines[index].trim();
            Matcher indexMatcher = INDEX_ONLY.matcher(value);
            if (!indexMatcher.matches()) {
                continue;
            }
            if (index + 4 >= lines.length) {
                continue;
            }
            String name = lines[index + 1].trim();
            String age = lines[index + 2].trim();
            String birthDate = lines[index + 3].trim();
            String certificateNo = lines[index + 4].trim();
            if (!isLikelyGuestName(name) || !AGE_ONLY.matcher(age).matches()
                    || !DATE_ONLY.matcher(birthDate).matches() || !ID_CARD_ONLY.matcher(certificateNo).matches()) {
                continue;
            }
            int cursor = index + 5;
            String phone = null;
            String roomGroup = null;
            List<String> remarkParts = new ArrayList<>();
            while (cursor < lines.length) {
                String next = lines[cursor].trim();
                if (INDEX_ONLY.matcher(next).matches()) {
                    break;
                }
                if (PHONE.matcher(next).matches()) {
                    phone = next;
                } else if (isRoomingValue(next)) {
                    roomGroup = next;
                    remarkParts.add(next);
                } else if (StringUtils.hasText(next)) {
                    remarkParts.add(next);
                }
                cursor += 1;
            }
            guests.add(buildGuest(
                    Integer.parseInt(indexMatcher.group(1)),
                    name,
                    certificateNo,
                    age,
                    birthDate,
                    phone,
                    roomGroup,
                    roomGroup,
                    String.join("；", remarkParts),
                    false
            ));
        }
        if (existingCount > 0) {
            return List.of();
        }
        return guests;
    }

    private BookingAiImportResponse.GuestInfo buildGuest(
            Integer indexNo,
            String name,
            String certificateNo,
            String aiAge,
            String aiBirthDate,
            String phone,
            String roomGroup,
            String roomingRemark,
            String remark,
            boolean forcedLeader
    ) {
        IdCardValidationResult validation = idCardValidator.validate(certificateNo);
        List<String> warnings = new ArrayList<>(validation.warnings());
        String normalizedBirthDate = normalizeDate(aiBirthDate);
        if (validation.birthDate() != null && StringUtils.hasText(normalizedBirthDate)
                && !validation.birthDate().equals(normalizedBirthDate)) {
            warnings.add("出生日期与证件不一致");
        }
        boolean leader = forcedLeader || containsLeaderKeyword(remark);
        boolean suspectedLeader = !leader && containsSuspectedLeaderKeyword(remark);
        String cleanedRoomGroup = normalizeBlank(roomGroup);
        String cleanedRoomingRemark = normalizeBlank(roomingRemark);
        String cleanedRemark = normalizeBlank(remark);
        return new BookingAiImportResponse.GuestInfo(
                indexNo,
                name,
                null,
                certificateNo == null ? null : certificateNo.toUpperCase(),
                validation.gender(),
                validation.birthDate() == null ? normalizedBirthDate : validation.birthDate(),
                validation.age() == null ? parseInt(aiAge).orElse(null) : validation.age(),
                phone,
                null,
                null,
                null,
                null,
                null,
                cleanedRoomGroup,
                cleanedRoomingRemark,
                leader,
                suspectedLeader,
                leader || suspectedLeader ? cleanedRemark : null,
                cleanedRemark,
                cleanedRemark,
                validation.valid(),
                List.copyOf(warnings)
        );
    }

    private BookingAiImportResponse.GuestSummary guestSummary(List<BookingAiImportResponse.GuestInfo> guests) {
        int invalidIdCardCount = (int) guests.stream().filter(item -> !Boolean.TRUE.equals(item.idCardValid())).count();
        int missingRequiredCount = (int) guests.stream()
                .filter(item -> !StringUtils.hasText(item.name()) || !StringUtils.hasText(item.certificateNo()))
                .count();
        int suspectedMissingCount = guests.isEmpty() ? 1 : 0;
        return new BookingAiImportResponse.GuestSummary(
                guests.size(),
                invalidIdCardCount,
                missingRequiredCount,
                suspectedMissingCount
        );
    }

    private BookingAiImportResponse.ModuleScores moduleScores(
            BookingAiImportResponse.TravelInfo travelInfo,
            BookingAiImportResponse.GuideInfo guideInfo,
            BookingAiImportResponse.CustomerInfo customerInfo,
            BookingAiImportResponse.PriceInfo priceInfo,
            BookingAiImportResponse.AdditionalInfo additionalInfo,
            BookingAiImportResponse.GuestSummary guestSummary
    ) {
        return new BookingAiImportResponse.ModuleScores(
                score(travelInfo.outboundTrafficNo(), travelInfo.returnTrafficNo()),
                score(guideInfo.guideName(), guideInfo.receptionRequirement()),
                score(customerInfo.customerName(), customerInfo.contactName(), customerInfo.contactPhone()),
                score(priceInfo.adultPrice(), priceInfo.childPrice(), priceInfo.singleRoomDifference(), priceInfo.priceLines().isEmpty() ? null : "lines"),
                score(additionalInfo.notes(), additionalInfo.receptionStandard(), additionalInfo.roomingNote(), additionalInfo.leaderNote()),
                guestSummary.guestCount() > 0 && guestSummary.missingRequiredCount() == 0 ? 1.0 : 0.0
        );
    }

    private double score(String... values) {
        int total = values.length;
        long hit = java.util.Arrays.stream(values).filter(StringUtils::hasText).count();
        return total == 0 ? 0 : (double) hit / total;
    }

    private double averageScore(BookingAiImportResponse.ModuleScores scores) {
        return (scores.travelScore() + scores.guideScore() + scores.customerScore()
                + scores.priceScore() + scores.additionalScore() + scores.guestListScore()) / 6.0;
    }

    private List<String> evidenceLines(String text) {
        return text.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> line.contains("航班") || line.matches(".*\\d{4}年\\d{1,2}月\\d{1,2}日.*")
                        || line.contains("导游") || line.contains("客户") || line.contains("报价")
                        || line.contains("分房") || line.contains("领队"))
                .limit(20)
                .toList();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\u00A0', ' ').trim();
    }

    private String date(String year, String month, String day) {
        return "%s-%02d-%02d".formatted(year, Integer.parseInt(month), Integer.parseInt(day));
    }

    private String time(String year, String month, String day, String hour, String minute) {
        return "%s-%02d-%02dT%s:%s:00".formatted(year, Integer.parseInt(month), Integer.parseInt(day), hour, minute);
    }

    private String normalizeTimePart(String value, int index) {
        String digits = value == null ? "" : value.replace(":", "");
        if (digits.length() == 3) {
            digits = "0" + digits;
        }
        if (digits.length() < 4) {
            return index == 0 ? "00" : "00";
        }
        return index == 0 ? digits.substring(0, 2) : digits.substring(2, 4);
    }

    private String normalizeTrafficNo(String value) {
        return value == null ? null : value.replaceAll("\\s+", "");
    }

    private String normalizeDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String[] parts = value.replace('/', '-').split("-");
        if (parts.length != 3) {
            return value;
        }
        return "%s-%02d-%02d".formatted(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    private Optional<Integer> parseInt(String value) {
        if (!StringUtils.hasText(value) || !AGE_ONLY.matcher(value).matches()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(value));
    }

    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean isLikelyGuestName(String value) {
        return StringUtils.hasText(value) && value.matches("[\\p{IsHan}A-Za-z·]{2,20}");
    }

    private boolean isRoomingValue(String value) {
        return StringUtils.hasText(value)
                && (value.contains("房") || value.contains("间") || value.contains("床") || value.contains("拼住"));
    }

    private String extractSentence(String text, String keyword) {
        return text.lines()
                .map(String::trim)
                .filter(line -> line.contains(keyword))
                .findFirst()
                .orElse(null);
    }

    private String extractRoomingText(String text) {
        return text.lines()
                .map(String::trim)
                .filter(line -> line.contains("住一间") || line.contains("同住") || line.contains("分房") || line.contains("家庭房"))
                .findFirst()
                .orElse(null);
    }

    private String extractLeaderText(String text) {
        return text.lines()
                .map(String::trim)
                .filter(line -> line.contains("领队") || line.contains("全陪") || line.contains("带队老师") || line.contains("负责人"))
                .findFirst()
                .orElse(null);
    }

    private boolean containsLeaderKeyword(String value) {
        return StringUtils.hasText(value) && (value.contains("领队") || value.contains("全陪"));
    }

    private boolean containsSuspectedLeaderKeyword(String value) {
        return StringUtils.hasText(value) && (value.contains("带队") || value.contains("负责人") || value.contains("联系人"));
    }

    private record FlightLine(
            String date,
            String originCity,
            String destinationCity,
            String trafficNo,
            String departureTime,
            String arrivalTime
    ) {
    }

    private record ParsedGuest(
            Integer indexNo,
            String name,
            String certificateNo,
            String aiAge,
            String aiBirthDate,
            String phone,
            String rooming,
            String remark,
            boolean newRoomingBlock,
            boolean forcedLeader
    ) {
    }

    private record CostTableParseResult(
            String totalAmount,
            List<String> lines
    ) {
    }
}
