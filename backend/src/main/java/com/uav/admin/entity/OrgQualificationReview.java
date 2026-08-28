package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资质评定记录实体
 */
@Data
@TableName("org_qualification_review")
public class OrgQualificationReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private Long reviewerId;
    private BigDecimal evaluationScore;
    private String suggestion;
    private String result;
    private LocalDateTime reviewTime;
}
