package com.mtravel.platform.sales.team.dto;

/**
 * 销售团队页面展示名称格式化工具。
 *
 * <p>直接建团会为产品快照名称追加当前团号以规避产品名称唯一性冲突；该后缀属于内部快照标识，
 * 面向用户展示团队名称时应统一移除，避免列表、团队操作和拼团选择团期显示成“团队名称-团号”。</p>
 */
public final class SalesTeamDisplayNameFormatter {

    private SalesTeamDisplayNameFormatter() {
    }

    /**
     * 将产品快照名称转换为团队展示名称。
     *
     * @param productName 产品快照名称，可能包含直接建团追加的团号后缀
     * @param teamNo 当前团队团号
     * @return 面向页面展示的团队名称
     */
    public static String productDisplayName(String productName, String teamNo) {
        String cleanName = clean(productName);
        String cleanTeamNo = clean(teamNo);
        if (cleanName == null || cleanTeamNo == null) {
            return cleanName;
        }
        String suffix = "-" + cleanTeamNo;
        return cleanName.endsWith(suffix) ? cleanName.substring(0, cleanName.length() - suffix.length()) : cleanName;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
