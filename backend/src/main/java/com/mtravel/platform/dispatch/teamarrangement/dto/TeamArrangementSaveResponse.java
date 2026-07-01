package com.mtravel.platform.dispatch.teamarrangement.dto;

import java.util.List;

/**
 * 正式团队安排保存结果。
 *
 * @param id 首条保存记录 ID
 * @param ids 本次保存产生的全部安排 ID；多订单均摊会返回多条
 */
public record TeamArrangementSaveResponse(Long id, List<Long> ids) {
}
