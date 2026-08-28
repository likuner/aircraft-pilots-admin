# 无人机驾驶员管理后台 — 最终实施计划

> 目标目录：`/Users/likun/Documents/aircraft-pilots-admin`（greenfield，当前为空）
> 业务定位：核心管理机构后台 —— 无人机驾驶员考试管理、合格证颁发、训练机构资质认证。
> 技术栈（用户指定）：后端 Java + Maven + SpringBoot + RabbitMQ + MySQL + Redis；前端 Vue3 + Element Plus + Tailwind CSS + Pinia + Vuex + Vue Router。
> 用户已确认：完整业务闭环（含状态机流转与消息通知）、Spring Security + JWT RBAC、提供 docker-compose 一键环境、需要种子数据。

## 执行总览（实施时按此落地）

**目录结构（将创建）：**
```
aircraft-pilots-admin/
├── backend/                 # Spring Boot 3.3.x 单体应用（Maven + mvnw）
├── frontend/                # Vue3 + Vite5 + Element Plus + Tailwind + Pinia + Vuex + Router
├── docker/
│   └── docker-compose.yml   # MySQL8 + Redis7 + RabbitMQ3（healthcheck + 端口映射）
├── docker/mysql/init/01-schema.sql + 02-data.sql   # 26 张表 DDL + 种子数据（与 sql/ 同源）
├── sql/01-schema.sql + 02-data.sql
└── README.md                # 启动手册（compose → 建库 → 起后端 → 起前端）
```

**前置条件（需在 M0 完成，可能需要用户确认）：**
1. **JDK 17**：本机当前仅有 JDK 8，Spring Boot 3.x 需要 17+。方案 A：`brew install openjdk@17` 并配置 JAVA_HOME（推荐）；方案 B：若无法安装，回退 Spring Boot 2.7.x + javax 依赖线（仅改 pom，代码尽量兼容）。
2. **Maven**：本机未安装，工程内嵌 Maven Wrapper（`mvnw`），首次用 `./mvnw` 自举。
3. Node v22 / Docker 29 已就绪，无需处理。

**实施顺序：M0 基础设施 → M1 认证与RBAC → M2 考生档案+考试主链路 → M3 成绩与判定 → M4 合格证 → M5 机构认证 → M6 收尾**（各里程碑详见第 8 节，每阶段可独立验证）。

---

## 0. 环境现状与前提（已探测）

| 项 | 现状 | 影响 |
|---|---|---|
| Java | 仅 1.8.0_161 | Spring Boot 3.x 需要 JDK 17，需先装 JDK17 |
| Maven | 未安装 | 使用 Maven Wrapper（`mvnw`）或通过 Docker 构建 |
| Node | v22.22.2 / npm 10.9.7 | 完全满足 Vite5/6 + Vue3 要求 |
| Docker | 29.7.2 / Compose v5.4.0 | 完全满足中间件一键启动 |
| 目标目录 | 空 | 全新初始化 |

---

## 1. 技术选型与理由

### 1.1 后端

**Spring Boot 版本：推荐 3.3.x（如 3.3.5）+ JDK 17**

| 维度 | 2.7.x | 3.3.x（推荐） |
|---|---|---|
| JDK 要求 | 8/11/17 | 17+ |
| 生态现状 | OSS 支持已停止（社区 EOL 2023-11） | 当前主流 LTS 线，持续维护 |
| 依赖 | javax.* | jakarta.*（新库均以 jakarta 为基准） |
| 适配 | MyBatis-Plus/Springdoc 等需兼容层 | 有官方 `-spring-boot3` starter，开箱即用 |
| 结论 | 仅当无法装 JDK17 时兜底 | **选定** |

> 本机当前只有 JDK8。落地前需装 JDK17（`brew install openjdk@17` 或项目内用 `maven:3.9-eclipse-temurin-17` 镜像构建）。由于 Maven 未安装，工程必须内嵌 **Maven Wrapper**（`mvnw`）。

**ORM：推荐 MyBatis-Plus 3.5.x（`mybatis-plus-spring-boot3-starter`）**

理由：本系统有大量动态条件查询、分组统计（报名人数/通过率）、多表联查与报表；MyBatis-Plus 提供分页插件、逻辑删除、乐观锁、代码生成器，且复杂 SQL 可直接写 XML 精确控制；JPA 在复杂统计与状态机场景下调优成本高。MyBatis-Plus 是国内管理后台事实标准，团队上手快。

其余关键依赖：

| 用途 | 依赖 |
|---|---|
| JWT | `io.jsonwebtoken:jjwt-api/impl/jackson` 0.12.x |
| API 文档 | `springdoc-openapi-starter-webmvc-ui` 2.5.x |
| 消息 | `spring-boot-starter-amqp` |
| 缓存 | `spring-boot-starter-data-redis`（Lettuce）|
| 数据库 | `com.mysql:mysql-connector-j` |
| 参数校验 | `spring-boot-starter-validation` |
| 加密 | Spring Security 内置 BCryptPasswordEncoder |

### 1.2 前端

Vue3.4+ / Vite 5 / Element Plus 2.7+ / Tailwind CSS 3.4+ / Pinia 2.x / Vuex 4.x / Vue Router 4.x / axios。
构建层无需特殊配置，Node v22 直接支持。

### 1.3 Pinia 与 Vuex 职责划分（无冲突原则）

| 状态库 | 负责范围 | 理由 |
|---|---|---|
| **Vuex（全局基础设施）** | 登录态 token、用户信息、角色/权限集合、动态菜单/路由、应用级 UI 状态（侧边栏折叠、主题） | 属于**启动即需、跨页面全局**的基础设施，且 router 守卫依赖它做权限过滤；Vuex 单一全局 store 天然适合 |
| **Pinia（业务模块状态）** | examStore（考试计划/场次/批次/报名/成绩）、certificateStore（申请/证书）、institutionStore（机构/认证）、noticeStore、studentStore | 按业务 feature 拆分、按需加载，setup 风格写业务状态（列表查询条件、选中行、草稿表单、分页）更清爽 |

冲突避免原则：
1. 权限、token、用户、菜单**只进 Vuex**，Pinia 不得复制。
2. 各业务列表页的查询条件/草稿/选中态**只进 Pinia**，Vuex 不承担业务数据。
3. 登录流程：Vuex user module 持 token；登录成功后用接口返回的菜单树在 Vuex permission module 中 `addRoute` 动态路由。

---

## 2. 总体架构

