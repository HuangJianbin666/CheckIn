# 习惯打卡全栈应用 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 交付一个真实联通的 uniapp H5 + Spring Boot + MySQL + Redis 习惯打卡应用，并用自动化测试证明连续天数、幂等打卡和缓存行为正确。

**Architecture:** 前后端分离，H5 通过 REST API 调用 Spring Boot；MySQL 是业务事实源，Redis 保存登录会话及短期业务缓存。通过数据库唯一约束保证并发幂等，通过可注入时钟和固定业务时区保证连续天数可测试、可复现。

**Tech Stack:** JDK 17、Spring Boot 3.x、Maven Wrapper、Spring Security、Spring Data JPA/JdbcTemplate、Flyway、MySQL 8（已安装）、Redis 6+（已安装的 Docker 容器）；uniapp Vue 3 + Vite CLI（JavaScript、Composition API）；JUnit 5、Playwright。Spring Boot 编译、Maven Wrapper 和全部测试统一使用 JDK 17。

---

## Context

`E:\checkIn-project` 当前为空，也不是 Git 仓库，没有可复用代码。目标不是静态页面或 mock 演示，而是建立从 H5 操作、后端鉴权与业务处理、MySQL 持久化、Redis 缓存到界面刷新的完整闭环。本计划只描述后续开发步骤，本轮不实施。

## 已确认的需求决策

- **账号：** 注册 + 用户名密码登录；密码哈希存储；Redis 保存登录态；提供退出和当前用户查询。
- **打卡项：** 只做创建、列表、打卡和连续天数展示；不做编辑、删除、归档、分类、提醒或社交。
- **打卡：** 每用户、每打卡项、每自然日最多一条；只打当天，不补卡、不撤销；重复点击和网络重试幂等返回已有结果。
- **时区：** 业务日期固定由服务端按 `Asia/Shanghai` 计算，不接受客户端指定打卡日期；数据库时间戳统一按 UTC 写入。
- **连续天数：** 今日已打则算至今日；今日未打但昨天有记录则保留截至昨天的连续值，同时标记今日未打；昨天也无记录则为 0；断卡后的新记录从 1 重新计算。
- **运行：** 复用已安装的 MySQL 和 Docker Redis；后端、H5 在本机运行，不将 MySQL、后端或 H5 容器化。开发、集成测试、E2E 使用独立 MySQL schema，并共享现有 Redis 容器的 logical DB 0/1/2，防止污染开发数据；不重新安装或重复部署 MySQL/Redis。
- **工程：** 官方 uniapp Vue 3 + Vite CLI，可用 npm scripts 启动、构建和执行浏览器验收。
- **验收：** 算法单元测试、真实 MySQL/Redis 集成测试、并发重复打卡测试、Playwright H5 全链路测试和中文 README。

## 非目标

不实现邮箱/手机验证、第三方登录、补卡、撤销、历史统计图表、历史最长连续、打卡项编辑/删除、后台管理、消息队列、分布式锁、JWT、生产部署或多时区用户配置。前端不使用 mock 数据，按钮加载状态只用于体验，不代替数据库幂等约束。

## 本机环境现状与前置条件

MySQL 和 Redis 均已安装，Redis 已运行在 Docker 中。实施时复用现有服务，不下载、安装、重装或重新部署 MySQL/Redis，也不新增 `compose.yaml`、拉取新镜像或创建第二个 Redis 实例。环境检查只验证版本、连接和容器状态：

