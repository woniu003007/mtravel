package com.mtravel.platform.sales.ordertransfer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 团队操作页转团请求。
 *
 * @param orderIds 需要转团的订单 ID 列表
 * @param targetTeamId 已有目标团队 ID
 * @param createNewTeam 是否复制当前团队生成新团队后转入
 * @param lineType 新团队模式的团队类型
 * @param tourDate 新团队发团日期
 * @param allNum 新团队总位数
 * @param lineName 新团队名称或团号说明
 * @param memo 新团队备注
 * @param remark 转团备注
 */
public record SalesOrderTransferMoveRequest(
        @NotEmpty(message = "请选择订单") List<Long> orderIds,
        Long targetTeamId,
        boolean createNewTeam,
        @Size(max = 20, message = "团队类型不能超过20个字符") String lineType,
        LocalDate tourDate,
        Integer allNum,
        @Size(max = 120, message = "团队名称不能超过120个字符") String lineName,
        @Size(max = 1000, message = "新团队备注不能超过1000个字符") String memo,
        @Size(max = 1000, message = "转团备注不能超过1000个字符") String remark
) {
}