```
┌────────────────────────────────────────────┐
│  前端 Vue3 (Vite dev :5173 / build 静态)      │
│  Element Plus(组件) + Tailwind(布局)          │
│  Pinia(业务状态) + Vuex(权限/全局) + Router守卫 │
└───────────────┬────────────────────────────┘
                │ REST /api/** (JWT Bearer)
┌───────────────▼────────────────────────────┐
│  Spring Boot 3.3 (AdminApplication :8080)   │
│  Security(JWT+RBAC) → Controller → Service   │
│      → Mapper(MyBatis-Plus) → MySQL 8        │
│  Redis: 验证码/Token/名额扣减/缓存/分布式锁     │
│  RabbitMQ: 考试结果/证书签发/公告广播(异步)      │
│  定时任务: 场次状态流转、证书到期预警            │
└────────────────────────────────────────────┘
        │ compose up
┌───────▼────────────────────────────────────┐
│  Docker Compose: MySQL8 + Redis7 + RabbitMQ3 │
└────────────────────────────────────────────┘
```

---

## 3. 数据库设计（重点）

- 统一约定：所有表含 `id BIGINT UNSIGNED PK AUTO_INCREMENT`、`create_time`、`update_time`；业务主表加 `deleted TINYINT`（0 正常 1 删除，逻辑删除）；所有 ENUM 用 `VARCHAR(32)` 存英文值，`COMMENT` 标注中文枚举，避免改库结构。
- 字符集 `utf8mb4`，排序 `utf8mb4_general_ci`；InnoDB 引擎。
- 数据库名：`uav_admin`。

### 3.1 系统域（7 表）

**sys_user 用户**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| username | VARCHAR(50) | UNIQUE NOT NULL | 登录名 |
| password | VARCHAR(100) | NOT NULL | BCrypt 密文 |
| real_name | VARCHAR(50) | | 姓名 |
| phone | VARCHAR(20) | | 手机号 |
| email | VARCHAR(100) | | 邮箱 |
| avatar | VARCHAR(255) | | 头像 URL |
| id_card | VARCHAR(18) | | 身份证号（考试身份核验）|
| status | TINYINT | NOT NULL DEFAULT 1 | 1启用 0禁用 |
| last_login_time | DATETIME | | 最后登录时间 |
| last_login_ip | VARCHAR(50) | | 最后登录 IP |
| remark | VARCHAR(255) | | 备注 |
| create_by/update_by | BIGINT | | 操作人 |

索引：`uk_username`、`idx_status`、`idx_phone`。

**sys_role 角色**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| role_code | VARCHAR(50) | UNIQUE NOT NULL | ADMIN / EXAMINER / INSTITUTION_ADMIN / STUDENT |
| role_name | VARCHAR(50) | NOT NULL | 角色名称 |
| data_scope | VARCHAR(20) | DEFAULT 'ALL' | ALL/SELF/INSTITUTION（数据范围）|
| status | TINYINT | DEFAULT 1 | 1启用 0禁用 |
| description | VARCHAR(255) | | 描述 |

**sys_menu 菜单/权限（按钮权限复用本表）**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| parent_id | BIGINT | DEFAULT 0 | 父菜单 |
| menu_name | VARCHAR(50) | NOT NULL | 名称 |
| menu_type | TINYINT | NOT NULL | 1目录 2菜单 3按钮 |
| path | VARCHAR(200) | | 前端路由路径 |
| component | VARCHAR(200) | | 前端组件路径 |
| perms | VARCHAR(100) | | 权限标识，如 `exam:plan:add` |
| icon | VARCHAR(100) | | 图标 |
| order_num | INT | DEFAULT 0 | 排序 |
| visible | TINYINT | DEFAULT 1 | 1显示 0隐藏 |
| status | TINYINT | DEFAULT 1 | 1启用 0停用 |

**sys_user_role**：`user_id`、`role_id`，联合主键 `(user_id, role_id)`。
**sys_role_menu**：`role_id`、`menu_id`，联合主键 `(role_id, menu_id)`。

**sys_operation_log 操作日志**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| user_id | BIGINT | | 操作人 |
| username | VARCHAR(50) | | 操作人账号 |
| module | VARCHAR(50) | | 模块（exam/cert/institution…）|
| operation | VARCHAR(100) | | 操作描述 |
| method | VARCHAR(200) | | 控制器方法签名 |
| request_url | VARCHAR(255) | | 请求地址 |
| request_method | VARCHAR(10) | | GET/POST… |
| params | TEXT | | 请求参数 |
| ip | VARCHAR(50) | | 来源 IP |
| status | TINYINT | | 1成功 0失败 |
| error_msg | TEXT | | 错误信息 |
| cost_time | BIGINT | | 耗时 ms |

索引：`idx_user_id`、`idx_create_time`、`idx_module`。

**sys_notice 公告通知**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| title | VARCHAR(100) | NOT NULL | 标题 |
| content | TEXT | NOT NULL | 内容 |
| notice_type | VARCHAR(20) | NOT NULL | ANNOUNCEMENT公告 / NOTICE通知 / WARNING预警 |
| status | VARCHAR(20) | NOT NULL | DRAFT草稿 / PUBLISHED已发布 / CLOSED已关闭 |
| publisher_id | BIGINT | | 发布人 |
| publish_time | DATETIME | | 发布时间 |
| target_role | VARCHAR(50) | | 可见角色，NULL=全部 |

索引：`idx_status`、`idx_publish_time`。

**状态机（公告）**：`DRAFT → PUBLISHED → CLOSED`。

### 3.2 考生档案域（1 表）

**stu_pilot_profile 驾驶员档案**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| user_id | BIGINT | UNIQUE NOT NULL | 关联 sys_user（考生账号）|
| name | VARCHAR(50) | NOT NULL | 姓名 |
| id_card | VARCHAR(18) | UNIQUE | 身份证号 |
| gender | TINYINT | | 1男 2女 |
| birth_date | DATE | | 出生日期 |
| phone | VARCHAR(20) | | 联系电话 |
| pilot_type | VARCHAR(30) | | 飞行器类别：MULTIROTOR多旋翼 / FIXED_WING固定翼 / HELICOPTER直升机 / VTOL垂直起降 |
| aircraft_model | VARCHAR(50) | | 准驾机型 |
| flying_hours | DECIMAL(8,2) | DEFAULT 0 | 累计飞行时长 |
| institution_id | BIGINT | | 所属训练机构（FK org_institution.id）|
| exam_category | VARCHAR(50) | | 报考类别（视距内/超视距…）|
| education | VARCHAR(20) | | 学历 |
| emergency_contact | VARCHAR(50) | | 紧急联系人 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | ACTIVE正常 / INACTIVE停用 |
| remark | VARCHAR(255) | | 备注 |

索引：`uk_user_id`、`uk_id_card`、`idx_institution_id`、`idx_pilot_type`。

### 3.3 考试域（7 表）

**exm_exam_plan 考试计划**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| plan_code | VARCHAR(32) | UNIQUE | 计划编号（如 PLAN2026-001）|
| plan_name | VARCHAR(100) | NOT NULL | 计划名称 |
| exam_type | VARCHAR(20) | NOT NULL | THEORY理论 / PRACTICAL实操 / BOTH理论与实操 |
| start_date | DATE | NOT NULL | 计划开始 |
| end_date | DATE | NOT NULL | 计划结束 |
| region | VARCHAR(100) | | 覆盖区域 |
| description | VARCHAR(500) | | 说明 |
| status | VARCHAR(20) | NOT NULL | DRAFT草稿 / PUBLISHED已发布 / CLOSED已结束 / CANCELLED已取消 |
| creator_id | BIGINT | | 创建人 |