- 后端必须具备并使用 **JDK 17**：检查 `java -version`、`JAVA_HOME` 和 `.\mvnw.cmd -version`，确保 Spring Boot 编译、Maven Wrapper 和测试实际使用同一 JDK 17，而不是仅修改 `pom.xml` 版本号。
- 项目提供 Maven Wrapper，避免依赖本机 Maven 版本；已有 Maven 3.8.4 仅作为环境信息，不要求重装。
- 已有 Node.js 22.22.0、npm 10.9.4；实施时验证其与所选 uniapp 官方模板兼容。
- 复用已安装的 MySQL 8 服务，验证连接后创建 `checkin_dev`、`checkin_test`、`checkin_e2e` 三个应用 schema。该步骤只初始化业务数据库，不是重装 MySQL。
- 复用已有 Docker Redis 容器，通过 `docker ps` 确认实际容器名和运行状态；仅在容器停止时用 `docker start <redis-container>` 启动，并用 `docker exec <redis-container> redis-cli ping` 验证返回 `PONG`。
- 开发、集成测试、E2E 共享上述 Redis 容器，分别使用 logical DB 0/1/2；不使用 H2 或嵌入式 Redis，唯一键冲突、事务隔离和缓存行为必须在真实 MySQL/Redis 上验证。

## 推荐架构与工程结构

```text
H5 页面 → /api/v1 → Controller/DTO 校验 → Service → MySQL
                         │                    └→ Redis（会话/短期缓存）
                         └→ 统一响应、鉴权、异常处理
```

- MySQL 是唯一业务事实源，连续天数由打卡记录计算，不另设统计表。
- 登录使用安全随机的**不透明 Bearer token**，Redis 保存 token 到用户的会话；不使用 JWT。前端通过 `Authorization: Bearer ...` 认证，H5 开发服务器代理 `/api` 到后端。
- 打卡正确性只依赖 MySQL 唯一约束和原子 upsert，不依赖前端按钮禁用、Redis 锁或“先查再插”。
- Redis 操作不放在数据库事务中；数据库提交成功后才失效缓存。

```text
E:\checkIn-project\
├─ README.md
├─ .gitignore
├─ scripts\verify.ps1
├─ backend\
│  ├─ pom.xml / mvnw / mvnw.cmd
│  └─ src\
│     ├─ main\java\com\example\checkin\
│     │  ├─ common\       # ApiResponse、异常及 requestId
│     │  ├─ config\       # Clock、Security、Redis、测试数据安全守卫
│     │  ├─ auth\         # 注册登录、RedisSessionStore、认证过滤器
│     │  ├─ user\         # User、UserRepository
│     │  ├─ habit\        # HabitController/Service/Entity/Repository
│     │  └─ checkin\      # 打卡写入、连续天数、记录仓储、业务缓存
│     ├─ main\resources\
│     │  ├─ application.yml / application-dev.yml / application-e2e.yml
│     │  └─ db\migration\V1__init.sql
│     └─ test\            # *Test 单测、*IT 真实服务集成测试
└─ frontend\
   ├─ package.json / vite.config.js / playwright.config.js
   ├─ src\api\            # request、auth、habit API 封装
   ├─ src\pages\auth\     # login.vue、register.vue
   ├─ src\pages\habits\   # index.vue、create.vue
   └─ tests\e2e\checkin.spec.js
```

后端依赖限定为 Web、Validation、Security、Data JPA、Data Redis、MySQL Connector、Flyway 和测试依赖；不引入 Lombok、Redisson、消息队列。前端使用官方 Vue 3 JavaScript 模板和少量原生组件/CSS，不引入重型 UI 库。

## API 契约

所有接口使用 `/api/v1` 前缀。DTO 使用独立 record/对象，不直接暴露 Entity；日期格式为 `yyyy-MM-dd`，时间戳为 UTC ISO-8601。

| 方法 | 路径 | 认证 | 成功结果 |
|---|---|---|---|
| POST | `/auth/register` | 否 | 201，创建用户，不自动登录 |
| POST | `/auth/login` | 否 | 200，返回 Bearer token 和用户信息 |
| POST | `/auth/logout` | 是 | 200，删除 Redis 会话 |
| GET | `/auth/me` | 是 | 200，恢复当前登录用户 |
| POST | `/habits` | 是 | 201，创建打卡项 |
| GET | `/habits` | 是 | 200，返回列表、今日状态、当前连续天数 |
| POST | `/habits/{habitId}/check-ins` | 是 | 200，首次或重复打卡均幂等返回同一记录 |

