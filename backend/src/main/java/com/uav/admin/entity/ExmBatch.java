package com.uav.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 批次实体
 */
@Data
@TableName("exm_batch")
public class ExmBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long roomId;
    private String batchCode;
    private LocalDateTime batchTime;
    private Long invigilatorId;
    private Long examinerId;
    private Integer capacity;
    private Integer enrolledCount;
    private String status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
