-- =====================================================================
-- 无人机驾驶员管理后台 种子数据 02-data.sql
-- 密码统一为 123456（BCrypt: $2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW）
-- 所有 INSERT 使用 INSERT IGNORE + 固定ID，保证幂等可重复执行
-- =====================================================================

USE `uav_admin`;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 角色
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`id`, `role_code`, `role_name`, `data_scope`, `status`, `description`) VALUES
(1, 'ADMIN', '超级管理员', 'ALL', 1, '系统最高权限，管理全部业务'),
(2, 'EXAMINER', '考官', 'ALL', 1, '负责考试组织与成绩录入'),
(3, 'INSTITUTION_ADMIN', '机构管理员', 'INSTITUTION', 1, '训练机构账号，提交认证与学员管理'),
(4, 'STUDENT', '考生', 'SELF', 1, '考生账号，报名考试与查询证书');

-- ---------------------------------------------------------------------
-- 2. 菜单/权限（目录 1000-1500 / 菜单 x01 / 按钮 xxxx01+）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `order_num`, `visible`, `status`) VALUES
-- 一级目录
(1000, 0, '首页', 1, '/dashboard', 'dashboard/index', 'dashboard:view', 'Odometer', 1, 1, 1),
(1100, 0, '系统管理', 1, '/system', NULL, NULL, 'Setting', 2, 1, 1),
(1200, 0, '考生档案', 1, '/student', NULL, NULL, 'User', 3, 1, 1),
(1300, 0, '考试管理', 1, '/exam', NULL, NULL, 'EditPen', 4, 1, 1),
(1400, 0, '合格证管理', 1, '/certificate', NULL, NULL, 'Medal', 5, 1, 1),
(1500, 0, '机构认证', 1, '/institution', NULL, NULL, 'OfficeBuilding', 6, 1, 1),
-- 系统管理-菜单
(1101, 1100, '用户管理', 2, 'user', 'system/user/index', 'system:user:list', 'User', 1, 1, 1),
(1102, 1100, '角色管理', 2, 'role', 'system/role/index', 'system:role:list', 'Avatar', 2, 1, 1),
(1103, 1100, '菜单管理', 2, 'menu', 'system/menu/index', 'system:menu:list', 'Menu', 3, 1, 1),
(1104, 1100, '操作日志', 2, 'log', 'system/log/index', 'system:log:list', 'Document', 4, 1, 1),
(1105, 1100, '公告管理', 2, 'notice', 'system/notice/index', 'system:notice:list', 'Bell', 5, 1, 1),
-- 考生档案-菜单
(1201, 1200, '档案列表', 2, 'profile', 'student/profileList/index', 'student:profile:list', 'UserFilled', 1, 1, 1),
-- 考试管理-菜单
(1301, 1300, '考试计划', 2, 'plan', 'exam/plan/index', 'exam:plan:list', 'Calendar', 1, 1, 1),
(1302, 1300, '场次管理', 2, 'session', 'exam/session/index', 'exam:session:list', 'AlarmClock', 2, 1, 1),
(1303, 1300, '考场管理', 2, 'room', 'exam/room/index', 'exam:room:list', 'House', 3, 1, 1),
(1304, 1300, '批次编排', 2, 'batch', 'exam/batch/index', 'exam:batch:list', 'Sort', 4, 1, 1),
(1305, 1300, '报名管理', 2, 'registration', 'exam/registration/index', 'exam:registration:list', 'List', 5, 1, 1),
(1306, 1300, '成绩录入', 2, 'score', 'exam/score/index', 'exam:score:list', 'Edit', 6, 1, 1),
(1307, 1300, '成绩审核', 2, 'scoreAudit', 'exam/scoreAudit/index', 'exam:scoreAudit:list', 'Checked', 7, 1, 1),
-- 合格证-菜单
(1401, 1400, '申请审核', 2, 'apply', 'certificate/apply/index', 'cert:apply:list', 'Tickets', 1, 1, 1),
(1402, 1400, '证书管理', 2, 'certificate', 'certificate/certificate/index', 'cert:certificate:list', 'Medal', 2, 1, 1),
-- 机构认证-菜单
(1501, 1500, '机构管理', 2, 'institution', 'institution/institution/index', 'inst:institution:list', 'OfficeBuilding', 1, 1, 1),
(1502, 1500, '认证申请', 2, 'application', 'institution/application/index', 'inst:application:list', 'DocumentAdd', 2, 1, 1),
(1503, 1500, '材料审查', 2, 'materialReview', 'institution/materialReview/index', 'inst:material:list', 'FolderChecked', 3, 1, 1),
(1504, 1500, '实地核查', 2, 'inspection', 'institution/inspection/index', 'inst:inspection:list', 'Location', 4, 1, 1),
(1505, 1500, '资质证管理', 2, 'qualification', 'institution/qualification/index', 'inst:qualification:list', 'Stamp', 5, 1, 1),
-- 系统管理-按钮
(110101, 1101, '用户新增', 3, NULL, NULL, 'system:user:add', NULL, 1, 1, 1),
(110102, 1101, '用户编辑', 3, NULL, NULL, 'system:user:edit', NULL, 2, 1, 1),
(110103, 1101, '用户删除', 3, NULL, NULL, 'system:user:delete', NULL, 3, 1, 1),
(110104, 1101, '重置密码', 3, NULL, NULL, 'system:user:resetPwd', NULL, 4, 1, 1),
(110105, 1101, '分配角色', 3, NULL, NULL, 'system:user:assignRole', NULL, 5, 1, 1),
(110201, 1102, '角色新增', 3, NULL, NULL, 'system:role:add', NULL, 1, 1, 1),
(110202, 1102, '角色编辑', 3, NULL, NULL, 'system:role:edit', NULL, 2, 1, 1),
(110203, 1102, '角色删除', 3, NULL, NULL, 'system:role:delete', NULL, 3, 1, 1),
(110204, 1102, '分配菜单', 3, NULL, NULL, 'system:role:assignMenu', NULL, 4, 1, 1),
(110301, 1103, '菜单新增', 3, NULL, NULL, 'system:menu:add', NULL, 1, 1, 1),
(110302, 1103, '菜单编辑', 3, NULL, NULL, 'system:menu:edit', NULL, 2, 1, 1),
(110303, 1103, '菜单删除', 3, NULL, NULL, 'system:menu:delete', NULL, 3, 1, 1),
(110501, 1105, '公告新增', 3, NULL, NULL, 'system:notice:add', NULL, 1, 1, 1),
(110502, 1105, '公告编辑', 3, NULL, NULL, 'system:notice:edit', NULL, 2, 1, 1),
(110503, 1105, '公告删除', 3, NULL, NULL, 'system:notice:delete', NULL, 3, 1, 1),
(110504, 1105, '公告发布', 3, NULL, NULL, 'system:notice:publish', NULL, 4, 1, 1),
-- 考生档案-按钮
(120101, 1201, '档案新增', 3, NULL, NULL, 'student:profile:add', NULL, 1, 1, 1),
(120102, 1201, '档案编辑', 3, NULL, NULL, 'student:profile:edit', NULL, 2, 1, 1),
(120103, 1201, '档案删除', 3, NULL, NULL, 'student:profile:delete', NULL, 3, 1, 1),
-- 考试-按钮
(130101, 1301, '计划新增', 3, NULL, NULL, 'exam:plan:add', NULL, 1, 1, 1),
(130102, 1301, '计划编辑', 3, NULL, NULL, 'exam:plan:edit', NULL, 2, 1, 1),
(130103, 1301, '计划删除', 3, NULL, NULL, 'exam:plan:delete', NULL, 3, 1, 1),
(130104, 1301, '计划发布', 3, NULL, NULL, 'exam:plan:publish', NULL, 4, 1, 1),
(130201, 1302, '场次新增', 3, NULL, NULL, 'exam:session:add', NULL, 1, 1, 1),
(130202, 1302, '场次编辑', 3, NULL, NULL, 'exam:session:edit', NULL, 2, 1, 1),
(130203, 1302, '场次删除', 3, NULL, NULL, 'exam:session:delete', NULL, 3, 1, 1),
(130204, 1302, '场次发布', 3, NULL, NULL, 'exam:session:publish', NULL, 4, 1, 1),
(130205, 1302, '截止报名', 3, NULL, NULL, 'exam:session:close', NULL, 5, 1, 1),
(130301, 1303, '考场新增', 3, NULL, NULL, 'exam:room:add', NULL, 1, 1, 1),
(130302, 1303, '考场编辑', 3, NULL, NULL, 'exam:room:edit', NULL, 2, 1, 1),
(130303, 1303, '考场删除', 3, NULL, NULL, 'exam:room:delete', NULL, 3, 1, 1),
(130401, 1304, '批次新增', 3, NULL, NULL, 'exam:batch:add', NULL, 1, 1, 1),
(130402, 1304, '批次编辑', 3, NULL, NULL, 'exam:batch:edit', NULL, 2, 1, 1),
(130403, 1304, '批次删除', 3, NULL, NULL, 'exam:batch:delete', NULL, 3, 1, 1),
(130501, 1305, '报名新增', 3, NULL, NULL, 'exam:registration:add', NULL, 1, 1, 1),
(130502, 1305, '报名审核', 3, NULL, NULL, 'exam:registration:approve', NULL, 2, 1, 1),
(130503, 1305, '报名驳回', 3, NULL, NULL, 'exam:registration:reject', NULL, 3, 1, 1),
(130504, 1305, '批次编排', 3, NULL, NULL, 'exam:registration:schedule', NULL, 4, 1, 1),
(130601, 1306, '成绩录入', 3, NULL, NULL, 'exam:score:add', NULL, 1, 1, 1),
(130602, 1306, '成绩提交', 3, NULL, NULL, 'exam:score:submit', NULL, 2, 1, 1),
(130701, 1307, '成绩审核', 3, NULL, NULL, 'exam:scoreAudit:audit', NULL, 1, 1, 1),
-- 合格证-按钮
(140101, 1401, '申请审核', 3, NULL, NULL, 'cert:apply:audit', NULL, 1, 1, 1),
(140201, 1402, '证书换发', 3, NULL, NULL, 'cert:certificate:reissue', NULL, 1, 1, 1),
(140202, 1402, '证书吊销', 3, NULL, NULL, 'cert:certificate:revoke', NULL, 2, 1, 1),
-- 机构认证-按钮
(150101, 1501, '机构新增', 3, NULL, NULL, 'inst:institution:add', NULL, 1, 1, 1),
(150102, 1501, '机构编辑', 3, NULL, NULL, 'inst:institution:edit', NULL, 2, 1, 1),
(150103, 1501, '机构删除', 3, NULL, NULL, 'inst:institution:delete', NULL, 3, 1, 1),
(150201, 1502, '提交申请', 3, NULL, NULL, 'inst:application:submit', NULL, 1, 1, 1),
(150202, 1502, '提交材料', 3, NULL, NULL, 'inst:application:submitMaterial', NULL, 2, 1, 1),
(150301, 1503, '材料审查', 3, NULL, NULL, 'inst:material:review', NULL, 1, 1, 1),
(150401, 1504, '核查派发', 3, NULL, NULL, 'inst:inspection:assign', NULL, 1, 1, 1),
(150402, 1504, '核查完成', 3, NULL, NULL, 'inst:inspection:complete', NULL, 2, 1, 1),
(150501, 1505, '资质吊销', 3, NULL, NULL, 'inst:qualification:revoke', NULL, 1, 1, 1);

