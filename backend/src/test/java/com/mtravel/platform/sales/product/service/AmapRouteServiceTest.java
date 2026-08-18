package com.mtravel.platform.sales.product.service;

import com.mtravel.platform.common.map.service.AmapMapService;
import com.mtravel.platform.common.map.service.AmapWebServiceClient;
import com.mtravel.platform.sales.product.dto.AmapRouteCalculateRequest;
import com.mtravel.platform.sales.product.dto.AmapRoutePointRequest;
import com.mtravel.platform.sales.product.dto.AmapStaticMapRequest;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 高德路线服务测试。
 *
 * <p>路书点位的“到下一站距离”和“车程”要按相邻两点真实驾车路线计算，不能把整条路线平均分配。</p>
 */
class AmapRouteServiceTest {

    @Test
    void calculateDrivingRouteShouldReturnExactDistanceForEachAdjacentPair() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        AmapRouteService service = service(restTemplate);

        server.expect(requestTo(containsString("origin=120.1,30.1")))
                .andExpect(requestTo(containsString("destination=120.2,30.2")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(routeResponse(1000, 600), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("origin=120.2,30.2")))
                .andExpect(requestTo(containsString("destination=120.3,30.3")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(routeResponse(3000, 1200), MediaType.APPLICATION_JSON));

        var response = service.calculateDrivingRoute(1L, new AmapRouteCalculateRequest(List.of(
                new AmapRoutePointRequest("120.1", "30.1"),
                new AmapRoutePointRequest("120.2", "30.2"),
                new AmapRoutePointRequest("120.3", "30.3")
        )));

        assertThat(response.totalDistanceMeters()).isEqualTo(4000);
        assertThat(response.totalDurationSeconds()).isEqualTo(1800);
        assertThat(response.segments()).hasSize(2);
        assertThat(response.segments().get(0).distanceMeters()).isEqualTo(1000);
        assertThat(response.segments().get(0).durationSeconds()).isEqualTo(600);
        assertThat(response.segments().get(1).distanceMeters()).isEqualTo(3000);
        assertThat(response.segments().get(1).durationSeconds()).isEqualTo(1200);
        server.verify();
    }

    @Test
    void searchTipsShouldReuseCachedResultForSameKeywordAndCity() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AmapRouteService service = service(restTemplate);

        server.expect(requestTo(containsString("keywords=%E8%A5%BF%E6%B9%96")))
                .andExpect(requestTo(containsString("city=%E6%9D%AD%E5%B7%9E")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(tipsResponse(), MediaType.APPLICATION_JSON));

        var first = service.searchTips(1L, "西湖", "杭州");
        var second = service.searchTips(1L, " 西湖 ", "杭州");

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        assertThat(second.get(0).name()).isEqualTo("杭州西湖风景名胜区");
        server.verify();
    }

    @Test
    void calculateDrivingRouteShouldReuseCachedSegmentForSameAdjacentPair() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AmapRouteService service = service(restTemplate);

        server.expect(requestTo(containsString("origin=120.1,30.1")))
                .andExpect(requestTo(containsString("destination=120.2,30.2")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(routeResponse(1000, 600), MediaType.APPLICATION_JSON));

        AmapRouteCalculateRequest request = new AmapRouteCalculateRequest(List.of(
                new AmapRoutePointRequest("120.1", "30.1"),
                new AmapRoutePointRequest("120.2", "30.2")
        ));
        var first = service.calculateDrivingRoute(1L, request);
        var second = service.calculateDrivingRoute(1L, request);

        assertThat(first.totalDistanceMeters()).isEqualTo(1000);
        assertThat(second.totalDistanceMeters()).isEqualTo(1000);
        server.verify();
    }

    @Test
    void jsConfigShouldKeepLegacySalesResponseShape() {
        AmapRouteService service = service(new RestTemplate());

        var response = service.jsConfig(1L);

        assertThat(response.key()).isEqualTo("test-key");
        assertThat(response.securityJsCode()).isEqualTo("test-key");
    }

    @Test
    void staticMapShouldKeepReturningBase64DataUrl() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AmapRouteService service = service(restTemplate);

        server.expect(requestTo(containsString("/v3/staticmap")))
                .andExpect(requestTo(containsString("size=700*360")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(new byte[]{1, 2, 3}, MediaType.IMAGE_PNG));

        String response = service.staticMapImage(1L, new AmapStaticMapRequest(
                List.of(new AmapRoutePointRequest("120.1", "30.1"))
        ));

        assertThat(response).isEqualTo("data:image/png;base64,AQID");
        server.verify();
    }

    private AmapRouteService service(RestTemplate restTemplate) {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        SystemConfigMapper configMapper = mock(SystemConfigMapper.class);
        SystemConfigEntity config = new SystemConfigEntity();
        config.setConfigValue("test-key");
        when(builder.build()).thenReturn(restTemplate);
        when(configMapper.selectOne(any())).thenReturn(config);
        AmapWebServiceClient client = new AmapWebServiceClient(builder, configMapper, "");
        return new AmapRouteService(new AmapMapService(client), client);
    }

    private String routeResponse(int distance, int duration) {
        return """
                {
                  "status": "1",
                  "route": {
                    "paths": [
                      {
                        "distance": "%d",
                        "duration": "%d",
                        "steps": [
                          { "distance": "%d", "duration": "%d" }
                        ]
                      }
                    ]
                  }
                }
                """.formatted(distance, duration, distance, duration);
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
}