索引：`uk_plan_code`、`idx_status`、`idx_start_date`。

**exm_exam_session 考试场次**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| plan_id | BIGINT | FK→exm_exam_plan | 所属计划 |
| session_code | VARCHAR(32) | UNIQUE | 场次编号 |
| session_name | VARCHAR(100) | NOT NULL | 场次名称 |
| exam_type | VARCHAR(20) | NOT NULL | THEORY / PRACTICAL |
| exam_date | DATE | NOT NULL | 考试日期 |
| start_time | TIME | | 开始时间 |
| end_time | TIME | | 结束时间 |
| location | VARCHAR(200) | | 地点 |
| room_id | BIGINT | FK→exm_exam_room | 考场 |
| examiner_id | BIGINT | FK→sys_user | 主考考官 |
| full_score | DECIMAL(5,2) | DEFAULT 100 | 满分 |
| pass_score | DECIMAL(5,2) | NOT NULL | 及格线（自动判定用）|
| capacity | INT | NOT NULL | 名额上限 |
| enrolled_count | INT | DEFAULT 0 | 已报名数 |
| status | VARCHAR(20) | NOT NULL | 见状态机 |

索引：`uk_session_code`、`idx_plan_id`、`idx_exam_date`、`idx_status`、`idx_examiner_id`。

**exm_exam_room 考场**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| room_code | VARCHAR(32) | UNIQUE | 考场编码 |
| room_name | VARCHAR(100) | NOT NULL | 考场名称 |
| location | VARCHAR(200) | | 地址 |
| capacity | INT | | 可容纳人数 |
| status | TINYINT | DEFAULT 1 | 1可用 0停用 |

**exm_batch 批次（考场编排单元）**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| session_id | BIGINT | FK→exm_exam_session | 场次 |
| room_id | BIGINT | FK→exm_exam_room | 考场 |
| batch_code | VARCHAR(32) | | 批次号（如 S001-B01）|
| batch_time | DATETIME | | 开考时间 |
| invigilator_id | BIGINT | FK→sys_user | 监考员 |
| examiner_id | BIGINT | FK→sys_user | 实操考官 |
| capacity | INT | | 批次容量 |
| enrolled_count | INT | DEFAULT 0 | 已编排人数 |
| status | VARCHAR(20) | NOT NULL | PLANNED已编排 / ONGOING进行中 / FINISHED已结束 / CANCELLED已取消 |

索引：`idx_session_id`、`idx_room_id`。

**exm_registration 报名记录**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| registration_no | VARCHAR(32) | UNIQUE | 报名单号 |
| session_id | BIGINT | FK→exm_exam_session | 场次 |
| batch_id | BIGINT | 可空 FK→exm_batch | 编排批次 |
| student_user_id | BIGINT | FK→sys_user | 考生账号 |
| student_profile_id | BIGINT | FK→stu_pilot_profile | 考生档案 |
| institution_id | BIGINT | 可空 FK→org_institution | 所在机构 |
| apply_time | DATETIME | NOT NULL | 报名时间 |
| status | VARCHAR(20) | NOT NULL | 见状态机 |
| reject_reason | VARCHAR(255) | | 驳回原因 |
| approve_time | DATETIME | | 审核通过时间 |
| approver_id | BIGINT | | 审核人 |

索引：`uk_session_student(session_id, student_user_id)`（一人一场次仅一单）、`idx_status`、`idx_student_user_id`。

**exm_score 成绩记录**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| registration_id | BIGINT | UNIQUE FK→exm_registration | 报名单（一单一成绩）|
| session_id | BIGINT | FK→exm_exam_session | 场次 |
| student_user_id | BIGINT | FK→sys_user | 考生 |
| exam_type | VARCHAR(20) | NOT NULL | THEORY / PRACTICAL |
| score | DECIMAL(5,2) | | 分数 |
| pass_status | VARCHAR(20) | | PASS合格 / FAIL不合格 / NOT_EVALUATED未判定 |
| status | VARCHAR(20) | NOT NULL | 见状态机 |
| examiner_id | BIGINT | FK→sys_user | 录入考官 |
| entry_time | DATETIME | | 录入时间 |
| audit_by | BIGINT | | 审核人 |
| audit_time | DATETIME | | 审核时间 |
| audit_remark | VARCHAR(255) | | 审核意见 |
| remark | VARCHAR(255) | | 备注 |

索引：`uk_registration_id`、`idx_session_id`、`idx_student_user_id`、`idx_status`、`idx_examiner_id`。

**exm_score_audit 成绩审核流水**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| score_id | BIGINT | FK→exm_score | 成绩单 |
| auditor_id | BIGINT | | 审核人 |
| action | VARCHAR(20) | | PASS通过 / REJECT驳回 |
| comment | VARCHAR(255) | | 意见 |
| audit_time | DATETIME | | 时间 |

**状态机汇总（考试域）**

报名 `exm_registration.status`：
```
PENDING待审核 ─┬─ 审核通过 → APPROVED ──(编排批次)──→ SCHEDULED ──(开考)──→ COMPLETED
               ├─ 驳回 → REJECTED
               └─ 考生取消 → CANCELLED
               (缺考 ABSENT 由成绩域录入时回写)
```

场次 `exm_exam_session.status`：
```
DRAFT → PUBLISHED ─┬─ 到截止时间(定时任务) → ENROLLMENT_CLOSED ──(开考)──→ IN_PROGRESS ──(全部成绩已录/到结束时间)──→ COMPLETED
                   └─ 直接取消 → CANCELLED
```

成绩 `exm_score.status`：
```
DRAFT草稿 ─(考官提交)─→ SUBMITTED待审核 ─┬─ 审核通过 → APPROVED ──(自动按 pass_score 判定)──→ pass_status=PASS/FAIL
                                        └─ 审核驳回 → REJECTED(退回 DRAFT 重新录入)
```

### 3.4 合格证域（4 表）

**cer_certificate_apply 合格证申请**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| apply_no | VARCHAR(32) | UNIQUE | 申请编号 |
| registration_id | BIGINT | UNIQUE FK→exm_registration | 来源考试报名（一单一生效申请）|
| score_id | BIGINT | FK→exm_score | 对应合格成绩 |
| student_user_id | BIGINT | FK→sys_user | 申请人 |
| certificate_type | VARCHAR(30) | | 证类别（按准驾机型/类别）|
| apply_time | DATETIME | NOT NULL | 申请时间 |
| status | VARCHAR(20) | NOT NULL | 见状态机 |
| audit_by | BIGINT | | 审核人 |
| audit_time | DATETIME | | 审核时间 |
| audit_remark | VARCHAR(255) | | 审核意见 |

