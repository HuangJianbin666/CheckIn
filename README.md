# 习惯打卡（Habit Check-in）

一个完整的习惯打卡应用：**注册 → 登录 → 创建打卡项 → 打卡 → 连续天数展示 → 打卡详情月历**。

- 后端：Spring Boot 3（Java 17）+ MySQL + Redis，统一响应 `{code, message, data}`。
- 前端：uniapp Vue 3 + Vite（H5），Composition API，原生组件 + CSS，不引入 UI 库、不 mock、不自算 streak。
- 连续天数：列表卡片醒目展示当前连续天数，点击进入详情页查看历史打卡月历。
- 验收：Playwright 全链路 E2E + `scripts/verify.ps1` 一键验收。

## 目录结构

```text
checkIn-project/
├─ backend/                 # Spring Boot 后端
│  ├─ src/main/java/com/example/checkin/
│  │  ├─ auth/              # 注册/登录/退出/会话（Bearer token）
│  │  ├─ habit/             # 打卡项创建与列表
│  │  ├─ checkin/           # 打卡、连续天数、读透缓存
│  │  ├─ user/              # 用户实体与仓储
│  │  ├─ common/            # 统一响应、错误码、异常处理、请求上下文
│  │  └─ config/            # 业务时钟、安全配置
│  └─ src/main/resources/   # application-{dev,test,e2e}.yml、Flyway 迁移
├─ frontend/                # uniapp H5 前端
│  ├─ src/api/              # request/token/auth/habit 封装
│  ├─ src/utils/            # calendar.js 月历纯函数
│  ├─ src/pages/            # auth(login/register)、habits(index/create/detail)
│  └─ tests/e2e/            # Playwright 用例
├─ docs/api.md              # 接口契约与错误码
└─ scripts/verify.ps1       # 一键验收脚本
```

## 环境要求

- JDK 17
- Node.js 22 / npm 10
- Docker
- MySQL 8（本地 `127.0.0.1:3306`）
- Redis 7（Docker 容器名 `redis`）

MySQL 与 Redis 的首次部署见下方「MySQL 与 Redis 部署（Docker）」。

## MySQL 与 Redis 部署（Docker）

以下命令用 Docker 拉起 MySQL 8 与 Redis 7，默认值与后端配置对齐（`root` / `root`、端口 `3306` / `6379`）。

### 首次部署

Redis（容器名固定为 `redis`，与 `scripts/verify.ps1` 一致）：

```powershell
docker run -d --name redis -p 6379:6379 --restart unless-stopped redis:7
```

MySQL 8（默认 root 密码 `root`，与后端默认 `DB_PASSWORD=root` 一致；数据卷持久化）：

```powershell
docker run -d --name mysql -p 3306:3306 --restart unless-stopped `
  -e MYSQL_ROOT_PASSWORD=root `
  -v mysql-data:/var/lib/mysql `
  mysql:8.0 `
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

### 预建数据库

三个库由 Flyway 启动时自动建表，但**数据库本身需预建**（`checkin_dev` / `checkin_test` / `checkin_e2e` 对应 dev / test / e2e profile）：

```powershell
docker exec -i mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS checkin_dev DEFAULT CHARACTER SET utf8mb4; CREATE DATABASE IF NOT EXISTS checkin_test DEFAULT CHARACTER SET utf8mb4; CREATE DATABASE IF NOT EXISTS checkin_e2e DEFAULT CHARACTER SET utf8mb4;"
```

### 连通性检查

```powershell
docker exec mysql mysql -uroot -proot -e "SELECT 1"
docker exec redis redis-cli ping   # 应返回 PONG
```

### 容器管理

```powershell
docker start mysql ; docker start redis      # 启动
docker stop mysql ; docker stop redis        # 停止
docker restart mysql ; docker restart redis  # 重启
docker logs -f mysql                         # 跟踪 MySQL 日志
docker logs -f redis                         # 跟踪 Redis 日志
```

> 若已存在同名容器，`docker run` 会报错，改为 `docker start mysql` / `docker start redis` 复用已有容器即可。

## 连接环境变量

后端通过环境变量覆盖数据库与 Redis 连接，均带默认值：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | `root` | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_DATABASE` | 按 profile 取 0/1/2 | Redis logical DB（见下表，无需手动设置） |

若部署时修改了密码或端口，可通过 `DB_PASSWORD`、`REDIS_PORT` 等变量覆盖，与上节 Docker 部署的默认值对应。

## 环境隔离

| Spring profile | MySQL 库 | Redis DB | 用途 |
|---|---|---|---|
| `dev`（默认） | `checkin_dev` | `0` | 本地联调 |
| `test` | `checkin_test` | `1` | 后端集成测试（`*IT`） |
| `e2e` | `checkin_e2e` | `2` | Playwright 端到端验收 |

