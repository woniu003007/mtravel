package com.mtravel.platform.system.config.service;

import com.mtravel.platform.system.config.dto.AiConfigUpdateRequest;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 配置服务测试。
 *
 * <p>百炼 Key 可以保存到系统配置，但接口返回必须脱敏，避免前端、日志或截图泄露完整 Key。</p>
 */
class AiConfigServiceTest {

    @Test
    void updateAiConfigShouldPersistRawKeyButReturnMaskedKey() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        AiConfigService service = new AiConfigService(mapper);

        var response = service.updateAiConfig(1L, new AiConfigUpdateRequest(
                "aliyun_bailian",
                "sk-1234567890abcdef",
                "qwen-plus",
                "qwen-vl-ocr-latest"
        ));

        ArgumentCaptor<SystemConfigEntity> captor = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(mapper, org.mockito.Mockito.times(4)).upsert(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(entity -> {
                    assertThat(entity.getConfigKey()).isEqualTo(AiConfigService.BAILIAN_API_KEY);
                    assertThat(entity.getConfigValue()).isEqualTo("sk-1234567890abcdef");
                });
        assertThat(response.apiKeyMasked()).isEqualTo("sk-1********cdef");
    }

    @Test
    void getAiConfigShouldMaskStoredKey() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigValue("sk-abcdef1234567890");
        when(mapper.selectOne(any())).thenReturn(entity);
        AiConfigService service = new AiConfigService(mapper);

        assertThat(service.getAiConfig(1L).apiKeyMasked()).isEqualTo("sk-a********7890");
    }

    @Test
    void getAiConfigShouldUseOcrVisionModelByDefault() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        AiConfigService service = new AiConfigService(mapper);

        assertThat(service.getAiConfig(1L).visionModel()).isEqualTo("qwen-vl-ocr-latest");
    }
}
