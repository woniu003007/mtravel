package com.mtravel.platform.dispatch.teamarrangement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;

/**
 * 正式团队安排分类流程状态实体。
 *
 * <p>该表只保存团队安排页每个分类的流程状态，例如住宿是否已完成或无需安排。
 * 资源成本、供应商确认和导游报账仍保存在正式团队安排成本表族中。</p>
 */
@TableName("dispatch_team_arrangement_section_statuses")
public class DispatchTeamArrangementSectionStatusEntity extends TenantSoftDeleteEntity {

    /** 所属团队 ID。 */
    @TableField("team_id")
    private Long teamId;

    /** 团队编号快照。 */
    @TableField("team_no")
    private String teamNo;

    /** 团队类型快照。 */
    @TableField("team_type")
    private String teamType;

    /** 安排分类类型。 */
    @TableField("arrangement_type")
    private String arrangementType;

    /** 分类流程状态：pending 未完成，none 无需，done 完成。 */
    @TableField("status")
    private String status;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamNo() {
        return teamNo;
    }

    public void setTeamNo(String teamNo) {
        this.teamNo = teamNo;
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public String getArrangementType() {
        return arrangementType;
    }

    public void setArrangementType(String arrangementType) {
        this.arrangementType = arrangementType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
