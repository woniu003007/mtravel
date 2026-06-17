package com.mtravel.platform.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录安全配置。
 *
 * <p>配置来源为 application.yml 或环境变量，真实密码和 JWT 密钥不能写死到生产环境配置中。</p>
 */
@ConfigurationProperties(prefix = "mtravel.security")
public class SecurityProperties {

    /** JWT 签名密钥，生产环境必须通过环境变量配置强密钥。 */
    private String jwtSecret = "dev-only-change-me-please-use-a-strong-secret-key-32bytes";

    /** 访问 token 有效分钟数。 */
    private long accessTokenMinutes = 120;

    /** 默认无操作退出分钟数，租户级配置会优先生效。 */
    private long idleTimeoutMinutes = 120;

    /** 历史 demo 登录账号配置，数据库用户登录接入后不再作为认证依据。 */
    private String demoUsername = "admin";

    /** 历史 demo 登录密码配置，数据库用户登录接入后不再作为认证依据。 */
    private String demoPassword = "123456";

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getIdleTimeoutMinutes() {
        return idleTimeoutMinutes;
    }

    public void setIdleTimeoutMinutes(long idleTimeoutMinutes) {
        this.idleTimeoutMinutes = idleTimeoutMinutes;
    }

    public String getDemoUsername() {
        return demoUsername;
    }

    public void setDemoUsername(String demoUsername) {
        this.demoUsername = demoUsername;
    }

    public String getDemoPassword() {
        return demoPassword;
    }

    public void setDemoPassword(String demoPassword) {
        this.demoPassword = demoPassword;
    }
}
