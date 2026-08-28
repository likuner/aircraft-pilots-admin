package com.uav.admin.dto;

import lombok.Data;

/**
 * 报名入参
 */
@Data
public class RegistrationCreateDTO {

    /** 场次 ID */
    private Long sessionId;
    /** 考生档案 ID */
    private Long studentProfileId;
    /** 所在机构（可空） */
    private Long institutionId;
}
