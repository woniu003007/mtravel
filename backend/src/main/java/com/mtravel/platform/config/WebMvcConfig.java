package com.mtravel.platform.config;

import com.mtravel.platform.system.log.web.OperationLogInterceptor;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OperationLogInterceptor operationLogInterceptor;
    private final Path uploadRoot;

    public WebMvcConfig(
            OperationLogInterceptor operationLogInterceptor,
            @Value("${mtravel.upload.root:./data/uploads}") String uploadRoot
    ) {
        this.operationLogInterceptor = operationLogInterceptor;
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(operationLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
    }

    /**
     * 暴露本地附件目录给已登录用户访问。
     *
     * <p>合同文件预览由前端通过 requestClient 拉取 Blob，浏览器请求会携带 JWT，
     * 因此这里仅负责把 /attachments/** 映射到上传目录，不把合同文件改成公开接口。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/attachments/**")
                .addResourceLocations(uploadRoot.toUri().toString());
    }
}