打卡请求不接收日期、时间或时区字段。列表响应包含 `today`、`items[{id,name,createdAt,todayChecked,streak,streakAsOf}]`；打卡响应包含 `recordId`、`habitId`、`checkInDate`、`todayChecked=true`、`streak`、`streakAsOf`。不根据 JDBC affected rows 判断是否新建，重复请求始终返回数据库已有记录。

统一响应结构：

```json
{ "code": "OK", "message": "success", "data": {}, "requestId": "...", "timestamp": "..." }
```

失败时 `data=null`，HTTP 状态表达错误类别，`code` 为稳定机器可读原因：

- 400：`VALIDATION_ERROR`、`MALFORMED_JSON`
- 401：`UNAUTHENTICATED`、`INVALID_CREDENTIALS`
- 403：`ACCESS_DENIED`
- 404：`HABIT_NOT_FOUND`、`ENDPOINT_NOT_FOUND`
- 405：`METHOD_NOT_ALLOWED`
- 409：`USERNAME_EXISTS`
- 503：`AUTH_SERVICE_UNAVAILABLE`、`DATABASE_UNAVAILABLE`
- 500：`INTERNAL_ERROR`

访问他人的打卡项也返回 `HABIT_NOT_FOUND`，避免泄露资源存在性。未知异常只记录服务端堆栈，不向前端泄露 SQL、Redis key、密码或堆栈。

## MySQL 表、约束与索引

迁移文件：`backend/src/main/resources/db/migration/V1__init.sql`。统一 InnoDB、`utf8mb4`、UTC 会话；`created_at` 使用 `TIMESTAMP(6)`，业务日期使用 `DATE`，JPA 设置 `ddl-auto=validate`，只能由 Flyway 改表。

1. **`users`**：`id` PK、`username`、`password_hash`、`created_at`；`uk_users_username(username)` 唯一。用户名统一 trim 后用 `Locale.ROOT` 转小写，3–32 位字母/数字/下划线；BCrypt 哈希密码，密码至少 8 位且 UTF-8 不超过 72 字节。
2. **`habits`**：`id` PK、`user_id`、`name`、`created_at`；名称 trim 后 1–50 字，允许同名。外键关联用户，并建立 `(user_id,id)` 唯一索引支持归属校验及列表查询。
3. **`check_in_records`**：`id` PK、`user_id`、`habit_id`、`check_in_date`、`created_at`；核心唯一约束 **`uk_records_user_habit_date(user_id,habit_id,check_in_date)`**，另建 `(user_id,check_in_date)` 索引支持列表查询。复合外键 `(user_id,habit_id)` 关联 `habits(user_id,id)`，从数据库层防止记录挂到他人打卡项。

不提供删除打卡项或记录的 API，外键默认 RESTRICT。

## 连续天数与时区算法

关键文件：`config/ClockConfig.java`、`checkin/StreakService.java`。生产注入 `Clock.system(ZoneId.of("Asia/Shanghai"))`，测试注入 fixed/mutable Clock；业务类禁止直接调用无 Clock 的 `LocalDate.now()`、数据库 `CURDATE()` 或客户端日期。每个请求开始时只获取一次 `today`，避免请求跨午夜产生混合结果。

对该用户、该打卡项查询 `check_in_date <= today` 的倒序去重日期：

1. 无记录：`todayChecked=false`、`streak=0`、`streakAsOf=null`。
2. 最近日期是今天：从今天向前逐日计数。
3. 最近日期是昨天：从昨天向前逐日计数，返回 `todayChecked=false`，保留截至昨天的连续值。
4. 最近日期早于昨天：`streak=0`，不把历史最长段误当当前连续。
5. 遇到第一个日期缺口停止；`streakAsOf` 为本次连续段的锚点（今天或昨天）。

