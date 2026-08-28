package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 合格证审核记录实体
 */
@Data
@TableName("cer_certificate_audit")
public class CerCertificateAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private String auditType;
    private Long auditorId;
    private String action;
    private String comment;
    private LocalDateTime auditTime;
}
