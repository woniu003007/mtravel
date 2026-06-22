package com.mtravel.platform.purchase.relation.tickettemplate.dto;

import com.mtravel.platform.purchase.relation.tickettemplate.enums.TicketTemplateFillMode;
import java.util.Arrays;
import java.util.List;

/**
 * 游客名单模板填充方式选项返回对象。
 */
public record TicketTemplateFillModeResponse(String value, String label) {
    /** 返回前端可选填充方式。 */
    public static List<TicketTemplateFillModeResponse> all() {
        return Arrays.stream(TicketTemplateFillMode.values())
                .map(item -> new TicketTemplateFillModeResponse(item.value(), item.label()))
                .toList();
    }
}
