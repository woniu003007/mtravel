package com.mtravel.platform.common.map.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 公共高德地点服务测试。
 *
 * <p>覆盖地点搜索缓存、JS 配置回退、逆地理编码和第三方失败转换，保证公共接口不会泄漏底层异常。</p>
 */
class AmapMapServiceTest {

    @Test
    void searchTipsShouldNormalizeInputAndReuseCache() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AmapMapService service = service(restTemplate, "test-key");

        server.expect(requestTo(containsString("keywords=%E8%A5%BF%E6%B9%96")))
                .andExpect(requestTo(containsString("city=%E6%9D%AD%E5%B7%9E")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(tipsResponse(), MediaType.APPLICATION_JSON));

        var first = service.searchTips(1L, " 西湖 ", " 杭州 ");
        var second = service.searchTips(1L, "西湖", "杭州");

        assertThat(first).hasSize(1);
        assertThat(first.getFirst().name()).isEqualTo("杭州西湖风景名胜区");
        assertThat(first.getFirst().longitude()).isEqualTo("120.14353");
        assertThat(second).isEqualTo(first);
        server.verify();
    }

    @Test
    void jsConfigShouldFallBackToWebServiceKey() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(new RestTemplate());
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        AmapWebServiceClient client = new AmapWebServiceClient(builder, mapper, " env-web-key ");

        var response = new AmapMapService(client).jsConfig(1L);

        assertThat(response.key()).isEqualTo("env-web-key");
        assertThat(response.securityJsCode()).isNull();
    }

    @Test
    void jsConfigShouldRejectMissingAllKeys() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(new RestTemplate());
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        AmapWebServiceClient client = new AmapWebServiceClient(builder, mapper, "");

        assertThatThrownBy(() -> new AmapMapService(client).jsConfig(1L))
                .isInstanceOf(BizException.class)
                .hasMessage("未配置高德地图 Web服务 Key");
    }

    @Test
    void reverseGeocodeShouldReturnFormattedAddressAndReuseCache() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AmapMapService service = service(restTemplate, "test-key");

        server.expect(requestTo(containsString("location=120.14353,30.23689")))
                .andExpect(requestTo(containsString("extensions=base")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(regeoResponse(), MediaType.APPLICATION_JSON));

        var first = service.reverseGeocode(1L, new BigDecimal("120.1435300"), new BigDecimal("30.2368900"));
        var second = service.reverseGeocode(1L, new BigDecimal("120.14353"), new BigDecimal("30.23689"));

        assertThat(first.address()).isEqualTo("浙江省杭州市西湖区龙井路1号");
        assertThat(second).isEqualTo(first);
        server.verify();
    }

    @Test
    void reverseGeocodeShouldRejectOutOfRangeCoordinatesBeforeCallingAmap() {
        AmapMapService service = service(new RestTemplate(), "test-key");

        assertThatThrownBy(() -> service.reverseGeocode(1L, new BigDecimal("181"), BigDecimal.ZERO))
                .isInstanceOf(BizException.class)
                .hasMessage("经度必须在-180到180之间");
        assertThatThrownBy(() -> service.reverseGeocode(1L, BigDecimal.ZERO, new BigDecimal("-91")))
                .isInstanceOf(BizException.class)
                .hasMessage("纬度必须在-90到90之间");
    }

    @Test
    void searchTipsShouldExposeControlledMessageWhenAmapIsUnavailable() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AmapMapService service = service(restTemplate, "test-key");

        server.expect(requestTo(containsString("/v3/assistant/inputtips")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> service.searchTips(1L, "西湖", "杭州"))
                .isInstanceOf(BizException.class)
                .hasMessage("高德地图服务暂时不可用");
        server.verify();
    }

    @Test
    void searchTipsShouldExposeAmapBusinessFailure() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AmapMapService service = service(restTemplate, "test-key");

        server.expect(requestTo(containsString("/v3/assistant/inputtips")))
                .andRespond(withSuccess("{\"status\":\"0\",\"info\":\"INVALID_USER_KEY\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.searchTips(1L, "西湖", "杭州"))
                .isInstanceOf(BizException.class)
                .hasMessage("高德地图调用失败：INVALID_USER_KEY");
        server.verify();
    }

    private AmapMapService service(RestTemplate restTemplate, String webServiceKey) {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        if (webServiceKey == null) {
            when(mapper.selectOne(any())).thenReturn(null);
        } else {
            SystemConfigEntity config = new SystemConfigEntity();
            config.setConfigValue(webServiceKey);
            when(mapper.selectOne(any())).thenReturn(config);
        }
        return new AmapMapService(new AmapWebServiceClient(builder, mapper, ""));
    }

    private String tipsResponse() {
        return """
                {
                  "status": "1",
                  "tips": [
                    {
                      "name": "杭州西湖风景名胜区",
                      "district": "浙江省杭州市西湖区",
                      "address": "龙井路1号",
                      "location": "120.14353,30.23689"
                    }
                  ]
                }
                """;
    }

    private String regeoResponse() {
        return """
                {
                  "status": "1",
                  "regeocode": {
                    "formatted_address": "浙江省杭州市西湖区龙井路1号"
                  }
                }
                """;
    }
}
