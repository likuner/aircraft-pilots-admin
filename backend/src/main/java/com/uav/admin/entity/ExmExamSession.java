package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考试场次实体
 */
@Data
@TableName("exm_exam_session")
public class ExmExamSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String sessionCode;
    private String sessionName;
    private String examType;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private Long roomId;
    private Long examinerId;
    private BigDecimal fullScore;
    private BigDecimal passScore;
    private Integer capacity;
    private Integer enrolledCount;
    private String status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
