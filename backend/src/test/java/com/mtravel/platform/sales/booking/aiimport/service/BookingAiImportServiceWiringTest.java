package com.mtravel.platform.sales.booking.aiimport.service;

import com.mtravel.platform.common.attachment.service.CommonAttachmentService;
import com.mtravel.platform.common.knowledge.service.AliyunOcrClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 确认单 AI 辅助录入服务 Spring 装配测试。
 *
 * <p>该服务保留了测试构造器，运行时构造器必须能被 Spring 明确识别。否则本地单测能过，
 * 但后端重启会因为找不到默认构造器而启动失败。</p>
 */
class BookingAiImportServiceWiringTest {

    @Test
    void shouldCreateBookingAiImportServiceInSpringContext() {
        new ApplicationContextRunner()
                .withBean(LocalBookingImportParser.class, () -> new LocalBookingImportParser(new IdCardValidator()))
                .withBean(AiModelClient.class, () -> (tenantId, sourceText) -> Optional.empty())
                .withBean(CommonAttachmentService.class, () -> mock(CommonAttachmentService.class))
                .withBean(AliyunOcrClient.class, () -> mock(AliyunOcrClient.class))
                .withBean(BookingImportAttachmentTextExtractor.class)
                .withBean(BookingAiImportService.class)
                .run(context -> assertThat(context).hasSingleBean(BookingAiImportService.class));
    }
}
