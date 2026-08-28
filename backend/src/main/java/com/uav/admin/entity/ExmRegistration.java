package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报名记录实体
 */
@Data
@TableName("exm_registration")
public class ExmRegistration implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String registrationNo;
    private Long sessionId;
    private Long batchId;
    private Long studentUserId;
    private Long studentProfileId;
    private Long institutionId;
    private LocalDateTime applyTime;
    private String status;
    private String rejectReason;
    private LocalDateTime approveTime;
    private Long approverId;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
