# 习惯打卡后端接口文档

**Base URL:** `http://127.0.0.1:8080/api/v1`
**认证方式:** Bearer Token（登录后从 `token` 字段获取，请求头 `Authorization: Bearer <token>`）
**Content-Type:** `application/json`

## 统一响应结构

成功和失败都使用同一外壳：

```json
{
  "code": "OK",
  "message": "success",
  "data": { },
  "requestId": "uuid",
  "timestamp": "2026-11-21T03:00:00Z"
}
```

- 成功：`code="OK"`，HTTP 状态为 2xx，`data` 为具体业务对象。
- 失败：`data=null`，HTTP 状态表达错误类别，`code` 为稳定机器可读原因。
- 每次响应头都带 `X-Request-Id`，可用于排查日志。
- 时间戳统一为 UTC ISO-8601（如 `createdAt`），业务日期统一为 `yyyy-MM-dd`（如 `streakAsOf`、`checkInDate`）。

---

## 接口清单

| # | 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|---|
| 1 | POST | `/auth/register` | 否 | 注册用户，不自动登录 |
| 2 | POST | `/auth/login` | 否 | 登录，返回 token |
| 3 | GET | `/auth/me` | 是 | 查询当前登录用户 |
| 4 | POST | `/auth/logout` | 是 | 退出，删除会话 |
| 5 | POST | `/habits` | 是 | 创建打卡项 |
| 6 | GET | `/habits` | 是 | 打卡项列表（含今日状态、连续天数） |
| 7 | POST | `/habits/{habitId}/check-ins` | 是 | 打卡（幂等） |

---

## 1. 注册

`POST /auth/register`

请求体：

```json
{
  "username": "alice",
  "password": "password123"
}
```

字段约束：

- `username`：必填，3–32 位，仅字母、数字、下划线（服务端会统一转小写）。
- `password`：必填，8–72 位。

成功 `201 Created`：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": 1,
    "username": "alice",
    "createdAt": "2026-11-21T03:00:00Z"
  },
  "requestId": "uuid",
  "timestamp": "2026-11-21T03:00:00Z"
}
```

失败：

- `409 USERNAME_EXISTS` — 用户名已存在。
- `400 VALIDATION_ERROR` — 用户名/密码格式不合法。

---

## 2. 登录

`POST /auth/login`

请求体：

```json
{
  "username": "alice",
  "password": "password123"
}
```

成功 `200 OK`：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "token": "64位十六进制字符串",
    "user": {
      "id": 1,
      "username": "alice",
      "createdAt": "2026-11-21T03:00:00Z"
    }
  },
  "requestId": "uuid",
  "timestamp": "2026-11-21T03:00:00Z"
}
```

> `token` 为不透明 Bearer token，后续所有受保护接口都需要携带。

失败：

- `401 INVALID_CREDENTIALS` — 用户名或密码错误。
- `400 VALIDATION_ERROR` — 用户名/密码为空。

---

## 3. 查询当前用户

`GET /auth/me`

请求头：`Authorization: Bearer <token>`

成功 `200 OK`：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": 1,
    "username": "alice",
    "createdAt": "2026-11-21T03:00:00Z"
  },
  "requestId": "uuid",
  "timestamp": "2026-11-21T03:00:00Z"
}
```

失败：

- `401 UNAUTHENTICATED` — 无 token / token 无效 / token 已过期。
- `503 AUTH_SERVICE_UNAVAILABLE` — Redis 会话服务不可用（绝不放行）。

---

## 4. 退出

`POST /auth/logout`

请求头：`Authorization: Bearer <token>`

成功 `200 OK`，`data=null`：

```json
{
  "code": "OK",
  "message": "success",
  "data": null,
  "requestId": "uuid",
  "timestamp": "2026-11-21T03:00:00Z"
}
```

> 退出后服务端删除会话，该 token 立即失效。

---

## 5. 创建打卡项

`POST /habits`

请求头：`Authorization: Bearer <token>`

请求体：

```json
{
  "name": "阅读 30 分钟"
}
```

字段约束：`name` 必填，1–50 字。

成功 `201 Created`：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": 1,
    "name": "阅读 30 分钟",
    "createdAt": "2026-11-21T03:00:00Z",
    "todayChecked": false,
    "streak": 0,
    "streakAsOf": null
  },
  "requestId": "uuid",
  "timestamp": "2026-11-21T03:00:00Z"
}
```

