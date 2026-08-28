package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 机构认证申请实体
 */
@Data
@TableName("org_certification_apply")
public class OrgCertificationApply implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String applyNo;
    private Long institutionId;
    private String applyType;
    private String category;
    private Integer currentStep;
    private LocalDateTime applyTime;
    private String status;
    private Long submittedBy;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
