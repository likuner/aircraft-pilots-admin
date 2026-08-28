-- =====================================================================
-- 无人机驾驶员管理后台 数据库初始化脚本 01-schema.sql
-- 数据库: uav_admin  字符集: utf8mb4  引擎: InnoDB
-- 共 26 张表：系统域(7) + 考生档案(1) + 考试域(7) + 合格证域(4) + 机构认证域(7)
-- 约定：id BIGINT UNSIGNED 自增主键；create_time/update_time 通用审计字段；
--       业务主表带 deleted 逻辑删除(0正常/1删除)；ENUM 一律 VARCHAR 存英文值。
-- 脚本可重复执行（CREATE TABLE IF NOT EXISTS + 幂等插入）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `uav_admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `uav_admin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 1. 系统域（7 表）
-- =====================================================================

-- 1.1 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`         VARCHAR(50)     NOT NULL COMMENT '登录名',
  `password`         VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密文',
  `real_name`        VARCHAR(50)     DEFAULT NULL COMMENT '姓名',
  `phone`            VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
  `email`            VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
  `avatar`           VARCHAR(255)    DEFAULT NULL COMMENT '头像URL',
  `id_card`          VARCHAR(18)     DEFAULT NULL COMMENT '身份证号',
  `status`           TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `last_login_time`  DATETIME        DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip`    VARCHAR(50)     DEFAULT NULL COMMENT '最后登录IP',
  `remark`           VARCHAR(255)    DEFAULT NULL COMMENT '备注',
  `create_by`        BIGINT          DEFAULT NULL COMMENT '创建人',
  `update_by`        BIGINT          DEFAULT NULL COMMENT '更新人',
  `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 1.2 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code`   VARCHAR(50)     NOT NULL COMMENT '角色编码：ADMIN/EXAMINER/INSTITUTION_ADMIN/STUDENT',
  `role_name`   VARCHAR(50)     NOT NULL COMMENT '角色名称',
  `data_scope`  VARCHAR(20)     NOT NULL DEFAULT 'ALL' COMMENT '数据范围 ALL全部/SELF本人/INSTITUTION本机构',
  `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `description` VARCHAR(255)    DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 1.3 菜单/权限表（按钮权限复用）
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` BIGINT          NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为根',
  `menu_name` VARCHAR(50)     NOT NULL COMMENT '菜单名称',
  `menu_type` TINYINT         NOT NULL COMMENT '1目录 2菜单 3按钮',
  `path`      VARCHAR(200)    DEFAULT NULL COMMENT '前端路由路径',
  `component` VARCHAR(200)    DEFAULT NULL COMMENT '前端组件路径',
  `perms`     VARCHAR(100)    DEFAULT NULL COMMENT '权限标识，如 exam:plan:add',
  `icon`      VARCHAR(100)    DEFAULT NULL COMMENT '图标',
  `order_num` INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `visible`   TINYINT         NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
  `status`    TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单/权限表';

-- 1.4 用户-角色关联
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 1.5 角色-菜单关联
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- 1.6 操作日志
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`        BIGINT          DEFAULT NULL COMMENT '操作人ID',
  `username`       VARCHAR(50)     DEFAULT NULL COMMENT '操作人账号',
  `module`         VARCHAR(50)     DEFAULT NULL COMMENT '模块 exam/cert/institution等',
  `operation`      VARCHAR(100)    DEFAULT NULL COMMENT '操作描述',
  `method`         VARCHAR(200)    DEFAULT NULL COMMENT '控制器方法签名',
  `request_url`    VARCHAR(255)    DEFAULT NULL COMMENT '请求地址',
  `request_method` VARCHAR(10)     DEFAULT NULL COMMENT '请求方法',
  `params`         TEXT            DEFAULT NULL COMMENT '请求参数',
  `ip`             VARCHAR(50)     DEFAULT NULL COMMENT '来源IP',
  `status`         TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  `error_msg`      TEXT            DEFAULT NULL COMMENT '错误信息',
  `cost_time`      BIGINT          DEFAULT NULL COMMENT '耗时ms',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- 1.7 公告通知
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`        VARCHAR(100)    NOT NULL COMMENT '标题',
  `content`      TEXT            NOT NULL COMMENT '内容',
  `notice_type`  VARCHAR(20)     NOT NULL COMMENT '类型 ANNOUNCEMENT公告/NOTICE通知/WARNING预警',
  `status`       VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿/PUBLISHED已发布/CLOSED已关闭',
  `publisher_id` BIGINT          DEFAULT NULL COMMENT '发布人',
  `publish_time` DATETIME        DEFAULT NULL COMMENT '发布时间',
  `target_role`  VARCHAR(50)     DEFAULT NULL COMMENT '可见角色编码，NULL=全部',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告通知表';