失败：

- `400 VALIDATION_ERROR` — 名称为空或超过 50 字。
- `401 UNAUTHENTICATED` — 未登录。

---

## 6. 打卡项列表

`GET /habits`

请求头：`Authorization: Bearer <token>`

成功 `200 OK`：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "today": "2026-11-21",
    "items": [
      {
        "id": 1,
        "name": "阅读 30 分钟",
        "createdAt": "2026-11-21T03:00:00Z",
        "todayChecked": true,
        "streak": 1,
        "streakAsOf": "2026-11-21"
      }
    ]
  },
  "requestId": "uuid",
  "timestamp": "2026-11-21T03:00:00Z"
}
```

字段含义：

- `todayChecked`：今日是否已打卡。
- `streak`：当前连续天数。
- `streakAsOf`：连续段的锚点日期（今日或昨日）；无有效连续段时为 `null`。

> 列表只返回当前用户自己的打卡项，按创建时间升序。

---

## 7. 打卡（幂等）

`POST /habits/{habitId}/check-ins`

请求头：`Authorization: Bearer <token>`

`habitId` 为路径参数。请求体为空（不接收日期、时间、时区字段）。

成功 `200 OK`（首次或重复均返回同一结果）：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "recordId": 1,
    "habitId": 1,
    "checkInDate": "2026-11-21",
    "todayChecked": true,
    "streak": 1,
    "streakAsOf": "2026-11-21"
  },
  "requestId": "uuid",
  "timestamp": "2026-11-21T03:00:00Z"
}
```

幂等语义：同一用户、同一打卡项、同一自然日重复请求，`recordId` 不变，数据库仅一条记录。

失败：

- `404 HABIT_NOT_FOUND` — 打卡项不存在，**或**属于他人（不泄露资源存在性）。
- `401 UNAUTHENTICATED` — 未登录。
- `503 DATABASE_UNAVAILABLE` / `500 INTERNAL_ERROR` — 数据库不可用/内部错误。

---

## 错误码速查表

| HTTP | code | 触发场景 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 请求字段校验失败 |
| 400 | `MALFORMED_JSON` | 请求体不是合法 JSON |
| 401 | `UNAUTHENTICATED` | 未登录 / token 无效 / 过期 |
| 401 | `INVALID_CREDENTIALS` | 用户名或密码错误 |
| 403 | `ACCESS_DENIED` | 无权访问 |
| 404 | `HABIT_NOT_FOUND` | 打卡项不存在或属于他人 |
| 404 | `ENDPOINT_NOT_FOUND` | 接口路径不存在 |
| 405 | `METHOD_NOT_ALLOWED` | HTTP 方法不允许 |
| 409 | `USERNAME_EXISTS` | 用户名已存在 |
| 503 | `AUTH_SERVICE_UNAVAILABLE` | 认证 Redis 不可用 |
| 503 | `DATABASE_UNAVAILABLE` | 数据库不可用 |
| 500 | `INTERNAL_ERROR` | 未知内部错误 |

---

## 建议测试顺序（端到端链路）

1. `POST /auth/register` → 201
2. `POST /auth/login` → 200，拿到 `token`
3. `GET /auth/me`（带 token）→ 200，校验用户信息
4. `POST /habits`（带 token）→ 201，拿到 `habitId`
5. `GET /habits` → 200，`todayChecked=false`、`streak=0`
6. `POST /habits/{habitId}/check-ins` → 200，`streak=1`
7. 重复执行第 6 步 → 200，`recordId` 不变（幂等）
8. `GET /habits` → 200，`todayChecked=true`、`streak=1`
9. `POST /auth/logout` → 200
10. `GET /auth/me`（旧 token）→ 401

**负面测试**：重复注册（409）、错误密码登录（401）、无 token 访问受保护接口（401）、访问不存在的 `habitId`（404）。