package com.mtravel.platform.purchase.groundagent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mtravel.platform.common.TenantSoftDeleteEntity;
import java.math.BigDecimal;

/** 地接外委实体，对应 ground_agents 表。 */
@TableName("ground_agents")
public class GroundAgentEntity extends TenantSoftDeleteEntity {
    @TableField("ground_agent_name") private String groundAgentName;
    @TableField("city") private String city;
    @TableField("contact_name") private String contactName;
    @TableField("contact_phone") private String contactPhone;
    @TableField("task_name") private String taskName;
    @TableField("itinerary_requirement") private String itineraryRequirement;
    @TableField("total_budget") private BigDecimal totalBudget;
    @TableField("confirmation_attachment_id") private Long confirmationAttachmentId;
    @TableField("confirmation_file_url") private String confirmationFileUrl;
    @TableField("status") private String status;
    public String getGroundAgentName(){return groundAgentName;} public void setGroundAgentName(String groundAgentName){this.groundAgentName=groundAgentName;}
    public String getCity(){return city;} public void setCity(String city){this.city=city;}
    public String getContactName(){return contactName;} public void setContactName(String contactName){this.contactName=contactName;}
    public String getContactPhone(){return contactPhone;} public void setContactPhone(String contactPhone){this.contactPhone=contactPhone;}
    public String getTaskName(){return taskName;} public void setTaskName(String taskName){this.taskName=taskName;}
    public String getItineraryRequirement(){return itineraryRequirement;} public void setItineraryRequirement(String itineraryRequirement){this.itineraryRequirement=itineraryRequirement;}
    public BigDecimal getTotalBudget(){return totalBudget;} public void setTotalBudget(BigDecimal totalBudget){this.totalBudget=totalBudget;}
    public Long getConfirmationAttachmentId(){return confirmationAttachmentId;} public void setConfirmationAttachmentId(Long confirmationAttachmentId){this.confirmationAttachmentId=confirmationAttachmentId;}
    public String getConfirmationFileUrl(){return confirmationFileUrl;} public void setConfirmationFileUrl(String confirmationFileUrl){this.confirmationFileUrl=confirmationFileUrl;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
