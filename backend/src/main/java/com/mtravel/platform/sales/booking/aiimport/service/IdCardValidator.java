package com.mtravel.platform.sales.booking.aiimport.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 中国居民身份证校验器。
 *
 * <p>用于游客名单导入预览阶段。校验内容包括 18 位格式、出生日期和校验位，并从证件号反推
 * 出生日期、性别和年龄。该判断必须由程序完成，不能依赖大模型。</p>
 */
@Component
public class IdCardValidator {

    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 按当前年份校验身份证并推导游客资料。
     *
     * @param rawIdCard 用户录入或 AI 识别出的证件号
     * @return 校验结果
     */
    public IdCardValidationResult validate(String rawIdCard) {
        return validate(rawIdCard, LocalDate.now(ZoneId.of("Asia/Shanghai")).getYear());
    }

    /**
     * 按指定年份校验身份证，测试可固定年份避免时间漂移。
     */
    public IdCardValidationResult validate(String rawIdCard, int currentYear) {
        List<String> warnings = new ArrayList<>();
        String idCard = normalize(rawIdCard);
        if (!StringUtils.hasText(idCard)) {
            warnings.add("身份证号为空");
            return new IdCardValidationResult(false, null, null, null, warnings);
        }
        if (!idCard.matches("\\d{17}[0-9X]")) {
            warnings.add("身份证格式不正确");
            return new IdCardValidationResult(false, null, null, null, warnings);
        }

        LocalDate birthDate = parseBirthDate(idCard, warnings);
        String gender = ((idCard.charAt(16) - '0') % 2 == 1) ? "男" : "女";
        Integer age = birthDate == null ? null : Math.max(0, currentYear - birthDate.getYear());
        boolean checksumValid = checksumValid(idCard);
        if (!checksumValid) {
            warnings.add("身份证校验位不正确");
        }
        boolean valid = warnings.isEmpty();
        return new IdCardValidationResult(
                valid,
                birthDate == null ? null : birthDate.toString(),
                gender,
                age,
                List.copyOf(warnings)
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private LocalDate parseBirthDate(String idCard, List<String> warnings) {
        try {
            int year = Integer.parseInt(idCard.substring(6, 10));
            int month = Integer.parseInt(idCard.substring(10, 12));
            int day = Integer.parseInt(idCard.substring(12, 14));
            return LocalDate.of(year, month, day);
        } catch (DateTimeException | NumberFormatException ex) {
            warnings.add("身份证出生日期不合法");
            return null;
        }
    }

    private boolean checksumValid(String idCard) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHTS[i];
        }
        return CHECK_CODES[sum % 11] == idCard.charAt(17);
    }
}
