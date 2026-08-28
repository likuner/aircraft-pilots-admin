package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 机构资质证实体
 */
@Data
@TableName("org_qualification")
public class OrgQualification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String qualificationNo;
    private Long applyId;
    private Long institutionId;
    private String qualificationLevel;
    private String category;
    private LocalDate issueDate;
    private LocalDate validUntil;
    private String status;
    private Long issuerId;
    private String revokeReason;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
