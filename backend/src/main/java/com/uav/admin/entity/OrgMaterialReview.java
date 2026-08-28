package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 材料审查记录实体
 */
@Data
@TableName("org_material_review")
public class OrgMaterialReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private Long reviewerId;
    private String result;
    private String comment;
    private LocalDateTime reviewTime;
    private Integer reviewStep;
}