索引：`uk_apply_no`、`uk_registration_id`、`idx_student_user_id`、`idx_status`。

**cer_certificate 合格证**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| cert_no | VARCHAR(64) | UNIQUE NOT NULL | 证号（签发时生成，如 UVA-2026-000001）|
| apply_id | BIGINT | FK→cer_certificate_apply | 来源申请 |
| student_user_id | BIGINT | FK→sys_user | 持证人 |
| certificate_type | VARCHAR(30) | | 类别 |
| issue_date | DATE | | 签发日期 |
| valid_from | DATE | NOT NULL | 生效日期 |
| valid_until | DATE | NOT NULL | 有效期至（默认 6 年，可配置）|
| status | VARCHAR(20) | NOT NULL | 见状态机 |
| issuer_id | BIGINT | | 签发人 |
| issue_org | VARCHAR(200) | | 签发机构名称 |
| remark | VARCHAR(255) | | 备注 |

索引：`uk_cert_no`、`idx_student_user_id`、`idx_status`、`idx_valid_until`、`idx_apply_id`。

**cer_certificate_audit 审核记录**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| apply_id | BIGINT | FK→cer_certificate_apply | 申请 |
| audit_type | VARCHAR(20) | | APPLY_AUDIT申请审核 / REISSUE_AUDIT换发审核 |
| auditor_id | BIGINT | | 审核人 |
| action | VARCHAR(20) | | PASS / REJECT |
| comment | VARCHAR(255) | | 意见 |
| audit_time | DATETIME | | 时间 |

**cer_certificate_change_record 变更记录（作废/换发/吊销）**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| cert_id | BIGINT | FK→cer_certificate | 原证书 |
| new_cert_id | BIGINT | 可空 | 换发后的新证书 |
| change_type | VARCHAR(20) | | REVOKE吊销 / REISSUE换发 / EXPIRE到期 / VOID作废 |
| reason | VARCHAR(255) | | 原因 |
| operator_id | BIGINT | | 操作人 |
| before_status / after_status | VARCHAR(20) | | 变更前后状态 |
| operate_time | DATETIME | | 时间 |

索引：`idx_cert_id`。

**状态机（合格证域）**

申请 `cer_certificate_apply.status`：
```
SUBMITTED已提交 → (审核) → PENDING_AUDIT审核中 ─┬─ 通过 → AUDIT_PASSED ─(异步签发)→ 生成证书 ISSUED
                                              └─ 驳回 → AUDIT_REJECTED
                                              (考生主动撤销 → CANCELLED)
```

证书 `cer_certificate.status`：
```
VALID有效 ─┬─ 到期(定时任务) → EXPIRED
           ├─ 违规吊销 → REVOKED
           ├─ 操作作废 → VOID
           └─ 换发 → REISSUED（生成新证书新证号，原证 REISSUED，new_cert_id 指向新证）
```

### 3.5 机构认证域（7 表）

**org_institution 训练机构**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| inst_code | VARCHAR(32) | UNIQUE NOT NULL | 机构编码 |
| inst_name | VARCHAR(100) | NOT NULL | 机构名称 |
| credit_code | VARCHAR(18) | | 统一社会信用代码 |
| org_type | VARCHAR(20) | | 企业/事业单位/其他 |
| legal_person | VARCHAR(50) | | 法定代表人 |
| registered_capital | DECIMAL(12,2) | | 注册资本 |
| address | VARCHAR(255) | | 地址 |
| contact_name / contact_phone / email | VARCHAR(50)/VARCHAR(20)/VARCHAR(100) | | 联系人信息 |
| business_scope | VARCHAR(500) | | 经营范围 |
| qualification_status | VARCHAR(20) | NOT NULL DEFAULT 'NONE' | NONE未认证 / PENDING认证中 / CERTIFIED已获证 / EXPIRED过期 / REVOKED吊销 / SUSPENDED暂停 |
| status | TINYINT | DEFAULT 1 | 1启用 0停用 |

索引：`uk_inst_code`、`idx_qualification_status`。

**org_certification_apply 认证申请**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| apply_no | VARCHAR(32) | UNIQUE | 申请编号 |
| institution_id | BIGINT | FK→org_institution | 机构 |
| apply_type | VARCHAR(20) | NOT NULL | NEW首次认证 / RENEW续期 / CHANGE变更 |
| category | VARCHAR(50) | | 认证类别（培训类型）|
| current_step | INT | DEFAULT 1 | 当前环节序号（流程引导）|
| apply_time | DATETIME | NOT NULL | 提交时间 |
| status | VARCHAR(20) | NOT NULL | 见状态机 |
| submitted_by | BIGINT | | 提交人 |

索引：`uk_apply_no`、`idx_institution_id`、`idx_status`。

**org_apply_material 申请材料**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| apply_id | BIGINT | FK→org_certification_apply | 申请 |
| material_type | VARCHAR(50) | | 材料类型（营业执照/场地证明/教员资质/设备清单…）|
| file_name | VARCHAR(200) | | 文件名 |
| file_url | VARCHAR(500) | | 存储地址 |
| upload_by | BIGINT | | 上传人 |
| upload_time | DATETIME | | 上传时间 |

索引：`idx_apply_id`。

**org_material_review 材料审查记录**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| apply_id | BIGINT | FK→org_certification_apply | 申请 |
| reviewer_id | BIGINT | | 审查人 |
| result | VARCHAR(20) | | PASS通过 / REJECT退回 |
| comment | VARCHAR(255) | | 意见 |
| review_time | DATETIME | | 时间 |
| review_step | INT | | 审查环节 |

索引：`idx_apply_id`。

**org_site_inspection 实地核查任务**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| apply_id | BIGINT | FK→org_certification_apply | 申请 |
| institution_id | BIGINT | FK→org_institution | 机构 |
| inspector_id | BIGINT | FK→sys_user | 核查员 |
| inspection_date | DATE | | 核查日期 |
| address | VARCHAR(255) | | 核查地址 |
| checklist | TEXT | | 核查项清单 |
| result | VARCHAR(20) | | PENDING待出 / PASS通过 / FAIL不通过 |
| summary | TEXT | | 核查结论 |
| report_url | VARCHAR(500) | | 报告文件 |
| status | VARCHAR(20) | NOT NULL | PENDING待派发 / ASSIGNED已派发 / IN_PROGRESS核查中 / COMPLETED已完成 / CANCELLED取消 |
| assign_time / complete_time | DATETIME | | 派发/完成时间 |

索引：`idx_apply_id`、`idx_inspector_id`。

**org_qualification_review 资质评定记录**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| apply_id | BIGINT | FK→org_certification_apply | 申请 |
| reviewer_id | BIGINT | | 评定人 |
| evaluation_score | DECIMAL(5,2) | | 评定得分 |
| suggestion | VARCHAR(255) | | 评定建议 |
| result | VARCHAR(20) | | PASS通过 / REJECT驳回 |
| review_time | DATETIME | | 时间 |