-- ---------------------------------------------------------------------
-- 3. 角色-菜单授权
-- ---------------------------------------------------------------------
-- ADMIN：全部菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`;
-- EXAMINER：首页 + 考试管理（不含成绩审核/计划发布等管理操作）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1000), (2, 1300), (2, 1301), (2, 1302), (2, 1303), (2, 1304), (2, 1305), (2, 1306),
(2, 130201), (2, 130202), (2, 130301), (2, 130302), (2, 130401), (2, 130402),
(2, 130501), (2, 130601), (2, 130602);
-- INSTITUTION_ADMIN：首页 + 机构认证（机构/申请/材料/核查查看 + 提交材料）+ 公告
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3, 1000), (3, 1105), (3, 1500), (3, 1501), (3, 1502), (3, 1503), (3, 1504), (3, 1505),
(3, 150201), (3, 150202), (3, 150301), (3, 150401);
-- STUDENT：首页 + 公告 + 证书查看
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 1000), (4, 1105), (4, 1400), (4, 1402);

-- ---------------------------------------------------------------------
-- 4. 用户
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `id_card`, `status`, `remark`) VALUES
(1,  'admin',      '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '超级管理员', '13800000001', 'admin@uav.gov.cn', NULL, 1, '系统管理员'),
(2,  'examiner1',  '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '王考官',     '13800000002', 'examiner1@uav.gov.cn', NULL, 1, '理论考试主考'),
(3,  'examiner2',  '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '李考官',     '13800000003', 'examiner2@uav.gov.cn', NULL, 1, '实操考试主考'),
(4,  'inst_admin1','$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '赵机构',     '13800000004', 'inst@tianji.com', NULL, 1, '天际飞训机构管理员'),
(5,  'student1',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '张三',       '13800000005', 'zs@student.com', '110101199001011234', 1, '考生'),
(6,  'student2',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '李四',       '13800000006', 'ls@student.com', '110101199002021235', 1, '考生'),
(7,  'student3',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '王五',       '13800000007', 'ww@student.com', '110101199003031236', 1, '考生'),
(8,  'student4',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '赵六',       '13800000008', 'zl@student.com', '110101199004041237', 1, '考生'),
(9,  'student5',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '钱七',       '13800000009', 'qq@student.com', '110101199005051238', 1, '考生'),
(10, 'student6',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '孙八',       '13800000010', 'sb@student.com', '110101199006061239', 1, '考生'),
(11, 'student7',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '周九',       '13800000011', 'zj@student.com', '110101199007071240', 1, '考生'),
(12, 'student8',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '吴十',       '13800000012', 'ws@student.com', '110101199008081241', 1, '考生'),
(13, 'student9',   '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '郑一',       '13800000013', 'zy@student.com', '110101199009091242', 1, '考生'),
(14, 'student10',  '$2b$10$H3PnFMbGTUfgfYyx3fK3X.nyWcRGh41z6UQintikeYU3xnXJ3s1oW', '冯二',       '13800000014', 'fe@student.com', '110101199010101243', 1, '考生');

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), (2, 2), (3, 2), (4, 3),
(5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 4), (12, 4), (13, 4), (14, 4);

-- ---------------------------------------------------------------------
-- 5. 训练机构
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `org_institution` (`id`, `inst_code`, `inst_name`, `credit_code`, `org_type`, `legal_person`, `registered_capital`, `address`, `contact_name`, `contact_phone`, `email`, `business_scope`, `qualification_status`, `status`) VALUES
(1, 'TJ-001', '天际飞训无人机培训中心', '91110108MA01AAAA1A', '企业', '刘总', 500.00, '北京市海淀区中关村科技园', '赵机构', '13800000004', 'inst@tianji.com', '无人机驾驶员培训、飞行技能训练', 'CERTIFIED', 1),
(2, 'YY-002', '云翼无人机培训学校', '91110108MA01BBBB2B', '企业', '陈总', 300.00, '上海市浦东新区张江高科技园', '陈老师', '13800000021', 'inst@yunyi.com', '多旋翼驾驶员培训、青少年航模培训', 'PENDING', 1),
(3, 'CX-003', '晨曦航空技术培训', '91110108MA01CCCC3C', '企业', '杨总', 200.00, '深圳市南山区科技园', '杨老师', '13800000022', 'inst@chenxi.com', '固定翼无人机培训、行业应用培训', 'NONE', 1);

-- ---------------------------------------------------------------------
-- 6. 考生档案（10 份）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `stu_pilot_profile` (`id`, `user_id`, `name`, `id_card`, `gender`, `birth_date`, `phone`, `pilot_type`, `aircraft_model`, `flying_hours`, `institution_id`, `exam_category`, `education`, `emergency_contact`, `status`) VALUES
(1,  5,  '张三', '110101199001011234', 1, '1990-01-01', '13800000005', 'MULTIROTOR', '大疆 M300 RTK', 120.50, 1, '超视距', '本科', '张父', 'ACTIVE'),
(2,  6,  '李四', '110101199002021235', 1, '1990-02-02', '13800000006', 'MULTIROTOR', '大疆 Mavic 3E', 85.00, 1, '视距内', '大专', '李母', 'ACTIVE'),
(3,  7,  '王五', '110101199003031236', 2, '1990-03-03', '13800000007', 'FIXED_WING', '垂直起降固定翼 V330', 200.00, 1, '超视距', '硕士', '王父', 'ACTIVE'),
(4,  8,  '赵六', '110101199004041237', 1, '1990-04-04', '13800000008', 'MULTIROTOR', '大疆 M300 RTK', 60.00, 2, '视距内', '高中', '赵母', 'ACTIVE'),
(5,  9,  '钱七', '110101199005051238', 1, '1990-05-05', '13800000009', 'MULTIROTOR', '大疆 Inspire 3', 150.00, 2, '超视距', '本科', '钱父', 'ACTIVE'),
(6,  10, '孙八', '110101199006061239', 2, '1990-06-06', '13800000010', 'HELICOPTER', '无人机直升机 700', 300.00, NULL, '超视距', '本科', '孙母', 'ACTIVE'),
(7,  11, '周九', '110101199007071240', 1, '1990-07-07', '13800000011', 'MULTIROTOR', '大疆 Mavic 3', 45.00, NULL, '视距内', '大专', '周父', 'ACTIVE'),
(8,  12, '吴十', '110101199008081241', 1, '1990-08-08', '13800000012', 'FIXED_WING', '固定翼巡检机', 180.00, 3, '超视距', '本科', '吴母', 'ACTIVE'),
(9,  13, '郑一', '110101199009091242', 2, '1990-09-09', '13800000013', 'MULTIROTOR', '大疆 M300 RTK', 95.00, 3, '视距内', '大专', '郑父', 'ACTIVE'),
(10, 14, '冯二', '110101199010101243', 1, '1990-10-10', '13800000014', 'VTOL', '垂直起降 VTOL 500', 260.00, NULL, '超视距', '本科', '冯母', 'ACTIVE');

-- ---------------------------------------------------------------------
-- 7. 考场 / 考试计划 / 场次 / 批次
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `exm_exam_room` (`id`, `room_code`, `room_name`, `location`, `capacity`, `status`) VALUES
(1, 'T-L01', '理论考场A', '北京考试中心 3F', 50, 1),
(2, 'P-L01', '实操考场B', '北京飞行训练场', 30, 1);

INSERT IGNORE INTO `exm_exam_plan` (`id`, `plan_code`, `plan_name`, `exam_type`, `start_date`, `end_date`, `region`, `description`, `status`, `creator_id`) VALUES
(1, 'PLAN2026-001', '2026年度第一期无人机驾驶员考试', 'BOTH', '2026-03-01', '2026-03-31', '华北区', '包含理论与实操考试，面向多旋翼与固定翼驾驶员', 'CLOSED', 1),
(2, 'PLAN2026-002', '2026年度第二期无人机驾驶员考试', 'BOTH', '2026-09-01', '2026-09-30', '华北区', '秋季考试计划，报名进行中', 'PUBLISHED', 1);

INSERT IGNORE INTO `exm_exam_session` (`id`, `plan_id`, `session_code`, `session_name`, `exam_type`, `exam_date`, `start_time`, `end_time`, `location`, `room_id`, `examiner_id`, `full_score`, `pass_score`, `capacity`, `enrolled_count`, `status`) VALUES
(1, 1, 'SES2026-001', '第一期理论考试', 'THEORY', '2026-03-15', '09:00:00', '11:00:00', '北京考试中心 3F', 1, 2, 100, 60, 50, 5, 'COMPLETED'),
(2, 1, 'SES2026-002', '第一期实操考试', 'PRACTICAL', '2026-03-20', '09:00:00', '17:00:00', '北京飞行训练场', 2, 3, 100, 70, 30, 3, 'COMPLETED'),
(3, 2, 'SES2026-003', '第二期理论考试', 'THEORY', '2026-09-12', '09:00:00', '11:00:00', '北京考试中心 3F', 1, 2, 100, 60, 60, 3, 'PUBLISHED'),
(4, 2, 'SES2026-004', '第二期实操考试', 'PRACTICAL', '2026-09-18', '09:00:00', '17:00:00', '北京飞行训练场', 2, 3, 100, 70, 30, 0, 'PUBLISHED');

INSERT IGNORE INTO `exm_batch` (`id`, `session_id`, `room_id`, `batch_code`, `batch_time`, `invigilator_id`, `examiner_id`, `capacity`, `enrolled_count`, `status`) VALUES
(1, 1, 1, 'SES2026-001-B01', '2026-03-15 09:00:00', 2, NULL, 25, 3, 'FINISHED'),
(2, 1, 1, 'SES2026-001-B02', '2026-03-15 10:00:00', 2, NULL, 25, 2, 'FINISHED'),
(3, 2, 2, 'SES2026-002-B01', '2026-03-20 09:00:00', 3, 3, 30, 3, 'FINISHED'),
(4, 3, 1, 'SES2026-003-B01', '2026-09-12 09:00:00', 2, NULL, 30, 2, 'PLANNED');

-- ---------------------------------------------------------------------
-- 8. 报名记录（8 条，覆盖各状态）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `exm_registration` (`id`, `registration_no`, `session_id`, `batch_id`, `student_user_id`, `student_profile_id`, `institution_id`, `apply_time`, `status`, `approve_time`, `approver_id`) VALUES
(1, 'RG20260001', 1, 1, 5, 1, 1, '2026-02-20 10:00:00', 'COMPLETED', '2026-02-21 09:00:00', 1),
(2, 'RG20260002', 1, 1, 6, 2, 1, '2026-02-20 10:05:00', 'COMPLETED', '2026-02-21 09:00:00', 1),
(3, 'RG20260003', 1, 2, 7, 3, 1, '2026-02-20 10:10:00', 'COMPLETED', '2026-02-21 09:00:00', 1),
(4, 'RG20260004', 1, 2, 8, 4, 2, '2026-02-20 10:15:00', 'COMPLETED', '2026-02-21 09:00:00', 1),
(5, 'RG20260005', 1, 1, 9, 5, 2, '2026-02-20 10:20:00', 'REJECTED', NULL, 1),
(6, 'RG20260006', 2, 3, 5, 1, 1, '2026-02-22 10:00:00', 'COMPLETED', '2026-02-23 09:00:00', 1),
(7, 'RG20260007', 2, 3, 6, 2, 1, '2026-02-22 10:05:00', 'COMPLETED', '2026-02-23 09:00:00', 1),
(8, 'RG20260008', 3, 4, 10, 6, NULL, '2026-08-20 09:00:00', 'SCHEDULED', '2026-08-21 09:00:00', 1),
(9, 'RG20260009', 3, 4, 11, 7, NULL, '2026-08-20 09:05:00', 'APPROVED', '2026-08-21 09:00:00', 1),
(10, 'RG20260010', 3, NULL, 12, 8, 3, '2026-08-22 09:00:00', 'PENDING', NULL, NULL);

-- ---------------------------------------------------------------------
-- 9. 成绩（5 条，覆盖各状态；关联报名 1,2,3,6,7）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `exm_score` (`id`, `registration_id`, `session_id`, `student_user_id`, `exam_type`, `score`, `pass_status`, `status`, `examiner_id`, `entry_time`, `audit_by`, `audit_time`, `audit_remark`) VALUES
(1, 1, 1, 5,  'THEORY', 88.00, 'PASS',  'APPROVED', 2, '2026-03-15 12:00:00', 1, '2026-03-16 10:00:00', '成绩真实有效'),
(2, 2, 1, 6,  'THEORY', 55.00, 'FAIL',  'APPROVED', 2, '2026-03-15 12:00:00', 1, '2026-03-16 10:00:00', '低于及格线'),
(3, 3, 1, 7,  'THEORY', 92.00, 'PASS',  'APPROVED', 2, '2026-03-15 12:00:00', 1, '2026-03-16 10:00:00', '成绩真实有效'),
(4, 4, 1, 8,  'THEORY', 78.00, 'NOT_EVALUATED', 'SUBMITTED', 2, '2026-03-15 12:00:00', NULL, NULL, NULL),
(5, 6, 2, 5,  'PRACTICAL', 85.00, 'PASS',  'APPROVED', 3, '2026-03-20 18:00:00', 1, '2026-03-21 10:00:00', '实操考核通过'),
(6, 7, 2, 6,  'PRACTICAL', 95.00, 'PASS',  'APPROVED', 3, '2026-03-20 18:00:00', 1, '2026-03-21 10:00:00', '实操考核通过');

INSERT IGNORE INTO `exm_score_audit` (`id`, `score_id`, `auditor_id`, `action`, `comment`, `audit_time`) VALUES
(1, 1, 1, 'PASS', '同意', '2026-03-16 10:00:00'),
(2, 2, 1, 'PASS', '同意，判定不合格', '2026-03-16 10:00:00'),
(3, 3, 1, 'PASS', '同意', '2026-03-16 10:00:00'),
(4, 5, 1, 'PASS', '同意', '2026-03-21 10:00:00'),
(5, 6, 1, 'PASS', '同意', '2026-03-21 10:00:00');

-- ---------------------------------------------------------------------
-- 10. 合格证（1 张已签发 VALID + 1 条申请待审）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `cer_certificate_apply` (`id`, `apply_no`, `registration_id`, `score_id`, `student_user_id`, `certificate_type`, `apply_time`, `status`, `audit_by`, `audit_time`, `audit_remark`) VALUES
(1, 'CA20260001', 1, 1, 5, 'MULTIROTOR', '2026-03-16 11:00:00', 'ISSUED', 1, '2026-03-17 09:00:00', '符合发证条件'),
(2, 'CA20260002', 6, 5, 5, 'MULTIROTOR', '2026-03-21 11:00:00', 'PENDING_AUDIT', NULL, NULL, NULL),
(3, 'CA20260003', 3, 3, 7, 'FIXED_WING', '2026-03-16 11:00:00', 'ISSUED', 1, '2026-03-17 09:00:00', '符合发证条件');

INSERT IGNORE INTO `cer_certificate_audit` (`id`, `apply_id`, `audit_type`, `auditor_id`, `action`, `comment`, `audit_time`) VALUES
(1, 1, 'APPLY_AUDIT', 1, 'PASS', '审核通过', '2026-03-17 09:00:00'),
(2, 3, 'APPLY_AUDIT', 1, 'PASS', '审核通过', '2026-03-17 09:00:00');

INSERT IGNORE INTO `cer_certificate` (`id`, `cert_no`, `apply_id`, `student_user_id`, `certificate_type`, `issue_date`, `valid_from`, `valid_until`, `status`, `issuer_id`, `issue_org`) VALUES
(1, 'UVA-2026-000001', 1, 5, 'MULTIROTOR', '2026-03-18', '2026-03-18', '2032-03-18', 'VALID', 1, '无人机驾驶员管理机构'),
(2, 'UVA-2026-000002', 3, 7, 'FIXED_WING', '2026-03-18', '2026-03-18', '2032-03-18', 'VALID', 1, '无人机驾驶员管理机构');

-- ---------------------------------------------------------------------
-- 11. 机构资质证（天际飞训 1 张）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `org_qualification` (`id`, `qualification_no`, `apply_id`, `institution_id`, `qualification_level`, `category`, `issue_date`, `valid_until`, `status`, `issuer_id`) VALUES
(1, 'QF-2026-000001', NULL, 1, 'A级', '多旋翼驾驶员培训', '2026-01-10', '2029-01-10', 'VALID', 1);

-- ---------------------------------------------------------------------
-- 12. 机构认证申请（云翼培训进行中：材料审查中）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `org_certification_apply` (`id`, `apply_no`, `institution_id`, `apply_type`, `category`, `current_step`, `apply_time`, `status`, `submitted_by`) VALUES
(1, 'IA20260001', 2, 'NEW', '多旋翼驾驶员培训', 2, '2026-08-01 09:00:00', 'MATERIAL_REVIEWING', 4);

INSERT IGNORE INTO `org_apply_material` (`id`, `apply_id`, `material_type`, `file_name`, `file_url`, `upload_by`, `upload_time`) VALUES
(1, 1, '营业执照', '营业执照.pdf', '/files/yy-license.pdf', 4, '2026-08-01 09:10:00'),
(2, 1, '场地证明', '训练场地证明.pdf', '/files/yy-site.pdf', 4, '2026-08-01 09:10:00'),
(3, 1, '教员资质', '教员资质证书.zip', '/files/yy-teachers.zip', 4, '2026-08-01 09:10:00');

-- ---------------------------------------------------------------------
-- 13. 公告
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_notice` (`id`, `title`, `content`, `notice_type`, `status`, `publisher_id`, `publish_time`, `target_role`) VALUES
(1, '2026年第二期考试报名通知', '2026年度第二期无人机驾驶员考试将于9月12日举行，请各位考生于9月5日前完成报名。', 'ANNOUNCEMENT', 'PUBLISHED', 1, '2026-08-15 09:00:00', NULL),
(2, '资质认证材料规范更新', '自2026年9月起，训练机构资质认证需补充设备清单与安全管理制度文件。', 'NOTICE', 'DRAFT', 1, NULL, 'INSTITUTION_ADMIN');

-- ---------------------------------------------------------------------
-- 14. 操作日志样例
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_operation_log` (`id`, `user_id`, `username`, `module`, `operation`, `method`, `request_url`, `request_method`, `params`, `ip`, `status`, `cost_time`, `create_time`) VALUES
(1, 1, 'admin', 'auth', '用户登录', 'AuthController.login', '/api/auth/login', 'POST', '{"username":"admin"}', '127.0.0.1', 1, 120, '2026-08-27 09:00:00'),
(2, 1, 'admin', 'exam', '报名审核通过', 'RegistrationController.approve', '/api/exam/registrations/10/approve', 'POST', '{"id":10}', '127.0.0.1', 1, 80, '2026-08-27 09:10:00'),
(3, 2, 'examiner1', 'exam', '录入成绩', 'ScoreController.create', '/api/exam/scores', 'POST', '{"registrationId":8}', '127.0.0.1', 1, 95, '2026-08-27 09:20:00');
