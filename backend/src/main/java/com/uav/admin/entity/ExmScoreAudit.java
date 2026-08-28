package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 成绩审核流水实体
 */
@Data
@TableName("exm_score_audit")
public class ExmScoreAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scoreId;
    private Long auditorId;
    private String action;
    private String comment;
    private LocalDateTime auditTime;
}
