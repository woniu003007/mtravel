package com.mtravel.platform.sales.booking.aiimport.service;

import com.mtravel.platform.system.config.service.AiConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 百炼模型客户端 Spring 装配测试。
 *
 * <p>该客户端同时保留运行时构造器和测试构造器，运行时构造器必须能被 Spring 明确识别，
 * 否则后端启动会因为找不到默认构造器而失败。</p>
 */
class BailianAiModelClientWiringTest {

    @Test
    void shouldCreateBailianAiModelClientInSpringContext() {
        new ApplicationContextRunner()
                .withBean(AiConfigService.class, () -> mock(AiConfigService.class))
                .withBean(RestTemplateBuilder.class, RestTemplateBuilder::new)
                .withBean(BailianAiModelClient.class)
                .run(context -> assertThat(context).hasSingleBean(BailianAiModelClient.class));
    }

    @Test
    void recognizeImageOrDocumentShouldCallBailianVisionApiWithDataUrl() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        AiConfigService aiConfigService = mock(AiConfigService.class);
        when(builder.build()).thenReturn(restTemplate);
        when(aiConfigService.rawValue(anyLong(), eq(AiConfigService.BAILIAN_API_KEY))).thenReturn("sk-test");
        when(aiConfigService.rawValue(anyLong(), eq(AiConfigService.BAILIAN_VISION_MODEL))).thenReturn("qwen-vl-ocr-latest");
        BailianAiModelClient client = new BailianAiModelClient("", "qwen-plus", "", aiConfigService, builder);

        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(content().string(containsString("\"model\":\"qwen-vl-ocr-latest\"")))
                .andExpect(content().string(containsString("data:image/png;base64,AQID")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "游客名单：张三 210204198206214832"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.recognizeImageOrDocument(1L, "png", new byte[]{1, 2, 3});

        assertThat(response).contains("游客名单：张三 210204198206214832");
        server.verify();
    }
}