必须测试首次打卡、连续多日、断卡、今日未打但昨天有、昨天也无、跨月、跨年、闰日，以及 `2026-11-19T15:59:59Z`（上海 11 月 19 日）和 `2026-11-19T16:00:00Z`（上海 11 月 20 日）的边界。

## 并发幂等与事务

关键文件：`checkin/CheckInService.java`、`CheckInWriter.java`、`CheckInRecordJdbcRepository.java`。

- `CheckInWriter` 使用独立事务 Bean 和 `READ_COMMITTED`：按 `habitId + 当前 userId` 校验归属，捕获一次服务端日期，然后由 JdbcTemplate 执行 `INSERT ... ON DUPLICATE KEY UPDATE id=id`，最后按自然键重新读取记录并计算连续天数。
- 并发唯一键冲突时，upsert 等待前序事务结束；对方提交则本次变为 no-op，对方回滚则本次正常插入。写后查询保证返回同一个 `recordId`。
- 不在同一事务里捕获 `DuplicateKeyException` 后继续提交，避免 rollback-only、持久化上下文和快照读问题。注册冲突则让唯一键异常离开事务，由全局异常处理器转为 409。
- `CheckInService` 是非事务协调器：等待 `CheckInWriter` 提交后再操作 Redis；缓存失败不回滚已经成功的数据库打卡。
- 即便缓存显示“已打卡”，打卡写接口仍执行数据库 upsert；缓存不是防重复依据。

## Redis key、TTL、一致性与降级

所有缓存写入在单条命令中同时设置值和过期时间，避免永久 key；日志不记录原始 token。

| Key | 值 | TTL | 为什么放 Redis |
|---|---|---:|---|
| `checkin:auth:session:{sha256(token)}` | userId | 固定 12 小时 | 每次请求快速验证、支持主动退出，无需查用户表；不滑动续期 |
| `checkin:biz:today:{userId}:{yyyy-MM-dd}` | 当日已打 habitId JSON 集合 | 60 秒 | 减少列表页反复查询当天记录；不缓存长期状态 |
| `checkin:biz:streak:{userId}:{habitId}:{yyyy-MM-dd}` | streak、todayChecked、streakAsOf JSON | 60 秒 | 避免重复扫描历史记录；日期进入 key，午夜后不沿用旧结果 |

一致性策略：

- **DB 是事实源。** 业务缓存 miss 或读取异常时查询 MySQL 并尝试重建；重建失败不影响返回数据库结果。
- 打卡提交后删除该用户当日状态 key 和该 habit 当日 streak key；创建 habit 后删除当日状态 key；重复打卡也执行失效，以修复潜在脏缓存。
- 读请求可能在写后失效前读到旧 DB 状态，并在失效后重新写回旧缓存；60 秒 TTL 将这类竞争及缓存删除失败造成的脏读限制在最多 60 秒。打卡请求自身始终返回刚提交的数据库结果。
- 认证缓存与业务缓存语义不同：Redis 认证异常返回 503，绝不跳过鉴权；登录时 Redis 写会话失败则登录 503；业务缓存故障才允许回退 DB。
- 若整站 Redis 停止，受保护接口会先因认证失败返回 503，不会进入业务回退。登出删除失败时前端仍可丢弃 token，服务端会话最长残留至 TTL。
- 人工修库、恢复备份或导入记录后必须清理对应业务 key；正常 API 不提供补卡、撤销或删除，因此没有额外历史缓存失效分支。

## 前端实现

