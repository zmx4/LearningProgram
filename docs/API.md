# 接口文档

## 基础信息

- 默认服务地址：`http://localhost:1231`
- 所有接口使用 `/api` 前缀
- 除特别说明外，请求和响应均使用 JSON
- 登录接口使用 `application/x-www-form-urlencoded`

## 统一响应格式

成功响应：

```json
{
  "code": 200,
  "data": {},
  "message": "success"
}
```

失败响应：

```json
{
  "code": 400,
  "data": null,
  "message": "错误信息"
}
```

常见状态码：

| code | 含义 |
| --- | --- |
| `200` | 请求成功 |
| `400` | 参数错误或业务校验失败 |
| `401` | 未登录或登录状态已失效 |
| `403` | 没有访问权限 |
| `404` | 资源不存在 |
| `409` | 数据冲突 |

## 认证

登录成功后，服务端返回 JWT。访问需要登录的接口时，请在请求头携带：

```http
Authorization: Bearer YOUR_JWT_VALUE
```

前端会将登录信息保存到浏览器的 `authorize` 存储项中，并自动为请求添加认证头。

## 认证接口

### 登录

```http
POST /api/auth/login
Content-Type: application/x-www-form-urlencoded
```

参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 用户名或邮箱 |
| `password` | string | 是 | 密码 |

成功响应中的 `data` 包含 `token`、`expireTime`、`username` 和 `role`。

### 注册

```http
POST /api/auth/register
Content-Type: application/json
```

请求体包含用户名、密码、重复密码、邮箱和验证码等注册字段，具体校验以 `EmailRegisterVO` 为准。

### 确认重置密码

```http
POST /api/auth/reset-confirm
Content-Type: application/json
```

请求体：

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

### 重置密码

```http
POST /api/auth/reset-password
Content-Type: application/json
```

请求体包含邮箱、验证码和新密码。

### 退出登录

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

服务端会使当前 JWT 失效。

## 个人资料接口

### 查询当前用户资料

```http
GET /api/profile
Authorization: Bearer <token>
```

响应数据：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "phone": "",
  "bio": "",
  "role": "user"
}
```

### 更新当前用户资料

```http
PUT /api/profile
Authorization: Bearer <token>
Content-Type: application/json
```

请求体：

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "phone": "13800000000",
  "bio": "学习者"
}
```

角色不接受前端修改，由管理员接口管理。

## 通知接口

### 查询当前用户通知

```http
GET /api/notifications
Authorization: Bearer <token>
```

响应数据包含 `items` 和 `unreadCount`。

### 标记单条通知已读

```http
POST /api/notifications/{id}/read
Authorization: Bearer <token>
```

### 标记全部通知已读

```http
POST /api/notifications/read-all
Authorization: Bearer <token>
```

## 用户接口

### 查询用户公开信息

```http
GET /api/user/{id}
Authorization: Bearer <token>
```

返回用户名、邮箱、个人简介和临时头像信息。

## 单词接口

### 查询单词

```http
GET /api/dictionary?word=hello
Authorization: ******
```

也支持使用路径参数：`GET /api/dictionary/{word}`。

成功响应中的 `data` 包含单词、释义和记录 ID：

```json
{
  "id": 1,
  "word": "hello",
  "translation": "你好"
}
```

单词不存在时返回 `404`，单词为空或超过 255 个字符时返回 `400`。

## 签到接口

### 查询签到日历

```http
GET /api/check-in?year=2026&month=9
Authorization: ******
```

返回指定月份的签到日期、今日签到状态、连续签到天数、累计积分，以及该月的签到次数、积分和每日积分明细。

### 签到

```http
POST /api/check-in
Authorization: ******
```

每天只能签到一次。首次签到获得 1 分，连续签到时获得的积分等于当前连续签到天数；中断后重新从 1 分开始。

### 查询总积分

```http
GET /api/points
Authorization: ******
```

响应：

```json
{
  "totalPoints": 12
}
```

### 查询 CET4/CET6 单词

```http
GET /api/dictionary/cet4?count=10
Authorization: ******
```

```http
GET /api/dictionary/cet6?count=10
Authorization: ******
```

`count` 可选，默认值为 `1`，取值范围为 `1` 至 `100`。接口每次随机返回指定数量的单词，单个请求和批量请求均返回数组：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "word": "ability",
      "translation": "能力"
    }
  ],
  "message": "success"
}
```

## 管理员接口

以下接口要求当前用户具有 `admin` 角色，否则返回 `403`。

### 查询所有用户

```http
GET /api/admin/users
Authorization: Bearer <admin-token>
```

### 修改用户角色

```http
PUT /api/admin/users/{id}/role
Authorization: Bearer <admin-token>
Content-Type: application/json
```

请求体：

```json
{
  "role": "user"
}
```

角色支持 `user` 和 `admin`。管理员不能移除自己的管理员权限。

### 删除用户

```http
DELETE /api/admin/users/{id}
Authorization: Bearer <admin-token>
```

管理员不能删除当前登录账号。

### 发送通知

```http
POST /api/admin/notifications
Authorization: Bearer <admin-token>
Content-Type: application/json
```

发送给全体用户：

```json
{
  "title": "系统通知",
  "content": "平台将于今晚进行维护。",
  "type": "system",
  "targetType": "all"
}
```

按角色发送：

```json
{
  "title": "管理员通知",
  "content": "请查看最新管理安排。",
  "type": "system",
  "targetType": "role",
  "targetRole": "admin"
}
```

发送给指定用户：

```json
{
  "title": "学习提醒",
  "content": "请完成本周学习任务。",
  "type": "system",
  "targetType": "users",
  "userIds": [2, 5, 8]
}
```

`targetType` 支持：

- `all`：全体用户
- `role`：指定角色用户
- `users`：指定用户 ID

成功响应：

```json
{
  "code": 200,
  "data": {
    "sentCount": 3
  },
  "message": "success"
}
```

## 测试接口

```http
GET /api/test/hello
Authorization: Bearer <token>
```

用于验证登录态和后端鉴权是否正常。
