package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实地核查任务实体
 */
@Data
@TableName("org_site_inspection")
public class OrgSiteInspection implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private Long institutionId;
    private Long inspectorId;
    private LocalDate inspectionDate;
    private String address;
    private String checklist;
    private String result;
    private String summary;
    private String reportUrl;
    private String status;
    private LocalDateTime assignTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
