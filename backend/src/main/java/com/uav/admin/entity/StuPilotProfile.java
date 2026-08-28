package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 驾驶员档案实体
 */
@Data
@TableName("stu_pilot_profile")
public class StuPilotProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String idCard;
    private Integer gender;
    private LocalDate birthDate;
    private String phone;
    private String pilotType;
    private String aircraftModel;
    private BigDecimal flyingHours;
    private Long institutionId;
    private String examCategory;
    private String education;
    private String emergencyContact;
    private String status;
    private String remark;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
