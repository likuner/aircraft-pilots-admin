package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 训练机构实体
 */
@Data
@TableName("org_institution")
public class OrgInstitution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String instCode;
    private String instName;
    private String creditCode;
    private String orgType;
    private String legalPerson;
    private BigDecimal registeredCapital;
    private String address;
    private String contactName;
    private String contactPhone;
    private String email;
    private String businessScope;
    private String qualificationStatus;
    private Integer status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
