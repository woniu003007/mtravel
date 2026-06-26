package com.mtravel.platform.sales.booking.order.dto;

import java.util.List;

/**
 * 游客名单 Excel 导入预览结果。
 *
 * @param guests 解析出的游客草稿，前端追加到当前订单后由保存订单接口统一落库。
 * @param importedCount 解析出的游客行数。
 * @param validCount 身份证校验通过的游客数。
 * @param invalidCount 身份证校验异常的游客数。
 * @param duplicateCount 文件内重复跳过数量。
 * @param warnings 导入过程中的人工核对提示。
 */
public record SalesBookingGuestImportPreviewResponse(
        List<SalesBookingOrderGuestResponse> guests,
        int importedCount,
        int validCount,
        int invalidCount,
        int duplicateCount,
        List<String> warnings
) {
}
