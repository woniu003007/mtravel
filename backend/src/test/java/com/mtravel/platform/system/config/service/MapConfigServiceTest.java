package com.mtravel.platform.system.config.service;

import com.mtravel.platform.system.config.dto.MapConfigUpdateRequest;
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
 * 高德地图配置测试。
 *
 * <p>高德 Key 保存在租户系统配置中，查询接口只返回脱敏值，避免页面截图或接口日志泄露完整密钥。</p>
 */
class MapConfigServiceTest {

    @Test
    void updateShouldPersistRawKeysButReturnMaskedValues() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        MapConfigService service = new MapConfigService(mapper);

        var response = service.updateMapConfig(1L, new MapConfigUpdateRequest(
                "amap-web-1234567890",
                "amap-js-abcdef123456",
                "sec-abcdef123456"
        ));

        ArgumentCaptor<SystemConfigEntity> captor = ArgumentCaptor.forClass(SystemConfigEntity.class);
        verify(mapper, org.mockito.Mockito.times(3)).upsert(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(entity -> {
                    assertThat(entity.getConfigKey()).isEqualTo(MapConfigService.AMAP_WEB_SERVICE_KEY);
                    assertThat(entity.getConfigValue()).isEqualTo("amap-web-1234567890");
                })
                .anySatisfy(entity -> {
                    assertThat(entity.getConfigKey()).isEqualTo(MapConfigService.AMAP_JS_KEY);
                    assertThat(entity.getConfigValue()).isEqualTo("amap-js-abcdef123456");
                });
        assertThat(response.webServiceKeyMasked()).isEqualTo("amap********7890");
        assertThat(response.jsKeyMasked()).isEqualTo("amap********3456");
    }

    @Test
    void getShouldMaskStoredMapKeys() {
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigValue("amap-web-1234567890");
        when(mapper.selectOne(any())).thenReturn(entity);
        MapConfigService service = new MapConfigService(mapper);

        assertThat(service.getMapConfig(1L).webServiceKeyMasked()).isEqualTo("amap********7890");
    }
}
