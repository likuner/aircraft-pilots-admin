package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩记录实体
 */
@Data
@TableName("exm_score")
public class ExmScore implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long registrationId;
    private Long sessionId;
    private Long studentUserId;
    private String examType;
    private BigDecimal score;
    private String passStatus;
    private String status;
    private Long examinerId;
    private LocalDateTime entryTime;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String auditRemark;
    private String remark;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