三个库均由 Flyway 在启动时自动建表，但**数据库本身需预先存在**。

## 后端启动

```powershell
cd backend

# dev profile（默认，无需显式指定）
.\mvnw.cmd spring-boot:run

# e2e profile（Playwright 验收用）
$env:SPRING_PROFILES_ACTIVE = 'e2e'
.\mvnw.cmd spring-boot:run
```

监听 `http://127.0.0.1:8080`，接口前缀 `/api/v1`。

## 前端启动

```powershell
cd frontend
npm ci
npm run dev:h5     # http://127.0.0.1:5173，/api 代理到 127.0.0.1:8080
npm run build:h5   # 生产构建
npm run test:e2e   # Playwright（前置：e2e 后端 + H5 dev server）
```

首次运行 E2E 前安装浏览器（幂等）：`npx playwright install chromium`。

## API 与错误码

接口清单、请求/响应示例、字段约束与错误码速查表见 [`docs/api.md`](docs/api.md)。要点：

- 统一响应 `{code, message, data, requestId, timestamp}`，成功 `code="OK"`。
- 认证方式：`Authorization: Bearer <token>`。
- 错误码：`VALIDATION_ERROR`、`UNAUTHENTICATED`、`INVALID_CREDENTIALS`、`HABIT_NOT_FOUND`、`USERNAME_EXISTS` 等。

## 连续天数（streak）算法

`StreakService` 为纯函数，不访问时钟与数据库，日期由调用方注入。给定「该用户该打卡项的打卡日期集合（≤ 今日）」与「今日」：

1. 忽略晚于今日的日期，取最新打卡日期 `latest`。
2. 若 `latest` 早于「昨日」→ `streak=0`、`todayChecked=false`、`streakAsOf=null`。
3. 否则锚点 `anchor = (latest == 今日 ? 今日 : 昨日)`，从 `anchor` 向前连续计数，直到日期断档。
4. `streakAsOf` 为连续段锚点（今日或昨日）。

前端**只消费**后端返回的 `streak / todayChecked / streakAsOf`，映射为：

- `todayChecked=true` → `今日已打卡，连续 N 天`
- `todayChecked=false && streak>0` → `今日未打卡，截至昨天连续 N 天`
- 否则 → `今日未打卡，连续 0 天`

## 并发幂等方案

打卡写入的幂等由数据库层保证，前端按钮 `disabled` 仅为交互体验，不据此判定成功：

- 唯一约束 `uk_records_user_habit_date(user_id, habit_id, check_in_date)`：同一用户、同一打卡项、同一自然日仅一条记录。
- 原子 upsert：`INSERT ... ON DUPLICATE KEY UPDATE id = id`，命中唯一键时变为 no-op，`recordId` 不变。
- `CheckInWriter` 以 `READ_COMMITTED` 隔离级别执行写后读 `findByKey`，返回同一 `recordId`。
- 复合外键 `fk_records_habit(user_id, habit_id) → habits(user_id, id)`，从数据库层阻止记录挂到他人打卡项。

## Redis key 语义

| key | 值 | TTL | 失效语义 |
|---|---|---|---|
| `checkin:auth:session:{sha256(token)}` | `userId` | 12 小时 | 登录写入；退出删除；不存在/过期 → `401 UNAUTHENTICATED` |
| `checkin:biz:today:{userId}:{date}` | 今日已打卡的 `habitId` 集合（JSON） | 60 秒 | 读透重建；打卡/创建后删除 |
| `checkin:biz:streak:{userId}:{habitId}:{date}` | `{streak,todayChecked,streakAsOf}`（JSON） | 60 秒 | 读透重建；打卡后删除 |

会话 key 使用 token 的 SHA-256 摘要，避免原始 token 落盘。

## 60 秒有界不一致

业务缓存（`checkin:biz:*`）是**读透缓存**，MySQL 始终是事实源：

- 读 miss 或读异常时回退数据库并尝试重建缓存；写/删失败只记日志，不影响已提交的数据库结果。
- 打卡/创建成功后主动删除缓存，删除发生在写事务提交之后，且删除失败不回滚数据库写入。
- 竞态窗口：打卡删除 key 与并发读重建 key 之间，可能读到删除前已写入的旧值；旧值最长存活至 TTL 到期，即 **最多 60 秒的有界不一致**，最终一致由 TTL 兜底。
- key 内嵌业务日期，午夜后不会沿用旧日期的缓存值。

## 一键验收

```powershell
.\scripts\verify.ps1
```

脚本顺序执行：`java -version` → Redis 连通性 → 后端单测 `mvnw test` → 后端集成测试 `mvnw verify -Pintegration` → 前端 `npm ci` + `build:h5` + `test:e2e`，任一步失败立即以非零退出码终止。运行前请确保 8080 / 5173 端口空闲。