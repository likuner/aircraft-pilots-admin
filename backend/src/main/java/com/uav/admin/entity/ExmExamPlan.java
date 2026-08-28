package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考试计划实体
 */
@Data
@TableName("exm_exam_plan")
public class ExmExamPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String planCode;
    private String planName;
    private String examType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String region;
    private String description;
    private String status;
    private Long creatorId;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