索引：`idx_apply_id`。

**org_qualification 机构资质证**
| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| id | BIGINT UNSIGNED | PK AUTO | 主键 |
| qualification_no | VARCHAR(64) | UNIQUE NOT NULL | 资质证号 |
| apply_id | BIGINT | FK→org_certification_apply | 来源申请 |
| institution_id | BIGINT | FK→org_institution | 机构 |
| qualification_level | VARCHAR(20) | | 资质等级 |
| category | VARCHAR(50) | | 认证类别 |
| issue_date | DATE | | 发证日期 |
| valid_until | DATE | NOT NULL | 有效期至（默认 3 年，可配置）|
| status | VARCHAR(20) | NOT NULL | VALID有效 / EXPIRED过期 / REVOKED吊销 / SUSPENDED暂停 |
| issuer_id | BIGINT | | 签发人 |
| revoke_reason | VARCHAR(255) | | 吊销原因 |

索引：`uk_qualification_no`、`idx_institution_id`、`idx_status`。

**状态机（机构认证域）**

认证申请 `org_certification_apply.status`（线性推进 + 回退）：
```
SUBMITTED已提交
  → MATERIAL_REVIEWING材料审查中 ──退回──→ MATERIAL_REJECTED(可重新提交)
  → MATERIAL_PASSED材料通过
  → INSPECTION_PENDING待核查 ──(派发核查任务)──→ INSPECTION_SCHEDULED已派发 ──(核查完成)──→ INSPECTED核查完成(通过)
     (核查 FAIL → 回 MATERIAL_REJECTED 或终止)
  → QUALIFICATION_REVIEWING资质评定中 ─┬─ 通过 → APPROVED(发证/续期)
                                       └─ 驳回 → REJECTED
  (机构撤销 → CANCELLED)
```

机构资质状态与机构表 `qualification_status` 联动：`NONE → PENDING → CERTIFIED ↔ EXPIRED / REVOKED / SUSPENDED`。

### 3.6 建表清单汇总（共 26 表）

| # | 表名 | 归属 |
|---|---|---|
| 1-7 | sys_user / sys_role / sys_menu / sys_user_role / sys_role_menu / sys_operation_log / sys_notice | 系统域 |
| 8 | stu_pilot_profile | 考生档案 |
| 9-15 | exm_exam_plan / exm_exam_session / exm_exam_room / exm_batch / exm_registration / exm_score / exm_score_audit | 考试域 |
| 16-19 | cer_certificate_apply / cer_certificate / cer_certificate_audit / cer_certificate_change_record | 合格证域 |
| 20-26 | org_institution / org_certification_apply / org_apply_material / org_material_review / org_site_inspection / org_qualification_review / org_qualification | 机构认证域 |

> 可选扩展：`sys_dict`/`sys_dict_data` 数据字典表、`sys_file` 文件上传表，按需在 M1 后补充，不阻塞主链路。

---

## 4. 后端设计

### 4.1 Maven 工程结构（单体）

```
aircraft-pilots-admin/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/          # Maven Wrapper（本机无 mvn，必须）
├── docker/
│   ├── docker-compose.yml
│   └── mysql/init/01-schema.sql + 02-data.sql
├── sql/01-schema.sql + 02-data.sql   # 与 docker 挂载目录保持一致（同一份）
└── src/main/
    ├── java/com/uav/admin/
    │   ├── AdminApplication.java
    │   ├── common/        # Result<T>/PageResult<T>/ErrorCode/BaseException/GlobalExceptionHandler/BaseEntity/Constants
    │   ├── config/        # SecurityConfig/RedisConfig/RabbitConfig/MybatisPlusConfig(分页/乐观锁)/WebConfig(CORS)/AsyncConfig/OpenApiConfig
    │   ├── security/      # JwtUtil/JwtAuthenticationFilter/LoginUser/UserDetailsServiceImpl/RestAccessDeniedHandler/RestAuthEntryPoint
    │   ├── enums/         # 各状态枚举类（与 DB ENUM 值一一对应）
    │   ├── mq/            # RabbitListener 消费者 + 消息体 DTO
    │   ├── task/          # 定时任务：场次状态流转/证书到期/资质到期预警
    │   ├── controller/    # auth/system/student/exam/certificate/institution/notice
    │   ├── service/       # 接口 + impl（状态机流转逻辑集中在 service）
    │   ├── mapper/        # MyBatis-Plus BaseMapper + 自定义 XML
    │   ├── entity/        # 数据库实体
    │   ├── dto/           # 入参对象（含校验注解）
    │   └── vo/            # 出参对象
    └── resources/
        ├── application.yml / application-dev.yml
        └── mapper/*.xml
```

### 4.2 核心类清单

| 层 | 关键类 | 职责 |
|---|---|---|
| common | `Result<T>`/`ErrorCode`/`GlobalExceptionHandler` | 统一响应 `{code,msg,data}`；全局异常兜底 |
| security | `JwtUtil` | HS256 签发/解析/刷新，token 内含 userId+roles |
| security | `JwtAuthenticationFilter` | 解析 Bearer Token → 写入 SecurityContext |
| security | `LoginUser implements UserDetails` | 缓存当前用户+角色+权限集合 |
| security | `SecurityConfig` | 无状态过滤链、白名单、CORS、@EnableMethodSecurity |
| mq | `ExamResultConsumer`/`CertIssueConsumer`/`NoticeBroadcastConsumer` | 异步消费者 |
| task | `ExamSessionStatusTask`/`CertificateExpireTask`/`QualificationExpireTask` | 定时状态推进/预警 |
| service | `ExamRegisterService`/`ScoreAuditService`/`CertIssueService`/`CertificationFlowService` | 状态机流转唯一入口（防并发/幂等）|

### 4.3 REST API 路由设计（按模块）

统一前缀 `/api`。除标 `[PUBLIC]` 外均需 JWT。

**auth**
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/auth/captcha | 图形验证码 `[PUBLIC]` |
| POST | /api/auth/login | 登录（username+password+captcha）返回 token |
| POST | /api/auth/refresh | 刷新 token |
| POST | /api/auth/logout | 登出（token 拉黑）|
| GET | /api/auth/me | 当前用户信息+权限 |
| PUT | /api/auth/password | 修改密码 |

**system**
| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/PUT/DELETE | /api/system/users... | 用户 CRUD、重置密码、启停 |
| GET/POST/PUT/DELETE | /api/system/roles... | 角色 CRUD、分配菜单 |
| GET/POST/PUT/DELETE | /api/system/menus... | 菜单树 CRUD |
| GET | /api/system/menus/role/{roleId} | 角色已有菜单 |
| PUT | /api/system/users/{id}/roles | 用户分配角色 |
| GET | /api/system/logs | 操作日志分页 |
| GET/POST/PUT | /api/system/notices | 公告 CRUD |
| POST | /api/system/notices/{id}/publish | 发布公告 |

