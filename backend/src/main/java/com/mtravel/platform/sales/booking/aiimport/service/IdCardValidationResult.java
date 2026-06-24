package com.mtravel.platform.sales.booking.aiimport.service;

import java.util.List;

/**
 * 身份证程序校验结果。
 *
 * <p>AI 识别出的游客证件号必须经过本对象承载的程序校验结果后再展示给前端。</p>
 */
public record IdCardValidationResult(
        boolean valid,
        String birthDate,
        String gender,
        Integer age,
        List<String> warnings
) {
}
