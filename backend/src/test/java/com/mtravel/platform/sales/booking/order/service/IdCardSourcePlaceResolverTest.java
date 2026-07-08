package com.mtravel.platform.sales.booking.order.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 身份证客源地解析测试。
 *
 * <p>客源地用于订单管理列表展示，必须按身份证前六位行政区划码稳定解析。</p>
 */
class IdCardSourcePlaceResolverTest {

    @Test
    void resolveShouldReturnProvinceCityAndDistrictForNormalProvince() {
        var place = IdCardSourcePlaceResolver.resolve("330110198206214839");

        assertThat(place).isNotNull();
        assertThat(place.province()).isEqualTo("浙江省");
        assertThat(place.city()).isEqualTo("杭州市");
        assertThat(place.district()).isEqualTo("余杭区");
    }

    @Test
    void resolveShouldAvoidDuplicatedCityForMunicipality() {
        var place = IdCardSourcePlaceResolver.resolve("310101201010287414");

        assertThat(place).isNotNull();
        assertThat(place.province()).isEqualTo("上海市");
        assertThat(place.city()).isNull();
        assertThat(place.district()).isEqualTo("黄浦区");
    }

    @Test
    void resolveShouldReturnNullWhenIdCardCodeCannotBeRecognized() {
        assertThat(IdCardSourcePlaceResolver.resolve("990000201010287414")).isNull();
        assertThat(IdCardSourcePlaceResolver.resolve("护照123")).isNull();
    }
}
