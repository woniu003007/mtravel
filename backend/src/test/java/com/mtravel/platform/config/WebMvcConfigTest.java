package com.mtravel.platform.config;

import com.mtravel.platform.system.log.web.OperationLogInterceptor;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebMvcConfigTest {

    @TempDir
    Path uploadRoot;

    @Test
    void shouldMapAttachmentFilesToUploadRoot() {
        WebMvcConfig config = new WebMvcConfig(mock(OperationLogInterceptor.class), uploadRoot.toString());
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
                new StaticApplicationContext(),
                new MockServletContext()
        );

        config.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/attachments/**")).isTrue();
    }
}