**student（档案）**
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/student/profiles | 档案分页（多条件：姓名/机型/机构/状态）|
| GET | /api/student/profiles/{id} | 档案详情（含考试/证书历史）|
| POST/PUT | /api/student/profiles | 新建/编辑档案 |
| GET | /api/student/profiles/{id}/records | 考试记录/证书记录时间线 |

**exam**
| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/PUT/DELETE | /api/exam/plans | 考试计划 CRUD |
| POST | /api/exam/plans/{id}/publish | 发布计划 |
| GET/POST/PUT/DELETE | /api/exam/sessions | 场次 CRUD |
| POST | /api/exam/sessions/{id}/publish | 发布场次（开放报名）|
| POST | /api/exam/sessions/{id}/close | 截止报名 |
| GET/POST/PUT/DELETE | /api/exam/rooms | 考场 CRUD |
| GET | /api/exam/sessions/{id}/batches | 场次批次列表 |
| POST | /api/exam/batches | 新增批次 |
| POST | /api/exam/registrations | 考生报名（Redis 原子扣名额）|
| GET | /api/exam/registrations | 报名分页 |
| POST | /api/exam/registrations/{id}/approve | 报名审核通过 |
| POST | /api/exam/registrations/{id}/reject | 报名驳回 |
| POST | /api/exam/registrations/{id}/schedule | 编排批次 |
| POST | /api/exam/scores | 考官录入成绩 |
| GET | /api/exam/scores | 成绩分页 |
| POST | /api/exam/scores/{id}/submit | 提交成绩（进入待审）|
| POST | /api/exam/scores/{id}/audit | 成绩审核（通过→自动判定合格并发通知）|

**certificate**
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/cert/applications | 提交合格证申请（成绩合格后自动触发）|
| GET | /api/cert/applications | 申请分页 |
| POST | /api/cert/applications/{id}/audit | 申请审核（通过→发 MQ 异步签发）|
| GET | /api/cert/certificates | 证书分页（证号/状态/有效期检索）|
| GET | /api/cert/certificates/{id} | 证书详情 |
| POST | /api/cert/certificates/{id}/reissue | 换发（生成新证号）|
| POST | /api/cert/certificates/{id}/revoke | 吊销/作废 |
| GET | /api/cert/certificates/{id}/changes | 变更记录 |

**institution**
| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/PUT | /api/institution/institutions | 机构 CRUD |
| POST | /api/institution/applications | 提交认证申请 |
| GET | /api/institution/applications | 申请分页 |
| POST | /api/institution/applications/{id}/submit-material | 提交材料 |
| POST | /api/institution/applications/{id}/review-material | 材料审查（通过/退回）|
| POST | /api/institution/applications/{id}/assign-inspection | 派发核查任务 |
| POST | /api/institution/inspections/{id}/complete | 完成实地核查（通过/不通过）|
| POST | /api/institution/applications/{id}/qualify | 资质评定（通过→发证/续期）|
| GET | /api/institution/qualifications | 机构资质证分页 |
| POST | /api/institution/qualifications/{id}/revoke | 吊销/暂停 |

### 4.4 Spring Security 配置要点

1. **无状态 JWT**：`sessionCreationPolicy(STATELESS)`；`JwtAuthenticationFilter` 在每个请求解析 header `Authorization: Bearer xxx`。
2. **白名单 permitAll**：`/api/auth/login`、`/api/auth/captcha`、`/api/auth/refresh`、`/swagger-ui/**`、`/v3/api-docs/**`；其余 `anyRequest().authenticated()`。
3. **RBAC 授权**：`@EnableMethodSecurity` 启用 `@PreAuthorize("hasAuthority('exam:plan:add')")`（按钮级权限走 menu.perms）；角色用 `hasRole('ADMIN')`。
4. **密码**：`BCryptPasswordEncoder`（强度 10）。
5. **异常处理**：自定义 `RestAuthenticationEntryPoint`（401 JSON）与 `RestAccessDeniedHandler`（403 JSON），统一返回 `Result`。
6. **CORS**：允许 `http://localhost:5173`，放行 `Authorization` 头。
7. **登录校验链**：验证码（Redis）→ 用户名密码 → 生成 access token（短时效 30min）+ refresh token（7d）→ 写 Redis 会话。
8. **数据权限**：`INSTITUTION_ADMIN` 通过 `data_scope=INSTITUTION` 在 service 层拼接 `institution_id` 过滤（MyBatis-Plus `@InterceptorIgnore` 或自定义拦截器）。

### 4.5 RabbitMQ 应用场景

| 场景 | 交换机/类型 | 队列 | 路由键 | 流程 |
|---|---|---|---|---|
| 考试结果通知 | `uav.exchange.direct` | `uav.queue.exam.result` | `exam.result` | 成绩审核通过→发布消息→消费后站内信/短信通知考生 |
| 证书异步签发 | `uav.exchange.direct` | `uav.queue.cert.issue` | `cert.issue` | 申请审核通过→发消息→消费端生成证号/有效期、写 cer_certificate、回写 apply.status=ISSUED |
| 公告广播 | `uav.exchange.fanout` | `uav.queue.notice.broadcast` | - | 发布公告→广播消费（可扩展 WebSocket 推送前端）|
| 机构核查结果通知 | `uav.exchange.direct` | `uav.queue.inst.notify` | `inst.notify` | 核查完成/评定结果→通知机构管理员 |

可靠性：`spring.rabbitmq.listener.simple.acknowledge-mode=MANUAL`，消费失败重试 3 次后进死信队列 `uav.queue.dlx`；消息体统一封装 `{eventType, payload, traceId, timestamp}`。证书签发消费端用 Redis 分布式锁保证证号幂等。

### 4.6 Redis 应用场景

| Key 前缀 | 用途 | 说明 |
|---|---|---|
| `uav:captcha:{uuid}` | 登录图形验证码 | TTL 5min，登录即删 |
| `uav:token:blacklist:{jti}` | 登出 token 拉黑 | 剩余有效期 |
| `uav:refresh:{userId}` | refresh token 管理 | 可主动失效 |
| `uav:exam:session:{id}:capacity` / `uav:exam:batch:{id}:capacity` | 报名名额原子扣减 | **Lua 脚本** `DECR` + 校验，扣减成功才落库报名记录，防止超报 |
| `uav:user:info:{userId}` | 用户+权限缓存 | 登录后缓存，权限变更删除 |
| `uav:menu:{roleId}` | 菜单树缓存 | 菜单变更删除 |
| `uav:cert:no:seq` | 证号自增段 | `INCR` 生成证号流水 |
| `uav:lock:{biz}` | 分布式锁 | 证书签发/状态机流转加锁防并发 |

### 4.7 定时任务（@Scheduled）

