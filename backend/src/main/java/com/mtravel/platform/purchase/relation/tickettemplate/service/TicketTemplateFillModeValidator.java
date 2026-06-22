package com.mtravel.platform.purchase.relation.tickettemplate.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateFieldSaveRequest;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TicketTemplateFillMode;
import com.mtravel.platform.purchase.relation.tickettemplate.enums.TouristSystemField;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 游客名单模板列填充方式校验器。
 *
 * <p>把填充方式相关判断集中在这里，避免 Service 保存逻辑里散落字符串判断。</p>
 */
@Component
public class TicketTemplateFillModeValidator {

    /**
     * 校验单个字段映射的填充规则。
     *
     * @param field 字段映射请求
     */
    public void validate(TicketTemplateFieldSaveRequest field) {
        TicketTemplateFillMode fillMode = TicketTemplateFillMode.fromValue(defaultFillMode(field.fillMode()))
                .orElseThrow(() -> new BizException("填充方式不合法：" + field.fillMode()));
        if (fillMode == TicketTemplateFillMode.TOURIST_FIELD) {
            if (!StringUtils.hasText(field.systemField())) {
                throw new BizException("游客字段填充必须选择系统字段");
            }
            TouristSystemField.fromValue(field.systemField())
                    .orElseThrow(() -> new BizException("系统字段不合法：" + field.systemField()));
        }
        if (fillMode == TicketTemplateFillMode.CONSTANT && !StringUtils.hasText(field.fixedValue())) {
            throw new BizException("固定值填充必须填写固定值");
        }
    }

    /** 未传填充方式时按旧数据兼容为游客字段。 */
    public String defaultFillMode(String fillMode) {
        return StringUtils.hasText(fillMode) ? fillMode : TicketTemplateFillMode.TOURIST_FIELD.value();
    }
}