-- =====================================================================
-- 2. 考生档案域（1 表）
-- =====================================================================

CREATE TABLE IF NOT EXISTS `stu_pilot_profile` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`           BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user 考生账号',
  `name`              VARCHAR(50)     NOT NULL COMMENT '姓名',
  `id_card`           VARCHAR(18)     DEFAULT NULL COMMENT '身份证号',
  `gender`            TINYINT         DEFAULT NULL COMMENT '1男 2女',
  `birth_date`        DATE            DEFAULT NULL COMMENT '出生日期',
  `phone`             VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
  `pilot_type`        VARCHAR(30)     DEFAULT NULL COMMENT '飞行器类别 MULTIROTOR多旋翼/FIXED_WING固定翼/HELICOPTER直升机/VTOL垂直起降',
  `aircraft_model`    VARCHAR(50)     DEFAULT NULL COMMENT '准驾机型',
  `flying_hours`      DECIMAL(8,2)    NOT NULL DEFAULT 0 COMMENT '累计飞行时长',
  `institution_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '所属训练机构 org_institution.id',
  `exam_category`     VARCHAR(50)     DEFAULT NULL COMMENT '报考类别 视距内/超视距等',
  `education`         VARCHAR(20)     DEFAULT NULL COMMENT '学历',
  `emergency_contact` VARCHAR(50)     DEFAULT NULL COMMENT '紧急联系人',
  `status`            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE正常/INACTIVE停用',
  `remark`            VARCHAR(255)    DEFAULT NULL COMMENT '备注',
  `deleted`           TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_id_card` (`id_card`),
  KEY `idx_institution_id` (`institution_id`),
  KEY `idx_pilot_type` (`pilot_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='驾驶员档案表';

-- =====================================================================
-- 3. 考试域（7 表）
-- =====================================================================

-- 3.1 考试计划
CREATE TABLE IF NOT EXISTS `exm_exam_plan` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_code`   VARCHAR(32)     DEFAULT NULL COMMENT '计划编号 PLAN2026-001',
  `plan_name`   VARCHAR(100)    NOT NULL COMMENT '计划名称',
  `exam_type`   VARCHAR(20)     NOT NULL COMMENT '类型 THEORY理论/PRACTICAL实操/BOTH理论与实操',
  `start_date`  DATE            NOT NULL COMMENT '计划开始',
  `end_date`    DATE            NOT NULL COMMENT '计划结束',
  `region`      VARCHAR(100)    DEFAULT NULL COMMENT '覆盖区域',
  `description` VARCHAR(500)    DEFAULT NULL COMMENT '说明',
  `status`      VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿/PUBLISHED已发布/CLOSED已结束/CANCELLED已取消',
  `creator_id`  BIGINT          DEFAULT NULL COMMENT '创建人',
  `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`),
  KEY `idx_status` (`status`),
  KEY `idx_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试计划表';