1. 场次报名截止：到 `end_time` 自动 `PUBLISHED → ENROLLMENT_CLOSED`。
2. 场次结束：全部成绩 APPROVED 或超时 → `IN_PROGRESS → COMPLETED`。
3. 证书/资质到期预警：`valid_until - 30d` 生成预警公告/站内信。
4. 到期自动流转：`VALID → EXPIRED`。

---

## 5. 前端设计

### 5.1 目录结构

```
frontend/
├── vite.config.js / tailwind.config.js / postcss.config.js
├── index.html
└── src/
    ├── main.js                     # 挂载 Vuex → Pinia → ElementPlus → router → tailwind.css
    ├── api/                        # axios 实例 + 模块化接口
    │   ├── request.js              # 拦截器：注入 token、401 跳登录、错误提示
    │   └── auth.js / system.js / student.js / exam.js / certificate.js / institution.js / notice.js
    ├── utils/                      # auth.js(token 读写) / validate.js
    ├── router/
    │   ├── index.js                # 静态路由(login/404)
    │   └── guard.js                # 全局前置守卫
    ├── stores/                     # Pinia（业务状态，按 feature）
    │   ├── index.js
    │   └── modules/exam.js / certificate.js / institution.js / notice.js / student.js
    ├── store/                      # Vuex（全局基础设施）
    │   ├── index.js
    │   └── modules/user.js / permission.js / app.js
    ├── layout/                     # 后台主框架
    │   ├── index.vue
    │   └── components/Sidebar.vue / Navbar.vue / TagsView.vue / AppMain.vue
    ├── views/
    │   ├── login/index.vue
    │   ├── dashboard/index.vue
    │   ├── system/   user/ role/ menu/ log/ notice/
    │   ├── student/  profileList/ profileDetail/
    │   ├── exam/     plan/ session/ batch/ registration/ score/ scoreAudit/
    │   ├── certificate/ apply/ audit/ certificate/ reissue/
    │   ├── institution/ institution/ application/ materialReview/ inspection/ qualification/
    │   └── error/404.vue
    ├── components/                 # 通用：Pagination.vue / StatusTag.vue(v-bind 状态色) / DictTag.vue / FormModal.vue
    ├── directives/permission.js    # v-hasPermi 按钮级权限指令
    └── styles/                     # tailwind 入口 + element-plus 主题变量覆盖
```

### 5.2 页面清单（按模块）

| 模块 | 页面/组件 | 要点 |
|---|---|---|
| 登录 | login/index | 图形验证码 + 账号密码 |
| 首页 | dashboard | 统计卡片：报名数/通过率/待审成绩/待核查机构 + 公告列表 |
| 系统 | user：列表+新增/编辑/分配角色/重置密码 | 弹窗表单 + 分页 |
| 系统 | role：列表+分配菜单（el-tree 勾选）| 菜单树组件复用 |
| 系统 | menu：树形 CRUD | el-table tree 结构 |
| 系统 | log：查询列表 | 只读 |
| 系统 | notice：CRUD + 发布 | 富文本（vue-quill）可选 |
| 考生 | profileList / profileDetail | 详情含考试+证书时间线 Tab |
| 考试 | plan：计划列表+发布 | 状态 Tag |
| 考试 | session：场次列表+新增+发布+截止 | 名额/已报进度条 |
| 考试 | batch：批次编排 | 将已 APPROVED 报名拖入批次（或表格勾选）|
| 考试 | registration：报名列表+审核+编排 | 状态流转按钮按权限渲染 |
| 考试 | score：成绩录入列表 | 考官按场次录入 |
| 考试 | scoreAudit：成绩审核 | 通过/驳回，通过时展示自动判定结果 |
| 证书 | apply：申请列表+审核 | |
| 证书 | certificate：证书列表+详情+换发/吊销 | 证号、有效期、状态 Tag |
| 机构 | institution：机构列表+详情 | |
| 机构 | application：认证申请+提交材料 | 步骤条 el-steps 展示流程进度 |
| 机构 | materialReview：材料审查 | 预览文件 + 通过/退回 |
| 机构 | inspection：核查任务管理 | 派发/填写结果 |
| 机构 | qualification：资质证列表 | 续期/吊销 |

### 5.3 路由与权限守卫

1. 静态路由：`/login`、`/404`。
2. 登录成功 → Vuex user 保存 token + 调用 `/api/auth/me` 获取用户与权限集合 → Vuex permission 依据后端菜单树 `router.addRoute` 注册动态路由（component 用 `import.meta.glob` 按路径映射 views 目录）→ 存菜单到 store。
3. 全局前置守卫 `guard.js`：
   - 无 token 且非白名单 → 重定向 `/login?redirect=...`；
   - 有 token 但未拉取权限 → `await store.dispatch('permission/generateRoutes')`；
   - 已登录访问 `/login` → 跳首页。
4. 按钮级控制：指令 `v-hasPermi="'exam:score:audit'"`；非 admin 且无 `*:*:*` 通配时隐藏。
5. 404 兜底：动态路由注册失败或非法路径 → `/404`。

### 5.4 Pinia / Vuex 分工（已见 1.3）+ 使用示例

- Vuex `user.js`：`token / userInfo / roles / permissions`；`login / getInfo / logout` actions。
- Vuex `permission.js`：`routes / menus`；`generateRoutes(menus)`。
- Vuex `app.js`：`sidebarCollapsed / theme`。
- Pinia `exam.js`：`listQuery {page,status,keyword} / list / total / selectedRows / currentSession`；`fetchList() / resetQuery()`。
- Pinia `certificate.js` / `institution.js` 同理。

约定：业务页面只 `useXxxStore()`（Pinia），不触碰 Vuex；只有登录/布局/权限相关组件 `useStore()`（Vuex）。

### 5.5 Element Plus + Tailwind 组合约定

1. **职责边界**：Element Plus 负责业务组件（表格/表单/弹窗/分页/树/日期/消息）；Tailwind 负责布局与视觉（间距、flex/grid、字号配色、响应式、hover 态）。
2. **页面骨架模板**（统一约定）：页面外层 `<div class="p-5">` → 搜索区 `<el-form inline>` → 工具条（按钮组）→ `<el-table>` → `<el-pagination>`。
3. **样式入口**：`styles/index.css` 中 `@tailwind base/components/utilities`；Element Plus 主题色通过 `--el-color-primary` 等 CSS 变量在 `:root` 覆盖，配合 Tailwind `theme.extend.colors.primary` 保持同源。
4. **禁止混用**：不在 el-table 上堆叠 Tailwind 覆盖组件内部结构；自定义通用视觉（卡片、状态圆点）抽成 Tailwind 组件类 `@apply`。
5. **状态 Tag 组件**：`StatusTag` 以 prop `status` 映射枚举色，前后端枚举文案统一维护一份常量表。

---

## 6. docker-compose 与初始化