- `src/api/request.js` 封装 `uni.request`、Bearer token、统一响应解析、错误提示及 401 清理登录态并跳转；base URL 使用 `/api/v1`，Vite 代理到 `127.0.0.1:8080`。
- 注册、登录、创建打卡项、列表和打卡全部调用真实 API；禁止本地假列表、mock 响应或浏览器计算 streak。
- token 使用 uniapp 存储 API 保存以支持 H5 刷新；本机演示采用 Bearer 以避免 Cookie/CSRF 复杂度，README 说明生产环境需配合 XSS 防护，或可改用 Secure HttpOnly Cookie。
- 打卡按钮请求期间禁用只是体验优化；最终幂等仍由后端保证。
- 展示文案：今日已打为“今日已打卡，连续 N 天”；今日未打且 N>0 为“今日未打卡，截至昨天连续 N 天”；N=0 为“今日未打卡，连续 0 天”。

## 分阶段 TDD 开发任务

### Task 0：环境与工程骨架

确认 `java -version` 和 `.\mvnw.cmd -version` 均使用 JDK 17；验证已安装的 MySQL 可连接，并创建三个应用 schema，不重新安装 MySQL。通过 `docker ps` 确认现有 Redis 容器，只有容器已停止时才执行 `docker start <redis-container>`，随后执行 `docker exec <redis-container> redis-cli ping` 并确认返回 `PONG`；不安装、重建或替换 Redis。创建后端、uniapp、npm scripts 和 Maven Wrapper，配置 Surefire 跑 `*Test`、Failsafe 的 `integration` profile 跑 `*IT`。先验证空工程单测与 H5 构建，再进入业务开发。

### Task 1：统一响应、异常和数据库迁移

先写成功/校验失败/未知异常结构测试及 schema 集成测试，再实现 `ApiResponse`、`GlobalExceptionHandler`、requestId、`V1__init.sql`、Entity/Repository。验证三表、唯一约束、复合外键、UTC 配置及 JPA validate。

### Task 2：注册登录与 Redis 会话

先写注册、重复用户名、密码哈希、错误密码、假/过期 token、退出、用户隔离及认证 Redis 故障测试，再实现 Auth、RedisSessionStore、认证过滤器和 Security 配置。会话集成测试连接现有 Docker Redis 的 logical DB 1，不使用嵌入式 Redis 或新建实例。Spring Security 禁止额外创建 `JSESSIONID`，Redis 异常必须终止过滤链。

### Task 3：打卡项创建和数据库版列表

先写创建后可见、空/超长名称、多用户隔离、新项目状态为 0 的测试，再实现 Habit API 和 MySQL 查询。本阶段暂不引入业务缓存，先验证事实源路径。

### Task 4：连续天数算法

在 `StreakServiceTest` 中先覆盖上述所有日期边界并确认失败，再实现最小算法；所有测试使用注入 Clock，不依赖运行测试时的真实日期。

### Task 5：打卡和真实并发

先写首次打卡、顺序重复、不存在/他人资源、客户端伪造日期字段和 32 并发 HTTP 请求测试，再实现 READ_COMMITTED writer、原子 upsert、写后查询及非事务协调器。并发测试必须直接核对数据库行数和所有响应的 recordId。

### Task 6：Redis 业务缓存与故障注入

先写缓存命中、key 日期隔离、TTL、写后失效、读异常回退 DB、写/删异常不影响数据库结果及整站 Redis 故障返回 503 的测试，再实现缓存适配器。缓存集成测试连接现有 Docker Redis 的 logical DB 1，不创建新实例。故障注入只存在测试中，正常应用和 E2E 不 mock 业务数据。

### Task 7：H5 真实联调

先以真实后端 API 为契约实现请求封装，再完成注册、登录、创建、列表、打卡、刷新恢复和退出。检查浏览器 Network 确实访问后端，页面没有硬编码业务结果。

### Task 8：Playwright、文档和统一验收

配置 E2E 使用独立 schema 和现有 Docker Redis 的 logical DB 2，真实启动后端和 H5；完成 Playwright 全链路测试、中文 README 和 `scripts/verify.ps1`。README 写明 JDK 17、现有 MySQL/Redis 连接环境变量，以及已有 Redis 容器的启动、停止、日志查看和恢复命令，但不提供 MySQL/Redis 安装教程。任何一步失败均返回非零退出码，不宣称验收通过。