-- 3.2 考试场次
CREATE TABLE IF NOT EXISTS `exm_exam_session` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id`       BIGINT UNSIGNED NOT NULL COMMENT '所属计划 exm_exam_plan.id',
  `session_code`  VARCHAR(32)     DEFAULT NULL COMMENT '场次编号',
  `session_name`  VARCHAR(100)    NOT NULL COMMENT '场次名称',
  `exam_type`     VARCHAR(20)     NOT NULL COMMENT '类型 THEORY/PRACTICAL',
  `exam_date`     DATE            NOT NULL COMMENT '考试日期',
  `start_time`    TIME            DEFAULT NULL COMMENT '开始时间',
  `end_time`      TIME            DEFAULT NULL COMMENT '结束时间',
  `location`      VARCHAR(200)    DEFAULT NULL COMMENT '地点',
  `room_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '考场 exm_exam_room.id',
  `examiner_id`   BIGINT          DEFAULT NULL COMMENT '主考考官 sys_user.id',
  `full_score`    DECIMAL(5,2)    NOT NULL DEFAULT 100 COMMENT '满分',
  `pass_score`    DECIMAL(5,2)    NOT NULL COMMENT '及格线',
  `capacity`      INT             NOT NULL COMMENT '名额上限',
  `enrolled_count` INT            NOT NULL DEFAULT 0 COMMENT '已报名数',
  `status`        VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PUBLISHED/ENROLLMENT_CLOSED/IN_PROGRESS/COMPLETED/CANCELLED',
  `deleted`       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_code` (`session_code`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_exam_date` (`exam_date`),
  KEY `idx_status` (`status`),
  KEY `idx_examiner_id` (`examiner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试场次表';

-- 3.3 考场
CREATE TABLE IF NOT EXISTS `exm_exam_room` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_code`   VARCHAR(32)     DEFAULT NULL COMMENT '考场编码',
  `room_name`   VARCHAR(100)    NOT NULL COMMENT '考场名称',
  `location`    VARCHAR(200)    DEFAULT NULL COMMENT '地址',
  `capacity`    INT             DEFAULT NULL COMMENT '可容纳人数',
  `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '1可用 0停用',
  `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_code` (`room_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考场表';

-- 3.4 批次（考场编排单元）
CREATE TABLE IF NOT EXISTS `exm_batch` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`     BIGINT UNSIGNED NOT NULL COMMENT '场次 exm_exam_session.id',
  `room_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '考场',
  `batch_code`     VARCHAR(32)     DEFAULT NULL COMMENT '批次号 S001-B01',
  `batch_time`     DATETIME        DEFAULT NULL COMMENT '开考时间',
  `invigilator_id` BIGINT          DEFAULT NULL COMMENT '监考员 sys_user.id',
  `examiner_id`    BIGINT          DEFAULT NULL COMMENT '实操考官 sys_user.id',
  `capacity`       INT             NOT NULL DEFAULT 30 COMMENT '批次容量',
  `enrolled_count` INT             NOT NULL DEFAULT 0 COMMENT '已编排人数',
  `status`         VARCHAR(20)     NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED已编排/ONGOING进行中/FINISHED已结束/CANCELLED已取消',
  `deleted`        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='批次表';

-- 3.5 报名记录
CREATE TABLE IF NOT EXISTS `exm_registration` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `registration_no`   VARCHAR(32)     DEFAULT NULL COMMENT '报名单号',
  `session_id`        BIGINT UNSIGNED NOT NULL COMMENT '场次',
  `batch_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '编排批次 exm_batch.id',
  `student_user_id`   BIGINT UNSIGNED NOT NULL COMMENT '考生账号 sys_user.id',
  `student_profile_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '考生档案 stu_pilot_profile.id',
  `institution_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '所在机构',
  `apply_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审核/APPROVED通过/REJECTED驳回/SCHEDULED已编排/COMPLETED已完成/CANCELLED取消/ABSENT缺考',
  `reject_reason`     VARCHAR(255)    DEFAULT NULL COMMENT '驳回原因',
  `approve_time`      DATETIME        DEFAULT NULL COMMENT '审核通过时间',
  `approver_id`       BIGINT          DEFAULT NULL COMMENT '审核人',
  `deleted`           TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registration_no` (`registration_no`),
  UNIQUE KEY `uk_session_student` (`session_id`, `student_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_student_user_id` (`student_user_id`),
  KEY `idx_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报名记录表';

-- 3.6 成绩记录
CREATE TABLE IF NOT EXISTS `exm_score` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `registration_id` BIGINT UNSIGNED NOT NULL COMMENT '报名单 exm_registration.id',
  `session_id`      BIGINT UNSIGNED NOT NULL COMMENT '场次',
  `student_user_id` BIGINT UNSIGNED NOT NULL COMMENT '考生',
  `exam_type`       VARCHAR(20)     NOT NULL COMMENT 'THEORY/PRACTICAL',
  `score`           DECIMAL(5,2)    DEFAULT NULL COMMENT '分数',
  `pass_status`     VARCHAR(20)     NOT NULL DEFAULT 'NOT_EVALUATED' COMMENT 'PASS合格/FAIL不合格/NOT_EVALUATED未判定',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/SUBMITTED待审核/APPROVED通过/REJECTED退回',
  `examiner_id`     BIGINT          DEFAULT NULL COMMENT '录入考官',
  `entry_time`      DATETIME        DEFAULT NULL COMMENT '录入时间',
  `audit_by`        BIGINT          DEFAULT NULL COMMENT '审核人',
  `audit_time`      DATETIME        DEFAULT NULL COMMENT '审核时间',
  `audit_remark`    VARCHAR(255)    DEFAULT NULL COMMENT '审核意见',
  `remark`          VARCHAR(255)    DEFAULT NULL COMMENT '备注',
  `deleted`         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registration_id` (`registration_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_student_user_id` (`student_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_examiner_id` (`examiner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩记录表';

-- 3.7 成绩审核流水
CREATE TABLE IF NOT EXISTS `exm_score_audit` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `score_id`   BIGINT UNSIGNED NOT NULL COMMENT '成绩单 exm_score.id',
  `auditor_id` BIGINT          DEFAULT NULL COMMENT '审核人',
  `action`     VARCHAR(20)     NOT NULL COMMENT 'PASS通过/REJECT驳回',
  `comment`    VARCHAR(255)    DEFAULT NULL COMMENT '意见',
  `audit_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_score_id` (`score_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩审核流水表';

-- =====================================================================
-- 4. 合格证域（4 表）
-- =====================================================================

-- 4.1 合格证申请
CREATE TABLE IF NOT EXISTS `cer_certificate_apply` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_no`          VARCHAR(32)     DEFAULT NULL COMMENT '申请编号',
  `registration_id`   BIGINT UNSIGNED NOT NULL COMMENT '来源考试报名 exm_registration.id',
  `score_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '对应合格成绩 exm_score.id',
  `student_user_id`   BIGINT UNSIGNED NOT NULL COMMENT '申请人 sys_user.id',
  `certificate_type`  VARCHAR(30)     DEFAULT NULL COMMENT '证类别（按准驾机型/类别）',
  `apply_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `status`            VARCHAR(20)     NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED已提交/PENDING_AUDIT审核中/AUDIT_PASSED通过/AUDIT_REJECTED驳回/ISSUED已签发/CANCELLED撤销',
  `audit_by`          BIGINT          DEFAULT NULL COMMENT '审核人',
  `audit_time`        DATETIME        DEFAULT NULL COMMENT '审核时间',
  `audit_remark`      VARCHAR(255)    DEFAULT NULL COMMENT '审核意见',
  `deleted`           TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  UNIQUE KEY `uk_registration_id` (`registration_id`),
  KEY `idx_student_user_id` (`student_user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合格证申请表';

-- 4.2 合格证
CREATE TABLE IF NOT EXISTS `cer_certificate` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cert_no`          VARCHAR(64)     NOT NULL COMMENT '证号 UVA-2026-000001',
  `apply_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '来源申请 cer_certificate_apply.id',
  `student_user_id`  BIGINT UNSIGNED NOT NULL COMMENT '持证人',
  `certificate_type` VARCHAR(30)     DEFAULT NULL COMMENT '类别',
  `issue_date`       DATE            DEFAULT NULL COMMENT '签发日期',
  `valid_from`       DATE            NOT NULL COMMENT '生效日期',
  `valid_until`      DATE            NOT NULL COMMENT '有效期至',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'VALID' COMMENT 'VALID有效/EXPIRED过期/REVOKED吊销/VOID作废/REISSUED已换发',
  `issuer_id`        BIGINT          DEFAULT NULL COMMENT '签发人',
  `issue_org`        VARCHAR(200)    DEFAULT NULL COMMENT '签发机构名称',
  `remark`           VARCHAR(255)    DEFAULT NULL COMMENT '备注',
  `deleted`          TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cert_no` (`cert_no`),
  KEY `idx_student_user_id` (`student_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_valid_until` (`valid_until`),
  KEY `idx_apply_id` (`apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合格证表';

-- 4.3 合格证审核记录
CREATE TABLE IF NOT EXISTS `cer_certificate_audit` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id`   BIGINT UNSIGNED NOT NULL COMMENT '申请 cer_certificate_apply.id',
  `audit_type` VARCHAR(20)     NOT NULL DEFAULT 'APPLY_AUDIT' COMMENT 'APPLY_AUDIT申请审核/REISSUE_AUDIT换发审核',
  `auditor_id` BIGINT          DEFAULT NULL COMMENT '审核人',
  `action`     VARCHAR(20)     NOT NULL COMMENT 'PASS/REJECT',
  `comment`    VARCHAR(255)    DEFAULT NULL COMMENT '意见',
  `audit_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_apply_id` (`apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合格证审核记录表';

-- 4.4 合格证变更记录
CREATE TABLE IF NOT EXISTS `cer_certificate_change_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cert_id`       BIGINT UNSIGNED NOT NULL COMMENT '原证书 cer_certificate.id',
  `new_cert_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '换发后的新证书',
  `change_type`   VARCHAR(20)     NOT NULL COMMENT 'REVOKE吊销/REISSUE换发/EXPIRE到期/VOID作废',
  `reason`        VARCHAR(255)    DEFAULT NULL COMMENT '原因',
  `operator_id`   BIGINT          DEFAULT NULL COMMENT '操作人',
  `before_status` VARCHAR(20)     DEFAULT NULL COMMENT '变更前状态',
  `after_status`  VARCHAR(20)     DEFAULT NULL COMMENT '变更后状态',
  `operate_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_cert_id` (`cert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合格证变更记录表';

-- =====================================================================
-- 5. 机构认证域（7 表）
-- =====================================================================

-- 5.1 训练机构
CREATE TABLE IF NOT EXISTS `org_institution` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inst_code`          VARCHAR(32)     NOT NULL COMMENT '机构编码',
  `inst_name`          VARCHAR(100)    NOT NULL COMMENT '机构名称',
  `credit_code`        VARCHAR(18)     DEFAULT NULL COMMENT '统一社会信用代码',
  `org_type`           VARCHAR(20)     DEFAULT NULL COMMENT '类型 企业/事业单位/其他',
  `legal_person`       VARCHAR(50)     DEFAULT NULL COMMENT '法定代表人',
  `registered_capital` DECIMAL(12,2)   DEFAULT NULL COMMENT '注册资本',
  `address`            VARCHAR(255)    DEFAULT NULL COMMENT '地址',
  `contact_name`       VARCHAR(50)     DEFAULT NULL COMMENT '联系人',
  `contact_phone`      VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
  `email`              VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
  `business_scope`     VARCHAR(500)    DEFAULT NULL COMMENT '经营范围',
  `qualification_status` VARCHAR(20)   NOT NULL DEFAULT 'NONE' COMMENT 'NONE未认证/PENDING认证中/CERTIFIED已获证/EXPIRED过期/REVOKED吊销/SUSPENDED暂停',
  `status`             TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `deleted`            TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inst_code` (`inst_code`),
  KEY `idx_qualification_status` (`qualification_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='训练机构表';

-- 5.2 认证申请
CREATE TABLE IF NOT EXISTS `org_certification_apply` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_no`       VARCHAR(32)     DEFAULT NULL COMMENT '申请编号',
  `institution_id` BIGINT UNSIGNED NOT NULL COMMENT '机构 org_institution.id',
  `apply_type`     VARCHAR(20)     NOT NULL COMMENT 'NEW首次认证/RENEW续期/CHANGE变更',
  `category`       VARCHAR(50)     DEFAULT NULL COMMENT '认证类别（培训类型）',
  `current_step`   INT             NOT NULL DEFAULT 1 COMMENT '当前环节序号',
  `apply_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `status`         VARCHAR(32)     NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/MATERIAL_REVIEWING/MATERIAL_PASSED/MATERIAL_REJECTED/INSPECTION_PENDING/INSPECTION_SCHEDULED/INSPECTED/QUALIFICATION_REVIEWING/APPROVED/REJECTED/CANCELLED',
  `submitted_by`   BIGINT          DEFAULT NULL COMMENT '提交人',
  `deleted`        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_institution_id` (`institution_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='机构认证申请表';

-- 5.3 申请材料
CREATE TABLE IF NOT EXISTS `org_apply_material` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id`      BIGINT UNSIGNED NOT NULL COMMENT '申请 org_certification_apply.id',
  `material_type` VARCHAR(50)     DEFAULT NULL COMMENT '材料类型 营业执照/场地证明/教员资质/设备清单',
  `file_name`     VARCHAR(200)    DEFAULT NULL COMMENT '文件名',
  `file_url`      VARCHAR(500)    DEFAULT NULL COMMENT '存储地址',
  `upload_by`     BIGINT          DEFAULT NULL COMMENT '上传人',
  `upload_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_apply_id` (`apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='认证申请材料表';

-- 5.4 材料审查记录
CREATE TABLE IF NOT EXISTS `org_material_review` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id`    BIGINT UNSIGNED NOT NULL COMMENT '申请',
  `reviewer_id` BIGINT          DEFAULT NULL COMMENT '审查人',
  `result`      VARCHAR(20)     NOT NULL COMMENT 'PASS通过/REJECT退回',
  `comment`     VARCHAR(255)    DEFAULT NULL COMMENT '意见',
  `review_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  `review_step` INT             DEFAULT NULL COMMENT '审查环节',
  PRIMARY KEY (`id`),
  KEY `idx_apply_id` (`apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='材料审查记录表';

-- 5.5 实地核查任务
CREATE TABLE IF NOT EXISTS `org_site_inspection` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id`        BIGINT UNSIGNED NOT NULL COMMENT '申请',
  `institution_id`  BIGINT UNSIGNED NOT NULL COMMENT '机构',
  `inspector_id`    BIGINT          DEFAULT NULL COMMENT '核查员 sys_user.id',
  `inspection_date` DATE            DEFAULT NULL COMMENT '核查日期',
  `address`         VARCHAR(255)    DEFAULT NULL COMMENT '核查地址',
  `checklist`       TEXT            DEFAULT NULL COMMENT '核查项清单',
  `result`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待出/PASS通过/FAIL不通过',
  `summary`         TEXT            DEFAULT NULL COMMENT '核查结论',
  `report_url`      VARCHAR(500)    DEFAULT NULL COMMENT '报告文件',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待派发/ASSIGNED已派发/IN_PROGRESS核查中/COMPLETED已完成/CANCELLED取消',
  `assign_time`     DATETIME        DEFAULT NULL COMMENT '派发时间',
  `complete_time`   DATETIME        DEFAULT NULL COMMENT '完成时间',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_apply_id` (`apply_id`),
  KEY `idx_inspector_id` (`inspector_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='实地核查任务表';

-- 5.6 资质评定记录
CREATE TABLE IF NOT EXISTS `org_qualification_review` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id`         BIGINT UNSIGNED NOT NULL COMMENT '申请',
  `reviewer_id`      BIGINT          DEFAULT NULL COMMENT '评定人',
  `evaluation_score` DECIMAL(5,2)    DEFAULT NULL COMMENT '评定得分',
  `suggestion`       VARCHAR(255)    DEFAULT NULL COMMENT '评定建议',
  `result`           VARCHAR(20)     NOT NULL COMMENT 'PASS通过/REJECT驳回',
  `review_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_apply_id` (`apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资质评定记录表';

-- 5.7 机构资质证
CREATE TABLE IF NOT EXISTS `org_qualification` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `qualification_no`   VARCHAR(64)     NOT NULL COMMENT '资质证号',
  `apply_id`           BIGINT UNSIGNED DEFAULT NULL COMMENT '来源申请',
  `institution_id`     BIGINT UNSIGNED NOT NULL COMMENT '机构',
  `qualification_level` VARCHAR(20)    DEFAULT NULL COMMENT '资质等级',
  `category`           VARCHAR(50)     DEFAULT NULL COMMENT '认证类别',
  `issue_date`         DATE            DEFAULT NULL COMMENT '发证日期',
  `valid_until`        DATE            NOT NULL COMMENT '有效期至',
  `status`             VARCHAR(20)     NOT NULL DEFAULT 'VALID' COMMENT 'VALID有效/EXPIRED过期/REVOKED吊销/SUSPENDED暂停',
  `issuer_id`          BIGINT          DEFAULT NULL COMMENT '签发人',
  `revoke_reason`      VARCHAR(255)    DEFAULT NULL COMMENT '吊销原因',
  `deleted`            TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qualification_no` (`qualification_no`),
  KEY `idx_institution_id` (`institution_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='机构资质证表';

SET FOREIGN_KEY_CHECKS = 1;