### 6.1 docker-compose.yml 服务定义要点

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: uav-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: uav_admin
      TZ: Asia/Shanghai
    ports: ["3306:3306"]
    volumes:
      - ./mysql/init:/docker-entrypoint-initdb.d   # 首次建库自动执行 01-schema.sql、02-data.sql
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-uroot", "-proot123"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  redis:
    image: redis:7-alpine
    container_name: uav-redis
    ports: ["6379:6379"]
    command: ["redis-server", "--appendonly", "yes"]
    volumes: [redis-data:/data]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:3-management
    container_name: uav-rabbitmq
    ports: ["5672:5672", "15672:15672"]          # 15672 管理台
    environment:
      RABBITMQ_DEFAULT_USER: uav
      RABBITMQ_DEFAULT_PASS: uav123
    volumes: [rabbitmq-data:/var/lib/rabbitmq]
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 15s
      timeout: 10s
      retries: 5

volumes:
  mysql-data: / redis-data: / rabbitmq-data:
```

> 说明：MySQL 初始化脚本仅首次建卷时执行；后续改 SQL 需 `docker compose down -v` 重建卷，或在脚本内做 `CREATE TABLE IF NOT EXISTS` + 幂等插入（`INSERT ... ON DUPLICATE KEY UPDATE` / 存在判断）。`mysql/init` 与仓库 `sql/` 目录保持同一份文件（或 symlink/构建复制），避免两份漂移。

### 6.2 端口规划

| 服务 | 端口 |
|---|---|
| 后端 Spring Boot | 8080 |
| 前端 Vite dev | 5173 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672（AMQP）/ 15672（管理台）|

### 6.3 初始化 SQL 挂载

`01-schema.sql`：26 张表 DDL + 索引 + 外键（外键以逻辑外键为主，仅关键关系建 FK，其余靠索引）。
`02-data.sql`：种子数据（见第 7 节）。

---

## 7. 种子数据方案

`02-data.sql` 或后端启动 `CommandLineRunner` 兜底补数据（推荐 SQL 为主、runner 为辅，避免密码明文出现在 SQL——管理员密码预生成 BCrypt 密文写入 SQL）。

| 类别 | 内容 |
|---|---|
| 角色 | ADMIN / EXAMINER / INSTITUTION_ADMIN / STUDENT 四条角色记录 |
| 菜单权限 | 目录-菜单-按钮三级菜单树（约 40 条），权限标识如 `exam:plan:add`、`exam:score:audit`、`cert:issue` 等 |
| 管理员 | `admin`（BCrypt 密文）/ `examiner1`、`examiner2`（考官）/ `inst_admin1`（机构管理员）/ `student1..10`（考生，密码统一 `123456`）|
| 角色绑定 | admin→ADMIN；examiner→EXAMINER；inst_admin1→INSTITUTION_ADMIN + 关联某机构；students→STUDENT |
| 样例机构 | 3 家：A「天际飞训」资质 CERTIFIED(有效)+资质证；B「云翼培训」认证申请流转中(MATERIAL_REVIEWING)；C「晨曦航空」NONE 未认证 |
| 考生档案 | 10 份档案，覆盖 3 家机构 + 个人报考，机型覆盖多旋翼/固定翼 |
| 考试数据 | 1 个计划（含 2 场次：理论+实操）；3 个批次；报名 8 条覆盖 PENDING/APPROVED/SCHEDULED/COMPLETED/REJECTED |
| 成绩 | 5 条：含 SUBMITTED、APPROVED+PASS、APPROVED+FAIL、REJECTED 各态，覆盖自动判定逻辑 |
| 合格证 | 2 张：1 张 VALID（含完整申请+审核+签发流水）、1 张申请待审 PENDING_AUDIT |
| 公告 | 2 条：已发布考试通知、草稿 |
| 操作日志 | 若干样例（登录/报名审核/成绩录入）|
| 机构认证 | 1 条进行中申请（含材料、审查记录、待核查任务）|

---

## 8. 实施顺序（里程碑，每阶段可独立验证）

| 里程碑 | 内容 | 验证标准 |
|---|---|---|
| **M0 基础设施** | 建 git 仓库、Maven Wrapper、docker-compose 起 MySQL/Redis/RabbitMQ、`01-schema.sql` 全量 DDL、Spring Boot 空壳连库、前端脚手架（Vite+Tailwind+ElementPlus+Pinia+Vuex+Router）| `docker compose up -d` 全绿；`mvnw spring-boot:run` 启动；`npm run dev` 出页面 |
| **M1 认证与 RBAC** | Spring Security+JWT、验证码、登录/登出/刷新、sys_user/role/menu 全套 CRUD、动态路由接口、前端登录页+布局+权限守卫+菜单渲染 | 各角色登录成功、菜单按角色显示、按钮权限生效 |
| **M2 考生档案+考试主链路** | 档案 CRUD、考试计划/场次/批次/考场 CRUD、报名（Redis 名额扣减+Lua）、报名审核/编排 | 并发报名不超名额；状态机流转正确 |
| **M3 成绩与判定** | 成绩录入、提交、审核、自动合格判定、RabbitMQ 结果通知、操作日志切面 | 成绩 APPROVED 后自动 PASS/FAIL 并产生通知 |
| **M4 合格证** | 申请（成绩合格自动触发）→审核→MQ 异步签发（证号/有效期）→列表/详情/换发/吊销 | 审核通过后异步生成证号，消息可靠消费，幂等 |
| **M5 机构认证** | 机构 CRUD、认证申请、材料审查、核查任务派发/完成、资质评定、发证/续期/吊销 | 完整走通 B 机构认证闭环到发证 |
| **M6 收尾** | 公告模块、dashboard 统计、种子数据完善、定时任务（截止/到期）、README、整体验收脚本（curl 冒烟） | 全流程端到端演示通过 |

---

## 9. 关键风险与注意事项

1. **JDK 版本**：本机仅 JDK8，M0 前必须先装 JDK17（`brew install openjdk@17`）；若无法安装，回退方案为 Spring Boot 2.7.x + javax 依赖线（代码层面尽量隔离版本差异，仅改动 pom）。
2. **Maven 缺失**：必须使用 Maven Wrapper，首次 `./mvnw -N wrapper` 生成；CI/本机构建不依赖全局 mvn。
3. **SQL 初始化幂等性**：docker 卷重建场景，脚本需可重复执行。
4. **并发报名**：名额扣减必须 Redis Lua 原子操作 + 数据库唯一键 `uk_session_student` 双保险。
5. **MQ 幂等**：证书签发、通知等消费端按 `traceId` 幂等去重。
6. **状态机收敛**：所有状态变更收敛到各 `xxxService` 的专用方法（如 `transition`），禁止 Controller 直接改状态字段，保证审计与并发安全。
7. **密码安全**：种子密码统一为演示用，上线前必须强制修改；JWT secret 走环境变量注入，不入库不进 git。
