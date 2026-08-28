package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 合格证申请实体
 */
@Data
@TableName("cer_certificate_apply")
public class CerCertificateApply implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String applyNo;
    private Long registrationId;
    private Long scoreId;
    private Long studentUserId;
    private String certificateType;
    private LocalDateTime applyTime;
    private String status;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
