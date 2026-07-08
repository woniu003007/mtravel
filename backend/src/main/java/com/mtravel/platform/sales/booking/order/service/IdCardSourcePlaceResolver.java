package com.mtravel.platform.sales.booking.order.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * 身份证客源地解析器。
 *
 * <p>老系统订单管理页的“客源地”按第一位游客身份证前六位行政区划码生成。该解析器只负责
 * 从身份证号解析省、市、区县，不参与身份证校验位判断；身份证合法性仍由 IdCardValidator 负责。</p>
 */
final class IdCardSourcePlaceResolver {

    private static final String REGION_DATA_PATH = "data/id-card-region-map.json";
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[0-9X]");
    private static final Map<String, SourcePlace> REGION_MAP = loadRegionMap();

    private IdCardSourcePlaceResolver() {
    }

    /**
     * 按身份证前六位解析客源地。
     *
     * @param rawIdCard 游客身份证号
     * @return 可识别时返回客源地；证件格式不符或区划码未知时返回 null
     */
    static SourcePlace resolve(String rawIdCard) {
        String idCard = rawIdCard == null ? "" : rawIdCard.trim().toUpperCase();
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return null;
        }
        SourcePlace place = REGION_MAP.get(idCard.substring(0, 6));
        if (place == null) {
            return null;
        }
        return new SourcePlace(
                clean(place.province()),
                normalizeCity(clean(place.province()), clean(place.city())),
                clean(place.district())
        );
    }

    private static Map<String, SourcePlace> loadRegionMap() {
        try (InputStream inputStream = IdCardSourcePlaceResolver.class
                .getClassLoader()
                .getResourceAsStream(REGION_DATA_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("身份证行政区划数据不存在：" + REGION_DATA_PATH);
            }
            return new ObjectMapper().readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("身份证行政区划数据读取失败：" + REGION_DATA_PATH, exception);
        }
    }

    private static String normalizeCity(String province, String city) {
        if (Objects.equals(province, city)) {
            return null;
        }
        return city;
    }

    private static String clean(String value) {
        String result = value == null ? "" : value.trim();
        return StringUtils.hasText(result) ? result : null;
    }

    /**
     * 身份证行政区划解析结果。
     *
     * @param province 省份或直辖市
     * @param city 地级市；直辖市与省份同名时为空，避免列表展示重复
     * @param district 区县
     */
    record SourcePlace(String province, String city, String district) {
    }
}
