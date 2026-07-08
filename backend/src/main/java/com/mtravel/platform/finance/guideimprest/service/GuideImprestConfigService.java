package com.mtravel.platform.finance.guideimprest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtravel.platform.system.config.entity.SystemConfigEntity;
import com.mtravel.platform.system.config.mapper.SystemConfigMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 导游备用金配置服务。
 *
 * <p>当前维护公司规定加点率。配置保存在 system_configs，缺省按 70% 计算。</p>
 */
@Service
public class GuideImprestConfigService {

    /** 公司规定加点率配置键，按百分数保存，例如 70 表示 70%。 */
    public static final String GUIDE_IMPREST_COMPANY_MARKUP_RATE = "guide_imprest_company_markup_rate";

    private static final BigDecimal DEFAULT_COMPANY_MARKUP_RATE = new BigDecimal("70");

    private final SystemConfigMapper mapper;

    public GuideImprestConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    /** 查询当前租户导游备用金公司加点率，未配置或格式错误时使用 70%。 */
    public BigDecimal getCompanyMarkupRatePercent(Long tenantId) {
        SystemConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getTenantId, tenantId)
                .eq(SystemConfigEntity::getConfigKey, GUIDE_IMPREST_COMPANY_MARKUP_RATE));
        if (entity == null || !StringUtils.hasText(entity.getConfigValue())) {
            return DEFAULT_COMPANY_MARKUP_RATE;
        }
        try {
            return new BigDecimal(entity.getConfigValue().trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_COMPANY_MARKUP_RATE;
        }
    }
}
