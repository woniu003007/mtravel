package com.mtravel.platform.config;

import com.mtravel.platform.auth.config.SecurityProperties;
import com.mtravel.platform.agent.security.config.AgentServiceTokenBootstrapProperties;
import com.mtravel.platform.tenant.TenantProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        SecurityProperties.class,
        TenantProperties.class,
        AgentServiceTokenBootstrapProperties.class
})
public class AppConfig {
}
