package com.mtravel.platform.purchase.relation.tickettemplate.service;

import com.mtravel.platform.common.BizException;
import com.mtravel.platform.purchase.relation.tickettemplate.dto.TicketTemplateFieldSaveRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTemplateFillModeValidatorTest {

    @Test
    void validateShouldAllowSequenceConstantAndKeepOriginalWithoutSystemField() {
        TicketTemplateFillModeValidator validator = new TicketTemplateFillModeValidator();

        assertThatCode(() -> validator.validate(new TicketTemplateFieldSaveRequest(
                "序号", 1, null, "sequence", null, true, 0
        ))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(new TicketTemplateFieldSaveRequest(
                "票型名称", 2, null, "constant", "成人", true, 1
        ))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(new TicketTemplateFieldSaveRequest(
                "备注", 4, null, "keep_original", null, false, 2
        ))).doesNotThrowAnyException();
    }

    @Test
    void validateShouldRequireSystemFieldWhenFillModeIsTouristField() {
        TicketTemplateFillModeValidator validator = new TicketTemplateFillModeValidator();

        assertThatThrownBy(() -> validator.validate(new TicketTemplateFieldSaveRequest(
                "姓名", 2, null, "tourist_field", null, true, 0
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("游客字段填充必须选择系统字段");
    }
}
