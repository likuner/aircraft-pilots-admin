package com.uav.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成绩录入入参
 */
@Data
public class ScoreCreateDTO {

    /** 报名单 ID */
    private Long registrationId;
    /** THEORY / PRACTICAL */
    private String examType;
    /** 分数 */
    private BigDecimal score;
    /** 备注 */
    private String remark;
}
