package com.mtravel.platform.sales.booking.aiimport.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 身份证校验测试。
 *
 * <p>AI 只负责提取证件号，合法性、出生日期、性别和年龄必须由程序判断，避免模型误判后写入游客名单。</p>
 */
class IdCardValidatorTest {

    @Test
    void validateShouldAcceptLegalIdCardAndDeriveProfileFields() {
        IdCardValidator validator = new IdCardValidator();

        IdCardValidationResult result = validator.validate("210204198206214832", 2026);

        assertThat(result.valid()).isTrue();
        assertThat(result.birthDate()).isEqualTo("1982-06-21");
        assertThat(result.gender()).isEqualTo("男");
        assertThat(result.age()).isEqualTo(44);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void validateShouldRejectInvalidChecksum() {
        IdCardValidator validator = new IdCardValidator();

        IdCardValidationResult result = validator.validate("210204198206214831", 2026);

        assertThat(result.valid()).isFalse();
        assertThat(result.warnings()).contains("身份证校验位不正确");
    }

    @Test
    void validateShouldRejectImpossibleBirthDate() {
        IdCardValidator validator = new IdCardValidator();

        IdCardValidationResult result = validator.validate("110105198213014516", 2026);

        assertThat(result.valid()).isFalse();
        assertThat(result.warnings()).contains("身份证出生日期不合法");
    }
}
