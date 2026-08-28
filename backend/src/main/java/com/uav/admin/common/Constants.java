package com.uav.admin.common;

/**
 * 常量定义
 */
public interface Constants {

    /** Redis key 前缀 */
    String CAPTCHA_PREFIX = "uav:captcha:";
    String TOKEN_BLACKLIST_PREFIX = "uav:token:blacklist:";
    String REFRESH_PREFIX = "uav:refresh:";
    String SESSION_CAPACITY_PREFIX = "uav:exam:session:";
    String BATCH_CAPACITY_PREFIX = "uav:exam:batch:";
    String CAPACITY_SUFFIX = ":capacity";
    String USER_INFO_PREFIX = "uav:user:info:";
    String MENU_PREFIX = "uav:menu:";
    String CERT_NO_SEQ = "uav:cert:no:seq";
    String QUALIFICATION_NO_SEQ = "uav:qual:no:seq";
    String LOCK_PREFIX = "uav:lock:";

    /** RabbitMQ 交换机/队列 */
    String DIRECT_EXCHANGE = "uav.exchange.direct";
    String FANOUT_EXCHANGE = "uav.exchange.fanout";
    String DLX_EXCHANGE = "uav.exchange.dlx";
    String EXAM_RESULT_QUEUE = "uav.queue.exam.result";
    String CERT_ISSUE_QUEUE = "uav.queue.cert.issue";
    String NOTICE_BROADCAST_QUEUE = "uav.queue.notice.broadcast";
    String INST_NOTIFY_QUEUE = "uav.queue.inst.notify";
    String DLX_QUEUE = "uav.queue.dlx";
    String EXAM_RESULT_ROUTING = "exam.result";
    String CERT_ISSUE_ROUTING = "cert.issue";
    String INST_NOTIFY_ROUTING = "inst.notify";

    /** 认证请求头 */
    String HEADER_AUTHORIZATION = "Authorization";
    String TOKEN_PREFIX = "Bearer ";

    /** 角色编码 */
    String ROLE_ADMIN = "ADMIN";
    String ROLE_EXAMINER = "EXAMINER";
    String ROLE_INSTITUTION_ADMIN = "INSTITUTION_ADMIN";
    String ROLE_STUDENT = "STUDENT";

    /** 全权限通配 */
    String ALL_PERMISSION = "*:*:*";
}