## 测试隔离与安全

| 环境 | MySQL schema | Redis logical DB |
|---|---|---:|
| dev | `checkin_dev` | 0 |
| integration test | `checkin_test` | 1 |
| Playwright E2E | `checkin_e2e` | 2 |

三个环境复用同一套已安装的 MySQL 服务和 Docker Redis 容器，不新建或替换实例，仅通过 schema 和 logical DB 隔离数据。test/e2e profile 启动守卫必须检查 JDBC schema 和 Redis DB，不匹配即拒绝启动。禁止 `FLUSHALL`；测试清理只针对测试 schema 和对应 Redis DB 中的测试 key。凭据通过环境变量或忽略的本地配置注入，`.gitignore` 排除凭据、`target`、`node_modules`、构建及 Playwright 产物。

## 端到端验证与完成标准

MySQL 和 Docker Redis 均已安装，不要重新安装、重建或替换实例。先确认 JDK 17 生效、MySQL 可连接且三个应用 schema 已初始化，再检查并启动已有 Redis 容器、设置所需环境变量。将 `<redis-container>` 替换为 `docker ps` 显示的实际容器名：

```powershell
cd E:\checkIn-project
java -version                          # 期望显示 17.x
docker ps                              # 确认现有 Redis 容器名及状态
docker start <redis-container>         # 仅容器已停止时执行
docker exec <redis-container> redis-cli ping  # 期望 PONG

cd E:\checkIn-project\backend
.\mvnw.cmd -version                    # 期望 Java version 为 17.x
.\mvnw.cmd test                        # 期望 BUILD SUCCESS
.\mvnw.cmd verify -Pintegration        # 期望真实 MySQL/Redis 测试全部通过

cd E:\checkIn-project\frontend
npm ci
npx playwright install chromium
npm run build:h5                       # 期望生成 H5 构建产物
npm run test:e2e                       # 期望全部浏览器用例通过

cd E:\checkIn-project
.\scripts\verify.ps1                   # 最终重复执行统一验收
```

验收必须证明：

1. 注册→登录→创建打卡项→打卡→界面显示连续 1 天的数据确实经过后端和 MySQL；刷新后仍保持。
2. 32 个同用户、同 habit、同日期并发请求全部成功、响应 recordId 相同、数据库仅一行、最终 streak=1，无 `UnexpectedRollbackException`。
3. 断卡、昨日锚点、跨月/年/闰日及上海午夜边界测试全部通过。
4. 第二个用户看不到第一个用户的数据；退出或 token 无效后无法访问受保护接口。
5. 业务缓存可回退数据库；认证 Redis 故障返回 503 而非放行；缓存重建后的状态与数据库一致。
6. Playwright 覆盖注册、错误/正确登录、创建、打卡、重复点击、刷新、用户隔离、退出；不使用 route mock。
7. 人工复核一次登录后停止现有 Docker Redis 容器：受保护接口为 503；重新启动同一容器后，会话按 TTL 状态恢复或要求重新登录，业务数据仍以 MySQL 为准。演练不得删除容器、镜像或数据。
8. README 写清 JDK 17 和现有 MySQL/Redis 连接环境变量、应用启动命令、已有 Redis 容器的启动/停止/日志/恢复命令、API/错误码、算法示例、唯一约束并发方案、每个 Redis key 的用途/TTL/失效与故障语义，以及 60 秒有界不一致；不提供 MySQL/Redis 安装教程，不隐瞒任何限制。

第 7 项故障演练只操作上述现有 Redis 容器：

```powershell
docker stop <redis-container>
docker start <redis-container>
docker exec <redis-container> redis-cli ping  # 期望 PONG
```
