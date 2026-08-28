package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 认证申请材料实体
 */
@Data
@TableName("org_apply_material")
public class OrgApplyMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private String materialType;
    private String fileName;
    private String fileUrl;
    private Long uploadBy;
    private LocalDateTime uploadTime;
}
