package com.uav.admin.dto;

import lombok.Data;

/**
 * 合格证申请入参
 */
@Data
public class CertApplyCreateDTO {

    /** 报名单 ID（一单一生效申请） */
    private Long registrationId;
    /** 对应合格成绩 ID */
    private Long scoreId;
    /** 证类别 */
    private String certificateType;
}
