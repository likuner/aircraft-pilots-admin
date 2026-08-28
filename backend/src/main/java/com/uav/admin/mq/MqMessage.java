package com.uav.admin.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * MQ 统一消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型：EXAM_RESULT / CERT_ISSUE / NOTICE / INST_NOTIFY */
    private String eventType;
    /** 业务负载 */
    private Map<String, Object> payload;
    /** 幂等追踪号 */
    private String traceId;
    /** 时间戳 */
    private String timestamp;
}
