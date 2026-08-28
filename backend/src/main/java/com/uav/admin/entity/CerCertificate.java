package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合格证实体
 */
@Data
@TableName("cer_certificate")
public class CerCertificate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String certNo;
    private Long applyId;
    private Long studentUserId;
    private String certificateType;
    private LocalDate issueDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String status;
    private Long issuerId;
    private String issueOrg;
    private String remark;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
