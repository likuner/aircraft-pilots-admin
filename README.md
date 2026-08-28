# 无人机驾驶员管理后台

面向无人机驾驶员**考试管理、合格证颁发、训练机构资质认证**的核心管理机构后台系统。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17 · Maven · Spring Boot 3.3 · MyBatis-Plus · Spring Security + JWT · RabbitMQ · MySQL 8 · Redis 7 |
| 前端 | Vue 3 · TypeScript · Vite 5 · Element Plus · Tailwind CSS · Pinia（业务状态）· Vuex（全局基础设施）· Vue Router |

## 目录结构

```
aircraft-pilots-admin/
├── backend/          # Spring Boot 后端（内嵌 Maven Wrapper ./mvnw）
├── frontend/         # Vue3 前端
├── docker/
│   ├── docker-compose.yml        # MySQL(3307) + Redis(6379) + RabbitMQ(5672/15672)
│   └── mysql/init/               # 01-schema.sql(26表) + 02-data.sql(种子数据)
├── sql/              # 与 docker/mysql/init 同源
└── scripts/start.sh  # 一键启动/停止
```

## 快速启动

### 1. 基础设施（MySQL / Redis / RabbitMQ）

```bash
docker compose -f docker/docker-compose.yml up -d
# 验证：docker compose -f docker/docker-compose.yml ps  （三个容器均为 healthy）
# MySQL 首次启动自动执行 01-schema.sql（26 张表）+ 02-data.sql（种子数据）
```

> 网络说明：若无法直连 Docker Hub，已将镜像前缀指向可用镜像源 `docker.1panel.live/library/...`。

### 2. 后端（:8080）

```bash
# 需要 JDK 17
brew install openjdk@17   # macOS
export JAVA_HOME=/usr/local/opt/openjdk@17   # 或 /opt/homebrew/opt/openjdk@17
cd backend && ./mvnw spring-boot:run
# API 文档: http://localhost:8080/swagger-ui.html
```

### 3. 前端（:5173）

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

或一键：`./scripts/start.sh`（all / backend / frontend / stop）

## 默认账号（密码均为 `123456`）

| 账号 | 角色 | 可见菜单 |
|---|---|---|
| admin | ADMIN（超级管理员） | 全部（首页/系统/考生/考试/证书/机构） |
| examiner1 | EXAMINER（考官） | 首页、考试管理 |
| inst_admin1 | INSTITUTION_ADMIN（机构管理员） | 首页、考生档案、机构认证 |
| student1 | STUDENT（考生） | 首页、考生档案、证书申请 |

## 数据库（26 张表）

| 域 | 表 |
|---|---|
| 系统域 | sys_user / sys_role / sys_menu / sys_user_role / sys_role_menu / sys_operation_log / sys_notice |
| 考生档案 | stu_pilot_profile |
| 考试域 | exm_exam_plan / exm_exam_session / exm_exam_room / exm_batch / exm_registration / exm_score / exm_score_audit |
| 合格证域 | cer_certificate_apply / cer_certificate / cer_certificate_audit / cer_certificate_change_record |
| 机构认证 | org_institution / org_certification_apply / org_apply_material / org_material_review / org_site_inspection / org_qualification_review / org_qualification |

## 核心设计

- **认证**：Spring Security 无状态 RBAC + JWT（access 30min / refresh 7d，Redis 黑名单），登录图形验证码（数学算式，Redis 5min）。
- **消息异步（RabbitMQ）**：考试结果通知 `exam.result`、证书异步签发 `cert.issue`、公告广播（fanout）、机构通知 `inst.notify`，均配死信队列。
- **Redis**：验证码 / token 黑名单 / refresh token / 报名名额 Lua 原子扣减 / 证号自增 / 分布式锁。
- **状态机**：报名 PENDING→APPROVED→SCHEDULED→COMPLETED；场次 DRAFT→PUBLISHED→ENROLLMENT_CLOSED→IN_PROGRESS→COMPLETED；成绩 DRAFT→SUBMITTED→APPROVED/REJECTED；证书申请 SUBMITTED→PENDING_AUDIT→AUDIT_PASSED→ISSUED；机构认证 SUBMITTED→MATERIAL_REVIEWING→INSPECTION_PENDING→QUALIFICATION_REVIEWING→APPROVED。
- **前端分工**：Pinia 管业务状态（exam/certificate/institution/notice/student），Vuex 管全局基础设施（token/用户/权限/动态菜单/布局）。
- **操作日志**：`@Log` 注解 + AOP 环绕切面自动落库。
